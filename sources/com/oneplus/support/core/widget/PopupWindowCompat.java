package com.oneplus.support.core.widget;

import android.os.Build.VERSION;
import android.util.Log;
import android.widget.PopupWindow;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PopupWindowCompat {
    private static Field sOverlapAnchorField;
    private static boolean sOverlapAnchorFieldAttempted;
    private static Method sSetWindowLayoutTypeMethod;
    private static boolean sSetWindowLayoutTypeMethodAttempted;

    public static void setOverlapAnchor(PopupWindow popupWindow, boolean z) {
        int i = VERSION.SDK_INT;
        if (i >= 23) {
            popupWindow.setOverlapAnchor(z);
        } else if (i >= 21) {
            String str = "PopupWindowCompatApi21";
            if (!sOverlapAnchorFieldAttempted) {
                try {
                    sOverlapAnchorField = PopupWindow.class.getDeclaredField("mOverlapAnchor");
                    sOverlapAnchorField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(str, "Could not fetch mOverlapAnchor field from PopupWindow", e);
                }
                sOverlapAnchorFieldAttempted = true;
            }
            Field field = sOverlapAnchorField;
            if (field != null) {
                try {
                    field.set(popupWindow, Boolean.valueOf(z));
                } catch (IllegalAccessException e2) {
                    Log.i(str, "Could not set overlap anchor field in PopupWindow", e2);
                }
            }
        }
    }

    public static void setWindowLayoutType(PopupWindow popupWindow, int i) {
        if (VERSION.SDK_INT >= 23) {
            popupWindow.setWindowLayoutType(i);
            return;
        }
        if (!sSetWindowLayoutTypeMethodAttempted) {
            try {
                sSetWindowLayoutTypeMethod = PopupWindow.class.getDeclaredMethod("setWindowLayoutType", new Class[]{Integer.TYPE});
                sSetWindowLayoutTypeMethod.setAccessible(true);
            } catch (Exception unused) {
            }
            sSetWindowLayoutTypeMethodAttempted = true;
        }
        Method method = sSetWindowLayoutTypeMethod;
        if (method != null) {
            try {
                method.invoke(popupWindow, new Object[]{Integer.valueOf(i)});
            } catch (Exception unused2) {
            }
        }
    }
}
