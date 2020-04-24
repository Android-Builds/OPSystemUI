package com.oneplus.lib.menu;

public interface MenuView {

    public interface ItemView {
        MenuItemImpl getItemData();

        void initialize(MenuItemImpl menuItemImpl, int i);

        boolean prefersCondensedTitle();
    }
}
