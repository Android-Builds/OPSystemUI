package com.android.systemui.p007qs.tiles;

import android.content.Intent;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.oneplus.opzenmode.OpZenModeController;
import com.oneplus.opzenmode.OpZenModeController.Callback;

/* renamed from: com.android.systemui.qs.tiles.OPDndTile */
public class OPDndTile extends QSTileImpl<BooleanState> implements Callback {
    private static final Icon TOTAL_SILENCE = ResourceIcon.get(R$drawable.ic_qs_dnd_on_total_silence);
    private static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");
    private static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    private boolean isDedEnable = false;
    private final Icon mIcon = ResourceIcon.get(R$drawable.ic_qs_dnd_on);
    private boolean mListening;
    private OpZenModeController mOPZenModeController = ((OpZenModeController) Dependency.get(OpZenModeController.class));

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public int getMetricsCategory() {
        return 118;
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
    }

    public boolean isAvailable() {
        return false;
    }

    public OPDndTile(QSHost qSHost) {
        super(qSHost);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return ZEN_PRIORITY_SETTINGS;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("handleClick:");
        sb.append(isDndEnabled());
        Log.i(str, sb.toString());
        this.isDedEnable = !isDndEnabled();
        this.mOPZenModeController.setDndEnable(this.isDedEnable);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_dnd_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        if (this.mOPZenModeController == null) {
            Log.w(this.TAG, "mOPZenModeController is empty!!");
            return;
        }
        booleanState.state = this.isDedEnable ? 2 : 1;
        booleanState.value = this.isDedEnable;
        booleanState.label = getTileLabel();
        booleanState.icon = this.mIcon;
        booleanState.contentDescription = booleanState.label;
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("handleUpdateState state:");
        sb.append(booleanState);
        Log.w(str, sb.toString());
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_dnd_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_dnd_changed_off);
    }

    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (z) {
                this.mOPZenModeController.addCallback(this);
                refreshState();
            } else {
                this.mOPZenModeController.removeCallback(this);
            }
        }
    }

    private boolean isDndEnabled() {
        return this.mOPZenModeController.getDndEnable();
    }

    public void onDndChanged(boolean z) {
        this.isDedEnable = isDndEnabled();
        refreshState();
    }

    public void onThreeKeyStatus(int i) {
        refreshState();
    }
}
