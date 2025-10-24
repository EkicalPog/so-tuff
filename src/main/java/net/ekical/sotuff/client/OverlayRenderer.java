package net.ekical.sotuff.client;

import net.ekical.sotuff.config.SoTuffConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.util.*;

public final class OverlayRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final List<Identifier> SKULLS = new ArrayList<>();
    private static final Random RNG = new Random();
    private static int forcedIndex = -1;

    private static Identifier lastPick = null;
    private static long lastPickFrame = -1;

    private static final double SHAKE_DURATION_S = 1.0;   // total length
    private static final double SHAKE_FREQ_HZ    = 15.0;  // wobble speed
    private static final double SHAKE_AMP_X_PX   = 60.0;  // horizontal amplitude
    private static final double SHAKE_AMP_Y_PX   = 6.0;   // subtle vertical amplitude

    private static volatile boolean shaking = false;
    private static volatile long    shakeStartNanos = -1L;

    private OverlayRenderer() {}

    public static void triggerShake() {
        if (!net.ekical.sotuff.config.SoTuffConfig.get().shakeEnabled) return; // <- respect toggle
        if (!shaking) {
            shaking = true;
            shakeStartNanos = System.nanoTime();
        }
    }

    public static void forceRestartShake() {
        if (!net.ekical.sotuff.config.SoTuffConfig.get().shakeEnabled) return; // <- respect toggle
        shaking = true;
        shakeStartNanos = System.nanoTime();
    }

    public static synchronized void reloadUserImages() {
        SKULLS.clear();
        forcedIndex = -1;
        lastPick = null;
        lastPickFrame = -1;
        loadSkullResources();
    }

    public static void renderCinematicBars(DrawContext ctx) {
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int vw = (int)Math.round(h * 9.0 / 16.0);
        final int BLACK = 0xFF000000;

        if (vw <= w) {
            int leftPad  = (w - vw) / 2;
            int rightPad = w - (leftPad + vw);
            if (leftPad > 0)  ctx.fill(0, 0, leftPad, h, BLACK);
            if (rightPad > 0) ctx.fill(w - rightPad, 0, w, h, BLACK);
        } else {
            int vhFitted = (int)Math.round(w * 16.0 / 9.0);
            int topPad    = (h - vhFitted) / 2;
            int bottomPad = h - (topPad + vhFitted);
            if (topPad > 0)    ctx.fill(0, 0, w, topPad, BLACK);
            if (bottomPad > 0) ctx.fill(0, h - bottomPad, w, h, BLACK);
        }
    }

    public static void render(DrawContext ctx) {
        ensureSkullsLoaded();
        if (SKULLS.isEmpty()) return;

        Identifier textureId = pickCurrent();
        int fullW = ctx.getScaledWindowWidth();
        int fullH = ctx.getScaledWindowHeight();

        int texW = 512, texH = 512;
        double scale  = Math.min((double) fullW / texW, (double) fullH / texH) * 0.30;
        int centerX   = fullW >> 1;
        int centerY   = fullH >> 1;
        double yShift = fullH * 0.25;

        double shakeX = 0.0, shakeY = 0.0;
        if (SoTuffConfig.get().shakeEnabled && shaking) {
            double elapsed = (System.nanoTime() - shakeStartNanos) / 1_000_000_000.0;
            if (elapsed < SHAKE_DURATION_S) {
                double t = elapsed / SHAKE_DURATION_S;
                double decay = (1.0 - t); decay *= decay;
                double phase = elapsed * SHAKE_FREQ_HZ * 2 * Math.PI;
                shakeX = Math.sin(phase) * SHAKE_AMP_X_PX * decay;
                shakeY = Math.sin(phase + Math.PI / 2.0) * SHAKE_AMP_Y_PX * decay;
            } else {
                shaking = false;
            }
        }

        double offsetX = centerX - (texW * scale) / 2.0 + shakeX;
        double offsetY = centerY - (texH * scale) / 2.0 + yShift + shakeY;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(offsetX, offsetY, 0);
        ctx.getMatrices().scale((float) scale, (float) scale, 1f);
        ctx.drawTexture(RenderLayer::getGuiTextured, textureId, 0, 0, 0, 0, texW, texH, texW, texH);
        ctx.getMatrices().pop();
    }

    public static void setSyncedSkullIndex(int idx) {
        ensureSkullsLoaded();
        if (SKULLS.isEmpty()) { forcedIndex = -1; return; }
        forcedIndex = Math.floorMod(idx, SKULLS.size());
        lastPick = null;
        lastPickFrame = -1;
    }

    private static Identifier pickCurrent() {
        if (forcedIndex >= 0 && forcedIndex < SKULLS.size()) return SKULLS.get(forcedIndex);
        long frame = MC.world != null ? MC.world.getTime() : 0;
        if (lastPick != null && frame == lastPickFrame) return lastPick;
        lastPick = SKULLS.get(RNG.nextInt(SKULLS.size()));
        lastPickFrame = frame;
        return lastPick;
    }

    private static void ensureSkullsLoaded() {
        if (SKULLS.isEmpty()) {
            loadSkullResources();
        }
    }

    private static void loadSkullResources() {
        try {
            Map<Identifier, Resource> imgs = MC.getResourceManager().findResources(
                    "textures/skulls",
                    id -> "so-tuff".equals(id.getNamespace()) && id.getPath().endsWith(".png")
            );
            var ids = new ArrayList<>(imgs.keySet());
            ids.sort(Comparator.comparing(Identifier::toString));
            SKULLS.addAll(ids);
        } catch (Throwable ignored) {
        }
    }
}
