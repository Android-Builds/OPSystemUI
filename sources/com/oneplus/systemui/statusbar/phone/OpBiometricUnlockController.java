package com.oneplus.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.oneplus.keyguard.OpKeyguardUpdateMonitor;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.keyguard.OpKeyguardViewMediator;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpBiometricUnlockController extends KeyguardUpdateMonitorCallback {
    private static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static int FP_FAILED_ATTEMPTS_TO_WAKEUP = 3;
    private static int FP_FAILED_ATTEMPTS_TO_WAKEUP_IN_DOZE = 1;
    private final boolean IS_SUPPORT_CUSTOM_FINGERPRINT = OpUtils.isCustomFingerprint();
    private boolean mApplySpeedUpPolicy = false;
    private int mFaceLockMode = 0;
    private boolean mForceShowBouncer = false;
    private boolean mIsFingerprintAuthenticating = false;
    private boolean mIsPlaying;
    protected boolean mIsScreenOffUnlock;
    private final KeyguardViewMediator mKeyguardViewMediator;
    private boolean mNoBouncerAnim = false;
    private final PowerManager mPowerManager;
    private final StatusBar mStatusBar;
    private final StatusBarWindowController mStatusBarWindowController;
    private final KeyguardUpdateMonitor mUpdateMonitor;

    public OpBiometricUnlockController(Context context, KeyguardViewMediator keyguardViewMediator, StatusBar statusBar) {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mStatusBar = statusBar;
    }

    /* access modifiers changed from: protected */
    public void opShowBouncer() {
        this.mNoBouncerAnim = false;
        if (this.IS_SUPPORT_CUSTOM_FINGERPRINT && this.mForceShowBouncer) {
            this.mNoBouncerAnim = true;
        }
        this.mNoBouncerAnim = false;
        if (calculateMode(BiometricSourceType.FINGERPRINT) == 3) {
            getStatusBarKeyguardViewManager().showBouncer(false);
        }
        if (!getStatusBarKeyguardViewManager().isBouncerShowing()) {
            getStatusBarKeyguardViewManager().animateCollapsePanels(this.mNoBouncerAnim ? 999.0f : 1.1f);
        }
        this.mNoBouncerAnim = false;
        setPendingShowBouncer(false);
        this.mForceShowBouncer = false;
    }

    public boolean isBouncerAnimNeeded() {
        return this.mNoBouncerAnim;
    }

    public void onScreenTurnedOn() {
        this.mIsScreenOffUnlock = false;
    }

    public void onScreenTurnedOff() {
        this.mUpdateMonitor.setWakingUpReason(null);
    }

    private void onFingerprintUnlockStart() {
        boolean isFinishedScreenTuredOn = OpLsState.getInstance().isFinishedScreenTuredOn();
        boolean isShowingLiveWallpaper = this.mStatusBarWindowController.isShowingLiveWallpaper(true);
        boolean isDreaming = this.mUpdateMonitor.isDreaming();
        boolean isInteractive = this.mPowerManager.isInteractive();
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintUnlockStart, screenOn:");
            sb.append(isFinishedScreenTuredOn);
            sb.append(" , dream:");
            sb.append(isDreaming);
            sb.append(" , live:");
            sb.append(isShowingLiveWallpaper);
            sb.append(", interactive: ");
            sb.append(isInteractive);
            Log.d("OpBiometricUnlockController", sb.toString());
        }
        if (!isFinishedScreenTuredOn && !isShowingLiveWallpaper) {
            if (this.mKeyguardViewMediator.isShowingAndNotOccluded()) {
                changePanelVisibilityByAlpha(0, false);
            }
            String str = "com.android.systemui:UnlockStart";
            this.mUpdateMonitor.setWakingUpReason(str);
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), str);
            this.mIsScreenOffUnlock = true;
        } else if (isFinishedScreenTuredOn && !isInteractive && this.mKeyguardViewMediator.isShowingAndNotOccluded() && !isShowingLiveWallpaper) {
            changePanelVisibilityByAlpha(0, false);
        }
    }

    /* access modifiers changed from: protected */
    public void onFingerprintUnlockCancel(int i) {
        boolean userCanSkipBouncer = this.mUpdateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser());
        int i2 = FP_FAILED_ATTEMPTS_TO_WAKEUP;
        if (this.IS_SUPPORT_CUSTOM_FINGERPRINT) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
            if (!OpKeyguardUpdateMonitor.IS_SUPPORT_MOTOR_CAMERA && !keyguardUpdateMonitor.isDeviceInteractive() && this.mUpdateMonitor.isUnlockWithFacelockPossible()) {
                i2 = FP_FAILED_ATTEMPTS_TO_WAKEUP_IN_DOZE;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("onFingerprintUnlockCancel: Reason:");
        sb.append(i);
        sb.append(", OffUnlock:");
        sb.append(this.mIsScreenOffUnlock);
        sb.append(", attemps:");
        sb.append(this.mUpdateMonitor.getFingerprintFailedUnlockAttempts());
        sb.append(", Authenticating:");
        sb.append(isFingerprintAuthenticating());
        sb.append(", prevent:");
        sb.append(OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive());
        sb.append(", skip:");
        sb.append(userCanSkipBouncer);
        sb.append(", ScreenOn:");
        sb.append(this.mUpdateMonitor.isScreenOn());
        sb.append(", interactive:");
        sb.append(this.mUpdateMonitor.isDeviceInteractive());
        sb.append(", bouncer:");
        sb.append(getStatusBarKeyguardViewManager().isBouncerShowing());
        sb.append(", threshold:");
        sb.append(i2);
        sb.append(", motor:");
        sb.append(OpKeyguardUpdateMonitor.IS_SUPPORT_MOTOR_CAMERA);
        Log.d("OpBiometricUnlockController", sb.toString());
        if (this.IS_SUPPORT_CUSTOM_FINGERPRINT && i == 0 && !this.mUpdateMonitor.isDeviceInteractive() && !getStatusBarKeyguardViewManager().isShowing()) {
            this.mKeyguardViewMediator.doKeyguardTimeout(null);
        }
        if (isFingerprintAuthenticating()) {
            changePanelVisibilityByAlpha(1, false);
            if (i == 0 && this.mUpdateMonitor.getFingerprintFailedUnlockAttempts() >= i2) {
                if (!userCanSkipBouncer && !OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive()) {
                    KeyguardUpdateMonitor keyguardUpdateMonitor2 = this.mUpdateMonitor;
                    if (OpKeyguardUpdateMonitor.IS_SUPPORT_MOTOR_CAMERA) {
                        if (keyguardUpdateMonitor2.isDeviceInteractive()) {
                            getStatusBarKeyguardViewManager().showBouncer(false);
                            if (!this.mStatusBar.isBouncerShowing()) {
                                getStatusBarKeyguardViewManager().animateCollapsePanels(1.1f);
                            }
                        } else if (!this.mUpdateMonitor.isDeviceInteractive()) {
                            this.mForceShowBouncer = true;
                            setPendingShowBouncer(true);
                        }
                    } else if (!keyguardUpdateMonitor2.isScreenOn() || this.mUpdateMonitor.isDeviceInteractive()) {
                        getStatusBarKeyguardViewManager().showBouncer(false);
                        if (!this.mStatusBar.isBouncerShowing()) {
                            getStatusBarKeyguardViewManager().animateCollapsePanels(1.1f);
                        }
                    } else {
                        this.mForceShowBouncer = true;
                        setPendingShowBouncer(true);
                    }
                }
                String str = "com.android.systemui:FailedAttempts";
                this.mUpdateMonitor.setWakingUpReason(str);
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), str);
                if (!this.IS_SUPPORT_CUSTOM_FINGERPRINT) {
                    boolean z = this.mIsScreenOffUnlock;
                } else {
                    boolean isBouncerShowing = getStatusBarKeyguardViewManager().isBouncerShowing();
                }
            } else if (this.mIsScreenOffUnlock) {
                this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 9, 0);
                this.mStatusBar.getFacelockController().onPreStartedGoingToSleep();
                OpLsState.getInstance().onFingerprintStartedGoingToSleep();
            }
        }
        setFingerprintState(false, 5);
    }

    public void onFingerprintAcquired(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onFingerprintAcquired: accquireInfo=");
        sb.append(i);
        String str = "OpBiometricUnlockController";
        Log.d(str, sb.toString());
        releaseBiometricWakeLock();
        PowerManager powerManager = this.mPowerManager;
        boolean isInteractive = powerManager != null ? powerManager.isInteractive() : false;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("isCustomFingerprint = ");
        sb2.append(OpUtils.isCustomFingerprint());
        sb2.append(" , acquireInfo = ");
        sb2.append(i);
        sb2.append(" , isInteractive = ");
        sb2.append(isInteractive);
        sb2.append(" , isHomeApp = ");
        sb2.append(OpUtils.isHomeApp());
        sb2.append(" , mUpdateMonitor.isUnlockingWithBiometricAllowed() = ");
        sb2.append(this.mUpdateMonitor.isUnlockingWithBiometricAllowed());
        sb2.append(" , OpLsState.getInstance().isFinishedScreenTuredOn() = ");
        sb2.append(OpLsState.getInstance().isFinishedScreenTuredOn());
        sb2.append(" , mUpdateMonitor.isDeviceInteractive() = ");
        sb2.append(this.mUpdateMonitor.isDeviceInteractive());
        Log.i(str, sb2.toString());
        if (OpUtils.isCustomFingerprint() && !OpLsState.getInstance().isFinishedScreenTuredOn()) {
            Log.d(str, "don't deal with event if screen does not turne on");
        } else if (i != 6) {
            if (!this.mUpdateMonitor.isDeviceInteractive()) {
                OpLsState.getInstance().getBiometricUnlockController().acquireWakeLock();
            }
        } else if (!this.mUpdateMonitor.isUnlockingWithBiometricAllowed()) {
            Log.d(str, "not allow unlock with biometric");
            String str2 = "com.android.systemui:onAcquired";
            this.mUpdateMonitor.setWakingUpReason(str2);
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), str2);
        } else {
            if (OpUtils.isCustomFingerprint()) {
                if (isInteractive) {
                    this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                } else {
                    this.mStatusBar.onFingerprintPoke();
                }
                if (isInteractive || OpUtils.isHomeApp()) {
                    setFingerprintState(true, 7);
                } else {
                    setFingerprintState(true, 1);
                }
            } else if (!isInteractive) {
                setFingerprintState(true, 1);
            } else {
                setFingerprintState(true, 7);
            }
            onFingerprintUnlockStart();
        }
    }

    public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
        if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintHelp: msgId = ");
            sb.append(i);
            sb.append(", helpString = ");
            sb.append(str);
            Log.d("OpBiometricUnlockController", sb.toString());
            if (i != -1) {
                onFingerprintUnlockCancel(1);
            }
        }
        cleanup();
    }

    /* access modifiers changed from: protected */
    public void onFingerprintAuthFailed() {
        StringBuilder sb = new StringBuilder();
        sb.append("onFingerprintAuthFailed: ");
        sb.append(this.mUpdateMonitor.getFingerprintFailedUnlockAttempts());
        String str = ", ";
        sb.append(str);
        sb.append(isFingerprintAuthenticating());
        sb.append(str);
        sb.append(OpLsState.getInstance().isFinishedScreenTuredOn());
        Log.d("OpBiometricUnlockController", sb.toString());
        String str2 = "finger";
        String str3 = "lock_unlock_failed";
        if (this.mPowerManager.isInteractive()) {
            OpMdmLogger.log(str3, str2, "1");
        } else {
            OpMdmLogger.log(str3, str2, "0");
        }
        onFingerprintUnlockCancel(0);
    }

    public void onFingerprintTimeout() {
        onFingerprintUnlockCancel(3);
        cleanup();
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("onKeyguardVisibilityChanged: ");
        sb.append(z);
        sb.append(" , ");
        sb.append(this.mIsFingerprintAuthenticating);
        Log.d("OpBiometricUnlockController", sb.toString());
        if (!z || !this.mIsFingerprintAuthenticating) {
            changePanelVisibilityByAlpha(1, true);
        } else {
            changePanelVisibilityByAlpha(0, false);
        }
    }

    /* access modifiers changed from: protected */
    public void setFingerprintState(boolean z, int i) {
        if (this.mIsFingerprintAuthenticating != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("setFingerprintState: ");
            sb.append(z);
            sb.append(", result: ");
            sb.append(i);
            Log.d("OpBiometricUnlockController", sb.toString());
        }
        this.mIsFingerprintAuthenticating = z;
        this.mKeyguardViewMediator.notifyScreenOffAuthenticate(this.mIsFingerprintAuthenticating, OpKeyguardViewMediator.AUTHENTICATE_FINGERPRINT, i);
    }

    public boolean isFingerprintAuthenticating() {
        return this.mIsFingerprintAuthenticating;
    }

    /* access modifiers changed from: protected */
    public void changePanelVisibilityByAlpha(int i, boolean z) {
        if (z) {
            this.mKeyguardViewMediator.changePanelAlpha(i, OpKeyguardViewMediator.AUTHENTICATE_IGNORE);
        } else {
            this.mKeyguardViewMediator.changePanelAlpha(i, OpKeyguardViewMediator.AUTHENTICATE_FINGERPRINT);
        }
    }

    public boolean shouldApplySpeedUpPolicy() {
        return this.IS_SUPPORT_CUSTOM_FINGERPRINT && !this.mStatusBarWindowController.isShowingLiveWallpaper(true);
    }

    public void resetSpeedUpPolicy() {
        this.mApplySpeedUpPolicy = false;
    }

    public boolean isPlayingScrimAnimation() {
        return this.mIsPlaying;
    }

    public void opResetMode() {
        resetMode();
    }

    public void setFaceLockMode(int i) {
        this.mFaceLockMode = i;
    }

    public int getFaceLockMode() {
        return this.mFaceLockMode;
    }

    public void startWakeAndUnlockForFace(int i) {
        setFaceLockMode(i);
        boolean isDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        StringBuilder sb = new StringBuilder();
        sb.append("startWakeAndUnlockForFace:");
        sb.append(i);
        sb.append(", ");
        sb.append(isDeviceInteractive);
        Log.d("OpBiometricUnlockController", sb.toString());
        if (i == 1) {
            this.mStatusBarWindowController.setStatusBarFocusable(false);
            this.mKeyguardViewMediator.onWakeAndUnlocking(isLauncherOnTop());
            if (this.mStatusBar.getNavigationBarView() != null) {
                this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
            }
            Trace.endSection();
        } else if (i != 5) {
            if (i == 6) {
                getStatusBarKeyguardViewManager().notifyKeyguardAuthenticated(true);
            }
        } else if (!isDeviceInteractive) {
            setPendingShowBouncer(true);
        } else {
            opShowBouncer();
        }
        this.mStatusBar.notifyBiometricAuthModeChanged();
    }

    private boolean isLauncherOnTop() {
        String str = "OpBiometricUnlockController";
        try {
            int activityType = ((RunningTaskInfo) ActivityManager.getService().getTasks(1).get(0)).configuration.windowConfiguration.getActivityType();
            StringBuilder sb = new StringBuilder();
            sb.append("isLauncherOnTop: ");
            sb.append(activityType);
            Log.d(str, sb.toString());
            return activityType == 2;
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Exception e = ");
            sb2.append(e.toString());
            Log.w(str, sb2.toString());
        }
    }

    /* access modifiers changed from: protected */
    public boolean opIsBiometricUnlock() {
        if (!isWakeAndUnlock() && getMode() != 5) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
            if (keyguardUpdateMonitor == null || !keyguardUpdateMonitor.isFacelockUnlocking()) {
                return false;
            }
        }
        return true;
    }

    private int calculateMode(BiometricSourceType biometricSourceType) {
        return ((Integer) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(BiometricUnlockController.class, "calculateMode", BiometricSourceType.class), biometricSourceType)).intValue();
    }

    private void setPendingShowBouncer(boolean z) {
        OpReflectionUtils.setValue(BiometricUnlockController.class, this, "mPendingShowBouncer", Boolean.valueOf(z));
    }

    private StatusBarKeyguardViewManager getStatusBarKeyguardViewManager() {
        return (StatusBarKeyguardViewManager) OpReflectionUtils.getValue(BiometricUnlockController.class, this, "mStatusBarKeyguardViewManager");
    }

    private void cleanup() {
        OpReflectionUtils.methodInvokeVoid(BiometricUnlockController.class, this, "cleanup", new Object[0]);
    }

    private void resetMode() {
        OpReflectionUtils.methodInvokeVoid(BiometricUnlockController.class, this, "resetMode", new Object[0]);
    }

    private void releaseBiometricWakeLock() {
        OpReflectionUtils.methodInvokeVoid(BiometricUnlockController.class, this, "releaseBiometricWakeLock", new Object[0]);
    }

    private int getMode() {
        return ((Integer) OpReflectionUtils.getValue(BiometricUnlockController.class, this, "mMode")).intValue();
    }

    private boolean isWakeAndUnlock() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(BiometricUnlockController.class, this, "isWakeAndUnlock", new Object[0])).booleanValue();
    }
}
