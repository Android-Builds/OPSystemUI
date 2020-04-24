package com.android.systemui.statusbar.phone;

import android.content.Context;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.policy.CallbackController;

public interface HighlightHintController extends CallbackController<OnHighlightHintStateChangeListener> {

    public interface OnHighlightHintStateChangeListener {
        void onCarModeHighlightHintInfoChange() {
        }

        void onCarModeHighlightHintStateChange(boolean z) {
        }

        void onHighlightHintInfoChange() {
        }

        void onHighlightHintStateChange() {
        }
    }

    int getHighlighColor();

    NotificationData getNotificationData();

    boolean isCarModeHighlightHintSHow();

    boolean isHighLightHintShow();

    void launchCarModeAp(Context context);

    void onBarStatechange(int i);

    void onExpandedVisibleChange(boolean z);

    void onHeadUpPinnedModeChange(boolean z);

    void onNotificationUpdate(NotificationData notificationData);

    boolean showOvalLayout();
}
