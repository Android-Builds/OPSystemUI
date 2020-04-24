package com.android.settingslib.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.KeyValueListParser;
import android.util.Slog;

public class BatterySaverUtils {

    private static class Parameters {
        public final int endNth;
        private final Context mContext;
        public final int startNth;

        public Parameters(Context context) {
            this.mContext = context;
            String string = Global.getString(this.mContext.getContentResolver(), "low_power_mode_suggestion_params");
            KeyValueListParser keyValueListParser = new KeyValueListParser(',');
            try {
                keyValueListParser.setString(string);
            } catch (IllegalArgumentException unused) {
                StringBuilder sb = new StringBuilder();
                sb.append("Bad constants: ");
                sb.append(string);
                Slog.wtf("BatterySaverUtils", sb.toString());
            }
            this.startNth = keyValueListParser.getInt("start_nth", 4);
            this.endNth = keyValueListParser.getInt("end_nth", 8);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0079, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized boolean setPowerSaveMode(android.content.Context r7, boolean r8, boolean r9) {
        /*
            java.lang.Class<com.android.settingslib.fuelgauge.BatterySaverUtils> r0 = com.android.settingslib.fuelgauge.BatterySaverUtils.class
            monitor-enter(r0)
            android.content.ContentResolver r1 = r7.getContentResolver()     // Catch:{ all -> 0x007c }
            android.os.Bundle r2 = new android.os.Bundle     // Catch:{ all -> 0x007c }
            r3 = 1
            r2.<init>(r3)     // Catch:{ all -> 0x007c }
            java.lang.String r4 = "extra_confirm_only"
            r5 = 0
            r2.putBoolean(r4, r5)     // Catch:{ all -> 0x007c }
            java.lang.String r4 = "com.android.settings"
            java.lang.String r6 = r7.getPackageName()     // Catch:{ all -> 0x007c }
            boolean r4 = r4.equals(r6)     // Catch:{ all -> 0x007c }
            if (r4 == 0) goto L_0x0028
            java.lang.String r4 = "extra_power_save_mode_caller"
            java.lang.String r6 = r7.getPackageName()     // Catch:{ all -> 0x007c }
            r2.putString(r4, r6)     // Catch:{ all -> 0x007c }
        L_0x0028:
            if (r8 == 0) goto L_0x0034
            if (r9 == 0) goto L_0x0034
            boolean r4 = maybeShowBatterySaverConfirmation(r7, r2)     // Catch:{ all -> 0x007c }
            if (r4 == 0) goto L_0x0034
            monitor-exit(r0)
            return r5
        L_0x0034:
            if (r8 == 0) goto L_0x003b
            if (r9 != 0) goto L_0x003b
            setBatterySaverConfirmationAcknowledged(r7)     // Catch:{ all -> 0x007c }
        L_0x003b:
            java.lang.Class<android.os.PowerManager> r9 = android.os.PowerManager.class
            java.lang.Object r9 = r7.getSystemService(r9)     // Catch:{ all -> 0x007c }
            android.os.PowerManager r9 = (android.os.PowerManager) r9     // Catch:{ all -> 0x007c }
            boolean r9 = r9.setPowerSaveModeEnabled(r8)     // Catch:{ all -> 0x007c }
            if (r9 == 0) goto L_0x007a
            if (r8 == 0) goto L_0x0078
            java.lang.String r8 = "low_power_manual_activation_count"
            int r8 = android.provider.Settings.Secure.getInt(r1, r8, r5)     // Catch:{ all -> 0x007c }
            int r8 = r8 + r3
            java.lang.String r9 = "low_power_manual_activation_count"
            android.provider.Settings.Secure.putInt(r1, r9, r8)     // Catch:{ all -> 0x007c }
            com.android.settingslib.fuelgauge.BatterySaverUtils$Parameters r9 = new com.android.settingslib.fuelgauge.BatterySaverUtils$Parameters     // Catch:{ all -> 0x007c }
            r9.<init>(r7)     // Catch:{ all -> 0x007c }
            int r4 = r9.startNth     // Catch:{ all -> 0x007c }
            if (r8 < r4) goto L_0x0078
            int r9 = r9.endNth     // Catch:{ all -> 0x007c }
            if (r8 > r9) goto L_0x0078
            java.lang.String r8 = "low_power_trigger_level"
            int r8 = android.provider.Settings.Global.getInt(r1, r8, r5)     // Catch:{ all -> 0x007c }
            if (r8 != 0) goto L_0x0078
            java.lang.String r8 = "suppress_auto_battery_saver_suggestion"
            int r8 = android.provider.Settings.Secure.getInt(r1, r8, r5)     // Catch:{ all -> 0x007c }
            if (r8 != 0) goto L_0x0078
            showAutoBatterySaverSuggestion(r7, r2)     // Catch:{ all -> 0x007c }
        L_0x0078:
            monitor-exit(r0)
            return r3
        L_0x007a:
            monitor-exit(r0)
            return r5
        L_0x007c:
            r7 = move-exception
            monitor-exit(r0)
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.fuelgauge.BatterySaverUtils.setPowerSaveMode(android.content.Context, boolean, boolean):boolean");
    }

    public static boolean maybeShowBatterySaverConfirmation(Context context, Bundle bundle) {
        if (Secure.getInt(context.getContentResolver(), "low_power_warning_acknowledged", 0) != 0) {
            return false;
        }
        context.sendBroadcast(getSystemUiBroadcast("PNW.startSaverConfirmation", bundle));
        return true;
    }

    private static void showAutoBatterySaverSuggestion(Context context, Bundle bundle) {
        context.sendBroadcast(getSystemUiBroadcast("PNW.autoSaverSuggestion", bundle));
    }

    private static Intent getSystemUiBroadcast(String str, Bundle bundle) {
        Intent intent = new Intent(str);
        intent.setFlags(268435456);
        intent.setPackage("com.android.systemui");
        intent.putExtras(bundle);
        return intent;
    }

    private static void setBatterySaverConfirmationAcknowledged(Context context) {
        Secure.putInt(context.getContentResolver(), "low_power_warning_acknowledged", 1);
    }

    public static void suppressAutoBatterySaver(Context context) {
        Secure.putInt(context.getContentResolver(), "suppress_auto_battery_saver_suggestion", 1);
    }
}
