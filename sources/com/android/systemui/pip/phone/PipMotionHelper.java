package com.android.systemui.pip.phone;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.RectEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager.StackInfo;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.PipSnapAlgorithm;
import com.android.systemui.Interpolators;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;

public class PipMotionHelper implements Callback, PipAppOpsListener.Callback {
    private static final RectEvaluator RECT_EVALUATOR = new RectEvaluator(new Rect());
    private IActivityManager mActivityManager;
    private IActivityTaskManager mActivityTaskManager;
    /* access modifiers changed from: private */
    public AnimationHandler mAnimationHandler;
    private final Rect mBounds = new Rect();
    private ValueAnimator mBoundsAnimator = null;
    private Context mContext;
    private FlingAnimationUtils mFlingAnimationUtils;
    private Handler mHandler;
    private PipMenuActivityController mMenuController;
    private PipSnapAlgorithm mSnapAlgorithm;
    private final Rect mStableInsets = new Rect();

    public PipMotionHelper(Context context, IActivityManager iActivityManager, IActivityTaskManager iActivityTaskManager, PipMenuActivityController pipMenuActivityController, PipSnapAlgorithm pipSnapAlgorithm, FlingAnimationUtils flingAnimationUtils) {
        this.mContext = context;
        this.mHandler = new Handler(ForegroundThread.get().getLooper(), this);
        this.mActivityManager = iActivityManager;
        this.mActivityTaskManager = iActivityTaskManager;
        this.mMenuController = pipMenuActivityController;
        this.mSnapAlgorithm = pipSnapAlgorithm;
        this.mFlingAnimationUtils = flingAnimationUtils;
        this.mAnimationHandler = new AnimationHandler();
        this.mAnimationHandler.setProvider(new SfVsyncFrameCallbackProvider());
        onConfigurationChanged();
    }

    /* access modifiers changed from: 0000 */
    public void onConfigurationChanged() {
        this.mSnapAlgorithm.onConfigurationChanged();
        WindowManagerWrapper.getInstance().getStableInsets(this.mStableInsets);
    }

    /* access modifiers changed from: 0000 */
    public void synchronizePinnedStackBounds() {
        cancelAnimations();
        try {
            StackInfo stackInfo = this.mActivityTaskManager.getStackInfo(2, 0);
            if (stackInfo != null) {
                this.mBounds.set(stackInfo.bounds);
            }
        } catch (RemoteException unused) {
            Log.w("PipMotionHelper", "Failed to get pinned stack bounds");
        }
    }

    /* access modifiers changed from: 0000 */
    public void movePip(Rect rect) {
        cancelAnimations();
        resizePipUnchecked(rect);
        this.mBounds.set(rect);
    }

    /* access modifiers changed from: 0000 */
    public void expandPip() {
        expandPip(false);
    }

    /* access modifiers changed from: 0000 */
    public void expandPip(boolean z) {
        cancelAnimations();
        this.mMenuController.hideMenuWithoutResize();
        this.mHandler.post(new Runnable(z) {
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                PipMotionHelper.this.lambda$expandPip$0$PipMotionHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$expandPip$0$PipMotionHelper(boolean z) {
        try {
            this.mActivityTaskManager.dismissPip(!z, 300);
        } catch (RemoteException e) {
            Log.e("PipMotionHelper", "Error expanding PiP activity", e);
        }
    }

    public void dismissPip() {
        cancelAnimations();
        this.mMenuController.hideMenuWithoutResize();
        this.mHandler.post(new Runnable() {
            public final void run() {
                PipMotionHelper.this.lambda$dismissPip$1$PipMotionHelper();
            }
        });
    }

    public /* synthetic */ void lambda$dismissPip$1$PipMotionHelper() {
        try {
            this.mActivityTaskManager.removeStacksInWindowingModes(new int[]{2});
        } catch (RemoteException e) {
            Log.e("PipMotionHelper", "Failed to remove PiP", e);
        }
    }

    /* access modifiers changed from: 0000 */
    public Rect getBounds() {
        return this.mBounds;
    }

    /* access modifiers changed from: 0000 */
    public Rect getClosestMinimizedBounds(Rect rect, Rect rect2) {
        Point point = new Point();
        this.mContext.getDisplay().getRealSize(point);
        Rect findClosestSnapBounds = this.mSnapAlgorithm.findClosestSnapBounds(rect2, rect);
        this.mSnapAlgorithm.applyMinimizedOffset(findClosestSnapBounds, rect2, point, this.mStableInsets);
        return findClosestSnapBounds;
    }

    /* access modifiers changed from: 0000 */
    public boolean shouldDismissPip() {
        Point point = new Point();
        this.mContext.getDisplay().getRealSize(point);
        int i = point.y - this.mStableInsets.bottom;
        Rect rect = this.mBounds;
        int i2 = rect.bottom;
        if (i2 <= i || ((float) (i2 - i)) / ((float) rect.height()) < 0.3f) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: 0000 */
    public Rect animateToClosestMinimizedState(Rect rect, AnimatorUpdateListener animatorUpdateListener) {
        cancelAnimations();
        Rect closestMinimizedBounds = getClosestMinimizedBounds(this.mBounds, rect);
        if (!this.mBounds.equals(closestMinimizedBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, closestMinimizedBounds, 200, Interpolators.LINEAR_OUT_SLOW_IN);
            if (animatorUpdateListener != null) {
                this.mBoundsAnimator.addUpdateListener(animatorUpdateListener);
            }
            this.mBoundsAnimator.start();
        }
        return closestMinimizedBounds;
    }

    /* access modifiers changed from: 0000 */
    public Rect flingToSnapTarget(float f, float f2, float f3, Rect rect, AnimatorUpdateListener animatorUpdateListener, AnimatorListener animatorListener, Point point) {
        cancelAnimations();
        Rect findClosestSnapBounds = this.mSnapAlgorithm.findClosestSnapBounds(rect, this.mBounds, f2, f3, point);
        if (!this.mBounds.equals(findClosestSnapBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, findClosestSnapBounds, 0, Interpolators.FAST_OUT_SLOW_IN);
            this.mFlingAnimationUtils.apply(this.mBoundsAnimator, 0.0f, distanceBetweenRectOffsets(this.mBounds, findClosestSnapBounds), f);
            if (animatorUpdateListener != null) {
                this.mBoundsAnimator.addUpdateListener(animatorUpdateListener);
            }
            if (animatorListener != null) {
                this.mBoundsAnimator.addListener(animatorListener);
            }
            this.mBoundsAnimator.start();
        }
        return findClosestSnapBounds;
    }

    /* access modifiers changed from: 0000 */
    public Rect animateToClosestSnapTarget(Rect rect, AnimatorUpdateListener animatorUpdateListener, AnimatorListener animatorListener) {
        cancelAnimations();
        Rect findClosestSnapBounds = this.mSnapAlgorithm.findClosestSnapBounds(rect, this.mBounds);
        if (!this.mBounds.equals(findClosestSnapBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, findClosestSnapBounds, 225, Interpolators.FAST_OUT_SLOW_IN);
            if (animatorUpdateListener != null) {
                this.mBoundsAnimator.addUpdateListener(animatorUpdateListener);
            }
            if (animatorListener != null) {
                this.mBoundsAnimator.addListener(animatorListener);
            }
            this.mBoundsAnimator.start();
        }
        return findClosestSnapBounds;
    }

    /* access modifiers changed from: 0000 */
    public float animateToExpandedState(Rect rect, Rect rect2, Rect rect3) {
        float snapFraction = this.mSnapAlgorithm.getSnapFraction(new Rect(this.mBounds), rect2);
        this.mSnapAlgorithm.applySnapFraction(rect, rect3, snapFraction);
        resizeAndAnimatePipUnchecked(rect, 250);
        return snapFraction;
    }

    /* access modifiers changed from: 0000 */
    public void animateToUnexpandedState(Rect rect, float f, Rect rect2, Rect rect3, boolean z, boolean z2) {
        if (f < 0.0f) {
            f = this.mSnapAlgorithm.getSnapFraction(new Rect(this.mBounds), rect3);
        }
        this.mSnapAlgorithm.applySnapFraction(rect, rect2, f);
        if (z) {
            rect = getClosestMinimizedBounds(rect, rect2);
        }
        if (z2) {
            movePip(rect);
        } else {
            resizeAndAnimatePipUnchecked(rect, 250);
        }
    }

    /* access modifiers changed from: 0000 */
    public void animateToOffset(Rect rect, int i) {
        cancelAnimations();
        adjustAndAnimatePipOffset(rect, i, 300);
    }

    private void adjustAndAnimatePipOffset(Rect rect, int i, int i2) {
        if (i != 0) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = rect;
            obtain.argi1 = i;
            obtain.argi2 = i2;
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(3, obtain));
        }
    }

    /* access modifiers changed from: 0000 */
    public Rect animateDismiss(Rect rect, float f, float f2, AnimatorUpdateListener animatorUpdateListener) {
        cancelAnimations();
        float length = PointF.length(f, f2);
        boolean z = length > this.mFlingAnimationUtils.getMinVelocityPxPerSecond();
        Point dismissEndPoint = getDismissEndPoint(rect, f, f2, z);
        Rect rect2 = new Rect(rect);
        rect2.offsetTo(dismissEndPoint.x, dismissEndPoint.y);
        this.mBoundsAnimator = createAnimationToBounds(this.mBounds, rect2, 175, Interpolators.FAST_OUT_LINEAR_IN);
        this.mBoundsAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                PipMotionHelper.this.dismissPip();
            }
        });
        if (z) {
            this.mFlingAnimationUtils.apply(this.mBoundsAnimator, 0.0f, distanceBetweenRectOffsets(this.mBounds, rect2), length);
        }
        if (animatorUpdateListener != null) {
            this.mBoundsAnimator.addUpdateListener(animatorUpdateListener);
        }
        this.mBoundsAnimator.start();
        return rect2;
    }

    /* access modifiers changed from: 0000 */
    public void cancelAnimations() {
        ValueAnimator valueAnimator = this.mBoundsAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
            this.mBoundsAnimator = null;
        }
    }

    private ValueAnimator createAnimationToBounds(Rect rect, Rect rect2, int i, Interpolator interpolator) {
        C09362 r0 = new ValueAnimator() {
            public AnimationHandler getAnimationHandler() {
                return PipMotionHelper.this.mAnimationHandler;
            }
        };
        r0.setObjectValues(new Object[]{rect, rect2});
        r0.setEvaluator(RECT_EVALUATOR);
        r0.setDuration((long) i);
        r0.setInterpolator(interpolator);
        r0.addUpdateListener(new AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                PipMotionHelper.this.lambda$createAnimationToBounds$2$PipMotionHelper(valueAnimator);
            }
        });
        return r0;
    }

    public /* synthetic */ void lambda$createAnimationToBounds$2$PipMotionHelper(ValueAnimator valueAnimator) {
        resizePipUnchecked((Rect) valueAnimator.getAnimatedValue());
    }

    private void resizePipUnchecked(Rect rect) {
        if (!rect.equals(this.mBounds)) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = rect;
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(1, obtain));
        }
    }

    private void resizeAndAnimatePipUnchecked(Rect rect, int i) {
        if (!rect.equals(this.mBounds)) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = rect;
            obtain.argi1 = i;
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(2, obtain));
        }
    }

    private Point getDismissEndPoint(Rect rect, float f, float f2, boolean z) {
        Point point = new Point();
        this.mContext.getDisplay().getRealSize(point);
        float height = ((float) point.y) + (((float) rect.height()) * 0.1f);
        if (!z || f == 0.0f || f2 == 0.0f) {
            return new Point(rect.left, (int) height);
        }
        float f3 = f2 / f;
        return new Point((int) ((height - (((float) rect.top) - (((float) rect.left) * f3))) / f3), (int) height);
    }

    private float distanceBetweenRectOffsets(Rect rect, Rect rect2) {
        return PointF.length((float) (rect.left - rect2.left), (float) (rect.top - rect2.top));
    }

    public boolean handleMessage(Message message) {
        int i = message.what;
        String str = "PipMotionHelper";
        if (i == 1) {
            Rect rect = (Rect) ((SomeArgs) message.obj).arg1;
            try {
                this.mActivityTaskManager.resizePinnedStack(rect, null);
                this.mBounds.set(rect);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Could not resize pinned stack to bounds: ");
                sb.append(rect);
                Log.e(str, sb.toString(), e);
            }
            return true;
        } else if (i == 2) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            Rect rect2 = (Rect) someArgs.arg1;
            int i2 = someArgs.argi1;
            try {
                StackInfo stackInfo = this.mActivityTaskManager.getStackInfo(2, 0);
                if (stackInfo == null) {
                    return true;
                }
                this.mActivityTaskManager.resizeStack(stackInfo.stackId, rect2, false, true, true, i2);
                this.mBounds.set(rect2);
                return true;
            } catch (RemoteException e2) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Could not animate resize pinned stack to bounds: ");
                sb2.append(rect2);
                Log.e(str, sb2.toString(), e2);
            }
        } else if (i != 3) {
            return false;
        } else {
            SomeArgs someArgs2 = (SomeArgs) message.obj;
            Rect rect3 = (Rect) someArgs2.arg1;
            int i3 = someArgs2.argi1;
            int i4 = someArgs2.argi2;
            try {
                StackInfo stackInfo2 = this.mActivityTaskManager.getStackInfo(2, 0);
                if (stackInfo2 == null) {
                    return true;
                }
                this.mActivityTaskManager.offsetPinnedStackBounds(stackInfo2.stackId, rect3, 0, i3, i4);
                Rect rect4 = new Rect(rect3);
                rect4.offset(0, i3);
                this.mBounds.set(rect4);
                return true;
            } catch (RemoteException e3) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Could not animate offset pinned stack with offset: ");
                sb3.append(i3);
                Log.e(str, sb3.toString(), e3);
            }
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("  ");
        String sb2 = sb.toString();
        StringBuilder sb3 = new StringBuilder();
        sb3.append(str);
        sb3.append("PipMotionHelper");
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append(sb2);
        sb4.append("mBounds=");
        sb4.append(this.mBounds);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append(sb2);
        sb5.append("mStableInsets=");
        sb5.append(this.mStableInsets);
        printWriter.println(sb5.toString());
    }
}
