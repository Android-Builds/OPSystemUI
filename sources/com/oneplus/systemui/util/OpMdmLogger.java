package com.oneplus.systemui.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.Log;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.sarah.SarahClient;
import java.util.HashMap;
import java.util.Map;
import net.oneplus.odm.OpDeviceManagerInjector;

public class OpMdmLogger implements ConfigurationListener {
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.mdm.systemui", false);
    /* access modifiers changed from: private */
    public static Context mContext;
    private static boolean sAutomatic = false;
    private static String sCurOrien;
    private static Handler sHandler;
    private static HandlerThread sHandlerThread;
    private static boolean sIgnoreOnce = false;
    private static OpMdmLogger sInstance = null;
    private static boolean sNpvExpanded = false;
    private static HashMap<String, String> sQsEvent = new HashMap<>();
    private static boolean sQsExpanded = false;
    private static boolean sStatusBarPulled = false;
    private static Map<String, String> sTagMap = new HashMap();

    static {
        sTagMap.put("Tile.AirplaneModeTile", "quick_airplane");
        sTagMap.put("Tile.BatterySaverTile", "quick_battery");
        sTagMap.put("Tile.BluetoothTile", "quick_bt");
        sTagMap.put("Tile.CastTile", "quick_cast");
        sTagMap.put("Tile.CellularTile", "quick_mobile");
        sTagMap.put("Tile.ColorInversionTile", "quick_invert");
        sTagMap.put("Tile.DataSaverTile", "quick_ds");
        sTagMap.put("Tile.FlashlightTile", "quick_fl");
        sTagMap.put("Tile.GameModeTile", "quick_game");
        sTagMap.put("Tile.HotspotTile", "quick_hot");
        sTagMap.put("Tile.LocationTile", "quick_location");
        sTagMap.put("Tile.NfcTile", "quick_nfc");
        sTagMap.put("Tile.NightDisplayTile", "quick_night");
        sTagMap.put("Tile.ReadModeTile", "quick_read");
        sTagMap.put("Tile.RotationLockTile", "quick_ar");
        sTagMap.put("Tile.VPNTile", "quick_vpn");
        sTagMap.put("Tile.WifiTile", "quick_wifi");
        sTagMap.put("Tile.WorkModeTile", "quick_work");
        sTagMap.put("Tile.OtgTile", "quick_otg");
    }

    public static void init(Context context) {
        mContext = context;
        String str = "MdmLogger";
        if (sHandlerThread == null) {
            sHandlerThread = new HandlerThread(str, 10);
            sHandlerThread.start();
            sHandler = new Handler(sHandlerThread.getLooper());
        }
        initQsEvent();
        Log.d(str, "MdmLogger is initialized");
    }

    private static void initQsEvent() {
        sQsEvent.clear();
        String str = "0";
        sQsEvent.put("click_tile", str);
        sQsEvent.put("click_bright", str);
        sQsEvent.put("click_settings", str);
        sQsEvent.put("click_notif", str);
        sQsEvent.put("swipe_notif", str);
        sCurOrien = getOrientationEvent();
    }

    public static void log(String str, String str2, String str3) {
        log(str, str2, str3, "X9HFK50WT7");
    }

    public static void log(final String str, final String str2, final String str3, String str4) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("log:tag=");
            sb.append(str);
            sb.append(", label=");
            sb.append(str2);
            sb.append(", value=");
            sb.append(str3);
            sb.append(", appId=");
            sb.append(str4);
            Log.d("MdmLogger", sb.toString());
        }
        if ("lock_unlock_success".equals(str)) {
            Context context = mContext;
            if (context != null) {
                SarahClient.getInstance(context).notifyUnlock(str2);
            }
        }
        final HashMap hashMap = new HashMap();
        hashMap.put("appid", str4);
        sHandler.post(new Runnable() {
            public void run() {
                HashMap hashMap = new HashMap();
                hashMap.put(str2, str3);
                OpDeviceManagerInjector.getInstance().preserveAppData(OpMdmLogger.mContext, str, hashMap, hashMap);
            }
        });
    }

    public static void logQsTile(String str, String str2, String str3) {
        String str4 = (String) sTagMap.get(str);
        if (str4 != null) {
            log(str4, str2, str3);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cannot get tag from tileTag : ");
        sb.append(str);
        Log.e("MdmLogger", sb.toString());
    }

    public static void notifyNpvExpanded(boolean z) {
        if (sNpvExpanded != z) {
            if (z && sStatusBarPulled) {
                sStatusBarPulled = false;
                log("landscape_full_screen", "status_bar", "1");
            }
            handleQsUpdate(z);
            sNpvExpanded = z;
        }
    }

    public static void notifyQsExpanded(boolean z) {
        if (sQsExpanded != z && !sNpvExpanded) {
            handleQsUpdate(z);
            sQsExpanded = z;
        }
    }

    private static boolean isLandscape() {
        return Resources.getSystem().getConfiguration().orientation == 2;
    }

    private static String getOrientationEvent() {
        return isLandscape() ? "landscape_pull" : "portrait_pull";
    }

    private static void handleQsUpdate(boolean z) {
        if (z) {
            log(getOrientationEvent(), "pull_down", "1");
            initQsEvent();
            return;
        }
        reportQsEvents();
    }

    private static void reportQsEvents() {
        for (String str : sQsEvent.keySet()) {
            log(sCurOrien, str, (String) sQsEvent.get(str));
        }
    }

    public static void logQsPanel(String str) {
        if (isLandscape() && ("click_settings".equals(str) || "click_notif".equals(str))) {
            sIgnoreOnce = true;
        }
        if (sQsEvent.containsKey(str)) {
            sQsEvent.put(str, "1");
        }
    }

    public static void setStatusBarState(boolean z) {
        if (isLandscape()) {
            if (sStatusBarPulled && !z) {
                log("landscape_full_screen", "status_bar", "0");
            }
            sStatusBarPulled = z;
        }
    }

    public static void notifyBrightnessMode(boolean z) {
        sAutomatic = z;
    }

    public static void brightnessSliderClicked() {
        log("quick_bright", "manual", sAutomatic ? "1" : "0");
    }
}
