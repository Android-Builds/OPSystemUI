package com.oneplus.systemui.statusbar.notification.collection;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.widget.Chronometer;
import com.android.systemui.statusbar.notification.NotificationFilter;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.oneplus.util.OpReflectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OpNotificationData {
    private StatusBarNotification mCarModeHighlightHintNotification;
    private StatusBarNotification mHighlightHintNotification;
    private boolean mShowCarModeHighlightNotification;
    private boolean mShowHighlightNotification;

    public void filterAndSort() {
        ArrayMap entries = getEntries();
        ArrayList sortedAndFiltered = getSortedAndFiltered();
        sortedAndFiltered.clear();
        this.mShowHighlightNotification = false;
        this.mHighlightHintNotification = null;
        this.mShowCarModeHighlightNotification = false;
        this.mCarModeHighlightHintNotification = null;
        synchronized (entries) {
            int size = entries.size();
            int i = 0;
            for (int i2 = 0; i2 < size; i2++) {
                NotificationEntry notificationEntry = (NotificationEntry) entries.valueAt(i2);
                StatusBarNotification statusBarNotification = notificationEntry.notification;
                Notification notification = statusBarNotification.getNotification();
                if (notification.showOnStatusBar()) {
                    int priorityOnStatusBar = notification.getPriorityOnStatusBar();
                    if (priorityOnStatusBar == 200) {
                        this.mShowCarModeHighlightNotification = true;
                        this.mCarModeHighlightHintNotification = statusBarNotification;
                    } else if (priorityOnStatusBar > i) {
                        this.mShowHighlightNotification = true;
                        this.mHighlightHintNotification = statusBarNotification;
                        i = priorityOnStatusBar;
                    }
                }
                if (!getNotificationFilter().shouldFilterOut(notificationEntry)) {
                    sortedAndFiltered.add(notificationEntry);
                }
            }
        }
        Collections.sort(sortedAndFiltered, getRankingComparator());
    }

    public boolean isLocked(String str) {
        Ranking tmpRanking = getTmpRanking();
        if (getRankingMap() == null) {
            return false;
        }
        getRanking(str, tmpRanking);
        return tmpRanking.isLocked();
    }

    public boolean showNotification() {
        return this.mShowHighlightNotification;
    }

    public StatusBarNotification getHighlightHintNotification() {
        return this.mHighlightHintNotification;
    }

    public Chronometer getStatusBarChronometer() {
        StatusBarNotification statusBarNotification = this.mHighlightHintNotification;
        if (statusBarNotification == null) {
            return null;
        }
        return get(statusBarNotification.getKey()).statusBarChronometer;
    }

    public Chronometer getKeyguardChronometer() {
        StatusBarNotification statusBarNotification = this.mHighlightHintNotification;
        if (statusBarNotification == null) {
            return null;
        }
        return get(statusBarNotification.getKey()).keyguardChronometer;
    }

    public boolean showCarModeNotification() {
        return this.mShowCarModeHighlightNotification;
    }

    public StatusBarNotification getCarModeHighlightHintNotification() {
        return this.mCarModeHighlightHintNotification;
    }

    public boolean shouldShowOnKeyguard(StatusBarNotification statusBarNotification) {
        Bundle bundle = statusBarNotification.getNotification().extras;
        if (bundle == null) {
            return true;
        }
        return bundle.getBoolean("keyguard_visible", true);
    }

    private ArrayMap<String, NotificationEntry> getEntries() {
        return (ArrayMap) OpReflectionUtils.getValue(NotificationData.class, this, "mEntries");
    }

    private NotificationFilter getNotificationFilter() {
        return (NotificationFilter) OpReflectionUtils.getValue(NotificationData.class, this, "mNotificationFilter");
    }

    private NotificationEntry get(String str) {
        return (NotificationEntry) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(NotificationData.class, "get", String.class), str);
    }

    private boolean getRanking(String str, Ranking ranking) {
        return ((Boolean) OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(NotificationData.class, "getRanking", String.class, Ranking.class), str, ranking)).booleanValue();
    }

    private Comparator<NotificationEntry> getRankingComparator() {
        return (Comparator) OpReflectionUtils.getValue(NotificationData.class, this, "mRankingComparator");
    }

    private RankingMap getRankingMap() {
        return (RankingMap) OpReflectionUtils.getValue(NotificationData.class, this, "mRankingMap");
    }

    private ArrayList<NotificationEntry> getSortedAndFiltered() {
        return (ArrayList) OpReflectionUtils.getValue(NotificationData.class, this, "mSortedAndFiltered");
    }

    private Ranking getTmpRanking() {
        return (Ranking) OpReflectionUtils.getValue(NotificationData.class, this, "mTmpRanking");
    }
}
