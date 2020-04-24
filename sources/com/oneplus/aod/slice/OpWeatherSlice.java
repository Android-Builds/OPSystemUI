package com.oneplus.aod.slice;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$drawable;
import com.oneplus.aod.slice.OpSliceManager.Callback;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OpWeatherSlice extends OpSlice {
    /* access modifiers changed from: private */
    public static final Uri WEATHER_CONTENT_URI = Uri.parse("content://com.oneplus.weather.ContentProvider/data");
    private LocalDateTime mActiveStart;
    /* access modifiers changed from: private */
    public Context mContext;
    private boolean mFirstQueryInfo = false;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor = null;
    private WeatherObserver mObserver;
    private boolean mPendingUpdate = false;
    /* access modifiers changed from: private */
    public boolean mReady = false;
    /* access modifiers changed from: private */
    public int mState = 0;
    private LocalDateTime mUserActiveTime;

    private enum WeatherColumns {
        TIMESTAMP(0),
        WEATHER_CODE(2),
        WEATHER_NAME(6),
        TEMP(3),
        TEMP_HIGH(4),
        TEMP_LOW(5);
        
        /* access modifiers changed from: private */
        public int index;

        private WeatherColumns(int i) {
            this.index = i;
        }
    }

    private class WeatherObserver extends ContentObserver {
        public WeatherObserver() {
            super(OpWeatherSlice.this.mHandler);
        }

        public void onChange(boolean z) {
            super.onChange(z);
            String str = OpWeatherSlice.this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("weather info onChange query mState=");
            sb.append(OpWeatherSlice.this.mState);
            Log.d(str, sb.toString());
            OpWeatherSlice.this.queryWeatherInfo();
        }
    }

    private enum WeatherType {
        SUNNY(1001, R$drawable.op_ic_weather_sunny),
        SUNNY_INTERVALS(1002, R$drawable.op_ic_weather_sunny),
        CLOUDY(1003, R$drawable.op_ic_weather_cloudy),
        OVERCAST(1004, R$drawable.op_ic_weather_overcast),
        DRIZZLE(1005, R$drawable.op_ic_weather_rain),
        RAIN(1006, R$drawable.op_ic_weather_rain),
        SHOWER(1007, R$drawable.op_ic_weather_rain),
        DOWNPOUR(1008, R$drawable.op_ic_weather_rain),
        RAINSTORM(1009, R$drawable.op_ic_weather_rain),
        SLEET(1010, R$drawable.op_ic_weather_sleet),
        FLURRY(1011, R$drawable.op_ic_weather_snow),
        SNOW(1012, R$drawable.op_ic_weather_snow),
        SNOWSTORM(1013, R$drawable.op_ic_weather_snow),
        HAIL(1014, R$drawable.op_ic_weather_hail),
        THUNDERSHOWER(1015, R$drawable.op_ic_weather_rain),
        SANDSTORM(1016, R$drawable.op_ic_weather_sandstorm),
        FOG(1017, R$drawable.op_ic_weather_fog),
        HURRICANE(1018, R$drawable.op_ic_weather_typhoon),
        HAZE(1019, R$drawable.op_ic_weather_haze),
        NONE(9999, 0);
        
        int iconId;
        int weatherCode;

        private WeatherType(int i, int i2) {
            this.weatherCode = i;
            this.iconId = i2;
        }

        public static WeatherType getWeather(int i) {
            WeatherType[] values;
            for (WeatherType weatherType : values()) {
                if (weatherType.weatherCode == i) {
                    return weatherType;
                }
            }
            return NONE;
        }

        public String toString() {
            return String.valueOf(this.weatherCode);
        }
    }

    public OpWeatherSlice(Context context, Callback callback) {
        super(callback);
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    /* access modifiers changed from: protected */
    public void handleSetEnabled(boolean z) {
        if (z) {
            if (this.mObserver == null) {
                this.mObserver = new WeatherObserver();
                try {
                    this.mContext.getContentResolver().registerContentObserver(WEATHER_CONTENT_URI, true, this.mObserver);
                } catch (SecurityException e) {
                    String str = this.mTag;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Register observer fail: ");
                    sb.append(WEATHER_CONTENT_URI);
                    Log.d(str, sb.toString(), e);
                    this.mObserver = null;
                }
            }
            queryWeatherInfo();
        } else if (this.mObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            this.mObserver = null;
            this.mFirstQueryInfo = false;
        }
        refreshActive();
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        super.handleSetListening(z);
        String str = this.mTag;
        StringBuilder sb = new StringBuilder();
        sb.append("handleSetListening listening=");
        sb.append(z);
        sb.append(" mFirstQueryInfo=");
        sb.append(this.mFirstQueryInfo);
        sb.append(" mReady=");
        sb.append(this.mReady);
        Log.d(str, sb.toString());
        if (!this.mKeyguardUpdateMonitor.isUserUnlocked() || !z) {
            onUserActive();
            return;
        }
        refreshState();
        if (!this.mFirstQueryInfo) {
            queryWeatherInfo();
            this.mFirstQueryInfo = true;
        }
    }

    public void refreshState() {
        Context context = this.mContext;
        String string = context.getSharedPreferences(context.getPackageName(), 0).getString("pref_name_sleep_end", null);
        String str = "Parse sleep end time fail: e";
        if (string != null) {
            try {
                this.mActiveStart = LocalDateTime.parse(string).plusMinutes(15);
            } catch (DateTimeParseException e) {
                String str2 = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append(str);
                sb.append(e);
                Log.e(str2, sb.toString());
            }
        }
        Context context2 = this.mContext;
        String string2 = context2.getSharedPreferences(context2.getPackageName(), 0).getString("pref_name_initiative_pulse", null);
        if (TextUtils.isEmpty(string2)) {
            this.mUserActiveTime = null;
        } else {
            try {
                this.mUserActiveTime = LocalDateTime.parse(string2);
            } catch (DateTimeParseException e2) {
                String str3 = this.mTag;
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(e2);
                Log.e(str3, sb2.toString());
            }
        }
        refreshActive();
        if (OpSlice.DEBUG) {
            String str4 = this.mTag;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("time from sp=");
            sb3.append(string2);
            sb3.append(" mActiveStart=");
            sb3.append(this.mActiveStart);
            sb3.append(" mUserActiveTime=");
            sb3.append(this.mUserActiveTime);
            sb3.append(" now=");
            sb3.append(LocalDateTime.now());
            Log.d(str4, sb3.toString());
        }
    }

    private void refreshActive() {
        if (!isEnabled() || !this.mReady || this.mUserActiveTime == null || !LocalDateTime.now().isBefore(this.mUserActiveTime.plusMinutes(60))) {
            setActive(false);
        } else {
            setActive(true);
        }
        updateUI();
    }

    public void handleTimeChanged() {
        super.handleTimeChanged();
        if (!this.mFirstQueryInfo && !this.mReady && !isActive() && this.mKeyguardUpdateMonitor.isUserUnlocked()) {
            refreshState();
            queryWeatherInfo();
            this.mFirstQueryInfo = true;
            Log.i(this.mTag, "query weather info");
        }
    }

    /* access modifiers changed from: private */
    public void queryWeatherInfo() {
        new Thread(new Runnable() {
            public void run() {
                String str = OpWeatherSlice.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("run queryWeatherInfo mState=");
                sb.append(OpWeatherSlice.this.mState);
                Log.d(str, sb.toString());
                if (OpWeatherSlice.this.mState != 0) {
                    String str2 = OpWeatherSlice.this.mTag;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("skip mRunnable mState=");
                    sb2.append(OpWeatherSlice.this.mState);
                    Log.d(str2, sb2.toString());
                } else if (!OpUtils.isPackageInstalled(OpWeatherSlice.this.mContext, "net.oneplus.weather")) {
                    OpWeatherSlice.this.mReady = false;
                    Log.d(OpWeatherSlice.this.mTag, "Query weather info fail: app is not installed");
                } else {
                    OpWeatherSlice.this.mState = 1;
                    ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
                    Future submit = newSingleThreadExecutor.submit(new Runnable() {
                        public void run() {
                            Cursor query = OpWeatherSlice.this.mContext.getContentResolver().query(OpWeatherSlice.WEATHER_CONTENT_URI, null, null, null, null);
                            OpWeatherSlice.this.mState = 0;
                            OpWeatherSlice.this.processWeatherInfo(query);
                        }
                    });
                    try {
                        submit.get(3, TimeUnit.SECONDS);
                    } catch (TimeoutException unused) {
                        submit.cancel(true);
                        newSingleThreadExecutor.shutdownNow();
                        OpWeatherSlice.this.mState = 0;
                        Log.d(OpWeatherSlice.this.mTag, "Query weather info timeout: 3 seconds");
                    } catch (InterruptedException | ExecutionException e) {
                        OpWeatherSlice.this.mState = 0;
                        String str3 = OpWeatherSlice.this.mTag;
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("Query weather info fail: ");
                        sb3.append(e);
                        Log.e(str3, sb3.toString());
                    }
                }
            }
        }).start();
    }

    /* access modifiers changed from: private */
    public void processWeatherInfo(Cursor cursor) {
        String str = " ";
        String str2 = "˚";
        if (cursor == null) {
            Log.d(this.mTag, "Query weather info fail: cursor is null");
        } else if (!cursor.moveToFirst()) {
            Log.d(this.mTag, "Query weather info fail: cannot move to first");
            cursor.close();
        } else if (cursor.getColumnCount() < WeatherColumns.values().length) {
            Log.d(this.mTag, "Column count is not met the spec, need to check with OPWeather");
            String str3 = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("expected columns: ");
            sb.append(WeatherColumns.values().length);
            sb.append(", actual columns: ");
            sb.append(cursor.getColumnCount());
            Log.d(str3, sb.toString());
            cursor.close();
        } else {
            try {
                String string = cursor.getString(WeatherColumns.WEATHER_CODE.index);
                String string2 = cursor.getString(WeatherColumns.WEATHER_NAME.index);
                String string3 = cursor.getString(WeatherColumns.TEMP.index);
                String string4 = cursor.getString(WeatherColumns.TEMP_HIGH.index);
                String string5 = cursor.getString(WeatherColumns.TEMP_LOW.index);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("weatherCode: ");
                sb2.append(string);
                sb2.append(" weatherName: ");
                sb2.append(string2);
                sb2.append(" temperature: ");
                sb2.append(string3);
                sb2.append(" temperatureHigh: ");
                sb2.append(string4);
                sb2.append(" temperatureLow: ");
                sb2.append(string5);
                Log.d(this.mTag, sb2.toString());
                WeatherType weather = WeatherType.getWeather(Integer.parseInt(string));
                if (weather.weatherCode != 9999) {
                    this.mIcon = weather.iconId;
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(string2);
                    sb3.append(str);
                    sb3.append(string3);
                    sb3.append(str2);
                    this.mPrimary = sb3.toString();
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(string4);
                    sb4.append(str2);
                    sb4.append("/");
                    sb4.append(str);
                    sb4.append(string5);
                    sb4.append(str2);
                    this.mSecondary = sb4.toString();
                    if (OpSlice.DEBUG) {
                        String str4 = this.mTag;
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("processWeatherInfo: primary = ");
                        sb5.append(this.mPrimary);
                        sb5.append(", secondary = ");
                        sb5.append(this.mSecondary);
                        Log.i(str4, sb5.toString());
                    }
                    this.mReady = true;
                }
            } catch (IllegalStateException e) {
                String str5 = this.mTag;
                StringBuilder sb6 = new StringBuilder();
                sb6.append("invalid cursor data: ");
                sb6.append(e);
                Log.e(str5, sb6.toString());
            } catch (NullPointerException | NumberFormatException e2) {
                String str6 = this.mTag;
                StringBuilder sb7 = new StringBuilder();
                sb7.append("unexpected weather data: ");
                sb7.append(e2);
                Log.e(str6, sb7.toString());
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
            cursor.close();
            refreshActive();
        }
    }

    public void onUserActive() {
        if (this.mUserActiveTime == null && this.mActiveStart != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(this.mActiveStart)) {
                this.mUserActiveTime = now;
                Context context = this.mContext;
                context.getSharedPreferences(context.getPackageName(), 0).edit().putString("pref_name_initiative_pulse", now.toString()).apply();
                String str = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("save user initiative pulse time: ");
                sb.append(now);
                Log.d(str, sb.toString());
            }
        }
        refreshActive();
    }

    public void dump(PrintWriter printWriter) {
        super.dump(printWriter);
        printWriter.print("  mUserActiveTime=");
        printWriter.print(this.mUserActiveTime);
        printWriter.print(" mActiveStart=");
        printWriter.println(this.mActiveStart);
        printWriter.print(" now=");
        printWriter.println(LocalDateTime.now());
    }
}
