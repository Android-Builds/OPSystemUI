package com.android.systemui.biometrics;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.os.SomeArgs;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle.Observer;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;

public class BiometricDialogImpl extends SystemUI implements Callbacks {
    private Callback mCallback = new Callback();
    private BiometricDialogView mCurrentDialog;
    private SomeArgs mCurrentDialogArgs;
    /* access modifiers changed from: private */
    public boolean mDialogShowing;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    BiometricDialogImpl.this.handleShowDialog((SomeArgs) message.obj, false, null);
                    return;
                case 2:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    BiometricDialogImpl.this.handleBiometricAuthenticated(((Boolean) someArgs.arg1).booleanValue(), (String) someArgs.arg2);
                    someArgs.recycle();
                    return;
                case 3:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    BiometricDialogImpl.this.handleBiometricHelp((String) someArgs2.arg1);
                    someArgs2.recycle();
                    return;
                case 4:
                    BiometricDialogImpl.this.handleBiometricError((String) message.obj);
                    return;
                case 5:
                    BiometricDialogImpl.this.handleHideDialog(((Boolean) message.obj).booleanValue());
                    return;
                case 6:
                    BiometricDialogImpl.this.handleButtonNegative();
                    return;
                case 7:
                    BiometricDialogImpl.this.handleUserCanceled();
                    return;
                case 8:
                    BiometricDialogImpl.this.handleButtonPositive();
                    return;
                case 9:
                    BiometricDialogImpl.this.handleTryAgainPressed();
                    return;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown message: ");
                    sb.append(message.what);
                    Log.w("BiometricDialogImpl", sb.toString());
                    return;
            }
        }
    };
    private IBiometricServiceReceiverInternal mReceiver;
    private WakefulnessLifecycle mWakefulnessLifecycle;
    final Observer mWakefulnessObserver = new Observer() {
        public void onStartedGoingToSleep() {
            if (BiometricDialogImpl.this.mDialogShowing) {
                Log.d("BiometricDialogImpl", "User canceled due to screen off");
                BiometricDialogImpl.this.mHandler.obtainMessage(7).sendToTarget();
            }
        }
    };
    private WindowManager mWindowManager;

    private class Callback implements DialogViewCallback {
        private Callback() {
        }

        public void onUserCanceled() {
            BiometricDialogImpl.this.mHandler.obtainMessage(7).sendToTarget();
        }

        public void onErrorShown() {
            BiometricDialogImpl.this.mHandler.sendMessageDelayed(BiometricDialogImpl.this.mHandler.obtainMessage(5, Boolean.valueOf(false)), 2000);
        }

        public void onNegativePressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(6).sendToTarget();
        }

        public void onPositivePressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(8).sendToTarget();
        }

        public void onTryAgainPressed() {
            BiometricDialogImpl.this.mHandler.obtainMessage(9).sendToTarget();
        }
    }

    public void start() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.fingerprint") || packageManager.hasSystemFeature("android.hardware.biometrics.face") || packageManager.hasSystemFeature("android.hardware.biometrics.iris")) {
            ((CommandQueue) getComponent(CommandQueue.class)).addCallback((Callbacks) this);
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
            this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
            this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        }
    }

    public void showBiometricDialog(Bundle bundle, IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal, int i, boolean z, int i2) {
        StringBuilder sb = new StringBuilder();
        sb.append("showBiometricDialog, type: ");
        sb.append(i);
        sb.append(", requireConfirmation: ");
        sb.append(z);
        Log.d("BiometricDialogImpl", sb.toString());
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(5);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = bundle;
        obtain.arg2 = iBiometricServiceReceiverInternal;
        obtain.argi1 = i;
        obtain.arg3 = Boolean.valueOf(z);
        obtain.argi2 = i2;
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, obtain).sendToTarget();
    }

    public void onBiometricAuthenticated(boolean z, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("onBiometricAuthenticated: ");
        sb.append(z);
        sb.append(" reason: ");
        sb.append(str);
        Log.d("BiometricDialogImpl", sb.toString());
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = Boolean.valueOf(z);
        obtain.arg2 = str;
        this.mHandler.obtainMessage(2, obtain).sendToTarget();
    }

    public void onBiometricHelp(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("onBiometricHelp: ");
        sb.append(str);
        Log.d("BiometricDialogImpl", sb.toString());
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = str;
        this.mHandler.obtainMessage(3, obtain).sendToTarget();
    }

    public void onBiometricError(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("onBiometricError: ");
        sb.append(str);
        Log.d("BiometricDialogImpl", sb.toString());
        this.mHandler.obtainMessage(4, str).sendToTarget();
    }

    public void hideBiometricDialog() {
        Log.d("BiometricDialogImpl", "hideBiometricDialog");
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(5, Boolean.valueOf(false)).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleShowDialog(SomeArgs someArgs, boolean z, Bundle bundle) {
        BiometricDialogView biometricDialogView;
        this.mCurrentDialogArgs = someArgs;
        int i = someArgs.argi1;
        boolean z2 = !((Boolean) someArgs.arg3).booleanValue();
        String str = "BiometricDialogImpl";
        if (i == 1) {
            biometricDialogView = new FingerprintDialogView(this.mContext, this.mCallback, z2);
        } else if (i == 4) {
            biometricDialogView = new FaceDialogView(this.mContext, this.mCallback, z2);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsupported type: ");
            sb.append(i);
            Log.e(str, sb.toString());
            return;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("handleShowDialog,  savedState: ");
        sb2.append(bundle);
        sb2.append(" mCurrentDialog: ");
        sb2.append(this.mCurrentDialog);
        sb2.append(" newDialog: ");
        sb2.append(biometricDialogView);
        sb2.append(" type: ");
        sb2.append(i);
        Log.d(str, sb2.toString());
        if (bundle != null) {
            biometricDialogView.restoreState(bundle);
        } else {
            BiometricDialogView biometricDialogView2 = this.mCurrentDialog;
            if (biometricDialogView2 != null && this.mDialogShowing) {
                biometricDialogView2.forceRemove();
            }
        }
        this.mReceiver = (IBiometricServiceReceiverInternal) someArgs.arg2;
        biometricDialogView.setBundle((Bundle) someArgs.arg1);
        biometricDialogView.setRequireConfirmation(((Boolean) someArgs.arg3).booleanValue());
        biometricDialogView.setUserId(someArgs.argi2);
        biometricDialogView.setSkipIntro(z);
        this.mCurrentDialog = biometricDialogView;
        WindowManager windowManager = this.mWindowManager;
        BiometricDialogView biometricDialogView3 = this.mCurrentDialog;
        windowManager.addView(biometricDialogView3, biometricDialogView3.getLayoutParams());
        this.mDialogShowing = true;
    }

    /* access modifiers changed from: private */
    public void handleBiometricAuthenticated(boolean z, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleBiometricAuthenticated: ");
        sb.append(z);
        sb.append(", ");
        sb.append(str);
        Log.d("BiometricDialogImpl", sb.toString());
        if (z) {
            this.mCurrentDialog.announceForAccessibility(this.mContext.getResources().getText(this.mCurrentDialog.getAuthenticatedAccessibilityResourceId()));
            if (this.mCurrentDialog.requiresConfirmation()) {
                this.mCurrentDialog.updateState(3);
                return;
            }
            this.mCurrentDialog.updateState(4);
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    BiometricDialogImpl.this.lambda$handleBiometricAuthenticated$0$BiometricDialogImpl();
                }
            }, (long) this.mCurrentDialog.getDelayAfterAuthenticatedDurationMs());
            return;
        }
        this.mCurrentDialog.onAuthenticationFailed(str);
    }

    public /* synthetic */ void lambda$handleBiometricAuthenticated$0$BiometricDialogImpl() {
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleBiometricHelp(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleBiometricHelp: ");
        sb.append(str);
        Log.d("BiometricDialogImpl", sb.toString());
        this.mCurrentDialog.onHelpReceived(str);
    }

    /* access modifiers changed from: private */
    public void handleBiometricError(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleBiometricError: ");
        sb.append(str);
        String str2 = "BiometricDialogImpl";
        Log.d(str2, sb.toString());
        if (!this.mDialogShowing) {
            Log.d(str2, "Dialog already dismissed");
        } else {
            this.mCurrentDialog.onErrorReceived(str);
        }
    }

    /* access modifiers changed from: private */
    public void handleHideDialog(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleHideDialog, userCanceled: ");
        sb.append(z);
        String str = "BiometricDialogImpl";
        Log.d(str, sb.toString());
        if (!this.mDialogShowing) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Dialog already dismissed, userCanceled: ");
            sb2.append(z);
            Log.w(str, sb2.toString());
            return;
        }
        if (z) {
            try {
                this.mReceiver.onDialogDismissed(3);
            } catch (RemoteException e) {
                Log.e(str, "RemoteException when hiding dialog", e);
            }
        }
        this.mReceiver = null;
        this.mDialogShowing = false;
        this.mCurrentDialog.startDismiss();
    }

    /* access modifiers changed from: private */
    public void handleButtonNegative() {
        IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mReceiver;
        String str = "BiometricDialogImpl";
        if (iBiometricServiceReceiverInternal == null) {
            Log.e(str, "Receiver is null");
            return;
        }
        try {
            iBiometricServiceReceiverInternal.onDialogDismissed(2);
        } catch (RemoteException e) {
            Log.e(str, "Remote exception when handling negative button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleButtonPositive() {
        IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mReceiver;
        String str = "BiometricDialogImpl";
        if (iBiometricServiceReceiverInternal == null) {
            Log.e(str, "Receiver is null");
            return;
        }
        try {
            iBiometricServiceReceiverInternal.onDialogDismissed(1);
        } catch (RemoteException e) {
            Log.e(str, "Remote exception when handling positive button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleUserCanceled() {
        handleHideDialog(true);
    }

    /* access modifiers changed from: private */
    public void handleTryAgainPressed() {
        try {
            this.mReceiver.onTryAgainPressed();
        } catch (RemoteException e) {
            Log.e("BiometricDialogImpl", "RemoteException when handling try again", e);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean z = this.mDialogShowing;
        Bundle bundle = new Bundle();
        BiometricDialogView biometricDialogView = this.mCurrentDialog;
        if (biometricDialogView != null) {
            biometricDialogView.onSaveState(bundle);
        }
        if (this.mDialogShowing) {
            this.mCurrentDialog.forceRemove();
            this.mDialogShowing = false;
        }
        if (z) {
            handleShowDialog(this.mCurrentDialogArgs, true, bundle);
        }
    }
}
