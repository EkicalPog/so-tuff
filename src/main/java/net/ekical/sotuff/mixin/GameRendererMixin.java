package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.ekical.sotuff.client.MonochromeRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At("TAIL"))
    private void soTuff$drawOverlayLast(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!FreezeTickControl.isActive()) {
            return;
        }

        final MinecraftClient client = MinecraftClient.getInstance();
        final VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
        final DrawContext drawContext = new DrawContext(client, vertexConsumers);

        MonochromeRenderer.renderHud(drawContext);
        vertexConsumers.draw();
    }
}
