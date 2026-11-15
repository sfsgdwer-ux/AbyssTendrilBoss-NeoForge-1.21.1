package dev.chatgpt.abysstendril;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AbyssBossEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("animation.abyss_tendril_beast.idle");
    private static final RawAnimation WALK=RawAnimation.begin().thenLoop("animation.abyss_tendril_beast.walk");
    private static final RawAnimation RUN=RawAnimation.begin().thenLoop("animation.abyss_tendril_beast.run");
    private static final RawAnimation TENTACLE=RawAnimation.begin().thenPlay("animation.abyss_tendril_beast.attack_tentacle");
    private static final RawAnimation STOMP=RawAnimation.begin().thenPlay("animation.abyss_tendril_beast.attack_stomp");
    private static final RawAnimation BITE=RawAnimation.begin().thenPlay("animation.abyss_tendril_beast.attack_bite");
    private static final RawAnimation ROAR=RawAnimation.begin().thenPlay("animation.abyss_tendril_beast.attack_roar");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int attackTicks=0, attackCooldown=0, attackMode=0;
    private boolean damageApplied=false;
    public AbyssBossEntity(EntityType<? extends Monster> type,Level level){super(type,level);this.xpReward=80;}
    public static AttributeSupplier.Builder createBossAttributes(){
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,320D).add(Attributes.MOVEMENT_SPEED,0.23D)
        .add(Attributes.FOLLOW_RANGE,40D).add(Attributes.ATTACK_DAMAGE,14D).add(Attributes.ARMOR,12D)
        .add(Attributes.KNOCKBACK_RESISTANCE,0.85D);
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1D,true));
        goalSelector.addGoal(5,new RandomStrollGoal(this,0.75D));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,18F));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean doHurtTarget(Entity victim){
        if(level().isClientSide()||attackCooldown>0||attackTicks>0||!(victim instanceof LivingEntity target)||!target.isAlive()||distanceToSqr(target)>42D)return false;
        attackMode=getRandom().nextInt(4);
        damageApplied=false;
        attackTicks=switch(attackMode){case 0->34;case 1->38;case 2->27;default->51;};
        attackCooldown=attackTicks+12;
        triggerAnim("attacks",switch(attackMode){case 0->"tentacle";case 1->"stomp";case 2->"bite";default->"roar";});
        return true;
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide())return;
        if(attackCooldown>0)attackCooldown--;
        if(attackTicks<=0)return;
        attackTicks--;
        int impactAt=switch(attackMode){case 0,1->20;case 2->15;default->27;};
        if(!damageApplied&&attackTicks<=impactAt){damageApplied=true;applyBossAttack();}
    }
    private void applyBossAttack(){
        LivingEntity target=getTarget();
        if(attackMode==1||attackMode==3){
            double radius=attackMode==1?5D:6D;
            AABB area=getBoundingBox().inflate(radius,1.5D,radius);
            for(LivingEntity other:level().getEntitiesOfClass(LivingEntity.class,area)){
                if(other==this||!other.isAlive()||isAlliedTo(other)||distanceToSqr(other)>radius*radius)continue;
                if(attackMode==1)other.hurt(damageSources().mobAttack(this),12F);
                Vec3 away=other.position().subtract(position());
                double dist=Math.max(0.01D,away.horizontalDistance());
                other.push(away.x/dist*0.9D,0.32D,away.z/dist*0.9D);
            }
        }else if(target!=null&&target.isAlive()){
            double radius=attackMode==0?6.3D:4D;
            if(distanceToSqr(target)<=radius*radius&&hasLineOfSight(target))
                target.hurt(damageSources().mobAttack(this),attackMode==0?16F:21F);
        }
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<>(this,"movement",5,state->{
            if(attackTicks>0)return PlayState.STOP;
            if(state.isMoving()){
                if(getDeltaMovement().horizontalDistanceSqr()>0.09D)return state.setAndContinue(RUN);
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
        c.add(new AnimationController<>(this,"attacks",0,state->PlayState.STOP)
            .triggerableAnim("tentacle",TENTACLE).triggerableAnim("stomp",STOMP)
            .triggerableAnim("bite",BITE).triggerableAnim("roar",ROAR));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
