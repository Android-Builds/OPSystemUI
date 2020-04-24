package com.android.systemui.statusbar.notification.row;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.notification.stack.ExpandableViewState;

public class FooterView extends StackScrollerDecorView {
    /* access modifiers changed from: private */
    public final int mClearAllTopPadding;
    private FooterViewButton mDismissButton;
    private FooterViewButton mManageButton;

    public class FooterViewState extends ExpandableViewState {
        public FooterViewState() {
        }

        public void applyToView(View view) {
            super.applyToView(view);
            if (view instanceof FooterView) {
                FooterView footerView = (FooterView) view;
                boolean z = true;
                if (!(this.clipTopAmount < FooterView.this.mClearAllTopPadding) || !footerView.isVisible()) {
                    z = false;
                }
                footerView.setContentVisible(z);
            }
        }
    }

    public FooterView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mClearAllTopPadding = context.getResources().getDimensionPixelSize(R$dimen.clear_all_padding_top);
    }

    /* access modifiers changed from: protected */
    public View findContentView() {
        return findViewById(R$id.content);
    }

    /* access modifiers changed from: protected */
    public View findSecondaryView() {
        return findViewById(R$id.dismiss_text);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDismissButton = (FooterViewButton) findSecondaryView();
        this.mManageButton = (FooterViewButton) findViewById(R$id.manage_text);
    }

    public void setTextColor(int i) {
        this.mManageButton.setTextColor(i);
        this.mDismissButton.setTextColor(i);
    }

    public void setManageButtonClickListener(OnClickListener onClickListener) {
        this.mManageButton.setOnClickListener(onClickListener);
    }

    public void setDismissButtonClickListener(OnClickListener onClickListener) {
        this.mDismissButton.setOnClickListener(onClickListener);
    }

    public boolean isOnEmptySpace(float f, float f2) {
        return f < this.mContent.getX() || f > this.mContent.getX() + ((float) this.mContent.getWidth()) || f2 < this.mContent.getY() || f2 > this.mContent.getY() + ((float) this.mContent.getHeight());
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDismissButton.setText(R$string.clear_all_notifications_text);
        this.mDismissButton.setContentDescription(this.mContext.getString(R$string.accessibility_clear_all));
        this.mManageButton.setText(R$string.manage_notifications_text);
        this.mManageButton.setContentDescription(this.mContext.getString(R$string.accessibility_manage_notification));
    }

    public ExpandableViewState createExpandableViewState() {
        return new FooterViewState();
    }
}
