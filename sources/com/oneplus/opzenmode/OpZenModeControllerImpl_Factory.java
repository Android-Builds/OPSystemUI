package com.oneplus.opzenmode;

import android.content.Context;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class OpZenModeControllerImpl_Factory implements Factory<OpZenModeControllerImpl> {
    private final Provider<Context> contextProvider;

    public OpZenModeControllerImpl_Factory(Provider<Context> provider) {
        this.contextProvider = provider;
    }

    public OpZenModeControllerImpl get() {
        return provideInstance(this.contextProvider);
    }

    public static OpZenModeControllerImpl provideInstance(Provider<Context> provider) {
        return new OpZenModeControllerImpl((Context) provider.get());
    }

    public static OpZenModeControllerImpl_Factory create(Provider<Context> provider) {
        return new OpZenModeControllerImpl_Factory(provider);
    }
}
