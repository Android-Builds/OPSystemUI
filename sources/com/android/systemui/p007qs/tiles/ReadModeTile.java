package com.android.systemui.p007qs.tiles;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.provider.Settings.System;
import android.util.Log;
import android.util.OpFeatures;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.SecureSetting;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.SystemSetting;
import com.oneplus.util.ThemeColorUtils;

/* renamed from: com.android.systemui.qs.tiles.ReadModeTile */
public class ReadModeTile extends QSTileImpl<BooleanState> {
    private SecureSetting mDaltonizerSetting;
    private SystemSetting mDefaultSetting;
    private SystemSetting mGrayModeSetting;
    private SecureSetting mInversionSetting;
    private boolean mIsSupportColorMode = OpFeatures.isSupport(new int[]{241});
    private SystemSetting mModeSetting;

    public int getMetricsCategory() {
        return 2001;
    }

    public ReadModeTile(QSHost qSHost) {
        super(qSHost);
        C10581 r0 = new SystemSetting(this.mContext, null, "reading_mode_status", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                String access$000 = ReadModeTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleValueChanged:READ_MODE_STATUS=");
                sb.append(i);
                Log.d(access$000, sb.toString());
                ReadModeTile.this.refreshState();
            }
        };
        this.mModeSetting = r0;
        this.mInversionSetting = new SecureSetting(this.mContext, null, "accessibility_display_inversion_enabled") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ReadModeTile.this.refreshState();
            }
        };
        this.mDaltonizerSetting = new SecureSetting(this.mContext, null, "accessibility_display_daltonizer_enabled") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ReadModeTile.this.refreshState();
            }
        };
        C10614 r02 = new SystemSetting(this.mContext, null, "accessibility_display_grayscale_enabled", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ReadModeTile.this.refreshState();
            }
        };
        this.mGrayModeSetting = r02;
        C10625 r03 = new SystemSetting(this.mContext, null, "reading_mode_option_manual", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ReadModeTile.this.refreshState();
            }
        };
        this.mDefaultSetting = r03;
    }

    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("oem.read_mode.support");
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (isColorCalibrationAvailable()) {
            boolean z = !((BooleanState) this.mState).value;
            MetricsLogger.action(this.mContext, getMetricsCategory(), z);
            String str = "force-on";
            String str2 = "force-off";
            if (!this.mIsSupportColorMode) {
                if (QSTileImpl.DEBUG) {
                    Log.d(this.TAG, "legacy read mode clicked.");
                }
                if (!z) {
                    str = str2;
                }
                setEnabled(str);
            } else if (isEnabled()) {
                if (QSTileImpl.DEBUG) {
                    Log.d(this.TAG, "off by user.");
                }
                setEnabled(str2);
            } else {
                int value = this.mDefaultSetting.getValue();
                if (QSTileImpl.DEBUG) {
                    String str3 = this.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("option=");
                    sb.append(value);
                    Log.d(str3, sb.toString());
                }
                if (value == 1) {
                    setEnabled(str);
                } else if (value != 2) {
                    popDialog();
                } else {
                    setEnabled("force-on-color");
                }
            }
        }
    }

    private void setEnabled(String str) {
        System.putStringForUser(this.mContext.getContentResolver(), "reading_mode_status_manual", str, -2);
    }

    private boolean isEnabled() {
        return this.mModeSetting.getValue() != 0;
    }

    private boolean isBlackEnabled() {
        return this.mModeSetting.getValue() == 1;
    }

    private boolean isColorEnabled() {
        return this.mModeSetting.getValue() == 2;
    }

    private boolean isGrayEnabled() {
        return this.mGrayModeSetting.getValue(1) == 0;
    }

    private void popDialog() {
        int i;
        CharSequence[] charSequenceArr = {this.mContext.getString(84869270), this.mContext.getString(84869269)};
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            i = R$style.oneplus_theme_dialog_dark;
        } else {
            i = R$style.oneplus_theme_dialog_light;
        }
        AlertDialog create = new Builder(this.mContext, i).setTitle(84869271).setItems(charSequenceArr, new OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                ReadModeTile.this.lambda$popDialog$0$ReadModeTile(dialogInterface, i);
            }
        }).setNegativeButton(R$string.cancel, new OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                ReadModeTile.this.lambda$popDialog$1$ReadModeTile(dialogInterface, i);
            }
        }).create();
        create.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(create, true);
        SystemUIDialog.registerDismissListener(create);
        SystemUIDialog.setWindowOnTop(create);
        create.show();
    }

    public /* synthetic */ void lambda$popDialog$0$ReadModeTile(DialogInterface dialogInterface, int i) {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("select: ");
        sb.append(i);
        Log.d(str, sb.toString());
        String str2 = "1";
        String str3 = "sel_effect";
        if (i == 0) {
            OpMdmLogger.log(str3, "clr", str2);
            setEnabled("force-on-color");
        } else if (i == 1) {
            OpMdmLogger.log(str3, "bw", str2);
            setEnabled("force-on");
        }
    }

    public /* synthetic */ void lambda$popDialog$1$ReadModeTile(DialogInterface dialogInterface, int i) {
        Log.d(this.TAG, "user cancel");
        dialogInterface.dismiss();
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
    public void handleUserSwitch(int i) {
        this.mInversionSetting.setUserId(i);
        this.mDaltonizerSetting.setUserId(i);
        refreshState();
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        int i;
        booleanState.value = isEnabled();
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("handleUpdateState:state.value=");
        sb.append(booleanState.value);
        Log.d(str, sb.toString());
        if (booleanState.slash == null) {
            booleanState.slash = new SlashState();
        }
        int i2 = 1;
        booleanState.slash.isSlashed = !booleanState.value;
        booleanState.label = this.mContext.getString(R$string.quick_settings_read_mode_label);
        booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_read_mode_on);
        Context context = this.mContext;
        if (booleanState.value) {
            i = R$string.quick_settings_read_mode_summary_on;
        } else {
            i = R$string.quick_settings_read_mode_summary_off;
        }
        booleanState.contentDescription = context.getString(i);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        if (!isColorCalibrationAvailable()) {
            booleanState.state = 0;
        } else {
            if (booleanState.value) {
                i2 = 2;
            }
            booleanState.state = i2;
        }
        String str2 = "";
        if (!this.mIsSupportColorMode || !booleanState.value) {
            booleanState.secondaryLabel = str2;
        } else if (isColorEnabled()) {
            booleanState.secondaryLabel = this.mContext.getString(84869270);
        } else if (isBlackEnabled()) {
            booleanState.secondaryLabel = this.mContext.getString(84869269);
        } else {
            booleanState.secondaryLabel = str2;
        }
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.OP_READING_MODE_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        this.mModeSetting.setListening(z);
        this.mInversionSetting.setListening(z);
        this.mDaltonizerSetting.setListening(z);
        this.mGrayModeSetting.setListening(z);
        if (this.mIsSupportColorMode) {
            this.mDefaultSetting.setListening(z);
        }
        if (z) {
            refreshState();
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_read_mode_label);
    }
}
