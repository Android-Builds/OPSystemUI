package com.oneplus.systemui.statusbar.phone;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.util.OpFeatures;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.WLBSwitchController;
import com.android.systemui.statusbar.phone.WLBSwitchController.WLBControllerCallbacks;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.util.NotificationChannels;
import com.oneplus.opzenmode.OpZenModeController;
import com.oneplus.opzenmode.OpZenModeController.Callback;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpPhoneStatusBarPolicy implements Callback, WLBControllerCallbacks {
    /* access modifiers changed from: private */
    public static boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private final Handler mBgHandler;
    private BluetoothController mBluetooth;
    private int mBluetoothBatteryLevel;
    private boolean mBluetoothConnected;
    /* access modifiers changed from: private */
    public String mBluetoothContentDescription;
    /* access modifiers changed from: private */
    public int mBluetoothIconId;
    /* access modifiers changed from: private */
    public boolean mBluetoothIconVisible;
    private Context mContext;
    /* access modifiers changed from: private */
    public StatusBarIconController mIconController;
    BroadcastReceiver mIntentReceiver;
    private boolean mIsNotifyShown;
    private final BroadcastReceiver mNfcReceiver;
    private NotificationManager mNotificationManager;
    private OpZenModeController mOpZenModeController;
    private final SettingObserver mSettingObserver = new SettingObserver();
    private Runnable mShowRunnable;
    private String mSlotWLB;
    private int mVibrateWhenMute;
    private WLBSwitchController mWlbSwitchController;

    public class OpUpdateVolumeZenObject {
        public String volumeDescription = null;
        public int volumeIconId = 0;
        public boolean volumeVisible = false;
        public String zenDescription = null;
        public int zenIconId = 0;
        public boolean zenVisible = false;

        OpUpdateVolumeZenObject(boolean z, int i, String str, boolean z2, int i2, String str2) {
            this.zenVisible = z;
            this.zenIconId = i;
            this.zenDescription = str;
            this.volumeVisible = z2;
            this.volumeIconId = i2;
            this.volumeDescription = str2;
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            OpPhoneStatusBarPolicy.this.updateVolumeZen();
        }
    }

    public void onExpansionChanged(float f) {
    }

    public OpPhoneStatusBarPolicy(Context context, StatusBarIconController statusBarIconController) {
        boolean z = false;
        this.mVibrateWhenMute = 0;
        this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mBluetoothIconId = 0;
        this.mBluetoothContentDescription = null;
        this.mBluetoothConnected = false;
        this.mBluetoothBatteryLevel = -1;
        this.mBluetoothIconVisible = false;
        this.mIsNotifyShown = false;
        this.mNotificationManager = null;
        this.mIntentReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
            /* JADX WARNING: Removed duplicated region for block: B:14:0x0034  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(android.content.Context r4, android.content.Intent r5) {
                /*
                    r3 = this;
                    java.lang.String r4 = r5.getAction()
                    int r0 = r4.hashCode()
                    r1 = 579327048(0x2287d448, float:3.6816585E-18)
                    r2 = 1
                    if (r0 == r1) goto L_0x001e
                    r1 = 880816517(0x34803185, float:2.3877888E-7)
                    if (r0 == r1) goto L_0x0014
                    goto L_0x0028
                L_0x0014:
                    java.lang.String r0 = "com.oem.intent.action.ACTION_USB_HEADSET_PLUG"
                    boolean r4 = r4.equals(r0)
                    if (r4 == 0) goto L_0x0028
                    r4 = r2
                    goto L_0x0029
                L_0x001e:
                    java.lang.String r0 = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
                    boolean r4 = r4.equals(r0)
                    if (r4 == 0) goto L_0x0028
                    r4 = 0
                    goto L_0x0029
                L_0x0028:
                    r4 = -1
                L_0x0029:
                    if (r4 == 0) goto L_0x0034
                    if (r4 == r2) goto L_0x002e
                    goto L_0x0046
                L_0x002e:
                    com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy r3 = com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.this
                    r3.updateHeadsetPlug(r5)
                    goto L_0x0046
                L_0x0034:
                    boolean r4 = com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.DEBUG_ONEPLUS
                    if (r4 == 0) goto L_0x0041
                    java.lang.String r4 = "OpPhoneStatusBarPolicy"
                    java.lang.String r5 = "BT battery level changed"
                    android.util.Log.d(r4, r5)
                L_0x0041:
                    com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy r3 = com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.this
                    r3.updateBluetooth()
                L_0x0046:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.C20951.onReceive(android.content.Context, android.content.Intent):void");
            }
        };
        this.mShowRunnable = new Runnable() {
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("set BT Icon: ");
                sb.append(OpPhoneStatusBarPolicy.this.mBluetoothIconVisible);
                Log.d("OpPhoneStatusBarPolicy", sb.toString());
                OpPhoneStatusBarPolicy.this.mIconController.setIcon(OpPhoneStatusBarPolicy.this.getSlotBluetooth(), OpPhoneStatusBarPolicy.this.mBluetoothIconId, OpPhoneStatusBarPolicy.this.mBluetoothContentDescription);
                OpPhoneStatusBarPolicy.this.mIconController.setIconVisibility(OpPhoneStatusBarPolicy.this.getSlotBluetooth(), OpPhoneStatusBarPolicy.this.mBluetoothIconVisible);
            }
        };
        this.mNfcReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int intExtra = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 0);
                String str = "nfc";
                if (intExtra == 3 || intExtra == 2) {
                    OpPhoneStatusBarPolicy.this.mIconController.setIconVisibility(str, true);
                } else {
                    OpPhoneStatusBarPolicy.this.mIconController.setIconVisibility(str, false);
                }
            }
        };
        this.mContext = context;
        this.mIconController = statusBarIconController;
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oem_vibrate_under_silent"), false, this.mSettingObserver, -1);
        this.mOpZenModeController = (OpZenModeController) Dependency.get(OpZenModeController.class);
        this.mOpZenModeController.addCallback(this);
        this.mBluetooth = (BluetoothController) Dependency.get(BluetoothController.class);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
        if (OpFeatures.isSupport(new int[]{66})) {
            this.mIsNotifyShown = this.mContext.getSharedPreferences("pref_name_notify_shown", 0).getBoolean("pref_key_notify_shown", false);
            if (!this.mIsNotifyShown) {
                intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
            }
        }
        intentFilter.addAction("com.oem.intent.action.ACTION_USB_HEADSET_PLUG");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, getHandler());
        String str = "nfc";
        this.mIconController.setIcon(str, R$drawable.stat_sys_nfc, null);
        try {
            NfcAdapter nfcAdapter = NfcAdapter.getNfcAdapter(this.mContext);
            this.mIconController.setIconVisibility(str, nfcAdapter != null && nfcAdapter.isEnabled());
        } catch (UnsupportedOperationException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fail to get Nfc adapter ");
            sb.append(e);
            Log.e("OpPhoneStatusBarPolicy", sb.toString());
            this.mIconController.setIconVisibility(str, false);
        }
        this.mContext.registerReceiverAsUser(this.mNfcReceiver, UserHandle.ALL, new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED"), null, null);
        this.mWlbSwitchController = (WLBSwitchController) Dependency.get(WLBSwitchController.class);
        this.mSlotWLB = context.getString(17041114);
        this.mIconController.setIcon(this.mSlotWLB, R$drawable.stat_sys_wlb_mode, null);
        StatusBarIconController statusBarIconController2 = this.mIconController;
        String str2 = this.mSlotWLB;
        if (this.mWlbSwitchController.getCurrentMode() > 0) {
            z = true;
        }
        statusBarIconController2.setIconVisibility(str2, z);
        this.mWlbSwitchController.setCallBacks(this);
    }

    /* access modifiers changed from: protected */
    public void OpUpdateBluetooth() {
        this.mBgHandler.post(new Runnable() {
            public final void run() {
                OpPhoneStatusBarPolicy.this.lambda$OpUpdateBluetooth$0$OpPhoneStatusBarPolicy();
            }
        });
    }

    public /* synthetic */ void lambda$OpUpdateBluetooth$0$OpPhoneStatusBarPolicy() {
        int i;
        String str;
        int i2;
        boolean z;
        BluetoothController bluetoothController = this.mBluetooth;
        String str2 = "OpPhoneStatusBarPolicy";
        boolean z2 = false;
        if (bluetoothController != null) {
            z = bluetoothController.isBluetoothConnected();
            if (OpUtils.isUST()) {
                z2 = this.mBluetooth.isBluetoothEnabled();
            } else if (z) {
                z2 = this.mBluetooth.isBluetoothEnabled();
            }
            if (z) {
                str = this.mContext.getString(R$string.accessibility_bluetooth_connected);
                i = this.mBluetooth.getBatteryLevel();
                if (i == -1 || getBluetoothBatteryIcon(i) == 0) {
                    i2 = R$drawable.stat_sys_data_bluetooth_connected;
                } else {
                    i2 = getBluetoothBatteryIcon(i);
                }
            } else {
                i = -1;
                i2 = R$drawable.stat_sys_data_bluetooth;
                str = this.mContext.getString(R$string.accessibility_quick_settings_bluetooth_on);
            }
        } else {
            Log.e(str2, "BluetoothController == null");
            int i3 = R$drawable.stat_sys_data_bluetooth;
            str = this.mContext.getString(R$string.accessibility_quick_settings_bluetooth_on);
            i = -1;
            i2 = i3;
            z = false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("OpUpdateBluetooth, ");
        sb.append(z);
        sb.append(", ");
        sb.append(z2);
        Log.d(str2, sb.toString());
        if (!(this.mBluetoothIconId == i2 && this.mBluetoothIconVisible == z2 && this.mBluetoothConnected == z && this.mBluetoothBatteryLevel == i && (str == null || str.equals(this.mBluetoothContentDescription)))) {
            StringBuilder sb2 = new StringBuilder("updateBluetooth");
            sb2.append(" mBluetooth is ");
            sb2.append(this.mBluetooth == null ? "" : "not ");
            sb2.append("null");
            sb2.append(" mIconId=");
            sb2.append(this.mBluetoothIconId);
            sb2.append(" iconId=");
            sb2.append(i2);
            sb2.append(" mConnected=");
            sb2.append(this.mBluetoothConnected);
            sb2.append(" connected=");
            sb2.append(z);
            sb2.append(" mVisible=");
            sb2.append(this.mBluetoothIconVisible);
            sb2.append(", visible=");
            sb2.append(z2);
            sb2.append(" mDescrip=");
            sb2.append(this.mBluetoothContentDescription);
            sb2.append(", descrip=");
            sb2.append(str);
            sb2.append(" mLevel=");
            sb2.append(this.mBluetoothBatteryLevel);
            sb2.append(" batteryLevel=");
            sb2.append(i);
            Log.d(str2, sb2.toString());
            this.mBluetoothIconId = i2;
            this.mBluetoothIconVisible = z2;
            this.mBluetoothConnected = z;
            this.mBluetoothBatteryLevel = i;
            this.mBluetoothContentDescription = str;
        }
        postShowBT();
    }

    private void postShowBT() {
        getHandler().removeCallbacks(this.mShowRunnable);
        getHandler().postDelayed(this.mShowRunnable, 50);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0100  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.OpUpdateVolumeZenObject updateVolumeZen(int r14) {
        /*
            r13 = this;
            android.content.Context r2 = r13.mContext
            java.lang.String r3 = "audio"
            java.lang.Object r2 = r2.getSystemService(r3)
            android.media.AudioManager r2 = (android.media.AudioManager) r2
            android.content.Context r3 = r13.mContext
            android.content.ContentResolver r3 = r3.getContentResolver()
            int r4 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()
            r5 = 0
            java.lang.String r6 = "oem_vibrate_under_silent"
            int r3 = android.provider.Settings.System.getIntForUser(r3, r6, r5, r4)
            r13.mVibrateWhenMute = r3
            com.oneplus.opzenmode.OpZenModeController r3 = r13.mOpZenModeController
            int r3 = r3.getThreeKeySatus()
            com.oneplus.opzenmode.OpZenModeController r4 = r13.mOpZenModeController
            boolean r4 = r4.getDndEnable()
            boolean r6 = com.oneplus.util.OpUtils.isSupportSOCThreekey()
            java.lang.String r7 = "show silent icon on status bar"
            java.lang.String r8 = "show no disturb icon on status bar"
            java.lang.String r9 = "three key in normal state, no icon shows on status bar"
            r10 = 23
            r11 = 1
            if (r6 == 0) goto L_0x0065
            if (r4 == 0) goto L_0x0051
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_no_disturb
            android.content.Context r4 = r13.mContext
            int r6 = com.android.systemui.R$string.interruption_level_none
            java.lang.String r4 = r4.getString(r6)
            android.content.Context r6 = r13.mContext
            java.lang.Object[] r8 = new java.lang.Object[r11]
            java.lang.String r12 = "show disturb icon on status bar"
            r8[r5] = r12
            com.android.systemui.volume.Events.writeEvent(r6, r10, r8)
            goto L_0x00af
        L_0x0051:
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_no_disturb
            android.content.Context r4 = r13.mContext
            int r6 = com.android.systemui.R$string.interruption_level_none
            java.lang.String r4 = r4.getString(r6)
            android.content.Context r6 = r13.mContext
            java.lang.Object[] r12 = new java.lang.Object[r11]
            r12[r5] = r8
            com.android.systemui.volume.Events.writeEvent(r6, r10, r12)
            goto L_0x007d
        L_0x0065:
            if (r14 == r11) goto L_0x009c
            r4 = 3
            if (r14 == r4) goto L_0x0081
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_normal
            android.content.Context r4 = r13.mContext
            int r6 = com.android.systemui.R$string.quick_settings_dnd_label
            java.lang.String r4 = r4.getString(r6)
            android.content.Context r6 = r13.mContext
            java.lang.Object[] r8 = new java.lang.Object[r11]
            r8[r5] = r9
            com.android.systemui.volume.Events.writeEvent(r6, r10, r8)
        L_0x007d:
            r6 = r4
            r8 = r5
        L_0x007f:
            r4 = r0
            goto L_0x00b2
        L_0x0081:
            int r0 = r13.mVibrateWhenMute
            if (r0 != r11) goto L_0x0088
            int r0 = com.android.systemui.R$drawable.stat_sys_ringer_vibrate
            goto L_0x008a
        L_0x0088:
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_silent
        L_0x008a:
            android.content.Context r4 = r13.mContext
            int r6 = com.android.systemui.R$string.interruption_level_priority
            java.lang.String r4 = r4.getString(r6)
            android.content.Context r6 = r13.mContext
            java.lang.Object[] r8 = new java.lang.Object[r11]
            r8[r5] = r7
            com.android.systemui.volume.Events.writeEvent(r6, r10, r8)
            goto L_0x00af
        L_0x009c:
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_no_disturb
            android.content.Context r4 = r13.mContext
            int r6 = com.android.systemui.R$string.interruption_level_none
            java.lang.String r4 = r4.getString(r6)
            android.content.Context r6 = r13.mContext
            java.lang.Object[] r12 = new java.lang.Object[r11]
            r12[r5] = r8
            com.android.systemui.volume.Events.writeEvent(r6, r10, r12)
        L_0x00af:
            r6 = r4
            r8 = r11
            goto L_0x007f
        L_0x00b2:
            boolean r0 = com.oneplus.util.OpUtils.isSupportSOCThreekey()
            if (r0 == 0) goto L_0x0100
            if (r3 == r11) goto L_0x00e9
            r0 = 2
            if (r3 == r0) goto L_0x00d3
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_normal
            android.content.Context r2 = r13.mContext
            int r3 = com.android.systemui.R$string.quick_settings_dnd_label
            java.lang.String r2 = r2.getString(r3)
            android.content.Context r3 = r13.mContext
            java.lang.Object[] r7 = new java.lang.Object[r11]
            r7[r5] = r9
            com.android.systemui.volume.Events.writeEvent(r3, r10, r7)
            r7 = r0
            r9 = r2
            goto L_0x0124
        L_0x00d3:
            int r0 = com.android.systemui.R$drawable.stat_sys_ringer_vibrate
            android.content.Context r2 = r13.mContext
            int r3 = com.android.systemui.R$string.accessibility_ringer_vibrate
            java.lang.String r2 = r2.getString(r3)
            android.content.Context r3 = r13.mContext
            java.lang.Object[] r7 = new java.lang.Object[r11]
            java.lang.String r9 = "show vibrate icon on status bar"
            r7[r5] = r9
            com.android.systemui.volume.Events.writeEvent(r3, r10, r7)
            goto L_0x00fc
        L_0x00e9:
            int r0 = com.android.systemui.R$drawable.stat_sys_three_key_silent
            android.content.Context r2 = r13.mContext
            int r3 = com.android.systemui.R$string.accessibility_ringer_silent
            java.lang.String r2 = r2.getString(r3)
            android.content.Context r3 = r13.mContext
            java.lang.Object[] r9 = new java.lang.Object[r11]
            r9[r5] = r7
            com.android.systemui.volume.Events.writeEvent(r3, r10, r9)
        L_0x00fc:
            r7 = r0
            r9 = r2
            r5 = r11
            goto L_0x0124
        L_0x0100:
            android.content.Context r0 = r13.mContext
            boolean r0 = com.android.systemui.p007qs.tiles.DndTile.isVisible(r0)
            if (r0 == 0) goto L_0x0121
            android.content.Context r0 = r13.mContext
            boolean r0 = com.android.systemui.p007qs.tiles.DndTile.isCombinedIcon(r0)
            if (r0 != 0) goto L_0x0121
            int r0 = r2.getRingerModeInternal()
            if (r0 != 0) goto L_0x0121
            int r0 = com.android.systemui.R$drawable.stat_sys_ringer_silent
            android.content.Context r2 = r13.mContext
            int r3 = com.android.systemui.R$string.accessibility_ringer_silent
            java.lang.String r2 = r2.getString(r3)
            goto L_0x00fc
        L_0x0121:
            r0 = 0
            r9 = r0
            r7 = r5
        L_0x0124:
            com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy$OpUpdateVolumeZenObject r10 = new com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy$OpUpdateVolumeZenObject
            r0 = r10
            r1 = r13
            r2 = r8
            r3 = r4
            r4 = r6
            r6 = r7
            r7 = r9
            r0.<init>(r2, r3, r4, r5, r6, r7)
            return r10
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.updateVolumeZen(int):com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy$OpUpdateVolumeZenObject");
    }

    private int getBluetoothBatteryIcon(int i) {
        switch (i / 10) {
            case 0:
                return R$drawable.stat_sys_bt_battery_0;
            case 1:
                return R$drawable.stat_sys_bt_battery_1;
            case 2:
                return R$drawable.stat_sys_bt_battery_2;
            case 3:
                return R$drawable.stat_sys_bt_battery_3;
            case 4:
                return R$drawable.stat_sys_bt_battery_4;
            case 5:
                return R$drawable.stat_sys_bt_battery_5;
            case 6:
                return R$drawable.stat_sys_bt_battery_6;
            case 7:
                return R$drawable.stat_sys_bt_battery_7;
            case 8:
                return R$drawable.stat_sys_bt_battery_8;
            case 9:
                return R$drawable.stat_sys_bt_battery_9;
            case 10:
                return R$drawable.stat_sys_bt_battery_10;
            default:
                return 0;
        }
    }

    public void onThreeKeyStatus(int i) {
        Log.i("OpPhoneStatusBarPolicy", " onThreeKeyStatus");
        getHandler().post(new Runnable() {
            public void run() {
                OpPhoneStatusBarPolicy.this.updateVolumeZen();
            }
        });
    }

    public void onDndChanged(boolean z) {
        getHandler().post(new Runnable() {
            public void run() {
                OpPhoneStatusBarPolicy.this.updateVolumeZen();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void sendHeadsetNotify() {
        if (this.mNotificationManager != null && !this.mIsNotifyShown) {
            Builder contentText = new Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(R$drawable.ic_earphone).setContentTitle(this.mContext.getString(R$string.non_op_earphone_notification_title)).setContentText(this.mContext.getString(R$string.non_op_earphone_notification_content));
            Intent intent = new Intent("android.oneplus.EARPHONE_MODE_SETTINGS");
            intent.putExtra("earmode_from_notify", true);
            contentText.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT));
            SystemUI.overrideNotificationAppName(this.mContext, contentText, true);
            String str = "headset";
            this.mNotificationManager.notifyAsUser(str, 54088, contentText.build(), UserHandle.ALL);
            this.mIsNotifyShown = true;
            this.mContext.getSharedPreferences("pref_name_notify_shown", 0).edit().putBoolean("pref_key_notify_shown", true).commit();
        }
    }

    /* access modifiers changed from: protected */
    public void cancelHeadsetNotify() {
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            notificationManager.cancelAsUser("headset", 54088, UserHandle.ALL);
        }
    }

    public void opUpdateHotspot() {
        if (OpUtils.isUST()) {
            this.mIconController.setIcon(getSlotHotspot(), R$drawable.stat_sys_hotspot, this.mContext.getString(R$string.accessibility_status_bar_hotspot));
        }
    }

    private Handler getHandler() {
        return (Handler) OpReflectionUtils.getValue(PhoneStatusBarPolicy.class, this, "mHandler");
    }

    /* access modifiers changed from: private */
    public void updateVolumeZen() {
        OpReflectionUtils.methodInvokeVoid(PhoneStatusBarPolicy.class, this, "updateVolumeZen", new Object[0]);
    }

    /* access modifiers changed from: private */
    public String getSlotBluetooth() {
        return (String) OpReflectionUtils.getValue(PhoneStatusBarPolicy.class, this, "mSlotBluetooth");
    }

    /* access modifiers changed from: private */
    public void updateBluetooth() {
        OpReflectionUtils.methodInvokeVoid(PhoneStatusBarPolicy.class, this, "updateBluetooth", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void updateHeadsetPlug(Intent intent) {
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(PhoneStatusBarPolicy.class, "updateHeadsetPlug", Intent.class), intent);
    }

    private String getSlotHotspot() {
        return (String) OpReflectionUtils.getValue(PhoneStatusBarPolicy.class, this, "mSlotHotspot");
    }

    public void onWLBModeChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onWLBModeChanged : ");
        sb.append(i);
        Log.d("OpPhoneStatusBarPolicy", sb.toString());
        if (i == 1 || i == 2) {
            this.mIconController.setIconVisibility(this.mSlotWLB, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotWLB, false);
        }
    }

    public void hideStatusBarIcon() {
        this.mIconController.setIconVisibility(this.mSlotWLB, false);
    }
}
