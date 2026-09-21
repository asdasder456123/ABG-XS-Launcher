package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.ui.AbgUiManager;
import net.kdt.pojavlaunch.utils.GpuUtils;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);

        SwitchPreference pref =
                requirePreference("freedrenoSysmem", SwitchPreference.class);

        boolean hasFreedreno = GpuUtils.getGlInfo().isAdreno();
        pref.setVisible(hasFreedreno);

        Preference uiRemove = requirePreference("ui_remove", Preference.class);
        uiRemove.setOnPreferenceClickListener(preference -> {
            AbgUiManager.resetUi(requireContext());
            applyUiAndReturn();
            return true;
        });

        Preference uiDefault = requirePreference("ui_default", Preference.class);
        uiDefault.setOnPreferenceClickListener(preference -> {
            AbgUiManager.setSelectedUi(
                    requireContext(),
                    AbgUiManager.UI_DEFAULT
            );
            applyUiAndReturn();
            return true;
        });

        Preference uiAnimated = requirePreference("ui_animated", Preference.class);
        uiAnimated.setOnPreferenceClickListener(preference -> {
            AbgUiManager.setSelectedUi(
                    requireContext(),
                    AbgUiManager.UI_ANIMATED
            );
            applyUiAndReturn();
            return true;
        });
    }

    private void applyUiAndReturn() {
        requireActivity().recreate();
    }
}
