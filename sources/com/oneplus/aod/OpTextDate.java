package com.oneplus.aod;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug.ExportedProperty;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.oneplus.util.OpUtils;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.LocaleData;

@RemoteView
public class OpTextDate extends View {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private int mClockStyle;
    private float mDateFontBaseLineY;
    private Paint mDatePaint;
    private CharSequence mDescFormat;
    private CharSequence mDescFormat12;
    private CharSequence mDescFormat24;
    @ExportedProperty
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    @ExportedProperty
    private boolean mHasSeconds;
    private int mMarginTopAnalog;
    private int mMarginTopAnalogMcl;
    private int mMarginTopDefault;
    private boolean mShowCurrentUserTime;
    private float mTextSize;
    /* access modifiers changed from: private */
    public final Runnable mTicker;
    private Calendar mTime;
    private String mTimeZone;

    private static CharSequence abc(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return charSequence == null ? charSequence2 == null ? charSequence3 : charSequence2 : charSequence;
    }

    public OpTextDate(Context context) {
        super(context);
        this.mDatePaint = new Paint();
        this.mTicker = new Runnable() {
            public void run() {
                OpTextDate.this.onTimeChanged();
                long uptimeMillis = SystemClock.uptimeMillis();
                OpTextDate.this.getHandler().postAtTime(OpTextDate.this.mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        init();
    }

    public OpTextDate(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpTextDate(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    /* JADX INFO: finally extract failed */
    public OpTextDate(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDatePaint = new Paint();
        this.mTicker = new Runnable() {
            public void run() {
                OpTextDate.this.onTimeChanged();
                long uptimeMillis = SystemClock.uptimeMillis();
                OpTextDate.this.getHandler().postAtTime(OpTextDate.this.mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TextClock, i, i2);
        try {
            this.mFormat12 = obtainStyledAttributes.getText(0);
            this.mFormat24 = obtainStyledAttributes.getText(1);
            this.mTimeZone = obtainStyledAttributes.getString(2);
            obtainStyledAttributes.recycle();
            init();
        } catch (Throwable th) {
            obtainStyledAttributes.recycle();
            throw th;
        }
    }

    private void init() {
        if (this.mFormat12 == null || this.mFormat24 == null) {
            LocaleData localeData = LocaleData.get(getContext().getResources().getConfiguration().locale);
            if (this.mFormat12 == null) {
                this.mFormat12 = localeData.timeFormat_hm;
            }
            if (this.mFormat24 == null) {
                this.mFormat24 = localeData.timeFormat_Hm;
            }
        }
        reloadDimen();
        createTime(this.mTimeZone);
        chooseFormat(false);
        this.mDatePaint.setAntiAlias(true);
        this.mDatePaint.setLetterSpacing(Float.parseFloat("0.025"));
        this.mDatePaint.setColor(getResources().getColor(R$color.date_view_white));
        this.mDatePaint.setTextAlign(Align.CENTER);
    }

    private void createTime(String str) {
        if (str != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(str));
        } else {
            this.mTime = Calendar.getInstance();
        }
    }

    @RemotableViewMethod
    public void setFormat12Hour(CharSequence charSequence) {
        this.mFormat12 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    @RemotableViewMethod
    public void setFormat24Hour(CharSequence charSequence) {
        this.mFormat24 = charSequence;
        chooseFormat();
        onTimeChanged();
    }

    public void setShowCurrentUserTime(boolean z) {
        this.mShowCurrentUserTime = z;
        chooseFormat();
        onTimeChanged();
    }

    public boolean is24HourModeEnabled() {
        if (this.mShowCurrentUserTime) {
            return DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser());
        }
        return DateFormat.is24HourFormat(getContext());
    }

    @RemotableViewMethod
    public void setTimeZone(String str) {
        this.mTimeZone = str;
        createTime(str);
        onTimeChanged();
    }

    private void chooseFormat() {
        chooseFormat(true);
    }

    private void chooseFormat(boolean z) {
        boolean is24HourModeEnabled = is24HourModeEnabled();
        LocaleData localeData = LocaleData.get(getContext().getResources().getConfiguration().locale);
        if (is24HourModeEnabled) {
            this.mFormat = abc(this.mFormat24, this.mFormat12, localeData.timeFormat_Hm);
            this.mDescFormat = abc(this.mDescFormat24, this.mDescFormat12, this.mFormat);
        } else {
            this.mFormat = abc(this.mFormat12, this.mFormat24, localeData.timeFormat_hm);
            this.mDescFormat = abc(this.mDescFormat12, this.mDescFormat24, this.mFormat);
        }
        boolean z2 = this.mHasSeconds;
        this.mHasSeconds = DateFormat.hasSeconds(this.mFormat);
        if (z && z2 != this.mHasSeconds) {
            if (z2) {
                getHandler().removeCallbacks(this.mTicker);
            } else {
                this.mTicker.run();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimeZone(TimeZone.getDefault().getID());
        updateMarginTop();
    }

    /* access modifiers changed from: private */
    public void onTimeChanged() {
        setContentDescription(DateFormat.format(this.mDescFormat, this.mTime));
        invalidate();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        drawText(canvas);
        super.onDraw(canvas);
    }

    private void reloadDimen() {
        this.mDateFontBaseLineY = getResources().getDimension(R$dimen.date_view_font_base_line_y);
        this.mMarginTopDefault = getResources().getDimensionPixelSize(R$dimen.date_view_default_marginTop);
        this.mMarginTopAnalog = getResources().getDimensionPixelSize(R$dimen.date_view_analog_marginTop);
        this.mMarginTopAnalogMcl = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.date_view_analog_mcl_marginTop));
        this.mTextSize = (float) getResources().getDimensionPixelSize(R$dimen.date_view_font_size);
        this.mDatePaint.setTextSize(this.mTextSize);
        Typeface mclTypeface = OpUtils.isMCLVersion() ? OpUtils.getMclTypeface(3) : null;
        if (mclTypeface == null) {
            mclTypeface = Typeface.create("sans-serif-medium", 0);
        }
        this.mDatePaint.setTypeface(mclTypeface);
    }

    private void drawText(Canvas canvas) {
        String str;
        int width = canvas.getWidth() / 2;
        Rect rect = new Rect();
        LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        Locale locale = Locale.getDefault();
        if (locale.toString().contains("zh_")) {
            String bestDateTimePattern = DateFormat.getBestDateTimePattern(locale, "MMMMd");
            String bestDateTimePattern2 = DateFormat.getBestDateTimePattern(locale, "EEE");
            String charSequence = DateFormat.format(bestDateTimePattern, this.mTime).toString();
            String charSequence2 = DateFormat.format(bestDateTimePattern2, this.mTime).toString();
            StringBuilder sb = new StringBuilder();
            sb.append(charSequence);
            sb.append(" ");
            sb.append(charSequence2);
            str = sb.toString();
        } else {
            str = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEE, MMM d"), this.mTime).toString();
        }
        canvas.drawText(str, (float) width, this.mDateFontBaseLineY, this.mDatePaint);
        this.mDatePaint.getTextBounds(str, 0, str.length(), rect);
        layoutParams.width = (int) this.mDatePaint.measureText(str);
        FontMetrics fontMetrics = this.mDatePaint.getFontMetrics();
        layoutParams.height = (int) Math.ceil((double) (fontMetrics.bottom - fontMetrics.top));
        if (OpUtils.isMCLVersion()) {
            layoutParams.height += 4;
        }
        setLayoutParams(layoutParams);
    }

    public void setClockStyle(int i) {
        if (this.mClockStyle != i) {
            this.mClockStyle = i;
            updateMarginTop();
        }
    }

    private void updateMarginTop() {
        LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        int i = this.mClockStyle;
        int i2 = i == 0 ? this.mMarginTopDefault : i == 1 ? this.mMarginTopAnalog : i == 10 ? this.mMarginTopAnalogMcl : 0;
        layoutParams.topMargin = i2;
        if (OpUtils.isMCLVersion()) {
            layoutParams.topMargin -= 4;
        }
        setLayoutParams(layoutParams);
    }
}
