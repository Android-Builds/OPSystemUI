package com.android.systemui.statusbar.policy;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Global;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.net.DataUsageController.Callback;
import com.android.settingslib.net.DataUsageController.NetworkNameProvider;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R$bool;
import com.android.systemui.R$string;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.oneplus.networkspeed.NetworkSpeedController;
import com.oneplus.signal.OpSignalIcons;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;

public class NetworkControllerImpl extends BroadcastReceiver implements NetworkController, DemoMode, NetworkNameProvider, ConfigurationChangedReceiver, Dumpable {
    static final boolean CHATTY;
    static final boolean DEBUG;
    public static int SOFTSIM_DISABLE = 0;
    public static int SOFTSIM_ENABLE = 1;
    public static int SOFTSIM_ENABLE_PILOT = 2;
    private static final Uri SOFTSIM_URL = Uri.parse("content://com.redteamobile.provider");
    private final AccessPointControllerImpl mAccessPoints;
    /* access modifiers changed from: private */
    public int mActiveMobileDataSubscription;
    private boolean mAirplaneMode;
    /* access modifiers changed from: private */
    public final CallbackHandler mCallbackHandler;
    private Config mConfig;
    private final BitSet mConnectedTransports;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private List<SubscriptionInfo> mCurrentSubscriptions;
    private int mCurrentUserId;
    private final DataSaverController mDataSaverController;
    private final DataUsageController mDataUsageController;
    private MobileSignalController mDefaultSignalController;
    private boolean mDemoInetCondition;
    private boolean mDemoMode;
    private WifiState mDemoWifiState;
    private int mEmergencySource;
    @VisibleForTesting
    final EthernetSignalController mEthernetSignalController;
    private IExtTelephony mExtTelephony;
    @VisibleForTesting
    FiveGServiceClient mFiveGServiceClient;
    private final boolean mHasMobileDataFeature;
    private boolean mHasNoSubs;
    private final HotspotController mHotspotController;
    private boolean mInetCondition;
    private boolean mIsEmergency;
    private IconState[] mLTEIconStates;
    private boolean[] mLTEstatus;
    @VisibleForTesting
    ServiceState mLastServiceState;
    @VisibleForTesting
    boolean mListening;
    private Locale mLocale;
    private final Object mLock;
    @VisibleForTesting
    final SparseArray<MobileSignalController> mMobileSignalControllers;
    private NetworkSpeedController mNetworkSpeedController;
    /* access modifiers changed from: private */
    public boolean mOpActionTetherDialogShowing;
    private final TelephonyManager mPhone;
    private PhoneStateListener mPhoneStateListener;
    private int[] mProvisionState;
    private final Handler mReceiverHandler;
    private final Runnable mRegisterListeners;
    private boolean mSimDetected;
    private final SubscriptionDefaults mSubDefaults;
    private OnSubscriptionsChangedListener mSubscriptionListener;
    private final SubscriptionManager mSubscriptionManager;
    private boolean mUserSetup;
    private final CurrentUserTracker mUserTracker;
    private final BitSet mValidatedTransports;
    /* access modifiers changed from: private */
    public final WifiManager mWifiManager;
    @VisibleForTesting
    final WifiSignalController mWifiSignalController;
    private int[] softSimState;

    @VisibleForTesting
    static class Config {
        private static final Map<String, Integer> NR_STATUS_STRING_TO_INDEX = new HashMap(4);
        boolean alwaysShowCdmaRssi = false;
        boolean alwaysShowDataRatIcon = false;
        boolean alwaysShowNetworkTypeIcon = false;
        boolean hideLtePlus = false;
        boolean hideNoInternetState = false;
        boolean hspaDataDistinguishable;
        boolean inflateSignalStrengths = false;
        Map<Integer, MobileIconGroup> nr5GIconMap = new HashMap();
        public String patternOfCarrierSpecificDataIcon = "";
        boolean readIconsFromXml;
        boolean show4gForLte = false;
        boolean showAtLeast3G = false;
        boolean showRsrpSignalLevelforLTE = false;
        boolean showVolteIcon = false;

        Config() {
        }

        static {
            NR_STATUS_STRING_TO_INDEX.put("connected_mmwave", Integer.valueOf(1));
            NR_STATUS_STRING_TO_INDEX.put("connected", Integer.valueOf(2));
            NR_STATUS_STRING_TO_INDEX.put("not_restricted", Integer.valueOf(3));
            NR_STATUS_STRING_TO_INDEX.put("restricted", Integer.valueOf(4));
        }

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources resources = context.getResources();
            config.showAtLeast3G = resources.getBoolean(R$bool.config_showMin3G);
            config.alwaysShowCdmaRssi = resources.getBoolean(17891358);
            config.hspaDataDistinguishable = resources.getBoolean(R$bool.config_hspa_data_distinguishable);
            config.inflateSignalStrengths = false;
            config.alwaysShowNetworkTypeIcon = context.getResources().getBoolean(R$bool.config_alwaysShowTypeIcon);
            config.showRsrpSignalLevelforLTE = resources.getBoolean(R$bool.config_showRsrpSignalLevelforLTE);
            config.hideNoInternetState = resources.getBoolean(R$bool.config_hideNoInternetState);
            config.showVolteIcon = resources.getBoolean(R$bool.config_display_volte);
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
            SubscriptionManager.from(context);
            PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(SubscriptionManager.getDefaultDataSubscriptionId());
            if (configForSubId != null) {
                config.alwaysShowDataRatIcon = configForSubId.getBoolean("always_show_data_rat_icon_bool");
                config.show4gForLte = configForSubId.getBoolean("show_4g_for_lte_data_icon_bool");
                config.hideLtePlus = configForSubId.getBoolean("hide_lte_plus_data_icon_bool");
                config.patternOfCarrierSpecificDataIcon = configForSubId.getString("show_carrier_data_icon_pattern_string");
                String string = configForSubId.getString("5g_icon_configuration_string");
                if (!TextUtils.isEmpty(string)) {
                    for (String add5GIconMapping : string.trim().split(",")) {
                        add5GIconMapping(add5GIconMapping, config);
                    }
                }
            }
            config.readIconsFromXml = resources.getBoolean(R$bool.config_read_icons_from_xml);
            config.showRsrpSignalLevelforLTE = resources.getBoolean(R$bool.config_showRsrpSignalLevelforLTE);
            config.showVolteIcon = resources.getBoolean(R$bool.config_display_volte);
            return config;
        }

        @VisibleForTesting
        static void add5GIconMapping(String str, Config config) {
            String[] split = str.trim().toLowerCase().split(":");
            if (split.length != 2) {
                if (NetworkControllerImpl.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid 5G icon configuration, config = ");
                    sb.append(str);
                    Log.e("NetworkController", sb.toString());
                }
                return;
            }
            String str2 = split[0];
            String str3 = split[1];
            if (!str3.equals("none") && NR_STATUS_STRING_TO_INDEX.containsKey(str2) && TelephonyIcons.ICON_NAME_TO_ICON.containsKey(str3)) {
                config.nr5GIconMap.put((Integer) NR_STATUS_STRING_TO_INDEX.get(str2), (MobileIconGroup) TelephonyIcons.ICON_NAME_TO_ICON.get(str3));
            }
        }
    }

    private class SubListener extends OnSubscriptionsChangedListener {
        private SubListener() {
        }

        public void onSubscriptionsChanged() {
            NetworkControllerImpl.this.updateMobileControllers();
        }
    }

    public static class SubscriptionDefaults {
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        public int getDefaultDataSubId() {
            return SubscriptionManager.getDefaultDataSubscriptionId();
        }
    }

    static {
        boolean z = Build.DEBUG_ONEPLUS;
        DEBUG = z;
        CHATTY = z;
    }

    public NetworkControllerImpl(Context context, Looper looper, DeviceProvisionedController deviceProvisionedController) {
        this(context, (ConnectivityManager) context.getSystemService("connectivity"), (TelephonyManager) context.getSystemService("phone"), (WifiManager) context.getSystemService("wifi"), SubscriptionManager.from(context), Config.readConfig(context), looper, new CallbackHandler(), new AccessPointControllerImpl(context), new DataUsageController(context), new SubscriptionDefaults(), deviceProvisionedController);
        this.mReceiverHandler.post(this.mRegisterListeners);
    }

    @VisibleForTesting
    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager, TelephonyManager telephonyManager, WifiManager wifiManager, SubscriptionManager subscriptionManager, Config config, Looper looper, CallbackHandler callbackHandler, AccessPointControllerImpl accessPointControllerImpl, DataUsageController dataUsageController, SubscriptionDefaults subscriptionDefaults, DeviceProvisionedController deviceProvisionedController) {
        Context context2 = context;
        Looper looper2 = looper;
        final DeviceProvisionedController deviceProvisionedController2 = deviceProvisionedController;
        this.mLock = new Object();
        this.mActiveMobileDataSubscription = -1;
        this.mMobileSignalControllers = new SparseArray<>();
        this.mConnectedTransports = new BitSet();
        this.mValidatedTransports = new BitSet();
        this.mAirplaneMode = false;
        this.mLocale = null;
        this.mCurrentSubscriptions = new ArrayList();
        this.mOpActionTetherDialogShowing = false;
        this.mRegisterListeners = new Runnable() {
            public void run() {
                NetworkControllerImpl.this.registerListeners();
            }
        };
        this.mLTEstatus = new boolean[]{false, false, false, false, false, false};
        this.mLTEIconStates = new IconState[this.mLTEstatus.length];
        this.mProvisionState = new int[2];
        this.softSimState = new int[2];
        this.mContext = context2;
        this.mConfig = config;
        this.mReceiverHandler = new Handler(looper2);
        this.mCallbackHandler = callbackHandler;
        this.mDataSaverController = new DataSaverControllerImpl(context);
        this.mSubscriptionManager = subscriptionManager;
        this.mSubDefaults = subscriptionDefaults;
        this.mConnectivityManager = connectivityManager;
        this.mHasMobileDataFeature = this.mConnectivityManager.isNetworkSupported(0);
        this.mPhone = telephonyManager;
        this.mWifiManager = wifiManager;
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mAccessPoints = accessPointControllerImpl;
        this.mDataUsageController = dataUsageController;
        this.mDataUsageController.setNetworkController(this);
        this.mDataUsageController.setCallback(new Callback() {
            public void onMobileDataEnabled(boolean z) {
                NetworkControllerImpl.this.mCallbackHandler.setMobileDataEnabled(z);
            }
        });
        WifiSignalController wifiSignalController = new WifiSignalController(this.mContext, this.mHasMobileDataFeature, this.mCallbackHandler, this, this.mWifiManager);
        this.mWifiSignalController = wifiSignalController;
        this.mEthernetSignalController = new EthernetSignalController(this.mContext, this.mCallbackHandler, this);
        updateAirplaneMode(true);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int i) {
                NetworkControllerImpl.this.onUserSwitched(i);
            }
        };
        this.mUserTracker.startTracking();
        deviceProvisionedController2.addCallback(new DeviceProvisionedListener() {
            public void onUserSetupChanged() {
                NetworkControllerImpl networkControllerImpl = NetworkControllerImpl.this;
                DeviceProvisionedController deviceProvisionedController = deviceProvisionedController2;
                networkControllerImpl.setUserSetupComplete(deviceProvisionedController.isUserSetup(deviceProvisionedController.getCurrentUser()));
            }
        });
        this.mFiveGServiceClient = FiveGServiceClient.getInstance(context);
        this.mConnectivityManager.registerDefaultNetworkCallback(new NetworkCallback() {
            private Network mLastNetwork;
            private NetworkCapabilities mLastNetworkCapabilities;

            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                NetworkCapabilities networkCapabilities2 = this.mLastNetworkCapabilities;
                boolean z = networkCapabilities2 != null && networkCapabilities2.hasCapability(16);
                boolean hasCapability = networkCapabilities.hasCapability(16);
                if (!network.equals(this.mLastNetwork) || !networkCapabilities.equalsTransportTypes(this.mLastNetworkCapabilities) || hasCapability != z) {
                    this.mLastNetwork = network;
                    this.mLastNetworkCapabilities = networkCapabilities;
                    NetworkControllerImpl.this.updateConnectivity();
                }
            }
        }, this.mReceiverHandler);
        this.mPhoneStateListener = new PhoneStateListener(looper2) {
            public void onActiveDataSubscriptionIdChanged(int i) {
                NetworkControllerImpl.this.mActiveMobileDataSubscription = i;
                NetworkControllerImpl.this.doUpdateMobileControllers();
            }
        };
        initProvistionState();
        this.mHotspotController = (HotspotController) Dependency.get(HotspotController.class);
    }

    public DataSaverController getDataSaverController() {
        return this.mDataSaverController;
    }

    /* access modifiers changed from: private */
    public void registerListeners() {
        boolean z = false;
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            MobileSignalController mobileSignalController = (MobileSignalController) this.mMobileSignalControllers.valueAt(i);
            mobileSignalController.registerListener();
            mobileSignalController.registerFiveGStateListener(this.mFiveGServiceClient);
        }
        if (this.mSubscriptionListener == null) {
            this.mSubscriptionListener = new SubListener();
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mPhone.listen(this.mPhoneStateListener, 4194304);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
        intentFilter.addAction("android.intent.action.setupDataError_tether");
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            z = true;
        }
        if (z) {
            intentFilter.addAction("org.codeaurora.intent.action.ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED");
        }
        this.mContext.registerReceiver(this, intentFilter, null, this.mReceiverHandler);
        this.mListening = true;
        updateMobileControllers();
    }

    private void unregisterListeners() {
        this.mListening = false;
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            MobileSignalController mobileSignalController = (MobileSignalController) this.mMobileSignalControllers.valueAt(i);
            mobileSignalController.unregisterListener();
            mobileSignalController.unregisterFiveGStateListener(this.mFiveGServiceClient);
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mContext.unregisterReceiver(this);
    }

    public AccessPointController getAccessPointController() {
        return this.mAccessPoints;
    }

    public DataUsageController getMobileDataController() {
        return this.mDataUsageController;
    }

    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return this.mPhone.getPhoneType() != 0;
    }

    private MobileSignalController getDataController() {
        int defaultDataSubId = this.mSubDefaults.getDefaultDataSubId();
        String str = "NetworkController";
        if (!SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            if (DEBUG) {
                Log.e(str, "No data sim selected");
            }
            return this.mDefaultSignalController;
        } else if (this.mMobileSignalControllers.indexOfKey(defaultDataSubId) >= 0) {
            return (MobileSignalController) this.mMobileSignalControllers.get(defaultDataSubId);
        } else {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Cannot find controller for data sub: ");
                sb.append(defaultDataSubId);
                Log.e(str, sb.toString());
            }
            return this.mDefaultSignalController;
        }
    }

    public String getMobileDataNetworkName() {
        MobileSignalController dataController = getDataController();
        return dataController != null ? ((MobileState) dataController.getState()).networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        boolean z = true;
        if (this.mMobileSignalControllers.size() == 0) {
            this.mEmergencySource = 0;
            ServiceState serviceState = this.mLastServiceState;
            if (serviceState == null || !serviceState.isEmergencyOnly()) {
                z = false;
            }
            return z;
        }
        int defaultVoiceSubId = this.mSubDefaults.getDefaultVoiceSubId();
        String str = "NetworkController";
        if (!SubscriptionManager.isValidSubscriptionId(defaultVoiceSubId)) {
            for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
                MobileSignalController mobileSignalController = (MobileSignalController) this.mMobileSignalControllers.valueAt(i);
                if (!((MobileState) mobileSignalController.getState()).isEmergency) {
                    this.mEmergencySource = mobileSignalController.mSubscriptionInfo.getSubscriptionId() + 100;
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Found emergency ");
                        sb.append(mobileSignalController.mTag);
                        Log.d(str, sb.toString());
                    }
                    return false;
                }
            }
        }
        if (this.mMobileSignalControllers.indexOfKey(defaultVoiceSubId) >= 0) {
            this.mEmergencySource = defaultVoiceSubId + 200;
            if (DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Getting emergency from ");
                sb2.append(defaultVoiceSubId);
                Log.d(str, sb2.toString());
            }
            return ((MobileState) ((MobileSignalController) this.mMobileSignalControllers.get(defaultVoiceSubId)).getState()).isEmergency;
        } else if (this.mMobileSignalControllers.size() == 1) {
            this.mEmergencySource = this.mMobileSignalControllers.keyAt(0) + 400;
            if (DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Getting assumed emergency from ");
                sb3.append(this.mMobileSignalControllers.keyAt(0));
                Log.d(str, sb3.toString());
            }
            return ((MobileState) ((MobileSignalController) this.mMobileSignalControllers.valueAt(0)).getState()).isEmergency;
        } else {
            if (DEBUG) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Cannot find controller for voice sub: ");
                sb4.append(defaultVoiceSubId);
                Log.e(str, sb4.toString());
            }
            this.mEmergencySource = defaultVoiceSubId + 300;
            return true;
        }
    }

    /* access modifiers changed from: 0000 */
    public void recalculateEmergency() {
        this.mIsEmergency = isEmergencyOnly();
        this.mCallbackHandler.setEmergencyCallsOnly(this.mIsEmergency);
    }

    public void addCallback(SignalCallback signalCallback) {
        signalCallback.setSubs(this.mCurrentSubscriptions);
        signalCallback.setIsAirplaneMode(new IconState(this.mAirplaneMode, TelephonyIcons.FLIGHT_MODE_ICON, R$string.accessibility_airplane_mode, this.mContext));
        signalCallback.setNoSims(this.mHasNoSubs, this.mSimDetected);
        this.mWifiSignalController.notifyListeners(signalCallback);
        this.mEthernetSignalController.notifyListeners(signalCallback);
        int i = 0;
        for (int i2 = 0; i2 < this.mMobileSignalControllers.size(); i2++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i2)).notifyListeners(signalCallback);
        }
        this.mCallbackHandler.setListening(signalCallback, true);
        signalCallback.setLTEStatus(this.mLTEIconStates);
        while (true) {
            int[] iArr = this.mProvisionState;
            if (i < iArr.length) {
                signalCallback.setProvision(i, iArr[i]);
                i++;
            } else {
                signalCallback.setVirtualSimstate(this.softSimState);
                signalCallback.setHasAnySimReady(hasSimReady());
                return;
            }
        }
    }

    public void removeCallback(SignalCallback signalCallback) {
        this.mCallbackHandler.setListening(signalCallback, false);
    }

    public void setWifiEnabled(final boolean z) {
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... voidArr) {
                NetworkControllerImpl.this.mWifiManager.setWifiEnabled(z);
                return null;
            }
        }.execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    public void onUserSwitched(int i) {
        this.mCurrentUserId = i;
        this.mAccessPoints.onUserSwitched(i);
        updateConnectivity();
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onReceive(android.content.Context r5, android.content.Intent r6) {
        /*
            r4 = this;
            boolean r5 = CHATTY
            java.lang.String r0 = "NetworkController"
            if (r5 == 0) goto L_0x001a
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r1 = "onReceive: intent="
            r5.append(r1)
            r5.append(r6)
            java.lang.String r5 = r5.toString()
            android.util.Log.d(r0, r5)
        L_0x001a:
            java.lang.String r5 = r6.getAction()
            int r1 = r5.hashCode()
            r2 = -1
            r3 = 0
            switch(r1) {
                case -2104353374: goto L_0x0085;
                case -1465084191: goto L_0x007b;
                case -1172645946: goto L_0x0071;
                case -1138588223: goto L_0x0067;
                case -1076576821: goto L_0x005d;
                case -229777127: goto L_0x0053;
                case -25388475: goto L_0x0049;
                case 623179603: goto L_0x003f;
                case 1267145707: goto L_0x0034;
                case 2052218635: goto L_0x0029;
                default: goto L_0x0027;
            }
        L_0x0027:
            goto L_0x008f
        L_0x0029:
            java.lang.String r1 = "org.codeaurora.intent.action.ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 8
            goto L_0x0090
        L_0x0034:
            java.lang.String r1 = "android.intent.action.setupDataError_tether"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 9
            goto L_0x0090
        L_0x003f:
            java.lang.String r1 = "android.net.conn.INET_CONDITION_ACTION"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 1
            goto L_0x0090
        L_0x0049:
            java.lang.String r1 = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 4
            goto L_0x0090
        L_0x0053:
            java.lang.String r1 = "android.intent.action.SIM_STATE_CHANGED"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 5
            goto L_0x0090
        L_0x005d:
            java.lang.String r1 = "android.intent.action.AIRPLANE_MODE"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 2
            goto L_0x0090
        L_0x0067:
            java.lang.String r1 = "android.telephony.action.CARRIER_CONFIG_CHANGED"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 7
            goto L_0x0090
        L_0x0071:
            java.lang.String r1 = "android.net.conn.CONNECTIVITY_CHANGE"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = r3
            goto L_0x0090
        L_0x007b:
            java.lang.String r1 = "android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 3
            goto L_0x0090
        L_0x0085:
            java.lang.String r1 = "android.intent.action.SERVICE_STATE"
            boolean r5 = r5.equals(r1)
            if (r5 == 0) goto L_0x008f
            r5 = 6
            goto L_0x0090
        L_0x008f:
            r5 = r2
        L_0x0090:
            switch(r5) {
                case 0: goto L_0x01a1;
                case 1: goto L_0x01a1;
                case 2: goto L_0x019a;
                case 3: goto L_0x0196;
                case 4: goto L_0x016d;
                case 5: goto L_0x013b;
                case 6: goto L_0x0124;
                case 7: goto L_0x0110;
                case 8: goto L_0x00e3;
                case 9: goto L_0x00b5;
                default: goto L_0x0093;
            }
        L_0x0093:
            java.lang.String r5 = "subscription"
            int r5 = r6.getIntExtra(r5, r2)
            boolean r0 = android.telephony.SubscriptionManager.isValidSubscriptionId(r5)
            if (r0 == 0) goto L_0x01a9
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r0 = r4.mMobileSignalControllers
            int r0 = r0.indexOfKey(r5)
            if (r0 < 0) goto L_0x01a5
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r4 = r4.mMobileSignalControllers
            java.lang.Object r4 = r4.get(r5)
            com.android.systemui.statusbar.policy.MobileSignalController r4 = (com.android.systemui.statusbar.policy.MobileSignalController) r4
            r4.handleBroadcast(r6)
            goto L_0x01ae
        L_0x00b5:
            boolean r5 = com.oneplus.util.OpUtils.isUSS()
            if (r5 == 0) goto L_0x01ae
            boolean r5 = DEBUG
            if (r5 == 0) goto L_0x00c4
            java.lang.String r5 = "TetherError callback"
            android.util.Log.i(r0, r5)
        L_0x00c4:
            com.android.systemui.statusbar.policy.HotspotController r5 = r4.mHotspotController
            boolean r5 = r5.isHotspotEnabled()
            if (r5 == 0) goto L_0x01ae
            java.lang.String r5 = "data_call_error"
            boolean r5 = r6.getBooleanExtra(r5, r3)
            if (r5 == 0) goto L_0x01ae
            java.lang.String r5 = "setTetherError "
            android.util.Log.i(r0, r5)
            com.android.systemui.statusbar.policy.HotspotController r5 = r4.mHotspotController
            r5.setHotspotEnabled(r3)
            r4.opActionTetherErrorAlertDialog()
            goto L_0x01ae
        L_0x00e3:
            java.lang.String r5 = "phone"
            int r5 = r6.getIntExtra(r5, r2)
            java.lang.String r1 = "newProvisionState"
            int r6 = r6.getIntExtra(r1, r3)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "onProvisionChange provisionedState: "
            r1.append(r2)
            r1.append(r6)
            java.lang.String r2 = " slotId: "
            r1.append(r2)
            r1.append(r5)
            java.lang.String r1 = r1.toString()
            android.util.Log.v(r0, r1)
            r4.onProvisionChange(r5, r6)
            goto L_0x01ae
        L_0x0110:
            android.content.Context r5 = r4.mContext
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r5 = com.android.systemui.statusbar.policy.NetworkControllerImpl.Config.readConfig(r5)
            r4.mConfig = r5
            android.os.Handler r5 = r4.mReceiverHandler
            com.android.systemui.statusbar.policy.-$$Lambda$ybM43k5QVX_SxWbQACu1XwL3Knk r6 = new com.android.systemui.statusbar.policy.-$$Lambda$ybM43k5QVX_SxWbQACu1XwL3Knk
            r6.<init>()
            r5.post(r6)
            goto L_0x01ae
        L_0x0124:
            android.os.Bundle r5 = r6.getExtras()
            android.telephony.ServiceState r5 = android.telephony.ServiceState.newFromBundle(r5)
            r4.mLastServiceState = r5
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r5 = r4.mMobileSignalControllers
            int r5 = r5.size()
            if (r5 != 0) goto L_0x01ae
            r4.recalculateEmergency()
            goto L_0x01ae
        L_0x013b:
            java.lang.String r5 = "rebroadcastOnUnlock"
            boolean r5 = r6.getBooleanExtra(r5, r3)
            if (r5 == 0) goto L_0x0144
            goto L_0x01ae
        L_0x0144:
            r4.updateMobileControllers()
            java.lang.String r5 = "ss"
            java.lang.String r5 = r6.getStringExtra(r5)
            java.lang.String r6 = "ABSENT"
            boolean r6 = r6.equals(r5)
            if (r6 != 0) goto L_0x015d
            java.lang.String r6 = "READY"
            boolean r5 = r6.equals(r5)
            if (r5 == 0) goto L_0x0160
        L_0x015d:
            r4.initProvistionState()
        L_0x0160:
            r4.checkVirtualSimcard()
            com.android.systemui.statusbar.policy.CallbackHandler r5 = r4.mCallbackHandler
            boolean r4 = r4.hasSimReady()
            r5.setHasAnySimReady(r4)
            goto L_0x01ae
        L_0x016d:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r5 = r4.mMobileSignalControllers
            int r5 = r5.size()
            if (r3 >= r5) goto L_0x0183
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r5 = r4.mMobileSignalControllers
            java.lang.Object r5 = r5.valueAt(r3)
            com.android.systemui.statusbar.policy.MobileSignalController r5 = (com.android.systemui.statusbar.policy.MobileSignalController) r5
            r5.handleBroadcast(r6)
            int r3 = r3 + 1
            goto L_0x016d
        L_0x0183:
            android.content.Context r5 = r4.mContext
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r5 = com.android.systemui.statusbar.policy.NetworkControllerImpl.Config.readConfig(r5)
            r4.mConfig = r5
            android.os.Handler r5 = r4.mReceiverHandler
            com.android.systemui.statusbar.policy.-$$Lambda$ybM43k5QVX_SxWbQACu1XwL3Knk r6 = new com.android.systemui.statusbar.policy.-$$Lambda$ybM43k5QVX_SxWbQACu1XwL3Knk
            r6.<init>()
            r5.post(r6)
            goto L_0x01ae
        L_0x0196:
            r4.recalculateEmergency()
            goto L_0x01ae
        L_0x019a:
            r4.refreshLocale()
            r4.updateAirplaneMode(r3)
            goto L_0x01ae
        L_0x01a1:
            r4.updateConnectivity()
            goto L_0x01ae
        L_0x01a5:
            r4.updateMobileControllers()
            goto L_0x01ae
        L_0x01a9:
            com.android.systemui.statusbar.policy.WifiSignalController r4 = r4.mWifiSignalController
            r4.handleBroadcast(r6)
        L_0x01ae:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.NetworkControllerImpl.onReceive(android.content.Context, android.content.Intent):void");
    }

    public void onConfigurationChanged(Configuration configuration) {
        this.mConfig = Config.readConfig(this.mContext);
        this.mReceiverHandler.post(new Runnable() {
            public void run() {
                NetworkControllerImpl.this.handleConfigurationChanged();
            }
        });
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void handleConfigurationChanged() {
        updateMobileControllers();
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).setConfiguration(this.mConfig);
        }
        refreshLocale();
    }

    /* access modifiers changed from: private */
    public void updateMobileControllers() {
        if (this.mListening) {
            doUpdateMobileControllers();
        }
    }

    private void filterMobileSubscriptionInSameGroup(List<SubscriptionInfo> list) {
        if (list.size() == 2) {
            SubscriptionInfo subscriptionInfo = (SubscriptionInfo) list.get(0);
            SubscriptionInfo subscriptionInfo2 = (SubscriptionInfo) list.get(1);
            if (subscriptionInfo.getGroupUuid() != null && subscriptionInfo.getGroupUuid().equals(subscriptionInfo2.getGroupUuid()) && (subscriptionInfo.isOpportunistic() || subscriptionInfo2.isOpportunistic())) {
                if (CarrierConfigManager.getDefaultConfig().getBoolean("always_show_primary_signal_bar_in_opportunistic_network_boolean")) {
                    if (!subscriptionInfo.isOpportunistic()) {
                        subscriptionInfo = subscriptionInfo2;
                    }
                    list.remove(subscriptionInfo);
                } else {
                    if (subscriptionInfo.getSubscriptionId() == this.mActiveMobileDataSubscription) {
                        subscriptionInfo = subscriptionInfo2;
                    }
                    list.remove(subscriptionInfo);
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void doUpdateMobileControllers() {
        List activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList(false);
        if (activeSubscriptionInfoList == null) {
            activeSubscriptionInfoList = Collections.emptyList();
        }
        filterMobileSubscriptionInSameGroup(activeSubscriptionInfoList);
        if (hasCorrectMobileControllers(activeSubscriptionInfoList)) {
            updateNoSims();
            return;
        }
        synchronized (this.mLock) {
            setCurrentSubscriptionsLocked(activeSubscriptionInfoList);
        }
        updateNoSims();
        recalculateEmergency();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void updateNoSims() {
        boolean z = this.mHasMobileDataFeature && this.mMobileSignalControllers.size() == 0;
        boolean hasAnySim = hasAnySim();
        if (z != this.mHasNoSubs || hasAnySim != this.mSimDetected) {
            this.mHasNoSubs = z;
            this.mSimDetected = hasAnySim;
            this.mCallbackHandler.setNoSims(this.mHasNoSubs, this.mSimDetected);
        }
    }

    private boolean hasAnySim() {
        int simCount = this.mPhone.getSimCount();
        for (int i = 0; i < simCount; i++) {
            int simState = this.mPhone.getSimState(i);
            if (simState != 1 && simState != 0) {
                return true;
            }
        }
        return false;
    }

    @GuardedBy({"mLock"})
    @VisibleForTesting
    public void setCurrentSubscriptionsLocked(List<SubscriptionInfo> list) {
        int i;
        List<SubscriptionInfo> list2 = list;
        Collections.sort(list2, new Comparator<SubscriptionInfo>() {
            public int compare(SubscriptionInfo subscriptionInfo, SubscriptionInfo subscriptionInfo2) {
                int i;
                int i2;
                if (subscriptionInfo.getSimSlotIndex() == subscriptionInfo2.getSimSlotIndex()) {
                    i2 = subscriptionInfo.getSubscriptionId();
                    i = subscriptionInfo2.getSubscriptionId();
                } else {
                    i2 = subscriptionInfo.getSimSlotIndex();
                    i = subscriptionInfo2.getSimSlotIndex();
                }
                return i2 - i;
            }
        });
        this.mCurrentSubscriptions = list2;
        SparseArray sparseArray = new SparseArray();
        for (int i2 = 0; i2 < this.mMobileSignalControllers.size(); i2++) {
            sparseArray.put(this.mMobileSignalControllers.keyAt(i2), (MobileSignalController) this.mMobileSignalControllers.valueAt(i2));
        }
        this.mMobileSignalControllers.clear();
        int size = list.size();
        int i3 = 0;
        while (i3 < size) {
            int subscriptionId = ((SubscriptionInfo) list2.get(i3)).getSubscriptionId();
            if (sparseArray.indexOfKey(subscriptionId) < 0 || ((MobileSignalController) sparseArray.get(subscriptionId)).getSimSlotIndex() != ((SubscriptionInfo) list2.get(i3)).getSimSlotIndex()) {
                Context context = this.mContext;
                Config config = this.mConfig;
                boolean z = this.mHasMobileDataFeature;
                TelephonyManager createForSubscriptionId = this.mPhone.createForSubscriptionId(subscriptionId);
                CallbackHandler callbackHandler = this.mCallbackHandler;
                SubscriptionInfo subscriptionInfo = (SubscriptionInfo) list2.get(i3);
                MobileSignalController mobileSignalController = r0;
                SubscriptionDefaults subscriptionDefaults = this.mSubDefaults;
                i = size;
                int i4 = subscriptionId;
                MobileSignalController mobileSignalController2 = new MobileSignalController(context, config, z, createForSubscriptionId, callbackHandler, this, subscriptionInfo, subscriptionDefaults, this.mReceiverHandler.getLooper());
                mobileSignalController.setUserSetupComplete(this.mUserSetup);
                StringBuilder sb = new StringBuilder();
                sb.append("create controller SubId:");
                sb.append(i4);
                sb.append(" SlotIndex:");
                sb.append(mobileSignalController.getSimSlotIndex());
                Log.i("NetworkController", sb.toString());
                this.mMobileSignalControllers.put(i4, mobileSignalController);
                if (((SubscriptionInfo) list2.get(i3)).getSimSlotIndex() == 0) {
                    this.mDefaultSignalController = mobileSignalController;
                }
                if (this.mListening) {
                    mobileSignalController.registerListener();
                    mobileSignalController.registerFiveGStateListener(this.mFiveGServiceClient);
                }
            } else {
                this.mMobileSignalControllers.put(subscriptionId, (MobileSignalController) sparseArray.get(subscriptionId));
                sparseArray.remove(subscriptionId);
                i = size;
            }
            i3++;
            size = i;
        }
        if (this.mListening) {
            for (int i5 = 0; i5 < sparseArray.size(); i5++) {
                int keyAt = sparseArray.keyAt(i5);
                if (sparseArray.get(keyAt) == this.mDefaultSignalController) {
                    this.mDefaultSignalController = null;
                }
                ((MobileSignalController) sparseArray.get(keyAt)).unregisterListener();
                ((MobileSignalController) sparseArray.get(keyAt)).unregisterFiveGStateListener(this.mFiveGServiceClient);
            }
        }
        this.mCallbackHandler.setSubs(list2);
        this.mCallbackHandler.setLTEStatus(this.mLTEIconStates);
        int i6 = 0;
        while (true) {
            int[] iArr = this.mProvisionState;
            if (i6 < iArr.length) {
                this.mCallbackHandler.setProvision(i6, iArr[i6]);
                i6++;
            } else {
                notifySetVirtualSimstate(this.softSimState);
                notifyAllListeners();
                pushConnectivityToSignals();
                updateAirplaneMode(true);
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void setUserSetupComplete(boolean z) {
        this.mReceiverHandler.post(new Runnable(z) {
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NetworkControllerImpl.this.lambda$setUserSetupComplete$0$NetworkControllerImpl(this.f$1);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: handleSetUserSetupComplete */
    public void lambda$setUserSetupComplete$0$NetworkControllerImpl(boolean z) {
        this.mUserSetup = z;
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).setUserSetupComplete(this.mUserSetup);
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean hasCorrectMobileControllers(List<SubscriptionInfo> list) {
        if (list.size() != this.mMobileSignalControllers.size()) {
            return false;
        }
        for (SubscriptionInfo subscriptionInfo : list) {
            if (this.mMobileSignalControllers.indexOfKey(subscriptionInfo.getSubscriptionId()) < 0) {
                return false;
            }
            MobileSignalController mobileSignalController = (MobileSignalController) this.mMobileSignalControllers.get(subscriptionInfo.getSubscriptionId());
            if (mobileSignalController != null && mobileSignalController.getSimSlotIndex() != subscriptionInfo.getSimSlotIndex()) {
                StringBuilder sb = new StringBuilder();
                sb.append("hasCorrectMobileControllers SubId:");
                sb.append(subscriptionInfo.getSubscriptionId());
                sb.append(" change from:");
                sb.append(mobileSignalController.getSimSlotIndex());
                sb.append(" to:");
                sb.append(subscriptionInfo.getSimSlotIndex());
                Log.i("NetworkController", sb.toString());
                return false;
            }
        }
        return true;
    }

    private void updateAirplaneMode(boolean z) {
        boolean z2 = true;
        if (Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
            z2 = false;
        }
        if (z2 != this.mAirplaneMode || z) {
            this.mAirplaneMode = z2;
            for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
                ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).setAirplaneMode(this.mAirplaneMode);
            }
            notifyListeners();
        }
    }

    private void refreshLocale() {
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        if (!locale.equals(this.mLocale)) {
            this.mLocale = locale;
            this.mWifiSignalController.refreshLocale();
            notifyAllListeners();
        }
    }

    private void notifyAllListeners() {
        notifyListeners();
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).notifyListeners();
        }
        this.mWifiSignalController.notifyListeners();
        this.mEthernetSignalController.notifyListeners();
    }

    private void notifyListeners() {
        this.mCallbackHandler.setIsAirplaneMode(new IconState(this.mAirplaneMode, TelephonyIcons.FLIGHT_MODE_ICON, R$string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSubs, this.mSimDetected);
    }

    /* access modifiers changed from: private */
    public void updateConnectivity() {
        NetworkCapabilities[] defaultNetworkCapabilitiesForUser;
        int[] transportTypes;
        this.mConnectedTransports.clear();
        this.mValidatedTransports.clear();
        for (NetworkCapabilities networkCapabilities : this.mConnectivityManager.getDefaultNetworkCapabilitiesForUser(this.mCurrentUserId)) {
            for (int i : networkCapabilities.getTransportTypes()) {
                this.mConnectedTransports.set(i);
                if (networkCapabilities.hasCapability(16)) {
                    this.mValidatedTransports.set(i);
                }
            }
        }
        if (CHATTY) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateConnectivity: mConnectedTransports=");
            sb.append(this.mConnectedTransports);
            String str = "NetworkController";
            Log.d(str, sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("updateConnectivity: mValidatedTransports=");
            sb2.append(this.mValidatedTransports);
            Log.d(str, sb2.toString());
        }
        this.mInetCondition = !this.mValidatedTransports.isEmpty();
        pushConnectivityToSignals();
    }

    private void pushConnectivityToSignals() {
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
        this.mWifiSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        this.mEthernetSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        NetworkSpeedController networkSpeedController = this.mNetworkSpeedController;
        if (networkSpeedController != null) {
            networkSpeedController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NetworkController state:");
        printWriter.println("  - telephony ------");
        printWriter.print("  hasVoiceCallingFeature()=");
        printWriter.println(hasVoiceCallingFeature());
        printWriter.println("  - connectivity ------");
        printWriter.print("  mConnectedTransports=");
        printWriter.println(this.mConnectedTransports);
        printWriter.print("  mValidatedTransports=");
        printWriter.println(this.mValidatedTransports);
        printWriter.print("  mInetCondition=");
        printWriter.println(this.mInetCondition);
        printWriter.print("  mAirplaneMode=");
        printWriter.println(this.mAirplaneMode);
        printWriter.print("  mLocale=");
        printWriter.println(this.mLocale);
        printWriter.print("  mLastServiceState=");
        printWriter.println(this.mLastServiceState);
        printWriter.print("  mIsEmergency=");
        printWriter.println(this.mIsEmergency);
        printWriter.print("  mEmergencySource=");
        printWriter.println(emergencyToString(this.mEmergencySource));
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).dump(printWriter);
        }
        this.mWifiSignalController.dump(printWriter);
        this.mEthernetSignalController.dump(printWriter);
        this.mAccessPoints.dump(printWriter);
    }

    private static final String emergencyToString(int i) {
        String str = ")";
        if (i > 300) {
            StringBuilder sb = new StringBuilder();
            sb.append("ASSUMED_VOICE_CONTROLLER(");
            sb.append(i - 200);
            sb.append(str);
            return sb.toString();
        } else if (i > 300) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("NO_SUB(");
            sb2.append(i - 300);
            sb2.append(str);
            return sb2.toString();
        } else if (i > 200) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("VOICE_CONTROLLER(");
            sb3.append(i - 200);
            sb3.append(str);
            return sb3.toString();
        } else if (i <= 100) {
            return i == 0 ? "NO_CONTROLLERS" : "UNKNOWN_SOURCE";
        } else {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("FIRST_CONTROLLER(");
            sb4.append(i - 100);
            sb4.append(str);
            return sb4.toString();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:228:0x045d, code lost:
        if (r11.equals(r14) != false) goto L_0x043b;
     */
    /* JADX WARNING: Removed duplicated region for block: B:257:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x04f6  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0188  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x019f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dispatchDemoCommand(java.lang.String r26, android.os.Bundle r27) {
        /*
            r25 = this;
            r0 = r25
            r1 = r26
            r2 = r27
            boolean r3 = r0.mDemoMode
            java.lang.String r4 = "NetworkController"
            r5 = 1
            if (r3 != 0) goto L_0x0039
            java.lang.String r3 = "enter"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0039
            boolean r1 = DEBUG
            if (r1 == 0) goto L_0x001e
            java.lang.String r1 = "Entering demo mode"
            android.util.Log.d(r4, r1)
        L_0x001e:
            r25.unregisterListeners()
            r0.mDemoMode = r5
            boolean r1 = r0.mInetCondition
            r0.mDemoInetCondition = r1
            com.android.systemui.statusbar.policy.WifiSignalController r1 = r0.mWifiSignalController
            com.android.systemui.statusbar.policy.SignalController$State r1 = r1.getState()
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r1 = (com.android.systemui.statusbar.policy.WifiSignalController.WifiState) r1
            r0.mDemoWifiState = r1
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r0 = r0.mDemoWifiState
            java.lang.String r1 = "DemoMode"
            r0.ssid = r1
            goto L_0x0537
        L_0x0039:
            boolean r3 = r0.mDemoMode
            r6 = 0
            if (r3 == 0) goto L_0x007b
            java.lang.String r3 = "exit"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x007b
            boolean r1 = DEBUG
            if (r1 == 0) goto L_0x004f
            java.lang.String r1 = "Exiting demo mode"
            android.util.Log.d(r4, r1)
        L_0x004f:
            r0.mDemoMode = r6
            r25.updateMobileControllers()
        L_0x0054:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r1 = r0.mMobileSignalControllers
            int r1 = r1.size()
            if (r6 >= r1) goto L_0x006a
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r1 = r0.mMobileSignalControllers
            java.lang.Object r1 = r1.valueAt(r6)
            com.android.systemui.statusbar.policy.MobileSignalController r1 = (com.android.systemui.statusbar.policy.MobileSignalController) r1
            r1.resetLastState()
            int r6 = r6 + 1
            goto L_0x0054
        L_0x006a:
            com.android.systemui.statusbar.policy.WifiSignalController r1 = r0.mWifiSignalController
            r1.resetLastState()
            android.os.Handler r1 = r0.mReceiverHandler
            java.lang.Runnable r2 = r0.mRegisterListeners
            r1.post(r2)
            r25.notifyAllListeners()
            goto L_0x0537
        L_0x007b:
            boolean r3 = r0.mDemoMode
            if (r3 == 0) goto L_0x0537
            java.lang.String r3 = "network"
            boolean r1 = r1.equals(r3)
            if (r1 == 0) goto L_0x0537
            java.lang.String r1 = "airplane"
            java.lang.String r1 = r2.getString(r1)
            java.lang.String r3 = "show"
            if (r1 == 0) goto L_0x00a5
            boolean r1 = r1.equals(r3)
            com.android.systemui.statusbar.policy.CallbackHandler r4 = r0.mCallbackHandler
            com.android.systemui.statusbar.policy.NetworkController$IconState r7 = new com.android.systemui.statusbar.policy.NetworkController$IconState
            int r8 = com.android.systemui.statusbar.policy.TelephonyIcons.FLIGHT_MODE_ICON
            int r9 = com.android.systemui.R$string.accessibility_airplane_mode
            android.content.Context r10 = r0.mContext
            r7.<init>(r1, r8, r9, r10)
            r4.setIsAirplaneMode(r7)
        L_0x00a5:
            java.lang.String r1 = "fully"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x00e8
            boolean r1 = java.lang.Boolean.parseBoolean(r1)
            r0.mDemoInetCondition = r1
            java.util.BitSet r1 = new java.util.BitSet
            r1.<init>()
            boolean r4 = r0.mDemoInetCondition
            if (r4 == 0) goto L_0x00c3
            com.android.systemui.statusbar.policy.WifiSignalController r4 = r0.mWifiSignalController
            int r4 = r4.mTransportType
            r1.set(r4)
        L_0x00c3:
            com.android.systemui.statusbar.policy.WifiSignalController r4 = r0.mWifiSignalController
            r4.updateConnectivity(r1, r1)
            r4 = r6
        L_0x00c9:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r7 = r0.mMobileSignalControllers
            int r7 = r7.size()
            if (r4 >= r7) goto L_0x00e8
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r7 = r0.mMobileSignalControllers
            java.lang.Object r7 = r7.valueAt(r4)
            com.android.systemui.statusbar.policy.MobileSignalController r7 = (com.android.systemui.statusbar.policy.MobileSignalController) r7
            boolean r8 = r0.mDemoInetCondition
            if (r8 == 0) goto L_0x00e2
            int r8 = r7.mTransportType
            r1.set(r8)
        L_0x00e2:
            r7.updateConnectivity(r1, r1)
            int r4 = r4 + 1
            goto L_0x00c9
        L_0x00e8:
            java.lang.String r1 = "wifi"
            java.lang.String r1 = r2.getString(r1)
            r4 = 110414(0x1af4e, float:1.54723E-40)
            r7 = 3365(0xd25, float:4.715E-42)
            java.lang.String r8 = "activity"
            java.lang.String r9 = "null"
            java.lang.String r10 = "level"
            r12 = 2
            if (r1 == 0) goto L_0x01c1
            boolean r1 = r1.equals(r3)
            java.lang.String r13 = r2.getString(r10)
            if (r13 == 0) goto L_0x0129
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r14 = r0.mDemoWifiState
            boolean r15 = r13.equals(r9)
            if (r15 == 0) goto L_0x0111
            r13 = -1
            goto L_0x011c
        L_0x0111:
            int r13 = java.lang.Integer.parseInt(r13)
            int r15 = com.android.systemui.statusbar.policy.WifiIcons.WIFI_LEVEL_COUNT
            int r15 = r15 - r5
            int r13 = java.lang.Math.min(r13, r15)
        L_0x011c:
            r14.level = r13
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r13 = r0.mDemoWifiState
            int r14 = r13.level
            if (r14 < 0) goto L_0x0126
            r14 = r5
            goto L_0x0127
        L_0x0126:
            r14 = r6
        L_0x0127:
            r13.connected = r14
        L_0x0129:
            java.lang.String r13 = "inflate"
            boolean r14 = r2.containsKey(r13)
            if (r14 == 0) goto L_0x0153
            r14 = r6
        L_0x0132:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            int r15 = r15.size()
            if (r14 >= r15) goto L_0x0153
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            java.lang.Object r15 = r15.valueAt(r14)
            com.android.systemui.statusbar.policy.MobileSignalController r15 = (com.android.systemui.statusbar.policy.MobileSignalController) r15
            java.lang.String r11 = r2.getString(r13)
            java.lang.String r6 = "true"
            boolean r6 = r6.equals(r11)
            r15.mInflateSignalStrengths = r6
            int r14 = r14 + 1
            r6 = 0
            goto L_0x0132
        L_0x0153:
            java.lang.String r6 = r2.getString(r8)
            if (r6 == 0) goto L_0x01a6
            int r11 = r6.hashCode()
            if (r11 == r7) goto L_0x017b
            if (r11 == r4) goto L_0x0171
            r13 = 100357129(0x5fb5409, float:2.3634796E-35)
            if (r11 == r13) goto L_0x0167
            goto L_0x0185
        L_0x0167:
            java.lang.String r11 = "inout"
            boolean r6 = r6.equals(r11)
            if (r6 == 0) goto L_0x0185
            r6 = 0
            goto L_0x0186
        L_0x0171:
            java.lang.String r11 = "out"
            boolean r6 = r6.equals(r11)
            if (r6 == 0) goto L_0x0185
            r6 = r12
            goto L_0x0186
        L_0x017b:
            java.lang.String r11 = "in"
            boolean r6 = r6.equals(r11)
            if (r6 == 0) goto L_0x0185
            r6 = r5
            goto L_0x0186
        L_0x0185:
            r6 = -1
        L_0x0186:
            if (r6 == 0) goto L_0x019f
            if (r6 == r5) goto L_0x0199
            if (r6 == r12) goto L_0x0193
            com.android.systemui.statusbar.policy.WifiSignalController r6 = r0.mWifiSignalController
            r11 = 0
            r6.setActivity(r11)
            goto L_0x01ac
        L_0x0193:
            com.android.systemui.statusbar.policy.WifiSignalController r6 = r0.mWifiSignalController
            r6.setActivity(r12)
            goto L_0x01ac
        L_0x0199:
            com.android.systemui.statusbar.policy.WifiSignalController r6 = r0.mWifiSignalController
            r6.setActivity(r5)
            goto L_0x01ac
        L_0x019f:
            com.android.systemui.statusbar.policy.WifiSignalController r6 = r0.mWifiSignalController
            r11 = 3
            r6.setActivity(r11)
            goto L_0x01ac
        L_0x01a6:
            com.android.systemui.statusbar.policy.WifiSignalController r6 = r0.mWifiSignalController
            r11 = 0
            r6.setActivity(r11)
        L_0x01ac:
            java.lang.String r6 = "ssid"
            java.lang.String r6 = r2.getString(r6)
            if (r6 == 0) goto L_0x01b8
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r11 = r0.mDemoWifiState
            r11.ssid = r6
        L_0x01b8:
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r6 = r0.mDemoWifiState
            r6.enabled = r1
            com.android.systemui.statusbar.policy.WifiSignalController r1 = r0.mWifiSignalController
            r1.notifyListeners()
        L_0x01c1:
            java.lang.String r1 = "sims"
            java.lang.String r1 = r2.getString(r1)
            r6 = 8
            if (r1 == 0) goto L_0x021c
            int r1 = java.lang.Integer.parseInt(r1)
            int r1 = android.util.MathUtils.constrain(r1, r5, r6)
            java.util.ArrayList r11 = new java.util.ArrayList
            r11.<init>()
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r13 = r0.mMobileSignalControllers
            int r13 = r13.size()
            if (r1 == r13) goto L_0x021c
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r13 = r0.mMobileSignalControllers
            r13.clear()
            android.telephony.SubscriptionManager r13 = r0.mSubscriptionManager
            int r13 = r13.getActiveSubscriptionInfoCountMax()
            r14 = r13
        L_0x01ec:
            int r15 = r13 + r1
            if (r14 >= r15) goto L_0x01fa
            android.telephony.SubscriptionInfo r15 = r0.addSignalController(r14, r14)
            r11.add(r15)
            int r14 = r14 + 1
            goto L_0x01ec
        L_0x01fa:
            com.android.systemui.statusbar.policy.CallbackHandler r1 = r0.mCallbackHandler
            r1.setSubs(r11)
            r1 = 0
        L_0x0200:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r11 = r0.mMobileSignalControllers
            int r11 = r11.size()
            if (r1 >= r11) goto L_0x021c
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r11 = r0.mMobileSignalControllers
            int r11 = r11.keyAt(r1)
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r13 = r0.mMobileSignalControllers
            java.lang.Object r11 = r13.get(r11)
            com.android.systemui.statusbar.policy.MobileSignalController r11 = (com.android.systemui.statusbar.policy.MobileSignalController) r11
            r11.notifyListeners()
            int r1 = r1 + 1
            goto L_0x0200
        L_0x021c:
            java.lang.String r1 = "nosim"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x0233
            boolean r1 = r1.equals(r3)
            r0.mHasNoSubs = r1
            com.android.systemui.statusbar.policy.CallbackHandler r1 = r0.mCallbackHandler
            boolean r11 = r0.mHasNoSubs
            boolean r13 = r0.mSimDetected
            r1.setNoSims(r11, r13)
        L_0x0233:
            java.lang.String r1 = "mobile"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x0514
            boolean r1 = r1.equals(r3)
            java.lang.String r11 = "datatype"
            java.lang.String r11 = r2.getString(r11)
            java.lang.String r13 = "slot"
            java.lang.String r13 = r2.getString(r13)
            boolean r14 = android.text.TextUtils.isEmpty(r13)
            if (r14 == 0) goto L_0x0253
            r13 = 0
            goto L_0x0257
        L_0x0253:
            int r13 = java.lang.Integer.parseInt(r13)
        L_0x0257:
            r14 = 0
            int r13 = android.util.MathUtils.constrain(r13, r14, r6)
            java.util.ArrayList r14 = new java.util.ArrayList
            r14.<init>()
        L_0x0261:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            int r15 = r15.size()
            if (r15 > r13) goto L_0x0277
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            int r15 = r15.size()
            android.telephony.SubscriptionInfo r15 = r0.addSignalController(r15, r15)
            r14.add(r15)
            goto L_0x0261
        L_0x0277:
            boolean r15 = r14.isEmpty()
            if (r15 != 0) goto L_0x0282
            com.android.systemui.statusbar.policy.CallbackHandler r15 = r0.mCallbackHandler
            r15.setSubs(r14)
        L_0x0282:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r14 = r0.mMobileSignalControllers
            java.lang.Object r13 = r14.valueAt(r13)
            com.android.systemui.statusbar.policy.MobileSignalController r13 = (com.android.systemui.statusbar.policy.MobileSignalController) r13
            com.android.systemui.statusbar.policy.SignalController$State r14 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r14 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r14
            if (r11 == 0) goto L_0x0294
            r15 = r5
            goto L_0x0295
        L_0x0294:
            r15 = 0
        L_0x0295:
            r14.dataSim = r15
            com.android.systemui.statusbar.policy.SignalController$State r14 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r14 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r14
            if (r11 == 0) goto L_0x02a1
            r15 = r5
            goto L_0x02a2
        L_0x02a1:
            r15 = 0
        L_0x02a2:
            r14.isDefault = r15
            com.android.systemui.statusbar.policy.SignalController$State r14 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r14 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r14
            if (r11 == 0) goto L_0x02ae
            r15 = r5
            goto L_0x02af
        L_0x02ae:
            r15 = 0
        L_0x02af:
            r14.dataConnected = r15
            java.lang.String r14 = "lte+"
            java.lang.String r15 = "lte"
            if (r11 == 0) goto L_0x0381
            com.android.systemui.statusbar.policy.SignalController$State r16 = r13.getState()
            r6 = r16
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r6 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r6
            java.lang.String r12 = "1x"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x02cb
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.ONE_X
            goto L_0x037f
        L_0x02cb:
            java.lang.String r12 = "2g"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x02d7
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.TWO_G
            goto L_0x037f
        L_0x02d7:
            java.lang.String r12 = "3g"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x02e3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.THREE_G
            goto L_0x037f
        L_0x02e3:
            java.lang.String r12 = "3g+"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x02ef
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.THREE_G_PLUS
            goto L_0x037f
        L_0x02ef:
            java.lang.String r12 = "4g"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x02fb
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.FOUR_G
            goto L_0x037f
        L_0x02fb:
            java.lang.String r12 = "4g+"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0307
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.FOUR_G_PLUS
            goto L_0x037f
        L_0x0307:
            java.lang.String r12 = "e"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0313
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.f100E
            goto L_0x037f
        L_0x0313:
            java.lang.String r12 = "g"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x031e
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.f101G
            goto L_0x037f
        L_0x031e:
            java.lang.String r12 = "h"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0329
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.f102H
            goto L_0x037f
        L_0x0329:
            java.lang.String r12 = "h+"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0334
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.H_PLUS
            goto L_0x037f
        L_0x0334:
            boolean r12 = r11.equals(r15)
            if (r12 == 0) goto L_0x033d
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.LTE
            goto L_0x037f
        L_0x033d:
            boolean r12 = r11.equals(r14)
            if (r12 == 0) goto L_0x0346
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.LTE_PLUS
            goto L_0x037f
        L_0x0346:
            java.lang.String r12 = "dis"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0351
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.DATA_DISABLED
            goto L_0x037f
        L_0x0351:
            java.lang.String r12 = "not"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x035c
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.NOT_DEFAULT_DATA
            goto L_0x037f
        L_0x035c:
            java.lang.String r12 = "4glte"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0367
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.FOUR_G_LTE
            goto L_0x037f
        L_0x0367:
            java.lang.String r12 = "5g"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x0372
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.FIVE_G_BASIC
            goto L_0x037f
        L_0x0372:
            java.lang.String r12 = "5g_uwb"
            boolean r12 = r11.equals(r12)
            if (r12 == 0) goto L_0x037d
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.FIVE_G_UWB
            goto L_0x037f
        L_0x037d:
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r12 = com.android.systemui.statusbar.policy.TelephonyIcons.UNKNOWN
        L_0x037f:
            r6.iconGroup = r12
        L_0x0381:
            java.lang.String r6 = "roam"
            boolean r6 = r2.containsKey(r6)
            if (r6 == 0) goto L_0x039b
            com.android.systemui.statusbar.policy.SignalController$State r6 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r6 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r6
            java.lang.String r12 = "roam"
            java.lang.String r12 = r2.getString(r12)
            boolean r12 = r3.equals(r12)
            r6.roaming = r12
        L_0x039b:
            java.lang.String r6 = r2.getString(r10)
            if (r6 == 0) goto L_0x03d0
            com.android.systemui.statusbar.policy.SignalController$State r10 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r10 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r10
            boolean r9 = r6.equals(r9)
            if (r9 == 0) goto L_0x03af
            r6 = -1
            goto L_0x03b9
        L_0x03af:
            int r6 = java.lang.Integer.parseInt(r6)
            int r9 = android.telephony.SignalStrength.NUM_SIGNAL_STRENGTH_BINS
            int r6 = java.lang.Math.min(r6, r9)
        L_0x03b9:
            r10.level = r6
            com.android.systemui.statusbar.policy.SignalController$State r6 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r6 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r6
            com.android.systemui.statusbar.policy.SignalController$State r9 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r9 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r9
            int r9 = r9.level
            if (r9 < 0) goto L_0x03cd
            r9 = r5
            goto L_0x03ce
        L_0x03cd:
            r9 = 0
        L_0x03ce:
            r6.connected = r9
        L_0x03d0:
            java.lang.String r6 = "stack"
            java.lang.String r6 = r2.getString(r6)
            if (r6 == 0) goto L_0x0489
            if (r11 == 0) goto L_0x0489
            boolean r6 = r6.equals(r3)
            if (r6 == 0) goto L_0x0489
            com.android.systemui.statusbar.policy.SignalController$State r6 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r6 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r6
            int r6 = r6.level
            com.android.systemui.statusbar.policy.SignalController$State r9 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r9 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r9
            boolean r9 = r9.roaming
            com.android.systemui.statusbar.policy.SignalController$State r10 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r10 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r10
            com.android.systemui.statusbar.policy.SignalController$IconGroup r10 = r10.iconGroup
            r18 = r10
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r18 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileIconGroup) r18
            java.lang.String r10 = "1x"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0408
        L_0x0404:
            r10 = 7
        L_0x0405:
            r20 = r10
            goto L_0x0460
        L_0x0408:
            java.lang.String r10 = "g"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0413
            r20 = r5
            goto L_0x0460
        L_0x0413:
            java.lang.String r10 = "2g"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x041d
            r10 = 4
            goto L_0x0405
        L_0x041d:
            java.lang.String r10 = "3g"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0428
            r10 = 17
            goto L_0x0405
        L_0x0428:
            java.lang.String r10 = "4g"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0433
        L_0x0430:
            r10 = 13
            goto L_0x0405
        L_0x0433:
            java.lang.String r10 = "4g+"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x043e
        L_0x043b:
            r10 = 19
            goto L_0x0405
        L_0x043e:
            java.lang.String r10 = "h"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0449
        L_0x0446:
            r20 = 8
            goto L_0x0460
        L_0x0449:
            java.lang.String r10 = "h+"
            boolean r10 = r11.equals(r10)
            if (r10 == 0) goto L_0x0452
            goto L_0x0446
        L_0x0452:
            boolean r10 = r11.equals(r15)
            if (r10 == 0) goto L_0x0459
            goto L_0x0430
        L_0x0459:
            boolean r10 = r11.equals(r14)
            if (r10 == 0) goto L_0x0404
            goto L_0x043b
        L_0x0460:
            boolean r10 = r11.equals(r15)
            if (r10 != 0) goto L_0x0470
            boolean r10 = r11.equals(r14)
            if (r10 == 0) goto L_0x046d
            goto L_0x0470
        L_0x046d:
            r24 = 0
            goto L_0x0472
        L_0x0470:
            r24 = r5
        L_0x0472:
            com.android.systemui.statusbar.policy.SignalController$State r10 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r10 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r10
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = new com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup
            r17 = r11
            r19 = r20
            r21 = r6
            r22 = r6
            r23 = r9
            r17.<init>(r18, r19, r20, r21, r22, r23, r24)
            r10.iconGroup = r11
        L_0x0489:
            java.lang.String r6 = "virtual"
            boolean r6 = r2.containsKey(r6)
            if (r6 == 0) goto L_0x04a5
            com.android.systemui.statusbar.policy.SignalController$State r6 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r6 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r6
            java.lang.String r9 = "virtual"
            java.lang.String r9 = r2.getString(r9)
            boolean r9 = r3.equals(r9)
            r6.isVirtual = r9
        L_0x04a5:
            java.lang.String r6 = r2.getString(r8)
            if (r6 == 0) goto L_0x04fc
            com.android.systemui.statusbar.policy.SignalController$State r8 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r8 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r8
            r8.dataConnected = r5
            int r8 = r6.hashCode()
            if (r8 == r7) goto L_0x04d5
            if (r8 == r4) goto L_0x04cb
            r4 = 100357129(0x5fb5409, float:2.3634796E-35)
            if (r8 == r4) goto L_0x04c1
            goto L_0x04df
        L_0x04c1:
            java.lang.String r4 = "inout"
            boolean r4 = r6.equals(r4)
            if (r4 == 0) goto L_0x04df
            r4 = 0
            goto L_0x04e0
        L_0x04cb:
            java.lang.String r4 = "out"
            boolean r4 = r6.equals(r4)
            if (r4 == 0) goto L_0x04df
            r4 = 2
            goto L_0x04e0
        L_0x04d5:
            java.lang.String r4 = "in"
            boolean r4 = r6.equals(r4)
            if (r4 == 0) goto L_0x04df
            r4 = r5
            goto L_0x04e0
        L_0x04df:
            r4 = -1
        L_0x04e0:
            if (r4 == 0) goto L_0x04f6
            if (r4 == r5) goto L_0x04f1
            r6 = 2
            if (r4 == r6) goto L_0x04ec
            r11 = 0
            r13.setActivity(r11)
            goto L_0x0500
        L_0x04ec:
            r11 = 0
            r13.setActivity(r6)
            goto L_0x0500
        L_0x04f1:
            r11 = 0
            r13.setActivity(r5)
            goto L_0x0500
        L_0x04f6:
            r11 = 0
            r4 = 3
            r13.setActivity(r4)
            goto L_0x0500
        L_0x04fc:
            r11 = 0
            r13.setActivity(r11)
        L_0x0500:
            com.android.systemui.statusbar.policy.SignalController$State r4 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r4 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r4
            r4.enabled = r1
            com.android.systemui.statusbar.policy.SignalController$State r4 = r13.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r4 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r4
            r4.isDemoMode = r1
            r13.notifyListeners()
            goto L_0x0515
        L_0x0514:
            r11 = 0
        L_0x0515:
            java.lang.String r1 = "carriernetworkchange"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x0537
            boolean r1 = r1.equals(r3)
        L_0x0521:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r2 = r0.mMobileSignalControllers
            int r2 = r2.size()
            if (r11 >= r2) goto L_0x0537
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r2 = r0.mMobileSignalControllers
            java.lang.Object r2 = r2.valueAt(r11)
            com.android.systemui.statusbar.policy.MobileSignalController r2 = (com.android.systemui.statusbar.policy.MobileSignalController) r2
            r2.setCarrierNetworkChangeMode(r1)
            int r11 = r11 + 1
            goto L_0x0521
        L_0x0537:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.NetworkControllerImpl.dispatchDemoCommand(java.lang.String, android.os.Bundle):void");
    }

    private SubscriptionInfo addSignalController(int i, int i2) {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(i, "", i2, "", "", 0, 0, "", 0, null, null, null, "", false, null, null);
        MobileSignalController mobileSignalController = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone.createForSubscriptionId(subscriptionInfo.getSubscriptionId()), this.mCallbackHandler, this, subscriptionInfo, this.mSubDefaults, this.mReceiverHandler.getLooper());
        this.mMobileSignalControllers.put(i, mobileSignalController);
        ((MobileState) mobileSignalController.getState()).userSetup = true;
        return subscriptionInfo;
    }

    public boolean hasEmergencyCryptKeeperText() {
        return EncryptionHelper.IS_DATA_ENCRYPTED;
    }

    public boolean isRadioOn() {
        return !this.mAirplaneMode;
    }

    public void onLTEStatusUpdate() {
        if (this.mContext != null) {
            boolean[][] zArr = (boolean[][]) Array.newInstance(boolean.class, new int[]{2, 6});
            int[] iArr = {0, 0, 0, 0, 0, 0};
            for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
                MobileSignalController mobileSignalController = (MobileSignalController) this.mMobileSignalControllers.valueAt(i);
                boolean[] lTEStatus = mobileSignalController.getLTEStatus();
                for (int i2 = 0; i2 < lTEStatus.length; i2++) {
                    if (mobileSignalController.getSimSlotIndex() < zArr.length) {
                        zArr[mobileSignalController.getSimSlotIndex()][i2] = lTEStatus[i2];
                    }
                }
            }
            if (zArr[0][0] && zArr[1][0]) {
                iArr[0] = 3;
            } else if (zArr[0][0] && !zArr[1][0]) {
                iArr[0] = 1;
            } else if (!zArr[0][0] && zArr[1][0]) {
                iArr[0] = 2;
            }
            if (zArr[0][2] && zArr[1][2]) {
                iArr[2] = 3;
            } else if (zArr[0][2] && !zArr[1][2]) {
                iArr[2] = 1;
            } else if (!zArr[0][2] && zArr[1][2]) {
                iArr[2] = 2;
            }
            if (OpUtils.isUSS() && iArr[2] > 0) {
                iArr[0] = 0;
            }
            StringBuffer stringBuffer = new StringBuffer();
            for (int valueOf : iArr) {
                stringBuffer.append(String.valueOf(valueOf));
            }
            IconState[] multiLTEIcons = getMultiLTEIcons(iArr);
            StringBuilder sb = new StringBuilder();
            sb.append(" multiLTEstatus:");
            sb.append(stringBuffer);
            sb.append(" ShowHD:");
            sb.append(OpUtils.isSupportShowHD());
            Log.i("NetworkController", sb.toString());
            IconState[] iconStateArr = this.mLTEIconStates;
            if (!(iconStateArr[0] == multiLTEIcons[0] && iconStateArr[2] == multiLTEIcons[2])) {
                this.mLTEIconStates = multiLTEIcons;
                this.mCallbackHandler.setLTEStatus(this.mLTEIconStates);
            }
        }
    }

    private IconState[] getMultiLTEIcons(int[] iArr) {
        int[] iArr2 = new int[iArr.length];
        IconState[] iconStateArr = new IconState[iArr.length];
        int i = 0;
        int i2 = 0;
        while (true) {
            int[] iArr3 = this.mProvisionState;
            if (i >= iArr3.length) {
                break;
            }
            if (iArr3[i] == 1) {
                i2++;
            }
            i++;
        }
        boolean z = i2 > 1 && this.mMobileSignalControllers.size() > 1;
        if (i2 > 0) {
            if (OpUtils.isSupportShowVoLTE(this.mContext)) {
                if (!OpUtils.isSupportShowHD()) {
                    if (OpUtils.isSupportMultiLTEstatus(this.mContext) && z && iArr[0] > 0) {
                        int i3 = iArr[0];
                        int[] iArr4 = OpSignalIcons.VOLTE_ICONS;
                        if (i3 <= iArr4.length) {
                            iArr2[0] = iArr4[iArr[0] - 1];
                        }
                    }
                    if (iArr[0] > 0) {
                        iArr2[0] = OpSignalIcons.VOLTE;
                    }
                } else if (iArr[0] == 0) {
                    iArr2[0] = OpSignalIcons.HD_UNAVAILABLE;
                } else {
                    if (OpUtils.isSupportMultiLTEstatus(this.mContext) && z && iArr[0] > 0) {
                        int i4 = iArr[0];
                        int[] iArr5 = OpSignalIcons.HD_ICONS;
                        if (i4 <= iArr5.length) {
                            iArr2[0] = iArr5[iArr[0] - 1];
                        }
                    }
                    iArr2[0] = OpSignalIcons.f118HD;
                }
            }
            if (OpUtils.isSupportShowVoWifi()) {
                if (OpUtils.isSupportMultiLTEstatus(this.mContext) && z && iArr[2] > 0) {
                    int i5 = iArr[2];
                    int[] iArr6 = OpSignalIcons.VOWIFI_ICONS;
                    if (i5 <= iArr6.length) {
                        iArr2[2] = iArr6[iArr[2] - 1];
                    }
                }
                if (iArr[2] > 0) {
                    iArr2[2] = OpSignalIcons.VOWIFI;
                }
            }
        }
        for (int i6 = 0; i6 < iconStateArr.length; i6++) {
            iconStateArr[i6] = new IconState(iArr2[i6] > 0, iArr2[i6], null);
        }
        return iconStateArr;
    }

    private void onProvisionChange(int i, int i2) {
        if (i < this.mProvisionState.length) {
            StringBuilder sb = new StringBuilder();
            sb.append("onProvisionChange slotId:");
            sb.append(i);
            sb.append(" provi:");
            sb.append(i2);
            Log.i("NetworkController", sb.toString());
            this.mProvisionState[i] = i2;
            onLTEStatusUpdate();
            CallbackHandler callbackHandler = this.mCallbackHandler;
            if (callbackHandler != null) {
                callbackHandler.setProvision(i, i2);
            }
        }
    }

    private int getSlotProvisionStatus(int i, int i2) {
        String str = " Exception: ";
        String str2 = "Failed to get pref, slotId: ";
        String str3 = "NetworkController";
        if (this.mExtTelephony == null) {
            this.mExtTelephony = Stub.asInterface(ServiceManager.getService("extphone"));
        }
        try {
            i2 = this.mExtTelephony.getCurrentUiccCardProvisioningStatus(i);
        } catch (RemoteException e) {
            this.mExtTelephony = null;
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append(i);
            sb.append(str);
            sb.append(e);
            Log.e(str3, sb.toString());
        } catch (NullPointerException e2) {
            this.mExtTelephony = null;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str2);
            sb2.append(i);
            sb2.append(str);
            sb2.append(e2);
            Log.e(str3, sb2.toString());
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("getSlotProvisionStatus slotId: ");
        sb3.append(i);
        sb3.append(", status = ");
        sb3.append(i2);
        Log.d(str3, sb3.toString());
        return i2;
    }

    private void initProvistionState() {
        Log.i("NetworkController", "init provision");
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < this.mProvisionState.length; i++) {
            onProvisionChange(i, getSlotProvisionStatus(i, 1));
        }
    }

    private void checkVirtualSimcard() {
        int simCount = this.mPhone.getSimCount();
        boolean z = false;
        for (int i = 0; i < simCount; i++) {
            boolean isSoftSIM = isSoftSIM(i);
            boolean equals = "1".equals(getVirtualPilot(this.mContext, i));
            int i2 = SOFTSIM_DISABLE;
            if (isSoftSIM && equals) {
                i2 = SOFTSIM_ENABLE_PILOT;
            } else if (isSoftSIM && !equals) {
                i2 = SOFTSIM_ENABLE;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("checkVirtualSimcard slotId:");
            sb.append(i);
            sb.append(" virtualSimState:");
            sb.append(i2);
            Log.i("NetworkController", sb.toString());
            int[] iArr = this.softSimState;
            if (i2 != iArr[i]) {
                iArr[i] = i2;
                if (i2 == SOFTSIM_ENABLE_PILOT) {
                    this.mConnectivityManager.stopTethering(0);
                }
                z = true;
            }
        }
        if (z) {
            notifySetVirtualSimstate(this.softSimState);
        }
    }

    private boolean isSoftSIM(int i) {
        String str = "isSoftSIM";
        String str2 = "NetworkController";
        if (this.mExtTelephony == null) {
            this.mExtTelephony = Stub.asInterface(ServiceManager.getService("extphone"));
        }
        try {
            if (this.mExtTelephony != null) {
                Bundle bundle = new Bundle();
                bundle.putInt("phone", i);
                StringBuilder sb = new StringBuilder();
                sb.append("isSoftSIM slot = ");
                sb.append(i);
                Log.i(str2, sb.toString());
                Method declaredMethod = this.mExtTelephony.getClass().getDeclaredMethod("generalGetter", new Class[]{String.class, Bundle.class});
                declaredMethod.setAccessible(true);
                boolean z = ((Bundle) declaredMethod.invoke(this.mExtTelephony, new Object[]{str, bundle})).getBoolean(str, false);
                String str3 = "slot ";
                if (z) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str3);
                    sb2.append(i);
                    sb2.append(" is softsim");
                    Log.i(str2, sb2.toString());
                    return true;
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append(str3);
                sb3.append(i);
                sb3.append(" is NOT softsim");
                Log.i(str2, sb3.toString());
            }
        } catch (Exception e) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("exception : ");
            sb4.append(e);
            Log.i(str2, sb4.toString());
        }
        return false;
    }

    private String getVirtualPilot(Context context, int i) {
        String str = "NetworkController";
        String str2 = null;
        if (context == null) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("slot=\"");
            sb.append(i);
            sb.append("\"");
            Cursor query = context.getContentResolver().query(SOFTSIM_URL, new String[]{"slot", "iccid", "permit_package", "forbid_package", "pilot"}, new StringBuilder(sb.toString()).toString(), null, "slot");
            if (query != null) {
                query.moveToFirst();
                while (!query.isAfterLast()) {
                    str2 = query.getString(4);
                    query.moveToNext();
                }
                query.close();
            }
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("getVirtualIccid SQLiteException ");
            sb2.append(e);
            Log.d(str, sb2.toString());
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Virtual sim slot:");
        sb3.append(i);
        sb3.append(" pilot = ");
        sb3.append(str2);
        Log.d(str, sb3.toString());
        return str2;
    }

    private void notifySetVirtualSimstate(int[] iArr) {
        this.mCallbackHandler.setVirtualSimstate(iArr);
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            ((MobileSignalController) this.mMobileSignalControllers.valueAt(i)).setVirtualSimstate(iArr);
        }
    }

    public void setNetworkSpeedController(NetworkSpeedController networkSpeedController) {
        this.mNetworkSpeedController = networkSpeedController;
        NetworkSpeedController networkSpeedController2 = this.mNetworkSpeedController;
        if (networkSpeedController2 != null) {
            networkSpeedController2.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
    }

    private boolean hasSimReady() {
        int simCount = this.mPhone.getSimCount();
        for (int i = 0; i < simCount; i++) {
            int simState = this.mPhone.getSimState(i);
            if (simState == 5 || simState == 10) {
                return true;
            }
        }
        return false;
    }

    private void opActionTetherErrorAlertDialog() {
        if (!this.mOpActionTetherDialogShowing) {
            int i = R$string.hotspot_operator_dialog_othererror_title;
            int i2 = R$string.hotspot_operator_dialog_othererror_msg;
            AlertDialog create = new Builder(this.mContext).setMessage(i2).setTitle(i).setCancelable(false).setPositiveButton(R$string.hotspot_operator_dialog_othererror_button, new OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    NetworkControllerImpl.this.mOpActionTetherDialogShowing = false;
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
            this.mOpActionTetherDialogShowing = true;
            create.show();
        }
    }
}
