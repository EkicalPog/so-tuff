package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public final class MinecraftMixin {
    
    @Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
    private void soTuff$cancelInput(CallbackInfo ci) {
        if (FreezeTickControl.isActive()) {
            ci.cancel();
        }
    }
}
