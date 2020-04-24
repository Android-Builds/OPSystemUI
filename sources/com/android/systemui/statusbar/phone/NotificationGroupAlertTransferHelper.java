package com.android.systemui.statusbar.phone;

import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.AlertingNotificationManager;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.NotificationGroupManager.NotificationGroup;
import com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.ArrayList;
import java.util.Objects;

public class NotificationGroupAlertTransferHelper implements OnHeadsUpChangedListener, OnAmbientChangedListener, StateListener {
    private final AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    private NotificationEntryManager mEntryManager;
    /* access modifiers changed from: private */
    public final ArrayMap<String, GroupAlertEntry> mGroupAlertEntries = new ArrayMap<>();
    /* access modifiers changed from: private */
    public final NotificationGroupManager mGroupManager = ((NotificationGroupManager) Dependency.get(NotificationGroupManager.class));
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsDozing;
    private final NotificationEntryListener mNotificationEntryListener = new NotificationEntryListener() {
        public void onPendingEntryAdded(NotificationEntry notificationEntry) {
            GroupAlertEntry groupAlertEntry = (GroupAlertEntry) NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.get(NotificationGroupAlertTransferHelper.this.mGroupManager.getGroupKey(notificationEntry.notification));
            if (groupAlertEntry != null) {
                NotificationGroupAlertTransferHelper.this.checkShouldTransferBack(groupAlertEntry);
            }
        }

        public void onEntryReinflated(NotificationEntry notificationEntry) {
            PendingAlertInfo pendingAlertInfo = (PendingAlertInfo) NotificationGroupAlertTransferHelper.this.mPendingAlerts.remove(notificationEntry.key);
            if (pendingAlertInfo == null) {
                return;
            }
            if (pendingAlertInfo.isStillValid()) {
                NotificationGroupAlertTransferHelper notificationGroupAlertTransferHelper = NotificationGroupAlertTransferHelper.this;
                notificationGroupAlertTransferHelper.alertNotificationWhenPossible(notificationEntry, notificationGroupAlertTransferHelper.getActiveAlertManager());
                return;
            }
            notificationEntry.getRow().freeContentViewWhenSafe(pendingAlertInfo.mAlertManager.getContentFlag());
        }

        public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
            NotificationGroupAlertTransferHelper.this.mPendingAlerts.remove(notificationEntry.key);
        }
    };
    private final OnGroupChangeListener mOnGroupChangeListener = new OnGroupChangeListener() {
        public void onGroupCreated(NotificationGroup notificationGroup, String str) {
            NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.put(str, new GroupAlertEntry(notificationGroup));
        }

        public void onGroupRemoved(NotificationGroup notificationGroup, String str) {
            NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.remove(str);
        }

        public void onGroupSuppressionChanged(NotificationGroup notificationGroup, boolean z) {
            AlertingNotificationManager access$200 = NotificationGroupAlertTransferHelper.this.getActiveAlertManager();
            if (z) {
                if (access$200.isAlerting(notificationGroup.summary.key)) {
                    NotificationGroupAlertTransferHelper.this.handleSuppressedSummaryAlerted(notificationGroup.summary, access$200);
                }
            } else if (notificationGroup.summary != null) {
                GroupAlertEntry groupAlertEntry = (GroupAlertEntry) NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.get(NotificationGroupAlertTransferHelper.this.mGroupManager.getGroupKey(notificationGroup.summary.notification));
                if (groupAlertEntry.mAlertSummaryOnNextAddition) {
                    if (!access$200.isAlerting(notificationGroup.summary.key)) {
                        NotificationGroupAlertTransferHelper.this.alertNotificationWhenPossible(notificationGroup.summary, access$200);
                    }
                    groupAlertEntry.mAlertSummaryOnNextAddition = false;
                } else {
                    NotificationGroupAlertTransferHelper.this.checkShouldTransferBack(groupAlertEntry);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final ArrayMap<String, PendingAlertInfo> mPendingAlerts = new ArrayMap<>();

    private static class GroupAlertEntry {
        boolean mAlertSummaryOnNextAddition;
        final NotificationGroup mGroup;
        long mLastAlertTransferTime;

        GroupAlertEntry(NotificationGroup notificationGroup) {
            this.mGroup = notificationGroup;
        }
    }

    private class PendingAlertInfo {
        boolean mAbortOnInflation;
        final AlertingNotificationManager mAlertManager;
        final NotificationEntry mEntry;
        final StatusBarNotification mOriginalNotification;

        PendingAlertInfo(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
            this.mOriginalNotification = notificationEntry.notification;
            this.mEntry = notificationEntry;
            this.mAlertManager = alertingNotificationManager;
        }

        /* access modifiers changed from: private */
        public boolean isStillValid() {
            if (!this.mAbortOnInflation && this.mAlertManager == NotificationGroupAlertTransferHelper.this.getActiveAlertManager() && this.mEntry.notification.getGroupKey() == this.mOriginalNotification.getGroupKey() && this.mEntry.notification.getNotification().isGroupSummary() == this.mOriginalNotification.getNotification().isGroupSummary()) {
                return true;
            }
            return false;
        }
    }

    public void onStateChanged(int i) {
    }

    public NotificationGroupAlertTransferHelper() {
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public void bind(NotificationEntryManager notificationEntryManager, NotificationGroupManager notificationGroupManager) {
        if (this.mEntryManager == null) {
            this.mEntryManager = notificationEntryManager;
            this.mEntryManager.addNotificationEntryListener(this.mNotificationEntryListener);
            notificationGroupManager.addOnGroupChangeListener(this.mOnGroupChangeListener);
            return;
        }
        throw new IllegalStateException("Already bound.");
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void onDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            for (GroupAlertEntry groupAlertEntry : this.mGroupAlertEntries.values()) {
                groupAlertEntry.mLastAlertTransferTime = 0;
                groupAlertEntry.mAlertSummaryOnNextAddition = false;
            }
        }
        this.mIsDozing = z;
    }

    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z, this.mAmbientPulseManager);
    }

    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z, this.mHeadsUpManager);
    }

    private void onAlertStateChanged(NotificationEntry notificationEntry, boolean z, AlertingNotificationManager alertingNotificationManager) {
        if (z && this.mGroupManager.isSummaryOfSuppressedGroup(notificationEntry.notification)) {
            handleSuppressedSummaryAlerted(notificationEntry, alertingNotificationManager);
        }
    }

    private int getPendingChildrenNotAlerting(NotificationGroup notificationGroup) {
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        int i = 0;
        if (notificationEntryManager == null) {
            return 0;
        }
        for (NotificationEntry notificationEntry : notificationEntryManager.getPendingNotificationsIterator()) {
            if (isPendingNotificationInGroup(notificationEntry, notificationGroup) && onlySummaryAlerts(notificationEntry)) {
                i++;
            }
        }
        return i;
    }

    private boolean pendingInflationsWillAddChildren(NotificationGroup notificationGroup) {
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        if (notificationEntryManager == null) {
            return false;
        }
        for (NotificationEntry isPendingNotificationInGroup : notificationEntryManager.getPendingNotificationsIterator()) {
            if (isPendingNotificationInGroup(isPendingNotificationInGroup, notificationGroup)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPendingNotificationInGroup(NotificationEntry notificationEntry, NotificationGroup notificationGroup) {
        return this.mGroupManager.isGroupChild(notificationEntry.notification) && Objects.equals(this.mGroupManager.getGroupKey(notificationEntry.notification), this.mGroupManager.getGroupKey(notificationGroup.summary.notification)) && !notificationGroup.children.containsKey(notificationEntry.key);
    }

    /* access modifiers changed from: private */
    public void handleSuppressedSummaryAlerted(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        GroupAlertEntry groupAlertEntry = (GroupAlertEntry) this.mGroupAlertEntries.get(this.mGroupManager.getGroupKey(statusBarNotification));
        if (this.mGroupManager.isSummaryOfSuppressedGroup(notificationEntry.notification) && alertingNotificationManager.isAlerting(statusBarNotification.getKey()) && groupAlertEntry != null && !pendingInflationsWillAddChildren(groupAlertEntry.mGroup)) {
            NotificationEntry notificationEntry2 = (NotificationEntry) this.mGroupManager.getLogicalChildren(notificationEntry.notification).iterator().next();
            if (notificationEntry2 != null && !notificationEntry2.getRow().keepInParent() && !notificationEntry2.isRowRemoved() && !notificationEntry2.isRowDismissed()) {
                if (!alertingNotificationManager.isAlerting(notificationEntry2.key) && onlySummaryAlerts(notificationEntry)) {
                    groupAlertEntry.mLastAlertTransferTime = SystemClock.elapsedRealtime();
                }
                transferAlertState(notificationEntry, notificationEntry2, alertingNotificationManager);
            }
        }
    }

    private void transferAlertState(NotificationEntry notificationEntry, NotificationEntry notificationEntry2, AlertingNotificationManager alertingNotificationManager) {
        alertingNotificationManager.removeNotification(notificationEntry.key, true);
        alertNotificationWhenPossible(notificationEntry2, alertingNotificationManager);
    }

    /* access modifiers changed from: private */
    public void checkShouldTransferBack(GroupAlertEntry groupAlertEntry) {
        if (SystemClock.elapsedRealtime() - groupAlertEntry.mLastAlertTransferTime < 300) {
            NotificationEntry notificationEntry = groupAlertEntry.mGroup.summary;
            AlertingNotificationManager activeAlertManager = getActiveAlertManager();
            if (onlySummaryAlerts(notificationEntry)) {
                ArrayList logicalChildren = this.mGroupManager.getLogicalChildren(notificationEntry.notification);
                int size = logicalChildren.size();
                int pendingChildrenNotAlerting = getPendingChildrenNotAlerting(groupAlertEntry.mGroup);
                int i = size + pendingChildrenNotAlerting;
                if (i > 1) {
                    boolean z = false;
                    boolean z2 = false;
                    for (int i2 = 0; i2 < logicalChildren.size(); i2++) {
                        NotificationEntry notificationEntry2 = (NotificationEntry) logicalChildren.get(i2);
                        if (onlySummaryAlerts(notificationEntry2) && activeAlertManager.isAlerting(notificationEntry2.key)) {
                            activeAlertManager.removeNotification(notificationEntry2.key, true);
                            z2 = true;
                        }
                        if (this.mPendingAlerts.containsKey(notificationEntry2.key)) {
                            ((PendingAlertInfo) this.mPendingAlerts.get(notificationEntry2.key)).mAbortOnInflation = true;
                            z2 = true;
                        }
                    }
                    if (z2 && !activeAlertManager.isAlerting(notificationEntry.key)) {
                        if (i - pendingChildrenNotAlerting > 1) {
                            z = true;
                        }
                        if (z) {
                            alertNotificationWhenPossible(notificationEntry, activeAlertManager);
                        } else {
                            groupAlertEntry.mAlertSummaryOnNextAddition = true;
                        }
                        groupAlertEntry.mLastAlertTransferTime = 0;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void alertNotificationWhenPossible(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
        int contentFlag = alertingNotificationManager.getContentFlag();
        if (!notificationEntry.getRow().isInflationFlagSet(contentFlag)) {
            this.mPendingAlerts.put(notificationEntry.key, new PendingAlertInfo(notificationEntry, alertingNotificationManager));
            notificationEntry.getRow().updateInflationFlag(contentFlag, true);
            notificationEntry.getRow().inflateViews();
            return;
        }
        if (alertingNotificationManager.isAlerting(notificationEntry.key)) {
            alertingNotificationManager.updateNotification(notificationEntry.key, true);
        } else {
            alertingNotificationManager.showNotification(notificationEntry);
        }
    }

    /* access modifiers changed from: private */
    public AlertingNotificationManager getActiveAlertManager() {
        return this.mIsDozing ? this.mAmbientPulseManager : this.mHeadsUpManager;
    }

    private boolean onlySummaryAlerts(NotificationEntry notificationEntry) {
        return notificationEntry != null && notificationEntry.notification.getNotification().getGroupAlertBehavior() == 1;
    }
}
