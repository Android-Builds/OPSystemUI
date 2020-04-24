package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;

public final class PhoneStatusBarTransitions extends BarTransitions {
    private View mBattery;
    private Animator mCurrentAnimation;
    private final float mIconAlphaWhenOpaque = this.mView.getContext().getResources().getFraction(R$dimen.status_bar_icon_drawing_alpha, 1, 1);
    private View mLeftSide;
    private View mStatusIcons;
    private final PhoneStatusBarView mView;

    private boolean isOpaque(int i) {
        return (i == 1 || i == 2 || i == 4 || i == 6) ? false : true;
    }

    public PhoneStatusBarTransitions(PhoneStatusBarView phoneStatusBarView) {
        super(phoneStatusBarView, R$drawable.status_background);
        this.mView = phoneStatusBarView;
    }

    public void init() {
        this.mLeftSide = this.mView.findViewById(R$id.status_bar_left_side);
        this.mStatusIcons = this.mView.findViewById(R$id.statusIcons);
        this.mBattery = this.mView.findViewById(R$id.battery);
        applyModeBackground(-1, getMode(), false);
        applyMode(getMode(), false);
    }

    public ObjectAnimator animateTransitionTo(View view, float f) {
        return ObjectAnimator.ofFloat(view, "alpha", new float[]{view.getAlpha(), f});
    }

    private float getNonBatteryClockAlphaFor(int i) {
        if (isLightsOut(i)) {
            return 0.0f;
        }
        if (!isOpaque(i)) {
            return 1.0f;
        }
        return this.mIconAlphaWhenOpaque;
    }

    private float getBatteryClockAlpha(int i) {
        if (isLightsOut(i)) {
            return 0.5f;
        }
        return getNonBatteryClockAlphaFor(i);
    }

    /* access modifiers changed from: protected */
    public void onTransition(int i, int i2, boolean z) {
        super.onTransition(i, i2, z);
        applyMode(i2, z);
    }

    private void applyMode(int i, boolean z) {
        if (this.mLeftSide != null) {
            float nonBatteryClockAlphaFor = getNonBatteryClockAlphaFor(i);
            float batteryClockAlpha = getBatteryClockAlpha(i);
            Animator animator = this.mCurrentAnimation;
            if (animator != null) {
                animator.cancel();
            }
            if (z) {
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(new Animator[]{animateTransitionTo(this.mLeftSide, nonBatteryClockAlphaFor), animateTransitionTo(this.mStatusIcons, nonBatteryClockAlphaFor), animateTransitionTo(this.mBattery, batteryClockAlpha)});
                if (isLightsOut(i)) {
                    animatorSet.setDuration(500);
                }
                animatorSet.start();
                this.mCurrentAnimation = animatorSet;
            } else {
                this.mLeftSide.setAlpha(nonBatteryClockAlphaFor);
                this.mStatusIcons.setAlpha(nonBatteryClockAlphaFor);
                this.mBattery.setAlpha(batteryClockAlpha);
            }
        }
    }
}
