package android.support.p002v7.view.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.p000v4.view.ViewCompat;
import android.support.p002v7.appcompat.R$attr;
import android.support.p002v7.appcompat.R$id;
import android.support.p002v7.appcompat.R$styleable;
import android.support.p002v7.view.menu.MenuView.ItemView;
import android.support.p002v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView.SelectionBoundsAdjuster;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/* renamed from: android.support.v7.view.menu.ListMenuItemView */
public class ListMenuItemView extends LinearLayout implements ItemView, SelectionBoundsAdjuster {
    private Drawable mBackground;
    private LinearLayout mContent;
    private ImageView mGroupDivider;
    private boolean mHasListDivider;
    private ImageView mIconView;
    private boolean mPreserveIconSpacing;
    private TextView mShortcutView;
    private Drawable mSubMenuArrow;
    private ImageView mSubMenuArrowView;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private TextView mTitleView;

    public ListMenuItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.listMenuViewStyle);
    }

    public ListMenuItemView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet);
        TintTypedArray obtainStyledAttributes = TintTypedArray.obtainStyledAttributes(getContext(), attributeSet, R$styleable.MenuView, i, 0);
        this.mBackground = obtainStyledAttributes.getDrawable(R$styleable.MenuView_android_itemBackground);
        this.mTextAppearance = obtainStyledAttributes.getResourceId(R$styleable.MenuView_android_itemTextAppearance, -1);
        this.mPreserveIconSpacing = obtainStyledAttributes.getBoolean(R$styleable.MenuView_preserveIconSpacing, false);
        this.mTextAppearanceContext = context;
        this.mSubMenuArrow = obtainStyledAttributes.getDrawable(R$styleable.MenuView_subMenuArrow);
        TypedArray obtainStyledAttributes2 = context.getTheme().obtainStyledAttributes(null, new int[]{16843049}, R$attr.dropDownListViewStyle, 0);
        this.mHasListDivider = obtainStyledAttributes2.hasValue(0);
        obtainStyledAttributes.recycle();
        obtainStyledAttributes2.recycle();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        ViewCompat.setBackground(this, this.mBackground);
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
        this.mGroupDivider = (ImageView) findViewById(R$id.group_divider);
        this.mContent = (LinearLayout) findViewById(R$id.content);
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
    }

    public void adjustListItemSelectionBounds(Rect rect) {
        ImageView imageView = this.mGroupDivider;
        if (imageView != null && imageView.getVisibility() == 0) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mGroupDivider.getLayoutParams();
            rect.top += this.mGroupDivider.getHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
        }
    }
}
