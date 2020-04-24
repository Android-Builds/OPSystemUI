package com.android.systemui.p007qs;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.p007qs.TouchAnimator.Builder;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.phone.WLBSwitch;
import com.android.systemui.statusbar.phone.WLBSwitchController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;

/* renamed from: com.android.systemui.qs.QSFooterImpl */
public class QSFooterImpl extends FrameLayout implements QSFooter, OnClickListener, OnUserInfoChangedListener {
    private View mActionsContainer;
    private final ActivityStarter mActivityStarter;
    private QSCarrierGroup mCarrierGroup;
    private final ContentObserver mDeveloperSettingsObserver;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private View mDivider;
    private View mDragHandle;
    protected TouchAnimator mDragHandleAnimator;
    protected View mEdit;
    private OnClickListener mExpandClickListener;
    private boolean mExpanded;
    private float mExpansionAmount;
    protected TouchAnimator mFooterAnimator;
    private boolean mIsGuestUser;
    private boolean mIsLandscape;
    private boolean mListening;
    private ImageView mMultiUserAvatar;
    protected MultiUserSwitch mMultiUserSwitch;
    private PageIndicator mPageIndicator;
    private boolean mQsDisabled;
    private QSPanel mQsPanel;
    private SettingsButton mSettingsButton;
    private TouchAnimator mSettingsCogAnimator;
    protected View mSettingsContainer;
    private boolean mShowEditIcon;
    private final UserInfoController mUserInfoController;
    private ImageView mWlbButton;
    private WLBSwitch mWlbSwitch;

    static /* synthetic */ void lambda$onClick$4() {
    }

    public QSFooterImpl(Context context, AttributeSet attributeSet, ActivityStarter activityStarter, UserInfoController userInfoController, DeviceProvisionedController deviceProvisionedController) {
        super(context, attributeSet);
        this.mShowEditIcon = true;
        this.mDeveloperSettingsObserver = new ContentObserver(new Handler(this.mContext.getMainLooper())) {
            public void onChange(boolean z, Uri uri) {
                super.onChange(z, uri);
                QSFooterImpl.this.setBuildText();
            }
        };
        this.mActivityStarter = activityStarter;
        this.mUserInfoController = userInfoController;
        this.mDeviceProvisionedController = deviceProvisionedController;
    }

    public QSFooterImpl(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, (ActivityStarter) Dependency.get(ActivityStarter.class), (UserInfoController) Dependency.get(UserInfoController.class), (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mEdit = findViewById(16908291);
        if (canShowEditIcon()) {
            this.mEdit.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    QSFooterImpl.this.lambda$onFinishInflate$1$QSFooterImpl(view);
                }
            });
        }
        this.mDivider = findViewById(R$id.qs_footer_divider);
        this.mPageIndicator = (PageIndicator) findViewById(R$id.footer_page_indicator);
        this.mSettingsButton = (SettingsButton) findViewById(R$id.settings_button);
        this.mSettingsContainer = findViewById(R$id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R$id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(R$id.multi_user_avatar);
        this.mWlbSwitch = (WLBSwitch) findViewById(R$id.wlb_switch);
        this.mWlbButton = (ImageView) findViewById(R$id.wlb_avatar);
        if (!OpUtils.isWLBAllowed(this.mContext) || OpUtils.isWLBFeatureDisable(this.mContext)) {
            this.mWlbSwitch.setVisibility(8);
        }
        this.mDragHandle = findViewById(R$id.qs_drag_handle_view);
        this.mActionsContainer = findViewById(R$id.qs_footer_actions_container);
        this.mCarrierGroup = (QSCarrierGroup) findViewById(R$id.carrier_group);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        updateResources();
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                QSFooterImpl.this.lambda$onFinishInflate$2$QSFooterImpl(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        setImportantForAccessibility(1);
        updateThemeColor();
        updateEverything();
        setBuildText();
    }

    public /* synthetic */ void lambda$onFinishInflate$1$QSFooterImpl(View view) {
        this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable(view) {
            private final /* synthetic */ View f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                QSFooterImpl.this.lambda$onFinishInflate$0$QSFooterImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onFinishInflate$0$QSFooterImpl(View view) {
        if (this.mQsPanel.getVisibility() == 0) {
            this.mQsPanel.showEdit(view);
        } else {
            Log.i("QSFooterImpl", "Don't show Editor when mQsPanel hide");
        }
    }

    public /* synthetic */ void lambda$onFinishInflate$2$QSFooterImpl(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateAnimator(i3 - i);
    }

    private void updateThemeColor() {
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_BUTTON);
        this.mSettingsButton.setImageTintList(ColorStateList.valueOf(color));
        ((ImageView) this.mEdit).setImageTintList(ColorStateList.valueOf(color));
        this.mDivider.setBackgroundColor(ThemeColorUtils.getColor(ThemeColorUtils.QS_SEPARATOR));
        if (OpUtils.isMCLVersion() && ThemeColorUtils.getCurrentTheme() != 0) {
            setBackgroundResource(R$drawable.op_qs_footer_background_my);
        }
        WLBSwitchController wLBSwitchController = (WLBSwitchController) Dependency.get(WLBSwitchController.class);
        if (wLBSwitchController != null) {
            this.mWlbButton.setImageTintList(wLBSwitchController.getCurrentMode() > 0 ? ColorStateList.valueOf(ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT)) : ColorStateList.valueOf(color));
        }
    }

    /* access modifiers changed from: private */
    public void setBuildText() {
        TextView textView = (TextView) findViewById(R$id.build);
        if (textView != null) {
            if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this.mContext)) {
                textView.setText(this.mContext.getString(17039623, new Object[]{VERSION.RELEASE, Build.ID}));
                textView.setVisibility(0);
            } else {
                textView.setVisibility(8);
            }
        }
    }

    private void updateAnimator(int i) {
        setExpansion(this.mExpansionAmount);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mIsLandscape = configuration.orientation == 2;
        if (this.mIsLandscape) {
            this.mShowEditIcon = false;
        } else {
            this.mShowEditIcon = true;
        }
        if (canShowEditIcon()) {
            this.mEdit.setVisibility(0);
        } else {
            this.mEdit.setVisibility(8);
        }
        updateResources();
        updateEverything();
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
        updateDragHandleAnimator();
    }

    private void updateFooterAnimator() {
        this.mFooterAnimator = createFooterAnimator();
    }

    private void updateDragHandleAnimator() {
        this.mDragHandleAnimator = createDragHandleAnimator();
    }

    private TouchAnimator createFooterAnimator() {
        Builder builder = new Builder();
        builder.setStartDelay(0.46f);
        String str = "alpha";
        if (canShowEditIcon()) {
            builder.addFloat(this.mEdit, str, 0.0f, 1.0f);
        }
        builder.addFloat(this.mMultiUserSwitch, str, 0.0f, 1.0f);
        builder.addFloat(this.mPageIndicator, str, 0.0f, 1.0f);
        return builder.build();
    }

    private TouchAnimator createDragHandleAnimator() {
        Builder builder = new Builder();
        builder.addFloat(this.mDragHandle, "alpha", 1.0f, 0.0f, 0.0f);
        return builder.build();
    }

    public void setKeyguardShowing(boolean z) {
        setExpansion(this.mExpansionAmount);
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded != z) {
            this.mExpanded = z;
            updateEverything();
        }
    }

    public void setExpansion(float f) {
        this.mExpansionAmount = f;
        TouchAnimator touchAnimator = this.mSettingsCogAnimator;
        if (touchAnimator != null) {
            touchAnimator.setPosition(f);
        }
        TouchAnimator touchAnimator2 = this.mFooterAnimator;
        if (touchAnimator2 != null) {
            touchAnimator2.setPosition(f);
        }
        TouchAnimator touchAnimator3 = this.mDragHandleAnimator;
        if (touchAnimator3 != null) {
            touchAnimator3.setPosition(f);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("development_settings_enabled"), false, this.mDeveloperSettingsObserver, -1);
    }

    public void onDetachedFromWindow() {
        setListening(false);
        this.mContext.getContentResolver().unregisterContentObserver(this.mDeveloperSettingsObserver);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean z) {
        if (z != this.mListening) {
            this.mListening = z;
            this.mCarrierGroup.setListening(this.mListening);
            updateListeners();
            updateEverything();
        }
    }

    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i == 262144) {
            OnClickListener onClickListener = this.mExpandClickListener;
            if (onClickListener != null) {
                onClickListener.onClick(null);
                return true;
            }
        }
        return super.performAccessibilityAction(i, bundle);
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_EXPAND);
    }

    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 != this.mQsDisabled) {
            this.mQsDisabled = z2;
            updateEverything();
        }
    }

    public void updateEverything() {
        post(new Runnable() {
            public final void run() {
                QSFooterImpl.this.lambda$updateEverything$3$QSFooterImpl();
            }
        });
    }

    public /* synthetic */ void lambda$updateEverything$3$QSFooterImpl() {
        updateVisibilities();
        updateClickabilities();
        setClickable(false);
    }

    private void updateClickabilities() {
        MultiUserSwitch multiUserSwitch = this.mMultiUserSwitch;
        boolean z = true;
        multiUserSwitch.setClickable(multiUserSwitch.getVisibility() == 0);
        View view = this.mEdit;
        view.setClickable(view.getVisibility() == 0);
        SettingsButton settingsButton = this.mSettingsButton;
        if (settingsButton.getVisibility() != 0) {
            z = false;
        }
        settingsButton.setClickable(z);
    }

    private void updateVisibilities() {
        this.mSettingsContainer.setVisibility(this.mQsDisabled ? 8 : 0);
        int i = 4;
        if (canShowEditIcon()) {
            this.mEdit.setVisibility(!this.mExpanded ? 4 : 0);
        } else {
            this.mEdit.setVisibility(8);
        }
        boolean isDeviceInDemoMode = UserManager.isDeviceInDemoMode(this.mContext);
        this.mMultiUserSwitch.setVisibility(showUserSwitcher() ? 0 : 4);
        SettingsButton settingsButton = this.mSettingsButton;
        if (!isDeviceInDemoMode) {
            i = 0;
        }
        settingsButton.setVisibility(i);
    }

    private boolean showUserSwitcher() {
        boolean z = true;
        boolean z2 = this.mExpanded && this.mMultiUserSwitch.isMultiUserEnabled();
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isKeyguardDone()) {
            z = false;
        }
        if (!z2 || !z || this.mMultiUserSwitch.hasMultipleUsers()) {
            return z2;
        }
        return false;
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mUserInfoController.addCallback(this);
        } else {
            this.mUserInfoController.removeCallback(this);
        }
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        if (this.mQsPanel != null) {
            this.mMultiUserSwitch.setQsPanel(qSPanel);
            this.mQsPanel.setFooterPageIndicator(this.mPageIndicator);
            if (OpUtils.isWLBAllowed(this.mContext)) {
                this.mWlbSwitch.setQsPanel(qSPanel);
            }
        }
    }

    public void onClick(View view) {
        if (view == this.mSettingsButton) {
            if (!this.mDeviceProvisionedController.isCurrentUserSetup()) {
                this.mActivityStarter.postQSRunnableDismissingKeyguard($$Lambda$QSFooterImpl$ORlOcuwnOcEc1bdhJcTagEFJfI4.INSTANCE);
                return;
            }
            MetricsLogger.action(this.mContext, this.mExpanded ? 406 : 490);
            OpMdmLogger.logQsPanel("click_settings");
            startSettingsActivity();
        }
    }

    private void startSettingsActivity() {
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        this.mIsGuestUser = UserManager.get(this.mContext).isGuestUser(ActivityManager.getCurrentUser());
        updateResources();
        updateEverything();
        this.mMultiUserAvatar.setImageDrawable(drawable);
    }

    private boolean canShowEditIcon() {
        return this.mShowEditIcon && !this.mIsGuestUser;
    }
}
