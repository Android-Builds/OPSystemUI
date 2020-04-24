package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Iterator;

public class HighlightHintControllerImpl implements HighlightHintController, Callback {
    private int mBarState;
    private int mBgColor = 0;
    private final ArrayList<OnHighlightHintStateChangeListener> mCallbacks = new ArrayList<>();
    private int mCarModeBgColor = 0;
    private boolean mCarModeShow = false;
    private boolean mExpandedVisible = false;
    private boolean mHeadUpShow = false;
    private boolean mHighlightHintShow = false;
    private KeyguardMonitorImpl mKeyguardMonitor = ((KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class));
    private boolean mKeyguardShow = false;
    private NotificationData mNotificationData;

    public boolean showOvalLayout() {
        return true;
    }

    public HighlightHintControllerImpl() {
        this.mKeyguardMonitor.addCallback((Callback) this);
    }

    public void onKeyguardShowingChanged() {
        this.mKeyguardShow = this.mKeyguardMonitor.isShowing();
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardShowingChanged mKeyguardShow:");
            sb.append(this.mKeyguardShow);
            Log.d("HighlightHintCtrl", sb.toString());
        }
        onCarModeHighlightHintStateChange();
    }

    public void onNotificationUpdate(NotificationData notificationData) {
        this.mNotificationData = notificationData;
        NotificationData notificationData2 = this.mNotificationData;
        if (!(notificationData2 == null || notificationData2.getHighlightHintNotification() == null || this.mNotificationData.getHighlightHintNotification().getNotification() == null)) {
            this.mBgColor = this.mNotificationData.getHighlightHintNotification().getNotification().getBackgroundColorOnStatusBar();
        }
        onHighlightHintStateChange();
        onHighlightHintInfoUpdate();
        NotificationData notificationData3 = this.mNotificationData;
        if (!(notificationData3 == null || notificationData3.getCarModeHighlightHintNotification() == null || this.mNotificationData.getCarModeHighlightHintNotification().getNotification() == null)) {
            this.mCarModeBgColor = this.mNotificationData.getCarModeHighlightHintNotification().getNotification().getBackgroundColorOnStatusBar();
        }
        onCarModeHighlightHintInfoUpdate();
        onCarModeHighlightHintStateChange();
    }

    public void onHeadUpPinnedModeChange(boolean z) {
        if (this.mHeadUpShow != z) {
            this.mHeadUpShow = z;
            onHighlightHintStateChange();
            onCarModeHighlightHintStateChange();
        }
    }

    public void onExpandedVisibleChange(boolean z) {
        if (this.mExpandedVisible != z) {
            this.mExpandedVisible = z;
            onHighlightHintStateChange();
            onCarModeHighlightHintStateChange();
        }
    }

    public void onBarStatechange(int i) {
        if (this.mBarState != i) {
            this.mBarState = i;
            StringBuilder sb = new StringBuilder();
            sb.append("onBarStatechange barstate:");
            sb.append(i);
            Log.d("HighlightHintCtrl", sb.toString());
            onHighlightHintStateChange();
            onCarModeHighlightHintStateChange();
        }
    }

    private void onHighlightHintStateChange() {
        boolean shouldShowHighlightHint = shouldShowHighlightHint();
        if (shouldShowHighlightHint != this.mHighlightHintShow) {
            this.mHighlightHintShow = shouldShowHighlightHint;
            dumpInfo();
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((OnHighlightHintStateChangeListener) it.next()).onHighlightHintStateChange();
            }
        }
    }

    private void onHighlightHintInfoUpdate() {
        Iterator it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            ((OnHighlightHintStateChangeListener) it.next()).onHighlightHintInfoChange();
        }
    }

    private void onCarModeHighlightHintStateChange() {
        boolean shouldShowCarModeHighlightHint = shouldShowCarModeHighlightHint();
        if (shouldShowCarModeHighlightHint != this.mCarModeShow) {
            StringBuilder sb = new StringBuilder();
            sb.append("onCarModeHighlightHintStateChange show:");
            sb.append(shouldShowCarModeHighlightHint);
            Log.d("HighlightHintCtrl", sb.toString());
            this.mCarModeShow = shouldShowCarModeHighlightHint;
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((OnHighlightHintStateChangeListener) it.next()).onCarModeHighlightHintStateChange(this.mCarModeShow);
            }
        }
    }

    private void onCarModeHighlightHintInfoUpdate() {
        Iterator it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            ((OnHighlightHintStateChangeListener) it.next()).onCarModeHighlightHintInfoChange();
        }
    }

    public boolean isHighLightHintShow() {
        return this.mHighlightHintShow;
    }

    public NotificationData getNotificationData() {
        return this.mNotificationData;
    }

    public int getHighlighColor() {
        return this.mBgColor;
    }

    public boolean isCarModeHighlightHintSHow() {
        return this.mCarModeShow;
    }

    public boolean shouldShowHighlightHint() {
        boolean z = false;
        if (this.mNotificationData == null) {
            return false;
        }
        boolean z2 = this.mExpandedVisible && this.mBarState == 0;
        if (this.mNotificationData.showNotification() && !this.mHeadUpShow && !z2) {
            z = true;
        }
        return z;
    }

    private boolean shouldShowCarModeHighlightHint() {
        NotificationData notificationData = this.mNotificationData;
        boolean z = false;
        if (notificationData == null) {
            return false;
        }
        if (notificationData.showCarModeNotification() && !this.mKeyguardShow) {
            z = true;
        }
        return z;
    }

    public void launchCarModeAp(Context context) {
        Log.d("HighlightHintCtrl", "launchCarModeAp");
        StatusBarNotification carModeHighlightHintNotification = this.mNotificationData.getCarModeHighlightHintNotification();
        Intent intentOnStatusBar = carModeHighlightHintNotification != null ? carModeHighlightHintNotification.getNotification().getIntentOnStatusBar() : null;
        if (intentOnStatusBar != null && context != null) {
            context.sendBroadcast(intentOnStatusBar);
        }
    }

    public void addCallback(OnHighlightHintStateChangeListener onHighlightHintStateChangeListener) {
        this.mCallbacks.add(onHighlightHintStateChangeListener);
        onHighlightHintStateChangeListener.onHighlightHintStateChange();
        onHighlightHintStateChangeListener.onHighlightHintInfoChange();
    }

    public void removeCallback(OnHighlightHintStateChangeListener onHighlightHintStateChangeListener) {
        this.mCallbacks.remove(onHighlightHintStateChangeListener);
    }

    private void dumpInfo() {
        if (OpUtils.DEBUG_ONEPLUS) {
            boolean z = this.mExpandedVisible && this.mBarState == 0;
            StringBuilder sb = new StringBuilder();
            sb.append("mHighlightHintShow:");
            sb.append(this.mHighlightHintShow);
            sb.append(" showNotification:");
            sb.append(this.mNotificationData.showNotification());
            sb.append(" HeadsUp:");
            sb.append(this.mHeadUpShow);
            sb.append(" expanededAfterUnlock:");
            sb.append(z);
            Log.i("HighlightHintCtrl", sb.toString());
        }
    }
}
