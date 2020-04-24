package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.Global;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager.CapabilityCallback;
import android.telephony.ims.ImsMmTelManager.RegistrationCallback;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.airbnb.lottie.C0526R;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsManager.Connector;
import com.android.ims.ImsManager.Connector.Listener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.settingslib.Utils;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.policy.FiveGServiceClient.FiveGServiceState;
import com.android.systemui.statusbar.policy.FiveGServiceClient.IFiveGStateListener;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;
import java.util.regex.Pattern;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private static final boolean[] LTE_DEFAULT_STATUS = {false, false, false, false, false, false};
    private static String[] SHOW_LTE_OPERATORS = {"310004", "310005", "310012", "311480", "311481-9", "310026", "310160", "310170", "310200", "310210", "310220", "310230", "310240", "310250", "310260", "310270", "310280", "310290", "310310", "310330", "310490", "310580", "310660", "310800", "310090", "310150", "310380", "310410", "310560", "310680", "310980", "310990", "310120", "316010", "310020", "23203", "23207", "26002", "26201", "23001", "26006"};
    private int MAX_NOTIFYLISTENER_INTERVAL = 100;
    private final int MSG_NOTIFY_LISTENER_IF_NESSARY = 1000;
    private final int MSG_RECOVER_DATA = 100;
    private final int MSG_UPDATE_ACTIVITY_DELAY = C0526R.styleable.AppCompatTheme_textAppearanceLargePopupMenu;
    private final int MSG_UPDATE_TELEPHONY_DELAY = C0526R.styleable.AppCompatTheme_switchStyle;
    private final int NUM_LEVELS_ON_5G;
    private final int UPDATE_ACTIVITY_DELAY_DURATION = 500;
    int mBackupDataNetType = 0;
    /* access modifiers changed from: private */
    public int mCallState = 0;
    private CapabilityCallback mCapabilityCallback = new CapabilityCallback() {
        public void onCapabilitiesStatusChanged(MmTelCapabilities mmTelCapabilities) {
            ((MobileState) MobileSignalController.this.mCurrentState).voiceCapable = mmTelCapabilities.isCapable(1);
            ((MobileState) MobileSignalController.this.mCurrentState).videoCapable = mmTelCapabilities.isCapable(2);
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onCapabilitiesStatusChanged isVoiceCapable=");
            sb.append(((MobileState) MobileSignalController.this.mCurrentState).voiceCapable);
            sb.append(" isVideoCapable=");
            sb.append(((MobileState) MobileSignalController.this.mCurrentState).videoCapable);
            Log.d(str, sb.toString());
            MobileSignalController.this.notifyListenersIfNecessary();
        }
    };
    /* access modifiers changed from: private */
    public Config mConfig;
    /* access modifiers changed from: private */
    public int mDataNetType = 0;
    /* access modifiers changed from: private */
    public int mDataState = 0;
    private MobileIconGroup mDefaultIcons;
    private final SubscriptionDefaults mDefaults;
    @VisibleForTesting
    FiveGServiceState mFiveGState;
    @VisibleForTesting
    FiveGStateListener mFiveGStateListener;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public ImsManager mImsManager;
    private Connector mImsManagerConnector;
    private final RegistrationCallback mImsRegistrationCallback = new RegistrationCallback() {
        public void onRegistered(int i) {
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onRegistered imsTransportType=");
            sb.append(i);
            Log.d(str, sb.toString());
            MobileSignalController mobileSignalController = MobileSignalController.this;
            ((MobileState) mobileSignalController.mCurrentState).imsRegistered = true;
            mobileSignalController.notifyListenersIfNecessary();
        }

        public void onRegistering(int i) {
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onRegistering imsTransportType=");
            sb.append(i);
            Log.d(str, sb.toString());
            MobileSignalController mobileSignalController = MobileSignalController.this;
            ((MobileState) mobileSignalController.mCurrentState).imsRegistered = false;
            mobileSignalController.notifyListenersIfNecessary();
        }

        public void onUnregistered(ImsReasonInfo imsReasonInfo) {
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onDeregistered imsReasonInfo=");
            sb.append(imsReasonInfo);
            Log.d(str, sb.toString());
            MobileSignalController mobileSignalController = MobileSignalController.this;
            ((MobileState) mobileSignalController.mCurrentState).imsRegistered = false;
            mobileSignalController.notifyListenersIfNecessary();
        }
    };
    @VisibleForTesting
    boolean mInflateSignalStrengths = false;
    boolean mIsRemainCa = false;
    /* access modifiers changed from: private */
    public boolean[] mLTEStatus = LTE_DEFAULT_STATUS;
    /* access modifiers changed from: private */
    public long mLastUpdateActivityTime = 0;
    /* access modifiers changed from: private */
    public long mLastUpdateTime = 0;
    private boolean mListening = false;
    private DataState mMMSDataState = DataState.DISCONNECTED;
    private String mMccmnc = "";
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup = new SparseArray<>();
    BroadcastReceiver mOPMoblileReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean z = false;
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                if (MobileSignalController.this.mSubscriptionInfo != null) {
                    int intExtra = intent.getIntExtra("slot", 0);
                    int intExtra2 = intent.getIntExtra("subscription", -1);
                    String stringExtra = intent.getStringExtra("ss");
                    if (SignalController.DEBUG) {
                        String str = MobileSignalController.this.mTag;
                        StringBuilder sb = new StringBuilder();
                        sb.append("onSIMstateChange state: ");
                        sb.append(stringExtra);
                        sb.append(" slotId: ");
                        sb.append(intExtra);
                        sb.append(" subId ");
                        sb.append(intExtra2);
                        sb.append(" getSimSlotIndex: ");
                        sb.append(MobileSignalController.this.getSimSlotIndex());
                        Log.v(str, sb.toString());
                    }
                    if (MobileSignalController.this.getSimSlotIndex() == intExtra || MobileSignalController.this.mSubscriptionInfo.getSubscriptionId() == intExtra2) {
                        MobileSignalController mobileSignalController = MobileSignalController.this;
                        ((MobileState) mobileSignalController.mCurrentState).simstate = stringExtra;
                        mobileSignalController.customizeIconsMap();
                        MobileSignalController.this.updateTelephony();
                    }
                }
            } else if ("android.intent.action.PHONE_STATE".equals(action)) {
                MobileSignalController.this.onPhoneStateChange(intent.getStringExtra("state"));
            } else if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                MobileSignalController mobileSignalController2 = MobileSignalController.this;
                MobileState mobileState = (MobileState) mobileSignalController2.mCurrentState;
                if (mobileSignalController2.getDefaultDataSubId() == MobileSignalController.this.getSubId()) {
                    z = true;
                }
                mobileState.isDefaultDataSubId = z;
                MobileSignalController.this.updateTelephony();
            }
        }
    };
    private final ContentObserver mObserver;
    private final TelephonyManager mPhone;
    private int mPhoneState;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    /* access modifiers changed from: private */
    public ServiceState mServiceState;
    /* access modifiers changed from: private */
    public SignalStrength mSignalStrength;
    final SubscriptionInfo mSubscriptionInfo;
    /* access modifiers changed from: private */
    public int mVoiceNetType = 0;
    private final BroadcastReceiver mVolteSwitchObserver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("action=");
            sb.append(intent.getAction());
            Log.d(str, sb.toString());
            if (MobileSignalController.this.mConfig.showVolteIcon) {
                MobileSignalController.this.notifyListeners();
            }
        }
    };

    class FiveGStateListener implements IFiveGStateListener {
        FiveGStateListener() {
        }

        public void onStateChanged(FiveGServiceState fiveGServiceState) {
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onStateChanged: state=");
            sb.append(fiveGServiceState);
            Log.d(str, sb.toString());
            MobileSignalController mobileSignalController = MobileSignalController.this;
            mobileSignalController.mFiveGState = fiveGServiceState;
            mobileSignalController.notifyListeners();
        }
    }

    static class MobileIconGroup extends IconGroup {
        final int mActivityId;
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;
        final int mSingleSignalIcon;
        final int[] mStackedDataIcon;
        final int[] mStackedVoiceIcon;

        public MobileIconGroup(String str, int[][] iArr, int[][] iArr2, int[] iArr3, int i, int i2, int i3, int i4, int i5, int i6, int i7, boolean z) {
            this(str, iArr, iArr2, iArr3, i, i2, i3, i4, i5, i6, i7, z, 0, new int[2], new int[2], 0);
        }

        public MobileIconGroup(String str, int[][] iArr, int[][] iArr2, int[] iArr3, int i, int i2, int i3, int i4, int i5, int i6, int i7, boolean z, int i8, int[] iArr4, int[] iArr5, int i9) {
            int i10 = i7;
            super(str, iArr, iArr2, iArr3, i, i2, i3, i4, i5);
            this.mDataContentDescription = i6;
            this.mDataType = i10;
            this.mIsWide = z;
            this.mQsDataType = i10;
            this.mSingleSignalIcon = i8;
            this.mStackedDataIcon = iArr4;
            this.mStackedVoiceIcon = iArr5;
            this.mActivityId = i9;
        }

        public MobileIconGroup(MobileIconGroup mobileIconGroup, int i, int i2, int i3, int i4, boolean z, boolean z2) {
            MobileIconGroup mobileIconGroup2 = mobileIconGroup;
            boolean z3 = z2;
            this(mobileIconGroup2.mName, mobileIconGroup2.mSbIcons, mobileIconGroup2.mQsIcons, mobileIconGroup2.mContentDesc, mobileIconGroup2.mSbNullState, mobileIconGroup2.mQsNullState, mobileIconGroup2.mSbDiscState, mobileIconGroup2.mQsDiscState, mobileIconGroup2.mDiscContentDesc, mobileIconGroup2.mDataContentDescription, mobileIconGroup2.mDataType, mobileIconGroup2.mIsWide, 0, TelephonyIcons.getStackedDataIcon(i, i4, z3), TelephonyIcons.getStackedVoiceIcon(i2, i3, z, z3), 0);
        }
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(Looper looper) {
            super(looper);
        }

        public void onImsCapabilityStatusChange(boolean[] zArr) {
            StringBuffer stringBuffer = new StringBuffer();
            boolean z = false;
            for (int i = 0; i < zArr.length; i++) {
                if (MobileSignalController.this.mLTEStatus[i] != zArr[i]) {
                    stringBuffer.append(String.valueOf(zArr[i]).toUpperCase());
                    z = true;
                } else {
                    stringBuffer.append(String.valueOf(zArr[i]).toLowerCase());
                }
                stringBuffer.append(",");
            }
            MobileSignalController.this.mLTEStatus = zArr;
            if (z || SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onImsCapabilityStatusChange:");
                sb.append(stringBuffer.toString());
                Log.i(str, sb.toString());
            }
            NetworkControllerImpl networkControllerImpl = MobileSignalController.this.mNetworkController;
            if (networkControllerImpl != null && z) {
                networkControllerImpl.onLTEStatusUpdate();
            }
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (MobileSignalController.this.mSignalStrength != null) {
                int i = signalStrength.getSmoothSignalLevelAll()[0];
                int i2 = signalStrength.getSmoothSignalLevelAll()[1];
                int smoothSignalLevel = signalStrength.getSmoothSignalLevel();
                int i3 = MobileSignalController.this.mSignalStrength.getSmoothSignalLevelAll()[0];
                int i4 = MobileSignalController.this.mSignalStrength.getSmoothSignalLevelAll()[1];
                int smoothSignalLevel2 = MobileSignalController.this.mSignalStrength.getSmoothSignalLevel();
                if (!(i == i3 && i2 == i4 && smoothSignalLevel == smoothSignalLevel2)) {
                    String str = MobileSignalController.this.mTag;
                    StringBuilder sb = new StringBuilder();
                    sb.append("onSignalStrengthsChanged signalStrength=");
                    sb.append(signalStrength);
                    sb.append(" level=");
                    sb.append(smoothSignalLevel);
                    sb.append(" voicelevel=");
                    sb.append(i);
                    sb.append(" datalevel=");
                    sb.append(i2);
                    Log.d(str, sb.toString());
                }
            }
            MobileSignalController.this.mSignalStrength = signalStrength;
            MobileSignalController.this.updateTelephony();
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            if (serviceState == null) {
                Log.d(MobileSignalController.this.mTag, "onServiceStateChanged / state == null");
                return;
            }
            String str = MobileSignalController.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("onServiceStateChanged voiceState=");
            sb.append(serviceState.getVoiceRegState());
            sb.append(" dataState=");
            sb.append(serviceState.getDataRegState());
            sb.append(" isUsingCarrierAggregation:");
            sb.append(serviceState.isUsingCarrierAggregation());
            sb.append(" getDataNetworkType:");
            sb.append(serviceState.getDataNetworkType());
            sb.append(" getVoiceNetworkType:");
            sb.append(serviceState.getVoiceNetworkType());
            Log.d(str, sb.toString());
            MobileSignalController.this.mServiceState = serviceState;
            if (MobileSignalController.this.mServiceState != null) {
                updateDataNetType(MobileSignalController.this.mServiceState.getDataNetworkType());
            }
            MobileSignalController.this.mVoiceNetType = serviceState.getVoiceNetworkType();
            MobileSignalController.this.makeDataNetTypeStable();
            MobileSignalController.this.updateTelephony();
        }

        public void onDataConnectionStateChanged(int i, int i2) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onDataConnectionStateChanged: state=");
                sb.append(i);
                sb.append(" type=");
                sb.append(i2);
                sb.append(" isUsingCarrierAggregation:");
                sb.append(MobileSignalController.this.mServiceState != null ? MobileSignalController.this.mServiceState.isUsingCarrierAggregation() : false);
                Log.d(str, sb.toString());
            }
            MobileSignalController.this.mDataState = i;
            updateDataNetType(i2);
            MobileSignalController.this.updateTelephony();
        }

        private void updateDataNetType(int i) {
            MobileSignalController.this.mDataNetType = i;
            if (MobileSignalController.this.mDataNetType == 13) {
                if (MobileSignalController.this.isCarrierSpecificDataIcon()) {
                    MobileSignalController.this.mDataNetType = 21;
                } else if (MobileSignalController.this.mServiceState != null && MobileSignalController.this.mServiceState.isUsingCarrierAggregation()) {
                    MobileSignalController.this.mDataNetType = 19;
                }
            }
            MobileSignalController.this.makeDataNetTypeStable();
            MobileSignalController.this.updateTelephony();
        }

        public void onDataActivity(int i) {
            MobileSignalController.this.setActivity(i);
        }

        public void onCarrierNetworkChange(boolean z) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onCarrierNetworkChange: active=");
                sb.append(z);
                Log.d(str, sb.toString());
            }
            MobileSignalController mobileSignalController = MobileSignalController.this;
            ((MobileState) mobileSignalController.mCurrentState).carrierNetworkChangeMode = z;
            mobileSignalController.updateTelephony();
        }

        public void onCallStateChanged(int i, String str) {
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onCallStateChanged: state=");
                sb.append(i);
                Log.d(str2, sb.toString());
            }
            MobileSignalController.this.mCallState = i;
            MobileSignalController.this.updateTelephony();
        }
    }

    static class MobileState extends State {
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        boolean dataConnected;
        boolean dataSim;
        boolean imsRegistered;
        boolean isDefault;
        boolean isDefaultDataSubId;
        boolean isDemoMode;
        boolean isEmergency;
        boolean isVirtual;
        String networkName;
        String networkNameData;
        boolean roaming;
        String simstate;
        boolean userSetup;
        boolean videoCapable;
        boolean voiceCapable;

        MobileState() {
        }

        public void copyFrom(State state) {
            super.copyFrom(state);
            MobileState mobileState = (MobileState) state;
            this.dataSim = mobileState.dataSim;
            this.networkName = mobileState.networkName;
            this.networkNameData = mobileState.networkNameData;
            this.dataConnected = mobileState.dataConnected;
            this.isDefault = mobileState.isDefault;
            this.isEmergency = mobileState.isEmergency;
            this.airplaneMode = mobileState.airplaneMode;
            this.carrierNetworkChangeMode = mobileState.carrierNetworkChangeMode;
            this.userSetup = mobileState.userSetup;
            this.roaming = mobileState.roaming;
            this.imsRegistered = mobileState.imsRegistered;
            this.voiceCapable = mobileState.voiceCapable;
            this.videoCapable = mobileState.videoCapable;
            this.simstate = mobileState.simstate;
            this.isDefaultDataSubId = mobileState.isDefaultDataSubId;
            this.isVirtual = mobileState.isVirtual;
            this.isDemoMode = mobileState.isDemoMode;
        }

        /* access modifiers changed from: protected */
        public void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(',');
            sb.append("dataSim=");
            sb.append(this.dataSim);
            sb.append(',');
            sb.append("networkName=");
            sb.append(this.networkName);
            sb.append(',');
            sb.append("networkNameData=");
            sb.append(this.networkNameData);
            sb.append(',');
            sb.append("dataConnected=");
            sb.append(this.dataConnected);
            sb.append(',');
            sb.append("roaming=");
            sb.append(this.roaming);
            sb.append(',');
            sb.append("isDefault=");
            sb.append(this.isDefault);
            sb.append(',');
            sb.append("isEmergency=");
            sb.append(this.isEmergency);
            sb.append(',');
            sb.append("airplaneMode=");
            sb.append(this.airplaneMode);
            sb.append(',');
            sb.append("carrierNetworkChangeMode=");
            sb.append(this.carrierNetworkChangeMode);
            sb.append(',');
            sb.append("userSetup=");
            sb.append(this.userSetup);
            sb.append(',');
            sb.append("imsRegistered=");
            sb.append(this.imsRegistered);
            sb.append(',');
            sb.append("voiceCapable=");
            sb.append(this.voiceCapable);
            sb.append(',');
            sb.append("videoCapable=");
            sb.append(this.videoCapable);
            sb.append("simstate=");
            sb.append(this.simstate);
            sb.append(',');
            sb.append("isDefaultDataSubId=");
            sb.append(this.isDefaultDataSubId);
            sb.append(',');
            sb.append("isVirtual");
            sb.append(this.isVirtual);
            sb.append("isDemoMode");
            sb.append(this.isDemoMode);
        }

        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                MobileState mobileState = (MobileState) obj;
                if (Objects.equals(mobileState.networkName, this.networkName) && Objects.equals(mobileState.networkNameData, this.networkNameData) && mobileState.dataSim == this.dataSim && mobileState.dataConnected == this.dataConnected && mobileState.isEmergency == this.isEmergency && mobileState.airplaneMode == this.airplaneMode && mobileState.carrierNetworkChangeMode == this.carrierNetworkChangeMode && mobileState.userSetup == this.userSetup && mobileState.isDefault == this.isDefault && mobileState.roaming == this.roaming && mobileState.imsRegistered == this.imsRegistered && mobileState.voiceCapable == this.voiceCapable && mobileState.videoCapable == this.videoCapable && mobileState.simstate == this.simstate && mobileState.isDefaultDataSubId == this.isDefaultDataSubId && mobileState.isVirtual == this.isVirtual && mobileState.isDemoMode == this.isDemoMode) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isVolteSwitchOn() {
        return false;
    }

    private void updateInflateSignalStrength() {
    }

    public MobileSignalController(Context context, Config config, boolean z, TelephonyManager telephonyManager, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, SubscriptionInfo subscriptionInfo, SubscriptionDefaults subscriptionDefaults, Looper looper) {
        String str;
        boolean z2 = z;
        Looper looper2 = looper;
        StringBuilder sb = new StringBuilder();
        sb.append("MobileSignalController(");
        sb.append(subscriptionInfo.getSubscriptionId());
        sb.append(")");
        String sb2 = sb.toString();
        super(sb2, context, 0, callbackHandler, networkControllerImpl);
        this.mConfig = config;
        this.mPhone = telephonyManager;
        this.mDefaults = subscriptionDefaults;
        this.mSubscriptionInfo = subscriptionInfo;
        this.mFiveGStateListener = new FiveGStateListener();
        this.mFiveGState = new FiveGServiceState();
        this.mPhoneStateListener = new MobilePhoneStateListener(looper2);
        this.mNetworkNameSeparator = getStringIfExists(R$string.status_bar_network_name_separator);
        this.mNetworkNameDefault = getStringIfExists(17040261);
        this.mHandler = new Handler(looper2) {
            public void handleMessage(Message message) {
                int i = message.what;
                if (i != 1000) {
                    switch (i) {
                        case C0526R.styleable.AppCompatTheme_spinnerStyle /*100*/:
                            MobileSignalController.this.recoverDataNetTypeStable();
                            return;
                        case C0526R.styleable.AppCompatTheme_switchStyle /*101*/:
                            MobileSignalController.this.updateTelephony();
                            return;
                        case C0526R.styleable.AppCompatTheme_textAppearanceLargePopupMenu /*102*/:
                            MobileSignalController.this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_textAppearanceLargePopupMenu);
                            MobileSignalController.this.mLastUpdateActivityTime = System.currentTimeMillis();
                            if (SignalController.DEBUG) {
                                String str = MobileSignalController.this.mTag;
                                StringBuilder sb = new StringBuilder();
                                sb.append("update activity activityIn=");
                                sb.append(((MobileState) MobileSignalController.this.mCurrentState).activityIn);
                                sb.append(" activityOut=");
                                sb.append(((MobileState) MobileSignalController.this.mCurrentState).activityOut);
                                Log.d(str, sb.toString());
                            }
                            MobileSignalController.this.notifyListenersIfNecessary();
                            return;
                        default:
                            return;
                    }
                } else {
                    MobileSignalController.this.mLastUpdateTime = System.currentTimeMillis();
                    MobileSignalController.this.notifyListenersIfNecessary();
                    if (SignalController.DEBUG) {
                        Log.i(MobileSignalController.this.mTag, "notifyIfNecessary");
                    }
                }
            }
        };
        customizeIconsMap();
        if (subscriptionInfo.getCarrierName() != null) {
            str = subscriptionInfo.getCarrierName().toString();
        } else {
            str = this.mNetworkNameDefault;
        }
        T t = this.mLastState;
        MobileState mobileState = (MobileState) t;
        T t2 = this.mCurrentState;
        ((MobileState) t2).networkName = str;
        mobileState.networkName = str;
        MobileState mobileState2 = (MobileState) t;
        ((MobileState) t2).networkNameData = str;
        mobileState2.networkNameData = str;
        MobileState mobileState3 = (MobileState) t;
        ((MobileState) t2).enabled = z2;
        mobileState3.enabled = z2;
        MobileState mobileState4 = (MobileState) t;
        MobileState mobileState5 = (MobileState) t2;
        MobileIconGroup mobileIconGroup = this.mDefaultIcons;
        mobileState5.iconGroup = mobileIconGroup;
        mobileState4.iconGroup = mobileIconGroup;
        updateDataSim();
        this.NUM_LEVELS_ON_5G = FiveGServiceClient.getNumLevels(this.mContext);
        this.mImsManagerConnector = new Connector(this.mContext, this.mSubscriptionInfo.getSimSlotIndex(), new Listener() {
            public void connectionReady(ImsManager imsManager) throws ImsException {
                Log.d(MobileSignalController.this.mTag, "ImsManager: connection ready.");
                MobileSignalController.this.mImsManager = imsManager;
                MobileSignalController.this.setListeners();
            }

            public void connectionUnavailable() {
                Log.d(MobileSignalController.this.mTag, "ImsManager: connection unavailable.");
                MobileSignalController.this.removeListeners();
            }
        });
        this.mObserver = new ContentObserver(new Handler(looper2)) {
            public void onChange(boolean z) {
                MobileSignalController.this.updateTelephony();
            }
        };
    }

    public void setConfiguration(Config config) {
        this.mConfig = config;
        updateInflateSignalStrength();
        customizeIconsMap();
        updateTelephony();
    }

    public void setAirplaneMode(boolean z) {
        ((MobileState) this.mCurrentState).airplaneMode = z;
        mayNotifyListeners();
    }

    public void setUserSetupComplete(boolean z) {
        ((MobileState) this.mCurrentState).userSetup = z;
        mayNotifyListeners();
    }

    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        boolean z = bitSet2.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = bitSet.get(this.mTransportType);
        int i = 0;
        ((MobileState) this.mCurrentState).isDefaultDataSubId = getDefaultDataSubId() == getSubId();
        T t = this.mCurrentState;
        if (((MobileState) t).isDefaultDataSubId) {
            MobileState mobileState = (MobileState) t;
            if (z || !((MobileState) t).isDefault) {
                i = 1;
            }
            mobileState.inetCondition = i;
        } else {
            ((MobileState) t).inetCondition = 1;
        }
        mayNotifyListeners();
    }

    public void setCarrierNetworkChangeMode(boolean z) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = z;
        updateTelephony();
    }

    public void registerListener() {
        this.mPhone.listen(this.mPhoneStateListener, -2147417631);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mContext.registerReceiver(this.mOPMoblileReceiver, intentFilter, null, this.mHandler);
        String str = "mobile_data";
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(str), true, this.mObserver);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(this.mSubscriptionInfo.getSubscriptionId());
        contentResolver.registerContentObserver(Global.getUriFor(sb.toString()), true, this.mObserver);
        this.mListening = true;
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(str), true, this.mObserver);
        ContentResolver contentResolver2 = this.mContext.getContentResolver();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append(this.mSubscriptionInfo.getSubscriptionId());
        contentResolver2.registerContentObserver(Global.getUriFor(sb2.toString()), true, this.mObserver);
        this.mContext.registerReceiver(this.mVolteSwitchObserver, new IntentFilter("org.codeaurora.intent.action.ACTION_ENHANCE_4G_SWITCH"));
        this.mImsManagerConnector.connect();
    }

    public void unregisterListener() {
        if (this.mListening) {
            this.mPhone.listen(this.mPhoneStateListener, 0);
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            this.mContext.unregisterReceiver(this.mVolteSwitchObserver);
            this.mImsManagerConnector.disconnect();
            this.mContext.unregisterReceiver(this.mOPMoblileReceiver);
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            cleanLTEStatus();
            NetworkControllerImpl networkControllerImpl = this.mNetworkController;
            if (networkControllerImpl != null) {
                networkControllerImpl.onLTEStatusUpdate();
            } else {
                Log.w(this.mTag, "unregisterListener mNetworkController is null");
            }
        }
    }

    private void mapIconSets() {
        opMapIconSets();
    }

    private void opMapIconSets() {
        this.mNetworkToIconLookup.clear();
        this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(17, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(20, TelephonyIcons.FIVE_G_SA);
        if (!this.mConfig.showAtLeast3G) {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.UNKNOWN);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.f100E);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.ONE_X);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.ONE_X);
            this.mDefaultIcons = TelephonyIcons.f101G;
        } else {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
            this.mDefaultIcons = TelephonyIcons.THREE_G;
        }
        MobileIconGroup mobileIconGroup = TelephonyIcons.f102H;
        MobileIconGroup mobileIconGroup2 = TelephonyIcons.H_PLUS;
        this.mNetworkToIconLookup.put(8, mobileIconGroup);
        this.mNetworkToIconLookup.put(9, mobileIconGroup);
        this.mNetworkToIconLookup.put(10, mobileIconGroup2);
        this.mNetworkToIconLookup.put(15, mobileIconGroup2);
        this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
        this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_PLUS);
        this.mNetworkToIconLookup.put(21, TelephonyIcons.LTE_CA_5G_E);
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
    }

    private int getNumLevels() {
        if (this.mInflateSignalStrengths) {
            return SignalStrength.NUM_SIGNAL_STRENGTH_BINS + 1;
        }
        return SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
    }

    public int getCurrentIconId() {
        if (((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        }
        if (isVirtualSim()) {
            return TelephonyIcons.getOneplusVirtualSimSignalIconId(((MobileState) this.mCurrentState).level);
        }
        if (((MobileState) this.mCurrentState).roaming && !showStacked(this.mDataNetType) && !OpUtils.isRemoveRoamingIcon()) {
            return TelephonyIcons.getOneplusRoamingSignalIconId(((MobileState) this.mCurrentState).level);
        }
        T t = this.mCurrentState;
        if (((MobileState) t).connected) {
            int signalLevel = is5GConnected() ? this.mFiveGState.getSignalLevel() : ((MobileState) this.mCurrentState).level;
            if (is5GConnected() && this.mFiveGState.getRsrp() == -32768) {
                signalLevel = ((MobileState) this.mCurrentState).level;
            }
            if (this.mInflateSignalStrengths) {
                signalLevel++;
            }
            if (this.mConfig.readIconsFromXml) {
                return ((MobileIconGroup) getIcons()).mSingleSignalIcon;
            }
            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[((MobileState) this.mCurrentState).inetCondition][signalLevel];
        } else if (((MobileState) t).enabled) {
            return TelephonyIcons.TELEPHONY_NO_NETWORK;
        } else {
            return 0;
        }
    }

    public int getQsCurrentIconId() {
        return getCurrentIconId();
    }

    private int getVolteResId() {
        int voiceNetworkType = getVoiceNetworkType();
        T t = this.mCurrentState;
        if ((((MobileState) t).voiceCapable || ((MobileState) t).videoCapable) && ((MobileState) this.mCurrentState).imsRegistered) {
            return R$drawable.ic_volte;
        }
        int i = this.mDataNetType;
        if (i == 13 || i == 19 || voiceNetworkType == 13 || voiceNetworkType == 19) {
            return R$drawable.ic_volte_no_voice;
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public void setListeners() {
        String str = " into ";
        String str2 = "slot:";
        ImsManager imsManager = this.mImsManager;
        if (imsManager == null) {
            Log.e(this.mTag, "setListeners mImsManager is null");
            return;
        }
        try {
            imsManager.addCapabilitiesCallback(this.mCapabilityCallback);
            this.mImsManager.addRegistrationCallback(this.mImsRegistrationCallback);
            String str3 = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append(getSimSlotIndex());
            sb.append("addCapabilitiesCallback ");
            sb.append(this.mCapabilityCallback);
            sb.append(str);
            sb.append(this.mImsManager);
            Log.d(str3, sb.toString());
            String str4 = this.mTag;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str2);
            sb2.append(getSimSlotIndex());
            sb2.append("addRegistrationCallback ");
            sb2.append(this.mImsRegistrationCallback);
            sb2.append(str);
            sb2.append(this.mImsManager);
            Log.d(str4, sb2.toString());
        } catch (ImsException unused) {
            Log.d(this.mTag, "unable to addCapabilitiesCallback callback.");
        }
        queryImsState();
    }

    private void queryImsState() {
        TelephonyManager createForSubscriptionId = this.mPhone.createForSubscriptionId(this.mSubscriptionInfo.getSubscriptionId());
        ((MobileState) this.mCurrentState).voiceCapable = createForSubscriptionId.isVolteAvailable();
        ((MobileState) this.mCurrentState).videoCapable = createForSubscriptionId.isVideoTelephonyAvailable();
        ((MobileState) this.mCurrentState).imsRegistered = this.mPhone.isImsRegistered(this.mSubscriptionInfo.getSubscriptionId());
        if (SignalController.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("queryImsState tm=");
            sb.append(createForSubscriptionId);
            sb.append(" phone=");
            sb.append(this.mPhone);
            sb.append(" voiceCapable=");
            sb.append(((MobileState) this.mCurrentState).voiceCapable);
            sb.append(" videoCapable=");
            sb.append(((MobileState) this.mCurrentState).videoCapable);
            sb.append(" imsResitered=");
            sb.append(((MobileState) this.mCurrentState).imsRegistered);
            Log.d(str, sb.toString());
        }
        notifyListenersIfNecessary();
    }

    /* access modifiers changed from: private */
    public void removeListeners() {
        String str = " from ";
        ImsManager imsManager = this.mImsManager;
        String str2 = "slot:";
        if (imsManager == null) {
            String str3 = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append(getSimSlotIndex());
            sb.append(" removeListeners mImsManager is null");
            Log.e(str3, sb.toString());
            return;
        }
        try {
            imsManager.removeCapabilitiesCallback(this.mCapabilityCallback);
            this.mImsManager.removeRegistrationListener(this.mImsRegistrationCallback);
            String str4 = this.mTag;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str2);
            sb2.append(getSimSlotIndex());
            sb2.append(" removeCapabilitiesCallback ");
            sb2.append(this.mCapabilityCallback);
            sb2.append(str);
            sb2.append(this.mImsManager);
            Log.d(str4, sb2.toString());
            String str5 = this.mTag;
            StringBuilder sb3 = new StringBuilder();
            sb3.append(str2);
            sb3.append(getSimSlotIndex());
            sb3.append(" removeRegistrationCallback ");
            sb3.append(this.mImsRegistrationCallback);
            sb3.append(str);
            sb3.append(this.mImsManager);
            Log.d(str5, sb3.toString());
        } catch (ImsException unused) {
            Log.d(this.mTag, "unable to remove callback.");
        }
    }

    private void mayNotifyListeners() {
        long currentTimeMillis = System.currentTimeMillis() - this.mLastUpdateTime;
        Message message = new Message();
        message.what = 1000;
        if (currentTimeMillis < ((long) this.MAX_NOTIFYLISTENER_INTERVAL)) {
            this.mHandler.removeMessages(1000);
            this.mHandler.sendMessageDelayed(message, 50);
            return;
        }
        this.mHandler.sendMessage(message);
    }

    /* JADX WARNING: type inference failed for: r9v4 */
    /* JADX WARNING: type inference failed for: r9v6 */
    /* JADX WARNING: type inference failed for: r9v8, types: [java.lang.String] */
    /* JADX WARNING: type inference failed for: r9v10 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x016b  */
    /* JADX WARNING: Unknown variable types count: 2 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyListeners(com.android.systemui.statusbar.policy.NetworkController.SignalCallback r24) {
        /*
            r23 = this;
            r0 = r23
            boolean r1 = r0.mListening
            if (r1 != 0) goto L_0x0016
            T r1 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            boolean r1 = r1.isDemoMode
            if (r1 != 0) goto L_0x0016
            java.lang.String r0 = r0.mTag
            java.lang.String r1 = " Controller already unregister listener, don't notify change"
            android.util.Log.i(r0, r1)
            return
        L_0x0016:
            com.android.systemui.statusbar.policy.SignalController$IconGroup r1 = r23.getIcons()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileIconGroup) r1
            T r2 = r0.mCurrentState
            r3 = r2
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            com.android.systemui.statusbar.policy.SignalController$IconGroup r3 = r3.iconGroup
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r4 = com.android.systemui.statusbar.policy.TelephonyIcons.DATA_DISABLED
            r5 = 1
            r6 = 0
            if (r3 == r4) goto L_0x0031
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r2 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r2
            com.android.systemui.statusbar.policy.SignalController$IconGroup r2 = r2.iconGroup
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r3 = com.android.systemui.statusbar.policy.TelephonyIcons.NOT_DEFAULT_DATA
            if (r2 != r3) goto L_0x003b
        L_0x0031:
            T r2 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r2 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r2
            boolean r2 = r2.userSetup
            if (r2 == 0) goto L_0x003b
            r2 = r5
            goto L_0x003c
        L_0x003b:
            r2 = r6
        L_0x003c:
            boolean r3 = r23.is5GConnected()
            if (r3 == 0) goto L_0x005a
            com.android.systemui.statusbar.policy.FiveGServiceClient$FiveGServiceState r3 = r0.mFiveGState
            boolean r3 = r3.isConnectedOnSaMode()
            if (r3 != 0) goto L_0x0054
            com.android.systemui.statusbar.policy.FiveGServiceClient$FiveGServiceState r3 = r0.mFiveGState
            boolean r3 = r3.isConnectedOnNsaMode()
            if (r3 == 0) goto L_0x005a
            if (r2 != 0) goto L_0x005a
        L_0x0054:
            com.android.systemui.statusbar.policy.FiveGServiceClient$FiveGServiceState r1 = r0.mFiveGState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r1 = r1.getIconGroup()
        L_0x005a:
            int r3 = r23.getContentDescription()
            java.lang.String r3 = r0.getStringIfExists(r3)
            int r4 = r1.mDataContentDescription
            java.lang.String r4 = r0.getStringIfExists(r4)
            T r7 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            int r7 = r7.inetCondition
            if (r7 != 0) goto L_0x0078
            android.content.Context r4 = r0.mContext
            int r7 = com.android.systemui.R$string.data_connection_no_internet
            java.lang.String r4 = r4.getString(r7)
        L_0x0078:
            r17 = r4
            T r4 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r4 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r4
            boolean r4 = r4.dataConnected
            if (r4 != 0) goto L_0x0087
            if (r2 == 0) goto L_0x0085
            goto L_0x0087
        L_0x0085:
            r4 = r6
            goto L_0x0088
        L_0x0087:
            r4 = r5
        L_0x0088:
            com.android.systemui.statusbar.policy.NetworkController$IconState r8 = new com.android.systemui.statusbar.policy.NetworkController$IconState
            T r7 = r0.mCurrentState
            r9 = r7
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r9 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r9
            boolean r9 = r9.enabled
            if (r9 == 0) goto L_0x009b
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.airplaneMode
            if (r7 != 0) goto L_0x009b
            r7 = r5
            goto L_0x009c
        L_0x009b:
            r7 = r6
        L_0x009c:
            int r9 = r23.getCurrentIconId()
            r8.<init>(r7, r9, r3)
            T r7 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.isDefault
            if (r7 != 0) goto L_0x00b0
            if (r2 == 0) goto L_0x00ae
            goto L_0x00b0
        L_0x00ae:
            r2 = r6
            goto L_0x00b1
        L_0x00b0:
            r2 = r5
        L_0x00b1:
            r2 = r2 & r4
            T r4 = r0.mCurrentState
            r7 = r4
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.dataConnected
            if (r7 == 0) goto L_0x00c3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r4 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r4
            boolean r4 = r4.isDefault
            if (r4 == 0) goto L_0x00c3
            r4 = r5
            goto L_0x00c4
        L_0x00c3:
            r4 = r6
        L_0x00c4:
            T r7 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.dataSim
            r9 = 0
            if (r7 == 0) goto L_0x0108
            if (r2 != 0) goto L_0x00d8
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r7 = r0.mConfig
            boolean r7 = r7.alwaysShowDataRatIcon
            if (r7 == 0) goto L_0x00d6
            goto L_0x00d8
        L_0x00d6:
            r7 = r6
            goto L_0x00da
        L_0x00d8:
            int r7 = r1.mQsDataType
        L_0x00da:
            com.android.systemui.statusbar.policy.NetworkController$IconState r10 = new com.android.systemui.statusbar.policy.NetworkController$IconState
            T r11 = r0.mCurrentState
            r12 = r11
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r12 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r12
            boolean r12 = r12.enabled
            if (r12 == 0) goto L_0x00ed
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r11 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r11
            boolean r11 = r11.isEmergency
            if (r11 != 0) goto L_0x00ed
            r11 = r5
            goto L_0x00ee
        L_0x00ed:
            r11 = r6
        L_0x00ee:
            int r12 = r23.getQsCurrentIconId()
            r10.<init>(r11, r12, r3)
            T r3 = r0.mCurrentState
            r11 = r3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r11 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r11
            boolean r11 = r11.isEmergency
            if (r11 == 0) goto L_0x00ff
            goto L_0x0103
        L_0x00ff:
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            java.lang.String r9 = r3.networkName
        L_0x0103:
            r11 = r7
            r18 = r9
            r9 = r10
            goto L_0x010b
        L_0x0108:
            r11 = r6
            r18 = r9
        L_0x010b:
            T r3 = r0.mCurrentState
            r7 = r3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.dataConnected
            if (r7 == 0) goto L_0x0123
            r7 = r3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.carrierNetworkChangeMode
            if (r7 != 0) goto L_0x0123
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            boolean r3 = r3.activityIn
            if (r3 == 0) goto L_0x0123
            r12 = r5
            goto L_0x0124
        L_0x0123:
            r12 = r6
        L_0x0124:
            T r3 = r0.mCurrentState
            r7 = r3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.dataConnected
            if (r7 == 0) goto L_0x013c
            r7 = r3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r7 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r7
            boolean r7 = r7.carrierNetworkChangeMode
            if (r7 != 0) goto L_0x013c
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            boolean r3 = r3.activityOut
            if (r3 == 0) goto L_0x013c
            r13 = r5
            goto L_0x013d
        L_0x013c:
            r13 = r6
        L_0x013d:
            if (r2 != 0) goto L_0x0154
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r3 = r0.mConfig
            boolean r5 = r3.alwaysShowDataRatIcon
            if (r5 != 0) goto L_0x0154
            boolean r3 = r3.alwaysShowNetworkTypeIcon
            if (r3 != 0) goto L_0x0154
            com.android.systemui.statusbar.policy.FiveGServiceClient$FiveGServiceState r3 = r0.mFiveGState
            boolean r3 = r3.isConnectedOnSaMode()
            if (r3 == 0) goto L_0x0152
            goto L_0x0154
        L_0x0152:
            r10 = r6
            goto L_0x0157
        L_0x0154:
            int r3 = r1.mDataType
            r10 = r3
        L_0x0157:
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r3 = r0.mConfig
            boolean r3 = r3.showVolteIcon
            if (r3 == 0) goto L_0x0167
            boolean r3 = r23.isVolteSwitchOn()
            if (r3 == 0) goto L_0x0167
            int r6 = r23.getVolteResId()
        L_0x0167:
            boolean r3 = com.android.systemui.statusbar.policy.SignalController.DEBUG
            if (r3 == 0) goto L_0x024b
            java.lang.String r3 = r0.mTag
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r7 = "notifyListeners mConfig.alwaysShowNetworkTypeIcon="
            r5.append(r7)
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r7 = r0.mConfig
            boolean r7 = r7.alwaysShowNetworkTypeIcon
            r5.append(r7)
            java.lang.String r7 = "  mDataNetType:"
            r5.append(r7)
            int r7 = r0.mDataNetType
            r5.append(r7)
            java.lang.String r7 = "/"
            r5.append(r7)
            int r14 = r0.mDataNetType
            java.lang.String r14 = android.telephony.TelephonyManager.getNetworkTypeName(r14)
            r5.append(r14)
            java.lang.String r14 = " voiceNetType="
            r5.append(r14)
            int r14 = r23.getVoiceNetworkType()
            r5.append(r14)
            r5.append(r7)
            int r7 = r23.getVoiceNetworkType()
            java.lang.String r7 = android.telephony.TelephonyManager.getNetworkTypeName(r7)
            r5.append(r7)
            java.lang.String r7 = " showDataIcon="
            r5.append(r7)
            r5.append(r2)
            java.lang.String r2 = " mConfig.alwaysShowDataRatIcon="
            r5.append(r2)
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r2 = r0.mConfig
            boolean r2 = r2.alwaysShowDataRatIcon
            r5.append(r2)
            java.lang.String r2 = " icons.mDataType="
            r5.append(r2)
            int r2 = r1.mDataType
            r5.append(r2)
            java.lang.String r2 = " mConfig.showVolteIcon="
            r5.append(r2)
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r2 = r0.mConfig
            boolean r2 = r2.showVolteIcon
            r5.append(r2)
            java.lang.String r2 = " isVolteSwitchOn="
            r5.append(r2)
            boolean r2 = r23.isVolteSwitchOn()
            r5.append(r2)
            java.lang.String r2 = " volteIcon="
            r5.append(r2)
            r5.append(r6)
            java.lang.String r2 = " mSubscriptionInfo.getSubscriptionId():"
            r5.append(r2)
            android.telephony.SubscriptionInfo r2 = r0.mSubscriptionInfo
            int r2 = r2.getSubscriptionId()
            r5.append(r2)
            java.lang.String r2 = " mCurrentState.roaming:"
            r5.append(r2)
            T r2 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r2 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r2
            boolean r2 = r2.roaming
            r5.append(r2)
            java.lang.String r2 = " showDataConnectedIcon:"
            r5.append(r2)
            r5.append(r4)
            java.lang.String r2 = " isUST:"
            r5.append(r2)
            boolean r2 = com.oneplus.util.OpUtils.isUST()
            r5.append(r2)
            java.lang.String r2 = " isUSS:"
            r5.append(r2)
            boolean r2 = com.oneplus.util.OpUtils.isUSS()
            r5.append(r2)
            java.lang.String r2 = " isInService:"
            r5.append(r2)
            android.telephony.ServiceState r2 = r0.mServiceState
            boolean r2 = com.android.settingslib.Utils.isInService(r2)
            r5.append(r2)
            java.lang.String r2 = " phoneId:"
            r5.append(r2)
            int r2 = r23.getSimSlotIndex()
            r5.append(r2)
            java.lang.String r2 = r5.toString()
            android.util.Log.d(r3, r2)
        L_0x024b:
            r14 = 0
            int[] r15 = r1.mStackedDataIcon
            int[] r2 = r1.mStackedVoiceIcon
            boolean r1 = r1.mIsWide
            android.telephony.SubscriptionInfo r3 = r0.mSubscriptionInfo
            int r20 = r3.getSubscriptionId()
            T r0 = r0.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            boolean r0 = r0.roaming
            r7 = r24
            r16 = r2
            r19 = r1
            r21 = r0
            r22 = r4
            r7.setMobileDataIndicators(r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21, r22)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.MobileSignalController.notifyListeners(com.android.systemui.statusbar.policy.NetworkController$SignalCallback):void");
    }

    /* access modifiers changed from: protected */
    public MobileState cleanState() {
        return new MobileState();
    }

    private boolean isCdma() {
        SignalStrength signalStrength = this.mSignalStrength;
        return signalStrength != null && !signalStrength.isGsm();
    }

    public boolean isEmergencyOnly() {
        ServiceState serviceState = this.mServiceState;
        return serviceState != null && serviceState.isEmergencyOnly();
    }

    private boolean isRoaming() {
        boolean z = false;
        if (isCarrierNetworkChangeActive() || this.mServiceState == null) {
            return false;
        }
        if (SignalController.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append(" isRoaming iconMode:");
            sb.append(this.mServiceState.getCdmaEriIconMode());
            sb.append(" EriIconIndex:");
            sb.append(this.mServiceState.getCdmaEriIconIndex());
            sb.append(" isRoaming:");
            ServiceState serviceState = this.mServiceState;
            sb.append(serviceState != null && serviceState.getRoaming());
            Log.d(str, sb.toString());
        }
        if (OpUtils.isUSS()) {
            ServiceState serviceState2 = this.mServiceState;
            if (serviceState2 != null && serviceState2.getRoaming()) {
                z = true;
            }
            return z;
        }
        if (isCdma()) {
            ServiceState serviceState3 = this.mServiceState;
            if (serviceState3 != null) {
                int cdmaEriIconMode = serviceState3.getCdmaEriIconMode();
                if (Utils.isInService(this.mServiceState) && this.mServiceState.getCdmaEriIconIndex() != 1 && (cdmaEriIconMode == 0 || cdmaEriIconMode == 1)) {
                    z = true;
                }
                return z;
            }
        }
        ServiceState serviceState4 = this.mServiceState;
        if (serviceState4 != null && serviceState4.getRoaming()) {
            z = true;
        }
        return z;
    }

    private boolean isCarrierNetworkChangeActive() {
        return ((MobileState) this.mCurrentState).carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getStringExtra("spnData"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
            mayNotifyListeners();
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            updateDataSim();
            mayNotifyListeners();
        } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
            String stringExtra = intent.getStringExtra("apnType");
            String stringExtra2 = intent.getStringExtra("state");
            if ("mms".equals(stringExtra)) {
                if (SignalController.DEBUG) {
                    String str = this.mTag;
                    StringBuilder sb = new StringBuilder();
                    sb.append("handleBroadcast MMS connection state=");
                    sb.append(stringExtra2);
                    Log.d(str, sb.toString());
                }
                this.mMMSDataState = DataState.valueOf(stringExtra2);
                updateTelephony();
            }
        }
    }

    private void updateDataSim() {
        int defaultDataSubId = this.mDefaults.getDefaultDataSubId();
        boolean z = true;
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            MobileState mobileState = (MobileState) this.mCurrentState;
            if (defaultDataSubId != this.mSubscriptionInfo.getSubscriptionId()) {
                z = false;
            }
            mobileState.dataSim = z;
            return;
        }
        ((MobileState) this.mCurrentState).dataSim = true;
    }

    /* access modifiers changed from: private */
    public boolean isCarrierSpecificDataIcon() {
        String[] strArr;
        String str = this.mConfig.patternOfCarrierSpecificDataIcon;
        if (!(str == null || str.length() == 0)) {
            Pattern compile = Pattern.compile(this.mConfig.patternOfCarrierSpecificDataIcon);
            for (String str2 : new String[]{this.mServiceState.getOperatorAlphaLongRaw(), this.mServiceState.getOperatorAlphaShortRaw()}) {
                if (!TextUtils.isEmpty(str2) && compile.matcher(str2).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: 0000 */
    public void updateNetworkName(boolean z, String str, String str2, boolean z2, String str3) {
        if (SignalController.CHATTY) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateNetworkName showSpn=");
            sb.append(z);
            sb.append(" spn=");
            sb.append(str);
            sb.append(" dataSpn=");
            sb.append(str2);
            sb.append(" showPlmn=");
            sb.append(z2);
            sb.append(" plmn=");
            sb.append(str3);
            Log.d("CarrierLabel", sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        if (z2 && str3 != null) {
            sb2.append(str3);
            sb3.append(str3);
        }
        String str4 = ")";
        String str5 = "(";
        if (z && str != null) {
            if (sb2.length() != 0) {
                sb2.append(str5);
                sb2.append(str);
                sb2.append(str4);
            } else {
                sb2.append(str);
            }
            if (sb2.length() != 0) {
                sb2.append(this.mNetworkNameSeparator);
            }
            sb2.append(str);
        }
        if (sb2.length() != 0) {
            ((MobileState) this.mCurrentState).networkName = sb2.toString();
        } else {
            ((MobileState) this.mCurrentState).networkName = this.mNetworkNameDefault;
        }
        if (z && str2 != null) {
            if (sb3.length() != 0) {
                sb3.append(str5);
                sb3.append(str2);
                sb3.append(str4);
            } else {
                sb3.append(str2);
            }
            if (sb3.length() != 0) {
                sb3.append(this.mNetworkNameSeparator);
            }
            sb3.append(str2);
        }
        if (sb3.length() != 0) {
            ((MobileState) this.mCurrentState).networkNameData = sb3.toString();
            return;
        }
        ((MobileState) this.mCurrentState).networkNameData = this.mNetworkNameDefault;
    }

    /* access modifiers changed from: private */
    public final void updateTelephony() {
        if (SignalController.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("updateTelephonySignalStrength: hasService=");
            sb.append(Utils.isInService(this.mServiceState));
            sb.append(" ss=");
            sb.append(this.mSignalStrength);
            Log.d(str, sb.toString());
        }
        boolean z = true;
        int i = 0;
        ((MobileState) this.mCurrentState).connected = Utils.isInService(this.mServiceState) && this.mSignalStrength != null;
        if (((MobileState) this.mCurrentState).connected) {
            if (this.mSignalStrength.isGsm() || !this.mConfig.alwaysShowCdmaRssi) {
                ((MobileState) this.mCurrentState).level = showStacked(this.mDataNetType) ? this.mSignalStrength.getSmoothSignalLevelAll()[1] : this.mSignalStrength.getSmoothSignalLevel();
                if (this.mConfig.showRsrpSignalLevelforLTE) {
                    if (SignalController.DEBUG) {
                        String str2 = this.mTag;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("updateTelephony CS:");
                        sb2.append(this.mServiceState.getVoiceNetworkType());
                        String str3 = "/";
                        sb2.append(str3);
                        sb2.append(TelephonyManager.getNetworkTypeName(this.mServiceState.getVoiceNetworkType()));
                        sb2.append(", PS:");
                        sb2.append(this.mServiceState.getDataNetworkType());
                        sb2.append(str3);
                        sb2.append(TelephonyManager.getNetworkTypeName(this.mServiceState.getDataNetworkType()));
                        Log.d(str2, sb2.toString());
                    }
                    int dataNetworkType = this.mServiceState.getDataNetworkType();
                    if (dataNetworkType == 13 || dataNetworkType == 19) {
                        ((MobileState) this.mCurrentState).level = getAlternateLteLevel(this.mSignalStrength);
                    } else if (dataNetworkType == 0) {
                        int voiceNetworkType = this.mServiceState.getVoiceNetworkType();
                        if (voiceNetworkType == 13 || voiceNetworkType == 19) {
                            ((MobileState) this.mCurrentState).level = getAlternateLteLevel(this.mSignalStrength);
                        }
                    }
                }
            } else {
                ((MobileState) this.mCurrentState).level = this.mSignalStrength.getCdmaLevel();
            }
        }
        if (!OpUtils.isSupportFiveBar()) {
            T t = this.mCurrentState;
            MobileState mobileState = (MobileState) t;
            int i2 = 4;
            if (((MobileState) t).level <= 4) {
                i2 = ((MobileState) t).level;
            }
            mobileState.level = i2;
        }
        MobileIconGroup nr5GIconGroup = getNr5GIconGroup();
        if (nr5GIconGroup != null) {
            ((MobileState) this.mCurrentState).iconGroup = nr5GIconGroup;
        } else if (this.mNetworkToIconLookup.indexOfKey(this.mDataNetType) < 0) {
            ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
        } else if (showStacked(this.mDataNetType)) {
            MobileState mobileState2 = (MobileState) this.mCurrentState;
            MobileIconGroup mobileIconGroup = new MobileIconGroup((MobileIconGroup) this.mNetworkToIconLookup.get(this.mDataNetType), this.mDataNetType, this.mVoiceNetType, getVoiceSignalLevel(), ((MobileState) this.mCurrentState).level, isRoaming(), showLTE());
            mobileState2.iconGroup = mobileIconGroup;
            if (SignalController.DEBUG) {
                String str4 = this.mTag;
                StringBuilder sb3 = new StringBuilder();
                sb3.append(" showStacked dataType:");
                sb3.append(this.mDataNetType);
                sb3.append(" getCurrentPhoneType:");
                sb3.append(TelephonyManager.getDefault().getCurrentPhoneType(this.mSubscriptionInfo.getSubscriptionId()));
                sb3.append(" SubscriptionId:");
                sb3.append(this.mSubscriptionInfo.getSubscriptionId());
                Log.d(str4, sb3.toString());
            }
        } else {
            ((MobileState) this.mCurrentState).iconGroup = (IconGroup) this.mNetworkToIconLookup.get(this.mDataNetType);
        }
        T t2 = this.mCurrentState;
        MobileState mobileState3 = (MobileState) t2;
        if (!((MobileState) t2).connected || !(this.mDataState == 2 || this.mMMSDataState == DataState.CONNECTED)) {
            z = false;
        }
        mobileState3.dataConnected = z;
        boolean isRoaming = isRoaming();
        T t3 = this.mCurrentState;
        if (((MobileState) t3).roaming != isRoaming) {
            ((MobileState) t3).roaming = isRoaming;
            customizeIconsMap();
        }
        if (SignalController.DEBUG) {
            String str5 = this.mTag;
            StringBuilder sb4 = new StringBuilder();
            sb4.append("updateTelephony, isDataDisabled():");
            sb4.append(isDataDisabled());
            Log.d(str5, sb4.toString());
        }
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled() && !this.mConfig.alwaysShowDataRatIcon && !OpUtils.isUSS()) {
            if (this.mSubscriptionInfo.getSubscriptionId() != this.mDefaults.getDefaultDataSubId()) {
                ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.NOT_DEFAULT_DATA;
            } else {
                ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.DATA_DISABLED;
            }
        }
        boolean isEmergencyOnly = isEmergencyOnly();
        T t4 = this.mCurrentState;
        if (isEmergencyOnly != ((MobileState) t4).isEmergency) {
            ((MobileState) t4).isEmergency = isEmergencyOnly();
            this.mNetworkController.recalculateEmergency();
        }
        if (((MobileState) this.mCurrentState).networkName.equals(this.mNetworkNameDefault)) {
            ServiceState serviceState = this.mServiceState;
            if (serviceState != null && !TextUtils.isEmpty(serviceState.getOperatorAlphaShort())) {
                ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
            }
        }
        if (((MobileState) this.mCurrentState).networkNameData.equals(this.mNetworkNameDefault)) {
            ServiceState serviceState2 = this.mServiceState;
            if (serviceState2 != null && ((MobileState) this.mCurrentState).dataSim && !TextUtils.isEmpty(serviceState2.getDataOperatorAlphaShort())) {
                ((MobileState) this.mCurrentState).networkNameData = this.mServiceState.getDataOperatorAlphaShort();
            }
        }
        if (this.mConfig.alwaysShowNetworkTypeIcon && nr5GIconGroup == null) {
            if (((MobileState) this.mCurrentState).connected) {
                if (isDataNetworkTypeAvailable()) {
                    i = this.mDataNetType;
                } else {
                    i = getVoiceNetworkType();
                }
            }
            if (this.mNetworkToIconLookup.indexOfKey(i) >= 0) {
                ((MobileState) this.mCurrentState).iconGroup = (IconGroup) this.mNetworkToIconLookup.get(i);
            } else {
                ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
            }
        }
        mayNotifyListeners();
    }

    private MobileIconGroup getNr5GIconGroup() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState == null) {
            return null;
        }
        int nrState = serviceState.getNrState();
        if (nrState == 3) {
            if (this.mServiceState.getNrFrequencyRange() == 4 && this.mConfig.nr5GIconMap.containsKey(Integer.valueOf(1))) {
                return (MobileIconGroup) this.mConfig.nr5GIconMap.get(Integer.valueOf(1));
            }
            if (this.mConfig.nr5GIconMap.containsKey(Integer.valueOf(2))) {
                return (MobileIconGroup) this.mConfig.nr5GIconMap.get(Integer.valueOf(2));
            }
        } else if (nrState == 2) {
            if (this.mConfig.nr5GIconMap.containsKey(Integer.valueOf(3))) {
                return (MobileIconGroup) this.mConfig.nr5GIconMap.get(Integer.valueOf(3));
            }
        } else if (nrState == 1 && this.mConfig.nr5GIconMap.containsKey(Integer.valueOf(4))) {
            return (MobileIconGroup) this.mConfig.nr5GIconMap.get(Integer.valueOf(4));
        }
        return null;
    }

    private boolean isDataDisabled() {
        return !this.mPhone.getDataEnabled(this.mSubscriptionInfo.getSubscriptionId()) || (OpUtils.isUST() && !Utils.isInService(this.mServiceState));
    }

    private boolean isDataNetworkTypeAvailable() {
        if (this.mDataNetType == 0) {
            return false;
        }
        int dataNetworkType = getDataNetworkType();
        int voiceNetworkType = getVoiceNetworkType();
        if ((dataNetworkType == 6 || dataNetworkType == 12 || dataNetworkType == 14 || dataNetworkType == 13 || dataNetworkType == 19) && ((voiceNetworkType == 16 || voiceNetworkType == 7 || voiceNetworkType == 4) && !isCallIdle())) {
            return false;
        }
        return true;
    }

    private boolean isCallIdle() {
        return this.mCallState == 0;
    }

    private int getVoiceNetworkType() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState != null) {
            return serviceState.getVoiceNetworkType();
        }
        return 0;
    }

    private int getDataNetworkType() {
        ServiceState serviceState = this.mServiceState;
        if (serviceState != null) {
            return serviceState.getDataNetworkType();
        }
        return 0;
    }

    private int getAlternateLteLevel(SignalStrength signalStrength) {
        int lteDbm = signalStrength.getLteDbm();
        if (lteDbm == Integer.MAX_VALUE) {
            int level = signalStrength.getLevel();
            if (SignalController.DEBUG) {
                String str = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("getAlternateLteLevel lteRsrp:INVALID  signalStrengthLevel = ");
                sb.append(level);
                Log.d(str, sb.toString());
            }
            return level;
        }
        int i = 0;
        if (lteDbm <= -44) {
            if (lteDbm >= -97) {
                i = 4;
            } else if (lteDbm >= -105) {
                i = 3;
            } else if (lteDbm >= -113) {
                i = 2;
            } else if (lteDbm >= -120) {
                i = 1;
            }
        }
        if (SignalController.DEBUG) {
            String str2 = this.mTag;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("getAlternateLteLevel lteRsrp:");
            sb2.append(lteDbm);
            sb2.append(" rsrpLevel = ");
            sb2.append(i);
            Log.d(str2, sb2.toString());
        }
        return i;
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setActivity(int i) {
        boolean z = false;
        ((MobileState) this.mCurrentState).activityIn = i == 3 || i == 1;
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (i == 3 || i == 2) {
            z = true;
        }
        mobileState.activityOut = z;
        long currentTimeMillis = System.currentTimeMillis() - this.mLastUpdateActivityTime;
        Message message = new Message();
        message.what = C0526R.styleable.AppCompatTheme_textAppearanceLargePopupMenu;
        this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_textAppearanceLargePopupMenu);
        if (currentTimeMillis < 500) {
            this.mHandler.sendMessageDelayed(message, 500 - currentTimeMillis);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    public void registerFiveGStateListener(FiveGServiceClient fiveGServiceClient) {
        fiveGServiceClient.registerListener(this.mSubscriptionInfo.getSimSlotIndex(), this.mFiveGStateListener);
    }

    public void unregisterFiveGStateListener(FiveGServiceClient fiveGServiceClient) {
        fiveGServiceClient.unregisterListener(this.mSubscriptionInfo.getSimSlotIndex());
    }

    private boolean isDataRegisteredOnLteNr() {
        int dataNetworkType = getDataNetworkType();
        return dataNetworkType == 13 || dataNetworkType == 19 || dataNetworkType == 20;
    }

    private boolean is5GConnected() {
        return this.mFiveGState.isConnectedOnSaMode() || (this.mFiveGState.isConnectedOnNsaMode() && isDataRegisteredOnLteNr());
    }

    public void dump(PrintWriter printWriter) {
        super.dump(printWriter);
        StringBuilder sb = new StringBuilder();
        sb.append("  mSubscription=");
        sb.append(this.mSubscriptionInfo);
        String str = ",";
        sb.append(str);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mServiceState=");
        sb2.append(this.mServiceState);
        sb2.append(str);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mSignalStrength=");
        sb3.append(this.mSignalStrength);
        sb3.append(str);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mDataState=");
        sb4.append(this.mDataState);
        sb4.append(str);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mDataNetType=");
        sb5.append(this.mDataNetType);
        sb5.append(str);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mInflateSignalStrengths=");
        sb6.append(this.mInflateSignalStrengths);
        sb6.append(str);
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("  isDataDisabled=");
        sb7.append(isDataDisabled());
        sb7.append(str);
        printWriter.println(sb7.toString());
        StringBuilder sb8 = new StringBuilder();
        sb8.append("  mFiveGState=");
        sb8.append(this.mFiveGState);
        sb8.append(str);
        printWriter.println(sb8.toString());
    }

    public boolean[] getLTEStatus() {
        return this.mLTEStatus;
    }

    private void cleanLTEStatus() {
        this.mLTEStatus = LTE_DEFAULT_STATUS;
        Log.i(this.mTag, "cleanLTEStatus");
    }

    public int getSimSlotIndex() {
        SubscriptionInfo subscriptionInfo = this.mSubscriptionInfo;
        int simSlotIndex = subscriptionInfo != null ? subscriptionInfo.getSimSlotIndex() : -1;
        if (SignalController.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("getSimSlotIndex, slotId: ");
            sb.append(simSlotIndex);
            Log.d(str, sb.toString());
        }
        return simSlotIndex;
    }

    public void setVirtualSimstate(int[] iArr) {
        int simSlotIndex = getSimSlotIndex();
        if (simSlotIndex < iArr.length && simSlotIndex >= 0) {
            ((MobileState) this.mCurrentState).isVirtual = iArr[simSlotIndex] != NetworkControllerImpl.SOFTSIM_DISABLE;
            notifyListenersIfNecessary();
        }
    }

    private boolean isVirtualSim() {
        return ((MobileState) this.mCurrentState).isVirtual && Utils.isInService(this.mServiceState) && ((MobileState) this.mCurrentState).simstate != "ABSENT";
    }

    private int getVoiceSignalLevel() {
        if (this.mSignalStrength == null) {
            return 0;
        }
        boolean showStacked = showStacked(this.mDataNetType);
        SignalStrength signalStrength = this.mSignalStrength;
        int level = showStacked ? signalStrength.getSmoothSignalLevelAll()[0] : signalStrength.getLevel();
        if (!OpUtils.isSupportFiveBar() && level > 4) {
            level = 4;
        }
        return level;
    }

    /* access modifiers changed from: private */
    public void onPhoneStateChange(String str) {
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(str)) {
            this.mPhoneState = 0;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(str)) {
            this.mPhoneState = 2;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(str)) {
            this.mPhoneState = 1;
        }
        if (SignalController.DEBUG) {
            String str2 = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("handlePhoneStateChanged(");
            sb.append(str);
            sb.append(")");
            Log.d(str2, sb.toString());
        }
        if (this.mPhoneState != 0 && this.mIsRemainCa) {
            this.mHandler.removeMessages(100);
            recoverDataNetTypeStable();
        }
    }

    /* access modifiers changed from: private */
    public void recoverDataNetTypeStable() {
        this.mIsRemainCa = false;
        if (this.mBackupDataNetType != 19) {
            StringBuilder sb = new StringBuilder();
            sb.append("Restore mDataNetType to mBackupDataNetType:");
            sb.append(TelephonyManager.getNetworkTypeName(this.mBackupDataNetType));
            Log.d("CADebug", sb.toString());
            this.mDataNetType = this.mBackupDataNetType;
            updateTelephony();
        }
    }

    /* access modifiers changed from: private */
    public void makeDataNetTypeStable() {
        int i = this.mDataNetType;
        this.mBackupDataNetType = i;
        String str = "CADebug";
        if (i == 19) {
            Log.d(str, "Starting to make CA stable");
            this.mIsRemainCa = true;
            this.mHandler.removeMessages(100);
            this.mHandler.sendEmptyMessageDelayed(100, 10000);
        } else if ((i != 19 && i != 13) || this.mPhoneState != 0) {
            this.mHandler.removeMessages(100);
            this.mIsRemainCa = false;
        } else if (this.mIsRemainCa) {
            Log.d(str, "mDataNetType changed, force it to display CA");
            this.mDataNetType = 19;
        }
    }

    public int getDefaultDataSubId() {
        int defaultDataSubId = this.mDefaults.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            return Integer.MAX_VALUE;
        }
        return defaultDataSubId;
    }

    public int getSubId() {
        SubscriptionInfo subscriptionInfo = this.mSubscriptionInfo;
        int subscriptionId = subscriptionInfo != null ? subscriptionInfo.getSubscriptionId() : -1;
        if (SignalController.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("getSubId, subId: ");
            sb.append(subscriptionId);
            Log.d(str, sb.toString());
        }
        return subscriptionId;
    }

    public boolean showLTE() {
        if (this.mPhone == null) {
            return false;
        }
        String str = this.mMccmnc;
        int i = 0;
        while (true) {
            String[] strArr = SHOW_LTE_OPERATORS;
            if (i >= strArr.length) {
                return false;
            }
            if (str.equals(strArr[i])) {
                return true;
            }
            i++;
        }
    }

    private boolean isBouygues() {
        if (this.mPhone != null && TextUtils.equals(this.mMccmnc, "20820")) {
            return true;
        }
        return false;
    }

    private boolean isEESim() {
        if (this.mPhone == null) {
            return false;
        }
        String str = this.mMccmnc;
        if (TextUtils.equals(str, "23430") || TextUtils.equals(str, "23433")) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void customizeIconsMap() {
        TelephonyManager telephonyManager = this.mPhone;
        if (telephonyManager != null) {
            this.mMccmnc = telephonyManager.getSimOperatorNumericForPhone(getSimSlotIndex());
            boolean z = false;
            boolean z2 = OpUtils.isUST() || OpUtils.isUSS();
            if (showLTE() || isBouygues() || isEESim()) {
                z = true;
            }
            mapIconSets();
            if (z) {
                if (showLTE()) {
                    this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
                    this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE_PLUS);
                } else if (isBouygues()) {
                    this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(1, TelephonyIcons.f101G);
                    this.mNetworkToIconLookup.put(2, TelephonyIcons.f100E);
                    this.mNetworkToIconLookup.put(9, TelephonyIcons.THREE_G_PLUS);
                    this.mNetworkToIconLookup.put(8, TelephonyIcons.THREE_G_PLUS);
                    this.mNetworkToIconLookup.put(10, TelephonyIcons.H_PLUS);
                    this.mNetworkToIconLookup.put(15, TelephonyIcons.H_PLUS);
                    this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
                    this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_PLUS);
                } else if (isEESim()) {
                    this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G);
                }
            }
            if (z2) {
                if (OpUtils.isUST()) {
                    if (((MobileState) this.mCurrentState).roaming) {
                        this.mNetworkToIconLookup.put(15, TelephonyIcons.THREE_G);
                    } else {
                        this.mNetworkToIconLookup.put(15, TelephonyIcons.FOUR_G);
                    }
                    this.mNetworkToIconLookup.put(10, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(1, TelephonyIcons.f101G);
                    this.mNetworkToIconLookup.put(2, TelephonyIcons.TWO_G);
                }
                if (OpUtils.isSupportShow4GLTE()) {
                    this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G_LTE);
                    this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_LTE);
                }
                if (OpUtils.isUSS()) {
                    this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(17, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
                    this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
                    this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE);
                }
            }
        }
    }

    private boolean showStacked(int i) {
        return i != 0 && isCdma() && !isDataDisabled() && !OpUtils.isUSS();
    }
}
