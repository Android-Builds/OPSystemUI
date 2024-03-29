package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.VPNTile_Factory */
public final class VPNTile_Factory implements Factory<VPNTile> {
    private final Provider<QSHost> hostProvider;

    public VPNTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public VPNTile get() {
        return provideInstance(this.hostProvider);
    }

    public static VPNTile provideInstance(Provider<QSHost> provider) {
        return new VPNTile((QSHost) provider.get());
    }

    public static VPNTile_Factory create(Provider<QSHost> provider) {
        return new VPNTile_Factory(provider);
    }
}
