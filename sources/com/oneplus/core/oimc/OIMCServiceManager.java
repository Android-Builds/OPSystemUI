package com.oneplus.core.oimc;

import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import com.oneplus.core.oimc.IOIMCService.Stub;

public class OIMCServiceManager implements IOIMCServiceManager {
    private static final Singleton<IOIMCService> CREATOR = new Singleton<IOIMCService>() {
        /* access modifiers changed from: protected */
        public IOIMCService create() {
            IBinder service = ServiceManager.getService("oimc_service");
            String str = "OIMCServiceManager";
            if (service == null) {
                Log.e(str, "can't get service binder: OIMCServiceManager");
                return null;
            }
            IOIMCService asInterface = Stub.asInterface(service);
            if (asInterface == null) {
                Log.e(str, "can't get service interface: OIMCServiceManager");
            }
            return asInterface;
        }
    };
    private static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;

    public static IOIMCService getService() {
        return (IOIMCService) CREATOR.get();
    }

    public void addFuncRuleGlobal(OIMCRule oIMCRule) {
        IOIMCService service = getService();
        if (service != null) {
            try {
                service.addFuncRuleGlobal(oIMCRule);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("OIMCServiceManager", "OIMCService disconnected");
        }
    }

    public void removeFuncRuleGlobal(OIMCRule oIMCRule) {
        IOIMCService service = getService();
        if (service != null) {
            try {
                service.removeFuncRuleGlobal(oIMCRule);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("OIMCServiceManager", "OIMCService disconnected");
        }
    }

    public void notifyModeChange(String str, int i, int i2) {
        IOIMCService service = getService();
        if (service != null) {
            try {
                service.notifyModeChange(str, i, i2);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("OIMCServiceManager", "OIMCService disconnected");
        }
    }

    public int getRemoteFuncStatus(String str) {
        IOIMCService service = getService();
        if (service != null) {
            try {
                return service.getRemoteFuncStatus(str);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("OIMCServiceManager", "OIMCService disconnected");
            return 0;
        }
    }
}
