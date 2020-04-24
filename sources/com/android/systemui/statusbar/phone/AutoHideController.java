package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings.System;
import android.util.Log;
import android.view.IWindowManager;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.NotificationRemoteInputManager;

public class AutoHideController implements Callbacks {
    protected static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private final Runnable mAutoHide = new Runnable() {
        public final void run() {
            AutoHideController.this.lambda$new$0$AutoHideController();
        }
    };
    private boolean mAutoHideSuspended;
    private final CommandQueue mCommandQueue;
    /* access modifiers changed from: private */
    public Context mContext;
    @VisibleForTesting
    int mDisplayId;
    private final Handler mHandler;
    private int mLastDispatchedSystemUiVisibility = -1;
    /* access modifiers changed from: private */
    public NavigationBarFragment mNavigationBar;
    private final NotificationRemoteInputManager mRemoteInputManager;
    private StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public boolean mSwapNavKeys = false;
    private ContentObserver mSwapNavKeysObserver;
    @VisibleForTesting
    int mSystemUiVisibility;
    private final IWindowManager mWindowManagerService;

    public /* synthetic */ void lambda$new$0$AutoHideController() {
        int i = this.mSystemUiVisibility & (~getTransientMask());
        if (this.mSystemUiVisibility != i) {
            notifySystemUiVisibilityChanged(i);
        }
    }

    public AutoHideController(Context context, Handler handler) {
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mCommandQueue.addCallback((Callbacks) this);
        this.mHandler = handler;
        this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        this.mWindowManagerService = (IWindowManager) Dependency.get(IWindowManager.class);
        this.mDisplayId = context.getDisplayId();
        this.mContext = context;
        this.mSwapNavKeysObserver = getSwapNavObserver(this.mHandler);
        this.mSwapNavKeysObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oem_acc_key_define"), true, this.mSwapNavKeysObserver, -1);
    }

    public void onDisplayRemoved(int i) {
        if (i == this.mDisplayId) {
            this.mCommandQueue.removeCallback((Callbacks) this);
        }
    }

    /* access modifiers changed from: 0000 */
    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    /* access modifiers changed from: 0000 */
    public void setNavigationBar(NavigationBarFragment navigationBarFragment) {
        this.mNavigationBar = navigationBarFragment;
    }

    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        int i6 = i5;
        if (i == this.mDisplayId) {
            int i7 = this.mSystemUiVisibility;
            int i8 = ((~i6) & i7) | (i2 & i6);
            if ((i7 ^ i8) != 0) {
                this.mSystemUiVisibility = i8;
                if (hasStatusBar() && (268435456 & i2) != 0) {
                    this.mSystemUiVisibility &= -268435457;
                }
                if (hasNavigationBar() && (536870912 & i2) != 0) {
                    this.mSystemUiVisibility &= -536870913;
                }
                int i9 = this.mSystemUiVisibility;
                if (i9 != i8) {
                    this.mCommandQueue.setSystemUiVisibility(this.mDisplayId, i9, i3, i4, i5, rect, rect2, z);
                }
                notifySystemUiVisibilityChanged(this.mSystemUiVisibility);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void notifySystemUiVisibilityChanged(int i) {
        try {
            if (this.mLastDispatchedSystemUiVisibility != i) {
                this.mWindowManagerService.statusBarVisibilityChanged(this.mDisplayId, i);
                this.mLastDispatchedSystemUiVisibility = i;
            }
        } catch (RemoteException unused) {
            Log.w("AutoHideController", "Cannot get WindowManager");
        }
    }

    /* access modifiers changed from: 0000 */
    public void resumeSuspendedAutoHide() {
        if (this.mAutoHideSuspended) {
            scheduleAutoHide();
            Runnable checkBarModesRunnable = getCheckBarModesRunnable();
            if (checkBarModesRunnable != null) {
                this.mHandler.postDelayed(checkBarModesRunnable, 500);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void suspendAutoHide() {
        this.mHandler.removeCallbacks(this.mAutoHide);
        Runnable checkBarModesRunnable = getCheckBarModesRunnable();
        if (checkBarModesRunnable != null) {
            this.mHandler.removeCallbacks(checkBarModesRunnable);
        }
        boolean z = false;
        if (!hasStatusBar() || !this.mStatusBar.isHideNavBar()) {
            if ((this.mSystemUiVisibility & getTransientMask()) != 0) {
                z = true;
            }
            this.mAutoHideSuspended = z;
            return;
        }
        this.mHandler.post(this.mAutoHide);
        this.mAutoHideSuspended = false;
    }

    /* access modifiers changed from: 0000 */
    public void touchAutoHide() {
        if ((!hasStatusBar() || this.mStatusBar.getStatusBarMode() != 1) && (!hasNavigationBar() || !this.mNavigationBar.isSemiTransparent())) {
            cancelAutoHide();
        } else {
            scheduleAutoHide();
        }
    }

    private Runnable getCheckBarModesRunnable() {
        if (hasStatusBar()) {
            return new Runnable() {
                public final void run() {
                    AutoHideController.this.lambda$getCheckBarModesRunnable$1$AutoHideController();
                }
            };
        }
        if (hasNavigationBar()) {
            return new Runnable() {
                public final void run() {
                    AutoHideController.this.lambda$getCheckBarModesRunnable$2$AutoHideController();
                }
            };
        }
        return null;
    }

    public /* synthetic */ void lambda$getCheckBarModesRunnable$1$AutoHideController() {
        this.mStatusBar.checkBarModes();
    }

    public /* synthetic */ void lambda$getCheckBarModesRunnable$2$AutoHideController() {
        this.mNavigationBar.checkNavBarModes();
    }

    private void cancelAutoHide() {
        this.mAutoHideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutoHide);
    }

    private void scheduleAutoHide() {
        cancelAutoHide();
        this.mHandler.postDelayed(this.mAutoHide, 2250);
    }

    /* access modifiers changed from: 0000 */
    public void checkUserAutoHide(MotionEvent motionEvent) {
        boolean z = (this.mSystemUiVisibility & getTransientMask()) != 0 && motionEvent.getAction() == 4 && motionEvent.getX() == 0.0f && motionEvent.getY() == 0.0f;
        if (hasStatusBar()) {
            z &= !this.mRemoteInputManager.getController().isRemoteInputActive();
        }
        if (z) {
            userAutoHide();
        }
    }

    private void userAutoHide() {
        cancelAutoHide();
        if (DEBUG_ONEPLUS) {
            Log.i("AutoHideController", " userAutohide");
        }
        this.mHandler.postDelayed(this.mAutoHide, 350);
    }

    private int getTransientMask() {
        int i = hasStatusBar() ? 67108864 : 0;
        return hasNavigationBar() ? i | 134217728 : i;
    }

    /* access modifiers changed from: 0000 */
    public boolean hasNavigationBar() {
        return this.mNavigationBar != null;
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean hasStatusBar() {
        return this.mStatusBar != null;
    }

    private ContentObserver getSwapNavObserver(Handler handler) {
        return new ContentObserver(handler) {
            public void onChange(boolean z) {
                boolean z2 = true;
                if (System.getInt(AutoHideController.this.mContext.getContentResolver(), "oem_acc_key_define", 0) != 1) {
                    z2 = false;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("swap key from ");
                sb.append(AutoHideController.this.mSwapNavKeys);
                sb.append(" to ");
                sb.append(z2);
                Log.d("AutoHideController", sb.toString());
                if (AutoHideController.this.mSwapNavKeys != z2) {
                    AutoHideController.this.mSwapNavKeys = z2;
                    if (AutoHideController.this.mNavigationBar != null) {
                        AutoHideController.this.mNavigationBar.refreshLayout(0);
                    }
                }
            }
        };
    }
}
