package com.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.notification.collection.NotificationData.KeyguardEnvironment;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class SystemUIFactory_ProvideKeyguardEnvironmentFactory implements Factory<KeyguardEnvironment> {
    private final Provider<Context> contextProvider;
    private final SystemUIFactory module;

    public SystemUIFactory_ProvideKeyguardEnvironmentFactory(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        this.module = systemUIFactory;
        this.contextProvider = provider;
    }

    public KeyguardEnvironment get() {
        return provideInstance(this.module, this.contextProvider);
    }

    public static KeyguardEnvironment provideInstance(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return proxyProvideKeyguardEnvironment(systemUIFactory, (Context) provider.get());
    }

    public static SystemUIFactory_ProvideKeyguardEnvironmentFactory create(SystemUIFactory systemUIFactory, Provider<Context> provider) {
        return new SystemUIFactory_ProvideKeyguardEnvironmentFactory(systemUIFactory, provider);
    }

    public static KeyguardEnvironment proxyProvideKeyguardEnvironment(SystemUIFactory systemUIFactory, Context context) {
        KeyguardEnvironment provideKeyguardEnvironment = systemUIFactory.provideKeyguardEnvironment(context);
        Preconditions.checkNotNull(provideKeyguardEnvironment, "Cannot return null from a non-@Nullable @Provides method");
        return provideKeyguardEnvironment;
    }
}
