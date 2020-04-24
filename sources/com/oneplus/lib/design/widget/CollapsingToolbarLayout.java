package com.oneplus.lib.design.widget;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.design.widget.AppBarLayout.OnOffsetChangedListener;
import com.oneplus.lib.widget.actionbar.Toolbar;
import com.oneplus.support.core.graphics.drawable.DrawableCompat;
import com.oneplus.support.core.view.OnApplyWindowInsetsListener;
import com.oneplus.support.core.view.ViewCompat;
import com.oneplus.support.core.view.WindowInsetsCompat;
import com.oneplus.support.core.view.animation.FastOutLinearInInterpolator;
import com.oneplus.support.core.view.animation.LinearOutSlowInInterpolator;

public class CollapsingToolbarLayout extends FrameLayout {
    private int mCollapsedTitileMarginBottom;
    final CollapsingTextHelper mCollapsingTextHelper;
    private boolean mCollapsingTitleEnabled;
    private Drawable mContentScrim;
    int mCurrentOffset;
    private boolean mDrawCollapsingTitle;
    private View mDummyView;
    private int mExpandedMarginBottom;
    private int mExpandedMarginEnd;
    private int mExpandedMarginStart;
    private int mExpandedMarginTop;
    WindowInsetsCompat mLastInsets;
    private OnOffsetChangedListener mOnOffsetChangedListener;
    private boolean mRefreshToolbar;
    private int mScrimAlpha;
    private long mScrimAnimationDuration;
    private ValueAnimator mScrimAnimator;
    private int mScrimVisibleHeightTrigger;
    private boolean mScrimsAreShown;
    Drawable mStatusBarScrim;
    private final Rect mTmpRect;
    private Toolbar mToolbar;
    private View mToolbarDirectChild;
    private int mToolbarDrawIndex;
    private int mToolbarId;

    public static class LayoutParams extends android.widget.FrameLayout.LayoutParams {
        int mCollapseMode = 0;
        float mParallaxMult = 0.5f;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OpCollapsingToolbarLayout_Layout);
            this.mCollapseMode = obtainStyledAttributes.getInt(R$styleable.OpCollapsingToolbarLayout_Layout_op_layout_collapseMode, 0);
            setParallaxMultiplier(obtainStyledAttributes.getFloat(R$styleable.f111xe4ee9dec, 0.5f));
            obtainStyledAttributes.recycle();
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public void setParallaxMultiplier(float f) {
            this.mParallaxMult = f;
        }
    }

    private class OffsetUpdateListener implements OnOffsetChangedListener {
        OffsetUpdateListener() {
        }

        public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
            CollapsingToolbarLayout collapsingToolbarLayout = CollapsingToolbarLayout.this;
            collapsingToolbarLayout.mCurrentOffset = i;
            WindowInsetsCompat windowInsetsCompat = collapsingToolbarLayout.mLastInsets;
            int systemWindowInsetTop = windowInsetsCompat != null ? windowInsetsCompat.getSystemWindowInsetTop() : 0;
            int childCount = CollapsingToolbarLayout.this.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                View childAt = CollapsingToolbarLayout.this.getChildAt(i2);
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                ViewOffsetHelper viewOffsetHelper = CollapsingToolbarLayout.getViewOffsetHelper(childAt);
                int i3 = layoutParams.mCollapseMode;
                if (i3 == 1) {
                    viewOffsetHelper.setTopAndBottomOffset(Utils.constrain(-i, 0, CollapsingToolbarLayout.this.getMaxOffsetForPinChild(childAt)));
                } else if (i3 == 2) {
                    viewOffsetHelper.setTopAndBottomOffset(Math.round(((float) (-i)) * layoutParams.mParallaxMult));
                }
            }
            CollapsingToolbarLayout.this.updateScrimVisibility();
            CollapsingToolbarLayout collapsingToolbarLayout2 = CollapsingToolbarLayout.this;
            if (collapsingToolbarLayout2.mStatusBarScrim != null && systemWindowInsetTop > 0) {
                ViewCompat.postInvalidateOnAnimation(collapsingToolbarLayout2);
            }
            CollapsingToolbarLayout.this.mCollapsingTextHelper.setExpansionFraction(((float) Math.abs(i)) / ((float) ((CollapsingToolbarLayout.this.getHeight() - ViewCompat.getMinimumHeight(CollapsingToolbarLayout.this)) - systemWindowInsetTop)));
        }
    }

    public CollapsingToolbarLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CollapsingToolbarLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRefreshToolbar = true;
        this.mTmpRect = new Rect();
        this.mScrimVisibleHeightTrigger = -1;
        this.mCollapsingTextHelper = new CollapsingTextHelper(this);
        this.mCollapsingTextHelper.setTextSizeInterpolator(new DecelerateInterpolator());
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OpCollapsingToolbarLayout, i, R$style.Widget_Design_CollapsingToolbar);
        this.mCollapsingTextHelper.setExpandedTextGravity(obtainStyledAttributes.getInt(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleGravity, 8388691));
        this.mCollapsingTextHelper.setCollapsedTextGravity(obtainStyledAttributes.getInt(R$styleable.OpCollapsingToolbarLayout_opCollapsedTitleGravity, 8388627));
        int dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMargin, 0);
        this.mExpandedMarginBottom = dimensionPixelSize;
        this.mExpandedMarginEnd = dimensionPixelSize;
        this.mExpandedMarginTop = dimensionPixelSize;
        this.mExpandedMarginStart = dimensionPixelSize;
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginStart)) {
            this.mExpandedMarginStart = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginStart, 0);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginEnd)) {
            this.mExpandedMarginEnd = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginEnd, 0);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginTop)) {
            this.mExpandedMarginTop = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginTop, 0);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginBottom)) {
            this.mExpandedMarginBottom = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleMarginBottom, 0);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opCollapsedTitleMarginBottom)) {
            this.mCollapsedTitileMarginBottom = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opCollapsedTitleMarginBottom, 0);
        }
        this.mCollapsingTitleEnabled = obtainStyledAttributes.getBoolean(R$styleable.OpCollapsingToolbarLayout_opTitleEnabled, true);
        setTitle(obtainStyledAttributes.getText(R$styleable.OpCollapsingToolbarLayout_android_title));
        this.mCollapsingTextHelper.setExpandedTextAppearance(R$style.OPTextAppearance_Design_CollapsingToolbar_Expanded);
        this.mCollapsingTextHelper.setCollapsedTextAppearance(R$style.TextAppearance_Widget_ActionBar_Title);
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleTextAppearance)) {
            this.mCollapsingTextHelper.setExpandedTextAppearance(obtainStyledAttributes.getResourceId(R$styleable.OpCollapsingToolbarLayout_opExpandedTitleTextAppearance, 0));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OpCollapsingToolbarLayout_opCollapsedTitleTextAppearance)) {
            this.mCollapsingTextHelper.setCollapsedTextAppearance(obtainStyledAttributes.getResourceId(R$styleable.OpCollapsingToolbarLayout_opCollapsedTitleTextAppearance, 0));
        }
        this.mScrimVisibleHeightTrigger = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OpCollapsingToolbarLayout_opScrimVisibleHeightTrigger, -1);
        this.mScrimAnimationDuration = (long) obtainStyledAttributes.getInt(R$styleable.OpCollapsingToolbarLayout_opScrimAnimationDuration, 600);
        setContentScrim(obtainStyledAttributes.getDrawable(R$styleable.OpCollapsingToolbarLayout_opContentScrim));
        setStatusBarScrim(obtainStyledAttributes.getDrawable(R$styleable.OpCollapsingToolbarLayout_opStatusBarScrim));
        this.mToolbarId = obtainStyledAttributes.getResourceId(R$styleable.OpCollapsingToolbarLayout_opToolbarId, -1);
        obtainStyledAttributes.recycle();
        setWillNotDraw(false);
        ViewCompat.setOnApplyWindowInsetsListener(this, new OnApplyWindowInsetsListener() {
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
                return CollapsingToolbarLayout.this.onWindowInsetChanged(windowInsetsCompat);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if (parent instanceof AppBarLayout) {
            ViewCompat.setFitsSystemWindows(this, ViewCompat.getFitsSystemWindows((View) parent));
            if (this.mOnOffsetChangedListener == null) {
                this.mOnOffsetChangedListener = new OffsetUpdateListener();
            }
            ((AppBarLayout) parent).addOnOffsetChangedListener(this.mOnOffsetChangedListener);
            ViewCompat.requestApplyInsets(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        ViewParent parent = getParent();
        OnOffsetChangedListener onOffsetChangedListener = this.mOnOffsetChangedListener;
        if (onOffsetChangedListener != null && (parent instanceof AppBarLayout)) {
            ((AppBarLayout) parent).removeOnOffsetChangedListener(onOffsetChangedListener);
        }
        super.onDetachedFromWindow();
    }

    /* access modifiers changed from: 0000 */
    public WindowInsetsCompat onWindowInsetChanged(WindowInsetsCompat windowInsetsCompat) {
        WindowInsetsCompat windowInsetsCompat2 = ViewCompat.getFitsSystemWindows(this) ? windowInsetsCompat : null;
        if (!Utils.objectEquals(this.mLastInsets, windowInsetsCompat2)) {
            this.mLastInsets = windowInsetsCompat2;
            requestLayout();
        }
        return windowInsetsCompat.consumeSystemWindowInsets();
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        ensureToolbar();
        if (this.mToolbar == null) {
            Drawable drawable = this.mContentScrim;
            if (drawable != null && this.mScrimAlpha > 0) {
                drawable.mutate().setAlpha(this.mScrimAlpha);
                this.mContentScrim.draw(canvas);
            }
        }
        if (this.mCollapsingTitleEnabled && this.mDrawCollapsingTitle) {
            this.mCollapsingTextHelper.draw(canvas);
        }
        if (this.mStatusBarScrim != null && this.mScrimAlpha > 0) {
            WindowInsetsCompat windowInsetsCompat = this.mLastInsets;
            int systemWindowInsetTop = windowInsetsCompat != null ? windowInsetsCompat.getSystemWindowInsetTop() : 0;
            if (systemWindowInsetTop > 0) {
                this.mStatusBarScrim.setBounds(0, -this.mCurrentOffset, getWidth(), systemWindowInsetTop - this.mCurrentOffset);
                this.mStatusBarScrim.mutate().setAlpha(this.mScrimAlpha);
                this.mStatusBarScrim.draw(canvas);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View view, long j) {
        boolean drawChild = super.drawChild(canvas, view, j);
        if (this.mContentScrim == null || this.mScrimAlpha <= 0 || !isToolbarChildDrawnNext(view)) {
            return drawChild;
        }
        this.mContentScrim.mutate().setAlpha(this.mScrimAlpha);
        this.mContentScrim.draw(canvas);
        return true;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        Drawable drawable = this.mContentScrim;
        if (drawable != null) {
            drawable.setBounds(0, 0, i, i2);
        }
    }

    private void ensureToolbar() {
        if (this.mRefreshToolbar) {
            Toolbar toolbar = null;
            this.mToolbar = null;
            this.mToolbarDirectChild = null;
            int i = this.mToolbarId;
            if (i != -1) {
                this.mToolbar = (Toolbar) findViewById(i);
                Toolbar toolbar2 = this.mToolbar;
                if (toolbar2 != null) {
                    this.mToolbarDirectChild = findDirectChild(toolbar2);
                }
            }
            if (this.mToolbar == null) {
                int childCount = getChildCount();
                int i2 = 0;
                while (true) {
                    if (i2 >= childCount) {
                        break;
                    }
                    View childAt = getChildAt(i2);
                    if (childAt instanceof Toolbar) {
                        toolbar = (Toolbar) childAt;
                        break;
                    }
                    i2++;
                }
                this.mToolbar = toolbar;
            }
            updateDummyView();
            this.mRefreshToolbar = false;
        }
    }

    private boolean isToolbarChildDrawnNext(View view) {
        int i = this.mToolbarDrawIndex;
        return i >= 0 && i == indexOfChild(view) + 1;
    }

    private View findDirectChild(View view) {
        ViewParent parent = view.getParent();
        while (parent != this && parent != null) {
            if (parent instanceof View) {
                view = (View) parent;
            }
            parent = parent.getParent();
        }
        return view;
    }

    private void updateDummyView() {
        if (!this.mCollapsingTitleEnabled) {
            View view = this.mDummyView;
            if (view != null) {
                ViewParent parent = view.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(this.mDummyView);
                }
            }
        }
        if (this.mCollapsingTitleEnabled && this.mToolbar != null) {
            if (this.mDummyView == null) {
                this.mDummyView = new View(getContext());
            }
            if (this.mDummyView.getParent() == null) {
                this.mToolbar.addView(this.mDummyView, -1, -1);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        ensureToolbar();
        super.onMeasure(i, i2);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        super.onLayout(z, i, i2, i3, i4);
        WindowInsetsCompat windowInsetsCompat = this.mLastInsets;
        if (windowInsetsCompat != null) {
            int systemWindowInsetTop = windowInsetsCompat.getSystemWindowInsetTop();
            int childCount = getChildCount();
            for (int i7 = 0; i7 < childCount; i7++) {
                View childAt = getChildAt(i7);
                if (!ViewCompat.getFitsSystemWindows(childAt) && childAt.getTop() < systemWindowInsetTop) {
                    ViewCompat.offsetTopAndBottom(childAt, systemWindowInsetTop);
                }
            }
        }
        if (this.mCollapsingTitleEnabled) {
            View view = this.mDummyView;
            if (view != null) {
                boolean z2 = true;
                this.mDrawCollapsingTitle = ViewCompat.isAttachedToWindow(view) && this.mDummyView.getVisibility() == 0;
                if (this.mDrawCollapsingTitle) {
                    if (ViewCompat.getLayoutDirection(this) != 1) {
                        z2 = false;
                    }
                    View view2 = this.mToolbarDirectChild;
                    if (view2 == null) {
                        view2 = this.mToolbar;
                    }
                    int maxOffsetForPinChild = getMaxOffsetForPinChild(view2);
                    Utils.getDescendantRect(this, this.mDummyView, this.mTmpRect);
                    CollapsingTextHelper collapsingTextHelper = this.mCollapsingTextHelper;
                    int i8 = this.mTmpRect.left;
                    if (z2) {
                        i5 = this.mToolbar.getTitleMarginEnd();
                    } else {
                        i5 = this.mToolbar.getTitleMarginStart();
                    }
                    int i9 = i8 + i5;
                    int max = this.mTmpRect.top + maxOffsetForPinChild + Math.max(this.mToolbar.getTitleMarginTop(), this.mToolbar.getTitieTopWithoutOffset());
                    int i10 = this.mTmpRect.right;
                    if (z2) {
                        i6 = this.mToolbar.getTitleMarginStart();
                    } else {
                        i6 = this.mToolbar.getTitleMarginEnd();
                    }
                    collapsingTextHelper.setCollapsedBounds(i9, max, i10 + i6, (this.mTmpRect.bottom + maxOffsetForPinChild) - this.mToolbar.getTitleMarginBottom());
                    this.mCollapsingTextHelper.setExpandedBounds(z2 ? this.mExpandedMarginEnd : this.mExpandedMarginStart, this.mTmpRect.top + this.mExpandedMarginTop, (i3 - i) - (z2 ? this.mExpandedMarginStart : this.mExpandedMarginEnd), (i4 - i2) - this.mExpandedMarginBottom);
                    this.mCollapsingTextHelper.recalculate();
                }
            }
        }
        int childCount2 = getChildCount();
        for (int i11 = 0; i11 < childCount2; i11++) {
            getViewOffsetHelper(getChildAt(i11)).onViewLayout();
        }
        if (this.mToolbar != null) {
            if (this.mCollapsingTitleEnabled && TextUtils.isEmpty(this.mCollapsingTextHelper.getText())) {
                this.mCollapsingTextHelper.setText(this.mToolbar.getTitle());
            }
            View view3 = this.mToolbarDirectChild;
            if (view3 == null || view3 == this) {
                setMinimumHeight(getHeightWithMargins(this.mToolbar));
                this.mToolbarDrawIndex = indexOfChild(this.mToolbar);
            } else {
                setMinimumHeight(getHeightWithMargins(view3));
                this.mToolbarDrawIndex = indexOfChild(this.mToolbarDirectChild);
            }
        } else {
            this.mToolbarDrawIndex = -1;
        }
        updateScrimVisibility();
    }

    private static int getHeightWithMargins(View view) {
        android.view.ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof MarginLayoutParams)) {
            return view.getHeight();
        }
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) layoutParams;
        return view.getHeight() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
    }

    static ViewOffsetHelper getViewOffsetHelper(View view) {
        ViewOffsetHelper viewOffsetHelper = (ViewOffsetHelper) view.getTag(R$id.view_offset_helper);
        if (viewOffsetHelper != null) {
            return viewOffsetHelper;
        }
        ViewOffsetHelper viewOffsetHelper2 = new ViewOffsetHelper(view);
        view.setTag(R$id.view_offset_helper, viewOffsetHelper2);
        return viewOffsetHelper2;
    }

    public void setTitle(CharSequence charSequence) {
        this.mCollapsingTextHelper.setText(charSequence);
    }

    public void setScrimsShown(boolean z) {
        setScrimsShown(z, ViewCompat.isLaidOut(this) && !isInEditMode());
    }

    public void setScrimsShown(boolean z, boolean z2) {
        if (this.mScrimsAreShown != z) {
            int i = 255;
            if (z2) {
                if (!z) {
                    i = 0;
                }
                animateScrim(i);
            } else {
                if (!z) {
                    i = 0;
                }
                setScrimAlpha(i);
            }
            this.mScrimsAreShown = z;
        }
    }

    private void animateScrim(int i) {
        TimeInterpolator timeInterpolator;
        ensureToolbar();
        ValueAnimator valueAnimator = this.mScrimAnimator;
        if (valueAnimator == null) {
            this.mScrimAnimator = new ValueAnimator();
            this.mScrimAnimator.setDuration(this.mScrimAnimationDuration);
            ValueAnimator valueAnimator2 = this.mScrimAnimator;
            if (i > this.mScrimAlpha) {
                timeInterpolator = new FastOutLinearInInterpolator();
            } else {
                timeInterpolator = new LinearOutSlowInInterpolator();
            }
            valueAnimator2.setInterpolator(timeInterpolator);
            this.mScrimAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CollapsingToolbarLayout.this.setScrimAlpha(((Integer) valueAnimator.getAnimatedValue()).intValue());
                }
            });
        } else if (valueAnimator.isRunning()) {
            this.mScrimAnimator.cancel();
        }
        this.mScrimAnimator.setIntValues(new int[]{this.mScrimAlpha, i});
        this.mScrimAnimator.start();
    }

    /* access modifiers changed from: 0000 */
    public void setScrimAlpha(int i) {
        if (i != this.mScrimAlpha) {
            if (this.mContentScrim != null) {
                Toolbar toolbar = this.mToolbar;
                if (toolbar != null) {
                    ViewCompat.postInvalidateOnAnimation(toolbar);
                }
            }
            this.mScrimAlpha = i;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setContentScrim(Drawable drawable) {
        Drawable drawable2 = this.mContentScrim;
        if (drawable2 != drawable) {
            Drawable drawable3 = null;
            if (drawable2 != null) {
                drawable2.setCallback(null);
            }
            if (drawable != null) {
                drawable3 = drawable.mutate();
            }
            this.mContentScrim = drawable3;
            Drawable drawable4 = this.mContentScrim;
            if (drawable4 != null) {
                drawable4.setBounds(0, 0, getWidth(), getHeight());
                this.mContentScrim.setCallback(this);
                this.mContentScrim.setAlpha(this.mScrimAlpha);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setStatusBarScrim(Drawable drawable) {
        Drawable drawable2 = this.mStatusBarScrim;
        if (drawable2 != drawable) {
            Drawable drawable3 = null;
            if (drawable2 != null) {
                drawable2.setCallback(null);
            }
            if (drawable != null) {
                drawable3 = drawable.mutate();
            }
            this.mStatusBarScrim = drawable3;
            Drawable drawable4 = this.mStatusBarScrim;
            if (drawable4 != null) {
                if (drawable4.isStateful()) {
                    this.mStatusBarScrim.setState(getDrawableState());
                }
                DrawableCompat.setLayoutDirection(this.mStatusBarScrim, ViewCompat.getLayoutDirection(this));
                this.mStatusBarScrim.setVisible(getVisibility() == 0, false);
                this.mStatusBarScrim.setCallback(this);
                this.mStatusBarScrim.setAlpha(this.mScrimAlpha);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        int[] drawableState = getDrawableState();
        Drawable drawable = this.mStatusBarScrim;
        boolean z = false;
        if (drawable != null && drawable.isStateful()) {
            z = false | drawable.setState(drawableState);
        }
        Drawable drawable2 = this.mContentScrim;
        if (drawable2 != null && drawable2.isStateful()) {
            z |= drawable2.setState(drawableState);
        }
        CollapsingTextHelper collapsingTextHelper = this.mCollapsingTextHelper;
        if (collapsingTextHelper != null) {
            z |= collapsingTextHelper.setState(drawableState);
        }
        if (z) {
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mContentScrim || drawable == this.mStatusBarScrim;
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        boolean z = i == 0;
        Drawable drawable = this.mStatusBarScrim;
        if (!(drawable == null || drawable.isVisible() == z)) {
            this.mStatusBarScrim.setVisible(z, false);
        }
        Drawable drawable2 = this.mContentScrim;
        if (drawable2 != null && drawable2.isVisible() != z) {
            this.mContentScrim.setVisible(z, false);
        }
    }

    public int getScrimVisibleHeightTrigger() {
        int i = this.mScrimVisibleHeightTrigger;
        if (i >= 0) {
            return i;
        }
        WindowInsetsCompat windowInsetsCompat = this.mLastInsets;
        int systemWindowInsetTop = windowInsetsCompat != null ? windowInsetsCompat.getSystemWindowInsetTop() : 0;
        int minimumHeight = ViewCompat.getMinimumHeight(this);
        if (minimumHeight > 0) {
            return Math.min((minimumHeight * 2) + systemWindowInsetTop, getHeight());
        }
        return getHeight() / 3;
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    public android.widget.FrameLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    /* access modifiers changed from: protected */
    public android.widget.FrameLayout.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    /* access modifiers changed from: 0000 */
    public final void updateScrimVisibility() {
        if (this.mContentScrim != null || this.mStatusBarScrim != null) {
            setScrimsShown(getHeight() + this.mCurrentOffset < getScrimVisibleHeightTrigger());
        }
    }

    /* access modifiers changed from: 0000 */
    public final int getMaxOffsetForPinChild(View view) {
        return ((getHeight() - getViewOffsetHelper(view).getLayoutTop()) - view.getHeight()) - ((LayoutParams) view.getLayoutParams()).bottomMargin;
    }
}
