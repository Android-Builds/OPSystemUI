package com.android.systemui.p007qs.tileimpl;

import com.android.systemui.p007qs.tiles.AirplaneModeTile;
import com.android.systemui.p007qs.tiles.BatterySaverTile;
import com.android.systemui.p007qs.tiles.BluetoothTile;
import com.android.systemui.p007qs.tiles.CastTile;
import com.android.systemui.p007qs.tiles.CellularTile;
import com.android.systemui.p007qs.tiles.ColorInversionTile;
import com.android.systemui.p007qs.tiles.DataSaverTile;
import com.android.systemui.p007qs.tiles.DataSwitchTile;
import com.android.systemui.p007qs.tiles.DndTile;
import com.android.systemui.p007qs.tiles.FlashlightTile;
import com.android.systemui.p007qs.tiles.GameModeTile;
import com.android.systemui.p007qs.tiles.HotspotTile;
import com.android.systemui.p007qs.tiles.LocationTile;
import com.android.systemui.p007qs.tiles.NfcTile;
import com.android.systemui.p007qs.tiles.NightDisplayTile;
import com.android.systemui.p007qs.tiles.OPDndCarModeTile;
import com.android.systemui.p007qs.tiles.OPDndTile;
import com.android.systemui.p007qs.tiles.OtgTile;
import com.android.systemui.p007qs.tiles.ReadModeTile;
import com.android.systemui.p007qs.tiles.RotationLockTile;
import com.android.systemui.p007qs.tiles.UiModeNightTile;
import com.android.systemui.p007qs.tiles.UserTile;
import com.android.systemui.p007qs.tiles.VPNTile;
import com.android.systemui.p007qs.tiles.WifiTile;
import com.android.systemui.p007qs.tiles.WorkModeTile;
import com.android.systemui.util.leak.GarbageMonitor.MemoryTile;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tileimpl.QSFactoryImpl_Factory */
public final class QSFactoryImpl_Factory implements Factory<QSFactoryImpl> {
    private final Provider<AirplaneModeTile> airplaneModeTileProvider;
    private final Provider<BatterySaverTile> batterySaverTileProvider;
    private final Provider<BluetoothTile> bluetoothTileProvider;
    private final Provider<CastTile> castTileProvider;
    private final Provider<CellularTile> cellularTileProvider;
    private final Provider<ColorInversionTile> colorInversionTileProvider;
    private final Provider<DataSaverTile> dataSaverTileProvider;
    private final Provider<DataSwitchTile> dataSwitchTileProvider;
    private final Provider<DndTile> dndTileProvider;
    private final Provider<FlashlightTile> flashlightTileProvider;
    private final Provider<GameModeTile> gameModeTileProvider;
    private final Provider<HotspotTile> hotspotTileProvider;
    private final Provider<LocationTile> locationTileProvider;
    private final Provider<MemoryTile> memoryTileProvider;
    private final Provider<NfcTile> nfcTileProvider;
    private final Provider<NightDisplayTile> nightDisplayTileProvider;
    private final Provider<OPDndCarModeTile> opDndCarModeTileProvider;
    private final Provider<OPDndTile> opDndTileProvider;
    private final Provider<OtgTile> otgTileProvider;
    private final Provider<ReadModeTile> readModeTileProvider;
    private final Provider<RotationLockTile> rotationLockTileProvider;
    private final Provider<UiModeNightTile> uiModeNightTileProvider;
    private final Provider<UserTile> userTileProvider;
    private final Provider<VPNTile> vpnTileProvider;
    private final Provider<WifiTile> wifiTileProvider;
    private final Provider<WorkModeTile> workModeTileProvider;

    public QSFactoryImpl_Factory(Provider<WifiTile> provider, Provider<BluetoothTile> provider2, Provider<CellularTile> provider3, Provider<DndTile> provider4, Provider<ColorInversionTile> provider5, Provider<AirplaneModeTile> provider6, Provider<WorkModeTile> provider7, Provider<RotationLockTile> provider8, Provider<FlashlightTile> provider9, Provider<LocationTile> provider10, Provider<CastTile> provider11, Provider<HotspotTile> provider12, Provider<UserTile> provider13, Provider<BatterySaverTile> provider14, Provider<DataSaverTile> provider15, Provider<NightDisplayTile> provider16, Provider<NfcTile> provider17, Provider<MemoryTile> provider18, Provider<UiModeNightTile> provider19, Provider<ReadModeTile> provider20, Provider<GameModeTile> provider21, Provider<OPDndCarModeTile> provider22, Provider<OtgTile> provider23, Provider<DataSwitchTile> provider24, Provider<VPNTile> provider25, Provider<OPDndTile> provider26) {
        this.wifiTileProvider = provider;
        this.bluetoothTileProvider = provider2;
        this.cellularTileProvider = provider3;
        this.dndTileProvider = provider4;
        this.colorInversionTileProvider = provider5;
        this.airplaneModeTileProvider = provider6;
        this.workModeTileProvider = provider7;
        this.rotationLockTileProvider = provider8;
        this.flashlightTileProvider = provider9;
        this.locationTileProvider = provider10;
        this.castTileProvider = provider11;
        this.hotspotTileProvider = provider12;
        this.userTileProvider = provider13;
        this.batterySaverTileProvider = provider14;
        this.dataSaverTileProvider = provider15;
        this.nightDisplayTileProvider = provider16;
        this.nfcTileProvider = provider17;
        this.memoryTileProvider = provider18;
        this.uiModeNightTileProvider = provider19;
        this.readModeTileProvider = provider20;
        this.gameModeTileProvider = provider21;
        this.opDndCarModeTileProvider = provider22;
        this.otgTileProvider = provider23;
        this.dataSwitchTileProvider = provider24;
        this.vpnTileProvider = provider25;
        this.opDndTileProvider = provider26;
    }

    public QSFactoryImpl get() {
        return provideInstance(this.wifiTileProvider, this.bluetoothTileProvider, this.cellularTileProvider, this.dndTileProvider, this.colorInversionTileProvider, this.airplaneModeTileProvider, this.workModeTileProvider, this.rotationLockTileProvider, this.flashlightTileProvider, this.locationTileProvider, this.castTileProvider, this.hotspotTileProvider, this.userTileProvider, this.batterySaverTileProvider, this.dataSaverTileProvider, this.nightDisplayTileProvider, this.nfcTileProvider, this.memoryTileProvider, this.uiModeNightTileProvider, this.readModeTileProvider, this.gameModeTileProvider, this.opDndCarModeTileProvider, this.otgTileProvider, this.dataSwitchTileProvider, this.vpnTileProvider, this.opDndTileProvider);
    }

    public static QSFactoryImpl provideInstance(Provider<WifiTile> provider, Provider<BluetoothTile> provider2, Provider<CellularTile> provider3, Provider<DndTile> provider4, Provider<ColorInversionTile> provider5, Provider<AirplaneModeTile> provider6, Provider<WorkModeTile> provider7, Provider<RotationLockTile> provider8, Provider<FlashlightTile> provider9, Provider<LocationTile> provider10, Provider<CastTile> provider11, Provider<HotspotTile> provider12, Provider<UserTile> provider13, Provider<BatterySaverTile> provider14, Provider<DataSaverTile> provider15, Provider<NightDisplayTile> provider16, Provider<NfcTile> provider17, Provider<MemoryTile> provider18, Provider<UiModeNightTile> provider19, Provider<ReadModeTile> provider20, Provider<GameModeTile> provider21, Provider<OPDndCarModeTile> provider22, Provider<OtgTile> provider23, Provider<DataSwitchTile> provider24, Provider<VPNTile> provider25, Provider<OPDndTile> provider26) {
        QSFactoryImpl qSFactoryImpl = new QSFactoryImpl(provider, provider2, provider3, provider4, provider5, provider6, provider7, provider8, provider9, provider10, provider11, provider12, provider13, provider14, provider15, provider16, provider17, provider18, provider19, provider20, provider21, provider22, provider23, provider24, provider25, provider26);
        return qSFactoryImpl;
    }

    public static QSFactoryImpl_Factory create(Provider<WifiTile> provider, Provider<BluetoothTile> provider2, Provider<CellularTile> provider3, Provider<DndTile> provider4, Provider<ColorInversionTile> provider5, Provider<AirplaneModeTile> provider6, Provider<WorkModeTile> provider7, Provider<RotationLockTile> provider8, Provider<FlashlightTile> provider9, Provider<LocationTile> provider10, Provider<CastTile> provider11, Provider<HotspotTile> provider12, Provider<UserTile> provider13, Provider<BatterySaverTile> provider14, Provider<DataSaverTile> provider15, Provider<NightDisplayTile> provider16, Provider<NfcTile> provider17, Provider<MemoryTile> provider18, Provider<UiModeNightTile> provider19, Provider<ReadModeTile> provider20, Provider<GameModeTile> provider21, Provider<OPDndCarModeTile> provider22, Provider<OtgTile> provider23, Provider<DataSwitchTile> provider24, Provider<VPNTile> provider25, Provider<OPDndTile> provider26) {
        QSFactoryImpl_Factory qSFactoryImpl_Factory = new QSFactoryImpl_Factory(provider, provider2, provider3, provider4, provider5, provider6, provider7, provider8, provider9, provider10, provider11, provider12, provider13, provider14, provider15, provider16, provider17, provider18, provider19, provider20, provider21, provider22, provider23, provider24, provider25, provider26);
        return qSFactoryImpl_Factory;
    }
}
