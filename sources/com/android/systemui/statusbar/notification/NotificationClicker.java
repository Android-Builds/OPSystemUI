package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.systemui.DejankUtils;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.phone.ShadeController;

public final class NotificationClicker implements OnClickListener {
    private final BubbleController mBubbleController;
    private final NotificationActivityStarter mNotificationActivityStarter;
    private final ShadeController mShadeController;

    public NotificationClicker(ShadeController shadeController, BubbleController bubbleController, NotificationActivityStarter notificationActivityStarter) {
        this.mShadeController = shadeController;
        this.mBubbleController = bubbleController;
        this.mNotificationActivityStarter = notificationActivityStarter;
    }

    public void onClick(View view) {
        String str = "NotificationClicker";
        if (!(view instanceof ExpandableNotificationRow)) {
            Log.e(str, "NotificationClicker called on a view that is not a notification row.");
            return;
        }
        this.mShadeController.wakeUpIfDozing(SystemClock.uptimeMillis(), view, "NOTIFICATION_CLICK");
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
        if (statusBarNotification == null) {
            Log.e(str, "NotificationClicker called on an unclickable notification,");
        } else if (isMenuVisible(expandableNotificationRow)) {
            expandableNotificationRow.animateTranslateNotification(0.0f);
        } else if (expandableNotificationRow.isChildInGroup() && isMenuVisible(expandableNotificationRow.getNotificationParent())) {
            expandableNotificationRow.getNotificationParent().animateTranslateNotification(0.0f);
        } else if (!expandableNotificationRow.isSummaryWithChildren() || !expandableNotificationRow.areChildrenExpanded()) {
            expandableNotificationRow.setJustClicked(true);
            DejankUtils.postAfterTraversal(new Runnable() {
                public final void run() {
                    ExpandableNotificationRow.this.setJustClicked(false);
                }
            });
            if (!expandableNotificationRow.getEntry().isBubble()) {
                this.mBubbleController.collapseStack();
            }
            this.mNotificationActivityStarter.onNotificationClicked(statusBarNotification, expandableNotificationRow);
        }
    }

    private boolean isMenuVisible(ExpandableNotificationRow expandableNotificationRow) {
        return expandableNotificationRow.getProvider() != null && expandableNotificationRow.getProvider().isMenuVisible();
    }

    public void register(ExpandableNotificationRow expandableNotificationRow, StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        if (notification.contentIntent == null && notification.fullScreenIntent == null && !expandableNotificationRow.getEntry().isBubble()) {
            expandableNotificationRow.setOnClickListener(null);
        } else {
            expandableNotificationRow.setOnClickListener(this);
        }
    }
}
