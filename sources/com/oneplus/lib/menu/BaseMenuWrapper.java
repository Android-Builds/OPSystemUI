package com.oneplus.lib.menu;

import android.content.Context;
import android.view.MenuItem;
import android.view.SubMenu;
import com.oneplus.support.collection.ArrayMap;
import java.util.Map;

abstract class BaseMenuWrapper<T> extends BaseWrapper<T> {
    final Context mContext;
    private Map<SupportMenuItem, MenuItem> mMenuItems;
    private Map<SupportSubMenu, SubMenu> mSubMenus;

    /* access modifiers changed from: 0000 */
    public final MenuItem getMenuItemWrapper(MenuItem menuItem) {
        if (!(menuItem instanceof SupportMenuItem)) {
            return menuItem;
        }
        SupportMenuItem supportMenuItem = (SupportMenuItem) menuItem;
        if (this.mMenuItems == null) {
            this.mMenuItems = new ArrayMap();
        }
        MenuItem menuItem2 = (MenuItem) this.mMenuItems.get(menuItem);
        if (menuItem2 != null) {
            return menuItem2;
        }
        this.mMenuItems.put(supportMenuItem, supportMenuItem);
        return supportMenuItem;
    }

    /* access modifiers changed from: 0000 */
    public final SubMenu getSubMenuWrapper(SubMenu subMenu) {
        if (!(subMenu instanceof SupportSubMenu)) {
            return subMenu;
        }
        SupportSubMenu supportSubMenu = (SupportSubMenu) subMenu;
        if (this.mSubMenus == null) {
            this.mSubMenus = new ArrayMap();
        }
        SubMenu subMenu2 = (SubMenu) this.mSubMenus.get(supportSubMenu);
        if (subMenu2 != null) {
            return subMenu2;
        }
        this.mSubMenus.put(supportSubMenu, supportSubMenu);
        return supportSubMenu;
    }
}
