package net.ekical.sotuff.client;

import net.ekical.sotuff.common.FreezeControlBase;

public final class FreezeTickControl {
    private static volatile long untilMs = 0;

    public static boolean activate(long ms) {
        boolean wasActive = isActive();
        untilMs = FreezeControlBase.computeReplaceUntil((int) ms);
        return !wasActive;
    }

    public static void clear() { untilMs = 0; }

    public static boolean isActive() {
        if (FreezeControlBase.isActive(untilMs)) {
            return true;
        }
        untilMs = 0;
        return false;
    }
}
