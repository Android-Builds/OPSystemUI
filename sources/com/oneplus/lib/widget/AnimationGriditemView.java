package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$integer;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.AnimatorUtils;
import com.oneplus.lib.widget.button.OPCheckBox;

public class AnimationGriditemView extends FrameLayout {
    private static final int ANIMATION_DURATION_RES = R$integer.oneplus_contorl_time_part5;
    private static final int RADIUS_RES = R$dimen.oneplus_contorl_radius_r12;
    private static final RadiusMode[] sRadiusModeTypeArray = {RadiusMode.NONE, RadiusMode.RADIUS};
    private static final ScaleType[] sScaleTypeArray = {ScaleType.MATRIX, ScaleType.FIT_XY, ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.CENTER, ScaleType.CENTER_CROP, ScaleType.CENTER_INSIDE};
    private OPCheckBox mCheckBox;
    private int mDuration;
    private ImageView mImage;
    private Interpolator mInterpolator = AnimatorUtils.GRID_ITEM_ANIMATION_INTERPOLATOR;
    private int mRadius;
    private RadiusMode mRadiusMode = RadiusMode.NONE;
    private float mZoomOffset = 0.7f;

    public enum RadiusMode {
        NONE(0),
        RADIUS(1);
        
        final int nativeInt;

        private RadiusMode(int i) {
            this.nativeInt = i;
        }
    }

    private static class RoundRectOutlineProvider extends ViewOutlineProvider {
        private int mRadius;

        public RoundRectOutlineProvider(int i) {
            this.mRadius = i;
        }

        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), (float) this.mRadius);
        }
    }

    public AnimationGriditemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(attributeSet);
    }

    private void init(AttributeSet attributeSet) {
        LayoutInflater.from(getContext()).inflate(R$layout.op_animation_grid_list_item, this, true);
        this.mImage = (ImageView) findViewById(R$id.grid_item_img);
        this.mCheckBox = (OPCheckBox) findViewById(R$id.grid_item_checkbox);
        this.mRadius = getResources().getDimensionPixelOffset(RADIUS_RES);
        this.mDuration = getResources().getInteger(ANIMATION_DURATION_RES);
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R$styleable.AnimationGridItemView, 0, 0);
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.AnimationGridItemView_android_src);
        if (drawable != null) {
            setImageDrawable(drawable);
        }
        int i = obtainStyledAttributes.getInt(R$styleable.AnimationGridItemView_android_scaleType, 1);
        if (i >= 0) {
            this.mImage.setScaleType(sScaleTypeArray[i]);
        }
        int i2 = obtainStyledAttributes.getInt(R$styleable.AnimationGridItemView_radiusMode, -1);
        if (i2 >= 0) {
            setRadiusMode(sRadiusModeTypeArray[i2]);
        }
        obtainStyledAttributes.recycle();
    }

    public void setImageDrawable(Drawable drawable) {
        this.mImage.setImageDrawable(drawable);
    }

    public void setRadiusMode(RadiusMode radiusMode) {
        if (this.mRadiusMode != radiusMode) {
            this.mRadiusMode = radiusMode;
            scheduleRadiusChange();
        }
    }

    private void scheduleRadiusChange() {
        if (this.mRadiusMode == RadiusMode.RADIUS) {
            setOutlineProvider(new RoundRectOutlineProvider(this.mRadius));
            setClipToOutline(true);
            this.mImage.setOutlineProvider(new RoundRectOutlineProvider(this.mRadius));
            this.mImage.setClipToOutline(true);
        }
    }
}
