package com.oneplus.networkspeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsService.Stub;
import android.net.NetworkInfo;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.Dependency;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.networkspeed.NetworkSpeedController.INetworkSpeedStateCallBack;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Timer;

public class NetworkSpeedControllerImpl extends BroadcastReceiver implements Tunable, NetworkSpeedController {
    /* access modifiers changed from: private */
    public static boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static long ERTRY_POINT = 1024;
    private static int HANDRED = 100;
    /* access modifiers changed from: private */
    public static String TAG = "NetworkSpeedController";
    private static int TEN = 10;
    private static int THOUSAND = 1000;
    private static String UNIT_GB = "GB";
    private static String UNIT_KB = "KB";
    private static String UNIT_MB = "MB";
    /* access modifiers changed from: private */
    public static int UPDATE_INTERVAL = 3;
    /* access modifiers changed from: private */
    public int MSG_MAYBE_STOP_NETWORTSPEED = 1001;
    /* access modifiers changed from: private */
    public int MSG_UPDATE_NETWORTSPEED = 1000;
    /* access modifiers changed from: private */
    public int MSG_UPDATE_SHOW = 1002;
    /* access modifiers changed from: private */
    public int MSG_UPDATE_SPEED_ON_BG = 2001;
    /* access modifiers changed from: private */
    public MyBackgroundHandler mBackgroundHandler = new MyBackgroundHandler(BackgroundThread.getHandler().getLooper());
    private boolean mBlockNetworkSpeed = true;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    /* access modifiers changed from: private */
    public MyHandler mHandler = new MyHandler(Looper.getMainLooper());
    /* access modifiers changed from: private */
    public boolean mHotSpotEnable = false;
    private StatusBarIconController mIconController;
    /* access modifiers changed from: private */
    public boolean mIsFirstLoad = true;
    private final ArrayList<INetworkSpeedStateCallBack> mNetworkSpeedStateCallBack = new ArrayList<>();
    private boolean mNetworkTraceState = false;
    private boolean mShow = false;
    /* access modifiers changed from: private */
    public String mSpeed = "";
    /* access modifiers changed from: private */
    public MySpeedMachine mSpeedMachine = new MySpeedMachine();
    private INetworkStatsService mStatsService;
    private Timer mTimer;

    private class MyBackgroundHandler extends Handler {
        public MyBackgroundHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            if (message.what == NetworkSpeedControllerImpl.this.MSG_UPDATE_SPEED_ON_BG) {
                NetworkSpeedControllerImpl.this.mBackgroundHandler.removeMessages(NetworkSpeedControllerImpl.this.MSG_UPDATE_SPEED_ON_BG);
                if (NetworkSpeedControllerImpl.this.mSpeedMachine.isTurnOn()) {
                    NetworkSpeedControllerImpl.this.mSpeedMachine.updateSpeedonBG();
                    NetworkSpeedControllerImpl.this.scheduleNextUpdate();
                }
            }
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == NetworkSpeedControllerImpl.this.MSG_UPDATE_NETWORTSPEED) {
                Object obj = message.obj;
                if (obj instanceof String) {
                    NetworkSpeedControllerImpl.this.mSpeed = (String) obj;
                    NetworkSpeedControllerImpl.this.refreshSpeed();
                }
            } else if (i == NetworkSpeedControllerImpl.this.MSG_MAYBE_STOP_NETWORTSPEED) {
                NetworkSpeedControllerImpl.this.updateState();
            } else if (i == NetworkSpeedControllerImpl.this.MSG_UPDATE_SHOW) {
                NetworkSpeedControllerImpl.this.onShowStateChange();
            }
        }
    }

    private class MySpeedMachine {
        long incrementRxBytes = 0;
        long incrementTxBytes = 0;
        boolean isTurnOn = false;
        boolean mIsFirstLoadTether = true;
        long oldRxBytes = 0;
        long oldTetherRxBytes = 0;
        long oldTetherTxBytes = 0;
        long oldTxBytes = 0;

        public MySpeedMachine() {
            reset();
        }

        /* access modifiers changed from: private */
        public void updateSpeedonBG() {
            long j;
            long j2;
            long j3;
            long j4;
            if (NetworkSpeedControllerImpl.this.isNetworkSpeedTracing()) {
                long totalTxBytes = TrafficStats.getTotalTxBytes();
                long totalRxBytes = TrafficStats.getTotalRxBytes();
                this.incrementTxBytes = totalTxBytes - this.oldTxBytes;
                this.incrementRxBytes = totalRxBytes - this.oldRxBytes;
                this.oldTxBytes = totalTxBytes;
                this.oldRxBytes = totalRxBytes;
                if (!NetworkSpeedControllerImpl.this.mHotSpotEnable) {
                    this.oldTetherTxBytes = 0;
                    this.oldTetherRxBytes = 0;
                    this.mIsFirstLoadTether = true;
                    j = totalRxBytes;
                } else {
                    long[] access$1300 = NetworkSpeedControllerImpl.this.getTetherStats();
                    if (access$1300 == null || access$1300.length != 2) {
                        j4 = 0;
                        j3 = 0;
                    } else {
                        long j5 = access$1300[0];
                        long j6 = access$1300[1];
                        j4 = j6 - this.oldTetherTxBytes;
                        j3 = j5 - this.oldTetherRxBytes;
                        this.oldTetherTxBytes = j6;
                        this.oldTetherRxBytes = j5;
                    }
                    if (NetworkSpeedControllerImpl.DEBUG) {
                        String access$1500 = NetworkSpeedControllerImpl.TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("NetWorkSpeed TetherTx:");
                        sb.append(NetworkSpeedControllerImpl.this.formateSpeed(j4 / ((long) NetworkSpeedControllerImpl.UPDATE_INTERVAL)));
                        sb.append(" tTetherRx:");
                        sb.append(NetworkSpeedControllerImpl.this.formateSpeed(j3 / ((long) NetworkSpeedControllerImpl.UPDATE_INTERVAL)));
                        sb.append(" systemTx:");
                        j = totalRxBytes;
                        sb.append(NetworkSpeedControllerImpl.this.formateSpeed(this.incrementTxBytes / ((long) NetworkSpeedControllerImpl.UPDATE_INTERVAL)));
                        sb.append(" systemRx:");
                        sb.append(NetworkSpeedControllerImpl.this.formateSpeed(this.incrementRxBytes / ((long) NetworkSpeedControllerImpl.UPDATE_INTERVAL)));
                        Log.d(access$1500, sb.toString());
                    } else {
                        j = totalRxBytes;
                    }
                    if (this.mIsFirstLoadTether) {
                        this.mIsFirstLoadTether = false;
                    } else {
                        this.incrementTxBytes = this.incrementTxBytes + j4 + j3;
                    }
                }
                if (NetworkSpeedControllerImpl.this.mIsFirstLoad) {
                    if (NetworkSpeedControllerImpl.DEBUG) {
                        Log.d(NetworkSpeedControllerImpl.TAG, "NetWorkSpeed is first load.");
                    }
                    j2 = 0;
                    this.incrementTxBytes = 0;
                    this.incrementRxBytes = 0;
                    NetworkSpeedControllerImpl.this.mIsFirstLoad = false;
                } else {
                    j2 = 0;
                }
                if (this.incrementTxBytes < j2) {
                    this.incrementTxBytes = j2;
                }
                if (this.incrementRxBytes < j2) {
                    this.incrementRxBytes = j2;
                }
                long j7 = this.incrementRxBytes;
                long j8 = this.incrementTxBytes;
                if (j7 <= j8) {
                    j7 = j8;
                }
                long access$1600 = j7 / ((long) NetworkSpeedControllerImpl.UPDATE_INTERVAL);
                String access$1700 = NetworkSpeedControllerImpl.this.formateSpeed(access$1600);
                if (NetworkSpeedControllerImpl.DEBUG) {
                    String access$15002 = NetworkSpeedControllerImpl.TAG;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("NetWorkSpeed refresh totalTxBytes=");
                    sb2.append(totalTxBytes);
                    sb2.append(", totalRxBytes=");
                    sb2.append(j);
                    sb2.append(", incrementPs=");
                    sb2.append(access$1600);
                    sb2.append(", mSpeed=");
                    sb2.append(access$1700);
                    sb2.append(" ,incrementBytes:");
                    sb2.append(j7);
                    Log.d(access$15002, sb2.toString());
                }
                Message obtainMessage = NetworkSpeedControllerImpl.this.mHandler.obtainMessage();
                obtainMessage.what = NetworkSpeedControllerImpl.this.MSG_UPDATE_NETWORTSPEED;
                obtainMessage.obj = access$1700;
                NetworkSpeedControllerImpl.this.mHandler.sendMessage(obtainMessage);
                return;
            }
            Message obtainMessage2 = NetworkSpeedControllerImpl.this.mHandler.obtainMessage();
            obtainMessage2.what = NetworkSpeedControllerImpl.this.MSG_MAYBE_STOP_NETWORTSPEED;
            NetworkSpeedControllerImpl.this.mHandler.sendMessage(obtainMessage2);
            Log.d(NetworkSpeedControllerImpl.TAG, "send MSG_CLOSE_NETWORTSPEED");
        }

        public void reset() {
            this.oldTxBytes = 0;
            this.incrementTxBytes = 0;
            this.oldRxBytes = 0;
            this.incrementRxBytes = 0;
        }

        public void setTurnOn() {
            this.isTurnOn = true;
        }

        public void setTurnOff() {
            this.isTurnOn = false;
        }

        public boolean isTurnOn() {
            return this.isTurnOn;
        }
    }

    public NetworkSpeedControllerImpl(Context context) {
        this.mContext = context;
        this.mTimer = new Timer();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mContext.registerReceiver(this, intentFilter);
        this.mStatsService = Stub.asInterface(ServiceManager.getService("netstats"));
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mIconController = (StatusBarIconController) Dependency.get(StatusBarIconController.class);
        String str = "networkspeed";
        this.mIconController.setOPCustView(str, R$layout.status_bar_network_speed, this.mShow);
        this.mIconController.setIconVisibility(str, this.mShow);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
    }

    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateConnectivity connectedTransports:");
            sb.append(bitSet);
            sb.append(" validatedTransports:");
            sb.append(bitSet2);
            Log.d(str, sb.toString());
        }
        updateState();
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == "android.intent.action.TIME_SET") {
            updateState();
        } else if (action == "android.intent.action.TIMEZONE_CHANGED") {
            updateState();
        } else if (action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
            this.mHotSpotEnable = intent.getIntExtra("wifi_state", 14) == 13;
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("HotSpot enable:");
                sb.append(this.mHotSpotEnable);
                Log.i(str, sb.toString());
            }
            updateState();
        }
    }

    private String divToFractionDigits(long j, long j2, int i) {
        if (j2 == 0) {
            Log.i(TAG, "divisor shouldn't be 0");
            return "Error";
        }
        StringBuffer stringBuffer = new StringBuffer();
        long j3 = j / j2;
        long j4 = j % j2;
        stringBuffer.append(j3);
        if (i > 0) {
            stringBuffer.append(".");
            for (int i2 = 0; i2 < i; i2++) {
                long j5 = j4 * 10;
                long j6 = j5 / j2;
                j4 = j5 % j2;
                stringBuffer.append(j6);
            }
        }
        return stringBuffer.toString();
    }

    /* access modifiers changed from: private */
    public String formateSpeed(long j) {
        String str;
        long j2;
        StringBuffer stringBuffer = new StringBuffer();
        String str2 = UNIT_KB;
        long j3 = ERTRY_POINT;
        int i = 2;
        if (j >= j3) {
            if (j < j3 || j >= ((long) THOUSAND) * j3) {
                long j4 = ERTRY_POINT;
                int i2 = THOUSAND;
                if (j < ((long) i2) * j4 || j >= j4 * j4 * ((long) i2)) {
                    long j5 = ERTRY_POINT;
                    long j6 = j5 * j5 * j5;
                    str = UNIT_GB;
                    if (j < j5 * j5 * ((long) THOUSAND) || j >= ((long) TEN) * j6) {
                        long j7 = ERTRY_POINT;
                        if (j < j7 * j7 * j7 * ((long) TEN) || j >= ((long) HANDRED) * j6) {
                            j2 = j6;
                        } else {
                            i = 1;
                        }
                    }
                    j2 = j6;
                    stringBuffer.append(divToFractionDigits(j, j2, i));
                    stringBuffer.append(":");
                    stringBuffer.append(str);
                    stringBuffer.append("/S");
                    return stringBuffer.toString();
                }
                long j8 = j4 * j4;
                String str3 = UNIT_MB;
                if (j < j4 * ((long) i2) || j >= ((long) TEN) * j8) {
                    long j9 = ERTRY_POINT;
                    if (j < j9 * j9 * ((long) TEN) || j >= ((long) HANDRED) * j8) {
                        long j10 = ERTRY_POINT;
                        if (j >= j10 * j10 * ((long) HANDRED)) {
                            int i3 = (j > (((long) THOUSAND) * j8) ? 1 : (j == (((long) THOUSAND) * j8) ? 0 : -1));
                        }
                        i = 0;
                    } else {
                        i = 1;
                    }
                }
                long j11 = j8;
                str = str3;
                j2 = j11;
                stringBuffer.append(divToFractionDigits(j, j2, i));
                stringBuffer.append(":");
                stringBuffer.append(str);
                stringBuffer.append("/S");
                return stringBuffer.toString();
            } else if (j < j3 || j >= ((long) TEN) * j3) {
                long j12 = ERTRY_POINT;
                if (j < ((long) TEN) * j12 || j >= j12 * ((long) HANDRED)) {
                    long j13 = ERTRY_POINT;
                    if (j >= ((long) HANDRED) * j13) {
                        int i4 = (j > (j13 * ((long) THOUSAND)) ? 1 : (j == (j13 * ((long) THOUSAND)) ? 0 : -1));
                    }
                    str = str2;
                    j2 = j3;
                } else {
                    str = str2;
                    i = 1;
                    j2 = j3;
                    stringBuffer.append(divToFractionDigits(j, j2, i));
                    stringBuffer.append(":");
                    stringBuffer.append(str);
                    stringBuffer.append("/S");
                    return stringBuffer.toString();
                }
            }
            i = 0;
            stringBuffer.append(divToFractionDigits(j, j2, i));
            stringBuffer.append(":");
            stringBuffer.append(str);
            stringBuffer.append("/S");
            return stringBuffer.toString();
        }
        str = str2;
        j2 = j3;
        stringBuffer.append(divToFractionDigits(j, j2, i));
        stringBuffer.append(":");
        stringBuffer.append(str);
        stringBuffer.append("/S");
        return stringBuffer.toString();
    }

    public void updateState() {
        boolean isNetworkSpeedTracing = isNetworkSpeedTracing();
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateState traceState:");
            sb.append(isNetworkSpeedTracing);
            Log.d(str, sb.toString());
        }
        if (this.mNetworkTraceState != isNetworkSpeedTracing) {
            this.mNetworkTraceState = isNetworkSpeedTracing;
            if (this.mNetworkTraceState) {
                onStartTraceSpeed();
            } else {
                onStopTraceSpeed();
            }
            Message obtainMessage = this.mHandler.obtainMessage();
            obtainMessage.what = this.MSG_UPDATE_SHOW;
            this.mHandler.sendMessage(obtainMessage);
        }
    }

    /* access modifiers changed from: private */
    public void onShowStateChange() {
        boolean z = this.mNetworkTraceState;
        if (this.mShow != z) {
            this.mShow = z;
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onShowStateChange s:");
                sb.append(z);
                Log.d(str, sb.toString());
            }
            StatusBarIconController statusBarIconController = this.mIconController;
            if (statusBarIconController != null) {
                statusBarIconController.setIconVisibility("networkspeed", z);
            }
            Iterator it = this.mNetworkSpeedStateCallBack.iterator();
            while (it.hasNext()) {
                ((INetworkSpeedStateCallBack) it.next()).onSpeedShow(z);
            }
        }
    }

    private void onStartTraceSpeed() {
        if (DEBUG) {
            Log.d(TAG, "onStartTraceSpeed");
        }
        updateSpeed();
    }

    private void onStopTraceSpeed() {
        if (DEBUG) {
            Log.d(TAG, "onStopTraceSpeed");
        }
        this.mIsFirstLoad = true;
        stopSpeed();
        this.mSpeed = "";
    }

    private void updateSpeed() {
        this.mIsFirstLoad = true;
        if (DEBUG) {
            Log.d(TAG, "updateSpeed");
        }
        this.mSpeed = "";
        Message obtainMessage = this.mHandler.obtainMessage();
        obtainMessage.what = this.MSG_UPDATE_NETWORTSPEED;
        obtainMessage.obj = this.mSpeed;
        this.mHandler.sendMessage(obtainMessage);
        MySpeedMachine mySpeedMachine = this.mSpeedMachine;
        if (mySpeedMachine != null) {
            mySpeedMachine.reset();
            this.mSpeedMachine.setTurnOn();
        }
        this.mBackgroundHandler.removeMessages(this.MSG_UPDATE_SPEED_ON_BG);
        Message message = new Message();
        message.what = this.MSG_UPDATE_SPEED_ON_BG;
        this.mBackgroundHandler.sendMessage(message);
    }

    /* access modifiers changed from: private */
    public void scheduleNextUpdate() {
        long uptimeMillis = SystemClock.uptimeMillis() + ((long) (UPDATE_INTERVAL * 1000));
        Message message = new Message();
        message.what = this.MSG_UPDATE_SPEED_ON_BG;
        this.mBackgroundHandler.sendMessageAtTime(message, uptimeMillis);
    }

    private void stopSpeed() {
        MySpeedMachine mySpeedMachine = this.mSpeedMachine;
        if (mySpeedMachine != null) {
            mySpeedMachine.reset();
            this.mSpeedMachine.setTurnOff();
        }
        this.mBackgroundHandler.removeMessages(this.MSG_UPDATE_SPEED_ON_BG);
    }

    /* access modifiers changed from: private */
    public void refreshSpeed() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("refreshSpeed sp:");
            sb.append(this.mSpeed);
            Log.d(str, sb.toString());
        }
        Iterator it = this.mNetworkSpeedStateCallBack.iterator();
        while (it.hasNext()) {
            INetworkSpeedStateCallBack iNetworkSpeedStateCallBack = (INetworkSpeedStateCallBack) it.next();
            if (iNetworkSpeedStateCallBack != null) {
                iNetworkSpeedStateCallBack.onSpeedChange(this.mSpeed);
            }
        }
    }

    private boolean isNetworkConnected() {
        boolean z = false;
        if (this.mContext == null) {
            return false;
        }
        NetworkInfo networkInfo = null;
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        if (networkInfo != null && networkInfo.isAvailable()) {
            z = true;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("isNetworkConnected = ");
            sb.append(z);
            Log.v(str, sb.toString());
        }
        return z;
    }

    /* access modifiers changed from: private */
    public boolean isNetworkSpeedTracing() {
        return isNetworkConnected() && !this.mBlockNetworkSpeed;
    }

    public void onTuningChanged(String str, String str2) {
        if ("icon_blacklist".equals(str)) {
            boolean contains = StatusBarIconController.getIconBlacklist(str2).contains("networkspeed");
            if (contains != this.mBlockNetworkSpeed) {
                String str3 = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append(" onTuningChanged blocknetworkSpeed:");
                sb.append(contains);
                Log.i(str3, sb.toString());
                this.mBlockNetworkSpeed = contains;
                updateState();
            }
        }
    }

    public void addCallback(INetworkSpeedStateCallBack iNetworkSpeedStateCallBack) {
        synchronized (this) {
            this.mNetworkSpeedStateCallBack.add(iNetworkSpeedStateCallBack);
            try {
                iNetworkSpeedStateCallBack.onSpeedChange(this.mSpeed);
                iNetworkSpeedStateCallBack.onSpeedShow(this.mShow);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to call to IKeyguardStateCallback", e);
            }
        }
    }

    public void removeCallback(INetworkSpeedStateCallBack iNetworkSpeedStateCallBack) {
        synchronized (this) {
            this.mNetworkSpeedStateCallBack.remove(iNetworkSpeedStateCallBack);
        }
    }

    /* access modifiers changed from: private */
    public long[] getTetherStats() {
        long[] jArr = new long[2];
        try {
            NetworkStats networkStatsTethering = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management")).getNetworkStatsTethering(-1);
            int size = networkStatsTethering != null ? networkStatsTethering.size() : 0;
            long j = 0;
            Entry entry = null;
            long j2 = 0;
            for (int i = 0; i < size; i++) {
                entry = networkStatsTethering.getValues(i, entry);
                j += entry.rxBytes;
                j2 += entry.txBytes;
            }
            jArr[0] = j;
            jArr[1] = j2;
        } catch (RemoteException unused) {
        }
        return jArr;
    }
}
