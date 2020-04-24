package com.oneplus.aod;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.oneplus.util.OpUtils;

public class OpAodMain extends RelativeLayout {
    private LinearLayout mBatteryAnalogContainer;
    private LinearLayout mBatteryContainer;
    private LinearLayout mBatteryDefaultContainer;
    private int mClockStyle = 0;
    private View mDateTimeView;
    private LinearLayout mNotificationIconContainer;
    private LinearLayout mSliceInfoContainer;

    public OpAodMain(Context context) {
        super(context);
    }

    public OpAodMain(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpAodMain(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public OpAodMain(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBatteryContainer = (LinearLayout) findViewById(R$id.battery_container);
        this.mBatteryDefaultContainer = (LinearLayout) findViewById(R$id.battery_default_container);
        this.mBatteryAnalogContainer = (LinearLayout) findViewById(R$id.battery_analog_container);
        this.mNotificationIconContainer = (LinearLayout) findViewById(R$id.notification_icon_area_inner);
        this.mSliceInfoContainer = (LinearLayout) findViewById(R$id.slice_info_container);
        this.mDateTimeView = findViewById(R$id.date_time_view);
        updateRTL();
        updateLayout();
        updateClockStyle();
    }

    public void onUserSwitchComplete(int i) {
        updateClockStyle();
        updateRTL();
    }

    private void updateRTL() {
        updateRTL(getResources().getConfiguration().getLayoutDirection());
    }

    /* access modifiers changed from: protected */
    public void updateRTL(int i) {
        if (i == 1) {
            this.mNotificationIconContainer.setLayoutDirection(1);
            this.mBatteryDefaultContainer.setLayoutDirection(1);
            this.mBatteryAnalogContainer.setLayoutDirection(1);
            this.mSliceInfoContainer.setLayoutDirection(1);
        } else {
            this.mNotificationIconContainer.setLayoutDirection(0);
            this.mBatteryDefaultContainer.setLayoutDirection(0);
            this.mBatteryAnalogContainer.setLayoutDirection(0);
            this.mSliceInfoContainer.setLayoutDirection(0);
        }
        invalidate();
    }

    public void onDensityOrFontScaleChanged() {
        updateLayout();
    }

    private void updateLayout() {
        int convertDpToFixedPx = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.main_view_horizontal_margin));
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mDateTimeView.getLayoutParams();
        marginLayoutParams.setMarginStart(convertDpToFixedPx);
        marginLayoutParams.setMarginEnd(convertDpToFixedPx);
        MarginLayoutParams marginLayoutParams2 = (MarginLayoutParams) this.mBatteryDefaultContainer.getLayoutParams();
        marginLayoutParams2.setMarginStart(convertDpToFixedPx);
        marginLayoutParams2.setMarginEnd(convertDpToFixedPx);
        int convertDpToFixedPx2 = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_slice_view_horizontal_margin));
        MarginLayoutParams marginLayoutParams3 = (MarginLayoutParams) this.mSliceInfoContainer.getLayoutParams();
        marginLayoutParams3.setMarginsRelative(convertDpToFixedPx2, marginLayoutParams3.topMargin, convertDpToFixedPx2, OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_main_layout_margin_bottom)));
    }

    private void updateClockStyle() {
        int i = this.mClockStyle;
        if (i == 2 || i == 3) {
            this.mNotificationIconContainer.setVisibility(8);
            this.mBatteryContainer.setVisibility(8);
            return;
        }
        this.mNotificationIconContainer.setVisibility(0);
        this.mBatteryContainer.setVisibility(0);
        if (this.mBatteryContainer.getParent() != null) {
            ((ViewGroup) this.mBatteryContainer.getParent()).removeView(this.mBatteryContainer);
        }
        LayoutParams layoutParams = (LayoutParams) this.mBatteryContainer.getLayoutParams();
        boolean z = this.mClockStyle == 10;
        int i2 = this.mClockStyle;
        if (i2 == 0 || z) {
            if (z) {
                layoutParams.topMargin = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.aod_mcl_battery_marginTop));
            } else {
                layoutParams.topMargin = getResources().getDimensionPixelSize(R$dimen.battery_empty_view_height);
            }
            layoutParams.setMarginStart(0);
            this.mBatteryDefaultContainer.addView(this.mBatteryContainer, layoutParams);
        } else if (i2 == 1) {
            layoutParams.setMarginStart(getResources().getDimensionPixelSize(R$dimen.battery_status_view_margin_left_with_date_text));
            layoutParams.topMargin = getResources().getDimensionPixelSize(R$dimen.date_view_analog_marginTop);
            this.mBatteryAnalogContainer.addView(this.mBatteryContainer, layoutParams);
        }
    }

    public void setClockStyle(int i) {
        this.mClockStyle = i;
        updateClockStyle();
    }
}
