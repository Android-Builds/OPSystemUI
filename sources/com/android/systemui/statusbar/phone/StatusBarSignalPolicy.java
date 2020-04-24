package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.R$bool;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;

public class StatusBarSignalPolicy implements SignalCallback, SecurityControllerCallback, Tunable {
    static final boolean OP_DEBUG = Build.DEBUG_ONEPLUS;
    private boolean mActivityEnabled;
    private String mBlackList;
    private boolean mBlockAirplane;
    private boolean mBlockEthernet;
    private boolean[] mBlockLTE;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private boolean mCTA;
    private final Context mContext;
    private IExtTelephony mExtTelephony;
    private boolean mForceBlockWifi;
    private final Handler mHandler = Handler.getMain();
    private final StatusBarIconController mIconController;
    private boolean mIsAirplaneMode;
    private boolean mIsVoLteEnable;
    private boolean mIsVoWifiEnable;
    private List<LTEIconState> mLTEIconStates;
    private ArrayList<MobileIconState> mMobileStates;
    private final NetworkController mNetworkController;
    private int[] mProvisionState;
    private final SecurityController mSecurityController;
    private boolean mShowNoSim;
    private final String mSlotAirplane;
    private final String mSlotEthernet;
    private final String mSlotMobile;
    private final String mSlotVpn;
    private final String mSlotWifi;
    private WifiIconState mWifiIconState;
    private boolean mWifiVisible;

    public static class LTEIconState {
        public String contentDescription;
        public int imsIndex = 0;
        public int resId;
        public String slot;
        public boolean visible;

        public LTEIconState(String str) {
            this.slot = str;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (obj != null && LTEIconState.class == obj.getClass()) {
                if (!super.equals(obj)) {
                    return false;
                }
                LTEIconState lTEIconState = (LTEIconState) obj;
                if (this.resId == lTEIconState.resId && this.slot == lTEIconState.slot && this.contentDescription == lTEIconState.contentDescription) {
                    z = true;
                }
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(super.hashCode()), Integer.valueOf(this.resId), this.slot, this.contentDescription});
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("LTEIconState(slot=");
            sb.append(this.slot);
            sb.append(", visible=");
            sb.append(this.visible);
            sb.append(", resId=");
            sb.append(this.resId);
            sb.append(", imsIndex:");
            sb.append(this.imsIndex);
            sb.append(", contentDescription");
            sb.append(this.contentDescription);
            sb.append(")");
            return sb.toString();
        }
    }

    public static class MobileIconState extends SignalIconState {
        public boolean dataConnected;
        public boolean needsLeadingPadding;
        public int phoneId;
        public boolean roaming;
        public boolean showNoSim;
        public int stackedDataStrengthId;
        public int stackedDataTypeId;
        public int stackedVoiceStrengthId;
        public int stackedVoiceTypeId;
        public int strengthId;
        public int subId;
        public String typeContentDescription;
        public int typeId;
        public int volteId;

        private MobileIconState(int i) {
            super();
            this.stackedDataStrengthId = 0;
            this.stackedVoiceStrengthId = 0;
            this.stackedDataTypeId = 0;
            this.stackedVoiceTypeId = 0;
            this.dataConnected = false;
            this.phoneId = 0;
            this.subId = i;
            if (i < 0) {
                this.showNoSim = true;
                this.visible = true;
            }
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (obj != null && MobileIconState.class == obj.getClass()) {
                if (!super.equals(obj)) {
                    return false;
                }
                MobileIconState mobileIconState = (MobileIconState) obj;
                if (this.subId == mobileIconState.subId && this.strengthId == mobileIconState.strengthId && this.typeId == mobileIconState.typeId && this.roaming == mobileIconState.roaming && this.stackedDataTypeId == mobileIconState.stackedDataTypeId && this.stackedVoiceTypeId == mobileIconState.stackedVoiceTypeId && this.stackedDataStrengthId == mobileIconState.stackedDataStrengthId && this.stackedVoiceStrengthId == mobileIconState.stackedVoiceStrengthId && this.dataConnected == mobileIconState.dataConnected && this.showNoSim == mobileIconState.showNoSim && this.phoneId == mobileIconState.phoneId && this.needsLeadingPadding == mobileIconState.needsLeadingPadding && Objects.equals(this.typeContentDescription, mobileIconState.typeContentDescription) && this.volteId == mobileIconState.volteId) {
                    z = true;
                }
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(super.hashCode()), Integer.valueOf(this.subId), Integer.valueOf(this.strengthId), Integer.valueOf(this.typeId), Boolean.valueOf(this.roaming), Boolean.valueOf(this.needsLeadingPadding), this.typeContentDescription});
        }

        public MobileIconState copy() {
            MobileIconState mobileIconState = new MobileIconState(this.subId);
            copyTo(mobileIconState);
            return mobileIconState;
        }

        public void copyTo(MobileIconState mobileIconState) {
            super.copyTo(mobileIconState);
            mobileIconState.subId = this.subId;
            mobileIconState.strengthId = this.strengthId;
            mobileIconState.typeId = this.typeId;
            mobileIconState.roaming = this.roaming;
            mobileIconState.needsLeadingPadding = this.needsLeadingPadding;
            mobileIconState.typeContentDescription = this.typeContentDescription;
            mobileIconState.volteId = this.volteId;
            mobileIconState.stackedDataStrengthId = this.stackedDataStrengthId;
            mobileIconState.stackedVoiceStrengthId = this.stackedVoiceStrengthId;
            mobileIconState.stackedDataTypeId = this.stackedDataTypeId;
            mobileIconState.stackedVoiceTypeId = this.stackedVoiceTypeId;
            mobileIconState.dataConnected = this.dataConnected;
            mobileIconState.showNoSim = this.showNoSim;
            mobileIconState.phoneId = this.phoneId;
        }

        /* access modifiers changed from: private */
        public static List<MobileIconState> copyStates(List<MobileIconState> list) {
            ArrayList arrayList = new ArrayList();
            for (MobileIconState mobileIconState : list) {
                MobileIconState mobileIconState2 = new MobileIconState(mobileIconState.subId);
                mobileIconState.copyTo(mobileIconState2);
                arrayList.add(mobileIconState2);
            }
            return arrayList;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MobileIconState(subId=");
            sb.append(this.subId);
            sb.append(", strengthId=");
            sb.append(this.strengthId);
            sb.append(", roaming=");
            sb.append(this.roaming);
            sb.append(", typeId=");
            sb.append(this.typeId);
            sb.append(", volteId=");
            sb.append(this.volteId);
            sb.append(", visible=");
            sb.append(this.visible);
            sb.append(")");
            return sb.toString();
        }

        public boolean isAbsent() {
            return this.subId < 0;
        }
    }

    private static abstract class SignalIconState {
        public boolean activityIn;
        public boolean activityOut;
        public String contentDescription;
        public String slot;
        public boolean visible;

        private SignalIconState() {
        }

        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SignalIconState signalIconState = (SignalIconState) obj;
            if (this.visible == signalIconState.visible && this.activityOut == signalIconState.activityOut && this.activityIn == signalIconState.activityIn && Objects.equals(this.contentDescription, signalIconState.contentDescription) && Objects.equals(this.slot, signalIconState.slot)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Boolean.valueOf(this.visible), Boolean.valueOf(this.activityOut), this.slot});
        }

        /* access modifiers changed from: protected */
        public void copyTo(SignalIconState signalIconState) {
            signalIconState.visible = this.visible;
            signalIconState.activityIn = this.activityIn;
            signalIconState.activityOut = this.activityOut;
            signalIconState.slot = this.slot;
            signalIconState.contentDescription = this.contentDescription;
        }
    }

    public static class WifiIconState extends SignalIconState {
        public boolean airplaneSpacerVisible;
        public int resId;
        public boolean signalSpacerVisible;

        public WifiIconState() {
            super();
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (obj != null && WifiIconState.class == obj.getClass()) {
                if (!super.equals(obj)) {
                    return false;
                }
                WifiIconState wifiIconState = (WifiIconState) obj;
                if (this.resId == wifiIconState.resId && this.airplaneSpacerVisible == wifiIconState.airplaneSpacerVisible && this.signalSpacerVisible == wifiIconState.signalSpacerVisible) {
                    z = true;
                }
            }
            return z;
        }

        public void copyTo(WifiIconState wifiIconState) {
            super.copyTo(wifiIconState);
            wifiIconState.resId = this.resId;
            wifiIconState.airplaneSpacerVisible = this.airplaneSpacerVisible;
            wifiIconState.signalSpacerVisible = this.signalSpacerVisible;
        }

        public WifiIconState copy() {
            WifiIconState wifiIconState = new WifiIconState();
            copyTo(wifiIconState);
            return wifiIconState;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(super.hashCode()), Integer.valueOf(this.resId), Boolean.valueOf(this.airplaneSpacerVisible), Boolean.valueOf(this.signalSpacerVisible)});
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("WifiIconState(resId=");
            sb.append(this.resId);
            sb.append(", visible=");
            sb.append(this.visible);
            sb.append(")");
            return sb.toString();
        }
    }

    public void setMobileDataEnabled(boolean z) {
    }

    public void setNoSims(boolean z, boolean z2) {
    }

    public StatusBarSignalPolicy(Context context, StatusBarIconController statusBarIconController) {
        boolean z = false;
        this.mIsAirplaneMode = false;
        this.mWifiVisible = false;
        this.mMobileStates = new ArrayList<>();
        this.mWifiIconState = new WifiIconState();
        this.mShowNoSim = true;
        this.mCTA = false;
        this.mIsVoWifiEnable = false;
        this.mIsVoLteEnable = false;
        this.mLTEIconStates = new ArrayList();
        this.mBlockLTE = new boolean[]{false, false, false, false, false, false};
        this.mBlackList = null;
        this.mProvisionState = new int[]{1, 1};
        this.mContext = context;
        this.mSlotAirplane = this.mContext.getString(17041079);
        this.mSlotMobile = this.mContext.getString(17041096);
        this.mSlotWifi = this.mContext.getString(17041113);
        this.mSlotEthernet = this.mContext.getString(17041089);
        this.mSlotVpn = this.mContext.getString(17041112);
        this.mActivityEnabled = this.mContext.getResources().getBoolean(R$bool.config_showActivity);
        this.mCTA = OpUtils.hasCtaFeature(context);
        if (this.mShowNoSim || this.mCTA) {
            z = true;
        }
        this.mShowNoSim = z;
        initProvistionState();
        this.mIconController = statusBarIconController;
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
        this.mNetworkController.addCallback(this);
        this.mSecurityController.addCallback(this);
    }

    private void initProvistionState() {
        Log.i("StatusBarSignalPolicy", "init provision");
        int i = 1;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            i = -1;
        }
        int i2 = 0;
        while (true) {
            int[] iArr = this.mProvisionState;
            if (i2 < iArr.length) {
                iArr[i2] = getSlotProvisionStatus(i2, i);
                i2++;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateVpn() {
        boolean isVpnEnabled = this.mSecurityController.isVpnEnabled();
        this.mIconController.setIcon(this.mSlotVpn, currentVpnIconId(this.mSecurityController.isVpnBranded()), this.mContext.getResources().getString(R$string.accessibility_vpn_on));
        this.mIconController.setIconVisibility(this.mSlotVpn, isVpnEnabled);
    }

    private int currentVpnIconId(boolean z) {
        return z ? R$drawable.stat_sys_branded_vpn : R$drawable.stat_sys_vpn_ic;
    }

    public void onStateChanged() {
        this.mHandler.post(new Runnable() {
            public final void run() {
                StatusBarSignalPolicy.this.updateVpn();
            }
        });
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0073, code lost:
        if (r9 == r4[2]) goto L_0x0097;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTuningChanged(java.lang.String r9, java.lang.String r10) {
        /*
            r8 = this;
            java.lang.String r0 = "icon_blacklist"
            boolean r9 = r0.equals(r9)
            if (r9 != 0) goto L_0x0009
            return
        L_0x0009:
            boolean r9 = OP_DEBUG
            if (r9 == 0) goto L_0x002d
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r0 = "onTuningChanged / newValue:"
            r9.append(r0)
            r9.append(r10)
            java.lang.String r0 = " / mBlackList:"
            r9.append(r0)
            java.lang.String r0 = r8.mBlackList
            r9.append(r0)
            java.lang.String r9 = r9.toString()
            java.lang.String r0 = "StatusBarSignalPolicy"
            android.util.Log.i(r0, r9)
        L_0x002d:
            r8.mBlackList = r10
            android.util.ArraySet r9 = com.android.systemui.statusbar.phone.StatusBarIconController.getIconBlacklist(r10)
            java.lang.String r10 = r8.mSlotAirplane
            boolean r10 = r9.contains(r10)
            java.lang.String r0 = r8.mSlotMobile
            boolean r0 = r9.contains(r0)
            java.lang.String r1 = r8.mSlotWifi
            boolean r1 = r9.contains(r1)
            java.lang.String r2 = r8.mSlotEthernet
            boolean r2 = r9.contains(r2)
            java.lang.String r3 = "volte"
            boolean r3 = r9.contains(r3)
            java.lang.String r4 = "vowifi"
            boolean r9 = r9.contains(r4)
            boolean r4 = r8.mBlockAirplane
            r5 = 2
            r6 = 0
            if (r10 != r4) goto L_0x0075
            boolean r4 = r8.mBlockMobile
            if (r0 != r4) goto L_0x0075
            boolean r4 = r8.mBlockEthernet
            if (r2 != r4) goto L_0x0075
            boolean r4 = r8.mBlockWifi
            if (r1 != r4) goto L_0x0075
            boolean[] r4 = r8.mBlockLTE
            boolean r7 = r4[r6]
            if (r3 != r7) goto L_0x0075
            boolean r4 = r4[r5]
            if (r9 == r4) goto L_0x0097
        L_0x0075:
            r8.mBlockAirplane = r10
            r8.mBlockMobile = r0
            r8.mBlockEthernet = r2
            if (r1 != 0) goto L_0x0084
            boolean r10 = r8.mForceBlockWifi
            if (r10 == 0) goto L_0x0082
            goto L_0x0084
        L_0x0082:
            r10 = r6
            goto L_0x0085
        L_0x0084:
            r10 = 1
        L_0x0085:
            r8.mBlockWifi = r10
            boolean[] r10 = r8.mBlockLTE
            r10[r6] = r3
            r10[r5] = r9
            com.android.systemui.statusbar.policy.NetworkController r9 = r8.mNetworkController
            r9.removeCallback(r8)
            com.android.systemui.statusbar.policy.NetworkController r9 = r8.mNetworkController
            r9.addCallback(r8)
        L_0x0097:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBarSignalPolicy.onTuningChanged(java.lang.String, java.lang.String):void");
    }

    public void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
        boolean z5 = true;
        boolean z6 = iconState.visible && !this.mBlockWifi;
        boolean z7 = z2 && this.mActivityEnabled && z6;
        boolean z8 = z3 && this.mActivityEnabled && z6;
        WifiIconState copy = this.mWifiIconState.copy();
        copy.visible = z6;
        copy.resId = iconState.icon;
        copy.activityIn = z7;
        copy.activityOut = z8;
        copy.slot = this.mSlotWifi;
        copy.airplaneSpacerVisible = this.mIsAirplaneMode;
        copy.contentDescription = iconState.contentDescription;
        MobileIconState firstMobileState = getFirstMobileState();
        if (firstMobileState == null || (firstMobileState.typeId == 0 && firstMobileState.stackedDataTypeId == 0 && firstMobileState.stackedVoiceTypeId == 0 && !firstMobileState.showNoSim)) {
            z5 = false;
        }
        copy.signalSpacerVisible = z5;
        updateWifiIconWithState(copy);
        this.mWifiIconState = copy;
    }

    private void updateShowWifiSignalSpacer(WifiIconState wifiIconState) {
        MobileIconState firstMobileState = getFirstMobileState();
        wifiIconState.signalSpacerVisible = (firstMobileState == null || (firstMobileState.typeId == 0 && firstMobileState.stackedDataTypeId == 0 && firstMobileState.stackedVoiceTypeId == 0 && !firstMobileState.showNoSim)) ? false : true;
    }

    private void updateWifiIconWithState(WifiIconState wifiIconState) {
        if (!wifiIconState.visible || wifiIconState.resId <= 0) {
            this.mIconController.setIconVisibility(this.mSlotWifi, false);
            return;
        }
        this.mIconController.setSignalIcon(this.mSlotWifi, wifiIconState);
        this.mIconController.setIconVisibility(this.mSlotWifi, true);
    }

    public void setMobileDataIndicators(IconState iconState, IconState iconState2, int i, int i2, boolean z, boolean z2, int i3, int[] iArr, int[] iArr2, String str, String str2, boolean z3, int i4, boolean z4, boolean z5) {
        MobileIconState state = getState(i4);
        if (state == null) {
            Log.i("StatusBarSignalPolicy", "setMobileDataIndicators / state == null");
            return;
        }
        int i5 = state.typeId;
        boolean z6 = i != i5 && (i == 0 || i5 == 0);
        state.visible = iconState.visible && !this.mBlockMobile;
        state.strengthId = iconState.icon;
        state.typeId = i;
        state.contentDescription = iconState.contentDescription;
        state.typeContentDescription = str;
        state.roaming = z4;
        state.activityIn = z && this.mActivityEnabled;
        state.activityOut = z2 && this.mActivityEnabled;
        state.stackedDataStrengthId = iArr[0];
        state.stackedVoiceStrengthId = iArr2[0];
        state.stackedDataTypeId = iArr[1];
        state.stackedVoiceTypeId = iArr2[1];
        state.dataConnected = z5;
        this.mIconController.setMobileIcons(this.mSlotMobile, MobileIconState.copyStates(this.mMobileStates));
        if (z6) {
            WifiIconState copy = this.mWifiIconState.copy();
            updateShowWifiSignalSpacer(copy);
            if (!Objects.equals(copy, this.mWifiIconState)) {
                updateWifiIconWithState(copy);
                this.mWifiIconState = copy;
            }
        }
    }

    private MobileIconState getState(int i) {
        Iterator it = this.mMobileStates.iterator();
        while (it.hasNext()) {
            MobileIconState mobileIconState = (MobileIconState) it.next();
            if (mobileIconState.subId == i) {
                return mobileIconState;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected subscription ");
        sb.append(i);
        Log.e("StatusBarSignalPolicy", sb.toString());
        return null;
    }

    private MobileIconState getFirstMobileState() {
        if (this.mMobileStates.size() > 0) {
            return (MobileIconState) this.mMobileStates.get(0);
        }
        return null;
    }

    public void setSubs(List<SubscriptionInfo> list) {
        if (!hasCorrectSubs(list)) {
            String str = "StatusBarSignalPolicy";
            Log.i(str, "setSubs s");
            initLTEIcon();
            this.mIconController.removeAllIconsForSlot(this.mSlotMobile);
            this.mMobileStates.clear();
            int size = list.size();
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            ArrayList arrayList = new ArrayList();
            int i = phoneCount;
            for (int i2 = 0; i2 < size; i2++) {
                int simSlotIndex = ((SubscriptionInfo) list.get(i2)).getSimSlotIndex();
                MobileIconState mobileIconState = new MobileIconState(((SubscriptionInfo) list.get(i2)).getSubscriptionId());
                mobileIconState.phoneId = simSlotIndex;
                arrayList.add(mobileIconState);
                if (simSlotIndex >= i) {
                    i = simSlotIndex + 1;
                }
            }
            MobileIconState[] mobileIconStateArr = new MobileIconState[i];
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                mobileIconStateArr[((MobileIconState) arrayList.get(i3)).phoneId] = (MobileIconState) arrayList.get(i3);
            }
            if (this.mShowNoSim) {
                for (int i4 = i - 1; i4 >= 0; i4--) {
                    if (size == 0 || (this.mCTA && mobileIconStateArr[i4] == null)) {
                        MobileIconState mobileIconState2 = new MobileIconState((0 - i4) - 1);
                        mobileIconState2.phoneId = i4;
                        this.mMobileStates.add(mobileIconState2);
                        mobileIconState2.visible = !this.mIsAirplaneMode && !this.mBlockMobile;
                    } else if (mobileIconStateArr[i4] != null) {
                        this.mMobileStates.add(mobileIconStateArr[i4]);
                    }
                }
            }
            this.mIconController.setMobileIcons(this.mSlotMobile, MobileIconState.copyStates(this.mMobileStates));
            dumpMobileStates();
            Log.i(str, "setSubs e");
        }
    }

    private void dumpMobileStates() {
        for (int i = 0; i < this.mMobileStates.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(" setSubs log:");
            sb.append(((MobileIconState) this.mMobileStates.get(i)).toString());
            Log.i("StatusBarSignalPolicy", sb.toString());
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> list) {
        int size = list.size();
        if (size != this.mMobileStates.size() || list.size() == 0) {
            return false;
        }
        int i = 0;
        while (i < size) {
            if (((MobileIconState) this.mMobileStates.get(i)).subId != ((SubscriptionInfo) list.get(i)).getSubscriptionId()) {
                return false;
            }
            if (((MobileIconState) this.mMobileStates.get(i)).subId != ((SubscriptionInfo) list.get(i)).getSubscriptionId() || ((MobileIconState) this.mMobileStates.get(i)).phoneId == ((SubscriptionInfo) list.get(i)).getSimSlotIndex()) {
                i++;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("hasCorrectSubs SubId:");
                sb.append(((MobileIconState) this.mMobileStates.get(i)).subId);
                sb.append(" change from:");
                sb.append(((MobileIconState) this.mMobileStates.get(i)).phoneId);
                sb.append(" to:");
                sb.append(((SubscriptionInfo) list.get(i)).getSimSlotIndex());
                Log.i("StatusBarSignalPolicy", sb.toString());
                return false;
            }
        }
        return true;
    }

    public void setEthernetIndicators(IconState iconState) {
        if (iconState.visible) {
            boolean z = this.mBlockEthernet;
        }
        int i = iconState.icon;
        String str = iconState.contentDescription;
        if (i > 0) {
            this.mIconController.setIcon(this.mSlotEthernet, i, str);
            this.mIconController.setIconVisibility(this.mSlotEthernet, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotEthernet, false);
    }

    public void setIsAirplaneMode(IconState iconState) {
        this.mIsAirplaneMode = iconState.visible && !this.mBlockAirplane;
        int i = iconState.icon;
        String str = iconState.contentDescription;
        if (!this.mIsAirplaneMode || i <= 0) {
            this.mIconController.setIconVisibility(this.mSlotAirplane, false);
        } else {
            this.mIconController.setIcon(this.mSlotAirplane, i, str);
            this.mIconController.setIconVisibility(this.mSlotAirplane, true);
        }
        Iterator it = this.mMobileStates.iterator();
        while (it.hasNext()) {
            MobileIconState mobileIconState = (MobileIconState) it.next();
            if (mobileIconState.subId < 0) {
                mobileIconState.visible = !this.mIsAirplaneMode && !this.mBlockMobile;
            }
        }
        this.mIconController.setMobileIcons(this.mSlotMobile, MobileIconState.copyStates(this.mMobileStates));
    }

    public void setLTEStatus(IconState[] iconStateArr) {
        String str = "StatusBarSignalPolicy";
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("setLTEStatus / mBlackList:");
            sb.append(this.mBlackList);
            Log.i(str, sb.toString());
        }
        String str2 = this.mBlackList;
        if (str2 == null) {
            ArraySet iconBlacklist = StatusBarIconController.getIconBlacklist(str2);
            boolean contains = iconBlacklist.contains("volte");
            boolean contains2 = iconBlacklist.contains("vowifi");
            boolean[] zArr = this.mBlockLTE;
            zArr[0] = contains;
            zArr[2] = contains2;
        }
        for (int i = 0; i < this.mLTEIconStates.size(); i++) {
            LTEIconState lTEIconState = (LTEIconState) this.mLTEIconStates.get(i);
            int i2 = lTEIconState.imsIndex;
            lTEIconState.visible = iconStateArr[i2].visible && !this.mBlockLTE[i2] && iconStateArr[i2].icon > 0;
            if (OP_DEBUG && !lTEIconState.visible) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("setLTEStatus / status[imsIndex].visible:");
                sb2.append(iconStateArr[i2].visible);
                sb2.append(" / mBlockLTE[imsIndex]:");
                sb2.append(this.mBlockLTE[i2]);
                sb2.append(" / status[imsIndex].icon:");
                sb2.append(iconStateArr[i2].icon);
                sb2.append(" / imsIndex:");
                sb2.append(i2);
                Log.i(str, sb2.toString());
            }
            if (lTEIconState.visible) {
                lTEIconState.resId = iconStateArr[i2].icon;
                lTEIconState.contentDescription = iconStateArr[i2].contentDescription;
                this.mIconController.setIcon(lTEIconState.slot, lTEIconState.resId, lTEIconState.contentDescription);
                this.mIconController.setIconVisibility(lTEIconState.slot, true);
            } else {
                this.mIconController.setIconVisibility(lTEIconState.slot, false);
            }
        }
        if (OP_DEBUG) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(" setLTEStatus:");
            sb3.append(this.mLTEIconStates.toString());
            Log.i(str, sb3.toString());
        }
    }

    public void setProvision(int i, int i2) {
        int[] iArr = this.mProvisionState;
        if (i < iArr.length) {
            iArr[i] = i2;
            MobileIconState stateByPhoneId = getStateByPhoneId(i);
            if (stateByPhoneId != null) {
                stateByPhoneId.showNoSim = isDataDisable(i) || stateByPhoneId.isAbsent();
                StringBuilder sb = new StringBuilder();
                sb.append("setProvision slotId:");
                sb.append(i);
                sb.append(" provision:");
                sb.append(i2);
                sb.append(" state.isAbsent():");
                sb.append(stateByPhoneId.isAbsent());
                sb.append(" state.showNoSim:");
                sb.append(stateByPhoneId.showNoSim);
                Log.i("StatusBarSignalPolicy", sb.toString());
                this.mIconController.setMobileIcons(this.mSlotMobile, MobileIconState.copyStates(this.mMobileStates));
            }
        }
    }

    private boolean isDataDisable(int i) {
        int[] iArr = this.mProvisionState;
        if (i >= iArr.length) {
            return false;
        }
        int i2 = iArr[i];
        boolean z = true;
        if (i2 == 1) {
            z = false;
        }
        return z;
    }

    private MobileIconState getStateByPhoneId(int i) {
        Iterator it = this.mMobileStates.iterator();
        while (it.hasNext()) {
            MobileIconState mobileIconState = (MobileIconState) it.next();
            if (mobileIconState.phoneId == i) {
                return mobileIconState;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected slotId ");
        sb.append(i);
        Log.e("StatusBarSignalPolicy", sb.toString());
        return null;
    }

    private void initLTEIcon() {
        String str = "volte";
        this.mIconController.removeAllIconsForSlot(str);
        String str2 = "vowifi";
        this.mIconController.removeAllIconsForSlot(str2);
        this.mLTEIconStates.clear();
        LTEIconState lTEIconState = new LTEIconState(str);
        lTEIconState.resId = R$drawable.stat_sys_volte;
        lTEIconState.imsIndex = 0;
        LTEIconState lTEIconState2 = new LTEIconState(str2);
        lTEIconState2.resId = R$drawable.stat_sys_vowifi;
        lTEIconState2.imsIndex = 2;
        this.mLTEIconStates.add(lTEIconState);
        this.mLTEIconStates.add(lTEIconState2);
        this.mIconController.setIcon(str2, R$drawable.stat_sys_vowifi, null);
        this.mIconController.setIcon(str, R$drawable.stat_sys_volte, null);
        this.mIconController.setIconVisibility(str, false);
        this.mIconController.setIconVisibility(str2, false);
    }

    private int getSlotProvisionStatus(int i, int i2) {
        String str = " Exception: ";
        String str2 = "Failed to get pref, slotId: ";
        String str3 = "StatusBarSignalPolicy";
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
}
