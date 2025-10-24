package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public abstract class FluidMixin {
    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeFlow(ServerWorld world, BlockPos pos, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (FreezeServerControl.isActive()) {
            world.scheduleFluidTick(pos, fluidState.getFluid(), 1);
            ci.cancel();
        }
    }
}
