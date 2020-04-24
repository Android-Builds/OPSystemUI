package com.android.systemui.statusbar.notification.row;

import android.app.Notification.Action;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Rect;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.MediaTransferManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationCustomViewWrapper;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.InflatedSmartReplies;
import com.android.systemui.statusbar.policy.InflatedSmartReplies.SmartRepliesAndActions;
import com.android.systemui.statusbar.policy.RemoteInputView;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.statusbar.policy.SmartReplyView;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartActions;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartReplies;
import com.oneplus.plugin.OpLsState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class NotificationContentView extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("NotificationContentView", 3);
    private View mAmbientChild;
    private NotificationViewWrapper mAmbientWrapper;
    /* access modifiers changed from: private */
    public boolean mAnimate;
    /* access modifiers changed from: private */
    public int mAnimationStartVisibleType = -1;
    private boolean mBeforeN;
    private RemoteInputView mCachedExpandedRemoteInput;
    private RemoteInputView mCachedHeadsUpRemoteInput;
    private int mClipBottomAmount;
    private final Rect mClipBounds = new Rect();
    private boolean mClipToActualHeight = true;
    private int mClipTopAmount;
    private ExpandableNotificationRow mContainingNotification;
    private int mContentHeight;
    private int mContentHeightAtAnimationStart = -1;
    private View mContractedChild;
    private NotificationViewWrapper mContractedWrapper;
    private SmartRepliesAndActions mCurrentSmartRepliesAndActions;
    private boolean mDark;
    private final OnPreDrawListener mEnableAnimationPredrawListener = new OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationContentView.this.post(new Runnable() {
                public void run() {
                    NotificationContentView.this.mAnimate = true;
                }
            });
            NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };
    private OnClickListener mExpandClickListener;
    private boolean mExpandable;
    private View mExpandedChild;
    private InflatedSmartReplies mExpandedInflatedSmartReplies;
    private RemoteInputView mExpandedRemoteInput;
    private SmartReplyView mExpandedSmartReplyView;
    private Runnable mExpandedVisibleListener;
    private NotificationViewWrapper mExpandedWrapper;
    private boolean mFirstSeen = true;
    private boolean mFocusOnVisibilityChange;
    private boolean mForceSelectNextLayout = true;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private View mHeadsUpChild;
    private int mHeadsUpHeight;
    private InflatedSmartReplies mHeadsUpInflatedSmartReplies;
    private RemoteInputView mHeadsUpRemoteInput;
    private SmartReplyView mHeadsUpSmartReplyView;
    private NotificationViewWrapper mHeadsUpWrapper;
    private HybridGroupManager mHybridGroupManager = new HybridGroupManager(getContext(), this);
    private boolean mIconsVisible;
    private boolean mIsChildInGroup;
    private boolean mIsContentExpandable;
    private boolean mIsHeadsUp;
    private boolean mIsLowPriority;
    private boolean mIsQuickReply = false;
    private boolean mLegacy;
    private final Object mLock = new Object();
    private MediaTransferManager mMediaTransferManager = new MediaTransferManager(getContext());
    private int mMinContractedHeight;
    private int mNotificationAmbientHeight;
    private int mNotificationContentMarginEnd;
    private int mNotificationMaxHeight;
    private final ArrayMap<View, Runnable> mOnContentViewInactiveListeners = new ArrayMap<>();
    private PendingIntent mPreviousExpandedRemoteInputIntent;
    private PendingIntent mPreviousHeadsUpRemoteInputIntent;
    private View mQuickReplyChild;
    private NotificationViewWrapper mQuickReplyContractedWrapper;
    private View mQuickReplyHeadsUpChild;
    private NotificationViewWrapper mQuickReplyHeadsUpWrapper;
    private int mQuickReplyHeight;
    private RemoteInputController mRemoteInputController;
    private boolean mRemoteInputVisible;
    private HybridNotificationView mSingleLineView;
    private int mSingleLineWidthIndention;
    private int mSmallHeight;
    private SmartReplyConstants mSmartReplyConstants = ((SmartReplyConstants) Dependency.get(SmartReplyConstants.class));
    private SmartReplyController mSmartReplyController = ((SmartReplyController) Dependency.get(SmartReplyController.class));
    private StatusBarNotification mStatusBarNotification;
    private int mTransformationStartVisibleType;
    private int mUnrestrictedContentHeight;
    private boolean mUserExpanding;
    /* access modifiers changed from: private */
    public int mVisibleType = 0;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public NotificationContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }

    public void initView() {
        this.mMinContractedHeight = getResources().getDimensionPixelSize(R$dimen.min_notification_layout_height);
        this.mNotificationContentMarginEnd = getResources().getDimensionPixelSize(17105307);
        this.mQuickReplyHeight = this.mContext.getResources().getDimensionPixelSize(R$dimen.quick_reply_notification_height);
    }

    public void setHeights(int i, int i2, int i3, int i4) {
        this.mSmallHeight = i;
        this.mHeadsUpHeight = i2;
        this.mNotificationMaxHeight = i3;
        this.mNotificationAmbientHeight = i4;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        boolean z;
        boolean z2;
        int i4;
        int i5;
        boolean z3;
        int mode = MeasureSpec.getMode(i2);
        boolean z4 = true;
        boolean z5 = mode == 1073741824;
        int i6 = Integer.MIN_VALUE;
        boolean z6 = mode == Integer.MIN_VALUE;
        int i7 = 1073741823;
        int size = MeasureSpec.getSize(i);
        if (z5 || z6) {
            i7 = MeasureSpec.getSize(i2);
        }
        int i8 = i7;
        if (this.mExpandedChild != null) {
            int i9 = this.mNotificationMaxHeight;
            SmartReplyView smartReplyView = this.mExpandedSmartReplyView;
            if (smartReplyView != null) {
                i9 += smartReplyView.getHeightUpperLimit();
            }
            int extraMeasureHeight = i9 + this.mExpandedWrapper.getExtraMeasureHeight();
            int i10 = this.mExpandedChild.getLayoutParams().height;
            if (i10 >= 0) {
                extraMeasureHeight = Math.min(extraMeasureHeight, i10);
                z3 = true;
            } else {
                z3 = false;
            }
            measureChildWithMargins(this.mExpandedChild, i, 0, MeasureSpec.makeMeasureSpec(extraMeasureHeight, z3 ? 1073741824 : Integer.MIN_VALUE), 0);
            i3 = Math.max(0, this.mExpandedChild.getMeasuredHeight());
        } else {
            i3 = 0;
        }
        View view = this.mContractedChild;
        if (view != null) {
            int i11 = this.mSmallHeight;
            int i12 = view.getLayoutParams().height;
            if (i12 >= 0) {
                i11 = Math.min(i11, i12);
                z2 = true;
            } else {
                z2 = false;
            }
            if (shouldContractedBeFixedSize() || z2) {
                i4 = MeasureSpec.makeMeasureSpec(i11, 1073741824);
            } else {
                i4 = MeasureSpec.makeMeasureSpec(i11, Integer.MIN_VALUE);
            }
            int i13 = i4;
            measureChildWithMargins(this.mContractedChild, i, 0, i13, 0);
            int measuredHeight = this.mContractedChild.getMeasuredHeight();
            int i14 = this.mMinContractedHeight;
            if (measuredHeight < i14) {
                i5 = MeasureSpec.makeMeasureSpec(i14, 1073741824);
                measureChildWithMargins(this.mContractedChild, i, 0, i5, 0);
            } else {
                i5 = i13;
            }
            i3 = Math.max(i3, measuredHeight);
            if (updateContractedHeaderWidth()) {
                measureChildWithMargins(this.mContractedChild, i, 0, i5, 0);
            }
            if (this.mExpandedChild != null && this.mContractedChild.getMeasuredHeight() > this.mExpandedChild.getMeasuredHeight()) {
                measureChildWithMargins(this.mExpandedChild, i, 0, MeasureSpec.makeMeasureSpec(this.mContractedChild.getMeasuredHeight(), 1073741824), 0);
            }
        }
        if (this.mHeadsUpChild != null) {
            int i15 = this.mHeadsUpHeight;
            SmartReplyView smartReplyView2 = this.mHeadsUpSmartReplyView;
            if (smartReplyView2 != null) {
                i15 += smartReplyView2.getHeightUpperLimit();
            }
            int extraMeasureHeight2 = i15 + this.mHeadsUpWrapper.getExtraMeasureHeight();
            int i16 = this.mHeadsUpChild.getLayoutParams().height;
            if (i16 >= 0) {
                extraMeasureHeight2 = Math.min(extraMeasureHeight2, i16);
                z = true;
            } else {
                z = false;
            }
            measureChildWithMargins(this.mHeadsUpChild, i, 0, MeasureSpec.makeMeasureSpec(extraMeasureHeight2, z ? 1073741824 : Integer.MIN_VALUE), 0);
            i3 = Math.max(i3, this.mHeadsUpChild.getMeasuredHeight());
        }
        if (this.mSingleLineView != null) {
            this.mSingleLineView.measure((this.mSingleLineWidthIndention == 0 || MeasureSpec.getMode(i) == 0) ? i : MeasureSpec.makeMeasureSpec((size - this.mSingleLineWidthIndention) + this.mSingleLineView.getPaddingEnd(), 1073741824), MeasureSpec.makeMeasureSpec(this.mNotificationMaxHeight, Integer.MIN_VALUE));
            i3 = Math.max(i3, this.mSingleLineView.getMeasuredHeight());
        }
        View view2 = this.mAmbientChild;
        if (view2 != null) {
            int i17 = this.mNotificationAmbientHeight;
            int i18 = view2.getLayoutParams().height;
            if (i18 >= 0) {
                i17 = Math.min(i17, i18);
            } else {
                z4 = false;
            }
            View view3 = this.mAmbientChild;
            if (z4) {
                i6 = 1073741824;
            }
            view3.measure(i, MeasureSpec.makeMeasureSpec(i17, i6));
            i3 = Math.max(i3, this.mAmbientChild.getMeasuredHeight());
        } else {
            int i19 = i;
        }
        View view4 = this.mQuickReplyChild;
        if (view4 != null) {
            measureChildWithMargins(view4, i, 0, MeasureSpec.makeMeasureSpec(this.mQuickReplyHeight, 1073741824), 0);
            i3 = Math.max(i3, this.mQuickReplyChild.getMeasuredHeight());
        }
        View view5 = this.mQuickReplyHeadsUpChild;
        if (view5 != null) {
            measureChildWithMargins(view5, i, 0, MeasureSpec.makeMeasureSpec(this.mQuickReplyHeight, 1073741824), 0);
            i3 = Math.max(i3, this.mQuickReplyHeadsUpChild.getMeasuredHeight());
        }
        setMeasuredDimension(size, Math.min(i3, i8));
    }

    private int getExtraRemoteInputHeight(RemoteInputView remoteInputView) {
        if (remoteInputView == null || (!remoteInputView.isActive() && !remoteInputView.isSending())) {
            return 0;
        }
        return getResources().getDimensionPixelSize(17105306);
    }

    private boolean updateContractedHeaderWidth() {
        int i;
        NotificationHeaderView notificationHeader = this.mContractedWrapper.getNotificationHeader();
        if (notificationHeader != null) {
            if (this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null) {
                int i2 = this.mNotificationContentMarginEnd;
                if (notificationHeader.getPaddingEnd() != i2) {
                    if (notificationHeader.isLayoutRtl()) {
                        i = i2;
                    } else {
                        i = notificationHeader.getPaddingLeft();
                    }
                    int paddingTop = notificationHeader.getPaddingTop();
                    if (notificationHeader.isLayoutRtl()) {
                        i2 = notificationHeader.getPaddingLeft();
                    }
                    notificationHeader.setPadding(i, paddingTop, i2, notificationHeader.getPaddingBottom());
                    notificationHeader.setShowWorkBadgeAtEnd(false);
                    return true;
                }
            } else {
                int headerTextMarginEnd = this.mExpandedWrapper.getNotificationHeader().getHeaderTextMarginEnd();
                if (headerTextMarginEnd != notificationHeader.getHeaderTextMarginEnd()) {
                    notificationHeader.setHeaderTextMarginEnd(headerTextMarginEnd);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldContractedBeFixedSize() {
        return this.mBeforeN && (this.mContractedWrapper instanceof NotificationCustomViewWrapper);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View view = this.mExpandedChild;
        int height = view != null ? view.getHeight() : 0;
        super.onLayout(z, i, i2, i3, i4);
        if (!(height == 0 || this.mExpandedChild.getHeight() == height)) {
            this.mContentHeightAtAnimationStart = height;
        }
        updateClipping();
        invalidateOutline();
        selectLayout(false, this.mForceSelectNextLayout);
        this.mForceSelectNextLayout = false;
        updateExpandButtons(this.mExpandable);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public View getHeadsUpChild() {
        return this.mHeadsUpChild;
    }

    public View getAmbientChild() {
        return this.mAmbientChild;
    }

    public void setContractedChild(View view) {
        View view2 = this.mContractedChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mContractedChild);
        }
        if (view == null) {
            this.mContractedChild = null;
            this.mContractedWrapper = null;
            if (this.mTransformationStartVisibleType == 0) {
                this.mTransformationStartVisibleType = -1;
            }
            return;
        }
        addView(view);
        this.mContractedChild = view;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    private NotificationViewWrapper getWrapperForView(View view) {
        if (view == this.mContractedChild) {
            return this.mContractedWrapper;
        }
        if (view == this.mExpandedChild) {
            return this.mExpandedWrapper;
        }
        if (view == this.mHeadsUpChild) {
            return this.mHeadsUpWrapper;
        }
        if (view == this.mAmbientChild) {
            return this.mAmbientWrapper;
        }
        if (view == this.mQuickReplyChild) {
            return this.mQuickReplyContractedWrapper;
        }
        if (view == this.mQuickReplyHeadsUpChild) {
            return this.mQuickReplyHeadsUpWrapper;
        }
        return null;
    }

    public void setExpandedChild(View view) {
        if (this.mExpandedChild != null) {
            this.mPreviousExpandedRemoteInputIntent = null;
            RemoteInputView remoteInputView = this.mExpandedRemoteInput;
            if (remoteInputView != null) {
                remoteInputView.onNotificationUpdateOrReset();
                if (this.mExpandedRemoteInput.isActive()) {
                    this.mPreviousExpandedRemoteInputIntent = this.mExpandedRemoteInput.getPendingIntent();
                    RemoteInputView remoteInputView2 = this.mExpandedRemoteInput;
                    this.mCachedExpandedRemoteInput = remoteInputView2;
                    remoteInputView2.dispatchStartTemporaryDetach();
                    ((ViewGroup) this.mExpandedRemoteInput.getParent()).removeView(this.mExpandedRemoteInput);
                }
            }
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
            this.mExpandedRemoteInput = null;
        }
        if (view == null) {
            this.mExpandedChild = null;
            this.mExpandedWrapper = null;
            if (this.mTransformationStartVisibleType == 1) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 1) {
                selectLayout(false, true);
            }
            return;
        }
        addView(view);
        this.mExpandedChild = view;
        this.mExpandedWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void setHeadsUpChild(View view) {
        if (this.mHeadsUpChild != null) {
            this.mPreviousHeadsUpRemoteInputIntent = null;
            synchronized (this.mLock) {
                if (this.mHeadsUpRemoteInput != null) {
                    this.mHeadsUpRemoteInput.onNotificationUpdateOrReset();
                    if (this.mHeadsUpRemoteInput.isActive()) {
                        this.mPreviousHeadsUpRemoteInputIntent = this.mHeadsUpRemoteInput.getPendingIntent();
                        this.mCachedHeadsUpRemoteInput = this.mHeadsUpRemoteInput;
                        this.mHeadsUpRemoteInput.dispatchStartTemporaryDetach();
                        ((ViewGroup) this.mHeadsUpRemoteInput.getParent()).removeView(this.mHeadsUpRemoteInput);
                    }
                }
                this.mHeadsUpChild.animate().cancel();
                removeView(this.mHeadsUpChild);
                this.mHeadsUpRemoteInput = null;
            }
        }
        if (view == null) {
            this.mHeadsUpChild = null;
            this.mHeadsUpWrapper = null;
            if (this.mTransformationStartVisibleType == 2) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 2) {
                selectLayout(false, true);
            }
            return;
        }
        addView(view);
        this.mHeadsUpChild = view;
        this.mHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void setAmbientChild(View view) {
        View view2 = this.mAmbientChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mAmbientChild);
        }
        if (view == null) {
            this.mAmbientChild = null;
            this.mAmbientWrapper = null;
            if (this.mTransformationStartVisibleType == 4) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 4) {
                selectLayout(false, true);
            }
            return;
        }
        addView(view);
        this.mAmbientChild = view;
        this.mAmbientWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        view.setTag(R$id.row_tag_for_content_view, this.mContainingNotification);
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        updateVisibility();
        if (i != 0) {
            for (Runnable run : this.mOnContentViewInactiveListeners.values()) {
                run.run();
            }
            this.mOnContentViewInactiveListeners.clear();
        }
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
    }

    private void setVisible(boolean z) {
        if (z) {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
            return;
        }
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
        this.mAnimate = false;
    }

    private void focusExpandButtonIfNecessary() {
        if (this.mFocusOnVisibilityChange) {
            NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
            if (visibleNotificationHeader != null) {
                ImageView expandButton = visibleNotificationHeader.getExpandButton();
                if (expandButton != null) {
                    expandButton.requestAccessibilityFocus();
                }
            }
            this.mFocusOnVisibilityChange = false;
        }
    }

    public void setContentHeight(int i) {
        this.mUnrestrictedContentHeight = Math.max(i, getMinHeight());
        this.mContentHeight = Math.min(this.mUnrestrictedContentHeight, (this.mContainingNotification.getIntrinsicHeight() - getExtraRemoteInputHeight(this.mExpandedRemoteInput)) - getExtraRemoteInputHeight(this.mHeadsUpRemoteInput));
        selectLayout(this.mAnimate, false);
        if (this.mContractedChild != null) {
            int minContentHeightHint = getMinContentHeightHint();
            NotificationViewWrapper visibleWrapper = getVisibleWrapper(this.mVisibleType);
            if (visibleWrapper != null) {
                visibleWrapper.setContentHeight(this.mUnrestrictedContentHeight, minContentHeightHint);
            }
            NotificationViewWrapper visibleWrapper2 = getVisibleWrapper(this.mTransformationStartVisibleType);
            if (visibleWrapper2 != null) {
                visibleWrapper2.setContentHeight(this.mUnrestrictedContentHeight, minContentHeightHint);
            }
            updateClipping();
            invalidateOutline();
        }
    }

    private int getMinContentHeightHint() {
        int i;
        if (this.mIsChildInGroup && isVisibleOrTransitioning(3)) {
            return this.mContext.getResources().getDimensionPixelSize(17105297);
        }
        if (!(this.mHeadsUpChild == null || this.mExpandedChild == null)) {
            boolean z = isTransitioningFromTo(2, 1) || isTransitioningFromTo(1, 2);
            boolean z2 = !isVisibleOrTransitioning(0) && (this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && !this.mContainingNotification.isOnKeyguard();
            if (z || z2) {
                return Math.min(getViewHeight(2), getViewHeight(1));
            }
        }
        if (this.mVisibleType == 1) {
            int i2 = this.mContentHeightAtAnimationStart;
            if (i2 >= 0 && this.mExpandedChild != null) {
                return Math.min(i2, getViewHeight(1));
            }
        }
        if (this.mAmbientChild != null && isVisibleOrTransitioning(4)) {
            i = this.mAmbientChild.getHeight();
        } else if (this.mQuickReplyHeadsUpChild != null && isVisibleOrTransitioning(7)) {
            i = getViewHeight(7);
        } else if (this.mQuickReplyChild != null && isVisibleOrTransitioning(6)) {
            i = getViewHeight(6);
        } else if (this.mHeadsUpChild != null && isVisibleOrTransitioning(2)) {
            i = getViewHeight(2);
        } else if (this.mExpandedChild != null) {
            i = getViewHeight(1);
        } else {
            i = getViewHeight(0) + this.mContext.getResources().getDimensionPixelSize(17105297);
        }
        if (this.mExpandedChild != null && isVisibleOrTransitioning(1)) {
            i = Math.min(i, getViewHeight(1));
        }
        return i;
    }

    private boolean isTransitioningFromTo(int i, int i2) {
        return (this.mTransformationStartVisibleType == i || this.mAnimationStartVisibleType == i) && this.mVisibleType == i2;
    }

    private boolean isVisibleOrTransitioning(int i) {
        return this.mVisibleType == i || this.mTransformationStartVisibleType == i || this.mAnimationStartVisibleType == i;
    }

    private void updateContentTransformation() {
        int calculateVisibleType = calculateVisibleType();
        int i = this.mVisibleType;
        if (calculateVisibleType != i) {
            this.mTransformationStartVisibleType = i;
            TransformableView transformableViewForVisibleType = getTransformableViewForVisibleType(calculateVisibleType);
            TransformableView transformableViewForVisibleType2 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            if (transformableViewForVisibleType != null && transformableViewForVisibleType2 != null) {
                transformableViewForVisibleType.transformFrom(transformableViewForVisibleType2, 0.0f);
                getViewForVisibleType(calculateVisibleType).setVisibility(0);
                transformableViewForVisibleType2.transformTo(transformableViewForVisibleType, 0.0f);
                this.mVisibleType = calculateVisibleType;
                updateBackgroundColor(true);
            } else {
                return;
            }
        }
        if (this.mForceSelectNextLayout) {
            forceUpdateVisibilities();
        }
        int i2 = this.mTransformationStartVisibleType;
        if (i2 == -1 || this.mVisibleType == i2 || getViewForVisibleType(i2) == null) {
            updateViewVisibilities(calculateVisibleType);
            updateBackgroundColor(false);
        } else {
            TransformableView transformableViewForVisibleType3 = getTransformableViewForVisibleType(this.mVisibleType);
            TransformableView transformableViewForVisibleType4 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            if (transformableViewForVisibleType3 != null && transformableViewForVisibleType4 != null) {
                float calculateTransformationAmount = calculateTransformationAmount();
                transformableViewForVisibleType3.transformFrom(transformableViewForVisibleType4, calculateTransformationAmount);
                transformableViewForVisibleType4.transformTo(transformableViewForVisibleType3, calculateTransformationAmount);
                updateBackgroundTransformation(calculateTransformationAmount);
            }
        }
    }

    private void updateBackgroundTransformation(float f) {
        int backgroundColor = getBackgroundColor(this.mVisibleType);
        int backgroundColor2 = getBackgroundColor(this.mTransformationStartVisibleType);
        if (backgroundColor != backgroundColor2) {
            if (backgroundColor2 == 0) {
                backgroundColor2 = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            if (backgroundColor == 0) {
                backgroundColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            backgroundColor = NotificationUtils.interpolateColors(backgroundColor2, backgroundColor, f);
        }
        this.mContainingNotification.updateBackgroundAlpha(f);
        this.mContainingNotification.setContentBackground(backgroundColor, false, this);
    }

    private float calculateTransformationAmount() {
        int viewHeight = getViewHeight(this.mTransformationStartVisibleType);
        int viewHeight2 = getViewHeight(this.mVisibleType);
        int abs = Math.abs(this.mContentHeight - viewHeight);
        int abs2 = Math.abs(viewHeight2 - viewHeight);
        if (abs2 != 0) {
            return Math.min(1.0f, ((float) abs) / ((float) abs2));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("the total transformation distance is 0\n StartType: ");
        sb.append(this.mTransformationStartVisibleType);
        String str = " height: ";
        sb.append(str);
        sb.append(viewHeight);
        sb.append("\n VisibleType: ");
        sb.append(this.mVisibleType);
        sb.append(str);
        sb.append(viewHeight2);
        sb.append("\n mContentHeight: ");
        sb.append(this.mContentHeight);
        Log.wtf("NotificationContentView", sb.toString());
        return 1.0f;
    }

    public int getMaxHeight() {
        int viewHeight;
        int extraRemoteInputHeight;
        if (this.mExpandedChild != null) {
            viewHeight = getViewHeight(1);
            extraRemoteInputHeight = getExtraRemoteInputHeight(this.mExpandedRemoteInput);
        } else if (this.mIsQuickReply) {
            return getViewHeight(6);
        } else {
            if (this.mContainingNotification.isOnAmbient() && getShowingAmbientView() != null) {
                return getShowingAmbientView().getHeight();
            }
            if (this.mIsHeadsUp && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard()) {
                viewHeight = getViewHeight(2);
                extraRemoteInputHeight = getExtraRemoteInputHeight(this.mHeadsUpRemoteInput);
            } else if (this.mContractedChild != null) {
                return getViewHeight(0);
            } else {
                return this.mNotificationMaxHeight;
            }
        }
        return viewHeight + extraRemoteInputHeight;
    }

    private int getViewHeight(int i) {
        int i2;
        View viewForVisibleType = getViewForVisibleType(i);
        if (viewForVisibleType != null) {
            i2 = viewForVisibleType.getHeight();
        } else {
            int intrinsicHeight = this.mContainingNotification.getIntrinsicHeight();
            StringBuilder sb = new StringBuilder();
            sb.append("Fail to get view from visible type: ");
            sb.append(i);
            sb.append(", apply intrinsic height instead");
            Log.d("NotificationContentView", sb.toString());
            i2 = intrinsicHeight;
        }
        NotificationViewWrapper wrapperForView = getWrapperForView(viewForVisibleType);
        return wrapperForView != null ? i2 + wrapperForView.getHeaderTranslation() : i2;
    }

    public int getMinHeight() {
        return getMinHeight(false);
    }

    public int getMinHeight(boolean z) {
        if (this.mContainingNotification.isOnAmbient() && getShowingAmbientView() != null) {
            return getShowingAmbientView().getHeight();
        }
        if (this.mIsQuickReply && (!this.mIsChildInGroup || z || isGroupExpanded())) {
            return getViewHeight(6);
        }
        if (this.mIsHeadsUp && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard()) {
            return getViewHeight(2);
        }
        if (!z && this.mIsChildInGroup && !isGroupExpanded()) {
            return this.mSingleLineView.getHeight();
        }
        return this.mContractedChild != null ? getViewHeight(0) : this.mMinContractedHeight;
    }

    public View getShowingAmbientView() {
        View view = this.mIsChildInGroup ? this.mSingleLineView : this.mAmbientChild;
        if (view != null) {
            return view;
        }
        return this.mContractedChild;
    }

    private boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    public void setClipTopAmount(int i) {
        this.mClipTopAmount = i;
        updateClipping();
    }

    public void setClipBottomAmount(int i) {
        this.mClipBottomAmount = i;
        updateClipping();
    }

    public void setTranslationY(float f) {
        super.setTranslationY(f);
        updateClipping();
    }

    private void updateClipping() {
        if (this.mClipToActualHeight) {
            int translationY = (int) (((float) this.mClipTopAmount) - getTranslationY());
            this.mClipBounds.set(0, translationY, getWidth(), Math.max(translationY, (int) (((float) (this.mUnrestrictedContentHeight - this.mClipBottomAmount)) - getTranslationY())));
            setClipBounds(this.mClipBounds);
            return;
        }
        setClipBounds(null);
    }

    public void setClipToActualHeight(boolean z) {
        this.mClipToActualHeight = z;
        updateClipping();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0047, code lost:
        if (r9.getVisibility() == 0) goto L_0x0049;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void selectLayout(boolean r8, boolean r9) {
        /*
            r7 = this;
            android.view.View r0 = r7.mContractedChild
            if (r0 != 0) goto L_0x0005
            return
        L_0x0005:
            boolean r0 = r7.mUserExpanding
            if (r0 == 0) goto L_0x000e
            r7.updateContentTransformation()
            goto L_0x0096
        L_0x000e:
            int r0 = r7.calculateVisibleType()
            int r1 = r7.mVisibleType
            r2 = 1
            r3 = 0
            if (r0 == r1) goto L_0x001a
            r1 = r2
            goto L_0x001b
        L_0x001a:
            r1 = r3
        L_0x001b:
            if (r1 != 0) goto L_0x001f
            if (r9 == 0) goto L_0x0096
        L_0x001f:
            android.view.View r9 = r7.getViewForVisibleType(r0)
            r4 = 6
            r5 = 7
            if (r9 == 0) goto L_0x0052
            r9.setVisibility(r3)
            r7.transferRemoteInputFocus(r0)
            boolean r9 = r7.mFirstSeen
            if (r9 == 0) goto L_0x0052
            if (r0 == r5) goto L_0x0035
            if (r0 != r4) goto L_0x0052
        L_0x0035:
            android.view.View r9 = r7.mQuickReplyHeadsUpChild
            if (r9 == 0) goto L_0x003f
            int r9 = r9.getVisibility()
            if (r9 == 0) goto L_0x0049
        L_0x003f:
            android.view.View r9 = r7.mQuickReplyChild
            if (r9 == 0) goto L_0x0052
            int r9 = r9.getVisibility()
            if (r9 != 0) goto L_0x0052
        L_0x0049:
            java.lang.String r9 = "NotificationContentView"
            java.lang.String r6 = "Notification is displayed as Quick Reply style"
            android.util.Log.d(r9, r6)
            r7.mFirstSeen = r3
        L_0x0052:
            if (r8 == 0) goto L_0x007a
            if (r0 != r2) goto L_0x005a
            android.view.View r9 = r7.mExpandedChild
            if (r9 != 0) goto L_0x0076
        L_0x005a:
            r9 = 2
            if (r0 != r9) goto L_0x0061
            android.view.View r9 = r7.mHeadsUpChild
            if (r9 != 0) goto L_0x0076
        L_0x0061:
            r9 = 3
            if (r0 != r9) goto L_0x0068
            com.android.systemui.statusbar.notification.row.HybridNotificationView r9 = r7.mSingleLineView
            if (r9 != 0) goto L_0x0076
        L_0x0068:
            if (r0 != r4) goto L_0x006e
            android.view.View r9 = r7.mQuickReplyChild
            if (r9 != 0) goto L_0x0076
        L_0x006e:
            if (r0 != r5) goto L_0x0074
            android.view.View r9 = r7.mQuickReplyHeadsUpChild
            if (r9 != 0) goto L_0x0076
        L_0x0074:
            if (r0 != 0) goto L_0x007a
        L_0x0076:
            r7.animateToVisibleType(r0)
            goto L_0x007d
        L_0x007a:
            r7.updateViewVisibilities(r0)
        L_0x007d:
            r7.mVisibleType = r0
            if (r1 == 0) goto L_0x0084
            r7.focusExpandButtonIfNecessary()
        L_0x0084:
            com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper r9 = r7.getVisibleWrapper(r0)
            if (r9 == 0) goto L_0x0093
            int r0 = r7.mUnrestrictedContentHeight
            int r1 = r7.getMinContentHeightHint()
            r9.setContentHeight(r0, r1)
        L_0x0093:
            r7.updateBackgroundColor(r8)
        L_0x0096:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.row.NotificationContentView.selectLayout(boolean, boolean):void");
    }

    private void forceUpdateVisibilities() {
        forceUpdateVisibility(0, this.mContractedChild, this.mContractedWrapper);
        forceUpdateVisibility(1, this.mExpandedChild, this.mExpandedWrapper);
        forceUpdateVisibility(2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        HybridNotificationView hybridNotificationView = this.mSingleLineView;
        forceUpdateVisibility(3, hybridNotificationView, hybridNotificationView);
        forceUpdateVisibility(4, this.mAmbientChild, this.mAmbientWrapper);
        forceUpdateVisibility(6, this.mQuickReplyChild, this.mQuickReplyContractedWrapper);
        forceUpdateVisibility(7, this.mQuickReplyHeadsUpChild, this.mQuickReplyHeadsUpWrapper);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
    }

    private void fireExpandedVisibleListenerIfVisible() {
        if (this.mExpandedVisibleListener != null && this.mExpandedChild != null && isShown() && this.mExpandedChild.getVisibility() == 0) {
            Runnable runnable = this.mExpandedVisibleListener;
            this.mExpandedVisibleListener = null;
            runnable.run();
        }
    }

    private void forceUpdateVisibility(int i, View view, TransformableView transformableView) {
        if (view != null) {
            if (!(this.mVisibleType == i || this.mTransformationStartVisibleType == i)) {
                view.setVisibility(4);
            } else {
                transformableView.setVisible(true);
            }
        }
    }

    public void updateBackgroundColor(boolean z) {
        int backgroundColor = getBackgroundColor(this.mVisibleType);
        this.mContainingNotification.resetBackgroundAlpha();
        this.mContainingNotification.setContentBackground(backgroundColor, z, this);
    }

    public void setBackgroundTintColor(int i) {
        SmartReplyView smartReplyView = this.mExpandedSmartReplyView;
        if (smartReplyView != null) {
            smartReplyView.setBackgroundTintColor(i);
        }
        SmartReplyView smartReplyView2 = this.mHeadsUpSmartReplyView;
        if (smartReplyView2 != null) {
            smartReplyView2.setBackgroundTintColor(i);
        }
    }

    public int getVisibleType() {
        return this.mVisibleType;
    }

    public int getBackgroundColorForExpansionState() {
        int i;
        if (this.mContainingNotification.isGroupExpanded() || this.mContainingNotification.isUserLocked()) {
            i = calculateVisibleType();
        } else {
            i = getVisibleType();
        }
        return getBackgroundColor(i);
    }

    public int getBackgroundColor(int i) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(i);
        if (visibleWrapper != null) {
            return visibleWrapper.getCustomBackgroundColor();
        }
        return 0;
    }

    private void updateViewVisibilities(int i) {
        updateViewVisibility(i, 0, this.mContractedChild, this.mContractedWrapper);
        updateViewVisibility(i, 1, this.mExpandedChild, this.mExpandedWrapper);
        updateViewVisibility(i, 2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        HybridNotificationView hybridNotificationView = this.mSingleLineView;
        updateViewVisibility(i, 3, hybridNotificationView, hybridNotificationView);
        updateViewVisibility(i, 4, this.mAmbientChild, this.mAmbientWrapper);
        updateViewVisibility(i, 6, this.mQuickReplyChild, this.mQuickReplyContractedWrapper);
        updateViewVisibility(i, 7, this.mQuickReplyHeadsUpChild, this.mQuickReplyHeadsUpWrapper);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
    }

    private void updateViewVisibility(int i, int i2, View view, TransformableView transformableView) {
        if (view != null) {
            transformableView.setVisible(i == i2);
        }
    }

    private void animateToVisibleType(int i) {
        TransformableView transformableViewForVisibleType = getTransformableViewForVisibleType(i);
        final TransformableView transformableViewForVisibleType2 = getTransformableViewForVisibleType(this.mVisibleType);
        if (transformableViewForVisibleType == transformableViewForVisibleType2 || transformableViewForVisibleType2 == null) {
            transformableViewForVisibleType.setVisible(true);
            return;
        }
        this.mAnimationStartVisibleType = this.mVisibleType;
        transformableViewForVisibleType.transformFrom(transformableViewForVisibleType2);
        getViewForVisibleType(i).setVisibility(0);
        transformableViewForVisibleType2.transformTo(transformableViewForVisibleType, (Runnable) new Runnable() {
            public void run() {
                TransformableView transformableView = transformableViewForVisibleType2;
                NotificationContentView notificationContentView = NotificationContentView.this;
                if (transformableView != notificationContentView.getTransformableViewForVisibleType(notificationContentView.mVisibleType)) {
                    transformableViewForVisibleType2.setVisible(false);
                }
                NotificationContentView.this.mAnimationStartVisibleType = -1;
            }
        });
        fireExpandedVisibleListenerIfVisible();
    }

    private void transferRemoteInputFocus(int i) {
        if (i == 2 && this.mHeadsUpRemoteInput != null) {
            RemoteInputView remoteInputView = this.mExpandedRemoteInput;
            if (remoteInputView != null && remoteInputView.isActive()) {
                this.mHeadsUpRemoteInput.stealFocusFrom(this.mExpandedRemoteInput);
            }
        }
        if (i == 1 && this.mExpandedRemoteInput != null) {
            RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
            if (remoteInputView2 != null && remoteInputView2.isActive()) {
                this.mExpandedRemoteInput.stealFocusFrom(this.mHeadsUpRemoteInput);
            }
        }
    }

    /* access modifiers changed from: private */
    public TransformableView getTransformableViewForVisibleType(int i) {
        if (i == 1) {
            return this.mExpandedWrapper;
        }
        if (i == 2) {
            return this.mHeadsUpWrapper;
        }
        if (i == 3) {
            return this.mSingleLineView;
        }
        if (i == 4) {
            return this.mAmbientWrapper;
        }
        if (i == 6) {
            return this.mQuickReplyContractedWrapper;
        }
        if (i != 7) {
            return this.mContractedWrapper;
        }
        return this.mQuickReplyHeadsUpWrapper;
    }

    private View getViewForVisibleType(int i) {
        if (i == 1) {
            return this.mExpandedChild;
        }
        if (i == 2) {
            return this.mHeadsUpChild;
        }
        if (i == 3) {
            return this.mSingleLineView;
        }
        if (i == 4) {
            return this.mAmbientChild;
        }
        if (i == 6) {
            return this.mQuickReplyChild;
        }
        if (i != 7) {
            return this.mContractedChild;
        }
        return this.mQuickReplyHeadsUpChild;
    }

    public NotificationViewWrapper getVisibleWrapper(int i) {
        if (i == 0) {
            return this.mContractedWrapper;
        }
        if (i == 1) {
            return this.mExpandedWrapper;
        }
        if (i == 2) {
            return this.mHeadsUpWrapper;
        }
        if (i == 4) {
            return this.mAmbientWrapper;
        }
        if (i == 6) {
            return this.mQuickReplyContractedWrapper;
        }
        if (i != 7) {
            return null;
        }
        return this.mQuickReplyHeadsUpWrapper;
    }

    public int calculateVisibleType() {
        int i;
        int i2;
        if (this.mUserExpanding) {
            if (!this.mIsChildInGroup || isGroupExpanded() || this.mContainingNotification.isExpanded(true)) {
                i = this.mContainingNotification.getMaxContentHeight();
            } else {
                i = this.mContainingNotification.getShowingLayout().getMinHeight();
            }
            if (i == 0) {
                i = this.mContentHeight;
            }
            int visualTypeForHeight = getVisualTypeForHeight((float) i);
            if (!this.mIsChildInGroup || isGroupExpanded()) {
                i2 = getVisualTypeForHeight((float) this.mContainingNotification.getCollapsedHeight());
            } else {
                i2 = 3;
            }
            if (this.mTransformationStartVisibleType != i2) {
                visualTypeForHeight = i2;
            }
            return visualTypeForHeight;
        }
        int intrinsicHeight = this.mContainingNotification.getIntrinsicHeight();
        int i3 = this.mContentHeight;
        if (intrinsicHeight != 0) {
            i3 = Math.min(i3, intrinsicHeight);
        }
        return getVisualTypeForHeight((float) i3);
    }

    private int getVisualTypeForHeight(float f) {
        int i = 0;
        boolean z = this.mExpandedChild == null;
        if (!z && f == ((float) getViewHeight(1))) {
            return 1;
        }
        boolean isOnAmbient = this.mContainingNotification.isOnAmbient();
        if (!this.mUserExpanding && this.mIsChildInGroup && !isGroupExpanded()) {
            return 3;
        }
        if (this.mIsQuickReply) {
            if (this.mIsHeadsUp && this.mQuickReplyHeadsUpChild != null) {
                return 7;
            }
            if (this.mQuickReplyChild != null) {
                return 6;
            }
        }
        if ((!this.mIsHeadsUp && !this.mHeadsUpAnimatingAway) || this.mHeadsUpChild == null || this.mContainingNotification.isOnKeyguard()) {
            if (isOnAmbient && this.mAmbientChild != null) {
                i = 4;
            }
            if (z || (this.mContractedChild != null && f <= ((float) getViewHeight(i)) && (!this.mIsChildInGroup || isGroupExpanded() || !this.mContainingNotification.isExpanded(true)))) {
                return i;
            }
            return 1;
        } else if (f <= ((float) getViewHeight(2)) || z) {
            return 2;
        } else {
            return 1;
        }
    }

    public boolean isContentExpandable() {
        return this.mIsContentExpandable;
    }

    public void setDark(boolean z, boolean z2, long j) {
        if (this.mContractedChild != null) {
            this.mDark = z;
            selectLayout(!z && z2, false);
        }
    }

    public void setHeadsUp(boolean z) {
        this.mIsHeadsUp = z;
        selectLayout(false, true);
        updateExpandButtons(this.mExpandable);
    }

    public void setLegacy(boolean z) {
        this.mLegacy = z;
        updateLegacy();
    }

    private void updateLegacy() {
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setLegacy(this.mLegacy);
        }
        if (this.mQuickReplyChild != null) {
            this.mQuickReplyContractedWrapper.setLegacy(false);
        }
        if (this.mQuickReplyHeadsUpChild != null) {
            this.mQuickReplyHeadsUpWrapper.setLegacy(false);
        }
    }

    public void setIsChildInGroup(boolean z) {
        this.mIsChildInGroup = z;
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        updateAllSingleLineViews();
    }

    public void onNotificationUpdated(NotificationEntry notificationEntry) {
        this.mStatusBarNotification = notificationEntry.notification;
        this.mOnContentViewInactiveListeners.clear();
        this.mBeforeN = notificationEntry.targetSdk < 24;
        updateAllSingleLineViews();
        ExpandableNotificationRow row = notificationEntry.getRow();
        if (this.mContractedChild != null) {
            this.mContractedWrapper.onContentUpdated(row);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.onContentUpdated(row);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.onContentUpdated(row);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.onContentUpdated(row);
        }
        applyRemoteInputAndSmartReply(notificationEntry);
        applyMediaTransfer(notificationEntry);
        updateLegacy();
        this.mForceSelectNextLayout = true;
        setDark(this.mDark, false, 0);
        this.mPreviousExpandedRemoteInputIntent = null;
        this.mPreviousHeadsUpRemoteInputIntent = null;
    }

    private void updateAllSingleLineViews() {
        updateSingleLineView();
    }

    private void updateSingleLineView() {
        if (this.mIsChildInGroup) {
            boolean z = this.mSingleLineView == null;
            this.mSingleLineView = this.mHybridGroupManager.bindFromNotification(this.mSingleLineView, this.mStatusBarNotification.getNotification());
            if (z) {
                int i = this.mVisibleType;
                HybridNotificationView hybridNotificationView = this.mSingleLineView;
                updateViewVisibility(i, 3, hybridNotificationView, hybridNotificationView);
                return;
            }
            return;
        }
        HybridNotificationView hybridNotificationView2 = this.mSingleLineView;
        if (hybridNotificationView2 != null) {
            removeView(hybridNotificationView2);
            this.mSingleLineView = null;
        }
    }

    private void applyMediaTransfer(NotificationEntry notificationEntry) {
        View view = this.mExpandedChild;
        if (view != null && notificationEntry.isMediaNotification()) {
            View findViewById = view.findViewById(16909096);
            if (findViewById instanceof LinearLayout) {
                this.mMediaTransferManager.applyMediaTransferView((ViewGroup) findViewById, notificationEntry);
            }
        }
    }

    private void applyRemoteInputAndSmartReply(NotificationEntry notificationEntry) {
        SmartRepliesAndActions smartRepliesAndActions;
        int i;
        if (this.mRemoteInputController != null) {
            applyRemoteInput(notificationEntry, InflatedSmartReplies.hasFreeformRemoteInput(notificationEntry));
            String str = "NotificationContentView";
            if (this.mExpandedInflatedSmartReplies == null && this.mHeadsUpInflatedSmartReplies == null) {
                if (DEBUG) {
                    Log.d(str, "Both expanded, and heads-up InflatedSmartReplies are null, don't add smart replies.");
                }
                return;
            }
            InflatedSmartReplies inflatedSmartReplies = this.mExpandedInflatedSmartReplies;
            if (inflatedSmartReplies != null) {
                smartRepliesAndActions = inflatedSmartReplies.getSmartRepliesAndActions();
            } else {
                smartRepliesAndActions = this.mHeadsUpInflatedSmartReplies.getSmartRepliesAndActions();
            }
            this.mCurrentSmartRepliesAndActions = smartRepliesAndActions;
            if (DEBUG) {
                Object[] objArr = new Object[3];
                int i2 = 0;
                objArr[0] = notificationEntry.notification.getKey();
                SmartActions smartActions = this.mCurrentSmartRepliesAndActions.smartActions;
                if (smartActions == null) {
                    i = 0;
                } else {
                    i = smartActions.actions.size();
                }
                objArr[1] = Integer.valueOf(i);
                SmartReplies smartReplies = this.mCurrentSmartRepliesAndActions.smartReplies;
                if (smartReplies != null) {
                    i2 = smartReplies.choices.length;
                }
                objArr[2] = Integer.valueOf(i2);
                Log.d(str, String.format("Adding suggestions for %s, %d actions, and %d replies.", objArr));
            }
            applySmartReplyView(this.mCurrentSmartRepliesAndActions, notificationEntry);
        }
    }

    private void applyRemoteInput(NotificationEntry notificationEntry, boolean z) {
        View view = this.mExpandedChild;
        if (view != null) {
            this.mExpandedRemoteInput = applyRemoteInput(view, notificationEntry, z, this.mPreviousExpandedRemoteInputIntent, this.mCachedExpandedRemoteInput, this.mExpandedWrapper);
        } else {
            this.mExpandedRemoteInput = null;
        }
        RemoteInputView remoteInputView = this.mCachedExpandedRemoteInput;
        if (!(remoteInputView == null || remoteInputView == this.mExpandedRemoteInput)) {
            remoteInputView.dispatchFinishTemporaryDetach();
        }
        this.mCachedExpandedRemoteInput = null;
        View view2 = this.mHeadsUpChild;
        if (view2 != null) {
            this.mHeadsUpRemoteInput = applyRemoteInput(view2, notificationEntry, z, this.mPreviousHeadsUpRemoteInputIntent, this.mCachedHeadsUpRemoteInput, this.mHeadsUpWrapper);
        } else {
            this.mHeadsUpRemoteInput = null;
        }
        RemoteInputView remoteInputView2 = this.mCachedHeadsUpRemoteInput;
        if (!(remoteInputView2 == null || remoteInputView2 == this.mHeadsUpRemoteInput)) {
            remoteInputView2.dispatchFinishTemporaryDetach();
        }
        this.mCachedHeadsUpRemoteInput = null;
    }

    private RemoteInputView applyRemoteInput(View view, NotificationEntry notificationEntry, boolean z, PendingIntent pendingIntent, RemoteInputView remoteInputView, NotificationViewWrapper notificationViewWrapper) {
        View findViewById = view.findViewById(16908695);
        if (!(findViewById instanceof FrameLayout)) {
            return null;
        }
        RemoteInputView remoteInputView2 = (RemoteInputView) view.findViewWithTag(RemoteInputView.VIEW_TAG);
        if (remoteInputView2 != null) {
            remoteInputView2.onNotificationUpdateOrReset();
        }
        if (remoteInputView2 != null || !z) {
            remoteInputView = remoteInputView2;
        } else {
            FrameLayout frameLayout = (FrameLayout) findViewById;
            if (remoteInputView == null) {
                remoteInputView = RemoteInputView.inflate(this.mContext, frameLayout, notificationEntry, this.mRemoteInputController);
                remoteInputView.setVisibility(4);
                frameLayout.addView(remoteInputView, new LayoutParams(-1, -1));
            } else {
                frameLayout.addView(remoteInputView);
                remoteInputView.dispatchFinishTemporaryDetach();
                remoteInputView.requestFocus();
            }
        }
        if (z) {
            int i = notificationEntry.notification.getNotification().color;
            if (i == 0) {
                i = this.mContext.getColor(R$color.default_remote_input_background);
            }
            remoteInputView.setBackgroundColor(ContrastColorUtil.ensureTextBackgroundColor(i, this.mContext.getColor(R$color.remote_input_text_enabled), this.mContext.getColor(R$color.remote_input_hint)));
            remoteInputView.setWrapper(notificationViewWrapper);
            remoteInputView.setOnVisibilityChangedListener(new Consumer() {
                public final void accept(Object obj) {
                    NotificationContentView.this.setRemoteInputVisible(((Boolean) obj).booleanValue());
                }
            });
            if (pendingIntent != null || remoteInputView.isActive()) {
                Action[] actionArr = notificationEntry.notification.getNotification().actions;
                if (pendingIntent != null) {
                    remoteInputView.setPendingIntent(pendingIntent);
                }
                if (remoteInputView.updatePendingIntentFromActions(actionArr)) {
                    if (!remoteInputView.isActive()) {
                        remoteInputView.focus();
                    }
                } else if (remoteInputView.isActive()) {
                    remoteInputView.close();
                }
            }
        }
        return remoteInputView;
    }

    private void applySmartReplyView(SmartRepliesAndActions smartRepliesAndActions, NotificationEntry notificationEntry) {
        int i;
        int i2;
        boolean z;
        View view = this.mExpandedChild;
        if (view != null) {
            this.mExpandedSmartReplyView = applySmartReplyView(view, smartRepliesAndActions, notificationEntry, this.mExpandedInflatedSmartReplies);
            if (!(this.mExpandedSmartReplyView == null || (smartRepliesAndActions.smartReplies == null && smartRepliesAndActions.smartActions == null))) {
                SmartReplies smartReplies = smartRepliesAndActions.smartReplies;
                boolean z2 = false;
                if (smartReplies == null) {
                    i = 0;
                } else {
                    i = smartReplies.choices.length;
                }
                SmartActions smartActions = smartRepliesAndActions.smartActions;
                if (smartActions == null) {
                    i2 = 0;
                } else {
                    i2 = smartActions.actions.size();
                }
                SmartReplies smartReplies2 = smartRepliesAndActions.smartReplies;
                if (smartReplies2 == null) {
                    z = smartRepliesAndActions.smartActions.fromAssistant;
                } else {
                    z = smartReplies2.fromAssistant;
                }
                boolean z3 = z;
                SmartReplies smartReplies3 = smartRepliesAndActions.smartReplies;
                if (smartReplies3 != null && this.mSmartReplyConstants.getEffectiveEditChoicesBeforeSending(smartReplies3.remoteInput.getEditChoicesBeforeSending())) {
                    z2 = true;
                }
                this.mSmartReplyController.smartSuggestionsAdded(notificationEntry, i, i2, z3, z2);
            }
        }
        if (this.mHeadsUpChild != null && this.mSmartReplyConstants.getShowInHeadsUp()) {
            this.mHeadsUpSmartReplyView = applySmartReplyView(this.mHeadsUpChild, smartRepliesAndActions, notificationEntry, this.mHeadsUpInflatedSmartReplies);
        }
    }

    private SmartReplyView applySmartReplyView(View view, SmartRepliesAndActions smartRepliesAndActions, NotificationEntry notificationEntry, InflatedSmartReplies inflatedSmartReplies) {
        View findViewById = view.findViewById(16909374);
        SmartReplyView smartReplyView = null;
        if (!(findViewById instanceof LinearLayout)) {
            return null;
        }
        LinearLayout linearLayout = (LinearLayout) findViewById;
        if (!InflatedSmartReplies.shouldShowSmartReplyView(notificationEntry, smartRepliesAndActions)) {
            linearLayout.setVisibility(8);
            return null;
        }
        if (linearLayout.getChildCount() == 1 && (linearLayout.getChildAt(0) instanceof SmartReplyView)) {
            linearLayout.removeAllViews();
        }
        if (!(linearLayout.getChildCount() != 0 || inflatedSmartReplies == null || inflatedSmartReplies.getSmartReplyView() == null)) {
            smartReplyView = inflatedSmartReplies.getSmartReplyView();
            linearLayout.addView(smartReplyView);
        }
        if (smartReplyView != null) {
            smartReplyView.resetSmartSuggestions(linearLayout);
            smartReplyView.addPreInflatedButtons(inflatedSmartReplies.getSmartSuggestionButtons());
            smartReplyView.setBackgroundTintColor(notificationEntry.getRow().getCurrentBackgroundTint());
            linearLayout.setVisibility(0);
        }
        return smartReplyView;
    }

    public void setExpandedInflatedSmartReplies(InflatedSmartReplies inflatedSmartReplies) {
        this.mExpandedInflatedSmartReplies = inflatedSmartReplies;
        if (inflatedSmartReplies == null) {
            this.mExpandedSmartReplyView = null;
        }
    }

    public void setHeadsUpInflatedSmartReplies(InflatedSmartReplies inflatedSmartReplies) {
        this.mHeadsUpInflatedSmartReplies = inflatedSmartReplies;
        if (inflatedSmartReplies == null) {
            this.mHeadsUpSmartReplyView = null;
        }
    }

    public SmartRepliesAndActions getCurrentSmartRepliesAndActions() {
        return this.mCurrentSmartRepliesAndActions;
    }

    public void closeRemoteInput() {
        RemoteInputView remoteInputView = this.mHeadsUpRemoteInput;
        if (remoteInputView != null) {
            remoteInputView.close();
        }
        RemoteInputView remoteInputView2 = this.mExpandedRemoteInput;
        if (remoteInputView2 != null) {
            remoteInputView2.close();
        }
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
    }

    public void setRemoteInputController(RemoteInputController remoteInputController) {
        this.mRemoteInputController = remoteInputController;
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
    }

    public void updateExpandButtons(boolean z) {
        this.mExpandable = z;
        View view = this.mExpandedChild;
        if (!(view == null || view.getHeight() == 0 || ((this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard() ? this.mExpandedChild.getHeight() > this.mHeadsUpChild.getHeight() : this.mExpandedChild.getHeight() > this.mContractedChild.getHeight()))) {
            z = false;
        }
        if (this.mContainingNotification.isContentHidden() || (this.mVisibleType == 2 && OpLsState.getInstance().getPhoneStatusBar() != null && OpLsState.getInstance().getPhoneStatusBar().getPanel() != null && OpLsState.getInstance().getPhoneStatusBar().getPanel().isQsExpanded())) {
            z = false;
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.updateExpandability(z, this.mExpandClickListener);
        }
        if (this.mContractedChild != null) {
            this.mContractedWrapper.updateExpandability(z, this.mExpandClickListener);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.updateExpandability(z, this.mExpandClickListener);
        }
        this.mIsContentExpandable = z;
    }

    public NotificationHeaderView getNotificationHeader() {
        NotificationHeaderView notificationHeader = this.mContractedChild != null ? this.mContractedWrapper.getNotificationHeader() : null;
        if (notificationHeader == null && this.mExpandedChild != null) {
            notificationHeader = this.mExpandedWrapper.getNotificationHeader();
        }
        if (notificationHeader == null && this.mHeadsUpChild != null) {
            notificationHeader = this.mHeadsUpWrapper.getNotificationHeader();
        }
        return (notificationHeader != null || this.mAmbientChild == null) ? notificationHeader : this.mAmbientWrapper.getNotificationHeader();
    }

    public void showAppOpsIcons(ArraySet<Integer> arraySet) {
        if (!(this.mContractedChild == null || this.mContractedWrapper.getNotificationHeader() == null)) {
            this.mContractedWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
        if (!(this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null)) {
            this.mExpandedWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
        if (this.mHeadsUpChild != null && this.mHeadsUpWrapper.getNotificationHeader() != null) {
            this.mHeadsUpWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
    }

    public void setRecentlyAudiblyAlerted(boolean z) {
        if (!(this.mContractedChild == null || this.mContractedWrapper.getNotificationHeader() == null)) {
            this.mContractedWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
        if (!(this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null)) {
            this.mExpandedWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
        if (this.mHeadsUpChild != null && this.mHeadsUpWrapper.getNotificationHeader() != null) {
            this.mHeadsUpWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
    }

    public NotificationHeaderView getContractedNotificationHeader() {
        if (this.mContractedChild != null) {
            return this.mContractedWrapper.getNotificationHeader();
        }
        return null;
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(this.mVisibleType);
        if (visibleWrapper == null) {
            return null;
        }
        return visibleWrapper.getNotificationHeader();
    }

    public void setContainingNotification(ExpandableNotificationRow expandableNotificationRow) {
        this.mContainingNotification = expandableNotificationRow;
    }

    public void requestSelectLayout(boolean z) {
        selectLayout(z, false);
    }

    public void reInflateViews() {
        if (this.mIsChildInGroup) {
            HybridNotificationView hybridNotificationView = this.mSingleLineView;
            if (hybridNotificationView != null) {
                removeView(hybridNotificationView);
                this.mSingleLineView = null;
                updateAllSingleLineViews();
            }
        }
    }

    public void setUserExpanding(boolean z) {
        this.mUserExpanding = z;
        if (z) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            return;
        }
        this.mTransformationStartVisibleType = -1;
        this.mVisibleType = calculateVisibleType();
        updateViewVisibilities(this.mVisibleType);
        updateBackgroundColor(false);
    }

    public void setSingleLineWidthIndention(int i) {
        if (i != this.mSingleLineWidthIndention) {
            this.mSingleLineWidthIndention = i;
            this.mContainingNotification.forceLayout();
            forceLayout();
        }
    }

    public HybridNotificationView getSingleLineView() {
        return this.mSingleLineView;
    }

    public void setRemoved() {
        RemoteInputView remoteInputView = this.mExpandedRemoteInput;
        if (remoteInputView != null) {
            remoteInputView.setRemoved();
        }
        RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
        if (remoteInputView2 != null) {
            remoteInputView2.setRemoved();
        }
    }

    public void setContentHeightAnimating(boolean z) {
        if (!z) {
            this.mContentHeightAtAnimationStart = -1;
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean isAnimatingVisibleType() {
        return this.mAnimationStartVisibleType != -1;
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        selectLayout(false, true);
    }

    public void setFocusOnVisibilityChange() {
        this.mFocusOnVisibilityChange = true;
    }

    public void setIconsVisible(boolean z) {
        this.mIconsVisible = z;
        updateIconVisibilities();
    }

    private void updateIconVisibilities() {
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        if (notificationViewWrapper != null) {
            NotificationHeaderView notificationHeader = notificationViewWrapper.getNotificationHeader();
            if (notificationHeader != null) {
                notificationHeader.getIcon().setForceHidden(!this.mIconsVisible);
            }
        }
        NotificationViewWrapper notificationViewWrapper2 = this.mHeadsUpWrapper;
        if (notificationViewWrapper2 != null) {
            NotificationHeaderView notificationHeader2 = notificationViewWrapper2.getNotificationHeader();
            if (notificationHeader2 != null) {
                notificationHeader2.getIcon().setForceHidden(!this.mIconsVisible);
            }
        }
        NotificationViewWrapper notificationViewWrapper3 = this.mExpandedWrapper;
        if (notificationViewWrapper3 != null) {
            NotificationHeaderView notificationHeader3 = notificationViewWrapper3.getNotificationHeader();
            if (notificationHeader3 != null) {
                notificationHeader3.getIcon().setForceHidden(!this.mIconsVisible);
            }
        }
    }

    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (z) {
            fireExpandedVisibleListenerIfVisible();
        }
    }

    public void setOnExpandedVisibleListener(Runnable runnable) {
        this.mExpandedVisibleListener = runnable;
        fireExpandedVisibleListenerIfVisible();
    }

    public void performWhenContentInactive(int i, Runnable runnable) {
        View viewForVisibleType = getViewForVisibleType(i);
        if (viewForVisibleType == null || isContentViewInactive(i)) {
            runnable.run();
        } else {
            this.mOnContentViewInactiveListeners.put(viewForVisibleType, runnable);
        }
    }

    public boolean isContentViewInactive(int i) {
        return isContentViewInactive(getViewForVisibleType(i));
    }

    private boolean isContentViewInactive(View view) {
        boolean z = true;
        if (view == null) {
            return true;
        }
        if (isShown() && (view.getVisibility() == 0 || getViewForVisibleType(this.mVisibleType) == view)) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void onChildVisibilityChanged(View view, int i, int i2) {
        super.onChildVisibilityChanged(view, i, i2);
        if (isContentViewInactive(view)) {
            Runnable runnable = (Runnable) this.mOnContentViewInactiveListeners.remove(view);
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
    }

    public boolean isDimmable() {
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        return notificationViewWrapper != null && notificationViewWrapper.isDimmable();
    }

    public boolean disallowSingleClick(float f, float f2) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(getVisibleType());
        if (visibleWrapper != null) {
            return visibleWrapper.disallowSingleClick(f, f2);
        }
        return false;
    }

    public boolean shouldClipToRounding(boolean z, boolean z2) {
        boolean shouldClipToRounding = shouldClipToRounding(getVisibleType(), z, z2);
        return this.mUserExpanding ? shouldClipToRounding | shouldClipToRounding(this.mTransformationStartVisibleType, z, z2) : shouldClipToRounding;
    }

    private boolean shouldClipToRounding(int i, boolean z, boolean z2) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(i);
        if (visibleWrapper == null) {
            return false;
        }
        return visibleWrapper.shouldClipToRounding(z, z2);
    }

    public CharSequence getActiveRemoteInputText() {
        RemoteInputView remoteInputView = this.mExpandedRemoteInput;
        if (remoteInputView != null && remoteInputView.isActive()) {
            return this.mExpandedRemoteInput.getText();
        }
        RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
        if (remoteInputView2 == null || !remoteInputView2.isActive()) {
            return null;
        }
        return this.mHeadsUpRemoteInput.getText();
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        float y = motionEvent.getY();
        RemoteInputView remoteInputForView = getRemoteInputForView(getViewForVisibleType(this.mVisibleType));
        if (remoteInputForView != null && remoteInputForView.getVisibility() == 0) {
            int height = this.mUnrestrictedContentHeight - remoteInputForView.getHeight();
            if (y <= ((float) this.mUnrestrictedContentHeight) && y >= ((float) height)) {
                motionEvent.offsetLocation(0.0f, (float) (-height));
                return remoteInputForView.dispatchTouchEvent(motionEvent);
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public boolean pointInView(float f, float f2, float f3) {
        return f >= (-f3) && f2 >= ((float) this.mClipTopAmount) - f3 && f < ((float) (this.mRight - this.mLeft)) + f3 && f2 < ((float) this.mUnrestrictedContentHeight) + f3;
    }

    private RemoteInputView getRemoteInputForView(View view) {
        if (view == this.mExpandedChild) {
            return this.mExpandedRemoteInput;
        }
        if (view == this.mHeadsUpChild) {
            return this.mHeadsUpRemoteInput;
        }
        return null;
    }

    public int getExpandHeight() {
        return getViewHeight(this.mExpandedChild == null ? 0 : 1) + getExtraRemoteInputHeight(this.mExpandedRemoteInput);
    }

    public int getHeadsUpHeight() {
        return getViewHeight(this.mHeadsUpChild == null ? 0 : 2) + getExtraRemoteInputHeight(this.mHeadsUpRemoteInput) + getExtraRemoteInputHeight(this.mExpandedRemoteInput);
    }

    public void setRemoteInputVisible(boolean z) {
        this.mRemoteInputVisible = z;
        setClipChildren(!z);
    }

    public void setClipChildren(boolean z) {
        super.setClipChildren(z && !this.mRemoteInputVisible);
    }

    public void setHeaderVisibleAmount(float f) {
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        if (notificationViewWrapper != null) {
            notificationViewWrapper.setHeaderVisibleAmount(f);
        }
        NotificationViewWrapper notificationViewWrapper2 = this.mHeadsUpWrapper;
        if (notificationViewWrapper2 != null) {
            notificationViewWrapper2.setHeaderVisibleAmount(f);
        }
        NotificationViewWrapper notificationViewWrapper3 = this.mExpandedWrapper;
        if (notificationViewWrapper3 != null) {
            notificationViewWrapper3.setHeaderVisibleAmount(f);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("    ");
        StringBuilder sb = new StringBuilder();
        sb.append("contentView visibility: ");
        sb.append(getVisibility());
        printWriter.print(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        String str = ", alpha: ";
        sb2.append(str);
        sb2.append(getAlpha());
        printWriter.print(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        String str2 = ", clipBounds: ";
        sb3.append(str2);
        sb3.append(getClipBounds());
        printWriter.print(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append(", contentHeight: ");
        sb4.append(this.mContentHeight);
        printWriter.print(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append(", visibleType: ");
        sb5.append(this.mVisibleType);
        printWriter.print(sb5.toString());
        View viewForVisibleType = getViewForVisibleType(this.mVisibleType);
        printWriter.print(", visibleView ");
        if (viewForVisibleType != null) {
            StringBuilder sb6 = new StringBuilder();
            sb6.append(" visibility: ");
            sb6.append(viewForVisibleType.getVisibility());
            printWriter.print(sb6.toString());
            StringBuilder sb7 = new StringBuilder();
            sb7.append(str);
            sb7.append(viewForVisibleType.getAlpha());
            printWriter.print(sb7.toString());
            StringBuilder sb8 = new StringBuilder();
            sb8.append(str2);
            sb8.append(viewForVisibleType.getClipBounds());
            printWriter.print(sb8.toString());
        } else {
            printWriter.print("null");
        }
        printWriter.println();
    }

    public RemoteInputView getExpandedRemoteInput() {
        return this.mExpandedRemoteInput;
    }

    public void setQuickReplyContractedChild(View view) {
        View view2 = this.mQuickReplyChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mQuickReplyChild);
        }
        addView(view);
        this.mQuickReplyChild = view;
        this.mQuickReplyContractedWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void setQuickReplyHeadsUpChild(View view) {
        View view2 = this.mQuickReplyHeadsUpChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mQuickReplyHeadsUpChild);
        }
        if (view == null) {
            this.mQuickReplyHeadsUpChild = null;
            this.mQuickReplyHeadsUpWrapper = null;
            if (this.mVisibleType == 7) {
                this.mVisibleType = 0;
            }
            if (this.mTransformationStartVisibleType == 7) {
                this.mTransformationStartVisibleType = -1;
            }
            return;
        }
        addView(view);
        this.mQuickReplyHeadsUpChild = view;
        this.mQuickReplyHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public boolean isQuickReply() {
        return this.mIsQuickReply;
    }

    public void setQuickReply(boolean z) {
        this.mIsQuickReply = z;
    }

    public View getQuickReplyContractedChild() {
        return this.mQuickReplyChild;
    }

    public View getQuickReplyHeadsUpChild() {
        return this.mQuickReplyHeadsUpChild;
    }

    public int getQuickReplyHeight() {
        return getViewHeight(this.mQuickReplyChild == null ? 0 : 6);
    }

    public int getQuickReplyHeadsUpHeight() {
        return getViewHeight(this.mQuickReplyHeadsUpChild == null ? 0 : 7);
    }
}
