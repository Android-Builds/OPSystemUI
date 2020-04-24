package androidx.appcompat.widget;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.HorizontalScrollView;

public class ScrollingTabContainerView extends HorizontalScrollView implements OnItemSelectedListener {
    private static final Interpolator sAlphaInterpolator = new DecelerateInterpolator();

    public void setAllowCollapse(boolean z) {
        throw null;
    }
}
