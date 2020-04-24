package com.oneplus.lib.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.lib.preference.Preference.BaseSavedState;
import com.oneplus.lib.util.OPFeaturesUtils;
import com.oneplus.lib.util.VibratorSceneUtils;

public final class PreferenceScreen extends PreferenceGroup implements OnItemClickListener, OnDismissListener {
    private Dialog mDialog;
    private ListView mListView;
    private ListAdapter mRootAdapter;
    private long[] mVibratePattern;
    private Vibrator mVibrator;

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        Bundle dialogBundle;
        boolean isDialogShowing;

        public SavedState(Parcel parcel) {
            super(parcel);
            boolean z = true;
            if (parcel.readInt() != 1) {
                z = false;
            }
            this.isDialogShowing = z;
            this.dialogBundle = parcel.readBundle();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.isDialogShowing ? 1 : 0);
            parcel.writeBundle(this.dialogBundle);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isOnSameScreenAsChildren() {
        return false;
    }

    public PreferenceScreen(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, R$attr.op_preferenceScreenStyle);
        if (OPFeaturesUtils.isSupportXVibrate()) {
            this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        }
    }

    public ListAdapter getRootAdapter() {
        if (this.mRootAdapter == null) {
            this.mRootAdapter = onCreateRootAdapter();
        }
        return this.mRootAdapter;
    }

    /* access modifiers changed from: protected */
    public ListAdapter onCreateRootAdapter() {
        return new PreferenceGroupAdapter(this);
    }

    public void bind(ListView listView) {
        listView.setOnItemClickListener(this);
        listView.setAdapter(getRootAdapter());
        onAttachedToActivity();
    }

    /* access modifiers changed from: protected */
    public void onClick() {
        if (getIntent() == null && getFragment() == null && getPreferenceCount() != 0) {
            showDialog(null);
            throw null;
        }
    }

    private void showDialog(Bundle bundle) {
        Context context = getContext();
        ListView listView = this.mListView;
        if (listView != null) {
            listView.setAdapter(null);
        }
        View inflate = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R$layout.op_preference_list_fragment, null);
        this.mListView = (ListView) inflate.findViewById(16908298);
        bind(this.mListView);
        CharSequence title = getTitle();
        Dialog dialog = new Dialog(context, 0);
        this.mDialog = dialog;
        if (TextUtils.isEmpty(title)) {
            dialog.getWindow().requestFeature(1);
        } else {
            dialog.setTitle(title);
        }
        dialog.setContentView(inflate);
        dialog.setOnDismissListener(this);
        if (bundle != null) {
            dialog.onRestoreInstanceState(bundle);
        }
        getPreferenceManager().addPreferencesScreen(dialog);
        throw null;
    }

    public void onDismiss(DialogInterface dialogInterface) {
        this.mDialog = null;
        getPreferenceManager().removePreferencesScreen(dialogInterface);
        throw null;
    }

    public void onItemClick(AdapterView adapterView, View view, int i, long j) {
        if (adapterView instanceof ListView) {
            i -= ((ListView) adapterView).getHeaderViewsCount();
        }
        Object item = getRootAdapter().getItem(i);
        if (item instanceof Preference) {
            Preference preference = (Preference) item;
            if ((item instanceof SwitchPreference) && VibratorSceneUtils.systemVibrateEnabled(getContext())) {
                this.mVibratePattern = VibratorSceneUtils.getVibratorScenePattern(getContext(), this.mVibrator, 1003);
                VibratorSceneUtils.vibrateIfNeeded(this.mVibratePattern, this.mVibrator);
            }
            preference.performClick(this);
        }
    }
}
