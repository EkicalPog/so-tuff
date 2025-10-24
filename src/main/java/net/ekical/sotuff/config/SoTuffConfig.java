package net.ekical.sotuff.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ekical.sotuff.client.MathUtils;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static net.ekical.sotuff.SoTuffConstants.*;

public final class SoTuffConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static volatile SoTuffConfig INSTANCE;
    private static volatile File configFile;

    public boolean triggerAfterEachAction = true;

    public double  frequency01 = 0.5;
    public boolean useCustomWindow = false;
    public int     customMinSec = 60 * 2;
    public int     customMaxSec = 60 * 10;

    public boolean varySoundSpeed = false;
    public float   soundPitchMin  = 0.5f;
    public float   soundPitchMax  = 3.0f;
    public float soundPitchDefault = 1f;

    public String customMediaDir = "";

    public boolean cinematicBars  = false;
    public boolean shakeEnabled = true;

    private SoTuffConfig() {}

    public static SoTuffConfig get() {
        SoTuffConfig result = INSTANCE;
        if (result == null) {
            synchronized (SoTuffConfig.class) {
                result = INSTANCE;
                if (result == null) {
                    INSTANCE = result = load();
                }
            }
        }
        return result;
    }

    public static void save() {
        try {
            File file = getFile();
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            
            SoTuffConfig c = get();
            normalizePitchValues(c);
            
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(c, w);
            }
        } catch (Exception ignored) {}
    }

    private static SoTuffConfig load() {
        try {
            File f = getFile();
            if (f.isFile()) {
                try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                    SoTuffConfig cfg = GSON.fromJson(r, SoTuffConfig.class);
                    if (cfg != null) {
                        normalizePitchValues(cfg);
                        return cfg;
                    }
                }
            }
        } catch (Exception ignored) {}
        return new SoTuffConfig();
    }

    private static File getFile() {
        File result = configFile;
        if (result == null) {
            synchronized (SoTuffConfig.class) {
                result = configFile;
                if (result == null) {
                    File gameDir = MinecraftClient.getInstance().runDirectory;
                    configFile = result = new File(gameDir, "config/so_tuff.json");
                }
            }
        }
        return result;
    }

    private static void normalizePitchValues(SoTuffConfig cfg) {
        cfg.soundPitchMin = MathUtils.clampFloat(cfg.soundPitchMin, PITCH_MIN, PITCH_MAX);
        cfg.soundPitchMax = MathUtils.clampFloat(cfg.soundPitchMax, PITCH_MIN, PITCH_MAX);
        if (cfg.soundPitchMax < cfg.soundPitchMin) {
            float t = cfg.soundPitchMin;
            cfg.soundPitchMin = cfg.soundPitchMax;
            cfg.soundPitchMax = t;
        }
    }
}
