package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.airbnb.lottie.C0526R;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.biometrics.OpFodHelper.OnFingerprintStateChangeListener;
import com.oneplus.systemui.biometrics.OpFrameAnimationHelper.Callbacks;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;

public class OpFingerprintDialogView extends LinearLayout implements OnFingerprintStateChangeListener {
    private static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public boolean mAnimatingAway;
    Callbacks mCallbacks = new Callbacks() {
        public void animationFinished() {
            String str = "OpFingerprintDialogView";
            Log.d(str, "animationFinished");
            OpFingerprintDialogView.this.stopAnimation();
            OpFingerprintDialogView.this.doFingerPressUpInternal();
            OpFingerprintDialogView.this.mDialogImpl.collapseTransparentLayout();
            if (OpFingerprintDialogView.this.mPendingHideDialog) {
                Log.d(str, "Pending hide fingerprint dialog until animation end");
                OpFingerprintDialogView opFingerprintDialogView = OpFingerprintDialogView.this;
                opFingerprintDialogView.mPendingHideDialog = false;
                opFingerprintDialogView.mAnimatingAway = false;
                OpFingerprintDialogView.this.mDialogImpl.hideFodDialogInner("animation finished");
                OpFingerprintDialogView.this.updateIconVisibility(false);
            }
        }
    };
    private Context mContext;
    private final LinearLayout mDialog;
    /* access modifiers changed from: private */
    public OpBiometricDialogImpl mDialogImpl;
    protected Display mDisplay;
    private float mDisplayWidth;
    private OpFodWindowManager mFodWindowManager;
    /* access modifiers changed from: private */
    public OpFingerprintAnimationCtrl mFpAnimationCtrl;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private ViewGroup mHighlightView;
    private boolean mHighlightViewAttached = false;
    /* access modifiers changed from: private */
    public ViewGroup mLayout;
    private Handler mMainThreadhandler = new Handler(Looper.getMainLooper());
    KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onScreenTurnedOff() {
            super.onScreenTurnedOff();
            StringBuilder sb = new StringBuilder();
            sb.append("onScreenTurnedOff : fp client = ");
            sb.append(OpFingerprintDialogView.this.getOwnerString());
            sb.append(", showOnWindow = ");
            sb.append(OpFingerprintDialogView.this.mShowOnWindow);
            Log.d("OpFingerprintDialogView", sb.toString());
            OpFingerprintDialogView.this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_tooltipForegroundColor);
            OpFingerprintDialogView.this.mHandler.sendEmptyMessage(C0526R.styleable.AppCompatTheme_tooltipForegroundColor);
        }

        public void onStartedWakingUp() {
            super.onStartedWakingUp();
            boolean isKeyguardClient = OpFodHelper.getInstance().isKeyguardClient();
            boolean isShowing = OpFingerprintDialogView.this.mStatusBarKeyguardViewManager.isShowing();
            StringBuilder sb = new StringBuilder();
            sb.append("onStartedWakingUp: owner= ");
            sb.append(OpFingerprintDialogView.this.getOwnerString());
            sb.append(", isKeyguardShowing= ");
            sb.append(isShowing);
            sb.append(", mPendingHideDialog= ");
            sb.append(OpFingerprintDialogView.this.mPendingHideDialog);
            String str = "OpFingerprintDialogView";
            Log.d(str, sb.toString());
            OpFingerprintDialogView.this.mFpAnimationCtrl.updateAnimationDelayTime(OpFingerprintDialogView.this.mPm.isInteractive());
            if (!isKeyguardClient || !isShowing || !OpFingerprintDialogView.this.mPendingHideDialog) {
                OpFingerprintDialogView.this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_tooltipFrameBackground);
                OpFingerprintDialogView.this.mHandler.sendEmptyMessage(C0526R.styleable.AppCompatTheme_tooltipFrameBackground);
                return;
            }
            Log.d(str, "unlock success skip onStartedWakingUp");
        }

        public void onFinishedGoingToSleep(int i) {
            super.onFinishedGoingToSleep(i);
            StringBuilder sb = new StringBuilder();
            sb.append("onFinishedGoingToSleep: why= ");
            sb.append(i);
            Log.d("OpFingerprintDialogView", sb.toString());
            OpFingerprintDialogView.this.mHandler.removeMessages(117);
            Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(117);
            obtainMessage.arg1 = i;
            OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
        }

        public void onScreenTurnedOn() {
            super.onScreenTurnedOn();
            StringBuilder sb = new StringBuilder();
            sb.append("onScreenTurnedOn: ");
            sb.append(OpFingerprintDialogView.this.mLayout.getAlpha());
            Log.d("OpFingerprintDialogView", sb.toString());
            OpFingerprintDialogView.this.mHandler.removeMessages(116);
            OpFingerprintDialogView.this.mHandler.sendEmptyMessage(116);
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            super.onKeyguardVisibilityChanged(z);
            boolean isKeyguardClient = OpFodHelper.getInstance().isKeyguardClient();
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardVisibilityChanged: showing= ");
            sb.append(z);
            sb.append(", isKeyguardClient= ");
            sb.append(isKeyguardClient);
            sb.append(", mPendingHideDialog= ");
            sb.append(OpFingerprintDialogView.this.mPendingHideDialog);
            String str = "OpFingerprintDialogView";
            Log.d(str, sb.toString());
            if (!isKeyguardClient || !OpFingerprintDialogView.this.mPendingHideDialog || z) {
                OpFingerprintDialogView.this.mHandler.removeMessages(118);
                Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(118);
                obtainMessage.obj = Boolean.valueOf(z);
                OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
                return;
            }
            Log.d(str, "unlock success skip onKeyguardVisibilityChanged");
        }

        public void onKeyguardBouncerChanged(boolean z) {
            OpFingerprintDialogView.this.mHandler.removeMessages(129);
            Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(129);
            obtainMessage.obj = Boolean.valueOf(z);
            OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
        }

        public void onFacelockStateChanged(int i) {
            boolean isScreenOffUnlock = OpLsState.getInstance().getPhoneStatusBar().getFacelockController().isScreenOffUnlock();
            StringBuilder sb = new StringBuilder();
            sb.append("onFacelockStateChanged: type:");
            sb.append(i);
            sb.append(", isOffUnlock:");
            sb.append(isScreenOffUnlock);
            Log.d("OpFingerprintDialogView", sb.toString());
            if (i == 4) {
                OpFingerprintDialogView.this.mHandler.removeMessages(120);
                Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(120);
                obtainMessage.arg1 = i;
                OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
            }
        }

        public void onStrongAuthStateChanged(int i) {
            super.onStrongAuthStateChanged(i);
            OpFingerprintDialogView.this.mHandler.removeMessages(121);
            Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(121);
            obtainMessage.arg1 = i;
            OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
        }

        public void onSimStateChanged(int i, int i2, State state) {
            OpFingerprintDialogView.this.mHandler.removeMessages(122);
            Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(122);
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = Integer.valueOf(i);
            obtain.arg2 = Integer.valueOf(i2);
            obtain.arg3 = state;
            obtainMessage.obj = obtain;
            OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
        }

        public void onUserSwitching(int i) {
            super.onUserSwitching(i);
            OpFingerprintDialogView.this.mHandler.removeMessages(123);
            Message obtainMessage = OpFingerprintDialogView.this.mHandler.obtainMessage(123);
            obtainMessage.arg1 = i;
            OpFingerprintDialogView.this.mHandler.sendMessage(obtainMessage);
        }

        public void onKeyguardDoneChanged(boolean z) {
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardDoneChanged: isKeyguardDone = ");
            sb.append(z);
            sb.append(", mPendingHideDialog= ");
            sb.append(OpFingerprintDialogView.this.mPendingHideDialog);
            String str = "OpFingerprintDialogView";
            Log.d(str, sb.toString());
            if (!z || !OpFingerprintDialogView.this.mPendingHideDialog) {
                OpFingerprintDialogView.this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_textColorAlertDialogListItem);
                OpFingerprintDialogView.this.mHandler.obtainMessage(C0526R.styleable.AppCompatTheme_textColorAlertDialogListItem, Boolean.valueOf(z)).sendToTarget();
                return;
            }
            Log.d(str, "unlock success skip onKeyguardDoneChanged");
        }

        public void onQSExpendChanged(boolean z) {
            if (OpFingerprintDialogView.this.isDialogShowing()) {
                boolean isUnlockingWithBiometricAllowed = OpFingerprintDialogView.this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
                KeyguardUpdateMonitor access$2600 = OpFingerprintDialogView.this.mUpdateMonitor;
                OpFingerprintDialogView.this.mUpdateMonitor;
                boolean isUnlockWithFingerprintPossible = access$2600.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
                StringBuilder sb = new StringBuilder();
                sb.append("onQSExpendChanged: expend= ");
                sb.append(z);
                sb.append(", isUnlockingWithBiometricAllowed= ");
                sb.append(isUnlockingWithBiometricAllowed);
                sb.append(", isUnlockWithFingerprintPossible= ");
                sb.append(isUnlockWithFingerprintPossible);
                Log.d("OpFingerprintDialogView", sb.toString());
                if (!OpFodHelper.getInstance().isEmptyClient() && isUnlockWithFingerprintPossible) {
                    OpFingerprintDialogView.this.updateIconVisibility(false);
                }
            }
            OpFodHelper.getInstance().handleQSExpandChanged(z);
        }

        public void onShuttingDown() {
            if (OpFingerprintDialogView.this.isDialogShowing()) {
                Log.d("OpFingerprintDialogView", "onShuttingDown");
                if (!OpFodHelper.getInstance().isEmptyClient()) {
                    OpFingerprintDialogView.this.updateIconVisibility(true);
                }
            }
        }
    };
    private OpFodDisplayController mOpFodDisplayController;
    /* access modifiers changed from: private */
    public OpFodIconViewController mOpFodIconViewController;
    private boolean mPendingDismissDialog;
    boolean mPendingHideDialog = false;
    /* access modifiers changed from: private */
    public PowerManager mPm;
    private Runnable mPressTimeoutRunnable = new Runnable() {
        public void run() {
            boolean isFingerDown = OpFingerprintDialogView.this.mDialogImpl.isFingerDown();
            StringBuilder sb = new StringBuilder();
            sb.append("Press Timeout: pressed = ");
            sb.append(isFingerDown);
            Log.d("OpFingerprintDialogView", sb.toString());
            if (isFingerDown) {
                OpFingerprintDialogView.this.setDisplayPressModeFingerUp();
                OpFingerprintDialogView.this.playAnimation(2);
                OpLsState.getInstance().getPhoneStatusBar().onFpPressedTimeOut();
            }
        }
    };
    private Point mRealDisplaySize = new Point();
    /* access modifiers changed from: private */
    public boolean mShowOnWindow;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    private final WindowManager mWindowManager;

    public void initHandler(Looper looper) {
        this.mHandler = new Handler(looper) {
            public void handleMessage(Message message) {
                int i = message.what;
                if (i != 129) {
                    switch (i) {
                        case C0526R.styleable.AppCompatTheme_textColorAlertDialogListItem /*110*/:
                            OpFingerprintDialogView.this.handleNotifyKeyguardDone(((Boolean) message.obj).booleanValue());
                            return;
                        case C0526R.styleable.AppCompatTheme_textColorSearchUrl /*111*/:
                            OpFingerprintDialogView.this.handleNotifyBrightnessChange();
                            return;
                        case C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle /*112*/:
                            OpFingerprintDialogView.this.mOpFodIconViewController.handleUpdateIconVisibility(((Boolean) message.obj).booleanValue());
                            return;
                        case C0526R.styleable.AppCompatTheme_toolbarStyle /*113*/:
                            SomeArgs someArgs = (SomeArgs) message.obj;
                            OpFingerprintDialogView.this.handleUpdateLayoutDimension(((Boolean) someArgs.arg1).booleanValue(), ((Float) someArgs.arg2).floatValue());
                            return;
                        case C0526R.styleable.AppCompatTheme_tooltipForegroundColor /*114*/:
                            OpFingerprintDialogView.this.handleOnScreenTurnedOff();
                            return;
                        case C0526R.styleable.AppCompatTheme_tooltipFrameBackground /*115*/:
                            OpFingerprintDialogView.this.handleOnStartedWakingUp();
                            return;
                        case 116:
                            OpFingerprintDialogView.this.handleOnScreenTurnedOn();
                            return;
                        case 117:
                            OpFingerprintDialogView.this.handleOnFinishedGoingToSleep(message.arg1);
                            return;
                        case 118:
                            OpFingerprintDialogView.this.handleOnKeyguardVisibilityChanged(((Boolean) message.obj).booleanValue());
                            return;
                        default:
                            switch (i) {
                                case 120:
                                    OpFingerprintDialogView.this.handleOnFacelockStateChanged(message.arg1);
                                    return;
                                case 121:
                                    OpFingerprintDialogView.this.handleOnStrongAuthStateChanged(message.arg1);
                                    return;
                                case 122:
                                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                                    OpFingerprintDialogView.this.handleOnSimStateChanged(((Integer) someArgs2.arg1).intValue(), ((Integer) someArgs2.arg2).intValue(), (State) someArgs2.arg3);
                                    return;
                                case 123:
                                    OpFingerprintDialogView.this.handleOnUserSwitching(message.arg1);
                                    return;
                                default:
                                    return;
                            }
                    }
                } else {
                    OpFingerprintDialogView.this.handleOnKeyguardBouncerChanged(((Boolean) message.obj).booleanValue());
                }
            }
        };
    }

    public OpFingerprintDialogView(Context context, OpBiometricDialogImpl opBiometricDialogImpl) {
        super(context);
        this.mDialogImpl = opBiometricDialogImpl;
        initHandler(opBiometricDialogImpl.getMainLooper());
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.mDisplayWidth = (float) displayMetrics.widthPixels;
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplay.getRealSize(this.mRealDisplaySize);
        LayoutInflater from = LayoutInflater.from(getContext());
        this.mContext = context;
        this.mPm = (PowerManager) this.mContext.getSystemService("power");
        this.mLayout = (ViewGroup) from.inflate(R$layout.op_fingerprint_view, this, false);
        this.mHighlightView = (ViewGroup) from.inflate(R$layout.op_fingerprint_high_light_view, null, false);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mOpFodDisplayController = new OpFodDisplayController(context);
        this.mFpAnimationCtrl = new OpFingerprintAnimationCtrl(this.mLayout, context);
        this.mOpFodIconViewController = new OpFodIconViewController(context, this.mHighlightView, this.mLayout, opBiometricDialogImpl);
        this.mUpdateMonitor.registerCallback(this.mMonitorCallback);
        OpFodHelper.getInstance().addFingerprintStateChangeListener(this);
        OpLsState.getInstance().setFpAnimationCtrl(this.mFpAnimationCtrl);
        this.mStatusBarKeyguardViewManager = OpLsState.getInstance().getStatusBarKeyguardViewManager();
        addView(this.mLayout);
        this.mDialog = (LinearLayout) this.mLayout.findViewById(R$id.dialog);
        this.mLayout.setFocusableInTouchMode(true);
        this.mLayout.requestFocus();
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.setFodDialogView(this);
        }
    }

    public void addToWindow() {
        String str = "OpFingerprintDialogView";
        if (this.mShowOnWindow) {
            Log.w(str, "addToWindow: already added.");
        } else {
            Log.d(str, "addToWindow: addFpViewToWindow");
            this.mFodWindowManager.addFpViewToWindow();
        }
        addHighlightView();
    }

    private void addHighlightView() {
        boolean isAttachedToWindow = this.mHighlightView.isAttachedToWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("addHighlightView: mHighlightViewAttached= ");
        sb.append(this.mHighlightViewAttached);
        sb.append(", reallyAttached= ");
        sb.append(isAttachedToWindow);
        String str = "OpFingerprintDialogView";
        Log.d(str, sb.toString());
        if (this.mHighlightViewAttached) {
            Log.d(str, "addHighlightView: try to add highlight view, but it is already attached before.");
        } else if (!isAttachedToWindow) {
            this.mFodWindowManager.addHighlightViewToWindow(this.mHighlightView);
            this.mHighlightViewAttached = true;
        } else {
            Log.d(str, "addHighlightView: highlight view is already attached.");
        }
    }

    private void removeHighlightView() {
        boolean isAttachedToWindow = this.mHighlightView.isAttachedToWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("removeHighlightView: mHighlightViewAttached= ");
        sb.append(this.mHighlightViewAttached);
        sb.append(", reallyAttached= ");
        sb.append(isAttachedToWindow);
        String sb2 = sb.toString();
        String str = "OpFingerprintDialogView";
        Log.d(str, sb2);
        if (!this.mHighlightViewAttached) {
            Log.d(str, "removeHighlightView: try to remove highlight view, but is is already removed before.");
        } else if (this.mHighlightView.isAttachedToWindow()) {
            this.mWindowManager.removeViewImmediate(this.mHighlightView);
            this.mHighlightViewAttached = false;
        } else {
            Log.d(str, "removeHighlightView: highlight view isn't attached");
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDialog.getLayoutParams().width = (int) this.mDisplayWidth;
        this.mLayout.setAlpha(1.0f);
        this.mPendingHideDialog = false;
        StringBuilder sb = new StringBuilder();
        sb.append("onAttachedToWindow: isKeyguardDone= ");
        sb.append(this.mUpdateMonitor.isKeyguardDone());
        sb.append(", mPendingDismissDialog= ");
        sb.append(this.mPendingDismissDialog);
        String str = "OpFingerprintDialogView";
        Log.d(str, sb.toString());
        this.mShowOnWindow = true;
        if (this.mPendingDismissDialog) {
            Log.d(str, "onAttachedToWindow: should dismiss now.");
            startDismiss(false);
            this.mPendingDismissDialog = false;
            return;
        }
        this.mOpFodIconViewController.handleUpdateIconVisibility(false);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        Log.d("OpFingerprintDialogView", "onDetachedFromWindow");
        resetState();
        this.mPendingHideDialog = false;
        super.onDetachedFromWindow();
        this.mShowOnWindow = false;
        this.mPendingDismissDialog = false;
    }

    public void startDismiss(boolean z) {
        String str = "OpFingerprintDialogView";
        if (!this.mShowOnWindow) {
            Log.d(str, "startDismiss: window has already been removed or not attached yet.");
            this.mPendingDismissDialog = true;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("startDismiss: starting..., authenticated: ");
        sb.append(z);
        sb.append(", callers: ");
        sb.append(Debug.getCallers(2));
        Log.d(str, sb.toString());
        Trace.traceBegin(8, "OpFingerprintDialogView.hide");
        removeHighlightView();
        this.mOpFodDisplayController.dismiss();
        this.mWindowManager.removeViewImmediate(this);
        this.mDialogImpl.onFingerprintDialogDismissDone();
        Trace.traceEnd(8);
    }

    public void forceRemove() {
        Log.d("OpFingerprintDialogView", "forceRemove");
        this.mLayout.animate().cancel();
        this.mDialog.animate().cancel();
        if (isAttachedToWindow()) {
            this.mWindowManager.removeViewImmediate(this);
        }
        this.mAnimatingAway = false;
        removeHighlightView();
    }

    public boolean isAnimatingAway() {
        return this.mAnimatingAway;
    }

    private void updateAnimationResource() {
        if (!OpFodHelper.getInstance().isEmptyClient()) {
            this.mFpAnimationCtrl.updateAnimationRes(this.mPm.isInteractive());
        }
    }

    public void onFingerprintStateChanged() {
        if (OpFodHelper.getInstance().isFingerprintDetecting()) {
            updateAnimationResource();
        }
    }

    public void postPressTimeOutRunnable() {
        if (DEBUG_ONEPLUS) {
            Log.d("OpFingerprintDialogView", "post press timeout message");
        }
        removePressTimeOutMessage();
        this.mHandler.postDelayed(this.mPressTimeoutRunnable, 1000);
    }

    public void removePressTimeOutMessage() {
        if (this.mHandler.hasCallbacks(this.mPressTimeoutRunnable)) {
            if (DEBUG_ONEPLUS) {
                Log.d("OpFingerprintDialogView", "remove press timeout message");
            }
            this.mHandler.removeCallbacks(this.mPressTimeoutRunnable);
        }
    }

    public void doFingerprintPressDown(int i) {
        if (i == 0) {
            postPressTimeOutRunnable();
        }
        String str = "OpFingerprintDialogView";
        if (!this.mDialogImpl.isFingerDown()) {
            Log.d(str, "doFingerprintPressDown: press state the same");
        } else if (!this.mShowOnWindow) {
            Log.d(str, "doFingerprintPressDown: fp window not exist don't show pressed button");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("doFingerprintPressDown: owner:");
            sb.append(getOwnerString());
            sb.append(", done:");
            sb.append(this.mUpdateMonitor.isKeyguardDone());
            sb.append(", callers: ");
            sb.append(Debug.getCallers(1));
            Log.d(str, sb.toString());
            this.mMainThreadhandler.post(new Runnable() {
                public void run() {
                    OpLsState.getInstance().getPhoneStatusBar().getAodDisplayViewManager().onFingerPressed(true);
                }
            });
            this.mDialogImpl.expandTransparentLayout();
            playAnimation(1);
            setDisplayPressModeFingerDown();
        }
    }

    public void doFingerprintPressUp() {
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("doFingerprintPressUp owner:");
            sb.append(getOwnerString());
            sb.append(", done:");
            sb.append(this.mUpdateMonitor.isKeyguardDone());
            sb.append(", pending hide dialog: ");
            sb.append(this.mPendingHideDialog);
            sb.append(", callers: ");
            sb.append(Debug.getCallers(1));
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        if (!this.mPendingHideDialog) {
            stopAnimation();
        }
        doFingerPressUpInternal();
    }

    /* access modifiers changed from: private */
    public void doFingerPressUpInternal() {
        removePressTimeOutMessage();
        setDisplayPressModeFingerUp();
        this.mMainThreadhandler.post(new Runnable() {
            public void run() {
                OpLsState.getInstance().getPhoneStatusBar().getAodDisplayViewManager().onFingerPressed(false);
            }
        });
    }

    public void onFpEventCancel() {
        boolean isFingerDown = this.mDialogImpl.isFingerDown();
        StringBuilder sb = new StringBuilder();
        sb.append("onFpEventCancel: pressed= ");
        sb.append(isFingerDown);
        sb.append(", keyguardDone = ");
        sb.append(this.mUpdateMonitor.isKeyguardDone());
        sb.append(", callers= ");
        sb.append(Debug.getCallers(1));
        String str = "OpFingerprintDialogView";
        Log.d(str, sb.toString());
        if (!this.mShowOnWindow) {
            Log.d(str, "onFpEventCancel: fp window has been removed.");
            return;
        }
        removePressTimeOutMessage();
        if (isFingerDown) {
            if (this.mFpAnimationCtrl.isPlayingAnimation()) {
                this.mFpAnimationCtrl.waitAnimationFinished(this.mCallbacks);
            } else {
                this.mCallbacks.animationFinished();
            }
        }
    }

    /* access modifiers changed from: private */
    public void playAnimation(int i) {
        String str = "OpFingerprintDialogView";
        if (this.mShowOnWindow) {
            Log.d(str, "playAnimation: dialog showing");
        }
        if (OpFodHelper.isSettings(getOwnerString())) {
            Log.d(str, "playAnimation in settings");
        } else if (this.mUpdateMonitor.isKeyguardDone() && this.mUpdateMonitor.isDeviceInteractive() && OpFodHelper.isSystemUI(getOwnerString())) {
            Log.d(str, "playAnimation: keyguard done");
        } else if (this.mUpdateMonitor.isGoingToSleep() || (!this.mUpdateMonitor.isScreenOn() && !this.mUpdateMonitor.isScreenTurningOn())) {
            Log.d(str, "playAnimation: don't play animation due to going to sleep or screen off");
        } else {
            OpFingerprintAnimationCtrl opFingerprintAnimationCtrl = this.mFpAnimationCtrl;
            if (opFingerprintAnimationCtrl != null) {
                opFingerprintAnimationCtrl.playAnimation(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public void stopAnimation() {
        OpFingerprintAnimationCtrl opFingerprintAnimationCtrl = this.mFpAnimationCtrl;
        if (opFingerprintAnimationCtrl != null) {
            opFingerprintAnimationCtrl.stopAnimation(0);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnScreenTurnedOff() {
        if (DEBUG_ONEPLUS) {
            Log.d("OpFingerprintDialogView", "handleOnScreenTurnedOff");
        }
        resetState();
        shouldForceShowFod("screen turned off");
    }

    private boolean shouldForceShowFod(String str) {
        boolean z = this.mShowOnWindow;
        boolean isUnlockWithFingerprintPossible = this.mUpdateMonitor.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
        boolean z2 = !this.mUpdateMonitor.isUnlockingWithBiometricAllowed() || OpFodHelper.getInstance().isFingerprintLockout();
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean isKeyguardDone = this.mUpdateMonitor.isKeyguardDone();
        boolean isDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        boolean isDreaming = this.mUpdateMonitor.isDreaming();
        StringBuilder sb = new StringBuilder();
        sb.append("shouldForceShowFod: reason= ");
        sb.append(str);
        sb.append(", isWindowAttached= ");
        sb.append(z);
        sb.append(", isFpAvailable= ");
        sb.append(isUnlockWithFingerprintPossible);
        sb.append(", isKeyguardShowing= ");
        sb.append(isShowing);
        sb.append(", isKeyguardDone= ");
        sb.append(isKeyguardDone);
        sb.append(", isDeviceInteractive= ");
        sb.append(isDeviceInteractive);
        sb.append(", isDreaming= ");
        sb.append(isDreaming);
        sb.append(", isLockout= ");
        sb.append(z2);
        String str2 = "OpFingerprintDialogView";
        Log.d(str2, sb.toString());
        if (!OpUtils.isCustomFingerprint() || !isUnlockWithFingerprintPossible || ((!isShowing && (isDeviceInteractive || !isDreaming)) || isKeyguardDone)) {
            return false;
        }
        if (OpFodHelper.getInstance().isEmptyClient()) {
            Log.d(str2, "shouldForceShowFod: add to window.");
            Bundle bundle = new Bundle();
            bundle.putString("key_fingerprint_package_name", "forceShow-keyguard");
            this.mDialogImpl.forceShowFodDialog(bundle);
        } else {
            Log.d(str2, "shouldForceShowFod: already added to window, update ui.");
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void handleOnStartedWakingUp() {
        boolean isKeyguardClient = OpFodHelper.getInstance().isKeyguardClient();
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleOnStartedWakingUp : ownerString = ");
            sb.append(getOwnerString());
            sb.append(", isKeyguardShowing = ");
            sb.append(isShowing);
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        if (!isKeyguardClient || isShowing) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        } else if (this.mDialogImpl != null) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(true);
            this.mDialogImpl.hideFodDialogInner("start waking up");
        }
        updateAnimationResource();
    }

    /* access modifiers changed from: private */
    public void handleOnScreenTurnedOn() {
        String str = "OpFingerprintDialogView";
        if (DEBUG_ONEPLUS) {
            Log.d(str, "handleOnScreenTurnedOn");
        }
        if (!shouldForceShowFod("screen turned on") && !OpFodHelper.getInstance().isEmptyClient() && !OpFodHelper.getInstance().isKeyguardClient()) {
            Log.d(str, "update icon visibility while turned on.");
            updateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnFinishedGoingToSleep(int i) {
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleOnFinishedGoingToSleep: why= ");
            sb.append(i);
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        shouldForceShowFod("finish going to sleep");
    }

    /* access modifiers changed from: private */
    public void handleOnKeyguardVisibilityChanged(boolean z) {
        if (DEBUG_ONEPLUS) {
            Log.d("OpFingerprintDialogView", "handleOnKeyguardVisibilityChanged");
        }
        if (z) {
            shouldForceShowFod("keyguard visibility changed: show");
        } else {
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnKeyguardBouncerChanged(boolean z) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnFacelockStateChanged(int i) {
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleOnFacelockStateChanged : type = ");
            sb.append(i);
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        this.mDialogImpl.onFaceUnlocked();
        this.mOpFodIconViewController.handleUpdateIconVisibility(true);
        onFpEventCancel();
    }

    /* access modifiers changed from: private */
    public void handleOnStrongAuthStateChanged(int i) {
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
        boolean isUnlockingWithBiometricAllowed = keyguardUpdateMonitor != null ? keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() : true;
        StringBuilder sb = new StringBuilder();
        sb.append("onStrongAuthStateChanged, ");
        sb.append(isUnlockingWithBiometricAllowed);
        Log.d("OpFingerprintDialogView", sb.toString());
        if (!isUnlockingWithBiometricAllowed) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnSimStateChanged(int i, int i2, State state) {
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
        if (keyguardUpdateMonitor != null && keyguardUpdateMonitor.isSimPinSecure()) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleOnUserSwitching(int i) {
        OpFingerprintAnimationCtrl opFingerprintAnimationCtrl = this.mFpAnimationCtrl;
        if (opFingerprintAnimationCtrl != null) {
            opFingerprintAnimationCtrl.resetState();
            this.mFpAnimationCtrl.checkAnimationValueValid();
            this.mFpAnimationCtrl.updateAnimationRes(this.mPm.isInteractive());
        }
    }

    private void resetState() {
        this.mOpFodDisplayController.resetState();
        StringBuilder sb = new StringBuilder();
        sb.append("resetState: mPendingHideDialog ");
        sb.append(this.mPendingHideDialog);
        String str = "OpFingerprintDialogView";
        Log.i(str, sb.toString());
        this.mPendingHideDialog = false;
        if (OpFodHelper.getInstance().isEmptyClient()) {
            this.mFpAnimationCtrl.resetState();
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("resetState: fp client to ");
            sb2.append(getOwnerString());
            sb2.append(", reuse animation");
            Log.i(str, sb2.toString());
        }
        this.mOpFodIconViewController.handleUpdateIconVisibility(false);
        stopAnimation();
    }

    public void updateIconVisibility(boolean z) {
        if (z) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateIconVisibility: forceHide trace = ");
            sb.append(Debug.getCallers(5));
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle);
        Message obtainMessage = this.mHandler.obtainMessage(C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle);
        obtainMessage.obj = Boolean.valueOf(z);
        this.mHandler.sendMessage(obtainMessage);
    }

    public void hideFODDim() {
        this.mOpFodDisplayController.hideFODDim();
    }

    private boolean isKeyguard(String str) {
        return OpFodHelper.isKeyguard(str);
    }

    /* access modifiers changed from: private */
    public void handleNotifyKeyguardDone(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleNotifyKeyguardDone, ");
        sb.append(z);
        Log.d("OpFingerprintDialogView", sb.toString());
        if (z) {
            this.mOpFodIconViewController.handleUpdateIconVisibility(true);
            onFpEventCancel();
            this.mDialogImpl.hideFodDialogInner("keyguard done");
        }
    }

    private boolean needsToWaitUntilAnimationDone() {
        boolean z = !this.mUpdateMonitor.isKeyguardDone() && OpUtils.isHomeApp() && OpUtils.isSupportRefreshRateSwitch() && (this.mUpdateMonitor.isKeyguardVisible() || !this.mPm.isInteractive());
        this.mPendingHideDialog = z;
        this.mAnimatingAway = z;
        return z;
    }

    public void notifyFingerprintAuthenticated() {
        boolean isHomeApp = OpUtils.isHomeApp();
        boolean isKeyguardVisible = KeyguardUpdateMonitor.getInstance(this.mContext).isKeyguardVisible();
        boolean needsToWaitUntilAnimationDone = needsToWaitUntilAnimationDone();
        StringBuilder sb = new StringBuilder();
        sb.append("notifyFingerprintAuthenticated isInteractive, ");
        sb.append(this.mPm.isInteractive());
        sb.append(" isKeyguardVisible:");
        sb.append(isKeyguardVisible);
        sb.append(" isFingerDown:");
        sb.append(this.mDialogImpl.isFingerDown());
        sb.append(" isHomeApp:");
        sb.append(isHomeApp);
        sb.append(" needsToWaitUntilAnimationDone: ");
        sb.append(needsToWaitUntilAnimationDone);
        Log.d("OpFingerprintDialogView", sb.toString());
        doFingerprintPressUp();
        this.mOpFodDisplayController.notifyFingerprintAuthenticated();
        this.mOpFodIconViewController.handleUpdateIconVisibility(true);
        if (needsToWaitUntilAnimationDone) {
            OpFingerprintAnimationCtrl opFingerprintAnimationCtrl = this.mFpAnimationCtrl;
            if (opFingerprintAnimationCtrl == null || opFingerprintAnimationCtrl.isPlayingAnimation()) {
                this.mFpAnimationCtrl.waitAnimationFinished(this.mCallbacks);
            } else {
                this.mCallbacks.animationFinished();
            }
        } else {
            this.mDialogImpl.collapseTransparentLayout();
            this.mDialogImpl.hideFodDialogInner("fp authenticated");
        }
    }

    public boolean isPendingHideDialog() {
        return this.mPendingHideDialog;
    }

    private void setDisplayPressModeFingerDown() {
        this.mOpFodDisplayController.onFingerPressDown();
    }

    /* access modifiers changed from: private */
    public void setDisplayPressModeFingerUp() {
        this.mOpFodDisplayController.onFingerPressUp();
    }

    public String getOwnerString() {
        return OpFodHelper.getInstance().getCurrentOwner();
    }

    public boolean shouldHideDismissButton() {
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("shouldHideDismissButton: ");
            sb.append(getOwnerString());
            Log.d("OpFingerprintDialogView", sb.toString());
        }
        if (!isKeyguard(getOwnerString()) && !OpFodHelper.isAppLocker(getOwnerString()) && !OpFodHelper.isSettings(getOwnerString()) && !OpFodHelper.isFileManager(getOwnerString()) && OpFodHelper.getInstance().isEmptyClient()) {
        }
        return false;
    }

    public void notifyBrightnessChange() {
        this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_textColorSearchUrl);
        this.mHandler.sendEmptyMessage(C0526R.styleable.AppCompatTheme_textColorSearchUrl);
    }

    /* access modifiers changed from: private */
    public void handleNotifyBrightnessChange() {
        if (this.mShowOnWindow) {
            this.mOpFodIconViewController.onBrightnessChange();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (DEBUG_ONEPLUS) {
            Log.i("OpFingerprintDialogView", "onConfigurationChanged");
        }
        this.mFodWindowManager.handleConfigurationChange();
    }

    public void updateLayoutDimension(boolean z, float f) {
        this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_toolbarStyle);
        Message obtainMessage = this.mHandler.obtainMessage(C0526R.styleable.AppCompatTheme_toolbarStyle);
        SomeArgs obtain = SomeArgs.obtain();
        obtain.arg1 = Boolean.valueOf(z);
        obtain.arg2 = Float.valueOf(f);
        obtainMessage.obj = obtain;
        this.mHandler.sendMessage(obtainMessage);
    }

    /* access modifiers changed from: private */
    public void handleUpdateLayoutDimension(boolean z, float f) {
        if (OpUtils.isSupportResolutionSwitch(this.mContext) || OpUtils.isSupportCutout()) {
            this.mDisplayWidth = f;
            this.mOpFodIconViewController.handleUpdateLayoutDimension();
            this.mFpAnimationCtrl.updateLayoutDimension(z);
            this.mFodWindowManager.handleConfigurationChange();
        }
    }

    public boolean isDialogShowing() {
        return this.mShowOnWindow;
    }

    public void setFodWindowManager(OpFodWindowManager opFodWindowManager) {
        this.mFodWindowManager = opFodWindowManager;
    }

    public void dump(PrintWriter printWriter) {
        Log.d("OpFingerprintDialogView", "dump FingerprintDialogUI");
        Handler handler = this.mHandler;
        if (handler != null && handler.getLooper() != null) {
            this.mHandler.getLooper().dump(new PrintWriterPrinter(printWriter), "OpFingerprintDialogView  ");
        }
    }
}
