package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.Context;
import android.util.Log;
import com.android.settingslib.R$drawable;
import java.util.ArrayList;
import java.util.List;

public class HearingAidProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */

    /* renamed from: V */
    public static boolean f55V = true;
    private Context mContext;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHearingAid mService;

    private final class HearingAidServiceListener implements ServiceListener {
        private HearingAidServiceListener() {
        }

        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            HearingAidProfile.this.mService = (BluetoothHearingAid) bluetoothProfile;
            List connectedDevices = HearingAidProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedDevices.remove(0);
                CachedBluetoothDevice findDevice = HearingAidProfile.this.mDeviceManager.findDevice(bluetoothDevice);
                if (findDevice == null) {
                    if (HearingAidProfile.f55V) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("HearingAidProfile found new device: ");
                        sb.append(bluetoothDevice);
                        Log.d("HearingAidProfile", sb.toString());
                    }
                    findDevice = HearingAidProfile.this.mDeviceManager.addDevice(bluetoothDevice);
                }
                findDevice.onProfileStateChanged(HearingAidProfile.this, 2);
                findDevice.refresh();
            }
            HearingAidProfile.this.mDeviceManager.updateHearingAidsDevices();
            HearingAidProfile.this.mIsProfileReady = true;
            HearingAidProfile.this.mProfileManager.callServiceConnectedListeners();
        }

        public void onServiceDisconnected(int i) {
            HearingAidProfile.this.mIsProfileReady = false;
        }
    }

    public boolean accessProfileEnabled() {
        return false;
    }

    public int getProfileId() {
        return 21;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public String toString() {
        return "HearingAid";
    }

    HearingAidProfile(Context context, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mContext = context;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new HearingAidServiceListener(), 21);
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return false;
        }
        return bluetoothHearingAid.connect(bluetoothDevice);
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return false;
        }
        if (bluetoothHearingAid.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return 0;
        }
        return bluetoothHearingAid.getConnectionState(bluetoothDevice);
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return false;
        }
        return bluetoothHearingAid.setActiveDevice(bluetoothDevice);
    }

    public List<BluetoothDevice> getActiveDevices() {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return new ArrayList();
        }
        return bluetoothHearingAid.getActiveDevices();
    }

    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        boolean z = false;
        if (bluetoothHearingAid == null) {
            return false;
        }
        if (bluetoothHearingAid.getPriority(bluetoothDevice) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid != null) {
            if (!z) {
                bluetoothHearingAid.setPriority(bluetoothDevice, 0);
            } else if (bluetoothHearingAid.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
            }
        }
    }

    public long getHiSyncId(BluetoothDevice bluetoothDevice) {
        BluetoothHearingAid bluetoothHearingAid = this.mService;
        if (bluetoothHearingAid == null) {
            return 0;
        }
        return bluetoothHearingAid.getHiSyncId(bluetoothDevice);
    }

    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R$drawable.ic_bt_hearing_aid;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        String str = "HearingAidProfile";
        Log.d(str, "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(21, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w(str, "Error cleaning up Hearing Aid proxy", th);
            }
        }
    }
}
