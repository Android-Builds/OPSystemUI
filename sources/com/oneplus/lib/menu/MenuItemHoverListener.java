package com.oneplus.lib.menu;

import android.view.MenuItem;

public interface MenuItemHoverListener {
    void onItemHoverEnter(MenuBuilder menuBuilder, MenuItem menuItem);

    void onItemHoverExit(MenuBuilder menuBuilder, MenuItem menuItem);
}
