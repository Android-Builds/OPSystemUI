package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

final class A2dpSinkProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] SRC_UUIDS = {BluetoothUuid.AudioSource, BluetoothUuid.AdvAudioDist};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    private final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothA2dpSink mService;

    private final class A2dpSinkServiceListener implements ServiceListener {
        private A2dpSinkServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            A2dpSinkProfile.this.mService = (BluetoothA2dpSink) bluetoothProfile;
            List connectedDevices = A2dpSinkProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = A2dpSinkProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("A2dpSinkProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("A2dpSinkProfile", sb.toString());
                    findDevice = A2dpSinkProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(A2dpSinkProfile.this, 2);
                findDevice.refresh();
            }
            A2dpSinkProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            A2dpSinkProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 11;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "A2DPSink";
    }

    A2dpSinkProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new A2dpSinkServiceListener(), 11);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothA2dpSink bluetoothA2dpSink = this.mService;
        if (bluetoothA2dpSink == null) {
            return false;
        }
        return bluetoothA2dpSink.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothA2dpSink bluetoothA2dpSink = this.mService;
        if (bluetoothA2dpSink == null) {
            return false;
        }
        if (bluetoothA2dpSink.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothA2dpSink bluetoothA2dpSink = this.mService;
        if (bluetoothA2dpSink == null) {
            return 0;
        }
        return bluetoothA2dpSink.getConnectionState(bluetoothDevice);
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothA2dpSink bluetoothA2dpSink = this.mService;
        boolean z = false;
        if (bluetoothA2dpSink == null) {
            return false;
        }
        if (bluetoothA2dpSink.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothA2dpSink bluetoothA2dpSink = this.mService;
        if (bluetoothA2dpSink != null) {
            if (!z) {
                bluetoothA2dpSink.setPriority(bluetoothDevice, 0);
            } else if (bluetoothA2dpSink.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_headphones_a2dp;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "A2dpSinkProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(11, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up A2DP proxy", th);
            }
        }
    }
}
