package com.oneplus.lib.widget.button;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.design.widget.AppBarLayout;
import com.oneplus.lib.design.widget.CoordinatorLayout;
import com.oneplus.lib.design.widget.CoordinatorLayout.DefaultBehavior;
import com.oneplus.lib.design.widget.CoordinatorLayout.LayoutParams;
import com.oneplus.lib.design.widget.Utils;
import com.oneplus.support.core.view.ViewCompat;
import java.util.List;

@DefaultBehavior(Behavior.class)
public class OPFloatingActionButton extends ImageView {
    private ColorStateList mBackgroundTint;
    private Mode mBackgroundTintMode;
    private int mBorderWidth;
    /* access modifiers changed from: private */
    public int mContentPadding;
    private final OPFloatingActionButtonImpl mImpl;
    private int mRippleColor;
    /* access modifiers changed from: private */
    public final Rect mShadowPadding;
    private int mSize;
    private int mUserSetVisibility;

    public static class Behavior extends com.oneplus.lib.design.widget.CoordinatorLayout.Behavior<OPFloatingActionButton> {
        private boolean mAutoHideEnabled;
        private OnVisibilityChangedListener mInternalAutoHideListener;
        private Rect mTmpRect;

        private static boolean isBottomSheet(View view) {
            return false;
        }

        public Behavior() {
            this.mAutoHideEnabled = true;
        }

        public Behavior(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OpFloatingActionButton_Behavior_Layout);
            this.mAutoHideEnabled = obtainStyledAttributes.getBoolean(R$styleable.OpFloatingActionButton_Behavior_Layout_op_behavior_autoHide, true);
            obtainStyledAttributes.recycle();
        }

        public void onAttachedToLayoutParams(LayoutParams layoutParams) {
            if (layoutParams.dodgeInsetEdges == 0) {
                layoutParams.dodgeInsetEdges = 80;
            }
        }

        public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, OPFloatingActionButton oPFloatingActionButton, View view) {
            if (view instanceof AppBarLayout) {
                updateFabVisibilityForAppBarLayout(coordinatorLayout, (AppBarLayout) view, oPFloatingActionButton);
            } else if (isBottomSheet(view)) {
                updateFabVisibilityForBottomSheet(view, oPFloatingActionButton);
            }
            return false;
        }

        /* access modifiers changed from: 0000 */
        public void setInternalAutoHideListener(OnVisibilityChangedListener onVisibilityChangedListener) {
            this.mInternalAutoHideListener = onVisibilityChangedListener;
        }

        private boolean shouldUpdateVisibility(View view, OPFloatingActionButton oPFloatingActionButton) {
            LayoutParams layoutParams = (LayoutParams) oPFloatingActionButton.getLayoutParams();
            if (this.mAutoHideEnabled && layoutParams.getAnchorId() == view.getId() && oPFloatingActionButton.getUserSetVisibility() == 0) {
                return true;
            }
            return false;
        }

        private boolean updateFabVisibilityForAppBarLayout(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, OPFloatingActionButton oPFloatingActionButton) {
            if (!shouldUpdateVisibility(appBarLayout, oPFloatingActionButton)) {
                return false;
            }
            if (this.mTmpRect == null) {
                this.mTmpRect = new Rect();
            }
            Rect rect = this.mTmpRect;
            Utils.getDescendantRect(coordinatorLayout, appBarLayout, rect);
            if (rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {
                oPFloatingActionButton.hide(false);
            } else {
                oPFloatingActionButton.show(false);
            }
            return true;
        }

        private boolean updateFabVisibilityForBottomSheet(View view, OPFloatingActionButton oPFloatingActionButton) {
            if (!shouldUpdateVisibility(view, oPFloatingActionButton)) {
                return false;
            }
            if (view.getTop() < (oPFloatingActionButton.getHeight() / 2) + ((LayoutParams) oPFloatingActionButton.getLayoutParams()).topMargin) {
                oPFloatingActionButton.hide(false);
            } else {
                oPFloatingActionButton.show(false);
            }
            return true;
        }

        public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, OPFloatingActionButton oPFloatingActionButton, int i) {
            List dependencies = coordinatorLayout.getDependencies(oPFloatingActionButton);
            int size = dependencies.size();
            for (int i2 = 0; i2 < size; i2++) {
                View view = (View) dependencies.get(i2);
                if (!(view instanceof AppBarLayout)) {
                    if (isBottomSheet(view) && updateFabVisibilityForBottomSheet(view, oPFloatingActionButton)) {
                        break;
                    }
                } else if (updateFabVisibilityForAppBarLayout(coordinatorLayout, (AppBarLayout) view, oPFloatingActionButton)) {
                    break;
                }
            }
            coordinatorLayout.onLayoutChild(oPFloatingActionButton, i);
            offsetIfNeeded(coordinatorLayout, oPFloatingActionButton);
            return true;
        }

        public boolean getInsetDodgeRect(CoordinatorLayout coordinatorLayout, OPFloatingActionButton oPFloatingActionButton, Rect rect) {
            Rect access$000 = oPFloatingActionButton.mShadowPadding;
            rect.set(oPFloatingActionButton.getLeft() + access$000.left, oPFloatingActionButton.getTop() + access$000.top, oPFloatingActionButton.getRight() - access$000.right, oPFloatingActionButton.getBottom() - access$000.bottom);
            return true;
        }

        private void offsetIfNeeded(CoordinatorLayout coordinatorLayout, OPFloatingActionButton oPFloatingActionButton) {
            Rect access$000 = oPFloatingActionButton.mShadowPadding;
            if (access$000 != null && access$000.centerX() > 0 && access$000.centerY() > 0) {
                LayoutParams layoutParams = (LayoutParams) oPFloatingActionButton.getLayoutParams();
                int i = 0;
                int i2 = oPFloatingActionButton.getRight() >= coordinatorLayout.getWidth() - layoutParams.rightMargin ? access$000.right : oPFloatingActionButton.getLeft() <= layoutParams.leftMargin ? -access$000.left : 0;
                if (oPFloatingActionButton.getBottom() >= coordinatorLayout.getHeight() - layoutParams.bottomMargin) {
                    i = access$000.bottom;
                } else if (oPFloatingActionButton.getTop() <= layoutParams.topMargin) {
                    i = -access$000.top;
                }
                if (i != 0) {
                    ViewCompat.offsetTopAndBottom(oPFloatingActionButton, i);
                }
                if (i2 != 0) {
                    ViewCompat.offsetLeftAndRight(oPFloatingActionButton, i2);
                }
            }
        }
    }

    public static abstract class OnVisibilityChangedListener {
    }

    public OPFloatingActionButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPFloatingActionButtonStyle);
    }

    public OPFloatingActionButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mUserSetVisibility = getVisibility();
        this.mShadowPadding = new Rect();
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPFloatingActionButton, i, R$style.OnePlus_Widget_Design_FloatingActionButton);
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.OPFloatingActionButton_android_background);
        this.mBackgroundTint = obtainStyledAttributes.getColorStateList(R$styleable.OPFloatingActionButton_op_backgroundTint);
        this.mBackgroundTintMode = parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPFloatingActionButton_op_backgroundTintMode, -1), null);
        this.mRippleColor = obtainStyledAttributes.getColor(R$styleable.OPFloatingActionButton_op_rippleColor, 0);
        this.mSize = obtainStyledAttributes.getInt(R$styleable.OPFloatingActionButton_op_fabSize, 0);
        this.mBorderWidth = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPFloatingActionButton_op_borderWidth, 0);
        float dimension = obtainStyledAttributes.getDimension(R$styleable.OPFloatingActionButton_op_elevation, 0.0f);
        float dimension2 = obtainStyledAttributes.getDimension(R$styleable.OPFloatingActionButton_op_pressedTranslationZ, 0.0f);
        obtainStyledAttributes.recycle();
        this.mImpl = new OPFloatingActionButtonImpl(this, new OPShadowViewDelegate() {
            public void setShadowPadding(int i, int i2, int i3, int i4) {
                OPFloatingActionButton.this.mShadowPadding.set(i, i2, i3, i4);
                OPFloatingActionButton oPFloatingActionButton = OPFloatingActionButton.this;
                oPFloatingActionButton.setPadding(i + oPFloatingActionButton.mContentPadding, i2 + OPFloatingActionButton.this.mContentPadding, i3 + OPFloatingActionButton.this.mContentPadding, i4 + OPFloatingActionButton.this.mContentPadding);
            }

            public void setBackground(Drawable drawable) {
                OPFloatingActionButton.super.setBackground(drawable);
            }
        });
        this.mContentPadding = (getSizeDimension() - ((int) getResources().getDimension(R$dimen.design_fab_content_size))) / 2;
        this.mImpl.setBackground(drawable, this.mBackgroundTint, this.mBackgroundTintMode, this.mRippleColor, this.mBorderWidth);
        this.mImpl.setElevation(dimension);
        this.mImpl.setPressedTranslationZ(dimension2);
        setClickable(true);
    }

    public void setVisibility(int i) {
        internalSetVisibility(i, true);
    }

    /* access modifiers changed from: 0000 */
    public final void internalSetVisibility(int i, boolean z) {
        super.setVisibility(i);
        if (z) {
            this.mUserSetVisibility = i;
        }
    }

    /* access modifiers changed from: 0000 */
    public final int getUserSetVisibility() {
        return this.mUserSetVisibility;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int sizeDimension = getSizeDimension();
        int min = Math.min(resolveAdjustedSize(sizeDimension, i), resolveAdjustedSize(sizeDimension, i2));
        Rect rect = this.mShadowPadding;
        setMeasuredDimension(rect.left + min + rect.right, min + rect.top + rect.bottom);
    }

    public ColorStateList getBackgroundTintList() {
        return this.mBackgroundTint;
    }

    public void setBackgroundTintList(ColorStateList colorStateList) {
        if (this.mBackgroundTint != colorStateList) {
            this.mBackgroundTint = colorStateList;
            this.mImpl.setBackgroundTintList(colorStateList);
        }
    }

    public Mode getBackgroundTintMode() {
        return this.mBackgroundTintMode;
    }

    public void setBackgroundTintMode(Mode mode) {
        if (this.mBackgroundTintMode != mode) {
            this.mBackgroundTintMode = mode;
            this.mImpl.setBackgroundTintMode(mode);
        }
    }

    public void setBackground(Drawable drawable) {
        OPFloatingActionButtonImpl oPFloatingActionButtonImpl = this.mImpl;
        if (oPFloatingActionButtonImpl != null) {
            oPFloatingActionButtonImpl.setBackground(drawable, this.mBackgroundTint, this.mBackgroundTintMode, this.mRippleColor, this.mBorderWidth);
        }
    }

    public void show(boolean z) {
        this.mImpl.show(z);
    }

    public void hide(boolean z) {
        this.mImpl.hide(z);
    }

    /* access modifiers changed from: 0000 */
    public final int getSizeDimension() {
        if (this.mSize != 1) {
            return getResources().getDimensionPixelSize(R$dimen.design_fab_size_normal);
        }
        return getResources().getDimensionPixelSize(R$dimen.design_fab_size_mini);
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        this.mImpl.onDrawableStateChanged(getDrawableState());
    }

    @TargetApi(11)
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        this.mImpl.jumpDrawableToCurrentState();
    }

    private static int resolveAdjustedSize(int i, int i2) {
        int mode = MeasureSpec.getMode(i2);
        int size = MeasureSpec.getSize(i2);
        if (mode != Integer.MIN_VALUE) {
            return (mode == 0 || mode != 1073741824) ? i : size;
        }
        return Math.min(i, size);
    }

    static Mode parseTintMode(int i, Mode mode) {
        if (i == 3) {
            return Mode.SRC_OVER;
        }
        if (i == 5) {
            return Mode.SRC_IN;
        }
        if (i == 9) {
            return Mode.SRC_ATOP;
        }
        if (i != 14) {
            return i != 15 ? mode : Mode.SCREEN;
        }
        return Mode.MULTIPLY;
    }
}
