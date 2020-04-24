package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.biometrics.BiometricSourceType;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.policy.IKeyguardStateCallback.Stub;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.ViewClippingUtil.ClippingParameters;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitor.BatteryStatus;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$anim;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$raw;
import com.android.systemui.R$string;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.IllegalFormatConversionException;
import java.util.Locale;

public class KeyguardIndicationController implements StateListener, ConfigurationListener, OnUnlockMethodChangedListener {
    private static int CHARGING_INFO_ANITMAION_DURATION = 100;
    private final AccessibilityController mAccessibilityController;
    private AudioManager mAudioManager;
    private final IBatteryStats mBatteryInfo;
    /* access modifiers changed from: private */
    public int mBatteryLevel;
    /* access modifiers changed from: private */
    public TextView mChargingInfo;
    /* access modifiers changed from: private */
    public ValueAnimator mChargingInfofadeInAnimation;
    /* access modifiers changed from: private */
    public ValueAnimator mChargingInfofadeOutAnimation;
    private SoundPool mChargingSound;
    private int mChargingSoundId;
    /* access modifiers changed from: private */
    public int mChargingSpeed;
    /* access modifiers changed from: private */
    public int mChargingWattage;
    private final ClippingParameters mClippingParams;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public AnimationDrawable mDashAnimation;
    /* access modifiers changed from: private */
    public FrameLayout mDashContainer;
    /* access modifiers changed from: private */
    public ImageView mDashView;
    private final DevicePolicyManager mDevicePolicyManager;
    private KeyguardIndicationTextView mDisclosure;
    /* access modifiers changed from: private */
    public boolean mDozing;
    Animator mErrorAnimator;
    AnimatorSet mFadeOutAnimatorSet;
    /* access modifiers changed from: private */
    public final int mFastThreshold;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    /* access modifiers changed from: private */
    public ViewGroup mIndicationArea;
    /* access modifiers changed from: private */
    public ViewGroup mInfoView;
    /* access modifiers changed from: private */
    public ColorStateList mInitialTextColorState;
    private KeyguardBottomAreaView mKeyguardBottomArea;
    private KeyguardStateCallback mKeyguardStateCallback;
    /* access modifiers changed from: private */
    public KeyguardStatusView mKeyguardStatusView;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    /* access modifiers changed from: private */
    public int mLastChargingSpeed;
    /* access modifiers changed from: private */
    public final LockIcon mLockIcon;
    private final LockPatternUtils mLockPatternUtils;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    /* access modifiers changed from: private */
    public String mMessageToShowOnScreenOn;
    private View mOwnerInfo;
    /* access modifiers changed from: private */
    public boolean mPdCharging;
    /* access modifiers changed from: private */
    public boolean mPlaySoundDone;
    /* access modifiers changed from: private */
    public boolean mPowerCharged;
    /* access modifiers changed from: private */
    public boolean mPowerPluggedIn;
    /* access modifiers changed from: private */
    public boolean mPowerPluggedInWired;
    private String mRestingIndication;
    private final ShadeController mShadeController;
    private boolean mShowMsgWhenExiting;
    /* access modifiers changed from: private */
    public boolean mShowingError;
    /* access modifiers changed from: private */
    public final int mSlowThreshold;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final StatusBarStateController mStatusBarStateController;
    private KeyguardIndicationTextView mTextView;
    private final KeyguardUpdateMonitorCallback mTickReceiver;
    private CharSequence mTransientIndication;
    private ColorStateList mTransientTextColorState;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private final UserManager mUserManager;
    /* access modifiers changed from: private */
    public boolean mVisible;
    private final SettableWakeLock mWakeLock;

    protected class BaseKeyguardCallback extends KeyguardUpdateMonitorCallback {
        protected BaseKeyguardCallback() {
        }

        public void onRefreshBatteryInfo(BatteryStatus batteryStatus) {
            int i = batteryStatus.status;
            boolean z = i == 2 || i == 5;
            boolean access$1400 = KeyguardIndicationController.this.mPowerPluggedIn;
            KeyguardIndicationController.this.mPowerPluggedInWired = batteryStatus.isPluggedInWired() && z;
            KeyguardIndicationController.this.mPowerPluggedIn = batteryStatus.isPluggedIn() && z;
            KeyguardIndicationController.this.mPowerCharged = batteryStatus.isCharged();
            KeyguardIndicationController.this.mChargingWattage = batteryStatus.maxChargingWattage;
            KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
            keyguardIndicationController.mLastChargingSpeed = keyguardIndicationController.mChargingSpeed;
            KeyguardIndicationController keyguardIndicationController2 = KeyguardIndicationController.this;
            keyguardIndicationController2.mChargingSpeed = batteryStatus.getChargingSpeed(keyguardIndicationController2.mSlowThreshold, KeyguardIndicationController.this.mFastThreshold);
            KeyguardIndicationController.this.mBatteryLevel = batteryStatus.level;
            KeyguardIndicationController.this.mPdCharging = batteryStatus.isPdCharging();
            StringBuilder sb = new StringBuilder();
            sb.append("onRefreshBatteryInfo: plugged:");
            sb.append(KeyguardIndicationController.this.mPowerPluggedIn);
            sb.append(", wasPluggedIn: ");
            sb.append(access$1400);
            sb.append(", charged:");
            sb.append(KeyguardIndicationController.this.mPowerCharged);
            sb.append(", level:");
            sb.append(KeyguardIndicationController.this.mBatteryLevel);
            sb.append(", speed:");
            sb.append(KeyguardIndicationController.this.mChargingSpeed);
            sb.append(", last speed:");
            sb.append(KeyguardIndicationController.this.mLastChargingSpeed);
            sb.append(", visible:");
            sb.append(KeyguardIndicationController.this.mVisible);
            sb.append(", isPdCharging:");
            sb.append(KeyguardIndicationController.this.mPdCharging);
            String str = "KeyguardIndication";
            Log.d(str, sb.toString());
            if (KeyguardIndicationController.this.mInfoView != null) {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("onRefreshBatteryInfo, mInfoView.getVisibility():");
                    sb2.append(KeyguardIndicationController.this.mInfoView.getVisibility());
                    Log.d(str, sb2.toString());
                }
            } else if (Build.DEBUG_ONEPLUS) {
                Log.d(str, "onRefreshBatteryInfo, mInfoView: null");
            }
            boolean z2 = KeyguardIndicationController.this.isFastCharge() && !KeyguardIndicationController.this.mPdCharging;
            String access$1300 = KeyguardIndicationController.this.computePowerIndication();
            if (KeyguardIndicationController.this.mPowerPluggedIn && (!z2 || KeyguardIndicationController.this.mLastChargingSpeed == KeyguardIndicationController.this.mChargingSpeed)) {
                KeyguardIndicationController.this.mChargingInfo.setText(access$1300);
            }
            if (!access$1400 && KeyguardIndicationController.this.mPowerPluggedIn) {
                if (!z2) {
                    KeyguardIndicationController.this.mChargingInfofadeOutAnimation.cancel();
                    if (KeyguardIndicationController.this.mInfoView != null) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("!playFastChargeAnimation, mInfoView.getVisibility():");
                        sb3.append(KeyguardIndicationController.this.mInfoView.getVisibility());
                        sb3.append(", mInfoViewID:");
                        sb3.append(KeyguardIndicationController.this.mInfoView.getId());
                        Log.d(str, sb3.toString());
                    } else {
                        Log.d(str, "!playFastChargeAnimation, mInfoView: null");
                    }
                    if (KeyguardIndicationController.this.mChargingInfo.getVisibility() == 0 || KeyguardIndicationController.this.mInfoView.getVisibility() == 0) {
                        KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
                    } else {
                        Log.i(str, "before mChargingInfofadeInAnimation.start");
                        KeyguardIndicationController.this.mChargingInfofadeInAnimation.start();
                    }
                }
                KeyguardIndicationController.this.mKeyguardStatusView.setCharging(true);
            } else if (access$1400 && !KeyguardIndicationController.this.mPowerPluggedIn) {
                if (KeyguardIndicationController.this.mKeyguardStatusView.hasOwnerInfo()) {
                    KeyguardIndicationController.this.mKeyguardStatusView.setCharging(false);
                    KeyguardIndicationController.this.mChargingInfo.setVisibility(8);
                } else {
                    KeyguardIndicationController.this.mChargingInfofadeInAnimation.cancel();
                    KeyguardIndicationController.this.mChargingInfofadeOutAnimation.start();
                }
                KeyguardIndicationController.this.mPlaySoundDone = false;
            }
            if (KeyguardIndicationController.this.mDozing) {
                if (!access$1400 && KeyguardIndicationController.this.mPowerPluggedIn) {
                    KeyguardIndicationController keyguardIndicationController3 = KeyguardIndicationController.this;
                    keyguardIndicationController3.showTransientIndication((CharSequence) keyguardIndicationController3.computePowerIndication());
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                } else if (access$1400 && !KeyguardIndicationController.this.mPowerPluggedIn) {
                    KeyguardIndicationController.this.hideTransientIndication();
                }
            }
            if (KeyguardIndicationController.this.mDashView == null || KeyguardIndicationController.this.mDashAnimation == null) {
                Log.w(str, "no dash view");
                return;
            }
            if (!z2) {
                KeyguardIndicationController.this.mHandler.removeMessages(3);
                KeyguardIndicationController.this.mHandler.removeMessages(4);
                if (KeyguardIndicationController.this.mChargingInfo.getVisibility() != 0 && !access$1400 && KeyguardIndicationController.this.mPowerPluggedIn) {
                    KeyguardIndicationController.this.mChargingInfo.setText(access$1300);
                    KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
                }
                if (KeyguardIndicationController.this.mDashAnimation != null) {
                    KeyguardIndicationController.this.mDashAnimation.stop();
                }
                KeyguardIndicationController.this.mFadeOutAnimatorSet.cancel();
                KeyguardIndicationController.this.mDashView.setVisibility(4);
                KeyguardIndicationController.this.mDashContainer.setVisibility(8);
            } else if (KeyguardIndicationController.this.mLastChargingSpeed != KeyguardIndicationController.this.mChargingSpeed) {
                if (OpUtils.SUPPORT_WARP_CHARGING) {
                    KeyguardIndicationController.this.mChargingInfo.setText(access$1300);
                    KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
                } else {
                    KeyguardIndicationController.this.mDashView.setImageResource(0);
                    KeyguardIndicationController.this.mDashView.setVisibility(4);
                    KeyguardIndicationController.this.mDashContainer.setVisibility(0);
                    Message message = new Message();
                    message.what = 3;
                    KeyguardIndicationController.this.mHandler.sendEmptyMessage(message.what);
                }
                if (!KeyguardIndicationController.this.mPlaySoundDone) {
                    KeyguardIndicationController keyguardIndicationController4 = KeyguardIndicationController.this;
                    keyguardIndicationController4.mPlaySoundDone = keyguardIndicationController4.playFastWarpChargingSound();
                }
            } else if (!access$1400 && KeyguardIndicationController.this.mPowerPluggedIn) {
                KeyguardIndicationController.this.mChargingInfo.setText(access$1300);
                KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
            }
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            if (z) {
                KeyguardIndicationController.this.updateDisclosure();
            }
            if (OpUtils.isCustomFingerprint()) {
                boolean isUnlockWithFingerprintPossible = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext).isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
                KeyguardIndicationController.this.updateBottomMargins(z, isUnlockWithFingerprintPossible);
                KeyguardIndicationController.this.updatePadding(z, isUnlockWithFingerprintPossible);
            }
        }

        public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (instance.isUnlockingWithBiometricAllowed()) {
                animatePadlockError();
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, KeyguardIndicationController.this.mInitialTextColorState, 1);
                } else if (instance.isScreenOn() && instance.isDeviceInteractive()) {
                    if (KeyguardIndicationController.this.mLockIcon != null) {
                        KeyguardIndicationController.this.mLockIcon.setTransientBiometricsError(true);
                    }
                    KeyguardIndicationController.this.mShowingError = true;
                    KeyguardIndicationController.this.showTransientIndication((CharSequence) str);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(1300);
                }
            }
        }

        public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (!shouldSuppressBiometricError(i, biometricSourceType, instance)) {
                animatePadlockError();
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, KeyguardIndicationController.this.mInitialTextColorState, 1);
                } else if (instance.isScreenOn()) {
                    KeyguardIndicationController.this.mShowingError = true;
                    KeyguardIndicationController.this.showTransientIndication((CharSequence) str);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                } else {
                    KeyguardIndicationController.this.mMessageToShowOnScreenOn = str;
                }
            }
        }

        private void animatePadlockError() {
            if (KeyguardIndicationController.this.mLockIcon != null) {
                KeyguardIndicationController.this.mLockIcon.setTransientBiometricsError(true);
            }
            KeyguardIndicationController.this.mHandler.removeMessages(2);
            KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300);
        }

        private boolean shouldSuppressBiometricError(int i, BiometricSourceType biometricSourceType, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
                return shouldSuppressFingerprintError(i, keyguardUpdateMonitor);
            }
            if (biometricSourceType == BiometricSourceType.FACE) {
                return shouldSuppressFaceError(i, keyguardUpdateMonitor);
            }
            return false;
        }

        private boolean shouldSuppressFingerprintError(int i, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            return (!keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() && i != 9) || (!keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() && KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) || i == 5;
        }

        private boolean shouldSuppressFaceError(int i, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            return (!keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() && i != 9) || i == 5;
        }

        public void onTrustAgentErrorMessage(CharSequence charSequence) {
            KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
            keyguardIndicationController.showTransientIndication(charSequence, Utils.getColorError(keyguardIndicationController.mContext));
        }

        public void onScreenTurnedOn() {
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn == null && instance.isFacelockDisabled()) {
                KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
                keyguardIndicationController.mMessageToShowOnScreenOn = keyguardIndicationController.mContext.getString(instance.getFacelockNotifyMsgId(instance.getFacelockRunningType()));
            }
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn != null) {
                KeyguardIndicationController keyguardIndicationController2 = KeyguardIndicationController.this;
                keyguardIndicationController2.showTransientIndication(keyguardIndicationController2.mMessageToShowOnScreenOn, Utils.getColorError(KeyguardIndicationController.this.mContext));
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
            if (z) {
                KeyguardIndicationController.this.hideTransientIndication();
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
            super.onBiometricAuthenticated(i, biometricSourceType);
            KeyguardIndicationController.this.mHandler.sendEmptyMessage(1);
        }

        public void onUserUnlocked() {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication(false);
            }
        }

        public void onKeyguardBouncerChanged(boolean z) {
            if (KeyguardIndicationController.this.mLockIcon != null) {
                KeyguardIndicationController.this.mLockIcon.setBouncerVisible(z);
            }
        }
    }

    private class KeyguardStateCallback extends Stub {
        public void onDisabledStateChanged(boolean z) {
        }

        public void onFingerprintStateChange(boolean z, int i, int i2, int i3) {
        }

        public void onHasLockscreenWallpaperChanged(boolean z) {
        }

        public void onInputRestrictedStateChanged(boolean z) {
        }

        public void onPocketModeActiveChanged(boolean z) {
        }

        public void onSimSecureStateChanged(boolean z) {
        }

        public void onTrustedChanged(boolean z) {
        }

        private KeyguardStateCallback() {
        }

        public void onShowingStateChanged(boolean z) {
            StringBuilder sb = new StringBuilder();
            sb.append("onShowingStateChanged ");
            sb.append(z);
            Log.d("KeyguardIndication", sb.toString());
            if (z) {
                KeyguardIndicationController.this.updateDashViews();
            } else {
                KeyguardIndicationController.this.releaseDashViews();
            }
        }
    }

    private String getTrustManagedIndication() {
        return null;
    }

    public void onStateChanged(int i) {
    }

    private ValueAnimator chargingInfoFadeInAnimation() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final LayoutParams layoutParams = this.mChargingInfo.getLayoutParams();
        final int chargingInfoHeight = getChargingInfoHeight();
        ofFloat.setDuration((long) CHARGING_INFO_ANITMAION_DURATION);
        ofFloat.setInterpolator(Interpolators.LINEAR);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                layoutParams.height = 0;
                KeyguardIndicationController.this.mChargingInfo.setLayoutParams(layoutParams);
                KeyguardIndicationController.this.mChargingInfo.requestLayout();
                KeyguardIndicationController.this.mChargingInfo.setVisibility(4);
                String str = "KeyguardIndication";
                Log.d(str, "chargingInfoFadeInAnimation onAnimationStart");
                if (KeyguardIndicationController.this.mInfoView != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onAnimationStart, mInfoView.getVisibility():");
                    sb.append(KeyguardIndicationController.this.mInfoView.getVisibility());
                    sb.append(", mInfoViewID:");
                    sb.append(KeyguardIndicationController.this.mInfoView.getId());
                    Log.d(str, sb.toString());
                    return;
                }
                Log.d(str, "onAnimationStart, mInfoView: null");
            }

            public void onAnimationEnd(Animator animator) {
                layoutParams.height = chargingInfoHeight;
                KeyguardIndicationController.this.mChargingInfo.setLayoutParams(layoutParams);
                KeyguardIndicationController.this.mChargingInfo.requestLayout();
                StringBuilder sb = new StringBuilder();
                sb.append("chargingInfoFadeInAnimation onAnimationEnd / height:");
                sb.append(chargingInfoHeight);
                String str = "KeyguardIndication";
                Log.d(str, sb.toString());
                if (KeyguardIndicationController.this.mInfoView != null) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("onAnimationEnd, mInfoView.getVisibility():");
                    sb2.append(KeyguardIndicationController.this.mInfoView.getVisibility());
                    sb2.append(", mInfoViewID:");
                    sb2.append(KeyguardIndicationController.this.mInfoView.getId());
                    Log.d(str, sb2.toString());
                    return;
                }
                Log.d(str, "onAnimationEnd, mInfoView: null");
            }
        });
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
                layoutParams.height = (int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * ((float) chargingInfoHeight));
                KeyguardIndicationController.this.mChargingInfo.setLayoutParams(layoutParams);
                KeyguardIndicationController.this.mChargingInfo.requestLayout();
            }
        });
        return ofFloat;
    }

    private ValueAnimator chargingInfoFadeOutAnimation() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final LayoutParams layoutParams = this.mChargingInfo.getLayoutParams();
        final int chargingInfoHeight = getChargingInfoHeight();
        ofFloat.setDuration((long) CHARGING_INFO_ANITMAION_DURATION);
        ofFloat.setInterpolator(Interpolators.LINEAR);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                Log.d("KeyguardIndication", "chargingInfoFadeOutAnimation onAnimationStart");
            }

            public void onAnimationEnd(Animator animator) {
                KeyguardIndicationController.this.mKeyguardStatusView.setCharging(false);
                KeyguardIndicationController.this.mChargingInfo.setVisibility(8);
                layoutParams.height = chargingInfoHeight;
                KeyguardIndicationController.this.mChargingInfo.setLayoutParams(layoutParams);
                KeyguardIndicationController.this.mChargingInfo.requestLayout();
                Log.d("KeyguardIndication", "chargingInfoFadeOutAnimation onAnimationEnd");
            }
        });
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layoutParams.height = (int) ((1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue()) * ((float) chargingInfoHeight));
                KeyguardIndicationController.this.mChargingInfo.setLayoutParams(layoutParams);
                KeyguardIndicationController.this.mChargingInfo.requestLayout();
            }
        });
        return ofFloat;
    }

    public KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon, KeyguardStatusView keyguardStatusView, KeyguardBottomAreaView keyguardBottomAreaView) {
        this(context, viewGroup, lockIcon, new LockPatternUtils(context), WakeLock.createPartial(context, "Doze:KeyguardIndication"), (ShadeController) Dependency.get(ShadeController.class), (AccessibilityController) Dependency.get(AccessibilityController.class), UnlockMethodCache.getInstance(context), (StatusBarStateController) Dependency.get(StatusBarStateController.class), KeyguardUpdateMonitor.getInstance(context), keyguardStatusView, keyguardBottomAreaView);
        registerKeyguardStateCallbackCallbacks();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    @VisibleForTesting
    KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon, LockPatternUtils lockPatternUtils, WakeLock wakeLock, ShadeController shadeController, AccessibilityController accessibilityController, UnlockMethodCache unlockMethodCache, StatusBarStateController statusBarStateController, KeyguardUpdateMonitor keyguardUpdateMonitor, KeyguardStatusView keyguardStatusView, KeyguardBottomAreaView keyguardBottomAreaView) {
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mClippingParams = new ClippingParameters() {
            public boolean shouldFinish(View view) {
                return view == KeyguardIndicationController.this.mIndicationArea;
            }
        };
        this.mPlaySoundDone = false;
        this.mShowingError = false;
        this.mShowMsgWhenExiting = false;
        this.mTickReceiver = new KeyguardUpdateMonitorCallback() {
            public void onTimeChanged() {
                if (KeyguardIndicationController.this.mVisible) {
                    KeyguardIndicationController.this.updateIndication(false);
                }
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 1) {
                    KeyguardIndicationController.this.hideTransientIndication();
                } else if (i != 2) {
                    String str = "KeyguardIndication";
                    if (i == 3) {
                        Log.d(str, "MSG_PLAY_FAST_CHARGE_ANIMATION");
                        KeyguardIndicationController.this.mChargingInfofadeInAnimation.cancel();
                        KeyguardIndicationController.this.mChargingInfo.setVisibility(8);
                        KeyguardIndicationController.this.mDashView.setVisibility(0);
                        if (KeyguardIndicationController.this.mDashAnimation != null) {
                            KeyguardIndicationController.this.mDashAnimation.start();
                        }
                        KeyguardIndicationController.this.mHandler.sendEmptyMessageDelayed(4, 900);
                    } else if (i == 4) {
                        Log.d(str, "MSG_PLAY_FADE_OUT_ANIMATION");
                        KeyguardIndicationController.this.mFadeOutAnimatorSet.start();
                    }
                } else if (KeyguardIndicationController.this.mLockIcon != null) {
                    KeyguardIndicationController.this.mLockIcon.setTransientBiometricsError(false);
                }
            }
        };
        this.mContext = context;
        if (OpUtils.isCustomFingerprint()) {
            this.mInitialTextColorState = Utils.getColorAttr(this.mContext, R$attr.wallpaperTextColor);
            KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
            if (keyguardIndicationTextView != null) {
                keyguardIndicationTextView.setTypeface(Typeface.DEFAULT);
            }
        }
        this.mLockIcon = lockIcon;
        this.mShadeController = shadeController;
        this.mAccessibilityController = accessibilityController;
        this.mUnlockMethodCache = unlockMethodCache;
        this.mStatusBarStateController = statusBarStateController;
        this.mKeyguardUpdateMonitor = keyguardUpdateMonitor;
        LockIcon lockIcon2 = this.mLockIcon;
        if (lockIcon2 != null) {
            lockIcon2.setOnLongClickListener(new OnLongClickListener() {
                public final boolean onLongClick(View view) {
                    return KeyguardIndicationController.this.handleLockLongClick(view);
                }
            });
            this.mLockIcon.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    KeyguardIndicationController.this.handleLockClick(view);
                }
            });
        }
        String str = "KeyguardIndication";
        this.mWakeLock = new SettableWakeLock(wakeLock, str);
        this.mLockPatternUtils = lockPatternUtils;
        Resources resources = context.getResources();
        this.mSlowThreshold = resources.getInteger(R$integer.config_chargingSlowlyThreshold);
        this.mFastThreshold = resources.getInteger(R$integer.config_chargingFastThreshold);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mKeyguardStatusView = keyguardStatusView;
        this.mDashContainer = (FrameLayout) keyguardStatusView.findViewById(R$id.charging_dash_container);
        this.mDashView = (ImageView) keyguardStatusView.findViewById(R$id.charging_dash);
        this.mChargingInfo = (TextView) keyguardStatusView.findViewById(R$id.charging_info);
        this.mInfoView = (ViewGroup) keyguardStatusView.findViewById(com.android.keyguard.R$id.charging_and_owner_info_view);
        if (this.mInfoView != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("new, mInfoView.getVisibility():");
            sb.append(this.mInfoView.getVisibility());
            sb.append(", mInfoViewID:");
            sb.append(this.mInfoView.getId());
            sb.append(", resId:");
            sb.append(com.android.keyguard.R$id.charging_and_owner_info_view);
            sb.append(", mInfoView:");
            sb.append(this.mInfoView);
            Log.d(str, sb.toString());
        }
        this.mKeyguardBottomArea = keyguardBottomAreaView;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mChargingSound = new SoundPool(1, 1, 0);
        this.mChargingSoundId = this.mChargingSound.load(context, R$raw.charging, 1);
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        setIndicationArea(viewGroup);
        this.mChargingInfofadeInAnimation = chargingInfoFadeInAnimation();
        this.mChargingInfofadeOutAnimation = chargingInfoFadeOutAnimation();
        this.mFadeOutAnimatorSet = getFadeOutAnimation();
        this.mErrorAnimator = AnimatorInflater.loadAnimator(this.mContext, R$anim.ic_keyguard_Indication_error_animation);
        updateDisclosure();
        this.mOwnerInfo = keyguardStatusView.findViewById(R$id.owner_info);
        updateChargingInfoAndOwnerInfo();
        this.mKeyguardUpdateMonitor.registerCallback(getKeyguardCallback());
        this.mKeyguardUpdateMonitor.registerCallback(this.mTickReceiver);
        this.mStatusBarStateController.addCallback(this);
        this.mUnlockMethodCache.addListener(this);
    }

    public void setIndicationArea(ViewGroup viewGroup) {
        if (viewGroup instanceof KeyguardBottomAreaView) {
            this.mKeyguardBottomArea = (KeyguardBottomAreaView) viewGroup;
            viewGroup = (ViewGroup) viewGroup.findViewById(R$id.keyguard_indication_area);
        }
        this.mIndicationArea = viewGroup;
        this.mTextView = (KeyguardIndicationTextView) viewGroup.findViewById(R$id.keyguard_indication_text);
        KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
        this.mInitialTextColorState = keyguardIndicationTextView != null ? keyguardIndicationTextView.getTextColors() : ColorStateList.valueOf(-1);
        this.mDisclosure = (KeyguardIndicationTextView) viewGroup.findViewById(R$id.keyguard_indication_enterprise_disclosure);
        updateIndication(false);
    }

    private int getChargingInfoHeight() {
        return OpUtils.SUPPORT_WARP_CHARGING ? this.mContext.getResources().getDimensionPixelSize(R$dimen.charging_dash_margin_top) + this.mContext.getResources().getDimensionPixelSize(R$dimen.op_owner_info_font_size) : this.mContext.getResources().getDimensionPixelSize(R$dimen.op_keyguard_indication_height);
    }

    private void updateChargingInfoAndOwnerInfo() {
        if (this.mChargingInfo != null && this.mOwnerInfo != null) {
            Log.d("KeyguardIndication", "updateChargingInfoAndOwnerInfo");
            int chargingInfoHeight = getChargingInfoHeight();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mChargingInfo.getLayoutParams();
            layoutParams.height = chargingInfoHeight;
            this.mChargingInfo.setLayoutParams(layoutParams);
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mOwnerInfo.getLayoutParams();
            layoutParams2.height = chargingInfoHeight;
            this.mOwnerInfo.setLayoutParams(layoutParams2);
        }
    }

    public void onDensityOrFontScaleChanged() {
        ValueAnimator valueAnimator = this.mChargingInfofadeInAnimation;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mChargingInfofadeInAnimation.cancel();
        }
        this.mChargingInfofadeInAnimation = chargingInfoFadeInAnimation();
        ValueAnimator valueAnimator2 = this.mChargingInfofadeOutAnimation;
        if (valueAnimator2 != null && valueAnimator2.isStarted()) {
            this.mChargingInfofadeOutAnimation.cancel();
        }
        this.mChargingInfofadeOutAnimation = chargingInfoFadeOutAnimation();
        updateChargingInfoAndOwnerInfo();
    }

    private void registerKeyguardStateCallbackCallbacks() {
        getKeyguardStateCallback();
        ((KeyguardViewMediator) ((SystemUIApplication) this.mContext).getComponent(KeyguardViewMediator.class)).addStateMonitorCallback(this.mKeyguardStateCallback);
    }

    /* access modifiers changed from: private */
    public boolean handleLockLongClick(View view) {
        this.mLockscreenGestureLogger.write(191, 0, 0);
        showTransientIndication(R$string.keyguard_indication_trust_disabled);
        this.mKeyguardUpdateMonitor.onLockIconPressed();
        this.mLockPatternUtils.requireCredentialEntry(KeyguardUpdateMonitor.getCurrentUser());
        return true;
    }

    /* access modifiers changed from: private */
    public void handleLockClick(View view) {
        if (view == this.mLockIcon) {
            StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
            if (OpUtils.isCustomFingerprint() && KeyguardUpdateMonitor.getInstance(this.mContext).isCameraErrorState()) {
                Log.d("KeyguardIndication", "enter bouncer when camera error");
                phoneStatusBar.showBouncer(false);
                phoneStatusBar.animateCollapsePanels(0, true);
                return;
            } else if (KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockAvailable() && phoneStatusBar.getFacelockController() != null && phoneStatusBar.getFacelockController().tryToStartFaceLock(true)) {
                return;
            }
        }
        if (this.mAccessibilityController.isAccessibilityEnabled()) {
            this.mShadeController.animateCollapsePanels(0, true);
        }
    }

    /* access modifiers changed from: protected */
    public KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (this.mUpdateMonitorCallback == null) {
            this.mUpdateMonitorCallback = new BaseKeyguardCallback();
        }
        return this.mUpdateMonitorCallback;
    }

    private void getKeyguardStateCallback() {
        if (this.mKeyguardStateCallback == null) {
            this.mKeyguardStateCallback = new KeyguardStateCallback();
        }
    }

    /* access modifiers changed from: private */
    public void updateDisclosure() {
        DevicePolicyManager devicePolicyManager = this.mDevicePolicyManager;
        if (devicePolicyManager != null) {
            if (this.mDozing || !devicePolicyManager.isDeviceManaged()) {
                this.mDisclosure.setVisibility(8);
            } else {
                CharSequence deviceOwnerOrganizationName = this.mDevicePolicyManager.getDeviceOwnerOrganizationName();
                if (deviceOwnerOrganizationName != null) {
                    this.mDisclosure.switchIndication((CharSequence) this.mContext.getResources().getString(R$string.do_disclosure_with_name, new Object[]{deviceOwnerOrganizationName}));
                } else {
                    this.mDisclosure.switchIndication(R$string.do_disclosure_generic);
                }
                this.mDisclosure.setVisibility(0);
            }
        }
    }

    public void setVisible(boolean z) {
        this.mVisible = z;
        this.mIndicationArea.setVisibility(z ? 0 : 8);
        if (z) {
            if (!this.mHandler.hasMessages(1)) {
                hideTransientIndication();
            }
            updateIndication(false);
        } else if (!z) {
            hideTransientIndication();
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public String getTrustGrantedIndication() {
        if (this.mKeyguardUpdateMonitor.canSkipBouncerByFacelock()) {
            return getLocaleString(R$string.op_keyguard_indication_face_unlocked);
        }
        return getLocaleString(R$string.op_keyguard_indication_trust_unlocked);
    }

    public void hideTransientIndicationDelayed(long j) {
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), j);
    }

    public void showTransientIndication(int i) {
        showTransientIndication((CharSequence) this.mContext.getResources().getString(i));
    }

    public void showTransientIndication(CharSequence charSequence) {
        showTransientIndication(charSequence, this.mInitialTextColorState);
    }

    public void showTransientIndication(CharSequence charSequence, ColorStateList colorStateList) {
        if (OpUtils.isCustomFingerprint()) {
            ColorStateList colorError = Utils.getColorError(this.mContext);
            colorStateList = this.mInitialTextColorState;
        }
        this.mTransientIndication = charSequence;
        this.mTransientTextColorState = colorStateList;
        this.mHandler.removeMessages(1);
        if (this.mDozing && !TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(true);
            hideTransientIndicationDelayed(5000);
        }
        updateIndication(false);
        if (this.mShowingError) {
            this.mShowingError = false;
            animateErrorText(this.mTextView);
        }
    }

    private void animateErrorText(KeyguardIndicationTextView keyguardIndicationTextView) {
        Animator loadAnimator = AnimatorInflater.loadAnimator(this.mContext, R$anim.oneplus_control_text_error_message_anim);
        if (loadAnimator != null) {
            loadAnimator.cancel();
            loadAnimator.setTarget(keyguardIndicationTextView);
            loadAnimator.start();
        }
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication(false);
        }
    }

    /* access modifiers changed from: protected */
    public final void updateIndication(boolean z) {
        if (TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(false);
        }
        if (this.mVisible) {
            if (this.mDozing) {
                this.mTextView.setTextColor(-1);
                if (!TextUtils.isEmpty(this.mTransientIndication)) {
                    this.mTextView.switchIndication(this.mTransientIndication);
                } else {
                    this.mTextView.switchIndication((CharSequence) NumberFormat.getPercentInstance().format((double) (((float) this.mBatteryLevel) / 100.0f)));
                }
                return;
            }
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            String trustGrantedIndication = getTrustGrantedIndication();
            String trustManagedIndication = getTrustManagedIndication();
            if (!this.mUserManager.isUserUnlocked(currentUser)) {
                this.mTextView.switchIndication(17040297);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else if (!TextUtils.isEmpty(this.mTransientIndication)) {
                this.mTextView.switchIndication(this.mTransientIndication);
                this.mTextView.setTextColor(this.mTransientTextColorState);
            } else if (!TextUtils.isEmpty(trustGrantedIndication) && this.mKeyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication((CharSequence) trustGrantedIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else if (TextUtils.isEmpty(trustManagedIndication) || !this.mKeyguardUpdateMonitor.getUserTrustIsManaged(currentUser) || this.mKeyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication((CharSequence) this.mRestingIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else {
                this.mTextView.switchIndication((CharSequence) trustManagedIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            }
            if (this.mPowerPluggedIn) {
                this.mChargingInfo.setText(computePowerIndication());
            }
        }
    }

    /* access modifiers changed from: private */
    public String computePowerIndication() {
        int i;
        if (this.mBatteryLevel >= 100) {
            return this.mContext.getResources().getString(R$string.keyguard_charged);
        }
        try {
            this.mBatteryInfo.computeChargeTimeRemaining();
        } catch (RemoteException e) {
            Log.e("KeyguardIndication", "Error calling IBatteryStats: ", e);
        }
        if (this.mPowerPluggedInWired) {
            int i2 = this.mChargingSpeed;
            if (i2 == 0) {
                i = R$string.keyguard_plugged_in_charging_slowly;
            } else if (i2 == 2) {
                i = R$string.keyguard_plugged_in_charging_fast;
            } else if (i2 != 3) {
                i = R$string.keyguard_plugged_in;
            } else {
                i = R$string.keyguard_plugged_in_charging_warp;
            }
            if (this.mPdCharging) {
                i = R$string.keyguard_plugged_in_charging_fast;
            }
        } else {
            i = R$string.keyguard_plugged_in_wireless;
        }
        String format = NumberFormat.getPercentInstance().format((double) (((float) this.mBatteryLevel) / 100.0f));
        try {
            return this.mContext.getResources().getString(i, new Object[]{format});
        } catch (IllegalFormatConversionException unused) {
            return this.mContext.getResources().getString(i);
        }
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    private AnimatorSet getFadeOutAnimation() {
        new ValueAnimator();
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ofFloat.setDuration(800);
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                KeyguardIndicationController.this.mChargingInfo.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        new ValueAnimator();
        ValueAnimator ofFloat2 = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        ofFloat2.setDuration(800);
        ofFloat2.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                KeyguardIndicationController.this.mDashContainer.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(800);
        animatorSet.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                KeyguardIndicationController.this.mChargingInfo.setText(KeyguardIndicationController.this.computePowerIndication());
                KeyguardIndicationController.this.mChargingInfo.setVisibility(0);
                KeyguardIndicationController.this.mChargingInfo.setAlpha(0.0f);
            }

            public void onAnimationEnd(Animator animator) {
                KeyguardIndicationController.this.mDashContainer.setVisibility(8);
                KeyguardIndicationController.this.mDashContainer.setAlpha(1.0f);
                KeyguardIndicationController.this.mChargingInfo.setAlpha(1.0f);
            }
        });
        animatorSet.playTogether(new Animator[]{ofFloat2, ofFloat});
        return animatorSet;
    }

    public void setDozing(boolean z) {
        if (this.mDozing != z) {
            this.mDozing = z;
            updateIndication(false);
            updateDisclosure();
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("KeyguardIndicationController:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mTransientTextColorState: ");
        sb.append(this.mTransientTextColorState);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mInitialTextColorState: ");
        sb2.append(this.mInitialTextColorState);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mPowerPluggedInWired: ");
        sb3.append(this.mPowerPluggedInWired);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mPowerPluggedIn: ");
        sb4.append(this.mPowerPluggedIn);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mPowerCharged: ");
        sb5.append(this.mPowerCharged);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mChargingSpeed: ");
        sb6.append(this.mChargingSpeed);
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("  mChargingWattage: ");
        sb7.append(this.mChargingWattage);
        printWriter.println(sb7.toString());
        StringBuilder sb8 = new StringBuilder();
        sb8.append("  mMessageToShowOnScreenOn: ");
        sb8.append(this.mMessageToShowOnScreenOn);
        printWriter.println(sb8.toString());
        StringBuilder sb9 = new StringBuilder();
        sb9.append("  mDozing: ");
        sb9.append(this.mDozing);
        printWriter.println(sb9.toString());
        StringBuilder sb10 = new StringBuilder();
        sb10.append("  mBatteryLevel: ");
        sb10.append(this.mBatteryLevel);
        printWriter.println(sb10.toString());
        StringBuilder sb11 = new StringBuilder();
        sb11.append("  mTextView.getText(): ");
        KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
        Integer num = null;
        sb11.append(keyguardIndicationTextView == null ? null : keyguardIndicationTextView.getText());
        printWriter.println(sb11.toString());
        StringBuilder sb12 = new StringBuilder();
        sb12.append("  computePowerIndication(): ");
        sb12.append(computePowerIndication());
        printWriter.println(sb12.toString());
        StringBuilder sb13 = new StringBuilder();
        sb13.append("  mInfoView.getVisibility: ");
        ViewGroup viewGroup = this.mInfoView;
        sb13.append(viewGroup == null ? null : Integer.valueOf(viewGroup.getVisibility()));
        printWriter.println(sb13.toString());
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb14 = new StringBuilder();
            sb14.append("  mTransientIndication: ");
            sb14.append(this.mTransientIndication);
            printWriter.println(sb14.toString());
            StringBuilder sb15 = new StringBuilder();
            sb15.append("  mTextView.getVisibility(): ");
            KeyguardIndicationTextView keyguardIndicationTextView2 = this.mTextView;
            if (keyguardIndicationTextView2 != null) {
                num = Integer.valueOf(keyguardIndicationTextView2.getVisibility());
            }
            sb15.append(num);
            printWriter.println(sb15.toString());
        }
    }

    public void onDozingChanged(boolean z) {
        setDozing(z);
    }

    public void onUnlockMethodStateChanged() {
        updateIndication(!this.mDozing);
    }

    /* access modifiers changed from: private */
    public boolean playFastWarpChargingSound() {
        boolean isStreamMute = this.mAudioManager.isStreamMute(2);
        StringBuilder sb = new StringBuilder();
        sb.append("play dash anim, ");
        sb.append(isStreamMute);
        sb.append(", ");
        sb.append(this.mVisible);
        Log.d("KeyguardIndication", sb.toString());
        if (isStreamMute || !this.mVisible) {
            return false;
        }
        this.mChargingSound.play(this.mChargingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        return true;
    }

    public void updateDashViews() {
        this.mDashView.setBackground(this.mContext.getDrawable(R$drawable.charging_dash_animation));
        this.mDashAnimation = (AnimationDrawable) this.mDashView.getBackground();
    }

    public void releaseDashViews() {
        this.mDashView.setBackground(null);
        this.mDashAnimation = null;
    }

    public void notifyPreventModeChange(boolean z) {
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean isFingerprintLockout = instance.isFingerprintLockout();
        boolean isUnlockingWithBiometricAllowed = instance.isUnlockingWithBiometricAllowed();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isDeviceInteractive = instance.isDeviceInteractive();
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyPreventModeChange, ");
            sb.append(z);
            String str = ", ";
            sb.append(str);
            sb.append(this.mShowMsgWhenExiting);
            sb.append(str);
            sb.append(isFingerprintLockout);
            sb.append(str);
            sb.append(isUnlockingWithBiometricAllowed);
            sb.append(str);
            sb.append(isBouncerShowing);
            sb.append(str);
            sb.append(isDeviceInteractive);
            Log.d("KeyguardIndication", sb.toString());
        }
        if (z) {
            this.mShowMsgWhenExiting = true;
        } else {
            if (this.mShowMsgWhenExiting && isFingerprintLockout) {
                String string = this.mContext.getString(17040032);
                if (isUnlockingWithBiometricAllowed) {
                    ColorStateList colorError = Utils.getColorError(this.mContext);
                    if (!isBouncerShowing && isDeviceInteractive) {
                        LockIcon lockIcon = this.mLockIcon;
                        if (lockIcon != null) {
                            lockIcon.setTransientBiometricsError(false);
                        }
                        showTransientIndication(string, colorError);
                        this.mHandler.removeMessages(1);
                        this.mHandler.removeMessages(2);
                        hideTransientIndicationDelayed(5000);
                    }
                } else {
                    return;
                }
            }
            this.mShowMsgWhenExiting = false;
        }
    }

    /* access modifiers changed from: private */
    public boolean isFastCharge() {
        if (this.mVisible && this.mPowerPluggedIn) {
            int i = this.mChargingSpeed;
            if (i == 2 || i == 3) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void updateBottomMargins(boolean z, boolean z2) {
        if (z && this.mIndicationArea != null) {
            this.mKeyguardBottomArea.updateIndicationArea();
        }
    }

    /* access modifiers changed from: private */
    public void updatePadding(boolean z, boolean z2) {
        int i;
        if (z) {
            if (z2) {
                i = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_left5);
            } else {
                i = this.mContext.getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_height);
            }
            this.mTextView.setPadding(i, 0, i, 0);
        }
    }

    public boolean isShowingText() {
        KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
        if (keyguardIndicationTextView != null && !TextUtils.isEmpty(keyguardIndicationTextView.getText()) && this.mTextView.getVisibility() == 0) {
            return true;
        }
        return false;
    }

    public LockIcon getLockIcon() {
        return this.mLockIcon;
    }

    public void unregisterCallback() {
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null && this.mStatusBarStateController != null && this.mUnlockMethodCache != null) {
            keyguardUpdateMonitor.removeCallback(getKeyguardCallback());
            this.mKeyguardUpdateMonitor.removeCallback(this.mTickReceiver);
            this.mStatusBarStateController.removeCallback(this);
            this.mUnlockMethodCache.removeListener(this);
        }
    }

    private String getLocaleString(int i) {
        Resources resources = this.mContext.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = configuration.locale;
        String language = locale.getLanguage();
        if (resources == null || configuration == null || locale == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("can't fetch locale sring: resources = ");
            sb.append(resources);
            sb.append(", curConfig = ");
            sb.append(configuration);
            sb.append(", local = ");
            sb.append(locale);
            Log.d("KeyguardIndication", sb.toString());
            return this.mContext.getResources().getString(i);
        } else if ("zh".equals(language) || "en".equals(language)) {
            return resources.getString(i);
        } else {
            Configuration configuration2 = new Configuration(configuration);
            configuration2.setLocale(Locale.US);
            return this.mContext.createConfigurationContext(configuration2).getResources().getString(i);
        }
    }

    public void reInflateAdjust() {
        if (OpUtils.isCustomFingerprint()) {
            updateBottomMargins(this.mKeyguardUpdateMonitor.isKeyguardVisible(), this.mKeyguardUpdateMonitor.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser()));
        }
        boolean z = true;
        if (this.mStatusBarStateController.getState() != 1) {
            z = false;
        }
        setVisible(z);
    }
}
