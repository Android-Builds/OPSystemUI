package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import com.android.settingslib.utils.StringUtil;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.SecureSetting;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.oneplus.util.OpUtils;

/* renamed from: com.android.systemui.qs.tiles.BatterySaverTile */
public class BatterySaverTile extends QSTileImpl<BooleanState> implements BatteryStateChangeCallback {
    private final BatteryController mBatteryController;
    private boolean mCharging;
    private Icon mIcon = ResourceIcon.get(17302779);
    private int mLevel;
    private boolean mPluggedIn;
    private boolean mPowerSave;
    private final SecureSetting mSetting;

    public int getMetricsCategory() {
        return 261;
    }

    public BatterySaverTile(QSHost qSHost, BatteryController batteryController) {
        super(qSHost);
        this.mBatteryController = batteryController;
        this.mBatteryController.observe(getLifecycle(), this);
        this.mSetting = new SecureSetting(this.mContext, this.mHandler, "low_power_warning_acknowledged") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                BatterySaverTile.this.handleRefreshState(null);
            }
        };
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
        this.mSetting.setListening(false);
    }

    public void handleSetListening(boolean z) {
        this.mSetting.setListening(z);
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.BATTERY_SAVER_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((BooleanState) getState()).state != 0) {
            this.mBatteryController.setPowerSaveMode(!this.mPowerSave);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.battery_detail_switch_title);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        int i = this.mPluggedIn ? 0 : this.mPowerSave ? 2 : 1;
        booleanState.state = i;
        booleanState.icon = this.mIcon;
        booleanState.label = this.mContext.getString(R$string.battery_detail_switch_title);
        booleanState.contentDescription = booleanState.label;
        booleanState.value = this.mPowerSave;
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.showRippleEffect = this.mSetting.getValue() == 0;
        String str = null;
        if (this.mPowerSave) {
            long batteryTimeRemaining = OpUtils.getBatteryTimeRemaining(this.mContext);
            if (batteryTimeRemaining > 0) {
                Context context = this.mContext;
                str = context.getString(R$string.battery_usage_remaining_time, new Object[]{StringUtil.formatElapsedTime(context, (double) batteryTimeRemaining, false)});
            }
        }
        booleanState.secondaryLabel = str;
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        this.mLevel = i;
        this.mPluggedIn = z;
        this.mCharging = z2;
        refreshState(Integer.valueOf(i));
    }

    public void onPowerSaveChanged(boolean z) {
        this.mPowerSave = z;
        refreshState(null);
    }
}
