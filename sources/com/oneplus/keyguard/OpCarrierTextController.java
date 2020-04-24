package com.oneplus.keyguard;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.keyguard.CarrierTextController;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R$string;
import com.android.systemui.R$array;
import com.android.systemui.statusbar.policy.FiveGServiceClient;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpCarrierTextController {
    private static final int[] SHOW_ROAMING_BANNER = {3, 134, 144, 154, 179, 180, 213, 214, 215, 130, 140, 150};
    private final CarrierConfigManager mCarrierConfigManager;
    private Context mContext;
    private FiveGServiceClient mFiveGServiceClient;
    private CharSequence mSeparator;
    private boolean mShowAirplaneMode;
    private boolean mShowMissingSim;
    private final TelephonyManager mTelephonyManager = ((TelephonyManager) this.mContext.getSystemService("phone"));
    private WifiManager mWifiManager;

    public OpCarrierTextController(Context context, CharSequence charSequence, boolean z, boolean z2) {
        this.mShowAirplaneMode = z;
        this.mShowMissingSim = z2;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mContext = context;
        this.mSeparator = charSequence;
        this.mCarrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
    }

    /* access modifiers changed from: protected */
    public String opGetCustomizeCarrierName(CharSequence charSequence, SubscriptionInfo subscriptionInfo) {
        StringBuilder sb = new StringBuilder();
        networkClassToString(TelephonyManager.getNetworkClass(getNetworkType(subscriptionInfo.getSubscriptionId())));
        if (this.mFiveGServiceClient == null) {
            this.mFiveGServiceClient = FiveGServiceClient.getInstance(this.mContext);
            this.mFiveGServiceClient.registerCallback(getKeyguardUpdateMonitorCallback());
        }
        if (this.mFiveGServiceClient.getCurrentServiceState(subscriptionInfo.getSimSlotIndex()).isConnectedOnNsaMode()) {
            this.mContext.getResources().getString(R$string.data_connection_5g);
        }
        if (!TextUtils.isEmpty(charSequence)) {
            String[] split = charSequence.toString().split(this.mSeparator.toString(), 2);
            for (int i = 0; i < split.length; i++) {
                split[i] = opGetLocalString(split[i], R$array.oneplus_origin_carrier_names, R$array.oneplus_locale_carrier_names);
                if (!TextUtils.isEmpty(split[i]) && (i <= 0 || !split[i].equals(split[i - 1]))) {
                    if (i > 0) {
                        sb.append(this.mSeparator);
                    }
                    sb.append(split[i]);
                }
            }
        }
        return sb.toString();
    }

    protected static CharSequence opConcatenate(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        String str;
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            if (TextUtils.isEmpty(charSequence3)) {
                StringBuilder sb = new StringBuilder();
                sb.append(charSequence);
                sb.append(" | ");
                sb.append(charSequence2);
                str = sb.toString();
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(charSequence);
                sb2.append(charSequence3);
                sb2.append(charSequence2);
                str = sb2.toString();
            }
            return str;
        } else if (z) {
            return charSequence;
        } else {
            return z2 ? charSequence2 : "";
        }
    }

    /* access modifiers changed from: protected */
    public CharSequence opMakeCarrierStringOnEmergencyCapable(CharSequence charSequence, CharSequence charSequence2) {
        return getIsEmergencyCallCapable() ? opConcatenate(charSequence, charSequence2, " - ") : charSequence;
    }

    /* access modifiers changed from: protected */
    public String opGetLocalString(String str, int i, int i2) {
        String[] stringArray = this.mContext.getResources().getStringArray(i);
        String[] stringArray2 = this.mContext.getResources().getStringArray(i2);
        int i3 = 0;
        while (true) {
            String str2 = "carrier\"";
            String str3 = "OpCarrierTextController";
            if (i3 >= stringArray.length) {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str2);
                    sb.append(str);
                    sb.append("\"doesn't exist local name");
                    Log.i(str3, sb.toString());
                }
                return str;
            } else if (stringArray[i3].equalsIgnoreCase(str)) {
                String str4 = "com.android.systemui";
                String str5 = "string";
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str2);
                    sb2.append(str);
                    sb2.append("\"exist local nameId:");
                    sb2.append(stringArray2[i3]);
                    sb2.append(" local name:");
                    Context context = this.mContext;
                    sb2.append(context.getString(context.getResources().getIdentifier(stringArray2[i3], str5, str4)));
                    Log.i(str3, sb2.toString());
                }
                Context context2 = this.mContext;
                return context2.getString(context2.getResources().getIdentifier(stringArray2[i3], str5, str4));
            } else {
                i3++;
            }
        }
    }

    /* access modifiers changed from: protected */
    public CharSequence opGetCarrierName(int i, CharSequence charSequence) {
        if (!OpUtils.isUSS()) {
            return charSequence;
        }
        ServiceState serviceState = (ServiceState) getKeyguardUpdateMonitor().opGetServiceStates().get(Integer.valueOf(i));
        if (serviceState == null) {
            return charSequence;
        }
        if (showUSSRoamingIndicator(serviceState)) {
            String eriText = getEriText(i);
            StringBuilder sb = new StringBuilder();
            sb.append("(getEriText=");
            sb.append(eriText);
            Log.d("OpCarrierTextController", sb.toString());
            return eriText;
        } else if (showRoaming(serviceState)) {
            return this.mContext.getResources().getString(R$string.sim_status_uss_network_roam);
        } else {
            return serviceState.getOperatorAlphaLong();
        }
    }

    private boolean showUSSRoamingIndicator(ServiceState serviceState) {
        boolean z;
        boolean z2;
        if (serviceState == null || this.mCarrierConfigManager == null) {
            return false;
        }
        boolean roaming = serviceState.getRoaming();
        PersistableBundle config = this.mCarrierConfigManager.getConfig();
        String str = "OpCarrierTextController";
        if (config != null) {
            String string = config.getString("carrier_eri_file_name_string");
            z = !TextUtils.equals(string, "eri.xml");
            StringBuilder sb = new StringBuilder();
            sb.append("showUSSRoamingIndicator eriFile:");
            sb.append(string);
            Log.i(str, sb.toString());
        } else {
            z = false;
        }
        int cdmaRoamingIndicator = serviceState.getCdmaRoamingIndicator();
        int i = 0;
        while (true) {
            int[] iArr = SHOW_ROAMING_BANNER;
            if (i >= iArr.length) {
                z2 = false;
                break;
            } else if (cdmaRoamingIndicator == iArr[i]) {
                z2 = true;
                break;
            } else {
                i++;
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("showUSSRoamingIndicator isRoaming:");
        sb2.append(roaming);
        sb2.append(" isSpecifyEriInfoCard:");
        sb2.append(z);
        sb2.append(" isShowRoamingbanner:");
        sb2.append(z2);
        sb2.append(" romaingIndicator:");
        sb2.append(cdmaRoamingIndicator);
        Log.i(str, sb2.toString());
        if (!roaming || !z || !z2) {
            return false;
        }
        return true;
    }

    private String getEriText(int i) {
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager == null) {
            return "";
        }
        return telephonyManager.getCdmaEriText(i);
    }

    private boolean showRoaming(ServiceState serviceState) {
        if (serviceState == null) {
            return false;
        }
        Context context = this.mContext;
        if (context != null && OpUtils.isSprintMccMnc(context) && isUssCDMA() && serviceState.getVoiceRoaming()) {
            return true;
        }
        return false;
    }

    private boolean isUssCDMA() {
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager != null) {
            int voiceNetworkType = telephonyManager.getVoiceNetworkType();
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("isUssCDMA getType = ");
                sb.append(voiceNetworkType);
                Log.d("OpCarrierTextController", sb.toString());
            }
            if (voiceNetworkType == 4 || voiceNetworkType == 5 || voiceNetworkType == 6 || voiceNetworkType == 7 || voiceNetworkType == 12 || voiceNetworkType == 14) {
                return true;
            }
        }
        return false;
    }

    private boolean getIsEmergencyCallCapable() {
        return ((Boolean) OpReflectionUtils.getValue(CarrierTextController.class, this, "mIsEmergencyCallCapable")).booleanValue();
    }

    private KeyguardUpdateMonitorCallback getKeyguardUpdateMonitorCallback() {
        return (KeyguardUpdateMonitorCallback) OpReflectionUtils.getValue(CarrierTextController.class, this, "mCallback");
    }

    private int getNetworkType(int i) {
        return ((Integer) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(CarrierTextController.class, "getNetworkType", Integer.TYPE), Integer.valueOf(i))).intValue();
    }

    private String networkClassToString(int i) {
        return (String) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(CarrierTextController.class, "networkClassToString", Integer.TYPE), Integer.valueOf(i));
    }

    private KeyguardUpdateMonitor getKeyguardUpdateMonitor() {
        return (KeyguardUpdateMonitor) OpReflectionUtils.getValue(CarrierTextController.class, this, "mKeyguardUpdateMonitor");
    }
}
