package net.kdt.pojavlaunch.modrinth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.utils.DownloadUtils;

public final class ModrinthModInstaller {

    private static final String API_BASE =
            "https://api.modrinth.com/v2/project/";

    private static final List<String> MODS = Arrays.asList(
            "sodium",
            "fabric-api",
            "iris",
            "entityculling",
            "modmenu",
            "lithium",
            "sodium-extra"
    );

    private ModrinthModInstaller() {
    }

    public static void installCompatibleMods(
            Instance instance,
            String minecraftVersion
    ) {
        File modsDir = new File(instance.getGameDirectory(), "mods");

        if (!modsDir.exists() && !modsDir.mkdirs()) {
            System.out.println("[Modrinth] Could not create mods directory.");
            return;
        }

        for (String modId : MODS) {
            try {
                installMod(modsDir, modId, minecraftVersion);
            } catch (Exception e) {
                // A missing/incompatible mod must never break Fabric installation.
                System.out.println(
                        "[Modrinth] Skipping " + modId + ": " + e.getMessage()
                );
            }
        }
    }

    private static void installMod(
            File modsDir,
            String modId,
            String minecraftVersion
    ) throws IOException, JSONException {

        String url = API_BASE + modId + "/version"
                + "?loaders=%5B%22fabric%22%5D"
                + "&game_versions=%5B%22"
                + minecraftVersion.replace("\"", "")
                + "%22%5D";

        String response = DownloadUtils.downloadString(url);
        JSONArray versions = new JSONArray(response);

        if (versions.length() == 0) {
            System.out.println(
                    "[Modrinth] No compatible Fabric version: "
                            + modId + " / " + minecraftVersion
            );
            return;
        }

        JSONObject version = null;

        // Prefer a stable release when Modrinth returns multiple compatible versions.
        for (int i = 0; i < versions.length(); i++) {
            JSONObject candidate = versions.getJSONObject(i);
            if ("release".equalsIgnoreCase(candidate.optString("version_type"))) {
                version = candidate;
                break;
            }
        }

        // Fall back to the first compatible version if no release is available.
        if (version == null) {
            version = versions.getJSONObject(0);
        }

        JSONArray files = version.optJSONArray("files");

        if (files == null || files.length() == 0) {
            System.out.println(
                    "[Modrinth] No downloadable file: " + modId
            );
            return;
        }

        JSONObject fileInfo = null;

        // Prefer Modrinth's explicitly marked primary JAR.
        for (int i = 0; i < files.length(); i++) {
            JSONObject candidate = files.getJSONObject(i);
            String filename = candidate.optString("filename", "");

            if (candidate.optBoolean("primary", false)
                    && filename.toLowerCase().endsWith(".jar")) {
                fileInfo = candidate;
                break;
            }
        }

        // Fallback: first JAR file.
        if (fileInfo == null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject candidate = files.getJSONObject(i);
                String filename = candidate.optString("filename", "");

                if (filename.toLowerCase().endsWith(".jar")) {
                    fileInfo = candidate;
                    break;
                }
            }
        }

        if (fileInfo == null) {
            System.out.println(
                    "[Modrinth] No JAR file available: " + modId
            );
            return;
        }

        String downloadUrl = fileInfo.getString("url");
        String fileName = fileInfo.getString("filename");

        File output = new File(modsDir, fileName);

        if (output.exists() && output.length() > 0) {
            System.out.println(
                    "[Modrinth] Already installed: " + fileName
            );
            return;
        }

        System.out.println(
                "[Modrinth] Downloading " + modId
                        + " -> " + fileName
        );

        DownloadUtils.downloadFile(downloadUrl, output);

        System.out.println(
                "[Modrinth] Installed: " + fileName
        );
    }
}
