package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.OnStartTetheringCallback;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.SoftApCallback;
import android.os.Build;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;
import com.android.systemui.p007qs.GlobalSetting;
import com.android.systemui.statusbar.policy.HotspotController.Callback;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class HotspotControllerImpl implements HotspotController, SoftApCallback {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDisableByOperator = false;
    private int mHotspotState;
    private final Handler mMainHandler;
    private int mNumConnectedDevices;
    private GlobalSetting mTetheredData;
    private boolean mWaitingForTerminalState;
    private final WifiManager mWifiManager;

    private static String stateToString(int i) {
        switch (i) {
            case 10:
                return "DISABLING";
            case 11:
                return "DISABLED";
            case 12:
                return "ENABLING";
            case 13:
                return "ENABLED";
            case 14:
                return "FAILED";
            default:
                return null;
        }
    }

    public void onNumClientsChanged(int i) {
    }

    public HotspotControllerImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mMainHandler = handler;
        if (OpUtils.isUSS()) {
            this.mTetheredData = new GlobalSetting(this.mContext, null, "TetheredData") {
                /* access modifiers changed from: protected */
                public void handleValueChanged(int i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("HotspotControllerImpl / handleValueChanged / value");
                    sb.append(i);
                    Log.i("HotspotController", sb.toString());
                    HotspotControllerImpl hotspotControllerImpl = HotspotControllerImpl.this;
                    boolean z = true;
                    if (i != 1) {
                        z = false;
                    }
                    hotspotControllerImpl.mDisableByOperator = z;
                    if (HotspotControllerImpl.this.mDisableByOperator && HotspotControllerImpl.this.isHotspotEnabled()) {
                        HotspotControllerImpl.this.setHotspotEnabled(false);
                    }
                    HotspotControllerImpl hotspotControllerImpl2 = HotspotControllerImpl.this;
                    hotspotControllerImpl2.fireOperatorChangedCallback(hotspotControllerImpl2.isOperatorHotspotDisable());
                }
            };
            boolean z = true;
            this.mTetheredData.setListening(true);
            if (this.mTetheredData.getValue() != 1) {
                z = false;
            }
            this.mDisableByOperator = z;
        }
    }

    public boolean isHotspotSupported() {
        return this.mConnectivityManager.isTetheringSupported() && this.mConnectivityManager.getTetherableWifiRegexs().length != 0 && UserManager.get(this.mContext).isUserAdmin(ActivityManager.getCurrentUser());
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HotspotController state:");
        printWriter.print("  mHotspotState=");
        printWriter.println(stateToString(this.mHotspotState));
        printWriter.print("  mNumConnectedDevices=");
        printWriter.println(this.mNumConnectedDevices);
        printWriter.print("  mWaitingForTerminalState=");
        printWriter.println(this.mWaitingForTerminalState);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x005a, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x005c, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCallback(com.android.systemui.statusbar.policy.HotspotController.Callback r5) {
        /*
            r4 = this;
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r0 = r4.mCallbacks
            monitor-enter(r0)
            if (r5 == 0) goto L_0x005b
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r1 = r4.mCallbacks     // Catch:{ all -> 0x005d }
            boolean r1 = r1.contains(r5)     // Catch:{ all -> 0x005d }
            if (r1 == 0) goto L_0x000e
            goto L_0x005b
        L_0x000e:
            boolean r1 = DEBUG     // Catch:{ all -> 0x005d }
            if (r1 == 0) goto L_0x0028
            java.lang.String r1 = "HotspotController"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x005d }
            r2.<init>()     // Catch:{ all -> 0x005d }
            java.lang.String r3 = "addCallback "
            r2.append(r3)     // Catch:{ all -> 0x005d }
            r2.append(r5)     // Catch:{ all -> 0x005d }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x005d }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x005d }
        L_0x0028:
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r1 = r4.mCallbacks     // Catch:{ all -> 0x005d }
            r1.add(r5)     // Catch:{ all -> 0x005d }
            android.net.wifi.WifiManager r1 = r4.mWifiManager     // Catch:{ all -> 0x005d }
            if (r1 == 0) goto L_0x004c
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r1 = r4.mCallbacks     // Catch:{ all -> 0x005d }
            int r1 = r1.size()     // Catch:{ all -> 0x005d }
            r2 = 1
            if (r1 != r2) goto L_0x0042
            android.net.wifi.WifiManager r1 = r4.mWifiManager     // Catch:{ all -> 0x005d }
            android.os.Handler r2 = r4.mMainHandler     // Catch:{ all -> 0x005d }
            r1.registerSoftApCallback(r4, r2)     // Catch:{ all -> 0x005d }
            goto L_0x004c
        L_0x0042:
            android.os.Handler r1 = r4.mMainHandler     // Catch:{ all -> 0x005d }
            com.android.systemui.statusbar.policy.-$$Lambda$HotspotControllerImpl$C17PPPxxCR-pTmr2izVaDhyC9AQ r2 = new com.android.systemui.statusbar.policy.-$$Lambda$HotspotControllerImpl$C17PPPxxCR-pTmr2izVaDhyC9AQ     // Catch:{ all -> 0x005d }
            r2.<init>(r5)     // Catch:{ all -> 0x005d }
            r1.post(r2)     // Catch:{ all -> 0x005d }
        L_0x004c:
            boolean r1 = com.oneplus.util.OpUtils.isUSS()     // Catch:{ all -> 0x005d }
            if (r1 == 0) goto L_0x0059
            boolean r4 = r4.isOperatorHotspotDisable()     // Catch:{ all -> 0x005d }
            r5.onOperatorHotspotChanged(r4)     // Catch:{ all -> 0x005d }
        L_0x0059:
            monitor-exit(r0)     // Catch:{ all -> 0x005d }
            return
        L_0x005b:
            monitor-exit(r0)     // Catch:{ all -> 0x005d }
            return
        L_0x005d:
            r4 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x005d }
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.HotspotControllerImpl.addCallback(com.android.systemui.statusbar.policy.HotspotController$Callback):void");
    }

    public /* synthetic */ void lambda$addCallback$0$HotspotControllerImpl(Callback callback) {
        callback.onHotspotChanged(isHotspotEnabled(), this.mNumConnectedDevices);
    }

    public void removeCallback(Callback callback) {
        if (callback != null) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("removeCallback ");
                sb.append(callback);
                Log.d("HotspotController", sb.toString());
            }
            synchronized (this.mCallbacks) {
                this.mCallbacks.remove(callback);
                if (this.mCallbacks.isEmpty() && this.mWifiManager != null) {
                    this.mWifiManager.unregisterSoftApCallback(this);
                }
            }
        }
    }

    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    public boolean isOperatorHotspotDisable() {
        if (OpUtils.isUSS()) {
            return this.mDisableByOperator;
        }
        return false;
    }

    public boolean isHotspotTransient() {
        return this.mWaitingForTerminalState || this.mHotspotState == 12;
    }

    public void setHotspotEnabled(boolean z) {
        String str = "HotspotController";
        if (this.mWaitingForTerminalState) {
            if (DEBUG) {
                Log.d(str, "Ignoring setHotspotEnabled; waiting for terminal state.");
            }
            return;
        }
        if (z) {
            this.mWaitingForTerminalState = true;
            if (DEBUG) {
                Log.d(str, "Starting tethering");
            }
            this.mConnectivityManager.startTethering(0, false, new OnStartTetheringCallback() {
                public void onTetheringFailed() {
                    if (HotspotControllerImpl.DEBUG) {
                        Log.d("HotspotController", "onTetheringFailed");
                    }
                    HotspotControllerImpl.this.maybeResetSoftApState();
                    HotspotControllerImpl.this.fireHotspotChangedCallback();
                }
            });
        } else {
            this.mConnectivityManager.stopTethering(0);
        }
    }

    public int getNumConnectedDevices() {
        return this.mNumConnectedDevices;
    }

    /* access modifiers changed from: private */
    public void fireHotspotChangedCallback() {
        synchronized (this.mCallbacks) {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((Callback) it.next()).onHotspotChanged(isHotspotEnabled(), this.mNumConnectedDevices);
            }
        }
    }

    /* access modifiers changed from: private */
    public void fireOperatorChangedCallback(boolean z) {
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("fireOperatorChangedCallback / enabled:");
            sb.append(z);
            sb.append(" / codeStack:");
            sb.append(Log.getStackTraceString(new Throwable()));
            Log.i("HotspotController", sb.toString());
        }
        synchronized (this.mCallbacks) {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((Callback) it.next()).onOperatorHotspotChanged(z);
            }
        }
    }

    public void onStateChanged(int i, int i2) {
        this.mHotspotState = i;
        maybeResetSoftApState();
        if (!isHotspotEnabled()) {
            this.mNumConnectedDevices = 0;
        }
        fireHotspotChangedCallback();
    }

    /* access modifiers changed from: private */
    public void maybeResetSoftApState() {
        if (this.mWaitingForTerminalState) {
            int i = this.mHotspotState;
            if (!(i == 11 || i == 13)) {
                if (i == 14) {
                    this.mConnectivityManager.stopTethering(0);
                }
            }
            this.mWaitingForTerminalState = false;
        }
    }

    public void onStaConnected(String str, int i) {
        this.mNumConnectedDevices = i;
        fireHotspotChangedCallback();
    }

    public void onStaDisconnected(String str, int i) {
        this.mNumConnectedDevices = i;
        fireHotspotChangedCallback();
    }
}
