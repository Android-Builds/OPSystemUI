package com.oneplus.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$anim;
import com.android.systemui.R$drawable;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.oneplus.phone.OpTrustDrawable;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpLockIcon extends KeyguardAffordanceView {
    protected boolean mDeviceInteractive;
    /* access modifiers changed from: private */
    public Animation mFacelockAnimationSet;
    private Animation mFacelockFailAnimationSet;
    private AnimatorSet mFacelockRetryAnimationSet;
    private int mFacelockRunningType = 0;
    protected boolean mLastDeviceInteractive;
    private LockPatternUtils mLockPatternUtils;
    protected final Runnable mPaddingRetryRunnable = new Runnable() {
        public final void run() {
            OpLockIcon.this.lambda$new$0$OpLockIcon();
        }
    };
    protected OpTrustDrawable mTrustDrawable;

    protected static class IntrinsicSizeDrawable extends InsetDrawable {
        private final int mIntrinsicHeight;
        private final int mIntrinsicWidth;

        public IntrinsicSizeDrawable(Drawable drawable, int i, int i2) {
            super(drawable, 0);
            this.mIntrinsicWidth = i;
            this.mIntrinsicHeight = i2;
        }

        public int getIntrinsicWidth() {
            return this.mIntrinsicWidth;
        }

        public int getIntrinsicHeight() {
            return this.mIntrinsicHeight;
        }
    }

    public OpLockIcon(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTrustDrawable = new OpTrustDrawable(context);
        this.mFacelockAnimationSet = AnimationUtils.loadAnimation(this.mContext, R$anim.facelock_lock_blink);
        this.mFacelockFailAnimationSet = AnimationUtils.loadAnimation(this.mContext, R$anim.facelock_lock_fail_blink);
        this.mFacelockRetryAnimationSet = getFacelockRetryAnimator();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
    }

    private AnimatorSet getFacelockRetryAnimator() {
        AnimatorSet animatorSet = new AnimatorSet();
        AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{1.0f, 0.5f});
        ofFloat.setDuration(150);
        ofFloat.setInterpolator(accelerateInterpolator);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d("LockIcon", "zoomOutAnimtor start");
                }
            }

            public void onAnimationEnd(Animator animator) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d("LockIcon", "zoomOutAnimtor end");
                }
            }
        });
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpLockIcon.this.setScaleX(floatValue);
                OpLockIcon.this.setScaleY(floatValue);
            }
        });
        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
        ValueAnimator ofFloat2 = ValueAnimator.ofFloat(new float[]{0.5f, 1.0f});
        ofFloat2.setDuration(150);
        ofFloat2.setInterpolator(decelerateInterpolator);
        ofFloat2.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                if (OpLockIcon.this.getLastState() == 12) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.d("LockIcon", "zoomInAnimtor start");
                    }
                    OpLockIcon.this.setRetryIcon();
                }
            }

            public void onAnimationEnd(Animator animator) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d("LockIcon", "zoomInAnimtor end");
                }
            }
        });
        ofFloat2.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpLockIcon.this.setScaleX(floatValue);
                OpLockIcon.this.setScaleY(floatValue);
            }
        });
        animatorSet.playSequentially(new Animator[]{ofFloat, ofFloat2});
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                OpLockIcon.this.setScaleX(1.0f);
                OpLockIcon.this.setScaleY(1.0f);
            }

            public void onAnimationEnd(Animator animator) {
                OpLockIcon.this.setScaleX(1.0f);
                OpLockIcon.this.setScaleY(1.0f);
            }
        });
        return animatorSet;
    }

    /* access modifiers changed from: private */
    public void setRetryIcon() {
        setBackground(this.mContext.getDrawable(R$drawable.op_facelock_lock_ripple_drawable));
        boolean isFacelockWaitingTap = KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockWaitingTap();
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("setRetryIcon, ");
            sb.append(isFacelockWaitingTap);
            Log.d("LockIcon", sb.toString());
        }
        if (!isFacelockWaitingTap) {
            setImageDrawable(this.mContext.getDrawable(R$drawable.facelock_refresh_fod), false);
        } else {
            setImageDrawable(this.mContext.getDrawable(R$drawable.facelock_lock_icon_fod), false);
        }
    }

    public void opSetScreenOn(boolean z) {
        if (!z) {
            if (this.mFacelockRetryAnimationSet.isStarted()) {
                this.mFacelockRetryAnimationSet.cancel();
            }
            removeCallbacks(this.mPaddingRetryRunnable);
        }
    }

    public void setFacelockRunning(int i, boolean z) {
        if (this.mFacelockRunningType != i) {
            StringBuilder sb = new StringBuilder();
            sb.append("setFacelockRunning , type:");
            sb.append(i);
            sb.append(", updateIcon:");
            sb.append(z);
            Log.d("LockIcon", sb.toString());
            this.mFacelockRunningType = i;
            if (z) {
                opUpdate();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void opUpdateIconAnimation(final View view, int i) {
        if (this.mFacelockAnimationSet != null && this.mFacelockFailAnimationSet != null) {
            String str = "LockIcon";
            if (!KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockRecognizing()) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d(str, "stop recog anim");
                }
                view.clearAnimation();
                this.mFacelockAnimationSet.setAnimationListener(null);
                if (this.mFacelockRetryAnimationSet.isStarted()) {
                    this.mFacelockRetryAnimationSet.cancel();
                }
                if (i == 12) {
                    if (getScreenOn()) {
                        if (Build.DEBUG_ONEPLUS) {
                            Log.d(str, "play retry anim");
                        }
                        this.mFacelockRetryAnimationSet.start();
                        return;
                    }
                    if (Build.DEBUG_ONEPLUS) {
                        Log.d(str, "screen is off, padding show try icon");
                    }
                    removeCallbacks(this.mPaddingRetryRunnable);
                    postDelayed(this.mPaddingRetryRunnable, 150);
                } else if (KeyguardUpdateMonitor.getInstance(this.mContext).shouldPlayFacelockFailAnim()) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.d(str, "play fail anim");
                    }
                    view.startAnimation(this.mFacelockFailAnimationSet);
                }
            } else {
                this.mFacelockAnimationSet.setAnimationListener(new AnimationListener() {
                    public void onAnimationRepeat(Animation animation) {
                    }

                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        if (KeyguardUpdateMonitor.getInstance(OpLockIcon.this.mContext).isFacelockRecognizing() && OpLockIcon.this.mFacelockAnimationSet != null) {
                            if (Build.DEBUG_ONEPLUS) {
                                Log.d("LockIcon", "start recog anim again");
                            }
                            OpLockIcon.this.mFacelockAnimationSet.setAnimationListener(this);
                            view.startAnimation(OpLockIcon.this.mFacelockAnimationSet);
                        }
                    }
                });
                if (Build.DEBUG_ONEPLUS) {
                    Log.d(str, "start anim");
                }
                view.startAnimation(this.mFacelockAnimationSet);
            }
        }
    }

    /* access modifiers changed from: protected */
    public int opGetState() {
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean isFingerprintDetectionRunning = instance.isFingerprintDetectionRunning();
        boolean isUnlockingWithBiometricAllowed = instance.isUnlockingWithBiometricAllowed();
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockAvailable()) {
            return 12;
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockRecognizing()) {
            return 11;
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isCameraErrorState()) {
            return 13;
        }
        if (!getUnlockMethodCache().isMethodSecure()) {
            return 14;
        }
        if (getTransientBiometricsError() && !OpUtils.isCustomFingerprint()) {
            return 3;
        }
        if (getUnlockMethodCache().canSkipBouncer()) {
            return 1;
        }
        return (!isFingerprintDetectionRunning || !isUnlockingWithBiometricAllowed || OpUtils.isCustomFingerprint()) ? 0 : 15;
    }

    public /* synthetic */ void lambda$new$0$OpLockIcon() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("LockIcon", "mPaddingRetryRunnable run");
        }
        setRetryIcon();
    }

    /* access modifiers changed from: protected */
    public void opUpdateClickability() {
        if (getAccessibilityController() != null) {
            boolean isAccessibilityEnabled = getAccessibilityController().isAccessibilityEnabled();
            boolean z = true;
            boolean z2 = getUnlockMethodCache().isTrustManaged() && !isAccessibilityEnabled;
            boolean canSkipBouncer = getUnlockMethodCache().canSkipBouncer();
            boolean z3 = getUnlockMethodCache().isTrustManaged() && !isAccessibilityEnabled && canSkipBouncer;
            boolean isFacelockAvailable = KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockAvailable();
            boolean z4 = OpUtils.isCustomFingerprint() && KeyguardUpdateMonitor.getInstance(this.mContext).isCameraErrorState();
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("opUpdateClickability: clickToUnlock= ");
                sb.append(isAccessibilityEnabled);
                sb.append(", canSkipBouncer= ");
                sb.append(canSkipBouncer);
                sb.append(", clickToForceLock= ");
                sb.append(z2);
                sb.append(", longClickToForceLock= ");
                sb.append(z3);
                sb.append(", isFacelockAvailable= ");
                sb.append(isFacelockAvailable);
                sb.append(", isCameraErrorState= ");
                sb.append(z4);
                Log.d("LockIcon", sb.toString());
            }
            boolean z5 = isFacelockAvailable || z4;
            setClickable(z2 || isAccessibilityEnabled || z5);
            if (!z3 && !z5) {
                z = false;
            }
            setLongClickable(z);
            setFocusable(getAccessibilityController().isAccessibilityEnabled());
        }
    }

    public void opSetDeviceInteractive(boolean z) {
        this.mDeviceInteractive = z;
        opUpdate();
    }

    /* access modifiers changed from: protected */
    public Drawable opGetIconForState(int i, boolean z, boolean z2) {
        int i2;
        if (i == 0) {
            i2 = R$drawable.ic_lock_24dp;
        } else if (i != 1) {
            if (i != 3) {
                switch (i) {
                    case 11:
                        return this.mContext.getDrawable(R$drawable.facelock_lock_icon_fod);
                    case 12:
                        return this.mContext.getDrawable(R$drawable.facelock_lock_icon_fod);
                    case 13:
                        return this.mContext.getDrawable(R$drawable.facelock_alert_fod);
                    case 14:
                        i2 = R$drawable.ic_lock_empty;
                        break;
                    case 15:
                        if (z && z2) {
                            i2 = R$drawable.ic_fingerprint;
                            break;
                        } else {
                            i2 = R$drawable.lockscreen_fingerprint_draw_on_animation;
                            break;
                        }
                    default:
                        throw new IllegalArgumentException();
                }
            } else {
                i2 = R$drawable.ic_fingerprint_error;
            }
        } else if (KeyguardUpdateMonitor.getInstance(this.mContext).canSkipBouncerByFacelock() || !KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockUnlocking()) {
            i2 = R$drawable.ic_lock_open_fod;
        } else {
            i2 = R$drawable.facelock_lock_icon_fod;
        }
        return this.mContext.getDrawable(i2);
    }

    /* access modifiers changed from: protected */
    public int opGetAnimationResForTransition(int i, int i2, boolean z, boolean z2, boolean z3, boolean z4) {
        if (KeyguardUpdateMonitor.getInstance(this.mContext).shouldShowFacelockIcon()) {
            return -1;
        }
        if (i == 15 && i2 == 3) {
            return R$drawable.lockscreen_fingerprint_fp_to_error_state_animation;
        }
        if (i == 1 && i2 == 3) {
            return R$drawable.op_trusted_state_to_error_animation;
        }
        if (i == 3 && i2 == 1) {
            return R$drawable.op_error_to_trustedstate_animation;
        }
        if (i == 3 && i2 == 15) {
            return R$drawable.lockscreen_fingerprint_error_state_to_fp_animation;
        }
        if (i == 15 && i2 == 1 && !getUnlockMethodCache().isTrusted()) {
            return R$drawable.lockscreen_fingerprint_draw_off_animation;
        }
        if (i2 != 15 || ((z3 || !z4 || !z2) && (!z4 || z || !z2))) {
            return -1;
        }
        return R$drawable.lockscreen_fingerprint_draw_on_animation;
    }

    private AccessibilityController getAccessibilityController() {
        return (AccessibilityController) OpReflectionUtils.getValue(LockIcon.class, this, "mAccessibilityController");
    }

    /* access modifiers changed from: private */
    public int getLastState() {
        return ((Integer) OpReflectionUtils.getValue(LockIcon.class, this, "mLastState")).intValue();
    }

    private boolean getScreenOn() {
        return ((Boolean) OpReflectionUtils.getValue(LockIcon.class, this, "mScreenOn")).booleanValue();
    }

    private void opUpdate() {
        OpReflectionUtils.methodInvokeVoid(LockIcon.class, this, "update", new Object[0]);
    }

    private UnlockMethodCache getUnlockMethodCache() {
        return (UnlockMethodCache) OpReflectionUtils.getValue(LockIcon.class, this, "mUnlockMethodCache");
    }

    private boolean getTransientBiometricsError() {
        return ((Boolean) OpReflectionUtils.getValue(LockIcon.class, this, "mTransientBiometricsError")).booleanValue();
    }
}
