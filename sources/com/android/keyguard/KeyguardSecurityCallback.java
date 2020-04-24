package com.android.keyguard;

import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.oneplus.keyguard.OpEmergencyPanel;

public interface KeyguardSecurityCallback {
    void dismiss(boolean z, int i);

    SecurityMode getCurrentSecurityMode();

    OpEmergencyPanel getEmergencyPanel();

    void hideSecurityIcon();

    void onCancelClicked() {
    }

    void reportMDMEvent(String str, String str2, String str3);

    void reportUnlockAttempt(int i, boolean z, int i2);

    void reset();

    void tryToStartFaceLockFromBouncer();

    void userActivity();
}
