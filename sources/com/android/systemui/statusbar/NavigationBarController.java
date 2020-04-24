package com.android.systemui.statusbar;

import android.app.Fragment;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.RegisterStatusBarResult;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.oneplus.util.OpNavBarUtils;

public class NavigationBarController implements Callbacks {
    private static final String TAG = "NavigationBarController";
    private final Context mContext;
    private final DisplayManager mDisplayManager;
    private final Handler mHandler;
    private View mNavBarView;
    private int mNavigationBarColor = 0;
    @VisibleForTesting
    SparseArray<NavigationBarFragment> mNavigationBars = new SparseArray<>();
    private String mPackageName;

    public NavigationBarController(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        CommandQueue commandQueue = (CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class);
        if (commandQueue != null) {
            commandQueue.addCallback((Callbacks) this);
        }
    }

    public void onDisplayRemoved(int i) {
        removeNavigationBar(i);
    }

    public void onDisplayReady(int i) {
        createNavigationBar(this.mDisplayManager.getDisplay(i), null);
    }

    public void createNavigationBars(boolean z, RegisterStatusBarResult registerStatusBarResult) {
        Display[] displays;
        for (Display display : this.mDisplayManager.getDisplays()) {
            if (z || display.getDisplayId() != 0) {
                createNavigationBar(display, registerStatusBarResult);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void createNavigationBar(Display display, RegisterStatusBarResult registerStatusBarResult) {
        Context context;
        if (display != null) {
            int displayId = display.getDisplayId();
            boolean z = displayId == 0;
            try {
                if (WindowManagerGlobal.getWindowManagerService().hasNavigationBar(displayId) || z) {
                    if (z) {
                        context = this.mContext;
                    } else {
                        context = this.mContext.createDisplayContext(display);
                    }
                    Context context2 = context;
                    $$Lambda$NavigationBarController$oyTONslWMHHQSXiga3Vs0njIek8 r0 = new FragmentListener(z, context2, displayId, registerStatusBarResult, display) {
                        private final /* synthetic */ boolean f$1;
                        private final /* synthetic */ Context f$2;
                        private final /* synthetic */ int f$3;
                        private final /* synthetic */ RegisterStatusBarResult f$4;
                        private final /* synthetic */ Display f$5;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                            this.f$3 = r4;
                            this.f$4 = r5;
                            this.f$5 = r6;
                        }

                        public final void onFragmentViewCreated(String str, Fragment fragment) {
                            NavigationBarController.this.lambda$createNavigationBar$0$NavigationBarController(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, str, fragment);
                        }
                    };
                    this.mNavBarView = NavigationBarFragment.create(context2, r0);
                }
            } catch (RemoteException unused) {
                Log.w(TAG, "Cannot get WindowManager.");
            }
        }
    }

    public /* synthetic */ void lambda$createNavigationBar$0$NavigationBarController(boolean z, Context context, int i, RegisterStatusBarResult registerStatusBarResult, Display display, String str, Fragment fragment) {
        LightBarController lightBarController;
        AutoHideController autoHideController;
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) fragment;
        if (z) {
            lightBarController = (LightBarController) Dependency.get(LightBarController.class);
        } else {
            lightBarController = new LightBarController(context, (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class), (BatteryController) Dependency.get(BatteryController.class));
        }
        navigationBarFragment.setLightBarController(lightBarController);
        if (z) {
            autoHideController = (AutoHideController) Dependency.get(AutoHideController.class);
        } else {
            autoHideController = new AutoHideController(context, this.mHandler);
        }
        navigationBarFragment.setAutoHideController(autoHideController);
        navigationBarFragment.restoreSystemUiVisibilityState();
        if (OpNavBarUtils.isSupportCustomNavBar()) {
            navigationBarFragment.notifyNavBarColorChanged(this.mNavigationBarColor, this.mPackageName);
        }
        this.mNavigationBars.append(i, navigationBarFragment);
        if (registerStatusBarResult != null) {
            navigationBarFragment.setImeWindowStatus(display.getDisplayId(), registerStatusBarResult.mImeToken, registerStatusBarResult.mImeWindowVis, registerStatusBarResult.mImeBackDisposition, registerStatusBarResult.mShowImeSwitcher);
            StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
            if (statusBar != null && statusBar.isImeStateChange()) {
                Log.d(TAG, "Update ImeWindowStatus when add view by IME showing");
                statusBar.updateImeWindowStatus();
            }
        }
    }

    private void removeNavigationBar(int i) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            if (navigationBarFragment.getView() != null) {
                WindowManagerGlobal.getInstance().removeView(navigationBarFragment.getView().getRootView(), true);
            }
            this.mNavigationBars.remove(i);
        }
    }

    public void checkNavBarModes(int i) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.checkNavBarModes();
        }
    }

    public void finishBarAnimations(int i) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.finishBarAnimations();
        }
    }

    public void touchAutoDim(int i) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.touchAutoDim();
        }
    }

    public void transitionTo(int i, int i2, boolean z) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.transitionTo(i2, z);
        }
    }

    public void disableAnimationsDuringHide(int i, long j) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment != null) {
            navigationBarFragment.disableAnimationsDuringHide(j);
        }
    }

    public View getNavView() {
        return this.mNavBarView;
    }

    public NavigationBarView getNavigationBarView(int i) {
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(i);
        if (navigationBarFragment == null) {
            return null;
        }
        return (NavigationBarView) navigationBarFragment.getView();
    }

    public NavigationBarFragment getDefaultNavigationBarFragment() {
        return (NavigationBarFragment) this.mNavigationBars.get(0);
    }

    public void notifyNavBarColorChanged(int i, String str) {
        this.mNavigationBarColor = i;
        this.mPackageName = str;
        NavigationBarFragment navigationBarFragment = (NavigationBarFragment) this.mNavigationBars.get(0);
        if (navigationBarFragment != null) {
            navigationBarFragment.notifyNavBarColorChanged(i, str);
        }
    }
}
