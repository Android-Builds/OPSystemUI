package com.oneplus.lib.design.widget;

import android.content.Context;
import android.view.MenuItem;
import android.view.SubMenu;
import com.oneplus.lib.menu.MenuBuilder;
import com.oneplus.lib.menu.MenuItemImpl;

public final class BottomNavigationMenu extends MenuBuilder {
    public BottomNavigationMenu(Context context) {
        super(context);
    }

    public SubMenu addSubMenu(int i, int i2, int i3, CharSequence charSequence) {
        throw new UnsupportedOperationException("BottomNavigationView does not support submenus");
    }

    /* access modifiers changed from: protected */
    public MenuItem addInternal(int i, int i2, int i3, CharSequence charSequence) {
        if (size() + 1 <= 5) {
            stopDispatchingItemsChanged();
            MenuItem addInternal = super.addInternal(i, i2, i3, charSequence);
            if (addInternal instanceof MenuItemImpl) {
                ((MenuItemImpl) addInternal).setExclusiveCheckable(true);
            }
            startDispatchingItemsChanged();
            return addInternal;
        }
        throw new IllegalArgumentException("Maximum number of items supported by BottomNavigationView is 5. Limit can be checked with BottomNavigationView#getMaxItemCount()");
    }
}
