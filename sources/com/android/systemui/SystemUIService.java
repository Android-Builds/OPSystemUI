package com.android.systemui;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.BinderInternal.BinderProxyLimitListener;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.shared.plugins.PluginManagerImpl;
import com.oneplus.systemui.OpSystemUIService;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemUIService extends OpSystemUIService {
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.crash_sysui", false)) {
            throw new RuntimeException();
        } else if (Build.IS_DEBUGGABLE) {
            BinderInternal.nSetBinderProxyCountEnabled(true);
            BinderInternal.nSetBinderProxyCountWatermarks(1000, 900);
            BinderInternal.setBinderProxyCountCallback(new BinderProxyLimitListener() {
                public void onLimitReached(int i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("uid ");
                    sb.append(i);
                    sb.append(" sent too many Binder proxies to uid ");
                    sb.append(Process.myUid());
                    Slog.w("SystemUIService", sb.toString());
                }
            }, (Handler) Dependency.get(Dependency.MAIN_HANDLER));
        }
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        int i = 0;
        if (strArr == null || strArr.length == 0) {
            int length = services.length;
            while (i < length) {
                SystemUI systemUI = services[i];
                StringBuilder sb = new StringBuilder();
                sb.append("dumping service: ");
                sb.append(systemUI.getClass().getName());
                printWriter.println(sb.toString());
                systemUI.dump(fileDescriptor, printWriter, strArr);
                i++;
            }
            if (Build.IS_DEBUGGABLE) {
                printWriter.println("dumping plugins:");
                ((PluginManagerImpl) Dependency.get(PluginManager.class)).dump(fileDescriptor, printWriter, strArr);
                return;
            }
            return;
        }
        String lowerCase = strArr[0].toLowerCase();
        int length2 = services.length;
        while (i < length2) {
            SystemUI systemUI2 = services[i];
            if (systemUI2.getClass().getName().toLowerCase().endsWith(lowerCase)) {
                systemUI2.dump(fileDescriptor, printWriter, strArr);
            }
            i++;
        }
    }
}
