package com.oneplus.lib.design.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Map;

public class TextScale extends Transition {
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        if (view instanceof TextView) {
            String str = "android:textscale:scale";
            transitionValues.values.put(str, Float.valueOf(((TextView) view).getScaleX()));
        }
    }

    public Animator createAnimator(ViewGroup viewGroup, TransitionValues transitionValues, TransitionValues transitionValues2) {
        ValueAnimator valueAnimator = null;
        if (!(transitionValues == null || transitionValues2 == null || !(transitionValues.view instanceof TextView))) {
            View view = transitionValues2.view;
            if (view instanceof TextView) {
                final TextView textView = (TextView) view;
                Map map = transitionValues.values;
                Map map2 = transitionValues2.values;
                String str = "android:textscale:scale";
                float f = 1.0f;
                float floatValue = map.get(str) != null ? ((Float) map.get(str)).floatValue() : 1.0f;
                if (map2.get(str) != null) {
                    f = ((Float) map2.get(str)).floatValue();
                }
                if (floatValue == f) {
                    return null;
                }
                valueAnimator = ValueAnimator.ofFloat(new float[]{floatValue, f});
                valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        textView.setScaleX(floatValue);
                        textView.setScaleY(floatValue);
                    }
                });
            }
        }
        return valueAnimator;
    }
}
