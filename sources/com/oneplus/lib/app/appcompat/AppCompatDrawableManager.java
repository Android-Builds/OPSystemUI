package com.oneplus.lib.app.appcompat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.Theme;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.LayerDrawable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.support.collection.ArrayMap;
import com.oneplus.support.collection.LongSparseArray;
import com.oneplus.support.collection.LruCache;
import com.oneplus.support.core.content.ContextCompat;
import com.oneplus.support.core.graphics.ColorUtils;
import com.oneplus.support.core.graphics.drawable.DrawableCompat;
import com.oneplus.support.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import com.oneplus.support.vectordrawable.graphics.drawable.VectorDrawableCompat;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import org.xmlpull.v1.XmlPullParser;

public class AppCompatDrawableManager {
    private static final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = {R$drawable.abc_popup_background_mtrl_mult, R$drawable.abc_cab_background_internal_bg, R$drawable.abc_menu_hardkey_panel_mtrl_mult};
    private static final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = {R$drawable.abc_textfield_activated_mtrl_alpha, R$drawable.abc_textfield_search_activated_mtrl_alpha, R$drawable.abc_cab_background_top_mtrl_alpha, R$drawable.abc_text_cursor_material, R$drawable.abc_text_select_handle_left_mtrl_dark, R$drawable.abc_text_select_handle_middle_mtrl_dark, R$drawable.abc_text_select_handle_right_mtrl_dark, R$drawable.abc_text_select_handle_left_mtrl_light, R$drawable.abc_text_select_handle_middle_mtrl_light, R$drawable.abc_text_select_handle_right_mtrl_light};
    private static final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = {R$drawable.abc_textfield_search_default_mtrl_alpha, R$drawable.abc_textfield_default_mtrl_alpha, R$drawable.abc_ab_share_pack_mtrl_alpha};
    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);
    private static final Mode DEFAULT_MODE = Mode.SRC_IN;
    private static AppCompatDrawableManager INSTANCE;
    private static final int[] TINT_CHECKABLE_BUTTON_LIST = {R$drawable.abc_btn_check_material, R$drawable.abc_btn_radio_material};
    private static final int[] TINT_COLOR_CONTROL_NORMAL = {R$drawable.abc_ic_commit_search_api_mtrl_alpha, R$drawable.abc_seekbar_tick_mark_material, R$drawable.abc_ic_menu_share_mtrl_alpha, R$drawable.abc_ic_menu_copy_mtrl_am_alpha, R$drawable.abc_ic_menu_cut_mtrl_alpha, R$drawable.abc_ic_menu_selectall_mtrl_alpha, R$drawable.abc_ic_menu_paste_mtrl_am_alpha};
    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {R$drawable.abc_tab_indicator_material, R$drawable.abc_textfield_search_material};
    private ArrayMap<String, InflateDelegate> mDelegates;
    private final Object mDrawableCacheLock = new Object();
    private final WeakHashMap<Context, LongSparseArray<WeakReference<ConstantState>>> mDrawableCaches = new WeakHashMap<>(0);
    private boolean mHasCheckedVectorDrawableSetup;
    private SparseArray<String> mKnownDrawableIdTags;
    private WeakHashMap<Context, SparseArray<ColorStateList>> mTintLists;
    private TypedValue mTypedValue;

    private static class AvdcInflateDelegate implements InflateDelegate {
        AvdcInflateDelegate() {
        }

        public Drawable createFromXmlInner(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet, Theme theme) {
            try {
                return AnimatedVectorDrawableCompat.createFromXmlInner(context, context.getResources(), xmlPullParser, attributeSet, theme);
            } catch (Exception e) {
                Log.e("AvdcInflateDelegate", "Exception while inflating <animated-vector>", e);
                return null;
            }
        }
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {
        public ColorFilterLruCache(int i) {
            super(i);
        }

        /* access modifiers changed from: 0000 */
        public PorterDuffColorFilter get(int i, Mode mode) {
            return (PorterDuffColorFilter) get(Integer.valueOf(generateCacheKey(i, mode)));
        }

        /* access modifiers changed from: 0000 */
        public PorterDuffColorFilter put(int i, Mode mode, PorterDuffColorFilter porterDuffColorFilter) {
            return (PorterDuffColorFilter) put(Integer.valueOf(generateCacheKey(i, mode)), porterDuffColorFilter);
        }

        private static int generateCacheKey(int i, Mode mode) {
            return ((i + 31) * 31) + mode.hashCode();
        }
    }

    private interface InflateDelegate {
        Drawable createFromXmlInner(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet, Theme theme);
    }

    private static class VdcInflateDelegate implements InflateDelegate {
        VdcInflateDelegate() {
        }

        public Drawable createFromXmlInner(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet, Theme theme) {
            try {
                return VectorDrawableCompat.createFromXmlInner(context.getResources(), xmlPullParser, attributeSet, theme);
            } catch (Exception e) {
                Log.e("VdcInflateDelegate", "Exception while inflating <vector>", e);
                return null;
            }
        }
    }

    public static AppCompatDrawableManager get() {
        if (INSTANCE == null) {
            INSTANCE = new AppCompatDrawableManager();
            installDefaultInflateDelegates(INSTANCE);
        }
        return INSTANCE;
    }

    private static void installDefaultInflateDelegates(AppCompatDrawableManager appCompatDrawableManager) {
        int i = VERSION.SDK_INT;
        if (i < 24) {
            appCompatDrawableManager.addDelegate("vector", new VdcInflateDelegate());
            if (i >= 11) {
                appCompatDrawableManager.addDelegate("animated-vector", new AvdcInflateDelegate());
            }
        }
    }

    public Drawable getDrawable(Context context, int i) {
        return getDrawable(context, i, false);
    }

    /* access modifiers changed from: 0000 */
    public Drawable getDrawable(Context context, int i, boolean z) {
        checkVectorDrawableSetup(context);
        Drawable loadDrawableFromDelegates = loadDrawableFromDelegates(context, i);
        if (loadDrawableFromDelegates == null) {
            loadDrawableFromDelegates = createDrawableIfNeeded(context, i);
        }
        if (loadDrawableFromDelegates == null) {
            loadDrawableFromDelegates = ContextCompat.getDrawable(context, i);
        }
        if (loadDrawableFromDelegates != null) {
            loadDrawableFromDelegates = tintDrawable(context, i, z, loadDrawableFromDelegates);
        }
        if (loadDrawableFromDelegates != null) {
            DrawableUtils.fixDrawable(loadDrawableFromDelegates);
        }
        return loadDrawableFromDelegates;
    }

    private static long createCacheKey(TypedValue typedValue) {
        return (((long) typedValue.assetCookie) << 32) | ((long) typedValue.data);
    }

    private Drawable createDrawableIfNeeded(Context context, int i) {
        if (this.mTypedValue == null) {
            this.mTypedValue = new TypedValue();
        }
        TypedValue typedValue = this.mTypedValue;
        context.getResources().getValue(i, typedValue, true);
        long createCacheKey = createCacheKey(typedValue);
        Drawable cachedDrawable = getCachedDrawable(context, createCacheKey);
        if (cachedDrawable != null) {
            return cachedDrawable;
        }
        if (i == R$drawable.abc_cab_background_top_material) {
            cachedDrawable = new LayerDrawable(new Drawable[]{getDrawable(context, R$drawable.abc_cab_background_internal_bg), getDrawable(context, R$drawable.abc_cab_background_top_mtrl_alpha)});
        }
        if (cachedDrawable != null) {
            cachedDrawable.setChangingConfigurations(typedValue.changingConfigurations);
            addDrawableToCache(context, createCacheKey, cachedDrawable);
        }
        return cachedDrawable;
    }

    private Drawable tintDrawable(Context context, int i, boolean z, Drawable drawable) {
        ColorStateList tintList = getTintList(context, i);
        if (tintList != null) {
            if (DrawableUtils.canSafelyMutateDrawable(drawable)) {
                drawable = drawable.mutate();
            }
            Drawable wrap = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(wrap, tintList);
            Mode tintMode = getTintMode(i);
            if (tintMode == null) {
                return wrap;
            }
            DrawableCompat.setTintMode(wrap, tintMode);
            return wrap;
        } else if (i == R$drawable.abc_seekbar_track_material) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            setPorterDuffColorFilter(layerDrawable.findDrawableByLayerId(16908288), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlNormal), DEFAULT_MODE);
            setPorterDuffColorFilter(layerDrawable.findDrawableByLayerId(16908303), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlNormal), DEFAULT_MODE);
            setPorterDuffColorFilter(layerDrawable.findDrawableByLayerId(16908301), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated), DEFAULT_MODE);
            return drawable;
        } else if (i == R$drawable.abc_ratingbar_material || i == R$drawable.abc_ratingbar_indicator_material || i == R$drawable.abc_ratingbar_small_material) {
            LayerDrawable layerDrawable2 = (LayerDrawable) drawable;
            setPorterDuffColorFilter(layerDrawable2.findDrawableByLayerId(16908288), ThemeUtils.getDisabledThemeAttrColor(context, R$attr.colorControlNormal), DEFAULT_MODE);
            setPorterDuffColorFilter(layerDrawable2.findDrawableByLayerId(16908303), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated), DEFAULT_MODE);
            setPorterDuffColorFilter(layerDrawable2.findDrawableByLayerId(16908301), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated), DEFAULT_MODE);
            return drawable;
        } else if (tintDrawableUsingColorFilter(context, i, drawable) || !z) {
            return drawable;
        } else {
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0073 A[Catch:{ Exception -> 0x009f }] */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0097 A[Catch:{ Exception -> 0x009f }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.graphics.drawable.Drawable loadDrawableFromDelegates(android.content.Context r9, int r10) {
        /*
            r8 = this;
            com.oneplus.support.collection.ArrayMap<java.lang.String, com.oneplus.lib.app.appcompat.AppCompatDrawableManager$InflateDelegate> r0 = r8.mDelegates
            r1 = 0
            if (r0 == 0) goto L_0x00a7
            boolean r0 = r0.isEmpty()
            if (r0 != 0) goto L_0x00a7
            android.util.SparseArray<java.lang.String> r0 = r8.mKnownDrawableIdTags
            java.lang.String r2 = "appcompat_skip_skip"
            if (r0 == 0) goto L_0x0028
            java.lang.Object r0 = r0.get(r10)
            java.lang.String r0 = (java.lang.String) r0
            boolean r3 = r2.equals(r0)
            if (r3 != 0) goto L_0x0027
            if (r0 == 0) goto L_0x002f
            com.oneplus.support.collection.ArrayMap<java.lang.String, com.oneplus.lib.app.appcompat.AppCompatDrawableManager$InflateDelegate> r3 = r8.mDelegates
            java.lang.Object r0 = r3.get(r0)
            if (r0 != 0) goto L_0x002f
        L_0x0027:
            return r1
        L_0x0028:
            android.util.SparseArray r0 = new android.util.SparseArray
            r0.<init>()
            r8.mKnownDrawableIdTags = r0
        L_0x002f:
            android.util.TypedValue r0 = r8.mTypedValue
            if (r0 != 0) goto L_0x003a
            android.util.TypedValue r0 = new android.util.TypedValue
            r0.<init>()
            r8.mTypedValue = r0
        L_0x003a:
            android.util.TypedValue r0 = r8.mTypedValue
            android.content.res.Resources r1 = r9.getResources()
            r3 = 1
            r1.getValue(r10, r0, r3)
            long r4 = createCacheKey(r0)
            android.graphics.drawable.Drawable r4 = r8.getCachedDrawable(r9, r4)
            if (r4 == 0) goto L_0x004f
            return r4
        L_0x004f:
            java.lang.CharSequence r5 = r0.string
            if (r5 == 0) goto L_0x009f
            java.lang.String r5 = r5.toString()
            java.lang.String r6 = ".xml"
            boolean r5 = r5.endsWith(r6)
            if (r5 == 0) goto L_0x009f
            android.content.res.XmlResourceParser r1 = r1.getXml(r10)     // Catch:{ Exception -> 0x009f }
            android.util.AttributeSet r5 = android.util.Xml.asAttributeSet(r1)     // Catch:{ Exception -> 0x009f }
        L_0x0067:
            int r6 = r1.next()     // Catch:{ Exception -> 0x009f }
            r7 = 2
            if (r6 == r7) goto L_0x0071
            if (r6 == r3) goto L_0x0071
            goto L_0x0067
        L_0x0071:
            if (r6 != r7) goto L_0x0097
            java.lang.String r3 = r1.getName()     // Catch:{ Exception -> 0x009f }
            android.util.SparseArray<java.lang.String> r6 = r8.mKnownDrawableIdTags     // Catch:{ Exception -> 0x009f }
            r6.append(r10, r3)     // Catch:{ Exception -> 0x009f }
            com.oneplus.support.collection.ArrayMap<java.lang.String, com.oneplus.lib.app.appcompat.AppCompatDrawableManager$InflateDelegate> r6 = r8.mDelegates     // Catch:{ Exception -> 0x009f }
            java.lang.Object r3 = r6.get(r3)     // Catch:{ Exception -> 0x009f }
            com.oneplus.lib.app.appcompat.AppCompatDrawableManager$InflateDelegate r3 = (com.oneplus.lib.app.appcompat.AppCompatDrawableManager.InflateDelegate) r3     // Catch:{ Exception -> 0x009f }
            if (r3 == 0) goto L_0x008f
            android.content.res.Resources$Theme r6 = r9.getTheme()     // Catch:{ Exception -> 0x009f }
            android.graphics.drawable.Drawable r9 = r3.createFromXmlInner(r9, r1, r5, r6)     // Catch:{ Exception -> 0x009f }
            r4 = r9
        L_0x008f:
            if (r4 == 0) goto L_0x009f
            int r9 = r0.changingConfigurations     // Catch:{ Exception -> 0x009f }
            r4.setChangingConfigurations(r9)     // Catch:{ Exception -> 0x009f }
            goto L_0x009f
        L_0x0097:
            org.xmlpull.v1.XmlPullParserException r9 = new org.xmlpull.v1.XmlPullParserException     // Catch:{ Exception -> 0x009f }
            java.lang.String r0 = "No start tag found"
            r9.<init>(r0)     // Catch:{ Exception -> 0x009f }
            throw r9     // Catch:{ Exception -> 0x009f }
        L_0x009f:
            if (r4 != 0) goto L_0x00a6
            android.util.SparseArray<java.lang.String> r8 = r8.mKnownDrawableIdTags
            r8.append(r10, r2)
        L_0x00a6:
            return r4
        L_0x00a7:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.app.appcompat.AppCompatDrawableManager.loadDrawableFromDelegates(android.content.Context, int):android.graphics.drawable.Drawable");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x002e, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.graphics.drawable.Drawable getCachedDrawable(android.content.Context r4, long r5) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mDrawableCacheLock
            monitor-enter(r0)
            java.util.WeakHashMap<android.content.Context, com.oneplus.support.collection.LongSparseArray<java.lang.ref.WeakReference<android.graphics.drawable.Drawable$ConstantState>>> r3 = r3.mDrawableCaches     // Catch:{ all -> 0x002f }
            java.lang.Object r3 = r3.get(r4)     // Catch:{ all -> 0x002f }
            com.oneplus.support.collection.LongSparseArray r3 = (com.oneplus.support.collection.LongSparseArray) r3     // Catch:{ all -> 0x002f }
            r1 = 0
            if (r3 != 0) goto L_0x0010
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            return r1
        L_0x0010:
            java.lang.Object r2 = r3.get(r5)     // Catch:{ all -> 0x002f }
            java.lang.ref.WeakReference r2 = (java.lang.ref.WeakReference) r2     // Catch:{ all -> 0x002f }
            if (r2 == 0) goto L_0x002d
            java.lang.Object r2 = r2.get()     // Catch:{ all -> 0x002f }
            android.graphics.drawable.Drawable$ConstantState r2 = (android.graphics.drawable.Drawable.ConstantState) r2     // Catch:{ all -> 0x002f }
            if (r2 == 0) goto L_0x002a
            android.content.res.Resources r3 = r4.getResources()     // Catch:{ all -> 0x002f }
            android.graphics.drawable.Drawable r3 = r2.newDrawable(r3)     // Catch:{ all -> 0x002f }
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            return r3
        L_0x002a:
            r3.delete(r5)     // Catch:{ all -> 0x002f }
        L_0x002d:
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            return r1
        L_0x002f:
            r3 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.app.appcompat.AppCompatDrawableManager.getCachedDrawable(android.content.Context, long):android.graphics.drawable.Drawable");
    }

    private boolean addDrawableToCache(Context context, long j, Drawable drawable) {
        ConstantState constantState = drawable.getConstantState();
        if (constantState == null) {
            return false;
        }
        synchronized (this.mDrawableCacheLock) {
            LongSparseArray longSparseArray = (LongSparseArray) this.mDrawableCaches.get(context);
            if (longSparseArray == null) {
                longSparseArray = new LongSparseArray();
                this.mDrawableCaches.put(context, longSparseArray);
            }
            longSparseArray.put(j, new WeakReference(constantState));
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0060 A[RETURN] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static boolean tintDrawableUsingColorFilter(android.content.Context r5, int r6, android.graphics.drawable.Drawable r7) {
        /*
            android.graphics.PorterDuff$Mode r0 = DEFAULT_MODE
            int[] r1 = COLORFILTER_TINT_COLOR_CONTROL_NORMAL
            boolean r1 = arrayContains(r1, r6)
            r2 = 16842801(0x1010031, float:2.3693695E-38)
            r3 = 0
            r4 = 1
            if (r1 == 0) goto L_0x0014
            int r2 = com.oneplus.commonctrl.R$attr.colorControlNormal
        L_0x0011:
            r1 = r3
        L_0x0012:
            r6 = r4
            goto L_0x0042
        L_0x0014:
            int[] r1 = COLORFILTER_COLOR_CONTROL_ACTIVATED
            boolean r1 = arrayContains(r1, r6)
            if (r1 == 0) goto L_0x001f
            int r2 = com.oneplus.commonctrl.R$attr.colorControlActivated
            goto L_0x0011
        L_0x001f:
            int[] r1 = COLORFILTER_COLOR_BACKGROUND_MULTIPLY
            boolean r1 = arrayContains(r1, r6)
            if (r1 == 0) goto L_0x002a
            android.graphics.PorterDuff$Mode r0 = android.graphics.PorterDuff.Mode.MULTIPLY
            goto L_0x0011
        L_0x002a:
            int r1 = com.oneplus.commonctrl.R$drawable.abc_list_divider_mtrl_alpha
            if (r6 != r1) goto L_0x003a
            r2 = 16842800(0x1010030, float:2.3693693E-38)
            r6 = 1109603123(0x42233333, float:40.8)
            int r6 = java.lang.Math.round(r6)
            r1 = r6
            goto L_0x0012
        L_0x003a:
            int r1 = com.oneplus.commonctrl.R$drawable.abc_dialog_material_background
            if (r6 != r1) goto L_0x003f
            goto L_0x0011
        L_0x003f:
            r6 = r3
            r1 = r6
            r2 = r1
        L_0x0042:
            if (r6 == 0) goto L_0x0060
            boolean r6 = com.oneplus.lib.app.appcompat.DrawableUtils.canSafelyMutateDrawable(r7)
            if (r6 == 0) goto L_0x004e
            android.graphics.drawable.Drawable r7 = r7.mutate()
        L_0x004e:
            int r5 = com.oneplus.lib.app.appcompat.ThemeUtils.getThemeAttrColor(r5, r2)
            android.graphics.PorterDuffColorFilter r5 = getPorterDuffColorFilter(r5, r0)
            r7.setColorFilter(r5)
            r5 = -1
            if (r1 == r5) goto L_0x005f
            r7.setAlpha(r1)
        L_0x005f:
            return r4
        L_0x0060:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.app.appcompat.AppCompatDrawableManager.tintDrawableUsingColorFilter(android.content.Context, int, android.graphics.drawable.Drawable):boolean");
    }

    private void addDelegate(String str, InflateDelegate inflateDelegate) {
        if (this.mDelegates == null) {
            this.mDelegates = new ArrayMap<>();
        }
        this.mDelegates.put(str, inflateDelegate);
    }

    private static boolean arrayContains(int[] iArr, int i) {
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    static Mode getTintMode(int i) {
        if (i == R$drawable.abc_switch_thumb_material) {
            return Mode.MULTIPLY;
        }
        return null;
    }

    /* access modifiers changed from: 0000 */
    public ColorStateList getTintList(Context context, int i) {
        return getTintList(context, i, null);
    }

    /* access modifiers changed from: 0000 */
    public ColorStateList getTintList(Context context, int i, ColorStateList colorStateList) {
        ColorStateList colorStateList2;
        boolean z = colorStateList == null;
        ColorStateList tintListFromCache = z ? getTintListFromCache(context, i) : null;
        if (tintListFromCache == null) {
            if (i == R$drawable.abc_edit_text_material) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_edittext);
            } else if (i == R$drawable.abc_switch_track_mtrl_alpha) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_switch_track);
            } else if (i == R$drawable.abc_switch_thumb_material) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.op_abc_tint_switch_thumb);
            } else if (i == R$drawable.abc_btn_default_mtrl_shape) {
                colorStateList2 = createDefaultButtonColorStateList(context, colorStateList);
            } else if (i == R$drawable.abc_btn_borderless_material) {
                colorStateList2 = createBorderlessButtonColorStateList(context, colorStateList);
            } else if (i == R$drawable.abc_btn_colored_material) {
                colorStateList2 = createColoredButtonColorStateList(context, colorStateList);
            } else if (i == R$drawable.abc_spinner_mtrl_am_alpha || i == R$drawable.abc_spinner_textfield_background_material) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_spinner);
            } else if (arrayContains(TINT_COLOR_CONTROL_NORMAL, i)) {
                colorStateList2 = ThemeUtils.getThemeAttrColorStateList(context, R$attr.colorControlNormal);
            } else if (arrayContains(TINT_COLOR_CONTROL_STATE_LIST, i)) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_default);
            } else if (arrayContains(TINT_CHECKABLE_BUTTON_LIST, i)) {
                colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_btn_checkable);
            } else {
                if (i == R$drawable.abc_seekbar_thumb_material) {
                    colorStateList2 = AppCompatResources.getColorStateList(context, R$color.abc_tint_seek_thumb);
                }
                if (z && tintListFromCache != null) {
                    addTintListToCache(context, i, tintListFromCache);
                }
            }
            tintListFromCache = colorStateList2;
            addTintListToCache(context, i, tintListFromCache);
        }
        return tintListFromCache;
    }

    private ColorStateList getTintListFromCache(Context context, int i) {
        WeakHashMap<Context, SparseArray<ColorStateList>> weakHashMap = this.mTintLists;
        if (weakHashMap == null) {
            return null;
        }
        SparseArray sparseArray = (SparseArray) weakHashMap.get(context);
        if (sparseArray != null) {
            return (ColorStateList) sparseArray.get(i);
        }
        return null;
    }

    private void addTintListToCache(Context context, int i, ColorStateList colorStateList) {
        if (this.mTintLists == null) {
            this.mTintLists = new WeakHashMap<>();
        }
        SparseArray sparseArray = (SparseArray) this.mTintLists.get(context);
        if (sparseArray == null) {
            sparseArray = new SparseArray();
            this.mTintLists.put(context, sparseArray);
        }
        sparseArray.append(i, colorStateList);
    }

    private ColorStateList createDefaultButtonColorStateList(Context context, ColorStateList colorStateList) {
        return createButtonColorStateList(context, ThemeUtils.getThemeAttrColor(context, R$attr.colorButtonNormal), colorStateList);
    }

    private ColorStateList createBorderlessButtonColorStateList(Context context, ColorStateList colorStateList) {
        return createButtonColorStateList(context, 0, null);
    }

    private ColorStateList createColoredButtonColorStateList(Context context, ColorStateList colorStateList) {
        return createButtonColorStateList(context, ThemeUtils.getThemeAttrColor(context, R$attr.colorAccent), colorStateList);
    }

    private ColorStateList createButtonColorStateList(Context context, int i, ColorStateList colorStateList) {
        int i2;
        int i3;
        int[][] iArr = new int[4][];
        int[] iArr2 = new int[4];
        int themeAttrColor = ThemeUtils.getThemeAttrColor(context, R$attr.colorControlHighlight);
        int disabledThemeAttrColor = ThemeUtils.getDisabledThemeAttrColor(context, R$attr.colorButtonNormal);
        iArr[0] = ThemeUtils.DISABLED_STATE_SET;
        if (colorStateList != null) {
            disabledThemeAttrColor = colorStateList.getColorForState(iArr[0], 0);
        }
        iArr2[0] = disabledThemeAttrColor;
        iArr[1] = ThemeUtils.PRESSED_STATE_SET;
        if (colorStateList == null) {
            i2 = i;
        } else {
            i2 = colorStateList.getColorForState(iArr[1], 0);
        }
        iArr2[1] = ColorUtils.compositeColors(themeAttrColor, i2);
        iArr[2] = ThemeUtils.FOCUSED_STATE_SET;
        if (colorStateList == null) {
            i3 = i;
        } else {
            i3 = colorStateList.getColorForState(iArr[2], 0);
        }
        iArr2[2] = ColorUtils.compositeColors(themeAttrColor, i3);
        iArr[3] = ThemeUtils.EMPTY_STATE_SET;
        if (colorStateList != null) {
            i = colorStateList.getColorForState(iArr[3], 0);
        }
        iArr2[3] = i;
        return new ColorStateList(iArr, iArr2);
    }

    public static PorterDuffColorFilter getPorterDuffColorFilter(int i, Mode mode) {
        PorterDuffColorFilter porterDuffColorFilter = COLOR_FILTER_CACHE.get(i, mode);
        if (porterDuffColorFilter != null) {
            return porterDuffColorFilter;
        }
        PorterDuffColorFilter porterDuffColorFilter2 = new PorterDuffColorFilter(i, mode);
        COLOR_FILTER_CACHE.put(i, mode, porterDuffColorFilter2);
        return porterDuffColorFilter2;
    }

    private static void setPorterDuffColorFilter(Drawable drawable, int i, Mode mode) {
        if (DrawableUtils.canSafelyMutateDrawable(drawable)) {
            drawable = drawable.mutate();
        }
        if (mode == null) {
            mode = DEFAULT_MODE;
        }
        drawable.setColorFilter(getPorterDuffColorFilter(i, mode));
    }

    private void checkVectorDrawableSetup(Context context) {
        if (!this.mHasCheckedVectorDrawableSetup) {
            this.mHasCheckedVectorDrawableSetup = true;
        }
    }
}
