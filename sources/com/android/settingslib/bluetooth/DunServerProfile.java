package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDun;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.Context;
import android.util.Log;
import com.android.settingslib.R$drawable;

public final class DunServerProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public BluetoothDun mService;

    private final class DunServiceListener implements ServiceListener {
        private DunServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            Log.d("DunServerProfile", "Bluetooth service connected");
            DunServerProfile.this.mService = (BluetoothDun) bluetoothProfile;
            DunServerProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            Log.d("DunServerProfile", "Bluetooth service disconnected");
            DunServerProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    public int getProfileId() {
        return 22;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return true;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    public String toString() {
        return "DUN Server";
    }

    DunServerProfile(Context context) {
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new DunServiceListener(), 22);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothDun bluetoothDun = this.mService;
        if (bluetoothDun == null) {
            return false;
        }
        return bluetoothDun.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothDun bluetoothDun = this.mService;
        if (bluetoothDun == null) {
            return 0;
        }
        return bluetoothDun.getConnectionState(bluetoothDevice);
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_network_pan;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "DunServerProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(22, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up DUN proxy", th);
            }
        }
    }
}
