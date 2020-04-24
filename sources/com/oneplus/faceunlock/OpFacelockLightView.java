package com.oneplus.faceunlock;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class OpFacelockLightView extends RelativeLayout {
    private Context mContext;

    public OpFacelockLightView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
    }

    public OpFacelockLightView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    public OpFacelockLightView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public OpFacelockLightView(Context context) {
        super(context);
        this.mContext = context;
    }
}
