package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.AlphaControlledSignalTileView;
import com.android.systemui.p007qs.QSDetailItems;
import com.android.systemui.p007qs.QSDetailItems.Callback;
import com.android.systemui.p007qs.QSDetailItems.Item;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile.SignalState;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController.AccessPointCallback;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.WifiIcons;
import com.oneplus.systemui.statusbar.policy.OpWifiIcons;
import com.oneplus.util.OpUtils;
import java.util.List;

/* renamed from: com.android.systemui.qs.tiles.WifiTile */
public class WifiTile extends QSTileImpl<SignalState> {
    /* access modifiers changed from: private */
    public static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    /* access modifiers changed from: private */
    public final ActivityStarter mActivityStarter;
    private long mClickTimeMillis = -1;
    protected final NetworkController mController;
    /* access modifiers changed from: private */
    public final WifiDetailAdapter mDetailAdapter;
    private boolean mExpectDisabled;
    protected final WifiSignalCallback mSignalCallback = new WifiSignalCallback();
    private final SignalState mStateBeforeClick = newTileState();
    /* access modifiers changed from: private */
    public boolean mTransientEnabling = false;
    /* access modifiers changed from: private */
    public final AccessPointController mWifiController;

    /* renamed from: com.android.systemui.qs.tiles.WifiTile$CallbackInfo */
    protected static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        boolean isTransient;
        String ssid;
        public String statusLabel;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        protected CallbackInfo() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("CallbackInfo[");
            sb.append("enabled=");
            sb.append(this.enabled);
            sb.append(",connected=");
            sb.append(this.connected);
            sb.append(",wifiSignalIconId=");
            sb.append(this.wifiSignalIconId);
            sb.append(",ssid=");
            sb.append(this.ssid);
            sb.append(",activityIn=");
            sb.append(this.activityIn);
            sb.append(",activityOut=");
            sb.append(this.activityOut);
            sb.append(",wifiSignalContentDescription=");
            sb.append(this.wifiSignalContentDescription);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(']');
            return sb.toString();
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.WifiTile$WifiDetailAdapter */
    protected class WifiDetailAdapter implements DetailAdapter, AccessPointCallback, Callback {
        private AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        public int getMetricsCategory() {
            return 152;
        }

        public void onDetailItemDisconnect(Item item) {
        }

        protected WifiDetailAdapter() {
        }

        public CharSequence getTitle() {
            return WifiTile.this.mContext.getString(R$string.quick_settings_wifi_label);
        }

        public Intent getSettingsIntent() {
            return WifiTile.WIFI_SETTINGS;
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((SignalState) WifiTile.this.mState).value);
        }

        public void setToggleState(boolean z) {
            if (QSTileImpl.DEBUG) {
                String access$1000 = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("setToggleState ");
                sb.append(z);
                Log.d(access$1000, sb.toString());
            }
            MetricsLogger.action(WifiTile.this.mContext, 153, z);
            WifiTile.this.mController.setWifiEnabled(z);
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            if (QSTileImpl.DEBUG) {
                String access$1300 = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("createDetailView convertView=");
                sb.append(view != null);
                Log.d(access$1300, sb.toString());
            }
            this.mAccessPoints = null;
            this.mItems = QSDetailItems.convertOrInflate(context, view, viewGroup);
            this.mItems.setTagSuffix("Wifi");
            this.mItems.setCallback(this);
            WifiTile.this.mWifiController.scanForAccessPoints();
            setItemsVisible(((SignalState) WifiTile.this.mState).value);
            return this.mItems;
        }

        public void onAccessPointsChanged(List<AccessPoint> list) {
            this.mAccessPoints = (AccessPoint[]) list.toArray(new AccessPoint[list.size()]);
            filterUnreachableAPs();
            updateItems();
        }

        private void filterUnreachableAPs() {
            int i = 0;
            for (AccessPoint isReachable : this.mAccessPoints) {
                if (isReachable.isReachable()) {
                    i++;
                }
            }
            AccessPoint[] accessPointArr = this.mAccessPoints;
            if (i != accessPointArr.length) {
                this.mAccessPoints = new AccessPoint[i];
                int i2 = 0;
                for (AccessPoint accessPoint : accessPointArr) {
                    if (accessPoint.isReachable()) {
                        int i3 = i2 + 1;
                        this.mAccessPoints[i2] = accessPoint;
                        i2 = i3;
                    }
                }
            }
        }

        public void onSettingsActivityTriggered(Intent intent) {
            WifiTile.this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }

        public void onDetailItemClick(Item item) {
            if (item != null) {
                Object obj = item.tag;
                if (obj != null) {
                    AccessPoint accessPoint = (AccessPoint) obj;
                    if (!accessPoint.isActive() && WifiTile.this.mWifiController.connect(accessPoint)) {
                        WifiTile.this.mHost.collapsePanels();
                    }
                    WifiTile.this.showDetail(false);
                }
            }
        }

        public void setItemsVisible(boolean z) {
            QSDetailItems qSDetailItems = this.mItems;
            if (qSDetailItems != null) {
                qSDetailItems.setItemsVisible(z);
            }
        }

        /* access modifiers changed from: private */
        /* JADX WARNING: Removed duplicated region for block: B:13:0x002c  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x004b  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void updateItems() {
            /*
                r6 = this;
                com.android.systemui.qs.QSDetailItems r0 = r6.mItems
                if (r0 != 0) goto L_0x0005
                return
            L_0x0005:
                com.android.settingslib.wifi.AccessPoint[] r0 = r6.mAccessPoints
                r1 = 0
                if (r0 == 0) goto L_0x000d
                int r0 = r0.length
                if (r0 > 0) goto L_0x0017
            L_0x000d:
                com.android.systemui.qs.tiles.WifiTile r0 = com.android.systemui.p007qs.tiles.WifiTile.this
                com.android.systemui.qs.tiles.WifiTile$WifiSignalCallback r2 = r0.mSignalCallback
                com.android.systemui.qs.tiles.WifiTile$CallbackInfo r2 = r2.mInfo
                boolean r2 = r2.enabled
                if (r2 != 0) goto L_0x001d
            L_0x0017:
                com.android.systemui.qs.tiles.WifiTile r0 = com.android.systemui.p007qs.tiles.WifiTile.this
                r0.fireScanStateChanged(r1)
                goto L_0x0021
            L_0x001d:
                r2 = 1
                r0.fireScanStateChanged(r2)
            L_0x0021:
                com.android.systemui.qs.tiles.WifiTile r0 = com.android.systemui.p007qs.tiles.WifiTile.this
                com.android.systemui.qs.tiles.WifiTile$WifiSignalCallback r0 = r0.mSignalCallback
                com.android.systemui.qs.tiles.WifiTile$CallbackInfo r0 = r0.mInfo
                boolean r0 = r0.enabled
                r2 = 0
                if (r0 != 0) goto L_0x004b
                boolean r0 = com.oneplus.util.OpUtils.isUSS()
                if (r0 == 0) goto L_0x003c
                com.android.systemui.qs.QSDetailItems r0 = r6.mItems
                int r1 = com.oneplus.systemui.statusbar.policy.OpWifiIcons.OP_QS_WIFI_NO_NETWORK
                int r3 = com.android.systemui.R$string.wifi_is_off
                r0.setEmptyState(r1, r3)
                goto L_0x0045
            L_0x003c:
                com.android.systemui.qs.QSDetailItems r0 = r6.mItems
                int r1 = com.android.systemui.statusbar.policy.WifiIcons.QS_WIFI_NO_NETWORK
                int r3 = com.android.systemui.R$string.wifi_is_off
                r0.setEmptyState(r1, r3)
            L_0x0045:
                com.android.systemui.qs.QSDetailItems r6 = r6.mItems
                r6.setItems(r2)
                return
            L_0x004b:
                boolean r0 = com.oneplus.util.OpUtils.isUSS()
                if (r0 == 0) goto L_0x005b
                com.android.systemui.qs.QSDetailItems r0 = r6.mItems
                int r3 = com.oneplus.systemui.statusbar.policy.OpWifiIcons.OP_QS_WIFI_NO_NETWORK
                int r4 = com.android.systemui.R$string.quick_settings_wifi_detail_empty_text
                r0.setEmptyState(r3, r4)
                goto L_0x0064
            L_0x005b:
                com.android.systemui.qs.QSDetailItems r0 = r6.mItems
                int r3 = com.android.systemui.statusbar.policy.WifiIcons.QS_WIFI_NO_NETWORK
                int r4 = com.android.systemui.R$string.quick_settings_wifi_detail_empty_text
                r0.setEmptyState(r3, r4)
            L_0x0064:
                com.android.settingslib.wifi.AccessPoint[] r0 = r6.mAccessPoints
                if (r0 == 0) goto L_0x00aa
                int r0 = r0.length
                com.android.systemui.qs.QSDetailItems$Item[] r0 = new com.android.systemui.p007qs.QSDetailItems.Item[r0]
            L_0x006b:
                com.android.settingslib.wifi.AccessPoint[] r3 = r6.mAccessPoints
                int r4 = r3.length
                if (r1 >= r4) goto L_0x00ab
                r3 = r3[r1]
                com.android.systemui.qs.QSDetailItems$Item r4 = new com.android.systemui.qs.QSDetailItems$Item
                r4.<init>()
                r4.tag = r3
                com.android.systemui.qs.tiles.WifiTile r5 = com.android.systemui.p007qs.tiles.WifiTile.this
                com.android.systemui.statusbar.policy.NetworkController$AccessPointController r5 = r5.mWifiController
                int r5 = r5.getIcon(r3)
                r4.iconResId = r5
                java.lang.CharSequence r5 = r3.getSsid()
                r4.line1 = r5
                boolean r5 = r3.isActive()
                if (r5 == 0) goto L_0x0096
                java.lang.String r5 = r3.getSummary()
                goto L_0x0097
            L_0x0096:
                r5 = r2
            L_0x0097:
                r4.line2 = r5
                int r3 = r3.getSecurity()
                if (r3 == 0) goto L_0x00a2
                int r3 = com.android.systemui.R$drawable.qs_ic_wifi_lock
                goto L_0x00a3
            L_0x00a2:
                r3 = -1
            L_0x00a3:
                r4.icon2 = r3
                r0[r1] = r4
                int r1 = r1 + 1
                goto L_0x006b
            L_0x00aa:
                r0 = r2
            L_0x00ab:
                com.android.systemui.qs.QSDetailItems r6 = r6.mItems
                r6.setItems(r0)
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.tiles.WifiTile.WifiDetailAdapter.updateItems():void");
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.WifiTile$WifiSignalCallback */
    protected final class WifiSignalCallback implements SignalCallback {
        final CallbackInfo mInfo = new CallbackInfo();

        protected WifiSignalCallback() {
        }

        public void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
            if (QSTileImpl.DEBUG) {
                String access$100 = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onWifiSignalChanged: enabled=");
                sb.append(z);
                sb.append(", isTrans=");
                sb.append(z4);
                sb.append(", conn=");
                sb.append(iconState2.visible);
                sb.append(", actIn=");
                sb.append(z2);
                sb.append(", actOut=");
                sb.append(z3);
                sb.append(", label=");
                sb.append(str2);
                Log.d(access$100, sb.toString());
            }
            CallbackInfo callbackInfo = this.mInfo;
            callbackInfo.enabled = z;
            callbackInfo.connected = iconState2.visible;
            callbackInfo.wifiSignalIconId = iconState2.icon;
            callbackInfo.ssid = str;
            callbackInfo.activityIn = z2;
            callbackInfo.activityOut = z3;
            callbackInfo.wifiSignalContentDescription = iconState2.contentDescription;
            callbackInfo.isTransient = z4;
            callbackInfo.statusLabel = str2;
            if (WifiTile.this.isShowingDetail()) {
                WifiTile.this.mDetailAdapter.updateItems();
            }
            WifiTile.this.mTransientEnabling = false;
            WifiTile.this.refreshState();
        }
    }

    public int getMetricsCategory() {
        return 126;
    }

    public void handleSetListening(boolean z) {
    }

    public WifiTile(QSHost qSHost, NetworkController networkController, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = networkController;
        this.mWifiController = this.mController.getAccessPointController();
        this.mDetailAdapter = (WifiDetailAdapter) createDetailAdapter();
        this.mActivityStarter = activityStarter;
        this.mController.observe(getLifecycle(), this.mSignalCallback);
    }

    public SignalState newTileState() {
        return new SignalState();
    }

    public void setDetailListening(boolean z) {
        if (z) {
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    /* access modifiers changed from: protected */
    public DetailAdapter createDetailAdapter() {
        return new WifiDetailAdapter();
    }

    public QSIconView createTileView(Context context) {
        return new AlphaControlledSignalTileView(context);
    }

    public Intent getLongClickIntent() {
        return WIFI_SETTINGS;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        Object obj;
        ((SignalState) this.mState).copyTo(this.mStateBeforeClick);
        boolean z = ((SignalState) this.mState).value;
        boolean z2 = true;
        this.mTransientEnabling = !z;
        if (this.mTransientEnabling) {
            this.mClickTimeMillis = SystemClock.uptimeMillis();
        }
        if (z) {
            obj = null;
        } else {
            obj = QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
        }
        refreshState(obj);
        NetworkController networkController = this.mController;
        if (z) {
            z2 = false;
        }
        networkController.setWifiEnabled(z2);
        this.mExpectDisabled = z;
        if (this.mExpectDisabled) {
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    WifiTile.this.lambda$handleClick$0$WifiTile();
                }
            }, 0);
        }
    }

    public /* synthetic */ void lambda$handleClick$0$WifiTile() {
        if (this.mExpectDisabled) {
            this.mExpectDisabled = false;
            refreshState();
        }
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        if (!this.mWifiController.canConfigWifi()) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"), 0);
            return;
        }
        showDetail(true);
        if (!((SignalState) this.mState).value) {
            this.mController.setWifiEnabled(true);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_wifi_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(SignalState signalState, Object obj) {
        if (QSTileImpl.DEBUG) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("handleUpdateState arg=");
            sb.append(obj);
            Log.d(str, sb.toString());
        }
        CallbackInfo callbackInfo = this.mSignalCallback.mInfo;
        if (this.mExpectDisabled) {
            if (callbackInfo.enabled) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d(this.TAG, "handleUpdateState: skipping update.");
                }
                return;
            }
            this.mExpectDisabled = false;
        }
        boolean z = obj == QSTileImpl.ARG_SHOW_TRANSIENT_ENABLING;
        boolean z2 = callbackInfo.enabled && callbackInfo.wifiSignalIconId > 0 && callbackInfo.ssid != null;
        boolean z3 = callbackInfo.wifiSignalIconId > 0 && callbackInfo.ssid == null;
        if (signalState.value != callbackInfo.enabled) {
            this.mDetailAdapter.setItemsVisible(callbackInfo.enabled);
            fireToggleStateChanged(callbackInfo.enabled);
        }
        if (signalState.slash == null) {
            signalState.slash = new SlashState();
            signalState.slash.rotation = 6.0f;
        }
        signalState.slash.isSlashed = false;
        if (this.mTransientEnabling && SystemClock.uptimeMillis() - this.mClickTimeMillis >= 10000) {
            this.mTransientEnabling = false;
        }
        this.mTransientEnabling = callbackInfo.enabled ? false : this.mTransientEnabling;
        boolean z4 = z | this.mTransientEnabling;
        boolean z5 = z4 || callbackInfo.isTransient;
        signalState.secondaryLabel = getSecondaryLabel(z5, callbackInfo.statusLabel);
        signalState.state = 2;
        signalState.dualTarget = true;
        signalState.value = z4 || callbackInfo.enabled;
        signalState.activityIn = callbackInfo.enabled && callbackInfo.activityIn;
        signalState.activityOut = callbackInfo.enabled && callbackInfo.activityOut;
        StringBuffer stringBuffer = new StringBuffer();
        Resources resources = this.mContext.getResources();
        if (Build.DEBUG_ONEPLUS) {
            String str2 = this.TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("handleUpdateState: state.value=");
            sb2.append(signalState.value);
            sb2.append(", cb.enabled=");
            sb2.append(callbackInfo.enabled);
            sb2.append(", cb.isTran=");
            sb2.append(callbackInfo.isTransient);
            sb2.append(", mTrans=");
            sb2.append(this.mTransientEnabling);
            sb2.append(", wifiConned=");
            sb2.append(z2);
            sb2.append(", wifiNotConned=");
            sb2.append(z3);
            Log.d(str2, sb2.toString());
        }
        if (OpUtils.isUSS()) {
            if (z5) {
                signalState.icon = ResourceIcon.get(R$drawable.op_ic_signal_wifi_transient_animation);
                signalState.label = resources.getString(R$string.quick_settings_wifi_label);
            } else if (!signalState.value) {
                signalState.slash.isSlashed = true;
                signalState.state = 1;
                signalState.icon = ResourceIcon.get(OpWifiIcons.OP_QS_WIFI_DISABLED);
                signalState.label = resources.getString(R$string.quick_settings_wifi_label);
            } else if (z2) {
                signalState.icon = ResourceIcon.get(callbackInfo.wifiSignalIconId);
                signalState.label = removeDoubleQuotes(callbackInfo.ssid);
            } else if (z3) {
                signalState.icon = ResourceIcon.get(R$drawable.op_ic_qs_wifi_disconnected);
                signalState.label = resources.getString(R$string.quick_settings_wifi_label);
            } else {
                signalState.icon = ResourceIcon.get(OpWifiIcons.OP_QS_WIFI_NO_NETWORK);
                signalState.label = resources.getString(R$string.quick_settings_wifi_label);
            }
        } else if (z5) {
            signalState.icon = ResourceIcon.get(17302814);
            signalState.label = resources.getString(R$string.quick_settings_wifi_label);
        } else if (!signalState.value) {
            signalState.slash.isSlashed = true;
            signalState.state = 1;
            signalState.icon = ResourceIcon.get(WifiIcons.QS_WIFI_DISABLED);
            signalState.label = resources.getString(R$string.quick_settings_wifi_label);
        } else if (z2) {
            signalState.icon = ResourceIcon.get(callbackInfo.wifiSignalIconId);
            signalState.label = removeDoubleQuotes(callbackInfo.ssid);
        } else if (z3) {
            signalState.icon = ResourceIcon.get(R$drawable.op_q_ic_qs_wifi_disconnected);
            signalState.label = resources.getString(R$string.quick_settings_wifi_label);
        } else {
            signalState.icon = ResourceIcon.get(WifiIcons.QS_WIFI_NO_NETWORK);
            signalState.label = resources.getString(R$string.quick_settings_wifi_label);
        }
        if (Build.DEBUG_ONEPLUS) {
            String str3 = this.TAG;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("handleUpdateState: state=");
            sb3.append(signalState.state);
            sb3.append(", label=");
            sb3.append(signalState.label);
            sb3.append(", secondaryLabel=");
            sb3.append(signalState.secondaryLabel);
            sb3.append(", actIn=");
            sb3.append(signalState.activityIn);
            sb3.append(", actOut=");
            sb3.append(signalState.activityOut);
            sb3.append(", iconRes=");
            sb3.append(signalState.icon);
            Log.d(str3, sb3.toString());
        }
        stringBuffer.append(this.mContext.getString(R$string.quick_settings_wifi_label));
        String str4 = ",";
        stringBuffer.append(str4);
        if (signalState.value && z2) {
            stringBuffer.append(callbackInfo.wifiSignalContentDescription);
            stringBuffer.append(str4);
            stringBuffer.append(removeDoubleQuotes(callbackInfo.ssid));
            if (!TextUtils.isEmpty(signalState.secondaryLabel)) {
                stringBuffer.append(str4);
                stringBuffer.append(signalState.secondaryLabel);
            }
        }
        signalState.contentDescription = stringBuffer.toString();
        signalState.dualLabelContentDescription = resources.getString(R$string.accessibility_quick_settings_open_settings, new Object[]{getTileLabel()});
        signalState.expandedAccessibilityClassName = Switch.class.getName();
    }

    private CharSequence getSecondaryLabel(boolean z, String str) {
        return z ? this.mContext.getString(R$string.quick_settings_wifi_secondary_label_transient) : str;
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.value == ((SignalState) this.mState).value;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((SignalState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_wifi_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_wifi_changed_off);
    }

    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi");
    }

    private static String removeDoubleQuotes(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"') {
            int i = length - 1;
            if (str.charAt(i) == '\"') {
                str = str.substring(1, i);
            }
        }
        return str;
    }
}
