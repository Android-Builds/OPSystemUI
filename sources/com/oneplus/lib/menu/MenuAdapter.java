package com.oneplus.lib.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.lib.menu.MenuView.ItemView;
import java.util.ArrayList;

public class MenuAdapter extends BaseAdapter {
    static final int ITEM_LAYOUT = R$layout.op_abc_popup_menu_item_layout;
    static final int PADDING_BOTTOM = R$dimen.oneplus_contorl_margin_bottom1;
    static final int PADDING_TOP = R$dimen.oneplus_contorl_margin_top1;
    MenuBuilder mAdapterMenu;
    private int mExpandedIndex = -1;
    private boolean mForceShowIcon;
    private final LayoutInflater mInflater;
    private final boolean mOverflowOnly;
    private int mPaddingBottom;
    private int mPaddingTop;

    public long getItemId(int i) {
        return (long) i;
    }

    public MenuAdapter(MenuBuilder menuBuilder, LayoutInflater layoutInflater, boolean z) {
        this.mOverflowOnly = z;
        this.mInflater = layoutInflater;
        this.mAdapterMenu = menuBuilder;
        if (layoutInflater.getContext() != null) {
            this.mPaddingBottom = layoutInflater.getContext().getResources().getDimensionPixelSize(PADDING_BOTTOM);
            this.mPaddingTop = layoutInflater.getContext().getResources().getDimensionPixelSize(PADDING_TOP);
        }
        findExpandedIndex();
    }

    public void setForceShowIcon(boolean z) {
        this.mForceShowIcon = z;
    }

    public int getCount() {
        ArrayList nonActionItems = this.mOverflowOnly ? this.mAdapterMenu.getNonActionItems() : this.mAdapterMenu.getVisibleItems();
        if (this.mExpandedIndex < 0) {
            return nonActionItems.size();
        }
        return nonActionItems.size() - 1;
    }

    public MenuBuilder getAdapterMenu() {
        return this.mAdapterMenu;
    }

    public MenuItemImpl getItem(int i) {
        ArrayList nonActionItems = this.mOverflowOnly ? this.mAdapterMenu.getNonActionItems() : this.mAdapterMenu.getVisibleItems();
        int i2 = this.mExpandedIndex;
        if (i2 >= 0 && i >= i2) {
            i++;
        }
        return (MenuItemImpl) nonActionItems.get(i);
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(ITEM_LAYOUT, viewGroup, false);
        }
        ItemView itemView = (ItemView) view;
        if (this.mForceShowIcon) {
            ((ListMenuItemView) view).setForceShowIcon(true);
        }
        itemView.initialize(getItem(i), 0);
        return view;
    }

    /* access modifiers changed from: 0000 */
    public void findExpandedIndex() {
        MenuItemImpl expandedItem = this.mAdapterMenu.getExpandedItem();
        if (expandedItem != null) {
            ArrayList nonActionItems = this.mAdapterMenu.getNonActionItems();
            int size = nonActionItems.size();
            for (int i = 0; i < size; i++) {
                if (((MenuItemImpl) nonActionItems.get(i)) == expandedItem) {
                    this.mExpandedIndex = i;
                    return;
                }
            }
        }
        this.mExpandedIndex = -1;
    }

    public void notifyDataSetChanged() {
        findExpandedIndex();
        super.notifyDataSetChanged();
    }
}
