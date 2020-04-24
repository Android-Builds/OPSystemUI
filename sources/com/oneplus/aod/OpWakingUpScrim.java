package com.oneplus.aod;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.PathInterpolator;
import com.oneplus.util.OpUtils;

public class OpWakingUpScrim extends View {
    private boolean isDisappearAnimationStart = false;
    /* access modifiers changed from: private */
    public float mAnimationFrame;
    private int mCircle1Colr = -16777216;
    private int mCircle2Colr = -16777216;
    private int mCircle3Colr = -16777216;
    private ValueAnimator mDisappearAnimator;
    /* access modifiers changed from: private */
    public float mRadius = 0.0f;
    private boolean mTestUnlockSpeed;
    private int mWithoutDelayAnimationDuration = 0;
    private float mWithoutDelayAnimationStartFrame = 0.0f;

    private int getColor(float f) {
        return (((int) (f * 255.0f)) << 24) & -1;
    }

    public OpWakingUpScrim(Context context) {
        super(context);
    }

    public OpWakingUpScrim(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpWakingUpScrim(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int i = width / 2;
        int i2 = height / 2;
        Path path = new Path();
        path.addRect(0.0f, 0.0f, (float) width, (float) height, Direction.CCW);
        float f = (float) i;
        float f2 = (float) i2;
        path.addCircle(f, f2, this.mRadius, Direction.CW);
        path.setFillType(FillType.EVEN_ODD);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(10.0f);
        canvas2.drawPath(path, paint);
        int i3 = (this.mRadius > f ? 1 : (this.mRadius == f ? 0 : -1));
        if (this.mRadius > 0.0f) {
            canvas.save();
            RadialGradient radialGradient = new RadialGradient(f, f2, this.mRadius, new int[]{this.mCircle1Colr, this.mCircle2Colr, this.mCircle3Colr}, null, TileMode.CLAMP);
            Paint paint2 = new Paint();
            paint2.setShader(radialGradient);
            canvas2.drawCircle(f, f2, this.mRadius, paint2);
            canvas.restore();
            if (this.mTestUnlockSpeed) {
                canvas.save();
                Paint paint3 = new Paint();
                paint3.setColor(-1);
                paint3.setStyle(Style.FILL);
                paint3.setTextSize(100.0f);
                canvas2.drawText(Float.toString(this.mAnimationFrame), 200.0f, 200.0f, paint3);
                canvas.restore();
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("mTestUnlockSpeed draw mRadius:");
                    sb.append(this.mAnimationFrame);
                    Log.i("OpWakingUpScrim", sb.toString());
                }
            }
        }
    }

    public void reset() {
        String str = "OpWakingUpScrim";
        Log.i(str, "reset");
        setAlpha(1.0f);
        this.mRadius = 0.0f;
        invalidate();
        this.mTestUnlockSpeed = SystemProperties.getBoolean("debug.wakingup.scrim", false);
        if (this.mTestUnlockSpeed) {
            this.mWithoutDelayAnimationStartFrame = ((float) SystemProperties.getInt("debug.wakingup.scrim.animation.start.frame", 0)) / 100.0f;
            this.mWithoutDelayAnimationDuration = SystemProperties.getInt("debug.wakingup.scrim.animation.start.duration", 0);
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("debug AnimationStartFrame:");
                sb.append(this.mWithoutDelayAnimationStartFrame);
                sb.append(" AnimationDuration:");
                sb.append(this.mWithoutDelayAnimationDuration);
                Log.i(str, sb.toString());
            }
        }
    }

    public ValueAnimator getDisappearAnimationWithoutDelay() {
        float f = this.mWithoutDelayAnimationStartFrame;
        if (f <= 0.0f) {
            f = 0.5f;
        }
        int i = this.mWithoutDelayAnimationDuration;
        if (i <= 0) {
            i = 200;
        }
        this.mDisappearAnimator = ValueAnimator.ofFloat(new float[]{f, 1.0f});
        this.mDisappearAnimator.setDuration((long) i);
        this.mDisappearAnimator.setInterpolator(new PathInterpolator(0.6f, 0.0f, 0.6f, 1.0f));
        this.mDisappearAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpWakingUpScrim opWakingUpScrim = OpWakingUpScrim.this;
                opWakingUpScrim.mRadius = (((7.7f * floatValue) + 0.5f) * ((float) opWakingUpScrim.getWidth())) / 2.0f;
                OpWakingUpScrim.this.mAnimationFrame = floatValue;
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("DisappearAnimationWithoutDelay mRadius:");
                    sb.append(floatValue);
                    Log.i("OpWakingUpScrim", sb.toString());
                }
                OpWakingUpScrim.this.calculateCircleColor(floatValue);
                OpWakingUpScrim.this.invalidate();
            }
        });
        this.mDisappearAnimator.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }
        });
        return this.mDisappearAnimator;
    }

    public ValueAnimator getDisappearAnimationWithDelay() {
        this.mDisappearAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mDisappearAnimator.setDuration(300);
        getHeight();
        PathInterpolator pathInterpolator = new PathInterpolator(0.6f, 0.0f, 0.6f, 1.0f);
        this.mDisappearAnimator.setStartDelay(75);
        this.mDisappearAnimator.setInterpolator(pathInterpolator);
        this.mDisappearAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpWakingUpScrim opWakingUpScrim = OpWakingUpScrim.this;
                opWakingUpScrim.mRadius = (((7.7f * floatValue) + 0.5f) * ((float) opWakingUpScrim.getWidth())) / 2.0f;
                OpWakingUpScrim.this.mAnimationFrame = floatValue;
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("DisappearAnimationWithDelay mRadius:");
                    sb.append(floatValue);
                    Log.i("OpWakingUpScrim", sb.toString());
                }
                OpWakingUpScrim.this.calculateCircleColor(floatValue);
                OpWakingUpScrim.this.invalidate();
            }
        });
        this.mDisappearAnimator.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }
        });
        return this.mDisappearAnimator;
    }

    /* access modifiers changed from: private */
    public void calculateCircleColor(float f) {
        float f2;
        float f3;
        float width = (((float) getWidth()) * 8.2f) / 2.0f;
        float f4 = width / 2.0f;
        float f5 = (9.0f * width) / 10.0f;
        float f6 = this.mRadius;
        float f7 = 0.0f;
        if (width >= f6 && f6 >= f4) {
            if (f6 > f5) {
                f3 = ((width - f6) / (width - f5)) * 0.91f;
            } else {
                f3 = 1.0f - f;
                if (0.91f >= f3) {
                    f3 = 0.91f;
                }
            }
            f7 = f3;
            f2 = ((width - this.mRadius) / (width - f4)) * 0.81f * f7;
        } else if (this.mRadius < f4) {
            float f8 = 1.0f - f;
            f7 = 0.91f < f8 ? f8 : 0.91f;
            float f9 = 0.81f * f7;
            if (f8 > f9) {
                f9 = f8;
            }
            f2 = f9;
        } else {
            f2 = 0.0f;
        }
        this.mCircle1Colr = getColor(f2);
        this.mCircle2Colr = getColor(f7);
        this.mCircle3Colr = -16777216;
    }
}
