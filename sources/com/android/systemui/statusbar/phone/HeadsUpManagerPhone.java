package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pools.Pool;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import androidx.collection.ArraySet;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R$dimen;
import com.android.systemui.ScreenDecorations.DisplayCutoutView;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.VisualStabilityManager.Callback;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.oneplus.systemui.util.OpMdmLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class HeadsUpManagerPhone extends HeadsUpManager implements Dumpable, Callback, OnHeadsUpChangedListener, ConfigurationListener, StateListener {
    private AnimationStateHandler mAnimationStateHandler;
    private int mDisplayCutoutTouchableRegionSize;
    /* access modifiers changed from: private */
    public HashSet<NotificationEntry> mEntriesToRemoveAfterExpand = new HashSet<>();
    /* access modifiers changed from: private */
    public ArraySet<NotificationEntry> mEntriesToRemoveWhenReorderingAllowed = new ArraySet<>();
    private final Pool<HeadsUpEntryPhone> mEntryPool = new Pool<HeadsUpEntryPhone>() {
        private Stack<HeadsUpEntryPhone> mPoolObjects = new Stack<>();

        public HeadsUpEntryPhone acquire() {
            if (!this.mPoolObjects.isEmpty()) {
                return (HeadsUpEntryPhone) this.mPoolObjects.pop();
            }
            return new HeadsUpEntryPhone();
        }

        public boolean release(HeadsUpEntryPhone headsUpEntryPhone) {
            this.mPoolObjects.push(headsUpEntryPhone);
            return true;
        }
    };
    private final NotificationGroupManager mGroupManager;
    private boolean mHeadsUpGoingAway;
    private int mHeadsUpInset;
    private boolean mIsExpanded;
    private boolean mReleaseOnExpandFinish;
    private int mStatusBarHeight;
    private int mStatusBarState;
    /* access modifiers changed from: private */
    public final StatusBarTouchableRegionManager mStatusBarTouchableRegionManager;
    private final View mStatusBarWindowView;
    private HashSet<String> mSwipedOutKeys = new HashSet<>();
    private int[] mTmpTwoArray = new int[2];
    private Region mTouchableRegion = new Region();
    /* access modifiers changed from: private */
    public boolean mTrackingHeadsUp;
    /* access modifiers changed from: private */
    public final VisualStabilityManager mVisualStabilityManager;

    public interface AnimationStateHandler {
        void setHeadsUpGoingAwayAnimationsAllowed(boolean z);
    }

    protected class HeadsUpEntryPhone extends HeadsUpEntry {
        private boolean mMenuShownPinned;

        protected HeadsUpEntryPhone() {
            super();
        }

        /* access modifiers changed from: protected */
        public boolean isSticky() {
            return super.isSticky() || this.mMenuShownPinned;
        }

        public void setEntry(NotificationEntry notificationEntry) {
            setEntry(notificationEntry, new Runnable(notificationEntry) {
                private final /* synthetic */ NotificationEntry f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HeadsUpEntryPhone.this.lambda$setEntry$0$HeadsUpManagerPhone$HeadsUpEntryPhone(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$setEntry$0$HeadsUpManagerPhone$HeadsUpEntryPhone(NotificationEntry notificationEntry) {
            if (!HeadsUpManagerPhone.this.mVisualStabilityManager.isReorderingAllowed()) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.add(notificationEntry);
                HeadsUpManagerPhone.this.mVisualStabilityManager.addReorderingAllowedCallback(HeadsUpManagerPhone.this);
            } else if (!HeadsUpManagerPhone.this.mTrackingHeadsUp) {
                if (!(notificationEntry.getRow() == null || notificationEntry.getRow().getPrivateLayout() == null)) {
                    View quickReplyHeadsUpChild = notificationEntry.getRow().getPrivateLayout().getQuickReplyHeadsUpChild();
                    if (quickReplyHeadsUpChild != null && quickReplyHeadsUpChild.getVisibility() == 0 && notificationEntry.getRow().isPinned()) {
                        OpMdmLogger.log("landscape_quick_reply", "hun_action", "0", "YLTI9SVG4L");
                    }
                }
                HeadsUpManagerPhone.this.removeAlertEntry(notificationEntry.key);
            } else {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.add(notificationEntry);
            }
        }

        public void updateEntry(boolean z) {
            super.updateEntry(z);
            if (HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.contains(this.mEntry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.remove(this.mEntry);
            }
            if (HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.contains(this.mEntry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.remove(this.mEntry);
            }
        }

        public void setExpanded(boolean z) {
            if (this.expanded != z) {
                this.expanded = z;
                if (z) {
                    removeAutoRemovalCallbacks();
                } else {
                    updateEntry(false);
                }
            }
        }

        public void setMenuShownPinned(boolean z) {
            if (this.mMenuShownPinned != z) {
                this.mMenuShownPinned = z;
                if (z) {
                    removeAutoRemovalCallbacks();
                } else {
                    updateEntry(false);
                }
            }
        }

        public void reset() {
            super.reset();
            this.mMenuShownPinned = false;
        }
    }

    public HeadsUpManagerPhone(Context context, View view, NotificationGroupManager notificationGroupManager, StatusBar statusBar, VisualStabilityManager visualStabilityManager) {
        super(context);
        this.mStatusBarWindowView = view;
        this.mStatusBarTouchableRegionManager = new StatusBarTouchableRegionManager(context, this, statusBar, view);
        this.mGroupManager = notificationGroupManager;
        this.mVisualStabilityManager = visualStabilityManager;
        initResources();
        addListener(new OnHeadsUpChangedListener() {
            public void onHeadsUpPinnedModeChanged(boolean z) {
                String str = "HeadsUpManagerPhone";
                if (Log.isLoggable(str, 5)) {
                    Log.w(str, "onHeadsUpPinnedModeChanged");
                }
                HeadsUpManagerPhone.this.mStatusBarTouchableRegionManager.updateTouchableRegion();
            }
        });
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public void setAnimationStateHandler(AnimationStateHandler animationStateHandler) {
        this.mAnimationStateHandler = animationStateHandler;
    }

    private void initResources() {
        Resources resources = this.mContext.getResources();
        this.mStatusBarHeight = resources.getDimensionPixelSize(17105422);
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(R$dimen.heads_up_status_bar_padding);
        this.mDisplayCutoutTouchableRegionSize = resources.getDimensionPixelSize(17105140);
    }

    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        initResources();
    }

    public void onOverlayChanged() {
        initResources();
    }

    public boolean shouldSwallowClick(String str) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(str);
        return headsUpEntry != null && this.mClock.currentTimeMillis() < headsUpEntry.mPostTime;
    }

    public void onExpandingFinished() {
        if (this.mReleaseOnExpandFinish) {
            releaseAllImmediately();
            this.mReleaseOnExpandFinish = false;
        } else {
            Iterator it = this.mEntriesToRemoveAfterExpand.iterator();
            while (it.hasNext()) {
                NotificationEntry notificationEntry = (NotificationEntry) it.next();
                if (isAlerting(notificationEntry.key)) {
                    removeAlertEntry(notificationEntry.key);
                }
            }
        }
        this.mEntriesToRemoveAfterExpand.clear();
    }

    public void setTrackingHeadsUp(boolean z) {
        this.mTrackingHeadsUp = z;
    }

    public void setIsPanelExpanded(boolean z) {
        if (z != this.mIsExpanded) {
            this.mIsExpanded = z;
            if (z) {
                this.mHeadsUpGoingAway = false;
            }
            this.mStatusBarTouchableRegionManager.setIsStatusBarExpanded(z);
            this.mStatusBarTouchableRegionManager.updateTouchableRegion();
        }
    }

    public void onStateChanged(int i) {
        this.mStatusBarState = i;
    }

    public void setHeadsUpGoingAway(boolean z) {
        if (z != this.mHeadsUpGoingAway) {
            this.mHeadsUpGoingAway = z;
            if (!z) {
                this.mStatusBarTouchableRegionManager.updateTouchableRegionAfterLayout();
            } else {
                this.mStatusBarTouchableRegionManager.updateTouchableRegion();
            }
        }
    }

    public boolean isHeadsUpGoingAway() {
        return this.mHeadsUpGoingAway;
    }

    public void setRemoteInputActive(NotificationEntry notificationEntry, boolean z) {
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(notificationEntry.key);
        if (headsUpEntryPhone != null && headsUpEntryPhone.remoteInputActive != z) {
            headsUpEntryPhone.remoteInputActive = z;
            if (z) {
                headsUpEntryPhone.removeAutoRemovalCallbacks();
            } else {
                headsUpEntryPhone.updateEntry(false);
            }
        }
    }

    public void setMenuShown(NotificationEntry notificationEntry, boolean z) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(notificationEntry.key);
        if ((headsUpEntry instanceof HeadsUpEntryPhone) && notificationEntry.isRowPinned()) {
            ((HeadsUpEntryPhone) headsUpEntry).setMenuShownPinned(z);
        }
    }

    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    public void snooze() {
        super.snooze();
        this.mReleaseOnExpandFinish = true;
        if (getTopHeadsUpEntry() != null && getTopHeadsUpEntry().mEntry != null && getTopHeadsUpEntry().mEntry.getRow() != null && getTopHeadsUpEntry().mEntry.getRow().getPrivateLayout() != null) {
            View quickReplyHeadsUpChild = getTopHeadsUpEntry().mEntry.getRow().getPrivateLayout().getQuickReplyHeadsUpChild();
            if (quickReplyHeadsUpChild != null && quickReplyHeadsUpChild.getVisibility() == 0) {
                OpMdmLogger.log("landscape_quick_reply", "hun_action", "3", "YLTI9SVG4L");
            }
        }
    }

    public void addSwipedOutNotification(String str) {
        this.mSwipedOutKeys.add(str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HeadsUpManagerPhone state:");
        dumpInternal(fileDescriptor, printWriter, strArr);
    }

    public void updateTouchableRegion(InternalInsetsInfo internalInsetsInfo) {
        internalInsetsInfo.setTouchableInsets(3);
        internalInsetsInfo.touchableRegion.set(calculateTouchableRegion());
    }

    public Region calculateTouchableRegion() {
        NotificationEntry topEntry = getTopEntry();
        if (!hasPinnedHeadsUp() || topEntry == null) {
            this.mTouchableRegion.set(0, 0, this.mStatusBarWindowView.getWidth(), this.mStatusBarHeight);
            updateRegionForNotch(this.mTouchableRegion);
        } else {
            removeOversizeEntries();
            if (topEntry.isChildInGroup()) {
                NotificationEntry groupSummary = this.mGroupManager.getGroupSummary(topEntry.notification);
                if (groupSummary != null) {
                    topEntry = groupSummary;
                }
            }
            ExpandableNotificationRow row = topEntry.getRow();
            row.getLocationOnScreen(this.mTmpTwoArray);
            int[] iArr = this.mTmpTwoArray;
            this.mTouchableRegion.set(iArr[0], 0, iArr[0] + row.getWidth(), this.mHeadsUpInset + row.getIntrinsicHeight());
        }
        return this.mTouchableRegion;
    }

    private void updateRegionForNotch(Region region) {
        DisplayCutout displayCutout = this.mStatusBarWindowView.getRootWindowInsets().getDisplayCutout();
        if (displayCutout != null) {
            Rect rect = new Rect();
            DisplayCutoutView.boundsFromDirection(this.mContext, displayCutout, 48, rect);
            rect.offset(0, this.mDisplayCutoutTouchableRegionSize);
            region.union(rect);
        }
    }

    public boolean shouldExtendLifetime(NotificationEntry notificationEntry) {
        return this.mVisualStabilityManager.isReorderingAllowed() && super.shouldExtendLifetime(notificationEntry);
    }

    public void onConfigChanged(Configuration configuration) {
        initResources();
    }

    public void onReorderingAllowed() {
        this.mAnimationStateHandler.setHeadsUpGoingAwayAnimationsAllowed(false);
        Iterator it = this.mEntriesToRemoveWhenReorderingAllowed.iterator();
        while (it.hasNext()) {
            NotificationEntry notificationEntry = (NotificationEntry) it.next();
            if (isAlerting(notificationEntry.key)) {
                removeAlertEntry(notificationEntry.key);
            }
        }
        this.mEntriesToRemoveWhenReorderingAllowed.clear();
        this.mAnimationStateHandler.setHeadsUpGoingAwayAnimationsAllowed(true);
    }

    /* access modifiers changed from: protected */
    public HeadsUpEntry createAlertEntry() {
        return (HeadsUpEntry) this.mEntryPool.acquire();
    }

    /* access modifiers changed from: protected */
    public void onAlertEntryRemoved(AlertEntry alertEntry) {
        super.onAlertEntryRemoved(alertEntry);
        this.mEntryPool.release((HeadsUpEntryPhone) alertEntry);
    }

    /* access modifiers changed from: protected */
    public boolean shouldHeadsUpBecomePinned(NotificationEntry notificationEntry) {
        if ((this.mStatusBarState == 1 || this.mIsExpanded) && !super.shouldHeadsUpBecomePinned(notificationEntry)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void dumpInternal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dumpInternal(fileDescriptor, printWriter, strArr);
        printWriter.print("  mBarState=");
        printWriter.println(this.mStatusBarState);
        printWriter.print("  mTouchableRegion=");
        printWriter.println(this.mTouchableRegion);
    }

    private HeadsUpEntryPhone getHeadsUpEntryPhone(String str) {
        return (HeadsUpEntryPhone) this.mAlertEntries.get(str);
    }

    private HeadsUpEntryPhone getTopHeadsUpEntryPhone() {
        return (HeadsUpEntryPhone) getTopHeadsUpEntry();
    }

    /* access modifiers changed from: protected */
    public boolean canRemoveImmediately(String str) {
        boolean z = true;
        if (this.mSwipedOutKeys.contains(str)) {
            this.mSwipedOutKeys.remove(str);
            if (!(getTopHeadsUpEntry() == null || getTopHeadsUpEntry().mEntry == null || getTopHeadsUpEntry().mEntry.getRow() == null || getTopHeadsUpEntry().mEntry.getRow().getPrivateLayout() == null)) {
                View quickReplyHeadsUpChild = getTopHeadsUpEntry().mEntry.getRow().getPrivateLayout().getQuickReplyHeadsUpChild();
                if (quickReplyHeadsUpChild != null && quickReplyHeadsUpChild.getVisibility() == 0) {
                    OpMdmLogger.log("landscape_quick_reply", "hun_action", "3", "YLTI9SVG4L");
                }
            }
            return true;
        }
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(str);
        HeadsUpEntryPhone topHeadsUpEntryPhone = getTopHeadsUpEntryPhone();
        if (headsUpEntryPhone != null && headsUpEntryPhone == topHeadsUpEntryPhone && !super.canRemoveImmediately(str)) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void removeOversizeEntries() {
        ArrayMap headsUpEntries = getHeadsUpEntries();
        if (headsUpEntries != null && headsUpEntries.size() > 1) {
            int intrinsicHeight = getTopEntry() == null ? 0 : getTopEntry().getRow().getIntrinsicHeight();
            Iterator it = new ArrayList(headsUpEntries.keySet()).iterator();
            while (it.hasNext()) {
                String str = (String) it.next();
                HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(str);
                if (headsUpEntryPhone != null) {
                    NotificationEntry notificationEntry = headsUpEntryPhone.mEntry;
                    if (notificationEntry != null && notificationEntry.getRow().getIntrinsicHeight() > intrinsicHeight) {
                        headsUpEntryPhone.remove();
                        StringBuilder sb = new StringBuilder();
                        sb.append("remove heads-up ");
                        sb.append(str);
                        sb.append(" which is higher than the current one");
                        Log.d("HeadsUpManagerPhone", sb.toString());
                    }
                }
            }
        }
    }
}
