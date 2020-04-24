package com.oneplus.lib.design.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class BottomNavigationNotification implements Parcelable {
    public static final Creator<BottomNavigationNotification> CREATOR = new Creator<BottomNavigationNotification>() {
        public BottomNavigationNotification createFromParcel(Parcel parcel) {
            return new BottomNavigationNotification(parcel);
        }

        public BottomNavigationNotification[] newArray(int i) {
            return new BottomNavigationNotification[i];
        }
    };
    private int mBackgroundColor;
    private boolean mIndeterminate;
    private int mNumber;
    private int mTextColor;
    private int udbc;
    private int udtc;

    public int describeContents() {
        return 0;
    }

    private BottomNavigationNotification() {
        this.udtc = 0;
        this.udbc = 0;
    }

    private BottomNavigationNotification(Parcel parcel) {
        boolean z = false;
        this.udtc = 0;
        this.udbc = 0;
        this.mNumber = parcel.readInt();
        this.mTextColor = parcel.readInt();
        this.mBackgroundColor = parcel.readInt();
        this.udtc = parcel.readInt();
        this.udbc = parcel.readInt();
        if (parcel.readInt() == 0) {
            z = true;
        }
        this.mIndeterminate = z;
    }

    public boolean isIndeterminate() {
        return this.mIndeterminate;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mNumber);
        parcel.writeInt(this.mTextColor);
        parcel.writeInt(this.mBackgroundColor);
        parcel.writeInt(this.udtc);
        parcel.writeInt(this.udbc);
        parcel.writeInt(this.mIndeterminate ^ true ? 1 : 0);
    }
}
