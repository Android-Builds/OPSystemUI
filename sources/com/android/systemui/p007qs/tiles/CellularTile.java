package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.airbnb.lottie.C0526R;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.net.DataUsageController.DataUsageInfo;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSTile.SignalState;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;

/* renamed from: com.android.systemui.qs.tiles.CellularTile */
public class CellularTile extends QSTileImpl<SignalState> {
    static final Intent CELLULAR_SETTINGS = new Intent("com.oneplus.security.action.USAGE_DATA_SUMMARY");
    private final ActivityStarter mActivityStarter;
    private final NetworkController mController;
    /* access modifiers changed from: private */
    public final DataUsageController mDataController;
    /* access modifiers changed from: private */
    public final CellularDetailAdapter mDetailAdapter;
    /* access modifiers changed from: private */
    public final CellSignalCallback mSignalCallback = new CellSignalCallback();
    private SubscriptionManager mSubscriptionManager = null;

    /* renamed from: com.android.systemui.qs.tiles.CellularTile$CallbackInfo */
    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        boolean noSim;
        boolean roaming;

        private CallbackInfo() {
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.CellularTile$CellSignalCallback */
    private final class CellSignalCallback implements SignalCallback {
        /* access modifiers changed from: private */
        public final CallbackInfo mInfo;

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        public void setNoSims(boolean z, boolean z2) {
            CallbackInfo callbackInfo = this.mInfo;
            callbackInfo.noSim = z;
            CellularTile.this.refreshState(callbackInfo);
        }

        public void setIsAirplaneMode(IconState iconState) {
            CallbackInfo callbackInfo = this.mInfo;
            callbackInfo.airplaneModeEnabled = iconState.visible;
            CellularTile.this.refreshState(callbackInfo);
        }

        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(z);
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.CellularTile$CellularDetailAdapter */
    private final class CellularDetailAdapter implements DetailAdapter {
        public int getMetricsCategory() {
            return 117;
        }

        private CellularDetailAdapter() {
        }

        public CharSequence getTitle() {
            return CellularTile.this.mContext.getString(R$string.quick_settings_cellular_detail_title);
        }

        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        public Intent getSettingsIntent() {
            return CellularTile.CELLULAR_SETTINGS;
        }

        public void setToggleState(boolean z) {
            MetricsLogger.action(CellularTile.this.mContext, 155, z);
            CellularTile.this.mDataController.setMobileDataEnabled(z);
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            int i = 0;
            if (view == null) {
                view = LayoutInflater.from(CellularTile.this.mContext).inflate(R$layout.data_usage, viewGroup, false);
            }
            DataUsageDetailView dataUsageDetailView = (DataUsageDetailView) view;
            DataUsageInfo dataUsageInfo = CellularTile.this.mDataController.getDataUsageInfo();
            if (dataUsageInfo == null) {
                return dataUsageDetailView;
            }
            dataUsageDetailView.bind(dataUsageInfo);
            View findViewById = dataUsageDetailView.findViewById(R$id.roaming_text);
            if (!CellularTile.this.mSignalCallback.mInfo.roaming) {
                i = 4;
            }
            findViewById.setVisibility(i);
            return dataUsageDetailView;
        }

        public void setMobileDataEnabled(boolean z) {
            CellularTile.this.fireToggleStateChanged(z);
        }
    }

    public int getMetricsCategory() {
        return C0526R.styleable.AppCompatTheme_tooltipFrameBackground;
    }

    public void handleSetListening(boolean z) {
    }

    public CellularTile(QSHost qSHost, NetworkController networkController, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = networkController;
        this.mActivityStarter = activityStarter;
        this.mDataController = this.mController.getMobileDataController();
        this.mDetailAdapter = new CellularDetailAdapter();
        this.mController.observe(getLifecycle(), this.mSignalCallback);
        CELLULAR_SETTINGS.putExtra("tracker_event", 2);
    }

    public SignalState newTileState() {
        return new SignalState();
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x0043  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0065  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.content.Intent getLongClickIntent() {
        /*
            r6 = this;
            java.lang.String r0 = "gsm.sim.state"
            java.lang.String r0 = android.os.SystemProperties.get(r0)
            r1 = 0
            java.lang.String r2 = ","
            java.lang.String[] r0 = android.text.TextUtils.split(r0, r2)     // Catch:{ Exception -> 0x0034 }
            r2 = r1
            r3 = r2
        L_0x000f:
            int r4 = r0.length     // Catch:{ Exception -> 0x0035 }
            if (r2 >= r4) goto L_0x003c
            r4 = r0[r2]     // Catch:{ Exception -> 0x0035 }
            boolean r4 = r4.isEmpty()     // Catch:{ Exception -> 0x0035 }
            if (r4 != 0) goto L_0x0031
            r4 = r0[r2]     // Catch:{ Exception -> 0x0035 }
            java.lang.String r5 = "ABSENT"
            boolean r4 = r4.equalsIgnoreCase(r5)     // Catch:{ Exception -> 0x0035 }
            if (r4 != 0) goto L_0x0031
            r4 = r0[r2]     // Catch:{ Exception -> 0x0035 }
            java.lang.String r5 = "NOT_READY"
            boolean r4 = r4.equalsIgnoreCase(r5)     // Catch:{ Exception -> 0x0035 }
            if (r4 == 0) goto L_0x002f
            goto L_0x0031
        L_0x002f:
            int r3 = r3 + 1
        L_0x0031:
            int r2 = r2 + 1
            goto L_0x000f
        L_0x0034:
            r3 = r1
        L_0x0035:
            java.lang.String r0 = r6.TAG
            java.lang.String r2 = "Error to parse sim state"
            android.util.Log.e(r0, r2)
        L_0x003c:
            android.content.Intent r0 = CELLULAR_SETTINGS
            java.lang.String r2 = "select_tab"
            r4 = 1
            if (r3 <= r4) goto L_0x0065
            int r1 = r6.getDefaultDataSimIndex()
            r0.putExtra(r2, r1)
            java.lang.String r1 = r6.TAG
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Go to selected sim tab="
            r2.append(r3)
            int r6 = r6.getDefaultDataSimIndex()
            r2.append(r6)
            java.lang.String r6 = r2.toString()
            android.util.Log.d(r1, r6)
            goto L_0x006f
        L_0x0065:
            r0.putExtra(r2, r1)
            java.lang.String r6 = r6.TAG
            java.lang.String r1 = "Go to sim tab 0"
            android.util.Log.d(r6, r1)
        L_0x006f:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.tiles.CellularTile.getLongClickIntent():android.content.Intent");
    }

    private int getDefaultDataSimIndex() {
        if (this.mSubscriptionManager == null) {
            this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        }
        return this.mSubscriptionManager.getDefaultDataPhoneId();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((SignalState) getState()).state != 0) {
            DataUsageController dataUsageController = this.mDataController;
            dataUsageController.setMobileDataEnabled(!dataUsageController.isMobileDataEnabled());
        }
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        if (this.mDataController.isMobileDataSupported()) {
            showDetail(true);
        } else {
            this.mActivityStarter.postStartActivityDismissingKeyguard(getCellularSettingIntent(), 0);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_cellular_detail_title);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(SignalState signalState, Object obj) {
        Object obj2;
        CallbackInfo callbackInfo = (CallbackInfo) obj;
        if (callbackInfo == null) {
            callbackInfo = this.mSignalCallback.mInfo;
        }
        Resources resources = this.mContext.getResources();
        signalState.label = resources.getString(R$string.mobile_data);
        boolean z = this.mDataController.isMobileDataSupported() && this.mDataController.isMobileDataEnabled();
        signalState.value = z;
        signalState.activityIn = z && callbackInfo.activityIn;
        signalState.activityOut = z && callbackInfo.activityOut;
        signalState.expandedAccessibilityClassName = Switch.class.getName();
        if (callbackInfo.noSim) {
            signalState.icon = ResourceIcon.get(R$drawable.ic_qs_no_sim);
        } else {
            signalState.icon = ResourceIcon.get(R$drawable.ic_swap_vert);
        }
        if (Build.DEBUG_ONEPLUS) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("handleUpdateState: supported=");
            sb.append(this.mDataController.isMobileDataSupported());
            sb.append(", enabled=");
            sb.append(this.mDataController.isMobileDataEnabled());
            sb.append(", noSim=");
            sb.append(callbackInfo.noSim);
            sb.append(", airplaneMode=");
            sb.append(callbackInfo.airplaneModeEnabled);
            Log.d(str, sb.toString());
        }
        if (callbackInfo.noSim) {
            signalState.state = 0;
        } else if (callbackInfo.airplaneModeEnabled) {
            signalState.state = 0;
        } else if (z) {
            signalState.state = 2;
        } else {
            signalState.state = 1;
        }
        if (signalState.state == 1) {
            obj2 = resources.getString(R$string.cell_data_off_content_description);
        } else {
            obj2 = signalState.secondaryLabel;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(signalState.label);
        sb2.append(", ");
        sb2.append(obj2);
        signalState.contentDescription = sb2.toString();
    }

    public boolean isAvailable() {
        return this.mController.hasMobileDataFeature();
    }

    private Intent getCellularSettingIntent() {
        Intent intent = new Intent("codeaurora.intent.action.MOBILE_NETWORK_SETTINGS");
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        Context context = this.mContext;
        String str = "CellularTile";
        if (context == null || intent.resolveActivity(context.getPackageManager()) == null) {
            intent = new Intent("android.settings.NETWORK_OPERATOR_SETTINGS");
            if (defaultDataSubscriptionId != -1) {
                intent.putExtra("android.provider.extra.SUB_ID", SubscriptionManager.getDefaultDataSubscriptionId());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Using default network settings for sub: ");
            sb.append(defaultDataSubscriptionId);
            Log.d(str, sb.toString());
        } else {
            intent.putExtra("slot_id", SubscriptionManager.getSlotIndex(defaultDataSubscriptionId));
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Using vendor network settings for sub: ");
            sb2.append(defaultDataSubscriptionId);
            Log.d(str, sb2.toString());
        }
        return intent;
    }
}
