package com.android.systemui.statusbar.phone;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.StatusBarNotification;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.service.vr.IVrStateCallbacks.Stub;
import android.util.Log;
import android.util.Slog;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.widget.MessagingGroup;
import com.android.internal.widget.MessagingMessage;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.ForegroundServiceNotificationListener;
import com.android.systemui.InitController;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter.OnDismissAction;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager.Callback;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.notification.AboveShelfObserver;
import com.android.systemui.statusbar.notification.AboveShelfObserver.HasViewAboveShelfChangedListener;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationAlertingManager;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider.HeadsUpSuppressor;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinderImpl;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinderImpl.BindRowCallback;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager.OnSettingsClickListener;
import com.android.systemui.statusbar.notification.row.NotificationInfo.CheckSaveListener;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public class StatusBarNotificationPresenter implements NotificationPresenter, ConfigurationListener, BindRowCallback {
    private final AboveShelfObserver mAboveShelfObserver;
    private final AccessibilityManager mAccessibilityManager;
    private final ActivityLaunchAnimator mActivityLaunchAnimator;
    private final ActivityStarter mActivityStarter = ((ActivityStarter) Dependency.get(ActivityStarter.class));
    protected AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    /* access modifiers changed from: private */
    public final IStatusBarService mBarService;
    private final CheckSaveListener mCheckSaveListener = new CheckSaveListener() {
        public void checkSave(Runnable runnable, StatusBarNotification statusBarNotification) {
            if (!StatusBarNotificationPresenter.this.mLockscreenUserManager.isLockscreenPublicMode(statusBarNotification.getUser().getIdentifier()) || !StatusBarNotificationPresenter.this.mKeyguardManager.isKeyguardLocked()) {
                runnable.run();
            } else {
                StatusBarNotificationPresenter.this.onLockedNotificationImportanceChange(new OnDismissAction(runnable) {
                    private final /* synthetic */ Runnable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final boolean onDismiss() {
                        return this.f$0.run();
                    }
                });
            }
        }
    };
    private final CommandQueue mCommandQueue;
    private final Context mContext;
    private boolean mDispatchUiModeChangeOnUserSwitched;
    private final DozeScrimController mDozeScrimController;
    private final NotificationEntryManager mEntryManager = ((NotificationEntryManager) Dependency.get(NotificationEntryManager.class));
    private final NotificationGutsManager mGutsManager = ((NotificationGutsManager) Dependency.get(NotificationGutsManager.class));
    private final HeadsUpManagerPhone mHeadsUpManager;
    /* access modifiers changed from: private */
    public final KeyguardManager mKeyguardManager;
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    private final LockscreenGestureLogger mLockscreenGestureLogger = ((LockscreenGestureLogger) Dependency.get(LockscreenGestureLogger.class));
    /* access modifiers changed from: private */
    public final NotificationLockscreenUserManager mLockscreenUserManager = ((NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class));
    private final int mMaxAllowedKeyguardNotifications;
    private int mMaxKeyguardNotifications;
    private final NotificationMediaManager mMediaManager = ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class));
    private final NotificationInterruptionStateProvider mNotificationInterruptionStateProvider = ((NotificationInterruptionStateProvider) Dependency.get(NotificationInterruptionStateProvider.class));
    private final NotificationPanelView mNotificationPanel;
    private final OnSettingsClickListener mOnSettingsClickListener = new OnSettingsClickListener() {
        public void onSettingsClick(String str) {
            try {
                StatusBarNotificationPresenter.this.mBarService.onNotificationSettingsViewed(str);
            } catch (RemoteException unused) {
            }
        }
    };
    private final OpNotificationController mOpNotificationController = ((OpNotificationController) Dependency.get(OpNotificationController.class));
    private boolean mReinflateNotificationsOnUserSwitched;
    private final ScrimController mScrimController;
    /* access modifiers changed from: private */
    public final ShadeController mShadeController = ((ShadeController) Dependency.get(ShadeController.class));
    private final StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final SysuiStatusBarStateController mStatusBarStateController = ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class));
    private final UnlockMethodCache mUnlockMethodCache;
    private final NotificationViewHierarchyManager mViewHierarchyManager = ((NotificationViewHierarchyManager) Dependency.get(NotificationViewHierarchyManager.class));
    private final VisualStabilityManager mVisualStabilityManager = ((VisualStabilityManager) Dependency.get(VisualStabilityManager.class));
    protected boolean mVrMode;
    private final IVrStateCallbacks mVrStateCallbacks = new Stub() {
        public void onVrStateChanged(boolean z) {
            StatusBarNotificationPresenter.this.mVrMode = z;
        }
    };

    public StatusBarNotificationPresenter(Context context, NotificationPanelView notificationPanelView, HeadsUpManagerPhone headsUpManagerPhone, StatusBarWindowView statusBarWindowView, ViewGroup viewGroup, DozeScrimController dozeScrimController, ScrimController scrimController, ActivityLaunchAnimator activityLaunchAnimator, StatusBarKeyguardViewManager statusBarKeyguardViewManager, NotificationAlertingManager notificationAlertingManager, NotificationRowBinderImpl notificationRowBinderImpl) {
        this.mContext = context;
        this.mNotificationPanel = notificationPanelView;
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mAboveShelfObserver = new AboveShelfObserver(viewGroup);
        this.mActivityLaunchAnimator = activityLaunchAnimator;
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
        this.mAboveShelfObserver.setListener((HasViewAboveShelfChangedListener) statusBarWindowView.findViewById(R$id.notification_container_parent));
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mDozeScrimController = dozeScrimController;
        this.mScrimController = scrimController;
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mKeyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        this.mMaxAllowedKeyguardNotifications = context.getResources().getInteger(R$integer.keyguard_max_notification_count);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        IVrManager asInterface = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (asInterface != null) {
            try {
                asInterface.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to register VR mode state listener: ");
                sb.append(e);
                Slog.e("StatusBarNotificationPresenter", sb.toString());
            }
        }
        NotificationRemoteInputManager notificationRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        notificationRemoteInputManager.setUpWithCallback((Callback) Dependency.get(Callback.class), this.mNotificationPanel.createRemoteInputDelegate());
        notificationRemoteInputManager.getController().addCallback((RemoteInputController.Callback) Dependency.get(StatusBarWindowController.class));
        ((InitController) Dependency.get(InitController.class)).addPostInitTask(new Runnable((NotificationListContainer) viewGroup, notificationRemoteInputManager, notificationRowBinderImpl) {
            private final /* synthetic */ NotificationListContainer f$1;
            private final /* synthetic */ NotificationRemoteInputManager f$2;
            private final /* synthetic */ NotificationRowBinderImpl f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                StatusBarNotificationPresenter.this.lambda$new$0$StatusBarNotificationPresenter(this.f$1, this.f$2, this.f$3);
            }
        });
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        notificationAlertingManager.setHeadsUpManager(this.mHeadsUpManager);
    }

    public /* synthetic */ void lambda$new$0$StatusBarNotificationPresenter(NotificationListContainer notificationListContainer, NotificationRemoteInputManager notificationRemoteInputManager, NotificationRowBinderImpl notificationRowBinderImpl) {
        C15531 r0 = new NotificationEntryListener() {
            public void onNotificationAdded(NotificationEntry notificationEntry) {
                StatusBarNotificationPresenter.this.mShadeController.updateAreThereNotifications();
            }

            public void onPostEntryUpdated(NotificationEntry notificationEntry) {
                StatusBarNotificationPresenter.this.mShadeController.updateAreThereNotifications();
            }

            public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
                StatusBarNotificationPresenter.this.onNotificationRemoved(notificationEntry.key, notificationEntry.notification);
                if (z) {
                    StatusBarNotificationPresenter.this.maybeEndAmbientPulse();
                }
            }
        };
        this.mViewHierarchyManager.setUpWithPresenter(this, notificationListContainer);
        this.mEntryManager.setUpWithPresenter(this, notificationListContainer, this.mHeadsUpManager);
        this.mEntryManager.addNotificationEntryListener(r0);
        this.mEntryManager.addNotificationLifetimeExtender(this.mHeadsUpManager);
        this.mEntryManager.addNotificationLifetimeExtender(this.mAmbientPulseManager);
        this.mEntryManager.addNotificationLifetimeExtender(this.mGutsManager);
        this.mEntryManager.addNotificationLifetimeExtenders(notificationRemoteInputManager.getLifetimeExtenders());
        notificationRowBinderImpl.setUpWithPresenter(this, notificationListContainer, this.mHeadsUpManager, this.mEntryManager, this);
        this.mNotificationInterruptionStateProvider.setUpWithPresenter(this, this.mHeadsUpManager, new HeadsUpSuppressor() {
            public final boolean canHeadsUp(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification) {
                return StatusBarNotificationPresenter.this.canHeadsUp(notificationEntry, statusBarNotification);
            }
        });
        this.mLockscreenUserManager.setUpWithPresenter(this);
        this.mMediaManager.setUpWithPresenter(this);
        this.mVisualStabilityManager.setUpWithPresenter(this);
        this.mGutsManager.setUpWithPresenter(this, notificationListContainer, this.mCheckSaveListener, this.mOnSettingsClickListener);
        Dependency.get(ForegroundServiceNotificationListener.class);
        onUserSwitched(this.mLockscreenUserManager.getCurrentUserId());
    }

    public void onDensityOrFontScaleChanged() {
        MessagingMessage.dropCache();
        MessagingGroup.dropCache();
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSwitchingUser()) {
            updateNotificationsOnDensityOrFontScaleChanged();
        } else {
            this.mReinflateNotificationsOnUserSwitched = true;
        }
    }

    public void onUiModeChanged() {
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSwitchingUser()) {
            updateNotificationOnUiModeChanged();
        } else {
            this.mDispatchUiModeChangeOnUserSwitched = true;
        }
    }

    public void onOverlayChanged() {
        onDensityOrFontScaleChanged();
    }

    private void updateNotificationOnUiModeChanged() {
        ArrayList notificationsForCurrentUser = this.mEntryManager.getNotificationData().getNotificationsForCurrentUser();
        for (int i = 0; i < notificationsForCurrentUser.size(); i++) {
            ExpandableNotificationRow row = ((NotificationEntry) notificationsForCurrentUser.get(i)).getRow();
            if (row != null) {
                row.onUiModeChanged();
            }
        }
    }

    private void updateNotificationsOnDensityOrFontScaleChanged() {
        ArrayList notificationsForCurrentUser = this.mEntryManager.getNotificationData().getNotificationsForCurrentUser();
        for (int i = 0; i < notificationsForCurrentUser.size(); i++) {
            NotificationEntry notificationEntry = (NotificationEntry) notificationsForCurrentUser.get(i);
            notificationEntry.onDensityOrFontScaleChanged();
            if (notificationEntry.areGutsExposed()) {
                this.mGutsManager.onDensityOrFontScaleChanged(notificationEntry);
            }
        }
        this.mOpNotificationController.updateSimpleHeadsUps();
    }

    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing() || this.mActivityLaunchAnimator.isAnimationPending() || this.mActivityLaunchAnimator.isAnimationRunning();
    }

    /* access modifiers changed from: private */
    public void maybeEndAmbientPulse() {
        if (this.mNotificationPanel.hasPulsingNotifications() && !this.mAmbientPulseManager.hasNotifications()) {
            this.mDozeScrimController.pulseOutNow();
        }
    }

    public void updateNotificationViews() {
        if (this.mScrimController != null) {
            if (isCollapsing()) {
                this.mShadeController.addPostCollapseAction(new Runnable() {
                    public final void run() {
                        StatusBarNotificationPresenter.this.updateNotificationViews();
                    }
                });
                return;
            }
            this.mViewHierarchyManager.updateNotificationViews();
            this.mNotificationPanel.updateNotificationViews();
        }
    }

    public void onNotificationRemoved(String str, StatusBarNotification statusBarNotification) {
        if (statusBarNotification != null && !hasActiveNotifications() && !this.mNotificationPanel.isTracking() && !this.mNotificationPanel.isQsExpanded()) {
            if (this.mStatusBarStateController.getState() == 0) {
                this.mCommandQueue.animateCollapsePanels();
            } else if (this.mStatusBarStateController.getState() == 2 && !isCollapsing()) {
                this.mShadeController.goToKeyguard();
            }
        }
        this.mShadeController.updateAreThereNotifications();
    }

    public boolean hasActiveNotifications() {
        return !this.mEntryManager.getNotificationData().getActiveNotifications().isEmpty();
    }

    public boolean canHeadsUp(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification) {
        String str = "StatusBarNotificationPresenter";
        boolean z = false;
        if (this.mShadeController.isDozing() || this.mShadeController.isDozingCustom()) {
            if (OpUtils.DEBUG_ONEPLUS) {
                Log.d(str, "No heads up: system is in dozing state");
            }
            return false;
        }
        if (this.mShadeController.isOccluded()) {
            NotificationLockscreenUserManager notificationLockscreenUserManager = this.mLockscreenUserManager;
            boolean z2 = notificationLockscreenUserManager.isLockscreenPublicMode(notificationLockscreenUserManager.getCurrentUserId()) || this.mLockscreenUserManager.isLockscreenPublicMode(statusBarNotification.getUserId());
            boolean needsRedaction = this.mLockscreenUserManager.needsRedaction(notificationEntry);
            if (z2 && needsRedaction) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    Log.d(str, "No heads up: lockscreen is in public mode or needs redaction");
                }
                return false;
            }
        }
        if (!this.mCommandQueue.panelsEnabled() && !this.mOpNotificationController.isPanelDisabledInBrickMode()) {
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("No heads up: disabled panel : ");
                sb.append(statusBarNotification.getKey());
                Log.d(str, sb.toString());
            }
            return false;
        } else if (statusBarNotification.getNotification().fullScreenIntent == null) {
            return true;
        } else {
            if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("No heads up: accessible fullscreen: ");
                    sb2.append(statusBarNotification.getKey());
                    Log.d(str, sb2.toString());
                }
                return false;
            }
            if (!this.mKeyguardMonitor.isShowing() || this.mShadeController.isOccluded()) {
                z = true;
            }
            if (!z && OpUtils.DEBUG_ONEPLUS) {
                Log.d(str, "No heads up: lockscreen is showing and not occluded");
            }
            return z;
        }
    }

    public void onUserSwitched(int i) {
        this.mHeadsUpManager.setUser(i);
        this.mCommandQueue.animateCollapsePanels();
        if (this.mReinflateNotificationsOnUserSwitched) {
            updateNotificationsOnDensityOrFontScaleChanged();
            this.mReinflateNotificationsOnUserSwitched = false;
        }
        if (this.mDispatchUiModeChangeOnUserSwitched) {
            updateNotificationOnUiModeChanged();
            this.mDispatchUiModeChangeOnUserSwitched = false;
        }
        updateNotificationViews();
        this.mMediaManager.clearCurrentMediaNotification();
        this.mShadeController.setLockscreenUser(i);
        updateMediaMetaData(true, false);
        this.mOpNotificationController.updateNotificationRule();
    }

    public void onBindRow(NotificationEntry notificationEntry, PackageManager packageManager, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setAboveShelfChangedListener(this.mAboveShelfObserver);
        UnlockMethodCache unlockMethodCache = this.mUnlockMethodCache;
        Objects.requireNonNull(unlockMethodCache);
        expandableNotificationRow.setSecureStateProvider(new BooleanSupplier() {
            public final boolean getAsBoolean() {
                return UnlockMethodCache.this.canSkipBouncer();
            }
        });
    }

    public boolean isPresenterFullyCollapsed() {
        return this.mNotificationPanel.isFullyCollapsed();
    }

    public void onActivated(ActivatableNotificationView activatableNotificationView) {
        onActivated();
        if (activatableNotificationView != null) {
            this.mNotificationPanel.setActivatedChild(activatableNotificationView);
        }
    }

    public void onActivated() {
        this.mLockscreenGestureLogger.write(192, 0, 0);
        this.mNotificationPanel.showTransientIndication(R$string.notification_tap_again);
        ActivatableNotificationView activatedChild = this.mNotificationPanel.getActivatedChild();
        if (activatedChild != null) {
            activatedChild.makeInactive(true);
        }
    }

    public void onActivationReset(ActivatableNotificationView activatableNotificationView) {
        if (activatableNotificationView == this.mNotificationPanel.getActivatedChild()) {
            this.mNotificationPanel.setActivatedChild(null);
            this.mShadeController.onActivationReset();
        }
    }

    public void updateMediaMetaData(boolean z, boolean z2) {
        this.mMediaManager.updateMediaMetaData(z, z2);
    }

    public int getMaxNotificationsWhileLocked(boolean z) {
        if (!z) {
            return this.mMaxKeyguardNotifications;
        }
        this.mMaxKeyguardNotifications = Math.max(1, this.mNotificationPanel.computeMaxKeyguardNotifications(this.mMaxAllowedKeyguardNotifications));
        return this.mMaxKeyguardNotifications;
    }

    public void onUpdateRowStates() {
        this.mNotificationPanel.onUpdateRowStates();
    }

    public void onExpandClicked(NotificationEntry notificationEntry, boolean z) {
        this.mHeadsUpManager.setExpanded(notificationEntry, z);
        if (this.mStatusBarStateController.getState() == 1 && z) {
            this.mShadeController.goToLockedShade(notificationEntry.getRow());
        }
    }

    public boolean isDeviceInVrMode() {
        return this.mVrMode;
    }

    /* access modifiers changed from: private */
    public void onLockedNotificationImportanceChange(OnDismissAction onDismissAction) {
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        this.mActivityStarter.dismissKeyguardThenExecute(onDismissAction, null, true);
    }
}
