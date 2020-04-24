package com.android.systemui.p007qs.tiles;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.notification.EnableZenModeDialog;
import com.android.systemui.Prefs;
import com.android.systemui.R$drawable;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.SysUIToast;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.SlashState;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeController.Callback;
import com.android.systemui.volume.ZenModePanel;
import com.oneplus.util.ThemeColorUtils;

/* renamed from: com.android.systemui.qs.tiles.DndTile */
public class DndTile extends QSTileImpl<BooleanState> {
    /* access modifiers changed from: private */
    public static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");
    /* access modifiers changed from: private */
    public static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    /* access modifiers changed from: private */
    public final ActivityStarter mActivityStarter;
    /* access modifiers changed from: private */
    public final ZenModeController mController;
    /* access modifiers changed from: private */
    public final DndDetailAdapter mDetailAdapter;
    private boolean mListening;
    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            if ("DndTileCombinedIcon".equals(str) || "DndTileVisible".equals(str)) {
                DndTile.this.refreshState();
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            DndTile.setVisible(DndTile.this.mContext, intent.getBooleanExtra("visible", false));
            DndTile.this.refreshState();
        }
    };
    private boolean mReceiverRegistered;
    /* access modifiers changed from: private */
    public boolean mShowingDetail;
    private final Callback mZenCallback = new Callback() {
        public void onZenChanged(int i) {
            DndTile.this.refreshState(Integer.valueOf(i));
            if (DndTile.this.isShowingDetail()) {
                DndTile.this.mDetailAdapter.updatePanel();
            }
        }

        public void onConfigChanged(ZenModeConfig zenModeConfig) {
            if (DndTile.this.isShowingDetail()) {
                DndTile.this.mDetailAdapter.updatePanel();
            }
        }
    };
    /* access modifiers changed from: private */
    public final ZenModePanel.Callback mZenModePanelCallback = new ZenModePanel.Callback() {
        public void onExpanded(boolean z) {
        }

        public void onInteraction() {
        }

        public void onPrioritySettings() {
            DndTile.this.mActivityStarter.postStartActivityDismissingKeyguard(DndTile.ZEN_PRIORITY_SETTINGS, 0);
        }
    };

    /* renamed from: com.android.systemui.qs.tiles.DndTile$DndDetailAdapter */
    private final class DndDetailAdapter implements DetailAdapter, OnAttachStateChangeListener {
        private boolean mAuto;
        private ZenModePanel mZenPanel;

        public int getMetricsCategory() {
            return 149;
        }

        private DndDetailAdapter() {
        }

        public CharSequence getTitle() {
            return DndTile.this.mContext.getString(R$string.quick_settings_dnd_label);
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((BooleanState) DndTile.this.mState).value);
        }

        public Intent getSettingsIntent() {
            return DndTile.ZEN_SETTINGS;
        }

        public void setToggleState(boolean z) {
            MetricsLogger.action(DndTile.this.mContext, 166, z);
            if (!z) {
                DndTile.this.mController.setZen(0, null, DndTile.this.TAG);
                this.mAuto = false;
                return;
            }
            DndTile.this.mController.setZen(1, null, DndTile.this.TAG);
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            ZenModePanel zenModePanel;
            if (view != null) {
                zenModePanel = (ZenModePanel) view;
            } else {
                zenModePanel = (ZenModePanel) LayoutInflater.from(context).inflate(R$layout.zen_mode_panel, viewGroup, false);
            }
            this.mZenPanel = zenModePanel;
            if (view == null) {
                this.mZenPanel.init(DndTile.this.mController);
                this.mZenPanel.addOnAttachStateChangeListener(this);
                this.mZenPanel.setCallback(DndTile.this.mZenModePanelCallback);
                this.mZenPanel.setEmptyState(R$drawable.ic_qs_dnd_detail_empty, R$string.dnd_is_off);
            }
            updatePanel();
            return this.mZenPanel;
        }

        /* access modifiers changed from: private */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0046  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x007e  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0084  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void updatePanel() {
            /*
                r6 = this;
                com.android.systemui.volume.ZenModePanel r0 = r6.mZenPanel
                if (r0 != 0) goto L_0x0005
                return
            L_0x0005:
                r0 = 0
                r6.mAuto = r0
                com.android.systemui.qs.tiles.DndTile r1 = com.android.systemui.p007qs.tiles.DndTile.this
                com.android.systemui.statusbar.policy.ZenModeController r1 = r1.mController
                int r1 = r1.getZen()
                if (r1 != 0) goto L_0x001c
                com.android.systemui.volume.ZenModePanel r6 = r6.mZenPanel
                r0 = 2
                r6.setState(r0)
                goto L_0x0090
            L_0x001c:
                com.android.systemui.qs.tiles.DndTile r1 = com.android.systemui.p007qs.tiles.DndTile.this
                com.android.systemui.statusbar.policy.ZenModeController r1 = r1.mController
                android.service.notification.ZenModeConfig r1 = r1.getConfig()
                android.service.notification.ZenModeConfig$ZenRule r2 = r1.manualRule
                if (r2 == 0) goto L_0x0033
                java.lang.String r2 = r2.enabler
                if (r2 == 0) goto L_0x0033
                java.lang.String r2 = r6.getOwnerCaption(r2)
                goto L_0x0035
            L_0x0033:
                java.lang.String r2 = ""
            L_0x0035:
                android.util.ArrayMap r1 = r1.automaticRules
                java.util.Collection r1 = r1.values()
                java.util.Iterator r1 = r1.iterator()
            L_0x003f:
                boolean r3 = r1.hasNext()
                r4 = 1
                if (r3 == 0) goto L_0x0078
                java.lang.Object r3 = r1.next()
                android.service.notification.ZenModeConfig$ZenRule r3 = (android.service.notification.ZenModeConfig.ZenRule) r3
                boolean r5 = r3.isAutomaticActive()
                if (r5 == 0) goto L_0x003f
                boolean r2 = r2.isEmpty()
                if (r2 == 0) goto L_0x006b
                com.android.systemui.qs.tiles.DndTile r2 = com.android.systemui.p007qs.tiles.DndTile.this
                android.content.Context r2 = r2.mContext
                int r5 = com.android.systemui.R$string.qs_dnd_prompt_auto_rule
                java.lang.Object[] r4 = new java.lang.Object[r4]
                java.lang.String r3 = r3.name
                r4[r0] = r3
                java.lang.String r2 = r2.getString(r5, r4)
                goto L_0x003f
            L_0x006b:
                com.android.systemui.qs.tiles.DndTile r2 = com.android.systemui.p007qs.tiles.DndTile.this
                android.content.Context r2 = r2.mContext
                int r3 = com.android.systemui.R$string.qs_dnd_prompt_auto_rule_app
                java.lang.String r2 = r2.getString(r3)
                goto L_0x003f
            L_0x0078:
                boolean r1 = r2.isEmpty()
                if (r1 == 0) goto L_0x0084
                com.android.systemui.volume.ZenModePanel r6 = r6.mZenPanel
                r6.setState(r0)
                goto L_0x0090
            L_0x0084:
                r6.mAuto = r4
                com.android.systemui.volume.ZenModePanel r0 = r6.mZenPanel
                r0.setState(r4)
                com.android.systemui.volume.ZenModePanel r6 = r6.mZenPanel
                r6.setAutoText(r2)
            L_0x0090:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.tiles.DndTile.DndDetailAdapter.updatePanel():void");
        }

        private String getOwnerCaption(String str) {
            PackageManager packageManager = DndTile.this.mContext.getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
                if (applicationInfo != null) {
                    CharSequence loadLabel = applicationInfo.loadLabel(packageManager);
                    if (loadLabel != null) {
                        String trim = loadLabel.toString().trim();
                        return DndTile.this.mContext.getString(R$string.qs_dnd_prompt_app, new Object[]{trim});
                    }
                }
            } catch (Throwable th) {
                Slog.w(DndTile.this.TAG, "Error loading owner caption", th);
            }
            return "";
        }

        public void onViewAttachedToWindow(View view) {
            DndTile.this.mShowingDetail = true;
        }

        public void onViewDetachedFromWindow(View view) {
            DndTile.this.mShowingDetail = false;
            this.mZenPanel = null;
        }
    }

    public int getMetricsCategory() {
        return 118;
    }

    public DndTile(QSHost qSHost, ZenModeController zenModeController, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = zenModeController;
        this.mActivityStarter = activityStarter;
        this.mDetailAdapter = new DndDetailAdapter();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("com.android.systemui.dndtile.SET_VISIBLE"));
        this.mReceiverRegistered = true;
        this.mController.observe(getLifecycle(), this.mZenCallback);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
        if (this.mReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiverRegistered = false;
        }
    }

    public static void setVisible(Context context, boolean z) {
        Prefs.putBoolean(context, "DndTileVisible", z);
    }

    public static boolean isVisible(Context context) {
        return Prefs.getBoolean(context, "DndTileVisible", false);
    }

    public static void setCombinedIcon(Context context, boolean z) {
        Prefs.putBoolean(context, "DndTileCombinedIcon", z);
    }

    public static boolean isCombinedIcon(Context context) {
        return Prefs.getBoolean(context, "DndTileCombinedIcon", false);
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return ZEN_SETTINGS;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((BooleanState) this.mState).value) {
            this.mController.setZen(0, null, this.TAG);
        } else {
            showDetail(true);
        }
    }

    public void showDetail(boolean z) {
        int intForUser = Secure.getIntForUser(this.mContext.getContentResolver(), "zen_duration", 0, KeyguardUpdateMonitor.getCurrentUser());
        String str = "show_zen_upgrade_notification";
        if ((Secure.getInt(this.mContext.getContentResolver(), str, 0) == 0 || Secure.getInt(this.mContext.getContentResolver(), "zen_settings_updated", 0) == 1) ? false : true) {
            Secure.putInt(this.mContext.getContentResolver(), str, 0);
            this.mController.setZen(1, null, this.TAG);
            Intent intent = new Intent("android.settings.ZEN_MODE_ONBOARDING");
            intent.addFlags(268468224);
            this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        } else if (intForUser == -1) {
            this.mUiHandler.post(new Runnable() {
                public final void run() {
                    DndTile.this.lambda$showDetail$1$DndTile();
                }
            });
        } else if (intForUser != 0) {
            this.mController.setZen(1, ZenModeConfig.toTimeCondition(this.mContext, intForUser, ActivityManager.getCurrentUser(), true).id, this.TAG);
        } else {
            this.mController.setZen(1, null, this.TAG);
        }
    }

    public /* synthetic */ void lambda$showDetail$1$DndTile() {
        int i;
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            i = R$style.Oneplus_DND_Dialog_Dark;
        } else {
            i = R$style.Oneplus_DND_Dialog_Light;
        }
        this.mContext.setTheme(i);
        Dialog createDialog = new EnableZenModeDialog(this.mContext).createDialog();
        createDialog.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(createDialog, true);
        SystemUIDialog.registerDismissListener(createDialog);
        SystemUIDialog.setWindowOnTop(createDialog);
        this.mUiHandler.post(new Runnable(createDialog) {
            private final /* synthetic */ Dialog f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                DndTile.lambda$showDetail$0(this.f$0);
            }
        });
    }

    static /* synthetic */ void lambda$showDetail$0(Dialog dialog) {
        dialog.show();
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
            alertDialog.getButton(-1).setTextColor(color);
            alertDialog.getButton(-2).setTextColor(color);
        }
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        if (this.mController.isVolumeRestricted()) {
            this.mHost.collapsePanels();
            Context context = this.mContext;
            SysUIToast.makeText(context, (CharSequence) context.getString(17039915), 1).show();
            return;
        }
        if (!((BooleanState) this.mState).value) {
            this.mController.addCallback(new Callback() {
                public void onZenChanged(int i) {
                    DndTile.this.mController.removeCallback(this);
                    DndTile.this.showDetail(true);
                }
            });
            this.mController.setZen(1, null, this.TAG);
        } else {
            showDetail(true);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_dnd_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        ZenModeController zenModeController = this.mController;
        if (zenModeController != null) {
            int intValue = obj instanceof Integer ? ((Integer) obj).intValue() : zenModeController.getZen();
            boolean z = intValue != 0;
            boolean z2 = booleanState.value != z;
            if (booleanState.slash == null) {
                booleanState.slash = new SlashState();
            }
            booleanState.dualTarget = true;
            booleanState.value = z;
            booleanState.state = booleanState.value ? 2 : 1;
            booleanState.slash.isSlashed = !booleanState.value;
            booleanState.label = getTileLabel();
            booleanState.secondaryLabel = TextUtils.emptyIfNull(ZenModeConfig.getDescription(this.mContext, intValue != 0, this.mController.getConfig(), false));
            booleanState.icon = ResourceIcon.get(17302781);
            checkIfRestrictionEnforcedByAdminOnly(booleanState, "no_adjust_volume");
            String str = ", ";
            if (intValue == 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.mContext.getString(R$string.accessibility_quick_settings_dnd));
                sb.append(str);
                sb.append(booleanState.secondaryLabel);
                booleanState.contentDescription = sb.toString();
            } else if (intValue == 2) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(this.mContext.getString(R$string.accessibility_quick_settings_dnd));
                sb2.append(str);
                sb2.append(this.mContext.getString(R$string.accessibility_quick_settings_dnd_none_on));
                sb2.append(str);
                sb2.append(booleanState.secondaryLabel);
                booleanState.contentDescription = sb2.toString();
            } else if (intValue != 3) {
                booleanState.contentDescription = this.mContext.getString(R$string.accessibility_quick_settings_dnd);
            } else {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(this.mContext.getString(R$string.accessibility_quick_settings_dnd));
                sb3.append(str);
                sb3.append(this.mContext.getString(R$string.accessibility_quick_settings_dnd_alarms_on));
                sb3.append(str);
                sb3.append(booleanState.secondaryLabel);
                booleanState.contentDescription = sb3.toString();
            }
            if (z2) {
                fireToggleStateChanged(booleanState.value);
            }
            booleanState.dualLabelContentDescription = this.mContext.getResources().getString(R$string.accessibility_quick_settings_open_settings, new Object[]{getTileLabel()});
            booleanState.expandedAccessibilityClassName = Switch.class.getName();
        }
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_dnd_changed_on);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_dnd_changed_off);
    }

    public void handleSetListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            if (this.mListening) {
                Prefs.registerListener(this.mContext, this.mPrefListener);
            } else {
                Prefs.unregisterListener(this.mContext, this.mPrefListener);
            }
        }
    }

    public boolean isAvailable() {
        return isVisible(this.mContext);
    }
}
