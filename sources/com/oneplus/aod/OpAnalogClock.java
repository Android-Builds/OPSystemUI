package com.oneplus.aod;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.oneplus.util.OpUtils;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpAnalogClock extends FrameLayout {
    private static final int[][] ANALOG_RES = {new int[]{R$drawable.analog_background, R$drawable.analog_hour, R$drawable.analog_min, R$drawable.analog_dot, 0}, new int[]{R$drawable.analog_my_background, R$drawable.analog_my_hour, R$drawable.analog_my_min, R$drawable.analog_my_dot, R$drawable.analog_my_outer}};
    private View mBackground;
    private int mClockSize;
    private View mDot;
    private View mHour;
    private View mMin;
    private View mOuter;
    private int mStyle;

    public OpAnalogClock(Context context) {
        super(context);
    }

    public OpAnalogClock(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpAnalogClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHour = findViewById(R$id.analog_hour);
        this.mMin = findViewById(R$id.analog_min);
        this.mBackground = findViewById(R$id.analog_background);
        this.mOuter = findViewById(R$id.analog_outer);
        this.mDot = findViewById(R$id.analog_dot);
        loadDimensions();
        updateLayout();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshTime();
    }

    public void refreshTime() {
        String format = new SimpleDateFormat("hh:mm").format(new Date());
        String[] split = format.toString().split(":");
        int parseInt = Integer.parseInt(split[0]);
        int parseInt2 = Integer.parseInt(split[1]);
        StringBuilder sb = new StringBuilder();
        sb.append("refreshTime: ");
        sb.append(format);
        sb.append(" hour = ");
        sb.append(parseInt);
        sb.append(", min = ");
        sb.append(parseInt2);
        Log.d("OpAnalogClock", sb.toString());
        float f = (float) parseInt2;
        float f2 = ((((float) parseInt) * 360.0f) / 12.0f) + ((30.0f * f) / 60.0f);
        float f3 = (f * 360.0f) / 60.0f;
        View view = this.mHour;
        view.setRotation(view.getRotation());
        View view2 = this.mMin;
        view2.setRotation(view2.getRotation());
        View view3 = this.mOuter;
        view3.setRotation(view3.getRotation());
        this.mHour.setRotation(f2);
        this.mMin.setRotation(f3);
        this.mOuter.setRotation(f3);
    }

    public void setClockStyle(int i) {
        if (i == 1) {
            this.mStyle = 0;
        } else if (i == 10) {
            this.mStyle = 1;
        } else {
            return;
        }
        loadDimensions();
        updateLayout();
        this.mHour.setBackgroundResource(ANALOG_RES[this.mStyle][1]);
        this.mMin.setBackgroundResource(ANALOG_RES[this.mStyle][2]);
        this.mBackground.setBackgroundResource(ANALOG_RES[this.mStyle][0]);
        this.mDot.setBackgroundResource(ANALOG_RES[this.mStyle][3]);
        this.mOuter.setBackgroundResource(ANALOG_RES[this.mStyle][4]);
        refreshTime();
    }

    private void updateLayout() {
        LayoutParams layoutParams = getLayoutParams();
        int i = this.mClockSize;
        layoutParams.width = i;
        layoutParams.height = i;
    }

    private void loadDimensions() {
        if (this.mStyle == 1) {
            this.mClockSize = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.op_aod_clock_analog_my_size));
        } else {
            this.mClockSize = getResources().getDimensionPixelSize(R$dimen.clock_analog_size);
        }
    }
}
