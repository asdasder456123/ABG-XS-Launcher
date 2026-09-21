package net.kdt.pojavlaunch.ai;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.tasks.MoJsonDownloader;
import net.kdt.pojavlaunch.tasks.MoJsonExtras;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LauncherAiActions {

    private static final Pattern MINECRAFT_VERSION =
            Pattern.compile(
                    "(?i)(?:minecraft|ماين\\s*كرافت).*?" +
                    "(?:الإصدار|اصدار|نسخة|version)?\\s*" +
                    "(\\d+\\.\\d+(?:\\.\\d+)?)"
            );

    private static final Pattern VERSION_FIRST =
            Pattern.compile(
                    "(?i)(?:الإصدار|اصدار|نسخة|version)?\\s*" +
                    "(\\d+\\.\\d+(?:\\.\\d+)?).*?" +
                    "(?:minecraft|ماين\\s*كرافت)"
            );

    private static final Pattern LAUNCH_REQUEST =
            Pattern.compile(
                    "(?i)(شغل|شغلي|شغّل|تشغيل|افتح|ابدأ|start|launch|run)"
            );

    private LauncherAiActions() {
    }

    public static boolean tryHandle(Context context, String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        if (!LAUNCH_REQUEST.matcher(message).find()) {
            return false;
        }

        String version = extractVersion(message);

        if (version == null) {
            return false;
        }

        handleVersion(context, version);
        return true;
    }

    private static String extractVersion(String message) {
        Matcher matcher = MINECRAFT_VERSION.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = VERSION_FIRST.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static void handleVersion(Context context, String version) {
        Instance instance = Instances.loadSelectedInstance();

        if (instance == null) {
            Toast.makeText(
                    context,
                    "مفيش ملف لعب محدد حاليًا.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String normalizedVersion = MoJsonExtras.normalizeVersionId(version);

        File versionJson = MoJsonDownloader.createGameJsonPath(normalizedVersion);

        if (versionJson.isFile()) {
            launchInstalled(context, instance, normalizedVersion);
            return;
        }

        JVersionList.Version listedVersion =
                MoJsonExtras.getListedVersion(normalizedVersion);

        if (listedVersion == null) {
            Toast.makeText(
                    context,
                    "الإصدار " + version + " مش موجود في قائمة Minecraft المتاحة.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("الإصدار غير موجود")
                .setMessage(
                        "Minecraft " + normalizedVersion +
                        " مش موجود عندك.\n\n" +
                        "تحب أحمّله لك؟"
                )
                .setPositiveButton(
                        "أيوه، حمّله",
                        (dialog, which) ->
                                downloadAndLaunch(
                                        context,
                                        instance,
                                        normalizedVersion,
                                        listedVersion
                                )
                )
                .setNegativeButton("لا", null)
                .show();
    }

    private static void launchInstalled(
            Context context,
            Instance instance,
            String version
    ) {
        instance.versionId = version;
        instance.maybeWrite();

        Toast.makeText(
                context,
                "حاضر، هشغل Minecraft " + version,
                Toast.LENGTH_SHORT
        ).show();

        ExtraCore.setValue(
                ExtraConstants.LAUNCH_GAME,
                true
        );
    }

    private static void downloadAndLaunch(
            Context context,
            Instance instance,
            String version,
            JVersionList.Version listedVersion
    ) {
        instance.versionId = version;
        instance.maybeWrite();

        Toast.makeText(
                context,
                "تمام، ببدأ تحميل Minecraft " + version,
                Toast.LENGTH_SHORT
        ).show();

        new MoJsonDownloader().start(
                context.getAssets(),
                listedVersion,
                version,
                new ContextAwareDoneListener(context, version)
        );
    }
}
