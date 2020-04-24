package com.oneplus.anim;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.WindowManager;
import com.oneplus.android.os.IOpMotorManager;
import com.oneplus.android.os.IOpMotorStateChangedCallback;
import com.oneplus.android.os.IOpMotorStateChangedCallback.Stub;
import com.oneplus.faceunlock.OpFacelockController;
import com.oneplus.plugin.OpLsState;

public class OpCameraAnimateController {
    private IOpMotorStateChangedCallback mAvailabilityCallback;
    private Context mContext;
    /* access modifiers changed from: private */
    public OpGraphLight mGraphLight;
    private IOpMotorManager mManager;

    private class MotorStateChangedCallback extends Stub {
        private MotorStateChangedCallback() {
        }

        public void onMotorStateChanged(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("onMotorStateChanged state: ");
            sb.append(i);
            Log.i("OpCameraAnimateController", sb.toString());
            if (i == 1) {
                OpCameraAnimateController.this.mGraphLight.postShow();
            } else if (i == 5 || i == -10) {
                OpFacelockController opFacelockController = null;
                if (OpLsState.getInstance().getPhoneStatusBar() != null) {
                    opFacelockController = OpLsState.getInstance().getPhoneStatusBar().getFacelockController();
                }
                if (opFacelockController != null) {
                    opFacelockController.onMotorStateChanged(i);
                }
            }
        }
    }

    public OpCameraAnimateController(Context context) {
        this.mContext = context;
    }

    public void init() {
        Log.i("OpCameraAnimateController", "init");
        this.mManager = IOpMotorManager.Stub.asInterface(ServiceManager.getService("motor"));
        this.mGraphLight = new OpGraphLight((WindowManager) this.mContext.getSystemService(WindowManager.class), this.mContext, new Handler());
        registerCameraCallback();
    }

    private void registerCameraCallback() {
        StringBuilder sb = new StringBuilder();
        sb.append("registerCameraCallback mManager:");
        sb.append(this.mManager);
        String str = "OpCameraAnimateController";
        Log.i(str, sb.toString());
        if (this.mManager != null) {
            if (this.mAvailabilityCallback == null) {
                this.mAvailabilityCallback = new MotorStateChangedCallback();
            }
            try {
                this.mManager.registerMotorStateChangedCallback(this.mAvailabilityCallback);
            } catch (RemoteException e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("registerCameraCallback: ");
                sb2.append(e.getMessage());
                Log.w(str, sb2.toString());
            }
        }
    }
}
