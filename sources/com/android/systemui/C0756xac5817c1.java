package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

/* renamed from: com.android.systemui.SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory */
public final class C0756xac5817c1 implements Factory<NotificationInterruptionStateProvider> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public C0756xac5817c1(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    public NotificationInterruptionStateProvider get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static NotificationInterruptionStateProvider provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideNotificationInterruptionStateProvider(systemUIFactory, (Context) provider.get());
    }

    public static C0756xac5817c1 create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new C0756xac5817c1(systemUIFactory, provider);
    }

    public static NotificationInterruptionStateProvider proxyProvideNotificationInterruptionStateProvider(SystemUIFactory systemUIFactory, Context context) {
        NotificationInterruptionStateProvider provideNotificationInterruptionStateProvider = systemUIFactory.provideNotificationInterruptionStateProvider(context);
        Preconditions.checkNotNull(provideNotificationInterruptionStateProvider, "Cannot return null from a non-@Nullable @Provides method");
        return provideNotificationInterruptionStateProvider;
    }
}
