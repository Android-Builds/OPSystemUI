package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeHost.PulseCallback;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.phone.ScrimController.Callback;
import com.oneplus.aod.OpDozeScrimController;

public class DozeScrimController extends OpDozeScrimController implements StateListener {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public final DozeParameters mDozeParameters;
    /* access modifiers changed from: private */
    public boolean mDozing;
    /* access modifiers changed from: private */
    public boolean mFullyPulsing;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private PulseCallback mPulseCallback;
    /* access modifiers changed from: private */
    public final Runnable mPulseOut;
    /* access modifiers changed from: private */
    public final Runnable mPulseOutExtended;
    /* access modifiers changed from: private */
    public int mPulseReason;
    /* access modifiers changed from: private */
    public boolean mRequestPulsing;
    private final Callback mScrimCallback;

    public void onStateChanged(int i) {
    }

    public DozeScrimController(DozeParameters dozeParameters) {
        this.mHandler = new Handler();
        this.mScrimCallback = new Callback() {
            public void onDisplayBlanked() {
                if (DozeScrimController.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Pulse in, mDozing=");
                    sb.append(DozeScrimController.this.mDozing);
                    sb.append(", mPulseReason=");
                    sb.append(DozeLog.reasonToString(DozeScrimController.this.mPulseReason));
                    sb.append(", mRequestPulsing=");
                    sb.append(DozeScrimController.this.mRequestPulsing);
                    Log.d("DozeScrimController", sb.toString());
                }
                if (DozeScrimController.this.mDozing && DozeScrimController.this.mRequestPulsing) {
                    DozeScrimController.this.pulseStarted();
                }
            }

            public void onFinished() {
                if (DozeScrimController.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Pulse in finished, mDozing=");
                    sb.append(DozeScrimController.this.mDozing);
                    Log.d("DozeScrimController", sb.toString());
                }
                if (DozeScrimController.this.mDozing) {
                    DozeScrimController.this.acquireWakeLock();
                    DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOut, (long) DozeScrimController.this.mDozeParameters.getPulseVisibleDuration(DozeScrimController.this.mPulseReason));
                    DozeScrimController.this.mFullyPulsing = true;
                }
            }

            public void onCancelled() {
                DozeScrimController.this.pulseFinished();
            }

            public boolean shouldTimeoutWallpaper() {
                return DozeScrimController.this.mPulseReason == 6;
            }
        };
        this.mPulseOutExtended = new Runnable() {
            public void run() {
                DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOut);
                DozeScrimController.this.mPulseOut.run();
            }
        };
        this.mPulseOut = new Runnable() {
            public void run() {
                DozeScrimController.this.releaseWaleLock();
                DozeScrimController.this.mFullyPulsing = false;
                DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOut);
                DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOutExtended);
                if (DozeScrimController.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Pulse out, mDozing=");
                    sb.append(DozeScrimController.this.mDozing);
                    Log.d("DozeScrimController", sb.toString());
                }
                if (DozeScrimController.this.mDozing) {
                    DozeScrimController.this.pulseFinished();
                }
            }
        };
        this.mDozeParameters = dozeParameters;
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public DozeScrimController(Context context, DozeParameters dozeParameters) {
        this(dozeParameters);
        initWakeLock(context);
    }

    @VisibleForTesting
    public void setDozing(boolean z) {
        if (this.mDozing != z) {
            this.mDozing = z;
            if (!this.mDozing) {
                cancelPulsing();
            }
        }
    }

    public void pulse(PulseCallback pulseCallback, int i) {
        if (pulseCallback != null) {
            boolean z = true;
            if (!this.mDozing || this.mPulseCallback != null) {
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Pulse supressed. Dozing: ");
                    sb.append(this.mDozeParameters);
                    sb.append(" had callback? ");
                    if (this.mPulseCallback == null) {
                        z = false;
                    }
                    sb.append(z);
                    Log.d("DozeScrimController", sb.toString());
                }
                pulseCallback.onPulseFinished();
                return;
            }
            this.mRequestPulsing = true;
            this.mPulseCallback = pulseCallback;
            this.mPulseReason = i;
            this.mScrimCallback.onDisplayBlanked();
            return;
        }
        throw new IllegalArgumentException("callback must not be null");
    }

    public void pulseOutNow() {
        if (this.mPulseCallback != null && this.mFullyPulsing) {
            this.mPulseOut.run();
        }
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    public void extendPulse() {
        this.mHandler.removeCallbacks(this.mPulseOut);
    }

    public void extendPulse(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("extendPulse: ");
            sb.append(i);
            Log.d("DozeScrimController", sb.toString());
        }
        this.mHandler.removeCallbacks(this.mPulseOut);
        this.mPulseReason = i;
        this.mHandler.postDelayed(this.mPulseOut, (long) this.mDozeParameters.getPulseVisibleDuration(i));
    }

    private void cancelPulsing() {
        if (this.mPulseCallback != null) {
            if (DEBUG) {
                Log.d("DozeScrimController", "Cancel pulsing");
            }
            this.mFullyPulsing = false;
            this.mHandler.removeCallbacks(this.mPulseOut);
            this.mHandler.removeCallbacks(this.mPulseOutExtended);
            pulseFinished();
        }
    }

    /* access modifiers changed from: private */
    public void pulseStarted() {
        DozeLog.tracePulseStart(this.mPulseReason);
        PulseCallback pulseCallback = this.mPulseCallback;
        if (pulseCallback != null) {
            pulseCallback.onPulseStarted();
            this.mScrimCallback.onFinished();
        }
    }

    /* access modifiers changed from: private */
    public void pulseFinished() {
        DozeLog.tracePulseFinish();
        this.mRequestPulsing = false;
        PulseCallback pulseCallback = this.mPulseCallback;
        if (pulseCallback != null) {
            pulseCallback.onPulseFinished();
            this.mPulseCallback = null;
        }
        releaseWaleLock();
    }

    public Callback getScrimCallback() {
        return this.mScrimCallback;
    }

    public void onDozingChanged(boolean z) {
        setDozing(z);
    }
}
