package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$id;
import com.oneplus.lib.preference.Preference.BaseSavedState;

public class EditTextPreference extends DialogPreference {
    private EditText mEditText;
    private String mText;

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        String text;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.text = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(this.text);
        }
    }

    public EditTextPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mEditText = new EditText(context, attributeSet);
        this.mEditText.setId(16908291);
        this.mEditText.setEnabled(true);
    }

    public EditTextPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public EditTextPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_editTextPreferenceStyle);
    }

    public String getText() {
        return this.mText;
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = this.mEditText;
        editText.setText(getText());
        ViewParent parent = editText.getParent();
        if (parent != view) {
            if (parent != null) {
                ((ViewGroup) parent).removeView(editText);
            }
            onAddEditTextToDialogView(view, editText);
        }
    }

    /* access modifiers changed from: protected */
    public void onAddEditTextToDialogView(View view, EditText editText) {
        ViewGroup viewGroup = (ViewGroup) view.findViewById(R$id.edittext_container);
        if (viewGroup != null) {
            viewGroup.addView(editText, -1, -2);
        }
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return typedArray.getString(i);
    }

    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(this.mText) || super.shouldDisableDependents();
    }
}
