package com.oneplus.systemui.statusbar.phone;

import android.app.AppOpsManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.phone.HighlightHintController;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.phone.HighlightHintView;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpPhoneStatusBarView extends PanelBar implements OnHighlightHintStateChangeListener {
    private View mHighlightHintView;
    private View mNotifications;
    private View mStatusBarContentLeft;
    private View mStatusBarLeftSide;
    private StatusIconContainer mStatusIconContainer;
    private ViewGroup mStatusbarContent;
    private View mSystemIcons;

    public void panelScrimMinFractionChanged(float f) {
    }

    public OpPhoneStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void onFinishInflate() {
        this.mSystemIcons = findViewById(R$id.statusIcons);
        this.mNotifications = findViewById(R$id.notification_icon_area);
        this.mStatusBarContentLeft = findViewById(R$id.status_bar_contents_left);
        this.mStatusbarContent = (ViewGroup) findViewById(R$id.status_bar_contents);
        this.mStatusBarLeftSide = findViewById(R$id.status_bar_left_side);
        this.mStatusIconContainer = (StatusIconContainer) findViewById(R$id.statusIcons);
        this.mStatusIconContainer.setOpTag("status_icon_container");
        if (!OpUtils.isSupportCustomStatusBar() || OpUtils.isSupportHolePunchFrontCam() || OpUtils.isCutoutHide(getContext())) {
            View view = this.mStatusBarLeftSide;
            if (view != null) {
                View findViewById = view.findViewById(R$id.clock);
                if (findViewById.getParent() != null) {
                    ((ViewGroup) findViewById.getParent()).removeView(findViewById);
                }
                View findViewById2 = this.mStatusBarLeftSide.findViewById(R$id.highlighthintview);
                if (findViewById2.getParent() != null) {
                    ((ViewGroup) findViewById2.getParent()).removeView(findViewById2);
                }
            }
        } else {
            View view2 = this.mStatusBarContentLeft;
            view2.setPaddingRelative(view2.getPaddingStart(), this.mStatusBarContentLeft.getPaddingTop(), 0, this.mStatusBarContentLeft.getPaddingBottom());
            ViewGroup viewGroup = this.mStatusbarContent;
            if (viewGroup != null) {
                View findViewById3 = viewGroup.findViewById(R$id.clock);
                if (findViewById3.getParent() != null) {
                    ((ViewGroup) findViewById3.getParent()).removeView(findViewById3);
                }
                View findViewById4 = this.mStatusbarContent.findViewById(R$id.highlighthintview);
                if (findViewById4.getParent() != null) {
                    ((ViewGroup) findViewById4.getParent()).removeView(findViewById4);
                }
            }
        }
        if (!((HighlightHintController) Dependency.get(HighlightHintController.class)).showOvalLayout()) {
            View view3 = this.mStatusBarContentLeft;
            if (view3 != null) {
                View findViewById5 = view3.findViewById(R$id.highlighthintview);
                if (findViewById5.getParent() != null) {
                    ((ViewGroup) findViewById5.getParent()).removeView(findViewById5);
                }
            }
        }
        this.mHighlightHintView = findViewById(R$id.highlighthintview);
        this.mHighlightHintView.setTag(Integer.valueOf(HighlightHintView.TAG_STATUSBAR));
        OpReflectionUtils.setValue(PhoneStatusBarView.class, this, "mAppOps", (AppOpsManager) getContext().getSystemService("appops"));
    }

    public void onHighlightHintStateChange() {
        HighlightHintController highlightHintController = (HighlightHintController) Dependency.get(HighlightHintController.class);
        boolean isHighLightHintShow = highlightHintController.isHighLightHintShow();
        int i = 8;
        if (highlightHintController.showOvalLayout()) {
            this.mSystemIcons.requestLayout();
            View view = this.mNotifications;
            if (!isHighLightHintShow) {
                i = 0;
            }
            view.setVisibility(i);
        } else if (isHighLightHintShow) {
            this.mStatusBarContentLeft.setVisibility(8);
            this.mSystemIcons.setVisibility(4);
        } else {
            this.mStatusBarContentLeft.setVisibility(0);
            this.mSystemIcons.setVisibility(0);
        }
    }

    public int getHighlightHintWidth() {
        if (((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow()) {
            View view = this.mHighlightHintView;
            if (view != null) {
                return view.getWidth();
            }
        }
        return 0;
    }
}
