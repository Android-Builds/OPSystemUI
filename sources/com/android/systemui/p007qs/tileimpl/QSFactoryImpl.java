package com.android.systemui.p007qs.tileimpl;

import android.view.ContextThemeWrapper;
import com.android.systemui.R$style;
import com.android.systemui.p007qs.QSTileHost;
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
import com.android.systemui.plugins.p006qs.QSFactory;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTileView;
import com.android.systemui.util.leak.GarbageMonitor.MemoryTile;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tileimpl.QSFactoryImpl */
public class QSFactoryImpl implements QSFactory {
    private final Provider<AirplaneModeTile> mAirplaneModeTileProvider;
    private final Provider<BatterySaverTile> mBatterySaverTileProvider;
    private final Provider<BluetoothTile> mBluetoothTileProvider;
    private final Provider<CastTile> mCastTileProvider;
    private final Provider<CellularTile> mCellularTileProvider;
    private final Provider<ColorInversionTile> mColorInversionTileProvider;
    private final Provider<DataSaverTile> mDataSaverTileProvider;
    private final Provider<DataSwitchTile> mDataSwitchTileProvider;
    private final Provider<DndTile> mDndTileProvider;
    private final Provider<FlashlightTile> mFlashlightTileProvider;
    private final Provider<GameModeTile> mGameModeTileProvider;
    private QSTileHost mHost;
    private final Provider<HotspotTile> mHotspotTileProvider;
    private final Provider<LocationTile> mLocationTileProvider;
    private final Provider<MemoryTile> mMemoryTileProvider;
    private final Provider<NfcTile> mNfcTileProvider;
    private final Provider<NightDisplayTile> mNightDisplayTileProvider;
    private final Provider<OPDndCarModeTile> mOPDndCarModeTileProvider;
    private final Provider<OPDndTile> mOPDndTileProvider;
    private final Provider<OtgTile> mOtgTileProvider;
    private final Provider<ReadModeTile> mReadModeTileProvider;
    private final Provider<RotationLockTile> mRotationLockTileProvider;
    private final Provider<UiModeNightTile> mUiModeNightTileProvider;
    private final Provider<UserTile> mUserTileProvider;
    private final Provider<VPNTile> mVPNTileProvider;
    private final Provider<WifiTile> mWifiTileProvider;
    private final Provider<WorkModeTile> mWorkModeTileProvider;

    public QSFactoryImpl(Provider<WifiTile> provider, Provider<BluetoothTile> provider2, Provider<CellularTile> provider3, Provider<DndTile> provider4, Provider<ColorInversionTile> provider5, Provider<AirplaneModeTile> provider6, Provider<WorkModeTile> provider7, Provider<RotationLockTile> provider8, Provider<FlashlightTile> provider9, Provider<LocationTile> provider10, Provider<CastTile> provider11, Provider<HotspotTile> provider12, Provider<UserTile> provider13, Provider<BatterySaverTile> provider14, Provider<DataSaverTile> provider15, Provider<NightDisplayTile> provider16, Provider<NfcTile> provider17, Provider<MemoryTile> provider18, Provider<UiModeNightTile> provider19, Provider<ReadModeTile> provider20, Provider<GameModeTile> provider21, Provider<OPDndCarModeTile> provider22, Provider<OtgTile> provider23, Provider<DataSwitchTile> provider24, Provider<VPNTile> provider25, Provider<OPDndTile> provider26) {
        this.mWifiTileProvider = provider;
        this.mBluetoothTileProvider = provider2;
        this.mCellularTileProvider = provider3;
        this.mDndTileProvider = provider4;
        this.mColorInversionTileProvider = provider5;
        this.mAirplaneModeTileProvider = provider6;
        this.mWorkModeTileProvider = provider7;
        this.mRotationLockTileProvider = provider8;
        this.mFlashlightTileProvider = provider9;
        this.mLocationTileProvider = provider10;
        this.mCastTileProvider = provider11;
        this.mHotspotTileProvider = provider12;
        this.mUserTileProvider = provider13;
        this.mBatterySaverTileProvider = provider14;
        this.mDataSaverTileProvider = provider15;
        this.mNightDisplayTileProvider = provider16;
        this.mNfcTileProvider = provider17;
        this.mMemoryTileProvider = provider18;
        this.mUiModeNightTileProvider = provider19;
        this.mGameModeTileProvider = provider21;
        this.mReadModeTileProvider = provider20;
        this.mOPDndCarModeTileProvider = provider22;
        this.mOtgTileProvider = provider23;
        this.mDataSwitchTileProvider = provider24;
        this.mVPNTileProvider = provider25;
        this.mOPDndTileProvider = provider26;
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public QSTile createTile(String str) {
        QSTileImpl createTileInternal = createTileInternal(str);
        if (createTileInternal != null) {
            createTileInternal.handleStale();
        }
        return createTileInternal;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.systemui.p007qs.tileimpl.QSTileImpl createTileInternal(java.lang.String r2) {
        /*
            r1 = this;
            int r0 = r2.hashCode()
            switch(r0) {
                case -2099331234: goto L_0x011c;
                case -2016941037: goto L_0x0112;
                case -1183073498: goto L_0x0107;
                case -677011630: goto L_0x00fd;
                case -331239923: goto L_0x00f2;
                case -40300674: goto L_0x00e8;
                case 3154: goto L_0x00de;
                case 99610: goto L_0x00d4;
                case 108971: goto L_0x00c9;
                case 110370: goto L_0x00be;
                case 116980: goto L_0x00b1;
                case 3046207: goto L_0x00a5;
                case 3049826: goto L_0x009a;
                case 3075958: goto L_0x008e;
                case 3165170: goto L_0x0082;
                case 3496342: goto L_0x0076;
                case 3599307: goto L_0x0069;
                case 3649301: goto L_0x005d;
                case 3655441: goto L_0x0051;
                case 104817688: goto L_0x0045;
                case 105947033: goto L_0x0039;
                case 109211285: goto L_0x002d;
                case 1099603663: goto L_0x0021;
                case 1361772990: goto L_0x0015;
                case 1901043637: goto L_0x0009;
                default: goto L_0x0007;
            }
        L_0x0007:
            goto L_0x0127
        L_0x0009:
            java.lang.String r0 = "location"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 9
            goto L_0x0128
        L_0x0015:
            java.lang.String r0 = "opdndcarmode"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 20
            goto L_0x0128
        L_0x0021:
            java.lang.String r0 = "hotspot"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 11
            goto L_0x0128
        L_0x002d:
            java.lang.String r0 = "saver"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 14
            goto L_0x0128
        L_0x0039:
            java.lang.String r0 = "opdnd"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 24
            goto L_0x0128
        L_0x0045:
            java.lang.String r0 = "night"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 15
            goto L_0x0128
        L_0x0051:
            java.lang.String r0 = "work"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 6
            goto L_0x0128
        L_0x005d:
            java.lang.String r0 = "wifi"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 0
            goto L_0x0128
        L_0x0069:
            java.lang.String r0 = "user"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 12
            goto L_0x0128
        L_0x0076:
            java.lang.String r0 = "read"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 19
            goto L_0x0128
        L_0x0082:
            java.lang.String r0 = "game"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 18
            goto L_0x0128
        L_0x008e:
            java.lang.String r0 = "dark"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 17
            goto L_0x0128
        L_0x009a:
            java.lang.String r0 = "cell"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 2
            goto L_0x0128
        L_0x00a5:
            java.lang.String r0 = "cast"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 10
            goto L_0x0128
        L_0x00b1:
            java.lang.String r0 = "vpn"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 23
            goto L_0x0128
        L_0x00be:
            java.lang.String r0 = "otg"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 21
            goto L_0x0128
        L_0x00c9:
            java.lang.String r0 = "nfc"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 16
            goto L_0x0128
        L_0x00d4:
            java.lang.String r0 = "dnd"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 3
            goto L_0x0128
        L_0x00de:
            java.lang.String r0 = "bt"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 1
            goto L_0x0128
        L_0x00e8:
            java.lang.String r0 = "rotation"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 7
            goto L_0x0128
        L_0x00f2:
            java.lang.String r0 = "battery"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 13
            goto L_0x0128
        L_0x00fd:
            java.lang.String r0 = "airplane"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 5
            goto L_0x0128
        L_0x0107:
            java.lang.String r0 = "flashlight"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 8
            goto L_0x0128
        L_0x0112:
            java.lang.String r0 = "inversion"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 4
            goto L_0x0128
        L_0x011c:
            java.lang.String r0 = "dataswitch"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x0127
            r0 = 22
            goto L_0x0128
        L_0x0127:
            r0 = -1
        L_0x0128:
            switch(r0) {
                case 0: goto L_0x0212;
                case 1: goto L_0x0209;
                case 2: goto L_0x0200;
                case 3: goto L_0x01f7;
                case 4: goto L_0x01ee;
                case 5: goto L_0x01e5;
                case 6: goto L_0x01dc;
                case 7: goto L_0x01d3;
                case 8: goto L_0x01ca;
                case 9: goto L_0x01c1;
                case 10: goto L_0x01b8;
                case 11: goto L_0x01af;
                case 12: goto L_0x01a6;
                case 13: goto L_0x019d;
                case 14: goto L_0x0194;
                case 15: goto L_0x018b;
                case 16: goto L_0x0182;
                case 17: goto L_0x0179;
                case 18: goto L_0x0170;
                case 19: goto L_0x0167;
                case 20: goto L_0x015e;
                case 21: goto L_0x0155;
                case 22: goto L_0x014c;
                case 23: goto L_0x0143;
                case 24: goto L_0x013a;
                default: goto L_0x012b;
            }
        L_0x012b:
            java.lang.String r0 = "intent("
            boolean r0 = r2.startsWith(r0)
            if (r0 == 0) goto L_0x021b
            com.android.systemui.qs.QSTileHost r1 = r1.mHost
            com.android.systemui.qs.tiles.IntentTile r1 = com.android.systemui.p007qs.tiles.IntentTile.create(r1, r2)
            return r1
        L_0x013a:
            javax.inject.Provider<com.android.systemui.qs.tiles.OPDndTile> r1 = r1.mOPDndTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0143:
            javax.inject.Provider<com.android.systemui.qs.tiles.VPNTile> r1 = r1.mVPNTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x014c:
            javax.inject.Provider<com.android.systemui.qs.tiles.DataSwitchTile> r1 = r1.mDataSwitchTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0155:
            javax.inject.Provider<com.android.systemui.qs.tiles.OtgTile> r1 = r1.mOtgTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x015e:
            javax.inject.Provider<com.android.systemui.qs.tiles.OPDndCarModeTile> r1 = r1.mOPDndCarModeTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0167:
            javax.inject.Provider<com.android.systemui.qs.tiles.ReadModeTile> r1 = r1.mReadModeTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0170:
            javax.inject.Provider<com.android.systemui.qs.tiles.GameModeTile> r1 = r1.mGameModeTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0179:
            javax.inject.Provider<com.android.systemui.qs.tiles.UiModeNightTile> r1 = r1.mUiModeNightTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0182:
            javax.inject.Provider<com.android.systemui.qs.tiles.NfcTile> r1 = r1.mNfcTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x018b:
            javax.inject.Provider<com.android.systemui.qs.tiles.NightDisplayTile> r1 = r1.mNightDisplayTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0194:
            javax.inject.Provider<com.android.systemui.qs.tiles.DataSaverTile> r1 = r1.mDataSaverTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x019d:
            javax.inject.Provider<com.android.systemui.qs.tiles.BatterySaverTile> r1 = r1.mBatterySaverTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01a6:
            javax.inject.Provider<com.android.systemui.qs.tiles.UserTile> r1 = r1.mUserTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01af:
            javax.inject.Provider<com.android.systemui.qs.tiles.HotspotTile> r1 = r1.mHotspotTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01b8:
            javax.inject.Provider<com.android.systemui.qs.tiles.CastTile> r1 = r1.mCastTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01c1:
            javax.inject.Provider<com.android.systemui.qs.tiles.LocationTile> r1 = r1.mLocationTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01ca:
            javax.inject.Provider<com.android.systemui.qs.tiles.FlashlightTile> r1 = r1.mFlashlightTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01d3:
            javax.inject.Provider<com.android.systemui.qs.tiles.RotationLockTile> r1 = r1.mRotationLockTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01dc:
            javax.inject.Provider<com.android.systemui.qs.tiles.WorkModeTile> r1 = r1.mWorkModeTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01e5:
            javax.inject.Provider<com.android.systemui.qs.tiles.AirplaneModeTile> r1 = r1.mAirplaneModeTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01ee:
            javax.inject.Provider<com.android.systemui.qs.tiles.ColorInversionTile> r1 = r1.mColorInversionTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x01f7:
            javax.inject.Provider<com.android.systemui.qs.tiles.DndTile> r1 = r1.mDndTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0200:
            javax.inject.Provider<com.android.systemui.qs.tiles.CellularTile> r1 = r1.mCellularTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0209:
            javax.inject.Provider<com.android.systemui.qs.tiles.BluetoothTile> r1 = r1.mBluetoothTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x0212:
            javax.inject.Provider<com.android.systemui.qs.tiles.WifiTile> r1 = r1.mWifiTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x021b:
            java.lang.String r0 = "custom("
            boolean r0 = r2.startsWith(r0)
            if (r0 == 0) goto L_0x022a
            com.android.systemui.qs.QSTileHost r1 = r1.mHost
            com.android.systemui.qs.external.CustomTile r1 = com.android.systemui.p007qs.external.CustomTile.create(r1, r2)
            return r1
        L_0x022a:
            boolean r0 = android.os.Build.IS_DEBUGGABLE
            if (r0 == 0) goto L_0x023f
            java.lang.String r0 = "dbg:mem"
            boolean r0 = r2.equals(r0)
            if (r0 == 0) goto L_0x023f
            javax.inject.Provider<com.android.systemui.util.leak.GarbageMonitor$MemoryTile> r1 = r1.mMemoryTileProvider
            java.lang.Object r1 = r1.get()
            com.android.systemui.qs.tileimpl.QSTileImpl r1 = (com.android.systemui.p007qs.tileimpl.QSTileImpl) r1
            return r1
        L_0x023f:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r0 = "No stock tile spec: "
            r1.append(r0)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            java.lang.String r2 = "QSFactory"
            android.util.Log.w(r2, r1)
            r1 = 0
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.tileimpl.QSFactoryImpl.createTileInternal(java.lang.String):com.android.systemui.qs.tileimpl.QSTileImpl");
    }

    public QSTileView createTileView(QSTile qSTile, boolean z) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mHost.getContext(), R$style.qs_theme);
        QSIconView createTileView = qSTile.createTileView(contextThemeWrapper);
        if (z) {
            return new QSTileBaseView(contextThemeWrapper, createTileView, z);
        }
        return new QSTileView(contextThemeWrapper, createTileView);
    }
}
