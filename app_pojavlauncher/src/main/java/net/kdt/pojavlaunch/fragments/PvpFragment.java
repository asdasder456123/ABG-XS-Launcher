package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.pvp.PvpManager;
import net.kdt.pojavlaunch.pvp.PvpModrinthManager;


public class PvpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {

        // PVP UI can only exist while the master switch is ON.
        if (!PvpManager.isEnabled(requireContext())) {
            Tools.backToMainMenu(requireActivity());
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.fragment_pvp, container, false);

        Button modrinth = view.findViewById(R.id.pvp_modrinth);
        Button disable = view.findViewById(R.id.pvp_disable);

        modrinth.setOnClickListener(v -> {
            if (!PvpManager.isEnabled(requireContext())) {
                return;
            }

            modrinth.setEnabled(false);
            Toast.makeText(
                    requireContext(),
                    "Checking Modrinth PVP compatibility...",
                    Toast.LENGTH_SHORT
            ).show();

            new Thread(() -> {
                final java.util.List<String> installed =
                        PvpModrinthManager.installCompatiblePvpMods(
                                requireContext()
                        );

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    modrinth.setEnabled(true);

                    if (installed.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                "No compatible PVP mod was installed.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                requireContext(),
                                "PVP mods ready: " + installed.size(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }).start();
        });

        disable.setOnClickListener(v -> {
            net.kdt.pojavlaunch.instances.Instance instance =
                    net.kdt.pojavlaunch.instances.Instances.loadSelectedInstance();

            // Restore the user's original mods before shutting down the master switch.
            if (instance != null) {
                PvpModrinthManager.deactivatePvp(instance);
            }

            // Master shutdown: every future PVP component must follow this state.
            PvpManager.disable(requireContext());

            // Return immediately to the original launcher flow.
            Tools.backToMainMenu(requireActivity());
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If PVP was disabled elsewhere, this screen must not remain active.
        if (isAdded() && !PvpManager.isEnabled(requireContext())) {
            Tools.backToMainMenu(requireActivity());
        }
    }
}
