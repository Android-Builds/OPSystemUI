package com.oneplus.aod;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$styleable;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.oneplus.battery.OpBatteryMeterDrawable;
import com.oneplus.util.OpUtils;

public class OpAodBatteryMeterView extends ImageView implements BatteryStateChangeCallback {
    private BatteryController mBatteryController;
    private int mBatteryStyle;
    private final OpBatteryMeterDrawable mDrawable;
    private int mFrameColor;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public OpAodBatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public OpAodBatteryMeterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpAodBatteryMeterView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBatteryStyle = 0;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.BatteryMeterView, i, 0);
        int color = obtainStyledAttributes.getColor(R$styleable.BatteryMeterView_frameColor, context.getColor(R$color.batterymeter_frame_color));
        this.mDrawable = new OpBatteryMeterDrawable(context, color);
        obtainStyledAttributes.recycle();
        this.mFrameColor = color;
        setImageDrawable(this.mDrawable);
    }

    public void onAttachedToWindow() {
        int i;
        super.onAttachedToWindow();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        updateViewState();
        int i2 = 0;
        boolean z = this.mBatteryStyle == 2;
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) getLayoutParams();
        if (z) {
            i = 0;
        } else {
            i = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_width));
        }
        marginLayoutParams.width = i;
        marginLayoutParams.height = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_height));
        if (!z) {
            i2 = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_margin_start));
        }
        marginLayoutParams.setMarginStart(i2);
        marginLayoutParams.bottomMargin = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_battery_icon_margin_bottom));
        setLayoutParams(marginLayoutParams);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBatteryController.removeCallback(this);
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mDrawable.setBatteryLevel(i);
        this.mDrawable.setCharging(z);
        updateViewState();
    }

    private void updateViewState() {
        if (this.mBatteryStyle != 2) {
            this.mDrawable.setColors(-1, this.mFrameColor, -1);
            requestLayout();
        }
    }

    public void onPowerSaveChanged(boolean z) {
        this.mDrawable.setPowerSaveEnabled(z);
        updateViewState();
    }

    public void onBatteryStyleChanged(int i) {
        if (this.mBatteryStyle != i) {
            this.mBatteryStyle = i;
            this.mDrawable.onBatteryStyleChanged(i);
            updateViewState();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int size = MeasureSpec.getSize(i);
        int size2 = MeasureSpec.getSize(i2);
        if (this.mBatteryStyle != 1 || size == size2) {
            onSizeChanged(size, size2, 0, 0);
        } else {
            size = size2;
        }
        setMeasuredDimension(size, size2);
    }
}
