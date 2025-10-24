package net.ekical.sotuff.server;

import net.ekical.sotuff.common.FreezeControlBase;

public final class FreezeServerControl {
    private static volatile long untilMs = 0;

    public static void activate(int ms) {
        untilMs = FreezeControlBase.computeReplaceUntil(ms);
    }

    public static boolean isActive() {
        if (FreezeControlBase.isActive(untilMs)) {
            return true;
        }
        untilMs = 0;
        return false;
    }
}
