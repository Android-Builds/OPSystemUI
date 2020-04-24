package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

public class HeadsetProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHeadset mService;

    private final class HeadsetServiceListener implements ServiceListener {
        private HeadsetServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            HeadsetProfile.this.mService = (BluetoothHeadset) bluetoothProfile;
            List connectedDevices = HeadsetProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = HeadsetProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("HeadsetProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("HeadsetProfile", sb.toString());
                    findDevice = HeadsetProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(HeadsetProfile.this, 2);
                findDevice.refresh();
            }
            HeadsetProfile.this.mIsProfileReady = true;
            HeadsetProfile.this.mProfileManager.callServiceConnectedListeners();
        }

        public void onServiceDisconnected(int i) {
            HeadsetProfile.this.mProfileManager.callServiceDisconnectedListeners();
            HeadsetProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 1;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "HEADSET";
    }

    HeadsetProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new HeadsetServiceListener(), 1);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset == null) {
            return false;
        }
        return bluetoothHeadset.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset == null) {
            return false;
        }
        if (bluetoothHeadset.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset == null) {
            return 0;
        }
        return bluetoothHeadset.getConnectionState(bluetoothDevice);
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset == null) {
            return false;
        }
        return bluetoothHeadset.setActiveDevice(bluetoothDevice);
    }

    public BluetoothDevice getActiveDevice() {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset == null) {
            return null;
        }
        return bluetoothHeadset.getActiveDevice();
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        boolean z = false;
        if (bluetoothHeadset == null) {
            return false;
        }
        if (bluetoothHeadset.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothHeadset bluetoothHeadset = this.mService;
        if (bluetoothHeadset != null) {
            if (!z) {
                bluetoothHeadset.setPriority(bluetoothDevice, 0);
            } else if (bluetoothHeadset.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_headset_hfp;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "HeadsetProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(1, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up HID proxy", th);
            }
        }
    }
}
