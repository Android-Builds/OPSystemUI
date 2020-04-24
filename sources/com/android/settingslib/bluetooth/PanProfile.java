package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.Context;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.HashMap;
import java.util.List;

public class PanProfile implements LocalBluetoothProfile {
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap = new HashMap<>();
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public BluetoothPan mService;

    private final class PanServiceListener implements ServiceListener {
        private PanServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            PanProfile.this.mService = (BluetoothPan) bluetoothProfile;
            PanProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            PanProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 5;
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
        return "PAN";
    }

    PanProfile(Context context) {
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new PanServiceListener(), 5);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return false;
        }
        List<BluetoothDevice> connectedDevices = bluetoothPan.getConnectedDevices();
        if (connectedDevices != null) {
            for (BluetoothDevice disconnect : connectedDevices) {
                this.mService.disconnect(disconnect);
            }
        }
        return this.mService.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return false;
        }
        return bluetoothPan.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothPan bluetoothPan = this.mService;
        if (bluetoothPan == null) {
            return 0;
        }
        return bluetoothPan.getConnectionState(bluetoothDevice);
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_network_pan;
    }

    /* access modifiers changed from: 0000 */
    public void setLocalRole(BluetoothDevice bluetoothDevice, int i) {
        this.mDeviceRoleMap.put(bluetoothDevice, Integer.valueOf(i));
    }

    /* access modifiers changed from: 0000 */
    public boolean isLocalRoleNap(BluetoothDevice bluetoothDevice) {
        if (!this.mDeviceRoleMap.containsKey(bluetoothDevice)) {
            return false;
        }
        int intValue = ((Integer) this.mDeviceRoleMap.get(bluetoothDevice)).intValue();
        boolean z = true;
        if (intValue != 1) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "PanProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(5, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up PAN proxy", th);
            }
        }
    }
}
