package com.oneplus.util;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OpReflectionUtils {
    public static Object getValue(Class cls, Object obj, String str) {
        String str2 = " field ";
        String str3 = "OpReflectionUtils";
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            try {
                return declaredField.get(obj);
            } catch (IllegalAccessException unused) {
                StringBuilder sb = new StringBuilder();
                sb.append("getValue IllegalAccess ");
                sb.append(cls);
                sb.append(str2);
                sb.append(str);
                Log.e(str3, sb.toString());
                return null;
            }
        } catch (NoSuchFieldException unused2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("getValue NoSuchField ");
            sb2.append(cls);
            sb2.append(str2);
            sb2.append(str);
            Log.e(str3, sb2.toString());
            return null;
        }
    }

    public static void setValue(Class cls, Object obj, String str, Object obj2) {
        String str2 = " field ";
        String str3 = "OpReflectionUtils";
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            try {
                declaredField.set(obj, obj2);
            } catch (IllegalAccessException unused) {
                StringBuilder sb = new StringBuilder();
                sb.append("getValue IllegalAccess ");
                sb.append(cls);
                sb.append(str2);
                sb.append(str);
                Log.e(str3, sb.toString());
            }
        } catch (NoSuchFieldException unused2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("getValue NoSuchField ");
            sb2.append(cls);
            sb2.append(str2);
            sb2.append(str);
            Log.e(str3, sb2.toString());
        }
    }

    public static Object methodInvokeVoid(Class cls, Object obj, String str, Object... objArr) {
        String str2 = " method ";
        String str3 = "OpReflectionUtils";
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, new Class[0]);
            declaredMethod.setAccessible(true);
            try {
                return declaredMethod.invoke(obj, objArr);
            } catch (IllegalAccessException unused) {
                StringBuilder sb = new StringBuilder();
                sb.append("methodInvokeVoid IllegalAccess ");
                sb.append(cls);
                sb.append(str2);
                sb.append(str);
                Log.e(str3, sb.toString());
                return null;
            } catch (InvocationTargetException unused2) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("methodInvokeVoid InvocationTarget Class ");
                sb2.append(cls);
                sb2.append(str2);
                sb2.append(str);
                Log.e(str3, sb2.toString());
                return null;
            }
        } catch (NoSuchMethodException unused3) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("methodInvokeVoid NoSuchMethod ");
            sb3.append(cls);
            sb3.append(str2);
            sb3.append(str);
            Log.e(str3, sb3.toString());
            return null;
        }
    }

    public static Method getMethodWithParams(Class cls, String str, Class<?>... clsArr) {
        try {
            return cls.getDeclaredMethod(str, clsArr);
        } catch (NoSuchMethodException unused) {
            StringBuilder sb = new StringBuilder();
            sb.append("getMethodWithParams NoSuchMethod ");
            sb.append(cls);
            sb.append(" method ");
            sb.append(str);
            Log.e("OpReflectionUtils", sb.toString());
            return null;
        }
    }

    public static Object methodInvokeWithArgs(Object obj, Method method, Object... objArr) {
        String str = " method ";
        String str2 = "OpReflectionUtils";
        method.setAccessible(true);
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("methodInvokeWithArgs IllegalAccess Object ");
            sb.append(obj.toString());
            sb.append(str);
            sb.append(method.getName());
            Log.e(str2, sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("methodInvokeWithArgs IllegalAccess e ");
            sb2.append(e.toString());
            Log.e(str2, sb2.toString());
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e2) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("methodInvokeWithArgs InvocationTarget Object ");
            sb3.append(obj.toString());
            sb3.append(str);
            sb3.append(method.getName());
            Log.e(str2, sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            sb4.append("methodInvokeWithArgs ");
            sb4.append(e2.toString());
            Log.e(str2, sb4.toString());
            return null;
        }
    }
}
