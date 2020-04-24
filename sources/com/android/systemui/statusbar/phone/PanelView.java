package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.BoostFramework;
import android.util.Log;
import android.util.OpFeatures;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Interpolator;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$bool;
import com.android.systemui.R$dimen;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.phone.NotificationPanelView.PanelExpansionListener;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.oneplus.battery.OpChargingAnimationController;
import com.oneplus.keyguard.OpKeyguardUpdateMonitor;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.statusbar.phone.OpPanelView;
import com.oneplus.systemui.util.OpMdmLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class PanelView extends OpPanelView {
    public static final String TAG = "PanelView";
    /* access modifiers changed from: private */
    public boolean mAnimateAfterExpanding;
    private boolean mAnimatingOnDown;
    protected PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapsedAndHeadsUpOnDown;
    private long mDownTime;
    private boolean mExpandLatencyTracking;
    private float mExpandedFraction = 0.0f;
    protected float mExpandedHeight = 0.0f;
    protected boolean mExpanding;
    protected PanelExpansionListener mExpansionListener;
    private FalsingManager mFalsingManager;
    private int mFixedDuration = -1;
    private FlingAnimationUtils mFlingAnimationUtils;
    private FlingAnimationUtils mFlingAnimationUtilsClosing;
    private FlingAnimationUtils mFlingAnimationUtilsDismissing;
    private final Runnable mFlingCollapseRunnable = new Runnable() {
        public void run() {
            PanelView panelView = PanelView.this;
            panelView.fling(0.0f, false, panelView.mNextCollapseSpeedUpFactor, false);
        }
    };
    private GestureDetector mGestureDetector;
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    protected HeadsUpManagerPhone mHeadsUpManager;
    private ValueAnimator mHeightAnimator;
    private int mHighlightHintVisualWidth;
    protected int mHighlightHintVisualX;
    private boolean mHightHintIntercepting;
    protected boolean mHintAnimationRunning;
    private float mHintDistance;
    private boolean mIgnoreXTouchSlop;
    private float mInitialOffsetOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    /* access modifiers changed from: private */
    public boolean mInstantExpanding;
    private boolean mJustPeeked;
    protected KeyguardBottomAreaView mKeyguardBottomArea;
    protected final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    protected boolean mLaunchingNotification;
    private LockscreenGestureLogger mLockscreenGestureLogger = new LockscreenGestureLogger();
    private float mMinExpandHeight;
    private boolean mMotionAborted;
    /* access modifiers changed from: private */
    public float mNextCollapseSpeedUpFactor = 1.0f;
    private boolean mNotificationsDragEnabled;
    private int mOrientation;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private boolean mPanelUpdateWhenAnimatorEnds;
    /* access modifiers changed from: private */
    public ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekTouching;
    /* access modifiers changed from: private */
    public BoostFramework mPerf = null;
    protected final Runnable mPostCollapseRunnable = new Runnable() {
        public void run() {
            PanelView.this.collapse(false, 1.0f);
        }
    };
    protected StatusBar mStatusBar;
    protected final SysuiStatusBarStateController mStatusBarStateController = ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class));
    private boolean mTouchAboveFalsingThreshold;
    private boolean mTouchDisabled;
    protected int mTouchSlop;
    private boolean mTouchSlopExceeded;
    private boolean mTouchStartedInEmptyArea;
    protected boolean mTracking;
    private int mTrackingPointer;
    private int mUnlockFalsingThreshold;
    private boolean mUpdateFlingOnLayout;
    private float mUpdateFlingVelocity;
    private boolean mUpwardsWhenTresholdReached;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private boolean mVibrateOnOpening;
    private final VibratorHelper mVibratorHelper;
    private String mViewName;

    /* access modifiers changed from: protected */
    public abstract boolean fullyExpandedClearAllVisible();

    /* access modifiers changed from: protected */
    public abstract int getClearAllHeight();

    /* access modifiers changed from: protected */
    public abstract int getMaxPanelHeight();

    /* access modifiers changed from: protected */
    public abstract float getOpeningHeight();

    /* access modifiers changed from: protected */
    public abstract float getOverExpansionAmount();

    /* access modifiers changed from: protected */
    public abstract float getOverExpansionPixels();

    /* access modifiers changed from: protected */
    public abstract boolean hasConflictingGestures();

    /* access modifiers changed from: protected */
    public abstract boolean isClearAllVisible();

    /* access modifiers changed from: protected */
    public abstract boolean isInContentBounds(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract boolean isPanelVisibleBecauseOfHeadsUp();

    /* access modifiers changed from: protected */
    public boolean isScrolledToBottom() {
        return true;
    }

    /* access modifiers changed from: protected */
    public abstract boolean isTrackingBlocked();

    /* access modifiers changed from: protected */
    public void onExpandingStarted() {
    }

    /* access modifiers changed from: protected */
    public abstract void onHeightUpdated(float f);

    /* access modifiers changed from: protected */
    public abstract boolean onMiddleClicked();

    /* access modifiers changed from: protected */
    public void opFlingToHeightAnimatorForBiometricUnlock() {
    }

    public abstract void resetViews(boolean z);

    /* access modifiers changed from: protected */
    public abstract void setOverExpansion(float f, boolean z);

    /* access modifiers changed from: protected */
    public abstract boolean shouldGestureIgnoreXTouchSlop(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract boolean shouldUseDismissingAnimation();

    /* access modifiers changed from: protected */
    public void onExpandingFinished() {
        this.mBar.onExpandingFinished();
    }

    /* access modifiers changed from: private */
    public void notifyExpandingStarted() {
        if (!this.mExpanding) {
            this.mExpanding = true;
            onExpandingStarted();
        }
    }

    /* access modifiers changed from: protected */
    public final void notifyExpandingFinished() {
        endClosing();
        if (this.mExpanding) {
            this.mExpanding = false;
            onExpandingFinished();
        }
    }

    private void runPeekAnimation(long j, float f, final boolean z) {
        if (checkRunPeekAnimation()) {
            this.mPeekHeight = f;
            if (this.mHeightAnimator == null) {
                ObjectAnimator objectAnimator = this.mPeekAnimator;
                if (objectAnimator != null) {
                    objectAnimator.cancel();
                }
                this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", new float[]{this.mPeekHeight}).setDuration(j);
                this.mPeekAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
                this.mPeekAnimator.addListener(new AnimatorListenerAdapter() {
                    private boolean mCancelled;

                    public void onAnimationCancel(Animator animator) {
                        this.mCancelled = true;
                    }

                    public void onAnimationEnd(Animator animator) {
                        PanelView.this.mPeekAnimator = null;
                        if (!this.mCancelled && z) {
                            PanelView panelView = PanelView.this;
                            panelView.postOnAnimation(panelView.mPostCollapseRunnable);
                        }
                    }
                });
                notifyExpandingStarted();
                this.mPeekAnimator.start();
                this.mJustPeeked = true;
            }
        }
    }

    public PanelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f, 0.6f);
        this.mFlingAnimationUtilsClosing = new FlingAnimationUtils(context, 0.5f, 0.6f);
        FlingAnimationUtils flingAnimationUtils = new FlingAnimationUtils(context, 0.5f, 0.2f, 0.6f, 0.84f);
        this.mFlingAnimationUtilsDismissing = flingAnimationUtils;
        this.mBounceInterpolator = new BounceInterpolator();
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mNotificationsDragEnabled = getResources().getBoolean(R$bool.config_enableNotificationShadeDrag);
        this.mVibratorHelper = (VibratorHelper) Dependency.get(VibratorHelper.class);
        this.mVibrateOnOpening = this.mContext.getResources().getBoolean(R$bool.config_vibrateOnIconAnimation);
        this.mPerf = new BoostFramework();
        this.mGestureDetector = new GestureDetector(context, new SimpleOnGestureListener());
        this.mGestureDetector.setOnDoubleTapListener(this);
        this.mOrientation = context.getResources().getConfiguration().orientation;
    }

    /* access modifiers changed from: protected */
    public void loadDimens() {
        Resources resources = getContext().getResources();
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mHintDistance = resources.getDimension(R$dimen.hint_move_distance);
        this.mUnlockFalsingThreshold = resources.getDimensionPixelSize(R$dimen.unlock_falsing_threshold);
        if (((HighlightHintController) Dependency.get(HighlightHintController.class)).showOvalLayout()) {
            this.mHighlightHintVisualWidth = resources.getDimensionPixelSize(R$dimen.highlight_hint_icon_size_notch) + (resources.getDimensionPixelSize(R$dimen.highlight_hint_padding) * 2);
        }
    }

    private void addMovement(MotionEvent motionEvent) {
        float rawX = motionEvent.getRawX() - motionEvent.getX();
        float rawY = motionEvent.getRawY() - motionEvent.getY();
        motionEvent.offsetLocation(rawX, rawY);
        this.mVelocityTracker.addMovement(motionEvent);
        motionEvent.offsetLocation(-rawX, -rawY);
    }

    public void setTouchAndAnimationDisabled(boolean z) {
        this.mTouchDisabled = z;
        if (this.mTouchDisabled) {
            cancelHeightAnimator();
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            notifyExpandingFinished();
        }
    }

    public void startExpandLatencyTracking() {
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionStart(0);
            this.mExpandLatencyTracking = true;
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        if (!this.mInstantExpanding && ((!this.mTouchDisabled || motionEvent.getActionMasked() == 3) && (!this.mMotionAborted || motionEvent.getActionMasked() == 0))) {
            if (this.mGestureDetector.onTouchEvent(motionEvent)) {
                return true;
            }
            if (!this.mNotificationsDragEnabled) {
                if (this.mTracking) {
                    onTrackingStopped(true);
                }
                return false;
            } else if (!isFullyCollapsed() || !motionEvent.isFromSource(8194)) {
                StatusBar statusBar = this.mStatusBar;
                if (statusBar == null || !statusBar.isQsDisabled() || this.mStatusBarStateController.getState() == 1 || !this.mStatusBar.isKeyguardShowing()) {
                    int findPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
                    if (findPointerIndex < 0) {
                        this.mTrackingPointer = motionEvent.getPointerId(0);
                        findPointerIndex = 0;
                    }
                    float x = motionEvent.getX(findPointerIndex);
                    float y = motionEvent.getY(findPointerIndex);
                    if (motionEvent.getActionMasked() == 0) {
                        this.mGestureWaitForTouchSlop = isFullyCollapsed() || hasConflictingGestures();
                        this.mIgnoreXTouchSlop = isFullyCollapsed() || shouldGestureIgnoreXTouchSlop(x, y);
                    }
                    int actionMasked = motionEvent.getActionMasked();
                    if (actionMasked != 0) {
                        if (actionMasked != 1) {
                            if (actionMasked == 2) {
                                addMovement(motionEvent);
                                float f = y - this.mInitialTouchY;
                                if (Math.abs(f) > ((float) this.mTouchSlop) && (Math.abs(f) > Math.abs(x - this.mInitialTouchX) || this.mIgnoreXTouchSlop)) {
                                    this.mTouchSlopExceeded = true;
                                    if (this.mGestureWaitForTouchSlop && !this.mTracking && !this.mCollapsedAndHeadsUpOnDown) {
                                        if (!this.mJustPeeked && this.mInitialOffsetOnTouch != 0.0f) {
                                            startExpandMotion(x, y, false, this.mExpandedHeight);
                                            f = 0.0f;
                                        }
                                        cancelHeightAnimator();
                                        onTrackingStarted();
                                    }
                                }
                                float max = Math.max(0.0f, this.mInitialOffsetOnTouch + f);
                                if (max > this.mPeekHeight) {
                                    ObjectAnimator objectAnimator = this.mPeekAnimator;
                                    if (objectAnimator != null) {
                                        objectAnimator.cancel();
                                    }
                                    this.mJustPeeked = false;
                                } else if (this.mPeekAnimator == null && this.mJustPeeked) {
                                    float f2 = this.mExpandedHeight;
                                    this.mInitialOffsetOnTouch = f2;
                                    this.mInitialTouchY = y;
                                    this.mMinExpandHeight = f2;
                                    this.mJustPeeked = false;
                                }
                                float max2 = Math.max(max, this.mMinExpandHeight);
                                if ((-f) >= ((float) getFalsingThreshold())) {
                                    this.mTouchAboveFalsingThreshold = true;
                                    this.mUpwardsWhenTresholdReached = isDirectionUpwards(x, y);
                                }
                                if (!this.mJustPeeked && ((!this.mGestureWaitForTouchSlop || this.mTracking) && !isTrackingBlocked())) {
                                    setExpandedHeightInternal(max2);
                                }
                            } else if (actionMasked != 3) {
                                if (actionMasked != 5) {
                                    if (actionMasked == 6) {
                                        int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                                        if (this.mTrackingPointer == pointerId) {
                                            int i = motionEvent.getPointerId(0) != pointerId ? 0 : 1;
                                            float y2 = motionEvent.getY(i);
                                            float x2 = motionEvent.getX(i);
                                            this.mTrackingPointer = motionEvent.getPointerId(i);
                                            startExpandMotion(x2, y2, true, this.mExpandedHeight);
                                        }
                                    }
                                } else if (this.mStatusBarStateController.getState() == 1) {
                                    this.mMotionAborted = true;
                                    endMotionEvent(motionEvent, x, y, true);
                                    return false;
                                }
                            }
                        }
                        addMovement(motionEvent);
                        endMotionEvent(motionEvent, x, y, false);
                    } else {
                        startExpandMotion(x, y, false, this.mExpandedHeight);
                        this.mJustPeeked = false;
                        this.mMinExpandHeight = 0.0f;
                        this.mPanelClosedOnDown = isFullyCollapsed();
                        this.mHasLayoutedSinceDown = false;
                        this.mUpdateFlingOnLayout = false;
                        this.mMotionAborted = false;
                        this.mPeekTouching = this.mPanelClosedOnDown;
                        this.mDownTime = SystemClock.uptimeMillis();
                        this.mTouchAboveFalsingThreshold = false;
                        this.mCollapsedAndHeadsUpOnDown = isFullyCollapsed() && this.mHeadsUpManager.hasPinnedHeadsUp();
                        addMovement(motionEvent);
                        if (!this.mGestureWaitForTouchSlop || ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekAnimator != null)) {
                            this.mTouchSlopExceeded = (this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekAnimator != null;
                            cancelHeightAnimator();
                            cancelPeek();
                            onTrackingStarted();
                        }
                        if (isFullyCollapsed() && !this.mHeadsUpManager.hasPinnedHeadsUp() && !this.mStatusBar.isBouncerShowing() && !((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow() && !((HighlightHintController) Dependency.get(HighlightHintController.class)).isCarModeHighlightHintSHow()) {
                            startOpening(motionEvent);
                        }
                    }
                    if (!this.mGestureWaitForTouchSlop || this.mTracking) {
                        z = true;
                    }
                } else {
                    if (motionEvent.getActionMasked() == 0) {
                        Log.d(TAG, "disable panel touch when QS disabled");
                    }
                    return true;
                }
            } else {
                if (motionEvent.getAction() == 1) {
                    expand(true);
                }
                return true;
            }
        }
        return z;
    }

    private void startOpening(MotionEvent motionEvent) {
        runPeekAnimation(200, getOpeningHeight(), false);
        notifyBarPanelExpansionChanged();
        if (this.mVibrateOnOpening) {
            this.mVibratorHelper.vibrate(2);
        }
        float displayWidth = this.mStatusBar.getDisplayWidth();
        float displayHeight = this.mStatusBar.getDisplayHeight();
        this.mLockscreenGestureLogger.writeAtFractionalPosition(1328, (int) ((motionEvent.getX() / displayWidth) * 100.0f), (int) ((motionEvent.getY() / displayHeight) * 100.0f), this.mStatusBar.getRotation());
    }

    private boolean isDirectionUpwards(float f, float f2) {
        float f3 = f - this.mInitialTouchX;
        float f4 = f2 - this.mInitialTouchY;
        boolean z = false;
        if (f4 >= 0.0f) {
            return false;
        }
        if (Math.abs(f4) >= Math.abs(f3)) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void startExpandingFromPeek() {
        this.mStatusBar.handlePeekToExpandTransistion();
    }

    /* access modifiers changed from: protected */
    public void startExpandMotion(float f, float f2, boolean z, float f3) {
        this.mInitialOffsetOnTouch = f3;
        this.mInitialTouchY = f2;
        this.mInitialTouchX = f;
        if (z) {
            this.mTouchSlopExceeded = true;
            setExpandedHeight(this.mInitialOffsetOnTouch);
            onTrackingStarted();
        }
    }

    private void endMotionEvent(MotionEvent motionEvent, float f, float f2, boolean z) {
        this.mTrackingPointer = -1;
        boolean z2 = true;
        if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(f - this.mInitialTouchX) > ((float) this.mTouchSlop) || Math.abs(f2 - this.mInitialTouchY) > ((float) this.mTouchSlop) || motionEvent.getActionMasked() == 3 || z) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            float yVelocity = this.mVelocityTracker.getYVelocity();
            float hypot = (float) Math.hypot((double) this.mVelocityTracker.getXVelocity(), (double) this.mVelocityTracker.getYVelocity());
            boolean z3 = OpLsState.getInstance().getBiometricUnlockController().getMode() == 5;
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
            boolean isFacelockUnlocking = instance.isFacelockUnlocking();
            boolean z4 = (flingExpands(yVelocity, hypot, f, f2) || motionEvent.getActionMasked() == 3 || z) && !z3 && !isFacelockUnlocking;
            DozeLog.traceFling(z4, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isWakeUpComingFromTouch());
            if (!z4 && this.mStatusBarStateController.getState() == 1) {
                float displayDensity = this.mStatusBar.getDisplayDensity();
                this.mLockscreenGestureLogger.write(186, (int) Math.abs((f2 - this.mInitialTouchY) / displayDensity), (int) Math.abs(yVelocity / displayDensity));
                boolean isMotorCameraSupported = OpKeyguardUpdateMonitor.isMotorCameraSupported();
                boolean isKeyguardVisible = instance.isKeyguardVisible();
                if (Build.DEBUG_ONEPLUS) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("fling, ");
                    sb.append(isMotorCameraSupported);
                    String str2 = ", ";
                    sb.append(str2);
                    sb.append(isKeyguardVisible);
                    sb.append(str2);
                    sb.append(isFacelockUnlocking);
                    Log.i(str, sb.toString());
                }
                KeyguardMonitor keyguardMonitor = this.mKeyguardMonitor;
                if (keyguardMonitor != null && !keyguardMonitor.isSecure()) {
                    OpMdmLogger.log("lock_unlock_success", "swipe", "1");
                } else if (isMotorCameraSupported && isKeyguardVisible && !isFacelockUnlocking) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.i(TAG, "fling to start");
                    }
                    this.mStatusBar.flingToStartFacelock();
                }
            }
            fling(yVelocity, z4, isFalseTouch(f, f2));
            if (Build.DEBUG_ONEPLUS) {
                String str3 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("isFalseTouch:");
                sb2.append(isFalseTouch(f, f2));
                sb2.append(", expand:");
                sb2.append(z4);
                Log.i(str3, sb2.toString());
            }
            onTrackingStopped(z4);
            if (!z4 || !this.mPanelClosedOnDown || this.mHasLayoutedSinceDown) {
                z2 = false;
            }
            this.mUpdateFlingOnLayout = z2;
            if (this.mUpdateFlingOnLayout) {
                this.mUpdateFlingVelocity = yVelocity;
            }
        } else if (!this.mPanelClosedOnDown || this.mHeadsUpManager.hasPinnedHeadsUp() || this.mTracking || this.mStatusBar.isBouncerShowing() || this.mKeyguardMonitor.isKeyguardFadingAway() || ((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow() || ((HighlightHintController) Dependency.get(HighlightHintController.class)).isCarModeHighlightHintSHow()) {
            if (!this.mStatusBar.isBouncerShowing()) {
                onTrackingStopped(onEmptySpaceClick(this.mInitialTouchX));
            }
        } else if (SystemClock.uptimeMillis() - this.mDownTime < ((long) ViewConfiguration.getLongPressTimeout())) {
            runPeekAnimation(360, getOpeningHeight(), true);
        } else {
            postOnAnimation(this.mPostCollapseRunnable);
        }
        this.mVelocityTracker.clear();
        this.mPeekTouching = false;
    }

    /* access modifiers changed from: protected */
    public float getCurrentExpandVelocity() {
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getYVelocity();
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mUnlockFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    /* access modifiers changed from: protected */
    public void onTrackingStopped(boolean z) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(z);
        notifyBarPanelExpansionChanged();
    }

    /* access modifiers changed from: protected */
    public void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mBar.onTrackingStarted();
        notifyExpandingStarted();
        notifyBarPanelExpansionChanged();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mInstantExpanding || !this.mNotificationsDragEnabled || this.mTouchDisabled || (this.mMotionAborted && motionEvent.getActionMasked() != 0)) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onInterceptTouchEvent: mInstantExpanding = ");
            sb.append(this.mInstantExpanding);
            sb.append(", mTouchDisabled = ");
            sb.append(this.mTouchDisabled);
            sb.append(", mMotionAborted = ");
            sb.append(this.mMotionAborted);
            sb.append(", enable:");
            sb.append(this.mNotificationsDragEnabled);
            Log.d(str, sb.toString());
            return false;
        }
        int i = 1;
        if (OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive()) {
            OpLsState.getInstance().getPreventModeCtrl().disPatchTouchEvent(motionEvent);
            return true;
        } else if (OpLsState.getInstance().getBiometricUnlockController().getMode() == 5 || KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockUnlocking() || ((OpChargingAnimationController) Dependency.get(OpChargingAnimationController.class)).isAnimationStarted()) {
            return true;
        } else {
            int findPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
            if (findPointerIndex < 0) {
                this.mTrackingPointer = motionEvent.getPointerId(0);
                findPointerIndex = 0;
            }
            float x = motionEvent.getX(findPointerIndex);
            float y = motionEvent.getY(findPointerIndex);
            boolean isScrolledToBottom = isScrolledToBottom();
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked != 0) {
                if (actionMasked != 1) {
                    if (actionMasked == 2) {
                        float f = y - this.mInitialTouchY;
                        addMovement(motionEvent);
                        if (isScrolledToBottom || this.mTouchStartedInEmptyArea || this.mAnimatingOnDown) {
                            float abs = Math.abs(f);
                            int i2 = this.mTouchSlop;
                            if ((f < ((float) (-i2)) || (this.mAnimatingOnDown && abs > ((float) i2))) && abs > Math.abs(x - this.mInitialTouchX)) {
                                cancelHeightAnimator();
                                startExpandMotion(x, y, true, this.mExpandedHeight);
                                return true;
                            }
                        }
                    } else if (actionMasked != 3) {
                        if (actionMasked != 5) {
                            if (actionMasked == 6) {
                                int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                                if (this.mTrackingPointer == pointerId) {
                                    if (motionEvent.getPointerId(0) != pointerId) {
                                        i = 0;
                                    }
                                    this.mTrackingPointer = motionEvent.getPointerId(i);
                                    this.mInitialTouchX = motionEvent.getX(i);
                                    this.mInitialTouchY = motionEvent.getY(i);
                                }
                            }
                        } else if (this.mStatusBarStateController.getState() == 1) {
                            this.mMotionAborted = true;
                            this.mVelocityTracker.clear();
                        }
                    }
                }
                this.mVelocityTracker.clear();
            } else {
                this.mStatusBar.userActivity();
                this.mAnimatingOnDown = this.mHeightAnimator != null;
                this.mMinExpandHeight = 0.0f;
                this.mDownTime = SystemClock.uptimeMillis();
                if ((!this.mAnimatingOnDown || !this.mClosing || this.mHintAnimationRunning) && this.mPeekAnimator == null) {
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mTouchStartedInEmptyArea = !isInContentBounds(x, y);
                    this.mTouchSlopExceeded = false;
                    this.mJustPeeked = false;
                    this.mMotionAborted = false;
                    this.mPanelClosedOnDown = isFullyCollapsed();
                    this.mCollapsedAndHeadsUpOnDown = false;
                    this.mHasLayoutedSinceDown = false;
                    this.mUpdateFlingOnLayout = false;
                    this.mTouchAboveFalsingThreshold = false;
                    addMovement(motionEvent);
                } else {
                    cancelHeightAnimator();
                    cancelPeek();
                    this.mTouchSlopExceeded = true;
                    return true;
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void cancelHeightAnimator() {
        ValueAnimator valueAnimator = this.mHeightAnimator;
        if (valueAnimator != null) {
            if (valueAnimator.isRunning()) {
                this.mPanelUpdateWhenAnimatorEnds = false;
            }
            this.mHeightAnimator.cancel();
        }
        endClosing();
    }

    private void endClosing() {
        if (this.mClosing) {
            this.mClosing = false;
            onClosingFinished();
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        loadDimens();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        loadDimens();
        int i = this.mOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mOrientation = i2;
        }
    }

    /* access modifiers changed from: protected */
    public boolean flingExpands(float f, float f2, float f3, float f4) {
        if (this.mFalsingManager.isUnlockingDisabled() || isFalseTouch(f3, f4)) {
            return true;
        }
        boolean z = false;
        if (Math.abs(f2) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            if (getExpandedFraction() > 0.5f) {
                z = true;
            }
            return z;
        }
        if (f > 0.0f) {
            z = true;
        }
        return z;
    }

    private boolean isFalseTouch(float f, float f2) {
        if (OpFeatures.isSupport(new int[]{74})) {
            if (!this.mStatusBar.isFalsingThresholdNeeded()) {
                return false;
            }
            if (this.mFalsingManager.isClassiferEnabled()) {
                boolean isFalseTouch = this.mFalsingManager.isFalseTouch();
                if (isFalseTouch) {
                    Log.i(TAG, "isFalseTouch since mFalsingManager");
                }
                return isFalseTouch;
            } else if (!this.mTouchAboveFalsingThreshold) {
                Log.i(TAG, " isFalseTouch since mTouchAboveFalsingThreshold");
                return true;
            }
        }
        if (this.mUpwardsWhenTresholdReached) {
            return false;
        }
        boolean isDirectionUpwards = true ^ isDirectionUpwards(f, f2);
        if (isDirectionUpwards) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("isFalseTouch isDirectionUpwards return false, touch:");
            sb.append(f);
            String str2 = ",";
            sb.append(str2);
            sb.append(f2);
            sb.append(" to ");
            sb.append(this.mInitialTouchX);
            sb.append(str2);
            sb.append(this.mInitialTouchY);
            Log.i(str, sb.toString());
        }
        return isDirectionUpwards;
    }

    /* access modifiers changed from: protected */
    public void fling(float f, boolean z) {
        fling(f, z, 1.0f, false);
    }

    /* access modifiers changed from: protected */
    public void fling(float f, boolean z, boolean z2) {
        fling(f, z, 1.0f, z2);
    }

    /* access modifiers changed from: protected */
    public void fling(float f, boolean z, float f2, boolean z2) {
        cancelPeek();
        float maxPanelHeight = z ? (float) getMaxPanelHeight() : 0.0f;
        if (!z) {
            this.mClosing = true;
        }
        flingToHeight(f, z, maxPanelHeight, f2, z2);
    }

    /* access modifiers changed from: protected */
    public void flingToHeight(float f, boolean z, float f2, float f3, boolean z2) {
        boolean z3 = true;
        final boolean z4 = z && fullyExpandedClearAllVisible() && this.mExpandedHeight < ((float) (getMaxPanelHeight() - getClearAllHeight())) && !isClearAllVisible();
        if (z4) {
            f2 = (float) (getMaxPanelHeight() - getClearAllHeight());
        }
        float f4 = f2;
        if (f4 == this.mExpandedHeight || (getOverExpansionAmount() > 0.0f && z)) {
            notifyExpandingFinished();
            return;
        }
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append(" flingToHeight target:");
            sb.append(f4);
            sb.append(" getMaxPanelHeight:");
            sb.append(getMaxPanelHeight());
            sb.append(" getClearAllHeight:");
            sb.append(getClearAllHeight());
            Log.i(str, sb.toString());
        }
        if (getOverExpansionAmount() <= 0.0f) {
            z3 = false;
        }
        this.mOverExpandedBeforeFling = z3;
        ValueAnimator createHeightAnimator = createHeightAnimator(f4);
        if (z) {
            if (z2 && f < 0.0f) {
                f = 0.0f;
            }
            this.mFlingAnimationUtils.apply(createHeightAnimator, this.mExpandedHeight, f4, f, (float) getHeight());
            if (f == 0.0f) {
                createHeightAnimator.setDuration(350);
            }
        } else {
            if (!shouldUseDismissingAnimation()) {
                this.mFlingAnimationUtilsClosing.apply(createHeightAnimator, this.mExpandedHeight, f4, f, (float) getHeight());
            } else if (f == 0.0f) {
                createHeightAnimator.setInterpolator(Interpolators.PANEL_CLOSE_ACCELERATED);
                createHeightAnimator.setDuration((long) (((this.mExpandedHeight / ((float) getHeight())) * 100.0f) + 200.0f));
            } else {
                this.mFlingAnimationUtilsDismissing.apply(createHeightAnimator, this.mExpandedHeight, f4, f, (float) getHeight());
            }
            if (f == 0.0f) {
                createHeightAnimator.setDuration((long) (((float) createHeightAnimator.getDuration()) / f3));
            }
            int i = this.mFixedDuration;
            if (i != -1) {
                createHeightAnimator.setDuration((long) i);
            }
        }
        if (this.mPerf != null) {
            this.mPerf.perfHint(4224, this.mContext.getPackageName(), -1, 3);
        }
        boolean isBiometricUnlock = OpLsState.getInstance().getBiometricUnlockController().isBiometricUnlock();
        boolean isKeyguardVisible = KeyguardUpdateMonitor.getInstance(this.mContext).isKeyguardVisible();
        String str2 = TAG;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(" isBiometricUnlock:");
        sb2.append(isBiometricUnlock);
        String str3 = ", ";
        sb2.append(str3);
        sb2.append(isKeyguardVisible);
        sb2.append(str3);
        sb2.append(this.mClosing);
        Log.i(str2, sb2.toString());
        if (!isBiometricUnlock || !isKeyguardVisible) {
            createHeightAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animator) {
                    if (PanelView.this.mPerf != null) {
                        PanelView.this.mPerf.perfLockRelease();
                    }
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    if (PanelView.this.mPerf != null) {
                        PanelView.this.mPerf.perfLockRelease();
                    }
                    if (z4 && !this.mCancelled) {
                        PanelView panelView = PanelView.this;
                        panelView.setExpandedHeightInternal((float) panelView.getMaxPanelHeight());
                    }
                    PanelView.this.setAnimator(null);
                    if (!this.mCancelled) {
                        PanelView.this.notifyExpandingFinished();
                    }
                    PanelView.this.notifyBarPanelExpansionChanged();
                }
            });
            setAnimator(createHeightAnimator);
            createHeightAnimator.start();
            return;
        }
        opFlingToHeightAnimatorForBiometricUnlock();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mViewName = getResources().getResourceName(getId());
    }

    public void setExpandedHeight(float f) {
        setExpandedHeightInternal(f + getOverExpansionPixels());
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mStatusBar.onPanelLaidOut();
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (this.mUpdateFlingOnLayout) {
            abortAnimations();
            fling(this.mUpdateFlingVelocity, true);
            this.mUpdateFlingOnLayout = false;
        }
    }

    /* access modifiers changed from: protected */
    public void requestPanelHeightUpdate() {
        float maxPanelHeight = (float) getMaxPanelHeight();
        if (isFullyCollapsed() || maxPanelHeight == this.mExpandedHeight || this.mPeekAnimator != null || this.mPeekTouching || (this.mTracking && !isTrackingBlocked())) {
            return;
        }
        if (this.mHeightAnimator != null) {
            this.mPanelUpdateWhenAnimatorEnds = true;
        } else {
            setExpandedHeight(maxPanelHeight);
        }
    }

    public void setExpandedHeightInternal(float f) {
        float f2 = 0.0f;
        if (this.mExpandLatencyTracking && f != 0.0f) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public final void run() {
                    PanelView.this.lambda$setExpandedHeightInternal$0$PanelView();
                }
            });
            this.mExpandLatencyTracking = false;
        }
        float maxPanelHeight = ((float) getMaxPanelHeight()) - getOverExpansionAmount();
        if (this.mHeightAnimator == null) {
            float max = Math.max(0.0f, f - maxPanelHeight);
            if (getOverExpansionPixels() != max && this.mTracking) {
                setOverExpansion(max, true);
            }
            this.mExpandedHeight = Math.min(f, maxPanelHeight) + getOverExpansionAmount();
        } else {
            this.mExpandedHeight = f;
            if (this.mOverExpandedBeforeFling) {
                setOverExpansion(Math.max(0.0f, f - maxPanelHeight), false);
            }
        }
        float f3 = this.mExpandedHeight;
        if (f3 < 1.0f && f3 != 0.0f && this.mClosing) {
            this.mExpandedHeight = 0.0f;
            ValueAnimator valueAnimator = this.mHeightAnimator;
            if (valueAnimator != null) {
                valueAnimator.end();
            }
        }
        if (maxPanelHeight != 0.0f) {
            f2 = this.mExpandedHeight / maxPanelHeight;
        }
        this.mExpandedFraction = Math.min(1.0f, f2);
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("setExpandedHeightInternal: h= ");
            sb.append(f);
            sb.append(", fhWithoutOverExpansion= ");
            sb.append(maxPanelHeight);
            sb.append(", mExpandedHeight= ");
            sb.append(this.mExpandedHeight);
            sb.append(", mExpandedFraction= ");
            sb.append(this.mExpandedFraction);
            Log.i(str, sb.toString());
        }
        onHeightUpdated(this.mExpandedHeight);
        notifyBarPanelExpansionChanged();
    }

    public /* synthetic */ void lambda$setExpandedHeightInternal$0$PanelView() {
        LatencyTracker.getInstance(this.mContext).onActionEnd(0);
    }

    public void setExpandedFraction(float f) {
        setExpandedHeight(((float) getMaxPanelHeight()) * f);
    }

    public float getExpandedHeight() {
        return this.mExpandedHeight;
    }

    public float getExpandedFraction() {
        return this.mExpandedFraction;
    }

    public boolean isFullyExpanded() {
        return this.mExpandedHeight >= ((float) getMaxPanelHeight());
    }

    public boolean isFullyCollapsed() {
        return this.mExpandedFraction <= 0.0f;
    }

    public boolean isCollapsing() {
        return this.mClosing || this.mLaunchingNotification;
    }

    public boolean isTracking() {
        return this.mTracking;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }

    public void collapse(boolean z, float f) {
        if (canPanelBeCollapsed()) {
            cancelHeightAnimator();
            notifyExpandingStarted();
            this.mClosing = true;
            if (z) {
                this.mNextCollapseSpeedUpFactor = f;
                postDelayed(this.mFlingCollapseRunnable, 120);
                return;
            }
            fling(0.0f, false, f, false);
        }
    }

    public boolean canPanelBeCollapsed() {
        boolean z = OpLsState.getInstance().getBiometricUnlockController().getFaceLockMode() == 5;
        boolean z2 = OpLsState.getInstance().getBiometricUnlockController().getMode() == 5;
        boolean z3 = z || z2;
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("mExpandedFraction = ");
            sb.append(this.mExpandedFraction);
            sb.append(", mTracking = ");
            sb.append(this.mTracking);
            sb.append(", mClosing = ");
            sb.append(this.mClosing);
            sb.append(", isFaceUnlocking = ");
            sb.append(z);
            sb.append(", isBiometricUnlocking = ");
            sb.append(z2);
            Log.i(str, sb.toString());
        }
        if (!isFullyCollapsed()) {
            boolean z4 = this.mTracking;
            if ((!z4 || (z4 && z3)) && !this.mClosing) {
                return true;
            }
        }
        return false;
    }

    public void cancelPeek() {
        boolean z;
        ObjectAnimator objectAnimator = this.mPeekAnimator;
        if (objectAnimator != null) {
            z = true;
            objectAnimator.cancel();
        } else {
            z = false;
        }
        if (z) {
            notifyBarPanelExpansionChanged();
        }
    }

    public void expand(boolean z) {
        if (isFullyCollapsed() || isCollapsing()) {
            this.mInstantExpanding = true;
            this.mAnimateAfterExpanding = z;
            this.mUpdateFlingOnLayout = false;
            abortAnimations();
            cancelPeek();
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            if (this.mExpanding) {
                notifyExpandingFinished();
            }
            notifyBarPanelExpansionChanged();
            getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    if (!PanelView.this.mInstantExpanding) {
                        PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        return;
                    }
                    if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                        PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (PanelView.this.mAnimateAfterExpanding) {
                            PanelView.this.notifyExpandingStarted();
                            PanelView.this.fling(0.0f, true);
                        } else {
                            PanelView.this.setExpandedFraction(1.0f);
                        }
                        PanelView.this.mInstantExpanding = false;
                    }
                }
            });
            requestLayout();
        }
    }

    public void instantCollapse() {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("instantCollapse: mExpanding = ");
        sb.append(this.mExpanding);
        sb.append(", mInstantExpanding = ");
        sb.append(this.mInstantExpanding);
        Log.d(str, sb.toString());
        abortAnimations();
        setExpandedFraction(0.0f);
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
        if (this.mInstantExpanding) {
            this.mInstantExpanding = false;
            notifyBarPanelExpansionChanged();
        }
    }

    private void abortAnimations() {
        cancelPeek();
        cancelHeightAnimator();
        removeCallbacks(this.mPostCollapseRunnable);
        removeCallbacks(this.mFlingCollapseRunnable);
    }

    /* access modifiers changed from: protected */
    public void onClosingFinished() {
        this.mBar.onClosingFinished();
    }

    /* access modifiers changed from: protected */
    public void startUnlockHintAnimation() {
        if (this.mHeightAnimator == null && !this.mTracking) {
            cancelPeek();
            notifyExpandingStarted();
            startUnlockHintAnimationPhase1(new Runnable() {
                public final void run() {
                    PanelView.this.lambda$startUnlockHintAnimation$1$PanelView();
                }
            });
            onUnlockHintStarted();
            this.mHintAnimationRunning = true;
        }
    }

    public /* synthetic */ void lambda$startUnlockHintAnimation$1$PanelView() {
        notifyExpandingFinished();
        onUnlockHintFinished();
        this.mHintAnimationRunning = false;
    }

    /* access modifiers changed from: protected */
    public void onUnlockHintFinished() {
        this.mStatusBar.onHintFinished();
    }

    /* access modifiers changed from: protected */
    public void onUnlockHintStarted() {
        this.mStatusBar.onUnlockHintStarted();
    }

    public boolean isUnlockHintRunning() {
        return this.mHintAnimationRunning;
    }

    private void startUnlockHintAnimationPhase1(final Runnable runnable) {
        View[] viewArr;
        ValueAnimator createHeightAnimator = createHeightAnimator(Math.max(0.0f, ((float) getMaxPanelHeight()) - this.mHintDistance));
        createHeightAnimator.setDuration(250);
        createHeightAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        createHeightAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    PanelView.this.setAnimator(null);
                    runnable.run();
                    return;
                }
                PanelView.this.startUnlockHintAnimationPhase2(runnable);
            }
        });
        createHeightAnimator.start();
        setAnimator(createHeightAnimator);
        for (View view : new View[]{this.mKeyguardBottomArea.getIndicationArea(), this.mStatusBar.getAmbientIndicationContainer()}) {
            if (view != null) {
                view.animate().translationY(-this.mHintDistance).setDuration(250).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable(view) {
                    private final /* synthetic */ View f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        PanelView.this.lambda$startUnlockHintAnimationPhase1$2$PanelView(this.f$1);
                    }
                }).start();
            }
        }
    }

    public /* synthetic */ void lambda$startUnlockHintAnimationPhase1$2$PanelView(View view) {
        view.animate().translationY(0.0f).setDuration(450).setInterpolator(this.mBounceInterpolator).start();
    }

    /* access modifiers changed from: private */
    public void setAnimator(ValueAnimator valueAnimator) {
        this.mHeightAnimator = valueAnimator;
        if (valueAnimator == null && this.mPanelUpdateWhenAnimatorEnds) {
            this.mPanelUpdateWhenAnimatorEnds = false;
            requestPanelHeightUpdate();
        }
    }

    /* access modifiers changed from: private */
    public void startUnlockHintAnimationPhase2(final Runnable runnable) {
        ValueAnimator createHeightAnimator = createHeightAnimator((float) getMaxPanelHeight());
        createHeightAnimator.setDuration(450);
        createHeightAnimator.setInterpolator(this.mBounceInterpolator);
        createHeightAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                PanelView.this.setAnimator(null);
                runnable.run();
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        createHeightAnimator.start();
        setAnimator(createHeightAnimator);
    }

    private ValueAnimator createHeightAnimator(float f) {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{this.mExpandedHeight, f});
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                PanelView.this.lambda$createHeightAnimator$3$PanelView(valueAnimator);
            }
        });
        return ofFloat;
    }

    public /* synthetic */ void lambda$createHeightAnimator$3$PanelView(ValueAnimator valueAnimator) {
        setExpandedHeightInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    /* access modifiers changed from: protected */
    public void notifyBarPanelExpansionChanged() {
        PanelBar panelBar = this.mBar;
        if (panelBar != null) {
            float f = this.mExpandedFraction;
            panelBar.panelExpansionChanged(f, f > 0.0f || this.mPeekAnimator != null || this.mInstantExpanding || isPanelVisibleBecauseOfHeadsUp() || this.mTracking || this.mHeightAnimator != null);
        }
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(getContext());
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("notifyBarPanelExpansionChanged: isKeyguardShowing= ");
        sb.append(this.mStatusBar.isKeyguardShowing());
        sb.append(", isKeyguardDone= ");
        sb.append(instance.isKeyguardDone());
        sb.append(", isSwitching= ");
        sb.append(instance.isSwitchingUser());
        Log.d(str, sb.toString());
        if (!this.mStatusBar.isKeyguardShowing() && instance.isKeyguardDone() && !instance.isSwitchingUser()) {
            float f2 = this.mExpandedFraction;
            if (f2 == 0.0f) {
                instance.setQSExpanded(false);
            } else if (f2 > 0.01f) {
                instance.setQSExpanded(true);
            }
        }
        PanelExpansionListener panelExpansionListener = this.mExpansionListener;
        if (panelExpansionListener != null) {
            panelExpansionListener.onPanelExpansionChanged(this.mExpandedFraction, this.mTracking);
        }
    }

    public void setExpansionListener(PanelExpansionListener panelExpansionListener) {
        this.mExpansionListener = panelExpansionListener;
    }

    /* access modifiers changed from: protected */
    public boolean onEmptySpaceClick(float f) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        return onMiddleClicked();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Object[] objArr = new Object[11];
        objArr[0] = getClass().getSimpleName();
        objArr[1] = Float.valueOf(getExpandedHeight());
        objArr[2] = Integer.valueOf(getMaxPanelHeight());
        String str = "T";
        String str2 = "f";
        objArr[3] = this.mClosing ? str : str2;
        objArr[4] = this.mTracking ? str : str2;
        objArr[5] = this.mJustPeeked ? str : str2;
        ObjectAnimator objectAnimator = this.mPeekAnimator;
        objArr[6] = objectAnimator;
        String str3 = " (started)";
        String str4 = "";
        objArr[7] = (objectAnimator == null || !objectAnimator.isStarted()) ? str4 : str3;
        ValueAnimator valueAnimator = this.mHeightAnimator;
        objArr[8] = valueAnimator;
        if (valueAnimator == null || !valueAnimator.isStarted()) {
            str3 = str4;
        }
        objArr[9] = str3;
        if (!this.mTouchDisabled) {
            str = str2;
        }
        objArr[10] = str;
        printWriter.println(String.format("[PanelView(%s): expandedHeight=%f maxPanelHeight=%d closing=%s tracking=%s justPeeked=%s peekAnim=%s%s timeAnim=%s%s touchDisabled=%s]", objArr));
    }

    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        this.mHeadsUpManager = headsUpManagerPhone;
    }

    public void setLaunchingNotification(boolean z) {
        this.mLaunchingNotification = z;
    }

    public void collapseWithDuration(int i) {
        this.mFixedDuration = i;
        collapse(false, 1.0f);
        this.mFixedDuration = -1;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0025, code lost:
        if (r6 != 3) goto L_0x00aa;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onHightlightHintIntercept(android.view.MotionEvent r6) {
        /*
            r5 = this;
            int r0 = r5.mTrackingPointer
            int r0 = r6.findPointerIndex(r0)
            r1 = 0
            if (r0 >= 0) goto L_0x0010
            int r0 = r6.getPointerId(r1)
            r5.mTrackingPointer = r0
            r0 = r1
        L_0x0010:
            float r2 = r6.getX(r0)
            float r0 = r6.getY(r0)
            int r6 = r6.getActionMasked()
            r3 = 1
            if (r6 == 0) goto L_0x009f
            if (r6 == r3) goto L_0x0033
            r3 = 2
            if (r6 == r3) goto L_0x0029
            r0 = 3
            if (r6 == r0) goto L_0x009c
            goto L_0x00aa
        L_0x0029:
            boolean r6 = r5.shouldHightHintIntercept(r2, r0)
            if (r6 != 0) goto L_0x00aa
            r5.mHightHintIntercepting = r1
            goto L_0x00aa
        L_0x0033:
            java.lang.String r6 = TAG
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onHightlightHintIntercept / ACTION_UP / x:"
            r3.append(r4)
            r3.append(r2)
            java.lang.String r4 = ", y:"
            r3.append(r4)
            r3.append(r0)
            java.lang.String r4 = ", mHighlightHintVisualWidth:"
            r3.append(r4)
            int r4 = r5.mHighlightHintVisualWidth
            r3.append(r4)
            java.lang.String r4 = ", mHighlightHintVisualX:"
            r3.append(r4)
            int r4 = r5.mHighlightHintVisualX
            r3.append(r4)
            java.lang.String r4 = ", mOrientation:"
            r3.append(r4)
            int r4 = r5.mOrientation
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r6, r3)
            boolean r6 = r5.mHightHintIntercepting
            if (r6 == 0) goto L_0x009c
            boolean r6 = r5.shouldHightHintIntercept(r2, r0)
            if (r6 == 0) goto L_0x009c
            java.lang.Class<com.android.systemui.statusbar.phone.HighlightHintController> r6 = com.android.systemui.statusbar.phone.HighlightHintController.class
            java.lang.Object r6 = com.android.systemui.Dependency.get(r6)
            com.android.systemui.statusbar.phone.HighlightHintController r6 = (com.android.systemui.statusbar.phone.HighlightHintController) r6
            boolean r6 = r6.isCarModeHighlightHintSHow()
            if (r6 == 0) goto L_0x0097
            java.lang.Class<com.android.systemui.statusbar.phone.HighlightHintController> r6 = com.android.systemui.statusbar.phone.HighlightHintController.class
            java.lang.Object r6 = com.android.systemui.Dependency.get(r6)
            com.android.systemui.statusbar.phone.HighlightHintController r6 = (com.android.systemui.statusbar.phone.HighlightHintController) r6
            android.content.Context r0 = r5.getContext()
            r6.launchCarModeAp(r0)
            goto L_0x009c
        L_0x0097:
            com.android.systemui.statusbar.phone.StatusBar r6 = r5.mStatusBar
            r6.launchHighlightHintAp()
        L_0x009c:
            r5.mHightHintIntercepting = r1
            goto L_0x00aa
        L_0x009f:
            boolean r6 = r5.shouldHightHintIntercept(r2, r0)
            if (r6 == 0) goto L_0x00a8
            r5.mHightHintIntercepting = r3
            goto L_0x00aa
        L_0x00a8:
            r5.mHightHintIntercepting = r1
        L_0x00aa:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PanelView.onHightlightHintIntercept(android.view.MotionEvent):boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0082, code lost:
        if (r10 <= r9) goto L_0x0084;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0097, code lost:
        if (r10 <= ((float) r0.getRight())) goto L_0x0084;
     */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009c A[ADDED_TO_REGION] */
    /* JADX WARNING: Removed duplicated region for block: B:40:? A[ADDED_TO_REGION, RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean shouldHightHintIntercept(float r10, float r11) {
        /*
            r9 = this;
            com.android.systemui.statusbar.phone.StatusBar r0 = r9.mStatusBar
            android.view.View r0 = r0.getStatusBarView()
            int r1 = r0.getTop()
            float r1 = (float) r1
            int r1 = (r11 > r1 ? 1 : (r11 == r1 ? 0 : -1))
            r2 = 0
            r3 = 1
            if (r1 < 0) goto L_0x001c
            int r1 = r0.getBottom()
            float r1 = (float) r1
            int r11 = (r11 > r1 ? 1 : (r11 == r1 ? 0 : -1))
            if (r11 > 0) goto L_0x001c
            r11 = r3
            goto L_0x001d
        L_0x001c:
            r11 = r2
        L_0x001d:
            java.lang.Class<com.android.systemui.statusbar.phone.HighlightHintController> r1 = com.android.systemui.statusbar.phone.HighlightHintController.class
            java.lang.Object r1 = com.android.systemui.Dependency.get(r1)
            com.android.systemui.statusbar.phone.HighlightHintController r1 = (com.android.systemui.statusbar.phone.HighlightHintController) r1
            boolean r1 = r1.showOvalLayout()
            if (r1 == 0) goto L_0x0088
            java.lang.Class<com.android.systemui.statusbar.phone.HighlightHintController> r1 = com.android.systemui.statusbar.phone.HighlightHintController.class
            java.lang.Object r1 = com.android.systemui.Dependency.get(r1)
            com.android.systemui.statusbar.phone.HighlightHintController r1 = (com.android.systemui.statusbar.phone.HighlightHintController) r1
            boolean r1 = r1.isCarModeHighlightHintSHow()
            if (r1 != 0) goto L_0x0088
            boolean r1 = r9.isLayoutRtl()
            int r4 = r9.mOrientation
            if (r4 != r3) goto L_0x0043
            r4 = r3
            goto L_0x0044
        L_0x0043:
            r4 = r2
        L_0x0044:
            int r5 = r9.mHighlightHintVisualWidth
            float r5 = (float) r5
            r6 = 1073741824(0x40000000, float:2.0)
            float r5 = r5 / r6
            int r5 = (int) r5
            int r7 = r9.mHighlightHintVisualX
            int r7 = r7 - r5
            float r7 = (float) r7
            float r8 = r0.getX()
            int r8 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1))
            if (r8 >= 0) goto L_0x005b
            float r7 = r0.getX()
        L_0x005b:
            int r8 = r9.mHighlightHintVisualX
            int r9 = r9.mHighlightHintVisualWidth
            int r8 = r8 + r9
            int r8 = r8 + r5
            int r9 = r0.getRight()
            float r9 = (float) r9
            float r9 = r9 / r6
            if (r4 == 0) goto L_0x0071
            if (r1 == 0) goto L_0x006d
            r7 = r9
            goto L_0x0071
        L_0x006d:
            float r7 = r0.getX()
        L_0x0071:
            if (r4 == 0) goto L_0x007b
            if (r1 == 0) goto L_0x007c
            int r9 = r0.getRight()
            float r9 = (float) r9
            goto L_0x007c
        L_0x007b:
            float r9 = (float) r8
        L_0x007c:
            int r0 = (r10 > r7 ? 1 : (r10 == r7 ? 0 : -1))
            if (r0 < 0) goto L_0x0086
            int r9 = (r10 > r9 ? 1 : (r10 == r9 ? 0 : -1))
            if (r9 > 0) goto L_0x0086
        L_0x0084:
            r9 = r3
            goto L_0x009a
        L_0x0086:
            r9 = r2
            goto L_0x009a
        L_0x0088:
            float r9 = r0.getX()
            int r9 = (r10 > r9 ? 1 : (r10 == r9 ? 0 : -1))
            if (r9 < 0) goto L_0x0086
            int r9 = r0.getRight()
            float r9 = (float) r9
            int r9 = (r10 > r9 ? 1 : (r10 == r9 ? 0 : -1))
            if (r9 > 0) goto L_0x0086
            goto L_0x0084
        L_0x009a:
            if (r9 == 0) goto L_0x009f
            if (r11 == 0) goto L_0x009f
            r2 = r3
        L_0x009f:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PanelView.shouldHightHintIntercept(float, float):boolean");
    }
}
