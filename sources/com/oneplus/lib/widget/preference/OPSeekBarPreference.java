package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.preference.Preference.BaseSavedState;
import com.oneplus.lib.widget.OPSeekBar;
import com.oneplus.lib.widget.OPSeekBar.OnSeekBarChangeListener;
import com.oneplus.lib.widget.util.C1963utils;

public class OPSeekBarPreference extends OPPreference implements OnSeekBarChangeListener {
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

    public OPSeekBarPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
        setMax(100);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SeekBarPreference, C1963utils.resolveDefStyleAttr(context, i), i2);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.SeekBarPreference_android_layout, R$layout.preference_widget_seekbar);
        obtainStyledAttributes.recycle();
        setLayoutResource(resourceId);
    }

    public OPSeekBarPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.OnePlus_DeviceDefault_Preference_Material_SeekBarPreference);
    }

    public OPSeekBarPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_seekBarPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        ((SeekBar) view.findViewById(R$id.seekbar)).setVisibility(8);
        OPSeekBar oPSeekBar = (OPSeekBar) view.findViewById(R$id.opseekbar);
        if (oPSeekBar != null) {
            oPSeekBar.setOnSeekBarChangeListener(this);
            oPSeekBar.setMax(this.mMax);
            oPSeekBar.setProgress(this.mProgress);
            oPSeekBar.setEnabled(isEnabled());
            oPSeekBar.setVisibility(0);
        }
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
    public void syncProgress(OPSeekBar oPSeekBar) {
        int progress = oPSeekBar.getProgress();
        if (progress == this.mProgress) {
            return;
        }
        if (callChangeListener(Integer.valueOf(progress))) {
            setProgress(progress, false);
        } else {
            oPSeekBar.setProgress(this.mProgress);
        }
    }

    public void onProgressChanged(OPSeekBar oPSeekBar, int i, boolean z) {
        if (z && !this.mTrackingTouch) {
            syncProgress(oPSeekBar);
        }
    }

    public void onStartTrackingTouch(OPSeekBar oPSeekBar) {
        this.mTrackingTouch = true;
    }

    public void onStopTrackingTouch(OPSeekBar oPSeekBar) {
        this.mTrackingTouch = false;
        if (oPSeekBar.getProgress() != this.mProgress) {
            syncProgress(oPSeekBar);
        }
    }
}
