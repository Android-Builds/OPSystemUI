package com.oneplus.lib.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.menu.MenuView.ItemView;

public class ListMenuItemView extends LinearLayout implements ItemView {
    private static int DOUBLE_LIEN_HEIGHT = R$dimen.oneplus_contorl_list_item_height_two_line1;
    private static final int ICON_SIZE = R$dimen.oneplus_contorl_icon_size_button;
    private static final int MARGIN_LEFT2 = R$dimen.oneplus_contorl_margin_left2;
    private static final int MARGIN_LEFT4 = R$dimen.oneplus_contorl_margin_left4;
    private Drawable mBackground;
    private CheckBox mCheckBox;
    private int mDoubleLineHeight;
    private boolean mForceShowIcon;
    private int mIconSize;
    private ImageView mIconView;
    private LayoutInflater mInflater;
    private MenuItemImpl mItemData;
    private int mMarginLeft2;
    private int mMarginLeft4;
    private int mMenuType;
    private boolean mPreserveIconSpacing;
    private RadioButton mRadioButton;
    private TextView mShortcutView;
    private Drawable mSubMenuArrow;
    private ImageView mSubMenuArrowView;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private RelativeLayout mTitleLayout;
    private TextView mTitleView;

    public boolean prefersCondensedTitle() {
        return false;
    }

    public ListMenuItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16844018);
    }

    public ListMenuItemView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R$styleable.MenuView, i, 0);
        this.mMarginLeft2 = context.getResources().getDimensionPixelSize(MARGIN_LEFT2);
        this.mMarginLeft4 = context.getResources().getDimensionPixelSize(MARGIN_LEFT4);
        this.mIconSize = context.getResources().getDimensionPixelSize(ICON_SIZE);
        this.mBackground = obtainStyledAttributes.getDrawable(R$styleable.MenuView_android_itemBackground);
        this.mTextAppearance = obtainStyledAttributes.getResourceId(R$styleable.MenuView_android_itemTextAppearance, -1);
        this.mPreserveIconSpacing = obtainStyledAttributes.getBoolean(R$styleable.MenuView_preserveIconSpacing, false);
        this.mTextAppearanceContext = context;
        this.mSubMenuArrow = obtainStyledAttributes.getDrawable(R$styleable.MenuView_android_subMenuArrow);
        this.mDoubleLineHeight = context.getResources().getDimensionPixelSize(DOUBLE_LIEN_HEIGHT);
        obtainStyledAttributes.recycle();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setBackground(this.mBackground);
        this.mTitleView = (TextView) findViewById(R$id.title);
        int i = this.mTextAppearance;
        if (i != -1) {
            this.mTitleView.setTextAppearance(this.mTextAppearanceContext, i);
        }
        this.mShortcutView = (TextView) findViewById(R$id.shortcut);
        this.mSubMenuArrowView = (ImageView) findViewById(R$id.submenuarrow);
        ImageView imageView = this.mSubMenuArrowView;
        if (imageView != null) {
            imageView.setImageDrawable(this.mSubMenuArrow);
        }
        this.mTitleLayout = (RelativeLayout) findViewById(R$id.title_layout);
    }

    public void initialize(MenuItemImpl menuItemImpl, int i) {
        this.mItemData = menuItemImpl;
        this.mMenuType = i;
        setVisibility(menuItemImpl.isVisible() ? 0 : 8);
        setTitle(menuItemImpl.getTitleForItemView(this));
        setCheckable(menuItemImpl.isCheckable());
        setShortcut(menuItemImpl.shouldShowShortcut(), menuItemImpl.getShortcut());
        setIcon(menuItemImpl.getIcon());
        setEnabled(menuItemImpl.isEnabled());
        setSubMenuArrowVisible(menuItemImpl.hasSubMenu());
        setContentDescription(menuItemImpl.getContentDescription());
    }

    public void setForceShowIcon(boolean z) {
        this.mForceShowIcon = z;
        this.mPreserveIconSpacing = z;
    }

    public void setTitle(CharSequence charSequence) {
        if (charSequence != null) {
            this.mTitleView.setText(charSequence);
            if (this.mTitleView.getVisibility() != 0) {
                this.mTitleView.setVisibility(0);
            }
        } else if (this.mTitleView.getVisibility() != 8) {
            this.mTitleView.setVisibility(8);
        }
    }

    public MenuItemImpl getItemData() {
        return this.mItemData;
    }

    public void setCheckable(boolean z) {
        CompoundButton compoundButton;
        CompoundButton compoundButton2;
        if (z || this.mRadioButton != null || this.mCheckBox != null) {
            if (this.mItemData.isExclusiveCheckable()) {
                if (this.mRadioButton == null) {
                    insertRadioButton();
                }
                compoundButton2 = this.mRadioButton;
                compoundButton = this.mCheckBox;
            } else {
                if (this.mCheckBox == null) {
                    insertCheckBox();
                }
                compoundButton2 = this.mCheckBox;
                compoundButton = this.mRadioButton;
            }
            if (z) {
                compoundButton2.setChecked(this.mItemData.isChecked());
                int i = z ? 0 : 8;
                if (compoundButton2.getVisibility() != i) {
                    compoundButton2.setVisibility(i);
                }
                if (!(compoundButton == null || compoundButton.getVisibility() == 8)) {
                    compoundButton.setVisibility(8);
                }
            } else {
                CheckBox checkBox = this.mCheckBox;
                if (checkBox != null) {
                    checkBox.setVisibility(8);
                }
                RadioButton radioButton = this.mRadioButton;
                if (radioButton != null) {
                    radioButton.setVisibility(8);
                }
            }
        }
    }

    private void setSubMenuArrowVisible(boolean z) {
        ImageView imageView = this.mSubMenuArrowView;
        if (imageView != null) {
            imageView.setVisibility(z ? 0 : 8);
        }
    }

    public void setShortcut(boolean z, char c) {
        int i = (!z || !this.mItemData.shouldShowShortcut()) ? 8 : 0;
        if (i == 0) {
            this.mShortcutView.setText(this.mItemData.getShortcutLabel());
        }
        if (this.mShortcutView.getVisibility() != i) {
            this.mShortcutView.setVisibility(i);
        }
    }

    public void setIcon(Drawable drawable) {
        boolean z = this.mItemData.shouldShowIcon() || this.mForceShowIcon;
        if (!z && !this.mPreserveIconSpacing) {
            return;
        }
        if (this.mIconView != null || drawable != null || this.mPreserveIconSpacing) {
            if (this.mIconView == null) {
                insertIconView();
            }
            if (drawable != null || this.mPreserveIconSpacing) {
                ImageView imageView = this.mIconView;
                if (!z) {
                    drawable = null;
                }
                imageView.setImageDrawable(drawable);
                if (this.mIconView.getVisibility() != 0) {
                    this.mIconView.setVisibility(0);
                }
            } else {
                this.mIconView.setVisibility(8);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        if (this.mIconView != null && this.mPreserveIconSpacing) {
            LayoutParams layoutParams = getLayoutParams();
            LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) this.mIconView.getLayoutParams();
            if (layoutParams.height > 0 && layoutParams2.width <= 0) {
                layoutParams2.width = layoutParams.height;
            }
        }
        super.onMeasure(i, i2);
        TextView textView = this.mTitleView;
        if (textView != null && textView.getLineCount() >= 2) {
            setMeasuredDimension(getMeasuredWidth(), this.mDoubleLineHeight);
        }
    }

    private void insertIconView() {
        this.mIconView = (ImageView) getInflater().inflate(R$layout.op_abc_list_menu_item_icon, this, false);
        addView(this.mIconView, 0);
        RelativeLayout relativeLayout = this.mTitleLayout;
        if (relativeLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) relativeLayout.getLayoutParams();
            layoutParams.leftMargin = (this.mMarginLeft4 - this.mMarginLeft2) - this.mIconSize;
            this.mTitleLayout.setLayoutParams(layoutParams);
        }
    }

    private void insertRadioButton() {
        this.mRadioButton = (RadioButton) getInflater().inflate(R$layout.op_abc_list_menu_item_radio, this, false);
        addView(this.mRadioButton);
    }

    private void insertCheckBox() {
        this.mCheckBox = (CheckBox) getInflater().inflate(R$layout.op_abc_list_menu_item_checkbox, this, false);
        addView(this.mCheckBox);
    }

    private LayoutInflater getInflater() {
        if (this.mInflater == null) {
            this.mInflater = LayoutInflater.from(getContext());
        }
        return this.mInflater;
    }
}
