package com.android.systemui.assist;

import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService.OverviewProxyListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

final class AssistHandleReminderExpBehavior implements BehaviorController {
    private static final String[] DEFAULT_HOME_CHANGE_ACTIONS = {"android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED", "android.intent.action.BOOT_COMPLETED", "android.intent.action.PACKAGE_ADDED", "android.intent.action.PACKAGE_CHANGED", "android.intent.action.PACKAGE_REMOVED"};
    private static final long DEFAULT_LEARNING_TIME_MS = TimeUnit.DAYS.toMillis(3);
    private static final long DEFAULT_SHOW_AND_GO_DELAYED_LONG_DELAY_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long DEFAULT_SHOW_AND_GO_DELAY_RESET_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3);
    private final ActivityManagerWrapper mActivityManagerWrapper;
    private AssistHandleCallbacks mAssistHandleCallbacks;
    private int mConsecutiveTaskSwitches;
    private Context mContext;
    /* access modifiers changed from: private */
    public ComponentName mDefaultHome;
    private final BroadcastReceiver mDefaultHomeBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            AssistHandleReminderExpBehavior.this.mDefaultHome = AssistHandleReminderExpBehavior.getCurrentDefaultHome();
        }
    };
    private final IntentFilter mDefaultHomeIntentFilter;
    private final Handler mHandler;
    private boolean mIsDozing;
    private boolean mIsLauncherShowing;
    private boolean mIsLearned;
    private boolean mIsNavBarHidden;
    private long mLastLearningTimestamp;
    private long mLearnedHintLastShownEpochDay;
    private int mLearningCount;
    private long mLearningTimeElapsed;
    private boolean mOnLockscreen;
    private final OverviewProxyListener mOverviewProxyListener = new OverviewProxyListener() {
        public void onOverviewShown(boolean z) {
            AssistHandleReminderExpBehavior.this.handleOverviewShown();
        }

        public void onSystemUiStateChanged(int i) {
            AssistHandleReminderExpBehavior.this.handleSystemUiStateChanged(i);
        }
    };
    private final OverviewProxyService mOverviewProxyService;
    private final PhenotypeHelper mPhenotypeHelper;
    private final Runnable mResetConsecutiveTaskSwitches = new Runnable() {
        public final void run() {
            AssistHandleReminderExpBehavior.this.resetConsecutiveTaskSwitches();
        }
    };
    private int mRunningTaskId;
    private final StatusBarStateController mStatusBarStateController;
    private final StateListener mStatusBarStateListener = new StateListener() {
        public void onStateChanged(int i) {
            AssistHandleReminderExpBehavior.this.handleStatusBarStateChanged(i);
        }

        public void onDozingChanged(boolean z) {
            AssistHandleReminderExpBehavior.this.handleDozingChanged(z);
        }
    };
    private final TaskStackChangeListener mTaskStackChangeListener = new TaskStackChangeListener() {
        public void onTaskMovedToFront(RunningTaskInfo runningTaskInfo) {
            AssistHandleReminderExpBehavior.this.handleTaskStackTopChanged(runningTaskInfo.taskId, runningTaskInfo.topActivity);
        }

        public void onTaskCreated(int i, ComponentName componentName) {
            AssistHandleReminderExpBehavior.this.handleTaskStackTopChanged(i, componentName);
        }
    };

    private static boolean isNavBarHidden(int i) {
        return (i & 2) != 0;
    }

    private boolean onLockscreen(int i) {
        return i == 1 || i == 2;
    }

    AssistHandleReminderExpBehavior(Handler handler, PhenotypeHelper phenotypeHelper) {
        this.mHandler = handler;
        this.mPhenotypeHelper = phenotypeHelper;
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        this.mActivityManagerWrapper = ActivityManagerWrapper.getInstance();
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mDefaultHomeIntentFilter = new IntentFilter();
        for (String addAction : DEFAULT_HOME_CHANGE_ACTIONS) {
            this.mDefaultHomeIntentFilter.addAction(addAction);
        }
    }

    public void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks) {
        int i;
        this.mContext = context;
        this.mAssistHandleCallbacks = assistHandleCallbacks;
        this.mConsecutiveTaskSwitches = 0;
        this.mDefaultHome = getCurrentDefaultHome();
        context.registerReceiver(this.mDefaultHomeBroadcastReceiver, this.mDefaultHomeIntentFilter);
        this.mOnLockscreen = onLockscreen(this.mStatusBarStateController.getState());
        this.mIsDozing = this.mStatusBarStateController.isDozing();
        this.mStatusBarStateController.addCallback(this.mStatusBarStateListener);
        RunningTaskInfo runningTask = this.mActivityManagerWrapper.getRunningTask();
        if (runningTask == null) {
            i = 0;
        } else {
            i = runningTask.taskId;
        }
        this.mRunningTaskId = i;
        this.mActivityManagerWrapper.registerTaskStackListener(this.mTaskStackChangeListener);
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        this.mLearningTimeElapsed = Secure.getLong(context.getContentResolver(), "reminder_exp_learning_time_elapsed", 0);
        this.mLearningCount = Secure.getInt(context.getContentResolver(), "reminder_exp_learning_event_count", 0);
        this.mLearnedHintLastShownEpochDay = Secure.getLong(context.getContentResolver(), "reminder_exp_learned_hint_last_shown", 0);
        this.mLastLearningTimestamp = SystemClock.uptimeMillis();
        callbackForCurrentState(false);
    }

    public void onModeDeactivated() {
        this.mAssistHandleCallbacks = null;
        Context context = this.mContext;
        if (context != null) {
            context.unregisterReceiver(this.mDefaultHomeBroadcastReceiver);
            Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learning_time_elapsed", 0);
            Secure.putInt(this.mContext.getContentResolver(), "reminder_exp_learning_event_count", 0);
            Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learned_hint_last_shown", 0);
            this.mContext = null;
        }
        this.mStatusBarStateController.removeCallback(this.mStatusBarStateListener);
        this.mActivityManagerWrapper.unregisterTaskStackListener(this.mTaskStackChangeListener);
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
    }

    public void onAssistantGesturePerformed() {
        Context context = this.mContext;
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            int i = this.mLearningCount + 1;
            this.mLearningCount = i;
            Secure.putLong(contentResolver, "reminder_exp_learning_event_count", (long) i);
        }
    }

    /* access modifiers changed from: private */
    public static ComponentName getCurrentDefaultHome() {
        ArrayList arrayList = new ArrayList();
        ComponentName homeActivities = PackageManagerWrapper.getInstance().getHomeActivities(arrayList);
        if (homeActivities != null) {
            return homeActivities;
        }
        Iterator it = arrayList.iterator();
        int i = Integer.MIN_VALUE;
        while (true) {
            ComponentName componentName = null;
            while (true) {
                if (!it.hasNext()) {
                    return componentName;
                }
                ResolveInfo resolveInfo = (ResolveInfo) it.next();
                int i2 = resolveInfo.priority;
                if (i2 > i) {
                    componentName = resolveInfo.activityInfo.getComponentName();
                    i = resolveInfo.priority;
                } else if (i2 == i) {
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleStatusBarStateChanged(int i) {
        boolean onLockscreen = onLockscreen(i);
        if (this.mOnLockscreen != onLockscreen) {
            resetConsecutiveTaskSwitches();
            this.mOnLockscreen = onLockscreen;
            callbackForCurrentState(!onLockscreen);
        }
    }

    /* access modifiers changed from: private */
    public void handleDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            resetConsecutiveTaskSwitches();
            this.mIsDozing = z;
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleTaskStackTopChanged(int i, ComponentName componentName) {
        if (this.mRunningTaskId != i && componentName != null) {
            this.mRunningTaskId = i;
            this.mIsLauncherShowing = componentName.equals(this.mDefaultHome);
            if (this.mIsLauncherShowing) {
                resetConsecutiveTaskSwitches();
            } else {
                rescheduleConsecutiveTaskSwitchesReset();
                this.mConsecutiveTaskSwitches++;
            }
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleSystemUiStateChanged(int i) {
        boolean isNavBarHidden = isNavBarHidden(i);
        if (this.mIsNavBarHidden != isNavBarHidden) {
            resetConsecutiveTaskSwitches();
            this.mIsNavBarHidden = isNavBarHidden;
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOverviewShown() {
        resetConsecutiveTaskSwitches();
        callbackForCurrentState(false);
    }

    private void callbackForCurrentState(boolean z) {
        updateLearningStatus();
        if (this.mIsLearned) {
            callbackForLearnedState(z);
        } else {
            callbackForUnlearnedState();
        }
    }

    private void callbackForLearnedState(boolean z) {
        if (this.mAssistHandleCallbacks != null) {
            if (this.mIsDozing || this.mIsNavBarHidden || this.mOnLockscreen || !getShowWhenTaught()) {
                this.mAssistHandleCallbacks.hide();
            } else if (z) {
                long epochDay = LocalDate.now().toEpochDay();
                if (this.mLearnedHintLastShownEpochDay < epochDay) {
                    Context context = this.mContext;
                    if (context != null) {
                        Secure.putLong(context.getContentResolver(), "reminder_exp_learned_hint_last_shown", epochDay);
                    }
                    this.mLearnedHintLastShownEpochDay = epochDay;
                    this.mAssistHandleCallbacks.showAndGo();
                }
            }
        }
    }

    private void callbackForUnlearnedState() {
        if (this.mAssistHandleCallbacks != null) {
            if (this.mIsDozing || this.mIsNavBarHidden || isSuppressed()) {
                this.mAssistHandleCallbacks.hide();
            } else if (this.mOnLockscreen) {
                this.mAssistHandleCallbacks.showAndStay();
            } else if (this.mIsLauncherShowing) {
                this.mAssistHandleCallbacks.showAndGo();
            } else if (this.mConsecutiveTaskSwitches == 1) {
                this.mAssistHandleCallbacks.showAndGoDelayed(getShowAndGoDelayedShortDelayMs(), false);
            } else {
                this.mAssistHandleCallbacks.showAndGoDelayed(getShowAndGoDelayedLongDelayMs(), true);
            }
        }
    }

    private boolean isSuppressed() {
        if (this.mOnLockscreen) {
            return getSuppressOnLockscreen();
        }
        if (this.mIsLauncherShowing) {
            return getSuppressOnLauncher();
        }
        return getSuppressOnApps();
    }

    private void updateLearningStatus() {
        if (this.mContext != null) {
            long uptimeMillis = SystemClock.uptimeMillis();
            this.mLearningTimeElapsed += uptimeMillis - this.mLastLearningTimestamp;
            this.mLastLearningTimestamp = uptimeMillis;
            Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learning_time_elapsed", this.mLearningTimeElapsed);
            this.mIsLearned = this.mLearningCount >= getLearningCount() || this.mLearningTimeElapsed >= getLearningTimeMs();
        }
    }

    /* access modifiers changed from: private */
    public void resetConsecutiveTaskSwitches() {
        this.mHandler.removeCallbacks(this.mResetConsecutiveTaskSwitches);
        this.mConsecutiveTaskSwitches = 0;
    }

    private void rescheduleConsecutiveTaskSwitchesReset() {
        this.mHandler.removeCallbacks(this.mResetConsecutiveTaskSwitches);
        this.mHandler.postDelayed(this.mResetConsecutiveTaskSwitches, getShowAndGoDelayResetTimeoutMs());
    }

    private long getLearningTimeMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_learn_time_ms", DEFAULT_LEARNING_TIME_MS);
    }

    private int getLearningCount() {
        return this.mPhenotypeHelper.getInt("assist_handles_learn_count", 3);
    }

    private long getShowAndGoDelayedShortDelayMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delayed_short_delay_ms", 150);
    }

    private long getShowAndGoDelayedLongDelayMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delayed_long_delay_ms", DEFAULT_SHOW_AND_GO_DELAYED_LONG_DELAY_MS);
    }

    private long getShowAndGoDelayResetTimeoutMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delay_reset_timeout_ms", DEFAULT_SHOW_AND_GO_DELAY_RESET_TIMEOUT_MS);
    }

    private boolean getSuppressOnLockscreen() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_lockscreen", false);
    }

    private boolean getSuppressOnLauncher() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_launcher", false);
    }

    private boolean getSuppressOnApps() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_apps", true);
    }

    private boolean getShowWhenTaught() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_show_when_taught", false);
    }

    public void dump(PrintWriter printWriter, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("Current AssistHandleReminderExpBehavior State:");
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append("   mOnLockscreen=");
        sb2.append(this.mOnLockscreen);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(str);
        sb3.append("   mIsDozing=");
        sb3.append(this.mIsDozing);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append(str);
        sb4.append("   mRunningTaskId=");
        sb4.append(this.mRunningTaskId);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append(str);
        sb5.append("   mDefaultHome=");
        sb5.append(this.mDefaultHome);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append(str);
        sb6.append("   mIsNavBarHidden=");
        sb6.append(this.mIsNavBarHidden);
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append(str);
        sb7.append("   mIsLauncherShowing=");
        sb7.append(this.mIsLauncherShowing);
        printWriter.println(sb7.toString());
        StringBuilder sb8 = new StringBuilder();
        sb8.append(str);
        sb8.append("   mConsecutiveTaskSwitches=");
        sb8.append(this.mConsecutiveTaskSwitches);
        printWriter.println(sb8.toString());
        StringBuilder sb9 = new StringBuilder();
        sb9.append(str);
        sb9.append("   mIsLearned=");
        sb9.append(this.mIsLearned);
        printWriter.println(sb9.toString());
        StringBuilder sb10 = new StringBuilder();
        sb10.append(str);
        sb10.append("   mLastLearningTimestamp=");
        sb10.append(this.mLastLearningTimestamp);
        printWriter.println(sb10.toString());
        StringBuilder sb11 = new StringBuilder();
        sb11.append(str);
        sb11.append("   mLearningTimeElapsed=");
        sb11.append(this.mLearningTimeElapsed);
        printWriter.println(sb11.toString());
        StringBuilder sb12 = new StringBuilder();
        sb12.append(str);
        sb12.append("   mLearningCount=");
        sb12.append(this.mLearningCount);
        printWriter.println(sb12.toString());
        StringBuilder sb13 = new StringBuilder();
        sb13.append(str);
        sb13.append("   mLearnedHintLastShownEpochDay=");
        sb13.append(this.mLearnedHintLastShownEpochDay);
        printWriter.println(sb13.toString());
        StringBuilder sb14 = new StringBuilder();
        sb14.append(str);
        sb14.append("   mAssistHandleCallbacks present: ");
        sb14.append(this.mAssistHandleCallbacks != null);
        printWriter.println(sb14.toString());
        StringBuilder sb15 = new StringBuilder();
        sb15.append(str);
        sb15.append("   Phenotype Flags:");
        printWriter.println(sb15.toString());
        StringBuilder sb16 = new StringBuilder();
        sb16.append(str);
        String str2 = "      ";
        sb16.append(str2);
        sb16.append("assist_handles_learn_time_ms");
        String str3 = "=";
        sb16.append(str3);
        sb16.append(getLearningTimeMs());
        printWriter.println(sb16.toString());
        StringBuilder sb17 = new StringBuilder();
        sb17.append(str);
        sb17.append(str2);
        sb17.append("assist_handles_learn_count");
        sb17.append(str3);
        sb17.append(getLearningCount());
        printWriter.println(sb17.toString());
        StringBuilder sb18 = new StringBuilder();
        sb18.append(str);
        sb18.append(str2);
        sb18.append("assist_handles_show_and_go_delayed_short_delay_ms");
        sb18.append(str3);
        sb18.append(getShowAndGoDelayedShortDelayMs());
        printWriter.println(sb18.toString());
        StringBuilder sb19 = new StringBuilder();
        sb19.append(str);
        sb19.append(str2);
        sb19.append("assist_handles_show_and_go_delayed_long_delay_ms");
        sb19.append(str3);
        sb19.append(getShowAndGoDelayedLongDelayMs());
        printWriter.println(sb19.toString());
        StringBuilder sb20 = new StringBuilder();
        sb20.append(str);
        sb20.append(str2);
        sb20.append("assist_handles_show_and_go_delay_reset_timeout_ms");
        sb20.append(str3);
        sb20.append(getShowAndGoDelayResetTimeoutMs());
        printWriter.println(sb20.toString());
        StringBuilder sb21 = new StringBuilder();
        sb21.append(str);
        sb21.append(str2);
        sb21.append("assist_handles_suppress_on_lockscreen");
        sb21.append(str3);
        sb21.append(getSuppressOnLockscreen());
        printWriter.println(sb21.toString());
        StringBuilder sb22 = new StringBuilder();
        sb22.append(str);
        sb22.append(str2);
        sb22.append("assist_handles_suppress_on_launcher");
        sb22.append(str3);
        sb22.append(getSuppressOnLauncher());
        printWriter.println(sb22.toString());
        StringBuilder sb23 = new StringBuilder();
        sb23.append(str);
        sb23.append(str2);
        sb23.append("assist_handles_suppress_on_apps");
        sb23.append(str3);
        sb23.append(getSuppressOnApps());
        printWriter.println(sb23.toString());
        StringBuilder sb24 = new StringBuilder();
        sb24.append(str);
        sb24.append(str2);
        sb24.append("assist_handles_show_when_taught");
        sb24.append(str3);
        sb24.append(getShowWhenTaught());
        printWriter.println(sb24.toString());
    }
}
