package dev.chatgpt.abysstendril;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class AbyssBossModel extends GeoModel<AbyssBossEntity> {
    @Override public ResourceLocation getModelResource(AbyssBossEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(AbyssTendrilMod.MODID,"geo/abyss_tendril_beast.geo.json");
    }
    @Override public ResourceLocation getTextureResource(AbyssBossEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(AbyssTendrilMod.MODID,"textures/entity/abyss_tendril_beast.png");
    }
    @Override public ResourceLocation getAnimationResource(AbyssBossEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(AbyssTendrilMod.MODID,"animations/abyss_tendril_beast.animation.json");
    }
}
