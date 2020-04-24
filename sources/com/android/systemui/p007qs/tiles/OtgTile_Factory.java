package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.OtgTile_Factory */
public final class OtgTile_Factory implements Factory<OtgTile> {
    private final Provider<QSHost> hostProvider;

    public OtgTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public OtgTile get() {
        return provideInstance(this.hostProvider);
    }

    public static OtgTile provideInstance(Provider<QSHost> provider) {
        return new OtgTile((QSHost) provider.get());
    }

    public static OtgTile_Factory create(Provider<QSHost> provider) {
        return new OtgTile_Factory(provider);
    }
}
