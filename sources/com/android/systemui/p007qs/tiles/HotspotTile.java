package com.android.systemui.p007qs.tiles;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.Telephony.Carriers;
import android.util.Log;
import android.util.OpFeatures;
import android.widget.Switch;
import androidx.lifecycle.LifecycleOwner;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$plurals;
import com.android.systemui.R$string;
import com.android.systemui.SysUIToast;
import com.android.systemui.p007qs.GlobalSetting;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DataSaverController.Listener;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.HotspotController.Callback;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.oneplus.util.OpUtils;

/* renamed from: com.android.systemui.qs.tiles.HotspotTile */
public class HotspotTile extends QSTileImpl<BooleanState> {
    /* access modifiers changed from: private */
    public static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
    private final HotspotAndDataSaverCallbacks mCallbacks = new HotspotAndDataSaverCallbacks();
    private final DataSaverController mDataSaverController;
    private final Icon mEnabledStatic = ResourceIcon.get(R$drawable.ic_hotspot);
    /* access modifiers changed from: private */
    public final HotspotController mHotspotController;
    private final boolean mIeee80211acSupport;
    private final boolean mIeee80211axSupport;
    private boolean mListening;
    protected final NetworkController mNetworkController;
    /* access modifiers changed from: private */
    public boolean mNoSimError = false;
    /* access modifiers changed from: private */
    public boolean mOperatorDialogShowing = false;
    private final GlobalSetting mOverHeatMode;
    /* access modifiers changed from: private */
    public boolean mReguireTileToGray = false;
    protected final HotspotSignalCallback mSignalCallback = new HotspotSignalCallback();
    /* access modifiers changed from: private */
    public boolean mVirtualSimExist = false;
    private final Icon mWifi5EnabledStatic = ResourceIcon.get(R$drawable.ic_wifi_5_hotspot);
    private final Icon mWifi6EnabledStatic = ResourceIcon.get(R$drawable.ic_wifi_6_hotspot);

    /* renamed from: com.android.systemui.qs.tiles.HotspotTile$CallbackInfo */
    protected static final class CallbackInfo {
        boolean isDataSaverEnabled;
        boolean isHotspotEnabled;
        int numConnectedDevices;

        protected CallbackInfo() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("CallbackInfo[");
            sb.append("isHotspotEnabled=");
            sb.append(this.isHotspotEnabled);
            sb.append(",numConnectedDevices=");
            sb.append(this.numConnectedDevices);
            sb.append(",isDataSaverEnabled=");
            sb.append(this.isDataSaverEnabled);
            sb.append(']');
            return sb.toString();
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.HotspotTile$HotspotAndDataSaverCallbacks */
    private final class HotspotAndDataSaverCallbacks implements Callback, Listener {
        CallbackInfo mCallbackInfo;

        private HotspotAndDataSaverCallbacks() {
            this.mCallbackInfo = new CallbackInfo();
        }

        public void onDataSaverChanged(boolean z) {
            CallbackInfo callbackInfo = this.mCallbackInfo;
            callbackInfo.isDataSaverEnabled = z;
            HotspotTile.this.refreshState(callbackInfo);
        }

        public void onHotspotChanged(boolean z, int i) {
            CallbackInfo callbackInfo = this.mCallbackInfo;
            callbackInfo.isHotspotEnabled = z;
            callbackInfo.numConnectedDevices = i;
            HotspotTile.this.refreshState(callbackInfo);
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.HotspotTile$HotspotSignalCallback */
    protected final class HotspotSignalCallback implements SignalCallback {
        protected HotspotSignalCallback() {
        }

        public void setVirtualSimstate(int[] iArr) {
            boolean z = false;
            if (iArr != null && iArr.length > 0) {
                int length = iArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (iArr[i] == NetworkControllerImpl.SOFTSIM_ENABLE_PILOT) {
                        z = true;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            String access$300 = HotspotTile.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("virtual sim state change: ");
            sb.append(HotspotTile.this.mVirtualSimExist);
            sb.append(" to ");
            sb.append(z);
            Log.d(access$300, sb.toString());
            HotspotTile.this.mVirtualSimExist = z;
            HotspotTile.this.refreshState();
        }

        public void setHasAnySimReady(boolean z) {
            if (OpUtils.isUSS()) {
                if (HotspotTile.DEBUG_ONEPLUS) {
                    String access$600 = HotspotTile.this.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("setHasAnySimReady / simReady:");
                    sb.append(z);
                    Log.i(access$600, sb.toString());
                }
                HotspotTile.this.mNoSimError = !z;
                if (z) {
                    HotspotTile.this.mReguireTileToGray = false;
                    HotspotTile.this.refreshState();
                } else if (!z) {
                    HotspotTile.this.mHotspotController.isHotspotEnabled();
                }
            }
        }
    }

    public int getMetricsCategory() {
        return 120;
    }

    public HotspotTile(QSHost qSHost, HotspotController hotspotController, DataSaverController dataSaverController) {
        super(qSHost);
        this.mHotspotController = hotspotController;
        this.mDataSaverController = dataSaverController;
        this.mHotspotController.observe((LifecycleOwner) this, this.mCallbacks);
        this.mDataSaverController.observe((LifecycleOwner) this, this.mCallbacks);
        this.mIeee80211acSupport = this.mContext.getResources().getBoolean(17891599);
        this.mIeee80211axSupport = this.mContext.getResources().getBoolean(17891600);
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mOverHeatMode = new GlobalSetting(this.mContext, null, "op_overheat_temperature_type") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i) {
            }
        };
    }

    public boolean isAvailable() {
        return this.mHotspotController.isHotspotSupported() && !this.mHotspotController.isOperatorHotspotDisable();
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (z) {
                this.mNetworkController.addCallback(this.mSignalCallback);
                refreshState();
            } else {
                this.mNetworkController.removeCallback(this.mSignalCallback);
            }
            this.mOverHeatMode.setListening(z);
        }
    }

    public Intent getLongClickIntent() {
        Intent intent = new Intent(TETHER_SETTINGS);
        intent.putExtra("from_quick_setting", "1");
        return intent;
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        Object obj;
        if (OpUtils.isUSS()) {
            if (((BooleanState) this.mState).state != 0) {
                if (DEBUG_ONEPLUS) {
                    String str = this.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("handleClick / mNoSimError");
                    sb.append(this.mNoSimError);
                    sb.append(" / !isHaveProfile():");
                    sb.append(!isHaveProfile());
                    Log.d(str, sb.toString());
                }
                if (!isOperatorValid()) {
                    Log.d(this.TAG, "!isOperatorValid() AlertDialog");
                    this.mHotspotController.setHotspotEnabled(false);
                    operatorAlertDialog();
                    refreshState();
                    return;
                }
            } else {
                return;
            }
        }
        boolean z = ((BooleanState) this.mState).value;
        if (!z && this.mDataSaverController.isDataSaverEnabled()) {
            return;
        }
        if (this.mVirtualSimExist) {
            Log.d(this.TAG, "virtual sim exist. ignore click.");
            return;
        }
        if (!z && this.mOverHeatMode.getValue() != 0) {
            Context context = this.mContext;
            SysUIToast.makeText(context, (CharSequence) context.getString(R$string.overheat_toast_content), 1).show();
        }
        if (z) {
            obj = null;
        } else {
            obj = QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
        }
        refreshState(obj);
        this.mHotspotController.setHotspotEnabled(!z);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_hotspot_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        boolean z;
        int i;
        int i2 = 1;
        boolean z2 = obj == QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
        if (booleanState.slash == null) {
            booleanState.slash = new SlashState();
        }
        boolean z3 = z2 || this.mHotspotController.isHotspotTransient();
        checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_config_tethering");
        if (obj instanceof CallbackInfo) {
            CallbackInfo callbackInfo = (CallbackInfo) obj;
            booleanState.value = z2 || callbackInfo.isHotspotEnabled;
            i = callbackInfo.numConnectedDevices;
            z = callbackInfo.isDataSaverEnabled;
        } else {
            booleanState.value = z2 || this.mHotspotController.isHotspotEnabled();
            i = this.mHotspotController.getNumConnectedDevices();
            z = this.mDataSaverController.isDataSaverEnabled();
        }
        booleanState.icon = this.mEnabledStatic;
        booleanState.label = this.mContext.getString(R$string.quick_settings_hotspot_label);
        booleanState.isTransient = z3;
        booleanState.slash.isSlashed = !booleanState.value && !booleanState.isTransient;
        if (!OpUtils.isUST()) {
            if (booleanState.isTransient) {
                booleanState.icon = ResourceIcon.get(17302425);
            } else if (booleanState.value) {
                if (OpFeatures.isSupport(new int[]{226})) {
                    if (this.mIeee80211axSupport) {
                        booleanState.icon = this.mWifi6EnabledStatic;
                    } else if (this.mIeee80211acSupport) {
                        booleanState.icon = this.mWifi5EnabledStatic;
                    }
                }
            }
        }
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.contentDescription = booleanState.label;
        boolean z4 = z || this.mVirtualSimExist || (OpUtils.isUSS() && this.mReguireTileToGray);
        if (this.mVirtualSimExist) {
            Log.d(this.TAG, "virtual sim exist, set to unavailable.");
        }
        boolean z5 = booleanState.value || booleanState.isTransient;
        if (DEBUG_ONEPLUS) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("handleUpdateState / isTileActive:");
            sb.append(z5);
            sb.append(" / state.value:");
            sb.append(booleanState.value);
            sb.append(" / state.isTransient:");
            sb.append(booleanState.isTransient);
            sb.append(" / mReguireTileToGray: ");
            sb.append(this.mReguireTileToGray);
            Log.d(str, sb.toString());
        }
        if (z4) {
            booleanState.state = 0;
        } else {
            if (z5) {
                i2 = 2;
            }
            booleanState.state = i2;
        }
        booleanState.secondaryLabel = getSecondaryLabel(z5, z3, z, i);
    }

    private String getSecondaryLabel(boolean z, boolean z2, boolean z3, int i) {
        if (z2) {
            return this.mContext.getString(R$string.quick_settings_hotspot_secondary_label_transient);
        }
        if (z3) {
            return this.mContext.getString(R$string.quick_settings_hotspot_secondary_label_data_saver_enabled);
        }
        if (i <= 0 || !z) {
            return null;
        }
        return this.mContext.getResources().getQuantityString(R$plurals.quick_settings_hotspot_secondary_label_num_devices, i, new Object[]{Integer.valueOf(i)});
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_hotspot_changed_off);
    }

    private boolean isHaveProfile() {
        if (!OpUtils.isSprintMccMnc(this.mContext)) {
            return true;
        }
        String mmcMnc = OpUtils.getMmcMnc(this.mContext, 1);
        if (mmcMnc == null || mmcMnc.length() == 0) {
            Log.i(this.TAG, "no mccmnc");
            return false;
        }
        Cursor query = this.mContext.getContentResolver().query(Carriers.CONTENT_URI, new String[]{"apn"}, "type = ? and numeric = ? and user_visible != ? and name != ?", new String[]{"dun", mmcMnc, "0", "3G_HOT"}, null);
        if (query == null || query.getCount() <= 0 || !query.moveToFirst()) {
            query.close();
            return false;
        }
        query.close();
        return true;
    }

    private boolean isOperatorValid() {
        if (this.mHotspotController.isHotspotEnabled()) {
            return true;
        }
        if (this.mNoSimError || !isHaveProfile()) {
            return false;
        }
        return true;
    }

    private void operatorAlertDialog() {
        int i;
        int i2;
        int i3;
        if (!this.mOperatorDialogShowing) {
            if (this.mNoSimError) {
                i3 = R$string.hotspot_operator_dialog_nosim_title;
                i2 = R$string.hotspot_operator_dialog_nosim_msg;
                i = R$string.hotspot_operator_dialog_nosim_button;
            } else if (!isHaveProfile()) {
                i3 = R$string.hotspot_operator_dialog_othererror_title;
                i2 = R$string.hotspot_operator_dialog_othererror_msg;
                i = R$string.hotspot_operator_dialog_othererror_button;
            } else {
                i3 = 0;
                i2 = 0;
                i = 0;
            }
            AlertDialog create = new Builder(this.mContext).setMessage(i2).setTitle(i3).setCancelable(false).setPositiveButton(i, new OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    HotspotTile.this.mOperatorDialogShowing = false;
                }
            }).create();
            create.getWindow().setType(2014);
            create.setOnShowListener(new OnShowListener(create) {
                private final /* synthetic */ AlertDialog f$0;

                {
                    this.f$0 = r1;
                }

                public final void onShow(DialogInterface dialogInterface) {
                    this.f$0.getButton(-1).setTextColor(-65536);
                }
            });
            this.mOperatorDialogShowing = true;
            create.show();
        }
    }
}
