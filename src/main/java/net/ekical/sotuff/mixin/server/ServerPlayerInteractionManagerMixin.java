package net.ekical.sotuff.mixin.server;

import net.ekical.sotuff.config.SoTuffRuntime;
import net.ekical.sotuff.network.NetworkHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.ekical.sotuff.SoTuffConstants.DEFAULT_FREEZE_DURATION_MS;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void soTuff$afterInteractBlock(ServerPlayerEntity player,
                                           World world,
                                           ItemStack stack,
                                           Hand hand,
                                           BlockHitResult hit,
                                           CallbackInfoReturnable<ActionResult> cir) {
        if (!SoTuffRuntime.triggerAfterEachAction()) return;

        ActionResult result = cir.getReturnValue();
        if (!result.isAccepted()) return;
        
        if (player.currentScreenHandler != player.playerScreenHandler) {
            return;
        }
        
        if (!(stack.getItem() instanceof BlockItem)) return;
        if (!(stack.getItem() instanceof BlockItem)) return; // not a block placement

        NetworkHandler.startFreezeFor(player, DEFAULT_FREEZE_DURATION_MS);
    }
    
    @Inject(method = "interactItem", at = @At("RETURN"))
    private void soTuff$afterInteractItem(ServerPlayerEntity player,
                                          World world,
                                          ItemStack stack,
                                          Hand hand,
                                          CallbackInfoReturnable<ActionResult> cir) {
        if (!SoTuffRuntime.triggerAfterEachAction()) return;
        
        ActionResult result = cir.getReturnValue();
        if (!result.isAccepted()) return;
        if (!(stack.getItem() instanceof BucketItem)) return;
        
        NetworkHandler.startFreezeFor(player, DEFAULT_FREEZE_DURATION_MS);
    }
}
