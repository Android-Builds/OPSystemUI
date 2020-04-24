package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.OPDndTile_Factory */
public final class OPDndTile_Factory implements Factory<OPDndTile> {
    private final Provider<QSHost> hostProvider;

    public OPDndTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public OPDndTile get() {
        return provideInstance(this.hostProvider);
    }

    public static OPDndTile provideInstance(Provider<QSHost> provider) {
        return new OPDndTile((QSHost) provider.get());
    }

    public static OPDndTile_Factory create(Provider<QSHost> provider) {
        return new OPDndTile_Factory(provider);
    }
}
