package com.oneplus.opthreekey;

import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.VolumeDialog.Callback;
import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@DependsOn(target = Callback.class)
@ProvidesInterface(action = "com.android.systemui.action.PLUGIN_OPThreekeyDialog", version = 1)
public interface OpThreekeyDialog extends Plugin {

    public interface UserActivityListener {
        void onThreekeyStateUserActivity();
    }
}
