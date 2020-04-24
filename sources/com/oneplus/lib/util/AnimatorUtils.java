package com.oneplus.lib.util;

import android.animation.TimeInterpolator;
import android.os.Bundle;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.oneplus.support.core.view.animation.FastOutLinearInInterpolator;
import com.oneplus.support.core.view.animation.LinearOutSlowInInterpolator;

public class AnimatorUtils {
    public static final TimeInterpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new FastOutLinearInInterpolator();
    public static final Interpolator FastOutLinearInInterpolator = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator FastOutLinearInInterpolatorSine = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    public static final Interpolator FastOutSlowInInterpolator = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    public static final Interpolator GRID_ITEM_ANIMATION_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.3f, 1.0f);
    public static final TimeInterpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR = new LinearOutSlowInInterpolator();
    public static final Interpolator LinearOutSlowInInterpolator = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    public static String PROPNAME_HEIGHT = "space:height";
    public static String PROPNAME_SCREENLOCATION_LEFT = "location:left";
    public static String PROPNAME_SCREENLOCATION_TOP = "location:top";
    public static String PROPNAME_WIDTH = "space:width";
    private static Bundle mEndValues = new Bundle();
}
