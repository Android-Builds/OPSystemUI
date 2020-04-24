package com.oneplus.aod;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$anim;
import com.android.systemui.R$string;

public class OpFpAodIndicationText extends TextView {
    private OpAodDisplayViewManager mAodDisplayViewManager;
    /* access modifiers changed from: private */
    public Context mContext;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public View mIndication;
    private LockPatternUtils mLockPatternUtils;
    private SecurityMode mSecurityMode = SecurityMode.None;
    private KeyguardUpdateMonitor mUpdateMonitor;

    public OpFpAodIndicationText(Context context) {
        super(context);
        this.mContext = context;
    }

    public OpFpAodIndicationText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public OpFpAodIndicationText(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    public void init(OpAodDisplayViewManager opAodDisplayViewManager, Handler handler) {
        this.mAodDisplayViewManager = opAodDisplayViewManager;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mHandler = handler;
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mIndication = this;
    }

    public void updateFPIndicationText(boolean z, String str) {
        int i;
        if (this.mAodDisplayViewManager != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateFPIndicationText: lockout:");
            sb.append(this.mUpdateMonitor.isFingerprintLockout());
            String str2 = ", ";
            sb.append(str2);
            sb.append(z);
            sb.append(str2);
            sb.append(str);
            String str3 = "OpFpAodIndicationText";
            Log.d(str3, sb.toString());
            if (this.mUpdateMonitor.isFingerprintLockout()) {
                setAodIndicationText(17040033);
                return;
            }
            int i2 = 0;
            if (z) {
                i = 1;
                setAodIndicationText(str);
                animateErrorText();
            } else {
                this.mSecurityMode = getSecurityMode();
                SecurityMode securityMode = this.mSecurityMode;
                if (securityMode == SecurityMode.Pattern) {
                    i2 = R$string.op_kg_prompt_reason_timeout_pattern;
                } else if (securityMode == SecurityMode.Password) {
                    i2 = R$string.op_kg_prompt_reason_timeout_password;
                } else if (securityMode == SecurityMode.PIN) {
                    i2 = R$string.op_kg_prompt_reason_timeout_pin;
                }
                i = i2;
                if (i != 0 && !this.mUpdateMonitor.isUnlockingWithBiometricAllowed()) {
                    setAodIndicationText(i);
                    animateErrorText();
                }
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("updateFPIndicationText: ");
            sb2.append(this.mSecurityMode);
            sb2.append(str2);
            sb2.append(i);
            Log.d(str3, sb2.toString());
            if (i == 0) {
                setAodIndicationText("");
            }
        }
    }

    private SecurityMode getSecurityMode() {
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(KeyguardUpdateMonitor.getCurrentUser());
        if (keyguardStoredPasswordQuality == 0) {
            return SecurityMode.None;
        }
        if (keyguardStoredPasswordQuality == 65536) {
            return SecurityMode.Pattern;
        }
        if (keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608) {
            return SecurityMode.PIN;
        }
        if (keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288) {
            return SecurityMode.Password;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unknown security quality:");
        sb.append(keyguardStoredPasswordQuality);
        throw new IllegalStateException(sb.toString());
    }

    private void setAodIndicationText(int i) {
        setAodIndicationText(this.mContext.getString(i));
    }

    private void setAodIndicationText(final String str) {
        this.mHandler.post(new Runnable() {
            public void run() {
                OpFpAodIndicationText.this.setText(str);
            }
        });
    }

    public void resetState() {
        if (!this.mUpdateMonitor.isFingerprintLockout() && this.mUpdateMonitor.isUnlockingWithBiometricAllowed()) {
            setAodIndicationText("");
        }
    }

    public void animateErrorText() {
        this.mHandler.post(new Runnable() {
            public void run() {
                Animator loadAnimator = AnimatorInflater.loadAnimator(OpFpAodIndicationText.this.mContext, R$anim.oneplus_control_text_error_message_anim);
                if (loadAnimator != null) {
                    loadAnimator.cancel();
                    loadAnimator.setTarget(OpFpAodIndicationText.this.mIndication);
                    loadAnimator.start();
                }
            }
        });
    }
}
