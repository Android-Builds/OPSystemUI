package com.android.systemui.biometrics;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SystemUIApplication;
import com.oneplus.systemui.biometrics.OpBiometricDialogImpl;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;

public class FingerprintDialogView extends BiometricDialogView {
    private boolean mFodShowing;

    private class Callback implements DialogViewCallback {
        private DialogViewCallback mDialogViewCallback;

        public Callback(DialogViewCallback dialogViewCallback) {
            this.mDialogViewCallback = dialogViewCallback;
        }

        public void onUserCanceled() {
            FingerprintDialogView.this.hideFodImmediately();
            this.mDialogViewCallback.onUserCanceled();
        }

        public void onErrorShown() {
            FingerprintDialogView.this.hideFodImmediately();
            this.mDialogViewCallback.onErrorShown();
        }

        public void onNegativePressed() {
            FingerprintDialogView.this.hideFodImmediately();
            this.mDialogViewCallback.onNegativePressed();
        }

        public void onPositivePressed() {
            this.mDialogViewCallback.onPositivePressed();
        }

        public void onTryAgainPressed() {
            this.mDialogViewCallback.onTryAgainPressed();
        }
    }

    /* access modifiers changed from: protected */
    public int getAuthenticatedAccessibilityResourceId() {
        return 17040028;
    }

    /* access modifiers changed from: protected */
    public int getDelayAfterAuthenticatedDurationMs() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnimateForTransition(int i, int i2) {
        if (i2 == 2) {
            return true;
        }
        if (i == 2 && i2 == 1) {
            return true;
        }
        if (i == 1 && i2 == 4) {
            return false;
        }
        if (!(i == 2 && i2 == 4) && i2 == 1) {
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean shouldGrayAreaDismissDialog() {
        return true;
    }

    public FingerprintDialogView(Context context, DialogViewCallback dialogViewCallback, boolean z) {
        super(context, dialogViewCallback, z);
    }

    /* access modifiers changed from: protected */
    public void handleResetMessage() {
        updateState(1);
        this.mErrorText.setText(getHintStringResourceId());
        this.mErrorText.setTextColor(this.mTextColor);
    }

    /* access modifiers changed from: protected */
    public int getHintStringResourceId() {
        return R$string.fingerprint_dialog_touch_sensor;
    }

    /* access modifiers changed from: protected */
    public int getIconDescriptionResourceId() {
        return R$string.accessibility_fingerprint_dialog_fingerprint_icon;
    }

    /* access modifiers changed from: protected */
    public void updateIcon(int i, int i2) {
        String str = "FingerprintDialogView";
        if (OpUtils.isCustomFingerprint()) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateIcon ");
            sb.append(i);
            String str2 = " ";
            sb.append(str2);
            sb.append(i2);
            sb.append(str2);
            sb.append(this.mFodShowing);
            Log.d(str, sb.toString());
            boolean z = this.mBiometricIcon.getVisibility() == 0;
            if (i2 == 2 && i != i2) {
                this.mBiometricIcon.setImageResource(R$drawable.fp_icon_default_disable);
                this.mBiometricIcon.setColorFilter(getResources().getColor(R$color.biometric_fingerprint_error_color), Mode.MULTIPLY);
            }
            if (!this.mFodShowing && !z) {
                this.mBiometricIcon.setVisibility(0);
            } else if (this.mFodShowing && z) {
                this.mBiometricIcon.setVisibility(4);
            }
            return;
        }
        Drawable animationForTransition = getAnimationForTransition(i, i2);
        if (animationForTransition == null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Animation not found, ");
            sb2.append(i);
            sb2.append(" -> ");
            sb2.append(i2);
            Log.e(str, sb2.toString());
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable = animationForTransition instanceof AnimatedVectorDrawable ? (AnimatedVectorDrawable) animationForTransition : null;
        opUpdateAccentColor(animatedVectorDrawable);
        this.mBiometricIcon.setImageDrawable(animationForTransition);
        if (animatedVectorDrawable != null && shouldAnimateForTransition(i, i2)) {
            animatedVectorDrawable.forceAnimationOnUI();
            animatedVectorDrawable.start();
        }
    }

    /* access modifiers changed from: protected */
    public Drawable getAnimationForTransition(int i, int i2) {
        int i3;
        if (i2 == 2) {
            i3 = R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i == 2 && i2 == 1) {
            i3 = R$drawable.fingerprint_dialog_error_to_fp;
        } else if (i == 1 && i2 == 4) {
            i3 = R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i == 2 && i2 == 4) {
            i3 = R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i2 != 1) {
            return null;
        } else {
            i3 = R$drawable.fingerprint_dialog_fp_to_error;
        }
        return this.mContext.getDrawable(i3);
    }

    private void opUpdateAccentColor(AnimatedVectorDrawable animatedVectorDrawable) {
        if (animatedVectorDrawable != null) {
            int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
            String[] strArr = {"_R_G_L_1_G_D_0_P_0", "_R_G_L_1_G_D_1_P_0", "_R_G_L_1_G_D_2_P_0", "_R_G_L_1_G_D_3_P_0", "_R_G_L_1_G_D_4_P_0"};
            for (String changePathStrokeColor : strArr) {
                animatedVectorDrawable.changePathStrokeColor(changePathStrokeColor, color);
            }
        }
    }

    /* access modifiers changed from: protected */
    public DialogViewCallback needWrap(DialogViewCallback dialogViewCallback) {
        if (OpUtils.isCustomFingerprint()) {
            return new Callback(dialogViewCallback);
        }
        super.needWrap(dialogViewCallback);
        return dialogViewCallback;
    }

    /* access modifiers changed from: protected */
    public void onBiometricPromptReady() {
        if (OpUtils.isCustomFingerprint()) {
            int i = this.mBundle.getInt("key_cookie", 0);
            if (i == 0) {
                Log.d("FingerprintDialogView", "onBiometricPromptReady: cookie must not be zero.");
                return;
            }
            getFodDialogImpl().onBiometricPromptReady(i);
            this.mFodShowing = true;
            updateState(1);
        }
    }

    private OpBiometricDialogImpl getFodDialogImpl() {
        return (OpBiometricDialogImpl) ((SystemUIApplication) getContext().getApplicationContext()).getComponent(OpBiometricDialogImpl.class);
    }

    /* access modifiers changed from: private */
    public void hideFodImmediately() {
        getFodDialogImpl().hideFodImmediately();
        this.mFodShowing = false;
        updateState(getState());
    }
}
