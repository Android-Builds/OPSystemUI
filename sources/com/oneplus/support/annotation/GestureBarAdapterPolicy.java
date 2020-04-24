package com.oneplus.support.annotation;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings.Secure;

public class GestureBarAdapterPolicy extends ContentObserver {
    private static final Uri URI = Uri.parse("content://settings/secure/navigation_mode");

    public static boolean gestureButtonEnabled(Context context) {
        return Secure.getInt(context.getContentResolver(), "navigation_mode", 0) == 2;
    }

    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        return resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));
    }
}
