package com.android.systemui.statusbar;

import android.service.notification.StatusBarNotification;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;

public interface NotificationLockscreenUserManager {

    public interface UserChangedListener {
        void onUserChanged(int i);
    }

    int getCurrentUserId();

    boolean isAnyProfilePublicMode();

    boolean isCurrentProfile(int i);

    boolean isLockscreenPublicMode(int i);

    boolean isSecure();

    boolean needsRedaction(NotificationEntry notificationEntry);

    boolean needsSeparateWorkChallenge(int i) {
        return false;
    }

    void setUpWithPresenter(NotificationPresenter notificationPresenter);

    boolean shouldAllowLockscreenRemoteInput();

    boolean shouldHideNotifications(int i);

    boolean shouldHideNotifications(String str);

    boolean shouldShowLockscreenNotifications();

    boolean shouldShowOnKeyguard(StatusBarNotification statusBarNotification);

    void updatePublicMode();

    boolean userAllowsPrivateNotificationsInPublic(int i);
}
