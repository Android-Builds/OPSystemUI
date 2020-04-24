package com.oneplus.systemui.statusbar.phone;

import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.input.InputManager;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.ISystemGestureExclusionListener;
import android.view.ISystemGestureExclusionListener.Stub;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManagerGlobal;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.phone.NavigationHandle;
import com.android.systemui.statusbar.phone.StatusBar;
import java.util.concurrent.Executor;

public class OpEdgeNavGestureHandler implements DisplayListener {
    private boolean mAllowGesture = false;
    private boolean mAllowNavGesture = false;
    private StatusBar mBar;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final int mDisplayId;
    private final Point mDisplaySize = new Point();
    private final int mEdgeWidth;
    /* access modifiers changed from: private */
    public final Region mExcludeRegion = new Region();
    private ISystemGestureExclusionListener mGestureExclusionListener = new Stub() {
        public void onSystemGestureExclusionChanged(int i, Region region) {
            if (i == OpEdgeNavGestureHandler.this.mDisplayId) {
                OpEdgeNavGestureHandler.this.mMainExecutor.execute(new Runnable(region) {
                    private final /* synthetic */ Region f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        C20812.this.mo24790x2bbc87c0(this.f$1);
                    }
                });
            }
        }

        /* renamed from: lambda$onSystemGestureExclusionChanged$0$OpEdgeNavGestureHandler$2 */
        public /* synthetic */ void mo24790x2bbc87c0(Region region) {
            OpEdgeNavGestureHandler.this.mExcludeRegion.set(region);
        }
    };
    private NavigationHandle mHomeHandle;
    private final IPinnedStackListener.Stub mImeChangedListener = new IPinnedStackListener.Stub() {
        public void onActionsChanged(ParceledListSlice parceledListSlice) {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) {
        }

        public void onMinimizedStateChanged(boolean z) {
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
        }

        public void onShelfVisibilityChanged(boolean z, int i) {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            OpEdgeNavGestureHandler opEdgeNavGestureHandler = OpEdgeNavGestureHandler.this;
            if (!z) {
                i = 0;
            }
            opEdgeNavGestureHandler.mImeHeight = i;
        }
    };
    /* access modifiers changed from: private */
    public int mImeHeight = 0;
    private InputEventReceiver mInputEventReceiver;
    private InputMonitor mInputMonitor;
    private boolean mIsAttached;
    private boolean mIsEnabled;
    private boolean mIsGesturalModeEnabled;
    /* access modifiers changed from: private */
    public final Executor mMainExecutor;
    private final int mNavBarHeight;
    private final int mNavEdgeWidth;
    private final int mNavEdgeWidthLand;
    private final OverviewProxyService mOverviewProxyService;
    private int mRotation = 0;

    class SysUiInputEventReceiver extends InputEventReceiver {
        SysUiInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent inputEvent) {
            OpEdgeNavGestureHandler.this.onInputEvent(inputEvent);
            finishInputEvent(inputEvent, true);
        }
    }

    public void onDisplayAdded(int i) {
    }

    public void onDisplayRemoved(int i) {
    }

    public OpEdgeNavGestureHandler(Context context, OverviewProxyService overviewProxyService) {
        Resources resources = context.getResources();
        this.mContext = context;
        this.mDisplayId = context.getDisplayId();
        this.mMainExecutor = context.getMainExecutor();
        this.mOverviewProxyService = overviewProxyService;
        this.mEdgeWidth = resources.getDimensionPixelSize(17105049);
        this.mNavEdgeWidth = resources.getDimensionPixelSize(84279737);
        this.mNavEdgeWidthLand = resources.getDimensionPixelSize(84279736);
        this.mNavBarHeight = resources.getDimensionPixelSize(17105287);
        this.mBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
    }

    public void onNavBarAttached() {
        Log.d("OpEdgeNavGestureHandler", "onNavBarAttached");
        this.mIsAttached = true;
        updateIsEnabled();
    }

    public void onNavBarDetached() {
        Log.d("OpEdgeNavGestureHandler", "onNavBarDetached");
        this.mIsAttached = false;
        updateIsEnabled();
    }

    public void onNavigationModeChanged(int i) {
        this.mIsGesturalModeEnabled = QuickStepContract.isGesturalMode(i);
        updateIsEnabled();
    }

    private void disposeInputChannel() {
        InputEventReceiver inputEventReceiver = this.mInputEventReceiver;
        if (inputEventReceiver != null) {
            inputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        InputMonitor inputMonitor = this.mInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mInputMonitor = null;
        }
    }

    private void updateIsEnabled() {
        StringBuilder sb = new StringBuilder();
        sb.append("updateIsEnabled: ");
        sb.append(this.mIsGesturalModeEnabled);
        sb.append(", Attach: ");
        sb.append(this.mIsAttached);
        sb.append(", enable: ");
        sb.append(this.mIsEnabled);
        String str = "OpEdgeNavGestureHandler";
        Log.d(str, sb.toString());
        boolean z = this.mIsAttached && this.mIsGesturalModeEnabled;
        if (z != this.mIsEnabled) {
            this.mIsEnabled = z;
            disposeInputChannel();
            if (!this.mIsEnabled) {
                WindowManagerWrapper.getInstance().removePinnedStackListener(this.mImeChangedListener);
                try {
                    if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                        WindowManagerGlobal.getWindowManagerService().unregisterSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                    } else {
                        Log.d(str, "It is not unregister system gesture exclusion listener, because display is null or display already removed.");
                    }
                } catch (RemoteException e) {
                    Log.e(str, "Failed to unregister window manager callbacks", e);
                }
            } else {
                updateDisplaySize();
                try {
                    if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                        WindowManagerWrapper.getInstance().addPinnedStackListener(this.mImeChangedListener);
                        WindowManagerGlobal.getWindowManagerService().registerSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                    } else {
                        Log.d(str, "It is not register system gesture exclusion listener, because display is null or display already removed.");
                    }
                } catch (RemoteException e2) {
                    Log.e(str, "Failed to register window manager callbacks", e2);
                }
                this.mInputMonitor = InputManager.getInstance().monitorGestureInput("edge-nav-swipe", this.mDisplayId);
                this.mInputEventReceiver = new SysUiInputEventReceiver(this.mInputMonitor.getInputChannel(), Looper.getMainLooper());
            }
        }
    }

    /* access modifiers changed from: private */
    public void onInputEvent(InputEvent inputEvent) {
        if ((inputEvent instanceof MotionEvent) && this.mHomeHandle != null) {
            onMotionEvent((MotionEvent) inputEvent);
        }
    }

    private boolean isWithinTouchRegion(int i, int i2) {
        if (i2 > this.mDisplaySize.y - Math.max(this.mImeHeight, this.mNavBarHeight)) {
            return false;
        }
        int i3 = this.mEdgeWidth;
        if (i <= i3 || i >= this.mDisplaySize.x - i3) {
            return !this.mExcludeRegion.contains(i, i2);
        }
        return false;
    }

    private boolean isWithinNavTouchRegion(int i, int i2) {
        Point point = this.mDisplaySize;
        String str = "OpEdgeNavGestureHandler";
        if (i2 < point.y - this.mNavBarHeight || this.mAllowGesture) {
            Log.i(str, " mAllowNavGesture touch point is height than NavBarHeight.");
            return false;
        } else if (i <= point.x - getNavEdgeWidth(this.mRotation) && i >= getNavEdgeWidth(this.mRotation)) {
            return !this.mExcludeRegion.contains(i, i2);
        } else {
            Log.i(str, " mAllowNavGesture touch point is out of range");
            return false;
        }
    }

    private void onMotionEvent(MotionEvent motionEvent) {
        motionEvent.getRawX();
        motionEvent.getRawY();
        int actionMasked = motionEvent.getActionMasked();
        String str = "OpEdgeNavGestureHandler";
        boolean z = true;
        if (actionMasked == 0) {
            int systemUiStateFlags = this.mOverviewProxyService.getSystemUiStateFlags();
            this.mAllowGesture = !QuickStepContract.isBackGestureDisabled(systemUiStateFlags) && isWithinTouchRegion((int) motionEvent.getX(), (int) motionEvent.getY());
            if (QuickStepContract.isBackGestureDisabled(systemUiStateFlags) || !isWithinNavTouchRegion((int) motionEvent.getX(), (int) motionEvent.getY())) {
                z = false;
            }
            this.mAllowNavGesture = z;
            if (this.mAllowNavGesture && this.mHomeHandle != null) {
                Log.d(str, "AllowNavGesture actionDown");
                this.mHomeHandle.handleTouch(motionEvent);
            }
        } else if (this.mAllowNavGesture) {
            if (actionMasked == 1) {
                Log.d(str, "AllowNavGesture actionUp");
            }
            this.mHomeHandle.handleTouch(motionEvent);
        }
    }

    public void onDisplayChanged(int i) {
        if (i == this.mDisplayId) {
            updateDisplaySize();
        }
    }

    private void updateDisplaySize() {
        if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) == null) {
            Log.d("OpEdgeNavGestureHandler", "It's not update display size, because display is null or display already removed.");
        } else {
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId).getRealSize(this.mDisplaySize);
        }
    }

    public void onConfigurationChanged(int i) {
        this.mRotation = i;
        if (this.mIsEnabled) {
            updateDisplaySize();
        }
    }

    public void setHomeHandle(View view) {
        this.mHomeHandle = (NavigationHandle) view;
    }

    private int getNavEdgeWidth(int i) {
        if (i == 0 || i == 2) {
            return this.mNavEdgeWidth;
        }
        if (i == 1 || i == 3) {
            return this.mNavEdgeWidthLand;
        }
        return this.mNavEdgeWidth;
    }
}
