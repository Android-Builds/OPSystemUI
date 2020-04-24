package com.oneplus.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import com.android.systemui.R$array;
import java.util.ArrayList;

public class OpGraphLight {
    private int m2kOr1080p;
    private int mAnimateImageHeight = 70;
    private AnimateImageView mAnimateImageView;
    /* access modifiers changed from: private */
    public int mAnimateImageWidth = 507;
    private final Context mContext;
    private int mFrontCameraPosistion;
    private final Handler mHandler;
    private Runnable mShowRunnable = new Runnable() {
        public void run() {
            OpGraphLight.this.show();
        }
    };
    private boolean mViewAdded;
    private LinearLayout mViewContainer;
    /* access modifiers changed from: private */
    public final WindowManager mWm;

    private class AnimateImageView extends ImageView {
        private final ValueAnimator mAlphaInAnimator;
        private final ValueAnimator mAlphaInAnimatorDisappear;
        TypedArray mAnimationArray;
        private final AnimatorSet mAnimator;
        LayoutParams mLp;
        private int mOrientationType = getOrientation();
        ArrayList mStartAnimationAssets1 = new ArrayList();

        public AnimateImageView(Context context) {
            super(context);
            setScaleType(ScaleType.FIT_XY);
            this.mAnimationArray = this.mContext.getResources().obtainTypedArray(R$array.op_light_start_animation);
            this.mAlphaInAnimator = ValueAnimator.ofInt(new int[]{0, 225}).setDuration(225);
            this.mAlphaInAnimator.addUpdateListener(new AnimatorUpdateListener(OpGraphLight.this) {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    AnimateImageView.this.checkOrientationType();
                    if (intValue <= 225) {
                        AnimateImageView animateImageView = AnimateImageView.this;
                        animateImageView.mLp = animateImageView.getLayoutParams();
                        AnimateImageView animateImageView2 = AnimateImageView.this;
                        animateImageView2.mLp.width = (int) ((((float) OpGraphLight.this.mAnimateImageWidth) * 0.5f) + (((float) OpGraphLight.this.mAnimateImageWidth) * 0.5f * (((float) intValue) / 225.0f)));
                        AnimateImageView animateImageView3 = AnimateImageView.this;
                        animateImageView3.setLayoutParams(animateImageView3.mLp);
                    }
                    if (intValue <= 150) {
                        AnimateImageView.this.setImageAlpha((int) ((((float) intValue) * 255.0f) / 150.0f));
                    }
                }
            });
            this.mAlphaInAnimator.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f));
            this.mAlphaInAnimatorDisappear = ValueAnimator.ofInt(new int[]{0, 150}).setDuration(150);
            this.mAlphaInAnimatorDisappear.addUpdateListener(new AnimatorUpdateListener(OpGraphLight.this) {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    if (intValue <= 150) {
                        AnimateImageView animateImageView = AnimateImageView.this;
                        animateImageView.mLp = animateImageView.getLayoutParams();
                        AnimateImageView animateImageView2 = AnimateImageView.this;
                        animateImageView2.mLp.width = (int) (((float) OpGraphLight.this.mAnimateImageWidth) * (1.0f - (((float) intValue) / 150.0f)));
                        AnimateImageView animateImageView3 = AnimateImageView.this;
                        animateImageView3.setLayoutParams(animateImageView3.mLp);
                    }
                    if (intValue >= 50) {
                        AnimateImageView.this.setImageAlpha((int) ((1.0f - (((float) (intValue - 50)) / 100.0f)) * 255.0f));
                    }
                }
            });
            this.mAlphaInAnimatorDisappear.setInterpolator(new PathInterpolator(0.8f, 0.0f, 1.0f, 1.0f));
            this.mAlphaInAnimatorDisappear.setStartDelay(225);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.play(this.mAlphaInAnimator).before(this.mAlphaInAnimatorDisappear);
            this.mAnimator.addListener(new AnimatorListenerAdapter(OpGraphLight.this) {
                boolean mCancelled;

                public void onAnimationStart(Animator animator) {
                    this.mCancelled = false;
                }

                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        Log.i("OpGraphLight", "onAnimationEnd & !mCancelled");
                        AnimateImageView.this.relaseAnimationList();
                        OpGraphLight.this.hide();
                    }
                }
            });
        }

        private int getOrientation() {
            Display defaultDisplay = OpGraphLight.this.mWm.getDefaultDisplay();
            if (defaultDisplay == null) {
                return 0;
            }
            return defaultDisplay.getRotation();
        }

        /* access modifiers changed from: private */
        public int checkOrientationType() {
            int orientation = getOrientation();
            StringBuilder sb = new StringBuilder();
            sb.append("checkOrientationType / rotation:");
            sb.append(orientation);
            String str = "OpGraphLight";
            Log.v(str, sb.toString());
            if (this.mOrientationType != orientation) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("detect checkOrientationType() / rotation:");
                sb2.append(orientation);
                sb2.append(" / mOrientationType:");
                sb2.append(this.mOrientationType);
                Log.v(str, sb2.toString());
                this.mOrientationType = orientation;
                this.mAnimator.cancel();
                relaseAnimationList();
                OpGraphLight.this.hide();
                OpGraphLight.this.postShow();
            }
            return this.mOrientationType;
        }

        /* access modifiers changed from: protected */
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            startAnimation();
        }

        private void startAnimation() {
            this.mAnimator.cancel();
            setAnimationList();
            Log.i("OpGraphLight", "startAnimation");
            this.mLp = getLayoutParams();
            this.mLp.width = (int) (((float) OpGraphLight.this.mAnimateImageWidth) * 0.5f);
            setLayoutParams(this.mLp);
            setImageResource(((Integer) this.mStartAnimationAssets1.get(0)).intValue());
            this.mAnimator.start();
        }

        private void setAnimationList() {
            Log.i("OpGraphLight", "setAnimationList (clear & add)");
            this.mStartAnimationAssets1.clear();
            for (int i = 0; i < this.mAnimationArray.length(); i++) {
                this.mStartAnimationAssets1.add(Integer.valueOf(this.mAnimationArray.getResourceId(i, 0)));
            }
        }

        /* access modifiers changed from: private */
        public void relaseAnimationList() {
            Log.i("OpGraphLight", "relaseAnimationList");
            setImageDrawable(null);
            this.mAnimationArray.recycle();
            this.mStartAnimationAssets1.clear();
        }
    }

    public OpGraphLight(WindowManager windowManager, Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWm = windowManager;
    }

    public void postShow() {
        this.mHandler.removeCallbacks(this.mShowRunnable);
        this.mHandler.postDelayed(this.mShowRunnable, 50);
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0185  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void show() {
        /*
            r17 = this;
            r0 = r17
            com.oneplus.anim.OpGraphLight$AnimateImageView r1 = r0.mAnimateImageView
            if (r1 != 0) goto L_0x000f
            com.oneplus.anim.OpGraphLight$AnimateImageView r1 = new com.oneplus.anim.OpGraphLight$AnimateImageView
            android.content.Context r2 = r0.mContext
            r1.<init>(r2)
            r0.mAnimateImageView = r1
        L_0x000f:
            android.content.Context r1 = r0.mContext
            android.content.res.Resources r1 = r1.getResources()
            android.util.DisplayMetrics r2 = new android.util.DisplayMetrics
            r2.<init>()
            android.view.WindowManager r3 = r0.mWm
            android.view.Display r3 = r3.getDefaultDisplay()
            r3.getRealMetrics(r2)
            int r3 = r2.widthPixels
            int r4 = r2.heightPixels
            int r3 = java.lang.Math.max(r3, r4)
            int r4 = r2.widthPixels
            int r5 = r2.heightPixels
            int r4 = java.lang.Math.min(r4, r5)
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "screenHeight:"
            r5.append(r6)
            r5.append(r3)
            java.lang.String r3 = " / screenWidth:"
            r5.append(r3)
            r5.append(r4)
            java.lang.String r3 = r5.toString()
            java.lang.String r5 = "OpGraphLight"
            android.util.Log.i(r5, r3)
            r3 = 1080(0x438, float:1.513E-42)
            r6 = 0
            r7 = 1
            if (r4 != r3) goto L_0x005a
            r0.m2kOr1080p = r7
            goto L_0x005c
        L_0x005a:
            r0.m2kOr1080p = r6
        L_0x005c:
            int r3 = com.android.systemui.R$drawable.op_front_camera_animation_graph
            java.io.InputStream r3 = r1.openRawResource(r3)
            android.graphics.Bitmap r4 = android.graphics.BitmapFactory.decodeStream(r3)
            int r8 = r4.getWidth()
            r0.mAnimateImageWidth = r8
            int r8 = r4.getHeight()
            r0.mAnimateImageHeight = r8
            if (r3 == 0) goto L_0x0077
            r3.close()     // Catch:{ IOException -> 0x0077 }
        L_0x0077:
            if (r4 == 0) goto L_0x007c
            r4.recycle()
        L_0x007c:
            int r3 = r0.m2kOr1080p
            if (r3 != r7) goto L_0x0095
            int r3 = r0.mAnimateImageWidth
            double r3 = (double) r3
            r8 = 4604930618986332160(0x3fe8000000000000, double:0.75)
            double r3 = r3 * r8
            int r3 = (int) r3
            r0.mAnimateImageWidth = r3
            int r3 = r0.mAnimateImageHeight
            double r3 = (double) r3
            r8 = 4604918909627300997(0x3fe7f559b3d07c85, double:0.7487)
            double r3 = r3 * r8
            int r3 = (int) r3
            r0.mAnimateImageHeight = r3
        L_0x0095:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "mAnimateImageWidth:"
            r3.append(r4)
            int r4 = r0.mAnimateImageWidth
            r3.append(r4)
            java.lang.String r4 = " / mAnimateImageHeight:"
            r3.append(r4)
            int r4 = r0.mAnimateImageHeight
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.i(r5, r3)
            int r3 = com.android.systemui.R$dimen.op_front_camera_animation_front_camera_posistion
            int r1 = r1.getDimensionPixelSize(r3)
            r0.mFrontCameraPosistion = r1
            android.view.WindowManager r1 = r0.mWm
            android.view.Display r1 = r1.getDefaultDisplay()
            if (r1 != 0) goto L_0x00c6
            return
        L_0x00c6:
            android.widget.LinearLayout r3 = r0.mViewContainer
            if (r3 != 0) goto L_0x00d3
            android.widget.LinearLayout r3 = new android.widget.LinearLayout
            android.content.Context r4 = r0.mContext
            r3.<init>(r4)
            r0.mViewContainer = r3
        L_0x00d3:
            int r1 = r1.getRotation()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "in first show() / rotation:"
            r3.append(r4)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            android.util.Log.v(r5, r3)
            int r3 = r0.mAnimateImageWidth
            int r4 = r0.mAnimateImageHeight
            int r3 = java.lang.Math.max(r3, r4)
            int r4 = r0.mAnimateImageWidth
            int r5 = r0.mAnimateImageHeight
            int r4 = java.lang.Math.max(r4, r5)
            int r5 = r0.mFrontCameraPosistion
            r8 = 0
            if (r1 == 0) goto L_0x016b
            if (r1 == r7) goto L_0x014d
            r9 = 2
            if (r1 == r9) goto L_0x0127
            r9 = 3
            if (r1 == r9) goto L_0x010a
            goto L_0x017f
        L_0x010a:
            int r1 = r2.widthPixels
            int r6 = r1 - r3
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setPivotX(r8)
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setPivotY(r8)
            android.widget.LinearLayout r1 = r0.mViewContainer
            r2 = 1119092736(0x42b40000, float:90.0)
            r1.setRotation(r2)
            android.widget.LinearLayout r1 = r0.mViewContainer
            float r2 = (float) r3
            r1.setTranslationX(r2)
            r13 = r5
            goto L_0x0169
        L_0x0127:
            int r1 = r2.widthPixels
            int r1 = r1 - r5
            int r6 = r1 - r3
            int r1 = r2.heightPixels
            int r1 = r1 - r4
            android.widget.LinearLayout r2 = r0.mViewContainer
            r2.setPivotX(r8)
            android.widget.LinearLayout r2 = r0.mViewContainer
            r2.setPivotY(r8)
            android.widget.LinearLayout r2 = r0.mViewContainer
            r5 = 1127481344(0x43340000, float:180.0)
            r2.setRotation(r5)
            android.widget.LinearLayout r2 = r0.mViewContainer
            float r5 = (float) r3
            r2.setTranslationX(r5)
            android.widget.LinearLayout r2 = r0.mViewContainer
            float r5 = (float) r4
            r2.setTranslationY(r5)
            goto L_0x0168
        L_0x014d:
            int r1 = r2.heightPixels
            int r1 = r1 - r5
            int r1 = r1 - r4
            android.widget.LinearLayout r2 = r0.mViewContainer
            r2.setPivotX(r8)
            android.widget.LinearLayout r2 = r0.mViewContainer
            r2.setPivotY(r8)
            android.widget.LinearLayout r2 = r0.mViewContainer
            r5 = -1028390912(0xffffffffc2b40000, float:-90.0)
            r2.setRotation(r5)
            android.widget.LinearLayout r2 = r0.mViewContainer
            float r5 = (float) r4
            r2.setTranslationY(r5)
        L_0x0168:
            r13 = r1
        L_0x0169:
            r12 = r6
            goto L_0x0181
        L_0x016b:
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setPivotX(r8)
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setPivotY(r8)
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setRotation(r8)
            android.widget.LinearLayout r1 = r0.mViewContainer
            r1.setTranslationY(r8)
        L_0x017f:
            r12 = r5
            r13 = r6
        L_0x0181:
            boolean r1 = r0.mViewAdded
            if (r1 != 0) goto L_0x01cc
            android.view.WindowManager$LayoutParams r1 = new android.view.WindowManager$LayoutParams
            int r10 = java.lang.Math.max(r3, r4)
            int r11 = java.lang.Math.max(r3, r4)
            r14 = 2014(0x7de, float:2.822E-42)
            r15 = 1304(0x518, float:1.827E-42)
            r16 = -3
            r9 = r1
            r9.<init>(r10, r11, r12, r13, r14, r15, r16)
            r2 = -3
            r1.format = r2
            java.lang.String r2 = "GraphLight"
            r1.setTitle(r2)
            r2 = 51
            r1.gravity = r2
            r1.layoutInDisplayCutoutMode = r7
            android.view.WindowManager r2 = r0.mWm
            android.widget.LinearLayout r3 = r0.mViewContainer
            r2.addView(r3, r1)
            android.widget.LinearLayout$LayoutParams r1 = new android.widget.LinearLayout$LayoutParams
            int r2 = r0.mAnimateImageWidth
            int r3 = r0.mAnimateImageHeight
            r1.<init>(r2, r3)
            r1.gravity = r7
            com.oneplus.anim.OpGraphLight$AnimateImageView r2 = r0.mAnimateImageView
            r2.setLayoutParams(r1)
            android.widget.LinearLayout r2 = r0.mViewContainer
            r2.setOrientation(r7)
            android.widget.LinearLayout r2 = r0.mViewContainer
            com.oneplus.anim.OpGraphLight$AnimateImageView r3 = r0.mAnimateImageView
            r2.addView(r3, r1)
            r0.mViewAdded = r7
        L_0x01cc:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.anim.OpGraphLight.show():void");
    }

    /* access modifiers changed from: private */
    public void hide() {
        if (this.mViewAdded) {
            AnimateImageView animateImageView = this.mAnimateImageView;
            if (!(animateImageView == null || animateImageView.getParent() == null)) {
                this.mViewContainer.removeView(this.mAnimateImageView);
            }
            this.mAnimateImageView = null;
            if (this.mViewContainer.getParent() != null) {
                this.mWm.removeView(this.mViewContainer);
            }
            this.mViewAdded = false;
        }
    }
}
