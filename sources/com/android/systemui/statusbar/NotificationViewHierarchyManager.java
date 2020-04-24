package com.android.systemui.statusbar;

import android.content.Context;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R$bool;
import com.android.systemui.bubbles.BubbleData;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController.Listener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.util.Assert;
import com.oneplus.systemui.statusbar.OpNotificationViewHierarchyManager;
import dagger.Lazy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class NotificationViewHierarchyManager extends OpNotificationViewHierarchyManager implements Listener {
    private final boolean mAlwaysExpandNonGroupedNotification;
    private final BubbleData mBubbleData;
    private final DynamicPrivacyController mDynamicPrivacyController;
    private final NotificationEntryManager mEntryManager;
    protected final NotificationGroupManager mGroupManager;
    private final Handler mHandler;
    private boolean mIsHandleDynamicPrivacyChangeScheduled;
    private NotificationListContainer mListContainer;
    protected final NotificationLockscreenUserManager mLockscreenUserManager;
    private boolean mPerformingUpdate;
    private NotificationPresenter mPresenter;
    private final Lazy<ShadeController> mShadeController;
    private final SysuiStatusBarStateController mStatusBarStateController;
    private final HashMap<ExpandableNotificationRow, List<ExpandableNotificationRow>> mTmpChildOrderMap = new HashMap<>();
    protected final VisualStabilityManager mVisualStabilityManager;

    public NotificationViewHierarchyManager(Context context, Handler handler, NotificationLockscreenUserManager notificationLockscreenUserManager, NotificationGroupManager notificationGroupManager, VisualStabilityManager visualStabilityManager, StatusBarStateController statusBarStateController, NotificationEntryManager notificationEntryManager, Lazy<ShadeController> lazy, BubbleData bubbleData, DynamicPrivacyController dynamicPrivacyController) {
        this.mHandler = handler;
        this.mLockscreenUserManager = notificationLockscreenUserManager;
        this.mGroupManager = notificationGroupManager;
        this.mVisualStabilityManager = visualStabilityManager;
        this.mStatusBarStateController = (SysuiStatusBarStateController) statusBarStateController;
        this.mEntryManager = notificationEntryManager;
        this.mShadeController = lazy;
        this.mAlwaysExpandNonGroupedNotification = context.getResources().getBoolean(R$bool.config_alwaysExpandNonGroupedNotifications);
        this.mBubbleData = bubbleData;
        this.mDynamicPrivacyController = dynamicPrivacyController;
        dynamicPrivacyController.addListener(this);
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationListContainer notificationListContainer) {
        this.mPresenter = notificationPresenter;
        this.mListContainer = notificationListContainer;
    }

    public void updateNotificationViews() {
        Assert.isMainThread();
        beginUpdate();
        ArrayList activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
        ArrayList arrayList = new ArrayList(activeNotifications.size());
        setAnyNotificationLocked(false);
        int size = activeNotifications.size();
        for (int i = 0; i < size; i++) {
            NotificationEntry notificationEntry = (NotificationEntry) activeNotifications.get(i);
            if (!notificationEntry.isRowDismissed() && !notificationEntry.isRowRemoved() && (!this.mBubbleData.hasBubbleWithKey(notificationEntry.key) || notificationEntry.showInShadeWhenBubble())) {
                int userId = notificationEntry.notification.getUserId();
                int currentUserId = this.mLockscreenUserManager.getCurrentUserId();
                boolean isLockscreenPublicMode = this.mLockscreenUserManager.isLockscreenPublicMode(currentUserId);
                boolean z = isLockscreenPublicMode || this.mLockscreenUserManager.isLockscreenPublicMode(userId);
                if (z && this.mDynamicPrivacyController.isDynamicallyUnlocked() && (userId == currentUserId || userId == -1 || !this.mLockscreenUserManager.needsSeparateWorkChallenge(userId))) {
                    z = false;
                }
                boolean z2 = this.mLockscreenUserManager.isSecure() && this.mEntryManager.getNotificationData().isLocked(notificationEntry.key);
                boolean z3 = this.mLockscreenUserManager.needsRedaction(notificationEntry) || z2;
                notificationEntry.getRow().setSensitive((z || z2) && z3, (isLockscreenPublicMode && !this.mLockscreenUserManager.userAllowsPrivateNotificationsInPublic(currentUserId)) || z2);
                notificationEntry.getRow().setNeedsRedaction(z3 || z2);
                notificationEntry.getRow().setContentHidden(z2);
                if (z2) {
                    setAnyNotificationLocked(true);
                }
                if (this.mGroupManager.isChildInGroupWithSummary(notificationEntry.notification)) {
                    NotificationEntry groupSummary = this.mGroupManager.getGroupSummary(notificationEntry.notification);
                    List list = (List) this.mTmpChildOrderMap.get(groupSummary.getRow());
                    if (list == null) {
                        list = new ArrayList();
                        this.mTmpChildOrderMap.put(groupSummary.getRow(), list);
                    }
                    list.add(notificationEntry.getRow());
                } else {
                    arrayList.add(notificationEntry.getRow());
                }
            }
        }
        ArrayList arrayList2 = new ArrayList();
        for (int i2 = 0; i2 < this.mListContainer.getContainerChildCount(); i2++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i2);
            if (!arrayList.contains(containerChildAt) && (containerChildAt instanceof ExpandableNotificationRow)) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                if (!expandableNotificationRow.isBlockingHelperShowing()) {
                    arrayList2.add(expandableNotificationRow);
                }
            }
        }
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) it.next();
            if (this.mGroupManager.isChildInGroupWithSummary(expandableNotificationRow2.getStatusBarNotification())) {
                this.mListContainer.setChildTransferInProgress(true);
            }
            if (expandableNotificationRow2.isSummaryWithChildren()) {
                expandableNotificationRow2.removeAllChildren();
            }
            this.mListContainer.removeContainerView(expandableNotificationRow2);
            this.mListContainer.setChildTransferInProgress(false);
        }
        removeNotificationChildren();
        int i3 = 0;
        while (i3 < arrayList.size()) {
            View view = (View) arrayList.get(i3);
            if (view.getParent() == null) {
                this.mVisualStabilityManager.notifyViewAddition(view);
                this.mListContainer.addContainerView(view);
            } else if (!this.mListContainer.containsView(view)) {
                arrayList.remove(view);
                i3--;
            }
            i3++;
        }
        addNotificationChildrenAndSort();
        int i4 = 0;
        for (int i5 = 0; i5 < this.mListContainer.getContainerChildCount(); i5++) {
            View containerChildAt2 = this.mListContainer.getContainerChildAt(i5);
            if ((containerChildAt2 instanceof ExpandableNotificationRow) && !((ExpandableNotificationRow) containerChildAt2).isBlockingHelperShowing()) {
                ExpandableNotificationRow expandableNotificationRow3 = (ExpandableNotificationRow) arrayList.get(i4);
                if (containerChildAt2 != expandableNotificationRow3) {
                    if (this.mVisualStabilityManager.canReorderNotification(expandableNotificationRow3)) {
                        this.mListContainer.changeViewPosition(expandableNotificationRow3, i5);
                    } else {
                        this.mVisualStabilityManager.addReorderingAllowedCallback(this.mEntryManager);
                    }
                }
                i4++;
            }
        }
        this.mVisualStabilityManager.onReorderingFinished();
        this.mTmpChildOrderMap.clear();
        updateRowStatesInternal();
        this.mListContainer.onNotificationViewUpdateFinished();
        endUpdate();
    }

    private void addNotificationChildrenAndSort() {
        boolean z = false;
        for (int i = 0; i < this.mListContainer.getContainerChildCount(); i++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                List notificationChildren = expandableNotificationRow.getNotificationChildren();
                List list = (List) this.mTmpChildOrderMap.get(expandableNotificationRow);
                int i2 = 0;
                while (list != null && i2 < list.size()) {
                    ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) list.get(i2);
                    if (notificationChildren == null || !notificationChildren.contains(expandableNotificationRow2)) {
                        if (expandableNotificationRow2.getParent() != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("trying to add a notification child that already has a parent. class:");
                            sb.append(expandableNotificationRow2.getParent().getClass());
                            sb.append("\n child: ");
                            sb.append(expandableNotificationRow2);
                            Log.wtf("NotificationViewHierarchyManager", sb.toString());
                            ((ViewGroup) expandableNotificationRow2.getParent()).removeView(expandableNotificationRow2);
                        }
                        this.mVisualStabilityManager.notifyViewAddition(expandableNotificationRow2);
                        expandableNotificationRow.addChildNotification(expandableNotificationRow2, i2);
                        this.mListContainer.notifyGroupChildAdded(expandableNotificationRow2);
                    }
                    i2++;
                }
                z |= expandableNotificationRow.applyChildOrder(list, this.mVisualStabilityManager, this.mEntryManager);
            }
        }
        if (z) {
            this.mListContainer.generateChildOrderChangedEvent();
        }
    }

    private void removeNotificationChildren() {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mListContainer.getContainerChildCount(); i++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                List list = (List) this.mTmpChildOrderMap.get(expandableNotificationRow);
                if (notificationChildren != null) {
                    arrayList.clear();
                    for (ExpandableNotificationRow expandableNotificationRow2 : notificationChildren) {
                        if ((list == null || !list.contains(expandableNotificationRow2)) && !expandableNotificationRow2.keepInParent()) {
                            arrayList.add(expandableNotificationRow2);
                        }
                    }
                    Iterator it = arrayList.iterator();
                    while (it.hasNext()) {
                        ExpandableNotificationRow expandableNotificationRow3 = (ExpandableNotificationRow) it.next();
                        expandableNotificationRow.removeChildNotification(expandableNotificationRow3);
                        if (this.mEntryManager.getNotificationData().get(expandableNotificationRow3.getStatusBarNotification().getKey()) == null) {
                            this.mListContainer.notifyGroupChildRemoved(expandableNotificationRow3, expandableNotificationRow.getChildrenContainer());
                        }
                    }
                }
            }
        }
    }

    public void updateRowStates() {
        Assert.isMainThread();
        beginUpdate();
        updateRowStatesInternal();
        endUpdate();
    }

    private void updateRowStatesInternal() {
        Trace.beginSection("NotificationViewHierarchyManager#updateRowStates");
        int containerChildCount = this.mListContainer.getContainerChildCount();
        boolean z = this.mStatusBarStateController.getState() == 1;
        this.mListContainer.setMaxDisplayedNotifications(z ? this.mPresenter.getMaxNotificationsWhileLocked(true) : -1);
        Stack stack = new Stack();
        for (int i = containerChildCount - 1; i >= 0; i--) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                stack.push((ExpandableNotificationRow) containerChildAt);
            }
        }
        int i2 = 0;
        while (!stack.isEmpty()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) stack.pop();
            NotificationEntry entry = expandableNotificationRow.getEntry();
            boolean isChildInGroupWithSummary = this.mGroupManager.isChildInGroupWithSummary(entry.notification);
            expandableNotificationRow.setOnKeyguard(z);
            if (!z) {
                expandableNotificationRow.setSystemExpanded(this.mAlwaysExpandNonGroupedNotification || ((i2 == 0 || entry.notification.getNotification().extras.getBoolean("op_always_expanded")) && !isChildInGroupWithSummary && !expandableNotificationRow.isLowPriority()));
            }
            entry.getRow().setOnAmbient(((ShadeController) this.mShadeController.get()).isDozing());
            int userId = entry.notification.getUserId();
            boolean z2 = this.mGroupManager.isSummaryOfSuppressedGroup(entry.notification) && !entry.isRowRemoved();
            boolean shouldShowOnKeyguard = this.mLockscreenUserManager.shouldShowOnKeyguard(entry.notification);
            if (!shouldShowOnKeyguard && this.mGroupManager.isChildInGroupWithSummary(entry.notification)) {
                NotificationEntry logicalGroupSummary = this.mGroupManager.getLogicalGroupSummary(entry.notification);
                if (logicalGroupSummary != null && this.mLockscreenUserManager.shouldShowOnKeyguard(logicalGroupSummary.notification)) {
                    shouldShowOnKeyguard = true;
                }
            }
            if (z2 || this.mLockscreenUserManager.shouldHideNotifications(userId) || (z && !shouldShowOnKeyguard)) {
                entry.getRow().setVisibility(8);
            } else {
                boolean z3 = entry.getRow().getVisibility() == 8;
                if (z3) {
                    entry.getRow().setVisibility(0);
                }
                if (!isChildInGroupWithSummary && !entry.getRow().isRemoved()) {
                    if (z3) {
                        this.mListContainer.generateAddAnimation(entry.getRow(), !shouldShowOnKeyguard);
                    }
                    i2++;
                }
            }
            if (expandableNotificationRow.isSummaryWithChildren()) {
                List notificationChildren = expandableNotificationRow.getNotificationChildren();
                for (int size = notificationChildren.size() - 1; size >= 0; size--) {
                    stack.push((ExpandableNotificationRow) notificationChildren.get(size));
                }
            }
            expandableNotificationRow.showAppOpsIcons(entry.mActiveAppOps);
            expandableNotificationRow.setLastAudiblyAlertedMs(entry.lastAudiblyAlertedMs);
        }
        Trace.beginSection("NotificationPresenter#onUpdateRowStates");
        this.mPresenter.onUpdateRowStates();
        Trace.endSection();
        Trace.endSection();
    }

    public void onDynamicPrivacyChanged() {
        if (this.mPerformingUpdate) {
            Log.w("NotificationViewHierarchyManager", "onDynamicPrivacyChanged made a re-entrant call");
        }
        if (!this.mIsHandleDynamicPrivacyChangeScheduled) {
            this.mIsHandleDynamicPrivacyChangeScheduled = true;
            this.mHandler.post(new Runnable() {
                public final void run() {
                    NotificationViewHierarchyManager.this.onHandleDynamicPrivacyChanged();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void onHandleDynamicPrivacyChanged() {
        this.mIsHandleDynamicPrivacyChangeScheduled = false;
        updateNotificationViews();
    }

    private void beginUpdate() {
        if (this.mPerformingUpdate) {
            Log.wtf("NotificationViewHierarchyManager", "Re-entrant code during update", new Exception());
        }
        this.mPerformingUpdate = true;
    }

    private void endUpdate() {
        if (!this.mPerformingUpdate) {
            Log.wtf("NotificationViewHierarchyManager", "Manager state has become desynced", new Exception());
        }
        this.mPerformingUpdate = false;
    }

    public boolean shouldHideSensitive(NotificationEntry notificationEntry, int i) {
        int currentUserId = this.mLockscreenUserManager.getCurrentUserId();
        boolean isLockscreenPublicMode = this.mLockscreenUserManager.isLockscreenPublicMode(currentUserId);
        boolean z = this.mLockscreenUserManager.isSecure() && this.mEntryManager.getNotificationData().isLocked(notificationEntry.key);
        boolean z2 = isLockscreenPublicMode || z || this.mLockscreenUserManager.isLockscreenPublicMode(i);
        if (z2 && this.mDynamicPrivacyController.isDynamicallyUnlocked() && (i == currentUserId || i == -1 || !this.mLockscreenUserManager.needsSeparateWorkChallenge(i))) {
            z2 = false;
        }
        boolean z3 = z2 && (this.mLockscreenUserManager.needsRedaction(notificationEntry) || z);
        boolean z4 = isLockscreenPublicMode && !this.mLockscreenUserManager.userAllowsPrivateNotificationsInPublic(currentUserId);
        if (z3 || z4 || z) {
            return true;
        }
        return false;
    }
}
