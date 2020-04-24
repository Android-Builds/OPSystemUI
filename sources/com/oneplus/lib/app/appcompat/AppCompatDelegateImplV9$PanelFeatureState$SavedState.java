package com.oneplus.lib.app.appcompat;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

class AppCompatDelegateImplV9$PanelFeatureState$SavedState implements Parcelable {
    public static final Creator<AppCompatDelegateImplV9$PanelFeatureState$SavedState> CREATOR = new Creator<AppCompatDelegateImplV9$PanelFeatureState$SavedState>() {
        public AppCompatDelegateImplV9$PanelFeatureState$SavedState createFromParcel(Parcel parcel) {
            return new AppCompatDelegateImplV9$PanelFeatureState$SavedState(parcel);
        }

        public AppCompatDelegateImplV9$PanelFeatureState$SavedState[] newArray(int i) {
            return new AppCompatDelegateImplV9$PanelFeatureState$SavedState[i];
        }
    };
    int featureId;
    boolean isOpen;
    Bundle menuState;

    public int describeContents() {
        return 0;
    }

    AppCompatDelegateImplV9$PanelFeatureState$SavedState() {
    }

    protected AppCompatDelegateImplV9$PanelFeatureState$SavedState(Parcel parcel) {
        this.featureId = parcel.readInt();
        this.isOpen = parcel.readByte() != 0;
        this.menuState = parcel.readBundle();
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.featureId);
        parcel.writeInt(this.isOpen ? 1 : 0);
        if (this.isOpen) {
            parcel.writeBundle(this.menuState);
        }
    }
}
