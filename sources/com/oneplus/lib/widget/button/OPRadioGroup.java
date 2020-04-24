package com.oneplus.lib.widget.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import com.oneplus.commonctrl.R$styleable;

public class OPRadioGroup extends LinearLayout {
    /* access modifiers changed from: private */
    public int mCheckedId = -1;
    /* access modifiers changed from: private */
    public com.oneplus.lib.widget.button.OPCompoundButton.OnCheckedChangeListener mChildOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private PassThroughHierarchyChangeListener mPassThroughListener;
    /* access modifiers changed from: private */
    public boolean mProtectFromCheckedChange = false;

    private class CheckedStateTracker implements com.oneplus.lib.widget.button.OPCompoundButton.OnCheckedChangeListener {
        private CheckedStateTracker() {
        }

        public void onCheckedChanged(OPCompoundButton oPCompoundButton, boolean z) {
            if (!OPRadioGroup.this.mProtectFromCheckedChange) {
                OPRadioGroup.this.mProtectFromCheckedChange = true;
                if (OPRadioGroup.this.mCheckedId != -1) {
                    OPRadioGroup oPRadioGroup = OPRadioGroup.this;
                    oPRadioGroup.setCheckedStateForView(oPRadioGroup.mCheckedId, false);
                }
                OPRadioGroup.this.mProtectFromCheckedChange = false;
                OPRadioGroup.this.setCheckedId(oPCompoundButton.getId());
            }
        }
    }

    public static class LayoutParams extends android.widget.LinearLayout.LayoutParams {
        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }

        /* access modifiers changed from: protected */
        public void setBaseAttributes(TypedArray typedArray, int i, int i2) {
            if (typedArray.hasValue(i)) {
                this.width = typedArray.getLayoutDimension(i, "layout_width");
            } else {
                this.width = -2;
            }
            if (typedArray.hasValue(i2)) {
                this.height = typedArray.getLayoutDimension(i2, "layout_height");
            } else {
                this.height = -2;
            }
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(OPRadioGroup oPRadioGroup, int i);
    }

    private class PassThroughHierarchyChangeListener implements OnHierarchyChangeListener {
        /* access modifiers changed from: private */
        public OnHierarchyChangeListener mOnHierarchyChangeListener;

        private PassThroughHierarchyChangeListener() {
        }

        public void onChildViewAdded(View view, View view2) {
            if (view == OPRadioGroup.this && (view2 instanceof OPRadioButton)) {
                if (view2.getId() == -1) {
                    view2.setId(View.generateViewId());
                }
                ((OPRadioButton) view2).setOnCheckedChangeWidgetListener(OPRadioGroup.this.mChildOnCheckedChangeListener);
            }
            OnHierarchyChangeListener onHierarchyChangeListener = this.mOnHierarchyChangeListener;
            if (onHierarchyChangeListener != null) {
                onHierarchyChangeListener.onChildViewAdded(view, view2);
            }
        }

        public void onChildViewRemoved(View view, View view2) {
            if (view == OPRadioGroup.this && (view2 instanceof OPRadioButton)) {
                ((OPRadioButton) view2).setOnCheckedChangeWidgetListener(null);
            }
            OnHierarchyChangeListener onHierarchyChangeListener = this.mOnHierarchyChangeListener;
            if (onHierarchyChangeListener != null) {
                onHierarchyChangeListener.onChildViewRemoved(view, view2);
            }
        }
    }

    public OPRadioGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPRadioGroup, 16842878, 0);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.OPRadioGroup_android_checkedButton, -1);
        if (resourceId != -1) {
            this.mCheckedId = resourceId;
        }
        setOrientation(obtainStyledAttributes.getInt(R$styleable.OPRadioGroup_android_orientation, 1));
        obtainStyledAttributes.recycle();
        init();
    }

    private void init() {
        this.mChildOnCheckedChangeListener = new CheckedStateTracker();
        this.mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(this.mPassThroughListener);
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mPassThroughListener.mOnHierarchyChangeListener = onHierarchyChangeListener;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        int i = this.mCheckedId;
        if (i != -1) {
            this.mProtectFromCheckedChange = true;
            setCheckedStateForView(i, true);
            this.mProtectFromCheckedChange = false;
            setCheckedId(this.mCheckedId);
        }
    }

    public void addView(View view, int i, android.view.ViewGroup.LayoutParams layoutParams) {
        if (view instanceof OPRadioButton) {
            OPRadioButton oPRadioButton = (OPRadioButton) view;
            if (oPRadioButton.isChecked()) {
                this.mProtectFromCheckedChange = true;
                int i2 = this.mCheckedId;
                if (i2 != -1) {
                    setCheckedStateForView(i2, false);
                }
                this.mProtectFromCheckedChange = false;
                setCheckedId(oPRadioButton.getId());
            }
        }
        super.addView(view, i, layoutParams);
    }

    /* access modifiers changed from: private */
    public void setCheckedId(int i) {
        this.mCheckedId = i;
        OnCheckedChangeListener onCheckedChangeListener = this.mOnCheckedChangeListener;
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(this, this.mCheckedId);
        }
    }

    /* access modifiers changed from: private */
    public void setCheckedStateForView(int i, boolean z) {
        View findViewById = findViewById(i);
        if (findViewById != null && (findViewById instanceof OPRadioButton)) {
            ((OPRadioButton) findViewById).setChecked(z);
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public android.widget.LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OPRadioGroup.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OPRadioGroup.class.getName());
    }
}
