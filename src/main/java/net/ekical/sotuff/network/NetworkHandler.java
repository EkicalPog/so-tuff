package net.ekical.sotuff.network;

import net.ekical.sotuff.client.MonochromeRenderer;
import net.ekical.sotuff.client.OverlayRenderer;
import net.ekical.sotuff.network.payload.ClientPrefsC2SPayload;
import net.ekical.sotuff.network.payload.EffectivePrefsS2CPayload;
import net.ekical.sotuff.network.payload.FreezeRequestC2SPayload;
import net.ekical.sotuff.network.payload.FreezeStartS2CPayload;
import net.ekical.sotuff.network.payload.FreezeUpdatePitchS2CPayload;
import net.ekical.sotuff.server.FreezeServerControl;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.ekical.sotuff.SoTuffConstants.*;

public final class NetworkHandler {
    private NetworkHandler() {}

    private static final Deque<QueuedFreeze> FREEZE_QUEUE = new ArrayDeque<>(8);
    private static boolean lastFreezeActive = false;
    private static long lastTriggerTime = 0L;
    private static final long TRIGGER_COOLDOWN_MS = 200;
    private static UUID lastKilledEntityUUID = null;
    private static long lastKillTime = 0L;
    
    private static final class QueuedFreeze {
        final ServerWorld world; 
        final int durationMs;
        
        QueuedFreeze(ServerWorld world, int durationMs) { 
            this.world = world; 
            this.durationMs = durationMs; 
        }
    }

    public static void registerPayloadTypes() {
        // C2S
        PayloadTypeRegistry.playC2S().register(FreezeRequestC2SPayload.ID, FreezeRequestC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientPrefsC2SPayload.ID, ClientPrefsC2SPayload.CODEC);

        // S2C
        PayloadTypeRegistry.playS2C().register(FreezeStartS2CPayload.ID,  FreezeStartS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EffectivePrefsS2CPayload.ID, EffectivePrefsS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FreezeUpdatePitchS2CPayload.ID, FreezeUpdatePitchS2CPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ClientPrefsC2SPayload.ID, (payload, ctx) -> {
            ctx.player().getServer().execute(() -> {
                ServerPlayerEntity p = ctx.player();
                // Only ops can configure the pitch
                if (p.hasPermissionLevel(2) || !p.getServer().isDedicated()) {
                    net.ekical.sotuff.server.PrefAggregator.setControllerPrefs(
                            payload.perAction(), payload.freq01(), payload.vary(), payload.bars(),
                            clampPitch(payload.pitchMin()), clampPitch(payload.pitchMax()), clampPitch(payload.pitchDefault())
                    );
                    var players = p.getServerWorld().getPlayers();
                    net.ekical.sotuff.server.PrefAggregator.broadcastToAll(players);

                    // Live update pitch
                    if (net.ekical.sotuff.server.FreezeServerControl.isActive()) {
                        float newPitch = calculatePitch();
                        FreezeUpdatePitchS2CPayload upd = new FreezeUpdatePitchS2CPayload(newPitch);
                        for (ServerPlayerEntity sp2 : players) {
                            ServerPlayNetworking.send(sp2, upd);
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FreezeRequestC2SPayload.ID, (payload, context) -> 
            context.player().getServer().execute(() -> 
                startFreezeFor(context.player(), payload.durationMs())
            )
        );
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EffectivePrefsS2CPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> net.ekical.sotuff.config.SoTuffRuntime.applyFromServer(
                            payload.perAction(), payload.freq01(), payload.vary(), payload.bars(),
                            payload.pitchMin(), payload.pitchMax(), payload.pitchDefault()
                ))
        );

        ClientPlayNetworking.registerGlobalReceiver(FreezeStartS2CPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    OverlayRenderer.setSyncedSkullIndex(payload.skullIndex());
                    MonochromeRenderer.start(payload.durationMs(), payload.skullIndex(), payload.pitch(), payload.soundId());
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(FreezeUpdatePitchS2CPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> MonochromeRenderer.updatePitch(payload.pitch()))
        );
    }

    public static void sendFreezeTrigger(int durationMs) {
        if (ClientPlayNetworking.canSend(FreezeRequestC2SPayload.ID)) {
            ClientPlayNetworking.send(new FreezeRequestC2SPayload(durationMs));
        } else {
            OverlayRenderer.setSyncedSkullIndex(ThreadLocalRandom.current().nextInt(0, SKULL_INDEX_RANGE));
            MonochromeRenderer.start(durationMs, 0, 1.0f, null);
        }
    }

    public static void registerServerEventTriggers() {
        PlayerBlockBreakEvents.AFTER.register((World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) -> {
            if (!net.ekical.sotuff.config.SoTuffRuntime.triggerAfterEachAction()) return;
            if (player instanceof ServerPlayerEntity sp) startFreezeFor(sp, DEFAULT_FREEZE_DURATION_MS);
        });


        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean active = FreezeServerControl.isActive();
            if (!active && lastFreezeActive && !FREEZE_QUEUE.isEmpty()) {
                QueuedFreeze q = FREEZE_QUEUE.pollFirst();
                if (q != null) startGlobalFreeze(q.world, q.durationMs);
            }
            lastFreezeActive = active;
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                (ServerWorld world, net.minecraft.entity.Entity killer, net.minecraft.entity.LivingEntity victim) -> {
                    if (!net.ekical.sotuff.config.SoTuffRuntime.triggerAfterEachAction()) return;
                    
                    long now = System.currentTimeMillis();
                    UUID victimUUID = victim.getUuid();
                    if (victimUUID.equals(lastKilledEntityUUID) && (now - lastKillTime) < 500) {
                        return;
                    }
                    lastKilledEntityUUID = victimUUID;
                    lastKillTime = now;
                    
                    if (killer instanceof ServerPlayerEntity sp) {
                        startFreezeFor(sp, DEFAULT_FREEZE_DURATION_MS);
                    } else if (victim instanceof ServerPlayerEntity dead) {
                        startFreezeFor(dead, DEFAULT_FREEZE_DURATION_MS);
                    }
                }
        );
    }

    public static void startFreezeFor(ServerPlayerEntity sp, int durationMs) {
        ServerWorld sw = sp.getServerWorld();
        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < TRIGGER_COOLDOWN_MS) {
            return;
        }
        lastTriggerTime = now;
        
        if (FreezeServerControl.isActive()) {
            FREEZE_QUEUE.addLast(new QueuedFreeze(sw, durationMs));
            while (FREEZE_QUEUE.size() > 64) FREEZE_QUEUE.pollFirst();
        } else {
            startGlobalFreeze(sw, durationMs);
        }
    }

    private static void startGlobalFreeze(ServerWorld sw, int durationMs) {
        float pitch = calculatePitch();
        String soundId = pickRandomSound();
        int adjustedDurationMs = (int) (durationMs / pitch);
        FreezeServerControl.activate(adjustedDurationMs);
        int skull = ThreadLocalRandom.current().nextInt(0, SKULL_INDEX_RANGE);
        FreezeStartS2CPayload msg = new FreezeStartS2CPayload(durationMs, skull, pitch, soundId);
        for (ServerPlayerEntity other : sw.getPlayers()) {
            ServerPlayNetworking.send(other, msg);
        }
    }

    public static void sendClientPrefsFromConfig() {
        if (!ClientPlayNetworking.canSend(ClientPrefsC2SPayload.ID)) return;
        
        var c = net.ekical.sotuff.config.SoTuffConfig.get();
        ClientPlayNetworking.send(new ClientPrefsC2SPayload(
                c.triggerAfterEachAction, c.frequency01, c.varySoundSpeed, c.cinematicBars,
                c.soundPitchMin, c.soundPitchMax, c.soundPitchDefault
        ));
    }
    
    private static float calculatePitch() {
        if (!net.ekical.sotuff.server.PrefAggregator.controllerVary()) {
            return 1.0f;
        }
        float lo = Math.max(PITCH_MIN, Math.min(PITCH_MAX, net.ekical.sotuff.server.PrefAggregator.controllerPitchMin()));
        float hi = Math.max(PITCH_MIN, Math.min(PITCH_MAX, net.ekical.sotuff.server.PrefAggregator.controllerPitchMax()));
        if (hi < lo) {
            float t = lo; lo = hi; hi = t;
        }
        if (hi > lo) {
            return lo + ThreadLocalRandom.current().nextFloat() * (hi - lo);
        }
        float def = Math.max(PITCH_MIN, Math.min(PITCH_MAX, net.ekical.sotuff.server.PrefAggregator.controllerPitchDefault()));
        return def;
    }
    
    private static final List<String> DEFAULT_SOUNDS = Collections.unmodifiableList(List.of(
            "so-tuff:phonk/ef1",
            "so-tuff:phonk/ef2",
            "so-tuff:phonk/ef3",
            "so-tuff:phonk/ef4",
            "so-tuff:phonk/ef5",
            "so-tuff:phonk/ef6",
            "so-tuff:phonk/ef7",
            "so-tuff:phonk/ef8",
            "so-tuff:phonk/ef9"
    ));
    
    private static String pickRandomSound() {
        return DEFAULT_SOUNDS.get(ThreadLocalRandom.current().nextInt(DEFAULT_SOUNDS.size()));
    }

    private static float clampPitch(float p) {
        return Math.max(PITCH_MIN, Math.min(PITCH_MAX, p));
    }
}
