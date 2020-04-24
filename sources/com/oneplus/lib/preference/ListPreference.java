package com.oneplus.lib.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.app.OPAlertDialog.Builder;
import com.oneplus.lib.preference.Preference.BaseSavedState;

public class ListPreference extends DialogPreference {
    /* access modifiers changed from: private */
    public int mClickedDialogEntryIndex;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private String mSummary;
    private String mValue;

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        String value;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.value = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(this.value);
        }
    }

    public ListPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.ListPreference, i, i2);
        this.mEntries = obtainStyledAttributes.getTextArray(R$styleable.ListPreference_android_entries);
        this.mEntryValues = obtainStyledAttributes.getTextArray(R$styleable.ListPreference_android_entryValues);
        obtainStyledAttributes.recycle();
        TypedArray obtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R$styleable.Preference, i, i2);
        this.mSummary = obtainStyledAttributes2.getString(R$styleable.Preference_android_summary);
        obtainStyledAttributes2.recycle();
    }

    public ListPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ListPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_dialogPreferenceStyle);
    }

    public CharSequence getSummary() {
        Object entry = getEntry();
        String str = this.mSummary;
        if (str == null) {
            return super.getSummary();
        }
        Object[] objArr = new Object[1];
        if (entry == null) {
            entry = "";
        }
        objArr[0] = entry;
        return String.format(str, objArr);
    }

    public CharSequence getEntry() {
        int valueIndex = getValueIndex();
        if (valueIndex >= 0) {
            CharSequence[] charSequenceArr = this.mEntries;
            if (charSequenceArr != null) {
                return charSequenceArr[valueIndex];
            }
        }
        return null;
    }

    public int findIndexOfValue(String str) {
        if (str != null) {
            CharSequence[] charSequenceArr = this.mEntryValues;
            if (charSequenceArr != null) {
                for (int length = charSequenceArr.length - 1; length >= 0; length--) {
                    if (this.mEntryValues[length].equals(str)) {
                        return length;
                    }
                }
            }
        }
        return -1;
    }

    private int getValueIndex() {
        return findIndexOfValue(this.mValue);
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (this.mEntries == null || this.mEntryValues == null) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
        }
        this.mClickedDialogEntryIndex = getValueIndex();
        builder.setSingleChoiceItems(this.mEntries, this.mClickedDialogEntryIndex, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                ListPreference.this.mClickedDialogEntryIndex = i;
                ListPreference.this.onClick(dialogInterface, -1);
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(null, null);
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return typedArray.getString(i);
    }
}
