package com.android.systemui.p007qs.tiles;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.plugins.p006qs.QSTile.State;
import java.util.Arrays;
import java.util.Objects;

/* renamed from: com.android.systemui.qs.tiles.IntentTile */
public class IntentTile extends QSTileImpl<State> {
    private int mCurrentUserId;
    private String mIntentPackage;
    private Intent mLastIntent;
    private PendingIntent mOnClick;
    private String mOnClickUri;
    private PendingIntent mOnLongClick;
    private String mOnLongClickUri;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            IntentTile.this.refreshState(intent);
        }
    };

    /* renamed from: com.android.systemui.qs.tiles.IntentTile$BytesIcon */
    private static class BytesIcon extends Icon {
        private final byte[] mBytes;

        public BytesIcon(byte[] bArr) {
            this.mBytes = bArr;
        }

        public Drawable getDrawable(Context context) {
            byte[] bArr = this.mBytes;
            return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(bArr, 0, bArr.length));
        }

        public boolean equals(Object obj) {
            return (obj instanceof BytesIcon) && Arrays.equals(((BytesIcon) obj).mBytes, this.mBytes);
        }

        public String toString() {
            return String.format("BytesIcon[len=%s]", new Object[]{Integer.valueOf(this.mBytes.length)});
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.IntentTile$PackageDrawableIcon */
    private class PackageDrawableIcon extends Icon {
        private final String mPackage;
        private final int mResId;

        public PackageDrawableIcon(String str, int i) {
            this.mPackage = str;
            this.mResId = i;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof PackageDrawableIcon)) {
                return false;
            }
            PackageDrawableIcon packageDrawableIcon = (PackageDrawableIcon) obj;
            if (Objects.equals(packageDrawableIcon.mPackage, this.mPackage) && packageDrawableIcon.mResId == this.mResId) {
                z = true;
            }
            return z;
        }

        public Drawable getDrawable(Context context) {
            try {
                return context.createPackageContext(this.mPackage, 0).getDrawable(this.mResId);
            } catch (Throwable th) {
                String access$100 = IntentTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Error loading package drawable pkg=");
                sb.append(this.mPackage);
                sb.append(" id=");
                sb.append(this.mResId);
                Log.w(access$100, sb.toString(), th);
                return null;
            }
        }

        public String toString() {
            return String.format("PackageDrawableIcon[pkg=%s,id=0x%08x]", new Object[]{this.mPackage, Integer.valueOf(this.mResId)});
        }
    }

    public Intent getLongClickIntent() {
        return null;
    }

    public int getMetricsCategory() {
        return 121;
    }

    public void handleSetListening(boolean z) {
    }

    private IntentTile(QSHost qSHost, String str) {
        super(qSHost);
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(str));
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public static IntentTile create(QSHost qSHost, String str) {
        if (str == null || !str.startsWith("intent(") || !str.endsWith(")")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Bad intent tile spec: ");
            sb.append(str);
            throw new IllegalArgumentException(sb.toString());
        }
        String substring = str.substring(7, str.length() - 1);
        if (!substring.isEmpty()) {
            return new IntentTile(qSHost, substring);
        }
        throw new IllegalArgumentException("Empty intent tile spec action");
    }

    public State newTileState() {
        return new State();
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        super.handleUserSwitch(i);
        this.mCurrentUserId = i;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        sendIntent("click", this.mOnClick, this.mOnClickUri);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        sendIntent("long-click", this.mOnLongClick, this.mOnLongClickUri);
    }

    private void sendIntent(String str, PendingIntent pendingIntent, String str2) {
        if (pendingIntent != null) {
            try {
                if (pendingIntent.isActivity()) {
                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(pendingIntent);
                } else {
                    pendingIntent.send();
                }
            } catch (Throwable th) {
                String str3 = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Error sending ");
                sb.append(str);
                sb.append(" intent");
                Log.w(str3, sb.toString(), th);
            }
        } else if (str2 != null) {
            this.mContext.sendBroadcastAsUser(Intent.parseUri(str2, 1), new UserHandle(this.mCurrentUserId));
        }
    }

    public CharSequence getTileLabel() {
        return getState().label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(State state, Object obj) {
        Intent intent = (Intent) obj;
        if (intent == null) {
            intent = this.mLastIntent;
            if (intent == null) {
                return;
            }
        }
        this.mLastIntent = intent;
        state.contentDescription = intent.getStringExtra("contentDescription");
        state.label = intent.getStringExtra("label");
        state.icon = null;
        byte[] byteArrayExtra = intent.getByteArrayExtra("iconBitmap");
        if (byteArrayExtra != null) {
            try {
                state.icon = new BytesIcon(byteArrayExtra);
            } catch (Throwable th) {
                String str = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Error loading icon bitmap, length ");
                sb.append(byteArrayExtra.length);
                Log.w(str, sb.toString(), th);
            }
        } else {
            int intExtra = intent.getIntExtra("iconId", 0);
            if (intExtra != 0) {
                String stringExtra = intent.getStringExtra("iconPackage");
                if (!TextUtils.isEmpty(stringExtra)) {
                    state.icon = new PackageDrawableIcon(stringExtra, intExtra);
                } else {
                    state.icon = ResourceIcon.get(intExtra);
                }
            }
        }
        this.mOnClick = (PendingIntent) intent.getParcelableExtra("onClick");
        this.mOnClickUri = intent.getStringExtra("onClickUri");
        this.mOnLongClick = (PendingIntent) intent.getParcelableExtra("onLongClick");
        this.mOnLongClickUri = intent.getStringExtra("onLongClickUri");
        this.mIntentPackage = intent.getStringExtra("package");
        String str2 = this.mIntentPackage;
        if (str2 == null) {
            str2 = "";
        }
        this.mIntentPackage = str2;
    }
}
