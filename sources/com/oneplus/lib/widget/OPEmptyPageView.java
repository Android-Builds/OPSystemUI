package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;

public class OPEmptyPageView extends LinearLayout implements OnClickListener {
    private TextView mBottomActionTextView;
    private ImageView mImageView;
    private TextView mMiddleActionTextView;
    private View mTempView;
    private TextView mTextView;
    private TextView mTopActionTextView;
    private OnEmptyViewActionButtonClickedListener onActionButtonClickedListener;

    public interface OnEmptyViewActionButtonClickedListener {
        void onEmptyViewActionButtonClicked(int i, int i2);
    }

    public OPEmptyPageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_emptyPageStyle);
    }

    public OPEmptyPageView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OPEmptyPageView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        LayoutInflater.from(context).inflate(R$layout.op_control_empty_view, this);
        initView();
        initTypedArray(context, attributeSet, i, i2);
    }

    private void initTypedArray(Context context, AttributeSet attributeSet, int i, int i2) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPEmptyPageView, i, i2);
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.OPEmptyPageView_emptyDrawable);
        String string = obtainStyledAttributes.getString(R$styleable.OPEmptyPageView_emptyText);
        String string2 = obtainStyledAttributes.getString(R$styleable.OPEmptyPageView_topActionText);
        boolean z = obtainStyledAttributes.getBoolean(R$styleable.OPEmptyPageView_topActionClick, true);
        String string3 = obtainStyledAttributes.getString(R$styleable.OPEmptyPageView_middleActionText);
        boolean z2 = obtainStyledAttributes.getBoolean(R$styleable.OPEmptyPageView_middleActionClick, true);
        String string4 = obtainStyledAttributes.getString(R$styleable.OPEmptyPageView_bottomActionText);
        boolean z3 = obtainStyledAttributes.getBoolean(R$styleable.OPEmptyPageView_bottomActionClick, true);
        setEmptyDrawable(drawable);
        if (obtainStyledAttributes.hasValue(R$styleable.OPEmptyPageView_topActionColor)) {
            setTopActionColor(obtainStyledAttributes.getColorStateList(R$styleable.OPEmptyPageView_topActionColor));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPEmptyPageView_middleActionColor)) {
            setMiddleActionColor(obtainStyledAttributes.getColorStateList(R$styleable.OPEmptyPageView_middleActionColor));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPEmptyPageView_bottomActionColor)) {
            setBottomActionColor(obtainStyledAttributes.getColorStateList(R$styleable.OPEmptyPageView_bottomActionColor));
        }
        setEmptyText(string);
        setTopActionText(string2);
        this.mTopActionTextView.setClickable(z);
        setMiddleActionText(string3);
        this.mMiddleActionTextView.setClickable(z2);
        setBottomActionText(string4);
        this.mBottomActionTextView.setClickable(z3);
        obtainStyledAttributes.recycle();
    }

    private void initView() {
        this.mImageView = (ImageView) findViewById(R$id.empty_image);
        this.mTempView = findViewById(R$id.empty_temp);
        this.mTextView = (TextView) findViewById(R$id.empty_content);
        this.mTopActionTextView = (TextView) findViewById(R$id.empty_top_text);
        this.mMiddleActionTextView = (TextView) findViewById(R$id.empty_middle_text);
        this.mBottomActionTextView = (TextView) findViewById(R$id.empty_bottom_text);
        this.mTopActionTextView.setOnClickListener(this);
        this.mMiddleActionTextView.setOnClickListener(this);
        this.mBottomActionTextView.setOnClickListener(this);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (!checkShowComplete()) {
            LayoutParams layoutParams = (LayoutParams) this.mTextView.getLayoutParams();
            layoutParams.topMargin = this.mImageView.getTop();
            this.mTextView.setLayoutParams(layoutParams);
            this.mTextView.requestLayout();
            this.mImageView.setVisibility(8);
        }
    }

    private boolean checkShowComplete() {
        int i;
        int i2;
        int i3;
        if (8 == this.mImageView.getVisibility()) {
            return true;
        }
        int height = this.mTopActionTextView.getVisibility() == 0 ? this.mTopActionTextView.getHeight() : -1;
        int height2 = this.mMiddleActionTextView.getVisibility() == 0 ? this.mMiddleActionTextView.getHeight() : -1;
        int height3 = this.mBottomActionTextView.getVisibility() == 0 ? this.mBottomActionTextView.getHeight() : -1;
        StringBuilder sb = new StringBuilder();
        sb.append("topTextHeight = ");
        sb.append(height);
        sb.append(" ,middleTextHeight = ");
        sb.append(height2);
        sb.append(" ,bottomTextHeight = ");
        sb.append(height3);
        Log.d("OPEmptyPageView", sb.toString());
        if (height != -1) {
            i2 = height + 0;
            i = height;
            i3 = 1;
        } else {
            i3 = 0;
            i2 = 0;
            i = 0;
        }
        if (height2 != -1) {
            i2 += height2;
            i3++;
        } else {
            height2 = i;
        }
        if (height3 != -1) {
            i2 += height3;
            i3++;
        } else {
            height3 = height2;
        }
        if (i3 == 0) {
            return true;
        }
        if (i2 == 0 || height3 * i3 != i2) {
            return false;
        }
        return true;
    }

    public void setEmptyDrawable(Drawable drawable) {
        ImageView imageView = this.mImageView;
        if (imageView != null) {
            imageView.setImageDrawable(drawable);
        }
    }

    public void setEmptyText(CharSequence charSequence) {
        TextView textView = this.mTextView;
        if (textView != null) {
            textView.setText(charSequence);
        }
    }

    public void setTopActionText(CharSequence charSequence) {
        if (this.mTopActionTextView != null) {
            if (TextUtils.isEmpty(charSequence)) {
                this.mTopActionTextView.setVisibility(8);
            } else {
                this.mTopActionTextView.setVisibility(0);
            }
            this.mTopActionTextView.setText(charSequence);
        }
    }

    public void setTopActionColor(ColorStateList colorStateList) {
        TextView textView = this.mTopActionTextView;
        if (textView != null) {
            textView.setTextColor(colorStateList);
        }
    }

    public void setMiddleActionText(CharSequence charSequence) {
        if (this.mMiddleActionTextView != null) {
            if (TextUtils.isEmpty(charSequence)) {
                this.mMiddleActionTextView.setVisibility(8);
            } else {
                this.mMiddleActionTextView.setVisibility(0);
            }
            this.mMiddleActionTextView.setText(charSequence);
        }
    }

    public void setMiddleActionColor(ColorStateList colorStateList) {
        TextView textView = this.mMiddleActionTextView;
        if (textView != null) {
            textView.setTextColor(colorStateList);
        }
    }

    public void setBottomActionText(CharSequence charSequence) {
        if (this.mBottomActionTextView != null) {
            if (TextUtils.isEmpty(charSequence)) {
                this.mBottomActionTextView.setVisibility(8);
            } else {
                this.mBottomActionTextView.setVisibility(0);
            }
            this.mBottomActionTextView.setText(charSequence);
        }
    }

    public void setBottomActionColor(ColorStateList colorStateList) {
        TextView textView = this.mBottomActionTextView;
        if (textView != null) {
            textView.setTextColor(colorStateList);
        }
    }

    public void onClick(View view) {
        if (this.onActionButtonClickedListener != null) {
            int i = -1;
            int i2 = this.mTopActionTextView == view ? 0 : this.mMiddleActionTextView == view ? 1 : this.mBottomActionTextView == view ? 2 : -1;
            Object tag = view.getTag();
            if (tag instanceof Integer) {
                i = ((Integer) tag).intValue();
            }
            this.onActionButtonClickedListener.onEmptyViewActionButtonClicked(i2, i);
        }
    }
}
