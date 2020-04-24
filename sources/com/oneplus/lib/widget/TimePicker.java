package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.BaseSavedState;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$integer;
import com.oneplus.commonctrl.R$styleable;
import java.util.Calendar;
import java.util.Locale;

public class TimePicker extends FrameLayout {
    private static final String LOG_TAG = "TimePicker";
    private final TimePickerDelegate mDelegate;
    private final int mMode;

    static abstract class AbstractTimePickerDelegate implements TimePickerDelegate {
        protected OnTimeChangedListener mAutoFillChangeListener;
        protected final Context mContext;
        protected final TimePicker mDelegator;
        protected final Locale mLocale;
        protected OnTimeChangedListener mOnTimeChangedListener;

        protected static class SavedState extends BaseSavedState {
            public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
                public SavedState createFromParcel(Parcel parcel) {
                    return new SavedState(parcel);
                }

                public SavedState[] newArray(int i) {
                    return new SavedState[i];
                }
            };
            private final int mCurrentItemShowing;
            private final int mHour;
            private final boolean mIs24HourMode;
            private final int mMinute;

            public SavedState(Parcelable parcelable, int i, int i2, boolean z) {
                this(parcelable, i, i2, z, 0);
            }

            public SavedState(Parcelable parcelable, int i, int i2, boolean z, int i3) {
                super(parcelable);
                this.mHour = i;
                this.mMinute = i2;
                this.mIs24HourMode = z;
                this.mCurrentItemShowing = i3;
            }

            private SavedState(Parcel parcel) {
                super(parcel);
                this.mHour = parcel.readInt();
                this.mMinute = parcel.readInt();
                boolean z = true;
                if (parcel.readInt() != 1) {
                    z = false;
                }
                this.mIs24HourMode = z;
                this.mCurrentItemShowing = parcel.readInt();
            }

            public int getHour() {
                return this.mHour;
            }

            public int getMinute() {
                return this.mMinute;
            }

            public boolean is24HourMode() {
                return this.mIs24HourMode;
            }

            public int getCurrentItemShowing() {
                return this.mCurrentItemShowing;
            }

            public void writeToParcel(Parcel parcel, int i) {
                super.writeToParcel(parcel, i);
                parcel.writeInt(this.mHour);
                parcel.writeInt(this.mMinute);
                parcel.writeInt(this.mIs24HourMode ? 1 : 0);
                parcel.writeInt(this.mCurrentItemShowing);
            }
        }

        public AbstractTimePickerDelegate(TimePicker timePicker, Context context) {
            this.mDelegator = timePicker;
            this.mContext = context;
            this.mLocale = context.getResources().getConfiguration().locale;
        }

        public void setAutoFillChangeListener(OnTimeChangedListener onTimeChangedListener) {
            this.mAutoFillChangeListener = onTimeChangedListener;
        }

        public void setDate(long j) {
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(j);
            setHour(instance.get(11));
            setMinute(instance.get(12));
        }

        public long getDate() {
            Calendar instance = Calendar.getInstance(this.mLocale);
            instance.set(11, getHour());
            instance.set(12, getMinute());
            return instance.getTimeInMillis();
        }
    }

    public interface OnTimeChangedListener {
        void onTimeChanged(TimePicker timePicker, int i, int i2);
    }

    interface TimePickerDelegate {
        int getBaseline();

        long getDate();

        int getHour();

        int getMinute();

        boolean isEnabled();

        void onRestoreInstanceState(Parcelable parcelable);

        Parcelable onSaveInstanceState(Parcelable parcelable);

        void setAutoFillChangeListener(OnTimeChangedListener onTimeChangedListener);

        void setDate(long j);

        void setEnabled(boolean z);

        void setHour(int i);

        void setMinute(int i);
    }

    public TimePicker(Context context) {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.timePickerStyle);
    }

    public TimePicker(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TimePicker(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.TimePicker, i, i2);
        boolean z = obtainStyledAttributes.getBoolean(R$styleable.TimePicker_dialogMode, false);
        int i3 = obtainStyledAttributes.getInt(R$styleable.TimePicker_android_timePickerMode, 2);
        obtainStyledAttributes.recycle();
        if (i3 != 2 || !z) {
            this.mMode = i3;
        } else {
            this.mMode = context.getResources().getInteger(R$integer.time_picker_mode);
        }
        if (this.mMode != 2) {
            TimePickerSpinnerDelegate timePickerSpinnerDelegate = new TimePickerSpinnerDelegate(this, context, attributeSet, i, i2);
            this.mDelegate = timePickerSpinnerDelegate;
        } else {
            TimePickerClockDelegate timePickerClockDelegate = new TimePickerClockDelegate(this, context, attributeSet, i, i2);
            this.mDelegate = timePickerClockDelegate;
        }
        this.mDelegate.setAutoFillChangeListener(new OnTimeChangedListener() {
            public void onTimeChanged(TimePicker timePicker, int i, int i2) {
                if (VERSION.SDK_INT >= 26) {
                    ((AutofillManager) TimePicker.this.getContext().getSystemService(AutofillManager.class)).notifyValueChanged(TimePicker.this);
                }
            }
        });
    }

    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.mDelegate.setEnabled(z);
    }

    public boolean isEnabled() {
        return this.mDelegate.isEnabled();
    }

    public int getBaseline() {
        return this.mDelegate.getBaseline();
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        return this.mDelegate.onSaveInstanceState(super.onSaveInstanceState());
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        BaseSavedState baseSavedState = (BaseSavedState) parcelable;
        super.onRestoreInstanceState(baseSavedState.getSuperState());
        this.mDelegate.onRestoreInstanceState(baseSavedState);
    }

    public CharSequence getAccessibilityClassName() {
        return TimePicker.class.getName();
    }

    public static String[] getAmPmStrings(Context context) {
        return new String[]{DateUtils.getAMPMString(0), DateUtils.getAMPMString(1)};
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
            this.mDelegate.setDate(autofillValue.getDateValue());
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
