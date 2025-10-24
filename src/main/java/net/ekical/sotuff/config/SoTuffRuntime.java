package net.ekical.sotuff.config;

import net.ekical.sotuff.client.MathUtils;

public final class SoTuffRuntime {
    private SoTuffRuntime() {}

    private static boolean hasServerSet = false;
    private static boolean triggerAfterEachAction = false;
    private static double  frequency01 = 0.5;     // 0..1
    private static boolean varySoundSpeed = true;
    private static boolean cinematicBars = true;

    private static float pitchMin = 1.0f;
    private static float pitchMax = 1.0f;
    private static float pitchDefault = 1.0f;

    public static boolean triggerAfterEachAction() { return hasServerSet ? triggerAfterEachAction : SoTuffConfig.get().triggerAfterEachAction; }
    public static double  frequency01()            { return hasServerSet ? frequency01            : SoTuffConfig.get().frequency01; }
    public static boolean varySoundSpeed()         { return hasServerSet ? varySoundSpeed         : SoTuffConfig.get().varySoundSpeed; }
    public static boolean cinematicBars()          { return hasServerSet ? cinematicBars          : SoTuffConfig.get().cinematicBars; }

    public static float pitchMin()                 { return hasServerSet ? pitchMin     : SoTuffConfig.get().soundPitchMin; }
    public static float pitchMax()                 { return hasServerSet ? pitchMax     : SoTuffConfig.get().soundPitchMax; }
    public static float pitchDefault()             { return hasServerSet ? pitchDefault : SoTuffConfig.get().soundPitchDefault; }

    public static void applyFromServer(boolean perAction, double freq01, boolean vary, boolean bars,
                                       float pMin, float pMax, float pDef) {
        hasServerSet = true;
        triggerAfterEachAction = perAction;
        frequency01 = MathUtils.clamp01(freq01);
        varySoundSpeed = vary;
        cinematicBars = bars;
        pitchMin = pMin;
        pitchMax = pMax;
        pitchDefault = pDef;
    }

    public static void clearServerState() { hasServerSet = false; }
    
    public static boolean isServerControlled() { return hasServerSet; }
}
