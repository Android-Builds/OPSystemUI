package com.android.systemui.statusbar.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R$array;
import com.android.systemui.R$integer;
import com.google.android.collect.Lists;
import com.oneplus.util.OpUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.codeaurora.internal.BearerAllocationStatus;
import org.codeaurora.internal.Client;
import org.codeaurora.internal.DcParam;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;
import org.codeaurora.internal.INetworkCallback;
import org.codeaurora.internal.NetworkCallbackBase;
import org.codeaurora.internal.NrConfigType;
import org.codeaurora.internal.NrIconType;
import org.codeaurora.internal.ServiceUtil;
import org.codeaurora.internal.SignalStrength;
import org.codeaurora.internal.Status;
import org.codeaurora.internal.Token;
import org.codeaurora.internal.UpperLayerIndInfo;

public class FiveGServiceClient {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = true;
    private static FiveGServiceClient sInstance;
    /* access modifiers changed from: private */
    public int mBindRetryTimes = 0;
    @VisibleForTesting
    protected INetworkCallback mCallback = new NetworkCallbackBase() {
        public void on5gStatus(int i, Token token, Status status, boolean z) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("on5gStatus: slotId= ");
                sb.append(i);
                sb.append(" token=");
                sb.append(token);
                sb.append(" status=");
                sb.append(status);
                sb.append(" enableStatus=");
                sb.append(z);
                Log.d("FiveGServiceClient", sb.toString());
            }
        }

        public void onNrDcParam(int i, Token token, Status status, DcParam dcParam) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onNrDcParam: slotId=");
                sb.append(i);
                sb.append(" token=");
                sb.append(token);
                sb.append(" status=");
                sb.append(status);
                sb.append(" dcParam=");
                sb.append(dcParam);
                Log.d("FiveGServiceClient", sb.toString());
            }
            if (status.get() == 1) {
                FiveGServiceClient.this.getCurrentServiceState(i).mDcnr = dcParam.getDcnr();
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void onSignalStrength(int i, Token token, Status status, SignalStrength signalStrength) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onSignalStrength: slotId=");
                sb.append(i);
                sb.append(" token=");
                sb.append(token);
                sb.append(" status=");
                sb.append(status);
                sb.append(" signalStrength=");
                sb.append(signalStrength);
                Log.d("FiveGServiceClient", sb.toString());
            }
            if (status.get() == 1 && signalStrength != null) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mLevel = FiveGServiceClient.this.getRsrpLevel(signalStrength.getRsrp());
                currentServiceState.mRsrp = signalStrength.getRsrp();
                FiveGServiceClient.this.update5GIcon(currentServiceState, i);
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void onAnyNrBearerAllocation(int i, Token token, Status status, BearerAllocationStatus bearerAllocationStatus) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onAnyNrBearerAllocation bearerStatus=");
                sb.append(bearerAllocationStatus.get());
                Log.d("FiveGServiceClient", sb.toString());
            }
            if (status.get() == 1) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mBearerAllocationStatus = bearerAllocationStatus.get();
                FiveGServiceClient.this.update5GIcon(currentServiceState, i);
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void onUpperLayerIndInfo(int i, Token token, Status status, UpperLayerIndInfo upperLayerIndInfo) throws RemoteException {
            if (FiveGServiceClient.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onUpperLayerIndInfo plmn=");
                sb.append(upperLayerIndInfo.getPlmnInfoListAvailable());
                sb.append(" upperLayerIndInfo=");
                sb.append(upperLayerIndInfo.getUpperLayerIndInfoAvailable());
                Log.d("FiveGServiceClient", sb.toString());
            }
            if (status.get() == 1) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mPlmn = upperLayerIndInfo.getPlmnInfoListAvailable();
                currentServiceState.mUpperLayerInd = upperLayerIndInfo.getUpperLayerIndInfoAvailable();
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void on5gConfigInfo(int i, Token token, Status status, NrConfigType nrConfigType) throws RemoteException {
            StringBuilder sb = new StringBuilder();
            sb.append("on5gConfigInfo: slotId = ");
            sb.append(i);
            sb.append(" token = ");
            sb.append(token);
            sb.append(" status");
            sb.append(status);
            sb.append(" NrConfigType = ");
            sb.append(nrConfigType);
            Log.d("FiveGServiceClient", sb.toString());
            if (status.get() == 1) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mNrConfigType = nrConfigType.get();
                FiveGServiceClient.this.update5GIcon(currentServiceState, i);
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }

        public void onNrIconType(int i, Token token, Status status, NrIconType nrIconType) throws RemoteException {
            StringBuilder sb = new StringBuilder();
            sb.append("onNrIconType: slotId = ");
            sb.append(i);
            sb.append(" token = ");
            sb.append(token);
            sb.append(" status");
            sb.append(status);
            sb.append(" NrIconType = ");
            sb.append(nrIconType);
            Log.d("FiveGServiceClient", sb.toString());
            if (status.get() == 1) {
                FiveGServiceState currentServiceState = FiveGServiceClient.this.getCurrentServiceState(i);
                currentServiceState.mNrIconType = nrIconType.get();
                FiveGServiceClient.this.update5GIcon(currentServiceState, i);
                FiveGServiceClient.this.notifyListenersIfNecessary(i);
            }
        }
    };
    /* access modifiers changed from: private */
    public Client mClient;
    private Context mContext;
    private final SparseArray<FiveGServiceState> mCurrentServiceStates = new SparseArray<>();
    private final int[] mFivebarRsrpThresholds;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1024:
                    FiveGServiceClient.this.binderService();
                    return;
                case 1025:
                    FiveGServiceClient.this.initFiveGServiceState();
                    return;
                case 1026:
                    FiveGServiceClient.this.notifyMonitorCallback();
                    return;
                default:
                    return;
            }
        }
    };
    private int mInitRetryTimes = 0;
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mKeyguardUpdateMonitorCallbacks = Lists.newArrayList();
    private final SparseArray<FiveGServiceState> mLastServiceStates = new SparseArray<>();
    /* access modifiers changed from: private */
    public IExtTelephony mNetworkService;
    /* access modifiers changed from: private */
    public String mPackageName;
    private final int[] mRsrpThresholds;
    /* access modifiers changed from: private */
    public boolean mServiceConnected;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            StringBuilder sb = new StringBuilder();
            sb.append("onServiceConnected:");
            sb.append(iBinder);
            String str = "FiveGServiceClient";
            Log.d(str, sb.toString());
            try {
                FiveGServiceClient.this.mNetworkService = Stub.asInterface(iBinder);
                FiveGServiceClient.this.mClient = FiveGServiceClient.this.mNetworkService.registerCallback(FiveGServiceClient.this.mPackageName, FiveGServiceClient.this.mCallback);
                FiveGServiceClient.this.mServiceConnected = true;
                FiveGServiceClient.this.initFiveGServiceState();
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Client = ");
                sb2.append(FiveGServiceClient.this.mClient);
                Log.d(str, sb2.toString());
            } catch (Exception e) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("onServiceConnected: Exception = ");
                sb3.append(e);
                Log.d(str, sb3.toString());
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            StringBuilder sb = new StringBuilder();
            sb.append("onServiceDisconnected:");
            sb.append(componentName);
            Log.d("FiveGServiceClient", sb.toString());
            cleanup();
        }

        public void onBindingDied(ComponentName componentName) {
            StringBuilder sb = new StringBuilder();
            sb.append("onBindingDied:");
            sb.append(componentName);
            String sb2 = sb.toString();
            String str = "FiveGServiceClient";
            Log.d(str, sb2);
            cleanup();
            if (FiveGServiceClient.this.mBindRetryTimes < 4) {
                Log.d(str, "try to re-bind");
                FiveGServiceClient.this.mHandler.sendEmptyMessageDelayed(1024, (long) ((FiveGServiceClient.this.mBindRetryTimes * 2000) + 3000));
            }
        }

        private void cleanup() {
            Log.d("FiveGServiceClient", "cleanup");
            FiveGServiceClient.this.mServiceConnected = false;
            FiveGServiceClient.this.mNetworkService = null;
            FiveGServiceClient.this.mClient = null;
        }
    };
    private final int[] mSnrThresholds;
    @VisibleForTesting
    final SparseArray<IFiveGStateListener> mStatesListeners = new SparseArray<>();

    public static class FiveGServiceState {
        /* access modifiers changed from: private */
        public int mBearerAllocationStatus = 0;
        /* access modifiers changed from: private */
        public int mDcnr = 0;
        /* access modifiers changed from: private */
        public MobileIconGroup mIconGroup = TelephonyIcons.UNKNOWN;
        /* access modifiers changed from: private */
        public int mLevel = 0;
        /* access modifiers changed from: private */
        public int mNrConfigType = 0;
        /* access modifiers changed from: private */
        public int mNrIconType = -1;
        /* access modifiers changed from: private */
        public int mPlmn = 0;
        /* access modifiers changed from: private */
        public int mRsrp;
        /* access modifiers changed from: private */
        public int mUpperLayerInd = 0;

        public boolean isConnectedOnSaMode() {
            return this.mNrConfigType == 1 && this.mIconGroup != TelephonyIcons.UNKNOWN;
        }

        public boolean isConnectedOnNsaMode() {
            return this.mNrConfigType == 0 && this.mIconGroup != TelephonyIcons.UNKNOWN;
        }

        @VisibleForTesting
        public MobileIconGroup getIconGroup() {
            return this.mIconGroup;
        }

        @VisibleForTesting
        public int getSignalLevel() {
            return this.mLevel;
        }

        @VisibleForTesting
        public int getAllocated() {
            return this.mBearerAllocationStatus;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public int getNrConfigType() {
            return this.mNrConfigType;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public int getDcnr() {
            return this.mDcnr;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public int getPlmn() {
            return this.mPlmn;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public int getUpperLayerInd() {
            return this.mUpperLayerInd;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public int getNrIconType() {
            return this.mNrIconType;
        }

        public int getRsrp() {
            return this.mRsrp;
        }

        public void copyFrom(FiveGServiceState fiveGServiceState) {
            this.mBearerAllocationStatus = fiveGServiceState.mBearerAllocationStatus;
            this.mPlmn = fiveGServiceState.mPlmn;
            this.mUpperLayerInd = fiveGServiceState.mUpperLayerInd;
            this.mDcnr = fiveGServiceState.mDcnr;
            this.mLevel = fiveGServiceState.mLevel;
            this.mNrConfigType = fiveGServiceState.mNrConfigType;
            this.mIconGroup = fiveGServiceState.mIconGroup;
            this.mNrIconType = fiveGServiceState.mNrIconType;
            this.mRsrp = fiveGServiceState.mRsrp;
        }

        public boolean equals(FiveGServiceState fiveGServiceState) {
            return this.mBearerAllocationStatus == fiveGServiceState.mBearerAllocationStatus && this.mPlmn == fiveGServiceState.mPlmn && this.mUpperLayerInd == fiveGServiceState.mUpperLayerInd && this.mDcnr == fiveGServiceState.mDcnr && this.mLevel == fiveGServiceState.mLevel && this.mNrConfigType == fiveGServiceState.mNrConfigType && this.mIconGroup == fiveGServiceState.mIconGroup && this.mNrIconType == fiveGServiceState.mNrIconType;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("mBearerAllocationStatus=");
            sb.append(this.mBearerAllocationStatus);
            String str = ", ";
            sb.append(str);
            sb.append("mPlmn=");
            sb.append(this.mPlmn);
            sb.append(str);
            sb.append("mUpperLayerInd=");
            sb.append(this.mUpperLayerInd);
            sb.append(str);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mDcnr=");
            sb2.append(this.mDcnr);
            sb.append(sb2.toString());
            sb.append(str);
            sb.append("mLevel=");
            sb.append(this.mLevel);
            sb.append(str);
            sb.append("mNrConfigType=");
            sb.append(this.mNrConfigType);
            sb.append(str);
            sb.append("mIconGroup=");
            sb.append(this.mIconGroup);
            sb.append("mNrIconType=");
            sb.append(this.mNrIconType);
            sb.append("mRsrp=");
            sb.append(this.mRsrp);
            return sb.toString();
        }
    }

    public interface IFiveGStateListener {
        void onStateChanged(FiveGServiceState fiveGServiceState);
    }

    static {
        Log.isLoggable("FiveGServiceClient", 3);
    }

    public FiveGServiceClient(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
        this.mRsrpThresholds = this.mContext.getResources().getIntArray(R$array.op_config_5g_signal_rsrp_thresholds);
        this.mSnrThresholds = this.mContext.getResources().getIntArray(R$array.config_5g_signal_snr_thresholds);
        this.mFivebarRsrpThresholds = this.mContext.getResources().getIntArray(R$array.config_5_bar_5g_signal_rsrp_thresholds);
    }

    public static FiveGServiceClient getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FiveGServiceClient(context);
        }
        return sInstance;
    }

    public void registerCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        this.mKeyguardUpdateMonitorCallbacks.add(new WeakReference(keyguardUpdateMonitorCallback));
    }

    public void registerListener(int i, IFiveGStateListener iFiveGStateListener) {
        StringBuilder sb = new StringBuilder();
        sb.append("registerListener phoneId=");
        sb.append(i);
        Log.d("FiveGServiceClient", sb.toString());
        this.mStatesListeners.put(i, iFiveGStateListener);
        if (!isServiceConnected()) {
            binderService();
        } else {
            initFiveGServiceState(i);
        }
    }

    public void unregisterListener(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("unregisterListener phoneId=");
        sb.append(i);
        Log.d("FiveGServiceClient", sb.toString());
        this.mStatesListeners.remove(i);
        this.mCurrentServiceStates.remove(i);
        this.mLastServiceStates.remove(i);
    }

    /* access modifiers changed from: private */
    public void binderService() {
        boolean bindService = ServiceUtil.bindService(this.mContext, this.mServiceConnection);
        StringBuilder sb = new StringBuilder();
        sb.append(" bind service ");
        sb.append(bindService);
        Log.d("FiveGServiceClient", sb.toString());
        if (!bindService && this.mBindRetryTimes < 4 && !this.mHandler.hasMessages(1024)) {
            this.mHandler.sendEmptyMessageDelayed(1024, (long) ((this.mBindRetryTimes * 2000) + 3000));
            this.mBindRetryTimes++;
        }
    }

    public static int getNumLevels(Context context) {
        return context.getResources().getInteger(R$integer.config_5g_num_signal_strength_bins);
    }

    public boolean isServiceConnected() {
        return this.mServiceConnected;
    }

    @VisibleForTesting
    public FiveGServiceState getCurrentServiceState(int i) {
        return getServiceState(i, this.mCurrentServiceStates);
    }

    private FiveGServiceState getLastServiceState(int i) {
        return getServiceState(i, this.mLastServiceStates);
    }

    private static FiveGServiceState getServiceState(int i, SparseArray<FiveGServiceState> sparseArray) {
        FiveGServiceState fiveGServiceState = (FiveGServiceState) sparseArray.get(i);
        if (fiveGServiceState != null) {
            return fiveGServiceState;
        }
        FiveGServiceState fiveGServiceState2 = new FiveGServiceState();
        sparseArray.put(i, fiveGServiceState2);
        return fiveGServiceState2;
    }

    /* access modifiers changed from: private */
    public int getRsrpLevel(int i) {
        if (OpUtils.isSupportFiveBar()) {
            return getLevel(i, this.mFivebarRsrpThresholds, false);
        }
        return getLevel(i, this.mRsrpThresholds);
    }

    private int getLevel(int i, int[] iArr) {
        return getLevel(i, iArr, true);
    }

    private int getLevel(int i, int[] iArr, boolean z) {
        int i2;
        int i3 = 0;
        if (iArr[iArr.length - 1] >= i && i >= iArr[0]) {
            while (true) {
                if (i3 >= iArr.length - 1) {
                    i2 = 1;
                    break;
                }
                if (iArr[i3] < i) {
                    i2 = i3 + 1;
                    if (i <= iArr[i2]) {
                        break;
                    }
                }
                i3++;
            }
        } else {
            i2 = 0;
        }
        if (z) {
            i2++;
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("value=");
            sb.append(i);
            sb.append(" level=");
            sb.append(i2);
            Log.d("FiveGServiceClient", sb.toString());
        }
        return i2;
    }

    /* access modifiers changed from: private */
    public void notifyListenersIfNecessary(int i) {
        FiveGServiceState currentServiceState = getCurrentServiceState(i);
        FiveGServiceState lastServiceState = getLastServiceState(i);
        if (!currentServiceState.equals(lastServiceState)) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("phoneId(");
                sb.append(i);
                sb.append(") Change in state from ");
                sb.append(lastServiceState);
                sb.append(" \n\tto ");
                sb.append(currentServiceState);
                Log.d("FiveGServiceClient", sb.toString());
            }
            lastServiceState.copyFrom(currentServiceState);
            IFiveGStateListener iFiveGStateListener = (IFiveGStateListener) this.mStatesListeners.get(i);
            if (iFiveGStateListener != null) {
                iFiveGStateListener.onStateChanged(currentServiceState);
            }
            this.mHandler.sendEmptyMessage(1026);
        }
    }

    /* access modifiers changed from: private */
    public void initFiveGServiceState() {
        StringBuilder sb = new StringBuilder();
        sb.append("initFiveGServiceState size=");
        sb.append(this.mStatesListeners.size());
        Log.d("FiveGServiceClient", sb.toString());
        for (int i = 0; i < this.mStatesListeners.size(); i++) {
            initFiveGServiceState(this.mStatesListeners.keyAt(i));
        }
    }

    private void initFiveGServiceState(int i) {
        String str = "initFiveGServiceState: Exception = ";
        StringBuilder sb = new StringBuilder();
        sb.append("mNetworkService=");
        sb.append(this.mNetworkService);
        sb.append(" mClient=");
        sb.append(this.mClient);
        String str2 = "FiveGServiceClient";
        Log.d(str2, sb.toString());
        if (this.mNetworkService != null && this.mClient != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("query 5G service state for phoneId ");
            sb2.append(i);
            Log.d(str2, sb2.toString());
            try {
                Token queryNrDcParam = this.mNetworkService.queryNrDcParam(i, this.mClient);
                StringBuilder sb3 = new StringBuilder();
                sb3.append("queryNrDcParam result:");
                sb3.append(queryNrDcParam);
                Log.d(str2, sb3.toString());
                Token queryNrBearerAllocation = this.mNetworkService.queryNrBearerAllocation(i, this.mClient);
                StringBuilder sb4 = new StringBuilder();
                sb4.append("queryNrBearerAllocation result:");
                sb4.append(queryNrBearerAllocation);
                Log.d(str2, sb4.toString());
                Token queryNrSignalStrength = this.mNetworkService.queryNrSignalStrength(i, this.mClient);
                StringBuilder sb5 = new StringBuilder();
                sb5.append("queryNrSignalStrength result:");
                sb5.append(queryNrSignalStrength);
                Log.d(str2, sb5.toString());
                Token queryUpperLayerIndInfo = this.mNetworkService.queryUpperLayerIndInfo(i, this.mClient);
                StringBuilder sb6 = new StringBuilder();
                sb6.append("queryUpperLayerIndInfo result:");
                sb6.append(queryUpperLayerIndInfo);
                Log.d(str2, sb6.toString());
                Token query5gConfigInfo = this.mNetworkService.query5gConfigInfo(i, this.mClient);
                StringBuilder sb7 = new StringBuilder();
                sb7.append("query5gConfigInfo result:");
                sb7.append(query5gConfigInfo);
                Log.d(str2, sb7.toString());
                Token queryNrIconType = this.mNetworkService.queryNrIconType(i, this.mClient);
                StringBuilder sb8 = new StringBuilder();
                sb8.append("queryNrIconType result:");
                sb8.append(queryNrIconType);
                Log.d(str2, sb8.toString());
            } catch (DeadObjectException e) {
                StringBuilder sb9 = new StringBuilder();
                sb9.append(str);
                sb9.append(e);
                Log.e(str2, sb9.toString());
                Log.d(str2, "try to re-binder service");
                this.mInitRetryTimes = 0;
                this.mServiceConnected = false;
                this.mNetworkService = null;
                this.mClient = null;
                binderService();
            } catch (Exception e2) {
                StringBuilder sb10 = new StringBuilder();
                sb10.append(str);
                sb10.append(e2);
                Log.d(str2, sb10.toString());
                if (this.mInitRetryTimes < 4 && !this.mHandler.hasMessages(1025)) {
                    this.mHandler.sendEmptyMessageDelayed(1025, (long) ((this.mInitRetryTimes * 2000) + 3000));
                    this.mInitRetryTimes++;
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void update5GIcon(FiveGServiceState fiveGServiceState, int i) {
        if (fiveGServiceState.mNrConfigType == 1) {
            fiveGServiceState.mIconGroup = getSaIcon(fiveGServiceState);
        } else if (fiveGServiceState.mNrConfigType == 0) {
            if (showFake5GIcon(i)) {
                fiveGServiceState.mNrIconType = 1;
            }
            fiveGServiceState.mIconGroup = getNrIconGroup(fiveGServiceState.mNrIconType, i);
        } else {
            fiveGServiceState.mIconGroup = TelephonyIcons.UNKNOWN;
        }
    }

    private boolean showFake5GIcon(int i) {
        FiveGServiceState currentServiceState = getCurrentServiceState(i);
        if (currentServiceState == null || this.mRsrpThresholds == null) {
            return false;
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("showFake5GIcon:");
            sb.append(currentServiceState.mRsrp);
            sb.append("mRsrpThresholds[mRsrpThresholds.length-1]:");
            int[] iArr = this.mRsrpThresholds;
            sb.append(iArr[iArr.length - 1]);
            sb.append(" mRsrpThresholds[0]:");
            sb.append(this.mRsrpThresholds[0]);
            Log.d("FiveGServiceClient", sb.toString());
        }
        int[] iArr2 = this.mRsrpThresholds;
        if (iArr2[iArr2.length - 1] < currentServiceState.mRsrp || currentServiceState.mRsrp < this.mRsrpThresholds[0]) {
            return false;
        }
        return true;
    }

    private MobileIconGroup getSaIcon(FiveGServiceState fiveGServiceState) {
        if (fiveGServiceState.mBearerAllocationStatus > 0) {
            return TelephonyIcons.FIVE_G_SA;
        }
        return TelephonyIcons.UNKNOWN;
    }

    private MobileIconGroup getNrIconGroup(int i, int i2) {
        MobileIconGroup mobileIconGroup = TelephonyIcons.UNKNOWN;
        if (i == 1) {
            return TelephonyIcons.FIVE_G_BASIC;
        }
        if (i != 2) {
            return mobileIconGroup;
        }
        return TelephonyIcons.FIVE_G_UWB;
    }

    /* access modifiers changed from: private */
    public void notifyMonitorCallback() {
        for (int i = 0; i < this.mKeyguardUpdateMonitorCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mKeyguardUpdateMonitorCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }
}
