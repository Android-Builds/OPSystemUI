package com.oneplus.custom.utils;

import android.util.Log;

public class MyLog {
    protected static final boolean DBG = "true".equals(SystemProperties.get("persist.sys.assert.panic"));

    /* renamed from: v */
    protected static void m58v(String str, String str2) {
        if (DBG) {
            Log.v(str, str2);
        }
    }

    /* renamed from: w */
    protected static void m59w(String str, String str2) {
        if (DBG) {
            Log.w(str, str2);
        }
    }

    /* renamed from: e */
    protected static void m57e(String str, String str2) {
        if (DBG) {
            Log.e(str, str2);
        }
    }
}
