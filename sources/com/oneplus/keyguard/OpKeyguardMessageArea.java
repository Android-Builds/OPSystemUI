package com.oneplus.keyguard;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import com.android.keyguard.KeyguardMessageArea;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R$anim;
import com.android.settingslib.Utils;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.oneolus.anim.OpFadeAnim;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpKeyguardMessageArea extends TextView {
    boolean mAnimWasCanceled = false;
    private AnimatorSet mFadeAnimator;
    int mLastTargetVisible;
    protected int mMessageType;

    public OpKeyguardMessageArea(Context context, AttributeSet attributeSet) {
        super(context, null);
    }

    public OpKeyguardMessageArea(Context context, AttributeSet attributeSet, KeyguardUpdateMonitor keyguardUpdateMonitor, ConfigurationController configurationController) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void setTextWithAnim(final CharSequence charSequence, boolean z) {
        final int i = 4;
        if (charSequence != null && !TextUtils.isEmpty(charSequence) && !charSequence.toString().equals(" ") && !charSequence.toString().equals("  ")) {
            i = 0;
            setText(charSequence);
            setVisibility(0);
        }
        String str = "OpKeyguardMessageArea";
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("setTextWithAnim / targetVisible:");
            sb.append(i);
            sb.append("/ Last:");
            sb.append(this.mLastTargetVisible);
            sb.append("/ status:");
            sb.append(charSequence);
            sb.append("/ getAlpha:");
            sb.append(getAlpha());
            sb.append(", bouncerVisible:");
            sb.append(z);
            Log.i(str, sb.toString());
        }
        if (i == this.mLastTargetVisible) {
            this.mLastTargetVisible = i;
            return;
        }
        this.mLastTargetVisible = i;
        AnimatorSet animatorSet = this.mFadeAnimator;
        if (animatorSet != null && animatorSet.isRunning()) {
            if (OpUtils.DEBUG_ONEPLUS) {
                Log.i(str, "canceled");
            }
            this.mAnimWasCanceled = true;
            this.mFadeAnimator.cancel();
        }
        this.mFadeAnimator = OpFadeAnim.getFadeInOutVisibilityAnimation(this, i, Float.valueOf(getAlpha()), true);
        AnimatorSet animatorSet2 = this.mFadeAnimator;
        if (animatorSet2 != null) {
            animatorSet2.removeAllListeners();
            this.mFadeAnimator.addListener(new AnimatorListener() {
                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationStart(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    if (OpUtils.DEBUG_ONEPLUS) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("onAnimationEnd, targetVisible:");
                        sb.append(i);
                        sb.append(" ,status:");
                        sb.append(charSequence);
                        sb.append(" / mAnimWasCanceled:");
                        sb.append(OpKeyguardMessageArea.this.mAnimWasCanceled);
                        Log.i("OpKeyguardMessageArea", sb.toString());
                    }
                    OpKeyguardMessageArea opKeyguardMessageArea = OpKeyguardMessageArea.this;
                    if (!opKeyguardMessageArea.mAnimWasCanceled) {
                        opKeyguardMessageArea.setAlpha(i == 0 ? 1.0f : 0.0f);
                        OpKeyguardMessageArea.this.mAnimWasCanceled = false;
                    }
                    int i = i;
                    if (i != 0) {
                        OpKeyguardMessageArea.this.setVisibility(i);
                    }
                }
            });
            this.mFadeAnimator.start();
        }
    }

    /* access modifiers changed from: protected */
    public void animateErrorText(TextView textView) {
        Animator loadAnimator = AnimatorInflater.loadAnimator(this.mContext, R$anim.oneplus_control_text_error_message_anim);
        if (loadAnimator != null) {
            loadAnimator.cancel();
            loadAnimator.setTarget(textView);
            loadAnimator.start();
        }
    }

    public void setMessage(CharSequence charSequence, int i) {
        this.mMessageType = i;
        setMessage(charSequence);
    }

    public void setMessage(int i, int i2) {
        this.mMessageType = i2;
        setMessage(i);
    }

    public static int getOpMsgType(Context context, ColorStateList colorStateList) {
        return (colorStateList.getDefaultColor() == -65536 || colorStateList.getDefaultColor() == Utils.getColorErrorDefaultColor(context)) ? 1 : 0;
    }

    private void setMessage(CharSequence charSequence) {
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardMessageArea.class, "setMessage", CharSequence.class), charSequence);
    }

    private void setMessage(int i) {
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(KeyguardMessageArea.class, "setMessage", Integer.TYPE), Integer.valueOf(i));
    }
}
