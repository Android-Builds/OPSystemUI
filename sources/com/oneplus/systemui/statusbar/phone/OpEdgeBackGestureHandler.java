package com.oneplus.systemui.statusbar.phone;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.phone.EdgeBackGestureHandler;

public class OpEdgeBackGestureHandler extends EdgeBackGestureHandler implements OpOnNavigationBarHiddenListener {
    public OpEdgeBackGestureHandler(Context context, OverviewProxyService overviewProxyService) {
        super(context, overviewProxyService);
    }

    public void onNavigationBarHidden() {
        Log.d("OpEdgeBackGestureHandler", "onNavigationBarHidden");
        this.mIsHidden = true;
        onNavBarAttached();
    }

    public void onNavigationBarShow() {
        Log.d("OpEdgeBackGestureHandler", "onNavigationBarShow");
        this.mIsHidden = false;
        onNavBarDetached();
    }

    public void onConfigurationChanged(int i) {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("OpEdgeBackGestureHandler", "OpEdgeBackGestureHandler onConfigurationChanged");
        }
        super.onConfigurationChanged(i);
    }
}
