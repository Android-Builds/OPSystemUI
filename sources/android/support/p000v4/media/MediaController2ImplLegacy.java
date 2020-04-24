package android.support.p000v4.media;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.ResultReceiver;
import android.support.p000v4.media.MediaController2.ControllerCallback;
import android.support.p000v4.media.session.MediaControllerCompat.Callback;
import android.util.Log;
import java.util.concurrent.Executor;

@TargetApi(16)
/* renamed from: android.support.v4.media.MediaController2ImplLegacy */
class MediaController2ImplLegacy implements SupportLibraryImpl {
    private static final boolean DEBUG = Log.isLoggable("MC2ImplLegacy", 3);
    static final Bundle sDefaultRootExtras = new Bundle();
    /* access modifiers changed from: private */
    public final ControllerCallback mCallback;
    /* access modifiers changed from: private */
    public final Executor mCallbackExecutor;
    /* access modifiers changed from: private */
    public final HandlerThread mHandlerThread;
    /* access modifiers changed from: private */
    public MediaController2 mInstance;

    /* renamed from: android.support.v4.media.MediaController2ImplLegacy$3 */
    class C00433 extends ResultReceiver {
        final /* synthetic */ MediaController2ImplLegacy this$0;

        /* access modifiers changed from: protected */
        public void onReceiveResult(int i, Bundle bundle) {
            if (this.this$0.mHandlerThread.isAlive()) {
                if (i == -1) {
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            C00433.this.this$0.mCallback.onDisconnected(C00433.this.this$0.mInstance);
                        }
                    });
                    this.this$0.close();
                    throw null;
                } else if (i == 0) {
                    this.this$0.onConnectedNotLocked(bundle);
                    throw null;
                }
            }
        }
    }

    /* renamed from: android.support.v4.media.MediaController2ImplLegacy$ControllerCompatCallback */
    private final class ControllerCompatCallback extends Callback {
        final /* synthetic */ MediaController2ImplLegacy this$0;

        /* renamed from: android.support.v4.media.MediaController2ImplLegacy$ControllerCompatCallback$1 */
        class C00451 extends ResultReceiver {
            final /* synthetic */ ControllerCompatCallback this$1;

            /* access modifiers changed from: protected */
            public void onReceiveResult(int i, Bundle bundle) {
                if (this.this$1.this$0.mHandlerThread.isAlive()) {
                    if (i == -1) {
                        this.this$1.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                C00451.this.this$1.this$0.mCallback.onDisconnected(C00451.this.this$1.this$0.mInstance);
                            }
                        });
                        this.this$1.this$0.close();
                        throw null;
                    } else if (i == 0) {
                        this.this$1.this$0.onConnectedNotLocked(bundle);
                        throw null;
                    }
                }
            }
        }
    }

    public void close() {
        throw null;
    }

    /* access modifiers changed from: 0000 */
    public void onConnectedNotLocked(Bundle bundle) {
        throw null;
    }

    static {
        sDefaultRootExtras.putBoolean("android.support.v4.media.root_default_root", true);
    }
}
