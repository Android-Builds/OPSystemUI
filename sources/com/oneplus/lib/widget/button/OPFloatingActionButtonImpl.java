package com.oneplus.lib.widget.button;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.oneplus.commonctrl.R$color;

class OPFloatingActionButtonImpl {
    static final int[] EMPTY_STATE_SET = new int[0];
    static final int[] FOCUSED_ENABLED_STATE_SET = {16842908, 16842910};
    static final int[] PRESSED_ENABLED_STATE_SET = {16842919, 16842910};
    private Drawable mBorderDrawable;
    private Interpolator mInterpolator;
    /* access modifiers changed from: private */
    public boolean mIsHiding;
    private RippleDrawable mRippleDrawable;
    final OPShadowViewDelegate mShadowViewDelegate;
    private Drawable mShapeDrawable;
    final OPFloatingActionButton mView;

    /* access modifiers changed from: 0000 */
    public void jumpDrawableToCurrentState() {
    }

    /* access modifiers changed from: 0000 */
    public void onDrawableStateChanged(int[] iArr) {
    }

    OPFloatingActionButtonImpl(OPFloatingActionButton oPFloatingActionButton, OPShadowViewDelegate oPShadowViewDelegate) {
        this.mView = oPFloatingActionButton;
        this.mShadowViewDelegate = oPShadowViewDelegate;
        if (!oPFloatingActionButton.isInEditMode()) {
            this.mInterpolator = AnimationUtils.loadInterpolator(this.mView.getContext(), 17563661);
        }
    }

    /* access modifiers changed from: 0000 */
    public Drawable createBorderDrawable(int i, ColorStateList colorStateList) {
        Resources resources = this.mView.getResources();
        OPCircularBorderDrawable oPCircularBorderDrawable = new OPCircularBorderDrawable();
        oPCircularBorderDrawable.setGradientColors(resources.getColor(R$color.design_fab_stroke_top_outer_color), resources.getColor(R$color.design_fab_stroke_top_inner_color), resources.getColor(R$color.design_fab_stroke_end_inner_color), resources.getColor(R$color.design_fab_stroke_end_outer_color));
        oPCircularBorderDrawable.setBorderWidth((float) i);
        oPCircularBorderDrawable.setTintColor(colorStateList.getDefaultColor());
        return oPCircularBorderDrawable;
    }

    /* access modifiers changed from: 0000 */
    public void setBackground(Drawable drawable, ColorStateList colorStateList, Mode mode, int i, int i2) {
        Drawable drawable2;
        this.mShapeDrawable = drawable.mutate();
        this.mShapeDrawable.setTintList(colorStateList);
        if (mode != null) {
            this.mShapeDrawable.setTintMode(mode);
        }
        if (i2 > 0) {
            this.mBorderDrawable = createBorderDrawable(i2, colorStateList);
            drawable2 = new LayerDrawable(new Drawable[]{this.mBorderDrawable, this.mShapeDrawable});
        } else {
            this.mBorderDrawable = null;
            drawable2 = this.mShapeDrawable;
        }
        this.mRippleDrawable = new RippleDrawable(ColorStateList.valueOf(i), drawable2, null);
        this.mShadowViewDelegate.setBackground(this.mRippleDrawable);
        this.mShadowViewDelegate.setShadowPadding(0, 0, 0, 0);
    }

    /* access modifiers changed from: 0000 */
    public void setBackgroundTintList(ColorStateList colorStateList) {
        this.mShapeDrawable.setTintList(colorStateList);
        Drawable drawable = this.mBorderDrawable;
        if (drawable != null) {
            drawable.setTintList(colorStateList);
        }
    }

    /* access modifiers changed from: 0000 */
    public void setBackgroundTintMode(Mode mode) {
        this.mShapeDrawable.setTintMode(mode);
    }

    public void setElevation(float f) {
        this.mView.setElevation(f);
    }

    /* access modifiers changed from: 0000 */
    public void setPressedTranslationZ(float f) {
        StateListAnimator stateListAnimator = new StateListAnimator();
        int[] iArr = PRESSED_ENABLED_STATE_SET;
        String str = "translationZ";
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mView, str, new float[]{f});
        setupAnimator(ofFloat);
        stateListAnimator.addState(iArr, ofFloat);
        int[] iArr2 = FOCUSED_ENABLED_STATE_SET;
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mView, str, new float[]{f});
        setupAnimator(ofFloat2);
        stateListAnimator.addState(iArr2, ofFloat2);
        int[] iArr3 = EMPTY_STATE_SET;
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(this.mView, str, new float[]{0.0f});
        setupAnimator(ofFloat3);
        stateListAnimator.addState(iArr3, ofFloat3);
        this.mView.setStateListAnimator(stateListAnimator);
    }

    private Animator setupAnimator(Animator animator) {
        animator.setInterpolator(this.mInterpolator);
        return animator;
    }

    /* access modifiers changed from: 0000 */
    public void hide(final boolean z) {
        if (!this.mIsHiding && this.mView.getVisibility() == 0) {
            if (!this.mView.isLaidOut() || this.mView.isInEditMode()) {
                this.mView.internalSetVisibility(8, z);
            } else {
                this.mView.animate().scaleX(0.0f).scaleY(0.0f).alpha(0.0f).setDuration(200).setInterpolator(new FastOutSlowInInterpolator()).setListener(new AnimatorListener() {
                    public void onAnimationRepeat(Animator animator) {
                    }

                    public void onAnimationStart(Animator animator) {
                        OPFloatingActionButtonImpl.this.mIsHiding = true;
                        OPFloatingActionButtonImpl.this.mView.internalSetVisibility(0, z);
                    }

                    public void onAnimationEnd(Animator animator) {
                        OPFloatingActionButtonImpl.this.mIsHiding = false;
                        OPFloatingActionButtonImpl.this.mView.internalSetVisibility(8, z);
                    }

                    public void onAnimationCancel(Animator animator) {
                        OPFloatingActionButtonImpl.this.mIsHiding = false;
                    }
                });
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void show(final boolean z) {
        if (this.mView.getVisibility() == 0) {
            return;
        }
        if (!this.mView.isLaidOut() || this.mView.isInEditMode()) {
            this.mView.internalSetVisibility(0, z);
            this.mView.setAlpha(1.0f);
            this.mView.setScaleY(1.0f);
            this.mView.setScaleX(1.0f);
            return;
        }
        this.mView.setAlpha(0.0f);
        this.mView.setScaleY(0.0f);
        this.mView.setScaleX(0.0f);
        this.mView.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(200).setInterpolator(new FastOutSlowInInterpolator()).setListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                OPFloatingActionButtonImpl.this.mView.internalSetVisibility(0, z);
            }
        });
    }
}
