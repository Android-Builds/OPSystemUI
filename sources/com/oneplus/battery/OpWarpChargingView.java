package com.oneplus.battery;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.airbnb.lottie.C0526R;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.R$array;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.oneplus.util.OpUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class OpWarpChargingView extends FrameLayout {
    private static final Interpolator ANIMATION_INTERPILATOR = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    static final boolean DEBUG = OpUtils.DEBUG_ONEPLUS;
    private static Resources mRes;
    private final String TAG;
    /* access modifiers changed from: private */
    public boolean isAnimationStart;
    /* access modifiers changed from: private */
    public boolean mAssetLoaded;
    /* access modifiers changed from: private */
    public boolean mAssetLoading;
    /* access modifiers changed from: private */
    public boolean mAssetReleasing;
    private Handler mBackgroundHandler;
    /* access modifiers changed from: private */
    public ImageView mBackgroundView;
    private TextView mBatteryLevel;
    private AnimatorSet mChargeAnimation;
    /* access modifiers changed from: private */
    public OpChargingAnimationController mChargingAnimationController;
    private Context mContext;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public View mInfoView;
    /* access modifiers changed from: private */
    public boolean mIsPaddingStartAnimation;
    private boolean mPlugin;
    private View mScrim;
    ArrayList<Bitmap> mStartAnimationAssets1;
    private Bitmap mWallpaper;
    private ImageView mWallpaperView;
    private ImageView mWrapview;

    private void refresh() {
    }

    public void onConfigurationChanged(Configuration configuration) {
    }

    public OpWarpChargingView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.TAG = "OpWarpChargingView";
        this.mPlugin = false;
        this.mBackgroundHandler = new Handler(BackgroundThread.getHandler().getLooper());
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mStartAnimationAssets1 = new ArrayList<>();
        this.mAssetLoading = false;
        this.mAssetLoaded = false;
        this.mAssetReleasing = false;
        this.mIsPaddingStartAnimation = false;
        this.mWallpaper = null;
        this.mContext = context;
        mRes = this.mContext.getResources();
    }

    public OpWarpChargingView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OpWarpChargingView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public OpWarpChargingView(Context context) {
        this(context, null, 0, 0);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryLevel = (TextView) findViewById(R$id.battery_level);
        this.mWrapview = (ImageView) findViewById(R$id.wrap_view);
        this.mBackgroundView = (ImageView) findViewById(R$id.background_view);
        this.mWallpaperView = (ImageView) findViewById(R$id.wallpaper_view);
        this.mWallpaperView.setImageBitmap(this.mWallpaper);
        this.mInfoView = findViewById(R$id.info_view);
        this.mScrim = findViewById(R$id.scrim_view);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (i == 0) {
            refresh();
        }
    }

    public void setChargingAnimationController(OpChargingAnimationController opChargingAnimationController) {
        this.mChargingAnimationController = opChargingAnimationController;
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(i);
        stringBuffer.append("%");
        TextView textView = this.mBatteryLevel;
        if (textView != null) {
            textView.setText(stringBuffer.toString());
        }
    }

    public void startAnimation() {
        String str = "OpWarpChargingView";
        if (this.mAssetLoaded) {
            this.mIsPaddingStartAnimation = false;
            if (!this.isAnimationStart) {
                Log.i(str, "startAnimation");
                this.isAnimationStart = true;
                AnimatorSet animatorSet = this.mChargeAnimation;
                if (animatorSet != null) {
                    animatorSet.cancel();
                } else {
                    this.mChargeAnimation = getWarpFastChargeAnimation();
                }
                this.mChargeAnimation.start();
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.i(str, "startAnimation / else / prepareAsset");
        }
        this.mIsPaddingStartAnimation = true;
        prepareAsset();
    }

    public void stopAnimation() {
        if (this.isAnimationStart) {
            Log.i("OpWarpChargingView", "stopAnimation");
            AnimatorSet animatorSet = this.mChargeAnimation;
            if (animatorSet != null) {
                animatorSet.cancel();
            }
        }
    }

    private AnimatorSet getWarpFastChargeAnimation() {
        new ValueAnimator();
        ValueAnimator ofInt = ValueAnimator.ofInt(new int[]{0, this.mStartAnimationAssets1.size() - 1});
        ofInt.setDuration((long) 432);
        ofInt.setInterpolator(ANIMATION_INTERPILATOR);
        ofInt.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                OpWarpChargingView.this.mBackgroundView.setImageBitmap((Bitmap) OpWarpChargingView.this.mStartAnimationAssets1.get(((Integer) valueAnimator.getAnimatedValue()).intValue()));
            }
        });
        ofInt.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                OpWarpChargingView.this.mBackgroundView.setImageBitmap(null);
            }
        });
        new ValueAnimator();
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.05f});
        ofFloat.setDuration((long) 144);
        ofFloat.setInterpolator(ANIMATION_INTERPILATOR);
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpWarpChargingView.this.mInfoView.setScaleX(floatValue);
                OpWarpChargingView.this.mInfoView.setScaleY(floatValue);
            }
        });
        new ValueAnimator();
        ValueAnimator ofFloat2 = ValueAnimator.ofFloat(new float[]{1.05f, 1.0f});
        ofFloat2.setDuration((long) 560);
        ofFloat2.setInterpolator(ANIMATION_INTERPILATOR);
        ofFloat2.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpWarpChargingView.this.mInfoView.setScaleX(floatValue);
                OpWarpChargingView.this.mInfoView.setScaleY(floatValue);
            }
        });
        new ValueAnimator();
        ValueAnimator ofFloat3 = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        ofFloat3.setStartDelay(2000);
        ofFloat3.setDuration((long) 255);
        ofFloat3.setInterpolator(ANIMATION_INTERPILATOR);
        ofFloat3.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                OpWarpChargingView.this.mInfoView.setScaleX(floatValue);
                OpWarpChargingView.this.mInfoView.setScaleY(floatValue);
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                if (OpWarpChargingView.DEBUG) {
                    Log.i("OpWarpChargingView", "onAnimationStart()");
                }
                OpWarpChargingView.this.mInfoView.setScaleX(0.0f);
                OpWarpChargingView.this.mInfoView.setScaleY(0.0f);
                OpWarpChargingView.this.setVisibility(0);
                if (OpWarpChargingView.this.mChargingAnimationController != null) {
                    OpWarpChargingView.this.mChargingAnimationController.animationStart(C0526R.styleable.AppCompatTheme_textAppearanceListItem);
                }
            }

            public void onAnimationEnd(Animator animator) {
                if (OpWarpChargingView.DEBUG) {
                    Log.i("OpWarpChargingView", "onAnimationEnd()");
                }
                OpWarpChargingView.this.setVisibility(8);
                OpWarpChargingView.this.mInfoView.setScaleX(0.0f);
                OpWarpChargingView.this.mInfoView.setScaleY(0.0f);
                OpWarpChargingView.this.mBackgroundView.setImageBitmap(null);
                if (OpWarpChargingView.this.mChargingAnimationController != null) {
                    OpWarpChargingView.this.mChargingAnimationController.animationEnd(C0526R.styleable.AppCompatTheme_textAppearanceListItem);
                }
                OpWarpChargingView.this.isAnimationStart = false;
                OpWarpChargingView.this.releaseAsset();
            }
        });
        animatorSet.play(ofInt).before(ofFloat);
        animatorSet.play(ofFloat2).after(ofFloat);
        animatorSet.play(ofFloat3).after(ofFloat2);
        return animatorSet;
    }

    public void prepareAsset() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("prepareAsset() / mAssetLoading:");
            sb.append(this.mAssetLoading);
            sb.append(" / mAssetLoaded:");
            sb.append(this.mAssetLoaded);
            Log.i("OpWarpChargingView", sb.toString());
        }
        if (!this.mAssetLoading && !this.mAssetLoaded) {
            this.mAssetLoading = true;
            View view = this.mInfoView;
            if (view != null) {
                view.setBackgroundResource(R$drawable.fast_charging_background);
            }
            this.mBackgroundHandler.post(new Runnable() {
                public void run() {
                    OpWarpChargingView.this.preloadAnimationList();
                }
            });
        }
    }

    public void releaseAsset() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("releaseAsset() / mAssetLoaded:");
            sb.append(this.mAssetLoaded);
            sb.append(" / isAnimationStart:");
            sb.append(this.isAnimationStart);
            sb.append(" / mAssetReleasing:");
            sb.append(this.mAssetReleasing);
            Log.i("OpWarpChargingView", sb.toString());
        }
        if (this.mAssetLoaded && !this.isAnimationStart && !this.mAssetReleasing) {
            this.mAssetReleasing = true;
            View view = this.mInfoView;
            if (view != null) {
                view.setBackgroundResource(0);
            }
            this.mBackgroundHandler.post(new Runnable() {
                public void run() {
                    OpWarpChargingView.this.relaseAnimationList();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void preloadAnimationList() {
        String str = "OpWarpChargingView";
        if (DEBUG) {
            Log.i(str, "preloadAnimationList()");
        }
        long currentTimeMillis = System.currentTimeMillis();
        TypedArray obtainTypedArray = mRes.obtainTypedArray(R$array.fast_charging_start_animation1);
        for (int i = 0; i < this.mStartAnimationAssets1.size(); i++) {
            Bitmap bitmap = (Bitmap) this.mStartAnimationAssets1.get(i);
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        this.mStartAnimationAssets1.clear();
        for (int i2 = 0; i2 < obtainTypedArray.length(); i2++) {
            InputStream openRawResource = getResources().openRawResource(obtainTypedArray.getResourceId(i2, 0));
            this.mStartAnimationAssets1.add(BitmapFactory.decodeStream(openRawResource));
            if (openRawResource != null) {
                try {
                    openRawResource.close();
                } catch (IOException unused) {
                }
            }
        }
        obtainTypedArray.recycle();
        long currentTimeMillis2 = System.currentTimeMillis();
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("preloadAnimationList: cost Time");
            sb.append(currentTimeMillis2 - currentTimeMillis);
            sb.append(" mStartAnimationAssets1 size:");
            sb.append(this.mStartAnimationAssets1.size());
            Log.i(str, sb.toString());
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                OpWarpChargingView.this.mAssetLoading = false;
                OpWarpChargingView.this.mAssetLoaded = true;
                if (OpWarpChargingView.this.mIsPaddingStartAnimation) {
                    OpWarpChargingView.this.startAnimation();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void relaseAnimationList() {
        if (DEBUG) {
            Log.i("OpWarpChargingView", "relaseAnimationList()");
        }
        this.mBackgroundView.setImageBitmap(null);
        for (int i = 0; i < this.mStartAnimationAssets1.size(); i++) {
            Bitmap bitmap = (Bitmap) this.mStartAnimationAssets1.get(i);
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        this.mStartAnimationAssets1.clear();
        this.mHandler.post(new Runnable() {
            public void run() {
                OpWarpChargingView.this.mAssetLoaded = false;
                OpWarpChargingView.this.mAssetReleasing = false;
            }
        });
    }

    public void setBackgroundWallpaper(Bitmap bitmap) {
        this.mWallpaper = bitmap;
        ImageView imageView = this.mWallpaperView;
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void updaetScrimColor(int i) {
        View view = this.mScrim;
        if (view != null) {
            view.setBackgroundColor(i);
        }
    }

    public void updateColors(int i) {
        TextView textView = this.mBatteryLevel;
        if (textView != null) {
            textView.setTextColor(i);
        }
        ImageView imageView = this.mWrapview;
        if (imageView != null) {
            imageView.setColorFilter(i);
        }
    }
}
