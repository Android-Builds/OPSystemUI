package com.oneplus.aod;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.util.OpFeatures;
import com.android.keyguard.KeyguardUpdateMonitor;

public class OpAodUtils {
    public static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private static boolean mIsAlwaysOnModeEnabled;
    private static boolean mIsNotificationWakeUpEnabled;
    private static boolean mMotionAwakeOn;
    private static boolean mSingleTapAwakeOn;

    public static boolean isSupportAlwaysOn() {
        return false;
    }

    public static void init(Context context, int i) {
        updateDozeSettings(context, i);
    }

    public static String getDeviceTag() {
        return SystemProperties.get("ro.boot.project_name");
    }

    public static boolean isMotionAwakeOn() {
        StringBuilder sb = new StringBuilder();
        sb.append("mMotionAwakeOn: ");
        sb.append(mMotionAwakeOn);
        Log.d("OPAodUtils", sb.toString());
        return mMotionAwakeOn;
    }

    public static void updateMotionAwakeState(Context context, int i) {
        boolean z = false;
        if (System.getIntForUser(context.getContentResolver(), "prox_wake_enabled", 0, i) == 1) {
            z = true;
        }
        mMotionAwakeOn = z;
        updateAlwaysOnState(context, i);
        StringBuilder sb = new StringBuilder();
        sb.append("updateMotionAwakeState: ");
        sb.append(mMotionAwakeOn);
        sb.append(", user = ");
        sb.append(i);
        Log.d("OPAodUtils", sb.toString());
    }

    public static void updateSingleTapAwakeState(Context context, int i) {
        boolean z = false;
        if (((System.getIntForUser(context.getContentResolver(), "oem_acc_blackscreen_gestrue_enable", 0, i) & 2048) >> 11) == 1) {
            z = true;
        }
        mSingleTapAwakeOn = z;
        StringBuilder sb = new StringBuilder();
        sb.append("updateSingleTapAwakeState: ");
        sb.append(mSingleTapAwakeOn);
        sb.append(", user = ");
        sb.append(i);
        Log.d("OPAodUtils", sb.toString());
    }

    public static boolean isAlwaysOnEnabled() {
        StringBuilder sb = new StringBuilder();
        sb.append("isAlwaysOnEnabled: ");
        sb.append(mIsAlwaysOnModeEnabled);
        Log.d("OPAodUtils", sb.toString());
        return "1".equals(SystemProperties.get("sys.aod.localtest"));
    }

    public static boolean isAlwaysOnEnabledWithTimer() {
        String str = SystemProperties.get("sys.aod.localtest.timer");
        StringBuilder sb = new StringBuilder();
        sb.append("isAlwaysOnEnabledWithTimer: ");
        String str2 = "1";
        sb.append(str2.equals(str));
        Log.d("OPAodUtils", sb.toString());
        return str2.equals(str);
    }

    public static void updateAlwaysOnState(Context context, int i) {
        String str = "OPAodUtils";
        boolean z = true;
        if (!mMotionAwakeOn || !isSupportAlwaysOn() || Secure.getIntForUser(context.getContentResolver(), "aod_display_mode", 0, i) != 1) {
            z = false;
        }
        mIsAlwaysOnModeEnabled = z;
        try {
            SystemProperties.set("sys.aod.disable", mIsAlwaysOnModeEnabled ? "0" : "1");
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception e = ");
            sb.append(e.toString());
            Log.d(str, sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("updateAlwaysOnState: ");
        sb2.append(mIsAlwaysOnModeEnabled);
        sb2.append(", user = ");
        sb2.append(i);
        Log.d(str, sb2.toString());
    }

    public static boolean isNotificationWakeUpEnabled() {
        return mIsNotificationWakeUpEnabled;
    }

    public static void updateNotificationWakeState(Context context, int i) {
        boolean z = true;
        if (1 != Secure.getIntForUser(context.getContentResolver(), "notification_wake_enabled", 1, i)) {
            z = false;
        }
        mIsNotificationWakeUpEnabled = z;
        StringBuilder sb = new StringBuilder();
        sb.append("updateNotificationWakeState: ");
        sb.append(mIsNotificationWakeUpEnabled);
        sb.append(", user = ");
        sb.append(i);
        Log.d("OPAodUtils", sb.toString());
    }

    public static void updateDozeSettings(Context context, int i) {
        updateMotionAwakeState(context, i);
        updateSingleTapAwakeState(context, i);
        updateNotificationWakeState(context, i);
    }

    public static boolean isSingleTapEnabled() {
        StringBuilder sb = new StringBuilder();
        sb.append("isSingleTapEnabled: ");
        sb.append(mSingleTapAwakeOn);
        Log.d("OPAodUtils", sb.toString());
        return mSingleTapAwakeOn;
    }

    public static boolean isCustomFingerprint() {
        return OpFeatures.isSupport(new int[]{80});
    }

    public static boolean isNotificationLightEnabled() {
        if ("1".equals(SystemProperties.get("sys.aod.notif_light_disable"))) {
            return false;
        }
        return OpFeatures.isSupport(new int[]{133});
    }

    public static void checkAodSettingsState(Context context, int i) {
        int i2 = 0;
        boolean z = 1 == System.getIntForUser(context.getContentResolver(), "prox_wake_enabled", 0, i);
        boolean isSingleTapEnabled = isSingleTapEnabled();
        int intForUser = Secure.getIntForUser(context.getContentResolver(), "aod_clock_style", 0, i);
        if ((z || isSingleTapEnabled) && intForUser != 3) {
            i2 = 1;
        }
        System.putIntForUser(context.getContentResolver(), "aod_smart_display_enabled", i2, i);
    }

    public static void checkAodStyle(Context context, int i) {
        String str = "aod_clock_style";
        r0 = Secure.getIntForUser(context.getContentResolver(), str, 0, i) == 3 ? isCustomFingerprint() ? !KeyguardUpdateMonitor.getInstance(context).isUnlockWithFingerprintPossible(i) : true : false;
        if (r0) {
            Log.i("OPAodUtils", "Reset aod clock style for user");
            Secure.putIntForUser(context.getContentResolver(), str, 0, i);
        }
    }
}
