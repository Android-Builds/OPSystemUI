package android.support.p002v7.view.menu;

import android.content.Context;
import android.support.p000v4.internal.view.SupportMenu;
import android.view.MenuItem;

/* renamed from: android.support.v7.view.menu.MenuBuilder */
public class MenuBuilder implements SupportMenu {
    private static final int[] sCategoryToOrder = {1, 4, 5, 3, 2, 0};

    /* renamed from: android.support.v7.view.menu.MenuBuilder$ItemInvoker */
    public interface ItemInvoker {
        boolean invokeItem(MenuItemImpl menuItemImpl);
    }

    public boolean collapseItemActionView(MenuItemImpl menuItemImpl) {
        throw null;
    }

    public boolean expandItemActionView(MenuItemImpl menuItemImpl) {
        throw null;
    }

    public Context getContext() {
        throw null;
    }

    public boolean hasVisibleItems() {
        throw null;
    }

    /* access modifiers changed from: 0000 */
    public void onItemActionRequestChanged(MenuItemImpl menuItemImpl) {
        throw null;
    }

    /* access modifiers changed from: 0000 */
    public void onItemVisibleChanged(MenuItemImpl menuItemImpl) {
        throw null;
    }

    public void onItemsChanged(boolean z) {
        throw null;
    }

    public boolean performItemAction(MenuItem menuItem, int i) {
        throw null;
    }

    /* access modifiers changed from: 0000 */
    public void setExclusiveItemChecked(MenuItem menuItem) {
        throw null;
    }
}
