package com.oneplus.scene;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.util.Utils;
import com.oneplus.plugin.OpLsState;
import java.util.ArrayList;

public class OpSceneModeObserver {
    /* access modifiers changed from: private */
    public final ArrayList<Callback> mCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public Context mContext = null;
    /* access modifiers changed from: private */
    public boolean mIsInBrickMode = false;
    /* access modifiers changed from: private */
    public NavigationBarController mNavigationBarController;
    private SettingsObserver mSettingsObserver;

    public interface Callback {
        void onBrickModeChanged();
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mOpBrickModeStatusUri = Secure.getUriFor("op_breath_mode_status");

        SettingsObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: 0000 */
        public void observe() {
            OpSceneModeObserver.this.mContext.getContentResolver().registerContentObserver(this.mOpBrickModeStatusUri, false, this, -1);
            update(null);
        }

        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver contentResolver = OpSceneModeObserver.this.mContext.getContentResolver();
            if (uri == null || this.mOpBrickModeStatusUri.equals(uri)) {
                boolean equals = "1".equals(Secure.getStringForUser(contentResolver, "op_breath_mode_status", -2));
                if (OpSceneModeObserver.this.mIsInBrickMode != equals) {
                    OpSceneModeObserver.this.mIsInBrickMode = equals;
                    if (OpLsState.getInstance().getPhoneStatusBar() != null) {
                        OpLsState.getInstance().getPhoneStatusBar().onBrickModeChanged(equals);
                    }
                    if (!(OpSceneModeObserver.this.mNavigationBarController == null || OpSceneModeObserver.this.mNavigationBarController.getDefaultNavigationBarFragment() == null)) {
                        OpSceneModeObserver.this.mNavigationBarController.getDefaultNavigationBarFragment().onBrickModeChanged(equals);
                    }
                    Utils.safeForeach(OpSceneModeObserver.this.mCallbacks, C1994x664397c1.INSTANCE);
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("update uri: ");
            sb.append(uri);
            sb.append(" mIsInBrickMode: ");
            sb.append(OpSceneModeObserver.this.mIsInBrickMode);
            Log.d("OpSceneModeObserver", sb.toString());
        }
    }

    public OpSceneModeObserver(Context context) {
        this.mContext = context;
        this.mNavigationBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
        this.mSettingsObserver = new SettingsObserver(new Handler());
        this.mSettingsObserver.observe();
    }

    public boolean isInBrickMode() {
        return this.mIsInBrickMode;
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }
}
