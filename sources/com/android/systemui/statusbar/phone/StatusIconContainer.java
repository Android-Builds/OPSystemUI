package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.notification.stack.AnimationFilter;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.ViewState;
import com.oneplus.systemui.statusbar.phone.OpStatusIconContainer;
import java.util.ArrayList;

public class StatusIconContainer extends OpStatusIconContainer {
    /* access modifiers changed from: private */
    public static final AnimationProperties ADD_ICON_PROPERTIES;
    private static final AnimationProperties ANIMATE_ALL_PROPERTIES;
    /* access modifiers changed from: private */
    public static final AnimationProperties X_ANIMATION_PROPERTIES;
    private final int DELAY_INTERVAL;
    private final int MAX_RETRY_TIMES;
    private int mDotPadding;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private int mIconDotFrameWidth;
    private ArrayList<String> mIgnoredSlots;
    private ArrayList<StatusIconState> mLayoutStates;
    private ArrayList<View> mMeasureViews;
    private boolean mNeedsUnderflow;
    private String mOpTag;
    /* access modifiers changed from: private */
    public Runnable mReRequestLayout;
    /* access modifiers changed from: private */
    public int mReRequestLayoutTimes;
    private boolean mShouldRestrictIcons;
    private int mStaticDotDiameter;
    private StatusBar mStatusBar;
    private int mUnderflowStart;
    private int mUnderflowWidth;

    public static class StatusIconState extends ViewState {
        float distanceToViewEnd = -1.0f;
        public boolean justAdded = true;
        public int visibleState = 0;

        public void applyToView(View view) {
            float width = (view.getParent() instanceof View ? (float) ((View) view.getParent()).getWidth() : 0.0f) - this.xTranslation;
            if (view instanceof StatusIconDisplayable) {
                StatusIconDisplayable statusIconDisplayable = (StatusIconDisplayable) view;
                AnimationProperties animationProperties = null;
                if (this.justAdded || (statusIconDisplayable.getVisibleState() == 2 && this.visibleState == 0)) {
                    super.applyToView(view);
                    animationProperties = StatusIconContainer.ADD_ICON_PROPERTIES;
                } else if (!(this.visibleState == 2 || this.distanceToViewEnd == width)) {
                    animationProperties = StatusIconContainer.X_ANIMATION_PROPERTIES;
                }
                statusIconDisplayable.setVisibleState(this.visibleState, true);
                if (animationProperties != null) {
                    animateTo(view, animationProperties);
                } else {
                    super.applyToView(view);
                }
                this.justAdded = false;
                this.distanceToViewEnd = width;
            }
        }
    }

    public void setOpTag(String str) {
        this.mOpTag = str;
    }

    public StatusIconContainer(Context context) {
        this(context, null);
    }

    public StatusIconContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.DELAY_INTERVAL = 100;
        this.mReRequestLayoutTimes = 0;
        this.MAX_RETRY_TIMES = 2;
        this.mUnderflowStart = 0;
        this.mShouldRestrictIcons = true;
        this.mLayoutStates = new ArrayList<>();
        this.mMeasureViews = new ArrayList<>();
        this.mIgnoredSlots = new ArrayList<>();
        this.mOpTag = "";
        this.mReRequestLayout = new Runnable() {
            public void run() {
                StatusIconContainer.this.mHandler.removeCallbacks(StatusIconContainer.this.mReRequestLayout);
                StatusIconContainer.this.mReRequestLayoutTimes = StatusIconContainer.this.mReRequestLayoutTimes + 1;
                boolean z = StatusIconContainer.this.mReRequestLayoutTimes >= 2;
                StringBuilder sb = new StringBuilder();
                sb.append("mReRequestLayout, mReRequestLayoutTimes:");
                sb.append(StatusIconContainer.this.mReRequestLayoutTimes);
                sb.append(", timeout:");
                sb.append(z);
                Log.i("StatusIconContainer", sb.toString());
                if (!StatusIconContainer.this.isLayoutRequested() || z) {
                    StatusIconContainer.this.mReRequestLayoutTimes = 0;
                    StatusIconContainer.this.requestLayout();
                    return;
                }
                StatusIconContainer.this.mHandler.postDelayed(StatusIconContainer.this.mReRequestLayout, 100);
            }
        };
        this.mHandler = new Handler();
        initDimens();
        setWillNotDraw(true);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setShouldRestrictIcons(boolean z) {
        this.mShouldRestrictIcons = z;
    }

    public boolean isRestrictingIcons() {
        return this.mShouldRestrictIcons;
    }

    private void initDimens() {
        this.mIconDotFrameWidth = getResources().getDimensionPixelSize(17105425);
        this.mDotPadding = getResources().getDimensionPixelSize(R$dimen.overflow_icon_dot_padding);
        this.mStaticDotDiameter = getResources().getDimensionPixelSize(R$dimen.overflow_dot_radius) * 2;
        this.mUnderflowWidth = setUnderflowWidth(this.mIconDotFrameWidth, this.mStaticDotDiameter, this.mDotPadding);
    }

    private int getOpWidth() {
        return getOpMaxWidth(getWidth());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:5:0x001b, code lost:
        if ("demo_status_icon_container".equals(r2.mOpTag) != false) goto L_0x001d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getOpMaxWidth(int r3) {
        /*
            r2 = this;
            java.lang.String r0 = r2.mOpTag
            boolean r0 = r0.isEmpty()
            if (r0 != 0) goto L_0x0038
            java.lang.String r0 = r2.mOpTag
            java.lang.String r1 = "status_icon_container"
            boolean r0 = r1.equals(r0)
            if (r0 != 0) goto L_0x001d
            java.lang.String r0 = r2.mOpTag
            java.lang.String r1 = "demo_status_icon_container"
            boolean r0 = r1.equals(r0)
            if (r0 == 0) goto L_0x0038
        L_0x001d:
            com.android.systemui.statusbar.phone.StatusBar r0 = r2.mStatusBar
            if (r0 != 0) goto L_0x002d
            android.content.Context r0 = r2.mContext
            java.lang.Class<com.android.systemui.statusbar.phone.StatusBar> r1 = com.android.systemui.statusbar.phone.StatusBar.class
            java.lang.Object r0 = com.android.systemui.SysUiServiceProvider.getComponent(r0, r1)
            com.android.systemui.statusbar.phone.StatusBar r0 = (com.android.systemui.statusbar.phone.StatusBar) r0
            r2.mStatusBar = r0
        L_0x002d:
            com.android.systemui.statusbar.phone.StatusBar r2 = r2.mStatusBar
            int r2 = r2.getSystemIconAreaMaxWidth(r3)
            if (r2 <= 0) goto L_0x0038
            if (r3 <= r2) goto L_0x0038
            goto L_0x0039
        L_0x0038:
            r2 = r3
        L_0x0039:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusIconContainer.getOpMaxWidth(int):int");
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        float height = ((float) getHeight()) / 2.0f;
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            int measuredWidth = childAt.getMeasuredWidth();
            int measuredHeight = childAt.getMeasuredHeight();
            int i6 = (int) (height - (((float) measuredHeight) / 2.0f));
            childAt.layout(0, i6, measuredWidth, measuredHeight + i6);
        }
        resetViewStates();
        calculateIconTranslations();
        applyIconStates();
        if (getWidth() > getOpWidth()) {
            StringBuilder sb = new StringBuilder();
            sb.append("onLayout, last, getWidth() > getOpWidth(),  getWidth():");
            sb.append(getWidth());
            sb.append(", getMeasuredWidth:");
            sb.append(getMeasuredWidth());
            sb.append(", getOpWidth():");
            sb.append(getOpWidth());
            sb.append(", getParent().getParent():");
            sb.append(getParent() != null ? getParent().getParent() : "Null");
            Log.i("StatusIconContainer", sb.toString());
            this.mHandler.postDelayed(this.mReRequestLayout, 100);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        this.mMeasureViews.clear();
        int mode = MeasureSpec.getMode(i);
        int opMaxWidth = getOpMaxWidth(MeasureSpec.getSize(i));
        int childCount = getChildCount();
        for (int i4 = 0; i4 < childCount; i4++) {
            StatusIconDisplayable statusIconDisplayable = (StatusIconDisplayable) getChildAt(i4);
            if (statusIconDisplayable.isIconVisible() && !statusIconDisplayable.isIconBlocked() && !this.mIgnoredSlots.contains(statusIconDisplayable.getSlot())) {
                this.mMeasureViews.add((View) statusIconDisplayable);
            }
        }
        int size = this.mMeasureViews.size();
        int i5 = size <= 70 ? 70 : 69;
        int i6 = this.mPaddingLeft + this.mPaddingRight;
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(opMaxWidth, 0);
        this.mNeedsUnderflow = this.mShouldRestrictIcons && size > 70;
        boolean z = true;
        for (int i7 = 0; i7 < this.mMeasureViews.size(); i7++) {
            View view = (View) this.mMeasureViews.get((size - i7) - 1);
            measureChild(view, makeMeasureSpec, i2);
            if (!this.mShouldRestrictIcons) {
                i3 = getViewTotalMeasuredWidth(view);
            } else if (i7 >= i5 || !z) {
                if (z) {
                    i6 += this.mUnderflowWidth;
                    z = false;
                }
            } else {
                i3 = getViewTotalMeasuredWidth(view);
            }
            i6 += i3;
        }
        if (mode == 1073741824) {
            if (!this.mNeedsUnderflow && i6 > opMaxWidth) {
                this.mNeedsUnderflow = true;
            }
            setMeasuredDimension(opMaxWidth, MeasureSpec.getSize(i2));
            return;
        }
        if (mode != Integer.MIN_VALUE || i6 <= opMaxWidth) {
            opMaxWidth = i6;
        } else {
            this.mNeedsUnderflow = true;
        }
        setMeasuredDimension(opMaxWidth, MeasureSpec.getSize(i2));
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        StatusIconState statusIconState = new StatusIconState();
        statusIconState.justAdded = true;
        view.setTag(R$id.status_bar_view_state_tag, statusIconState);
    }

    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        view.setTag(R$id.status_bar_view_state_tag, null);
    }

    private void calculateIconTranslations() {
        int i;
        this.mLayoutStates.clear();
        float opWidth = (float) getOpWidth();
        float paddingEnd = opWidth - ((float) getPaddingEnd());
        float paddingStart = (float) getPaddingStart();
        int childCount = getChildCount();
        int i2 = childCount - 1;
        while (true) {
            if (i2 < 0) {
                break;
            }
            View childAt = getChildAt(i2);
            StatusIconDisplayable statusIconDisplayable = (StatusIconDisplayable) childAt;
            StatusIconState viewStateFromChild = getViewStateFromChild(childAt);
            if (!statusIconDisplayable.isIconVisible() || statusIconDisplayable.isIconBlocked() || this.mIgnoredSlots.contains(statusIconDisplayable.getSlot())) {
                viewStateFromChild.visibleState = 2;
            } else {
                viewStateFromChild.visibleState = 0;
                viewStateFromChild.xTranslation = paddingEnd - ((float) getViewTotalWidth(childAt));
                this.mLayoutStates.add(0, viewStateFromChild);
                paddingEnd -= (float) getViewTotalWidth(childAt);
            }
            i2--;
        }
        int size = this.mLayoutStates.size();
        int i3 = 70;
        if (size > 70) {
            i3 = 69;
        }
        this.mUnderflowStart = 0;
        int i4 = size - 1;
        int i5 = 0;
        while (true) {
            if (i4 < 0) {
                i4 = -1;
                break;
            }
            StatusIconState statusIconState = (StatusIconState) this.mLayoutStates.get(i4);
            if ((this.mNeedsUnderflow && statusIconState.xTranslation < ((float) this.mUnderflowWidth) + paddingStart) || (this.mShouldRestrictIcons && i5 >= i3)) {
                break;
            }
            this.mUnderflowStart = (int) Math.max(paddingStart, statusIconState.xTranslation - ((float) this.mUnderflowWidth));
            i5++;
            i4--;
        }
        if (i4 != -1) {
            int i6 = this.mStaticDotDiameter + this.mDotPadding;
            int i7 = (this.mUnderflowStart + this.mUnderflowWidth) - this.mIconDotFrameWidth;
            int i8 = 0;
            while (i4 >= 0) {
                StatusIconState statusIconState2 = (StatusIconState) this.mLayoutStates.get(i4);
                if (i8 < OpStatusIconContainer.MAX_DOTS) {
                    statusIconState2.xTranslation = (float) i7;
                    statusIconState2.visibleState = 1;
                    i7 -= i6;
                    i8++;
                } else {
                    statusIconState2.visibleState = 2;
                }
                i4--;
            }
        }
        if (isLayoutRtl()) {
            for (i = 0; i < childCount; i++) {
                View childAt2 = getChildAt(i);
                StatusIconState viewStateFromChild2 = getViewStateFromChild(childAt2);
                viewStateFromChild2.xTranslation = (opWidth - viewStateFromChild2.xTranslation) - ((float) childAt2.getWidth());
            }
        }
    }

    private void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            StatusIconState viewStateFromChild = getViewStateFromChild(childAt);
            if (viewStateFromChild != null) {
                viewStateFromChild.applyToView(childAt);
            }
        }
    }

    private void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            StatusIconState viewStateFromChild = getViewStateFromChild(childAt);
            if (viewStateFromChild != null) {
                viewStateFromChild.initFrom(childAt);
                viewStateFromChild.alpha = 1.0f;
                viewStateFromChild.hidden = false;
            }
        }
    }

    private static StatusIconState getViewStateFromChild(View view) {
        return (StatusIconState) view.getTag(R$id.status_bar_view_state_tag);
    }

    private static int getViewTotalMeasuredWidth(View view) {
        return view.getMeasuredWidth() + view.getPaddingStart() + view.getPaddingEnd();
    }

    private static int getViewTotalWidth(View view) {
        return view.getWidth() + view.getPaddingStart() + view.getPaddingEnd();
    }

    static {
        C15642 r0 = new AnimationProperties() {
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateAlpha();
                this.mAnimationFilter = animationFilter;
            }

            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r0.setDuration(200);
        r0.setDelay(50);
        ADD_ICON_PROPERTIES = r0;
        C15653 r02 = new AnimationProperties() {
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateX();
                this.mAnimationFilter = animationFilter;
            }

            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r02.setDuration(200);
        X_ANIMATION_PROPERTIES = r02;
        C15664 r03 = new AnimationProperties() {
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateX();
                animationFilter.animateY();
                animationFilter.animateAlpha();
                animationFilter.animateScale();
                this.mAnimationFilter = animationFilter;
            }

            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r03.setDuration(200);
        ANIMATE_ALL_PROPERTIES = r03;
    }
}
