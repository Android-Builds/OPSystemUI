package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService.Tunable;
import com.android.systemui.util.leak.LeakDetector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TunerServiceImpl extends TunerService {
    private static final String[] RESET_BLACKLIST = {"sysui_qs_tiles", "doze_always_on"};
    private ContentResolver mContentResolver;
    private final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentUser;
    private final LeakDetector mLeakDetector;
    private final ArrayMap<Uri, String> mListeningUris = new ArrayMap<>();
    private final Observer mObserver = new Observer();
    private TelephonyManager mTM;
    private final HashMap<String, Set<Tunable>> mTunableLookup = new HashMap<>();
    private final HashSet<Tunable> mTunables;
    private CurrentUserTracker mUserTracker;

    private class Observer extends ContentObserver {
        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void onChange(boolean z, Uri uri, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                TunerServiceImpl.this.reloadSetting(uri);
            }
        }
    }

    public TunerServiceImpl(Context context, Handler handler, LeakDetector leakDetector) {
        this.mTunables = LeakDetector.ENABLED ? new HashSet<>() : null;
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mLeakDetector = leakDetector;
        for (UserInfo userHandle : UserManager.get(this.mContext).getUsers()) {
            this.mCurrentUser = userHandle.getUserHandle().getIdentifier();
            String str = "sysui_tuner_version";
            if (getValue(str, 0) != 4) {
                upgradeTuner(getValue(str, 0), 4, handler);
            }
        }
        this.mCurrentUser = ActivityManager.getCurrentUser();
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int i) {
                TunerServiceImpl.this.mCurrentUser = i;
                TunerServiceImpl.this.reloadAll();
                TunerServiceImpl.this.reregisterAll();
            }
        };
        this.mUserTracker.startTracking();
        this.mTM = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    private void upgradeTuner(int i, int i2, Handler handler) {
        if (i < 1) {
            String str = "icon_blacklist";
            String value = getValue(str);
            if (value != null) {
                ArraySet iconBlacklist = StatusBarIconController.getIconBlacklist(value);
                iconBlacklist.add("rotate");
                TelephonyManager telephonyManager = this.mTM;
                if (telephonyManager != null && TextUtils.equals(telephonyManager.getSimOperatorNumeric(SubscriptionManager.getDefaultDataSubscriptionId()), "23410")) {
                    Log.d("TunerService", "O2 UK sim, add volte/vowifi to blacklist by default");
                    iconBlacklist.add("volte");
                    iconBlacklist.add("vowifi");
                }
                Secure.putStringForUser(this.mContentResolver, str, TextUtils.join(",", iconBlacklist), this.mCurrentUser);
            }
        }
        setValue("sysui_tuner_version", i2);
    }

    public String getValue(String str) {
        return Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
    }

    public void setValue(String str, String str2) {
        Secure.putStringForUser(this.mContentResolver, str, str2, this.mCurrentUser);
    }

    public int getValue(String str, int i) {
        return Secure.getIntForUser(this.mContentResolver, str, i, this.mCurrentUser);
    }

    public void setValue(String str, int i) {
        Secure.putIntForUser(this.mContentResolver, str, i, this.mCurrentUser);
    }

    public void addTunable(Tunable tunable, String... strArr) {
        for (String addTunable : strArr) {
            addTunable(tunable, addTunable);
        }
    }

    private void addTunable(Tunable tunable, String str) {
        if (!this.mTunableLookup.containsKey(str)) {
            this.mTunableLookup.put(str, new ArraySet());
        }
        ((Set) this.mTunableLookup.get(str)).add(tunable);
        if (LeakDetector.ENABLED) {
            this.mTunables.add(tunable);
            this.mLeakDetector.trackCollection(this.mTunables, "TunerService.mTunables");
        }
        Uri uriFor = Secure.getUriFor(str);
        if (!this.mListeningUris.containsKey(uriFor)) {
            this.mListeningUris.put(uriFor, str);
            this.mContentResolver.registerContentObserver(uriFor, false, this.mObserver, this.mCurrentUser);
        }
        tunable.onTuningChanged(str, Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser));
    }

    public void removeTunable(Tunable tunable) {
        for (Set remove : this.mTunableLookup.values()) {
            remove.remove(tunable);
        }
        if (LeakDetector.ENABLED) {
            this.mTunables.remove(tunable);
        }
    }

    /* access modifiers changed from: protected */
    public void reregisterAll() {
        if (this.mListeningUris.size() != 0) {
            this.mContentResolver.unregisterContentObserver(this.mObserver);
            for (Uri registerContentObserver : this.mListeningUris.keySet()) {
                this.mContentResolver.registerContentObserver(registerContentObserver, false, this.mObserver, this.mCurrentUser);
            }
        }
    }

    /* access modifiers changed from: private */
    public void reloadSetting(Uri uri) {
        String str = (String) this.mListeningUris.get(uri);
        Set<Tunable> set = (Set) this.mTunableLookup.get(str);
        if (set != null) {
            String stringForUser = Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
            for (Tunable onTuningChanged : set) {
                onTuningChanged.onTuningChanged(str, stringForUser);
            }
        }
    }

    /* access modifiers changed from: private */
    public void reloadAll() {
        for (String str : this.mTunableLookup.keySet()) {
            String stringForUser = Secure.getStringForUser(this.mContentResolver, str, this.mCurrentUser);
            for (Tunable onTuningChanged : (Set) this.mTunableLookup.get(str)) {
                onTuningChanged.onTuningChanged(str, stringForUser);
            }
        }
    }

    public void clearAll() {
        clearAllFromUser(this.mCurrentUser);
    }

    public void clearAllFromUser(int i) {
        Global.putString(this.mContentResolver, "sysui_demo_allowed", null);
        Intent intent = new Intent("com.android.systemui.demo");
        intent.putExtra("command", "exit");
        this.mContext.sendBroadcast(intent);
        for (String str : this.mTunableLookup.keySet()) {
            if (!ArrayUtils.contains(RESET_BLACKLIST, str)) {
                Secure.putStringForUser(this.mContentResolver, str, null, i);
            }
        }
    }
}
