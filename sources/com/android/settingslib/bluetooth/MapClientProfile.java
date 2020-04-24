package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

public final class MapClientProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothMapClient mService;

    private final class MapClientServiceListener implements ServiceListener {
        private MapClientServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            MapClientProfile.this.mService = (BluetoothMapClient) bluetoothProfile;
            List connectedDevices = MapClientProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = MapClientProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MapProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("MapClientProfile", sb.toString());
                    findDevice = MapClientProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(MapClientProfile.this, 2);
                findDevice.refresh();
            }
            MapClientProfile.this.mProfileManager.callServiceConnectedListeners();
            MapClientProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            MapClientProfile.this.mProfileManager.callServiceDisconnectedListeners();
            MapClientProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 18;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "MAP Client";
    }

    MapClientProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new MapClientServiceListener(), 18);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothMapClient bluetoothMapClient = this.mService;
        if (bluetoothMapClient == null) {
            return false;
        }
        return bluetoothMapClient.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothMapClient bluetoothMapClient = this.mService;
        if (bluetoothMapClient == null) {
            return false;
        }
        if (bluetoothMapClient.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothMapClient bluetoothMapClient = this.mService;
        if (bluetoothMapClient == null) {
            return 0;
        }
        return bluetoothMapClient.getConnectionState(bluetoothDevice);
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothMapClient bluetoothMapClient = this.mService;
        boolean z = false;
        if (bluetoothMapClient == null) {
            return false;
        }
        if (bluetoothMapClient.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothMapClient bluetoothMapClient = this.mService;
        if (bluetoothMapClient != null) {
            if (!z) {
                bluetoothMapClient.setPriority(bluetoothDevice, 0);
            } else if (bluetoothMapClient.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_cellphone;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "MapClientProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(18, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up MAP Client proxy", th);
            }
        }
    }
}
