// net/ekical/sotuff/client/AutoScheduler.java
package net.ekical.sotuff.client;

import net.ekical.sotuff.config.SoTuffRuntime;
import net.ekical.sotuff.config.SoTuffConfig;
import net.ekical.sotuff.network.NetworkHandler;

public final class AutoScheduler {
    private static long nextAtMs = -1L;
    private AutoScheduler(){}

    public static void resetNow() { nextAtMs = -1L; }

    public static void tick() {
        if (SoTuffRuntime.triggerAfterEachAction()) { nextAtMs = -1L; return; }

        long now = System.currentTimeMillis();

        if (FreezeTickControl.isActive()) {
            if (nextAtMs < now) nextAtMs = now + sampleDelayMs();
            return;
        }

        if (nextAtMs < 0) {
            nextAtMs = now + sampleDelayMs();
            return;
        }

        if (now >= nextAtMs) {
            NetworkHandler.sendFreezeTrigger(4000);
            nextAtMs = now + sampleDelayMs();
        }
    }

    private static long sampleDelayMs() {
        SoTuffConfig c = SoTuffConfig.get();

        if (c.useCustomWindow) {
            int lo = MathUtils.clampInt(c.customMinSec, 5, 900);
            int hi = MathUtils.clampInt(c.customMaxSec, 5, 900);
            if (hi < lo) {
                int temp = lo;
                lo = hi;
                hi = temp;
            }
            int range = Math.max(1, hi - lo);
            return (long)((lo + Math.random() * range) * 1000.0);
        }

        double f = MathUtils.clamp01(SoTuffRuntime.frequency01());
        double t = MathUtils.easeInOutCubic(f);

        double minS = MathUtils.lerp(120, 5, t);
        double maxS = MathUtils.lerp(600, 20, t);
        double range = Math.max(1.0, maxS - minS);

        return (long)((minS + Math.random() * range) * 1000.0);
    }
}
