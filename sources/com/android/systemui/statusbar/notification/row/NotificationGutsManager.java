package com.android.systemui.statusbar.notification.row;

import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.MenuItem;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationLifetimeExtender;
import com.android.systemui.statusbar.NotificationLifetimeExtender.NotificationSafeToRemoveCallback;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.StatusBarStateControllerImpl;
import com.android.systemui.statusbar.notification.NotificationActivityStarter;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.NotificationGuts.OnGutsClosedListener;
import com.android.systemui.statusbar.notification.row.NotificationGuts.OnHeightChangedListener;
import com.android.systemui.statusbar.notification.row.NotificationInfo.CheckSaveListener;
import com.android.systemui.statusbar.notification.row.NotificationInfo.OnAppSettingsClickListener;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

public class NotificationGutsManager implements Dumpable, NotificationLifetimeExtender {
    /* access modifiers changed from: private */
    public final AccessibilityManager mAccessibilityManager;
    private CheckSaveListener mCheckSaveListener;
    private final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    /* access modifiers changed from: private */
    public MenuItem mGutsMenuItem;
    @VisibleForTesting
    protected String mKeyToRemoveOnGutsClosed;
    /* access modifiers changed from: private */
    public NotificationListContainer mListContainer;
    private final NotificationLockscreenUserManager mLockscreenUserManager = ((NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class));
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private NotificationActivityStarter mNotificationActivityStarter;
    private NotificationGuts mNotificationGutsExposed;
    private NotificationSafeToRemoveCallback mNotificationLifetimeFinishedCallback;
    private OnSettingsClickListener mOnSettingsClickListener;
    private NotificationPresenter mPresenter;
    private StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private final VisualStabilityManager mVisualStabilityManager;

    public interface OnSettingsClickListener {
        void onSettingsClick(String str);
    }

    public NotificationGutsManager(Context context, VisualStabilityManager visualStabilityManager) {
        this.mContext = context;
        this.mVisualStabilityManager = visualStabilityManager;
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationListContainer notificationListContainer, CheckSaveListener checkSaveListener, OnSettingsClickListener onSettingsClickListener) {
        this.mPresenter = notificationPresenter;
        this.mListContainer = notificationListContainer;
        this.mCheckSaveListener = checkSaveListener;
        this.mOnSettingsClickListener = onSettingsClickListener;
        this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
    }

    public void setNotificationActivityStarter(NotificationActivityStarter notificationActivityStarter) {
        this.mNotificationActivityStarter = notificationActivityStarter;
    }

    public void onDensityOrFontScaleChanged(NotificationEntry notificationEntry) {
        setExposedGuts(notificationEntry.getGuts());
        bindGuts(notificationEntry.getRow());
    }

    private void startAppNotificationSettingsActivity(String str, int i, NotificationChannel notificationChannel, ExpandableNotificationRow expandableNotificationRow) {
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", str);
        intent.putExtra("app_uid", i);
        if (notificationChannel != null) {
            Bundle bundle = new Bundle();
            String str2 = ":settings:fragment_args_key";
            intent.putExtra(str2, notificationChannel.getId());
            bundle.putString(str2, notificationChannel.getId());
            if (expandableNotificationRow != null) {
                Bundle bundle2 = expandableNotificationRow.getEntry().notification.getNotification().extras;
                if (bundle2.getBoolean("small_app")) {
                    bundle.putString("arg_instant_package_name", bundle2.getString("small_app_package"));
                }
            }
            intent.putExtra(":settings:show_fragment_args", bundle);
        }
        this.mNotificationActivityStarter.startNotificationGutsIntent(intent, i, expandableNotificationRow);
    }

    private void startAppDetailsSettingsActivity(String str, int i, NotificationChannel notificationChannel, ExpandableNotificationRow expandableNotificationRow) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", str, null));
        intent.putExtra("android.provider.extra.APP_PACKAGE", str);
        intent.putExtra("app_uid", i);
        if (notificationChannel != null) {
            intent.putExtra(":settings:fragment_args_key", notificationChannel.getId());
        }
        this.mNotificationActivityStarter.startNotificationGutsIntent(intent, i, expandableNotificationRow);
    }

    /* access modifiers changed from: protected */
    public void startAppOpsSettingsActivity(String str, int i, ArraySet<Integer> arraySet, ExpandableNotificationRow expandableNotificationRow) {
        boolean contains = arraySet.contains(Integer.valueOf(24));
        Integer valueOf = Integer.valueOf(27);
        Integer valueOf2 = Integer.valueOf(26);
        if (contains) {
            if (arraySet.contains(valueOf2) || arraySet.contains(valueOf)) {
                startAppDetailsSettingsActivity(str, i, null, expandableNotificationRow);
                return;
            }
            Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION");
            intent.setData(Uri.fromParts("package", str, null));
            this.mNotificationActivityStarter.startNotificationGutsIntent(intent, i, expandableNotificationRow);
        } else if (arraySet.contains(valueOf2) || arraySet.contains(valueOf)) {
            Intent intent2 = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
            intent2.putExtra("android.intent.extra.PACKAGE_NAME", str);
            this.mNotificationActivityStarter.startNotificationGutsIntent(intent2, i, expandableNotificationRow);
        }
    }

    public boolean bindGuts(ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.inflateGuts();
        return bindGuts(expandableNotificationRow, this.mGutsMenuItem);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean bindGuts(ExpandableNotificationRow expandableNotificationRow, MenuItem menuItem) {
        StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
        expandableNotificationRow.setGutsView(menuItem);
        expandableNotificationRow.setTag(statusBarNotification.getPackageName());
        expandableNotificationRow.getGuts().setClosedListener(new OnGutsClosedListener(expandableNotificationRow, statusBarNotification) {
            private final /* synthetic */ ExpandableNotificationRow f$1;
            private final /* synthetic */ StatusBarNotification f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void onGutsClosed(NotificationGuts notificationGuts) {
                NotificationGutsManager.this.lambda$bindGuts$0$NotificationGutsManager(this.f$1, this.f$2, notificationGuts);
            }
        });
        View gutsView = menuItem.getGutsView();
        try {
            if (gutsView instanceof NotificationSnooze) {
                initializeSnoozeView(expandableNotificationRow, (NotificationSnooze) gutsView);
            } else if (gutsView instanceof AppOpsInfo) {
                initializeAppOpsInfo(expandableNotificationRow, (AppOpsInfo) gutsView);
            } else if (gutsView instanceof NotificationInfo) {
                initializeNotificationInfo(expandableNotificationRow, (NotificationInfo) gutsView);
            }
            return true;
        } catch (Exception e) {
            Log.e("NotificationGutsManager", "error binding guts", e);
            return false;
        }
    }

    public /* synthetic */ void lambda$bindGuts$0$NotificationGutsManager(ExpandableNotificationRow expandableNotificationRow, StatusBarNotification statusBarNotification, NotificationGuts notificationGuts) {
        expandableNotificationRow.onGutsClosed();
        if (!notificationGuts.willBeRemoved() && !expandableNotificationRow.isRemoved()) {
            this.mListContainer.onHeightChanged(expandableNotificationRow, !this.mPresenter.isPresenterFullyCollapsed());
        }
        if (this.mNotificationGutsExposed == notificationGuts) {
            this.mNotificationGutsExposed = null;
            this.mGutsMenuItem = null;
        }
        String key = statusBarNotification.getKey();
        if (key.equals(this.mKeyToRemoveOnGutsClosed)) {
            this.mKeyToRemoveOnGutsClosed = null;
            NotificationSafeToRemoveCallback notificationSafeToRemoveCallback = this.mNotificationLifetimeFinishedCallback;
            if (notificationSafeToRemoveCallback != null) {
                notificationSafeToRemoveCallback.onSafeToRemove(key);
            }
        }
    }

    private void initializeSnoozeView(ExpandableNotificationRow expandableNotificationRow, NotificationSnooze notificationSnooze) {
        NotificationGuts guts = expandableNotificationRow.getGuts();
        StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
        notificationSnooze.setSnoozeListener(this.mListContainer.getSwipeActionHelper());
        notificationSnooze.setStatusBarNotification(statusBarNotification);
        notificationSnooze.setSnoozeOptions(expandableNotificationRow.getEntry().snoozeCriteria);
        guts.setHeightChangedListener(new OnHeightChangedListener(expandableNotificationRow) {
            private final /* synthetic */ ExpandableNotificationRow f$1;

            {
                this.f$1 = r2;
            }

            public final void onHeightChanged(NotificationGuts notificationGuts) {
                NotificationGutsManager.this.lambda$initializeSnoozeView$1$NotificationGutsManager(this.f$1, notificationGuts);
            }
        });
    }

    public /* synthetic */ void lambda$initializeSnoozeView$1$NotificationGutsManager(ExpandableNotificationRow expandableNotificationRow, NotificationGuts notificationGuts) {
        this.mListContainer.onHeightChanged(expandableNotificationRow, expandableNotificationRow.isShown());
    }

    private void initializeAppOpsInfo(ExpandableNotificationRow expandableNotificationRow, AppOpsInfo appOpsInfo) {
        NotificationGuts guts = expandableNotificationRow.getGuts();
        StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
        PackageManager packageManagerForUser = StatusBar.getPackageManagerForUser(this.mContext, statusBarNotification.getUser().getIdentifier());
        $$Lambda$NotificationGutsManager$QUX76CVRNteGCzCinyuNeuYX3tU r3 = new com.android.systemui.statusbar.notification.row.AppOpsInfo.OnSettingsClickListener(guts, expandableNotificationRow) {
            private final /* synthetic */ NotificationGuts f$1;
            private final /* synthetic */ ExpandableNotificationRow f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void onClick(View view, String str, int i, ArraySet arraySet) {
                NotificationGutsManager.this.lambda$initializeAppOpsInfo$2$NotificationGutsManager(this.f$1, this.f$2, view, str, i, arraySet);
            }
        };
        if (!expandableNotificationRow.getEntry().mActiveAppOps.isEmpty()) {
            appOpsInfo.bindGuts(packageManagerForUser, r3, statusBarNotification, expandableNotificationRow.getEntry().mActiveAppOps);
        }
    }

    public /* synthetic */ void lambda$initializeAppOpsInfo$2$NotificationGutsManager(NotificationGuts notificationGuts, ExpandableNotificationRow expandableNotificationRow, View view, String str, int i, ArraySet arraySet) {
        this.mMetricsLogger.action(1346);
        notificationGuts.resetFalsingCheck();
        startAppOpsSettingsActivity(str, i, arraySet, expandableNotificationRow);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void initializeNotificationInfo(ExpandableNotificationRow expandableNotificationRow, NotificationInfo notificationInfo) throws Exception {
        $$Lambda$NotificationGutsManager$Q50_8sHdIRaYdx4NmoW9bex_4o r12;
        NotificationGuts guts = expandableNotificationRow.getGuts();
        StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
        String packageName = statusBarNotification.getPackageName();
        UserHandle user = statusBarNotification.getUser();
        PackageManager packageManagerForUser = StatusBar.getPackageManagerForUser(this.mContext, user.getIdentifier());
        INotificationManager asInterface = Stub.asInterface(ServiceManager.getService("notification"));
        $$Lambda$NotificationGutsManager$5sbilrrQIt_lf8k9ZdwNLnjs r13 = new OnAppSettingsClickListener(guts, statusBarNotification, expandableNotificationRow) {
            private final /* synthetic */ NotificationGuts f$1;
            private final /* synthetic */ StatusBarNotification f$2;
            private final /* synthetic */ ExpandableNotificationRow f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void onClick(View view, Intent intent) {
                NotificationGutsManager.this.lambda$initializeNotificationInfo$3$NotificationGutsManager(this.f$1, this.f$2, this.f$3, view, intent);
            }
        };
        boolean isBlockingHelperShowing = expandableNotificationRow.isBlockingHelperShowing();
        if (!user.equals(UserHandle.ALL) || this.mLockscreenUserManager.getCurrentUserId() == 0) {
            r12 = new com.android.systemui.statusbar.notification.row.NotificationInfo.OnSettingsClickListener(guts, statusBarNotification, packageName, expandableNotificationRow) {
                private final /* synthetic */ NotificationGuts f$1;
                private final /* synthetic */ StatusBarNotification f$2;
                private final /* synthetic */ String f$3;
                private final /* synthetic */ ExpandableNotificationRow f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void onClick(View view, NotificationChannel notificationChannel, int i) {
                    NotificationGutsManager.this.lambda$initializeNotificationInfo$4$NotificationGutsManager(this.f$1, this.f$2, this.f$3, this.f$4, view, notificationChannel, i);
                }
            };
        } else {
            r12 = null;
        }
        VisualStabilityManager visualStabilityManager = this.mVisualStabilityManager;
        NotificationChannel notificationChannel = expandableNotificationRow.getEntry().channel;
        ArraySet uniqueChannels = expandableNotificationRow.getUniqueChannels();
        CheckSaveListener checkSaveListener = this.mCheckSaveListener;
        boolean isDeviceProvisioned = this.mDeviceProvisionedController.isDeviceProvisioned();
        boolean isNonblockable = expandableNotificationRow.getIsNonblockable();
        int i = expandableNotificationRow.getEntry().importance;
        int i2 = i;
        notificationInfo.bindNotification(packageManagerForUser, asInterface, visualStabilityManager, packageName, notificationChannel, uniqueChannels, statusBarNotification, checkSaveListener, r12, r13, isDeviceProvisioned, isNonblockable, isBlockingHelperShowing, i2, expandableNotificationRow.getEntry().isHighPriority());
    }

    public /* synthetic */ void lambda$initializeNotificationInfo$3$NotificationGutsManager(NotificationGuts notificationGuts, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow, View view, Intent intent) {
        this.mMetricsLogger.action(206);
        notificationGuts.resetFalsingCheck();
        this.mNotificationActivityStarter.startNotificationGutsIntent(intent, statusBarNotification.getUid(), expandableNotificationRow);
    }

    public /* synthetic */ void lambda$initializeNotificationInfo$4$NotificationGutsManager(NotificationGuts notificationGuts, StatusBarNotification statusBarNotification, String str, ExpandableNotificationRow expandableNotificationRow, View view, NotificationChannel notificationChannel, int i) {
        this.mMetricsLogger.action(205);
        notificationGuts.resetFalsingCheck();
        this.mOnSettingsClickListener.onSettingsClick(statusBarNotification.getKey());
        startAppNotificationSettingsActivity(str, i, notificationChannel, expandableNotificationRow);
    }

    public void closeAndSaveGuts(boolean z, boolean z2, boolean z3, int i, int i2, boolean z4) {
        NotificationGuts notificationGuts = this.mNotificationGutsExposed;
        if (notificationGuts != null) {
            notificationGuts.closeControls(z, z3, i, i2, z2);
        }
        if (z4) {
            this.mListContainer.resetExposedMenuView(false, true);
        }
    }

    public NotificationGuts getExposedGuts() {
        return this.mNotificationGutsExposed;
    }

    public void setExposedGuts(NotificationGuts notificationGuts) {
        this.mNotificationGutsExposed = notificationGuts;
    }

    public boolean openGuts(View view, int i, int i2, MenuItem menuItem) {
        if (!(menuItem.getGutsView() instanceof NotificationInfo)) {
            return lambda$openGuts$5$NotificationGutsManager(view, i, i2, menuItem);
        }
        StatusBarStateController statusBarStateController = this.mStatusBarStateController;
        if (statusBarStateController instanceof StatusBarStateControllerImpl) {
            ((StatusBarStateControllerImpl) statusBarStateController).setLeaveOpenOnKeyguardHide(true);
        }
        $$Lambda$NotificationGutsManager$ujQD4EmV_laISDNVyhlRbAQC7J4 r3 = new Runnable(view, i, i2, menuItem) {
            private final /* synthetic */ View f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ MenuItem f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                NotificationGutsManager.this.lambda$openGuts$6$NotificationGutsManager(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        };
        this.mStatusBar.executeRunnableDismissingKeyguard(r3, null, false, true, true);
        return true;
    }

    public /* synthetic */ void lambda$openGuts$6$NotificationGutsManager(View view, int i, int i2, MenuItem menuItem) {
        Handler handler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        $$Lambda$NotificationGutsManager$5tF5K6xQWa5hRdlcAaoNBN2vKM r1 = new Runnable(view, i, i2, menuItem) {
            private final /* synthetic */ View f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ MenuItem f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                NotificationGutsManager.this.lambda$openGuts$5$NotificationGutsManager(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        };
        handler.post(r1);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    /* renamed from: openGutsInternal */
    public boolean lambda$openGuts$5$NotificationGutsManager(View view, int i, int i2, MenuItem menuItem) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return false;
        }
        if (view.getWindowToken() == null) {
            Log.e("NotificationGutsManager", "Trying to show notification guts, but not attached to window");
            return false;
        }
        final ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        if (expandableNotificationRow.isDark()) {
            return false;
        }
        view.performHapticFeedback(0);
        if (expandableNotificationRow.areGutsExposed()) {
            closeAndSaveGuts(false, false, true, -1, -1, true);
            return false;
        }
        expandableNotificationRow.inflateGuts();
        NotificationGuts guts = expandableNotificationRow.getGuts();
        this.mNotificationGutsExposed = guts;
        if (!bindGuts(expandableNotificationRow, menuItem) || guts == null) {
            return false;
        }
        guts.setVisibility(4);
        final NotificationGuts notificationGuts = guts;
        final int i3 = i;
        final int i4 = i2;
        final MenuItem menuItem2 = menuItem;
        C12671 r0 = new Runnable() {
            public void run() {
                if (expandableNotificationRow.getWindowToken() == null) {
                    Log.e("NotificationGutsManager", "Trying to show notification guts in post(), but not attached to window");
                    return;
                }
                notificationGuts.setVisibility(0);
                boolean z = NotificationGutsManager.this.mStatusBarStateController.getState() == 1 && !NotificationGutsManager.this.mAccessibilityManager.isTouchExplorationEnabled();
                NotificationGuts notificationGuts = notificationGuts;
                boolean z2 = !expandableNotificationRow.isBlockingHelperShowing();
                int i = i3;
                int i2 = i4;
                ExpandableNotificationRow expandableNotificationRow = expandableNotificationRow;
                Objects.requireNonNull(expandableNotificationRow);
                notificationGuts.openControls(z2, i, i2, z, new Runnable() {
                    public final void run() {
                        ExpandableNotificationRow.this.onGutsOpened();
                    }
                });
                expandableNotificationRow.closeRemoteInput();
                NotificationGutsManager.this.mListContainer.onHeightChanged(expandableNotificationRow, true);
                NotificationGutsManager.this.mGutsMenuItem = menuItem2;
            }
        };
        guts.post(r0);
        return true;
    }

    public void setCallback(NotificationSafeToRemoveCallback notificationSafeToRemoveCallback) {
        this.mNotificationLifetimeFinishedCallback = notificationSafeToRemoveCallback;
    }

    public boolean shouldExtendLifetime(NotificationEntry notificationEntry) {
        return (notificationEntry == null || this.mNotificationGutsExposed == null || notificationEntry.getGuts() == null || this.mNotificationGutsExposed != notificationEntry.getGuts() || this.mNotificationGutsExposed.isLeavebehind()) ? false : true;
    }

    public void setShouldManageLifetime(NotificationEntry notificationEntry, boolean z) {
        String str = "NotificationGutsManager";
        if (z) {
            this.mKeyToRemoveOnGutsClosed = notificationEntry.key;
            if (Log.isLoggable(str, 3)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Keeping notification because it's showing guts. ");
                sb.append(notificationEntry.key);
                Log.d(str, sb.toString());
                return;
            }
            return;
        }
        String str2 = this.mKeyToRemoveOnGutsClosed;
        if (str2 != null && str2.equals(notificationEntry.key)) {
            this.mKeyToRemoveOnGutsClosed = null;
            if (Log.isLoggable(str, 3)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Notification that was kept for guts was updated. ");
                sb2.append(notificationEntry.key);
                Log.d(str, sb2.toString());
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationGutsManager state:");
        printWriter.print("  mKeyToRemoveOnGutsClosed: ");
        printWriter.println(this.mKeyToRemoveOnGutsClosed);
    }
}
