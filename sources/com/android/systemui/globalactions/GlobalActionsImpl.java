package com.android.systemui.globalactions;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.colorextraction.drawable.ScrimDrawable;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$style;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.plugins.GlobalActions.GlobalActionsManager;
import com.android.systemui.plugins.GlobalActionsPanelPlugin;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionController.Extension;
import com.android.systemui.statusbar.policy.ExtensionController.ExtensionBuilder;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class GlobalActionsImpl implements GlobalActions, Callbacks {
    private final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private boolean mDisabled;
    private GlobalActionsDialog mGlobalActions;
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    private final Extension<GlobalActionsPanelPlugin> mPanelExtension;

    public GlobalActionsImpl(Context context) {
        this.mContext = context;
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).addCallback((Callbacks) this);
        ExtensionBuilder newExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(GlobalActionsPanelPlugin.class);
        newExtension.withPlugin(GlobalActionsPanelPlugin.class);
        this.mPanelExtension = newExtension.build();
    }

    public void destroy() {
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).removeCallback((Callbacks) this);
        GlobalActionsDialog globalActionsDialog = this.mGlobalActions;
        if (globalActionsDialog != null) {
            globalActionsDialog.destroy();
            this.mGlobalActions = null;
        }
    }

    public void showGlobalActions(GlobalActionsManager globalActionsManager) {
        if (!this.mDisabled) {
            if (this.mGlobalActions == null) {
                this.mGlobalActions = new GlobalActionsDialog(this.mContext, globalActionsManager);
            }
            this.mGlobalActions.showDialog(this.mKeyguardMonitor.isShowing(), this.mDeviceProvisionedController.isDeviceProvisioned(), (GlobalActionsPanelPlugin) this.mPanelExtension.get());
        }
    }

    public void showShutdownUi(boolean z, String str) {
        KeyguardUpdateMonitor.getInstance(this.mContext).notifyShutDownOrReboot();
        if (!hasCustomizedShutdownAnim()) {
            ScrimDrawable scrimDrawable = new ScrimDrawable();
            scrimDrawable.setAlpha(255);
            Dialog dialog = new Dialog(this.mContext, R$style.Theme_SystemUI_Dialog_GlobalActions);
            Window window = dialog.getWindow();
            window.requestFeature(1);
            LayoutParams attributes = window.getAttributes();
            attributes.systemUiVisibility |= 1792;
            window.getDecorView();
            window.getAttributes().width = -1;
            window.getAttributes().height = -1;
            window.getAttributes().layoutInDisplayCutoutMode = 1;
            window.setType(2020);
            window.clearFlags(2);
            window.addFlags(17629472);
            window.setBackgroundDrawable(scrimDrawable);
            window.setWindowAnimations(16973828);
            dialog.setContentView(17367289);
            dialog.setCancelable(false);
            ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isKeyguardLocked();
            ProgressBar progressBar = (ProgressBar) dialog.findViewById(16908301);
            if (progressBar != null) {
                progressBar.getIndeterminateDrawable().setTint(-1);
            }
            TextView textView = (TextView) dialog.findViewById(16908308);
            if (textView != null) {
                textView.setTextColor(-1);
                if (z) {
                    textView.setText(17040912);
                }
            }
            scrimDrawable.setColor(-16777216, false);
            dialog.show();
        }
    }

    public void disable(int i, int i2, int i3, boolean z) {
        boolean z2 = (i3 & 8) != 0;
        if (i == this.mContext.getDisplayId() && z2 != this.mDisabled) {
            this.mDisabled = z2;
            if (z2) {
                GlobalActionsDialog globalActionsDialog = this.mGlobalActions;
                if (globalActionsDialog != null) {
                    globalActionsDialog.dismissDialog();
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:3:0x0038, code lost:
        if (android.util.OpFeatures.isSupport(new int[]{215}) != false) goto L_0x003a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasCustomizedShutdownAnim() {
        /*
            r6 = this;
            r0 = 1
            int[] r1 = new int[r0]
            r2 = 0
            r3 = 82
            r1[r2] = r3
            boolean r1 = android.util.OpFeatures.isSupport(r1)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "hasCusShutdownAnim="
            r3.append(r4)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            java.lang.String r4 = "GlobalActionsImpl"
            android.util.Log.d(r4, r3)
            int[] r3 = new int[r0]
            r5 = 157(0x9d, float:2.2E-43)
            r3[r2] = r5
            boolean r3 = android.util.OpFeatures.isSupport(r3)
            if (r3 != 0) goto L_0x003a
            int[] r3 = new int[r0]
            r5 = 215(0xd7, float:3.01E-43)
            r3[r2] = r5
            boolean r3 = android.util.OpFeatures.isSupport(r3)
            if (r3 == 0) goto L_0x0061
        L_0x003a:
            if (r1 == 0) goto L_0x0061
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "show CRA enableShut="
            r1.append(r3)
            android.content.Context r3 = r6.mContext
            boolean r3 = com.oneplus.util.OpUtils.isEnableCustShutdownAnim(r3)
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r4, r1)
            android.content.Context r6 = r6.mContext
            boolean r6 = com.oneplus.util.OpUtils.isEnableCustShutdownAnim(r6)
            if (r6 == 0) goto L_0x005f
            goto L_0x0062
        L_0x005f:
            r0 = r2
            goto L_0x0062
        L_0x0061:
            r0 = r1
        L_0x0062:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.globalactions.GlobalActionsImpl.hasCustomizedShutdownAnim():boolean");
    }
}
