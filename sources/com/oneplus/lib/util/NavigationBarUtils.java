package com.oneplus.lib.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;

public class NavigationBarUtils {
    private static boolean isGesturalEnabled(Context context) {
        return Secure.getInt(context.getContentResolver(), "navigation_mode", 0) == 2;
    }

    public static void setNavBarColor(Activity activity) {
        if (isGesturalEnabled(activity) && VERSION.SDK_INT > 28) {
            activity.getWindow().setNavigationBarColor(activity.getResources().getColor(17170445));
        }
    }
}
