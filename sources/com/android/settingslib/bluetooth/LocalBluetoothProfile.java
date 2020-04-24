package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

public interface LocalBluetoothProfile {
    boolean accessProfileEnabled();

    boolean connect(BluetoothDevice bluetoothDevice);

    boolean disconnect(BluetoothDevice bluetoothDevice);

    int getConnectionStatus(BluetoothDevice bluetoothDevice);

    int getDrawableResource(BluetoothClass bluetoothClass);

    int getProfileId();

    boolean isAutoConnectable();

    boolean isPreferred(BluetoothDevice bluetoothDevice);

    void setPreferred(BluetoothDevice bluetoothDevice, boolean z);
}
