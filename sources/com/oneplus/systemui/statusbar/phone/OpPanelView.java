package com.oneplus.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector.OnDoubleTapListener;
import android.widget.FrameLayout;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.NotificationEntryManager;

public abstract class OpPanelView extends FrameLayout implements OnDoubleTapListener {
    private NotificationEntryManager mEntryManager = ((NotificationEntryManager) Dependency.get(NotificationEntryManager.class));
    private StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));

    public OpPanelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public boolean checkRunPeekAnimation() {
        return !showEmptyShadeView();
    }

    private boolean showEmptyShadeView() {
        if (this.mStatusBarStateController.getState() == 1 || this.mEntryManager.getNotificationData().getActiveNotifications().size() != 0) {
            return false;
        }
        return true;
    }
}
