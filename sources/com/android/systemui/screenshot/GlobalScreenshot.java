package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityOptions;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.SystemUI;
import com.android.systemui.screenshot.GlobalScreenshot.ActionProxyReceiver;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.NotificationChannels;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class GlobalScreenshot {
    /* access modifiers changed from: private */
    public ImageView mBackgroundView = ((ImageView) this.mScreenshotLayout.findViewById(R$id.global_screenshot_background));
    private float mBgPadding;
    /* access modifiers changed from: private */
    public float mBgPaddingScale;
    /* access modifiers changed from: private */
    public MediaActionSound mCameraSound;
    private Context mContext;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    private final int mPreviewHeight;
    private final int mPreviewWidth;
    private AsyncTask<Void, Void, Void> mSaveInBgTask;
    /* access modifiers changed from: private */
    public Bitmap mScreenBitmap;
    /* access modifiers changed from: private */
    public AnimatorSet mScreenshotAnimation;
    /* access modifiers changed from: private */
    public ImageView mScreenshotFlash = ((ImageView) this.mScreenshotLayout.findViewById(R$id.global_screenshot_flash));
    /* access modifiers changed from: private */
    public View mScreenshotLayout;
    /* access modifiers changed from: private */
    public ScreenshotSelectorView mScreenshotSelectorView = ((ScreenshotSelectorView) this.mScreenshotLayout.findViewById(R$id.global_screenshot_selector));
    /* access modifiers changed from: private */
    public ImageView mScreenshotView = ((ImageView) this.mScreenshotLayout.findViewById(R$id.global_screenshot));
    private LayoutParams mWindowLayoutParams;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;

    public static class ActionProxyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            ((StatusBar) SysUiServiceProvider.getComponent(context, StatusBar.class)).executeRunnableDismissingKeyguard(new Runnable(intent, context) {
                private final /* synthetic */ Intent f$0;
                private final /* synthetic */ Context f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    ActionProxyReceiver.lambda$onReceive$0(this.f$0, this.f$1);
                }
            }, null, true, true, true);
        }

        static /* synthetic */ void lambda$onReceive$0(Intent intent, Context context) {
            try {
                ActivityManagerWrapper.getInstance().closeSystemWindows("screenshot").get(3000, TimeUnit.MILLISECONDS);
                Intent intent2 = (Intent) intent.getParcelableExtra("android:screenshot_action_intent");
                if (intent.getBooleanExtra("android:screenshot_cancel_notification", false)) {
                    GlobalScreenshot.cancelScreenshotNotification(context);
                }
                ActivityOptions makeBasic = ActivityOptions.makeBasic();
                makeBasic.setDisallowEnterPictureInPictureWhileLaunching(intent.getBooleanExtra("android:screenshot_disallow_enter_pip", false));
                context.startActivityAsUser(intent2, makeBasic.toBundle(), UserHandle.CURRENT);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Slog.e("GlobalScreenshot", "Unable to share screenshot", e);
            }
        }
    }

    public static class DeleteScreenshotReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String str = "android:screenshot_uri_id";
            if (intent.hasExtra(str)) {
                GlobalScreenshot.cancelScreenshotNotification(context);
                Uri parse = Uri.parse(intent.getStringExtra(str));
                new DeleteImageInBackgroundTask(context).execute(new Uri[]{parse});
            }
        }
    }

    public static class TargetChosenReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            GlobalScreenshot.cancelScreenshotNotification(context);
        }
    }

    public GlobalScreenshot(Context context) {
        int i;
        Resources resources = context.getResources();
        this.mContext = context;
        this.mScreenshotLayout = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R$layout.global_screenshot, null);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotSelectorView.setFocusable(true);
        this.mScreenshotSelectorView.setFocusableInTouchMode(true);
        this.mScreenshotLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        LayoutParams layoutParams = new LayoutParams(-1, -1, 0, 0, 2036, 525568, -3);
        this.mWindowLayoutParams = layoutParams;
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowLayoutParams.layoutInDisplayCutoutMode = 1;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mNotificationIconSize = resources.getDimensionPixelSize(17104902);
        this.mBgPadding = (float) resources.getDimensionPixelSize(R$dimen.global_screenshot_bg_padding);
        this.mBgPaddingScale = this.mBgPadding / ((float) this.mDisplayMetrics.widthPixels);
        try {
            i = resources.getDimensionPixelSize(R$dimen.notification_panel_width);
        } catch (NotFoundException unused) {
            i = 0;
        }
        if (i <= 0) {
            i = this.mDisplayMetrics.widthPixels;
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = resources.getDimensionPixelSize(R$dimen.notification_max_height);
        this.mCameraSound = new MediaActionSound();
        this.mCameraSound.load(0);
    }

    /* access modifiers changed from: private */
    public void saveScreenshotInWorkerThread(Runnable runnable) {
        SaveImageInBackgroundData saveImageInBackgroundData = new SaveImageInBackgroundData();
        saveImageInBackgroundData.context = this.mContext;
        saveImageInBackgroundData.image = this.mScreenBitmap;
        saveImageInBackgroundData.iconSize = this.mNotificationIconSize;
        saveImageInBackgroundData.finisher = runnable;
        saveImageInBackgroundData.previewWidth = this.mPreviewWidth;
        saveImageInBackgroundData.previewheight = this.mPreviewHeight;
        AsyncTask<Void, Void, Void> asyncTask = this.mSaveInBgTask;
        if (asyncTask != null) {
            asyncTask.cancel(false);
        }
        this.mSaveInBgTask = new SaveImageInBackgroundTask(this.mContext, saveImageInBackgroundData, this.mNotificationManager).execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    public void takeScreenshot(Runnable runnable, boolean z, boolean z2, Rect rect) {
        this.mScreenBitmap = SurfaceControl.screenshot(rect, rect.width(), rect.height(), this.mDisplay.getRotation());
        Bitmap bitmap = this.mScreenBitmap;
        if (bitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager, R$string.screenshot_failed_to_capture_text);
            runnable.run();
            return;
        }
        bitmap.setHasAlpha(false);
        this.mScreenBitmap.prepareToDraw();
        DisplayMetrics displayMetrics = this.mDisplayMetrics;
        startAnimation(runnable, displayMetrics.widthPixels, displayMetrics.heightPixels, z, z2);
    }

    /* access modifiers changed from: 0000 */
    public void takeScreenshot(Runnable runnable, boolean z, boolean z2) {
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        DisplayMetrics displayMetrics = this.mDisplayMetrics;
        takeScreenshot(runnable, z, z2, new Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels));
    }

    /* access modifiers changed from: 0000 */
    public void takeScreenshotPartial(final Runnable runnable, final boolean z, final boolean z2) {
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        this.mScreenshotSelectorView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ScreenshotSelectorView screenshotSelectorView = (ScreenshotSelectorView) view;
                int action = motionEvent.getAction();
                if (action == 0) {
                    screenshotSelectorView.startSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                    return true;
                } else if (action == 1) {
                    screenshotSelectorView.setVisibility(8);
                    GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                    final Rect selectionRect = screenshotSelectorView.getSelectionRect();
                    if (!(selectionRect == null || selectionRect.width() == 0 || selectionRect.height() == 0)) {
                        GlobalScreenshot.this.mScreenshotLayout.post(new Runnable() {
                            public void run() {
                                C10852 r0 = C10852.this;
                                GlobalScreenshot.this.takeScreenshot(runnable, z, z2, selectionRect);
                            }
                        });
                    }
                    screenshotSelectorView.stopSelection();
                    return true;
                } else if (action != 2) {
                    return false;
                } else {
                    screenshotSelectorView.updateSelection((int) motionEvent.getX(), (int) motionEvent.getY());
                    return true;
                }
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            public void run() {
                GlobalScreenshot.this.mScreenshotSelectorView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotSelectorView.requestFocus();
            }
        });
    }

    /* access modifiers changed from: 0000 */
    public void stopScreenshot() {
        if (this.mScreenshotSelectorView.getSelectionRect() != null) {
            this.mWindowManager.removeView(this.mScreenshotLayout);
            this.mScreenshotSelectorView.stopSelection();
        }
    }

    private void startAnimation(final Runnable runnable, int i, int i2, boolean z, boolean z2) {
        if (((PowerManager) this.mContext.getSystemService("power")).isPowerSaveMode()) {
            Toast.makeText(this.mContext, R$string.screenshot_saved_title, 0).show();
        }
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        AnimatorSet animatorSet = this.mScreenshotAnimation;
        if (animatorSet != null) {
            if (animatorSet.isStarted()) {
                this.mScreenshotAnimation.end();
            }
            this.mScreenshotAnimation.removeAllListeners();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        ValueAnimator createScreenshotDropInAnimation = createScreenshotDropInAnimation();
        ValueAnimator createScreenshotDropOutAnimation = createScreenshotDropOutAnimation(i, i2, z, z2);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(new Animator[]{createScreenshotDropInAnimation, createScreenshotDropOutAnimation});
        this.mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.saveScreenshotInWorkerThread(runnable);
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                GlobalScreenshot.this.mScreenBitmap = null;
                GlobalScreenshot.this.mScreenshotView.setImageBitmap(null);
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            public void run() {
                GlobalScreenshot.this.mCameraSound.play(0);
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    private ValueAnimator createScreenshotDropInAnimation() {
        final C10906 r0 = new Interpolator() {
            public float getInterpolation(float f) {
                if (f <= 0.60465115f) {
                    return (float) Math.sin(((double) (f / 0.60465115f)) * 3.141592653589793d);
                }
                return 0.0f;
            }
        };
        final C10917 r1 = new Interpolator() {
            public float getInterpolation(float f) {
                if (f < 0.30232558f) {
                    return 0.0f;
                }
                return (f - 0.60465115f) / 0.39534885f;
            }
        };
        if ((this.mContext.getResources().getConfiguration().uiMode & 48) == 32) {
            this.mScreenshotView.getBackground().setTint(-16777216);
        } else {
            this.mScreenshotView.getBackground().setTintList(null);
        }
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ofFloat.setDuration(430);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setAlpha(0.0f);
                GlobalScreenshot.this.mBackgroundView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotView.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationX(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationY(0.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleX(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleY(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(8);
            }
        });
        ofFloat.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float access$1000 = (GlobalScreenshot.this.mBgPaddingScale + 1.0f) - (r1.getInterpolation(floatValue) * 0.27499998f);
                GlobalScreenshot.this.mBackgroundView.setAlpha(r1.getInterpolation(floatValue) * 0.5f);
                GlobalScreenshot.this.mScreenshotView.setAlpha(floatValue);
                GlobalScreenshot.this.mScreenshotView.setScaleX(access$1000);
                GlobalScreenshot.this.mScreenshotView.setScaleY(access$1000);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(r0.getInterpolation(floatValue));
            }
        });
        return ofFloat;
    }

    private ValueAnimator createScreenshotDropOutAnimation(int i, int i2, boolean z, boolean z2) {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ofFloat.setStartDelay(500);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                GlobalScreenshot.this.mBackgroundView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setLayerType(0, null);
            }
        });
        if (!z || !z2) {
            ofFloat.setDuration(320);
            ofFloat.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float access$1000 = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (0.125f * floatValue);
                    float f = 1.0f - floatValue;
                    GlobalScreenshot.this.mBackgroundView.setAlpha(0.5f * f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(f);
                    GlobalScreenshot.this.mScreenshotView.setScaleX(access$1000);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(access$1000);
                }
            });
        } else {
            final C108312 r6 = new Interpolator() {
                public float getInterpolation(float f) {
                    if (f < 0.8604651f) {
                        return (float) (1.0d - Math.pow((double) (1.0f - (f / 0.8604651f)), 2.0d));
                    }
                    return 1.0f;
                }
            };
            float f = (float) i;
            float f2 = this.mBgPadding;
            float f3 = (f - (f2 * 2.0f)) / 2.0f;
            float f4 = (((float) i2) - (f2 * 2.0f)) / 2.0f;
            final PointF pointF = new PointF((-f3) + (f3 * 0.45f), (-f4) + (f4 * 0.45f));
            ofFloat.setDuration(430);
            ofFloat.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float access$1000 = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (r6.getInterpolation(floatValue) * 0.27500004f);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - floatValue) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - r6.getInterpolation(floatValue));
                    GlobalScreenshot.this.mScreenshotView.setScaleX(access$1000);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(access$1000);
                    GlobalScreenshot.this.mScreenshotView.setTranslationX(pointF.x * floatValue);
                    GlobalScreenshot.this.mScreenshotView.setTranslationY(floatValue * pointF.y);
                }
            });
        }
        return ofFloat;
    }

    static void notifyScreenshotError(Context context, NotificationManager notificationManager, int i) {
        Resources resources = context.getResources();
        String string = resources.getString(i);
        Builder color = new Builder(context, NotificationChannels.ALERTS).setTicker(resources.getString(R$string.screenshot_failed_title)).setContentTitle(resources.getString(R$string.screenshot_failed_title)).setContentText(string).setSmallIcon(R$drawable.stat_notify_image_error).setWhen(System.currentTimeMillis()).setVisibility(1).setCategory("err").setAutoCancel(true).setColor(context.getColor(17170460));
        Intent createAdminSupportIntent = ((DevicePolicyManager) context.getSystemService("device_policy")).createAdminSupportIntent("policy_disable_screen_capture");
        if (createAdminSupportIntent != null) {
            color.setContentIntent(PendingIntent.getActivityAsUser(context, 0, createAdminSupportIntent, 0, null, UserHandle.CURRENT));
        }
        SystemUI.overrideNotificationAppName(context, color, true);
        notificationManager.notify(1, new BigTextStyle(color).bigText(string).build());
    }

    /* access modifiers changed from: private */
    public static void cancelScreenshotNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(1);
    }
}
