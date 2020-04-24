package com.oneplus.lib.widget.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Switch;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.OPFeaturesUtils;
import com.oneplus.lib.util.VibratorSceneUtils;
import com.oneplus.lib.widget.util.C1963utils;

public class OPSwitch extends Switch {
    public static String TAG = "OPSwitch";
    private long[] mVibratePattern;
    private Vibrator mVibrator;
    private int mVibratorSceneId;

    public OPSwitch(Context context) {
        this(context, null);
    }

    public OPSwitch(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843839);
    }

    public OPSwitch(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.Oneplus_DeviceDefault_Widget_Material_CompoundButton_Switch);
    }

    public OPSwitch(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
        this.mVibratorSceneId = 1003;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPSwitch, i, i2);
        setRadius(obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPSwitch_android_radius, -1));
        obtainStyledAttributes.recycle();
        if (OPFeaturesUtils.isSupportXVibrate()) {
            this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        }
    }

    public boolean performClick() {
        if (VibratorSceneUtils.systemVibrateEnabled(getContext())) {
            this.mVibratePattern = VibratorSceneUtils.getVibratorScenePattern(getContext(), this.mVibrator, this.mVibratorSceneId);
            VibratorSceneUtils.vibrateIfNeeded(this.mVibratePattern, this.mVibrator);
        }
        return super.performClick();
    }

    public void setChecked(boolean z) {
        super.setChecked(z);
    }

    private void setRadius(int i) {
        if (i != -1) {
            Drawable background = getBackground();
            if (background == null || !(background instanceof RippleDrawable)) {
                Log.i(TAG, "setRaidus fail , background not a rippleDrawable");
            } else {
                background.mutate();
                ((RippleDrawable) background).setRadius(i);
            }
        }
    }
}
