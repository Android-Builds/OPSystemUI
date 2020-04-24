package com.oneplus.aod;

import android.content.Context;
import android.graphics.Typeface;
import android.icu.text.NumberFormat;
import android.util.AttributeSet;
import android.util.OpFeatures;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.oneplus.util.OpUtils;

public class OpAodBatteryStatusView extends LinearLayout implements BatteryStateChangeCallback {
    private static final int[] CHARGE_ICONS = {R$drawable.aod_ic_battery_charged, R$drawable.aod_ic_battery_charging, R$drawable.aod_ic_battery_fast_charging};
    private View mBattery;
    private BatteryController mBatteryController;
    private OpAodBatteryDashChargeView mBatteryDashChargeView;
    private int mChargeState;
    private ImageView mChargeView;
    private boolean mFastCharge;
    private int mLevel;
    private TextView mPercentage;

    public OpAodBatteryStatusView(Context context) {
        this(context, null, 0);
    }

    public OpAodBatteryStatusView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpAodBatteryStatusView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mChargeState = -1;
        this.mFastCharge = false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPercentage = (TextView) findViewById(R$id.percentage);
        if (OpUtils.isMCLVersion()) {
            Typeface mclTypeface = OpUtils.getMclTypeface(3);
            if (mclTypeface != null) {
                this.mPercentage.setTypeface(mclTypeface);
            }
        }
        this.mBattery = findViewById(R$id.battery);
        this.mBatteryDashChargeView = (OpAodBatteryDashChargeView) findViewById(R$id.battery_dash_charge);
        this.mChargeView = (ImageView) findViewById(R$id.battery_charge);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mChargeView.getLayoutParams();
        marginLayoutParams.width = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_charging_icon_width));
        marginLayoutParams.height = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_charging_icon_height));
        marginLayoutParams.setMarginStart(OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_margin_start)));
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBatteryController.removeCallback(this);
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mLevel = i;
        if (!z) {
            this.mChargeState = -1;
        } else if (this.mLevel >= 100) {
            this.mChargeState = 0;
        } else if (this.mFastCharge) {
            this.mChargeState = 2;
        } else {
            this.mChargeState = 1;
        }
        this.mPercentage.setText(NumberFormat.getPercentInstance().format((double) (((float) i) / 100.0f)));
        updateViewState();
    }

    public void onFastChargeChanged(int i) {
        boolean z = i > 0;
        if (this.mFastCharge != z) {
            this.mFastCharge = z;
            updateViewState();
        }
    }

    private void updateViewState() {
        if (OpFeatures.isSupport(new int[]{94})) {
            if (this.mFastCharge) {
                this.mBattery.setVisibility(8);
                this.mBatteryDashChargeView.setLevel(this.mLevel);
                this.mBatteryDashChargeView.setVisibility(0);
            } else {
                this.mBattery.setVisibility(0);
                this.mBatteryDashChargeView.setVisibility(8);
            }
            this.mChargeView.setVisibility(8);
        } else if (this.mChargeState == -1) {
            this.mBattery.setVisibility(0);
            this.mChargeView.setVisibility(8);
            this.mBatteryDashChargeView.setVisibility(8);
        } else {
            this.mChargeView.setVisibility(0);
            this.mChargeView.setImageResource(CHARGE_ICONS[this.mChargeState]);
            this.mBattery.setVisibility(8);
            this.mBatteryDashChargeView.setVisibility(8);
        }
    }
}
