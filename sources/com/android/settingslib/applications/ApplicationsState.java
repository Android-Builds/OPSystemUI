package com.android.settingslib.applications;

import android.app.ActivityManager;
import android.app.Application;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver.Stub;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.SparseArray;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.Utils;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class ApplicationsState {
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            int compare = this.sCollator.compare(appEntry.label, appEntry2.label);
            if (compare != 0) {
                return compare;
            }
            ApplicationInfo applicationInfo = appEntry.info;
            if (!(applicationInfo == null || appEntry2.info == null)) {
                int compare2 = this.sCollator.compare(applicationInfo.packageName, appEntry2.info.packageName);
                if (compare2 != 0) {
                    return compare2;
                }
            }
            return appEntry.info.uid - appEntry2.info.uid;
        }
    };
    public static final Comparator<AppEntry> EXTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            long j = appEntry.externalSize;
            long j2 = appEntry2.externalSize;
            if (j < j2) {
                return 1;
            }
            if (j > j2) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    public static final AppFilter FILTER_ALL_ENABLED = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            ApplicationInfo applicationInfo = appEntry.info;
            return applicationInfo.enabled && !AppUtils.isInstant(applicationInfo);
        }
    };
    public static final AppFilter FILTER_AUDIO = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = true;
                if (appEntry.info.category != 1) {
                    z = false;
                }
            }
            return z;
        }
    };
    public static final AppFilter FILTER_DISABLED = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            ApplicationInfo applicationInfo = appEntry.info;
            return !applicationInfo.enabled && !AppUtils.isInstant(applicationInfo);
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            if (AppUtils.isInstant(appEntry.info)) {
                return false;
            }
            if (ApplicationsState.hasFlag(appEntry.info.flags, 128) || !ApplicationsState.hasFlag(appEntry.info.flags, 1) || appEntry.hasLauncherEntry) {
                return true;
            }
            if (!ApplicationsState.hasFlag(appEntry.info.flags, 1) || !appEntry.isHomeApp) {
                return false;
            }
            return true;
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER_AND_INSTANT = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info) || ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(appEntry);
        }
    };
    public static final AppFilter FILTER_EVERYTHING = new AppFilter() {
        public boolean filterApp(AppEntry appEntry) {
            return true;
        }

        public void init() {
        }
    };
    public static final AppFilter FILTER_GAMES = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry.info) {
                if (!ApplicationsState.hasFlag(appEntry.info.flags, 33554432)) {
                    if (appEntry.info.category != 0) {
                        z = false;
                    }
                }
                z = true;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_INSTANT = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info);
        }
    };
    public static final AppFilter FILTER_MOVIES = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 2;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_NOT_HIDE = new AppFilter() {
        private String[] mHidePackageNames;

        public void init() {
        }

        public void init(Context context) {
            this.mHidePackageNames = context.getResources().getStringArray(17236056);
        }

        public boolean filterApp(AppEntry appEntry) {
            if (ArrayUtils.contains(this.mHidePackageNames, appEntry.info.packageName)) {
                ApplicationInfo applicationInfo = appEntry.info;
                if (!applicationInfo.enabled || applicationInfo.enabledSetting == 4) {
                    return false;
                }
            }
            return true;
        }
    };
    public static final AppFilter FILTER_OTHER_APPS = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                if (!ApplicationsState.FILTER_AUDIO.filterApp(appEntry) && !ApplicationsState.FILTER_GAMES.filterApp(appEntry) && !ApplicationsState.FILTER_MOVIES.filterApp(appEntry)) {
                    if (!ApplicationsState.FILTER_PHOTOS.filterApp(appEntry)) {
                        z = false;
                    }
                }
                z = true;
            }
            return !z;
        }
    };
    public static final AppFilter FILTER_PERSONAL = new AppFilter() {
        private int mCurrentUser;

        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) == this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_PHOTOS = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 3;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_THIRD_PARTY = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            if (!ApplicationsState.hasFlag(appEntry.info.flags, 128) && ApplicationsState.hasFlag(appEntry.info.flags, 1)) {
                return false;
            }
            return true;
        }
    };
    public static final AppFilter FILTER_WITHOUT_DISABLED_UNTIL_USED = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            return appEntry.info.enabledSetting != 4;
        }
    };
    public static final AppFilter FILTER_WITH_DOMAIN_URLS = new AppFilter() {
        public void init() {
        }

        public boolean filterApp(AppEntry appEntry) {
            return !AppUtils.isInstant(appEntry.info) && ApplicationsState.hasFlag(appEntry.info.privateFlags, 16);
        }
    };
    public static final AppFilter FILTER_WORK = new AppFilter() {
        private int mCurrentUser;

        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) != this.mCurrentUser;
        }
    };
    public static final Comparator<AppEntry> INTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            long j = appEntry.internalSize;
            long j2 = appEntry2.internalSize;
            if (j < j2) {
                return 1;
            }
            if (j > j2) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    private static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            long j = appEntry.size;
            long j2 = appEntry2.size;
            if (j < j2) {
                return 1;
            }
            if (j > j2) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    /* access modifiers changed from: private */
    public static final ArrayList<String> THEME_OVERLAY_CATEGORYS = new ArrayList<String>() {
        {
            add("oneplus_basiccolor_black");
            add("oneplus_basiccolor_white");
            add("oneplus_aodnotification_gold");
            add("oneplus_aodnotification_purple");
            add("oneplus_aodnotification_red");
            add("oneplus_shape_circle");
            add("oneplus_shape_roundedrect");
            add("oneplus_shape_square");
            add("oneplus_shape_squircle");
            add("oneplus_shape_teardrop");
        }
    };
    static ApplicationsState sInstance;
    private static final Object sLock = new Object();
    final ArrayList<WeakReference<Session>> mActiveSessions = new ArrayList<>();
    final int mAdminRetrieveFlags;
    final ArrayList<AppEntry> mAppEntries = new ArrayList<>();
    List<ApplicationInfo> mApplications = new ArrayList();
    final BackgroundHandler mBackgroundHandler;
    final Context mContext;
    String mCurComputingSizePkg;
    int mCurComputingSizeUserId;
    UUID mCurComputingSizeUuid;
    long mCurId = 1;
    final IconDrawableFactory mDrawableFactory;
    final SparseArray<HashMap<String, AppEntry>> mEntriesMap = new SparseArray<>();
    boolean mHaveDisabledApps;
    boolean mHaveInstantApps;
    private InterestingConfigChanges mInterestingConfigChanges = new InterestingConfigChanges();
    final IPackageManager mIpm;
    final MainHandler mMainHandler = new MainHandler(Looper.getMainLooper());
    PackageIntentReceiver mPackageIntentReceiver;
    final PackageManager mPm;
    final ArrayList<Session> mRebuildingSessions = new ArrayList<>();
    boolean mResumed;
    final int mRetrieveFlags;
    final ArrayList<Session> mSessions = new ArrayList<>();
    boolean mSessionsChanged;
    final StorageStatsManager mStats;
    final HashMap<String, Boolean> mSystemModules = new HashMap<>();
    final HashMap<String, Boolean> mSystemPersistApplications = new HashMap<>();
    final HandlerThread mThread;
    final UserManager mUm;

    public static class AppEntry extends SizeInfo {
        public final File apkFile;
        public long externalSize;
        public String externalSizeStr;
        public boolean hasLauncherEntry;
        public Drawable icon;

        /* renamed from: id */
        public final long f54id;
        public ApplicationInfo info;
        public long internalSize;
        public String internalSizeStr;
        public boolean isHomeApp;
        public String label;
        public boolean launcherEntryEnabled;
        public boolean mounted;
        public long size = -1;
        public long sizeLoadStart;
        public boolean sizeStale = true;
        public String sizeStr;

        public AppEntry(Context context, ApplicationInfo applicationInfo, long j) {
            this.apkFile = new File(applicationInfo.sourceDir);
            this.f54id = j;
            this.info = applicationInfo;
            ensureLabel(context);
        }

        public void ensureLabel(Context context) {
            if (this.label != null && this.mounted) {
                return;
            }
            if (!this.apkFile.exists()) {
                this.mounted = false;
                this.label = this.info.packageName;
                return;
            }
            this.mounted = true;
            CharSequence loadLabel = this.info.loadLabel(context.getPackageManager());
            this.label = loadLabel != null ? loadLabel.toString() : this.info.packageName;
        }

        /* access modifiers changed from: 0000 */
        public boolean ensureIconLocked(Context context, IconDrawableFactory iconDrawableFactory) {
            if (this.icon == null) {
                if (this.apkFile.exists()) {
                    this.icon = iconDrawableFactory.getBadgedIcon(this.info);
                    return true;
                }
                this.mounted = false;
                this.icon = context.getDrawable(17303643);
            } else if (!this.mounted && this.apkFile.exists()) {
                this.mounted = true;
                this.icon = iconDrawableFactory.getBadgedIcon(this.info);
                return true;
            }
            return false;
        }
    }

    public interface AppFilter {
        boolean filterApp(AppEntry appEntry);

        void init();

        void init(Context context) {
            init();
        }
    }

    private class BackgroundHandler extends Handler {
        boolean mRunning;
        final Stub mStatsObserver = new Stub() {
            /* JADX WARNING: Code restructure failed: missing block: B:46:0x010f, code lost:
                return;
             */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onGetStatsCompleted(android.content.pm.PackageStats r13, boolean r14) {
                /*
                    r12 = this;
                    if (r14 != 0) goto L_0x0003
                    return
                L_0x0003:
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r14 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this
                    com.android.settingslib.applications.ApplicationsState r14 = com.android.settingslib.applications.ApplicationsState.this
                    android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r14 = r14.mEntriesMap
                    monitor-enter(r14)
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r0 = r0.mEntriesMap     // Catch:{ all -> 0x0110 }
                    int r1 = r13.userHandle     // Catch:{ all -> 0x0110 }
                    java.lang.Object r0 = r0.get(r1)     // Catch:{ all -> 0x0110 }
                    java.util.HashMap r0 = (java.util.HashMap) r0     // Catch:{ all -> 0x0110 }
                    if (r0 != 0) goto L_0x001c
                    monitor-exit(r14)     // Catch:{ all -> 0x0110 }
                    return
                L_0x001c:
                    java.lang.String r1 = r13.packageName     // Catch:{ all -> 0x0110 }
                    java.lang.Object r0 = r0.get(r1)     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState$AppEntry r0 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r0     // Catch:{ all -> 0x0110 }
                    if (r0 == 0) goto L_0x00e1
                    monitor-enter(r0)     // Catch:{ all -> 0x0110 }
                    r1 = 0
                    r0.sizeStale = r1     // Catch:{ all -> 0x00de }
                    r2 = 0
                    r0.sizeLoadStart = r2     // Catch:{ all -> 0x00de }
                    long r2 = r13.externalCodeSize     // Catch:{ all -> 0x00de }
                    long r4 = r13.externalObbSize     // Catch:{ all -> 0x00de }
                    long r2 = r2 + r4
                    long r4 = r13.externalDataSize     // Catch:{ all -> 0x00de }
                    long r6 = r13.externalMediaSize     // Catch:{ all -> 0x00de }
                    long r4 = r4 + r6
                    long r6 = r2 + r4
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r8 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r8 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r8 = r8.getTotalInternalSize(r13)     // Catch:{ all -> 0x00de }
                    long r6 = r6 + r8
                    long r8 = r0.size     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r6 ? 1 : (r8 == r6 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.cacheSize     // Catch:{ all -> 0x00de }
                    long r10 = r13.cacheSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.codeSize     // Catch:{ all -> 0x00de }
                    long r10 = r13.codeSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.dataSize     // Catch:{ all -> 0x00de }
                    long r10 = r13.dataSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.externalCodeSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r2 ? 1 : (r8 == r2 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.externalDataSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r4 ? 1 : (r8 == r4 ? 0 : -1))
                    if (r8 != 0) goto L_0x0075
                    long r8 = r0.externalCacheSize     // Catch:{ all -> 0x00de }
                    long r10 = r13.externalCacheSize     // Catch:{ all -> 0x00de }
                    int r8 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                    if (r8 == 0) goto L_0x00c4
                L_0x0075:
                    r0.size = r6     // Catch:{ all -> 0x00de }
                    long r6 = r13.cacheSize     // Catch:{ all -> 0x00de }
                    r0.cacheSize = r6     // Catch:{ all -> 0x00de }
                    long r6 = r13.codeSize     // Catch:{ all -> 0x00de }
                    r0.codeSize = r6     // Catch:{ all -> 0x00de }
                    long r6 = r13.dataSize     // Catch:{ all -> 0x00de }
                    r0.dataSize = r6     // Catch:{ all -> 0x00de }
                    r0.externalCodeSize = r2     // Catch:{ all -> 0x00de }
                    r0.externalDataSize = r4     // Catch:{ all -> 0x00de }
                    long r1 = r13.externalCacheSize     // Catch:{ all -> 0x00de }
                    r0.externalCacheSize = r1     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r2 = r0.size     // Catch:{ all -> 0x00de }
                    java.lang.String r1 = r1.getSizeStr(r2)     // Catch:{ all -> 0x00de }
                    r0.sizeStr = r1     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r1 = r1.getTotalInternalSize(r13)     // Catch:{ all -> 0x00de }
                    r0.internalSize = r1     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r2 = r0.internalSize     // Catch:{ all -> 0x00de }
                    java.lang.String r1 = r1.getSizeStr(r2)     // Catch:{ all -> 0x00de }
                    r0.internalSizeStr = r1     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r1 = r1.getTotalExternalSize(r13)     // Catch:{ all -> 0x00de }
                    r0.externalSize = r1     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x00de }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x00de }
                    long r2 = r0.externalSize     // Catch:{ all -> 0x00de }
                    java.lang.String r1 = r1.getSizeStr(r2)     // Catch:{ all -> 0x00de }
                    r0.externalSizeStr = r1     // Catch:{ all -> 0x00de }
                    r1 = 1
                L_0x00c4:
                    monitor-exit(r0)     // Catch:{ all -> 0x00de }
                    if (r1 == 0) goto L_0x00e1
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x0110 }
                    r1 = 4
                    java.lang.String r2 = r13.packageName     // Catch:{ all -> 0x0110 }
                    android.os.Message r0 = r0.obtainMessage(r1, r2)     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r1 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState$MainHandler r1 = r1.mMainHandler     // Catch:{ all -> 0x0110 }
                    r1.sendMessage(r0)     // Catch:{ all -> 0x0110 }
                    goto L_0x00e1
                L_0x00de:
                    r12 = move-exception
                    monitor-exit(r0)     // Catch:{ all -> 0x00de }
                    throw r12     // Catch:{ all -> 0x0110 }
                L_0x00e1:
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    java.lang.String r0 = r0.mCurComputingSizePkg     // Catch:{ all -> 0x0110 }
                    if (r0 == 0) goto L_0x010e
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    java.lang.String r0 = r0.mCurComputingSizePkg     // Catch:{ all -> 0x0110 }
                    java.lang.String r1 = r13.packageName     // Catch:{ all -> 0x0110 }
                    boolean r0 = r0.equals(r1)     // Catch:{ all -> 0x0110 }
                    if (r0 == 0) goto L_0x010e
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    int r0 = r0.mCurComputingSizeUserId     // Catch:{ all -> 0x0110 }
                    int r13 = r13.userHandle     // Catch:{ all -> 0x0110 }
                    if (r0 != r13) goto L_0x010e
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r13 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState r13 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0110 }
                    r0 = 0
                    r13.mCurComputingSizePkg = r0     // Catch:{ all -> 0x0110 }
                    com.android.settingslib.applications.ApplicationsState$BackgroundHandler r12 = com.android.settingslib.applications.ApplicationsState.BackgroundHandler.this     // Catch:{ all -> 0x0110 }
                    r13 = 7
                    r12.sendEmptyMessage(r13)     // Catch:{ all -> 0x0110 }
                L_0x010e:
                    monitor-exit(r14)     // Catch:{ all -> 0x0110 }
                    return
                L_0x0110:
                    r12 = move-exception
                    monitor-exit(r14)     // Catch:{ all -> 0x0110 }
                    throw r12
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.applications.ApplicationsState.BackgroundHandler.C06691.onGetStatsCompleted(android.content.pm.PackageStats, boolean):void");
            }
        };

        BackgroundHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:43:0x00ef, code lost:
            return;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r19) {
            /*
                r18 = this;
                r1 = r18
                r0 = r19
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$Session> r2 = r2.mRebuildingSessions
                monitor-enter(r2)
                com.android.settingslib.applications.ApplicationsState r3 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b5 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$Session> r3 = r3.mRebuildingSessions     // Catch:{ all -> 0x03b5 }
                int r3 = r3.size()     // Catch:{ all -> 0x03b5 }
                r4 = 0
                if (r3 <= 0) goto L_0x0025
                java.util.ArrayList r3 = new java.util.ArrayList     // Catch:{ all -> 0x03b5 }
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b5 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$Session> r5 = r5.mRebuildingSessions     // Catch:{ all -> 0x03b5 }
                r3.<init>(r5)     // Catch:{ all -> 0x03b5 }
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b5 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$Session> r5 = r5.mRebuildingSessions     // Catch:{ all -> 0x03b5 }
                r5.clear()     // Catch:{ all -> 0x03b5 }
                goto L_0x0026
            L_0x0025:
                r3 = r4
            L_0x0026:
                monitor-exit(r2)     // Catch:{ all -> 0x03b5 }
                r2 = 0
                if (r3 == 0) goto L_0x003d
                r5 = r2
            L_0x002b:
                int r6 = r3.size()
                if (r5 >= r6) goto L_0x003d
                java.lang.Object r6 = r3.get(r5)
                com.android.settingslib.applications.ApplicationsState$Session r6 = (com.android.settingslib.applications.ApplicationsState.Session) r6
                r6.handleRebuildList()
                int r5 = r5 + 1
                goto L_0x002b
            L_0x003d:
                com.android.settingslib.applications.ApplicationsState r3 = com.android.settingslib.applications.ApplicationsState.this
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$Session> r3 = r3.mSessions
                int r3 = r1.getCombinedSessionFlags(r3)
                int r5 = r0.what
                r6 = 8388608(0x800000, float:1.17549435E-38)
                r7 = 3
                r8 = 7
                r9 = 8
                r10 = 5
                r11 = 2
                r12 = 4
                r13 = 6
                r14 = 1
                switch(r5) {
                    case 1: goto L_0x03b4;
                    case 2: goto L_0x02c7;
                    case 3: goto L_0x026d;
                    case 4: goto L_0x019e;
                    case 5: goto L_0x019e;
                    case 6: goto L_0x0120;
                    case 7: goto L_0x0057;
                    default: goto L_0x0055;
                }
            L_0x0055:
                goto L_0x03b4
            L_0x0057:
                boolean r0 = com.android.settingslib.applications.ApplicationsState.hasFlag(r3, r12)
                if (r0 == 0) goto L_0x03b4
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r3 = r0.mEntriesMap
                monitor-enter(r3)
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                java.lang.String r0 = r0.mCurComputingSizePkg     // Catch:{ all -> 0x011d }
                if (r0 == 0) goto L_0x006a
                monitor-exit(r3)     // Catch:{ all -> 0x011d }
                return
            L_0x006a:
                long r4 = android.os.SystemClock.uptimeMillis()     // Catch:{ all -> 0x011d }
                r0 = r2
            L_0x006f:
                com.android.settingslib.applications.ApplicationsState r7 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r7 = r7.mAppEntries     // Catch:{ all -> 0x011d }
                int r7 = r7.size()     // Catch:{ all -> 0x011d }
                if (r0 >= r7) goto L_0x00f4
                com.android.settingslib.applications.ApplicationsState r7 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r7 = r7.mAppEntries     // Catch:{ all -> 0x011d }
                java.lang.Object r7 = r7.get(r0)     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$AppEntry r7 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r7     // Catch:{ all -> 0x011d }
                android.content.pm.ApplicationInfo r8 = r7.info     // Catch:{ all -> 0x011d }
                int r8 = r8.flags     // Catch:{ all -> 0x011d }
                boolean r8 = com.android.settingslib.applications.ApplicationsState.hasFlag(r8, r6)     // Catch:{ all -> 0x011d }
                if (r8 == 0) goto L_0x00f0
                long r8 = r7.size     // Catch:{ all -> 0x011d }
                r11 = -1
                int r8 = (r8 > r11 ? 1 : (r8 == r11 ? 0 : -1))
                if (r8 == 0) goto L_0x0099
                boolean r8 = r7.sizeStale     // Catch:{ all -> 0x011d }
                if (r8 == 0) goto L_0x00f0
            L_0x0099:
                long r8 = r7.sizeLoadStart     // Catch:{ all -> 0x011d }
                r10 = 0
                int r0 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                if (r0 == 0) goto L_0x00ab
                long r8 = r7.sizeLoadStart     // Catch:{ all -> 0x011d }
                r10 = 20000(0x4e20, double:9.8813E-320)
                long r10 = r4 - r10
                int r0 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1))
                if (r0 >= 0) goto L_0x00ee
            L_0x00ab:
                boolean r0 = r1.mRunning     // Catch:{ all -> 0x011d }
                if (r0 != 0) goto L_0x00c4
                r1.mRunning = r14     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x011d }
                java.lang.Integer r2 = java.lang.Integer.valueOf(r14)     // Catch:{ all -> 0x011d }
                android.os.Message r0 = r0.obtainMessage(r13, r2)     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r2 = r2.mMainHandler     // Catch:{ all -> 0x011d }
                r2.sendMessage(r0)     // Catch:{ all -> 0x011d }
            L_0x00c4:
                r7.sizeLoadStart = r4     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                android.content.pm.ApplicationInfo r2 = r7.info     // Catch:{ all -> 0x011d }
                java.util.UUID r2 = r2.storageUuid     // Catch:{ all -> 0x011d }
                r0.mCurComputingSizeUuid = r2     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                android.content.pm.ApplicationInfo r2 = r7.info     // Catch:{ all -> 0x011d }
                java.lang.String r2 = r2.packageName     // Catch:{ all -> 0x011d }
                r0.mCurComputingSizePkg = r2     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                android.content.pm.ApplicationInfo r2 = r7.info     // Catch:{ all -> 0x011d }
                int r2 = r2.uid     // Catch:{ all -> 0x011d }
                int r2 = android.os.UserHandle.getUserId(r2)     // Catch:{ all -> 0x011d }
                r0.mCurComputingSizeUserId = r2     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$BackgroundHandler r0 = r0.mBackgroundHandler     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.-$$Lambda$ApplicationsState$BackgroundHandler$7jhXQzAcRoT6ACDzmPBTQMi7Ldc r2 = new com.android.settingslib.applications.-$$Lambda$ApplicationsState$BackgroundHandler$7jhXQzAcRoT6ACDzmPBTQMi7Ldc     // Catch:{ all -> 0x011d }
                r2.<init>()     // Catch:{ all -> 0x011d }
                r0.post(r2)     // Catch:{ all -> 0x011d }
            L_0x00ee:
                monitor-exit(r3)     // Catch:{ all -> 0x011d }
                return
            L_0x00f0:
                int r0 = r0 + 1
                goto L_0x006f
            L_0x00f4:
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x011d }
                boolean r0 = r0.hasMessages(r10)     // Catch:{ all -> 0x011d }
                if (r0 != 0) goto L_0x011a
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x011d }
                r0.sendEmptyMessage(r10)     // Catch:{ all -> 0x011d }
                r1.mRunning = r2     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x011d }
                java.lang.Integer r2 = java.lang.Integer.valueOf(r2)     // Catch:{ all -> 0x011d }
                android.os.Message r0 = r0.obtainMessage(r13, r2)     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState r1 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x011d }
                com.android.settingslib.applications.ApplicationsState$MainHandler r1 = r1.mMainHandler     // Catch:{ all -> 0x011d }
                r1.sendMessage(r0)     // Catch:{ all -> 0x011d }
            L_0x011a:
                monitor-exit(r3)     // Catch:{ all -> 0x011d }
                goto L_0x03b4
            L_0x011d:
                r0 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x011d }
                throw r0
            L_0x0120:
                boolean r0 = com.android.settingslib.applications.ApplicationsState.hasFlag(r3, r11)
                if (r0 == 0) goto L_0x0199
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r3 = r0.mEntriesMap
                monitor-enter(r3)
                r0 = r2
            L_0x012c:
                com.android.settingslib.applications.ApplicationsState r4 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0196 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r4 = r4.mAppEntries     // Catch:{ all -> 0x0196 }
                int r4 = r4.size()     // Catch:{ all -> 0x0196 }
                if (r2 >= r4) goto L_0x017b
                if (r0 >= r11) goto L_0x017b
                com.android.settingslib.applications.ApplicationsState r4 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0196 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r4 = r4.mAppEntries     // Catch:{ all -> 0x0196 }
                java.lang.Object r4 = r4.get(r2)     // Catch:{ all -> 0x0196 }
                com.android.settingslib.applications.ApplicationsState$AppEntry r4 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r4     // Catch:{ all -> 0x0196 }
                android.graphics.drawable.Drawable r5 = r4.icon     // Catch:{ all -> 0x0196 }
                if (r5 == 0) goto L_0x014a
                boolean r5 = r4.mounted     // Catch:{ all -> 0x0196 }
                if (r5 != 0) goto L_0x0175
            L_0x014a:
                monitor-enter(r4)     // Catch:{ all -> 0x0196 }
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0178 }
                android.content.Context r5 = r5.mContext     // Catch:{ all -> 0x0178 }
                com.android.settingslib.applications.ApplicationsState r6 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0178 }
                android.util.IconDrawableFactory r6 = r6.mDrawableFactory     // Catch:{ all -> 0x0178 }
                boolean r5 = r4.ensureIconLocked(r5, r6)     // Catch:{ all -> 0x0178 }
                if (r5 == 0) goto L_0x0174
                boolean r5 = r1.mRunning     // Catch:{ all -> 0x0178 }
                if (r5 != 0) goto L_0x0172
                r1.mRunning = r14     // Catch:{ all -> 0x0178 }
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0178 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r5 = r5.mMainHandler     // Catch:{ all -> 0x0178 }
                java.lang.Integer r6 = java.lang.Integer.valueOf(r14)     // Catch:{ all -> 0x0178 }
                android.os.Message r5 = r5.obtainMessage(r13, r6)     // Catch:{ all -> 0x0178 }
                com.android.settingslib.applications.ApplicationsState r6 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0178 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r6 = r6.mMainHandler     // Catch:{ all -> 0x0178 }
                r6.sendMessage(r5)     // Catch:{ all -> 0x0178 }
            L_0x0172:
                int r0 = r0 + 1
            L_0x0174:
                monitor-exit(r4)     // Catch:{ all -> 0x0178 }
            L_0x0175:
                int r2 = r2 + 1
                goto L_0x012c
            L_0x0178:
                r0 = move-exception
                monitor-exit(r4)     // Catch:{ all -> 0x0178 }
                throw r0     // Catch:{ all -> 0x0196 }
            L_0x017b:
                monitor-exit(r3)     // Catch:{ all -> 0x0196 }
                if (r0 <= 0) goto L_0x018f
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r2 = r2.mMainHandler
                boolean r2 = r2.hasMessages(r7)
                if (r2 != 0) goto L_0x018f
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r2 = r2.mMainHandler
                r2.sendEmptyMessage(r7)
            L_0x018f:
                if (r0 < r11) goto L_0x0199
                r1.sendEmptyMessage(r13)
                goto L_0x03b4
            L_0x0196:
                r0 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x0196 }
                throw r0
            L_0x0199:
                r1.sendEmptyMessage(r8)
                goto L_0x03b4
            L_0x019e:
                if (r5 != r12) goto L_0x01a6
                boolean r5 = com.android.settingslib.applications.ApplicationsState.hasFlag(r3, r9)
                if (r5 != 0) goto L_0x01b2
            L_0x01a6:
                int r5 = r0.what
                if (r5 != r10) goto L_0x025d
                r5 = 16
                boolean r3 = com.android.settingslib.applications.ApplicationsState.hasFlag(r3, r5)
                if (r3 == 0) goto L_0x025d
            L_0x01b2:
                android.content.Intent r3 = new android.content.Intent
                java.lang.String r5 = "android.intent.action.MAIN"
                r3.<init>(r5, r4)
                int r4 = r0.what
                if (r4 != r12) goto L_0x01c0
                java.lang.String r4 = "android.intent.category.LAUNCHER"
                goto L_0x01c2
            L_0x01c0:
                java.lang.String r4 = "android.intent.category.LEANBACK_LAUNCHER"
            L_0x01c2:
                r3.addCategory(r4)
                r4 = r2
            L_0x01c6:
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r5 = r5.mEntriesMap
                int r5 = r5.size()
                if (r4 >= r5) goto L_0x024c
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r5 = r5.mEntriesMap
                int r5 = r5.keyAt(r4)
                com.android.settingslib.applications.ApplicationsState r6 = com.android.settingslib.applications.ApplicationsState.this
                android.content.pm.PackageManager r6 = r6.mPm
                r7 = 786944(0xc0200, float:1.102743E-39)
                java.util.List r6 = r6.queryIntentActivitiesAsUser(r3, r7, r5)
                com.android.settingslib.applications.ApplicationsState r7 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r7 = r7.mEntriesMap
                monitor-enter(r7)
                com.android.settingslib.applications.ApplicationsState r9 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x0249 }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r9 = r9.mEntriesMap     // Catch:{ all -> 0x0249 }
                java.lang.Object r9 = r9.valueAt(r4)     // Catch:{ all -> 0x0249 }
                java.util.HashMap r9 = (java.util.HashMap) r9     // Catch:{ all -> 0x0249 }
                int r11 = r6.size()     // Catch:{ all -> 0x0249 }
                r15 = r2
            L_0x01f7:
                if (r15 >= r11) goto L_0x0240
                java.lang.Object r16 = r6.get(r15)     // Catch:{ all -> 0x0249 }
                r2 = r16
                android.content.pm.ResolveInfo r2 = (android.content.pm.ResolveInfo) r2     // Catch:{ all -> 0x0249 }
                android.content.pm.ActivityInfo r13 = r2.activityInfo     // Catch:{ all -> 0x0249 }
                java.lang.String r13 = r13.packageName     // Catch:{ all -> 0x0249 }
                java.lang.Object r17 = r9.get(r13)     // Catch:{ all -> 0x0249 }
                r10 = r17
                com.android.settingslib.applications.ApplicationsState$AppEntry r10 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r10     // Catch:{ all -> 0x0249 }
                if (r10 == 0) goto L_0x021b
                r10.hasLauncherEntry = r14     // Catch:{ all -> 0x0249 }
                boolean r13 = r10.launcherEntryEnabled     // Catch:{ all -> 0x0249 }
                android.content.pm.ActivityInfo r2 = r2.activityInfo     // Catch:{ all -> 0x0249 }
                boolean r2 = r2.enabled     // Catch:{ all -> 0x0249 }
                r2 = r2 | r13
                r10.launcherEntryEnabled = r2     // Catch:{ all -> 0x0249 }
                goto L_0x0239
            L_0x021b:
                java.lang.String r2 = "ApplicationsState"
                java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ all -> 0x0249 }
                r10.<init>()     // Catch:{ all -> 0x0249 }
                java.lang.String r14 = "Cannot find pkg: "
                r10.append(r14)     // Catch:{ all -> 0x0249 }
                r10.append(r13)     // Catch:{ all -> 0x0249 }
                java.lang.String r13 = " on user "
                r10.append(r13)     // Catch:{ all -> 0x0249 }
                r10.append(r5)     // Catch:{ all -> 0x0249 }
                java.lang.String r10 = r10.toString()     // Catch:{ all -> 0x0249 }
                android.util.Log.w(r2, r10)     // Catch:{ all -> 0x0249 }
            L_0x0239:
                int r15 = r15 + 1
                r2 = 0
                r10 = 5
                r13 = 6
                r14 = 1
                goto L_0x01f7
            L_0x0240:
                monitor-exit(r7)     // Catch:{ all -> 0x0249 }
                int r4 = r4 + 1
                r2 = 0
                r10 = 5
                r13 = 6
                r14 = 1
                goto L_0x01c6
            L_0x0249:
                r0 = move-exception
                monitor-exit(r7)     // Catch:{ all -> 0x0249 }
                throw r0
            L_0x024c:
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r2 = r2.mMainHandler
                boolean r2 = r2.hasMessages(r8)
                if (r2 != 0) goto L_0x025d
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r2 = r2.mMainHandler
                r2.sendEmptyMessage(r8)
            L_0x025d:
                int r0 = r0.what
                if (r0 != r12) goto L_0x0267
                r0 = 5
                r1.sendEmptyMessage(r0)
                goto L_0x03b4
            L_0x0267:
                r2 = 6
                r1.sendEmptyMessage(r2)
                goto L_0x03b4
            L_0x026d:
                r2 = r14
                boolean r0 = com.android.settingslib.applications.ApplicationsState.hasFlag(r3, r2)
                if (r0 == 0) goto L_0x02c2
                java.util.ArrayList r0 = new java.util.ArrayList
                r0.<init>()
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                android.content.pm.PackageManager r2 = r2.mPm
                r2.getHomeActivities(r0)
                com.android.settingslib.applications.ApplicationsState r2 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r2 = r2.mEntriesMap
                monitor-enter(r2)
                com.android.settingslib.applications.ApplicationsState r3 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x02bf }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r3 = r3.mEntriesMap     // Catch:{ all -> 0x02bf }
                int r3 = r3.size()     // Catch:{ all -> 0x02bf }
                r4 = 0
            L_0x028e:
                if (r4 >= r3) goto L_0x02bd
                com.android.settingslib.applications.ApplicationsState r5 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x02bf }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r5 = r5.mEntriesMap     // Catch:{ all -> 0x02bf }
                java.lang.Object r5 = r5.valueAt(r4)     // Catch:{ all -> 0x02bf }
                java.util.HashMap r5 = (java.util.HashMap) r5     // Catch:{ all -> 0x02bf }
                java.util.Iterator r6 = r0.iterator()     // Catch:{ all -> 0x02bf }
            L_0x029e:
                boolean r7 = r6.hasNext()     // Catch:{ all -> 0x02bf }
                if (r7 == 0) goto L_0x02ba
                java.lang.Object r7 = r6.next()     // Catch:{ all -> 0x02bf }
                android.content.pm.ResolveInfo r7 = (android.content.pm.ResolveInfo) r7     // Catch:{ all -> 0x02bf }
                android.content.pm.ActivityInfo r7 = r7.activityInfo     // Catch:{ all -> 0x02bf }
                java.lang.String r7 = r7.packageName     // Catch:{ all -> 0x02bf }
                java.lang.Object r7 = r5.get(r7)     // Catch:{ all -> 0x02bf }
                com.android.settingslib.applications.ApplicationsState$AppEntry r7 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r7     // Catch:{ all -> 0x02bf }
                if (r7 == 0) goto L_0x029e
                r8 = 1
                r7.isHomeApp = r8     // Catch:{ all -> 0x02bf }
                goto L_0x029e
            L_0x02ba:
                int r4 = r4 + 1
                goto L_0x028e
            L_0x02bd:
                monitor-exit(r2)     // Catch:{ all -> 0x02bf }
                goto L_0x02c2
            L_0x02bf:
                r0 = move-exception
                monitor-exit(r2)     // Catch:{ all -> 0x02bf }
                throw r0
            L_0x02c2:
                r1.sendEmptyMessage(r12)
                goto L_0x03b4
            L_0x02c7:
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r2 = r0.mEntriesMap
                monitor-enter(r2)
                r3 = 0
                r4 = 0
            L_0x02ce:
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                java.util.List<android.content.pm.ApplicationInfo> r0 = r0.mApplications     // Catch:{ all -> 0x03b1 }
                int r0 = r0.size()     // Catch:{ all -> 0x03b1 }
                if (r3 >= r0) goto L_0x0394
                r5 = 6
                if (r4 >= r5) goto L_0x0394
                boolean r0 = r1.mRunning     // Catch:{ all -> 0x03b1 }
                if (r0 != 0) goto L_0x02f7
                r5 = 1
                r1.mRunning = r5     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler     // Catch:{ all -> 0x03b1 }
                java.lang.Integer r8 = java.lang.Integer.valueOf(r5)     // Catch:{ all -> 0x03b1 }
                r10 = 6
                android.os.Message r0 = r0.obtainMessage(r10, r8)     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState r8 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r8 = r8.mMainHandler     // Catch:{ all -> 0x03b1 }
                r8.sendMessage(r0)     // Catch:{ all -> 0x03b1 }
                goto L_0x02f8
            L_0x02f7:
                r5 = 1
            L_0x02f8:
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                java.util.List<android.content.pm.ApplicationInfo> r0 = r0.mApplications     // Catch:{ all -> 0x03b1 }
                java.lang.Object r0 = r0.get(r3)     // Catch:{ all -> 0x03b1 }
                r8 = r0
                android.content.pm.ApplicationInfo r8 = (android.content.pm.ApplicationInfo) r8     // Catch:{ all -> 0x03b1 }
                int r0 = r8.uid     // Catch:{ all -> 0x03b1 }
                int r10 = android.os.UserHandle.getUserId(r0)     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r0 = r0.mEntriesMap     // Catch:{ all -> 0x03b1 }
                java.lang.Object r0 = r0.get(r10)     // Catch:{ all -> 0x03b1 }
                java.util.HashMap r0 = (java.util.HashMap) r0     // Catch:{ all -> 0x03b1 }
                java.lang.String r12 = r8.packageName     // Catch:{ all -> 0x03b1 }
                java.lang.Object r0 = r0.get(r12)     // Catch:{ all -> 0x03b1 }
                if (r0 != 0) goto L_0x034a
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ NameNotFoundException -> 0x0346 }
                android.content.Context r0 = r0.mContext     // Catch:{ NameNotFoundException -> 0x0346 }
                android.content.pm.PackageManager r0 = r0.getPackageManager()     // Catch:{ NameNotFoundException -> 0x0346 }
                java.lang.String r12 = r8.packageName     // Catch:{ NameNotFoundException -> 0x0346 }
                r13 = 0
                android.content.pm.PackageInfo r0 = r0.getPackageInfo(r12, r13)     // Catch:{ NameNotFoundException -> 0x0346 }
                java.lang.String r12 = r0.overlayCategory     // Catch:{ NameNotFoundException -> 0x0346 }
                boolean r12 = android.text.TextUtils.isEmpty(r12)     // Catch:{ NameNotFoundException -> 0x0346 }
                if (r12 != 0) goto L_0x033e
                java.util.ArrayList r12 = com.android.settingslib.applications.ApplicationsState.THEME_OVERLAY_CATEGORYS     // Catch:{ NameNotFoundException -> 0x0346 }
                java.lang.String r0 = r0.overlayCategory     // Catch:{ NameNotFoundException -> 0x0346 }
                boolean r0 = r12.contains(r0)     // Catch:{ NameNotFoundException -> 0x0346 }
                if (r0 != 0) goto L_0x034a
            L_0x033e:
                int r4 = r4 + 1
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ NameNotFoundException -> 0x0346 }
                r0.getEntryLocked(r8)     // Catch:{ NameNotFoundException -> 0x0346 }
                goto L_0x034a
            L_0x0346:
                r0 = move-exception
                r0.printStackTrace()     // Catch:{ all -> 0x03b1 }
            L_0x034a:
                if (r10 == 0) goto L_0x038f
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r0 = r0.mEntriesMap     // Catch:{ all -> 0x03b1 }
                r10 = 0
                int r0 = r0.indexOfKey(r10)     // Catch:{ all -> 0x03b1 }
                if (r0 < 0) goto L_0x038d
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r0 = r0.mEntriesMap     // Catch:{ all -> 0x03b1 }
                java.lang.Object r0 = r0.get(r10)     // Catch:{ all -> 0x03b1 }
                java.util.HashMap r0 = (java.util.HashMap) r0     // Catch:{ all -> 0x03b1 }
                java.lang.String r10 = r8.packageName     // Catch:{ all -> 0x03b1 }
                java.lang.Object r0 = r0.get(r10)     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState$AppEntry r0 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r0     // Catch:{ all -> 0x03b1 }
                if (r0 == 0) goto L_0x038f
                android.content.pm.ApplicationInfo r10 = r0.info     // Catch:{ all -> 0x03b1 }
                int r10 = r10.flags     // Catch:{ all -> 0x03b1 }
                boolean r10 = com.android.settingslib.applications.ApplicationsState.hasFlag(r10, r6)     // Catch:{ all -> 0x03b1 }
                if (r10 != 0) goto L_0x038f
                com.android.settingslib.applications.ApplicationsState r10 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r10 = r10.mEntriesMap     // Catch:{ all -> 0x03b1 }
                r12 = 0
                java.lang.Object r10 = r10.get(r12)     // Catch:{ all -> 0x03b1 }
                java.util.HashMap r10 = (java.util.HashMap) r10     // Catch:{ all -> 0x03b1 }
                java.lang.String r8 = r8.packageName     // Catch:{ all -> 0x03b1 }
                r10.remove(r8)     // Catch:{ all -> 0x03b1 }
                com.android.settingslib.applications.ApplicationsState r8 = com.android.settingslib.applications.ApplicationsState.this     // Catch:{ all -> 0x03b1 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r8 = r8.mAppEntries     // Catch:{ all -> 0x03b1 }
                r8.remove(r0)     // Catch:{ all -> 0x03b1 }
                goto L_0x0390
            L_0x038d:
                r12 = r10
                goto L_0x0390
            L_0x038f:
                r12 = 0
            L_0x0390:
                int r3 = r3 + 1
                goto L_0x02ce
            L_0x0394:
                monitor-exit(r2)     // Catch:{ all -> 0x03b1 }
                r2 = 6
                if (r4 < r2) goto L_0x039c
                r1.sendEmptyMessage(r11)
                goto L_0x03b4
            L_0x039c:
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler
                boolean r0 = r0.hasMessages(r9)
                if (r0 != 0) goto L_0x03ad
                com.android.settingslib.applications.ApplicationsState r0 = com.android.settingslib.applications.ApplicationsState.this
                com.android.settingslib.applications.ApplicationsState$MainHandler r0 = r0.mMainHandler
                r0.sendEmptyMessage(r9)
            L_0x03ad:
                r1.sendEmptyMessage(r7)
                goto L_0x03b4
            L_0x03b1:
                r0 = move-exception
                monitor-exit(r2)     // Catch:{ all -> 0x03b1 }
                throw r0
            L_0x03b4:
                return
            L_0x03b5:
                r0 = move-exception
                monitor-exit(r2)     // Catch:{ all -> 0x03b5 }
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.applications.ApplicationsState.BackgroundHandler.handleMessage(android.os.Message):void");
        }

        public /* synthetic */ void lambda$handleMessage$0$ApplicationsState$BackgroundHandler() {
            try {
                StorageStats queryStatsForPackage = ApplicationsState.this.mStats.queryStatsForPackage(ApplicationsState.this.mCurComputingSizeUuid, ApplicationsState.this.mCurComputingSizePkg, UserHandle.of(ApplicationsState.this.mCurComputingSizeUserId));
                PackageStats packageStats = new PackageStats(ApplicationsState.this.mCurComputingSizePkg, ApplicationsState.this.mCurComputingSizeUserId);
                packageStats.codeSize = queryStatsForPackage.getCodeBytes();
                packageStats.dataSize = queryStatsForPackage.getDataBytes();
                packageStats.cacheSize = queryStatsForPackage.getCacheBytes();
                this.mStatsObserver.onGetStatsCompleted(packageStats, true);
            } catch (NameNotFoundException | IOException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to query stats: ");
                sb.append(e);
                Log.w("ApplicationsState", sb.toString());
                try {
                    this.mStatsObserver.onGetStatsCompleted(null, false);
                } catch (RemoteException unused) {
                }
            }
        }

        private int getCombinedSessionFlags(List<Session> list) {
            int i;
            synchronized (ApplicationsState.this.mEntriesMap) {
                i = 0;
                for (Session access$400 : list) {
                    i |= access$400.mFlags;
                }
            }
            return i;
        }
    }

    public interface Callbacks {
        void onAllSizesComputed();

        void onLauncherInfoChanged();

        void onLoadEntriesCompleted();

        void onPackageIconChanged();

        void onPackageListChanged();

        void onPackageSizeChanged(String str);

        void onRebuildComplete(ArrayList<AppEntry> arrayList);

        void onRunningStateChanged(boolean z);
    }

    class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            ApplicationsState.this.rebuildActiveSessions();
            switch (message.what) {
                case 1:
                    Session session = (Session) message.obj;
                    Iterator it = ApplicationsState.this.mActiveSessions.iterator();
                    while (it.hasNext()) {
                        Session session2 = (Session) ((WeakReference) it.next()).get();
                        if (session2 != null && session2 == session) {
                            session.mCallbacks.onRebuildComplete(session.mLastAppList);
                        }
                    }
                    return;
                case 2:
                    Iterator it2 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it2.hasNext()) {
                        Session session3 = (Session) ((WeakReference) it2.next()).get();
                        if (session3 != null) {
                            session3.mCallbacks.onPackageListChanged();
                        }
                    }
                    return;
                case 3:
                    Iterator it3 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it3.hasNext()) {
                        Session session4 = (Session) ((WeakReference) it3.next()).get();
                        if (session4 != null) {
                            session4.mCallbacks.onPackageIconChanged();
                        }
                    }
                    return;
                case 4:
                    Iterator it4 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it4.hasNext()) {
                        Session session5 = (Session) ((WeakReference) it4.next()).get();
                        if (session5 != null) {
                            session5.mCallbacks.onPackageSizeChanged((String) message.obj);
                        }
                    }
                    return;
                case 5:
                    Iterator it5 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it5.hasNext()) {
                        Session session6 = (Session) ((WeakReference) it5.next()).get();
                        if (session6 != null) {
                            session6.mCallbacks.onAllSizesComputed();
                        }
                    }
                    return;
                case 6:
                    Iterator it6 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it6.hasNext()) {
                        Session session7 = (Session) ((WeakReference) it6.next()).get();
                        if (session7 != null) {
                            session7.mCallbacks.onRunningStateChanged(message.arg1 != 0);
                        }
                    }
                    return;
                case 7:
                    Iterator it7 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it7.hasNext()) {
                        Session session8 = (Session) ((WeakReference) it7.next()).get();
                        if (session8 != null) {
                            session8.mCallbacks.onLauncherInfoChanged();
                        }
                    }
                    return;
                case 8:
                    Iterator it8 = ApplicationsState.this.mActiveSessions.iterator();
                    while (it8.hasNext()) {
                        Session session9 = (Session) ((WeakReference) it8.next()).get();
                        if (session9 != null) {
                            session9.mCallbacks.onLoadEntriesCompleted();
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private class PackageIntentReceiver extends BroadcastReceiver {
        private PackageIntentReceiver() {
        }

        /* access modifiers changed from: 0000 */
        public void registerReceiver() {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addDataScheme("package");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter);
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter2);
            IntentFilter intentFilter3 = new IntentFilter();
            intentFilter3.addAction("android.intent.action.USER_ADDED");
            intentFilter3.addAction("android.intent.action.USER_REMOVED");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter3);
        }

        /* access modifiers changed from: 0000 */
        public void unregisterReceiver() {
            ApplicationsState.this.mContext.unregisterReceiver(this);
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int i = 0;
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                String encodedSchemeSpecificPart = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState applicationsState = ApplicationsState.this;
                    applicationsState.addPackage(encodedSchemeSpecificPart, applicationsState.mEntriesMap.keyAt(i));
                    i++;
                }
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                String encodedSchemeSpecificPart2 = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState applicationsState2 = ApplicationsState.this;
                    applicationsState2.removePackage(encodedSchemeSpecificPart2, applicationsState2.mEntriesMap.keyAt(i));
                    i++;
                }
            } else if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                String encodedSchemeSpecificPart3 = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState applicationsState3 = ApplicationsState.this;
                    applicationsState3.invalidatePackage(encodedSchemeSpecificPart3, applicationsState3.mEntriesMap.keyAt(i));
                    i++;
                }
            } else {
                String str = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
                if (str.equals(action) || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                    if (stringArrayExtra != null && stringArrayExtra.length != 0 && str.equals(action)) {
                        for (String str2 : stringArrayExtra) {
                            for (int i2 = 0; i2 < ApplicationsState.this.mEntriesMap.size(); i2++) {
                                ApplicationsState applicationsState4 = ApplicationsState.this;
                                applicationsState4.invalidatePackage(str2, applicationsState4.mEntriesMap.keyAt(i2));
                            }
                        }
                        return;
                    }
                    return;
                }
                String str3 = "android.intent.extra.user_handle";
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    ApplicationsState.this.addUser(intent.getIntExtra(str3, -10000));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    ApplicationsState.this.removeUser(intent.getIntExtra(str3, -10000));
                }
            }
        }
    }

    public class Session implements LifecycleObserver {
        final Callbacks mCallbacks;
        /* access modifiers changed from: private */
        public int mFlags;
        private final boolean mHasLifecycle;
        ArrayList<AppEntry> mLastAppList;
        boolean mRebuildAsync;
        Comparator<AppEntry> mRebuildComparator;
        AppFilter mRebuildFilter;
        boolean mRebuildForeground;
        boolean mRebuildRequested;
        ArrayList<AppEntry> mRebuildResult;
        final Object mRebuildSync;
        boolean mResumed;
        final /* synthetic */ ApplicationsState this$0;

        @OnLifecycleEvent(Event.ON_RESUME)
        public void onResume() {
            synchronized (this.this$0.mEntriesMap) {
                if (!this.mResumed) {
                    this.mResumed = true;
                    this.this$0.mSessionsChanged = true;
                    this.this$0.doPauseLocked();
                    this.this$0.doResumeIfNeededLocked();
                }
            }
        }

        @OnLifecycleEvent(Event.ON_PAUSE)
        public void onPause() {
            synchronized (this.this$0.mEntriesMap) {
                if (this.mResumed) {
                    this.mResumed = false;
                    this.this$0.mSessionsChanged = true;
                    this.this$0.mBackgroundHandler.removeMessages(1, this);
                    this.this$0.doPauseIfNeededLocked();
                }
            }
        }

        /* access modifiers changed from: 0000 */
        /* JADX WARNING: Code restructure failed: missing block: B:11:0x0020, code lost:
            if (r1 == null) goto L_0x0029;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:12:0x0022, code lost:
            r1.init(r7.this$0.mContext);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:13:0x0029, code lost:
            r3 = r7.this$0.mEntriesMap;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:14:0x002d, code lost:
            monitor-enter(r3);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:16:?, code lost:
            r0 = new java.util.ArrayList(r7.this$0.mAppEntries);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:17:0x0037, code lost:
            monitor-exit(r3);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:18:0x0038, code lost:
            r3 = new java.util.ArrayList<>();
            r0 = r0.iterator();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:20:0x0045, code lost:
            if (r0.hasNext() == false) goto L_0x006d;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:21:0x0047, code lost:
            r4 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r0.next();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:22:0x004d, code lost:
            if (r4 == null) goto L_0x0041;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:23:0x004f, code lost:
            if (r1 == null) goto L_0x0057;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x0055, code lost:
            if (r1.filterApp(r4) == false) goto L_0x0041;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:26:0x0057, code lost:
            r5 = r7.this$0.mEntriesMap;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:27:0x005b, code lost:
            monitor-enter(r5);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x005c, code lost:
            if (r2 == null) goto L_0x0065;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:?, code lost:
            r4.ensureLabel(r7.this$0.mContext);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x0065, code lost:
            r3.add(r4);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x0068, code lost:
            monitor-exit(r5);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:37:0x006d, code lost:
            if (r2 == null) goto L_0x007c;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:38:0x006f, code lost:
            r0 = r7.this$0.mEntriesMap;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:39:0x0073, code lost:
            monitor-enter(r0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:41:?, code lost:
            java.util.Collections.sort(r3, r2);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:42:0x0077, code lost:
            monitor-exit(r0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:47:0x007c, code lost:
            r0 = r7.mRebuildSync;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:48:0x007e, code lost:
            monitor-enter(r0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:51:0x0081, code lost:
            if (r7.mRebuildRequested != false) goto L_0x00ab;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:52:0x0083, code lost:
            r7.mLastAppList = r3;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:53:0x0087, code lost:
            if (r7.mRebuildAsync != false) goto L_0x0091;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:54:0x0089, code lost:
            r7.mRebuildResult = r3;
            r7.mRebuildSync.notifyAll();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:56:0x009a, code lost:
            if (r7.this$0.mMainHandler.hasMessages(1, r7) != false) goto L_0x00ab;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:57:0x009c, code lost:
            r7.this$0.mMainHandler.sendMessage(r7.this$0.mMainHandler.obtainMessage(1, r7));
         */
        /* JADX WARNING: Code restructure failed: missing block: B:58:0x00ab, code lost:
            monitor-exit(r0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:59:0x00ac, code lost:
            android.os.Process.setThreadPriority(10);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:60:0x00b1, code lost:
            return;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleRebuildList() {
            /*
                r7 = this;
                java.lang.Object r0 = r7.mRebuildSync
                monitor-enter(r0)
                boolean r1 = r7.mRebuildRequested     // Catch:{ all -> 0x00b8 }
                if (r1 != 0) goto L_0x0009
                monitor-exit(r0)     // Catch:{ all -> 0x00b8 }
                return
            L_0x0009:
                com.android.settingslib.applications.ApplicationsState$AppFilter r1 = r7.mRebuildFilter     // Catch:{ all -> 0x00b8 }
                java.util.Comparator<com.android.settingslib.applications.ApplicationsState$AppEntry> r2 = r7.mRebuildComparator     // Catch:{ all -> 0x00b8 }
                r3 = 0
                r7.mRebuildRequested = r3     // Catch:{ all -> 0x00b8 }
                r4 = 0
                r7.mRebuildFilter = r4     // Catch:{ all -> 0x00b8 }
                r7.mRebuildComparator = r4     // Catch:{ all -> 0x00b8 }
                boolean r4 = r7.mRebuildForeground     // Catch:{ all -> 0x00b8 }
                if (r4 == 0) goto L_0x001f
                r4 = -2
                android.os.Process.setThreadPriority(r4)     // Catch:{ all -> 0x00b8 }
                r7.mRebuildForeground = r3     // Catch:{ all -> 0x00b8 }
            L_0x001f:
                monitor-exit(r0)     // Catch:{ all -> 0x00b8 }
                if (r1 == 0) goto L_0x0029
                com.android.settingslib.applications.ApplicationsState r0 = r7.this$0
                android.content.Context r0 = r0.mContext
                r1.init(r0)
            L_0x0029:
                com.android.settingslib.applications.ApplicationsState r0 = r7.this$0
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r3 = r0.mEntriesMap
                monitor-enter(r3)
                java.util.ArrayList r0 = new java.util.ArrayList     // Catch:{ all -> 0x00b5 }
                com.android.settingslib.applications.ApplicationsState r4 = r7.this$0     // Catch:{ all -> 0x00b5 }
                java.util.ArrayList<com.android.settingslib.applications.ApplicationsState$AppEntry> r4 = r4.mAppEntries     // Catch:{ all -> 0x00b5 }
                r0.<init>(r4)     // Catch:{ all -> 0x00b5 }
                monitor-exit(r3)     // Catch:{ all -> 0x00b5 }
                java.util.ArrayList r3 = new java.util.ArrayList
                r3.<init>()
                java.util.Iterator r0 = r0.iterator()
            L_0x0041:
                boolean r4 = r0.hasNext()
                if (r4 == 0) goto L_0x006d
                java.lang.Object r4 = r0.next()
                com.android.settingslib.applications.ApplicationsState$AppEntry r4 = (com.android.settingslib.applications.ApplicationsState.AppEntry) r4
                if (r4 == 0) goto L_0x0041
                if (r1 == 0) goto L_0x0057
                boolean r5 = r1.filterApp(r4)
                if (r5 == 0) goto L_0x0041
            L_0x0057:
                com.android.settingslib.applications.ApplicationsState r5 = r7.this$0
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r5 = r5.mEntriesMap
                monitor-enter(r5)
                if (r2 == 0) goto L_0x0065
                com.android.settingslib.applications.ApplicationsState r6 = r7.this$0     // Catch:{ all -> 0x006a }
                android.content.Context r6 = r6.mContext     // Catch:{ all -> 0x006a }
                r4.ensureLabel(r6)     // Catch:{ all -> 0x006a }
            L_0x0065:
                r3.add(r4)     // Catch:{ all -> 0x006a }
                monitor-exit(r5)     // Catch:{ all -> 0x006a }
                goto L_0x0041
            L_0x006a:
                r7 = move-exception
                monitor-exit(r5)     // Catch:{ all -> 0x006a }
                throw r7
            L_0x006d:
                if (r2 == 0) goto L_0x007c
                com.android.settingslib.applications.ApplicationsState r0 = r7.this$0
                android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r0 = r0.mEntriesMap
                monitor-enter(r0)
                java.util.Collections.sort(r3, r2)     // Catch:{ all -> 0x0079 }
                monitor-exit(r0)     // Catch:{ all -> 0x0079 }
                goto L_0x007c
            L_0x0079:
                r7 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x0079 }
                throw r7
            L_0x007c:
                java.lang.Object r0 = r7.mRebuildSync
                monitor-enter(r0)
                boolean r1 = r7.mRebuildRequested     // Catch:{ all -> 0x00b2 }
                if (r1 != 0) goto L_0x00ab
                r7.mLastAppList = r3     // Catch:{ all -> 0x00b2 }
                boolean r1 = r7.mRebuildAsync     // Catch:{ all -> 0x00b2 }
                if (r1 != 0) goto L_0x0091
                r7.mRebuildResult = r3     // Catch:{ all -> 0x00b2 }
                java.lang.Object r7 = r7.mRebuildSync     // Catch:{ all -> 0x00b2 }
                r7.notifyAll()     // Catch:{ all -> 0x00b2 }
                goto L_0x00ab
            L_0x0091:
                com.android.settingslib.applications.ApplicationsState r1 = r7.this$0     // Catch:{ all -> 0x00b2 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r1 = r1.mMainHandler     // Catch:{ all -> 0x00b2 }
                r2 = 1
                boolean r1 = r1.hasMessages(r2, r7)     // Catch:{ all -> 0x00b2 }
                if (r1 != 0) goto L_0x00ab
                com.android.settingslib.applications.ApplicationsState r1 = r7.this$0     // Catch:{ all -> 0x00b2 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r1 = r1.mMainHandler     // Catch:{ all -> 0x00b2 }
                android.os.Message r1 = r1.obtainMessage(r2, r7)     // Catch:{ all -> 0x00b2 }
                com.android.settingslib.applications.ApplicationsState r7 = r7.this$0     // Catch:{ all -> 0x00b2 }
                com.android.settingslib.applications.ApplicationsState$MainHandler r7 = r7.mMainHandler     // Catch:{ all -> 0x00b2 }
                r7.sendMessage(r1)     // Catch:{ all -> 0x00b2 }
            L_0x00ab:
                monitor-exit(r0)     // Catch:{ all -> 0x00b2 }
                r7 = 10
                android.os.Process.setThreadPriority(r7)
                return
            L_0x00b2:
                r7 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x00b2 }
                throw r7
            L_0x00b5:
                r7 = move-exception
                monitor-exit(r3)     // Catch:{ all -> 0x00b5 }
                throw r7
            L_0x00b8:
                r7 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x00b8 }
                throw r7
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.applications.ApplicationsState.Session.handleRebuildList():void");
        }

        @OnLifecycleEvent(Event.ON_DESTROY)
        public void onDestroy() {
            if (!this.mHasLifecycle) {
                onPause();
            }
            synchronized (this.this$0.mEntriesMap) {
                this.this$0.mSessions.remove(this);
            }
        }
    }

    public static class SizeInfo {
        public long cacheSize;
        public long codeSize;
        public long dataSize;
        public long externalCacheSize;
        public long externalCodeSize;
        public long externalDataSize;
    }

    /* access modifiers changed from: private */
    public static boolean hasFlag(int i, int i2) {
        return (i & i2) != 0;
    }

    static ApplicationsState getInstance(Application application, IPackageManager iPackageManager) {
        ApplicationsState applicationsState;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ApplicationsState(application, iPackageManager);
            }
            applicationsState = sInstance;
        }
        return applicationsState;
    }

    /* access modifiers changed from: 0000 */
    public void setInterestingConfigChanges(InterestingConfigChanges interestingConfigChanges) {
        this.mInterestingConfigChanges = interestingConfigChanges;
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [] */
    /* JADX WARNING: Missing exception handler attribute for start block: B:20:0x0121 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ApplicationsState(android.app.Application r8, android.content.pm.IPackageManager r9) {
        /*
            r7 = this;
            r7.<init>()
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r7.mSessions = r0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r7.mRebuildingSessions = r0
            com.android.settingslib.applications.InterestingConfigChanges r0 = new com.android.settingslib.applications.InterestingConfigChanges
            r0.<init>()
            r7.mInterestingConfigChanges = r0
            android.util.SparseArray r0 = new android.util.SparseArray
            r0.<init>()
            r7.mEntriesMap = r0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r7.mAppEntries = r0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r7.mApplications = r0
            r0 = 1
            r7.mCurId = r0
            java.util.HashMap r2 = new java.util.HashMap
            r2.<init>()
            r7.mSystemModules = r2
            java.util.ArrayList r2 = new java.util.ArrayList
            r2.<init>()
            r7.mActiveSessions = r2
            com.android.settingslib.applications.ApplicationsState$MainHandler r2 = new com.android.settingslib.applications.ApplicationsState$MainHandler
            android.os.Looper r3 = android.os.Looper.getMainLooper()
            r2.<init>(r3)
            r7.mMainHandler = r2
            java.util.HashMap r2 = new java.util.HashMap
            r2.<init>()
            r7.mSystemPersistApplications = r2
            r7.mContext = r8
            android.content.Context r8 = r7.mContext
            android.content.pm.PackageManager r8 = r8.getPackageManager()
            r7.mPm = r8
            android.content.Context r8 = r7.mContext
            android.util.IconDrawableFactory r8 = android.util.IconDrawableFactory.newInstance(r8)
            r7.mDrawableFactory = r8
            r7.mIpm = r9
            android.content.Context r8 = r7.mContext
            java.lang.Class<android.os.UserManager> r9 = android.os.UserManager.class
            java.lang.Object r8 = r8.getSystemService(r9)
            android.os.UserManager r8 = (android.os.UserManager) r8
            r7.mUm = r8
            android.content.Context r8 = r7.mContext
            java.lang.Class<android.app.usage.StorageStatsManager> r9 = android.app.usage.StorageStatsManager.class
            java.lang.Object r8 = r8.getSystemService(r9)
            android.app.usage.StorageStatsManager r8 = (android.app.usage.StorageStatsManager) r8
            r7.mStats = r8
            android.os.UserManager r8 = r7.mUm
            int r9 = android.os.UserHandle.myUserId()
            int[] r8 = r8.getProfileIdsWithDisabled(r9)
            int r9 = r8.length
            r2 = 0
            r3 = r2
        L_0x008a:
            if (r3 >= r9) goto L_0x009b
            r4 = r8[r3]
            android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r5 = r7.mEntriesMap
            java.util.HashMap r6 = new java.util.HashMap
            r6.<init>()
            r5.put(r4, r6)
            int r3 = r3 + 1
            goto L_0x008a
        L_0x009b:
            android.os.HandlerThread r8 = new android.os.HandlerThread
            r9 = 10
            java.lang.String r3 = "ApplicationsState.Loader"
            r8.<init>(r3, r9)
            r7.mThread = r8
            android.os.HandlerThread r8 = r7.mThread
            r8.start()
            com.android.settingslib.applications.ApplicationsState$BackgroundHandler r8 = new com.android.settingslib.applications.ApplicationsState$BackgroundHandler
            android.os.HandlerThread r9 = r7.mThread
            android.os.Looper r9 = r9.getLooper()
            r8.<init>(r9)
            r7.mBackgroundHandler = r8
            r8 = 4227584(0x408200, float:5.924107E-39)
            r7.mAdminRetrieveFlags = r8
            r8 = 33280(0x8200, float:4.6635E-41)
            r7.mRetrieveFlags = r8
            android.content.pm.PackageManager r8 = r7.mPm
            java.util.List r8 = r8.getInstalledModules(r2)
            java.util.Iterator r8 = r8.iterator()
        L_0x00cc:
            boolean r9 = r8.hasNext()
            if (r9 == 0) goto L_0x00ea
            java.lang.Object r9 = r8.next()
            android.content.pm.ModuleInfo r9 = (android.content.pm.ModuleInfo) r9
            java.util.HashMap<java.lang.String, java.lang.Boolean> r3 = r7.mSystemModules
            java.lang.String r4 = r9.getPackageName()
            boolean r9 = r9.isHidden()
            java.lang.Boolean r9 = java.lang.Boolean.valueOf(r9)
            r3.put(r4, r9)
            goto L_0x00cc
        L_0x00ea:
            android.content.pm.PackageManager r8 = r7.mPm
            java.util.List r8 = r8.getInstalledPackages(r2)
            java.util.Iterator r8 = r8.iterator()
        L_0x00f4:
            boolean r9 = r8.hasNext()
            if (r9 == 0) goto L_0x0116
            java.lang.Object r9 = r8.next()
            android.content.pm.PackageInfo r9 = (android.content.pm.PackageInfo) r9
            android.content.pm.ApplicationInfo r2 = r9.applicationInfo
            boolean r2 = r7.isSystemApp(r2)
            if (r2 == 0) goto L_0x00f4
            android.content.pm.ApplicationInfo r2 = r9.applicationInfo
            boolean r2 = r7.getSystemPersistMetaData(r2)
            if (r2 == 0) goto L_0x00f4
            android.content.pm.ApplicationInfo r9 = r9.applicationInfo
            r7.addForceStopWhiteList(r9)
            goto L_0x00f4
        L_0x0116:
            android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r8 = r7.mEntriesMap
            monitor-enter(r8)
            android.util.SparseArray<java.util.HashMap<java.lang.String, com.android.settingslib.applications.ApplicationsState$AppEntry>> r7 = r7.mEntriesMap     // Catch:{ InterruptedException -> 0x0121 }
            r7.wait(r0)     // Catch:{ InterruptedException -> 0x0121 }
            goto L_0x0121
        L_0x011f:
            r7 = move-exception
            goto L_0x0123
        L_0x0121:
            monitor-exit(r8)     // Catch:{ all -> 0x011f }
            return
        L_0x0123:
            monitor-exit(r8)     // Catch:{ all -> 0x011f }
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.applications.ApplicationsState.<init>(android.app.Application, android.content.pm.IPackageManager):void");
    }

    private void addForceStopWhiteList(ApplicationInfo applicationInfo) {
        this.mSystemPersistApplications.put(applicationInfo.packageName, Boolean.valueOf(true));
    }

    private boolean getSystemPersistMetaData(ApplicationInfo applicationInfo) {
        try {
            ApplicationInfo applicationInfo2 = this.mPm.getApplicationInfo(applicationInfo.packageName, 128);
            if (!(applicationInfo2 == null || applicationInfo2.metaData == null)) {
                return applicationInfo2.metaData.getBoolean("persist");
            }
        } catch (NameNotFoundException unused) {
        }
        return false;
    }

    private boolean isSystemApp(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & 1) != 0;
    }

    /* access modifiers changed from: 0000 */
    public void doResumeIfNeededLocked() {
        if (!this.mResumed) {
            this.mResumed = true;
            if (this.mPackageIntentReceiver == null) {
                this.mPackageIntentReceiver = new PackageIntentReceiver();
                this.mPackageIntentReceiver.registerReceiver();
            }
            List<ApplicationInfo> list = this.mApplications;
            this.mApplications = new ArrayList();
            for (UserInfo userInfo : this.mUm.getProfiles(UserHandle.myUserId())) {
                try {
                    if (this.mEntriesMap.indexOfKey(userInfo.id) < 0) {
                        this.mEntriesMap.put(userInfo.id, new HashMap());
                    }
                    this.mApplications.addAll(this.mIpm.getInstalledApplications(userInfo.isAdmin() ? this.mAdminRetrieveFlags : this.mRetrieveFlags, userInfo.id).getList());
                } catch (Exception e) {
                    Log.e("ApplicationsState", "Error during doResumeIfNeededLocked", e);
                }
            }
            int i = 0;
            if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
                clearEntries();
            } else {
                for (int i2 = 0; i2 < this.mAppEntries.size(); i2++) {
                    ((AppEntry) this.mAppEntries.get(i2)).sizeStale = true;
                }
            }
            this.mHaveDisabledApps = false;
            this.mHaveInstantApps = false;
            while (i < this.mApplications.size()) {
                ApplicationInfo applicationInfo = (ApplicationInfo) this.mApplications.get(i);
                if (!applicationInfo.enabled) {
                    if (applicationInfo.enabledSetting != 3) {
                        this.mApplications.remove(i);
                        i--;
                        i++;
                    } else {
                        this.mHaveDisabledApps = true;
                    }
                }
                if (isHiddenModule(applicationInfo.packageName)) {
                    int i3 = i - 1;
                    this.mApplications.remove(i);
                    i = i3;
                } else {
                    if (!this.mHaveInstantApps && AppUtils.isInstant(applicationInfo)) {
                        this.mHaveInstantApps = true;
                    }
                    int userId = UserHandle.getUserId(applicationInfo.uid);
                    if (userId != 999 || (applicationInfo.flags & 1) <= 0) {
                        AppEntry appEntry = (AppEntry) ((HashMap) this.mEntriesMap.get(userId)).get(applicationInfo.packageName);
                        if (appEntry != null) {
                            appEntry.info = applicationInfo;
                        }
                    } else {
                        this.mApplications.remove(i);
                        i--;
                    }
                }
                i++;
            }
            if (anyAppIsRemoved(list, this.mApplications)) {
                clearEntries();
            }
            this.mCurComputingSizePkg = null;
            if (!this.mBackgroundHandler.hasMessages(2)) {
                this.mBackgroundHandler.sendEmptyMessage(2);
            }
        }
    }

    private static boolean anyAppIsRemoved(List<ApplicationInfo> list, List<ApplicationInfo> list2) {
        if (list.size() == 0) {
            return false;
        }
        if (list2.size() < list.size()) {
            return true;
        }
        HashMap hashMap = new HashMap();
        for (ApplicationInfo applicationInfo : list2) {
            String valueOf = String.valueOf(UserHandle.getUserId(applicationInfo.uid));
            HashSet hashSet = (HashSet) hashMap.get(valueOf);
            if (hashSet == null) {
                hashSet = new HashSet();
                hashMap.put(valueOf, hashSet);
            }
            if (hasFlag(applicationInfo.flags, 8388608)) {
                hashSet.add(applicationInfo.packageName);
            }
        }
        for (ApplicationInfo applicationInfo2 : list) {
            if (hasFlag(applicationInfo2.flags, 8388608)) {
                HashSet hashSet2 = (HashSet) hashMap.get(String.valueOf(UserHandle.getUserId(applicationInfo2.uid)));
                if (hashSet2 == null || !hashSet2.remove(applicationInfo2.packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: 0000 */
    public void clearEntries() {
        for (int i = 0; i < this.mEntriesMap.size(); i++) {
            ((HashMap) this.mEntriesMap.valueAt(i)).clear();
        }
        this.mAppEntries.clear();
    }

    /* access modifiers changed from: 0000 */
    public boolean isHiddenModule(String str) {
        Boolean bool = (Boolean) this.mSystemModules.get(str);
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    /* access modifiers changed from: 0000 */
    public void doPauseIfNeededLocked() {
        if (this.mResumed) {
            int i = 0;
            while (i < this.mSessions.size()) {
                if (!((Session) this.mSessions.get(i)).mResumed) {
                    i++;
                } else {
                    return;
                }
            }
            doPauseLocked();
        }
    }

    /* access modifiers changed from: 0000 */
    public void doPauseLocked() {
        this.mResumed = false;
        PackageIntentReceiver packageIntentReceiver = this.mPackageIntentReceiver;
        if (packageIntentReceiver != null) {
            packageIntentReceiver.unregisterReceiver();
            this.mPackageIntentReceiver = null;
        }
    }

    /* access modifiers changed from: 0000 */
    public int indexOfApplicationInfoLocked(String str, int i) {
        for (int size = this.mApplications.size() - 1; size >= 0; size--) {
            ApplicationInfo applicationInfo = (ApplicationInfo) this.mApplications.get(size);
            if (applicationInfo.packageName.equals(str) && UserHandle.getUserId(applicationInfo.uid) == i) {
                return size;
            }
        }
        return -1;
    }

    /* access modifiers changed from: 0000 */
    public void addPackage(String str, int i) {
        try {
            synchronized (this.mEntriesMap) {
                if (this.mResumed) {
                    if (indexOfApplicationInfoLocked(str, i) < 0) {
                        ApplicationInfo applicationInfo = this.mIpm.getApplicationInfo(str, this.mUm.isUserAdmin(i) ? this.mAdminRetrieveFlags : this.mRetrieveFlags, i);
                        if (applicationInfo != null) {
                            if (!applicationInfo.enabled) {
                                if (applicationInfo.enabledSetting == 3) {
                                    this.mHaveDisabledApps = true;
                                } else {
                                    return;
                                }
                            }
                            if (AppUtils.isInstant(applicationInfo)) {
                                this.mHaveInstantApps = true;
                            }
                            this.mApplications.add(applicationInfo);
                            if (!this.mBackgroundHandler.hasMessages(2)) {
                                this.mBackgroundHandler.sendEmptyMessage(2);
                            }
                            if (!this.mMainHandler.hasMessages(2)) {
                                this.mMainHandler.sendEmptyMessage(2);
                            }
                        }
                    }
                }
            }
        } catch (RemoteException unused) {
        }
    }

    public void removePackage(String str, int i) {
        synchronized (this.mEntriesMap) {
            int indexOfApplicationInfoLocked = indexOfApplicationInfoLocked(str, i);
            if (indexOfApplicationInfoLocked >= 0) {
                AppEntry appEntry = (AppEntry) ((HashMap) this.mEntriesMap.get(i)).get(str);
                if (appEntry != null) {
                    ((HashMap) this.mEntriesMap.get(i)).remove(str);
                    this.mAppEntries.remove(appEntry);
                }
                ApplicationInfo applicationInfo = (ApplicationInfo) this.mApplications.get(indexOfApplicationInfoLocked);
                this.mApplications.remove(indexOfApplicationInfoLocked);
                if (!applicationInfo.enabled) {
                    this.mHaveDisabledApps = false;
                    Iterator it = this.mApplications.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (!((ApplicationInfo) it.next()).enabled) {
                                this.mHaveDisabledApps = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                if (AppUtils.isInstant(applicationInfo)) {
                    this.mHaveInstantApps = false;
                    Iterator it2 = this.mApplications.iterator();
                    while (true) {
                        if (it2.hasNext()) {
                            if (AppUtils.isInstant((ApplicationInfo) it2.next())) {
                                this.mHaveInstantApps = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    public void invalidatePackage(String str, int i) {
        removePackage(str, i);
        addPackage(str, i);
    }

    /* access modifiers changed from: private */
    public void addUser(int i) {
        if (ArrayUtils.contains(this.mUm.getProfileIdsWithDisabled(UserHandle.myUserId()), i)) {
            synchronized (this.mEntriesMap) {
                this.mEntriesMap.put(i, new HashMap());
                if (this.mResumed) {
                    doPauseLocked();
                    doResumeIfNeededLocked();
                }
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeUser(int i) {
        synchronized (this.mEntriesMap) {
            HashMap hashMap = (HashMap) this.mEntriesMap.get(i);
            if (hashMap != null) {
                for (AppEntry appEntry : hashMap.values()) {
                    this.mAppEntries.remove(appEntry);
                    this.mApplications.remove(appEntry.info);
                }
                this.mEntriesMap.remove(i);
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public AppEntry getEntryLocked(ApplicationInfo applicationInfo) {
        int userId = UserHandle.getUserId(applicationInfo.uid);
        AppEntry appEntry = (AppEntry) ((HashMap) this.mEntriesMap.get(userId)).get(applicationInfo.packageName);
        if (appEntry == null) {
            if (isHiddenModule(applicationInfo.packageName)) {
                return null;
            }
            Context context = this.mContext;
            long j = this.mCurId;
            this.mCurId = 1 + j;
            appEntry = new AppEntry(context, applicationInfo, j);
            ((HashMap) this.mEntriesMap.get(userId)).put(applicationInfo.packageName, appEntry);
            this.mAppEntries.add(appEntry);
        } else if (appEntry.info != applicationInfo) {
            appEntry.info = applicationInfo;
        }
        return appEntry;
    }

    /* access modifiers changed from: private */
    public long getTotalInternalSize(PackageStats packageStats) {
        if (packageStats != null) {
            return (packageStats.codeSize + packageStats.dataSize) - packageStats.cacheSize;
        }
        return -2;
    }

    /* access modifiers changed from: private */
    public long getTotalExternalSize(PackageStats packageStats) {
        if (packageStats != null) {
            return packageStats.externalCodeSize + packageStats.externalDataSize + packageStats.externalCacheSize + packageStats.externalMediaSize + packageStats.externalObbSize;
        }
        return -2;
    }

    /* access modifiers changed from: private */
    public String getSizeStr(long j) {
        if (j >= 0) {
            return Utils.formatFileSize(this.mContext, j);
        }
        return null;
    }

    /* access modifiers changed from: 0000 */
    public void rebuildActiveSessions() {
        synchronized (this.mEntriesMap) {
            if (this.mSessionsChanged) {
                this.mActiveSessions.clear();
                for (int i = 0; i < this.mSessions.size(); i++) {
                    Session session = (Session) this.mSessions.get(i);
                    if (session.mResumed) {
                        this.mActiveSessions.add(new WeakReference(session));
                    }
                }
            }
        }
    }
}
