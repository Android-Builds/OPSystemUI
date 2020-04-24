package com.oneplus.lib.design.widget;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.oneplus.support.core.view.animation.FastOutLinearInInterpolator;
import com.oneplus.support.core.view.animation.FastOutSlowInInterpolator;
import com.oneplus.support.core.view.animation.LinearOutSlowInInterpolator;

class OPAnimationUtils {
    static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    static final Interpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new FastOutLinearInInterpolator();
    static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    static final Interpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR = new LinearOutSlowInInterpolator();
}
