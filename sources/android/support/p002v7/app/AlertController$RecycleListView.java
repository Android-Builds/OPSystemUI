package android.support.p002v7.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.p002v7.appcompat.R$styleable;
import android.util.AttributeSet;
import android.widget.ListView;

/* renamed from: android.support.v7.app.AlertController$RecycleListView */
public class AlertController$RecycleListView extends ListView {
    private final int mPaddingBottomNoButtons;
    private final int mPaddingTopNoTitle;

    public AlertController$RecycleListView(Context context) {
        this(context, null);
    }

    public AlertController$RecycleListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.RecycleListView);
        this.mPaddingBottomNoButtons = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.RecycleListView_paddingBottomNoButtons, -1);
        this.mPaddingTopNoTitle = obtainStyledAttributes.getDimensionPixelOffset(R$styleable.RecycleListView_paddingTopNoTitle, -1);
    }
}
