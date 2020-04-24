package com.android.systemui.statusbar.phone;

import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.IWallpaperVisibilityListener;
import android.view.IWallpaperVisibilityListener.Stub;
import android.view.IWindowManager;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.Dependency;
import com.android.systemui.R$bool;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.LightBarTransitionsController.DarkIntensityApplier;
import com.oneplus.util.OpNavBarUtils;
import java.util.ArrayList;
import java.util.List;

public final class NavigationBarTransitions extends BarTransitions implements DarkIntensityApplier {
    private final boolean mAllowAutoDimWallpaperNotVisible;
    private boolean mAutoDim;
    private final IStatusBarService mBarService;
    private List<DarkIntensityListener> mDarkIntensityListeners;
    /* access modifiers changed from: private */
    public final Handler mHandler = Handler.getMain();
    private final LightBarTransitionsController mLightTransitionsController;
    private boolean mLightsOut;
    private int mNavBarMode = 0;
    private View mNavButtons;
    private final NavigationBarView mView;
    private final IWallpaperVisibilityListener mWallpaperVisibilityListener = new Stub() {
        public void onWallpaperVisibilityChanged(boolean z, int i) throws RemoteException {
            NavigationBarTransitions.this.mWallpaperVisible = z;
            NavigationBarTransitions.this.mHandler.post(new Runnable() {
                public final void run() {
                    C14581.this.lambda$onWallpaperVisibilityChanged$0$NavigationBarTransitions$1();
                }
            });
        }

        public /* synthetic */ void lambda$onWallpaperVisibilityChanged$0$NavigationBarTransitions$1() {
            NavigationBarTransitions.this.applyLightsOut(false, false);
        }
    };
    /* access modifiers changed from: private */
    public boolean mWallpaperVisible;

    public interface DarkIntensityListener {
        void onDarkIntensity(float f);
    }

    public NavigationBarTransitions(NavigationBarView navigationBarView) {
        super(navigationBarView, R$drawable.nav_background);
        this.mView = navigationBarView;
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mLightTransitionsController = new LightBarTransitionsController(navigationBarView.getContext(), this);
        this.mAllowAutoDimWallpaperNotVisible = navigationBarView.getContext().getResources().getBoolean(R$bool.config_navigation_bar_enable_auto_dim_no_visible_wallpaper);
        this.mDarkIntensityListeners = new ArrayList();
        try {
            this.mWallpaperVisible = ((IWindowManager) Dependency.get(IWindowManager.class)).registerWallpaperVisibilityListener(this.mWallpaperVisibilityListener, 0);
        } catch (RemoteException unused) {
        }
        this.mView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                NavigationBarTransitions.this.lambda$new$0$NavigationBarTransitions(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        View currentView = this.mView.getCurrentView();
        if (currentView != null) {
            this.mNavButtons = currentView.findViewById(R$id.nav_buttons);
        }
    }

    public /* synthetic */ void lambda$new$0$NavigationBarTransitions(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        View currentView = this.mView.getCurrentView();
        if (currentView != null) {
            this.mNavButtons = currentView.findViewById(R$id.nav_buttons);
            applyLightsOut(false, true);
        }
    }

    public void init() {
        applyModeBackground(-1, getMode(), false);
        applyLightsOut(false, true);
    }

    public void destroy() {
        try {
            ((IWindowManager) Dependency.get(IWindowManager.class)).unregisterWallpaperVisibilityListener(this.mWallpaperVisibilityListener, 0);
        } catch (RemoteException unused) {
        }
    }

    public void setAutoDim(boolean z) {
        if ((!z || !NavBarTintController.isEnabled(this.mView.getContext(), this.mNavBarMode)) && this.mAutoDim != z) {
            this.mAutoDim = z;
            applyLightsOut(true, false);
        }
    }

    /* access modifiers changed from: 0000 */
    public void setBackgroundFrame(Rect rect) {
        this.mBarBackground.setFrame(rect);
    }

    /* access modifiers changed from: protected */
    public boolean isLightsOut(int i) {
        return super.isLightsOut(i) || (this.mAllowAutoDimWallpaperNotVisible && this.mAutoDim && !this.mWallpaperVisible && i != 5);
    }

    public LightBarTransitionsController getLightTransitionsController() {
        return this.mLightTransitionsController;
    }

    /* access modifiers changed from: protected */
    public void onTransition(int i, int i2, boolean z) {
        super.onTransition(i, i2, z);
        applyLightsOut(z, false);
        this.mView.onBarTransition(i2);
    }

    /* access modifiers changed from: private */
    public void applyLightsOut(boolean z, boolean z2) {
        applyLightsOut(isLightsOut(getMode()), z, z2);
    }

    private void applyLightsOut(boolean z, boolean z2, boolean z3) {
        if (z3 || z != this.mLightsOut) {
            this.mLightsOut = z;
            View view = this.mNavButtons;
            if (view != null) {
                view.animate().cancel();
                float currentDarkIntensity = z ? (this.mLightTransitionsController.getCurrentDarkIntensity() / 10.0f) + 0.6f : 1.0f;
                if (!z2) {
                    this.mNavButtons.setAlpha(currentDarkIntensity);
                } else {
                    this.mNavButtons.animate().alpha(currentDarkIntensity).setDuration((long) (z ? 500 : 250)).start();
                }
            }
        }
    }

    public void reapplyDarkIntensity() {
        applyDarkIntensity(this.mLightTransitionsController.getCurrentDarkIntensity());
    }

    public void applyDarkIntensity(float f) {
        if (this.mView == null) {
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("mView == null, ");
                sb.append(f);
                sb.append(", ");
                sb.append(this.mNavBarMode);
                Log.d("NavBarTransitions", sb.toString());
            }
            return;
        }
        boolean z = false;
        if (!OpNavBarUtils.isSupportCustomNavBar() || QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            SparseArray buttonDispatchers = this.mView.getButtonDispatchers();
            for (int size = buttonDispatchers.size() - 1; size >= 0; size--) {
                ((ButtonDispatcher) buttonDispatchers.valueAt(size)).setDarkIntensity(f);
            }
            this.mView.getRotationButtonController().setDarkIntensity(f);
            for (DarkIntensityListener onDarkIntensity : this.mDarkIntensityListeners) {
                onDarkIntensity.onDarkIntensity(f);
            }
            if (this.mAutoDim) {
                applyLightsOut(false, true);
            }
            return;
        }
        NavigationBarView navigationBarView = this.mView;
        if (f == 1.0f) {
            z = true;
        }
        navigationBarView.setLightBar(z);
    }

    public int getTintAnimationDuration() {
        if (NavBarTintController.isEnabled(this.mView.getContext(), this.mNavBarMode)) {
            return Math.max(1700, 400);
        }
        return 120;
    }

    public void onNavigationModeChanged(int i) {
        this.mNavBarMode = i;
    }

    public float addDarkIntensityListener(DarkIntensityListener darkIntensityListener) {
        this.mDarkIntensityListeners.add(darkIntensityListener);
        return this.mLightTransitionsController.getCurrentDarkIntensity();
    }

    public void removeDarkIntensityListener(DarkIntensityListener darkIntensityListener) {
        this.mDarkIntensityListeners.remove(darkIntensityListener);
    }
}
