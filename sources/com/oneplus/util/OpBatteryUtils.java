package com.oneplus.util;

import com.android.systemui.R$drawable;

public class OpBatteryUtils {
    public static int getDashImageResId(int i) {
        if (i == 0) {
            return R$drawable.op_dash_charging_state_0;
        }
        if (i > 0 && i <= 10) {
            return R$drawable.op_dash_charging_state_10;
        }
        if (i > 10 && i <= 20) {
            return R$drawable.op_dash_charging_state_20;
        }
        if (i > 20 && i <= 30) {
            return R$drawable.op_dash_charging_state_30;
        }
        if (i > 30 && i <= 40) {
            return R$drawable.op_dash_charging_state_40;
        }
        if (i > 40 && i <= 50) {
            return R$drawable.op_dash_charging_state_50;
        }
        if (i > 50 && i <= 60) {
            return R$drawable.op_dash_charging_state_60;
        }
        if (i > 60 && i <= 70) {
            return R$drawable.op_dash_charging_state_70;
        }
        if (i > 70 && i <= 80) {
            return R$drawable.op_dash_charging_state_80;
        }
        if (i > 80 && i <= 90) {
            return R$drawable.op_dash_charging_state_90;
        }
        if (i <= 90 || i > 100) {
            return 0;
        }
        return R$drawable.op_dash_charging_state_100;
    }
}
