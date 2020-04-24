package com.oneplus.battery;

import android.graphics.Bitmap;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.policy.CallbackController;

public interface OpChargingAnimationController extends CallbackController<ChargingStateChangeCallback> {

    public interface ChargingStateChangeCallback {
        void onWarpCharingAnimationEnd(int i) {
        }

        void onWarpCharingAnimationStart(int i) {
        }
    }

    void animationEnd(int i);

    void animationStart(int i);

    void init(KeyguardViewMediator keyguardViewMediator);

    boolean isAnimationStarted();

    void onWallpaperChange(Bitmap bitmap);
}
