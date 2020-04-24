package com.oneplus.lib.widget.util;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.View;
import com.oneplus.commonctrl.R$styleable;
import java.lang.reflect.Method;

public class ViewUtils {
    static final int[] VIEW_STATE_IDS = {16842909, 1, 16842913, 2, 16842908, 4, 16842910, 8, 16842919, 16, 16843518, 32, 16843547, 64, 16843623, 128, 16843624, 256, 16843625, 512};
    private static final int[][] VIEW_STATE_SETS = new int[(1 << (VIEW_STATE_IDS.length / 2))][];
    private static Method sComputeFitSystemWindowsMethod;

    public static int combineMeasuredStates(int i, int i2) {
        return i | i2;
    }

    static {
        int[] iArr = new int[VIEW_STATE_IDS.length];
        int i = 0;
        while (true) {
            int[] iArr2 = R$styleable.OPViewDrawableStates;
            if (i >= iArr2.length) {
                break;
            }
            int i2 = iArr2[i];
            int i3 = 0;
            while (true) {
                int[] iArr3 = VIEW_STATE_IDS;
                if (i3 >= iArr3.length) {
                    break;
                }
                if (iArr3[i3] == i2) {
                    int i4 = i * 2;
                    iArr[i4] = i2;
                    iArr[i4 + 1] = iArr3[i3 + 1];
                }
                i3 += 2;
            }
            i++;
        }
        for (int i5 = 0; i5 < VIEW_STATE_SETS.length; i5++) {
            int[] iArr4 = new int[Integer.bitCount(i5)];
            int i6 = 0;
            for (int i7 = 0; i7 < iArr.length; i7 += 2) {
                if ((iArr[i7 + 1] & i5) != 0) {
                    int i8 = i6 + 1;
                    iArr4[i6] = iArr[i7];
                    i6 = i8;
                }
            }
            VIEW_STATE_SETS[i5] = iArr4;
        }
        if (VERSION.SDK_INT >= 18) {
            try {
                sComputeFitSystemWindowsMethod = View.class.getDeclaredMethod("computeFitSystemWindows", new Class[]{Rect.class, Rect.class});
                if (!sComputeFitSystemWindowsMethod.isAccessible()) {
                    sComputeFitSystemWindowsMethod.setAccessible(true);
                }
            } catch (NoSuchMethodException unused) {
                Log.d("ViewUtils", "Could not find method computeFitSystemWindows. Oh well.");
            }
        }
    }

    public static int[] getViewState(int i) {
        int[][] iArr = VIEW_STATE_SETS;
        if (i < iArr.length) {
            return iArr[i];
        }
        throw new IllegalArgumentException("Invalid state set mask");
    }

    public static boolean isLayoutRtl(View view) {
        return view.getLayoutDirection() == 1;
    }

    public static void computeFitSystemWindows(View view, Rect rect, Rect rect2) {
        Method method = sComputeFitSystemWindowsMethod;
        if (method != null) {
            try {
                method.invoke(view, new Object[]{rect, rect2});
            } catch (Exception e) {
                Log.d("ViewUtils", "Could not invoke computeFitSystemWindows", e);
            }
        }
    }

    public static int dip2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static boolean isVisibleToUser(View view, Rect rect) {
        return view.isAttachedToWindow() && view.getGlobalVisibleRect(rect);
    }

    public static void scaleRect(Rect rect, float f) {
        if (f != 1.0f) {
            rect.left = (int) ((((float) rect.left) * f) + 0.5f);
            rect.top = (int) ((((float) rect.top) * f) + 0.5f);
            rect.right = (int) ((((float) rect.right) * f) + 0.5f);
            rect.bottom = (int) ((((float) rect.bottom) * f) + 0.5f);
        }
    }
}
