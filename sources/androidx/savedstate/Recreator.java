package androidx.savedstate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleOwner;
import androidx.savedstate.SavedStateRegistry.AutoRecreated;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;

@SuppressLint({"RestrictedApi"})
final class Recreator implements GenericLifecycleObserver {
    private final SavedStateRegistryOwner mOwner;

    Recreator(SavedStateRegistryOwner savedStateRegistryOwner) {
        this.mOwner = savedStateRegistryOwner;
    }

    public void onStateChanged(LifecycleOwner lifecycleOwner, Event event) {
        if (event == Event.ON_CREATE) {
            lifecycleOwner.getLifecycle().removeObserver(this);
            Bundle consumeRestoredStateForKey = this.mOwner.getSavedStateRegistry().consumeRestoredStateForKey("androidx.savedstate.Restarter");
            if (consumeRestoredStateForKey != null) {
                ArrayList stringArrayList = consumeRestoredStateForKey.getStringArrayList("classes_to_restore");
                if (stringArrayList != null) {
                    Iterator it = stringArrayList.iterator();
                    while (it.hasNext()) {
                        reflectiveNew((String) it.next());
                    }
                    return;
                }
                throw new IllegalStateException("Bundle with restored state for the component \"androidx.savedstate.Restarter\" must contain list of strings by the key \"classes_to_restore\"");
            }
            return;
        }
        throw new AssertionError("Next event must be ON_CREATE");
    }

    private void reflectiveNew(String str) {
        try {
            Class asSubclass = Class.forName(str, false, Recreator.class.getClassLoader()).asSubclass(AutoRecreated.class);
            try {
                Constructor declaredConstructor = asSubclass.getDeclaredConstructor(new Class[0]);
                declaredConstructor.setAccessible(true);
                try {
                    ((AutoRecreated) declaredConstructor.newInstance(new Object[0])).onRecreated(this.mOwner);
                } catch (Exception e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Failed to instantiate ");
                    sb.append(str);
                    throw new RuntimeException(sb.toString(), e);
                }
            } catch (NoSuchMethodException e2) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Class");
                sb2.append(asSubclass.getSimpleName());
                sb2.append(" must have default constructor in order to be automatically recreated");
                throw new IllegalStateException(sb2.toString(), e2);
            }
        } catch (ClassNotFoundException e3) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Class ");
            sb3.append(str);
            sb3.append(" wasn't found");
            throw new RuntimeException(sb3.toString(), e3);
        }
    }
}
