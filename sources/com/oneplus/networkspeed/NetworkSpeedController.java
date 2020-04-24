package com.oneplus.networkspeed;

import com.android.systemui.statusbar.policy.CallbackController;
import java.util.BitSet;

public interface NetworkSpeedController extends CallbackController<INetworkSpeedStateCallBack> {

    public interface INetworkSpeedStateCallBack {
        void onSpeedChange(String str) {
        }

        void onSpeedShow(boolean z) {
        }
    }

    void updateConnectivity(BitSet bitSet, BitSet bitSet2);
}
