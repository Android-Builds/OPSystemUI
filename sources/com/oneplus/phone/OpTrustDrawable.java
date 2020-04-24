package com.oneplus.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.Interpolator;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;

public class OpTrustDrawable extends Drawable {
    private int mAlpha;
    private final AnimatorUpdateListener mAlphaUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            OpTrustDrawable.this.mCurAlpha = ((Integer) valueAnimator.getAnimatedValue()).intValue();
            OpTrustDrawable.this.invalidateSelf();
        }
    };
    private boolean mAnimating;
    /* access modifiers changed from: private */
    public int mCurAlpha;
    private Animator mCurAnimator;
    /* access modifiers changed from: private */
    public float mCurInnerRadius;
    private final float mInnerRadiusEnter;
    private final float mInnerRadiusExit;
    private final float mInnerRadiusVisibleMax;
    private final float mInnerRadiusVisibleMin;
    private Paint mPaint;
    private final AnimatorUpdateListener mRadiusUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            OpTrustDrawable.this.mCurInnerRadius = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            OpTrustDrawable.this.invalidateSelf();
        }
    };
    private int mState = -1;
    private final float mThickness;
    private boolean mTrustManaged;
    private final Animator mVisibleAnimator;

    private class StateUpdateAnimatorListener extends AnimatorListenerAdapter {
        boolean mCancelled;

        private StateUpdateAnimatorListener() {
        }

        public void onAnimationStart(Animator animator) {
            this.mCancelled = false;
        }

        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        public void onAnimationEnd(Animator animator) {
            if (!this.mCancelled) {
                OpTrustDrawable.this.updateState(false);
            }
        }
    }

    public int getOpacity() {
        return -3;
    }

    public OpTrustDrawable(Context context) {
        Resources resources = context.getResources();
        this.mInnerRadiusVisibleMin = resources.getDimension(R$dimen.trust_circle_inner_radius_visible_min);
        this.mInnerRadiusVisibleMax = resources.getDimension(R$dimen.trust_circle_inner_radius_visible_max);
        this.mInnerRadiusExit = resources.getDimension(R$dimen.trust_circle_inner_radius_exit);
        this.mInnerRadiusEnter = resources.getDimension(R$dimen.trust_circle_inner_radius_enter);
        this.mThickness = resources.getDimension(R$dimen.trust_circle_thickness);
        this.mCurInnerRadius = this.mInnerRadiusEnter;
        this.mVisibleAnimator = makeVisibleAnimator();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setColor(Utils.getColorAttrDefaultColor(context, R$attr.wallpaperTextColor));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(this.mThickness);
    }

    public void draw(Canvas canvas) {
        int i = (this.mCurAlpha * this.mAlpha) / 256;
        if (i != 0) {
            Rect bounds = getBounds();
            this.mPaint.setAlpha(i);
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), this.mCurInnerRadius, this.mPaint);
        }
    }

    public void setAlpha(int i) {
        this.mAlpha = i;
    }

    public int getAlpha() {
        return this.mAlpha;
    }

    public void setColorFilter(ColorFilter colorFilter) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void start() {
        if (!this.mAnimating) {
            this.mAnimating = true;
            updateState(true);
            invalidateSelf();
        }
    }

    public void stop() {
        if (this.mAnimating) {
            this.mAnimating = false;
            Animator animator = this.mCurAnimator;
            if (animator != null) {
                animator.cancel();
                this.mCurAnimator = null;
            }
            this.mState = -1;
            this.mCurAlpha = 0;
            this.mCurInnerRadius = this.mInnerRadiusEnter;
            invalidateSelf();
        }
    }

    public void setTrustManaged(boolean z) {
        if (z != this.mTrustManaged || this.mState == -1) {
            this.mTrustManaged = z;
            updateState(true);
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001c, code lost:
        if (r6.mTrustManaged == false) goto L_0x001e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0024, code lost:
        if (r6.mTrustManaged == false) goto L_0x001e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x002b, code lost:
        if (r6.mTrustManaged != false) goto L_0x0016;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0014, code lost:
        if (r6.mTrustManaged != false) goto L_0x0016;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateState(boolean r7) {
        /*
            r6 = this;
            boolean r0 = r6.mAnimating
            if (r0 != 0) goto L_0x0005
            return
        L_0x0005:
            int r0 = r6.mState
            r1 = -1
            r2 = 2
            r3 = 3
            r4 = 1
            if (r0 != r1) goto L_0x0010
            boolean r0 = r6.mTrustManaged
            goto L_0x002e
        L_0x0010:
            if (r0 != 0) goto L_0x0018
            boolean r5 = r6.mTrustManaged
            if (r5 == 0) goto L_0x002e
        L_0x0016:
            r0 = r4
            goto L_0x002e
        L_0x0018:
            if (r0 != r4) goto L_0x0020
            boolean r5 = r6.mTrustManaged
            if (r5 != 0) goto L_0x002e
        L_0x001e:
            r0 = r3
            goto L_0x002e
        L_0x0020:
            if (r0 != r2) goto L_0x0027
            boolean r5 = r6.mTrustManaged
            if (r5 != 0) goto L_0x002e
            goto L_0x001e
        L_0x0027:
            if (r0 != r3) goto L_0x002e
            boolean r5 = r6.mTrustManaged
            if (r5 == 0) goto L_0x002e
            goto L_0x0016
        L_0x002e:
            r5 = 0
            if (r7 != 0) goto L_0x0037
            if (r0 != r4) goto L_0x0034
            r0 = r2
        L_0x0034:
            if (r0 != r3) goto L_0x0037
            r0 = r5
        L_0x0037:
            int r7 = r6.mState
            if (r0 == r7) goto L_0x008d
            android.animation.Animator r7 = r6.mCurAnimator
            if (r7 == 0) goto L_0x0045
            r7.cancel()
            r7 = 0
            r6.mCurAnimator = r7
        L_0x0045:
            if (r0 != 0) goto L_0x004e
            r6.mCurAlpha = r5
            float r7 = r6.mInnerRadiusEnter
            r6.mCurInnerRadius = r7
            goto L_0x0081
        L_0x004e:
            if (r0 != r4) goto L_0x0066
            float r7 = r6.mCurInnerRadius
            int r2 = r6.mCurAlpha
            android.animation.Animator r7 = r6.makeEnterAnimator(r7, r2)
            r6.mCurAnimator = r7
            int r7 = r6.mState
            if (r7 != r1) goto L_0x0081
            android.animation.Animator r7 = r6.mCurAnimator
            r1 = 200(0xc8, double:9.9E-322)
            r7.setStartDelay(r1)
            goto L_0x0081
        L_0x0066:
            if (r0 != r2) goto L_0x0075
            r7 = 76
            r6.mCurAlpha = r7
            float r7 = r6.mInnerRadiusVisibleMax
            r6.mCurInnerRadius = r7
            android.animation.Animator r7 = r6.mVisibleAnimator
            r6.mCurAnimator = r7
            goto L_0x0081
        L_0x0075:
            if (r0 != r3) goto L_0x0081
            float r7 = r6.mCurInnerRadius
            int r1 = r6.mCurAlpha
            android.animation.Animator r7 = r6.makeExitAnimator(r7, r1)
            r6.mCurAnimator = r7
        L_0x0081:
            r6.mState = r0
            android.animation.Animator r7 = r6.mCurAnimator
            if (r7 == 0) goto L_0x008a
            r7.start()
        L_0x008a:
            r6.invalidateSelf()
        L_0x008d:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.phone.OpTrustDrawable.updateState(boolean):void");
    }

    private Animator makeVisibleAnimator() {
        return makeAnimators(this.mInnerRadiusVisibleMax, this.mInnerRadiusVisibleMin, 76, 38, 1000, Interpolators.ACCELERATE_DECELERATE, true, false);
    }

    private Animator makeEnterAnimator(float f, int i) {
        return makeAnimators(f, this.mInnerRadiusVisibleMax, i, 76, 500, Interpolators.LINEAR_OUT_SLOW_IN, false, true);
    }

    private Animator makeExitAnimator(float f, int i) {
        return makeAnimators(f, this.mInnerRadiusExit, i, 0, 500, Interpolators.FAST_OUT_SLOW_IN, false, true);
    }

    private Animator makeAnimators(float f, float f2, int i, int i2, long j, Interpolator interpolator, boolean z, boolean z2) {
        ValueAnimator ofInt = ValueAnimator.ofInt(new int[]{i, i2});
        long j2 = j;
        Interpolator interpolator2 = interpolator;
        boolean z3 = z;
        configureAnimator(ofInt, j2, this.mAlphaUpdateListener, interpolator2, z3);
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{f, f2});
        configureAnimator(ofFloat, j2, this.mRadiusUpdateListener, interpolator2, z3);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{ofInt, ofFloat});
        if (z2) {
            animatorSet.addListener(new StateUpdateAnimatorListener());
        }
        return animatorSet;
    }

    private ValueAnimator configureAnimator(ValueAnimator valueAnimator, long j, AnimatorUpdateListener animatorUpdateListener, Interpolator interpolator, boolean z) {
        valueAnimator.setDuration(j);
        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.setInterpolator(interpolator);
        if (z) {
            valueAnimator.setRepeatCount(-1);
            valueAnimator.setRepeatMode(2);
        }
        return valueAnimator;
    }
}
