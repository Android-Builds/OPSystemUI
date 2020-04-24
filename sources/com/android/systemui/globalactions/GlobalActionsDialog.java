package com.android.systemui.globalactions;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.dreams.IDreamManager;
import android.service.dreams.IDreamManager.Stub;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.colorextraction.ColorExtractor.OnColorsChangedListener;
import com.android.internal.colorextraction.drawable.ScrimDrawable;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.util.ScreenRecordHelper;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.view.RotationPolicy;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.MultiListLayout;
import com.android.systemui.MultiListLayout.MultiListAdapter;
import com.android.systemui.MultiListLayout.RotationListener;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.globalactions.GlobalActionsDialog.MyAdapter;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.GlobalActions.GlobalActionsManager;
import com.android.systemui.plugins.GlobalActionsPanelPlugin;
import com.android.systemui.plugins.GlobalActionsPanelPlugin.Callbacks;
import com.android.systemui.plugins.GlobalActionsPanelPlugin.PanelViewController;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.util.leak.RotationUtils;
import com.android.systemui.volume.SystemUIInterpolators$LogAccelerateInterpolator;
import com.oneplus.scene.OpSceneModeObserver;
import java.util.ArrayList;
import java.util.List;

public class GlobalActionsDialog implements OnDismissListener, OnShowListener, ConfigurationListener {
    /* access modifiers changed from: private */
    public final ActivityStarter mActivityStarter;
    /* access modifiers changed from: private */
    public MyAdapter mAdapter;
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean z) {
            GlobalActionsDialog.this.onAirplaneModeChanged();
        }
    };
    /* access modifiers changed from: private */
    public ToggleAction mAirplaneModeOn;
    /* access modifiers changed from: private */
    public State mAirplaneState = State.Off;
    /* access modifiers changed from: private */
    public final AudioManager mAudioManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                String stringExtra = intent.getStringExtra("reason");
                if (!"globalactions".equals(stringExtra)) {
                    GlobalActionsDialog.this.mHandler.sendMessage(GlobalActionsDialog.this.mHandler.obtainMessage(0, stringExtra));
                }
            } else if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && GlobalActionsDialog.this.mIsWaitingForEcmExit) {
                GlobalActionsDialog.this.mIsWaitingForEcmExit = false;
                GlobalActionsDialog.this.changeAirplaneModeSystemSetting(true);
            }
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned = false;
    /* access modifiers changed from: private */
    public ActionsDialog mDialog;
    private final IDreamManager mDreamManager;
    /* access modifiers changed from: private */
    public final EmergencyAffordanceManager mEmergencyAffordanceManager;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 0) {
                if (i == 1) {
                    GlobalActionsDialog.this.refreshSilentMode();
                    GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
                } else if (i == 2) {
                    GlobalActionsDialog.this.handleShow();
                }
            } else if (GlobalActionsDialog.this.mDialog != null) {
                if ("dream".equals(message.obj)) {
                    GlobalActionsDialog.this.mDialog.dismissImmediately();
                } else {
                    GlobalActionsDialog.this.mDialog.dismiss();
                }
                GlobalActionsDialog.this.mDialog = null;
            }
        }
    };
    private boolean mHasLockdownButton;
    private boolean mHasLogoutButton;
    /* access modifiers changed from: private */
    public boolean mHasTelephony;
    private boolean mHasVibrator;
    /* access modifiers changed from: private */
    public boolean mIsWaitingForEcmExit = false;
    /* access modifiers changed from: private */
    public ArrayList<Action> mItems;
    private final KeyguardManager mKeyguardManager;
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing = false;
    private final LockPatternUtils mLockPatternUtils;
    private GlobalActionsPanelPlugin mPanelPlugin;
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onServiceStateChanged(ServiceState serviceState) {
            if (GlobalActionsDialog.this.mHasTelephony) {
                GlobalActionsDialog.this.mAirplaneState = serviceState.getState() == 3 ? State.On : State.Off;
                GlobalActionsDialog.this.mAirplaneModeOn.updateState(GlobalActionsDialog.this.mAirplaneState);
                GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                GlobalActionsDialog.this.mHandler.sendEmptyMessage(1);
            }
        }
    };
    /* access modifiers changed from: private */
    public final ScreenRecordHelper mScreenRecordHelper;
    private final ScreenshotHelper mScreenshotHelper;
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    /* access modifiers changed from: private */
    public final GlobalActionsManager mWindowManagerFuncs;

    public interface Action {
        View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater);

        boolean isEnabled();

        void onPress();

        boolean shouldBeSeparated() {
            return false;
        }

        boolean showBeforeProvisioning();

        boolean showDuringKeyguard();
    }

    private static final class ActionsDialog extends Dialog implements DialogInterface, OnColorsChangedListener {
        private final MyAdapter mAdapter;
        private Drawable mBackgroundDrawable;
        private final SysuiColorExtractor mColorExtractor;
        /* access modifiers changed from: private */
        public final Context mContext;
        private MultiListLayout mGlobalActionsLayout;
        private boolean mKeyguardShowing;
        /* access modifiers changed from: private */
        public final PanelViewController mPanelController;
        private ResetOrientationData mResetOrientationData;
        private float mScrimAlpha;
        private boolean mShowing;
        private final IStatusBarService mStatusBarService;
        private final IBinder mToken = new Binder();

        private static class ResetOrientationData {
            public boolean locked;
            public int rotation;

            private ResetOrientationData() {
            }
        }

        ActionsDialog(Context context, MyAdapter myAdapter, PanelViewController panelViewController) {
            super(context, R$style.Theme_SystemUI_Dialog_GlobalActions);
            this.mContext = context;
            this.mAdapter = myAdapter;
            this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
            this.mStatusBarService = (IStatusBarService) Dependency.get(IStatusBarService.class);
            Window window = getWindow();
            window.requestFeature(1);
            window.getDecorView();
            LayoutParams attributes = window.getAttributes();
            attributes.systemUiVisibility |= 1792;
            window.setLayout(-1, -1);
            window.clearFlags(2);
            window.addFlags(17629472);
            if (((OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class)).isInBrickMode()) {
                window.addFlags(8);
            }
            window.setType(2020);
            setTitle(17040078);
            this.mPanelController = panelViewController;
            initializeLayout();
        }

        private boolean shouldUsePanel() {
            PanelViewController panelViewController = this.mPanelController;
            return (panelViewController == null || panelViewController.getPanelContent() == null) ? false : true;
        }

        private void initializePanel() {
            int rotation = RotationUtils.getRotation(this.mContext);
            boolean isRotationLocked = RotationPolicy.isRotationLocked(this.mContext);
            if (rotation == 0) {
                if (!isRotationLocked) {
                    if (this.mResetOrientationData == null) {
                        this.mResetOrientationData = new ResetOrientationData();
                        this.mResetOrientationData.locked = false;
                    }
                    this.mGlobalActionsLayout.post(new Runnable() {
                        public final void run() {
                            ActionsDialog.this.lambda$initializePanel$1$GlobalActionsDialog$ActionsDialog();
                        }
                    });
                }
                setRotationSuggestionsEnabled(false);
                ((FrameLayout) findViewById(R$id.global_actions_panel_container)).addView(this.mPanelController.getPanelContent(), new FrameLayout.LayoutParams(-1, -1));
                this.mBackgroundDrawable = this.mPanelController.getBackgroundDrawable();
                this.mScrimAlpha = 1.0f;
            } else if (isRotationLocked) {
                if (this.mResetOrientationData == null) {
                    this.mResetOrientationData = new ResetOrientationData();
                    ResetOrientationData resetOrientationData = this.mResetOrientationData;
                    resetOrientationData.locked = true;
                    resetOrientationData.rotation = rotation;
                }
                this.mGlobalActionsLayout.post(new Runnable() {
                    public final void run() {
                        ActionsDialog.this.lambda$initializePanel$0$GlobalActionsDialog$ActionsDialog();
                    }
                });
            }
        }

        public /* synthetic */ void lambda$initializePanel$0$GlobalActionsDialog$ActionsDialog() {
            RotationPolicy.setRotationLockAtAngle(this.mContext, false, 0);
        }

        public /* synthetic */ void lambda$initializePanel$1$GlobalActionsDialog$ActionsDialog() {
            RotationPolicy.setRotationLockAtAngle(this.mContext, true, 0);
        }

        private void initializeLayout() {
            setContentView(getGlobalActionsLayoutId(this.mContext));
            fixNavBarClipping();
            this.mGlobalActionsLayout = (MultiListLayout) findViewById(R$id.global_actions_view);
            this.mGlobalActionsLayout.setOutsideTouchListener(new OnClickListener() {
                public final void onClick(View view) {
                    ActionsDialog.this.lambda$initializeLayout$2$GlobalActionsDialog$ActionsDialog(view);
                }
            });
            ((View) this.mGlobalActionsLayout.getParent()).setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    ActionsDialog.this.lambda$initializeLayout$3$GlobalActionsDialog$ActionsDialog(view);
                }
            });
            this.mGlobalActionsLayout.setListViewAccessibilityDelegate(new AccessibilityDelegate() {
                public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    accessibilityEvent.getText().add(ActionsDialog.this.mContext.getString(17040078));
                    return true;
                }
            });
            this.mGlobalActionsLayout.setRotationListener(new RotationListener() {
                public final void onRotate(int i, int i2) {
                    ActionsDialog.this.onRotate(i, i2);
                }
            });
            this.mGlobalActionsLayout.setAdapter(this.mAdapter);
            if (shouldUsePanel()) {
                initializePanel();
            }
            if (this.mBackgroundDrawable == null) {
                this.mBackgroundDrawable = new ScrimDrawable();
                this.mScrimAlpha = 0.2f;
            }
            getWindow().setBackgroundDrawable(this.mBackgroundDrawable);
        }

        public /* synthetic */ void lambda$initializeLayout$2$GlobalActionsDialog$ActionsDialog(View view) {
            dismiss();
        }

        public /* synthetic */ void lambda$initializeLayout$3$GlobalActionsDialog$ActionsDialog(View view) {
            dismiss();
        }

        private void fixNavBarClipping() {
            ViewGroup viewGroup = (ViewGroup) findViewById(16908290);
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
            ViewGroup viewGroup2 = (ViewGroup) viewGroup.getParent();
            viewGroup2.setClipChildren(false);
            viewGroup2.setClipToPadding(false);
        }

        private int getGlobalActionsLayoutId(Context context) {
            int rotation = RotationUtils.getRotation(context);
            boolean z = GlobalActionsDialog.isForceGridEnabled(context) || (shouldUsePanel() && rotation == 0);
            if (rotation == 2) {
                if (z) {
                    return R$layout.global_actions_grid_seascape;
                }
                return R$layout.global_actions_column_seascape;
            } else if (z) {
                return R$layout.global_actions_grid;
            } else {
                return R$layout.global_actions_column;
            }
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            super.setCanceledOnTouchOutside(true);
            super.onStart();
            this.mGlobalActionsLayout.updateList();
            if (this.mBackgroundDrawable instanceof ScrimDrawable) {
                this.mColorExtractor.addOnColorsChangedListener(this);
                updateColors(this.mColorExtractor.getNeutralColors(), false);
            }
        }

        private void updateColors(GradientColors gradientColors, boolean z) {
            ScrimDrawable scrimDrawable = this.mBackgroundDrawable;
            if (scrimDrawable instanceof ScrimDrawable) {
                scrimDrawable.setColor(gradientColors.getMainColor(), z);
                View decorView = getWindow().getDecorView();
                if (gradientColors.supportsDarkText()) {
                    decorView.setSystemUiVisibility(8208);
                } else {
                    decorView.setSystemUiVisibility(0);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            super.onStop();
            this.mColorExtractor.removeOnColorsChangedListener(this);
        }

        public void show() {
            super.show();
            this.mShowing = true;
            this.mBackgroundDrawable.setAlpha(0);
            MultiListLayout multiListLayout = this.mGlobalActionsLayout;
            multiListLayout.setTranslationX(multiListLayout.getAnimationOffsetX());
            MultiListLayout multiListLayout2 = this.mGlobalActionsLayout;
            multiListLayout2.setTranslationY(multiListLayout2.getAnimationOffsetY());
            this.mGlobalActionsLayout.setAlpha(0.0f);
            this.mGlobalActionsLayout.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(300).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setUpdateListener(new AnimatorUpdateListener() {
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ActionsDialog.this.lambda$show$4$GlobalActionsDialog$ActionsDialog(valueAnimator);
                }
            }).start();
        }

        public /* synthetic */ void lambda$show$4$GlobalActionsDialog$ActionsDialog(ValueAnimator valueAnimator) {
            this.mBackgroundDrawable.setAlpha((int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * this.mScrimAlpha * 255.0f));
        }

        public void dismiss() {
            if (this.mShowing) {
                this.mShowing = false;
                this.mGlobalActionsLayout.setTranslationX(0.0f);
                this.mGlobalActionsLayout.setTranslationY(0.0f);
                this.mGlobalActionsLayout.setAlpha(1.0f);
                this.mGlobalActionsLayout.animate().alpha(0.0f).translationX(this.mGlobalActionsLayout.getAnimationOffsetX()).translationY(this.mGlobalActionsLayout.getAnimationOffsetY()).setDuration(300).withEndAction(new Runnable() {
                    public final void run() {
                        ActionsDialog.this.lambda$dismiss$5$GlobalActionsDialog$ActionsDialog();
                    }
                }).setInterpolator(new SystemUIInterpolators$LogAccelerateInterpolator()).setUpdateListener(new AnimatorUpdateListener() {
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ActionsDialog.this.lambda$dismiss$6$GlobalActionsDialog$ActionsDialog(valueAnimator);
                    }
                }).start();
                dismissPanel();
                resetOrientation();
            }
        }

        public /* synthetic */ void lambda$dismiss$5$GlobalActionsDialog$ActionsDialog() {
            super.dismiss();
        }

        public /* synthetic */ void lambda$dismiss$6$GlobalActionsDialog$ActionsDialog(ValueAnimator valueAnimator) {
            this.mBackgroundDrawable.setAlpha((int) ((1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue()) * this.mScrimAlpha * 255.0f));
        }

        /* access modifiers changed from: 0000 */
        public void dismissImmediately() {
            super.dismiss();
            this.mShowing = false;
            dismissPanel();
            resetOrientation();
        }

        private void dismissPanel() {
            PanelViewController panelViewController = this.mPanelController;
            if (panelViewController != null) {
                panelViewController.onDismissed();
            }
        }

        private void setRotationSuggestionsEnabled(boolean z) {
            try {
                this.mStatusBarService.disable2ForUser(z ? 0 : 16, this.mToken, this.mContext.getPackageName(), Binder.getCallingUserHandle().getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        private void resetOrientation() {
            ResetOrientationData resetOrientationData = this.mResetOrientationData;
            if (resetOrientationData != null) {
                RotationPolicy.setRotationLockAtAngle(this.mContext, resetOrientationData.locked, resetOrientationData.rotation);
            }
            setRotationSuggestionsEnabled(true);
        }

        public void onColorsChanged(ColorExtractor colorExtractor, int i) {
            if (this.mKeyguardShowing) {
                if ((i & 2) != 0) {
                    updateColors(colorExtractor.getColors(2), true);
                }
            } else if ((i & 1) != 0) {
                updateColors(colorExtractor.getColors(1), true);
            }
        }

        public void setKeyguardShowing(boolean z) {
            this.mKeyguardShowing = z;
        }

        public void refreshDialog() {
            initializeLayout();
            this.mGlobalActionsLayout.updateList();
        }

        public void onRotate(int i, int i2) {
            if (this.mShowing) {
                refreshDialog();
            }
        }
    }

    private final class BootloaderAction extends SinglePressAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return false;
        }

        private BootloaderAction() {
            super(R$drawable.ic_bootloader, 84869392);
        }

        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.op_reboot(false, "bootloader", false);
        }
    }

    private class BugReportAction extends SinglePressAction implements LongPressAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public BugReportAction() {
            super(17302451, 17039624);
        }

        public void onPress() {
            if (!ActivityManager.isUserAMonkey()) {
                GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            MetricsLogger.action(GlobalActionsDialog.this.mContext, 292);
                            ActivityManager.getService().requestBugReport(1);
                        } catch (RemoteException unused) {
                        }
                    }
                }, 500);
            }
        }

        public boolean onLongPress() {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            try {
                MetricsLogger.action(GlobalActionsDialog.this.mContext, 293);
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException unused) {
            }
            return false;
        }
    }

    private abstract class EmergencyAction extends SinglePressAction {
        public boolean showBeforeProvisioning() {
            return true;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        EmergencyAction(int i, int i2) {
            super(i, i2);
        }

        public boolean shouldBeSeparated() {
            return GlobalActionsDialog.shouldUseSeparatedView();
        }

        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            int i;
            View create = super.create(context, view, viewGroup, layoutInflater);
            if (shouldBeSeparated()) {
                i = create.getResources().getColor(R$color.global_actions_alert_text);
            } else {
                i = create.getResources().getColor(R$color.global_actions_text);
            }
            TextView textView = (TextView) create.findViewById(16908299);
            textView.setTextColor(i);
            textView.setSelected(true);
            ((ImageView) create.findViewById(16908294)).getDrawable().setTint(i);
            return create;
        }
    }

    private class EmergencyAffordanceAction extends EmergencyAction {
        EmergencyAffordanceAction() {
            super(17302201, 17040066);
        }

        public void onPress() {
            GlobalActionsDialog.this.mEmergencyAffordanceManager.performEmergencyCall();
        }
    }

    private class EmergencyDialerAction extends EmergencyAction {
        private EmergencyDialerAction() {
            super(R$drawable.ic_emergency_star, R$string.global_action_emergency);
        }

        public void onPress() {
            MetricsLogger.action(GlobalActionsDialog.this.mContext, 1569);
            Intent intent = new Intent("oneplus.telephony.action.EMERGENCY_ASSISTANCE");
            intent.addFlags(343932928);
            intent.putExtra("com.android.phone.EmergencyDialer.extra.ENTRY_TYPE", 2);
            GlobalActionsDialog.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private final class LogoutAction extends SinglePressAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        private LogoutAction() {
            super(17302501, 17040069);
        }

        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    LogoutAction.this.lambda$onPress$0$GlobalActionsDialog$LogoutAction();
                }
            }, 500);
        }

        public /* synthetic */ void lambda$onPress$0$GlobalActionsDialog$LogoutAction() {
            try {
                int i = GlobalActionsDialog.this.getCurrentUser().id;
                ActivityManager.getService().switchUser(0);
                ActivityManager.getService().stopUser(i, true, null);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Couldn't logout user ");
                sb.append(e);
                Log.e("GlobalActionsDialog", sb.toString());
            }
        }
    }

    private interface LongPressAction extends Action {
        boolean onLongPress();
    }

    public class MyAdapter extends MultiListAdapter {
        public boolean areAllItemsEnabled() {
            return false;
        }

        public long getItemId(int i) {
            return (long) i;
        }

        public MyAdapter() {
        }

        private int countItems(boolean z) {
            int i = 0;
            for (int i2 = 0; i2 < GlobalActionsDialog.this.mItems.size(); i2++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i2);
                if (shouldBeShown(action) && action.shouldBeSeparated() == z) {
                    i++;
                }
            }
            return i;
        }

        private boolean shouldBeShown(Action action) {
            if (GlobalActionsDialog.this.mKeyguardShowing && !action.showDuringKeyguard()) {
                return false;
            }
            if (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning()) {
                return true;
            }
            return false;
        }

        public int countSeparatedItems() {
            return countItems(true);
        }

        public int countListItems() {
            return countItems(false);
        }

        public int getCount() {
            return countSeparatedItems() + countListItems();
        }

        public boolean isEnabled(int i) {
            return getItem(i).isEnabled();
        }

        public Action getItem(int i) {
            int i2 = 0;
            for (int i3 = 0; i3 < GlobalActionsDialog.this.mItems.size(); i3++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i3);
                if (shouldBeShown(action)) {
                    if (i2 == i) {
                        return action;
                    }
                    i2++;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("position ");
            sb.append(i);
            sb.append(" out of range of showable actions, filtered count=");
            sb.append(getCount());
            sb.append(", keyguardshowing=");
            sb.append(GlobalActionsDialog.this.mKeyguardShowing);
            sb.append(", provisioned=");
            sb.append(GlobalActionsDialog.this.mDeviceProvisioned);
            throw new IllegalArgumentException(sb.toString());
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            View create = getItem(i).create(GlobalActionsDialog.this.mContext, view, viewGroup, LayoutInflater.from(GlobalActionsDialog.this.mContext));
            create.setOnClickListener(new OnClickListener(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    MyAdapter.this.lambda$getView$0$GlobalActionsDialog$MyAdapter(this.f$1, view);
                }
            });
            create.setOnLongClickListener(new OnLongClickListener(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final boolean onLongClick(View view) {
                    return MyAdapter.this.lambda$getView$1$GlobalActionsDialog$MyAdapter(this.f$1, view);
                }
            });
            return create;
        }

        public /* synthetic */ void lambda$getView$0$GlobalActionsDialog$MyAdapter(int i, View view) {
            onClickItem(i);
        }

        public /* synthetic */ boolean lambda$getView$1$GlobalActionsDialog$MyAdapter(int i, View view) {
            return onLongClickItem(i);
        }

        public boolean onLongClickItem(int i) {
            Action item = GlobalActionsDialog.this.mAdapter.getItem(i);
            if (!(item instanceof LongPressAction)) {
                return false;
            }
            if (GlobalActionsDialog.this.mDialog != null) {
                GlobalActionsDialog.this.mDialog.dismiss();
            }
            return ((LongPressAction) item).onLongPress();
        }

        public void onClickItem(int i) {
            Action item = GlobalActionsDialog.this.mAdapter.getItem(i);
            if (!(item instanceof SilentModeTriStateAction) && GlobalActionsDialog.this.mDialog != null) {
                GlobalActionsDialog.this.mDialog.dismiss();
            }
            item.onPress();
        }

        public boolean shouldBeSeparated(int i) {
            return getItem(i).shouldBeSeparated();
        }
    }

    private final class PowerAction extends SinglePressAction implements LongPressAction {
        public boolean showBeforeProvisioning() {
            return true;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        private PowerAction() {
            super(17301552, 17040070);
        }

        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.shutdown();
        }
    }

    private final class RecoveryAction extends SinglePressAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return false;
        }

        private RecoveryAction() {
            super(R$drawable.ic_recovery, 84869397);
        }

        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    RecoveryAction.this.lambda$onPress$0$GlobalActionsDialog$RecoveryAction();
                }
            }, 500);
        }

        public /* synthetic */ void lambda$onPress$0$GlobalActionsDialog$RecoveryAction() {
            GlobalActionsDialog.this.mWindowManagerFuncs.op_reboot(false, "recovery", false);
        }
    }

    private final class RestartAction extends SinglePressAction implements LongPressAction {
        public boolean showBeforeProvisioning() {
            return true;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        private RestartAction() {
            super(17302787, 17040071);
        }

        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(false);
        }
    }

    private class ScreenshotAction extends SinglePressAction implements LongPressAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public ScreenshotAction() {
            super(R$drawable.ic_global_action_screenshot, 17040072);
        }

        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        WindowManagerGlobal.getWindowManagerService().takeOPScreenshot(1, 0);
                    } catch (RemoteException e) {
                        Log.e("GlobalActionsDialog", "Error while trying to takeOPScreenshot.", e);
                    }
                    MetricsLogger.action(GlobalActionsDialog.this.mContext, 1282);
                }
            }, 500);
        }

        public boolean onLongPress() {
            if (FeatureFlagUtils.isEnabled(GlobalActionsDialog.this.mContext, "settings_screenrecord_long_press")) {
                GlobalActionsDialog.this.mScreenRecordHelper.launchRecordPrompt();
            } else {
                onPress();
            }
            return true;
        }
    }

    private class SilentModeToggleAction extends ToggleAction {
        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public SilentModeToggleAction() {
            super(17302300, 17302299, 17040076, 17040075, 17040074);
        }

        /* access modifiers changed from: 0000 */
        public void onToggle(boolean z) {
            if (z) {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(0);
            } else {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(2);
            }
        }
    }

    private static class SilentModeTriStateAction implements Action, OnClickListener {
        private final int[] ITEM_IDS = {16909197, 16909198, 16909199};
        private final AudioManager mAudioManager;
        private final Handler mHandler;

        private int indexToRingerMode(int i) {
            return i;
        }

        private int ringerModeToIndex(int i) {
            return i;
        }

        public boolean isEnabled() {
            return true;
        }

        public void onPress() {
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        SilentModeTriStateAction(AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandler = handler;
        }

        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View inflate = layoutInflater.inflate(17367159, viewGroup, false);
            int ringerMode = this.mAudioManager.getRingerMode();
            ringerModeToIndex(ringerMode);
            int i = 0;
            while (i < 3) {
                View findViewById = inflate.findViewById(this.ITEM_IDS[i]);
                findViewById.setSelected(ringerMode == i);
                findViewById.setTag(Integer.valueOf(i));
                findViewById.setOnClickListener(this);
                i++;
            }
            return inflate;
        }

        public void onClick(View view) {
            if (view.getTag() instanceof Integer) {
                int intValue = ((Integer) view.getTag()).intValue();
                AudioManager audioManager = this.mAudioManager;
                indexToRingerMode(intValue);
                audioManager.setRingerMode(intValue);
                this.mHandler.sendEmptyMessageDelayed(0, 300);
            }
        }
    }

    private static abstract class SinglePressAction implements Action {
        private final Drawable mIcon;
        private final int mIconResId;
        private final CharSequence mMessage;
        private final int mMessageResId;

        public String getStatus() {
            return null;
        }

        public boolean isEnabled() {
            return true;
        }

        public abstract void onPress();

        protected SinglePressAction(int i, int i2) {
            this.mIconResId = i;
            this.mMessageResId = i2;
            this.mMessage = null;
            this.mIcon = null;
        }

        protected SinglePressAction(int i, Drawable drawable, CharSequence charSequence) {
            this.mIconResId = i;
            this.mMessageResId = 0;
            this.mMessage = charSequence;
            this.mIcon = drawable;
        }

        /* access modifiers changed from: protected */
        public int getActionLayoutId(Context context) {
            return R$layout.global_actions_grid_item;
        }

        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View inflate = layoutInflater.inflate(getActionLayoutId(context), viewGroup, false);
            ImageView imageView = (ImageView) inflate.findViewById(16908294);
            TextView textView = (TextView) inflate.findViewById(16908299);
            textView.setSelected(true);
            TextView textView2 = (TextView) inflate.findViewById(16909406);
            String status = getStatus();
            if (!TextUtils.isEmpty(status)) {
                textView2.setText(status);
            } else {
                textView2.setVisibility(8);
            }
            Drawable drawable = this.mIcon;
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                imageView.setScaleType(ScaleType.CENTER_CROP);
            } else {
                int i = this.mIconResId;
                if (i != 0) {
                    imageView.setImageDrawable(context.getDrawable(i));
                }
            }
            CharSequence charSequence = this.mMessage;
            if (charSequence != null) {
                textView.setText(charSequence);
            } else {
                textView.setText(this.mMessageResId);
            }
            return inflate;
        }
    }

    private static abstract class ToggleAction implements Action {
        protected int mDisabledIconResid;
        protected int mDisabledStatusMessageResId;
        protected int mEnabledIconResId;
        protected int mEnabledStatusMessageResId;
        protected int mMessageResId;
        protected State mState = State.Off;

        enum State {
            Off(false),
            TurningOn(true),
            TurningOff(true),
            On(false);
            
            private final boolean inTransition;

            private State(boolean z) {
                this.inTransition = z;
            }

            public boolean inTransition() {
                return this.inTransition;
            }
        }

        /* access modifiers changed from: 0000 */
        public abstract void onToggle(boolean z);

        /* access modifiers changed from: 0000 */
        public void willCreate() {
        }

        public ToggleAction(int i, int i2, int i3, int i4, int i5) {
            this.mEnabledIconResId = i;
            this.mDisabledIconResid = i2;
            this.mMessageResId = i3;
            this.mEnabledStatusMessageResId = i4;
            this.mDisabledStatusMessageResId = i5;
        }

        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            willCreate();
            View inflate = layoutInflater.inflate(17367158, viewGroup, false);
            ImageView imageView = (ImageView) inflate.findViewById(16908294);
            TextView textView = (TextView) inflate.findViewById(16908299);
            TextView textView2 = (TextView) inflate.findViewById(16909406);
            boolean isEnabled = isEnabled();
            boolean z = true;
            if (textView != null) {
                textView.setText(this.mMessageResId);
                textView.setEnabled(isEnabled);
                textView.setSelected(true);
            }
            State state = this.mState;
            if (!(state == State.On || state == State.TurningOn)) {
                z = false;
            }
            if (imageView != null) {
                imageView.setImageDrawable(context.getDrawable(z ? this.mEnabledIconResId : this.mDisabledIconResid));
                imageView.setEnabled(isEnabled);
            }
            if (textView2 != null) {
                textView2.setText(z ? this.mEnabledStatusMessageResId : this.mDisabledStatusMessageResId);
                textView2.setVisibility(0);
                textView2.setEnabled(isEnabled);
            }
            inflate.setEnabled(isEnabled);
            return inflate;
        }

        public final void onPress() {
            if (this.mState.inTransition()) {
                Log.w("GlobalActionsDialog", "shouldn't be able to toggle when in transition");
                return;
            }
            boolean z = this.mState != State.On;
            onToggle(z);
            changeStateFromPress(z);
        }

        public boolean isEnabled() {
            return !this.mState.inTransition();
        }

        /* access modifiers changed from: protected */
        public void changeStateFromPress(boolean z) {
            this.mState = z ? State.On : State.Off;
        }

        public void updateState(State state) {
            this.mState = state;
        }
    }

    /* access modifiers changed from: private */
    public static boolean shouldUseSeparatedView() {
        return true;
    }

    public GlobalActionsDialog(Context context, GlobalActionsManager globalActionsManager) {
        boolean z = false;
        this.mContext = new ContextThemeWrapper(context, R$style.qs_theme);
        this.mWindowManagerFuncs = globalActionsManager;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mDreamManager = Stub.asInterface(ServiceManager.getService("dreams"));
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mHasTelephony = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator != null && vibrator.hasVibrator()) {
            z = true;
        }
        this.mHasVibrator = z;
        this.mShowSilentToggle = !this.mContext.getResources().getBoolean(17891564);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
        this.mScreenshotHelper = new ScreenshotHelper(context);
        this.mScreenRecordHelper = new ScreenRecordHelper(context);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        UnlockMethodCache instance = UnlockMethodCache.getInstance(context);
        instance.addListener(new OnUnlockMethodChangedListener(instance) {
            private final /* synthetic */ UnlockMethodCache f$1;

            {
                this.f$1 = r2;
            }

            public final void onUnlockMethodStateChanged() {
                GlobalActionsDialog.this.lambda$new$0$GlobalActionsDialog(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$new$0$GlobalActionsDialog(UnlockMethodCache unlockMethodCache) {
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null && actionsDialog.mPanelController != null) {
            this.mDialog.mPanelController.onDeviceLockStateChanged(!unlockMethodCache.canSkipBouncer());
        }
    }

    public void showDialog(boolean z, boolean z2, GlobalActionsPanelPlugin globalActionsPanelPlugin) {
        this.mKeyguardShowing = z;
        this.mDeviceProvisioned = z2;
        this.mPanelPlugin = globalActionsPanelPlugin;
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null) {
            actionsDialog.dismiss();
            this.mDialog = null;
            this.mHandler.sendEmptyMessage(2);
            return;
        }
        handleShow();
    }

    public void dismissDialog() {
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessage(0);
    }

    private void awakenIfNecessary() {
        IDreamManager iDreamManager = this.mDreamManager;
        if (iDreamManager != null) {
            try {
                if (iDreamManager.isDreaming()) {
                    this.mDreamManager.awaken();
                }
            } catch (RemoteException unused) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleShow() {
        awakenIfNecessary();
        this.mDialog = createDialog();
        prepareDialog();
        if (this.mAdapter.getCount() != 1 || !(this.mAdapter.getItem(0) instanceof SinglePressAction) || (this.mAdapter.getItem(0) instanceof LongPressAction)) {
            LayoutParams attributes = this.mDialog.getWindow().getAttributes();
            attributes.setTitle("ActionsDialog");
            attributes.layoutInDisplayCutoutMode = 1;
            this.mDialog.getWindow().setAttributes(attributes);
            this.mDialog.show();
            this.mWindowManagerFuncs.onGlobalActionsShown();
            return;
        }
        ((SinglePressAction) this.mAdapter.getItem(0)).onPress();
    }

    private ActionsDialog createDialog() {
        PanelViewController panelViewController;
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mAudioManager, this.mHandler);
        }
        C08751 r3 = new ToggleAction(17302447, 17302449, 17040081, 17040080, 17040079) {
            public boolean showBeforeProvisioning() {
                return false;
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            /* access modifiers changed from: 0000 */
            public void onToggle(boolean z) {
                if (!GlobalActionsDialog.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    GlobalActionsDialog.this.changeAirplaneModeSystemSetting(z);
                    return;
                }
                GlobalActionsDialog.this.mIsWaitingForEcmExit = true;
                Intent intent = new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", null);
                intent.addFlags(268435456);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            /* access modifiers changed from: protected */
            public void changeStateFromPress(boolean z) {
                if (GlobalActionsDialog.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = z ? State.TurningOn : State.TurningOff;
                    GlobalActionsDialog.this.mAirplaneState = this.mState;
                }
            }
        };
        this.mAirplaneModeOn = r3;
        onAirplaneModeChanged();
        this.mItems = new ArrayList<>();
        String[] stringArray = this.mContext.getResources().getStringArray(17236055);
        ArraySet arraySet = new ArraySet();
        this.mHasLogoutButton = false;
        this.mHasLockdownButton = false;
        int i = 0;
        while (true) {
            panelViewController = null;
            if (i >= stringArray.length) {
                break;
            }
            String str = stringArray[i];
            if (!arraySet.contains(str)) {
                if ("power".equals(str)) {
                    this.mItems.add(new PowerAction());
                } else if ("airplane".equals(str)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if ("bugreport".equals(str)) {
                    if (Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", 0) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(new BugReportAction());
                    }
                } else if ("silent".equals(str)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if ("users".equals(str)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if ("settings".equals(str)) {
                    this.mItems.add(getSettingsAction());
                } else if ("lockdown".equals(str)) {
                    if (Secure.getIntForUser(this.mContext.getContentResolver(), "lockdown_in_power_menu", 0, getCurrentUser().id) != 0 && shouldDisplayLockdown()) {
                        this.mItems.add(getLockdownAction());
                        this.mHasLockdownButton = true;
                    }
                } else if ("voiceassist".equals(str)) {
                    this.mItems.add(getVoiceAssistAction());
                } else if ("assist".equals(str)) {
                    this.mItems.add(getAssistAction());
                } else if ("restart".equals(str)) {
                    this.mItems.add(new RestartAction());
                } else if ("screenshot".equals(str)) {
                    this.mItems.add(new ScreenshotAction());
                } else if ("logout".equals(str)) {
                    if (this.mDevicePolicyManager.isLogoutEnabled() && getCurrentUser().id != 0) {
                        this.mItems.add(new LogoutAction());
                        this.mHasLogoutButton = true;
                    }
                } else if ("emergency".equals(str)) {
                    if (!this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
                        this.mItems.add(new EmergencyDialerAction());
                    }
                } else if ("recovery".equals(str)) {
                    if (isAdvancedRebootEnabled()) {
                        this.mItems.add(new RecoveryAction());
                    }
                } else if (!"bootloader".equals(str)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid global action key ");
                    sb.append(str);
                    Log.e("GlobalActionsDialog", sb.toString());
                } else if (isAdvancedRebootEnabled()) {
                    this.mItems.add(new BootloaderAction());
                }
                arraySet.add(str);
            }
            i++;
        }
        if (this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
            this.mItems.add(new EmergencyAffordanceAction());
        }
        this.mAdapter = new MyAdapter();
        GlobalActionsPanelPlugin globalActionsPanelPlugin = this.mPanelPlugin;
        if (globalActionsPanelPlugin != null) {
            panelViewController = globalActionsPanelPlugin.onPanelShown(new Callbacks() {
                public void dismissGlobalActionsMenu() {
                    if (GlobalActionsDialog.this.mDialog != null) {
                        GlobalActionsDialog.this.mDialog.dismiss();
                    }
                }

                public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent) {
                    GlobalActionsDialog.this.mActivityStarter.startPendingIntentDismissingKeyguard(pendingIntent);
                }
            }, this.mKeyguardManager.isDeviceLocked());
        }
        ActionsDialog actionsDialog = new ActionsDialog(this.mContext, this.mAdapter, panelViewController);
        actionsDialog.setCanceledOnTouchOutside(false);
        actionsDialog.setKeyguardShowing(this.mKeyguardShowing);
        actionsDialog.setOnDismissListener(this);
        actionsDialog.setOnShowListener(this);
        return actionsDialog;
    }

    private boolean shouldDisplayLockdown() {
        int i = getCurrentUser().id;
        boolean z = false;
        if (!this.mKeyguardManager.isDeviceSecure(i)) {
            return false;
        }
        int strongAuthForUser = this.mLockPatternUtils.getStrongAuthForUser(i);
        if (strongAuthForUser == 0 || strongAuthForUser == 4) {
            z = true;
        }
        return z;
    }

    public void onUiModeChanged() {
        this.mContext.getTheme().applyStyle(this.mContext.getThemeResId(), true);
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null && actionsDialog.isShowing()) {
            this.mDialog.refreshDialog();
        }
    }

    public void destroy() {
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    private Action getSettingsAction() {
        return new SinglePressAction(17302795, 17040073) {
            public boolean showBeforeProvisioning() {
                return true;
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public void onPress() {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getAssistAction() {
        return new SinglePressAction(17302282, 17040064) {
            public boolean showBeforeProvisioning() {
                return true;
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public void onPress() {
                Intent intent = new Intent("android.intent.action.ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getVoiceAssistAction() {
        return new SinglePressAction(17302835, 17040077) {
            public boolean showBeforeProvisioning() {
                return true;
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public void onPress() {
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getLockdownAction() {
        return new SinglePressAction(17302454, 17040068) {
            public boolean showBeforeProvisioning() {
                return false;
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public void onPress() {
                new LockPatternUtils(GlobalActionsDialog.this.mContext).requireStrongAuth(32, -1);
                GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            WindowManagerGlobal.getWindowManagerService().lockNow(null);
                            new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).post(new Runnable(this) {
                                private final /* synthetic */ C08841 f$0;

                                public final 
/*
Method generation error in method: com.android.systemui.globalactions.-$$Lambda$GlobalActionsDialog$6$1$1BpFTjs5a81AxMWdZQ10CAffkio.run():null, dex: classes.dex
                                java.lang.NullPointerException
                                	at jadx.core.codegen.ClassGen.useType(ClassGen.java:442)
                                	at jadx.core.codegen.MethodGen.addDefinition(MethodGen.java:109)
                                	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:311)
                                	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                                	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:299)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:68)
                                	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:303)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:110)
                                	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:76)
                                	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:44)
                                	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:32)
                                	at jadx.core.codegen.CodeGen.generate(CodeGen.java:20)
                                	at jadx.core.ProcessClass.process(ProcessClass.java:36)
                                	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
                                	at jadx.api.JavaClass.decompile(JavaClass.java:62)
                                	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
                                
*/
                            });
                        } catch (RemoteException e) {
                            Log.e("GlobalActionsDialog", "Error while trying to lock device.", e);
                        }
                    }

                    public /* synthetic */ void lambda$run$0$GlobalActionsDialog$6$1() {
                        GlobalActionsDialog.this.lockProfiles();
                    }
                }, 300);
            }
        };
    }

    /* access modifiers changed from: private */
    public void lockProfiles() {
        int[] enabledProfileIds;
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        TrustManager trustManager = (TrustManager) this.mContext.getSystemService("trust");
        int i = getCurrentUser().id;
        for (int i2 : userManager.getEnabledProfileIds(i)) {
            if (i2 != i) {
                trustManager.setDeviceLockedForUser(i2, true);
            }
        }
    }

    /* access modifiers changed from: private */
    public UserInfo getCurrentUser() {
        try {
            return ActivityManager.getService().getCurrentUser();
        } catch (RemoteException unused) {
            return null;
        }
    }

    private boolean isCurrentUserOwner() {
        UserInfo currentUser = getCurrentUser();
        return currentUser == null || currentUser.isPrimary();
    }

    private void addUsersToMenu(ArrayList<Action> arrayList) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.isUserSwitcherEnabled()) {
            List<UserInfo> users = userManager.getUsers();
            UserInfo currentUser = getCurrentUser();
            for (final UserInfo userInfo : users) {
                if (userInfo.supportsSwitchToByUser()) {
                    boolean z = true;
                    if (currentUser != null ? currentUser.id != userInfo.id : userInfo.id != 0) {
                        z = false;
                    }
                    String str = userInfo.iconPath;
                    Drawable createFromPath = str != null ? Drawable.createFromPath(str) : null;
                    StringBuilder sb = new StringBuilder();
                    String str2 = userInfo.name;
                    if (str2 == null) {
                        str2 = "Primary";
                    }
                    sb.append(str2);
                    sb.append(z ? " ✔" : "");
                    C08857 r3 = new SinglePressAction(17302670, createFromPath, sb.toString()) {
                        public boolean showBeforeProvisioning() {
                            return false;
                        }

                        public boolean showDuringKeyguard() {
                            return true;
                        }

                        public void onPress() {
                            try {
                                ActivityManager.getService().switchUser(userInfo.id);
                            } catch (RemoteException e) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Couldn't switch user ");
                                sb.append(e);
                                Log.e("GlobalActionsDialog", sb.toString());
                            }
                        }
                    };
                    arrayList.add(r3);
                }
            }
        }
    }

    private void prepareDialog() {
        refreshSilentMode();
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
        this.mAdapter.notifyDataSetChanged();
        if (this.mShowSilentToggle) {
            this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        }
    }

    /* access modifiers changed from: private */
    public void refreshSilentMode() {
        if (!this.mHasVibrator) {
            ((ToggleAction) this.mSilentModeAction).updateState(this.mAudioManager.getRingerMode() != 2 ? State.On : State.Off);
        }
    }

    public void onDismiss(DialogInterface dialogInterface) {
        this.mWindowManagerFuncs.onGlobalActionsHidden();
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w("GlobalActionsDialog", e);
            }
        }
    }

    public void onShow(DialogInterface dialogInterface) {
        MetricsLogger.visible(this.mContext, 1568);
    }

    /* access modifiers changed from: private */
    public void onAirplaneModeChanged() {
        if (!this.mHasTelephony) {
            boolean z = false;
            if (Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
                z = true;
            }
            this.mAirplaneState = z ? State.On : State.Off;
            this.mAirplaneModeOn.updateState(this.mAirplaneState);
        }
    }

    /* access modifiers changed from: private */
    public void changeAirplaneModeSystemSetting(boolean z) {
        Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", z ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra("state", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = z ? State.On : State.Off;
        }
    }

    private static boolean isPanelDebugModeEnabled(Context context) {
        return Secure.getInt(context.getContentResolver(), "global_actions_panel_debug_enabled", 0) == 1;
    }

    /* access modifiers changed from: private */
    public static boolean isForceGridEnabled(Context context) {
        return isPanelDebugModeEnabled(context);
    }

    private boolean isAdvancedRebootEnabled() {
        return Secure.getInt(this.mContext.getContentResolver(), "advanced_reboot", 0) != 0;
    }
}
