package android.support.p002v7.widget;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.p000v4.view.ActionProvider.SubUiVisibilityListener;
import android.support.p002v7.view.menu.BaseMenuPresenter;

/* renamed from: android.support.v7.widget.ActionMenuPresenter */
class ActionMenuPresenter extends BaseMenuPresenter implements SubUiVisibilityListener {

    /* renamed from: android.support.v7.widget.ActionMenuPresenter$SavedState */
    private static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        public int openSubMenuId;

        public int describeContents() {
            return 0;
        }

        SavedState() {
        }

        SavedState(Parcel parcel) {
            this.openSubMenuId = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.openSubMenuId);
        }
    }

    public boolean dismissPopupMenus() {
        throw null;
    }

    public boolean hideOverflowMenu() {
        throw null;
    }

    public boolean isOverflowMenuShowing() {
        throw null;
    }

    public void onConfigurationChanged(Configuration configuration) {
        throw null;
    }

    public boolean showOverflowMenu() {
        throw null;
    }

    public void updateMenuView(boolean z) {
        throw null;
    }
}
