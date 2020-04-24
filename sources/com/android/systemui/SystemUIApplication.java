package com.android.systemui;

import android.app.ActivityThread;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimingsTraceLog;
import com.android.systemui.SystemUI.Injector;
import com.android.systemui.plugins.OverlayPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.phone.StatusBarWindowController.OtherwisedCollapsedListener;
import com.android.systemui.util.NotificationChannels;
import com.oneplus.systemui.biometrics.OpBiometricDialogImpl;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SystemUIApplication extends Application implements SysUiServiceProvider {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public boolean mBootCompleted;
    private final Map<Class<?>, Object> mComponents = new HashMap();
    /* access modifiers changed from: private */
    public SystemUI[] mServices;
    /* access modifiers changed from: private */
    public boolean mServicesStarted;

    public void onCreate() {
        super.onCreate();
        setTheme(R$style.Theme_SystemUI);
        SystemUIFactory.createFromConfig(this);
        if (Process.myUserHandle().equals(UserHandle.SYSTEM)) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter.setPriority(1000);
            registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (!SystemUIApplication.this.mBootCompleted) {
                        if (SystemUIApplication.DEBUG) {
                            Log.v("SystemUIService", "BOOT_COMPLETED received");
                        }
                        SystemUIApplication.this.unregisterReceiver(this);
                        SystemUIApplication.this.mBootCompleted = true;
                        if (SystemUIApplication.this.mServicesStarted) {
                            for (SystemUI onBootCompleted : SystemUIApplication.this.mServices) {
                                onBootCompleted.onBootCompleted();
                            }
                        }
                    }
                }
            }, intentFilter);
            registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction()) && SystemUIApplication.this.mBootCompleted) {
                        NotificationChannels.createAll(context);
                    }
                }
            }, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        } else {
            String currentProcessName = ActivityThread.currentProcessName();
            ApplicationInfo applicationInfo = getApplicationInfo();
            if (currentProcessName != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(applicationInfo.processName);
                sb.append(":");
                if (currentProcessName.startsWith(sb.toString())) {
                    return;
                }
            }
            startSecondaryUserServicesIfNeeded();
        }
    }

    public void startServicesIfNeeded() {
        startServicesIfNeeded(needOverrideServicesByOp(getResources().getStringArray(R$array.config_systemUIServiceComponents)));
    }

    private String[] needOverrideServicesByOp(String[] strArr) {
        ArrayList arrayList = new ArrayList();
        if (strArr != null && strArr.length > 0) {
            arrayList.addAll(Arrays.asList(strArr));
        }
        if (OpUtils.isCustomFingerprint()) {
            arrayList.add(OpBiometricDialogImpl.class.getName());
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    /* access modifiers changed from: 0000 */
    public void startSecondaryUserServicesIfNeeded() {
        startServicesIfNeeded(getResources().getStringArray(R$array.config_systemUIServiceComponentsPerUser));
    }

    private void startServicesIfNeeded(String[] strArr) {
        if (!this.mServicesStarted) {
            this.mServices = new SystemUI[strArr.length];
            String str = "SystemUIService";
            if (!this.mBootCompleted) {
                if ("1".equals(SystemProperties.get("sys.boot_completed"))) {
                    this.mBootCompleted = true;
                    if (DEBUG) {
                        Log.v(str, "BOOT_COMPLETED was already sent");
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Starting SystemUI services for user ");
            sb.append(Process.myUserHandle().getIdentifier());
            sb.append(".");
            Log.v(str, sb.toString());
            TimingsTraceLog timingsTraceLog = new TimingsTraceLog("SystemUIBootTiming", 4096);
            String str2 = "StartServices";
            timingsTraceLog.traceBegin(str2);
            OpMdmLogger.init(this);
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                String str3 = strArr[i];
                if (DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("loading: ");
                    sb2.append(str3);
                    Log.d(str, sb2.toString());
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append(str2);
                sb3.append(str3);
                timingsTraceLog.traceBegin(sb3.toString());
                long currentTimeMillis = System.currentTimeMillis();
                try {
                    Class cls = Class.forName(str3);
                    Object newInstance = cls.newInstance();
                    if (newInstance instanceof Injector) {
                        newInstance = ((Injector) newInstance).apply(this);
                    }
                    this.mServices[i] = (SystemUI) newInstance;
                    SystemUI[] systemUIArr = this.mServices;
                    systemUIArr[i].mContext = this;
                    systemUIArr[i].mComponents = this.mComponents;
                    if (DEBUG) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("running: ");
                        sb4.append(this.mServices[i]);
                        Log.d(str, sb4.toString());
                    }
                    this.mServices[i].start();
                    timingsTraceLog.traceEnd();
                    long currentTimeMillis2 = System.currentTimeMillis() - currentTimeMillis;
                    if (currentTimeMillis2 > 1000) {
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("Initialization of ");
                        sb5.append(cls.getName());
                        sb5.append(" took ");
                        sb5.append(currentTimeMillis2);
                        sb5.append(" ms");
                        Log.w(str, sb5.toString());
                    }
                    if (this.mBootCompleted) {
                        this.mServices[i].onBootCompleted();
                    }
                    i++;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e2) {
                    throw new RuntimeException(e2);
                } catch (InstantiationException e3) {
                    throw new RuntimeException(e3);
                }
            }
            ((InitController) Dependency.get(InitController.class)).executePostInitTasks();
            timingsTraceLog.traceEnd();
            final Handler handler = new Handler(Looper.getMainLooper());
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener<T>) new PluginListener<OverlayPlugin>() {
                /* access modifiers changed from: private */
                public ArraySet<OverlayPlugin> mOverlays = new ArraySet<>();

                /* renamed from: com.android.systemui.SystemUIApplication$3$Callback */
                class Callback implements com.android.systemui.plugins.OverlayPlugin.Callback {
                    private final OverlayPlugin mPlugin;

                    Callback(OverlayPlugin overlayPlugin) {
                        this.mPlugin = overlayPlugin;
                    }

                    public void onHoldStatusBarOpenChange() {
                        if (this.mPlugin.holdStatusBarOpen()) {
                            C07523.this.mOverlays.add(this.mPlugin);
                        } else {
                            C07523.this.mOverlays.remove(this.mPlugin);
                        }
                        handler.post(new Runnable() {
                            public void run() {
                                ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setStateListener(new OtherwisedCollapsedListener(this) {
                                    private final /* synthetic */ C07551 f$0;

                                    public final 
/*
Method generation error in method: com.android.systemui.-$$Lambda$SystemUIApplication$3$Callback$1$sx3y3YDR9PfTcBFpqL5skj6JDUg.setWouldOtherwiseCollapse(boolean):null, dex: classes.dex
                                    java.lang.NullPointerException
                                    	at jadx.core.codegen.ClassGen.useType(ClassGen.java:442)
                                    	at jadx.core.codegen.MethodGen.addDefinition(MethodGen.java:109)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:311)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:110)
                                    	at jadx.core.codegen.ClassGen.addInnerClasses(ClassGen.java:237)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:224)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                                    	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:138)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:62)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:110)
                                    	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:76)
                                    	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:44)
                                    	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:32)
                                    	at jadx.core.codegen.CodeGen.generate(CodeGen.java:20)
                                    	at jadx.core.ProcessClass.process(ProcessClass.java:36)
                                    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
                                    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
                                    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
                                    
*/
                                });
                                ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setForcePluginOpen(C07523.this.mOverlays.size() != 0);
                            }

                            public /* synthetic */ void lambda$run$1$SystemUIApplication$3$Callback$1(boolean z) {
                                C07523.this.mOverlays.forEach(new Consumer(z) {
                                    private final /* synthetic */ boolean f$0;

                                    public final 
/*
Method generation error in method: com.android.systemui.-$$Lambda$SystemUIApplication$3$Callback$1$BwolTXxR8lk33KXtnn_kk1xKxjQ.accept(java.lang.Object):null, dex: classes.dex
                                    java.lang.NullPointerException
                                    	at jadx.core.codegen.ClassGen.useType(ClassGen.java:442)
                                    	at jadx.core.codegen.MethodGen.addDefinition(MethodGen.java:109)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:311)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:110)
                                    	at jadx.core.codegen.ClassGen.addInnerClasses(ClassGen.java:237)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:224)
                                    	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:661)
                                    	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:595)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:353)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:223)
                                    	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:105)
                                    	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:773)
                                    	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:713)
                                    	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:357)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:239)
                                    	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
                                    	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                                    	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:138)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:62)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                    	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                    	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:210)
                                    	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:203)
                                    	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:316)
                                    	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:262)
                                    	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:225)
                                    	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:110)
                                    	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:76)
                                    	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:44)
                                    	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:32)
                                    	at jadx.core.codegen.CodeGen.generate(CodeGen.java:20)
                                    	at jadx.core.ProcessClass.process(ProcessClass.java:36)
                                    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
                                    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
                                    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
                                    
*/
                                });
                            }
                        });
                    }
                }

                public void onPluginConnected(final OverlayPlugin overlayPlugin, Context context) {
                    handler.post(new Runnable() {
                        public void run() {
                            StatusBar statusBar = (StatusBar) SystemUIApplication.this.getComponent(StatusBar.class);
                            if (statusBar != null) {
                                overlayPlugin.setup(statusBar.getStatusBarWindow(), statusBar.getNavigationBarView(), new Callback(overlayPlugin));
                            }
                        }
                    });
                }

                public void onPluginDisconnected(final OverlayPlugin overlayPlugin) {
                    handler.post(new Runnable() {
                        public void run() {
                            C07523.this.mOverlays.remove(overlayPlugin);
                            ((StatusBarWindowController) Dependency.get(StatusBarWindowController.class)).setForcePluginOpen(C07523.this.mOverlays.size() != 0);
                        }
                    });
                }
            }, OverlayPlugin.class, true);
            this.mServicesStarted = true;
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        StringBuilder sb = new StringBuilder();
        sb.append("onConfigurationChanged, call back to running services, mServicesStarted: ");
        sb.append(this.mServicesStarted);
        Log.d("SystemUIService", sb.toString());
        if (this.mServicesStarted) {
            int length = this.mServices.length;
            for (int i = 0; i < length; i++) {
                SystemUI[] systemUIArr = this.mServices;
                if (systemUIArr[i] != null) {
                    systemUIArr[i].onConfigurationChanged(configuration);
                }
            }
        }
    }

    public <T> T getComponent(Class<T> cls) {
        return this.mComponents.get(cls);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
