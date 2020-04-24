package com.oneplus.lib.util;

import android.graphics.PorterDuff.Mode;
import android.os.Build.VERSION;

public class DrawableUtils {
    public static Mode parseTintMode(int i, Mode mode) {
        if (i == 3) {
            return Mode.SRC_OVER;
        }
        if (i == 5) {
            return Mode.SRC_IN;
        }
        if (i == 9) {
            return Mode.SRC_ATOP;
        }
        switch (i) {
            case 14:
                return Mode.MULTIPLY;
            case 15:
                return Mode.SCREEN;
            case 16:
                if (VERSION.SDK_INT >= 11) {
                    mode = Mode.valueOf("ADD");
                }
                return mode;
            default:
                return mode;
        }
    }
}
