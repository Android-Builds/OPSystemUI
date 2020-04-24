package com.oneplus.lib.preference;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceGroupAdapter extends BaseAdapter implements OnPreferenceChangeInternalListener {
    private static LayoutParams sWrapperLayoutParams = new LayoutParams(-1, -2);
    private Handler mHandler = new Handler();
    private boolean mHasReturnedViewTypeCount = false;
    private Drawable mHighlightedDrawable;
    private int mHighlightedPosition = -1;
    private volatile boolean mIsSyncing = false;
    private PreferenceGroup mPreferenceGroup;
    private ArrayList<PreferenceLayout> mPreferenceLayouts;
    private List<Preference> mPreferenceList;
    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            PreferenceGroupAdapter.this.syncMyPreferences();
        }
    };
    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

    private static class PreferenceLayout implements Comparable<PreferenceLayout> {
        /* access modifiers changed from: private */
        public String name;
        /* access modifiers changed from: private */
        public int resId;
        /* access modifiers changed from: private */
        public int widgetResId;

        private PreferenceLayout() {
        }

        public int compareTo(PreferenceLayout preferenceLayout) {
            int compareTo = this.name.compareTo(preferenceLayout.name);
            if (compareTo == 0) {
                int i = this.resId;
                int i2 = preferenceLayout.resId;
                if (i == i2) {
                    int i3 = this.widgetResId;
                    int i4 = preferenceLayout.widgetResId;
                    if (i3 == i4) {
                        return 0;
                    }
                    return i3 - i4;
                }
                compareTo = i - i2;
            }
            return compareTo;
        }
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean hasStableIds() {
        return true;
    }

    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        this.mPreferenceGroup = preferenceGroup;
        this.mPreferenceGroup.setOnPreferenceChangeInternalListener(this);
        this.mPreferenceList = new ArrayList();
        this.mPreferenceLayouts = new ArrayList<>();
        syncMyPreferences();
    }

    /* access modifiers changed from: private */
    public void syncMyPreferences() {
        synchronized (this) {
            if (!this.mIsSyncing) {
                this.mIsSyncing = true;
                ArrayList arrayList = new ArrayList(this.mPreferenceList.size());
                flattenPreferenceGroup(arrayList, this.mPreferenceGroup);
                this.mPreferenceList = arrayList;
                notifyDataSetChanged();
                synchronized (this) {
                    this.mIsSyncing = false;
                    notifyAll();
                }
            }
        }
    }

    private void flattenPreferenceGroup(List<Preference> list, PreferenceGroup preferenceGroup) {
        preferenceGroup.sortPreferences();
        int preferenceCount = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceGroup.getPreference(i);
            list.add(preference);
            if (!this.mHasReturnedViewTypeCount && preference.canRecycleLayout()) {
                addPreferenceClassName(preference);
            }
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup2 = (PreferenceGroup) preference;
                if (preferenceGroup2.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(list, preferenceGroup2);
                }
            }
            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout preferenceLayout) {
        if (preferenceLayout == null) {
            preferenceLayout = new PreferenceLayout();
        }
        preferenceLayout.name = preference.getClass().getName();
        preferenceLayout.resId = preference.getLayoutResource();
        preferenceLayout.widgetResId = preference.getWidgetLayoutResource();
        return preferenceLayout;
    }

    private void addPreferenceClassName(Preference preference) {
        PreferenceLayout createPreferenceLayout = createPreferenceLayout(preference, null);
        int binarySearch = Collections.binarySearch(this.mPreferenceLayouts, createPreferenceLayout);
        if (binarySearch < 0) {
            this.mPreferenceLayouts.add((binarySearch * -1) - 1, createPreferenceLayout);
        }
    }

    public int getCount() {
        return this.mPreferenceList.size();
    }

    public Preference getItem(int i) {
        if (i < 0 || i >= getCount()) {
            return null;
        }
        return (Preference) this.mPreferenceList.get(i);
    }

    public long getItemId(int i) {
        if (i < 0 || i >= getCount()) {
            return Long.MIN_VALUE;
        }
        return getItem(i).getId();
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        Preference item = getItem(i);
        this.mTempPreferenceLayout = createPreferenceLayout(item, this.mTempPreferenceLayout);
        if (Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout) < 0 || getItemViewType(i) == getHighlightItemViewType()) {
            view = null;
        }
        View view2 = item.getView(view, viewGroup);
        if (i != this.mHighlightedPosition || this.mHighlightedDrawable == null) {
            return view2;
        }
        FrameLayout frameLayout = new FrameLayout(viewGroup.getContext());
        frameLayout.setLayoutParams(sWrapperLayoutParams);
        frameLayout.setBackgroundDrawable(this.mHighlightedDrawable);
        frameLayout.addView(view2);
        return frameLayout;
    }

    public boolean isEnabled(int i) {
        if (i < 0 || i >= getCount()) {
            return true;
        }
        return getItem(i).isSelectable();
    }

    public void onPreferenceChange(Preference preference) {
        notifyDataSetChanged();
    }

    private int getHighlightItemViewType() {
        return getViewTypeCount() - 1;
    }

    public int getItemViewType(int i) {
        if (i == this.mHighlightedPosition) {
            return getHighlightItemViewType();
        }
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        Preference item = getItem(i);
        if (!item.canRecycleLayout()) {
            return -1;
        }
        this.mTempPreferenceLayout = createPreferenceLayout(item, this.mTempPreferenceLayout);
        int binarySearch = Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout);
        if (binarySearch < 0) {
            return -1;
        }
        return binarySearch;
    }

    public int getViewTypeCount() {
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        return Math.max(1, this.mPreferenceLayouts.size()) + 1;
    }
}
