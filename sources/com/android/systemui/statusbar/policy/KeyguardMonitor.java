package com.android.systemui.statusbar.policy;

public interface KeyguardMonitor extends CallbackController<Callback> {

    public interface Callback {
        void onKeyguardShowingChanged();
    }

    long calculateGoingToFullShadeDelay();

    long getKeyguardFadingAwayDelay();

    long getKeyguardFadingAwayDuration();

    boolean isDeviceInteractive() {
        return false;
    }

    boolean isKeyguardFadingAway();

    boolean isKeyguardGoingAway();

    boolean isLaunchTransitionFadingAway();

    boolean isOccluded();

    boolean isSecure();

    boolean isShowing();

    void notifyKeyguardDoneFading() {
    }

    void notifyKeyguardFadingAway(long j, long j2) {
    }

    void notifyKeyguardGoingAway(boolean z) {
    }

    void notifyKeyguardState(boolean z, boolean z2, boolean z3) {
    }

    void setLaunchTransitionFadingAway(boolean z) {
    }
}
