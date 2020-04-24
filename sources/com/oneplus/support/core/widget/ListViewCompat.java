package com.oneplus.support.core.widget;

import android.os.Build.VERSION;
import android.view.View;
import android.widget.ListView;

public final class ListViewCompat {
    public static void scrollListBy(ListView listView, int i) {
        if (VERSION.SDK_INT >= 19) {
            listView.scrollListBy(i);
        } else {
            int firstVisiblePosition = listView.getFirstVisiblePosition();
            if (firstVisiblePosition != -1) {
                View childAt = listView.getChildAt(0);
                if (childAt != null) {
                    listView.setSelectionFromTop(firstVisiblePosition, childAt.getTop() - i);
                }
            }
        }
    }
}
