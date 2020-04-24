package com.oneplus.lib.widget.actionbar;

import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.CollapsibleActionView;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.oneplus.commonctrl.R$anim;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.menu.ActionMenuView;
import com.oneplus.lib.menu.ActionMenuView.OnMenuItemClickListener;
import com.oneplus.lib.menu.MenuBuilder;
import com.oneplus.lib.menu.MenuItemImpl;
import com.oneplus.lib.menu.MenuPresenter;
import com.oneplus.lib.menu.MenuPresenter.Callback;
import com.oneplus.lib.menu.SubMenuBuilder;
import com.oneplus.lib.util.AppUtils;
import com.oneplus.lib.widget.util.ViewUtils;
import com.oneplus.support.annotation.GestureBarAdapter;
import com.oneplus.support.annotation.GestureBarAdapterPolicy;
import java.util.ArrayList;
import java.util.List;

public class Toolbar extends android.widget.Toolbar {
    private static final int[] ACTION_BAR_DIVIDER_ATTR = {R$attr.onePlusActionbarLineColor};
    private static final int ICON_MIN_WIDTH = R$dimen.toolbar_icon_min_width;
    private static final int ICON_SIZE_STANDARD = R$dimen.oneplus_contorl_icon_size_button;
    private static final int MAX_ICON_SIZE = R$dimen.abc_action_menu_icon_size;
    private int mActionBarDividerColor;
    private Callback mActionMenuPresenterCallback;
    /* access modifiers changed from: private */
    public int mButtonGravity;
    /* access modifiers changed from: private */
    public ImageButton mCollapseButtonView;
    private CharSequence mCollapseDescription;
    private Drawable mCollapseIcon;
    private boolean mCollapsed;
    private boolean mCollapsible;
    private int mContentInsetEndWithActions;
    private int mContentInsetStartWithNavigation;
    private RtlSpacingHelper mContentInsets;
    private boolean mEatingTouch;
    View mExpandedActionView;
    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    private int mGravity;
    private boolean mHasActionBarLineColor;
    private int mHeightWithGestureBar;
    private final ArrayList<View> mHiddenViews;
    private boolean mInsetPaddingTopInGestureMode;
    private ImageView mLogoView;
    private int mMaxButtonHeight;
    private int mMaxIconSize;
    private boolean mMeasuredGestureBar;
    private MenuBuilder.Callback mMenuBuilderCallback;
    private ActionMenuView mMenuView;
    private final OnMenuItemClickListener mMenuViewItemClickListener;
    private ImageButton mMyNavButtonView;
    private int mNavButtonStyle;
    private boolean mNeedResetPadding;
    /* access modifiers changed from: private */
    public android.widget.Toolbar.OnMenuItemClickListener mOnMenuItemClickListener;
    private int mOrientation;
    private int mPaddingTopOffset;
    private Context mPopupContext;
    private int mPopupTheme;
    private int mRealPaddingBottom;
    private int mRealTitleMarginBottom;
    private final Runnable mShowOverflowMenuRunnable;
    private int mSubTitleMarginBottom;
    private CharSequence mSubtitleText;
    private int mSubtitleTextAppearance;
    private int mSubtitleTextColor;
    private TextView mSubtitleTextView;
    private final int[] mTempMargins;
    private final ArrayList<View> mTempViews;
    private int mTitleMarginBottom;
    private int mTitleMarginEnd;
    private int mTitleMarginStart;
    private int mTitleMarginTop;
    private CharSequence mTitleText;
    private int mTitleTextAppearance;
    private int mTitleTextColor;
    private TextView mTitleTextView;
    private final int[] mTmpStatesArray;
    private ToolbarWidgetWrapper mWrapper;

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuItemImpl mCurrentExpandedItem;
        MenuBuilder mMenu;

        public boolean flagActionItems() {
            return false;
        }

        public int getId() {
            return 0;
        }

        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
        }

        public void onRestoreInstanceState(Parcelable parcelable) {
        }

        public Parcelable onSaveInstanceState() {
            return null;
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
            return false;
        }

        private ExpandedActionViewMenuPresenter() {
        }

        public void initForMenu(Context context, MenuBuilder menuBuilder) {
            MenuBuilder menuBuilder2 = this.mMenu;
            if (menuBuilder2 != null) {
                MenuItemImpl menuItemImpl = this.mCurrentExpandedItem;
                if (menuItemImpl != null) {
                    menuBuilder2.collapseItemActionView(menuItemImpl);
                }
            }
            this.mMenu = menuBuilder;
        }

        public void updateMenuView(boolean z) {
            if (this.mCurrentExpandedItem != null) {
                MenuBuilder menuBuilder = this.mMenu;
                boolean z2 = false;
                if (menuBuilder != null) {
                    int size = menuBuilder.size();
                    int i = 0;
                    while (true) {
                        if (i >= size) {
                            break;
                        } else if (this.mMenu.getItem(i) == this.mCurrentExpandedItem) {
                            z2 = true;
                            break;
                        } else {
                            i++;
                        }
                    }
                }
                if (!z2) {
                    collapseItemActionView(this.mMenu, this.mCurrentExpandedItem);
                }
            }
        }

        public boolean expandItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
            Toolbar.this.ensureCollapseButtonView();
            ViewParent parent = Toolbar.this.mCollapseButtonView.getParent();
            Toolbar toolbar = Toolbar.this;
            if (parent != toolbar) {
                toolbar.addView(toolbar.mCollapseButtonView);
            }
            Toolbar.this.mExpandedActionView = menuItemImpl.getActionView();
            this.mCurrentExpandedItem = menuItemImpl;
            ViewParent parent2 = Toolbar.this.mExpandedActionView.getParent();
            Toolbar toolbar2 = Toolbar.this;
            if (parent2 != toolbar2) {
                LayoutParams generateDefaultLayoutParams = toolbar2.generateDefaultLayoutParams();
                generateDefaultLayoutParams.gravity = 8388611 | (Toolbar.this.mButtonGravity & 16);
                generateDefaultLayoutParams.mViewType = 2;
                Toolbar.this.mExpandedActionView.setLayoutParams(generateDefaultLayoutParams);
                Toolbar toolbar3 = Toolbar.this;
                toolbar3.addView(toolbar3.mExpandedActionView);
            }
            Toolbar.this.removeChildrenForExpandedActionView();
            Toolbar.this.requestLayout();
            menuItemImpl.setActionViewExpanded(true);
            View view = Toolbar.this.mExpandedActionView;
            if (view instanceof CollapsibleActionView) {
                ((CollapsibleActionView) view).onActionViewExpanded();
            }
            return true;
        }

        public boolean collapseItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
            View view = Toolbar.this.mExpandedActionView;
            if (view instanceof CollapsibleActionView) {
                ((CollapsibleActionView) view).onActionViewCollapsed();
            }
            Toolbar toolbar = Toolbar.this;
            toolbar.removeView(toolbar.mExpandedActionView);
            Toolbar toolbar2 = Toolbar.this;
            toolbar2.removeView(toolbar2.mCollapseButtonView);
            Toolbar toolbar3 = Toolbar.this;
            toolbar3.mExpandedActionView = null;
            toolbar3.addChildrenForExpandedActionView();
            this.mCurrentExpandedItem = null;
            Toolbar.this.requestLayout();
            menuItemImpl.setActionViewExpanded(false);
            return true;
        }
    }

    public static class LayoutParams extends android.widget.Toolbar.LayoutParams {
        int mViewType;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mViewType = 0;
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.mViewType = 0;
            this.gravity = 8388627;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super(layoutParams);
            this.mViewType = 0;
            this.mViewType = layoutParams.mViewType;
        }

        public LayoutParams(android.app.ActionBar.LayoutParams layoutParams) {
            super(layoutParams);
            this.mViewType = 0;
        }

        public LayoutParams(MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.mViewType = 0;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.mViewType = 0;
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
        public int expandedMenuItemId;
        public boolean isOverflowOpen;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.expandedMenuItemId = parcel.readInt();
            this.isOverflowOpen = parcel.readInt() != 0;
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.expandedMenuItemId);
            parcel.writeInt(this.isOverflowOpen ? 1 : 0);
        }
    }

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.toolbarStyle);
    }

    public Toolbar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Toolbar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        boolean z = true;
        this.mNeedResetPadding = true;
        this.mGravity = 8388627;
        this.mTempViews = new ArrayList<>();
        this.mHiddenViews = new ArrayList<>();
        this.mTempMargins = new int[2];
        this.mTmpStatesArray = new int[2];
        this.mMenuViewItemClickListener = new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (Toolbar.this.mOnMenuItemClickListener != null) {
                    return Toolbar.this.mOnMenuItemClickListener.onMenuItemClick(menuItem);
                }
                return false;
            }
        };
        this.mShowOverflowMenuRunnable = new Runnable() {
            public void run() {
                Toolbar.this.showOverflowMenu();
            }
        };
        this.mActionBarDividerColor = getDividerColor(context, ACTION_BAR_DIVIDER_ATTR[0]);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.Toolbar, i, i2);
        this.mCollapsible = obtainStyledAttributes.getBoolean(R$styleable.Toolbar_op_collapsible, false);
        if (this.mCollapsible && VERSION.SDK_INT >= 21) {
            setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, R$anim.op_design_appbar_state_list_animator));
            setCollapsedState(false);
        }
        this.mOrientation = context.getResources().getConfiguration().orientation;
        this.mTitleTextAppearance = obtainStyledAttributes.getResourceId(R$styleable.Toolbar_titleTextAppearance, 0);
        this.mSubtitleTextAppearance = obtainStyledAttributes.getResourceId(R$styleable.Toolbar_subtitleTextAppearance, 0);
        this.mNavButtonStyle = obtainStyledAttributes.getResourceId(R$styleable.Toolbar_opNavigationButtonStyle, 0);
        this.mGravity = obtainStyledAttributes.getInteger(R$styleable.Toolbar_android_gravity, this.mGravity);
        this.mButtonGravity = obtainStyledAttributes.getInteger(R$styleable.Toolbar_opButtonGravity, 48);
        int dimensionPixelOffset = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_titleMargin, 0);
        this.mTitleMarginBottom = dimensionPixelOffset;
        this.mTitleMarginTop = dimensionPixelOffset;
        this.mTitleMarginEnd = dimensionPixelOffset;
        this.mTitleMarginStart = dimensionPixelOffset;
        this.mMaxIconSize = getResources().getDimensionPixelSize(MAX_ICON_SIZE);
        int dimensionPixelOffset2 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_titleMarginStart, -1);
        if (dimensionPixelOffset2 >= 0) {
            this.mTitleMarginStart = dimensionPixelOffset2;
        }
        int dimensionPixelOffset3 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_titleMarginEnd, -1);
        if (dimensionPixelOffset3 >= 0) {
            this.mTitleMarginEnd = dimensionPixelOffset3;
        }
        int dimensionPixelOffset4 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_titleMarginTop, -1);
        if (dimensionPixelOffset4 >= 0) {
            this.mTitleMarginTop = dimensionPixelOffset4;
        }
        int dimensionPixelOffset5 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_titleMarginBottom, -1);
        if (dimensionPixelOffset5 >= 0) {
            this.mTitleMarginBottom = dimensionPixelOffset5;
        }
        int dimensionPixelOffset6 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_subTitleMarginBottom, -1);
        if (dimensionPixelOffset6 > 0) {
            this.mSubTitleMarginBottom = dimensionPixelOffset6;
        }
        this.mRealPaddingBottom = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_realPaddingBottom, 0);
        this.mRealTitleMarginBottom = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_realTitleMarginBottom, 0);
        this.mMaxButtonHeight = obtainStyledAttributes.getDimensionPixelSize(R$styleable.Toolbar_maxButtonHeight, -1);
        int dimensionPixelOffset7 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_contentInsetStart, Integer.MIN_VALUE);
        int dimensionPixelOffset8 = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_contentInsetEnd, Integer.MIN_VALUE);
        int dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.Toolbar_contentInsetLeft, 0);
        int dimensionPixelSize2 = obtainStyledAttributes.getDimensionPixelSize(R$styleable.Toolbar_contentInsetRight, 0);
        ensureContentInsets();
        this.mContentInsets.setAbsolute(dimensionPixelSize, dimensionPixelSize2);
        if (!(dimensionPixelOffset7 == Integer.MIN_VALUE && dimensionPixelOffset8 == Integer.MIN_VALUE)) {
            this.mContentInsets.setRelative(dimensionPixelOffset7, dimensionPixelOffset8);
        }
        this.mContentInsetStartWithNavigation = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_contentInsetStartWithNavigation, Integer.MIN_VALUE);
        this.mContentInsetEndWithActions = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.Toolbar_contentInsetEndWithActions, Integer.MIN_VALUE);
        this.mCollapseIcon = obtainStyledAttributes.getDrawable(R$styleable.Toolbar_collapseIcon);
        this.mCollapseDescription = obtainStyledAttributes.getText(R$styleable.Toolbar_collapseContentDescription);
        CharSequence text = obtainStyledAttributes.getText(R$styleable.Toolbar_title);
        if (!TextUtils.isEmpty(text)) {
            setTitle(text);
        }
        CharSequence text2 = obtainStyledAttributes.getText(R$styleable.Toolbar_subtitle);
        if (!TextUtils.isEmpty(text2)) {
            setSubtitle(text2);
        }
        this.mPopupContext = getContext();
        setPopupTheme(obtainStyledAttributes.getResourceId(R$styleable.Toolbar_android_popupTheme, 0));
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.Toolbar_navigationIcon);
        if (drawable != null) {
            setNavigationIcon(drawable);
        }
        CharSequence text3 = obtainStyledAttributes.getText(R$styleable.Toolbar_navigationContentDescription);
        if (!TextUtils.isEmpty(text3)) {
            setNavigationContentDescription(text3);
        }
        Drawable drawable2 = obtainStyledAttributes.getDrawable(R$styleable.Toolbar_android_logo);
        if (drawable2 != null) {
            setLogo(drawable2);
        }
        CharSequence text4 = obtainStyledAttributes.getText(R$styleable.Toolbar_logoDescription);
        if (!TextUtils.isEmpty(text4)) {
            setLogoDescription(text4);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.Toolbar_titleTextColor)) {
            setTitleTextColor(obtainStyledAttributes.getColor(R$styleable.Toolbar_titleTextColor, -1));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.Toolbar_subtitleTextColor)) {
            setSubtitleTextColor(obtainStyledAttributes.getColor(R$styleable.Toolbar_subtitleTextColor, -1));
        }
        obtainStyledAttributes.recycle();
        if (getFitsSystemWindows()) {
            z = false;
        }
        this.mNeedResetPadding = z;
    }

    public int getDividerColor(Context context, int i) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(i, typedValue, true);
        if (typedValue.resourceId != 0) {
            try {
                int color = getResources().getColor(typedValue.resourceId);
                this.mHasActionBarLineColor = true;
                return color;
            } catch (NotFoundException unused) {
                this.mHasActionBarLineColor = false;
            }
        }
        return typedValue.data;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = this.mOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mOrientation = i2;
            TypedValue typedValue = new TypedValue();
            int height = getHeight();
            if (getContext().getTheme().resolveAttribute(16843499, typedValue, true)) {
                try {
                    height = getResources().getDimensionPixelSize(typedValue.resourceId);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
            this.mRealPaddingBottom = getResources().getDimensionPixelOffset(R$dimen.op_toolbar_real_padding_bottom);
            android.view.ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.height = height;
            setLayoutParams(layoutParams);
        }
    }

    /* access modifiers changed from: protected */
    public int[] onCreateDrawableState(int i) {
        if (!this.mCollapsible) {
            return super.onCreateDrawableState(i);
        }
        int[] iArr = this.mTmpStatesArray;
        int[] onCreateDrawableState = super.onCreateDrawableState(i + iArr.length);
        iArr[0] = this.mCollapsible ? R$attr.op_state_collapsible : -R$attr.op_state_collapsible;
        iArr[1] = (!this.mCollapsible || !this.mCollapsed) ? R$attr.op_state_collapsed : -R$attr.op_state_collapsed;
        return android.widget.Toolbar.mergeDrawableStates(onCreateDrawableState, iArr);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public DecorToolbar getSupportWrap() {
        if (this.mWrapper == null) {
            this.mWrapper = new ToolbarWidgetWrapper(this, true);
        }
        return this.mWrapper;
    }

    public void setPopupTheme(int i) {
        if (this.mPopupTheme != i) {
            this.mPopupTheme = i;
            if (i == 0) {
                this.mPopupContext = getContext();
            } else {
                this.mPopupContext = new ContextThemeWrapper(getContext(), i);
            }
        }
    }

    public int getPopupTheme() {
        return this.mPopupTheme;
    }

    public void setTitleMargin(int i, int i2, int i3, int i4) {
        this.mTitleMarginStart = i;
        this.mTitleMarginTop = i2;
        this.mTitleMarginEnd = i3;
        this.mTitleMarginBottom = i4;
        requestLayout();
    }

    public int getTitleMarginStart() {
        return this.mTitleMarginStart;
    }

    public void setTitleMarginStart(int i) {
        this.mTitleMarginStart = i;
        requestLayout();
    }

    public int getTitleMarginTop() {
        return this.mTitleMarginTop;
    }

    public int getTitieTopWithoutOffset() {
        TextView textView = this.mTitleTextView;
        if (textView != null) {
            return textView.getTop() - (this.mPaddingTopOffset * 2);
        }
        return 0;
    }

    public void setTitleMarginTop(int i) {
        this.mTitleMarginTop = i;
        requestLayout();
    }

    public int getTitleMarginEnd() {
        return this.mTitleMarginEnd;
    }

    public void setTitleMarginEnd(int i) {
        this.mTitleMarginEnd = i;
        requestLayout();
    }

    public int getTitleMarginBottom() {
        return this.mTitleMarginBottom;
    }

    public void setTitleMarginBottom(int i) {
        this.mTitleMarginBottom = i;
        requestLayout();
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        ensureContentInsets();
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        boolean z = true;
        if (i != 1) {
            z = false;
        }
        rtlSpacingHelper.setDirection(z);
    }

    public void setLogo(int i) {
        setLogo(getContext().getDrawable(i));
    }

    public boolean canShowOverflowMenu() {
        if (getVisibility() == 0) {
            ActionMenuView actionMenuView = this.mMenuView;
            if (actionMenuView != null && actionMenuView.isOverflowReserved()) {
                return true;
            }
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        ActionMenuView actionMenuView = this.mMenuView;
        return actionMenuView != null && actionMenuView.isOverflowMenuShowing();
    }

    public boolean isOverflowMenuShowPending() {
        ActionMenuView actionMenuView = this.mMenuView;
        return actionMenuView != null && actionMenuView.isOverflowMenuShowPending();
    }

    public boolean showOverflowMenu() {
        ActionMenuView actionMenuView = this.mMenuView;
        return actionMenuView != null && actionMenuView.showOverflowMenu();
    }

    public boolean hideOverflowMenu() {
        ActionMenuView actionMenuView = this.mMenuView;
        return actionMenuView != null && actionMenuView.hideOverflowMenu();
    }

    public void dismissPopupMenus() {
        ActionMenuView actionMenuView = this.mMenuView;
        if (actionMenuView != null) {
            actionMenuView.dismissPopupMenus();
        }
    }

    public boolean isTitleTruncated() {
        TextView textView = this.mTitleTextView;
        if (textView == null) {
            return false;
        }
        Layout layout = textView.getLayout();
        if (layout == null) {
            return false;
        }
        int lineCount = layout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (layout.getEllipsisCount(i) > 0) {
                return true;
            }
        }
        return false;
    }

    public void setLogo(Drawable drawable) {
        if (drawable != null) {
            ensureLogoView();
            if (!isChildOrHidden(this.mLogoView)) {
                addSystemView(this.mLogoView, true);
            }
        } else {
            ImageView imageView = this.mLogoView;
            if (imageView != null && isChildOrHidden(imageView)) {
                removeView(this.mLogoView);
                this.mHiddenViews.remove(this.mLogoView);
            }
        }
        ImageView imageView2 = this.mLogoView;
        if (imageView2 != null) {
            imageView2.setImageDrawable(drawable);
        }
    }

    public Drawable getLogo() {
        ImageView imageView = this.mLogoView;
        if (imageView != null) {
            return imageView.getDrawable();
        }
        return null;
    }

    public void setLogoDescription(int i) {
        setLogoDescription(getContext().getText(i));
    }

    public void setLogoDescription(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            ensureLogoView();
        }
        ImageView imageView = this.mLogoView;
        if (imageView != null) {
            imageView.setContentDescription(charSequence);
        }
    }

    public CharSequence getLogoDescription() {
        ImageView imageView = this.mLogoView;
        if (imageView != null) {
            return imageView.getContentDescription();
        }
        return null;
    }

    private void ensureLogoView() {
        if (this.mLogoView == null) {
            this.mLogoView = new ImageView(getContext());
        }
    }

    public boolean hasExpandedActionView() {
        ExpandedActionViewMenuPresenter expandedActionViewMenuPresenter = this.mExpandedMenuPresenter;
        return (expandedActionViewMenuPresenter == null || expandedActionViewMenuPresenter.mCurrentExpandedItem == null) ? false : true;
    }

    public void collapseActionView() {
        MenuItemImpl menuItemImpl;
        ExpandedActionViewMenuPresenter expandedActionViewMenuPresenter = this.mExpandedMenuPresenter;
        if (expandedActionViewMenuPresenter == null) {
            menuItemImpl = null;
        } else {
            menuItemImpl = expandedActionViewMenuPresenter.mCurrentExpandedItem;
        }
        if (menuItemImpl != null) {
            menuItemImpl.collapseActionView();
        }
    }

    public CharSequence getTitle() {
        return this.mTitleText;
    }

    public void setTitle(int i) {
        setTitle(getContext().getText(i));
    }

    public void setTitle(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            if (this.mTitleTextView == null) {
                this.mTitleTextView = new TextView(getContext());
                this.mTitleTextView.setSingleLine();
                this.mTitleTextView.setEllipsize(TruncateAt.END);
                if (this.mTitleTextAppearance != 0) {
                    this.mTitleTextView.setTextAppearance(getContext(), this.mTitleTextAppearance);
                }
                int i = this.mTitleTextColor;
                if (i != 0) {
                    this.mTitleTextView.setTextColor(i);
                }
            }
            if (!isChildOrHidden(this.mTitleTextView)) {
                addSystemView(this.mTitleTextView, true);
            }
        } else {
            TextView textView = this.mTitleTextView;
            if (textView != null && isChildOrHidden(textView)) {
                removeView(this.mTitleTextView);
                this.mHiddenViews.remove(this.mTitleTextView);
            }
        }
        TextView textView2 = this.mTitleTextView;
        if (textView2 != null) {
            textView2.setText(charSequence);
        }
        this.mTitleText = charSequence;
    }

    public CharSequence getSubtitle() {
        return this.mSubtitleText;
    }

    public void setSubtitle(int i) {
        setSubtitle(getContext().getText(i));
    }

    public void setSubtitle(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            if (this.mSubtitleTextView == null) {
                this.mSubtitleTextView = new TextView(getContext());
                this.mSubtitleTextView.setSingleLine();
                this.mSubtitleTextView.setEllipsize(TruncateAt.END);
                if (this.mSubtitleTextAppearance != 0) {
                    this.mSubtitleTextView.setTextAppearance(getContext(), this.mSubtitleTextAppearance);
                }
                int i = this.mSubtitleTextColor;
                if (i != 0) {
                    this.mSubtitleTextView.setTextColor(i);
                }
            }
            if (!isChildOrHidden(this.mSubtitleTextView)) {
                addSystemView(this.mSubtitleTextView, true);
            }
        } else {
            TextView textView = this.mSubtitleTextView;
            if (textView != null && isChildOrHidden(textView)) {
                removeView(this.mSubtitleTextView);
                this.mHiddenViews.remove(this.mSubtitleTextView);
            }
        }
        TextView textView2 = this.mSubtitleTextView;
        if (textView2 != null) {
            textView2.setText(charSequence);
        }
        this.mSubtitleText = charSequence;
    }

    public void setTitleTextAppearance(Context context, int i) {
        this.mTitleTextAppearance = i;
        TextView textView = this.mTitleTextView;
        if (textView != null) {
            textView.setTextAppearance(getContext(), i);
        }
    }

    public void setSubtitleTextAppearance(Context context, int i) {
        this.mSubtitleTextAppearance = i;
        TextView textView = this.mSubtitleTextView;
        if (textView != null) {
            textView.setTextAppearance(getContext(), i);
        }
    }

    public void setTitleTextColor(int i) {
        this.mTitleTextColor = i;
        TextView textView = this.mTitleTextView;
        if (textView != null) {
            textView.setTextColor(i);
        }
    }

    public void setSubtitleTextColor(int i) {
        this.mSubtitleTextColor = i;
        TextView textView = this.mSubtitleTextView;
        if (textView != null) {
            textView.setTextColor(i);
        }
    }

    public CharSequence getNavigationContentDescription() {
        ImageButton imageButton = this.mMyNavButtonView;
        if (imageButton != null) {
            return imageButton.getContentDescription();
        }
        return null;
    }

    public void setNavigationContentDescription(int i) {
        setNavigationContentDescription(i != 0 ? getContext().getText(i) : null);
    }

    public void setNavigationContentDescription(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            ensureNavButtonView();
        }
        ImageButton imageButton = this.mMyNavButtonView;
        if (imageButton != null) {
            imageButton.setContentDescription(charSequence);
        }
    }

    public void setNavigationIcon(int i) {
        setNavigationIcon(getContext().getDrawable(i));
    }

    public void setNavigationIcon(Drawable drawable) {
        if (drawable != null) {
            ensureNavButtonView();
            if (!isChildOrHidden(this.mMyNavButtonView)) {
                addSystemView(this.mMyNavButtonView, true);
            }
        } else {
            ImageButton imageButton = this.mMyNavButtonView;
            if (imageButton != null && isChildOrHidden(imageButton)) {
                removeView(this.mMyNavButtonView);
                this.mHiddenViews.remove(this.mMyNavButtonView);
            }
        }
        ImageButton imageButton2 = this.mMyNavButtonView;
        if (imageButton2 != null) {
            imageButton2.setImageDrawable(drawable);
        }
    }

    public Drawable getNavigationIcon() {
        ImageButton imageButton = this.mMyNavButtonView;
        if (imageButton != null) {
            return imageButton.getDrawable();
        }
        return null;
    }

    public void setNavigationOnClickListener(OnClickListener onClickListener) {
        ensureNavButtonView();
        this.mMyNavButtonView.setOnClickListener(onClickListener);
    }

    public View getNavigationView() {
        return this.mMyNavButtonView;
    }

    public Menu getMenu() {
        ensureMenu();
        return this.mMenuView.getMenu();
    }

    public void setOverflowIcon(Drawable drawable) {
        ensureMenu();
        this.mMenuView.setOverflowIcon(drawable);
    }

    public Drawable getOverflowIcon() {
        ensureMenu();
        return this.mMenuView.getOverflowIcon();
    }

    private void ensureMenu() {
        ensureMenuView();
        if (this.mMenuView.peekMenu() == null) {
            MenuBuilder menuBuilder = (MenuBuilder) this.mMenuView.getMenu();
            if (this.mExpandedMenuPresenter == null) {
                this.mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
            }
            this.mMenuView.setExpandedActionViewsExclusive(true);
            menuBuilder.addMenuPresenter(this.mExpandedMenuPresenter, this.mPopupContext);
        }
    }

    private void ensureMenuView() {
        if (this.mMenuView == null) {
            this.mMenuView = new ActionMenuView(getContext());
            this.mMenuView.setToolbar(this);
            this.mMenuView.setPopupTheme(this.mPopupTheme);
            this.mMenuView.setOnMenuItemClickListener(this.mMenuViewItemClickListener);
            this.mMenuView.setMenuCallbacks(this.mActionMenuPresenterCallback, this.mMenuBuilderCallback);
            LayoutParams generateDefaultLayoutParams = generateDefaultLayoutParams();
            generateDefaultLayoutParams.gravity = 8388613 | (this.mButtonGravity & 16);
            this.mMenuView.setLayoutParams(generateDefaultLayoutParams);
            addSystemView(this.mMenuView, false);
        }
    }

    private MenuInflater getMenuInflater() {
        return new MenuInflater(getContext());
    }

    public void inflateMenu(int i) {
        getMenuInflater().inflate(i, getMenu());
    }

    public void setOnMenuItemClickListener(android.widget.Toolbar.OnMenuItemClickListener onMenuItemClickListener) {
        super.setOnMenuItemClickListener(onMenuItemClickListener);
        this.mOnMenuItemClickListener = onMenuItemClickListener;
    }

    public void setContentInsetsRelative(int i, int i2) {
        ensureContentInsets();
        this.mContentInsets.setRelative(i, i2);
    }

    public int getContentInsetStart() {
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        if (rtlSpacingHelper != null) {
            return rtlSpacingHelper.getStart();
        }
        return 0;
    }

    public int getContentInsetEnd() {
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        if (rtlSpacingHelper != null) {
            return rtlSpacingHelper.getEnd();
        }
        return 0;
    }

    public void setContentInsetsAbsolute(int i, int i2) {
        ensureContentInsets();
        this.mContentInsets.setAbsolute(i, i2);
    }

    public int getContentInsetLeft() {
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        if (rtlSpacingHelper != null) {
            return rtlSpacingHelper.getLeft();
        }
        return 0;
    }

    public int getContentInsetRight() {
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        if (rtlSpacingHelper != null) {
            return rtlSpacingHelper.getRight();
        }
        return 0;
    }

    public int getContentInsetStartWithNavigation() {
        int i = this.mContentInsetStartWithNavigation;
        return i != Integer.MIN_VALUE ? i : getContentInsetStart();
    }

    public void setContentInsetStartWithNavigation(int i) {
        if (i < 0) {
            i = Integer.MIN_VALUE;
        }
        if (i != this.mContentInsetStartWithNavigation) {
            this.mContentInsetStartWithNavigation = i;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    public int getContentInsetEndWithActions() {
        int i = this.mContentInsetEndWithActions;
        return i != Integer.MIN_VALUE ? i : getContentInsetEnd();
    }

    public void setContentInsetEndWithActions(int i) {
        if (i < 0) {
            i = Integer.MIN_VALUE;
        }
        if (i != this.mContentInsetEndWithActions) {
            this.mContentInsetEndWithActions = i;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    public int getCurrentContentInsetStart() {
        if (getNavigationIcon() != null) {
            return Math.max(getContentInsetStart(), Math.max(this.mContentInsetStartWithNavigation, 0));
        }
        return getContentInsetStart();
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0025  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0016  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getCurrentContentInsetEnd() {
        /*
            r2 = this;
            com.oneplus.lib.menu.ActionMenuView r0 = r2.mMenuView
            r1 = 0
            if (r0 == 0) goto L_0x0013
            com.oneplus.lib.menu.MenuBuilder r0 = r0.peekMenu()
            if (r0 == 0) goto L_0x0013
            boolean r0 = r0.hasVisibleItems()
            if (r0 == 0) goto L_0x0013
            r0 = 1
            goto L_0x0014
        L_0x0013:
            r0 = r1
        L_0x0014:
            if (r0 == 0) goto L_0x0025
            int r0 = r2.getContentInsetEnd()
            int r2 = r2.mContentInsetEndWithActions
            int r2 = java.lang.Math.max(r2, r1)
            int r2 = java.lang.Math.max(r0, r2)
            goto L_0x0029
        L_0x0025:
            int r2 = r2.getContentInsetEnd()
        L_0x0029:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.actionbar.Toolbar.getCurrentContentInsetEnd():int");
    }

    public int getCurrentContentInsetLeft() {
        if (ViewUtils.isLayoutRtl(this)) {
            return getCurrentContentInsetEnd();
        }
        return getCurrentContentInsetStart();
    }

    public int getCurrentContentInsetRight() {
        if (ViewUtils.isLayoutRtl(this)) {
            return getCurrentContentInsetStart();
        }
        return getCurrentContentInsetEnd();
    }

    private void ensureNavButtonView() {
        if (this.mMyNavButtonView == null) {
            this.mMyNavButtonView = new ImageButton(getContext(), null, 0, this.mNavButtonStyle);
            LayoutParams generateDefaultLayoutParams = generateDefaultLayoutParams();
            generateDefaultLayoutParams.gravity = 8388611 | (this.mButtonGravity & 16);
            this.mMyNavButtonView.setLayoutParams(generateDefaultLayoutParams);
        }
    }

    /* access modifiers changed from: private */
    public void ensureCollapseButtonView() {
        if (this.mCollapseButtonView == null) {
            this.mCollapseButtonView = new ImageButton(getContext(), null, 0, this.mNavButtonStyle);
            this.mCollapseButtonView.setImageDrawable(this.mCollapseIcon);
            this.mCollapseButtonView.setContentDescription(this.mCollapseDescription);
            LayoutParams generateDefaultLayoutParams = generateDefaultLayoutParams();
            generateDefaultLayoutParams.gravity = 8388611 | (this.mButtonGravity & 16);
            generateDefaultLayoutParams.mViewType = 2;
            this.mCollapseButtonView.setLayoutParams(generateDefaultLayoutParams);
            this.mCollapseButtonView.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    Toolbar.this.collapseActionView();
                }
            });
        }
    }

    private void addSystemView(View view, boolean z) {
        LayoutParams layoutParams;
        android.view.ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
        if (layoutParams2 == null) {
            layoutParams = generateDefaultLayoutParams();
        } else if (!checkLayoutParams(layoutParams2)) {
            layoutParams = generateLayoutParams(layoutParams2);
        } else {
            layoutParams = (LayoutParams) layoutParams2;
        }
        layoutParams.mViewType = 1;
        if (!z || this.mExpandedActionView == null) {
            addView(view, layoutParams);
            return;
        }
        view.setLayoutParams(layoutParams);
        this.mHiddenViews.add(view);
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        ExpandedActionViewMenuPresenter expandedActionViewMenuPresenter = this.mExpandedMenuPresenter;
        if (expandedActionViewMenuPresenter != null) {
            MenuItemImpl menuItemImpl = expandedActionViewMenuPresenter.mCurrentExpandedItem;
            if (menuItemImpl != null) {
                savedState.expandedMenuItemId = menuItemImpl.getItemId();
            }
        }
        savedState.isOverflowOpen = isOverflowMenuShowing();
        return savedState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        ActionMenuView actionMenuView = this.mMenuView;
        MenuBuilder peekMenu = actionMenuView != null ? actionMenuView.peekMenu() : null;
        int i = savedState.expandedMenuItemId;
        if (!(i == 0 || this.mExpandedMenuPresenter == null || peekMenu == null)) {
            MenuItem findItem = peekMenu.findItem(i);
            if (findItem != null) {
                findItem.expandActionView();
            }
        }
        if (savedState.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    private void postShowOverflowMenu() {
        removeCallbacks(this.mShowOverflowMenuRunnable);
        post(this.mShowOverflowMenuRunnable);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mShowOverflowMenuRunnable);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mEatingTouch = false;
        }
        if (!this.mEatingTouch) {
            boolean onTouchEvent = super.onTouchEvent(motionEvent);
            if (actionMasked == 0 && !onTouchEvent) {
                this.mEatingTouch = true;
            }
        }
        if (actionMasked == 1 || actionMasked == 3) {
            this.mEatingTouch = false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onSetLayoutParams(View view, android.view.ViewGroup.LayoutParams layoutParams) {
        if (!checkLayoutParams(layoutParams)) {
            view.setLayoutParams(generateLayoutParams(layoutParams));
        }
    }

    private void measureChildConstrained(View view, int i, int i2, int i3, int i4, int i5) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        int childMeasureSpec = android.widget.Toolbar.getChildMeasureSpec(i, getPaddingLeft() + getPaddingRight() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + i2, marginLayoutParams.width);
        int childMeasureSpec2 = android.widget.Toolbar.getChildMeasureSpec(i3, getPaddingTop() + this.mPaddingTopOffset + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height);
        int mode = MeasureSpec.getMode(childMeasureSpec2);
        if (mode != 1073741824 && i5 >= 0) {
            if (mode != 0) {
                i5 = Math.min(MeasureSpec.getSize(childMeasureSpec2), i5);
            }
            childMeasureSpec2 = MeasureSpec.makeMeasureSpec(i5, 1073741824);
        }
        view.measure(childMeasureSpec, childMeasureSpec2);
    }

    private int measureSearchChildCollapseMargins(View view, int i, int i2, int i3, int i4, int[] iArr) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        int i5 = 0;
        int i6 = marginLayoutParams.leftMargin - iArr[0];
        int i7 = marginLayoutParams.rightMargin - iArr[1];
        int max = Math.max(0, i6) + Math.max(0, i7);
        iArr[0] = Math.max(0, -i6);
        iArr[1] = Math.max(0, -i7);
        int paddingLeft = getPaddingLeft() + getPaddingRight() + max + i2;
        int childMeasureSpec = android.widget.Toolbar.getChildMeasureSpec(i, paddingLeft, (getMeasuredWidth() - paddingLeft) + Math.abs(i7));
        int paddingTop = getPaddingTop() + this.mPaddingTopOffset;
        if (this.mInsetPaddingTopInGestureMode) {
            i5 = getStatusBarHeight();
        }
        view.measure(childMeasureSpec, android.widget.Toolbar.getChildMeasureSpec(i3, paddingTop + i5 + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height));
        return view.getMeasuredWidth() + max;
    }

    private int measureChildCollapseMargins(View view, int i, int i2, int i3, int i4, int[] iArr) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        int i5 = marginLayoutParams.leftMargin - iArr[0];
        int i6 = marginLayoutParams.rightMargin - iArr[1];
        int max = Math.max(0, i5) + Math.max(0, i6);
        iArr[0] = Math.max(0, -i5);
        iArr[1] = Math.max(0, -i6);
        view.measure(android.widget.Toolbar.getChildMeasureSpec(i, getPaddingLeft() + getPaddingRight() + max + i2, marginLayoutParams.width), android.widget.Toolbar.getChildMeasureSpec(i3, getPaddingTop() + this.mPaddingTopOffset + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height));
        return view.getMeasuredWidth() + max;
    }

    private boolean shouldCollapse() {
        if (!this.mCollapsible) {
            return false;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (shouldLayout(childAt) && childAt.getMeasuredWidth() > 0 && childAt.getMeasuredHeight() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowInsetInGestureBarMode() {
        boolean gestureButtonEnabled = AppUtils.gestureButtonEnabled(getContext());
        GestureBarAdapter gestureBarAdapter = (GestureBarAdapter) getContext().getClass().getAnnotation(GestureBarAdapter.class);
        boolean z = gestureBarAdapter != null && gestureBarAdapter.transparentGestureButton();
        if (!gestureButtonEnabled || !z) {
            return false;
        }
        return true;
    }

    private int getStatusBarHeight() {
        return GestureBarAdapterPolicy.getStatusBarHeight(getContext());
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        char c;
        char c2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        boolean shouldLayout = shouldLayout(this.mMyNavButtonView);
        int[] iArr = this.mTempMargins;
        if (ViewUtils.isLayoutRtl(this)) {
            c2 = 1;
            c = 0;
        } else {
            c = 1;
            c2 = 0;
        }
        if (shouldLayout) {
            measureChildConstrained(this.mMyNavButtonView, i, 0, i2, 0, this.mMaxButtonHeight);
            i5 = this.mMyNavButtonView.getMeasuredWidth() + getHorizontalMargins(this.mMyNavButtonView);
            i4 = Math.max(0, this.mMyNavButtonView.getMeasuredHeight() + getVerticalMargins(this.mMyNavButtonView));
            i3 = android.widget.Toolbar.combineMeasuredStates(0, this.mMyNavButtonView.getMeasuredState());
        } else {
            i5 = 0;
            i4 = 0;
            i3 = 0;
        }
        if (shouldLayout(this.mCollapseButtonView)) {
            measureChildConstrained(this.mCollapseButtonView, i, 0, i2, 0, this.mMaxButtonHeight);
            i5 = this.mCollapseButtonView.getMeasuredWidth() + getHorizontalMargins(this.mCollapseButtonView);
            i4 = Math.max(i4, this.mCollapseButtonView.getMeasuredHeight() + getVerticalMargins(this.mCollapseButtonView));
            i3 = android.widget.Toolbar.combineMeasuredStates(i3, this.mCollapseButtonView.getMeasuredState());
        }
        int currentContentInsetStart = getCurrentContentInsetStart();
        int max = 0 + Math.max(currentContentInsetStart, i5);
        iArr[c2] = Math.max(0, currentContentInsetStart - i5);
        if (shouldLayout(this.mMenuView)) {
            measureChildConstrained(this.mMenuView, i, max, i2, 0, this.mMaxButtonHeight);
            i6 = this.mMenuView.getMeasuredWidth() + getHorizontalMargins(this.mMenuView);
            i4 = Math.max(i4, this.mMenuView.getMeasuredHeight() + getVerticalMargins(this.mMenuView));
            i3 = android.widget.Toolbar.combineMeasuredStates(i3, this.mMenuView.getMeasuredState());
        } else {
            i6 = 0;
        }
        int currentContentInsetEnd = getCurrentContentInsetEnd();
        int max2 = max + Math.max(currentContentInsetEnd, i6);
        iArr[c] = Math.max(0, currentContentInsetEnd - i6);
        if (shouldLayout(this.mExpandedActionView)) {
            if (!(this.mExpandedActionView instanceof CollapsibleActionView) || this.mMenuView.getChildCount() != 1) {
                i10 = measureChildCollapseMargins(this.mExpandedActionView, i, max2, i2, 0, iArr);
            } else {
                i10 = measureSearchChildCollapseMargins(this.mExpandedActionView, i, max2, i2, 0, iArr);
            }
            max2 += i10;
            i4 = Math.max(i4, this.mExpandedActionView.getMeasuredHeight() + getVerticalMargins(this.mExpandedActionView));
            i3 = android.widget.Toolbar.combineMeasuredStates(i3, this.mExpandedActionView.getMeasuredState());
        }
        if (shouldLayout(this.mLogoView)) {
            max2 += measureChildCollapseMargins(this.mLogoView, i, max2, i2, 0, iArr);
            i4 = Math.max(i4, this.mLogoView.getMeasuredHeight() + getVerticalMargins(this.mLogoView));
            i3 = android.widget.Toolbar.combineMeasuredStates(i3, this.mLogoView.getMeasuredState());
        }
        int childCount = getChildCount();
        int i11 = i4;
        int i12 = max2;
        for (int i13 = 0; i13 < childCount; i13++) {
            View childAt = getChildAt(i13);
            if (((LayoutParams) childAt.getLayoutParams()).mViewType == 0 && shouldLayout(childAt)) {
                View view = childAt;
                i12 += measureChildCollapseMargins(childAt, i, i12, i2, 0, iArr);
                View view2 = view;
                i11 = Math.max(i11, view.getMeasuredHeight() + getVerticalMargins(view2));
                i3 = android.widget.Toolbar.combineMeasuredStates(i3, view2.getMeasuredState());
            }
        }
        int i14 = this.mTitleMarginTop + this.mTitleMarginBottom;
        int i15 = this.mTitleMarginStart + this.mTitleMarginEnd;
        if (shouldLayout(this.mTitleTextView)) {
            measureChildCollapseMargins(this.mTitleTextView, i, i12 + i15, i2, i14, iArr);
            int measuredWidth = this.mTitleTextView.getMeasuredWidth() + getHorizontalMargins(this.mTitleTextView);
            i7 = this.mTitleTextView.getMeasuredHeight() + getVerticalMargins(this.mTitleTextView);
            i9 = android.widget.Toolbar.combineMeasuredStates(i3, this.mTitleTextView.getMeasuredState());
            i8 = measuredWidth;
        } else {
            i7 = 0;
            i9 = i3;
            i8 = 0;
        }
        if (shouldLayout(this.mSubtitleTextView)) {
            int i16 = i7 + i14;
            int i17 = i9;
            i8 = Math.max(i8, measureChildCollapseMargins(this.mSubtitleTextView, i, i12 + i15, i2, i16, iArr));
            i7 += this.mSubtitleTextView.getMeasuredHeight() + getVerticalMargins(this.mSubtitleTextView);
            i9 = android.widget.Toolbar.combineMeasuredStates(i17, this.mSubtitleTextView.getMeasuredState());
        } else {
            int i18 = i9;
        }
        int i19 = i12 + i8;
        int max3 = Math.max(i11, i7) + getPaddingTop() + getPaddingBottom();
        int i20 = i;
        int resolveSizeAndState = android.widget.Toolbar.resolveSizeAndState(Math.max(i19 + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), i20, -16777216 & i9);
        int resolveSizeAndState2 = android.widget.Toolbar.resolveSizeAndState(Math.max(max3, getSuggestedMinimumHeight()), i2, i9 << 16);
        if (this.mNeedResetPadding) {
            this.mPaddingTopOffset = (resolveSizeAndState2 - this.mMaxButtonHeight) / 2;
            int i21 = this.mPaddingTopOffset;
            int i22 = this.mRealPaddingBottom;
            if (i21 < i22) {
                this.mPaddingTopOffset = i21 + (i22 - i21);
            } else {
                this.mPaddingTopOffset = i21 - (i21 - i22);
            }
        }
        if (getFitsSystemWindows()) {
            resolveSizeAndState2 += this.mPaddingTopOffset;
        }
        this.mInsetPaddingTopInGestureMode = shouldShowInsetInGestureBarMode();
        if (!this.mInsetPaddingTopInGestureMode || this.mMeasuredGestureBar) {
            int i23 = 16777215 & resolveSizeAndState2;
            int i24 = this.mHeightWithGestureBar;
            if (i23 < i24) {
                resolveSizeAndState2 = i24;
            }
        } else {
            int statusBarHeight = getStatusBarHeight() + resolveSizeAndState2;
            this.mHeightWithGestureBar = statusBarHeight;
            this.mMeasuredGestureBar = true;
            resolveSizeAndState2 = statusBarHeight;
        }
        if (shouldCollapse()) {
            resolveSizeAndState2 = 0;
        }
        setMeasuredDimension(resolveSizeAndState, resolveSizeAndState2);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x02c0 A[LOOP:0: B:106:0x02be->B:107:0x02c0, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x02e3 A[LOOP:1: B:109:0x02e1->B:110:0x02e3, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x030e  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x031d A[LOOP:2: B:117:0x031b->B:118:0x031d, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0085  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00c5  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00dc  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0113  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x012b  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0139  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x013c  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0140  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0143  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0176  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01b0  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x01d2  */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x0245  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onLayout(boolean r19, int r20, int r21, int r22, int r23) {
        /*
            r18 = this;
            r0 = r18
            int r1 = r18.getLayoutDirection()
            r2 = 1
            r3 = 0
            if (r1 != r2) goto L_0x000c
            r1 = r2
            goto L_0x000d
        L_0x000c:
            r1 = r3
        L_0x000d:
            android.widget.ImageButton r4 = r0.mMyNavButtonView
            boolean r4 = r0.shouldLayout(r4)
            android.widget.ImageButton r5 = r0.mCollapseButtonView
            boolean r5 = r0.shouldLayout(r5)
            int r6 = r18.getPaddingLeft()
            int r7 = r18.getPaddingRight()
            int r8 = r18.getStatusBarHeight()
            int r9 = r18.getPaddingBottom()
            int r10 = r18.getWidth()
            int r11 = r18.getHeight()
            int r12 = r18.getPaddingTop()
            int r13 = r0.mPaddingTopOffset
            int r12 = r12 + r13
            boolean r13 = r0.mInsetPaddingTopInGestureMode
            if (r13 == 0) goto L_0x003e
            r13 = r8
            goto L_0x003f
        L_0x003e:
            r13 = r3
        L_0x003f:
            int r12 = r12 + r13
            int r13 = r18.getPaddingLeft()
            int r14 = r10 - r7
            int[] r15 = r0.mTempMargins
            r15[r2] = r3
            r15[r3] = r3
            int r2 = r18.getMinimumHeight()
            if (r2 < 0) goto L_0x0059
            int r3 = r23 - r21
            int r3 = java.lang.Math.min(r2, r3)
            goto L_0x005a
        L_0x0059:
            r3 = 0
        L_0x005a:
            if (r4 == 0) goto L_0x006b
            if (r1 == 0) goto L_0x0065
            android.widget.ImageButton r2 = r0.mMyNavButtonView
            int r2 = r0.layoutChildRight(r2, r14, r15, r3)
            goto L_0x006c
        L_0x0065:
            android.widget.ImageButton r2 = r0.mMyNavButtonView
            int r13 = r0.layoutChildLeft(r2, r13, r15, r3)
        L_0x006b:
            r2 = r14
        L_0x006c:
            if (r5 == 0) goto L_0x007d
            if (r1 == 0) goto L_0x0077
            android.widget.ImageButton r4 = r0.mCollapseButtonView
            int r2 = r0.layoutChildRight(r4, r2, r15, r3)
            goto L_0x007d
        L_0x0077:
            android.widget.ImageButton r4 = r0.mCollapseButtonView
            int r13 = r0.layoutChildLeft(r4, r13, r15, r3)
        L_0x007d:
            com.oneplus.lib.menu.ActionMenuView r4 = r0.mMenuView
            boolean r4 = r0.shouldLayout(r4)
            if (r4 == 0) goto L_0x0094
            if (r1 == 0) goto L_0x008e
            com.oneplus.lib.menu.ActionMenuView r4 = r0.mMenuView
            int r13 = r0.layoutChildLeft(r4, r13, r15, r3)
            goto L_0x0094
        L_0x008e:
            com.oneplus.lib.menu.ActionMenuView r4 = r0.mMenuView
            int r2 = r0.layoutChildRight(r4, r2, r15, r3)
        L_0x0094:
            int r4 = r18.getCurrentContentInsetLeft()
            int r5 = r18.getCurrentContentInsetRight()
            r22 = r7
            int r7 = r4 - r13
            r16 = r6
            r6 = 0
            int r7 = java.lang.Math.max(r6, r7)
            r15[r6] = r7
            int r7 = r14 - r2
            int r7 = r5 - r7
            int r7 = java.lang.Math.max(r6, r7)
            r6 = 1
            r15[r6] = r7
            int r4 = java.lang.Math.max(r13, r4)
            int r14 = r14 - r5
            int r2 = java.lang.Math.min(r2, r14)
            android.view.View r5 = r0.mExpandedActionView
            boolean r5 = r0.shouldLayout(r5)
            if (r5 == 0) goto L_0x00d4
            if (r1 == 0) goto L_0x00ce
            android.view.View r5 = r0.mExpandedActionView
            int r2 = r0.layoutChildRight(r5, r2, r15, r3)
            goto L_0x00d4
        L_0x00ce:
            android.view.View r5 = r0.mExpandedActionView
            int r4 = r0.layoutChildLeft(r5, r4, r15, r3)
        L_0x00d4:
            android.widget.ImageView r5 = r0.mLogoView
            boolean r5 = r0.shouldLayout(r5)
            if (r5 == 0) goto L_0x00eb
            if (r1 == 0) goto L_0x00e5
            android.widget.ImageView r5 = r0.mLogoView
            int r2 = r0.layoutChildRight(r5, r2, r15, r3)
            goto L_0x00eb
        L_0x00e5:
            android.widget.ImageView r5 = r0.mLogoView
            int r4 = r0.layoutChildLeft(r5, r4, r15, r3)
        L_0x00eb:
            android.widget.TextView r5 = r0.mTitleTextView
            boolean r5 = r0.shouldLayout(r5)
            android.widget.TextView r6 = r0.mSubtitleTextView
            boolean r6 = r0.shouldLayout(r6)
            if (r5 == 0) goto L_0x0110
            android.widget.TextView r7 = r0.mTitleTextView
            android.view.ViewGroup$LayoutParams r7 = r7.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r7 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r7
            int r13 = r7.topMargin
            android.widget.TextView r14 = r0.mTitleTextView
            int r14 = r14.getMeasuredHeight()
            int r13 = r13 + r14
            int r7 = r7.bottomMargin
            int r13 = r13 + r7
            r7 = 0
            int r13 = r13 + r7
            goto L_0x0111
        L_0x0110:
            r13 = 0
        L_0x0111:
            if (r6 == 0) goto L_0x012b
            android.widget.TextView r7 = r0.mSubtitleTextView
            android.view.ViewGroup$LayoutParams r7 = r7.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r7 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r7
            int r14 = r7.topMargin
            r17 = r10
            android.widget.TextView r10 = r0.mSubtitleTextView
            int r10 = r10.getMeasuredHeight()
            int r14 = r14 + r10
            int r7 = r7.bottomMargin
            int r14 = r14 + r7
            int r13 = r13 + r14
            goto L_0x012d
        L_0x012b:
            r17 = r10
        L_0x012d:
            if (r5 != 0) goto L_0x0137
            if (r6 == 0) goto L_0x0132
            goto L_0x0137
        L_0x0132:
            r21 = r3
        L_0x0134:
            r1 = 0
            goto L_0x02b0
        L_0x0137:
            if (r5 == 0) goto L_0x013c
            android.widget.TextView r7 = r0.mTitleTextView
            goto L_0x013e
        L_0x013c:
            android.widget.TextView r7 = r0.mSubtitleTextView
        L_0x013e:
            if (r6 == 0) goto L_0x0143
            android.widget.TextView r10 = r0.mSubtitleTextView
            goto L_0x0145
        L_0x0143:
            android.widget.TextView r10 = r0.mTitleTextView
        L_0x0145:
            android.view.ViewGroup$LayoutParams r7 = r7.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r7 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r7
            android.view.ViewGroup$LayoutParams r10 = r10.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r10 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r10
            if (r5 == 0) goto L_0x015b
            android.widget.TextView r14 = r0.mTitleTextView
            int r14 = r14.getMeasuredWidth()
            if (r14 > 0) goto L_0x0165
        L_0x015b:
            if (r6 == 0) goto L_0x0169
            android.widget.TextView r14 = r0.mSubtitleTextView
            int r14 = r14.getMeasuredWidth()
            if (r14 <= 0) goto L_0x0169
        L_0x0165:
            r21 = r3
            r14 = 1
            goto L_0x016c
        L_0x0169:
            r21 = r3
            r14 = 0
        L_0x016c:
            int r3 = r0.mGravity
            r3 = r3 & 16
            r23 = r4
            r4 = 48
            if (r3 == r4) goto L_0x01b0
            r4 = 80
            if (r3 == r4) goto L_0x01a6
            int r3 = r11 - r12
            int r3 = r3 - r9
            int r3 = r3 - r13
            int r3 = r3 / 2
            int r4 = r7.topMargin
            int r8 = r0.mTitleMarginTop
            int r4 = r4 + r8
            if (r3 >= r4) goto L_0x018d
            int r3 = r7.topMargin
            int r4 = r0.mTitleMarginTop
            int r3 = r3 + r4
            goto L_0x01a4
        L_0x018d:
            int r11 = r11 - r9
            int r11 = r11 - r13
            int r11 = r11 - r3
            int r11 = r11 - r12
            int r4 = r7.bottomMargin
            int r7 = r0.mTitleMarginBottom
            int r4 = r4 + r7
            if (r11 >= r4) goto L_0x01a4
            int r4 = r10.bottomMargin
            int r7 = r0.mTitleMarginBottom
            int r4 = r4 + r7
            int r4 = r4 - r11
            int r3 = r3 - r4
            r4 = 0
            int r3 = java.lang.Math.max(r4, r3)
        L_0x01a4:
            int r12 = r12 + r3
            goto L_0x01d0
        L_0x01a6:
            int r11 = r11 - r9
            int r3 = r10.bottomMargin
            int r11 = r11 - r3
            int r3 = r0.mTitleMarginBottom
            int r11 = r11 - r3
            int r12 = r11 - r13
            goto L_0x01d0
        L_0x01b0:
            boolean r3 = r0.mInsetPaddingTopInGestureMode
            if (r3 == 0) goto L_0x01c2
            int r3 = r18.getPaddingTop()
            int r4 = r0.mPaddingTopOffset
            int r3 = r3 + r4
            int r3 = r3 + r8
            int r4 = r7.topMargin
            int r3 = r3 + r4
            int r4 = r0.mTitleMarginTop
            goto L_0x01ce
        L_0x01c2:
            int r3 = r18.getPaddingTop()
            int r4 = r0.mPaddingTopOffset
            int r3 = r3 + r4
            int r4 = r7.topMargin
            int r3 = r3 + r4
            int r4 = r0.mTitleMarginTop
        L_0x01ce:
            int r12 = r3 + r4
        L_0x01d0:
            if (r1 == 0) goto L_0x0245
            if (r14 == 0) goto L_0x01d8
            int r3 = r0.mTitleMarginStart
            r1 = 1
            goto L_0x01da
        L_0x01d8:
            r1 = 1
            r3 = 0
        L_0x01da:
            r4 = r15[r1]
            int r3 = r3 - r4
            r4 = 0
            int r7 = java.lang.Math.max(r4, r3)
            int r2 = r2 - r7
            int r3 = -r3
            int r3 = java.lang.Math.max(r4, r3)
            r15[r1] = r3
            if (r5 == 0) goto L_0x0210
            android.widget.TextView r1 = r0.mTitleTextView
            android.view.ViewGroup$LayoutParams r1 = r1.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r1 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r1
            android.widget.TextView r3 = r0.mTitleTextView
            int r3 = r3.getMeasuredWidth()
            int r3 = r2 - r3
            android.widget.TextView r4 = r0.mTitleTextView
            int r4 = r4.getMeasuredHeight()
            int r4 = r4 + r12
            android.widget.TextView r5 = r0.mTitleTextView
            r5.layout(r3, r12, r2, r4)
            int r5 = r0.mTitleMarginEnd
            int r3 = r3 - r5
            int r1 = r1.bottomMargin
            int r12 = r4 + r1
            goto L_0x0211
        L_0x0210:
            r3 = r2
        L_0x0211:
            if (r6 == 0) goto L_0x0239
            android.widget.TextView r1 = r0.mSubtitleTextView
            android.view.ViewGroup$LayoutParams r1 = r1.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r1 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r1
            int r4 = r1.topMargin
            int r12 = r12 + r4
            android.widget.TextView r4 = r0.mSubtitleTextView
            int r4 = r4.getMeasuredWidth()
            int r4 = r2 - r4
            android.widget.TextView r5 = r0.mSubtitleTextView
            int r5 = r5.getMeasuredHeight()
            int r5 = r5 + r12
            android.widget.TextView r6 = r0.mSubtitleTextView
            r6.layout(r4, r12, r2, r5)
            int r4 = r0.mTitleMarginEnd
            int r4 = r2 - r4
            int r1 = r1.bottomMargin
            goto L_0x023a
        L_0x0239:
            r4 = r2
        L_0x023a:
            if (r14 == 0) goto L_0x0241
            int r1 = java.lang.Math.min(r3, r4)
            r2 = r1
        L_0x0241:
            r4 = r23
            goto L_0x0134
        L_0x0245:
            if (r14 == 0) goto L_0x024b
            int r3 = r0.mTitleMarginStart
            r1 = 0
            goto L_0x024d
        L_0x024b:
            r1 = 0
            r3 = 0
        L_0x024d:
            r4 = r15[r1]
            int r3 = r3 - r4
            int r4 = java.lang.Math.max(r1, r3)
            int r4 = r23 + r4
            int r3 = -r3
            int r3 = java.lang.Math.max(r1, r3)
            r15[r1] = r3
            if (r5 == 0) goto L_0x0282
            android.widget.TextView r3 = r0.mTitleTextView
            android.view.ViewGroup$LayoutParams r3 = r3.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r3 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r3
            android.widget.TextView r5 = r0.mTitleTextView
            int r5 = r5.getMeasuredWidth()
            int r5 = r5 + r4
            android.widget.TextView r7 = r0.mTitleTextView
            int r7 = r7.getMeasuredHeight()
            int r7 = r7 + r12
            android.widget.TextView r8 = r0.mTitleTextView
            r8.layout(r4, r12, r5, r7)
            int r8 = r0.mTitleMarginEnd
            int r5 = r5 + r8
            int r3 = r3.bottomMargin
            int r12 = r7 + r3
            goto L_0x0283
        L_0x0282:
            r5 = r4
        L_0x0283:
            if (r6 == 0) goto L_0x02a9
            android.widget.TextView r3 = r0.mSubtitleTextView
            android.view.ViewGroup$LayoutParams r3 = r3.getLayoutParams()
            com.oneplus.lib.widget.actionbar.Toolbar$LayoutParams r3 = (com.oneplus.lib.widget.actionbar.Toolbar.LayoutParams) r3
            int r6 = r3.topMargin
            int r12 = r12 + r6
            android.widget.TextView r6 = r0.mSubtitleTextView
            int r6 = r6.getMeasuredWidth()
            int r6 = r6 + r4
            android.widget.TextView r7 = r0.mSubtitleTextView
            int r7 = r7.getMeasuredHeight()
            int r7 = r7 + r12
            android.widget.TextView r8 = r0.mSubtitleTextView
            r8.layout(r4, r12, r6, r7)
            int r7 = r0.mTitleMarginEnd
            int r6 = r6 + r7
            int r3 = r3.bottomMargin
            goto L_0x02aa
        L_0x02a9:
            r6 = r4
        L_0x02aa:
            if (r14 == 0) goto L_0x02b0
            int r4 = java.lang.Math.max(r5, r6)
        L_0x02b0:
            java.util.ArrayList<android.view.View> r3 = r0.mTempViews
            r5 = 3
            r0.addCustomViewsWithGravity(r3, r5)
            java.util.ArrayList<android.view.View> r3 = r0.mTempViews
            int r3 = r3.size()
            r5 = r4
            r4 = r1
        L_0x02be:
            if (r4 >= r3) goto L_0x02d1
            java.util.ArrayList<android.view.View> r6 = r0.mTempViews
            java.lang.Object r6 = r6.get(r4)
            android.view.View r6 = (android.view.View) r6
            r7 = r21
            int r5 = r0.layoutChildLeft(r6, r5, r15, r7)
            int r4 = r4 + 1
            goto L_0x02be
        L_0x02d1:
            r7 = r21
            java.util.ArrayList<android.view.View> r3 = r0.mTempViews
            r4 = 5
            r0.addCustomViewsWithGravity(r3, r4)
            java.util.ArrayList<android.view.View> r3 = r0.mTempViews
            int r3 = r3.size()
            r4 = r2
            r2 = r1
        L_0x02e1:
            if (r2 >= r3) goto L_0x02f2
            java.util.ArrayList<android.view.View> r6 = r0.mTempViews
            java.lang.Object r6 = r6.get(r2)
            android.view.View r6 = (android.view.View) r6
            int r4 = r0.layoutChildRight(r6, r4, r15, r7)
            int r2 = r2 + 1
            goto L_0x02e1
        L_0x02f2:
            java.util.ArrayList<android.view.View> r2 = r0.mTempViews
            r3 = 1
            r0.addCustomViewsWithGravity(r2, r3)
            java.util.ArrayList<android.view.View> r2 = r0.mTempViews
            int r2 = r0.getViewListMeasuredWidth(r2, r15)
            int r10 = r17 - r16
            int r10 = r10 - r22
            int r10 = r10 / 2
            int r6 = r16 + r10
            int r3 = r2 / 2
            int r3 = r6 - r3
            int r2 = r2 + r3
            if (r3 >= r5) goto L_0x030e
            goto L_0x0315
        L_0x030e:
            if (r2 <= r4) goto L_0x0314
            int r2 = r2 - r4
            int r5 = r3 - r2
            goto L_0x0315
        L_0x0314:
            r5 = r3
        L_0x0315:
            java.util.ArrayList<android.view.View> r2 = r0.mTempViews
            int r2 = r2.size()
        L_0x031b:
            if (r1 >= r2) goto L_0x032c
            java.util.ArrayList<android.view.View> r3 = r0.mTempViews
            java.lang.Object r3 = r3.get(r1)
            android.view.View r3 = (android.view.View) r3
            int r5 = r0.layoutChildLeft(r3, r5, r15, r7)
            int r1 = r1 + 1
            goto L_0x031b
        L_0x032c:
            java.util.ArrayList<android.view.View> r0 = r0.mTempViews
            r0.clear()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.actionbar.Toolbar.onLayout(boolean, int, int, int, int):void");
    }

    private int getViewListMeasuredWidth(List<View> list, int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        int size = list.size();
        int i3 = i2;
        int i4 = i;
        int i5 = 0;
        int i6 = 0;
        while (i5 < size) {
            View view = (View) list.get(i5);
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            int i7 = layoutParams.leftMargin - i4;
            int i8 = layoutParams.rightMargin - i3;
            int max = Math.max(0, i7);
            int max2 = Math.max(0, i8);
            int max3 = Math.max(0, -i7);
            i6 += max + view.getMeasuredWidth() + max2;
            i5++;
            i3 = Math.max(0, -i8);
            i4 = max3;
        }
        return i6;
    }

    private int layoutChildLeft(View view, int i, int[] iArr, int i2) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        int i3 = layoutParams.leftMargin - iArr[0];
        int max = i + Math.max(0, i3);
        iArr[0] = Math.max(0, -i3);
        int childTop = getChildTop(view, i2);
        int measuredWidth = view.getMeasuredWidth();
        view.layout(max, childTop, max + measuredWidth, view.getMeasuredHeight() + childTop);
        return max + measuredWidth + layoutParams.rightMargin;
    }

    private int layoutChildRight(View view, int i, int[] iArr, int i2) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        int i3 = layoutParams.rightMargin - iArr[1];
        int max = i - Math.max(0, i3);
        iArr[1] = Math.max(0, -i3);
        int childTop = getChildTop(view, i2);
        int measuredWidth = view.getMeasuredWidth();
        view.layout(max - measuredWidth, childTop, max, view.getMeasuredHeight() + childTop);
        return max - (measuredWidth + layoutParams.leftMargin);
    }

    private int getChildTop(View view, int i) {
        int i2;
        int statusBarHeight = getStatusBarHeight();
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        int measuredHeight = view.getMeasuredHeight();
        int i3 = i > 0 ? (measuredHeight - i) / 2 : 0;
        int childVerticalGravity = getChildVerticalGravity(layoutParams.gravity);
        if (childVerticalGravity == 48) {
            if (this.mInsetPaddingTopInGestureMode) {
                i2 = getPaddingTop() + this.mPaddingTopOffset + statusBarHeight;
            } else {
                i2 = getPaddingTop() + this.mPaddingTopOffset;
            }
            return i2 - i3;
        } else if (childVerticalGravity == 80) {
            return (((getHeight() - getPaddingBottom()) - measuredHeight) - layoutParams.bottomMargin) - i3;
        } else {
            int paddingTop = getPaddingTop() + this.mPaddingTopOffset;
            if (!this.mInsetPaddingTopInGestureMode) {
                statusBarHeight = 0;
            }
            int i4 = paddingTop + statusBarHeight;
            int paddingBottom = getPaddingBottom();
            int height = getHeight();
            int i5 = (((height - i4) - paddingBottom) - measuredHeight) / 2;
            if (i5 < layoutParams.topMargin) {
                i5 = layoutParams.topMargin;
            } else {
                int i6 = (((height - paddingBottom) - measuredHeight) - i5) - i4;
                if (i6 < layoutParams.bottomMargin) {
                    i5 = Math.max(0, i5 - (layoutParams.bottomMargin - i6));
                }
            }
            return i4 + i5;
        }
    }

    private int getChildVerticalGravity(int i) {
        int i2 = i & 16;
        return (i2 == 16 || i2 == 48 || i2 == 80) ? i2 : this.mGravity & 16;
    }

    private void addCustomViewsWithGravity(List<View> list, int i) {
        boolean z = getLayoutDirection() == 1;
        int childCount = getChildCount();
        int absoluteGravity = Gravity.getAbsoluteGravity(i, getLayoutDirection());
        list.clear();
        if (z) {
            for (int i2 = childCount - 1; i2 >= 0; i2--) {
                View childAt = getChildAt(i2);
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (layoutParams.mViewType == 0 && shouldLayout(childAt) && getChildHorizontalGravity(layoutParams.gravity) == absoluteGravity) {
                    list.add(childAt);
                }
            }
            return;
        }
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt2 = getChildAt(i3);
            LayoutParams layoutParams2 = (LayoutParams) childAt2.getLayoutParams();
            if (layoutParams2.mViewType == 0 && shouldLayout(childAt2) && getChildHorizontalGravity(layoutParams2.gravity) == absoluteGravity) {
                list.add(childAt2);
            }
        }
    }

    private int getChildHorizontalGravity(int i) {
        int layoutDirection = getLayoutDirection();
        int absoluteGravity = Gravity.getAbsoluteGravity(i, layoutDirection) & 7;
        if (absoluteGravity != 1) {
            int i2 = 3;
            if (!(absoluteGravity == 3 || absoluteGravity == 5)) {
                if (layoutDirection == 1) {
                    i2 = 5;
                }
                return i2;
            }
        }
        return absoluteGravity;
    }

    private boolean shouldLayout(View view) {
        return (view == null || view.getParent() != this || view.getVisibility() == 8) ? false : true;
    }

    private int getHorizontalMargins(View view) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        return marginLayoutParams.getMarginStart() + marginLayoutParams.getMarginEnd();
    }

    private int getVerticalMargins(View view) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        return marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        if (layoutParams instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) layoutParams);
        }
        if (layoutParams instanceof android.app.ActionBar.LayoutParams) {
            return new LayoutParams((android.app.ActionBar.LayoutParams) layoutParams);
        }
        if (layoutParams instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) layoutParams);
        }
        return new LayoutParams(layoutParams);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return super.checkLayoutParams(layoutParams) && (layoutParams instanceof LayoutParams);
    }

    /* access modifiers changed from: 0000 */
    public void removeChildrenForExpandedActionView() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (!(((LayoutParams) childAt.getLayoutParams()).mViewType == 2 || childAt == this.mMenuView)) {
                removeViewAt(childCount);
                this.mHiddenViews.add(childAt);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void addChildrenForExpandedActionView() {
        for (int size = this.mHiddenViews.size() - 1; size >= 0; size--) {
            addView((View) this.mHiddenViews.get(size));
        }
        this.mHiddenViews.clear();
    }

    private boolean isChildOrHidden(View view) {
        boolean z = false;
        if (this.mHiddenViews == null) {
            return false;
        }
        if (view.getParent() == this || this.mHiddenViews.contains(view)) {
            z = true;
        }
        return z;
    }

    public void setCollapsible(boolean z) {
        this.mCollapsible = z;
        requestLayout();
    }

    public boolean setCollapsedState(boolean z) {
        int i = 0;
        if (!this.mHasActionBarLineColor || this.mCollapsed == z) {
            return false;
        }
        this.mCollapsed = z;
        Drawable background = getBackground();
        if (background instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) background;
            Drawable drawable = layerDrawable.getDrawable(1);
            if (drawable != null && layerDrawable.getId(1) == R$id.actionbar_divider) {
                if (z) {
                    i = this.mActionBarDividerColor;
                }
                drawable.setColorFilter(i, Mode.SRC);
            }
        }
        refreshDrawableState();
        jumpDrawablesToCurrentState();
        return true;
    }

    private void ensureContentInsets() {
        if (this.mContentInsets == null) {
            this.mContentInsets = new RtlSpacingHelper();
        }
    }

    /* access modifiers changed from: 0000 */
    public Context getPopupContext() {
        return this.mPopupContext;
    }

    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        try {
            return super.dispatchGenericMotionEvent(motionEvent);
        } catch (Exception unused) {
            return false;
        }
    }
}
