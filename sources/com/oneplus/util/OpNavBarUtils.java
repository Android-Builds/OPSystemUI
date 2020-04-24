package com.oneplus.util;

import android.content.Context;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.OpFeatures;
import com.android.systemui.R$string;

public class OpNavBarUtils {
    public static boolean isSupportCustomKeys() {
        return true;
    }

    public static boolean isKeySwapped(Context context) {
        try {
            return System.getInt(context.getContentResolver(), "oem_acc_key_define") == 1;
        } catch (SettingNotFoundException unused) {
            return true;
        }
    }

    public static boolean isBackKeyRight(Context context) {
        return (OpUtils.isGlobalROM(context) && isKeySwapped(context)) || (!OpUtils.isGlobalROM(context) && !isKeySwapped(context));
    }

    public static boolean isSupportCustomNavBar() {
        return OpFeatures.isSupport(new int[]{69});
    }

    public static boolean isSupportHideNavBar() {
        return OpFeatures.isSupport(new int[]{40});
    }

    public static int getNavBarLayout(Context context, boolean z) {
        int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(context.getResources().getConfiguration().locale);
        boolean isBackKeyRight = isBackKeyRight(context);
        int i = R$string.oneplus_config_navBarLayout;
        int i2 = R$string.oneplus_config_navBarLayout_RTL;
        int i3 = R$string.oneplus_config_navBarLayoutQuickstep;
        int i4 = R$string.oneplus_config_navBarLayoutQuickstep_RTL;
        if (z) {
            i = i3;
        }
        if (z) {
            i2 = i4;
        }
        return isBackKeyRight ? layoutDirectionFromLocale == 0 ? i2 : i : layoutDirectionFromLocale == 1 ? i2 : i;
    }
}
