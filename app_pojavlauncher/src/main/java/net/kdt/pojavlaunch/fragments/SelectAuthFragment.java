package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

public class SelectAuthFragment extends Fragment {
    public static final String TAG = "AUTH_SELECT_FRAGMENT";

    public SelectAuthFragment() {
        super(R.layout.fragment_select_auth_method);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button microsoftButton = view.findViewById(R.id.button_microsoft_authentication);
        Button localButton = view.findViewById(R.id.button_local_authentication);
        Button elyByButton = view.findViewById(R.id.button_elyby_authentication);

        microsoftButton.setOnClickListener(v ->
                launchAuthFragment(MicrosoftLoginFragment.class, MicrosoftLoginFragment.TAG));
        localButton.setOnClickListener(v ->
                launchAuthFragment(LocalLoginFragment.class, LocalLoginFragment.TAG));
        elyByButton.setOnClickListener(v ->
                launchAuthFragment(ElyByLoginFragment.class, ElyByLoginFragment.TAG));
    }

    private void launchAuthFragment(Class<? extends Fragment> fragmentClass, String fragmentTag) {
        if (ProgressKeeper.hasProgressKey(ProgressLayout.AUTHENTICATE)) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show();
            return;
        }
        Tools.swapFragment(requireActivity(), fragmentClass, fragmentTag, null);
    }
}
