package com.android.systemui.util.leak;

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;

public class DumpTruck {
    final StringBuilder body = new StringBuilder();
    private final Context context;
    private Uri hprofUri;
    private long pss;

    public DumpTruck(Context context2) {
        this.context = context2;
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0094  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00ed A[SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.android.systemui.util.leak.DumpTruck captureHeaps(int[] r18) {
        /*
            r17 = this;
            r1 = r17
            r0 = r18
            java.lang.Class<com.android.systemui.util.leak.GarbageMonitor> r2 = com.android.systemui.util.leak.GarbageMonitor.class
            java.lang.Object r2 = com.android.systemui.Dependency.get(r2)
            com.android.systemui.util.leak.GarbageMonitor r2 = (com.android.systemui.util.leak.GarbageMonitor) r2
            java.io.File r3 = new java.io.File
            android.content.Context r4 = r1.context
            java.io.File r4 = r4.getCacheDir()
            java.lang.String r5 = "leak"
            r3.<init>(r4, r5)
            r3.mkdirs()
            r4 = 0
            r1.hprofUri = r4
            java.lang.StringBuilder r4 = r1.body
            r5 = 0
            r4.setLength(r5)
            java.lang.StringBuilder r4 = r1.body
            java.lang.String r6 = "Build: "
            r4.append(r6)
            java.lang.String r6 = android.os.Build.DISPLAY
            r4.append(r6)
            java.lang.String r6 = "\n\nProcesses:\n"
            r4.append(r6)
            java.util.ArrayList r4 = new java.util.ArrayList
            r4.<init>()
            int r6 = android.os.Process.myPid()
            int r7 = r0.length
            int[] r7 = java.util.Arrays.copyOf(r0, r7)
            int r8 = r7.length
            r9 = r5
        L_0x0046:
            r0 = 1
            java.lang.String r10 = "\n"
            java.lang.String r11 = "DumpTruck"
            if (r9 >= r8) goto L_0x00f8
            r12 = r7[r9]
            java.lang.StringBuilder r13 = r1.body
            java.lang.String r14 = "  pid "
            r13.append(r14)
            r13.append(r12)
            if (r2 == 0) goto L_0x0091
            com.android.systemui.util.leak.GarbageMonitor$ProcessMemInfo r13 = r2.getMemInfo(r12)
            if (r13 == 0) goto L_0x0091
            java.lang.StringBuilder r14 = r1.body
            java.lang.String r15 = ":"
            r14.append(r15)
            java.lang.String r15 = " up="
            r14.append(r15)
            r16 = r6
            long r5 = r13.getUptime()
            r14.append(r5)
            java.lang.String r5 = " pss="
            r14.append(r5)
            long r5 = r13.currentPss
            r14.append(r5)
            java.lang.String r5 = " uss="
            r14.append(r5)
            long r5 = r13.currentUss
            r14.append(r5)
            long r5 = r13.currentPss
            r1.pss = r5
            r5 = r16
            goto L_0x0092
        L_0x0091:
            r5 = r6
        L_0x0092:
            if (r12 != r5) goto L_0x00ed
            java.io.File r6 = new java.io.File
            java.lang.Object[] r0 = new java.lang.Object[r0]
            java.lang.Integer r13 = java.lang.Integer.valueOf(r12)
            r14 = 0
            r0[r14] = r13
            java.lang.String r13 = "heap-%d.ahprof"
            java.lang.String r0 = java.lang.String.format(r13, r0)
            r6.<init>(r3, r0)
            java.lang.String r0 = r6.getPath()
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r13 = "Dumping memory info for process "
            r6.append(r13)
            r6.append(r12)
            java.lang.String r12 = " to "
            r6.append(r12)
            r6.append(r0)
            java.lang.String r6 = r6.toString()
            android.util.Log.v(r11, r6)
            android.os.Debug.dumpHprofData(r0)     // Catch:{ IOException -> 0x00d6 }
            r4.add(r0)     // Catch:{ IOException -> 0x00d6 }
            java.lang.StringBuilder r0 = r1.body     // Catch:{ IOException -> 0x00d6 }
            java.lang.String r6 = " (hprof attached)"
            r0.append(r6)     // Catch:{ IOException -> 0x00d6 }
            goto L_0x00ed
        L_0x00d6:
            r0 = move-exception
            java.lang.String r6 = "error dumping memory:"
            android.util.Log.e(r11, r6, r0)
            java.lang.StringBuilder r6 = r1.body
            java.lang.String r11 = "\n** Could not dump heap: \n"
            r6.append(r11)
            java.lang.String r0 = r0.toString()
            r6.append(r0)
            r6.append(r10)
        L_0x00ed:
            java.lang.StringBuilder r0 = r1.body
            r0.append(r10)
            int r9 = r9 + 1
            r6 = r5
            r5 = 0
            goto L_0x0046
        L_0x00f8:
            java.io.File r2 = new java.io.File     // Catch:{ IOException -> 0x0140 }
            java.lang.String r5 = "hprof-%d.zip"
            java.lang.Object[] r0 = new java.lang.Object[r0]     // Catch:{ IOException -> 0x0140 }
            long r6 = java.lang.System.currentTimeMillis()     // Catch:{ IOException -> 0x0140 }
            java.lang.Long r6 = java.lang.Long.valueOf(r6)     // Catch:{ IOException -> 0x0140 }
            r7 = 0
            r0[r7] = r6     // Catch:{ IOException -> 0x0140 }
            java.lang.String r0 = java.lang.String.format(r5, r0)     // Catch:{ IOException -> 0x0140 }
            r2.<init>(r3, r0)     // Catch:{ IOException -> 0x0140 }
            java.lang.String r0 = r2.getCanonicalPath()     // Catch:{ IOException -> 0x0140 }
            boolean r2 = zipUp(r0, r4)     // Catch:{ IOException -> 0x0140 }
            if (r2 == 0) goto L_0x0158
            java.io.File r2 = new java.io.File     // Catch:{ IOException -> 0x0140 }
            r2.<init>(r0)     // Catch:{ IOException -> 0x0140 }
            android.content.Context r0 = r1.context     // Catch:{ IOException -> 0x0140 }
            java.lang.String r3 = "com.android.systemui.fileprovider"
            android.net.Uri r0 = androidx.core.content.FileProvider.getUriForFile(r0, r3, r2)     // Catch:{ IOException -> 0x0140 }
            r1.hprofUri = r0     // Catch:{ IOException -> 0x0140 }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x0140 }
            r0.<init>()     // Catch:{ IOException -> 0x0140 }
            java.lang.String r2 = "Heap dump accessible at URI: "
            r0.append(r2)     // Catch:{ IOException -> 0x0140 }
            android.net.Uri r2 = r1.hprofUri     // Catch:{ IOException -> 0x0140 }
            r0.append(r2)     // Catch:{ IOException -> 0x0140 }
            java.lang.String r0 = r0.toString()     // Catch:{ IOException -> 0x0140 }
            android.util.Log.v(r11, r0)     // Catch:{ IOException -> 0x0140 }
            goto L_0x0158
        L_0x0140:
            r0 = move-exception
            java.lang.String r2 = "unable to zip up heapdumps"
            android.util.Log.e(r11, r2, r0)
            java.lang.StringBuilder r2 = r1.body
            java.lang.String r3 = "\n** Could not zip up files: \n"
            r2.append(r3)
            java.lang.String r0 = r0.toString()
            r2.append(r0)
            r2.append(r10)
        L_0x0158:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.util.leak.DumpTruck.captureHeaps(int[]):com.android.systemui.util.leak.DumpTruck");
    }

    public Intent createShareIntent() {
        Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
        intent.addFlags(268435456);
        intent.addFlags(1);
        intent.putExtra("android.intent.extra.SUBJECT", String.format("SystemUI memory dump (pss=%dM)", new Object[]{Long.valueOf(this.pss / 1024)}));
        intent.putExtra("android.intent.extra.TEXT", this.body.toString());
        if (this.hprofUri != null) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(this.hprofUri);
            intent.setType("application/zip");
            intent.putParcelableArrayListExtra("android.intent.extra.STREAM", arrayList);
            intent.setClipData(new ClipData(new ClipDescription("content", new String[]{"text/plain"}), new Item(this.hprofUri)));
            intent.addFlags(1);
        }
        return intent;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0045, code lost:
        r8 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:?, code lost:
        $closeResource(r7, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0049, code lost:
        throw r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0051, code lost:
        r8 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        $closeResource(r7, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0055, code lost:
        throw r8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean zipUp(java.lang.String r7, java.util.ArrayList<java.lang.String> r8) {
        /*
            r0 = 0
            java.util.zip.ZipOutputStream r1 = new java.util.zip.ZipOutputStream     // Catch:{ IOException -> 0x0056 }
            java.io.FileOutputStream r2 = new java.io.FileOutputStream     // Catch:{ IOException -> 0x0056 }
            r2.<init>(r7)     // Catch:{ IOException -> 0x0056 }
            r1.<init>(r2)     // Catch:{ IOException -> 0x0056 }
            r7 = 1048576(0x100000, float:1.469368E-39)
            byte[] r2 = new byte[r7]     // Catch:{ all -> 0x004f }
            java.util.Iterator r8 = r8.iterator()     // Catch:{ all -> 0x004f }
        L_0x0013:
            boolean r3 = r8.hasNext()     // Catch:{ all -> 0x004f }
            r4 = 0
            if (r3 == 0) goto L_0x004a
            java.lang.Object r3 = r8.next()     // Catch:{ all -> 0x004f }
            java.lang.String r3 = (java.lang.String) r3     // Catch:{ all -> 0x004f }
            java.io.BufferedInputStream r5 = new java.io.BufferedInputStream     // Catch:{ all -> 0x004f }
            java.io.FileInputStream r6 = new java.io.FileInputStream     // Catch:{ all -> 0x004f }
            r6.<init>(r3)     // Catch:{ all -> 0x004f }
            r5.<init>(r6)     // Catch:{ all -> 0x004f }
            java.util.zip.ZipEntry r6 = new java.util.zip.ZipEntry     // Catch:{ all -> 0x0043 }
            r6.<init>(r3)     // Catch:{ all -> 0x0043 }
            r1.putNextEntry(r6)     // Catch:{ all -> 0x0043 }
        L_0x0032:
            int r3 = r5.read(r2, r0, r7)     // Catch:{ all -> 0x0043 }
            if (r3 <= 0) goto L_0x003c
            r1.write(r2, r0, r3)     // Catch:{ all -> 0x0043 }
            goto L_0x0032
        L_0x003c:
            r1.closeEntry()     // Catch:{ all -> 0x0043 }
            $closeResource(r4, r5)     // Catch:{ all -> 0x004f }
            goto L_0x0013
        L_0x0043:
            r7 = move-exception
            throw r7     // Catch:{ all -> 0x0045 }
        L_0x0045:
            r8 = move-exception
            $closeResource(r7, r5)     // Catch:{ all -> 0x004f }
            throw r8     // Catch:{ all -> 0x004f }
        L_0x004a:
            r7 = 1
            $closeResource(r4, r1)     // Catch:{ IOException -> 0x0056 }
            return r7
        L_0x004f:
            r7 = move-exception
            throw r7     // Catch:{ all -> 0x0051 }
        L_0x0051:
            r8 = move-exception
            $closeResource(r7, r1)     // Catch:{ IOException -> 0x0056 }
            throw r8     // Catch:{ IOException -> 0x0056 }
        L_0x0056:
            r7 = move-exception
            java.lang.String r8 = "DumpTruck"
            java.lang.String r1 = "error zipping up profile data"
            android.util.Log.e(r8, r1, r7)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.util.leak.DumpTruck.zipUp(java.lang.String, java.util.ArrayList):boolean");
    }

    private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) {
        if (th != null) {
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        } else {
            autoCloseable.close();
        }
    }
}
