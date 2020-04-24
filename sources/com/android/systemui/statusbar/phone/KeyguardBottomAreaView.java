package com.android.systemui.statusbar.phone;

import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyCarrierArea;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.ActivityIntentHelper;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$bool;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.doze.util.BurnInHelperKt;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter.Callback;
import com.android.systemui.plugins.IntentButtonProvider;
import com.android.systemui.plugins.IntentButtonProvider.IntentButton;
import com.android.systemui.plugins.IntentButtonProvider.IntentButton.IconState;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.AccessibilityController.AccessibilityStateChangedCallback;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionController.Extension;
import com.android.systemui.statusbar.policy.ExtensionController.ExtensionBuilder;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.tuner.LockscreenFragment.LockButtonFactory;
import com.android.systemui.tuner.TunerService;
import com.oneplus.systemui.statusbar.phone.OpNotificationPanelView;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.oneplus.odm.OpDeviceManagerInjector;

public class KeyguardBottomAreaView extends FrameLayout implements OnClickListener, OnUnlockMethodChangedListener, AccessibilityStateChangedCallback {
    public static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
    /* access modifiers changed from: private */
    public static final Intent PHONE_INTENT = new Intent("android.intent.action.DIAL");
    /* access modifiers changed from: private */
    public static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    private AccessibilityController mAccessibilityController;
    private AccessibilityDelegate mAccessibilityDelegate;
    private ActivityIntentHelper mActivityIntentHelper;
    private ActivityStarter mActivityStarter;
    private KeyguardAffordanceHelper mAffordanceHelper;
    /* access modifiers changed from: private */
    public AssistManager mAssistManager;
    private int mBurnInXOffset;
    private int mBurnInYOffset;
    private View mCameraPreview;
    private float mDarkAmount;
    private ImageView mDashView;
    private final BroadcastReceiver mDevicePolicyReceiver;
    protected Display mDisplay;
    private final DisplayMetrics mDisplayMetrics;
    private boolean mDozing;
    private EmergencyCarrierArea mEmergencyCarrierArea;
    private TextView mEnterpriseDisclosure;
    private FlashlightController mFlashlightController;
    private Handler mHandler;
    private ViewGroup mIndicationArea;
    private int mIndicationBottomMargin;
    private TextView mIndicationText;
    private boolean mIs2KDisplay;
    /* access modifiers changed from: private */
    public KeyguardAffordanceView mLeftAffordanceView;
    /* access modifiers changed from: private */
    public Drawable mLeftAssistIcon;
    private IntentButton mLeftButton;
    private String mLeftButtonStr;
    private Extension<IntentButton> mLeftExtension;
    /* access modifiers changed from: private */
    public boolean mLeftIsVoiceAssist;
    private View mLeftPreview;
    private int mLockIconHeight;
    private int mLockIconMarginBottom;
    private int mLockIconWidth;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    private boolean mNeedShowOTAWizard;
    private ViewGroup mOverlayContainer;
    private ViewGroup mPreviewContainer;
    private PreviewInflater mPreviewInflater;
    private boolean mPrewarmBound;
    private final ServiceConnection mPrewarmConnection;
    /* access modifiers changed from: private */
    public Messenger mPrewarmMessenger;
    private Point mRealDisplaySize;
    /* access modifiers changed from: private */
    public KeyguardAffordanceView mRightAffordanceView;
    private IntentButton mRightButton;
    private String mRightButtonStr;
    private Extension<IntentButton> mRightExtension;
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    /* access modifiers changed from: private */
    public boolean mUserSetupComplete;
    protected WindowManager mWindowManager;

    private class DefaultLeftButton implements IntentButton {
        private IconState mIconState;

        private DefaultLeftButton() {
            this.mIconState = new IconState();
        }

        public IconState getIcon() {
            KeyguardBottomAreaView keyguardBottomAreaView = KeyguardBottomAreaView.this;
            keyguardBottomAreaView.mLeftIsVoiceAssist = keyguardBottomAreaView.canLaunchVoiceAssist();
            boolean z = KeyguardBottomAreaView.this.getResources().getBoolean(R$bool.oneplus_config_keyguardShowLeftAffordance);
            boolean z2 = true;
            if (KeyguardBottomAreaView.this.mLeftIsVoiceAssist) {
                IconState iconState = this.mIconState;
                if (!KeyguardBottomAreaView.this.mUserSetupComplete || !z || KeyguardBottomAreaView.this.isNeedHideLockIcon()) {
                    z2 = false;
                }
                iconState.isVisible = z2;
                if (KeyguardBottomAreaView.this.mLeftAssistIcon != null) {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mLeftAssistIcon;
                } else if (OpUtils.isCustomFingerprint()) {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R$drawable.ic_mic_fod);
                } else {
                    this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R$drawable.ic_mic_26dp);
                }
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R$string.accessibility_voice_assist_button);
            } else {
                IconState iconState2 = this.mIconState;
                if (!KeyguardBottomAreaView.this.mUserSetupComplete || !z || !KeyguardBottomAreaView.this.isPhoneVisible() || KeyguardBottomAreaView.this.isNeedHideLockIcon()) {
                    z2 = false;
                }
                iconState2.isVisible = z2;
                this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R$drawable.ic_phone_fod);
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R$string.accessibility_phone_button);
            }
            return this.mIconState;
        }

        public Intent getIntent() {
            return KeyguardBottomAreaView.PHONE_INTENT;
        }
    }

    private class DefaultRightButton implements IntentButton {
        private IconState mIconState;

        private DefaultRightButton() {
            this.mIconState = new IconState();
        }

        public IconState getIcon() {
            ResolveInfo resolveCameraIntent = KeyguardBottomAreaView.this.resolveCameraIntent();
            boolean z = true;
            boolean z2 = KeyguardBottomAreaView.this.mStatusBar != null && !KeyguardBottomAreaView.this.mStatusBar.isCameraAllowedByAdmin();
            IconState iconState = this.mIconState;
            if (z2 || resolveCameraIntent == null || !KeyguardBottomAreaView.this.getResources().getBoolean(R$bool.oneplus_config_keyguardShowCameraAffordance) || !KeyguardBottomAreaView.this.mUserSetupComplete || KeyguardBottomAreaView.this.isNeedHideLockIcon()) {
                z = false;
            }
            iconState.isVisible = z;
            this.mIconState.drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R$drawable.ic_camera_fod);
            this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R$string.accessibility_camera_button);
            return this.mIconState;
        }

        public Intent getIntent() {
            boolean userCanSkipBouncer = KeyguardUpdateMonitor.getInstance(KeyguardBottomAreaView.this.mContext).getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser());
            boolean isSecure = KeyguardBottomAreaView.this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser());
            if (KeyguardBottomAreaView.this.mStatusBar == null || !isSecure || userCanSkipBouncer || KeyguardBottomAreaView.this.mStatusBar.isKeyguardShowing()) {
                return (!isSecure || userCanSkipBouncer) ? KeyguardBottomAreaView.INSECURE_CAMERA_INTENT : KeyguardBottomAreaView.SECURE_CAMERA_INTENT;
            }
            Log.d("StatusBar/KeyguardBottomAreaView", "launchCamera to INSECURE");
            return KeyguardBottomAreaView.INSECURE_CAMERA_INTENT;
        }
    }

    /* access modifiers changed from: private */
    public static boolean isSuccessfulLaunch(int i) {
        return i == 0 || i == 3 || i == 2;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public KeyguardBottomAreaView(Context context) {
        this(context, null);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPrewarmConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = new Messenger(iBinder);
            }

            public void onServiceDisconnected(ComponentName componentName) {
                KeyguardBottomAreaView.this.mPrewarmMessenger = null;
            }
        };
        this.mRightButton = new DefaultRightButton();
        this.mLeftButton = new DefaultLeftButton();
        this.mHandler = null;
        this.mDisplayMetrics = (DisplayMetrics) Dependency.get(DisplayMetrics.class);
        this.mRealDisplaySize = new Point();
        this.mIs2KDisplay = false;
        this.mAccessibilityDelegate = new AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                String str = view == KeyguardBottomAreaView.this.mRightAffordanceView ? KeyguardBottomAreaView.this.getResources().getString(R$string.camera_label) : view == KeyguardBottomAreaView.this.mLeftAffordanceView ? KeyguardBottomAreaView.this.mLeftIsVoiceAssist ? KeyguardBottomAreaView.this.getResources().getString(R$string.voice_assist_label) : KeyguardBottomAreaView.this.getResources().getString(R$string.phone_label) : null;
                accessibilityNodeInfo.addAction(new AccessibilityAction(16, str));
            }

            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i == 16) {
                    if (view == KeyguardBottomAreaView.this.mRightAffordanceView) {
                        KeyguardBottomAreaView.this.launchCamera("lockscreen_affordance");
                        return true;
                    } else if (view == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                        KeyguardBottomAreaView.this.launchLeftAffordance();
                        return true;
                    }
                }
                return super.performAccessibilityAction(view, i, bundle);
            }
        };
        this.mLockIconMarginBottom = 0;
        this.mLockIconHeight = 0;
        this.mLockIconWidth = 0;
        this.mDevicePolicyReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                KeyguardBottomAreaView.this.post(new Runnable() {
                    public void run() {
                        KeyguardBottomAreaView.this.updateCameraVisibility();
                    }
                });
            }
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onUserSwitchComplete(int i) {
                KeyguardBottomAreaView.this.updateCameraVisibility();
            }

            public void onUserUnlocked() {
                KeyguardBottomAreaView.this.inflateCameraPreview();
                KeyguardBottomAreaView.this.updateCameraVisibility();
                KeyguardBottomAreaView.this.updateLeftAffordance();
            }
        };
        this.mNeedShowOTAWizard = false;
        initHandler();
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
    }

    public void initFrom(KeyguardBottomAreaView keyguardBottomAreaView) {
        setStatusBar(keyguardBottomAreaView.mStatusBar);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPreviewInflater = new PreviewInflater(this.mContext, new LockPatternUtils(this.mContext), new ActivityIntentHelper(this.mContext));
        this.mPreviewContainer = (ViewGroup) findViewById(R$id.preview_container);
        this.mEmergencyCarrierArea = (EmergencyCarrierArea) findViewById(R$id.keyguard_selector_fade_container);
        this.mOverlayContainer = (ViewGroup) findViewById(R$id.overlay_container);
        this.mRightAffordanceView = (KeyguardAffordanceView) findViewById(R$id.camera_button);
        this.mLeftAffordanceView = (KeyguardAffordanceView) findViewById(R$id.left_button);
        this.mIndicationArea = (ViewGroup) findViewById(R$id.keyguard_indication_area);
        this.mEnterpriseDisclosure = (TextView) findViewById(R$id.keyguard_indication_enterprise_disclosure);
        this.mIndicationText = (TextView) findViewById(R$id.keyguard_indication_text);
        calculateIndicationBottomMargin();
        this.mBurnInYOffset = getResources().getDimensionPixelSize(R$dimen.default_burn_in_prevention_offset);
        this.mDashView = (ImageView) findViewById(R$id.charging_dash);
        updateCameraVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        setClipChildren(false);
        setClipToPadding(false);
        inflateCameraPreview();
        this.mRightAffordanceView.setOnClickListener(this);
        this.mLeftAffordanceView.setOnClickListener(this);
        initAccessibility();
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mFlashlightController = (FlashlightController) Dependency.get(FlashlightController.class);
        this.mAccessibilityController = (AccessibilityController) Dependency.get(AccessibilityController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mActivityIntentHelper = new ActivityIntentHelper(getContext());
        updateLeftAffordance();
        PHONE_INTENT.addCategory("android.intent.category.DEFAULT");
        setLayoutDirection(getResources().getConfiguration().getLayoutDirection());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAccessibilityController.addStateChangedCallback(this);
        ExtensionBuilder newExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButton.class);
        newExtension.withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_RIGHT_BUTTON", $$Lambda$KeyguardBottomAreaView$g4KaNPI9kzVsHrOlMYmA_f9J2Y.INSTANCE);
        newExtension.withTunerFactory(new LockButtonFactory(this.mContext, "sysui_keyguard_right"));
        newExtension.withDefault(new Supplier() {
            public final Object get() {
                return KeyguardBottomAreaView.this.lambda$onAttachedToWindow$1$KeyguardBottomAreaView();
            }
        });
        newExtension.withCallback(new Consumer() {
            public final void accept(Object obj) {
                KeyguardBottomAreaView.this.lambda$onAttachedToWindow$2$KeyguardBottomAreaView((IntentButton) obj);
            }
        });
        this.mRightExtension = newExtension.build();
        ExtensionBuilder newExtension2 = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButton.class);
        newExtension2.withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_LEFT_BUTTON", $$Lambda$KeyguardBottomAreaView$Eh9_ou4HbbT4H4ZFilpDDtanY4k.INSTANCE);
        newExtension2.withTunerFactory(new LockButtonFactory(this.mContext, "sysui_keyguard_left"));
        newExtension2.withDefault(new Supplier() {
            public final Object get() {
                return KeyguardBottomAreaView.this.lambda$onAttachedToWindow$4$KeyguardBottomAreaView();
            }
        });
        newExtension2.withCallback(new Consumer() {
            public final void accept(Object obj) {
                KeyguardBottomAreaView.this.lambda$onAttachedToWindow$5$KeyguardBottomAreaView((IntentButton) obj);
            }
        });
        this.mLeftExtension = newExtension2.build();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getContext().registerReceiverAsUser(this.mDevicePolicyReceiver, UserHandle.ALL, intentFilter, null, null);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    public /* synthetic */ IntentButton lambda$onAttachedToWindow$1$KeyguardBottomAreaView() {
        return new DefaultRightButton();
    }

    public /* synthetic */ IntentButton lambda$onAttachedToWindow$4$KeyguardBottomAreaView() {
        return new DefaultLeftButton();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAccessibilityController.removeStateChangedCallback(this);
        this.mRightExtension.destroy();
        this.mLeftExtension.destroy();
        getContext().unregisterReceiver(this.mDevicePolicyReceiver);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    private void initAccessibility() {
        this.mLeftAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mRightAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateIndicationArea();
        this.mEnterpriseDisclosure.setTextSize(0, (float) getResources().getDimensionPixelSize(17105450));
        this.mIndicationText.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_text_size_body1));
        LayoutParams layoutParams = this.mRightAffordanceView.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_width);
        layoutParams.height = getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_height);
        this.mRightAffordanceView.setLayoutParams(layoutParams);
        updateRightAffordanceIcon();
        LayoutParams layoutParams2 = this.mLeftAffordanceView.getLayoutParams();
        layoutParams2.width = getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_width);
        layoutParams2.height = getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_height);
        this.mLeftAffordanceView.setLayoutParams(layoutParams2);
        updateLeftAffordanceIcon();
    }

    private void calculateIndicationBottomMargin() {
        if (OpUtils.isCustomFingerprint()) {
            boolean isUnlockWithFingerprintPossible = KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
            this.mDisplay.getMetrics(this.mDisplayMetrics);
            this.mDisplay.getRealSize(this.mRealDisplaySize);
            int i = this.mRealDisplaySize.y;
            boolean z = this.mDisplayMetrics.widthPixels == 1440;
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(z ? R$dimen.op_biometric_icon_normal_location_y_2k : R$dimen.op_biometric_icon_normal_location_y_1080p);
            if (OpUtils.isCutoutHide(this.mContext)) {
                dimensionPixelSize -= OpUtils.getCutoutPathdataHeight(this.mContext);
            }
            int dimensionPixelSize2 = (i - dimensionPixelSize) + this.mContext.getResources().getDimensionPixelSize(z ? R$dimen.op_keyguard_indication_padding_bottom_2k : R$dimen.op_keyguard_indication_padding_bottom_1080p);
            if (!isUnlockWithFingerprintPossible) {
                dimensionPixelSize2 = getResources().getDimensionPixelSize(R$dimen.margin_top1) + getResources().getDimensionPixelSize(R$dimen.keyguard_affordance_height);
            }
            this.mIndicationBottomMargin = dimensionPixelSize2;
            return;
        }
        this.mIndicationBottomMargin = getResources().getDimensionPixelSize(R$dimen.keyguard_indication_margin_bottom);
    }

    public void updateIndicationArea() {
        calculateIndicationBottomMargin();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mIndicationArea.getLayoutParams();
        int i = marginLayoutParams.bottomMargin;
        int i2 = this.mIndicationBottomMargin;
        if (i != i2) {
            marginLayoutParams.bottomMargin = i2;
            this.mIndicationArea.setLayoutParams(marginLayoutParams);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0081, code lost:
        if (r0 > 0) goto L_0x0093;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0091, code lost:
        if (r0 > 0) goto L_0x0093;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateRightAffordanceIcon() {
        /*
            r7 = this;
            com.android.systemui.plugins.IntentButtonProvider$IntentButton r0 = r7.mRightButton
            com.android.systemui.plugins.IntentButtonProvider$IntentButton$IconState r0 = r0.getIcon()
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mRightAffordanceView
            boolean r2 = r7.mDozing
            r3 = 0
            if (r2 != 0) goto L_0x0013
            boolean r2 = r0.isVisible
            if (r2 == 0) goto L_0x0013
            r2 = r3
            goto L_0x0015
        L_0x0013:
            r2 = 8
        L_0x0015:
            r1.setVisibility(r2)
            android.graphics.drawable.Drawable r1 = r0.drawable
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mRightAffordanceView
            android.graphics.drawable.Drawable r2 = r2.getDrawable()
            if (r1 != r2) goto L_0x002c
            boolean r1 = r0.tint
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mRightAffordanceView
            boolean r2 = r2.shouldTint()
            if (r1 == r2) goto L_0x0035
        L_0x002c:
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mRightAffordanceView
            android.graphics.drawable.Drawable r2 = r0.drawable
            boolean r4 = r0.tint
            r1.setImageDrawable(r2, r4)
        L_0x0035:
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mRightAffordanceView
            java.lang.CharSequence r2 = r0.contentDescription
            r1.setContentDescription(r2)
            boolean r1 = com.oneplus.util.OpUtils.isCustomFingerprint()
            if (r1 == 0) goto L_0x0098
            android.content.res.Resources r1 = r7.getResources()
            int r2 = com.android.systemui.R$dimen.op_keyguard_affordance_view_padding
            int r1 = r1.getDimensionPixelSize(r2)
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mRightAffordanceView
            r2.setPaddingRelative(r3, r3, r1, r1)
            android.content.res.Resources r2 = r7.getResources()
            int r4 = com.android.systemui.R$dimen.keyguard_affordance_height
            int r2 = r2.getDimensionPixelSize(r4)
            android.content.res.Resources r4 = r7.getResources()
            int r5 = com.android.systemui.R$dimen.keyguard_affordance_width
            int r4 = r4.getDimensionPixelSize(r5)
            android.graphics.drawable.Drawable r0 = r0.drawable
            if (r0 == 0) goto L_0x006e
            int r0 = r0.getIntrinsicWidth()
            goto L_0x006f
        L_0x006e:
            r0 = r3
        L_0x006f:
            int r5 = r7.getLayoutDirection()
            r6 = 1
            if (r5 != r6) goto L_0x0077
            goto L_0x0078
        L_0x0077:
            r6 = r3
        L_0x0078:
            if (r6 == 0) goto L_0x0086
            int r0 = r0 / 2
            int r4 = r1 + r0
            int r2 = r2 - r1
            int r0 = r2 - r0
            if (r0 <= 0) goto L_0x0084
            goto L_0x0093
        L_0x0084:
            r0 = r3
            goto L_0x0093
        L_0x0086:
            int r4 = r4 - r1
            int r0 = r0 / 2
            int r4 = r4 - r0
            if (r4 <= 0) goto L_0x008d
            goto L_0x008e
        L_0x008d:
            r4 = r3
        L_0x008e:
            int r2 = r2 - r1
            int r0 = r2 - r0
            if (r0 <= 0) goto L_0x0084
        L_0x0093:
            com.android.systemui.statusbar.KeyguardAffordanceView r7 = r7.mRightAffordanceView
            r7.setCenter(r4, r0)
        L_0x0098:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.KeyguardBottomAreaView.updateRightAffordanceIcon():void");
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        updateCameraVisibility();
    }

    public void setAffordanceHelper(KeyguardAffordanceHelper keyguardAffordanceHelper) {
        this.mAffordanceHelper = keyguardAffordanceHelper;
    }

    public void setUserSetupComplete(boolean z) {
        this.mUserSetupComplete = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    private Intent getCameraIntent() {
        return this.mRightButton.getIntent();
    }

    public ResolveInfo resolveCameraIntent() {
        return this.mContext.getPackageManager().resolveActivityAsUser(getCameraIntent(), 65536, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: private */
    public void updateCameraVisibility() {
        KeyguardAffordanceView keyguardAffordanceView = this.mRightAffordanceView;
        if (keyguardAffordanceView != null) {
            keyguardAffordanceView.setVisibility((this.mDozing || !this.mRightButton.getIcon().isVisible) ? 8 : 0);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0085, code lost:
        if (r0 > 0) goto L_0x0093;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0091, code lost:
        if (r0 > 0) goto L_0x0093;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateLeftAffordanceIcon() {
        /*
            r7 = this;
            com.android.systemui.plugins.IntentButtonProvider$IntentButton r0 = r7.mLeftButton
            com.android.systemui.plugins.IntentButtonProvider$IntentButton$IconState r0 = r0.getIcon()
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mLeftAffordanceView
            boolean r2 = r7.mDozing
            r3 = 0
            if (r2 != 0) goto L_0x0013
            boolean r2 = r0.isVisible
            if (r2 == 0) goto L_0x0013
            r2 = r3
            goto L_0x0015
        L_0x0013:
            r2 = 8
        L_0x0015:
            r1.setVisibility(r2)
            android.graphics.drawable.Drawable r1 = r0.drawable
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mLeftAffordanceView
            android.graphics.drawable.Drawable r2 = r2.getDrawable()
            if (r1 != r2) goto L_0x002c
            boolean r1 = r0.tint
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mLeftAffordanceView
            boolean r2 = r2.shouldTint()
            if (r1 == r2) goto L_0x0035
        L_0x002c:
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mLeftAffordanceView
            android.graphics.drawable.Drawable r2 = r0.drawable
            boolean r4 = r0.tint
            r1.setImageDrawable(r2, r4)
        L_0x0035:
            com.android.systemui.statusbar.KeyguardAffordanceView r1 = r7.mLeftAffordanceView
            java.lang.CharSequence r2 = r0.contentDescription
            r1.setContentDescription(r2)
            boolean r1 = com.oneplus.util.OpUtils.isCustomFingerprint()
            if (r1 == 0) goto L_0x0098
            android.content.res.Resources r1 = r7.getResources()
            int r2 = com.android.systemui.R$dimen.op_keyguard_affordance_view_padding
            int r1 = r1.getDimensionPixelSize(r2)
            com.android.systemui.statusbar.KeyguardAffordanceView r2 = r7.mLeftAffordanceView
            r2.setPaddingRelative(r1, r3, r3, r1)
            android.content.res.Resources r2 = r7.getResources()
            int r4 = com.android.systemui.R$dimen.keyguard_affordance_height
            int r2 = r2.getDimensionPixelSize(r4)
            android.content.res.Resources r4 = r7.getResources()
            int r5 = com.android.systemui.R$dimen.keyguard_affordance_width
            int r4 = r4.getDimensionPixelSize(r5)
            android.graphics.drawable.Drawable r0 = r0.drawable
            if (r0 == 0) goto L_0x006e
            int r0 = r0.getIntrinsicWidth()
            goto L_0x006f
        L_0x006e:
            r0 = r3
        L_0x006f:
            int r5 = r7.getLayoutDirection()
            r6 = 1
            if (r5 != r6) goto L_0x0077
            goto L_0x0078
        L_0x0077:
            r6 = r3
        L_0x0078:
            if (r6 == 0) goto L_0x008a
            int r4 = r4 - r1
            int r0 = r0 / 2
            int r4 = r4 - r0
            if (r4 <= 0) goto L_0x0081
            goto L_0x0082
        L_0x0081:
            r4 = r3
        L_0x0082:
            int r2 = r2 - r1
            int r0 = r2 - r0
            if (r0 <= 0) goto L_0x0088
            goto L_0x0093
        L_0x0088:
            r0 = r3
            goto L_0x0093
        L_0x008a:
            int r0 = r0 / 2
            int r4 = r1 + r0
            int r2 = r2 - r1
            int r0 = r2 - r0
            if (r0 <= 0) goto L_0x0088
        L_0x0093:
            com.android.systemui.statusbar.KeyguardAffordanceView r7 = r7.mLeftAffordanceView
            r7.setCenter(r4, r0)
        L_0x0098:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.KeyguardBottomAreaView.updateLeftAffordanceIcon():void");
    }

    public boolean isLeftVoiceAssist() {
        return this.mLeftIsVoiceAssist;
    }

    /* access modifiers changed from: private */
    public boolean isPhoneVisible() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (!packageManager.hasSystemFeature("android.hardware.telephony") || packageManager.resolveActivity(PHONE_INTENT, 0) == null) {
            return false;
        }
        return true;
    }

    public void onStateChanged(boolean z, boolean z2) {
        this.mRightAffordanceView.setClickable(z2);
        this.mLeftAffordanceView.setClickable(z2);
        this.mRightAffordanceView.setFocusable(z);
        this.mLeftAffordanceView.setFocusable(z);
    }

    public void onClick(View view) {
        if (view == this.mRightAffordanceView) {
            launchCamera("lockscreen_affordance");
        } else if (view == this.mLeftAffordanceView) {
            launchLeftAffordance();
        }
    }

    public void bindCameraPrewarmService() {
        ActivityInfo targetActivityInfo = this.mActivityIntentHelper.getTargetActivityInfo(getCameraIntent(), KeyguardUpdateMonitor.getCurrentUser(), true);
        if (targetActivityInfo != null && targetActivityInfo.metaData != null) {
            String string = targetActivityInfo.metaData.getString("android.media.still_image_camera_preview_service");
            if (string != null) {
                Intent intent = new Intent();
                intent.setClassName(targetActivityInfo.packageName, string);
                intent.setAction("android.service.media.CameraPrewarmService.ACTION_PREWARM");
                try {
                    if (getContext().bindServiceAsUser(intent, this.mPrewarmConnection, 67108865, new UserHandle(-2))) {
                        this.mPrewarmBound = true;
                    }
                } catch (SecurityException e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unable to bind to prewarm service package=");
                    sb.append(targetActivityInfo.packageName);
                    sb.append(" class=");
                    sb.append(string);
                    Log.w("StatusBar/KeyguardBottomAreaView", sb.toString(), e);
                }
            }
        }
    }

    public void unbindCameraPrewarmService(boolean z) {
        if (this.mPrewarmBound) {
            Messenger messenger = this.mPrewarmMessenger;
            if (messenger != null && z) {
                try {
                    messenger.send(Message.obtain(null, 1));
                } catch (RemoteException e) {
                    Log.w("StatusBar/KeyguardBottomAreaView", "Error sending camera fired message", e);
                }
            }
            this.mContext.unbindService(this.mPrewarmConnection);
            this.mPrewarmBound = false;
        }
    }

    public void launchCamera(String str) {
        launchOpCamera(str);
    }

    private void launchOpCamera(String str) {
        String str2 = "com.oneplus.camera";
        if (str == "power_double_tap") {
            final Intent doubleTapPowerOpAppIntent = this.mStatusBar.getDoubleTapPowerOpAppIntent(1);
            if (doubleTapPowerOpAppIntent != null) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        int i;
                        String str = "StatusBar/KeyguardBottomAreaView";
                        ActivityOptions makeBasic = ActivityOptions.makeBasic();
                        makeBasic.setDisallowEnterPictureInPictureWhileLaunching(true);
                        makeBasic.setRotationAnimationHint(3);
                        try {
                            i = ActivityTaskManager.getService().startActivityAsUser(null, KeyguardBottomAreaView.this.getContext().getBasePackageName(), doubleTapPowerOpAppIntent, doubleTapPowerOpAppIntent.resolveTypeIfNeeded(KeyguardBottomAreaView.this.mContext.getContentResolver()), null, null, 0, 268435456, null, makeBasic.toBundle(), UserHandle.CURRENT.getIdentifier());
                        } catch (RemoteException e) {
                            Log.w(str, "DoubleTapPower: Unable to start activity", e);
                            i = -96;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("DoubleTapPower: launching ");
                        sb.append(doubleTapPowerOpAppIntent);
                        sb.append(" result=");
                        sb.append(i);
                        Log.i(str, sb.toString());
                    }
                });
                return;
            }
        }
        String str3 = "StatusBar/KeyguardBottomAreaView";
        if (this.mStatusBar.notifyCameraLaunching(str)) {
            StringBuilder sb = new StringBuilder();
            sb.append("pending launchCamera, ");
            sb.append(str);
            Log.d(str3, sb.toString());
            return;
        }
        final Intent cameraIntent = getCameraIntent();
        StringBuilder sb2 = new StringBuilder();
        sb2.append("launchCamera, ");
        sb2.append(str);
        sb2.append(", intent:");
        sb2.append(cameraIntent);
        Log.d(str3, sb2.toString());
        try {
            this.mContext.getPackageManager().getPackageInfo(str2, 1);
            if (cameraIntent == SECURE_CAMERA_INTENT) {
                cameraIntent.setComponent(new ComponentName(str2, "com.oneplus.camera.OPSecureCameraActivity"));
            } else {
                cameraIntent.setComponent(new ComponentName(str2, "com.oneplus.camera.OPCameraActivity"));
            }
        } catch (NameNotFoundException unused) {
            Log.i(str3, "no op camera");
            cameraIntent.setComponent(new ComponentName("com.android.camera2", "com.android.camera.CameraActivity"));
        }
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", str);
        cameraIntent.putExtra("com.android.systemui.camera_launch_source_gesture", OpNotificationPanelView.mLastCameraGestureLaunchSource);
        OpNotificationPanelView.mLastCameraGestureLaunchSource = 0;
        boolean wouldLaunchResolverActivity = this.mActivityIntentHelper.wouldLaunchResolverActivity(cameraIntent, KeyguardUpdateMonitor.getCurrentUser());
        if (cameraIntent != SECURE_CAMERA_INTENT || wouldLaunchResolverActivity) {
            this.mActivityStarter.startActivity(cameraIntent, false, (Callback) new Callback() {
                public void onActivityStarted(int i) {
                    KeyguardBottomAreaView.this.unbindCameraPrewarmService(KeyguardBottomAreaView.isSuccessfulLaunch(i));
                }
            });
        } else {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    int i;
                    ActivityOptions makeBasic = ActivityOptions.makeBasic();
                    makeBasic.setDisallowEnterPictureInPictureWhileLaunching(true);
                    makeBasic.setRotationAnimationHint(3);
                    try {
                        i = ActivityTaskManager.getService().startActivityAsUser(null, KeyguardBottomAreaView.this.getContext().getBasePackageName(), cameraIntent, cameraIntent.resolveTypeIfNeeded(KeyguardBottomAreaView.this.getContext().getContentResolver()), null, null, 0, 268435456, null, makeBasic.toBundle(), UserHandle.CURRENT.getIdentifier());
                    } catch (RemoteException e) {
                        Log.w("StatusBar/KeyguardBottomAreaView", "Unable to start camera activity", e);
                        i = -96;
                    }
                    final boolean access$600 = KeyguardBottomAreaView.isSuccessfulLaunch(i);
                    KeyguardBottomAreaView.this.post(new Runnable() {
                        public void run() {
                            KeyguardBottomAreaView.this.unbindCameraPrewarmService(access$600);
                        }
                    });
                }
            });
        }
    }

    public void setDarkAmount(float f) {
        if (f != this.mDarkAmount) {
            this.mDarkAmount = f;
            dozeTimeTick();
        }
    }

    public void launchLeftAffordance() {
        String str = "1";
        String str2 = "";
        if (this.mLeftIsVoiceAssist) {
            launchVoiceAssist();
            OpMdmLogger.log("lock_voice", str2, str);
            return;
        }
        launchPhone();
        OpMdmLogger.log("lock_phone", str2, str);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void launchVoiceAssist() {
        C14298 r1 = new Runnable() {
            public void run() {
                KeyguardBottomAreaView.this.mAssistManager.launchVoiceAssistFromKeyguard();
            }
        };
        if (this.mStatusBar.isKeyguardCurrentlySecure()) {
            AsyncTask.execute(r1);
        } else {
            this.mStatusBar.executeRunnableDismissingKeyguard(r1, null, !TextUtils.isEmpty(this.mRightButtonStr) && ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_right_unlock", 1) != 0, false, true);
        }
        collectOpenAssistantEvent();
    }

    private void collectOpenAssistantEvent() {
        this.mHandler.post(new Runnable() {
            public void run() {
                OpDeviceManagerInjector.getInstance().preserveAssistantData(KeyguardBottomAreaView.this.mContext, 9);
            }
        });
    }

    private void initHandler() {
        HandlerThread handlerThread = new HandlerThread("thread");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
    }

    /* access modifiers changed from: private */
    public boolean canLaunchVoiceAssist() {
        return this.mAssistManager.canVoiceAssistBeLaunchedFromKeyguard();
    }

    private void launchPhone() {
        final TelecomManager from = TelecomManager.from(this.mContext);
        if (from.isInCall()) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    from.showInCallScreen(false);
                }
            });
            return;
        }
        boolean z = true;
        if (TextUtils.isEmpty(this.mLeftButtonStr) || ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_keyguard_left_unlock", 1) == 0) {
            z = false;
        }
        this.mActivityStarter.startActivity(this.mLeftButton.getIntent(), z);
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && i == 0) {
            updateCameraVisibility();
        }
    }

    public KeyguardAffordanceView getLeftView() {
        return this.mLeftAffordanceView;
    }

    public KeyguardAffordanceView getRightView() {
        return this.mRightAffordanceView;
    }

    public View getLeftPreview() {
        return this.mLeftPreview;
    }

    public View getRightPreview() {
        return this.mCameraPreview;
    }

    public View getIndicationArea() {
        return this.mIndicationArea;
    }

    public void onUnlockMethodStateChanged() {
        updateCameraVisibility();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0035  */
    /* JADX WARNING: Removed duplicated region for block: B:16:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0023  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void inflateCameraPreview() {
        /*
            r4 = this;
            android.view.View r0 = r4.mCameraPreview
            r1 = 0
            if (r0 == 0) goto L_0x0012
            android.view.ViewGroup r2 = r4.mPreviewContainer
            r2.removeView(r0)
            int r0 = r0.getVisibility()
            if (r0 != 0) goto L_0x0012
            r0 = 1
            goto L_0x0013
        L_0x0012:
            r0 = r1
        L_0x0013:
            com.android.systemui.statusbar.policy.PreviewInflater r2 = r4.mPreviewInflater
            android.content.Intent r3 = r4.getCameraIntent()
            android.view.View r2 = r2.inflatePreview(r3)
            r4.mCameraPreview = r2
            android.view.View r2 = r4.mCameraPreview
            if (r2 == 0) goto L_0x0031
            android.view.ViewGroup r3 = r4.mPreviewContainer
            r3.addView(r2)
            android.view.View r2 = r4.mCameraPreview
            if (r0 == 0) goto L_0x002d
            goto L_0x002e
        L_0x002d:
            r1 = 4
        L_0x002e:
            r2.setVisibility(r1)
        L_0x0031:
            com.android.systemui.statusbar.phone.KeyguardAffordanceHelper r4 = r4.mAffordanceHelper
            if (r4 == 0) goto L_0x0038
            r4.updatePreviews()
        L_0x0038:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.KeyguardBottomAreaView.inflateCameraPreview():void");
    }

    private void updateLeftPreview() {
        View view = this.mLeftPreview;
        if (view != null) {
            this.mPreviewContainer.removeView(view);
        }
        if (this.mLeftIsVoiceAssist) {
            ComponentName voiceInteractorComponentName = this.mAssistManager.getVoiceInteractorComponentName();
            if (voiceInteractorComponentName != null) {
                this.mLeftPreview = this.mPreviewInflater.inflatePreviewFromService(voiceInteractorComponentName);
            }
        } else {
            this.mLeftPreview = this.mPreviewInflater.inflatePreview(this.mLeftButton.getIntent());
        }
        View view2 = this.mLeftPreview;
        if (view2 != null) {
            this.mPreviewContainer.addView(view2);
            this.mLeftPreview.setVisibility(4);
        }
        KeyguardAffordanceHelper keyguardAffordanceHelper = this.mAffordanceHelper;
        if (keyguardAffordanceHelper != null) {
            keyguardAffordanceHelper.updatePreviews();
        }
    }

    public void startFinishDozeAnimation() {
        long j = 0;
        if (this.mLeftAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mLeftAffordanceView, 0);
            j = 48;
        }
        if (this.mRightAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mRightAffordanceView, j);
        }
    }

    private void startFinishDozeAnimationElement(View view, long j) {
        view.setAlpha(0.0f);
        view.setTranslationY((float) (view.getHeight() / 2));
        view.animate().alpha(1.0f).translationY(0.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(j).setDuration(250);
    }

    public void updateLeftAffordance() {
        updateLeftAffordanceIcon();
        updateLeftPreview();
    }

    /* access modifiers changed from: private */
    /* renamed from: setRightButton */
    public void lambda$onAttachedToWindow$2$KeyguardBottomAreaView(IntentButton intentButton) {
        this.mRightButton = intentButton;
        updateRightAffordanceIcon();
        updateCameraVisibility();
        inflateCameraPreview();
    }

    /* access modifiers changed from: private */
    /* renamed from: setLeftButton */
    public void lambda$onAttachedToWindow$5$KeyguardBottomAreaView(IntentButton intentButton) {
        this.mLeftButton = intentButton;
        if (!(this.mLeftButton instanceof DefaultLeftButton)) {
            this.mLeftIsVoiceAssist = false;
        }
        updateLeftAffordance();
    }

    public void setDozing(boolean z, boolean z2) {
        this.mDozing = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
        if (z) {
            this.mOverlayContainer.setVisibility(4);
            return;
        }
        this.mOverlayContainer.setVisibility(0);
        if (z2) {
            startFinishDozeAnimation();
        }
    }

    public void dozeTimeTick() {
        this.mIndicationArea.setTranslationY(((float) (BurnInHelperKt.getBurnInOffset(this.mBurnInYOffset * 2, false) - this.mBurnInYOffset)) * this.mDarkAmount);
    }

    public void setAntiBurnInOffsetX(int i) {
        if (this.mBurnInXOffset != i) {
            this.mBurnInXOffset = i;
            this.mIndicationArea.setTranslationX((float) i);
        }
    }

    public void setAffordanceAlpha(float f) {
        this.mLeftAffordanceView.setAlpha(f);
        this.mRightAffordanceView.setAlpha(f);
        this.mIndicationArea.setAlpha(f);
        this.mEmergencyCarrierArea.setAlpha(f);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        int safeInsetBottom = windowInsets.getDisplayCutout() != null ? windowInsets.getDisplayCutout().getSafeInsetBottom() : 0;
        if (isPaddingRelative()) {
            setPaddingRelative(getPaddingStart(), getPaddingTop(), getPaddingEnd(), safeInsetBottom);
        } else {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), safeInsetBottom);
        }
        return windowInsets;
    }

    public void setShowOTAWizard(boolean z) {
        this.mNeedShowOTAWizard = z;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    /* access modifiers changed from: private */
    public boolean isNeedHideLockIcon() {
        if (!this.mNeedShowOTAWizard || KeyguardUpdateMonitor.getInstance(this.mContext).isUserUnlocked()) {
            return false;
        }
        return this.mNeedShowOTAWizard;
    }
}
