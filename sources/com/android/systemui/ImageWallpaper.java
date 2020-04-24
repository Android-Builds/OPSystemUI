package com.android.systemui;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperService.Engine;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.glwallpaper.EglHelper;
import com.android.systemui.glwallpaper.GLWallpaperRenderer;
import com.android.systemui.glwallpaper.GLWallpaperRenderer.SurfaceProxy;
import com.android.systemui.glwallpaper.ImageWallpaperRenderer;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.phone.DozeParameters;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ImageWallpaper extends WallpaperService {
    /* access modifiers changed from: private */
    public static final String TAG = "ImageWallpaper";
    /* access modifiers changed from: private */
    public HandlerThread mWorker;

    class GLEngine extends Engine implements SurfaceProxy, StateListener {
        @VisibleForTesting
        static final int MIN_SURFACE_HEIGHT = 64;
        @VisibleForTesting
        static final int MIN_SURFACE_WIDTH = 64;
        private StatusBarStateController mController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
        private EglHelper mEglHelper;
        private final Runnable mFinishRenderingTask = new Runnable() {
            public final void run() {
                GLEngine.this.finishRendering();
            }
        };
        private final Object mMonitor = new Object();
        private boolean mNeedRedraw;
        private final boolean mNeedTransition = false;
        private GLWallpaperRenderer mRenderer;
        private boolean mWaitingForRendering;

        GLEngine(Context context) {
            super(ImageWallpaper.this);
            StatusBarStateController statusBarStateController = this.mController;
            if (statusBarStateController != null) {
                statusBarStateController.addCallback(this);
            }
            this.mEglHelper = new EglHelper();
            this.mRenderer = new ImageWallpaperRenderer(context, this);
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            setFixedSizeAllowed(true);
            setOffsetNotificationsEnabled(true);
            updateSurfaceSize();
        }

        private void updateSurfaceSize() {
            SurfaceHolder surfaceHolder = getSurfaceHolder();
            Size reportSurfaceSize = this.mRenderer.reportSurfaceSize();
            surfaceHolder.setFixedSize(Math.max(64, reportSurfaceSize.getWidth()), Math.max(64, reportSurfaceSize.getHeight()));
        }

        public /* synthetic */ void lambda$onOffsetsChanged$0$ImageWallpaper$GLEngine(float f, float f2) {
            this.mRenderer.updateOffsets(f, f2);
        }

        public void onOffsetsChanged(float f, float f2, float f3, float f4, int i, int i2) {
            ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable(f, f2) {
                private final /* synthetic */ float f$1;
                private final /* synthetic */ float f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    GLEngine.this.lambda$onOffsetsChanged$0$ImageWallpaper$GLEngine(this.f$1, this.f$2);
                }
            });
        }

        public void onAmbientModeChanged(boolean z, long j) {
            if (this.mNeedTransition) {
                ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable(z, j) {
                    private final /* synthetic */ boolean f$1;
                    private final /* synthetic */ long f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        GLEngine.this.lambda$onAmbientModeChanged$1$ImageWallpaper$GLEngine(this.f$1, this.f$2);
                    }
                });
                if (z && j == 0) {
                    waitForBackgroundRendering();
                }
            }
        }

        public /* synthetic */ void lambda$onAmbientModeChanged$1$ImageWallpaper$GLEngine(boolean z, long j) {
            this.mRenderer.updateAmbientMode(z, j);
        }

        /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x0022 */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void waitForBackgroundRendering() {
            /*
                r7 = this;
                java.lang.Object r0 = r7.mMonitor
                monitor-enter(r0)
                r1 = 0
                r2 = 1
                r7.mWaitingForRendering = r2     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                r3 = r2
            L_0x0008:
                boolean r4 = r7.mWaitingForRendering     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                if (r4 == 0) goto L_0x0022
                java.lang.Object r4 = r7.mMonitor     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                r5 = 100
                r4.wait(r5)     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                boolean r4 = r7.mWaitingForRendering     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                r5 = 10
                if (r3 >= r5) goto L_0x001b
                r5 = r2
                goto L_0x001c
            L_0x001b:
                r5 = r1
            L_0x001c:
                r4 = r4 & r5
                r7.mWaitingForRendering = r4     // Catch:{ InterruptedException -> 0x0022, all -> 0x0025 }
                int r3 = r3 + 1
                goto L_0x0008
            L_0x0022:
                r7.mWaitingForRendering = r1     // Catch:{ all -> 0x002b }
                goto L_0x0029
            L_0x0025:
                r2 = move-exception
                r7.mWaitingForRendering = r1     // Catch:{ all -> 0x002b }
                throw r2     // Catch:{ all -> 0x002b }
            L_0x0029:
                monitor-exit(r0)     // Catch:{ all -> 0x002b }
                return
            L_0x002b:
                r7 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x002b }
                throw r7
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.ImageWallpaper.GLEngine.waitForBackgroundRendering():void");
        }

        public void onDestroy() {
            StatusBarStateController statusBarStateController = this.mController;
            if (statusBarStateController != null) {
                statusBarStateController.removeCallback(this);
            }
            this.mController = null;
            ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable() {
                public final void run() {
                    GLEngine.this.lambda$onDestroy$2$ImageWallpaper$GLEngine();
                }
            });
        }

        public /* synthetic */ void lambda$onDestroy$2$ImageWallpaper$GLEngine() {
            this.mRenderer.finish();
            this.mRenderer = null;
            this.mEglHelper.finish();
            this.mEglHelper = null;
            getSurfaceHolder().getSurface().hwuiDestroy();
        }

        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable(surfaceHolder) {
                private final /* synthetic */ SurfaceHolder f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    GLEngine.this.lambda$onSurfaceCreated$3$ImageWallpaper$GLEngine(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onSurfaceCreated$3$ImageWallpaper$GLEngine(SurfaceHolder surfaceHolder) {
            this.mEglHelper.init(surfaceHolder);
            this.mRenderer.onSurfaceCreated();
        }

        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable(i2, i3) {
                private final /* synthetic */ int f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    GLEngine.this.lambda$onSurfaceChanged$4$ImageWallpaper$GLEngine(this.f$1, this.f$2);
                }
            });
        }

        public /* synthetic */ void lambda$onSurfaceChanged$4$ImageWallpaper$GLEngine(int i, int i2) {
            this.mRenderer.onSurfaceChanged(i, i2);
            this.mNeedRedraw = true;
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
            ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable() {
                public final void run() {
                    GLEngine.this.lambda$onSurfaceRedrawNeeded$5$ImageWallpaper$GLEngine();
                }
            });
        }

        public /* synthetic */ void lambda$onSurfaceRedrawNeeded$5$ImageWallpaper$GLEngine() {
            if (this.mNeedRedraw) {
                preRender();
                requestRender();
                postRender();
                this.mNeedRedraw = false;
            }
        }

        public void onStatePostChange() {
            if (this.mController.getState() == 0) {
                ImageWallpaper.this.mWorker.getThreadHandler().post(new Runnable() {
                    public final void run() {
                        GLEngine.this.scheduleFinishRendering();
                    }
                });
            }
        }

        public void preRender() {
            preRenderInternal();
        }

        private void preRenderInternal() {
            boolean z;
            Rect surfaceFrame = getSurfaceHolder().getSurfaceFrame();
            cancelFinishRenderingTask();
            if (!this.mEglHelper.hasEglContext()) {
                this.mEglHelper.destroyEglSurface();
                if (!this.mEglHelper.createEglContext()) {
                    Log.w(ImageWallpaper.TAG, "recreate egl context failed!");
                } else {
                    z = true;
                    if (this.mEglHelper.hasEglContext() && !this.mEglHelper.hasEglSurface() && !this.mEglHelper.createEglSurface(getSurfaceHolder())) {
                        Log.w(ImageWallpaper.TAG, "recreate egl surface failed!");
                    }
                    if (this.mEglHelper.hasEglContext() && this.mEglHelper.hasEglSurface() && z) {
                        this.mRenderer.onSurfaceCreated();
                        this.mRenderer.onSurfaceChanged(surfaceFrame.width(), surfaceFrame.height());
                        return;
                    }
                    return;
                }
            }
            z = false;
            Log.w(ImageWallpaper.TAG, "recreate egl surface failed!");
            if (this.mEglHelper.hasEglContext()) {
            }
        }

        public void requestRender() {
            requestRenderInternal();
        }

        private void requestRenderInternal() {
            Rect surfaceFrame = getSurfaceHolder().getSurfaceFrame();
            if (this.mEglHelper.hasEglContext() && this.mEglHelper.hasEglSurface() && surfaceFrame.width() > 0 && surfaceFrame.height() > 0) {
                this.mRenderer.onDrawFrame();
                if (!this.mEglHelper.swapBuffer()) {
                    Log.e(ImageWallpaper.TAG, "drawFrame failed!");
                    return;
                }
                return;
            }
            String access$100 = ImageWallpaper.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("requestRender: not ready, has context=");
            sb.append(this.mEglHelper.hasEglContext());
            sb.append(", has surface=");
            sb.append(this.mEglHelper.hasEglSurface());
            sb.append(", frame=");
            sb.append(surfaceFrame);
            Log.e(access$100, sb.toString());
        }

        public void postRender() {
            notifyWaitingThread();
            scheduleFinishRendering();
        }

        /* JADX WARNING: Can't wrap try/catch for region: R(5:2|3|(3:5|6|7)|8|9) */
        /* JADX WARNING: Missing exception handler attribute for start block: B:8:0x000f */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void notifyWaitingThread() {
            /*
                r2 = this;
                java.lang.Object r0 = r2.mMonitor
                monitor-enter(r0)
                boolean r1 = r2.mWaitingForRendering     // Catch:{ all -> 0x0011 }
                if (r1 == 0) goto L_0x000f
                r1 = 0
                r2.mWaitingForRendering = r1     // Catch:{ IllegalMonitorStateException -> 0x000f }
                java.lang.Object r2 = r2.mMonitor     // Catch:{ IllegalMonitorStateException -> 0x000f }
                r2.notify()     // Catch:{ IllegalMonitorStateException -> 0x000f }
            L_0x000f:
                monitor-exit(r0)     // Catch:{ all -> 0x0011 }
                return
            L_0x0011:
                r2 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x0011 }
                throw r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.ImageWallpaper.GLEngine.notifyWaitingThread():void");
        }

        private void cancelFinishRenderingTask() {
            ImageWallpaper.this.mWorker.getThreadHandler().removeCallbacks(this.mFinishRenderingTask);
        }

        /* access modifiers changed from: private */
        public void scheduleFinishRendering() {
            cancelFinishRenderingTask();
            ImageWallpaper.this.mWorker.getThreadHandler().postDelayed(this.mFinishRenderingTask, 1000);
        }

        /* access modifiers changed from: private */
        public void finishRendering() {
            EglHelper eglHelper = this.mEglHelper;
            if (eglHelper != null) {
                eglHelper.destroyEglSurface();
                if (!needPreserveEglContext()) {
                    this.mEglHelper.destroyEglContext();
                }
            }
        }

        private boolean needPreserveEglContext() {
            if (this.mNeedTransition) {
                StatusBarStateController statusBarStateController = this.mController;
                if (statusBarStateController != null && statusBarStateController.getState() == 1) {
                    return true;
                }
            }
            return false;
        }

        /* access modifiers changed from: protected */
        public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            super.dump(str, fileDescriptor, printWriter, strArr);
            printWriter.print(str);
            printWriter.print("Engine=");
            printWriter.println(this);
            boolean isHighEndGfx = ActivityManager.isHighEndGfx();
            printWriter.print(str);
            printWriter.print("isHighEndGfx=");
            printWriter.println(isHighEndGfx);
            DozeParameters instance = DozeParameters.getInstance(ImageWallpaper.this.getApplicationContext());
            printWriter.print(str);
            printWriter.print("displayNeedsBlanking=");
            Object obj = "null";
            printWriter.println(instance != null ? Boolean.valueOf(instance.getDisplayNeedsBlanking()) : obj);
            printWriter.print(str);
            printWriter.print("mNeedTransition=");
            printWriter.println(this.mNeedTransition);
            printWriter.print(str);
            printWriter.print("StatusBarState=");
            StatusBarStateController statusBarStateController = this.mController;
            printWriter.println(statusBarStateController != null ? Integer.valueOf(statusBarStateController.getState()) : obj);
            printWriter.print(str);
            printWriter.print("valid surface=");
            printWriter.println((getSurfaceHolder() == null || getSurfaceHolder().getSurface() == null) ? obj : Boolean.valueOf(getSurfaceHolder().getSurface().isValid()));
            printWriter.print(str);
            printWriter.print("surface frame=");
            if (getSurfaceHolder() != null) {
                obj = getSurfaceHolder().getSurfaceFrame();
            }
            printWriter.println(obj);
            this.mEglHelper.dump(str, fileDescriptor, printWriter, strArr);
            this.mRenderer.dump(str, fileDescriptor, printWriter, strArr);
        }
    }

    public void onCreate() {
        super.onCreate();
        this.mWorker = new HandlerThread(TAG);
        this.mWorker.start();
    }

    public Engine onCreateEngine() {
        return new GLEngine(this);
    }

    public void onDestroy() {
        super.onDestroy();
        this.mWorker.quitSafely();
        this.mWorker = null;
    }
}
