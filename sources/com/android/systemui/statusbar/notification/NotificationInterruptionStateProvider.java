package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Global;
import android.service.dreams.IDreamManager;
import android.service.dreams.IDreamManager.Stub;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.systemui.statusbar.notification.OpNotificationInterruptionStateProvider;

public class NotificationInterruptionStateProvider extends OpNotificationInterruptionStateProvider {
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDisableNotificationAlerts;
    private final IDreamManager mDreamManager;
    /* access modifiers changed from: private */
    public HeadsUpManager mHeadsUpManager;
    private ContentObserver mHeadsUpObserver;
    private HeadsUpSuppressor mHeadsUpSuppressor;
    private final NotificationFilter mNotificationFilter;
    private final OpNotificationController mOpNotificationController;
    private final PowerManager mPowerManager;
    private NotificationPresenter mPresenter;
    private ShadeController mShadeController;
    private final StatusBarStateController mStatusBarStateController;
    @VisibleForTesting
    protected boolean mUseHeadsUp;

    public interface HeadsUpSuppressor {
        boolean canHeadsUp(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification);
    }

    public NotificationInterruptionStateProvider(Context context) {
        this(context, (PowerManager) context.getSystemService("power"), Stub.asInterface(ServiceManager.checkService("dreams")), new AmbientDisplayConfiguration(context));
    }

    @VisibleForTesting
    protected NotificationInterruptionStateProvider(Context context, PowerManager powerManager, IDreamManager iDreamManager, AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        this.mNotificationFilter = (NotificationFilter) Dependency.get(NotificationFilter.class);
        this.mUseHeadsUp = false;
        this.mOpNotificationController = (OpNotificationController) Dependency.get(OpNotificationController.class);
        this.mContext = context;
        this.mPowerManager = powerManager;
        this.mDreamManager = iDreamManager;
        this.mAmbientDisplayConfiguration = ambientDisplayConfiguration;
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, HeadsUpManager headsUpManager, HeadsUpSuppressor headsUpSuppressor) {
        this.mPresenter = notificationPresenter;
        this.mHeadsUpManager = headsUpManager;
        this.mHeadsUpSuppressor = headsUpSuppressor;
        this.mHeadsUpObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            public void onChange(boolean z) {
                NotificationInterruptionStateProvider notificationInterruptionStateProvider = NotificationInterruptionStateProvider.this;
                boolean z2 = notificationInterruptionStateProvider.mUseHeadsUp;
                boolean z3 = false;
                if (!notificationInterruptionStateProvider.mDisableNotificationAlerts && Global.getInt(NotificationInterruptionStateProvider.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) != 0) {
                    z3 = true;
                }
                notificationInterruptionStateProvider.mUseHeadsUp = z3;
                StringBuilder sb = new StringBuilder();
                sb.append("heads up is ");
                sb.append(NotificationInterruptionStateProvider.this.mUseHeadsUp ? "enabled" : "disabled");
                String str = "InterruptionStateProvider";
                Log.d(str, sb.toString());
                boolean z4 = NotificationInterruptionStateProvider.this.mUseHeadsUp;
                if (z2 != z4 && !z4) {
                    Log.d(str, "dismissing any existing heads up notification on disable event");
                    NotificationInterruptionStateProvider.this.mHeadsUpManager.releaseAllImmediately();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mHeadsUpObserver.onChange(true);
    }

    private ShadeController getShadeController() {
        if (this.mShadeController == null) {
            this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        }
        return this.mShadeController;
    }

    public boolean shouldBubbleUp(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (!notificationEntry.canBubble || !notificationEntry.isBubble()) {
            return false;
        }
        Notification notification = statusBarNotification.getNotification();
        if (notification.getBubbleMetadata() == null || notification.getBubbleMetadata().getIntent() == null || !canHeadsUpCommon(notificationEntry)) {
            return false;
        }
        return true;
    }

    public boolean shouldHeadsUp(NotificationEntry notificationEntry) {
        boolean z;
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        String str = "InterruptionStateProvider";
        if (getShadeController().isDozing() || getShadeController().isDozingCustom()) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No heads up: device is dozing: ");
                sb.append(statusBarNotification.getKey());
                Log.d(str, sb.toString());
            }
            return false;
        }
        boolean z2 = true;
        boolean z3 = this.mStatusBarStateController.getState() == 0;
        if (notificationEntry.isBubble() && z3) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("No heads up: in unlocked shade where notification is shown as a bubble: ");
                sb2.append(statusBarNotification.getKey());
                Log.d(str, sb2.toString());
            }
            return false;
        } else if (!canAlertCommon(notificationEntry)) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("No heads up: notification shouldn't alert: ");
                sb3.append(statusBarNotification.getKey());
                Log.d(str, sb3.toString());
            }
            return false;
        } else if (!canHeadsUpCommon(notificationEntry)) {
            return false;
        } else {
            if (notificationEntry.importance < 4) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("No heads up: unimportant notification: ");
                    sb4.append(statusBarNotification.getKey());
                    Log.d(str, sb4.toString());
                }
                return false;
            }
            try {
                z = this.mDreamManager.isDreaming();
            } catch (RemoteException e) {
                Log.e(str, "Failed to query dream manager.", e);
                z = false;
            }
            if (!this.mPowerManager.isScreenOn() || z) {
                z2 = false;
            }
            if (!z2) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append("No heads up: not in use: ");
                    sb5.append(statusBarNotification.getKey());
                    Log.d(str, sb5.toString());
                }
                return false;
            } else if (!this.mHeadsUpSuppressor.canHeadsUp(notificationEntry, statusBarNotification)) {
                return false;
            } else {
                return super.shouldHeadsUp(statusBarNotification);
            }
        }
    }

    public boolean shouldPulse(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        String str = "InterruptionStateProvider";
        if (!this.mAmbientDisplayConfiguration.pulseOnNotificationEnabled(-2)) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No pulsing: disabled by setting: ");
                sb.append(statusBarNotification.getKey());
                Log.d(str, sb.toString());
            }
            return false;
        } else if (!getShadeController().isDozing() && !getShadeController().isDozingCustom()) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("No pulsing: not dozing: ");
                sb2.append(statusBarNotification.getKey());
                Log.d(str, sb2.toString());
            }
            return false;
        } else if (!canAlertCommon(notificationEntry)) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("No pulsing: notification shouldn't alert: ");
                sb3.append(statusBarNotification.getKey());
                Log.d(str, sb3.toString());
            }
            return false;
        } else if (notificationEntry.shouldSuppressAmbient()) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("No pulsing: ambient effect suppressed: ");
                sb4.append(statusBarNotification.getKey());
                Log.d(str, sb4.toString());
            }
            return false;
        } else if (notificationEntry.importance < 3) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("No pulsing: not important enough: ");
                sb5.append(statusBarNotification.getKey());
                Log.d(str, sb5.toString());
            }
            return false;
        } else {
            Bundle bundle = statusBarNotification.getNotification().extras;
            CharSequence charSequence = bundle.getCharSequence("android.title");
            CharSequence charSequence2 = bundle.getCharSequence("android.text");
            if (TextUtils.isEmpty(charSequence) && TextUtils.isEmpty(charSequence2)) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb6 = new StringBuilder();
                    sb6.append("No pulsing: title and text are empty: ");
                    sb6.append(statusBarNotification.getKey());
                    Log.d(str, sb6.toString());
                }
                return false;
            } else if (statusBarNotification.getNotification().fullScreenIntent != null) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb7 = new StringBuilder();
                    sb7.append("No pulsing: notification with fullScreenIntent: ");
                    sb7.append(statusBarNotification.getKey());
                    Log.d(str, sb7.toString());
                }
                return false;
            } else {
                if (!"com.android.dialer".equals(statusBarNotification.getPackageName()) || notificationEntry.importance >= 4 || this.mOpNotificationController.getCallState() != 2) {
                    return true;
                }
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb8 = new StringBuilder();
                    sb8.append("No pulsing: dialer off hook and not important enough: ");
                    sb8.append(statusBarNotification.getKey());
                    Log.d(str, sb8.toString());
                }
                return false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean canAlertCommon(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        boolean shouldFilterOut = this.mNotificationFilter.shouldFilterOut(notificationEntry);
        String str = "InterruptionStateProvider";
        if (shouldFilterOut) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No alerting: filtered notification: ");
                sb.append(statusBarNotification.getKey());
                Log.d(str, sb.toString());
            }
            return false;
        } else if (!statusBarNotification.isGroup() || !statusBarNotification.getNotification().suppressAlertingDueToGrouping()) {
            return true;
        } else {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                Log.d(str, "No alerting: suppressed due to group alert behavior");
            }
            return false;
        }
    }

    public boolean canHeadsUpCommon(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        String str = "InterruptionStateProvider";
        if (!this.mUseHeadsUp || this.mPresenter.isDeviceInVrMode()) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                Log.d(str, "No heads up: no huns or vr mode");
            }
            return false;
        } else if (notificationEntry.shouldSuppressPeek()) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("No heads up: suppressed by DND: ");
                sb.append(statusBarNotification.getKey());
                Log.d(str, sb.toString());
            }
            return false;
        } else if (isSnoozedPackage(statusBarNotification) && !this.mOpNotificationController.canHeadsUpSnoozedNotification(statusBarNotification)) {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("No heads up: snoozed package: ");
                sb2.append(statusBarNotification.getKey());
                Log.d(str, sb2.toString());
            }
            return false;
        } else if (!notificationEntry.hasJustLaunchedFullScreenIntent() && (this.mOpNotificationController.getEntryManager() == null || !this.mOpNotificationController.getEntryManager().mCachedNotifications.containsKey(statusBarNotification.getKey()) || !((NotificationEntry) this.mOpNotificationController.getEntryManager().mCachedNotifications.get(statusBarNotification.getKey())).hasJustLaunchedFullScreenIntent())) {
            return true;
        } else {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("No heads up: recent fullscreen: ");
                sb3.append(statusBarNotification.getKey());
                Log.d(str, sb3.toString());
            }
            return false;
        }
    }

    private boolean isSnoozedPackage(StatusBarNotification statusBarNotification) {
        return this.mHeadsUpManager.isSnoozed(statusBarNotification.getPackageName());
    }

    public void setDisableNotificationAlerts(boolean z) {
        this.mDisableNotificationAlerts = z;
        this.mHeadsUpObserver.onChange(true);
    }

    public int shouldHeadsUpWithReason(NotificationEntry notificationEntry) {
        boolean z;
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        String str = "InterruptionStateProvider";
        if (!getShadeController().isDozing() || !getShadeController().isDozingCustom()) {
            boolean z2 = true;
            boolean z3 = this.mStatusBarStateController.getState() == 0;
            if (notificationEntry.isBubble() && z3) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("No heads up: in unlocked shade where notification is shown as a bubble: ");
                    sb.append(statusBarNotification.getKey());
                    Log.d(str, sb.toString());
                }
                return 0;
            } else if (!canAlertCommon(notificationEntry)) {
                if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("No heads up: notification shouldn't alert: ");
                    sb2.append(statusBarNotification.getKey());
                    Log.d(str, sb2.toString());
                }
                return 0;
            } else if (!canHeadsUpCommon(notificationEntry)) {
                return 0;
            } else {
                if (notificationEntry.importance < 4) {
                    if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("No heads up: unimportant notification: ");
                        sb3.append(statusBarNotification.getKey());
                        Log.d(str, sb3.toString());
                    }
                    return 0;
                }
                try {
                    z = this.mDreamManager.isDreaming();
                } catch (RemoteException e) {
                    Log.e(str, "Failed to query dream manager.", e);
                    z = false;
                }
                if (!this.mPowerManager.isScreenOn() || z) {
                    z2 = false;
                }
                if (!z2) {
                    if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("No heads up: not in use: ");
                        sb4.append(statusBarNotification.getKey());
                        Log.d(str, sb4.toString());
                    }
                    return 0;
                } else if (!this.mHeadsUpSuppressor.canHeadsUp(notificationEntry, statusBarNotification)) {
                    return 0;
                } else {
                    return this.mOpNotificationController.canHeadsUp(statusBarNotification);
                }
            }
        } else {
            if (OpNotificationInterruptionStateProvider.OP_DEBUG) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("No heads up: device is dozing: ");
                sb5.append(statusBarNotification.getKey());
                Log.d(str, sb5.toString());
            }
            return 0;
        }
    }
}
