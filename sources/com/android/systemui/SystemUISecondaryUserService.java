package com.android.systemui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemUISecondaryUserService extends Service {
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startSecondaryUserServicesIfNeeded();
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        int i = 0;
        if (strArr == null || strArr.length == 0) {
            int length = services.length;
            while (i < length) {
                SystemUI systemUI = services[i];
                if (systemUI != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("dumping service: ");
                    sb.append(systemUI.getClass().getName());
                    printWriter.println(sb.toString());
                    systemUI.dump(fileDescriptor, printWriter, strArr);
                }
                i++;
            }
            return;
        }
        String str = strArr[0];
        int length2 = services.length;
        while (i < length2) {
            SystemUI systemUI2 = services[i];
            if (systemUI2 != null && systemUI2.getClass().getName().endsWith(str)) {
                systemUI2.dump(fileDescriptor, printWriter, strArr);
            }
            i++;
        }
    }
}
