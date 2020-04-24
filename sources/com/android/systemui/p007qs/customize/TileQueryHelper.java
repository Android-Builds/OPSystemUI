package com.android.systemui.p007qs.customize;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Button;
import com.android.systemui.Dependency;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSTileHost;
import com.android.systemui.p007qs.external.CustomTile;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.DrawableIcon;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTile.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/* renamed from: com.android.systemui.qs.customize.TileQueryHelper */
public class TileQueryHelper {
    private final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private ArrayList<QSTile> mAllTiles = new ArrayList<>();
    private final Handler mBgHandler;
    private final Context mContext;
    private boolean mFinished;
    private final TileStateListener mListener;
    private final Handler mMainHandler;
    private final ArraySet<String> mSpecs = new ArraySet<>();
    private final ArrayList<TileInfo> mTiles = new ArrayList<>();

    /* renamed from: com.android.systemui.qs.customize.TileQueryHelper$TileInfo */
    public static class TileInfo {
        public boolean isSystem;
        public boolean isVisible = true;
        public String spec;
        public State state;
    }

    /* renamed from: com.android.systemui.qs.customize.TileQueryHelper$TileStateListener */
    public interface TileStateListener {
        void onTilesChanged(List<TileInfo> list);
    }

    public TileQueryHelper(Context context, TileStateListener tileStateListener) {
        if (this.DEBUG) {
            Log.i("TileQueryHelper", "new TileQueryHelper");
        }
        this.mContext = context;
        this.mListener = tileStateListener;
        this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mMainHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
    }

    public void queryTiles(QSTileHost qSTileHost) {
        this.mTiles.clear();
        this.mSpecs.clear();
        this.mAllTiles.clear();
        this.mFinished = false;
        addCurrentAndStockTiles(qSTileHost);
        addPackageTiles(qSTileHost);
    }

    public boolean isFinished() {
        return this.mFinished;
    }

    private void addCurrentAndStockTiles(QSTileHost qSTileHost) {
        String[] split;
        String string = this.mContext.getString(R$string.quick_settings_tiles_stock);
        String string2 = Secure.getString(this.mContext.getContentResolver(), "sysui_qs_tiles");
        ArrayList arrayList = new ArrayList();
        String str = ",";
        if (string2 != null) {
            arrayList.addAll(Arrays.asList(string2.split(str)));
        } else {
            string2 = "";
        }
        for (String str2 : string.split(str)) {
            boolean z = false;
            for (String equals : string2.split(str)) {
                if (equals.equals(str2)) {
                    z = true;
                }
            }
            if (!z) {
                arrayList.add(str2);
            }
        }
        if (Build.IS_DEBUGGABLE) {
            String str3 = "dbg:mem";
            if (!string2.contains(str3)) {
                arrayList.add(str3);
            }
        }
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            String str4 = (String) it.next();
            QSTile createTile = qSTileHost.createTile(str4);
            if (createTile != null) {
                if (!createTile.isAvailable()) {
                    createTile.destroy();
                } else if (!qSTileHost.isNeedToHide(str4)) {
                    createTile.setListening(this, true);
                    createTile.refreshState();
                    createTile.setListening(this, false);
                    createTile.setTileSpec(str4);
                    arrayList2.add(createTile);
                    this.mAllTiles.add(createTile);
                }
            }
        }
        this.mBgHandler.post(new Runnable(arrayList2) {
            private final /* synthetic */ ArrayList f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TileQueryHelper.this.lambda$addCurrentAndStockTiles$0$TileQueryHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$addCurrentAndStockTiles$0$TileQueryHelper(ArrayList arrayList) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            QSTile qSTile = (QSTile) it.next();
            State copy = qSTile.getState().copy();
            copy.label = qSTile.getTileLabel();
            qSTile.destroy();
            addTile(qSTile.getTileSpec(), null, copy, true);
        }
        notifyTilesChanged(false);
    }

    private void addPackageTiles(QSTileHost qSTileHost) {
        this.mBgHandler.post(new Runnable(qSTileHost) {
            private final /* synthetic */ QSTileHost f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TileQueryHelper.this.lambda$addPackageTiles$1$TileQueryHelper(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$addPackageTiles$1$TileQueryHelper(QSTileHost qSTileHost) {
        Collection tiles = qSTileHost.getTiles();
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> queryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent("android.service.quicksettings.action.QS_TILE"), 0, ActivityManager.getCurrentUser());
        String string = this.mContext.getString(R$string.quick_settings_tiles_stock);
        for (ResolveInfo resolveInfo : queryIntentServicesAsUser) {
            ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            if (!string.contains(componentName.flattenToString())) {
                CharSequence loadLabel = resolveInfo.serviceInfo.applicationInfo.loadLabel(packageManager);
                String spec = CustomTile.toSpec(componentName);
                State state = getState(tiles, spec);
                if (!qSTileHost.isNeedToHide(spec)) {
                    String str = "spec=";
                    String str2 = "TileQueryHelper";
                    if (state != null) {
                        if (Build.DEBUG_ONEPLUS) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(str);
                            sb.append(spec);
                            sb.append(", addPkgTile");
                            Log.d(str2, sb.toString());
                        }
                        addTile(spec, loadLabel, state, false);
                    } else if (resolveInfo.serviceInfo.icon != 0 || resolveInfo.serviceInfo.applicationInfo.icon != 0) {
                        if (Build.DEBUG_ONEPLUS) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(str);
                            sb2.append(spec);
                            sb2.append(", serviceIcon=");
                            sb2.append(resolveInfo.serviceInfo.icon);
                            sb2.append(", appIcon=");
                            sb2.append(resolveInfo.serviceInfo.applicationInfo.icon);
                            Log.d(str2, sb2.toString());
                        }
                        Drawable loadIcon = resolveInfo.serviceInfo.loadIcon(packageManager);
                        if ("android.permission.BIND_QUICK_SETTINGS_TILE".equals(resolveInfo.serviceInfo.permission)) {
                            if (loadIcon != null) {
                                loadIcon.mutate();
                                loadIcon.setTint(this.mContext.getColor(17170443));
                                CharSequence loadLabel2 = resolveInfo.serviceInfo.loadLabel(packageManager);
                                if (Build.DEBUG_ONEPLUS) {
                                    StringBuilder sb3 = new StringBuilder();
                                    sb3.append(str);
                                    sb3.append(spec);
                                    sb3.append(", calling create. icon=");
                                    sb3.append(loadIcon);
                                    Log.d(str2, sb3.toString());
                                }
                                createStateAndAddTile(spec, loadIcon, loadLabel2 != null ? loadLabel2.toString() : "null", loadLabel);
                            } else if (Build.DEBUG_ONEPLUS) {
                                StringBuilder sb4 = new StringBuilder();
                                sb4.append(str);
                                sb4.append(spec);
                                sb4.append(", icon is null, skip.");
                                Log.d(str2, sb4.toString());
                            }
                        }
                    } else if (Build.DEBUG_ONEPLUS) {
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append(str);
                        sb5.append(spec);
                        sb5.append(", icons are 0. skip.");
                        Log.d(str2, sb5.toString());
                    }
                }
            }
        }
        notifyTilesChanged(true);
    }

    private void notifyTilesChanged(boolean z) {
        this.mMainHandler.post(new Runnable(new ArrayList(this.mTiles), z) {
            private final /* synthetic */ ArrayList f$1;
            private final /* synthetic */ boolean f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                TileQueryHelper.this.lambda$notifyTilesChanged$2$TileQueryHelper(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$notifyTilesChanged$2$TileQueryHelper(ArrayList arrayList, boolean z) {
        this.mListener.onTilesChanged(arrayList);
        this.mFinished = z;
        if (this.mFinished) {
            TileStateListener tileStateListener = this.mListener;
            if (tileStateListener instanceof QSEditPageManager) {
                ((QSEditPageManager) tileStateListener).recalcSpecs();
            }
        }
    }

    private State getState(Collection<QSTile> collection, String str) {
        for (QSTile qSTile : collection) {
            if (str.equals(qSTile.getTileSpec())) {
                return qSTile.getState().copy();
            }
        }
        return null;
    }

    private void addTile(String str, CharSequence charSequence, State state, boolean z) {
        if (!this.mSpecs.contains(str)) {
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("addTile: spec=");
                sb.append(str);
                sb.append(", isSystem=");
                sb.append(z);
                Log.d("TileQueryHelper", sb.toString());
            }
            TileInfo tileInfo = new TileInfo();
            tileInfo.state = state;
            State state2 = tileInfo.state;
            state2.dualTarget = false;
            state2.expandedAccessibilityClassName = Button.class.getName();
            tileInfo.spec = str;
            State state3 = tileInfo.state;
            if (z || TextUtils.equals(state.label, charSequence)) {
                charSequence = null;
            }
            state3.secondaryLabel = charSequence;
            tileInfo.isSystem = z;
            this.mTiles.add(tileInfo);
            this.mSpecs.add(str);
        }
    }

    private void createStateAndAddTile(String str, Drawable drawable, CharSequence charSequence, CharSequence charSequence2) {
        State state = new State();
        state.state = 1;
        state.label = charSequence;
        state.contentDescription = charSequence;
        state.icon = new DrawableIcon(drawable);
        addTile(str, charSequence2, state, false);
    }

    public void destroyTiles() {
        ArrayList<QSTile> arrayList = this.mAllTiles;
        if (arrayList != null) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                ((QSTile) it.next()).destroy();
            }
            this.mAllTiles.clear();
        }
    }

    public void recalcEditPage() {
        TileStateListener tileStateListener = this.mListener;
        if (tileStateListener != null && this.mFinished && (tileStateListener instanceof QSEditPageManager)) {
            ((QSEditPageManager) tileStateListener).recalcEditPage();
        }
    }
}
