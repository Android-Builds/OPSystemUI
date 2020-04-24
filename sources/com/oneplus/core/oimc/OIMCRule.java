package com.oneplus.core.oimc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class OIMCRule implements Parcelable {
    public static final Creator<OIMCRule> CREATOR = new Creator<OIMCRule>() {
        public OIMCRule createFromParcel(Parcel parcel) {
            return new OIMCRule(parcel);
        }

        public OIMCRule[] newArray(int i) {
            return new OIMCRule[i];
        }
    };
    public static final OIMCRule RULE_ALLOWWHITE_ACTIVITY;
    public static final OIMCRule RULE_ALLOWWHITE_VIBRATION;
    public static final OIMCRule RULE_AUDIOPROCESSES_CONTROLLER;
    public static final OIMCRule RULE_CAR_MODE_DOCKHANDLER;
    public static final OIMCRule RULE_CAR_MODE_OBSERVER;
    public static final OIMCRule RULE_COLORREADMODE_DISABLE_COLORBALANCE;
    public static final OIMCRule RULE_COLORREADMODE_DISABLE_GRAYCOLOR;
    public static final OIMCRule RULE_COLORREADMODE_PAPERCOLOR;
    public static final OIMCRule RULE_DISABLE_AUTOBACKLIGHT;
    public static final OIMCRule RULE_DISABLE_COLORBALANCE;
    public static final OIMCRule RULE_DISABLE_FINGERPRINTGESTURE;
    public static final OIMCRule RULE_DISABLE_GRAYCOLOR;
    public static final OIMCRule RULE_DISABLE_HEADSUPNOTIFICATION;
    public static final OIMCRule RULE_DISABLE_HEADSUPNOTIFICATION_ZEN;
    public static final OIMCRule RULE_DISABLE_PAPERCOLOR;
    public static final OIMCRule RULE_ENABLE_ANSWERWITHOUTUI;
    public static final OIMCRule RULE_ENABLE_ONEPLUSRAMBOOST;
    public static final OIMCRule RULE_ESPORTMODE;
    public static final OIMCRule RULE_FINGERPRINTMODE_COLORDISABLE;
    public static final OIMCRule RULE_FINGERPRINTMODE_DISABLE_COLORBALANCE;
    public static final OIMCRule RULE_FINGERPRINTMODE_DISABLE_GOOGLEMATRIX;
    public static final OIMCRule RULE_FINGERPRINTMODE_DISABLE_GRAYCOLOR;
    public static final OIMCRule RULE_FINGERPRINTMODE_DISABLE_PAPERCOLOR;
    public static final OIMCRule RULE_FLOATINGWINDOW_CONTROLLER;
    public static final OIMCRule RULE_GAME_MODE_OBSERVER;
    public static final OIMCRule RULE_GOOGLEMATRIXMODE_DISABLE_COLORBALANCE;
    public static final OIMCRule RULE_GOOGLEMATRIXMODE_DISABLE_GRAYCOLOR;
    public static final OIMCRule RULE_GOOGLEMATRIXMODE_DISABLE_PAPERCOLOR;
    public static final OIMCRule RULE_GOOGLEMATRIXMODE_GOOGLEMATRIX;
    public static final OIMCRule RULE_KEYBLOCKING;
    public static final OIMCRule RULE_NIGHTMODE_COLORBALANCE;
    public static final OIMCRule RULE_NOTIFYFOR3PTYCALLSBLOCKING;
    public static final OIMCRule RULE_NOTIFYFOR3PTYCALLSBLOCKING_CARMODE;
    public static final OIMCRule RULE_READMODE_DISABLE_COLORBALANCE;
    public static final OIMCRule RULE_READMODE_GRAYCOLOR;
    public static final OIMCRule RULE_SPECIAL_IMCALLING_HANDLER;
    public static final OIMCRule RULE_WIFIBACKGROUD_NETLIMIT;
    public static final OIMCRule RULE_ZEN_MODE_LEDCONTROLLER;
    public static final OIMCRule RULE_ZEN_MODE_OBSERVER;
    private int mAction;
    private String[] mDifferenceSwitch;
    private String mFuncName;
    private int mLevel;
    private int mReserved;
    private String[] mTriggerModes;

    public int describeContents() {
        return 0;
    }

    public OIMCRule(String str, int i, String[] strArr, int i2) {
        this.mLevel = 50;
        this.mFuncName = str;
        this.mAction = i;
        this.mTriggerModes = strArr;
        this.mReserved = i2;
        this.mLevel = 50;
        this.mDifferenceSwitch = null;
    }

    public OIMCRule(String str, int i, String[] strArr, int i2, int i3, String[] strArr2) {
        this.mLevel = 50;
        this.mFuncName = str;
        this.mAction = i;
        this.mTriggerModes = strArr;
        this.mReserved = i2;
        this.mLevel = i3;
        this.mDifferenceSwitch = strArr2;
    }

    static {
        String str = "GameMode";
        RULE_DISABLE_HEADSUPNOTIFICATION = new OIMCRule("HeadsUpNotification", 1, new String[]{str}, 0);
        String str2 = "ZenMode";
        RULE_DISABLE_HEADSUPNOTIFICATION_ZEN = new OIMCRule("HeadsUpNotificationZen", 1, new String[]{str2}, 0);
        RULE_ZEN_MODE_OBSERVER = new OIMCRule("ZenModeObserver", 1, new String[]{str2}, 0);
        RULE_GAME_MODE_OBSERVER = new OIMCRule("GameModeObserver", 1, new String[]{str}, 0);
        String str3 = "CarMode";
        RULE_CAR_MODE_OBSERVER = new OIMCRule("CarModeObserver", 1, new String[]{str3}, 0);
        RULE_WIFIBACKGROUD_NETLIMIT = new OIMCRule("WiFiBackgroudNetLimit", 1, new String[]{str}, 0);
        OIMCRule oIMCRule = new OIMCRule("NotifyFor3PtyCallsBlocking", 1, new String[]{str}, 0, 50, new String[]{"popNotify"});
        RULE_NOTIFYFOR3PTYCALLSBLOCKING = oIMCRule;
        OIMCRule oIMCRule2 = new OIMCRule("NotifyFor3PtyCallsBlocking_CarMode", 1, new String[]{str3}, 0, 51, new String[]{"not popNotify"});
        RULE_NOTIFYFOR3PTYCALLSBLOCKING_CARMODE = oIMCRule2;
        String str4 = "";
        OIMCRule oIMCRule3 = new OIMCRule("SpecialIMCallingHandler", 1, new String[]{str3}, 0, 50, new String[]{str4});
        RULE_SPECIAL_IMCALLING_HANDLER = oIMCRule3;
        OIMCRule oIMCRule4 = new OIMCRule("CarModeDockHandler", 1, new String[]{str3}, 0, 50, new String[]{str4});
        RULE_CAR_MODE_DOCKHANDLER = oIMCRule4;
        RULE_DISABLE_AUTOBACKLIGHT = new OIMCRule("TurnOffAutoBacklight", 1, new String[]{str}, 0);
        RULE_KEYBLOCKING = new OIMCRule("KeyBlocking", 1, new String[]{str}, 0);
        RULE_ALLOWWHITE_VIBRATION = new OIMCRule("AllowWhiteVibration", 1, new String[]{str2}, 0);
        RULE_ALLOWWHITE_ACTIVITY = new OIMCRule("AllowWhiteActivity", 1, new String[]{str2}, 0);
        RULE_ZEN_MODE_LEDCONTROLLER = new OIMCRule("ZenModeLedController", 1, new String[]{str2}, 0);
        RULE_FLOATINGWINDOW_CONTROLLER = new OIMCRule("FloatingWindowController", 1, new String[]{str2}, 0);
        RULE_AUDIOPROCESSES_CONTROLLER = new OIMCRule("AudioProcessesController", 1, new String[]{str2}, 0);
        String str5 = "ColorBalance";
        RULE_NIGHTMODE_COLORBALANCE = new OIMCRule(str5, 1, new String[]{"NightMode"}, 0);
        String str6 = "GrayColor";
        RULE_READMODE_GRAYCOLOR = new OIMCRule(str6, 1, new String[]{"ReadMode"}, 0);
        RULE_READMODE_DISABLE_COLORBALANCE = new OIMCRule(str5, 2, new String[]{"ReadMode"}, 0);
        String str7 = "SystemMode";
        RULE_DISABLE_GRAYCOLOR = new OIMCRule(str6, 2, new String[]{str7}, 0);
        RULE_DISABLE_COLORBALANCE = new OIMCRule(str5, 2, new String[]{str7}, 0);
        String str8 = "FingerPrintMode";
        RULE_FINGERPRINTMODE_COLORDISABLE = new OIMCRule("ColorDisable", 1, new String[]{str8}, 0);
        String str9 = "fast";
        OIMCRule oIMCRule5 = new OIMCRule("GrayColor", 2, new String[]{str8}, 0, 50, new String[]{str9});
        RULE_FINGERPRINTMODE_DISABLE_GRAYCOLOR = oIMCRule5;
        OIMCRule oIMCRule6 = new OIMCRule("ColorBalance", 2, new String[]{str8}, 0, 50, new String[]{str9});
        RULE_FINGERPRINTMODE_DISABLE_COLORBALANCE = oIMCRule6;
        RULE_ENABLE_ANSWERWITHOUTUI = new OIMCRule("AnswerWithoutUI", 1, new String[]{str}, 0);
        String str10 = "GoogleMatrixMode";
        RULE_GOOGLEMATRIXMODE_GOOGLEMATRIX = new OIMCRule("gooleMatrix", 1, new String[]{str10}, 0);
        RULE_GOOGLEMATRIXMODE_DISABLE_GRAYCOLOR = new OIMCRule(str6, 2, new String[]{str10}, 0);
        RULE_GOOGLEMATRIXMODE_DISABLE_COLORBALANCE = new OIMCRule(str5, 2, new String[]{str10}, 0);
        String str11 = "PaperColor";
        RULE_GOOGLEMATRIXMODE_DISABLE_PAPERCOLOR = new OIMCRule(str11, 2, new String[]{str10}, 0);
        OIMCRule oIMCRule7 = new OIMCRule("gooleMatrix", 2, new String[]{str8}, 0, 50, new String[]{str9});
        RULE_FINGERPRINTMODE_DISABLE_GOOGLEMATRIX = oIMCRule7;
        String str12 = "ColorReadMode";
        RULE_COLORREADMODE_PAPERCOLOR = new OIMCRule(str11, 1, new String[]{str12}, 0);
        RULE_DISABLE_PAPERCOLOR = new OIMCRule(str11, 2, new String[]{str7}, 0);
        RULE_COLORREADMODE_DISABLE_GRAYCOLOR = new OIMCRule(str6, 2, new String[]{str12}, 0);
        RULE_COLORREADMODE_DISABLE_COLORBALANCE = new OIMCRule(str5, 2, new String[]{str12}, 0);
        OIMCRule oIMCRule8 = new OIMCRule("PaperColor", 2, new String[]{str8}, 0, 50, new String[]{str9});
        RULE_FINGERPRINTMODE_DISABLE_PAPERCOLOR = oIMCRule8;
        RULE_DISABLE_FINGERPRINTGESTURE = new OIMCRule("FingerprintGestureLimit", 1, new String[]{str}, 0);
        RULE_ENABLE_ONEPLUSRAMBOOST = new OIMCRule("OnePlusRamboost", 1, new String[]{str7}, 0);
        RULE_ESPORTMODE = new OIMCRule("ESportMode", 1, new String[]{str}, 0);
    }

    private OIMCRule(Parcel parcel) {
        this.mLevel = 50;
        readFromParcel(parcel);
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mFuncName);
        parcel.writeInt(this.mAction);
        parcel.writeStringArray(this.mTriggerModes);
        parcel.writeInt(this.mReserved);
        parcel.writeInt(this.mLevel);
        parcel.writeStringArray(this.mDifferenceSwitch);
    }

    public void readFromParcel(Parcel parcel) {
        this.mFuncName = parcel.readString();
        this.mAction = parcel.readInt();
        this.mTriggerModes = parcel.readStringArray();
        this.mReserved = parcel.readInt();
        this.mLevel = parcel.readInt();
        this.mDifferenceSwitch = parcel.readStringArray();
    }
}
