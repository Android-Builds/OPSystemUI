package com.android.systemui.assist;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DeviceConfig.OnPropertiesChangedListener;
import android.provider.DeviceConfig.Properties;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.Dumpable;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AssistHandleBehaviorController implements AssistHandleCallbacks, Dumpable {
    private static final AssistHandleBehavior DEFAULT_BEHAVIOR = AssistHandleBehavior.REMINDER_EXP;
    private static final long DEFAULT_SHOW_AND_GO_DURATION_MS = TimeUnit.SECONDS.toMillis(3);
    private final AssistUtils mAssistUtils;
    private final Map<AssistHandleBehavior, BehaviorController> mBehaviorMap;
    private final Context mContext;
    private AssistHandleBehavior mCurrentBehavior;
    private final Handler mHandler;
    private long mHandlesLastHiddenAt;
    private boolean mHandlesShowing;
    private final Runnable mHideHandles;
    private boolean mInGesturalMode;
    private final PhenotypeHelper mPhenotypeHelper;
    private final Supplier<ScreenDecorations> mScreenDecorationsSupplier;
    private final Runnable mShowAndGo;

    interface BehaviorController {
        void dump(PrintWriter printWriter, String str) {
        }

        void onAssistantGesturePerformed() {
        }

        void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks);

        void onModeDeactivated() {
        }
    }

    AssistHandleBehaviorController(Context context, AssistUtils assistUtils, Handler handler) {
        this(context, assistUtils, handler, new Supplier(context) {
            private final /* synthetic */ Context f$0;

            {
                this.f$0 = r1;
            }

            public final Object get() {
                return AssistHandleBehaviorController.lambda$new$0(this.f$0);
            }
        }, new PhenotypeHelper(), null);
    }

    static /* synthetic */ ScreenDecorations lambda$new$0(Context context) {
        return (ScreenDecorations) SysUiServiceProvider.getComponent(context, ScreenDecorations.class);
    }

    @VisibleForTesting
    AssistHandleBehaviorController(Context context, AssistUtils assistUtils, Handler handler, Supplier<ScreenDecorations> supplier, PhenotypeHelper phenotypeHelper, BehaviorController behaviorController) {
        this.mHideHandles = new Runnable() {
            public final void run() {
                AssistHandleBehaviorController.this.hideHandles();
            }
        };
        this.mShowAndGo = new Runnable() {
            public final void run() {
                AssistHandleBehaviorController.this.showAndGoInternal();
            }
        };
        this.mBehaviorMap = new EnumMap(AssistHandleBehavior.class);
        this.mHandlesShowing = false;
        AssistHandleBehavior assistHandleBehavior = AssistHandleBehavior.OFF;
        this.mCurrentBehavior = assistHandleBehavior;
        this.mContext = context;
        this.mAssistUtils = assistUtils;
        this.mHandler = handler;
        this.mScreenDecorationsSupplier = supplier;
        this.mPhenotypeHelper = phenotypeHelper;
        this.mBehaviorMap.put(assistHandleBehavior, new AssistHandleOffBehavior());
        this.mBehaviorMap.put(AssistHandleBehavior.LIKE_HOME, new AssistHandleLikeHomeBehavior());
        this.mBehaviorMap.put(AssistHandleBehavior.REMINDER_EXP, new AssistHandleReminderExpBehavior(handler, phenotypeHelper));
        if (behaviorController != null) {
            this.mBehaviorMap.put(AssistHandleBehavior.TEST, behaviorController);
        }
        this.mInGesturalMode = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(new ModeChangedListener() {
            public final void onNavigationModeChanged(int i) {
                AssistHandleBehaviorController.this.handleNavigationModeChange(i);
            }
        }));
        setBehavior(getBehaviorMode());
        PhenotypeHelper phenotypeHelper2 = this.mPhenotypeHelper;
        Handler handler2 = this.mHandler;
        Objects.requireNonNull(handler2);
        phenotypeHelper2.addOnPropertiesChangedListener(new Executor(handler2) {
            private final /* synthetic */ Handler f$0;

            {
                this.f$0 = r1;
            }

            public final void execute(Runnable runnable) {
                this.f$0.post(runnable);
            }
        }, new OnPropertiesChangedListener() {
            public final void onPropertiesChanged(Properties properties) {
                AssistHandleBehaviorController.this.lambda$new$1$AssistHandleBehaviorController(properties);
            }
        });
        ((DumpController) Dependency.get(DumpController.class)).addListener(this);
    }

    public /* synthetic */ void lambda$new$1$AssistHandleBehaviorController(Properties properties) {
        String str = "assist_handles_behavior_mode";
        if (properties.getKeyset().contains(str)) {
            setBehavior(properties.getString(str, null));
        }
    }

    public void hide() {
        clearPendingCommands();
        this.mHandler.post(this.mHideHandles);
    }

    public void showAndGo() {
        clearPendingCommands();
        this.mHandler.post(this.mShowAndGo);
    }

    /* access modifiers changed from: private */
    public void showAndGoInternal() {
        maybeShowHandles(false);
        this.mHandler.postDelayed(this.mHideHandles, getShowAndGoDuration());
    }

    public void showAndGoDelayed(long j, boolean z) {
        clearPendingCommands();
        if (z) {
            this.mHandler.post(this.mHideHandles);
        }
        this.mHandler.postDelayed(this.mShowAndGo, j);
    }

    public void showAndStay() {
        clearPendingCommands();
        this.mHandler.post(new Runnable() {
            public final void run() {
                AssistHandleBehaviorController.this.lambda$showAndStay$2$AssistHandleBehaviorController();
            }
        });
    }

    public /* synthetic */ void lambda$showAndStay$2$AssistHandleBehaviorController() {
        maybeShowHandles(true);
    }

    /* access modifiers changed from: 0000 */
    public boolean areHandlesShowing() {
        return this.mHandlesShowing;
    }

    /* access modifiers changed from: 0000 */
    public void onAssistantGesturePerformed() {
        ((BehaviorController) this.mBehaviorMap.get(this.mCurrentBehavior)).onAssistantGesturePerformed();
    }

    /* access modifiers changed from: 0000 */
    public void setBehavior(AssistHandleBehavior assistHandleBehavior) {
        if (this.mCurrentBehavior != assistHandleBehavior) {
            if (!this.mBehaviorMap.containsKey(assistHandleBehavior)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unsupported behavior requested: ");
                sb.append(assistHandleBehavior.toString());
                Log.e("AssistHandleBehavior", sb.toString());
                return;
            }
            if (this.mInGesturalMode) {
                ((BehaviorController) this.mBehaviorMap.get(this.mCurrentBehavior)).onModeDeactivated();
                ((BehaviorController) this.mBehaviorMap.get(assistHandleBehavior)).onModeActivated(this.mContext, this);
            }
            this.mCurrentBehavior = assistHandleBehavior;
        }
    }

    private void setBehavior(String str) {
        try {
            setBehavior(AssistHandleBehavior.valueOf(str));
        } catch (IllegalArgumentException | NullPointerException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid behavior: ");
            sb.append(str);
            Log.e("AssistHandleBehavior", sb.toString(), e);
        }
    }

    private boolean handlesUnblocked(boolean z) {
        boolean z2 = z || SystemClock.elapsedRealtime() - this.mHandlesLastHiddenAt >= getShownFrequencyThreshold();
        ComponentName assistComponentForUser = this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser());
        if (!z2 || assistComponentForUser == null) {
            return false;
        }
        return true;
    }

    private long getShownFrequencyThreshold() {
        return this.mPhenotypeHelper.getLong("assist_handles_shown_frequency_threshold_ms", 0);
    }

    private long getShowAndGoDuration() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_duration_ms", DEFAULT_SHOW_AND_GO_DURATION_MS);
    }

    private String getBehaviorMode() {
        return this.mPhenotypeHelper.getString("assist_handles_behavior_mode", DEFAULT_BEHAVIOR.toString());
    }

    private void maybeShowHandles(boolean z) {
        if (!this.mHandlesShowing && handlesUnblocked(z)) {
            ScreenDecorations screenDecorations = (ScreenDecorations) this.mScreenDecorationsSupplier.get();
            if (screenDecorations == null) {
                Log.w("AssistHandleBehavior", "Couldn't show handles, ScreenDecorations unavailable");
            } else {
                this.mHandlesShowing = true;
                screenDecorations.lambda$setAssistHintVisible$1$ScreenDecorations(true);
            }
        }
    }

    /* access modifiers changed from: private */
    public void hideHandles() {
        if (this.mHandlesShowing) {
            ScreenDecorations screenDecorations = (ScreenDecorations) this.mScreenDecorationsSupplier.get();
            if (screenDecorations == null) {
                Log.w("AssistHandleBehavior", "Couldn't hide handles, ScreenDecorations unavailable");
            } else {
                this.mHandlesShowing = false;
                this.mHandlesLastHiddenAt = SystemClock.elapsedRealtime();
                screenDecorations.lambda$setAssistHintVisible$1$ScreenDecorations(false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNavigationModeChange(int i) {
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (this.mInGesturalMode != isGesturalMode) {
            this.mInGesturalMode = isGesturalMode;
            if (this.mInGesturalMode) {
                ((BehaviorController) this.mBehaviorMap.get(this.mCurrentBehavior)).onModeActivated(this.mContext, this);
            } else {
                ((BehaviorController) this.mBehaviorMap.get(this.mCurrentBehavior)).onModeDeactivated();
                hide();
            }
        }
    }

    private void clearPendingCommands() {
        this.mHandler.removeCallbacks(this.mHideHandles);
        this.mHandler.removeCallbacks(this.mShowAndGo);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setInGesturalModeForTest(boolean z) {
        this.mInGesturalMode = z;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Current AssistHandleBehaviorController State:");
        StringBuilder sb = new StringBuilder();
        sb.append("   mHandlesShowing=");
        sb.append(this.mHandlesShowing);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("   mHandlesLastHiddenAt=");
        sb2.append(this.mHandlesLastHiddenAt);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("   mInGesturalMode=");
        sb3.append(this.mInGesturalMode);
        printWriter.println(sb3.toString());
        printWriter.println("   Phenotype Flags:");
        StringBuilder sb4 = new StringBuilder();
        sb4.append("      assist_handles_show_and_go_duration_ms=");
        sb4.append(getShowAndGoDuration());
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("      assist_handles_shown_frequency_threshold_ms=");
        sb5.append(getShownFrequencyThreshold());
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("      assist_handles_behavior_mode=");
        sb6.append(getBehaviorMode());
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("   mCurrentBehavior=");
        sb7.append(this.mCurrentBehavior.toString());
        printWriter.println(sb7.toString());
        ((BehaviorController) this.mBehaviorMap.get(this.mCurrentBehavior)).dump(printWriter, "   ");
    }
}
