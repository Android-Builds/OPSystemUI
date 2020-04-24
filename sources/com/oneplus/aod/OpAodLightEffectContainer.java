package com.oneplus.aod;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.oneplus.util.OpUtils;

public class OpAodLightEffectContainer extends FrameLayout {
    /* access modifiers changed from: private */
    public int mAnimateIndex = 0;
    private Bitmap mAnimationBgLeft;
    private Bitmap mAnimationBgRight;
    /* access modifiers changed from: private */
    public Handler mBgHandler;
    /* access modifiers changed from: private */
    public Bitmap[] mBitmapLeft;
    /* access modifiers changed from: private */
    public Bitmap[] mBitmapRight;
    /* access modifiers changed from: private */
    public int mDecodeIndex = 0;
    /* access modifiers changed from: private */
    public final Runnable mFrameRunnable = new Runnable() {
        public void run() {
            if (OpAodLightEffectContainer.this.mAnimateIndex >= 0 && OpAodLightEffectContainer.this.mAnimateIndex < OpAodLightEffectContainer.this.mBitmapLeft.length) {
                OpAodLightEffectContainer.this.mLeftView.setImageBitmap(OpAodLightEffectContainer.this.mBitmapLeft[OpAodLightEffectContainer.this.mAnimateIndex]);
            }
            if (OpAodLightEffectContainer.this.mAnimateIndex >= 0 && OpAodLightEffectContainer.this.mAnimateIndex < OpAodLightEffectContainer.this.mBitmapRight.length) {
                OpAodLightEffectContainer.this.mRightView.setImageBitmap(OpAodLightEffectContainer.this.mBitmapRight[OpAodLightEffectContainer.this.mAnimateIndex]);
            }
            if (OpAodLightEffectContainer.this.mAnimateIndex < 100) {
                OpAodLightEffectContainer.this.mHandler.postDelayed(OpAodLightEffectContainer.this.mFrameRunnable, 16);
            } else {
                OpAodLightEffectContainer.this.mLeftView.setImageBitmap(null);
                OpAodLightEffectContainer.this.mRightView.setImageBitmap(null);
            }
            OpAodLightEffectContainer.this.mAnimateIndex = OpAodLightEffectContainer.this.mAnimateIndex + 1;
        }
    };
    /* access modifiers changed from: private */
    public Handler mHandler;
    private HandlerThread mHandlerThread;
    /* access modifiers changed from: private */
    public ImageView mLeftView;
    private ValueAnimator mLightAnimator;
    private int mLightIndex = 0;
    /* access modifiers changed from: private */
    public ImageView mRightView;

    public OpAodLightEffectContainer(Context context) {
        super(context);
        initViews();
    }

    public OpAodLightEffectContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpAodLightEffectContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public OpAodLightEffectContainer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        this.mLeftView = (ImageView) findViewById(R$id.notification_animation_left);
        this.mRightView = (ImageView) findViewById(R$id.notification_animation_right);
        relayoutViews();
    }

    private void relayoutViews() {
        boolean z = this.mLightIndex == 10;
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(z ? R$dimen.op_aod_light_effect_my_width : R$dimen.op_aod_light_effect_width);
        this.mLeftView.getLayoutParams().width = dimensionPixelSize;
        this.mRightView.getLayoutParams().width = dimensionPixelSize;
        if (z) {
            setScaleY(1.0f);
            setAlpha(1.0f);
            this.mLeftView.setScaleType(ScaleType.FIT_XY);
            this.mRightView.setScaleType(ScaleType.FIT_XY);
        }
    }

    public void setLightIndex(int i) {
        if (this.mLightIndex != i) {
            if (OpUtils.isMCLVersion() || i != 10) {
                boolean z = false;
                if (this.mLightIndex == 10 || i == 10) {
                    z = true;
                }
                this.mLightIndex = i;
                if (z) {
                    relayoutViews();
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Set horizon light failed. Invalid index: ");
                sb.append(i);
                Log.d("OpAodLightEffectContainer", sb.toString());
            }
        }
    }

    public void resetNotificationAnimView() {
        if (this.mLightIndex != 10) {
            setScaleY(0.0f);
            setAlpha(0.0f);
        }
        this.mRightView.setImageBitmap(null);
        this.mLeftView.setImageBitmap(null);
        Bitmap bitmap = this.mAnimationBgRight;
        if (bitmap != null) {
            bitmap.recycle();
            this.mAnimationBgRight = null;
        }
        Bitmap bitmap2 = this.mAnimationBgLeft;
        if (bitmap2 != null) {
            bitmap2.recycle();
            this.mAnimationBgLeft = null;
        }
        Handler handler = this.mBgHandler;
        if (handler != null) {
            handler.removeMessages(256);
            this.mBgHandler = null;
        }
        HandlerThread handlerThread = this.mHandlerThread;
        if (handlerThread != null) {
            handlerThread.getLooper().quit();
            this.mHandlerThread = null;
        }
        Handler handler2 = this.mHandler;
        if (handler2 != null) {
            handler2.removeCallbacks(this.mFrameRunnable);
            this.mHandler = null;
        }
        this.mDecodeIndex = 0;
        this.mAnimateIndex = 0;
        Bitmap[] bitmapArr = this.mBitmapLeft;
        if (bitmapArr != null) {
            for (Bitmap bitmap3 : bitmapArr) {
                if (bitmap3 != null) {
                    bitmap3.recycle();
                }
            }
        }
        Bitmap[] bitmapArr2 = this.mBitmapRight;
        if (bitmapArr2 != null) {
            for (Bitmap bitmap4 : bitmapArr2) {
                if (bitmap4 != null) {
                    bitmap4.recycle();
                }
            }
        }
    }

    public void showLight() {
        if (this.mLightIndex == 10) {
            prepareResources();
            int i = this.mAnimateIndex;
            if (i <= 0 || i >= 100) {
                this.mHandler.removeCallbacks(this.mFrameRunnable);
                this.mHandler.postDelayed(this.mFrameRunnable, 350);
                return;
            }
            return;
        }
        setAlpha(1.0f);
        ValueAnimator valueAnimator = this.mLightAnimator;
        if (valueAnimator == null || (valueAnimator != null && !valueAnimator.isRunning())) {
            loadResources();
            animateNotification();
        }
    }

    private void prepareResources() {
        if (this.mBitmapLeft == null) {
            this.mBitmapLeft = new Bitmap[100];
        }
        if (this.mBitmapRight == null) {
            this.mBitmapRight = new Bitmap[100];
        }
        if (this.mHandler == null) {
            this.mHandler = new Handler();
        }
        startHandlerThread();
    }

    private void startHandlerThread() {
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread("HandlerThread");
            this.mHandlerThread.start();
            this.mBgHandler = new Handler(this.mHandlerThread.getLooper()) {
                public void handleMessage(Message message) {
                    super.handleMessage(message);
                    if (message.what == 256 && OpAodLightEffectContainer.this.mDecodeIndex < 100) {
                        OpAodLightEffectContainer.this.mBgHandler.sendEmptyMessage(256);
                        OpAodLightEffectContainer.this.decodeBitmap();
                    }
                }
            };
            this.mBgHandler.sendEmptyMessage(256);
        }
    }

    /* access modifiers changed from: private */
    public void decodeBitmap() {
        if (this.mBitmapLeft == null || this.mBitmapRight == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to decodeBitmap. mBitmapLeft=");
            sb.append(this.mBitmapLeft);
            sb.append(" mBitmapRight=");
            sb.append(this.mBitmapRight);
            Log.d("OpAodLightEffectContainer", sb.toString());
            return;
        }
        int i = this.mDecodeIndex;
        Resources resources = this.mContext.getResources();
        StringBuilder sb2 = new StringBuilder();
        sb2.append("aod_light_my_");
        sb2.append(String.format("%02d", new Object[]{Integer.valueOf(i)}));
        Bitmap decodeResource = BitmapFactory.decodeResource(resources, resources.getIdentifier(sb2.toString(), "drawable", this.mContext.getPackageName()));
        this.mBitmapRight[i] = decodeResource;
        this.mBitmapLeft[i] = flip(decodeResource);
        this.mDecodeIndex++;
    }

    private void loadResources() {
        this.mAnimationBgRight = BitmapFactory.decodeResource(this.mContext.getResources(), R$drawable.aod_notification_light_right);
        this.mAnimationBgLeft = BitmapFactory.decodeResource(this.mContext.getResources(), R$drawable.aod_notification_light_left);
        this.mLeftView.setImageBitmap(this.mAnimationBgLeft);
        this.mRightView.setImageBitmap(this.mAnimationBgRight);
    }

    private Bitmap flip(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    private void animateNotification() {
        this.mLightAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 2.0f});
        this.mLightAnimator.setDuration(2000);
        this.mLightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpAodLightEffectContainer.this.setScaleY(floatValue);
                StringBuilder sb = new StringBuilder();
                sb.append("progress=");
                sb.append(floatValue);
                Log.d("OpAodLightEffectContainer", sb.toString());
                float f = 1.0f;
                if (floatValue <= 0.3f) {
                    f = floatValue / 0.3f;
                } else if (floatValue >= 1.0f) {
                    f = 2.0f - floatValue;
                }
                OpAodLightEffectContainer.this.setAlpha(f);
            }
        });
        this.mLightAnimator.start();
    }
}
