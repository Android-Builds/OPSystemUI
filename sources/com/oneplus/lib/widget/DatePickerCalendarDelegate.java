package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.os.Build.VERSION;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ViewAnimator;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$string;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.OPFeaturesUtils;
import com.oneplus.lib.util.calendar.OneplusLunarCalendar;
import com.oneplus.lib.util.calendar.OnepulsCalendarUtil;
import com.oneplus.lib.widget.DatePicker.OnDateChangedListener;
import com.oneplus.lib.widget.DayPickerView.OnDaySelectedListener;
import com.oneplus.lib.widget.YearPickerView.OnYearSelectedListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

class DatePickerCalendarDelegate extends AbstractDatePickerDelegate {
    private static final int[] ATTRS_DISABLED_ALPHA = {16842803};
    private static final int[] ATTRS_TEXT_COLOR = {16842904};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private ViewAnimator mAnimator;
    private ViewGroup mContainer;
    private int mCurrentView = -1;
    private DayPickerView mDayPickerView;
    private int mFirstDayOfWeek = 0;
    private TextView mHeaderLunarMonthDay;
    private TextView mHeaderMonthDay;
    private TextView mHeaderYear;
    private final Calendar mMaxDate;
    private final Calendar mMinDate;
    private SimpleDateFormat mMonthDayFormat;
    private final OnDaySelectedListener mOnDaySelectedListener = new OnDaySelectedListener() {
        public void onDaySelected(DayPickerView dayPickerView, Calendar calendar) {
            DatePickerCalendarDelegate.this.mCurrentDate.setTimeInMillis(calendar.getTimeInMillis());
            DatePickerCalendarDelegate.this.onDateChanged(true, true);
        }
    };
    private final OnClickListener mOnHeaderClickListener = new OnClickListener() {
        public void onClick(View view) {
            DatePickerCalendarDelegate.this.tryVibrate();
            if (view.getId() == R$id.date_picker_header_year) {
                DatePickerCalendarDelegate.this.setCurrentView(1);
            } else if (view.getId() == R$id.date_picker_header_date) {
                DatePickerCalendarDelegate.this.setCurrentView(0);
            }
        }
    };
    private final OnYearSelectedListener mOnYearSelectedListener = new OnYearSelectedListener() {
    };
    private String mSelectDay;
    private String mSelectYear;
    private final Calendar mTempDate;
    private SimpleDateFormat mYearFormat;
    private YearPickerView mYearPickerView;

    public DatePickerCalendarDelegate(DatePicker datePicker, Context context, AttributeSet attributeSet, int i, int i2) {
        super(datePicker, context);
        Locale locale = this.mCurrentLocale;
        this.mCurrentDate = Calendar.getInstance(locale);
        this.mTempDate = Calendar.getInstance(locale);
        this.mMinDate = Calendar.getInstance(locale);
        this.mMaxDate = Calendar.getInstance(locale);
        this.mMinDate.set(1900, 0, 1);
        this.mMaxDate.set(2100, 11, 31);
        Resources resources = this.mDelegator.getResources();
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R$styleable.DatePicker, i, i2);
        this.mContainer = (ViewGroup) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(obtainStyledAttributes.getResourceId(R$styleable.DatePicker_internalLayout, R$layout.op_date_picker_material), this.mDelegator, false);
        this.mContainer.setSaveFromParentEnabled(false);
        this.mDelegator.addView(this.mContainer);
        ViewGroup viewGroup = (ViewGroup) this.mContainer.findViewById(R$id.date_picker_header);
        this.mHeaderYear = (TextView) viewGroup.findViewById(R$id.date_picker_header_year);
        this.mHeaderYear.setOnClickListener(this.mOnHeaderClickListener);
        this.mHeaderMonthDay = (TextView) viewGroup.findViewById(R$id.date_picker_header_date);
        this.mHeaderMonthDay.setOnClickListener(this.mOnHeaderClickListener);
        this.mHeaderLunarMonthDay = (TextView) viewGroup.findViewById(R$id.date_picker_header_lunar);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.DatePicker_android_headerMonthTextAppearance, 0);
        if (resourceId != 0) {
            TypedArray obtainStyledAttributes2 = this.mContext.obtainStyledAttributes(null, ATTRS_TEXT_COLOR, 0, resourceId);
            obtainStyledAttributes2.getColorStateList(0);
            obtainStyledAttributes2.recycle();
        }
        ColorStateList colorStateList = obtainStyledAttributes.getColorStateList(R$styleable.DatePicker_headerTextColor);
        if (colorStateList != null) {
            this.mHeaderYear.setTextColor(colorStateList);
            this.mHeaderMonthDay.setTextColor(colorStateList);
        }
        obtainStyledAttributes.recycle();
        this.mAnimator = (ViewAnimator) this.mContainer.findViewById(R$id.animator);
        this.mDayPickerView = (DayPickerView) this.mAnimator.findViewById(R$id.date_picker_day_picker);
        this.mDayPickerView.setFirstDayOfWeek(this.mFirstDayOfWeek);
        this.mDayPickerView.setMinDate(this.mMinDate.getTimeInMillis());
        this.mDayPickerView.setMaxDate(this.mMaxDate.getTimeInMillis());
        this.mDayPickerView.setDate(this.mCurrentDate.getTimeInMillis());
        this.mDayPickerView.setOnDaySelectedListener(this.mOnDaySelectedListener);
        this.mYearPickerView = (YearPickerView) this.mAnimator.findViewById(R$id.date_picker_year_picker);
        this.mYearPickerView.setRange(this.mMinDate, this.mMaxDate);
        this.mYearPickerView.setYear(this.mCurrentDate.get(1));
        this.mYearPickerView.setOnYearSelectedListener(this.mOnYearSelectedListener);
        this.mSelectDay = resources.getString(R$string.select_day);
        this.mSelectYear = resources.getString(R$string.select_year);
        onLocaleChanged(this.mCurrentLocale);
        setCurrentView(0);
    }

    /* access modifiers changed from: protected */
    public void onLocaleChanged(Locale locale) {
        if (this.mHeaderYear != null) {
            this.mMonthDayFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EMMMd"), locale);
            this.mYearFormat = new SimpleDateFormat("y", locale);
            onCurrentDateChanged(false);
        }
    }

    private void onCurrentDateChanged(boolean z) {
        if (this.mHeaderYear != null) {
            this.mHeaderYear.setText(this.mYearFormat.format(this.mCurrentDate.getTime()));
            this.mHeaderMonthDay.setText(this.mMonthDayFormat.format(this.mCurrentDate.getTime()));
            updateLunarDate();
            if (z) {
                this.mAnimator.announceForAccessibility(getFormattedCurrentDate());
            }
        }
    }

    private void updateLunarDate() {
        StringBuilder sb = new StringBuilder();
        sb.append(Locale.getDefault().getLanguage());
        sb.append("_");
        sb.append(Locale.getDefault().getCountry());
        String sb2 = sb.toString();
        if (sb2 == null || !sb2.contains("zh")) {
            this.mHeaderLunarMonthDay.setVisibility(8);
            return;
        }
        OneplusLunarCalendar solarToLunar = OnepulsCalendarUtil.solarToLunar(this.mCurrentDate);
        boolean equals = "zh_CN".equals(sb2);
        TextView textView = this.mHeaderLunarMonthDay;
        StringBuilder sb3 = new StringBuilder();
        sb3.append(equals ? "农历：" : "農曆：");
        sb3.append(solarToLunar.getYYMMDD());
        textView.setText(sb3.toString());
        this.mHeaderLunarMonthDay.setVisibility(0);
    }

    /* access modifiers changed from: private */
    public void setCurrentView(int i) {
        if (i == 0) {
            this.mDayPickerView.setDate(this.mCurrentDate.getTimeInMillis());
            if (this.mCurrentView != i) {
                this.mHeaderMonthDay.setActivated(true);
                this.mHeaderMonthDay.getPaint().setFakeBoldText(true);
                this.mHeaderYear.setActivated(false);
                this.mHeaderYear.getPaint().setFakeBoldText(false);
                this.mAnimator.setDisplayedChild(0);
                this.mCurrentView = i;
            }
            this.mAnimator.announceForAccessibility(this.mSelectDay);
        } else if (i == 1) {
            changeYearLayoutParams();
            this.mYearPickerView.setYear(this.mCurrentDate.get(1));
            if (this.mCurrentView != i) {
                this.mHeaderMonthDay.setActivated(false);
                this.mHeaderMonthDay.getPaint().setFakeBoldText(false);
                this.mHeaderYear.setActivated(true);
                this.mHeaderYear.getPaint().setFakeBoldText(true);
                this.mAnimator.setDisplayedChild(1);
                this.mCurrentView = i;
            }
            this.mAnimator.announceForAccessibility(this.mSelectYear);
        }
    }

    public void changeYearLayoutParams() {
        if (this.mCurrentView == 1) {
            this.mYearPickerView.setLayoutParams(new LayoutParams(-1, this.mContext.getResources().getConfiguration().orientation == 2 ? -2 : -1));
        }
    }

    public void updateDate(int i, int i2, int i3) {
        this.mCurrentDate.set(1, i);
        this.mCurrentDate.set(2, i2);
        this.mCurrentDate.set(5, i3);
        onDateChanged(false, true);
    }

    /* access modifiers changed from: private */
    public void onDateChanged(boolean z, boolean z2) {
        int i = this.mCurrentDate.get(1);
        if (z2 && !(this.mOnDateChangedListener == null && this.mAutoFillChangeListener == null)) {
            int i2 = this.mCurrentDate.get(2);
            int i3 = this.mCurrentDate.get(5);
            OnDateChangedListener onDateChangedListener = this.mOnDateChangedListener;
            if (onDateChangedListener != null) {
                onDateChangedListener.onDateChanged(this.mDelegator, i, i2, i3);
            }
            OnDateChangedListener onDateChangedListener2 = this.mAutoFillChangeListener;
            if (onDateChangedListener2 != null) {
                onDateChangedListener2.onDateChanged(this.mDelegator, i, i2, i3);
            }
        }
        this.mDayPickerView.setDate(this.mCurrentDate.getTimeInMillis());
        this.mYearPickerView.setYear(i);
        onCurrentDateChanged(z);
        if (z) {
            tryVibrate();
        }
    }

    public void setFirstDayOfWeek(int i) {
        this.mFirstDayOfWeek = i;
        this.mDayPickerView.setFirstDayOfWeek(i);
    }

    public void setEnabled(boolean z) {
        this.mContainer.setEnabled(z);
        this.mDayPickerView.setEnabled(z);
        this.mYearPickerView.setEnabled(z);
        this.mHeaderYear.setEnabled(z);
        this.mHeaderMonthDay.setEnabled(z);
    }

    public boolean isEnabled() {
        return this.mContainer.isEnabled();
    }

    public void onConfigurationChanged(Configuration configuration) {
        setCurrentLocale(configuration.locale);
    }

    public Parcelable onSaveInstanceState(Parcelable parcelable) {
        SavedState savedState = new SavedState(parcelable, this.mCurrentDate.get(1), this.mCurrentDate.get(2), this.mCurrentDate.get(5), this.mMinDate.getTimeInMillis(), this.mMaxDate.getTimeInMillis(), this.mCurrentView, this.mCurrentView == 0 ? this.mDayPickerView.getMostVisiblePosition() : -1, -1);
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof SavedState) {
            SavedState savedState = (SavedState) parcelable;
            this.mCurrentDate.set(savedState.getSelectedYear(), savedState.getSelectedMonth(), savedState.getSelectedDay());
            this.mMinDate.setTimeInMillis(savedState.getMinDate());
            this.mMaxDate.setTimeInMillis(savedState.getMaxDate());
            onCurrentDateChanged(false);
            int currentView = savedState.getCurrentView();
            setCurrentView(currentView);
            int listPosition = savedState.getListPosition();
            if (listPosition == -1) {
                return;
            }
            if (currentView == 0) {
                this.mDayPickerView.setPosition(listPosition);
            } else if (currentView == 1) {
                savedState.getListPositionOffset();
            }
        }
    }

    /* access modifiers changed from: private */
    public void tryVibrate() {
        if (!OPFeaturesUtils.isSupportZVibrate() || VERSION.SDK_INT <= 26) {
            this.mDelegator.performHapticFeedback(5);
            return;
        }
        try {
            Field declaredField = VibrationEffect.class.getDeclaredField("EFFECT_CLICK");
            Method declaredMethod = VibrationEffect.class.getDeclaredMethod("get", new Class[]{Integer.TYPE});
            declaredMethod.setAccessible(true);
            declaredField.setAccessible(true);
            ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate((VibrationEffect) declaredMethod.invoke(null, new Object[]{Integer.valueOf(declaredField.getInt(null))}), VIBRATION_ATTRIBUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
