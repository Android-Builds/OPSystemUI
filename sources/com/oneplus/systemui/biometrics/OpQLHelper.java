package com.oneplus.systemui.biometrics;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.systemui.R$array;
import com.android.systemui.R$drawable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.phone.StatusBar;
import com.oneplus.systemui.biometrics.OpQLAdapter.ActionInfo;
import com.oneplus.systemui.biometrics.OpQLAdapter.OPQuickPayConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpQLHelper {
    final ArrayList<ActionInfo> mAppMap = new ArrayList<>();
    private Context mContext;
    private LauncherApps mLauncherApps;
    private PackageManager mPackageManager = null;
    private ArrayList<OPQuickPayConfig> mPaymentApps = new ArrayList<>();
    private ArrayList<String> mPaymentAppsName;
    private ArrayList<String> mWxMiniProgramAppsName;

    public OpQLHelper(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mWxMiniProgramAppsName = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R$array.op_wx_mini_program_strings)));
        this.mPaymentAppsName = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(84017198)));
        loadPaymentApps();
    }

    public ArrayList<ActionInfo> getQLApps() {
        return this.mAppMap;
    }

    public void parseQLConfig(String str) {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("parseQLConfig config ");
            sb.append(str);
            Log.d("QuickLaunch.QLHelper", sb.toString());
        }
        if (str != null) {
            String[] split = str.split(",");
            for (String parseSettingData : split) {
                ActionInfo parseSettingData2 = parseSettingData(parseSettingData);
                if (parseSettingData2 != null) {
                    this.mAppMap.add(0, parseSettingData2);
                }
            }
        }
    }

    private ActionInfo parseSettingData(String str) {
        String str2;
        if (str == null) {
            return null;
        }
        ActionInfo actionInfo = new ActionInfo();
        int indexOf = str.indexOf(":");
        if (indexOf < 0) {
            actionInfo.setActionName(str);
        } else {
            String[] strArr = new String[4];
            strArr[0] = str.substring(0, indexOf);
            String[] split = str.substring(indexOf + 1).split(";", 3);
            System.arraycopy(split, 0, strArr, 1, split.length);
            actionInfo.setActionName(strArr[0]);
            actionInfo.setPackage(strArr[1]);
            if (!isPackageAvailable(actionInfo.mPackageName)) {
                return null;
            }
            if ("OpenApp".equals(strArr[0])) {
                actionInfo.setUid(strArr[2]);
                actionInfo.mAppIcon = getApplicationIcon(actionInfo.mPackageName, actionInfo.mUid);
                actionInfo.mLabel = getAppLabel(actionInfo.mPackageName);
            } else {
                if ("OpenShortcut".equals(strArr[0])) {
                    actionInfo.setShortcutId(strArr[2]);
                    actionInfo.setUid(strArr[3]);
                    List loadShortCuts = loadShortCuts(actionInfo.mPackageName);
                    for (int i = 0; i < loadShortCuts.size(); i++) {
                        ShortcutInfo shortcutInfo = (ShortcutInfo) loadShortCuts.get(i);
                        if (shortcutInfo.getId().equals(actionInfo.mShortcutId)) {
                            actionInfo.mShortcutIcon = shortcutInfo.getIconResourceId();
                            actionInfo.mAppIcon = this.mLauncherApps.getShortcutIconDrawable(shortcutInfo, actionInfo.mUid);
                            if (actionInfo.mAppIcon == null) {
                                actionInfo.mAppIcon = getApplicationIcon(actionInfo.mPackageName, actionInfo.mUid);
                            }
                            CharSequence longLabel = shortcutInfo.getLongLabel();
                            if (TextUtils.isEmpty(longLabel)) {
                                longLabel = shortcutInfo.getShortLabel();
                            }
                            if (TextUtils.isEmpty(longLabel)) {
                                longLabel = shortcutInfo.getId();
                            }
                            actionInfo.mLabel = longLabel.toString();
                        }
                    }
                } else {
                    if (!"OpenQuickPay".equals(strArr[0])) {
                        if ("OpenWxMiniProgram".equals(strArr[0])) {
                            int parseInt = Integer.parseInt(strArr[2]);
                            if (isPackageAvailable(actionInfo.mPackageName)) {
                                actionInfo.mWxMiniProgramWhich = parseInt;
                                actionInfo.mAppIcon = getWxMiniProgramApplicationIcon(parseInt);
                                actionInfo.mLabel = (String) this.mWxMiniProgramAppsName.get(parseInt);
                            }
                        }
                    } else if (isPackageAvailable(actionInfo.mPackageName)) {
                        actionInfo.mPaymentWhich = Integer.parseInt(strArr[2]);
                        if (actionInfo.mPaymentWhich < this.mPaymentApps.size()) {
                            actionInfo.mQuickPayConfig = (OPQuickPayConfig) this.mPaymentApps.get(actionInfo.mPaymentWhich);
                            actionInfo.mAppIcon = actionInfo.mQuickPayConfig.appIcon;
                        }
                        boolean equals = this.mContext.getResources().getConfiguration().locale.getLanguage().equals("zh");
                        int i2 = actionInfo.mPaymentWhich;
                        if (i2 == 4) {
                            str2 = (String) this.mPaymentAppsName.get(i2);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append(getAppLabel(actionInfo.mPackageName));
                            sb.append((equals || actionInfo.mPaymentWhich == 4) ? "" : " ");
                            sb.append((String) this.mPaymentAppsName.get(actionInfo.mPaymentWhich));
                            str2 = sb.toString();
                        }
                        actionInfo.mLabel = str2;
                    }
                }
            }
        }
        return actionInfo;
    }

    private List<ShortcutInfo> loadShortCuts(String str) {
        if (this.mLauncherApps == null) {
            this.mLauncherApps = (LauncherApps) this.mContext.getSystemService("launcherapps");
        }
        if (this.mLauncherApps == null) {
            return null;
        }
        ShortcutQuery shortcutQuery = new ShortcutQuery();
        shortcutQuery.setQueryFlags(11);
        shortcutQuery.setPackage(str);
        return this.mLauncherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
    }

    private boolean isPackageAvailable(String str) {
        String str2 = "QuickLaunch.QLHelper";
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 0);
            if (applicationInfo != null && applicationInfo.enabled) {
                return true;
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception e = ");
            sb.append(e.toString());
            Log.w(str2, sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("QuickPay: ");
        sb2.append(str);
        sb2.append(" is not available.");
        Log.w(str2, sb2.toString());
        return false;
    }

    private Drawable getApplicationIcon(String str, int i) {
        ApplicationInfo applicationInfo;
        Resources resources;
        String str2 = "Exception e = ";
        String str3 = "QuickLaunch.QLHelper";
        if (i >= 0) {
            try {
                ApplicationInfo applicationInfo2 = this.mPackageManager.getApplicationInfo(str, 128);
                this.mContext.getResources();
                return this.mPackageManager.getUserBadgedIcon(this.mPackageManager.getApplicationIcon(applicationInfo2), UserHandle.getUserHandleForUid(i));
            } catch (NameNotFoundException unused) {
                StringBuilder sb = new StringBuilder();
                sb.append("Package [");
                sb.append(str);
                sb.append("] name not found");
                Log.e(str3, sb.toString());
                return null;
            }
        } else {
            try {
                applicationInfo = this.mPackageManager.getApplicationInfo(str, 0);
            } catch (Exception e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str2);
                sb2.append(e.toString());
                Log.w(str3, sb2.toString());
                applicationInfo = null;
            }
            try {
                resources = this.mPackageManager.getResourcesForApplication(str);
            } catch (NameNotFoundException unused2) {
                resources = null;
            }
            if (!(resources == null || applicationInfo == null)) {
                int i2 = applicationInfo.icon;
                if (i2 != 0) {
                    try {
                        return getDrawable(resources, i2);
                    } catch (Exception e2) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append(str2);
                        sb3.append(e2.toString());
                        Log.w(str3, sb3.toString());
                    }
                }
            }
            return null;
        }
    }

    private String getAppLabel(String str) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = this.mPackageManager.getApplicationInfo(str, 128);
        } catch (NameNotFoundException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception e = ");
            sb.append(e.toString());
            Log.w("QuickLaunch.QLHelper", sb.toString());
            applicationInfo = null;
        }
        return (String) (applicationInfo != null ? this.mPackageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    private void loadPaymentApps() {
        ArrayList arrayList = new ArrayList(Arrays.asList(this.mContext.getResources().getStringArray(84017197)));
        for (int i = 0; i < arrayList.size(); i++) {
            String[] split = ((String) arrayList.get(i)).split(";");
            if (split.length >= 4) {
                OPQuickPayConfig oPQuickPayConfig = new OPQuickPayConfig();
                oPQuickPayConfig.index = i;
                oPQuickPayConfig.packageName = split[0];
                oPQuickPayConfig.switchName = (String) this.mPaymentAppsName.get(i);
                if (split[1].equals("sdk")) {
                    oPQuickPayConfig.isSDKstart = true;
                } else if (split[1].contains("://")) {
                    oPQuickPayConfig.urlScheme = split[1];
                } else {
                    oPQuickPayConfig.className = split[1];
                    StringBuilder sb = new StringBuilder();
                    sb.append(oPQuickPayConfig.packageName);
                    sb.append("/");
                    sb.append(oPQuickPayConfig.className);
                    oPQuickPayConfig.targetClassName = sb.toString();
                }
                if ("default".equals(split[2])) {
                    oPQuickPayConfig.isDefault = true;
                }
                if (!"class".equals(split[3])) {
                    oPQuickPayConfig.targetClassName = split[3];
                }
                Drawable paymentApplicationIcon = getPaymentApplicationIcon(i);
                if (paymentApplicationIcon != null) {
                    int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(84279509);
                    paymentApplicationIcon.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
                    oPQuickPayConfig.appIcon = paymentApplicationIcon;
                } else {
                    oPQuickPayConfig.appIcon = getApplicationIcon(oPQuickPayConfig.packageName, -1);
                }
                this.mPaymentApps.add(oPQuickPayConfig);
            }
        }
    }

    public boolean startApp(String str, ActivityOptions activityOptions, int i) {
        Intent launchIntentForPackage = this.mPackageManager.getLaunchIntentForPackage(str);
        if (launchIntentForPackage != null) {
            this.mContext.startActivityAsUser(launchIntentForPackage, activityOptions.toBundle(), new UserHandle(UserHandle.getUserId(i)));
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("start app ");
        sb.append(str);
        sb.append(" failed because intent is null");
        Log.e("QuickLaunch.QLHelper", sb.toString());
        return false;
    }

    public void startQuickPay(int i, int i2) {
        String str = "QuickLaunch.QLHelper";
        String str2 = "com.eg.android.AlipayGphone";
        boolean z = i == 2 ? startShortcut(str2, "1002", i2, false) : i == 3 ? startShortcut(str2, "1001", i2, false) : false;
        if (!z) {
            try {
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("QuickPay: startQuickPay which=");
                    sb.append(i);
                    Log.d(str, sb.toString());
                }
                boolean isPackageAvailable = isPackageAvailable(((OPQuickPayConfig) this.mPaymentApps.get(i)).packageName);
                if (!isPackageAvailable) {
                    int i3 = 0;
                    while (true) {
                        if (i3 >= this.mPaymentApps.size()) {
                            break;
                        }
                        if (i != i3) {
                            OPQuickPayConfig oPQuickPayConfig = (OPQuickPayConfig) this.mPaymentApps.get(i3);
                            if (oPQuickPayConfig.isDefault && isPackageAvailable(oPQuickPayConfig.packageName)) {
                                if (Build.DEBUG_ONEPLUS) {
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("QuickPay: startQuickPay new which=");
                                    sb2.append(i3);
                                    Log.i(str, sb2.toString());
                                }
                                isPackageAvailable = true;
                                i = i3;
                            }
                        }
                        i3++;
                    }
                }
                if (isPackageAvailable) {
                    OPQuickPayConfig oPQuickPayConfig2 = (OPQuickPayConfig) this.mPaymentApps.get(i);
                    if (oPQuickPayConfig2.className != null) {
                        Intent intent = new Intent();
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("next.packageName ");
                        sb3.append(oPQuickPayConfig2.packageName);
                        sb3.append(" next.className ");
                        sb3.append(oPQuickPayConfig2.className);
                        Log.d(str, sb3.toString());
                        intent.setClassName(oPQuickPayConfig2.packageName, oPQuickPayConfig2.className);
                        intent.addFlags(268468224);
                        this.mContext.startActivityAsUser(intent, UserHandle.SYSTEM);
                    } else if (oPQuickPayConfig2.urlScheme != null) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("next.urlScheme ");
                        sb4.append(oPQuickPayConfig2.urlScheme);
                        Log.d(str, sb4.toString());
                        Intent intent2 = new Intent("android.intent.action.VIEW", Uri.parse(oPQuickPayConfig2.urlScheme));
                        intent2.addFlags(335544320);
                        this.mContext.startActivityAsUser(intent2, UserHandle.SYSTEM);
                    }
                } else {
                    Toast.makeText(this.mContext, 84869400, 0).show();
                    Log.w(str, "QuickPay: startQuickPay have no installed app.");
                }
            } catch (Exception e) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("QuickPay: startQuickPay failed ");
                sb5.append(e);
                Log.w(str, sb5.toString());
            }
        }
    }

    public void startWxMiniProgram(int i) {
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        if (statusBar != null) {
            statusBar.toggleWxBus();
        }
    }

    public boolean startShortcut(String str, String str2, int i, boolean z) {
        if (this.mLauncherApps == null) {
            this.mLauncherApps = (LauncherApps) this.mContext.getSystemService("launcherapps");
        }
        LauncherApps launcherApps = this.mLauncherApps;
        String str3 = "QuickLaunch.QLHelper";
        if (launcherApps != null) {
            try {
                launcherApps.startShortcut(str, str2, null, null, new UserHandle(UserHandle.getUserId(i)));
                return true;
            } catch (ActivityNotFoundException | IllegalStateException | SecurityException unused) {
                Log.e(str3, "start shortcut failed");
                return false;
            }
        } else {
            Log.e(str3, "shortcut service is null");
            return false;
        }
    }

    private Drawable getPaymentApplicationIcon(int i) {
        if (i == 0) {
            return getDrawable(null, R$drawable.ic_wechat_qrcode);
        }
        if (i == 1) {
            return getDrawable(null, R$drawable.ic_wechat_scanning);
        }
        if (i == 2) {
            return getDrawable(null, R$drawable.ic_alipay_qrcode);
        }
        if (i != 3) {
            return null;
        }
        return getDrawable(null, R$drawable.ic_alipay_scanning);
    }

    private Drawable getWxMiniProgramApplicationIcon(int i) {
        if (i != 0) {
            return null;
        }
        return getDrawable(null, R$drawable.ic_wechat_mini_program_bus);
    }

    private Drawable getDrawable(Resources resources, int i) {
        if (resources != null) {
            return resources.getDrawableForDensity(i, 640);
        }
        return this.mContext.getResources().getDrawableForDensity(i, 640);
    }

    public void resolveQuickPayConfigFromJSON(JSONArray jSONArray) {
        String str = "[OnlineConfig] QuickPayConfigUpdater, error message:";
        String str2 = "QuickLaunch.QLHelper";
        String str3 = "name";
        if (jSONArray != null) {
            int i = 0;
            while (i < jSONArray.length()) {
                try {
                    JSONObject jSONObject = jSONArray.getJSONObject(i);
                    if (jSONObject.getString(str3).equals("op_quick_pay_wechat_qrcode_config")) {
                        updateQuickPayIfNeed(0, jSONObject);
                    } else if (jSONObject.getString(str3).equals("op_quick_pay_wechat_scanning_config")) {
                        updateQuickPayIfNeed(1, jSONObject);
                    } else if (jSONObject.getString(str3).equals("op_quick_pay_alipay_qrcode_config")) {
                        updateQuickPayIfNeed(2, jSONObject);
                    } else if (jSONObject.getString(str3).equals("op_quick_pay_alipay_scanning_config")) {
                        updateQuickPayIfNeed(3, jSONObject);
                    } else if (jSONObject.getString(str3).equals("op_quick_pay_paytm_config")) {
                        updateQuickPayIfNeed(4, jSONObject);
                    }
                    i++;
                } catch (JSONException e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str);
                    sb.append(e.getMessage());
                    Log.e(str2, sb.toString());
                } catch (Exception e2) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str);
                    sb2.append(e2.getMessage());
                    Log.e(str2, sb2.toString());
                }
            }
            Log.v(str2, "[OnlineConfig] QuickPayConfigUpdater updated complete");
        }
    }

    /* access modifiers changed from: 0000 */
    public void updateQuickPayIfNeed(int i, JSONObject jSONObject) throws JSONException {
        JSONArray jSONArray = jSONObject.getJSONArray("value");
        for (int i2 = 0; i2 < jSONArray.length(); i2++) {
            String string = jSONArray.getString(i2);
            String[] split = string.split(";");
            if (split.length >= 5 && this.mPaymentApps.size() >= 5 && isNewConfig(((OPQuickPayConfig) this.mPaymentApps.get(i)).packageName, split[4])) {
                OPQuickPayConfig oPQuickPayConfig = new OPQuickPayConfig();
                oPQuickPayConfig.packageName = split[0];
                if (split[1].equals("sdk")) {
                    oPQuickPayConfig.isSDKstart = true;
                } else if (split[1].contains("://")) {
                    oPQuickPayConfig.urlScheme = split[1];
                } else {
                    oPQuickPayConfig.className = split[1];
                    StringBuilder sb = new StringBuilder();
                    sb.append(oPQuickPayConfig.packageName);
                    sb.append("/");
                    sb.append(oPQuickPayConfig.className);
                    oPQuickPayConfig.targetClassName = sb.toString();
                }
                if ("default".equals(split[2])) {
                    oPQuickPayConfig.isDefault = true;
                }
                if (!"class".equals(split[3])) {
                    oPQuickPayConfig.targetClassName = split[3];
                }
                this.mPaymentApps.set(i, oPQuickPayConfig);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("QuickPay: update ");
                sb2.append(i);
                sb2.append(" to ");
                sb2.append(string);
                Log.v("QuickLaunch.QLHelper", sb2.toString());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isNewConfig(String str, String str2) {
        String str3 = "\\.";
        if (isPackageAvailable(str) && str2 != "") {
            try {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 0);
                if (packageInfo != null) {
                    String[] split = packageInfo.versionName.split(str3);
                    String[] split2 = str2.split(str3);
                    int max = Math.max(split.length, split2.length);
                    int i = 0;
                    while (i < max) {
                        int parseInt = i < split.length ? Integer.parseInt(split[i]) : 0;
                        int parseInt2 = i < split2.length ? Integer.parseInt(split2[i]) : 0;
                        if (parseInt < parseInt2) {
                            return true;
                        }
                        if (parseInt > parseInt2) {
                            return false;
                        }
                        i++;
                    }
                }
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Exception e = ");
                sb.append(e.toString());
                Log.w("QuickLaunch.QLHelper", sb.toString());
            }
        }
        return false;
    }
}
