package com.android.systemui.p007qs;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.service.quicksettings.Tile;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.Dumpable;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.p007qs.QSHost.Callback;
import com.android.systemui.p007qs.external.CustomTile;
import com.android.systemui.p007qs.external.TileLifecycleManager;
import com.android.systemui.p007qs.external.TileServices;
import com.android.systemui.p007qs.tileimpl.QSFactoryImpl;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.p006qs.QSFactory;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTileView;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.phone.AutoTileManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.QSTileHost */
public class QSTileHost implements QSHost, Tunable, PluginListener<QSFactory>, Dumpable {
    private static final boolean DEBUG = Log.isLoggable("QSTileHost", 3);
    /* access modifiers changed from: private */
    public static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private AutoTileManager mAutoTiles;
    private final List<Callback> mCallbacks = new ArrayList();
    /* access modifiers changed from: private */
    public final Context mContext;
    private int mCurrentUser;
    private final DumpController mDumpController;
    private final StatusBarIconController mIconController;
    private final OperatorCustom mOperatorCustom;
    private final PluginManager mPluginManager;
    private final ArrayList<QSFactory> mQsFactories = new ArrayList<>();
    private final TileServices mServices;
    private StatusBar mStatusBar;
    protected final ArrayList<String> mTileSpecs = new ArrayList<>();
    private final LinkedHashMap<String, QSTile> mTiles = new LinkedHashMap<>();
    private final TunerService mTunerService;

    /* renamed from: com.android.systemui.qs.QSTileHost$OperatorCustom */
    private class OperatorCustom {
        /* access modifiers changed from: private */
        public List<String> mHideList;
        private HideTileBroadcastReceiver mHideTileReceiver;
        /* access modifiers changed from: private */
        public boolean mHospotDisableByOperator;
        private final HotspotController.Callback mHotspotCallback;
        /* access modifiers changed from: private */
        public int recordPosition;

        /* renamed from: com.android.systemui.qs.QSTileHost$OperatorCustom$HideTileBroadcastReceiver */
        public class HideTileBroadcastReceiver extends BroadcastReceiver {
            public HideTileBroadcastReceiver() {
            }

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if ("com.oneplus.systemui.qs.hide_tile".equals(intent.getAction())) {
                        String stringExtra = intent.getStringExtra("tile");
                        boolean booleanExtra = intent.getBooleanExtra("hide", false);
                        int intExtra = intent.getIntExtra("position", 100);
                        if (!TextUtils.isEmpty(stringExtra)) {
                            String trim = stringExtra.trim();
                            List loadTileSpecs = QSTileHost.loadTileSpecs(QSTileHost.this.mContext, Secure.getStringForUser(QSTileHost.this.mContext.getContentResolver(), "sysui_qs_tiles", ActivityManager.getCurrentUser()));
                            if (QSTileHost.DEBUG_ONEPLUS) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("HideTileReceiver: tile=");
                                sb.append(trim);
                                sb.append(", hide=");
                                sb.append(booleanExtra);
                                sb.append(", pos=");
                                sb.append(intExtra);
                                String str = "QSTileHost";
                                Log.d(str, sb.toString());
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("HideTileReceiver: list=");
                                sb2.append(QSTileHost.this.mTileSpecs);
                                Log.d(str, sb2.toString());
                                StringBuilder sb3 = new StringBuilder();
                                sb3.append("HideTileReceiver: hide=");
                                sb3.append(OperatorCustom.this.mHideList);
                                Log.d(str, sb3.toString());
                            }
                            if (booleanExtra) {
                                OperatorCustom.this.mHideList.add(trim);
                                if (loadTileSpecs.contains(trim)) {
                                    OperatorCustom.this.hideTile(trim, true, intExtra);
                                }
                            } else {
                                if (OperatorCustom.this.mHideList.contains(trim)) {
                                    OperatorCustom.this.mHideList.remove(trim);
                                }
                                OperatorCustom.this.hideTile(trim, false, intExtra);
                            }
                            String str2 = "op_sysui_qs_tiles_hide";
                            Secure.putStringForUser(QSTileHost.this.mContext.getContentResolver(), str2, TextUtils.join(",", OperatorCustom.this.mHideList), ActivityManager.getCurrentUser());
                        }
                    }
                }
            }
        }

        private OperatorCustom() {
            this.mHospotDisableByOperator = false;
            this.recordPosition = -2;
            this.mHideList = new ArrayList();
            this.mHotspotCallback = new HotspotController.Callback() {
                public void onHotspotChanged(boolean z, int i) {
                }

                public void onOperatorHotspotChanged(boolean z) {
                    String str = "QSTileHost";
                    if (QSTileHost.DEBUG_ONEPLUS) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("onOperatorHotspotChanged / disableByOperator:");
                        sb.append(z);
                        sb.append(" / recordPosition:");
                        sb.append(OperatorCustom.this.recordPosition);
                        Log.i(str, sb.toString());
                    }
                    String str2 = "hotspot";
                    if (OperatorCustom.this.recordPosition == -2) {
                        OperatorCustom operatorCustom = OperatorCustom.this;
                        operatorCustom.recordPosition = operatorCustom.existTile(str2);
                    }
                    OperatorCustom.this.mHospotDisableByOperator = z;
                    if (OperatorCustom.this.mHospotDisableByOperator) {
                        OperatorCustom operatorCustom2 = OperatorCustom.this;
                        operatorCustom2.recordPosition = operatorCustom2.existTile(str2);
                        QSTileHost.this.removeTile(str2);
                    } else if (OperatorCustom.this.mHospotDisableByOperator || OperatorCustom.this.recordPosition < 0) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("onOperatorHotspotChanged / else / disableByOperator:");
                        sb2.append(z);
                        sb2.append(" / existTile(HOTSPOT):");
                        sb2.append(OperatorCustom.this.existTile(str2));
                        Log.i(str, sb2.toString());
                    } else {
                        OperatorCustom operatorCustom3 = OperatorCustom.this;
                        operatorCustom3.addTileWithPosition(str2, operatorCustom3.recordPosition, false);
                    }
                }
            };
        }

        public void init() {
            String[] split;
            ((HotspotController) Dependency.get(HotspotController.class)).addCallback(this.mHotspotCallback);
            String stringForUser = Secure.getStringForUser(QSTileHost.this.mContext.getContentResolver(), "op_sysui_qs_tiles_hide", ActivityManager.getCurrentUser());
            if (QSTileHost.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("hideTiles=");
                sb.append(stringForUser);
                Log.d("QSTileHost", sb.toString());
            }
            this.mHideList.clear();
            if (!TextUtils.isEmpty(stringForUser)) {
                for (String str : stringForUser.split(",")) {
                    if (!TextUtils.isEmpty(str)) {
                        String trim = str.trim();
                        this.mHideList.add(trim);
                        hideTile(trim, true, -1);
                    }
                }
            }
            this.mHideTileReceiver = new HideTileBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.oneplus.systemui.qs.hide_tile");
            QSTileHost.this.mContext.registerReceiver(this.mHideTileReceiver, intentFilter, "android.permission.WRITE_SECURE_SETTINGS", null);
        }

        /* access modifiers changed from: private */
        public int existTile(String str) {
            return QSTileHost.loadTileSpecs(QSTileHost.this.mContext, Secure.getStringForUser(QSTileHost.this.mContext.getContentResolver(), "sysui_qs_tiles", ActivityManager.getCurrentUser())).indexOf(str);
        }

        /* access modifiers changed from: private */
        public void addTileWithPosition(String str, int i, boolean z) {
            String str2 = "sysui_qs_tiles";
            List loadTileSpecs = QSTileHost.loadTileSpecs(QSTileHost.this.mContext, Secure.getStringForUser(QSTileHost.this.mContext.getContentResolver(), str2, ActivityManager.getCurrentUser()));
            if (z || !loadTileSpecs.contains(str)) {
                loadTileSpecs.remove(str);
                if (QSTileHost.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("addTileWithPosition / position:");
                    sb.append(i);
                    sb.append(" / tileSpecs.size():");
                    sb.append(loadTileSpecs.size());
                    Log.i("QSTileHost", sb.toString());
                }
                if (i >= 0 && i < loadTileSpecs.size()) {
                    loadTileSpecs.add(i, str);
                } else if (i >= loadTileSpecs.size()) {
                    loadTileSpecs.add(str);
                }
                Secure.putStringForUser(QSTileHost.this.mContext.getContentResolver(), str2, TextUtils.join(",", loadTileSpecs), ActivityManager.getCurrentUser());
            }
        }

        /* access modifiers changed from: private */
        public void hideTile(String str, boolean z, int i) {
            if (z) {
                if (QSTileHost.this.mTileSpecs.contains(str)) {
                    ArrayList arrayList = new ArrayList(QSTileHost.this.mTileSpecs);
                    arrayList.remove(str);
                    QSTileHost qSTileHost = QSTileHost.this;
                    qSTileHost.changeTiles(qSTileHost.mTileSpecs, arrayList);
                }
            } else if (QSTileHost.this.mTileSpecs.size() <= 0) {
                addTileWithPosition(str, 0, true);
            } else {
                addTileWithPosition(str, i, true);
            }
        }
    }

    public void warn(String str, Throwable th) {
    }

    public QSTileHost(Context context, StatusBarIconController statusBarIconController, QSFactoryImpl qSFactoryImpl, Handler handler, Looper looper, PluginManager pluginManager, TunerService tunerService, Provider<AutoTileManager> provider, DumpController dumpController) {
        this.mIconController = statusBarIconController;
        this.mContext = context;
        this.mTunerService = tunerService;
        this.mPluginManager = pluginManager;
        this.mDumpController = dumpController;
        this.mServices = new TileServices(this, looper);
        qSFactoryImpl.setHost(this);
        this.mQsFactories.add(qSFactoryImpl);
        pluginManager.addPluginListener((PluginListener<T>) this, QSFactory.class, true);
        this.mDumpController.addListener(this);
        handler.post(new Runnable(tunerService, provider) {
            private final /* synthetic */ TunerService f$1;
            private final /* synthetic */ Provider f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                QSTileHost.this.lambda$new$0$QSTileHost(this.f$1, this.f$2);
            }
        });
        this.mOperatorCustom = new OperatorCustom();
        this.mOperatorCustom.init();
    }

    public /* synthetic */ void lambda$new$0$QSTileHost(TunerService tunerService, Provider provider) {
        tunerService.addTunable(this, "sysui_qs_tiles");
        this.mAutoTiles = (AutoTileManager) provider.get();
    }

    public StatusBarIconController getIconController() {
        return this.mIconController;
    }

    public void onPluginConnected(QSFactory qSFactory, Context context) {
        this.mQsFactories.add(0, qSFactory);
        String str = "sysui_qs_tiles";
        String value = this.mTunerService.getValue(str);
        onTuningChanged(str, "");
        onTuningChanged(str, value);
    }

    public void onPluginDisconnected(QSFactory qSFactory) {
        this.mQsFactories.remove(qSFactory);
        String str = "sysui_qs_tiles";
        String value = this.mTunerService.getValue(str);
        onTuningChanged(str, "");
        onTuningChanged(str, value);
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public Collection<QSTile> getTiles() {
        return this.mTiles.values();
    }

    public void collapsePanels() {
        if (this.mStatusBar == null) {
            this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        }
        this.mStatusBar.postAnimateCollapsePanels();
    }

    public void forceCollapsePanels() {
        if (this.mStatusBar == null) {
            this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        }
        this.mStatusBar.postAnimateForceCollapsePanels();
    }

    public void openPanels() {
        if (this.mStatusBar == null) {
            this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        }
        this.mStatusBar.postAnimateOpenPanels();
    }

    public Context getContext() {
        return this.mContext;
    }

    public TileServices getTileServices() {
        return this.mServices;
    }

    public int indexOf(String str) {
        return this.mTileSpecs.indexOf(str);
    }

    public void onTuningChanged(String str, String str2) {
        if ("sysui_qs_tiles".equals(str)) {
            String str3 = "QSTileHost";
            if (DEBUG) {
                Log.d(str3, "Recreating tiles");
            }
            if (str2 == null && UserManager.isDeviceInDemoMode(this.mContext)) {
                str2 = this.mContext.getResources().getString(R$string.quick_settings_tiles_retail_mode);
            }
            List<String> loadTileSpecs = loadTileSpecs(this.mContext, str2);
            int currentUser = ActivityManager.getCurrentUser();
            if (!loadTileSpecs.equals(this.mTileSpecs) || currentUser != this.mCurrentUser) {
                this.mTiles.entrySet().stream().filter(new Predicate(loadTileSpecs) {
                    private final /* synthetic */ List f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final boolean test(Object obj) {
                        return QSTileHost.lambda$onTuningChanged$2(this.f$0, (Entry) obj);
                    }
                }).forEach($$Lambda$QSTileHost$_TW3g9Ui2otBinO5ZHSBKxrVFI.INSTANCE);
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (String str4 : loadTileSpecs) {
                    QSTile qSTile = (QSTile) this.mTiles.get(str4);
                    if (!str4.startsWith("custom(")) {
                        str4 = str4.toLowerCase();
                    }
                    if (qSTile != null) {
                        boolean z = qSTile instanceof CustomTile;
                        if (!z || ((CustomTile) qSTile).getUser() == currentUser) {
                            if (qSTile.isAvailable()) {
                                if (DEBUG) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Adding ");
                                    sb.append(qSTile);
                                    Log.d(str3, sb.toString());
                                }
                                qSTile.removeCallbacks();
                                if (!z && this.mCurrentUser != currentUser) {
                                    qSTile.userSwitch(currentUser);
                                }
                                linkedHashMap.put(str4, qSTile);
                            } else {
                                qSTile.destroy();
                            }
                        }
                    }
                    if (DEBUG) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Creating tile: ");
                        sb2.append(str4);
                        Log.d(str3, sb2.toString());
                    }
                    if (qSTile != null) {
                        qSTile.destroy();
                    }
                    try {
                        QSTile createTile = createTile(str4);
                        if (createTile != null) {
                            if (createTile.isAvailable()) {
                                createTile.setTileSpec(str4);
                                linkedHashMap.put(str4, createTile);
                            } else {
                                createTile.destroy();
                            }
                        }
                    } catch (Throwable th) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("Error creating tile for spec: ");
                        sb3.append(str4);
                        Log.w(str3, sb3.toString(), th);
                    }
                }
                this.mCurrentUser = currentUser;
                ArrayList arrayList = new ArrayList(this.mTileSpecs);
                this.mTileSpecs.clear();
                this.mTileSpecs.addAll(loadTileSpecs);
                this.mTiles.clear();
                this.mTiles.putAll(linkedHashMap);
                if (!linkedHashMap.isEmpty() || loadTileSpecs.isEmpty()) {
                    for (int i = 0; i < this.mCallbacks.size(); i++) {
                        ((Callback) this.mCallbacks.get(i)).onTilesChanged();
                    }
                } else {
                    if (DEBUG) {
                        Log.d(str3, "No valid tiles on tuning changed. Setting to default.");
                    }
                    changeTiles(arrayList, loadTileSpecs(this.mContext, ""));
                }
            }
        }
    }

    static /* synthetic */ boolean lambda$onTuningChanged$2(List list, Entry entry) {
        return !list.contains(entry.getKey());
    }

    static /* synthetic */ void lambda$onTuningChanged$3(Entry entry) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Destroying tile: ");
            sb.append((String) entry.getKey());
            Log.d("QSTileHost", sb.toString());
        }
        ((QSTile) entry.getValue()).destroy();
    }

    public void removeTile(String str) {
        if (str != null) {
            changeTileSpecs(new Predicate(str) {
                private final /* synthetic */ String f$0;

                {
                    this.f$0 = r1;
                }

                public final boolean test(Object obj) {
                    return ((List) obj).remove(this.f$0);
                }
            });
        }
    }

    public void unmarkTileAsAutoAdded(String str) {
        AutoTileManager autoTileManager = this.mAutoTiles;
        if (autoTileManager != null) {
            autoTileManager.unmarkTileAsAutoAdded(str);
        }
    }

    public void addTile(String str) {
        changeTileSpecs(new Predicate(str) {
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            public final boolean test(Object obj) {
                return ((List) obj).add(this.f$0);
            }
        });
    }

    private void changeTileSpecs(Predicate<List<String>> predicate) {
        String str = "sysui_qs_tiles";
        List loadTileSpecs = loadTileSpecs(this.mContext, Secure.getStringForUser(this.mContext.getContentResolver(), str, ActivityManager.getCurrentUser()));
        if (predicate.test(loadTileSpecs)) {
            Secure.putStringForUser(this.mContext.getContentResolver(), str, TextUtils.join(",", loadTileSpecs), ActivityManager.getCurrentUser());
        }
    }

    public void addTile(ComponentName componentName) {
        ArrayList arrayList = new ArrayList(this.mTileSpecs);
        arrayList.add(0, CustomTile.toSpec(componentName));
        changeTiles(this.mTileSpecs, arrayList);
    }

    public void removeTile(ComponentName componentName) {
        ArrayList arrayList = new ArrayList(this.mTileSpecs);
        arrayList.remove(CustomTile.toSpec(componentName));
        changeTiles(this.mTileSpecs, arrayList);
    }

    public void changeTiles(List<String> list, List<String> list2) {
        String str = "QSTileHost";
        if (list == null || !list.equals(list2)) {
            int size = list.size();
            list2.size();
            for (int i = 0; i < size; i++) {
                String str2 = (String) list.get(i);
                if (str2.startsWith("custom(") && !list2.contains(str2)) {
                    ComponentName componentFromSpec = CustomTile.getComponentFromSpec(str2);
                    TileLifecycleManager tileLifecycleManager = new TileLifecycleManager(new Handler(), this.mContext, this.mServices, new Tile(), new Intent().setComponent(componentFromSpec), new UserHandle(ActivityManager.getCurrentUser()));
                    tileLifecycleManager.onStopListening();
                    tileLifecycleManager.onTileRemoved();
                    TileLifecycleManager.setTileAdded(this.mContext, componentFromSpec, false);
                    tileLifecycleManager.flushMessagesAndUnbind();
                }
            }
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("saveCurrentTiles ");
                sb.append(list2);
                Log.d(str, sb.toString());
            }
            Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", list2), ActivityManager.getCurrentUser());
            return;
        }
        if (DEBUG) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("changeTiles: no change skip. tiles=");
            sb2.append(list);
            Log.d(str, sb2.toString());
        }
    }

    public QSTile createTile(String str) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTile createTile = ((QSFactory) this.mQsFactories.get(i)).createTile(str);
            if (createTile != null) {
                return createTile;
            }
        }
        return null;
    }

    public QSTileView createTileView(QSTile qSTile, boolean z) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTileView createTileView = ((QSFactory) this.mQsFactories.get(i)).createTileView(qSTile, z);
            if (createTileView != null) {
                return createTileView;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Default factory didn't create view for ");
        sb.append(qSTile.getTileSpec());
        throw new RuntimeException(sb.toString());
    }

    protected static List<String> loadTileSpecs(Context context, String str) {
        Resources resources = context.getResources();
        String string = resources.getString(R$string.quick_settings_tiles_default);
        String str2 = "QSTileHost";
        if (OpUtils.isCurrentGuest(context)) {
            str = resources.getString(R$string.quick_settings_tiles_guest);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Loaded tile specs of guest from config: ");
                sb.append(str);
                Log.d(str2, sb.toString());
            }
        } else if (TextUtils.isEmpty(str)) {
            str = resources.getString(R$string.quick_settings_tiles);
            if (DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Loaded tile specs from config: ");
                sb2.append(str);
                Log.d(str2, sb2.toString());
            }
        } else if (DEBUG) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Loaded tile specs from setting: ");
            sb3.append(str);
            Log.d(str2, sb3.toString());
        }
        ArrayList arrayList = new ArrayList();
        String str3 = ",";
        boolean z = false;
        for (String trim : str.split(str3)) {
            String trim2 = trim.trim();
            if (!trim2.isEmpty()) {
                if (trim2.equals("default")) {
                    if (!z) {
                        arrayList.addAll(Arrays.asList(string.split(str3)));
                        if (Build.IS_DEBUGGABLE) {
                            arrayList.add("dbg:mem");
                        }
                        z = true;
                    }
                } else if (trim2.equals("opdnd")) {
                    arrayList.add("dnd");
                } else if (trim2.equals("powersaving")) {
                    arrayList.add("battery");
                } else {
                    arrayList.add(trim2);
                }
            }
        }
        return arrayList;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("QSTileHost:");
        this.mTiles.values().stream().filter($$Lambda$QSTileHost$w0YHlhMwIm7qnoeEO7kRZCq47o8.INSTANCE).forEach(new Consumer(fileDescriptor, printWriter, strArr) {
            private final /* synthetic */ FileDescriptor f$0;
            private final /* synthetic */ PrintWriter f$1;
            private final /* synthetic */ String[] f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void accept(Object obj) {
                ((Dumpable) ((QSTile) obj)).dump(this.f$0, this.f$1, this.f$2);
            }
        });
    }

    static /* synthetic */ boolean lambda$dump$6(QSTile qSTile) {
        return qSTile instanceof Dumpable;
    }

    public boolean isNeedToHide(String str) {
        OperatorCustom operatorCustom = this.mOperatorCustom;
        if (operatorCustom != null && operatorCustom.mHideList != null && this.mOperatorCustom.mHideList.size() > 0 && !TextUtils.isEmpty(str) && this.mOperatorCustom.mHideList.contains(str)) {
            return true;
        }
        return false;
    }
}
