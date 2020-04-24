package com.oneplus.lib.widget.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;

public class OPRectangleFloatingActionButton extends RelativeLayout {
    private boolean mIsDisappear1;
    private boolean mIsDisappear2;
    private boolean mIsSwitchState;
    private ImageView mNormalImageView;
    private ImageView mSwitchImageView;

    public OPRectangleFloatingActionButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPRectangleFloatingActionButtonStyle);
    }

    public OPRectangleFloatingActionButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsDisappear1 = false;
        this.mIsDisappear2 = false;
        this.mIsSwitchState = false;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPRectangleFloatingActionButton, i, R$style.OnePlus_Widget_Design_RectangleFloatingActionButton);
        ColorStateList colorStateList = obtainStyledAttributes.getColorStateList(R$styleable.OPRectangleFloatingActionButton_op_tint_color);
        Drawable mutate = getResources().getDrawable(R$drawable.op_rectangle_floating_action_button).mutate();
        mutate.setTintList(colorStateList);
        setBackground(new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R$color.white)), mutate, null));
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R$layout.op_float_switch_button, this);
        this.mNormalImageView = (ImageView) findViewById(R$id.normal_imageview);
        this.mNormalImageView.setImageDrawable(obtainStyledAttributes.getDrawable(R$styleable.OPRectangleFloatingActionButton_op_image));
        this.mSwitchImageView = (ImageView) findViewById(R$id.switch_imageview);
        obtainStyledAttributes.recycle();
    }
}
