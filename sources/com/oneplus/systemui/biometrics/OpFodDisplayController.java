package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.hardware.biometrics.BiometricSourceType;
import android.os.PowerManager;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.oneplus.systemui.biometrics.OpFodHelper.OnFingerprintStateChangeListener;

public class OpFodDisplayController extends KeyguardUpdateMonitorCallback implements OnFingerprintStateChangeListener {
    OpFodAodControl mAodControl = new OpFodAodControl(this);
    Context mContext;
    OpFodDimControl mDimControl = new OpFodDimControl(this);
    OpFodDisplayNotifier mDisplayNotifier;
    boolean mFaceUnlocked;
    OpFodHighlightControl mHighlightControl = new OpFodHighlightControl(this);
    PowerManager mPm;
    KeyguardUpdateMonitor mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);

    static abstract class OpDisplayControl implements OpDisplayControllerHelper {
        private OpFodDisplayController mController;
        protected final String mTAG = getClass().getSimpleName();

        public boolean canDisable() {
            return true;
        }

        public abstract boolean canEnable();

        public OpDisplayControl(OpFodDisplayController opFodDisplayController) {
            this.mController = opFodDisplayController;
        }

        /* access modifiers changed from: protected */
        public OpFodDisplayNotifier getNotifier() {
            return this.mController.mDisplayNotifier;
        }

        /* access modifiers changed from: protected */
        public KeyguardUpdateMonitor getUpdateMonitor() {
            return this.mController.mUpdateMonitor;
        }

        /* access modifiers changed from: protected */
        public PowerManager getPowerManager() {
            return this.mController.mPm;
        }

        /* access modifiers changed from: protected */
        public boolean isFaceUnlocked() {
            return this.mController.mFaceUnlocked;
        }

        /* access modifiers changed from: protected */
        public boolean isHighlight() {
            return this.mController.mHighlightControl.isHighlight();
        }

        /* access modifiers changed from: protected */
        public int getAodMode() {
            return this.mController.mAodControl.getAodMode();
        }

        public boolean enable(String str) {
            String str2 = this.mTAG;
            StringBuilder sb = new StringBuilder();
            sb.append("enable: ");
            sb.append(str);
            Log.d(str2, sb.toString());
            if (!canEnable()) {
                return false;
            }
            enableInner(str);
            return true;
        }

        public boolean disable(String str) {
            String str2 = this.mTAG;
            StringBuilder sb = new StringBuilder();
            sb.append("disable: ");
            sb.append(str);
            Log.d(str2, sb.toString());
            if (!canDisable()) {
                return false;
            }
            disableInner(str);
            return true;
        }
    }

    public interface OpDisplayControllerHelper {
        void disableInner(String str);

        void enableInner(String str);
    }

    public OpFodDisplayController(Context context) {
        this.mContext = context;
        this.mPm = (PowerManager) context.getSystemService("power");
        this.mDisplayNotifier = new OpFodDisplayNotifier(context);
        this.mUpdateMonitor.registerCallback(this);
        OpFodHelper.getInstance().addFingerprintStateChangeListener(this);
    }

    public void onFingerprintStateChanged() {
        if (OpFodHelper.getInstance().isFingerprintDetecting()) {
            this.mFaceUnlocked = false;
            this.mDimControl.enable("fp register or resume");
        } else if (OpFodHelper.getInstance().isFingerprintLockout()) {
            this.mDimControl.disable("lockout");
        } else if (OpFodHelper.getInstance().isFingerprintSuspended()) {
            String str = "suspend";
            this.mDimControl.disable(str);
            this.mHighlightControl.disable(str);
        } else {
            this.mDimControl.disable("fp unregister");
        }
    }

    public void resetState() {
        String str = "reset state";
        this.mDimControl.disable(str);
        this.mHighlightControl.disable(str);
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        if (z) {
            this.mDimControl.enable("keyguard visibility change to show");
        }
    }

    public void onFacelockStateChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onFacelockStateChanged: type:");
        sb.append(i);
        Log.d("OpFodDisplayController", sb.toString());
        if (i == 4) {
            this.mFaceUnlocked = true;
            if (OpFodHelper.getInstance().isKeyguardClient()) {
                this.mDimControl.disable("face unlocked");
            }
        }
    }

    public void onStartedWakingUp() {
        String str = "start waking up";
        this.mDimControl.enable(str);
        this.mAodControl.disable(str);
    }

    public void onFinishedGoingToSleep(int i) {
        this.mFaceUnlocked = false;
    }

    public void onScreenTurnedOn() {
        this.mDimControl.enable("screen on");
    }

    public void onScreenTurnedOff() {
        this.mAodControl.enable("screen turned off");
    }

    public void onKeyguardDoneChanged(boolean z) {
        if (z) {
            String str = "keyguardDone";
            this.mDimControl.disable(str);
            this.mHighlightControl.disable(str);
        }
    }

    public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
        if (BiometricSourceType.FINGERPRINT == biometricSourceType) {
            boolean isKeyguardVisible = this.mUpdateMonitor.isKeyguardVisible();
            boolean isKeyguardDone = this.mUpdateMonitor.isKeyguardDone();
            StringBuilder sb = new StringBuilder();
            sb.append("onBiometricAuthenticated isInteractive:");
            sb.append(this.mPm.isInteractive());
            sb.append(", isKeyguardVisible:");
            sb.append(isKeyguardVisible);
            sb.append(", isKeyguardDone:");
            sb.append(isKeyguardDone);
            Log.d("OpFodDisplayController", sb.toString());
            if (!isKeyguardDone && !this.mPm.isInteractive()) {
                this.mAodControl.disable("fp authenticated");
            }
        }
    }

    public void dismiss() {
        this.mDimControl.disable("dismiss");
    }

    public void hideFODDim() {
        this.mDimControl.disable("early hide dim");
    }

    public void notifyFingerprintAuthenticated() {
        if (!this.mUpdateMonitor.isKeyguardDone()) {
            this.mDimControl.disable("fp unlock");
        }
    }

    public void onFingerPressDown() {
        this.mHighlightControl.enable("finger press down");
    }

    public void onFingerPressUp() {
        this.mHighlightControl.disable("finger press up");
    }
}
