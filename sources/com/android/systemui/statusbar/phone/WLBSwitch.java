package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.p007qs.QSPanel;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.statusbar.phone.WLBSwitchController.WLBSwitchCallbacks;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.ThemeColorUtils;

public class WLBSwitch extends FrameLayout implements OnClickListener, WLBSwitchCallbacks {
    /* access modifiers changed from: private */
    public static final String TAG = "WLBSwitch";
    private Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentUser;
    private FeatureDisableObserver mFeatureDisableObr;
    protected QSPanel mQsPanel;
    private SettingsObserver mSettingsObserver;
    private final int[] mTmpInt2 = new int[2];
    private UserSwitchReceiver mUserSwitchReceiver;
    private ImageView mWlbAvathar;
    /* access modifiers changed from: private */
    public WLBSwitchController mWlbSwitchController;

    public class FeatureDisableObserver extends ContentObserver {
        public FeatureDisableObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z) {
            WLBSwitch.this.handleFeature();
        }
    }

    public class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z, Uri uri) {
            WLBSwitch.this.readCurrentMode();
            WLBSwitch.this.mWlbSwitchController.onWLBModeChanged();
            WLBSwitch.this.updateSWitchToCurrentMode();
        }
    }

    public class UserSwitchReceiver extends BroadcastReceiver {
        public UserSwitchReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            UserManager userManager = (UserManager) context.getSystemService("user");
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
            UserInfo userInfo = userManager.getUserInfo(intExtra);
            WLBSwitch.this.mCurrentUser = intExtra;
            String access$500 = WLBSwitch.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onReceive: isAdmin user ");
            sb.append(userInfo.isAdmin());
            Log.d(access$500, sb.toString());
            if (userInfo.isAdmin()) {
                WLBSwitch.this.mWlbSwitchController.setAdminUser(true);
                WLBSwitch.this.readCurrentMode();
                WLBSwitch.this.setVisibility(0);
                if (WLBSwitch.this.mWlbSwitchController != null) {
                    WLBSwitch.this.updateSWitchToCurrentMode();
                    WLBSwitch.this.mWlbSwitchController.onWLBModeChanged();
                    return;
                }
                Log.d(WLBSwitch.TAG, "onReceive: Controller null");
                return;
            }
            WLBSwitch.this.setVisibility(8);
            WLBSwitch.this.mWlbSwitchController.setAdminUser(false);
            WLBSwitch.this.mWlbSwitchController.hideStatusBarIcon();
        }
    }

    public WLBSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        this.mCurrentUser = ActivityManager.getCurrentUser();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append(" finishInflate current User :");
        sb.append(this.mCurrentUser);
        Log.d(str, sb.toString());
        if (this.mCurrentUser != 0) {
            setVisibility(8);
        }
        setOnClickListener(this);
        this.mWlbAvathar = (ImageView) findViewById(R$id.wlb_avatar);
    }

    public void onClick(View view) {
        Log.d(TAG, "WLB switch clicked ");
        if (this.mWlbSwitchController != null) {
            String str = "qt_enter";
            OpMdmLogger.log(str, str, "1", "C22AG9UUDL");
            if (System.getInt(this.mContext.getContentResolver(), "oneplus_wlb_setup_done", 0) == 1) {
                View childAt = getChildCount() > 0 ? getChildAt(0) : this;
                this.mWlbSwitchController.setIsFullyExpanded(this.mQsPanel.isExpanded());
                childAt.getLocationInWindow(this.mTmpInt2);
                int[] iArr = this.mTmpInt2;
                iArr[0] = iArr[0] + (childAt.getWidth() / 2);
                int[] iArr2 = this.mTmpInt2;
                iArr2[1] = iArr2[1] + (childAt.getHeight() / 2);
                this.mQsPanel.showDetailAdapter(true, getUserDetailAdapter(), this.mTmpInt2);
                if (this.mWlbSwitchController.getCurrentMode() == 0) {
                    readCurrentMode();
                }
                this.mWlbSwitchController.doUnbindService();
                this.mWlbSwitchController.doBindService();
            } else {
                Intent intent = new Intent("com.oneplus.intent.ACTION_WELCOME_PAGE");
                intent.setPackage("com.oneplus.opwlb");
                ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
            }
        }
    }

    /* JADX WARNING: Can't wrap try/catch for region: R(9:0|1|(1:3)|4|5|(1:7)|(3:8|9|(1:11))|12|14) */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:4:0x000f */
    /* JADX WARNING: Missing exception handler attribute for start block: B:8:0x001e */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0022 A[Catch:{ Exception -> 0x0029 }] */
    /* JADX WARNING: Removed duplicated region for block: B:7:0x0013 A[Catch:{ Exception -> 0x001e }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onDetachedFromWindow() {
        /*
            r2 = this;
            com.android.systemui.statusbar.phone.WLBSwitch$SettingsObserver r0 = r2.mSettingsObserver     // Catch:{ Exception -> 0x000f }
            if (r0 == 0) goto L_0x000f
            android.content.Context r0 = r2.mContext     // Catch:{ Exception -> 0x000f }
            android.content.ContentResolver r0 = r0.getContentResolver()     // Catch:{ Exception -> 0x000f }
            com.android.systemui.statusbar.phone.WLBSwitch$SettingsObserver r1 = r2.mSettingsObserver     // Catch:{ Exception -> 0x000f }
            r0.unregisterContentObserver(r1)     // Catch:{ Exception -> 0x000f }
        L_0x000f:
            com.android.systemui.statusbar.phone.WLBSwitch$FeatureDisableObserver r0 = r2.mFeatureDisableObr     // Catch:{ Exception -> 0x001e }
            if (r0 == 0) goto L_0x001e
            android.content.Context r0 = r2.mContext     // Catch:{ Exception -> 0x001e }
            android.content.ContentResolver r0 = r0.getContentResolver()     // Catch:{ Exception -> 0x001e }
            com.android.systemui.statusbar.phone.WLBSwitch$FeatureDisableObserver r1 = r2.mFeatureDisableObr     // Catch:{ Exception -> 0x001e }
            r0.unregisterContentObserver(r1)     // Catch:{ Exception -> 0x001e }
        L_0x001e:
            com.android.systemui.statusbar.phone.WLBSwitch$UserSwitchReceiver r0 = r2.mUserSwitchReceiver     // Catch:{ Exception -> 0x0029 }
            if (r0 == 0) goto L_0x0029
            android.content.Context r0 = r2.mContext     // Catch:{ Exception -> 0x0029 }
            com.android.systemui.statusbar.phone.WLBSwitch$UserSwitchReceiver r1 = r2.mUserSwitchReceiver     // Catch:{ Exception -> 0x0029 }
            r0.unregisterReceiver(r1)     // Catch:{ Exception -> 0x0029 }
        L_0x0029:
            super.onDetachedFromWindow()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.WLBSwitch.onDetachedFromWindow():void");
    }

    public void setQsPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        setUserSwitcherController((WLBSwitchController) Dependency.get(WLBSwitchController.class));
    }

    public void setUserSwitcherController(WLBSwitchController wLBSwitchController) {
        this.mWlbSwitchController = wLBSwitchController;
        this.mWlbSwitchController.setQSPanel(this.mQsPanel);
        this.mWlbSwitchController.setSwitchCallbacks(this);
        if (this.mSettingsObserver == null) {
            this.mSettingsObserver = new SettingsObserver(new Handler());
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_wlb_activated_mode"), false, this.mSettingsObserver);
        }
        if (this.mFeatureDisableObr == null) {
            this.mFeatureDisableObr = new FeatureDisableObserver(new Handler());
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("worklife_feature_enable"), false, this.mFeatureDisableObr);
        }
        if (this.mUserSwitchReceiver == null) {
            this.mUserSwitchReceiver = new UserSwitchReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(this.mUserSwitchReceiver, intentFilter);
        }
    }

    /* access modifiers changed from: private */
    public void handleFeature() {
        int i = System.getInt(this.mContext.getContentResolver(), "worklife_feature_enable", 0);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("handleFeature : WLB app present:");
        sb.append(this.mCurrentUser);
        Log.d(str, sb.toString());
        if (this.mCurrentUser != 0 || i == 1 || i == 2) {
            setVisibility(8);
            this.mWlbSwitchController.setCurrentMode(0);
            this.mWlbSwitchController.onWLBModeChanged();
            return;
        }
        setVisibility(0);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = System.getInt(this.mContext.getContentResolver(), "worklife_feature_enable", 0);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onConfigurationChanged: user :");
        sb.append(this.mCurrentUser);
        sb.append(" featureEnable:");
        sb.append(i);
        sb.append(" newConfig:");
        sb.append(configuration);
        Log.d(str, sb.toString());
        if (this.mCurrentUser != 0 || i == 1 || i == 2) {
            setVisibility(8);
            return;
        }
        WLBSwitchController wLBSwitchController = this.mWlbSwitchController;
        if (wLBSwitchController != null) {
            wLBSwitchController.onWLBModeChanged();
            updateSWitchToCurrentMode();
        } else {
            Log.d(TAG, "onConfigurationChanged: Controller null");
        }
    }

    /* access modifiers changed from: private */
    public void readCurrentMode() {
        this.mWlbSwitchController.setCurrentMode(System.getInt(this.mContext.getContentResolver(), "oneplus_wlb_activated_mode", 0));
    }

    /* access modifiers changed from: private */
    public void updateSWitchToCurrentMode() {
        if (this.mWlbSwitchController.getCurrentMode() > 0) {
            this.mWlbAvathar.setImageTintList(ColorStateList.valueOf(ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT)));
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Current Mode changed to : ");
            sb.append(this.mWlbSwitchController.getCurrentMode());
            Log.d(str, sb.toString());
            return;
        }
        this.mWlbAvathar.setImageTintList(ColorStateList.valueOf(ThemeColorUtils.getColor(ThemeColorUtils.QS_BUTTON)));
    }

    /* access modifiers changed from: protected */
    public DetailAdapter getUserDetailAdapter() {
        return this.mWlbSwitchController.wlbDetailAdapter;
    }

    public void updateSwitchState() {
        updateSWitchToCurrentMode();
    }
}
