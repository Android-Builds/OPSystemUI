package com.oneplus.systemui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.battery.OpBatteryDashChargeView;
import com.oneplus.battery.OpBatteryMeterDrawable;
import com.oneplus.util.OpReflectionUtils;

public class OpBatteryMeterView extends LinearLayout implements BatteryStateChangeCallback, Tunable, DarkReceiver, ConfigurationListener {
    protected OpBatteryDashChargeView mBatteryDashChargeView;
    protected int mBatteryStyle = 0;
    protected OpBatteryMeterDrawable mDrawable;
    protected boolean mFastCharge = false;
    protected float mFontScale = -1.0f;
    protected boolean mPowerSaveEnabled = false;
    private int mViewPositionType = 0;

    public void onDarkChanged(Rect rect, float f, int i) {
    }

    public void onTuningChanged(String str, String str2) {
    }

    public OpBatteryMeterView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpBatteryMeterView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void onPowerSaveChanged(boolean z) {
        this.mPowerSaveEnabled = z;
        updateViews();
    }

    /* access modifiers changed from: protected */
    public void updateColors(int i, int i2) {
        if (this.mViewPositionType == 1) {
            i = -1;
            i2 = -2130706433;
        }
        this.mBatteryDashChargeView.setIconTint(i);
        this.mDrawable.setColors(i, i2, i);
    }

    public void setViewPositionType(int i) {
        this.mViewPositionType = i;
    }

    /* access modifiers changed from: protected */
    public void updateViews() {
        this.mDrawable.setPowerSaveEnabled(this.mPowerSaveEnabled);
        updateAllBatteryColors();
    }

    public void setPowerSaveEnabled(boolean z) {
        this.mPowerSaveEnabled = z;
    }

    private void updateAllBatteryColors() {
        OpReflectionUtils.methodInvokeVoid(BatteryMeterView.class, this, "updateAllBatteryColors", new Object[0]);
    }
}
