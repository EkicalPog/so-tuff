package net.ekical.sotuff.common;

public final class FreezeControlBase {
    private FreezeControlBase() {}

    public static boolean isActive(long untilMs) {
        if (untilMs == 0) return false;
        if (untilMs == Long.MAX_VALUE) return true;
        return System.currentTimeMillis() < untilMs;
    }

    public static long computeNewUntil(long currentUntil, int durationMs) {
        long now = System.currentTimeMillis();
        long newUntil = (durationMs == Integer.MAX_VALUE)
            ? Long.MAX_VALUE
            : now + Math.max(0, durationMs);
        return Math.max(currentUntil, newUntil);
    }

    public static long computeReplaceUntil(int durationMs) {
        if (durationMs == Integer.MAX_VALUE) return Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        return now + Math.max(0, durationMs);
    }
}
