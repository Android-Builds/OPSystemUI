package com.oneplus.lib.design.widget;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class Utils {
    private static final Matrix IDENTITY = new Matrix();
    private static final ThreadLocal<Matrix> sMatrix = new ThreadLocal<>();
    private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();

    static int constrain(int i, int i2, int i3) {
        return i < i2 ? i2 : i > i3 ? i3 : i;
    }

    static float lerp(float f, float f2, float f3) {
        return f + (f3 * (f2 - f));
    }

    static boolean objectEquals(Object obj, Object obj2) {
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    static void offsetDescendantMatrix(ViewParent viewParent, View view, Matrix matrix) {
        ViewParent parent = view.getParent();
        if ((parent instanceof View) && parent != viewParent) {
            View view2 = (View) parent;
            offsetDescendantMatrix(viewParent, view2, matrix);
            matrix.preTranslate((float) (-view2.getScrollX()), (float) (-view2.getScrollY()));
        }
        matrix.preTranslate((float) view.getLeft(), (float) view.getTop());
        if (!view.getMatrix().isIdentity()) {
            matrix.preConcat(view.getMatrix());
        }
    }

    static void offsetDescendantRect(ViewGroup viewGroup, View view, Rect rect) {
        Matrix matrix = (Matrix) sMatrix.get();
        if (matrix == null) {
            matrix = new Matrix();
            sMatrix.set(matrix);
        } else {
            matrix.set(IDENTITY);
        }
        offsetDescendantMatrix(viewGroup, view, matrix);
        RectF rectF = (RectF) sRectF.get();
        if (rectF == null) {
            rectF = new RectF();
        }
        rectF.set(rect);
        matrix.mapRect(rectF);
        rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f), (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
    }

    public static void getDescendantRect(ViewGroup viewGroup, View view, Rect rect) {
        rect.set(0, 0, view.getWidth(), view.getHeight());
        offsetDescendantRect(viewGroup, view, rect);
    }
}
