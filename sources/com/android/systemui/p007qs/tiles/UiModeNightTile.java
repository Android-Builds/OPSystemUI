package com.android.systemui.p007qs.tiles;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.System;
import android.widget.Switch;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

/* renamed from: com.android.systemui.qs.tiles.UiModeNightTile */
public class UiModeNightTile extends QSTileImpl<BooleanState> implements ConfigurationListener, BatteryStateChangeCallback {
    private final BatteryController mBatteryController;
    private final Icon mIcon = ResourceIcon.get(17302784);
    private final UiModeManager mUiModeManager;

    public int getMetricsCategory() {
        return 1706;
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
    }

    public UiModeNightTile(QSHost qSHost, ConfigurationController configurationController, BatteryController batteryController) {
        super(qSHost);
        this.mBatteryController = batteryController;
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        configurationController.observe(getLifecycle(), this);
        batteryController.observe(getLifecycle(), this);
    }

    public void onUiModeChanged() {
        refreshState();
    }

    public void onPowerSaveChanged(boolean z) {
        refreshState();
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((BooleanState) getState()).state != 0) {
            int i = 1;
            boolean z = !((BooleanState) this.mState).value;
            UiModeManager uiModeManager = this.mUiModeManager;
            if (z) {
                i = 2;
            }
            uiModeManager.setNightMode(i);
            refreshState(Boolean.valueOf(z));
        }
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        int i;
        boolean isPowerSave = this.mBatteryController.isPowerSave();
        int i2 = 1;
        booleanState.value = (this.mContext.getResources().getConfiguration().uiMode & 48) == 32;
        Context context = this.mContext;
        if (isPowerSave) {
            i = R$string.quick_settings_ui_mode_night_label_battery_saver;
        } else {
            i = R$string.quick_settings_ui_mode_night_label;
        }
        booleanState.label = context.getString(i);
        booleanState.contentDescription = booleanState.label;
        booleanState.icon = this.mIcon;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (isPowerSave) {
            booleanState.state = 0;
        } else {
            if (booleanState.value) {
                i2 = 2;
            }
            booleanState.state = i2;
        }
        booleanState.showRippleEffect = false;
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    public CharSequence getTileLabel() {
        return ((BooleanState) getState()).label;
    }

    public boolean isAvailable() {
        return System.getInt(this.mContext.getContentResolver(), "oem_night_mode_tile", 0) == 1;
    }
}
