package com.android.systemui.doze;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TimeUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.doze.DozeMachine.State;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DozeLog {
    private static final boolean DEBUG = Log.isLoggable("DozeLog", 3);
    static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final int SIZE = (Build.IS_DEBUGGABLE ? 400 : 50);
    private static int sCount;
    private static SummaryStats sEmergencyCallStats;
    private static final KeyguardUpdateMonitorCallback sKeyguardCallback = new KeyguardUpdateMonitorCallback() {
        public void onEmergencyCallAction() {
            DozeLog.traceEmergencyCall();
        }

        public void onKeyguardBouncerChanged(boolean z) {
            DozeLog.traceKeyguardBouncerChanged(z);
        }

        public void onStartedWakingUp() {
            DozeLog.traceScreenOn();
        }

        public void onFinishedGoingToSleep(int i) {
            DozeLog.traceScreenOff(i);
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            DozeLog.traceKeyguard(z);
        }
    };
    private static String[] sMessages;
    private static SummaryStats sNotificationPulseStats;
    private static SummaryStats sPickupPulseNearVibrationStats;
    private static SummaryStats sPickupPulseNotNearVibrationStats;
    private static int sPosition;
    private static SummaryStats[][] sProxStats;
    private static boolean sPulsing;
    private static boolean sRegisterKeyguardCallback = true;
    private static SummaryStats sScreenOnNotPulsingStats;
    private static SummaryStats sScreenOnPulsingStats;
    /* access modifiers changed from: private */
    public static long sSince;
    private static long[] sTimes;

    private static class SummaryStats {
        private int mCount;

        private SummaryStats() {
        }

        public void append() {
            this.mCount++;
        }

        public void dump(PrintWriter printWriter, String str) {
            if (this.mCount != 0) {
                printWriter.print("    ");
                printWriter.print(str);
                printWriter.print(": n=");
                printWriter.print(this.mCount);
                printWriter.print(" (");
                printWriter.print((((double) this.mCount) / ((double) (System.currentTimeMillis() - DozeLog.sSince))) * 1000.0d * 60.0d * 60.0d);
                printWriter.print("/hr)");
                printWriter.println();
            }
        }
    }

    public static void tracePickupWakeUp(Context context, boolean z) {
        SummaryStats summaryStats;
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("pickupWakeUp withinVibrationThreshold=");
        sb.append(z);
        log(sb.toString());
        if (z) {
            summaryStats = sPickupPulseNearVibrationStats;
        } else {
            summaryStats = sPickupPulseNotNearVibrationStats;
        }
        summaryStats.append();
    }

    public static void tracePulseStart(int i) {
        sPulsing = true;
        StringBuilder sb = new StringBuilder();
        sb.append("pulseStart reason=");
        sb.append(reasonToString(i));
        log(sb.toString());
    }

    public static void tracePulseFinish() {
        sPulsing = false;
        log("pulseFinish");
    }

    public static void traceNotificationPulse(Context context) {
        init(context);
        log("notificationPulse");
        sNotificationPulseStats.append();
    }

    private static void init(Context context) {
        synchronized (DozeLog.class) {
            if (sMessages == null) {
                sTimes = new long[SIZE];
                sMessages = new String[SIZE];
                sSince = System.currentTimeMillis();
                sPickupPulseNearVibrationStats = new SummaryStats();
                sPickupPulseNotNearVibrationStats = new SummaryStats();
                sNotificationPulseStats = new SummaryStats();
                sScreenOnPulsingStats = new SummaryStats();
                sScreenOnNotPulsingStats = new SummaryStats();
                sEmergencyCallStats = new SummaryStats();
                sProxStats = (SummaryStats[][]) Array.newInstance(SummaryStats.class, new int[]{14, 2});
                for (int i = 0; i < 14; i++) {
                    sProxStats[i][0] = new SummaryStats();
                    sProxStats[i][1] = new SummaryStats();
                }
                log("init");
                if (sRegisterKeyguardCallback) {
                    KeyguardUpdateMonitor.getInstance(context).registerCallback(sKeyguardCallback);
                }
            }
        }
    }

    public static void traceDozing(Context context, boolean z) {
        sPulsing = false;
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("dozing ");
        sb.append(z);
        log(sb.toString());
    }

    public static void traceFling(boolean z, boolean z2, boolean z3, boolean z4) {
        StringBuilder sb = new StringBuilder();
        sb.append("fling expand=");
        sb.append(z);
        sb.append(" aboveThreshold=");
        sb.append(z2);
        sb.append(" thresholdNeeded=");
        sb.append(z3);
        sb.append(" screenOnFromTouch=");
        sb.append(z4);
        log(sb.toString());
    }

    public static void traceEmergencyCall() {
        log("emergencyCall");
        sEmergencyCallStats.append();
    }

    public static void traceKeyguardBouncerChanged(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("bouncer ");
        sb.append(z);
        log(sb.toString());
    }

    public static void traceScreenOn() {
        StringBuilder sb = new StringBuilder();
        sb.append("screenOn pulsing=");
        sb.append(sPulsing);
        log(sb.toString());
        (sPulsing ? sScreenOnPulsingStats : sScreenOnNotPulsingStats).append();
        sPulsing = false;
    }

    public static void traceScreenOff(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("screenOff why=");
        sb.append(i);
        log(sb.toString());
    }

    public static void traceMissedTick(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("missedTick by=");
        sb.append(str);
        log(sb.toString());
    }

    public static void traceTimeTickScheduled(long j, long j2) {
        StringBuilder sb = new StringBuilder();
        sb.append("timeTickScheduled at=");
        sb.append(FORMAT.format(new Date(j)));
        sb.append(" triggerAt=");
        sb.append(FORMAT.format(new Date(j2)));
        log(sb.toString());
    }

    public static void traceKeyguard(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("keyguard ");
        sb.append(z);
        log(sb.toString());
        if (!z) {
            sPulsing = false;
        }
    }

    public static void traceState(State state) {
        StringBuilder sb = new StringBuilder();
        sb.append("state ");
        sb.append(state);
        log(sb.toString());
    }

    public static void traceWakeDisplay(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("wakeDisplay ");
        sb.append(z);
        log(sb.toString());
    }

    public static void traceProximityResult(Context context, boolean z, long j, int i) {
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("proximityResult reason=");
        sb.append(reasonToString(i));
        sb.append(" near=");
        sb.append(z);
        sb.append(" millis=");
        sb.append(j);
        log(sb.toString());
        sProxStats[i][!z].append();
    }

    public static String reasonToString(int i) {
        switch (i) {
            case 0:
                return "intent";
            case 1:
                return "notification";
            case 2:
                return "sigmotion";
            case 3:
                return "pickup";
            case 4:
                return "doubletap";
            case 5:
                return "longpress";
            case 6:
                return "docking";
            case 7:
                return "wakeup";
            case 8:
                return "wakelockscreen";
            case 9:
                return "tap";
            case 10:
                return "threeKeyChanged";
            case 11:
                return "alwaysOn";
            case 12:
                return "singleTap";
            case 13:
                return "fingerprintPoke";
            default:
                StringBuilder sb = new StringBuilder();
                sb.append("bad reason: ");
                sb.append(i);
                throw new IllegalArgumentException(sb.toString());
        }
    }

    public static void dump(PrintWriter printWriter) {
        synchronized (DozeLog.class) {
            if (sMessages != null) {
                printWriter.println("  Doze log:");
                int i = ((sPosition - sCount) + SIZE) % SIZE;
                for (int i2 = 0; i2 < sCount; i2++) {
                    int i3 = (i + i2) % SIZE;
                    printWriter.print("    ");
                    printWriter.print(FORMAT.format(new Date(sTimes[i3])));
                    printWriter.print(' ');
                    printWriter.println(sMessages[i3]);
                }
                printWriter.print("  Doze summary stats (for ");
                TimeUtils.formatDuration(System.currentTimeMillis() - sSince, printWriter);
                printWriter.println("):");
                sPickupPulseNearVibrationStats.dump(printWriter, "Pickup pulse (near vibration)");
                sPickupPulseNotNearVibrationStats.dump(printWriter, "Pickup pulse (not near vibration)");
                sNotificationPulseStats.dump(printWriter, "Notification pulse");
                sScreenOnPulsingStats.dump(printWriter, "Screen on (pulsing)");
                sScreenOnNotPulsingStats.dump(printWriter, "Screen on (not pulsing)");
                sEmergencyCallStats.dump(printWriter, "Emergency call");
                for (int i4 = 0; i4 < 14; i4++) {
                    String reasonToString = reasonToString(i4);
                    SummaryStats summaryStats = sProxStats[i4][0];
                    StringBuilder sb = new StringBuilder();
                    sb.append("Proximity near (");
                    sb.append(reasonToString);
                    sb.append(")");
                    summaryStats.dump(printWriter, sb.toString());
                    SummaryStats summaryStats2 = sProxStats[i4][1];
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Proximity far (");
                    sb2.append(reasonToString);
                    sb2.append(")");
                    summaryStats2.dump(printWriter, sb2.toString());
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0031, code lost:
        if (DEBUG == false) goto L_0x0038;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0033, code lost:
        android.util.Log.d("DozeLog", r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0038, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void log(java.lang.String r5) {
        /*
            java.lang.Class<com.android.systemui.doze.DozeLog> r0 = com.android.systemui.doze.DozeLog.class
            monitor-enter(r0)
            java.lang.String[] r1 = sMessages     // Catch:{ all -> 0x0039 }
            if (r1 != 0) goto L_0x0009
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            return
        L_0x0009:
            long[] r1 = sTimes     // Catch:{ all -> 0x0039 }
            int r2 = sPosition     // Catch:{ all -> 0x0039 }
            long r3 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0039 }
            r1[r2] = r3     // Catch:{ all -> 0x0039 }
            java.lang.String[] r1 = sMessages     // Catch:{ all -> 0x0039 }
            int r2 = sPosition     // Catch:{ all -> 0x0039 }
            r1[r2] = r5     // Catch:{ all -> 0x0039 }
            int r1 = sPosition     // Catch:{ all -> 0x0039 }
            int r1 = r1 + 1
            int r2 = SIZE     // Catch:{ all -> 0x0039 }
            int r1 = r1 % r2
            sPosition = r1     // Catch:{ all -> 0x0039 }
            int r1 = sCount     // Catch:{ all -> 0x0039 }
            int r1 = r1 + 1
            int r2 = SIZE     // Catch:{ all -> 0x0039 }
            int r1 = java.lang.Math.min(r1, r2)     // Catch:{ all -> 0x0039 }
            sCount = r1     // Catch:{ all -> 0x0039 }
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            boolean r0 = DEBUG
            if (r0 == 0) goto L_0x0038
            java.lang.String r0 = "DozeLog"
            android.util.Log.d(r0, r5)
        L_0x0038:
            return
        L_0x0039:
            r5 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeLog.log(java.lang.String):void");
    }

    public static void tracePulseDropped(Context context, boolean z, State state, boolean z2) {
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("pulseDropped pulsePending=");
        sb.append(z);
        sb.append(" state=");
        sb.append(state);
        sb.append(" blocked=");
        sb.append(z2);
        log(sb.toString());
    }

    public static void tracePulseTouchDisabledByProx(Context context, boolean z) {
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("pulseTouchDisabledByProx ");
        sb.append(z);
        log(sb.toString());
    }

    public static void traceSensor(Context context, int i) {
        init(context);
        StringBuilder sb = new StringBuilder();
        sb.append("sensor type=");
        sb.append(reasonToString(i));
        log(sb.toString());
    }
}
