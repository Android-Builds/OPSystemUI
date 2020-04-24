package com.oneplus.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.oneplus.aod.OpAodNotificationIconAreaController;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import java.util.Objects;

public class OpNotificationIconAreaController {
    protected OpAodNotificationIconAreaController mAodNotificationIconCtrl;
    private final OpNotificationController mOpNotificationController = ((OpNotificationController) Dependency.get(OpNotificationController.class));

    /* access modifiers changed from: protected */
    public boolean shouldShowNotificationIcon(NotificationEntry notificationEntry, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) {
        StatusBarIconView statusBarIconView = notificationEntry.centeredIcon;
        if (z6 == (statusBarIconView != null && Objects.equals(statusBarIconView, getCenteredIconView()))) {
            return false;
        }
        if (getEntryManager().getNotificationData().isAmbient(notificationEntry.key) && !z) {
            return false;
        }
        if (z5 && notificationEntry.key.equals(getMediaManager().getMediaNotificationKey())) {
            return false;
        }
        if ((!z2 && !notificationEntry.isHighPriority()) || !notificationEntry.isTopLevelChild() || notificationEntry.getRow().getVisibility() == 8) {
            return false;
        }
        if (notificationEntry.isRowDismissed() && z3) {
            return false;
        }
        if (z4 && notificationEntry.isLastMessageFromReply()) {
            return false;
        }
        if ((!z || getFullyDark()) && notificationEntry.shouldSuppressStatusBar()) {
            return false;
        }
        if (z7) {
            Bundle bundle = null;
            if (notificationEntry.notification.getNotification() != null) {
                bundle = notificationEntry.notification.getNotification().extras;
            }
            return bundle == null || !bundle.getBoolean("hide_icon", false);
        }
    }

    /* access modifiers changed from: protected */
    public void updateTintForIconInternal(StatusBarIconView statusBarIconView, int i, Context context, ContrastColorUtil contrastColorUtil, Rect rect) {
        boolean equals = Boolean.TRUE.equals(statusBarIconView.getTag(R$id.icon_is_pre_L));
        boolean z = true;
        int i2 = 0;
        if (!this.mOpNotificationController.shouldColorizeIcon(statusBarIconView.getNotification() != null ? statusBarIconView.getNotification().getPackageName() : "") ? (equals || !OpUtils.isGlobalROM(context)) && !NotificationUtils.isGrayscale(statusBarIconView, contrastColorUtil) : equals && !NotificationUtils.isGrayscale(statusBarIconView, contrastColorUtil)) {
            z = false;
        }
        if (z) {
            i2 = DarkIconDispatcher.getTint(rect, statusBarIconView, i);
        }
        statusBarIconView.setStaticDrawableColor(i2);
        statusBarIconView.setDecorColor(i);
    }

    public void setAodIconController(OpAodNotificationIconAreaController opAodNotificationIconAreaController) {
        this.mAodNotificationIconCtrl = opAodNotificationIconAreaController;
    }

    private NotificationEntryManager getEntryManager() {
        return (NotificationEntryManager) OpReflectionUtils.getValue(NotificationIconAreaController.class, this, "mEntryManager");
    }

    private boolean getFullyDark() {
        return ((Boolean) OpReflectionUtils.getValue(NotificationIconAreaController.class, this, "mFullyDark")).booleanValue();
    }

    private NotificationMediaManager getMediaManager() {
        return (NotificationMediaManager) OpReflectionUtils.getValue(NotificationIconAreaController.class, this, "mMediaManager");
    }

    private StatusBarIconView getCenteredIconView() {
        return (StatusBarIconView) OpReflectionUtils.getValue(NotificationIconAreaController.class, this, "mCenteredIconView");
    }
}
