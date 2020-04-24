package com.oneplus.lib.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public abstract class AppUtils {
    private static final String TAG = "AppUtils";

    public static int getSystemIntegerRes(Context context, String str) {
        Resources resources = context.getResources();
        int identifier = resources.getIdentifier(str, "integer", "android");
        if (identifier != 0) {
            return resources.getInteger(identifier);
        }
        Log.e(TAG, "Failed to get system resource ID. Incompatible framework version?");
        return -1;
    }

    public static boolean gestureButtonEnabled(Context context) {
        return 2 == getSystemIntegerRes(context, "config_navBarInteractionMode");
    }
}
