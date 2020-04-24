package com.oneplus.faceunlock.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOPFaceSettingService extends IInterface {

    public static abstract class Stub extends Binder implements IOPFaceSettingService {

        private static class Proxy implements IOPFaceSettingService {
            public static IOPFaceSettingService sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public int checkState(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.oneplus.faceunlock.internal.IOPFaceSettingService");
                    obtain.writeInt(i);
                    if (!this.mRemote.transact(1, obtain, obtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().checkState(i);
                    }
                    obtain2.readException();
                    int readInt = obtain2.readInt();
                    obtain2.recycle();
                    obtain.recycle();
                    return readInt;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }

        public static IOPFaceSettingService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.oneplus.faceunlock.internal.IOPFaceSettingService");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOPFaceSettingService)) {
                return new Proxy(iBinder);
            }
            return (IOPFaceSettingService) queryLocalInterface;
        }

        public static IOPFaceSettingService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }

    int checkState(int i) throws RemoteException;
}
