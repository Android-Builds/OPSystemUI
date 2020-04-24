package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.preference.Preference.BaseSavedState;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
    private int mMax;
    private int mProgress;
    private boolean mTrackingTouch;

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int max;
        int progress;

        public SavedState(Parcel parcel) {
            super(parcel);
            this.progress = parcel.readInt();
            this.max = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.progress);
            parcel.writeInt(this.max);
        }
    }

    public CharSequence getSummary() {
        return null;
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setMax(100);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SeekBarPreference, i, i2);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.SeekBarPreference_android_layout, R$layout.preference_widget_seekbar);
        obtainStyledAttributes.recycle();
        setLayoutResource(resourceId);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_seekBarPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        SeekBar seekBar = (SeekBar) view.findViewById(R$id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(this.mMax);
        seekBar.setProgress(this.mProgress);
        seekBar.setEnabled(isEnabled());
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return Integer.valueOf(typedArray.getInt(i, 0));
    }

    public void setMax(int i) {
        if (i != this.mMax) {
            this.mMax = i;
            notifyChanged();
        }
    }

    private void setProgress(int i, boolean z) {
        int i2 = this.mMax;
        if (i > i2) {
            i = i2;
        }
        if (i < 0) {
            i = 0;
        }
        if (i != this.mProgress) {
            this.mProgress = i;
            persistInt(i);
            if (z) {
                notifyChanged();
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress == this.mProgress) {
            return;
        }
        if (callChangeListener(Integer.valueOf(progress))) {
            setProgress(progress, false);
        } else {
            seekBar.setProgress(this.mProgress);
        }
    }

    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z && !this.mTrackingTouch) {
            syncProgress(seekBar);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = true;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mTrackingTouch = false;
        if (seekBar.getProgress() != this.mProgress) {
            syncProgress(seekBar);
        }
    }
}
