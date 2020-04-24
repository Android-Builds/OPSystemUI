package com.oneplus.lib.widget;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.widget.util.C1963utils;

public class OPEditText extends EditText {
    private Drawable mBackground;
    private Context mContext;
    private Drawable mErrorBackground;
    ValueAnimator mHightlightColorAnimator;

    public OPEditText(Context context) {
        this(context, null);
    }

    public OPEditText(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPEditTextStyle);
    }

    public OPEditText(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.Oneplus_DeviceDefault_Widget_Material_EditText);
    }

    public OPEditText(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
        this.mContext = null;
        this.mBackground = null;
        this.mErrorBackground = null;
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attributeSet) {
        Log.i("OPListView", "OPEditText init");
        this.mContext = context;
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R$styleable.OPEditText, R$attr.OPEditTextStyle, R$style.Oneplus_DeviceDefault_Widget_Material_EditText);
        this.mBackground = obtainStyledAttributes.getDrawable(R$styleable.OPEditText_android_background);
        this.mErrorBackground = obtainStyledAttributes.getDrawable(R$styleable.OPEditText_colorError);
        obtainStyledAttributes.recycle();
        if (this.mBackground == null) {
            this.mBackground = getETBackground();
            this.mErrorBackground = getETErrBackground();
        }
        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View view) {
                OPEditText.this.startHightlightColorAnim();
                return false;
            }
        });
    }

    public void setError(CharSequence charSequence) {
        super.setError(charSequence);
        Log.i("OPListView", "OPEditText setError");
        if (charSequence != null) {
            setBackground(this.mErrorBackground);
        } else {
            setBackground(this.mBackground);
        }
    }

    private Drawable getETBackground() {
        return getResources().getDrawable(R$drawable.op_edit_text_material_light, this.mContext.getTheme());
    }

    private Drawable getETErrBackground() {
        return getResources().getDrawable(R$drawable.op_edit_text_error_material_light, this.mContext.getTheme());
    }

    /* access modifiers changed from: private */
    public void startHightlightColorAnim() {
        if (this.mHightlightColorAnimator == null) {
            int highlightColor = getHighlightColor();
            int alpha = Color.alpha(highlightColor);
            int red = Color.red(highlightColor);
            int blue = Color.blue(highlightColor);
            int green = Color.green(highlightColor);
            float f = (float) alpha;
            this.mHightlightColorAnimator = ValueAnimator.ofArgb(new int[]{Color.argb((int) (0.0f * f), red, green, blue), Color.argb((int) (f * 1.0f), red, green, blue)});
            this.mHightlightColorAnimator.setDuration(225);
            this.mHightlightColorAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    OPEditText.this.setHighlightColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                }
            });
        }
        this.mHightlightColorAnimator.start();
    }
}
