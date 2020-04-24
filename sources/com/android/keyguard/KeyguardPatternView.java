package com.android.keyguard;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricSourceType;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.CellState;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.android.keyguard.EmergencyButton.EmergencyButtonCallback;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$plurals;
import com.android.systemui.R$string;
import com.oneolus.anim.OpFadeAnim;
import com.oneplus.keyguard.OpEmergencyPanel;
import com.oneplus.keyguard.OpEmergencyPanel.PanelCallback;
import com.oneplus.keyguard.OpKeyguardMessageArea;
import com.oneplus.keyguard.OpKeyguardPatternView;
import com.oneplus.util.OpUtils;
import com.oneplus.util.VibratorSceneUtils;
import java.util.List;

public class KeyguardPatternView extends OpKeyguardPatternView implements KeyguardSecurityView, AppearAnimationCreator<CellState>, EmergencyButtonCallback {
    private final AppearAnimationUtils mAppearAnimationUtils;
    /* access modifiers changed from: private */
    public KeyguardSecurityCallback mCallback;
    /* access modifiers changed from: private */
    public Runnable mCancelPatternRunnable;
    private ViewGroup mContainer;
    private CountDownTimer mCountdownTimer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    private int mDisappearYTranslation;
    private View mEcaView;
    private View mEmergencyBubblePanel;
    /* access modifiers changed from: private */
    public EmergencyButton mEmergencyButton;
    private OpEmergencyPanel mEmergencyPanel;
    private boolean mEmergencyPanelShow;
    private View mFingerprintIcon;
    private Runnable mFinishRunnable;
    private boolean mIsMonitorRegistered;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private long mLastPokeTime;
    /* access modifiers changed from: private */
    public boolean mLockOut;
    private final Rect mLockPatternScreenBounds;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public LockPatternView mLockPatternView;
    /* access modifiers changed from: private */
    public int mMaxCountdownTimes;
    KeyguardUpdateMonitorCallback mMonitorCallback;
    int mOrientation;
    final PanelCallback mPanelCallback;
    /* access modifiers changed from: private */
    public AsyncTask<?, ?, ?> mPendingLockCheck;
    @VisibleForTesting
    KeyguardMessageArea mSecurityMessageDisplay;
    private final Rect mTempRect;
    private final int[] mTmpPosition;

    private class UnlockPatternListener implements OnPatternListener {
        public void onPatternCleared() {
        }

        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.setVisibility(0);
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
            KeyguardPatternView.this.mSecurityMessageDisplay.setMessage((CharSequence) "");
        }

        public void onPatternCellAdded(List<Cell> list) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<Cell> list) {
            KeyguardPatternView.this.mLockPatternView.disableInput();
            String str = "SecurityPatternView";
            if (KeyguardPatternView.this.mPendingLockCheck != null) {
                Log.d(str, "onPatternDetected to cancel");
                KeyguardPatternView.this.mPendingLockCheck.cancel(false);
            }
            final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (list.size() < 4) {
                KeyguardPatternView.this.mLockPatternView.enableInput();
                KeyguardMessageArea keyguardMessageArea = KeyguardPatternView.this.mSecurityMessageDisplay;
                if (keyguardMessageArea != null) {
                    keyguardMessageArea.setMessage(R$string.kg_at_least_four_points, 1);
                }
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                onPatternChecked(currentUser, false, 0, false);
                return;
            }
            Log.d(str, "checkPattern begin");
            if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(3);
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(4);
            }
            KeyguardPatternView keyguardPatternView = KeyguardPatternView.this;
            keyguardPatternView.mPendingLockCheck = LockPatternChecker.checkPattern(keyguardPatternView.mLockPatternUtils, list, currentUser, new OnCheckCallback() {
                public void onEarlyMatched() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onEarlyMatched: ");
                    sb.append(currentUser);
                    Log.d("SecurityPatternView", sb.toString());
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(3);
                    }
                    String str = "lock_unlock_success";
                    String str2 = "pattern";
                    KeyguardPatternView.this.mCallback.reportMDMEvent(str, str2, Integer.toString(KeyguardPatternView.this.mKeyguardUpdateMonitor.getFingerprintFailedUnlockAttempts()));
                    UnlockPatternListener.this.onPatternChecked(currentUser, true, 0, true);
                }

                public void onChecked(boolean z, int i) {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(4);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("onChecked,");
                    sb.append(z);
                    sb.append(",");
                    sb.append(i);
                    Log.d("SecurityPatternView", sb.toString());
                    if (!z) {
                        KeyguardPatternView.this.mCallback.reportMDMEvent("lock_unlock_failed", "pattern", "1");
                    }
                    if (!z) {
                        KeyguardPatternView.this.mLockPatternView.enableInput();
                    }
                    KeyguardPatternView.this.mLockPatternView.enableInput();
                    KeyguardPatternView.this.mPendingLockCheck = null;
                    if (!z) {
                        UnlockPatternListener.this.onPatternChecked(currentUser, false, i, true);
                    }
                }

                public void onCancelled() {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(4);
                    }
                }
            });
            if (list.size() > 2) {
                KeyguardPatternView.this.mCallback.userActivity();
            }
        }

        /* access modifiers changed from: private */
        public void onPatternChecked(int i, boolean z, int i2, boolean z2) {
            boolean z3 = KeyguardUpdateMonitor.getCurrentUser() == i;
            if (z) {
                KeyguardPatternView.this.mLockPatternUtils.sanitizePassword();
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(i, true, 0);
                if (z3) {
                    KeyguardPatternView.this.mLockPatternView.setDisplayMode(DisplayMode.Correct);
                    KeyguardPatternView.this.mCallback.dismiss(true, i);
                    return;
                }
                return;
            }
            KeyguardPatternView.this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            if (z2) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(i, false, i2);
                if (KeyguardPatternView.this.mMaxCountdownTimes <= 0 && i2 > 0) {
                    KeyguardPatternView.this.handleAttemptLockout(KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline(i, i2));
                }
            }
            if (i2 == 0) {
                int failedUnlockAttempts = KeyguardPatternView.this.mKeyguardUpdateMonitor.getFailedUnlockAttempts(i);
                KeyguardPatternView keyguardPatternView = KeyguardPatternView.this;
                KeyguardMessageArea keyguardMessageArea = keyguardPatternView.mSecurityMessageDisplay;
                if (keyguardMessageArea != null) {
                    int i3 = failedUnlockAttempts % 5;
                    if (i3 == 3) {
                        keyguardMessageArea.setMessage(R$string.kg_wrong_pattern_warning, 1);
                    } else if (i3 == 4) {
                        keyguardMessageArea.setMessage(R$string.kg_wrong_pattern_warning_one, 1);
                    } else {
                        keyguardMessageArea.setMessage((CharSequence) keyguardPatternView.getMessageWithCount(R$string.kg_wrong_pattern), 1);
                    }
                }
                KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 2000);
            }
            if (VibratorSceneUtils.isVibratorSceneSupported(KeyguardPatternView.this.mContext, 1014)) {
                VibratorSceneUtils.doVibrateWithSceneMultipleTimes(KeyguardPatternView.this.mContext, (Vibrator) KeyguardPatternView.this.mContext.getSystemService("vibrator"), 1014, 0, 70, 3);
            }
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean needsInput() {
        return false;
    }

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    public KeyguardPatternView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTmpPosition = new int[2];
        this.mTempRect = new Rect();
        this.mLockPatternScreenBounds = new Rect();
        this.mCountdownTimer = null;
        this.mIsMonitorRegistered = false;
        this.mLastPokeTime = -7000;
        this.mCancelPatternRunnable = new Runnable() {
            public void run() {
                KeyguardPatternView.this.mLockPatternView.clearPattern();
                KeyguardPatternView.this.mLockPatternView.setVisibility(0);
            }
        };
        this.mMaxCountdownTimes = 0;
        this.mLockOut = false;
        this.mPanelCallback = new PanelCallback() {
            public void onTimeout() {
                super.onTimeout();
                KeyguardPatternView.this.showEmergencyPanel(false);
            }

            public void onDrop() {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onDrop:");
                    sb.append(KeyguardPatternView.this.mEmergencyButton != null);
                    Log.i("SecurityPatternView", sb.toString());
                }
                if (KeyguardPatternView.this.mEmergencyButton != null) {
                    KeyguardPatternView.this.mEmergencyButton.takeEmergencyCallAction();
                }
            }

            public void onBubbleTouched() {
                if (KeyguardPatternView.this.mCallback != null) {
                    KeyguardPatternView.this.mCallback.userActivity();
                }
            }
        };
        this.mMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onScreenTurnedOff() {
                super.onScreenTurnedOff();
                if (Build.DEBUG_ONEPLUS) {
                    Log.d("SecurityPatternView", "onScreenTurnedOff");
                }
                KeyguardPatternView.this.startFinishRunnable();
            }
        };
        this.mOrientation = 1;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        AppearAnimationUtils appearAnimationUtils = new AppearAnimationUtils(context, 500, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, 17563662));
        this.mAppearAnimationUtils = appearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils = new DisappearAnimationUtils(context, 125, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtils = disappearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils2 = new DisappearAnimationUtils(context, 187, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtilsLocked = disappearAnimationUtils2;
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        this.mCallback = keyguardSecurityCallback;
        if (isShowEmergencyPanel()) {
            this.mEmergencyPanel = this.mCallback.getEmergencyPanel();
            OpEmergencyPanel opEmergencyPanel = this.mEmergencyPanel;
            if (opEmergencyPanel != null) {
                this.mEmergencyBubblePanel = opEmergencyPanel.findViewById(R$id.bubble_panel);
            }
        }
    }

    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
        if (lockPatternUtils == null) {
            lockPatternUtils = new LockPatternUtils(this.mContext);
        }
        this.mLockPatternUtils = lockPatternUtils;
        this.mLockPatternView = findViewById(R$id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        this.mContainer = (ViewGroup) findViewById(R$id.container);
        this.mFingerprintIcon = findViewById(R$id.fingerprint_icon);
        EmergencyButton emergencyButton = (EmergencyButton) findViewById(R$id.emergency_call_button);
        if (emergencyButton != null) {
            emergencyButton.setCallback(this);
        }
        this.mEmergencyButton = emergencyButton;
        View findViewById = findViewById(R$id.cancel_button);
        if (findViewById != null) {
            findViewById.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    KeyguardPatternView.this.lambda$onFinishInflate$0$KeyguardPatternView(view);
                }
            });
        }
        displayDefaultSecurityMessage();
    }

    public /* synthetic */ void lambda$onFinishInflate$0$KeyguardPatternView(View view) {
        this.mCallback.reset();
        this.mCallback.onCancelClicked();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
    }

    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        long elapsedRealtime = SystemClock.elapsedRealtime() - this.mLastPokeTime;
        if (super.onTouchEvent(motionEvent) && elapsedRealtime > 6900) {
            this.mLastPokeTime = SystemClock.elapsedRealtime();
        }
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(this.mLockPatternView, this.mTempRect);
        Rect rect = this.mTempRect;
        motionEvent.offsetLocation((float) rect.left, (float) rect.top);
        boolean dispatchTouchEvent = this.mLockPatternView.dispatchTouchEvent(motionEvent);
        Rect rect2 = this.mTempRect;
        motionEvent.offsetLocation((float) (-rect2.left), (float) (-rect2.top));
        notifyEmergencyPanelTouchEvent(motionEvent);
        return isInArea(this.mLockPatternView, motionEvent);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mLockPatternView.getLocationOnScreen(this.mTmpPosition);
        Rect rect = this.mLockPatternScreenBounds;
        int[] iArr = this.mTmpPosition;
        rect.set(iArr[0] - 40, iArr[1] - 40, iArr[0] + this.mLockPatternView.getWidth() + 40, this.mTmpPosition[1] + this.mLockPatternView.getHeight() + 40);
    }

    public void reset() {
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setVisibility(0);
        if (this.mSecurityMessageDisplay != null) {
            long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
            if (lockoutAttemptDeadline != 0) {
                handleAttemptLockout(lockoutAttemptDeadline);
            } else {
                if (this.mLockOut) {
                    this.mKeyguardUpdateMonitor.clearFailedUnlockAttempts(true);
                    this.mLockOut = false;
                }
                displayDefaultSecurityMessage();
            }
        }
    }

    /* access modifiers changed from: private */
    public void displayDefaultSecurityMessage() {
        int i = this.mKeyguardUpdateMonitor.isFirstUnlock() ? R$string.kg_first_unlock_instructions : R$string.kg_pattern_instructions;
        KeyguardMessageArea keyguardMessageArea = this.mSecurityMessageDisplay;
        if (keyguardMessageArea != null) {
            keyguardMessageArea.setMessage((CharSequence) getMessageWithCount(i));
        }
    }

    /* access modifiers changed from: private */
    public String getMessageWithCount(int i) {
        String string = getContext().getString(i);
        int failedUnlockAttempts = this.mMaxCountdownTimes - KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts(KeyguardUpdateMonitor.getCurrentUser());
        if (this.mMaxCountdownTimes <= 0 || failedUnlockAttempts <= 0) {
            return string;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(" - ");
        sb.append(getContext().getResources().getString(R$string.kg_remaining_attempts, new Object[]{Integer.valueOf(failedUnlockAttempts)}));
        return sb.toString();
    }

    /* access modifiers changed from: private */
    public void handleAttemptLockout(long j) {
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setEnabled(false);
        this.mKeyguardUpdateMonitor.updateBiometricListeningState();
        this.mKeyguardUpdateMonitor.notifyPasswordLockout();
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.hideSecurityIcon();
        }
        this.mLockOut = true;
        C05813 r0 = new CountDownTimer(((long) Math.ceil(((double) (j - SystemClock.elapsedRealtime())) / 1000.0d)) * 1000, 1000) {
            public void onTick(long j) {
                int round = (int) Math.round(((double) j) / 1000.0d);
                KeyguardPatternView keyguardPatternView = KeyguardPatternView.this;
                keyguardPatternView.mSecurityMessageDisplay.setMessage((CharSequence) keyguardPatternView.mContext.getResources().getQuantityString(R$plurals.kg_too_many_failed_attempts_countdown, round, new Object[]{Integer.valueOf(round)}));
            }

            public void onFinish() {
                KeyguardPatternView.this.mLockPatternView.setEnabled(true);
                KeyguardPatternView.this.displayDefaultSecurityMessage();
                KeyguardPatternView.this.mKeyguardUpdateMonitor.clearFailedUnlockAttempts(true);
                KeyguardPatternView.this.mLockOut = false;
                KeyguardPatternView.this.mKeyguardUpdateMonitor.updateBiometricListeningState();
            }
        };
        this.mCountdownTimer = r0.start();
    }

    public void onPause() {
        String str = "SecurityPatternView";
        if (this.mCountdownTimer != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("onPause to cancel CountdownTimer, ");
            sb.append(Debug.getCallers(7));
            Log.d(str, sb.toString());
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
        if (this.mPendingLockCheck != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onPause to cancel, ");
            sb2.append(Debug.getCallers(7));
            Log.d(str, sb2.toString());
            this.mPendingLockCheck.cancel(false);
            this.mPendingLockCheck = null;
        }
        displayDefaultSecurityMessage();
        reset();
        showEmergencyPanel(false, true);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("onPause mIsMonitorRegistered:");
            sb3.append(this.mIsMonitorRegistered);
            Log.d(str, sb3.toString());
        }
        if (this.mIsMonitorRegistered) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mMonitorCallback);
            this.mIsMonitorRegistered = false;
        }
    }

    public void onResume(int i) {
        showEmergencyPanel(false, true);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onResume:");
            sb.append(i);
            sb.append(" mIsMonitorRegistered:");
            sb.append(this.mIsMonitorRegistered);
            Log.d("SecurityPatternView", sb.toString());
        }
        if (!this.mIsMonitorRegistered && i == 1) {
            this.mKeyguardUpdateMonitor.registerCallback(this.mMonitorCallback);
            this.mIsMonitorRegistered = true;
        }
    }

    public void showPromptReason(int i) {
        long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (lockoutAttemptDeadline != 0 && lockoutAttemptDeadline > elapsedRealtime) {
            return;
        }
        if (this.mKeyguardUpdateMonitor.isFirstUnlock() && !KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
            KeyguardMessageArea keyguardMessageArea = this.mSecurityMessageDisplay;
            if (keyguardMessageArea != null) {
                keyguardMessageArea.setMessage((CharSequence) getMessageWithCount(R$string.kg_first_unlock_instructions));
            }
        } else if (this.mKeyguardUpdateMonitor.isFacelockAvailable()) {
            KeyguardMessageArea keyguardMessageArea2 = this.mSecurityMessageDisplay;
            if (keyguardMessageArea2 != null) {
                keyguardMessageArea2.setMessage(R$string.face_unlock_notify_pattern);
            }
        } else {
            if (i == 0) {
                int biometricTimeoutStringWhenPrompt = this.mKeyguardUpdateMonitor.getBiometricTimeoutStringWhenPrompt(BiometricSourceType.FACE, SecurityMode.Pattern);
                if (OpUtils.isWeakFaceUnlockEnabled() && biometricTimeoutStringWhenPrompt != 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("WeakFace pattern, ");
                    sb.append(i);
                    sb.append(", ");
                    sb.append(biometricTimeoutStringWhenPrompt);
                    Log.d("SecurityPatternView", sb.toString());
                    KeyguardMessageArea keyguardMessageArea3 = this.mSecurityMessageDisplay;
                    if (keyguardMessageArea3 != null) {
                        keyguardMessageArea3.setMessage(biometricTimeoutStringWhenPrompt);
                    }
                }
            } else if (i == 1) {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_restart_pattern);
            } else if (i == 2) {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_timeout_pattern);
            } else if (i == 3) {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_device_admin);
            } else if (i != 4) {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_timeout_pattern);
            } else {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_user_request);
            }
        }
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList) {
        this.mSecurityMessageDisplay.setNextMessageColor(colorStateList);
        this.mSecurityMessageDisplay.setMessage(charSequence, OpKeyguardMessageArea.getOpMsgType(this.mContext, colorStateList));
    }

    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            AppearAnimationUtils appearAnimationUtils = this.mAppearAnimationUtils;
            appearAnimationUtils.createAnimation((View) this.mSecurityMessageDisplay, 0, 220, appearAnimationUtils.getStartTranslation(), true, this.mAppearAnimationUtils.getInterpolator(), (Runnable) null);
        }
        View view = this.mFingerprintIcon;
        if (view != null && view.getVisibility() == 0) {
            AnimatorSet fadeInOutVisibilityAnimation = OpFadeAnim.getFadeInOutVisibilityAnimation(this.mFingerprintIcon, 0, null, true);
            if (fadeInOutVisibilityAnimation != null) {
                fadeInOutVisibilityAnimation.start();
            }
        }
    }

    public boolean startDisappearAnimation(Runnable runnable) {
        DisappearAnimationUtils disappearAnimationUtils;
        float f = this.mKeyguardUpdateMonitor.needsSlowUnlockTransition() ? 1.5f : 1.0f;
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, (long) (300.0f * f), -this.mDisappearAnimationUtils.getStartTranslation(), this.mDisappearAnimationUtils.getInterpolator());
        if (Build.DEBUG_ONEPLUS) {
            Log.i("SecurityPatternView", "startDisappearAnimation");
        }
        this.mFinishRunnable = runnable;
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            public final void run() {
                KeyguardPatternView.this.lambda$startDisappearAnimation$1$KeyguardPatternView();
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            DisappearAnimationUtils disappearAnimationUtils2 = this.mDisappearAnimationUtils;
            disappearAnimationUtils2.createAnimation((View) this.mSecurityMessageDisplay, 0, (long) (f * 200.0f), (-disappearAnimationUtils2.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
        }
        View view = this.mFingerprintIcon;
        if (view != null && view.getVisibility() == 0) {
            DisappearAnimationUtils disappearAnimationUtils3 = this.mDisappearAnimationUtils;
            disappearAnimationUtils3.createAnimation(this.mFingerprintIcon, 0, 200, (-disappearAnimationUtils3.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
        }
        return true;
    }

    public /* synthetic */ void lambda$startDisappearAnimation$1$KeyguardPatternView() {
        enableClipping(true);
        if (Build.DEBUG_ONEPLUS) {
            Log.i("SecurityPatternView", " disappearAnimationUtils end:");
        }
        startFinishRunnable();
    }

    /* access modifiers changed from: private */
    public void startFinishRunnable() {
        if (this.mFinishRunnable != null) {
            if (Build.DEBUG_ONEPLUS) {
                Log.d("SecurityPatternView", "startFinishRunnable");
            }
            this.mFinishRunnable.run();
            this.mFinishRunnable = null;
        }
    }

    /* access modifiers changed from: private */
    public void enableClipping(boolean z) {
        setClipChildren(z);
        this.mContainer.setClipToPadding(z);
        this.mContainer.setClipChildren(z);
    }

    public void createAnimation(CellState cellState, long j, long j2, float f, boolean z, Interpolator interpolator, Runnable runnable) {
        this.mLockPatternView.startCellStateAnimation(cellState, 1.0f, z ? 1.0f : 0.0f, z ? f : 0.0f, z ? 0.0f : f, z ? 0.0f : 1.0f, 1.0f, j, j2, interpolator, runnable);
        if (runnable != null) {
            this.mAppearAnimationUtils.createAnimation(this.mEcaView, j, j2, f, z, interpolator, (Runnable) null);
        }
    }

    public CharSequence getTitle() {
        return getContext().getString(17040180);
    }

    public boolean isCheckingPassword() {
        return this.mPendingLockCheck != null;
    }

    public void onConfigurationChanged(Configuration configuration) {
        if (this.mOrientation != configuration.orientation) {
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("onConfigurationChanged forom:");
                sb.append(this.mOrientation);
                sb.append(" to:");
                sb.append(configuration.orientation);
                Log.d("SecurityPatternView", sb.toString());
            }
            this.mOrientation = configuration.orientation;
            if (this.mOrientation == 1) {
                this.mLockPatternView.setVisibility(4);
                this.mLockPatternView.removeCallbacks(this.mCancelPatternRunnable);
                this.mLockPatternView.postDelayed(this.mCancelPatternRunnable, 800);
            }
        }
    }

    private boolean isInArea(View view, MotionEvent motionEvent) {
        if (!(view == null || motionEvent == null)) {
            int[] iArr = new int[2];
            view.getLocationOnScreen(iArr);
            int i = iArr[0];
            int i2 = iArr[1];
            int width = view.getWidth();
            int height = view.getHeight();
            float rawY = motionEvent.getRawY();
            float rawX = motionEvent.getRawX();
            if (rawX > ((float) (width + i)) || rawX < ((float) i) || rawY > ((float) (height + i2)) || rawY < ((float) i2)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void showEmergencyPanel(boolean z) {
        showEmergencyPanel(z, false);
    }

    private void showEmergencyPanel(boolean z, boolean z2) {
        String str = "SecurityPatternView";
        if (this.mEmergencyPanel == null) {
            Log.i(str, "showEmergencyPanel is null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("showEmergencyPanel:");
        sb.append(z);
        sb.append(" mEmergencyPanelShow:");
        sb.append(this.mEmergencyPanelShow);
        Log.i(str, sb.toString());
        if (z2 || this.mEmergencyPanelShow != z) {
            this.mEmergencyPanelShow = z;
            if (z) {
                this.mEmergencyPanel.setVisibility(0);
                this.mEmergencyPanel.onStart();
                this.mEmergencyPanel.addCallback(this.mPanelCallback);
                this.mEcaView.setVisibility(4);
                KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
                if (keyguardSecurityCallback != null) {
                    keyguardSecurityCallback.userActivity();
                }
            } else {
                this.mEmergencyPanel.setVisibility(8);
                this.mEmergencyPanel.onStop();
                this.mEmergencyPanel.removeCallback();
                this.mEcaView.setVisibility(0);
            }
        }
    }

    public void onLaunchEmergencyPanel() {
        showEmergencyPanel(true);
    }

    public boolean isShowEmergencyPanel() {
        return OpUtils.isSupportEmergencyPanel();
    }

    private void notifyEmergencyPanelTouchEvent(MotionEvent motionEvent) {
        if (this.mEmergencyPanel != null && this.mEmergencyBubblePanel != null) {
            int action = motionEvent.getAction();
            if (isInArea(this.mEmergencyBubblePanel, motionEvent) || action != 0) {
                this.mEmergencyPanel.dispatchTouchEvent(motionEvent);
            } else {
                showEmergencyPanel(false);
            }
        }
    }
}
