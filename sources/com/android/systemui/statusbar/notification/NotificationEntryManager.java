package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.os.Build;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.NotificationLifetimeExtender;
import com.android.systemui.statusbar.NotificationLifetimeExtender.NotificationSafeToRemoveCallback;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationRemoveInterceptor;
import com.android.systemui.statusbar.NotificationUiAdjustment;
import com.android.systemui.statusbar.NotificationUpdateHandler;
import com.android.systemui.statusbar.notification.VisualStabilityManager.Callback;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.collection.NotificationData.KeyguardEnvironment;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinder;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.NotificationContentInflater.InflationCallback;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.phone.HighlightHintController;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.util.leak.LeakDetector;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.worklife.OPWLBHelper;
import com.oneplus.worklife.OPWLBHelper.IWLBModeChangeListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NotificationEntryManager implements Dumpable, InflationCallback, NotificationUpdateHandler, Callback, IWLBModeChangeListener, OnHighlightHintStateChangeListener {
    private static final boolean DEBUG = Log.isLoggable("NotificationEntryMgr", 3);
    private static final boolean OP_DEBUG = Build.DEBUG_ONEPLUS;
    protected final HashMap<String, NotificationEntry> mCachedNotifications = new HashMap<>();
    protected final Context mContext;
    @VisibleForTesting
    protected NotificationData mNotificationData = new NotificationData();
    private final List<NotificationEntryListener> mNotificationEntryListeners = new ArrayList();
    @VisibleForTesting
    final ArrayList<NotificationLifetimeExtender> mNotificationLifetimeExtenders = new ArrayList<>();
    private NotificationRowBinder mNotificationRowBinder;
    private final OpNotificationController mOPNotificationController = ((OpNotificationController) Dependency.get(OpNotificationController.class));
    private OPWLBHelper mOpwlbHelper;
    @VisibleForTesting
    protected final HashMap<String, NotificationEntry> mPendingNotifications = new HashMap<>();
    private NotificationPresenter mPresenter;
    private NotificationRemoteInputManager mRemoteInputManager;
    private NotificationRemoveInterceptor mRemoveInterceptor;
    private final Map<NotificationEntry, NotificationLifetimeExtender> mRetainedNotifications = new ArrayMap();

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationEntryManager state:");
        printWriter.print("  mPendingNotifications=");
        if (this.mPendingNotifications.size() == 0) {
            printWriter.println("null");
        } else {
            for (NotificationEntry notificationEntry : this.mPendingNotifications.values()) {
                printWriter.println(notificationEntry.notification);
            }
        }
        printWriter.println("  Lifetime-extended notifications:");
        if (this.mRetainedNotifications.isEmpty()) {
            printWriter.println("    None");
            return;
        }
        for (Entry entry : this.mRetainedNotifications.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(((NotificationEntry) entry.getKey()).notification);
            sb.append(" retained by ");
            sb.append(((NotificationLifetimeExtender) entry.getValue()).getClass().getName());
            printWriter.println(sb.toString());
        }
    }

    public NotificationEntryManager(Context context) {
        this.mContext = context;
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).addCallback(this);
        this.mOpwlbHelper = OPWLBHelper.getInstance(this.mContext);
        this.mOpwlbHelper.registerChanges(this);
        this.mNotificationData.setWlbHelper(this.mOpwlbHelper);
    }

    public void addNotificationEntryListener(NotificationEntryListener notificationEntryListener) {
        this.mNotificationEntryListeners.add(notificationEntryListener);
    }

    public void setNotificationRemoveInterceptor(NotificationRemoveInterceptor notificationRemoveInterceptor) {
        this.mRemoveInterceptor = notificationRemoveInterceptor;
    }

    private NotificationRemoteInputManager getRemoteInputManager() {
        if (this.mRemoteInputManager == null) {
            this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        }
        return this.mRemoteInputManager;
    }

    public void setRowBinder(NotificationRowBinder notificationRowBinder) {
        this.mNotificationRowBinder = notificationRowBinder;
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationListContainer notificationListContainer, HeadsUpManager headsUpManager) {
        this.mPresenter = notificationPresenter;
        this.mNotificationData.setHeadsUpManager(headsUpManager);
        ((NotificationFilter) Dependency.get(NotificationFilter.class)).setWlbHelper(this.mOpwlbHelper);
    }

    public void addNotificationLifetimeExtenders(List<NotificationLifetimeExtender> list) {
        for (NotificationLifetimeExtender addNotificationLifetimeExtender : list) {
            addNotificationLifetimeExtender(addNotificationLifetimeExtender);
        }
    }

    public void addNotificationLifetimeExtender(NotificationLifetimeExtender notificationLifetimeExtender) {
        this.mNotificationLifetimeExtenders.add(notificationLifetimeExtender);
        notificationLifetimeExtender.setCallback(new NotificationSafeToRemoveCallback() {
            public final void onSafeToRemove(String str) {
                NotificationEntryManager.this.mo15482x96dd259f(str);
            }
        });
    }

    /* renamed from: lambda$addNotificationLifetimeExtender$0$NotificationEntryManager */
    public /* synthetic */ void mo15482x96dd259f(String str) {
        removeNotification(str, null, 0);
    }

    public NotificationData getNotificationData() {
        return this.mNotificationData;
    }

    public void onReorderingAllowed() {
        updateNotifications();
    }

    public void performRemoveNotification(StatusBarNotification statusBarNotification, int i) {
        removeNotificationInternal(statusBarNotification.getKey(), null, obtainVisibility(statusBarNotification.getKey()), false, true, i);
    }

    private NotificationVisibility obtainVisibility(String str) {
        return NotificationVisibility.obtain(str, this.mNotificationData.getRank(str), this.mNotificationData.getActiveNotifications().size(), true, NotificationLogger.getNotificationLocation(getNotificationData().get(str)));
    }

    private void abortExistingInflation(String str) {
        if (this.mPendingNotifications.containsKey(str)) {
            ((NotificationEntry) this.mPendingNotifications.get(str)).abortTask();
            this.mPendingNotifications.remove(str);
        }
        NotificationEntry notificationEntry = this.mNotificationData.get(str);
        if (notificationEntry != null) {
            notificationEntry.abortTask();
        }
    }

    public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
        removeNotificationInternal(statusBarNotification.getKey(), null, null, true, false, 4);
        for (NotificationEntryListener onInflationError : this.mNotificationEntryListeners) {
            onInflationError.onInflationError(statusBarNotification, exc);
        }
    }

    public void onAsyncInflationFinished(NotificationEntry notificationEntry, int i) {
        if (this.mCachedNotifications.containsKey(notificationEntry.key)) {
            if (((NotificationEntry) this.mCachedNotifications.get(notificationEntry.key)).hasJustLaunchedFullScreenIntent()) {
                notificationEntry.notifyFullScreenIntentLaunched();
                if (OP_DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onAsyncInflationFinished, update lastFullScreenIntentLaunchTime from cached entry, key: ");
                    sb.append(notificationEntry.key);
                    Log.d("NotificationEntryMgr", sb.toString());
                }
            }
            this.mCachedNotifications.remove(notificationEntry.key);
        }
        this.mPendingNotifications.remove(notificationEntry.key);
        if (!notificationEntry.isRowRemoved()) {
            if (this.mNotificationData.get(notificationEntry.key) == null) {
                for (NotificationEntryListener onEntryInflated : this.mNotificationEntryListeners) {
                    onEntryInflated.onEntryInflated(notificationEntry, i);
                }
                this.mNotificationData.add(notificationEntry);
                for (NotificationEntryListener onBeforeNotificationAdded : this.mNotificationEntryListeners) {
                    onBeforeNotificationAdded.onBeforeNotificationAdded(notificationEntry);
                }
                updateNotifications();
                for (NotificationEntryListener onNotificationAdded : this.mNotificationEntryListeners) {
                    onNotificationAdded.onNotificationAdded(notificationEntry);
                }
                return;
            }
            for (NotificationEntryListener onEntryReinflated : this.mNotificationEntryListeners) {
                onEntryReinflated.onEntryReinflated(notificationEntry);
            }
        }
    }

    public void removeNotification(String str, RankingMap rankingMap, int i) {
        removeNotificationInternal(str, rankingMap, obtainVisibility(str), false, false, i);
    }

    private void removeNotificationInternal(String str, RankingMap rankingMap, NotificationVisibility notificationVisibility, boolean z, boolean z2, int i) {
        NotificationRemoveInterceptor notificationRemoveInterceptor = this.mRemoveInterceptor;
        if (notificationRemoveInterceptor == null || !notificationRemoveInterceptor.onNotificationRemoveRequested(str, i)) {
            NotificationEntry notificationEntry = this.mNotificationData.get(str);
            boolean z3 = false;
            if (notificationEntry == null) {
                NotificationEntry notificationEntry2 = (NotificationEntry) this.mPendingNotifications.get(str);
                if (notificationEntry2 != null) {
                    Iterator it = this.mNotificationLifetimeExtenders.iterator();
                    while (it.hasNext()) {
                        NotificationLifetimeExtender notificationLifetimeExtender = (NotificationLifetimeExtender) it.next();
                        if (notificationLifetimeExtender.shouldExtendLifetimeForPendingNotification(notificationEntry2)) {
                            extendLifetime(notificationEntry2, notificationLifetimeExtender);
                            z3 = true;
                        }
                    }
                }
            }
            if (!z3) {
                abortExistingInflation(str);
            }
            if (this.mCachedNotifications.containsKey(str)) {
                this.mCachedNotifications.remove(str);
            }
            if (notificationEntry != null) {
                StatusBarNotification statusBarNotification = notificationEntry.notification;
                if (statusBarNotification != null && this.mOPNotificationController.shouldForceRemoveEntry(statusBarNotification.getPackageName())) {
                    z = true;
                }
            }
            if (notificationEntry != null) {
                boolean isRowDismissed = notificationEntry.isRowDismissed();
                if (!z && !isRowDismissed) {
                    Iterator it2 = this.mNotificationLifetimeExtenders.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        NotificationLifetimeExtender notificationLifetimeExtender2 = (NotificationLifetimeExtender) it2.next();
                        if (notificationLifetimeExtender2.shouldExtendLifetime(notificationEntry)) {
                            extendLifetime(notificationEntry, notificationLifetimeExtender2);
                            z3 = true;
                            break;
                        }
                    }
                }
                if (!z3) {
                    cancelLifetimeExtension(notificationEntry);
                    if (notificationEntry.rowExists()) {
                        notificationEntry.removeRow();
                    }
                    handleGroupSummaryRemoved(str);
                    this.mNotificationData.remove(str, rankingMap);
                    updateNotifications();
                    ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(notificationEntry);
                    boolean z4 = z2 | isRowDismissed;
                    for (NotificationEntryListener onEntryRemoved : this.mNotificationEntryListeners) {
                        onEntryRemoved.onEntryRemoved(notificationEntry, notificationVisibility, z4);
                    }
                    OPWLBHelper.getInstance(this.mContext).removeNotificationKey(str);
                }
            }
        }
    }

    private void handleGroupSummaryRemoved(String str) {
        NotificationEntry notificationEntry = this.mNotificationData.get(str);
        if (notificationEntry != null && notificationEntry.rowExists() && notificationEntry.isSummaryWithChildren() && (notificationEntry.notification.getOverrideGroupKey() == null || notificationEntry.isRowDismissed())) {
            List children = notificationEntry.getChildren();
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    NotificationEntry notificationEntry2 = (NotificationEntry) children.get(i);
                    boolean z = (notificationEntry.notification.getNotification().flags & 64) != 0;
                    boolean z2 = getRemoteInputManager().shouldKeepForRemoteInputHistory(notificationEntry2) || getRemoteInputManager().shouldKeepForSmartReplyHistory(notificationEntry2);
                    if (!z && !z2) {
                        notificationEntry2.setKeepInParent(true);
                        notificationEntry2.removeRow();
                    }
                }
            }
        }
    }

    private void addNotificationInternal(StatusBarNotification statusBarNotification, RankingMap rankingMap) throws InflationException {
        String key = statusBarNotification.getKey();
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("addNotification key=");
            sb.append(key);
            Log.d("NotificationEntryMgr", sb.toString());
        }
        this.mNotificationData.updateRanking(rankingMap);
        Ranking ranking = new Ranking();
        rankingMap.getRanking(key, ranking);
        NotificationEntry notificationEntry = new NotificationEntry(statusBarNotification, ranking);
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackInstance(notificationEntry);
        requireBinder().inflateViews(notificationEntry, new Runnable(statusBarNotification) {
            private final /* synthetic */ StatusBarNotification f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NotificationEntryManager.this.lambda$addNotificationInternal$1$NotificationEntryManager(this.f$1);
            }
        });
        abortExistingInflation(key);
        this.mPendingNotifications.put(key, notificationEntry);
        for (NotificationEntryListener onPendingEntryAdded : this.mNotificationEntryListeners) {
            onPendingEntryAdded.onPendingEntryAdded(notificationEntry);
        }
    }

    public /* synthetic */ void lambda$addNotificationInternal$1$NotificationEntryManager(StatusBarNotification statusBarNotification) {
        performRemoveNotification(statusBarNotification, 2);
    }

    public void addNotification(StatusBarNotification statusBarNotification, RankingMap rankingMap) {
        try {
            addNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    private void updateNotificationInternal(StatusBarNotification statusBarNotification, RankingMap rankingMap) throws InflationException {
        String str = "NotificationEntryMgr";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateNotification(");
            sb.append(statusBarNotification);
            sb.append(")");
            Log.d(str, sb.toString());
        }
        String key = statusBarNotification.getKey();
        abortExistingInflation(key);
        NotificationEntry notificationEntry = this.mNotificationData.get(key);
        if (notificationEntry != null) {
            if (notificationEntry.notification.getNotification().ShowChronometerOnStatusBar()) {
                notificationEntry.createChronometer(this.mContext);
            } else {
                notificationEntry.statusBarChronometer = null;
                notificationEntry.keyguardChronometer = null;
            }
            cancelLifetimeExtension(notificationEntry);
            this.mNotificationData.update(notificationEntry, rankingMap, statusBarNotification);
            for (NotificationEntryListener onPreEntryUpdated : this.mNotificationEntryListeners) {
                onPreEntryUpdated.onPreEntryUpdated(notificationEntry);
            }
            requireBinder().inflateViews(notificationEntry, new Runnable(statusBarNotification) {
                private final /* synthetic */ StatusBarNotification f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    NotificationEntryManager.this.lambda$updateNotificationInternal$2$NotificationEntryManager(this.f$1);
                }
            });
            updateNotifications();
            if (DEBUG) {
                boolean isNotificationForCurrentProfiles = ((KeyguardEnvironment) Dependency.get(KeyguardEnvironment.class)).isNotificationForCurrentProfiles(statusBarNotification);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("notification is ");
                sb2.append(isNotificationForCurrentProfiles ? "" : "not ");
                sb2.append("for you");
                Log.d(str, sb2.toString());
            }
            for (NotificationEntryListener onPostEntryUpdated : this.mNotificationEntryListeners) {
                onPostEntryUpdated.onPostEntryUpdated(notificationEntry);
            }
        }
    }

    public /* synthetic */ void lambda$updateNotificationInternal$2$NotificationEntryManager(StatusBarNotification statusBarNotification) {
        performRemoveNotification(statusBarNotification, 2);
    }

    public void updateNotification(StatusBarNotification statusBarNotification, RankingMap rankingMap) {
        try {
            updateNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    public void updateNotifications() {
        this.mNotificationData.filterAndSort();
        NotificationPresenter notificationPresenter = this.mPresenter;
        if (notificationPresenter != null) {
            notificationPresenter.updateNotificationViews();
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).onNotificationUpdate(this.mNotificationData);
        this.mOPNotificationController.setQuickReplyFlags();
    }

    public void updateNotificationRanking(RankingMap rankingMap) {
        ArrayList<NotificationEntry> arrayList = new ArrayList<>();
        arrayList.addAll(this.mNotificationData.getActiveNotifications());
        arrayList.addAll(this.mPendingNotifications.values());
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        for (NotificationEntry notificationEntry : arrayList) {
            arrayMap.put(notificationEntry.key, NotificationUiAdjustment.extractFromNotificationEntry(notificationEntry));
            arrayMap2.put(notificationEntry.key, Integer.valueOf(notificationEntry.importance));
        }
        this.mNotificationData.updateRanking(rankingMap);
        updateRankingOfPendingNotifications(rankingMap);
        for (NotificationEntry notificationEntry2 : arrayList) {
            requireBinder().onNotificationRankingUpdated(notificationEntry2, (Integer) arrayMap2.get(notificationEntry2.key), (NotificationUiAdjustment) arrayMap.get(notificationEntry2.key), NotificationUiAdjustment.extractFromNotificationEntry(notificationEntry2));
        }
        updateNotifications();
        for (NotificationEntryListener onNotificationRankingUpdated : this.mNotificationEntryListeners) {
            onNotificationRankingUpdated.onNotificationRankingUpdated(rankingMap);
        }
    }

    private void updateRankingOfPendingNotifications(RankingMap rankingMap) {
        if (rankingMap != null) {
            Ranking ranking = new Ranking();
            for (NotificationEntry notificationEntry : this.mPendingNotifications.values()) {
                rankingMap.getRanking(notificationEntry.key, ranking);
                notificationEntry.populateFromRanking(ranking);
            }
        }
    }

    public Iterable<NotificationEntry> getPendingNotificationsIterator() {
        return this.mPendingNotifications.values();
    }

    private void extendLifetime(NotificationEntry notificationEntry, NotificationLifetimeExtender notificationLifetimeExtender) {
        NotificationLifetimeExtender notificationLifetimeExtender2 = (NotificationLifetimeExtender) this.mRetainedNotifications.get(notificationEntry);
        if (!(notificationLifetimeExtender2 == null || notificationLifetimeExtender2 == notificationLifetimeExtender)) {
            notificationLifetimeExtender2.setShouldManageLifetime(notificationEntry, false);
        }
        this.mRetainedNotifications.put(notificationEntry, notificationLifetimeExtender);
        notificationLifetimeExtender.setShouldManageLifetime(notificationEntry, true);
    }

    private void cancelLifetimeExtension(NotificationEntry notificationEntry) {
        NotificationLifetimeExtender notificationLifetimeExtender = (NotificationLifetimeExtender) this.mRetainedNotifications.remove(notificationEntry);
        if (notificationLifetimeExtender != null) {
            notificationLifetimeExtender.setShouldManageLifetime(notificationEntry, false);
        }
    }

    private NotificationRowBinder requireBinder() {
        NotificationRowBinder notificationRowBinder = this.mNotificationRowBinder;
        if (notificationRowBinder != null) {
            return notificationRowBinder;
        }
        throw new RuntimeException("You must initialize NotificationEntryManager by callingsetRowBinder() before using.");
    }

    public void cacheNotification(String str, NotificationEntry notificationEntry) {
        if (!this.mCachedNotifications.containsKey(str)) {
            this.mCachedNotifications.put(str, notificationEntry);
        }
    }

    public void onModeChanged() {
        updateNotifications();
    }
}
