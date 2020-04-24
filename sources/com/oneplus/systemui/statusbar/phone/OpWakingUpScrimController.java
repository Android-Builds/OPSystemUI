package com.oneplus.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.systemui.R$layout;
import com.oneplus.aod.OpWakingUpScrim;
import com.oneplus.util.OpUtils;

public class OpWakingUpScrimController {
    private Context mContext;
    private boolean mIsAddToWindow;
    /* access modifiers changed from: private */
    public boolean mIsAnimationStarted;
    private boolean mIsStart = false;
    private Handler mMainThreadhandler = new Handler(Looper.getMainLooper());
    private boolean mRequestShow;
    /* access modifiers changed from: private */
    public Animator mScrimAnimator;
    private OpWakingUpScrim mScrimView;
    private Handler mUIHandler;
    private HandlerThread mUIHandlerThread;
    private final WindowManager mWindowManager;

    public void initHandler(Looper looper) {
        this.mUIHandler = new Handler(looper) {
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 1) {
                    OpWakingUpScrimController.this.handleStartAnimation(((Boolean) message.obj).booleanValue());
                } else if (i == 2) {
                    OpWakingUpScrimController.this.handleAddToWindow(((Boolean) message.obj).booleanValue());
                } else if (i == 3) {
                    OpWakingUpScrimController.this.handleRemoveFromWindow();
                }
            }
        };
    }

    public OpWakingUpScrimController(Context context) {
        this.mContext = context;
        this.mScrimView = (OpWakingUpScrim) LayoutInflater.from(this.mContext).inflate(R$layout.op_wakingup_scrim_view, null);
        this.mUIHandlerThread = new HandlerThread("WakingUpScrimUI", -8);
        this.mUIHandlerThread.start();
        initHandler(this.mUIHandlerThread.getLooper());
        this.mWindowManager = (WindowManager) context.getSystemService("window");
    }

    public void prepare() {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.d("OpWakingUpScrimController", "AddToWindow");
        }
        boolean z = true;
        this.mRequestShow = true;
        if (this.mIsAnimationStarted && this.mScrimAnimator != null) {
            this.mUIHandler.post(new Runnable() {
                public void run() {
                    OpWakingUpScrimController.this.mScrimAnimator.cancel();
                }
            });
        }
        if (!this.mUIHandler.hasMessages(3) && !this.mScrimView.isAttachedToWindow()) {
            z = false;
        }
        this.mUIHandler.removeMessages(3);
        this.mUIHandler.removeMessages(2);
        this.mUIHandler.obtainMessage(2, Boolean.valueOf(z)).sendToTarget();
    }

    public void handleAddToWindow(boolean z) {
        String str = "OpWakingUpScrimController";
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleAddToWindow:");
            sb.append(this.mScrimView.isAttachedToWindow());
            sb.append(" justResetState:");
            sb.append(z);
            sb.append(" mRequestShow: ");
            sb.append(this.mRequestShow);
            Log.d(str, sb.toString());
        }
        if (this.mScrimView.isAttachedToWindow() || !this.mRequestShow) {
            this.mScrimView.reset();
            return;
        }
        this.mIsAddToWindow = true;
        try {
            this.mWindowManager.addView(this.mScrimView, getWindowLayoutParams());
        } catch (IllegalStateException e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("addView occur exception. isAttachedToWindow= ");
            sb2.append(this.mScrimView.isAttachedToWindow());
            Log.e(str, sb2.toString(), e);
        }
        this.mIsAnimationStarted = false;
        int systemUiVisibility = this.mScrimView.getSystemUiVisibility() | 1792;
        this.mScrimView.reset();
        this.mScrimView.setVisibility(0);
        this.mScrimView.setSystemUiVisibility(systemUiVisibility);
    }

    public void startAnimation(boolean z) {
        String str = "OpWakingUpScrimController";
        if (!this.mScrimView.isAttachedToWindow()) {
            Log.w(str, "stop startAnimation window desn't attached");
        } else if (this.mIsAnimationStarted) {
            Log.w(str, "stop startAnimation since it's started");
        } else {
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("startAnimation:");
                sb.append(z);
                Log.d(str, sb.toString());
            }
            this.mRequestShow = false;
            this.mUIHandler.removeMessages(1);
            this.mUIHandler.obtainMessage(1, Boolean.valueOf(z)).sendToTarget();
            this.mIsAnimationStarted = true;
        }
    }

    public void handleStartAnimation(boolean z) {
        Animator animator = this.mScrimAnimator;
        String str = "OpWakingUpScrimController";
        if (animator == null || !animator.isRunning()) {
            if (this.mIsAnimationStarted) {
                Animator animator2 = this.mScrimAnimator;
                if (animator2 != null) {
                    animator2.cancel();
                }
            }
            if (z) {
                this.mScrimAnimator = this.mScrimView.getDisappearAnimationWithDelay();
            } else {
                this.mScrimAnimator = this.mScrimView.getDisappearAnimationWithoutDelay();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("handleStartAnimation withDelay:");
            sb.append(z);
            Log.w(str, sb.toString());
            this.mScrimAnimator.addListener(new AnimatorListenerAdapter() {
                boolean mCancelled;

                public void onAnimationStart(Animator animator) {
                    this.mCancelled = false;
                    Log.i("OpWakingUpScrimController", "WakingUpScrimView onAnimationStart");
                }

                public void onAnimationCancel(Animator animator) {
                    Log.i("OpWakingUpScrimController", "WakingUpScrimView onAnimationCancel");
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("WakingUpScrimView onAnimationEnd , cancel= ");
                    sb.append(this.mCancelled);
                    Log.i("OpWakingUpScrimController", sb.toString());
                    if (!this.mCancelled) {
                        OpWakingUpScrimController.this.selfRemoveFromWindow(false);
                    }
                    OpWakingUpScrimController.this.mIsAnimationStarted = false;
                }
            });
            this.mScrimAnimator.start();
            return;
        }
        Log.w(str, "animation running");
    }

    public void removeFromWindow(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.d("OpWakingUpScrimController", "removeFromWindow");
        }
        this.mRequestShow = false;
        selfRemoveFromWindow(z);
    }

    /* access modifiers changed from: private */
    public void selfRemoveFromWindow(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.d("OpWakingUpScrimController", "selfRemoveFromWindow");
        }
        this.mUIHandler.removeMessages(3);
        if (z) {
            this.mUIHandler.obtainMessage(3).sendToTarget();
            return;
        }
        Handler handler = this.mUIHandler;
        handler.sendMessageDelayed(handler.obtainMessage(3), 200);
    }

    /* access modifiers changed from: private */
    public void handleRemoveFromWindow() {
        Animator animator = this.mScrimAnimator;
        String str = "OpWakingUpScrimController";
        if (animator == null || !animator.isRunning()) {
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("handleRemoveFromWindow: mRequestShow= ");
                sb.append(this.mRequestShow);
                Log.d(str, sb.toString());
            }
            if (this.mScrimView.isAttachedToWindow() && !this.mRequestShow) {
                this.mIsAddToWindow = false;
                try {
                    this.mWindowManager.removeViewImmediate(this.mScrimView);
                } catch (IllegalStateException e) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("removeViewImmediate occur exception. isAttachedToWindow= ");
                    sb2.append(this.mScrimView.isAttachedToWindow());
                    Log.e(str, sb2.toString(), e);
                }
            }
            return;
        }
        Log.d(str, "animation still play, remove window until animation end");
    }

    private LayoutParams getWindowLayoutParams() {
        LayoutParams layoutParams = new LayoutParams();
        layoutParams.type = 2036;
        layoutParams.privateFlags = 16;
        layoutParams.layoutInDisplayCutoutMode = 1;
        layoutParams.flags = 1304;
        layoutParams.format = -2;
        layoutParams.width = -1;
        layoutParams.height = -1;
        layoutParams.gravity = 17;
        layoutParams.screenOrientation = 1;
        layoutParams.setTitle("OpWakingUpScrim");
        layoutParams.softInputMode = 3;
        return layoutParams;
    }
}
