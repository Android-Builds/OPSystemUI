package com.oneplus.systemui.p011qs;

import android.os.Build;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.util.LifecycleFragment;
import com.oneplus.util.OpUtils;

/* renamed from: com.oneplus.systemui.qs.OpQSFragment */
public class OpQSFragment extends LifecycleFragment {
    private static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private boolean mVisible;

    public void setExpansionHight(float f) {
        boolean z = true;
        boolean z2 = f != 0.0f;
        if (this.mVisible != z2) {
            this.mVisible = z2;
            if (OpUtils.isCustomFingerprint()) {
                KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(getContext());
                boolean isFingerprintEnrolled = instance.isFingerprintEnrolled(KeyguardUpdateMonitor.getCurrentUser());
                boolean isKeyguardVisible = instance.isKeyguardVisible();
                if (isFingerprintEnrolled) {
                    if (!this.mVisible || !isKeyguardVisible) {
                        z = false;
                    }
                    instance.setQSExpanded(z);
                } else {
                    instance.setQSExpanded(false);
                }
            }
        }
    }
}
