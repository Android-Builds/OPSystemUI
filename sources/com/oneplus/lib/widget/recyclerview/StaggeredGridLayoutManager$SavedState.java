package com.oneplus.lib.widget.recyclerview;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.List;

class StaggeredGridLayoutManager$SavedState implements Parcelable {
    public static final Creator<StaggeredGridLayoutManager$SavedState> CREATOR = new Creator<StaggeredGridLayoutManager$SavedState>() {
        public StaggeredGridLayoutManager$SavedState createFromParcel(Parcel parcel) {
            return new StaggeredGridLayoutManager$SavedState(parcel);
        }

        public StaggeredGridLayoutManager$SavedState[] newArray(int i) {
            return new StaggeredGridLayoutManager$SavedState[i];
        }
    };
    boolean mAnchorLayoutFromEnd;
    int mAnchorPosition;
    List<StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem> mFullSpanItems;
    boolean mLastLayoutRTL;
    boolean mReverseLayout;
    int[] mSpanLookup;
    int mSpanLookupSize;
    int[] mSpanOffsets;
    int mSpanOffsetsSize;
    int mVisibleAnchorPosition;

    public int describeContents() {
        return 0;
    }

    public StaggeredGridLayoutManager$SavedState() {
    }

    StaggeredGridLayoutManager$SavedState(Parcel parcel) {
        this.mAnchorPosition = parcel.readInt();
        this.mVisibleAnchorPosition = parcel.readInt();
        this.mSpanOffsetsSize = parcel.readInt();
        int i = this.mSpanOffsetsSize;
        if (i > 0) {
            this.mSpanOffsets = new int[i];
            parcel.readIntArray(this.mSpanOffsets);
        }
        this.mSpanLookupSize = parcel.readInt();
        int i2 = this.mSpanLookupSize;
        if (i2 > 0) {
            this.mSpanLookup = new int[i2];
            parcel.readIntArray(this.mSpanLookup);
        }
        boolean z = false;
        this.mReverseLayout = parcel.readInt() == 1;
        this.mAnchorLayoutFromEnd = parcel.readInt() == 1;
        if (parcel.readInt() == 1) {
            z = true;
        }
        this.mLastLayoutRTL = z;
        this.mFullSpanItems = parcel.readArrayList(StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem.class.getClassLoader());
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAnchorPosition);
        parcel.writeInt(this.mVisibleAnchorPosition);
        parcel.writeInt(this.mSpanOffsetsSize);
        if (this.mSpanOffsetsSize > 0) {
            parcel.writeIntArray(this.mSpanOffsets);
        }
        parcel.writeInt(this.mSpanLookupSize);
        if (this.mSpanLookupSize > 0) {
            parcel.writeIntArray(this.mSpanLookup);
        }
        parcel.writeInt(this.mReverseLayout ? 1 : 0);
        parcel.writeInt(this.mAnchorLayoutFromEnd ? 1 : 0);
        parcel.writeInt(this.mLastLayoutRTL ? 1 : 0);
        parcel.writeList(this.mFullSpanItems);
    }
}
