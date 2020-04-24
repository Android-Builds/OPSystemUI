package com.oneplus.aod.slice;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.oneplus.util.OpUtils;

public class OpSliceContainer extends LinearLayout {
    private boolean mEllipsize = true;
    private View mIcon;
    private TextView mPrimary;
    private TextView mRemark;

    public OpSliceContainer(Context context) {
        super(context);
    }

    public OpSliceContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpSliceContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public OpSliceContainer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = findViewById(R$id.slice_icon);
        this.mPrimary = (TextView) findViewById(R$id.slice_primary);
        this.mRemark = (TextView) findViewById(R$id.slice_remark);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateLayout();
        updateTextSize();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredWidth = this.mIcon.getMeasuredWidth();
        TextPaint paint = this.mPrimary.getPaint();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mIcon.getLayoutParams();
        float marginStart = ((float) marginLayoutParams.getMarginStart()) + 0.0f + ((float) measuredWidth) + ((float) marginLayoutParams.getMarginEnd());
        MarginLayoutParams marginLayoutParams2 = (MarginLayoutParams) this.mPrimary.getLayoutParams();
        float marginStart2 = marginStart + ((float) marginLayoutParams2.getMarginStart()) + paint.measureText(this.mPrimary.getText().toString()) + ((float) marginLayoutParams2.getMarginEnd());
        MarginLayoutParams marginLayoutParams3 = (MarginLayoutParams) this.mRemark.getLayoutParams();
        updateLayoutParams(((marginStart2 + ((float) marginLayoutParams3.getMarginStart())) + paint.measureText(this.mRemark.getText().toString())) + ((float) marginLayoutParams3.getMarginEnd()) > ((float) getMeasuredWidth()));
    }

    private void updateLayoutParams(boolean z) {
        if (this.mEllipsize != z) {
            this.mEllipsize = z;
            LayoutParams layoutParams = (LayoutParams) this.mPrimary.getLayoutParams();
            if (z) {
                layoutParams.width = 0;
                layoutParams.weight = 1.0f;
                return;
            }
            layoutParams.width = -2;
            layoutParams.weight = 0.0f;
        }
    }

    private void updateTextSize() {
        float f = getResources().getDisplayMetrics().scaledDensity;
        float convertSpToFixedPx = (float) OpUtils.convertSpToFixedPx(getResources().getDimension(R$dimen.aod_slice_text_size_primary), f);
        this.mPrimary.setTextSize(0, convertSpToFixedPx);
        this.mRemark.setTextSize(0, convertSpToFixedPx);
        ((TextView) findViewById(R$id.slice_secondary)).setTextSize(0, (float) OpUtils.convertSpToFixedPx(getResources().getDimension(R$dimen.aod_slice_text_size_secondary), f));
    }

    private void updateLayout() {
        ((MarginLayoutParams) findViewById(R$id.slice_primary_container).getLayoutParams()).bottomMargin = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_layout_primary_margin_bottom));
        int convertDpToFixedPx = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_icon_size));
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mIcon.getLayoutParams();
        marginLayoutParams.width = convertDpToFixedPx;
        marginLayoutParams.height = convertDpToFixedPx;
        ((MarginLayoutParams) this.mPrimary.getLayoutParams()).setMarginStart(OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_text_margin_start)));
        TextView textView = this.mPrimary;
        textView.setPaddingRelative(textView.getPaddingStart(), this.mPrimary.getPaddingTop(), OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_view_primary_padding_end)), this.mPrimary.getPaddingBottom());
        ((MarginLayoutParams) this.mRemark.getLayoutParams()).setMarginStart(OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_text_margin_start)));
        int convertDpToFixedPx2 = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_view_secondary_horizontal_margin));
        MarginLayoutParams marginLayoutParams2 = (MarginLayoutParams) ((TextView) findViewById(R$id.slice_secondary)).getLayoutParams();
        marginLayoutParams2.setMarginStart(convertDpToFixedPx2);
        marginLayoutParams2.setMarginEnd(convertDpToFixedPx2);
        int convertDpToFixedPx3 = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_view_horizontal_margin));
        MarginLayoutParams marginLayoutParams3 = (MarginLayoutParams) getLayoutParams();
        marginLayoutParams3.setMarginStart(convertDpToFixedPx3);
        marginLayoutParams3.setMarginEnd(convertDpToFixedPx3);
    }
}
