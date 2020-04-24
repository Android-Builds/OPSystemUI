package com.oneplus.custom.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class SystemProperties {
    private static Method sSystemPropertiesGetMethod;

    public static String get(String str) {
        if (sSystemPropertiesGetMethod == null) {
            try {
                Class cls = Class.forName("android.os.SystemProperties");
                if (cls != null) {
                    sSystemPropertiesGetMethod = cls.getMethod("get", new Class[]{String.class});
                }
            } catch (ClassNotFoundException | NoSuchMethodException unused) {
            }
        }
        Method method = sSystemPropertiesGetMethod;
        if (method != null) {
            try {
                return (String) method.invoke(null, new Object[]{str});
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException unused2) {
            }
        }
        return null;
    }
}
