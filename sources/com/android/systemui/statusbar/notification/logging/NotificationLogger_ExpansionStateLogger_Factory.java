package com.android.systemui.statusbar.notification.logging;

import com.android.systemui.UiOffloadThread;
import com.android.systemui.statusbar.notification.logging.NotificationLogger.ExpansionStateLogger;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationLogger_ExpansionStateLogger_Factory implements Factory<ExpansionStateLogger> {
    private final Provider<UiOffloadThread> uiOffloadThreadProvider;

    public NotificationLogger_ExpansionStateLogger_Factory(Provider<UiOffloadThread> provider) {
        this.uiOffloadThreadProvider = provider;
    }

    public ExpansionStateLogger get() {
        return provideInstance(this.uiOffloadThreadProvider);
    }

    public static ExpansionStateLogger provideInstance(Provider<UiOffloadThread> provider) {
        return new ExpansionStateLogger((UiOffloadThread) provider.get());
    }

    public static NotificationLogger_ExpansionStateLogger_Factory create(Provider<UiOffloadThread> provider) {
        return new NotificationLogger_ExpansionStateLogger_Factory(provider);
    }
}
