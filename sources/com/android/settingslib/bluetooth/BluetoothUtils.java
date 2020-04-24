package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import com.android.settingslib.R$drawable;
import com.android.settingslib.R$string;

public class BluetoothUtils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    static void showError(Context context, String str, int i) {
        ErrorListener errorListener = sErrorListener;
        if (errorListener != null) {
            errorListener.onShowError(context, str, i);
        }
    }

    public static void setErrorListener(ErrorListener errorListener) {
        sErrorListener = errorListener;
    }

    public static Pair<Drawable, String> getBtClassDrawableWithDescription(Context context, CachedBluetoothDevice cachedBluetoothDevice) {
        BluetoothClass btClass = cachedBluetoothDevice.getBtClass();
        if (btClass != null) {
            int majorDeviceClass = btClass.getMajorDeviceClass();
            if (majorDeviceClass == 256) {
                return new Pair<>(getBluetoothDrawable(context, R$drawable.ic_bt_laptop), context.getString(R$string.bluetooth_talkback_computer));
            }
            if (majorDeviceClass == 512) {
                return new Pair<>(getBluetoothDrawable(context, R$drawable.ic_bt_cellphone), context.getString(R$string.bluetooth_talkback_phone));
            }
            if (majorDeviceClass == 1280) {
                return new Pair<>(getBluetoothDrawable(context, HidProfile.getHidClassDrawable(btClass)), context.getString(R$string.bluetooth_talkback_input_peripheral));
            }
            if (majorDeviceClass == 1536) {
                return new Pair<>(getBluetoothDrawable(context, 17302799), context.getString(R$string.bluetooth_talkback_imaging));
            }
        }
        for (LocalBluetoothProfile drawableResource : cachedBluetoothDevice.getProfiles()) {
            int drawableResource2 = drawableResource.getDrawableResource(btClass);
            if (drawableResource2 != 0) {
                return new Pair<>(getBluetoothDrawable(context, drawableResource2), null);
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(0)) {
                return new Pair<>(getBluetoothDrawable(context, R$drawable.ic_bt_headset_hfp), context.getString(R$string.bluetooth_talkback_headset));
            }
            if (btClass.doesClassMatch(1)) {
                return new Pair<>(getBluetoothDrawable(context, R$drawable.ic_bt_headphones_a2dp), context.getString(R$string.bluetooth_talkback_headphone));
            }
        }
        return new Pair<>(getBluetoothDrawable(context, R$drawable.ic_settings_bluetooth), context.getString(R$string.bluetooth_talkback_bluetooth));
    }

    public static Drawable getBluetoothDrawable(Context context, int i) {
        return context.getDrawable(i);
    }
}
