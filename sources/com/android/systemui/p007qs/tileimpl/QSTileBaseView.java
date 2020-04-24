package com.android.systemui.p007qs.tileimpl;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.PathParser;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Switch;
import com.android.settingslib.Utils;
import com.android.systemui.R$dimen;
import com.android.systemui.R$string;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.State;
import com.android.systemui.plugins.p006qs.QSTileView;
import com.oneplus.util.OpLottieUtils;

/* renamed from: com.android.systemui.qs.tileimpl.QSTileBaseView */
public class QSTileBaseView extends QSTileView {
    private String mAccessibilityClass;
    private final ImageView mBg;
    private int mCircleColor;
    private boolean mClicked;
    private boolean mCollapsedView;
    private final int mColorActive;
    private final int mColorDisabled;
    private final int mColorInactive;
    private final C1026H mHandler = new C1026H();
    protected QSIconView mIcon;
    private final FrameLayout mIconFrame;
    private final int[] mLocInScreen = new int[2];
    private OpLottieUtils mLottieUtils;
    protected RippleDrawable mRipple = null;
    private boolean mShowRippleEffect = true;
    private Drawable mTileBackground;
    private boolean mTileState;

    /* renamed from: com.android.systemui.qs.tileimpl.QSTileBaseView$H */
    private class C1026H extends Handler {
        public C1026H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            if (message.what == 1) {
                QSTileBaseView.this.handleStateChanged((State) message.obj);
            }
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public QSTileBaseView(Context context, QSIconView qSIconView, boolean z) {
        super(context);
        context.getResources().getDimensionPixelSize(R$dimen.qs_quick_tile_padding);
        this.mIconFrame = new FrameLayout(context);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R$dimen.qs_quick_tile_size);
        addView(this.mIconFrame, new LayoutParams(dimensionPixelSize, dimensionPixelSize));
        this.mBg = new ImageView(getContext());
        ShapeDrawable shapeDrawable = new ShapeDrawable(new PathShape(new Path(PathParser.createPathFromPathData(context.getResources().getString(17039752))), 100.0f, 100.0f));
        shapeDrawable.setTintList(ColorStateList.valueOf(0));
        int dimensionPixelSize2 = context.getResources().getDimensionPixelSize(R$dimen.qs_tile_background_size);
        shapeDrawable.setIntrinsicHeight(dimensionPixelSize2);
        shapeDrawable.setIntrinsicWidth(dimensionPixelSize2);
        this.mBg.setImageDrawable(shapeDrawable);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dimensionPixelSize2, dimensionPixelSize2, 17);
        this.mIconFrame.addView(this.mBg, layoutParams);
        this.mBg.setLayoutParams(layoutParams);
        this.mIcon = qSIconView;
        this.mIconFrame.addView(this.mIcon, new FrameLayout.LayoutParams(-2, -2, 17));
        this.mIconFrame.setClipChildren(false);
        this.mIconFrame.setClipToPadding(false);
        this.mTileBackground = newTileBackground();
        Drawable drawable = this.mTileBackground;
        if (drawable instanceof RippleDrawable) {
            setRipple((RippleDrawable) drawable);
        }
        setImportantForAccessibility(1);
        setBackground(this.mTileBackground);
        this.mColorActive = Utils.getColorAttrDefaultColor(context, 16843829);
        this.mColorDisabled = Utils.getDisabled(context, Utils.getColorAttrDefaultColor(context, 16843282));
        this.mColorInactive = Utils.getColorAttrDefaultColor(context, 16842808);
        setPadding(0, 0, 0, 0);
        setClipChildren(false);
        setClipToPadding(false);
        this.mCollapsedView = z;
        setFocusable(true);
        if (this.mLottieUtils == null) {
            this.mLottieUtils = new OpLottieUtils(getContext(), this.mBg, this.mIcon);
        }
    }

    /* access modifiers changed from: protected */
    public Drawable newTileBackground() {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{16843868});
        Drawable drawable = obtainStyledAttributes.getDrawable(0);
        obtainStyledAttributes.recycle();
        return drawable;
    }

    private void setRipple(RippleDrawable rippleDrawable) {
        this.mRipple = rippleDrawable;
        if (getWidth() != 0) {
            updateRippleSize();
        }
    }

    private void updateRippleSize() {
        int measuredWidth = (this.mIconFrame.getMeasuredWidth() / 2) + this.mIconFrame.getLeft();
        int measuredHeight = (this.mIconFrame.getMeasuredHeight() / 2) + this.mIconFrame.getTop();
        int height = (int) (((float) this.mIcon.getHeight()) * 0.85f);
        this.mRipple.setHotspotBounds(measuredWidth - height, measuredHeight - height, measuredWidth + height, measuredHeight + height);
    }

    public void init(QSTile qSTile) {
        init(new OnClickListener() {
            public final void onClick(View view) {
                QSTile.this.click();
            }
        }, new OnClickListener() {
            public final void onClick(View view) {
                QSTile.this.secondaryClick();
            }
        }, new OnLongClickListener() {
            public final boolean onLongClick(View view) {
                return QSTile.this.longClick();
            }
        });
    }

    public void init(OnClickListener onClickListener, OnClickListener onClickListener2, OnLongClickListener onLongClickListener) {
        setOnClickListener(onClickListener);
        setOnLongClickListener(onLongClickListener);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mRipple != null) {
            updateRippleSize();
        }
    }

    public View updateAccessibilityOrder(View view) {
        setAccessibilityTraversalAfter(view.getId());
        return this;
    }

    public void onStateChanged(State state) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void handleStateChanged(State state) {
        int circleColor = getCircleColor(state.state);
        boolean animationsEnabled = animationsEnabled();
        ColorStateList imageTintList = this.mBg.getImageTintList();
        int defaultColor = imageTintList != null ? imageTintList.getDefaultColor() : 0;
        boolean z = true;
        if (circleColor != defaultColor) {
            if (!animationsEnabled) {
                QSIconViewImpl.setTint(this.mBg, circleColor);
            } else if (!this.mLottieUtils.applyLottieAnimIfNeeded(this.mIconFrame, state, animationsEnabled)) {
                ValueAnimator duration = ValueAnimator.ofArgb(new int[]{this.mCircleColor, circleColor}).setDuration(0);
                duration.addUpdateListener(new AnimatorUpdateListener() {
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        QSTileBaseView.this.lambda$handleStateChanged$3$QSTileBaseView(valueAnimator);
                    }
                });
                duration.start();
            }
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleStateChanged: label=");
            sb.append(state.label);
            sb.append(", iconRes=");
            sb.append(state.icon);
            sb.append(", cirColor=");
            sb.append(Integer.toHexString(defaultColor));
            sb.append("->");
            sb.append(Integer.toHexString(circleColor));
            Log.d("QSTileBaseView", sb.toString());
        }
        this.mShowRippleEffect = state.showRippleEffect;
        if (state.state == 0) {
            z = false;
        }
        setClickable(z);
        setLongClickable(state.handlesLongClick);
        if (!this.mLottieUtils.isNeedLottie(state)) {
            this.mIcon.setIcon(state, animationsEnabled);
        }
        setContentDescription(state.contentDescription);
        this.mAccessibilityClass = state.state == 0 ? null : state.expandedAccessibilityClassName;
        if (state instanceof BooleanState) {
            boolean z2 = ((BooleanState) state).value;
            if (this.mTileState != z2) {
                this.mClicked = false;
                this.mTileState = z2;
            }
        }
    }

    public /* synthetic */ void lambda$handleStateChanged$3$QSTileBaseView(ValueAnimator valueAnimator) {
        this.mBg.setImageTintList(ColorStateList.valueOf(((Integer) valueAnimator.getAnimatedValue()).intValue()));
    }

    /* access modifiers changed from: protected */
    public boolean animationsEnabled() {
        boolean z = false;
        if (!isShown() || getAlpha() != 1.0f) {
            return false;
        }
        getLocationOnScreen(this.mLocInScreen);
        if (this.mLocInScreen[1] >= (-getHeight())) {
            z = true;
        }
        return z;
    }

    private int getCircleColor(int i) {
        return QSTileImpl.getCircleColorForState(i);
    }

    public void setClickable(boolean z) {
        super.setClickable(z);
        setBackground((!z || !this.mShowRippleEffect) ? null : this.mRipple);
    }

    public int getDetailY() {
        return getTop() + (getHeight() / 2);
    }

    public QSIconView getIcon() {
        return this.mIcon;
    }

    public View getIconWithBackground() {
        return this.mIconFrame;
    }

    public boolean performClick() {
        OpLottieUtils opLottieUtils = this.mLottieUtils;
        if (opLottieUtils != null && opLottieUtils.performClick()) {
            return true;
        }
        this.mClicked = true;
        return super.performClick();
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityEvent.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                boolean z = this.mClicked ? !this.mTileState : this.mTileState;
                accessibilityEvent.setContentDescription(getResources().getString(z ? R$string.switch_bar_on : R$string.switch_bar_off));
                accessibilityEvent.setChecked(z);
            }
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        boolean z = false;
        accessibilityNodeInfo.setSelected(false);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            accessibilityNodeInfo.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                if (!this.mClicked) {
                    z = this.mTileState;
                } else if (!this.mTileState) {
                    z = true;
                }
                accessibilityNodeInfo.setText(getResources().getString(z ? R$string.switch_bar_on : R$string.switch_bar_off));
                accessibilityNodeInfo.setChecked(z);
                accessibilityNodeInfo.setCheckable(true);
                if (isLongClickable()) {
                    accessibilityNodeInfo.addAction(new AccessibilityAction(AccessibilityAction.ACTION_LONG_CLICK.getId(), getResources().getString(R$string.accessibility_long_click_tile)));
                }
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('[');
        StringBuilder sb2 = new StringBuilder();
        sb2.append("locInScreen=(");
        sb2.append(this.mLocInScreen[0]);
        sb2.append(", ");
        sb2.append(this.mLocInScreen[1]);
        sb2.append(")");
        sb.append(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(", iconView=");
        sb3.append(this.mIcon.toString());
        sb.append(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append(", tileState=");
        sb4.append(this.mTileState);
        sb.append(sb4.toString());
        sb.append("]");
        return sb.toString();
    }
}
