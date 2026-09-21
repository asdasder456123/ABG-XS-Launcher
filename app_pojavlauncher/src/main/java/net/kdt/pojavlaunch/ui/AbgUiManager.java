package net.kdt.pojavlaunch.ui;

import android.content.Context;
import android.content.SharedPreferences;

public final class AbgUiManager {

    private static final String PREFS_NAME = "abg_xs_ui";
    private static final String KEY_SELECTED_UI = "selected_ui";

    public static final String UI_DEFAULT = "default";
    public static final String UI_ANIMATED = "animated";

    private AbgUiManager() {
    }

    public static String getSelectedUi(Context context) {
        return getPreferences(context).getString(KEY_SELECTED_UI, UI_DEFAULT);
    }

    public static void setSelectedUi(Context context, String uiId) {
        getPreferences(context)
                .edit()
                .putString(KEY_SELECTED_UI, uiId)
                .apply();
    }

    public static void resetUi(Context context) {
        getPreferences(context)
                .edit()
                .remove(KEY_SELECTED_UI)
                .apply();
    }

    public static boolean isDefaultUi(Context context) {
        return UI_DEFAULT.equals(getSelectedUi(context));
    }

    public static boolean isAnimatedUi(Context context) {
        return UI_ANIMATED.equals(getSelectedUi(context));
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
