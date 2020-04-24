package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.OpFeatures;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.p007qs.tiles.RotationLockTile;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothController.Callback;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DataSaverController.Listener;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.SensorPrivacyController.OnSensorPrivacyChangedListener;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy;
import com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarPolicy.OpUpdateVolumeZenObject;
import java.util.Locale;

public class PhoneStatusBarPolicy extends OpPhoneStatusBarPolicy implements Callback, Callbacks, RotationLockControllerCallback, Listener, ZenModeController.Callback, DeviceProvisionedListener, KeyguardMonitor.Callback, LocationChangeCallback {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    public static final int LOCATION_STATUS_ICON_ID = R$drawable.op_perm_group_location;
    protected static final Handler mHandler = new Handler();
    private final AlarmManager mAlarmManager;
    private BluetoothController mBluetooth;
    private final CastController mCast;
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }
    };
    private final Context mContext;
    private boolean mCurrentUserSetup;
    private final DataSaverController mDataSaver;
    private final HotspotController mHotspot;
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        public void onHotspotChanged(boolean z, int i) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotHotspot, z);
        }
    };
    /* access modifiers changed from: private */
    public final StatusBarIconController mIconController;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(android.content.Context r3, android.content.Intent r4) {
            /*
                r2 = this;
                java.lang.String r3 = r4.getAction()
                int r0 = r3.hashCode()
                r1 = 0
                switch(r0) {
                    case -1676458352: goto L_0x0053;
                    case -1238404651: goto L_0x0049;
                    case -864107122: goto L_0x003f;
                    case -229777127: goto L_0x0035;
                    case 100931828: goto L_0x002b;
                    case 1051344550: goto L_0x0021;
                    case 1051477093: goto L_0x0017;
                    case 2070024785: goto L_0x000d;
                    default: goto L_0x000c;
                }
            L_0x000c:
                goto L_0x005d
            L_0x000d:
                java.lang.String r0 = "android.media.RINGER_MODE_CHANGED"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = r1
                goto L_0x005e
            L_0x0017:
                java.lang.String r0 = "android.intent.action.MANAGED_PROFILE_REMOVED"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 6
                goto L_0x005e
            L_0x0021:
                java.lang.String r0 = "android.telecom.action.CURRENT_TTY_MODE_CHANGED"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 3
                goto L_0x005e
            L_0x002b:
                java.lang.String r0 = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 1
                goto L_0x005e
            L_0x0035:
                java.lang.String r0 = "android.intent.action.SIM_STATE_CHANGED"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 2
                goto L_0x005e
            L_0x003f:
                java.lang.String r0 = "android.intent.action.MANAGED_PROFILE_AVAILABLE"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 4
                goto L_0x005e
            L_0x0049:
                java.lang.String r0 = "android.intent.action.MANAGED_PROFILE_UNAVAILABLE"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 5
                goto L_0x005e
            L_0x0053:
                java.lang.String r0 = "android.intent.action.HEADSET_PLUG"
                boolean r3 = r3.equals(r0)
                if (r3 == 0) goto L_0x005d
                r3 = 7
                goto L_0x005e
            L_0x005d:
                r3 = -1
            L_0x005e:
                switch(r3) {
                    case 0: goto L_0x0089;
                    case 1: goto L_0x0089;
                    case 2: goto L_0x007a;
                    case 3: goto L_0x006e;
                    case 4: goto L_0x0068;
                    case 5: goto L_0x0068;
                    case 6: goto L_0x0068;
                    case 7: goto L_0x0062;
                    default: goto L_0x0061;
                }
            L_0x0061:
                goto L_0x008e
            L_0x0062:
                com.android.systemui.statusbar.phone.PhoneStatusBarPolicy r2 = com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.this
                r2.updateHeadsetPlug(r4)
                goto L_0x008e
            L_0x0068:
                com.android.systemui.statusbar.phone.PhoneStatusBarPolicy r2 = com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.this
                r2.updateManagedProfile()
                goto L_0x008e
            L_0x006e:
                com.android.systemui.statusbar.phone.PhoneStatusBarPolicy r2 = com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.this
                java.lang.String r3 = "android.telecom.intent.extra.CURRENT_TTY_MODE"
                int r3 = r4.getIntExtra(r3, r1)
                r2.updateTTY(r3)
                goto L_0x008e
            L_0x007a:
                java.lang.String r3 = "rebroadcastOnUnlock"
                boolean r3 = r4.getBooleanExtra(r3, r1)
                if (r3 == 0) goto L_0x0083
                goto L_0x008e
            L_0x0083:
                com.android.systemui.statusbar.phone.PhoneStatusBarPolicy r2 = com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.this
                r2.updateSimState(r4)
                goto L_0x008e
            L_0x0089:
                com.android.systemui.statusbar.phone.PhoneStatusBarPolicy r2 = com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.this
                r2.updateVolumeZen()
            L_0x008e:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.C15036.onReceive(android.content.Context, android.content.Intent):void");
        }
    };
    private final KeyguardMonitor mKeyguardMonitor;
    private final LocationController mLocationController;
    private boolean mManagedProfileIconVisible = false;
    /* access modifiers changed from: private */
    public AlarmClockInfo mNextAlarm;
    private final NextAlarmChangeCallback mNextAlarmCallback = new NextAlarmChangeCallback() {
        public void onNextAlarmChanged(AlarmClockInfo alarmClockInfo) {
            PhoneStatusBarPolicy.this.mNextAlarm = alarmClockInfo;
            PhoneStatusBarPolicy.this.updateAlarm();
        }
    };
    private final NextAlarmController mNextAlarmController;
    private final DeviceProvisionedController mProvisionedController;
    private Runnable mRemoveCastIconRunnable = new Runnable() {
        public void run() {
            if (PhoneStatusBarPolicy.DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
            }
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotCast, false);
        }
    };
    private final RotationLockController mRotationLockController;
    private final SensorPrivacyController mSensorPrivacyController;
    private final OnSensorPrivacyChangedListener mSensorPrivacyListener = new OnSensorPrivacyChangedListener() {
        public void onSensorPrivacyChanged(boolean z) {
            PhoneStatusBarPolicy.mHandler.post(new Runnable(z) {
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    C15025.this.lambda$onSensorPrivacyChanged$0$PhoneStatusBarPolicy$5(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onSensorPrivacyChanged$0$PhoneStatusBarPolicy$5(boolean z) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotSensorsOff, z);
        }
    };
    State mSimState = State.READY;
    private final String mSlotAlarmClock;
    private final String mSlotBluetooth;
    /* access modifiers changed from: private */
    public final String mSlotCast;
    private final String mSlotDataSaver;
    private final String mSlotHeadset;
    /* access modifiers changed from: private */
    public final String mSlotHotspot;
    private final String mSlotLocation;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    /* access modifiers changed from: private */
    public final String mSlotSensorsOff;
    private final String mSlotTty;
    private final String mSlotVolume;
    private final String mSlotZen;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    /* access modifiers changed from: private */
    public final UserInfoController mUserInfoController;
    private final UserManager mUserManager;
    private final SynchronousUserSwitchObserver mUserSwitchListener = new SynchronousUserSwitchObserver() {
        public /* synthetic */ void lambda$onUserSwitching$0$PhoneStatusBarPolicy$1() {
            PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
        }

        public void onUserSwitching(int i) throws RemoteException {
            PhoneStatusBarPolicy.mHandler.post(new Runnable() {
                public final void run() {
                    C14981.this.lambda$onUserSwitching$0$PhoneStatusBarPolicy$1();
                }
            });
        }

        public void onUserSwitchComplete(int i) throws RemoteException {
            PhoneStatusBarPolicy.mHandler.post(new Runnable() {
                public final void run() {
                    C14981.this.lambda$onUserSwitchComplete$1$PhoneStatusBarPolicy$1();
                }
            });
        }

        public /* synthetic */ void lambda$onUserSwitchComplete$1$PhoneStatusBarPolicy$1() {
            PhoneStatusBarPolicy.this.updateAlarm();
            PhoneStatusBarPolicy.this.updateManagedProfile();
        }
    };
    private boolean mVolumeVisible;
    private final ZenModeController mZenController;
    private boolean mZenVisible;

    public PhoneStatusBarPolicy(Context context, StatusBarIconController statusBarIconController) {
        super(context, statusBarIconController);
        this.mContext = context;
        this.mIconController = statusBarIconController;
        this.mCast = (CastController) Dependency.get(CastController.class);
        this.mHotspot = (HotspotController) Dependency.get(HotspotController.class);
        this.mBluetooth = (BluetoothController) Dependency.get(BluetoothController.class);
        this.mNextAlarmController = (NextAlarmController) Dependency.get(NextAlarmController.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mRotationLockController = (RotationLockController) Dependency.get(RotationLockController.class);
        this.mDataSaver = (DataSaverController) Dependency.get(DataSaverController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mLocationController = (LocationController) Dependency.get(LocationController.class);
        this.mSensorPrivacyController = (SensorPrivacyController) Dependency.get(SensorPrivacyController.class);
        this.mSlotCast = context.getString(17041084);
        this.mSlotHotspot = context.getString(17041091);
        this.mSlotBluetooth = context.getString(17041082);
        this.mSlotTty = context.getString(17041108);
        this.mSlotZen = context.getString(17041115);
        this.mSlotVolume = context.getString(17041110);
        this.mSlotAlarmClock = context.getString(17041080);
        this.mSlotManagedProfile = context.getString(17041094);
        this.mSlotRotate = context.getString(17041102);
        this.mSlotHeadset = context.getString(17041090);
        this.mSlotDataSaver = context.getString(17041088);
        this.mSlotLocation = context.getString(17041093);
        this.mSlotSensorsOff = context.getString(17041104);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        intentFilter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, mHandler);
        boolean z = this.mContext.getResources().getBoolean(17891599);
        boolean z2 = this.mContext.getResources().getBoolean(17891600);
        try {
            ActivityManager.getService().registerUserSwitchObserver(this.mUserSwitchListener, "PhoneStatusBarPolicy");
        } catch (RemoteException unused) {
        }
        updateTTY();
        updateBluetooth();
        this.mIconController.setIcon(this.mSlotAlarmClock, R$drawable.stat_sys_alarm, null);
        this.mIconController.setIconVisibility(this.mSlotAlarmClock, false);
        this.mIconController.setIcon(this.mSlotZen, R$drawable.stat_sys_dnd, null);
        this.mIconController.setIconVisibility(this.mSlotZen, false);
        this.mIconController.setIcon(this.mSlotVolume, R$drawable.stat_sys_ringer_vibrate, null);
        this.mIconController.setIconVisibility(this.mSlotVolume, false);
        updateVolumeZen();
        this.mIconController.setIcon(this.mSlotCast, R$drawable.stat_sys_cast, null);
        this.mIconController.setIconVisibility(this.mSlotCast, false);
        if (z2) {
            if (OpFeatures.isSupport(new int[]{226})) {
                this.mIconController.setIcon(this.mSlotHotspot, R$drawable.stat_sys_wifi_6_hotspot, this.mContext.getString(R$string.accessibility_status_bar_hotspot));
                opUpdateHotspot();
                this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
                this.mIconController.setIcon(this.mSlotManagedProfile, R$drawable.stat_sys_managed_profile_status, this.mContext.getString(R$string.accessibility_managed_profile));
                this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
                this.mIconController.setIcon(this.mSlotDataSaver, R$drawable.stat_sys_data_saver, context.getString(R$string.accessibility_data_saver_on));
                this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
                this.mIconController.setIcon(this.mSlotLocation, LOCATION_STATUS_ICON_ID, this.mContext.getString(R$string.accessibility_location_active));
                this.mIconController.setIconVisibility(this.mSlotLocation, false);
                this.mIconController.setIcon(this.mSlotSensorsOff, R$drawable.stat_sys_sensors_off, this.mContext.getString(R$string.accessibility_sensors_off_active));
                this.mIconController.setIconVisibility(this.mSlotSensorsOff, this.mSensorPrivacyController.isSensorPrivacyEnabled());
                this.mRotationLockController.addCallback(this);
                this.mBluetooth.addCallback(this);
                this.mProvisionedController.addCallback(this);
                this.mZenController.addCallback(this);
                this.mCast.addCallback(this.mCastCallback);
                this.mHotspot.addCallback(this.mHotspotCallback);
                this.mNextAlarmController.addCallback(this.mNextAlarmCallback);
                this.mDataSaver.addCallback(this);
                this.mKeyguardMonitor.addCallback(this);
                this.mSensorPrivacyController.addCallback(this.mSensorPrivacyListener);
                this.mLocationController.addCallback(this);
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallback((Callbacks) this);
            }
        }
        if (z) {
            if (OpFeatures.isSupport(new int[]{226})) {
                this.mIconController.setIcon(this.mSlotHotspot, R$drawable.stat_sys_wifi_5_hotspot, this.mContext.getString(R$string.accessibility_status_bar_hotspot));
                opUpdateHotspot();
                this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
                this.mIconController.setIcon(this.mSlotManagedProfile, R$drawable.stat_sys_managed_profile_status, this.mContext.getString(R$string.accessibility_managed_profile));
                this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
                this.mIconController.setIcon(this.mSlotDataSaver, R$drawable.stat_sys_data_saver, context.getString(R$string.accessibility_data_saver_on));
                this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
                this.mIconController.setIcon(this.mSlotLocation, LOCATION_STATUS_ICON_ID, this.mContext.getString(R$string.accessibility_location_active));
                this.mIconController.setIconVisibility(this.mSlotLocation, false);
                this.mIconController.setIcon(this.mSlotSensorsOff, R$drawable.stat_sys_sensors_off, this.mContext.getString(R$string.accessibility_sensors_off_active));
                this.mIconController.setIconVisibility(this.mSlotSensorsOff, this.mSensorPrivacyController.isSensorPrivacyEnabled());
                this.mRotationLockController.addCallback(this);
                this.mBluetooth.addCallback(this);
                this.mProvisionedController.addCallback(this);
                this.mZenController.addCallback(this);
                this.mCast.addCallback(this.mCastCallback);
                this.mHotspot.addCallback(this.mHotspotCallback);
                this.mNextAlarmController.addCallback(this.mNextAlarmCallback);
                this.mDataSaver.addCallback(this);
                this.mKeyguardMonitor.addCallback(this);
                this.mSensorPrivacyController.addCallback(this.mSensorPrivacyListener);
                this.mLocationController.addCallback(this);
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallback((Callbacks) this);
            }
        }
        this.mIconController.setIcon(this.mSlotHotspot, R$drawable.stat_sys_hotspot, this.mContext.getString(R$string.accessibility_status_bar_hotspot));
        opUpdateHotspot();
        this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
        this.mIconController.setIcon(this.mSlotManagedProfile, R$drawable.stat_sys_managed_profile_status, this.mContext.getString(R$string.accessibility_managed_profile));
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
        this.mIconController.setIcon(this.mSlotDataSaver, R$drawable.stat_sys_data_saver, context.getString(R$string.accessibility_data_saver_on));
        this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
        this.mIconController.setIcon(this.mSlotLocation, LOCATION_STATUS_ICON_ID, this.mContext.getString(R$string.accessibility_location_active));
        this.mIconController.setIconVisibility(this.mSlotLocation, false);
        this.mIconController.setIcon(this.mSlotSensorsOff, R$drawable.stat_sys_sensors_off, this.mContext.getString(R$string.accessibility_sensors_off_active));
        this.mIconController.setIconVisibility(this.mSlotSensorsOff, this.mSensorPrivacyController.isSensorPrivacyEnabled());
        this.mRotationLockController.addCallback(this);
        this.mBluetooth.addCallback(this);
        this.mProvisionedController.addCallback(this);
        this.mZenController.addCallback(this);
        this.mCast.addCallback(this.mCastCallback);
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mNextAlarmController.addCallback(this.mNextAlarmCallback);
        this.mDataSaver.addCallback(this);
        this.mKeyguardMonitor.addCallback(this);
        this.mSensorPrivacyController.addCallback(this.mSensorPrivacyListener);
        this.mLocationController.addCallback(this);
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallback((Callbacks) this);
    }

    public void onZenChanged(int i) {
        updateVolumeZen();
    }

    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        updateVolumeZen();
    }

    /* access modifiers changed from: private */
    public void updateAlarm() {
        int i;
        AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(-2);
        boolean z = true;
        boolean z2 = nextAlarmClock != null && nextAlarmClock.getTriggerTime() > 0;
        boolean z3 = this.mZenController.getZen() == 2;
        StatusBarIconController statusBarIconController = this.mIconController;
        String str = this.mSlotAlarmClock;
        if (z3) {
            i = R$drawable.stat_sys_alarm_dim;
        } else {
            i = R$drawable.stat_sys_alarm;
        }
        statusBarIconController.setIcon(str, i, buildAlarmContentDescription());
        if (this.mContext != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateAlarm ");
            sb.append(formatNextAlarm(nextAlarmClock, this.mContext));
            sb.append(" hasAlarm: ");
            sb.append(z2);
            sb.append(" UserSetup: ");
            sb.append(this.mCurrentUserSetup);
            Log.d("PhoneStatusBarPolicy", sb.toString());
        }
        StatusBarIconController statusBarIconController2 = this.mIconController;
        String str2 = this.mSlotAlarmClock;
        if (!this.mCurrentUserSetup || !z2) {
            z = false;
        }
        statusBarIconController2.setIconVisibility(str2, z);
    }

    private String buildAlarmContentDescription() {
        if (this.mNextAlarm == null) {
            Context context = this.mContext;
            if (context != null) {
                return context.getString(R$string.status_bar_alarm);
            }
        }
        return formatNextAlarm(this.mNextAlarm, this.mContext);
    }

    private static String formatNextAlarm(AlarmClockInfo alarmClockInfo, Context context) {
        if (alarmClockInfo == null) {
            return "";
        }
        return context.getString(R$string.accessibility_quick_settings_alarm, new Object[]{DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString()});
    }

    /* access modifiers changed from: private */
    public final void updateSimState(Intent intent) {
        String stringExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stringExtra)) {
            this.mSimState = State.ABSENT;
        } else if ("CARD_IO_ERROR".equals(stringExtra)) {
            this.mSimState = State.CARD_IO_ERROR;
        } else if ("CARD_RESTRICTED".equals(stringExtra)) {
            this.mSimState = State.CARD_RESTRICTED;
        } else if ("READY".equals(stringExtra)) {
            this.mSimState = State.READY;
        } else if ("LOCKED".equals(stringExtra)) {
            String stringExtra2 = intent.getStringExtra("reason");
            if ("PIN".equals(stringExtra2)) {
                this.mSimState = State.PIN_REQUIRED;
            } else if ("PUK".equals(stringExtra2)) {
                this.mSimState = State.PUK_REQUIRED;
            } else {
                this.mSimState = State.NETWORK_LOCKED;
            }
        } else {
            this.mSimState = State.UNKNOWN;
        }
    }

    /* access modifiers changed from: private */
    public final void updateVolumeZen() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        OpUpdateVolumeZenObject updateVolumeZen = super.updateVolumeZen(this.mZenController.getZen());
        boolean z = updateVolumeZen.zenVisible;
        int i = updateVolumeZen.zenIconId;
        String str = updateVolumeZen.zenDescription;
        boolean z2 = updateVolumeZen.volumeVisible;
        int i2 = updateVolumeZen.volumeIconId;
        String str2 = updateVolumeZen.volumeDescription;
        if (z) {
            this.mIconController.setIcon(this.mSlotZen, i, str);
        }
        if (z != this.mZenVisible) {
            this.mIconController.setIconVisibility(this.mSlotZen, z);
            this.mZenVisible = z;
        }
        if (z2) {
            this.mIconController.setIcon(this.mSlotVolume, i2, str2);
        }
        if (z2 != this.mVolumeVisible) {
            this.mIconController.setIconVisibility(this.mSlotVolume, z2);
            this.mVolumeVisible = z2;
        }
        updateAlarm();
    }

    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    public void onBluetoothStateChange(boolean z) {
        updateBluetooth();
    }

    public void onBluetoothActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("PhoneStatusBarPolicy", "updateBluetooth");
        }
        super.OpUpdateBluetooth();
    }

    private final void updateTTY() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        if (telecomManager == null) {
            updateTTY(0);
        } else {
            updateTTY(telecomManager.getCurrentTtyMode());
        }
    }

    /* access modifiers changed from: private */
    public final void updateTTY(int i) {
        boolean z = i != 0;
        String str = "PhoneStatusBarPolicy";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateTTY: enabled: ");
            sb.append(z);
            Log.v(str, sb.toString());
        }
        if (z) {
            if (DEBUG) {
                Log.v(str, "updateTTY: set TTY on");
            }
            this.mIconController.setIcon(this.mSlotTty, R$drawable.stat_sys_tty_mode, this.mContext.getString(R$string.accessibility_tty_enabled));
            this.mIconController.setIconVisibility(this.mSlotTty, true);
            return;
        }
        if (DEBUG) {
            Log.v(str, "updateTTY: set TTY off");
        }
        this.mIconController.setIconVisibility(this.mSlotTty, false);
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x001e, code lost:
        r0 = true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateCast() {
        /*
            r6 = this;
            com.android.systemui.statusbar.policy.CastController r0 = r6.mCast
            java.util.List r0 = r0.getCastDevices()
            java.util.Iterator r0 = r0.iterator()
        L_0x000a:
            boolean r1 = r0.hasNext()
            r2 = 1
            if (r1 == 0) goto L_0x0020
            java.lang.Object r1 = r0.next()
            com.android.systemui.statusbar.policy.CastController$CastDevice r1 = (com.android.systemui.statusbar.policy.CastController.CastDevice) r1
            int r1 = r1.state
            if (r1 == r2) goto L_0x001e
            r3 = 2
            if (r1 != r3) goto L_0x000a
        L_0x001e:
            r0 = r2
            goto L_0x0021
        L_0x0020:
            r0 = 0
        L_0x0021:
            boolean r1 = DEBUG
            java.lang.String r3 = "PhoneStatusBarPolicy"
            if (r1 == 0) goto L_0x003c
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r4 = "updateCast: isCasting: "
            r1.append(r4)
            r1.append(r0)
            java.lang.String r1 = r1.toString()
            android.util.Log.v(r3, r1)
        L_0x003c:
            android.os.Handler r1 = mHandler
            java.lang.Runnable r4 = r6.mRemoveCastIconRunnable
            r1.removeCallbacks(r4)
            if (r0 == 0) goto L_0x005e
            com.android.systemui.statusbar.phone.StatusBarIconController r0 = r6.mIconController
            java.lang.String r1 = r6.mSlotCast
            int r3 = com.android.systemui.R$drawable.stat_sys_cast
            android.content.Context r4 = r6.mContext
            int r5 = com.android.systemui.R$string.accessibility_casting
            java.lang.String r4 = r4.getString(r5)
            r0.setIcon(r1, r3, r4)
            com.android.systemui.statusbar.phone.StatusBarIconController r0 = r6.mIconController
            java.lang.String r6 = r6.mSlotCast
            r0.setIconVisibility(r6, r2)
            goto L_0x0071
        L_0x005e:
            boolean r0 = DEBUG
            if (r0 == 0) goto L_0x0068
            java.lang.String r0 = "updateCast: hiding icon in 3 sec..."
            android.util.Log.v(r3, r0)
        L_0x0068:
            android.os.Handler r0 = mHandler
            java.lang.Runnable r6 = r6.mRemoveCastIconRunnable
            r1 = 3000(0xbb8, double:1.482E-320)
            r0.postDelayed(r6, r1)
        L_0x0071:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.updateCast():void");
    }

    /* access modifiers changed from: private */
    public void updateManagedProfile() {
        this.mUiOffloadThread.submit(new Runnable() {
            public final void run() {
                PhoneStatusBarPolicy.this.lambda$updateManagedProfile$1$PhoneStatusBarPolicy();
            }
        });
    }

    public /* synthetic */ void lambda$updateManagedProfile$1$PhoneStatusBarPolicy() {
        try {
            int lastResumedActivityUserId = ActivityTaskManager.getService().getLastResumedActivityUserId();
            mHandler.post(new Runnable(this.mUserManager.isManagedProfile(lastResumedActivityUserId), lastResumedActivityUserId) {
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    PhoneStatusBarPolicy.this.lambda$updateManagedProfile$0$PhoneStatusBarPolicy(this.f$1, this.f$2);
                }
            });
        } catch (RemoteException e) {
            Log.w("PhoneStatusBarPolicy", "updateManagedProfile: ", e);
        }
    }

    public /* synthetic */ void lambda$updateManagedProfile$0$PhoneStatusBarPolicy(boolean z, int i) {
        boolean z2;
        if (!z || i == 999 || (this.mKeyguardMonitor.isShowing() && !this.mKeyguardMonitor.isOccluded())) {
            z2 = false;
        } else {
            z2 = true;
            this.mIconController.setIcon(this.mSlotManagedProfile, R$drawable.stat_sys_managed_profile_status, this.mContext.getString(R$string.accessibility_managed_profile));
        }
        if (this.mManagedProfileIconVisible != z2) {
            this.mIconController.setIconVisibility(this.mSlotManagedProfile, z2);
            this.mManagedProfileIconVisible = z2;
        }
    }

    public void appTransitionStarting(int i, long j, long j2, boolean z) {
        if (this.mContext.getDisplayId() == i) {
            updateManagedProfile();
        }
    }

    public void onKeyguardShowingChanged() {
        updateManagedProfile();
    }

    public void onUserSetupChanged() {
        DeviceProvisionedController deviceProvisionedController = this.mProvisionedController;
        boolean isUserSetup = deviceProvisionedController.isUserSetup(deviceProvisionedController.getCurrentUser());
        if (this.mCurrentUserSetup != isUserSetup) {
            this.mCurrentUserSetup = isUserSetup;
            updateAlarm();
        }
    }

    public void onRotationLockStateChanged(boolean z, boolean z2) {
        boolean isCurrentOrientationLockPortrait = RotationLockTile.isCurrentOrientationLockPortrait(this.mRotationLockController, this.mContext);
        if (z) {
            if (isCurrentOrientationLockPortrait) {
                this.mIconController.setIcon(this.mSlotRotate, R$drawable.stat_sys_rotate_portrait, this.mContext.getString(R$string.accessibility_rotation_lock_on_portrait));
            } else {
                this.mIconController.setIcon(this.mSlotRotate, R$drawable.stat_sys_rotate_landscape, this.mContext.getString(R$string.accessibility_rotation_lock_on_landscape));
            }
            this.mIconController.setIconVisibility(this.mSlotRotate, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotRotate, false);
    }

    /* access modifiers changed from: private */
    public void updateHeadsetPlug(Intent intent) {
        int i;
        int i2;
        boolean z = intent.getIntExtra("state", 0) != 0;
        boolean z2 = intent.getIntExtra("microphone", 0) != 0;
        StringBuilder sb = new StringBuilder();
        sb.append("receive ACTION_USBHEADSET_PLUG, connected:");
        sb.append(z);
        sb.append(", hasMic");
        sb.append(z2);
        Log.d("PhoneStatusBarPolicy", sb.toString());
        if (z) {
            Context context = this.mContext;
            if (z2) {
                i = R$string.accessibility_status_bar_headset;
            } else {
                i = R$string.accessibility_status_bar_headphones;
            }
            String string = context.getString(i);
            StatusBarIconController statusBarIconController = this.mIconController;
            String str = this.mSlotHeadset;
            if (z2) {
                i2 = R$drawable.stat_sys_headset_mic;
            } else {
                i2 = R$drawable.stat_sys_headset;
            }
            statusBarIconController.setIcon(str, i2, string);
            this.mIconController.setIconVisibility(this.mSlotHeadset, true);
            sendHeadsetNotify();
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotHeadset, false);
        cancelHeadsetNotify();
    }

    public void onDataSaverChanged(boolean z) {
        this.mIconController.setIconVisibility(this.mSlotDataSaver, z);
    }

    public void onLocationActiveChanged(boolean z) {
        updateLocation();
        StringBuilder sb = new StringBuilder();
        sb.append("onLocationActiveChanged, update status of location service: ");
        sb.append(z);
        Log.d("PhoneStatusBarPolicy", sb.toString());
    }

    private void updateLocation() {
        if (this.mLocationController.isLocationActive()) {
            this.mIconController.setIconVisibility(this.mSlotLocation, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotLocation, false);
        }
    }
}
