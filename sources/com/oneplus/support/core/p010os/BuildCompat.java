package com.oneplus.support.core.p010os;

import android.os.Build.VERSION;

/* renamed from: com.oneplus.support.core.os.BuildCompat */
public class BuildCompat {
    public static boolean isAtLeastP() {
        return VERSION.SDK_INT >= 28;
    }
}
