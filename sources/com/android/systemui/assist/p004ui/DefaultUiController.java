package com.android.systemui.assist.p004ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.metrics.LogMaker;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R$layout;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.assist.AssistManager.UiController;

/* renamed from: com.android.systemui.assist.ui.DefaultUiController */
public class DefaultUiController implements UiController {
    private boolean mAttached = false;
    private ValueAnimator mInvocationAnimator = new ValueAnimator();
    /* access modifiers changed from: private */
    public boolean mInvocationInProgress = false;
    protected InvocationLightsView mInvocationLightsView;
    /* access modifiers changed from: private */
    public float mLastInvocationProgress = 0.0f;
    private final LayoutParams mLayoutParams;
    private final PathInterpolator mProgressInterpolator = new PathInterpolator(0.83f, 0.0f, 0.84f, 1.0f);
    protected final FrameLayout mRoot;
    private final WindowManager mWindowManager;

    public DefaultUiController(Context context) {
        this.mRoot = new FrameLayout(context);
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        LayoutParams layoutParams = new LayoutParams(-1, -2, 0, 0, 2024, 808, -3);
        this.mLayoutParams = layoutParams;
        LayoutParams layoutParams2 = this.mLayoutParams;
        layoutParams2.privateFlags = 64;
        layoutParams2.gravity = 80;
        layoutParams2.setTitle("Assist");
        this.mInvocationLightsView = (InvocationLightsView) LayoutInflater.from(context).inflate(R$layout.invocation_lights, this.mRoot, false);
        this.mRoot.addView(this.mInvocationLightsView);
    }

    public void onInvocationProgress(int i, float f) {
        boolean z = this.mInvocationInProgress;
        String str = "DefaultUiController";
        if (f == 1.0f) {
            StringBuilder sb = new StringBuilder();
            sb.append("onInvocationProgress 1, type:");
            sb.append(i);
            Log.d(str, sb.toString());
            animateInvocationCompletion(i, 0.0f);
        } else if (f == 0.0f) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onInvocationProgress 0, type:");
            sb2.append(i);
            Log.d(str, sb2.toString());
            hide();
        } else {
            if (!z) {
                attach();
                this.mInvocationInProgress = true;
                updateAssistHandleVisibility();
            }
            setProgressInternal(i, f);
        }
        this.mLastInvocationProgress = f;
        logInvocationProgressMetrics(i, f, z);
    }

    public void onGestureCompletion(float f) {
        StringBuilder sb = new StringBuilder();
        sb.append("onGestureCompletion v:");
        sb.append(f);
        Log.d("DefaultUiController", sb.toString());
        animateInvocationCompletion(1, f);
    }

    public void hide() {
        ((AssistManager) Dependency.get(AssistManager.class)).hideAssist();
        detach();
        if (this.mInvocationAnimator.isRunning()) {
            this.mInvocationAnimator.cancel();
        }
        this.mInvocationLightsView.hide();
        this.mInvocationInProgress = false;
        updateAssistHandleVisibility();
    }

    protected static void logInvocationProgressMetrics(int i, float f, boolean z) {
        if (!z && f > 0.0f) {
            MetricsLogger.action(new LogMaker(1716).setType(4).setSubtype(((AssistManager) Dependency.get(AssistManager.class)).toLoggingSubType(i)));
        }
        if (z && f == 0.0f) {
            MetricsLogger.action(new LogMaker(1716).setType(5).setSubtype(1));
        }
    }

    private void updateAssistHandleVisibility() {
        ((ScreenDecorations) SysUiServiceProvider.getComponent(this.mRoot.getContext(), ScreenDecorations.class)).lambda$setAssistHintBlocked$2$ScreenDecorations(this.mInvocationInProgress);
    }

    private void attach() {
        if (!this.mAttached) {
            this.mWindowManager.addView(this.mRoot, this.mLayoutParams);
            this.mAttached = true;
        }
    }

    private void detach() {
        if (this.mAttached) {
            this.mWindowManager.removeViewImmediate(this.mRoot);
            this.mAttached = false;
        }
    }

    private void setProgressInternal(int i, float f) {
        this.mInvocationLightsView.onInvocationProgress(this.mProgressInterpolator.getInterpolation(f));
    }

    private void animateInvocationCompletion(int i, float f) {
        this.mInvocationAnimator = ValueAnimator.ofFloat(new float[]{this.mLastInvocationProgress, 1.0f});
        this.mInvocationAnimator.setStartDelay(1);
        this.mInvocationAnimator.setDuration(200);
        this.mInvocationAnimator.addUpdateListener(new AnimatorUpdateListener(i) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                DefaultUiController.this.lambda$animateInvocationCompletion$0$DefaultUiController(this.f$1, valueAnimator);
            }
        });
        this.mInvocationAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                DefaultUiController.this.mInvocationInProgress = false;
                DefaultUiController.this.mLastInvocationProgress = 0.0f;
                DefaultUiController.this.hide();
            }
        });
        this.mInvocationAnimator.start();
    }

    public /* synthetic */ void lambda$animateInvocationCompletion$0$DefaultUiController(int i, ValueAnimator valueAnimator) {
        setProgressInternal(i, ((Float) valueAnimator.getAnimatedValue()).floatValue());
    }
}
