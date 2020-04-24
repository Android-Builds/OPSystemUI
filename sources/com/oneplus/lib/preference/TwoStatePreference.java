package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import com.oneplus.lib.preference.Preference.BaseSavedState;

public abstract class TwoStatePreference extends Preference {
    boolean mChecked;
    private boolean mCheckedSet;
    private boolean mDisableDependentsState;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean checked;

        public SavedState(Parcel parcel) {
            super(parcel);
            boolean z = true;
            if (parcel.readInt() != 1) {
                z = false;
            }
            this.checked = z;
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.checked ? 1 : 0);
        }
    }

    public TwoStatePreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public TwoStatePreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /* access modifiers changed from: protected */
    public void onClick() {
        super.onClick();
        boolean z = !isChecked();
        if (callChangeListener(Boolean.valueOf(z))) {
            setChecked(z);
        }
    }

    public void setChecked(boolean z) {
        boolean z2 = this.mChecked != z;
        if (z2 || !this.mCheckedSet) {
            this.mChecked = z;
            this.mCheckedSet = true;
            persistBoolean(z);
            if (z2) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public boolean shouldDisableDependents() {
        boolean z = this.mDisableDependentsState ? this.mChecked : !this.mChecked;
        if (z || super.shouldDisableDependents()) {
            return true;
        }
        return false;
    }

    public void setSummaryOn(CharSequence charSequence) {
        setSummaryOnFromTwoState(charSequence);
        this.mSummaryOn = charSequence;
        if (isChecked()) {
            notifyChanged();
        }
    }

    public void setSummaryOff(CharSequence charSequence) {
        setSummaryOffFromTwoState(charSequence);
        this.mSummaryOff = charSequence;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    public void setDisableDependentsState(boolean z) {
        this.mDisableDependentsState = z;
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Boolean.valueOf(typedArray.getBoolean(i, false));
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0034  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004d  */
    /* JADX WARNING: Removed duplicated region for block: B:25:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void syncSummaryView(android.view.View r4) {
        /*
            r3 = this;
            r0 = 16908304(0x1020010, float:2.3877274E-38)
            android.view.View r4 = r4.findViewById(r0)
            android.widget.TextView r4 = (android.widget.TextView) r4
            if (r4 == 0) goto L_0x0050
            r0 = 1
            boolean r1 = r3.mChecked
            r2 = 0
            if (r1 == 0) goto L_0x0020
            java.lang.CharSequence r1 = r3.mSummaryOn
            boolean r1 = android.text.TextUtils.isEmpty(r1)
            if (r1 != 0) goto L_0x0020
            java.lang.CharSequence r0 = r3.mSummaryOn
            r4.setText(r0)
        L_0x001e:
            r0 = r2
            goto L_0x0032
        L_0x0020:
            boolean r1 = r3.mChecked
            if (r1 != 0) goto L_0x0032
            java.lang.CharSequence r1 = r3.mSummaryOff
            boolean r1 = android.text.TextUtils.isEmpty(r1)
            if (r1 != 0) goto L_0x0032
            java.lang.CharSequence r0 = r3.mSummaryOff
            r4.setText(r0)
            goto L_0x001e
        L_0x0032:
            if (r0 == 0) goto L_0x0042
            java.lang.CharSequence r3 = r3.getSummary()
            boolean r1 = android.text.TextUtils.isEmpty(r3)
            if (r1 != 0) goto L_0x0042
            r4.setText(r3)
            r0 = r2
        L_0x0042:
            r3 = 8
            if (r0 != 0) goto L_0x0047
            r3 = r2
        L_0x0047:
            int r0 = r4.getVisibility()
            if (r3 == r0) goto L_0x0050
            r4.setVisibility(r3)
        L_0x0050:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.preference.TwoStatePreference.syncSummaryView(android.view.View):void");
    }
}
