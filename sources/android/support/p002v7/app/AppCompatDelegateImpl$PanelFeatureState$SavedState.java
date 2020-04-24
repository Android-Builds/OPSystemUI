package android.support.p002v7.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;

/* renamed from: android.support.v7.app.AppCompatDelegateImpl$PanelFeatureState$SavedState */
class AppCompatDelegateImpl$PanelFeatureState$SavedState implements Parcelable {
    public static final Creator<AppCompatDelegateImpl$PanelFeatureState$SavedState> CREATOR = new ClassLoaderCreator<AppCompatDelegateImpl$PanelFeatureState$SavedState>() {
        public AppCompatDelegateImpl$PanelFeatureState$SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
            return AppCompatDelegateImpl$PanelFeatureState$SavedState.readFromParcel(parcel, classLoader);
        }

        public AppCompatDelegateImpl$PanelFeatureState$SavedState createFromParcel(Parcel parcel) {
            return AppCompatDelegateImpl$PanelFeatureState$SavedState.readFromParcel(parcel, null);
        }

        public AppCompatDelegateImpl$PanelFeatureState$SavedState[] newArray(int i) {
            return new AppCompatDelegateImpl$PanelFeatureState$SavedState[i];
        }
    };
    int featureId;
    boolean isOpen;
    Bundle menuState;

    public int describeContents() {
        return 0;
    }

    AppCompatDelegateImpl$PanelFeatureState$SavedState() {
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.featureId);
        parcel.writeInt(this.isOpen ? 1 : 0);
        if (this.isOpen) {
            parcel.writeBundle(this.menuState);
        }
    }

    static AppCompatDelegateImpl$PanelFeatureState$SavedState readFromParcel(Parcel parcel, ClassLoader classLoader) {
        AppCompatDelegateImpl$PanelFeatureState$SavedState appCompatDelegateImpl$PanelFeatureState$SavedState = new AppCompatDelegateImpl$PanelFeatureState$SavedState();
        appCompatDelegateImpl$PanelFeatureState$SavedState.featureId = parcel.readInt();
        boolean z = true;
        if (parcel.readInt() != 1) {
            z = false;
        }
        appCompatDelegateImpl$PanelFeatureState$SavedState.isOpen = z;
        if (appCompatDelegateImpl$PanelFeatureState$SavedState.isOpen) {
            appCompatDelegateImpl$PanelFeatureState$SavedState.menuState = parcel.readBundle(classLoader);
        }
        return appCompatDelegateImpl$PanelFeatureState$SavedState;
    }
}
