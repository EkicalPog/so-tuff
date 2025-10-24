package net.ekical.sotuff.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.ekical.sotuff.SoTuffConstants.MOD_ID;

public final class GeneratedSounds {
    private static final Random RAND = new Random();

    private static final List<Identifier> DEFAULT_KEYS = List.of(
            Identifier.of(MOD_ID, "phonk/ef1"),
            Identifier.of(MOD_ID, "phonk/ef2"),
            Identifier.of(MOD_ID, "phonk/ef3"),
            Identifier.of(MOD_ID, "phonk/ef4"),
            Identifier.of(MOD_ID, "phonk/ef5"),
            Identifier.of(MOD_ID, "phonk/ef6"),
            Identifier.of(MOD_ID, "phonk/ef7"),
            Identifier.of(MOD_ID, "phonk/ef8"),
            Identifier.of(MOD_ID, "phonk/ef9")
    );

    private static final Set<Identifier> DISCOVERED = new LinkedHashSet<>();
    private static List<Identifier> cachedWeightedPool = null;
    private static boolean triedDiscover = false;

    private GeneratedSounds() {}

    private static void discoverNow() {
        DISCOVERED.clear();
        cachedWeightedPool = null;
        triedDiscover = true;

        try {
            var rm = MinecraftClient.getInstance().getResourceManager();
            var rid = Identifier.of(MOD_ID, "sounds.json");

            var all = rm.getAllResources(rid);
            if (all.isEmpty()) {
                System.out.println("[SoTuff] GeneratedSounds: no layered sounds.json found for " + rid);
            }

            for (var res : all) {
                try (var in = res.getInputStream();
                     var rd = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(rd).getAsJsonObject();
                    for (var e : root.entrySet()) {
                        DISCOVERED.add(Identifier.of(MOD_ID, e.getKey()));
                    }
                }
            }

            System.out.println("[SoTuff] GeneratedSounds: discovered " + DISCOVERED.size() + " keys from layered sounds.json");
        } catch (Throwable t) {
            System.out.println("[SoTuff] GeneratedSounds: discover failed: " + t);
        }

        DISCOVERED.addAll(DEFAULT_KEYS);
    }

    public static SoundEvent getRandom() {
        if (!triedDiscover) {
            discoverNow();
        }

        if (cachedWeightedPool == null) {
            cachedWeightedPool = buildWeightedPool();
        }

        if (cachedWeightedPool.isEmpty()) {
            System.out.println("[SoTuff] GeneratedSounds: empty pool, using defaults");
            return SoundEvent.of(DEFAULT_KEYS.get(RAND.nextInt(DEFAULT_KEYS.size())));
        }

        Identifier pickId = cachedWeightedPool.get(RAND.nextInt(cachedWeightedPool.size()));
        System.out.println("[SoTuff] pick sound = " + pickId + " (weighted pool size=" + cachedWeightedPool.size() + ")");
        return SoundEvent.of(pickId);
    }

    private static List<Identifier> buildWeightedPool() {
        Set<Identifier> pool = new LinkedHashSet<>(DISCOVERED);
        
        if (pool.isEmpty()) {
            for (var e : Registries.SOUND_EVENT.getEntrySet()) {
                Identifier id = e.getKey().getValue();
                if (id != null && MOD_ID.equals(id.getNamespace())) {
                    pool.add(id);
                }
            }
            if (pool.isEmpty()) {
                pool.addAll(DEFAULT_KEYS);
            }
            System.out.println("[SoTuff] GeneratedSounds: built fallback pool size=" + pool.size());
        }

        List<Identifier> weighted = new ArrayList<>(pool.size() * 2);
        for (Identifier id : pool) {
            weighted.add(id);
            if (id.getPath().startsWith("custom/")) {
                weighted.add(id);
            }
        }
        return weighted;
    }

    public static void registerDefaults() {
        for (Identifier id : DEFAULT_KEYS) {
            if (!Registries.SOUND_EVENT.containsId(id)) {
                Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
            }
        }
    }

    public static void logSoTuffSounds() {
        System.out.println("[SoTuff] registry dump (sounds in so-tuff namespace):");
        for (var id : Registries.SOUND_EVENT.getIds()) {
            if (MOD_ID.equals(id.getNamespace())) {
                System.out.println("  - " + id);
            }
        }
    }
}
