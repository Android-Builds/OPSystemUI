package com.oneolus.anim;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.util.Log;
import android.view.View;
import android.view.animation.PathInterpolator;
import com.oneplus.util.OpUtils;

public class OpFadeAnim {
    public static AnimatorSet getFadeInOutVisibilityAnimation(final View view, int i, Float f, boolean z) {
        final float f2;
        final float f3;
        String str = "OpFadeAnim";
        if (z || view.getVisibility() != i) {
            if (i == 0) {
                f3 = 1.0f;
                f2 = 0.0f;
            } else if (i != 8 && i != 4) {
                return null;
            } else {
                f2 = 1.0f;
                f3 = 0.0f;
            }
            if (f != null) {
                f2 = f.floatValue();
            }
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("initValue:");
                sb.append(f2);
                sb.append(" / endValue:");
                sb.append(f3);
                Log.i(str, sb.toString());
            }
            new ValueAnimator();
            ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{f2, f3});
            ofFloat.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f));
            ofFloat.setDuration(225);
            ofFloat.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    view.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(225);
            animatorSet.addListener(new AnimatorListener() {
                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationStart(Animator animator) {
                    view.setAlpha(f2);
                }

                public void onAnimationEnd(Animator animator) {
                    if (OpUtils.DEBUG_ONEPLUS) {
                        Log.i("OpFadeAnim", "onAnimationEnd");
                    }
                    view.setAlpha(f3);
                }

                public void onAnimationCancel(Animator animator) {
                    if (OpUtils.DEBUG_ONEPLUS) {
                        Log.i("OpFadeAnim", "onAnimationCancel");
                    }
                    view.setAlpha(f3);
                }
            });
            animatorSet.play(ofFloat);
            return animatorSet;
        }
        Log.i(str, "return null");
        return null;
    }
}
