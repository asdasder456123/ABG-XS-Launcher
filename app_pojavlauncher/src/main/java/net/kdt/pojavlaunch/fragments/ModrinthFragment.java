package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

public class ModrinthFragment extends Fragment {

    public static final String TAG = "ModrinthFragment";

    private EditText search;
    private ProgressBar progress;
    private ModrinthAdapter adapter;

    public ModrinthFragment() {
        super(R.layout.fragment_modrinth);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        search = view.findViewById(R.id.modrinth_search);
        progress = view.findViewById(R.id.modrinth_progress);

        RecyclerView list = view.findViewById(R.id.modrinth_results);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ModrinthAdapter();
        list.setAdapter(adapter);

        view.findViewById(R.id.modrinth_search_button)
                .setOnClickListener(v -> searchMods());

        // إظهار مودات تلقائيًا عند فتح الصفحة
        loadMods("");
    }

    private void searchMods() {
        loadMods(search.getText().toString().trim());
    }

    private void loadMods(String query) {
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8.toString()
                );

                String facets = URLEncoder.encode(
                        "[[\"project_type:mod\"]]",
                        StandardCharsets.UTF_8.toString()
                );

                String api =
                        "https://api.modrinth.com/v2/search?query="
                                + encoded
                                + "&index=downloads"
                                + "&facets=" + facets
                                + "&limit=12";

                HttpURLConnection connection =
                        (HttpURLConnection) new URL(api).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                InputStream input = connection.getInputStream();
                String json = new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );
                input.close();
                connection.disconnect();

                JSONArray hits =
                        new JSONObject(json).getJSONArray("hits");

                List<ModItem> results = new ArrayList<>();

                for (int i = 0; i < hits.length(); i++) {
                    JSONObject item = hits.getJSONObject(i);

                    String projectId =
                            item.optString("project_id");

                    ModItem mod = new ModItem(
                            item.optString("title"),
                            item.optString("description"),
                            projectId,
                            item.optString("icon_url")
                    );

                    // جلب جميع إصدارات المود مع توافق Minecraft وملفات التحميل
                    loadVersions(mod);

                    results.add(mod);
                }

                android.app.Activity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    progress.setVisibility(View.GONE);
                    adapter.setItems(results);
                });

            } catch (Exception e) {
                android.app.Activity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;

                    progress.setVisibility(View.GONE);

                    Toast.makeText(
                            requireContext(),
                            "فشل الاتصال بـ Modrinth",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void loadVersions(ModItem mod) {
        try {
            Instance instance = Instances.loadSelectedInstance();

            String gameVersion =
                    instance != null && instance.versionId != null
                            ? instance.versionId
                            : "";

            String api =
                    "https://api.modrinth.com/v2/project/"
                            + URLEncoder.encode(
                                    mod.projectId,
                                    StandardCharsets.UTF_8.toString())
                            + "/version";

            HttpURLConnection connection =
                    (HttpURLConnection) new URL(api).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            InputStream input = connection.getInputStream();

            String json = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            input.close();
            connection.disconnect();

            JSONArray versions = new JSONArray(json);

            for (int i = 0; i < versions.length(); i++) {
                JSONObject version = versions.getJSONObject(i);

                String versionName =
                        version.optString("name");

                if (versionName.isEmpty()) {
                    versionName =
                            version.optString("version_number");
                }

                JSONArray gameVersions =
                        version.optJSONArray("game_versions");

                boolean compatible = false;

                if (gameVersion.isEmpty()) {
                    compatible = true;
                } else if (gameVersions != null) {
                    for (int j = 0; j < gameVersions.length(); j++) {
                        if (gameVersion.equals(
                                gameVersions.optString(j))) {
                            compatible = true;
                            break;
                        }
                    }
                }

                JSONArray files =
                        version.optJSONArray("files");

                if (files == null || files.length() == 0) {
                    continue;
                }

                JSONObject selected = null;

                for (int j = 0; j < files.length(); j++) {
                    JSONObject file = files.getJSONObject(j);

                    if (file.optBoolean("primary", false)) {
                        selected = file;
                        break;
                    }
                }

                if (selected == null) {
                    selected = files.getJSONObject(0);
                }

                String filename =
                        selected.optString("filename");

                String downloadUrl =
                        selected.optString("url");

                if (filename.isEmpty() || downloadUrl.isEmpty()) {
                    continue;
                }

                if (!filename.toLowerCase().endsWith(".jar")) {
                    continue;
                }

                ModVersion modVersion = new ModVersion(
                        versionName,
                        gameVersion,
                        compatible,
                        downloadUrl,
                        filename
                );

                mod.versions.add(modVersion);
            }

        } catch (Exception ignored) {
        }
    }

    public static void downloadToMods(
            Context context,
            String url,
            String fileName
    ) throws Exception {

        Instance instance = Instances.loadSelectedInstance();

        if (instance == null) {
            throw new Exception("No selected instance");
        }

        File gameDir = instance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");

        if (!modsDir.exists() && !modsDir.mkdirs()) {
            throw new Exception("Cannot create mods directory");
        }

        File target = new File(modsDir, fileName);

        HttpURLConnection connection =
                (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        InputStream input = connection.getInputStream();

        FileOutputStream output =
                new FileOutputStream(target);

        byte[] buffer = new byte[8192];
        int read;

        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        output.close();
        input.close();
        connection.disconnect();
    }

    static class ModItem {
        final String title;
        final String description;
        final String projectId;
        final String iconUrl;

        final List<ModVersion> versions = new ArrayList<>();

        ModItem(
                String title,
                String description,
                String projectId,
                String iconUrl
        ) {
            this.title = title;
            this.description = description;
            this.projectId = projectId;
            this.iconUrl = iconUrl;
        }
    }

    static class ModVersion {
        final String versionName;
        final String gameVersion;
        final boolean compatible;
        final String downloadUrl;
        final String fileName;

        ModVersion(
                String versionName,
                String gameVersion,
                boolean compatible,
                String downloadUrl,
                String fileName
        ) {
            this.versionName = versionName;
            this.gameVersion = gameVersion;
            this.compatible = compatible;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
        }
    }
}
