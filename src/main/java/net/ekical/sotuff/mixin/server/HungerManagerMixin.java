// src/main/java/net/ekical/sotuff/mixin/server/HungerManagerMixin.java
package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void soTuff$pauseHunger(ServerPlayerEntity player, CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }
}
