package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

public class MapProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothMap mService;

    private final class MapServiceListener implements ServiceListener {
        private MapServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            MapProfile.this.mService = (BluetoothMap) bluetoothProfile;
            List connectedDevices = MapProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = MapProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MapProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("MapProfile", sb.toString());
                    findDevice = MapProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(MapProfile.this, 2);
                findDevice.refresh();
            }
            MapProfile.this.mProfileManager.callServiceConnectedListeners();
            MapProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            MapProfile.this.mProfileManager.callServiceDisconnectedListeners();
            MapProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 9;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "MAP";
    }

    MapProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new MapServiceListener(), 9);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        Log.d("MapProfile", "connect() - should not get called");
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothMap bluetoothMap = this.mService;
        if (bluetoothMap == null) {
            return false;
        }
        if (bluetoothMap.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothMap bluetoothMap = this.mService;
        if (bluetoothMap == null) {
            return 0;
        }
        return bluetoothMap.getConnectionState(bluetoothDevice);
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothMap bluetoothMap = this.mService;
        boolean z = false;
        if (bluetoothMap == null) {
            return false;
        }
        if (bluetoothMap.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothMap bluetoothMap = this.mService;
        if (bluetoothMap != null) {
            if (!z) {
                bluetoothMap.setPriority(bluetoothDevice, 0);
            } else if (bluetoothMap.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_cellphone;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "MapProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(9, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up MAP proxy", th);
            }
        }
    }
}
