package com.oneplus.lib.design.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;
import android.widget.ImageButton;
import com.oneplus.support.core.view.AccessibilityDelegateCompat;
import com.oneplus.support.core.view.ViewCompat;
import com.oneplus.support.core.view.accessibility.AccessibilityNodeInfoCompat;

public class OPCheckableImageButton extends ImageButton implements Checkable {
    private static final int[] DRAWABLE_STATE_CHECKED = {16842912};
    private boolean mChecked;

    public OPCheckableImageButton(Context context) {
        this(context, null);
    }

    public OPCheckableImageButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842866);
    }

    public OPCheckableImageButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                super.onInitializeAccessibilityEvent(view, accessibilityEvent);
                accessibilityEvent.setChecked(OPCheckableImageButton.this.isChecked());
            }

            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfoCompat);
                accessibilityNodeInfoCompat.setCheckable(true);
                accessibilityNodeInfoCompat.setChecked(OPCheckableImageButton.this.isChecked());
            }
        });
    }

    public void setChecked(boolean z) {
        if (this.mChecked != z) {
            this.mChecked = z;
            refreshDrawableState();
            sendAccessibilityEvent(2048);
        }
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public void toggle() {
        setChecked(!this.mChecked);
    }

    public int[] onCreateDrawableState(int i) {
        if (this.mChecked) {
            return ImageButton.mergeDrawableStates(super.onCreateDrawableState(i + DRAWABLE_STATE_CHECKED.length), DRAWABLE_STATE_CHECKED);
        }
        return super.onCreateDrawableState(i);
    }
}
