package com.oneplus.custom.utils;

import java.util.ArrayList;

public class ParamReader {
    private static boolean mParamReadRet = false;
    private static ArrayList<Byte> mParamReadbyte = new ArrayList<>();

    /* JADX WARNING: Removed duplicated region for block: B:48:0x0096 A[SYNTHETIC, Splitter:B:48:0x0096] */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00b7 A[SYNTHETIC, Splitter:B:56:0x00b7] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getCustFlagVal() {
        /*
            java.lang.String r0 = "getCustFlagVal ~P result = "
            int r1 = android.os.Build.VERSION.SDK_INT
            r2 = 0
            r3 = 0
            r4 = 1
            java.lang.String r5 = "ParamReader"
            r6 = 27
            if (r1 > r6) goto L_0x00c9
            java.io.File r1 = new java.io.File
            java.lang.String r6 = "/sys/module/param_read_write/parameters/cust_flag"
            r1.<init>(r6)
            boolean r6 = r1.exists()
            if (r6 != 0) goto L_0x0020
            java.lang.String r0 = "CUSTOM_FN_PATH not existed"
            com.oneplus.custom.utils.MyLog.m59w(r5, r0)
            return r3
        L_0x0020:
            java.io.BufferedReader r6 = new java.io.BufferedReader     // Catch:{ Exception -> 0x008a, all -> 0x0087 }
            java.io.FileReader r7 = new java.io.FileReader     // Catch:{ Exception -> 0x008a, all -> 0x0087 }
            r7.<init>(r1)     // Catch:{ Exception -> 0x008a, all -> 0x0087 }
            r6.<init>(r7)     // Catch:{ Exception -> 0x008a, all -> 0x0087 }
            r1 = r3
        L_0x002b:
            java.lang.String r2 = r6.readLine()     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
            if (r2 == 0) goto L_0x0065
            r7 = -1
            int r8 = r2.hashCode()     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
            r9 = 2
            switch(r8) {
                case 49: goto L_0x004f;
                case 50: goto L_0x0045;
                case 51: goto L_0x003b;
                default: goto L_0x003a;
            }     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
        L_0x003a:
            goto L_0x0058
        L_0x003b:
            java.lang.String r8 = "3"
            boolean r2 = r2.equals(r8)     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
            if (r2 == 0) goto L_0x0058
            r7 = r9
            goto L_0x0058
        L_0x0045:
            java.lang.String r8 = "2"
            boolean r2 = r2.equals(r8)     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
            if (r2 == 0) goto L_0x0058
            r7 = r4
            goto L_0x0058
        L_0x004f:
            java.lang.String r8 = "1"
            boolean r2 = r2.equals(r8)     // Catch:{ Exception -> 0x0084, all -> 0x00b5 }
            if (r2 == 0) goto L_0x0058
            r7 = r3
        L_0x0058:
            if (r7 == 0) goto L_0x0063
            if (r7 == r4) goto L_0x0061
            if (r7 == r9) goto L_0x005f
            goto L_0x002b
        L_0x005f:
            r1 = 3
            goto L_0x002b
        L_0x0061:
            r1 = r9
            goto L_0x002b
        L_0x0063:
            r1 = r4
            goto L_0x002b
        L_0x0065:
            r6.close()     // Catch:{ Exception -> 0x0069 }
            goto L_0x0071
        L_0x0069:
            r2 = move-exception
            java.lang.String r2 = r2.getMessage()
            com.oneplus.custom.utils.MyLog.m57e(r5, r2)
        L_0x0071:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
        L_0x0076:
            r2.append(r0)
            r2.append(r1)
            java.lang.String r0 = r2.toString()
            com.oneplus.custom.utils.MyLog.m58v(r5, r0)
            return r1
        L_0x0084:
            r2 = move-exception
            r3 = r1
            goto L_0x008d
        L_0x0087:
            r6 = r2
        L_0x0088:
            r1 = r3
            goto L_0x00b5
        L_0x008a:
            r1 = move-exception
            r6 = r2
            r2 = r1
        L_0x008d:
            java.lang.String r1 = r2.getMessage()     // Catch:{ all -> 0x0088 }
            com.oneplus.custom.utils.MyLog.m57e(r5, r1)     // Catch:{ all -> 0x0088 }
            if (r6 == 0) goto L_0x00a2
            r6.close()     // Catch:{ Exception -> 0x009a }
            goto L_0x00a2
        L_0x009a:
            r1 = move-exception
            java.lang.String r1 = r1.getMessage()
            com.oneplus.custom.utils.MyLog.m57e(r5, r1)
        L_0x00a2:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            r1.append(r0)
            r1.append(r3)
            java.lang.String r0 = r1.toString()
            com.oneplus.custom.utils.MyLog.m58v(r5, r0)
            return r3
        L_0x00b5:
            if (r6 == 0) goto L_0x00c3
            r6.close()     // Catch:{ Exception -> 0x00bb }
            goto L_0x00c3
        L_0x00bb:
            r2 = move-exception
            java.lang.String r2 = r2.getMessage()
            com.oneplus.custom.utils.MyLog.m57e(r5, r2)
        L_0x00c3:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            goto L_0x0076
        L_0x00c9:
            java.lang.String r0 = "android.os.ServiceManager"
            java.lang.Class r0 = java.lang.Class.forName(r0)     // Catch:{ Exception -> 0x0144 }
            java.lang.String r1 = "getService"
            java.lang.Class[] r6 = new java.lang.Class[r4]     // Catch:{ Exception -> 0x0144 }
            java.lang.Class<java.lang.String> r7 = java.lang.String.class
            r6[r3] = r7     // Catch:{ Exception -> 0x0144 }
            java.lang.reflect.Method r0 = r0.getMethod(r1, r6)     // Catch:{ Exception -> 0x0144 }
            java.lang.Object[] r1 = new java.lang.Object[r4]     // Catch:{ Exception -> 0x0144 }
            java.lang.String r6 = "ParamService"
            r1[r3] = r6     // Catch:{ Exception -> 0x0144 }
            java.lang.Object r0 = r0.invoke(r2, r1)     // Catch:{ Exception -> 0x0144 }
            int r1 = android.os.Build.VERSION.SDK_INT     // Catch:{ Exception -> 0x0144 }
            r6 = 28
            if (r1 > r6) goto L_0x00f2
            java.lang.String r1 = "com.oem.os.IParamService$Stub"
            java.lang.Class r1 = java.lang.Class.forName(r1)     // Catch:{ Exception -> 0x0144 }
            goto L_0x00f8
        L_0x00f2:
            java.lang.String r1 = "com.oneplus.os.IParamService$Stub"
            java.lang.Class r1 = java.lang.Class.forName(r1)     // Catch:{ Exception -> 0x0144 }
        L_0x00f8:
            java.lang.String r6 = "asInterface"
            java.lang.Class[] r7 = new java.lang.Class[r4]     // Catch:{ Exception -> 0x0144 }
            java.lang.Class<android.os.IBinder> r8 = android.os.IBinder.class
            r7[r3] = r8     // Catch:{ Exception -> 0x0144 }
            java.lang.reflect.Method r1 = r1.getMethod(r6, r7)     // Catch:{ Exception -> 0x0144 }
            java.lang.Object[] r6 = new java.lang.Object[r4]     // Catch:{ Exception -> 0x0144 }
            r6[r3] = r0     // Catch:{ Exception -> 0x0144 }
            java.lang.Object r0 = r1.invoke(r2, r6)     // Catch:{ Exception -> 0x0144 }
            java.lang.Class r1 = r0.getClass()     // Catch:{ Exception -> 0x0144 }
            java.lang.String r2 = "getParamIntSYNC"
            java.lang.Class[] r6 = new java.lang.Class[r4]     // Catch:{ Exception -> 0x0144 }
            java.lang.Class r7 = java.lang.Integer.TYPE     // Catch:{ Exception -> 0x0144 }
            r6[r3] = r7     // Catch:{ Exception -> 0x0144 }
            java.lang.reflect.Method r1 = r1.getMethod(r2, r6)     // Catch:{ Exception -> 0x0144 }
            java.lang.Object[] r2 = new java.lang.Object[r4]     // Catch:{ Exception -> 0x0144 }
            r4 = 4
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)     // Catch:{ Exception -> 0x0144 }
            r2[r3] = r4     // Catch:{ Exception -> 0x0144 }
            java.lang.Object r0 = r1.invoke(r0, r2)     // Catch:{ Exception -> 0x0144 }
            java.lang.Integer r0 = (java.lang.Integer) r0     // Catch:{ Exception -> 0x0144 }
            int r3 = r0.intValue()     // Catch:{ Exception -> 0x0144 }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0144 }
            r0.<init>()     // Catch:{ Exception -> 0x0144 }
            java.lang.String r1 = "getCustFlagVal P~ result = "
            r0.append(r1)     // Catch:{ Exception -> 0x0144 }
            r0.append(r3)     // Catch:{ Exception -> 0x0144 }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0144 }
            com.oneplus.custom.utils.MyLog.m58v(r5, r0)     // Catch:{ Exception -> 0x0144 }
            goto L_0x0159
        L_0x0144:
            r0 = move-exception
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "getCustFlagVal throws exception: "
            r1.append(r2)
            r1.append(r0)
            java.lang.String r0 = r1.toString()
            com.oneplus.custom.utils.MyLog.m57e(r5, r0)
        L_0x0159:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.custom.utils.ParamReader.getCustFlagVal():int");
    }
}
