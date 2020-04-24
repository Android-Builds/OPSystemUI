package com.oneplus.systemui.power;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Slog;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Prefs;
import com.android.systemui.R$integer;
import com.android.systemui.SystemUI;
import com.android.systemui.power.BatteryStateSnapshot;
import com.android.systemui.power.PowerUI;
import com.android.systemui.power.PowerUI.WarningsUI;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import java.time.Duration;

public class OpPowerUI extends SystemUI {
    protected static final boolean OP_DEBUG = OpUtils.DEBUG_ONEPLUS;
    private static final long SIX_HOURS_MILLIS = Duration.ofHours(6).toMillis();
    private final boolean DEBUG;
    protected final int SOFT_REMIND_BUCKET;
    private boolean mCurrentPowerSave;
    /* access modifiers changed from: protected */
    public final int[] mLowBatteryReminderLevels;
    /* access modifiers changed from: private */
    public int mScreenTimeout;
    /* access modifiers changed from: private */
    public boolean mSelfChange;
    /* access modifiers changed from: private */
    public boolean mSelfChangeRestore;
    /* access modifiers changed from: private */
    public int mUser;

    protected class OpReceiver extends BroadcastReceiver {
        protected OpReceiver() {
        }

        public void init(IntentFilter intentFilter) {
            intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        }
    }

    protected interface OpWarningsUI {
        boolean isWarningNotificationShow();
    }

    public OpPowerUI() {
        this.DEBUG = Log.isLoggable("OpPowerUI", 3) || OP_DEBUG;
        this.mLowBatteryReminderLevels = new int[3];
        this.SOFT_REMIND_BUCKET = -2;
        this.mScreenTimeout = 0;
        this.mUser = 0;
        this.mCurrentPowerSave = false;
        this.mSelfChange = false;
        this.mSelfChangeRestore = false;
    }

    public void start() {
        this.mUser = KeyguardUpdateMonitor.getCurrentUser();
        restoreScreenTimeoutFromPrefsIfNeeded();
        this.mScreenTimeout = System.getIntForUser(this.mContext.getContentResolver(), "screen_off_timeout", 30000, this.mUser);
    }

    /* access modifiers changed from: protected */
    public void maybeShowBatteryWarning(BatteryStateSnapshot batteryStateSnapshot, BatteryStateSnapshot batteryStateSnapshot2) {
        WarningsUI warnings = getWarnings();
        boolean z = !(batteryStateSnapshot.getBucket() == batteryStateSnapshot2.getBucket() || batteryStateSnapshot.getBucket() == -2) || batteryStateSnapshot2.getPlugged();
        if (shouldShowLowBatteryWarningInternal(batteryStateSnapshot, batteryStateSnapshot2)) {
            warnings.showLowBatteryWarning(z);
        } else if (shouldDismissLowBatteryWarning(batteryStateSnapshot, batteryStateSnapshot2)) {
            warnings.dismissLowBatteryWarning();
        } else if (batteryStateSnapshot2.getBucket() != batteryStateSnapshot.getBucket()) {
            warnings.updateLowBatteryWarning();
        }
    }

    /* access modifiers changed from: protected */
    public void maybeShowHybridWarningInternal(BatteryStateSnapshot batteryStateSnapshot, BatteryStateSnapshot batteryStateSnapshot2) {
        WarningsUI warnings = getWarnings();
        boolean z = false;
        String str = "OpPowerUI";
        if (batteryStateSnapshot.getBatteryLevel() >= 45 && batteryStateSnapshot.getTimeRemainingMillis() > SIX_HOURS_MILLIS) {
            setLowWarningShownThisChargeCycle(false);
            setSevereWarningShownThisChargeCycle(false);
            if (this.DEBUG) {
                Slog.d(str, "Charge cycle reset! Can show warnings again");
            }
        }
        if (!(batteryStateSnapshot.getBucket() == batteryStateSnapshot2.getBucket() || batteryStateSnapshot.getBucket() == -2) || batteryStateSnapshot2.getPlugged()) {
            z = true;
        }
        if (shouldShowHybridWarning(batteryStateSnapshot)) {
            warnings.showLowBatteryWarning(z);
            if (batteryStateSnapshot.getTimeRemainingMillis() <= batteryStateSnapshot.getSevereThresholdMillis() || batteryStateSnapshot.getBatteryLevel() <= batteryStateSnapshot.getSevereLevelThreshold()) {
                setSevereWarningShownThisChargeCycle(true);
                setLowWarningShownThisChargeCycle(true);
                if (this.DEBUG) {
                    Slog.d(str, "Severe warning marked as shown this cycle");
                    return;
                }
                return;
            }
            Slog.d(str, "Low warning marked as shown this cycle");
            setLowWarningShownThisChargeCycle(true);
        } else if (shouldDismissHybridWarning(batteryStateSnapshot)) {
            if (this.DEBUG) {
                Slog.d(str, "Dismissing warning");
            }
            warnings.dismissLowBatteryWarning();
        } else {
            if (this.DEBUG) {
                Slog.d(str, "Updating warning");
            }
            warnings.updateLowBatteryWarning();
        }
    }

    /* access modifiers changed from: protected */
    public void registerObserverInternal(ContentResolver contentResolver, Handler handler, final PowerManager powerManager) {
        contentResolver.registerContentObserver(System.getUriFor("screen_off_timeout"), false, new ContentObserver(handler) {
            public void onChange(boolean z, Uri uri) {
                String str = "screen_off_timeout";
                if (System.getUriFor(str).equals(uri)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onChange:SCREEN_OFF_TIMEOUT:mSelfChange=");
                    sb.append(OpPowerUI.this.mSelfChange);
                    sb.append(", mSelfChangeRestore=");
                    sb.append(OpPowerUI.this.mSelfChangeRestore);
                    String str2 = "OpPowerUI";
                    Log.d(str2, sb.toString());
                    if (!powerManager.isPowerSaveMode()) {
                        return;
                    }
                    if (OpPowerUI.this.mSelfChange) {
                        OpPowerUI.this.mSelfChange = false;
                    } else if (OpPowerUI.this.mSelfChangeRestore) {
                        OpPowerUI.this.mSelfChangeRestore = false;
                    } else {
                        OpPowerUI opPowerUI = OpPowerUI.this;
                        opPowerUI.mScreenTimeout = System.getIntForUser(opPowerUI.mContext.getContentResolver(), str, 30000, OpPowerUI.this.mUser);
                        OpPowerUI.this.saveScreenTimeoutToPrefs(0);
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("SettingsObserver:onChange:User changed the timeout during power saving mode: mScreenTimeout=");
                        sb2.append(OpPowerUI.this.mScreenTimeout);
                        Log.d(str2, sb2.toString());
                    }
                }
            }
        }, -1);
    }

    /* access modifiers changed from: protected */
    public void restorePowerSavingSettingsForUser() {
        this.mSelfChangeRestore = true;
        System.putIntForUser(this.mContext.getContentResolver(), "screen_off_timeout", this.mScreenTimeout, this.mUser);
        StringBuilder sb = new StringBuilder();
        sb.append("restorePowerSavingSettingsForUser:mScreenTimeout=");
        sb.append(this.mScreenTimeout);
        sb.append(", user=");
        sb.append(this.mUser);
        Log.d("OpPowerUI", sb.toString());
    }

    private void restoreScreenTimeoutFromPrefsIfNeeded() {
        int i = Prefs.getInt(this.mContext, "PowerSavingTimeoutBackup", 0);
        if (i > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("restoreScreenTimeoutFromPrefsIfNeeded:");
            sb.append(i);
            Log.d("OpPowerUI", sb.toString());
            System.putIntForUser(this.mContext.getContentResolver(), "screen_off_timeout", i, this.mUser);
            saveScreenTimeoutToPrefs(0);
        }
    }

    /* access modifiers changed from: private */
    public void saveScreenTimeoutToPrefs(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("saveScreenTimeoutToPrefs:");
        sb.append(i);
        Log.d("OpPowerUI", sb.toString());
        Prefs.putInt(this.mContext, "PowerSavingTimeoutBackup", i);
    }

    private boolean shouldDismissHybridWarning(BatteryStateSnapshot batteryStateSnapshot) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(PowerUI.class, "shouldDismissHybridWarning", BatteryStateSnapshot.class), batteryStateSnapshot)).booleanValue();
    }

    private boolean shouldDismissLowBatteryWarning(BatteryStateSnapshot batteryStateSnapshot, BatteryStateSnapshot batteryStateSnapshot2) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(PowerUI.class, "shouldDismissLowBatteryWarning", BatteryStateSnapshot.class, BatteryStateSnapshot.class), batteryStateSnapshot, batteryStateSnapshot2)).booleanValue();
    }

    private boolean shouldShowHybridWarning(BatteryStateSnapshot batteryStateSnapshot) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(PowerUI.class, "shouldShowHybridWarning", BatteryStateSnapshot.class), batteryStateSnapshot)).booleanValue();
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowLowBatteryWarningInternal(BatteryStateSnapshot batteryStateSnapshot, BatteryStateSnapshot batteryStateSnapshot2) {
        boolean z = !getWarnings().isWarningNotificationShow() && batteryStateSnapshot.getBucket() == -2 && batteryStateSnapshot2.getBucket() == -1;
        if (batteryStateSnapshot.getPlugged() || batteryStateSnapshot.isPowerSaver()) {
            return false;
        }
        if (((batteryStateSnapshot.getBucket() >= batteryStateSnapshot2.getBucket() || z) && !batteryStateSnapshot2.getPlugged()) || batteryStateSnapshot.getBucket() >= 0 || batteryStateSnapshot.getBatteryStatus() == 1) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void updateBatteryWarningLevelsInternal() {
        int integer = this.mContext.getResources().getInteger(17694765);
        int integer2 = this.mContext.getResources().getInteger(17694833);
        if (integer2 < integer) {
            integer2 = integer;
        }
        int[] iArr = this.mLowBatteryReminderLevels;
        iArr[0] = integer2;
        iArr[1] = this.mContext.getResources().getInteger(R$integer.config_lowBatteryWarningLevel_1);
        int[] iArr2 = this.mLowBatteryReminderLevels;
        iArr2[2] = integer;
        setLowBatteryAlertCloseLevel(iArr2[0] + this.mContext.getResources().getInteger(17694832));
    }

    /* access modifiers changed from: protected */
    public void updatePowerSavingSettings(boolean z) {
        this.mUser = KeyguardUpdateMonitor.getCurrentUser();
        this.mSelfChange = true;
        if (z != this.mCurrentPowerSave) {
            this.mCurrentPowerSave = z;
            String str = ", user=";
            String str2 = "OpPowerUI";
            String str3 = "screen_off_timeout";
            if (z) {
                this.mScreenTimeout = System.getIntForUser(this.mContext.getContentResolver(), str3, 30000, this.mUser);
                saveScreenTimeoutToPrefs(this.mScreenTimeout);
                System.putIntForUser(this.mContext.getContentResolver(), str3, 30000, this.mUser);
                StringBuilder sb = new StringBuilder();
                sb.append("updatePowerSavingSettings:Enter PowerSaving Mode: mScreenTimeout=");
                sb.append(this.mScreenTimeout);
                sb.append(str);
                sb.append(this.mUser);
                Log.d(str2, sb.toString());
                return;
            }
            System.putIntForUser(this.mContext.getContentResolver(), str3, this.mScreenTimeout, this.mUser);
            saveScreenTimeoutToPrefs(0);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("updatePowerSavingSettings:Leave PowerSaving Mode: mScreenTimeout=");
            sb2.append(this.mScreenTimeout);
            sb2.append(str);
            sb2.append(this.mUser);
            Log.d(str2, sb2.toString());
        }
    }

    private void setLowBatteryAlertCloseLevel(int i) {
        OpReflectionUtils.setValue(PowerUI.class, this, "mLowBatteryAlertCloseLevel", Integer.valueOf(i));
    }

    private void setLowWarningShownThisChargeCycle(boolean z) {
        OpReflectionUtils.setValue(PowerUI.class, this, "mLowWarningShownThisChargeCycle", Boolean.valueOf(z));
    }

    private void setSevereWarningShownThisChargeCycle(boolean z) {
        OpReflectionUtils.setValue(PowerUI.class, this, "mSevereWarningShownThisChargeCycle", Boolean.valueOf(z));
    }

    private WarningsUI getWarnings() {
        return (WarningsUI) OpReflectionUtils.getValue(PowerUI.class, this, "mWarnings");
    }
}
