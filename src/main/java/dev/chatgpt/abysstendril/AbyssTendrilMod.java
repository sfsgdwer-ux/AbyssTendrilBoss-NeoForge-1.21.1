package dev.chatgpt.abysstendril;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AbyssTendrilMod.MODID)
public class AbyssTendrilMod {
    public static final String MODID = "abysstendril";

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssBossEntity>> BOSS =
        ENTITIES.register("abyss_tendril_beast", () ->
            EntityType.Builder.of(AbyssBossEntity::new, MobCategory.MONSTER)
                .sized(2.8F, 5.3F).clientTrackingRange(24)
                .build("abysstendril:abyss_tendril_beast"));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredHolder<Item, DeferredSpawnEggItem> BOSS_EGG =
        ITEMS.register("abyss_tendril_spawn_egg",
            () -> new DeferredSpawnEggItem(BOSS, 0x141519, 0x737780, new Item.Properties()));

    // A dedicated egg-icon tab in the Creative inventory (press E in Creative mode).
    // Resolve Death Knight's egg from its registry ID, keeping the other mod independent.
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NAZARICK_EGGS =
        CREATIVE_TABS.register("nazarick_eggs", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.abysstendril.nazarick_eggs"))
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                .icon(() -> new ItemStack(BOSS_EGG.get()))
                .displayItems((params, output) -> {
                    output.accept(BOSS_EGG.get());
                    Item deathKnightEgg = BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("deathknight", "death_knight_spawn_egg"));
                    if (deathKnightEgg != Items.AIR) {
                        output.accept(deathKnightEgg);
                    }
                }).build());

    public AbyssTendrilMod(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        modBus.addListener(this::attributes);
        modBus.addListener(this::creativeTab);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(BOSS.get(), AbyssBossEntity.createBossAttributes().build());
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BOSS_EGG.get());
        }
    }
}
