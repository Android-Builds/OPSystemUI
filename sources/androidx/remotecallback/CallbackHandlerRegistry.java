package androidx.remotecallback;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.collection.ArrayMap;
import java.lang.reflect.InvocationTargetException;

public class CallbackHandlerRegistry {
    public static final CallbackHandlerRegistry sInstance = new CallbackHandlerRegistry();
    private final ArrayMap<Class<? extends CallbackReceiver>, ClsHandler> mClsLookup = new ArrayMap<>();

    public interface CallbackHandler<T extends CallbackReceiver> {
        void executeCallback(Context context, T t, Bundle bundle);
    }

    static class ClsHandler {
        CallbackReceiver mCallStub;
        final ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> mHandlers = new ArrayMap<>();

        ClsHandler() {
        }
    }

    /* access modifiers changed from: 0000 */
    public <T extends CallbackReceiver> void ensureInitialized(Class<T> cls) {
        synchronized (this) {
            if (!this.mClsLookup.containsKey(cls)) {
                runInit(cls);
            }
        }
    }

    public <T extends CallbackReceiver> void invokeCallback(Context context, T t, Bundle bundle) {
        Class cls = t.getClass();
        ensureInitialized(cls);
        ClsHandler findMap = findMap(cls);
        String str = "CallbackHandlerRegistry";
        if (findMap == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("No map found for ");
            sb.append(cls.getName());
            Log.e(str, sb.toString());
            return;
        }
        String string = bundle.getString("remotecallback.method");
        CallbackHandler callbackHandler = (CallbackHandler) findMap.mHandlers.get(string);
        if (callbackHandler == null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("No handler found for ");
            sb2.append(string);
            sb2.append(" on ");
            sb2.append(cls.getName());
            Log.e(str, sb2.toString());
            return;
        }
        callbackHandler.executeCallback(context, t, bundle);
    }

    private ClsHandler findMap(Class<?> cls) {
        ClsHandler clsHandler;
        synchronized (this) {
            clsHandler = (ClsHandler) this.mClsLookup.get(cls);
        }
        if (clsHandler != null) {
            return clsHandler;
        }
        if (cls.getSuperclass() != null) {
            return findMap(cls.getSuperclass());
        }
        return null;
    }

    private <T extends CallbackReceiver> void runInit(Class<T> cls) {
        String str = "Unable to initialize ";
        String str2 = "CallbackHandlerRegistry";
        try {
            ClsHandler clsHandler = new ClsHandler();
            this.mClsLookup.put(cls, clsHandler);
            clsHandler.mCallStub = (CallbackReceiver) findInitClass(cls).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (InstantiationException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(cls.getName());
            Log.e(str2, sb.toString(), e);
        } catch (IllegalAccessException e2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);
            sb2.append(cls.getName());
            Log.e(str2, sb2.toString(), e2);
        } catch (InvocationTargetException e3) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(str);
            sb3.append(cls.getName());
            Log.e(str2, sb3.toString(), e3);
        } catch (NoSuchMethodException e4) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append(str);
            sb4.append(cls.getName());
            Log.e(str2, sb4.toString(), e4);
        } catch (ClassNotFoundException e5) {
            StringBuilder sb5 = new StringBuilder();
            sb5.append(str);
            sb5.append(cls.getName());
            Log.e(str2, sb5.toString(), e5);
        }
    }

    private static Class<? extends Runnable> findInitClass(Class<? extends CallbackReceiver> cls) throws ClassNotFoundException {
        return Class.forName(String.format("%s.%sInitializer", new Object[]{cls.getPackage().getName(), cls.getSimpleName()}), false, cls.getClassLoader());
    }
}
