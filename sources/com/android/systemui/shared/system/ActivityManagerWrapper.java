package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.Application;
import android.app.WallpaperManager;
import android.app.WindowConfiguration.ActivityType;
import android.appwidget.AppWidgetManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import java.util.List;
import java.util.concurrent.Future;

public class ActivityManagerWrapper {
    private static String sCurrentDockedPackageName = null;
    private static String sCurrentInputMethodPackageName = null;
    private static String sCurrentLauncherPackageName = null;
    private static String sCurrentWallpaperPackageName = null;
    private static final ActivityManagerWrapper sInstance = new ActivityManagerWrapper();
    ActivityManager mAm;
    AppWidgetManager mAppWidgetManager;
    private final BackgroundExecutor mBackgroundExecutor = BackgroundExecutor.get();
    InputMethodManager mImm;
    private final PackageManager mPackageManager;
    private final TaskStackChangeListeners mTaskStackChangeListeners = new TaskStackChangeListeners(Looper.getMainLooper());
    WallpaperManager mWallpaperManager;

    private ActivityManagerWrapper() {
        Application initialApplication = AppGlobals.getInitialApplication();
        this.mPackageManager = initialApplication.getPackageManager();
        this.mImm = (InputMethodManager) initialApplication.getSystemService("input_method");
        this.mAppWidgetManager = (AppWidgetManager) initialApplication.getSystemService(AppWidgetManager.class);
        this.mWallpaperManager = WallpaperManager.getInstance(initialApplication);
        this.mAm = (ActivityManager) initialApplication.getSystemService("activity");
    }

    public static ActivityManagerWrapper getInstance() {
        return sInstance;
    }

    public int getCurrentUserId() {
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            if (currentUser != null) {
                return currentUser.id;
            }
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public RunningTaskInfo getRunningTask() {
        return getRunningTask(3);
    }

    public RunningTaskInfo getRunningTask(@ActivityType int i) {
        try {
            List filteredTasks = ActivityTaskManager.getService().getFilteredTasks(1, i, 2);
            if (filteredTasks.isEmpty()) {
                return null;
            }
            return (RunningTaskInfo) filteredTasks.get(0);
        } catch (RemoteException unused) {
            return null;
        }
    }

    public boolean setTaskWindowingModeSplitScreenPrimary(int i, int i2, Rect rect) {
        try {
            return ActivityTaskManager.getService().setTaskWindowingModeSplitScreenPrimary(i, i2, true, false, rect, true);
        } catch (RemoteException unused) {
            return false;
        }
    }

    public void registerTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.addListener(ActivityManager.getService(), taskStackChangeListener);
        }
    }

    public void unregisterTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.removeListener(taskStackChangeListener);
        }
    }

    public Future<?> closeSystemWindows(final String str) {
        return this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                try {
                    ActivityManager.getService().closeSystemDialogs(str);
                } catch (RemoteException e) {
                    Log.w("ActivityManagerWrapper", "Failed to close system windows", e);
                }
            }
        });
    }

    public boolean isScreenPinningActive() {
        try {
            return ActivityTaskManager.getService().getLockTaskModeState() == 2;
        } catch (RemoteException unused) {
            return false;
        }
    }

    public boolean isLockTaskKioskModeActive() {
        try {
            return ActivityTaskManager.getService().getLockTaskModeState() == 1;
        } catch (RemoteException unused) {
            return false;
        }
    }
}
