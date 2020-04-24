package com.android.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import com.airbnb.lottie.C0526R;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityContainer.SecurityCallback;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.settingslib.Utils;
import com.android.systemui.plugins.ActivityStarter.OnDismissAction;
import com.oneplus.keyguard.OpKeyguardHostView;
import com.oneplus.util.OpUtils;
import java.io.File;

public class KeyguardHostView extends OpKeyguardHostView implements SecurityCallback {
    private AudioManager mAudioManager;
    private Runnable mCancelAction;
    private OnDismissAction mDismissAction;
    protected LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public KeyguardSecurityContainer mSecurityContainer;
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public int mType;
    private final KeyguardUpdateMonitorCallback mUpdateCallback;
    protected ViewMediatorCallback mViewMediatorCallback;

    public KeyguardHostView(Context context) {
        this(context, null);
    }

    public KeyguardHostView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTelephonyManager = null;
        this.mType = 0;
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            public void onUserSwitchComplete(int i) {
                KeyguardHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }

            public void onTrustGrantedWithFlags(int i, int i2) {
                if (i2 == KeyguardUpdateMonitor.getCurrentUser() && KeyguardHostView.this.isAttachedToWindow()) {
                    boolean isVisibleToUser = KeyguardHostView.this.isVisibleToUser();
                    boolean z = true;
                    boolean z2 = (i & 1) != 0;
                    if ((i & 2) == 0) {
                        z = false;
                    }
                    if (z2 || z) {
                        if (!KeyguardHostView.this.mViewMediatorCallback.isScreenOn() || (!isVisibleToUser && !z)) {
                            KeyguardHostView.this.mViewMediatorCallback.playTrustedSound();
                        } else {
                            if (!isVisibleToUser) {
                                Log.i("KeyguardViewBase", "TrustAgent dismissed Keyguard.");
                            }
                            KeyguardHostView.this.dismiss(false, i2);
                        }
                    }
                }
            }

            public void onKeyguardBouncerChanged(boolean z) {
                if (!KeyguardUpdateMonitor.getInstance(KeyguardHostView.this.mContext).isKeyguardVisible() && z && KeyguardHostView.this.mSecurityContainer != null) {
                    Log.d("KeyguardViewBase", "update security icon when occluded");
                    KeyguardHostView.this.mSecurityContainer.updateSecurityIcon();
                }
            }

            public void onFacelockStateChanged(int i) {
                if (KeyguardHostView.this.isVisibleToUser() && KeyguardHostView.this.mType != i && i == 0 && KeyguardHostView.this.mSecurityContainer != null && !KeyguardUpdateMonitor.getInstance(KeyguardHostView.this.mContext).isFacelockUnlocking()) {
                    KeyguardHostView.this.mSecurityContainer.updateSecurityIcon();
                }
                KeyguardHostView.this.mType = i;
            }
        };
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateCallback);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        ViewMediatorCallback viewMediatorCallback = this.mViewMediatorCallback;
        if (viewMediatorCallback != null) {
            viewMediatorCallback.keyguardDoneDrawing();
        }
    }

    public void setOnDismissAction(OnDismissAction onDismissAction, Runnable runnable) {
        Runnable runnable2 = this.mCancelAction;
        if (runnable2 != null) {
            runnable2.run();
            this.mCancelAction = null;
        }
        this.mDismissAction = onDismissAction;
        this.mCancelAction = runnable;
    }

    public boolean hasDismissActions() {
        return (this.mDismissAction == null && this.mCancelAction == null) ? false : true;
    }

    public void cancelDismissAction() {
        setOnDismissAction(null, null);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(R$id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
        this.mKeyguardSecurityNavigationSpace = findViewById(R$id.keyguard_security_navigation_space);
        updateNavigationSpace();
        if (OpUtils.isCustomFingerprint() && this.mSecurityContainer.getCurrentSecuritySelection() != SecurityMode.Password) {
            setPaddingRelative(getPaddingStart(), getResources().getDimensionPixelSize(R$dimen.emergency_button_margin_top) - getResources().getDimensionPixelSize(R$dimen.keyguard_security_view_top_margin), getPaddingEnd(), getPaddingBottom());
        }
    }

    public void showPrimarySecurityScreen() {
        Log.d("KeyguardViewBase", "show()");
        this.mSecurityContainer.showPrimarySecurityScreen(false);
        updateNavigationSpace();
    }

    public KeyguardSecurityView getCurrentSecurityView() {
        KeyguardSecurityContainer keyguardSecurityContainer = this.mSecurityContainer;
        if (keyguardSecurityContainer != null) {
            return keyguardSecurityContainer.getCurrentSecurityView();
        }
        return null;
    }

    public void showPromptReason(int i) {
        this.mSecurityContainer.showPromptReason(i);
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList) {
        this.mSecurityContainer.showMessage(charSequence, colorStateList);
    }

    public void showErrorMessage(CharSequence charSequence) {
        showMessage(charSequence, Utils.getColorError(this.mContext));
    }

    public boolean dismiss(int i) {
        return dismiss(false, i);
    }

    /* access modifiers changed from: protected */
    public KeyguardSecurityContainer getSecurityContainer() {
        return this.mSecurityContainer;
    }

    public boolean dismiss(boolean z, int i) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(z, i);
    }

    public void finish(boolean z, int i) {
        boolean z2;
        OnDismissAction onDismissAction = this.mDismissAction;
        if (onDismissAction != null) {
            z2 = onDismissAction.onDismiss();
            this.mDismissAction = null;
            this.mCancelAction = null;
        } else {
            z2 = false;
        }
        ViewMediatorCallback viewMediatorCallback = this.mViewMediatorCallback;
        if (viewMediatorCallback == null) {
            return;
        }
        if (z2) {
            viewMediatorCallback.keyguardDonePending(z, i);
        } else {
            viewMediatorCallback.keyguardDone(z, i);
        }
    }

    public void reset() {
        this.mViewMediatorCallback.resetKeyguard();
    }

    public void onCancelClicked() {
        this.mViewMediatorCallback.onCancelClicked();
    }

    public void resetSecurityContainer() {
        this.mSecurityContainer.reset();
    }

    public void onSecurityModeChanged(SecurityMode securityMode, boolean z) {
        ViewMediatorCallback viewMediatorCallback = this.mViewMediatorCallback;
        if (viewMediatorCallback != null) {
            viewMediatorCallback.setNeedsInput(z);
        }
    }

    public CharSequence getAccessibilityTitleForCurrentMode() {
        return this.mSecurityContainer.getTitle();
    }

    public void userActivity() {
        ViewMediatorCallback viewMediatorCallback = this.mViewMediatorCallback;
        if (viewMediatorCallback != null) {
            viewMediatorCallback.userActivity();
        }
    }

    public void onPause() {
        Log.d("KeyguardViewBase", String.format("screen off, instance %s at %s", new Object[]{Integer.toHexString(hashCode()), Long.valueOf(SystemClock.uptimeMillis())}));
        this.mSecurityContainer.showPrimarySecurityScreen(true);
        this.mSecurityContainer.onPause();
        clearFocus();
    }

    public void onResume() {
        StringBuilder sb = new StringBuilder();
        sb.append("screen on, instance ");
        sb.append(Integer.toHexString(hashCode()));
        Log.d("KeyguardViewBase", sb.toString());
        this.mSecurityContainer.onResume(1);
        requestFocus();
    }

    public void startAppearAnimation() {
        this.mSecurityContainer.startAppearAnimation();
    }

    public void startDisappearAnimation(Runnable runnable) {
        if (!this.mSecurityContainer.startDisappearAnimation(runnable) && runnable != null) {
            runnable.run();
        }
    }

    public void cleanUp() {
        getSecurityContainer().onPause();
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (interceptMediaKey(keyEvent)) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyEvent.getAction() == 0) {
            if (!(keyCode == 24 || keyCode == 25)) {
                if (!(keyCode == 79 || keyCode == 130)) {
                    if (keyCode != 164) {
                        if (keyCode != 222) {
                            if (!(keyCode == 126 || keyCode == 127)) {
                                switch (keyCode) {
                                    case C0526R.styleable.AppCompatTheme_listPreferredItemPaddingStart /*85*/:
                                        break;
                                    case 86:
                                    case C0526R.styleable.AppCompatTheme_panelMenuListTheme /*87*/:
                                    case C0526R.styleable.AppCompatTheme_panelMenuListWidth /*88*/:
                                    case C0526R.styleable.AppCompatTheme_popupMenuStyle /*89*/:
                                    case C0526R.styleable.AppCompatTheme_popupWindowStyle /*90*/:
                                    case C0526R.styleable.AppCompatTheme_radioButtonStyle /*91*/:
                                        break;
                                }
                            }
                            if (this.mTelephonyManager == null) {
                                this.mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
                            }
                            TelephonyManager telephonyManager = this.mTelephonyManager;
                            if (!(telephonyManager == null || telephonyManager.getCallState() == 0)) {
                                return true;
                            }
                        }
                    }
                }
                handleMediaKeyEvent(keyEvent);
                return true;
            }
            return false;
        } else if (keyEvent.getAction() == 1) {
            if (!(keyCode == 79 || keyCode == 130 || keyCode == 222 || keyCode == 126 || keyCode == 127)) {
                switch (keyCode) {
                    case C0526R.styleable.AppCompatTheme_listPreferredItemPaddingStart /*85*/:
                    case 86:
                    case C0526R.styleable.AppCompatTheme_panelMenuListTheme /*87*/:
                    case C0526R.styleable.AppCompatTheme_panelMenuListWidth /*88*/:
                    case C0526R.styleable.AppCompatTheme_popupMenuStyle /*89*/:
                    case C0526R.styleable.AppCompatTheme_popupWindowStyle /*90*/:
                    case C0526R.styleable.AppCompatTheme_radioButtonStyle /*91*/:
                        break;
                }
            }
            handleMediaKeyEvent(keyEvent);
            return true;
        }
        return false;
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        synchronized (this) {
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            }
        }
        this.mAudioManager.dispatchMediaKeyEvent(keyEvent);
    }

    public void dispatchSystemUiVisibilityChanged(int i) {
        super.dispatchSystemUiVisibilityChanged(i);
        if (!(this.mContext instanceof Activity)) {
            setSystemUiVisibility(4194304);
        }
    }

    public boolean shouldEnableMenuKey() {
        return !getResources().getBoolean(R$bool.config_disableMenuKeyInLockScreen) || ActivityManager.isRunningInTestHarness() || new File("/data/local/enable_menu_key").exists();
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mViewMediatorCallback.setNeedsInput(this.mSecurityContainer.needsInput());
    }

    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityContainer.setLockPatternUtils(lockPatternUtils);
    }

    public SecurityMode getSecurityMode() {
        return this.mSecurityContainer.getSecurityMode();
    }

    public SecurityMode getCurrentSecurityMode() {
        return this.mSecurityContainer.getCurrentSecurityMode();
    }

    public boolean isCheckingPassword() {
        return this.mSecurityContainer.isCheckingPassword();
    }

    public void reportMDMEvent(String str, String str2, String str3) {
        this.mViewMediatorCallback.reportMDMEvent(str, str2, str3);
    }

    public void tryToStartFaceLockFromBouncer() {
        this.mViewMediatorCallback.tryToStartFaceLockFromBouncer();
    }
}
