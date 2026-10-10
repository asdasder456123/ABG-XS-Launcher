package net.kdt.pojavlaunch.pvp;

import android.content.Context;

import net.kdt.pojavlaunch.ui.AbgUiManager;

/**
 * Single master gate for the complete ABG XS PVP subsystem.
 *
 * Every PVP feature must check this manager instead of keeping
 * its own independent enabled/disabled state.
 */
public final class PvpManager {

    private PvpManager() {
    }

    public static boolean isEnabled(Context context) {
        return AbgUiManager.isPvpEnabled(context);
    }

    public static void enable(Context context) {
        AbgUiManager.setPvpEnabled(context, true);
    }

    public static void disable(Context context) {
        AbgUiManager.setPvpEnabled(context, false);
    }

    public static void toggle(Context context) {
        if (isEnabled(context)) {
            disable(context);
        } else {
            enable(context);
        }
    }
}
