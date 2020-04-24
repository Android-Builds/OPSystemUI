package com.android.systemui.statusbar.notification.collection;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.BubbleMetadata;
import android.app.Notification.InboxStyle;
import android.app.Notification.MediaStyle;
import android.app.Notification.MessagingStyle;
import android.app.Notification.MessagingStyle.Message;
import android.app.NotificationChannel;
import android.app.Person;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.ImageView.ScaleType;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.StatusBarIconView.OnVisibilityChangedListener;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationGuts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class NotificationEntry {
    public boolean ambient;
    public boolean autoRedacted;
    public boolean canBubble;
    public StatusBarIconView centeredIcon;
    public NotificationChannel channel;
    public EditedSuggestionInfo editedSuggestionInfo;
    public StatusBarIconView expandedIcon;
    private boolean hasSentReply;
    public CharSequence headsUpStatusBarText;
    public CharSequence headsUpStatusBarTextPublic;
    public StatusBarIconView icon;
    public int importance;
    private long initializationTime = -1;
    private boolean interruption;
    public final String key;
    public Chronometer keyguardChronometer = null;
    public long lastAudiblyAlertedMs;
    private long lastFullScreenIntentLaunchTime = -2000;
    public long lastRemoteInputSent = -2000;
    public ArraySet<Integer> mActiveAppOps = new ArraySet<>(3);
    private int mCachedContrastColor = 1;
    private int mCachedContrastColorIsFor = 1;
    private Throwable mDebugThrowable;
    private boolean mHighPriority;
    public boolean mIsGamingModeNotification;
    public Boolean mIsSystemNotification;
    private InflationTask mRunningTask = null;
    private boolean mShowInShadeWhenBubble;
    private boolean mUserDismissedBubble;
    public StatusBarNotification notification;
    private NotificationEntry parent;
    public CharSequence remoteInputText;
    public CharSequence remoteInputTextWhenReset;
    private ExpandableNotificationRow row;
    public List<SnoozeCriterion> snoozeCriteria;
    public Chronometer statusBarChronometer = null;
    @VisibleForTesting
    public int suppressedVisualEffects;
    public boolean suspended;
    public List<Action> systemGeneratedSmartActions = Collections.emptyList();
    public CharSequence[] systemGeneratedSmartReplies = new CharSequence[0];
    public int targetSdk;
    public int userSentiment = 0;

    public static class EditedSuggestionInfo {
        public final int index;
        public final CharSequence originalText;

        public EditedSuggestionInfo(CharSequence charSequence, int i) {
            this.originalText = charSequence;
            this.index = i;
        }
    }

    public NotificationEntry(StatusBarNotification statusBarNotification, Ranking ranking) {
        this.key = statusBarNotification.getKey();
        this.notification = statusBarNotification;
        if (ranking != null) {
            populateFromRanking(ranking);
        }
        try {
            this.mIsGamingModeNotification = "scene_modes_game".equals(statusBarNotification.getNotification().getChannelId());
            this.mIsGamingModeNotification = "android".equals(statusBarNotification.getPackageName()) & this.mIsGamingModeNotification;
        } catch (Exception e) {
            Log.e("NotificationData", "exception while getting channel ID");
            e.printStackTrace();
        }
    }

    public void populateFromRanking(Ranking ranking) {
        CharSequence[] charSequenceArr;
        this.channel = ranking.getChannel();
        this.lastAudiblyAlertedMs = ranking.getLastAudiblyAlertedMillis();
        this.importance = ranking.getImportance();
        this.ambient = ranking.isAmbient();
        this.snoozeCriteria = ranking.getSnoozeCriteria();
        this.userSentiment = ranking.getUserSentiment();
        this.systemGeneratedSmartActions = ranking.getSmartActions() == null ? Collections.emptyList() : ranking.getSmartActions();
        if (ranking.getSmartReplies() == null) {
            charSequenceArr = new CharSequence[0];
        } else {
            charSequenceArr = (CharSequence[]) ranking.getSmartReplies().toArray(new CharSequence[0]);
        }
        this.systemGeneratedSmartReplies = charSequenceArr;
        this.suppressedVisualEffects = ranking.getSuppressedVisualEffects();
        this.suspended = ranking.isSuspended();
        this.canBubble = ranking.canBubble();
    }

    public void setInterruption() {
        this.interruption = true;
    }

    public boolean hasInterrupted() {
        return this.interruption;
    }

    public boolean isHighPriority() {
        return this.mHighPriority;
    }

    public void setIsHighPriority(boolean z) {
        this.mHighPriority = z;
    }

    public boolean isBubble() {
        return (this.notification.getNotification().flags & 4096) != 0;
    }

    public void setBubbleDismissed(boolean z) {
        this.mUserDismissedBubble = z;
    }

    public boolean isBubbleDismissed() {
        return this.mUserDismissedBubble;
    }

    public void setShowInShadeWhenBubble(boolean z) {
        this.mShowInShadeWhenBubble = z;
    }

    public boolean showInShadeWhenBubble() {
        return !isRowDismissed() && (!isClearable() || this.mShowInShadeWhenBubble);
    }

    public BubbleMetadata getBubbleMetadata() {
        return this.notification.getNotification().getBubbleMetadata();
    }

    public void reset() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.reset();
        }
    }

    public ExpandableNotificationRow getRow() {
        return this.row;
    }

    public void setRow(ExpandableNotificationRow expandableNotificationRow) {
        this.row = expandableNotificationRow;
    }

    public List<NotificationEntry> getChildren() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow == null) {
            return null;
        }
        List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
        if (notificationChildren == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (ExpandableNotificationRow entry : notificationChildren) {
            arrayList.add(entry.getEntry());
        }
        return arrayList;
    }

    public void notifyFullScreenIntentLaunched() {
        setInterruption();
        this.lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
    }

    public boolean hasJustLaunchedFullScreenIntent() {
        return SystemClock.elapsedRealtime() < this.lastFullScreenIntentLaunchTime + 2000;
    }

    public boolean hasJustSentRemoteInput() {
        return SystemClock.elapsedRealtime() < this.lastRemoteInputSent + 500;
    }

    public boolean hasFinishedInitialization() {
        return this.initializationTime == -1 || SystemClock.elapsedRealtime() > this.initializationTime + 400;
    }

    public void createChronometer(Context context) {
        Notification notification2 = this.notification.getNotification();
        if (notification2.getChronometerState() == 0) {
            initChronometers(context, notification2);
        } else {
            Chronometer chronometer = this.statusBarChronometer;
            if (chronometer == null) {
                initChronometers(context, notification2);
            } else {
                chronometer.setBase(notification2.getChronometerBase());
                this.keyguardChronometer.setBase(notification2.getChronometerBase());
            }
        }
        if (notification2.getChronometerState() == 0) {
            this.statusBarChronometer.start();
            this.keyguardChronometer.start();
            return;
        }
        this.statusBarChronometer.stop();
        this.keyguardChronometer.stop();
    }

    private void initChronometers(Context context, Notification notification2) {
        this.statusBarChronometer = new Chronometer(context);
        this.statusBarChronometer.setBase(notification2.getChronometerBase());
        this.keyguardChronometer = new Chronometer(context);
        this.keyguardChronometer.setBase(notification2.getChronometerBase());
    }

    public void createIcons(Context context, StatusBarNotification statusBarNotification) throws InflationException {
        Notification notification2 = statusBarNotification.getNotification();
        Icon smallIcon = notification2.getSmallIcon();
        if (smallIcon != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(statusBarNotification.getPackageName());
            String str = "/0x";
            sb.append(str);
            sb.append(Integer.toHexString(statusBarNotification.getId()));
            this.icon = new StatusBarIconView(context, sb.toString(), statusBarNotification);
            this.icon.setScaleType(ScaleType.CENTER_INSIDE);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(statusBarNotification.getPackageName());
            sb2.append(str);
            sb2.append(Integer.toHexString(statusBarNotification.getId()));
            this.expandedIcon = new StatusBarIconView(context, sb2.toString(), statusBarNotification);
            this.expandedIcon.setScaleType(ScaleType.CENTER_INSIDE);
            StatusBarIcon statusBarIcon = new StatusBarIcon(statusBarNotification.getUser(), statusBarNotification.getPackageName(), smallIcon, notification2.iconLevel, notification2.number, StatusBarIconView.contentDescForNotification(context, notification2));
            if (!this.icon.set(statusBarIcon) || !this.expandedIcon.set(statusBarIcon)) {
                this.icon = null;
                this.expandedIcon = null;
                this.centeredIcon = null;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Couldn't create icon: ");
                sb3.append(statusBarIcon);
                throw new InflationException(sb3.toString());
            }
            this.expandedIcon.setVisibility(4);
            this.expandedIcon.setOnVisibilityChangedListener(new OnVisibilityChangedListener() {
                public final void onVisibilityChanged(int i) {
                    NotificationEntry.this.lambda$createIcons$0$NotificationEntry(i);
                }
            });
            if (this.notification.getNotification().isMediaNotification()) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append(statusBarNotification.getPackageName());
                sb4.append(str);
                sb4.append(Integer.toHexString(statusBarNotification.getId()));
                this.centeredIcon = new StatusBarIconView(context, sb4.toString(), statusBarNotification);
                this.centeredIcon.setScaleType(ScaleType.CENTER_INSIDE);
                if (!this.centeredIcon.set(statusBarIcon)) {
                    this.centeredIcon = null;
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append("Couldn't update centered icon: ");
                    sb5.append(statusBarIcon);
                    throw new InflationException(sb5.toString());
                }
                return;
            }
            return;
        }
        StringBuilder sb6 = new StringBuilder();
        sb6.append("No small icon in notification from ");
        sb6.append(statusBarNotification.getPackageName());
        throw new InflationException(sb6.toString());
    }

    public /* synthetic */ void lambda$createIcons$0$NotificationEntry(int i) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setIconsVisible(i != 0);
        }
    }

    public void setIconTag(int i, Object obj) {
        StatusBarIconView statusBarIconView = this.icon;
        if (statusBarIconView != null) {
            statusBarIconView.setTag(i, obj);
            this.expandedIcon.setTag(i, obj);
        }
        StatusBarIconView statusBarIconView2 = this.centeredIcon;
        if (statusBarIconView2 != null) {
            statusBarIconView2.setTag(i, obj);
        }
    }

    public void updateIcons(Context context, StatusBarNotification statusBarNotification) throws InflationException {
        if (this.icon != null) {
            Notification notification2 = statusBarNotification.getNotification();
            StatusBarIcon statusBarIcon = new StatusBarIcon(this.notification.getUser(), this.notification.getPackageName(), notification2.getSmallIcon(), notification2.iconLevel, notification2.number, StatusBarIconView.contentDescForNotification(context, notification2));
            this.icon.setNotification(statusBarNotification);
            this.expandedIcon.setNotification(statusBarNotification);
            if (!this.icon.set(statusBarIcon) || !this.expandedIcon.set(statusBarIcon)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Couldn't update icon: ");
                sb.append(statusBarIcon);
                throw new InflationException(sb.toString());
            }
            StatusBarIconView statusBarIconView = this.centeredIcon;
            if (statusBarIconView != null) {
                statusBarIconView.setNotification(statusBarNotification);
                if (!this.centeredIcon.set(statusBarIcon)) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Couldn't update centered icon: ");
                    sb2.append(statusBarIcon);
                    throw new InflationException(sb2.toString());
                }
            }
        }
    }

    public int getContrastedColor(Context context, boolean z, int i) {
        int i2 = z ? 0 : this.notification.getNotification().color;
        if (this.mCachedContrastColorIsFor == i2) {
            int i3 = this.mCachedContrastColor;
            if (i3 != 1) {
                return i3;
            }
        }
        int resolveContrastColor = ContrastColorUtil.resolveContrastColor(context, i2, i);
        this.mCachedContrastColorIsFor = i2;
        this.mCachedContrastColor = resolveContrastColor;
        return this.mCachedContrastColor;
    }

    public CharSequence getUpdateMessage(Context context) {
        Notification notification2 = this.notification.getNotification();
        Class notificationStyle = notification2.getNotificationStyle();
        try {
            String str = "android.text";
            if (BigTextStyle.class.equals(notificationStyle)) {
                CharSequence charSequence = notification2.extras.getCharSequence("android.bigText");
                if (TextUtils.isEmpty(charSequence)) {
                    charSequence = notification2.extras.getCharSequence(str);
                }
                return charSequence;
            }
            if (MessagingStyle.class.equals(notificationStyle)) {
                Message findLatestIncomingMessage = MessagingStyle.findLatestIncomingMessage(Message.getMessagesFromBundleArray((Parcelable[]) notification2.extras.get("android.messages")));
                if (findLatestIncomingMessage != null) {
                    CharSequence name = findLatestIncomingMessage.getSenderPerson() != null ? findLatestIncomingMessage.getSenderPerson().getName() : null;
                    if (TextUtils.isEmpty(name)) {
                        return findLatestIncomingMessage.getText();
                    }
                    return context.getResources().getString(R$string.notification_summary_message_format, new Object[]{name, findLatestIncomingMessage.getText()});
                }
            } else if (InboxStyle.class.equals(notificationStyle)) {
                CharSequence[] charSequenceArray = notification2.extras.getCharSequenceArray("android.textLines");
                if (charSequenceArray != null && charSequenceArray.length > 0) {
                    return charSequenceArray[charSequenceArray.length - 1];
                }
            } else if (MediaStyle.class.equals(notificationStyle)) {
                return null;
            } else {
                return notification2.extras.getCharSequence(str);
            }
            return null;
        } catch (ArrayIndexOutOfBoundsException | ClassCastException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void abortTask() {
        InflationTask inflationTask = this.mRunningTask;
        if (inflationTask != null) {
            inflationTask.abort();
            this.mRunningTask = null;
        }
    }

    public void setInflationTask(InflationTask inflationTask) {
        InflationTask inflationTask2 = this.mRunningTask;
        abortTask();
        this.mRunningTask = inflationTask;
        if (inflationTask2 != null) {
            InflationTask inflationTask3 = this.mRunningTask;
            if (inflationTask3 != null) {
                inflationTask3.supersedeTask(inflationTask2);
            }
        }
    }

    public void onInflationTaskFinished() {
        this.mRunningTask = null;
    }

    @VisibleForTesting
    public InflationTask getRunningTask() {
        return this.mRunningTask;
    }

    public void setDebugThrowable(Throwable th) {
        this.mDebugThrowable = th;
    }

    public Throwable getDebugThrowable() {
        return this.mDebugThrowable;
    }

    public void onRemoteInputInserted() {
        this.lastRemoteInputSent = -2000;
        this.remoteInputTextWhenReset = null;
    }

    public void setHasSentReply() {
        this.hasSentReply = true;
    }

    public boolean isLastMessageFromReply() {
        if (!this.hasSentReply) {
            return false;
        }
        Bundle bundle = this.notification.getNotification().extras;
        if (!ArrayUtils.isEmpty(bundle.getCharSequenceArray("android.remoteInputHistory"))) {
            return true;
        }
        Parcelable[] parcelableArray = bundle.getParcelableArray("android.messages");
        if (parcelableArray != null && parcelableArray.length > 0) {
            Parcelable parcelable = parcelableArray[parcelableArray.length - 1];
            if (parcelable instanceof Bundle) {
                Message messageFromBundle = Message.getMessageFromBundle((Bundle) parcelable);
                if (messageFromBundle != null) {
                    Person senderPerson = messageFromBundle.getSenderPerson();
                    if (senderPerson == null) {
                        return true;
                    }
                    return Objects.equals((Person) bundle.getParcelable("android.messagingUser"), senderPerson);
                }
            }
        }
        return false;
    }

    public void setInitializationTime(long j) {
        if (this.initializationTime == -1) {
            this.initializationTime = j;
        }
    }

    public void sendAccessibilityEvent(int i) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.sendAccessibilityEvent(i);
        }
    }

    public boolean isMediaNotification() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow == null) {
            return false;
        }
        return expandableNotificationRow.isMediaRow();
    }

    public boolean isTopLevelChild() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isTopLevelChild();
    }

    public void resetUserExpansion() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.resetUserExpansion();
        }
    }

    public void freeContentViewWhenSafe(int i) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.freeContentViewWhenSafe(i);
        }
    }

    public void setAmbientPulsing(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setAmbientPulsing(z);
        }
    }

    public boolean rowExists() {
        return this.row != null;
    }

    public boolean isRowDismissed() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isDismissed();
    }

    public boolean isRowRemoved() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isRemoved();
    }

    public boolean isRemoved() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow == null || expandableNotificationRow.isRemoved();
    }

    public boolean isRowPinned() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isPinned();
    }

    public void setRowPinned(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setPinned(z);
        }
    }

    public boolean isRowHeadsUp() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isHeadsUp();
    }

    public void setHeadsUp(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setHeadsUp(z);
        }
    }

    public void setAmbientGoingAway(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setAmbientGoingAway(z);
        }
    }

    public boolean mustStayOnScreen() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.mustStayOnScreen();
    }

    public void setHeadsUpIsVisible() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setHeadsUpIsVisible();
        }
    }

    public ExpandableNotificationRow getHeadsUpAnimationView() {
        return this.row;
    }

    public void setUserLocked(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setUserLocked(z);
        }
    }

    public void setUserExpanded(boolean z, boolean z2) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setUserExpanded(z, z2);
        }
    }

    public void setGroupExpansionChanging(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setGroupExpansionChanging(z);
        }
    }

    public void notifyHeightChanged(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.notifyHeightChanged(z);
        }
    }

    public void closeRemoteInput() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.closeRemoteInput();
        }
    }

    public boolean areChildrenExpanded() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.areChildrenExpanded();
    }

    public boolean isGroupNotFullyVisible() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow == null || expandableNotificationRow.isGroupNotFullyVisible();
    }

    public NotificationGuts getGuts() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            return expandableNotificationRow.getGuts();
        }
        return null;
    }

    public void removeRow() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setRemoved();
        }
    }

    public boolean isSummaryWithChildren() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return expandableNotificationRow != null && expandableNotificationRow.isSummaryWithChildren();
    }

    public void setKeepInParent(boolean z) {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.setKeepInParent(z);
        }
    }

    public void onDensityOrFontScaleChanged() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.onDensityOrFontScaleChanged();
        }
    }

    public boolean areGutsExposed() {
        ExpandableNotificationRow expandableNotificationRow = this.row;
        return (expandableNotificationRow == null || expandableNotificationRow.getGuts() == null || !this.row.getGuts().isExposed()) ? false : true;
    }

    public boolean isChildInGroup() {
        return this.parent == null;
    }

    public boolean isClearable() {
        StatusBarNotification statusBarNotification = this.notification;
        if (statusBarNotification == null || !statusBarNotification.isClearable()) {
            return false;
        }
        List children = getChildren();
        if (children != null && children.size() > 0) {
            for (int i = 0; i < children.size(); i++) {
                if (!((NotificationEntry) children.get(i)).isClearable()) {
                    return false;
                }
            }
        }
        return true;
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean isExemptFromDndVisualSuppression() {
        if (isNotificationBlockedByPolicy(this.notification.getNotification())) {
            return false;
        }
        if ((this.notification.getNotification().flags & 64) != 0 || this.notification.getNotification().isMediaNotification()) {
            return true;
        }
        Boolean bool = this.mIsSystemNotification;
        if (bool == null || !bool.booleanValue()) {
            return false;
        }
        return true;
    }

    private boolean shouldSuppressVisualEffect(int i) {
        boolean z = false;
        if (isExemptFromDndVisualSuppression()) {
            return false;
        }
        if ((this.suppressedVisualEffects & i) != 0) {
            z = true;
        }
        return z;
    }

    public boolean shouldSuppressFullScreenIntent() {
        return shouldSuppressVisualEffect(4);
    }

    public boolean shouldSuppressPeek() {
        return shouldSuppressVisualEffect(16);
    }

    public boolean shouldSuppressStatusBar() {
        return shouldSuppressVisualEffect(32);
    }

    public boolean shouldSuppressAmbient() {
        return shouldSuppressVisualEffect(128);
    }

    public boolean shouldSuppressNotificationList() {
        return shouldSuppressVisualEffect(256);
    }

    private static boolean isNotificationBlockedByPolicy(Notification notification2) {
        return isCategory("call", notification2) || isCategory("msg", notification2) || isCategory("alarm", notification2) || isCategory("event", notification2) || isCategory("reminder", notification2);
    }

    private static boolean isCategory(String str, Notification notification2) {
        return Objects.equals(notification2.category, str);
    }

    public boolean isForegroundService() {
        return (this.notification.getNotification().flags & 64) != 0;
    }
}
