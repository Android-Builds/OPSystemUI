package com.oneplus.lib.widget.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;

public class OPSwitchFloatingActionButton extends RelativeLayout {
    private static final int[] FOCUSED_ENABLED_STATE_SET = {16842908, 16842910};
    public static final Interpolator SWITCH_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.4f, 1.0f);
    private boolean mIsDisappear1;
    private boolean mIsSwitchState;
    private ImageView mNormalImageView;
    private ImageView mSwitchImageView;

    public OPSwitchFloatingActionButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPSwitchFloatingActionButtonStyle);
    }

    public OPSwitchFloatingActionButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsDisappear1 = false;
        this.mIsSwitchState = false;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPSwitchFloatingActionButton, i, R$style.OnePlus_Widget_Design_SwitchFloatingActionButton);
        float dimension = obtainStyledAttributes.getDimension(R$styleable.OPSwitchFloatingActionButton_op_elevation, 0.0f);
        ColorStateList colorStateList = obtainStyledAttributes.getColorStateList(R$styleable.OPSwitchFloatingActionButton_op_tint_color);
        Drawable mutate = getResources().getDrawable(R$drawable.op_switch_floating_action_button).mutate();
        mutate.setTintList(colorStateList);
        setBackground(new RippleDrawable(ColorStateList.valueOf(getResources().getColor(R$color.white)), mutate, null));
        setElevation(dimension);
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R$layout.op_float_switch_button, this);
        this.mNormalImageView = (ImageView) findViewById(R$id.normal_imageview);
        this.mNormalImageView.setImageDrawable(obtainStyledAttributes.getDrawable(R$styleable.OPSwitchFloatingActionButton_op_image));
        this.mSwitchImageView = (ImageView) findViewById(R$id.switch_imageview);
        obtainStyledAttributes.recycle();
    }
}
