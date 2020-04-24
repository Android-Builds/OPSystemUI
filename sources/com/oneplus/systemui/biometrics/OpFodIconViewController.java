package com.oneplus.systemui.biometrics;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle.Observer;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.aod.OpAodDisplayViewManager;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.biometrics.OpFodHelper.OnFingerprintStateChangeListener;
import com.oneplus.util.OpUtils;

public class OpFodIconViewController implements OnFingerprintStateChangeListener {
    private AnimatorUpdateListener mAnimatorUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            OpFodIconViewController.this.mIconNormal.setAlpha(Float.parseFloat(valueAnimator.getAnimatedValue().toString()));
        }
    };
    private OpAodDisplayViewManager mAodDisplayViewManager;
    private Context mContext;
    /* access modifiers changed from: private */
    public ContentResolver mContextResolver;
    private OpBiometricDialogImpl mDialogImpl;
    /* access modifiers changed from: private */
    public boolean mGoingToSleep;
    private OpCircleImageView mIconDim;
    /* access modifiers changed from: private */
    public OpCircleImageView mIconDisable;
    private OpCircleImageView mIconFlash;
    /* access modifiers changed from: private */
    public OpCircleImageView mIconNormal;
    /* access modifiers changed from: private */
    public boolean mIsScreenTurningOn;
    /* access modifiers changed from: private */
    public boolean mIsWakingUp;
    KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStartedWakingUp() {
            super.onStartedWakingUp();
            OpFodIconViewController.this.mIsWakingUp = true;
            if (OpFodIconViewController.this.mIconDisable != null) {
                OpFodIconViewController.this.mIconDisable.setAlpha(0.2f);
            }
        }

        public void onStartedGoingToSleep(int i) {
            super.onStartedGoingToSleep(i);
            OpFodIconViewController.this.mGoingToSleep = true;
            OpFodIconViewController.this.mIsWakingUp = false;
        }

        public void onFinishedGoingToSleep(int i) {
            super.onFinishedGoingToSleep(i);
            OpFodIconViewController.this.mGoingToSleep = false;
            if (OpFodIconViewController.this.mIconDisable != null) {
                OpFodIconViewController.this.mIconDisable.setAlpha(0.2f);
            }
        }

        public void onScreenTurnedOn() {
            super.onScreenTurnedOn();
            OpFodIconViewController.this.mIsScreenTurningOn = false;
        }

        public void onScreenTurnedOff() {
            super.onScreenTurnedOff();
            OpFodIconViewController.this.mIsScreenTurningOn = false;
        }

        public void onScreenTurningOn() {
            super.onScreenTurningOn();
            OpFodIconViewController.this.mIsScreenTurningOn = true;
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            super.onKeyguardVisibilityChanged(z);
            OpFodIconViewController.this.mShowingKeyguard = z;
        }
    };
    private PowerManager mPowerManager;
    private SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public boolean mShowingKeyguard;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private ValueAnimator mTimeoutAnimator;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private WakefulnessLifecycle mWakefulnessLifecycle;
    final Observer mWakefulnessObserver = new Observer() {
        public void onFinishedWakingUp() {
            OpFodIconViewController.this.mIsWakingUp = false;
        }
    };

    private final class SettingsObserver extends ContentObserver {
        public final Uri fpScreenTimeoutAnimation = System.getUriFor("fp_screen_time_out");

        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            OpFodIconViewController.this.mContextResolver.registerContentObserver(this.fpScreenTimeoutAnimation, false, this);
        }

        public void onChange(boolean z) {
            super.onChange(z);
        }

        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            int i = System.getInt(OpFodIconViewController.this.mContextResolver, "fp_screen_time_out", 0);
        }
    }

    public OpFodIconViewController(Context context, ViewGroup viewGroup, ViewGroup viewGroup2, OpBiometricDialogImpl opBiometricDialogImpl) {
        this.mContext = context;
        this.mIconFlash = (OpCircleImageView) viewGroup.findViewById(R$id.op_fingerprint_icon_white);
        this.mIconNormal = (OpCircleImageView) viewGroup2.findViewById(R$id.op_fingerprint_icon);
        this.mIconDisable = (OpCircleImageView) viewGroup2.findViewById(R$id.op_fingerprint_icon_disable);
        this.mIconDim = (OpCircleImageView) viewGroup2.findViewById(R$id.op_fingerprint_icon_dim);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        this.mStatusBarKeyguardViewManager = OpLsState.getInstance().getStatusBarKeyguardViewManager();
        this.mAodDisplayViewManager = OpLsState.getInstance().getPhoneStatusBar().getAodDisplayViewManager();
        this.mDialogImpl = opBiometricDialogImpl;
        this.mContextResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mContext.getMainThreadHandler());
        this.mTimeoutAnimator = ValueAnimator.ofFloat(new float[]{0.9f, 1.0f});
        this.mTimeoutAnimator.setDuration(2000);
        this.mTimeoutAnimator.addUpdateListener(this.mAnimatorUpdateListener);
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver != null) {
            settingsObserver.register();
        }
        OpFodHelper.getInstance().addFingerprintStateChangeListener(this);
    }

    public void onFingerprintStateChanged() {
        handleUpdateIconVisibility(false);
    }

    public void handleUpdateLayoutDimension() {
        int i;
        int i2;
        boolean is2KResolution = OpUtils.is2KResolution();
        Resources resources = this.mContext.getResources();
        if (is2KResolution) {
            i = R$dimen.op_biometric_icon_normal_width_2k;
        } else {
            i = R$dimen.op_biometric_icon_normal_width_1080p;
        }
        int dimension = (int) resources.getDimension(i);
        LayoutParams layoutParams = this.mIconDisable.getLayoutParams();
        layoutParams.height = dimension;
        layoutParams.width = dimension;
        this.mIconDisable.setLayoutParams(layoutParams);
        this.mIconDisable.updateLayoutDimension(is2KResolution);
        LayoutParams layoutParams2 = this.mIconNormal.getLayoutParams();
        layoutParams2.height = dimension;
        layoutParams2.width = dimension;
        this.mIconNormal.setLayoutParams(layoutParams2);
        this.mIconNormal.updateLayoutDimension(is2KResolution);
        LayoutParams layoutParams3 = this.mIconDim.getLayoutParams();
        layoutParams3.height = dimension;
        layoutParams3.width = dimension;
        this.mIconDim.setLayoutParams(layoutParams3);
        this.mIconDim.updateLayoutDimension(is2KResolution);
        Resources resources2 = this.mContext.getResources();
        if (is2KResolution) {
            i2 = R$dimen.op_biometric_icon_flash_width_2k;
        } else {
            i2 = R$dimen.op_biometric_icon_flash_width_1080p;
        }
        int dimension2 = (int) resources2.getDimension(i2);
        LayoutParams layoutParams4 = this.mIconFlash.getLayoutParams();
        layoutParams4.height = dimension2;
        layoutParams4.width = dimension2;
    }

    public void handleUpdateIconVisibility(boolean z) {
        String str;
        boolean z2 = z;
        if (this.mUpdateMonitor == null) {
            this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        }
        String str2 = "OpFodIconViewController";
        if (this.mIconNormal == null || this.mAodDisplayViewManager == null || this.mIconDisable == null || this.mStatusBarKeyguardViewManager == null) {
            String str3 = str2;
            boolean z3 = false;
            StringBuilder sb = new StringBuilder();
            sb.append("not update when icon null, ");
            sb.append(this.mIconNormal == null);
            sb.append(", ");
            if (this.mIconDisable == null) {
                z3 = true;
            }
            sb.append(z3);
            Log.i(str3, sb.toString());
            return;
        }
        boolean isInteractive = this.mPowerManager.isInteractive();
        boolean isUnlockingWithBiometricAllowed = this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isImeShow = this.mUpdateMonitor.isImeShow();
        boolean isSimPinSecure = this.mUpdateMonitor.isSimPinSecure();
        boolean isDreaming = this.mUpdateMonitor.isDreaming();
        boolean isUserUnlocked = this.mUpdateMonitor.isUserUnlocked();
        boolean isQSExpanded = this.mUpdateMonitor.isQSExpanded();
        boolean isPreventModeActivte = this.mUpdateMonitor.isPreventModeActivte();
        boolean isFacelockRecognizing = this.mUpdateMonitor.isFacelockRecognizing();
        boolean isLaunchingCamera = this.mUpdateMonitor.isLaunchingCamera();
        boolean isLaunchingLeftAffordance = this.mUpdateMonitor.isLaunchingLeftAffordance();
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean z4 = isUserUnlocked;
        boolean isScreenOn = this.mUpdateMonitor.isScreenOn();
        String str4 = str2;
        boolean isKeyguardDone = this.mUpdateMonitor.isKeyguardDone();
        boolean isFaceUnlocked = this.mDialogImpl.isFaceUnlocked();
        boolean z5 = isFacelockRecognizing;
        boolean isShuttingDown = this.mUpdateMonitor.isShuttingDown();
        boolean isFromBiometricPrompt = OpFodHelper.getInstance().isFromBiometricPrompt();
        boolean isBiometricPromptReadyToShow = OpFodHelper.getInstance().isBiometricPromptReadyToShow();
        boolean isFingerprintSuspended = OpFodHelper.getInstance().isFingerprintSuspended();
        boolean z6 = isShowing && this.mStatusBarKeyguardViewManager.isOccluded();
        boolean z7 = isShowing;
        boolean isFingerprintStopped = OpFodHelper.getInstance().isFingerprintStopped();
        StringBuilder sb2 = new StringBuilder();
        boolean z8 = isPreventModeActivte;
        sb2.append("updateIconVisibility: fp client = ");
        sb2.append(OpFodHelper.getInstance().getCurrentOwner());
        sb2.append(", isOwnerKeyguard = ");
        sb2.append(OpFodHelper.getInstance().isKeyguardClient());
        sb2.append(", forceHide = ");
        sb2.append(z2);
        sb2.append(", isBouncer = ");
        sb2.append(isBouncerShowing);
        sb2.append(", isImeShow = ");
        sb2.append(isImeShow);
        sb2.append(", goingToSleep = ");
        sb2.append(this.mGoingToSleep);
        sb2.append(", screenOn = ");
        sb2.append(isScreenOn);
        sb2.append(", isUnlockAllowed = ");
        sb2.append(isUnlockingWithBiometricAllowed);
        sb2.append(", interactive = ");
        sb2.append(isInteractive);
        sb2.append(", keyguard visible = ");
        sb2.append(this.mShowingKeyguard);
        sb2.append(", isDreaming = ");
        sb2.append(isDreaming);
        sb2.append(", isKeyguardShowingAndOccluded = ");
        sb2.append(z6);
        sb2.append(", isFaceUnlocked = ");
        sb2.append(isFaceUnlocked);
        sb2.append(", isSimPinSecure = ");
        sb2.append(isSimPinSecure);
        sb2.append(", isQSExpanded = ");
        sb2.append(isQSExpanded);
        sb2.append(", isLaunchingCamera = ");
        sb2.append(isLaunchingCamera);
        sb2.append(", LeftAffordance:");
        sb2.append(isLaunchingLeftAffordance);
        sb2.append(", isPreventActivte = ");
        boolean z9 = z8;
        sb2.append(z9);
        boolean z10 = isDreaming;
        sb2.append(", isShowing = ");
        boolean z11 = z7;
        sb2.append(z11);
        boolean z12 = isFaceUnlocked;
        sb2.append(", isLockOut = ");
        sb2.append(this.mUpdateMonitor.isFingerprintLockout());
        sb2.append(", isFacelockRecognizing = ");
        sb2.append(z5);
        sb2.append(", mIsScreenTurningOn = ");
        sb2.append(this.mIsScreenTurningOn);
        sb2.append(", mIsWakingUp = ");
        sb2.append(this.mIsWakingUp);
        sb2.append(", isShuttingDown = ");
        boolean z13 = isShuttingDown;
        sb2.append(z13);
        boolean z14 = isUnlockingWithBiometricAllowed;
        sb2.append(", isFromBiometricPrompt = ");
        boolean z15 = isFromBiometricPrompt;
        sb2.append(z15);
        boolean z16 = isImeShow;
        sb2.append(", isBiometricPromptReadyToShow = ");
        boolean z17 = isBiometricPromptReadyToShow;
        sb2.append(z17);
        boolean z18 = isQSExpanded;
        sb2.append(", isFpSuspended = ");
        sb2.append(isFingerprintSuspended);
        sb2.append(", isFpStopped = ");
        sb2.append(isFingerprintStopped);
        sb2.append(", visibility = ");
        sb2.append(this.mIconNormal.getVisibility());
        String str5 = str4;
        Log.d(str5, sb2.toString());
        if (z2) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(4);
            str = "1";
        } else if (!this.mDialogImpl.isDialogShowing() || this.mDialogImpl.isAuthenticateSuccess() || z13) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(4);
            str = "2";
        } else if (z15 && !z17) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(4);
            str = "9";
        } else if (((isInteractive || this.mGoingToSleep) && z6 && !isBouncerShowing) || isSimPinSecure || isLaunchingCamera || isLaunchingLeftAffordance || ((isInteractive && !z11 && OpFodHelper.getInstance().isKeyguardClient() && (isScreenOn || this.mIsWakingUp)) || ((isInteractive && isScreenOn && !z11 && OpFodHelper.getInstance().isForceShowClient()) || ((isInteractive && !z9 && ((z18 && !isBouncerShowing && z4) || z16)) || isFingerprintSuspended || ((!OpFodHelper.getInstance().isEmptyClient() && isFingerprintStopped && z14) || !isScreenOn))))) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(4);
            this.mDialogImpl.updateTransparentIconVisibility(8);
            str = "3";
        } else if (z12) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(4);
            str = "4";
        } else if (this.mUpdateMonitor.isFingerprintLockout()) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(0);
            str = "5";
        } else if (!z14 && !isKeyguardDone && OpFodHelper.getInstance().isKeyguardClient()) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            this.mIconDisable.setVisibility(0);
            this.mDialogImpl.updateTransparentIconVisibility(8);
            str = "6";
        } else if (OpFodHelper.getInstance().isEmptyClient()) {
            this.mIconNormal.setVisibility(4);
            this.mIconDim.setVisibility(4);
            str = "7";
        } else {
            if (this.mIconNormal.getVisibility() == 4) {
                if (!OpFodHelper.getInstance().isKeyguardClient()) {
                    this.mIconNormal.setVisibility(0);
                    this.mIconDim.setVisibility(0);
                    this.mIconDisable.setVisibility(4);
                    this.mDialogImpl.updateTransparentIconVisibility(0);
                    str = "8-2";
                } else if (!this.mShowingKeyguard || !z5 || isScreenOn) {
                    boolean z19 = this.mShowingKeyguard;
                    if (z19 || z10 || (!z19 && isBouncerShowing)) {
                        this.mIconNormal.setVisibility(0);
                        this.mIconDim.setVisibility(0);
                        this.mIconDisable.setVisibility(4);
                        this.mDialogImpl.updateTransparentIconVisibility(0);
                        str = "8-1";
                    }
                } else {
                    this.mIconNormal.setVisibility(4);
                    this.mIconDim.setVisibility(4);
                    this.mIconDisable.setVisibility(4);
                    str = "8-0";
                }
            }
            str = "0";
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("caseLog: ");
        sb3.append(str);
        Log.d(str5, sb3.toString());
    }

    public void onBrightnessChange() {
        OpCircleImageView opCircleImageView = this.mIconDim;
        if (opCircleImageView != null) {
            opCircleImageView.onBrightnessChange();
        }
    }
}
