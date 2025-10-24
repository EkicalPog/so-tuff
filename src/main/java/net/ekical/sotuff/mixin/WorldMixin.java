package net.ekical.sotuff.mixin;

import net.ekical.sotuff.client.FreezeTickControl;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class WorldMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void soTuff$cancelWorldTick(CallbackInfo ci) {
        if (FreezeTickControl.isActive()) ci.cancel();
    }

    @Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true)
    private void soTuff$cancelEntityTick(CallbackInfo ci) {
        if (FreezeTickControl.isActive()) ci.cancel();
    }
}
