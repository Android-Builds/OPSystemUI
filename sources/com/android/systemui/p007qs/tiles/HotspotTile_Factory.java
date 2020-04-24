package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.HotspotTile_Factory */
public final class HotspotTile_Factory implements Factory<HotspotTile> {
    private final Provider<DataSaverController> dataSaverControllerProvider;
    private final Provider<QSHost> hostProvider;
    private final Provider<HotspotController> hotspotControllerProvider;

    public HotspotTile_Factory(Provider<QSHost> provider, Provider<HotspotController> provider2, Provider<DataSaverController> provider3) {
        this.hostProvider = provider;
        this.hotspotControllerProvider = provider2;
        this.dataSaverControllerProvider = provider3;
    }

    public HotspotTile get() {
        return provideInstance(this.hostProvider, this.hotspotControllerProvider, this.dataSaverControllerProvider);
    }

    public static HotspotTile provideInstance(Provider<QSHost> provider, Provider<HotspotController> provider2, Provider<DataSaverController> provider3) {
        return new HotspotTile((QSHost) provider.get(), (HotspotController) provider2.get(), (DataSaverController) provider3.get());
    }

    public static HotspotTile_Factory create(Provider<QSHost> provider, Provider<HotspotController> provider2, Provider<DataSaverController> provider3) {
        return new HotspotTile_Factory(provider, provider2, provider3);
    }
}
