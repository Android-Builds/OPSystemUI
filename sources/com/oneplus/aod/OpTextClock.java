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
import android.view.ViewGroup.LayoutParams;
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
public class OpTextClock extends View {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private int mClockStyle;
    private Context mContext;
    private CharSequence mDescFormat;
    private CharSequence mDescFormat12;
    private CharSequence mDescFormat24;
    private int mDigitColorRed;
    private int mDigitColorWhite;
    private float mFontBaseLineY;
    @ExportedProperty
    private CharSequence mFormat;
    private CharSequence mFormat12;
    private CharSequence mFormat24;
    @ExportedProperty
    private boolean mHasSeconds;
    private Paint mHourPaint;
    private Paint mMinPaint;
    private boolean mShowCurrentUserTime;
    /* access modifiers changed from: private */
    public final Runnable mTicker;
    private Calendar mTime;
    private String mTimeZone;

    private static CharSequence abc(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return charSequence == null ? charSequence2 == null ? charSequence3 : charSequence2 : charSequence;
    }

    public OpTextClock(Context context) {
        super(context);
        this.mHourPaint = new Paint();
        this.mMinPaint = new Paint();
        this.mTicker = new Runnable() {
            public void run() {
                OpTextClock.this.onTimeChanged();
                long uptimeMillis = SystemClock.uptimeMillis();
                OpTextClock.this.getHandler().postAtTime(OpTextClock.this.mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        this.mContext = context;
        init();
    }

    public OpTextClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
        this.mContext = context;
    }

    public OpTextClock(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
        this.mContext = context;
    }

    /* JADX INFO: finally extract failed */
    public OpTextClock(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHourPaint = new Paint();
        this.mMinPaint = new Paint();
        this.mTicker = new Runnable() {
            public void run() {
                OpTextClock.this.onTimeChanged();
                long uptimeMillis = SystemClock.uptimeMillis();
                OpTextClock.this.getHandler().postAtTime(OpTextClock.this.mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        this.mContext = context;
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
        createTime(this.mTimeZone);
        chooseFormat(false);
        this.mDigitColorRed = this.mContext.getResources().getColor(R$color.clock_ten_digit_red);
        this.mDigitColorWhite = this.mContext.getResources().getColor(R$color.clock_ten_digit_white);
        this.mHourPaint.setAntiAlias(true);
        this.mMinPaint.setAntiAlias(true);
        this.mMinPaint.setColor(this.mDigitColorWhite);
        Typeface typeface = null;
        if (OpUtils.isMCLVersion()) {
            typeface = OpUtils.getMclTypeface(2);
        }
        if (typeface == null) {
            typeface = Typeface.create("sans-serif", 1);
        }
        this.mHourPaint.setTypeface(typeface);
        this.mMinPaint.setTypeface(typeface);
        updateTextSize();
    }

    private void createTime(String str) {
        if (str != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(str), Locale.ENGLISH);
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
    }

    /* access modifiers changed from: private */
    public void onTimeChanged() {
        float f;
        updateTextSize();
        FontMetrics fontMetrics = this.mHourPaint.getFontMetrics();
        if (this.mClockStyle != 0) {
            f = fontMetrics.descent + Math.abs(fontMetrics.ascent);
        } else if (OpUtils.isMCLVersion()) {
            f = Math.abs(fontMetrics.top) * 2.0f;
        } else {
            f = (Math.abs(fontMetrics.ascent) * 2.0f) + 2.0f;
        }
        LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = (int) f;
        setLayoutParams(layoutParams);
        setContentDescription(DateFormat.format(this.mDescFormat, this.mTime));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        if (this.mClockStyle == 0) {
            drawClockDefault(canvas);
        }
    }

    private void drawClockDefault(Canvas canvas) {
        Rect rect = new Rect();
        char[] cArr = new char[2];
        String charSequence = DateFormat.format(is24HourModeEnabled() ? "HH" : "hh", this.mTime).toString();
        String charSequence2 = DateFormat.format("mm", this.mTime).toString();
        float[] fArr = new float[2];
        this.mHourPaint.getTextWidths(charSequence, fArr);
        float f = fArr[0];
        int width = (int) (((float) (canvas.getWidth() / 2)) - fArr[0]);
        this.mHourPaint.setTextAlign(Align.LEFT);
        this.mHourPaint.setColor(charSequence.charAt(0) == '1' ? this.mDigitColorRed : this.mDigitColorWhite);
        cArr[0] = charSequence.charAt(0);
        this.mHourPaint.getTextBounds(String.valueOf(cArr[0]), 0, 1, rect);
        this.mFontBaseLineY = (float) this.mContext.getResources().getDimensionPixelSize(R$dimen.clock_view_default_font_base_line1_y);
        StringBuilder sb = new StringBuilder();
        sb.append(cArr[0]);
        String str = "";
        sb.append(str);
        float f2 = (float) width;
        canvas.drawText(sb.toString(), f2, this.mFontBaseLineY, this.mHourPaint);
        cArr[1] = charSequence.charAt(1);
        this.mHourPaint.setColor(charSequence.charAt(1) == '1' ? this.mDigitColorRed : this.mDigitColorWhite);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(cArr[1]);
        sb2.append(str);
        canvas.drawText(sb2.toString(), f2 + f, this.mFontBaseLineY, this.mHourPaint);
        this.mFontBaseLineY = (float) this.mContext.getResources().getDimensionPixelSize(R$dimen.clock_view_default_font_base_line2_y);
        this.mHourPaint.setTextAlign(Align.CENTER);
        this.mHourPaint.setColor(this.mDigitColorWhite);
        canvas.drawText(charSequence2, (float) (canvas.getWidth() / 2), this.mFontBaseLineY, this.mHourPaint);
    }

    private void updateTextSize() {
        this.mHourPaint.setTextSize(this.mContext.getResources().getDimension(R$dimen.clock_view_default_font_size));
        this.mMinPaint.setTextSize(this.mContext.getResources().getDimension(R$dimen.clock_view_default_font_size));
    }

    public void setClockStyle(int i) {
        this.mClockStyle = i;
    }
}
