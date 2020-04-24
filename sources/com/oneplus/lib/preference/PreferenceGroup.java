package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$styleable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PreferenceGroup extends Preference implements GenericInflater$Parent<Preference> {
    private boolean mAttachedToActivity;
    private int mCurrentPreferenceOrder;
    private boolean mOrderingAsAdded;
    private List<Preference> mPreferenceList;

    /* access modifiers changed from: protected */
    public boolean isOnSameScreenAsChildren() {
        return true;
    }

    public PreferenceGroup(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mOrderingAsAdded = true;
        this.mCurrentPreferenceOrder = 0;
        this.mAttachedToActivity = false;
        this.mPreferenceList = new ArrayList();
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.PreferenceGroup, i, i2);
        this.mOrderingAsAdded = obtainStyledAttributes.getBoolean(R$styleable.PreferenceGroup_android_orderingFromXml, this.mOrderingAsAdded);
        obtainStyledAttributes.recycle();
    }

    public PreferenceGroup(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PreferenceGroup(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public int getPreferenceCount() {
        return this.mPreferenceList.size();
    }

    public Preference getPreference(int i) {
        return (Preference) this.mPreferenceList.get(i);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToActivity() {
        super.onAttachedToActivity();
        this.mAttachedToActivity = true;
        int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onAttachedToActivity();
        }
    }

    public void notifyDependencyChange(boolean z) {
        super.notifyDependencyChange(z);
        int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onParentChanged(this, z);
        }
    }

    /* access modifiers changed from: 0000 */
    public void sortPreferences() {
        synchronized (this) {
            Collections.sort(this.mPreferenceList);
        }
    }
}
