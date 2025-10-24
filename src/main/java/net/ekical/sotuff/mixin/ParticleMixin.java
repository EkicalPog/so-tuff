package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void soTuff$cancelParticleTick(CallbackInfo ci) {
        if (FreezeTickControl.isActive()) ci.cancel();
    }
}
