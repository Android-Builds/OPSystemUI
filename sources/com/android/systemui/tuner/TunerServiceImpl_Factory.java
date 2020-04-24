package com.android.systemui.tuner;

import android.content.Context;
import android.os.Handler;
import com.android.systemui.util.leak.LeakDetector;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class TunerServiceImpl_Factory implements Factory<TunerServiceImpl> {
    private final Provider<Handler> bgHandlerProvider;
    private final Provider<Context> contextProvider;
    private final Provider<LeakDetector> leakDetectorProvider;

    public TunerServiceImpl_Factory(Provider<Context> provider, Provider<Handler> provider2, Provider<LeakDetector> provider3) {
        this.contextProvider = provider;
        this.bgHandlerProvider = provider2;
        this.leakDetectorProvider = provider3;
    }

    public TunerServiceImpl get() {
        return provideInstance(this.contextProvider, this.bgHandlerProvider, this.leakDetectorProvider);
    }

    public static TunerServiceImpl provideInstance(Provider<Context> provider, Provider<Handler> provider2, Provider<LeakDetector> provider3) {
        return new TunerServiceImpl((Context) provider.get(), (Handler) provider2.get(), (LeakDetector) provider3.get());
    }

    public static TunerServiceImpl_Factory create(Provider<Context> provider, Provider<Handler> provider2, Provider<LeakDetector> provider3) {
        return new TunerServiceImpl_Factory(provider, provider2, provider3);
    }
}
