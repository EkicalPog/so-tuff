package net.ekical.sotuff.client;

import com.mojang.blaze3d.systems.RenderSystem;
import static net.ekical.sotuff.SoTuffConstants.*;
import net.ekical.sotuff.config.SoTuffConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.gui.screen.GameMenuScreen;

public final class MonochromeRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final int GRAY_R = 76;  
    private static final int GRAY_G = 150; 
    private static final int GRAY_B = 29;  

    private static NativeImageBackedTexture frozenTex;
    private static Identifier frozenId;
    private static int texW, texH;

    private static final long DEBOUNCE_MS = 200;
    private static long lastEnqueueAt = 0L;

    private static boolean pending = false;
    private static long pendingReadyAt = 0L;
    private static int pendingDurationMs = 0;
    private static int pendingSkullIndex = -1;
    private static float pendingPitch = 1.0f;
    private static String pendingSoundId = null;

    private static int activeSkullIndex = -1;
    private static SoundInstance currentSound = null;
    private static float currentPitch = 1.0f;
    private static Identifier currentSoundId = null;

    private MonochromeRenderer() {}

    public static void start(int durationMs, int skullIndex) {
        start(durationMs, skullIndex, calculatePitch(), null);
    }
    
    public static void start(int durationMs, int skullIndex, float pitch, String soundId) {
        long now = System.currentTimeMillis();
        if (now - lastEnqueueAt < 50) return;
        lastEnqueueAt = now;

        pending = true;
        pendingReadyAt = now + DEBOUNCE_MS;
        pendingDurationMs = durationMs;
        pendingSkullIndex = skullIndex;
        pendingPitch = pitch;
        pendingSoundId = soundId;
    }

    public static void tick() {
        if (FreezeTickControl.isActive()) {
            enforceNoPause();
        }

        long now = System.currentTimeMillis();
        if (pending && now >= pendingReadyAt) {
            pending = false;

            activeSkullIndex = pendingSkullIndex;
            currentPitch = pendingPitch;
            
            // Adjust duration based on pitch: faster sound = shorter duration
            int adjustedDurationMs = (int) (pendingDurationMs / currentPitch);
            FreezeTickControl.activate(adjustedDurationMs);

            OverlayRenderer.setSyncedSkullIndex(activeSkullIndex);
            OverlayRenderer.forceRestartShake();

            clearFrozenTexture();
            captureAndUploadGrayscale();

            SoundEvent evt;
            if (pendingSoundId != null && !pendingSoundId.isEmpty()) {
                evt = SoundEvent.of(Identifier.of(pendingSoundId));
            } else {
                evt = net.ekical.sotuff.utils.GeneratedSounds.getRandom();
            }
            
            if (evt != null) {
                stopCurrentSound(); 
                playEffectSound(evt, currentPitch);
            }
        }
        if (!FreezeTickControl.isActive()) {
            stopCurrentSound();
            clearFrozenTexture();
        }
    }

    private static void enforceNoPause() {
        if (MC.isPaused() || MC.currentScreen instanceof GameMenuScreen) {
            MC.setScreen(null);
        }
    }

    private static void stopCurrentSound() {
        if (currentSound != null) {
            try { MC.getSoundManager().stop(currentSound); } catch (Throwable ignored) {}
            currentSound = null;
        }
    }

    public static void updatePitch(float newPitch) {
        currentPitch = newPitch;
        if (currentSoundId != null && FreezeTickControl.isActive()) {
            stopCurrentSound();
            playEffectSound(SoundEvent.of(currentSoundId), currentPitch);
        }
    }

    private static void playEffectSound(SoundEvent evt, float pitch) {
        currentSoundId = evt.id();
        currentSound = new PositionedSoundInstance(
                currentSoundId,
                SoundCategory.MASTER,
                1.0f,
                pitch,
                Random.create(),
                false,
                0,
                SoundInstance.AttenuationType.NONE,
                0.0, 0.0, 0.0,
                true
        );
        MC.getSoundManager().play(currentSound);
    }

    public static void renderHud(DrawContext ctx) {
        if (!FreezeTickControl.isActive()) return;

        if (MC.currentScreen == null && frozenId != null) {
            int w = ctx.getScaledWindowWidth();
            int h = ctx.getScaledWindowHeight();

            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            ctx.getMatrices().push();
            ctx.drawTexture(RenderLayer::getGuiTextured, frozenId, 0, 0, 0, 0, w, h, w, h);
            ctx.getMatrices().pop();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
        }

        OverlayRenderer.setSyncedSkullIndex(activeSkullIndex);
        if (SoTuffConfig.get().cinematicBars) {
            OverlayRenderer.renderCinematicBars(ctx);
        }
        OverlayRenderer.render(ctx);
    }

    private static void captureAndUploadGrayscale() {
        try {
            NativeImage img = grabFramebufferImage();
            if (img == null) return;

            toGrayscale(img);

            clearFrozenTexture();
            frozenTex = new NativeImageBackedTexture(img);
            frozenId  = Identifier.of("so-tuff", "frozen_" + System.nanoTime());
            MC.getTextureManager().registerTexture(frozenId, frozenTex);
            texW = img.getWidth();
            texH = img.getHeight();
        } catch (Throwable ignored) {
        }
    }

    private static NativeImage grabFramebufferImage() {
        Framebuffer fb = MC.getFramebuffer();
        return fb != null ? ScreenshotRecorder.takeScreenshot(fb) : null;
    }

    private static void toGrayscale(NativeImage img) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getColorArgb(x, y);
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8)  & 0xFF;
                int b =  argb         & 0xFF;
                int gray = (r * GRAY_R + g * GRAY_G + b * GRAY_B) >>> 8;
                img.setColorArgb(x, y, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
            }
        }
    }

    private static void clearFrozenTexture() {
        if (frozenTex != null) {
            try { frozenTex.close(); } catch (Exception ignored) {}
            frozenTex = null;
        }
        frozenId = null;
        texW = texH = 0;
    }
    
    private static float calculatePitch() {
        SoTuffConfig cfg = SoTuffConfig.get();
        float lo = MathUtils.clampFloat(cfg.soundPitchMin, PITCH_MIN, PITCH_MAX);
        float hi = MathUtils.clampFloat(cfg.soundPitchMax, PITCH_MIN, PITCH_MAX);
        if (hi < lo) {
            float temp = lo;
            lo = hi;
            hi = temp;
        }
        
        if (cfg.varySoundSpeed && hi > lo) {
            return lo + (float)Math.random() * (hi - lo);
        }
        return MathUtils.clampFloat(cfg.soundPitchDefault, PITCH_MIN, PITCH_MAX);
    }
}
