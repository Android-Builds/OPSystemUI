package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Notification;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.LinearInterpolator;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

public class HighlightHintView extends FrameLayout implements OnHighlightHintStateChangeListener, ConfigurationListener {
    /* access modifiers changed from: private */
    public static final boolean OP_DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public static String TAG = "HighlightHintView";
    public static int TAG_KEYGUARD_STATUSBAR = 1001;
    public static int TAG_STATUSBAR = 1000;
    AnimatorSet breathingAnimatorSet;
    /* access modifiers changed from: private */
    public int mBackgroundColor;
    Drawable mBackgroundDrawable;
    FrameLayout mChronometerContainer;
    private ViewGroup mContainer;
    int mContentWidth;
    private Context mContext;
    private TextView mHint;
    private ImageView mIconView;
    private int mOrientation;
    private boolean mShowBreathingEffect;
    /* access modifiers changed from: private */
    public boolean mShowHighlightHint;

    public HighlightHintView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public HighlightHintView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mShowBreathingEffect = false;
        this.mShowHighlightHint = false;
        this.mContentWidth = 0;
        this.mContext = context;
        addView(LayoutInflater.from(context).inflate(R$layout.highlight_hint_view_notch, null));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIconView = (ImageView) findViewById(R$id.highlight_hint_notification_icon);
        this.mHint = (TextView) findViewById(R$id.highlight_hint_notification_text);
        this.mChronometerContainer = (FrameLayout) findViewById(R$id.highlight_hint_view_container);
        this.mContainer = (ViewGroup) findViewById(R$id.highlight_hint_container);
        ViewGroup viewGroup = this.mContainer;
        if (viewGroup != null) {
            this.mBackgroundDrawable = viewGroup.getBackground();
            this.mBackgroundDrawable.mutate();
        }
        this.mOrientation = this.mContext.getResources().getConfiguration().orientation;
        updateLayout();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        ViewGroup viewGroup = this.mContainer;
        if (viewGroup != null) {
            LayoutParams layoutParams = viewGroup.getLayoutParams();
            int width = getWidth() - getPaddingStart();
            if (layoutParams.width > width) {
                layoutParams.width = width;
                this.mContainer.setLayoutParams(layoutParams);
            }
        }
    }

    private void updateLayout() {
        ViewGroup viewGroup = this.mContainer;
        if (viewGroup != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
            layoutParams.width = this.mContext.getResources().getDimensionPixelSize(R$dimen.highlight_hint_width_notch);
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.highlight_hint_bg_height);
            int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_height) - (this.mContext.getResources().getDimensionPixelSize(R$dimen.highlight_hint_bg_vertical_padding) * 2);
            if (dimensionPixelSize > dimensionPixelSize2) {
                dimensionPixelSize = dimensionPixelSize2;
            }
            layoutParams.height = dimensionPixelSize;
            layoutParams.gravity = 17;
            setPaddingRelative(this.mContext.getResources().getDimensionPixelSize(R$dimen.highlight_hint_margin_start), getPaddingTop(), getPaddingTop(), getPaddingEnd());
        }
    }

    private void updateHint() {
        NotificationData notificationData = ((HighlightHintController) Dependency.get(HighlightHintController.class)).getNotificationData();
        if (!(notificationData == null || notificationData.getHighlightHintNotification() == null)) {
            Notification notification = notificationData.getHighlightHintNotification().getNotification();
            if (notification != null) {
                try {
                    this.mShowBreathingEffect = notification.extras.getBoolean("op_show_breathing");
                    int i = notification.extras.getInt("op_breathing_color_start");
                    int i2 = notification.extras.getInt("op_breathing_color_end");
                    if (this.mShowBreathingEffect) {
                        playBreathingAnimation(i, i2);
                    } else {
                        this.mBackgroundColor = notification.getBackgroundColorOnStatusBar();
                        if (this.mBackgroundDrawable != null) {
                            this.mBackgroundDrawable.setColorFilter(this.mBackgroundColor, Mode.SRC_ATOP);
                        }
                    }
                    Resources resourcesForApplication = this.mContext.getPackageManager().getResourcesForApplication(notificationData.getHighlightHintNotification().getPackageName());
                    if (notification.getStatusBarIcon() != -1) {
                        this.mIconView.setImageDrawable(resourcesForApplication.getDrawable(notification.getStatusBarIcon(), null));
                        this.mIconView.setVisibility(0);
                    } else {
                        this.mIconView.setVisibility(8);
                    }
                    int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_clock_size);
                    if (this.mHint != null) {
                        this.mHint.setTextSize(0, (float) dimensionPixelSize);
                        if (notification.getTextOnStatusBar() != -1) {
                            StringBuffer stringBuffer = new StringBuffer(resourcesForApplication.getString(notification.getTextOnStatusBar()));
                            stringBuffer.append(" ");
                            this.mHint.setText(stringBuffer);
                            int maxHighlightHintTextWidth = getMaxHighlightHintTextWidth();
                            if (maxHighlightHintTextWidth > 0) {
                                this.mHint.setMaxWidth(maxHighlightHintTextWidth);
                            }
                            this.mHint.setVisibility(0);
                        } else {
                            this.mHint.setVisibility(8);
                        }
                    }
                    if (this.mChronometerContainer != null) {
                        if (notification.ShowChronometerOnStatusBar()) {
                            this.mChronometerContainer.removeAllViews();
                            int color = this.mContext.getResources().getColor(R$color.highlight_hint_view_chronometer_text_color);
                            Chronometer statusBarChronometer = ((Integer) getTag()).intValue() == TAG_STATUSBAR ? notificationData.getStatusBarChronometer() : notificationData.getKeyguardChronometer();
                            if (statusBarChronometer != null) {
                                if (statusBarChronometer.getParent() != null) {
                                    ((ViewGroup) statusBarChronometer.getParent()).removeView(statusBarChronometer);
                                }
                                statusBarChronometer.setTextSize(0, (float) dimensionPixelSize);
                                statusBarChronometer.setTextColor(color);
                                statusBarChronometer.setEllipsize(TruncateAt.END);
                                this.mChronometerContainer.addView(statusBarChronometer);
                            }
                            this.mChronometerContainer.setVisibility(0);
                        } else {
                            this.mChronometerContainer.setVisibility(8);
                        }
                    }
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

    private int getMaxHighlightHintTextWidth() {
        NotificationData notificationData = ((HighlightHintController) Dependency.get(HighlightHintController.class)).getNotificationData();
        int i = 0;
        if (notificationData == null) {
            return 0;
        }
        Chronometer statusBarChronometer = ((Integer) getTag()).intValue() == TAG_STATUSBAR ? notificationData.getStatusBarChronometer() : notificationData.getKeyguardChronometer();
        if (statusBarChronometer == null) {
            return 0;
        }
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(0, 0);
        int makeMeasureSpec2 = MeasureSpec.makeMeasureSpec(0, 0);
        int measuredWidth = statusBarChronometer.getMeasuredWidth();
        if (measuredWidth == 0 || this.mContentWidth != measuredWidth) {
            statusBarChronometer.measure(makeMeasureSpec, makeMeasureSpec2);
            this.mContentWidth = measuredWidth;
        }
        int measuredWidth2 = statusBarChronometer.getMeasuredWidth();
        int measuredWidth3 = this.mIconView.getMeasuredWidth();
        Context context = this.mContext;
        if (!(context == null || context.getResources() == null)) {
            i = this.mContext.getResources().getDimensionPixelSize(R$dimen.highlighthint_chronometer_padding);
        }
        int width = ((getWidth() - measuredWidth2) - measuredWidth3) - i;
        int width2 = (int) (((double) getWidth()) * 0.3d);
        if (width > 0) {
            width2 = width;
        }
        return width2;
    }

    public void onHighlightHintStateChange() {
        if (((Integer) getTag()).intValue() == TAG_STATUSBAR) {
            this.mShowHighlightHint = ((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow();
            if (this.mShowHighlightHint) {
                updateHint();
                setVisibility(0);
            } else {
                setVisibility(8);
                AnimatorSet animatorSet = this.breathingAnimatorSet;
                if (animatorSet != null) {
                    animatorSet.removeAllListeners();
                    this.breathingAnimatorSet.cancel();
                    this.breathingAnimatorSet = null;
                }
            }
        }
    }

    public void onHighlightHintInfoChange() {
        updateHint();
    }

    private void playBreathingAnimation(int i, int i2) {
        if (OP_DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("playBreathingAnimation, Start with color[");
            sb.append(i);
            sb.append("] to color[");
            sb.append(i2);
            sb.append("]");
            Log.d(str, sb.toString());
        }
        if (this.breathingAnimatorSet == null) {
            this.breathingAnimatorSet = new AnimatorSet();
        }
        if (!this.breathingAnimatorSet.isRunning()) {
            C14111 r0 = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    HighlightHintView.this.mBackgroundColor = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    HighlightHintView highlightHintView = HighlightHintView.this;
                    highlightHintView.mBackgroundDrawable.setColorFilter(highlightHintView.mBackgroundColor, Mode.SRC_ATOP);
                }
            };
            ValueAnimator ofArgb = ValueAnimator.ofArgb(new int[]{i, i2});
            ofArgb.setDuration(1000);
            ofArgb.addUpdateListener(r0);
            ofArgb.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
            ValueAnimator ofArgb2 = ValueAnimator.ofArgb(new int[]{i2, i});
            ofArgb2.setDuration(1000);
            ofArgb2.addUpdateListener(r0);
            ofArgb2.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.breathingAnimatorSet.play(ofArgb2).after(ofArgb);
            this.breathingAnimatorSet.setInterpolator(new LinearInterpolator());
            this.breathingAnimatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    if (HighlightHintView.this.mShowHighlightHint) {
                        AnimatorSet animatorSet = HighlightHintView.this.breathingAnimatorSet;
                        if (animatorSet != null) {
                            animatorSet.start();
                            if (HighlightHintView.OP_DEBUG) {
                                Log.d(HighlightHintView.TAG, "playBreathingAnimation, onAnimationEnd: Restart breathing animation");
                            }
                        }
                    }
                }
            });
            this.breathingAnimatorSet.start();
            if (OP_DEBUG) {
                Log.d(TAG, "playBreathingAnimation: Start breathing animation");
            }
        }
    }
}
