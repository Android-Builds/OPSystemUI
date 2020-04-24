package com.oneplus.lib.widget;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public abstract class ActionProvider {
    private final Context mContext;
    private SubUiVisibilityListener mSubUiVisibilityListener;
    private VisibilityListener mVisibilityListener;

    public interface SubUiVisibilityListener {
    }

    public interface VisibilityListener {
    }

    public abstract boolean hasSubMenu();

    public boolean isVisible() {
        return true;
    }

    public abstract View onCreateActionView();

    public abstract boolean onPerformDefaultAction();

    public abstract void onPrepareSubMenu(SubMenu subMenu);

    public boolean overridesItemVisibility() {
        return false;
    }

    public ActionProvider(Context context) {
        this.mContext = context;
    }

    public View onCreateActionView(MenuItem menuItem) {
        return onCreateActionView();
    }

    public void setSubUiVisibilityListener(SubUiVisibilityListener subUiVisibilityListener) {
        this.mSubUiVisibilityListener = subUiVisibilityListener;
    }

    public void setVisibilityListener(VisibilityListener visibilityListener) {
        if (!(this.mVisibilityListener == null || visibilityListener == null)) {
            StringBuilder sb = new StringBuilder();
            sb.append("setVisibilityListener: Setting a new ActionProvider.VisibilityListener when one is already set. Are you reusing this ");
            sb.append(getClass().getSimpleName());
            sb.append(" instance while it is still in use somewhere else?");
            Log.w("ActionProvider(support)", sb.toString());
        }
        this.mVisibilityListener = visibilityListener;
    }
}
