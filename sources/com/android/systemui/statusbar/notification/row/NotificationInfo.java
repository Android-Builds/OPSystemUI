package com.android.systemui.statusbar.notification.row;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent;
import java.util.List;
import java.util.Set;

public class NotificationInfo extends LinearLayout implements GutsContent {
    /* access modifiers changed from: private */
    public static final Uri INSTANT_APP_BASE_URI = Uri.parse("content://com.nearme.instant.setting/notification");
    private String mAppName;
    private OnAppSettingsClickListener mAppSettingsClickListener;
    private int mAppUid;
    private ChannelEditorDialogController mChannelEditorDialogController;
    private CheckSaveListener mCheckSaveListener;
    private Integer mChosenImportance;
    private int mCurrentAlertingBehavior = -1;
    private String mDelegatePkg;
    private String mExitReason = "blocking_helper_dismissed";
    private AnimatorSet mExpandAnimation;
    private NotificationGuts mGutsContainer;
    private INotificationManager mINotificationManager;
    private int mImportanceButtonStroke;
    private String mInstantAppPkg;
    private boolean mIsDeviceProvisioned;
    private boolean mIsForBlockingHelper;
    private boolean mIsInstantApp = false;
    private boolean mIsNonblockable;
    private boolean mIsSingleDefaultChannel;
    private MetricsLogger mMetricsLogger;
    private int mNumUniqueChannelsInRow;
    private OnClickListener mOnAlert = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$0$NotificationInfo(view);
        }
    };
    private OnClickListener mOnDeliverSilently = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$4$NotificationInfo(view);
        }
    };
    private OnClickListener mOnDismissSettings = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$2$NotificationInfo(view);
        }
    };
    private OnClickListener mOnKeepShowing = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$3$NotificationInfo(view);
        }
    };
    private OnSettingsClickListener mOnSettingsClickListener;
    private OnClickListener mOnSilent = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$1$NotificationInfo(view);
        }
    };
    private OnClickListener mOnStopOrMinimizeNotifications = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$5$NotificationInfo(view);
        }
    };
    private OnClickListener mOnUndo = new OnClickListener() {
        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$7$NotificationInfo(view);
        }
    };
    private int mOneplusAccentColor;
    private String mPackageName;
    private Drawable mPkgIcon;
    private PackageManager mPm;
    private boolean mPresentingChannelEditorDialog = false;
    private boolean mPressedApply;
    private TextView mPriorityDescriptionView;
    private StatusBarNotification mSbn;
    private int mSecondaryColor;
    private TextView mSilentDescriptionView;
    private NotificationChannel mSingleNotificationChannel;
    private int mStartingChannelImportance;
    private Set<NotificationChannel> mUniqueChannelsInRow;
    private VisualStabilityManager mVisualStabilityManager;
    private boolean mWasShownHighPriority;

    public interface CheckSaveListener {
        void checkSave(Runnable runnable, StatusBarNotification statusBarNotification);
    }

    public interface OnAppSettingsClickListener {
        void onClick(View view, Intent intent);
    }

    public interface OnSettingsClickListener {
        void onClick(View view, NotificationChannel notificationChannel, int i);
    }

    private static class UpdateImportanceRunnable implements Runnable {
        private final int mAppUid;
        private boolean mCancelInstantApp = false;
        private final NotificationChannel mChannelToUpdate;
        private final Context mContext;
        private final int mCurrentImportance;
        private final INotificationManager mINotificationManager;
        private final boolean mIsInstantApp;
        private final int mNewImportance;
        private final String mPackageName;
        private final StatusBarNotification mSbn;

        public UpdateImportanceRunnable(INotificationManager iNotificationManager, String str, int i, NotificationChannel notificationChannel, int i2, int i3, Context context, StatusBarNotification statusBarNotification, boolean z) {
            this.mContext = context;
            this.mSbn = statusBarNotification;
            this.mIsInstantApp = z;
            this.mINotificationManager = iNotificationManager;
            this.mPackageName = str;
            this.mAppUid = i;
            this.mChannelToUpdate = notificationChannel;
            this.mCurrentImportance = i2;
            this.mNewImportance = i3;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:33:0x0097, code lost:
            if (r1 == null) goto L_0x00a6;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
                r11 = this;
                java.lang.String r0 = "InfoGuts"
                android.app.NotificationChannel r1 = r11.mChannelToUpdate     // Catch:{ RemoteException -> 0x00bf }
                r2 = 1
                r3 = 0
                if (r1 == 0) goto L_0x003c
                android.app.NotificationChannel r1 = r11.mChannelToUpdate     // Catch:{ RemoteException -> 0x00bf }
                int r4 = r11.mNewImportance     // Catch:{ RemoteException -> 0x00bf }
                r1.setImportance(r4)     // Catch:{ RemoteException -> 0x00bf }
                android.app.NotificationChannel r1 = r11.mChannelToUpdate     // Catch:{ RemoteException -> 0x00bf }
                r4 = 4
                r1.lockFields(r4)     // Catch:{ RemoteException -> 0x00bf }
                android.app.INotificationManager r1 = r11.mINotificationManager     // Catch:{ IllegalArgumentException -> 0x0021 }
                java.lang.String r4 = r11.mPackageName     // Catch:{ IllegalArgumentException -> 0x0021 }
                int r5 = r11.mAppUid     // Catch:{ IllegalArgumentException -> 0x0021 }
                android.app.NotificationChannel r6 = r11.mChannelToUpdate     // Catch:{ IllegalArgumentException -> 0x0021 }
                r1.updateNotificationChannelForPackage(r4, r5, r6)     // Catch:{ IllegalArgumentException -> 0x0021 }
                goto L_0x004e
            L_0x0021:
                r11 = move-exception
                java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x00bf }
                r1.<init>()     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r2 = "updateNotificationChannelForPackage: "
                r1.append(r2)     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r11 = r11.getMessage()     // Catch:{ RemoteException -> 0x00bf }
                r1.append(r11)     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r11 = r1.toString()     // Catch:{ RemoteException -> 0x00bf }
                android.util.Log.e(r0, r11)     // Catch:{ RemoteException -> 0x00bf }
                return
            L_0x003c:
                android.app.INotificationManager r1 = r11.mINotificationManager     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r4 = r11.mPackageName     // Catch:{ RemoteException -> 0x00bf }
                int r5 = r11.mAppUid     // Catch:{ RemoteException -> 0x00bf }
                int r6 = r11.mNewImportance     // Catch:{ RemoteException -> 0x00bf }
                int r7 = r11.mCurrentImportance     // Catch:{ RemoteException -> 0x00bf }
                if (r6 < r7) goto L_0x004a
                r6 = r2
                goto L_0x004b
            L_0x004a:
                r6 = r3
            L_0x004b:
                r1.setNotificationsEnabledWithImportanceLockForPackage(r4, r5, r6)     // Catch:{ RemoteException -> 0x00bf }
            L_0x004e:
                boolean r1 = r11.mIsInstantApp     // Catch:{ RemoteException -> 0x00bf }
                if (r1 == 0) goto L_0x00c5
                android.service.notification.StatusBarNotification r1 = r11.mSbn     // Catch:{ RemoteException -> 0x00bf }
                android.app.Notification r1 = r1.getNotification()     // Catch:{ RemoteException -> 0x00bf }
                android.os.Bundle r1 = r1.extras     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r4 = "small_app_package"
                java.lang.String r1 = r1.getString(r4)     // Catch:{ RemoteException -> 0x00bf }
                android.content.Context r4 = r11.mContext     // Catch:{ RemoteException -> 0x00bf }
                android.content.ContentResolver r5 = r4.getContentResolver()     // Catch:{ RemoteException -> 0x00bf }
                android.net.Uri r4 = com.android.systemui.statusbar.notification.row.NotificationInfo.INSTANT_APP_BASE_URI     // Catch:{ RemoteException -> 0x00bf }
                android.net.Uri r6 = android.net.Uri.withAppendedPath(r4, r1)     // Catch:{ RemoteException -> 0x00bf }
                r7 = 0
                r8 = 0
                r9 = 0
                r10 = 0
                android.database.Cursor r1 = r5.query(r6, r7, r8, r9, r10)     // Catch:{ RemoteException -> 0x00bf }
                if (r1 == 0) goto L_0x00a3
                boolean r4 = r1.moveToFirst()     // Catch:{ Exception -> 0x0091 }
                if (r4 == 0) goto L_0x00a3
                java.lang.String r4 = "notify"
                int r4 = r1.getColumnIndex(r4)     // Catch:{ Exception -> 0x0091 }
                int r4 = r1.getInt(r4)     // Catch:{ Exception -> 0x0091 }
                if (r4 != 0) goto L_0x008b
                goto L_0x008c
            L_0x008b:
                r2 = r3
            L_0x008c:
                r11.mCancelInstantApp = r2     // Catch:{ Exception -> 0x0091 }
                goto L_0x00a3
            L_0x008f:
                r11 = move-exception
                goto L_0x009d
            L_0x0091:
                r2 = move-exception
                java.lang.String r3 = "Fail to query data from Instant App base URI"
                android.util.Log.d(r0, r3, r2)     // Catch:{ all -> 0x008f }
                if (r1 == 0) goto L_0x00a6
            L_0x0099:
                r1.close()     // Catch:{ RemoteException -> 0x00bf }
                goto L_0x00a6
            L_0x009d:
                if (r1 == 0) goto L_0x00a2
                r1.close()     // Catch:{ RemoteException -> 0x00bf }
            L_0x00a2:
                throw r11     // Catch:{ RemoteException -> 0x00bf }
            L_0x00a3:
                if (r1 == 0) goto L_0x00a6
                goto L_0x0099
            L_0x00a6:
                boolean r1 = r11.mCancelInstantApp     // Catch:{ RemoteException -> 0x00bf }
                if (r1 == 0) goto L_0x00c5
                android.service.notification.StatusBarNotification r1 = r11.mSbn     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r1 = r1.getTag()     // Catch:{ RemoteException -> 0x00bf }
                android.service.notification.StatusBarNotification r2 = r11.mSbn     // Catch:{ RemoteException -> 0x00bf }
                int r2 = r2.getId()     // Catch:{ RemoteException -> 0x00bf }
                android.app.INotificationManager r3 = r11.mINotificationManager     // Catch:{ RemoteException -> 0x00bf }
                java.lang.String r11 = r11.mPackageName     // Catch:{ RemoteException -> 0x00bf }
                r4 = -1
                r3.cancelNotificationWithTag(r11, r1, r2, r4)     // Catch:{ RemoteException -> 0x00bf }
                goto L_0x00c5
            L_0x00bf:
                r11 = move-exception
                java.lang.String r1 = "Unable to update notification importance"
                android.util.Log.e(r0, r1, r11)
            L_0x00c5:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.row.NotificationInfo.UpdateImportanceRunnable.run():void");
        }
    }

    public View getContentView() {
        return this;
    }

    public boolean willBeRemoved() {
        return false;
    }

    public /* synthetic */ void lambda$new$0$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_keep_showing";
        this.mChosenImportance = Integer.valueOf(3);
        applyAlertingBehavior(0, true);
    }

    public /* synthetic */ void lambda$new$1$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_deliver_silently";
        this.mChosenImportance = Integer.valueOf(2);
        applyAlertingBehavior(1, true);
    }

    public /* synthetic */ void lambda$new$2$NotificationInfo(View view) {
        this.mPressedApply = true;
        closeControls(view, true);
    }

    public /* synthetic */ void lambda$new$3$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_keep_showing";
        closeControls(view, true);
        this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(4).setSubtype(5));
    }

    public /* synthetic */ void lambda$new$4$NotificationInfo(View view) {
        handleSaveImportance(4, 5);
    }

    public /* synthetic */ void lambda$new$5$NotificationInfo(View view) {
        swapContent(3, true);
    }

    private void handleSaveImportance(int i, int i2) {
        $$Lambda$NotificationInfo$ylZvn7FrHqRtVpgR47lZQtVgaFY r0 = new Runnable(i, i2) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NotificationInfo.this.lambda$handleSaveImportance$6$NotificationInfo(this.f$1, this.f$2);
            }
        };
        CheckSaveListener checkSaveListener = this.mCheckSaveListener;
        if (checkSaveListener != null) {
            checkSaveListener.checkSave(r0, this.mSbn);
        } else {
            r0.run();
        }
    }

    public /* synthetic */ void lambda$handleSaveImportance$6$NotificationInfo(int i, int i2) {
        saveImportanceAndExitReason(i);
        if (this.mIsForBlockingHelper) {
            swapContent(i, true);
            this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(4).setSubtype(i2));
        }
    }

    public /* synthetic */ void lambda$new$7$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_dismissed";
        if (this.mIsForBlockingHelper) {
            logBlockingHelperCounter("blocking_helper_undo");
            this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(5).setSubtype(7));
        } else {
            this.mMetricsLogger.write(importanceChangeLogMaker().setType(5));
        }
        saveImportanceAndExitReason(1);
        swapContent(1, true);
    }

    public NotificationInfo(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPriorityDescriptionView = (TextView) findViewById(R$id.alert_summary);
        this.mSilentDescriptionView = (TextView) findViewById(R$id.silence_summary);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void bindNotification(PackageManager packageManager, INotificationManager iNotificationManager, VisualStabilityManager visualStabilityManager, String str, NotificationChannel notificationChannel, Set<NotificationChannel> set, StatusBarNotification statusBarNotification, CheckSaveListener checkSaveListener, OnSettingsClickListener onSettingsClickListener, OnAppSettingsClickListener onAppSettingsClickListener, boolean z, boolean z2, int i, boolean z3) throws RemoteException {
        bindNotification(packageManager, iNotificationManager, visualStabilityManager, str, notificationChannel, set, statusBarNotification, checkSaveListener, onSettingsClickListener, onAppSettingsClickListener, z, z2, false, i, z3);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x00fe, code lost:
        if (r1 == null) goto L_0x010c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0107, code lost:
        if (r1 != null) goto L_0x0109;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0109, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x010c, code lost:
        ((android.widget.ImageView) findViewById(com.android.systemui.R$id.pkgicon)).setImageDrawable(r0.mPkgIcon);
        ((android.widget.TextView) findViewById(com.android.systemui.R$id.pkgname)).setText(r0.mAppName);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void bindNotification(android.content.pm.PackageManager r1, android.app.INotificationManager r2, com.android.systemui.statusbar.notification.VisualStabilityManager r3, java.lang.String r4, android.app.NotificationChannel r5, java.util.Set<android.app.NotificationChannel> r6, android.service.notification.StatusBarNotification r7, com.android.systemui.statusbar.notification.row.NotificationInfo.CheckSaveListener r8, com.android.systemui.statusbar.notification.row.NotificationInfo.OnSettingsClickListener r9, com.android.systemui.statusbar.notification.row.NotificationInfo.OnAppSettingsClickListener r10, boolean r11, boolean r12, boolean r13, int r14, boolean r15) throws android.os.RemoteException {
        /*
            r0 = this;
            r0.mINotificationManager = r2
            java.lang.Class<com.android.internal.logging.MetricsLogger> r2 = com.android.internal.logging.MetricsLogger.class
            java.lang.Object r2 = com.android.systemui.Dependency.get(r2)
            com.android.internal.logging.MetricsLogger r2 = (com.android.internal.logging.MetricsLogger) r2
            r0.mMetricsLogger = r2
            r0.mVisualStabilityManager = r3
            java.lang.Class<com.android.systemui.statusbar.notification.row.ChannelEditorDialogController> r2 = com.android.systemui.statusbar.notification.row.ChannelEditorDialogController.class
            java.lang.Object r2 = com.android.systemui.Dependency.get(r2)
            com.android.systemui.statusbar.notification.row.ChannelEditorDialogController r2 = (com.android.systemui.statusbar.notification.row.ChannelEditorDialogController) r2
            r0.mChannelEditorDialogController = r2
            r0.mPackageName = r4
            r0.mUniqueChannelsInRow = r6
            int r2 = r6.size()
            r0.mNumUniqueChannelsInRow = r2
            r0.mSbn = r7
            r0.mPm = r1
            r0.mAppSettingsClickListener = r10
            java.lang.String r1 = r0.mPackageName
            r0.mAppName = r1
            r0.mCheckSaveListener = r8
            r0.mOnSettingsClickListener = r9
            r0.mSingleNotificationChannel = r5
            android.app.NotificationChannel r1 = r0.mSingleNotificationChannel
            int r1 = r1.getImportance()
            r0.mStartingChannelImportance = r1
            r0.mWasShownHighPriority = r15
            r0.mIsNonblockable = r12
            r0.mIsForBlockingHelper = r13
            android.service.notification.StatusBarNotification r1 = r0.mSbn
            int r1 = r1.getUid()
            r0.mAppUid = r1
            android.service.notification.StatusBarNotification r1 = r0.mSbn
            java.lang.String r1 = r1.getOpPkg()
            r0.mDelegatePkg = r1
            r0.mIsDeviceProvisioned = r11
            android.service.notification.StatusBarNotification r1 = r0.mSbn
            android.app.Notification r1 = r1.getNotification()
            android.os.Bundle r1 = r1.extras
            java.lang.String r2 = "small_app"
            boolean r1 = r1.getBoolean(r2)
            r0.mIsInstantApp = r1
            android.app.INotificationManager r1 = r0.mINotificationManager
            int r2 = r0.mAppUid
            r3 = 0
            int r1 = r1.getNumNotificationChannelsForPackage(r4, r2, r3)
            int r2 = r0.mNumUniqueChannelsInRow
            if (r2 == 0) goto L_0x021f
            r4 = 1
            if (r2 != r4) goto L_0x0083
            android.app.NotificationChannel r2 = r0.mSingleNotificationChannel
            java.lang.String r2 = r2.getId()
            java.lang.String r5 = "miscellaneous"
            boolean r2 = r2.equals(r5)
            if (r2 == 0) goto L_0x0083
            if (r1 != r4) goto L_0x0083
            goto L_0x0084
        L_0x0083:
            r4 = r3
        L_0x0084:
            r0.mIsSingleDefaultChannel = r4
            r0.bindHeader()
            r0.bindChannelDetails()
            boolean r1 = r0.mIsForBlockingHelper
            if (r1 == 0) goto L_0x0094
            r0.bindBlockingHelper()
            goto L_0x0097
        L_0x0094:
            r0.bindInlineControls()
        L_0x0097:
            boolean r1 = r0.mIsInstantApp
            if (r1 == 0) goto L_0x0126
            android.service.notification.StatusBarNotification r1 = r0.mSbn
            android.app.Notification r1 = r1.getNotification()
            android.os.Bundle r1 = r1.extras
            java.lang.String r2 = "small_app_package"
            java.lang.String r1 = r1.getString(r2)
            r0.mInstantAppPkg = r1
            android.service.notification.StatusBarNotification r1 = r0.mSbn
            android.app.Notification r1 = r1.getNotification()
            android.os.Bundle r1 = r1.extras
            java.lang.String r2 = "small_app_name"
            java.lang.String r1 = r1.getString(r2)
            r0.mAppName = r1
            android.content.Context r1 = r0.mContext
            android.content.ContentResolver r4 = r1.getContentResolver()
            android.net.Uri r1 = INSTANT_APP_BASE_URI
            java.lang.String r2 = r0.mInstantAppPkg
            android.net.Uri r5 = android.net.Uri.withAppendedPath(r1, r2)
            r6 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            android.database.Cursor r1 = r4.query(r5, r6, r7, r8, r9)
            if (r1 == 0) goto L_0x0107
            boolean r2 = r1.moveToFirst()     // Catch:{ Exception -> 0x00f6 }
            if (r2 == 0) goto L_0x0107
            java.lang.String r2 = "icon"
            int r2 = r1.getColumnIndex(r2)     // Catch:{ Exception -> 0x00f6 }
            byte[] r2 = r1.getBlob(r2)     // Catch:{ Exception -> 0x00f6 }
            int r4 = r2.length     // Catch:{ Exception -> 0x00f6 }
            android.graphics.Bitmap r2 = android.graphics.BitmapFactory.decodeByteArray(r2, r3, r4)     // Catch:{ Exception -> 0x00f6 }
            android.graphics.drawable.BitmapDrawable r4 = new android.graphics.drawable.BitmapDrawable     // Catch:{ Exception -> 0x00f6 }
            android.content.res.Resources r5 = r0.getResources()     // Catch:{ Exception -> 0x00f6 }
            r4.<init>(r5, r2)     // Catch:{ Exception -> 0x00f6 }
            r0.mPkgIcon = r4     // Catch:{ Exception -> 0x00f6 }
            goto L_0x0107
        L_0x00f4:
            r0 = move-exception
            goto L_0x0101
        L_0x00f6:
            r2 = move-exception
            java.lang.String r4 = "InfoGuts"
            java.lang.String r5 = "Fail to query data from Instant App base URI"
            android.util.Log.d(r4, r5, r2)     // Catch:{ all -> 0x00f4 }
            if (r1 == 0) goto L_0x010c
            goto L_0x0109
        L_0x0101:
            if (r1 == 0) goto L_0x0106
            r1.close()
        L_0x0106:
            throw r0
        L_0x0107:
            if (r1 == 0) goto L_0x010c
        L_0x0109:
            r1.close()
        L_0x010c:
            int r1 = com.android.systemui.R$id.pkgicon
            android.view.View r1 = r0.findViewById(r1)
            android.widget.ImageView r1 = (android.widget.ImageView) r1
            android.graphics.drawable.Drawable r2 = r0.mPkgIcon
            r1.setImageDrawable(r2)
            int r1 = com.android.systemui.R$id.pkgname
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            java.lang.String r2 = r0.mAppName
            r1.setText(r2)
        L_0x0126:
            com.android.internal.logging.MetricsLogger r1 = r0.mMetricsLogger
            android.metrics.LogMaker r2 = r0.notificationControlsLogMaker()
            r1.write(r2)
            android.content.res.Resources r1 = r0.getResources()
            int r2 = com.android.systemui.R$dimen.op_notification_info_importance_button_stroke
            float r1 = r1.getDimension(r2)
            android.content.res.Resources r2 = r0.getResources()
            android.util.DisplayMetrics r2 = r2.getDisplayMetrics()
            float r2 = r2.density
            float r1 = r1 * r2
            int r1 = (int) r1
            r0.mImportanceButtonStroke = r1
            int r1 = android.content.res.OpThemeUtils.getOneplusAccentColor(r3)
            r0.mOneplusAccentColor = r1
            android.content.res.Resources r1 = r0.getResources()
            int r2 = com.android.systemui.R$color.op_notification_info_secondary_color
            int r1 = r1.getColor(r2)
            r0.mSecondaryColor = r1
            int r1 = r0.mOneplusAccentColor
            if (r1 == 0) goto L_0x021e
            int r1 = com.android.systemui.R$id.blocking_helper_turn_off_notifications
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            int r2 = r0.mOneplusAccentColor
            r1.setTextColor(r2)
            int r1 = com.android.systemui.R$id.deliver_silently
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            int r2 = r0.mOneplusAccentColor
            r1.setTextColor(r2)
            int r1 = com.android.systemui.R$id.keep_showing
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            int r2 = r0.mOneplusAccentColor
            r1.setTextColor(r2)
            int r1 = com.android.systemui.R$id.turn_off_notifications
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            int r2 = r0.mOneplusAccentColor
            r1.setTextColor(r2)
            int r1 = com.android.systemui.R$id.done
            android.view.View r1 = r0.findViewById(r1)
            android.widget.TextView r1 = (android.widget.TextView) r1
            int r2 = r0.mOneplusAccentColor
            r1.setTextColor(r2)
            int r1 = com.android.systemui.R$id.alert
            android.view.View r1 = r0.findViewById(r1)
            android.graphics.drawable.Drawable r2 = r1.getBackground()
            android.graphics.drawable.GradientDrawable r2 = (android.graphics.drawable.GradientDrawable) r2
            boolean r1 = r1.isSelected()
            int r3 = r0.mImportanceButtonStroke
            if (r1 == 0) goto L_0x01b5
            int r4 = r0.mOneplusAccentColor
            goto L_0x01b7
        L_0x01b5:
            int r4 = r0.mSecondaryColor
        L_0x01b7:
            r2.setStroke(r3, r4)
            int r2 = com.android.systemui.R$id.alert_icon
            android.view.View r2 = r0.findViewById(r2)
            android.widget.ImageView r2 = (android.widget.ImageView) r2
            if (r1 == 0) goto L_0x01c7
            int r3 = r0.mOneplusAccentColor
            goto L_0x01c9
        L_0x01c7:
            int r3 = r0.mSecondaryColor
        L_0x01c9:
            r2.setColorFilter(r3)
            int r2 = com.android.systemui.R$id.alert_label
            android.view.View r2 = r0.findViewById(r2)
            android.widget.TextView r2 = (android.widget.TextView) r2
            if (r1 == 0) goto L_0x01d9
            int r1 = r0.mOneplusAccentColor
            goto L_0x01db
        L_0x01d9:
            int r1 = r0.mSecondaryColor
        L_0x01db:
            r2.setTextColor(r1)
            int r1 = com.android.systemui.R$id.silence
            android.view.View r1 = r0.findViewById(r1)
            android.graphics.drawable.Drawable r2 = r1.getBackground()
            android.graphics.drawable.GradientDrawable r2 = (android.graphics.drawable.GradientDrawable) r2
            boolean r1 = r1.isSelected()
            int r3 = r0.mImportanceButtonStroke
            if (r1 == 0) goto L_0x01f5
            int r4 = r0.mOneplusAccentColor
            goto L_0x01f7
        L_0x01f5:
            int r4 = r0.mSecondaryColor
        L_0x01f7:
            r2.setStroke(r3, r4)
            int r2 = com.android.systemui.R$id.silence_icon
            android.view.View r2 = r0.findViewById(r2)
            android.widget.ImageView r2 = (android.widget.ImageView) r2
            if (r1 == 0) goto L_0x0207
            int r3 = r0.mOneplusAccentColor
            goto L_0x0209
        L_0x0207:
            int r3 = r0.mSecondaryColor
        L_0x0209:
            r2.setColorFilter(r3)
            int r2 = com.android.systemui.R$id.silence_label
            android.view.View r2 = r0.findViewById(r2)
            android.widget.TextView r2 = (android.widget.TextView) r2
            if (r1 == 0) goto L_0x0219
            int r0 = r0.mOneplusAccentColor
            goto L_0x021b
        L_0x0219:
            int r0 = r0.mSecondaryColor
        L_0x021b:
            r2.setTextColor(r0)
        L_0x021e:
            return
        L_0x021f:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException
            java.lang.String r1 = "bindNotification requires at least one channel"
            r0.<init>(r1)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.row.NotificationInfo.bindNotification(android.content.pm.PackageManager, android.app.INotificationManager, com.android.systemui.statusbar.notification.VisualStabilityManager, java.lang.String, android.app.NotificationChannel, java.util.Set, android.service.notification.StatusBarNotification, com.android.systemui.statusbar.notification.row.NotificationInfo$CheckSaveListener, com.android.systemui.statusbar.notification.row.NotificationInfo$OnSettingsClickListener, com.android.systemui.statusbar.notification.row.NotificationInfo$OnAppSettingsClickListener, boolean, boolean, boolean, int, boolean):void");
    }

    private void bindBlockingHelper() {
        int i = 8;
        findViewById(R$id.inline_controls).setVisibility(8);
        findViewById(R$id.blocking_helper).setVisibility(0);
        findViewById(R$id.undo).setOnClickListener(this.mOnUndo);
        View findViewById = findViewById(R$id.blocking_helper_turn_off_notifications);
        findViewById.setOnClickListener(getSettingsOnClickListener());
        if (findViewById.hasOnClickListeners()) {
            i = 0;
        }
        findViewById.setVisibility(i);
        ((TextView) findViewById(R$id.keep_showing)).setOnClickListener(this.mOnKeepShowing);
        findViewById(R$id.deliver_silently).setOnClickListener(this.mOnDeliverSilently);
    }

    private void bindInlineControls() {
        findViewById(R$id.inline_controls).setVisibility(0);
        int i = 8;
        findViewById(R$id.blocking_helper).setVisibility(8);
        if (this.mIsNonblockable) {
            findViewById(R$id.non_configurable_text).setVisibility(0);
            findViewById(R$id.non_configurable_multichannel_text).setVisibility(8);
            findViewById(R$id.interruptiveness_settings).setVisibility(8);
            ((TextView) findViewById(R$id.done)).setText(R$string.inline_done_button);
            findViewById(R$id.turn_off_notifications).setVisibility(8);
        } else if (this.mNumUniqueChannelsInRow > 1) {
            findViewById(R$id.non_configurable_text).setVisibility(8);
            findViewById(R$id.interruptiveness_settings).setVisibility(8);
            findViewById(R$id.non_configurable_multichannel_text).setVisibility(0);
        } else if (this.mIsInstantApp) {
            findViewById(R$id.non_configurable_text).setVisibility(8);
            findViewById(R$id.non_configurable_multichannel_text).setVisibility(8);
            findViewById(R$id.interruptiveness_settings).setVisibility(8);
            findViewById(R$id.bottom_buttons).setVisibility(0);
        } else {
            findViewById(R$id.non_configurable_text).setVisibility(8);
            findViewById(R$id.non_configurable_multichannel_text).setVisibility(8);
            findViewById(R$id.interruptiveness_settings).setVisibility(0);
        }
        View findViewById = findViewById(R$id.turn_off_notifications);
        findViewById.setOnClickListener(this.mOnStopOrMinimizeNotifications);
        if (findViewById.hasOnClickListeners() && !this.mIsNonblockable) {
            i = 0;
        }
        findViewById.setVisibility(i);
        findViewById(R$id.done).setOnClickListener(this.mOnDismissSettings);
        View findViewById2 = findViewById(R$id.silence);
        View findViewById3 = findViewById(R$id.alert);
        findViewById2.setOnClickListener(this.mOnSilent);
        findViewById3.setOnClickListener(this.mOnAlert);
        applyAlertingBehavior(this.mWasShownHighPriority ^ true ? 1 : 0, false);
    }

    private void bindHeader() {
        this.mPkgIcon = null;
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(this.mPackageName, 795136);
            if (applicationInfo != null) {
                this.mAppName = String.valueOf(this.mPm.getApplicationLabel(applicationInfo));
                this.mPkgIcon = this.mPm.getApplicationIcon(applicationInfo);
            }
        } catch (NameNotFoundException unused) {
            this.mPkgIcon = this.mPm.getDefaultActivityIcon();
        }
        ((ImageView) findViewById(R$id.pkgicon)).setImageDrawable(this.mPkgIcon);
        ((TextView) findViewById(R$id.pkgname)).setText(this.mAppName);
        bindDelegate();
        View findViewById = findViewById(R$id.app_settings);
        Intent appSettingsIntent = getAppSettingsIntent(this.mPm, this.mPackageName, this.mSingleNotificationChannel, this.mSbn.getId(), this.mSbn.getTag());
        int i = 8;
        boolean z = false;
        if (appSettingsIntent == null || TextUtils.isEmpty(this.mSbn.getNotification().getSettingsText())) {
            findViewById.setVisibility(8);
        } else {
            findViewById.setVisibility(0);
            findViewById.setOnClickListener(new OnClickListener(appSettingsIntent) {
                private final /* synthetic */ Intent f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    NotificationInfo.this.lambda$bindHeader$8$NotificationInfo(this.f$1, view);
                }
            });
        }
        View findViewById2 = findViewById(R$id.info);
        findViewById2.setOnClickListener(getSettingsOnClickListener());
        if (findViewById2.hasOnClickListeners()) {
            i = 0;
        }
        findViewById2.setVisibility(i);
        Configuration configuration = getResources().getConfiguration();
        if (configuration != null) {
            if ((configuration.uiMode & 48) == 32) {
                z = true;
            }
            if (z) {
                ((ImageView) findViewById2).setColorFilter(getResources().getColor(R$color.oneplus_contorl_icon_color_active_dark));
                return;
            }
            ((ImageView) findViewById2).setColorFilter(getResources().getColor(R$color.oneplus_contorl_icon_color_active_light));
        }
    }

    public /* synthetic */ void lambda$bindHeader$8$NotificationInfo(Intent intent, View view) {
        this.mAppSettingsClickListener.onClick(view, intent);
    }

    private OnClickListener getSettingsOnClickListener() {
        int i = this.mAppUid;
        if (i < 0 || this.mOnSettingsClickListener == null || !this.mIsDeviceProvisioned) {
            return null;
        }
        return new OnClickListener(i) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                NotificationInfo.this.lambda$getSettingsOnClickListener$9$NotificationInfo(this.f$1, view);
            }
        };
    }

    public /* synthetic */ void lambda$getSettingsOnClickListener$9$NotificationInfo(int i, View view) {
        logBlockingHelperCounter("blocking_helper_notif_settings");
        this.mOnSettingsClickListener.onClick(view, this.mNumUniqueChannelsInRow > 1 ? null : this.mSingleNotificationChannel, i);
    }

    private void bindChannelDetails() throws RemoteException {
        bindName();
        bindGroup();
    }

    private void bindName() {
        TextView textView = (TextView) findViewById(R$id.channel_name);
        if (this.mIsSingleDefaultChannel || this.mNumUniqueChannelsInRow > 1) {
            textView.setVisibility(8);
        } else {
            textView.setText(this.mSingleNotificationChannel.getName());
        }
    }

    private void bindDelegate() {
        TextView textView = (TextView) findViewById(R$id.delegate_name);
        TextView textView2 = (TextView) findViewById(R$id.pkg_divider);
        if (!TextUtils.equals(this.mPackageName, this.mDelegatePkg)) {
            textView.setVisibility(0);
            textView2.setVisibility(0);
            return;
        }
        textView.setVisibility(8);
        textView2.setVisibility(8);
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x002c  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0034  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void bindGroup() throws android.os.RemoteException {
        /*
            r4 = this;
            android.app.NotificationChannel r0 = r4.mSingleNotificationChannel
            if (r0 == 0) goto L_0x0021
            java.lang.String r0 = r0.getGroup()
            if (r0 == 0) goto L_0x0021
            android.app.INotificationManager r0 = r4.mINotificationManager
            android.app.NotificationChannel r1 = r4.mSingleNotificationChannel
            java.lang.String r1 = r1.getGroup()
            java.lang.String r2 = r4.mPackageName
            int r3 = r4.mAppUid
            android.app.NotificationChannelGroup r0 = r0.getNotificationChannelGroupForPackage(r1, r2, r3)
            if (r0 == 0) goto L_0x0021
            java.lang.CharSequence r0 = r0.getName()
            goto L_0x0022
        L_0x0021:
            r0 = 0
        L_0x0022:
            int r1 = com.android.systemui.R$id.group_name
            android.view.View r4 = r4.findViewById(r1)
            android.widget.TextView r4 = (android.widget.TextView) r4
            if (r0 == 0) goto L_0x0034
            r4.setText(r0)
            r0 = 0
            r4.setVisibility(r0)
            goto L_0x0039
        L_0x0034:
            r0 = 8
            r4.setVisibility(r0)
        L_0x0039:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.row.NotificationInfo.bindGroup():void");
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void logBlockingHelperCounter(String str) {
        if (this.mIsForBlockingHelper) {
            this.mMetricsLogger.count(str, 1);
        }
    }

    private void saveImportance() {
        if (!this.mIsNonblockable || this.mExitReason != "blocking_helper_stop_notifications") {
            if (this.mChosenImportance == null) {
                this.mChosenImportance = Integer.valueOf(this.mStartingChannelImportance);
            }
            updateImportance();
        }
    }

    private void updateImportance() {
        if (this.mChosenImportance != null) {
            this.mMetricsLogger.write(importanceChangeLogMaker());
            int intValue = this.mChosenImportance.intValue();
            Handler handler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
            UpdateImportanceRunnable updateImportanceRunnable = new UpdateImportanceRunnable(this.mINotificationManager, this.mPackageName, this.mAppUid, this.mNumUniqueChannelsInRow == 1 ? this.mSingleNotificationChannel : null, this.mStartingChannelImportance, intValue, this.mContext, this.mSbn, this.mIsInstantApp);
            handler.post(updateImportanceRunnable);
            this.mVisualStabilityManager.temporarilyAllowReordering();
        }
    }

    private void applyAlertingBehavior(int i, boolean z) {
        boolean z2 = true;
        if (z && this.mCurrentAlertingBehavior != i) {
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.setOrdering(0);
            transitionSet.addTransition(new Fade(2)).addTransition(new ChangeBounds()).addTransition(new Fade(1).setStartDelay(150).setDuration(200).setInterpolator(Interpolators.FAST_OUT_SLOW_IN));
            transitionSet.setDuration(350);
            transitionSet.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            TransitionManager.beginDelayedTransition(this, transitionSet);
        }
        View findViewById = findViewById(R$id.alert);
        View findViewById2 = findViewById(R$id.silence);
        this.mCurrentAlertingBehavior = i;
        if (i == 0) {
            this.mPriorityDescriptionView.setVisibility(0);
            this.mSilentDescriptionView.setVisibility(8);
            post(new Runnable(findViewById, findViewById2) {
                private final /* synthetic */ View f$1;
                private final /* synthetic */ View f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    NotificationInfo.this.lambda$applyAlertingBehavior$12$NotificationInfo(this.f$1, this.f$2);
                }
            });
        } else if (i == 1) {
            this.mSilentDescriptionView.setVisibility(0);
            this.mPriorityDescriptionView.setVisibility(8);
            post(new Runnable(findViewById, findViewById2) {
                private final /* synthetic */ View f$1;
                private final /* synthetic */ View f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    NotificationInfo.this.lambda$applyAlertingBehavior$13$NotificationInfo(this.f$1, this.f$2);
                }
            });
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Unrecognized alerting behavior: ");
            sb.append(i);
            throw new IllegalArgumentException(sb.toString());
        }
        if (this.mWasShownHighPriority == (i == 0)) {
            z2 = false;
        }
        ((TextView) findViewById(R$id.done)).setText(z2 ? R$string.inline_ok_button : R$string.inline_done_button);
    }

    public /* synthetic */ void lambda$applyAlertingBehavior$12$NotificationInfo(View view, View view2) {
        view.setSelected(true);
        view2.setSelected(false);
        if (this.mOneplusAccentColor != 0) {
            ((GradientDrawable) view2.getBackground()).setStroke(this.mImportanceButtonStroke, this.mSecondaryColor);
            ((ImageView) findViewById(R$id.silence_icon)).setColorFilter(this.mSecondaryColor);
            ((TextView) findViewById(R$id.silence_label)).setTextColor(this.mSecondaryColor);
            ((GradientDrawable) view.getBackground()).setStroke(this.mImportanceButtonStroke, this.mOneplusAccentColor);
            ((ImageView) findViewById(R$id.alert_icon)).setColorFilter(this.mOneplusAccentColor);
            ((TextView) findViewById(R$id.alert_label)).setTextColor(this.mOneplusAccentColor);
        }
    }

    public /* synthetic */ void lambda$applyAlertingBehavior$13$NotificationInfo(View view, View view2) {
        view.setSelected(false);
        view2.setSelected(true);
        if (this.mOneplusAccentColor != 0) {
            ((GradientDrawable) view.getBackground()).setStroke(this.mImportanceButtonStroke, this.mSecondaryColor);
            ((ImageView) findViewById(R$id.alert_icon)).setColorFilter(this.mSecondaryColor);
            ((TextView) findViewById(R$id.alert_label)).setTextColor(this.mSecondaryColor);
            ((GradientDrawable) view2.getBackground()).setStroke(this.mImportanceButtonStroke, this.mOneplusAccentColor);
            ((ImageView) findViewById(R$id.silence_icon)).setColorFilter(this.mOneplusAccentColor);
            ((TextView) findViewById(R$id.silence_label)).setTextColor(this.mOneplusAccentColor);
        }
    }

    private void saveImportanceAndExitReason(int i) {
        int i2;
        String str = "notify";
        if (i == 1) {
            this.mExitReason = "blocking_helper_undo";
            if (this.mIsInstantApp) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(str, Integer.valueOf(1));
                this.mContext.getContentResolver().update(Uri.withAppendedPath(INSTANT_APP_BASE_URI, this.mInstantAppPkg), contentValues, null, null);
            } else {
                this.mChosenImportance = Integer.valueOf(this.mStartingChannelImportance);
            }
            this.mPressedApply = true;
        } else if (i == 3) {
            this.mExitReason = "blocking_helper_stop_notifications";
            if (this.mIsInstantApp) {
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put(str, Integer.valueOf(0));
                this.mContext.getContentResolver().update(Uri.withAppendedPath(INSTANT_APP_BASE_URI, this.mInstantAppPkg), contentValues2, null, null);
            } else {
                this.mChosenImportance = Integer.valueOf(0);
            }
            this.mPressedApply = true;
        } else if (i == 4) {
            this.mExitReason = "blocking_helper_deliver_silently";
            if (this.mWasShownHighPriority) {
                i2 = 2;
            } else {
                i2 = this.mStartingChannelImportance;
            }
            this.mChosenImportance = Integer.valueOf(i2);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void swapContent(int i, boolean z) {
        AnimatorSet animatorSet = this.mExpandAnimation;
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        final View findViewById = findViewById(R$id.blocking_helper);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R$id.confirmation);
        TextView textView = (TextView) findViewById(R$id.confirmation_text);
        ((TextView) findViewById(R$id.undo)).setOnClickListener(this.mOnUndo);
        saveImportanceAndExitReason(i);
        int i2 = 8;
        if (i == 1) {
            bindInlineControls();
            findViewById(R$id.blocking_helper_text).setVisibility(8);
            findViewById(R$id.block_buttons).setVisibility(8);
        } else if (i == 3) {
            textView.setText(R$string.notification_channel_disabled);
            findViewById(R$id.inline_controls).setVisibility(8);
        } else if (i == 4) {
            textView.setText(R$string.notification_channel_silenced);
        } else {
            throw new IllegalArgumentException();
        }
        final boolean z2 = i == 1;
        findViewById(R$id.channel_info).setVisibility(z2 ? 0 : 8);
        findViewById(R$id.header).setVisibility(z2 ? 0 : 8);
        if (!z2) {
            i2 = 0;
        }
        viewGroup.setVisibility(i2);
        if (z) {
            Property property = View.ALPHA;
            float[] fArr = new float[2];
            fArr[0] = findViewById.getAlpha();
            float f = 1.0f;
            fArr[1] = z2 ? 1.0f : 0.0f;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(findViewById, property, fArr);
            ofFloat.setInterpolator(z2 ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT);
            Property property2 = View.ALPHA;
            float[] fArr2 = new float[2];
            fArr2[0] = viewGroup.getAlpha();
            if (z2) {
                f = 0.0f;
            }
            fArr2[1] = f;
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(viewGroup, property2, fArr2);
            ofFloat2.setInterpolator(z2 ? Interpolators.ALPHA_OUT : Interpolators.ALPHA_IN);
            this.mExpandAnimation = new AnimatorSet();
            this.mExpandAnimation.playTogether(new Animator[]{ofFloat, ofFloat2});
            this.mExpandAnimation.setDuration(150);
            this.mExpandAnimation.addListener(new AnimatorListenerAdapter() {
                boolean mCancelled = false;

                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        int i = 0;
                        findViewById.setVisibility(z2 ? 0 : 8);
                        ViewGroup viewGroup = viewGroup;
                        if (z2) {
                            i = 8;
                        }
                        viewGroup.setVisibility(i);
                    }
                }
            });
            this.mExpandAnimation.start();
        }
        NotificationGuts notificationGuts = this.mGutsContainer;
        if (notificationGuts != null) {
            notificationGuts.resetFalsingCheck();
        }
    }

    public void onFinishedClosing() {
        Integer num = this.mChosenImportance;
        if (num != null) {
            this.mStartingChannelImportance = num.intValue();
        }
        String str = this.mExitReason;
        if (str == "blocking_helper_stop_notifications" || str == "blocking_helper_undo") {
            bindInlineControls();
            findViewById(R$id.channel_info).setVisibility(0);
            findViewById(R$id.header).setVisibility(0);
            findViewById(R$id.confirmation).setVisibility(8);
        }
        this.mExitReason = "blocking_helper_dismissed";
        if (this.mIsForBlockingHelper) {
            bindBlockingHelper();
        } else {
            bindInlineControls();
        }
        this.mMetricsLogger.write(notificationControlsLogMaker().setType(2));
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (this.mGutsContainer != null && accessibilityEvent.getEventType() == 32) {
            if (this.mGutsContainer.isExposed()) {
                accessibilityEvent.getText().add(this.mContext.getString(R$string.notification_channel_controls_opened_accessibility, new Object[]{this.mAppName}));
                return;
            }
            accessibilityEvent.getText().add(this.mContext.getString(R$string.notification_channel_controls_closed_accessibility, new Object[]{this.mAppName}));
        }
    }

    private Intent getAppSettingsIntent(PackageManager packageManager, String str, NotificationChannel notificationChannel, int i, String str2) {
        Intent intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES").setPackage(str);
        List queryIntentActivities = packageManager.queryIntentActivities(intent, 65536);
        if (queryIntentActivities == null || queryIntentActivities.size() == 0 || queryIntentActivities.get(0) == null) {
            return null;
        }
        ActivityInfo activityInfo = ((ResolveInfo) queryIntentActivities.get(0)).activityInfo;
        intent.setClassName(activityInfo.packageName, activityInfo.name);
        if (notificationChannel != null) {
            intent.putExtra("android.intent.extra.CHANNEL_ID", notificationChannel.getId());
        }
        intent.putExtra("android.intent.extra.NOTIFICATION_ID", i);
        intent.putExtra("android.intent.extra.NOTIFICATION_TAG", str2);
        return intent;
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void closeControls(View view, boolean z) {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        this.mGutsContainer.getLocationOnScreen(iArr);
        view.getLocationOnScreen(iArr2);
        int width = view.getWidth() / 2;
        this.mGutsContainer.closeControls((iArr2[0] - iArr[0]) + width, (iArr2[1] - iArr[1]) + (view.getHeight() / 2), z, false);
    }

    public void setGutsParent(NotificationGuts notificationGuts) {
        this.mGutsContainer = notificationGuts;
    }

    public boolean shouldBeSaved() {
        return this.mPressedApply;
    }

    public boolean handleCloseControls(boolean z, boolean z2) {
        if (this.mPresentingChannelEditorDialog) {
            ChannelEditorDialogController channelEditorDialogController = this.mChannelEditorDialogController;
            if (channelEditorDialogController != null) {
                this.mPresentingChannelEditorDialog = false;
                channelEditorDialogController.setOnFinishListener(null);
                this.mChannelEditorDialogController.close();
            }
        }
        if (z) {
            saveImportance();
        }
        logBlockingHelperCounter(this.mExitReason);
        return false;
    }

    public int getActualHeight() {
        return getHeight();
    }

    @VisibleForTesting
    public boolean isAnimating() {
        AnimatorSet animatorSet = this.mExpandAnimation;
        return animatorSet != null && animatorSet.isRunning();
    }

    private LogMaker getLogMaker() {
        StatusBarNotification statusBarNotification = this.mSbn;
        if (statusBarNotification == null) {
            return new LogMaker(1621);
        }
        return statusBarNotification.getLogMaker().setCategory(1621);
    }

    private LogMaker importanceChangeLogMaker() {
        Integer num = this.mChosenImportance;
        return getLogMaker().setCategory(291).setType(4).setSubtype(Integer.valueOf(num != null ? num.intValue() : this.mStartingChannelImportance).intValue() - this.mStartingChannelImportance);
    }

    private LogMaker notificationControlsLogMaker() {
        return getLogMaker().setCategory(204).setType(1).setSubtype(this.mIsForBlockingHelper ? 1 : 0);
    }
}
