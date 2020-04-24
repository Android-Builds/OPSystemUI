package com.oneplus.lib.widget.util;

import android.content.Context;
import android.util.TypedValue;

/* renamed from: com.oneplus.lib.widget.util.utils */
public class C1963utils {
    public static int resolveDefStyleAttr(Context context, int i) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(i, typedValue, true);
        if ((typedValue.resourceId >>> 24) == 1) {
            return 0;
        }
        return i;
    }
}
