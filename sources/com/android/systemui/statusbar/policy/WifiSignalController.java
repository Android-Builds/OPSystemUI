package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.TrafficStateCallback;
import android.text.TextUtils;
import android.util.Log;
import android.util.OpFeatures;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.systemui.R$bool;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.oneplus.systemui.statusbar.policy.OpWifiIcons;
import com.oneplus.util.OpUtils;
import com.oneplus.worklife.OPWLBHelper;
import java.util.Objects;

public class WifiSignalController extends SignalController<WifiState, IconGroup> {
    private final IconGroup mDefaultWifiIconGroup;
    private final boolean mHasMobileData;
    private final IconGroup mOpDefaultWifiIconGroup;
    private final IconGroup mWifi4IconGroup;
    private final IconGroup mWifi5IconGroup;
    private final IconGroup mWifi6IconGroup;
    private final WifiStatusTracker mWifiTracker;

    static class WifiState extends State {
        boolean isReady;
        boolean isTransient;
        String ssid;
        String statusLabel;
        int wifiGenerationVersion;

        WifiState() {
        }

        public void copyFrom(State state) {
            super.copyFrom(state);
            WifiState wifiState = (WifiState) state;
            this.ssid = wifiState.ssid;
            this.wifiGenerationVersion = wifiState.wifiGenerationVersion;
            this.isReady = wifiState.isReady;
            this.isTransient = wifiState.isTransient;
            this.statusLabel = wifiState.statusLabel;
        }

        /* access modifiers changed from: protected */
        public void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(",ssid=");
            sb.append(this.ssid);
            sb.append(",wifiGenerationVersion=");
            sb.append(this.wifiGenerationVersion);
            sb.append(",isReady=");
            sb.append(this.isReady);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(",statusLabel=");
            sb.append(this.statusLabel);
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!super.equals(obj)) {
                return false;
            }
            WifiState wifiState = (WifiState) obj;
            if (Objects.equals(wifiState.ssid, this.ssid) && wifiState.wifiGenerationVersion == this.wifiGenerationVersion && wifiState.isReady == this.isReady && wifiState.isTransient == this.isTransient && TextUtils.equals(wifiState.statusLabel, this.statusLabel)) {
                z = true;
            }
            return z;
        }
    }

    private class WifiTrafficStateCallback implements TrafficStateCallback {
        private WifiTrafficStateCallback() {
        }

        public void onStateChanged(int i) {
            WifiSignalController.this.setActivity(i);
        }
    }

    public WifiSignalController(Context context, boolean z, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, WifiManager wifiManager) {
        Context context2 = context;
        WifiManager wifiManager2 = wifiManager;
        super("WifiSignalController", context, 1, callbackHandler, networkControllerImpl);
        WifiManager wifiManager3 = wifiManager;
        WifiStatusTracker wifiStatusTracker = new WifiStatusTracker(this.mContext, wifiManager3, (NetworkScoreManager) context2.getSystemService(NetworkScoreManager.class), (ConnectivityManager) context2.getSystemService(ConnectivityManager.class), new Runnable() {
            public final void run() {
                WifiSignalController.this.handleStatusUpdated();
            }
        });
        this.mWifiTracker = wifiStatusTracker;
        this.mWifiTracker.setListening(true);
        this.mHasMobileData = z;
        if (wifiManager2 != null) {
            wifiManager2.registerTrafficStateCallback(new WifiTrafficStateCallback(), null);
        }
        IconGroup iconGroup = new IconGroup("Wi-Fi Icons", OpWifiIcons.OP_WIFI_SIGNAL_STRENGTH, OpWifiIcons.OP_QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, OpWifiIcons.OP_WIFI_NO_NETWORK, OpWifiIcons.OP_QS_WIFI_NO_NETWORK, OpWifiIcons.OP_WIFI_NO_NETWORK, OpWifiIcons.OP_QS_WIFI_NO_NETWORK, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mOpDefaultWifiIconGroup = iconGroup;
        IconGroup iconGroup2 = new IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mDefaultWifiIconGroup = iconGroup2;
        IconGroup iconGroup3 = new IconGroup("Wi-Fi 4 Icons", WifiIcons.WIFI_4_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_4_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi4IconGroup = iconGroup3;
        IconGroup iconGroup4 = new IconGroup("Wi-Fi 5 Icons", WifiIcons.WIFI_5_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_5_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi5IconGroup = iconGroup4;
        IconGroup iconGroup5 = new IconGroup("Wi-Fi 6 Icons", WifiIcons.WIFI_6_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_6_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, WifiIcons.WIFI_NO_NETWORK, WifiIcons.QS_WIFI_NO_NETWORK, AccessibilityContentDescriptions.WIFI_NO_CONNECTION);
        this.mWifi6IconGroup = iconGroup5;
        WifiState wifiState = (WifiState) this.mCurrentState;
        WifiState wifiState2 = (WifiState) this.mLastState;
        IconGroup iconGroup6 = this.mDefaultWifiIconGroup;
        wifiState2.iconGroup = iconGroup6;
        wifiState.iconGroup = iconGroup6;
    }

    /* access modifiers changed from: protected */
    public WifiState cleanState() {
        return new WifiState();
    }

    /* access modifiers changed from: 0000 */
    public void refreshLocale() {
        this.mWifiTracker.refreshLocale();
    }

    public void notifyListeners(SignalCallback signalCallback) {
        boolean z = this.mContext.getResources().getBoolean(R$bool.config_showWifiIndicatorWhenEnabled);
        T t = this.mCurrentState;
        boolean z2 = ((WifiState) t).enabled && (((WifiState) t).connected || !this.mHasMobileData || z);
        String str = z2 ? ((WifiState) this.mCurrentState).ssid : null;
        boolean z3 = z2 && ((WifiState) this.mCurrentState).ssid != null;
        String stringIfExists = getStringIfExists(getContentDescription());
        if (((WifiState) this.mCurrentState).inetCondition == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(stringIfExists);
            sb.append(",");
            sb.append(this.mContext.getString(R$string.data_connection_no_internet));
            stringIfExists = sb.toString();
        }
        IconState iconState = new IconState(z2, getCurrentIconId(), stringIfExists);
        IconState iconState2 = new IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), stringIfExists);
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("wifi icon res name:");
            sb2.append(OpUtils.getResourceName(this.mContext, iconState.icon));
            Log.i("WifiSignalController", sb2.toString());
        }
        T t2 = this.mCurrentState;
        boolean z4 = ((WifiState) t2).enabled;
        boolean z5 = z3 && ((WifiState) t2).activityIn;
        boolean z6 = z3 && ((WifiState) this.mCurrentState).activityOut;
        T t3 = this.mCurrentState;
        signalCallback.setWifiIndicators(z4, iconState, iconState2, z5, z6, str, ((WifiState) t3).isTransient, ((WifiState) t3).statusLabel);
    }

    private void updateIconGroup() {
        if (OpUtils.isUSS()) {
            ((WifiState) this.mCurrentState).iconGroup = this.mOpDefaultWifiIconGroup;
            return;
        }
        if (!OpFeatures.isSupport(new int[]{226})) {
            ((WifiState) this.mCurrentState).iconGroup = this.mDefaultWifiIconGroup;
            return;
        }
        T t = this.mCurrentState;
        if (((WifiState) t).wifiGenerationVersion == 4) {
            ((WifiState) t).iconGroup = this.mWifi4IconGroup;
        } else if (((WifiState) t).wifiGenerationVersion == 5) {
            ((WifiState) t).iconGroup = ((WifiState) t).isReady ? this.mWifi6IconGroup : this.mWifi5IconGroup;
        } else if (((WifiState) t).wifiGenerationVersion == 6) {
            ((WifiState) t).iconGroup = this.mWifi6IconGroup;
        } else {
            ((WifiState) t).iconGroup = this.mDefaultWifiIconGroup;
        }
    }

    public void handleBroadcast(Intent intent) {
        this.mWifiTracker.handleBroadcast(intent);
        T t = this.mCurrentState;
        WifiState wifiState = (WifiState) t;
        WifiStatusTracker wifiStatusTracker = this.mWifiTracker;
        wifiState.enabled = wifiStatusTracker.enabled;
        ((WifiState) t).connected = wifiStatusTracker.connected;
        ((WifiState) t).ssid = wifiStatusTracker.ssid;
        ((WifiState) t).rssi = wifiStatusTracker.rssi;
        ((WifiState) t).level = wifiStatusTracker.level;
        ((WifiState) t).statusLabel = wifiStatusTracker.statusLabel;
        ((WifiState) t).wifiGenerationVersion = wifiStatusTracker.wifiGeneration;
        ((WifiState) t).isReady = wifiStatusTracker.vhtMax8SpatialStreamsSupport && wifiStatusTracker.twtSupport;
        updateIconGroup();
        if (!"android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction()) || intent.getIntExtra("wifi_enable_cancel", 0) != 1) {
            notifyListenersIfNecessary();
        } else {
            Log.d("WifiSignalController", "wifi change with cancel. force notify.");
            notifyListenersIfNecessary(true);
        }
        sendConnectedBroadcast();
    }

    private void sendConnectedBroadcast() {
        OPWLBHelper.getInstance(this.mContext).processWifiConnectivity(((WifiState) this.mCurrentState).connected);
    }

    /* access modifiers changed from: private */
    public void handleStatusUpdated() {
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        notifyListenersIfNecessary();
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setActivity(int i) {
        boolean z = false;
        ((WifiState) this.mCurrentState).activityIn = i == 3 || i == 1;
        WifiState wifiState = (WifiState) this.mCurrentState;
        if (i == 3 || i == 2) {
            z = true;
        }
        wifiState.activityOut = z;
        notifyListenersIfNecessary();
    }
}
