package com.oneplus.systemui.power;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;
import com.android.systemui.R$string;
import com.android.systemui.power.PowerNotificationWarnings;
import com.oneplus.util.OpReflectionUtils;
import java.text.NumberFormat;

public class OpPowerNotificationWarnings {
    protected static final boolean OP_DEBUG = OpPowerUI.OP_DEBUG;

    public long computePowerSaveExtendTime(long j) {
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(getContext(), true);
        batteryStatsHelper.create(null);
        long computeBatteryTimeRemaining = batteryStatsHelper.getStats().computeBatteryTimeRemaining(j);
        if (computeBatteryTimeRemaining == -1) {
            return -1;
        }
        return (computeBatteryTimeRemaining * 12) / 100;
    }

    /* access modifiers changed from: protected */
    public boolean isWarningNotificationShow() {
        return getWarning();
    }

    private int getBatteryLevel() {
        return ((Integer) OpReflectionUtils.getValue(PowerNotificationWarnings.class, this, "mBatteryLevel")).intValue();
    }

    /* access modifiers changed from: protected */
    public String getBatteryLowDescription() {
        String str;
        Context context = getContext();
        long convertUsToMs = PowerUtil.convertUsToMs(computePowerSaveExtendTime(SystemClock.elapsedRealtime()));
        if (convertUsToMs <= 0 || convertUsToMs < 600000) {
            String format = NumberFormat.getPercentInstance().format(((double) getBatteryLevel()) / 100.0d);
            str = context.getString(R$string.battery_low_warning, new Object[]{format});
        } else {
            str = context.getString(R$string.battery_usage_extanded, new Object[]{getFormattedTime(convertUsToMs)});
        }
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("getBatteryLowDescription extendTime=");
            sb.append(convertUsToMs);
            sb.append(" str=");
            sb.append(str);
            Log.i("OpPowerUI.Notification", sb.toString());
        }
        return str;
    }

    private Context getContext() {
        return (Context) OpReflectionUtils.getValue(PowerNotificationWarnings.class, this, "mContext");
    }

    private CharSequence getFormattedTime(long j) {
        return StringUtil.formatElapsedTime(getContext(), (double) j, false);
    }

    private boolean getWarning() {
        return ((Boolean) OpReflectionUtils.getValue(PowerNotificationWarnings.class, this, "mWarning")).booleanValue();
    }
}
