package com.oneplus.util;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.IOverlayManager.Stub;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.OpFeatures;
import android.util.PathParser;
import android.view.Display.Mode;
import com.android.internal.os.BatteryStatsHelper;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.utils.PowerUtil;
import com.oneplus.custom.utils.OpCustomizeSettings;
import com.oneplus.custom.utils.OpCustomizeSettings.CUSTOM_TYPE;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class OpUtils {
    public static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private static String[] DETmoMmcMnc = {"23203", "23207", "26002", "26201", "23001"};
    public static final boolean SUPPORT_WARP_CHARGING = isSupportWarpCharging();
    private static String[] SprintMmcMnc = {"310120", "312530", "311870", "311490", "310000"};
    private static int mDensityDpi;
    private static boolean mIsCTS = false;
    private static boolean mIsCTSAdded = false;
    private static boolean mIsHomeApp = false;
    private static boolean mIsNeedDarkNavBar = false;
    private static boolean mIsScreenCompat = false;
    private static boolean mIsSupportResolutionSwitch = false;
    private static boolean mIsSystemUI = false;
    private static IOverlayManager mOverlayManager;
    public static int mScreenResolution;
    private static SettingsObserver mSettingsObserver;
    private static ConcurrentHashMap<Integer, Typeface> mTypefaceCache = new ConcurrentHashMap<>();

    private static final class SettingsObserver extends ContentObserver {
        private static final Uri OP_DISPLAY_DENSITY_FORCE = Secure.getUriFor("display_density_forced");
        private static final Uri OP_SCREEN_RESOLUTION_ADJUST_URI = Global.getUriFor("oneplus_screen_resolution_adjust");
        private Context mContext;

        public SettingsObserver(Context context) {
            super(new Handler());
            this.mContext = context;
            ContentResolver contentResolver = this.mContext.getContentResolver();
            contentResolver.registerContentObserver(OP_SCREEN_RESOLUTION_ADJUST_URI, false, this, -1);
            contentResolver.registerContentObserver(OP_DISPLAY_DENSITY_FORCE, false, this, -1);
            onChange(true, null);
        }

        public void onChange(boolean z, Uri uri) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (uri == null || OP_SCREEN_RESOLUTION_ADJUST_URI.equals(uri)) {
                OpUtils.mScreenResolution = Global.getInt(contentResolver, "oneplus_screen_resolution_adjust", 2);
            }
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("update settings observer uri=");
                sb.append(uri);
                sb.append(" mScreenResolution=");
                sb.append(OpUtils.mScreenResolution);
                Log.d("OpUtils", sb.toString());
            }
        }
    }

    public static int getMaxDotsForStatusIconContainer() {
        return 0;
    }

    public static boolean isSupportSOCThreekey() {
        return true;
    }

    public static <T> void safeForeach(List<T> list, Consumer<T> consumer) {
        for (int size = list.size() - 1; size >= 0; size--) {
            consumer.accept(list.get(size));
        }
    }

    public static void init(Context context) {
        updateDensityDpi(context.getResources().getConfiguration().densityDpi);
        mSettingsObserver = new SettingsObserver(context);
        mIsSupportResolutionSwitch = checkIsSupportResolutionSwitch(context);
        loadMCLTypeface();
        mOverlayManager = Stub.asInterface(ServiceManager.getService("overlay"));
    }

    public static boolean isGlobalROM(Context context) {
        return OpFeatures.isSupport(new int[]{1});
    }

    public static boolean isGoogleDarkTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static int getThemeColor(Context context) {
        return System.getInt(context.getContentResolver(), "oem_black_mode", 2);
    }

    public static int getThemeAccentColor(Context context, int i) {
        String string = System.getString(context.getContentResolver(), "oneplus_accent_color");
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("getThemeAccentColor color: ");
            sb.append(string);
            Log.d("OpUtils", sb.toString());
        }
        if (string == null || TextUtils.isEmpty(string)) {
            return context.getResources().getColor(i);
        }
        if (string.charAt(0) != '#') {
            StringBuilder sb2 = new StringBuilder();
            sb2.append('#');
            sb2.append(string);
            string = sb2.toString();
        }
        return Color.parseColor(string);
    }

    public static boolean hasCtaFeature(Context context) {
        return context.getPackageManager().hasSystemFeature("oem.ctaSwitch.support");
    }

    public static boolean isCurrentGuest(Context context) {
        UserInfo userInfo = ((UserManager) context.getSystemService("user")).getUserInfo(ActivityManager.getCurrentUser());
        if (userInfo == null) {
            return false;
        }
        return userInfo.isGuest();
    }

    public static boolean isSpecialTheme(Context context) {
        return System.getInt(context.getContentResolver(), "oem_special_theme", 0) == 1;
    }

    public static boolean isSupportCustomStatusBar() {
        return OpFeatures.isSupport(new int[]{51});
    }

    public static boolean isSupportMultiLTEstatus(Context context) {
        boolean z = false;
        if (context == null) {
            return false;
        }
        if (context.getResources().getBoolean(17891427) && !isUSS()) {
            z = true;
        }
        return z;
    }

    public static boolean isSupportShowHD() {
        return OpFeatures.isSupport(new int[]{70});
    }

    public static boolean isSupportRefreshRateSwitch() {
        return OpFeatures.isSupport(new int[]{121});
    }

    public static boolean isPreventModeEnalbed(Context context) {
        boolean z = false;
        if (KeyguardUpdateMonitor.getCurrentUser() != 0) {
            return false;
        }
        try {
            if (System.getInt(context.getContentResolver(), "oem_acc_anti_misoperation_screen") != 0) {
                z = true;
            }
        } catch (SettingNotFoundException unused) {
        }
        return z;
    }

    public static void updateTopPackage(Context context, String str) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        if (context != null) {
            ResolveInfo resolveActivity = context.getPackageManager().resolveActivity(intent, 65536);
            String str2 = null;
            if (resolveActivity != null) {
                str2 = resolveActivity.activityInfo.packageName;
            }
            boolean z = true;
            if (str2 == null || str == null) {
                mIsHomeApp = false;
            } else {
                mIsHomeApp = str.equals("net.oneplus.launcher") || str.equals("net.oneplus.h2launcher") || str.equals(str2);
            }
            if (str != null) {
                mIsSystemUI = str.equals("com.android.systemui");
            } else {
                mIsSystemUI = false;
            }
            if (str != null) {
                mIsCTS = str.equals("android.systemui.cts");
            } else {
                mIsCTS = false;
            }
            if (str != null) {
                mIsNeedDarkNavBar = str.equals("com.mobile.legends") || str.equals("com.tencent.tmgp.sgame");
            } else {
                mIsNeedDarkNavBar = false;
            }
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
            if (str != null) {
                try {
                    if (appOpsManager.checkOpNoThrow(1006, context.getPackageManager().getApplicationInfo(str, 1).uid, str) != 0) {
                        z = false;
                    }
                    mIsScreenCompat = z;
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    mIsScreenCompat = false;
                }
            } else {
                mIsScreenCompat = false;
            }
        }
    }

    public static boolean isHomeApp() {
        return mIsHomeApp;
    }

    public static boolean isSystemUI() {
        return mIsSystemUI;
    }

    public static boolean isScreenCompat() {
        return mIsScreenCompat;
    }

    public static boolean isCTS() {
        return mIsCTS;
    }

    public static void setCTSAdded(boolean z) {
        mIsCTSAdded = z;
    }

    public static boolean isCTSAdded() {
        return mIsCTSAdded;
    }

    public static boolean isNeedDarkNavBar(Context context) {
        return mIsNeedDarkNavBar && KeyguardUpdateMonitor.getInstance(context).isHangupRecently();
    }

    public static boolean isRemoveRoamingIcon() {
        return isUST();
    }

    public static boolean isSupportShow4GLTE() {
        return isUST();
    }

    public static boolean isSupportFiveBar() {
        return (isUST() || isUSS()) && SignalStrength.NUM_SIGNAL_STRENGTH_BINS == 6;
    }

    public static boolean isUST() {
        if (OpFeatures.isSupport(new int[]{93})) {
            if ("tmo".equals(SystemProperties.get("ro.boot.opcarrier"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUSS() {
        if (OpFeatures.isSupport(new int[]{157})) {
            if ("sprint".equals(SystemProperties.get("ro.boot.opcarrier"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportShowVoLTE(Context context) {
        return !isUST() && !isDETmoMccMnc(context);
    }

    public static boolean isSupportShowVoWifi() {
        return !isUST();
    }

    public static boolean isCustomFingerprint() {
        return OpFeatures.isSupport(new int[]{80});
    }

    public static boolean isSupportQuickLaunch() {
        return OpFeatures.isSupport(new int[]{108});
    }

    /* JADX WARNING: Code restructure failed: missing block: B:3:0x0018, code lost:
        if (android.util.OpFeatures.isSupport(new int[]{150}) != false) goto L_0x001a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isSupportWarpCharging() {
        /*
            com.oneplus.custom.utils.OpCustomizeSettings$CUSTOM_TYPE r0 = com.oneplus.custom.utils.OpCustomizeSettings.CUSTOM_TYPE.MCL
            com.oneplus.custom.utils.OpCustomizeSettings$CUSTOM_TYPE r1 = com.oneplus.custom.utils.OpCustomizeSettings.getCustomType()
            boolean r0 = r0.equals(r1)
            r1 = 0
            r2 = 1
            if (r0 != 0) goto L_0x001a
            int[] r0 = new int[r2]
            r3 = 150(0x96, float:2.1E-43)
            r0[r1] = r3
            boolean r0 = android.util.OpFeatures.isSupport(r0)
            if (r0 == 0) goto L_0x001b
        L_0x001a:
            r1 = r2
        L_0x001b:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "getCustomType:"
            r0.append(r2)
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            java.lang.String r2 = "OpUtils"
            android.util.Log.i(r2, r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.util.OpUtils.isSupportWarpCharging():boolean");
    }

    public static boolean isMCLVersion() {
        return CUSTOM_TYPE.MCL.equals(OpCustomizeSettings.getCustomType());
    }

    public static boolean isMCLVersionFont() {
        if (isMCLVersion()) {
            if (OpFeatures.isSupport(new int[]{256})) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportCustomFingerprintType2() {
        return SystemProperties.get("persist.vendor.oem.fp.version").equals("6");
    }

    public static boolean isSupportLinearVibration() {
        return OpFeatures.isSupport(new int[]{122});
    }

    public static boolean isSupportResolutionSwitch(Context context) {
        return mIsSupportResolutionSwitch;
    }

    private static boolean checkIsSupportResolutionSwitch(Context context) {
        boolean z = false;
        if (context == null) {
            Log.e("OpUtils", "It can't accept null context");
            return false;
        }
        Mode[] supportedModes = ((DisplayManager) context.getSystemService("display")).getDisplay(0).getSupportedModes();
        if (supportedModes != null && supportedModes.length > 2) {
            z = true;
        }
        return z;
    }

    public static boolean isSupportZVibrationMotor() {
        return OpFeatures.isSupport(new int[]{224});
    }

    public static boolean isSupportEmergencyPanel() {
        return true ^ OpFeatures.isSupport(new int[]{145});
    }

    public static boolean isPackageInstalled(Context context, String str) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(str, 0);
        } catch (NameNotFoundException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(" is not installed.");
            sb.append(e);
            Log.d("OpUtils", sb.toString());
            packageInfo = null;
        }
        if (packageInfo != null) {
            return true;
        }
        return false;
    }

    public static void updateDensityDpi(int i) {
        mDensityDpi = i;
    }

    private static int getCurrentDefaultDpi() {
        if (!mIsSupportResolutionSwitch) {
            return 420;
        }
        int i = mScreenResolution;
        if (i == 0 || i == 2) {
            return 560;
        }
        return 420;
    }

    private static float getCurrentDefaultDensity() {
        if (!mIsSupportResolutionSwitch) {
            return 2.625f;
        }
        int i = mScreenResolution;
        if (i == 0 || i == 2) {
            return 3.5f;
        }
        return 2.625f;
    }

    public static boolean is2KResolution() {
        if (!mIsSupportResolutionSwitch) {
            return false;
        }
        int i = mScreenResolution;
        if (i == 0 || i == 2) {
            return true;
        }
        return false;
    }

    public static int convertDpToFixedPx(float f) {
        return Math.round(f * (((float) getCurrentDefaultDpi()) / ((float) mDensityDpi)));
    }

    public static int convertSpToFixedPx(float f, float f2) {
        return Math.round((f / f2) * getCurrentDefaultDensity());
    }

    public static boolean isEnableCustShutdownAnim(Context context) {
        return System.getInt(context.getContentResolver(), "enable_cust_shutdown_anim", 0) == 1;
    }

    public static boolean isSprintMccMnc(Context context) {
        String mmcMnc = getMmcMnc(context, 0);
        if (mmcMnc != null) {
            int i = 0;
            while (true) {
                String[] strArr = SprintMmcMnc;
                if (i >= strArr.length) {
                    break;
                } else if (mmcMnc.equals(strArr[i])) {
                    return true;
                } else {
                    i++;
                }
            }
        }
        return false;
    }

    public static String getMmcMnc(Context context, int i) {
        String str = null;
        if (context == null) {
            return null;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager == null) {
            return null;
        }
        if (i == 0) {
            String subscriberId = telephonyManager.getSubscriberId();
            if (DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("getMmcMnc / imsi:");
                sb.append(subscriberId);
                Log.i("OpUtils", sb.toString());
            }
            if (!TextUtils.isEmpty(subscriberId) && subscriberId.length() > 6) {
                str = subscriberId.substring(0, 6);
            }
        } else if (i == 1) {
            str = telephonyManager.getSimOperator();
        }
        return str;
    }

    public static String getResourceName(Context context, int i) {
        if (i == 0) {
            return "(null)";
        }
        try {
            return context.getResources().getResourceName(i);
        } catch (NotFoundException unused) {
            return "(unknown)";
        }
    }

    public static Typeface getMclTypeface(int i) {
        return (Typeface) mTypefaceCache.get(Integer.valueOf(i));
    }

    private static Typeface getTypefaceByPath(String str) {
        try {
            return Typeface.createFromFile(str);
        } catch (RuntimeException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("RuntimeException, ");
            sb.append(e.getMessage());
            Log.e("OpUtils", sb.toString());
            return null;
        }
    }

    private static void loadMCLTypeface() {
        String str = "/op1/fonts/McLarenBespoke_Lt.ttf";
        if (new File(str).exists()) {
            mTypefaceCache.put(Integer.valueOf(1), getTypefaceByPath(str));
            mTypefaceCache.put(Integer.valueOf(2), getTypefaceByPath("/op1/fonts/McLarenBespoke_Bd.ttf"));
            mTypefaceCache.put(Integer.valueOf(3), getTypefaceByPath("/op1/fonts/McLarenBespoke_Rg.ttf"));
            return;
        }
        Log.d("OpUtils", "Load MCL Typeface failed. Font does not exist: /op1/fonts/McLarenBespoke_Lt.ttf");
    }

    public static long getBatteryTimeRemaining(Context context) {
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, true);
        batteryStatsHelper.create(null);
        return PowerUtil.convertUsToMs(batteryStatsHelper.getStats().computeBatteryTimeRemaining(SystemClock.elapsedRealtime()));
    }

    public static boolean isWLBAllowed(Context context) {
        return isAppExists(context, "com.oneplus.opwlb");
    }

    private static boolean isAppExists(Context context, String str) {
        return getApplicationInfo(context, str) != null;
    }

    public static boolean isWLBFeatureDisable(Context context) {
        int i = System.getInt(context.getContentResolver(), "worklife_feature_enable", 0);
        return i == 1 || i == 2;
    }

    private static ApplicationInfo getApplicationInfo(Context context, String str) {
        try {
            return context.getPackageManager().getApplicationInfo(str, 0);
        } catch (NameNotFoundException unused) {
            Log.d("OpUtils", "App not exists");
            return null;
        }
    }

    public static boolean isDETmoMccMnc(Context context) {
        String mmcMnc = getMmcMnc(context, 0);
        if (mmcMnc != null) {
            int i = 0;
            while (true) {
                String[] strArr = DETmoMmcMnc;
                if (i >= strArr.length) {
                    break;
                } else if (mmcMnc.equals(strArr[i])) {
                    return true;
                } else {
                    i++;
                }
            }
        }
        return false;
    }

    public static boolean isSupportHolePunchFrontCam() {
        return OpFeatures.isSupport(new int[]{263});
    }

    public static boolean isCutoutHide(Context context) {
        if (context != null) {
            return context.getResources().getBoolean(17891481);
        }
        Log.d("OpUtils", "context is null");
        return false;
    }

    public static int getCutoutPathdataHeight(Context context) {
        String str = "OpUtils";
        try {
            Path createPathFromPathData = PathParser.createPathFromPathData(context.getResources().getString(17039759));
            RectF rectF = new RectF();
            createPathFromPathData.computeBounds(rectF, false);
            Rect rect = new Rect();
            rectF.round(rect);
            StringBuilder sb = new StringBuilder();
            sb.append("outRect:");
            sb.append(rect);
            sb.append(", height:");
            sb.append(rect.height());
            Log.i(str, sb.toString());
            return rect.height();
        } catch (Throwable th) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Could not inflate path: ");
            sb2.append(th.toString());
            Log.i(str, sb2.toString());
            return 0;
        }
    }

    public static boolean isCutoutEmulationEnabled() {
        String str = "OpUtils";
        new ArrayList();
        try {
            if (mOverlayManager != null) {
                for (OverlayInfo overlayInfo : mOverlayManager.getOverlayInfosForTarget("android", 0)) {
                    if ("com.android.internal.display_cutout_emulation".equals(overlayInfo.category) && overlayInfo.isEnabled()) {
                        if (DEBUG_ONEPLUS) {
                            Log.d(str, "CutoutEmulation is enabled");
                        }
                        return true;
                    }
                }
            }
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("isCutoutEmulationEnabled: ");
            sb.append(e.toString());
            Log.d(str, sb.toString());
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0028, code lost:
        if (isCutoutHide(r4) == false) goto L_0x002c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getMaxDotsForNotificationIconContainer(android.content.Context r4) {
        /*
            r0 = 0
            if (r4 != 0) goto L_0x000b
            java.lang.String r4 = "OpUtils"
            java.lang.String r1 = "getMaxDotsForNotificationIconContainer context is null"
            android.util.Log.d(r4, r1)
            return r0
        L_0x000b:
            r1 = 1
            int[] r2 = new int[r1]
            r3 = 67
            r2[r0] = r3
            boolean r2 = android.util.OpFeatures.isSupport(r2)
            if (r2 == 0) goto L_0x002b
            int[] r2 = new int[r1]
            r3 = 263(0x107, float:3.69E-43)
            r2[r0] = r3
            boolean r2 = android.util.OpFeatures.isSupport(r2)
            if (r2 != 0) goto L_0x002b
            boolean r4 = isCutoutHide(r4)
            if (r4 != 0) goto L_0x002b
            goto L_0x002c
        L_0x002b:
            r0 = r1
        L_0x002c:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.util.OpUtils.getMaxDotsForNotificationIconContainer(android.content.Context):int");
    }

    public static boolean isWeakFaceUnlockEnabled() {
        return OpFeatures.isSupport(new int[]{293});
    }

    public static boolean isSupportCutout() {
        return OpFeatures.isSupport(new int[]{68});
    }
}
