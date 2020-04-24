package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager.ServiceListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

public class BluetoothControllerImpl implements BluetoothController, BluetoothCallback, Callback, ServiceListener {
    private final Handler mBgHandler;
    private final WeakHashMap<CachedBluetoothDevice, ActuallyCachedState> mCachedState = new WeakHashMap<>();
    private final List<CachedBluetoothDevice> mConnectedDevices = new ArrayList();
    private int mConnectionState = 0;
    private final int mCurrentUser;
    /* access modifiers changed from: private */
    public boolean mEnabled;
    private final C1574H mHandler = new C1574H(Looper.getMainLooper());
    /* access modifiers changed from: private */
    public int mLastChangedActiveBluetoothProfile;
    /* access modifiers changed from: private */
    public CachedBluetoothDevice mLastChangedActiveDevice;
    private final LocalBluetoothManager mLocalBluetoothManager;
    /* access modifiers changed from: private */
    public int mState;
    /* access modifiers changed from: private */
    public boolean mTransientEnabling = false;
    private final UserManager mUserManager;

    private static class ActuallyCachedState implements Runnable {
        /* access modifiers changed from: private */
        public int mBondState;
        private final WeakReference<CachedBluetoothDevice> mDevice;
        private int mMaxConnectionState;
        private final Handler mUiHandler;

        private ActuallyCachedState(CachedBluetoothDevice cachedBluetoothDevice, Handler handler) {
            this.mBondState = 10;
            this.mMaxConnectionState = 0;
            this.mDevice = new WeakReference<>(cachedBluetoothDevice);
            this.mUiHandler = handler;
        }

        public void run() {
            CachedBluetoothDevice cachedBluetoothDevice = (CachedBluetoothDevice) this.mDevice.get();
            if (cachedBluetoothDevice != null) {
                this.mBondState = cachedBluetoothDevice.getBondState();
                this.mMaxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
                this.mUiHandler.removeMessages(1);
                this.mUiHandler.sendEmptyMessage(1);
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.policy.BluetoothControllerImpl$H */
    private final class C1574H extends Handler {
        /* access modifiers changed from: private */
        public final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList<>();

        public C1574H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                firePairedDevicesChanged();
            } else if (i == 2) {
                fireStateChange();
                if (BluetoothControllerImpl.this.mState == 12 || BluetoothControllerImpl.this.mState == 10 || BluetoothControllerImpl.this.mState == 13) {
                    BluetoothControllerImpl.this.mTransientEnabling = false;
                }
            } else if (i == 3) {
                this.mCallbacks.add((BluetoothController.Callback) message.obj);
            } else if (i == 4) {
                this.mCallbacks.remove((BluetoothController.Callback) message.obj);
            } else if (i == 5) {
                fireActvieDeviceChange();
            }
        }

        private void firePairedDevicesChanged() {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((BluetoothController.Callback) it.next()).onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                fireStateChange((BluetoothController.Callback) it.next());
            }
        }

        private void fireActvieDeviceChange() {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((BluetoothController.Callback) it.next()).onBluetoothActiveDeviceChanged(BluetoothControllerImpl.this.mLastChangedActiveDevice, BluetoothControllerImpl.this.mLastChangedActiveBluetoothProfile);
            }
        }

        private void fireStateChange(BluetoothController.Callback callback) {
            callback.onBluetoothStateChange(BluetoothControllerImpl.this.mEnabled);
        }
    }

    public void onServiceDisconnected() {
    }

    public BluetoothControllerImpl(Context context, Looper looper, LocalBluetoothManager localBluetoothManager) {
        this.mLocalBluetoothManager = localBluetoothManager;
        this.mBgHandler = new Handler(looper);
        LocalBluetoothManager localBluetoothManager2 = this.mLocalBluetoothManager;
        if (localBluetoothManager2 != null) {
            localBluetoothManager2.getEventManager().registerCallback(this);
            this.mLocalBluetoothManager.getProfileManager().addServiceListener(this);
            onBluetoothStateChanged(this.mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mCurrentUser = ActivityManager.getCurrentUser();
    }

    public boolean canConfigBluetooth() {
        if (!this.mUserManager.hasUserRestriction("no_config_bluetooth", UserHandle.of(this.mCurrentUser))) {
            if (!this.mUserManager.hasUserRestriction("no_bluetooth", UserHandle.of(this.mCurrentUser))) {
                return true;
            }
        }
        return false;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("BluetoothController state:");
        printWriter.print("  mLocalBluetoothManager=");
        printWriter.println(this.mLocalBluetoothManager);
        if (this.mLocalBluetoothManager != null) {
            printWriter.print("  mEnabled=");
            printWriter.println(this.mEnabled);
            printWriter.print("  mConnectionState=");
            printWriter.println(stateToString(this.mConnectionState));
            printWriter.print("  mConnectedDevices=");
            printWriter.println(this.mConnectedDevices);
            printWriter.print("  mCallbacks.size=");
            printWriter.println(this.mHandler.mCallbacks.size());
            printWriter.println("  Bluetooth Devices:");
            for (CachedBluetoothDevice cachedBluetoothDevice : getDevices()) {
                StringBuilder sb = new StringBuilder();
                sb.append("    ");
                sb.append(getDeviceString(cachedBluetoothDevice));
                printWriter.println(sb.toString());
            }
            if (this.mLocalBluetoothManager.getBluetoothAdapter() != null) {
                printWriter.println("  getConnectionState=");
                printWriter.println(this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState());
            }
        }
    }

    private static String stateToString(int i) {
        if (i == 0) {
            return "DISCONNECTED";
        }
        if (i == 1) {
            return "CONNECTING";
        }
        if (i == 2) {
            return "CONNECTED";
        }
        if (i == 3) {
            return "DISCONNECTING";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("UNKNOWN(");
        sb.append(i);
        sb.append(")");
        return sb.toString();
    }

    private String getDeviceString(CachedBluetoothDevice cachedBluetoothDevice) {
        StringBuilder sb = new StringBuilder();
        sb.append(cachedBluetoothDevice.getName());
        String str = " ";
        sb.append(str);
        sb.append(cachedBluetoothDevice.getBondState());
        sb.append(str);
        sb.append(cachedBluetoothDevice.isConnected());
        return sb.toString();
    }

    public int getBondState(CachedBluetoothDevice cachedBluetoothDevice) {
        return getCachedState(cachedBluetoothDevice).mBondState;
    }

    public List<CachedBluetoothDevice> getConnectedDevices() {
        return this.mConnectedDevices;
    }

    public void addCallback(BluetoothController.Callback callback) {
        this.mHandler.obtainMessage(3, callback).sendToTarget();
        this.mHandler.sendEmptyMessage(2);
    }

    public void removeCallback(BluetoothController.Callback callback) {
        this.mHandler.obtainMessage(4, callback).sendToTarget();
    }

    public boolean isBluetoothEnabled() {
        return this.mEnabled;
    }

    public int getBluetoothState() {
        return this.mState;
    }

    public boolean isBluetoothConnected() {
        return this.mConnectionState == 2;
    }

    public boolean isBluetoothConnecting() {
        return this.mConnectionState == 1;
    }

    public void setBluetoothTransientEnabling() {
        this.mTransientEnabling = true;
    }

    public boolean isBluetoothTransientEnabling() {
        return this.mTransientEnabling;
    }

    public void setBluetoothEnabled(boolean z) {
        LocalBluetoothManager localBluetoothManager = this.mLocalBluetoothManager;
        if (localBluetoothManager != null) {
            localBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(z);
        }
    }

    public boolean isBluetoothSupported() {
        return this.mLocalBluetoothManager != null;
    }

    public void connect(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mLocalBluetoothManager != null && cachedBluetoothDevice != null) {
            cachedBluetoothDevice.connect(true);
        }
    }

    public void disconnect(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mLocalBluetoothManager != null && cachedBluetoothDevice != null) {
            cachedBluetoothDevice.disconnect();
        }
    }

    public String getConnectedDeviceName() {
        if (this.mConnectedDevices.size() == 1) {
            return ((CachedBluetoothDevice) this.mConnectedDevices.get(0)).getName();
        }
        return null;
    }

    public Collection<CachedBluetoothDevice> getDevices() {
        LocalBluetoothManager localBluetoothManager = this.mLocalBluetoothManager;
        if (localBluetoothManager != null) {
            return localBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        return null;
    }

    private void updateConnected() {
        int connectionState = this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        this.mConnectedDevices.clear();
        int i = connectionState;
        for (CachedBluetoothDevice cachedBluetoothDevice : getDevices()) {
            int maxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
            if (maxConnectionState > i) {
                i = maxConnectionState;
            }
            if (cachedBluetoothDevice.isConnected()) {
                this.mConnectedDevices.add(cachedBluetoothDevice);
            }
        }
        String str = "BluetoothController";
        if (this.mConnectedDevices.isEmpty() && i == 2) {
            i = 0;
            Log.d(str, "update state to DISCONNECTED");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("updateConnected: ");
        sb.append(connectionState);
        sb.append(" to ");
        sb.append(i);
        sb.append(", connection:");
        sb.append(this.mConnectionState);
        sb.append(", empty:");
        sb.append(this.mConnectedDevices.isEmpty());
        Log.d(str, sb.toString());
        if (i != this.mConnectionState) {
            this.mConnectionState = i;
            this.mHandler.sendEmptyMessage(2);
        }
    }

    public void onBluetoothStateChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("BluetoothStateChanged=");
        sb.append(stateToString(i));
        Log.d("BluetoothController", sb.toString());
        this.mEnabled = i == 12 || i == 11;
        this.mState = i;
        updateConnected();
        this.mHandler.sendEmptyMessage(2);
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceAdded=");
        sb.append(cachedBluetoothDevice != null ? cachedBluetoothDevice.getAddress() : "null");
        Log.d("BluetoothController", sb.toString());
        cachedBluetoothDevice.registerCallback(this);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceDeleted=");
        sb.append(cachedBluetoothDevice != null ? cachedBluetoothDevice.getAddress() : "null");
        Log.d("BluetoothController", sb.toString());
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceBondStateChanged=");
        sb.append(cachedBluetoothDevice != null ? cachedBluetoothDevice.getAddress() : "null");
        Log.d("BluetoothController", sb.toString());
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceAttributesChanged() {
        Log.d("BluetoothController", "DeviceAttributesChanged");
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("ConnectionStateChanged=");
        sb.append(cachedBluetoothDevice != null ? cachedBluetoothDevice.getAddress() : "null");
        sb.append(" ");
        sb.append(stateToString(i));
        Log.d("BluetoothController", sb.toString());
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(2);
    }

    public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onActiveDeviceChanged= ");
        sb.append(cachedBluetoothDevice);
        sb.append(", bluetoothProfile:");
        sb.append(i);
        Log.d("BluetoothController", sb.toString());
        this.mLastChangedActiveDevice = cachedBluetoothDevice;
        this.mLastChangedActiveBluetoothProfile = i;
        this.mHandler.sendEmptyMessage(5);
    }

    public void onAclConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACLConnectionStateChanged=");
        sb.append(cachedBluetoothDevice != null ? cachedBluetoothDevice.getAddress() : "null");
        sb.append(" ");
        sb.append(stateToString(i));
        Log.d("BluetoothController", sb.toString());
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(2);
    }

    private ActuallyCachedState getCachedState(CachedBluetoothDevice cachedBluetoothDevice) {
        ActuallyCachedState actuallyCachedState = (ActuallyCachedState) this.mCachedState.get(cachedBluetoothDevice);
        if (actuallyCachedState != null) {
            return actuallyCachedState;
        }
        ActuallyCachedState actuallyCachedState2 = new ActuallyCachedState(cachedBluetoothDevice, this.mHandler);
        this.mBgHandler.post(actuallyCachedState2);
        this.mCachedState.put(cachedBluetoothDevice, actuallyCachedState2);
        return actuallyCachedState2;
    }

    public void onServiceConnected() {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    public int getBatteryLevel() {
        int i;
        synchronized (this.mConnectedDevices) {
            if (!this.mConnectedDevices.isEmpty()) {
                CachedBluetoothDevice cachedBluetoothDevice = null;
                CachedBluetoothDevice cachedBluetoothDevice2 = null;
                for (CachedBluetoothDevice cachedBluetoothDevice3 : this.mConnectedDevices) {
                    if (cachedBluetoothDevice3.isActiveDevice(1)) {
                        cachedBluetoothDevice = cachedBluetoothDevice3;
                    }
                    if (cachedBluetoothDevice3.isActiveDevice(2)) {
                        cachedBluetoothDevice2 = cachedBluetoothDevice3;
                    }
                }
                if (cachedBluetoothDevice != null) {
                    i = cachedBluetoothDevice.getBatteryLevel();
                } else if (cachedBluetoothDevice2 != null) {
                    i = cachedBluetoothDevice2.getBatteryLevel();
                }
            }
            i = -1;
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("getBatteryLevel batteryLevel ");
            sb.append(i);
            Log.d("BluetoothController", sb.toString());
        }
        return i;
    }
}
