package com.oneplus.custom.utils;

public class OpCustomizeSettings {
    private static final String PROJECT_NAME = SystemProperties.get("ro.boot.project_name");
    private static OpCustomizeSettings sOpCustomizeSettings;

    public enum CUSTOM_TYPE {
        NONE,
        JCC,
        SW,
        AVG,
        MCL,
        OPR_RETAIL
    }

    public static CUSTOM_TYPE getCustomType() {
        return getInstance().getCustomization();
    }

    private static OpCustomizeSettings getInstance() {
        if (sOpCustomizeSettings == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("PROJECT_NAME = ");
            sb.append(PROJECT_NAME);
            MyLog.m58v("OpCustomizeSettings", sb.toString());
            if (!"16859".equals(PROJECT_NAME)) {
                if (!"17801".equals(PROJECT_NAME)) {
                    if (!"15801".equals(PROJECT_NAME)) {
                        if (!"15811".equals(PROJECT_NAME)) {
                            sOpCustomizeSettings = new OpCustomizeSettingsG2();
                        }
                    }
                    sOpCustomizeSettings = new OpCustomizeSettings();
                }
            }
            sOpCustomizeSettings = new OpCustomizeSettingsG1();
        }
        return sOpCustomizeSettings;
    }

    /* access modifiers changed from: protected */
    public CUSTOM_TYPE getCustomization() {
        return CUSTOM_TYPE.NONE;
    }
}
