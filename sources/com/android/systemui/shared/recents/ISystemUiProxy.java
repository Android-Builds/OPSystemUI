package com.android.systemui.shared.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.view.MotionEvent;

public interface ISystemUiProxy extends IInterface {

    public static abstract class Stub extends Binder implements ISystemUiProxy {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.android.systemui.shared.recents.ISystemUiProxy");
        }

        /* JADX WARNING: type inference failed for: r0v4 */
        /* JADX WARNING: type inference failed for: r0v5, types: [android.view.MotionEvent] */
        /* JADX WARNING: type inference failed for: r0v7, types: [android.view.MotionEvent] */
        /* JADX WARNING: type inference failed for: r0v8, types: [android.os.Bundle] */
        /* JADX WARNING: type inference failed for: r0v10, types: [android.os.Bundle] */
        /* JADX WARNING: type inference failed for: r0v11 */
        /* JADX WARNING: type inference failed for: r0v12 */
        /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r0v4
          assigns: [?[int, float, boolean, short, byte, char, OBJECT, ARRAY], android.os.Bundle, android.view.MotionEvent]
          uses: [android.view.MotionEvent, android.os.Bundle]
          mth insns count: 117
        	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
        	at jadx.core.ProcessClass.process(ProcessClass.java:30)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:49)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:49)
        	at jadx.core.ProcessClass.process(ProcessClass.java:35)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
         */
        /* JADX WARNING: Unknown variable types count: 3 */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onTransact(int r5, android.os.Parcel r6, android.os.Parcel r7, int r8) throws android.os.RemoteException {
            /*
                r4 = this;
                r0 = 2
                r1 = 1
                java.lang.String r2 = "com.android.systemui.shared.recents.ISystemUiProxy"
                if (r5 == r0) goto L_0x0127
                r0 = 1598968902(0x5f4e5446, float:1.4867585E19)
                if (r5 == r0) goto L_0x0123
                r0 = 102(0x66, float:1.43E-43)
                if (r5 == r0) goto L_0x0119
                r0 = 103(0x67, float:1.44E-43)
                if (r5 == r0) goto L_0x010b
                r0 = 0
                r3 = 0
                switch(r5) {
                    case 6: goto L_0x0101;
                    case 7: goto L_0x00f0;
                    case 8: goto L_0x00d9;
                    case 9: goto L_0x00c4;
                    case 10: goto L_0x00ab;
                    default: goto L_0x0018;
                }
            L_0x0018:
                switch(r5) {
                    case 13: goto L_0x009d;
                    case 14: goto L_0x0084;
                    case 15: goto L_0x0065;
                    case 16: goto L_0x0057;
                    case 17: goto L_0x004d;
                    case 18: goto L_0x0043;
                    case 19: goto L_0x0035;
                    case 20: goto L_0x0020;
                    default: goto L_0x001b;
                }
            L_0x001b:
                boolean r4 = super.onTransact(r5, r6, r7, r8)
                return r4
            L_0x0020:
                r6.enforceInterface(r2)
                float r5 = r6.readFloat()
                int r6 = r6.readInt()
                if (r6 == 0) goto L_0x002e
                r3 = r1
            L_0x002e:
                r4.setNavBarButtonAlpha(r5, r3)
                r7.writeNoException()
                return r1
            L_0x0035:
                r6.enforceInterface(r2)
                float r5 = r6.readFloat()
                r4.onAssistantGestureCompletion(r5)
                r7.writeNoException()
                return r1
            L_0x0043:
                r6.enforceInterface(r2)
                r4.stopScreenPinning()
                r7.writeNoException()
                return r1
            L_0x004d:
                r6.enforceInterface(r2)
                r4.notifyAccessibilityButtonLongClicked()
                r7.writeNoException()
                return r1
            L_0x0057:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                r4.notifyAccessibilityButtonClicked(r5)
                r7.writeNoException()
                return r1
            L_0x0065:
                r6.enforceInterface(r2)
                java.lang.String r5 = r6.readString()
                int r6 = r6.readInt()
                android.os.Bundle r4 = r4.monitorGestureInput(r5, r6)
                r7.writeNoException()
                if (r4 == 0) goto L_0x0080
                r7.writeInt(r1)
                r4.writeToParcel(r7, r1)
                goto L_0x0083
            L_0x0080:
                r7.writeInt(r3)
            L_0x0083:
                return r1
            L_0x0084:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                if (r5 == 0) goto L_0x0096
                android.os.Parcelable$Creator r5 = android.os.Bundle.CREATOR
                java.lang.Object r5 = r5.createFromParcel(r6)
                r0 = r5
                android.os.Bundle r0 = (android.os.Bundle) r0
            L_0x0096:
                r4.startAssistant(r0)
                r7.writeNoException()
                return r1
            L_0x009d:
                r6.enforceInterface(r2)
                float r5 = r6.readFloat()
                r4.onAssistantProgress(r5)
                r7.writeNoException()
                return r1
            L_0x00ab:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                if (r5 == 0) goto L_0x00bd
                android.os.Parcelable$Creator r5 = android.view.MotionEvent.CREATOR
                java.lang.Object r5 = r5.createFromParcel(r6)
                r0 = r5
                android.view.MotionEvent r0 = (android.view.MotionEvent) r0
            L_0x00bd:
                r4.onStatusBarMotionEvent(r0)
                r7.writeNoException()
                return r1
            L_0x00c4:
                r6.enforceInterface(r2)
                float r5 = r6.readFloat()
                int r6 = r6.readInt()
                if (r6 == 0) goto L_0x00d2
                r3 = r1
            L_0x00d2:
                r4.setBackButtonAlpha(r5, r3)
                r7.writeNoException()
                return r1
            L_0x00d9:
                r6.enforceInterface(r2)
                android.graphics.Rect r4 = r4.getNonMinimizedSplitScreenSecondaryBounds()
                r7.writeNoException()
                if (r4 == 0) goto L_0x00ec
                r7.writeInt(r1)
                r4.writeToParcel(r7, r1)
                goto L_0x00ef
            L_0x00ec:
                r7.writeInt(r3)
            L_0x00ef:
                return r1
            L_0x00f0:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                if (r5 == 0) goto L_0x00fa
                r3 = r1
            L_0x00fa:
                r4.onOverviewShown(r3)
                r7.writeNoException()
                return r1
            L_0x0101:
                r6.enforceInterface(r2)
                r4.onSplitScreenInvoked()
                r7.writeNoException()
                return r1
            L_0x010b:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                r4.notifyGestureEnded(r5)
                r7.writeNoException()
                return r1
            L_0x0119:
                r6.enforceInterface(r2)
                r4.notifyGestureStarted()
                r7.writeNoException()
                return r1
            L_0x0123:
                r7.writeString(r2)
                return r1
            L_0x0127:
                r6.enforceInterface(r2)
                int r5 = r6.readInt()
                r4.startScreenPinning(r5)
                r7.writeNoException()
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.shared.recents.ISystemUiProxy.Stub.onTransact(int, android.os.Parcel, android.os.Parcel, int):boolean");
        }
    }

    Rect getNonMinimizedSplitScreenSecondaryBounds() throws RemoteException;

    Bundle monitorGestureInput(String str, int i) throws RemoteException;

    void notifyAccessibilityButtonClicked(int i) throws RemoteException;

    void notifyAccessibilityButtonLongClicked() throws RemoteException;

    void notifyGestureEnded(int i) throws RemoteException;

    void notifyGestureStarted() throws RemoteException;

    void onAssistantGestureCompletion(float f) throws RemoteException;

    void onAssistantProgress(float f) throws RemoteException;

    void onOverviewShown(boolean z) throws RemoteException;

    void onSplitScreenInvoked() throws RemoteException;

    void onStatusBarMotionEvent(MotionEvent motionEvent) throws RemoteException;

    void setBackButtonAlpha(float f, boolean z) throws RemoteException;

    void setNavBarButtonAlpha(float f, boolean z) throws RemoteException;

    void startAssistant(Bundle bundle) throws RemoteException;

    void startScreenPinning(int i) throws RemoteException;

    void stopScreenPinning() throws RemoteException;
}
