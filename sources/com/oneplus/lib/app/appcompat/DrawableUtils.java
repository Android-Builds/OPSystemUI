package com.oneplus.lib.app.appcompat;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build.VERSION;
import com.oneplus.lib.app.appcompat.graphics.drawable.DrawableWrapper;
import java.lang.reflect.Method;

public class DrawableUtils {
    public static final Rect INSETS_NONE = new Rect();
    private static Class<?> sInsetsClazz;

    static {
        if (VERSION.SDK_INT >= 18) {
            try {
                sInsetsClazz = Class.forName("android.graphics.Insets");
            } catch (ClassNotFoundException unused) {
            }
        }
    }

    static void fixDrawable(Drawable drawable) {
        if (VERSION.SDK_INT == 21) {
            if ("android.graphics.drawable.VectorDrawable".equals(drawable.getClass().getName())) {
                fixVectorDrawableTinting(drawable);
            }
        }
    }

    public static boolean canSafelyMutateDrawable(Drawable drawable) {
        Class cls;
        if (VERSION.SDK_INT < 15 && (drawable instanceof InsetDrawable)) {
            return false;
        }
        if (VERSION.SDK_INT < 15 && (drawable instanceof GradientDrawable)) {
            return false;
        }
        if (VERSION.SDK_INT < 17 && (drawable instanceof LayerDrawable)) {
            return false;
        }
        Class cls2 = null;
        try {
            cls = Class.forName("com.oneplus.support.core.graphics.drawable.WrappedDrawable");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            cls = null;
        }
        try {
            cls2 = Class.forName("com.oneplus.support.core.graphics.drawable.DrawableWrapper");
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        }
        if (drawable instanceof DrawableContainer) {
            ConstantState constantState = drawable.getConstantState();
            if (constantState instanceof DrawableContainerState) {
                for (Drawable canSafelyMutateDrawable : ((DrawableContainerState) constantState).getChildren()) {
                    if (!canSafelyMutateDrawable(canSafelyMutateDrawable)) {
                        return false;
                    }
                }
            }
        } else if (drawable.getClass() == cls || drawable.getClass() == cls2) {
            if (cls != null) {
                cls2 = cls;
            }
            try {
                Method method = cls2.getMethod("getWrappedDrawable", new Class[0]);
                method.setAccessible(true);
                return canSafelyMutateDrawable((Drawable) method.invoke(drawable, new Object[0]));
            } catch (Exception e3) {
                e3.printStackTrace();
                return false;
            }
        } else if (drawable instanceof DrawableWrapper) {
            return canSafelyMutateDrawable(((DrawableWrapper) drawable).getWrappedDrawable());
        } else {
            if (drawable instanceof ScaleDrawable) {
                return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
            }
        }
        return true;
    }

    private static void fixVectorDrawableTinting(Drawable drawable) {
        int[] state = drawable.getState();
        if (state == null || state.length == 0) {
            drawable.setState(ThemeUtils.CHECKED_STATE_SET);
        } else {
            drawable.setState(ThemeUtils.EMPTY_STATE_SET);
        }
        drawable.setState(state);
    }
}
