package com.android.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.hardware.biometrics.BiometricSourceType;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton.EmergencyButtonCallback;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$plurals;
import com.android.systemui.R$string;
import com.oneplus.keyguard.OpEmergencyPanel;
import com.oneplus.keyguard.OpEmergencyPanel.PanelCallback;
import com.oneplus.keyguard.OpKeyguardAbsKeyInputView;
import com.oneplus.keyguard.OpKeyguardMessageArea;
import com.oneplus.util.OpUtils;
import com.oneplus.util.VibratorSceneUtils;
import java.util.Arrays;

public abstract class KeyguardAbsKeyInputView extends OpKeyguardAbsKeyInputView implements KeyguardSecurityView, EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    private CountDownTimer mCountdownTimer;
    private boolean mDismissing;
    protected View mEcaView;
    private View mEmergencyBubblePanel;
    /* access modifiers changed from: private */
    public EmergencyButton mEmergencyButton;
    private OpEmergencyPanel mEmergencyPanel;
    private boolean mEmergencyPanelShow;
    protected boolean mEnableHaptics;
    /* access modifiers changed from: private */
    public boolean mLockOut;
    protected LockPatternUtils mLockPatternUtils;
    private int mMaxCountdownTimes;
    final PanelCallback mPanelCallback;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected boolean mResumed;
    protected SecurityMessageDisplay mSecurityMessageDisplay;
    private KeyguardUpdateMonitorCallback mUpdateCallback;

    /* access modifiers changed from: protected */
    public abstract byte[] getPasswordText();

    /* access modifiers changed from: protected */
    public abstract int getPasswordTextViewId();

    /* access modifiers changed from: protected */
    public abstract int getPromptReasonStringRes(int i);

    public boolean needsInput() {
        return false;
    }

    /* access modifiers changed from: protected */
    public abstract void resetPasswordText(boolean z, boolean z2);

    /* access modifiers changed from: protected */
    public abstract void resetState();

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryEnabled(boolean z);

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryInputEnabled(boolean z);

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long j) {
        return j != 0;
    }

    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCountdownTimer = null;
        this.mMaxCountdownTimes = 0;
        this.mLockOut = false;
        this.mPanelCallback = new PanelCallback() {
            public void onTimeout() {
                super.onTimeout();
                KeyguardAbsKeyInputView.this.showEmergencyPanel(false);
            }

            public void onDrop() {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onDrop:");
                    sb.append(KeyguardAbsKeyInputView.this.mEmergencyButton);
                    Log.i("KeyguardAbsKeyInputView", sb.toString());
                }
                if (KeyguardAbsKeyInputView.this.mEmergencyButton != null) {
                    KeyguardAbsKeyInputView.this.mEmergencyButton.takeEmergencyCallAction();
                }
            }

            public void onBubbleTouched() {
                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardAbsKeyInputView.this.mCallback;
                if (keyguardSecurityCallback != null) {
                    keyguardSecurityCallback.userActivity();
                }
            }
        };
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            public void onFinishedGoingToSleep(int i) {
                if (KeyguardAbsKeyInputView.this.mCallback.getCurrentSecurityMode() == SecurityMode.PIN) {
                    int keyguardPinPasswordLength = KeyguardUpdateMonitor.getInstance(KeyguardAbsKeyInputView.this.mContext).keyguardPinPasswordLength();
                    int i2 = 0;
                    if (KeyguardAbsKeyInputView.this.getPasswordText() != null) {
                        i2 = KeyguardAbsKeyInputView.this.getPasswordText().length;
                    }
                    if (keyguardPinPasswordLength != 0 && i2 > 3) {
                        KeyguardAbsKeyInputView.this.verifyPasswordAndUnlock();
                    }
                }
            }
        };
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
        this.mEnableHaptics = this.mLockPatternUtils.isTactileFeedbackEnabled();
    }

    public void reset() {
        this.mDismissing = false;
        resetPasswordText(false, false);
        long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(lockoutAttemptDeadline)) {
            handleAttemptLockout(lockoutAttemptDeadline);
            return;
        }
        if (this.mLockOut) {
            KeyguardUpdateMonitor.getInstance(this.mContext).clearFailedUnlockAttempts(true);
            this.mLockOut = false;
        }
        resetState();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        EmergencyButton emergencyButton = (EmergencyButton) findViewById(R$id.emergency_call_button);
        this.mMaxCountdownTimes = this.mContext.getResources().getInteger(R$integer.config_max_unlock_countdown_times);
        if (emergencyButton != null) {
            emergencyButton.setCallback(this);
        }
        this.mEmergencyButton = emergencyButton;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
    }

    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateCallback);
    }

    /* access modifiers changed from: protected */
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        if (!this.mDismissing) {
            final byte[] passwordText = getPasswordText();
            setPasswordEntryInputEnabled(false);
            String str = "KeyguardAbsKeyInputView";
            if (this.mPendingLockCheck != null) {
                Log.d(str, "verifyPasswordAndUnlock to cancel");
                this.mPendingLockCheck.cancel(false);
            }
            final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (passwordText.length <= 3) {
                setPasswordEntryInputEnabled(true);
                onPasswordChecked(currentUser, false, 0, false);
                Arrays.fill(passwordText, 0);
                return;
            }
            Log.d(str, "checkPassword begin");
            if (LatencyTracker.isEnabled(this.mContext)) {
                LatencyTracker.getInstance(this.mContext).onActionStart(3);
                LatencyTracker.getInstance(this.mContext).onActionStart(4);
            }
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, passwordText, currentUser, new OnCheckCallback() {
                public void onEarlyMatched() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onEarlyMatched: ");
                    sb.append(currentUser);
                    Log.d("KeyguardAbsKeyInputView", sb.toString());
                    if (LatencyTracker.isEnabled(KeyguardAbsKeyInputView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardAbsKeyInputView.this.mContext).onActionEnd(3);
                    }
                    int fingerprintFailedUnlockAttempts = KeyguardUpdateMonitor.getInstance(KeyguardAbsKeyInputView.this.mContext).getFingerprintFailedUnlockAttempts();
                    KeyguardAbsKeyInputView keyguardAbsKeyInputView = KeyguardAbsKeyInputView.this;
                    keyguardAbsKeyInputView.mCallback.reportMDMEvent("lock_unlock_success", keyguardAbsKeyInputView.getSecurityModeLabel(), Integer.toString(fingerprintFailedUnlockAttempts));
                    KeyguardAbsKeyInputView.this.onPasswordChecked(currentUser, true, 0, true);
                    Arrays.fill(passwordText, 0);
                }

                public void onChecked(boolean z, int i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onChecked, ");
                    sb.append(z);
                    sb.append(", ");
                    sb.append(i);
                    Log.d("KeyguardAbsKeyInputView", sb.toString());
                    if (!z) {
                        KeyguardAbsKeyInputView keyguardAbsKeyInputView = KeyguardAbsKeyInputView.this;
                        keyguardAbsKeyInputView.mCallback.reportMDMEvent("lock_unlock_failed", keyguardAbsKeyInputView.getSecurityModeLabel(), "1");
                    }
                    if (LatencyTracker.isEnabled(KeyguardAbsKeyInputView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardAbsKeyInputView.this.mContext).onActionEnd(4);
                    }
                    if (!z) {
                        KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                    }
                    KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                    KeyguardAbsKeyInputView keyguardAbsKeyInputView2 = KeyguardAbsKeyInputView.this;
                    keyguardAbsKeyInputView2.mPendingLockCheck = null;
                    if (!z) {
                        keyguardAbsKeyInputView2.onPasswordChecked(currentUser, false, i, true);
                    }
                    Arrays.fill(passwordText, 0);
                }

                public void onCancelled() {
                    if (LatencyTracker.isEnabled(KeyguardAbsKeyInputView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardAbsKeyInputView.this.mContext).onActionEnd(4);
                    }
                    Arrays.fill(passwordText, 0);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public String getSecurityModeLabel() {
        SecurityMode currentSecurityMode = this.mCallback.getCurrentSecurityMode();
        if (currentSecurityMode == SecurityMode.PIN) {
            return "pin";
        }
        return currentSecurityMode == SecurityMode.Password ? "password" : "";
    }

    /* access modifiers changed from: private */
    public void onPasswordChecked(int i, boolean z, int i2, boolean z2) {
        boolean z3 = KeyguardUpdateMonitor.getCurrentUser() == i;
        if (z) {
            this.mLockPatternUtils.sanitizePassword();
            this.mCallback.reportUnlockAttempt(i, true, 0);
            if (z3) {
                this.mDismissing = true;
                this.mCallback.dismiss(true, i);
            }
        } else {
            if (z2) {
                this.mCallback.reportUnlockAttempt(i, false, i2);
                if (this.mMaxCountdownTimes <= 0 && i2 > 0) {
                    handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(i, i2));
                }
            }
            if (i2 == 0) {
                SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
                if (securityMessageDisplay != null) {
                    securityMessageDisplay.setMessage(getWrongPasswordStringId(), 1);
                }
            }
            Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
            if (!OpUtils.isSupportLinearVibration()) {
                vibrator.vibrate(1000);
            } else if (VibratorSceneUtils.isVibratorSceneSupported(this.mContext, 1014)) {
                VibratorSceneUtils.doVibrateWithSceneMultipleTimes(this.mContext, vibrator, 1014, 80, 50, 2);
            }
        }
        resetPasswordText(true, !z);
    }

    /* access modifiers changed from: protected */
    public void handleAttemptLockout(long j) {
        setPasswordEntryEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        KeyguardUpdateMonitor.getInstance(this.mContext).updateBiometricListeningState();
        KeyguardUpdateMonitor.getInstance(this.mContext).notifyPasswordLockout();
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.hideSecurityIcon();
        }
        this.mLockOut = true;
        C05614 r0 = new CountDownTimer(((long) Math.ceil(((double) (j - elapsedRealtime)) / 1000.0d)) * 1000, 1000) {
            public void onTick(long j) {
                int round = (int) Math.round(((double) j) / 1000.0d);
                KeyguardAbsKeyInputView keyguardAbsKeyInputView = KeyguardAbsKeyInputView.this;
                keyguardAbsKeyInputView.mSecurityMessageDisplay.setMessage((CharSequence) keyguardAbsKeyInputView.mContext.getResources().getQuantityString(R$plurals.kg_too_many_failed_attempts_countdown, round, new Object[]{Integer.valueOf(round)}));
            }

            public void onFinish() {
                KeyguardAbsKeyInputView.this.resetState();
                KeyguardUpdateMonitor.getInstance(KeyguardAbsKeyInputView.this.mContext).clearFailedUnlockAttempts(true);
                KeyguardAbsKeyInputView.this.mLockOut = false;
                KeyguardUpdateMonitor.getInstance(KeyguardAbsKeyInputView.this.mContext).updateBiometricListeningState();
            }
        };
        this.mCountdownTimer = r0.start();
    }

    /* access modifiers changed from: protected */
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

    /* access modifiers changed from: protected */
    public void onUserInput() {
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.userActivity();
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) "");
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 3) {
            KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
            if (keyguardSecurityCallback != null) {
                keyguardSecurityCallback.userActivity();
            }
        } else if (i != 0) {
            if (i == 4) {
                KeyguardSecurityCallback keyguardSecurityCallback2 = this.mCallback;
                if (keyguardSecurityCallback2 != null) {
                    keyguardSecurityCallback2.userActivity();
                }
                return false;
            }
            onUserInput();
        }
        return false;
    }

    public void onPause() {
        this.mResumed = false;
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            this.mCountdownTimer = null;
        }
        AsyncTask<?, ?, ?> asyncTask = this.mPendingLockCheck;
        if (asyncTask != null) {
            asyncTask.cancel(false);
            this.mPendingLockCheck = null;
        }
        reset();
        showEmergencyPanel(false, true);
    }

    public void onResume(int i) {
        this.mResumed = true;
        showEmergencyPanel(false, true);
    }

    public void showPromptReason(int i) {
        long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (lockoutAttemptDeadline != 0 && lockoutAttemptDeadline > elapsedRealtime) {
            return;
        }
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isUserUnlocked() && !KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
            SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
            if (securityMessageDisplay != null) {
                securityMessageDisplay.setMessage(R$string.kg_first_unlock_instructions);
            }
        } else if (KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockAvailable()) {
            SecurityMessageDisplay securityMessageDisplay2 = this.mSecurityMessageDisplay;
            if (securityMessageDisplay2 != null) {
                securityMessageDisplay2.setMessage(R$string.face_unlock_notify_password);
            }
        } else {
            if (i != 0) {
                int promptReasonStringRes = getPromptReasonStringRes(i);
                if (promptReasonStringRes != 0) {
                    this.mSecurityMessageDisplay.setMessage(promptReasonStringRes);
                }
            } else if (OpUtils.isWeakFaceUnlockEnabled()) {
                KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
                if (keyguardSecurityCallback != null) {
                    int i2 = 0;
                    SecurityMode currentSecurityMode = keyguardSecurityCallback.getCurrentSecurityMode();
                    if (currentSecurityMode == SecurityMode.PIN || currentSecurityMode == SecurityMode.Password) {
                        i2 = KeyguardUpdateMonitor.getInstance(this.mContext).getBiometricTimeoutStringWhenPrompt(BiometricSourceType.FACE, currentSecurityMode);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("WeakFace, mode ");
                    sb.append(currentSecurityMode);
                    sb.append(", ");
                    sb.append(i);
                    sb.append(", resId:");
                    sb.append(i2);
                    Log.d("KeyguardAbsKeyInputView", sb.toString());
                    if (i2 != 0) {
                        SecurityMessageDisplay securityMessageDisplay3 = this.mSecurityMessageDisplay;
                        if (securityMessageDisplay3 != null) {
                            securityMessageDisplay3.setMessage(i2);
                        }
                    }
                }
            }
        }
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList) {
        this.mSecurityMessageDisplay.setNextMessageColor(colorStateList);
        this.mSecurityMessageDisplay.setMessage(charSequence, OpKeyguardMessageArea.getOpMsgType(this.mContext, colorStateList));
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }

    public boolean isCheckingPassword() {
        return this.mPendingLockCheck != null;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean onTouchEvent = super.onTouchEvent(motionEvent);
        notifyEmergencyPanelTouchEvent(motionEvent);
        return onTouchEvent;
    }

    public void onLaunchEmergencyPanel() {
        showEmergencyPanel(true);
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
        String str = "KeyguardAbsKeyInputView";
        if (this.mEmergencyPanel == null) {
            Log.i(str, "showEmergencyPanel is null");
            return;
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("showEmergencyPanel:");
            sb.append(z);
            sb.append(" mEmergencyPanelShow:");
            sb.append(this.mEmergencyPanelShow);
            Log.i(str, sb.toString());
        }
        if (this.mEmergencyPanelShow != z || z2) {
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
