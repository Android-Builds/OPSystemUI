package com.oneplus.aod.slice;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class OpSliceManager {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static final int[] PRIORITY = {1, 2, 3};
    /* access modifiers changed from: private */
    public boolean mCalendarEnabled = false;
    /* access modifiers changed from: private */
    public Context mContext;
    private final C1752H mHandler = new C1752H((Looper) Dependency.get(Dependency.BG_LOOPER));
    private ImageView mIcon;
    private boolean mListening = false;
    /* access modifiers changed from: private */
    public boolean mMusicInfoEnabled = false;
    private TextView mPrimary;
    private TextView mRemark;
    private TextView mSecondary;
    private SettingsObserver mSettingsObserver;
    private final BroadcastReceiver mSleepStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = "OpSliceManager";
            if (OpSliceManager.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onReceive: ");
                sb.append(intent);
                Log.d(str, sb.toString());
            }
            String action = intent.getAction();
            if ("net.oneplus.powercontroller.intent.SLEEP_CHANGED".equals(action) || "com.android.systemui.intent.SLEEP_CHANGED".equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null && extras.getInt("state") == 7788) {
                    String localDateTime = LocalDateTime.now().toString();
                    OpSliceManager.this.mContext.getSharedPreferences(OpSliceManager.this.mContext.getPackageName(), 0).edit().putString("pref_name_sleep_end", localDateTime).apply();
                    OpSliceManager.this.mContext.getSharedPreferences(OpSliceManager.this.mContext.getPackageName(), 0).edit().putString("pref_name_initiative_pulse", "").apply();
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("save sleep end time: ");
                    sb2.append(localDateTime);
                    sb2.append(" and clear user initiative pulse time");
                    Log.d(str, sb2.toString());
                    OpSlice opSlice = (OpSlice) OpSliceManager.this.mSlices.get(Integer.valueOf(3));
                    if (opSlice != null && opSlice.isEnabled()) {
                        ((OpWeatherSlice) opSlice).refreshState();
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final LinkedHashMap<Integer, OpSlice> mSlices = new LinkedHashMap<>();
    /* access modifiers changed from: private */
    public boolean mSmartDisplayCurState = false;
    /* access modifiers changed from: private */
    public boolean mSmartDisplayEnabled = false;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private int mUserId;
    private OpSliceContainer mViewContainer;

    public class Callback {
        private OpSliceManager mSliceManager;

        public Callback(OpSliceManager opSliceManager) {
            this.mSliceManager = opSliceManager;
        }

        public void updateUI() {
            this.mSliceManager.refresh();
        }
    }

    /* renamed from: com.oneplus.aod.slice.OpSliceManager$H */
    protected final class C1752H extends Handler {
        protected C1752H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            if (message.what == 1) {
                OpSliceManager.this.refresh();
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri uriCalendarEnabled = System.getUriFor("aod_smart_display_calendar_enabled");
        private final Uri uriMusicInfoEnabled = System.getUriFor("aod_smart_display_music_info_enabled");
        private final Uri uriSmartDisplayCurState = System.getUriFor("aod_smart_display_cur_state");
        private final Uri uriSmartDisplayEnabled = System.getUriFor("aod_smart_display_enabled");

        SettingsObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: 0000 */
        public void observe() {
            ContentResolver contentResolver = OpSliceManager.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(this.uriSmartDisplayEnabled, false, this, -1);
            contentResolver.registerContentObserver(this.uriSmartDisplayCurState, false, this, -1);
            contentResolver.registerContentObserver(this.uriMusicInfoEnabled, false, this, -1);
            contentResolver.registerContentObserver(this.uriCalendarEnabled, false, this, -1);
            update(null);
        }

        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver contentResolver = OpSliceManager.this.mContext.getContentResolver();
            boolean z = false;
            if (uri == null || this.uriSmartDisplayEnabled.equals(uri)) {
                OpSliceManager.this.mSmartDisplayEnabled = 1 == System.getIntForUser(contentResolver, "aod_smart_display_enabled", 1, -2);
            }
            if (uri == null || this.uriSmartDisplayCurState.equals(uri)) {
                OpSliceManager.this.mSmartDisplayCurState = 1 == System.getIntForUser(contentResolver, "aod_smart_display_cur_state", 1, -2);
            }
            if (uri == null || this.uriMusicInfoEnabled.equals(uri)) {
                OpSliceManager.this.mMusicInfoEnabled = 1 == System.getIntForUser(contentResolver, "aod_smart_display_music_info_enabled", 1, -2);
            }
            if (uri == null || this.uriCalendarEnabled.equals(uri)) {
                OpSliceManager opSliceManager = OpSliceManager.this;
                if (1 == System.getIntForUser(contentResolver, "aod_smart_display_calendar_enabled", 1, -2)) {
                    z = true;
                }
                opSliceManager.mCalendarEnabled = z;
            }
            OpSliceManager.this.updateEnabled();
            if (OpSliceManager.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("update uri=");
                sb.append(uri);
                sb.append(" mSmartDisplayEnabled=");
                sb.append(OpSliceManager.this.mSmartDisplayEnabled);
                sb.append(" mSmartDisplayCurState = ");
                sb.append(OpSliceManager.this.mSmartDisplayCurState);
                sb.append(" mMusicInfoEnabled=");
                sb.append(OpSliceManager.this.mMusicInfoEnabled);
                sb.append(" mCalendarEnabled=");
                sb.append(OpSliceManager.this.mCalendarEnabled);
                Log.d("OpSliceManager", sb.toString());
            }
        }
    }

    private String getSliceName(int i) {
        return i == 1 ? "calendar" : i == 2 ? "music" : i == 3 ? "weather" : "none";
    }

    public OpSliceManager(Context context, View view) {
        this.mContext = context;
        this.mUserId = KeyguardUpdateMonitor.getCurrentUser();
        Callback callback = new Callback(this);
        this.mSlices.put(Integer.valueOf(2), new OpMusicSlice(this.mContext, callback));
        this.mSlices.put(Integer.valueOf(3), new OpWeatherSlice(this.mContext, callback));
        this.mSlices.put(Integer.valueOf(1), new OpCalendarSlice(this.mContext, callback));
        initViews(view);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        IntentFilter intentFilter = new IntentFilter("net.oneplus.powercontroller.intent.SLEEP_CHANGED");
        intentFilter.addAction("com.android.systemui.intent.SLEEP_CHANGED");
        this.mContext.registerReceiverAsUser(this.mSleepStateReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    public void initViews(View view) {
        this.mViewContainer = (OpSliceContainer) view;
        this.mIcon = (ImageView) view.findViewById(R$id.slice_icon);
        this.mPrimary = (TextView) view.findViewById(R$id.slice_primary);
        this.mRemark = (TextView) view.findViewById(R$id.slice_remark);
        this.mSecondary = (TextView) view.findViewById(R$id.slice_secondary);
    }

    public void setListening(boolean z) {
        String str = "OpSliceManager";
        if (this.mUserId != 0) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Do not active slices since current user is ");
                sb.append(this.mUserId);
                Log.i(str, sb.toString());
            }
        } else if (!this.mSmartDisplayEnabled) {
            if (DEBUG) {
                Log.i(str, "Do not active slices since smart aod is disabled");
            }
        } else if (this.mListening != z) {
            this.mListening = z;
            this.mSlices.values().forEach(new Consumer(z) {
                private final /* synthetic */ boolean f$0;

                {
                    this.f$0 = r1;
                }

                public final void accept(Object obj) {
                    ((OpSlice) obj).setListening(this.f$0);
                }
            });
            this.mHandler.sendEmptyMessage(1);
        }
    }

    /* access modifiers changed from: private */
    public void updateEnabled() {
        for (Integer intValue : this.mSlices.keySet()) {
            int intValue2 = intValue.intValue();
            boolean z = false;
            boolean z2 = this.mSmartDisplayEnabled && this.mSmartDisplayCurState;
            if (this.mUserId == 0 && ((intValue2 != 2 || this.mMusicInfoEnabled) && (intValue2 != 1 || this.mCalendarEnabled))) {
                z = z2;
            }
            ((OpSlice) this.mSlices.get(Integer.valueOf(intValue2))).setEnabled(z);
        }
    }

    /* access modifiers changed from: private */
    public void refresh() {
        this.mUiHandler.post(new Runnable() {
            public final void run() {
                OpSliceManager.this.lambda$refresh$1$OpSliceManager();
            }
        });
    }

    public /* synthetic */ void lambda$refresh$1$OpSliceManager() {
        int activeSlice = getActiveSlice();
        OpSlice opSlice = (OpSlice) this.mSlices.get(Integer.valueOf(activeSlice));
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("slice count: ");
            sb.append(this.mSlices.size());
            sb.append(" refresh to ");
            sb.append(getSliceName(activeSlice));
            Log.i("OpSliceManager", sb.toString());
        }
        if (opSlice != null) {
            this.mIcon.setVisibility(0);
            this.mPrimary.setVisibility(0);
            this.mSecondary.setVisibility(0);
            this.mIcon.setImageResource(opSlice.getIcon());
            this.mPrimary.setText(opSlice.getPrimaryString());
            this.mSecondary.setText(opSlice.getSecondaryString());
            String remark = opSlice.getRemark();
            if (remark == null || remark.trim().length() == 0) {
                this.mRemark.setVisibility(8);
                return;
            }
            this.mRemark.setText(remark);
            this.mRemark.setVisibility(0);
            return;
        }
        this.mIcon.setVisibility(8);
        this.mPrimary.setVisibility(8);
        this.mSecondary.setVisibility(8);
        this.mRemark.setVisibility(8);
    }

    public void onTimeChanged() {
        if (this.mListening) {
            for (Integer intValue : this.mSlices.keySet()) {
                OpSlice opSlice = (OpSlice) this.mSlices.get(Integer.valueOf(intValue.intValue()));
                if (opSlice != null && opSlice.isEnabled()) {
                    opSlice.onTimeChanged();
                }
            }
        }
    }

    public void onInitiativePulse() {
        OpSlice opSlice = (OpSlice) this.mSlices.get(Integer.valueOf(3));
        if (opSlice != null && opSlice.isEnabled()) {
            ((OpWeatherSlice) opSlice).onUserActive();
        }
    }

    private int getActiveSlice() {
        String str;
        int length = PRIORITY.length;
        for (int i = 0; i < length; i++) {
            OpSlice opSlice = (OpSlice) this.mSlices.get(Integer.valueOf(PRIORITY[i]));
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("setSlice: ");
                sb.append(getSliceName(PRIORITY[i]));
                sb.append(" priority: ");
                sb.append(i);
                if (opSlice == null) {
                    str = " slice is null";
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(" isActive=");
                    sb2.append(opSlice.isActive());
                    sb2.append(" isEnabled=");
                    sb2.append(opSlice.isEnabled());
                    str = sb2.toString();
                }
                sb.append(str);
                Log.i("OpSliceManager", sb.toString());
            }
            if (opSlice != null && opSlice.isActive() && opSlice.isEnabled()) {
                return PRIORITY[i];
            }
        }
        return 0;
    }

    public void onUserSwitchComplete(int i) {
        this.mUserId = i;
        updateEnabled();
    }

    public void dump(PrintWriter printWriter) {
        this.mSlices.values().forEach(new Consumer(printWriter) {
            private final /* synthetic */ PrintWriter f$0;

            {
                this.f$0 = r1;
            }

            public final void accept(Object obj) {
                ((OpSlice) obj).dump(this.f$0);
            }
        });
    }
}
