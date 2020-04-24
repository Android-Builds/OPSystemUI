package com.oneplus.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;

public class OpDragPanelController {
    /* access modifiers changed from: private */
    public static int mDropThreshold;
    private boolean animationRunning = false;
    /* access modifiers changed from: private */
    public boolean isStart = false;
    /* access modifiers changed from: private */
    public View mArrowView;
    /* access modifiers changed from: private */
    public Context mContext;
    private AnimatorSet mDragEndAnimation;
    /* access modifiers changed from: private */
    public AnimatorSet mDragStartAnimation;
    private boolean mDropped = false;
    /* access modifiers changed from: private */
    public Handler mHander = new Handler() {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 0) {
                OpDragPanelController.this.createHintAnimation();
                OpDragPanelController.this.mHintAnimation.start();
            } else if (i == 1) {
                OpDragPanelController.this.createShakeAnimator();
                OpDragPanelController.this.mShakeAnimator.start();
            } else if (i == 3) {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" TIME_OUT:");
                    sb.append(OpDragPanelController.this.mPanel);
                    sb.append(" isStart:");
                    sb.append(OpDragPanelController.this.isStart);
                    Log.i("OpDragPanelController", sb.toString());
                }
                if (OpDragPanelController.this.mPanel != null && OpDragPanelController.this.isStart) {
                    OpDragPanelController.this.mPanel.onTimeout();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public AnimatorSet mHintAnimation;
    private TextView mHintText;
    private LinearLayout mIndicatorLayout;
    /* access modifiers changed from: private */
    public boolean mIsReseting = false;
    /* access modifiers changed from: private */
    public float mLastX = 0.0f;
    /* access modifiers changed from: private */
    public float mLastY = 0.0f;
    /* access modifiers changed from: private */
    public OpEmergencyPanel mPanel;
    /* access modifiers changed from: private */
    public AnimatorSet mShakeAnimator;
    /* access modifiers changed from: private */
    public OpEmergencyBubble mTouchView;
    /* access modifiers changed from: private */
    public boolean mTouched = false;
    /* access modifiers changed from: private */
    public VelocityTracker mVelocityTracker;
    private boolean shakeAnimationRunning = false;

    private class OpOnTouchListener implements OnTouchListener {
        private OpOnTouchListener() {
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getPointerCount() == 1) {
                int action = motionEvent.getAction();
                float rawY = motionEvent.getRawY();
                float rawX = motionEvent.getRawX();
                OpDragPanelController.this.mHander.removeMessages(3);
                OpDragPanelController.this.mHander.sendEmptyMessageDelayed(3, 5000);
                if (OpDragPanelController.this.isStart && !OpDragPanelController.this.mIsReseting) {
                    if (action == 0) {
                        OpDragPanelController.this.mLastY = rawY;
                        OpDragPanelController.this.mLastX = rawX;
                        OpDragPanelController.this.stopShakeAnimation();
                        OpDragPanelController.this.stopHintAnimation();
                        if (OpDragPanelController.this.mDragStartAnimation.isRunning()) {
                            OpDragPanelController.this.mDragStartAnimation.cancel();
                        }
                        OpDragPanelController.this.mDragStartAnimation.start();
                        OpDragPanelController.this.mTouched = true;
                        OpDragPanelController opDragPanelController = OpDragPanelController.this;
                        opDragPanelController.mVelocityTracker = new VelocityTracker(opDragPanelController.mLastX, OpDragPanelController.this.mLastY);
                        if (OpDragPanelController.this.mPanel != null) {
                            OpDragPanelController.this.mPanel.onBubbleTouched();
                        }
                        return true;
                    } else if (action == 2) {
                        float access$800 = rawY - OpDragPanelController.this.mLastY;
                        if (access$800 < 0.0f) {
                            access$800 = 0.0f;
                        } else if (access$800 > ((float) OpDragPanelController.mDropThreshold)) {
                            access$800 = (float) OpDragPanelController.mDropThreshold;
                        }
                        if (OpDragPanelController.this.mTouched) {
                            OpDragPanelController.this.mTouchView.setTranslationY(access$800);
                            OpDragPanelController.this.mVelocityTracker.updateMovePoint(rawX, rawY);
                            OpDragPanelController.this.onDragStart(access$800);
                            return true;
                        }
                    } else if (action == 1 || action == 3) {
                        OpDragPanelController.this.mVelocityTracker.updateMovePoint(rawX, rawY);
                        if (OpDragPanelController.this.mTouched) {
                            OpDragPanelController.this.mTouched = false;
                            OpDragPanelController.this.onDragEnd();
                            return true;
                        } else if (Build.DEBUG_ONEPLUS) {
                            Log.i("OpDragPanelController", " doesn't touch before, skip drag");
                        }
                    }
                }
            }
            return false;
        }
    }

    class VelocityTracker {
        long endTime = 0;
        Point mEndPoint;
        Point mStartPoint;
        long startTime = 0;

        public VelocityTracker(float f, float f2) {
            this.mStartPoint = new Point((int) f, (int) f2);
            this.startTime = System.currentTimeMillis();
        }

        public void updateMovePoint(float f, float f2) {
            this.mEndPoint = new Point((int) f, (int) f2);
            this.endTime = System.currentTimeMillis();
        }

        public boolean isDrop() {
            return Math.abs(this.mEndPoint.y - this.mStartPoint.y) >= OpDragPanelController.mDropThreshold;
        }
    }

    public OpDragPanelController(Context context, OpEmergencyPanel opEmergencyPanel, OpEmergencyBubble opEmergencyBubble, LinearLayout linearLayout) {
        this.mContext = context;
        this.mPanel = opEmergencyPanel;
        this.mTouchView = opEmergencyBubble;
        this.mTouchView.setOnTouchListener(new OpOnTouchListener());
        this.mIndicatorLayout = linearLayout;
        this.mArrowView = this.mIndicatorLayout.findViewById(R$id.arrow_panel);
        this.mHintText = (TextView) this.mIndicatorLayout.findViewById(R$id.hint);
        mDropThreshold = this.mContext.getResources().getDimensionPixelOffset(R$dimen.op_emergency_bubble_drop_distance);
        this.mDragStartAnimation = getZoomInAnimatorSet(116);
        this.mDragEndAnimation = getZoomOutAnimatorSet(75);
    }

    public void onStart() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("OpDragPanelController", "onStart");
        }
        this.isStart = true;
        reset();
        restartAnimation();
        this.mHander.sendEmptyMessageDelayed(3, 5000);
    }

    public void onStop() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("OpDragPanelController", "onStop");
        }
        this.isStart = false;
        reset();
        this.mHander.removeMessages(3);
    }

    private void reset() {
        this.mTouched = false;
        this.mDropped = false;
        this.mIsReseting = false;
        stopHintAnimation();
        stopShakeAnimation();
        resetIndicatorLayout();
        resetArrow(false);
        resetTouchView();
        resetText();
        if (Build.DEBUG_ONEPLUS) {
            Log.i("OpDragPanelController", "reset");
        }
    }

    /* access modifiers changed from: private */
    public void onDragStart(float f) {
        if (this.isStart) {
            float f2 = f / ((float) mDropThreshold);
            if (Build.DEBUG_ONEPLUS) {
                Log.i("OpDragPanelController", "onDragStart:");
            }
            int i = OpEmergencyBubble.ACTIVE_CIRCLE_COLOR;
            int abs = (int) (Math.abs(f2) * 255.0f);
            int i2 = -1376216;
            int abs2 = (int) ((0.4d - (((double) Math.abs(f2)) * 0.3d)) * 255.0d);
            if (((double) Math.abs(f2)) >= 0.5d) {
                i2 = OpEmergencyBubble.ACTIVE_TEXT_COLOR;
                abs2 = (int) (((((double) Math.abs(f2)) * 0.8d) + 0.2d) * 255.0d);
            }
            this.mTouchView.drawView(i, abs, 0, i2, abs2);
            this.mIndicatorLayout.setAlpha(1.0f - f2);
        }
    }

    /* access modifiers changed from: private */
    public void onDragEnd() {
        if (this.isStart) {
            String str = "OpDragPanelController";
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("onDragEnd: ");
                sb.append(this.mDragStartAnimation.isRunning());
                sb.append(", ");
                sb.append(this.mDropped);
                Log.i(str, sb.toString());
            }
            this.mDropped = this.mVelocityTracker.isDrop();
            if (this.mDragStartAnimation.isRunning()) {
                this.mDragStartAnimation.cancel();
            }
            if (!this.mDropped) {
                this.mDragEndAnimation.start();
                restartAnimation();
                return;
            }
            getAlphaAnimatorSet().start();
            OpEmergencyPanel opEmergencyPanel = this.mPanel;
            if (opEmergencyPanel != null) {
                opEmergencyPanel.onDrop();
            }
            if (Build.DEBUG_ONEPLUS) {
                Log.i(str, "onDrop");
            }
        }
    }

    private void resetTouchView() {
        OpEmergencyBubble opEmergencyBubble = this.mTouchView;
        if (opEmergencyBubble != null) {
            opEmergencyBubble.setTranslationX(0.0f);
            this.mTouchView.setTranslationY(0.0f);
            this.mTouchView.setScaleX(1.0f);
            this.mTouchView.setScaleY(1.0f);
            this.mTouchView.setAlpha(1.0f);
            this.mTouchView.reset();
        }
    }

    private void resetArrow(boolean z) {
        if (Build.DEBUG_ONEPLUS) {
            Log.i("OpDragPanelController", "resetArrow");
        }
        View view = this.mArrowView;
        if (view != null) {
            view.setAlpha(z ? 1.0f : 0.0f);
            this.mArrowView.setTranslationX(0.0f);
            this.mArrowView.setTranslationY(0.0f);
        }
    }

    private void resetIndicatorLayout() {
        if (Build.DEBUG_ONEPLUS) {
            Log.i("OpDragPanelController", "IndicatorLayout");
        }
        this.mIndicatorLayout.setAlpha(1.0f);
    }

    private void restartAnimation() {
        if (Build.DEBUG_ONEPLUS) {
            Log.i("OpDragPanelController", "restartAnimation");
        }
        resetTouchView();
        resetIndicatorLayout();
        resetArrow(false);
        startShakeAnimation();
        startHintAnimation();
    }

    private AnimatorSet getZoomInAnimatorSet(long j) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mTouchView, "scaleX", new float[]{1.0f, 1.15f});
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mTouchView, "scaleY", new float[]{1.0f, 1.15f});
        Interpolator loadInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(ofFloat).with(ofFloat2);
        animatorSet.setDuration(j);
        animatorSet.setInterpolator(loadInterpolator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if (OpDragPanelController.this.mTouchView != null) {
                    OpDragPanelController.this.mTouchView.setScaleX(1.15f);
                    OpDragPanelController.this.mTouchView.setScaleY(1.15f);
                }
            }
        });
        return animatorSet;
    }

    private AnimatorSet getZoomOutAnimatorSet(long j) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mTouchView, "scaleX", new float[]{1.15f, 1.0f});
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mTouchView, "scaleY", new float[]{1.15f, 1.0f});
        Interpolator loadInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(ofFloat).with(ofFloat2);
        animatorSet.setDuration(j);
        animatorSet.setInterpolator(loadInterpolator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if (OpDragPanelController.this.mTouchView != null) {
                    OpDragPanelController.this.mTouchView.setScaleX(1.0f);
                    OpDragPanelController.this.mTouchView.setScaleY(1.0f);
                }
            }
        });
        return animatorSet;
    }

    private AnimatorSet getAlphaAnimatorSet() {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mTouchView, View.ALPHA, new float[]{1.0f, 0.0f});
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mTouchView, "scaleX", new float[]{1.15f, 1.0f});
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(this.mTouchView, "scaleY", new float[]{1.15f, 1.0f});
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{ofFloat, ofFloat2, ofFloat3});
        animatorSet.setDuration(150);
        return animatorSet;
    }

    /* access modifiers changed from: private */
    public void createHintAnimation() {
        if (this.mHintAnimation == null) {
            this.mHintAnimation = getHintAnimation();
            this.mHintAnimation.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator, boolean z) {
                    OpDragPanelController.this.mContext.getResources().getDimension(R$dimen.op_emergency_panel_indicator_arrow_animation_transY);
                    if (OpDragPanelController.this.mArrowView == null) {
                        OpDragPanelController.this.mArrowView.setTranslationY(0.0f);
                    }
                    OpDragPanelController.this.mHander.sendEmptyMessageDelayed(0, 600);
                }
            });
        }
    }

    private AnimatorSet getHintAnimation() {
        float dimension = this.mContext.getResources().getDimension(R$dimen.op_emergency_panel_indicator_arrow_animation_transY);
        Interpolator loadInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mArrowView, View.ALPHA, new float[]{0.0f, 1.0f});
        ofFloat.setDuration(75);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mArrowView, View.ALPHA, new float[]{1.0f, 0.0f});
        ofFloat2.setDuration(183);
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(this.mArrowView, View.TRANSLATION_Y, new float[]{-dimension, dimension});
        ofFloat3.setDuration(1183);
        ofFloat3.setInterpolator(loadInterpolator);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(new Animator[]{ofFloat, ofFloat3, ofFloat2});
        return animatorSet;
    }

    /* access modifiers changed from: private */
    public void createShakeAnimator() {
        if (this.mShakeAnimator == null) {
            AnimatorSet zoomInAnimatorSet = getZoomInAnimatorSet(1000);
            AnimatorSet zoomOutAnimatorSet = getZoomOutAnimatorSet(1000);
            this.mShakeAnimator = new AnimatorSet();
            this.mShakeAnimator.playSequentially(new Animator[]{zoomInAnimatorSet, zoomOutAnimatorSet});
            this.mShakeAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.i("OpDragPanelController", "ShakeAnimation end");
                    }
                    OpDragPanelController.this.mHander.sendEmptyMessageDelayed(1, 100);
                }
            });
        }
    }

    private void startShakeAnimation() {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("startShakeAnimation, ");
            sb.append(isShakeAnimationRunning());
            Log.i("OpDragPanelController", sb.toString());
        }
        if (!isShakeAnimationRunning()) {
            this.mHander.removeMessages(1);
            this.mHander.sendEmptyMessageDelayed(1, 100);
            this.shakeAnimationRunning = true;
        }
    }

    private void startHintAnimation() {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("startAnimation animationRunning: ");
            sb.append(isAnimationRunning());
            Log.i("OpDragPanelController", sb.toString());
        }
        if (!isAnimationRunning()) {
            this.mHander.removeMessages(0);
            this.mHander.sendEmptyMessageDelayed(0, 100);
            this.animationRunning = true;
        }
    }

    /* access modifiers changed from: private */
    public void stopShakeAnimation() {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("stopShakeAnimation, ");
            sb.append(isShakeAnimationRunning());
            Log.i("OpDragPanelController", sb.toString());
        }
        if (isShakeAnimationRunning()) {
            AnimatorSet animatorSet = this.mShakeAnimator;
            if (animatorSet != null) {
                animatorSet.end();
            }
            this.mHander.removeMessages(1);
            this.shakeAnimationRunning = false;
            resetTouchView();
        }
    }

    /* access modifiers changed from: private */
    public void stopHintAnimation() {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("stopAnimation animationRunning: ");
            sb.append(isAnimationRunning());
            Log.i("OpDragPanelController", sb.toString());
        }
        if (isAnimationRunning()) {
            AnimatorSet animatorSet = this.mHintAnimation;
            if (animatorSet != null) {
                animatorSet.end();
            }
            this.mHander.removeMessages(0);
            this.animationRunning = false;
            resetIndicatorLayout();
            resetArrow(true);
        }
    }

    private boolean isShakeAnimationRunning() {
        return this.shakeAnimationRunning;
    }

    private boolean isAnimationRunning() {
        return this.animationRunning;
    }

    private void resetText() {
        TextView textView = this.mHintText;
        if (textView != null) {
            textView.setAlpha(1.0f);
        }
    }
}
