package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.ReadModeTile_Factory */
public final class ReadModeTile_Factory implements Factory<ReadModeTile> {
    private final Provider<QSHost> hostProvider;

    public ReadModeTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public ReadModeTile get() {
        return provideInstance(this.hostProvider);
    }

    public static ReadModeTile provideInstance(Provider<QSHost> provider) {
        return new ReadModeTile((QSHost) provider.get());
    }

    public static ReadModeTile_Factory create(Provider<QSHost> provider) {
        return new ReadModeTile_Factory(provider);
    }
}
