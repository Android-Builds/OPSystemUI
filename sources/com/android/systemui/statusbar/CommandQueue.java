package com.android.systemui.statusbar;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodSystemProperty;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBar.Stub;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.CallbackController;
import java.util.ArrayList;

public class CommandQueue extends Stub implements CallbackController<Callbacks>, DisplayListener {
    /* access modifiers changed from: private */
    public ArrayList<Callbacks> mCallbacks = new ArrayList<>();
    private SparseArray<Pair<Integer, Integer>> mDisplayDisabled = new SparseArray<>();
    /* access modifiers changed from: private */
    public Handler mHandler = new C1151H(Looper.getMainLooper());
    private int mLastUpdatedImeDisplayId = -1;
    private final Object mLock = new Object();

    public interface Callbacks {
        void addQsTile(ComponentName componentName) {
        }

        void animateCollapsePanels(int i, boolean z) {
        }

        void animateExpandNotificationsPanel() {
        }

        void animateExpandSettingsPanel(String str) {
        }

        void appTransitionCancelled(int i) {
        }

        void appTransitionFinished(int i) {
        }

        void appTransitionPending(int i, boolean z) {
        }

        void appTransitionStarting(int i, long j, long j2, boolean z) {
        }

        void cancelPreloadRecentApps() {
        }

        void clickTile(ComponentName componentName) {
        }

        void disable(int i, int i2, int i3, boolean z) {
        }

        void dismissKeyboardShortcutsMenu() {
        }

        void handleShowGlobalActionsMenu() {
        }

        void handleShowShutdownUi(boolean z, String str) {
        }

        void handleSystemKey(int i) {
        }

        void hideBiometricDialog() {
        }

        void hideFodDialog(Bundle bundle, String str) {
        }

        void hideRecentApps(boolean z, boolean z2) {
        }

        void notifyImeWindowVisibleStatus(int i, IBinder iBinder, int i2, int i3, boolean z) {
        }

        void notifyNavBarColorChanged(int i, String str) {
        }

        void onBiometricAuthenticated(boolean z, String str) {
        }

        void onBiometricError(String str) {
        }

        void onBiometricHelp(String str) {
        }

        void onCameraLaunchGestureDetected(int i) {
        }

        void onDisplayReady(int i) {
        }

        void onDisplayRemoved(int i) {
        }

        void onFingerprintAcquired(int i, int i2) {
        }

        void onFingerprintAuthenticatedFail() {
        }

        void onFingerprintAuthenticatedSuccess() {
        }

        void onFingerprintEnrollResult(int i) {
        }

        void onFingerprintError(int i) {
        }

        void onPackagePreferencesCleared() {
        }

        void onRecentsAnimationStateChanged(boolean z) {
        }

        void onRotationProposal(int i, boolean z) {
        }

        void passSystemUIEvent(int i) {
        }

        void preloadRecentApps() {
        }

        void remQsTile(ComponentName componentName) {
        }

        void removeIcon(String str) {
        }

        void setIcon(String str, StatusBarIcon statusBarIcon) {
        }

        void setImeWindowStatus(int i, IBinder iBinder, int i2, int i3, boolean z) {
        }

        void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        }

        void setTopAppHidesStatusBar(boolean z) {
        }

        void setWindowState(int i, int i2, int i3) {
        }

        void showAssistDisclosure() {
        }

        void showBiometricDialog(Bundle bundle, IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal, int i, boolean z, int i2) {
        }

        void showFodDialog(Bundle bundle, String str) {
        }

        void showPictureInPictureMenu() {
        }

        void showPinningEnterExitToast(boolean z) {
        }

        void showPinningEscapeToast() {
        }

        void showRecentApps(boolean z) {
        }

        void showScreenPinningRequest(int i) {
        }

        void showWirelessChargingAnimation(int i) {
        }

        void startAssist(Bundle bundle) {
        }

        void toggleKeyboardShortcutsMenu(int i) {
        }

        void togglePanel() {
        }

        void toggleRecentApps() {
        }

        void toggleSplitScreen() {
        }

        void toggleWxBus() {
        }
    }

    public static class CommandQueueStart extends SystemUI {
        public void start() {
            putComponent(CommandQueue.class, new CommandQueue(this.mContext));
        }
    }

    /* renamed from: com.android.systemui.statusbar.CommandQueue$H */
    private final class C1151H extends Handler {
        private C1151H(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:100:0x039e, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:101:0x03a0, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).togglePanel();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:103:0x03bc, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:104:0x03be, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).handleShowGlobalActionsMenu();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:106:0x03da, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:107:0x03dc, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).handleSystemKey(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:109:0x03fa, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:10:0x0056, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onFingerprintAuthenticatedSuccess();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:110:0x03fc, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).dismissKeyboardShortcutsMenu();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:112:0x0418, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:113:0x041a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).appTransitionFinished(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:115:0x0438, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:116:0x043a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).toggleSplitScreen();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:118:0x0456, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:119:0x0458, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).clickTile((android.content.ComponentName) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:121:0x0478, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:122:0x047a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).remQsTile((android.content.ComponentName) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:124:0x049a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:125:0x049c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).addQsTile((android.content.ComponentName) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:127:0x04bc, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:128:0x04be, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showPictureInPictureMenu();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:130:0x04da, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:131:0x04dc, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).toggleKeyboardShortcutsMenu(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:133:0x04fa, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:134:0x04fc, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onCameraLaunchGestureDetected(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:136:0x051a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:137:0x051c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).startAssist((android.os.Bundle) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:139:0x053c, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:140:0x053e, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showAssistDisclosure();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:150:0x0597, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:151:0x0599, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).appTransitionCancelled(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:161:0x05df, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:162:0x05e1, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showScreenPinningRequest(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:184:0x0652, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:185:0x0654, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).setWindowState(r14.arg1, r14.arg2, ((java.lang.Integer) r14.obj).intValue());
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:187:0x067c, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:188:0x067e, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).cancelPreloadRecentApps();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:190:0x069a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:191:0x069c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).preloadRecentApps();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:193:0x06b8, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:194:0x06ba, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).toggleRecentApps();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:201:0x06f3, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:202:0x06f5, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onDisplayReady(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:213:0x0757, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:214:0x0759, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).animateExpandSettingsPanel((java.lang.String) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:224:0x07a1, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:225:0x07a3, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).animateExpandNotificationsPanel();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:22:0x00e6, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:23:0x00e8, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onPackagePreferencesCleared();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x0104, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:26:0x0106, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).passSystemUIEvent(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x0124, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:0x0126, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onFingerprintAuthenticatedFail();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x0142, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x0144, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onFingerprintEnrollResult(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:330:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:331:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:332:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:333:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:334:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:335:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:336:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:337:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:338:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:340:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:341:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:342:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:343:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:344:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:345:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:349:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x0162, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:350:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:351:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:352:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:353:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:354:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:355:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:356:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:357:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:358:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:359:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:35:0x0164, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onFingerprintAcquired(r14.arg1, r14.arg2);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:360:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:361:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:362:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:364:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:366:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:369:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:370:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:371:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:372:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:373:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:374:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:376:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:37:0x0184, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:38:0x0186, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).notifyNavBarColorChanged(r14.arg1, (java.lang.String) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:3:0x0016, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:48:0x01ce, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:49:0x01d0, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showPinningEscapeToast();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:4:0x0018, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).toggleWxBus();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:51:0x01ec, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:52:0x01ee, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showPinningEnterExitToast(((java.lang.Boolean) r14.obj).booleanValue());
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:54:0x0212, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:55:0x0214, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).showWirelessChargingAnimation(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:57:0x0232, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:58:0x0234, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).hideBiometricDialog();
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:60:0x0250, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:61:0x0252, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onBiometricError((java.lang.String) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:63:0x0272, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:64:0x0274, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onBiometricHelp((java.lang.String) r14.obj);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:6:0x0034, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:7:0x0036, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).get(r1)).onFingerprintError(r14.arg1);
            r1 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:9:0x0054, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r13.this$0).size()) goto L_0x0839;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r14) {
            /*
                r13 = this;
                int r0 = r14.what
                r1 = -65536(0xffffffffffff0000, float:NaN)
                r0 = r0 & r1
                r1 = 0
                r2 = 1
                switch(r0) {
                    case 65536: goto L_0x07e5;
                    case 131072: goto L_0x07b5;
                    case 196608: goto L_0x0797;
                    case 262144: goto L_0x076f;
                    case 327680: goto L_0x074d;
                    case 393216: goto L_0x0709;
                    case 458752: goto L_0x06e9;
                    case 524288: goto L_0x06cc;
                    case 589824: goto L_0x06ae;
                    case 655360: goto L_0x0690;
                    case 720896: goto L_0x0672;
                    case 786432: goto L_0x0648;
                    case 851968: goto L_0x0622;
                    case 917504: goto L_0x05f5;
                    case 1179648: goto L_0x05d5;
                    case 1245184: goto L_0x05ad;
                    case 1310720: goto L_0x058d;
                    case 1376256: goto L_0x0550;
                    case 1441792: goto L_0x0532;
                    case 1507328: goto L_0x0510;
                    case 1572864: goto L_0x04f0;
                    case 1638400: goto L_0x04d0;
                    case 1703936: goto L_0x04b2;
                    case 1769472: goto L_0x0490;
                    case 1835008: goto L_0x046e;
                    case 1900544: goto L_0x044c;
                    case 1966080: goto L_0x042e;
                    case 2031616: goto L_0x040e;
                    case 2097152: goto L_0x03f0;
                    case 2162688: goto L_0x03d0;
                    case 2228224: goto L_0x03b2;
                    case 2293760: goto L_0x0394;
                    case 2359296: goto L_0x036a;
                    case 2424832: goto L_0x0344;
                    case 2490368: goto L_0x031c;
                    case 2555904: goto L_0x02bd;
                    case 2621440: goto L_0x028a;
                    case 2686976: goto L_0x0268;
                    case 2752512: goto L_0x0246;
                    case 2818048: goto L_0x0228;
                    case 2883584: goto L_0x0208;
                    case 2949120: goto L_0x01e2;
                    case 3014656: goto L_0x01c4;
                    case 3080192: goto L_0x019e;
                    case 6553600: goto L_0x017a;
                    case 6750208: goto L_0x0158;
                    case 6815744: goto L_0x0138;
                    case 6881280: goto L_0x011a;
                    case 6946816: goto L_0x00fa;
                    case 7012352: goto L_0x00dc;
                    case 7077888: goto L_0x0097;
                    case 7143424: goto L_0x0068;
                    case 7208960: goto L_0x004a;
                    case 7274496: goto L_0x002a;
                    case 7340032: goto L_0x000c;
                    default: goto L_0x000a;
                }
            L_0x000a:
                goto L_0x0839
            L_0x000c:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.toggleWxBus()
                int r1 = r1 + 1
                goto L_0x000c
            L_0x002a:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.onFingerprintError(r2)
                int r1 = r1 + 1
                goto L_0x002a
            L_0x004a:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.onFingerprintAuthenticatedSuccess()
                int r1 = r1 + 1
                goto L_0x004a
            L_0x0068:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
            L_0x006c:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0092
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.arg1
                android.os.Bundle r2 = (android.os.Bundle) r2
                java.lang.Object r3 = r14.arg2
                java.lang.String r3 = (java.lang.String) r3
                r0.hideFodDialog(r2, r3)
                int r1 = r1 + 1
                goto L_0x006c
            L_0x0092:
                r14.recycle()
                goto L_0x0839
            L_0x0097:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r0 = r0.mHandler
                r2 = 7274496(0x6f0000, float:1.019374E-38)
                r0.removeMessages(r2)
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r0 = r0.mHandler
                r2 = 7208960(0x6e0000, float:1.0101905E-38)
                r0.removeMessages(r2)
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
            L_0x00b1:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x00d7
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.arg1
                android.os.Bundle r2 = (android.os.Bundle) r2
                java.lang.Object r3 = r14.arg2
                java.lang.String r3 = (java.lang.String) r3
                r0.showFodDialog(r2, r3)
                int r1 = r1 + 1
                goto L_0x00b1
            L_0x00d7:
                r14.recycle()
                goto L_0x0839
            L_0x00dc:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.onPackagePreferencesCleared()
                int r1 = r1 + 1
                goto L_0x00dc
            L_0x00fa:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.passSystemUIEvent(r2)
                int r1 = r1 + 1
                goto L_0x00fa
            L_0x011a:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.onFingerprintAuthenticatedFail()
                int r1 = r1 + 1
                goto L_0x011a
            L_0x0138:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.onFingerprintEnrollResult(r2)
                int r1 = r1 + 1
                goto L_0x0138
            L_0x0158:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                int r3 = r14.arg2
                r0.onFingerprintAcquired(r2, r3)
                int r1 = r1 + 1
                goto L_0x0158
            L_0x017a:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                java.lang.Object r3 = r14.obj
                java.lang.String r3 = (java.lang.String) r3
                r0.notifyNavBarColorChanged(r2, r3)
                int r1 = r1 + 1
                goto L_0x017a
            L_0x019e:
                r0 = r1
            L_0x019f:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                if (r4 <= 0) goto L_0x01bd
                r4 = r2
                goto L_0x01be
            L_0x01bd:
                r4 = r1
            L_0x01be:
                r3.onRecentsAnimationStateChanged(r4)
                int r0 = r0 + 1
                goto L_0x019f
            L_0x01c4:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.showPinningEscapeToast()
                int r1 = r1 + 1
                goto L_0x01c4
            L_0x01e2:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                java.lang.Boolean r2 = (java.lang.Boolean) r2
                boolean r2 = r2.booleanValue()
                r0.showPinningEnterExitToast(r2)
                int r1 = r1 + 1
                goto L_0x01e2
            L_0x0208:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.showWirelessChargingAnimation(r2)
                int r1 = r1 + 1
                goto L_0x0208
            L_0x0228:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.hideBiometricDialog()
                int r1 = r1 + 1
                goto L_0x0228
            L_0x0246:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                java.lang.String r2 = (java.lang.String) r2
                r0.onBiometricError(r2)
                int r1 = r1 + 1
                goto L_0x0246
            L_0x0268:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                java.lang.String r2 = (java.lang.String) r2
                r0.onBiometricHelp(r2)
                int r1 = r1 + 1
                goto L_0x0268
            L_0x028a:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
            L_0x028e:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x02b8
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.arg1
                java.lang.Boolean r2 = (java.lang.Boolean) r2
                boolean r2 = r2.booleanValue()
                java.lang.Object r3 = r14.arg2
                java.lang.String r3 = (java.lang.String) r3
                r0.onBiometricAuthenticated(r2, r3)
                int r1 = r1 + 1
                goto L_0x028e
            L_0x02b8:
                r14.recycle()
                goto L_0x0839
            L_0x02bd:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r0 = r0.mHandler
                r2 = 2752512(0x2a0000, float:3.857091E-39)
                r0.removeMessages(r2)
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r0 = r0.mHandler
                r2 = 2686976(0x290000, float:3.765255E-39)
                r0.removeMessages(r2)
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r0 = r0.mHandler
                r2 = 2621440(0x280000, float:3.67342E-39)
                r0.removeMessages(r2)
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
            L_0x02e2:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0317
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                r2 = r0
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r0 = r14.arg1
                r3 = r0
                android.os.Bundle r3 = (android.os.Bundle) r3
                java.lang.Object r0 = r14.arg2
                r4 = r0
                android.hardware.biometrics.IBiometricServiceReceiverInternal r4 = (android.hardware.biometrics.IBiometricServiceReceiverInternal) r4
                int r5 = r14.argi1
                java.lang.Object r0 = r14.arg3
                java.lang.Boolean r0 = (java.lang.Boolean) r0
                boolean r6 = r0.booleanValue()
                int r7 = r14.argi2
                r2.showBiometricDialog(r3, r4, r5, r6, r7)
                int r1 = r1 + 1
                goto L_0x02e2
            L_0x0317:
                r14.recycle()
                goto L_0x0839
            L_0x031c:
                r0 = r1
            L_0x031d:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                int r5 = r14.arg2
                if (r5 == 0) goto L_0x033d
                r5 = r2
                goto L_0x033e
            L_0x033d:
                r5 = r1
            L_0x033e:
                r3.onRotationProposal(r4, r5)
                int r0 = r0 + 1
                goto L_0x031d
            L_0x0344:
                r0 = r1
            L_0x0345:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                if (r4 == 0) goto L_0x0363
                r4 = r2
                goto L_0x0364
            L_0x0363:
                r4 = r1
            L_0x0364:
                r3.setTopAppHidesStatusBar(r4)
                int r0 = r0 + 1
                goto L_0x0345
            L_0x036a:
                r0 = r1
            L_0x036b:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                if (r4 == 0) goto L_0x0389
                r4 = r2
                goto L_0x038a
            L_0x0389:
                r4 = r1
            L_0x038a:
                java.lang.Object r5 = r14.obj
                java.lang.String r5 = (java.lang.String) r5
                r3.handleShowShutdownUi(r4, r5)
                int r0 = r0 + 1
                goto L_0x036b
            L_0x0394:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.togglePanel()
                int r1 = r1 + 1
                goto L_0x0394
            L_0x03b2:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.handleShowGlobalActionsMenu()
                int r1 = r1 + 1
                goto L_0x03b2
            L_0x03d0:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.handleSystemKey(r2)
                int r1 = r1 + 1
                goto L_0x03d0
            L_0x03f0:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.dismissKeyboardShortcutsMenu()
                int r1 = r1 + 1
                goto L_0x03f0
            L_0x040e:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.appTransitionFinished(r2)
                int r1 = r1 + 1
                goto L_0x040e
            L_0x042e:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.toggleSplitScreen()
                int r1 = r1 + 1
                goto L_0x042e
            L_0x044c:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                android.content.ComponentName r2 = (android.content.ComponentName) r2
                r0.clickTile(r2)
                int r1 = r1 + 1
                goto L_0x044c
            L_0x046e:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                android.content.ComponentName r2 = (android.content.ComponentName) r2
                r0.remQsTile(r2)
                int r1 = r1 + 1
                goto L_0x046e
            L_0x0490:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                android.content.ComponentName r2 = (android.content.ComponentName) r2
                r0.addQsTile(r2)
                int r1 = r1 + 1
                goto L_0x0490
            L_0x04b2:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.showPictureInPictureMenu()
                int r1 = r1 + 1
                goto L_0x04b2
            L_0x04d0:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.toggleKeyboardShortcutsMenu(r2)
                int r1 = r1 + 1
                goto L_0x04d0
            L_0x04f0:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.onCameraLaunchGestureDetected(r2)
                int r1 = r1 + 1
                goto L_0x04f0
            L_0x0510:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                android.os.Bundle r2 = (android.os.Bundle) r2
                r0.startAssist(r2)
                int r1 = r1 + 1
                goto L_0x0510
            L_0x0532:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.showAssistDisclosure()
                int r1 = r1 + 1
                goto L_0x0532
            L_0x0550:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
                r0 = r1
            L_0x0555:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                r4 = r3
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r14.argi1
                java.lang.Object r3 = r14.arg1
                java.lang.Long r3 = (java.lang.Long) r3
                long r6 = r3.longValue()
                java.lang.Object r3 = r14.arg2
                java.lang.Long r3 = (java.lang.Long) r3
                long r8 = r3.longValue()
                int r3 = r14.argi2
                if (r3 == 0) goto L_0x0586
                r10 = r2
                goto L_0x0587
            L_0x0586:
                r10 = r1
            L_0x0587:
                r4.appTransitionStarting(r5, r6, r8, r10)
                int r0 = r0 + 1
                goto L_0x0555
            L_0x058d:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.appTransitionCancelled(r2)
                int r1 = r1 + 1
                goto L_0x058d
            L_0x05ad:
                r0 = r1
            L_0x05ae:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                int r5 = r14.arg2
                if (r5 == 0) goto L_0x05ce
                r5 = r2
                goto L_0x05cf
            L_0x05ce:
                r5 = r1
            L_0x05cf:
                r3.appTransitionPending(r4, r5)
                int r0 = r0 + 1
                goto L_0x05ae
            L_0x05d5:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.showScreenPinningRequest(r2)
                int r1 = r1 + 1
                goto L_0x05d5
            L_0x05f5:
                r0 = r1
            L_0x05f6:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                if (r4 == 0) goto L_0x0614
                r4 = r2
                goto L_0x0615
            L_0x0614:
                r4 = r1
            L_0x0615:
                int r5 = r14.arg2
                if (r5 == 0) goto L_0x061b
                r5 = r2
                goto L_0x061c
            L_0x061b:
                r5 = r1
            L_0x061c:
                r3.hideRecentApps(r4, r5)
                int r0 = r0 + 1
                goto L_0x05f6
            L_0x0622:
                r0 = r1
            L_0x0623:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                if (r4 == 0) goto L_0x0641
                r4 = r2
                goto L_0x0642
            L_0x0641:
                r4 = r1
            L_0x0642:
                r3.showRecentApps(r4)
                int r0 = r0 + 1
                goto L_0x0623
            L_0x0648:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                int r3 = r14.arg2
                java.lang.Object r4 = r14.obj
                java.lang.Integer r4 = (java.lang.Integer) r4
                int r4 = r4.intValue()
                r0.setWindowState(r2, r3, r4)
                int r1 = r1 + 1
                goto L_0x0648
            L_0x0672:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.cancelPreloadRecentApps()
                int r1 = r1 + 1
                goto L_0x0672
            L_0x0690:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.preloadRecentApps()
                int r1 = r1 + 1
                goto L_0x0690
            L_0x06ae:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.toggleRecentApps()
                int r1 = r1 + 1
                goto L_0x06ae
            L_0x06cc:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                int r4 = r14.argi1
                java.lang.Object r13 = r14.arg1
                r5 = r13
                android.os.IBinder r5 = (android.os.IBinder) r5
                int r6 = r14.argi2
                int r7 = r14.argi3
                int r13 = r14.argi4
                if (r13 == 0) goto L_0x06e3
                r8 = r2
                goto L_0x06e4
            L_0x06e3:
                r8 = r1
            L_0x06e4:
                r3.handleShowImeButton(r4, r5, r6, r7, r8)
                goto L_0x0839
            L_0x06e9:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                int r2 = r14.arg1
                r0.onDisplayReady(r2)
                int r1 = r1 + 1
                goto L_0x06e9
            L_0x0709:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
                r0 = r1
            L_0x070e:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0748
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                r4 = r3
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r14.argi1
                int r6 = r14.argi2
                int r7 = r14.argi3
                int r8 = r14.argi4
                int r9 = r14.argi5
                java.lang.Object r3 = r14.arg1
                r10 = r3
                android.graphics.Rect r10 = (android.graphics.Rect) r10
                java.lang.Object r3 = r14.arg2
                r11 = r3
                android.graphics.Rect r11 = (android.graphics.Rect) r11
                int r3 = r14.argi6
                if (r3 != r2) goto L_0x0741
                r12 = r2
                goto L_0x0742
            L_0x0741:
                r12 = r1
            L_0x0742:
                r4.setSystemUiVisibility(r5, r6, r7, r8, r9, r10, r11, r12)
                int r0 = r0 + 1
                goto L_0x070e
            L_0x0748:
                r14.recycle()
                goto L_0x0839
            L_0x074d:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                java.lang.String r2 = (java.lang.String) r2
                r0.animateExpandSettingsPanel(r2)
                int r1 = r1 + 1
                goto L_0x074d
            L_0x076f:
                r0 = r1
            L_0x0770:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.arg1
                int r5 = r14.arg2
                if (r5 == 0) goto L_0x0790
                r5 = r2
                goto L_0x0791
            L_0x0790:
                r5 = r1
            L_0x0791:
                r3.animateCollapsePanels(r4, r5)
                int r0 = r0 + 1
                goto L_0x0770
            L_0x0797:
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                int r14 = r14.size()
                if (r1 >= r14) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r14 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r14 = r14.mCallbacks
                java.lang.Object r14 = r14.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r14 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r14
                r14.animateExpandNotificationsPanel()
                int r1 = r1 + 1
                goto L_0x0797
            L_0x07b5:
                java.lang.Object r14 = r14.obj
                com.android.internal.os.SomeArgs r14 = (com.android.internal.os.SomeArgs) r14
                r0 = r1
            L_0x07ba:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r0 >= r3) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r0)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                int r4 = r14.argi1
                int r5 = r14.argi2
                int r6 = r14.argi3
                int r7 = r14.argi4
                if (r7 == 0) goto L_0x07de
                r7 = r2
                goto L_0x07df
            L_0x07de:
                r7 = r1
            L_0x07df:
                r3.disable(r4, r5, r6, r7)
                int r0 = r0 + 1
                goto L_0x07ba
            L_0x07e5:
                int r0 = r14.arg1
                if (r0 == r2) goto L_0x080f
                r2 = 2
                if (r0 == r2) goto L_0x07ed
                goto L_0x0839
            L_0x07ed:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.obj
                java.lang.String r2 = (java.lang.String) r2
                r0.removeIcon(r2)
                int r1 = r1 + 1
                goto L_0x07ed
            L_0x080f:
                java.lang.Object r14 = r14.obj
                android.util.Pair r14 = (android.util.Pair) r14
            L_0x0813:
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                int r0 = r0.size()
                if (r1 >= r0) goto L_0x0839
                com.android.systemui.statusbar.CommandQueue r0 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r0 = r0.mCallbacks
                java.lang.Object r0 = r0.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r0 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r0
                java.lang.Object r2 = r14.first
                java.lang.String r2 = (java.lang.String) r2
                java.lang.Object r3 = r14.second
                com.android.internal.statusbar.StatusBarIcon r3 = (com.android.internal.statusbar.StatusBarIcon) r3
                r0.setIcon(r2, r3)
                int r1 = r1 + 1
                goto L_0x0813
            L_0x0839:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.CommandQueue.C1151H.handleMessage(android.os.Message):void");
        }
    }

    public void onDisplayAdded(int i) {
    }

    public void onDisplayChanged(int i) {
    }

    public void topAppWindowChanged(int i, boolean z) {
    }

    public CommandQueue(Context context) {
        ((DisplayManager) context.getSystemService(DisplayManager.class)).registerDisplayListener(this, this.mHandler);
        setDisabled(0, 0, 0);
    }

    public void onDisplayRemoved(int i) {
        synchronized (this.mLock) {
            this.mDisplayDisabled.remove(i);
        }
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            ((Callbacks) this.mCallbacks.get(size)).onDisplayRemoved(i);
        }
    }

    public boolean panelsEnabled() {
        int disabled1 = getDisabled1(0);
        int disabled2 = getDisabled2(0);
        if ((disabled1 & 65536) == 0 && (disabled2 & 4) == 0 && !StatusBar.ONLY_CORE_APPS) {
            return true;
        }
        return false;
    }

    public void addCallback(Callbacks callbacks) {
        this.mCallbacks.add(callbacks);
        for (int i = 0; i < this.mDisplayDisabled.size(); i++) {
            int keyAt = this.mDisplayDisabled.keyAt(i);
            callbacks.disable(keyAt, getDisabled1(keyAt), getDisabled2(keyAt), false);
        }
    }

    public void removeCallback(Callbacks callbacks) {
        this.mCallbacks.remove(callbacks);
    }

    public void setIcon(String str, StatusBarIcon statusBarIcon) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 1, 0, new Pair(str, statusBarIcon)).sendToTarget();
        }
    }

    public void removeIcon(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 2, 0, str).sendToTarget();
        }
    }

    public void disable(int i, int i2, int i3, boolean z) {
        synchronized (this.mLock) {
            if (Build.DEBUG_ONEPLUS) {
                int disabled1 = getDisabled1(i);
                int disabled2 = getDisabled2(i);
                if (!(disabled1 == i2 && disabled2 == i3)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("disable: s1=0x");
                    sb.append(Integer.toHexString(disabled1));
                    sb.append("->0x");
                    sb.append(Integer.toHexString(i2));
                    sb.append(", s2=0x");
                    sb.append(Integer.toHexString(disabled2));
                    sb.append("->0x");
                    sb.append(Integer.toHexString(i3));
                    Log.d("CommandQueue", sb.toString());
                }
            }
            setDisabled(i, i2, i3);
            this.mHandler.removeMessages(131072);
            SomeArgs obtain = SomeArgs.obtain();
            obtain.argi1 = i;
            obtain.argi2 = i2;
            obtain.argi3 = i3;
            obtain.argi4 = z ? 1 : 0;
            Message obtainMessage = this.mHandler.obtainMessage(131072, obtain);
            if (Looper.myLooper() == this.mHandler.getLooper()) {
                this.mHandler.handleMessage(obtainMessage);
                obtainMessage.recycle();
            } else {
                obtainMessage.sendToTarget();
            }
        }
    }

    public void disable(int i, int i2, int i3) {
        disable(i, i2, i3, true);
    }

    public void recomputeDisableFlags(int i, boolean z) {
        synchronized (this.mLock) {
            disable(i, getDisabled1(i), getDisabled2(i), z);
        }
    }

    private void setDisabled(int i, int i2, int i3) {
        this.mDisplayDisabled.put(i, new Pair(Integer.valueOf(i2), Integer.valueOf(i3)));
    }

    private int getDisabled1(int i) {
        return ((Integer) getDisabled(i).first).intValue();
    }

    private int getDisabled2(int i) {
        return ((Integer) getDisabled(i).second).intValue();
    }

    private Pair<Integer, Integer> getDisabled(int i) {
        Pair<Integer, Integer> pair = (Pair) this.mDisplayDisabled.get(i);
        if (pair != null) {
            return pair;
        }
        Pair<Integer, Integer> pair2 = new Pair<>(Integer.valueOf(0), Integer.valueOf(0));
        this.mDisplayDisabled.put(i, pair2);
        return pair2;
    }

    public void animateExpandNotificationsPanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(196608);
            this.mHandler.sendEmptyMessage(196608);
        }
    }

    public void animateCollapsePanels() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(262144);
            this.mHandler.obtainMessage(262144, 0, 0).sendToTarget();
        }
    }

    public void animateCollapsePanels(int i, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(262144);
            this.mHandler.obtainMessage(262144, i, z ? 1 : 0).sendToTarget();
        }
    }

    public void togglePanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2293760);
            this.mHandler.obtainMessage(2293760, 0, 0).sendToTarget();
        }
    }

    public void animateExpandSettingsPanel(String str) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(327680);
            this.mHandler.obtainMessage(327680, str).sendToTarget();
        }
    }

    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.argi1 = i;
            obtain.argi2 = i2;
            obtain.argi3 = i3;
            obtain.argi4 = i4;
            obtain.argi5 = i5;
            obtain.argi6 = z ? 1 : 0;
            obtain.arg1 = rect;
            obtain.arg2 = rect2;
            this.mHandler.obtainMessage(393216, obtain).sendToTarget();
        }
    }

    public void setImeWindowStatus(int i, IBinder iBinder, int i2, int i3, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(524288);
            SomeArgs obtain = SomeArgs.obtain();
            obtain.argi1 = i;
            obtain.argi2 = i2;
            obtain.argi3 = i3;
            obtain.argi4 = z ? 1 : 0;
            obtain.arg1 = iBinder;
            this.mHandler.obtainMessage(524288, obtain).sendToTarget();
        }
    }

    public void showRecentApps(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(851968);
            this.mHandler.obtainMessage(851968, z ? 1 : 0, 0, null).sendToTarget();
        }
    }

    public void hideRecentApps(boolean z, boolean z2) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(917504);
            this.mHandler.obtainMessage(917504, z ? 1 : 0, z2 ? 1 : 0, null).sendToTarget();
        }
    }

    public void toggleSplitScreen() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1966080);
            this.mHandler.obtainMessage(1966080, 0, 0, null).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(589824);
            Message obtainMessage = this.mHandler.obtainMessage(589824, 0, 0, null);
            obtainMessage.setAsynchronous(true);
            obtainMessage.sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(655360);
            this.mHandler.obtainMessage(655360, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void dismissKeyboardShortcutsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2097152);
            this.mHandler.obtainMessage(2097152).sendToTarget();
        }
    }

    public void toggleKeyboardShortcutsMenu(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1638400);
            this.mHandler.obtainMessage(1638400, i, 0).sendToTarget();
        }
    }

    public void showPictureInPictureMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1703936);
            this.mHandler.obtainMessage(1703936).sendToTarget();
        }
    }

    public void setWindowState(int i, int i2, int i3) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(786432, i, i2, Integer.valueOf(i3)).sendToTarget();
        }
    }

    public void showScreenPinningRequest(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1179648, i, 0, null).sendToTarget();
        }
    }

    public void appTransitionPending(int i) {
        appTransitionPending(i, false);
    }

    public void appTransitionPending(int i, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1245184, i, z ? 1 : 0).sendToTarget();
        }
    }

    public void appTransitionCancelled(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1310720, i, 0).sendToTarget();
        }
    }

    public void appTransitionStarting(int i, long j, long j2) {
        appTransitionStarting(i, j, j2, false);
    }

    public void appTransitionStarting(int i, long j, long j2, boolean z) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.argi1 = i;
            obtain.argi2 = z ? 1 : 0;
            obtain.arg1 = Long.valueOf(j);
            obtain.arg2 = Long.valueOf(j2);
            this.mHandler.obtainMessage(1376256, obtain).sendToTarget();
        }
    }

    public void appTransitionFinished(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2031616, i, 0).sendToTarget();
        }
    }

    public void showAssistDisclosure() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1441792);
            this.mHandler.obtainMessage(1441792).sendToTarget();
        }
    }

    public void startAssist(Bundle bundle) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1507328);
            this.mHandler.obtainMessage(1507328, bundle).sendToTarget();
        }
    }

    public void onCameraLaunchGestureDetected(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1572864);
            this.mHandler.obtainMessage(1572864, i, 0).sendToTarget();
        }
    }

    public void addQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1769472, componentName).sendToTarget();
        }
    }

    public void remQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1835008, componentName).sendToTarget();
        }
    }

    public void clickQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1900544, componentName).sendToTarget();
        }
    }

    public void handleSystemKey(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2162688, i, 0).sendToTarget();
        }
    }

    public void showPinningEnterExitToast(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2949120, Boolean.valueOf(z)).sendToTarget();
        }
    }

    public void showPinningEscapeToast() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(3014656).sendToTarget();
        }
    }

    public void showGlobalActionsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2228224);
            this.mHandler.obtainMessage(2228224).sendToTarget();
        }
    }

    public void setTopAppHidesStatusBar(boolean z) {
        this.mHandler.removeMessages(2424832);
        this.mHandler.obtainMessage(2424832, z ? 1 : 0, 0).sendToTarget();
    }

    public void showShutdownUi(boolean z, String str) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2359296);
            this.mHandler.obtainMessage(2359296, z ? 1 : 0, 0, str).sendToTarget();
        }
    }

    public void showWirelessChargingAnimation(int i) {
        this.mHandler.removeMessages(2883584);
        this.mHandler.obtainMessage(2883584, i, 0).sendToTarget();
    }

    public void onProposedRotationChanged(int i, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2490368);
            this.mHandler.obtainMessage(2490368, i, z ? 1 : 0, null).sendToTarget();
        }
    }

    public void showBiometricDialog(Bundle bundle, IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal, int i, boolean z, int i2) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = bundle;
            obtain.arg2 = iBiometricServiceReceiverInternal;
            obtain.argi1 = i;
            obtain.arg3 = Boolean.valueOf(z);
            obtain.argi2 = i2;
            this.mHandler.obtainMessage(2555904, obtain).sendToTarget();
        }
    }

    public void onBiometricAuthenticated(boolean z, String str) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = Boolean.valueOf(z);
            obtain.arg2 = str;
            this.mHandler.obtainMessage(2621440, obtain).sendToTarget();
        }
    }

    public void onBiometricHelp(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2686976, str).sendToTarget();
        }
    }

    public void onBiometricError(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2752512, str).sendToTarget();
        }
    }

    public void hideBiometricDialog() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2818048).sendToTarget();
        }
    }

    public void onDisplayReady(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(458752, i, 0).sendToTarget();
        }
    }

    public void onFingerprintAcquired(int i, int i2) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(6750208, i, i2).sendToTarget();
        }
    }

    public void onFingerprintEnrollResult(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(6815744, i, 0).sendToTarget();
        }
    }

    public void onFingerprintAuthenticatedFailed() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(6881280).sendToTarget();
        }
    }

    public void onRecentsAnimationStateChanged(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(3080192, z ? 1 : 0, 0).sendToTarget();
        }
    }

    /* access modifiers changed from: private */
    public void handleShowImeButton(int i, IBinder iBinder, int i2, int i3, boolean z) {
        if (i != -1) {
            if (!InputMethodSystemProperty.MULTI_CLIENT_IME_ENABLED) {
                int i4 = this.mLastUpdatedImeDisplayId;
                if (!(i4 == i || i4 == -1)) {
                    sendImeInvisibleStatusForPrevNavBar();
                }
            }
            for (int i5 = 0; i5 < this.mCallbacks.size(); i5++) {
                int i6 = i;
                IBinder iBinder2 = iBinder;
                int i7 = i2;
                int i8 = i3;
                boolean z2 = z;
                ((Callbacks) this.mCallbacks.get(i5)).notifyImeWindowVisibleStatus(i6, iBinder2, i7, i8, z2);
                ((Callbacks) this.mCallbacks.get(i5)).setImeWindowStatus(i6, iBinder2, i7, i8, z2);
            }
            this.mLastUpdatedImeDisplayId = i;
        }
    }

    private void sendImeInvisibleStatusForPrevNavBar() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callbacks) this.mCallbacks.get(i)).setImeWindowStatus(this.mLastUpdatedImeDisplayId, null, 4, 0, false);
        }
    }

    public void notifyNavBarColorChanged(int i, String str) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(6553600);
            this.mHandler.obtainMessage(6553600, i, 0, str).sendToTarget();
        }
    }

    public void passSystemUIEvent(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(6946816);
            this.mHandler.obtainMessage(6946816, i, 0).sendToTarget();
        }
    }

    public void onPackagePreferencesCleared() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(7012352);
            this.mHandler.obtainMessage(7012352).sendToTarget();
        }
    }

    public void showFodDialog(Bundle bundle, String str) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = bundle;
            obtain.arg2 = str;
            this.mHandler.obtainMessage(7077888, obtain).sendToTarget();
        }
    }

    public void hideFodDialog(Bundle bundle, String str) {
        synchronized (this.mLock) {
            SomeArgs obtain = SomeArgs.obtain();
            obtain.arg1 = bundle;
            obtain.arg2 = str;
            this.mHandler.obtainMessage(7143424, obtain).sendToTarget();
        }
    }

    public void onFingerprintAuthenticatedSuccess() {
        synchronized (this.mLock) {
            this.mHandler.sendEmptyMessage(7208960);
        }
    }

    public void onFingerprintError(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(7274496, i, 0).sendToTarget();
        }
    }

    public void toggleWxBus() {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        StringBuilder sb = new StringBuilder();
        sb.append("toggleWxBus, callingPid:");
        sb.append(callingPid);
        sb.append(", callingUid:");
        sb.append(callingUid);
        Log.d("CommandQueue", sb.toString());
        if (callingUid == 1000) {
            synchronized (this.mLock) {
                this.mHandler.removeMessages(7340032);
                this.mHandler.obtainMessage(7340032).sendToTarget();
            }
            return;
        }
        Log.d("CommandQueue", "toggleWxBus, permission denied");
    }
}
