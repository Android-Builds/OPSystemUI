package com.airbnb.lottie.utils;

import android.util.Log;
import com.airbnb.lottie.C0489L;
import com.airbnb.lottie.LottieLogger;
import java.util.HashSet;
import java.util.Set;

public class LogcatLogger implements LottieLogger {
    private static final Set<String> loggedMessages = new HashSet();

    public void debug(String str) {
        debug(str, null);
    }

    public void debug(String str, Throwable th) {
        if (C0489L.DBG) {
            Log.d(C0489L.TAG, str, th);
        }
    }

    public void warning(String str) {
        warning(str, null);
    }

    public void warning(String str, Throwable th) {
        if (!loggedMessages.contains(str)) {
            Log.w(C0489L.TAG, str, th);
            loggedMessages.add(str);
        }
    }
}
