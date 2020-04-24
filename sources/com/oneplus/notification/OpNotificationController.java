package com.oneplus.notification;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.OpFeatures;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsService.Stub;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.android.scene.OnePlusSceneCallBlockManagerInjector;
import com.oneplus.core.oimc.OIMCRule;
import com.oneplus.core.oimc.OIMCServiceManager;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.statusbar.phone.OpStatusBar;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.notification.SimpleHeadsUpController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class OpNotificationController implements ConfigurationListener {
    /* access modifiers changed from: private */
    public static final Uri DRIVING_MODE_STATE_URI = Secure.getUriFor("driving_mode_state");
    /* access modifiers changed from: private */
    public static final Uri ESPORTS_MODE_ENABLED = System.getUriFor("esport_mode_enabled");
    /* access modifiers changed from: private */
    public static final Uri GAME_MODE_3RD_PARTY_CALLS_UID_URI = System.getUriFor("game_mode_notifications_3rd_calls_uid");
    /* access modifiers changed from: private */
    public static final Uri GAME_MODE_BLOCK_HEADS_UP_URI = System.getUriFor("game_mode_block_notification");
    private static final List<String> ICON_COLORIZE_LIST = Arrays.asList(new String[]{"com.blueline.signalcheck", "com.yuedong.sport"});
    private static final List<String> LIFETIME_EXTENSION_LIST;
    /* access modifiers changed from: private */
    public static final boolean OP_DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public static final Uri OP_OIMC_FUNC_DISABLE_HEADSUP_BRICK_URI = Global.getUriFor("op_oimc_func_disable_headsup_breath");
    /* access modifiers changed from: private */
    public static final Uri OP_OIMC_FUNC_DISABLE_HEADSUP_URI = Global.getUriFor("op_oimc_func_disable_headsup");
    /* access modifiers changed from: private */
    public static final Uri OP_QUICKREPLY_IM_LIST_URI = System.getUriFor("op_quickreply_im_list");
    private static final List<String> PRIORITY_LIST_BRICK_MODE;
    private static final List<String> PRIORITY_LIST_DRIVING_MODE;
    private static final List<String> PRIORITY_LIST_GAME_MODE;
    private static final int[] PRIVACY_ALERT_OPS = {26, 27};
    private static final List<String> SYSTEM_APP_LIST = Arrays.asList(new String[]{"com.oneplus.soundrecorder"});
    private IAppOpsActiveCallback mAppOpsActiveCallback;
    private IAppOpsService mAppOpsService;
    /* access modifiers changed from: private */
    public boolean mBlockedByBrick = false;
    /* access modifiers changed from: private */
    public boolean mBlockedByDriving = false;
    /* access modifiers changed from: private */
    public boolean mBlockedByGame = false;
    private final CommandQueue mCommandQueue;
    private ConfigurationController mConfigurationController;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final Consumer<Boolean> mDockedListener = new Consumer<Boolean>() {
        public void accept(Boolean bool) {
            OpNotificationController.this.mDockedStackExists = bool.booleanValue();
        }
    };
    /* access modifiers changed from: private */
    public boolean mDockedStackExists = false;
    private NotificationEntryManager mEntryManager;
    /* access modifiers changed from: private */
    public boolean mEsportsModeOn = false;
    /* access modifiers changed from: private */
    public String mGameMode3rdPartyCallsUid = "-1";
    /* access modifiers changed from: private */
    public int mGameModeNotifyType = 0;
    private boolean mIsFreeForm = false;
    /* access modifiers changed from: private */
    public boolean mIsKeyguardShowing;
    private final NotificationLockscreenUserManager mLockscreenUserManager;
    private KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onSystemReady() {
            Log.d("OpNotificationController", "onSystemReady to register provider and OIMC");
            OpNotificationController opNotificationController = OpNotificationController.this;
            opNotificationController.mSettingsObserver = new SettingsObserver(new Handler());
            OpNotificationController.this.mSettingsObserver.observe();
            OpNotificationController opNotificationController2 = OpNotificationController.this;
            opNotificationController2.mOimcObserver = new OimcObserver(new Handler());
            OpNotificationController.this.mOimcObserver.observe();
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            OpNotificationController.this.mIsKeyguardShowing = z;
        }

        public void onPhoneStateChanged(int i) {
            OpNotificationController.this.mPhoneState = i;
        }
    };
    private NotificationManager mNoMan;
    /* access modifiers changed from: private */
    public final OIMCServiceManager mOIMCServiceManager;
    /* access modifiers changed from: private */
    public OimcObserver mOimcObserver;
    private int mOrientation = 0;
    private PackageManager mPackageManager;
    /* access modifiers changed from: private */
    public int mPhoneState;
    private HashMap<String, Integer> mPrivacyAlertList = new HashMap<>();
    private HashMap<String, Integer> mPrivacyGroupCount = new HashMap<>();
    /* access modifiers changed from: private */
    public List<String> mQuickReplyList = Arrays.asList(new String[0]);
    /* access modifiers changed from: private */
    public SettingsObserver mSettingsObserver;
    private SimpleHeadsUpController mSimpleHeadsUpController;
    private String mTopActivity;

    private final class OimcObserver extends ContentObserver {
        OimcObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: 0000 */
        public void observe() {
            ContentResolver contentResolver = OpNotificationController.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(OpNotificationController.OP_OIMC_FUNC_DISABLE_HEADSUP_URI, false, this, -1);
            contentResolver.registerContentObserver(OpNotificationController.OP_OIMC_FUNC_DISABLE_HEADSUP_BRICK_URI, false, this, -1);
            update(null);
        }

        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            boolean z = false;
            if (uri == null || OpNotificationController.OP_OIMC_FUNC_DISABLE_HEADSUP_BRICK_URI.equals(uri)) {
                OpNotificationController.this.mBlockedByBrick = OpNotificationController.this.mOIMCServiceManager.getRemoteFuncStatus("HeadsUpNotificationZen") == 1;
            }
            if (uri == null || OpNotificationController.OP_OIMC_FUNC_DISABLE_HEADSUP_URI.equals(uri)) {
                int remoteFuncStatus = OpNotificationController.this.mOIMCServiceManager.getRemoteFuncStatus("HeadsUpNotification");
                OpNotificationController opNotificationController = OpNotificationController.this;
                if (remoteFuncStatus == 1) {
                    z = true;
                }
                opNotificationController.mBlockedByGame = z;
            }
            if (OpNotificationController.OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("OIMC update uri: ");
                sb.append(uri);
                sb.append(" mBlockedByBrick: ");
                sb.append(OpNotificationController.this.mBlockedByBrick);
                sb.append(" mBlockedByGame: ");
                sb.append(OpNotificationController.this.mBlockedByGame);
                Log.d("OpNotificationController", sb.toString());
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: 0000 */
        public void observe() {
            ContentResolver contentResolver = OpNotificationController.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(OpNotificationController.OP_QUICKREPLY_IM_LIST_URI, false, this, -1);
            contentResolver.registerContentObserver(OpNotificationController.DRIVING_MODE_STATE_URI, false, this, -1);
            contentResolver.registerContentObserver(OpNotificationController.ESPORTS_MODE_ENABLED, false, this, -1);
            contentResolver.registerContentObserver(OpNotificationController.GAME_MODE_BLOCK_HEADS_UP_URI, false, this, -1);
            contentResolver.registerContentObserver(OpNotificationController.GAME_MODE_3RD_PARTY_CALLS_UID_URI, false, this, -1);
            update(null);
        }

        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver contentResolver = OpNotificationController.this.mContext.getContentResolver();
            boolean z = true;
            if (uri == null || OpNotificationController.DRIVING_MODE_STATE_URI.equals(uri)) {
                OpNotificationController.this.mBlockedByDriving = Secure.getIntForUser(contentResolver, "driving_mode_state", 0, -2) != 0;
            }
            if (uri == null || OpNotificationController.ESPORTS_MODE_ENABLED.equals(uri)) {
                OpNotificationController opNotificationController = OpNotificationController.this;
                if (System.getIntForUser(contentResolver, "esport_mode_enabled", 0, -2) != 1) {
                    z = false;
                }
                opNotificationController.mEsportsModeOn = z;
            }
            if (uri == null || OpNotificationController.GAME_MODE_BLOCK_HEADS_UP_URI.equals(uri)) {
                OpNotificationController.this.mGameModeNotifyType = System.getIntForUser(contentResolver, "game_mode_block_notification", 0, -2);
                if (OpNotificationController.this.mGameModeNotifyType != 0) {
                    OpNotificationController.this.mOIMCServiceManager.addFuncRuleGlobal(OIMCRule.RULE_DISABLE_HEADSUPNOTIFICATION);
                } else {
                    OpNotificationController.this.mOIMCServiceManager.removeFuncRuleGlobal(OIMCRule.RULE_DISABLE_HEADSUPNOTIFICATION);
                }
            }
            String str = "OpNotificationController";
            if (uri == null || OpNotificationController.GAME_MODE_3RD_PARTY_CALLS_UID_URI.equals(uri)) {
                OpNotificationController.this.mGameMode3rdPartyCallsUid = System.getStringForUser(contentResolver, "game_mode_notifications_3rd_calls_uid", -2);
                if (OpNotificationController.this.mGameMode3rdPartyCallsUid != null) {
                    if (!"-1".equals(OpNotificationController.this.mGameMode3rdPartyCallsUid)) {
                        OpLsState.getInstance().getPhoneStatusBar().removeHeadsUps();
                        if (OpNotificationController.OP_DEBUG) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("removeHeadsUps for 3rd party app calling uid: ");
                            sb.append(OpNotificationController.this.mGameMode3rdPartyCallsUid);
                            Log.d(str, sb.toString());
                        }
                    }
                }
            }
            if (uri == null || OpNotificationController.OP_QUICKREPLY_IM_LIST_URI.equals(uri)) {
                String stringForUser = System.getStringForUser(contentResolver, "op_quickreply_im_list", -2);
                if (stringForUser != null && !stringForUser.isEmpty()) {
                    OpNotificationController.this.mQuickReplyList = Arrays.asList(stringForUser.split(";"));
                    OpNotificationController.this.setQuickReplyFlags();
                }
                if (OpNotificationController.OP_DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("list= ");
                    sb2.append(stringForUser);
                    Log.d(str, sb2.toString());
                }
            }
            if (uri == null) {
                OpNotificationController.this.mOIMCServiceManager.addFuncRuleGlobal(OIMCRule.RULE_DISABLE_HEADSUPNOTIFICATION_ZEN);
            }
            if (OpNotificationController.OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("update uri: ");
                sb3.append(uri);
                sb3.append(" mBlockedByDriving: ");
                sb3.append(OpNotificationController.this.mBlockedByDriving);
                sb3.append(" mEsportsModeOn: ");
                sb3.append(OpNotificationController.this.mEsportsModeOn);
                sb3.append(" mGameModeNotifyType: ");
                sb3.append(OpNotificationController.this.mGameModeNotifyType);
                sb3.append(" mGameMode3rdPartyCallsUid: ");
                sb3.append(OpNotificationController.this.mGameMode3rdPartyCallsUid);
                Log.d(str, sb3.toString());
            }
        }
    }

    static {
        String str = "com.android.incallui";
        String str2 = "com.android.dialer";
        PRIORITY_LIST_BRICK_MODE = Arrays.asList(new String[]{str, str2});
        String str3 = "com.oneplus.deskclock";
        PRIORITY_LIST_DRIVING_MODE = Arrays.asList(new String[]{str3, str2});
        PRIORITY_LIST_GAME_MODE = Arrays.asList(new String[]{str, str3, str2});
        LIFETIME_EXTENSION_LIST = Arrays.asList(new String[]{str2, "com.whatsapp"});
    }

    public OpNotificationController(Context context) {
        Integer valueOf = Integer.valueOf(0);
        this.mContext = context;
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mConfigurationController = (ConfigurationController) Dependency.get(ConfigurationController.class);
        this.mConfigurationController.addCallback(this);
        DockedStackExistsListener.register(this.mDockedListener);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mMonitorCallback);
        this.mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        this.mOIMCServiceManager = new OIMCServiceManager();
        this.mSimpleHeadsUpController = new SimpleHeadsUpController(this.mContext);
        this.mAppOpsService = Stub.asInterface(ServiceManager.getService("appops"));
        this.mAppOpsActiveCallback = new IAppOpsActiveCallback.Stub() {
            public void opActiveChanged(int i, int i2, String str, boolean z) {
                StringBuilder sb = new StringBuilder();
                sb.append("opActiveChanged, op: ");
                sb.append(i);
                sb.append(", uid: ");
                sb.append(i2);
                sb.append(", packageName: ");
                sb.append(str);
                sb.append(", active: ");
                sb.append(z);
                Log.d("OpNotificationController", sb.toString());
                OpNotificationController.this.preparePrivacyAlertNotification(i, str, z);
                OpNotificationController.this.cancelPrivacyAlertIfNeeded();
            }
        };
        try {
            this.mAppOpsService.startWatchingActive(PRIVACY_ALERT_OPS, this.mAppOpsActiveCallback);
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("AppOpsService: startWatchingMode fail: ");
            sb.append(e.toString());
            Log.d("OpNotificationController", sb.toString());
        }
        this.mNoMan = (NotificationManager) this.mContext.getSystemService("notification");
        this.mPackageManager = this.mContext.getPackageManager();
        NotificationChannel notificationChannel = new NotificationChannel("privacy_alert", this.mContext.getString(84869288), 3);
        notificationChannel.setBlockableSystem(true);
        notificationChannel.enableVibration(false);
        notificationChannel.setSound(null, null);
        notificationChannel.enableLights(false);
        this.mNoMan.createNotificationChannel(notificationChannel);
        this.mPrivacyGroupCount.put("PrivacyAlertGroupCamera", valueOf);
        this.mPrivacyGroupCount.put("PrivacyAlertGroupMicrophone", valueOf);
    }

    public void setEntryManager(NotificationEntryManager notificationEntryManager) {
        this.mEntryManager = notificationEntryManager;
    }

    public int getCallState() {
        return this.mPhoneState;
    }

    public NotificationEntryManager getEntryManager() {
        return this.mEntryManager;
    }

    public int canHeadsUp(StatusBarNotification statusBarNotification) {
        String key = statusBarNotification.getKey();
        String str = "OpNotificationController";
        if (blockedByBrickMode(statusBarNotification.getPackageName())) {
            if (OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No heads up: blocked by brick mode: ");
                sb.append(key);
                Log.d(str, sb.toString());
            }
            return 1;
        } else if (blockedByDrivingMode(statusBarNotification)) {
            if (OP_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("No heads up: blocked by driving mode: ");
                sb2.append(key);
                Log.d(str, sb2.toString());
            }
            return 2;
        } else if (blockedByEsportsMode(statusBarNotification)) {
            if (OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("No heads up: blocked by esports mode: ");
                sb3.append(key);
                Log.d(str, sb3.toString());
            }
            return 3;
        } else if (blockedByGameMode3rdPartyCall(statusBarNotification)) {
            if (OP_DEBUG) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("No heads up: blocked by game mode 3rd party calling, key: ");
                sb4.append(key);
                sb4.append(" uid: ");
                sb4.append(this.mGameMode3rdPartyCallsUid);
                Log.d(str, sb4.toString());
            }
            return 5;
        } else if (blockedByGameMode(statusBarNotification)) {
            if (OP_DEBUG) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("No heads up: blocked by game mode: ");
                sb5.append(key);
                Log.d(str, sb5.toString());
            }
            return 4;
        } else if (!blockedByReadingMode()) {
            return -1;
        } else {
            if (OP_DEBUG) {
                StringBuilder sb6 = new StringBuilder();
                sb6.append("No heads up: blocked by reading mode: ");
                sb6.append(key);
                Log.d(str, sb6.toString());
            }
            return 6;
        }
    }

    public boolean canHeadsUpSnoozedNotification(StatusBarNotification statusBarNotification) {
        return (this.mBlockedByBrick && PRIORITY_LIST_BRICK_MODE.contains(statusBarNotification.getPackageName())) || (this.mBlockedByGame && statusBarNotification.getNotification().fullScreenIntent != null);
    }

    public boolean shouldSuppressFullScreenIntent(NotificationEntry notificationEntry) {
        String str = "OpNotificationController";
        if (blockedByBrickMode(notificationEntry.notification.getPackageName())) {
            if (OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No Fullscreen intent: suppressed by brick mode: ");
                sb.append(notificationEntry.key);
                Log.d(str, sb.toString());
            }
            return true;
        } else if (blockedByDrivingMode(notificationEntry.notification)) {
            if (OP_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("No Fullscreen intent: suppressed by driving mode: ");
                sb2.append(notificationEntry.key);
                Log.d(str, sb2.toString());
            }
            return true;
        } else if (blockedByEsportsMode(notificationEntry.notification)) {
            if (OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("No Fullscreen intent: suppressed by esports mode: ");
                sb3.append(notificationEntry.key);
                Log.d(str, sb3.toString());
            }
            return true;
        } else if (!blockedByGameMode(notificationEntry.notification)) {
            return false;
        } else {
            if (OP_DEBUG) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("No Fullscreen intent: suppressed by game mode: ");
                sb4.append(notificationEntry.key);
                Log.d(str, sb4.toString());
            }
            return true;
        }
    }

    private boolean blockedByBrickMode(String str) {
        return this.mBlockedByBrick && !PRIORITY_LIST_BRICK_MODE.contains(str);
    }

    private boolean blockedByDrivingMode(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        if (!this.mBlockedByDriving) {
            return false;
        }
        if (PRIORITY_LIST_DRIVING_MODE.contains(packageName) && this.mIsKeyguardShowing) {
            return false;
        }
        Bundle bundle = statusBarNotification.getNotification().extras;
        if (bundle != null) {
            return !bundle.getBoolean("oneplus.shouldPeekInCarMode", false);
        }
        return true;
    }

    private boolean blockedByEsportsMode(StatusBarNotification statusBarNotification) {
        return this.mEsportsModeOn && OnePlusSceneCallBlockManagerInjector.isNotificationMutedByESport(statusBarNotification);
    }

    private boolean blockedByGameMode(StatusBarNotification statusBarNotification) {
        if (PRIORITY_LIST_GAME_MODE.contains(statusBarNotification.getPackageName())) {
            return false;
        }
        if ("call".equals(statusBarNotification.getNotification().category)) {
            return false;
        }
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("mBlockedByGame: ");
            sb.append(this.mBlockedByGame);
            sb.append(" type: ");
            sb.append(this.mGameModeNotifyType);
            Log.d("OpNotificationController", sb.toString());
        }
        if (!this.mBlockedByGame || this.mGameModeNotifyType == 0) {
            return false;
        }
        Bundle bundle = statusBarNotification.getNotification().extras;
        if (bundle != null) {
            return !bundle.getBoolean("oneplus.shouldPeekInGameMode", false);
        }
        return true;
    }

    private boolean blockedByGameMode3rdPartyCall(StatusBarNotification statusBarNotification) {
        String str = this.mGameMode3rdPartyCallsUid;
        return str != null && !"-1".equals(str) && !PRIORITY_LIST_GAME_MODE.contains(statusBarNotification.getPackageName());
    }

    private boolean blockedByReadingMode() {
        int intForUser = System.getIntForUser(this.mContext.getContentResolver(), "reading_mode_status", 0, -2);
        return (intForUser == 1 || intForUser == 2) && System.getIntForUser(this.mContext.getContentResolver(), "reading_mode_block_notification", 0, -2) == 1;
    }

    public boolean shouldColorizeIcon(String str) {
        return ICON_COLORIZE_LIST.contains(str);
    }

    public boolean shouldForceRemoveEntry(String str) {
        return LIFETIME_EXTENSION_LIST.contains(str);
    }

    public boolean isPanelDisabledInBrickMode() {
        return this.mBlockedByBrick && !this.mCommandQueue.panelsEnabled();
    }

    public void maybeShowSimpleHeadsUp(int i, StatusBarNotification statusBarNotification) {
        int i2;
        String packageName = statusBarNotification.getPackageName();
        if (i == 4 && this.mBlockedByGame && this.mGameModeNotifyType == 2) {
            if (OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Show simple heads-up: game mode: ");
                sb.append(packageName);
                Log.d("OpNotificationController", sb.toString());
            }
            i2 = 0;
        } else {
            i2 = -1;
        }
        showSimpleHeadsUp(i2, statusBarNotification);
    }

    private void showSimpleHeadsUp(int i, StatusBarNotification statusBarNotification) {
        String key = statusBarNotification.getKey();
        if (i == -1) {
            if (this.mSimpleHeadsUpController.getCurrentKey() != null && this.mSimpleHeadsUpController.getCurrentKey().equals(key)) {
                hideSimpleHeadsUps();
            }
            return;
        }
        this.mSimpleHeadsUpController.show(statusBarNotification, this.mLockscreenUserManager.isSecure() && this.mEntryManager.getNotificationData().isLocked(key), 1);
    }

    public void hideSimpleHeadsUps() {
        SimpleHeadsUpController simpleHeadsUpController = this.mSimpleHeadsUpController;
        if (simpleHeadsUpController != null) {
            simpleHeadsUpController.hide();
        }
    }

    public void updateSimpleHeadsUps() {
        SimpleHeadsUpController simpleHeadsUpController = this.mSimpleHeadsUpController;
        if (simpleHeadsUpController != null) {
            simpleHeadsUpController.onDensityOrFontScaleChanged();
        }
    }

    public boolean keepNotificationLightBlinking(List<NotificationEntry> list) {
        int size = list.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            String packageName = ((NotificationEntry) list.get(i)).notification.getPackageName();
            if ("com.android.dialer".equals(packageName) || "com.android.server.telecom".equals(packageName)) {
                z = true;
            }
        }
        return z;
    }

    public void snoozeHeadsUp(Notification notification) {
        PendingIntent swipeUpHeadsUpIntent = notification.getSwipeUpHeadsUpIntent();
        if (swipeUpHeadsUpIntent != null) {
            try {
                if (OP_DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("snooze ");
                    sb.append(notification);
                    sb.append(" send pending intent ");
                    sb.append(swipeUpHeadsUpIntent);
                    Log.d("OpNotificationController", sb.toString());
                }
                swipeUpHeadsUpIntent.send();
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    public View getQuickReplyView(StatusBarNotification statusBarNotification) {
        View inflate = LayoutInflater.from(this.mContext).inflate(R$layout.op_quick_reply_notification, null);
        Notification notification = statusBarNotification.getNotification();
        ImageView imageView = (ImageView) inflate.findViewById(R$id.app_icon);
        if (imageView != null) {
            imageView.setImageBitmap(getAppIcon(statusBarNotification));
        }
        TextView textView = (TextView) inflate.findViewById(R$id.title);
        if (textView != null) {
            textView.setText(notification.extras.getCharSequence("android.title"));
            textView.setTextColor(Utils.getColorAttr(this.mContext, 16842806));
        }
        TextView textView2 = (TextView) inflate.findViewById(R$id.text);
        if (textView2 != null) {
            textView2.setText(notification.extras.getCharSequence("android.text"));
            textView2.setTextColor(Utils.getColorAttr(this.mContext, 16842808));
        }
        View findViewById = inflate.findViewById(R$id.notification_content);
        if (findViewById != null) {
            findViewById.setOnClickListener(new OnClickListener(statusBarNotification) {
                private final /* synthetic */ StatusBarNotification f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    OpNotificationController.this.lambda$getQuickReplyView$0$OpNotificationController(this.f$1, view);
                }
            });
        }
        View findViewById2 = inflate.findViewById(R$id.btn_reply);
        if (findViewById2 != null) {
            findViewById2.setOnClickListener(new OnClickListener(statusBarNotification) {
                private final /* synthetic */ StatusBarNotification f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    OpNotificationController.this.lambda$getQuickReplyView$1$OpNotificationController(this.f$1, view);
                }
            });
        }
        return inflate;
    }

    public /* synthetic */ void lambda$getQuickReplyView$0$OpNotificationController(StatusBarNotification statusBarNotification, View view) {
        OpMdmLogger.log("landscape_quick_reply", "hun_action", "2", "YLTI9SVG4L");
        sentIntent(statusBarNotification, false);
    }

    public /* synthetic */ void lambda$getQuickReplyView$1$OpNotificationController(StatusBarNotification statusBarNotification, View view) {
        String str = "YLTI9SVG4L";
        String str2 = "1";
        String str3 = "landscape_quick_reply";
        if (OpLsState.getInstance().getPhoneStatusBar().getPresenter().isPresenterFullyCollapsed()) {
            OpMdmLogger.log(str3, "hun_action", str2, str);
        } else {
            OpMdmLogger.log(str3, "nd_action", str2, str);
        }
        sentIntent(statusBarNotification, true);
    }

    private void sentIntent(StatusBarNotification statusBarNotification, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("launch pkg: ");
        sb.append(statusBarNotification.getPackageName());
        sb.append(" userId: ");
        sb.append(statusBarNotification.getUserId());
        sb.append(" in freeform mode: ");
        sb.append(z);
        String str = "OpNotificationController";
        Log.d(str, sb.toString());
        StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
        Notification notification = statusBarNotification.getNotification();
        PendingIntent pendingIntent = notification.contentIntent;
        if (pendingIntent == null) {
            pendingIntent = notification.fullScreenIntent;
        }
        PendingIntent pendingIntent2 = pendingIntent;
        if (pendingIntent2 != null) {
            try {
                ActivityOptions activityOptionsInternal = OpStatusBar.getActivityOptionsInternal(null);
                if (z) {
                    activityOptionsInternal.setLaunchWindowingMode(5);
                }
                Bundle bundle = activityOptionsInternal.toBundle();
                if (OpFeatures.isSupport(new int[]{206})) {
                    bundle.putString("android:activity.packageName", "OP_EXTRA_REMOTE_INPUT_DRAFT");
                }
                int sendAndReturnResult = pendingIntent2.sendAndReturnResult(this.mContext, 0, null, null, null, null, bundle);
                ActivityLaunchAnimator activityLaunchAnimator = phoneStatusBar.getActivityLaunchAnimator();
                if (activityLaunchAnimator != null) {
                    activityLaunchAnimator.setLaunchResult(sendAndReturnResult, true);
                }
                if (phoneStatusBar.getPresenter().isPresenterFullyCollapsed()) {
                    phoneStatusBar.removeHeadsUps();
                }
            } catch (CanceledException e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Sending PendingIntent failed: ");
                sb2.append(e);
                Log.w(str, sb2.toString());
            }
        }
    }

    private Bitmap getAppIcon(StatusBarNotification statusBarNotification) {
        Drawable drawable;
        String str = "OpNotificationController";
        String packageName = statusBarNotification.getPackageName();
        try {
            drawable = this.mContext.getPackageManager().getApplicationIcon(packageName);
        } catch (NameNotFoundException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Quick Reply: Get package fail, ");
            sb.append(e.toString());
            Log.d(str, sb.toString());
            drawable = statusBarNotification.getNotification().getSmallIcon().loadDrawable(this.mContext);
        }
        if (drawable != null) {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            Bitmap createBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, drawable.getOpacity() != -1 ? Config.ARGB_8888 : Config.RGB_565);
            Canvas canvas = new Canvas(createBitmap);
            drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
            drawable.draw(canvas);
            return createBitmap;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Quick Reply: Cannot resolve application icon, pkg: ");
        sb2.append(packageName);
        Log.d(str, sb2.toString());
        return null;
    }

    public boolean supportQuickReply(String str) {
        return !this.mDockedStackExists && isOnQuickReplyList(str) && this.mOrientation == 2;
    }

    public boolean isFreeFormActive() {
        return this.mIsFreeForm;
    }

    public boolean isOnQuickReplyList(String str) {
        return this.mQuickReplyList.contains(str);
    }

    public void setIsFreeForm(boolean z) {
        this.mIsFreeForm = z;
    }

    private void setShowQuickReply(NotificationEntry notificationEntry) {
        String packageName = notificationEntry.notification.getPackageName();
        boolean z = true;
        boolean z2 = (notificationEntry.notification.getNotification().flags & 64) != 0;
        String str = this.mTopActivity;
        boolean z3 = (str == null || packageName == null || !packageName.equals(str)) ? false : true;
        if (!supportQuickReply(packageName) || this.mIsFreeForm || z3 || z2 || notificationEntry.getRow().isContentHidden()) {
            z = false;
        }
        notificationEntry.getRow().setShowQuickReply(z);
    }

    public void setTopActivity(String str) {
        this.mTopActivity = str;
    }

    public void setQuickReplyFlags() {
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        if (notificationEntryManager != null && notificationEntryManager.getNotificationData() != null) {
            Iterator it = this.mEntryManager.getNotificationData().getActiveNotifications().iterator();
            while (it.hasNext()) {
                NotificationEntry notificationEntry = (NotificationEntry) it.next();
                notificationEntry.getRow().reinflateQuickReply(isOnQuickReplyList(notificationEntry.notification.getPackageName()));
                setShowQuickReply(notificationEntry);
            }
        }
    }

    /* access modifiers changed from: private */
    public void preparePrivacyAlertNotification(int i, String str, boolean z) {
        String str2;
        String str3;
        String str4;
        int i2;
        int i3;
        int i4;
        int i5 = i;
        String str5 = str;
        String appName = getAppName(str5);
        String str6 = null;
        boolean z2 = false;
        if (i5 == 26) {
            str6 = this.mContext.getString(R$string.privacy_type_camera);
            i2 = R$drawable.privacy_alert_icon_camera;
            str4 = this.mContext.getString(84869290, new Object[]{appName, str6});
            str2 = this.mContext.getString(84869289);
            str3 = "PrivacyAlertGroupCamera";
            i3 = 1;
        } else if (i5 != 27) {
            str4 = null;
            str3 = null;
            str2 = null;
            i3 = 0;
            i2 = 0;
        } else {
            str6 = this.mContext.getString(R$string.privacy_type_microphone);
            int i6 = R$drawable.privacy_alert_icon_microphone;
            String string = this.mContext.getString(84869290, new Object[]{appName, str6});
            i2 = i6;
            str2 = this.mContext.getString(84869289);
            str3 = "PrivacyAlertGroupMicrophone";
            i3 = 2;
            str4 = string;
        }
        if (isSystemApp(str5) || SYSTEM_APP_LIST.contains(str5)) {
            if (OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("preparePrivacyAlertNotification, stop preparing for system application, packageName: ");
                sb.append(str5);
                sb.append(", privacyName: ");
                sb.append(str6);
                sb.append(", privacyType: ");
                sb.append(i3);
                Log.d("OpNotificationController", sb.toString());
            }
            return;
        }
        int intValue = ((Integer) this.mPrivacyGroupCount.get(str3)).intValue();
        int intValue2 = this.mPrivacyAlertList.containsKey(str5) ? ((Integer) this.mPrivacyAlertList.get(str5)).intValue() : 0;
        if ((intValue2 & i3) == 1) {
            z2 = true;
        }
        if (!z || z2) {
            i4 = intValue2 & (~i3);
            if (intValue > 0) {
                this.mPrivacyGroupCount.put(str3, Integer.valueOf(intValue - 1));
            }
        } else {
            i4 = intValue2 | i3;
            sendPrivacyAlertNotification(str, i3, str3, i2, str4, str2);
            this.mPrivacyGroupCount.put(str3, Integer.valueOf(intValue + 1));
        }
        this.mPrivacyAlertList.put(str5, Integer.valueOf(i4));
    }

    private void sendPrivacyAlertNotification(String str, int i, String str2, int i2, String str3, String str4) {
        String str5 = str;
        int i3 = i;
        String str6 = str2;
        int i4 = i2;
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("sendPrivacyAlertNotification, packageName: ");
            sb.append(str5);
            sb.append(", privacyType: ");
            sb.append(i3);
            sb.append(", group: ");
            sb.append(str6);
            Log.d("OpNotificationController", sb.toString());
        }
        String str7 = "android.substName";
        String str8 = "android.provider.extra.CHANNEL_ID";
        String str9 = "privacy_alert";
        if (((Integer) this.mPrivacyGroupCount.get(str6)).intValue() == 0) {
            Notification build = new Builder(this.mContext, str9).setDefaults(-1).setGroup(str6).setGroupSummary(true).setSmallIcon(i4).setOngoing(true).setVisibility(1).setContentIntent(PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS").setFlags(268435456).putExtra(str8, str9), 134217728)).build();
            build.extras.putString(str7, this.mContext.getString(84869288));
            this.mNoMan.notifyAsUser("PrivacyAlertSummary", i3, build, UserHandle.ALL);
        }
        Notification build2 = new Builder(this.mContext, str9).setDefaults(-1).setGroup(str6).setSmallIcon(i4).setContentTitle(str3).setContentText(str4).setOngoing(true).setVisibility(1).setContentIntent(PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", str5, null)).setFlags(268435456).putExtra(str8, str9), 134217728)).build();
        build2.extras.putString(str7, this.mContext.getString(84869288));
        this.mNoMan.notifyAsUser(str5, i3, build2, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void cancelPrivacyAlertIfNeeded() {
        String str = "PrivacyAlertGroupMicrophone";
        String str2 = "PrivacyAlertGroupCamera";
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("cancelPrivacyAlertIfNeeded, size of mPrivacyAlertList: ");
            sb.append(this.mPrivacyAlertList.size());
            sb.append(", mPrivacyGroupCount(Camera): ");
            sb.append(this.mPrivacyGroupCount.get(str2));
            sb.append(", mPrivacyGroupCount(Microphone): ");
            sb.append(this.mPrivacyGroupCount.get(str));
            Log.d("OpNotificationController", sb.toString());
        }
        Iterator it = this.mPrivacyAlertList.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (entry != null) {
                String str3 = (String) entry.getKey();
                int intValue = ((Integer) entry.getValue()).intValue();
                if ((intValue & 1) == 0) {
                    cancelPrivacyAlert(str3, 1);
                }
                if ((intValue & 2) == 0) {
                    cancelPrivacyAlert(str3, 2);
                }
                if (intValue == 0 && this.mPrivacyAlertList.containsKey(str3)) {
                    it.remove();
                }
            }
        }
        int intValue2 = ((Integer) this.mPrivacyGroupCount.get(str2)).intValue();
        String str4 = "PrivacyAlertSummary";
        if (intValue2 == 0) {
            cancelPrivacyAlert(str4, 1);
        }
        if (((Integer) this.mPrivacyGroupCount.get(str)).intValue() == 0) {
            cancelPrivacyAlert(str4, 2);
        }
    }

    private void cancelPrivacyAlert(String str, int i) {
        if (OP_DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("cancelPrivacyAlert, tag: ");
            sb.append(str);
            sb.append(", privacyType: ");
            sb.append(i);
            Log.d("OpNotificationController", sb.toString());
        }
        this.mNoMan.cancelAsUser(str, i, UserHandle.ALL);
    }

    private boolean isSystemApp(String str) {
        try {
            return this.mPackageManager.getPackageInfo(str, 64).applicationInfo.isSystemApp();
        } catch (NameNotFoundException unused) {
            Log.e("OpNotificationController", "cacheIsSystemNotification: Could not find package info");
            return false;
        }
    }

    private String getAppName(String str) {
        try {
            String charSequence = this.mPackageManager.getApplicationLabel(this.mPackageManager.getApplicationInfo(str, 0)).toString();
            if (charSequence == null) {
                charSequence = "";
            }
            return charSequence;
        } catch (NameNotFoundException unused) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to find app name for ");
            sb.append(str);
            Log.e("OpNotificationController", sb.toString());
            return null;
        }
    }

    public void updateNotificationRule() {
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver != null) {
            settingsObserver.update(null);
        }
        OimcObserver oimcObserver = this.mOimcObserver;
        if (oimcObserver != null) {
            oimcObserver.update(null);
        }
    }

    public void onConfigChanged(Configuration configuration) {
        int i = this.mOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mOrientation = i2;
            hideSimpleHeadsUps();
            setQuickReplyFlags();
        }
    }
}
