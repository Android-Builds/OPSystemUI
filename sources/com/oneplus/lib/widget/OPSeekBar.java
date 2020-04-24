package com.oneplus.lib.widget;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.widget.util.C1963utils;

public class OPSeekBar extends OPAbsSeekBar {
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(OPSeekBar oPSeekBar, int i, boolean z);

        void onStartTrackingTouch(OPSeekBar oPSeekBar);

        void onStopTrackingTouch(OPSeekBar oPSeekBar);
    }

    public OPSeekBar(Context context) {
        this(context, null);
    }

    public OPSeekBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPSeekBarStyle);
    }

    public OPSeekBar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.Oneplus_DeviceDefault_Widget_Material_SeekBar);
    }

    public OPSeekBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }

    /* access modifiers changed from: 0000 */
    public void onProgressRefresh(float f, boolean z, int i) {
        super.onProgressRefresh(f, z, i);
        OnSeekBarChangeListener onSeekBarChangeListener = this.mOnSeekBarChangeListener;
        if (onSeekBarChangeListener != null) {
            onSeekBarChangeListener.onProgressChanged(this, i, z);
        }
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
        this.mOnSeekBarChangeListener = onSeekBarChangeListener;
    }

    /* access modifiers changed from: 0000 */
    public void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        OnSeekBarChangeListener onSeekBarChangeListener = this.mOnSeekBarChangeListener;
        if (onSeekBarChangeListener != null) {
            onSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    /* access modifiers changed from: 0000 */
    public void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        OnSeekBarChangeListener onSeekBarChangeListener = this.mOnSeekBarChangeListener;
        if (onSeekBarChangeListener != null) {
            onSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    public CharSequence getAccessibilityClassName() {
        return OPSeekBar.class.getName();
    }
}
