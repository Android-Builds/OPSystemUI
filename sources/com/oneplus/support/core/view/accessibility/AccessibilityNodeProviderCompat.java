package com.oneplus.support.core.view.accessibility;

public class AccessibilityNodeProviderCompat {
    private final Object mProvider;

    public AccessibilityNodeProviderCompat(Object obj) {
        this.mProvider = obj;
    }

    public Object getProvider() {
        return this.mProvider;
    }
}
