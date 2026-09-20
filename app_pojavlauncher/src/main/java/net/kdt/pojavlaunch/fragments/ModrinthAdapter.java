package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.kdt.pojavlaunch.R;

public class ModrinthAdapter
        extends RecyclerView.Adapter<ModrinthAdapter.Holder> {

    private final List<ModrinthFragment.ModItem> items = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void setItems(List<ModrinthFragment.ModItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_modrinth, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ModrinthFragment.ModItem item = items.get(position);

        holder.title.setText(item.title);
        holder.description.setText(item.description);
        holder.icon.setImageResource(android.R.drawable.ic_menu_gallery);

        if (item.iconUrl != null && !item.iconUrl.isEmpty()) {
            executor.execute(() -> {
                try {
                    HttpURLConnection connection =
                            (HttpURLConnection) new URL(item.iconUrl).openConnection();

                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);

                    input.close();
                    connection.disconnect();

                    mainHandler.post(() -> {
                        if (bitmap != null &&
                                holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                            holder.icon.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception ignored) {
                }
            });
        }

        holder.download.setOnClickListener(v -> showVersions(v, item));
    }

    private void showVersions(View view, ModrinthFragment.ModItem item) {
        if (item.versions == null || item.versions.isEmpty()) {
            Toast.makeText(
                    view.getContext(),
                    "لم يتم العثور على إصدارات لهذا المود",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        final ArrayList<ModrinthFragment.ModVersion> versions =
                new ArrayList<>(item.versions);

        ArrayAdapter<ModrinthFragment.ModVersion> adapter =
                new ArrayAdapter<ModrinthFragment.ModVersion>(
                        view.getContext(),
                        android.R.layout.simple_list_item_1,
                        versions
                ) {
                    @Override
                    public View getView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent) {

                        TextView text = (TextView) super.getView(
                                position,
                                convertView,
                                parent
                        );

                        ModrinthFragment.ModVersion version =
                                versions.get(position);

                        text.setText(
                                version.gameVersion
                                        + "  •  "
                                        + version.versionName
                        );

                        text.setTextSize(16);
                        text.setPadding(24, 20, 24, 20);

                        if (version.compatible) {
                            text.setTextColor(Color.WHITE);
                            text.setEnabled(true);
                        } else {
                            text.setTextColor(Color.RED);
                            text.setEnabled(false);
                        }

                        return text;
                    }
                };

        AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                .setTitle("اختر إصدار المود")
                .setAdapter(adapter, null)
                .setNegativeButton("إلغاء", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.ListView list = dialog.getListView();

            list.setOnItemClickListener((parent, child, position, id) -> {
                ModrinthFragment.ModVersion version =
                        versions.get(position);

                if (!version.compatible) {
                    return;
                }

                dialog.dismiss();

                downloadMod(
                        view,
                        item,
                        version
                );
            });
        });

        dialog.show();
    }

    private void downloadMod(
            View view,
            ModrinthFragment.ModItem item,
            ModrinthFragment.ModVersion version) {

        if (version.downloadUrl == null ||
                version.downloadUrl.isEmpty()) {

            Toast.makeText(
                    view.getContext(),
                    "لم يتم العثور على ملف لهذا الإصدار",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Toast.makeText(
                view.getContext(),
                "جاري تحميل " + item.title,
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {
            try {
                ModrinthFragment.downloadToMods(
                        view.getContext(),
                        version.downloadUrl,
                        version.fileName
                );

                mainHandler.post(() ->
                        Toast.makeText(
                                view.getContext(),
                                "تم تحميل " + item.title,
                                Toast.LENGTH_LONG
                        ).show());

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(
                                view.getContext(),
                                "فشل تحميل المود",
                                Toast.LENGTH_LONG
                        ).show());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView description;
        final Button download;

        Holder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.mod_icon);
            title = itemView.findViewById(R.id.mod_title);
            description = itemView.findViewById(R.id.mod_description);
            download = itemView.findViewById(R.id.mod_download);
        }
    }
}
