package net.ekical.sotuff.server;

import net.ekical.sotuff.client.MathUtils;
import net.ekical.sotuff.network.payload.EffectivePrefsS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public final class PrefAggregator {
    private static final class Pref {
        boolean perAction, vary, bars;
        double freq01;
        float pitchMin, pitchMax, pitchDefault;
    }

    private static Pref controller = null; // authoritative settings

    private PrefAggregator(){}

    public static void setControllerPrefs(boolean perAction, double freq01, boolean vary, boolean bars,
                                          float pitchMin, float pitchMax, float pitchDefault) {
        Pref pr = new Pref();
        pr.perAction = perAction;
        pr.freq01 = MathUtils.clamp01(freq01);
        pr.vary = vary;
        pr.bars = bars;
        pr.pitchMin = pitchMin;
        pr.pitchMax = pitchMax;
        pr.pitchDefault = pitchDefault;
        controller = pr;
    }

    public static EffectivePrefsS2CPayload computeEffective() {
        if (controller == null) {
            return new EffectivePrefsS2CPayload(false, 0.5, true, true, 1.0f, 1.0f, 1.0f);
        }
        return new EffectivePrefsS2CPayload(
                controller.perAction,
                controller.freq01,
                controller.vary,
                controller.bars,
                controller.pitchMin,
                controller.pitchMax,
                controller.pitchDefault
        );
    }

    public static void broadcastToAll(Iterable<ServerPlayerEntity> players) {
        EffectivePrefsS2CPayload eff = computeEffective();
        for (ServerPlayerEntity sp : players) {
            ServerPlayNetworking.send(sp, eff);
        }
    }

    public static boolean controllerVary() {
        return controller != null && controller.vary;
    }
    public static float controllerPitchMin() {
        return controller != null ? controller.pitchMin : 1.0f;
    }
    public static float controllerPitchMax() {
        return controller != null ? controller.pitchMax : 1.0f;
    }
    public static float controllerPitchDefault() {
        return controller != null ? controller.pitchDefault : 1.0f;
    }
}
