package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View.BaseSavedState;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$integer;
import com.oneplus.commonctrl.R$styleable;
import java.util.Calendar;
import java.util.Locale;

public class DatePicker extends FrameLayout {
    private static final String LOG_TAG = "DatePicker";
    private final DatePickerDelegate mDelegate;
    private final int mMode;

    static abstract class AbstractDatePickerDelegate implements DatePickerDelegate {
        protected OnDateChangedListener mAutoFillChangeListener;
        protected Context mContext;
        protected Calendar mCurrentDate;
        protected Locale mCurrentLocale;
        protected DatePicker mDelegator;
        protected OnDateChangedListener mOnDateChangedListener;

        static class SavedState extends BaseSavedState {
            public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
                public SavedState createFromParcel(Parcel parcel) {
                    return new SavedState(parcel);
                }

                public SavedState[] newArray(int i) {
                    return new SavedState[i];
                }
            };
            private final int mCurrentView;
            private final int mListPosition;
            private final int mListPositionOffset;
            private final long mMaxDate;
            private final long mMinDate;
            private final int mSelectedDay;
            private final int mSelectedMonth;
            private final int mSelectedYear;

            public SavedState(Parcelable parcelable, int i, int i2, int i3, long j, long j2) {
                this(parcelable, i, i2, i3, j, j2, 0, 0, 0);
            }

            public SavedState(Parcelable parcelable, int i, int i2, int i3, long j, long j2, int i4, int i5, int i6) {
                super(parcelable);
                this.mSelectedYear = i;
                this.mSelectedMonth = i2;
                this.mSelectedDay = i3;
                this.mMinDate = j;
                this.mMaxDate = j2;
                this.mCurrentView = i4;
                this.mListPosition = i5;
                this.mListPositionOffset = i6;
            }

            private SavedState(Parcel parcel) {
                super(parcel);
                this.mSelectedYear = parcel.readInt();
                this.mSelectedMonth = parcel.readInt();
                this.mSelectedDay = parcel.readInt();
                this.mMinDate = parcel.readLong();
                this.mMaxDate = parcel.readLong();
                this.mCurrentView = parcel.readInt();
                this.mListPosition = parcel.readInt();
                this.mListPositionOffset = parcel.readInt();
            }

            public void writeToParcel(Parcel parcel, int i) {
                super.writeToParcel(parcel, i);
                parcel.writeInt(this.mSelectedYear);
                parcel.writeInt(this.mSelectedMonth);
                parcel.writeInt(this.mSelectedDay);
                parcel.writeLong(this.mMinDate);
                parcel.writeLong(this.mMaxDate);
                parcel.writeInt(this.mCurrentView);
                parcel.writeInt(this.mListPosition);
                parcel.writeInt(this.mListPositionOffset);
            }

            public int getSelectedDay() {
                return this.mSelectedDay;
            }

            public int getSelectedMonth() {
                return this.mSelectedMonth;
            }

            public int getSelectedYear() {
                return this.mSelectedYear;
            }

            public long getMinDate() {
                return this.mMinDate;
            }

            public long getMaxDate() {
                return this.mMaxDate;
            }

            public int getCurrentView() {
                return this.mCurrentView;
            }

            public int getListPosition() {
                return this.mListPosition;
            }

            public int getListPositionOffset() {
                return this.mListPositionOffset;
            }
        }

        /* access modifiers changed from: protected */
        public void onLocaleChanged(Locale locale) {
        }

        public AbstractDatePickerDelegate(DatePicker datePicker, Context context) {
            this.mDelegator = datePicker;
            this.mContext = context;
            setCurrentLocale(Locale.getDefault());
        }

        /* access modifiers changed from: protected */
        public void setCurrentLocale(Locale locale) {
            if (!locale.equals(this.mCurrentLocale)) {
                this.mCurrentLocale = locale;
                onLocaleChanged(locale);
            }
        }

        public void setAutoFillChangeListener(OnDateChangedListener onDateChangedListener) {
            this.mAutoFillChangeListener = onDateChangedListener;
        }

        public void updateDate(long j) {
            Calendar instance = Calendar.getInstance(this.mCurrentLocale);
            instance.setTimeInMillis(j);
            updateDate(instance.get(1), instance.get(2), instance.get(5));
        }

        public long getDate() {
            return this.mCurrentDate.getTimeInMillis();
        }

        /* access modifiers changed from: protected */
        public String getFormattedCurrentDate() {
            return DateUtils.formatDateTime(this.mContext, this.mCurrentDate.getTimeInMillis(), 22);
        }
    }

    interface DatePickerDelegate {
        long getDate();

        boolean isEnabled();

        void onConfigurationChanged(Configuration configuration);

        void onRestoreInstanceState(Parcelable parcelable);

        Parcelable onSaveInstanceState(Parcelable parcelable);

        void setAutoFillChangeListener(OnDateChangedListener onDateChangedListener);

        void setEnabled(boolean z);

        void setFirstDayOfWeek(int i);

        void updateDate(int i, int i2, int i3);

        void updateDate(long j);
    }

    public interface OnDateChangedListener {
        void onDateChanged(DatePicker datePicker, int i, int i2, int i3);
    }

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843612);
    }

    public DatePicker(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DatePicker(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.DatePicker, i, i2);
        boolean z = obtainStyledAttributes.getBoolean(R$styleable.DatePicker_dialogMode, false);
        int i3 = obtainStyledAttributes.getInt(R$styleable.DatePicker_android_datePickerMode, 1);
        int i4 = obtainStyledAttributes.getInt(R$styleable.DatePicker_android_firstDayOfWeek, 0);
        obtainStyledAttributes.recycle();
        if (i3 != 2 || !z) {
            this.mMode = i3;
        } else {
            this.mMode = context.getResources().getInteger(R$integer.date_picker_mode);
        }
        if (this.mMode != 2) {
            this.mDelegate = createSpinnerUIDelegate(context, attributeSet, i, i2);
        } else {
            this.mDelegate = createCalendarUIDelegate(context, attributeSet, i, i2);
        }
        if (i4 != 0) {
            setFirstDayOfWeek(i4);
        }
        this.mDelegate.setAutoFillChangeListener(new OnDateChangedListener() {
            public void onDateChanged(DatePicker datePicker, int i, int i2, int i3) {
                if (VERSION.SDK_INT >= 26) {
                    ((AutofillManager) DatePicker.this.getContext().getSystemService(AutofillManager.class)).notifyValueChanged(DatePicker.this);
                }
            }
        });
    }

    private DatePickerDelegate createSpinnerUIDelegate(Context context, AttributeSet attributeSet, int i, int i2) {
        DatePickerSpinnerDelegate datePickerSpinnerDelegate = new DatePickerSpinnerDelegate(this, context, attributeSet, i, i2);
        return datePickerSpinnerDelegate;
    }

    private DatePickerDelegate createCalendarUIDelegate(Context context, AttributeSet attributeSet, int i, int i2) {
        DatePickerCalendarDelegate datePickerCalendarDelegate = new DatePickerCalendarDelegate(this, context, attributeSet, i, i2);
        return datePickerCalendarDelegate;
    }

    public void setEnabled(boolean z) {
        if (this.mDelegate.isEnabled() != z) {
            super.setEnabled(z);
            this.mDelegate.setEnabled(z);
        }
    }

    public boolean isEnabled() {
        return this.mDelegate.isEnabled();
    }

    public CharSequence getAccessibilityClassName() {
        return DatePicker.class.getName();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDelegate.onConfigurationChanged(configuration);
    }

    public void setFirstDayOfWeek(int i) {
        if (i < 1 || i > 7) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }
        this.mDelegate.setFirstDayOfWeek(i);
    }

    /* access modifiers changed from: protected */
    public void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        dispatchThawSelfOnly(sparseArray);
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        return this.mDelegate.onSaveInstanceState(super.onSaveInstanceState());
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        DatePickerDelegate datePickerDelegate = this.mDelegate;
        if (datePickerDelegate instanceof DatePickerCalendarDelegate) {
            ((DatePickerCalendarDelegate) datePickerDelegate).changeYearLayoutParams();
        }
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        BaseSavedState baseSavedState = (BaseSavedState) parcelable;
        super.onRestoreInstanceState(baseSavedState.getSuperState());
        this.mDelegate.onRestoreInstanceState(baseSavedState);
    }

    public void autofill(AutofillValue autofillValue) {
        if (isEnabled() && VERSION.SDK_INT >= 26) {
            if (!autofillValue.isDate()) {
                String str = LOG_TAG;
                StringBuilder sb = new StringBuilder();
                sb.append(autofillValue);
                sb.append(" could not be autofilled into ");
                sb.append(this);
                Log.w(str, sb.toString());
                return;
            }
            this.mDelegate.updateDate(autofillValue.getDateValue());
        }
    }

    public int getAutofillType() {
        return isEnabled() ? 4 : 0;
    }

    public AutofillValue getAutofillValue() {
        if (VERSION.SDK_INT < 26) {
            return super.getAutofillValue();
        }
        return isEnabled() ? AutofillValue.forDate(this.mDelegate.getDate()) : null;
    }
}
