package com.oneplus.plugin;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$string;

public class OpPreventModeView extends RelativeLayout {
    private final String TAG = "OpPreventModeView";
    private Configuration mConfig;
    private Context mContext;
    private LinearLayout mInnerView;
    int mOrientatin = 1;
    private ImageView mPhone;
    private OpRippleView mRippleView;
    private View mScrim;
    private TextView mTag;
    private TextView mTag2;
    private TextView mTagNum1;
    private TextView mTagNum2;
    private TextView mTitle;
    private TextView mTitleCancel;

    private void relayout() {
    }

    public OpPreventModeView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
    }

    public OpPreventModeView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    public OpPreventModeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public OpPreventModeView(Context context) {
        super(context);
        this.mContext = context;
    }

    public void onConfigurationChanged(Configuration configuration) {
        this.mTitle.setText(R$string.prevent_view_title);
        this.mTitleCancel.setText(R$string.prevent_view_title_cancel);
        this.mTag.setText(R$string.prevent_view_top_tag_cancel);
        this.mTag2.setText(R$string.prevent_view_top_tag_cancel2);
        this.mConfig = new Configuration(configuration);
        int i = configuration.orientation;
        if (i != this.mOrientatin) {
            this.mOrientatin = i;
            relayout();
        }
    }

    public void init() {
        this.mInnerView = (LinearLayout) findViewById(R$id.prevent_mode_inner_view);
        this.mTitle = (TextView) findViewById(R$id.prevent_view_title);
        this.mPhone = (ImageView) findViewById(R$id.prevent_mode_phone);
        this.mTitleCancel = (TextView) findViewById(R$id.prevent_view_title_cancel);
        this.mTag = (TextView) findViewById(R$id.prevent_view_top_tag_cancel);
        this.mTag2 = (TextView) findViewById(R$id.prevent_view_top_tag_cancel2);
        this.mTagNum1 = (TextView) findViewById(R$id.prevent_view_top_tag_number1);
        this.mTagNum2 = (TextView) findViewById(R$id.prevent_view_top_tag_number2);
        this.mRippleView = (OpRippleView) findViewById(R$id.rippleview_first);
        this.mConfig = new Configuration(this.mContext.getResources().getConfiguration());
        this.mScrim = findViewById(R$id.scrim_view);
        this.mPhone.setImageResource(R$drawable.prevent_mode_img);
        relayout();
    }

    private void playRippleAniamor() {
        this.mRippleView.prepare();
        this.mRippleView.startRipple();
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (this.mTitleCancel != null && this.mTitle != null && this.mTag != null) {
            if (i != 0) {
                this.mRippleView.stopRipple();
            } else {
                playRippleAniamor();
            }
        }
    }
}
