package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.network.NetworkHandler;
import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.ekical.sotuff.SoTuffConstants.DEFAULT_FREEZE_DURATION_MS;

@Mixin(net.minecraft.entity.LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeMovement(CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }

    @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeBaseTick(CallbackInfo ci) {
        if (FreezeServerControl.isActive()) ci.cancel();
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void soTuff$noDamage(ServerWorld world, DamageSource source, float amount,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (FreezeServerControl.isActive()) cir.setReturnValue(false);
    }
    
    @Inject(method = "damage", at = @At("RETURN"))
    private void soTuff$afterDamage(ServerWorld world, DamageSource source, float amount,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!net.ekical.sotuff.config.SoTuffRuntime.triggerAfterEachAction()) return;
        if (!cir.getReturnValue()) return;
        
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity instanceof ServerPlayerEntity sp) {
            NetworkHandler.startFreezeFor(sp, DEFAULT_FREEZE_DURATION_MS);
        }
    }
}
