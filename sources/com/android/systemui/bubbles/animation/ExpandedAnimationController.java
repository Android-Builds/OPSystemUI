package com.android.systemui.bubbles.animation;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.View;
import android.view.WindowInsets;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.DynamicAnimation.ViewProperty;
import androidx.dynamicanimation.animation.SpringForce;
import com.android.systemui.R$dimen;
import com.google.android.collect.Sets;
import java.util.Set;

public class ExpandedAnimationController extends PhysicsAnimationController {
    private Runnable mAfterCollapse;
    private Runnable mAfterExpand;
    private boolean mAnimatingCollapse = false;
    private boolean mAnimatingExpand = false;
    private boolean mBubbleDraggedOutEnough = false;
    private View mBubbleDraggingOut;
    private float mBubblePaddingPx;
    private float mBubbleSizePx;
    private PointF mCollapsePoint;
    private Point mDisplaySize;
    private int mExpandedViewPadding;
    private boolean mIndividualBubbleWithinDismissTarget = false;
    private float mPipDismissHeight;
    private boolean mSpringingBubbleToTouch = false;
    private float mStackOffsetPx;
    private float mStatusBarHeight;

    /* access modifiers changed from: 0000 */
    public int getNextAnimationInChain(ViewProperty viewProperty, int i) {
        return -1;
    }

    /* access modifiers changed from: 0000 */
    public float getOffsetForChainedPropertyAnimation(ViewProperty viewProperty) {
        return 0.0f;
    }

    public ExpandedAnimationController(Point point, int i) {
        this.mDisplaySize = point;
        this.mExpandedViewPadding = i;
    }

    public void expandFromStack(Runnable runnable) {
        this.mAnimatingCollapse = false;
        this.mAnimatingExpand = true;
        this.mAfterExpand = runnable;
        startOrUpdateExpandAnimation();
    }

    public void collapseBackToStack(PointF pointF, Runnable runnable) {
        this.mAnimatingExpand = false;
        this.mAnimatingCollapse = true;
        this.mAfterCollapse = runnable;
        this.mCollapsePoint = pointF;
        startOrUpdateCollapseAnimation();
    }

    private void startOrUpdateExpandAnimation() {
        animationsForChildrenFromIndex(0, new ChildAnimationConfigurator() {
            public final void configureAnimationForChildAtIndex(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
                ExpandedAnimationController.this.mo12173x6a360fa8(i, physicsPropertyAnimator);
            }
        }).startAll(new Runnable() {
            public final void run() {
                ExpandedAnimationController.this.mo12174x9385f429();
            }
        });
    }

    /* renamed from: lambda$startOrUpdateExpandAnimation$0$ExpandedAnimationController */
    public /* synthetic */ void mo12173x6a360fa8(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
        physicsPropertyAnimator.position(getBubbleLeft(i), getExpandedY(), new Runnable[0]);
    }

    /* renamed from: lambda$startOrUpdateExpandAnimation$1$ExpandedAnimationController */
    public /* synthetic */ void mo12174x9385f429() {
        this.mAnimatingExpand = false;
        Runnable runnable = this.mAfterExpand;
        if (runnable != null) {
            runnable.run();
        }
        this.mAfterExpand = null;
    }

    private void startOrUpdateCollapseAnimation() {
        animationsForChildrenFromIndex(0, new ChildAnimationConfigurator(this.mLayout.isFirstChildXLeftOfCenter(this.mCollapsePoint.x) ? -1.0f : 1.0f) {
            private final /* synthetic */ float f$1;

            {
                this.f$1 = r2;
            }

            public final void configureAnimationForChildAtIndex(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
                ExpandedAnimationController.this.mo12171x4a246c77(this.f$1, i, physicsPropertyAnimator);
            }
        }).startAll(new Runnable() {
            public final void run() {
                ExpandedAnimationController.this.mo12172x737450f8();
            }
        });
    }

    /* renamed from: lambda$startOrUpdateCollapseAnimation$2$ExpandedAnimationController */
    public /* synthetic */ void mo12171x4a246c77(float f, int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
        PointF pointF = this.mCollapsePoint;
        physicsPropertyAnimator.position(pointF.x + (f * ((float) i) * this.mStackOffsetPx), pointF.y, new Runnable[0]);
    }

    /* renamed from: lambda$startOrUpdateCollapseAnimation$3$ExpandedAnimationController */
    public /* synthetic */ void mo12172x737450f8() {
        this.mAnimatingCollapse = false;
        Runnable runnable = this.mAfterCollapse;
        if (runnable != null) {
            runnable.run();
        }
        this.mAfterCollapse = null;
    }

    public void prepareForBubbleDrag(View view) {
        this.mLayout.cancelAnimationsOnView(view);
        this.mBubbleDraggingOut = view;
        this.mBubbleDraggingOut.setTranslationZ(32767.0f);
    }

    public void dragBubbleOut(View view, float f, float f2) {
        boolean z = true;
        if (this.mSpringingBubbleToTouch) {
            if (this.mLayout.arePropertiesAnimatingOnView(view, DynamicAnimation.TRANSLATION_X, DynamicAnimation.TRANSLATION_Y)) {
                PhysicsPropertyAnimator animationForChild = animationForChild(this.mBubbleDraggingOut);
                animationForChild.translationX(f, new Runnable[0]);
                animationForChild.translationY(f2, new Runnable[0]);
                animationForChild.withStiffness(10000.0f);
                animationForChild.start(new Runnable[0]);
            } else {
                this.mSpringingBubbleToTouch = false;
            }
        }
        if (!this.mSpringingBubbleToTouch && !this.mIndividualBubbleWithinDismissTarget) {
            view.setTranslationX(f);
            view.setTranslationY(f2);
        }
        if (f2 <= getExpandedY() + this.mBubbleSizePx && f2 >= getExpandedY() - this.mBubbleSizePx) {
            z = false;
        }
        if (z != this.mBubbleDraggedOutEnough) {
            updateBubblePositions();
            this.mBubbleDraggedOutEnough = z;
        }
    }

    public void dismissDraggedOutBubble(View view, Runnable runnable) {
        this.mIndividualBubbleWithinDismissTarget = false;
        PhysicsPropertyAnimator animationForChild = animationForChild(view);
        animationForChild.withStiffness(10000.0f);
        animationForChild.scaleX(1.1f, new Runnable[0]);
        animationForChild.scaleY(1.1f, new Runnable[0]);
        animationForChild.alpha(0.0f, runnable);
        animationForChild.start(new Runnable[0]);
        updateBubblePositions();
    }

    public View getDraggedOutBubble() {
        return this.mBubbleDraggingOut;
    }

    public void magnetBubbleToDismiss(View view, float f, float f2, float f3, Runnable runnable) {
        this.mIndividualBubbleWithinDismissTarget = true;
        this.mSpringingBubbleToTouch = false;
        PhysicsPropertyAnimator animationForChild = animationForChild(view);
        animationForChild.withStiffness(1500.0f);
        animationForChild.withDampingRatio(0.75f);
        animationForChild.withPositionStartVelocities(f, f2);
        animationForChild.translationX((((float) this.mLayout.getWidth()) / 2.0f) - (this.mBubbleSizePx / 2.0f), new Runnable[0]);
        animationForChild.translationY(f3, runnable);
        animationForChild.start(new Runnable[0]);
    }

    public void demagnetizeBubbleTo(float f, float f2, float f3, float f4) {
        this.mIndividualBubbleWithinDismissTarget = false;
        this.mSpringingBubbleToTouch = true;
        PhysicsPropertyAnimator animationForChild = animationForChild(this.mBubbleDraggingOut);
        animationForChild.translationX(f, new Runnable[0]);
        animationForChild.translationY(f2, new Runnable[0]);
        animationForChild.withPositionStartVelocities(f3, f4);
        animationForChild.withStiffness(10000.0f);
        animationForChild.start(new Runnable[0]);
    }

    public void snapBubbleBack(View view, float f, float f2) {
        int indexOfChild = this.mLayout.indexOfChild(view);
        PhysicsPropertyAnimator animationForChildAtIndex = animationForChildAtIndex(indexOfChild);
        animationForChildAtIndex.position(getBubbleLeft(indexOfChild), getExpandedY(), new Runnable[0]);
        animationForChildAtIndex.withPositionStartVelocities(f, f2);
        animationForChildAtIndex.start(new Runnable(view) {
            private final /* synthetic */ View f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                this.f$0.setTranslationZ(0.0f);
            }
        });
        updateBubblePositions();
    }

    public void onGestureFinished() {
        this.mBubbleDraggedOutEnough = false;
        this.mBubbleDraggingOut = null;
    }

    public void updateYPosition(Runnable runnable) {
        if (this.mLayout != null) {
            animationsForChildrenFromIndex(0, new ChildAnimationConfigurator() {
                public final void configureAnimationForChildAtIndex(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
                    ExpandedAnimationController.this.lambda$updateYPosition$5$ExpandedAnimationController(i, physicsPropertyAnimator);
                }
            }).startAll(runnable);
        }
    }

    public /* synthetic */ void lambda$updateYPosition$5$ExpandedAnimationController(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
        physicsPropertyAnimator.translationY(getExpandedY(), new Runnable[0]);
    }

    public float getExpandedY() {
        PhysicsAnimationLayout physicsAnimationLayout = this.mLayout;
        float f = 0.0f;
        if (physicsAnimationLayout == null || physicsAnimationLayout.getRootWindowInsets() == null) {
            return 0.0f;
        }
        WindowInsets rootWindowInsets = this.mLayout.getRootWindowInsets();
        float f2 = this.mBubblePaddingPx;
        float f3 = this.mStatusBarHeight;
        if (rootWindowInsets.getDisplayCutout() != null) {
            f = (float) rootWindowInsets.getDisplayCutout().getSafeInsetTop();
        }
        return f2 + Math.max(f3, f);
    }

    /* access modifiers changed from: 0000 */
    public void onActiveControllerForLayout(PhysicsAnimationLayout physicsAnimationLayout) {
        Resources resources = physicsAnimationLayout.getResources();
        this.mStackOffsetPx = (float) resources.getDimensionPixelSize(R$dimen.bubble_stack_offset);
        this.mBubblePaddingPx = (float) resources.getDimensionPixelSize(R$dimen.bubble_padding);
        this.mBubbleSizePx = (float) resources.getDimensionPixelSize(R$dimen.individual_bubble_size);
        this.mStatusBarHeight = (float) resources.getDimensionPixelSize(17105422);
        this.mPipDismissHeight = (float) resources.getDimensionPixelSize(R$dimen.pip_dismiss_gradient_height);
        this.mLayout.setVisibility(0);
        animationsForChildrenFromIndex(0, C0820x26c6f19d.INSTANCE).startAll(new Runnable[0]);
    }

    static /* synthetic */ void lambda$onActiveControllerForLayout$7(int i, PhysicsPropertyAnimator physicsPropertyAnimator) {
        physicsPropertyAnimator.scaleX(1.0f, new Runnable[0]);
        physicsPropertyAnimator.scaleY(1.0f, new Runnable[0]);
        physicsPropertyAnimator.alpha(1.0f, new Runnable[0]);
    }

    /* access modifiers changed from: 0000 */
    public Set<ViewProperty> getAnimatedProperties() {
        return Sets.newHashSet(new ViewProperty[]{DynamicAnimation.TRANSLATION_X, DynamicAnimation.TRANSLATION_Y, DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y, DynamicAnimation.ALPHA});
    }

    /* access modifiers changed from: 0000 */
    public SpringForce getSpringForce(ViewProperty viewProperty, View view) {
        SpringForce springForce = new SpringForce();
        springForce.setDampingRatio(0.75f);
        springForce.setStiffness(200.0f);
        return springForce;
    }

    /* access modifiers changed from: 0000 */
    public void onChildAdded(View view, int i) {
        if (this.mAnimatingExpand) {
            startOrUpdateExpandAnimation();
        } else if (this.mAnimatingCollapse) {
            startOrUpdateCollapseAnimation();
        } else {
            view.setTranslationX(getXForChildAtIndex(i));
            PhysicsPropertyAnimator animationForChild = animationForChild(view);
            animationForChild.translationY(getExpandedY() - (this.mBubbleSizePx * 4.0f), getExpandedY(), new Runnable[0]);
            animationForChild.start(new Runnable[0]);
            updateBubblePositions();
        }
    }

    /* access modifiers changed from: 0000 */
    public void onChildRemoved(View view, int i, Runnable runnable) {
        PhysicsPropertyAnimator animationForChild = animationForChild(view);
        if (view.equals(this.mBubbleDraggingOut)) {
            this.mBubbleDraggingOut = null;
            runnable.run();
        } else {
            animationForChild.alpha(0.0f, runnable);
            animationForChild.withStiffness(10000.0f);
            animationForChild.withDampingRatio(1.0f);
            animationForChild.scaleX(1.1f, new Runnable[0]);
            animationForChild.scaleY(1.1f, new Runnable[0]);
            animationForChild.start(new Runnable[0]);
        }
        updateBubblePositions();
    }

    /* access modifiers changed from: 0000 */
    public void onChildReordered(View view, int i, int i2) {
        updateBubblePositions();
    }

    private void updateBubblePositions() {
        if (!this.mAnimatingExpand && !this.mAnimatingCollapse) {
            int i = 0;
            while (i < this.mLayout.getChildCount()) {
                View childAt = this.mLayout.getChildAt(i);
                if (!childAt.equals(this.mBubbleDraggingOut)) {
                    PhysicsPropertyAnimator animationForChild = animationForChild(childAt);
                    animationForChild.translationX(getBubbleLeft(i), new Runnable[0]);
                    animationForChild.start(new Runnable[0]);
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    private float getXForChildAtIndex(int i) {
        float f = this.mBubblePaddingPx;
        return f + ((this.mBubbleSizePx + f) * ((float) i));
    }

    public float getBubbleLeft(int i) {
        return getRowLeft() + (((float) i) * (this.mBubbleSizePx + this.mBubblePaddingPx));
    }

    private float getRowLeft() {
        PhysicsAnimationLayout physicsAnimationLayout = this.mLayout;
        if (physicsAnimationLayout == null) {
            return 0.0f;
        }
        int childCount = physicsAnimationLayout.getChildCount();
        return ((float) (this.mDisplaySize.x / 2)) - (((((float) (childCount - 1)) * this.mBubblePaddingPx) + ((float) ((double) (((float) childCount) * this.mBubbleSizePx)))) / 2.0f);
    }
}
