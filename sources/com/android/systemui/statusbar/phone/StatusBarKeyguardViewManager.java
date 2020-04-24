package com.android.systemui.statusbar.phone;

import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.StatsLog;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.dock.DockManager.DockEventListener;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.plugins.ActivityStarter.OnDismissAction;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.RemoteInputController.Callback;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import com.android.systemui.statusbar.phone.NotificationPanelView.PanelExpansionListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.oneplus.systemui.statusbar.phone.OpStatusBarKeyguardViewManager;
import com.oneplus.util.OpNavBarUtils;
import java.io.PrintWriter;
import java.util.ArrayList;

public class StatusBarKeyguardViewManager extends OpStatusBarKeyguardViewManager implements Callback, StateListener, ConfigurationListener, PanelExpansionListener, ModeChangedListener {
    private static String TAG = "StatusBarKeyguardViewManager";
    private OnDismissAction mAfterKeyguardGoneAction;
    private final ArrayList<Runnable> mAfterKeyguardGoneRunnables = new ArrayList<>();
    private BiometricUnlockController mBiometricUnlockController;
    protected KeyguardBouncer mBouncer;
    /* access modifiers changed from: private */
    public ViewGroup mContainer;
    protected final Context mContext;
    private final DockEventListener mDockEventListener = new DockEventListener() {
    };
    private final DockManager mDockManager;
    private boolean mDozing;
    private final BouncerExpansionCallback mExpansionCallback = new BouncerExpansionCallback() {
        public void onFullyShown() {
            StatusBarKeyguardViewManager.this.updateStates();
            StatusBarKeyguardViewManager.this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), StatusBarKeyguardViewManager.this.mContainer, "BOUNCER_VISIBLE");
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }

        public void onStartingToHide() {
            StatusBarKeyguardViewManager.this.updateStates();
        }

        public void onStartingToShow() {
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }

        public void onFullyHidden() {
            StatusBarKeyguardViewManager.this.updateStates();
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }
    };
    protected boolean mFirstUpdate = true;
    private boolean mGesturalNav;
    private boolean mGoingToSleepVisibleNotOccluded;
    private INotificationManager mINotificationManager;
    private boolean mIsDocked;
    private final KeyguardMonitorImpl mKeyguardMonitor = ((KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class));
    private int mLastBiometricMode;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    private boolean mLastDozing;
    private boolean mLastGesturalNav;
    private boolean mLastIsDocked;
    private boolean mLastLockVisible;
    protected boolean mLastOccluded;
    private boolean mLastPulsing;
    protected boolean mLastRemoteInputActive;
    protected boolean mLastShowing;
    private ViewGroup mLockIconContainer;
    protected LockPatternUtils mLockPatternUtils;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        public void run() {
            StatusBarKeyguardViewManager.this.mStatusBar.getNavigationBarView().getRootView().setVisibility(0);
        }
    };
    private final NotificationMediaManager mMediaManager = ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class));
    private NotificationPanelView mNotificationPanelView;
    protected boolean mOccluded;
    private DismissWithActionRequest mPendingWakeupAction;
    private boolean mPulsing;
    protected boolean mRemoteInputActive;
    protected boolean mShowing;
    protected StatusBar mStatusBar;
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    /* access modifiers changed from: private */
    public final StatusBarWindowController mStatusBarWindowController;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onEmergencyCallAction() {
            StatusBarKeyguardViewManager statusBarKeyguardViewManager = StatusBarKeyguardViewManager.this;
            if (statusBarKeyguardViewManager.mOccluded) {
                statusBarKeyguardViewManager.reset(true);
            }
        }
    };
    protected ViewMediatorCallback mViewMediatorCallback;

    private static class DismissWithActionRequest {
        final boolean afterKeyguardGone;
        final Runnable cancelAction;
        final OnDismissAction dismissAction;
        final String message;

        DismissWithActionRequest(OnDismissAction onDismissAction, Runnable runnable, boolean z, String str) {
            this.dismissAction = onDismissAction;
            this.cancelAction = runnable;
            this.afterKeyguardGone = z;
            this.message = str;
        }
    }

    public void onCancelClicked() {
    }

    public void onScreenTurnedOn() {
    }

    public void onScreenTurningOn() {
    }

    public void onStartedWakingUp() {
    }

    /* access modifiers changed from: protected */
    public boolean shouldDestroyViewOnReset() {
        return false;
    }

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateMonitorCallback);
        this.mStatusBarStateController.addCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mGesturalNav = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this));
        this.mDockManager = (DockManager) Dependency.get(DockManager.class);
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.addListener(this.mDockEventListener);
            this.mIsDocked = this.mDockManager.isDocked();
        }
    }

    public void registerStatusBar(StatusBar statusBar, ViewGroup viewGroup, NotificationPanelView notificationPanelView, BiometricUnlockController biometricUnlockController, DismissCallbackRegistry dismissCallbackRegistry, ViewGroup viewGroup2) {
        this.mStatusBar = statusBar;
        this.mContainer = viewGroup;
        this.mLockIconContainer = viewGroup2;
        ViewGroup viewGroup3 = this.mLockIconContainer;
        if (viewGroup3 != null) {
            this.mLastLockVisible = viewGroup3.getVisibility() == 0;
        }
        this.mBiometricUnlockController = biometricUnlockController;
        this.mBouncer = SystemUIFactory.getInstance().createKeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, viewGroup, dismissCallbackRegistry, this.mExpansionCallback);
        this.mNotificationPanelView = notificationPanelView;
        notificationPanelView.setExpansionListener(this);
        this.mINotificationManager = Stub.asInterface(ServiceManager.getService("notification"));
    }

    public void onPanelExpansionChanged(float f, boolean z) {
        if (this.mNotificationPanelView.isUnlockHintRunning()) {
            this.mBouncer.setExpansion(1.0f);
        } else if (bouncerNeedsScrimming()) {
            this.mBouncer.setExpansion(0.0f);
        } else if (this.mShowing) {
            if (!isWakeAndUnlocking() && !this.mStatusBar.isInLaunchTransition()) {
                this.mBouncer.setExpansion(f);
            }
            if (f != 1.0f && z && this.mStatusBar.isKeyguardCurrentlySecure() && !this.mBouncer.isShowing() && !this.mBouncer.isAnimatingAway()) {
                this.mBouncer.show(false, false);
            }
        } else if (this.mPulsing && f == 0.0f) {
            this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), this.mContainer, "BOUNCER_VISIBLE");
        }
    }

    public void onQsExpansionChanged(float f) {
        updateLockIcon();
    }

    /* access modifiers changed from: private */
    public void updateLockIcon() {
        if (this.mLockIconContainer != null) {
            boolean z = true;
            boolean z2 = this.mStatusBarStateController.getState() == 1 && !this.mNotificationPanelView.isQsExpanded();
            if ((!this.mBouncer.isShowing() && !z2) || this.mBouncer.isAnimatingAway()) {
                z = false;
            }
            if (this.mLastLockVisible != z) {
                this.mLastLockVisible = z;
                if (z) {
                    CrossFadeHelper.fadeIn((View) this.mLockIconContainer, 220, 0);
                } else {
                    CrossFadeHelper.fadeOut(this.mLockIconContainer, 110, 0, null);
                }
            }
        }
    }

    public void show(Bundle bundle) {
        this.mShowing = true;
        this.mStatusBarWindowController.setKeyguardShowing(true);
        KeyguardMonitorImpl keyguardMonitorImpl = this.mKeyguardMonitor;
        keyguardMonitorImpl.notifyKeyguardState(this.mShowing, keyguardMonitorImpl.isSecure(), this.mKeyguardMonitor.isOccluded());
        reset(true);
        StatsLog.write(62, 2);
        if (isSecure()) {
            try {
                this.mINotificationManager.setAppLock();
            } catch (RemoteException unused) {
                Log.w(TAG, "Talk to NotificationManagerService fail");
            }
        }
    }

    /* access modifiers changed from: protected */
    public void showBouncerOrKeyguard(boolean z) {
        if (this.mBouncer.isUserUnlocked() || (this.mBouncer.isShowing() && KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn())) {
            if (!this.mBouncer.needsFullscreenBouncer() || this.mDozing) {
                this.mStatusBar.showKeyguard();
                if (z) {
                    hideBouncer(shouldDestroyViewOnReset());
                    this.mBouncer.prepare();
                }
            } else {
                this.mStatusBar.hideKeyguard();
                this.mBouncer.show(true);
            }
            updateStates();
            return;
        }
        this.mStatusBar.showKeyguard();
        this.mStatusBar.instantCollapseNotificationPanel();
        this.mBouncer.show(true);
        updateStates();
    }

    /* access modifiers changed from: private */
    public void hideBouncer(boolean z) {
        KeyguardBouncer keyguardBouncer = this.mBouncer;
        if (keyguardBouncer != null) {
            keyguardBouncer.hide(z);
            cancelPendingWakeupAction();
        }
    }

    public void showBouncer(boolean z) {
        if (this.mShowing && !this.mBouncer.isShowing()) {
            this.mBouncer.show(false, z);
        }
        updateStates();
    }

    public void dismissWithAction(OnDismissAction onDismissAction, Runnable runnable, boolean z) {
        dismissWithAction(onDismissAction, runnable, z, null);
    }

    public void dismissWithAction(OnDismissAction onDismissAction, Runnable runnable, boolean z, String str) {
        if (this.mShowing) {
            cancelPendingWakeupAction();
            if (this.mDozing && !isWakeAndUnlocking()) {
                this.mPendingWakeupAction = new DismissWithActionRequest(onDismissAction, runnable, z, str);
                return;
            } else if (!z) {
                this.mBouncer.showWithDismissAction(onDismissAction, runnable);
            } else {
                this.mAfterKeyguardGoneAction = onDismissAction;
                this.mBouncer.show(false);
            }
        }
        updateStates();
    }

    private boolean isWakeAndUnlocking() {
        int mode = this.mBiometricUnlockController.getMode();
        return mode == 1 || mode == 2;
    }

    public void addAfterKeyguardGoneRunnable(Runnable runnable) {
        this.mAfterKeyguardGoneRunnables.add(runnable);
    }

    public void reset(boolean z) {
        BiometricUnlockController biometricUnlockController = this.mBiometricUnlockController;
        boolean isBiometricUnlock = biometricUnlockController == null ? false : biometricUnlockController.isBiometricUnlock();
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("reset / mShowing:");
            sb.append(this.mShowing);
            sb.append(" / mOccluded:");
            sb.append(this.mOccluded);
            sb.append(" / mDozing:");
            sb.append(this.mDozing);
            sb.append(" / hideBouncerWhenShowing:");
            sb.append(z);
            sb.append(" / isBiometricUnlock: ");
            sb.append(isBiometricUnlock);
            sb.append(" / isSimPinSecure ");
            sb.append(KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure());
            Log.i(str, sb.toString());
        }
        if (!this.mShowing) {
            return;
        }
        if (!isBiometricUnlock || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
            if (!this.mOccluded || this.mDozing) {
                showBouncerOrKeyguard(z);
            } else {
                this.mStatusBar.hideKeyguard();
                this.mStatusBarWindowController.setKeyguardFadingAway(false);
                if (z || this.mBouncer.needsFullscreenBouncer()) {
                    hideBouncer(false);
                }
            }
            KeyguardUpdateMonitor.getInstance(this.mContext).sendKeyguardReset();
            updateStates();
        }
    }

    public boolean isGoingToSleepVisibleNotOccluded() {
        return this.mGoingToSleepVisibleNotOccluded;
    }

    public void onStartedGoingToSleep() {
        this.mGoingToSleepVisibleNotOccluded = isShowing() && !isOccluded();
        opOnStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep() {
        this.mGoingToSleepVisibleNotOccluded = false;
        this.mBouncer.onScreenTurnedOff();
    }

    public void onRemoteInputActive(boolean z) {
        this.mRemoteInputActive = z;
        updateStates();
    }

    private void setDozing(boolean z) {
        if (this.mDozing != z) {
            this.mDozing = z;
            if (z || this.mBouncer.needsFullscreenBouncer() || this.mOccluded) {
                reset(z);
            }
            updateStates();
            if (!z) {
                launchPendingWakeupAction();
            }
        }
    }

    public void setPulsing(boolean z) {
        if (this.mPulsing != z) {
            this.mPulsing = z;
            updateStates();
        }
    }

    public void setNeedsInput(boolean z) {
        this.mStatusBarWindowController.setKeyguardNeedsInput(z);
    }

    public boolean isUnlockWithWallpaper() {
        return this.mStatusBarWindowController.isShowingWallpaper();
    }

    public void setOccluded(boolean z, boolean z2) {
        this.mStatusBar.setOccluded(z);
        boolean z3 = true;
        if (z && !this.mOccluded && this.mShowing) {
            StatsLog.write(62, 3);
            if (this.mStatusBar.isInLaunchTransition()) {
                this.mOccluded = true;
                this.mStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
                    public void run() {
                        StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardOccluded(StatusBarKeyguardViewManager.this.mOccluded);
                        StatusBarKeyguardViewManager.this.reset(true);
                    }
                });
                return;
            }
        } else if (!z && this.mOccluded && this.mShowing) {
            StatsLog.write(62, 2);
        }
        boolean z4 = !this.mOccluded && z;
        this.mOccluded = z;
        if (this.mShowing) {
            NotificationMediaManager notificationMediaManager = this.mMediaManager;
            if (!z2 || z) {
                z3 = false;
            }
            notificationMediaManager.updateMediaMetaData(false, z3);
        }
        this.mStatusBarWindowController.setKeyguardOccluded(z);
        if (!this.mDozing) {
            reset(z4);
        }
        if (z2 && !z && this.mShowing && !this.mBouncer.isShowing()) {
            this.mStatusBar.animateKeyguardUnoccluding();
        }
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(runnable);
            this.mNotificationPanelView.onBouncerPreHideAnimation();
        } else if (runnable != null) {
            runnable.run();
        }
        this.mNotificationPanelView.blockExpansionForCurrentTouch();
        updateLockIcon();
    }

    public void hide(long j, long j2) {
        long j3;
        long j4;
        this.mShowing = false;
        KeyguardMonitorImpl keyguardMonitorImpl = this.mKeyguardMonitor;
        keyguardMonitorImpl.notifyKeyguardState(this.mShowing, keyguardMonitorImpl.isSecure(), this.mKeyguardMonitor.isOccluded());
        launchPendingWakeupAction();
        long max = Math.max(0, (j - 48) - SystemClock.uptimeMillis());
        if (this.mStatusBar.isInLaunchTransition()) {
            this.mStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardShowing(false);
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardFadingAway(true);
                    StatusBarKeyguardViewManager.this.hideBouncer(true);
                    StatusBarKeyguardViewManager.this.updateStates();
                }
            }, new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBar.hideKeyguard();
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.keyguardGone();
                    StatusBarKeyguardViewManager.this.executeAfterKeyguardGoneAction();
                }
            });
        } else {
            executeAfterKeyguardGoneAction();
            boolean z = this.mBiometricUnlockController.getMode() == 2;
            if (z) {
                j4 = 0;
                j3 = 240;
            } else {
                j3 = j2;
                j4 = max;
            }
            this.mStatusBar.setKeyguardFadingAway(j, j4, j3);
            this.mBiometricUnlockController.startKeyguardFadingAway();
            hideBouncer(true);
            if (z) {
                this.mStatusBar.fadeKeyguardWhilePulsing();
                wakeAndUnlockDejank();
            } else if (!this.mStatusBar.hideKeyguard()) {
                this.mStatusBarWindowController.setKeyguardFadingAway(true);
                this.mStatusBar.updateScrimController();
                wakeAndUnlockDejank();
            } else {
                this.mStatusBar.finishKeyguardFadingAway();
                this.mBiometricUnlockController.finishKeyguardFadingAway();
            }
            updateStates();
            this.mStatusBarWindowController.setKeyguardShowing(false);
            this.mViewMediatorCallback.keyguardGone();
        }
        StatsLog.write(62, 1);
    }

    public void onDensityOrFontScaleChanged() {
        if (Build.DEBUG_ONEPLUS) {
            Log.i(TAG, "onDensityOrFontScaleChanged");
        }
        hideBouncer(true);
        reset(true);
    }

    public void onNavigationModeChanged(int i) {
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (isGesturalMode != this.mGesturalNav) {
            this.mGesturalNav = isGesturalMode;
            updateStates();
        }
    }

    public void onThemeChanged() {
        hideBouncer(true);
        this.mBouncer.prepare();
        opOnThemeChanged();
    }

    public /* synthetic */ void lambda$onKeyguardFadedAway$0$StatusBarKeyguardViewManager() {
        this.mStatusBarWindowController.setKeyguardFadingAway(false);
    }

    public void onKeyguardFadedAway() {
        this.mContainer.postDelayed(new Runnable() {
            public final void run() {
                StatusBarKeyguardViewManager.this.lambda$onKeyguardFadedAway$0$StatusBarKeyguardViewManager();
            }
        }, 100);
        boolean z = this.mBiometricUnlockController.getMode() == 5;
        this.mStatusBar.finishKeyguardFadingAway();
        this.mBiometricUnlockController.finishKeyguardFadingAway();
        opTrimMemory(z);
    }

    private void wakeAndUnlockDejank() {
        if (this.mBiometricUnlockController.getMode() == 1 && LatencyTracker.isEnabled(this.mContext)) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public final void run() {
                    StatusBarKeyguardViewManager.this.lambda$wakeAndUnlockDejank$1$StatusBarKeyguardViewManager();
                }
            });
        }
    }

    public /* synthetic */ void lambda$wakeAndUnlockDejank$1$StatusBarKeyguardViewManager() {
        LatencyTracker.getInstance(this.mContext).onActionEnd(2);
    }

    /* access modifiers changed from: private */
    public void executeAfterKeyguardGoneAction() {
        OnDismissAction onDismissAction = this.mAfterKeyguardGoneAction;
        if (onDismissAction != null) {
            onDismissAction.onDismiss();
            this.mAfterKeyguardGoneAction = null;
        }
        for (int i = 0; i < this.mAfterKeyguardGoneRunnables.size(); i++) {
            ((Runnable) this.mAfterKeyguardGoneRunnables.get(i)).run();
        }
        this.mAfterKeyguardGoneRunnables.clear();
    }

    public void dismissAndCollapse() {
        this.mStatusBar.executeRunnableDismissingKeyguard(null, null, true, false, true);
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed(boolean z) {
        if (!this.mBouncer.isShowing()) {
            return false;
        }
        if (this.mBouncer.isCheckingPassword()) {
            Log.d(TAG, "onBackPressed when checking pass");
            return true;
        }
        this.mStatusBar.endAffordanceLaunch();
        if (!this.mBouncer.isUserUnlocked()) {
            reset(z);
            return true;
        }
        if (!this.mBouncer.isScrimmed() || this.mBouncer.needsFullscreenBouncer()) {
            reset(z);
        } else {
            hideBouncer(false);
            updateStates();
        }
        return true;
    }

    public boolean isBouncerShowing() {
        return this.mBouncer.isShowing();
    }

    public boolean isBouncerPartiallyVisible() {
        return this.mBouncer.isPartiallyVisible();
    }

    private long getNavBarShowDelay() {
        if (this.mKeyguardMonitor.isKeyguardFadingAway()) {
            return this.mKeyguardMonitor.getKeyguardFadingAwayDelay();
        }
        return this.mBouncer.isShowing() ? 320 : 0;
    }

    /* access modifiers changed from: protected */
    public void updateStates() {
        int systemUiVisibility = this.mContainer.getSystemUiVisibility();
        boolean z = this.mShowing;
        boolean z2 = this.mOccluded;
        boolean isShowing = this.mBouncer.isShowing();
        boolean z3 = true;
        boolean z4 = !this.mBouncer.isFullscreenBouncer();
        boolean z5 = this.mRemoteInputActive;
        if ((z4 || !z || z5) != (this.mLastBouncerDismissible || !this.mLastShowing || this.mLastRemoteInputActive) || this.mFirstUpdate) {
            if (z4 || !z || z5) {
                this.mContainer.setSystemUiVisibility(-4194305 & systemUiVisibility);
            } else {
                this.mContainer.setSystemUiVisibility(4194304 | systemUiVisibility);
            }
        }
        boolean isNavBarVisible = isNavBarVisible();
        boolean lastNavBarVisible = getLastNavBarVisible();
        if (isNavBarVisible != lastNavBarVisible || this.mFirstUpdate) {
            updateNavigationBarVisibility(isNavBarVisible);
        }
        StringBuilder sb = new StringBuilder("updateStates, vis:");
        sb.append(systemUiVisibility);
        sb.append(",Showing:");
        sb.append(this.mShowing);
        sb.append(",LastShowing:");
        sb.append(this.mLastShowing);
        sb.append(",Occluded:");
        sb.append(this.mOccluded);
        sb.append(",LastOccluded:");
        sb.append(this.mLastOccluded);
        sb.append(",BouncerShow:");
        sb.append(isShowing);
        sb.append(",LastBouncerShow:");
        sb.append(this.mLastBouncerShowing);
        sb.append(",BouncerDismiss:");
        sb.append(z4);
        sb.append(",LastBouncerDismiss:");
        sb.append(this.mLastBouncerDismissible);
        sb.append(",navBarVisible:");
        sb.append(isNavBarVisible);
        sb.append(",lastNavBarVisible:");
        sb.append(lastNavBarVisible);
        sb.append(",wakeAndUnlocking:");
        sb.append(isWakeAndUnlocking());
        sb.append(",mDozing:");
        sb.append(this.mDozing);
        sb.append(",mLastBiometricMode:");
        sb.append(this.mLastBiometricMode);
        sb.append(",LastRemoteInputActive:");
        sb.append(this.mLastRemoteInputActive);
        Log.d(TAG, sb.toString());
        if (isShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            this.mStatusBarWindowController.setBouncerShowing(isShowing);
            this.mStatusBar.setBouncerShowing(isShowing);
        }
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((z && !z2) != (this.mLastShowing && !this.mLastOccluded) || this.mFirstUpdate) {
            if (OpNavBarUtils.isSupportHideNavBar() && this.mStatusBar.getNavigationBarView() != null) {
                this.mStatusBar.getNavigationBarView().onShowKeyguard(z && !z2);
            }
            if (!z || z2) {
                z3 = false;
            }
            instance.onKeyguardVisibilityChanged(z3);
        }
        if (isShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            instance.sendKeyguardBouncerChanged(isShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = z;
        this.mLastOccluded = z2;
        this.mLastBouncerShowing = isShowing;
        this.mLastBouncerDismissible = z4;
        this.mLastRemoteInputActive = z5;
        this.mLastDozing = this.mDozing;
        this.mLastPulsing = this.mPulsing;
        this.mLastBiometricMode = this.mBiometricUnlockController.getMode();
        this.mLastGesturalNav = this.mGesturalNav;
        this.mLastIsDocked = this.mIsDocked;
        this.mStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    /* access modifiers changed from: protected */
    public void updateNavigationBarVisibility(boolean z) {
        if (this.mStatusBar.getNavigationBarView() == null) {
            return;
        }
        if (z) {
            long navBarShowDelay = getNavBarShowDelay();
            if (navBarShowDelay == 0) {
                this.mMakeNavigationBarVisibleRunnable.run();
            } else {
                this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, navBarShowDelay);
            }
        } else {
            this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
            this.mStatusBar.getNavigationBarView().getRootView().setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isNavBarVisible() {
        int mode = this.mBiometricUnlockController.getMode();
        boolean z = this.mShowing && !this.mOccluded;
        boolean z2 = this.mDozing && mode != 2;
        boolean z3 = ((z && !this.mDozing) || (this.mPulsing && !this.mIsDocked)) && this.mGesturalNav;
        if ((z || z2) && !this.mBouncer.isShowing() && !this.mRemoteInputActive && !z3) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean getLastNavBarVisible() {
        boolean z = this.mLastShowing && !this.mLastOccluded;
        boolean z2 = this.mLastDozing && this.mLastBiometricMode != 2;
        boolean z3 = ((z && !this.mLastDozing) || (this.mLastPulsing && !this.mLastIsDocked)) && this.mLastGesturalNav;
        if ((z || z2) && !this.mLastBouncerShowing && !this.mLastRemoteInputActive && !z3) {
            return false;
        }
        return true;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mBouncer.shouldDismissOnMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        return this.mBouncer.interceptMediaKey(keyEvent);
    }

    public void readyForKeyguardDone() {
        this.mViewMediatorCallback.readyForKeyguardDone();
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        return this.mStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        return ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).leaveOpenOnKeyguardHide();
    }

    public void keyguardGoingAway() {
        this.mStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float f) {
        this.mStatusBar.animateCollapsePanels(0, true, false, f);
    }

    public void notifyKeyguardAuthenticated(boolean z) {
        this.mBouncer.notifyKeyguardAuthenticated(z);
    }

    public ViewRootImpl getViewRootImpl() {
        return this.mStatusBar.getStatusBarView().getViewRootImpl();
    }

    public void launchPendingWakeupAction() {
        DismissWithActionRequest dismissWithActionRequest = this.mPendingWakeupAction;
        this.mPendingWakeupAction = null;
        if (dismissWithActionRequest == null) {
            return;
        }
        if (this.mShowing) {
            dismissWithAction(dismissWithActionRequest.dismissAction, dismissWithActionRequest.cancelAction, dismissWithActionRequest.afterKeyguardGone, dismissWithActionRequest.message);
            return;
        }
        OnDismissAction onDismissAction = dismissWithActionRequest.dismissAction;
        if (onDismissAction != null) {
            onDismissAction.onDismiss();
        }
    }

    public void cancelPendingWakeupAction() {
        DismissWithActionRequest dismissWithActionRequest = this.mPendingWakeupAction;
        this.mPendingWakeupAction = null;
        if (dismissWithActionRequest != null) {
            Runnable runnable = dismissWithActionRequest.cancelAction;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public boolean bouncerNeedsScrimming() {
        return this.mOccluded || this.mBouncer.willDismissWithAction() || this.mStatusBar.isFullScreenUserSwitcherState() || (this.mBouncer.isShowing() && this.mBouncer.isScrimmed()) || this.mBouncer.isFullscreenBouncer();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("StatusBarKeyguardViewManager:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mShowing: ");
        sb.append(this.mShowing);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mOccluded: ");
        sb2.append(this.mOccluded);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mRemoteInputActive: ");
        sb3.append(this.mRemoteInputActive);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mDozing: ");
        sb4.append(this.mDozing);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mGoingToSleepVisibleNotOccluded: ");
        sb5.append(this.mGoingToSleepVisibleNotOccluded);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mAfterKeyguardGoneAction: ");
        sb6.append(this.mAfterKeyguardGoneAction);
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("  mAfterKeyguardGoneRunnables: ");
        sb7.append(this.mAfterKeyguardGoneRunnables);
        printWriter.println(sb7.toString());
        StringBuilder sb8 = new StringBuilder();
        sb8.append("  mPendingWakeupAction: ");
        sb8.append(this.mPendingWakeupAction);
        printWriter.println(sb8.toString());
        KeyguardBouncer keyguardBouncer = this.mBouncer;
        if (keyguardBouncer != null) {
            keyguardBouncer.dump(printWriter);
        }
    }

    public void onStateChanged(int i) {
        updateLockIcon();
    }

    public void onDozingChanged(boolean z) {
        setDozing(z);
    }

    public KeyguardBouncer getBouncer() {
        return this.mBouncer;
    }

    public void onHideNavBar(boolean z) {
        KeyguardBouncer keyguardBouncer = this.mBouncer;
        if (keyguardBouncer != null) {
            keyguardBouncer.onHideNavBar(z);
        }
    }

    public void forceHideBouncer() {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.forceHide();
        }
    }
}
