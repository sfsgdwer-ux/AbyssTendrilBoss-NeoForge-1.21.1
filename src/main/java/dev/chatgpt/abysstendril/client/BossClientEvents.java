package dev.chatgpt.abysstendril.client;
import dev.chatgpt.abysstendril.AbyssBossModel;
import dev.chatgpt.abysstendril.AbyssTendrilMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
@EventBusSubscriber(modid=AbyssTendrilMod.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BossClientEvents {
    @SubscribeEvent public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AbyssTendrilMod.BOSS.get(),context -> new GeoEntityRenderer<>(context,new AbyssBossModel()));
    }
}
