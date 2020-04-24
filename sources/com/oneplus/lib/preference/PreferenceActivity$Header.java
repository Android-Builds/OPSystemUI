package com.oneplus.lib.preference;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

public final class PreferenceActivity$Header implements Parcelable {
    public static final Creator<PreferenceActivity$Header> CREATOR = new Creator<PreferenceActivity$Header>() {
        public PreferenceActivity$Header createFromParcel(Parcel parcel) {
            return new PreferenceActivity$Header(parcel);
        }

        public PreferenceActivity$Header[] newArray(int i) {
            return new PreferenceActivity$Header[i];
        }
    };
    public CharSequence breadCrumbShortTitle;
    public int breadCrumbShortTitleRes;
    public CharSequence breadCrumbTitle;
    public int breadCrumbTitleRes;
    public Bundle extras;
    public String fragment;
    public Bundle fragmentArguments;
    public int iconRes;

    /* renamed from: id */
    public long f116id = -1;
    public Intent intent;
    public CharSequence summary;
    public int summaryRes;
    public CharSequence title;
    public int titleRes;

    public int describeContents() {
        return 0;
    }

    public PreferenceActivity$Header() {
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.f116id);
        parcel.writeInt(this.titleRes);
        TextUtils.writeToParcel(this.title, parcel, i);
        parcel.writeInt(this.summaryRes);
        TextUtils.writeToParcel(this.summary, parcel, i);
        parcel.writeInt(this.breadCrumbTitleRes);
        TextUtils.writeToParcel(this.breadCrumbTitle, parcel, i);
        parcel.writeInt(this.breadCrumbShortTitleRes);
        TextUtils.writeToParcel(this.breadCrumbShortTitle, parcel, i);
        parcel.writeInt(this.iconRes);
        parcel.writeString(this.fragment);
        parcel.writeBundle(this.fragmentArguments);
        if (this.intent != null) {
            parcel.writeInt(1);
            this.intent.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeBundle(this.extras);
    }

    public void readFromParcel(Parcel parcel) {
        this.f116id = parcel.readLong();
        this.titleRes = parcel.readInt();
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.summaryRes = parcel.readInt();
        this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.breadCrumbTitleRes = parcel.readInt();
        this.breadCrumbTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.breadCrumbShortTitleRes = parcel.readInt();
        this.breadCrumbShortTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.iconRes = parcel.readInt();
        this.fragment = parcel.readString();
        this.fragmentArguments = parcel.readBundle();
        if (parcel.readInt() != 0) {
            this.intent = (Intent) Intent.CREATOR.createFromParcel(parcel);
        }
        this.extras = parcel.readBundle();
    }

    PreferenceActivity$Header(Parcel parcel) {
        readFromParcel(parcel);
    }
}
