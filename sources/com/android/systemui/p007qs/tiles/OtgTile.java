package com.android.systemui.p007qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;

/* renamed from: com.android.systemui.qs.tiles.OtgTile */
public class OtgTile extends QSTileImpl<BooleanState> {
    private static final Intent OTG_SETTINGS = new Intent("oneplus.intent.action.OTG_SETTINGS");
    private BroadcastReceiver mOTGBReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("oneplus.intent.action.otg_auto_shutdown".equals(intent.getAction())) {
                OtgTile.this.refreshState();
            }
        }
    };
    private boolean mRegistered = false;
    UsbManager mUsbManager = ((UsbManager) this.mContext.getSystemService("usb"));

    public int getMetricsCategory() {
        return 415;
    }

    public boolean isAvailable() {
        return true;
    }

    public OtgTile(QSHost qSHost) {
        super(qSHost);
    }

    public void handleSetListening(boolean z) {
        if (z) {
            if (!this.mRegistered) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("oneplus.intent.action.otg_auto_shutdown");
                this.mContext.registerReceiver(this.mOTGBReceiver, intentFilter);
                this.mRegistered = true;
            }
        } else if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mOTGBReceiver);
            this.mRegistered = false;
        }
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return OTG_SETTINGS;
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_otg_label);
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        setOtgEnabled(!isOtgEnabled());
        refreshState();
    }

    private void setOtgEnabled(boolean z) {
        Global.putInt(this.mContext.getContentResolver(), "oneplus_otg_auto_disable", z ? 1 : 0);
        try {
            UsbManager.class.getMethod("setOtgEnabled", new Class[]{Boolean.TYPE}).invoke(this.mUsbManager, new Object[]{Boolean.valueOf(z)});
        } catch (Exception e) {
            Log.e(this.TAG, "Cannot setOtgEnabled", e);
        }
    }

    private boolean isOtgEnabled() {
        return SystemProperties.getBoolean("persist.sys.oem.otg_support", false);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        booleanState.value = isOtgEnabled();
        if (booleanState.value) {
            booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_otg_on);
        } else {
            booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_otg_off);
        }
        booleanState.label = this.mContext.getString(R$string.quick_settings_otg_label);
        booleanState.contentDescription = this.mContext.getString(R$string.quick_settings_otg_label);
        booleanState.state = booleanState.value ? 2 : 1;
    }
}
