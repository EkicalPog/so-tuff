package net.ekical.sotuff.client;

public final class MathUtils {
    private MathUtils() {}

    public static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    public static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static float clampFloat(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double clampDouble(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double easeInOutCubic(double x) {
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    }
}
