package com.oneplus.aod;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import com.android.systemui.R$dimen;
import com.oneplus.util.OpBatteryUtils;
import com.oneplus.util.OpUtils;

public class OpAodBatteryDashChargeView extends ImageView {
    public OpAodBatteryDashChargeView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpAodBatteryDashChargeView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OpAodBatteryDashChargeView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) getLayoutParams();
        marginLayoutParams.width = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_dash_icon_width));
        marginLayoutParams.height = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_dash_icon_height));
        marginLayoutParams.setMarginStart(OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_margin_start)));
        setLayoutParams(marginLayoutParams);
    }

    public void setLevel(int i) {
        setImageResource(getImageResId(i));
    }

    private int getImageResId(int i) {
        return OpBatteryUtils.getDashImageResId(i);
    }
}
