package com.oneplus.systemui.statusbar.phone;

import android.content.res.Resources;
import com.android.systemui.R$dimen;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpKeyguardClockPositionAlgorithm {
    private int mKeyguardClockY;

    public void opLoadDimens(Resources resources) {
        if (OpUtils.isCustomFingerprint()) {
            this.mKeyguardClockY = (resources.getDimensionPixelSize(R$dimen.op_keyguard_clock_y) - resources.getDimensionPixelSize(R$dimen.keyguard_affordance_height)) - resources.getDimensionPixelSize(R$dimen.op_keyguard_lockicon_margin_bottom);
        } else {
            this.mKeyguardClockY = resources.getDimensionPixelSize(R$dimen.op_keyguard_clock_y);
        }
    }

    public int opGetMaxClockY() {
        return this.mKeyguardClockY;
    }

    public int opGetClockY() {
        return ((Integer) OpReflectionUtils.methodInvokeVoid(KeyguardClockPositionAlgorithm.class, this, "getClockY", new Object[0])).intValue();
    }
}
