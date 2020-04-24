package com.oneplus.systemui.util;

import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;

public class OpNotificationChannels extends SystemUI {
    protected static String TAG = "NotificationChannels";

    public void start() {
    }

    protected static void opMayUpdateBatteryNotificationCannel(Context context, NotificationChannel notificationChannel) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        if (notificationManager.getNotificationChannel(NotificationChannels.BATTERY) != null) {
            boolean z = !notificationChannel.getSound().equals(notificationManager.getNotificationChannel(NotificationChannels.BATTERY).getSound());
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("isSoundPathChange:");
            sb.append(z);
            Log.i(str, sb.toString());
            if (z) {
                try {
                    INotificationManager asInterface = Stub.asInterface(ServiceManager.getService("notification"));
                    if (asInterface != null) {
                        asInterface.updateNotificationChannelForPackage(context.getApplicationInfo().packageName, context.getApplicationInfo().uid, notificationChannel);
                        String str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("change soundPath from:");
                        sb2.append(notificationManager.getNotificationChannel(NotificationChannels.BATTERY));
                        sb2.append(" to:");
                        sb2.append(notificationChannel);
                        Log.i(str2, sb2.toString());
                        if (notificationChannel.getSound().equals(notificationManager.getNotificationChannel(NotificationChannels.BATTERY).getSound())) {
                            Log.i(TAG, "change soundPath success:");
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to update BATTERY notification", e);
                }
            }
        } else {
            Log.i(TAG, "batteryChannel isn't exist:");
        }
    }
}
