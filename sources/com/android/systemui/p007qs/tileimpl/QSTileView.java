package com.android.systemui.p007qs.tileimpl;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile.State;
import com.oneplus.util.ThemeColorUtils;
import java.util.Objects;

/* renamed from: com.android.systemui.qs.tileimpl.QSTileView */
public class QSTileView extends QSTileBaseView {
    protected int mColor;
    private ColorStateList mColorLabelDefault;
    private ColorStateList mColorLabelUnavailable;
    private View mDivider;
    private View mExpandIndicator;
    private View mExpandSpace;
    protected TextView mLabel;
    private ViewGroup mLabelContainer;
    private ImageView mPadLock;
    protected TextView mSecondLine;
    private int mState;

    public QSTileView(Context context, QSIconView qSIconView) {
        this(context, qSIconView, false);
    }

    public QSTileView(Context context, QSIconView qSIconView, boolean z) {
        super(context, qSIconView, z);
        this.mColor = 0;
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setId(View.generateViewId());
        createLabel();
        setOrientation(1);
        setGravity(49);
        this.mColorLabelDefault = Utils.getColorAttr(getContext(), 16842806);
        this.mColorLabelUnavailable = Utils.getColorAttr(getContext(), 16842808);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mLabel, R$dimen.qs_tile_text_size);
        FontSizeUtils.updateFontSize(this.mSecondLine, R$dimen.qs_tile_text_size);
    }

    public int getDetailY() {
        return getTop() + this.mLabelContainer.getTop() + (this.mLabelContainer.getHeight() / 2);
    }

    /* access modifiers changed from: protected */
    public void createLabel() {
        this.mLabelContainer = (ViewGroup) LayoutInflater.from(getContext()).inflate(R$layout.qs_tile_label, this, false);
        this.mLabelContainer.setClipChildren(false);
        this.mLabelContainer.setClipToPadding(false);
        this.mLabel = (TextView) this.mLabelContainer.findViewById(R$id.tile_label);
        this.mPadLock = (ImageView) this.mLabelContainer.findViewById(R$id.restricted_padlock);
        this.mDivider = this.mLabelContainer.findViewById(R$id.underline);
        this.mExpandIndicator = this.mLabelContainer.findViewById(R$id.expand_indicator);
        this.mExpandSpace = this.mLabelContainer.findViewById(R$id.expand_space);
        this.mSecondLine = (TextView) this.mLabelContainer.findViewById(R$id.app_label);
        addView(this.mLabelContainer);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mLabel.getLineCount() > 2 || (!TextUtils.isEmpty(this.mSecondLine.getText()) && this.mSecondLine.getLineHeight() > this.mSecondLine.getHeight())) {
            this.mLabel.setSingleLine();
            super.onMeasure(i, i2);
        }
    }

    /* access modifiers changed from: protected */
    public void handleStateChanged(State state) {
        super.handleStateChanged(state);
        if (!Objects.equals(this.mLabel.getText(), state.label) || this.mState != state.state) {
            this.mLabel.setTextColor(QSTileImpl.getOPColorForState(state.state));
            this.mState = state.state;
            this.mLabel.setText(state.label);
        }
        int i = 0;
        if (!Objects.equals(this.mSecondLine.getText(), state.secondaryLabel)) {
            this.mSecondLine.setText(state.secondaryLabel);
            this.mSecondLine.setVisibility(TextUtils.isEmpty(state.secondaryLabel) ? 8 : 0);
        }
        this.mExpandIndicator.setVisibility(8);
        this.mExpandSpace.setVisibility(8);
        this.mLabelContainer.setContentDescription(null);
        if (this.mLabelContainer.isClickable()) {
            this.mLabelContainer.setClickable(false);
            this.mLabelContainer.setLongClickable(false);
            this.mLabelContainer.setBackground(null);
        }
        this.mLabel.setEnabled(!state.disabledByPolicy);
        ImageView imageView = this.mPadLock;
        if (!state.disabledByPolicy) {
            i = 8;
        }
        imageView.setVisibility(i);
        updateThemeColor(state);
    }

    private void updateThemeColor(State state) {
        this.mColor = ThemeColorUtils.getColor(ThemeColorUtils.QS_PRIMARY_TEXT);
        this.mLabel.setTextColor(this.mColor);
        this.mSecondLine.setTextColor(ThemeColorUtils.getColor(ThemeColorUtils.QS_SECONDARY_TEXT));
    }

    public void init(OnClickListener onClickListener, OnClickListener onClickListener2, OnLongClickListener onLongClickListener) {
        super.init(onClickListener, onClickListener2, onLongClickListener);
        this.mLabelContainer.setOnClickListener(onClickListener2);
        this.mLabelContainer.setOnLongClickListener(onLongClickListener);
        this.mLabelContainer.setClickable(false);
        this.mLabelContainer.setLongClickable(false);
    }
}
