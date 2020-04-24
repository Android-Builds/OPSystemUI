package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings.Secure;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;

public class NavigationBarGuide {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private final Runnable mConfirm = new Runnable() {
        public void run() {
            if (NavigationBarGuide.DEBUG) {
                Slog.d("NavigationBarGuide", "mConfirm.run()");
            }
            if (!NavigationBarGuide.this.mConfirmed) {
                NavigationBarGuide.this.mConfirmed = true;
                NavigationBarGuide.this.saveSetting();
            }
            NavigationBarGuide.this.handleHide();
        }
    };
    /* access modifiers changed from: private */
    public boolean mConfirmed;
    private ContentWindowView mContentWindow;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final C1457H mHandler;
    /* access modifiers changed from: private */
    public int mRotation;
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;
    private final IBinder mWindowToken = new Binder();

    private class ContentWindowView extends FrameLayout {
        /* access modifiers changed from: private */
        public final ColorDrawable mColor = new ColorDrawable(0);
        /* access modifiers changed from: private */
        public ValueAnimator mColorAnim;
        /* access modifiers changed from: private */
        public final Runnable mConfirm;
        /* access modifiers changed from: private */
        public ViewGroup mContentLayout;
        private OnComputeInternalInsetsListener mInsetsListener = new OnComputeInternalInsetsListener() {
            private final int[] mTmpInt2 = new int[2];

            public void onComputeInternalInsets(InternalInsetsInfo internalInsetsInfo) {
                ContentWindowView.this.mContentLayout.getLocationInWindow(this.mTmpInt2);
                internalInsetsInfo.setTouchableInsets(3);
                Region region = internalInsetsInfo.touchableRegion;
                int[] iArr = this.mTmpInt2;
                region.set(iArr[0], iArr[1], iArr[0] + ContentWindowView.this.mContentLayout.getWidth(), this.mTmpInt2[1] + ContentWindowView.this.mContentLayout.getHeight());
            }
        };
        /* access modifiers changed from: private */
        public final Interpolator mInterpolator;
        private MyOrientationEventListener mOrientationListener;
        /* access modifiers changed from: private */
        public Runnable mUpdateLayoutRunnable = new Runnable() {
            public void run() {
                if (ContentWindowView.this.mContentLayout != null && ContentWindowView.this.mContentLayout.getParent() != null) {
                    ContentWindowView.this.initViews();
                }
            }
        };

        private class MyOrientationEventListener extends OrientationEventListener {
            public MyOrientationEventListener(Context context, int i) {
                super(context, i);
            }

            public void onOrientationChanged(int i) {
                if (!(i == -1 || NavigationBarGuide.this.mRotation == NavigationBarGuide.this.mWindowManager.getDefaultDisplay().getRotation())) {
                    ContentWindowView contentWindowView = ContentWindowView.this;
                    contentWindowView.post(contentWindowView.mUpdateLayoutRunnable);
                }
            }
        }

        public boolean onTouchEvent(MotionEvent motionEvent) {
            return true;
        }

        public ContentWindowView(Context context, Runnable runnable) {
            super(context);
            this.mConfirm = runnable;
            setBackground(this.mColor);
            setImportantForAccessibility(2);
            this.mInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        }

        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            this.mOrientationListener = new MyOrientationEventListener(this.mContext, 3);
            this.mOrientationListener.enable();
            getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsListener);
            initViews();
        }

        public void onDetachedFromWindow() {
            this.mOrientationListener.disable();
            NavigationBarGuide.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (NavigationBarGuide.this.mStatusBar.getNavigationBarView() != null) {
                        NavigationBarGuide.this.mStatusBar.getNavigationBarView().setVisibility(0);
                    }
                }
            }, 250);
        }

        /* access modifiers changed from: private */
        public void initViews() {
            int i;
            int i2;
            int i3;
            removeAllViews();
            NavigationBarGuide navigationBarGuide = NavigationBarGuide.this;
            navigationBarGuide.mRotation = navigationBarGuide.mWindowManager.getDefaultDisplay().getRotation();
            if (NavigationBarGuide.this.mRotation == 1) {
                i = R$layout.navigation_bar_guide_rot90;
            } else if (NavigationBarGuide.this.mRotation == 3) {
                i = R$layout.navigation_bar_guide_rot270;
            } else {
                i = R$layout.navigation_bar_guide;
            }
            this.mContentLayout = (ViewGroup) View.inflate(getContext(), i, null);
            TextView textView = (TextView) this.mContentLayout.findViewById(R$id.nav_bar_guide_title);
            if (textView != null) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getResources().getString(R$string.nav_bar_guide_title));
                int length = spannableStringBuilder.length();
                spannableStringBuilder.append("pin_off");
                Drawable drawable = getResources().getDrawable(R$drawable.ic_nav_bar_guide_pin_off);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                spannableStringBuilder.setSpan(new ImageSpan(drawable, 1), length, spannableStringBuilder.length(), 33);
                textView.setText(spannableStringBuilder);
            }
            ((Button) this.mContentLayout.findViewById(R$id.f60ok)).setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    ContentWindowView.this.mConfirm.run();
                }
            });
            addView(this.mContentLayout, NavigationBarGuide.this.getBubbleLayoutParams());
            if (ActivityManager.isHighEndGfx()) {
                final ViewGroup viewGroup = this.mContentLayout;
                if (NavigationBarGuide.this.mRotation == 1 || NavigationBarGuide.this.mRotation == 3) {
                    i3 = getResources().getDimensionPixelSize(R$dimen.nav_bar_guide_anim_offsetX_land);
                    i2 = getResources().getDimensionPixelSize(R$dimen.nav_bar_guide_anim_offsetY_land);
                } else {
                    i3 = getResources().getDimensionPixelSize(R$dimen.nav_bar_guide_anim_offsetX);
                    i2 = getResources().getDimensionPixelSize(R$dimen.nav_bar_guide_anim_offsetY);
                }
                if (NavigationBarGuide.this.mRotation == 3) {
                    i3 = 0 - i3;
                }
                viewGroup.setAlpha(0.0f);
                viewGroup.setTranslationX((float) i3);
                viewGroup.setTranslationY((float) i2);
                postOnAnimation(new Runnable() {
                    public void run() {
                        viewGroup.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(250).setInterpolator(ContentWindowView.this.mInterpolator).withLayer().start();
                        ContentWindowView.this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(0), Integer.valueOf(Integer.MIN_VALUE)});
                        ContentWindowView.this.mColorAnim.addUpdateListener(new AnimatorUpdateListener() {
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                ContentWindowView.this.mColor.setColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                            }
                        });
                        ContentWindowView.this.mColorAnim.setDuration(250);
                        ContentWindowView.this.mColorAnim.setInterpolator(ContentWindowView.this.mInterpolator);
                        ContentWindowView.this.mColorAnim.start();
                    }
                });
                return;
            }
            this.mColor.setColor(Integer.MIN_VALUE);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NavigationBarGuide$H */
    private final class C1457H extends Handler {
        private C1457H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                NavigationBarGuide.this.handleShow();
            } else if (i == 2) {
                NavigationBarGuide.this.handleHide();
            }
        }
    }

    public NavigationBarGuide(Context context, StatusBar statusBar) {
        this.mContext = context;
        this.mHandler = new C1457H();
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mConfirmed = isConfirmed();
        this.mStatusBar = statusBar;
    }

    private boolean isConfirmed() {
        String str = "NavigationBarGuide";
        boolean z = false;
        try {
            z = "confirmed".equals(Secure.getString(this.mContext.getContentResolver(), "navigation_bar_guide_confirmation"));
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Loaded confirmed=");
                sb.append(z);
                Slog.d(str, sb.toString());
            }
        } catch (Throwable th) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Error loading confirmations, value=");
            sb2.append(null);
            Slog.w(str, sb2.toString(), th);
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void saveSetting() {
        String str = "NavigationBarGuide";
        if (DEBUG) {
            Slog.d(str, "saveSetting()");
        }
        try {
            String str2 = this.mConfirmed ? "confirmed" : null;
            Secure.putString(this.mContext.getContentResolver(), "navigation_bar_guide_confirmation", str2);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Saved value=");
                sb.append(str2);
                Slog.d(str, sb.toString());
            }
        } catch (Throwable th) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Error saving confirmations, mConfirmed=");
            sb2.append(this.mConfirmed);
            Slog.w(str, sb2.toString(), th);
        }
    }

    /* access modifiers changed from: private */
    public void handleHide() {
        if (this.mContentWindow != null) {
            if (DEBUG) {
                Slog.d("NavigationBarGuide", "Hiding navigation bar guide confirmation");
            }
            this.mWindowManager.removeView(this.mContentWindow);
            this.mContentWindow = null;
        }
    }

    private LayoutParams getContentWindowLayoutParams() {
        LayoutParams layoutParams = new LayoutParams(-1, -1, 2014, 16777504, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("NavigationBarGuide");
        layoutParams.windowAnimations = R$style.Animation_NavigationBarGuide;
        layoutParams.token = getWindowToken();
        return layoutParams;
    }

    /* access modifiers changed from: private */
    public FrameLayout.LayoutParams getBubbleLayoutParams() {
        int i;
        int i2 = this.mRotation;
        int i3 = -1;
        int i4 = 3;
        if (i2 == 1 || i2 == 3) {
            i = (int) (((float) this.mContext.getResources().getDimensionPixelSize(R$dimen.nav_bar_guide_width_land)) * this.mContext.getResources().getConfiguration().fontScale);
        } else {
            i3 = -2;
            i = -1;
        }
        int i5 = this.mRotation;
        if (i5 == 1) {
            i4 = 5;
        } else if (i5 != 3) {
            i4 = 80;
        }
        return new FrameLayout.LayoutParams(i, i3, i4);
    }

    public void show() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("show DEBUG_SHOW_EVERY_TIME=false, mConfirmed=");
            sb.append(this.mConfirmed);
            Slog.d("NavigationBarGuide", sb.toString());
        }
        if (!this.mConfirmed) {
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessageDelayed(1, 750);
        }
    }

    public void hide() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(2);
    }

    public IBinder getWindowToken() {
        return this.mWindowToken;
    }

    /* access modifiers changed from: private */
    public void handleShow() {
        if (DEBUG) {
            Slog.d("NavigationBarGuide", "Showing navigation bar guide confirmation");
        }
        this.mContentWindow = new ContentWindowView(this.mContext, this.mConfirm);
        this.mContentWindow.setSystemUiVisibility(768);
        this.mWindowManager.addView(this.mContentWindow, getContentWindowLayoutParams());
        this.mStatusBar.getNavigationBarView().setVisibility(8);
    }
}
