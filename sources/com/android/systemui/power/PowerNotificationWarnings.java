package com.android.systemui.power;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.utils.PowerUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter.Callback;
import com.android.systemui.power.PowerUI.WarningsUI;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.volume.Events;
import com.oneplus.systemui.power.OpPowerNotificationWarnings;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class PowerNotificationWarnings extends OpPowerNotificationWarnings implements WarningsUI {
    private static final AudioAttributes AUDIO_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private static final boolean DEBUG = PowerUI.DEBUG;
    private static final String[] SHOWING_STRINGS = {"SHOWING_NOTHING", "SHOWING_WARNING", "SHOWING_SAVER", "SHOWING_INVALID_CHARGER", "SHOWING_AUTO_SAVER_SUGGESTION"};
    private ActivityStarter mActivityStarter;
    private int mBatteryLevel;
    private int mBucket;
    /* access modifiers changed from: private */
    public final Context mContext;
    private BatteryStateSnapshot mCurrentBatterySnapshot;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper());
    private SystemUIDialog mHighTempDialog;
    private boolean mHighTempWarning;
    private boolean mInvalidCharger;
    private final KeyguardManager mKeyguard;
    private final NotificationManager mNoMan;
    /* access modifiers changed from: private */
    public final Intent mOpenBatterySettings = settings("android.intent.action.POWER_USAGE_SUMMARY");
    private boolean mPlaySound;
    private final PowerManager mPowerMan;
    private final Receiver mReceiver = new Receiver();
    /* access modifiers changed from: private */
    public SystemUIDialog mSaverConfirmation;
    private long mScreenOffTime;
    private boolean mShowAutoSaverSuggestion;
    private int mShowing;
    private SystemUIDialog mThermalShutdownDialog;
    SystemUIDialog mUsbHighTempDialog;
    private boolean mWarning;
    private long mWarningTriggerTimeMs;

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("PNW.batterySettings");
            intentFilter.addAction("PNW.startSaver");
            intentFilter.addAction("PNW.dismissedWarning");
            intentFilter.addAction("PNW.clickedTempWarning");
            intentFilter.addAction("PNW.dismissedTempWarning");
            intentFilter.addAction("PNW.clickedThermalShutdownWarning");
            intentFilter.addAction("PNW.dismissedThermalShutdownWarning");
            intentFilter.addAction("PNW.startSaverConfirmation");
            intentFilter.addAction("PNW.autoSaverSuggestion");
            intentFilter.addAction("PNW.enableAutoSaver");
            intentFilter.addAction("PNW.autoSaverNoThanks");
            intentFilter.addAction("PNW.dismissAutoSaverSuggestion");
            PowerNotificationWarnings.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, "android.permission.DEVICE_POWER", PowerNotificationWarnings.this.mHandler);
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            StringBuilder sb = new StringBuilder();
            sb.append("Received ");
            sb.append(action);
            Slog.i("PowerUI.Notification", sb.toString());
            if (action.equals("PNW.batterySettings")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.mContext.startActivityAsUser(PowerNotificationWarnings.this.mOpenBatterySettings, UserHandle.CURRENT);
            } else if (action.equals("PNW.startSaver")) {
                PowerNotificationWarnings.this.setSaverMode(true, true);
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
            } else if (action.equals("PNW.startSaverConfirmation")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.showStartSaverConfirmation(intent.getExtras());
            } else if (action.equals("PNW.dismissedWarning")) {
                PowerNotificationWarnings.this.dismissLowBatteryWarning();
            } else if ("PNW.clickedTempWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissHighTemperatureWarningInternal();
                PowerNotificationWarnings.this.showHighTemperatureDialog();
            } else if ("PNW.dismissedTempWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissHighTemperatureWarningInternal();
            } else if ("PNW.clickedThermalShutdownWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissThermalShutdownWarning();
                PowerNotificationWarnings.this.showThermalShutdownDialog();
            } else if ("PNW.dismissedThermalShutdownWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissThermalShutdownWarning();
            } else if ("PNW.autoSaverSuggestion".equals(action)) {
                PowerNotificationWarnings.this.showAutoSaverSuggestion();
            } else if ("PNW.dismissAutoSaverSuggestion".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
            } else if ("PNW.enableAutoSaver".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
                PowerNotificationWarnings.this.startBatterySaverSchedulePage();
            } else if ("PNW.autoSaverNoThanks".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
                BatterySaverUtils.suppressAutoBatterySaver(context);
            }
        }
    }

    public PowerNotificationWarnings(Context context, ActivityStarter activityStarter) {
        this.mContext = context;
        this.mNoMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mPowerMan = (PowerManager) context.getSystemService("power");
        this.mKeyguard = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
        this.mReceiver.init();
        this.mActivityStarter = activityStarter;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.print("mWarning=");
        printWriter.println(this.mWarning);
        printWriter.print("mPlaySound=");
        printWriter.println(this.mPlaySound);
        printWriter.print("mInvalidCharger=");
        printWriter.println(this.mInvalidCharger);
        printWriter.print("mShowing=");
        printWriter.println(SHOWING_STRINGS[this.mShowing]);
        printWriter.print("mSaverConfirmation=");
        String str = "not null";
        printWriter.println(this.mSaverConfirmation != null ? str : null);
        printWriter.print("mSaverEnabledConfirmation=");
        printWriter.print("mHighTempWarning=");
        printWriter.println(this.mHighTempWarning);
        printWriter.print("mHighTempDialog=");
        printWriter.println(this.mHighTempDialog != null ? str : null);
        printWriter.print("mThermalShutdownDialog=");
        printWriter.println(this.mThermalShutdownDialog != null ? str : null);
        printWriter.print("mUsbHighTempDialog=");
        if (this.mUsbHighTempDialog == null) {
            str = null;
        }
        printWriter.println(str);
    }

    public void update(int i, int i2, long j) {
        this.mBatteryLevel = i;
        if (i2 >= 0) {
            this.mWarningTriggerTimeMs = 0;
        } else if (i2 < this.mBucket) {
            this.mWarningTriggerTimeMs = System.currentTimeMillis();
        }
        this.mBucket = i2;
        this.mScreenOffTime = j;
    }

    public void updateSnapshot(BatteryStateSnapshot batteryStateSnapshot) {
        this.mCurrentBatterySnapshot = batteryStateSnapshot;
    }

    private void updateNotification() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateNotification mWarning=");
            sb.append(this.mWarning);
            sb.append(" mPlaySound=");
            sb.append(this.mPlaySound);
            sb.append(" mInvalidCharger=");
            sb.append(this.mInvalidCharger);
            Slog.d("PowerUI.Notification", sb.toString());
        }
        if (this.mInvalidCharger) {
            showInvalidChargerNotification();
            this.mShowing = 3;
        } else if (this.mWarning) {
            showWarningNotification();
            this.mShowing = 1;
        } else if (this.mShowAutoSaverSuggestion) {
            if (this.mShowing != 4) {
                showAutoSaverSuggestionNotification();
            }
            this.mShowing = 4;
        } else {
            String str = "low_battery";
            this.mNoMan.cancelAsUser(str, 2, UserHandle.ALL);
            this.mNoMan.cancelAsUser(str, 3, UserHandle.ALL);
            this.mNoMan.cancelAsUser("auto_saver", 49, UserHandle.ALL);
            this.mShowing = 0;
        }
    }

    private void showInvalidChargerNotification() {
        Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(R$drawable.ic_power_low).setWhen(0).setShowWhen(false).setOngoing(true).setContentTitle(this.mContext.getString(R$string.invalid_charger_title)).setContentText(this.mContext.getString(R$string.invalid_charger_text)).setColor(this.mContext.getColor(17170460));
        SystemUI.overrideNotificationAppName(this.mContext, color, false);
        Notification build = color.build();
        String str = "low_battery";
        this.mNoMan.cancelAsUser(str, 3, UserHandle.ALL);
        this.mNoMan.notifyAsUser(str, 2, build, UserHandle.ALL);
    }

    /* access modifiers changed from: protected */
    public void showWarningNotification() {
        String str;
        String format = NumberFormat.getPercentInstance().format(((double) this.mCurrentBatterySnapshot.getBatteryLevel()) / 100.0d);
        String string = this.mContext.getString(R$string.battery_low_title);
        if (this.mCurrentBatterySnapshot.isHybrid()) {
            str = getHybridContentString(format);
        } else {
            str = getBatteryLowDescription();
        }
        Notification.Builder visibility = new Notification.Builder(this.mContext, !this.mPlaySound ? NotificationChannels.HINTS : NotificationChannels.BATTERY).setSmallIcon(R$drawable.ic_power_low).setWhen(this.mWarningTriggerTimeMs).setShowWhen(false).setContentText(str).setContentTitle(string).setOnlyAlertOnce(true).setDeleteIntent(pendingBroadcast("PNW.dismissedWarning")).setStyle(new BigTextStyle().bigText(str)).setVisibility(1);
        Bundle bundle = new Bundle();
        bundle.putBoolean("oneplus.shouldPeekInGameMode", true);
        bundle.putBoolean("oneplus.shouldPeekInCarMode", true);
        visibility.setExtras(bundle);
        visibility.setLargeIcon(Icon.createWithResource(this.mContext, R$drawable.ic_notif_low_battery));
        if (hasBatterySettings()) {
            visibility.setContentIntent(pendingBroadcast("PNW.batterySettings"));
        }
        if (!this.mCurrentBatterySnapshot.isHybrid() || this.mBucket < 0 || this.mCurrentBatterySnapshot.getTimeRemainingMillis() < this.mCurrentBatterySnapshot.getSevereThresholdMillis()) {
            visibility.setColor(Utils.getColorAttrDefaultColor(this.mContext, 16844099));
        }
        if (!this.mPowerMan.isPowerSaveMode()) {
            visibility.addAction(0, this.mContext.getString(R$string.battery_saver_start_action), pendingBroadcast("PNW.startSaver"));
        }
        visibility.setOnlyAlertOnce(!this.mPlaySound);
        this.mPlaySound = false;
        SystemUI.overrideNotificationAppName(this.mContext, visibility, false);
        Notification build = visibility.build();
        String str2 = "low_battery";
        this.mNoMan.cancelAsUser(str2, 2, UserHandle.ALL);
        this.mNoMan.notifyAsUser(str2, 3, build, UserHandle.ALL);
    }

    private void showAutoSaverSuggestionNotification() {
        Notification.Builder contentText = new Notification.Builder(this.mContext, NotificationChannels.HINTS).setSmallIcon(R$drawable.ic_power_saver).setWhen(0).setShowWhen(false).setContentTitle(this.mContext.getString(R$string.auto_saver_title)).setContentText(this.mContext.getString(R$string.auto_saver_text));
        contentText.setContentIntent(pendingBroadcast("PNW.enableAutoSaver"));
        contentText.setDeleteIntent(pendingBroadcast("PNW.dismissAutoSaverSuggestion"));
        contentText.addAction(0, this.mContext.getString(R$string.no_auto_saver_action), pendingBroadcast("PNW.autoSaverNoThanks"));
        SystemUI.overrideNotificationAppName(this.mContext, contentText, false);
        String str = "auto_saver";
        this.mNoMan.notifyAsUser(str, 49, contentText.build(), UserHandle.ALL);
    }

    private String getHybridContentString(String str) {
        return PowerUtil.getBatteryRemainingStringFormatted(this.mContext, this.mCurrentBatterySnapshot.getTimeRemainingMillis(), str, this.mCurrentBatterySnapshot.isBasedOnUsage());
    }

    private PendingIntent pendingBroadcast(String str) {
        return PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(str).setPackage(this.mContext.getPackageName()).setFlags(268435456), 0, UserHandle.CURRENT);
    }

    private static Intent settings(String str) {
        return new Intent(str).setFlags(1551892480);
    }

    public boolean isInvalidChargerWarningShowing() {
        return this.mInvalidCharger;
    }

    public void dismissHighTemperatureWarning() {
        if (this.mHighTempWarning) {
            dismissHighTemperatureWarningInternal();
        }
    }

    /* access modifiers changed from: private */
    public void dismissHighTemperatureWarningInternal() {
        this.mNoMan.cancelAsUser("high_temp", 4, UserHandle.ALL);
        this.mHighTempWarning = false;
    }

    public void showHighTemperatureWarning() {
        if (!this.mHighTempWarning) {
            this.mHighTempWarning = true;
            Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(R$drawable.ic_device_thermostat_24).setWhen(0).setShowWhen(false).setContentTitle(this.mContext.getString(R$string.high_temp_title)).setContentText(this.mContext.getString(R$string.high_temp_notif_message)).setVisibility(1).setContentIntent(pendingBroadcast("PNW.clickedTempWarning")).setDeleteIntent(pendingBroadcast("PNW.dismissedTempWarning")).setColor(Utils.getColorAttrDefaultColor(this.mContext, 16844099));
            SystemUI.overrideNotificationAppName(this.mContext, color, false);
            String str = "high_temp";
            this.mNoMan.notifyAsUser(str, 4, color.build(), UserHandle.ALL);
        }
    }

    /* access modifiers changed from: private */
    public void showHighTemperatureDialog() {
        if (this.mHighTempDialog == null) {
            SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
            systemUIDialog.setIconAttribute(16843605);
            systemUIDialog.setTitle(R$string.high_temp_title);
            systemUIDialog.setMessage(R$string.high_temp_dialog_message);
            systemUIDialog.setPositiveButton(17039370, null);
            systemUIDialog.setShowForAllUsers(true);
            systemUIDialog.setOnDismissListener(new OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PowerNotificationWarnings.this.lambda$showHighTemperatureDialog$0$PowerNotificationWarnings(dialogInterface);
                }
            });
            systemUIDialog.show();
            this.mHighTempDialog = systemUIDialog;
        }
    }

    public /* synthetic */ void lambda$showHighTemperatureDialog$0$PowerNotificationWarnings(DialogInterface dialogInterface) {
        this.mHighTempDialog = null;
    }

    /* access modifiers changed from: 0000 */
    public void dismissThermalShutdownWarning() {
        this.mNoMan.cancelAsUser("high_temp", 39, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void showThermalShutdownDialog() {
        if (this.mThermalShutdownDialog == null) {
            SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
            systemUIDialog.setIconAttribute(16843605);
            systemUIDialog.setTitle(R$string.thermal_shutdown_title);
            systemUIDialog.setMessage(R$string.thermal_shutdown_dialog_message);
            systemUIDialog.setPositiveButton(17039370, null);
            systemUIDialog.setShowForAllUsers(true);
            systemUIDialog.setOnDismissListener(new OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PowerNotificationWarnings.this.lambda$showThermalShutdownDialog$1$PowerNotificationWarnings(dialogInterface);
                }
            });
            systemUIDialog.show();
            this.mThermalShutdownDialog = systemUIDialog;
        }
    }

    public /* synthetic */ void lambda$showThermalShutdownDialog$1$PowerNotificationWarnings(DialogInterface dialogInterface) {
        this.mThermalShutdownDialog = null;
    }

    public void showThermalShutdownWarning() {
        Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(R$drawable.ic_device_thermostat_24).setWhen(0).setShowWhen(false).setContentTitle(this.mContext.getString(R$string.thermal_shutdown_title)).setContentText(this.mContext.getString(R$string.thermal_shutdown_message)).setVisibility(1).setContentIntent(pendingBroadcast("PNW.clickedThermalShutdownWarning")).setDeleteIntent(pendingBroadcast("PNW.dismissedThermalShutdownWarning")).setColor(Utils.getColorAttrDefaultColor(this.mContext, 16844099));
        SystemUI.overrideNotificationAppName(this.mContext, color, false);
        String str = "high_temp";
        this.mNoMan.notifyAsUser(str, 39, color.build(), UserHandle.ALL);
    }

    public void showUsbHighTemperatureAlarm() {
        this.mHandler.post(new Runnable() {
            public final void run() {
                PowerNotificationWarnings.this.lambda$showUsbHighTemperatureAlarm$2$PowerNotificationWarnings();
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: showUsbHighTemperatureAlarmInternal */
    public void lambda$showUsbHighTemperatureAlarm$2$PowerNotificationWarnings() {
        if (this.mUsbHighTempDialog == null) {
            SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext, R$style.Theme_SystemUI_Dialog_Alert);
            systemUIDialog.setCancelable(false);
            systemUIDialog.setIconAttribute(16843605);
            systemUIDialog.setTitle(R$string.high_temp_alarm_title);
            systemUIDialog.setShowForAllUsers(true);
            systemUIDialog.setMessage(this.mContext.getString(R$string.high_temp_alarm_notify_message, new Object[]{""}));
            systemUIDialog.setPositiveButton(17039370, new OnClickListener() {
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PowerNotificationWarnings.this.mo13479xcabad378(dialogInterface, i);
                }
            });
            systemUIDialog.setNegativeButton(R$string.high_temp_alarm_help_care_steps, new OnClickListener() {
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PowerNotificationWarnings.this.mo13481x3d5cf4fa(dialogInterface, i);
                }
            });
            systemUIDialog.setOnDismissListener(new OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PowerNotificationWarnings.this.mo13482x76ae05bb(dialogInterface);
                }
            });
            systemUIDialog.getWindow().addFlags(2097280);
            systemUIDialog.show();
            this.mUsbHighTempDialog = systemUIDialog;
            Events.writeEvent(this.mContext, 19, Integer.valueOf(3), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
        }
    }

    /* renamed from: lambda$showUsbHighTemperatureAlarmInternal$3$PowerNotificationWarnings */
    public /* synthetic */ void mo13479xcabad378(DialogInterface dialogInterface, int i) {
        this.mUsbHighTempDialog = null;
    }

    /* renamed from: lambda$showUsbHighTemperatureAlarmInternal$5$PowerNotificationWarnings */
    public /* synthetic */ void mo13481x3d5cf4fa(DialogInterface dialogInterface, int i) {
        String string = this.mContext.getString(R$string.high_temp_alarm_help_url);
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
        intent.putExtra("android.intent.extra.TEXT", string);
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).startActivity(intent, true, (Callback) new Callback() {
            public final void onActivityStarted(int i) {
                PowerNotificationWarnings.this.mo13480x40be439(i);
            }
        });
    }

    /* renamed from: lambda$showUsbHighTemperatureAlarmInternal$4$PowerNotificationWarnings */
    public /* synthetic */ void mo13480x40be439(int i) {
        this.mUsbHighTempDialog = null;
    }

    /* renamed from: lambda$showUsbHighTemperatureAlarmInternal$6$PowerNotificationWarnings */
    public /* synthetic */ void mo13482x76ae05bb(DialogInterface dialogInterface) {
        this.mUsbHighTempDialog = null;
        Events.writeEvent(this.mContext, 20, Integer.valueOf(9), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
    }

    public void updateLowBatteryWarning() {
        updateNotification();
    }

    public void dismissLowBatteryWarning() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("dismissing low battery warning: level=");
            sb.append(this.mBatteryLevel);
            Slog.d("PowerUI.Notification", sb.toString());
        }
        dismissLowBatteryNotification();
    }

    /* access modifiers changed from: private */
    public void dismissLowBatteryNotification() {
        if (this.mWarning) {
            Slog.i("PowerUI.Notification", "dismissing low battery notification");
        }
        this.mWarning = false;
        updateNotification();
    }

    private boolean hasBatterySettings() {
        return this.mOpenBatterySettings.resolveActivity(this.mContext.getPackageManager()) != null;
    }

    public void showLowBatteryWarning(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("show low battery warning: level=");
        sb.append(this.mBatteryLevel);
        sb.append(" [");
        sb.append(this.mBucket);
        sb.append("] playSound=");
        sb.append(z);
        Slog.i("PowerUI.Notification", sb.toString());
        this.mPlaySound = z;
        this.mWarning = true;
        updateNotification();
    }

    public void dismissInvalidChargerWarning() {
        dismissInvalidChargerNotification();
    }

    private void dismissInvalidChargerNotification() {
        if (this.mInvalidCharger) {
            Slog.i("PowerUI.Notification", "dismissing invalid charger notification");
        }
        this.mInvalidCharger = false;
        updateNotification();
    }

    public void showInvalidChargerWarning() {
        this.mInvalidCharger = true;
        updateNotification();
    }

    /* access modifiers changed from: private */
    public void showAutoSaverSuggestion() {
        this.mShowAutoSaverSuggestion = true;
        updateNotification();
    }

    /* access modifiers changed from: private */
    public void dismissAutoSaverSuggestion() {
        this.mShowAutoSaverSuggestion = false;
        updateNotification();
    }

    public void userSwitched() {
        updateNotification();
    }

    /* access modifiers changed from: private */
    public void showStartSaverConfirmation(Bundle bundle) {
        if (this.mSaverConfirmation == null) {
            SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
            boolean z = bundle.getBoolean("extra_confirm_only");
            int i = bundle.getInt("extra_power_save_mode_trigger", 0);
            int i2 = bundle.getInt("extra_power_save_mode_trigger_level", 0);
            String string = bundle.getString("extra_power_save_mode_caller");
            if (string != null && !string.equals(this.mContext.getPackageName())) {
                if (OpPowerNotificationWarnings.OP_DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Set power save mode confirm window type for caller: ");
                    sb.append(string);
                    Log.d("PowerUI.Notification", sb.toString());
                }
                systemUIDialog.getWindow().setType(2008);
            }
            systemUIDialog.setMessage(getBatterySaverDescription());
            if (isEnglishLocale()) {
                systemUIDialog.setMessageHyphenationFrequency(0);
            }
            systemUIDialog.setMessageMovementMethod(LinkMovementMethod.getInstance());
            if (z) {
                systemUIDialog.setTitle(R$string.battery_saver_confirmation_title_generic);
                systemUIDialog.setPositiveButton(17039805, new OnClickListener(i, i2) {
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ int f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void onClick(DialogInterface dialogInterface, int i) {
                        PowerNotificationWarnings.this.lambda$showStartSaverConfirmation$7$PowerNotificationWarnings(this.f$1, this.f$2, dialogInterface, i);
                    }
                });
            } else {
                systemUIDialog.setTitle(R$string.battery_saver_confirmation_title);
                systemUIDialog.setPositiveButton(R$string.battery_saver_confirmation_ok, new OnClickListener() {
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        PowerNotificationWarnings.this.lambda$showStartSaverConfirmation$8$PowerNotificationWarnings(dialogInterface, i);
                    }
                });
                systemUIDialog.setNegativeButton(17039360, null);
            }
            systemUIDialog.setShowForAllUsers(true);
            systemUIDialog.setOnDismissListener(new OnDismissListener() {
                public final void onDismiss(DialogInterface dialogInterface) {
                    PowerNotificationWarnings.this.lambda$showStartSaverConfirmation$9$PowerNotificationWarnings(dialogInterface);
                }
            });
            systemUIDialog.show();
            this.mSaverConfirmation = systemUIDialog;
        }
    }

    public /* synthetic */ void lambda$showStartSaverConfirmation$7$PowerNotificationWarnings(int i, int i2, DialogInterface dialogInterface, int i3) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Global.putInt(contentResolver, "automatic_power_save_mode", i);
        Global.putInt(contentResolver, "low_power_trigger_level", i2);
        Secure.putInt(contentResolver, "low_power_warning_acknowledged", 1);
    }

    public /* synthetic */ void lambda$showStartSaverConfirmation$8$PowerNotificationWarnings(DialogInterface dialogInterface, int i) {
        setSaverMode(true, false);
    }

    public /* synthetic */ void lambda$showStartSaverConfirmation$9$PowerNotificationWarnings(DialogInterface dialogInterface) {
        this.mSaverConfirmation = null;
    }

    private boolean isEnglishLocale() {
        return Objects.equals(Locale.getDefault().getLanguage(), Locale.ENGLISH.getLanguage());
    }

    private CharSequence getBatterySaverDescription() {
        Annotation[] annotationArr;
        String charSequence = this.mContext.getText(R$string.help_uri_battery_saver_learn_more_link_target).toString();
        if (TextUtils.isEmpty(charSequence)) {
            return this.mContext.getText(17039601);
        }
        SpannableString spannableString = new SpannableString(this.mContext.getText(17039602));
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannableString);
        for (Annotation annotation : (Annotation[]) spannableString.getSpans(0, spannableString.length(), Annotation.class)) {
            if ("url".equals(annotation.getValue())) {
                int spanStart = spannableString.getSpanStart(annotation);
                int spanEnd = spannableString.getSpanEnd(annotation);
                C09601 r8 = new URLSpan(charSequence) {
                    public void updateDrawState(TextPaint textPaint) {
                        super.updateDrawState(textPaint);
                        textPaint.setUnderlineText(false);
                    }

                    public void onClick(View view) {
                        if (PowerNotificationWarnings.this.mSaverConfirmation != null) {
                            PowerNotificationWarnings.this.mSaverConfirmation.dismiss();
                        }
                        PowerNotificationWarnings.this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS").setFlags(268435456));
                        Uri parse = Uri.parse(getURL());
                        Context context = view.getContext();
                        Intent flags = new Intent("android.intent.action.VIEW", parse).setFlags(268435456);
                        try {
                            context.startActivity(flags);
                        } catch (ActivityNotFoundException unused) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Activity was not found for intent, ");
                            sb.append(flags.toString());
                            Log.w("PowerUI.Notification", sb.toString());
                        }
                    }
                };
                spannableStringBuilder.setSpan(r8, spanStart, spanEnd, spannableString.getSpanFlags(r8));
            }
        }
        return spannableStringBuilder;
    }

    /* access modifiers changed from: private */
    public void setSaverMode(boolean z, boolean z2) {
        BatterySaverUtils.setPowerSaveMode(this.mContext, z, z2);
    }

    /* access modifiers changed from: private */
    public void startBatterySaverSchedulePage() {
        Intent intent = new Intent("com.android.settings.BATTERY_SAVER_SCHEDULE_SETTINGS");
        intent.setFlags(268468224);
        this.mActivityStarter.startActivity(intent, true);
    }

    public boolean isWarningNotificationShow() {
        return super.isWarningNotificationShow();
    }
}
