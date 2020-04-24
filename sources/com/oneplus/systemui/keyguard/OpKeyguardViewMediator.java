package com.oneplus.systemui.keyguard;

import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.phone.StatusBar;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;

public class OpKeyguardViewMediator extends SystemUI {
    public static int AUTHENTICATE_FACEUNLOCK = 2;
    public static int AUTHENTICATE_FINGERPRINT = 1;
    public static int AUTHENTICATE_IGNORE = 0;
    protected static final boolean IS_CUSTOM_FINGERPRINT = OpUtils.isCustomFingerprint();
    private int mAuthenticatingType = AUTHENTICATE_IGNORE;
    protected boolean mIgnoreHandleShow = false;
    private int mLastAlpha = 1;
    private boolean mPendingHidePanel = false;
    private boolean mSettingShowing = false;
    /* access modifiers changed from: protected */
    public StatusBar mStatusBar;
    private KeyguardUpdateMonitor mUpdateMonitor;

    protected class OpHandler extends Handler {
        public void handleMessage(Message message) {
        }

        public OpHandler(Looper looper, Callback callback, boolean z) {
            super(looper, callback, z);
        }
    }

    public void start() {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    /* access modifiers changed from: protected */
    public void opHandleHide() {
        if (this.mSettingShowing) {
            Log.d("OpKeyguardViewMediator", "delay GoingAway");
            getHandler().postDelayed(getKeyguardGoingAwayRunnableRunnable(), 500);
        } else {
            getKeyguardGoingAwayRunnableRunnable().run();
        }
        if (this.mPendingHidePanel) {
            this.mPendingHidePanel = false;
            changePanelAlpha(0, AUTHENTICATE_FINGERPRINT);
        }
    }

    public void notifyScreenOffAuthenticate(boolean z, int i) {
        notifyScreenOffAuthenticate(z, i, 4);
    }

    public void notifyScreenOffAuthenticate(boolean z, int i, int i2) {
        StringBuilder sb = new StringBuilder();
        sb.append("notifyAuthenticate Change: ");
        sb.append(z);
        sb.append(", type:");
        sb.append(i);
        sb.append(", currentType:");
        sb.append(this.mAuthenticatingType);
        sb.append(", result:");
        sb.append(i2);
        String str = "OpKeyguardViewMediator";
        Log.d(str, sb.toString());
        String str2 = "not handle another Authenticate";
        if (!z) {
            if (IS_CUSTOM_FINGERPRINT) {
                int i3 = this.mAuthenticatingType;
                int i4 = AUTHENTICATE_FACEUNLOCK;
                if (i3 == i4 && i != i4 && i2 == 5) {
                    Log.d(str, str2);
                }
            }
            this.mAuthenticatingType = AUTHENTICATE_IGNORE;
        } else if (this.mAuthenticatingType != AUTHENTICATE_IGNORE) {
            Log.d(str, str2);
            return;
        } else {
            this.mAuthenticatingType = i;
        }
        if (i2 != 7) {
            for (int size = getKeyguardStateCallback().size() - 1; size >= 0; size--) {
                try {
                    ((IKeyguardStateCallback) getKeyguardStateCallback().get(size)).onFingerprintStateChange(z, i, i2, 0);
                } catch (RemoteException e) {
                    Slog.w(str, "Failed to call onFingerprintStateChange", e);
                    if (e instanceof DeadObjectException) {
                        getKeyguardStateCallback().remove(size);
                    }
                }
            }
        }
        if (OpUtils.isCustomFingerprint()) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dispatchAuthenticateChanged(z, i, i2, 0);
        }
    }

    public boolean isScreenOffAuthenticating() {
        return this.mAuthenticatingType != 0;
    }

    public void notifyLidSwitchChanged(boolean z) {
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.notifyLidSwitchChanged(z);
        }
    }

    public void changePanelAlpha(int i, int i2) {
        StatusBar statusBar = this.mStatusBar;
        if (statusBar != null) {
            String str = "OpKeyguardViewMediator";
            if (i > 0 && statusBar.isShowingWallpaper() && !isScreenOffAuthenticating()) {
                Log.d(str, "not set backdrop alpha");
            } else if (i2 == AUTHENTICATE_IGNORE || !isScreenOffAuthenticating() || i2 == this.mAuthenticatingType) {
                StringBuilder sb = new StringBuilder();
                sb.append("changePanelAlpha to ");
                sb.append(i);
                sb.append(", type:");
                sb.append(i2);
                sb.append(", currentType:");
                sb.append(this.mAuthenticatingType);
                Log.d(str, sb.toString());
                float f = (float) i;
                this.mStatusBar.setPanelViewAlpha(f, false, this.mAuthenticatingType);
                this.mStatusBar.setWallpaperAlpha(f);
                this.mLastAlpha = i;
            } else {
                Log.d(str, "return set alpha");
            }
        }
    }

    public void notifyBarHeightChange(int i) {
        if (this.mLastAlpha == 0) {
            Log.d("OpKeyguardViewMediator", "recover alpha");
            this.mStatusBar.setWallpaperAlpha(1.0f);
            this.mStatusBar.setPanelViewAlpha(1.0f, false, -1);
            this.mLastAlpha = 1;
        }
    }

    public void notifyPreventModeChange(boolean z) {
        for (int size = getKeyguardStateCallback().size() - 1; size >= 0; size--) {
            try {
                ((IKeyguardStateCallback) getKeyguardStateCallback().get(size)).onPocketModeActiveChanged(z);
            } catch (RemoteException e) {
                Log.w("OpKeyguardViewMediator", "Failed to call onPocketModeActiveChanged", e);
                if (e instanceof DeadObjectException) {
                    getKeyguardStateCallback().remove(size);
                }
            }
        }
    }

    public void onWakeAndUnlocking(boolean z) {
        onWakeAndUnlocking();
        if (!IS_CUSTOM_FINGERPRINT || !z || KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockUnlocking()) {
            this.mPendingHidePanel = true;
            return;
        }
        changePanelAlpha(0, AUTHENTICATE_FINGERPRINT);
        StatusBar statusBar = this.mStatusBar;
        if (statusBar != null) {
            statusBar.onWakingAndUnlocking();
        }
    }

    private OpHandler getHandler() {
        return (OpHandler) OpReflectionUtils.getValue(KeyguardViewMediator.class, this, "mHandler");
    }

    private Runnable getKeyguardGoingAwayRunnableRunnable() {
        return (Runnable) OpReflectionUtils.getValue(KeyguardViewMediator.class, this, "mKeyguardGoingAwayRunnable");
    }

    private void onWakeAndUnlocking() {
        OpReflectionUtils.methodInvokeVoid(KeyguardViewMediator.class, this, "onWakeAndUnlocking", new Object[0]);
    }

    private ArrayList<IKeyguardStateCallback> getKeyguardStateCallback() {
        return (ArrayList) OpReflectionUtils.getValue(KeyguardViewMediator.class, this, "mKeyguardStateCallbacks");
    }
}
