package com.oneplus.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.BoostFramework;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.HighlightHintController;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpReflectionUtils;

public class OpNotificationPanelView extends PanelView {
    protected static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    protected static String TAG = "OpNotificationPanelView";
    public static int mLastCameraGestureLaunchSource = 0;
    protected Handler mHandler;
    protected LockIcon mLockIcon;
    protected boolean mNeedShowOTAWizard = false;
    private OpNotificationController mOpNotificationController;
    protected final Runnable mUpdateCameraStateTimeout = new Runnable() {
        public void run() {
            KeyguardUpdateMonitor.getInstance(OpNotificationPanelView.this.mContext).updateLaunchingCameraState(false);
        }
    };

    /* access modifiers changed from: protected */
    public boolean fullyExpandedClearAllVisible() {
        return false;
    }

    /* access modifiers changed from: protected */
    public int getClearAllHeight() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public int getMaxPanelHeight() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public float getOpeningHeight() {
        return 0.0f;
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionAmount() {
        return 0.0f;
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionPixels() {
        return 0.0f;
    }

    /* access modifiers changed from: protected */
    public boolean hasConflictingGestures() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isClearAllVisible() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isInContentBounds(float f, float f2) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isPanelVisibleBecauseOfHeadsUp() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isTrackingBlocked() {
        return false;
    }

    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onHeightUpdated(float f) {
    }

    /* access modifiers changed from: protected */
    public boolean onMiddleClicked() {
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    public void resetViews(boolean z) {
    }

    /* access modifiers changed from: protected */
    public void setOverExpansion(float f, boolean z) {
    }

    /* access modifiers changed from: protected */
    public boolean shouldGestureIgnoreXTouchSlop(float f, float f2) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUseDismissingAnimation() {
        return false;
    }

    public OpNotificationPanelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "OpNotificationPanelView init ");
        this.mOpNotificationController = (OpNotificationController) Dependency.get(OpNotificationController.class);
    }

    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (getNotificationStackScroller() == null || getNotificationStackScroller().getVisibility() != 0 || !this.mStatusBar.isFalsingThresholdNeeded() || this.mStatusBar.isBouncerShowing() || isFingerprintAuthenticating() || OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive()) {
            return false;
        }
        Log.d(TAG, "onDoubleTap to sleep");
        ((PowerManager) getContext().getSystemService("power")).goToSleep(SystemClock.uptimeMillis());
        return true;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHandler = new Handler();
    }

    /* access modifiers changed from: protected */
    public void initOpBottomArea() {
        setShowOTAWizard(this.mNeedShowOTAWizard);
    }

    /* access modifiers changed from: protected */
    public void updateLaunchingCameraState(boolean z, int i) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("launchCamera, ");
        sb.append(z);
        String str2 = ", ";
        sb.append(str2);
        sb.append(i);
        sb.append(str2);
        sb.append(isFullyCollapsed());
        sb.append(str2);
        sb.append(this.mStatusBar.getFacelockController().isFacelockRunning());
        sb.append(str2);
        sb.append(Debug.getCallers(3));
        Log.d(str, sb.toString());
        mLastCameraGestureLaunchSource = i;
        this.mHandler.removeCallbacks(this.mUpdateCameraStateTimeout);
        KeyguardUpdateMonitor.getInstance(this.mContext).updateLaunchingCameraState(true);
        this.mHandler.postDelayed(this.mUpdateCameraStateTimeout, 2000);
        if (OpLsState.getInstance().getStatusBarKeyguardViewManager() != null) {
            OpLsState.getInstance().getStatusBarKeyguardViewManager().forceHideBouncer();
        }
        this.mStatusBar.notifyCameraLaunching(null);
    }

    public void setShowOTAWizard(boolean z) {
        this.mNeedShowOTAWizard = z;
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("setShowOTAWizard, ");
        sb.append(z);
        Log.d(str, sb.toString());
        this.mKeyguardBottomArea.setShowOTAWizard(this.mNeedShowOTAWizard);
    }

    public void setUnlockAlpha(float f) {
        int i = 0;
        boolean z = f > 0.0f;
        BiometricUnlockController biometricUnlockController = OpLsState.getInstance().getBiometricUnlockController();
        if (biometricUnlockController.shouldApplySpeedUpPolicy()) {
            if (!z || getKeyguardShowing()) {
                getKeyguardStatusView().setVisibility(z ? 0 : 4);
            }
            getNotificationStackScroller().setAlpha(f);
            if (this.mKeyguardBottomArea.getVisibility() != 8) {
                KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
                if (!z) {
                    i = 4;
                }
                keyguardBottomAreaView.setVisibility(i);
            }
            if (z) {
                biometricUnlockController.resetSpeedUpPolicy();
            }
        } else {
            setAlpha(f);
        }
        setAlpha(f);
    }

    public boolean isCameraForeground() {
        String str;
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        boolean z = false;
        if (keyguardBottomAreaView == null) {
            return false;
        }
        ResolveInfo resolveCameraIntent = keyguardBottomAreaView.resolveCameraIntent();
        if (resolveCameraIntent != null) {
            ActivityInfo activityInfo = resolveCameraIntent.activityInfo;
            if (activityInfo != null) {
                str = activityInfo.packageName;
                if (isForegroundApp(str) || isForegroundApp("com.oneplus.gallery")) {
                    z = true;
                }
                return z;
            }
        }
        str = null;
        z = true;
        return z;
    }

    /* access modifiers changed from: protected */
    public void updateOpLockIcon() {
        KeyguardStatusView keyguardStatusView = getKeyguardStatusView();
        if (keyguardStatusView != null) {
            StatusBar statusBar = this.mStatusBar;
            if (statusBar != null) {
                StatusBarWindowView statusBarWindow = statusBar.getStatusBarWindow();
                if (statusBarWindow == null) {
                    Log.i(TAG, " updateOpLockIcon statusBarWindow is null");
                    return;
                }
                ViewGroup lockIconContainer = keyguardStatusView.getLockIconContainer();
                if (this.mLockIcon == null) {
                    this.mLockIcon = (LockIcon) statusBarWindow.findViewById(R$id.lock_icon);
                }
                if (lockIconContainer != null) {
                    LockIcon lockIcon = this.mLockIcon;
                    if (lockIcon != null) {
                        ((ViewGroup) lockIcon.getParent()).removeView(this.mLockIcon);
                        lockIconContainer.addView(this.mLockIcon);
                        LayoutParams layoutParams = this.mLockIcon.getLayoutParams();
                        if (layoutParams instanceof FrameLayout.LayoutParams) {
                            ((FrameLayout.LayoutParams) layoutParams).setMargins(0, 0, 0, 0);
                            this.mLockIcon.setLayoutParams(layoutParams);
                        }
                        Log.i(TAG, " updateOpLockIcon complete");
                        return;
                    }
                }
                if (lockIconContainer == null) {
                    Log.i(TAG, " updateOpLockIcon statusbarViewContainer is null");
                } else if (this.mLockIcon == null) {
                    Log.i(TAG, " updateOpLockIcon lockIcon is null");
                }
                return;
            }
        }
        if (keyguardStatusView == null) {
            Log.i(TAG, " updateOpLockIcon keyguardStatusView is null");
        } else if (this.mStatusBar == null) {
            Log.i(TAG, " updateOpLockIcon mStatusBar is null");
        }
    }

    public void setUpHighlightHintInfo() {
        HighlightHintController highlightHintController = (HighlightHintController) Dependency.get(HighlightHintController.class);
        if (highlightHintController.isHighLightHintShow() && highlightHintController.showOvalLayout()) {
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    OpNotificationPanelView.this.run();
                }
            }, 500);
        }
    }

    /* access modifiers changed from: protected */
    public ValueAnimator createHeightAnimatorForBiometricUnlock() {
        float opGetMaxClockY = (float) getKeyguardClockPositionAlgorithm().opGetMaxClockY();
        float opGetClockY = (float) getKeyguardClockPositionAlgorithm().opGetClockY();
        float f = opGetMaxClockY - 100.0f;
        float f2 = this.mExpandedHeight;
        final float f3 = f2 - (opGetClockY - opGetMaxClockY);
        final float f4 = f2 - (opGetClockY - f);
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{f3, f4});
        PathInterpolator pathInterpolator = new PathInterpolator(0.6f, 0.0f, 0.6f, 1.0f);
        boolean isPreventModeActive = OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive();
        if (isPreventModeActive) {
            ofFloat.setDuration(0);
        } else {
            ofFloat.setDuration(150);
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("createHeightAnimatorForBiometricUnlock startHeight:");
        sb.append(f3);
        sb.append(" endHeight:");
        sb.append(f4);
        sb.append(" isPreventViewShow:");
        sb.append(isPreventModeActive);
        Log.d(str, sb.toString());
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float f = f4;
                float f2 = 1.0f - ((floatValue - f) / (f3 - f));
                OpNotificationPanelView.this.setAlpha(1.0f - f2);
                if (f2 == 1.0f) {
                    OpNotificationPanelView.this.setExpandedHeightInternal(0.0f);
                } else {
                    OpNotificationPanelView.this.setExpandedHeightInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            }
        });
        ofFloat.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationStart(Animator animator) {
                Log.d(OpNotificationPanelView.TAG, "HeightAnimatorForBiometricUnlock start");
            }

            public void onAnimationCancel(Animator animator) {
                if (OpNotificationPanelView.this.getPerf() != null) {
                    OpNotificationPanelView.this.getPerf().perfLockRelease();
                }
                this.mCancelled = true;
                Log.d(OpNotificationPanelView.TAG, "HeightAnimatorForBiometricUnlock cancel");
            }

            public void onAnimationEnd(Animator animator) {
                String str = OpNotificationPanelView.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("HeightAnimatorForBiometricUnlock end, Cancelled:");
                sb.append(this.mCancelled);
                Log.d(str, sb.toString());
                OpNotificationPanelView.this.setAlpha(1.0f);
                if (OpNotificationPanelView.this.getPerf() != null) {
                    OpNotificationPanelView.this.getPerf().perfLockRelease();
                }
                OpNotificationPanelView.this.setAnimator(null);
                if (!this.mCancelled) {
                    OpNotificationPanelView.this.notifyExpandingFinished();
                }
                OpNotificationPanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        ofFloat.setInterpolator(pathInterpolator);
        return ofFloat;
    }

    public AnimatorSet getNotificationAppearAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();
        PathInterpolator pathInterpolator = new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, new float[]{50.0f, 0.0f});
        ofFloat.setDuration(300);
        ofFloat.setInterpolator(pathInterpolator);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this, View.ALPHA, new float[]{0.0f, 1.0f});
        ofFloat2.setDuration(300);
        ofFloat2.setInterpolator(pathInterpolator);
        animatorSet.playTogether(new Animator[]{ofFloat, ofFloat2});
        animatorSet.setStartDelay(150);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            boolean mCancelled;

            public void onAnimationStart(Animator animator) {
                this.mCancelled = false;
                Log.i(OpNotificationPanelView.TAG, "notificationEnterAnimation onAnimationStart:");
                OpNotificationPanelView.this.setTranslationY(100.0f);
                OpNotificationPanelView.this.setAlpha(0.0f);
            }

            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animator) {
                Log.i(OpNotificationPanelView.TAG, "notificationEnterAnimation onAnimationEnd:");
                OpNotificationPanelView.this.setTranslationY(0.0f);
                OpNotificationPanelView.this.setAlpha(1.0f);
            }
        });
        return animatorSet;
    }

    /* access modifiers changed from: protected */
    public void opFlingToHeightAnimatorForBiometricUnlock() {
        ValueAnimator createHeightAnimatorForBiometricUnlock = createHeightAnimatorForBiometricUnlock();
        setAnimator(createHeightAnimatorForBiometricUnlock);
        createHeightAnimatorForBiometricUnlock.start();
    }

    private KeyguardStatusView getKeyguardStatusView() {
        return (KeyguardStatusView) OpReflectionUtils.getValue(NotificationPanelView.class, this, "mKeyguardStatusView");
    }

    private NotificationStackScrollLayout getNotificationStackScroller() {
        return (NotificationStackScrollLayout) OpReflectionUtils.getValue(NotificationPanelView.class, this, "mNotificationStackScroller");
    }

    public boolean isClosingWithAlphaFadeOut() {
        return ((Boolean) OpReflectionUtils.getValue(NotificationPanelView.class, this, "mClosingWithAlphaFadeOut")).booleanValue();
    }

    private boolean isFingerprintAuthenticating() {
        return OpLsState.getInstance().getBiometricUnlockController().isFingerprintAuthenticating();
    }

    private boolean isForegroundApp(String str) {
        Object methodInvokeWithArgs = OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(NotificationPanelView.class, "isForegroundApp", String.class), str);
        if (methodInvokeWithArgs != null) {
            return ((Boolean) methodInvokeWithArgs).booleanValue();
        }
        String str2 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Get null when calling isForegroundApp pkg: ");
        sb.append(str);
        Log.d(str2, sb.toString());
        return false;
    }

    /* access modifiers changed from: private */
    public BoostFramework getPerf() {
        return (BoostFramework) OpReflectionUtils.getValue(PanelView.class, this, "mPerf");
    }

    private KeyguardClockPositionAlgorithm getKeyguardClockPositionAlgorithm() {
        return (KeyguardClockPositionAlgorithm) OpReflectionUtils.getValue(NotificationPanelView.class, this, "mClockPositionAlgorithm");
    }

    /* access modifiers changed from: private */
    public void setAnimator(ValueAnimator valueAnimator) {
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(PanelView.class, "setAnimator", ValueAnimator.class), valueAnimator);
    }

    private boolean getKeyguardShowing() {
        return ((Boolean) OpReflectionUtils.getValue(NotificationPanelView.class, this, "mKeyguardShowing")).booleanValue();
    }

    /* access modifiers changed from: private */
    public void run() {
        StatusBar statusBar = this.mStatusBar;
        if (statusBar != null && statusBar.getOpStatusBarView() != null) {
            View findViewById = this.mStatusBar.getOpStatusBarView().findViewById(R$id.highlight_hint_container);
            int[] iArr = new int[2];
            if (findViewById != null) {
                findViewById.getLocationOnScreen(iArr);
            }
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("HighlightHintInfo target[0]:");
            sb.append(iArr[0]);
            Log.i(str, sb.toString());
            this.mHighlightHintVisualX = iArr[0];
        }
    }

    /* access modifiers changed from: protected */
    public boolean skipOnTouchEvent() {
        if (!this.mOpNotificationController.isPanelDisabledInBrickMode()) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "Skip onTouchEvent by BrickMode");
        }
        return true;
    }
}
