package net.kdt.pojavlaunch.pvp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.utils.DownloadUtils;

public final class PvpModrinthManager {

    private static final String API_BASE =
            "https://api.modrinth.com/v2/project/";

    /*
     * ABG XS PVP library.
     *
     * Only mods explicitly added here belong to the PVP library.
     */
    private static final String PVP_MANIFEST = "pvp-mods.txt";

    private static final String[] PVP_LIBRARY = {
            "feather-remake",
            "fabric-api"
    };

    private PvpModrinthManager() {
    }

    public static File getPvpDirectory(Instance instance) {
        return new File(instance.getGameDirectory(), "pvp");
    }

    public static File getPvpModsDirectory(Instance instance) {
        return new File(getPvpDirectory(instance), "mods");
    }

    /**
     * Downloads only exact Minecraft-version + Fabric-compatible PVP mods.
     *
     * No fallback to another Minecraft version is allowed.
     */
    public static List<String> installCompatiblePvpMods(Context context) {
        List<String> installed = new ArrayList<>();

        if (!PvpManager.isEnabled(context)) {
            System.out.println("[PVP] Master switch is OFF. Modrinth disabled.");
            return installed;
        }

        Instance instance = Instances.loadSelectedInstance();

        if (instance == null) {
            System.out.println("[PVP] No selected instance.");
            return installed;
        }

        String minecraftVersion = instance.versionId;

        if (minecraftVersion == null || minecraftVersion.trim().isEmpty()) {
            System.out.println("[PVP] Selected instance has no Minecraft version.");
            return installed;
        }

        // Download directly into the real Minecraft mods directory.
        // This is the directory Fabric actually reads when the game launches.
        File modsDir = new File(instance.getGameDirectory(), "mods");

        if (!modsDir.exists() && !modsDir.mkdirs()) {
            System.out.println("[PVP] Could not create Minecraft mods directory.");
            return installed;
        }

        for (String projectSlug : PVP_LIBRARY) {
            try {
                String fileName = installExactCompatibleMod(
                        modsDir,
                        projectSlug,
                        minecraftVersion
                );

                if (fileName != null) {
                    installed.add(fileName);

                    // The JAR is already in the real Minecraft mods/.
                    // Record it as a PVP file so activatePvp() does not
                    // move it into disabled-mods as a normal user mod.
                    File pvpDir = getPvpDirectory(instance);
                    File manifest = new File(pvpDir, PVP_MANIFEST);

                    if (!pvpDir.exists()) {
                        pvpDir.mkdirs();
                    }

                    appendManifestEntry(manifest, fileName);
                }
            } catch (Exception e) {
                System.out.println(
                        "[PVP] Failed " + projectSlug + ": "
                                + e.getMessage()
                );
            }
        }

        return installed;
    }

    private static void appendManifestEntry(
            File manifest,
            String fileName
    ) {
        if (manifest == null || fileName == null || fileName.trim().isEmpty()) {
            return;
        }

        try {
            if (manifest.exists()) {
                try (
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(manifest),
                                        StandardCharsets.UTF_8
                                )
                        )
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (fileName.equals(line.trim())) {
                            return;
                        }
                    }
                }
            }

            try (
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            manifest,
                                            true
                                    ),
                                    StandardCharsets.UTF_8
                            )
                    )
            ) {
                writer.write(fileName);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println(
                    "[PVP] Failed to update manifest: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Prepare Fabric's normal mods/ directory for an active PVP session.
     *
     * PVP mods remain stored permanently under pvp/mods/.
     * Only the files required for this launch are copied into mods/.
     *
     * @return files copied into the normal mods directory
     */
    public static boolean activatePvp(
            Context context,
            Instance instance
    ) {
        if (!PvpManager.isEnabled(context) || instance == null) {
            return false;
        }

        File gameDir = instance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");
        File pvpDir = getPvpDirectory(instance);
        File pvpModsDir = getPvpModsDirectory(instance);
        File disabledDir = new File(pvpDir, "disabled-mods");
        File manifest = new File(pvpDir, PVP_MANIFEST);

        if (!modsDir.exists() && !modsDir.mkdirs()) return false;
        if (!pvpDir.exists() && !pvpDir.mkdirs()) return false;
        if (!pvpModsDir.exists() && !pvpModsDir.mkdirs()) return false;
        if (!disabledDir.exists() && !disabledDir.mkdirs()) return false;

        /*
         * First recover normal user mods from an interrupted previous
         * session. PVP mods are identified by the manifest.
         */
        restoreUserMods(modsDir, disabledDir, manifest);

        /*
         * If this is the first activation, the downloaded PVP library
         * lives in pvp/mods/. Record those JAR names.
         */
        writeManifestFromLibrary(pvpModsDir, manifest);

        /*
         * Move all normal/user JARs out of Minecraft's real mods/.
         */
        moveUserModsToDisabled(modsDir, disabledDir, manifest);

        /*
         * Re-activate PVP JARs that were disabled by a previous session.
         */
        movePvpFromDisabled(modsDir, disabledDir, manifest);

        /*
         * First activation: move downloaded PVP JARs into real mods/.
         */
        File[] libraryFiles = pvpModsDir.listFiles();
        if (libraryFiles != null) {
            for (File file : libraryFiles) {
                if (!isJar(file)) continue;

                File destination = new File(modsDir, file.getName());
                if (!destination.exists()) {
                    moveFile(file, destination);
                }
            }
        }

        return hasPvpMods(modsDir, manifest);
    }

    public static void deactivatePvp(Instance instance) {
        if (instance == null) return;

        File gameDir = instance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");
        File pvpDir = getPvpDirectory(instance);
        File disabledDir = new File(pvpDir, "disabled-mods");
        File manifest = new File(pvpDir, PVP_MANIFEST);

        if (!disabledDir.exists()) {
            disabledDir.mkdirs();
        }

        movePvpToDisabled(modsDir, disabledDir, manifest);
        restoreUserMods(modsDir, disabledDir, manifest);
    }

    /*
     * Compatibility wrapper used by GameRunner.
     */
    public static List<File> prepareLaunch(
            Context context,
            Instance instance
    ) {
        List<File> active = new ArrayList<>();

        if (!activatePvp(context, instance)) {
            return active;
        }

        File modsDir = new File(
                instance.getGameDirectory(),
                "mods"
        );

        File[] files = modsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isJar(file) && isPvpFile(file, instance)) {
                    active.add(file);
                }
            }
        }

        return active;
    }

    /*
     * Compatibility wrapper used by GameRunner.
     */
    public static void cleanupLaunch(List<File> ignored) {
        Instance instance = Instances.loadSelectedInstance();
        if (instance != null) {
            deactivatePvp(instance);
        }
    }

    private static void moveUserModsToDisabled(
            File modsDir,
            File disabledDir,
            File manifest
    ) {
        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isJar(file) || isManifestPvp(file, manifest)) {
                continue;
            }

            moveFile(
                    file,
                    new File(disabledDir, file.getName())
            );
        }
    }

    private static void movePvpToDisabled(
            File modsDir,
            File disabledDir,
            File manifest
    ) {
        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isJar(file) || !isManifestPvp(file, manifest)) {
                continue;
            }

            moveFile(
                    file,
                    new File(disabledDir, file.getName())
            );
        }
    }

    private static void movePvpFromDisabled(
            File modsDir,
            File disabledDir,
            File manifest
    ) {
        File[] files = disabledDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isJar(file) || !isManifestPvp(file, manifest)) {
                continue;
            }

            moveFile(
                    file,
                    new File(modsDir, file.getName())
            );
        }
    }

    private static void restoreUserMods(
            File modsDir,
            File disabledDir,
            File manifest
    ) {
        File[] files = disabledDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isJar(file) || isManifestPvp(file, manifest)) {
                continue;
            }

            moveFile(
                    file,
                    new File(modsDir, file.getName())
            );
        }
    }

    private static void writeManifestFromLibrary(
            File pvpModsDir,
            File manifest
    ) {
        File[] files = pvpModsDir.listFiles();
        if (files == null || files.length == 0 || manifest.exists()) {
            return;
        }

        try (
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        manifest
                                ),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            for (File file : files) {
                if (isJar(file)) {
                    writer.write(file.getName());
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            System.out.println(
                    "[PVP] Failed to write manifest: "
                            + e.getMessage()
            );
        }
    }

    private static boolean isManifestPvp(
            File file,
            File manifest
    ) {
        if (file == null || !file.isFile() || !manifest.exists()) {
            return false;
        }

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(manifest),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (file.getName().equals(line.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println(
                    "[PVP] Failed to read manifest: "
                            + e.getMessage()
            );
        }

        return false;
    }

    private static boolean isPvpFile(
            File file,
            Instance instance
    ) {
        return isManifestPvp(
                file,
                new File(
                        getPvpDirectory(instance),
                        PVP_MANIFEST
                )
        );
    }

    private static boolean hasPvpMods(
            File modsDir,
            File manifest
    ) {
        File[] files = modsDir.listFiles();
        if (files == null) return false;

        for (File file : files) {
            if (isJar(file) && isManifestPvp(file, manifest)) {
                return true;
            }
        }

        return false;
    }

    private static boolean moveFile(
            File source,
            File destination
    ) {
        if (source == null || destination == null || !source.exists()) {
            return false;
        }

        if (destination.exists() && !destination.delete()) {
            return false;
        }

        return source.renameTo(destination);
    }

    private static boolean isJar(File file) {
        return file != null
                && file.isFile()
                && file.getName()
                .toLowerCase()
                .endsWith(".jar");
    }


    private static void restoreDisabledMods(
            File modsDir,
            File disabledModsDir
    ) {
        if (!disabledModsDir.exists()) {
            return;
        }

        File[] backups = disabledModsDir.listFiles();

        if (backups == null) {
            return;
        }

        for (File backup : backups) {
            if (!backup.isFile()
                    || !backup.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }

            File restored =
                    new File(modsDir, backup.getName());

            if (restored.exists()) {
                restored.delete();
            }

            if (backup.renameTo(restored)) {
                System.out.println(
                        "[PVP] Restored normal mod: "
                                + restored.getName()
                );
            }
        }
    }

    private static String installExactCompatibleMod(
            File destination,
            String projectSlug,
            String minecraftVersion
    ) throws Exception {

        JSONObject version = findBestCompatibleVersion(
                projectSlug,
                minecraftVersion
        );

        if (version == null) {
            System.out.println(
                    "[PVP] No compatible version found: "
                            + projectSlug
                            + " / "
                            + minecraftVersion
            );
            return null;
        }

        String installedName = installVersionFile(
                destination,
                version,
                projectSlug
        );

        /*
         * Resolve required Modrinth dependencies automatically.
         *
         * A dependency with an explicit version_id is preferred because
         * Modrinth has already associated that exact version with the mod.
         *
         * A dependency without version_id is resolved again using the
         * exact Minecraft version + Fabric requirement.
         */
        JSONArray dependencies = version.optJSONArray("dependencies");

        if (dependencies != null) {
            for (int i = 0; i < dependencies.length(); i++) {
                JSONObject dependency = dependencies.getJSONObject(i);

                if (!"required".equalsIgnoreCase(
                        dependency.optString("dependency_type")
                )) {
                    continue;
                }

                String dependencyVersionId =
                        dependency.optString("version_id", "");

                String dependencyProjectId =
                        dependency.optString("project_id", "");

                if (!dependencyVersionId.isEmpty()) {
                    installExactDependencyVersion(
                            destination,
                            dependencyVersionId,
                            minecraftVersion
                    );
                } else if (!dependencyProjectId.isEmpty()) {
                    JSONObject dependencyVersion =
                            findBestCompatibleVersionByProjectId(
                                    dependencyProjectId,
                                    minecraftVersion
                            );

                    if (dependencyVersion == null) {
                        throw new IllegalStateException(
                                "Required dependency is not compatible: "
                                        + dependencyProjectId
                        );
                    }

                    installVersionFile(
                            destination,
                            dependencyVersion,
                            dependencyProjectId
                    );
                }
            }
        }

        return installedName;
    }

    private static JSONObject findBestCompatibleVersion(
            String projectSlug,
            String minecraftVersion
    ) throws Exception {

        String url = API_BASE
                + projectSlug
                + "/version"
                + "?loaders=%5B%22fabric%22%5D"
                + "&game_versions=%5B%22"
                + encodeApiValue(minecraftVersion)
                + "%22%5D";

        System.out.println(
                "[PVP] Resolving "
                        + projectSlug
                        + " for Minecraft "
                        + minecraftVersion
                        + " / Fabric"
        );

        JSONArray versions =
                new JSONArray(DownloadUtils.downloadString(url));

        JSONObject firstCompatible = null;

        for (int i = 0; i < versions.length(); i++) {
            JSONObject candidate = versions.getJSONObject(i);

            if (!hasExactMinecraftVersion(
                    candidate,
                    minecraftVersion
            )) {
                continue;
            }

            if (!hasFabricLoader(candidate)) {
                continue;
            }

            if (firstCompatible == null) {
                firstCompatible = candidate;
            }

            if ("release".equalsIgnoreCase(
                    candidate.optString("version_type")
            )) {
                return candidate;
            }
        }

        return firstCompatible;
    }

    private static JSONObject findBestCompatibleVersionByProjectId(
            String projectId,
            String minecraftVersion
    ) throws Exception {

        String url = "https://api.modrinth.com/v2/project/"
                + projectId
                + "/version"
                + "?loaders=%5B%22fabric%22%5D"
                + "&game_versions=%5B%22"
                + encodeApiValue(minecraftVersion)
                + "%22%5D";

        JSONArray versions =
                new JSONArray(DownloadUtils.downloadString(url));

        JSONObject firstCompatible = null;

        for (int i = 0; i < versions.length(); i++) {
            JSONObject candidate = versions.getJSONObject(i);

            if (!hasExactMinecraftVersion(
                    candidate,
                    minecraftVersion
            )) {
                continue;
            }

            if (!hasFabricLoader(candidate)) {
                continue;
            }

            if (firstCompatible == null) {
                firstCompatible = candidate;
            }

            if ("release".equalsIgnoreCase(
                    candidate.optString("version_type")
            )) {
                return candidate;
            }
        }

        return firstCompatible;
    }

    private static void installExactDependencyVersion(
            File destination,
            String versionId,
            String minecraftVersion
    ) throws Exception {

        String url =
                "https://api.modrinth.com/v2/version/"
                        + versionId;

        JSONObject version =
                new JSONObject(
                        DownloadUtils.downloadString(url)
                );

        if (!hasExactMinecraftVersion(
                version,
                minecraftVersion
        )) {
            throw new IllegalStateException(
                    "Dependency version does not support Minecraft "
                            + minecraftVersion
            );
        }

        if (!hasFabricLoader(version)) {
            throw new IllegalStateException(
                    "Dependency version is not Fabric compatible"
            );
        }

        installVersionFile(
                destination,
                version,
                versionId
        );
    }

    private static String installVersionFile(
            File destination,
            JSONObject version,
            String projectName
    ) throws Exception {

        JSONArray files = version.optJSONArray("files");

        if (files == null || files.length() == 0) {
            throw new IllegalStateException(
                    "No downloadable files for " + projectName
            );
        }

        JSONObject selectedFile = null;

        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);

            if (!file.optBoolean("primary", false)) {
                continue;
            }

            String filename = file.optString("filename", "");

            if (filename.toLowerCase().endsWith(".jar")) {
                selectedFile = file;
                break;
            }
        }

        if (selectedFile == null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String filename = file.optString("filename", "");

                if (filename.toLowerCase().endsWith(".jar")) {
                    selectedFile = file;
                    break;
                }
            }
        }

        if (selectedFile == null) {
            throw new IllegalStateException(
                    "No JAR file available for " + projectName
            );
        }

        String downloadUrl =
                selectedFile.optString("url", "");

        String filename =
                selectedFile.optString("filename", "");

        if (downloadUrl.isEmpty() || filename.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid download metadata for " + projectName
            );
        }

        File output = new File(destination, filename);

        if (output.exists() && output.length() > 0) {
            System.out.println(
                    "[PVP] Already installed: " + filename
            );
            return filename;
        }

        System.out.println(
                "[PVP] Downloading compatible version: "
                        + filename
        );

        DownloadUtils.downloadFile(downloadUrl, output);

        if (!output.exists() || output.length() <= 0) {
            throw new IllegalStateException(
                    "Invalid downloaded file: " + filename
            );
        }

        System.out.println(
                "[PVP] Installed: " + filename
        );

        return filename;
    }

    private static String encodeApiValue(String value) {
        return value
                .replace("\\", "")
                .replace("\"", "");
    }

    private static boolean hasExactMinecraftVersion(
            JSONObject version,
            String minecraftVersion
    ) {
        JSONArray gameVersions =
                version.optJSONArray("game_versions");

        if (gameVersions == null) {
            return false;
        }

        for (int i = 0; i < gameVersions.length(); i++) {
            if (minecraftVersion.equals(
                    gameVersions.optString(i)
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasFabricLoader(JSONObject version) {
        JSONArray loaders =
                version.optJSONArray("loaders");

        if (loaders == null) {
            return false;
        }

        for (int i = 0; i < loaders.length(); i++) {
            if ("fabric".equalsIgnoreCase(
                    loaders.optString(i)
            )) {
                return true;
            }
        }

        return false;
    }
}
