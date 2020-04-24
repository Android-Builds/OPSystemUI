package com.android.systemui.p007qs.tileimpl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Prefs;
import com.android.systemui.p007qs.PagedTileLayout.TilePage;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Callback;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.plugins.p006qs.QSTile.State;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.oneplus.util.ThemeColorUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* renamed from: com.android.systemui.qs.tileimpl.QSTileImpl */
public abstract class QSTileImpl<TState extends State> implements QSTile, LifecycleOwner, Dumpable {
    protected static final Object ARG_SHOW_TRANSIENT_ENABLING = new Object();
    protected static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public final String MDM_TAG;
    protected String TAG = this.MDM_TAG;
    private boolean mAnnounceNextStateChange;
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    /* access modifiers changed from: protected */
    public final Context mContext;
    /* access modifiers changed from: private */
    public EnforcedAdmin mEnforcedAdmin;
    /* access modifiers changed from: protected */
    public C1027H mHandler = new C1027H<>((Looper) Dependency.get(Dependency.BG_LOOPER));
    protected final QSHost mHost;
    private int mIsFullQs;
    private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
    private final ArraySet<Object> mListeners = new ArraySet<>();
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private boolean mShowingDetail;
    private final Object mStaleListener = new Object();
    protected TState mState = newTileState();
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private String mTileSpec;
    private TState mTmpState = newTileState();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());

    /* renamed from: com.android.systemui.qs.tileimpl.QSTileImpl$DrawableIcon */
    public static class DrawableIcon extends Icon {
        protected final Drawable mDrawable;
        protected final Drawable mInvisibleDrawable;

        public DrawableIcon(Drawable drawable) {
            this.mDrawable = drawable;
            this.mInvisibleDrawable = drawable.getConstantState().newDrawable();
        }

        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }

        public Drawable getInvisibleDrawable(Context context) {
            return this.mInvisibleDrawable;
        }
    }

    /* renamed from: com.android.systemui.qs.tileimpl.QSTileImpl$H */
    protected final class C1027H extends Handler {
        @VisibleForTesting
        protected C1027H(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: Removed duplicated region for block: B:78:0x0191  */
        /* JADX WARNING: Removed duplicated region for block: B:82:0x01ae  */
        /* JADX WARNING: Removed duplicated region for block: B:83:0x01b5  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r10) {
            /*
                r9 = this;
                long r0 = android.os.SystemClock.elapsedRealtime()
                r2 = 0
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r4 = 1
                if (r3 != r4) goto L_0x0017
                java.lang.String r2 = "handleAddCallback"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.Object r10 = r10.obj     // Catch:{ all -> 0x0169 }
                com.android.systemui.plugins.qs.QSTile$Callback r10 = (com.android.systemui.plugins.p006qs.QSTile.Callback) r10     // Catch:{ all -> 0x0169 }
                r3.handleAddCallback(r10)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0017:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 11
                if (r3 != r5) goto L_0x0026
                java.lang.String r2 = "handleRemoveCallbacks"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleRemoveCallbacks()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0026:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 12
                if (r3 != r5) goto L_0x0039
                java.lang.String r2 = "handleRemoveCallback"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.Object r10 = r10.obj     // Catch:{ all -> 0x0169 }
                com.android.systemui.plugins.qs.QSTile$Callback r10 = (com.android.systemui.plugins.p006qs.QSTile.Callback) r10     // Catch:{ all -> 0x0169 }
                r3.handleRemoveCallback(r10)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0039:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 2
                java.lang.String r6 = "1"
                r7 = 0
                if (r3 != r5) goto L_0x0086
                java.lang.String r2 = "handleClick"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = r10.MDM_TAG     // Catch:{ all -> 0x0169 }
                java.lang.String r3 = "full"
                com.oneplus.systemui.util.OpMdmLogger.logQsTile(r10, r3, r6)     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = "click_tile"
                com.oneplus.systemui.util.OpMdmLogger.logQsPanel(r10)     // Catch:{ all -> 0x0169 }
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                TState r10 = r10.mState     // Catch:{ all -> 0x0169 }
                boolean r10 = r10.disabledByPolicy     // Catch:{ all -> 0x0169 }
                if (r10 == 0) goto L_0x007f
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = r10.TAG     // Catch:{ all -> 0x0169 }
                java.lang.String r3 = "disabledByPolicy"
                android.util.Log.d(r10, r3)     // Catch:{ all -> 0x0169 }
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                android.content.Context r10 = r10.mContext     // Catch:{ all -> 0x0169 }
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                com.android.settingslib.RestrictedLockUtils$EnforcedAdmin r3 = r3.mEnforcedAdmin     // Catch:{ all -> 0x0169 }
                android.content.Intent r10 = com.android.settingslib.RestrictedLockUtils.getShowAdminSupportDetailsIntent(r10, r3)     // Catch:{ all -> 0x0169 }
                java.lang.Class<com.android.systemui.plugins.ActivityStarter> r3 = com.android.systemui.plugins.ActivityStarter.class
                java.lang.Object r3 = com.android.systemui.Dependency.get(r3)     // Catch:{ all -> 0x0169 }
                com.android.systemui.plugins.ActivityStarter r3 = (com.android.systemui.plugins.ActivityStarter) r3     // Catch:{ all -> 0x0169 }
                r3.postStartActivityDismissingKeyguard(r10, r7)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x007f:
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleClick()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0086:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 3
                if (r3 != r5) goto L_0x009f
                java.lang.String r2 = "handleSecondaryClick"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = r10.MDM_TAG     // Catch:{ all -> 0x0169 }
                java.lang.String r3 = "half"
                com.oneplus.systemui.util.OpMdmLogger.logQsTile(r10, r3, r6)     // Catch:{ all -> 0x0169 }
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleSecondaryClick()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x009f:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 4
                if (r3 != r5) goto L_0x00b8
                java.lang.String r2 = "handleLongClick"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = r10.MDM_TAG     // Catch:{ all -> 0x0169 }
                java.lang.String r3 = "long"
                com.oneplus.systemui.util.OpMdmLogger.logQsTile(r10, r3, r6)     // Catch:{ all -> 0x0169 }
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleLongClick()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x00b8:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 5
                if (r3 != r5) goto L_0x00c8
                java.lang.String r2 = "handleRefreshState"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.Object r10 = r10.obj     // Catch:{ all -> 0x0169 }
                r3.handleRefreshState(r10)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x00c8:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 6
                if (r3 != r5) goto L_0x00dc
                java.lang.String r2 = "handleShowDetail"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                int r10 = r10.arg1     // Catch:{ all -> 0x0169 }
                if (r10 == 0) goto L_0x00d6
                goto L_0x00d7
            L_0x00d6:
                r4 = r7
            L_0x00d7:
                r3.handleShowDetail(r4)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x00dc:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 7
                if (r3 != r5) goto L_0x00ec
                java.lang.String r2 = "handleUserSwitch"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                int r10 = r10.arg1     // Catch:{ all -> 0x0169 }
                r3.handleUserSwitch(r10)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x00ec:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 8
                if (r3 != r5) goto L_0x010a
                java.lang.String r3 = "handleToggleStateChanged"
                com.android.systemui.qs.tileimpl.QSTileImpl r5 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0107 }
                int r10 = r10.arg1     // Catch:{ all -> 0x0107 }
                if (r10 == 0) goto L_0x00fb
                goto L_0x00fc
            L_0x00fb:
                r4 = r7
            L_0x00fc:
                r5.handleToggleStateChanged(r4)     // Catch:{ all -> 0x0107 }
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0107 }
                r10.handleRefreshState(r2)     // Catch:{ all -> 0x0107 }
                r2 = r3
                goto L_0x0189
            L_0x0107:
                r10 = move-exception
                r2 = r3
                goto L_0x016a
            L_0x010a:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 9
                if (r3 != r5) goto L_0x011e
                java.lang.String r2 = "handleScanStateChanged"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                int r10 = r10.arg1     // Catch:{ all -> 0x0169 }
                if (r10 == 0) goto L_0x0119
                goto L_0x011a
            L_0x0119:
                r4 = r7
            L_0x011a:
                r3.handleScanStateChanged(r4)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x011e:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 10
                if (r3 != r5) goto L_0x012c
                java.lang.String r2 = "handleDestroy"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleDestroy()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x012c:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r5 = 13
                if (r3 != r5) goto L_0x0142
                java.lang.String r2 = "handleSetListeningInternal"
                com.android.systemui.qs.tileimpl.QSTileImpl r3 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                java.lang.Object r5 = r10.obj     // Catch:{ all -> 0x0169 }
                int r10 = r10.arg1     // Catch:{ all -> 0x0169 }
                if (r10 == 0) goto L_0x013d
                goto L_0x013e
            L_0x013d:
                r4 = r7
            L_0x013e:
                r3.handleSetListeningInternal(r5, r4)     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0142:
                int r3 = r10.what     // Catch:{ all -> 0x0169 }
                r4 = 14
                if (r3 != r4) goto L_0x0150
                java.lang.String r2 = "handleStale"
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this     // Catch:{ all -> 0x0169 }
                r10.handleStale()     // Catch:{ all -> 0x0169 }
                goto L_0x0189
            L_0x0150:
                java.lang.IllegalArgumentException r3 = new java.lang.IllegalArgumentException     // Catch:{ all -> 0x0169 }
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0169 }
                r4.<init>()     // Catch:{ all -> 0x0169 }
                java.lang.String r5 = "Unknown msg: "
                r4.append(r5)     // Catch:{ all -> 0x0169 }
                int r10 = r10.what     // Catch:{ all -> 0x0169 }
                r4.append(r10)     // Catch:{ all -> 0x0169 }
                java.lang.String r10 = r4.toString()     // Catch:{ all -> 0x0169 }
                r3.<init>(r10)     // Catch:{ all -> 0x0169 }
                throw r3     // Catch:{ all -> 0x0169 }
            L_0x0169:
                r10 = move-exception
            L_0x016a:
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Error in "
                r3.append(r4)
                r3.append(r2)
                java.lang.String r3 = r3.toString()
                com.android.systemui.qs.tileimpl.QSTileImpl r4 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this
                java.lang.String r4 = r4.TAG
                android.util.Log.w(r4, r3, r10)
                com.android.systemui.qs.tileimpl.QSTileImpl r4 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this
                com.android.systemui.qs.QSHost r4 = r4.mHost
                r4.warn(r3, r10)
            L_0x0189:
                long r3 = android.os.SystemClock.elapsedRealtime()
                boolean r10 = android.os.Build.DEBUG_ONEPLUS
                if (r10 != 0) goto L_0x0199
                long r5 = r3 - r0
                r7 = 1000(0x3e8, double:4.94E-321)
                int r10 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1))
                if (r10 < 0) goto L_0x01d7
            L_0x0199:
                com.android.systemui.qs.tileimpl.QSTileImpl r10 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this
                java.lang.String r10 = r10.TAG
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "state="
                r5.append(r6)
                com.android.systemui.qs.tileimpl.QSTileImpl r9 = com.android.systemui.p007qs.tileimpl.QSTileImpl.this
                TState r9 = r9.mState
                if (r9 == 0) goto L_0x01b5
                int r9 = r9.state
                java.lang.Integer r9 = java.lang.Integer.valueOf(r9)
                goto L_0x01b7
            L_0x01b5:
                java.lang.String r9 = "null"
            L_0x01b7:
                r5.append(r9)
                java.lang.String r9 = ", Time cost in handleMessage: name="
                r5.append(r9)
                r5.append(r2)
                java.lang.String r9 = ", time="
                r5.append(r9)
                long r3 = r3 - r0
                r5.append(r3)
                java.lang.String r9 = " ms"
                r5.append(r9)
                java.lang.String r9 = r5.toString()
                android.util.Log.d(r10, r9)
            L_0x01d7:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.tileimpl.QSTileImpl.C1027H.handleMessage(android.os.Message):void");
        }

        public void removeAllMessages() {
            for (int i = 1; i <= 14; i++) {
                removeMessages(i);
            }
        }
    }

    /* renamed from: com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon */
    public static class ResourceIcon extends Icon {
        private static final SparseArray<Icon> ICONS = new SparseArray<>();
        protected final int mResId;

        private ResourceIcon(int i) {
            this.mResId = i;
        }

        public static synchronized Icon get(int i) {
            Icon icon;
            synchronized (ResourceIcon.class) {
                icon = (Icon) ICONS.get(i);
                if (icon == null) {
                    icon = new ResourceIcon(i);
                    ICONS.put(i, icon);
                }
            }
            return icon;
        }

        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public Drawable getInvisibleDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public boolean equals(Object obj) {
            return (obj instanceof ResourceIcon) && ((ResourceIcon) obj).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", new Object[]{Integer.valueOf(this.mResId)});
        }
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        return null;
    }

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public abstract Intent getLongClickIntent();

    public abstract int getMetricsCategory();

    /* access modifiers changed from: protected */
    public long getStaleTimeout() {
        return 600000;
    }

    /* access modifiers changed from: protected */
    public abstract void handleClick();

    /* access modifiers changed from: protected */
    public abstract void handleSetListening(boolean z);

    /* access modifiers changed from: protected */
    public abstract void handleUpdateState(TState tstate, Object obj);

    public boolean isAvailable() {
        return true;
    }

    public abstract TState newTileState();

    public void setDetailListening(boolean z) {
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected QSTileImpl(QSHost qSHost) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tile.");
        sb.append(getClass().getSimpleName());
        this.MDM_TAG = sb.toString();
        try {
            String[] split = toString().split("@");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(this.TAG);
            sb2.append("(");
            sb2.append(split[1]);
            sb2.append(")");
            this.TAG = sb2.toString();
        } catch (Exception unused) {
        }
        Log.d(this.TAG, "init constructor");
        this.mHost = qSHost;
        this.mContext = qSHost.getContext();
    }

    public Lifecycle getLifecycle() {
        return this.mLifecycle;
    }

    public void setListening(Object obj, boolean z) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(13, z ? 1 : 0, 0, obj).sendToTarget();
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void handleStale() {
        setListening(this.mStaleListener, true);
    }

    public String getTileSpec() {
        return this.mTileSpec;
    }

    public void setTileSpec(String str) {
        this.mTileSpec = str;
    }

    public QSHost getHost() {
        return this.mHost;
    }

    public QSIconView createTileView(Context context) {
        return new QSIconViewImpl(context);
    }

    public void addCallback(Callback callback) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(1, callback).sendToTarget();
        }
    }

    public void removeCallback(Callback callback) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(12, callback).sendToTarget();
        }
    }

    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void click() {
        this.mMetricsLogger.write(populate(new LogMaker(925).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        C1027H h = this.mHandler;
        if (h != null) {
            h.sendEmptyMessage(2);
        }
    }

    public void secondaryClick() {
        this.mMetricsLogger.write(populate(new LogMaker(926).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        C1027H h = this.mHandler;
        if (h != null) {
            h.sendEmptyMessage(3);
        }
    }

    public void longClick() {
        this.mMetricsLogger.write(populate(new LogMaker(366).setType(4).addTaggedData(1592, Integer.valueOf(this.mStatusBarStateController.getState()))));
        C1027H h = this.mHandler;
        if (h != null) {
            h.sendEmptyMessage(4);
        }
        Prefs.putInt(this.mContext, "QsLongPressTooltipShownCount", 2);
    }

    public LogMaker populate(LogMaker logMaker) {
        TState tstate = this.mState;
        if (tstate instanceof BooleanState) {
            logMaker.addTaggedData(928, Integer.valueOf(((BooleanState) tstate).value ? 1 : 0));
        }
        return logMaker.setSubtype(getMetricsCategory()).addTaggedData(1593, Integer.valueOf(this.mIsFullQs)).addTaggedData(927, Integer.valueOf(this.mHost.indexOf(this.mTileSpec)));
    }

    public void showDetail(boolean z) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(6, z ? 1 : 0, 0).sendToTarget();
        }
    }

    public void refreshState() {
        refreshState(null);
    }

    /* access modifiers changed from: protected */
    public final void refreshState(Object obj) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(5, obj).sendToTarget();
        }
    }

    public void userSwitch(int i) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(7, i, 0).sendToTarget();
        }
    }

    public void fireToggleStateChanged(boolean z) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
        }
    }

    public void fireScanStateChanged(boolean z) {
        C1027H h = this.mHandler;
        if (h != null) {
            h.obtainMessage(9, z ? 1 : 0, 0).sendToTarget();
        }
    }

    public void destroy() {
        C1027H h = this.mHandler;
        if (h != null) {
            h.sendEmptyMessage(10);
        }
    }

    public TState getState() {
        return this.mState;
    }

    /* access modifiers changed from: private */
    public void handleAddCallback(Callback callback) {
        this.mCallbacks.add(callback);
        callback.onStateChanged(this.mState);
    }

    /* access modifiers changed from: private */
    public void handleRemoveCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    /* access modifiers changed from: private */
    public void handleRemoveCallbacks() {
        this.mCallbacks.clear();
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(getLongClickIntent(), 0);
    }

    /* access modifiers changed from: protected */
    public void handleRefreshState(Object obj) {
        handleUpdateState(this.mTmpState, obj);
        if (this.mTmpState.copyTo(this.mState)) {
            handleStateChanged();
        }
        C1027H h = this.mHandler;
        if (h != null) {
            h.removeMessages(14);
        }
        C1027H h2 = this.mHandler;
        if (h2 != null) {
            h2.sendEmptyMessageDelayed(14, getStaleTimeout());
        }
        setListening(this.mStaleListener, false);
    }

    private void handleStateChanged() {
        boolean shouldAnnouncementBeDelayed = shouldAnnouncementBeDelayed();
        boolean z = false;
        if (this.mCallbacks.size() != 0) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                ((Callback) this.mCallbacks.get(i)).onStateChanged(this.mState);
            }
            if (this.mAnnounceNextStateChange && !shouldAnnouncementBeDelayed) {
                String composeChangeAnnouncement = composeChangeAnnouncement();
                if (composeChangeAnnouncement != null) {
                    ((Callback) this.mCallbacks.get(0)).onAnnouncementRequested(composeChangeAnnouncement);
                }
            }
        }
        if (this.mAnnounceNextStateChange && shouldAnnouncementBeDelayed) {
            z = true;
        }
        this.mAnnounceNextStateChange = z;
    }

    /* access modifiers changed from: private */
    public void handleShowDetail(boolean z) {
        this.mShowingDetail = z;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callback) this.mCallbacks.get(i)).onShowDetail(z);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isShowingDetail() {
        return this.mShowingDetail;
    }

    /* access modifiers changed from: private */
    public void handleToggleStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callback) this.mCallbacks.get(i)).onToggleStateChanged(z);
        }
    }

    /* access modifiers changed from: private */
    public void handleScanStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callback) this.mCallbacks.get(i)).onScanStateChanged(z);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        handleRefreshState(null);
    }

    /* access modifiers changed from: private */
    public void handleSetListeningInternal(Object obj, boolean z) {
        if (z) {
            if (this.mListeners.add(obj) && this.mListeners.size() == 1) {
                if (DEBUG) {
                    Log.d(this.TAG, "handleSetListening true");
                }
                this.mLifecycle.markState(Lifecycle.State.RESUMED);
                handleSetListening(z);
                refreshState();
            }
        } else if (this.mListeners.remove(obj) && this.mListeners.size() == 0) {
            if (DEBUG) {
                Log.d(this.TAG, "handleSetListening false");
            }
            this.mLifecycle.markState(Lifecycle.State.DESTROYED);
            handleSetListening(z);
        }
        updateIsFullQs();
    }

    private void updateIsFullQs() {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            if (TilePage.class.equals(it.next().getClass())) {
                this.mIsFullQs = 1;
                return;
            }
        }
        this.mIsFullQs = 0;
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        if (this.mListeners.size() != 0) {
            handleSetListening(false);
        }
        this.mCallbacks.clear();
        this.mHandler.removeAllMessages();
        this.mHandler = null;
    }

    /* access modifiers changed from: protected */
    public void checkIfRestrictionEnforcedByAdminOnly(State state, String str) {
        EnforcedAdmin checkIfRestrictionEnforced = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this.mContext, str, ActivityManager.getCurrentUser());
        if (checkIfRestrictionEnforced == null || RestrictedLockUtilsInternal.hasBaseUserRestriction(this.mContext, str, ActivityManager.getCurrentUser())) {
            state.disabledByPolicy = false;
            this.mEnforcedAdmin = null;
            return;
        }
        state.disabledByPolicy = true;
        this.mEnforcedAdmin = checkIfRestrictionEnforced;
    }

    public static int getColorForState(Context context, int i) {
        int i2;
        if (i == 0) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_DISABLE);
        }
        if (i == 1) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_OFF);
        }
        if (i != 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid state ");
            sb.append(i);
            Log.e("QSTile", sb.toString());
            return 0;
        }
        if (ThemeColorUtils.getCurrentTheme() == 2) {
            i2 = -16777216;
        } else {
            i2 = Color.parseColor("#FFF5F5F5");
        }
        return i2;
    }

    public static int getOPColorForState(int i) {
        int i2;
        if (i == 0) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_DISABLE);
        }
        if (i == 1) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_OFF);
        }
        if (ThemeColorUtils.getCurrentTheme() == 2) {
            i2 = -1;
        } else {
            i2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        }
        return i2;
    }

    public static int getCircleColorForState(int i) {
        int i2;
        if (i == 0) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_CIRCLE_DISABLE);
        }
        if (i == 1) {
            return ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_CIRCLE_OFF);
        }
        if (ThemeColorUtils.getCurrentTheme() == 2) {
            i2 = -1;
        } else {
            i2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        }
        return i2;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(":");
        printWriter.println(sb.toString());
        printWriter.print("    ");
        printWriter.println(getState().toString());
    }
}
