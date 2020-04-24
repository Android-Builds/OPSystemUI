package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.drawable.UserIcons;
import com.android.systemui.Dumpable;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.Prefs;
import com.android.systemui.R$bool;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.p007qs.tiles.UserDetailView;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import com.oneplus.systemui.statusbar.phone.OpSystemUIDialog;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController implements Dumpable {
    private final ActivityStarter mActivityStarter;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private Dialog mAddUserDialog;
    /* access modifiers changed from: private */
    public boolean mAddUsersWhenLocked;
    private final Callback mCallback = new Callback() {
        public void onKeyguardShowingChanged() {
            if (!UserSwitcherController.this.mKeyguardMonitor.isShowing()) {
                UserSwitcherController userSwitcherController = UserSwitcherController.this;
                userSwitcherController.mHandler.post(new Runnable() {
                    public final void run() {
                        UserSwitcherController.this.notifyAdapters();
                    }
                });
                return;
            }
            UserSwitcherController.this.notifyAdapters();
        }
    };
    protected final Context mContext;
    /* access modifiers changed from: private */
    public Dialog mExitGuestDialog;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    protected final Handler mHandler;
    /* access modifiers changed from: private */
    public final KeyguardMonitor mKeyguardMonitor;
    /* access modifiers changed from: private */
    public int mLastNonGuestUser = 0;
    /* access modifiers changed from: private */
    public boolean mPauseRefreshUsers;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        private int mCallState;

        public void onCallStateChanged(int i, String str) {
            if (this.mCallState != i) {
                this.mCallState = i;
                UserSwitcherController.this.refreshUsers(-10000);
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = "android.intent.extra.user_handle";
            boolean z = false;
            int i = -10000;
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                int intExtra = intent.getIntExtra(str, -1);
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(intExtra);
                int size = UserSwitcherController.this.mUsers.size();
                int i2 = 0;
                while (i2 < size) {
                    UserRecord userRecord = (UserRecord) UserSwitcherController.this.mUsers.get(i2);
                    UserInfo userInfo2 = userRecord.info;
                    if (userInfo2 != null) {
                        boolean z2 = userInfo2.id == intExtra;
                        if (userRecord.isCurrent != z2) {
                            UserSwitcherController.this.mUsers.set(i2, userRecord.copyWithIsCurrent(z2));
                        }
                        if (z2 && !userRecord.isGuest) {
                            UserSwitcherController.this.mLastNonGuestUser = userRecord.info.id;
                        }
                        if ((userInfo == null || !userInfo.isAdmin()) && userRecord.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i2);
                            i2--;
                        }
                    }
                    i2++;
                }
                UserSwitcherController.this.notifyAdapters();
                if (UserSwitcherController.this.mSecondaryUser != -10000) {
                    context.stopServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(UserSwitcherController.this.mSecondaryUser));
                    UserSwitcherController.this.mSecondaryUser = -10000;
                }
                if (!(userInfo == null || userInfo.id == 0)) {
                    context.startServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(userInfo.id));
                    UserSwitcherController.this.mSecondaryUser = userInfo.id;
                }
                z = true;
            } else {
                if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                    i = intent.getIntExtra(str, -10000);
                } else {
                    if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && intent.getIntExtra(str, -10000) != 0) {
                        return;
                    }
                }
            }
            UserSwitcherController.this.refreshUsers(i);
            if (z) {
                UserSwitcherController.this.mUnpauseRefreshUsers.run();
            }
        }
    };
    private boolean mResumeUserOnGuestLogout = true;
    /* access modifiers changed from: private */
    public int mSecondaryUser = -10000;
    /* access modifiers changed from: private */
    public Intent mSecondaryUserServiceIntent;
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean z) {
            UserSwitcherController userSwitcherController = UserSwitcherController.this;
            boolean z2 = false;
            userSwitcherController.mSimpleUserSwitcher = Global.getInt(userSwitcherController.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0;
            UserSwitcherController userSwitcherController2 = UserSwitcherController.this;
            if (Global.getInt(userSwitcherController2.mContext.getContentResolver(), "add_users_when_locked", 0) != 0) {
                z2 = true;
            }
            userSwitcherController2.mAddUsersWhenLocked = z2;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    /* access modifiers changed from: private */
    public boolean mSimpleUserSwitcher;
    /* access modifiers changed from: private */
    public final Runnable mUnpauseRefreshUsers = new Runnable() {
        public void run() {
            UserSwitcherController.this.mHandler.removeCallbacks(this);
            UserSwitcherController.this.mPauseRefreshUsers = false;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    protected final UserManager mUserManager;
    /* access modifiers changed from: private */
    public ArrayList<UserRecord> mUsers = new ArrayList<>();
    public final DetailAdapter userDetailAdapter = new DetailAdapter() {
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        public int getMetricsCategory() {
            return 125;
        }

        public Boolean getToggleState() {
            return null;
        }

        public void setToggleState(boolean z) {
        }

        public CharSequence getTitle() {
            return UserSwitcherController.this.mContext.getString(R$string.quick_settings_user_title);
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            UserDetailView userDetailView;
            if (!(view instanceof UserDetailView)) {
                userDetailView = UserDetailView.inflate(context, viewGroup, false);
                userDetailView.createAndSetAdapter(UserSwitcherController.this);
            } else {
                userDetailView = (UserDetailView) view;
            }
            userDetailView.refreshAdapter();
            return userDetailView;
        }

        public Intent getSettingsIntent() {
            return this.USER_SETTINGS_INTENT;
        }
    };

    private final class AddUserDialog extends OpSystemUIDialog implements OnClickListener {
        public AddUserDialog(Context context) {
            super(context);
            setTitle(R$string.user_add_user_title);
            setMessage(context.getString(R$string.user_add_user_message_short));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(17039370), this);
            OpSystemUIDialog.setWindowOnTop(this);
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                cancel();
            } else {
                dismiss();
                if (!ActivityManager.isUserAMonkey()) {
                    new Thread() {
                        public void run() {
                            String str = "UserSwitcherController";
                            Log.d(str, "switchTo:createUser:START");
                            UserSwitcherController userSwitcherController = UserSwitcherController.this;
                            UserInfo createUser = userSwitcherController.mUserManager.createUser(userSwitcherController.mContext.getString(R$string.user_new_user_name), 0);
                            Log.d(str, "switchTo:createUser:END");
                            if (createUser != null) {
                                int i = createUser.id;
                                UserSwitcherController.this.mUserManager.setUserIcon(i, UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(UserSwitcherController.this.mContext.getResources(), i, false)));
                                UserSwitcherController.this.switchToUserId(i);
                            }
                        }
                    }.start();
                }
            }
        }
    }

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;
        private final KeyguardMonitor mKeyguardMonitor;
        private final UnlockMethodCache mUnlockMethodCache;

        public long getItemId(int i) {
            return (long) i;
        }

        protected BaseUserAdapter(UserSwitcherController userSwitcherController) {
            this.mController = userSwitcherController;
            this.mKeyguardMonitor = userSwitcherController.mKeyguardMonitor;
            this.mUnlockMethodCache = UnlockMethodCache.getInstance(userSwitcherController.mContext);
            userSwitcherController.addAdapter(new WeakReference(this));
        }

        public int getUserCount() {
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mUnlockMethodCache.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int size = this.mController.getUsers().size();
            int i = 0;
            for (int i2 = 0; i2 < size; i2++) {
                if (!((UserRecord) this.mController.getUsers().get(i2)).isGuest) {
                    if (((UserRecord) this.mController.getUsers().get(i2)).isRestricted) {
                        break;
                    }
                    i++;
                }
            }
            return i;
        }

        public int getCount() {
            int i = 0;
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mUnlockMethodCache.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int size = this.mController.getUsers().size();
            int i2 = 0;
            while (i < size && !((UserRecord) this.mController.getUsers().get(i)).isRestricted) {
                i2++;
                i++;
            }
            return i2;
        }

        public UserRecord getItem(int i) {
            return (UserRecord) this.mController.getUsers().get(i);
        }

        public void switchTo(UserRecord userRecord) {
            this.mController.switchTo(userRecord);
        }

        public String getName(Context context, UserRecord userRecord) {
            if (userRecord.isGuest) {
                if (userRecord.isCurrent) {
                    return context.getString(R$string.guest_exit_guest);
                }
                return context.getString(userRecord.info == null ? R$string.guest_new_guest : R$string.guest_nickname);
            } else if (userRecord.isAddUser) {
                return context.getString(R$string.user_add_user);
            } else {
                return userRecord.info.name;
            }
        }

        public Drawable getDrawable(Context context, UserRecord userRecord) {
            if (userRecord.isAddUser) {
                return context.getDrawable(R$drawable.ic_add_circle_qs);
            }
            return UserIcons.getDefaultUserIcon(context.getResources(), userRecord.resolveId(), false);
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    private final class ExitGuestDialog extends OpSystemUIDialog implements OnClickListener {
        private final int mGuestId;
        private final int mTargetId;

        public ExitGuestDialog(Context context, int i, int i2) {
            super(context);
            setTitle(R$string.guest_exit_guest_dialog_title);
            setMessage(context.getString(R$string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(R$string.guest_exit_guest_dialog_remove), this);
            OpSystemUIDialog.setWindowOnTop(this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = i;
            this.mTargetId = i2;
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                cancel();
                return;
            }
            dismiss();
            UserSwitcherController.this.exitGuest(this.mGuestId, this.mTargetId);
        }
    }

    public static final class UserRecord {
        public EnforcedAdmin enforcedAdmin;
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public boolean isDisabledByAdmin;
        public final boolean isGuest;
        public final boolean isRestricted;
        public boolean isStorageInsufficient;
        public boolean isSwitchToEnabled;
        public final Bitmap picture;

        public UserRecord(UserInfo userInfo, Bitmap bitmap, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
            this.info = userInfo;
            this.picture = bitmap;
            this.isGuest = z;
            this.isCurrent = z2;
            this.isAddUser = z3;
            this.isRestricted = z4;
            this.isSwitchToEnabled = z5;
        }

        public UserRecord copyWithIsCurrent(boolean z) {
            UserRecord userRecord = new UserRecord(this.info, this.picture, this.isGuest, z, this.isAddUser, this.isRestricted, this.isSwitchToEnabled);
            return userRecord;
        }

        public int resolveId() {
            if (!this.isGuest) {
                UserInfo userInfo = this.info;
                if (userInfo != null) {
                    return userInfo.id;
                }
            }
            return -10000;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"");
                sb.append(this.info.name);
                sb.append("\" id=");
                sb.append(this.info.id);
            } else if (this.isGuest) {
                sb.append("<add guest placeholder>");
            } else if (this.isAddUser) {
                sb.append("<add user placeholder>");
            }
            if (this.isGuest) {
                sb.append(" <isGuest>");
            }
            if (this.isAddUser) {
                sb.append(" <isAddUser>");
            }
            if (this.isCurrent) {
                sb.append(" <isCurrent>");
            }
            if (this.picture != null) {
                sb.append(" <hasPicture>");
            }
            if (this.isRestricted) {
                sb.append(" <isRestricted>");
            }
            if (this.isDisabledByAdmin) {
                sb.append(" <isDisabledByAdmin>");
                sb.append(" enforcedAdmin=");
                sb.append(this.enforcedAdmin);
            }
            if (this.isSwitchToEnabled) {
                sb.append(" <isSwitchToEnabled>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler, ActivityStarter activityStarter) {
        this.mContext = context;
        if (!UserManager.isGuestUserEphemeral()) {
            this.mGuestResumeSessionReceiver.register(context);
        }
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mActivityStarter = activityStarter;
        this.mUserManager = UserManager.get(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, intentFilter, null, null);
        this.mSecondaryUserServiceIntent = new Intent(context, SystemUISecondaryUserService.class);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, new IntentFilter(), "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("allow_user_switching_when_system_user_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        listenForCallState();
        refreshUsers(-10000);
    }

    /* access modifiers changed from: private */
    public void refreshUsers(int i) {
        if (i != -10000) {
            this.mForcePictureLoadForUserId.put(i, true);
        }
        if (!this.mPauseRefreshUsers) {
            boolean z = this.mForcePictureLoadForUserId.get(-1);
            SparseArray sparseArray = new SparseArray(this.mUsers.size());
            int size = this.mUsers.size();
            for (int i2 = 0; i2 < size; i2++) {
                UserRecord userRecord = (UserRecord) this.mUsers.get(i2);
                if (!(userRecord == null || userRecord.picture == null)) {
                    UserInfo userInfo = userRecord.info;
                    if (userInfo != null && !z && !this.mForcePictureLoadForUserId.get(userInfo.id)) {
                        sparseArray.put(userRecord.info.id, userRecord.picture);
                    }
                }
            }
            this.mForcePictureLoadForUserId.clear();
            final boolean z2 = this.mAddUsersWhenLocked;
            new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
                /* access modifiers changed from: protected */
                public ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... sparseArrayArr) {
                    int i;
                    int i2 = 0;
                    SparseArray<Bitmap> sparseArray = sparseArrayArr[0];
                    List<UserInfo> users = UserSwitcherController.this.mUserManager.getUsers(true);
                    UserRecord userRecord = null;
                    if (users == null) {
                        return null;
                    }
                    ArrayList<UserRecord> arrayList = new ArrayList<>(users.size());
                    int currentUser = ActivityManager.getCurrentUser();
                    boolean canSwitchUsers = UserSwitcherController.this.mUserManager.canSwitchUsers();
                    UserInfo userInfo = null;
                    for (UserInfo userInfo2 : users) {
                        boolean z = currentUser == userInfo2.id;
                        UserInfo userInfo3 = z ? userInfo2 : userInfo;
                        boolean z2 = canSwitchUsers || z;
                        if (userInfo2.isEnabled()) {
                            if (userInfo2.isGuest()) {
                                userRecord = new UserRecord(userInfo2, null, true, z, false, false, canSwitchUsers);
                            } else if (userInfo2.supportsSwitchToByUser()) {
                                Bitmap bitmap = (Bitmap) sparseArray.get(userInfo2.id);
                                if (bitmap == null) {
                                    bitmap = UserSwitcherController.this.mUserManager.getUserIcon(userInfo2.id);
                                    if (bitmap != null) {
                                        int dimensionPixelSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(R$dimen.max_avatar_size);
                                        bitmap = Bitmap.createScaledBitmap(bitmap, dimensionPixelSize, dimensionPixelSize, true);
                                    }
                                }
                                Bitmap bitmap2 = bitmap;
                                if (z) {
                                    i = 0;
                                } else {
                                    i = arrayList.size();
                                }
                                UserRecord userRecord2 = new UserRecord(userInfo2, bitmap2, false, z, false, false, z2);
                                arrayList.add(i, userRecord2);
                            }
                        }
                        userInfo = userInfo3;
                    }
                    String str = "HasSeenMultiUser";
                    if (arrayList.size() > 1 || userRecord != null) {
                        Prefs.putBoolean(UserSwitcherController.this.mContext, str, true);
                    } else {
                        Prefs.putBoolean(UserSwitcherController.this.mContext, str, false);
                    }
                    boolean z3 = !UserSwitcherController.this.mUserManager.hasBaseUserRestriction("no_add_user", UserHandle.SYSTEM);
                    boolean z4 = userInfo != null && (userInfo.isAdmin() || userInfo.id == 0) && z3;
                    boolean z5 = z3 && z2;
                    boolean z6 = (z4 || z5) && userRecord == null;
                    boolean z7 = (z4 || z5) && UserSwitcherController.this.mUserManager.canAddMoreUsers();
                    boolean z8 = !z2;
                    if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                        if (userRecord != null) {
                            if (!userRecord.isCurrent) {
                                i2 = arrayList.size();
                            }
                            arrayList.add(i2, userRecord);
                        } else if (z6) {
                            UserRecord userRecord3 = new UserRecord(null, null, true, false, false, z8, canSwitchUsers);
                            UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord3);
                            arrayList.add(userRecord3);
                        }
                    }
                    if (!UserSwitcherController.this.mSimpleUserSwitcher && z7) {
                        UserRecord userRecord4 = new UserRecord(null, null, false, false, true, z8, canSwitchUsers);
                        UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord4);
                        arrayList.add(userRecord4);
                    }
                    return arrayList;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(ArrayList<UserRecord> arrayList) {
                    if (arrayList != null) {
                        UserSwitcherController.this.mUsers = arrayList;
                        UserSwitcherController.this.notifyAdapters();
                    }
                }
            }.execute(new SparseArray[]{sparseArray});
        }
    }

    private void pauseRefreshUsers() {
        if (!this.mPauseRefreshUsers) {
            this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000);
            this.mPauseRefreshUsers = true;
        }
    }

    /* access modifiers changed from: private */
    public void notifyAdapters() {
        for (int size = this.mAdapters.size() - 1; size >= 0; size--) {
            BaseUserAdapter baseUserAdapter = (BaseUserAdapter) ((WeakReference) this.mAdapters.get(size)).get();
            if (baseUserAdapter != null) {
                baseUserAdapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(size);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public boolean useFullscreenUserSwitcher() {
        int i = System.getInt(this.mContext.getContentResolver(), "enable_fullscreen_user_switcher", -1);
        if (i == -1) {
            return this.mContext.getResources().getBoolean(R$bool.config_enableFullscreenUserSwitcher);
        }
        return i != 0;
    }

    public void switchTo(UserRecord userRecord) {
        if (userRecord.isGuest && userRecord.info == null) {
            new Thread() {
                public void run() {
                    String str = "UserSwitcherController";
                    Log.d(str, "switchTo:createGuest:START");
                    UserSwitcherController userSwitcherController = UserSwitcherController.this;
                    UserManager userManager = userSwitcherController.mUserManager;
                    Context context = userSwitcherController.mContext;
                    UserInfo createGuest = userManager.createGuest(context, context.getString(R$string.guest_nickname));
                    Log.d(str, "switchTo:createGuest:END");
                    if (createGuest != null) {
                        UserSwitcherController.this.switchToUserId(createGuest.id);
                    }
                }
            }.start();
        } else if (userRecord.isAddUser) {
            showAddUserDialog();
        } else {
            int i = userRecord.info.id;
            int currentUser = ActivityManager.getCurrentUser();
            if (currentUser == i) {
                if (userRecord.isGuest) {
                    showExitGuestDialog(i);
                }
                return;
            }
            if (UserManager.isGuestUserEphemeral()) {
                UserInfo userInfo = this.mUserManager.getUserInfo(currentUser);
                if (userInfo != null && userInfo.isGuest()) {
                    showExitGuestDialog(currentUser, userRecord.resolveId());
                    return;
                }
            }
            switchToUserId(i);
        }
    }

    public int getSwitchableUserCount() {
        int size = this.mUsers.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = ((UserRecord) this.mUsers.get(i2)).info;
            if (userInfo != null && userInfo.supportsSwitchToByUser()) {
                i++;
            }
        }
        return i;
    }

    /* access modifiers changed from: protected */
    public void switchToUserId(int i) {
        String str = "UserSwitcherController";
        Log.d(str, "switchToUserId START");
        try {
            if (OpUtils.isCustomFingerprint()) {
                KeyguardUpdateMonitor.getInstance(this.mContext).earlyNotifySwitchingUser();
            }
            pauseRefreshUsers();
            ActivityManager.getService().switchUser(i);
        } catch (RemoteException e) {
            Log.e(str, "Couldn't switch user.", e);
        }
        Log.d(str, "switchToUserId END");
    }

    private void showExitGuestDialog(int i) {
        int i2;
        if (this.mResumeUserOnGuestLogout) {
            int i3 = this.mLastNonGuestUser;
            if (i3 != 0) {
                UserInfo userInfo = this.mUserManager.getUserInfo(i3);
                if (userInfo != null && userInfo.isEnabled() && userInfo.supportsSwitchToByUser()) {
                    i2 = userInfo.id;
                    showExitGuestDialog(i, i2);
                }
            }
        }
        i2 = 0;
        showExitGuestDialog(i, i2);
    }

    /* access modifiers changed from: protected */
    public void showExitGuestDialog(int i, int i2) {
        Dialog dialog = this.mExitGuestDialog;
        if (dialog != null && dialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, i, i2);
        this.mExitGuestDialog.show();
    }

    public void showAddUserDialog() {
        Dialog dialog = this.mAddUserDialog;
        if (dialog != null && dialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        this.mAddUserDialog = new AddUserDialog(this.mContext);
        this.mAddUserDialog.show();
    }

    /* access modifiers changed from: protected */
    public void exitGuest(int i, int i2) {
        switchToUserId(i2);
        this.mUserManager.removeUser(i);
    }

    private void listenForCallState() {
        TelephonyManager.from(this.mContext).listen(this.mPhoneStateListener, 32);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UserSwitcherController state:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mLastNonGuestUser=");
        sb.append(this.mLastNonGuestUser);
        printWriter.println(sb.toString());
        printWriter.print("  mUsers.size=");
        printWriter.println(this.mUsers.size());
        for (int i = 0; i < this.mUsers.size(); i++) {
            UserRecord userRecord = (UserRecord) this.mUsers.get(i);
            printWriter.print("    ");
            printWriter.println(userRecord.toString());
        }
    }

    public String getCurrentUserName(Context context) {
        if (this.mUsers.isEmpty()) {
            return null;
        }
        UserRecord userRecord = (UserRecord) this.mUsers.get(0);
        if (userRecord != null) {
            UserInfo userInfo = userRecord.info;
            if (userInfo != null) {
                if (userRecord.isGuest) {
                    return context.getString(R$string.guest_nickname);
                }
                return userInfo.name;
            }
        }
        return null;
    }

    public void onDensityOrFontScaleChanged() {
        refreshUsers(-1);
    }

    @VisibleForTesting
    public void addAdapter(WeakReference<BaseUserAdapter> weakReference) {
        this.mAdapters.add(weakReference);
    }

    @VisibleForTesting
    public ArrayList<UserRecord> getUsers() {
        return this.mUsers;
    }

    /* access modifiers changed from: private */
    public void checkIfAddUserDisallowedByAdminOnly(UserRecord userRecord) {
        String str = "no_add_user";
        EnforcedAdmin checkIfRestrictionEnforced = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this.mContext, str, ActivityManager.getCurrentUser());
        if (checkIfRestrictionEnforced == null || RestrictedLockUtilsInternal.hasBaseUserRestriction(this.mContext, str, ActivityManager.getCurrentUser())) {
            userRecord.isDisabledByAdmin = false;
            userRecord.enforcedAdmin = null;
            return;
        }
        userRecord.isDisabledByAdmin = true;
        userRecord.enforcedAdmin = checkIfRestrictionEnforced;
    }

    public void startActivity(Intent intent) {
        this.mActivityStarter.startActivity(intent, true);
    }
}
