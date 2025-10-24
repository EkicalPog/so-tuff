package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityMixin {
    @Inject(method = "tickNewAi", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeAi(CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }
}
