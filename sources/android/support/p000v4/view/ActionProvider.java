package android.support.p000v4.view;

import android.view.MenuItem;
import android.view.View;

/* renamed from: android.support.v4.view.ActionProvider */
public abstract class ActionProvider {

    /* renamed from: android.support.v4.view.ActionProvider$SubUiVisibilityListener */
    public interface SubUiVisibilityListener {
    }

    public abstract boolean isVisible();

    public abstract View onCreateActionView(MenuItem menuItem);

    public abstract boolean overridesItemVisibility();

    public abstract void subUiVisibilityChanged(boolean z);
}
