package com.android.keyguard;

import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.oneolus.anim.OpFadeAnim;
import com.oneplus.keyguard.OpKeyguardPinBasedInputViewForPin;

public class KeyguardPINView extends OpKeyguardPinBasedInputViewForPin {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private ViewGroup mContainer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    private int mDisappearYTranslation;
    private View mFingerprintIcon;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private View[][] mViews;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context);
        Context context2 = context;
        DisappearAnimationUtils disappearAnimationUtils = new DisappearAnimationUtils(context2, 125, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtils = disappearAnimationUtils;
        DisappearAnimationUtils disappearAnimationUtils2 = new DisappearAnimationUtils(context2, 187, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, 17563663));
        this.mDisappearAnimationUtilsLocked = disappearAnimationUtils2;
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        super.resetState();
        displayDefaultSecurityMessage();
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R$id.pinEntry;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContainer = (ViewGroup) findViewById(R$id.container);
        this.mRow0 = (ViewGroup) findViewById(R$id.row0);
        this.mRow1 = (ViewGroup) findViewById(R$id.row1);
        this.mRow2 = (ViewGroup) findViewById(R$id.row2);
        this.mRow3 = (ViewGroup) findViewById(R$id.row3);
        this.mViews = new View[][]{new View[]{null, findViewById(R$id.keyguard_message_area), null}, new View[]{this.mRow0, null, null}, new View[]{findViewById(R$id.key1), findViewById(R$id.key2), findViewById(R$id.key3)}, new View[]{findViewById(R$id.key4), findViewById(R$id.key5), findViewById(R$id.key6)}, new View[]{findViewById(R$id.key7), findViewById(R$id.key8), findViewById(R$id.key9)}, new View[]{findViewById(R$id.deleteOrCancel), findViewById(R$id.key0), findViewById(R$id.key_enter)}, new View[]{null, this.mEcaView, null}};
        View findViewById = findViewById(R$id.cancel_button);
        if (findViewById != null) {
            findViewById.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    KeyguardPINView.this.lambda$onFinishInflate$0$KeyguardPINView(view);
                }
            });
        }
        this.mFingerprintIcon = findViewById(R$id.fingerprint_icon);
        displayDefaultSecurityMessage();
    }

    public /* synthetic */ void lambda$onFinishInflate$0$KeyguardPINView(View view) {
        this.mCallback.reset();
        this.mCallback.onCancelClicked();
    }

    private void displayDefaultSecurityMessage() {
        int i = this.mKeyguardUpdateMonitor.isFirstUnlock() ? R$string.kg_first_unlock_instructions : R$string.kg_pin_instructions;
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage((CharSequence) getMessageWithCount(i));
        }
    }

    public int getWrongPasswordStringId() {
        int failedUnlockAttempts = KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts(KeyguardUpdateMonitor.getCurrentUser()) % 5;
        if (failedUnlockAttempts == 3) {
            return R$string.kg_wrong_pin_warning;
        }
        if (failedUnlockAttempts == 4) {
            return R$string.kg_wrong_pin_warning_one;
        }
        return R$string.kg_wrong_pin;
    }

    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            public void run() {
                KeyguardPINView.this.enableClipping(true);
            }
        });
        View view = this.mFingerprintIcon;
        if (view != null && view.getVisibility() == 0) {
            AnimatorSet fadeInOutVisibilityAnimation = OpFadeAnim.getFadeInOutVisibilityAnimation(this.mFingerprintIcon, 0, null, true);
            if (fadeInOutVisibilityAnimation != null) {
                fadeInOutVisibilityAnimation.start();
            }
        }
    }

    public boolean startDisappearAnimation(final Runnable runnable) {
        DisappearAnimationUtils disappearAnimationUtils;
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 500, (float) this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            public void run() {
                KeyguardPINView.this.enableClipping(true);
                Runnable runnable = runnable;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        if (this.mFingerprintIcon.getVisibility() == 0) {
            DisappearAnimationUtils disappearAnimationUtils2 = this.mDisappearAnimationUtils;
            disappearAnimationUtils2.createAnimation(this.mFingerprintIcon, 0, 200, 3.0f * (-disappearAnimationUtils2.getStartTranslation()), false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void enableClipping(boolean z) {
        this.mContainer.setClipToPadding(z);
        this.mContainer.setClipChildren(z);
        this.mRow1.setClipToPadding(z);
        this.mRow2.setClipToPadding(z);
        this.mRow3.setClipToPadding(z);
        setClipChildren(z);
    }
}
