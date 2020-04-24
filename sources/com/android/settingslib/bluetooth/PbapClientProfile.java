package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.List;

public final class PbapClientProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] SRC_UUIDS = {BluetoothUuid.PBAP_PSE};
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    private final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothPbapClient mService;

    private final class PbapClientServiceListener implements ServiceListener {
        private PbapClientServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            PbapClientProfile.this.mService = (BluetoothPbapClient) bluetoothProfile;
            List connectedDevices = PbapClientProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = PbapClientProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("PbapClientProfile found new device: ");
                    sb.append(bluetoothDevice);
                    Log.w("PbapClientProfile", sb.toString());
                    findDevice = PbapClientProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(PbapClientProfile.this, 2);
                findDevice.refresh();
            }
            PbapClientProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int i) {
            PbapClientProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return true;
    }

    public int getProfileId() {
        return 17;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "PbapClient";
    }

    PbapClientProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new PbapClientServiceListener(), 17);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        String str = "PbapClientProfile";
        Log.d(str, "PBAPClientProfile got connect request");
        if (this.mService == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PBAPClientProfile attempting to connect to ");
        sb.append(bluetoothDevice.getAddress());
        Log.d(str, sb.toString());
        return this.mService.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        Log.d("PbapClientProfile", "PBAPClientProfile got disconnect request");
        BluetoothPbapClient bluetoothPbapClient = this.mService;
        if (bluetoothPbapClient == null) {
            return false;
        }
        return bluetoothPbapClient.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothPbapClient bluetoothPbapClient = this.mService;
        if (bluetoothPbapClient == null) {
            return 0;
        }
        return bluetoothPbapClient.getConnectionState(bluetoothDevice);
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothPbapClient bluetoothPbapClient = this.mService;
        boolean z = false;
        if (bluetoothPbapClient == null) {
            return false;
        }
        if (bluetoothPbapClient.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothPbapClient bluetoothPbapClient = this.mService;
        if (bluetoothPbapClient != null) {
            if (!z) {
                bluetoothPbapClient.setPriority(bluetoothDevice, 0);
            } else if (bluetoothPbapClient.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_cellphone;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "PbapClientProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(17, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up PBAP Client proxy", th);
            }
        }
    }
}
