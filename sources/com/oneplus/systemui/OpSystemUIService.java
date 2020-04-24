package com.oneplus.systemui;

import android.app.Service;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.SystemProperties;
import android.util.Slog;

public class OpSystemUIService extends Service {
    private int mFontConfig = 0;

    public void onCreate() {
        super.onCreate();
        initFont();
    }

    private void initFont() {
        int i = 1;
        try {
            i = SystemProperties.getInt("persist.sys.font", 1);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Font]");
            sb.append(e.getMessage());
            Slog.e("Font", sb.toString());
        }
        if (this.mFontConfig != i) {
            Typeface.changeFont(i);
            this.mFontConfig = i;
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        int i = configuration.oneplusfont;
        if (i != this.mFontConfig) {
            Typeface.changeFont(i);
            this.mFontConfig = configuration.oneplusfont;
        }
    }
}
