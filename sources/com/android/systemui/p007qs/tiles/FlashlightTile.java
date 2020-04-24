package com.android.systemui.p007qs.tiles;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.R$string;
import com.android.systemui.SysUIToast;
import com.android.systemui.p007qs.GlobalSetting;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener;

/* renamed from: com.android.systemui.qs.tiles.FlashlightTile */
public class FlashlightTile extends QSTileImpl<BooleanState> implements FlashlightListener {
    private final FlashlightController mFlashlightController;
    private final Icon mIcon = ResourceIcon.get(17302782);
    private final GlobalSetting mOverHeatMode;

    public int getMetricsCategory() {
        return 119;
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
    }

    public FlashlightTile(QSHost qSHost, FlashlightController flashlightController) {
        super(qSHost);
        this.mFlashlightController = flashlightController;
        this.mFlashlightController.observe(getLifecycle(), this);
        this.mOverHeatMode = new GlobalSetting(this.mContext, null, "op_overheat_temperature_type") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i) {
            }
        };
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public void handleSetListening(boolean z) {
        this.mOverHeatMode.setListening(z);
    }

    public Intent getLongClickIntent() {
        return new Intent("android.media.action.STILL_IMAGE_CAMERA");
    }

    public boolean isAvailable() {
        return this.mFlashlightController.hasFlashlight();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (!ActivityManager.isUserAMonkey() && this.mFlashlightController.isAvailable()) {
            boolean z = !((BooleanState) this.mState).value;
            if (z && this.mOverHeatMode.getValue() != 0) {
                Context context = this.mContext;
                SysUIToast.makeText(context, (CharSequence) context.getString(R$string.overheat_toast_content), 1).show();
            }
            refreshState(Boolean.valueOf(z));
            this.mFlashlightController.setFlashlight(z);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_flashlight_label);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        if (booleanState.slash == null) {
            booleanState.slash = new SlashState();
        }
        booleanState.label = this.mHost.getContext().getString(R$string.quick_settings_flashlight_label);
        int i = 1;
        if (!this.mFlashlightController.isAvailable()) {
            booleanState.icon = this.mIcon;
            booleanState.slash.isSlashed = true;
            booleanState.contentDescription = this.mContext.getString(R$string.accessibility_quick_settings_flashlight_unavailable);
            booleanState.state = 0;
            return;
        }
        if (obj instanceof Boolean) {
            boolean booleanValue = ((Boolean) obj).booleanValue();
            if (booleanValue != booleanState.value) {
                booleanState.value = booleanValue;
            } else {
                return;
            }
        } else {
            booleanState.value = this.mFlashlightController.isEnabled();
        }
        booleanState.icon = this.mIcon;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.contentDescription = this.mContext.getString(R$string.quick_settings_flashlight_label);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (booleanState.value) {
            i = 2;
        }
        booleanState.state = i;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_flashlight_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_flashlight_changed_off);
    }

    public void onFlashlightChanged(boolean z) {
        refreshState(Boolean.valueOf(z));
    }

    public void onFlashlightError() {
        refreshState(Boolean.valueOf(false));
    }

    public void onFlashlightAvailabilityChanged(boolean z) {
        refreshState();
    }
}
