package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public class CreeperEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeCreeperTick(CallbackInfo ci) {
        if (FreezeServerControl.isActive()) {
            // Cancel creeper’s fuse/swelling/explosion handling for the duration
            ci.cancel();
        }
    }
}
