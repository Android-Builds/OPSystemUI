package com.android.systemui.p007qs.tiles;

import android.content.Intent;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.NightDisplayListener;
import android.hardware.display.NightDisplayListener.Callback;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.SecureSetting;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.oneplus.util.SystemSetting;
import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;

/* renamed from: com.android.systemui.qs.tiles.NightDisplayTile */
public class NightDisplayTile extends QSTileImpl<BooleanState> implements Callback {
    private SecureSetting mDaltonizerSetting = new SecureSetting(this.mContext, null, "accessibility_display_daltonizer_enabled") {
        /* access modifiers changed from: protected */
        public void handleValueChanged(int i, boolean z) {
            NightDisplayTile.this.refreshState();
        }
    };
    private SystemSetting mGrayModeSetting;
    private SecureSetting mInversionSetting = new SecureSetting(this.mContext, null, "accessibility_display_inversion_enabled") {
        /* access modifiers changed from: protected */
        public void handleValueChanged(int i, boolean z) {
            NightDisplayTile.this.refreshState();
        }
    };
    private boolean mIsListening;
    private NightDisplayListener mListener = new NightDisplayListener(this.mContext, new Handler(Looper.myLooper()));
    private final ColorDisplayManager mManager = ((ColorDisplayManager) this.mContext.getSystemService(ColorDisplayManager.class));

    public int getMetricsCategory() {
        return 491;
    }

    public NightDisplayTile(QSHost qSHost) {
        super(qSHost);
        C10543 r3 = new SystemSetting(this.mContext, null, "accessibility_display_grayscale_enabled", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                NightDisplayTile.this.refreshState();
            }
        };
        this.mGrayModeSetting = r3;
    }

    public boolean isAvailable() {
        return ColorDisplayManager.isNightDisplayAvailable(this.mContext);
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (isColorCalibrationAvailable()) {
            if ("1".equals(Global.getString(this.mContext.getContentResolver(), "night_display_forced_auto_mode_available")) && this.mManager.getNightDisplayAutoModeRaw() == -1) {
                this.mManager.setNightDisplayAutoMode(1);
                Log.i("NightDisplayTile", "Enrolled in forced night display auto mode");
            }
            this.mManager.setNightDisplayActivated(!((BooleanState) this.mState).value);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        if (this.mIsListening) {
            this.mListener.setCallback(null);
        }
        this.mListener = new NightDisplayListener(this.mContext, i, new Handler(Looper.myLooper()));
        if (this.mIsListening) {
            this.mListener.setCallback(this);
        }
        this.mInversionSetting.setUserId(i);
        this.mDaltonizerSetting.setUserId(i);
        super.handleUserSwitch(i);
    }

    private boolean isGrayEnabled() {
        return this.mGrayModeSetting.getValue(1) == 0;
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        if (isColorCalibrationAvailable()) {
            super.handleLongClick();
        }
    }

    private boolean isColorCalibrationAvailable() {
        boolean z = this.mInversionSetting.getValue() == 1;
        boolean z2 = this.mDaltonizerSetting.getValue() == 1;
        boolean isGrayEnabled = isGrayEnabled();
        if (z || z2 || isGrayEnabled) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        booleanState.value = this.mManager.isNightDisplayActivated();
        booleanState.label = this.mContext.getString(R$string.op_quick_settings_night_display_label);
        booleanState.icon = ResourceIcon.get(17302783);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (!isColorCalibrationAvailable()) {
            booleanState.state = 0;
        } else {
            booleanState.state = booleanState.value ? 2 : 1;
        }
        booleanState.secondaryLabel = getSecondaryLabel(booleanState.value);
        booleanState.contentDescription = booleanState.label;
    }

    private String getSecondaryLabel(boolean z) {
        LocalTime localTime;
        int i;
        String str;
        int nightDisplayAutoMode = this.mManager.getNightDisplayAutoMode();
        if (nightDisplayAutoMode == 1) {
            if (z) {
                localTime = this.mManager.getNightDisplayCustomEndTime();
                i = R$string.quick_settings_secondary_label_until;
            } else {
                localTime = this.mManager.getNightDisplayCustomStartTime();
                i = R$string.quick_settings_night_secondary_label_on_at;
            }
            Calendar instance = Calendar.getInstance();
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this.mContext);
            timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            instance.setTimeZone(timeFormat.getTimeZone());
            instance.set(11, localTime.getHour());
            instance.set(12, localTime.getMinute());
            instance.set(13, 0);
            instance.set(14, 0);
            return this.mContext.getString(i, new Object[]{timeFormat.format(instance.getTime())});
        } else if (nightDisplayAutoMode != 2) {
            return null;
        } else {
            if (z) {
                str = this.mContext.getString(R$string.quick_settings_night_secondary_label_until_sunrise);
            } else {
                str = this.mContext.getString(R$string.quick_settings_night_secondary_label_on_at_sunset);
            }
            return str;
        }
    }

    public LogMaker populate(LogMaker logMaker) {
        return super.populate(logMaker).addTaggedData(1311, Integer.valueOf(this.mManager.getNightDisplayAutoModeRaw()));
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.NIGHT_DISPLAY_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        this.mIsListening = z;
        this.mInversionSetting.setListening(z);
        this.mDaltonizerSetting.setListening(z);
        this.mGrayModeSetting.setListening(z);
        if (z) {
            this.mListener.setCallback(this);
            refreshState();
            return;
        }
        this.mListener.setCallback(null);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.op_quick_settings_night_display_label);
    }

    public void onActivated(boolean z) {
        refreshState();
    }
}
