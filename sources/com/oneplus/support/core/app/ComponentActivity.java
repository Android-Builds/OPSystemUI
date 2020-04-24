package com.oneplus.support.core.app;

import android.app.Activity;
import android.os.Bundle;
import com.oneplus.support.collection.SimpleArrayMap;
import com.oneplus.support.lifecycle.Lifecycle;
import com.oneplus.support.lifecycle.Lifecycle.State;
import com.oneplus.support.lifecycle.LifecycleOwner;
import com.oneplus.support.lifecycle.LifecycleRegistry;
import com.oneplus.support.lifecycle.ReportFragment;

public class ComponentActivity extends Activity implements LifecycleOwner {
    private SimpleArrayMap<Class<? extends Object>, Object> mExtraDataMap = new SimpleArrayMap<>();
    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ReportFragment.injectIfNeededIn(this);
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle bundle) {
        this.mLifecycleRegistry.markState(State.CREATED);
        super.onSaveInstanceState(bundle);
    }

    public Lifecycle getLifecycle() {
        return this.mLifecycleRegistry;
    }
}
