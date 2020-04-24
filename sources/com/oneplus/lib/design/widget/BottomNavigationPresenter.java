package com.oneplus.lib.design.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.oneplus.lib.menu.MenuBuilder;
import com.oneplus.lib.menu.MenuItemImpl;
import com.oneplus.lib.menu.MenuPresenter;
import com.oneplus.lib.menu.SubMenuBuilder;

public class BottomNavigationPresenter implements MenuPresenter {

    /* renamed from: id */
    private int f115id;
    private MenuBuilder menu;
    private BottomNavigationMenuView menuView;
    private boolean updateSuspended = false;

    static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int selectedItemId;

        public int describeContents() {
            return 0;
        }

        SavedState() {
        }

        SavedState(Parcel parcel) {
            this.selectedItemId = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.selectedItemId);
        }
    }

    public boolean collapseItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
        return false;
    }

    public boolean expandItemActionView(MenuBuilder menuBuilder, MenuItemImpl menuItemImpl) {
        return false;
    }

    public boolean flagActionItems() {
        return false;
    }

    public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
    }

    public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
        return false;
    }

    public void setBottomNavigationMenuView(BottomNavigationMenuView bottomNavigationMenuView) {
        this.menuView = bottomNavigationMenuView;
    }

    public void initForMenu(Context context, MenuBuilder menuBuilder) {
        this.menu = menuBuilder;
        this.menuView.initialize(this.menu);
    }

    public void updateMenuView(boolean z) {
        if (!this.updateSuspended) {
            if (z) {
                this.menuView.buildMenuView();
            } else {
                this.menuView.updateMenuView();
            }
        }
    }

    public void setId(int i) {
        this.f115id = i;
    }

    public int getId() {
        return this.f115id;
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.selectedItemId = this.menuView.getSelectedItemId();
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof SavedState) {
            this.menuView.tryRestoreSelectedItemId(((SavedState) parcelable).selectedItemId);
        }
    }

    public void setUpdateSuspended(boolean z) {
        this.updateSuspended = z;
    }
}
