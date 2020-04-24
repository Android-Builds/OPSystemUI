package com.oneplus.lib.widget.recyclerview;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

class StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem implements Parcelable {
    public static final Creator<StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem> CREATOR = new Creator<StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem>() {
        public StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem createFromParcel(Parcel parcel) {
            return new StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem(parcel);
        }

        public StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem[] newArray(int i) {
            return new StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem[i];
        }
    };
    int mGapDir;
    int[] mGapPerSpan;
    boolean mHasUnwantedGapAfter;
    int mPosition;

    public int describeContents() {
        return 0;
    }

    public StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem(Parcel parcel) {
        this.mPosition = parcel.readInt();
        this.mGapDir = parcel.readInt();
        boolean z = true;
        if (parcel.readInt() != 1) {
            z = false;
        }
        this.mHasUnwantedGapAfter = z;
        int readInt = parcel.readInt();
        if (readInt > 0) {
            this.mGapPerSpan = new int[readInt];
            parcel.readIntArray(this.mGapPerSpan);
        }
    }

    public StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem() {
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPosition);
        parcel.writeInt(this.mGapDir);
        parcel.writeInt(this.mHasUnwantedGapAfter ? 1 : 0);
        int[] iArr = this.mGapPerSpan;
        if (iArr == null || iArr.length <= 0) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(iArr.length);
        parcel.writeIntArray(this.mGapPerSpan);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FullSpanItem{mPosition=");
        sb.append(this.mPosition);
        sb.append(", mGapDir=");
        sb.append(this.mGapDir);
        sb.append(", mHasUnwantedGapAfter=");
        sb.append(this.mHasUnwantedGapAfter);
        sb.append(", mGapPerSpan=");
        sb.append(Arrays.toString(this.mGapPerSpan));
        sb.append('}');
        return sb.toString();
    }
}
