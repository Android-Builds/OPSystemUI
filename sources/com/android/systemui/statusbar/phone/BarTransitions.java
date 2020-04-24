package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.oneplus.util.OpNavBarUtils;
import com.oneplus.util.OpUtils;

public class BarTransitions {
    private boolean mAlwaysOpaque = false;
    protected BarBackgroundDrawable mBarBackground;
    private int mMode;
    private final String mTag;
    private View mView;

    protected static class BarBackgroundDrawable extends Drawable {
        private boolean mAnimating;
        private int mColor;
        private int mColorStart;
        private Context mContext = null;
        private long mEndTime;
        private Rect mFrame;
        private final Drawable mGradient;
        private int mGradientAlpha;
        private int mGradientAlphaStart;
        /* access modifiers changed from: private */
        public int mHighlightColor;
        private int mMode = -1;
        private final int mOpaque;
        private Paint mPaint = new Paint();
        private final int mSemiTransparent;
        private long mStartTime;
        private PorterDuffColorFilter mTintFilter;
        private final int mTransparent;
        private final int mWarning;

        public int getOpacity() {
            return -3;
        }

        public void setAlpha(int i) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public BarBackgroundDrawable(Context context, int i, View view) {
            this.mContext = context;
            context.getResources();
            if (!OpNavBarUtils.isSupportCustomNavBar() || !view.getClass().getSimpleName().equals("NavigationBarView")) {
                this.mOpaque = context.getColor(R$color.system_bar_background_opaque);
                this.mSemiTransparent = context.getColor(17170985);
            } else {
                this.mOpaque = context.getColor(R$color.op_nav_bar_background_light);
                this.mSemiTransparent = context.getColor(R$color.op_nav_bar_background_transparent);
            }
            this.mTransparent = context.getColor(R$color.system_bar_background_transparent);
            this.mWarning = Utils.getColorAttrDefaultColor(context, 16844099);
            this.mGradient = context.getDrawable(i);
        }

        public void setFrame(Rect rect) {
            this.mFrame = rect;
        }

        public void setTint(int i) {
            Mode mode;
            PorterDuffColorFilter porterDuffColorFilter = this.mTintFilter;
            if (porterDuffColorFilter == null) {
                mode = Mode.SRC_IN;
            } else {
                mode = porterDuffColorFilter.getMode();
            }
            PorterDuffColorFilter porterDuffColorFilter2 = this.mTintFilter;
            if (porterDuffColorFilter2 == null || porterDuffColorFilter2.getColor() != i) {
                this.mTintFilter = new PorterDuffColorFilter(i, mode);
            }
            invalidateSelf();
        }

        public void setTintMode(Mode mode) {
            PorterDuffColorFilter porterDuffColorFilter = this.mTintFilter;
            int color = porterDuffColorFilter == null ? 0 : porterDuffColorFilter.getColor();
            PorterDuffColorFilter porterDuffColorFilter2 = this.mTintFilter;
            if (porterDuffColorFilter2 == null || porterDuffColorFilter2.getMode() != mode) {
                this.mTintFilter = new PorterDuffColorFilter(color, mode);
            }
            invalidateSelf();
        }

        /* access modifiers changed from: protected */
        public void onBoundsChange(Rect rect) {
            super.onBoundsChange(rect);
            this.mGradient.setBounds(rect);
        }

        public void applyModeBackground(int i, int i2, boolean z) {
            this.mMode = i2;
            this.mAnimating = z;
            if (z) {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                this.mStartTime = elapsedRealtime;
                this.mEndTime = elapsedRealtime + ((long) (this.mMode == 0 ? 0 : 200));
                this.mGradientAlphaStart = this.mGradientAlpha;
                this.mColorStart = this.mColor;
            }
            invalidateSelf();
        }

        public void finishAnimation() {
            if (this.mAnimating) {
                this.mAnimating = false;
                invalidateSelf();
            }
        }

        public void draw(Canvas canvas) {
            int i;
            int i2 = this.mMode;
            if (i2 == 5) {
                i = this.mWarning;
            } else if (i2 == 2) {
                i = this.mSemiTransparent;
            } else if (i2 == 1) {
                i = this.mSemiTransparent;
            } else if (i2 == 4 || i2 == 6) {
                i = this.mTransparent;
            } else if (i2 == 7) {
                i = this.mHighlightColor;
            } else if (i2 == 11) {
                i = this.mOpaque;
            } else if (!OpNavBarUtils.isSupportCustomNavBar() || this.mContext == null || !OpUtils.isScreenCompat() || WindowManagerProxy.getInstance().getDockSide() != -1) {
                if (OpNavBarUtils.isSupportCustomNavBar()) {
                    Context context = this.mContext;
                    if (context != null && OpUtils.isNeedDarkNavBar(context)) {
                        i = this.mTransparent;
                    }
                }
                i = this.mOpaque;
            } else {
                i = -16777216;
            }
            if (!this.mAnimating) {
                this.mColor = i;
                this.mGradientAlpha = 0;
            } else {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                long j = this.mEndTime;
                if (elapsedRealtime >= j) {
                    this.mAnimating = false;
                    this.mColor = i;
                    this.mGradientAlpha = 0;
                } else {
                    long j2 = this.mStartTime;
                    float max = Math.max(0.0f, Math.min(Interpolators.LINEAR.getInterpolation(((float) (elapsedRealtime - j2)) / ((float) (j - j2))), 1.0f));
                    float f = 1.0f - max;
                    this.mGradientAlpha = (int) ((((float) 0) * max) + (((float) this.mGradientAlphaStart) * f));
                    if (!OpNavBarUtils.isSupportCustomNavBar() || i != this.mTransparent) {
                        this.mColor = Color.argb((int) ((((float) Color.alpha(i)) * max) + (((float) Color.alpha(this.mColorStart)) * f)), (int) ((((float) Color.red(i)) * max) + (((float) Color.red(this.mColorStart)) * f)), (int) ((((float) Color.green(i)) * max) + (((float) Color.green(this.mColorStart)) * f)), (int) ((max * ((float) Color.blue(i))) + (((float) Color.blue(this.mColorStart)) * f)));
                    } else {
                        this.mColor = Color.argb((int) ((max * ((float) Color.alpha(i))) + (((float) Color.alpha(this.mColorStart)) * f)), Color.red(this.mColorStart), Color.green(this.mColorStart), Color.blue(this.mColorStart));
                    }
                }
            }
            int i3 = this.mGradientAlpha;
            if (i3 > 0) {
                this.mGradient.setAlpha(i3);
                this.mGradient.draw(canvas);
            }
            if (Color.alpha(this.mColor) > 0) {
                this.mPaint.setColor(this.mColor);
                PorterDuffColorFilter porterDuffColorFilter = this.mTintFilter;
                if (porterDuffColorFilter != null) {
                    this.mPaint.setColorFilter(porterDuffColorFilter);
                }
                Rect rect = this.mFrame;
                if (rect != null) {
                    canvas.drawRect(rect, this.mPaint);
                } else {
                    canvas.drawPaint(this.mPaint);
                }
            }
            if (this.mAnimating) {
                invalidateSelf();
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean isLightsOut(int i) {
        return i == 3 || i == 6;
    }

    public BarTransitions(View view, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("BarTransitions.");
        sb.append(view.getClass().getSimpleName());
        this.mTag = sb.toString();
        this.mView = view;
        this.mBarBackground = new BarBackgroundDrawable(this.mView.getContext(), i, view);
        View view2 = this.mView;
        if (view2 != null) {
            view2.setBackground(this.mBarBackground);
        }
    }

    public int getMode() {
        return this.mMode;
    }

    public boolean isAlwaysOpaque() {
        return this.mAlwaysOpaque;
    }

    public void transitionTo(int i, int i2, boolean z) {
        if (i != 7) {
            transitionTo(i, z);
            return;
        }
        int i3 = this.mMode;
        this.mMode = 7;
        BarBackgroundDrawable barBackgroundDrawable = this.mBarBackground;
        if (barBackgroundDrawable != null) {
            barBackgroundDrawable.mHighlightColor = i2;
        }
        onTransition(i3, this.mMode, z);
    }

    public void transitionTo(int i, boolean z) {
        if (isAlwaysOpaque() && (i == 1 || i == 2 || i == 4)) {
            i = 0;
        }
        if (isAlwaysOpaque() && i == 6) {
            i = 3;
        }
        int i2 = this.mMode;
        if (i2 != i) {
            this.mMode = i;
            if (Build.DEBUG_ONEPLUS) {
                Log.d(this.mTag, String.format("%s -> %s animate=%s", new Object[]{modeToString(i2), modeToString(i), Boolean.valueOf(z)}));
            }
            onTransition(i2, this.mMode, z);
        }
    }

    /* access modifiers changed from: protected */
    public void onTransition(int i, int i2, boolean z) {
        applyModeBackground(i, i2, z);
    }

    /* access modifiers changed from: protected */
    public void applyModeBackground(int i, int i2, boolean z) {
        if (Build.DEBUG_ONEPLUS) {
            Log.d(this.mTag, String.format("applyModeBackground oldMode=%s newMode=%s animate=%s", new Object[]{modeToString(i), modeToString(i2), Boolean.valueOf(z)}));
        }
        BarBackgroundDrawable barBackgroundDrawable = this.mBarBackground;
        if (barBackgroundDrawable != null) {
            barBackgroundDrawable.applyModeBackground(i, i2, z);
        }
    }

    public static String modeToString(int i) {
        if (i == 0) {
            return "MODE_OPAQUE";
        }
        if (i == 1) {
            return "MODE_SEMI_TRANSPARENT";
        }
        if (i == 2) {
            return "MODE_TRANSLUCENT";
        }
        if (i == 3) {
            return "MODE_LIGHTS_OUT";
        }
        if (i == 4) {
            return "MODE_TRANSPARENT";
        }
        if (i == 5) {
            return "MODE_WARNING";
        }
        if (i == 6) {
            return "MODE_LIGHTS_OUT_TRANSPARENT";
        }
        if (i == 7) {
            return "MODE_HIGHLIGHT_HINT";
        }
        if (i == 11) {
            return "MODE_NOTCH_IGNORE";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("modeToString Unknown mode ");
        sb.append(i);
        Log.w("BarTransitions", sb.toString());
        return "MODE_UNKNOWN";
    }

    public void finishAnimations() {
        BarBackgroundDrawable barBackgroundDrawable = this.mBarBackground;
        if (barBackgroundDrawable != null) {
            barBackgroundDrawable.finishAnimation();
        }
    }
}
