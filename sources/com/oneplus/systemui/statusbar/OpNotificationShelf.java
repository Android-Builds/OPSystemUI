package com.oneplus.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpNotificationShelf extends ActivatableNotificationView {
    protected static final boolean OP_DEBUG = OpUtils.DEBUG_ONEPLUS;

    /* access modifiers changed from: protected */
    public View getContentView() {
        return null;
    }

    public OpNotificationShelf(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void updateInteractivenessInternal() {
        boolean hasItemsInStableShelf = getHasItemsInStableShelf();
        boolean z = false;
        int i = 1;
        boolean z2 = getStatusBarState() == 1 && hasItemsInStableShelf && !this.mDark;
        setInteractive(z2);
        if (hasItemsInStableShelf && !this.mDark) {
            z = true;
        }
        setClickable(z);
        setFocusable(z2);
        if (!z2) {
            i = 4;
        }
        setImportantForAccessibility(i);
    }

    private boolean setInteractive(boolean z) {
        OpReflectionUtils.setValue(NotificationShelf.class, this, "mInteractive", Boolean.valueOf(z));
        return z;
    }

    private boolean getHasItemsInStableShelf() {
        return ((Boolean) OpReflectionUtils.getValue(NotificationShelf.class, this, "mHasItemsInStableShelf")).booleanValue();
    }

    private int getStatusBarState() {
        return ((Integer) OpReflectionUtils.getValue(NotificationShelf.class, this, "mStatusBarState")).intValue();
    }
}
