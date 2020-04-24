package com.android.systemui.statusbar.notification.collection;

import android.app.Notification;
import android.app.Notification.MessagingStyle;
import android.app.NotificationChannel;
import android.os.Bundle;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.notification.NotificationFilter;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.oneplus.systemui.statusbar.notification.collection.OpNotificationData;
import com.oneplus.worklife.OPWLBHelper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

public class NotificationData extends OpNotificationData {
    private final ArrayMap<String, NotificationEntry> mEntries = new ArrayMap<>();
    private KeyguardEnvironment mEnvironment;
    private final ArrayList<NotificationEntry> mFilteredForUser = new ArrayList<>();
    private final NotificationGroupManager mGroupManager = ((NotificationGroupManager) Dependency.get(NotificationGroupManager.class));
    /* access modifiers changed from: private */
    public HeadsUpManager mHeadsUpManager;
    private NotificationMediaManager mMediaManager;
    private final NotificationFilter mNotificationFilter = ((NotificationFilter) Dependency.get(NotificationFilter.class));
    @VisibleForTesting
    protected final Comparator<NotificationEntry> mRankingComparator = new Comparator<NotificationEntry>() {
        private final Ranking mRankingA = new Ranking();
        private final Ranking mRankingB = new Ranking();

        public int compare(NotificationEntry notificationEntry, NotificationEntry notificationEntry2) {
            int i;
            int i2;
            int i3;
            NotificationEntry notificationEntry3 = notificationEntry;
            NotificationEntry notificationEntry4 = notificationEntry2;
            StatusBarNotification statusBarNotification = notificationEntry3.notification;
            StatusBarNotification statusBarNotification2 = notificationEntry4.notification;
            int i4 = 3;
            if (NotificationData.this.mRankingMap != null) {
                NotificationData.this.getRanking(notificationEntry3.key, this.mRankingA);
                NotificationData.this.getRanking(notificationEntry4.key, this.mRankingB);
                i4 = this.mRankingA.getImportance();
                i3 = this.mRankingB.getImportance();
                i2 = this.mRankingA.getRank();
                i = this.mRankingB.getRank();
            } else {
                i3 = 3;
                i2 = 0;
                i = 0;
            }
            String mediaNotificationKey = NotificationData.this.getMediaManager().getMediaNotificationKey();
            boolean z = notificationEntry3.key.equals(mediaNotificationKey) && i4 > 1;
            boolean z2 = notificationEntry4.key.equals(mediaNotificationKey) && i3 > 1;
            boolean z3 = i4 >= 4 && NotificationData.isSystemNotification(statusBarNotification);
            boolean z4 = i3 >= 4 && NotificationData.isSystemNotification(statusBarNotification2);
            boolean isHeadsUp = notificationEntry.getRow().isHeadsUp();
            int i5 = -1;
            String str = "NotificationData";
            if (isHeadsUp != notificationEntry2.getRow().isHeadsUp()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Comparator, a.isHeadsUp?");
                sb.append(notificationEntry.getRow().isHeadsUp());
                sb.append(", b.isHeadsUp?");
                sb.append(notificationEntry2.getRow().isHeadsUp());
                Slog.d(str, sb.toString());
                if (!isHeadsUp) {
                    i5 = 1;
                }
                return i5;
            } else if (isHeadsUp) {
                Slog.d(str, "Comparator, both a&b are heads up");
                return NotificationData.this.mHeadsUpManager.compare(notificationEntry3, notificationEntry4);
            } else if (notificationEntry3.mIsGamingModeNotification) {
                Slog.d(str, "Comparator, a is gaming mode notification");
                return -1;
            } else if (notificationEntry4.mIsGamingModeNotification) {
                Slog.d(str, "Comparator, b is gaming mode notification");
                return 1;
            } else if (notificationEntry.getRow().showingAmbientPulsing() != notificationEntry2.getRow().showingAmbientPulsing()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Comparator, a.showingAmbientPulsing?");
                sb2.append(notificationEntry.getRow().showingAmbientPulsing());
                sb2.append(", b.showingAmbientPulsing?");
                sb2.append(notificationEntry2.getRow().showingAmbientPulsing());
                Slog.d(str, sb2.toString());
                if (!notificationEntry.getRow().showingAmbientPulsing()) {
                    i5 = 1;
                }
                return i5;
            } else if (z != z2) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Comparator, aMedia?");
                sb3.append(z);
                sb3.append(", bMedia?");
                sb3.append(z2);
                Slog.d(str, sb3.toString());
                if (!z) {
                    i5 = 1;
                }
                return i5;
            } else if (z3 != z4) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Comparator, aSystemMax?");
                sb4.append(z3);
                sb4.append(", bSystemMax?");
                sb4.append(z4);
                Slog.d(str, sb4.toString());
                if (!z3) {
                    i5 = 1;
                }
                return i5;
            } else if (notificationEntry.isHighPriority() != notificationEntry2.isHighPriority()) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("Comparator, a.isHighPriority?");
                sb5.append(notificationEntry.isHighPriority());
                sb5.append(", b.isHighPriority?");
                sb5.append(notificationEntry2.isHighPriority());
                Slog.d(str, sb5.toString());
                return Boolean.compare(notificationEntry.isHighPriority(), notificationEntry2.isHighPriority()) * -1;
            } else if (i2 != i) {
                StringBuilder sb6 = new StringBuilder();
                sb6.append("Comparator, aRank?");
                sb6.append(i2);
                sb6.append(", bRank?");
                sb6.append(i);
                Slog.d(str, sb6.toString());
                return i2 - i;
            } else {
                StringBuilder sb7 = new StringBuilder();
                sb7.append("Comparator, a.when?");
                sb7.append(statusBarNotification.getNotification().when);
                sb7.append(", b.when?");
                sb7.append(statusBarNotification2.getNotification().when);
                Slog.d(str, sb7.toString());
                if (statusBarNotification.getNotification().when == 0) {
                    if (statusBarNotification2.getNotification().when == 0) {
                        return 0;
                    }
                    return 1;
                } else if (statusBarNotification2.getNotification().when == 0) {
                    return -1;
                } else {
                    return Long.compare(statusBarNotification2.getNotification().when, statusBarNotification.getNotification().when);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public RankingMap mRankingMap;
    private final ArrayList<NotificationEntry> mSortedAndFiltered = new ArrayList<>();
    private final Ranking mTmpRanking = new Ranking();

    public interface KeyguardEnvironment {
        boolean isDeviceProvisioned();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    private KeyguardEnvironment getEnvironment() {
        if (this.mEnvironment == null) {
            this.mEnvironment = (KeyguardEnvironment) Dependency.get(KeyguardEnvironment.class);
        }
        return this.mEnvironment;
    }

    /* access modifiers changed from: private */
    public NotificationMediaManager getMediaManager() {
        if (this.mMediaManager == null) {
            this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        }
        return this.mMediaManager;
    }

    public ArrayList<NotificationEntry> getActiveNotifications() {
        return this.mSortedAndFiltered;
    }

    public ArrayList<NotificationEntry> getNotificationsForCurrentUser() {
        this.mFilteredForUser.clear();
        synchronized (this.mEntries) {
            int size = this.mEntries.size();
            for (int i = 0; i < size; i++) {
                NotificationEntry notificationEntry = (NotificationEntry) this.mEntries.valueAt(i);
                if (getEnvironment().isNotificationForCurrentProfiles(notificationEntry.notification)) {
                    this.mFilteredForUser.add(notificationEntry);
                }
            }
        }
        return this.mFilteredForUser;
    }

    public NotificationEntry get(String str) {
        return (NotificationEntry) this.mEntries.get(str);
    }

    public void add(NotificationEntry notificationEntry) {
        synchronized (this.mEntries) {
            this.mEntries.put(notificationEntry.notification.getKey(), notificationEntry);
        }
        this.mGroupManager.onEntryAdded(notificationEntry);
        updateRankingAndSort(this.mRankingMap);
    }

    public NotificationEntry remove(String str, RankingMap rankingMap) {
        NotificationEntry notificationEntry;
        synchronized (this.mEntries) {
            notificationEntry = (NotificationEntry) this.mEntries.remove(str);
        }
        if (notificationEntry == null) {
            return null;
        }
        if (rankingMap == null) {
            rankingMap = this.mRankingMap;
        }
        this.mGroupManager.onEntryRemoved(notificationEntry);
        updateRankingAndSort(rankingMap);
        return notificationEntry;
    }

    public void update(NotificationEntry notificationEntry, RankingMap rankingMap, StatusBarNotification statusBarNotification) {
        updateRanking(rankingMap);
        StatusBarNotification statusBarNotification2 = notificationEntry.notification;
        notificationEntry.notification = statusBarNotification;
        this.mGroupManager.onEntryUpdated(notificationEntry, statusBarNotification2);
    }

    public void updateRanking(RankingMap rankingMap) {
        updateRankingAndSort(rankingMap);
    }

    public void updateAppOp(int i, int i2, String str, String str2, boolean z) {
        synchronized (this.mEntries) {
            int size = this.mEntries.size();
            for (int i3 = 0; i3 < size; i3++) {
                NotificationEntry notificationEntry = (NotificationEntry) this.mEntries.valueAt(i3);
                if (i2 == notificationEntry.notification.getUid() && str.equals(notificationEntry.notification.getPackageName()) && str2.equals(notificationEntry.key)) {
                    if (z) {
                        notificationEntry.mActiveAppOps.add(Integer.valueOf(i));
                    } else {
                        notificationEntry.mActiveAppOps.remove(Integer.valueOf(i));
                    }
                }
            }
        }
    }

    public boolean isHighPriority(StatusBarNotification statusBarNotification) {
        if (this.mRankingMap != null) {
            getRanking(statusBarNotification.getKey(), this.mTmpRanking);
            if (this.mTmpRanking.getImportance() >= 3 || hasHighPriorityCharacteristics(this.mTmpRanking.getChannel(), statusBarNotification)) {
                return true;
            }
            if (this.mGroupManager.isSummaryOfGroup(statusBarNotification)) {
                Iterator it = this.mGroupManager.getLogicalChildren(statusBarNotification).iterator();
                while (it.hasNext()) {
                    if (isHighPriority(((NotificationEntry) it.next()).notification)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasHighPriorityCharacteristics(NotificationChannel notificationChannel, StatusBarNotification statusBarNotification) {
        if (!isImportantOngoing(statusBarNotification.getNotification()) && !statusBarNotification.getNotification().hasMediaSession() && !hasPerson(statusBarNotification.getNotification()) && !hasStyle(statusBarNotification.getNotification(), MessagingStyle.class)) {
            return false;
        }
        if (notificationChannel == null || !notificationChannel.hasUserSetImportance()) {
            return true;
        }
        return false;
    }

    private boolean isImportantOngoing(Notification notification) {
        return notification.isForegroundService() && this.mTmpRanking.getImportance() >= 2;
    }

    private boolean hasStyle(Notification notification, Class cls) {
        return cls.equals(notification.getNotificationStyle());
    }

    private boolean hasPerson(Notification notification) {
        ArrayList arrayList;
        Bundle bundle = notification.extras;
        if (bundle != null) {
            arrayList = bundle.getParcelableArrayList("android.people.list");
        } else {
            arrayList = new ArrayList();
        }
        return arrayList != null && !arrayList.isEmpty();
    }

    public boolean isAmbient(String str) {
        if (this.mRankingMap == null) {
            return false;
        }
        getRanking(str, this.mTmpRanking);
        return this.mTmpRanking.isAmbient();
    }

    public int getVisibilityOverride(String str) {
        if (this.mRankingMap == null) {
            return -1000;
        }
        getRanking(str, this.mTmpRanking);
        return this.mTmpRanking.getVisibilityOverride();
    }

    public String getOverrideGroupKey(String str) {
        if (this.mRankingMap == null) {
            return null;
        }
        getRanking(str, this.mTmpRanking);
        return this.mTmpRanking.getOverrideGroupKey();
    }

    public int getRank(String str) {
        if (this.mRankingMap == null) {
            return 0;
        }
        getRanking(str, this.mTmpRanking);
        return this.mTmpRanking.getRank();
    }

    private void updateRankingAndSort(RankingMap rankingMap) {
        if (rankingMap != null) {
            this.mRankingMap = rankingMap;
            synchronized (this.mEntries) {
                int size = this.mEntries.size();
                for (int i = 0; i < size; i++) {
                    NotificationEntry notificationEntry = (NotificationEntry) this.mEntries.valueAt(i);
                    if (getRanking(notificationEntry.key, this.mTmpRanking)) {
                        StatusBarNotification cloneLight = notificationEntry.notification.cloneLight();
                        String overrideGroupKey = getOverrideGroupKey(notificationEntry.key);
                        if (!Objects.equals(cloneLight.getOverrideGroupKey(), overrideGroupKey)) {
                            notificationEntry.notification.setOverrideGroupKey(overrideGroupKey);
                            this.mGroupManager.onEntryUpdated(notificationEntry, cloneLight);
                        }
                        notificationEntry.populateFromRanking(this.mTmpRanking);
                        notificationEntry.setIsHighPriority(isHighPriority(notificationEntry.notification));
                    }
                }
            }
        }
        filterAndSort();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean getRanking(String str, Ranking ranking) {
        return this.mRankingMap.getRanking(str, ranking);
    }

    public void filterAndSort() {
        super.filterAndSort();
    }

    public void dump(PrintWriter printWriter, String str) {
        int size = this.mSortedAndFiltered.size();
        printWriter.print(str);
        StringBuilder sb = new StringBuilder();
        sb.append("active notifications: ");
        sb.append(size);
        printWriter.println(sb.toString());
        int i = 0;
        while (i < size) {
            dumpEntry(printWriter, str, i, (NotificationEntry) this.mSortedAndFiltered.get(i));
            i++;
        }
        synchronized (this.mEntries) {
            int size2 = this.mEntries.size();
            printWriter.print(str);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("inactive notifications: ");
            sb2.append(size2 - i);
            printWriter.println(sb2.toString());
            int i2 = 0;
            for (int i3 = 0; i3 < size2; i3++) {
                NotificationEntry notificationEntry = (NotificationEntry) this.mEntries.valueAt(i3);
                if (!this.mSortedAndFiltered.contains(notificationEntry)) {
                    dumpEntry(printWriter, str, i2, notificationEntry);
                    i2++;
                }
            }
        }
    }

    private void dumpEntry(PrintWriter printWriter, String str, int i, NotificationEntry notificationEntry) {
        getRanking(notificationEntry.key, this.mTmpRanking);
        printWriter.print(str);
        StringBuilder sb = new StringBuilder();
        sb.append("  [");
        sb.append(i);
        sb.append("] key=");
        sb.append(notificationEntry.key);
        sb.append(" icon=");
        sb.append(notificationEntry.icon);
        printWriter.println(sb.toString());
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        printWriter.print(str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("      pkg=");
        sb2.append(statusBarNotification.getPackageName());
        sb2.append(" id=");
        sb2.append(statusBarNotification.getId());
        sb2.append(" importance=");
        sb2.append(this.mTmpRanking.getImportance());
        printWriter.println(sb2.toString());
        printWriter.print(str);
        StringBuilder sb3 = new StringBuilder();
        sb3.append("      notification=");
        sb3.append(statusBarNotification.getNotification());
        printWriter.println(sb3.toString());
    }

    /* access modifiers changed from: private */
    public static boolean isSystemNotification(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        return "android".equals(packageName) || "com.android.systemui".equals(packageName);
    }

    public void setWlbHelper(OPWLBHelper oPWLBHelper) {
        if (oPWLBHelper != null) {
            oPWLBHelper.setNotificationMediaManager(this.mMediaManager);
        }
    }
}
