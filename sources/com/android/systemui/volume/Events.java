package com.android.systemui.volume;

import com.android.systemui.plugins.VolumeDialogController.State;

public class Events {
    public static final String[] DISMISS_REASONS = {"unknown", "touch_outside", "volume_controller", "timeout", "screen_off", "settings_clicked", "done_clicked", "a11y_stream_changed", "output_chooser", "usb_temperature_below_threshold", "still_showing_after_dismissed"};
    private static final String[] EVENT_TAGS = {"show_dialog", "dismiss_dialog", "active_stream_changed", "expand", "key", "collection_started", "collection_stopped", "icon_click", "settings_click", "touch_level_changed", "level_changed", "internal_ringer_mode_changed", "external_ringer_mode_changed", "zen_mode_changed", "suppressor_changed", "mute_changed", "touch_level_done", "zen_mode_config_changed", "ringer_toggle", "show_usb_overheat_alarm", "dismiss_usb_overheat_alarm", "odi_captions_click", "odi_captions_tooltip_click", "status_bar_icon_changed"};
    public static final String[] SHOW_REASONS = {"unknown", "volume_changed", "remote_volume_changed", "usb_temperature_above_threshold", "output_chooser_back"};
    private static final String TAG = Util.logTag(Events.class);
    public static Callback sCallback;

    public interface Callback {
        void writeEvent(long j, int i, Object[] objArr);

        void writeState(long j, State state);
    }

    private static String ringerModeToString(int i) {
        return i != 0 ? i != 1 ? i != 2 ? "unknown" : "normal" : "vibrate" : "silent";
    }

    private static String zenModeToString(int i) {
        return i != 0 ? i != 1 ? i != 2 ? i != 3 ? "unknown" : "alarms" : "no_interruptions" : "important_interruptions" : "off";
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x00db, code lost:
        r3.append(ringerModeToString(r12[0].intValue()));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x00ec, code lost:
        r3.append(android.media.AudioSystem.streamToString(r12[0].intValue()));
        r3.append(' ');
        r3.append(r12[1]);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void writeEvent(android.content.Context r10, int r11, java.lang.Object... r12) {
        /*
            com.android.internal.logging.MetricsLogger r0 = new com.android.internal.logging.MetricsLogger
            r0.<init>()
            long r1 = java.lang.System.currentTimeMillis()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r4 = "writeEvent "
            r3.<init>(r4)
            java.lang.String[] r4 = EVENT_TAGS
            r4 = r4[r11]
            r3.append(r4)
            if (r12 == 0) goto L_0x01cc
            int r4 = r12.length
            if (r4 <= 0) goto L_0x01cc
            java.lang.String r4 = " "
            r3.append(r4)
            r4 = 1457(0x5b1, float:2.042E-42)
            r5 = 207(0xcf, float:2.9E-43)
            java.lang.String r6 = " keyguard="
            r7 = 32
            r8 = 1
            r9 = 0
            switch(r11) {
                case 0: goto L_0x01a4;
                case 1: goto L_0x0191;
                case 2: goto L_0x0174;
                case 3: goto L_0x0161;
                case 4: goto L_0x013c;
                case 5: goto L_0x002f;
                case 6: goto L_0x002f;
                case 7: goto L_0x010c;
                case 8: goto L_0x0105;
                case 9: goto L_0x00ec;
                case 10: goto L_0x00ec;
                case 11: goto L_0x00db;
                case 12: goto L_0x00ce;
                case 13: goto L_0x00bd;
                case 14: goto L_0x00ae;
                case 15: goto L_0x00ec;
                case 16: goto L_0x00a0;
                case 17: goto L_0x002f;
                case 18: goto L_0x0091;
                case 19: goto L_0x0068;
                case 20: goto L_0x003f;
                case 21: goto L_0x002f;
                case 22: goto L_0x002f;
                case 23: goto L_0x0038;
                default: goto L_0x002f;
            }
        L_0x002f:
            java.util.List r10 = java.util.Arrays.asList(r12)
            r3.append(r10)
            goto L_0x01cc
        L_0x0038:
            r10 = r12[r9]
            r3.append(r10)
            goto L_0x01cc
        L_0x003f:
            com.android.internal.logging.MetricsLogger.hidden(r10, r4)
            r0 = r12[r8]
            java.lang.Boolean r0 = (java.lang.Boolean) r0
            boolean r0 = r0.booleanValue()
            java.lang.String r4 = "dismiss_usb_overheat_alarm"
            com.android.internal.logging.MetricsLogger.histogram(r10, r4, r0)
            java.lang.String[] r10 = DISMISS_REASONS
            r0 = r12[r9]
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r10 = r10[r0]
            r3.append(r10)
            r3.append(r6)
            r10 = r12[r8]
            r3.append(r10)
            goto L_0x01cc
        L_0x0068:
            com.android.internal.logging.MetricsLogger.visible(r10, r4)
            r0 = r12[r8]
            java.lang.Boolean r0 = (java.lang.Boolean) r0
            boolean r0 = r0.booleanValue()
            java.lang.String r4 = "show_usb_overheat_alarm"
            com.android.internal.logging.MetricsLogger.histogram(r10, r4, r0)
            java.lang.String[] r10 = SHOW_REASONS
            r0 = r12[r9]
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r10 = r10[r0]
            r3.append(r10)
            r3.append(r6)
            r10 = r12[r8]
            r3.append(r10)
            goto L_0x01cc
        L_0x0091:
            r10 = 1385(0x569, float:1.941E-42)
            r4 = r12[r9]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r0.action(r10, r4)
            goto L_0x01cc
        L_0x00a0:
            r0 = 209(0xd1, float:2.93E-43)
            r4 = r12[r8]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r10, r0, r4)
            goto L_0x00ec
        L_0x00ae:
            r10 = r12[r9]
            r3.append(r10)
            r3.append(r7)
            r10 = r12[r8]
            r3.append(r10)
            goto L_0x01cc
        L_0x00bd:
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = zenModeToString(r10)
            r3.append(r10)
            goto L_0x01cc
        L_0x00ce:
            r0 = 213(0xd5, float:2.98E-43)
            r4 = r12[r9]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r10, r0, r4)
        L_0x00db:
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = ringerModeToString(r10)
            r3.append(r10)
            goto L_0x01cc
        L_0x00ec:
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = android.media.AudioSystem.streamToString(r10)
            r3.append(r10)
            r3.append(r7)
            r10 = r12[r8]
            r3.append(r10)
            goto L_0x01cc
        L_0x0105:
            r10 = 1386(0x56a, float:1.942E-42)
            r0.action(r10)
            goto L_0x01cc
        L_0x010c:
            r0 = 212(0xd4, float:2.97E-43)
            r4 = r12[r9]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r10, r0, r4)
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = android.media.AudioSystem.streamToString(r10)
            r3.append(r10)
            r3.append(r7)
            r10 = r12[r8]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = iconStateToString(r10)
            r3.append(r10)
            goto L_0x01cc
        L_0x013c:
            r0 = 211(0xd3, float:2.96E-43)
            r4 = r12[r9]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r10, r0, r4)
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = android.media.AudioSystem.streamToString(r10)
            r3.append(r10)
            r3.append(r7)
            r10 = r12[r8]
            r3.append(r10)
            goto L_0x01cc
        L_0x0161:
            r0 = 208(0xd0, float:2.91E-43)
            r4 = r12[r9]
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r4 = r4.booleanValue()
            com.android.internal.logging.MetricsLogger.visibility(r10, r0, r4)
            r10 = r12[r9]
            r3.append(r10)
            goto L_0x01cc
        L_0x0174:
            r0 = 210(0xd2, float:2.94E-43)
            r4 = r12[r9]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r10, r0, r4)
            r10 = r12[r9]
            java.lang.Integer r10 = (java.lang.Integer) r10
            int r10 = r10.intValue()
            java.lang.String r10 = android.media.AudioSystem.streamToString(r10)
            r3.append(r10)
            goto L_0x01cc
        L_0x0191:
            com.android.internal.logging.MetricsLogger.hidden(r10, r5)
            java.lang.String[] r10 = DISMISS_REASONS
            r0 = r12[r9]
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r10 = r10[r0]
            r3.append(r10)
            goto L_0x01cc
        L_0x01a4:
            com.android.internal.logging.MetricsLogger.visible(r10, r5)
            r0 = r12[r8]
            java.lang.Boolean r0 = (java.lang.Boolean) r0
            boolean r0 = r0.booleanValue()
            java.lang.String r4 = "volume_from_keyguard"
            com.android.internal.logging.MetricsLogger.histogram(r10, r4, r0)
            java.lang.String[] r10 = SHOW_REASONS
            r0 = r12[r9]
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            r10 = r10[r0]
            r3.append(r10)
            r3.append(r6)
            r10 = r12[r8]
            r3.append(r10)
        L_0x01cc:
            java.lang.String r10 = TAG
            java.lang.String r0 = r3.toString()
            android.util.Log.i(r10, r0)
            com.android.systemui.volume.Events$Callback r10 = sCallback
            if (r10 == 0) goto L_0x01dc
            r10.writeEvent(r1, r11, r12)
        L_0x01dc:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.volume.Events.writeEvent(android.content.Context, int, java.lang.Object[]):void");
    }

    public static void writeState(long j, State state) {
        Callback callback = sCallback;
        if (callback != null) {
            callback.writeState(j, state);
        }
    }

    private static String iconStateToString(int i) {
        if (i == 1) {
            return "unmute";
        }
        if (i == 2) {
            return "mute";
        }
        if (i == 3) {
            return "vibrate";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("unknown_state_");
        sb.append(i);
        return sb.toString();
    }
}
