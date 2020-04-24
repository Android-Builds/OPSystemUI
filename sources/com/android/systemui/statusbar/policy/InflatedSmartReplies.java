package com.android.systemui.statusbar.policy;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.systemui.Dependency;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.DevicePolicyManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.statusbar.NotificationUiAdjustment;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartActions;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartReplies;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InflatedSmartReplies {
    private static final boolean DEBUG = Log.isLoggable("InflatedSmartReplies", 3);
    private final SmartRepliesAndActions mSmartRepliesAndActions;
    private final SmartReplyView mSmartReplyView;
    private final List<Button> mSmartSuggestionButtons;

    public static class SmartRepliesAndActions {
        public final SmartActions smartActions;
        public final SmartReplies smartReplies;

        SmartRepliesAndActions(SmartReplies smartReplies2, SmartActions smartActions2) {
            this.smartReplies = smartReplies2;
            this.smartActions = smartActions2;
        }

        public CharSequence[] getSmartReplies() {
            SmartReplies smartReplies2 = this.smartReplies;
            return smartReplies2 == null ? new CharSequence[0] : smartReplies2.choices;
        }

        public List<Action> getSmartActions() {
            SmartActions smartActions2 = this.smartActions;
            return smartActions2 == null ? Collections.emptyList() : smartActions2.actions;
        }
    }

    private InflatedSmartReplies(SmartReplyView smartReplyView, List<Button> list, SmartRepliesAndActions smartRepliesAndActions) {
        this.mSmartReplyView = smartReplyView;
        this.mSmartSuggestionButtons = list;
        this.mSmartRepliesAndActions = smartRepliesAndActions;
    }

    public SmartReplyView getSmartReplyView() {
        return this.mSmartReplyView;
    }

    public List<Button> getSmartSuggestionButtons() {
        return this.mSmartSuggestionButtons;
    }

    public SmartRepliesAndActions getSmartRepliesAndActions() {
        return this.mSmartRepliesAndActions;
    }

    public static InflatedSmartReplies inflate(Context context, NotificationEntry notificationEntry, SmartReplyConstants smartReplyConstants, SmartReplyController smartReplyController, HeadsUpManager headsUpManager, SmartRepliesAndActions smartRepliesAndActions) {
        SmartRepliesAndActions chooseSmartRepliesAndActions = chooseSmartRepliesAndActions(smartReplyConstants, notificationEntry);
        if (!shouldShowSmartReplyView(notificationEntry, chooseSmartRepliesAndActions)) {
            return new InflatedSmartReplies(null, null, chooseSmartRepliesAndActions);
        }
        boolean z = !areSuggestionsSimilar(smartRepliesAndActions, chooseSmartRepliesAndActions);
        SmartReplyView inflate = SmartReplyView.inflate(context);
        ArrayList arrayList = new ArrayList();
        SmartReplies smartReplies = chooseSmartRepliesAndActions.smartReplies;
        if (smartReplies != null) {
            arrayList.addAll(inflate.inflateRepliesFromRemoteInput(smartReplies, smartReplyController, notificationEntry, z));
        }
        SmartActions smartActions = chooseSmartRepliesAndActions.smartActions;
        if (smartActions != null) {
            arrayList.addAll(inflate.inflateSmartActions(smartActions, smartReplyController, notificationEntry, headsUpManager, z));
        }
        return new InflatedSmartReplies(inflate, arrayList, chooseSmartRepliesAndActions);
    }

    @VisibleForTesting
    static boolean areSuggestionsSimilar(SmartRepliesAndActions smartRepliesAndActions, SmartRepliesAndActions smartRepliesAndActions2) {
        if (smartRepliesAndActions == smartRepliesAndActions2) {
            return true;
        }
        if (smartRepliesAndActions == null || smartRepliesAndActions2 == null || !Arrays.equals(smartRepliesAndActions.getSmartReplies(), smartRepliesAndActions2.getSmartReplies())) {
            return false;
        }
        return !NotificationUiAdjustment.areDifferent(smartRepliesAndActions.getSmartActions(), smartRepliesAndActions2.getSmartActions());
    }

    public static boolean shouldShowSmartReplyView(NotificationEntry notificationEntry, SmartRepliesAndActions smartRepliesAndActions) {
        if ((smartRepliesAndActions.smartReplies != null || smartRepliesAndActions.smartActions != null) && !notificationEntry.notification.getNotification().extras.getBoolean("android.remoteInputSpinner", false) && !notificationEntry.notification.getNotification().extras.getBoolean("android.hideSmartReplies", false)) {
            return true;
        }
        return false;
    }

    public static SmartRepliesAndActions chooseSmartRepliesAndActions(SmartReplyConstants smartReplyConstants, NotificationEntry notificationEntry) {
        Notification notification = notificationEntry.notification.getNotification();
        boolean z = false;
        Pair findRemoteInputActionPair = notification.findRemoteInputActionPair(false);
        Pair findRemoteInputActionPair2 = notification.findRemoteInputActionPair(true);
        SmartActions smartActions = null;
        if (!smartReplyConstants.isEnabled()) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Smart suggestions not enabled, not adding suggestions for ");
                sb.append(notificationEntry.notification.getKey());
                Log.d("InflatedSmartReplies", sb.toString());
            }
            return new SmartRepliesAndActions(null, null);
        }
        boolean z2 = (!smartReplyConstants.requiresTargetingP() || notificationEntry.targetSdk >= 28) && findRemoteInputActionPair != null && !ArrayUtils.isEmpty(((RemoteInput) findRemoteInputActionPair.first).getChoices()) && ((Action) findRemoteInputActionPair.second).actionIntent != null;
        List contextualActions = notification.getContextualActions();
        boolean z3 = !contextualActions.isEmpty();
        SmartReplies smartReplies = z2 ? new SmartReplies(((RemoteInput) findRemoteInputActionPair.first).getChoices(), (RemoteInput) findRemoteInputActionPair.first, ((Action) findRemoteInputActionPair.second).actionIntent, false) : null;
        if (z3) {
            smartActions = new SmartActions(contextualActions, false);
        }
        if (!z2 && !z3) {
            if (!ArrayUtils.isEmpty(notificationEntry.systemGeneratedSmartReplies) && findRemoteInputActionPair2 != null && ((Action) findRemoteInputActionPair2.second).getAllowGeneratedReplies() && ((Action) findRemoteInputActionPair2.second).actionIntent != null) {
                smartReplies = new SmartReplies(notificationEntry.systemGeneratedSmartReplies, (RemoteInput) findRemoteInputActionPair2.first, ((Action) findRemoteInputActionPair2.second).actionIntent, true);
            }
            if (!ArrayUtils.isEmpty(notificationEntry.systemGeneratedSmartActions) && notification.getAllowSystemGeneratedContextualActions()) {
                z = true;
            }
            if (z) {
                List<Action> list = notificationEntry.systemGeneratedSmartActions;
                if (((ActivityManagerWrapper) Dependency.get(ActivityManagerWrapper.class)).isLockTaskKioskModeActive()) {
                    list = filterWhiteListedLockTaskApps(list);
                }
                smartActions = new SmartActions(list, true);
            }
        }
        return new SmartRepliesAndActions(smartReplies, smartActions);
    }

    private static List<Action> filterWhiteListedLockTaskApps(List<Action> list) {
        PackageManagerWrapper packageManagerWrapper = (PackageManagerWrapper) Dependency.get(PackageManagerWrapper.class);
        DevicePolicyManagerWrapper devicePolicyManagerWrapper = (DevicePolicyManagerWrapper) Dependency.get(DevicePolicyManagerWrapper.class);
        ArrayList arrayList = new ArrayList();
        for (Action action : list) {
            PendingIntent pendingIntent = action.actionIntent;
            if (pendingIntent != null) {
                ResolveInfo resolveActivity = packageManagerWrapper.resolveActivity(pendingIntent.getIntent(), 0);
                if (resolveActivity != null && devicePolicyManagerWrapper.isLockTaskPermitted(resolveActivity.activityInfo.packageName)) {
                    arrayList.add(action);
                }
            }
        }
        return arrayList;
    }

    public static boolean hasFreeformRemoteInput(NotificationEntry notificationEntry) {
        if (notificationEntry.notification.getNotification().findRemoteInputActionPair(true) != null) {
            return true;
        }
        return false;
    }
}
