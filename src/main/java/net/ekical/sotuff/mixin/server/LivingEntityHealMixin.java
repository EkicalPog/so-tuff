// src/main/java/net/ekical/sotuff/mixin/server/LivingEntityHealMixin.java
package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityHealMixin {

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void soTuff$pauseHeal(float amount, CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }
}
