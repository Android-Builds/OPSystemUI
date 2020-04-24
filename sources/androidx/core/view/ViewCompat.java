package androidx.core.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import androidx.core.R$id;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewCompat {
    private static final int[] ACCESSIBILITY_ACTIONS_RESOURCE_IDS = {R$id.accessibility_custom_action_0, R$id.accessibility_custom_action_1, R$id.accessibility_custom_action_2, R$id.accessibility_custom_action_3, R$id.accessibility_custom_action_4, R$id.accessibility_custom_action_5, R$id.accessibility_custom_action_6, R$id.accessibility_custom_action_7, R$id.accessibility_custom_action_8, R$id.accessibility_custom_action_9, R$id.accessibility_custom_action_10, R$id.accessibility_custom_action_11, R$id.accessibility_custom_action_12, R$id.accessibility_custom_action_13, R$id.accessibility_custom_action_14, R$id.accessibility_custom_action_15, R$id.accessibility_custom_action_16, R$id.accessibility_custom_action_17, R$id.accessibility_custom_action_18, R$id.accessibility_custom_action_19, R$id.accessibility_custom_action_20, R$id.accessibility_custom_action_21, R$id.accessibility_custom_action_22, R$id.accessibility_custom_action_23, R$id.accessibility_custom_action_24, R$id.accessibility_custom_action_25, R$id.accessibility_custom_action_26, R$id.accessibility_custom_action_27, R$id.accessibility_custom_action_28, R$id.accessibility_custom_action_29, R$id.accessibility_custom_action_30, R$id.accessibility_custom_action_31};
    private static boolean sAccessibilityDelegateCheckFailed = false;
    private static Field sAccessibilityDelegateField;
    private static AccessibilityPaneVisibilityManager sAccessibilityPaneVisibilityManager = new AccessibilityPaneVisibilityManager();
    private static Field sMinHeightField;
    private static boolean sMinHeightFieldFetched;
    private static Field sMinWidthField;
    private static boolean sMinWidthFieldFetched;
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static ThreadLocal<Rect> sThreadLocalRect;
    private static WeakHashMap<View, String> sTransitionNameMap;
    private static WeakHashMap<View, ViewPropertyAnimatorCompat> sViewPropertyAnimatorMap = null;

    static class AccessibilityPaneVisibilityManager implements OnGlobalLayoutListener, OnAttachStateChangeListener {
        private WeakHashMap<View, Boolean> mPanesToVisible = new WeakHashMap<>();

        public void onViewDetachedFromWindow(View view) {
        }

        AccessibilityPaneVisibilityManager() {
        }

        public void onGlobalLayout() {
            for (Entry entry : this.mPanesToVisible.entrySet()) {
                checkPaneVisibility((View) entry.getKey(), ((Boolean) entry.getValue()).booleanValue());
            }
        }

        public void onViewAttachedToWindow(View view) {
            registerForLayoutCallback(view);
        }

        private void checkPaneVisibility(View view, boolean z) {
            boolean z2 = view.getVisibility() == 0;
            if (z != z2) {
                if (z2) {
                    ViewCompat.notifyViewAccessibilityStateChangedIfNeeded(view, 16);
                }
                this.mPanesToVisible.put(view, Boolean.valueOf(z2));
            }
        }

        private void registerForLayoutCallback(View view) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
    }

    static abstract class AccessibilityViewProperty<T> {
        private final int mContentChangeType;
        private final int mFrameworkMinimumSdk;
        private final int mTagKey;
        private final Class<T> mType;

        /* access modifiers changed from: 0000 */
        public abstract T frameworkGet(View view);

        AccessibilityViewProperty(int i, Class<T> cls, int i2) {
            this(i, cls, 0, i2);
        }

        AccessibilityViewProperty(int i, Class<T> cls, int i2, int i3) {
            this.mTagKey = i;
            this.mType = cls;
            this.mContentChangeType = i2;
            this.mFrameworkMinimumSdk = i3;
        }

        /* access modifiers changed from: 0000 */
        public T get(View view) {
            if (frameworkAvailable()) {
                return frameworkGet(view);
            }
            if (extrasAvailable()) {
                T tag = view.getTag(this.mTagKey);
                if (this.mType.isInstance(tag)) {
                    return tag;
                }
            }
            return null;
        }

        private boolean frameworkAvailable() {
            return VERSION.SDK_INT >= this.mFrameworkMinimumSdk;
        }

        private boolean extrasAvailable() {
            return VERSION.SDK_INT >= 19;
        }
    }

    public interface OnUnhandledKeyEventListenerCompat {
        boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent);
    }

    static class UnhandledKeyEventManager {
        private static final ArrayList<WeakReference<View>> sViewsWithListeners = new ArrayList<>();
        private SparseArray<WeakReference<View>> mCapturedKeys = null;
        private WeakReference<KeyEvent> mLastDispatchedPreViewKeyEvent = null;
        private WeakHashMap<View, Boolean> mViewsContainingListeners = null;

        UnhandledKeyEventManager() {
        }

        private SparseArray<WeakReference<View>> getCapturedKeys() {
            if (this.mCapturedKeys == null) {
                this.mCapturedKeys = new SparseArray<>();
            }
            return this.mCapturedKeys;
        }

        /* renamed from: at */
        static UnhandledKeyEventManager m4at(View view) {
            UnhandledKeyEventManager unhandledKeyEventManager = (UnhandledKeyEventManager) view.getTag(R$id.tag_unhandled_key_event_manager);
            if (unhandledKeyEventManager != null) {
                return unhandledKeyEventManager;
            }
            UnhandledKeyEventManager unhandledKeyEventManager2 = new UnhandledKeyEventManager();
            view.setTag(R$id.tag_unhandled_key_event_manager, unhandledKeyEventManager2);
            return unhandledKeyEventManager2;
        }

        /* access modifiers changed from: 0000 */
        public boolean dispatch(View view, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 0) {
                recalcViewsWithUnhandled();
            }
            View dispatchInOrder = dispatchInOrder(view, keyEvent);
            if (keyEvent.getAction() == 0) {
                int keyCode = keyEvent.getKeyCode();
                if (dispatchInOrder != null && !KeyEvent.isModifierKey(keyCode)) {
                    getCapturedKeys().put(keyCode, new WeakReference(dispatchInOrder));
                }
            }
            return dispatchInOrder != null;
        }

        private View dispatchInOrder(View view, KeyEvent keyEvent) {
            WeakHashMap<View, Boolean> weakHashMap = this.mViewsContainingListeners;
            if (weakHashMap != null && weakHashMap.containsKey(view)) {
                if (view instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                        View dispatchInOrder = dispatchInOrder(viewGroup.getChildAt(childCount), keyEvent);
                        if (dispatchInOrder != null) {
                            return dispatchInOrder;
                        }
                    }
                }
                if (onUnhandledKeyEvent(view, keyEvent)) {
                    return view;
                }
            }
            return null;
        }

        /* access modifiers changed from: 0000 */
        public boolean preDispatch(KeyEvent keyEvent) {
            WeakReference<KeyEvent> weakReference = this.mLastDispatchedPreViewKeyEvent;
            if (weakReference != null && weakReference.get() == keyEvent) {
                return false;
            }
            this.mLastDispatchedPreViewKeyEvent = new WeakReference<>(keyEvent);
            WeakReference weakReference2 = null;
            SparseArray capturedKeys = getCapturedKeys();
            if (keyEvent.getAction() == 1) {
                int indexOfKey = capturedKeys.indexOfKey(keyEvent.getKeyCode());
                if (indexOfKey >= 0) {
                    weakReference2 = (WeakReference) capturedKeys.valueAt(indexOfKey);
                    capturedKeys.removeAt(indexOfKey);
                }
            }
            if (weakReference2 == null) {
                weakReference2 = (WeakReference) capturedKeys.get(keyEvent.getKeyCode());
            }
            if (weakReference2 == null) {
                return false;
            }
            View view = (View) weakReference2.get();
            if (view != null && ViewCompat.isAttachedToWindow(view)) {
                onUnhandledKeyEvent(view, keyEvent);
            }
            return true;
        }

        private boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent) {
            ArrayList arrayList = (ArrayList) view.getTag(R$id.tag_unhandled_key_listeners);
            if (arrayList != null) {
                for (int size = arrayList.size() - 1; size >= 0; size--) {
                    if (((OnUnhandledKeyEventListenerCompat) arrayList.get(size)).onUnhandledKeyEvent(view, keyEvent)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void recalcViewsWithUnhandled() {
            WeakHashMap<View, Boolean> weakHashMap = this.mViewsContainingListeners;
            if (weakHashMap != null) {
                weakHashMap.clear();
            }
            if (!sViewsWithListeners.isEmpty()) {
                synchronized (sViewsWithListeners) {
                    if (this.mViewsContainingListeners == null) {
                        this.mViewsContainingListeners = new WeakHashMap<>();
                    }
                    for (int size = sViewsWithListeners.size() - 1; size >= 0; size--) {
                        View view = (View) ((WeakReference) sViewsWithListeners.get(size)).get();
                        if (view == null) {
                            sViewsWithListeners.remove(size);
                        } else {
                            this.mViewsContainingListeners.put(view, Boolean.TRUE);
                            for (ViewParent parent = view.getParent(); parent instanceof View; parent = parent.getParent()) {
                                this.mViewsContainingListeners.put((View) parent, Boolean.TRUE);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Rect getEmptyTempRect() {
        if (sThreadLocalRect == null) {
            sThreadLocalRect = new ThreadLocal<>();
        }
        Rect rect = (Rect) sThreadLocalRect.get();
        if (rect == null) {
            rect = new Rect();
            sThreadLocalRect.set(rect);
        }
        rect.setEmpty();
        return rect;
    }

    public static void setAccessibilityDelegate(View view, AccessibilityDelegateCompat accessibilityDelegateCompat) {
        AccessibilityDelegate accessibilityDelegate;
        if (accessibilityDelegateCompat == null && (getAccessibilityDelegateInternal(view) instanceof AccessibilityDelegateAdapter)) {
            accessibilityDelegateCompat = new AccessibilityDelegateCompat();
        }
        if (accessibilityDelegateCompat == null) {
            accessibilityDelegate = null;
        } else {
            accessibilityDelegate = accessibilityDelegateCompat.getBridge();
        }
        view.setAccessibilityDelegate(accessibilityDelegate);
    }

    @SuppressLint({"InlinedApi"})
    public static int getImportantForAutofill(View view) {
        if (VERSION.SDK_INT >= 26) {
            return view.getImportantForAutofill();
        }
        return 0;
    }

    public static void setImportantForAutofill(View view, int i) {
        if (VERSION.SDK_INT >= 26) {
            view.setImportantForAutofill(i);
        }
    }

    public static AccessibilityDelegateCompat getAccessibilityDelegate(View view) {
        AccessibilityDelegate accessibilityDelegateInternal = getAccessibilityDelegateInternal(view);
        if (accessibilityDelegateInternal == null) {
            return null;
        }
        if (accessibilityDelegateInternal instanceof AccessibilityDelegateAdapter) {
            return ((AccessibilityDelegateAdapter) accessibilityDelegateInternal).mCompat;
        }
        return new AccessibilityDelegateCompat(accessibilityDelegateInternal);
    }

    static AccessibilityDelegateCompat getOrCreateAccessibilityDelegateCompat(View view) {
        AccessibilityDelegateCompat accessibilityDelegate = getAccessibilityDelegate(view);
        if (accessibilityDelegate == null) {
            accessibilityDelegate = new AccessibilityDelegateCompat();
        }
        setAccessibilityDelegate(view, accessibilityDelegate);
        return accessibilityDelegate;
    }

    private static AccessibilityDelegate getAccessibilityDelegateInternal(View view) {
        if (VERSION.SDK_INT >= 29) {
            return view.getAccessibilityDelegate();
        }
        return getAccessibilityDelegateThroughReflection(view);
    }

    private static AccessibilityDelegate getAccessibilityDelegateThroughReflection(View view) {
        if (sAccessibilityDelegateCheckFailed) {
            return null;
        }
        if (sAccessibilityDelegateField == null) {
            try {
                sAccessibilityDelegateField = View.class.getDeclaredField("mAccessibilityDelegate");
                sAccessibilityDelegateField.setAccessible(true);
            } catch (Throwable unused) {
                sAccessibilityDelegateCheckFailed = true;
                return null;
            }
        }
        try {
            Object obj = sAccessibilityDelegateField.get(view);
            if (obj instanceof AccessibilityDelegate) {
                return (AccessibilityDelegate) obj;
            }
            return null;
        } catch (Throwable unused2) {
            sAccessibilityDelegateCheckFailed = true;
            return null;
        }
    }

    public static boolean hasTransientState(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.hasTransientState();
        }
        return false;
    }

    public static void postInvalidateOnAnimation(View view) {
        if (VERSION.SDK_INT >= 16) {
            view.postInvalidateOnAnimation();
        } else {
            view.postInvalidate();
        }
    }

    public static void postInvalidateOnAnimation(View view, int i, int i2, int i3, int i4) {
        if (VERSION.SDK_INT >= 16) {
            view.postInvalidateOnAnimation(i, i2, i3, i4);
        } else {
            view.postInvalidate(i, i2, i3, i4);
        }
    }

    public static void postOnAnimation(View view, Runnable runnable) {
        if (VERSION.SDK_INT >= 16) {
            view.postOnAnimation(runnable);
        } else {
            view.postDelayed(runnable, ValueAnimator.getFrameDelay());
        }
    }

    public static void postOnAnimationDelayed(View view, Runnable runnable, long j) {
        if (VERSION.SDK_INT >= 16) {
            view.postOnAnimationDelayed(runnable, j);
        } else {
            view.postDelayed(runnable, ValueAnimator.getFrameDelay() + j);
        }
    }

    public static int getImportantForAccessibility(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getImportantForAccessibility();
        }
        return 0;
    }

    public static void setImportantForAccessibility(View view, int i) {
        int i2 = VERSION.SDK_INT;
        if (i2 >= 19) {
            view.setImportantForAccessibility(i);
        } else if (i2 >= 16) {
            if (i == 4) {
                i = 2;
            }
            view.setImportantForAccessibility(i);
        }
    }

    public static void replaceAccessibilityAction(View view, AccessibilityActionCompat accessibilityActionCompat, CharSequence charSequence, AccessibilityViewCommand accessibilityViewCommand) {
        addAccessibilityAction(view, accessibilityActionCompat.createReplacementAction(charSequence, accessibilityViewCommand));
    }

    private static void addAccessibilityAction(View view, AccessibilityActionCompat accessibilityActionCompat) {
        if (VERSION.SDK_INT >= 21) {
            getOrCreateAccessibilityDelegateCompat(view);
            removeActionWithId(accessibilityActionCompat.getId(), view);
            getActionList(view).add(accessibilityActionCompat);
            notifyViewAccessibilityStateChangedIfNeeded(view, 0);
        }
    }

    private static void removeActionWithId(int i, View view) {
        List actionList = getActionList(view);
        for (int i2 = 0; i2 < actionList.size(); i2++) {
            if (((AccessibilityActionCompat) actionList.get(i2)).getId() == i) {
                actionList.remove(i2);
                return;
            }
        }
    }

    private static List<AccessibilityActionCompat> getActionList(View view) {
        ArrayList arrayList = (ArrayList) view.getTag(R$id.tag_accessibility_actions);
        if (arrayList != null) {
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList();
        view.setTag(R$id.tag_accessibility_actions, arrayList2);
        return arrayList2;
    }

    public static void setLayerPaint(View view, Paint paint) {
        if (VERSION.SDK_INT >= 17) {
            view.setLayerPaint(paint);
            return;
        }
        view.setLayerType(view.getLayerType(), paint);
        view.invalidate();
    }

    public static int getLayoutDirection(View view) {
        if (VERSION.SDK_INT >= 17) {
            return view.getLayoutDirection();
        }
        return 0;
    }

    public static ViewParent getParentForAccessibility(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getParentForAccessibility();
        }
        return view.getParent();
    }

    public static int getAccessibilityLiveRegion(View view) {
        if (VERSION.SDK_INT >= 19) {
            return view.getAccessibilityLiveRegion();
        }
        return 0;
    }

    public static int getMinimumWidth(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getMinimumWidth();
        }
        if (!sMinWidthFieldFetched) {
            try {
                sMinWidthField = View.class.getDeclaredField("mMinWidth");
                sMinWidthField.setAccessible(true);
            } catch (NoSuchFieldException unused) {
            }
            sMinWidthFieldFetched = true;
        }
        Field field = sMinWidthField;
        if (field != null) {
            try {
                return ((Integer) field.get(view)).intValue();
            } catch (Exception unused2) {
            }
        }
        return 0;
    }

    public static int getMinimumHeight(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getMinimumHeight();
        }
        if (!sMinHeightFieldFetched) {
            try {
                sMinHeightField = View.class.getDeclaredField("mMinHeight");
                sMinHeightField.setAccessible(true);
            } catch (NoSuchFieldException unused) {
            }
            sMinHeightFieldFetched = true;
        }
        Field field = sMinHeightField;
        if (field != null) {
            try {
                return ((Integer) field.get(view)).intValue();
            } catch (Exception unused2) {
            }
        }
        return 0;
    }

    public static ViewPropertyAnimatorCompat animate(View view) {
        if (sViewPropertyAnimatorMap == null) {
            sViewPropertyAnimatorMap = new WeakHashMap<>();
        }
        ViewPropertyAnimatorCompat viewPropertyAnimatorCompat = (ViewPropertyAnimatorCompat) sViewPropertyAnimatorMap.get(view);
        if (viewPropertyAnimatorCompat != null) {
            return viewPropertyAnimatorCompat;
        }
        ViewPropertyAnimatorCompat viewPropertyAnimatorCompat2 = new ViewPropertyAnimatorCompat(view);
        sViewPropertyAnimatorMap.put(view, viewPropertyAnimatorCompat2);
        return viewPropertyAnimatorCompat2;
    }

    public static void setElevation(View view, float f) {
        if (VERSION.SDK_INT >= 21) {
            view.setElevation(f);
        }
    }

    public static float getElevation(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getElevation();
        }
        return 0.0f;
    }

    public static void setTranslationZ(View view, float f) {
        if (VERSION.SDK_INT >= 21) {
            view.setTranslationZ(f);
        }
    }

    public static float getTranslationZ(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getTranslationZ();
        }
        return 0.0f;
    }

    public static void setTransitionName(View view, String str) {
        if (VERSION.SDK_INT >= 21) {
            view.setTransitionName(str);
            return;
        }
        if (sTransitionNameMap == null) {
            sTransitionNameMap = new WeakHashMap<>();
        }
        sTransitionNameMap.put(view, str);
    }

    public static String getTransitionName(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getTransitionName();
        }
        WeakHashMap<View, String> weakHashMap = sTransitionNameMap;
        if (weakHashMap == null) {
            return null;
        }
        return (String) weakHashMap.get(view);
    }

    public static int getWindowSystemUiVisibility(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getWindowSystemUiVisibility();
        }
        return 0;
    }

    public static void requestApplyInsets(View view) {
        int i = VERSION.SDK_INT;
        if (i >= 20) {
            view.requestApplyInsets();
        } else if (i >= 16) {
            view.requestFitSystemWindows();
        }
    }

    public static boolean getFitsSystemWindows(View view) {
        if (VERSION.SDK_INT >= 16) {
            return view.getFitsSystemWindows();
        }
        return false;
    }

    public static void setOnApplyWindowInsetsListener(View view, final OnApplyWindowInsetsListener onApplyWindowInsetsListener) {
        if (VERSION.SDK_INT >= 21) {
            if (onApplyWindowInsetsListener == null) {
                view.setOnApplyWindowInsetsListener(null);
                return;
            }
            view.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    return WindowInsetsCompat.unwrap(onApplyWindowInsetsListener.onApplyWindowInsets(view, WindowInsetsCompat.wrap(windowInsets)));
                }
            });
        }
    }

    public static WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
        if (VERSION.SDK_INT < 21) {
            return windowInsetsCompat;
        }
        WindowInsets unwrap = WindowInsetsCompat.unwrap(windowInsetsCompat);
        WindowInsets onApplyWindowInsets = view.onApplyWindowInsets(unwrap);
        if (!onApplyWindowInsets.equals(unwrap)) {
            unwrap = new WindowInsets(onApplyWindowInsets);
        }
        return WindowInsetsCompat.wrap(unwrap);
    }

    public static WindowInsetsCompat dispatchApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
        if (VERSION.SDK_INT < 21) {
            return windowInsetsCompat;
        }
        WindowInsets unwrap = WindowInsetsCompat.unwrap(windowInsetsCompat);
        WindowInsets dispatchApplyWindowInsets = view.dispatchApplyWindowInsets(unwrap);
        if (!dispatchApplyWindowInsets.equals(unwrap)) {
            unwrap = new WindowInsets(dispatchApplyWindowInsets);
        }
        return WindowInsetsCompat.wrap(unwrap);
    }

    public static void setBackground(View view, Drawable drawable) {
        if (VERSION.SDK_INT >= 16) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static ColorStateList getBackgroundTintList(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getBackgroundTintList();
        }
        return view instanceof TintableBackgroundView ? ((TintableBackgroundView) view).getSupportBackgroundTintList() : null;
    }

    public static void setBackgroundTintList(View view, ColorStateList colorStateList) {
        if (VERSION.SDK_INT >= 21) {
            view.setBackgroundTintList(colorStateList);
            if (VERSION.SDK_INT == 21) {
                Drawable background = view.getBackground();
                boolean z = (view.getBackgroundTintList() == null && view.getBackgroundTintMode() == null) ? false : true;
                if (background != null && z) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        } else if (view instanceof TintableBackgroundView) {
            ((TintableBackgroundView) view).setSupportBackgroundTintList(colorStateList);
        }
    }

    public static Mode getBackgroundTintMode(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getBackgroundTintMode();
        }
        return view instanceof TintableBackgroundView ? ((TintableBackgroundView) view).getSupportBackgroundTintMode() : null;
    }

    public static void setBackgroundTintMode(View view, Mode mode) {
        if (VERSION.SDK_INT >= 21) {
            view.setBackgroundTintMode(mode);
            if (VERSION.SDK_INT == 21) {
                Drawable background = view.getBackground();
                boolean z = (view.getBackgroundTintList() == null && view.getBackgroundTintMode() == null) ? false : true;
                if (background != null && z) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        } else if (view instanceof TintableBackgroundView) {
            ((TintableBackgroundView) view).setSupportBackgroundTintMode(mode);
        }
    }

    public static boolean isNestedScrollingEnabled(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.isNestedScrollingEnabled();
        }
        if (view instanceof NestedScrollingChild) {
            return ((NestedScrollingChild) view).isNestedScrollingEnabled();
        }
        return false;
    }

    public static void stopNestedScroll(View view) {
        if (VERSION.SDK_INT >= 21) {
            view.stopNestedScroll();
        } else if (view instanceof NestedScrollingChild) {
            ((NestedScrollingChild) view).stopNestedScroll();
        }
    }

    public static boolean isLaidOut(View view) {
        if (VERSION.SDK_INT >= 19) {
            return view.isLaidOut();
        }
        return view.getWidth() > 0 && view.getHeight() > 0;
    }

    public static float getZ(View view) {
        if (VERSION.SDK_INT >= 21) {
            return view.getZ();
        }
        return 0.0f;
    }

    public static void setZ(View view, float f) {
        if (VERSION.SDK_INT >= 21) {
            view.setZ(f);
        }
    }

    public static void offsetTopAndBottom(View view, int i) {
        int i2 = VERSION.SDK_INT;
        if (i2 >= 23) {
            view.offsetTopAndBottom(i);
        } else if (i2 >= 21) {
            Rect emptyTempRect = getEmptyTempRect();
            boolean z = false;
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                View view2 = (View) parent;
                emptyTempRect.set(view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom());
                z = !emptyTempRect.intersects(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }
            compatOffsetTopAndBottom(view, i);
            if (z && emptyTempRect.intersect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(emptyTempRect);
            }
        } else {
            compatOffsetTopAndBottom(view, i);
        }
    }

    private static void compatOffsetTopAndBottom(View view, int i) {
        view.offsetTopAndBottom(i);
        if (view.getVisibility() == 0) {
            tickleInvalidationFlag(view);
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    public static void offsetLeftAndRight(View view, int i) {
        int i2 = VERSION.SDK_INT;
        if (i2 >= 23) {
            view.offsetLeftAndRight(i);
        } else if (i2 >= 21) {
            Rect emptyTempRect = getEmptyTempRect();
            boolean z = false;
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                View view2 = (View) parent;
                emptyTempRect.set(view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom());
                z = !emptyTempRect.intersects(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }
            compatOffsetLeftAndRight(view, i);
            if (z && emptyTempRect.intersect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(emptyTempRect);
            }
        } else {
            compatOffsetLeftAndRight(view, i);
        }
    }

    private static void compatOffsetLeftAndRight(View view, int i) {
        view.offsetLeftAndRight(i);
        if (view.getVisibility() == 0) {
            tickleInvalidationFlag(view);
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    private static void tickleInvalidationFlag(View view) {
        float translationY = view.getTranslationY();
        view.setTranslationY(1.0f + translationY);
        view.setTranslationY(translationY);
    }

    public static boolean isAttachedToWindow(View view) {
        if (VERSION.SDK_INT >= 19) {
            return view.isAttachedToWindow();
        }
        return view.getWindowToken() != null;
    }

    public static boolean hasOnClickListeners(View view) {
        if (VERSION.SDK_INT >= 15) {
            return view.hasOnClickListeners();
        }
        return false;
    }

    public static void setScrollIndicators(View view, int i, int i2) {
        if (VERSION.SDK_INT >= 23) {
            view.setScrollIndicators(i, i2);
        }
    }

    public static Display getDisplay(View view) {
        if (VERSION.SDK_INT >= 17) {
            return view.getDisplay();
        }
        if (isAttachedToWindow(view)) {
            return ((WindowManager) view.getContext().getSystemService("window")).getDefaultDisplay();
        }
        return null;
    }

    static boolean dispatchUnhandledKeyEventBeforeHierarchy(View view, KeyEvent keyEvent) {
        if (VERSION.SDK_INT >= 28) {
            return false;
        }
        return UnhandledKeyEventManager.m4at(view).preDispatch(keyEvent);
    }

    static boolean dispatchUnhandledKeyEventBeforeCallback(View view, KeyEvent keyEvent) {
        if (VERSION.SDK_INT >= 28) {
            return false;
        }
        return UnhandledKeyEventManager.m4at(view).dispatch(view, keyEvent);
    }

    public static boolean isScreenReaderFocusable(View view) {
        Boolean bool = (Boolean) screenReaderFocusableProperty().get(view);
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    private static AccessibilityViewProperty<Boolean> screenReaderFocusableProperty() {
        return new AccessibilityViewProperty<Boolean>(R$id.tag_screen_reader_focusable, Boolean.class, 28) {
            /* access modifiers changed from: 0000 */
            public Boolean frameworkGet(View view) {
                return Boolean.valueOf(view.isScreenReaderFocusable());
            }
        };
    }

    public static CharSequence getAccessibilityPaneTitle(View view) {
        return (CharSequence) paneTitleProperty().get(view);
    }

    private static AccessibilityViewProperty<CharSequence> paneTitleProperty() {
        return new AccessibilityViewProperty<CharSequence>(R$id.tag_accessibility_pane_title, CharSequence.class, 8, 28) {
            /* access modifiers changed from: 0000 */
            public CharSequence frameworkGet(View view) {
                return view.getAccessibilityPaneTitle();
            }
        };
    }

    public static boolean isAccessibilityHeading(View view) {
        Boolean bool = (Boolean) accessibilityHeadingProperty().get(view);
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    private static AccessibilityViewProperty<Boolean> accessibilityHeadingProperty() {
        return new AccessibilityViewProperty<Boolean>(R$id.tag_accessibility_heading, Boolean.class, 28) {
            /* access modifiers changed from: 0000 */
            public Boolean frameworkGet(View view) {
                return Boolean.valueOf(view.isAccessibilityHeading());
            }
        };
    }

    static void notifyViewAccessibilityStateChangedIfNeeded(View view, int i) {
        if (((AccessibilityManager) view.getContext().getSystemService("accessibility")).isEnabled()) {
            boolean z = getAccessibilityPaneTitle(view) != null;
            if (getAccessibilityLiveRegion(view) != 0 || (z && view.getVisibility() == 0)) {
                AccessibilityEvent obtain = AccessibilityEvent.obtain();
                obtain.setEventType(z ? 32 : 2048);
                obtain.setContentChangeTypes(i);
                view.sendAccessibilityEventUnchecked(obtain);
            } else if (view.getParent() != null) {
                try {
                    view.getParent().notifySubtreeAccessibilityStateChanged(view, view, i);
                } catch (AbstractMethodError e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(view.getParent().getClass().getSimpleName());
                    sb.append(" does not fully implement ViewParent");
                    Log.e("ViewCompat", sb.toString(), e);
                }
            }
        }
    }
}
