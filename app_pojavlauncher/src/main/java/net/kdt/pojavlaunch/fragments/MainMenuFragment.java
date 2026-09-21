package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.ui.AbgUiManager;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.jre.GameRunner;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment() {
        super();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        int layout = AbgUiManager.isAnimatedUi(requireContext())
                ? R.layout.fragment_launcher_animated
                : R.layout.fragment_launcher;

        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> {
        Instance instance = Instances.loadSelectedInstance();
        File gamedir = instance.getGameDirectory();

        if (GameRunner.hasVkMod(gamedir)) {
            new AlertDialog.Builder(requireContext())
            .setTitle(R.string.vk_mod_title)
            .setMessage(R.string.vk_mod_message)
            .setPositiveButton(R.string.continue_button, (d, w) -> {
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            })
            .show();
        } else if (GameRunner.hasReplay(gamedir) && GameRunner.hasFfmpeg(requireContext())) {
          new AlertDialog.Builder(requireContext())
            .setTitle(R.string.no_ffmpeg_title)
            .setMessage(R.string.no_ffmpeg_message)
            .setPositiveButton(R.string.install_button, (d, w) -> {
             Tools.openURL(requireActivity(), "https://github.com/MojoLauncher/FFmpegPlugin/releases");
    })
    .setNegativeButton(R.string.continue_button, (d, w) -> {
        ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
    })
    .show();
        } else {
        ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        }
        });

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));

        if (AbgUiManager.isAnimatedUi(requireContext())) {
            animateAnimatedUi(view);
        }



    }

    private void animateAnimatedUi(View root) {
        View content = root.findViewById(R.id.animated_content);
        if (content == null) return;

        content.setAlpha(0f);
        content.setTranslationY(32f);

        content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setStartDelay(80)
                .start();

        int[] buttonIds = {
                R.id.social_media_button,
                R.id.custom_control_button,
                R.id.install_jar_button,
                R.id.share_logs_button,
                R.id.open_files_button
        };

        for (int i = 0; i < buttonIds.length; i++) {
            View button = root.findViewById(buttonIds[i]);
            if (button == null) continue;

            button.setAlpha(0f);
            button.setTranslationX(24f);

            button.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setStartDelay(140L + (i * 55L))
                    .start();

            button.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate()
                                .scaleX(0.97f)
                                .scaleY(0.97f)
                                .setDuration(90)
                                .start();
                        break;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start();
                        break;
                }

                return false;
            });
        }

        View play = root.findViewById(R.id.play_button);
        if (play != null) {
            if (AbgUiManager.isAnimatedUi(requireContext())) {
                play.setBackgroundResource(R.drawable.bg_abg_animated_play);
            }

            play.setScaleX(0.94f);
            play.setScaleY(0.94f);
            play.setAlpha(0f);

            play.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(380)
                    .setStartDelay(360)
                    .start();

            play.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(80)
                            .start();
                } else if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP
                        || event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start();
                }

                return false;
            });
        }
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);

        if (mVersionSpinner != null) {
            mVersionSpinner.post(() -> {
                if (isAdded()) {
                    mVersionSpinner.reloadProfiles();
                }
            });
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
