package com.android.systemui.plugins;

import android.view.View;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(action = "com.android.systemui.action.PLUGIN_OVERLAY", version = 3)
public interface OverlayPlugin extends Plugin {
    public static final String ACTION = "com.android.systemui.action.PLUGIN_OVERLAY";
    public static final int VERSION = 3;

    public interface Callback {
        void onHoldStatusBarOpenChange();
    }

    boolean holdStatusBarOpen() {
        return false;
    }

    void setCollapseDesired(boolean z) {
    }

    void setup(View view, View view2);

    void setup(View view, View view2, Callback callback) {
        setup(view, view2);
    }
}
