package com.oneplus.aod;

import android.content.Context;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.oneplus.aod.OpDateTimeView.Patterns;
import com.oneplus.util.OpUtils;

public class OpClockViewCtrl {
    private OpAnalogClock mAnalogClockView;
    private int mClockStyle;
    private OpTextClock mClockView;
    private final Context mContext;
    private OpDateTimeView mDateTimeView;
    private OpTextDate mDateView;
    private String mDisplayText;
    private boolean mDreaming;
    private OpMinimalismClock mMiniClockView;
    private TextView mOwnerInfo;
    private int mUserId = KeyguardUpdateMonitor.getCurrentUser();

    public OpClockViewCtrl(Context context, ViewGroup viewGroup) {
        this.mContext = context;
        initViews(viewGroup);
    }

    public void initViews(ViewGroup viewGroup) {
        this.mDateTimeView = (OpDateTimeView) viewGroup.findViewById(R$id.date_time_view);
        this.mClockView = (OpTextClock) viewGroup.findViewById(R$id.clock_view);
        this.mClockView.setShowCurrentUserTime(true);
        this.mDateView = (OpTextDate) viewGroup.findViewById(R$id.date_view);
        this.mAnalogClockView = (OpAnalogClock) viewGroup.findViewById(R$id.analog_clock_view);
        this.mMiniClockView = (OpMinimalismClock) viewGroup.findViewById(R$id.minimalism_clock_view);
        this.mOwnerInfo = (TextView) viewGroup.findViewById(R$id.owner_info);
        updateClockDB();
        updateDisplayTextDB();
    }

    private void updateLayout() {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mOwnerInfo.getLayoutParams();
        int i = this.mClockStyle;
        if (i == 0) {
            marginLayoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.owner_info_default_marginTop);
        } else if (i == 1 || i == 10) {
            marginLayoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.owner_info_analog_marginTop);
        }
        this.mOwnerInfo.setLayoutParams(marginLayoutParams);
    }

    private void updateClockVisibility() {
        StringBuilder sb = new StringBuilder();
        sb.append("updateClockVisibility: mClockStyle=");
        sb.append(this.mClockStyle);
        Log.d("ClockViewCtrl", sb.toString());
        int i = this.mClockStyle;
        if (i == 0) {
            this.mMiniClockView.setVisibility(8);
            this.mAnalogClockView.setVisibility(8);
            this.mClockView.setVisibility(0);
            this.mDateView.setVisibility(0);
        } else if (i == 1 || i == 10) {
            this.mMiniClockView.setVisibility(8);
            this.mAnalogClockView.setVisibility(0);
            this.mClockView.setVisibility(8);
            this.mDateView.setVisibility(0);
        } else if (i == 2) {
            this.mMiniClockView.setVisibility(0);
            this.mAnalogClockView.setVisibility(8);
            this.mClockView.setVisibility(8);
            this.mDateView.setVisibility(8);
        } else if (i == 3) {
            this.mMiniClockView.setVisibility(8);
            this.mAnalogClockView.setVisibility(8);
            this.mClockView.setVisibility(8);
            this.mDateView.setVisibility(8);
        }
        this.mClockView.setClockStyle(this.mClockStyle);
        this.mDateView.setClockStyle(this.mClockStyle);
        this.mDateTimeView.setClockStyle(this.mClockStyle);
    }

    public int getClockStyle() {
        return this.mClockStyle;
    }

    private String getDisplayText() {
        return this.mDisplayText;
    }

    public void updateClockDB() {
        int intForUser = Secure.getIntForUser(this.mContext.getContentResolver(), "aod_clock_style", 0, this.mUserId);
        String str = "ClockViewCtrl";
        if (OpUtils.isMCLVersion() || intForUser != 10) {
            this.mClockStyle = intForUser;
            this.mAnalogClockView.setClockStyle(this.mClockStyle);
            this.mDateTimeView.setClockStyle(this.mClockStyle);
            Patterns.update(this.mContext, false, this.mClockStyle);
            updateLayout();
            updateClockVisibility();
            StringBuilder sb = new StringBuilder();
            sb.append("updateClock: style = ");
            sb.append(this.mClockStyle);
            sb.append(", user = ");
            sb.append(this.mUserId);
            Log.d(str, sb.toString());
            return;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Set clock style failed. Invalid clock style: ");
        sb2.append(intForUser);
        Log.d(str, sb2.toString());
    }

    public void updateDisplayTextDB() {
        this.mDisplayText = Secure.getStringForUser(this.mContext.getContentResolver(), "aod_display_text", this.mUserId);
        StringBuilder sb = new StringBuilder();
        sb.append("updateClock: updateDisplayTextDB = ");
        sb.append(this.mDisplayText);
        sb.append(", user = ");
        sb.append(this.mUserId);
        Log.d("ClockViewCtrl", sb.toString());
    }

    public void startDozing() {
        this.mDateTimeView.refresh();
        refreshTime();
        updateOwnerInfo();
    }

    public void onTimeChanged() {
        this.mDateTimeView.refresh();
        refreshTime();
    }

    public void onUserSwitchComplete(int i) {
        this.mDateTimeView.refresh();
        this.mUserId = i;
        updateClockDB();
        updateDisplayTextDB();
        updateOwnerInfo();
    }

    public void onUserInfoChanged(int i) {
        updateOwnerInfo();
    }

    public void onDreamingStateChanged(boolean z) {
        this.mDreaming = z;
    }

    public void onScreenTurnedOn() {
        if (this.mDreaming) {
            this.mDateTimeView.refresh();
            refreshTime();
        }
    }

    public void updateOwnerInfo() {
        Log.d("ClockViewCtrl", "updateOwnerInfo");
        if (this.mOwnerInfo != null) {
            int i = this.mClockStyle;
            if (i == 2 || i == 3) {
                this.mOwnerInfo.setVisibility(8);
                return;
            }
            String displayText = getDisplayText();
            if (!TextUtils.isEmpty(displayText)) {
                this.mOwnerInfo.setVisibility(0);
                this.mOwnerInfo.setText(displayText);
            } else {
                this.mOwnerInfo.setVisibility(8);
            }
        }
    }

    private void refreshTime() {
        int i = this.mClockStyle;
        if (i != 0) {
            if (i != 1) {
                if (i == 2) {
                    this.mMiniClockView.refreshTime();
                    return;
                } else if (i != 10) {
                    return;
                }
            }
            this.mAnalogClockView.refreshTime();
            return;
        }
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
    }
}
