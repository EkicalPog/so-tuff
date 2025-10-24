package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudHotBarMixin {
    @Inject(
            method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void soTuff$hideHotbar(DrawContext ctx, RenderTickCounter rtc, CallbackInfo ci) {
        if (FreezeTickControl.isActive()) ci.cancel();
    }
}
