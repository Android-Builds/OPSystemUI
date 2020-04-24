package com.oneplus.lib.util;

import android.os.Build.VERSION;

public class SystemUtils {
    public static boolean isAtLeastO() {
        if (!"REL".equals(VERSION.CODENAME)) {
            if ("O".equals(VERSION.CODENAME) || VERSION.CODENAME.startsWith("OMR")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAtLeastM() {
        return VERSION.SDK_INT >= 23;
    }
}
