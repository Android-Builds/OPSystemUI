package com.android.settingslib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.telephony.ServiceState;
import android.text.format.Formatter;
import com.android.internal.annotations.VisibleForTesting;
import java.text.NumberFormat;
import java.util.Locale;

public class Utils {
    @VisibleForTesting
    static final String STORAGE_MANAGER_ENABLED_PROPERTY = "ro.storage_manager.enabled";
    public static final String[] UNIT_OF_STORAGE = {"%28?<%21[吉千兆太]%29比特", "%28?<%21[吉千兆太]%29字节", "吉比特", "吉字节", "千比特", "千字节", "兆比特", "兆字节", "太比特", "太字节"};
    public static final String[] UNIT_OF_STORAGE_REPLACE = {"b", "B", "Gb", "GB", "Kb", "KB", "Mb", "MB", "Tb", "TB"};
    static final int[] WIFI_4_PIE = {17302845, 17302846, 17302847, 17302848, 17302849};
    static final int[] WIFI_5_PIE = {17302850, 17302851, 17302852, 17302853, 17302854};
    static final int[] WIFI_6_PIE = {17302855, 17302856, 17302857, 17302858, 17302859};
    static final int[] WIFI_PIE = {R$drawable.op_ic_wifi_signal_0, R$drawable.op_ic_wifi_signal_1, R$drawable.op_ic_wifi_signal_2, R$drawable.op_ic_wifi_signal_3, R$drawable.op_ic_wifi_signal_4};
    private static String sPermissionControllerPackageName;
    private static String sServicesSystemSharedLibPackageName;
    private static String sSharedSystemSharedLibPackageName;
    private static Signature[] sSystemSignature;

    public static void updateLocationEnabled(Context context, boolean z, int i, int i2) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LocationManager.class);
        Secure.putIntForUser(context.getContentResolver(), "location_changer", i2, i);
        Intent intent = new Intent("com.android.settings.location.MODE_CHANGING");
        int i3 = 3;
        int i4 = locationManager.isLocationEnabled() ? 3 : 0;
        if (!z) {
            i3 = 0;
        }
        intent.putExtra("CURRENT_MODE", i4);
        intent.putExtra("NEW_MODE", i3);
        context.sendBroadcastAsUser(intent, UserHandle.of(i), "android.permission.WRITE_SECURE_SETTINGS");
        locationManager.setLocationEnabledForUser(z, UserHandle.of(i));
    }

    public static String formatPercentage(long j, long j2) {
        return formatPercentage(((double) j) / ((double) j2));
    }

    public static String formatPercentage(int i) {
        return formatPercentage(((double) i) / 100.0d);
    }

    public static String formatPercentage(double d) {
        return NumberFormat.getPercentInstance().format(d);
    }

    public static ColorStateList getColorAccent(Context context) {
        return getColorAttr(context, 16843829);
    }

    public static ColorStateList getColorError(Context context) {
        return getColorAttr(context, 16844099);
    }

    public static int getColorAccentDefaultColor(Context context) {
        return getColorAttrDefaultColor(context, 16843829);
    }

    public static int getColorErrorDefaultColor(Context context) {
        return getColorAttrDefaultColor(context, 16844099);
    }

    public static int getColorStateListDefaultColor(Context context, int i) {
        return context.getResources().getColorStateList(i, context.getTheme()).getDefaultColor();
    }

    public static int getDisabled(Context context, int i) {
        return applyAlphaAttr(context, 16842803, i);
    }

    public static int applyAlphaAttr(Context context, int i, int i2) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        float f = obtainStyledAttributes.getFloat(0, 0.0f);
        obtainStyledAttributes.recycle();
        return applyAlpha(f, i2);
    }

    public static int applyAlpha(float f, int i) {
        return Color.argb((int) (f * ((float) Color.alpha(i))), Color.red(i), Color.green(i), Color.blue(i));
    }

    public static int getColorAttrDefaultColor(Context context, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = obtainStyledAttributes.getColor(0, 0);
        obtainStyledAttributes.recycle();
        return color;
    }

    public static ColorStateList getColorAttr(Context context, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        try {
            return obtainStyledAttributes.getColorStateList(0);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }

    public static int getThemeAttr(Context context, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int resourceId = obtainStyledAttributes.getResourceId(0, 0);
        obtainStyledAttributes.recycle();
        return resourceId;
    }

    public static Drawable getDrawable(Context context, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        Drawable drawable = obtainStyledAttributes.getDrawable(0);
        obtainStyledAttributes.recycle();
        return drawable;
    }

    public static boolean isSystemPackage(Resources resources, PackageManager packageManager, PackageInfo packageInfo) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(packageManager)};
        }
        if (sPermissionControllerPackageName == null) {
            sPermissionControllerPackageName = packageManager.getPermissionControllerPackageName();
        }
        if (sServicesSystemSharedLibPackageName == null) {
            sServicesSystemSharedLibPackageName = packageManager.getServicesSystemSharedLibraryPackageName();
        }
        if (sSharedSystemSharedLibPackageName == null) {
            sSharedSystemSharedLibPackageName = packageManager.getSharedSystemSharedLibraryPackageName();
        }
        Signature[] signatureArr = sSystemSignature;
        if ((signatureArr[0] == null || !signatureArr[0].equals(getFirstSignature(packageInfo))) && !packageInfo.packageName.equals(sPermissionControllerPackageName) && !packageInfo.packageName.equals(sServicesSystemSharedLibPackageName) && !packageInfo.packageName.equals(sSharedSystemSharedLibPackageName) && !packageInfo.packageName.equals("com.android.printspooler") && !isDeviceProvisioningPackage(resources, packageInfo.packageName)) {
            return false;
        }
        return true;
    }

    private static Signature getFirstSignature(PackageInfo packageInfo) {
        if (packageInfo != null) {
            Signature[] signatureArr = packageInfo.signatures;
            if (signatureArr != null && signatureArr.length > 0) {
                return signatureArr[0];
            }
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager packageManager) {
        try {
            return getFirstSignature(packageManager.getPackageInfo("android", 64));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isDeviceProvisioningPackage(Resources resources, String str) {
        String string = resources.getString(17039728);
        return string != null && string.equals(str);
    }

    public static boolean isInService(ServiceState serviceState) {
        if (serviceState == null) {
            return false;
        }
        int combinedServiceState = getCombinedServiceState(serviceState);
        return (combinedServiceState == 3 || combinedServiceState == 1 || combinedServiceState == 2) ? false : true;
    }

    public static int getCombinedServiceState(ServiceState serviceState) {
        if (serviceState == null) {
            return 1;
        }
        int state = serviceState.getState();
        int dataRegState = serviceState.getDataRegState();
        if ((state == 1 || state == 2) && dataRegState == 0 && serviceState.getDataNetworkType() != 18) {
            return 0;
        }
        return state;
    }

    public static String formatFileSize(Context context, long j) {
        String formatFileSize = Formatter.formatFileSize(context, j);
        if (VERSION.SDK_INT > 26) {
            Locale locale = Locale.getDefault();
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language.equalsIgnoreCase("zh") && country.equalsIgnoreCase("CN")) {
                int i = 0;
                while (true) {
                    String[] strArr = UNIT_OF_STORAGE;
                    if (i >= strArr.length) {
                        break;
                    }
                    formatFileSize = formatFileSize.replaceAll(strArr[i], UNIT_OF_STORAGE_REPLACE[i]);
                    i++;
                }
            }
        }
        return formatFileSize;
    }
}
