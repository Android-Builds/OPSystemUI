package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothPbap.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.R$drawable;

public class PbapServerProfile implements LocalBluetoothProfile {
    @VisibleForTesting
    public static final String NAME = "PBAP Server";
    static final ParcelUuid[] PBAB_CLIENT_UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree, BluetoothUuid.PBAP_PCE};
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public BluetoothPbap mService;

    private final class PbapServiceListener implements ServiceListener {
        private PbapServiceListener() {
        }

        public void onServiceConnected(BluetoothPbap bluetoothPbap) {
            PbapServerProfile.this.mService = bluetoothPbap;
            PbapServerProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected() {
            PbapServerProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    public int getProfileId() {
        return 6;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return false;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    public String toString() {
        return NAME;
    }

    PbapServerProfile(Context context) {
        new BluetoothPbap(context, new PbapServiceListener());
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothPbap bluetoothPbap = this.mService;
        if (bluetoothPbap == null) {
            return false;
        }
        return bluetoothPbap.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothPbap bluetoothPbap = this.mService;
        if (bluetoothPbap != null && bluetoothPbap.isConnected(bluetoothDevice)) {
            return 2;
        }
        return 0;
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_cellphone;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "PbapServerProfile";
        Log.d(str, "finalize()");
        BluetoothPbap bluetoothPbap = this.mService;
        if (bluetoothPbap != null) {
            try {
                bluetoothPbap.close();
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up PBAP proxy", th);
            }
        }
    }
}
