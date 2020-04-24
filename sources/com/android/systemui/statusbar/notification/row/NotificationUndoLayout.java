package com.android.systemui.statusbar.notification.row;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.android.systemui.R$id;

public class NotificationUndoLayout extends FrameLayout {
    private View mConfirmationTextView;
    private boolean mIsMultiline;
    private int mMultilineTopMargin;
    private View mUndoView;

    public NotificationUndoLayout(Context context) {
        this(context, null);
    }

    public NotificationUndoLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NotificationUndoLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsMultiline = false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mConfirmationTextView = findViewById(R$id.confirmation_text);
        this.mUndoView = findViewById(R$id.undo);
        this.mMultilineTopMargin = getResources().getDimensionPixelOffset(17105308);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        LayoutParams layoutParams = (LayoutParams) this.mConfirmationTextView.getLayoutParams();
        LayoutParams layoutParams2 = (LayoutParams) this.mUndoView.getLayoutParams();
        int measuredWidth = getMeasuredWidth();
        if (this.mConfirmationTextView.getMeasuredWidth() + layoutParams.rightMargin + layoutParams.leftMargin + this.mUndoView.getMeasuredWidth() + layoutParams2.rightMargin > measuredWidth) {
            this.mIsMultiline = true;
            setMeasuredDimension(measuredWidth, this.mMultilineTopMargin + this.mConfirmationTextView.getMeasuredHeight() + this.mUndoView.getMeasuredHeight() + layoutParams2.topMargin + layoutParams2.bottomMargin);
            return;
        }
        this.mIsMultiline = false;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        if (this.mIsMultiline) {
            int measuredHeight = getMeasuredHeight();
            int measuredWidth = getMeasuredWidth();
            LayoutParams layoutParams = (LayoutParams) this.mConfirmationTextView.getLayoutParams();
            LayoutParams layoutParams2 = (LayoutParams) this.mUndoView.getLayoutParams();
            this.mConfirmationTextView.layout(layoutParams.leftMargin, this.mMultilineTopMargin, layoutParams.leftMargin + this.mConfirmationTextView.getMeasuredWidth(), this.mMultilineTopMargin + this.mConfirmationTextView.getMeasuredHeight());
            if (getLayoutDirection() == 1) {
                i5 = layoutParams2.rightMargin;
            } else {
                i5 = (measuredWidth - this.mUndoView.getMeasuredWidth()) - layoutParams2.rightMargin;
            }
            View view = this.mUndoView;
            view.layout(i5, (measuredHeight - view.getMeasuredHeight()) - layoutParams2.bottomMargin, this.mUndoView.getMeasuredWidth() + i5, measuredHeight - layoutParams2.bottomMargin);
            return;
        }
        super.onLayout(z, i, i2, i3, i4);
    }
}
