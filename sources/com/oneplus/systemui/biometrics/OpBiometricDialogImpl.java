package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.hardware.fingerprint.IFingerprintClientActiveCallback.Stub;
import android.hardware.fingerprint.IFingerprintService;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.internal.os.SomeArgs;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$layout;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle.Observer;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.biometrics.OpFodHelper.OnFingerprintStateChangeListener;
import com.oneplus.systemui.biometrics.OpQLController.QLStateListener;

public class OpBiometricDialogImpl extends SystemUI implements Callbacks, ConfigurationListener, OnFingerprintStateChangeListener {
    protected static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    public static final boolean SHOW_TRANSPARENT_ICON_VIEW = SystemProperties.getBoolean("debug.show_transparent_icon_view", false);
    /* access modifiers changed from: private */
    public boolean mAuthenticatedSuccess;
    private boolean mDialogShowing = false;
    private final IFingerprintClientActiveCallback mFingerprintClientActiveCallback = new Stub() {
        public void onClientActiveChanged(boolean z) {
        }

        public void onClientActiveChangedWithPkg(boolean z, String str) {
            StringBuilder sb = new StringBuilder();
            sb.append("onClientActiveChanged, ");
            sb.append(z);
            sb.append(", pkg = ");
            sb.append(str);
            sb.append(", lastOwnerString = ");
            sb.append(OpFodHelper.getInstance().getLastOwner());
            sb.append(", null:");
            sb.append(OpBiometricDialogImpl.this.mFodDialogView == null);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }

        public void onFingerprintEventCallback(int i, int i2, int i3, int i4) {
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("onFingerprintEventCallback, ");
                sb.append(i);
                sb.append(", ");
                sb.append(i2);
                Log.d("OpBiometricDialogImpl", sb.toString());
            }
            if (i == 6 && i2 == 1) {
                OpBiometricDialogImpl.this.getHandler().obtainMessage(6, i, i2).sendToTarget();
            }
        }
    };
    protected OpFingerprintDialogView mFodDialogView;
    protected OpFodFingerTouchValidator mFodFingerTouchValidator;
    protected OpFodWindowManager mFodWindowManager;
    private FingerprintUIHandler mHandler;
    private boolean mIsFaceUnlocked = false;
    private PowerManager mPowerManager;
    protected OpQLController mQLController;
    private final QLStateListener mQLStateListener = new QLStateListener() {
        public void onQLVisibilityChanged(boolean z) {
            if (!z) {
                OpBiometricDialogImpl.this.mFodFingerTouchValidator.reset();
                OpBiometricDialogImpl.this.collapseTransparentLayout();
                if (!OpBiometricDialogImpl.this.isDialogShowing()) {
                    OpBiometricDialogImpl.this.shouldRemoveTransparentIconView();
                }
            }
        }
    };
    private ScreenLifecycle mScreenLifecycle;
    final Observer mScreenObserver = new Observer() {
        public void onScreenTurnedOff() {
            OpBiometricDialogImpl.this.mFodFingerTouchValidator.reset();
            OpBiometricDialogImpl.this.cancelQLShowing();
        }
    };
    /* access modifiers changed from: private */
    public OpFingerprintBlockTouchView mTransparentIconView;
    private WindowManager mWindowManager;

    private class FingerprintUIHandler extends Handler {
        public FingerprintUIHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    OpBiometricDialogImpl.this.opHandleShowDialog((SomeArgs) message.obj);
                    return;
                case 2:
                    OpBiometricDialogImpl.this.opHandleFingerprintAuthenticatedSuccess();
                    return;
                case 3:
                    OpBiometricDialogImpl.this.opHandleHideFodDialog((SomeArgs) message.obj);
                    return;
                case 5:
                    OpBiometricDialogImpl.this.opHandleFingerprintError(message.arg1);
                    return;
                case 6:
                    OpBiometricDialogImpl.this.handleFingerprintAcquire(message.arg1, message.arg2);
                    return;
                case 7:
                    OpBiometricDialogImpl.this.handleFingerprintEnroll(message.arg1);
                    return;
                case 8:
                    OpBiometricDialogImpl.this.handleFingerprintAuthenticatedFail();
                    return;
                case 9:
                    OpBiometricDialogImpl.this.handleUpdateTransparentIconLayoutParams(((Boolean) message.obj).booleanValue());
                    return;
                case 10:
                    OpBiometricDialogImpl.this.handleUpdateTransparentIconVisibility(message.arg1);
                    return;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown message: ");
                    sb.append(message.what);
                    Log.w("OpBiometricDialogImpl", sb.toString());
                    return;
            }
        }
    }

    public static class OpFingerprintBlockTouchView extends FrameLayout {
        private OpBiometricDialogImpl mDialog;
        private boolean mShowingRequest;

        public OpFingerprintBlockTouchView(Context context) {
            this(context, null);
        }

        public OpFingerprintBlockTouchView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mContext = context;
        }

        /* access modifiers changed from: protected */
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            StringBuilder sb = new StringBuilder();
            sb.append("onAttachedToWindow: mRequestShowing= ");
            sb.append(this.mShowingRequest);
            Log.d("OpFingerprintBlockTouchView", sb.toString());
            if (!this.mShowingRequest) {
                selfRemoveFromWindow();
            }
        }

        /* access modifiers changed from: protected */
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            StringBuilder sb = new StringBuilder();
            sb.append("onDetachedFromWindow: mRequestShowing= ");
            sb.append(this.mShowingRequest);
            Log.d("OpFingerprintBlockTouchView", sb.toString());
            if (this.mShowingRequest) {
                selfAddToWindow();
            }
        }

        public void setDialog(OpBiometricDialogImpl opBiometricDialogImpl) {
            this.mDialog = opBiometricDialogImpl;
        }

        public void addToWindow() {
            String str = "OpFingerprintBlockTouchView";
            if (!this.mShowingRequest) {
                this.mShowingRequest = true;
                if (!isAttachedToWindow()) {
                    this.mDialog.mFodWindowManager.addTransparentIconViewToWindow(this);
                } else {
                    Log.d(str, "addToWindow: maybe during removing progress. wait...");
                }
            } else {
                Log.d(str, "addToWindow: window already request to added.");
                this.mDialog.collapseTransparentLayout();
            }
        }

        public void removeFromWindow() {
            if (this.mShowingRequest) {
                this.mShowingRequest = false;
                selfRemoveFromWindow();
                return;
            }
            Log.d("OpFingerprintBlockTouchView", "removeFromWindow: window already request to removed.");
        }

        private void selfAddToWindow() {
            if (!isAttachedToWindow()) {
                this.mDialog.mFodWindowManager.addTransparentIconViewToWindow(this);
            } else {
                Log.d("OpFingerprintBlockTouchView", "addToWindow: maybe during removing progress. wait...");
            }
        }

        private void selfRemoveFromWindow() {
            if (!isAttachedToWindow()) {
                Log.d("OpFingerprintBlockTouchView", "removeFromWindow: maybe during adding progress. wait...");
            } else {
                this.mDialog.mFodWindowManager.removeTransparentIconView();
            }
        }

        public boolean isRequestShowing() {
            return this.mShowingRequest;
        }

        public String toString() {
            return String.format("([%s]: mShowingRequest: %b, isAttachedToWindow: %b)", new Object[]{"OpFingerprintBlockTouchView", Boolean.valueOf(this.mShowingRequest), Boolean.valueOf(isAttachedToWindow())}).toString();
        }
    }

    private class OpFodImeListener extends IPinnedStackListener.Stub {
        public void onActionsChanged(ParceledListSlice parceledListSlice) throws RemoteException {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) throws RemoteException {
        }

        public void onMinimizedStateChanged(boolean z) throws RemoteException {
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) throws RemoteException {
        }

        public void onShelfVisibilityChanged(boolean z, int i) throws RemoteException {
        }

        private OpFodImeListener() {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            if (OpBiometricDialogImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onImeVisibilityChanged: imeVisible= ");
                sb.append(z);
                sb.append(", imeHeight= ");
                sb.append(i);
                Log.d("OpBiometricDialogImpl", sb.toString());
            }
            KeyguardUpdateMonitor.getInstance(OpBiometricDialogImpl.this.mContext).onImeShow(z);
        }
    }

    public void showFodDialog(Bundle bundle, String str) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("showFodDialog: reason= ");
            sb.append(str);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        if (!this.mDialogShowing) {
            this.mHandler.removeMessages(6);
            this.mHandler.removeMessages(7);
        }
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(3);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = bundle;
        obtain.arg2 = str;
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, obtain).sendToTarget();
    }

    public void hideFodDialogInner(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("hideFodDialogInner: reason= ");
        sb.append(str);
        Log.d("OpBiometricDialogImpl", sb.toString());
        hideFodDialog(null, str);
    }

    public void hideFodDialog(Bundle bundle, String str) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("hideFodDialog: reason= ");
            sb.append(str);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = bundle;
        obtain.arg2 = str;
        this.mHandler.obtainMessage(3, obtain).sendToTarget();
    }

    public void onFingerprintAuthenticatedSuccess() {
        if (DEBUG) {
            Log.d("OpBiometricDialogImpl", "onFingerprintAuthenticatedSuccess");
        }
        this.mHandler.sendEmptyMessage(2);
    }

    public void onFingerprintError(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintError: errorCode= ");
            sb.append(i);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        this.mHandler.obtainMessage(5, i, 0).sendToTarget();
    }

    public void start() {
        String str = "OpBiometricDialogImpl";
        Log.d(str, "start");
        putComponent(OpBiometricDialogImpl.class, this);
        PackageManager packageManager = this.mContext.getPackageManager();
        OpFodHelper.init(this.mContext);
        OpFodHelper.getInstance().addFingerprintStateChangeListener(this);
        HandlerThread handlerThread = new HandlerThread("FingerprintDialogUI", -8);
        handlerThread.start();
        this.mHandler = new FingerprintUIHandler(handlerThread.getLooper());
        if (packageManager.hasSystemFeature("android.hardware.fingerprint")) {
            ((CommandQueue) getComponent(CommandQueue.class)).addCallback((Callbacks) this);
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
            this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
            this.mScreenLifecycle.addObserver(this.mScreenObserver);
        }
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        IFingerprintService asInterface = IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
        if (asInterface != null) {
            try {
                asInterface.addClientActiveCallback(this.mFingerprintClientActiveCallback);
            } catch (RemoteException e) {
                Log.e(str, "addClientActiveCallback: ", e);
            }
        }
        try {
            WindowManagerWrapper.getInstance().addPinnedStackListener(new OpFodImeListener());
        } catch (RemoteException e2) {
            Log.e(str, "addPinnedStackListener: ", e2);
        }
        this.mFodDialogView = new OpFingerprintDialogView(this.mContext, this);
        this.mFodWindowManager = new OpFodWindowManager(this.mContext, this, this.mFodDialogView);
        this.mFodFingerTouchValidator = new OpFodFingerTouchValidator(this.mContext);
        this.mTransparentIconView = (OpFingerprintBlockTouchView) LayoutInflater.from(this.mContext).inflate(R$layout.op_fingerprint_icon, null);
        this.mTransparentIconView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                String str = "OpBiometricDialogImpl";
                if (OpBiometricDialogImpl.this.mFodDialogView == null) {
                    Log.d(str, "mTransparentIconView onTouch mFodDialogView doesn't init yet");
                    return false;
                }
                int action = motionEvent.getAction();
                if (OpBiometricDialogImpl.DEBUG && (action == 1 || action == 0)) {
                    String str2 = action == 1 ? "finger up" : action == 0 ? "finger down" : Integer.toString(action);
                    StringBuilder sb = new StringBuilder();
                    sb.append("onTouchTransparent: ");
                    sb.append(str2);
                    sb.append(", mDialogShowing = ");
                    sb.append(OpBiometricDialogImpl.this.isDialogShowing());
                    sb.append(", ");
                    sb.append(OpBiometricDialogImpl.this.mTransparentIconView.toString());
                    sb.append(", isPendingHideDialog = ");
                    sb.append(OpBiometricDialogImpl.this.mFodDialogView.isPendingHideDialog());
                    sb.append(", isDreaming = ");
                    sb.append(KeyguardUpdateMonitor.getInstance(OpBiometricDialogImpl.this.mContext).isDreaming());
                    sb.append(", authenticated = ");
                    sb.append(OpBiometricDialogImpl.this.mAuthenticatedSuccess);
                    sb.append(", pressState = ");
                    sb.append(OpBiometricDialogImpl.this.mFodFingerTouchValidator.toString());
                    Log.d(str, sb.toString());
                }
                if (OpBiometricDialogImpl.this.mAuthenticatedSuccess && action == 0 && !KeyguardUpdateMonitor.getInstance(OpBiometricDialogImpl.this.mContext).isDreaming()) {
                    return false;
                }
                if (OpBiometricDialogImpl.this.mQLController.isQLShowing()) {
                    OpBiometricDialogImpl.this.mQLController.handleQLTouchEvent(motionEvent);
                    return true;
                }
                boolean validateFingerAction = OpBiometricDialogImpl.this.mFodFingerTouchValidator.validateFingerAction(action);
                if (OpBiometricDialogImpl.DEBUG && (action == 1 || action == 0)) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("onTouchTransparent: validate= ");
                    sb2.append(validateFingerAction);
                    sb2.append(", pressState= ");
                    sb2.append(OpBiometricDialogImpl.this.mFodFingerTouchValidator.toString());
                    Log.d(str, sb2.toString());
                }
                if (validateFingerAction) {
                    if (!OpBiometricDialogImpl.this.mFodFingerTouchValidator.isFingerDown()) {
                        OpBiometricDialogImpl.this.handleFingerprintPressUp();
                    } else if (OpBiometricDialogImpl.this.isDialogShowing() && !OpBiometricDialogImpl.this.mAuthenticatedSuccess) {
                        Log.d(str, "onTouchTransparent: touch on view before authenticated success");
                        OpBiometricDialogImpl.this.mFodDialogView.doFingerprintPressDown(0);
                    } else if (OpBiometricDialogImpl.this.mAuthenticatedSuccess) {
                        Log.d(str, "onTouchTransparent: touch on view after authenticated success");
                        OpBiometricDialogImpl.this.shouldShowQL();
                    }
                }
                return true;
            }
        });
        this.mTransparentIconView.setDialog(this);
        if (SHOW_TRANSPARENT_ICON_VIEW) {
            this.mTransparentIconView.setBackgroundColor(-65536);
            this.mTransparentIconView.setAlpha(0.3f);
        }
        OpQLController opQLController = new OpQLController(this.mContext, this.mHandler, this, this.mFodDialogView, this.mFodFingerTouchValidator, this.mQLStateListener);
        this.mQLController = opQLController;
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void onFingerprintStateChanged() {
        if (OpFodHelper.getInstance().isFingerprintSuspended() && OpFodHelper.getInstance().isDoingEnroll()) {
            this.mFodFingerTouchValidator.reset();
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean inFingerprintDialogUiThread() {
        return "FingerprintDialogUI".equals(Thread.currentThread().getName());
    }

    /* access modifiers changed from: 0000 */
    public Looper getMainLooper() {
        return this.mHandler.getLooper();
    }

    public void forceShowFodDialog(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("forceShowFodDialog callers: ");
        sb.append(Debug.getCallers(3));
        Log.d("OpBiometricDialogImpl", sb.toString());
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(1);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = bundle;
        obtain.arg2 = "force show";
        this.mHandler.obtainMessage(1, obtain).sendToTarget();
    }

    public void onFingerprintAcquired(int i, int i2) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintAcquired: acquireInfo = ");
            sb.append(i);
            sb.append(", vendorCode = ");
            sb.append(i2);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        getHandler().obtainMessage(6, i, i2).sendToTarget();
    }

    public void onFingerprintEnrollResult(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintEnrollResult: remaining= ");
            sb.append(i);
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        getHandler().obtainMessage(7, i, 0).sendToTarget();
    }

    public void onFingerprintAuthenticatedFail() {
        if (DEBUG) {
            Log.d("OpBiometricDialogImpl", "onFingerprintAuthenticatedFail");
        }
        getHandler().obtainMessage(8).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void opHandleShowDialog(SomeArgs someArgs) {
        String str = "OpBiometricDialogImpl";
        if (this.mFodDialogView == null) {
            Log.d(str, "opHandleShowDialog mFodDialogView doesn't init yet");
            return;
        }
        Bundle bundle = (Bundle) someArgs.arg1;
        String string = bundle.getString("key_fingerprint_package_name", "");
        String str2 = (String) someArgs.arg2;
        boolean z = bundle.getBoolean("key_resume");
        StringBuilder sb = new StringBuilder();
        sb.append("opHandleShowDialog authenticatedPkg: ");
        sb.append(string);
        sb.append(", reason: ");
        sb.append(str2);
        sb.append(", resume: ");
        sb.append(z);
        Log.d(str, sb.toString());
        if (TextUtils.isEmpty(string)) {
            Log.d(str, "opHandleShowDialog: ownerString empty return");
            return;
        }
        this.mIsFaceUnlocked = false;
        this.mAuthenticatedSuccess = false;
        OpFodHelper.getInstance().updateOwner(bundle);
        if (this.mFodDialogView.isAnimatingAway()) {
            Log.d(str, "opHandleShowDialog: Dialog is doing animating away, force remove first.");
            this.mFodDialogView.forceRemove();
        } else if (this.mDialogShowing) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("opHandleShowDialog: Dialog already showing. , really added ? ");
            sb2.append(isDialogShowing());
            Log.w(str, sb2.toString());
            return;
        }
        this.mFodDialogView.addToWindow();
        this.mTransparentIconView.addToWindow();
        this.mDialogShowing = true;
    }

    /* access modifiers changed from: protected */
    public void opHandleFingerprintAuthenticatedSuccess() {
        Log.d("OpBiometricDialogImpl", "opHandleFingerprintAuthenticatedSuccess");
        this.mAuthenticatedSuccess = true;
        this.mFodDialogView.notifyFingerprintAuthenticated();
    }

    /* access modifiers changed from: private */
    public void opHandleFingerprintError(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("opHandleFingerprintError: errorCode= ");
        sb.append(i);
        String str = "OpBiometricDialogImpl";
        Log.d(str, sb.toString());
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView != null) {
            opFingerprintDialogView.onFpEventCancel();
        }
        if (i == 9) {
            Log.d(str, "opHandleFingerprintError: in lockout state.");
            OpFodHelper.getInstance().changeState(FingerprintState.LOCKOUT);
        } else if (i == 5) {
            OpFodHelper.getInstance().changeState(FingerprintState.STOP);
        }
        if (i != 5) {
            collapseTransparentLayout();
            shouldRemoveTransparentIconView();
        }
    }

    /* access modifiers changed from: private */
    public void opHandleHideFodDialog(SomeArgs someArgs) {
        Bundle bundle = (Bundle) someArgs.arg1;
        String str = (String) someArgs.arg2;
        boolean z = bundle != null ? bundle.getBoolean("key_suspend", false) : false;
        StringBuilder sb = new StringBuilder();
        sb.append("opHandleHideFodDialog: do hide dialog. reason: ");
        sb.append(str);
        sb.append(", suspend: ");
        sb.append(z);
        String sb2 = sb.toString();
        String str2 = "OpBiometricDialogImpl";
        Log.d(str2, sb2);
        if (!this.mDialogShowing) {
            Log.w(str2, "opHandleHideFodDialog: Dialog already dismissed.");
            return;
        }
        this.mFodDialogView.onFpEventCancel();
        boolean isDozing = OpLsState.getInstance().getPhoneStatusBar().getAodWindowManager().isDozing();
        boolean isInteractive = this.mPowerManager.isInteractive();
        if (DEBUG) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("opHandleHideFodDialog: aodAppear= ");
            sb3.append(isDozing);
            sb3.append(", isInteractive= ");
            sb3.append(isInteractive);
            Log.d(str2, sb3.toString());
        }
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((!instance.isKeyguardDone() || (isDozing && !isInteractive)) && !instance.isFingerprintAlreadyAuthenticated() && instance.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser()) && !instance.isSwitchingUser() && ((!OpLsState.getInstance().getStatusBarKeyguardViewManager().isOccluded() && !OpLsState.getInstance().getPhoneStatusBar().isInLaunchTransition()) || OpLsState.getInstance().getPhoneStatusBar().isBouncerShowing())) {
            Log.d(str2, "opHandleHideFodDialog: don't hide window since keyguard is showing");
        } else if (this.mFodDialogView.isPendingHideDialog()) {
            Log.d(str2, "opHandleHideFodDialog: don't hide window since pending hide dialog until animation end");
        } else if (z) {
            Log.d(str2, "opHandleHideFodDialog: suspend return");
            OpFodHelper.getInstance().updateOwner(bundle);
        } else {
            this.mDialogShowing = false;
            OpFodHelper.getInstance().updateOwner(null);
            this.mFodDialogView.startDismiss(this.mAuthenticatedSuccess);
            StringBuilder sb4 = new StringBuilder();
            sb4.append("opHandleHideFodDialog: removeTransparentIconView , isRequestShowing= ");
            sb4.append(this.mTransparentIconView.isRequestShowing());
            sb4.append(", mAuthenticatedSuccess= ");
            sb4.append(this.mAuthenticatedSuccess);
            sb4.append(", ");
            sb4.append(this.mFodFingerTouchValidator.toString());
            Log.d("OpBiometricDialogImpl:removeTransparentIconView", sb4.toString());
            if (this.mTransparentIconView.isRequestShowing() && !this.mFodFingerTouchValidator.isFingerDown()) {
                collapseTransparentLayout();
                shouldRemoveTransparentIconView();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (configuration.orientation == 2) {
            boolean isQLShowing = this.mQLController.isQLShowing();
            StringBuilder sb = new StringBuilder();
            sb.append("onConfigurationChanged: landscape , ");
            sb.append(this.mTransparentIconView.toString());
            sb.append(", mQLShowing = ");
            sb.append(isQLShowing);
            sb.append(", pressState = ");
            sb.append(this.mFodFingerTouchValidator.toString());
            sb.append(", authenticated= ");
            sb.append(this.mAuthenticatedSuccess);
            sb.append(", faceUnlocked= ");
            sb.append(this.mIsFaceUnlocked);
            Log.d("OpBiometricDialogImpl", sb.toString());
            if (this.mFodFingerTouchValidator.isFingerDown() && this.mTransparentIconView.isRequestShowing() && !isQLShowing) {
                this.mFodFingerTouchValidator.reset();
                this.mIsFaceUnlocked = false;
                collapseTransparentLayout();
                if (!isDialogShowing()) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            OpBiometricDialogImpl.this.shouldRemoveTransparentIconView();
                        }
                    });
                }
            }
        }
    }

    public void expandTransparentLayout() {
        updateTransparentIconLayoutParams(true);
    }

    public void collapseTransparentLayout() {
        updateTransparentIconLayoutParams(false);
    }

    private void updateTransparentIconLayoutParams(boolean z) {
        getHandler().removeMessages(9);
        Message obtainMessage = getHandler().obtainMessage(9);
        obtainMessage.obj = Boolean.valueOf(z);
        getHandler().sendMessage(obtainMessage);
    }

    /* access modifiers changed from: private */
    public void handleUpdateTransparentIconLayoutParams(boolean z) {
        if (this.mQLController.isQLShowing()) {
            z = true;
        }
        this.mFodWindowManager.handleUpdateTransparentIconLayoutParams(z);
    }

    public void updateTransparentIconVisibility(int i) {
        getHandler().removeMessages(10);
        Message obtainMessage = getHandler().obtainMessage(10);
        obtainMessage.arg1 = i;
        getHandler().sendMessage(obtainMessage);
    }

    /* access modifiers changed from: private */
    public void handleUpdateTransparentIconVisibility(int i) {
        boolean isFingerDown = this.mFodFingerTouchValidator.isFingerDown();
        boolean isFingerprintSuspended = OpFodHelper.getInstance().isFingerprintSuspended();
        boolean isDoingEnroll = OpFodHelper.getInstance().isDoingEnroll();
        StringBuilder sb = new StringBuilder();
        sb.append("handleUpdateTransparentIconVisibility: pressState= ");
        sb.append(this.mFodFingerTouchValidator.toString());
        sb.append(", isEnrollClient= ");
        sb.append(isDoingEnroll);
        sb.append(", isFpSuspended= ");
        sb.append(isFingerprintSuspended);
        String str = "OpBiometricDialogImpl";
        Log.d(str, sb.toString());
        if (i != 8 || !isFingerDown || (isDoingEnroll && !isFingerprintSuspended)) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("handleUpdateTransparentIconVisibility: visibility= ");
            sb2.append(i);
            Log.d(str, sb2.toString());
            if (i == 8) {
                Log.d(str, "handleUpdateTransparentIconVisibility: collapse transparent icon layout while visibility set to gone");
                collapseTransparentLayout();
            }
            this.mTransparentIconView.setVisibility(i);
            return;
        }
        Log.d(str, "handleUpdateTransparentIconVisibility: finger down do not hide it");
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAcquire(int i, int i2) {
        boolean isDeviceInteractive = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        boolean isPendingHideDialog = this.mFodDialogView.isPendingHideDialog();
        StringBuilder sb = new StringBuilder();
        sb.append("handleFingerprintAcquire: acquireInfo = ");
        sb.append(i);
        sb.append(", vendorCode = ");
        sb.append(i2);
        sb.append(", interactive = ");
        sb.append(isDeviceInteractive);
        sb.append(", ");
        sb.append(this.mTransparentIconView.toString());
        sb.append(", dialogShowing = ");
        sb.append(isDialogShowing());
        sb.append(", pendingHideDialog = ");
        sb.append(isPendingHideDialog);
        sb.append(", pressState = ");
        sb.append(this.mFodFingerTouchValidator.toString());
        String str = "OpBiometricDialogImpl";
        Log.d(str, sb.toString());
        if (i == 0 || i == 6) {
            if (i == 6) {
                if (i2 == 0) {
                    this.mFodDialogView.removePressTimeOutMessage();
                }
                if (!this.mQLController.isQLShowing()) {
                    boolean validateFingerAction = this.mFodFingerTouchValidator.validateFingerAction(i, i2);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("handleFingerprintAcquire: validate= ");
                    sb2.append(validateFingerAction);
                    sb2.append(", pressState:= ");
                    sb2.append(this.mFodFingerTouchValidator.toString());
                    Log.d(str, sb2.toString());
                    if (validateFingerAction) {
                        if (this.mFodFingerTouchValidator.isFingerDown()) {
                            this.mFodDialogView.doFingerprintPressDown(1);
                        } else {
                            handleFingerprintPressUp();
                        }
                    }
                } else {
                    return;
                }
            }
            return;
        }
        this.mFodDialogView.onFpEventCancel();
    }

    /* access modifiers changed from: private */
    public void handleFingerprintPressUp() {
        boolean isEmptyClient = OpFodHelper.getInstance().isEmptyClient();
        StringBuilder sb = new StringBuilder();
        sb.append("handleFingerprintPressUp: isEmptyClient= ");
        sb.append(isEmptyClient);
        sb.append(", callers= ");
        sb.append(Debug.getCallers(2));
        String str = "OpBiometricDialogImpl";
        Log.d(str, sb.toString());
        collapseTransparentLayout();
        if (isEmptyClient) {
            Log.d(str, "handleFingerprintPressUp: finger press up and client is empty.");
            shouldRemoveTransparentIconView();
        }
        if (!this.mFodDialogView.isPendingHideDialog()) {
            this.mFodDialogView.doFingerprintPressUp();
        }
        cancelQLShowing();
    }

    /* access modifiers changed from: private */
    public void handleFingerprintEnroll(int i) {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        String str = "OpBiometricDialogImpl";
        if (opFingerprintDialogView == null) {
            Log.d(str, "handleFingerprintEnroll mFodDialogView doesn't init yet");
            return;
        }
        opFingerprintDialogView.onFpEventCancel();
        boolean isFingerDownOnSensor = this.mFodFingerTouchValidator.isFingerDownOnSensor();
        StringBuilder sb = new StringBuilder();
        sb.append("handleFingerprintEnroll: remaining= ");
        sb.append(i);
        sb.append(", pressState= ");
        sb.append(this.mFodFingerTouchValidator.toString());
        Log.d(str, sb.toString());
        if (i == 0 && isFingerDownOnSensor) {
            this.mFodFingerTouchValidator.resetTouchFromSensor();
            handleFingerprintPressUp();
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthenticatedFail() {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        if (opFingerprintDialogView == null) {
            Log.d("OpBiometricDialogImpl", "handleFingerprintAuthenticatedFail mFodDialogView doesn't init yet");
            return;
        }
        opFingerprintDialogView.onFpEventCancel();
        collapseTransparentLayout();
    }

    public void onDensityOrFontScaleChanged() {
        if (this.mFodWindowManager != null) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    OpBiometricDialogImpl.this.mFodWindowManager.onDensityOrFontScaleChanged();
                }
            });
        }
    }

    public void onFingerprintDialogDismissDone() {
        StringBuilder sb = new StringBuilder();
        sb.append("onFingerprintDialogDismissDone: ");
        sb.append(this.mTransparentIconView.toString());
        sb.append(", pressState = ");
        sb.append(this.mFodFingerTouchValidator.toString());
        sb.append(", authenticated= ");
        sb.append(this.mAuthenticatedSuccess);
        sb.append(", faceUnlocked= ");
        sb.append(this.mIsFaceUnlocked);
        Log.d("OpBiometricDialogImpl", sb.toString());
        shouldShowQL();
        this.mIsFaceUnlocked = false;
    }

    public boolean isAuthenticateSuccess() {
        return this.mAuthenticatedSuccess;
    }

    private boolean isQLEnabled() {
        return this.mQLController.isQLEnabled();
    }

    public void onFaceUnlocked() {
        this.mIsFaceUnlocked = true;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFaceUnlocked mIsEnableQL ");
            sb.append(isQLEnabled());
            sb.append(this.mTransparentIconView.toString());
            Log.d("OpBiometricDialogImpl", sb.toString());
        }
        cancelQLShowing();
    }

    /* access modifiers changed from: private */
    public void cancelQLShowing() {
        if (isQLEnabled()) {
            this.mQLController.interruptShowingQLView();
        }
    }

    private void removeTransparentIconView() {
        OpFingerprintBlockTouchView opFingerprintBlockTouchView = this.mTransparentIconView;
        if (opFingerprintBlockTouchView != null) {
            opFingerprintBlockTouchView.removeFromWindow();
        }
    }

    /* access modifiers changed from: private */
    public Handler getHandler() {
        return this.mHandler;
    }

    /* access modifiers changed from: 0000 */
    public boolean isDialogShowing() {
        OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
        return opFingerprintDialogView != null && opFingerprintDialogView.isDialogShowing();
    }

    public void onOverlayChanged() {
        OpFodWindowManager opFodWindowManager = this.mFodWindowManager;
        if (opFodWindowManager != null) {
            opFodWindowManager.onOverlayChanged();
        }
        if (this.mTransparentIconView.isRequestShowing()) {
            updateTransparentIconLayoutParams(this.mFodWindowManager.isTransparentViewExpanded());
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isFingerDown() {
        return this.mFodFingerTouchValidator.isFingerDown();
    }

    /* access modifiers changed from: 0000 */
    public boolean isFaceUnlocked() {
        return this.mIsFaceUnlocked;
    }

    public void hideFodImmediately() {
        boolean isBiometricPromptReadyToShow = OpFodHelper.getInstance().isBiometricPromptReadyToShow();
        StringBuilder sb = new StringBuilder();
        sb.append("hideFodImmediately: shouldForceHideByBiometric= ");
        sb.append(isBiometricPromptReadyToShow);
        Log.d("OpBiometricDialogImpl", sb.toString());
        if (isBiometricPromptReadyToShow) {
            this.mFodDialogView.updateIconVisibility(true);
        }
    }

    public void onBiometricPromptReady(int i) {
        if (OpFodHelper.getInstance().updateBiometricPromptReady(i)) {
            this.mFodDialogView.updateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public boolean shouldShowQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("shouldShowQL: mAuthenticatedSuccess= ");
        sb.append(this.mAuthenticatedSuccess);
        sb.append(", isKeyguardUnlocked= ");
        sb.append(OpFodHelper.getInstance().isKeyguardUnlocked());
        sb.append(", isFaceUnlocked= ");
        sb.append(this.mIsFaceUnlocked);
        Log.d("OpBiometricDialogImpl", sb.toString());
        if (!this.mAuthenticatedSuccess || !OpFodHelper.getInstance().isKeyguardUnlocked() || this.mIsFaceUnlocked) {
            return false;
        }
        this.mQLController.shouldShowQL();
        return true;
    }

    /* access modifiers changed from: private */
    public boolean shouldRemoveTransparentIconView() {
        boolean isFingerDown = this.mFodFingerTouchValidator.isFingerDown();
        StringBuilder sb = new StringBuilder();
        sb.append("shouldRemoveTransparentIconView: isFingerDownOnView= ");
        sb.append(isFingerDown);
        sb.append(", callers= ");
        sb.append(Debug.getCallers(2));
        Log.d("OpBiometricDialogImpl:removeTransparentIconView", sb.toString());
        if (isFingerDown) {
            return false;
        }
        removeTransparentIconView();
        return true;
    }
}
