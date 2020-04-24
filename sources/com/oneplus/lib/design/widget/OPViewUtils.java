package com.oneplus.lib.design.widget;

import android.graphics.PorterDuff.Mode;

class OPViewUtils {
    static Mode parseTintMode(int i, Mode mode) {
        if (i == 3) {
            return Mode.SRC_OVER;
        }
        if (i == 5) {
            return Mode.SRC_IN;
        }
        if (i == 9) {
            return Mode.SRC_ATOP;
        }
        if (i != 14) {
            return i != 15 ? mode : Mode.SCREEN;
        }
        return Mode.MULTIPLY;
    }
}
