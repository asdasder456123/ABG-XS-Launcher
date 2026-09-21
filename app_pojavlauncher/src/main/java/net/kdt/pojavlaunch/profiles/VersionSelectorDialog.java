package net.kdt.pojavlaunch.profiles;

import static net.kdt.pojavlaunch.extra.ExtraCore.getValue;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListView;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.ui.AbgUiManager;

public class VersionSelectorDialog {
    public static void open(Context context, boolean hideCustomVersions, VersionSelectorListener listener) {
        boolean animatedUi = AbgUiManager.isAnimatedUi(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogContent = LayoutInflater.from(context).inflate(
                animatedUi
                        ? R.layout.dialog_expendable_list_view_animated
                        : R.layout.dialog_expendable_list_view,
                null
        );

        ExpandableListView expandableListView =
                dialogContent.findViewById(R.id.expendable_list_view);
        JVersionList jVersionList = (JVersionList) getValue(ExtraConstants.RELEASE_TABLE);
        JVersionList.Version[] versionArray;
        if(jVersionList == null || jVersionList.versions == null) versionArray = new JVersionList.Version[0];
        else versionArray = jVersionList.versions;
        VersionListAdapter adapter = new VersionListAdapter(versionArray, hideCustomVersions, context);

        expandableListView.setAdapter(adapter);
        builder.setView(dialogContent);

        AlertDialog dialog = builder.show();

        if (animatedUi && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    R.drawable.bg_abg_animated_version_dialog
            );

            View decor = dialog.getWindow().getDecorView();
            decor.setAlpha(0f);
            decor.setScaleX(0.94f);
            decor.setScaleY(0.94f);

            decor.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start();
        }

        expandableListView.setOnChildClickListener((parent, v1, groupPosition, childPosition, id) -> {
            String version = adapter.getChild(groupPosition, childPosition);
            listener.onVersionSelected(version, adapter.isSnapshotSelected(groupPosition));
            dialog.dismiss();
            return true;
        });
    }
}
