package com.oneplus.systemui.biometrics;

import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.plugin.OpLsState;

public class OpFodDimControl extends OpDisplayControl {
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager = OpLsState.getInstance().getStatusBarKeyguardViewManager();

    public /* bridge */ /* synthetic */ boolean canDisable() {
        return super.canDisable();
    }

    public /* bridge */ /* synthetic */ boolean disable(String str) {
        return super.disable(str);
    }

    public /* bridge */ /* synthetic */ boolean enable(String str) {
        return super.enable(str);
    }

    public OpFodDimControl(OpFodDisplayController opFodDisplayController) {
        super(opFodDisplayController);
    }

    public boolean canEnable() {
        if (OpFodHelper.getInstance().isEmptyClient() || OpFodHelper.getInstance().isForceShowClient()) {
            Log.d(this.mTAG, "don't enable HBM due to no one registering fp");
            return false;
        } else if (getUpdateMonitor().isKeyguardDone() && OpFodHelper.getInstance().isKeyguardClient()) {
            Log.d(this.mTAG, "don't re-enable HBM due to fingerprint unlocking");
            return false;
        } else if (isHighlight()) {
            Log.d(this.mTAG, "force enable HBM since highlight icon is visible");
            return true;
        } else if (getUpdateMonitor().isGoingToSleep()) {
            Log.d(this.mTAG, "don't enable HBM due to going to sleep");
            return false;
        } else if (OpLsState.getInstance().getBiometricUnlockController().isWakeAndUnlock() || (getUpdateMonitor().isKeyguardDone() && OpFodHelper.getInstance().isKeyguardClient())) {
            Log.d(this.mTAG, "don't enable HBM due to duraing fp wake and unlock");
            return false;
        } else {
            if (getPowerManager().isInteractive()) {
                StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
                if (statusBarKeyguardViewManager != null && statusBarKeyguardViewManager.isOccluded() && !this.mStatusBarKeyguardViewManager.isBouncerShowing() && this.mStatusBarKeyguardViewManager.isShowing()) {
                    Log.d(this.mTAG, "don't enable HBM due to keyguard is occluded and device is interactive");
                    return false;
                }
            }
            if (!getPowerManager().isInteractive() && getUpdateMonitor().isScreenOn() && isHighlight()) {
                Log.d(this.mTAG, "force enable HBM in aod and fp is pressed");
                return true;
            } else if (!getPowerManager().isInteractive()) {
                Log.d(this.mTAG, "don't enable HBM due to device isn't interactive");
                return false;
            } else if (isFaceUnlocked()) {
                Log.d(this.mTAG, "don't enable HBM due to already face unlocked");
                return false;
            } else if (getUpdateMonitor().isFingerprintLockout() || getUpdateMonitor().isUserInLockdown(KeyguardUpdateMonitor.getCurrentUser())) {
                Log.d(this.mTAG, "don't enable HBM due to lockout");
                return false;
            } else if (getUpdateMonitor().isUnlockingWithBiometricAllowed() || getUpdateMonitor().isKeyguardDone() || !OpFodHelper.getInstance().isKeyguardClient()) {
                return true;
            } else {
                Log.d(this.mTAG, "don't enable HBM due to boot device or biometrice doesn't allow");
                return false;
            }
        }
    }

    public void enableInner(String str) {
        getNotifier().notifyDisplayDimMode(1, getAodMode());
    }

    public void disableInner(String str) {
        getNotifier().notifyDisplayDimMode(0, getAodMode());
    }
}
