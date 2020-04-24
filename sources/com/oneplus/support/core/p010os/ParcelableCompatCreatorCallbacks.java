package com.oneplus.support.core.p010os;

import android.os.Parcel;

@Deprecated
/* renamed from: com.oneplus.support.core.os.ParcelableCompatCreatorCallbacks */
public interface ParcelableCompatCreatorCallbacks<T> {
    T createFromParcel(Parcel parcel, ClassLoader classLoader);

    T[] newArray(int i);
}
