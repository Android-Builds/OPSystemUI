package com.oneplus.aod;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$string;

public class OpAodThreeKeyStatusView extends LinearLayout {
    public static int MODE_NONE = 0;
    public static int MODE_SILENCE = 1;
    public static int MODE_VIBRATE = 2;
    private String TAG = "AodThreeKeyStatusView";
    private ImageView mIcon;
    private TextView mTextView;

    public OpAodThreeKeyStatusView(Context context) {
        super(context);
    }

    public OpAodThreeKeyStatusView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OpAodThreeKeyStatusView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public OpAodThreeKeyStatusView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(R$id.three_key_icon);
        this.mTextView = (TextView) findViewById(R$id.three_key_text);
    }

    public void onThreeKeyChanged(int i) {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("mode = ");
        sb.append(i);
        Log.d(str, sb.toString());
        if (i != MODE_NONE) {
            ImageView imageView = this.mIcon;
            if (imageView != null) {
                if (i == MODE_SILENCE) {
                    imageView.setImageResource(R$drawable.aod_stat_sys_three_key_silent);
                } else if (i == MODE_VIBRATE) {
                    imageView.setImageResource(R$drawable.aod_stat_sys_ringer_vibrate);
                } else {
                    imageView.setImageResource(R$drawable.aod_stat_sys_three_key_normal);
                }
            }
            TextView textView = this.mTextView;
            if (textView != null) {
                if (i == MODE_SILENCE) {
                    textView.setText(R$string.volume_footer_slient);
                } else if (i == MODE_VIBRATE) {
                    textView.setText(R$string.volume_vibrate);
                } else {
                    textView.setText(R$string.volume_footer_ring);
                }
            }
        }
    }
}
