package com.oneplus.lib.design.widget;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.ScaleDrawable;
import android.util.Log;
import java.lang.reflect.Method;

class OPDrawableUtils {
    private static Method sSetConstantStateMethod;
    private static boolean sSetConstantStateMethodFetched;

    static boolean setContainerConstantState(DrawableContainer drawableContainer, ConstantState constantState) {
        return setContainerConstantStateV9(drawableContainer, constantState);
    }

    private static boolean setContainerConstantStateV9(DrawableContainer drawableContainer, ConstantState constantState) {
        String str = "DrawableUtils";
        if (!sSetConstantStateMethodFetched) {
            try {
                sSetConstantStateMethod = DrawableContainer.class.getDeclaredMethod("setConstantState", new Class[]{DrawableContainerState.class});
                sSetConstantStateMethod.setAccessible(true);
            } catch (NoSuchMethodException unused) {
                Log.e(str, "Could not fetch setConstantState(). Oh well.");
            }
            sSetConstantStateMethodFetched = true;
        }
        Method method = sSetConstantStateMethod;
        if (method != null) {
            try {
                method.invoke(drawableContainer, new Object[]{constantState});
                return true;
            } catch (Exception unused2) {
                Log.e(str, "Could not invoke setConstantState(). Oh well.");
            }
        }
        return false;
    }

    public static boolean canSafelyMutateDrawable(Drawable drawable) {
        if (drawable instanceof DrawableContainer) {
            ConstantState constantState = drawable.getConstantState();
            if (constantState instanceof DrawableContainerState) {
                for (Drawable canSafelyMutateDrawable : ((DrawableContainerState) constantState).getChildren()) {
                    if (!canSafelyMutateDrawable(canSafelyMutateDrawable)) {
                        return false;
                    }
                }
            }
        } else if (drawable instanceof ScaleDrawable) {
            return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
        }
        return true;
    }
}
