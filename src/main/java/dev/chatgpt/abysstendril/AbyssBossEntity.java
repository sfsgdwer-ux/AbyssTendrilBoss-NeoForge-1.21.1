package dev.chatgpt.abysstendril;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Giant V6 boss. Hostile to all nearby living creatures, including villagers and other monsters. */
public class AbyssBossEntity extends Monster implements GeoEntity {
    private static final String PREFIX = "animation.abyss_tendril_beast.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(PREFIX + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(PREFIX + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(PREFIX + "run");
    private static final RawAnimation TENTACLE = RawAnimation.begin().thenPlay(PREFIX + "attack_tentacle");
    private static final RawAnimation STOMP = RawAnimation.begin().thenPlay(PREFIX + "attack_titan_stomp");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay(PREFIX + "attack_bite");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay(PREFIX + "attack_ruin_roar");
    private static final RawAnimation MANY_FACES = RawAnimation.begin().thenPlay(PREFIX + "attack_many_faces");
    private static final RawAnimation BARRAGE = RawAnimation.begin().thenPlay(PREFIX + "attack_tentacle_barrage");

    // modes 0=tentacle, 1=stomp, 2=bite, 3=ruin roar, 4=many mouths, 5=barrage
    private static final int[] ATTACK_LENGTHS = {34, 56, 27, 67, 52, 48};
    private static final int[] IMPACT_REMAINING = {18, 25, 14, 30, 20, 20};
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int attackTicks;
    private int attackCooldown;
    private int attackMode;
    private boolean damageApplied;

    public AbyssBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1024D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 64D)
                .add(Attributes.ATTACK_DAMAGE, 55D)
                .add(Attributes.ARMOR, 30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1D);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        // Vanilla 1.21.1 caps max-health at 1024. Scale ordinary incoming damage to give titan-like endurance.
        return super.hurt(source, amount * 0.08F);
    }

    private boolean canHunt(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof AbyssBossEntity)
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Excludes self, other copies of the boss, and players in creative/spectator.
        // Everyone else is targetable: villagers, passive animals, hostile monsters and survival players.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canHunt));
    }

    @Override public boolean doHurtTarget(Entity victim) {
        if (level().isClientSide() || attackCooldown > 0 || attackTicks > 0
                || !(victim instanceof LivingEntity target) || !canHunt(target)) return false;
        if (distanceToSqr(target) > 20D * 20D) return false;
        attackMode = getRandom().nextInt(6);
        // Ensure block-breaking roar is chosen regularly, not only by a rare random roll.
        if (getRandom().nextInt(5) == 0) attackMode = 3;
        damageApplied = false;
        attackTicks = ATTACK_LENGTHS[attackMode];
        attackCooldown = attackTicks + 18;
        triggerAnim("attacks", switch (attackMode) {
            case 0 -> "tentacle";
            case 1 -> "stomp";
            case 2 -> "bite";
            case 3 -> "roar";
            case 4 -> "faces";
            default -> "barrage";
        });
        if (attackMode == 3) {
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5F, 0.55F);
        }
        return true;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (attackCooldown > 0) attackCooldown--;
        if (attackTicks <= 0) return;
        attackTicks--;
        if (!damageApplied && attackTicks <= IMPACT_REMAINING[attackMode]) {
            damageApplied = true;
            applyBossAttack();
        }
    }

    private void applyBossAttack() {
        final double radius = switch (attackMode) {
            case 0, 4, 5 -> 12D;
            case 1 -> 10D;
            case 2 -> 8D;
            default -> 16D;
        };
        final float damage = switch (attackMode) {
            case 0 -> 55F;
            case 1 -> 72F;
            case 2 -> 92F;
            case 3 -> 65F;
            case 4 -> 48F;
            default -> 58F;
        };
        // The large model is about 25 blocks tall. Damage is measured from its lower center;
        // 18-block vertical reach allows contact with mid-body targets without an enormous AABB.
        AABB area = getBoundingBox().inflate(radius, 8D, radius);
        for (LivingEntity other : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!canHunt(other)) continue;
            Vec3 offset = other.position().subtract(position());
            if (offset.horizontalDistanceSqr() > radius * radius) continue;
            if (attackMode == 2 && getTarget() != other && offset.horizontalDistanceSqr() > 4D * 4D) continue;
            other.hurt(damageSources().mobAttack(this), damage);
            double dist = Math.max(0.01D, offset.horizontalDistance());
            double knockback = attackMode == 3 ? 2.1D : 1.1D;
            other.push(offset.x / dist * knockback, attackMode == 3 ? 0.9D : 0.45D,
                    offset.z / dist * knockback);
            other.hurtMarked = true;
        }
        if (attackMode == 3) unleashRuinRoar();
        else if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 4, getZ(),
                    15, radius * 0.5D, 1.5D, radius * 0.5D, 0.2D);
        }
    }

    /** One bounded block-breaking pulse at the impact frame. Respects mobGriefing and protection hooks. */
    private void unleashRuinRoar() {
        if (!(level() instanceof ServerLevel server)) return;
        server.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.5F, 0.65F);
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 6D, getZ(),
                14, 5D, 3D, 5D, 0D);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 3D, getZ(),
                2, 1D, 0D, 1D, 0D);
        // Rule false means zero destroyed blocks. A maximum of 70 prevents large world lag.
        if (!EventHooks.canEntityGrief(server, this)) return;
        final int maxBlocks = 70;
        int destroyed = 0;
        BlockPos center = blockPosition();
        // Ring-shaped pulse around the feet, weighted toward blocks ahead of the boss.
        Vec3 forward = getLookAngle();
        for (int y = -1; y <= 5 && destroyed < maxBlocks; ++y) {
            for (int x = -9; x <= 9 && destroyed < maxBlocks; ++x) {
                for (int z = -9; z <= 9 && destroyed < maxBlocks; ++z) {
                    double distance2 = x*x + z*z;
                    if (distance2 < 16D || distance2 > 81D || getRandom().nextFloat() > 0.16F) continue;
                    // Keep behind-boss blocks mostly intact; destroy a wider fan in front.
                    if (x * forward.x + z * forward.z < -2D && getRandom().nextFloat() > 0.20F) continue;
                    BlockPos pos = center.offset(x, y, z);
                    if (!server.hasChunkAt(pos)) continue;
                    BlockState state = server.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) continue;
                    float hardness = state.getDestroySpeed(server, pos);
                    if (hardness < 0F || hardness > 15F) continue; // protect bedrock & very hard blocks
                    if (!EventHooks.onEntityDestroyBlock(this, pos, state)) continue;
                    if (server.destroyBlock(pos, false, this)) destroyed++;
                }
            }
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (state.isMoving()) {
                if (getDeltaMovement().horizontalDistanceSqr() > 0.09D) return state.setAndContinue(RUN);
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attacks", 0, state -> PlayState.STOP)
                .triggerableAnim("tentacle", TENTACLE)
                .triggerableAnim("stomp", STOMP)
                .triggerableAnim("bite", BITE)
                .triggerableAnim("roar", ROAR)
                .triggerableAnim("faces", MANY_FACES)
                .triggerableAnim("barrage", BARRAGE));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}