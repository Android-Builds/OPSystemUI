package com.android.systemui.volume;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioSystem;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.provider.Settings.Secure;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.VolumeDialog;
import com.android.systemui.plugins.VolumeDialog.Callback;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.plugins.VolumeDialogController.Callbacks;
import com.android.systemui.plugins.VolumeDialogController.State;
import com.android.systemui.plugins.VolumeDialogController.StreamState;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;
import com.oneplus.volume.OpOutputChooser;
import com.oneplus.volume.OpVolumeDialogImpl;
import com.oneplus.volume.OpVolumeDialogImpl.VolumeRow;

public class VolumeDialogImpl extends OpVolumeDialogImpl implements VolumeDialog, ConfigurationListener {
    /* access modifiers changed from: private */
    public static final String TAG = Util.logTag(VolumeDialogImpl.class);
    private final Accessibility mAccessibility = new Accessibility();
    private final AccessibilityManagerWrapper mAccessibilityMgr;
    private int mActiveStream;
    private final ActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public ValueAnimator mAnimVol;
    private boolean mAutomute = true;
    /* access modifiers changed from: private */
    public boolean mConfigChanged = false;
    private ConfigurableTexts mConfigurableTexts;
    /* access modifiers changed from: private */
    public final VolumeDialogController mController;
    private final Callbacks mControllerCallbackH = new Callbacks() {
        public void onShowRequested(int i) {
            try {
                VolumeDialogImpl.this.showH(i);
            } catch (RuntimeException e) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("RuntimeException, ");
                sb.append(e.getMessage());
                Log.e(access$200, sb.toString());
            }
        }

        public void onDismissRequested(int i) {
            VolumeDialogImpl.this.dismissH(i);
        }

        public void onScreenOff() {
            VolumeDialogImpl.this.dismissH(4);
        }

        public void onStateChanged(State state) {
            VolumeDialogImpl.this.onStateChangedH(state);
        }

        public void onLayoutDirectionChanged(int i) {
            VolumeDialogImpl.this.mDialogView.setLayoutDirection(i);
        }

        public void onConfigurationChanged() {
            if (VolumeDialogImpl.this.mODICaptionsView != null && VolumeDialogImpl.this.mODICaptionsView.getVisibility() == 0) {
                VolumeDialogImpl.this.updateODIRelatedLayout();
            }
            VolumeDialogImpl.this.loadOpDimens();
            if (OpUtils.DEBUG_ONEPLUS) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Vol onConfigurationChanged, mOpBeforeExpandWidth:");
                sb.append(VolumeDialogImpl.this.mOpBeforeExpandWidth);
                sb.append(", mOpafterExpandWidth:");
                sb.append(VolumeDialogImpl.this.mOpafterExpandWidth);
                Log.i(access$200, sb.toString());
            }
            VolumeDialogImpl.this.setExpandFeautureDismissState();
            VolumeDialogImpl.this.mDialog.dismiss();
            VolumeDialogImpl.this.mConfigChanged = true;
            VolumeDialogImpl.this.mPendingInit = true;
        }

        public void onShowVibrateHint() {
            if (VolumeDialogImpl.this.mSilentMode) {
                VolumeDialogImpl.this.mController.setRingerMode(0, false);
            }
        }

        public void onShowSilentHint() {
            if (VolumeDialogImpl.this.mSilentMode) {
                VolumeDialogImpl.this.mController.setRingerMode(2, false);
            }
        }

        public void onShowSafetyWarning(int i) {
            VolumeDialogImpl.this.showSafetyWarningH(i);
        }

        public void onAccessibilityModeChanged(Boolean bool) {
            VolumeDialogImpl.this.mShowA11yStream = bool == null ? false : bool.booleanValue();
            VolumeRow access$1400 = VolumeDialogImpl.this.getActiveRow();
            if (VolumeDialogImpl.this.mShowA11yStream || 10 != access$1400.stream) {
                VolumeDialogImpl.this.updateRowsH(access$1400);
            } else {
                VolumeDialogImpl.this.dismissH(7);
            }
        }

        public void onCaptionComponentStateChanged(Boolean bool, Boolean bool2) {
            VolumeDialogImpl.this.mIsCaptionComponentEnabled = bool.booleanValue();
            if (OpUtils.DEBUG_ONEPLUS) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onCaptionComponentStateChanged, mIsCaptionComponentEnabled:");
                sb.append(VolumeDialogImpl.this.mIsCaptionComponentEnabled);
                sb.append(", fromTooltip:");
                sb.append(bool2);
                Log.i(access$200, sb.toString());
            }
            VolumeDialogImpl.this.updateODICaptionsH(bool.booleanValue(), bool2.booleanValue());
        }
    };
    private final DeviceProvisionedController mDeviceProvisionedController;
    /* access modifiers changed from: private */
    public CustomDialog mDialog;
    private ViewGroup mDialogRowsView;
    /* access modifiers changed from: private */
    public ViewGroup mDialogView;
    private final SparseBooleanArray mDynamic = new SparseBooleanArray();
    /* access modifiers changed from: private */
    public final C1713H mHandler = new C1713H();
    private boolean mHasSeenODICaptionsTooltip;
    private boolean mHovering = false;
    /* access modifiers changed from: private */
    public boolean mIsCaptionComponentEnabled;
    private final KeyguardManager mKeyguard;
    private boolean mNeedPlayExpandAnim = false;
    private CaptionsToggleImageButton mODICaptionsIcon;
    private View mODICaptionsTooltipView = null;
    private ViewStub mODICaptionsTooltipViewStub;
    private int mPrevActiveStream;
    private ViewGroup mRinger;
    private ImageButton mRingerIcon;
    /* access modifiers changed from: private */
    public SafetyWarningDialog mSafetyWarning;
    /* access modifiers changed from: private */
    public final Object mSafetyWarningLock = new Object();
    private ImageButton mSettingsBackIcon;
    private View mSettingsBackView;
    private ImageButton mSettingsIcon;
    private ImageButton mSettingsOpSettingsIcon;
    private View mSettingsOpSettingsView;
    private View mSettingsView;
    /* access modifiers changed from: private */
    public boolean mShowA11yStream;
    private boolean mShowActiveStreamOnly;
    /* access modifiers changed from: private */
    public boolean mShowing;
    /* access modifiers changed from: private */
    public boolean mSilentMode = true;
    /* access modifiers changed from: private */
    public State mState;
    private int mTargetBottomSettingsBackIconVisible = 8;
    private int mTargetBottomSettingsIconVisible = 0;
    private int mTargetBottomSettingsOpSettingsIconVisible = 8;
    private Window mWindow;
    private FrameLayout mZenIcon;

    private final class Accessibility extends AccessibilityDelegate {
        private Accessibility() {
        }

        public void init() {
            VolumeDialogImpl.this.mDialogView.setAccessibilityDelegate(this);
        }

        public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            accessibilityEvent.getText().add(VolumeDialogImpl.this.composeWindowTitle());
            return true;
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            VolumeDialogImpl.this.rescheduleTimeoutH();
            return super.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
        }
    }

    public final class CustomDialog extends Dialog implements DialogInterface {
        public CustomDialog(Context context) {
            super(context, R$style.qs_theme);
        }

        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            VolumeDialogImpl.this.rescheduleTimeoutH();
            return super.dispatchTouchEvent(motionEvent);
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            super.setCanceledOnTouchOutside(true);
            super.onStart();
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            super.onStop();
            VolumeDialogImpl.this.mHandler.sendEmptyMessage(4);
        }

        public boolean onTouchEvent(MotionEvent motionEvent) {
            if (OpUtils.DEBUG_ONEPLUS) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("CustomDialog onTouchEvent, event.getAction():");
                sb.append(motionEvent.getAction());
                sb.append(", mShowing:");
                sb.append(VolumeDialogImpl.this.mShowing);
                Log.i(access$200, sb.toString());
            }
            if (!VolumeDialogImpl.this.mShowing) {
                return false;
            }
            VolumeDialogImpl.this.dismissH(1);
            return true;
        }
    }

    /* renamed from: com.android.systemui.volume.VolumeDialogImpl$H */
    private final class C1713H extends Handler {
        public C1713H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    VolumeDialogImpl.this.showH(message.arg1);
                    return;
                case 2:
                    VolumeDialogImpl.this.dismissH(message.arg1);
                    return;
                case 3:
                    VolumeDialogImpl.this.recheckH((VolumeRow) message.obj);
                    return;
                case 4:
                    VolumeDialogImpl.this.recheckH(null);
                    return;
                case 5:
                    VolumeDialogImpl.this.setStreamImportantH(message.arg1, message.arg2 != 0);
                    return;
                case 6:
                    VolumeDialogImpl.this.rescheduleTimeoutH();
                    return;
                case 7:
                    VolumeDialogImpl volumeDialogImpl = VolumeDialogImpl.this;
                    volumeDialogImpl.onStateChangedH(volumeDialogImpl.mState);
                    return;
                default:
                    return;
            }
        }
    }

    private final class VolumeSeekBarChangeListener implements OnSeekBarChangeListener {
        private final VolumeRow mRow;

        private VolumeSeekBarChangeListener(VolumeRow volumeRow) {
            this.mRow = volumeRow;
        }

        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            if (this.mRow.f122ss != null) {
                if (C1684D.BUG) {
                    String access$200 = VolumeDialogImpl.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append(AudioSystem.streamToString(this.mRow.stream));
                    sb.append(" onProgressChanged ");
                    sb.append(i);
                    sb.append(" fromUser=");
                    sb.append(z);
                    Log.d(access$200, sb.toString());
                }
                if (Build.DEBUG_ONEPLUS) {
                    String access$2002 = VolumeDialogImpl.TAG;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("mRow.stream:");
                    sb2.append(this.mRow.stream);
                    sb2.append(" / progress:");
                    sb2.append(i);
                    sb2.append(" / mRingerModeMinLevel * 100:");
                    sb2.append(100);
                    sb2.append(" / mRow.ss.levelMin:");
                    sb2.append(this.mRow.f122ss.levelMin);
                    Log.d(access$2002, sb2.toString());
                }
                if (z) {
                    int i2 = this.mRow.f122ss.levelMin;
                    if (i2 > 0) {
                        int i3 = i2 * 100;
                        if (i < i3) {
                            seekBar.setProgress(i3);
                            i = i3;
                        }
                    }
                    if (this.mRow.stream == 2 && i < 100) {
                        seekBar.setProgress(100);
                        i = 100;
                    }
                    int access$4200 = VolumeDialogImpl.getImpliedLevel(seekBar, i);
                    StreamState streamState = this.mRow.f122ss;
                    if (streamState.level != access$4200 || (streamState.muted && access$4200 > 0)) {
                        this.mRow.userAttempt = SystemClock.uptimeMillis();
                        if (this.mRow.requestedLevel != access$4200) {
                            VolumeDialogImpl.this.mController.setActiveStream(this.mRow.stream);
                            VolumeDialogImpl.this.mController.setStreamVolume(this.mRow.stream, access$4200);
                            this.mRow.requestedLevel = access$4200;
                            Events.writeEvent(VolumeDialogImpl.this.mContext, 9, Integer.valueOf(this.mRow.stream), Integer.valueOf(access$4200));
                        }
                    }
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            if (C1684D.BUG) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onStartTrackingTouch ");
                sb.append(this.mRow.stream);
                Log.d(access$200, sb.toString());
            }
            VolumeDialogImpl.this.mController.setActiveStream(this.mRow.stream);
            this.mRow.tracking = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (C1684D.BUG) {
                String access$200 = VolumeDialogImpl.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onStopTrackingTouch ");
                sb.append(this.mRow.stream);
                Log.d(access$200, sb.toString());
            }
            VolumeRow volumeRow = this.mRow;
            volumeRow.tracking = false;
            volumeRow.userAttempt = SystemClock.uptimeMillis();
            int access$4200 = VolumeDialogImpl.getImpliedLevel(seekBar, seekBar.getProgress());
            Events.writeEvent(VolumeDialogImpl.this.mContext, 16, Integer.valueOf(this.mRow.stream), Integer.valueOf(access$4200));
            if (this.mRow.f122ss.level != access$4200) {
                VolumeDialogImpl.this.mHandler.sendMessageDelayed(VolumeDialogImpl.this.mHandler.obtainMessage(3, this.mRow), 1000);
            }
        }
    }

    static /* synthetic */ void lambda$updateRowsH$19() {
    }

    static /* synthetic */ void lambda$updateRowsH$20() {
    }

    public VolumeDialogImpl(Context context) {
        this.mContext = new ContextThemeWrapper(context, R$style.qs_theme);
        this.mController = (VolumeDialogController) Dependency.get(VolumeDialogController.class);
        this.mKeyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mAccessibilityMgr = (AccessibilityManagerWrapper) Dependency.get(AccessibilityManagerWrapper.class);
        this.mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mShowActiveStreamOnly = showActiveStreamOnly();
        this.mHasSeenODICaptionsTooltip = Prefs.getBoolean(context, "HasSeenODICaptionsTooltip", false);
        this.mClickOutputChooser = new OnClickListener() {
            public void onClick(View view) {
                VolumeDialogImpl.this.dismissH(8);
                VolumeDialogImpl.this.showOutputChooserH();
            }
        };
        loadOpDimens();
        if (OpUtils.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Vol Controller, mOpBeforeExpandWidth:");
            sb.append(this.mOpBeforeExpandWidth);
            sb.append(", mOpafterExpandWidth:");
            sb.append(this.mOpafterExpandWidth);
            Log.i(str, sb.toString());
        }
        this.mDeviceInfo = new DeviceInfo();
    }

    public void onUiModeChanged() {
        this.mContext.getTheme().applyStyle(this.mContext.getThemeResId(), true);
    }

    public void init(int i, Callback callback) {
        initDialog();
        this.mAccessibility.init();
        this.mController.addCallback(this.mControllerCallbackH, this.mHandler);
        this.mController.getState();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void destroy() {
        this.mController.removeCallback(this.mControllerCallbackH);
        this.mHandler.removeCallbacksAndMessages(null);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    private void initDialog() {
        this.mDialog = new CustomDialog(this.mContext);
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
        this.mHovering = false;
        this.mShowing = false;
        this.mWindow = this.mDialog.getWindow();
        this.mWindow.requestFeature(1);
        this.mWindow.setBackgroundDrawable(new ColorDrawable(0));
        this.mWindow.clearFlags(65538);
        this.mWindow.addFlags(17563944);
        this.mWindow.setType(2020);
        this.mWindow.setWindowAnimations(16973828);
        LayoutParams attributes = this.mWindow.getAttributes();
        attributes.format = -3;
        attributes.setTitle(VolumeDialogImpl.class.getSimpleName());
        attributes.gravity = isLandscape() ? 21 : 19;
        attributes.windowAnimations = -1;
        this.mWindow.setAttributes(attributes);
        this.mWindow.setLayout(-2, -2);
        this.mDialog.setContentView(R$layout.op_volume_dialog);
        this.mDialogView = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog);
        this.mDialogUpper = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_upper);
        this.mDialogLower = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_lower);
        this.mDialogView.setAlpha(0.0f);
        this.mDialog.setCanceledOnTouchOutside(true);
        this.mDialog.setOnShowListener(new OnShowListener() {
            public final void onShow(DialogInterface dialogInterface) {
                VolumeDialogImpl.this.lambda$initDialog$1$VolumeDialogImpl(dialogInterface);
            }
        });
        this.mDialogView.setOnHoverListener(new OnHoverListener() {
            public final boolean onHover(View view, MotionEvent motionEvent) {
                return VolumeDialogImpl.this.lambda$initDialog$2$VolumeDialogImpl(view, motionEvent);
            }
        });
        this.mDialogView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    Log.i(VolumeDialogImpl.TAG, "DialogView, onTouch");
                }
                return true;
            }
        });
        this.mDialogRowContainer = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_row_container);
        this.mDialogRowsView = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_rows);
        this.mRinger = (ViewGroup) this.mDialog.findViewById(R$id.ringer);
        ViewGroup viewGroup = this.mRinger;
        if (viewGroup != null) {
            this.mRingerIcon = (ImageButton) viewGroup.findViewById(R$id.ringer_icon);
            this.mZenIcon = (FrameLayout) this.mRinger.findViewById(R$id.dnd_icon);
        }
        this.mODICaptionsView = (ViewGroup) this.mDialog.findViewById(R$id.odi_captions);
        ViewGroup viewGroup2 = this.mODICaptionsView;
        if (viewGroup2 != null) {
            this.mODICaptionsIcon = (CaptionsToggleImageButton) viewGroup2.findViewById(R$id.odi_captions_icon);
        }
        this.mODICaptionsTooltipViewStub = (ViewStub) this.mDialog.findViewById(R$id.odi_captions_tooltip_stub);
        if (this.mHasSeenODICaptionsTooltip) {
            ViewStub viewStub = this.mODICaptionsTooltipViewStub;
            if (viewStub != null) {
                this.mDialogView.removeView(viewStub);
                this.mODICaptionsTooltipViewStub = null;
            }
        }
        if (this.mODICaptionsView != null) {
            if (OpUtils.DEBUG_ONEPLUS) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("initDialog, mODICaptionsView:");
                sb.append(this.mODICaptionsView.getVisibility());
                sb.append(", mIsCaptionComponentEnabled:");
                sb.append(this.mIsCaptionComponentEnabled);
                Log.i(str, sb.toString());
            }
            updateODICaptionsH(this.mIsCaptionComponentEnabled, false);
        }
        this.mSettingsView = this.mDialog.findViewById(R$id.settings_container);
        this.mSettingsIcon = (ImageButton) this.mDialog.findViewById(R$id.settings);
        this.mOutputChooser = (ImageButton) this.mDialogView.findViewById(R$id.output_chooser);
        this.mOutputChooser.setOnClickListener(this.mClickOutputChooser);
        this.mConnectedDevice = (TextView) this.mDialogView.findViewById(R$id.volume_row_connected_device);
        this.mSettingsBackView = this.mDialog.findViewById(R$id.settings_back_container);
        this.mSettingsBackIcon = (ImageButton) this.mDialog.findViewById(R$id.settings_back);
        this.mSettingsOpSettingsView = this.mDialog.findViewById(R$id.settings_opsettings_container);
        this.mSettingsOpSettingsIcon = (ImageButton) this.mDialog.findViewById(R$id.settings_opsettings);
        this.mOutputChooserExpandIcon = (ImageView) this.mDialogView.findViewById(R$id.output_active_device_icon);
        this.mOutputChooserExpandEditText = (EditText) this.mDialogView.findViewById(R$id.output_active_device_name);
        this.mOutputChooserExpandTextView = (TextView) this.mDialogView.findViewById(R$id.output_active_device_name_marquee);
        final ViewGroup viewGroup3 = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_container);
        viewGroup3.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    Log.i(VolumeDialogImpl.TAG, "volume_dialog_container, onTouch");
                }
                if (VolumeDialogImpl.this.isLandscape() && ((int) motionEvent.getX()) < viewGroup3.getLayoutParams().width - VolumeDialogImpl.this.mOpBeforeExpandWidth) {
                    VolumeDialogImpl.this.dismissH(1);
                }
                return true;
            }
        });
        this.mOutputChooserBackgroundView = this.mDialog.findViewById(R$id.output_chooser_background_container);
        this.mOutputChooserBackgroundView.setOnClickListener(this.mClickOutputChooser);
        if (this.mRows.isEmpty()) {
            if (!AudioSystem.isSingleVolume(this.mContext)) {
                int i = R$drawable.ic_volume_accessibility;
                addRow(10, i, i, true, false);
            }
            addRow(3, R$drawable.ic_volume_media, R$drawable.ic_volume_media_mute, true, true);
            if (!AudioSystem.isSingleVolume(this.mContext)) {
                addRow(2, R$drawable.ic_volume_ringer, R$drawable.ic_volume_ringer_mute, true, false);
                addRow(4, R$drawable.ic_alarm, R$drawable.ic_volume_alarm_mute, true, false);
                addRow(0, 17302767, 17302767, false, false);
                int i2 = R$drawable.ic_volume_bt_sco;
                addRow(6, i2, i2, false, false);
                addRow(1, R$drawable.ic_volume_system, R$drawable.ic_volume_system_mute, false, false);
            }
        } else {
            addExistingRows();
        }
        updateRowsH(getActiveRow());
        initRingerH();
        initSettingsH();
        initODICaptionsH();
        this.mAccentColor = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        this.mController.getState();
        applyColorTheme(true);
        ViewGroup viewGroup4 = this.mODICaptionsView;
        if (viewGroup4 != null && viewGroup4.getVisibility() == 0) {
            updateODIRelatedLayout();
        }
    }

    public /* synthetic */ void lambda$initDialog$1$VolumeDialogImpl(DialogInterface dialogInterface) {
        if (isLandscape()) {
            ViewGroup viewGroup = this.mDialogView;
            viewGroup.setTranslationX(((float) viewGroup.getWidth()) / 2.0f);
        } else {
            ViewGroup viewGroup2 = this.mDialogView;
            viewGroup2.setTranslationX(((float) (-viewGroup2.getWidth())) / 2.0f);
        }
        this.mDialogView.setAlpha(0.0f);
        this.mDialogView.animate().alpha(1.0f).translationX(0.0f).setDuration(300).setInterpolator(new SystemUIInterpolators$LogDecelerateInterpolator()).withEndAction(new Runnable() {
            public final void run() {
                VolumeDialogImpl.this.lambda$initDialog$0$VolumeDialogImpl();
            }
        }).start();
    }

    public /* synthetic */ void lambda$initDialog$0$VolumeDialogImpl() {
        if (!Prefs.getBoolean(this.mContext, "TouchedRingerToggle", false)) {
            ImageButton imageButton = this.mRingerIcon;
            if (imageButton != null) {
                imageButton.postOnAnimationDelayed(getSinglePressFor(imageButton), 1500);
            }
        }
    }

    public /* synthetic */ boolean lambda$initDialog$2$VolumeDialogImpl(View view, MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        this.mHovering = actionMasked == 9 || actionMasked == 7;
        rescheduleTimeoutH();
        return true;
    }

    private int getAlphaAttr(int i) {
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{i});
        float f = obtainStyledAttributes.getFloat(0, 0.0f);
        obtainStyledAttributes.recycle();
        return (int) (f * 255.0f);
    }

    /* access modifiers changed from: private */
    public boolean isLandscape() {
        return this.mContext.getResources().getConfiguration().orientation == 2;
    }

    public void setStreamImportant(int i, boolean z) {
        this.mHandler.obtainMessage(5, i, z ? 1 : 0).sendToTarget();
    }

    public void setAutomute(boolean z) {
        if (this.mAutomute != z) {
            this.mAutomute = z;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void setSilentMode(boolean z) {
        if (this.mSilentMode != z) {
            this.mSilentMode = z;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    private void addRow(int i, int i2, int i3, boolean z, boolean z2) {
        addRow(i, i2, i3, z, z2, false);
    }

    private void addRow(int i, int i2, int i3, boolean z, boolean z2, boolean z3) {
        if (C1684D.BUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Adding row for stream ");
            sb.append(i);
            Slog.d(str, sb.toString());
        }
        VolumeRow volumeRow = new VolumeRow();
        initRow(volumeRow, i, i2, i3, z, z2);
        this.mDialogRowsView.addView(volumeRow.view);
        this.mRows.add(volumeRow);
    }

    private void addExistingRows() {
        int size = this.mRows.size();
        for (int i = 0; i < size; i++) {
            VolumeRow volumeRow = (VolumeRow) this.mRows.get(i);
            initRow(volumeRow, volumeRow.stream, volumeRow.iconRes, volumeRow.iconMuteRes, volumeRow.important, volumeRow.defaultStream);
            this.mDialogRowsView.addView(volumeRow.view);
            updateVolumeRowH(volumeRow);
        }
    }

    /* access modifiers changed from: private */
    public VolumeRow getActiveRow() {
        for (VolumeRow volumeRow : this.mRows) {
            if (volumeRow.stream == this.mActiveStream) {
                return volumeRow;
            }
        }
        for (VolumeRow volumeRow2 : this.mRows) {
            if (volumeRow2.stream == 3) {
                return volumeRow2;
            }
        }
        return (VolumeRow) this.mRows.get(0);
    }

    private VolumeRow findRow(int i) {
        for (VolumeRow volumeRow : this.mRows) {
            if (volumeRow.stream == i) {
                return volumeRow;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public static int getImpliedLevel(SeekBar seekBar, int i) {
        int max = seekBar.getMax();
        int i2 = max / 100;
        int i3 = i2 - 1;
        if (i == 0) {
            return 0;
        }
        return i == max ? i2 : ((int) ((((float) i) / ((float) max)) * ((float) i3))) + 1;
    }

    @SuppressLint({"InflateParams"})
    private void initRow(VolumeRow volumeRow, int i, int i2, int i3, boolean z, boolean z2) {
        volumeRow.stream = i;
        volumeRow.iconRes = i2;
        volumeRow.iconMuteRes = i3;
        volumeRow.important = z;
        volumeRow.defaultStream = z2;
        volumeRow.view = this.mDialog.getLayoutInflater().inflate(R$layout.op_volume_dialog_row, null);
        volumeRow.view.setId(volumeRow.stream);
        volumeRow.view.setTag(volumeRow);
        volumeRow.header = (TextView) volumeRow.view.findViewById(R$id.volume_row_header);
        volumeRow.header.setId(volumeRow.stream * 20);
        if (i == 10) {
            volumeRow.header.setFilters(new InputFilter[]{new LengthFilter(13)});
        }
        volumeRow.dndIcon = (FrameLayout) volumeRow.view.findViewById(R$id.dnd_icon);
        volumeRow.slider = (SeekBar) volumeRow.view.findViewById(R$id.volume_row_slider);
        volumeRow.slider.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(volumeRow));
        volumeRow.anim = null;
        volumeRow.icon = (ImageButton) volumeRow.view.findViewById(R$id.volume_row_icon);
        volumeRow.icon.setImageResource(i2);
        if (volumeRow.stream != 10) {
            volumeRow.icon.setOnClickListener(new OnClickListener(volumeRow, i) {
                private final /* synthetic */ VolumeRow f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void onClick(View view) {
                    VolumeDialogImpl.this.lambda$initRow$3$VolumeDialogImpl(this.f$1, this.f$2, view);
                }
            });
        } else {
            volumeRow.icon.setImportantForAccessibility(2);
        }
    }

    public /* synthetic */ void lambda$initRow$3$VolumeDialogImpl(VolumeRow volumeRow, int i, View view) {
        boolean z = false;
        Events.writeEvent(this.mContext, 7, Integer.valueOf(volumeRow.stream), Integer.valueOf(volumeRow.iconState));
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("initRow setOnClickListener row.stream:");
            sb.append(volumeRow.stream);
            sb.append(", row.iconState:");
            sb.append(volumeRow.iconState);
            sb.append(", row.ss.level:");
            sb.append(volumeRow.f122ss.level);
            sb.append(", row.ss.levelMin:");
            sb.append(volumeRow.f122ss.levelMin);
            sb.append(", row.lastAudibleLevel:");
            sb.append(volumeRow.lastAudibleLevel);
            Log.i(str, sb.toString());
        }
        this.mController.setActiveStream(volumeRow.stream);
        if (volumeRow.stream != 2) {
            StreamState streamState = volumeRow.f122ss;
            if (streamState.level == streamState.levelMin) {
                z = true;
            }
            this.mController.setStreamVolume(i, z ? volumeRow.lastAudibleLevel : volumeRow.f122ss.levelMin);
        } else if (volumeRow.f122ss.level == 1) {
            this.mController.setStreamVolume(i, volumeRow.lastAudibleLevel);
        } else {
            this.mController.setStreamVolume(i, 1);
        }
        volumeRow.userAttempt = 0;
    }

    public void initSettingsH() {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.i(TAG, "initSettingsH");
        }
        long j = 0;
        long j2 = 275;
        int i = 0;
        float f = 0.5f;
        float f2 = 0.0f;
        if (this.mSettingsView != null) {
            int i2 = (!this.mDeviceProvisionedController.isCurrentUserSetup() || this.mActivityManager.getLockTaskModeState() != 0 || this.mOpForceExpandState) ? 8 : 0;
            if (OpUtils.DEBUG_ONEPLUS) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("initSettingsH, targetBottomSettingsIconVisible: ");
                sb.append(i2);
                sb.append(", mTargetBottomSettingsIconVisible:");
                sb.append(this.mTargetBottomSettingsIconVisible);
                Log.i(str, sb.toString());
            }
            if (this.mTargetBottomSettingsIconVisible != i2) {
                this.mTargetBottomSettingsIconVisible = i2;
                if (i2 == 0) {
                    this.mSettingsView.setVisibility(0);
                }
                this.mSettingsView.animate().alpha(i2 == 8 ? 0.0f : 1.0f).scaleX(i2 == 8 ? 0.5f : 1.0f).scaleY(i2 == 8 ? 0.5f : 1.0f).setDuration(275).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction(new Runnable(i2) {
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        VolumeDialogImpl.this.lambda$initSettingsH$4$VolumeDialogImpl(this.f$1);
                    }
                }).setStartDelay(0);
            } else {
                this.mSettingsView.setVisibility(i2);
                this.mSettingsView.setAlpha(i2 == 8 ? 0.0f : 1.0f);
            }
        }
        ImageButton imageButton = this.mSettingsIcon;
        if (imageButton != null) {
            imageButton.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    VolumeDialogImpl.this.lambda$initSettingsH$5$VolumeDialogImpl(view);
                }
            });
        }
        if (this.mSettingsBackView != null) {
            int i3 = this.mOpForceExpandState ? 0 : 8;
            if (this.mTargetBottomSettingsBackIconVisible != i3) {
                this.mTargetBottomSettingsBackIconVisible = i3;
                if (i3 == 0) {
                    this.mSettingsBackView.setVisibility(0);
                }
                this.mSettingsBackView.animate().alpha(i3 == 8 ? 0.0f : 1.0f).scaleX(i3 == 8 ? 0.5f : 1.0f).scaleY(i3 == 8 ? 0.5f : 1.0f).setDuration(275).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction(new Runnable(i3) {
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        VolumeDialogImpl.this.lambda$initSettingsH$6$VolumeDialogImpl(this.f$1);
                    }
                }).setStartDelay(0);
            } else {
                this.mSettingsBackView.setVisibility(i3);
                this.mSettingsBackView.setAlpha(i3 == 8 ? 0.0f : 1.0f);
            }
        }
        if (this.mSettingsBackIcon != null) {
            if (isLandscape()) {
                this.mSettingsBackIcon.setRotation(180.0f);
            } else {
                this.mSettingsBackIcon.setRotation(0.0f);
            }
            this.mSettingsBackIcon.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    VolumeDialogImpl.this.lambda$initSettingsH$7$VolumeDialogImpl(view);
                }
            });
        }
        if (this.mSettingsOpSettingsView != null) {
            if (!this.mOpForceExpandState) {
                i = 8;
            }
            if (this.mTargetBottomSettingsOpSettingsIconVisible != i) {
                this.mTargetBottomSettingsOpSettingsIconVisible = i;
                ViewPropertyAnimator scaleX = this.mSettingsOpSettingsView.animate().alpha(i == 8 ? 0.0f : 1.0f).scaleX(i == 8 ? 0.5f : 1.0f);
                if (i != 8) {
                    f = 1.0f;
                }
                ViewPropertyAnimator scaleY = scaleX.scaleY(f);
                if (!this.mOpForceExpandState) {
                    j2 = 120;
                }
                ViewPropertyAnimator withEndAction = scaleY.setDuration(j2).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withStartAction(new Runnable(i) {
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        VolumeDialogImpl.this.lambda$initSettingsH$8$VolumeDialogImpl(this.f$1);
                    }
                }).withEndAction(new Runnable(i) {
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        VolumeDialogImpl.this.lambda$initSettingsH$9$VolumeDialogImpl(this.f$1);
                    }
                });
                if (this.mOpForceExpandState) {
                    j = 91;
                }
                withEndAction.setStartDelay(j);
            } else {
                this.mSettingsOpSettingsView.setVisibility(i);
                View view = this.mSettingsOpSettingsView;
                if (i != 8) {
                    f2 = 1.0f;
                }
                view.setAlpha(f2);
            }
        }
        ImageButton imageButton2 = this.mSettingsOpSettingsIcon;
        if (imageButton2 != null) {
            imageButton2.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    VolumeDialogImpl.this.lambda$initSettingsH$10$VolumeDialogImpl(view);
                }
            });
        }
    }

    public /* synthetic */ void lambda$initSettingsH$4$VolumeDialogImpl(int i) {
        this.mSettingsView.setVisibility(i);
        this.mSettingsView.setAlpha(i == 8 ? 0.0f : 1.0f);
    }

    public /* synthetic */ void lambda$initSettingsH$5$VolumeDialogImpl(View view) {
        Events.writeEvent(this.mContext, 8, new Object[0]);
        if (Build.DEBUG_ONEPLUS) {
            Log.i(TAG, "mSettingsIcon click");
        }
        if (this.mAnimVol == null || this.mIsExpandAnimDone) {
            boolean z = this.mOpForceExpandState;
            if (!z) {
                this.mOpLastforceExpandState = z;
                this.mOpForceExpandState = true;
                this.mNeedPlayExpandAnim = true;
                setDialogWidth(this.mOpafterExpandWidth);
                updateRowsH(getActiveRow());
            }
        }
    }

    public /* synthetic */ void lambda$initSettingsH$6$VolumeDialogImpl(int i) {
        this.mSettingsBackView.setVisibility(i);
        this.mSettingsBackView.setAlpha(i == 8 ? 0.0f : 1.0f);
    }

    public /* synthetic */ void lambda$initSettingsH$7$VolumeDialogImpl(View view) {
        if (Build.DEBUG_ONEPLUS) {
            Log.i(TAG, "mSettingsBackIcon click");
        }
        if (this.mAnimVol == null || this.mIsExpandAnimDone) {
            boolean z = this.mOpForceExpandState;
            if (z) {
                this.mOpLastforceExpandState = z;
                this.mOpForceExpandState = false;
                this.mNeedPlayExpandAnim = true;
                setDialogWidth(this.mOpafterExpandWidth);
                updateRowsH(getActiveRow());
            }
        }
    }

    public /* synthetic */ void lambda$initSettingsH$8$VolumeDialogImpl(int i) {
        if (i == 0) {
            this.mSettingsOpSettingsView.setVisibility(0);
        }
    }

    public /* synthetic */ void lambda$initSettingsH$9$VolumeDialogImpl(int i) {
        this.mSettingsOpSettingsView.setVisibility(i);
        this.mSettingsOpSettingsView.setAlpha(i == 8 ? 0.0f : 1.0f);
    }

    public /* synthetic */ void lambda$initSettingsH$10$VolumeDialogImpl(View view) {
        if (Build.DEBUG_ONEPLUS) {
            Log.i(TAG, "mSettingsOpSettingsIcon click");
        }
        if ((this.mAnimVol == null || this.mIsExpandAnimDone) && this.mOpForceExpandState) {
            Intent intent = new Intent("android.settings.SOUND_SETTINGS");
            dismissH(5);
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).startActivity(intent, true);
        }
    }

    public void initRingerH() {
        ImageButton imageButton = this.mRingerIcon;
        if (imageButton != null) {
            imageButton.setAccessibilityLiveRegion(1);
            this.mRingerIcon.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    VolumeDialogImpl.this.lambda$initRingerH$11$VolumeDialogImpl(view);
                }
            });
        }
        updateRingerH();
    }

    public /* synthetic */ void lambda$initRingerH$11$VolumeDialogImpl(View view) {
        Prefs.putBoolean(this.mContext, "TouchedRingerToggle", true);
        int i = 2;
        StreamState streamState = (StreamState) this.mState.states.get(2);
        if (streamState != null) {
            boolean hasVibrator = this.mController.hasVibrator();
            int i2 = this.mState.ringerModeInternal;
            if (i2 == 2) {
                if (hasVibrator) {
                    i = 1;
                    Events.writeEvent(this.mContext, 18, Integer.valueOf(i));
                    incrementManualToggleCount();
                    updateRingerH();
                    provideTouchFeedbackH(i);
                    this.mController.setRingerMode(i, false);
                    maybeShowToastH(i);
                }
            } else if (i2 != 1) {
                if (streamState.level == 0) {
                    this.mController.setStreamVolume(2, 1);
                }
                Events.writeEvent(this.mContext, 18, Integer.valueOf(i));
                incrementManualToggleCount();
                updateRingerH();
                provideTouchFeedbackH(i);
                this.mController.setRingerMode(i, false);
                maybeShowToastH(i);
            }
            i = 0;
            Events.writeEvent(this.mContext, 18, Integer.valueOf(i));
            incrementManualToggleCount();
            updateRingerH();
            provideTouchFeedbackH(i);
            this.mController.setRingerMode(i, false);
            maybeShowToastH(i);
        }
    }

    private void initODICaptionsH() {
        CaptionsToggleImageButton captionsToggleImageButton = this.mODICaptionsIcon;
        if (captionsToggleImageButton != null) {
            captionsToggleImageButton.setOnConfirmedTapListener(new ConfirmedTapListener() {
                public final void onConfirmedTap() {
                    VolumeDialogImpl.this.lambda$initODICaptionsH$12$VolumeDialogImpl();
                }
            }, this.mHandler);
        }
        this.mController.getCaptionsComponentState(false);
    }

    public /* synthetic */ void lambda$initODICaptionsH$12$VolumeDialogImpl() {
        onCaptionIconClicked();
        Events.writeEvent(this.mContext, 21, new Object[0]);
    }

    private void checkODICaptionsTooltip(boolean z) {
        if (!this.mHasSeenODICaptionsTooltip && !z && this.mODICaptionsTooltipViewStub != null) {
            this.mController.getCaptionsComponentState(true);
        } else if (this.mHasSeenODICaptionsTooltip && z && this.mODICaptionsTooltipView != null) {
            hideCaptionsTooltip();
        }
    }

    /* access modifiers changed from: protected */
    public void showCaptionsTooltip() {
        if (!this.mHasSeenODICaptionsTooltip) {
            ViewStub viewStub = this.mODICaptionsTooltipViewStub;
            if (viewStub != null) {
                this.mODICaptionsTooltipView = viewStub.inflate();
                this.mODICaptionsTooltipView.findViewById(R$id.dismiss).setOnClickListener(new OnClickListener() {
                    public final void onClick(View view) {
                        VolumeDialogImpl.this.lambda$showCaptionsTooltip$13$VolumeDialogImpl(view);
                    }
                });
                this.mODICaptionsTooltipViewStub = null;
                rescheduleTimeoutH();
            }
        }
        View view = this.mODICaptionsTooltipView;
        if (view != null) {
            view.setAlpha(0.0f);
            this.mODICaptionsTooltipView.animate().alpha(1.0f).setStartDelay(300).withEndAction(new Runnable() {
                public final void run() {
                    VolumeDialogImpl.this.lambda$showCaptionsTooltip$14$VolumeDialogImpl();
                }
            }).start();
        }
    }

    public /* synthetic */ void lambda$showCaptionsTooltip$13$VolumeDialogImpl(View view) {
        hideCaptionsTooltip();
        Events.writeEvent(this.mContext, 22, new Object[0]);
    }

    public /* synthetic */ void lambda$showCaptionsTooltip$14$VolumeDialogImpl() {
        if (C1684D.BUG) {
            Log.d(TAG, "tool:checkODICaptionsTooltip() putBoolean true");
        }
        Prefs.putBoolean(this.mContext, "HasSeenODICaptionsTooltip", true);
        this.mHasSeenODICaptionsTooltip = true;
        CaptionsToggleImageButton captionsToggleImageButton = this.mODICaptionsIcon;
        if (captionsToggleImageButton != null) {
            captionsToggleImageButton.postOnAnimation(getSinglePressFor(captionsToggleImageButton));
        }
    }

    private void hideCaptionsTooltip() {
        View view = this.mODICaptionsTooltipView;
        if (view != null && view.getVisibility() == 0) {
            this.mODICaptionsTooltipView.animate().cancel();
            this.mODICaptionsTooltipView.setAlpha(1.0f);
            this.mODICaptionsTooltipView.animate().alpha(0.0f).setStartDelay(0).setDuration(250).withEndAction(new Runnable() {
                public final void run() {
                    VolumeDialogImpl.this.lambda$hideCaptionsTooltip$15$VolumeDialogImpl();
                }
            }).start();
        }
    }

    public /* synthetic */ void lambda$hideCaptionsTooltip$15$VolumeDialogImpl() {
        this.mODICaptionsTooltipView.setVisibility(4);
    }

    /* access modifiers changed from: protected */
    public void tryToRemoveCaptionsTooltip() {
        if (this.mHasSeenODICaptionsTooltip && this.mODICaptionsTooltipView != null) {
            ((ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_container)).removeView(this.mODICaptionsTooltipView);
            this.mODICaptionsTooltipView = null;
        }
    }

    /* access modifiers changed from: private */
    public void updateODICaptionsH(boolean z, boolean z2) {
        if (this.mODICaptionsView != null) {
            if (z) {
                updateODIRelatedLayout();
            }
            this.mODICaptionsView.setVisibility(z ? 0 : 8);
        }
        if (z) {
            updateCaptionsIcon();
            if (z2) {
                showCaptionsTooltip();
            }
        }
    }

    private void updateCaptionsIcon() {
        boolean areCaptionsEnabled = this.mController.areCaptionsEnabled();
        if (this.mODICaptionsIcon.getCaptionsEnabled() != areCaptionsEnabled) {
            this.mHandler.post(this.mODICaptionsIcon.setCaptionsEnabled(areCaptionsEnabled));
        }
        boolean isCaptionStreamOptedOut = this.mController.isCaptionStreamOptedOut();
        if (this.mODICaptionsIcon.getOptedOut() != isCaptionStreamOptedOut) {
            this.mHandler.post(new Runnable(isCaptionStreamOptedOut) {
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    VolumeDialogImpl.this.lambda$updateCaptionsIcon$16$VolumeDialogImpl(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$updateCaptionsIcon$16$VolumeDialogImpl(boolean z) {
        this.mODICaptionsIcon.setOptedOut(z);
    }

    private void onCaptionIconClicked() {
        this.mController.setCaptionsEnabled(!this.mController.areCaptionsEnabled());
        updateCaptionsIcon();
    }

    private void incrementManualToggleCount() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str = "manual_ringer_toggle_count";
        Secure.putInt(contentResolver, str, Secure.getInt(contentResolver, str, 0) + 1);
    }

    private void provideTouchFeedbackH(int i) {
        VibrationEffect vibrationEffect;
        if (i == 0) {
            vibrationEffect = VibrationEffect.get(0);
        } else if (i != 2) {
            vibrationEffect = VibrationEffect.get(1);
        } else {
            this.mController.scheduleTouchFeedback();
            vibrationEffect = null;
        }
        if (vibrationEffect != null) {
            this.mController.vibrate(vibrationEffect);
        }
    }

    private void maybeShowToastH(int i) {
        String str = "RingerGuidanceCount";
        int i2 = Prefs.getInt(this.mContext, str, 0);
        if (i2 <= 12) {
            String str2 = null;
            if (i == 0) {
                str2 = this.mContext.getString(17041216);
            } else if (i != 2) {
                str2 = this.mContext.getString(17041217);
            } else {
                StreamState streamState = (StreamState) this.mState.states.get(2);
                if (streamState != null) {
                    str2 = this.mContext.getString(R$string.volume_dialog_ringer_guidance_ring, new Object[]{Utils.formatPercentage((long) streamState.level, (long) streamState.levelMax)});
                }
            }
            Toast.makeText(this.mContext, str2, 0).show();
            Prefs.putInt(this.mContext, str, i2 + 1);
        }
    }

    /* access modifiers changed from: private */
    public void showH(int i) {
        boolean z;
        if (Build.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("showH r=");
            sb.append(Events.SHOW_REASONS[i]);
            sb.append(", pending=");
            sb.append(this.mPendingInit);
            Log.d(str, sb.toString());
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        rescheduleTimeoutH();
        if (this.mConfigChanged) {
            initDialog();
            this.mConfigurableTexts.update();
            this.mConfigChanged = false;
            z = true;
        } else {
            z = false;
        }
        if (this.mDialog != null && this.mPendingInit) {
            if (!z) {
                initDialog();
                z = true;
            }
            this.mPendingInit = false;
        }
        if (i == 4 && !z) {
            initDialog();
        }
        initSettingsH();
        this.mShowing = true;
        applyColorTheme(false);
        this.mDialog.show();
        Events.writeEvent(this.mContext, 0, Integer.valueOf(i), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
        this.mController.notifyVisible(true);
        this.mController.getCaptionsComponentState(false);
        checkODICaptionsTooltip(false);
        if (this.mOpOutputChooser == null) {
            this.mOpOutputChooser = new OpOutputChooser(this.mContext);
            this.mOpOutputChooser.addCallback(this.mOutputChooserCallback);
        }
        if (isLandscape()) {
            setDialogWidth(this.mOpafterExpandWidth);
        } else {
            setDialogWidth(-2);
        }
        this.mFirstTimeInitDialog = false;
    }

    /* access modifiers changed from: protected */
    public void rescheduleTimeoutH() {
        this.mHandler.removeMessages(2);
        int computeTimeoutH = computeTimeoutH();
        C1713H h = this.mHandler;
        h.sendMessageDelayed(h.obtainMessage(2, 3, 0), (long) computeTimeoutH);
        if (Build.DEBUG_ONEPLUS || C1684D.BUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("rescheduleTimeout ");
            sb.append(computeTimeoutH);
            sb.append(" ");
            sb.append(Debug.getCaller());
            Log.d(str, sb.toString());
        }
        this.mController.userActivity();
    }

    private int computeTimeoutH() {
        if (this.mHovering) {
            return this.mAccessibilityMgr.getRecommendedTimeoutMillis(16000, 4);
        }
        if (this.mSafetyWarning != null) {
            return this.mAccessibilityMgr.getRecommendedTimeoutMillis(5000, 6);
        }
        if (!this.mHasSeenODICaptionsTooltip && this.mODICaptionsTooltipView != null) {
            return this.mAccessibilityMgr.getRecommendedTimeoutMillis(5000, 6);
        }
        if (!isStatusBarShowing()) {
            return 1500;
        }
        return this.mAccessibilityMgr.getRecommendedTimeoutMillis(3000, 4);
    }

    /* access modifiers changed from: protected */
    public void dismissH(int i) {
        if (C1684D.BUG || OpUtils.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("mDialog.dismiss() reason: ");
            sb.append(Events.DISMISS_REASONS[i]);
            sb.append(" from: ");
            sb.append(Debug.getCaller());
            Log.d(str, sb.toString());
        }
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
        this.mDialogView.animate().cancel();
        if (this.mShowing) {
            this.mShowing = false;
            Events.writeEvent(this.mContext, 1, Integer.valueOf(i));
        }
        this.mDialogView.setTranslationX(0.0f);
        this.mDialogView.setAlpha(1.0f);
        this.mDialogView.animate().alpha(0.0f).translationX((float) ((isLandscape() ? this.mDialogView.getWidth() : -this.mDialogView.getWidth()) / 2)).setDuration(250).setInterpolator(new SystemUIInterpolators$LogAccelerateInterpolator()).withEndAction(new Runnable() {
            public final void run() {
                VolumeDialogImpl.this.lambda$dismissH$18$VolumeDialogImpl();
            }
        }).start();
        checkODICaptionsTooltip(true);
        synchronized (this.mSafetyWarningLock) {
            if (this.mSafetyWarning != null) {
                if (C1684D.BUG) {
                    Log.d(TAG, "SafetyWarning dismissed");
                }
                this.mSafetyWarning.dismiss();
            }
        }
    }

    public /* synthetic */ void lambda$dismissH$18$VolumeDialogImpl() {
        this.mHandler.postDelayed(new Runnable() {
            public final void run() {
                VolumeDialogImpl.this.lambda$dismissH$17$VolumeDialogImpl();
            }
        }, 50);
    }

    public /* synthetic */ void lambda$dismissH$17$VolumeDialogImpl() {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.i(TAG, "dismissH withEndAction");
        }
        setExpandFeautureDismissState();
        this.mDialog.dismiss();
        this.mController.notifyVisible(false);
        tryToRemoveCaptionsTooltip();
    }

    private boolean showActiveStreamOnly() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback") || this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.television");
    }

    private boolean shouldBeVisibleH(VolumeRow volumeRow, VolumeRow volumeRow2) {
        boolean z = false;
        if (volumeRow.stream == volumeRow2.stream) {
            return true;
        }
        if (!this.mShowActiveStreamOnly) {
            int i = volumeRow.stream;
            if (i == 10) {
                return this.mShowA11yStream;
            }
            if (volumeRow2.stream == 10 && i == this.mPrevActiveStream) {
                return true;
            }
            if (volumeRow.defaultStream) {
                int i2 = volumeRow2.stream;
                if (i2 == 2 || i2 == 4 || i2 == 0 || i2 == 10 || this.mDynamic.get(i2)) {
                    z = true;
                }
            }
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void updateRowsH(VolumeRow volumeRow) {
        boolean z;
        boolean z2;
        boolean z3;
        VolumeRow volumeRow2 = volumeRow;
        if (C1684D.BUG) {
            Log.d(TAG, "updateRowsH");
        }
        if (!this.mShowing) {
            trimObsoleteH();
        }
        if (OpUtils.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateRowsH, mOpForceExpandState:");
            sb.append(this.mOpForceExpandState);
            sb.append(", mOpLastforceExpandState:");
            sb.append(this.mOpLastforceExpandState);
            sb.append(", mNeedPlayExpandAnim:");
            sb.append(this.mNeedPlayExpandAnim);
            sb.append(", mFirstTimeInitDialog:");
            sb.append(this.mFirstTimeInitDialog);
            sb.append(", activeRow:");
            sb.append(volumeRow2.stream);
            Log.i(str, sb.toString());
        }
        boolean z4 = this.mOpForceExpandState;
        int i = 2;
        int i2 = 3;
        if (z4) {
            for (VolumeRow volumeRow3 : this.mRows) {
                int i3 = volumeRow3.stream;
                if (i3 == i || i3 == 4 || i3 == i2) {
                    if (!this.mNeedPlayExpandAnim || this.mFirstTimeInitDialog) {
                        Util.setVisOrGone(volumeRow3.view, true);
                    } else {
                        int i4 = volumeRow3.stream;
                        if (i4 == i) {
                            volumeRow3.view.setAlpha(0.0f);
                            Util.setVisOrGone(volumeRow3.view, true);
                            volumeRow3.view.setTranslationX((float) ((-this.mContext.getResources().getDimensionPixelSize(R$dimen.op_volume_dialog_panel_width)) / i2));
                            volumeRow3.view.animate().alpha(1.0f).translationX(0.0f).setDuration(275).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction($$Lambda$VolumeDialogImpl$rNQvPZ4v1mumin0xPzfWijGPmaI.INSTANCE).setStartDelay(30);
                        } else if (i4 == 4) {
                            volumeRow3.view.setAlpha(0.0f);
                            Util.setVisOrGone(volumeRow3.view, true);
                            volumeRow3.view.setTranslationX((float) ((-this.mContext.getResources().getDimensionPixelSize(R$dimen.op_volume_dialog_panel_width)) / i2));
                            volumeRow3.view.animate().alpha(1.0f).translationX(0.0f).setDuration(275).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction($$Lambda$VolumeDialogImpl$m26Kv58I4TUt4m4TU2mNQQsVCVc.INSTANCE).setStartDelay(60);
                        } else {
                            Util.setVisOrGone(volumeRow3.view, true);
                        }
                    }
                    if (volumeRow3.view.isShown()) {
                        updateVolumeRowTintH(volumeRow3, true, true);
                    }
                }
                i = 2;
                i2 = 3;
            }
            if (this.mNeedPlayExpandAnim) {
                opExpandAnim(true);
                if (this.mIsExpandAnimDone) {
                    if (OpUtils.DEBUG_ONEPLUS) {
                        String str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("mAnimVol.start, ");
                        sb2.append(this.mOpForceExpandState);
                        Log.i(str2, sb2.toString());
                    }
                    setOpOutputChooserGravityNeedBeforeAnimStart(true);
                    setOpOutputChooserVisible(true);
                    this.mAnimVol.start();
                }
            }
            this.mNeedPlayExpandAnim = false;
        } else if (!z4) {
            if (this.mNeedPlayExpandAnim) {
                for (VolumeRow volumeRow4 : this.mRows) {
                    int i5 = volumeRow4.stream;
                    if (i5 == 2) {
                        volumeRow4.view.animate().alpha(0.0f).translationX((float) ((-this.mContext.getResources().getDimensionPixelSize(R$dimen.op_volume_dialog_panel_width)) / 5)).setDuration(175).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction(new Runnable() {
                            public final void run() {
                                VolumeDialogImpl.lambda$updateRowsH$21(VolumeRow.this);
                            }
                        }).setStartDelay(60);
                    } else if (i5 == 4) {
                        volumeRow4.view.animate().alpha(0.0f).translationX((float) ((-this.mContext.getResources().getDimensionPixelSize(R$dimen.op_volume_dialog_panel_width)) / 5)).setDuration(175).setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f)).withEndAction(new Runnable() {
                            public final void run() {
                                VolumeDialogImpl.lambda$updateRowsH$22(VolumeRow.this);
                            }
                        }).setStartDelay(30);
                    }
                }
                if (this.mNeedPlayExpandAnim) {
                    z3 = false;
                    opExpandAnim(false);
                    if (this.mIsExpandAnimDone) {
                        this.mAnimVol.start();
                    }
                } else {
                    z3 = false;
                }
                this.mNeedPlayExpandAnim = z3;
            } else {
                for (VolumeRow volumeRow5 : this.mRows) {
                    if (volumeRow2 == null) {
                        z2 = volumeRow5.stream == 3;
                        z = z2;
                    } else {
                        z = volumeRow5 == volumeRow2;
                        z2 = shouldBeVisibleH(volumeRow5, volumeRow2);
                    }
                    Util.setVisOrGone(volumeRow5.view, z2);
                    if (volumeRow5.stream != 3 && z) {
                        for (VolumeRow volumeRow6 : this.mRows) {
                            if (volumeRow6.stream == 3) {
                                Util.setVisOrGone(volumeRow6.view, false);
                            }
                        }
                    }
                    if (volumeRow5.view.isShown()) {
                        updateVolumeRowTintH(volumeRow5, true);
                    }
                }
            }
        }
        initSettingsH();
    }

    static /* synthetic */ void lambda$updateRowsH$21(VolumeRow volumeRow) {
        volumeRow.view.setAlpha(1.0f);
        volumeRow.view.setTranslationX(0.0f);
        Util.setVisOrGone(volumeRow.view, false);
    }

    static /* synthetic */ void lambda$updateRowsH$22(VolumeRow volumeRow) {
        volumeRow.view.setAlpha(1.0f);
        volumeRow.view.setTranslationX(0.0f);
        Util.setVisOrGone(volumeRow.view, false);
    }

    private void opExpandAnim(boolean z) {
        int i;
        int i2;
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.i(TAG, "init mAnimVol init");
        }
        this.mAnimVol = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ViewGroup viewGroup = (ViewGroup) this.mDialog.findViewById(R$id.volume_dialog_container);
        ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
        if (z) {
            i = this.mOpBeforeExpandWidth;
            i2 = this.mOpafterExpandWidth;
        } else {
            i = this.mOpafterExpandWidth;
            i2 = this.mOpBeforeExpandWidth;
        }
        int i3 = i2;
        this.mAnimVol.setDuration(275);
        this.mAnimVol.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.4f, 1.0f));
        ValueAnimator valueAnimator = this.mAnimVol;
        final ViewGroup.LayoutParams layoutParams2 = layoutParams;
        final int i4 = i;
        final ViewGroup viewGroup2 = viewGroup;
        final int i5 = i3;
        C17084 r1 = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    Log.i(VolumeDialogImpl.TAG, "opExpandAnim onAnimationStart");
                }
                VolumeDialogImpl.this.mIsExpandAnimDone = false;
                ViewGroup.LayoutParams layoutParams = layoutParams2;
                layoutParams.width = i4;
                viewGroup2.setLayoutParams(layoutParams);
                viewGroup2.requestLayout();
                if (!VolumeDialogImpl.this.mOpForceExpandState) {
                    VolumeDialogImpl.this.setOpOutputChooserVisible(false);
                }
            }

            public void onAnimationEnd(Animator animator) {
                if (OpUtils.DEBUG_ONEPLUS) {
                    String access$200 = VolumeDialogImpl.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("mAnimVol onAnimationEnd, isRunning:");
                    sb.append(VolumeDialogImpl.this.mAnimVol.isRunning());
                    Log.i(access$200, sb.toString());
                }
                ViewGroup.LayoutParams layoutParams = layoutParams2;
                layoutParams.width = i5;
                viewGroup2.setLayoutParams(layoutParams);
                viewGroup2.requestLayout();
                if (!VolumeDialogImpl.this.mOpForceExpandState) {
                    VolumeDialogImpl.this.setOpOutputChooserGravityNeedBeforeAnimStart(false);
                }
                if (!VolumeDialogImpl.this.isLandscape()) {
                    VolumeDialogImpl.this.setDialogWidth(-2);
                }
                VolumeDialogImpl.this.mIsExpandAnimDone = true;
                VolumeDialogImpl volumeDialogImpl = VolumeDialogImpl.this;
                volumeDialogImpl.updateRowsH(volumeDialogImpl.getActiveRow());
                if (OpUtils.DEBUG_ONEPLUS) {
                    String access$2002 = VolumeDialogImpl.TAG;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("mAnimVol onAnimationEnd 2 mIsExpandAnimDone");
                    sb2.append(VolumeDialogImpl.this.mIsExpandAnimDone);
                    sb2.append(",  width:");
                    sb2.append(VolumeDialogImpl.this.mDialog.getWindow().getAttributes().width);
                    Log.i(access$2002, sb2.toString());
                }
            }
        };
        valueAnimator.addListener(r1);
        ValueAnimator valueAnimator2 = this.mAnimVol;
        final int i6 = i3;
        final ViewGroup.LayoutParams layoutParams3 = layoutParams;
        final ViewGroup viewGroup3 = viewGroup;
        C17095 r12 = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                int i = i6;
                int i2 = i4;
                float f = (((float) (i - i2)) * floatValue) + ((float) i2);
                if (OpUtils.DEBUG_ONEPLUS) {
                    String access$200 = VolumeDialogImpl.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("onAnimationUpdate, t = ");
                    sb.append(floatValue);
                    sb.append(", scaleT:");
                    sb.append(f);
                    sb.append(", lp.width:");
                    sb.append(layoutParams3.width);
                    Log.i(access$200, sb.toString());
                }
                if (Math.abs(f - ((float) layoutParams3.width)) >= 10.0f) {
                    ViewGroup.LayoutParams layoutParams = layoutParams3;
                    layoutParams.width = (int) f;
                    viewGroup3.setLayoutParams(layoutParams);
                    viewGroup3.requestLayout();
                }
            }
        };
        valueAnimator2.addUpdateListener(r12);
    }

    /* access modifiers changed from: protected */
    public void updateRingerH() {
        State state = this.mState;
        if (state != null) {
            StreamState streamState = (StreamState) state.states.get(2);
            if (streamState != null) {
                State state2 = this.mState;
                int i = state2.zenMode;
                boolean z = false;
                boolean z2 = i == 3 || i == 2 || (i == 1 && state2.disallowRinger);
                enableRingerViewsH(!z2);
                int i2 = this.mState.ringerModeInternal;
                if (i2 == 0) {
                    this.mRingerIcon.setImageResource(R$drawable.ic_volume_ringer_mute);
                    this.mRingerIcon.setTag(Integer.valueOf(2));
                    addAccessibilityDescription(this.mRingerIcon, 0, this.mContext.getString(R$string.volume_ringer_hint_unmute));
                } else if (i2 != 1) {
                    if ((this.mAutomute && streamState.level == 0) || streamState.muted) {
                        z = true;
                    }
                    if (z2 || !z) {
                        this.mRingerIcon.setImageResource(R$drawable.ic_volume_ringer);
                        if (this.mController.hasVibrator()) {
                            addAccessibilityDescription(this.mRingerIcon, 2, this.mContext.getString(R$string.volume_ringer_hint_vibrate));
                        } else {
                            addAccessibilityDescription(this.mRingerIcon, 2, this.mContext.getString(R$string.volume_ringer_hint_mute));
                        }
                        this.mRingerIcon.setTag(Integer.valueOf(1));
                    } else {
                        this.mRingerIcon.setImageResource(R$drawable.ic_volume_ringer_mute);
                        addAccessibilityDescription(this.mRingerIcon, 2, this.mContext.getString(R$string.volume_ringer_hint_unmute));
                        this.mRingerIcon.setTag(Integer.valueOf(2));
                    }
                } else {
                    this.mRingerIcon.setImageResource(R$drawable.ic_volume_ringer_vibrate);
                    addAccessibilityDescription(this.mRingerIcon, 1, this.mContext.getString(R$string.volume_ringer_hint_mute));
                    this.mRingerIcon.setTag(Integer.valueOf(3));
                }
            }
        }
    }

    private void addAccessibilityDescription(View view, int i, final String str) {
        int i2;
        if (i == 0) {
            i2 = R$string.volume_ringer_status_silent;
        } else if (i != 1) {
            i2 = R$string.volume_ringer_status_normal;
        } else {
            i2 = R$string.volume_ringer_status_vibrate;
        }
        view.setContentDescription(this.mContext.getString(i2));
        view.setAccessibilityDelegate(new AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                accessibilityNodeInfo.addAction(new AccessibilityAction(16, str));
            }
        });
    }

    private void enableRingerViewsH(boolean z) {
        ImageButton imageButton = this.mRingerIcon;
        if (imageButton != null) {
            imageButton.setEnabled(z);
        }
        FrameLayout frameLayout = this.mZenIcon;
        if (frameLayout != null) {
            frameLayout.setVisibility(z ? 8 : 0);
        }
    }

    private void trimObsoleteH() {
        if (C1684D.BUG) {
            Log.d(TAG, "trimObsoleteH");
        }
        for (int size = this.mRows.size() - 1; size >= 0; size--) {
            VolumeRow volumeRow = (VolumeRow) this.mRows.get(size);
            StreamState streamState = volumeRow.f122ss;
            if (streamState != null && streamState.dynamic && !this.mDynamic.get(volumeRow.stream)) {
                this.mRows.remove(size);
                this.mDialogRowsView.removeView(volumeRow.view);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onStateChangedH(State state) {
        String str = "onStateChangedH() state: ";
        if (Build.DEBUG_ONEPLUS) {
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(state.toString());
            Log.d(str2, sb.toString());
        }
        if (C1684D.BUG) {
            String str3 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);
            sb2.append(state.toString());
            Log.d(str3, sb2.toString());
        }
        State state2 = this.mState;
        if (!(state2 == null || state == null)) {
            int i = state2.ringerModeInternal;
            int i2 = state.ringerModeInternal;
            if (i != i2 && i2 == 1) {
                this.mController.vibrate(VibrationEffect.get(5));
            }
        }
        this.mState = state;
        this.mDynamic.clear();
        for (int i3 = 0; i3 < state.states.size(); i3++) {
            int keyAt = state.states.keyAt(i3);
            if (((StreamState) state.states.valueAt(i3)).dynamic) {
                String str4 = TAG;
                StringBuilder sb3 = new StringBuilder();
                sb3.append(" onStateChangedH stream:");
                sb3.append(keyAt);
                Log.i(str4, sb3.toString());
                this.mDynamic.put(keyAt, true);
                if (findRow(keyAt) == null) {
                    addRow(keyAt, R$drawable.ic_volume_remote, R$drawable.ic_volume_remote_mute, true, false, true);
                }
            }
        }
        int i4 = this.mActiveStream;
        int i5 = state.activeStream;
        if (i4 != i5) {
            this.mPrevActiveStream = i4;
            this.mActiveStream = i5;
            updateRowsH(getActiveRow());
            if (this.mShowing) {
                rescheduleTimeoutH();
            }
        }
        for (VolumeRow updateVolumeRowH : this.mRows) {
            updateVolumeRowH(updateVolumeRowH);
        }
        updateRingerH();
        this.mWindow.setTitle(composeWindowTitle());
    }

    /* access modifiers changed from: 0000 */
    public CharSequence composeWindowTitle() {
        return this.mContext.getString(R$string.volume_dialog_title, new Object[]{getStreamLabelH(getActiveRow().f122ss)});
    }

    /* JADX WARNING: type inference failed for: r8v4 */
    /* JADX WARNING: type inference failed for: r8v8 */
    /* JADX WARNING: type inference failed for: r8v23 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateVolumeRowH(com.oneplus.volume.OpVolumeDialogImpl.VolumeRow r17) {
        /*
            r16 = this;
            r0 = r16
            r1 = r17
            boolean r2 = com.android.systemui.volume.C1684D.BUG
            java.lang.String r3 = "updateVolumeRowH s="
            if (r2 == 0) goto L_0x0021
            java.lang.String r2 = TAG
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r3)
            int r5 = r1.stream
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            android.util.Log.i(r2, r4)
        L_0x0021:
            com.android.systemui.plugins.VolumeDialogController$State r2 = r0.mState
            if (r2 != 0) goto L_0x0026
            return
        L_0x0026:
            android.util.SparseArray<com.android.systemui.plugins.VolumeDialogController$StreamState> r2 = r2.states
            int r4 = r1.stream
            java.lang.Object r2 = r2.get(r4)
            com.android.systemui.plugins.VolumeDialogController$StreamState r2 = (com.android.systemui.plugins.VolumeDialogController.StreamState) r2
            if (r2 != 0) goto L_0x0033
            return
        L_0x0033:
            r1.f122ss = r2
            int r4 = r2.level
            r5 = 2
            r6 = 1
            if (r4 <= 0) goto L_0x0042
            int r7 = r1.stream
            if (r7 == r5) goto L_0x0042
            r1.lastAudibleLevel = r4
            goto L_0x0069
        L_0x0042:
            int r4 = r2.level
            if (r4 <= r6) goto L_0x0069
            int r7 = r1.stream
            if (r7 != r5) goto L_0x0069
            r1.lastAudibleLevel = r4
            boolean r4 = android.os.Build.DEBUG_ONEPLUS
            if (r4 == 0) goto L_0x0069
            java.lang.String r4 = TAG
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "updateVolumeRowH, lastAudibleLevel = ss.level:"
            r7.append(r8)
            int r8 = r2.level
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Log.i(r4, r7)
        L_0x0069:
            int r4 = r2.level
            int r7 = r1.requestedLevel
            if (r4 != r7) goto L_0x0072
            r4 = -1
            r1.requestedLevel = r4
        L_0x0072:
            int r4 = r1.stream
            r7 = 10
            if (r4 != r7) goto L_0x007a
            r4 = r6
            goto L_0x007b
        L_0x007a:
            r4 = 0
        L_0x007b:
            int r7 = r1.stream
            if (r7 != r5) goto L_0x0081
            r7 = r6
            goto L_0x0082
        L_0x0081:
            r7 = 0
        L_0x0082:
            int r9 = r1.stream
            if (r9 != r6) goto L_0x0088
            r9 = r6
            goto L_0x0089
        L_0x0088:
            r9 = 0
        L_0x0089:
            int r10 = r1.stream
            r11 = 4
            if (r10 != r11) goto L_0x0090
            r10 = r6
            goto L_0x0091
        L_0x0090:
            r10 = 0
        L_0x0091:
            int r11 = r1.stream
            r12 = 3
            if (r11 != r12) goto L_0x0098
            r11 = r6
            goto L_0x0099
        L_0x0098:
            r11 = 0
        L_0x0099:
            if (r7 == 0) goto L_0x00a3
            com.android.systemui.plugins.VolumeDialogController$State r13 = r0.mState
            int r13 = r13.ringerModeInternal
            if (r13 != r6) goto L_0x00a3
            r13 = r6
            goto L_0x00a4
        L_0x00a3:
            r13 = 0
        L_0x00a4:
            if (r7 == 0) goto L_0x00ae
            com.android.systemui.plugins.VolumeDialogController$State r14 = r0.mState
            int r14 = r14.ringerModeInternal
            if (r14 != 0) goto L_0x00ae
            r14 = r6
            goto L_0x00af
        L_0x00ae:
            r14 = 0
        L_0x00af:
            com.android.systemui.plugins.VolumeDialogController$State r15 = r0.mState
            int r15 = r15.zenMode
            if (r15 != r6) goto L_0x00b7
            r15 = r6
            goto L_0x00b8
        L_0x00b7:
            r15 = 0
        L_0x00b8:
            com.android.systemui.plugins.VolumeDialogController$State r8 = r0.mState
            int r8 = r8.zenMode
            if (r8 != r12) goto L_0x00c0
            r8 = r6
            goto L_0x00c1
        L_0x00c0:
            r8 = 0
        L_0x00c1:
            com.android.systemui.plugins.VolumeDialogController$State r12 = r0.mState
            int r12 = r12.zenMode
            if (r12 != r5) goto L_0x00c9
            r12 = r6
            goto L_0x00ca
        L_0x00c9:
            r12 = 0
        L_0x00ca:
            if (r8 == 0) goto L_0x00d2
            if (r7 != 0) goto L_0x00d0
            if (r9 == 0) goto L_0x0100
        L_0x00d0:
            r8 = r6
            goto L_0x0101
        L_0x00d2:
            if (r12 == 0) goto L_0x00dd
            if (r7 != 0) goto L_0x00d0
            if (r9 != 0) goto L_0x00d0
            if (r10 != 0) goto L_0x00d0
            if (r11 == 0) goto L_0x0100
            goto L_0x00d0
        L_0x00dd:
            if (r15 == 0) goto L_0x0100
            if (r10 == 0) goto L_0x00e7
            com.android.systemui.plugins.VolumeDialogController$State r8 = r0.mState
            boolean r8 = r8.disallowAlarms
            if (r8 != 0) goto L_0x00d0
        L_0x00e7:
            if (r11 == 0) goto L_0x00ef
            com.android.systemui.plugins.VolumeDialogController$State r8 = r0.mState
            boolean r8 = r8.disallowMedia
            if (r8 != 0) goto L_0x00d0
        L_0x00ef:
            if (r7 == 0) goto L_0x00f7
            com.android.systemui.plugins.VolumeDialogController$State r8 = r0.mState
            boolean r8 = r8.disallowRinger
            if (r8 != 0) goto L_0x00d0
        L_0x00f7:
            if (r9 == 0) goto L_0x0100
            com.android.systemui.plugins.VolumeDialogController$State r8 = r0.mState
            boolean r8 = r8.disallowSystem
            if (r8 == 0) goto L_0x0100
            goto L_0x00d0
        L_0x0100:
            r8 = 0
        L_0x0101:
            int r9 = r2.levelMax
            int r9 = r9 * 100
            android.widget.SeekBar r10 = r1.slider
            int r10 = r10.getMax()
            if (r9 == r10) goto L_0x0112
            android.widget.SeekBar r10 = r1.slider
            r10.setMax(r9)
        L_0x0112:
            int r9 = r2.levelMin
            int r9 = r9 * 100
            android.widget.SeekBar r10 = r1.slider
            int r10 = r10.getMin()
            if (r9 == r10) goto L_0x0127
            int r10 = r1.stream
            if (r10 == 0) goto L_0x0127
            android.widget.SeekBar r10 = r1.slider
            r10.setMin(r9)
        L_0x0127:
            boolean r9 = android.os.Build.DEBUG_ONEPLUS
            if (r9 == 0) goto L_0x0155
            java.lang.String r9 = TAG
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            r10.append(r3)
            int r3 = r1.stream
            r10.append(r3)
            java.lang.String r3 = " min:"
            r10.append(r3)
            int r3 = r2.levelMin
            r10.append(r3)
            java.lang.String r3 = ", ss.muted:"
            r10.append(r3)
            boolean r3 = r2.muted
            r10.append(r3)
            java.lang.String r3 = r10.toString()
            android.util.Log.d(r9, r3)
        L_0x0155:
            android.widget.TextView r3 = r1.header
            java.lang.String r9 = r0.getStreamLabelH(r2)
            com.android.settingslib.volume.Util.setText(r3, r9)
            android.widget.SeekBar r3 = r1.slider
            android.widget.TextView r9 = r1.header
            java.lang.CharSequence r9 = r9.getText()
            r3.setContentDescription(r9)
            com.android.systemui.volume.ConfigurableTexts r3 = r0.mConfigurableTexts
            android.widget.TextView r9 = r1.header
            int r10 = r2.name
            r3.add(r9, r10)
            boolean r3 = r0.mAutomute
            if (r3 != 0) goto L_0x017a
            boolean r3 = r2.muteSupported
            if (r3 == 0) goto L_0x017e
        L_0x017a:
            if (r8 != 0) goto L_0x017e
            r3 = r6
            goto L_0x017f
        L_0x017e:
            r3 = 0
        L_0x017f:
            android.widget.ImageButton r9 = r1.icon
            r9.setEnabled(r3)
            android.widget.ImageButton r9 = r1.icon
            if (r3 == 0) goto L_0x018b
            r10 = 1065353216(0x3f800000, float:1.0)
            goto L_0x018d
        L_0x018b:
            r10 = 1056964608(0x3f000000, float:0.5)
        L_0x018d:
            r9.setAlpha(r10)
            if (r13 == 0) goto L_0x0195
            int r9 = com.android.systemui.R$drawable.ic_volume_ringer_vibrate
            goto L_0x01bf
        L_0x0195:
            if (r14 != 0) goto L_0x01bd
            if (r8 == 0) goto L_0x019a
            goto L_0x01bd
        L_0x019a:
            boolean r9 = r2.routedToBluetooth
            if (r9 == 0) goto L_0x01a8
            boolean r9 = r2.muted
            if (r9 == 0) goto L_0x01a5
            int r9 = com.android.systemui.R$drawable.ic_volume_media_bt_mute
            goto L_0x01bf
        L_0x01a5:
            int r9 = com.android.systemui.R$drawable.ic_volume_media_bt
            goto L_0x01bf
        L_0x01a8:
            boolean r9 = r0.mAutomute
            if (r9 == 0) goto L_0x01b3
            int r9 = r2.level
            if (r9 != 0) goto L_0x01b3
            int r9 = r1.iconMuteRes
            goto L_0x01bf
        L_0x01b3:
            boolean r9 = r2.muted
            if (r9 == 0) goto L_0x01ba
            int r9 = r1.iconMuteRes
            goto L_0x01bf
        L_0x01ba:
            int r9 = r1.iconRes
            goto L_0x01bf
        L_0x01bd:
            int r9 = r1.iconMuteRes
        L_0x01bf:
            android.widget.ImageButton r10 = r1.icon
            r10.setImageResource(r9)
            int r10 = com.android.systemui.R$drawable.ic_volume_ringer_vibrate
            if (r9 != r10) goto L_0x01ca
            r5 = 3
            goto L_0x01df
        L_0x01ca:
            int r10 = com.android.systemui.R$drawable.ic_volume_media_bt_mute
            if (r9 == r10) goto L_0x01df
            int r10 = r1.iconMuteRes
            if (r9 != r10) goto L_0x01d3
            goto L_0x01df
        L_0x01d3:
            int r5 = com.android.systemui.R$drawable.ic_volume_media_bt
            if (r9 == r5) goto L_0x01de
            int r5 = r1.iconRes
            if (r9 != r5) goto L_0x01dc
            goto L_0x01de
        L_0x01dc:
            r5 = 0
            goto L_0x01df
        L_0x01de:
            r5 = r6
        L_0x01df:
            r1.iconState = r5
            if (r3 == 0) goto L_0x0291
            if (r7 == 0) goto L_0x0242
            if (r13 == 0) goto L_0x01fe
            android.widget.ImageButton r3 = r1.icon
            android.content.Context r4 = r0.mContext
            int r5 = com.android.systemui.R$string.volume_stream_content_description_unmute
            java.lang.Object[] r6 = new java.lang.Object[r6]
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r9 = 0
            r6[r9] = r2
            java.lang.String r2 = r4.getString(r5, r6)
            r3.setContentDescription(r2)
            goto L_0x024d
        L_0x01fe:
            com.android.systemui.plugins.VolumeDialogController r3 = r0.mController
            boolean r3 = r3.hasVibrator()
            if (r3 == 0) goto L_0x0224
            android.widget.ImageButton r3 = r1.icon
            android.content.Context r4 = r0.mContext
            boolean r5 = r0.mShowA11yStream
            if (r5 == 0) goto L_0x0211
            int r5 = com.android.systemui.R$string.volume_stream_content_description_vibrate_a11y
            goto L_0x0213
        L_0x0211:
            int r5 = com.android.systemui.R$string.volume_stream_content_description_vibrate
        L_0x0213:
            java.lang.Object[] r6 = new java.lang.Object[r6]
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r9 = 0
            r6[r9] = r2
            java.lang.String r2 = r4.getString(r5, r6)
            r3.setContentDescription(r2)
            goto L_0x024d
        L_0x0224:
            android.widget.ImageButton r3 = r1.icon
            android.content.Context r4 = r0.mContext
            boolean r5 = r0.mShowA11yStream
            if (r5 == 0) goto L_0x022f
            int r5 = com.android.systemui.R$string.volume_stream_content_description_mute_a11y
            goto L_0x0231
        L_0x022f:
            int r5 = com.android.systemui.R$string.volume_stream_content_description_mute
        L_0x0231:
            java.lang.Object[] r6 = new java.lang.Object[r6]
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r9 = 0
            r6[r9] = r2
            java.lang.String r2 = r4.getString(r5, r6)
            r3.setContentDescription(r2)
            goto L_0x024d
        L_0x0242:
            if (r4 == 0) goto L_0x024f
            android.widget.ImageButton r3 = r1.icon
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r3.setContentDescription(r2)
        L_0x024d:
            r9 = 0
            goto L_0x029b
        L_0x024f:
            boolean r3 = r2.muted
            if (r3 != 0) goto L_0x027a
            boolean r3 = r0.mAutomute
            if (r3 == 0) goto L_0x025c
            int r3 = r2.level
            if (r3 != 0) goto L_0x025c
            goto L_0x027a
        L_0x025c:
            android.widget.ImageButton r3 = r1.icon
            android.content.Context r4 = r0.mContext
            boolean r5 = r0.mShowA11yStream
            if (r5 == 0) goto L_0x0267
            int r5 = com.android.systemui.R$string.volume_stream_content_description_mute_a11y
            goto L_0x0269
        L_0x0267:
            int r5 = com.android.systemui.R$string.volume_stream_content_description_mute
        L_0x0269:
            java.lang.Object[] r6 = new java.lang.Object[r6]
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r9 = 0
            r6[r9] = r2
            java.lang.String r2 = r4.getString(r5, r6)
            r3.setContentDescription(r2)
            goto L_0x029b
        L_0x027a:
            r9 = 0
            android.widget.ImageButton r3 = r1.icon
            android.content.Context r4 = r0.mContext
            int r5 = com.android.systemui.R$string.volume_stream_content_description_unmute
            java.lang.Object[] r6 = new java.lang.Object[r6]
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r6[r9] = r2
            java.lang.String r2 = r4.getString(r5, r6)
            r3.setContentDescription(r2)
            goto L_0x029b
        L_0x0291:
            r9 = 0
            android.widget.ImageButton r3 = r1.icon
            java.lang.String r2 = r0.getStreamLabelH(r2)
            r3.setContentDescription(r2)
        L_0x029b:
            if (r8 == 0) goto L_0x029f
            r1.tracking = r9
        L_0x029f:
            r2 = r8 ^ 1
            com.android.systemui.plugins.VolumeDialogController$StreamState r3 = r1.f122ss
            boolean r3 = r3.muted
            if (r3 == 0) goto L_0x02ad
            if (r7 != 0) goto L_0x02ad
            if (r8 != 0) goto L_0x02ad
            r8 = r9
            goto L_0x02b1
        L_0x02ad:
            com.android.systemui.plugins.VolumeDialogController$StreamState r3 = r1.f122ss
            int r8 = r3.level
        L_0x02b1:
            if (r7 == 0) goto L_0x02b9
            if (r13 != 0) goto L_0x02b7
            if (r14 == 0) goto L_0x02b9
        L_0x02b7:
            r2 = r9
            r8 = r2
        L_0x02b9:
            r0.updateVolumeRowSliderH(r1, r2, r8)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.volume.VolumeDialogImpl.updateVolumeRowH(com.oneplus.volume.OpVolumeDialogImpl$VolumeRow):void");
    }

    private void updateVolumeRowTintH(VolumeRow volumeRow, boolean z) {
        updateVolumeRowTintH(volumeRow, z, false);
    }

    private void updateVolumeRowTintH(VolumeRow volumeRow, boolean z, boolean z2) {
        if (z) {
            volumeRow.slider.requestFocus();
        }
        ColorStateList valueOf = ColorStateList.valueOf(this.mAccentColor);
        if (z) {
            Color.alpha(valueOf.getDefaultColor());
        } else {
            getAlphaAttr(16844115);
        }
        if (valueOf != volumeRow.cachedTint || this.mThemeColorMode != volumeRow.themeColorMode || z2) {
            volumeRow.slider.setProgressTintList(valueOf);
            volumeRow.slider.setThumbTintList(valueOf);
            volumeRow.cachedTint = valueOf;
            volumeRow.slider.setProgressDrawable(this.mContext.getResources().getDrawable(this.mThemeColorSeekbarBackgroundDrawable));
            volumeRow.themeColorMode = this.mThemeColorMode;
            volumeRow.icon.setColorFilter(this.mThemeColorIcon);
        }
    }

    private void updateVolumeRowSliderH(VolumeRow volumeRow, boolean z, int i) {
        volumeRow.slider.setEnabled(z);
        updateVolumeRowTintH(volumeRow, true);
        String str = "updateVolumeRowSliderH s=";
        if (volumeRow.tracking) {
            if (Build.DEBUG_ONEPLUS) {
                String str2 = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append(str);
                sb.append(volumeRow.stream);
                sb.append(" is tracking");
                Log.d(str2, sb.toString());
            }
            return;
        }
        int progress = volumeRow.slider.getProgress();
        int impliedLevel = getImpliedLevel(volumeRow.slider, progress);
        boolean z2 = volumeRow.view.getVisibility() == 0;
        boolean z3 = SystemClock.uptimeMillis() - volumeRow.userAttempt < 1000;
        this.mHandler.removeMessages(3, volumeRow);
        if (this.mShowing && z2 && z3) {
            if (C1684D.BUG) {
                Log.d(TAG, "inGracePeriod");
            }
            if (Build.DEBUG_ONEPLUS) {
                String str3 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(volumeRow.stream);
                sb2.append(" inGracePeriod");
                Log.d(str3, sb2.toString());
            }
            C1713H h = this.mHandler;
            h.sendMessageAtTime(h.obtainMessage(3, volumeRow), volumeRow.userAttempt + 1000);
        } else if (i != impliedLevel || !this.mShowing || !z2) {
            int i2 = i * 100;
            if (Build.DEBUG_ONEPLUS) {
                String str4 = TAG;
                StringBuilder sb3 = new StringBuilder();
                sb3.append(str);
                sb3.append(volumeRow.stream);
                sb3.append(" progress: ");
                sb3.append(progress);
                sb3.append(" newProgress: ");
                sb3.append(i2);
                sb3.append(" enable: ");
                sb3.append(z);
                Log.d(str4, sb3.toString());
            }
            if (progress != i2) {
                if (!this.mShowing || !z2) {
                    ObjectAnimator objectAnimator = volumeRow.anim;
                    if (objectAnimator != null) {
                        objectAnimator.cancel();
                    }
                    volumeRow.slider.setProgress(i2, true);
                } else {
                    ObjectAnimator objectAnimator2 = volumeRow.anim;
                    if (objectAnimator2 == null || !objectAnimator2.isRunning() || volumeRow.animTargetProgress != i2) {
                        ObjectAnimator objectAnimator3 = volumeRow.anim;
                        if (objectAnimator3 == null) {
                            volumeRow.anim = ObjectAnimator.ofInt(volumeRow.slider, "progress", new int[]{progress, i2});
                            volumeRow.anim.setInterpolator(new DecelerateInterpolator());
                        } else {
                            objectAnimator3.cancel();
                            volumeRow.anim.setIntValues(new int[]{progress, i2});
                        }
                        volumeRow.animTargetProgress = i2;
                        volumeRow.anim.setDuration(80);
                        volumeRow.anim.start();
                    } else {
                        if (Build.DEBUG_ONEPLUS) {
                            Log.d(TAG, "updateVolumeRowSliderH already animating to the target progress");
                        }
                    }
                }
            }
        } else {
            if (Build.DEBUG_ONEPLUS) {
                String str5 = TAG;
                StringBuilder sb4 = new StringBuilder();
                sb4.append(str);
                sb4.append(volumeRow.stream);
                sb4.append(" vlevel: ");
                sb4.append(i);
                sb4.append(" level: ");
                sb4.append(impliedLevel);
                Log.d(str5, sb4.toString());
            }
        }
    }

    /* access modifiers changed from: private */
    public void recheckH(VolumeRow volumeRow) {
        if (volumeRow == null) {
            if (C1684D.BUG) {
                Log.d(TAG, "recheckH ALL");
            }
            trimObsoleteH();
            for (VolumeRow updateVolumeRowH : this.mRows) {
                updateVolumeRowH(updateVolumeRowH);
            }
            return;
        }
        if (C1684D.BUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("recheckH ");
            sb.append(volumeRow.stream);
            Log.d(str, sb.toString());
        }
        updateVolumeRowH(volumeRow);
    }

    /* access modifiers changed from: private */
    public void setStreamImportantH(int i, boolean z) {
        for (VolumeRow volumeRow : this.mRows) {
            if (volumeRow.stream == i) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append(" setStreamImportantH stream:");
                sb.append(volumeRow.stream);
                sb.append(" important:");
                sb.append(z);
                Log.i(str, sb.toString());
                volumeRow.important = z;
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0046, code lost:
        recheckH(null);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void showSafetyWarningH(int r4) {
        /*
            r3 = this;
            java.lang.String r0 = TAG
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "showSafetyWarningH flags:"
            r1.append(r2)
            r1.append(r4)
            java.lang.String r2 = " mShowing:"
            r1.append(r2)
            boolean r2 = r3.mShowing
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
            r4 = r4 & 1025(0x401, float:1.436E-42)
            if (r4 != 0) goto L_0x0028
            boolean r4 = r3.mShowing
            if (r4 == 0) goto L_0x004a
        L_0x0028:
            java.lang.Object r4 = r3.mSafetyWarningLock
            monitor-enter(r4)
            com.android.systemui.volume.SafetyWarningDialog r0 = r3.mSafetyWarning     // Catch:{ all -> 0x004e }
            if (r0 == 0) goto L_0x0031
            monitor-exit(r4)     // Catch:{ all -> 0x004e }
            return
        L_0x0031:
            com.android.systemui.volume.VolumeDialogImpl$7 r0 = new com.android.systemui.volume.VolumeDialogImpl$7     // Catch:{ all -> 0x004e }
            android.content.Context r1 = r3.mContext     // Catch:{ all -> 0x004e }
            com.android.systemui.plugins.VolumeDialogController r2 = r3.mController     // Catch:{ all -> 0x004e }
            android.media.AudioManager r2 = r2.getAudioManager()     // Catch:{ all -> 0x004e }
            r0.<init>(r1, r2)     // Catch:{ all -> 0x004e }
            r3.mSafetyWarning = r0     // Catch:{ all -> 0x004e }
            com.android.systemui.volume.SafetyWarningDialog r0 = r3.mSafetyWarning     // Catch:{ all -> 0x004e }
            r0.show()     // Catch:{ all -> 0x004e }
            monitor-exit(r4)     // Catch:{ all -> 0x004e }
            r4 = 0
            r3.recheckH(r4)
        L_0x004a:
            r3.rescheduleTimeoutH()
            return
        L_0x004e:
            r3 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x004e }
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.volume.VolumeDialogImpl.showSafetyWarningH(int):void");
    }

    private String getStreamLabelH(StreamState streamState) {
        String str = "";
        if (streamState == null) {
            return str;
        }
        String str2 = streamState.remoteLabel;
        if (str2 != null) {
            return str2;
        }
        try {
            return this.mContext.getResources().getString(streamState.name);
        } catch (NotFoundException unused) {
            String str3 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Can't find translation for stream ");
            sb.append(streamState);
            Slog.e(str3, sb.toString());
            return str;
        }
    }

    private Runnable getSinglePressFor(ImageButton imageButton) {
        return new Runnable(imageButton) {
            private final /* synthetic */ ImageButton f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                VolumeDialogImpl.this.lambda$getSinglePressFor$23$VolumeDialogImpl(this.f$1);
            }
        };
    }

    public /* synthetic */ void lambda$getSinglePressFor$23$VolumeDialogImpl(ImageButton imageButton) {
        if (imageButton != null) {
            imageButton.setPressed(true);
            imageButton.postOnAnimationDelayed(getSingleUnpressFor(imageButton), 200);
        }
    }

    private Runnable getSingleUnpressFor(ImageButton imageButton) {
        return new Runnable(imageButton) {
            private final /* synthetic */ ImageButton f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                VolumeDialogImpl.lambda$getSingleUnpressFor$24(this.f$0);
            }
        };
    }

    static /* synthetic */ void lambda$getSingleUnpressFor$24(ImageButton imageButton) {
        if (imageButton != null) {
            imageButton.setPressed(false);
        }
    }

    public void onOverlayChanged() {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.i(TAG, "onOverlayChanged be trigger in vol");
        }
        applyColorTheme(true);
    }
}
