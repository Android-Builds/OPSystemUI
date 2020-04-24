package com.oneplus.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import com.android.systemui.R$array;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.android.systemui.R$style;

public class ThemeColorUtils {
    public static int QS_ACCENT = 100;
    public static int QS_BATTERY_BACKGROUND = 8;
    public static int QS_BUTTON = 7;
    public static int QS_EDIT_BOTTOM = 12;
    public static int QS_INDICATOR = 9;
    public static int QS_PANEL_PRIMARY = 5;
    public static int QS_PRIMARY_TEXT = 1;
    public static int QS_PROGRESS_BACKGROUND = 11;
    public static int QS_SECONDARY_TEXT = 2;
    public static int QS_SEPARATOR = 14;
    public static int QS_TEXT = 0;
    public static int QS_TILE_CIRCLE_DISABLE = 16;
    public static int QS_TILE_CIRCLE_OFF = 15;
    public static int QS_TILE_DISABLE = 4;
    public static int QS_TILE_OFF = 3;
    public static int QS_WLB_INDICATOR = 17;
    private static int sAccentColor = 0;
    private static int[] sColors = null;
    private static int sCurrentTheme = -1;
    private static boolean sSpecialTheme;
    private static String[] sThemeName;

    public static void init(Context context) {
        Resources resources = context.getResources();
        if (sCurrentTheme == -1) {
            sThemeName = resources.getStringArray(R$array.qs_theme_colors);
        }
        int themeColor = OpUtils.getThemeColor(context);
        boolean isGoogleDarkTheme = OpUtils.isGoogleDarkTheme(context);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("isGoogleDark=");
            sb.append(isGoogleDarkTheme);
            sb.append(", opTheme=");
            sb.append(themeColor);
            Log.d("ThemeColorUtils", sb.toString());
        }
        if (isGoogleDarkTheme) {
            themeColor = 1;
        }
        boolean isSpecialTheme = OpUtils.isSpecialTheme(context);
        if (!(sCurrentTheme == themeColor && sSpecialTheme == isSpecialTheme)) {
            sCurrentTheme = themeColor;
            sSpecialTheme = isSpecialTheme;
            sColors = resources.getIntArray(resources.getIdentifier(sThemeName[sCurrentTheme], null, "com.android.systemui"));
        }
        updateAccentColor(context);
    }

    private static void updateAccentColor(Context context) {
        int themeAccentColor = OpUtils.getThemeAccentColor(context, R$color.qs_tile_icon);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateAccentColor: accentColor=");
            sb.append(themeAccentColor);
            Log.d("ThemeColorUtils", sb.toString());
        }
        sAccentColor = themeAccentColor;
    }

    public static int getColor(int i) {
        if (i == QS_ACCENT) {
            return sAccentColor;
        }
        return sColors[i];
    }

    public static int getEditTheme() {
        int i = sCurrentTheme;
        if (i == 0) {
            return R$style.op_edit_theme_light;
        }
        if (i == 1) {
            return R$style.op_edit_theme_dark;
        }
        if (i != 2) {
            return R$style.op_edit_theme_android;
        }
        return R$style.op_edit_theme_android;
    }

    public static int getPopTheme() {
        int i = sCurrentTheme;
        if (i == 0) {
            return R$style.edit_poptheme_white;
        }
        if (i == 1) {
            return R$style.edit_poptheme_black;
        }
        if (i != 2) {
            return R$style.edit_poptheme_android;
        }
        return R$style.edit_poptheme_android;
    }

    public static int getThumbBackground() {
        int i = sCurrentTheme;
        if (i == 0) {
            return R$drawable.ripple_background_white;
        }
        if (i == 1) {
            return R$drawable.ripple_background_dark;
        }
        if (i != 2) {
            return R$drawable.ripple_background_dark;
        }
        return R$drawable.ripple_background_dark;
    }

    public static int getCurrentTheme() {
        return sCurrentTheme;
    }
}
