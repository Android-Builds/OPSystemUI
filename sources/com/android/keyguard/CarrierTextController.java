package com.android.keyguard;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.keyguard.CarrierTextController.CarrierTextCallback;
import com.android.keyguard.CarrierTextController.CarrierTextCallbackInfo;
import com.android.settingslib.WirelessUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R$bool;
import com.android.systemui.R$string;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle.Observer;
import com.android.systemui.statusbar.policy.FiveGServiceClient;
import com.oneplus.keyguard.OpCarrierTextController;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CarrierTextController extends OpCarrierTextController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public int mActiveMobileDataSubscription = -1;
    protected final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        public void onRefreshCarrierInfo() {
            if (CarrierTextController.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onRefreshCarrierInfo(), mTelephonyCapable: ");
                sb.append(Boolean.toString(CarrierTextController.this.mTelephonyCapable));
                Log.d("CarrierTextController", sb.toString());
            }
            CarrierTextController.this.updateCarrierText();
        }

        public void onTelephonyCapable(boolean z) {
            if (CarrierTextController.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onTelephonyCapable() mTelephonyCapable: ");
                sb.append(Boolean.toString(z));
                Log.d("CarrierTextController", sb.toString());
            }
            CarrierTextController.this.mTelephonyCapable = z;
            CarrierTextController.this.updateCarrierText();
        }

        public void onSimStateChanged(int i, int i2, State state) {
            String str = "CarrierTextController";
            if (i2 < 0 || i2 >= CarrierTextController.this.mSimSlotsNumber) {
                StringBuilder sb = new StringBuilder();
                sb.append("onSimStateChanged() - slotId invalid: ");
                sb.append(i2);
                sb.append(" mTelephonyCapable: ");
                sb.append(Boolean.toString(CarrierTextController.this.mTelephonyCapable));
                Log.d(str, sb.toString());
                return;
            }
            if (CarrierTextController.DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("onSimStateChanged: ");
                sb2.append(CarrierTextController.this.getStatusForIccState(state));
                Log.d(str, sb2.toString());
            }
            if (CarrierTextController.this.getStatusForIccState(state) == StatusMode.SimIoError) {
                CarrierTextController.this.mSimErrorState[i2] = true;
                CarrierTextController.this.updateCarrierText();
            } else if (CarrierTextController.this.mSimErrorState[i2]) {
                CarrierTextController.this.mSimErrorState[i2] = false;
                CarrierTextController.this.updateCarrierText();
            }
        }
    };
    /* access modifiers changed from: private */
    public CarrierTextCallback mCarrierTextCallback;
    private Context mContext;
    protected boolean mDisplayOpportunisticSubscriptionCarrierText;
    private FiveGServiceClient mFiveGServiceClient;
    private final boolean mIsEmergencyCallCapable;
    protected KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onActiveDataSubscriptionIdChanged(int i) {
            CarrierTextController.this.mActiveMobileDataSubscription = i;
            CarrierTextController carrierTextController = CarrierTextController.this;
            if (carrierTextController.mKeyguardUpdateMonitor != null) {
                carrierTextController.updateCarrierText();
            }
        }
    };
    private CharSequence mSeparator;
    private boolean mShowAirplaneMode;
    private boolean mShowMissingSim;
    /* access modifiers changed from: private */
    public boolean[] mSimErrorState;
    /* access modifiers changed from: private */
    public final int mSimSlotsNumber;
    /* access modifiers changed from: private */
    public boolean mTelephonyCapable;
    private WakefulnessLifecycle mWakefulnessLifecycle;
    private final Observer mWakefulnessObserver = new Observer() {
        public void onFinishedWakingUp() {
            CarrierTextController.this.mCarrierTextCallback.finishedWakingUp();
        }

        public void onStartedGoingToSleep() {
            CarrierTextController.this.mCarrierTextCallback.startedGoingToSleep();
        }
    };
    private WifiManager mWifiManager;

    /* renamed from: com.android.keyguard.CarrierTextController$4 */
    static /* synthetic */ class C05524 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[State.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode = new int[StatusMode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(38:0|(2:1|2)|3|(2:5|6)|7|(2:9|10)|11|(2:13|14)|15|(2:17|18)|19|(2:21|22)|23|(2:25|26)|27|(2:29|30)|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|(3:55|56|58)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(41:0|(2:1|2)|3|5|6|7|(2:9|10)|11|(2:13|14)|15|17|18|19|(2:21|22)|23|(2:25|26)|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|(3:55|56|58)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(45:0|(2:1|2)|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Can't wrap try/catch for region: R(46:0|1|2|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|(2:33|34)|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Can't wrap try/catch for region: R(47:0|1|2|3|5|6|7|(2:9|10)|11|13|14|15|17|18|19|(2:21|22)|23|25|26|27|29|30|31|33|34|35|37|38|39|40|41|42|43|44|45|46|47|48|49|50|51|52|53|54|55|56|58) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:39:0x0081 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:41:0x008b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:43:0x0095 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:45:0x009f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:47:0x00a9 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:49:0x00b3 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:51:0x00bd */
        /* JADX WARNING: Missing exception handler attribute for start block: B:53:0x00c7 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:55:0x00d1 */
        static {
            /*
                com.android.internal.telephony.IccCardConstants$State[] r0 = com.android.internal.telephony.IccCardConstants.State.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State = r0
                r0 = 1
                int[] r1 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.internal.telephony.IccCardConstants$State r2 = com.android.internal.telephony.IccCardConstants.State.ABSENT     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r2 = r2.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r1[r2] = r0     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                r1 = 2
                int[] r2 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.internal.telephony.IccCardConstants$State r3 = com.android.internal.telephony.IccCardConstants.State.NETWORK_LOCKED     // Catch:{ NoSuchFieldError -> 0x001f }
                int r3 = r3.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2[r3] = r1     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                r2 = 3
                int[] r3 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.internal.telephony.IccCardConstants$State r4 = com.android.internal.telephony.IccCardConstants.State.NOT_READY     // Catch:{ NoSuchFieldError -> 0x002a }
                int r4 = r4.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r3[r4] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                r3 = 4
                int[] r4 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.internal.telephony.IccCardConstants$State r5 = com.android.internal.telephony.IccCardConstants.State.PIN_REQUIRED     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r5 = r5.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r4[r5] = r3     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                r4 = 5
                int[] r5 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.internal.telephony.IccCardConstants$State r6 = com.android.internal.telephony.IccCardConstants.State.PUK_REQUIRED     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r6 = r6.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r5[r6] = r4     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                r5 = 6
                int[] r6 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x004b }
                com.android.internal.telephony.IccCardConstants$State r7 = com.android.internal.telephony.IccCardConstants.State.READY     // Catch:{ NoSuchFieldError -> 0x004b }
                int r7 = r7.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r6[r7] = r5     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                r6 = 7
                int[] r7 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0056 }
                com.android.internal.telephony.IccCardConstants$State r8 = com.android.internal.telephony.IccCardConstants.State.PERM_DISABLED     // Catch:{ NoSuchFieldError -> 0x0056 }
                int r8 = r8.ordinal()     // Catch:{ NoSuchFieldError -> 0x0056 }
                r7[r8] = r6     // Catch:{ NoSuchFieldError -> 0x0056 }
            L_0x0056:
                r7 = 8
                int[] r8 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0062 }
                com.android.internal.telephony.IccCardConstants$State r9 = com.android.internal.telephony.IccCardConstants.State.UNKNOWN     // Catch:{ NoSuchFieldError -> 0x0062 }
                int r9 = r9.ordinal()     // Catch:{ NoSuchFieldError -> 0x0062 }
                r8[r9] = r7     // Catch:{ NoSuchFieldError -> 0x0062 }
            L_0x0062:
                r8 = 9
                int[] r9 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x006e }
                com.android.internal.telephony.IccCardConstants$State r10 = com.android.internal.telephony.IccCardConstants.State.CARD_IO_ERROR     // Catch:{ NoSuchFieldError -> 0x006e }
                int r10 = r10.ordinal()     // Catch:{ NoSuchFieldError -> 0x006e }
                r9[r10] = r8     // Catch:{ NoSuchFieldError -> 0x006e }
            L_0x006e:
                com.android.keyguard.CarrierTextController$StatusMode[] r9 = com.android.keyguard.CarrierTextController.StatusMode.values()
                int r9 = r9.length
                int[] r9 = new int[r9]
                $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode = r9
                int[] r9 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x0081 }
                com.android.keyguard.CarrierTextController$StatusMode r10 = com.android.keyguard.CarrierTextController.StatusMode.Normal     // Catch:{ NoSuchFieldError -> 0x0081 }
                int r10 = r10.ordinal()     // Catch:{ NoSuchFieldError -> 0x0081 }
                r9[r10] = r0     // Catch:{ NoSuchFieldError -> 0x0081 }
            L_0x0081:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x008b }
                com.android.keyguard.CarrierTextController$StatusMode r9 = com.android.keyguard.CarrierTextController.StatusMode.SimNotReady     // Catch:{ NoSuchFieldError -> 0x008b }
                int r9 = r9.ordinal()     // Catch:{ NoSuchFieldError -> 0x008b }
                r0[r9] = r1     // Catch:{ NoSuchFieldError -> 0x008b }
            L_0x008b:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x0095 }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.NetworkLocked     // Catch:{ NoSuchFieldError -> 0x0095 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0095 }
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0095 }
            L_0x0095:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x009f }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimMissing     // Catch:{ NoSuchFieldError -> 0x009f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x009f }
                r0[r1] = r3     // Catch:{ NoSuchFieldError -> 0x009f }
            L_0x009f:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00a9 }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimPermDisabled     // Catch:{ NoSuchFieldError -> 0x00a9 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00a9 }
                r0[r1] = r4     // Catch:{ NoSuchFieldError -> 0x00a9 }
            L_0x00a9:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00b3 }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimMissingLocked     // Catch:{ NoSuchFieldError -> 0x00b3 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00b3 }
                r0[r1] = r5     // Catch:{ NoSuchFieldError -> 0x00b3 }
            L_0x00b3:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00bd }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimLocked     // Catch:{ NoSuchFieldError -> 0x00bd }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00bd }
                r0[r1] = r6     // Catch:{ NoSuchFieldError -> 0x00bd }
            L_0x00bd:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00c7 }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimPukLocked     // Catch:{ NoSuchFieldError -> 0x00c7 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00c7 }
                r0[r1] = r7     // Catch:{ NoSuchFieldError -> 0x00c7 }
            L_0x00c7:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00d1 }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimIoError     // Catch:{ NoSuchFieldError -> 0x00d1 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00d1 }
                r0[r1] = r8     // Catch:{ NoSuchFieldError -> 0x00d1 }
            L_0x00d1:
                int[] r0 = $SwitchMap$com$android$keyguard$CarrierTextController$StatusMode     // Catch:{ NoSuchFieldError -> 0x00dd }
                com.android.keyguard.CarrierTextController$StatusMode r1 = com.android.keyguard.CarrierTextController.StatusMode.SimUnknown     // Catch:{ NoSuchFieldError -> 0x00dd }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x00dd }
                r2 = 10
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x00dd }
            L_0x00dd:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.CarrierTextController.C05524.<clinit>():void");
        }
    }

    public interface CarrierTextCallback {
        void finishedWakingUp() {
        }

        void startedGoingToSleep() {
        }

        void updateCarrierInfo(CarrierTextCallbackInfo carrierTextCallbackInfo) {
        }
    }

    public static final class CarrierTextCallbackInfo {
        public boolean airplaneMode;
        public final boolean anySimReady;
        public final CharSequence carrierText;
        public final CharSequence[] listOfCarriers;
        public final int[] subscriptionIds;

        public CarrierTextCallbackInfo(CharSequence charSequence, CharSequence[] charSequenceArr, boolean z, int[] iArr) {
            this(charSequence, charSequenceArr, z, iArr, false);
        }

        public CarrierTextCallbackInfo(CharSequence charSequence, CharSequence[] charSequenceArr, boolean z, int[] iArr, boolean z2) {
            this.carrierText = charSequence;
            this.listOfCarriers = charSequenceArr;
            this.anySimReady = z;
            this.subscriptionIds = iArr;
            this.airplaneMode = z2;
        }
    }

    private enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady,
        SimIoError,
        SimUnknown
    }

    public CarrierTextController(Context context, CharSequence charSequence, boolean z, boolean z2) {
        super(context, charSequence, z, z2);
        this.mContext = context;
        this.mIsEmergencyCallCapable = context.getResources().getBoolean(17891576);
        this.mShowAirplaneMode = z;
        this.mShowMissingSim = z2;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mSeparator = charSequence;
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mSimSlotsNumber = ((TelephonyManager) context.getSystemService("phone")).getPhoneCount();
        this.mSimErrorState = new boolean[this.mSimSlotsNumber];
        updateDisplayOpportunisticSubscriptionCarrierText(SystemProperties.getBoolean("persist.radio.display_opportunistic_carrier", false));
    }

    private CharSequence updateCarrierTextWithSimIoError(CharSequence charSequence, CharSequence[] charSequenceArr, int[] iArr, boolean z) {
        CharSequence carrierTextForSimState = getCarrierTextForSimState(State.CARD_IO_ERROR, "");
        int i = 0;
        while (true) {
            boolean[] zArr = this.mSimErrorState;
            if (i >= zArr.length) {
                return charSequence;
            }
            if (zArr[i]) {
                if (z) {
                    return concatenate(carrierTextForSimState, getContext().getText(17039911), this.mSeparator);
                }
                if (iArr[i] != -1) {
                    int i2 = iArr[i];
                    charSequenceArr[i2] = concatenate(carrierTextForSimState, charSequenceArr[i2], this.mSeparator);
                } else {
                    charSequence = concatenate(charSequence, carrierTextForSimState, this.mSeparator);
                }
            }
            i++;
        }
    }

    public void setListening(CarrierTextCallback carrierTextCallback) {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (carrierTextCallback != null) {
            this.mCarrierTextCallback = carrierTextCallback;
            if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
                this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
                this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
                this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
                telephonyManager.listen(this.mPhoneStateListener, 4194304);
                return;
            }
            this.mKeyguardUpdateMonitor = null;
            carrierTextCallback.updateCarrierInfo(new CarrierTextCallbackInfo("", null, false, null));
            return;
        }
        this.mCarrierTextCallback = null;
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.removeCallback(this.mCallback);
            this.mWakefulnessLifecycle.removeObserver(this.mWakefulnessObserver);
        }
        telephonyManager.listen(this.mPhoneStateListener, 0);
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

    public void updateDisplayOpportunisticSubscriptionCarrierText(boolean z) {
        this.mDisplayOpportunisticSubscriptionCarrierText = z;
    }

    /* access modifiers changed from: protected */
    public List<SubscriptionInfo> getSubscriptionInfo() {
        if (!this.mDisplayOpportunisticSubscriptionCarrierText) {
            return this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        }
        List<SubscriptionInfo> activeSubscriptionInfoList = ((SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service")).getActiveSubscriptionInfoList(false);
        if (activeSubscriptionInfoList == null) {
            return new ArrayList();
        }
        filterMobileSubscriptionInSameGroup(activeSubscriptionInfoList);
        return activeSubscriptionInfoList;
    }

    /* access modifiers changed from: protected */
    public void updateCarrierText() {
        String str;
        boolean z;
        CharSequence updateCarrierTextWithSimIoError;
        boolean z2;
        CharSequence charSequence;
        getContext().getResources().getBoolean(R$bool.config_show_customize_carrier_name);
        List subscriptionInfo = getSubscriptionInfo();
        int size = subscriptionInfo.size();
        int[] iArr = new int[size];
        int[] iArr2 = new int[this.mSimSlotsNumber];
        for (int i = 0; i < this.mSimSlotsNumber; i++) {
            iArr2[i] = -1;
        }
        CharSequence[] charSequenceArr = new CharSequence[size];
        String str2 = "CarrierTextController";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateCarrierText() showCustomizeName: ");
            sb.append(true);
            sb.append(" numSubs: ");
            sb.append(size);
            Log.d(str2, sb.toString());
        }
        boolean z3 = true;
        int i2 = 0;
        boolean z4 = false;
        while (true) {
            str = "";
            if (i2 >= size) {
                break;
            }
            int subscriptionId = ((SubscriptionInfo) subscriptionInfo.get(i2)).getSubscriptionId();
            charSequenceArr[i2] = str;
            iArr[i2] = subscriptionId;
            iArr2[((SubscriptionInfo) subscriptionInfo.get(i2)).getSimSlotIndex()] = i2;
            State simState = this.mKeyguardUpdateMonitor.getSimState(subscriptionId);
            String customizeCarrierName = getCustomizeCarrierName(opGetCarrierName(subscriptionId, ((SubscriptionInfo) subscriptionInfo.get(i2)).getCarrierName()), (SubscriptionInfo) subscriptionInfo.get(i2));
            CharSequence carrierTextForSimState = getCarrierTextForSimState(simState, customizeCarrierName);
            if (DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Handling (subId=");
                sb2.append(subscriptionId);
                sb2.append("): ");
                sb2.append(simState);
                sb2.append(" ");
                sb2.append(customizeCarrierName);
                Log.d(str2, sb2.toString());
            }
            if (carrierTextForSimState != null) {
                charSequenceArr[i2] = carrierTextForSimState;
                z3 = false;
            }
            if (simState == State.READY) {
                ServiceState serviceState = (ServiceState) this.mKeyguardUpdateMonitor.mServiceStates.get(Integer.valueOf(subscriptionId));
                if (!(serviceState == null || serviceState.getDataRegState() != 0 || (serviceState.getRilDataRadioTechnology() == 18 && (!this.mWifiManager.isWifiEnabled() || this.mWifiManager.getConnectionInfo() == null || this.mWifiManager.getConnectionInfo().getBSSID() == null)))) {
                    if (DEBUG) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("SIM ready and in service: subId=");
                        sb3.append(subscriptionId);
                        sb3.append(", ss=");
                        sb3.append(serviceState);
                        Log.d(str2, sb3.toString());
                    }
                    z4 = true;
                }
            }
            i2++;
        }
        CharSequence charSequence2 = null;
        if (z3) {
            if (size != 0) {
                charSequence2 = makeCarrierStringOnEmergencyCapable(getMissingSimMessage(), ((SubscriptionInfo) subscriptionInfo.get(0)).getCarrierName());
            } else {
                CharSequence text = getContext().getText(17039911);
                Intent registerReceiver = getContext().registerReceiver(null, new IntentFilter("android.provider.Telephony.SPN_STRINGS_UPDATED"));
                if (registerReceiver != null) {
                    z = false;
                    String stringExtra = registerReceiver.getBooleanExtra("showSpn", false) ? registerReceiver.getStringExtra("spn") : str;
                    if (registerReceiver.getBooleanExtra("showPlmn", false)) {
                        str = registerReceiver.getStringExtra("plmn");
                    }
                    if (DEBUG) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("Getting plmn/spn sticky brdcst ");
                        sb4.append(str);
                        sb4.append("/");
                        sb4.append(stringExtra);
                        Log.d(str2, sb4.toString());
                    }
                    if (Objects.equals(str, stringExtra)) {
                        charSequence = str;
                    } else {
                        charSequence = concatenate(str, stringExtra, this.mSeparator);
                    }
                } else {
                    z = false;
                    charSequence = text;
                }
                charSequence2 = makeCarrierStringOnEmergencyCapable(getMissingSimMessage(), charSequence);
                updateCarrierTextWithSimIoError = updateCarrierTextWithSimIoError(charSequence2, charSequenceArr, iArr2, z3);
                if (!z4 || !WirelessUtils.isAirplaneModeOn(this.mContext)) {
                    z2 = z;
                } else {
                    updateCarrierTextWithSimIoError = getAirplaneModeMessage();
                    z2 = true;
                }
                if (TextUtils.isEmpty(updateCarrierTextWithSimIoError) && !z2) {
                    updateCarrierTextWithSimIoError = joinNotEmpty(this.mSeparator, charSequenceArr);
                }
                CarrierTextCallbackInfo carrierTextCallbackInfo = new CarrierTextCallbackInfo(updateCarrierTextWithSimIoError, charSequenceArr, !z3, iArr, z2);
                postToCallback(carrierTextCallbackInfo);
            }
        }
        z = false;
        updateCarrierTextWithSimIoError = updateCarrierTextWithSimIoError(charSequence2, charSequenceArr, iArr2, z3);
        if (!z4) {
        }
        z2 = z;
        updateCarrierTextWithSimIoError = joinNotEmpty(this.mSeparator, charSequenceArr);
        CarrierTextCallbackInfo carrierTextCallbackInfo2 = new CarrierTextCallbackInfo(updateCarrierTextWithSimIoError, charSequenceArr, !z3, iArr, z2);
        postToCallback(carrierTextCallbackInfo2);
    }

    /* access modifiers changed from: protected */
    public void postToCallback(CarrierTextCallbackInfo carrierTextCallbackInfo) {
        Handler handler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        CarrierTextCallback carrierTextCallback = this.mCarrierTextCallback;
        if (carrierTextCallback != null) {
            handler.post(new Runnable(carrierTextCallbackInfo) {
                private final /* synthetic */ CarrierTextCallbackInfo f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    CarrierTextCallback.this.updateCarrierInfo(this.f$1);
                }
            });
        }
    }

    private Context getContext() {
        return this.mContext;
    }

    private String getMissingSimMessage() {
        return (!this.mShowMissingSim || !this.mTelephonyCapable) ? "" : getContext().getString(R$string.keyguard_missing_sim_message_short);
    }

    private String getAirplaneModeMessage() {
        return this.mShowAirplaneMode ? getContext().getString(R$string.airplane_mode) : "";
    }

    private CharSequence getCarrierTextForSimState(State state, CharSequence charSequence) {
        switch (C05524.$SwitchMap$com$android$keyguard$CarrierTextController$StatusMode[getStatusForIccState(state).ordinal()]) {
            case 1:
                return charSequence;
            case 2:
                return "";
            case 3:
                return makeCarrierStringOnEmergencyCapable(this.mContext.getText(R$string.keyguard_network_locked_message), charSequence);
            case 5:
                return makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_permanent_disabled_sim_message_short), charSequence);
            case 7:
                return makeCarrierStringOnLocked(getContext().getText(R$string.keyguard_sim_locked_message), charSequence);
            case 8:
                return makeCarrierStringOnLocked(getContext().getText(R$string.keyguard_sim_puk_locked_message), charSequence);
            case 9:
                return makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_error_message_short), charSequence);
            default:
                return null;
        }
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence charSequence, CharSequence charSequence2) {
        return opMakeCarrierStringOnEmergencyCapable(charSequence, charSequence2);
    }

    private CharSequence makeCarrierStringOnLocked(CharSequence charSequence, CharSequence charSequence2) {
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            return this.mContext.getString(R$string.keyguard_carrier_name_with_sim_locked_template, new Object[]{charSequence2, charSequence});
        } else if (z) {
            return charSequence;
        } else {
            return z2 ? charSequence2 : "";
        }
    }

    /* access modifiers changed from: private */
    public StatusMode getStatusForIccState(State state) {
        if (state == null) {
            return StatusMode.Normal;
        }
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() && (state == State.ABSENT || state == State.PERM_DISABLED)) {
            state = State.NETWORK_LOCKED;
        }
        switch (C05524.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.SimMissingLocked;
            case 3:
                return StatusMode.SimNotReady;
            case 4:
                return StatusMode.SimLocked;
            case 5:
                return StatusMode.SimPukLocked;
            case 6:
                return StatusMode.Normal;
            case 7:
                return StatusMode.SimPermDisabled;
            case 8:
                return StatusMode.SimUnknown;
            case 9:
                return StatusMode.SimIoError;
            default:
                return StatusMode.SimUnknown;
        }
    }

    private static CharSequence concatenate(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return OpCarrierTextController.opConcatenate(charSequence, charSequence2, charSequence3);
    }

    private static CharSequence joinNotEmpty(CharSequence charSequence, CharSequence[] charSequenceArr) {
        int length = charSequenceArr.length;
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (!TextUtils.isEmpty(charSequenceArr[i])) {
                if (!TextUtils.isEmpty(sb)) {
                    sb.append(charSequence);
                }
                sb.append(charSequenceArr[i]);
            }
        }
        return sb.toString();
    }

    private String getCustomizeCarrierName(CharSequence charSequence, SubscriptionInfo subscriptionInfo) {
        new StringBuilder();
        networkClassToString(TelephonyManager.getNetworkClass(getNetworkType(subscriptionInfo.getSubscriptionId())));
        String str = get5GNetworkClass(subscriptionInfo);
        return opGetCustomizeCarrierName(charSequence, subscriptionInfo);
    }

    private int getNetworkType(int i) {
        ServiceState serviceState = (ServiceState) this.mKeyguardUpdateMonitor.mServiceStates.get(Integer.valueOf(i));
        if (serviceState == null || (serviceState.getDataRegState() != 0 && serviceState.getVoiceRegState() != 0)) {
            return 0;
        }
        int dataNetworkType = serviceState.getDataNetworkType();
        return dataNetworkType == 0 ? serviceState.getVoiceNetworkType() : dataNetworkType;
    }

    private String networkClassToString(int i) {
        int[] iArr = {R$string.config_rat_unknown, R$string.config_rat_2g, R$string.config_rat_3g, R$string.config_rat_4g};
        String string = i < iArr.length ? getContext().getResources().getString(iArr[i]) : null;
        return string == null ? "" : string;
    }

    private String get5GNetworkClass(SubscriptionInfo subscriptionInfo) {
        int simSlotIndex = subscriptionInfo.getSimSlotIndex();
        int subscriptionId = subscriptionInfo.getSubscriptionId();
        if (this.mFiveGServiceClient == null) {
            this.mFiveGServiceClient = FiveGServiceClient.getInstance(this.mContext);
            this.mFiveGServiceClient.registerCallback(this.mCallback);
        }
        if (!this.mFiveGServiceClient.getCurrentServiceState(simSlotIndex).isConnectedOnNsaMode() || !isDataRegisteredOnLte(subscriptionId)) {
            return null;
        }
        return this.mContext.getResources().getString(R$string.data_connection_5g);
    }

    private boolean isDataRegisteredOnLte(int i) {
        int dataNetworkType = ((TelephonyManager) this.mContext.getSystemService("phone")).getDataNetworkType(i);
        return dataNetworkType == 13 || dataNetworkType == 19;
    }
}
