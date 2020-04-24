package com.android.systemui.assist;

import android.content.Context;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService.OverviewProxyListener;
import java.io.PrintWriter;

final class AssistHandleLikeHomeBehavior implements BehaviorController {
    private AssistHandleCallbacks mAssistHandleCallbacks;
    private boolean mIsDozing;
    private boolean mIsHomeHandleHiding;
    private final OverviewProxyListener mOverviewProxyListener = new OverviewProxyListener() {
        public void onSystemUiStateChanged(int i) {
            AssistHandleLikeHomeBehavior.this.handleSystemUiStateChange(i);
        }
    };
    private final OverviewProxyService mOverviewProxyService = ((OverviewProxyService) Dependency.get(OverviewProxyService.class));
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private final StateListener mStatusBarStateListener = new StateListener() {
        public void onDozingChanged(boolean z) {
            AssistHandleLikeHomeBehavior.this.handleDozingChanged(z);
        }
    };

    private static boolean isHomeHandleHiding(int i) {
        return (i & 2) != 0;
    }

    AssistHandleLikeHomeBehavior() {
    }

    public void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks) {
        this.mAssistHandleCallbacks = assistHandleCallbacks;
        this.mIsDozing = this.mStatusBarStateController.isDozing();
        this.mStatusBarStateController.addCallback(this.mStatusBarStateListener);
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        callbackForCurrentState();
    }

    public void onModeDeactivated() {
        this.mAssistHandleCallbacks = null;
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
    }

    /* access modifiers changed from: private */
    public void handleDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            this.mIsDozing = z;
            callbackForCurrentState();
        }
    }

    /* access modifiers changed from: private */
    public void handleSystemUiStateChange(int i) {
        boolean isHomeHandleHiding = isHomeHandleHiding(i);
        if (this.mIsHomeHandleHiding != isHomeHandleHiding) {
            this.mIsHomeHandleHiding = isHomeHandleHiding;
            callbackForCurrentState();
        }
    }

    private void callbackForCurrentState() {
        AssistHandleCallbacks assistHandleCallbacks = this.mAssistHandleCallbacks;
        if (assistHandleCallbacks != null) {
            if (this.mIsHomeHandleHiding || this.mIsDozing) {
                this.mAssistHandleCallbacks.hide();
            } else {
                assistHandleCallbacks.showAndStay();
            }
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println("Current AssistHandleLikeHomeBehavior State:");
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("   mIsDozing=");
        sb.append(this.mIsDozing);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append("   mIsHomeHandleHiding=");
        sb2.append(this.mIsHomeHandleHiding);
        printWriter.println(sb2.toString());
    }
}
