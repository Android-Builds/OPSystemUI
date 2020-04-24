package com.android.systemui.statusbar.policy;

import com.android.systemui.R$drawable;
import com.oneplus.systemui.statusbar.policy.OpWifiIcons;

public class WifiIcons extends OpWifiIcons {
    public static final int[][] QS_WIFI_4_SIGNAL_STRENGTH = {WIFI_4_NO_INTERNET_ICONS, WIFI_4_FULL_ICONS};
    public static final int[][] QS_WIFI_5_SIGNAL_STRENGTH = {WIFI_5_NO_INTERNET_ICONS, WIFI_5_FULL_ICONS};
    public static final int[][] QS_WIFI_6_SIGNAL_STRENGTH = {WIFI_6_NO_INTERNET_ICONS, WIFI_6_FULL_ICONS};
    public static final int QS_WIFI_DISABLED = R$drawable.op_q_ic_qs_wifi_disabled;
    public static final int QS_WIFI_NO_NETWORK = R$drawable.op_q_stat_sys_wifi_signal_0_fully;
    public static final int[][] QS_WIFI_SIGNAL_STRENGTH = {WIFI_NO_INTERNET_ICONS, WIFI_FULL_ICONS};
    static final int[] WIFI_4_FULL_ICONS = {17302845, 17302846, 17302847, 17302848, 17302849};
    private static final int[] WIFI_4_NO_INTERNET_ICONS = {R$drawable.ic_qs_wifi_4_0, R$drawable.ic_qs_wifi_4_1, R$drawable.ic_qs_wifi_4_2, R$drawable.ic_qs_wifi_4_3, R$drawable.ic_qs_wifi_4_4};
    static final int[][] WIFI_4_SIGNAL_STRENGTH = QS_WIFI_4_SIGNAL_STRENGTH;
    static final int[] WIFI_5_FULL_ICONS = {17302850, 17302851, 17302852, 17302853, 17302854};
    private static final int[] WIFI_5_NO_INTERNET_ICONS = {R$drawable.ic_qs_wifi_5_0, R$drawable.ic_qs_wifi_5_1, R$drawable.ic_qs_wifi_5_2, R$drawable.ic_qs_wifi_5_3, R$drawable.ic_qs_wifi_5_4};
    static final int[][] WIFI_5_SIGNAL_STRENGTH = QS_WIFI_5_SIGNAL_STRENGTH;
    static final int[] WIFI_6_FULL_ICONS = {17302855, 17302856, 17302857, 17302858, 17302859};
    private static final int[] WIFI_6_NO_INTERNET_ICONS = {R$drawable.ic_qs_wifi_6_0, R$drawable.ic_qs_wifi_6_1, R$drawable.ic_qs_wifi_6_2, R$drawable.ic_qs_wifi_6_3, R$drawable.ic_qs_wifi_6_4};
    static final int[][] WIFI_6_SIGNAL_STRENGTH = QS_WIFI_6_SIGNAL_STRENGTH;
    static final int[] WIFI_FULL_ICONS = {R$drawable.op_q_stat_sys_wifi_signal_0_fully, R$drawable.op_q_stat_sys_wifi_signal_1_fully, R$drawable.op_q_stat_sys_wifi_signal_2_fully, R$drawable.op_q_stat_sys_wifi_signal_3_fully, R$drawable.op_q_stat_sys_wifi_signal_4_fully};
    static final int WIFI_LEVEL_COUNT = WIFI_SIGNAL_STRENGTH[0].length;
    private static final int[] WIFI_NO_INTERNET_ICONS = {R$drawable.op_q_stat_sys_wifi_signal_0, R$drawable.op_q_stat_sys_wifi_signal_1, R$drawable.op_q_stat_sys_wifi_signal_2, R$drawable.op_q_stat_sys_wifi_signal_3, R$drawable.op_q_stat_sys_wifi_signal_4};
    static final int WIFI_NO_NETWORK = QS_WIFI_NO_NETWORK;
    static final int[][] WIFI_SIGNAL_STRENGTH = QS_WIFI_SIGNAL_STRENGTH;
}
