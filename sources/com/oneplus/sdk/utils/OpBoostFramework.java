package com.oneplus.sdk.utils;

import android.os.Build;
import android.os.SystemProperties;
import android.util.BoostFramework;
import android.util.Log;

public class OpBoostFramework {
    /* access modifiers changed from: private */
    public static final boolean DBG = Build.DEBUG_ONEPLUS;
    private static BoostFramework sPerfBoostInstance = null;
    private static String sProjectName = SystemProperties.get("ro.boot.project_name");
    private static String sProjectName_old = SystemProperties.get("ro.prj_name");

    private static class MyLog {
        /* access modifiers changed from: private */
        /* renamed from: v */
        public static void m60v(String str, String str2) {
            if (OpBoostFramework.DBG) {
                Log.v(str, str2);
            }
        }
    }

    public OpBoostFramework() {
        if (sPerfBoostInstance == null) {
            sPerfBoostInstance = new BoostFramework();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("OpBoostFramework() : sPerfBoostInstance = ");
        sb.append(sPerfBoostInstance);
        MyLog.m60v("OpBoostFramework", sb.toString());
    }

    public int acquireBoostFor(int i, int i2) {
        String str;
        int i3;
        if (sProjectName_old.length() != 0) {
            str = sProjectName_old;
        } else {
            str = sProjectName;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("acquireBoostFor() : policy = ");
        sb.append(i);
        String str2 = "OpBoostFramework";
        MyLog.m60v(str2, sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("acquireBoostFor() : duration = ");
        sb2.append(i2);
        MyLog.m60v(str2, sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("projectName = ");
        sb3.append(str);
        MyLog.m60v(str2, sb3.toString());
        int i4 = 0;
        if (i != 0) {
            i4 = -3;
        } else if (i2 > 2000 || i2 < 0) {
            i4 = -2;
        } else {
            try {
                if ("14049".equals(str)) {
                    sPerfBoostInstance.perfLockAcquire(i2, new int[]{7681, 525, 19716, 7954});
                } else if ("15801".equals(str)) {
                    sPerfBoostInstance.perfLockAcquire(i2, new int[]{1082130432, 2100, 1082130688, 1600});
                } else if ("15811".equals(str)) {
                    sPerfBoostInstance.perfLockAcquire(i2, new int[]{1082130432, 2400, 1082130688, 1600});
                } else {
                    if (!"16859".equals(str) && !"17801".equals(str)) {
                        if (!"17819".equals(str)) {
                            MyLog.m60v(str2, "Try to acquire full speed perf lock for unspecified proj");
                            sPerfBoostInstance.perfLockAcquire(i2, new int[]{1082130432, 4094, 1082130688, 4094});
                        }
                    }
                    sPerfBoostInstance.perfLockAcquire(i2, new int[]{1082130432, 4094, 1082130688, 4094});
                }
            } catch (Exception e) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Exception ");
                sb4.append(e);
                Log.e(str2, sb4.toString());
                e.printStackTrace();
                i3 = -4;
            }
        }
        i3 = i4;
        StringBuilder sb5 = new StringBuilder();
        sb5.append("acquireBoostFor++++++() : ret = ");
        sb5.append(i3);
        MyLog.m60v(str2, sb5.toString());
        return i3;
    }
}
