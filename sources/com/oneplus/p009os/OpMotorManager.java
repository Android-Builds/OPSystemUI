package com.oneplus.p009os;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.oneplus.android.os.IOpMotorManager;
import com.oneplus.android.os.IOpMotorManager.Stub;

/* renamed from: com.oneplus.os.OpMotorManager */
public class OpMotorManager {
    private Context mContext;
    private IOpMotorManager mService;
    private IBinder mToken = new Binder();

    public OpMotorManager(Context context) {
        this.mContext = context;
        this.mService = Stub.asInterface(ServiceManager.getService("motor"));
        if (this.mService == null) {
            Log.v("OpMotorManager", "MotorManagerService was null");
        }
    }

    public int getMotorStateBySystemApp() {
        String str = "OpMotorManager";
        Log.d(str, "getMotorStateBySystemApp");
        IOpMotorManager iOpMotorManager = this.mService;
        if (iOpMotorManager != null) {
            try {
                return iOpMotorManager.getMotorStateBySystemApp();
            } catch (RemoteException e) {
                Log.w(str, "Remote exception in motormanager: ", e);
            }
        } else {
            Log.d(str, "getMotor get motor service again");
            this.mService = Stub.asInterface(ServiceManager.getService("motor"));
            return -10;
        }
    }

    public int downMotorBySystemApp(String str) {
        String str2 = "OpMotorManager";
        Log.d(str2, "downMotorBySystemApp");
        if (this.mService == null || str == null) {
            Log.d(str2, "downMotor get motor service again");
            this.mService = Stub.asInterface(ServiceManager.getService("motor"));
        } else {
            try {
                Log.d(str2, "call service downMotorBySystemApp");
                return this.mService.downMotorBySystemApp(str, this.mToken);
            } catch (RemoteException e) {
                Log.w(str2, "Remote exception in motormanager: ", e);
            }
        }
        return 0;
    }

    public int upMotorBySystemApp(String str) {
        String str2 = "OpMotorManager";
        Log.d(str2, "upMotorBySystemApp");
        if (this.mService == null || str == null) {
            Log.d(str2, "upMotor get motor service again");
            this.mService = Stub.asInterface(ServiceManager.getService("motor"));
        } else {
            try {
                Log.d(str2, "call service upMotorBySystemApp");
                return this.mService.upMotorBySystemApp(str, this.mToken);
            } catch (RemoteException e) {
                Log.w(str2, "Remote exception in motormanager: ", e);
            }
        }
        return 0;
    }
}
