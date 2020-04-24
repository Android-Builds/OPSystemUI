package com.oneplus.lib.util;

import android.util.Log;

public class ReflectUtil {
    public static String get(String str) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", new Class[]{String.class}).invoke(null, new Object[]{str});
        } catch (Exception e) {
            Log.e("ReflectUtil", e.getMessage());
            return "";
        }
    }
}
