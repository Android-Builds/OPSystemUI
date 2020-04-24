package com.oneplus.lib.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;

@SuppressLint({"NewApi"})
public class HintSearchView extends LinearLayout {
    private String mHintText;
    private TextView mHintView;
    private ImageView mIconSearch;
    private Drawable mOptionIconDrawable;
    private ImageView mOptionIconView;
    private Drawable mSearchIconDrawable;
    private int mSearchIconTintColor;

    public HintSearchView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.hintSearchViewStyle);
    }

    public HintSearchView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        LayoutInflater.from(context).inflate(R$layout.op_persistent_search_view, this, true);
        setOrientation(0);
        setFocusable(true);
        setClickable(true);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.HintSearchView, i, R$style.Oneplus_Widget_Desgin_HintSearchView);
        this.mSearchIconDrawable = obtainStyledAttributes.getDrawable(R$styleable.HintSearchView_android_icon);
        this.mOptionIconDrawable = obtainStyledAttributes.getDrawable(R$styleable.HintSearchView_optionIcon);
        this.mHintText = obtainStyledAttributes.getString(R$styleable.HintSearchView_android_text);
        this.mSearchIconTintColor = obtainStyledAttributes.getColor(R$styleable.HintSearchView_iconTintColor, getResources().getColor(R$color.oneplus_contorl_icon_color_active_default, getContext().getTheme()));
        int color = obtainStyledAttributes.getColor(R$styleable.HintSearchView_android_textColorHint, getResources().getColor(R$color.oneplus_contorl_text_color_hint_light, context.getTheme()));
        obtainStyledAttributes.recycle();
        this.mHintView = (TextView) findViewById(R$id.persistent_search_hint);
        this.mIconSearch = (ImageView) findViewById(R$id.persistent_search_icon1);
        this.mOptionIconView = (ImageView) findViewById(R$id.persistent_search_icon2);
        this.mHintView.setTextColor(color);
        setHintText(this.mHintText);
        Drawable drawable = this.mSearchIconDrawable;
        if (drawable != null) {
            drawable.setTint(this.mSearchIconTintColor);
        }
        setSearchIcon(this.mSearchIconDrawable);
        setOptionIcon(this.mOptionIconDrawable);
    }

    public void setHintText(CharSequence charSequence) {
        TextView textView = this.mHintView;
        if (textView != null) {
            textView.setText(charSequence);
        }
    }

    public void setSearchIcon(Drawable drawable) {
        ImageView imageView = this.mIconSearch;
        if (imageView != null && drawable != null) {
            imageView.setImageDrawable(drawable);
        }
    }

    public void setOptionIcon(Drawable drawable) {
        ImageView imageView = this.mOptionIconView;
        if (imageView != null && drawable != null) {
            imageView.setImageDrawable(drawable);
        }
    }
}
