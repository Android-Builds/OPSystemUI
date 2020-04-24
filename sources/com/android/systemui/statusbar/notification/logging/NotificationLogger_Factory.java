package com.android.systemui.statusbar.notification.logging;

import com.android.systemui.UiOffloadThread;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.logging.NotificationLogger.ExpansionStateLogger;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationLogger_Factory implements Factory<NotificationLogger> {
    private final Provider<NotificationEntryManager> entryManagerProvider;
    private final Provider<ExpansionStateLogger> expansionStateLoggerProvider;
    private final Provider<NotificationListener> notificationListenerProvider;
    private final Provider<StatusBarStateController> statusBarStateControllerProvider;
    private final Provider<UiOffloadThread> uiOffloadThreadProvider;

    public NotificationLogger_Factory(Provider<NotificationListener> provider, Provider<UiOffloadThread> provider2, Provider<NotificationEntryManager> provider3, Provider<StatusBarStateController> provider4, Provider<ExpansionStateLogger> provider5) {
        this.notificationListenerProvider = provider;
        this.uiOffloadThreadProvider = provider2;
        this.entryManagerProvider = provider3;
        this.statusBarStateControllerProvider = provider4;
        this.expansionStateLoggerProvider = provider5;
    }

    public NotificationLogger get() {
        return provideInstance(this.notificationListenerProvider, this.uiOffloadThreadProvider, this.entryManagerProvider, this.statusBarStateControllerProvider, this.expansionStateLoggerProvider);
    }

    public static NotificationLogger provideInstance(Provider<NotificationListener> provider, Provider<UiOffloadThread> provider2, Provider<NotificationEntryManager> provider3, Provider<StatusBarStateController> provider4, Provider<ExpansionStateLogger> provider5) {
        NotificationLogger notificationLogger = new NotificationLogger((NotificationListener) provider.get(), (UiOffloadThread) provider2.get(), (NotificationEntryManager) provider3.get(), (StatusBarStateController) provider4.get(), (ExpansionStateLogger) provider5.get());
        return notificationLogger;
    }

    public static NotificationLogger_Factory create(Provider<NotificationListener> provider, Provider<UiOffloadThread> provider2, Provider<NotificationEntryManager> provider3, Provider<StatusBarStateController> provider4, Provider<ExpansionStateLogger> provider5) {
        NotificationLogger_Factory notificationLogger_Factory = new NotificationLogger_Factory(provider, provider2, provider3, provider4, provider5);
        return notificationLogger_Factory;
    }
}
