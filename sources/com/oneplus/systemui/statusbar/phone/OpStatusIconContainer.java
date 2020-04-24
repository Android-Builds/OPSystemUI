package com.oneplus.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import com.android.keyguard.AlphaOptimizedLinearLayout;
import com.oneplus.util.OpUtils;

public class OpStatusIconContainer extends AlphaOptimizedLinearLayout {
    protected static final int MAX_DOTS = OpUtils.getMaxDotsForStatusIconContainer();

    /* access modifiers changed from: protected */
    public int setUnderflowWidth(int i, int i2, int i3) {
        return 0;
    }

    public OpStatusIconContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}
