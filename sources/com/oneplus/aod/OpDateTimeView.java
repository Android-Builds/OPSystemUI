package com.oneplus.aod;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.GridLayout;
import com.android.systemui.R$bool;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.oneplus.util.OpUtils;
import java.util.Locale;

public class OpDateTimeView extends GridLayout {
    private int mClockStyle;
    private Context mContext;
    private OpTextDate mDateView;

    public static final class Patterns {
        static String clockView12;
        static String clockView24;
        static String dateView;

        static void update(Context context, boolean z, int i) {
            int i2;
            Locale locale = Locale.getDefault();
            Resources resources = context.getResources();
            if (z) {
                i2 = R$string.abbrev_wday_month_day_no_year_alarm;
            } else {
                i2 = R$string.abbrev_wday_month_day_no_year;
            }
            String string = resources.getString(i2);
            String string2 = resources.getString(R$string.clock_12hr_format);
            String string3 = resources.getString(R$string.clock_24hr_format);
            dateView = DateFormat.getBestDateTimePattern(locale, string);
            clockView12 = "hh:mm";
            if (!context.getResources().getBoolean(R$bool.aod_config_showAmpm)) {
                String str = "a";
                if (!string2.contains(str)) {
                    clockView12 = clockView12.replaceAll(str, "").trim();
                }
            }
            clockView24 = DateFormat.getBestDateTimePattern(locale, string3);
            StringBuilder sb = new StringBuilder();
            sb.append("updateClockPattern: ");
            sb.append(i);
            String str2 = "DateTimeView";
            Log.d(str2, sb.toString());
            if (i == 0) {
                clockView24 = clockView24.replace(':', ' ');
                clockView12 = clockView12.replace(':', ' ');
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("update clockView12: ");
            sb2.append(clockView12);
            sb2.append(" clockView24:");
            sb2.append(clockView24);
            Log.d(str2, sb2.toString());
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public OpDateTimeView(Context context) {
        this(context, null, 0);
    }

    public OpDateTimeView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpDateTimeView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        Log.d("DateTimeView", "onFinishInflate: ");
        this.mDateView = (OpTextDate) findViewById(R$id.date_view);
        this.mDateView.setShowCurrentUserTime(true);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    public void refreshTime() {
        Log.d("DateTimeView", "refreshTime");
        this.mDateView.setFormat24Hour(Patterns.dateView);
        this.mDateView.setFormat12Hour(Patterns.dateView);
    }

    public void refresh() {
        Patterns.update(this.mContext, false, this.mClockStyle);
        refreshTime();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        int i;
        super.onAttachedToWindow();
        Log.d("DateTimeView", "onAttachedToWindow");
        Resources resources = getResources();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) getLayoutParams();
        int i2 = this.mClockStyle;
        int i3 = i2 == 0 ? resources.getDimensionPixelSize(R$dimen.date_time_view_default_marginTop) : i2 == 1 ? resources.getDimensionPixelSize(R$dimen.date_time_view_analog_marginTop) : i2 == 2 ? resources.getDimensionPixelSize(R$dimen.date_time_view_minimalism_marginTop) : 0;
        if (OpAodUtils.getDeviceTag().equals("17819")) {
            i = resources.getDimensionPixelSize(R$dimen.date_time_view_17819_additional_marginTop);
        } else if (OpAodUtils.getDeviceTag().equals("17801")) {
            i = resources.getDimensionPixelSize(R$dimen.date_time_view_17801_additional_marginTop);
        } else {
            i = resources.getDimensionPixelSize(R$dimen.date_time_view_additional_marginTop);
        }
        if (this.mClockStyle == 10) {
            marginLayoutParams.topMargin = OpUtils.convertDpToFixedPx(resources.getDimension(R$dimen.op_aod_clock_analog_my_margin_top));
        } else {
            marginLayoutParams.topMargin = i3 + i;
        }
        setLayoutParams(marginLayoutParams);
        MarginLayoutParams marginLayoutParams2 = (MarginLayoutParams) this.mDateView.getLayoutParams();
        marginLayoutParams2.topMargin = resources.getDimensionPixelSize(R$dimen.date_view_marginTop);
        this.mDateView.setLayoutParams(marginLayoutParams2);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("DateTimeView", "onDetachedFromWindow");
    }

    public void setClockStyle(int i) {
        this.mClockStyle = i;
        refresh();
    }
}
