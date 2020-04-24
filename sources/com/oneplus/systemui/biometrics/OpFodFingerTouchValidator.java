package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.os.Debug;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;

public class OpFodFingerTouchValidator {
    /* access modifiers changed from: private */
    public boolean mAodMode = false;
    private Context mContext;
    private boolean mFingerOnSensor;
    private boolean mFingerOnView;
    KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStartedWakingUp() {
            OpFodFingerTouchValidator.this.mAodMode = false;
        }

        public void onScreenTurnedOff() {
            OpFodFingerTouchValidator.this.mAodMode = true;
        }
    };
    private KeyguardUpdateMonitor mUpdateMonitor;

    public OpFodFingerTouchValidator(Context context) {
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
    }

    public boolean validateFingerAction(int i) {
        if (i != 0 || this.mFingerOnView) {
            if (i != 1 || !this.mFingerOnView) {
                return false;
            }
            this.mFingerOnView = false;
        } else if (KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockingWithBiometricAllowed() || OpFodHelper.getInstance().isDoingEnroll()) {
            this.mFingerOnView = true;
            this.mFingerOnSensor = false;
        } else {
            Log.d("OpFodFingerTouchValidator", "validateFingerAction: onView, not allow to unlock by fingerprint. return.");
            return false;
        }
        return true;
    }

    public boolean validateFingerAction(int i, int i2) {
        String str = "OpFodFingerTouchValidator";
        if (i != 6) {
            StringBuilder sb = new StringBuilder();
            sb.append("validateFingerAction: onSensor, return, acquiredInfo: ");
            sb.append(i);
            Log.d(str, sb.toString());
            return false;
        } else if (this.mFingerOnView) {
            Log.d(str, "validateFingerAction: onSensor, finger on view return.");
            return false;
        } else if (i2 != 0 && i2 != 1) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("validateFingerAction: onSensor, return, vendorCode: ");
            sb2.append(i2);
            Log.d(str, sb2.toString());
            return false;
        } else if (i2 == 0) {
            this.mFingerOnSensor = true;
            this.mFingerOnView = false;
            return true;
        } else if (this.mFingerOnSensor) {
            this.mFingerOnSensor = false;
            return true;
        } else {
            Log.d(str, "validateFingerAction: onSensor, not receive touch down before.");
            return false;
        }
    }

    public boolean isFingerDown() {
        return this.mFingerOnView || this.mFingerOnSensor;
    }

    public String toString() {
        return String.format("([%s]: aodMode: %b, fingerOnView: %b, fingerOnSensor: %b)", new Object[]{"OpFodFingerTouchValidator", Boolean.valueOf(this.mAodMode), Boolean.valueOf(this.mFingerOnView), Boolean.valueOf(this.mFingerOnSensor)}).toString();
    }

    public boolean isFingerDownOnView() {
        return this.mFingerOnView;
    }

    public boolean isFingerDownOnSensor() {
        return this.mFingerOnSensor;
    }

    public void resetTouchFromSensor() {
        Log.d("OpFodFingerTouchValidator", "resetTouchFromSensor");
        this.mFingerOnSensor = false;
    }

    public void reset() {
        StringBuilder sb = new StringBuilder();
        sb.append("reset: callers= ");
        sb.append(Debug.getCallers(1));
        Log.d("OpFodFingerTouchValidator", sb.toString());
        this.mFingerOnView = false;
        this.mFingerOnSensor = false;
    }
}
