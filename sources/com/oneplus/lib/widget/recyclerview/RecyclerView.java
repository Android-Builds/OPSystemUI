package com.oneplus.lib.widget.recyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo;
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.Scroller;
import com.oneplus.commonctrl.R$styleable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild {
    private static final boolean FORCE_INVALIDATE_DISPLAY_LIST;
    private static final Class<?>[] LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE;
    /* access modifiers changed from: private */
    public static final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float f) {
            float f2 = f - 1.0f;
            return (f2 * f2 * f2 * f2 * f2) + 1.0f;
        }
    };
    private RecyclerViewAccessibilityDelegate mAccessibilityDelegate;
    private final AccessibilityManager mAccessibilityManager;
    private OnItemTouchListener mActiveOnItemTouchListener;
    /* access modifiers changed from: private */
    public Adapter mAdapter;
    AdapterHelper mAdapterHelper;
    private boolean mAdapterUpdateDuringMeasure;
    private EdgeEffect mBottomGlow;
    private ChildDrawingOrderCallback mChildDrawingOrderCallback;
    ChildHelper mChildHelper;
    private boolean mClipToPadding;
    /* access modifiers changed from: private */
    public boolean mDataSetHasChangedAfterLayout;
    private boolean mEatRequestLayout;
    private int mEatenAccessibilityChangeFlags;
    /* access modifiers changed from: private */
    public boolean mFirstLayoutComplete;
    private boolean mIgnoreMotionEventTillDown;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private boolean mIsAttached;
    ItemAnimator mItemAnimator;
    private ItemAnimatorListener mItemAnimatorListener;
    private Runnable mItemAnimatorRunner;
    /* access modifiers changed from: private */
    public final ArrayList<ItemDecoration> mItemDecorations;
    boolean mItemsAddedOrRemoved;
    boolean mItemsChanged;
    private int mLastTouchX;
    private int mLastTouchY;
    /* access modifiers changed from: private */
    public LayoutManager mLayout;
    private boolean mLayoutFrozen;
    private int mLayoutOrScrollCounter;
    /* access modifiers changed from: private */
    public boolean mLayoutRequestEaten;
    private EdgeEffect mLeftGlow;
    private final int mMaxFlingVelocity;
    private final int mMinFlingVelocity;
    private final int[] mMinMaxLayoutPositions;
    private final int[] mNestedOffsets;
    private final RecyclerViewDataObserver mObserver;
    private List<OnChildAttachStateChangeListener> mOnChildAttachStateListeners;
    private final ArrayList<OnItemTouchListener> mOnItemTouchListeners;
    private SavedState mPendingSavedState;
    private final boolean mPostUpdatesOnAnimation;
    /* access modifiers changed from: private */
    public boolean mPostedAnimatorRunner;
    final Recycler mRecycler;
    /* access modifiers changed from: private */
    public RecyclerListener mRecyclerListener;
    private EdgeEffect mRightGlow;
    private final int[] mScrollConsumed;
    private float mScrollFactor;
    private OnScrollListener mScrollListener;
    private List<OnScrollListener> mScrollListeners;
    private final int[] mScrollOffset;
    private int mScrollPointerId;
    private int mScrollState;
    private final NestedScrollingChildHelper mScrollingChildHelper;
    final State mState;
    private final Rect mTempRect;
    private EdgeEffect mTopGlow;
    private int mTouchSlop;
    private final Runnable mUpdateChildViewsRunnable;
    private VelocityTracker mVelocityTracker;
    /* access modifiers changed from: private */
    public final ViewFlinger mViewFlinger;

    public static abstract class Adapter<VH extends ViewHolder> {
        public final void bindViewHolder(VH vh, int i) {
            throw null;
        }

        public abstract int getItemCount();

        public abstract int getItemViewType(int i);

        public final boolean hasStableIds() {
            throw null;
        }

        public abstract boolean onFailedToRecycleView(VH vh);

        public abstract void onViewAttachedToWindow(VH vh);

        public abstract void onViewDetachedFromWindow(VH vh);

        public abstract void onViewRecycled(VH vh);
    }

    public static abstract class AdapterDataObserver {
    }

    public interface ChildDrawingOrderCallback {
        int onGetChildDrawingOrder(int i, int i2);
    }

    public static abstract class ItemAnimator {
        private long mAddDuration = 120;
        private long mChangeDuration = 250;
        private ArrayList<ItemAnimatorFinishedListener> mFinishedListeners = new ArrayList<>();
        private ItemAnimatorListener mListener = null;
        private long mMoveDuration = 250;
        private long mRemoveDuration = 120;
        private boolean mSupportsChangeAnimations = true;

        public interface ItemAnimatorFinishedListener {
            void onAnimationsFinished();
        }

        interface ItemAnimatorListener {
            void onAddFinished(ViewHolder viewHolder);

            void onChangeFinished(ViewHolder viewHolder);

            void onMoveFinished(ViewHolder viewHolder);

            void onRemoveFinished(ViewHolder viewHolder);
        }

        public abstract boolean animateAdd(ViewHolder viewHolder);

        public abstract boolean animateChange(ViewHolder viewHolder, ViewHolder viewHolder2, int i, int i2, int i3, int i4);

        public abstract boolean animateMove(ViewHolder viewHolder, int i, int i2, int i3, int i4);

        public abstract boolean animateRemove(ViewHolder viewHolder);

        public abstract void endAnimation(ViewHolder viewHolder);

        public abstract void endAnimations();

        public abstract boolean isRunning();

        public void onAddFinished(ViewHolder viewHolder) {
        }

        public void onAddStarting(ViewHolder viewHolder) {
        }

        public void onChangeFinished(ViewHolder viewHolder, boolean z) {
        }

        public void onChangeStarting(ViewHolder viewHolder, boolean z) {
        }

        public void onMoveFinished(ViewHolder viewHolder) {
        }

        public void onMoveStarting(ViewHolder viewHolder) {
        }

        public void onRemoveFinished(ViewHolder viewHolder) {
        }

        public void onRemoveStarting(ViewHolder viewHolder) {
        }

        public abstract void runPendingAnimations();

        public long getMoveDuration() {
            return this.mMoveDuration;
        }

        public long getAddDuration() {
            return this.mAddDuration;
        }

        public long getRemoveDuration() {
            return this.mRemoveDuration;
        }

        public long getChangeDuration() {
            return this.mChangeDuration;
        }

        public boolean getSupportsChangeAnimations() {
            return this.mSupportsChangeAnimations;
        }

        /* access modifiers changed from: 0000 */
        public void setListener(ItemAnimatorListener itemAnimatorListener) {
            this.mListener = itemAnimatorListener;
        }

        public final void dispatchRemoveFinished(ViewHolder viewHolder) {
            onRemoveFinished(viewHolder);
            ItemAnimatorListener itemAnimatorListener = this.mListener;
            if (itemAnimatorListener != null) {
                itemAnimatorListener.onRemoveFinished(viewHolder);
            }
        }

        public final void dispatchMoveFinished(ViewHolder viewHolder) {
            onMoveFinished(viewHolder);
            ItemAnimatorListener itemAnimatorListener = this.mListener;
            if (itemAnimatorListener != null) {
                itemAnimatorListener.onMoveFinished(viewHolder);
            }
        }

        public final void dispatchAddFinished(ViewHolder viewHolder) {
            onAddFinished(viewHolder);
            ItemAnimatorListener itemAnimatorListener = this.mListener;
            if (itemAnimatorListener != null) {
                itemAnimatorListener.onAddFinished(viewHolder);
            }
        }

        public final void dispatchChangeFinished(ViewHolder viewHolder, boolean z) {
            onChangeFinished(viewHolder, z);
            ItemAnimatorListener itemAnimatorListener = this.mListener;
            if (itemAnimatorListener != null) {
                itemAnimatorListener.onChangeFinished(viewHolder);
            }
        }

        public final void dispatchRemoveStarting(ViewHolder viewHolder) {
            onRemoveStarting(viewHolder);
        }

        public final void dispatchMoveStarting(ViewHolder viewHolder) {
            onMoveStarting(viewHolder);
        }

        public final void dispatchAddStarting(ViewHolder viewHolder) {
            onAddStarting(viewHolder);
        }

        public final void dispatchChangeStarting(ViewHolder viewHolder, boolean z) {
            onChangeStarting(viewHolder, z);
        }

        public final void dispatchAnimationsFinished() {
            int size = this.mFinishedListeners.size();
            for (int i = 0; i < size; i++) {
                ((ItemAnimatorFinishedListener) this.mFinishedListeners.get(i)).onAnimationsFinished();
            }
            this.mFinishedListeners.clear();
        }
    }

    private class ItemAnimatorRestoreListener implements ItemAnimatorListener {
        private ItemAnimatorRestoreListener() {
        }

        public void onRemoveFinished(ViewHolder viewHolder) {
            viewHolder.setIsRecyclable(true);
            if (!RecyclerView.this.removeAnimatingView(viewHolder.itemView) && viewHolder.isTmpDetached()) {
                RecyclerView.this.removeDetachedView(viewHolder.itemView, false);
            }
        }

        public void onAddFinished(ViewHolder viewHolder) {
            viewHolder.setIsRecyclable(true);
            if (!viewHolder.shouldBeKeptAsChild()) {
                RecyclerView.this.removeAnimatingView(viewHolder.itemView);
            }
        }

        public void onMoveFinished(ViewHolder viewHolder) {
            viewHolder.setIsRecyclable(true);
            if (!viewHolder.shouldBeKeptAsChild()) {
                RecyclerView.this.removeAnimatingView(viewHolder.itemView);
            }
        }

        public void onChangeFinished(ViewHolder viewHolder) {
            viewHolder.setIsRecyclable(true);
            if (viewHolder.mShadowedHolder != null && viewHolder.mShadowingHolder == null) {
                viewHolder.mShadowedHolder = null;
                viewHolder.setFlags(-65, viewHolder.mFlags);
            }
            viewHolder.mShadowingHolder = null;
            if (!viewHolder.shouldBeKeptAsChild()) {
                RecyclerView.this.removeAnimatingView(viewHolder.itemView);
            }
        }
    }

    public static abstract class ItemDecoration {
        @Deprecated
        public void doDraw(Canvas canvas, RecyclerView recyclerView) {
        }

        @Deprecated
        public void onDrawOver(Canvas canvas, RecyclerView recyclerView) {
        }

        public void onDraw(Canvas canvas, RecyclerView recyclerView, State state) {
            doDraw(canvas, recyclerView);
        }

        public void onDrawOver(Canvas canvas, RecyclerView recyclerView, State state) {
            onDrawOver(canvas, recyclerView);
        }
    }

    private static class ItemHolderInfo {
        int bottom;
        ViewHolder holder;
        int left;
        int right;
        int top;

        ItemHolderInfo(ViewHolder viewHolder, int i, int i2, int i3, int i4) {
            this.holder = viewHolder;
            this.left = i;
            this.top = i2;
            this.right = i3;
            this.bottom = i4;
        }
    }

    public static abstract class LayoutManager {
        ChildHelper mChildHelper;
        private boolean mIsAttachedToWindow = false;
        RecyclerView mRecyclerView;
        /* access modifiers changed from: private */
        public boolean mRequestedSimpleAnimations = false;
        SmoothScroller mSmoothScroller;

        public boolean canScrollHorizontally() {
            return false;
        }

        public boolean canScrollVertically() {
            return false;
        }

        public boolean checkLayoutParams(LayoutParams layoutParams) {
            return layoutParams != null;
        }

        public int computeHorizontalScrollExtent(State state) {
            return 0;
        }

        public int computeHorizontalScrollOffset(State state) {
            return 0;
        }

        public int computeHorizontalScrollRange(State state) {
            return 0;
        }

        public int computeVerticalScrollExtent(State state) {
            return 0;
        }

        public int computeVerticalScrollOffset(State state) {
            return 0;
        }

        public int computeVerticalScrollRange(State state) {
            return 0;
        }

        public abstract LayoutParams generateDefaultLayoutParams();

        public int getBaseline() {
            return -1;
        }

        public int getSelectionModeForAccessibility(Recycler recycler, State state) {
            return 0;
        }

        public boolean isLayoutHierarchical(Recycler recycler, State state) {
            return false;
        }

        public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> arrayList, int i, int i2) {
            return false;
        }

        public void onAttachedToWindow(RecyclerView recyclerView) {
        }

        @Deprecated
        public void onDetachedFromWindow(RecyclerView recyclerView) {
        }

        public View onFocusSearchFailed(View view, int i, Recycler recycler, State state) {
            return null;
        }

        public View onInterceptFocusSearch(View view, int i) {
            return null;
        }

        public void onItemsAdded(RecyclerView recyclerView, int i, int i2) {
        }

        public void onItemsChanged(RecyclerView recyclerView) {
        }

        public void onItemsMoved(RecyclerView recyclerView, int i, int i2, int i3) {
        }

        public void onItemsRemoved(RecyclerView recyclerView, int i, int i2) {
        }

        public void onItemsUpdated(RecyclerView recyclerView, int i, int i2) {
        }

        public void onRestoreInstanceState(Parcelable parcelable) {
        }

        public Parcelable onSaveInstanceState() {
            return null;
        }

        public void onScrollStateChanged(int i) {
        }

        public boolean performAccessibilityActionForItem(Recycler recycler, State state, View view, int i, Bundle bundle) {
            return false;
        }

        public int scrollHorizontallyBy(int i, Recycler recycler, State state) {
            return 0;
        }

        public void scrollToPosition(int i) {
        }

        public int scrollVerticallyBy(int i, Recycler recycler, State state) {
            return 0;
        }

        public boolean supportsPredictiveItemAnimations() {
            return false;
        }

        /* access modifiers changed from: 0000 */
        public void setRecyclerView(RecyclerView recyclerView) {
            if (recyclerView == null) {
                this.mRecyclerView = null;
                this.mChildHelper = null;
                return;
            }
            this.mRecyclerView = recyclerView;
            this.mChildHelper = recyclerView.mChildHelper;
        }

        /* access modifiers changed from: 0000 */
        public void dispatchAttachedToWindow(RecyclerView recyclerView) {
            this.mIsAttachedToWindow = true;
            onAttachedToWindow(recyclerView);
        }

        /* access modifiers changed from: 0000 */
        public void dispatchDetachedFromWindow(RecyclerView recyclerView, Recycler recycler) {
            this.mIsAttachedToWindow = false;
            onDetachedFromWindow(recyclerView, recycler);
        }

        public void onDetachedFromWindow(RecyclerView recyclerView, Recycler recycler) {
            onDetachedFromWindow(recyclerView);
        }

        public void onLayoutChildren(Recycler recycler, State state) {
            Log.e("RecyclerView", "You must override onLayoutChildren(Recycler recycler, State state) ");
        }

        public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
            if (layoutParams instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) layoutParams);
            }
            if (layoutParams instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) layoutParams);
            }
            return new LayoutParams(layoutParams);
        }

        public LayoutParams generateLayoutParams(Context context, AttributeSet attributeSet) {
            return new LayoutParams(context, attributeSet);
        }

        public boolean isSmoothScrolling() {
            SmoothScroller smoothScroller = this.mSmoothScroller;
            return smoothScroller != null && smoothScroller.isRunning();
        }

        public int getLayoutDirection() {
            return this.mRecyclerView.getLayoutDirection();
        }

        public void removeView(View view) {
            this.mChildHelper.removeView(view);
        }

        public int getPosition(View view) {
            return ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        }

        public void removeAndRecycleView(View view, Recycler recycler) {
            removeView(view);
            recycler.recycleView(view);
        }

        public int getWidth() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getWidth();
            }
            return 0;
        }

        public int getHeight() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getHeight();
            }
            return 0;
        }

        public int getPaddingLeft() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getPaddingLeft();
            }
            return 0;
        }

        public int getPaddingTop() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getPaddingTop();
            }
            return 0;
        }

        public int getPaddingRight() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getPaddingRight();
            }
            return 0;
        }

        public int getPaddingBottom() {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null) {
                return recyclerView.getPaddingBottom();
            }
            return 0;
        }

        /* access modifiers changed from: 0000 */
        public void removeAndRecycleScrapInt(Recycler recycler) {
            int scrapCount = recycler.getScrapCount();
            for (int i = scrapCount - 1; i >= 0; i--) {
                View scrapViewAt = recycler.getScrapViewAt(i);
                ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(scrapViewAt);
                if (!childViewHolderInt.shouldIgnore()) {
                    childViewHolderInt.setIsRecyclable(false);
                    if (childViewHolderInt.isTmpDetached()) {
                        this.mRecyclerView.removeDetachedView(scrapViewAt, false);
                    }
                    ItemAnimator itemAnimator = this.mRecyclerView.mItemAnimator;
                    if (itemAnimator != null) {
                        itemAnimator.endAnimation(childViewHolderInt);
                    }
                    childViewHolderInt.setIsRecyclable(true);
                    recycler.quickRecycleScrapView(scrapViewAt);
                }
            }
            recycler.clearScrap();
            if (scrapCount > 0) {
                this.mRecyclerView.invalidate();
            }
        }

        public boolean requestChildRectangleOnScreen(RecyclerView recyclerView, View view, Rect rect, boolean z) {
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int width = getWidth() - getPaddingRight();
            int height = getHeight() - getPaddingBottom();
            int left = view.getLeft() + rect.left;
            int top = view.getTop() + rect.top;
            int width2 = rect.width() + left;
            int height2 = rect.height() + top;
            int i = left - paddingLeft;
            int min = Math.min(0, i);
            int i2 = top - paddingTop;
            int min2 = Math.min(0, i2);
            int i3 = width2 - width;
            int max = Math.max(0, i3);
            int max2 = Math.max(0, height2 - height);
            if (getLayoutDirection() != 1) {
                if (min == 0) {
                    min = Math.min(i, max);
                }
                max = min;
            } else if (max == 0) {
                max = Math.max(min, i3);
            }
            if (min2 == 0) {
                min2 = Math.min(i2, max2);
            }
            if (max == 0 && min2 == 0) {
                return false;
            }
            if (z) {
                recyclerView.scrollBy(max, min2);
            } else {
                recyclerView.smoothScrollBy(max, min2);
            }
            return true;
        }

        @Deprecated
        public boolean onRequestChildFocus(RecyclerView recyclerView, View view, View view2) {
            return isSmoothScrolling() || recyclerView.isComputingLayout();
        }

        public boolean onRequestChildFocus(RecyclerView recyclerView, State state, View view, View view2) {
            return onRequestChildFocus(recyclerView, view, view2);
        }

        public void onItemsUpdated(RecyclerView recyclerView, int i, int i2, Object obj) {
            onItemsUpdated(recyclerView, i, i2);
        }

        public void doMeasure(Recycler recycler, State state, int i, int i2) {
            this.mRecyclerView.defaultOnMeasure(i, i2);
        }

        /* access modifiers changed from: 0000 */
        public void stopSmoothScroller() {
            SmoothScroller smoothScroller = this.mSmoothScroller;
            if (smoothScroller != null) {
                smoothScroller.stop();
            }
        }

        /* access modifiers changed from: 0000 */
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
            RecyclerView recyclerView = this.mRecyclerView;
            onInitializeAccessibilityNodeInfo(recyclerView.mRecycler, recyclerView.mState, accessibilityNodeInfo);
        }

        public void onInitializeAccessibilityNodeInfo(Recycler recycler, State state, AccessibilityNodeInfo accessibilityNodeInfo) {
            if (this.mRecyclerView.canScrollVertically(-1) || this.mRecyclerView.canScrollHorizontally(-1)) {
                accessibilityNodeInfo.addAction(8192);
                accessibilityNodeInfo.setScrollable(true);
            }
            if (this.mRecyclerView.canScrollVertically(1) || this.mRecyclerView.canScrollHorizontally(1)) {
                accessibilityNodeInfo.addAction(4096);
                accessibilityNodeInfo.setScrollable(true);
            }
            accessibilityNodeInfo.setCollectionInfo(CollectionInfo.obtain(getRowCountForAccessibility(recycler, state), getColumnCountForAccessibility(recycler, state), isLayoutHierarchical(recycler, state), getSelectionModeForAccessibility(recycler, state)));
        }

        public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
            RecyclerView recyclerView = this.mRecyclerView;
            onInitializeAccessibilityEvent(recyclerView.mRecycler, recyclerView.mState, accessibilityEvent);
        }

        public void onInitializeAccessibilityEvent(Recycler recycler, State state, AccessibilityEvent accessibilityEvent) {
            AccessibilityEvent obtain = AccessibilityEvent.obtain(accessibilityEvent);
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView != null && obtain != null) {
                boolean z = true;
                if (!recyclerView.canScrollVertically(1) && !this.mRecyclerView.canScrollVertically(-1) && !this.mRecyclerView.canScrollHorizontally(-1) && !this.mRecyclerView.canScrollHorizontally(1)) {
                    z = false;
                }
                obtain.setScrollable(z);
                if (this.mRecyclerView.mAdapter != null) {
                    obtain.setItemCount(this.mRecyclerView.mAdapter.getItemCount());
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void onInitializeAccessibilityNodeInfoForItem(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
            if (childViewHolderInt != null && !childViewHolderInt.isRemoved() && !this.mChildHelper.isHidden(childViewHolderInt.itemView)) {
                RecyclerView recyclerView = this.mRecyclerView;
                onInitializeAccessibilityNodeInfoForItem(recyclerView.mRecycler, recyclerView.mState, view, accessibilityNodeInfo);
            }
        }

        public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler, State state, View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            int i = 0;
            int position = canScrollVertically() ? getPosition(view) : 0;
            if (canScrollHorizontally()) {
                i = getPosition(view);
            }
            accessibilityNodeInfo.setCollectionItemInfo(CollectionItemInfo.obtain(position, 1, i, 1, false, false));
        }

        public int getRowCountForAccessibility(Recycler recycler, State state) {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView == null || recyclerView.mAdapter == null || !canScrollVertically()) {
                return 1;
            }
            return this.mRecyclerView.mAdapter.getItemCount();
        }

        public int getColumnCountForAccessibility(Recycler recycler, State state) {
            RecyclerView recyclerView = this.mRecyclerView;
            if (recyclerView == null || recyclerView.mAdapter == null || !canScrollHorizontally()) {
                return 1;
            }
            return this.mRecyclerView.mAdapter.getItemCount();
        }

        /* access modifiers changed from: 0000 */
        public boolean performAccessibilityAction(int i, Bundle bundle) {
            RecyclerView recyclerView = this.mRecyclerView;
            return performAccessibilityAction(recyclerView.mRecycler, recyclerView.mState, i, bundle);
        }

        /* JADX WARNING: Removed duplicated region for block: B:24:0x0072 A[ADDED_TO_REGION] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean performAccessibilityAction(com.oneplus.lib.widget.recyclerview.RecyclerView.Recycler r2, com.oneplus.lib.widget.recyclerview.RecyclerView.State r3, int r4, android.os.Bundle r5) {
            /*
                r1 = this;
                com.oneplus.lib.widget.recyclerview.RecyclerView r2 = r1.mRecyclerView
                r3 = 0
                if (r2 != 0) goto L_0x0006
                return r3
            L_0x0006:
                r5 = 4096(0x1000, float:5.74E-42)
                r0 = 1
                if (r4 == r5) goto L_0x0042
                r5 = 8192(0x2000, float:1.14794E-41)
                if (r4 == r5) goto L_0x0012
                r2 = r3
                r4 = r2
                goto L_0x0070
            L_0x0012:
                r4 = -1
                boolean r2 = r2.canScrollVertically(r4)
                if (r2 == 0) goto L_0x0029
                int r2 = r1.getHeight()
                int r5 = r1.getPaddingTop()
                int r2 = r2 - r5
                int r5 = r1.getPaddingBottom()
                int r2 = r2 - r5
                int r2 = -r2
                goto L_0x002a
            L_0x0029:
                r2 = r3
            L_0x002a:
                com.oneplus.lib.widget.recyclerview.RecyclerView r5 = r1.mRecyclerView
                boolean r4 = r5.canScrollHorizontally(r4)
                if (r4 == 0) goto L_0x006f
                int r4 = r1.getWidth()
                int r5 = r1.getPaddingLeft()
                int r4 = r4 - r5
                int r5 = r1.getPaddingRight()
                int r4 = r4 - r5
                int r4 = -r4
                goto L_0x0070
            L_0x0042:
                boolean r2 = r2.canScrollVertically(r0)
                if (r2 == 0) goto L_0x0057
                int r2 = r1.getHeight()
                int r4 = r1.getPaddingTop()
                int r2 = r2 - r4
                int r4 = r1.getPaddingBottom()
                int r2 = r2 - r4
                goto L_0x0058
            L_0x0057:
                r2 = r3
            L_0x0058:
                com.oneplus.lib.widget.recyclerview.RecyclerView r4 = r1.mRecyclerView
                boolean r4 = r4.canScrollHorizontally(r0)
                if (r4 == 0) goto L_0x006f
                int r4 = r1.getWidth()
                int r5 = r1.getPaddingLeft()
                int r4 = r4 - r5
                int r5 = r1.getPaddingRight()
                int r4 = r4 - r5
                goto L_0x0070
            L_0x006f:
                r4 = r3
            L_0x0070:
                if (r2 != 0) goto L_0x0075
                if (r4 != 0) goto L_0x0075
                return r3
            L_0x0075:
                com.oneplus.lib.widget.recyclerview.RecyclerView r1 = r1.mRecyclerView
                r1.scrollBy(r4, r2)
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.LayoutManager.performAccessibilityAction(com.oneplus.lib.widget.recyclerview.RecyclerView$Recycler, com.oneplus.lib.widget.recyclerview.RecyclerView$State, int, android.os.Bundle):boolean");
        }

        /* access modifiers changed from: 0000 */
        public boolean performAccessibilityActionForItem(View view, int i, Bundle bundle) {
            RecyclerView recyclerView = this.mRecyclerView;
            return performAccessibilityActionForItem(recyclerView.mRecycler, recyclerView.mState, view, i, bundle);
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        final Rect mDecorInsets = new Rect();
        boolean mInsetsDirty = true;
        boolean mPendingInvalidate = false;
        ViewHolder mViewHolder;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public LayoutParams(LayoutParams layoutParams) {
            super(layoutParams);
        }

        public int getViewLayoutPosition() {
            return this.mViewHolder.getLayoutPosition();
        }
    }

    public interface OnChildAttachStateChangeListener {
        void onChildViewAttachedToWindow(View view);

        void onChildViewDetachedFromWindow(View view);
    }

    public interface OnItemTouchListener {
        boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent);

        void onRequestDisallowInterceptTouchEvent(boolean z);

        void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent);
    }

    public static abstract class OnScrollListener {
        public void onScrollStateChanged(RecyclerView recyclerView, int i) {
        }

        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
        }
    }

    public static class RecycledViewPool {
        private int mAttachCount = 0;
        private SparseIntArray mMaxScrap = new SparseIntArray();
        private SparseArray<ArrayList<ViewHolder>> mScrap = new SparseArray<>();

        public void putRecycledView(ViewHolder viewHolder) {
            int itemViewType = viewHolder.getItemViewType();
            ArrayList scrapHeapForType = getScrapHeapForType(itemViewType);
            if (this.mMaxScrap.get(itemViewType) > scrapHeapForType.size()) {
                viewHolder.resetInternal();
                scrapHeapForType.add(viewHolder);
            }
        }

        private ArrayList<ViewHolder> getScrapHeapForType(int i) {
            ArrayList<ViewHolder> arrayList = (ArrayList) this.mScrap.get(i);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mScrap.put(i, arrayList);
                if (this.mMaxScrap.indexOfKey(i) < 0) {
                    this.mMaxScrap.put(i, 5);
                }
            }
            return arrayList;
        }
    }

    public final class Recycler {
        boolean accessibilityDelegateCheckFailed = false;
        final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
        final ArrayList<ViewHolder> mCachedViews = new ArrayList<>();
        /* access modifiers changed from: private */
        public ArrayList<ViewHolder> mChangedScrap = null;
        private RecycledViewPool mRecyclerPool;
        private final List<ViewHolder> mUnmodifiableAttachedScrap = Collections.unmodifiableList(this.mAttachedScrap);
        private int mViewCacheMax = 2;

        public Recycler() {
        }

        public void clear() {
            this.mAttachedScrap.clear();
            recycleAndClearCachedViews();
        }

        public void recycleView(View view) {
            ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
            if (childViewHolderInt.isTmpDetached()) {
                RecyclerView.this.removeDetachedView(view, false);
            }
            if (childViewHolderInt.isScrap()) {
                childViewHolderInt.unScrap();
            } else if (childViewHolderInt.wasReturnedFromScrap()) {
                childViewHolderInt.clearReturnedFromScrapFlag();
            }
            recycleViewHolderInternal(childViewHolderInt);
        }

        /* access modifiers changed from: 0000 */
        public void recycleAndClearCachedViews() {
            for (int size = this.mCachedViews.size() - 1; size >= 0; size--) {
                recycleCachedViewAt(size);
            }
            this.mCachedViews.clear();
        }

        /* access modifiers changed from: 0000 */
        public void recycleCachedViewAt(int i) {
            addViewHolderToRecycledViewPool((ViewHolder) this.mCachedViews.get(i));
            this.mCachedViews.remove(i);
        }

        /* access modifiers changed from: 0000 */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x006b  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void recycleViewHolderInternal(com.oneplus.lib.widget.recyclerview.RecyclerView.ViewHolder r6) {
            /*
                r5 = this;
                boolean r0 = r6.isScrap()
                r1 = 1
                r2 = 0
                if (r0 != 0) goto L_0x009f
                android.view.View r0 = r6.itemView
                android.view.ViewParent r0 = r0.getParent()
                if (r0 == 0) goto L_0x0012
                goto L_0x009f
            L_0x0012:
                boolean r0 = r6.isTmpDetached()
                if (r0 != 0) goto L_0x0088
                boolean r0 = r6.shouldIgnore()
                if (r0 != 0) goto L_0x0080
                boolean r0 = r6.doesTransientStatePreventRecycling()
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$Adapter r3 = r3.mAdapter
                if (r3 == 0) goto L_0x003a
                if (r0 == 0) goto L_0x003a
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$Adapter r3 = r3.mAdapter
                boolean r3 = r3.onFailedToRecycleView(r6)
                if (r3 == 0) goto L_0x003a
                r3 = r1
                goto L_0x003b
            L_0x003a:
                r3 = r2
            L_0x003b:
                if (r3 != 0) goto L_0x0046
                boolean r3 = r6.isRecyclable()
                if (r3 == 0) goto L_0x0044
                goto L_0x0046
            L_0x0044:
                r3 = r2
                goto L_0x006f
            L_0x0046:
                r3 = 78
                boolean r3 = r6.hasAnyOfTheFlags(r3)
                if (r3 != 0) goto L_0x0068
                java.util.ArrayList<com.oneplus.lib.widget.recyclerview.RecyclerView$ViewHolder> r3 = r5.mCachedViews
                int r3 = r3.size()
                int r4 = r5.mViewCacheMax
                if (r3 != r4) goto L_0x005d
                if (r3 <= 0) goto L_0x005d
                r5.recycleCachedViewAt(r2)
            L_0x005d:
                int r4 = r5.mViewCacheMax
                if (r3 >= r4) goto L_0x0068
                java.util.ArrayList<com.oneplus.lib.widget.recyclerview.RecyclerView$ViewHolder> r3 = r5.mCachedViews
                r3.add(r6)
                r3 = r1
                goto L_0x0069
            L_0x0068:
                r3 = r2
            L_0x0069:
                if (r3 != 0) goto L_0x006f
                r5.addViewHolderToRecycledViewPool(r6)
                r2 = r1
            L_0x006f:
                com.oneplus.lib.widget.recyclerview.RecyclerView r5 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$State r5 = r5.mState
                r5.onViewRecycled(r6)
                if (r3 != 0) goto L_0x007f
                if (r2 != 0) goto L_0x007f
                if (r0 == 0) goto L_0x007f
                r5 = 0
                r6.mOwnerRecyclerView = r5
            L_0x007f:
                return
            L_0x0080:
                java.lang.IllegalArgumentException r5 = new java.lang.IllegalArgumentException
                java.lang.String r6 = "Trying to recycle an ignored view holder. You should first call stopIgnoringView(view) before calling recycle."
                r5.<init>(r6)
                throw r5
            L_0x0088:
                java.lang.IllegalArgumentException r5 = new java.lang.IllegalArgumentException
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "Tmp detached view should be removed from RecyclerView before it can be recycled: "
                r0.append(r1)
                r0.append(r6)
                java.lang.String r6 = r0.toString()
                r5.<init>(r6)
                throw r5
            L_0x009f:
                java.lang.IllegalArgumentException r5 = new java.lang.IllegalArgumentException
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "Scrapped or attached views may not be recycled. isScrap:"
                r0.append(r3)
                boolean r3 = r6.isScrap()
                r0.append(r3)
                java.lang.String r3 = " isAttached:"
                r0.append(r3)
                android.view.View r6 = r6.itemView
                android.view.ViewParent r6 = r6.getParent()
                if (r6 == 0) goto L_0x00c0
                goto L_0x00c1
            L_0x00c0:
                r1 = r2
            L_0x00c1:
                r0.append(r1)
                java.lang.String r6 = r0.toString()
                r5.<init>(r6)
                throw r5
            */
            throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.Recycler.recycleViewHolderInternal(com.oneplus.lib.widget.recyclerview.RecyclerView$ViewHolder):void");
        }

        /* access modifiers changed from: 0000 */
        public void addViewHolderToRecycledViewPool(ViewHolder viewHolder) {
            viewHolder.itemView.setAccessibilityDelegate(null);
            dispatchViewRecycled(viewHolder);
            viewHolder.mOwnerRecyclerView = null;
            getRecycledViewPool().putRecycledView(viewHolder);
        }

        /* access modifiers changed from: 0000 */
        public void quickRecycleScrapView(View view) {
            ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
            childViewHolderInt.mScrapContainer = null;
            childViewHolderInt.clearReturnedFromScrapFlag();
            recycleViewHolderInternal(childViewHolderInt);
        }

        /* access modifiers changed from: 0000 */
        public void unscrapView(ViewHolder viewHolder) {
            if (viewHolder.isChanged() && RecyclerView.this.supportsChangeAnimations()) {
                ArrayList<ViewHolder> arrayList = this.mChangedScrap;
                if (arrayList != null) {
                    arrayList.remove(viewHolder);
                    viewHolder.mScrapContainer = null;
                    viewHolder.clearReturnedFromScrapFlag();
                }
            }
            this.mAttachedScrap.remove(viewHolder);
            viewHolder.mScrapContainer = null;
            viewHolder.clearReturnedFromScrapFlag();
        }

        /* access modifiers changed from: 0000 */
        public int getScrapCount() {
            return this.mAttachedScrap.size();
        }

        /* access modifiers changed from: 0000 */
        public View getScrapViewAt(int i) {
            return ((ViewHolder) this.mAttachedScrap.get(i)).itemView;
        }

        /* access modifiers changed from: 0000 */
        public void clearScrap() {
            this.mAttachedScrap.clear();
        }

        /* access modifiers changed from: 0000 */
        public void dispatchViewRecycled(ViewHolder viewHolder) {
            if (RecyclerView.this.mRecyclerListener != null) {
                RecyclerView.this.mRecyclerListener.onViewRecycled(viewHolder);
            }
            if (RecyclerView.this.mAdapter != null) {
                RecyclerView.this.mAdapter.onViewRecycled(viewHolder);
            }
            State state = RecyclerView.this.mState;
            if (state != null) {
                state.onViewRecycled(viewHolder);
            }
        }

        /* access modifiers changed from: 0000 */
        public void offsetPositionRecordsForMove(int i, int i2) {
            int i3;
            int i4;
            int i5;
            if (i < i2) {
                i4 = i2;
                i3 = -1;
                i5 = i;
            } else {
                i4 = i;
                i3 = 1;
                i5 = i2;
            }
            int size = this.mCachedViews.size();
            for (int i6 = 0; i6 < size; i6++) {
                ViewHolder viewHolder = (ViewHolder) this.mCachedViews.get(i6);
                if (viewHolder != null) {
                    int i7 = viewHolder.mPosition;
                    if (i7 >= i5 && i7 <= i4) {
                        if (i7 == i) {
                            viewHolder.offsetPosition(i2 - i, false);
                        } else {
                            viewHolder.offsetPosition(i3, false);
                        }
                    }
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void offsetPositionRecordsForInsert(int i, int i2) {
            int size = this.mCachedViews.size();
            for (int i3 = 0; i3 < size; i3++) {
                ViewHolder viewHolder = (ViewHolder) this.mCachedViews.get(i3);
                if (viewHolder != null && viewHolder.getLayoutPosition() >= i) {
                    viewHolder.offsetPosition(i2, true);
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void offsetPositionRecordsForRemove(int i, int i2, boolean z) {
            int i3 = i + i2;
            for (int size = this.mCachedViews.size() - 1; size >= 0; size--) {
                ViewHolder viewHolder = (ViewHolder) this.mCachedViews.get(size);
                if (viewHolder != null) {
                    if (viewHolder.getLayoutPosition() >= i3) {
                        viewHolder.offsetPosition(-i2, z);
                    } else if (viewHolder.getLayoutPosition() >= i) {
                        viewHolder.addFlags(8);
                        recycleCachedViewAt(size);
                    }
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public RecycledViewPool getRecycledViewPool() {
            if (this.mRecyclerPool == null) {
                this.mRecyclerPool = new RecycledViewPool();
            }
            return this.mRecyclerPool;
        }

        /* access modifiers changed from: 0000 */
        public void viewRangeUpdate(int i, int i2) {
            int i3 = i2 + i;
            for (int size = this.mCachedViews.size() - 1; size >= 0; size--) {
                ViewHolder viewHolder = (ViewHolder) this.mCachedViews.get(size);
                if (viewHolder != null) {
                    int layoutPosition = viewHolder.getLayoutPosition();
                    if (layoutPosition >= i && layoutPosition < i3) {
                        viewHolder.addFlags(2);
                        recycleCachedViewAt(size);
                    }
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void markKnownViewsInvalid() {
            if (RecyclerView.this.mAdapter == null || !RecyclerView.this.mAdapter.hasStableIds()) {
                recycleAndClearCachedViews();
                return;
            }
            int size = this.mCachedViews.size();
            for (int i = 0; i < size; i++) {
                ViewHolder viewHolder = (ViewHolder) this.mCachedViews.get(i);
                if (viewHolder != null) {
                    viewHolder.addFlags(6);
                    viewHolder.addChangePayload(null);
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void clearOldPositions() {
            int size = this.mCachedViews.size();
            for (int i = 0; i < size; i++) {
                ((ViewHolder) this.mCachedViews.get(i)).clearOldPosition();
            }
            int size2 = this.mAttachedScrap.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((ViewHolder) this.mAttachedScrap.get(i2)).clearOldPosition();
            }
            ArrayList<ViewHolder> arrayList = this.mChangedScrap;
            if (arrayList != null) {
                int size3 = arrayList.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    ((ViewHolder) this.mChangedScrap.get(i3)).clearOldPosition();
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public void markItemDecorInsetsDirty() {
            int size = this.mCachedViews.size();
            for (int i = 0; i < size; i++) {
                LayoutParams layoutParams = (LayoutParams) ((ViewHolder) this.mCachedViews.get(i)).itemView.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.mInsetsDirty = true;
                }
            }
        }
    }

    public interface RecyclerListener {
        void onViewRecycled(ViewHolder viewHolder);
    }

    private class RecyclerViewDataObserver extends AdapterDataObserver {
        private RecyclerViewDataObserver() {
        }
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        Parcelable mLayoutState;

        SavedState(Parcel parcel) {
            super(parcel);
            this.mLayoutState = parcel.readParcelable(LayoutManager.class.getClassLoader());
        }

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeParcelable(this.mLayoutState, 0);
        }

        /* access modifiers changed from: private */
        public void copyFrom(SavedState savedState) {
            this.mLayoutState = savedState.mLayoutState;
        }
    }

    public static abstract class SmoothScroller {
        private boolean mPendingInitialRun;
        private RecyclerView mRecyclerView;
        private final Action mRecyclingAction;
        private boolean mRunning;
        private int mTargetPosition;
        private View mTargetView;

        public static class Action {
            private boolean changed;
            private int consecutiveUpdates;
            private int mDuration;
            private int mDx;
            private int mDy;
            private Interpolator mInterpolator;
            private int mJumpToPosition;

            /* access modifiers changed from: 0000 */
            public boolean hasJumpTarget() {
                throw null;
            }

            /* access modifiers changed from: private */
            public void runIfNecessary(RecyclerView recyclerView) {
                int i = this.mJumpToPosition;
                if (i >= 0) {
                    this.mJumpToPosition = -1;
                    recyclerView.jumpToPositionForSmoothScroller(i);
                    this.changed = false;
                    return;
                }
                if (this.changed) {
                    validate();
                    if (this.mInterpolator != null) {
                        recyclerView.mViewFlinger.smoothScrollBy(this.mDx, this.mDy, this.mDuration, this.mInterpolator);
                    } else if (this.mDuration == Integer.MIN_VALUE) {
                        recyclerView.mViewFlinger.smoothScrollBy(this.mDx, this.mDy);
                    } else {
                        recyclerView.mViewFlinger.smoothScrollBy(this.mDx, this.mDy, this.mDuration);
                    }
                    this.consecutiveUpdates++;
                    if (this.consecutiveUpdates > 10) {
                        Log.e("RecyclerView", "Smooth Scroll action is being updated too frequently. Make sure you are not changing it unless necessary");
                    }
                    this.changed = false;
                } else {
                    this.consecutiveUpdates = 0;
                }
            }

            private void validate() {
                if (this.mInterpolator != null && this.mDuration < 1) {
                    throw new IllegalStateException("If you provide an interpolator, you must set a positive duration");
                } else if (this.mDuration < 1) {
                    throw new IllegalStateException("Scroll duration must be a positive number");
                }
            }
        }

        public abstract int getChildPosition(View view);

        public abstract int getTargetPosition();

        public abstract boolean isPendingInitialRun();

        public abstract boolean isRunning();

        /* access modifiers changed from: protected */
        public abstract void onSeekTargetStep(int i, int i2, State state, Action action);

        /* access modifiers changed from: protected */
        public abstract void onTargetFound(View view, State state, Action action);

        public abstract void setTargetPosition(int i);

        /* access modifiers changed from: protected */
        public final void stop() {
            throw null;
        }

        /* access modifiers changed from: private */
        public void onAnimation(int i, int i2) {
            RecyclerView recyclerView = this.mRecyclerView;
            if (!this.mRunning || this.mTargetPosition == -1 || recyclerView == null) {
                stop();
            }
            this.mPendingInitialRun = false;
            View view = this.mTargetView;
            if (view != null) {
                if (getChildPosition(view) == this.mTargetPosition) {
                    onTargetFound(this.mTargetView, recyclerView.mState, this.mRecyclingAction);
                    this.mRecyclingAction.runIfNecessary(recyclerView);
                    stop();
                } else {
                    Log.e("RecyclerView", "Passed over target position while smooth scrolling.");
                    this.mTargetView = null;
                }
            }
            if (this.mRunning) {
                onSeekTargetStep(i, i2, recyclerView.mState, this.mRecyclingAction);
                this.mRecyclingAction.hasJumpTarget();
                throw null;
            }
        }
    }

    public static class State {
        private SparseArray<Object> mData;
        /* access modifiers changed from: private */
        public int mDeletedInvisibleItemCountSincePreviousLayout = 0;
        final List<View> mDisappearingViewsInLayoutPass = new ArrayList();
        /* access modifiers changed from: private */
        public boolean mInPreLayout = false;
        int mItemCount = 0;
        ArrayMap<Long, ViewHolder> mOldChangedHolders = new ArrayMap<>();
        ArrayMap<ViewHolder, ItemHolderInfo> mPostLayoutHolderMap = new ArrayMap<>();
        ArrayMap<ViewHolder, ItemHolderInfo> mPreLayoutHolderMap = new ArrayMap<>();
        /* access modifiers changed from: private */
        public int mPreviousLayoutItemCount = 0;
        /* access modifiers changed from: private */
        public boolean mRunPredictiveAnimations = false;
        /* access modifiers changed from: private */
        public boolean mRunSimpleAnimations = false;
        /* access modifiers changed from: private */
        public boolean mStructureChanged = false;
        private int mTargetPosition = -1;

        static /* synthetic */ int access$1212(State state, int i) {
            int i2 = state.mDeletedInvisibleItemCountSincePreviousLayout + i;
            state.mDeletedInvisibleItemCountSincePreviousLayout = i2;
            return i2;
        }

        public boolean isPreLayout() {
            return this.mInPreLayout;
        }

        public int getItemCount() {
            if (this.mInPreLayout) {
                return this.mPreviousLayoutItemCount - this.mDeletedInvisibleItemCountSincePreviousLayout;
            }
            return this.mItemCount;
        }

        /* access modifiers changed from: 0000 */
        public void onViewRecycled(ViewHolder viewHolder) {
            this.mPreLayoutHolderMap.remove(viewHolder);
            this.mPostLayoutHolderMap.remove(viewHolder);
            ArrayMap<Long, ViewHolder> arrayMap = this.mOldChangedHolders;
            if (arrayMap != null) {
                removeFrom(arrayMap, viewHolder);
            }
            this.mDisappearingViewsInLayoutPass.remove(viewHolder.itemView);
        }

        private void removeFrom(ArrayMap<Long, ViewHolder> arrayMap, ViewHolder viewHolder) {
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                if (viewHolder == arrayMap.valueAt(size)) {
                    arrayMap.removeAt(size);
                    return;
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("State{mTargetPosition=");
            sb.append(this.mTargetPosition);
            sb.append(", mPreLayoutHolderMap=");
            sb.append(this.mPreLayoutHolderMap);
            sb.append(", mPostLayoutHolderMap=");
            sb.append(this.mPostLayoutHolderMap);
            sb.append(", mData=");
            sb.append(this.mData);
            sb.append(", mItemCount=");
            sb.append(this.mItemCount);
            sb.append(", mPreviousLayoutItemCount=");
            sb.append(this.mPreviousLayoutItemCount);
            sb.append(", mDeletedInvisibleItemCountSincePreviousLayout=");
            sb.append(this.mDeletedInvisibleItemCountSincePreviousLayout);
            sb.append(", mStructureChanged=");
            sb.append(this.mStructureChanged);
            sb.append(", mInPreLayout=");
            sb.append(this.mInPreLayout);
            sb.append(", mRunSimpleAnimations=");
            sb.append(this.mRunSimpleAnimations);
            sb.append(", mRunPredictiveAnimations=");
            sb.append(this.mRunPredictiveAnimations);
            sb.append('}');
            return sb.toString();
        }
    }

    private class ViewFlinger implements Runnable {
        private boolean mEatRunOnAnimationRequest = false;
        private Interpolator mInterpolator = RecyclerView.sQuinticInterpolator;
        private int mLastFlingX;
        private int mLastFlingY;
        private boolean mReSchedulePostAnimationCallback = false;
        private Scroller mScroller;

        public ViewFlinger() {
            this.mScroller = new Scroller(RecyclerView.this.getContext(), RecyclerView.sQuinticInterpolator);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:61:0x015c, code lost:
            if (r12 > 0) goto L_0x0160;
         */
        /* JADX WARNING: Removed duplicated region for block: B:105:0x01e1  */
        /* JADX WARNING: Removed duplicated region for block: B:59:0x0158  */
        /* JADX WARNING: Removed duplicated region for block: B:65:0x0168  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
                r19 = this;
                r0 = r19
                r19.disableRunOnAnimationRequests()
                com.oneplus.lib.widget.recyclerview.RecyclerView r1 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r1.consumePendingUpdateOperations()
                android.widget.Scroller r1 = r0.mScroller
                com.oneplus.lib.widget.recyclerview.RecyclerView r2 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r2 = r2.mLayout
                com.oneplus.lib.widget.recyclerview.RecyclerView$SmoothScroller r2 = r2.mSmoothScroller
                boolean r3 = r1.computeScrollOffset()
                if (r3 == 0) goto L_0x01de
                int r3 = r1.getCurrX()
                int r5 = r1.getCurrY()
                int r6 = r0.mLastFlingX
                int r6 = r3 - r6
                int r7 = r0.mLastFlingY
                int r7 = r5 - r7
                r0.mLastFlingX = r3
                r0.mLastFlingY = r5
                com.oneplus.lib.widget.recyclerview.RecyclerView r8 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$Adapter r8 = r8.mAdapter
                if (r8 == 0) goto L_0x011e
                com.oneplus.lib.widget.recyclerview.RecyclerView r8 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r8.eatRequestLayout()
                com.oneplus.lib.widget.recyclerview.RecyclerView r8 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r8.onEnterLayoutOrScroll()
                java.lang.String r8 = "RV Scroll"
                android.os.Trace.beginSection(r8)
                if (r6 == 0) goto L_0x005a
                com.oneplus.lib.widget.recyclerview.RecyclerView r8 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r8 = r8.mLayout
                com.oneplus.lib.widget.recyclerview.RecyclerView r10 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$Recycler r11 = r10.mRecycler
                com.oneplus.lib.widget.recyclerview.RecyclerView$State r10 = r10.mState
                int r8 = r8.scrollHorizontallyBy(r6, r11, r10)
                int r10 = r6 - r8
                goto L_0x005c
            L_0x005a:
                r8 = 0
                r10 = 0
            L_0x005c:
                if (r7 == 0) goto L_0x0071
                com.oneplus.lib.widget.recyclerview.RecyclerView r11 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r11 = r11.mLayout
                com.oneplus.lib.widget.recyclerview.RecyclerView r12 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$Recycler r13 = r12.mRecycler
                com.oneplus.lib.widget.recyclerview.RecyclerView$State r12 = r12.mState
                int r11 = r11.scrollVerticallyBy(r7, r13, r12)
                int r12 = r7 - r11
                goto L_0x0073
            L_0x0071:
                r11 = 0
                r12 = 0
            L_0x0073:
                android.os.Trace.endSection()
                com.oneplus.lib.widget.recyclerview.RecyclerView r13 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                boolean r13 = r13.supportsChangeAnimations()
                if (r13 == 0) goto L_0x00d1
                com.oneplus.lib.widget.recyclerview.RecyclerView r13 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.ChildHelper r13 = r13.mChildHelper
                int r13 = r13.getChildCount()
                r14 = 0
            L_0x0087:
                if (r14 >= r13) goto L_0x00d1
                com.oneplus.lib.widget.recyclerview.RecyclerView r15 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.ChildHelper r15 = r15.mChildHelper
                android.view.View r15 = r15.getChildAt(r14)
                com.oneplus.lib.widget.recyclerview.RecyclerView r9 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$ViewHolder r9 = r9.getChildViewHolder(r15)
                if (r9 == 0) goto L_0x00c6
                com.oneplus.lib.widget.recyclerview.RecyclerView$ViewHolder r9 = r9.mShadowingHolder
                if (r9 == 0) goto L_0x00c6
                android.view.View r9 = r9.itemView
                int r4 = r15.getLeft()
                int r15 = r15.getTop()
                r16 = r8
                int r8 = r9.getLeft()
                if (r4 != r8) goto L_0x00b5
                int r8 = r9.getTop()
                if (r15 == r8) goto L_0x00c8
            L_0x00b5:
                int r8 = r9.getWidth()
                int r8 = r8 + r4
                int r17 = r9.getHeight()
                r18 = r11
                int r11 = r15 + r17
                r9.layout(r4, r15, r8, r11)
                goto L_0x00ca
            L_0x00c6:
                r16 = r8
            L_0x00c8:
                r18 = r11
            L_0x00ca:
                int r14 = r14 + 1
                r8 = r16
                r11 = r18
                goto L_0x0087
            L_0x00d1:
                r16 = r8
                r18 = r11
                com.oneplus.lib.widget.recyclerview.RecyclerView r4 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r4.onExitLayoutOrScroll()
                com.oneplus.lib.widget.recyclerview.RecyclerView r4 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r8 = 0
                r4.resumeRequestLayout(r8)
                if (r2 == 0) goto L_0x0118
                boolean r4 = r2.isPendingInitialRun()
                if (r4 != 0) goto L_0x0118
                boolean r4 = r2.isRunning()
                if (r4 == 0) goto L_0x0118
                com.oneplus.lib.widget.recyclerview.RecyclerView r4 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$State r4 = r4.mState
                int r4 = r4.getItemCount()
                if (r4 != 0) goto L_0x00fc
                r2.stop()
                goto L_0x0118
            L_0x00fc:
                int r8 = r2.getTargetPosition()
                if (r8 < r4) goto L_0x010f
                r8 = 1
                int r4 = r4 - r8
                r2.setTargetPosition(r4)
                int r4 = r6 - r10
                int r9 = r7 - r12
                r2.onAnimation(r4, r9)
                goto L_0x0119
            L_0x010f:
                r8 = 1
                int r4 = r6 - r10
                int r9 = r7 - r12
                r2.onAnimation(r4, r9)
                goto L_0x0119
            L_0x0118:
                r8 = 1
            L_0x0119:
                r4 = r16
                r9 = r18
                goto L_0x0123
            L_0x011e:
                r8 = 1
                r4 = 0
                r9 = 0
                r10 = 0
                r12 = 0
            L_0x0123:
                com.oneplus.lib.widget.recyclerview.RecyclerView r11 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                java.util.ArrayList r11 = r11.mItemDecorations
                boolean r11 = r11.isEmpty()
                if (r11 != 0) goto L_0x0134
                com.oneplus.lib.widget.recyclerview.RecyclerView r11 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r11.invalidate()
            L_0x0134:
                com.oneplus.lib.widget.recyclerview.RecyclerView r11 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                int r11 = r11.getOverScrollMode()
                r13 = 2
                if (r11 == r13) goto L_0x0142
                com.oneplus.lib.widget.recyclerview.RecyclerView r11 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r11.considerReleasingGlowsOnScroll(r6, r7)
            L_0x0142:
                if (r10 != 0) goto L_0x0146
                if (r12 == 0) goto L_0x0184
            L_0x0146:
                float r11 = r1.getCurrVelocity()
                int r11 = (int) r11
                if (r10 == r3) goto L_0x0155
                if (r10 >= 0) goto L_0x0151
                int r14 = -r11
                goto L_0x0156
            L_0x0151:
                if (r10 <= 0) goto L_0x0155
                r14 = r11
                goto L_0x0156
            L_0x0155:
                r14 = 0
            L_0x0156:
                if (r12 == r5) goto L_0x015f
                if (r12 >= 0) goto L_0x015c
                int r11 = -r11
                goto L_0x0160
            L_0x015c:
                if (r12 <= 0) goto L_0x015f
                goto L_0x0160
            L_0x015f:
                r11 = 0
            L_0x0160:
                com.oneplus.lib.widget.recyclerview.RecyclerView r15 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                int r15 = r15.getOverScrollMode()
                if (r15 == r13) goto L_0x016d
                com.oneplus.lib.widget.recyclerview.RecyclerView r13 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r13.absorbGlows(r14, r11)
            L_0x016d:
                if (r14 != 0) goto L_0x0177
                if (r10 == r3) goto L_0x0177
                int r3 = r1.getFinalX()
                if (r3 != 0) goto L_0x0184
            L_0x0177:
                if (r11 != 0) goto L_0x0181
                if (r12 == r5) goto L_0x0181
                int r3 = r1.getFinalY()
                if (r3 != 0) goto L_0x0184
            L_0x0181:
                r1.abortAnimation()
            L_0x0184:
                if (r4 != 0) goto L_0x0188
                if (r9 == 0) goto L_0x018d
            L_0x0188:
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r3.dispatchOnScrolled(r4, r9)
            L_0x018d:
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                boolean r3 = r3.awakenScrollBars()
                if (r3 != 0) goto L_0x019a
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r3.invalidate()
            L_0x019a:
                if (r7 == 0) goto L_0x01ac
                com.oneplus.lib.widget.recyclerview.RecyclerView r3 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r3 = r3.mLayout
                boolean r3 = r3.canScrollVertically()
                if (r3 == 0) goto L_0x01ac
                if (r9 != r7) goto L_0x01ac
                r3 = r8
                goto L_0x01ad
            L_0x01ac:
                r3 = 0
            L_0x01ad:
                if (r6 == 0) goto L_0x01bf
                com.oneplus.lib.widget.recyclerview.RecyclerView r5 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r5 = r5.mLayout
                boolean r5 = r5.canScrollHorizontally()
                if (r5 == 0) goto L_0x01bf
                if (r4 != r6) goto L_0x01bf
                r4 = r8
                goto L_0x01c0
            L_0x01bf:
                r4 = 0
            L_0x01c0:
                if (r6 != 0) goto L_0x01c4
                if (r7 == 0) goto L_0x01ca
            L_0x01c4:
                if (r4 != 0) goto L_0x01ca
                if (r3 == 0) goto L_0x01c9
                goto L_0x01ca
            L_0x01c9:
                r8 = 0
            L_0x01ca:
                boolean r1 = r1.isFinished()
                if (r1 != 0) goto L_0x01d7
                if (r8 != 0) goto L_0x01d3
                goto L_0x01d7
            L_0x01d3:
                r19.postOnAnimation()
                goto L_0x01de
            L_0x01d7:
                com.oneplus.lib.widget.recyclerview.RecyclerView r1 = com.oneplus.lib.widget.recyclerview.RecyclerView.this
                r3 = 0
                r1.setScrollState(r3)
                goto L_0x01df
            L_0x01de:
                r3 = 0
            L_0x01df:
                if (r2 == 0) goto L_0x01f1
                boolean r1 = r2.isPendingInitialRun()
                if (r1 == 0) goto L_0x01ea
                r2.onAnimation(r3, r3)
            L_0x01ea:
                boolean r1 = r0.mReSchedulePostAnimationCallback
                if (r1 != 0) goto L_0x01f1
                r2.stop()
            L_0x01f1:
                r19.enableRunOnAnimationRequests()
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.ViewFlinger.run():void");
        }

        private void disableRunOnAnimationRequests() {
            this.mReSchedulePostAnimationCallback = false;
            this.mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            this.mEatRunOnAnimationRequest = false;
            if (this.mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        /* access modifiers changed from: 0000 */
        public void postOnAnimation() {
            if (this.mEatRunOnAnimationRequest) {
                this.mReSchedulePostAnimationCallback = true;
                return;
            }
            RecyclerView.this.removeCallbacks(this);
            RecyclerView.this.postOnAnimation(this);
        }

        public void fling(int i, int i2) {
            RecyclerView.this.setScrollState(2);
            this.mLastFlingY = 0;
            this.mLastFlingX = 0;
            this.mScroller.fling(0, 0, i, i2, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void smoothScrollBy(int i, int i2) {
            smoothScrollBy(i, i2, 0, 0);
        }

        public void smoothScrollBy(int i, int i2, int i3, int i4) {
            smoothScrollBy(i, i2, computeScrollDuration(i, i2, i3, i4));
        }

        private float distanceInfluenceForSnapDuration(float f) {
            return (float) Math.sin((double) ((float) (((double) (f - 0.5f)) * 0.4712389167638204d)));
        }

        private int computeScrollDuration(int i, int i2, int i3, int i4) {
            int i5;
            int abs = Math.abs(i);
            int abs2 = Math.abs(i2);
            boolean z = abs > abs2;
            int sqrt = (int) Math.sqrt((double) ((i3 * i3) + (i4 * i4)));
            int sqrt2 = (int) Math.sqrt((double) ((i * i) + (i2 * i2)));
            RecyclerView recyclerView = RecyclerView.this;
            int width = z ? recyclerView.getWidth() : recyclerView.getHeight();
            int i6 = width / 2;
            float f = (float) width;
            float f2 = (float) i6;
            float distanceInfluenceForSnapDuration = f2 + (distanceInfluenceForSnapDuration(Math.min(1.0f, (((float) sqrt2) * 1.0f) / f)) * f2);
            if (sqrt > 0) {
                i5 = Math.round(Math.abs(distanceInfluenceForSnapDuration / ((float) sqrt)) * 1000.0f) * 4;
            } else {
                if (!z) {
                    abs = abs2;
                }
                i5 = (int) (((((float) abs) / f) + 1.0f) * 300.0f);
            }
            return Math.min(i5, 2000);
        }

        public void smoothScrollBy(int i, int i2, int i3) {
            smoothScrollBy(i, i2, i3, RecyclerView.sQuinticInterpolator);
        }

        public void smoothScrollBy(int i, int i2, int i3, Interpolator interpolator) {
            if (this.mInterpolator != interpolator) {
                this.mInterpolator = interpolator;
                this.mScroller = new Scroller(RecyclerView.this.getContext(), interpolator);
            }
            RecyclerView.this.setScrollState(2);
            this.mLastFlingY = 0;
            this.mLastFlingX = 0;
            this.mScroller.startScroll(0, 0, i, i2, i3);
            postOnAnimation();
        }

        public void stop() {
            RecyclerView.this.removeCallbacks(this);
            this.mScroller.abortAnimation();
        }
    }

    public static abstract class ViewHolder {
        private static final List<Object> FULLUPDATE_PAYLOADS = Collections.EMPTY_LIST;
        public final View itemView;
        /* access modifiers changed from: private */
        public int mFlags;
        private int mIsRecyclableCount;
        long mItemId;
        int mItemViewType;
        int mOldPosition;
        RecyclerView mOwnerRecyclerView;
        List<Object> mPayloads;
        int mPosition;
        int mPreLayoutPosition;
        /* access modifiers changed from: private */
        public Recycler mScrapContainer;
        ViewHolder mShadowedHolder;
        ViewHolder mShadowingHolder;
        List<Object> mUnmodifiedPayloads;
        private int mWasImportantForAccessibilityBeforeHidden;

        /* access modifiers changed from: 0000 */
        public void flagRemovedAndOffsetPosition(int i, int i2, boolean z) {
            addFlags(8);
            offsetPosition(i2, z);
            this.mPosition = i;
        }

        /* access modifiers changed from: 0000 */
        public void offsetPosition(int i, boolean z) {
            if (this.mOldPosition == -1) {
                this.mOldPosition = this.mPosition;
            }
            if (this.mPreLayoutPosition == -1) {
                this.mPreLayoutPosition = this.mPosition;
            }
            if (z) {
                this.mPreLayoutPosition += i;
            }
            this.mPosition += i;
            if (this.itemView.getLayoutParams() != null) {
                ((LayoutParams) this.itemView.getLayoutParams()).mInsetsDirty = true;
            }
        }

        /* access modifiers changed from: 0000 */
        public void clearOldPosition() {
            this.mOldPosition = -1;
            this.mPreLayoutPosition = -1;
        }

        /* access modifiers changed from: 0000 */
        public void saveOldPosition() {
            if (this.mOldPosition == -1) {
                this.mOldPosition = this.mPosition;
            }
        }

        /* access modifiers changed from: 0000 */
        public boolean shouldIgnore() {
            return (this.mFlags & 128) != 0;
        }

        public final int getLayoutPosition() {
            int i = this.mPreLayoutPosition;
            return i == -1 ? this.mPosition : i;
        }

        public final long getItemId() {
            return this.mItemId;
        }

        public final int getItemViewType() {
            return this.mItemViewType;
        }

        /* access modifiers changed from: 0000 */
        public boolean isScrap() {
            return this.mScrapContainer != null;
        }

        /* access modifiers changed from: 0000 */
        public void unScrap() {
            this.mScrapContainer.unscrapView(this);
        }

        /* access modifiers changed from: 0000 */
        public boolean wasReturnedFromScrap() {
            return (this.mFlags & 32) != 0;
        }

        /* access modifiers changed from: 0000 */
        public void clearReturnedFromScrapFlag() {
            this.mFlags &= -33;
        }

        /* access modifiers changed from: 0000 */
        public void clearTmpDetachFlag() {
            this.mFlags &= -257;
        }

        /* access modifiers changed from: 0000 */
        public boolean isInvalid() {
            return (this.mFlags & 4) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean needsUpdate() {
            return (this.mFlags & 2) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean isChanged() {
            return (this.mFlags & 64) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean isBound() {
            return (this.mFlags & 1) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean isRemoved() {
            return (this.mFlags & 8) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean hasAnyOfTheFlags(int i) {
            return (this.mFlags & i) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean isTmpDetached() {
            return (this.mFlags & 256) != 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean isAdapterPositionUnknown() {
            return (this.mFlags & 512) != 0 || isInvalid();
        }

        /* access modifiers changed from: 0000 */
        public void setFlags(int i, int i2) {
            this.mFlags = (i & i2) | (this.mFlags & (~i2));
        }

        /* access modifiers changed from: 0000 */
        public void addFlags(int i) {
            this.mFlags = i | this.mFlags;
        }

        /* access modifiers changed from: 0000 */
        public void addChangePayload(Object obj) {
            if (obj == null) {
                addFlags(1024);
            } else if ((1024 & this.mFlags) == 0) {
                createPayloadsIfNeeded();
                this.mPayloads.add(obj);
            }
        }

        private void createPayloadsIfNeeded() {
            if (this.mPayloads == null) {
                this.mPayloads = new ArrayList();
                this.mUnmodifiedPayloads = Collections.unmodifiableList(this.mPayloads);
            }
        }

        /* access modifiers changed from: 0000 */
        public void clearPayload() {
            List<Object> list = this.mPayloads;
            if (list != null) {
                list.clear();
            }
            this.mFlags &= -1025;
        }

        /* access modifiers changed from: 0000 */
        public void resetInternal() {
            this.mFlags = 0;
            this.mPosition = -1;
            this.mOldPosition = -1;
            this.mItemId = -1;
            this.mPreLayoutPosition = -1;
            this.mIsRecyclableCount = 0;
            this.mShadowedHolder = null;
            this.mShadowingHolder = null;
            clearPayload();
            this.mWasImportantForAccessibilityBeforeHidden = 0;
        }

        /* access modifiers changed from: private */
        public void onEnteredHiddenState() {
            this.mWasImportantForAccessibilityBeforeHidden = this.itemView.getImportantForAccessibility();
            this.itemView.setImportantForAccessibility(4);
        }

        /* access modifiers changed from: private */
        public void onLeftHiddenState() {
            this.itemView.setImportantForAccessibility(this.mWasImportantForAccessibilityBeforeHidden);
            this.mWasImportantForAccessibilityBeforeHidden = 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ViewHolder{");
            sb.append(Integer.toHexString(hashCode()));
            sb.append(" position=");
            sb.append(this.mPosition);
            sb.append(" id=");
            sb.append(this.mItemId);
            sb.append(", oldPos=");
            sb.append(this.mOldPosition);
            sb.append(", pLpos:");
            sb.append(this.mPreLayoutPosition);
            StringBuilder sb2 = new StringBuilder(sb.toString());
            if (isScrap()) {
                sb2.append(" scrap");
            }
            if (isInvalid()) {
                sb2.append(" invalid");
            }
            if (!isBound()) {
                sb2.append(" unbound");
            }
            if (needsUpdate()) {
                sb2.append(" update");
            }
            if (isRemoved()) {
                sb2.append(" removed");
            }
            if (shouldIgnore()) {
                sb2.append(" ignored");
            }
            if (isChanged()) {
                sb2.append(" changed");
            }
            if (isTmpDetached()) {
                sb2.append(" tmpDetached");
            }
            if (!isRecyclable()) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(" not recyclable(");
                sb3.append(this.mIsRecyclableCount);
                sb3.append(")");
                sb2.append(sb3.toString());
            }
            if (isAdapterPositionUnknown()) {
                sb2.append("undefined adapter position");
            }
            if (this.itemView.getParent() == null) {
                sb2.append(" no parent");
            }
            sb2.append("}");
            return sb2.toString();
        }

        public final void setIsRecyclable(boolean z) {
            int i = this.mIsRecyclableCount;
            this.mIsRecyclableCount = z ? i - 1 : i + 1;
            int i2 = this.mIsRecyclableCount;
            if (i2 < 0) {
                this.mIsRecyclableCount = 0;
                StringBuilder sb = new StringBuilder();
                sb.append("isRecyclable decremented below 0: unmatched pair of setIsRecyable() calls for ");
                sb.append(this);
                Log.e("View", sb.toString());
            } else if (!z && i2 == 1) {
                this.mFlags |= 16;
            } else if (z && this.mIsRecyclableCount == 0) {
                this.mFlags &= -17;
            }
        }

        public final boolean isRecyclable() {
            return (this.mFlags & 16) == 0 && !this.itemView.hasTransientState();
        }

        /* access modifiers changed from: private */
        public boolean shouldBeKeptAsChild() {
            return (this.mFlags & 16) != 0;
        }

        /* access modifiers changed from: private */
        public boolean doesTransientStatePreventRecycling() {
            return (this.mFlags & 16) == 0 && this.itemView.hasTransientState();
        }
    }

    public void onChildAttachedToWindow(View view) {
    }

    public void onChildDetachedFromWindow(View view) {
    }

    public void onScrollStateChanged(int i) {
    }

    public void onScrolled(int i, int i2) {
    }

    static {
        int i = VERSION.SDK_INT;
        FORCE_INVALIDATE_DISPLAY_LIST = i == 18 || i == 19 || i == 20;
        Class cls = Integer.TYPE;
        LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE = new Class[]{Context.class, AttributeSet.class, cls, cls};
    }

    public RecyclerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mObserver = new RecyclerViewDataObserver();
        this.mRecycler = new Recycler();
        this.mUpdateChildViewsRunnable = new Runnable() {
            public void run() {
                if (RecyclerView.this.mFirstLayoutComplete) {
                    if (RecyclerView.this.mDataSetHasChangedAfterLayout) {
                        Trace.beginSection("RV FullInvalidate");
                        RecyclerView.this.dispatchLayout();
                        Trace.endSection();
                    } else if (RecyclerView.this.mAdapterHelper.hasPendingUpdates()) {
                        Trace.beginSection("RV PartialInvalidate");
                        RecyclerView.this.eatRequestLayout();
                        RecyclerView.this.mAdapterHelper.preProcess();
                        if (!RecyclerView.this.mLayoutRequestEaten) {
                            RecyclerView.this.rebindUpdatedViewHolders();
                        }
                        RecyclerView.this.resumeRequestLayout(true);
                        Trace.endSection();
                    }
                }
            }
        };
        this.mTempRect = new Rect();
        this.mItemDecorations = new ArrayList<>();
        this.mOnItemTouchListeners = new ArrayList<>();
        this.mDataSetHasChangedAfterLayout = false;
        this.mLayoutOrScrollCounter = 0;
        this.mItemAnimator = new DefaultItemAnimator();
        this.mScrollState = 0;
        this.mScrollPointerId = -1;
        this.mScrollFactor = Float.MIN_VALUE;
        this.mViewFlinger = new ViewFlinger();
        this.mState = new State();
        this.mItemsAddedOrRemoved = false;
        this.mItemsChanged = false;
        this.mItemAnimatorListener = new ItemAnimatorRestoreListener();
        this.mPostedAnimatorRunner = false;
        this.mMinMaxLayoutPositions = new int[2];
        this.mScrollOffset = new int[2];
        this.mScrollConsumed = new int[2];
        this.mNestedOffsets = new int[2];
        this.mItemAnimatorRunner = new Runnable() {
            public void run() {
                ItemAnimator itemAnimator = RecyclerView.this.mItemAnimator;
                if (itemAnimator != null) {
                    itemAnimator.runPendingAnimations();
                }
                RecyclerView.this.mPostedAnimatorRunner = false;
            }
        };
        setScrollContainer(true);
        setFocusableInTouchMode(true);
        this.mPostUpdatesOnAnimation = VERSION.SDK_INT >= 16;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        setWillNotDraw(getOverScrollMode() == 2);
        this.mItemAnimator.setListener(this.mItemAnimatorListener);
        initAdapterManager();
        initChildrenHelper();
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        setAccessibilityDelegateCompat(new RecyclerViewAccessibilityDelegate(this));
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.RecyclerView, i, 0);
            String string = obtainStyledAttributes.getString(R$styleable.RecyclerView_op_layoutManager);
            obtainStyledAttributes.recycle();
            createLayoutManager(context, string, attributeSet, i, 0);
        }
        this.mScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    public void setAccessibilityDelegateCompat(RecyclerViewAccessibilityDelegate recyclerViewAccessibilityDelegate) {
        this.mAccessibilityDelegate = recyclerViewAccessibilityDelegate;
        setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    private void createLayoutManager(Context context, String str, AttributeSet attributeSet, int i, int i2) {
        ClassLoader classLoader;
        Constructor constructor;
        String str2 = ": Could not instantiate the LayoutManager: ";
        if (str != null) {
            String trim = str.trim();
            if (trim.length() != 0) {
                String fullClassName = getFullClassName(context, trim);
                try {
                    if (isInEditMode()) {
                        classLoader = getClass().getClassLoader();
                    } else {
                        classLoader = context.getClassLoader();
                    }
                    Class asSubclass = classLoader.loadClass(fullClassName).asSubclass(LayoutManager.class);
                    Object[] objArr = null;
                    try {
                        constructor = asSubclass.getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
                        objArr = new Object[]{context, attributeSet, Integer.valueOf(i), Integer.valueOf(i2)};
                    } catch (NoSuchMethodException e) {
                        constructor = asSubclass.getConstructor(new Class[0]);
                    }
                    constructor.setAccessible(true);
                    setLayoutManager((LayoutManager) constructor.newInstance(objArr));
                } catch (NoSuchMethodException e2) {
                    e2.initCause(e);
                    StringBuilder sb = new StringBuilder();
                    sb.append(attributeSet.getPositionDescription());
                    sb.append(": Error creating LayoutManager ");
                    sb.append(fullClassName);
                    throw new IllegalStateException(sb.toString(), e2);
                } catch (ClassNotFoundException e3) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(attributeSet.getPositionDescription());
                    sb2.append(": Unable to find LayoutManager ");
                    sb2.append(fullClassName);
                    throw new IllegalStateException(sb2.toString(), e3);
                } catch (InvocationTargetException e4) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(attributeSet.getPositionDescription());
                    sb3.append(str2);
                    sb3.append(fullClassName);
                    throw new IllegalStateException(sb3.toString(), e4);
                } catch (InstantiationException e5) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(attributeSet.getPositionDescription());
                    sb4.append(str2);
                    sb4.append(fullClassName);
                    throw new IllegalStateException(sb4.toString(), e5);
                } catch (IllegalAccessException e6) {
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append(attributeSet.getPositionDescription());
                    sb5.append(": Cannot access non-public constructor ");
                    sb5.append(fullClassName);
                    throw new IllegalStateException(sb5.toString(), e6);
                } catch (ClassCastException e7) {
                    StringBuilder sb6 = new StringBuilder();
                    sb6.append(attributeSet.getPositionDescription());
                    sb6.append(": Class is not a LayoutManager ");
                    sb6.append(fullClassName);
                    throw new IllegalStateException(sb6.toString(), e7);
                }
            }
        }
    }

    private String getFullClassName(Context context, String str) {
        if (str.charAt(0) == '.') {
            StringBuilder sb = new StringBuilder();
            sb.append(context.getPackageName());
            sb.append(str);
            return sb.toString();
        } else if (str.contains(".")) {
            return str;
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(RecyclerView.class.getPackage().getName());
            sb2.append('.');
            sb2.append(str);
            return sb2.toString();
        }
    }

    private void initChildrenHelper() {
        this.mChildHelper = new ChildHelper(new Callback() {
            public int getChildCount() {
                return RecyclerView.this.getChildCount();
            }

            public void addView(View view, int i) {
                RecyclerView.this.addView(view, i);
                RecyclerView.this.dispatchChildAttached(view);
            }

            public int indexOfChild(View view) {
                return RecyclerView.this.indexOfChild(view);
            }

            public void removeViewAt(int i) {
                View childAt = RecyclerView.this.getChildAt(i);
                if (childAt != null) {
                    RecyclerView.this.dispatchChildDetached(childAt);
                }
                RecyclerView.this.removeViewAt(i);
            }

            public View getChildAt(int i) {
                return RecyclerView.this.getChildAt(i);
            }

            public void removeAllViews() {
                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    RecyclerView.this.dispatchChildDetached(getChildAt(i));
                }
                RecyclerView.this.removeAllViews();
            }

            public void attachViewToParent(View view, int i, android.view.ViewGroup.LayoutParams layoutParams) {
                ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
                if (childViewHolderInt != null) {
                    if (childViewHolderInt.isTmpDetached() || childViewHolderInt.shouldIgnore()) {
                        childViewHolderInt.clearTmpDetachFlag();
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Called attach on a child which is not detached: ");
                        sb.append(childViewHolderInt);
                        throw new IllegalArgumentException(sb.toString());
                    }
                }
                RecyclerView.this.attachViewToParent(view, i, layoutParams);
            }

            public void onEnteredHiddenState(View view) {
                ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
                if (childViewHolderInt != null) {
                    childViewHolderInt.onEnteredHiddenState();
                }
            }

            public void onLeftHiddenState(View view) {
                ViewHolder childViewHolderInt = RecyclerView.getChildViewHolderInt(view);
                if (childViewHolderInt != null) {
                    childViewHolderInt.onLeftHiddenState();
                }
            }
        });
    }

    /* access modifiers changed from: 0000 */
    public void initAdapterManager() {
        this.mAdapterHelper = new AdapterHelper(new Callback() {
            public ViewHolder findViewHolder(int i) {
                ViewHolder findViewHolderForPosition = RecyclerView.this.findViewHolderForPosition(i, true);
                if (findViewHolderForPosition != null && !RecyclerView.this.mChildHelper.isHidden(findViewHolderForPosition.itemView)) {
                    return findViewHolderForPosition;
                }
                return null;
            }

            public void offsetPositionsForRemovingInvisible(int i, int i2) {
                RecyclerView.this.offsetPositionRecordsForRemove(i, i2, true);
                RecyclerView recyclerView = RecyclerView.this;
                recyclerView.mItemsAddedOrRemoved = true;
                State.access$1212(recyclerView.mState, i2);
            }

            public void offsetPositionsForRemovingLaidOutOrNewView(int i, int i2) {
                RecyclerView.this.offsetPositionRecordsForRemove(i, i2, false);
                RecyclerView.this.mItemsAddedOrRemoved = true;
            }

            public void markViewHoldersUpdated(int i, int i2, Object obj) {
                RecyclerView.this.viewRangeUpdate(i, i2, obj);
                RecyclerView.this.mItemsChanged = true;
            }

            public void onDispatchFirstPass(UpdateOp updateOp) {
                dispatchUpdate(updateOp);
            }

            /* access modifiers changed from: 0000 */
            public void dispatchUpdate(UpdateOp updateOp) {
                int i = updateOp.cmd;
                if (i == 0) {
                    RecyclerView.this.mLayout.onItemsAdded(RecyclerView.this, updateOp.positionStart, updateOp.itemCount);
                } else if (i == 1) {
                    RecyclerView.this.mLayout.onItemsRemoved(RecyclerView.this, updateOp.positionStart, updateOp.itemCount);
                } else if (i == 2) {
                    RecyclerView.this.mLayout.onItemsUpdated(RecyclerView.this, updateOp.positionStart, updateOp.itemCount, updateOp.payload);
                } else if (i == 3) {
                    RecyclerView.this.mLayout.onItemsMoved(RecyclerView.this, updateOp.positionStart, updateOp.itemCount, 1);
                }
            }

            public void onDispatchSecondPass(UpdateOp updateOp) {
                dispatchUpdate(updateOp);
            }

            public void offsetPositionsForAdd(int i, int i2) {
                RecyclerView.this.offsetPositionRecordsForInsert(i, i2);
                RecyclerView.this.mItemsAddedOrRemoved = true;
            }

            public void offsetPositionsForMove(int i, int i2) {
                RecyclerView.this.offsetPositionRecordsForMove(i, i2);
                RecyclerView.this.mItemsAddedOrRemoved = true;
            }
        });
    }

    public void setClipToPadding(boolean z) {
        if (z != this.mClipToPadding) {
            invalidateGlows();
        }
        this.mClipToPadding = z;
        super.setClipToPadding(z);
        if (this.mFirstLayoutComplete) {
            requestLayout();
        }
    }

    public int getBaseline() {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            return layoutManager.getBaseline();
        }
        return super.getBaseline();
    }

    public void setLayoutManager(LayoutManager layoutManager) {
        LayoutManager layoutManager2 = this.mLayout;
        if (layoutManager != layoutManager2) {
            if (layoutManager2 != null) {
                if (this.mIsAttached) {
                    layoutManager2.dispatchDetachedFromWindow(this, this.mRecycler);
                }
                this.mLayout.setRecyclerView(null);
            }
            this.mRecycler.clear();
            this.mChildHelper.removeAllViewsUnfiltered();
            this.mLayout = layoutManager;
            if (layoutManager != null) {
                if (layoutManager.mRecyclerView == null) {
                    this.mLayout.setRecyclerView(this);
                    if (this.mIsAttached) {
                        this.mLayout.dispatchAttachedToWindow(this);
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("LayoutManager ");
                    sb.append(layoutManager);
                    sb.append(" is already attached to a RecyclerView: ");
                    sb.append(layoutManager.mRecyclerView);
                    throw new IllegalArgumentException(sb.toString());
                }
            }
            requestLayout();
        }
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        SavedState savedState2 = this.mPendingSavedState;
        if (savedState2 != null) {
            savedState.copyFrom(savedState2);
        } else {
            LayoutManager layoutManager = this.mLayout;
            if (layoutManager != null) {
                savedState.mLayoutState = layoutManager.onSaveInstanceState();
            } else {
                savedState.mLayoutState = null;
            }
        }
        return savedState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        this.mPendingSavedState = (SavedState) parcelable;
        super.onRestoreInstanceState(this.mPendingSavedState.getSuperState());
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            Parcelable parcelable2 = this.mPendingSavedState.mLayoutState;
            if (parcelable2 != null) {
                layoutManager.onRestoreInstanceState(parcelable2);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        dispatchFreezeSelfOnly(sparseArray);
    }

    /* access modifiers changed from: protected */
    public void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        dispatchThawSelfOnly(sparseArray);
    }

    private void addAnimatingView(ViewHolder viewHolder) {
        View view = viewHolder.itemView;
        boolean z = view.getParent() == this;
        this.mRecycler.unscrapView(getChildViewHolder(view));
        if (viewHolder.isTmpDetached()) {
            this.mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        } else if (!z) {
            this.mChildHelper.addView(view, true);
        } else {
            this.mChildHelper.hide(view);
        }
    }

    /* access modifiers changed from: private */
    public boolean removeAnimatingView(View view) {
        eatRequestLayout();
        boolean removeViewIfHidden = this.mChildHelper.removeViewIfHidden(view);
        if (removeViewIfHidden) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(view);
            this.mRecycler.unscrapView(childViewHolderInt);
            this.mRecycler.recycleViewHolderInternal(childViewHolderInt);
        }
        resumeRequestLayout(false);
        return removeViewIfHidden;
    }

    public LayoutManager getLayoutManager() {
        return this.mLayout;
    }

    /* access modifiers changed from: private */
    public void setScrollState(int i) {
        if (i != this.mScrollState) {
            this.mScrollState = i;
            if (i != 2) {
                stopScrollersInternal();
            }
            dispatchOnScrollStateChanged(i);
        }
    }

    /* access modifiers changed from: private */
    public void jumpToPositionForSmoothScroller(int i) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            layoutManager.scrollToPosition(i);
            awakenScrollBars();
        }
    }

    public void scrollTo(int i, int i2) {
        throw new UnsupportedOperationException("RecyclerView does not support scrolling to an absolute position.");
    }

    public void scrollBy(int i, int i2) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null) {
            Log.e("RecyclerView", "Cannot scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.");
        } else if (!this.mLayoutFrozen) {
            boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
            boolean canScrollVertically = this.mLayout.canScrollVertically();
            if (canScrollHorizontally || canScrollVertically) {
                if (!canScrollHorizontally) {
                    i = 0;
                }
                if (!canScrollVertically) {
                    i2 = 0;
                }
                scrollByInternal(i, i2, null);
            }
        }
    }

    /* access modifiers changed from: private */
    public void consumePendingUpdateOperations() {
        this.mUpdateChildViewsRunnable.run();
    }

    /* access modifiers changed from: 0000 */
    public boolean scrollByInternal(int i, int i2, MotionEvent motionEvent) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11 = i;
        int i12 = i2;
        MotionEvent motionEvent2 = motionEvent;
        consumePendingUpdateOperations();
        if (this.mAdapter != null) {
            eatRequestLayout();
            onEnterLayoutOrScroll();
            Trace.beginSection("RV Scroll");
            if (i11 != 0) {
                i8 = this.mLayout.scrollHorizontallyBy(i11, this.mRecycler, this.mState);
                i7 = i11 - i8;
            } else {
                i8 = 0;
                i7 = 0;
            }
            if (i12 != 0) {
                i10 = this.mLayout.scrollVerticallyBy(i12, this.mRecycler, this.mState);
                i9 = i12 - i10;
            } else {
                i10 = 0;
                i9 = 0;
            }
            Trace.endSection();
            if (supportsChangeAnimations()) {
                int childCount = this.mChildHelper.getChildCount();
                for (int i13 = 0; i13 < childCount; i13++) {
                    View childAt = this.mChildHelper.getChildAt(i13);
                    ViewHolder childViewHolder = getChildViewHolder(childAt);
                    if (childViewHolder != null) {
                        ViewHolder viewHolder = childViewHolder.mShadowingHolder;
                        if (viewHolder != null) {
                            View view = viewHolder != null ? viewHolder.itemView : null;
                            if (view != null) {
                                int left = childAt.getLeft();
                                int top = childAt.getTop();
                                if (left != view.getLeft() || top != view.getTop()) {
                                    view.layout(left, top, view.getWidth() + left, view.getHeight() + top);
                                }
                            }
                        }
                    }
                }
            }
            onExitLayoutOrScroll();
            resumeRequestLayout(false);
            i4 = i8;
            i6 = i7;
            i3 = i10;
            i5 = i9;
        } else {
            i6 = 0;
            i5 = 0;
            i4 = 0;
            i3 = 0;
        }
        if (!this.mItemDecorations.isEmpty()) {
            invalidate();
        }
        if (dispatchNestedScroll(i4, i3, i6, i5, this.mScrollOffset)) {
            int i14 = this.mLastTouchX;
            int[] iArr = this.mScrollOffset;
            this.mLastTouchX = i14 - iArr[0];
            this.mLastTouchY -= iArr[1];
            if (motionEvent2 != null) {
                motionEvent2.offsetLocation((float) iArr[0], (float) iArr[1]);
            }
            int[] iArr2 = this.mNestedOffsets;
            int i15 = iArr2[0];
            int[] iArr3 = this.mScrollOffset;
            iArr2[0] = i15 + iArr3[0];
            iArr2[1] = iArr2[1] + iArr3[1];
        } else if (getOverScrollMode() != 2) {
            if (motionEvent2 != null) {
                pullGlows(motionEvent.getX(), (float) i6, motionEvent.getY(), (float) i5);
            }
            considerReleasingGlowsOnScroll(i, i2);
        }
        if (!(i4 == 0 && i3 == 0)) {
            dispatchOnScrolled(i4, i3);
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        if (i4 == 0 && i3 == 0) {
            return false;
        }
        return true;
    }

    public int computeHorizontalScrollOffset() {
        if (this.mLayout.canScrollHorizontally()) {
            return this.mLayout.computeHorizontalScrollOffset(this.mState);
        }
        return 0;
    }

    public int computeHorizontalScrollExtent() {
        if (this.mLayout.canScrollHorizontally()) {
            return this.mLayout.computeHorizontalScrollExtent(this.mState);
        }
        return 0;
    }

    public int computeHorizontalScrollRange() {
        if (this.mLayout.canScrollHorizontally()) {
            return this.mLayout.computeHorizontalScrollRange(this.mState);
        }
        return 0;
    }

    public int computeVerticalScrollOffset() {
        if (this.mLayout.canScrollVertically()) {
            return this.mLayout.computeVerticalScrollOffset(this.mState);
        }
        return 0;
    }

    public int computeVerticalScrollExtent() {
        if (this.mLayout.canScrollVertically()) {
            return this.mLayout.computeVerticalScrollExtent(this.mState);
        }
        return 0;
    }

    public int computeVerticalScrollRange() {
        if (this.mLayout.canScrollVertically()) {
            return this.mLayout.computeVerticalScrollRange(this.mState);
        }
        return 0;
    }

    /* access modifiers changed from: 0000 */
    public void eatRequestLayout() {
        if (!this.mEatRequestLayout) {
            this.mEatRequestLayout = true;
            if (!this.mLayoutFrozen) {
                this.mLayoutRequestEaten = false;
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void resumeRequestLayout(boolean z) {
        if (this.mEatRequestLayout) {
            if (z && this.mLayoutRequestEaten && !this.mLayoutFrozen && this.mLayout != null && this.mAdapter != null) {
                dispatchLayout();
            }
            this.mEatRequestLayout = false;
            if (!this.mLayoutFrozen) {
                this.mLayoutRequestEaten = false;
            }
        }
    }

    public void smoothScrollBy(int i, int i2) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null) {
            Log.e("RecyclerView", "Cannot smooth scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.");
        } else if (!this.mLayoutFrozen) {
            if (!layoutManager.canScrollHorizontally()) {
                i = 0;
            }
            if (!this.mLayout.canScrollVertically()) {
                i2 = 0;
            }
            if (!(i == 0 && i2 == 0)) {
                this.mViewFlinger.smoothScrollBy(i, i2);
            }
        }
    }

    public boolean fling(int i, int i2) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null) {
            Log.e("RecyclerView", "Cannot fling without a LayoutManager set. Call setLayoutManager with a non-null argument.");
            return false;
        } else if (this.mLayoutFrozen) {
            return false;
        } else {
            boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
            boolean canScrollVertically = this.mLayout.canScrollVertically();
            if (!canScrollHorizontally || Math.abs(i) < this.mMinFlingVelocity) {
                i = 0;
            }
            if (!canScrollVertically || Math.abs(i2) < this.mMinFlingVelocity) {
                i2 = 0;
            }
            if (i == 0 && i2 == 0) {
                return false;
            }
            float f = (float) i;
            float f2 = (float) i2;
            if (!dispatchNestedPreFling(f, f2)) {
                boolean z = canScrollHorizontally || canScrollVertically;
                dispatchNestedFling(f, f2, z);
                if (z) {
                    int i3 = this.mMaxFlingVelocity;
                    int max = Math.max(-i3, Math.min(i, i3));
                    int i4 = this.mMaxFlingVelocity;
                    this.mViewFlinger.fling(max, Math.max(-i4, Math.min(i2, i4)));
                    return true;
                }
            }
            return false;
        }
    }

    public void stopScroll() {
        setScrollState(0);
        stopScrollersInternal();
    }

    private void stopScrollersInternal() {
        this.mViewFlinger.stop();
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            layoutManager.stopSmoothScroller();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0056  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void pullGlows(float r7, float r8, float r9, float r10) {
        /*
            r6 = this;
            r0 = 0
            int r1 = (r8 > r0 ? 1 : (r8 == r0 ? 0 : -1))
            r2 = 1065353216(0x3f800000, float:1.0)
            r3 = 1
            if (r1 >= 0) goto L_0x0021
            r6.ensureLeftGlow()
            android.widget.EdgeEffect r1 = r6.mLeftGlow
            float r4 = -r8
            int r5 = r6.getWidth()
            float r5 = (float) r5
            float r4 = r4 / r5
            int r5 = r6.getHeight()
            float r5 = (float) r5
            float r9 = r9 / r5
            float r9 = r2 - r9
            r1.onPull(r4, r9)
        L_0x001f:
            r9 = r3
            goto L_0x003c
        L_0x0021:
            int r1 = (r8 > r0 ? 1 : (r8 == r0 ? 0 : -1))
            if (r1 <= 0) goto L_0x003b
            r6.ensureRightGlow()
            android.widget.EdgeEffect r1 = r6.mRightGlow
            int r4 = r6.getWidth()
            float r4 = (float) r4
            float r4 = r8 / r4
            int r5 = r6.getHeight()
            float r5 = (float) r5
            float r9 = r9 / r5
            r1.onPull(r4, r9)
            goto L_0x001f
        L_0x003b:
            r9 = 0
        L_0x003c:
            int r1 = (r10 > r0 ? 1 : (r10 == r0 ? 0 : -1))
            if (r1 >= 0) goto L_0x0056
            r6.ensureTopGlow()
            android.widget.EdgeEffect r9 = r6.mTopGlow
            float r1 = -r10
            int r2 = r6.getHeight()
            float r2 = (float) r2
            float r1 = r1 / r2
            int r2 = r6.getWidth()
            float r2 = (float) r2
            float r7 = r7 / r2
            r9.onPull(r1, r7)
            goto L_0x0072
        L_0x0056:
            int r1 = (r10 > r0 ? 1 : (r10 == r0 ? 0 : -1))
            if (r1 <= 0) goto L_0x0071
            r6.ensureBottomGlow()
            android.widget.EdgeEffect r9 = r6.mBottomGlow
            int r1 = r6.getHeight()
            float r1 = (float) r1
            float r1 = r10 / r1
            int r4 = r6.getWidth()
            float r4 = (float) r4
            float r7 = r7 / r4
            float r2 = r2 - r7
            r9.onPull(r1, r2)
            goto L_0x0072
        L_0x0071:
            r3 = r9
        L_0x0072:
            if (r3 != 0) goto L_0x007c
            int r7 = (r8 > r0 ? 1 : (r8 == r0 ? 0 : -1))
            if (r7 != 0) goto L_0x007c
            int r7 = (r10 > r0 ? 1 : (r10 == r0 ? 0 : -1))
            if (r7 == 0) goto L_0x007f
        L_0x007c:
            r6.postInvalidateOnAnimation()
        L_0x007f:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.pullGlows(float, float, float, float):void");
    }

    private void releaseGlows() {
        boolean z;
        EdgeEffect edgeEffect = this.mLeftGlow;
        if (edgeEffect != null) {
            edgeEffect.onRelease();
            z = this.mLeftGlow.isFinished();
        } else {
            z = false;
        }
        EdgeEffect edgeEffect2 = this.mTopGlow;
        if (edgeEffect2 != null) {
            edgeEffect2.onRelease();
            z |= this.mTopGlow.isFinished();
        }
        EdgeEffect edgeEffect3 = this.mRightGlow;
        if (edgeEffect3 != null) {
            edgeEffect3.onRelease();
            z |= this.mRightGlow.isFinished();
        }
        EdgeEffect edgeEffect4 = this.mBottomGlow;
        if (edgeEffect4 != null) {
            edgeEffect4.onRelease();
            z |= this.mBottomGlow.isFinished();
        }
        if (z) {
            postInvalidateOnAnimation();
        }
    }

    /* access modifiers changed from: private */
    public void considerReleasingGlowsOnScroll(int i, int i2) {
        boolean z;
        EdgeEffect edgeEffect = this.mLeftGlow;
        if (edgeEffect == null || edgeEffect.isFinished() || i <= 0) {
            z = false;
        } else {
            this.mLeftGlow.onRelease();
            z = this.mLeftGlow.isFinished();
        }
        EdgeEffect edgeEffect2 = this.mRightGlow;
        if (edgeEffect2 != null && !edgeEffect2.isFinished() && i < 0) {
            this.mRightGlow.onRelease();
            z |= this.mRightGlow.isFinished();
        }
        EdgeEffect edgeEffect3 = this.mTopGlow;
        if (edgeEffect3 != null && !edgeEffect3.isFinished() && i2 > 0) {
            this.mTopGlow.onRelease();
            z |= this.mTopGlow.isFinished();
        }
        EdgeEffect edgeEffect4 = this.mBottomGlow;
        if (edgeEffect4 != null && !edgeEffect4.isFinished() && i2 < 0) {
            this.mBottomGlow.onRelease();
            z |= this.mBottomGlow.isFinished();
        }
        if (z) {
            postInvalidateOnAnimation();
        }
    }

    /* access modifiers changed from: 0000 */
    public void absorbGlows(int i, int i2) {
        if (i < 0) {
            ensureLeftGlow();
            this.mLeftGlow.onAbsorb(-i);
        } else if (i > 0) {
            ensureRightGlow();
            this.mRightGlow.onAbsorb(i);
        }
        if (i2 < 0) {
            ensureTopGlow();
            this.mTopGlow.onAbsorb(-i2);
        } else if (i2 > 0) {
            ensureBottomGlow();
            this.mBottomGlow.onAbsorb(i2);
        }
        if (i != 0 || i2 != 0) {
            postInvalidateOnAnimation();
        }
    }

    /* access modifiers changed from: 0000 */
    public void ensureLeftGlow() {
        if (this.mLeftGlow == null) {
            this.mLeftGlow = new EdgeEffect(getContext());
            if (this.mClipToPadding) {
                this.mLeftGlow.setSize((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom(), (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight());
            } else {
                this.mLeftGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void ensureRightGlow() {
        if (this.mRightGlow == null) {
            this.mRightGlow = new EdgeEffect(getContext());
            if (this.mClipToPadding) {
                this.mRightGlow.setSize((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom(), (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight());
            } else {
                this.mRightGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void ensureTopGlow() {
        if (this.mTopGlow == null) {
            this.mTopGlow = new EdgeEffect(getContext());
            if (this.mClipToPadding) {
                this.mTopGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                this.mTopGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void ensureBottomGlow() {
        if (this.mBottomGlow == null) {
            this.mBottomGlow = new EdgeEffect(getContext());
            if (this.mClipToPadding) {
                this.mBottomGlow.setSize((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom());
            } else {
                this.mBottomGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void invalidateGlows() {
        this.mBottomGlow = null;
        this.mTopGlow = null;
        this.mRightGlow = null;
        this.mLeftGlow = null;
    }

    public View focusSearch(View view, int i) {
        View onInterceptFocusSearch = this.mLayout.onInterceptFocusSearch(view, i);
        if (onInterceptFocusSearch != null) {
            return onInterceptFocusSearch;
        }
        View findNextFocus = FocusFinder.getInstance().findNextFocus(this, view, i);
        if (findNextFocus == null && this.mAdapter != null && this.mLayout != null && !isComputingLayout() && !this.mLayoutFrozen) {
            eatRequestLayout();
            findNextFocus = this.mLayout.onFocusSearchFailed(view, i, this.mRecycler, this.mState);
            resumeRequestLayout(false);
        }
        if (findNextFocus == null) {
            findNextFocus = super.focusSearch(view, i);
        }
        return findNextFocus;
    }

    public void requestChildFocus(View view, View view2) {
        if (!this.mLayout.onRequestChildFocus(this, this.mState, view, view2) && view2 != null) {
            this.mTempRect.set(0, 0, view2.getWidth(), view2.getHeight());
            android.view.ViewGroup.LayoutParams layoutParams = view2.getLayoutParams();
            if (layoutParams instanceof LayoutParams) {
                LayoutParams layoutParams2 = (LayoutParams) layoutParams;
                if (!layoutParams2.mInsetsDirty) {
                    Rect rect = layoutParams2.mDecorInsets;
                    Rect rect2 = this.mTempRect;
                    rect2.left -= rect.left;
                    rect2.right += rect.right;
                    rect2.top -= rect.top;
                    rect2.bottom += rect.bottom;
                }
            }
            offsetDescendantRectToMyCoords(view2, this.mTempRect);
            offsetRectIntoDescendantCoords(view, this.mTempRect);
            requestChildRectangleOnScreen(view, this.mTempRect, !this.mFirstLayoutComplete);
        }
        super.requestChildFocus(view, view2);
    }

    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        return this.mLayout.requestChildRectangleOnScreen(this, view, rect, z);
    }

    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null || !layoutManager.onAddFocusables(this, arrayList, i, i2)) {
            super.addFocusables(arrayList, i, i2);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLayoutOrScrollCounter = 0;
        this.mIsAttached = true;
        this.mFirstLayoutComplete = false;
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            layoutManager.dispatchAttachedToWindow(this);
        }
        this.mPostedAnimatorRunner = false;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ItemAnimator itemAnimator = this.mItemAnimator;
        if (itemAnimator != null) {
            itemAnimator.endAnimations();
        }
        this.mFirstLayoutComplete = false;
        stopScroll();
        this.mIsAttached = false;
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            layoutManager.dispatchDetachedFromWindow(this, this.mRecycler);
        }
        removeCallbacks(this.mItemAnimatorRunner);
    }

    public boolean isAttachedToWindow() {
        return this.mIsAttached;
    }

    private boolean dispatchOnItemTouchIntercept(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 3 || action == 0) {
            this.mActiveOnItemTouchListener = null;
        }
        int size = this.mOnItemTouchListeners.size();
        int i = 0;
        while (i < size) {
            OnItemTouchListener onItemTouchListener = (OnItemTouchListener) this.mOnItemTouchListeners.get(i);
            if (!onItemTouchListener.onInterceptTouchEvent(this, motionEvent) || action == 3) {
                i++;
            } else {
                this.mActiveOnItemTouchListener = onItemTouchListener;
                return true;
            }
        }
        return false;
    }

    private boolean dispatchOnItemTouch(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        OnItemTouchListener onItemTouchListener = this.mActiveOnItemTouchListener;
        if (onItemTouchListener != null) {
            if (action == 0) {
                this.mActiveOnItemTouchListener = null;
            } else {
                onItemTouchListener.onTouchEvent(this, motionEvent);
                if (action == 3 || action == 1) {
                    this.mActiveOnItemTouchListener = null;
                }
                return true;
            }
        }
        if (action != 0) {
            int size = this.mOnItemTouchListeners.size();
            for (int i = 0; i < size; i++) {
                OnItemTouchListener onItemTouchListener2 = (OnItemTouchListener) this.mOnItemTouchListeners.get(i);
                if (onItemTouchListener2.onInterceptTouchEvent(this, motionEvent)) {
                    this.mActiveOnItemTouchListener = onItemTouchListener2;
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x00c8  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00dd  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onInterceptTouchEvent(android.view.MotionEvent r9) {
        /*
            r8 = this;
            boolean r0 = r8.mLayoutFrozen
            r1 = 0
            if (r0 == 0) goto L_0x0006
            return r1
        L_0x0006:
            boolean r0 = r8.dispatchOnItemTouchIntercept(r9)
            r2 = 1
            if (r0 == 0) goto L_0x0011
            r8.cancelTouch()
            return r2
        L_0x0011:
            com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r0 = r8.mLayout
            if (r0 != 0) goto L_0x0016
            return r1
        L_0x0016:
            boolean r0 = r0.canScrollHorizontally()
            com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r3 = r8.mLayout
            boolean r3 = r3.canScrollVertically()
            android.view.VelocityTracker r4 = r8.mVelocityTracker
            if (r4 != 0) goto L_0x002a
            android.view.VelocityTracker r4 = android.view.VelocityTracker.obtain()
            r8.mVelocityTracker = r4
        L_0x002a:
            android.view.VelocityTracker r4 = r8.mVelocityTracker
            r4.addMovement(r9)
            int r4 = r9.getActionMasked()
            int r5 = r9.getActionIndex()
            r6 = 2
            r7 = 1056964608(0x3f000000, float:0.5)
            if (r4 == 0) goto L_0x00f3
            if (r4 == r2) goto L_0x00ea
            if (r4 == r6) goto L_0x0071
            r0 = 3
            if (r4 == r0) goto L_0x006c
            r0 = 5
            if (r4 == r0) goto L_0x0050
            r0 = 6
            if (r4 == r0) goto L_0x004b
            goto L_0x012d
        L_0x004b:
            r8.onPointerUp(r9)
            goto L_0x012d
        L_0x0050:
            int r0 = r9.getPointerId(r5)
            r8.mScrollPointerId = r0
            float r0 = r9.getX(r5)
            float r0 = r0 + r7
            int r0 = (int) r0
            r8.mLastTouchX = r0
            r8.mInitialTouchX = r0
            float r9 = r9.getY(r5)
            float r9 = r9 + r7
            int r9 = (int) r9
            r8.mLastTouchY = r9
            r8.mInitialTouchY = r9
            goto L_0x012d
        L_0x006c:
            r8.cancelTouch()
            goto L_0x012d
        L_0x0071:
            int r4 = r8.mScrollPointerId
            int r4 = r9.findPointerIndex(r4)
            if (r4 >= 0) goto L_0x0097
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r0 = "Error processing scroll; pointer index for id "
            r9.append(r0)
            int r8 = r8.mScrollPointerId
            r9.append(r8)
            java.lang.String r8 = " not found. Did any MotionEvents get skipped?"
            r9.append(r8)
            java.lang.String r8 = r9.toString()
            java.lang.String r9 = "RecyclerView"
            android.util.Log.e(r9, r8)
            return r1
        L_0x0097:
            float r5 = r9.getX(r4)
            float r5 = r5 + r7
            int r5 = (int) r5
            float r9 = r9.getY(r4)
            float r9 = r9 + r7
            int r9 = (int) r9
            int r4 = r8.mScrollState
            if (r4 == r2) goto L_0x012d
            int r4 = r8.mInitialTouchX
            int r5 = r5 - r4
            int r4 = r8.mInitialTouchY
            int r9 = r9 - r4
            r4 = -1
            if (r0 == 0) goto L_0x00c5
            int r0 = java.lang.Math.abs(r5)
            int r6 = r8.mTouchSlop
            if (r0 <= r6) goto L_0x00c5
            int r0 = r8.mInitialTouchX
            if (r5 >= 0) goto L_0x00be
            r5 = r4
            goto L_0x00bf
        L_0x00be:
            r5 = r2
        L_0x00bf:
            int r6 = r6 * r5
            int r0 = r0 + r6
            r8.mLastTouchX = r0
            r0 = r2
            goto L_0x00c6
        L_0x00c5:
            r0 = r1
        L_0x00c6:
            if (r3 == 0) goto L_0x00db
            int r3 = java.lang.Math.abs(r9)
            int r5 = r8.mTouchSlop
            if (r3 <= r5) goto L_0x00db
            int r0 = r8.mInitialTouchY
            if (r9 >= 0) goto L_0x00d5
            goto L_0x00d6
        L_0x00d5:
            r4 = r2
        L_0x00d6:
            int r5 = r5 * r4
            int r0 = r0 + r5
            r8.mLastTouchY = r0
            r0 = r2
        L_0x00db:
            if (r0 == 0) goto L_0x012d
            android.view.ViewParent r9 = r8.getParent()
            if (r9 == 0) goto L_0x00e6
            r9.requestDisallowInterceptTouchEvent(r2)
        L_0x00e6:
            r8.setScrollState(r2)
            goto L_0x012d
        L_0x00ea:
            android.view.VelocityTracker r9 = r8.mVelocityTracker
            r9.clear()
            r8.stopNestedScroll()
            goto L_0x012d
        L_0x00f3:
            boolean r4 = r8.mIgnoreMotionEventTillDown
            if (r4 == 0) goto L_0x00f9
            r8.mIgnoreMotionEventTillDown = r1
        L_0x00f9:
            int r4 = r9.getPointerId(r1)
            r8.mScrollPointerId = r4
            float r4 = r9.getX()
            float r4 = r4 + r7
            int r4 = (int) r4
            r8.mLastTouchX = r4
            r8.mInitialTouchX = r4
            float r9 = r9.getY()
            float r9 = r9 + r7
            int r9 = (int) r9
            r8.mLastTouchY = r9
            r8.mInitialTouchY = r9
            int r9 = r8.mScrollState
            if (r9 != r6) goto L_0x0121
            android.view.ViewParent r9 = r8.getParent()
            r9.requestDisallowInterceptTouchEvent(r2)
            r8.setScrollState(r2)
        L_0x0121:
            if (r0 == 0) goto L_0x0125
            r9 = r2
            goto L_0x0126
        L_0x0125:
            r9 = r1
        L_0x0126:
            if (r3 == 0) goto L_0x012a
            r9 = r9 | 2
        L_0x012a:
            r8.startNestedScroll(r9)
        L_0x012d:
            int r8 = r8.mScrollState
            if (r8 != r2) goto L_0x0132
            r1 = r2
        L_0x0132:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.onInterceptTouchEvent(android.view.MotionEvent):boolean");
    }

    public void requestDisallowInterceptTouchEvent(boolean z) {
        int size = this.mOnItemTouchListeners.size();
        for (int i = 0; i < size; i++) {
            ((OnItemTouchListener) this.mOnItemTouchListeners.get(i)).onRequestDisallowInterceptTouchEvent(z);
        }
        super.requestDisallowInterceptTouchEvent(z);
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x0107  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x0117  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r13) {
        /*
            r12 = this;
            boolean r0 = r12.mLayoutFrozen
            r1 = 0
            if (r0 != 0) goto L_0x01bd
            boolean r0 = r12.mIgnoreMotionEventTillDown
            if (r0 == 0) goto L_0x000b
            goto L_0x01bd
        L_0x000b:
            boolean r0 = r12.dispatchOnItemTouch(r13)
            r2 = 1
            if (r0 == 0) goto L_0x0016
            r12.cancelTouch()
            return r2
        L_0x0016:
            com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r0 = r12.mLayout
            if (r0 != 0) goto L_0x001b
            return r1
        L_0x001b:
            boolean r0 = r0.canScrollHorizontally()
            com.oneplus.lib.widget.recyclerview.RecyclerView$LayoutManager r3 = r12.mLayout
            boolean r3 = r3.canScrollVertically()
            android.view.VelocityTracker r4 = r12.mVelocityTracker
            if (r4 != 0) goto L_0x002f
            android.view.VelocityTracker r4 = android.view.VelocityTracker.obtain()
            r12.mVelocityTracker = r4
        L_0x002f:
            android.view.MotionEvent r4 = android.view.MotionEvent.obtain(r13)
            int r5 = r13.getActionMasked()
            int r6 = r13.getActionIndex()
            if (r5 != 0) goto L_0x0043
            int[] r7 = r12.mNestedOffsets
            r7[r2] = r1
            r7[r1] = r1
        L_0x0043:
            int[] r7 = r12.mNestedOffsets
            r8 = r7[r1]
            float r8 = (float) r8
            r7 = r7[r2]
            float r7 = (float) r7
            r4.offsetLocation(r8, r7)
            r7 = 1056964608(0x3f000000, float:0.5)
            if (r5 == 0) goto L_0x018c
            if (r5 == r2) goto L_0x014a
            r8 = 2
            if (r5 == r8) goto L_0x0088
            r0 = 3
            if (r5 == r0) goto L_0x0083
            r0 = 5
            if (r5 == r0) goto L_0x0067
            r0 = 6
            if (r5 == r0) goto L_0x0062
            goto L_0x01b2
        L_0x0062:
            r12.onPointerUp(r13)
            goto L_0x01b2
        L_0x0067:
            int r0 = r13.getPointerId(r6)
            r12.mScrollPointerId = r0
            float r0 = r13.getX(r6)
            float r0 = r0 + r7
            int r0 = (int) r0
            r12.mLastTouchX = r0
            r12.mInitialTouchX = r0
            float r13 = r13.getY(r6)
            float r13 = r13 + r7
            int r13 = (int) r13
            r12.mLastTouchY = r13
            r12.mInitialTouchY = r13
            goto L_0x01b2
        L_0x0083:
            r12.cancelTouch()
            goto L_0x01b2
        L_0x0088:
            int r5 = r12.mScrollPointerId
            int r5 = r13.findPointerIndex(r5)
            if (r5 >= 0) goto L_0x00ae
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r0 = "Error processing scroll; pointer index for id "
            r13.append(r0)
            int r12 = r12.mScrollPointerId
            r13.append(r12)
            java.lang.String r12 = " not found. Did any MotionEvents get skipped?"
            r13.append(r12)
            java.lang.String r12 = r13.toString()
            java.lang.String r13 = "RecyclerView"
            android.util.Log.e(r13, r12)
            return r1
        L_0x00ae:
            float r6 = r13.getX(r5)
            float r6 = r6 + r7
            int r6 = (int) r6
            float r13 = r13.getY(r5)
            float r13 = r13 + r7
            int r13 = (int) r13
            int r5 = r12.mLastTouchX
            int r5 = r5 - r6
            int r7 = r12.mLastTouchY
            int r7 = r7 - r13
            int[] r8 = r12.mScrollConsumed
            int[] r9 = r12.mScrollOffset
            boolean r8 = r12.dispatchNestedPreScroll(r5, r7, r8, r9)
            if (r8 == 0) goto L_0x00ef
            int[] r8 = r12.mScrollConsumed
            r9 = r8[r1]
            int r5 = r5 - r9
            r8 = r8[r2]
            int r7 = r7 - r8
            int[] r8 = r12.mScrollOffset
            r9 = r8[r1]
            float r9 = (float) r9
            r8 = r8[r2]
            float r8 = (float) r8
            r4.offsetLocation(r9, r8)
            int[] r8 = r12.mNestedOffsets
            r9 = r8[r1]
            int[] r10 = r12.mScrollOffset
            r11 = r10[r1]
            int r9 = r9 + r11
            r8[r1] = r9
            r9 = r8[r2]
            r10 = r10[r2]
            int r9 = r9 + r10
            r8[r2] = r9
        L_0x00ef:
            int r8 = r12.mScrollState
            if (r8 == r2) goto L_0x0123
            if (r0 == 0) goto L_0x0104
            int r8 = java.lang.Math.abs(r5)
            int r9 = r12.mTouchSlop
            if (r8 <= r9) goto L_0x0104
            if (r5 <= 0) goto L_0x0101
            int r5 = r5 - r9
            goto L_0x0102
        L_0x0101:
            int r5 = r5 + r9
        L_0x0102:
            r8 = r2
            goto L_0x0105
        L_0x0104:
            r8 = r1
        L_0x0105:
            if (r3 == 0) goto L_0x0115
            int r9 = java.lang.Math.abs(r7)
            int r10 = r12.mTouchSlop
            if (r9 <= r10) goto L_0x0115
            if (r7 <= 0) goto L_0x0113
            int r7 = r7 - r10
            goto L_0x0114
        L_0x0113:
            int r7 = r7 + r10
        L_0x0114:
            r8 = r2
        L_0x0115:
            if (r8 == 0) goto L_0x0123
            android.view.ViewParent r8 = r12.getParent()
            if (r8 == 0) goto L_0x0120
            r8.requestDisallowInterceptTouchEvent(r2)
        L_0x0120:
            r12.setScrollState(r2)
        L_0x0123:
            int r8 = r12.mScrollState
            if (r8 != r2) goto L_0x01b2
            int[] r8 = r12.mScrollOffset
            r9 = r8[r1]
            int r6 = r6 - r9
            r12.mLastTouchX = r6
            r6 = r8[r2]
            int r13 = r13 - r6
            r12.mLastTouchY = r13
            if (r0 == 0) goto L_0x0136
            goto L_0x0137
        L_0x0136:
            r5 = r1
        L_0x0137:
            if (r3 == 0) goto L_0x013a
            goto L_0x013b
        L_0x013a:
            r7 = r1
        L_0x013b:
            boolean r13 = r12.scrollByInternal(r5, r7, r4)
            if (r13 == 0) goto L_0x01b2
            android.view.ViewParent r13 = r12.getParent()
            r13.requestDisallowInterceptTouchEvent(r2)
            goto L_0x01b2
        L_0x014a:
            android.view.VelocityTracker r13 = r12.mVelocityTracker
            r13.addMovement(r4)
            android.view.VelocityTracker r13 = r12.mVelocityTracker
            r5 = 1000(0x3e8, float:1.401E-42)
            int r6 = r12.mMaxFlingVelocity
            float r6 = (float) r6
            r13.computeCurrentVelocity(r5, r6)
            r13 = 0
            if (r0 == 0) goto L_0x0166
            android.view.VelocityTracker r0 = r12.mVelocityTracker
            int r5 = r12.mScrollPointerId
            float r0 = r0.getXVelocity(r5)
            float r0 = -r0
            goto L_0x0167
        L_0x0166:
            r0 = r13
        L_0x0167:
            if (r3 == 0) goto L_0x0173
            android.view.VelocityTracker r3 = r12.mVelocityTracker
            int r5 = r12.mScrollPointerId
            float r3 = r3.getYVelocity(r5)
            float r3 = -r3
            goto L_0x0174
        L_0x0173:
            r3 = r13
        L_0x0174:
            int r5 = (r0 > r13 ? 1 : (r0 == r13 ? 0 : -1))
            if (r5 != 0) goto L_0x017c
            int r13 = (r3 > r13 ? 1 : (r3 == r13 ? 0 : -1))
            if (r13 == 0) goto L_0x0184
        L_0x017c:
            int r13 = (int) r0
            int r0 = (int) r3
            boolean r13 = r12.fling(r13, r0)
            if (r13 != 0) goto L_0x0187
        L_0x0184:
            r12.setScrollState(r1)
        L_0x0187:
            r12.resetTouch()
            r1 = r2
            goto L_0x01b2
        L_0x018c:
            int r5 = r13.getPointerId(r1)
            r12.mScrollPointerId = r5
            float r5 = r13.getX()
            float r5 = r5 + r7
            int r5 = (int) r5
            r12.mLastTouchX = r5
            r12.mInitialTouchX = r5
            float r13 = r13.getY()
            float r13 = r13 + r7
            int r13 = (int) r13
            r12.mLastTouchY = r13
            r12.mInitialTouchY = r13
            if (r0 == 0) goto L_0x01aa
            r13 = r2
            goto L_0x01ab
        L_0x01aa:
            r13 = r1
        L_0x01ab:
            if (r3 == 0) goto L_0x01af
            r13 = r13 | 2
        L_0x01af:
            r12.startNestedScroll(r13)
        L_0x01b2:
            if (r1 != 0) goto L_0x01b9
            android.view.VelocityTracker r12 = r12.mVelocityTracker
            r12.addMovement(r4)
        L_0x01b9:
            r4.recycle()
            return r2
        L_0x01bd:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.recyclerview.RecyclerView.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private void resetTouch() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.clear();
        }
        stopNestedScroll();
        releaseGlows();
    }

    private void cancelTouch() {
        resetTouch();
        setScrollState(0);
    }

    private void onPointerUp(MotionEvent motionEvent) {
        int actionIndex = motionEvent.getActionIndex();
        if (motionEvent.getPointerId(actionIndex) == this.mScrollPointerId) {
            int i = actionIndex == 0 ? 1 : 0;
            this.mScrollPointerId = motionEvent.getPointerId(i);
            int x = (int) (motionEvent.getX(i) + 0.5f);
            this.mLastTouchX = x;
            this.mInitialTouchX = x;
            int y = (int) (motionEvent.getY(i) + 0.5f);
            this.mLastTouchY = y;
            this.mInitialTouchY = y;
        }
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (this.mLayout != null && !this.mLayoutFrozen && (motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8) {
            float f = this.mLayout.canScrollVertically() ? -motionEvent.getAxisValue(9) : 0.0f;
            float axisValue = this.mLayout.canScrollHorizontally() ? motionEvent.getAxisValue(10) : 0.0f;
            if (!(f == 0.0f && axisValue == 0.0f)) {
                float scrollFactor = getScrollFactor();
                scrollByInternal((int) (axisValue * scrollFactor), (int) (f * scrollFactor), motionEvent);
            }
        }
        return false;
    }

    private float getScrollFactor() {
        if (this.mScrollFactor == Float.MIN_VALUE) {
            TypedValue typedValue = new TypedValue();
            if (!getContext().getTheme().resolveAttribute(16842829, typedValue, true)) {
                return 0.0f;
            }
            this.mScrollFactor = typedValue.getDimension(getContext().getResources().getDisplayMetrics());
        }
        return this.mScrollFactor;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        if (this.mAdapterUpdateDuringMeasure) {
            eatRequestLayout();
            processAdapterUpdatesAndSetAnimationFlags();
            if (this.mState.mRunPredictiveAnimations) {
                this.mState.mInPreLayout = true;
            } else {
                this.mAdapterHelper.consumeUpdatesInOnePass();
                this.mState.mInPreLayout = false;
            }
            this.mAdapterUpdateDuringMeasure = false;
            resumeRequestLayout(false);
        }
        Adapter adapter = this.mAdapter;
        if (adapter != null) {
            this.mState.mItemCount = adapter.getItemCount();
        } else {
            this.mState.mItemCount = 0;
        }
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null) {
            defaultOnMeasure(i, i2);
        } else {
            layoutManager.doMeasure(this.mRecycler, this.mState, i, i2);
        }
        this.mState.mInPreLayout = false;
    }

    /* access modifiers changed from: private */
    public void defaultOnMeasure(int i, int i2) {
        int mode = MeasureSpec.getMode(i);
        int mode2 = MeasureSpec.getMode(i2);
        int size = MeasureSpec.getSize(i);
        int size2 = MeasureSpec.getSize(i2);
        if (!(mode == Integer.MIN_VALUE || mode == 1073741824)) {
            size = getMinimumWidth();
        }
        if (!(mode2 == Integer.MIN_VALUE || mode2 == 1073741824)) {
            size2 = getMinimumHeight();
        }
        setMeasuredDimension(size, size2);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i != i3 || i2 != i4) {
            invalidateGlows();
        }
    }

    /* access modifiers changed from: private */
    public void onEnterLayoutOrScroll() {
        this.mLayoutOrScrollCounter++;
    }

    /* access modifiers changed from: private */
    public void onExitLayoutOrScroll() {
        this.mLayoutOrScrollCounter--;
        if (this.mLayoutOrScrollCounter < 1) {
            this.mLayoutOrScrollCounter = 0;
            dispatchContentChangedIfNecessary();
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isAccessibilityEnabled() {
        AccessibilityManager accessibilityManager = this.mAccessibilityManager;
        return accessibilityManager != null && accessibilityManager.isEnabled();
    }

    private void dispatchContentChangedIfNecessary() {
        int i = this.mEatenAccessibilityChangeFlags;
        this.mEatenAccessibilityChangeFlags = 0;
        if (i != 0 && isAccessibilityEnabled()) {
            AccessibilityEvent obtain = AccessibilityEvent.obtain();
            obtain.setEventType(2048);
            obtain.setContentChangeTypes(i);
            sendAccessibilityEventUnchecked(obtain);
        }
    }

    public boolean isComputingLayout() {
        return this.mLayoutOrScrollCounter > 0;
    }

    /* access modifiers changed from: 0000 */
    public boolean shouldDeferAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (!isComputingLayout()) {
            return false;
        }
        int contentChangeTypes = accessibilityEvent != null ? accessibilityEvent.getContentChangeTypes() : 0;
        if (contentChangeTypes == 0) {
            contentChangeTypes = 0;
        }
        this.mEatenAccessibilityChangeFlags = contentChangeTypes | this.mEatenAccessibilityChangeFlags;
        return true;
    }

    public void sendAccessibilityEventUnchecked(AccessibilityEvent accessibilityEvent) {
        if (!shouldDeferAccessibilityEvent(accessibilityEvent)) {
            super.sendAccessibilityEventUnchecked(accessibilityEvent);
        }
    }

    /* access modifiers changed from: private */
    public boolean supportsChangeAnimations() {
        ItemAnimator itemAnimator = this.mItemAnimator;
        return itemAnimator != null && itemAnimator.getSupportsChangeAnimations();
    }

    private void postAnimationRunner() {
        if (!this.mPostedAnimatorRunner && this.mIsAttached) {
            postOnAnimation(this.mItemAnimatorRunner);
            this.mPostedAnimatorRunner = true;
        }
    }

    private boolean predictiveItemAnimationsEnabled() {
        return this.mItemAnimator != null && this.mLayout.supportsPredictiveItemAnimations();
    }

    private void processAdapterUpdatesAndSetAnimationFlags() {
        if (this.mDataSetHasChangedAfterLayout) {
            this.mAdapterHelper.reset();
            markKnownViewsInvalid();
            this.mLayout.onItemsChanged(this);
        }
        if (this.mItemAnimator == null || !this.mLayout.supportsPredictiveItemAnimations()) {
            this.mAdapterHelper.consumeUpdatesInOnePass();
        } else {
            this.mAdapterHelper.preProcess();
        }
        boolean z = false;
        boolean z2 = (this.mItemsAddedOrRemoved && !this.mItemsChanged) || this.mItemsAddedOrRemoved || (this.mItemsChanged && supportsChangeAnimations());
        this.mState.mRunSimpleAnimations = this.mFirstLayoutComplete && this.mItemAnimator != null && (this.mDataSetHasChangedAfterLayout || z2 || this.mLayout.mRequestedSimpleAnimations) && (!this.mDataSetHasChangedAfterLayout || this.mAdapter.hasStableIds());
        State state = this.mState;
        if (state.mRunSimpleAnimations && z2 && !this.mDataSetHasChangedAfterLayout && predictiveItemAnimationsEnabled()) {
            z = true;
        }
        state.mRunPredictiveAnimations = z;
    }

    /* access modifiers changed from: 0000 */
    public void dispatchLayout() {
        ArrayMap arrayMap;
        boolean z;
        String str = "RecyclerView";
        if (this.mAdapter == null) {
            Log.e(str, "No adapter attached; skipping layout");
        } else if (this.mLayout == null) {
            Log.e(str, "No layout manager attached; skipping layout");
        } else {
            this.mState.mDisappearingViewsInLayoutPass.clear();
            eatRequestLayout();
            onEnterLayoutOrScroll();
            processAdapterUpdatesAndSetAnimationFlags();
            State state = this.mState;
            state.mOldChangedHolders = (!state.mRunSimpleAnimations || !this.mItemsChanged || !supportsChangeAnimations()) ? null : new ArrayMap<>();
            this.mItemsChanged = false;
            this.mItemsAddedOrRemoved = false;
            State state2 = this.mState;
            state2.mInPreLayout = state2.mRunPredictiveAnimations;
            this.mState.mItemCount = this.mAdapter.getItemCount();
            findMinMaxChildLayoutPositions(this.mMinMaxLayoutPositions);
            if (this.mState.mRunSimpleAnimations) {
                this.mState.mPreLayoutHolderMap.clear();
                this.mState.mPostLayoutHolderMap.clear();
                int childCount = this.mChildHelper.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getChildAt(i));
                    if (!childViewHolderInt.shouldIgnore() && (!childViewHolderInt.isInvalid() || this.mAdapter.hasStableIds())) {
                        View view = childViewHolderInt.itemView;
                        ArrayMap<ViewHolder, ItemHolderInfo> arrayMap2 = this.mState.mPreLayoutHolderMap;
                        ItemHolderInfo itemHolderInfo = new ItemHolderInfo(childViewHolderInt, view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                        arrayMap2.put(childViewHolderInt, itemHolderInfo);
                    }
                }
            }
            if (this.mState.mRunPredictiveAnimations) {
                saveOldPositions();
                if (this.mState.mOldChangedHolders != null) {
                    int childCount2 = this.mChildHelper.getChildCount();
                    for (int i2 = 0; i2 < childCount2; i2++) {
                        ViewHolder childViewHolderInt2 = getChildViewHolderInt(this.mChildHelper.getChildAt(i2));
                        if (childViewHolderInt2.isChanged() && !childViewHolderInt2.isRemoved() && !childViewHolderInt2.shouldIgnore()) {
                            this.mState.mOldChangedHolders.put(Long.valueOf(getChangedHolderKey(childViewHolderInt2)), childViewHolderInt2);
                            this.mState.mPreLayoutHolderMap.remove(childViewHolderInt2);
                        }
                    }
                }
                boolean access$1400 = this.mState.mStructureChanged;
                this.mState.mStructureChanged = false;
                this.mLayout.onLayoutChildren(this.mRecycler, this.mState);
                this.mState.mStructureChanged = access$1400;
                arrayMap = new ArrayMap();
                for (int i3 = 0; i3 < this.mChildHelper.getChildCount(); i3++) {
                    View childAt = this.mChildHelper.getChildAt(i3);
                    if (!getChildViewHolderInt(childAt).shouldIgnore()) {
                        int i4 = 0;
                        while (true) {
                            if (i4 >= this.mState.mPreLayoutHolderMap.size()) {
                                z = false;
                                break;
                            } else if (((ViewHolder) this.mState.mPreLayoutHolderMap.keyAt(i4)).itemView == childAt) {
                                z = true;
                                break;
                            } else {
                                i4++;
                            }
                        }
                        if (!z) {
                            arrayMap.put(childAt, new Rect(childAt.getLeft(), childAt.getTop(), childAt.getRight(), childAt.getBottom()));
                        }
                    }
                }
                clearOldPositions();
                this.mAdapterHelper.consumePostponedUpdates();
            } else {
                clearOldPositions();
                this.mAdapterHelper.consumeUpdatesInOnePass();
                if (this.mState.mOldChangedHolders != null) {
                    int childCount3 = this.mChildHelper.getChildCount();
                    for (int i5 = 0; i5 < childCount3; i5++) {
                        ViewHolder childViewHolderInt3 = getChildViewHolderInt(this.mChildHelper.getChildAt(i5));
                        if (childViewHolderInt3.isChanged() && !childViewHolderInt3.isRemoved() && !childViewHolderInt3.shouldIgnore()) {
                            this.mState.mOldChangedHolders.put(Long.valueOf(getChangedHolderKey(childViewHolderInt3)), childViewHolderInt3);
                            this.mState.mPreLayoutHolderMap.remove(childViewHolderInt3);
                        }
                    }
                }
                arrayMap = null;
            }
            this.mState.mItemCount = this.mAdapter.getItemCount();
            this.mState.mDeletedInvisibleItemCountSincePreviousLayout = 0;
            this.mState.mInPreLayout = false;
            this.mLayout.onLayoutChildren(this.mRecycler, this.mState);
            this.mState.mStructureChanged = false;
            this.mPendingSavedState = null;
            State state3 = this.mState;
            state3.mRunSimpleAnimations = state3.mRunSimpleAnimations && this.mItemAnimator != null;
            if (this.mState.mRunSimpleAnimations) {
                SimpleArrayMap arrayMap3 = this.mState.mOldChangedHolders != null ? new ArrayMap() : null;
                int childCount4 = this.mChildHelper.getChildCount();
                for (int i6 = 0; i6 < childCount4; i6++) {
                    ViewHolder childViewHolderInt4 = getChildViewHolderInt(this.mChildHelper.getChildAt(i6));
                    if (!childViewHolderInt4.shouldIgnore()) {
                        View view2 = childViewHolderInt4.itemView;
                        long changedHolderKey = getChangedHolderKey(childViewHolderInt4);
                        if (arrayMap3 == null || this.mState.mOldChangedHolders.get(Long.valueOf(changedHolderKey)) == null) {
                            ArrayMap<ViewHolder, ItemHolderInfo> arrayMap4 = this.mState.mPostLayoutHolderMap;
                            ItemHolderInfo itemHolderInfo2 = r9;
                            ItemHolderInfo itemHolderInfo3 = new ItemHolderInfo(childViewHolderInt4, view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom());
                            arrayMap4.put(childViewHolderInt4, itemHolderInfo2);
                        } else {
                            arrayMap3.put(Long.valueOf(changedHolderKey), childViewHolderInt4);
                        }
                    }
                }
                processDisappearingList(arrayMap);
                for (int size = this.mState.mPreLayoutHolderMap.size() - 1; size >= 0; size--) {
                    if (!this.mState.mPostLayoutHolderMap.containsKey((ViewHolder) this.mState.mPreLayoutHolderMap.keyAt(size))) {
                        ItemHolderInfo itemHolderInfo4 = (ItemHolderInfo) this.mState.mPreLayoutHolderMap.valueAt(size);
                        this.mState.mPreLayoutHolderMap.removeAt(size);
                        ViewHolder viewHolder = itemHolderInfo4.holder;
                        View view3 = viewHolder.itemView;
                        this.mRecycler.unscrapView(viewHolder);
                        animateDisappearance(itemHolderInfo4);
                    }
                }
                int size2 = this.mState.mPostLayoutHolderMap.size();
                if (size2 > 0) {
                    for (int i7 = size2 - 1; i7 >= 0; i7--) {
                        ViewHolder viewHolder2 = (ViewHolder) this.mState.mPostLayoutHolderMap.keyAt(i7);
                        ItemHolderInfo itemHolderInfo5 = (ItemHolderInfo) this.mState.mPostLayoutHolderMap.valueAt(i7);
                        if (this.mState.mPreLayoutHolderMap.isEmpty() || !this.mState.mPreLayoutHolderMap.containsKey(viewHolder2)) {
                            this.mState.mPostLayoutHolderMap.removeAt(i7);
                            animateAppearance(viewHolder2, arrayMap != null ? (Rect) arrayMap.get(viewHolder2.itemView) : null, itemHolderInfo5.left, itemHolderInfo5.top);
                        }
                    }
                }
                int size3 = this.mState.mPostLayoutHolderMap.size();
                for (int i8 = 0; i8 < size3; i8++) {
                    ViewHolder viewHolder3 = (ViewHolder) this.mState.mPostLayoutHolderMap.keyAt(i8);
                    ItemHolderInfo itemHolderInfo6 = (ItemHolderInfo) this.mState.mPostLayoutHolderMap.valueAt(i8);
                    ItemHolderInfo itemHolderInfo7 = (ItemHolderInfo) this.mState.mPreLayoutHolderMap.get(viewHolder3);
                    if (!(itemHolderInfo7 == null || itemHolderInfo6 == null || (itemHolderInfo7.left == itemHolderInfo6.left && itemHolderInfo7.top == itemHolderInfo6.top))) {
                        viewHolder3.setIsRecyclable(false);
                        if (this.mItemAnimator.animateMove(viewHolder3, itemHolderInfo7.left, itemHolderInfo7.top, itemHolderInfo6.left, itemHolderInfo6.top)) {
                            postAnimationRunner();
                        }
                    }
                }
                ArrayMap<Long, ViewHolder> arrayMap5 = this.mState.mOldChangedHolders;
                for (int size4 = (arrayMap5 != null ? arrayMap5.size() : 0) - 1; size4 >= 0; size4--) {
                    long longValue = ((Long) this.mState.mOldChangedHolders.keyAt(size4)).longValue();
                    ViewHolder viewHolder4 = (ViewHolder) this.mState.mOldChangedHolders.get(Long.valueOf(longValue));
                    View view4 = viewHolder4.itemView;
                    if (!viewHolder4.shouldIgnore() && this.mRecycler.mChangedScrap != null && this.mRecycler.mChangedScrap.contains(viewHolder4)) {
                        animateChange(viewHolder4, (ViewHolder) arrayMap3.get(Long.valueOf(longValue)));
                    }
                }
            }
            resumeRequestLayout(false);
            this.mLayout.removeAndRecycleScrapInt(this.mRecycler);
            State state4 = this.mState;
            state4.mPreviousLayoutItemCount = state4.mItemCount;
            this.mDataSetHasChangedAfterLayout = false;
            this.mState.mRunSimpleAnimations = false;
            this.mState.mRunPredictiveAnimations = false;
            onExitLayoutOrScroll();
            this.mLayout.mRequestedSimpleAnimations = false;
            if (this.mRecycler.mChangedScrap != null) {
                this.mRecycler.mChangedScrap.clear();
            }
            this.mState.mOldChangedHolders = null;
            int[] iArr = this.mMinMaxLayoutPositions;
            if (didChildRangeChange(iArr[0], iArr[1])) {
                dispatchOnScrolled(0, 0);
            }
        }
    }

    private void findMinMaxChildLayoutPositions(int[] iArr) {
        int childCount = this.mChildHelper.getChildCount();
        if (childCount == 0) {
            iArr[0] = 0;
            iArr[1] = 0;
            return;
        }
        int i = Integer.MIN_VALUE;
        int i2 = Integer.MAX_VALUE;
        for (int i3 = 0; i3 < childCount; i3++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getChildAt(i3));
            if (!childViewHolderInt.shouldIgnore()) {
                int layoutPosition = childViewHolderInt.getLayoutPosition();
                if (layoutPosition < i2) {
                    i2 = layoutPosition;
                }
                if (layoutPosition > i) {
                    i = layoutPosition;
                }
            }
        }
        iArr[0] = i2;
        iArr[1] = i;
    }

    private boolean didChildRangeChange(int i, int i2) {
        int childCount = this.mChildHelper.getChildCount();
        boolean z = true;
        if (childCount == 0) {
            if (i == 0 && i2 == 0) {
                z = false;
            }
            return z;
        }
        for (int i3 = 0; i3 < childCount; i3++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getChildAt(i3));
            if (!childViewHolderInt.shouldIgnore()) {
                int layoutPosition = childViewHolderInt.getLayoutPosition();
                if (layoutPosition < i || layoutPosition > i2) {
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void removeDetachedView(View view, boolean z) {
        ViewHolder childViewHolderInt = getChildViewHolderInt(view);
        if (childViewHolderInt != null) {
            if (childViewHolderInt.isTmpDetached()) {
                childViewHolderInt.clearTmpDetachFlag();
            } else if (!childViewHolderInt.shouldIgnore()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Called removeDetachedView with a view which is not flagged as tmp detached.");
                sb.append(childViewHolderInt);
                throw new IllegalArgumentException(sb.toString());
            }
        }
        dispatchChildDetached(view);
        super.removeDetachedView(view, z);
    }

    /* access modifiers changed from: 0000 */
    public long getChangedHolderKey(ViewHolder viewHolder) {
        return this.mAdapter.hasStableIds() ? viewHolder.getItemId() : (long) viewHolder.mPosition;
    }

    private void processDisappearingList(ArrayMap<View, Rect> arrayMap) {
        List<View> list = this.mState.mDisappearingViewsInLayoutPass;
        for (int size = list.size() - 1; size >= 0; size--) {
            View view = (View) list.get(size);
            ViewHolder childViewHolderInt = getChildViewHolderInt(view);
            ItemHolderInfo itemHolderInfo = (ItemHolderInfo) this.mState.mPreLayoutHolderMap.remove(childViewHolderInt);
            if (!this.mState.isPreLayout()) {
                this.mState.mPostLayoutHolderMap.remove(childViewHolderInt);
            }
            if (arrayMap.remove(view) != null) {
                this.mLayout.removeAndRecycleView(view, this.mRecycler);
            } else if (itemHolderInfo != null) {
                animateDisappearance(itemHolderInfo);
            } else {
                ItemHolderInfo itemHolderInfo2 = new ItemHolderInfo(childViewHolderInt, view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                animateDisappearance(itemHolderInfo2);
            }
        }
        list.clear();
    }

    private void animateAppearance(ViewHolder viewHolder, Rect rect, int i, int i2) {
        View view = viewHolder.itemView;
        if (rect == null || (rect.left == i && rect.top == i2)) {
            viewHolder.setIsRecyclable(false);
            if (this.mItemAnimator.animateAdd(viewHolder)) {
                postAnimationRunner();
                return;
            }
            return;
        }
        viewHolder.setIsRecyclable(false);
        if (this.mItemAnimator.animateMove(viewHolder, rect.left, rect.top, i, i2)) {
            postAnimationRunner();
        }
    }

    private void animateDisappearance(ItemHolderInfo itemHolderInfo) {
        ViewHolder viewHolder = itemHolderInfo.holder;
        View view = viewHolder.itemView;
        addAnimatingView(viewHolder);
        int i = itemHolderInfo.left;
        int i2 = itemHolderInfo.top;
        int left = view.getLeft();
        int top = view.getTop();
        if (itemHolderInfo.holder.isRemoved() || (i == left && i2 == top)) {
            itemHolderInfo.holder.setIsRecyclable(false);
            if (this.mItemAnimator.animateRemove(itemHolderInfo.holder)) {
                postAnimationRunner();
                return;
            }
            return;
        }
        itemHolderInfo.holder.setIsRecyclable(false);
        view.layout(left, top, view.getWidth() + left, view.getHeight() + top);
        if (this.mItemAnimator.animateMove(itemHolderInfo.holder, i, i2, left, top)) {
            postAnimationRunner();
        }
    }

    private void animateChange(ViewHolder viewHolder, ViewHolder viewHolder2) {
        int i;
        int i2;
        viewHolder.setIsRecyclable(false);
        addAnimatingView(viewHolder);
        viewHolder.mShadowedHolder = viewHolder2;
        this.mRecycler.unscrapView(viewHolder);
        int left = viewHolder.itemView.getLeft();
        int top = viewHolder.itemView.getTop();
        if (viewHolder2 == null || viewHolder2.shouldIgnore()) {
            i2 = left;
            i = top;
        } else {
            int left2 = viewHolder2.itemView.getLeft();
            int top2 = viewHolder2.itemView.getTop();
            viewHolder2.setIsRecyclable(false);
            viewHolder2.mShadowingHolder = viewHolder;
            i2 = left2;
            i = top2;
        }
        if (this.mItemAnimator.animateChange(viewHolder, viewHolder2, left, top, i2, i)) {
            postAnimationRunner();
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        eatRequestLayout();
        Trace.beginSection("RV OnLayout");
        dispatchLayout();
        Trace.endSection();
        resumeRequestLayout(false);
        this.mFirstLayoutComplete = true;
    }

    public void requestLayout() {
        if (this.mEatRequestLayout || this.mLayoutFrozen) {
            this.mLayoutRequestEaten = true;
        } else {
            super.requestLayout();
        }
    }

    /* access modifiers changed from: 0000 */
    public void markItemDecorInsetsDirty() {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < unfilteredChildCount; i++) {
            ((LayoutParams) this.mChildHelper.getUnfilteredChildAt(i).getLayoutParams()).mInsetsDirty = true;
        }
        this.mRecycler.markItemDecorInsetsDirty();
    }

    public void draw(Canvas canvas) {
        boolean z;
        boolean z2;
        super.draw(canvas);
        int size = this.mItemDecorations.size();
        boolean z3 = false;
        for (int i = 0; i < size; i++) {
            ((ItemDecoration) this.mItemDecorations.get(i)).onDrawOver(canvas, this, this.mState);
        }
        EdgeEffect edgeEffect = this.mLeftGlow;
        if (edgeEffect == null || edgeEffect.isFinished()) {
            z = false;
        } else {
            int save = canvas.save();
            int paddingBottom = this.mClipToPadding ? getPaddingBottom() : 0;
            canvas.rotate(270.0f);
            canvas.translate((float) ((-getHeight()) + paddingBottom), 0.0f);
            EdgeEffect edgeEffect2 = this.mLeftGlow;
            z = edgeEffect2 != null && edgeEffect2.draw(canvas);
            canvas.restoreToCount(save);
        }
        EdgeEffect edgeEffect3 = this.mTopGlow;
        if (edgeEffect3 != null && !edgeEffect3.isFinished()) {
            int save2 = canvas.save();
            if (this.mClipToPadding) {
                canvas.translate((float) getPaddingLeft(), (float) getPaddingTop());
            }
            EdgeEffect edgeEffect4 = this.mTopGlow;
            z |= edgeEffect4 != null && edgeEffect4.draw(canvas);
            canvas.restoreToCount(save2);
        }
        EdgeEffect edgeEffect5 = this.mRightGlow;
        if (edgeEffect5 != null && !edgeEffect5.isFinished()) {
            int save3 = canvas.save();
            int width = getWidth();
            int paddingTop = this.mClipToPadding ? getPaddingTop() : 0;
            canvas.rotate(90.0f);
            canvas.translate((float) (-paddingTop), (float) (-width));
            EdgeEffect edgeEffect6 = this.mRightGlow;
            z |= edgeEffect6 != null && edgeEffect6.draw(canvas);
            canvas.restoreToCount(save3);
        }
        EdgeEffect edgeEffect7 = this.mBottomGlow;
        if (edgeEffect7 == null || edgeEffect7.isFinished()) {
            z2 = z;
        } else {
            int save4 = canvas.save();
            canvas.rotate(180.0f);
            if (this.mClipToPadding) {
                canvas.translate((float) ((-getWidth()) + getPaddingRight()), (float) ((-getHeight()) + getPaddingBottom()));
            } else {
                canvas.translate((float) (-getWidth()), (float) (-getHeight()));
            }
            EdgeEffect edgeEffect8 = this.mBottomGlow;
            if (edgeEffect8 != null && edgeEffect8.draw(canvas)) {
                z3 = true;
            }
            z2 = z3 | z;
            canvas.restoreToCount(save4);
        }
        if (!z2 && this.mItemAnimator != null && this.mItemDecorations.size() > 0 && this.mItemAnimator.isRunning()) {
            z2 = true;
        }
        if (z2) {
            postInvalidateOnAnimation();
        }
    }

    @SuppressLint({"WrongCall"})
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = this.mItemDecorations.size();
        for (int i = 0; i < size; i++) {
            ((ItemDecoration) this.mItemDecorations.get(i)).onDraw(canvas, this, this.mState);
        }
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return (layoutParams instanceof LayoutParams) && this.mLayout.checkLayoutParams((LayoutParams) layoutParams);
    }

    /* access modifiers changed from: protected */
    public android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            return layoutManager.generateDefaultLayoutParams();
        }
        throw new IllegalStateException("RecyclerView has no LayoutManager");
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            return layoutManager.generateLayoutParams(getContext(), attributeSet);
        }
        throw new IllegalStateException("RecyclerView has no LayoutManager");
    }

    /* access modifiers changed from: protected */
    public android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            return layoutManager.generateLayoutParams(layoutParams);
        }
        throw new IllegalStateException("RecyclerView has no LayoutManager");
    }

    /* access modifiers changed from: 0000 */
    public void saveOldPositions() {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < unfilteredChildCount; i++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i));
            if (!childViewHolderInt.shouldIgnore()) {
                childViewHolderInt.saveOldPosition();
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void clearOldPositions() {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < unfilteredChildCount; i++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i));
            if (!childViewHolderInt.shouldIgnore()) {
                childViewHolderInt.clearOldPosition();
            }
        }
        this.mRecycler.clearOldPositions();
    }

    /* access modifiers changed from: 0000 */
    public void offsetPositionRecordsForMove(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        if (i < i2) {
            i4 = i2;
            i3 = -1;
            i5 = i;
        } else {
            i4 = i;
            i5 = i2;
            i3 = 1;
        }
        for (int i6 = 0; i6 < unfilteredChildCount; i6++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i6));
            if (childViewHolderInt != null) {
                int i7 = childViewHolderInt.mPosition;
                if (i7 >= i5 && i7 <= i4) {
                    if (i7 == i) {
                        childViewHolderInt.offsetPosition(i2 - i, false);
                    } else {
                        childViewHolderInt.offsetPosition(i3, false);
                    }
                    this.mState.mStructureChanged = true;
                }
            }
        }
        this.mRecycler.offsetPositionRecordsForMove(i, i2);
        requestLayout();
    }

    /* access modifiers changed from: 0000 */
    public void offsetPositionRecordsForInsert(int i, int i2) {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i3 = 0; i3 < unfilteredChildCount; i3++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i3));
            if (childViewHolderInt != null && !childViewHolderInt.shouldIgnore() && childViewHolderInt.mPosition >= i) {
                childViewHolderInt.offsetPosition(i2, false);
                this.mState.mStructureChanged = true;
            }
        }
        this.mRecycler.offsetPositionRecordsForInsert(i, i2);
        requestLayout();
    }

    /* access modifiers changed from: 0000 */
    public void offsetPositionRecordsForRemove(int i, int i2, boolean z) {
        int i3 = i + i2;
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i4 = 0; i4 < unfilteredChildCount; i4++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i4));
            if (childViewHolderInt != null && !childViewHolderInt.shouldIgnore()) {
                int i5 = childViewHolderInt.mPosition;
                if (i5 >= i3) {
                    childViewHolderInt.offsetPosition(-i2, z);
                    this.mState.mStructureChanged = true;
                } else if (i5 >= i) {
                    childViewHolderInt.flagRemovedAndOffsetPosition(i - 1, -i2, z);
                    this.mState.mStructureChanged = true;
                }
            }
        }
        this.mRecycler.offsetPositionRecordsForRemove(i, i2, z);
        requestLayout();
    }

    /* access modifiers changed from: 0000 */
    public void viewRangeUpdate(int i, int i2, Object obj) {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        int i3 = i + i2;
        for (int i4 = 0; i4 < unfilteredChildCount; i4++) {
            View unfilteredChildAt = this.mChildHelper.getUnfilteredChildAt(i4);
            ViewHolder childViewHolderInt = getChildViewHolderInt(unfilteredChildAt);
            if (childViewHolderInt != null && !childViewHolderInt.shouldIgnore()) {
                int i5 = childViewHolderInt.mPosition;
                if (i5 >= i && i5 < i3) {
                    childViewHolderInt.addFlags(2);
                    childViewHolderInt.addChangePayload(obj);
                    if (supportsChangeAnimations()) {
                        childViewHolderInt.addFlags(64);
                    }
                    ((LayoutParams) unfilteredChildAt.getLayoutParams()).mInsetsDirty = true;
                }
            }
        }
        this.mRecycler.viewRangeUpdate(i, i2);
    }

    /* access modifiers changed from: 0000 */
    public void rebindUpdatedViewHolders() {
        int childCount = this.mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getChildAt(i));
            if (childViewHolderInt != null && !childViewHolderInt.shouldIgnore()) {
                if (childViewHolderInt.isRemoved() || childViewHolderInt.isInvalid()) {
                    requestLayout();
                } else if (!childViewHolderInt.needsUpdate()) {
                    continue;
                } else {
                    if (childViewHolderInt.getItemViewType() != this.mAdapter.getItemViewType(childViewHolderInt.mPosition)) {
                        requestLayout();
                        return;
                    } else if (!childViewHolderInt.isChanged() || !supportsChangeAnimations()) {
                        this.mAdapter.bindViewHolder(childViewHolderInt, childViewHolderInt.mPosition);
                    } else {
                        requestLayout();
                    }
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void markKnownViewsInvalid() {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < unfilteredChildCount; i++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i));
            if (childViewHolderInt != null && !childViewHolderInt.shouldIgnore()) {
                childViewHolderInt.addFlags(6);
            }
        }
        markItemDecorInsetsDirty();
        this.mRecycler.markKnownViewsInvalid();
    }

    public ViewHolder getChildViewHolder(View view) {
        ViewParent parent = view.getParent();
        if (parent == null || parent == this) {
            return getChildViewHolderInt(view);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("View ");
        sb.append(view);
        sb.append(" is not a direct child of ");
        sb.append(this);
        throw new IllegalArgumentException(sb.toString());
    }

    static ViewHolder getChildViewHolderInt(View view) {
        if (view == null) {
            return null;
        }
        return ((LayoutParams) view.getLayoutParams()).mViewHolder;
    }

    /* access modifiers changed from: 0000 */
    public ViewHolder findViewHolderForPosition(int i, boolean z) {
        int unfilteredChildCount = this.mChildHelper.getUnfilteredChildCount();
        for (int i2 = 0; i2 < unfilteredChildCount; i2++) {
            ViewHolder childViewHolderInt = getChildViewHolderInt(this.mChildHelper.getUnfilteredChildAt(i2));
            if (childViewHolderInt != null && !childViewHolderInt.isRemoved()) {
                if (z) {
                    if (childViewHolderInt.mPosition == i) {
                        return childViewHolderInt;
                    }
                } else if (childViewHolderInt.getLayoutPosition() == i) {
                    return childViewHolderInt;
                }
            }
        }
        return null;
    }

    public boolean drawChild(Canvas canvas, View view, long j) {
        return super.drawChild(canvas, view, j);
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnScrolled(int i, int i2) {
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);
        onScrolled(i, i2);
        OnScrollListener onScrollListener = this.mScrollListener;
        if (onScrollListener != null) {
            onScrollListener.onScrolled(this, i, i2);
        }
        List<OnScrollListener> list = this.mScrollListeners;
        if (list != null) {
            for (int size = list.size() - 1; size >= 0; size--) {
                ((OnScrollListener) this.mScrollListeners.get(size)).onScrolled(this, i, i2);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnScrollStateChanged(int i) {
        LayoutManager layoutManager = this.mLayout;
        if (layoutManager != null) {
            layoutManager.onScrollStateChanged(i);
        }
        onScrollStateChanged(i);
        OnScrollListener onScrollListener = this.mScrollListener;
        if (onScrollListener != null) {
            onScrollListener.onScrollStateChanged(this, i);
        }
        List<OnScrollListener> list = this.mScrollListeners;
        if (list != null) {
            for (int size = list.size() - 1; size >= 0; size--) {
                ((OnScrollListener) this.mScrollListeners.get(size)).onScrollStateChanged(this, i);
            }
        }
    }

    public boolean hasPendingAdapterUpdates() {
        return !this.mFirstLayoutComplete || this.mDataSetHasChangedAfterLayout || this.mAdapterHelper.hasPendingUpdates();
    }

    /* access modifiers changed from: private */
    public void dispatchChildDetached(View view) {
        ViewHolder childViewHolderInt = getChildViewHolderInt(view);
        onChildDetachedFromWindow(view);
        Adapter adapter = this.mAdapter;
        if (!(adapter == null || childViewHolderInt == null)) {
            adapter.onViewDetachedFromWindow(childViewHolderInt);
        }
        List<OnChildAttachStateChangeListener> list = this.mOnChildAttachStateListeners;
        if (list != null) {
            for (int size = list.size() - 1; size >= 0; size--) {
                ((OnChildAttachStateChangeListener) this.mOnChildAttachStateListeners.get(size)).onChildViewDetachedFromWindow(view);
            }
        }
    }

    /* access modifiers changed from: private */
    public void dispatchChildAttached(View view) {
        ViewHolder childViewHolderInt = getChildViewHolderInt(view);
        onChildAttachedToWindow(view);
        Adapter adapter = this.mAdapter;
        if (!(adapter == null || childViewHolderInt == null)) {
            adapter.onViewAttachedToWindow(childViewHolderInt);
        }
        List<OnChildAttachStateChangeListener> list = this.mOnChildAttachStateListeners;
        if (list != null) {
            for (int size = list.size() - 1; size >= 0; size--) {
                ((OnChildAttachStateChangeListener) this.mOnChildAttachStateListeners.get(size)).onChildViewAttachedToWindow(view);
            }
        }
    }

    public void setNestedScrollingEnabled(boolean z) {
        this.mScrollingChildHelper.setNestedScrollingEnabled(z);
    }

    public boolean isNestedScrollingEnabled() {
        return this.mScrollingChildHelper.isNestedScrollingEnabled();
    }

    public boolean startNestedScroll(int i) {
        return this.mScrollingChildHelper.startNestedScroll(i);
    }

    public void stopNestedScroll() {
        this.mScrollingChildHelper.stopNestedScroll();
    }

    public boolean hasNestedScrollingParent() {
        return this.mScrollingChildHelper.hasNestedScrollingParent();
    }

    public boolean dispatchNestedScroll(int i, int i2, int i3, int i4, int[] iArr) {
        return this.mScrollingChildHelper.dispatchNestedScroll(i, i2, i3, i4, iArr);
    }

    public boolean dispatchNestedPreScroll(int i, int i2, int[] iArr, int[] iArr2) {
        return this.mScrollingChildHelper.dispatchNestedPreScroll(i, i2, iArr, iArr2);
    }

    public boolean dispatchNestedFling(float f, float f2, boolean z) {
        return this.mScrollingChildHelper.dispatchNestedFling(f, f2, z);
    }

    public boolean dispatchNestedPreFling(float f, float f2) {
        return this.mScrollingChildHelper.dispatchNestedPreFling(f, f2);
    }

    /* access modifiers changed from: protected */
    public int getChildDrawingOrder(int i, int i2) {
        ChildDrawingOrderCallback childDrawingOrderCallback = this.mChildDrawingOrderCallback;
        if (childDrawingOrderCallback == null) {
            return super.getChildDrawingOrder(i, i2);
        }
        return childDrawingOrderCallback.onGetChildDrawingOrder(i, i2);
    }
}
