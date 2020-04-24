package com.oneplus.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;

public class OpNavigationHandle extends View {
    private AnimDirection mAnimDirection = AnimDirection.NONE;
    private int mDownX;
    private int mDownY;
    private float mOriginTranX;
    private float mOriginTranY;
    private Animator mStartAnim = null;
    private boolean mStartAnimPlayed = false;
    private float mStartAnimTargetTranX = 0.0f;
    private int mTranslationLimit;
    private int mTranslationRestore;
    private int mTranslationReverse;

    /* renamed from: com.oneplus.systemui.statusbar.phone.OpNavigationHandle$3 */
    static /* synthetic */ class C20903 {

        /* renamed from: $SwitchMap$com$oneplus$systemui$statusbar$phone$OpNavigationHandle$AnimDirection */
        static final /* synthetic */ int[] f121x4048429a = new int[AnimDirection.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                com.oneplus.systemui.statusbar.phone.OpNavigationHandle$AnimDirection[] r0 = com.oneplus.systemui.statusbar.phone.OpNavigationHandle.AnimDirection.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                f121x4048429a = r0
                int[] r0 = f121x4048429a     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.oneplus.systemui.statusbar.phone.OpNavigationHandle$AnimDirection r1 = com.oneplus.systemui.statusbar.phone.OpNavigationHandle.AnimDirection.LEFT     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = f121x4048429a     // Catch:{ NoSuchFieldError -> 0x001f }
                com.oneplus.systemui.statusbar.phone.OpNavigationHandle$AnimDirection r1 = com.oneplus.systemui.statusbar.phone.OpNavigationHandle.AnimDirection.RIGHT     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = f121x4048429a     // Catch:{ NoSuchFieldError -> 0x002a }
                com.oneplus.systemui.statusbar.phone.OpNavigationHandle$AnimDirection r1 = com.oneplus.systemui.statusbar.phone.OpNavigationHandle.AnimDirection.VERTICAL     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.statusbar.phone.OpNavigationHandle.C20903.<clinit>():void");
        }
    }

    public enum AnimDirection {
        NONE,
        VERTICAL,
        LEFT,
        RIGHT
    }

    public OpNavigationHandle(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Resources resources = context.getResources();
        this.mTranslationLimit = resources.getDimensionPixelSize(R$dimen.op_nav_home_handle_translation_limit);
        this.mTranslationReverse = resources.getDimensionPixelSize(R$dimen.op_nav_home_handle_anim_reverse);
        this.mTranslationRestore = resources.getDimensionPixelSize(R$dimen.op_nav_home_handle_anim_restore);
        this.mOriginTranX = getTranslationX();
        this.mOriginTranY = getTranslationY();
    }

    public void handleTouch(MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 0) {
            if (actionMasked != 1) {
                if (actionMasked == 2) {
                    int abs = Math.abs(x - this.mDownX);
                    int abs2 = Math.abs(y - this.mDownY);
                    if (this.mStartAnimPlayed) {
                        return;
                    }
                    if (abs >= 30 || abs2 >= 30) {
                        AnimDirection animDirection = this.mAnimDirection;
                        AnimDirection animDirection2 = AnimDirection.NONE;
                        if (animDirection == animDirection2) {
                            if (abs > abs2) {
                                this.mAnimDirection = x - this.mDownX < 0 ? AnimDirection.LEFT : AnimDirection.RIGHT;
                            } else if (abs < abs2) {
                                if (y - this.mDownY <= 0) {
                                    animDirection2 = AnimDirection.VERTICAL;
                                }
                                this.mAnimDirection = animDirection2;
                            }
                        }
                        doStartAnim();
                        return;
                    }
                    return;
                } else if (actionMasked != 3) {
                    return;
                }
            }
            if (this.mAnimDirection == AnimDirection.NONE) {
                reset();
            } else {
                doEndAnim();
            }
        } else {
            this.mAnimDirection = AnimDirection.NONE;
            this.mDownX = x;
            this.mDownY = y;
        }
    }

    private void doStartAnim() {
        int i = C20903.f121x4048429a[this.mAnimDirection.ordinal()];
        if (i == 1 || i == 2) {
            this.mStartAnimTargetTranX = this.mOriginTranX + ((float) (this.mAnimDirection == AnimDirection.LEFT ? 0 - this.mTranslationLimit : this.mTranslationLimit));
            this.mStartAnim = ObjectAnimator.ofFloat(this, "translationX", new float[]{this.mOriginTranX, this.mStartAnimTargetTranX});
        } else if (i == 3) {
            float f = this.mOriginTranY;
            this.mStartAnim = ObjectAnimator.ofFloat(this, "translationY", new float[]{f, f - ((float) this.mTranslationLimit)});
        }
        Animator animator = this.mStartAnim;
        if (animator != null) {
            this.mStartAnimPlayed = true;
            animator.setDuration(150);
            this.mStartAnim.setInterpolator(Interpolators.CUSTOM_40_40);
            this.mStartAnim.start();
        }
    }

    private void doEndAnim() {
        Animator animator;
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(150);
        int i = C20903.f121x4048429a[this.mAnimDirection.ordinal()];
        ObjectAnimator objectAnimator = null;
        if (i == 1 || i == 2) {
            int i2 = this.mAnimDirection == AnimDirection.LEFT ? this.mTranslationReverse : 0 - this.mTranslationReverse;
            int i3 = this.mAnimDirection == AnimDirection.LEFT ? this.mTranslationRestore : 0 - this.mTranslationRestore;
            float f = this.mStartAnimTargetTranX;
            float[] fArr = {f, f + ((float) i2)};
            String str = "translationX";
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, str, fArr);
            objectAnimator = ObjectAnimator.ofFloat(this, str, new float[]{(float) (getLeft() + i3), (float) getLeft()});
            animator = ofFloat;
        } else if (i != 3) {
            animator = null;
        } else {
            float f2 = this.mOriginTranY;
            int i4 = this.mTranslationLimit;
            float[] fArr2 = {f2 - ((float) i4), (f2 - ((float) i4)) + ((float) this.mTranslationReverse)};
            String str2 = "translationY";
            animator = ObjectAnimator.ofFloat(this, str2, fArr2);
            objectAnimator = ObjectAnimator.ofFloat(this, str2, new float[]{(float) (getTop() + this.mTranslationRestore), (float) getTop()});
        }
        if (animator != null && objectAnimator != null) {
            animatorSet.play(animator).before(objectAnimator);
            animatorSet.setInterpolator(Interpolators.CUSTOM_40_40);
            animatorSet.addListener(new AnimatorListener() {
                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationStart(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    OpNavigationHandle.this.reset();
                }

                public void onAnimationCancel(Animator animator) {
                    OpNavigationHandle.this.reset();
                }
            });
            if (this.mStartAnim.isRunning()) {
                this.mStartAnim.addListener(new AnimatorListener() {
                    public void onAnimationCancel(Animator animator) {
                    }

                    public void onAnimationRepeat(Animator animator) {
                    }

                    public void onAnimationStart(Animator animator) {
                    }

                    public void onAnimationEnd(Animator animator) {
                        animatorSet.start();
                    }
                });
            } else {
                animatorSet.start();
            }
        }
    }

    /* access modifiers changed from: private */
    public void reset() {
        this.mAnimDirection = AnimDirection.NONE;
        this.mStartAnimPlayed = false;
        this.mStartAnim = null;
        this.mStartAnimTargetTranX = 0.0f;
    }
}
