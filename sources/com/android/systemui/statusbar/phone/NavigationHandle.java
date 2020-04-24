package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout.LayoutParams;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.oneplus.systemui.statusbar.phone.OpNavigationHandle;

public class NavigationHandle extends OpNavigationHandle implements ButtonInterface {
    private int mBottom;
    private int mBottomLand;
    private Context mContext;
    private final int mDarkColor;
    private float mDarkIntensity;
    private boolean mIsVertical;
    private int mLandscapeWidth;
    private final int mLightColor;
    private final Paint mPaint;
    private int mPortraitWidth;
    private int mRadius;

    public void abortCurrentGesture() {
    }

    public void setDelayTouchFeedback(boolean z) {
    }

    public void setImageDrawable(Drawable drawable) {
    }

    public NavigationHandle(Context context) {
        this(context, null);
    }

    public NavigationHandle(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDarkIntensity = -1.0f;
        this.mPaint = new Paint();
        this.mIsVertical = true;
        this.mContext = context;
        Resources resources = context.getResources();
        this.mRadius = resources.getDimensionPixelSize(R$dimen.navigation_handle_radius);
        this.mBottom = resources.getDimensionPixelSize(17105288);
        this.mBottomLand = resources.getDimensionPixelSize(R$dimen.navigation_handle_bottom);
        this.mPortraitWidth = resources.getDimensionPixelSize(R$dimen.navigation_home_handle_width);
        this.mLandscapeWidth = resources.getDimensionPixelSize(R$dimen.navigation_home_handle_width_land);
        this.mLightColor = resources.getColor(R$color.op_home_handle_light_color);
        this.mDarkColor = resources.getColor(R$color.op_home_handle_dark_color);
        this.mPaint.setAntiAlias(true);
        setFocusable(false);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int i = this.mRadius * 3;
        float f = ((float) i) / 2.0f;
        int width = getWidth();
        int i2 = this.mIsVertical ? height - ((this.mBottom + i) / 2) : (height - this.mBottomLand) - i;
        canvas.drawRoundRect(0.0f, (float) i2, (float) width, (float) (i2 + i), f, f, this.mPaint);
    }

    public void setVertical(boolean z) {
        updateDisplaySize();
        this.mIsVertical = z;
        LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        layoutParams.width = this.mIsVertical ? this.mPortraitWidth : this.mLandscapeWidth;
        setLayoutParams(layoutParams);
    }

    public void setDarkIntensity(float f) {
        if (this.mDarkIntensity != f) {
            this.mPaint.setColor(((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(this.mLightColor), Integer.valueOf(this.mDarkColor))).intValue());
            this.mDarkIntensity = f;
            invalidate();
        }
    }

    private void updateDisplaySize() {
        this.mRadius = this.mContext.getResources().getDimensionPixelSize(R$dimen.navigation_handle_radius);
        this.mBottom = this.mContext.getResources().getDimensionPixelSize(17105288);
        this.mBottomLand = this.mContext.getResources().getDimensionPixelSize(R$dimen.navigation_handle_bottom);
        this.mPortraitWidth = this.mContext.getResources().getDimensionPixelSize(R$dimen.navigation_home_handle_width);
        this.mLandscapeWidth = this.mContext.getResources().getDimensionPixelSize(R$dimen.navigation_home_handle_width_land);
    }
}
