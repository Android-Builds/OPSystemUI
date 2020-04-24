package com.oneplus.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.OpFeatures;
import android.util.SparseIntArray;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitor.BatteryStatus;
import com.android.keyguard.KeyguardUpdateMonitor.StrongAuthTracker;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R$string;
import com.android.systemui.recents.OverviewProxyService;
import com.oneplus.keyguard.clock.OpClockCtrl;
import com.oneplus.keyguard.clock.OpClockCtrl.OnTimeUpdatedListener;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.biometrics.OpFingerprintDialogView;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpKeyguardUpdateMonitor {
    protected static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    public static final boolean IS_SUPPORT_BOOT_TO_ENTER_BOUNCER = OpFeatures.isSupport(new int[]{47});
    private static final boolean IS_SUPPORT_CUSTOM_FINGERPRINT = OpUtils.isCustomFingerprint();
    public static final boolean IS_SUPPORT_FACE_UNLOCK = OpFeatures.isSupport(new int[]{46});
    private static final boolean IS_SUPPORT_FINGERPRINT_POCKET;
    public static final boolean IS_SUPPORT_MOTOR_CAMERA = OpFeatures.isSupport(new int[]{114});
    private String FOD_UI_DEBUG = "sys.prop.fod_ui_test";
    /* access modifiers changed from: private */
    public boolean mAutoFacelockEnabled = false;
    /* access modifiers changed from: private */
    public boolean mBouncerRecognizeEnabled = false;
    protected boolean mCameraLaunched;
    private OpClockCtrl mClockCtrl = OpClockCtrl.getInstance();
    private final Context mContext;
    /* access modifiers changed from: protected */
    public boolean mDuringAcquired = false;
    /* access modifiers changed from: private */
    public boolean mFacelockEnabled = false;
    /* access modifiers changed from: private */
    public boolean mFacelockLightingEnabled = false;
    private int mFacelockRunningType = 0;
    private ContentObserver mFacelockSettingsObserver;
    /* access modifiers changed from: private */
    public int mFacelockSuccessTimes = 0;
    private boolean mFacelockUnlocking;
    private SparseIntArray mFailedAttempts = new SparseIntArray();
    private boolean mFakeLocking;
    private boolean mFingerprintAlreadyAuthenticated;
    protected SparseIntArray mFingerprintFailedAttempts = new SparseIntArray();
    private OpFingerprintDialogView mFodDialogView;
    private FingerprintManager mFpm;
    private int mGoingToSleepReason = -1;
    private boolean mImeShow = false;
    private boolean mIsFaceAdded = false;
    protected boolean mIsInBrickMode = false;
    private boolean mIsKeyguardDone = true;
    private boolean mIsUserUnlocked = true;
    private boolean mKeyguardShowing = false;
    private long mLastHangupTimeMillis = 0;
    private boolean mLaunchingCamera;
    private boolean mLaunchingLeftAffordance;
    /* access modifiers changed from: private */
    public boolean mLidOpen = true;
    /* access modifiers changed from: protected */
    public boolean mLockoutState = false;
    private Calendar mMeaWakingUpTime = null;
    /* access modifiers changed from: protected */
    public boolean mPendingSubInfoChange = false;
    SensorEventListener mPocketListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            String str = "OpKeyguardUpdateMonitor";
            int i = 0;
            if (sensorEvent.values.length == 0) {
                Log.d(str, "Event has no values!");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Event: value=");
                sb.append(sensorEvent.values[0]);
                sb.append(" max=");
                sb.append(OpKeyguardUpdateMonitor.this.mPocketSensor.getMaximumRange());
                Log.d(str, sb.toString());
                if (sensorEvent.values[0] == 1.0f) {
                    i = 1;
                }
                i = i != 0 ? 1 : 2;
            }
            if (OpKeyguardUpdateMonitor.this.mPocketState != i) {
                OpKeyguardUpdateMonitor opKeyguardUpdateMonitor = OpKeyguardUpdateMonitor.this;
                opKeyguardUpdateMonitor.updateFPStateBySensor(i, opKeyguardUpdateMonitor.mLidOpen);
            }
        }
    };
    /* access modifiers changed from: private */
    public Sensor mPocketSensor;
    private boolean mPocketSensorEnabled;
    /* access modifiers changed from: private */
    public int mPocketState = 0;
    protected PowerManager mPowerManager;
    private boolean mPreventModeActive = false;
    private boolean mQSExpanded = false;
    private boolean mScreenTurningOn = false;
    private SensorManager mSensorManager;
    private boolean mShutingDown = false;
    /* access modifiers changed from: protected */
    public boolean mSimUnlockSlot0 = false;
    /* access modifiers changed from: protected */
    public boolean mSimUnlockSlot1 = false;
    private boolean mSkipBouncerByFacelock = false;
    private OnTimeUpdatedListener mTimeTickListener = new OnTimeUpdatedListener() {
        public void onTimeChanged() {
            Log.i("OpKeyguardUpdateMonitor", "onTimeChanged");
            if (OpKeyguardUpdateMonitor.this.getHandler() != null) {
                OpKeyguardUpdateMonitor.this.getHandler().removeMessages(301);
                OpKeyguardUpdateMonitor.this.getHandler().sendEmptyMessage(301);
            }
        }
    };
    private String mWakingUpReason = null;

    protected class OpHandler extends Handler {
        public OpHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 701) {
                boolean z = false;
                switch (i) {
                    case 500:
                        OpKeyguardUpdateMonitor.this.handleScreenTurningOn();
                        break;
                    case 501:
                        OpKeyguardUpdateMonitor opKeyguardUpdateMonitor = OpKeyguardUpdateMonitor.this;
                        if (opKeyguardUpdateMonitor.mDuringAcquired) {
                            opKeyguardUpdateMonitor.mDuringAcquired = false;
                            opKeyguardUpdateMonitor.updateFingerprintListeningState();
                            OpKeyguardUpdateMonitor.this.handleFingerprintTimeout();
                            break;
                        }
                        break;
                    case 502:
                        OpKeyguardUpdateMonitor opKeyguardUpdateMonitor2 = OpKeyguardUpdateMonitor.this;
                        if (message.arg1 != 1) {
                            z = true;
                        }
                        opKeyguardUpdateMonitor2.handleLidSwitchChanged(z);
                        break;
                    case 503:
                        OpKeyguardUpdateMonitor.this.handleSystemReady();
                        break;
                    case 504:
                        OpKeyguardUpdateMonitor opKeyguardUpdateMonitor3 = OpKeyguardUpdateMonitor.this;
                        if (message.arg1 != 1) {
                            z = true;
                        }
                        opKeyguardUpdateMonitor3.handlePreventModeChanged(z);
                        break;
                }
            } else {
                OpKeyguardUpdateMonitor opKeyguardUpdateMonitor4 = OpKeyguardUpdateMonitor.this;
                if (opKeyguardUpdateMonitor4.mSimUnlockSlot0 || opKeyguardUpdateMonitor4.mSimUnlockSlot1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("timeout delay of slot: ");
                    sb.append(message.arg1);
                    String str = ", ";
                    sb.append(str);
                    sb.append(OpKeyguardUpdateMonitor.this.mSimUnlockSlot0);
                    sb.append(str);
                    sb.append(OpKeyguardUpdateMonitor.this.mSimUnlockSlot1);
                    Log.d("OpKeyguardUpdateMonitor", sb.toString());
                    OpKeyguardUpdateMonitor.this.opHandlePendingSubInfoChange(message.arg1);
                }
            }
        }
    }

    private boolean isSensorNear(int i, boolean z) {
        return i == 1 || !z;
    }

    public boolean isGoingToSleep() {
        throw null;
    }

    public boolean isSimPinSecure() {
        throw null;
    }

    public boolean isSwitchingUser() {
        throw null;
    }

    public boolean isUnlockingWithBiometricAllowed() {
        throw null;
    }

    public boolean isWeakFaceTimeout() {
        throw null;
    }

    static {
        boolean z;
        if (OpFeatures.isSupport(new int[]{81})) {
            if (OpFeatures.isSupport(new int[]{145})) {
                z = true;
                IS_SUPPORT_FINGERPRINT_POCKET = z;
            }
        }
        z = false;
        IS_SUPPORT_FINGERPRINT_POCKET = z;
    }

    protected OpKeyguardUpdateMonitor(Context context) {
        this.mContext = context;
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        }
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        startClockCtrl();
    }

    private void startClockCtrl() {
        OpClockCtrl opClockCtrl = this.mClockCtrl;
        if (opClockCtrl != null) {
            opClockCtrl.onStartCtrl(this.mTimeTickListener, this.mContext);
        }
    }

    private void onScreenStatusChanged(boolean z) {
        OpClockCtrl opClockCtrl = this.mClockCtrl;
        if (opClockCtrl == null) {
            return;
        }
        if (z) {
            opClockCtrl.onScreenTurnedOn();
        } else {
            opClockCtrl.onScreenTurnedOff();
        }
    }

    /* access modifiers changed from: protected */
    public void handleStartedWakingUp() {
        if (OpUtils.isCustomFingerprint()) {
            setPocketSensorEnabled(false);
        }
        onScreenStatusChanged(true);
        if (!this.mIsKeyguardDone) {
            this.mMeaWakingUpTime = Calendar.getInstance();
        }
    }

    /* access modifiers changed from: protected */
    public void init() {
        this.mSensorManager = new SystemSensorManager(this.mContext, getHandler().getLooper());
        this.mPocketSensor = this.mSensorManager.getDefaultSensor(33171025, true);
    }

    /* access modifiers changed from: protected */
    public void opHandleFingerprintAcquired(int i) {
        if (i == 0 || i == 6) {
            this.mDuringAcquired = true;
            getHandler().removeMessages(501);
            getHandler().sendEmptyMessageDelayed(501, IS_SUPPORT_CUSTOM_FINGERPRINT ? 3000 : 1500);
            for (int i2 = 0; i2 < getCallbacks().size(); i2++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i2)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onFingerprintAcquired(i);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void opHandlePendingSubInfoChange(int i) {
        if (i == 0) {
            this.mSimUnlockSlot0 = false;
        } else if (i == 1) {
            this.mSimUnlockSlot1 = false;
        }
        if (!this.mSimUnlockSlot0 && !this.mSimUnlockSlot1) {
            if (this.mPendingSubInfoChange) {
                Log.d("OpKeyguardUpdateMonitor", "handle pending subinfo change");
                handleSimSubscriptionInfoChanged();
            }
            this.mPendingSubInfoChange = false;
        }
    }

    public void opReportSimUnlocked(int i) {
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        StringBuilder sb = new StringBuilder();
        sb.append("reportSimUnlocked(subId=");
        sb.append(i);
        sb.append(", slotId=");
        sb.append(slotIndex);
        sb.append(")");
        Log.v("OpKeyguardUpdateMonitor", sb.toString());
        if (slotIndex == 0) {
            this.mSimUnlockSlot0 = true;
        } else if (slotIndex == 1) {
            this.mSimUnlockSlot1 = true;
        }
        getHandler().sendMessageDelayed(getHandler().obtainMessage(701, slotIndex, 0), 2000);
        handleSimStateChange(i, slotIndex, State.READY);
    }

    public boolean isUserUnlocked() {
        if (!IS_SUPPORT_BOOT_TO_ENTER_BOUNCER || !getLockPatternUtils().isSecure(getCurrentUser())) {
            return true;
        }
        return this.mIsUserUnlocked;
    }

    public void setUserUnlocked(boolean z) {
        if (this.mIsUserUnlocked != z && z) {
            OpLsState.getInstance().getPhoneStatusBar().getAodDisplayViewManager().handleUserUnlocked();
        }
        this.mIsUserUnlocked = z;
    }

    public void setFodDialogView(OpFingerprintDialogView opFingerprintDialogView) {
        this.mFodDialogView = opFingerprintDialogView;
    }

    public void onImeShow(boolean z) {
        if (IS_SUPPORT_CUSTOM_FINGERPRINT) {
            boolean isUnlockingWithBiometricAllowed = isUnlockingWithBiometricAllowed();
            StringBuilder sb = new StringBuilder();
            sb.append("onImeShow: show:( ");
            sb.append(this.mImeShow);
            sb.append(" -> ");
            sb.append(z);
            sb.append(" ), mLockoutState= ");
            sb.append(this.mLockoutState);
            sb.append(", isUnlockingWithBiometricAllowed= ");
            sb.append(isUnlockingWithBiometricAllowed);
            String str = "OpKeyguardUpdateMonitor";
            Log.d(str, sb.toString());
            if (this.mImeShow != z) {
                this.mImeShow = z;
                if (this.mLockoutState || !isUnlockingWithBiometricAllowed) {
                    Log.d(str, "onImeShow: in lockout state, just update ui.");
                } else {
                    Log.d(str, "onImeShow: update fingerprint listening state");
                    getHandler().postDelayed(getUpdateBiometricListeningStateRunnable(), isFingerprintDetectionRunning() ? 0 : 250);
                }
                OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
                if (opFingerprintDialogView != null && opFingerprintDialogView.isAttachedToWindow()) {
                    this.mFodDialogView.updateIconVisibility(false);
                }
            }
        }
    }

    public boolean isImeShow() {
        return this.mImeShow;
    }

    public void setQSExpanded(boolean z) {
        if (IS_SUPPORT_CUSTOM_FINGERPRINT && z != this.mQSExpanded) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("setQSExpanded: ");
                sb.append(z);
                Log.d("OpKeyguardUpdateMonitor", sb.toString());
            }
            this.mQSExpanded = z;
            if (isKeyguardVisible()) {
                updateFingerprintListeningState();
            }
            for (int i = 0; i < getCallbacks().size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onQSExpendChanged(z);
                }
            }
        }
    }

    public boolean isQSExpanded() {
        return this.mQSExpanded;
    }

    public void notifyShutDownOrReboot() {
        this.mShutingDown = true;
        updateFingerprintListeningState();
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onShuttingDown();
            }
        }
    }

    public boolean isShuttingDown() {
        return this.mShutingDown;
    }

    public void notifyScreenTurningOn() {
        Log.d("OpKeyguardUpdateMonitor", "notifyScreenTurningOn");
        synchronized (this) {
            this.mScreenTurningOn = true;
        }
        getHandler().sendEmptyMessage(500);
    }

    public void handleScreenTurningOn() {
        if (IS_SUPPORT_CUSTOM_FINGERPRINT) {
            updateFingerprintListeningState();
        }
        int size = getCallbacks().size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onScreenTurningOn();
            }
        }
        if (IS_SUPPORT_CUSTOM_FINGERPRINT && !isDeviceInteractive() && isDreaming() && !isUnlockingWithBiometricAllowed()) {
            isFingerprintEnrolled(getCurrentUser());
        }
    }

    public boolean isScreenTurningOn() {
        return this.mScreenTurningOn;
    }

    public void earlyNotifySwitchingUser() {
        Log.d("OpKeyguardUpdateMonitor", "earlyNotifySwitchingUser");
        hideFODDim();
        setSwitchingUser(true);
    }

    public boolean isPreventModeActivte() {
        return this.mPreventModeActive;
    }

    public void dispatchAuthenticateChanged(boolean z, int i, int i2, int i3) {
        int size = getCallbacks().size();
        for (int i4 = 0; i4 < size; i4++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i4)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onAuthenticateChanged(z, i, i2, i3);
            }
        }
    }

    public void hideFODDim() {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            opFingerprintDialogView.hideFODDim();
        }
    }

    public void notifyFakeLocking(boolean z) {
        this.mFakeLocking = z;
    }

    public void setGoingToSleepReason(int i) {
        this.mGoingToSleepReason = i;
    }

    public int getGoingToSleepReason() {
        return this.mGoingToSleepReason;
    }

    public boolean shouldHideDismissButton() {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            return opFingerprintDialogView.shouldHideDismissButton();
        }
        return false;
    }

    public void notifyBrightnessChange() {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            opFingerprintDialogView.notifyBrightnessChange();
        }
    }

    public void checkIfHangup(int i, String str) {
        if (i != 0 && TelephonyManager.EXTRA_STATE_IDLE.equals(str)) {
            this.mLastHangupTimeMillis = SystemClock.uptimeMillis();
        }
    }

    public boolean isHangupRecently() {
        boolean z = false;
        if (this.mLastHangupTimeMillis <= 0) {
            return false;
        }
        if (SystemClock.uptimeMillis() - this.mLastHangupTimeMillis < 2000) {
            z = true;
        }
        return z;
    }

    public boolean isFingerprintEnrolled(int i) {
        FingerprintManager fingerprintManager = this.mFpm;
        return fingerprintManager != null && fingerprintManager.isHardwareDetected() && this.mFpm.getEnrolledFingerprints(i).size() > 0;
    }

    public void updateLaunchingCameraState(boolean z) {
        if (this.mLaunchingCamera != z) {
            this.mLaunchingCamera = z;
            updateFingerprintListeningState();
            OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
            if (opFingerprintDialogView != null) {
                opFingerprintDialogView.updateIconVisibility(false);
            }
        }
        this.mCameraLaunched = z;
        StringBuilder sb = new StringBuilder();
        sb.append(" updateLaunchingCameraState:");
        sb.append(this.mCameraLaunched);
        Log.i("OpKeyguardUpdateMonitor", sb.toString());
    }

    public boolean isCameraLaunched() {
        return this.mCameraLaunched;
    }

    public boolean isLaunchingCamera() {
        return this.mLaunchingCamera;
    }

    public void notifyKeyguardDone(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("notifyKeyguardDone isKeyguardDone ");
        sb.append(z);
        String str = "OpKeyguardUpdateMonitor";
        Log.d(str, sb.toString());
        try {
            ActivityTaskManager.getService().setKeyguardDone(z);
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Exception e = ");
            sb2.append(e.toString());
            Log.w(str, sb2.toString());
        }
        if (this.mDuringAcquired) {
            getHandler().removeMessages(501);
            this.mDuringAcquired = false;
        }
        this.mIsKeyguardDone = z;
        if (this.mMeaWakingUpTime != null && this.mIsKeyguardDone) {
            reportMDMEvent();
            this.mMeaWakingUpTime = null;
        }
        int size = getCallbacks().size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onKeyguardDoneChanged(z);
            }
        }
        if (this.mIsKeyguardDone) {
            ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).notifyKeyguardDone();
        }
    }

    public boolean isKeyguardDone() {
        return this.mIsKeyguardDone;
    }

    /* access modifiers changed from: protected */
    public void opHandleStartedGoingToSleep() {
        this.mFingerprintAlreadyAuthenticated = false;
        if (IS_SUPPORT_CUSTOM_FINGERPRINT) {
            isFingerprintEnrolled(getCurrentUser());
        }
        onScreenStatusChanged(false);
        if (this.mMeaWakingUpTime != null && !this.mIsKeyguardDone) {
            reportMDMEvent();
            this.mMeaWakingUpTime = null;
        }
    }

    public void opOnKeyguardVisibilityChanged(boolean z) {
        this.mKeyguardShowing = z;
        if (!z) {
            this.mFingerprintAlreadyAuthenticated = false;
        } else {
            SystemProperties.getInt(this.FOD_UI_DEBUG, 0);
        }
        if (z) {
            ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).notifyNavBarButtonAlphaChanged(0.0f, true);
        } else if (!OpUtils.isHomeApp()) {
            ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).notifyNavBarButtonAlphaChanged(1.0f, true);
        }
    }

    public void dispatchSystemReady() {
        getHandler().sendEmptyMessage(503);
    }

    public void handleSystemReady() {
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onSystemReady();
            }
        }
    }

    public int keyguardPinPasswordLength() {
        int i;
        try {
            i = (int) getLockPatternUtils().getLockSettings().getLong("lockscreen.pin_password_length", 0, getCurrentUser());
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("getLong error: ");
            sb.append(e.getMessage());
            Log.d("OpKeyguardUpdateMonitor", sb.toString());
            i = 0;
        }
        if (i >= 4) {
            return i;
        }
        return 0;
    }

    public boolean isAutoCheckPinEnabled() {
        return keyguardPinPasswordLength() != 0;
    }

    /* access modifiers changed from: private */
    public void updateFPStateBySensor(int i, boolean z) {
        int i2 = this.mPocketState;
        boolean z2 = this.mLidOpen;
        this.mPocketState = i;
        this.mLidOpen = z;
        int i3 = !z ? 1 : i;
        FingerprintManager fingerprintManager = this.mFpm;
        if (fingerprintManager != null) {
            fingerprintManager.updateStatus(i3);
        }
        if (isFingerprintDetectionRunning() && isSensorNear(i, z)) {
            updateFingerprintListeningState();
        } else if (isSensorNear(i2, z2) && !isSensorNear(i, z)) {
            updateFingerprintListeningState();
        }
    }

    private void setPocketSensorEnabled(boolean z) {
        if (isPreventModeEnabled(this.mContext)) {
            String str = "OpKeyguardUpdateMonitor";
            if (this.mLidOpen || !z) {
                int currentUser = getCurrentUser();
                FingerprintManager fingerprintManager = this.mFpm;
                boolean z2 = fingerprintManager != null && fingerprintManager.isHardwareDetected() && this.mFpm.getEnrolledFingerprints(currentUser).size() > 0;
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("listen pocket-sensor: ");
                    sb.append(z);
                    sb.append(", current=");
                    sb.append(this.mPocketSensorEnabled);
                    sb.append(", FP enabled=");
                    sb.append(z2);
                    Log.d(str, sb.toString());
                }
                if (!z2 || !z) {
                    if (this.mPocketSensorEnabled) {
                        this.mPocketSensorEnabled = false;
                        if (isSensorNear(this.mPocketState, this.mLidOpen)) {
                            this.mPocketState = 0;
                            this.mLidOpen = true;
                            updateFingerprintListeningState();
                        }
                        this.mPocketState = 0;
                        this.mLidOpen = true;
                        FingerprintManager fingerprintManager2 = this.mFpm;
                        if (fingerprintManager2 != null) {
                            fingerprintManager2.updateStatus(0);
                        }
                        this.mSensorManager.unregisterListener(this.mPocketListener);
                    } else {
                        handleLidSwitchChanged(true);
                    }
                } else if (!this.mPocketSensorEnabled) {
                    this.mPocketSensorEnabled = true;
                    this.mSensorManager.registerListener(this.mPocketListener, this.mPocketSensor, 3);
                }
                return;
            }
            Log.d(str, "not register when Lid closed");
        }
    }

    /* access modifiers changed from: private */
    public void handleLidSwitchChanged(boolean z) {
        if (z != this.mLidOpen) {
            updateFPStateBySensor(this.mPocketState, z);
        }
    }

    public void notifyLidSwitchChanged(boolean z) {
        boolean z2 = !z;
        if (IS_SUPPORT_FINGERPRINT_POCKET) {
            StringBuilder sb = new StringBuilder();
            sb.append("LidOpen: ");
            sb.append(z);
            sb.append(", pocket enabled:");
            sb.append(this.mPocketSensorEnabled);
            Log.d("OpKeyguardUpdateMonitor", sb.toString());
            getHandler().removeMessages(502);
            getHandler().sendMessage(getHandler().obtainMessage(502, z2 ? 1 : 0, 0));
        }
    }

    /* access modifiers changed from: protected */
    public void opHandleFingerprintError1(int i) {
        if (i == 7 || (IS_SUPPORT_CUSTOM_FINGERPRINT && i == 9)) {
            this.mLockoutState = true;
            if (IS_SUPPORT_CUSTOM_FINGERPRINT && isKeyguardVisible() && isUnlockWithFingerprintPossible(getCurrentUser())) {
                OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
                if (opFingerprintDialogView != null) {
                    opFingerprintDialogView.isDialogShowing();
                }
            }
        }
    }

    public boolean isPreventModeEnabled(Context context) {
        boolean z = false;
        if (!IS_SUPPORT_FINGERPRINT_POCKET) {
            return false;
        }
        try {
            if (System.getInt(context.getContentResolver(), "oem_acc_anti_misoperation_screen") != 0) {
                z = true;
            }
        } catch (SettingNotFoundException unused) {
        }
        return z;
    }

    public boolean isFirstUnlock() {
        return !getStrongAuthTracker().hasUserAuthenticatedSinceBoot();
    }

    /* access modifiers changed from: private */
    public void handlePreventModeChanged(boolean z) {
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onPreventModeChanged(z);
            }
        }
        if (IS_SUPPORT_CUSTOM_FINGERPRINT && this.mQSExpanded) {
            updateFingerprintListeningState();
        }
    }

    public void notifyPreventModeChange(boolean z) {
        boolean z2 = !z;
        this.mPreventModeActive = z;
        getHandler().removeMessages(504);
        getHandler().sendMessage(getHandler().obtainMessage(504, z2 ? 1 : 0, 0));
    }

    /* access modifiers changed from: protected */
    public void opOnFingerprintAuthenticated(int i) {
        this.mFingerprintAlreadyAuthenticated = isUnlockingWithBiometricAllowed();
        if (!isDeviceInteractive()) {
            Intent intent = new Intent("com.oneplus.systemui.aod_unlock");
            intent.setPackage("com.oneplus.wallpaper");
            this.mContext.sendBroadcast(intent);
        }
        if (OpLsState.getInstance().getPhoneStatusBar() != null) {
            OpLsState.getInstance().getPhoneStatusBar().onFingerprintAuthenticated();
        }
    }

    /* access modifiers changed from: protected */
    public void opHandleFingerprintLockoutReset() {
        if (!IS_SUPPORT_CUSTOM_FINGERPRINT || this.mFingerprintFailedAttempts.get(getCurrentUser(), 0) < 5) {
            this.mLockoutState = false;
        } else {
            Log.i("OpKeyguardUpdateMonitor", "Not reset lockout state because failed attempts bigger than max failed attepmts");
        }
    }

    /* access modifiers changed from: protected */
    public void opHandleScreenTurnedOn() {
        this.mScreenTurningOn = false;
        if (!OpUtils.isCustomFingerprint()) {
            setPocketSensorEnabled(false);
        } else if (!isDeviceInteractive()) {
            setPocketSensorEnabled(true);
        }
        if (OpUtils.isCustomFingerprint() && !isDeviceInteractive()) {
            updateFingerprintListeningState();
        }
        onScreenStatusChanged(true);
    }

    /* access modifiers changed from: protected */
    public void opHandleScreenTurnedOff() {
        this.mScreenTurningOn = false;
        if (!OpUtils.isCustomFingerprint()) {
            setPocketSensorEnabled(true);
        } else {
            setPocketSensorEnabled(false);
        }
        if (OpUtils.isCustomFingerprint()) {
            updateFingerprintListeningState();
        }
        onScreenStatusChanged(false);
    }

    public void clearFailedUnlockAttempts(boolean z) {
        this.mFailedAttempts.delete(getCurrentUser());
        clearFingerprintFailedUnlockAttempts();
        StringBuilder sb = new StringBuilder();
        sb.append("clear ");
        sb.append(z);
        sb.append(", ");
        sb.append(isFacelockDisabled());
        Log.d("OpKeyguardUpdateMonitor", sb.toString());
        if (!OpUtils.isWeakFaceUnlockEnabled()) {
            clearFailedFacelockAttempts();
        } else if (z || !isFacelockDisabled()) {
            clearFailedFacelockAttempts();
        }
    }

    public int getFailedUnlockAttempts(int i) {
        return this.mFailedAttempts.get(i, 0);
    }

    public void reportFailedStrongAuthUnlockAttempt(int i) {
        this.mFailedAttempts.put(i, getFailedUnlockAttempts(i) + 1);
        String str = "confirm_lock_password_fragment.key_num_wrong_confirm_attempts";
        Secure.putIntForUser(this.mContext.getContentResolver(), str, Secure.getIntForUser(this.mContext.getContentResolver(), str, 0, getCurrentUser()) + 1, getCurrentUser());
    }

    private void clearFingerprintFailedUnlockAttempts() {
        this.mFingerprintFailedAttempts.delete(getCurrentUser());
    }

    public int getFingerprintFailedUnlockAttempts() {
        return this.mFingerprintFailedAttempts.get(getCurrentUser(), 0);
    }

    /* access modifiers changed from: private */
    public void handleFingerprintTimeout() {
        if (DEBUG) {
            Log.d("OpKeyguardUpdateMonitor", "handleFingerprintTimeout");
        }
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintTimeout();
            }
        }
    }

    public void updateLaunchingLeftAffordance(boolean z) {
        this.mLaunchingLeftAffordance = z;
        updateFingerprintListeningState();
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            opFingerprintDialogView.updateIconVisibility(false);
        }
    }

    public boolean isLaunchingLeftAffordance() {
        return this.mLaunchingLeftAffordance;
    }

    public boolean isFingerprintAlreadyAuthenticated() {
        return this.mFingerprintAlreadyAuthenticated;
    }

    public void resetFingerprintAlreadyAuthenticated() {
        this.mFingerprintAlreadyAuthenticated = false;
    }

    public boolean isFingerprintLockout() {
        return this.mLockoutState;
    }

    /* access modifiers changed from: protected */
    public boolean opShouldListenForFingerprint() {
        String str = "OpKeyguardUpdateMonitor";
        if (IS_SUPPORT_CUSTOM_FINGERPRINT) {
            if (!(!isDeviceInteractive() && !isScreenOn()) || this.mScreenTurningOn) {
                if ((!isKeyguardVisible() || isBouncer() || this.mPreventModeActive) ? false : this.mQSExpanded) {
                    Log.d(str, "opShouldListenForFingerprint false: disableByQSExpanded");
                    return false;
                } else if (OpLsState.getInstance().getPhoneStatusBar().getPanel().isFullyExpanded() && !this.mKeyguardShowing && !isBouncer()) {
                    Log.d(str, "opShouldListenForFingerprint false: disableByPanelExpanded");
                    return false;
                } else if (!isUnlockingWithBiometricAllowed() && !isFingerprintLockout()) {
                    Log.d(str, "opShouldListenForFingerprint false: biometric not allowed");
                    return false;
                } else if (this.mShutingDown) {
                    Log.d(str, "opShouldListenForFingerprint false: Shuting Down");
                    return false;
                } else if (this.mFacelockRunningType == 4) {
                    Log.d(str, "opShouldListenForFingerprint false: Facelock RECOGNIZED_OK");
                    return false;
                } else if (this.mIsKeyguardDone && !this.mFakeLocking) {
                    Log.d(str, "opShouldListenForFingerprint false: Keyguard Done and not fake locking");
                    return false;
                } else if (this.mFacelockUnlocking) {
                    Log.d(str, "opShouldListenForFingerprint false: FacelockUnlocking");
                    return false;
                } else if (isDreaming() && isKeyguardOccluded() && isDeviceInteractive() && (isScreenSaverEnabled() || isScreenSaverActivatedOnDock())) {
                    Log.d(str, "opShouldListenForFingerprint false: screen saver is enabled");
                    return false;
                } else if (this.mPowerManager.isInteractive()) {
                    if (isKeyguardOccluded() && !isBouncer()) {
                        Log.d(str, "opShouldListenForFingerprint false: start to wake up and keyguard is occluded.");
                        return false;
                    } else if (this.mIsKeyguardDone) {
                        Log.d(str, "opShouldListenForFingerprint false: start to wake up and keyguard is done.");
                        return false;
                    }
                }
            } else {
                Log.d(str, "opShouldListenForFingerprint false: screen off");
                return false;
            }
        }
        if (isBouncer() && this.mImeShow) {
            Log.d(str, "opShouldListenForFingerprint false: IME show");
            return false;
        } else if (this.mFingerprintAlreadyAuthenticated) {
            Log.d(str, "opShouldListenForFingerprint false: FingerprintAlreadyAuthenticated");
            return false;
        } else if (isSensorNear(this.mPocketState, this.mLidOpen) && isPreventModeEnabled(this.mContext)) {
            Log.d(str, "opShouldListenForFingerprint false: prevent mode");
            return false;
        } else if (isGoingToSleep()) {
            Log.d(str, "opShouldListenForFingerprint false: going to sleep");
            return false;
        } else {
            if (!isDeviceInteractive() || isGoingToSleep()) {
                this.mLaunchingCamera = false;
            }
            if (this.mLaunchingCamera) {
                Log.d(str, "opShouldListenForFingerprint false: Launching Camera");
                return false;
            } else if (this.mLaunchingLeftAffordance) {
                Log.d(str, "opShouldListenForFingerprint false: LaunchingLeftAffordance");
                return false;
            } else if (!this.mIsInBrickMode) {
                return true;
            } else {
                Log.d(str, "opShouldListenForFingerprint false: Brick Mode");
                return false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void watchForFacelockSettings() {
        this.mFacelockSettingsObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean z) {
                super.onChange(z);
                OpKeyguardUpdateMonitor.this.updateFacelockSettings();
                StringBuilder sb = new StringBuilder();
                sb.append("facelock state = ");
                sb.append(OpKeyguardUpdateMonitor.this.mFacelockEnabled);
                String str = ", ";
                sb.append(str);
                sb.append(OpKeyguardUpdateMonitor.this.mAutoFacelockEnabled);
                sb.append(str);
                sb.append(OpKeyguardUpdateMonitor.this.mFacelockLightingEnabled);
                sb.append(str);
                sb.append(OpKeyguardUpdateMonitor.this.mBouncerRecognizeEnabled);
                sb.append(str);
                sb.append(OpKeyguardUpdateMonitor.this.mFacelockSuccessTimes);
                Log.d("OpKeyguardUpdateMonitor", sb.toString());
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_face_unlock_enable"), false, this.mFacelockSettingsObserver, 0);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_auto_face_unlock_enable"), false, this.mFacelockSettingsObserver, 0);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_face_unlock_assistive_lighting_enable"), false, this.mFacelockSettingsObserver, 0);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_face_unlock_powerkey_recognize_enable"), false, this.mFacelockSettingsObserver, 0);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_face_unlock_success_times"), false, this.mFacelockSettingsObserver, 0);
        updateFacelockSettings();
    }

    /* access modifiers changed from: private */
    public void updateFacelockSettings() {
        boolean z = true;
        this.mFacelockEnabled = System.getIntForUser(this.mContext.getContentResolver(), "oneplus_face_unlock_enable", 0, 0) == 1;
        this.mAutoFacelockEnabled = System.getIntForUser(this.mContext.getContentResolver(), "oneplus_auto_face_unlock_enable", 0, 0) == 1;
        boolean z2 = System.getIntForUser(this.mContext.getContentResolver(), "oneplus_face_unlock_assistive_lighting_enable", 0, 0) == 1;
        if (z2 != this.mFacelockLightingEnabled) {
            this.mFacelockLightingEnabled = z2;
            int size = getCallbacks().size();
            for (int i = 0; i < size; i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onFacelockLightingChanged(z2);
                }
            }
        }
        if (System.getIntForUser(this.mContext.getContentResolver(), "oneplus_face_unlock_powerkey_recognize_enable", 0, 0) != 0) {
            z = false;
        }
        this.mBouncerRecognizeEnabled = z;
        this.mFacelockSuccessTimes = System.getIntForUser(this.mContext.getContentResolver(), "oneplus_face_unlock_success_times", 0, 0);
    }

    public void notifyFacelockStateChanged(final int i) {
        final int i2 = this.mFacelockRunningType;
        this.mFacelockRunningType = i;
        String str = "OpKeyguardUpdateMonitor";
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyFacelockStateChanged, type:");
            sb.append(i);
            sb.append(", isWeak:");
            sb.append(isWeakFaceTimeout());
            Log.d(str, sb.toString());
        }
        if (isFacelockWaitingTap() && isWeakFaceTimeout()) {
            this.mFacelockRunningType = 0;
            Log.d(str, "[WeakFace] change to not running");
            i = 0;
        }
        if (i == 4 && OpUtils.isCustomFingerprint()) {
            getHandler().sendEmptyMessage(336);
        }
        getHandler().post(new Runnable() {
            public void run() {
                for (int i = 0; i < OpKeyguardUpdateMonitor.this.getCallbacks().size(); i++) {
                    KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) OpKeyguardUpdateMonitor.this.getCallbacks().get(i)).get();
                    if (keyguardUpdateMonitorCallback != null) {
                        keyguardUpdateMonitorCallback.onFacelockStateChanged(i);
                    }
                }
                int i2 = i2;
                int i3 = i;
                if (i2 != i3) {
                    if (i3 == 2) {
                        OpKeyguardUpdateMonitor.this.updateFacelockTrustState(true);
                    } else {
                        OpKeyguardUpdateMonitor.this.updateFacelockTrustState(false);
                    }
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateFacelockTrustState(boolean z) {
        this.mSkipBouncerByFacelock = z;
        StringBuilder sb = new StringBuilder();
        sb.append("FacelockTrust,");
        sb.append(z);
        Log.d("OpKeyguardUpdateMonitor", sb.toString());
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustChanged(getCurrentUser());
            }
        }
    }

    public int getFacelockRunningType() {
        return this.mFacelockRunningType;
    }

    public boolean isFacelockWaitingTap() {
        return this.mFacelockRunningType == 12;
    }

    public boolean isFacelockAvailable() {
        int i = this.mFacelockRunningType;
        return i == 5 || i == 6 || i == 7 || i == 12;
    }

    public boolean isFacelockDisabled() {
        return this.mFacelockRunningType == 1;
    }

    public boolean isFacelockRecognizing() {
        return this.mFacelockRunningType == 3;
    }

    public boolean shouldShowFacelockIcon() {
        int i = this.mFacelockRunningType;
        return i == 3 || i == 4 || i == 5 || i == 6 || i == 7 || i == 12;
    }

    public boolean isCameraErrorState() {
        int i = this.mFacelockRunningType;
        return i == 8 || i == 9 || i == 10 || i == 11;
    }

    private void clearFailedFacelockAttempts() {
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onClearFailedFacelockAttempts();
            }
        }
        notifyFacelockStateChanged(0);
    }

    public boolean isFacelockEnabled() {
        return this.mFacelockEnabled;
    }

    public boolean isAutoFacelockEnabled() {
        if (IS_SUPPORT_MOTOR_CAMERA) {
            return !this.mBouncerRecognizeEnabled;
        }
        return this.mAutoFacelockEnabled;
    }

    public boolean isBouncerRecognizeEnabled() {
        return this.mBouncerRecognizeEnabled;
    }

    public static boolean isMotorCameraSupported() {
        return IS_SUPPORT_MOTOR_CAMERA;
    }

    public boolean isFacelockLightingEnabled() {
        return this.mFacelockLightingEnabled;
    }

    public boolean isFacelockAllowed() {
        StringBuilder sb = new StringBuilder();
        sb.append("isFacelockAllowed, visible:");
        sb.append(isKeyguardVisible());
        sb.append(", inter:");
        sb.append(isDeviceInteractive());
        sb.append(", bouncer:");
        sb.append(isBouncer());
        sb.append(", done:");
        sb.append(this.mIsKeyguardDone);
        sb.append(", switching:");
        sb.append(isSwitchingUser());
        sb.append(", enabled:");
        sb.append(isFacelockEnabled());
        sb.append(", added:");
        sb.append(this.mIsFaceAdded);
        sb.append(", simpin:");
        sb.append(isSimPinSecure());
        sb.append(", user:");
        sb.append(getCurrentUser());
        sb.append(", fp authenticated:");
        sb.append(this.mFingerprintAlreadyAuthenticated);
        sb.append(", on:");
        sb.append(isScreenOn());
        sb.append(", ");
        sb.append(isWeakFaceTimeout());
        Log.d("OpKeyguardUpdateMonitor", sb.toString());
        if (!allowShowingLock() || !isDeviceInteractive() || isSwitchingUser() || ((this.mFingerprintAlreadyAuthenticated && !isScreenOn()) || isWeakFaceTimeout() || !isUnlockWithFacelockPossible())) {
            return false;
        }
        return true;
    }

    public void setIsFaceAdded(boolean z) {
        this.mIsFaceAdded = z;
    }

    public int getFacelockNotifyMsgId(int i) {
        int i2;
        if (i != 1) {
            switch (i) {
                case 5:
                case 12:
                    i2 = R$string.face_unlock_tap_to_retry;
                    break;
                case 6:
                    i2 = R$string.face_unlock_no_face;
                    break;
                case 7:
                    i2 = R$string.face_unlock_fail;
                    break;
                case 8:
                    i2 = R$string.face_unlock_camera_error;
                    break;
                case 9:
                    i2 = R$string.face_unlock_no_permission;
                    break;
                case 10:
                    i2 = R$string.face_unlock_retry_other;
                    break;
                case 11:
                    i2 = R$string.face_unlock_retry_other;
                    break;
                default:
                    return 0;
            }
        } else {
            i2 = R$string.face_unlock_timeout;
        }
        return i2;
    }

    public boolean shouldPlayFacelockFailAnim() {
        int i = this.mFacelockRunningType;
        return i == 1 || i == 6 || i == 7 || i == 8 || i == 9 || i == 10 || i == 11;
    }

    public boolean canSkipBouncerByFacelock() {
        return this.mSkipBouncerByFacelock;
    }

    public void onFacelockUnlocking(boolean z) {
        this.mFacelockUnlocking = z;
    }

    public boolean isFacelockUnlocking() {
        return this.mFacelockUnlocking;
    }

    public boolean isUnlockWithFacelockPossible() {
        return isFacelockEnabled() && this.mIsFaceAdded && getLockPatternUtils().isSecure(getCurrentUser()) && !isSimPinSecure() && getCurrentUser() == 0;
    }

    public boolean isFaceAdded() {
        return this.mIsFaceAdded;
    }

    public void notifyPasswordLockout() {
        for (int i = 0; i < getCallbacks().size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) getCallbacks().get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onPasswordLockout();
            }
        }
    }

    public boolean allowShowingLock() {
        if (isKeyguardVisible()) {
            return true;
        }
        if (!isBouncer() || isKeyguardGoingAway() || isForegroundApp("com.oneplus.camera")) {
            return false;
        }
        return true;
    }

    private boolean isForegroundApp(String str) {
        boolean z = false;
        if (str == null) {
            return false;
        }
        List runningTasks = ((ActivityManager) this.mContext.getSystemService(ActivityManager.class)).getRunningTasks(1);
        if (!runningTasks.isEmpty() && str.equals(((RunningTaskInfo) runningTasks.get(0)).topActivity.getPackageName())) {
            z = true;
        }
        return z;
    }

    public void reportFaceUnlock() {
        if (this.mFacelockSuccessTimes < 3) {
            System.putIntForUser(this.mContext.getContentResolver(), "oneplus_face_unlock_success_times", this.mFacelockSuccessTimes + 1, getCurrentUser());
            this.mFacelockSuccessTimes++;
        }
    }

    public boolean getFacelockNoticeEnabled() {
        boolean z = false;
        if (!isBouncerRecognizeEnabled()) {
            return false;
        }
        if (this.mFacelockSuccessTimes < 3) {
            z = true;
        }
        return z;
    }

    public void setWakingUpReason(String str) {
        this.mWakingUpReason = str;
    }

    public String getWakingUpReason() {
        return this.mWakingUpReason;
    }

    public void onBrickModeChanged(boolean z) {
        this.mIsInBrickMode = z;
    }

    /* access modifiers changed from: protected */
    public void opDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        int currentUser = getCurrentUser();
        FingerprintManager fingerprintManager = this.mFpm;
        if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
            getStrongAuthTracker().getStrongAuthForUser(currentUser);
            StringBuilder sb = new StringBuilder();
            sb.append("    FingerprintFailedAttempts=");
            sb.append(getFingerprintFailedUnlockAttempts());
            printWriter.println(sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("    mPocketSensorEnabled=");
            sb2.append(this.mPocketSensorEnabled);
            printWriter.println(sb2.toString());
            StringBuilder sb3 = new StringBuilder();
            sb3.append("    mPocketState=");
            sb3.append(this.mPocketState);
            printWriter.println(sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            sb4.append("    mLaunchingCamera=");
            sb4.append(this.mLaunchingCamera);
            printWriter.println(sb4.toString());
            StringBuilder sb5 = new StringBuilder();
            sb5.append("    mDuringAcquired=");
            sb5.append(this.mDuringAcquired);
            printWriter.println(sb5.toString());
            StringBuilder sb6 = new StringBuilder();
            sb6.append("    mLockoutState=");
            sb6.append(this.mLockoutState);
            printWriter.println(sb6.toString());
            StringBuilder sb7 = new StringBuilder();
            sb7.append("    mFingerprintAlreadyAuthenticated=");
            sb7.append(this.mFingerprintAlreadyAuthenticated);
            printWriter.println(sb7.toString());
            StringBuilder sb8 = new StringBuilder();
            sb8.append("    EnrollSize=");
            sb8.append(this.mFpm.getEnrolledFingerprints(currentUser).size());
            printWriter.println(sb8.toString());
        }
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            opFingerprintDialogView.dump(printWriter);
        }
        StringBuilder sb9 = new StringBuilder();
        sb9.append("    mBatteryStatus=");
        sb9.append(getBatteryStatus().status);
        sb9.append(", level=");
        sb9.append(getBatteryStatus().level);
        sb9.append(", plugged=");
        sb9.append(getBatteryStatus().plugged);
        String str = ", health=";
        sb9.append(str);
        sb9.append(getBatteryStatus().health);
        sb9.append(", fastCharge=");
        sb9.append(getBatteryStatus().fastCharge);
        sb9.append(str);
        sb9.append(getBatteryStatus().health);
        sb9.append(", maxChargingWattage=");
        sb9.append(getBatteryStatus().maxChargingWattage);
        printWriter.println(sb9.toString());
        StringBuilder sb10 = new StringBuilder();
        sb10.append("    mKeyguardIsVisible=");
        sb10.append(isKeyguardIsVisible());
        printWriter.println(sb10.toString());
        StringBuilder sb11 = new StringBuilder();
        sb11.append("    mBootCompleted=");
        sb11.append(isBootCompleted());
        printWriter.println(sb11.toString());
        StringBuilder sb12 = new StringBuilder();
        sb12.append("    mGoingToSleep=");
        sb12.append(isGoingToSleep());
        printWriter.println(sb12.toString());
        StringBuilder sb13 = new StringBuilder();
        sb13.append("    isPreventModeEnabled=");
        sb13.append(isPreventModeEnabled(this.mContext));
        printWriter.println(sb13.toString());
        StringBuilder sb14 = new StringBuilder();
        sb14.append("    mPreventModeActive=");
        sb14.append(this.mPreventModeActive);
        printWriter.println(sb14.toString());
        StringBuilder sb15 = new StringBuilder();
        sb15.append("    mDeviceProvisioned=");
        sb15.append(isDeviceProvisioned());
        printWriter.println(sb15.toString());
        StringBuilder sb16 = new StringBuilder();
        sb16.append("    getFailedUnlockAttempts=");
        sb16.append(getFailedUnlockAttempts(currentUser));
        printWriter.println(sb16.toString());
        StringBuilder sb17 = new StringBuilder();
        sb17.append("    isBootCompleted()=");
        sb17.append(getUserCanSkipBouncer(currentUser));
        printWriter.println(sb17.toString());
        StringBuilder sb18 = new StringBuilder();
        sb18.append("    mDeviceInteractive=");
        sb18.append(isDeviceInteractive());
        printWriter.println(sb18.toString());
        StringBuilder sb19 = new StringBuilder();
        sb19.append("    mScreenOn=");
        sb19.append(isScreenOn());
        printWriter.println(sb19.toString());
        StringBuilder sb20 = new StringBuilder();
        sb20.append("    mIsKeyguardDone=");
        sb20.append(this.mIsKeyguardDone);
        printWriter.println(sb20.toString());
        StringBuilder sb21 = new StringBuilder();
        sb21.append("    IS_SUPPORT_BOOT_TO_ENTER_BOUNCER=");
        sb21.append(IS_SUPPORT_BOOT_TO_ENTER_BOUNCER);
        printWriter.println(sb21.toString());
        StringBuilder sb22 = new StringBuilder();
        sb22.append("    mIsUserUnlocked=");
        sb22.append(isUserUnlocked());
        printWriter.println(sb22.toString());
        StringBuilder sb23 = new StringBuilder();
        sb23.append("    mSimUnlockSlot0=");
        sb23.append(this.mSimUnlockSlot0);
        printWriter.println(sb23.toString());
        StringBuilder sb24 = new StringBuilder();
        sb24.append("    mSimUnlockSlot1=");
        sb24.append(this.mSimUnlockSlot1);
        printWriter.println(sb24.toString());
        StringBuilder sb25 = new StringBuilder();
        sb25.append("    mPendingSubInfoChange=");
        sb25.append(this.mPendingSubInfoChange);
        printWriter.println(sb25.toString());
        StringBuilder sb26 = new StringBuilder();
        sb26.append("    IS_SUPPORT_FACE_UNLOCK=");
        sb26.append(IS_SUPPORT_FACE_UNLOCK);
        printWriter.println(sb26.toString());
        StringBuilder sb27 = new StringBuilder();
        sb27.append("    mIsFaceAdded=");
        sb27.append(this.mIsFaceAdded);
        printWriter.println(sb27.toString());
        StringBuilder sb28 = new StringBuilder();
        sb28.append("    mIsWeakFaceTimeout=");
        sb28.append(isWeakFaceTimeout());
        printWriter.println(sb28.toString());
        StringBuilder sb29 = new StringBuilder();
        sb29.append("    mFacelockRunningType=");
        sb29.append(this.mFacelockRunningType);
        printWriter.println(sb29.toString());
        StringBuilder sb30 = new StringBuilder();
        sb30.append("    isSecure=");
        sb30.append(getLockPatternUtils().isSecure(getCurrentUser()));
        printWriter.println(sb30.toString());
        StringBuilder sb31 = new StringBuilder();
        sb31.append("    getCurrentUser=");
        sb31.append(getCurrentUser());
        printWriter.println(sb31.toString());
        StringBuilder sb32 = new StringBuilder();
        sb32.append("    mSkipBouncerByFacelock=");
        sb32.append(this.mSkipBouncerByFacelock);
        printWriter.println(sb32.toString());
        StringBuilder sb33 = new StringBuilder();
        sb33.append("    mFacelockUnlocking=");
        sb33.append(this.mFacelockUnlocking);
        printWriter.println(sb33.toString());
        StringBuilder sb34 = new StringBuilder();
        sb34.append("    mBouncerRecognizeEnabled=");
        sb34.append(this.mBouncerRecognizeEnabled);
        printWriter.println(sb34.toString());
        StringBuilder sb35 = new StringBuilder();
        sb35.append("    mFacelockTimes=");
        sb35.append(this.mFacelockSuccessTimes);
        printWriter.println(sb35.toString());
        StringBuilder sb36 = new StringBuilder();
        sb36.append("    IS_SUPPORT_FINGERPRINT_POCKET=");
        sb36.append(IS_SUPPORT_FINGERPRINT_POCKET);
        printWriter.println(sb36.toString());
        StringBuilder sb37 = new StringBuilder();
        sb37.append("    IS_SUPPORT_MOTOR_CAMERA=");
        sb37.append(IS_SUPPORT_MOTOR_CAMERA);
        printWriter.println(sb37.toString());
        StringBuilder sb38 = new StringBuilder();
        sb38.append("    isFacelockDisabled=");
        sb38.append(isFacelockDisabled());
        printWriter.println(sb38.toString());
        StringBuilder sb39 = new StringBuilder();
        sb39.append("    isUnlockWithFacelockPossible=");
        sb39.append(isUnlockWithFacelockPossible());
        printWriter.println(sb39.toString());
        StringBuilder sb40 = new StringBuilder();
        sb40.append("    isWeakFaceUnlockEnabled=");
        sb40.append(OpUtils.isWeakFaceUnlockEnabled());
        printWriter.println(sb40.toString());
        if (SystemProperties.getInt("sys.debug.systemui.pin", 0) == 56) {
            StringBuilder sb41 = new StringBuilder();
            sb41.append("    length=");
            sb41.append(keyguardPinPasswordLength());
            printWriter.println(sb41.toString());
        }
    }

    private boolean isScreenSaverEnabled() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "screensaver_enabled", this.mContext.getResources().getBoolean(17891424) ? 1 : 0, -2) != 0;
    }

    private boolean isScreenSaverActivatedOnDock() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "screensaver_activate_on_dock", this.mContext.getResources().getBoolean(17891422) ? 1 : 0, -2) != 0;
    }

    public HashMap<Integer, ServiceState> opGetServiceStates() {
        return getServiceStates();
    }

    /* access modifiers changed from: private */
    public void updateFingerprintListeningState() {
        OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "updateFingerprintListeningState", new Object[0]);
    }

    /* access modifiers changed from: private */
    public OpHandler getHandler() {
        return (OpHandler) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mHandler");
    }

    private Runnable getUpdateBiometricListeningStateRunnable() {
        return (Runnable) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mUpdateBiometricListeningState");
    }

    /* access modifiers changed from: private */
    public ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> getCallbacks() {
        return (ArrayList) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mCallbacks");
    }

    private boolean isKeyguardVisible() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "isKeyguardVisible", new Object[0])).booleanValue();
    }

    private boolean isDeviceInteractive() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mDeviceInteractive")).booleanValue();
    }

    private boolean isDreaming() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mIsDreaming")).booleanValue();
    }

    private boolean isKeyguardOccluded() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mKeyguardOccluded")).booleanValue();
    }

    private boolean isKeyguardGoingAway() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mKeyguardGoingAway")).booleanValue();
    }

    private int getCurrentUser() {
        return ((Integer) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "sCurrentUser")).intValue();
    }

    private LockPatternUtils getLockPatternUtils() {
        return (LockPatternUtils) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mLockPatternUtils");
    }

    private StrongAuthTracker getStrongAuthTracker() {
        return (StrongAuthTracker) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mStrongAuthTracker");
    }

    private boolean isFingerprintDetectionRunning() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "isFingerprintDetectionRunning", new Object[0])).booleanValue();
    }

    private boolean isScreenOn() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "isScreenOn", new Object[0])).booleanValue();
    }

    private boolean isBouncer() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mBouncer")).booleanValue();
    }

    private boolean isUnlockWithFingerprintPossible(int i) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardUpdateMonitor.class, "isUnlockWithFingerprintPossible", Integer.TYPE), Integer.valueOf(i))).booleanValue();
    }

    private void setSwitchingUser(boolean z) {
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardUpdateMonitor.class, "setSwitchingUser", Boolean.TYPE), Boolean.valueOf(z));
    }

    private BatteryStatus getBatteryStatus() {
        return (BatteryStatus) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mBatteryStatus");
    }

    private boolean isKeyguardIsVisible() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mKeyguardIsVisible")).booleanValue();
    }

    private boolean isBootCompleted() {
        return ((Boolean) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mBootCompleted")).booleanValue();
    }

    private boolean isDeviceProvisioned() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "isDeviceProvisioned", new Object[0])).booleanValue();
    }

    private boolean getUserCanSkipBouncer(int i) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardUpdateMonitor.class, "getUserCanSkipBouncer", Integer.TYPE), Integer.valueOf(i))).booleanValue();
    }

    private void handleSimSubscriptionInfoChanged() {
        OpReflectionUtils.methodInvokeVoid(KeyguardUpdateMonitor.class, this, "handleSimSubscriptionInfoChanged", new Object[0]);
    }

    private void handleSimStateChange(int i, int i2, State state) {
        Class cls = Integer.TYPE;
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardUpdateMonitor.class, "handleSimStateChange", cls, cls, State.class), Integer.valueOf(i), Integer.valueOf(i2), state);
    }

    private HashMap<Integer, ServiceState> getServiceStates() {
        return (HashMap) OpReflectionUtils.getValue(KeyguardUpdateMonitor.class, this, "mServiceStates");
    }

    private void reportMDMEvent() {
        StringBuilder sb = new StringBuilder();
        sb.append("reportMDMEvent: mMeaWakingUpTime is null = ");
        sb.append(this.mMeaWakingUpTime == null ? "true" : "false");
        Log.i("OpKeyguardUpdateMonitor", sb.toString());
        if (this.mMeaWakingUpTime != null) {
            Calendar instance = Calendar.getInstance();
            KeyguardUpdateMonitor.getCurrentUser();
            OpMdmLogger.log("AOD", "keyguard_temp", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(instance.getTimeInMillis() - this.mMeaWakingUpTime.getTimeInMillis())), "YLTI9SVG4L");
        }
    }
}
