package com.android.systemui.statusbar.phone;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.MathUtils;
import android.util.TimeUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Interpolators;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LightBarTransitionsController implements Dumpable, Callbacks, StateListener {
    private final DarkIntensityApplier mApplier;
    private boolean mBypassTransition;
    private final Context mContext;
    private float mDarkIntensity;
    private int mDisplayId;
    private float mDozeAmount;
    private final Handler mHandler;
    private final KeyguardMonitor mKeyguardMonitor;
    private float mNextDarkIntensity;
    private float mPendingDarkIntensity;
    private final StatusBarStateController mStatusBarStateController;
    private ValueAnimator mTintAnimator;
    private boolean mTintChangePending;
    /* access modifiers changed from: private */
    public boolean mTransitionDeferring;
    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() {
        public void run() {
            LightBarTransitionsController.this.mTransitionDeferring = false;
        }
    };
    private long mTransitionDeferringDuration;
    private long mTransitionDeferringStartTime;
    private boolean mTransitionPending;

    public interface DarkIntensityApplier {
        void applyDarkIntensity(float f);

        int getTintAnimationDuration();
    }

    public void onStateChanged(int i) {
    }

    public LightBarTransitionsController(Context context, DarkIntensityApplier darkIntensityApplier) {
        this.mApplier = darkIntensityApplier;
        this.mHandler = new Handler();
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).addCallback((Callbacks) this);
        this.mStatusBarStateController.addCallback(this);
        this.mDozeAmount = this.mStatusBarStateController.getDozeAmount();
        this.mContext = context;
        this.mDisplayId = this.mContext.getDisplayId();
        StringBuilder sb = new StringBuilder();
        sb.append("TransitionsController:");
        sb.append(this.mApplier);
        sb.append(" init:");
        Log.d("LightBar", sb.toString());
    }

    public void destroy(Context context) {
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).removeCallback((Callbacks) this);
        this.mStatusBarStateController.removeCallback(this);
    }

    public void saveState(Bundle bundle) {
        ValueAnimator valueAnimator = this.mTintAnimator;
        bundle.putFloat("dark_intensity", (valueAnimator == null || !valueAnimator.isRunning()) ? this.mDarkIntensity : this.mNextDarkIntensity);
    }

    public void restoreState(Bundle bundle) {
        setIconTintInternal(bundle.getFloat("dark_intensity", 0.0f));
    }

    public void appTransitionPending(int i, boolean z) {
        if (this.mDisplayId != i) {
            return;
        }
        if (!this.mKeyguardMonitor.isKeyguardGoingAway() || z) {
            this.mTransitionPending = true;
        }
    }

    public void appTransitionCancelled(int i) {
        if (this.mDisplayId == i) {
            if (this.mTransitionPending && this.mTintChangePending) {
                this.mTintChangePending = false;
                animateIconTint(this.mPendingDarkIntensity, 0, (long) this.mApplier.getTintAnimationDuration());
            }
            this.mTransitionPending = false;
        }
    }

    public void appTransitionStarting(int i, long j, long j2, boolean z) {
        if (this.mDisplayId != i) {
            return;
        }
        if (!this.mKeyguardMonitor.isKeyguardGoingAway() || z) {
            if (this.mTransitionPending && this.mTintChangePending) {
                this.mTintChangePending = false;
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("TransitionsController:");
                    sb.append(this.mApplier);
                    sb.append(" TransitionStarting, ");
                    sb.append(this.mPendingDarkIntensity);
                    String str = ", ";
                    sb.append(str);
                    sb.append(j2);
                    sb.append(str);
                    sb.append(z);
                    sb.append(str);
                    sb.append(this.mNextDarkIntensity);
                    Log.d("LightBar", sb.toString());
                }
                animateIconTint(this.mPendingDarkIntensity, Math.max(0, j - SystemClock.uptimeMillis()), j2);
            } else if (this.mTransitionPending) {
                this.mTransitionDeferring = true;
                this.mTransitionDeferringStartTime = j;
                this.mTransitionDeferringDuration = j2;
                this.mHandler.removeCallbacks(this.mTransitionDeferringDoneRunnable);
                this.mHandler.postAtTime(this.mTransitionDeferringDoneRunnable, j);
            }
            this.mTransitionPending = false;
        }
    }

    public void setIconsDark(boolean z, boolean z2) {
        String str = "LightBar";
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("TransitionsController:");
            sb.append(this.mApplier);
            sb.append(" setIconsDark, ");
            sb.append(z);
            String str2 = ",";
            sb.append(str2);
            sb.append(z2);
            sb.append(str2);
            sb.append(this.mTransitionPending);
            sb.append(str2);
            sb.append(this.mTransitionDeferring);
            sb.append(str2);
            sb.append(this.mNextDarkIntensity);
            sb.append(" mPendingDarkIntensity:");
            sb.append(this.mPendingDarkIntensity);
            Log.d(str, sb.toString());
        }
        float f = 1.0f;
        if (!z2) {
            setIconTintInternal(z ? 1.0f : 0.0f);
            if (!z) {
                f = 0.0f;
            }
            this.mNextDarkIntensity = f;
        } else if (this.mTransitionPending && !this.mBypassTransition) {
            if (!z) {
                f = 0.0f;
            }
            deferIconTintChange(f);
        } else if (this.mTransitionDeferring) {
            animateIconTint(z ? 1.0f : 0.0f, Math.max(0, this.mTransitionDeferringStartTime - SystemClock.uptimeMillis()), this.mTransitionDeferringDuration);
        } else {
            animateIconTint(z ? 1.0f : 0.0f, 0, (long) this.mApplier.getTintAnimationDuration());
        }
        if (this.mBypassTransition) {
            this.mBypassTransition = false;
            this.mTransitionPending = false;
            Log.i(str, "Force disable TransitionPending");
        }
    }

    public void bypassTransition(boolean z) {
        this.mBypassTransition = z;
    }

    public float getCurrentDarkIntensity() {
        return this.mDarkIntensity;
    }

    private void deferIconTintChange(float f) {
        if (!this.mTintChangePending || f != this.mPendingDarkIntensity) {
            this.mTintChangePending = true;
            this.mPendingDarkIntensity = f;
        }
    }

    private void animateIconTint(float f, long j, long j2) {
        if (this.mNextDarkIntensity != f) {
            ValueAnimator valueAnimator = this.mTintAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            this.mNextDarkIntensity = f;
            this.mTintAnimator = ValueAnimator.ofFloat(new float[]{this.mDarkIntensity, f});
            this.mTintAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    LightBarTransitionsController.this.lambda$animateIconTint$0$LightBarTransitionsController(valueAnimator);
                }
            });
            this.mTintAnimator.setDuration(j2);
            this.mTintAnimator.setStartDelay(j);
            this.mTintAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            this.mTintAnimator.start();
        }
    }

    public /* synthetic */ void lambda$animateIconTint$0$LightBarTransitionsController(ValueAnimator valueAnimator) {
        setIconTintInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    private void setIconTintInternal(float f) {
        this.mDarkIntensity = f;
        dispatchDark();
    }

    private void dispatchDark() {
        this.mApplier.applyDarkIntensity(MathUtils.lerp(this.mDarkIntensity, 0.0f, this.mDozeAmount));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mTransitionDeferring=");
        printWriter.print(this.mTransitionDeferring);
        if (this.mTransitionDeferring) {
            printWriter.println();
            printWriter.print("   mTransitionDeferringStartTime=");
            printWriter.println(TimeUtils.formatUptime(this.mTransitionDeferringStartTime));
            printWriter.print("   mTransitionDeferringDuration=");
            TimeUtils.formatDuration(this.mTransitionDeferringDuration, printWriter);
            printWriter.println();
        }
        printWriter.print("  mTransitionPending=");
        printWriter.print(this.mTransitionPending);
        printWriter.print(" mTintChangePending=");
        printWriter.println(this.mTintChangePending);
        printWriter.print("  mPendingDarkIntensity=");
        printWriter.print(this.mPendingDarkIntensity);
        printWriter.print(" mDarkIntensity=");
        printWriter.print(this.mDarkIntensity);
        printWriter.print(" mNextDarkIntensity=");
        printWriter.println(this.mNextDarkIntensity);
    }

    public void onDozeAmountChanged(float f, float f2) {
        this.mDozeAmount = f2;
        dispatchDark();
    }
}
