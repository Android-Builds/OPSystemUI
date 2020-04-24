package com.android.systemui.p007qs.tiles;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.GlobalSetting;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.SecureSetting;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;

/* renamed from: com.android.systemui.qs.tiles.OPDndCarModeTile */
public class OPDndCarModeTile extends QSTileImpl<BooleanState> {
    private final String CAR_MODE_STATUS;
    private final boolean DEBUG = false;
    private final String ZEN_MODE_CAR;
    private final int ZEN_MODE_TYPE_CAR;
    private GlobalSetting mCarModeDndSetting;
    private SecureSetting mCarModeSetting;
    /* access modifiers changed from: private */
    public int mCarModeStatus;
    private boolean mListening;
    private NotificationManager mNotificationManager;

    private boolean isEnabled() {
        return false;
    }

    public int getMetricsCategory() {
        return 2004;
    }

    public boolean isAvailable() {
        return false;
    }

    public OPDndCarModeTile(QSHost qSHost) {
        super(qSHost);
        String str = "oneplus_carmode_switch";
        this.CAR_MODE_STATUS = str;
        String str2 = "zen_mode_car";
        this.ZEN_MODE_CAR = str2;
        this.ZEN_MODE_TYPE_CAR = 1;
        this.mCarModeStatus = 1;
        this.mCarModeSetting = new SecureSetting(this.mContext, this.mHandler, str) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                StringBuilder sb = new StringBuilder();
                sb.append("CAR_MODE_STATUS  value:");
                sb.append(i);
                sb.append(" / observedChange:");
                sb.append(z);
                Log.i("OPDndCarModeTile", sb.toString());
                OPDndCarModeTile.this.mCarModeStatus = i;
                OPDndCarModeTile.this.changeTile();
            }
        };
        this.mCarModeDndSetting = new GlobalSetting(this.mContext, this.mHandler, str2) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i) {
                StringBuilder sb = new StringBuilder();
                sb.append("ZEN_MODE_CAR  value:");
                sb.append(i);
                Log.i("OPDndCarModeTile", sb.toString());
                OPDndCarModeTile.this.refreshState();
            }
        };
        this.mCarModeStatus = this.mCarModeSetting.getValue();
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
    }

    /* access modifiers changed from: private */
    public boolean changeTile() {
        if (this.mCarModeStatus != 0) {
            return false;
        }
        handleDestroy();
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        this.mCarModeSetting.setListening(false);
        this.mCarModeDndSetting.setListening(false);
        super.handleDestroy();
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return new Intent().setComponent(new ComponentName("com.oneplus.carmode", "com.oneplus.carmode.activity.SettingActivity"));
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean z = !((BooleanState) this.mState).value;
        StringBuilder sb = new StringBuilder();
        sb.append("user clicked dnd: ");
        sb.append(z ? 1 : 0);
        Log.d("OPDndCarModeTile", sb.toString());
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_op_car_mode_dnd_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        int i;
        booleanState.value = isEnabled();
        if (booleanState.slash == null) {
            booleanState.slash = new SlashState();
        }
        int i2 = 1;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.label = this.mContext.getString(R$string.quick_settings_op_car_mode_dnd_label);
        booleanState.icon = ResourceIcon.get(R$drawable.op_carmode_dnd_qs_icon);
        Context context = this.mContext;
        if (booleanState.value) {
            i = R$string.quick_settings_op_car_mode_dnd_summary_on;
        } else {
            i = R$string.quick_settings_op_car_mode_dnd_summary_off;
        }
        booleanState.contentDescription = context.getString(i);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (booleanState.value) {
            i2 = 2;
        }
        booleanState.state = i2;
    }

    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (this.mListening) {
                this.mCarModeSetting.setListening(true);
                this.mCarModeDndSetting.setListening(true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        this.mCarModeSetting.setUserId(i);
        super.handleUserSwitch(i);
    }
}
