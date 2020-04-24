package android.support.p000v4.view;

import android.os.Build.VERSION;
import android.support.compat.R$id;
import android.view.ViewGroup;

/* renamed from: android.support.v4.view.ViewGroupCompat */
public final class ViewGroupCompat {
    public static boolean isTransitionGroup(ViewGroup viewGroup) {
        if (VERSION.SDK_INT >= 21) {
            return viewGroup.isTransitionGroup();
        }
        Boolean bool = (Boolean) viewGroup.getTag(R$id.tag_transition_group);
        return ((bool == null || !bool.booleanValue()) && viewGroup.getBackground() == null && ViewCompat.getTransitionName(viewGroup) == null) ? false : true;
    }
}
