package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    
    @Shadow 
    private double cursorDeltaX;
    
    @Shadow 
    private double cursorDeltaY;

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void soTuff$eatCursorMove(long window, double x, double y, CallbackInfo ci) {
        if (FreezeTickControl.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void soTuff$noRotate(CallbackInfo ci) {
        if (FreezeTickControl.isActive()) {
            this.cursorDeltaX = 0.0;
            this.cursorDeltaY = 0.0;
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void soTuff$noScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (FreezeTickControl.isActive()) {
            ci.cancel();
        }
    }
}
