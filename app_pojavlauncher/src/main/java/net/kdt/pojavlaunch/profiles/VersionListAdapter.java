package net.kdt.pojavlaunch.profiles;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.FilteredSubList;
import net.kdt.pojavlaunch.ui.AbgUiManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class VersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
    
    private final LayoutInflater mLayoutInflater;

    private final String[] mGroups;
    private final String[] mInstalledVersions;
    private final List<?>[] mData;
    private final boolean mHideCustomVersions;
    private final int mSnapshotListPosition;
    private final boolean mAnimatedUi;

    public VersionListAdapter(JVersionList.Version[] versionList, boolean hideCustomVersions, Context ctx){
        mHideCustomVersions = hideCustomVersions;
        mAnimatedUi = AbgUiManager.isAnimatedUi(ctx);
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        List<JVersionList.Version> releaseList = new FilteredSubList<>(versionList, item -> item.type.equals("release"));
        List<JVersionList.Version> snapshotList = new FilteredSubList<>(versionList, item -> item.type.equals("snapshot"));
        List<JVersionList.Version> betaList = new FilteredSubList<>(versionList, item -> item.type.equals("old_beta"));
        List<JVersionList.Version> alphaList = new FilteredSubList<>(versionList, item -> item.type.equals("old_alpha"));

        // Query installed versions
        mInstalledVersions = new File(Tools.DIR_GAME_NEW + "/versions").list();
        if(mInstalledVersions != null)
            Arrays.sort(mInstalledVersions);

        if(!areInstalledVersionsAvailable()){
            mGroups = new String[]{
                    ctx.getString(R.string.mcl_setting_veroption_release),
                    ctx.getString(R.string.mcl_setting_veroption_snapshot),
                    ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                    ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            };
            mData = new List[]{ releaseList, snapshotList, betaList, alphaList};
            mSnapshotListPosition = 1;
        }else{
            mGroups = new String[]{
                    ctx.getString(R.string.mcl_setting_veroption_installed),
                    ctx.getString(R.string.mcl_setting_veroption_release),
                    ctx.getString(R.string.mcl_setting_veroption_snapshot),
                    ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                    ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            };
            mData = new List[]{Arrays.asList(mInstalledVersions), releaseList, snapshotList, betaList, alphaList};
            mSnapshotListPosition = 2;
        }
    }

    @Override
    public int getGroupCount() {
        return mGroups.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mData[groupPosition].size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mData[groupPosition];
    }

    @Override
    public String getChild(int groupPosition, int childPosition) {
        if(isInstalledVersionSelected(groupPosition)){
            return mInstalledVersions[childPosition];
        }
        return ((JVersionList.Version)mData[groupPosition].get(childPosition)).id;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (!mAnimatedUi) {
            if(convertView == null)
                convertView = mLayoutInflater.inflate(
                        android.R.layout.simple_expandable_list_item_1,
                        parent,
                        false
                );

            ((TextView) convertView).setText(mGroups[groupPosition]);
            return convertView;
        }

        if (convertView == null || convertView.findViewById(R.id.version_group_title) == null) {
            convertView = mLayoutInflater.inflate(
                    R.layout.item_version_group_animated,
                    parent,
                    false
            );
        }

        TextView title = convertView.findViewById(R.id.version_group_title);
        TextView arrow = convertView.findViewById(R.id.version_group_arrow);

        title.setText(mGroups[groupPosition]);
        arrow.setText(isExpanded ? "⌄" : "›");

        convertView.setScaleX(0.98f);
        convertView.setScaleY(0.98f);
        convertView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(140)
                .start();

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (!mAnimatedUi) {
            if(convertView == null)
                convertView = mLayoutInflater.inflate(
                        android.R.layout.simple_expandable_list_item_1,
                        parent,
                        false
                );

            ((TextView) convertView).setText(getChild(groupPosition, childPosition));
            return convertView;
        }

        if (convertView == null || convertView.findViewById(R.id.version_child_title) == null) {
            convertView = mLayoutInflater.inflate(
                    R.layout.item_version_child_animated,
                    parent,
                    false
            );
        }

        TextView title = convertView.findViewById(R.id.version_child_title);
        title.setText(getChild(groupPosition, childPosition));

        convertView.setAlpha(0.7f);
        convertView.setTranslationX(12f);
        convertView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .start();

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean isSnapshotSelected(int groupPosition) {
        return groupPosition == mSnapshotListPosition;
    }

    private boolean areInstalledVersionsAvailable(){
        if(mHideCustomVersions) return false;
        return !(mInstalledVersions == null || mInstalledVersions.length == 0);
    }

    private boolean isInstalledVersionSelected(int groupPosition){
        return groupPosition == 0 && areInstalledVersionsAvailable();
    }
}
