package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.NotificationLockscreenUserManager.UserChangedListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.oneplus.systemui.statusbar.OpNotificationLockscreenUserManagerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NotificationLockscreenUserManagerImpl extends OpNotificationLockscreenUserManagerImpl implements Dumpable, NotificationLockscreenUserManager, StateListener {
    protected final BroadcastReceiver mAllUsersReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction()) && NotificationLockscreenUserManagerImpl.this.isCurrentProfile(getSendingUserId())) {
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingPrivateNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
                NotificationLockscreenUserManagerImpl notificationLockscreenUserManagerImpl = NotificationLockscreenUserManagerImpl.this;
                notificationLockscreenUserManagerImpl.setSecure(notificationLockscreenUserManagerImpl.mLockPatternUtils, NotificationLockscreenUserManagerImpl.this.mCurrentUserId);
            }
        }
    };
    private boolean mAllowLockscreenRemoteInput;
    /* access modifiers changed from: private */
    public final IStatusBarService mBarService;
    protected final BroadcastReceiver mBaseBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                NotificationLockscreenUserManagerImpl.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                NotificationLockscreenUserManagerImpl.this.updateCurrentProfilesCache();
                StringBuilder sb = new StringBuilder();
                sb.append("userId ");
                sb.append(NotificationLockscreenUserManagerImpl.this.mCurrentUserId);
                sb.append(" is in the house");
                Log.v("LockscreenUserManager", sb.toString());
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.updatePublicMode();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().filterAndSort();
                NotificationLockscreenUserManagerImpl notificationLockscreenUserManagerImpl = NotificationLockscreenUserManagerImpl.this;
                notificationLockscreenUserManagerImpl.mPresenter.onUserSwitched(notificationLockscreenUserManagerImpl.mCurrentUserId);
                NotificationLockscreenUserManagerImpl notificationLockscreenUserManagerImpl2 = NotificationLockscreenUserManagerImpl.this;
                notificationLockscreenUserManagerImpl2.setSecure(notificationLockscreenUserManagerImpl2.mLockPatternUtils, NotificationLockscreenUserManagerImpl.this.mCurrentUserId);
                for (UserChangedListener onUserChanged : NotificationLockscreenUserManagerImpl.this.mListeners) {
                    onUserChanged.onUserChanged(NotificationLockscreenUserManagerImpl.this.mCurrentUserId);
                }
            } else if ("android.intent.action.USER_ADDED".equals(action)) {
                NotificationLockscreenUserManagerImpl.this.updateCurrentProfilesCache();
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).startConnectionToCurrentUser();
            } else if ("com.android.systemui.statusbar.work_challenge_unlocked_notification_action".equals(action)) {
                IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
                String stringExtra = intent.getStringExtra("android.intent.extra.INDEX");
                if (intentSender != null) {
                    try {
                        NotificationLockscreenUserManagerImpl.this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
                    } catch (SendIntentException unused) {
                    }
                }
                if (stringExtra != null) {
                    try {
                        NotificationLockscreenUserManagerImpl.this.mBarService.onNotificationClick(stringExtra, NotificationVisibility.obtain(stringExtra, NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().getRank(stringExtra), NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().getActiveNotifications().size(), true, NotificationLogger.getNotificationLocation(NotificationLockscreenUserManagerImpl.this.getEntryManager().getNotificationData().get(stringExtra))));
                    } catch (RemoteException unused2) {
                    }
                }
            }
        }
    };
    protected final Context mContext;
    protected final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();
    protected int mCurrentUserId = 0;
    private final DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public final DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private NotificationEntryManager mEntryManager;
    protected KeyguardManager mKeyguardManager;
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    /* access modifiers changed from: private */
    public final List<UserChangedListener> mListeners = new ArrayList();
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    private final SparseBooleanArray mLockscreenPublicMode = new SparseBooleanArray();
    protected ContentObserver mLockscreenSettingsObserver;
    protected NotificationPresenter mPresenter;
    protected ContentObserver mSettingsObserver;
    private boolean mShowLockscreenNotifications;
    private int mState = 0;
    private final UserManager mUserManager;
    /* access modifiers changed from: private */
    public final SparseBooleanArray mUsersAllowingNotifications = new SparseBooleanArray();
    /* access modifiers changed from: private */
    public final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    private final SparseBooleanArray mUsersWithSeperateWorkChallenge = new SparseBooleanArray();

    /* access modifiers changed from: private */
    public NotificationEntryManager getEntryManager() {
        if (this.mEntryManager == null) {
            this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        }
        return this.mEntryManager;
    }

    public NotificationLockscreenUserManagerImpl(Context context) {
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mBarService = Stub.asInterface(ServiceManager.getService("statusbar"));
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        setSecure(new LockPatternUtils(this.mContext), this.mCurrentUserId);
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter) {
        this.mPresenter = notificationPresenter;
        this.mLockscreenSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            public void onChange(boolean z) {
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingPrivateNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.mUsersAllowingNotifications.clear();
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
            }
        };
        this.mSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            public void onChange(boolean z) {
                NotificationLockscreenUserManagerImpl.this.updateLockscreenNotificationSetting();
                if (NotificationLockscreenUserManagerImpl.this.mDeviceProvisionedController.isDeviceProvisioned()) {
                    NotificationLockscreenUserManagerImpl.this.getEntryManager().updateNotifications();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("lock_screen_show_notifications"), false, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.registerReceiverAsUser(this.mAllUsersReceiver, UserHandle.ALL, new IntentFilter("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED"), null, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        this.mContext.registerReceiver(this.mBaseBroadcastReceiver, intentFilter2, "com.android.systemui.permission.SELF", null);
        updateCurrentProfilesCache();
        this.mSettingsObserver.onChange(false);
    }

    public boolean shouldShowLockscreenNotifications() {
        return this.mShowLockscreenNotifications;
    }

    public boolean shouldAllowLockscreenRemoteInput() {
        return this.mAllowLockscreenRemoteInput;
    }

    public boolean isCurrentProfile(int i) {
        boolean z;
        synchronized (this.mCurrentProfiles) {
            if (i != -1) {
                try {
                    if (this.mCurrentProfiles.get(i) == null) {
                        z = false;
                    }
                } finally {
                }
            }
            z = true;
        }
        return z;
    }

    private boolean shouldTemporarilyHideNotifications(int i) {
        if (i == -1) {
            i = this.mCurrentUserId;
        }
        return KeyguardUpdateMonitor.getInstance(this.mContext).isUserInLockdown(i);
    }

    public boolean shouldHideNotifications(int i) {
        if (!isLockscreenPublicMode(i) || userAllowsNotificationsInPublic(i)) {
            int i2 = this.mCurrentUserId;
            if ((i == i2 || !shouldHideNotifications(i2)) && !shouldTemporarilyHideNotifications(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldHideNotifications(String str) {
        boolean z = true;
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return true;
        }
        if (!isLockscreenPublicMode(this.mCurrentUserId) || getEntryManager().getNotificationData().getVisibilityOverride(str) != -1) {
            z = false;
        }
        return z;
    }

    public boolean shouldShowOnKeyguard(StatusBarNotification statusBarNotification) {
        boolean z;
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return false;
        }
        if (!NotificationUtils.useNewInterruptionModel(this.mContext) || !hideSilentNotificationsOnLockscreen()) {
            z = !getEntryManager().getNotificationData().isAmbient(statusBarNotification.getKey());
        } else {
            z = getEntryManager().getNotificationData().isHighPriority(statusBarNotification);
        }
        return shouldShowOnKeyguardInternal(this.mEntryManager, statusBarNotification, this.mShowLockscreenNotifications, z);
    }

    private boolean hideSilentNotificationsOnLockscreen() {
        return Secure.getInt(this.mContext.getContentResolver(), "lock_screen_show_silent_notifications", 1) == 0;
    }

    private void setShowLockscreenNotifications(boolean z) {
        this.mShowLockscreenNotifications = z;
    }

    private void setLockscreenAllowRemoteInput(boolean z) {
        this.mAllowLockscreenRemoteInput = z;
    }

    /* access modifiers changed from: protected */
    public void updateLockscreenNotificationSetting() {
        boolean z = true;
        boolean z2 = Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        boolean z3 = (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId) & 4) == 0;
        if (!z2 || !z3) {
            z = false;
        }
        setShowLockscreenNotifications(z);
        setLockscreenAllowRemoteInput(false);
    }

    public boolean userAllowsPrivateNotificationsInPublic(int i) {
        boolean z = true;
        if (i == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(i) >= 0) {
            return this.mUsersAllowingPrivateNotifications.get(i);
        }
        boolean z2 = Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, i) != 0;
        boolean adminAllowsKeyguardFeature = adminAllowsKeyguardFeature(i, 8);
        if (!z2 || !adminAllowsKeyguardFeature) {
            z = false;
        }
        this.mUsersAllowingPrivateNotifications.append(i, z);
        return z;
    }

    private boolean adminAllowsKeyguardFeature(int i, int i2) {
        boolean z = true;
        if (i == -1) {
            return true;
        }
        if ((this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, i) & i2) != 0) {
            z = false;
        }
        return z;
    }

    public void setLockscreenPublicMode(boolean z, int i) {
        this.mLockscreenPublicMode.put(i, z);
    }

    public boolean isLockscreenPublicMode(int i) {
        if (i == -1) {
            return this.mLockscreenPublicMode.get(this.mCurrentUserId, false);
        }
        return this.mLockscreenPublicMode.get(i, false);
    }

    public boolean needsSeparateWorkChallenge(int i) {
        return this.mUsersWithSeperateWorkChallenge.get(i, false);
    }

    private boolean userAllowsNotificationsInPublic(int i) {
        boolean z = true;
        if (isCurrentProfile(i) && i != this.mCurrentUserId) {
            return true;
        }
        if (this.mUsersAllowingNotifications.indexOfKey(i) >= 0) {
            return this.mUsersAllowingNotifications.get(i);
        }
        boolean z2 = Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0, i) != 0;
        boolean adminAllowsKeyguardFeature = adminAllowsKeyguardFeature(i, 4);
        boolean privateNotificationsAllowed = this.mKeyguardManager.getPrivateNotificationsAllowed();
        if (!z2 || !adminAllowsKeyguardFeature || !privateNotificationsAllowed) {
            z = false;
        }
        this.mUsersAllowingNotifications.append(i, z);
        return z;
    }

    public boolean needsRedaction(NotificationEntry notificationEntry) {
        boolean z = (userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) ^ true) || (userAllowsPrivateNotificationsInPublic(notificationEntry.notification.getUserId()) ^ true);
        boolean z2 = notificationEntry.notification.getNotification().visibility == 0;
        if (packageHasVisibilityOverride(notificationEntry.notification.getKey())) {
            return true;
        }
        if (!z2 || !z) {
            return false;
        }
        return true;
    }

    private boolean packageHasVisibilityOverride(String str) {
        boolean z = true;
        if (getEntryManager() == null) {
            Log.wtf("LockscreenUserManager", "mEntryManager was null!", new Throwable());
            return true;
        }
        if (getEntryManager().getNotificationData().getVisibilityOverride(str) != 0) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void updateCurrentProfilesCache() {
        synchronized (this.mCurrentProfiles) {
            this.mCurrentProfiles.clear();
            if (this.mUserManager != null) {
                for (UserInfo userInfo : this.mUserManager.getProfiles(this.mCurrentUserId)) {
                    this.mCurrentProfiles.put(userInfo.id, userInfo);
                }
            }
        }
    }

    public boolean isAnyProfilePublicMode() {
        for (int size = this.mCurrentProfiles.size() - 1; size >= 0; size--) {
            if (isLockscreenPublicMode(((UserInfo) this.mCurrentProfiles.valueAt(size)).id)) {
                return true;
            }
        }
        return false;
    }

    public int getCurrentUserId() {
        return this.mCurrentUserId;
    }

    public SparseArray<UserInfo> getCurrentProfiles() {
        return this.mCurrentProfiles;
    }

    public void onStateChanged(int i) {
        this.mState = i;
        updatePublicMode();
    }

    public void updatePublicMode() {
        boolean z = this.mState != 0 || this.mKeyguardMonitor.isShowing();
        boolean z2 = z && isSecure(getCurrentUserId());
        SparseArray currentProfiles = getCurrentProfiles();
        this.mUsersWithSeperateWorkChallenge.clear();
        for (int size = currentProfiles.size() - 1; size >= 0; size--) {
            int i = ((UserInfo) currentProfiles.valueAt(size)).id;
            boolean isSeparateProfileChallengeEnabled = this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i);
            boolean z3 = (z2 || i == getCurrentUserId() || !isSeparateProfileChallengeEnabled || !isSecure(i)) ? z2 : z || this.mKeyguardManager.isDeviceLocked(i);
            setLockscreenPublicMode(z3, i);
            this.mUsersWithSeperateWorkChallenge.put(i, isSeparateProfileChallengeEnabled);
        }
        getEntryManager().updateNotifications();
    }

    private boolean isSecure(int i) {
        return this.mKeyguardMonitor.isSecure() || this.mLockPatternUtils.isSecure(i);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationLockscreenUserManager state:");
        printWriter.print("  mCurrentUserId=");
        printWriter.println(this.mCurrentUserId);
        printWriter.print("  mShowLockscreenNotifications=");
        printWriter.println(this.mShowLockscreenNotifications);
        printWriter.print("  mAllowLockscreenRemoteInput=");
        printWriter.println(this.mAllowLockscreenRemoteInput);
        printWriter.print("  mCurrentProfiles=");
        for (int size = this.mCurrentProfiles.size() - 1; size >= 0; size--) {
            int i = ((UserInfo) this.mCurrentProfiles.valueAt(size)).id;
            StringBuilder sb = new StringBuilder();
            sb.append("");
            sb.append(i);
            sb.append(" ");
            printWriter.print(sb.toString());
        }
        printWriter.println();
        super.dump(fileDescriptor, printWriter, strArr);
    }
}
