package com.android.systemui.classifier;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.DeviceConfig.OnPropertiesChangedListener;
import android.provider.DeviceConfig.Properties;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.FalsingPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.shared.plugins.PluginManager;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

public class FalsingManagerProxy implements FalsingManager {
    /* access modifiers changed from: private */
    public FalsingManager mInternalFalsingManager;
    private final Handler mMainHandler;

    FalsingManagerProxy(final Context context, PluginManager pluginManager, Handler handler) {
        this.mMainHandler = handler;
        DeviceConfig.addOnPropertiesChangedListener("systemui", new Executor() {
            public final void execute(Runnable runnable) {
                FalsingManagerProxy.this.lambda$new$0$FalsingManagerProxy(runnable);
            }
        }, new OnPropertiesChangedListener(context) {
            private final /* synthetic */ Context f$1;

            {
                this.f$1 = r2;
            }

            public final void onPropertiesChanged(Properties properties) {
                FalsingManagerProxy.this.lambda$new$1$FalsingManagerProxy(this.f$1, properties);
            }
        });
        setupFalsingManager(context);
        pluginManager.addPluginListener(new PluginListener<FalsingPlugin>() {
            public void onPluginConnected(FalsingPlugin falsingPlugin, Context context) {
                FalsingManager falsingManager = falsingPlugin.getFalsingManager(context);
                if (falsingManager != null) {
                    FalsingManagerProxy.this.mInternalFalsingManager.cleanup();
                    FalsingManagerProxy.this.mInternalFalsingManager = falsingManager;
                }
            }

            public void onPluginDisconnected(FalsingPlugin falsingPlugin) {
                FalsingManagerProxy.this.mInternalFalsingManager = new FalsingManagerImpl(context);
            }
        }, FalsingPlugin.class);
    }

    public /* synthetic */ void lambda$new$0$FalsingManagerProxy(Runnable runnable) {
        this.mMainHandler.post(runnable);
    }

    public /* synthetic */ void lambda$new$1$FalsingManagerProxy(Context context, Properties properties) {
        onDeviceConfigPropertiesChanged(context, properties.getNamespace());
    }

    private void onDeviceConfigPropertiesChanged(Context context, String str) {
        if ("systemui".equals(str)) {
            setupFalsingManager(context);
        }
    }

    @VisibleForTesting
    public void setupFalsingManager(Context context) {
        DeviceConfig.getBoolean("systemui", "brightline_falsing_manager_enabled", true);
        FalsingManager falsingManager = this.mInternalFalsingManager;
        if (falsingManager != null) {
            falsingManager.cleanup();
        }
        this.mInternalFalsingManager = new FalsingManagerImpl(context);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public FalsingManager getInternalFalsingManager() {
        return this.mInternalFalsingManager;
    }

    public void onSucccessfulUnlock() {
        this.mInternalFalsingManager.onSucccessfulUnlock();
    }

    public void onNotificationActive() {
        this.mInternalFalsingManager.onNotificationActive();
    }

    public void setShowingAod(boolean z) {
        this.mInternalFalsingManager.setShowingAod(z);
    }

    public void onNotificatonStartDraggingDown() {
        this.mInternalFalsingManager.onNotificatonStartDraggingDown();
    }

    public boolean isUnlockingDisabled() {
        return this.mInternalFalsingManager.isUnlockingDisabled();
    }

    public boolean isFalseTouch() {
        return this.mInternalFalsingManager.isFalseTouch();
    }

    public void onNotificatonStopDraggingDown() {
        this.mInternalFalsingManager.onNotificatonStartDraggingDown();
    }

    public void setNotificationExpanded() {
        this.mInternalFalsingManager.setNotificationExpanded();
    }

    public boolean isClassiferEnabled() {
        return this.mInternalFalsingManager.isClassiferEnabled();
    }

    public void onQsDown() {
        this.mInternalFalsingManager.onQsDown();
    }

    public void setQsExpanded(boolean z) {
        this.mInternalFalsingManager.setQsExpanded(z);
    }

    public boolean shouldEnforceBouncer() {
        return this.mInternalFalsingManager.shouldEnforceBouncer();
    }

    public void onTrackingStarted(boolean z) {
        this.mInternalFalsingManager.onTrackingStarted(z);
    }

    public void onTrackingStopped() {
        this.mInternalFalsingManager.onTrackingStopped();
    }

    public void onLeftAffordanceOn() {
        this.mInternalFalsingManager.onLeftAffordanceOn();
    }

    public void onCameraOn() {
        this.mInternalFalsingManager.onCameraOn();
    }

    public void onAffordanceSwipingStarted(boolean z) {
        this.mInternalFalsingManager.onAffordanceSwipingStarted(z);
    }

    public void onAffordanceSwipingAborted() {
        this.mInternalFalsingManager.onAffordanceSwipingAborted();
    }

    public void onStartExpandingFromPulse() {
        this.mInternalFalsingManager.onStartExpandingFromPulse();
    }

    public void onExpansionFromPulseStopped() {
        this.mInternalFalsingManager.onExpansionFromPulseStopped();
    }

    public Uri reportRejectedTouch() {
        return this.mInternalFalsingManager.reportRejectedTouch();
    }

    public void onScreenOnFromTouch() {
        this.mInternalFalsingManager.onScreenOnFromTouch();
    }

    public boolean isReportingEnabled() {
        return this.mInternalFalsingManager.isReportingEnabled();
    }

    public void onUnlockHintStarted() {
        this.mInternalFalsingManager.onUnlockHintStarted();
    }

    public void onCameraHintStarted() {
        this.mInternalFalsingManager.onCameraHintStarted();
    }

    public void onLeftAffordanceHintStarted() {
        this.mInternalFalsingManager.onLeftAffordanceHintStarted();
    }

    public void onScreenTurningOn() {
        this.mInternalFalsingManager.onScreenTurningOn();
    }

    public void onScreenOff() {
        this.mInternalFalsingManager.onScreenOff();
    }

    public void onNotificatonStopDismissing() {
        this.mInternalFalsingManager.onNotificatonStopDismissing();
    }

    public void onNotificationDismissed() {
        this.mInternalFalsingManager.onNotificationDismissed();
    }

    public void onNotificatonStartDismissing() {
        this.mInternalFalsingManager.onNotificatonStartDismissing();
    }

    public void onNotificationDoubleTap(boolean z, float f, float f2) {
        this.mInternalFalsingManager.onNotificationDoubleTap(z, f, f2);
    }

    public void onBouncerShown() {
        this.mInternalFalsingManager.onBouncerShown();
    }

    public void onBouncerHidden() {
        this.mInternalFalsingManager.onBouncerHidden();
    }

    public void onTouchEvent(MotionEvent motionEvent, int i, int i2) {
        this.mInternalFalsingManager.onTouchEvent(motionEvent, i, i2);
    }

    public void dump(PrintWriter printWriter) {
        this.mInternalFalsingManager.dump(printWriter);
    }

    public void cleanup() {
        this.mInternalFalsingManager.cleanup();
    }
}
