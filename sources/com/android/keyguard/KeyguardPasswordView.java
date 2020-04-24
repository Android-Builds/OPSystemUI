package com.android.keyguard;

import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Rect;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.systemui.Dependency;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.oneolus.anim.OpFadeAnim;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;
import java.util.List;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements KeyguardSecurityView, OnEditorActionListener, TextWatcher {
    private static final boolean IS_CUSTOM_FINGERPRINT = OpUtils.isCustomFingerprint();
    private final String TAG;
    private final int mDisappearYTranslation;
    private Interpolator mFastOutLinearInInterpolator;
    /* access modifiers changed from: private */
    public View mFingerprintIcon;
    /* access modifiers changed from: private */
    public boolean mHasWindowFocus;
    private boolean mHideNavigationBar;
    InputMethodManager mImm;
    private boolean mIsResume;
    private Interpolator mLinearOutSlowInInterpolator;
    /* access modifiers changed from: private */
    public int mOriginFingerprintIconHeight;
    /* access modifiers changed from: private */
    public int mOriginPasswordEntryHeight;
    private OverviewProxyService mOverviewProxyService;
    /* access modifiers changed from: private */
    public TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;
    /* access modifiers changed from: private */
    public final boolean mShowImeAtScreenOn;
    private View mSwitchImeButton;

    public boolean needsInput() {
        return true;
    }

    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.TAG = "KeyguardPasswordView";
        this.mHideNavigationBar = false;
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mIsResume = false;
        this.mShowImeAtScreenOn = context.getResources().getBoolean(R$bool.kg_show_ime_at_screen_on);
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        this.mPasswordEntry.setTextOperationUser(UserHandle.of(KeyguardUpdateMonitor.getCurrentUser()));
        displayDefaultSecurityMessage();
        boolean isEnabled = this.mPasswordEntry.isEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append(" resetState wasDisabled:");
        sb.append(isEnabled);
        sb.append(", ");
        sb.append(isNeedToPendingIME());
        sb.append(", mResumed:");
        sb.append(this.mResumed);
        sb.append(", mPasswordEntry.isVisibleToUser():");
        sb.append(this.mPasswordEntry.isVisibleToUser());
        String str = "KeyguardPasswordView";
        Log.i(str, sb.toString());
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
        String str2 = "reset to hide IME when not shown";
        if (isNeedToPendingIME()) {
            KeyguardUpdateMonitor.getInstance(getContext()).updateBiometricListeningState();
            if (!isShown() && this.mHideNavigationBar) {
                this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
                Log.d(str, str2);
            }
        } else if (isShown() || !this.mHideNavigationBar) {
            if (isEnabled) {
                requestShowIME();
            }
        } else {
            this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
            Log.d(str, str2);
        }
    }

    private void displayDefaultSecurityMessage() {
        int i = KeyguardUpdateMonitor.getInstance(getContext()).isFirstUnlock() ? R$string.kg_first_unlock_instructions : R$string.kg_password_instructions;
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage((CharSequence) getMessageWithCount(i));
        }
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R$id.passwordEntry;
    }

    public void onResume(final int i) {
        super.onResume(i);
        this.mIsResume = true;
        post(new Runnable() {
            public void run() {
                if (KeyguardPasswordView.this.mPasswordEntry != null) {
                    KeyguardPasswordView keyguardPasswordView = KeyguardPasswordView.this;
                    keyguardPasswordView.mOriginPasswordEntryHeight = keyguardPasswordView.mPasswordEntry.getHeight();
                }
                if (KeyguardPasswordView.this.mFingerprintIcon != null) {
                    KeyguardPasswordView keyguardPasswordView2 = KeyguardPasswordView.this;
                    keyguardPasswordView2.mOriginFingerprintIconHeight = keyguardPasswordView2.mFingerprintIcon.getHeight();
                }
                String str = "KeyguardPasswordView";
                if (!(!OpUtils.DEBUG_ONEPLUS || KeyguardPasswordView.this.mPasswordEntry == null || KeyguardPasswordView.this.mFingerprintIcon == null)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onResume, mPwdEntry.getHeight():");
                    sb.append(KeyguardPasswordView.this.mPasswordEntry.getHeight());
                    sb.append(", mPwdEntry.getMeasuredHeight():");
                    sb.append(KeyguardPasswordView.this.mPasswordEntry.getMeasuredHeight());
                    sb.append(", mFIcon.getHeight():");
                    sb.append(KeyguardPasswordView.this.mFingerprintIcon.getHeight());
                    sb.append(", mFIcon.getMeasuredHeight():");
                    sb.append(KeyguardPasswordView.this.mFingerprintIcon.getMeasuredHeight());
                    sb.append(", getHeight():");
                    sb.append(KeyguardPasswordView.this.getHeight());
                    Log.i(str, sb.toString());
                }
                if (KeyguardPasswordView.this.isShown() && KeyguardPasswordView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(" onResume reason:");
                    sb2.append(i);
                    Log.d(str, sb2.toString());
                    if (i == 1 && !KeyguardPasswordView.this.mShowImeAtScreenOn) {
                        return;
                    }
                    if (!KeyguardPasswordView.this.isNeedToPendingIME()) {
                        KeyguardPasswordView.this.requestShowIME();
                    } else {
                        KeyguardUpdateMonitor.getInstance(KeyguardPasswordView.this.getContext()).updateBiometricListeningState();
                    }
                }
            }
        });
        TextView textView = this.mPasswordEntry;
        if (textView != null && this.mFingerprintIcon != null) {
            textView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    if (KeyguardPasswordView.this.mHasWindowFocus) {
                        KeyguardPasswordView.this.mPasswordEntry.removeOnLayoutChangeListener(this);
                        if (OpUtils.DEBUG_ONEPLUS) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("onLayoutChange, mPwdEntry.getHeight():");
                            sb.append(KeyguardPasswordView.this.mPasswordEntry.getHeight());
                            sb.append(", mOriginPasswordEntryHeight:");
                            sb.append(KeyguardPasswordView.this.mOriginPasswordEntryHeight);
                            sb.append(", mFIcon.getHeight():");
                            sb.append(KeyguardPasswordView.this.mFingerprintIcon.getHeight());
                            sb.append(", mOriginFingerprintIconHeight:");
                            sb.append(KeyguardPasswordView.this.mOriginFingerprintIconHeight);
                            Log.i("KeyguardPasswordView", sb.toString());
                        }
                        if (KeyguardPasswordView.this.mPasswordEntry.getHeight() < KeyguardPasswordView.this.mOriginPasswordEntryHeight) {
                            LayoutParams layoutParams = (LayoutParams) KeyguardPasswordView.this.mFingerprintIcon.getLayoutParams();
                            layoutParams.height = KeyguardPasswordView.this.mOriginFingerprintIconHeight - (KeyguardPasswordView.this.mOriginPasswordEntryHeight - KeyguardPasswordView.this.mPasswordEntry.getHeight());
                            KeyguardPasswordView.this.mFingerprintIcon.setLayoutParams(layoutParams);
                        }
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void requestShowIME() {
        boolean z = false;
        boolean z2 = !KeyguardUpdateMonitor.getInstance(getContext()).isUserUnlocked();
        if (OpLsState.getInstance().getPreventModeCtrl() != null) {
            z = OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("requestShowIME: mIsResume:");
        sb.append(this.mIsResume);
        sb.append(" isShown:");
        sb.append(isShown());
        sb.append(" hideNavigationBar:");
        sb.append(this.mHideNavigationBar);
        sb.append(" isPreventModeActive:");
        sb.append(z);
        sb.append(" input Enable:");
        sb.append(this.mPasswordEntry.isEnabled());
        String str = "KeyguardPasswordView";
        Log.i(str, sb.toString());
        if (this.mIsResume && this.mPasswordEntry.isEnabled()) {
            if (!z || !z2) {
                Log.i(str, "request IME show");
                this.mImm.showSoftInput(this.mPasswordEntry, 1);
            } else {
                Log.d(str, "not show IME when prevent mode");
            }
        }
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        StringBuilder sb = new StringBuilder();
        sb.append("onWindowFocusChanged: hasWindowFocus:");
        sb.append(z);
        Log.i("KeyguardPasswordView", sb.toString());
        this.mHasWindowFocus = z;
        if (isNeedToPendingIME()) {
            KeyguardUpdateMonitor.getInstance(getContext()).updateBiometricListeningState();
            return;
        }
        if (z) {
            TextView textView = this.mPasswordEntry;
            if (textView != null && textView.isEnabled()) {
                requestShowIME();
            }
        }
    }

    public void onScreenStateChanged(int i) {
        super.onScreenStateChanged(i);
        StringBuilder sb = new StringBuilder();
        sb.append("onScreenStateChanged: screenState:");
        sb.append(i);
        Log.i("KeyguardPasswordView", sb.toString());
        if (isNeedToPendingIME()) {
            KeyguardUpdateMonitor.getInstance(getContext()).updateBiometricListeningState();
            return;
        }
        if (i == 1) {
            requestShowIME();
        }
    }

    /* access modifiers changed from: protected */
    public int getPromptReasonStringRes(int i) {
        if (i == 0) {
            return 0;
        }
        if (i == 1) {
            return R$string.kg_prompt_reason_restart_password;
        }
        if (i == 2) {
            return R$string.kg_prompt_reason_timeout_password;
        }
        if (i == 3) {
            return R$string.kg_prompt_reason_device_admin;
        }
        if (i != 4) {
            return R$string.kg_prompt_reason_timeout_password;
        }
        return R$string.kg_prompt_reason_user_request;
    }

    public void onPause() {
        super.onPause();
        this.mIsResume = false;
        this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /* access modifiers changed from: private */
    public void updateSwitchImeButton() {
        boolean z = this.mSwitchImeButton.getVisibility() == 0;
        boolean hasMultipleEnabledIMEsOrSubtypes = hasMultipleEnabledIMEsOrSubtypes(this.mImm, false);
        if (z != hasMultipleEnabledIMEsOrSubtypes) {
            this.mSwitchImeButton.setVisibility(hasMultipleEnabledIMEsOrSubtypes ? 0 : 8);
        }
        if (this.mSwitchImeButton.getVisibility() != 0) {
            ViewGroup.LayoutParams layoutParams = this.mPasswordEntry.getLayoutParams();
            if (layoutParams instanceof MarginLayoutParams) {
                ((MarginLayoutParams) layoutParams).setMarginStart(0);
                this.mPasswordEntry.setLayoutParams(layoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mImm = (InputMethodManager) getContext().getSystemService("input_method");
        this.mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setTextOperationUser(UserHandle.of(KeyguardUpdateMonitor.getCurrentUser()));
        this.mPasswordEntryDisabler = new TextViewInputDisabler(this.mPasswordEntry);
        this.mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        this.mPasswordEntry.setInputType(129);
        this.mPasswordEntry.setOnEditorActionListener(this);
        this.mPasswordEntry.addTextChangedListener(this);
        this.mPasswordEntry.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                KeyguardPasswordView.this.mCallback.userActivity();
            }
        });
        boolean z = true;
        this.mPasswordEntry.setSelected(true);
        this.mSwitchImeButton = findViewById(R$id.switch_ime_button);
        this.mSwitchImeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                KeyguardPasswordView.this.mCallback.userActivity();
                KeyguardPasswordView keyguardPasswordView = KeyguardPasswordView.this;
                keyguardPasswordView.mImm.showInputMethodPickerFromSystem(false, keyguardPasswordView.getContext().getDisplayId());
            }
        });
        View findViewById = findViewById(R$id.cancel_button);
        if (findViewById != null) {
            findViewById.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    KeyguardPasswordView.this.lambda$onFinishInflate$0$KeyguardPasswordView(view);
                }
            });
        }
        updateSwitchImeButton();
        postDelayed(new Runnable() {
            public void run() {
                KeyguardPasswordView.this.updateSwitchImeButton();
            }
        }, 500);
        displayDefaultSecurityMessage();
        int navBarMode = this.mOverviewProxyService.getNavBarMode();
        if (!(System.getInt(getContext().getContentResolver(), "op_navigation_bar_type", 1) == 3 || System.getInt(getContext().getContentResolver(), "buttons_show_on_screen_navkeys", 0) == 0 || QuickStepContract.isGesturalMode(navBarMode))) {
            z = false;
        }
        this.mHideNavigationBar = z;
        this.mFingerprintIcon = findViewById(R$id.fingerprint_icon);
    }

    public /* synthetic */ void lambda$onFinishInflate$0$KeyguardPasswordView(View view) {
        this.mCallback.reset();
        this.mCallback.onCancelClicked();
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int i, Rect rect) {
        return this.mPasswordEntry.requestFocus(i, rect);
    }

    /* access modifiers changed from: protected */
    public void resetPasswordText(boolean z, boolean z2) {
        this.mPasswordEntry.setText("");
    }

    /* access modifiers changed from: protected */
    public byte[] getPasswordText() {
        return charSequenceToByteArray(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryInputEnabled(boolean z) {
        this.mPasswordEntryDisabler.setInputEnabled(z);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager inputMethodManager, boolean z) {
        boolean z2 = false;
        int i = 0;
        for (InputMethodInfo inputMethodInfo : inputMethodManager.getEnabledInputMethodListAsUser(KeyguardUpdateMonitor.getCurrentUser())) {
            if (i > 1) {
                return true;
            }
            List<InputMethodSubtype> enabledInputMethodSubtypeList = inputMethodManager.getEnabledInputMethodSubtypeList(inputMethodInfo, true);
            if (!enabledInputMethodSubtypeList.isEmpty()) {
                int i2 = 0;
                for (InputMethodSubtype isAuxiliary : enabledInputMethodSubtypeList) {
                    if (isAuxiliary.isAuxiliary()) {
                        i2++;
                    }
                }
                if (enabledInputMethodSubtypeList.size() - i2 <= 0) {
                    if (z) {
                        if (i2 <= 1) {
                        }
                    }
                }
            }
            i++;
        }
        if (i > 1 || inputMethodManager.getEnabledInputMethodSubtypeList(null, false).size() > 1) {
            z2 = true;
        }
        return z2;
    }

    public int getWrongPasswordStringId() {
        int failedUnlockAttempts = KeyguardUpdateMonitor.getInstance(getContext()).getFailedUnlockAttempts(KeyguardUpdateMonitor.getCurrentUser()) % 5;
        if (failedUnlockAttempts == 3) {
            return R$string.kg_wrong_pin_warning;
        }
        if (failedUnlockAttempts == 4) {
            return R$string.kg_wrong_pin_warning_one;
        }
        return R$string.kg_wrong_password;
    }

    public void startAppearAnimation() {
        setAlpha(0.0f);
        setTranslationY(0.0f);
        animate().alpha(1.0f).withLayer().setDuration(300).setInterpolator(this.mLinearOutSlowInInterpolator);
        View view = this.mFingerprintIcon;
        if (view != null && view.getVisibility() == 0) {
            AnimatorSet fadeInOutVisibilityAnimation = OpFadeAnim.getFadeInOutVisibilityAnimation(this.mFingerprintIcon, 0, null, true);
            if (fadeInOutVisibilityAnimation != null) {
                fadeInOutVisibilityAnimation.start();
            }
        }
    }

    public boolean startDisappearAnimation(Runnable runnable) {
        animate().alpha(0.0f).translationY((float) this.mDisappearYTranslation).setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100).withEndAction(runnable);
        return true;
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.userActivity();
        }
    }

    public void afterTextChanged(Editable editable) {
        if (!TextUtils.isEmpty(editable)) {
            onUserInput();
        }
    }

    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        boolean z = keyEvent == null && (i == 0 || i == 6 || i == 5);
        boolean z2 = keyEvent != null && KeyEvent.isConfirmKey(keyEvent.getKeyCode()) && keyEvent.getAction() == 0;
        if (!z && !z2) {
            return false;
        }
        verifyPasswordAndUnlock();
        return true;
    }

    public CharSequence getTitle() {
        return getContext().getString(17040178);
    }

    private static byte[] charSequenceToByteArray(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        byte[] bArr = new byte[charSequence.length()];
        for (int i = 0; i < charSequence.length(); i++) {
            bArr[i] = (byte) charSequence.charAt(i);
        }
        return bArr;
    }

    /* access modifiers changed from: private */
    public boolean isNeedToPendingIME() {
        if (!IS_CUSTOM_FINGERPRINT) {
            return false;
        }
        return KeyguardUpdateMonitor.getInstance(getContext()).isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean isCheckingPassword() {
        return super.isCheckingPassword();
    }
}
