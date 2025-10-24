package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.server.FreezeServerControl;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public class FallingBlockMixin {
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void soTuff$freezeNewFalls(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (FreezeServerControl.isActive()) {
            world.scheduleBlockTick(pos, state.getBlock(), 1);
            ci.cancel();
        }
    }
}
