package com.android.systemui.p007qs.tiles;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Prefs;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DataSaverController.Listener;
import com.android.systemui.statusbar.policy.NetworkController;

/* renamed from: com.android.systemui.qs.tiles.DataSaverTile */
public class DataSaverTile extends QSTileImpl<BooleanState> implements Listener {
    private static final Intent DATA_SAVER_SETTINGS = new Intent("com.oneplus.action.DATAUSAGE_SAVER");
    private final DataSaverController mDataSaverController;

    public int getMetricsCategory() {
        return 284;
    }

    public void handleSetListening(boolean z) {
    }

    public DataSaverTile(QSHost qSHost, NetworkController networkController) {
        super(qSHost);
        this.mDataSaverController = networkController.getDataSaverController();
        this.mDataSaverController.observe(getLifecycle(), this);
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public boolean isAvailable() {
        return ActivityManager.getCurrentUser() == 0;
    }

    public Intent getLongClickIntent() {
        return DATA_SAVER_SETTINGS;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (!((BooleanState) this.mState).value) {
            String str = "QsDataSaverDialogShown";
            if (!Prefs.getBoolean(this.mContext, str, false)) {
                SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
                systemUIDialog.setTitle(17039816);
                systemUIDialog.setMessage(17039814);
                systemUIDialog.setPositiveButton(17039815, new OnClickListener() {
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DataSaverTile.this.lambda$handleClick$0$DataSaverTile(dialogInterface, i);
                    }
                });
                systemUIDialog.setNegativeButton(17039360, null);
                systemUIDialog.setShowForAllUsers(true);
                systemUIDialog.show();
                Prefs.putBoolean(this.mContext, str, true);
                return;
            }
        }
        toggleDataSaver();
    }

    public /* synthetic */ void lambda$handleClick$0$DataSaverTile(DialogInterface dialogInterface, int i) {
        toggleDataSaver();
    }

    private void toggleDataSaver() {
        ((BooleanState) this.mState).value = !this.mDataSaverController.isDataSaverEnabled();
        this.mDataSaverController.setDataSaverEnabled(((BooleanState) this.mState).value);
        refreshState(Boolean.valueOf(((BooleanState) this.mState).value));
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.data_saver);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        boolean z;
        int i;
        if (obj instanceof Boolean) {
            z = ((Boolean) obj).booleanValue();
        } else {
            z = this.mDataSaverController.isDataSaverEnabled();
        }
        booleanState.value = z;
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.label = this.mContext.getString(R$string.data_saver);
        booleanState.contentDescription = booleanState.label;
        if (booleanState.value) {
            i = R$drawable.ic_data_saver;
        } else {
            i = R$drawable.ic_data_saver_off;
        }
        booleanState.icon = ResourceIcon.get(i);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_data_saver_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_data_saver_changed_off);
    }

    public void onDataSaverChanged(boolean z) {
        refreshState(Boolean.valueOf(z));
    }
}
