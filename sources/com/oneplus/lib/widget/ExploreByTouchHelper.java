package com.oneplus.lib.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeProvider;

public abstract class ExploreByTouchHelper extends AccessibilityDelegate {
    private static final String DEFAULT_CLASS_NAME = View.class.getName();
    private static final Rect INVALID_PARENT_BOUNDS = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private final Context mContext;
    private int mFocusedVirtualViewId = Integer.MIN_VALUE;
    private int mHoveredVirtualViewId = Integer.MIN_VALUE;
    private final AccessibilityManager mManager;
    private ExploreByTouchNodeProvider mNodeProvider;
    private IntArray mTempArray;
    private int[] mTempGlobalRect;
    private Rect mTempParentRect;
    private Rect mTempScreenRect;
    private Rect mTempVisibleRect;
    private final View mView;

    private class ExploreByTouchNodeProvider extends AccessibilityNodeProvider {
        private ExploreByTouchNodeProvider() {
        }

        public AccessibilityNodeInfo createAccessibilityNodeInfo(int i) {
            return ExploreByTouchHelper.this.createNode(i);
        }

        public boolean performAction(int i, int i2, Bundle bundle) {
            return ExploreByTouchHelper.this.performAction(i, i2, bundle);
        }
    }

    /* access modifiers changed from: protected */
    public abstract int getVirtualViewAt(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract void getVisibleVirtualViews(IntArray intArray);

    /* access modifiers changed from: protected */
    public abstract boolean onPerformActionForVirtualView(int i, int i2, Bundle bundle);

    /* access modifiers changed from: protected */
    public void onPopulateEventForHost(AccessibilityEvent accessibilityEvent) {
    }

    /* access modifiers changed from: protected */
    public abstract void onPopulateEventForVirtualView(int i, AccessibilityEvent accessibilityEvent);

    /* access modifiers changed from: protected */
    public void onPopulateNodeForHost(AccessibilityNodeInfo accessibilityNodeInfo) {
    }

    /* access modifiers changed from: protected */
    public abstract void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfo accessibilityNodeInfo);

    public ExploreByTouchHelper(View view) {
        if (view != null) {
            this.mView = view;
            this.mContext = view.getContext();
            this.mManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
            return;
        }
        throw new IllegalArgumentException("View may not be null");
    }

    public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
        if (this.mNodeProvider == null) {
            this.mNodeProvider = new ExploreByTouchNodeProvider();
        }
        return this.mNodeProvider;
    }

    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        boolean z = false;
        if (this.mManager.isEnabled() && this.mManager.isTouchExplorationEnabled()) {
            int action = motionEvent.getAction();
            if (action == 7 || action == 9) {
                int virtualViewAt = getVirtualViewAt(motionEvent.getX(), motionEvent.getY());
                updateHoveredVirtualView(virtualViewAt);
                if (virtualViewAt != Integer.MIN_VALUE) {
                    z = true;
                }
            } else if (action != 10 || this.mFocusedVirtualViewId == Integer.MIN_VALUE) {
                return false;
            } else {
                updateHoveredVirtualView(Integer.MIN_VALUE);
                return true;
            }
        }
        return z;
    }

    public boolean sendEventForVirtualView(int i, int i2) {
        if (i == Integer.MIN_VALUE || !this.mManager.isEnabled()) {
            return false;
        }
        ViewParent parent = this.mView.getParent();
        if (parent == null) {
            return false;
        }
        return parent.requestSendAccessibilityEvent(this.mView, createEvent(i, i2));
    }

    public void invalidateRoot() {
        invalidateVirtualView(-1, 1);
    }

    public void invalidateVirtualView(int i, int i2) {
        if (i != Integer.MIN_VALUE && this.mManager.isEnabled()) {
            ViewParent parent = this.mView.getParent();
            if (parent != null) {
                AccessibilityEvent createEvent = createEvent(i, 2048);
                createEvent.setContentChangeTypes(i2);
                parent.requestSendAccessibilityEvent(this.mView, createEvent);
            }
        }
    }

    private void updateHoveredVirtualView(int i) {
        int i2 = this.mHoveredVirtualViewId;
        if (i2 != i) {
            this.mHoveredVirtualViewId = i;
            sendEventForVirtualView(i, 128);
            sendEventForVirtualView(i2, 256);
        }
    }

    private AccessibilityEvent createEvent(int i, int i2) {
        if (i != -1) {
            return createEventForChild(i, i2);
        }
        return createEventForHost(i2);
    }

    private AccessibilityEvent createEventForHost(int i) {
        AccessibilityEvent obtain = AccessibilityEvent.obtain(i);
        this.mView.onInitializeAccessibilityEvent(obtain);
        onPopulateEventForHost(obtain);
        return obtain;
    }

    private AccessibilityEvent createEventForChild(int i, int i2) {
        AccessibilityEvent obtain = AccessibilityEvent.obtain(i2);
        obtain.setEnabled(true);
        obtain.setClassName(DEFAULT_CLASS_NAME);
        onPopulateEventForVirtualView(i, obtain);
        if (!obtain.getText().isEmpty() || obtain.getContentDescription() != null) {
            obtain.setPackageName(this.mView.getContext().getPackageName());
            obtain.setSource(this.mView, i);
            return obtain;
        }
        throw new RuntimeException("Callbacks must add text or a content description in populateEventForVirtualViewId()");
    }

    /* access modifiers changed from: private */
    public AccessibilityNodeInfo createNode(int i) {
        if (i != -1) {
            return createNodeForChild(i);
        }
        return createNodeForHost();
    }

    private AccessibilityNodeInfo createNodeForHost() {
        AccessibilityNodeInfo obtain = AccessibilityNodeInfo.obtain(this.mView);
        this.mView.onInitializeAccessibilityNodeInfo(obtain);
        int childCount = obtain.getChildCount();
        onPopulateNodeForHost(obtain);
        IntArray intArray = this.mTempArray;
        if (intArray == null) {
            this.mTempArray = new IntArray();
        } else {
            intArray.clear();
        }
        IntArray intArray2 = this.mTempArray;
        getVisibleVirtualViews(intArray2);
        if (childCount <= 0 || intArray2.size() <= 0) {
            int size = intArray2.size();
            for (int i = 0; i < size; i++) {
                obtain.addChild(this.mView, intArray2.get(i));
            }
            return obtain;
        }
        throw new RuntimeException("Views cannot have both real and virtual children");
    }

    private AccessibilityNodeInfo createNodeForChild(int i) {
        ensureTempRects();
        Rect rect = this.mTempParentRect;
        int[] iArr = this.mTempGlobalRect;
        Rect rect2 = this.mTempScreenRect;
        AccessibilityNodeInfo obtain = AccessibilityNodeInfo.obtain();
        obtain.setEnabled(true);
        obtain.setClassName(DEFAULT_CLASS_NAME);
        obtain.setBoundsInParent(INVALID_PARENT_BOUNDS);
        onPopulateNodeForVirtualView(i, obtain);
        if (obtain.getText() == null && obtain.getContentDescription() == null) {
            throw new RuntimeException("Callbacks must add text or a content description in populateNodeForVirtualViewId()");
        }
        obtain.getBoundsInParent(rect);
        if (!rect.equals(INVALID_PARENT_BOUNDS)) {
            int actions = obtain.getActions();
            if ((actions & 64) != 0) {
                throw new RuntimeException("Callbacks must not add ACTION_ACCESSIBILITY_FOCUS in populateNodeForVirtualViewId()");
            } else if ((actions & 128) == 0) {
                obtain.setPackageName(this.mView.getContext().getPackageName());
                obtain.setSource(this.mView, i);
                obtain.setParent(this.mView);
                if (this.mFocusedVirtualViewId == i) {
                    obtain.setAccessibilityFocused(true);
                    obtain.addAction(AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
                } else {
                    obtain.setAccessibilityFocused(false);
                    obtain.addAction(AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
                }
                if (intersectVisibleToUser(rect)) {
                    obtain.setVisibleToUser(true);
                    obtain.setBoundsInParent(rect);
                }
                this.mView.getLocationOnScreen(iArr);
                int i2 = iArr[0];
                int i3 = iArr[1];
                rect2.set(rect);
                rect2.offset(i2, i3);
                obtain.setBoundsInScreen(rect2);
                return obtain;
            } else {
                throw new RuntimeException("Callbacks must not add ACTION_CLEAR_ACCESSIBILITY_FOCUS in populateNodeForVirtualViewId()");
            }
        } else {
            throw new RuntimeException("Callbacks must set parent bounds in populateNodeForVirtualViewId()");
        }
    }

    private void ensureTempRects() {
        this.mTempGlobalRect = new int[2];
        this.mTempParentRect = new Rect();
        this.mTempScreenRect = new Rect();
    }

    /* access modifiers changed from: private */
    public boolean performAction(int i, int i2, Bundle bundle) {
        if (i != -1) {
            return performActionForChild(i, i2, bundle);
        }
        return performActionForHost(i2, bundle);
    }

    private boolean performActionForHost(int i, Bundle bundle) {
        return this.mView.performAccessibilityAction(i, bundle);
    }

    private boolean performActionForChild(int i, int i2, Bundle bundle) {
        if (i2 == 64 || i2 == 128) {
            return manageFocusForChild(i, i2);
        }
        return onPerformActionForVirtualView(i, i2, bundle);
    }

    private boolean manageFocusForChild(int i, int i2) {
        if (i2 == 64) {
            return requestAccessibilityFocus(i);
        }
        if (i2 != 128) {
            return false;
        }
        return clearAccessibilityFocus(i);
    }

    private boolean intersectVisibleToUser(Rect rect) {
        if (rect == null || rect.isEmpty() || this.mView.getWindowVisibility() != 0) {
            return false;
        }
        ViewParent parent = this.mView.getParent();
        while (parent instanceof View) {
            View view = (View) parent;
            if (view.getAlpha() <= 0.0f || view.getVisibility() != 0) {
                return false;
            }
            parent = view.getParent();
        }
        if (parent == null) {
            return false;
        }
        if (this.mTempVisibleRect == null) {
            this.mTempVisibleRect = new Rect();
        }
        Rect rect2 = this.mTempVisibleRect;
        if (!this.mView.getLocalVisibleRect(rect2)) {
            return false;
        }
        return rect.intersect(rect2);
    }

    private boolean isAccessibilityFocused(int i) {
        return this.mFocusedVirtualViewId == i;
    }

    private boolean requestAccessibilityFocus(int i) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        if (!this.mManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled() || isAccessibilityFocused(i)) {
            return false;
        }
        int i2 = this.mFocusedVirtualViewId;
        if (i2 != Integer.MIN_VALUE) {
            sendEventForVirtualView(i2, 65536);
        }
        this.mFocusedVirtualViewId = i;
        this.mView.invalidate();
        sendEventForVirtualView(i, 32768);
        return true;
    }

    private boolean clearAccessibilityFocus(int i) {
        if (!isAccessibilityFocused(i)) {
            return false;
        }
        this.mFocusedVirtualViewId = Integer.MIN_VALUE;
        this.mView.invalidate();
        sendEventForVirtualView(i, 65536);
        return true;
    }
}
