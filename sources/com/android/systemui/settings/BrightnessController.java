package com.android.systemui.settings;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.System;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.service.vr.IVrStateCallbacks.Stub;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.display.BrightnessUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.settings.ToggleSlider.Listener;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Iterator;

public class BrightnessController implements Listener {
    private int AUTO_BRIGHTNESS_MINIMUM = 0;
    /* access modifiers changed from: private */
    public volatile boolean mAutomatic;
    /* access modifiers changed from: private */
    public final boolean mAutomaticAvailable;
    /* access modifiers changed from: private */
    public final Handler mBackgroundHandler;
    /* access modifiers changed from: private */
    public int mBrightness = 0;
    /* access modifiers changed from: private */
    public final BrightnessObserver mBrightnessObserver;
    /* access modifiers changed from: private */
    public ArrayList<BrightnessStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final ToggleSlider mControl;
    private boolean mControlValueInitialized;
    /* access modifiers changed from: private */
    public final int mDefaultBacklight;
    /* access modifiers changed from: private */
    public final int mDefaultBacklightForVr;
    private final DisplayManager mDisplayManager;
    /* access modifiers changed from: private */
    public boolean mExternalChange;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            boolean z = true;
            BrightnessController.this.mExternalChange = true;
            try {
                int i = message.what;
                if (i == 0) {
                    BrightnessController brightnessController = BrightnessController.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    brightnessController.updateIcon(z);
                } else if (i == 1) {
                    Boolean valueOf = Boolean.valueOf(false);
                    if (message.obj != null && (valueOf instanceof Boolean)) {
                        valueOf = (Boolean) message.obj;
                    }
                    BrightnessController brightnessController2 = BrightnessController.this;
                    int i2 = message.arg1;
                    if (message.arg2 == 0) {
                        z = false;
                    }
                    brightnessController2.updateSlider(i2, z, valueOf.booleanValue());
                    BrightnessController.this.updateIcon(BrightnessController.this.mAutomatic);
                } else if (i == 2) {
                    ToggleSlider access$2000 = BrightnessController.this.mControl;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    access$2000.setChecked(z);
                } else if (i == 3) {
                    BrightnessController.this.mControl.setOnChangedListener(BrightnessController.this);
                } else if (i == 4) {
                    BrightnessController.this.mControl.setOnChangedListener(null);
                } else if (i != 5) {
                    super.handleMessage(message);
                } else {
                    BrightnessController brightnessController3 = BrightnessController.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    brightnessController3.updateVrMode(z);
                }
            } finally {
                BrightnessController.this.mExternalChange = false;
            }
        }
    };
    private final ImageView mIcon;
    /* access modifiers changed from: private */
    public volatile boolean mIsVrModeEnabled;
    private final ImageView mLevelIcon;
    private boolean mListening;
    private final int mMaximumBacklight;
    private final int mMaximumBacklightForVr;
    private final int mMinimumBacklight;
    private final int mMinimumBacklightForVr;
    private ImageView mMirrorIcon = null;
    private ImageView mMirrorLevelIcon = null;
    /* access modifiers changed from: private */
    public boolean mNewController = false;
    private ValueAnimator mSliderAnimator;
    private int mSliderMax = 0;
    private int mSliderValue = 0;
    private final Runnable mStartListeningRunnable = new Runnable() {
        public void run() {
            BrightnessController.this.mBrightnessObserver.startObserving();
            BrightnessController.this.mUserTracker.startTracking();
            BrightnessController.this.mUpdateModeRunnable.run();
            BrightnessController.this.mUpdateSliderNoAnimRunnable.run();
            BrightnessController.this.mHandler.sendEmptyMessage(3);
        }
    };
    private final Runnable mStopListeningRunnable = new Runnable() {
        public void run() {
            BrightnessController.this.mBrightnessObserver.stopObserving();
            BrightnessController.this.mUserTracker.stopTracking();
            BrightnessController.this.mHandler.sendEmptyMessage(4);
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("mStopListeningRunnable mTracking: ");
                sb.append(BrightnessController.this.mTracking);
                sb.append(", mAutomatic: ");
                sb.append(BrightnessController.this.mAutomatic);
                sb.append(", mNewController: ");
                sb.append(BrightnessController.this.mNewController);
                sb.append(", mBrightness: ");
                sb.append(BrightnessController.this.mBrightness);
                Log.d("StatusBar.BrightnessController", sb.toString());
            }
            if (BrightnessController.this.mTracking) {
                AsyncTask.execute(new Runnable() {
                    public final void run() {
                        C10992.this.lambda$run$0$BrightnessController$2();
                    }
                });
                BrightnessController.this.mTracking = false;
            }
        }

        public /* synthetic */ void lambda$run$0$BrightnessController$2() {
            System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), BrightnessController.this.mIsVrModeEnabled ? "screen_brightness_for_vr" : "screen_brightness", BrightnessController.this.mBrightness, -2);
        }
    };
    /* access modifiers changed from: private */
    public boolean mTracking = false;
    /* access modifiers changed from: private */
    public final Runnable mUpdateModeRunnable = new Runnable() {
        public void run() {
            if (BrightnessController.this.mAutomaticAvailable) {
                BrightnessController.this.mAutomatic = System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2) != 0;
                OpMdmLogger.notifyBrightnessMode(BrightnessController.this.mAutomatic);
                BrightnessController.this.mHandler.obtainMessage(0, BrightnessController.this.mAutomatic ? 1 : 0, 0).sendToTarget();
                return;
            }
            BrightnessController.this.mHandler.obtainMessage(2, 0, 0).sendToTarget();
            BrightnessController.this.mHandler.obtainMessage(0, 0, 0).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mUpdateSliderNoAnimRunnable = new Runnable() {
        public void run() {
            int i;
            boolean access$1300 = BrightnessController.this.mIsVrModeEnabled;
            if (access$1300) {
                i = System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_for_vr", BrightnessController.this.mDefaultBacklightForVr, -2);
            } else {
                i = System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", BrightnessController.this.mDefaultBacklight, -2);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("UpdateSliderNoAnimTask: val=");
            sb.append(i);
            sb.append(", auto=");
            sb.append(BrightnessController.this.mAutomatic);
            sb.append(", inVr=");
            sb.append(access$1300);
            Log.d("StatusBar.BrightnessController", sb.toString());
            BrightnessController.this.mHandler.obtainMessage(1, i, access$1300 ? 1 : 0, Boolean.valueOf(true)).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mUpdateSliderRunnable = new Runnable() {
        public void run() {
            int i;
            boolean access$1300 = BrightnessController.this.mIsVrModeEnabled;
            if (access$1300) {
                i = System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_for_vr", BrightnessController.this.mDefaultBacklightForVr, -2);
            } else {
                i = System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", BrightnessController.this.mDefaultBacklight, -2);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("UpdateSliderTask: val=");
            sb.append(i);
            sb.append(", auto=");
            sb.append(BrightnessController.this.mAutomatic);
            sb.append(", inVr=");
            sb.append(access$1300);
            Log.d("StatusBar.BrightnessController", sb.toString());
            BrightnessController.this.mHandler.obtainMessage(1, i, access$1300 ? 1 : 0).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public final CurrentUserTracker mUserTracker;
    private final IVrManager mVrManager;
    private final IVrStateCallbacks mVrStateCallbacks = new Stub() {
        public void onVrStateChanged(boolean z) {
            BrightnessController.this.mHandler.obtainMessage(5, z ? 1 : 0, 0).sendToTarget();
        }
    };

    private class BrightnessObserver extends ContentObserver {
        private final Uri BRIGHTNESS_FOR_VR_URI = System.getUriFor("screen_brightness_for_vr");
        private final Uri BRIGHTNESS_MODE_URI = System.getUriFor("screen_brightness_mode");
        private final Uri BRIGHTNESS_URI = System.getUriFor("screen_brightness");

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z) {
            onChange(z, null);
        }

        public void onChange(boolean z, Uri uri) {
            if (!z) {
                if (this.BRIGHTNESS_MODE_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_FOR_VR_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                }
                if (OpUtils.isCustomFingerprint()) {
                    KeyguardUpdateMonitor.getInstance(BrightnessController.this.mContext).notifyBrightnessChange();
                }
                Iterator it = BrightnessController.this.mChangeCallbacks.iterator();
                while (it.hasNext()) {
                    ((BrightnessStateChangeCallback) it.next()).onBrightnessLevelChanged();
                }
            }
        }

        public void startObserving() {
            ContentResolver contentResolver = BrightnessController.this.mContext.getContentResolver();
            contentResolver.unregisterContentObserver(this);
            contentResolver.registerContentObserver(this.BRIGHTNESS_MODE_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_FOR_VR_URI, false, this, -1);
        }

        public void stopObserving() {
            BrightnessController.this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public interface BrightnessStateChangeCallback {
        void onBrightnessLevelChanged();
    }

    public void onInit(ToggleSlider toggleSlider) {
    }

    public BrightnessController(Context context, ImageView imageView, ImageView imageView2, ToggleSlider toggleSlider) {
        this.mContext = context;
        this.mIcon = imageView2;
        this.mLevelIcon = imageView;
        this.mIcon.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                BrightnessController.this.onClickAutomaticIcon();
            }
        });
        this.mSliderMax = 1023;
        this.mControl = toggleSlider;
        this.mControl.setMax(1023);
        this.mBackgroundHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int i) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            }
        };
        this.mBrightnessObserver = new BrightnessObserver(this.mHandler);
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMinimumBacklight = powerManager.getMinimumScreenBrightnessSetting();
        this.mMaximumBacklight = powerManager.getMaximumScreenBrightnessSetting();
        this.mDefaultBacklight = powerManager.getDefaultScreenBrightnessSetting();
        this.mMinimumBacklightForVr = powerManager.getMinimumScreenBrightnessForVrSetting();
        this.mMaximumBacklightForVr = powerManager.getMaximumScreenBrightnessForVrSetting();
        this.mDefaultBacklightForVr = powerManager.getDefaultScreenBrightnessForVrSetting();
        this.mAutomaticAvailable = context.getResources().getBoolean(17891367);
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mVrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        this.mNewController = this.mContext.getPackageManager().hasSystemFeature("oem.autobrightctl.animation.support");
        StringBuilder sb = new StringBuilder();
        sb.append("mNewController=");
        sb.append(this.mNewController);
        String str = "StatusBar.BrightnessController";
        Log.d(str, sb.toString());
        this.AUTO_BRIGHTNESS_MINIMUM = this.mContext.getResources().getInteger(17694892);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("AUTO_BRIGHTNESS_MINIMUM=");
        sb2.append(this.AUTO_BRIGHTNESS_MINIMUM);
        Log.d(str, sb2.toString());
    }

    public void registerCallbacks() {
        if (!this.mListening) {
            String str = "StatusBar.BrightnessController";
            Log.d(str, "registerCallbacks");
            IVrManager iVrManager = this.mVrManager;
            if (iVrManager != null) {
                try {
                    iVrManager.registerListener(this.mVrStateCallbacks);
                    this.mIsVrModeEnabled = this.mVrManager.getVrModeState();
                } catch (RemoteException e) {
                    Log.e(str, "Failed to register VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStartListeningRunnable);
            this.mListening = true;
        }
    }

    public void unregisterCallbacks() {
        if (this.mListening) {
            String str = "StatusBar.BrightnessController";
            Log.d(str, "unregisterCallbacks");
            IVrManager iVrManager = this.mVrManager;
            if (iVrManager != null) {
                try {
                    iVrManager.unregisterListener(this.mVrStateCallbacks);
                } catch (RemoteException e) {
                    Log.e(str, "Failed to unregister VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStopListeningRunnable);
            this.mListening = false;
            this.mControlValueInitialized = false;
        }
    }

    public void onChanged(ToggleSlider toggleSlider, boolean z, boolean z2, int i, boolean z3) {
        int i2;
        int i3;
        final String str;
        int i4;
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("Slider.onChanged value=");
            sb.append(i);
            sb.append(", extChange=");
            sb.append(this.mExternalChange);
            sb.append(", tracking=");
            sb.append(z);
            sb.append(", auto=");
            sb.append(z2);
            Log.d("StatusBar.BrightnessController", sb.toString());
        }
        this.mSliderValue = i;
        this.mTracking = z;
        updateIcon(this.mAutomatic);
        if (!this.mExternalChange) {
            ValueAnimator valueAnimator = this.mSliderAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            if (this.mIsVrModeEnabled) {
                i3 = 498;
                i2 = this.mMinimumBacklightForVr;
                i4 = this.mMaximumBacklightForVr;
                str = "screen_brightness_for_vr";
            } else {
                i3 = this.mAutomatic ? 219 : 218;
                i2 = this.mMinimumBacklight;
                i4 = this.mMaximumBacklight;
                str = "screen_brightness";
            }
            final int convertGammaToLinear = BrightnessUtils.convertGammaToLinear(i, i2, i4);
            this.mBrightness = convertGammaToLinear;
            if (z3) {
                MetricsLogger.action(this.mContext, i3, convertGammaToLinear);
            }
            setBrightness(convertGammaToLinear);
            if (!z) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), str, convertGammaToLinear, -2);
                    }
                });
            }
            Iterator it = this.mChangeCallbacks.iterator();
            while (it.hasNext()) {
                ((BrightnessStateChangeCallback) it.next()).onBrightnessLevelChanged();
            }
        }
    }

    public void checkRestrictionAndSetEnabled() {
        this.mBackgroundHandler.post(new Runnable() {
            public void run() {
                ((ToggleSliderView) BrightnessController.this.mControl).setEnforcedAdmin(RestrictedLockUtilsInternal.checkIfRestrictionEnforced(BrightnessController.this.mContext, "no_config_brightness", BrightnessController.this.mUserTracker.getCurrentUserId()));
            }
        });
    }

    private void setMode(int i) {
        System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", i, this.mUserTracker.getCurrentUserId());
    }

    private void setBrightness(int i) {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("setBrightness ");
            sb.append(i);
            Log.d("StatusBar.BrightnessController", sb.toString());
        }
        this.mDisplayManager.setTemporaryBrightness(i);
    }

    /* access modifiers changed from: private */
    public void updateIcon(boolean z) {
        updateIconInternal(z, this.mIcon, this.mLevelIcon);
        updateIconInternal(z, this.mMirrorIcon, this.mMirrorLevelIcon);
    }

    private void updateIconInternal(boolean z, ImageView imageView, ImageView imageView2) {
        int i;
        if (imageView != null) {
            if (z) {
                i = R$drawable.ic_qs_brightness_auto_on;
            } else {
                i = R$drawable.ic_qs_brightness_auto_off;
            }
            imageView.setImageResource(i);
        }
        if (imageView2 == null) {
            return;
        }
        if (this.mIsVrModeEnabled) {
            int i2 = this.mSliderValue;
            if (i2 <= this.mMinimumBacklightForVr) {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_low);
            } else if (i2 >= this.mSliderMax - 1) {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_high);
            } else {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_medium);
            }
        } else {
            int i3 = this.mSliderValue;
            if (i3 <= this.mMinimumBacklight) {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_low);
            } else if (i3 >= this.mSliderMax - 1) {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_high);
            } else {
                imageView2.setImageResource(R$drawable.ic_qs_brightness_medium);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateVrMode(boolean z) {
        if (this.mIsVrModeEnabled != z) {
            this.mIsVrModeEnabled = z;
            this.mBackgroundHandler.post(this.mUpdateSliderRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void updateSlider(int i, boolean z, boolean z2) {
        int i2;
        int i3;
        if (z) {
            i2 = this.mMinimumBacklightForVr;
            i3 = this.mMaximumBacklightForVr;
        } else {
            i2 = this.mMinimumBacklight;
            i3 = this.mMaximumBacklight;
        }
        if (i != BrightnessUtils.convertGammaToLinear(this.mControl.getValue(), i2, i3)) {
            int convertLinearToGamma = BrightnessUtils.convertLinearToGamma(i, i2, i3);
            this.mSliderValue = convertLinearToGamma;
            animateSliderTo(convertLinearToGamma, z2);
        }
    }

    private void animateSliderTo(int i, boolean z) {
        String str = "StatusBar.BrightnessController";
        if (!this.mControlValueInitialized && z) {
            StringBuilder sb = new StringBuilder();
            sb.append("not inited, set to ");
            sb.append(i);
            Log.d(str, sb.toString());
            this.mControl.setValue(i);
            this.mControlValueInitialized = true;
        }
        ValueAnimator valueAnimator = this.mSliderAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            Log.d(str, "animateSliderTo: cancel anim.");
            this.mSliderAnimator.cancel();
        }
        this.mSliderAnimator = ValueAnimator.ofInt(new int[]{this.mControl.getValue(), i});
        StringBuilder sb2 = new StringBuilder();
        sb2.append("animateSliderTo: animating from ");
        sb2.append(this.mControl.getValue());
        sb2.append(" to ");
        sb2.append(i);
        Log.d(str, sb2.toString());
        this.mSliderAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                BrightnessController.this.lambda$animateSliderTo$0$BrightnessController(valueAnimator);
            }
        });
        this.mSliderAnimator.setDuration((long) ((Math.abs(this.mControl.getValue() - i) * 3000) / 1023));
        this.mSliderAnimator.start();
    }

    public /* synthetic */ void lambda$animateSliderTo$0$BrightnessController(ValueAnimator valueAnimator) {
        this.mExternalChange = true;
        this.mControl.setValue(((Integer) valueAnimator.getAnimatedValue()).intValue());
        this.mExternalChange = false;
    }

    public void onClickAutomaticIcon() {
        OpMdmLogger.log("quick_bright", "auto", "1");
        setMode(this.mAutomatic ^ true ? 1 : 0);
    }

    public void setMirrorView(View view) {
        this.mMirrorIcon = (ImageView) view.findViewById(R$id.brightness_icon);
        this.mMirrorLevelIcon = (ImageView) view.findViewById(R$id.brightness_level);
    }
}
