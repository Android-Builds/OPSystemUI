package com.android.systemui.statusbar.policy;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.policy.NetworkController.EmergencyListener;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallbackHandler extends Handler implements EmergencyListener, SignalCallback {
    private final ArrayList<EmergencyListener> mEmergencyListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public final ArrayList<SignalCallback> mSignalCallbacks = new ArrayList<>();

    public CallbackHandler() {
        super(Looper.getMainLooper());
    }

    @VisibleForTesting
    CallbackHandler(Looper looper) {
        super(looper);
    }

    public void handleMessage(Message message) {
        switch (message.what) {
            case 0:
                Iterator it = this.mEmergencyListeners.iterator();
                while (it.hasNext()) {
                    ((EmergencyListener) it.next()).setEmergencyCallsOnly(message.arg1 != 0);
                }
                return;
            case 1:
                Iterator it2 = this.mSignalCallbacks.iterator();
                while (it2.hasNext()) {
                    ((SignalCallback) it2.next()).setSubs((List) message.obj);
                }
                return;
            case 2:
                Iterator it3 = this.mSignalCallbacks.iterator();
                while (it3.hasNext()) {
                    ((SignalCallback) it3.next()).setNoSims(message.arg1 != 0, message.arg2 != 0);
                }
                return;
            case 3:
                Iterator it4 = this.mSignalCallbacks.iterator();
                while (it4.hasNext()) {
                    ((SignalCallback) it4.next()).setEthernetIndicators((IconState) message.obj);
                }
                return;
            case 4:
                Iterator it5 = this.mSignalCallbacks.iterator();
                while (it5.hasNext()) {
                    ((SignalCallback) it5.next()).setIsAirplaneMode((IconState) message.obj);
                }
                return;
            case 5:
                Iterator it6 = this.mSignalCallbacks.iterator();
                while (it6.hasNext()) {
                    ((SignalCallback) it6.next()).setMobileDataEnabled(message.arg1 != 0);
                }
                return;
            case 6:
                if (message.arg1 != 0) {
                    this.mEmergencyListeners.add((EmergencyListener) message.obj);
                    return;
                } else {
                    this.mEmergencyListeners.remove((EmergencyListener) message.obj);
                    return;
                }
            case 7:
                if (message.arg1 != 0) {
                    this.mSignalCallbacks.add((SignalCallback) message.obj);
                    return;
                } else {
                    this.mSignalCallbacks.remove((SignalCallback) message.obj);
                    return;
                }
            case 8:
                Iterator it7 = this.mSignalCallbacks.iterator();
                while (it7.hasNext()) {
                    ((SignalCallback) it7.next()).setLTEStatus((IconState[]) message.obj);
                }
                return;
            case 9:
                Iterator it8 = this.mSignalCallbacks.iterator();
                while (it8.hasNext()) {
                    ((SignalCallback) it8.next()).setProvision(message.arg1, message.arg2);
                }
                return;
            case 10:
                Iterator it9 = this.mSignalCallbacks.iterator();
                while (it9.hasNext()) {
                    ((SignalCallback) it9.next()).setVirtualSimstate((int[]) message.obj);
                }
                return;
            case 11:
                Iterator it10 = this.mSignalCallbacks.iterator();
                while (it10.hasNext()) {
                    ((SignalCallback) it10.next()).setHasAnySimReady(((Boolean) message.obj).booleanValue());
                }
                return;
            case 12:
                Iterator it11 = this.mSignalCallbacks.iterator();
                while (it11.hasNext()) {
                    ((SignalCallback) it11.next()).setTetherError((Intent) message.obj);
                }
                return;
            default:
                return;
        }
    }

    public void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
        final boolean z5 = z;
        final IconState iconState3 = iconState;
        final IconState iconState4 = iconState2;
        final boolean z6 = z2;
        final boolean z7 = z3;
        final String str3 = str;
        final boolean z8 = z4;
        final String str4 = str2;
        C15751 r0 = new Runnable() {
            public void run() {
                Iterator it = CallbackHandler.this.mSignalCallbacks.iterator();
                while (it.hasNext()) {
                    ((SignalCallback) it.next()).setWifiIndicators(z5, iconState3, iconState4, z6, z7, str3, z8, str4);
                }
            }
        };
        post(r0);
    }

    public void setMobileDataIndicators(IconState iconState, IconState iconState2, int i, int i2, boolean z, boolean z2, int i3, int[] iArr, int[] iArr2, String str, String str2, boolean z3, int i4, boolean z4, boolean z5) {
        final IconState iconState3 = iconState;
        final IconState iconState4 = iconState2;
        final int i5 = i;
        final int i6 = i2;
        final boolean z6 = z;
        final boolean z7 = z2;
        final int i7 = i3;
        final int[] iArr3 = iArr;
        final int[] iArr4 = iArr2;
        final String str3 = str;
        final String str4 = str2;
        final boolean z8 = z3;
        final int i8 = i4;
        final boolean z9 = z4;
        final boolean z10 = z5;
        C15763 r18 = r0;
        C15763 r0 = new Runnable(this) {
            final /* synthetic */ CallbackHandler this$0;

            {
                this.this$0 = r3;
            }

            public void run() {
                Iterator it = this.this$0.mSignalCallbacks.iterator();
                while (it.hasNext()) {
                    Iterator it2 = it;
                    ((SignalCallback) it.next()).setMobileDataIndicators(iconState3, iconState4, i5, i6, z6, z7, i7, iArr3, iArr4, str3, str4, z8, i8, z9, z10);
                    it = it2;
                }
            }
        };
        post(r18);
    }

    public void setSubs(List<SubscriptionInfo> list) {
        obtainMessage(1, list).sendToTarget();
    }

    public void setNoSims(boolean z, boolean z2) {
        obtainMessage(2, z ? 1 : 0, z2 ? 1 : 0).sendToTarget();
    }

    public void setMobileDataEnabled(boolean z) {
        obtainMessage(5, z ? 1 : 0, 0).sendToTarget();
    }

    public void setEmergencyCallsOnly(boolean z) {
        obtainMessage(0, z ? 1 : 0, 0).sendToTarget();
    }

    public void setEthernetIndicators(IconState iconState) {
        obtainMessage(3, iconState).sendToTarget();
    }

    public void setIsAirplaneMode(IconState iconState) {
        obtainMessage(4, iconState).sendToTarget();
    }

    public void setListening(SignalCallback signalCallback, boolean z) {
        obtainMessage(7, z ? 1 : 0, 0, signalCallback).sendToTarget();
    }

    public void setLTEStatus(IconState[] iconStateArr) {
        obtainMessage(8, iconStateArr).sendToTarget();
    }

    public void setProvision(int i, int i2) {
        obtainMessage(9, i, i2).sendToTarget();
    }

    public void setVirtualSimstate(int[] iArr) {
        obtainMessage(10, 0, 0, iArr).sendToTarget();
    }

    public void setHasAnySimReady(boolean z) {
        obtainMessage(11, 0, 0, Boolean.valueOf(z)).sendToTarget();
    }

    public void setTetherError(Intent intent) {
        obtainMessage(12, 0, 0, intent).sendToTarget();
    }
}
