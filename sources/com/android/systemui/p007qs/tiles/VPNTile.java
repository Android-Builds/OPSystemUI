package com.android.systemui.p007qs.tiles;

import android.content.Intent;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;

/* renamed from: com.android.systemui.qs.tiles.VPNTile */
public class VPNTile extends QSTileImpl<BooleanState> {
    private static final Intent VPN_SETTINGS = new Intent("android.net.vpn.SETTINGS");
    private final Callback mCallback = new Callback();
    private SecurityController mSecurityController = ((SecurityController) Dependency.get(SecurityController.class));

    /* renamed from: com.android.systemui.qs.tiles.VPNTile$Callback */
    private class Callback implements SecurityControllerCallback {
        private Callback() {
        }

        public void onStateChanged() {
            VPNTile.this.refreshState();
        }
    }

    public int getMetricsCategory() {
        return 2003;
    }

    public VPNTile(QSHost qSHost) {
        super(qSHost);
    }

    public void handleSetListening(boolean z) {
        if (z) {
            this.mSecurityController.addCallback(this.mCallback);
        } else {
            this.mSecurityController.removeCallback(this.mCallback);
        }
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return VPN_SETTINGS;
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.legacy_vpn_name);
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(VPN_SETTINGS, 0);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        booleanState.value = this.mSecurityController.isVpnEnabled();
        booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_vpn);
        booleanState.label = this.mContext.getString(R$string.legacy_vpn_name);
        booleanState.state = booleanState.value ? 2 : 1;
        booleanState.contentDescription = this.mContext.getString(R$string.legacy_vpn_name);
    }
}
