package com.android.keyguard;

import android.content.res.ColorStateList;
import com.android.internal.widget.LockPatternUtils;

public interface KeyguardSecurityView {
    CharSequence getTitle();

    boolean isCheckingPassword();

    boolean needsInput();

    void onPause();

    void onResume(int i);

    void reset();

    void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback);

    void setLockPatternUtils(LockPatternUtils lockPatternUtils);

    void showMessage(CharSequence charSequence, ColorStateList colorStateList);

    void showMessage(CharSequence charSequence, ColorStateList colorStateList, int i) {
    }

    void showPromptReason(int i);

    void startAppearAnimation();

    boolean startDisappearAnimation(Runnable runnable);
}
