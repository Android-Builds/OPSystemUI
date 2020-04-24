package com.oneplus.systemui.statusbar.phone;

import android.app.Fragment;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.CollapsedStatusBarFragment;
import com.oneplus.scene.OpSceneModeObserver;
import com.oneplus.scene.OpSceneModeObserver.Callback;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpCollapsedStatusBarFragment extends Fragment implements Callback {
    private OpSceneModeObserver mOpSceneModeObserver;
    protected int mState1;

    /* access modifiers changed from: protected */
    public void onCreateInternal() {
        this.mOpSceneModeObserver = (OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class);
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideNotificationIconsInternal() {
        return this.mOpSceneModeObserver.isInBrickMode();
    }

    /* access modifiers changed from: protected */
    public void adjustSystemIconAreaLayoutParams(LinearLayout linearLayout) {
        if (!OpUtils.isSupportCustomStatusBar() || OpUtils.isSupportHolePunchFrontCam() || OpUtils.isCutoutHide(getContext())) {
            LayoutParams layoutParams = (LayoutParams) linearLayout.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.weight = 0.0f;
            linearLayout.setLayoutParams(layoutParams);
        }
    }

    public void onResume() {
        super.onResume();
        this.mOpSceneModeObserver.addCallback(this);
    }

    public void onPause() {
        super.onPause();
        this.mOpSceneModeObserver.removeCallback(this);
    }

    public void onBrickModeChanged() {
        Class cls = Integer.TYPE;
        OpReflectionUtils.methodInvokeWithArgs(this, OpReflectionUtils.getMethodWithParams(CollapsedStatusBarFragment.class, "disable", cls, cls, cls, Boolean.TYPE), Integer.valueOf(getContext().getDisplayId()), Integer.valueOf(this.mState1), Integer.valueOf(this.mState1), Boolean.valueOf(false));
    }
}
