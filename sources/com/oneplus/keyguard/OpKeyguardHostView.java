package com.oneplus.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.systemui.Dependency;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.oneplus.util.OpReflectionUtils;

public class OpKeyguardHostView extends FrameLayout {
    protected View mKeyguardSecurityNavigationSpace;
    private boolean mNavigationbarHide = false;
    private OverviewProxyService mOverviewProxyService = ((OverviewProxyService) Dependency.get(OverviewProxyService.class));

    public OpKeyguardHostView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList, int i) {
        if (getSecurityContainer() != null) {
            getSecurityContainer().showMessage(charSequence, colorStateList, i);
        }
    }

    private KeyguardSecurityContainer getSecurityContainer() {
        return (KeyguardSecurityContainer) OpReflectionUtils.getValue(KeyguardHostView.class, this, "mSecurityContainer");
    }

    /* access modifiers changed from: protected */
    public void updateNavigationSpace() {
        onHideNavBar(this.mNavigationbarHide);
    }

    public void onHideNavBar(boolean z) {
        this.mNavigationbarHide = z;
        if (this.mKeyguardSecurityNavigationSpace != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("onHideNavBar:");
            sb.append(z);
            sb.append(", ");
            sb.append(isGestureNavigationBarMode());
            String str = "OpKeyguardHostView";
            Log.i(str, sb.toString());
            if (z || isGestureNavigationBarMode()) {
                int navigationSpaceHeight = getNavigationSpaceHeight();
                this.mKeyguardSecurityNavigationSpace.setVisibility(0);
                LayoutParams layoutParams = this.mKeyguardSecurityNavigationSpace.getLayoutParams();
                layoutParams.height = navigationSpaceHeight;
                this.mKeyguardSecurityNavigationSpace.setLayoutParams(layoutParams);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("onHideNavBar setVisibility VISIBLE, height:");
                sb2.append(navigationSpaceHeight);
                Log.i(str, sb2.toString());
                return;
            }
            Log.i(str, "onHideNavBar setVisibility GONE:");
            this.mKeyguardSecurityNavigationSpace.setVisibility(8);
        }
    }

    private boolean isGestureNavigationBarMode() {
        int navBarMode = this.mOverviewProxyService.getNavBarMode();
        if (System.getInt(getContext().getContentResolver(), "op_navigation_bar_type", 1) == 3 || System.getInt(getContext().getContentResolver(), "buttons_show_on_screen_navkeys", 0) == 0 || QuickStepContract.isGesturalMode(navBarMode)) {
            return true;
        }
        return false;
    }

    private int getNavigationSpaceHeight() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(17105285);
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(17105288);
        StringBuilder sb = new StringBuilder();
        sb.append("getNavigationSpaceHeight:");
        sb.append(dimensionPixelSize);
        sb.append(", ");
        sb.append(dimensionPixelSize2);
        Log.i("OpKeyguardHostView", sb.toString());
        return this.mNavigationbarHide ? dimensionPixelSize : dimensionPixelSize - dimensionPixelSize2;
    }
}
