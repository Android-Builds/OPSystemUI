package com.oneplus.systemui.statusbar.phone;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.text.Html;
import android.view.WindowManager.LayoutParams;
import com.android.systemui.Dependency;
import com.android.systemui.R$style;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.oneplus.lib.app.OPAlertDialog;
import com.oneplus.util.ThemeColorUtils;

public class OpSystemUIDialog extends OPAlertDialog {
    private final Context mContext;

    private static class DismissReceiver extends BroadcastReceiver implements OnDismissListener {
        private static final IntentFilter INTENT_FILTER = new IntentFilter();
        private final Dialog mDialog;
        private boolean mRegistered;

        static {
            INTENT_FILTER.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            INTENT_FILTER.addAction("android.intent.action.SCREEN_OFF");
        }

        DismissReceiver(Dialog dialog) {
            this.mDialog = dialog;
        }

        /* access modifiers changed from: 0000 */
        public void register() {
            this.mDialog.getContext().registerReceiverAsUser(this, UserHandle.CURRENT, INTENT_FILTER, null, null);
            this.mRegistered = true;
        }

        public void onReceive(Context context, Intent intent) {
            this.mDialog.dismiss();
        }

        public void onDismiss(DialogInterface dialogInterface) {
            if (this.mRegistered) {
                this.mDialog.getContext().unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }

    public OpSystemUIDialog(Context context) {
        int i;
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            i = R$style.Oneplus_Theme_Material_Dark_Dialog_Alert;
        } else {
            i = R$style.Oneplus_Theme_Material_Light_Dialog_Alert;
        }
        this(context, i);
    }

    public OpSystemUIDialog(Context context, int i) {
        super(context, i);
        this.mContext = context;
        applyFlags(this);
        LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attributes);
        registerDismissListener(this);
    }

    public void setTitle(int i) {
        setTitle((CharSequence) this.mContext.getString(i));
    }

    public void setTitle(CharSequence charSequence) {
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("<b><font color='#D8D8D8'>");
            sb.append(charSequence);
            sb.append("</font></b>");
            super.setTitle(Html.fromHtml(sb.toString()));
            return;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("<b>");
        sb2.append(charSequence);
        sb2.append("</b>");
        super.setTitle(Html.fromHtml(sb2.toString()));
    }

    public void setMessage(CharSequence charSequence) {
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("<font color='#8D8D8D'>");
            sb.append(charSequence);
            sb.append("</font>");
            super.setMessage(Html.fromHtml(sb.toString()));
            return;
        }
        super.setMessage(charSequence);
    }

    public void setButton(int i, CharSequence charSequence, OnClickListener onClickListener) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        sb.append(charSequence);
        sb.append("</b>");
        super.setButton(i, Html.fromHtml(sb.toString()), onClickListener);
    }

    public static void setWindowOnTop(Dialog dialog) {
        if (((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing()) {
            dialog.getWindow().setType(2014);
        } else {
            dialog.getWindow().setType(2017);
        }
    }

    public static OPAlertDialog applyFlags(OPAlertDialog oPAlertDialog) {
        oPAlertDialog.getWindow().setType(2014);
        oPAlertDialog.getWindow().addFlags(655360);
        return oPAlertDialog;
    }

    public static void registerDismissListener(Dialog dialog) {
        new DismissReceiver(dialog).register();
    }
}
