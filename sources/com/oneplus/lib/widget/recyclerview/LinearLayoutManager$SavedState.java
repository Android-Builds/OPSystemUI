package com.oneplus.lib.widget.recyclerview;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

class LinearLayoutManager$SavedState implements Parcelable {
    public static final Creator<LinearLayoutManager$SavedState> CREATOR = new Creator<LinearLayoutManager$SavedState>() {
        public LinearLayoutManager$SavedState createFromParcel(Parcel parcel) {
            return new LinearLayoutManager$SavedState(parcel);
        }

        public LinearLayoutManager$SavedState[] newArray(int i) {
            return new LinearLayoutManager$SavedState[i];
        }
    };
    boolean mAnchorLayoutFromEnd;
    int mAnchorOffset;
    int mAnchorPosition;

    public int describeContents() {
        return 0;
    }

    public LinearLayoutManager$SavedState() {
    }

    LinearLayoutManager$SavedState(Parcel parcel) {
        this.mAnchorPosition = parcel.readInt();
        this.mAnchorOffset = parcel.readInt();
        boolean z = true;
        if (parcel.readInt() != 1) {
            z = false;
        }
        this.mAnchorLayoutFromEnd = z;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAnchorPosition);
        parcel.writeInt(this.mAnchorOffset);
        parcel.writeInt(this.mAnchorLayoutFromEnd ? 1 : 0);
    }
}
