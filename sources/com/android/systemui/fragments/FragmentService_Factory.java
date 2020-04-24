package com.android.systemui.fragments;

import com.android.systemui.SystemUIFactory.SystemUIRootComponent;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class FragmentService_Factory implements Factory<FragmentService> {
    private final Provider<SystemUIRootComponent> rootComponentProvider;

    public FragmentService_Factory(Provider<SystemUIRootComponent> provider) {
        this.rootComponentProvider = provider;
    }

    public FragmentService get() {
        return provideInstance(this.rootComponentProvider);
    }

    public static FragmentService provideInstance(Provider<SystemUIRootComponent> provider) {
        return new FragmentService((SystemUIRootComponent) provider.get());
    }

    public static FragmentService_Factory create(Provider<SystemUIRootComponent> provider) {
        return new FragmentService_Factory(provider);
    }
}
