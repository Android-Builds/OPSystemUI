package com.oneplus.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardSecurityView;
import com.oneplus.util.OpReflectionUtils;

public class OpKeyguardSecurityContainer extends FrameLayout {
    public OpKeyguardSecurityContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList, int i) {
        if (getCurrentSecuritySelection() != SecurityMode.None) {
            getSecurityView(getCurrentSecuritySelection()).showMessage(charSequence, colorStateList, i);
        }
    }

    private KeyguardSecurityView getSecurityView(SecurityMode securityMode) {
        return (KeyguardSecurityView) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardSecurityContainer.class, "getSecurityView", SecurityMode.class), securityMode);
    }

    private SecurityMode getCurrentSecuritySelection() {
        return (SecurityMode) OpReflectionUtils.getValue(KeyguardSecurityContainer.class, this, "mCurrentSecuritySelection");
    }
}
