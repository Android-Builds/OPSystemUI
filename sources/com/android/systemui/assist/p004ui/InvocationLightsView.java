package com.android.systemui.assist.p004ui;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R$attr;
import com.android.systemui.assist.p004ui.PerimeterPathGuide.Region;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarTransitions.DarkIntensityListener;
import java.util.ArrayList;
import java.util.Iterator;

/* renamed from: com.android.systemui.assist.ui.InvocationLightsView */
public class InvocationLightsView extends View implements DarkIntensityListener {
    protected final ArrayList<EdgeLight> mAssistInvocationLights;
    private final int mDarkColor;
    protected final PerimeterPathGuide mGuide;
    private final int mLightColor;
    private final Paint mPaint;
    private final Path mPath;
    private boolean mRegistered;
    private int[] mScreenLocation;
    private final int mStrokeWidth;
    private boolean mUseNavBarColor;
    private final int mViewHeight;

    public InvocationLightsView(Context context) {
        this(context, null);
    }

    public InvocationLightsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public InvocationLightsView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public InvocationLightsView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mAssistInvocationLights = new ArrayList<>();
        this.mPaint = new Paint();
        this.mPath = new Path();
        this.mScreenLocation = new int[2];
        this.mRegistered = false;
        this.mUseNavBarColor = true;
        this.mStrokeWidth = DisplayUtils.convertDpToPx(3.0f, context);
        this.mPaint.setStrokeWidth((float) this.mStrokeWidth);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setStrokeJoin(Join.MITER);
        this.mPaint.setAntiAlias(true);
        Context context2 = context;
        PerimeterPathGuide perimeterPathGuide = new PerimeterPathGuide(context2, createCornerPathRenderer(context), this.mStrokeWidth / 2, DisplayUtils.getWidth(context), DisplayUtils.getHeight(context));
        this.mGuide = perimeterPathGuide;
        this.mViewHeight = Math.max(DisplayUtils.getCornerRadiusBottom(context), DisplayUtils.getCornerRadiusTop(context));
        int themeAttr = Utils.getThemeAttr(this.mContext, R$attr.darkIconTheme);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mContext, Utils.getThemeAttr(this.mContext, R$attr.lightIconTheme));
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(this.mContext, themeAttr);
        this.mLightColor = Utils.getColorAttrDefaultColor(contextThemeWrapper, R$attr.singleToneColor);
        this.mDarkColor = Utils.getColorAttrDefaultColor(contextThemeWrapper2, R$attr.singleToneColor);
        for (int i3 = 0; i3 < 4; i3++) {
            this.mAssistInvocationLights.add(new EdgeLight(0, 0.0f, 0.0f));
        }
    }

    public void onInvocationProgress(float f) {
        if (f == 0.0f) {
            setVisibility(8);
        } else {
            attemptRegisterNavBarListener();
            float regionWidth = this.mGuide.getRegionWidth(Region.BOTTOM_LEFT);
            float f2 = 0.6f * regionWidth;
            float f3 = (regionWidth - f2) / 2.0f;
            float lerp = MathUtils.lerp(f2 / 2.0f, this.mGuide.getRegionWidth(Region.BOTTOM) / 4.0f, f);
            float f4 = 1.0f - f;
            float f5 = ((-regionWidth) + f3) * f4;
            float regionWidth2 = this.mGuide.getRegionWidth(Region.BOTTOM) + ((regionWidth - f3) * f4);
            setLight(0, f5, lerp);
            setLight(1, f5 + lerp, lerp);
            setLight(2, regionWidth2 - (2.0f * lerp), lerp);
            setLight(3, regionWidth2 - lerp, lerp);
            setVisibility(0);
        }
        invalidate();
    }

    public void hide() {
        setVisibility(8);
        Iterator it = this.mAssistInvocationLights.iterator();
        while (it.hasNext()) {
            ((EdgeLight) it.next()).setLength(0.0f);
        }
        attemptUnregisterNavBarListener();
    }

    public void onDarkIntensity(float f) {
        updateDarkness(f);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        getLayoutParams().height = this.mViewHeight;
        requestLayout();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mGuide.setRotation(getContext().getDisplay().getRotation());
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        getLocationOnScreen(this.mScreenLocation);
        int[] iArr = this.mScreenLocation;
        canvas.translate((float) (-iArr[0]), (float) (-iArr[1]));
        if (this.mUseNavBarColor) {
            Iterator it = this.mAssistInvocationLights.iterator();
            while (it.hasNext()) {
                renderLight((EdgeLight) it.next(), canvas);
            }
            return;
        }
        this.mPaint.setStrokeCap(Cap.ROUND);
        renderLight((EdgeLight) this.mAssistInvocationLights.get(0), canvas);
        renderLight((EdgeLight) this.mAssistInvocationLights.get(3), canvas);
        this.mPaint.setStrokeCap(Cap.BUTT);
        renderLight((EdgeLight) this.mAssistInvocationLights.get(1), canvas);
        renderLight((EdgeLight) this.mAssistInvocationLights.get(2), canvas);
    }

    /* access modifiers changed from: protected */
    public void setLight(int i, float f, float f2) {
        if (i < 0 || i >= 4) {
            StringBuilder sb = new StringBuilder();
            sb.append("invalid invocation light index: ");
            sb.append(i);
            Log.w("InvocationLightsView", sb.toString());
        }
        ((EdgeLight) this.mAssistInvocationLights.get(i)).setOffset(f);
        ((EdgeLight) this.mAssistInvocationLights.get(i)).setLength(f2);
    }

    /* access modifiers changed from: protected */
    public CornerPathRenderer createCornerPathRenderer(Context context) {
        return new CircularCornerPathRenderer(DisplayUtils.getCornerRadiusBottom(context), DisplayUtils.getCornerRadiusTop(context), DisplayUtils.getWidth(context), DisplayUtils.getHeight(context));
    }

    /* access modifiers changed from: protected */
    public void updateDarkness(float f) {
        if (this.mUseNavBarColor) {
            int intValue = ((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(this.mLightColor), Integer.valueOf(this.mDarkColor))).intValue();
            Iterator it = this.mAssistInvocationLights.iterator();
            while (it.hasNext()) {
                ((EdgeLight) it.next()).setColor(intValue);
            }
            invalidate();
        }
    }

    private void renderLight(EdgeLight edgeLight, Canvas canvas) {
        this.mGuide.strokeSegment(this.mPath, edgeLight.getOffset(), edgeLight.getOffset() + edgeLight.getLength());
        this.mPaint.setColor(edgeLight.getColor());
        canvas.drawPath(this.mPath, this.mPaint);
    }

    private void attemptRegisterNavBarListener() {
        if (!this.mRegistered) {
            NavigationBarController navigationBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
            if (navigationBarController != null) {
                NavigationBarFragment defaultNavigationBarFragment = navigationBarController.getDefaultNavigationBarFragment();
                if (defaultNavigationBarFragment != null) {
                    updateDarkness(defaultNavigationBarFragment.getBarTransitions().addDarkIntensityListener(this));
                    this.mRegistered = true;
                }
            }
        }
    }

    private void attemptUnregisterNavBarListener() {
        if (this.mRegistered) {
            NavigationBarController navigationBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
            if (navigationBarController != null) {
                NavigationBarFragment defaultNavigationBarFragment = navigationBarController.getDefaultNavigationBarFragment();
                if (defaultNavigationBarFragment != null) {
                    defaultNavigationBarFragment.getBarTransitions().removeDarkIntensityListener(this);
                    this.mRegistered = false;
                }
            }
        }
    }
}
