package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.AreaEffectCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloudEntity.class)
public class AreaEffectCloudMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeAEC(CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }
}