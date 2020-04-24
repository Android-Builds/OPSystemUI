package com.android.systemui.statusbar.notification.row.wrapper;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;

public class NotificationDecoratedCustomViewWrapper extends NotificationTemplateViewWrapper {
    private View mWrappedView = null;

    protected NotificationDecoratedCustomViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
    }

    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        ViewGroup viewGroup = (ViewGroup) this.mView.findViewById(16909165);
        Integer num = (Integer) viewGroup.getTag(16909163);
        if (!(num == null || num.intValue() == -1)) {
            this.mWrappedView = viewGroup.getChildAt(num.intValue());
        }
        if (needsInversion(resolveBackgroundColor(), this.mWrappedView)) {
            View view = this.mView;
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup2 = (ViewGroup) view;
                int childCount = viewGroup2.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View childAt = viewGroup2.getChildAt(i);
                    if (!(childAt instanceof ImageView)) {
                        invertViewLuminosity(childAt);
                    }
                }
            }
        }
        super.onContentUpdated(expandableNotificationRow);
    }
}
