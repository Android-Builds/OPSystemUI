package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.oneplus.util.SystemSetting;

/* renamed from: com.android.systemui.qs.tiles.GameModeTile */
public class GameModeTile extends QSTileImpl<BooleanState> {
    private final String ESPORT_MODE_KEY = "esport_mode_enabled";
    private SystemSetting mEsportModeSetting;
    private Handler mHandler = new Handler();
    private boolean mIsListening;
    private long mLastUpdateNavBarTime = 0;
    private SystemSetting mModeSetting;

    public int getMetricsCategory() {
        return 2000;
    }

    public boolean isAvailable() {
        return false;
    }

    public GameModeTile(QSHost qSHost) {
        super(qSHost);
        C10441 r0 = new SystemSetting(this.mContext, null, "game_mode_status", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                String access$000 = GameModeTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleValueChanged: GAME_MODE_STATUS=");
                sb.append(i);
                Log.d(access$000, sb.toString());
                GameModeTile.this.refreshState();
            }
        };
        this.mModeSetting = r0;
        C10452 r6 = new SystemSetting(this.mContext, null, "esport_mode_enabled", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                String access$100 = GameModeTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleValueChanged: ESPORT_MODE_STATUS=");
                sb.append(i);
                Log.d(access$100, sb.toString());
                GameModeTile.this.refreshState();
            }
        };
        this.mEsportModeSetting = r6;
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean z = !((BooleanState) this.mState).value;
        MetricsLogger.action(this.mContext, getMetricsCategory(), z);
        setEnabled(z);
    }

    private void setEnabled(final boolean z) {
        long uptimeMillis = 700 - (SystemClock.uptimeMillis() - this.mLastUpdateNavBarTime);
        long j = 0;
        if (uptimeMillis >= 0) {
            j = uptimeMillis;
        }
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                System.putStringForUser(GameModeTile.this.mContext.getContentResolver(), "game_mode_status_manual", z ? "force-on" : "force-off", -2);
            }
        }, j);
        this.mLastUpdateNavBarTime = SystemClock.uptimeMillis();
    }

    private boolean isEnabled() {
        return this.mModeSetting.getValue() != 0;
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
        booleanState.label = this.mContext.getString(R$string.quick_settings_game_mode_label);
        if (this.mEsportModeSetting.getValue() == 1) {
            booleanState.secondaryLabel = this.mContext.getString(84869201);
        } else {
            booleanState.secondaryLabel = "";
        }
        booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_game_mode_on);
        Context context = this.mContext;
        if (booleanState.value) {
            i = R$string.quick_settings_game_mode_summary_on;
        } else {
            i = R$string.quick_settings_game_mode_summary_off;
        }
        booleanState.contentDescription = context.getString(i);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (booleanState.value) {
            i2 = 2;
        }
        booleanState.state = i2;
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.OP_GAMING_MODE_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        this.mIsListening = z;
        this.mModeSetting.setListening(z);
        this.mEsportModeSetting.setListening(z);
        if (z) {
            refreshState();
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_game_mode_label);
    }
}
