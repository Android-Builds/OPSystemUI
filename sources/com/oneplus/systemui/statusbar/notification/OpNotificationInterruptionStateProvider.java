package com.oneplus.systemui.statusbar.notification;

import android.service.notification.StatusBarNotification;
import com.android.systemui.Dependency;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.util.OpUtils;

public class OpNotificationInterruptionStateProvider {
    protected static final boolean OP_DEBUG = OpUtils.DEBUG_ONEPLUS;
    protected final OpNotificationController mOpNotificationController = ((OpNotificationController) Dependency.get(OpNotificationController.class));

    /* access modifiers changed from: protected */
    public boolean shouldHeadsUp(StatusBarNotification statusBarNotification) {
        return this.mOpNotificationController.canHeadsUp(statusBarNotification) == -1;
    }
}
