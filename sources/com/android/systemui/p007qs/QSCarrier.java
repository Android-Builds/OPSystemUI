package com.android.systemui.p007qs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.oneplus.util.ThemeColorUtils;

/* renamed from: com.android.systemui.qs.QSCarrier */
public class QSCarrier extends LinearLayout {
    private TextView mCarrierText;
    private float mColorForegroundIntensity;
    private ColorStateList mColorForegroundStateList;
    private DualToneHandler mDualToneHandler;
    private View mMobileGroup;
    private ImageView mMobileRoaming;
    private ImageView mMobileSignal;

    public QSCarrier(Context context) {
        super(context);
    }

    public QSCarrier(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public QSCarrier(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public QSCarrier(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDualToneHandler = new DualToneHandler(getContext());
        this.mMobileGroup = findViewById(R$id.mobile_combo);
        this.mMobileSignal = (ImageView) findViewById(R$id.mobile_signal);
        this.mMobileRoaming = (ImageView) findViewById(R$id.mobile_roaming);
        this.mCarrierText = (TextView) findViewById(R$id.qs_carrier_text);
        int colorAttrDefaultColor = Utils.getColorAttrDefaultColor(this.mContext, 16842800);
        this.mColorForegroundStateList = ColorStateList.valueOf(colorAttrDefaultColor);
        this.mColorForegroundIntensity = QuickStatusBarHeader.getColorIntensity(colorAttrDefaultColor);
        this.mCarrierText.setTextColor(ColorStateList.valueOf(ThemeColorUtils.getColor(ThemeColorUtils.QS_PRIMARY_TEXT)));
    }

    public void updateState(CellSignalState cellSignalState) {
        int i = 8;
        this.mMobileGroup.setVisibility(8);
        if (cellSignalState.visible) {
            ImageView imageView = this.mMobileRoaming;
            if (cellSignalState.roaming) {
                i = 0;
            }
            imageView.setVisibility(i);
            ColorStateList valueOf = ColorStateList.valueOf(this.mDualToneHandler.getSingleColor(this.mColorForegroundIntensity));
            this.mMobileRoaming.setImageTintList(valueOf);
            this.mMobileSignal.setImageDrawable(new SignalDrawable(this.mContext));
            this.mMobileSignal.setImageTintList(valueOf);
            this.mMobileSignal.setImageLevel(cellSignalState.mobileSignalIconId);
            StringBuilder sb = new StringBuilder();
            String str = cellSignalState.contentDescription;
            String str2 = ", ";
            if (str != null) {
                sb.append(str);
                sb.append(str2);
            }
            if (cellSignalState.roaming) {
                sb.append(this.mContext.getString(R$string.data_connection_roaming));
                sb.append(str2);
            }
            if (hasValidTypeContentDescription(cellSignalState.typeContentDescription)) {
                sb.append(cellSignalState.typeContentDescription);
            }
            this.mMobileSignal.setContentDescription(sb);
        }
    }

    private boolean hasValidTypeContentDescription(String str) {
        return TextUtils.equals(str, this.mContext.getString(R$string.data_connection_no_internet)) || TextUtils.equals(str, this.mContext.getString(R$string.cell_data_off_content_description)) || TextUtils.equals(str, this.mContext.getString(R$string.not_default_data_content_description));
    }

    public void setCarrierText(CharSequence charSequence) {
        this.mCarrierText.setText(charSequence);
    }
}
