package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothSap;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

final class SapProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.SAP};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothSap mService;

    private final class SapServiceListener implements ServiceListener {
        private SapServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            SapProfile.this.mService = (BluetoothSap) bluetoothProfile;
            List connectedDevices = SapProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = SapProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SapProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("SapProfile", sb.toString());
                    findDevice = SapProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(SapProfile.this, 2);
                findDevice.refresh();
            }
            SapProfile.this.mProfileManager.callServiceConnectedListeners();
            SapProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            SapProfile.this.mProfileManager.callServiceDisconnectedListeners();
            SapProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 10;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "SAP";
    }

    SapProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new SapServiceListener(), 10);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothSap bluetoothSap = this.mService;
        if (bluetoothSap == null) {
            return false;
        }
        return bluetoothSap.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothSap bluetoothSap = this.mService;
        if (bluetoothSap == null) {
            return false;
        }
        if (bluetoothSap.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothSap bluetoothSap = this.mService;
        if (bluetoothSap == null) {
            return 0;
        }
        return bluetoothSap.getConnectionState(bluetoothDevice);
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothSap bluetoothSap = this.mService;
        boolean z = false;
        if (bluetoothSap == null) {
            return false;
        }
        if (bluetoothSap.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothSap bluetoothSap = this.mService;
        if (bluetoothSap != null) {
            if (!z) {
                bluetoothSap.setPriority(bluetoothDevice, 0);
            } else if (bluetoothSap.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_cellphone;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "SapProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(10, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up SAP proxy", th);
            }
        }
    }
}
