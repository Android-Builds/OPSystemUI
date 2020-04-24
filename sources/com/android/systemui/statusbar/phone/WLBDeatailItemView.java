package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.oneplus.util.ThemeColorUtils;

public class WLBDeatailItemView extends LinearLayout {
    protected static int layoutResId = R$layout.wlb_detail_item_view;
    private ImageView mAvatar;
    private TextView mName;
    private TextView mTriggerName;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public WLBDeatailItemView(Context context) {
        super(context);
    }

    public WLBDeatailItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WLBDeatailItemView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public WLBDeatailItemView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void bind(String str, Drawable drawable, String str2) {
        this.mName.setText(str);
        this.mAvatar.setImageDrawable(drawable);
        this.mTriggerName.setText(str2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mAvatar = (ImageView) findViewById(R$id.user_picture);
        this.mName = (TextView) findViewById(R$id.user_name);
        this.mTriggerName = (TextView) findViewById(R$id.trigger_name);
    }

    public void updateThemeColor(boolean z) {
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_PRIMARY_TEXT);
        int color2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_SECONDARY_TEXT);
        this.mName.setTextColor(color);
        if (z) {
            this.mTriggerName.setTextColor(color);
        } else {
            this.mTriggerName.setTextColor(color2);
        }
    }

    public void setActivated(boolean z) {
        super.setActivated(z);
    }

    public static WLBDeatailItemView convertOrInflate(Context context, View view, ViewGroup viewGroup) {
        if (!(view instanceof WLBDeatailItemView)) {
            view = LayoutInflater.from(context).inflate(layoutResId, viewGroup, false);
        }
        return (WLBDeatailItemView) view;
    }
}
