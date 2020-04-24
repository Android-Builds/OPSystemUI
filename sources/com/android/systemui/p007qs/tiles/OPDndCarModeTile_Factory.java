package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.OPDndCarModeTile_Factory */
public final class OPDndCarModeTile_Factory implements Factory<OPDndCarModeTile> {
    private final Provider<QSHost> hostProvider;

    public OPDndCarModeTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public OPDndCarModeTile get() {
        return provideInstance(this.hostProvider);
    }

    public static OPDndCarModeTile provideInstance(Provider<QSHost> provider) {
        return new OPDndCarModeTile((QSHost) provider.get());
    }

    public static OPDndCarModeTile_Factory create(Provider<QSHost> provider) {
        return new OPDndCarModeTile_Factory(provider);
    }
}
