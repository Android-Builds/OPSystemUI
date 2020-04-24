package com.android.systemui.statusbar.notification.stack;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;

public class SectionHeaderView extends ActivatableNotificationView {
    private ImageView mClearAllButton;
    private ViewGroup mContents;
    private TextView mLabelView;
    private OnClickListener mOnClearClickListener = null;
    private final RectF mTmpRect = new RectF();

    public SectionHeaderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContents = (ViewGroup) Preconditions.checkNotNull((ViewGroup) findViewById(R$id.content));
        bindContents();
    }

    private void bindContents() {
        this.mLabelView = (TextView) Preconditions.checkNotNull((TextView) findViewById(R$id.header_label));
        this.mClearAllButton = (ImageView) Preconditions.checkNotNull((ImageView) findViewById(R$id.btn_clear_all));
        OnClickListener onClickListener = this.mOnClearClickListener;
        if (onClickListener != null) {
            this.mClearAllButton.setOnClickListener(onClickListener);
        }
    }

    /* access modifiers changed from: protected */
    public View getContentView() {
        return this.mContents;
    }

    /* access modifiers changed from: 0000 */
    public void reinflateContents() {
        this.mContents.removeAllViews();
        LayoutInflater.from(getContext()).inflate(R$layout.status_bar_notification_section_header_contents, this.mContents);
        bindContents();
    }

    /* access modifiers changed from: 0000 */
    public void onUiModeChanged() {
        updateBackgroundColors();
        this.mLabelView.setTextColor(getContext().getColor(R$color.notification_section_header_label_color));
        this.mClearAllButton.setImageResource(R$drawable.op_status_bar_notification_section_header_clear_btn);
        Configuration configuration = getResources().getConfiguration();
        if (configuration != null) {
            if ((configuration.uiMode & 48) == 32) {
                this.mClearAllButton.setColorFilter(getResources().getColor(R$color.oneplus_contorl_icon_color_accent_active_dark));
            } else {
                this.mClearAllButton.setColorFilter(getResources().getColor(R$color.oneplus_contorl_icon_color_accent_active_light));
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void setAreThereDismissableGentleNotifs(boolean z) {
        this.mClearAllButton.setVisibility(z ? 0 : 8);
    }

    /* access modifiers changed from: protected */
    public boolean disallowSingleClick(MotionEvent motionEvent) {
        this.mTmpRect.set((float) this.mClearAllButton.getLeft(), (float) this.mClearAllButton.getTop(), (float) (this.mClearAllButton.getLeft() + this.mClearAllButton.getWidth()), (float) (this.mClearAllButton.getTop() + this.mClearAllButton.getHeight()));
        return this.mTmpRect.contains(motionEvent.getX(), motionEvent.getY());
    }

    /* access modifiers changed from: 0000 */
    public void setOnHeaderClickListener(OnClickListener onClickListener) {
        this.mContents.setOnClickListener(onClickListener);
    }

    /* access modifiers changed from: 0000 */
    public void setOnClearAllClickListener(OnClickListener onClickListener) {
        this.mOnClearClickListener = onClickListener;
        this.mClearAllButton.setOnClickListener(onClickListener);
    }
}
