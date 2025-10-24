package net.ekical.sotuff.client;

import net.ekical.sotuff.config.SoTuffConfig;
import net.ekical.sotuff.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

import static net.ekical.sotuff.SoTuffConstants.*;

public final class SoTuffClient implements ClientModInitializer {
    private static volatile long dueManualTriggerAtMs = 0L;
    private static int dueManualDurationMs = DEFAULT_FREEZE_DURATION_MS;

    private static void enqueueManualTrigger(int durationMs, long delayMs) {
        long now = System.currentTimeMillis();
        dueManualDurationMs  = durationMs;
        dueManualTriggerAtMs = now + Math.max(0, delayMs);
    }

    @Override
    public void onInitializeClient() {
        NetworkHandler.registerClientReceivers();

        SoTuffKeybinds.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            var dir = SoTuffConfig.get().customMediaDir;
            if (dir != null && !dir.isBlank()) {
                client.execute(() -> {
                    UserMedia.setRootAndRefresh(dir);
                    OverlayRenderer.reloadUserImages();
                });
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> 
            NetworkHandler.sendClientPrefsFromConfig()
        );

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && player == MinecraftClient.getInstance().player
                    && SoTuffConfig.get().triggerAfterEachAction) {
                enqueueManualTrigger(DEFAULT_FREEZE_DURATION_MS, 75);
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            if (FreezeTickControl.isActive()) {
                MonochromeRenderer.renderHud(ctx);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MonochromeRenderer.tick();

            long now = System.currentTimeMillis();
            if (dueManualTriggerAtMs > 0 && now >= dueManualTriggerAtMs) {
                dueManualTriggerAtMs = 0;
                NetworkHandler.sendFreezeTrigger(dueManualDurationMs);
            }

            if (client.isPaused() || client.world == null || client.player == null) return;

            if (!SoTuffConfig.get().triggerAfterEachAction) {
                AutoScheduler.tick();
            }
        });
    }
}
