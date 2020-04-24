package com.oneplus.lib.animator;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ShareElementViewAttrs implements Parcelable {
    public static final Creator<ShareElementViewAttrs> CREATOR = new Creator<ShareElementViewAttrs>() {
        public ShareElementViewAttrs createFromParcel(Parcel parcel) {
            return new ShareElementViewAttrs(parcel);
        }

        public ShareElementViewAttrs[] newArray(int i) {
            return new ShareElementViewAttrs[i];
        }
    };
    public float alpha;
    public float height;

    /* renamed from: id */
    public int f113id;
    public float startX;
    public float startY;
    public float width;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.f113id);
        parcel.writeFloat(this.startX);
        parcel.writeFloat(this.startY);
        parcel.writeFloat(this.width);
        parcel.writeFloat(this.height);
        parcel.writeFloat(this.alpha);
    }

    protected ShareElementViewAttrs(Parcel parcel) {
        this.f113id = parcel.readInt();
        this.startX = parcel.readFloat();
        this.startY = parcel.readFloat();
        this.width = parcel.readFloat();
        this.height = parcel.readFloat();
        this.alpha = parcel.readFloat();
    }
}
