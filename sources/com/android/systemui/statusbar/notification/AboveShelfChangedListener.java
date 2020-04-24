package com.android.systemui.statusbar.notification;

public interface AboveShelfChangedListener {
    void onAboveShelfStateChanged(boolean z);

    void onHeadsUpStateChanged(boolean z) {
    }
}
