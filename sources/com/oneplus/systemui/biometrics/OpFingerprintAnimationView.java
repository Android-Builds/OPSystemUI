package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OpFingerprintAnimationView extends ImageView {
    private final String TAG = "FingerprintAnimationView";
    private int mAnimationDuration;
    private AnimationDrawable mBackground;
    /* access modifiers changed from: private */
    public OpFingerprintAnimationCtrl mFingerprintAnimationCtrl;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            int i = message.what;
            if (i == 1) {
                OpFingerprintAnimationView.this.stopAnimation();
            } else if (i == 2 && OpFingerprintAnimationView.this.mFingerprintAnimationCtrl != null) {
                OpFingerprintAnimationView.this.mFingerprintAnimationCtrl.playAnimation(message.arg1);
            }
        }
    };

    public OpFingerprintAnimationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public OpFingerprintAnimationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public OpFingerprintAnimationView(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        this.mBackground = (AnimationDrawable) getBackground();
        AnimationDrawable animationDrawable = this.mBackground;
        if (animationDrawable != null) {
            this.mAnimationDuration = animationDrawable.getNumberOfFrames() * this.mBackground.getDuration(0);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void stopAnimation() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        AnimationDrawable animationDrawable = this.mBackground;
        if (animationDrawable != null) {
            animationDrawable.stop();
        }
        setVisibility(4);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
