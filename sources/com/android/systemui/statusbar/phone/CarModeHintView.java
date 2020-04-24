package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Notification;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.util.OpUtils;

public class CarModeHintView extends FrameLayout implements OnHighlightHintStateChangeListener, ConfigurationListener {
    private static final Interpolator ANIMATION_INTERPILATOR = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    public static final boolean SHOW_OVAL_LAYOUT = OpUtils.isSupportCustomStatusBar();
    /* access modifiers changed from: private */
    public static String TAG = "CarModeHintView";
    private Context mContext;
    private TextView mHint;
    /* access modifiers changed from: private */
    public boolean mHintShow;
    private int mOrientation;
    private Animator mShowAnimation;

    private void updateLayout() {
    }

    public CarModeHintView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public CarModeHintView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHintShow = false;
        this.mContext = context;
        addView(LayoutInflater.from(context).inflate(SHOW_OVAL_LAYOUT ? R$layout.carmode_highlight_hint_view_notch : R$layout.carmode_highlight_view_without_notch, null));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHint = (TextView) findViewById(R$id.notification_text);
        updateLayout();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
    }

    private void updateHint() {
        NotificationData notificationData = ((HighlightHintController) Dependency.get(HighlightHintController.class)).getNotificationData();
        if (!(notificationData == null || notificationData.getCarModeHighlightHintNotification() == null)) {
            Notification notification = notificationData.getCarModeHighlightHintNotification().getNotification();
            if (notification != null) {
                int backgroundColorOnStatusBar = notification.getBackgroundColorOnStatusBar();
                try {
                    Resources resourcesForApplication = this.mContext.getPackageManager().getResourcesForApplication(notificationData.getCarModeHighlightHintNotification().getPackageName());
                    if (this.mHint != null) {
                        if (notification.getTextOnStatusBar() != -1) {
                            StringBuffer stringBuffer = new StringBuffer(resourcesForApplication.getString(notification.getTextOnStatusBar()));
                            stringBuffer.append(" ");
                            this.mHint.setText(stringBuffer);
                            this.mHint.setVisibility(0);
                        } else {
                            this.mHint.setVisibility(8);
                        }
                        this.mHint.setTextSize(0, (float) getContext().getResources().getDimensionPixelOffset(R$dimen.op_car_mode_highlight_hint_text_size));
                    }
                    setBackgroundColor(backgroundColorOnStatusBar);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onConfigChanged(Configuration configuration) {
        int i = this.mOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mOrientation = i2;
            updateLayout();
        }
    }

    public void onDensityOrFontScaleChanged() {
        updateHint();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).addCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).removeCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    public void onCarModeHighlightHintStateChange(boolean z) {
        boolean isCarModeHighlightHintSHow = ((HighlightHintController) Dependency.get(HighlightHintController.class)).isCarModeHighlightHintSHow();
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onCarModeHighlightHintStateChange show:");
            sb.append(isCarModeHighlightHintSHow);
            Log.i(str, sb.toString());
        }
        if (isCarModeHighlightHintSHow) {
            updateHint();
            show(z);
            return;
        }
        hide();
    }

    public void onCarModeHighlightHintInfoChange() {
        updateHint();
    }

    public void show(boolean z) {
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("show:");
            sb.append(z);
            Log.i(str, sb.toString());
        }
        if (!z) {
            setVisibility(0);
        } else if (!this.mHintShow) {
            Animator animator = this.mShowAnimation;
            if (animator != null) {
                animator.cancel();
            } else {
                this.mShowAnimation = getShowAnimation();
            }
            this.mShowAnimation.start();
        }
    }

    public void hide() {
        if (Build.DEBUG_ONEPLUS) {
            Log.i(TAG, "hide");
        }
        if (this.mHintShow) {
            this.mHintShow = false;
            Animator animator = this.mShowAnimation;
            if (animator != null) {
                animator.cancel();
            }
        }
        setVisibility(8);
    }

    private Animator getShowAnimation() {
        new ValueAnimator();
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ofFloat.setStartDelay(75);
        ofFloat.setDuration(150);
        ofFloat.setInterpolator(ANIMATION_INTERPILATOR);
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                CarModeHintView.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        ofFloat.addListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.i(CarModeHintView.TAG, "show");
                }
                CarModeHintView.this.mHintShow = true;
                CarModeHintView.this.setAlpha(0.0f);
                CarModeHintView.this.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                CarModeHintView.this.setAlpha(1.0f);
            }
        });
        return ofFloat;
    }
}
