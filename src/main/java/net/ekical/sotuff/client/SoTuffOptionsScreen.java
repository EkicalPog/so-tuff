package net.ekical.sotuff.client;

import net.ekical.sotuff.config.SoTuffConfig;
import net.ekical.sotuff.network.NetworkHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SoTuffOptionsScreen extends Screen {
    private final Screen parent;

    private ButtonWidget afterEachActionBtn;
    private ButtonWidget useCustomBtn;

    private FrequencySlider  freqSlider;
    private IntSecondsSlider minSlider;
    private IntSecondsSlider maxSlider;

    private ButtonWidget varySoundBtn;
    private PitchSlider  pitchMinSlider;
    private PitchSlider  pitchMaxSlider;

    private ButtonWidget shakeBtn;
    private ButtonWidget barsBtn;


    private TextFieldWidget mediaPathField;
    private ButtonWidget    refreshBtn;

    private ButtonWidget doneBtn;

    private static final int ROW_H = 20;
    private static final int GAP   = 12;

    private final List<net.minecraft.client.gui.widget.ClickableWidget> scrollWidgets = new ArrayList<>();
    private final Map<net.minecraft.client.gui.widget.ClickableWidget, Integer> baseY = new HashMap<>();

    private double scrollY = 0;
    private int contentTop, contentBottom, contentHeight;

    private boolean draggingScrollbar = false;
    private int     dragStartMouseY   = 0;
    private double  dragStartScrollY  = 0;

    public SoTuffOptionsScreen(Screen parent) {
        super(Text.literal("So Tuff Options (sigma)"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SoTuffConfig cfg = SoTuffConfig.get();

        int x = this.width / 2 - 100;

        contentTop    = 36;
        int donePad   = 8;
        int doneH     = ROW_H;
        int doneY     = this.height - doneH - donePad;
        contentBottom = doneY - 8;

        int y = contentTop;

        afterEachActionBtn = ButtonWidget.builder(
                Text.literal(toggleLabel("Trigger after each action", cfg.triggerAfterEachAction)),
                b -> {
                    cfg.triggerAfterEachAction = !cfg.triggerAfterEachAction;
                    b.setMessage(Text.literal(toggleLabel("Trigger after each action", cfg.triggerAfterEachAction)));
                    SoTuffConfig.save();
                    AutoScheduler.resetNow();
                    NetworkHandler.sendClientPrefsFromConfig();
                    updateEnabledStates();
                }
        ).dimensions(x, y, 200, ROW_H).build();
        afterEachActionBtn.setTooltip(Tooltip.of(Text.literal(
                "Play the effect after every action you do.\nIf you beat the game with this enabled, I respect the grind."
        )));
        addScrollable(afterEachActionBtn, y);

        y += ROW_H + GAP + 4;

        useCustomBtn = ButtonWidget.builder(
                Text.literal(toggleLabel("Use custom time window", cfg.useCustomWindow)),
                b -> {
                    cfg.useCustomWindow = !cfg.useCustomWindow;
                    b.setMessage(Text.literal(toggleLabel("Use custom time window", cfg.useCustomWindow)));
                    SoTuffConfig.save();
                    AutoScheduler.resetNow();
                    updateEnabledStates();
                }
        ).dimensions(x, y, 200, ROW_H).build();
        useCustomBtn.setTooltip(Tooltip.of(Text.literal("Choose your own min/max seconds for random triggers.")));
        addScrollable(useCustomBtn, y);

        y += ROW_H + GAP;

        double f = cfg.frequency01;
        if (Double.isNaN(f) || f < 0 || f > 1) f = 0.5;
        freqSlider = new FrequencySlider(
                x, y, 200, ROW_H, f,
                v -> {
                    cfg.frequency01 = MathUtils.clamp01(v);
                    SoTuffConfig.save();
                    NetworkHandler.sendClientPrefsFromConfig();
                }
        );
        freqSlider.setTooltip(Tooltip.of(Text.literal("How often the effect fires when playing normally.")));
        addScrollable(freqSlider, y);

        y += ROW_H + GAP + 4;

        int minSec = MathUtils.clampInt(cfg.customMinSec, 5, 900);
        int maxSec = MathUtils.clampInt(cfg.customMaxSec, 5, 900);
        if (minSec > maxSec) {
            int t = minSec;
            minSec = maxSec;
            maxSec = t;
        }

        minSlider = new IntSecondsSlider(
                x, y, 200, ROW_H, "Minimum delay",
                minSec, 5, 900,
                v -> {
                    SoTuffConfig c = SoTuffConfig.get();
                    c.customMinSec = MathUtils.clampInt(v, 5, Math.min(900, c.customMaxSec));
                    SoTuffConfig.save();
                }
        );
        minSlider.setTooltip(Tooltip.of(Text.literal("lower seconds used when custom window is ON.")));
        addScrollable(minSlider, y);

        y += ROW_H + GAP;

        maxSlider = new IntSecondsSlider(
                x, y, 200, ROW_H, "Maximum delay",
                maxSec, 5, 900,
                v -> {
                    SoTuffConfig c = SoTuffConfig.get();
                    c.customMaxSec = MathUtils.clampInt(v, Math.max(5, c.customMinSec), 900);
                    SoTuffConfig.save();
                }
        );
        maxSlider.setTooltip(Tooltip.of(Text.literal("higher seconds used when custom window is ON.")));
        addScrollable(maxSlider, y);

        y += ROW_H + GAP + 4;

        varySoundBtn = ButtonWidget.builder(
                Text.literal(toggleLabel("Randomize sound speed", cfg.varySoundSpeed)),
                b -> {
                    cfg.varySoundSpeed = !cfg.varySoundSpeed;
                    b.setMessage(Text.literal(toggleLabel("Randomize sound speed", cfg.varySoundSpeed)));
                    SoTuffConfig.save();
                    NetworkHandler.sendClientPrefsFromConfig();
                    updateEnabledStates();
                }
        ).dimensions(x, y, 200, ROW_H).build();
        addScrollable(varySoundBtn, y);

        y += ROW_H + GAP;

        pitchMinSlider = new PitchSlider(
                x, y, 200, ROW_H,
                "Min pitch", cfg.soundPitchMin,
                val -> { cfg.soundPitchMin = MathUtils.clampFloat((float) val, net.ekical.sotuff.SoTuffConstants.PITCH_MIN, net.ekical.sotuff.SoTuffConstants.PITCH_MAX); fixPitchOrder(); SoTuffConfig.save(); NetworkHandler.sendClientPrefsFromConfig(); }
        );
        addScrollable(pitchMinSlider, y);

        y += ROW_H + GAP;

        pitchMaxSlider = new PitchSlider(
                x, y, 200, ROW_H,
                "Max pitch", cfg.soundPitchMax,
                val -> { cfg.soundPitchMax = MathUtils.clampFloat((float) val, net.ekical.sotuff.SoTuffConstants.PITCH_MIN, net.ekical.sotuff.SoTuffConstants.PITCH_MAX); fixPitchOrder(); SoTuffConfig.save(); NetworkHandler.sendClientPrefsFromConfig(); }
        );
        addScrollable(pitchMaxSlider, y);

        y += ROW_H + GAP + 6;

        shakeBtn = ButtonWidget.builder(
                Text.literal(toggleLabel("Shake effect", cfg.shakeEnabled)),
                b -> {
                    cfg.shakeEnabled = !cfg.shakeEnabled;
                    b.setMessage(Text.literal(toggleLabel("Shake effect", cfg.shakeEnabled)));
                    SoTuffConfig.save();
                }
        ).dimensions(x, y, 200, ROW_H).build();
        shakeBtn.setTooltip(Tooltip.of(Text.literal("Enable/disable the overlay shake animation.")));
        addScrollable(shakeBtn, y);

        y += ROW_H + GAP + 6;

        barsBtn = ButtonWidget.builder(
                Text.literal(toggleLabel("Cinematic bars (9:16)", cfg.cinematicBars)),
                b -> {
                    cfg.cinematicBars = !cfg.cinematicBars;
                    b.setMessage(Text.literal(toggleLabel("Cinematic bars (9:16)", cfg.cinematicBars)));
                    SoTuffConfig.save();
                    NetworkHandler.sendClientPrefsFromConfig();
                }
        ).dimensions(x, y, 200, ROW_H).build();
        barsBtn.setTooltip(Tooltip.of(Text.literal("Enable W youtube shorts resolution")));
        addScrollable(barsBtn, y);

        y += ROW_H + GAP + 8;

        int fieldW = 200, btnW = 80, pad = 6;
        mediaPathField = new TextFieldWidget(this.textRenderer, x, y, fieldW, ROW_H, Text.empty());
        mediaPathField.setMaxLength(512);
        mediaPathField.setPlaceholder(Text.literal("Custom Media (root)"));
        mediaPathField.setText(SoTuffConfig.get().customMediaDir == null ? "" : SoTuffConfig.get().customMediaDir);
        mediaPathField.setChangedListener(val -> {
            SoTuffConfig c = SoTuffConfig.get();
            c.customMediaDir = val.trim();
            SoTuffConfig.save();
        });
        addSelectableChild(mediaPathField);
        addScrollable(mediaPathField, y);

        refreshBtn = ButtonWidget.builder(Text.literal("Refresh"), b -> {
            String p = SoTuffConfig.get().customMediaDir;
            net.ekical.sotuff.client.UserMedia.setRootAndRefresh(p);
            net.ekical.sotuff.client.OverlayRenderer.reloadUserImages();
        }).dimensions(x + fieldW + pad, y, btnW, ROW_H).build();
        addScrollable(refreshBtn, y);

        y += ROW_H + GAP + 10;

        contentHeight = (y + ROW_H) - contentTop;

        doneBtn = ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - 100, doneY, 200, doneH).build();
        addDrawableChild(doneBtn);

        updateEnabledStates();
        applyScroll();
    }

    private void addScrollable(net.minecraft.client.gui.widget.ClickableWidget w, int baseYVal) {
        scrollWidgets.add(w);
        baseY.put(w, baseYVal);
        addDrawableChild(w);
    }

    private void updateEnabledStates() {
        SoTuffConfig cfg = SoTuffConfig.get();
        boolean perAction = cfg.triggerAfterEachAction;
        boolean custom    = cfg.useCustomWindow;
        boolean pitchEnabled = cfg.varySoundSpeed;
        
        // Check if server is controlling settings
        boolean serverControlled = isServerControlled();

        if (useCustomBtn != null) useCustomBtn.active = !perAction;

        boolean enableFreq   = !perAction && !custom;
        boolean enableCustom = !perAction &&  custom;

        if (freqSlider   != null) freqSlider.active   = enableFreq;
        if (minSlider    != null) minSlider.active    = enableCustom;
        if (maxSlider    != null) maxSlider.active    = enableCustom;
        
        // Disable pitch controls if server is controlling them
        if (varySoundBtn != null) {
            varySoundBtn.active = !serverControlled;
            varySoundBtn.setTooltip(Tooltip.of(Text.literal(
                serverControlled 
                    ? "Randomize pitch (Controlled by server)"
                    : "Randomize pitch for the effect sound (slower/faster)."
            )));
        }
        if (pitchMinSlider != null) {
            pitchMinSlider.active = pitchEnabled && !serverControlled;
            pitchMinSlider.setTooltip(Tooltip.of(Text.literal(
                serverControlled 
                    ? "Minimum pitch (Controlled by server)"
                    : "Minimum pitch (0.50–2.00). 1.00 = normal speed."
            )));
        }
        if (pitchMaxSlider != null) {
            pitchMaxSlider.active = pitchEnabled && !serverControlled;
            pitchMaxSlider.setTooltip(Tooltip.of(Text.literal(
                serverControlled 
                    ? "Maximum pitch (Controlled by server)"
                    : "Maximum pitch (0.50–2.00). 1.00 = normal speed."
            )));
        }
    }


    private static void fixPitchOrder() {
        SoTuffConfig c = SoTuffConfig.get();
        float min = c.soundPitchMin;
        float max = c.soundPitchMax;
        if (max < min) {
            c.soundPitchMin = max;
            c.soundPitchMax = min;
        }
    }

    private String lastFreqHint = null;
    private double lastFreqValue = -1;
    private Boolean lastServerControlled = null;

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        if (freqSlider != null) {
            double f = freqSlider.getValue();
            if (Math.abs(f - lastFreqValue) > 0.001) {
                lastFreqValue = f;
                double t = MathUtils.easeInOutCubic(f);
                double minS = MathUtils.lerp(120,  5, t);
                double maxS = MathUtils.lerp(600, 20, t);
                lastFreqHint = String.format("Chance window: %.0f–%.0fs  (higher = more often, lower = rarer)", minS, maxS);
                freqSlider.setTooltip(Tooltip.of(Text.literal(lastFreqHint)));
            }
        }

        boolean sc = isServerControlled();
        if (lastServerControlled == null || lastServerControlled != sc) {
            lastServerControlled = sc;
            updateEnabledStates();
        }

        ctx.enableScissor(0, contentTop, this.width, contentBottom);
        if (mediaPathField != null) {
            int lx = mediaPathField.getX();
            int ly = mediaPathField.getY() - 10;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Custom Media:"), lx, ly, 0xA0A0A0);
        }
        for (var w : scrollWidgets) {
            w.render(ctx, mouseX, mouseY, delta);
        }
        ctx.disableScissor();

        if (doneBtn != null) {
            doneBtn.render(ctx, mouseX, mouseY, delta);
        }

        drawScrollbar(ctx);
    }

    private void drawScrollbar(DrawContext ctx) {
        int viewportH = Math.max(1, contentBottom - contentTop);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        if (maxScroll <= 0) return;

        int trackW = 6;
        int trackPad = 6;
        int trackX1 = this.width - trackW - trackPad;
        int trackX2 = this.width - trackPad;
        int trackY1 = contentTop;
        int trackY2 = contentBottom;

        int thumbH = Math.max(24, (int)((long)viewportH * viewportH / (long)contentHeight));
        int thumbMaxTravel = (viewportH - thumbH);
        int thumbY = trackY1 + (maxScroll == 0 ? 0 : (int)Math.round((scrollY / maxScroll) * thumbMaxTravel));

        int trackCol = 0x40FFFFFF;
        ctx.fill(trackX1, trackY1, trackX2, trackY2, trackCol);

        int thumbCol = draggingScrollbar ? 0xCCFFFFFF : 0x88FFFFFF;
        ctx.fill(trackX1, trackY1 + thumbY, trackX2, trackY1 + thumbY + thumbH, thumbCol);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            dragStartMouseY = (int) mouseY;
            dragStartScrollY = scrollY;
            return true;
        }

        if (doneBtn != null && doneBtn.isMouseOver(mouseX, mouseY)) {
            return doneBtn.mouseClicked(mouseX, mouseY, button);
        }

        if (mouseY < contentTop || mouseY > contentBottom) {
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScrollbar) {
            int viewportH = Math.max(1, contentBottom - contentTop);
            int maxScroll = Math.max(0, contentHeight - viewportH);
            if (maxScroll > 0) {
                int thumbH = Math.max(24, (int)((long)viewportH * viewportH / (long)contentHeight));
                int thumbMaxTravel = (viewportH - thumbH);
                if (thumbMaxTravel < 1) thumbMaxTravel = 1;

                double deltaPx = mouseY - dragStartMouseY;
                double deltaScroll = (deltaPx / thumbMaxTravel) * maxScroll;
                scrollY = MathUtils.clampDouble(dragStartScrollY + deltaScroll, 0, maxScroll);
                applyScroll();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int trackW = 6;
        int trackPad = 6;
        int x1 = this.width - trackW - trackPad;
        int x2 = this.width - trackPad;
        return mouseX >= x1 && mouseX <= x2 && mouseY >= contentTop && mouseY <= contentBottom
                && (contentHeight > (contentBottom - contentTop));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        int viewportH = Math.max(1, contentBottom - contentTop);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horiz, vert);

                scrollY = MathUtils.clampDouble(scrollY - vert * 12.0, 0, maxScroll);
        applyScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int viewportH = Math.max(1, contentBottom - contentTop);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        switch (keyCode) {
            case 265: // Up
                scrollY = MathUtils.clampDouble(scrollY - 12, 0, maxScroll); break;
            case 264: // Down
                scrollY = MathUtils.clampDouble(scrollY + 12, 0, maxScroll); break;
            case 266: // PageUp
                scrollY = MathUtils.clampDouble(scrollY - viewportH * 0.9, 0, maxScroll); break;
            case 267: // PageDown
                scrollY = MathUtils.clampDouble(scrollY + viewportH * 0.9, 0, maxScroll); break;
            case 268: // Home
                scrollY = 0; break;
            case 269: // End
                scrollY = maxScroll; break;
            default:
                return super.keyPressed(keyCode, scanCode, modifiers);
        }
        applyScroll();
        return true;
    }

    private void applyScroll() {
        int viewportH = Math.max(1, contentBottom - contentTop);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        scrollY = MathUtils.clampDouble(scrollY, 0, maxScroll);

        int dy = (int) (scrollY + 0.5); 

        for (var w : scrollWidgets) {
            Integer by = baseY.get(w);
            if (by != null) w.setY(by - dy);
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    private static String toggleLabel(String base, boolean on) {
        return on ? base + ": ON" : base + ": OFF";
    }
    
    private boolean isServerControlled() {
        if (!net.ekical.sotuff.config.SoTuffRuntime.isServerControlled()) {
            return false;
        }
        
        // Allow host/op to change settings
        if (this.client != null && this.client.player != null) {
            // Check if running integrated server (host)
            if (this.client.isIntegratedServerRunning()) {
                return false;
            }
            // Check if player has operator permissions
            if (this.client.player.hasPermissionLevel(2)) {
                return false;
            }
        }
        
        return true;
    }

    private static final class FrequencySlider extends SliderWidget {
        private final java.util.function.DoubleConsumer onChange;

        FrequencySlider(int x, int y, int w, int h, double initial01, java.util.function.DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), MathUtils.clamp01(initial01));
            this.onChange = onChange;
            updateMessage();
        }

        @Override protected void updateMessage() {
            int pct = (int)Math.round(this.value * 100.0);
            setMessage(Text.literal("Frequency: " + pct + "%"));
        }

        @Override protected void applyValue() { onChange.accept(this.value); }
        double getValue() { return this.value; }
    }

    private static final class IntSecondsSlider extends SliderWidget {
        private final java.util.function.IntConsumer onChange;
        private final String label;
        private final int minSec, maxSec;

        IntSecondsSlider(int x, int y, int w, int h, String label, int initialSec, int minSec, int maxSec,
                         java.util.function.IntConsumer onChange) {
            super(x, y, w, h, Text.empty(), norm(initialSec, minSec, maxSec));
            this.label = label;
            this.onChange = onChange;
            this.minSec = minSec;
            this.maxSec = maxSec;
            updateMessage();
        }

        private static double norm(int val, int lo, int hi) {
            val = Math.max(lo, Math.min(hi, val));
            return (val - lo) / (double)(hi - lo);
        }
        private int denorm(double v) { return (int)Math.round(minSec + v * (maxSec - minSec)); }

        @Override protected void updateMessage() {
            setMessage(Text.literal(label + ": " + denorm(this.value) + "s"));
        }

        @Override protected void applyValue() { onChange.accept(denorm(this.value)); }
    }

    private static final class PitchSlider extends SliderWidget {
        private final java.util.function.DoubleConsumer onChange;
        private final String label;

        PitchSlider(int x, int y, int w, int h, String label, float initialPitch,
                    java.util.function.DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), norm(initialPitch));
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        private static double norm(float pitch) {
            float p = pitch;
            if (p < 0.5f) p = 0.5f;
            if (p > 2.0f) p = 2.0f;
            return (p - 0.5f) / 1.5f;
        }
        private static float denorm(double v) {
            return (float)(0.5 + v * 1.5);
        }

        @Override protected void updateMessage() {
            setMessage(Text.literal(label + ": " + String.format("×%.2f", denorm(this.value))));
        }

        @Override protected void applyValue() {
            onChange.accept(denorm(this.value));
        }
    }
}
