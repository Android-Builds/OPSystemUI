package com.oneplus.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R$drawable;

public class OpSideGestureNavView extends View {
    public static final Interpolator BEZIER_ANIMATION_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    public static final Interpolator ICON_ENTER_ANIMATION_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    public static final Interpolator ICON_EXIT_ANIMATION_INTERPOLATOR = new PathInterpolator(0.35f, 0.41f, 0.24f, 1.11f);
    private Bitmap mAppIcon;
    private AnimatorListenerAdapter mAppIconAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            super.onAnimationCancel(animator);
            OpSideGestureNavView.this.mAppIconProcessing = false;
        }

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            OpSideGestureNavView.this.mAppIconProcessing = false;
        }

        public void onAnimationStart(Animator animator) {
            super.onAnimationStart(animator);
            OpSideGestureNavView.this.mAppIconProcessing = true;
        }
    };
    private ObjectAnimator mAppIconAnimator;
    private Matrix mAppIconMatrix = new Matrix();
    /* access modifiers changed from: private */
    public boolean mAppIconProcessing = false;
    private FloatProperty<OpSideGestureNavView> mAppIconProperty = new FloatProperty<OpSideGestureNavView>("AppIconAnimation") {
        public Float get(OpSideGestureNavView opSideGestureNavView) {
            return null;
        }

        public void setValue(OpSideGestureNavView opSideGestureNavView, float f) {
            OpSideGestureNavView.this.mAppIconScale = f;
        }
    };
    /* access modifiers changed from: private */
    public float mAppIconScale = 0.0f;
    private Bitmap mBackIcon;
    private AnimatorListenerAdapter mBackIconAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            super.onAnimationCancel(animator);
            OpSideGestureNavView.this.mBackIconProcessing = false;
        }

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            OpSideGestureNavView.this.mBackIconProcessing = false;
        }

        public void onAnimationStart(Animator animator) {
            super.onAnimationStart(animator);
            OpSideGestureNavView.this.mBackIconProcessing = true;
        }
    };
    private ObjectAnimator mBackIconAnimator;
    private Matrix mBackIconMatrix = new Matrix();
    /* access modifiers changed from: private */
    public boolean mBackIconProcessing = false;
    private FloatProperty<OpSideGestureNavView> mBackIconProperty = new FloatProperty<OpSideGestureNavView>("BackIconAnimation") {
        public Float get(OpSideGestureNavView opSideGestureNavView) {
            return null;
        }

        public void setValue(OpSideGestureNavView opSideGestureNavView, float f) {
            OpSideGestureNavView.this.mBackIconScale = f;
        }
    };
    /* access modifiers changed from: private */
    public float mBackIconScale = 1.0f;
    private int mBezierControlOffset1 = OpSideGestureConfiguration.getBezierControlOffset1();
    private int mBezierControlOffset2 = OpSideGestureConfiguration.getBezierControlOffsetSwitch2();
    private PointF mBezierControlPoint1 = new PointF();
    private PointF mBezierControlPoint2 = new PointF();
    private PointF mBezierControlPoint3 = new PointF();
    private PointF mBezierControlPoint4 = new PointF();
    private Paint mBezierPaint;
    /* access modifiers changed from: private */
    public PointF mCurrentPoint = new PointF();
    private PointF mEndPoint = new PointF();
    private AnimatorListenerAdapter mEnterAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            super.onAnimationCancel(animator);
            OpSideGestureNavView.this.mEnterAnimProcessing = false;
        }

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            OpSideGestureNavView.this.mEnterAnimProcessing = false;
        }

        public void onAnimationStart(Animator animator) {
            super.onAnimationStart(animator);
            if (OpSideGestureNavView.this.mExitAnimProcessing) {
                OpSideGestureNavView.this.mExitAnimator.cancel();
            }
            OpSideGestureNavView.this.mEnterAnimProcessing = true;
        }
    };
    /* access modifiers changed from: private */
    public boolean mEnterAnimProcessing = false;
    /* access modifiers changed from: private */
    public ObjectAnimator mEnterAnimator;
    private IntProperty<OpSideGestureNavView> mEnterProperty = new IntProperty<OpSideGestureNavView>("EnterAnimation") {
        public Integer get(OpSideGestureNavView opSideGestureNavView) {
            return null;
        }

        public void setValue(OpSideGestureNavView opSideGestureNavView, int i) {
            OpSideGestureNavView.this.mCurrentPoint.x = (float) i;
            OpSideGestureNavView.this.mCurrentPoint.y = OpSideGestureNavView.this.mTargetPoint.y;
            OpSideGestureNavView.this.updateView();
        }
    };
    private AnimatorListenerAdapter mExitAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            super.onAnimationCancel(animator);
            OpSideGestureNavView.this.mExitAnimProcessing = false;
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("opSideGestureNavView : mExitAnimListener onAnimationCancel mFinished ");
                sb.append(OpSideGestureNavView.this.mFinished);
                Log.d("OpSideGesture", sb.toString());
            }
            if (OpSideGestureNavView.this.mFinished) {
                OpSideGestureNavView.this.setVisibility(8);
            }
        }

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            OpSideGestureNavView.this.mExitAnimProcessing = false;
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("opSideGestureNavView : mExitAnimListener onAnimationEnd mFinished ");
                sb.append(OpSideGestureNavView.this.mFinished);
                Log.d("OpSideGesture", sb.toString());
            }
            if (OpSideGestureNavView.this.mFinished) {
                OpSideGestureNavView.this.setVisibility(8);
            }
        }

        public void onAnimationStart(Animator animator) {
            super.onAnimationStart(animator);
            if (OpSideGestureNavView.this.mEnterAnimProcessing) {
                OpSideGestureNavView.this.mEnterAnimator.cancel();
            }
            OpSideGestureNavView.this.mExitAnimProcessing = true;
            OpSideGestureNavView.this.mBackIconScale = 1.0f;
            OpSideGestureNavView.this.mAppIconScale = 0.0f;
        }
    };
    /* access modifiers changed from: private */
    public boolean mExitAnimProcessing = false;
    /* access modifiers changed from: private */
    public ObjectAnimator mExitAnimator;
    private IntProperty<OpSideGestureNavView> mExitProperty = new IntProperty<OpSideGestureNavView>("ExitAnimation") {
        public Integer get(OpSideGestureNavView opSideGestureNavView) {
            return null;
        }

        public void setValue(OpSideGestureNavView opSideGestureNavView, int i) {
            OpSideGestureNavView.this.mCurrentPoint.x = (float) i;
            OpSideGestureNavView.this.mCurrentPoint.y = OpSideGestureNavView.this.mTargetPoint.y;
            OpSideGestureNavView.this.updateView();
        }
    };
    /* access modifiers changed from: private */
    public boolean mFinished = true;
    private int mGestureState = -1;
    private boolean mHasDownEvent = false;
    private Paint mIconPaint;
    private boolean mInTouchArea = false;
    private int mLastGestureState = -1;
    private int mPosition;
    private int mRotation;
    private int mScreenHeight;
    private int mScreenWidth;
    private PointF mStartPoint = new PointF();
    /* access modifiers changed from: private */
    public PointF mTargetPoint = new PointF();
    private int mViewOffsetX;
    private int mViewOffsetY;

    public void adjustRectToBoundingBox(Rect rect) {
    }

    public void handleTouch(MotionEvent motionEvent) {
    }

    public void setIsDark(boolean z, boolean z2) {
    }

    public boolean shouldTriggerBack() {
        return true;
    }

    public OpSideGestureNavView(Context context, int i, int i2) {
        super(context);
        this.mPosition = i;
        calculateOffset(i2);
        initPaint();
        setLayoutDirection(0);
        Drawable drawable = context.getResources().getDrawable(R$drawable.op_gesture_button_side_back);
        if (drawable != null) {
            Bitmap createBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            Matrix matrix = new Matrix();
            matrix.postRotate(180.0f);
            this.mBackIcon = Bitmap.createBitmap(createBitmap, 0, 0, createBitmap.getWidth(), createBitmap.getHeight(), matrix, true);
        }
        setVisibility(8);
    }

    public void onUpdateGestureView(GesturePointContainer gesturePointContainer) {
        if (this.mHasDownEvent) {
            this.mFinished = false;
            this.mRotation = gesturePointContainer.getRotation();
            this.mScreenHeight = OpSideGestureConfiguration.getScreenHeight(this.mRotation);
            this.mScreenWidth = OpSideGestureConfiguration.getScreenWidth(this.mRotation);
            this.mLastGestureState = this.mGestureState;
            this.mGestureState = gesturePointContainer.getState();
            calculateTargetPoint(gesturePointContainer.getPoint(), this.mScreenWidth, this.mScreenHeight);
            int i = this.mGestureState;
            if (i != this.mLastGestureState && i == 2) {
                performHapticFeedback(1);
            }
            if ((this.mLastGestureState != 4 || this.mGestureState == 4) && this.mGestureState != 4 && !this.mEnterAnimProcessing && !this.mExitAnimProcessing) {
                PointF pointF = this.mCurrentPoint;
                PointF pointF2 = this.mTargetPoint;
                pointF.x = pointF2.x;
                pointF.y = pointF2.y;
            }
            if (this.mLastGestureState != 4 && this.mGestureState == 4) {
                int i2 = this.mPosition;
                if (i2 == 0) {
                    startExitAnimation((int) this.mCurrentPoint.x, 0, 200);
                } else if (i2 == 1) {
                    startExitAnimation((int) this.mCurrentPoint.x, this.mScreenWidth, 200);
                }
            } else if (this.mLastGestureState == 4 && this.mGestureState != 4) {
                startEnterAnimation((int) this.mCurrentPoint.x, (int) this.mTargetPoint.x, 200);
            }
            if (this.mGestureState == 3 && this.mLastGestureState == 2) {
                startBackIconAnimation(false, 20, 0);
                startAppIconAniamtion(true, 100, 0);
            } else if (this.mGestureState == 2 && this.mLastGestureState == 3) {
                startBackIconAnimation(true, 100, 0);
                startAppIconAniamtion(false, 20, 0);
            }
            if (!this.mEnterAnimProcessing && !this.mExitAnimProcessing) {
                updateView();
            }
        }
    }

    public void onGestureFinished(GesturePointContainer gesturePointContainer) {
        if (this.mHasDownEvent) {
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("opSideGestureNavView : onGestureFinished point : ");
                sb.append(gesturePointContainer.getPoint());
                Log.d("OpSideGesture", sb.toString());
            }
            this.mFinished = true;
            int i = this.mPosition;
            if (i == 0) {
                startExitAnimation((int) this.mCurrentPoint.x, 0, 200);
            } else if (i == 1) {
                startExitAnimation((int) this.mCurrentPoint.x, this.mScreenWidth, 200);
            }
        }
    }

    public void onDownEvent() {
        resetState();
        this.mHasDownEvent = true;
        calculateOffset(this.mRotation);
        setVisibility(0);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("opSideGestureNavView : onDownEvent mPosition : ");
            sb.append(this.mPosition);
            Log.d("OpSideGesture", sb.toString());
        }
    }

    public void onUpEvent() {
        this.mHasDownEvent = false;
        if (!this.mExitAnimProcessing) {
            setVisibility(8);
        }
        postDelayed(new Runnable() {
            public final void run() {
                OpSideGestureNavView.this.lambda$onUpEvent$0$OpSideGestureNavView();
            }
        }, 500);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("opSideGestureNavView : onUpEvent mPosition : ");
            sb.append(this.mPosition);
            Log.d("OpSideGesture", sb.toString());
        }
    }

    public /* synthetic */ void lambda$onUpEvent$0$OpSideGestureNavView() {
        if (!this.mHasDownEvent && getVisibility() != 8) {
            setVisibility(8);
        }
    }

    public void onConfigChanged(int i) {
        this.mRotation = i;
        this.mBezierControlOffset1 = OpSideGestureConfiguration.getBezierControlOffset1();
        this.mBezierControlOffset2 = OpSideGestureConfiguration.getBezierControlOffsetSwitch2();
        calculateOffset(i);
        resetState();
    }

    public boolean isExitAnimFinished() {
        return !this.mExitAnimProcessing;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(getBezierDrawingPath(), this.mBezierPaint);
        if (this.mGestureState == 2 || this.mBackIconProcessing) {
            canvas.drawBitmap(this.mBackIcon, this.mBackIconMatrix, this.mIconPaint);
        }
        if (this.mGestureState == 3 || this.mAppIconProcessing) {
            canvas.drawBitmap(this.mAppIcon, this.mAppIconMatrix, this.mIconPaint);
        }
    }

    private void initPaint() {
        this.mBezierPaint = new Paint();
        this.mBezierPaint.setStyle(Style.FILL);
        this.mBezierPaint.setStrokeWidth(12.0f);
        this.mBezierPaint.setARGB(128, 133, 133, 133);
        this.mBezierPaint.setAntiAlias(true);
        this.mIconPaint = new Paint();
        this.mIconPaint.setAntiAlias(true);
    }

    private void startEnterAnimation(int i, int i2, long j) {
        this.mEnterAnimator = ObjectAnimator.ofInt(this, this.mEnterProperty, new int[]{i, i2});
        this.mEnterAnimator.setInterpolator(BEZIER_ANIMATION_INTERPOLATOR);
        this.mEnterAnimator.setDuration(j);
        this.mEnterAnimator.addListener(this.mEnterAnimListener);
        this.mEnterAnimator.start();
    }

    private void startExitAnimation(int i, int i2, long j) {
        this.mExitAnimator = ObjectAnimator.ofInt(this, this.mExitProperty, new int[]{i, i2});
        this.mExitAnimator.setInterpolator(BEZIER_ANIMATION_INTERPOLATOR);
        this.mExitAnimator.setDuration(j);
        this.mExitAnimator.addListener(this.mExitAnimListener);
        this.mExitAnimator.start();
    }

    private void startBackIconAnimation(boolean z, long j, long j2) {
        ObjectAnimator objectAnimator = this.mBackIconAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (z) {
            this.mBackIconAnimator = ObjectAnimator.ofFloat(this, this.mBackIconProperty, new float[]{0.0f, 1.0f});
            this.mBackIconAnimator.setInterpolator(ICON_ENTER_ANIMATION_INTERPOLATOR);
        } else {
            this.mBackIconAnimator = ObjectAnimator.ofFloat(this, this.mBackIconProperty, new float[]{1.0f, 0.0f});
            this.mBackIconAnimator.setInterpolator(ICON_EXIT_ANIMATION_INTERPOLATOR);
        }
        this.mBackIconAnimator.addListener(this.mBackIconAnimListener);
        this.mBackIconAnimator.setDuration(j);
        this.mBackIconAnimator.setStartDelay(j2);
        this.mBackIconAnimator.start();
    }

    private void startAppIconAniamtion(boolean z, long j, long j2) {
        ObjectAnimator objectAnimator = this.mAppIconAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (z) {
            this.mAppIconAnimator = ObjectAnimator.ofFloat(this, this.mAppIconProperty, new float[]{0.0f, 1.0f});
            this.mAppIconAnimator.setInterpolator(ICON_ENTER_ANIMATION_INTERPOLATOR);
        } else {
            this.mAppIconAnimator = ObjectAnimator.ofFloat(this, this.mAppIconProperty, new float[]{1.0f, 0.0f});
            this.mAppIconAnimator.setInterpolator(ICON_EXIT_ANIMATION_INTERPOLATOR);
        }
        this.mAppIconAnimator.addListener(this.mAppIconAnimListener);
        this.mAppIconAnimator.setDuration(j);
        this.mAppIconAnimator.setStartDelay(j2);
        this.mAppIconAnimator.start();
    }

    /* access modifiers changed from: private */
    public void updateView() {
        calculateControlPoint();
        calculateMatrix();
        invalidate();
    }

    private Path getBezierDrawingPath() {
        Path path = new Path();
        PointF pointF = this.mStartPoint;
        path.moveTo(pointF.x - ((float) this.mViewOffsetX), pointF.y - ((float) this.mViewOffsetY));
        addCurveToPoint(path);
        return path;
    }

    private void addCurveToPoint(Path path) {
        PointF pointF = this.mBezierControlPoint1;
        float f = pointF.x;
        int i = this.mViewOffsetX;
        float f2 = f - ((float) i);
        float f3 = pointF.y;
        int i2 = this.mViewOffsetY;
        float f4 = f3 - ((float) i2);
        PointF pointF2 = this.mBezierControlPoint2;
        float f5 = pointF2.x - ((float) i);
        float f6 = pointF2.y - ((float) i2);
        PointF pointF3 = this.mCurrentPoint;
        path.cubicTo(f2, f4, f5, f6, pointF3.x - ((float) i), pointF3.y - ((float) i2));
        PointF pointF4 = this.mBezierControlPoint3;
        float f7 = pointF4.x;
        int i3 = this.mViewOffsetX;
        float f8 = f7 - ((float) i3);
        float f9 = pointF4.y;
        int i4 = this.mViewOffsetY;
        float f10 = f9 - ((float) i4);
        PointF pointF5 = this.mBezierControlPoint4;
        float f11 = pointF5.x - ((float) i3);
        float f12 = pointF5.y - ((float) i4);
        PointF pointF6 = this.mEndPoint;
        path.cubicTo(f8, f10, f11, f12, pointF6.x - ((float) i3), pointF6.y - ((float) i4));
    }

    private void calculateTargetPoint(PointF pointF, int i, int i2) {
        pointF.y = (float) OpSideGestureConfiguration.boundToRange((int) pointF.y, (int) Math.max(((float) i2) * OpSideGestureConfiguration.PORTRAIT_NON_DETECT_SCALE, (float) OpSideGestureConfiguration.getAnimRadius()), i2 - OpSideGestureConfiguration.getAnimRadius());
        int i3 = this.mPosition;
        if (i3 == 0) {
            this.mTargetPoint.x = (float) OpSideGestureConfiguration.boundToRange((int) (pointF.x * 0.32299998f), 0, OpSideGestureConfiguration.getAnimSwitchDistance());
            this.mTargetPoint.y = pointF.y;
        } else if (i3 == 1) {
            float f = (float) i;
            this.mTargetPoint.x = (float) OpSideGestureConfiguration.boundToRange((int) (f - ((f - pointF.x) * 0.32299998f)), i - OpSideGestureConfiguration.getAnimSwitchDistance(), i);
            this.mTargetPoint.y = pointF.y;
        }
    }

    private void calculateControlPoint() {
        int i = this.mPosition;
        if (i == 0) {
            PointF pointF = this.mStartPoint;
            pointF.x = 0.0f;
            pointF.y = this.mCurrentPoint.y - ((float) OpSideGestureConfiguration.getAnimRadius());
            PointF pointF2 = this.mEndPoint;
            pointF2.x = 0.0f;
            pointF2.y = this.mCurrentPoint.y + ((float) OpSideGestureConfiguration.getAnimRadius());
            PointF pointF3 = this.mBezierControlPoint1;
            pointF3.x = 0.0f;
            pointF3.y = this.mCurrentPoint.y - ((float) (OpSideGestureConfiguration.getAnimRadius() - this.mBezierControlOffset1));
            PointF pointF4 = this.mBezierControlPoint2;
            PointF pointF5 = this.mCurrentPoint;
            pointF4.x = pointF5.x;
            float f = pointF5.y;
            int i2 = this.mBezierControlOffset2;
            pointF4.y = f - ((float) i2);
            PointF pointF6 = this.mBezierControlPoint3;
            pointF6.x = pointF5.x;
            pointF6.y = pointF5.y + ((float) i2);
            PointF pointF7 = this.mBezierControlPoint4;
            pointF7.x = 0.0f;
            pointF7.y = pointF5.y + ((float) (OpSideGestureConfiguration.getAnimRadius() - this.mBezierControlOffset1));
        } else if (i == 1) {
            PointF pointF8 = this.mStartPoint;
            pointF8.x = (float) this.mScreenWidth;
            pointF8.y = this.mCurrentPoint.y - ((float) OpSideGestureConfiguration.getAnimRadius());
            PointF pointF9 = this.mEndPoint;
            pointF9.x = (float) this.mScreenWidth;
            pointF9.y = this.mCurrentPoint.y + ((float) OpSideGestureConfiguration.getAnimRadius());
            PointF pointF10 = this.mBezierControlPoint1;
            pointF10.x = (float) this.mScreenWidth;
            pointF10.y = this.mCurrentPoint.y - ((float) (OpSideGestureConfiguration.getAnimRadius() - this.mBezierControlOffset1));
            PointF pointF11 = this.mBezierControlPoint2;
            PointF pointF12 = this.mCurrentPoint;
            pointF11.x = pointF12.x;
            float f2 = pointF12.y;
            int i3 = this.mBezierControlOffset2;
            pointF11.y = f2 - ((float) i3);
            PointF pointF13 = this.mBezierControlPoint3;
            pointF13.x = pointF12.x;
            pointF13.y = pointF12.y + ((float) i3);
            PointF pointF14 = this.mBezierControlPoint4;
            pointF14.x = (float) this.mScreenWidth;
            pointF14.y = pointF12.y + ((float) (OpSideGestureConfiguration.getAnimRadius() - this.mBezierControlOffset1));
        }
    }

    private void calculateMatrix() {
        this.mBackIconMatrix.reset();
        this.mAppIconMatrix.reset();
        int i = this.mPosition;
        if (i == 0) {
            float boundToRange = (float) OpSideGestureConfiguration.boundToRange((int) ((this.mCurrentPoint.x - ((float) OpSideGestureConfiguration.getAnimCancelDistance())) - ((float) OpSideGestureConfiguration.getIconSize())), -OpSideGestureConfiguration.getIconSize(), OpSideGestureConfiguration.getAnimCancelDistance());
            this.mBackIconMatrix.setTranslate(boundToRange, (this.mCurrentPoint.y - ((float) this.mViewOffsetY)) - ((float) (OpSideGestureConfiguration.getIconSize() / 2)));
            Matrix matrix = this.mBackIconMatrix;
            float f = this.mBackIconScale;
            matrix.preScale(f, f, (float) (OpSideGestureConfiguration.getIconSize() / 2), (float) (OpSideGestureConfiguration.getIconSize() / 2));
            this.mAppIconMatrix.setTranslate(boundToRange, (this.mCurrentPoint.y - ((float) this.mViewOffsetY)) - ((float) (OpSideGestureConfiguration.getIconSize() / 2)));
            Matrix matrix2 = this.mAppIconMatrix;
            float f2 = this.mAppIconScale;
            matrix2.preScale(f2, f2, (float) (OpSideGestureConfiguration.getIconSize() / 2), (float) (OpSideGestureConfiguration.getIconSize() / 2));
        } else if (i == 1) {
            float boundToRange2 = (float) OpSideGestureConfiguration.boundToRange((int) ((this.mCurrentPoint.x - ((float) this.mViewOffsetX)) + ((float) OpSideGestureConfiguration.getAnimCancelDistance())), (OpSideGestureConfiguration.getWindowWidth() - OpSideGestureConfiguration.getAnimCancelDistance()) - OpSideGestureConfiguration.getIconSize(), OpSideGestureConfiguration.getWindowWidth());
            this.mBackIconMatrix.setTranslate(boundToRange2, (this.mCurrentPoint.y - ((float) this.mViewOffsetY)) - ((float) (OpSideGestureConfiguration.getIconSize() / 2)));
            Matrix matrix3 = this.mBackIconMatrix;
            float f3 = this.mBackIconScale;
            matrix3.preScale(f3, f3, (float) (OpSideGestureConfiguration.getIconSize() / 2), (float) (OpSideGestureConfiguration.getIconSize() / 2));
            this.mAppIconMatrix.setTranslate(boundToRange2, (this.mCurrentPoint.y - ((float) this.mViewOffsetY)) - ((float) (OpSideGestureConfiguration.getIconSize() / 2)));
            Matrix matrix4 = this.mAppIconMatrix;
            float f4 = this.mAppIconScale;
            matrix4.preScale(f4, f4, (float) (OpSideGestureConfiguration.getIconSize() / 2), (float) (OpSideGestureConfiguration.getIconSize() / 2));
        }
        if (getLayoutDirection() == 1) {
            this.mBackIconMatrix.preRotate(180.0f, (float) (OpSideGestureConfiguration.getIconSize() / 2), (float) (OpSideGestureConfiguration.getIconSize() / 2));
        }
    }

    private void calculateOffset(int i) {
        int i2 = this.mPosition;
        if (i2 == 0) {
            this.mViewOffsetX = 0;
            this.mViewOffsetY = OpSideGestureConfiguration.getScreenHeight(i) - OpSideGestureConfiguration.getWindowHeight(i);
        } else if (i2 == 1) {
            this.mViewOffsetX = OpSideGestureConfiguration.getScreenWidth(i) - OpSideGestureConfiguration.getWindowWidth();
            this.mViewOffsetY = OpSideGestureConfiguration.getScreenHeight(i) - OpSideGestureConfiguration.getWindowHeight(i);
        }
    }

    public void resetState() {
        this.mFinished = true;
        this.mLastGestureState = -1;
        this.mGestureState = -1;
        if (this.mEnterAnimProcessing) {
            this.mEnterAnimator.cancel();
        }
        if (this.mExitAnimProcessing) {
            this.mExitAnimator.cancel();
        }
        if (this.mBackIconProcessing) {
            this.mBackIconAnimator.cancel();
        }
        if (this.mAppIconProcessing) {
            this.mAppIconAnimator.cancel();
        }
        setVisibility(8);
    }

    public void setIsLeftPanel(boolean z) {
        this.mPosition = z ^ true ? 1 : 0;
    }
}
