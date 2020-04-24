package com.oneplus.util;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieComposition.Factory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.OnCompositionLoadedListener;
import com.android.systemui.R$dimen;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile.State;

public class OpLottieUtils {
    private static final String TAG = "OpLottieUtils";
    private ImageView mBg;
    private boolean mClickedForAnim;
    private Context mContext;
    /* access modifiers changed from: private */
    public QSIconView mIcon;
    private boolean mIsAnimating;
    /* access modifiers changed from: private */
    public LottieAnimationView mLottieAnimView;
    /* access modifiers changed from: private */
    public LottieDrawable mLottieDrawable;

    public OpLottieUtils(Context context, ImageView imageView, QSIconView qSIconView) {
        this.mContext = context;
        this.mBg = imageView;
        this.mIcon = qSIconView;
    }

    public boolean performClick() {
        if (this.mIsAnimating) {
            return true;
        }
        this.mClickedForAnim = true;
        return false;
    }

    public boolean applyLottieAnimIfNeeded(FrameLayout frameLayout, State state, boolean z) {
        if (this.mIsAnimating || !isNeedLottie(state)) {
            return false;
        }
        this.mIsAnimating = true;
        if (this.mLottieAnimView == null) {
            this.mLottieAnimView = new LottieAnimationView(this.mContext);
            this.mLottieAnimView.setScaleType(ScaleType.FIT_CENTER);
            this.mLottieDrawable = createLottieDrawable(state, z);
            this.mLottieAnimView.setImageDrawable(this.mLottieDrawable);
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_quick_tile_size);
            frameLayout.addView(this.mLottieAnimView, new LayoutParams(dimensionPixelSize, dimensionPixelSize));
        }
        Factory.fromAssetFileName(this.mContext, getLottieAnimFile(state), new OnCompositionLoadedListener() {
            public void onCompositionLoaded(LottieComposition lottieComposition) {
                OpLottieUtils.this.mLottieDrawable.setComposition(lottieComposition);
                OpLottieUtils.this.mLottieDrawable.playAnimation();
            }
        });
        return true;
    }

    public boolean isNeedLottie(State state) {
        if (state == null || TextUtils.isEmpty(state.lottiePrefix) || !this.mClickedForAnim || !isCurStateNeedLottie(state) || !isCurShapeNeedLottie(state)) {
            return false;
        }
        return true;
    }

    private LottieDrawable createLottieDrawable(final State state, final boolean z) {
        LottieDrawable lottieDrawable = new LottieDrawable();
        lottieDrawable.removeAllAnimatorListeners();
        lottieDrawable.addAnimatorUpdateListener(new AnimatorUpdateListener(lottieDrawable) {
            private final /* synthetic */ LottieDrawable f$1;

            {
                this.f$1 = r2;
            }

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                OpLottieUtils.this.lambda$createLottieDrawable$0$OpLottieUtils(this.f$1, valueAnimator);
            }
        });
        lottieDrawable.addAnimatorListener(new AnimatorListener() {
            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                OpLottieUtils.this.mLottieAnimView.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                hideLottieView();
                OpLottieUtils.this.setBgAnimator(state).start();
            }

            public void onAnimationCancel(Animator animator) {
                hideLottieView();
                OpLottieUtils.this.onFinish(state);
            }

            private void hideLottieView() {
                OpLottieUtils.this.mLottieAnimView.setVisibility(8);
                OpLottieUtils.this.mIcon.setIcon(state, z);
            }
        });
        return lottieDrawable;
    }

    public /* synthetic */ void lambda$createLottieDrawable$0$OpLottieUtils(LottieDrawable lottieDrawable, ValueAnimator valueAnimator) {
        ImageView imageView = this.mBg;
        if (imageView != null && !imageView.isShown() && lottieDrawable.isAnimating()) {
            lottieDrawable.cancelAnimation();
            lottieDrawable.setProgress(1.0f);
        }
    }

    /* access modifiers changed from: private */
    public ValueAnimator setBgAnimator(final State state) {
        ValueAnimator duration = ValueAnimator.ofArgb(new int[]{getCurrentBgColor(), QSTileImpl.getCircleColorForState(state.state)}).setDuration(0);
        duration.addUpdateListener(new AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                OpLottieUtils.this.lambda$setBgAnimator$1$OpLottieUtils(valueAnimator);
            }
        });
        duration.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                OpLottieUtils.this.onFinish(state);
            }

            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                OpLottieUtils.this.onFinish(state);
            }
        });
        return duration;
    }

    public /* synthetic */ void lambda$setBgAnimator$1$OpLottieUtils(ValueAnimator valueAnimator) {
        this.mBg.setImageTintList(ColorStateList.valueOf(((Integer) valueAnimator.getAnimatedValue()).intValue()));
    }

    private int getCurrentBgColor() {
        ImageView imageView = this.mBg;
        int i = 0;
        if (imageView == null) {
            Log.d(TAG, "getCurrentBgColor: mBg is null.");
            return 0;
        }
        ColorStateList imageTintList = imageView.getImageTintList();
        if (imageTintList != null) {
            i = imageTintList.getDefaultColor();
        }
        return i;
    }

    /* access modifiers changed from: private */
    public void onFinish(State state) {
        ImageView imageView = this.mBg;
        if (!(imageView == null || state == null)) {
            imageView.setImageTintList(ColorStateList.valueOf(QSTileImpl.getCircleColorForState(state.state)));
        }
        this.mClickedForAnim = false;
        this.mIsAnimating = false;
    }

    private String getLottieAnimFile(State state) {
        if (!isNeedLottie(state)) {
            return null;
        }
        boolean z = ThemeColorUtils.getCurrentTheme() == 2;
        StringBuilder sb = new StringBuilder(state.lottiePrefix);
        String str = "_";
        sb.append(str);
        sb.append(getCurrentShapeString(this.mContext));
        sb.append(str);
        int i = state.state;
        if (i == 0) {
            sb.append("unavailable");
        } else if (i != 2) {
            sb.append("inactive");
        } else {
            sb.append("active");
        }
        if (z) {
            sb.append(str);
            sb.append("android");
        }
        sb.append(".json");
        return sb.toString();
    }

    private boolean isCurStateNeedLottie(State state) {
        if (state == null) {
            return false;
        }
        int i = state.state;
        if (i != 0) {
            if (i != 2) {
                if ((state.lottieSupport & 32) != 0) {
                    return true;
                }
            } else if ((state.lottieSupport & 16) != 0) {
                return true;
            }
        } else if ((state.lottieSupport & 64) != 0) {
            return true;
        }
        return false;
    }

    private boolean isCurShapeNeedLottie(State state) {
        if (state == null) {
            return false;
        }
        int currentShape = getCurrentShape(this.mContext);
        if (currentShape != 2) {
            if (currentShape != 3) {
                if (currentShape != 4) {
                    if ((state.lottieSupport & 1) != 0) {
                        return true;
                    }
                } else if ((state.lottieSupport & 8) != 0) {
                    return true;
                }
            } else if ((state.lottieSupport & 4) != 0) {
                return true;
            }
        } else if ((state.lottieSupport & 2) != 0) {
            return true;
        }
        return false;
    }

    private static int getCurrentShape(Context context) {
        return System.getInt(context.getContentResolver(), "oneplus_shape", 1);
    }

    private static String getCurrentShapeString(Context context) {
        int currentShape = getCurrentShape(context);
        if (currentShape == 2) {
            return "roundedrect";
        }
        if (currentShape != 3) {
            return currentShape != 4 ? "circle" : "squircle";
        }
        return "teardrop";
    }
}
