package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SpinnerAdapter;
import android.widget.ThemedSpinnerAdapter;
import com.oneplus.lib.widget.util.ViewUtils;

public class Spinner extends android.widget.Spinner {
    private static final int[] ATTRS_ANDROID_SPINNERMODE = {16843505};
    private static final boolean IS_AT_LEAST_JB = (VERSION.SDK_INT >= 16);
    static final boolean IS_AT_LEAST_M = (VERSION.SDK_INT >= 23);
    /* access modifiers changed from: private */
    public DropDownAdapter mDropDownAdapter;
    /* access modifiers changed from: private */
    public int mDropDownWidth;
    private ForwardingListener mForwardingListener;
    /* access modifiers changed from: private */
    public DropdownPopup mPopup;
    private final Context mPopupContext;
    private final boolean mPopupSet;
    private Drawable[] mSelectedItemBackground;
    private SpinnerAdapter mTempAdapter;
    /* access modifiers changed from: private */
    public final Rect mTempRect;

    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter {
        private SpinnerAdapter mAdapter;
        private int mLastSelectedPosition = -1;
        private ListAdapter mListAdapter;
        private Drawable[] mSelectedItemBackground;

        public int getItemViewType(int i) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public DropDownAdapter(SpinnerAdapter spinnerAdapter, Theme theme) {
            this.mAdapter = spinnerAdapter;
            if (spinnerAdapter instanceof ListAdapter) {
                this.mListAdapter = (ListAdapter) spinnerAdapter;
            }
            if (theme == null) {
                return;
            }
            if (Spinner.IS_AT_LEAST_M && (spinnerAdapter instanceof ThemedSpinnerAdapter)) {
                ThemedSpinnerAdapter themedSpinnerAdapter = (ThemedSpinnerAdapter) spinnerAdapter;
                if (themedSpinnerAdapter.getDropDownViewTheme() != theme) {
                    themedSpinnerAdapter.setDropDownViewTheme(theme);
                }
            } else if (spinnerAdapter instanceof ThemedSpinnerAdapter) {
                ThemedSpinnerAdapter themedSpinnerAdapter2 = (ThemedSpinnerAdapter) spinnerAdapter;
                if (VERSION.SDK_INT >= 23 && themedSpinnerAdapter2.getDropDownViewTheme() == null) {
                    themedSpinnerAdapter2.setDropDownViewTheme(theme);
                }
            }
        }

        public void setSelectedItemBackground(Drawable[] drawableArr) {
            this.mSelectedItemBackground = drawableArr;
        }

        public int getCount() {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter == null) {
                return 0;
            }
            return spinnerAdapter.getCount();
        }

        public Object getItem(int i) {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter == null) {
                return null;
            }
            return spinnerAdapter.getItem(i);
        }

        public long getItemId(int i) {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter == null) {
                return -1;
            }
            return spinnerAdapter.getItemId(i);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            return getDropDownView(i, view, viewGroup);
        }

        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter == null) {
                return null;
            }
            View dropDownView = spinnerAdapter.getDropDownView(i, view, viewGroup);
            if (i != this.mLastSelectedPosition) {
                dropDownView.setBackground(null);
            } else if (i == 0) {
                dropDownView.setBackground(this.mSelectedItemBackground[0]);
            } else if (i == getCount() - 1) {
                dropDownView.setBackground(this.mSelectedItemBackground[2]);
            } else {
                dropDownView.setBackground(this.mSelectedItemBackground[1]);
            }
            return dropDownView;
        }

        public boolean hasStableIds() {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            return spinnerAdapter != null && spinnerAdapter.hasStableIds();
        }

        public void setSelectedItem(int i) {
            this.mLastSelectedPosition = i;
        }

        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter != null) {
                spinnerAdapter.registerDataSetObserver(dataSetObserver);
            }
        }

        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
            SpinnerAdapter spinnerAdapter = this.mAdapter;
            if (spinnerAdapter != null) {
                spinnerAdapter.unregisterDataSetObserver(dataSetObserver);
            }
        }

        public boolean areAllItemsEnabled() {
            ListAdapter listAdapter = this.mListAdapter;
            if (listAdapter != null) {
                return listAdapter.areAllItemsEnabled();
            }
            return true;
        }

        public boolean isEnabled(int i) {
            ListAdapter listAdapter = this.mListAdapter;
            if (listAdapter != null) {
                return listAdapter.isEnabled(i);
            }
            return true;
        }

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    private class DropdownPopup extends ListPopupWindow {
        ListAdapter mAdapter;
        private CharSequence mHintText;
        private final Rect mVisibleRect = new Rect();

        /* access modifiers changed from: protected */
        public boolean needInterceptorTouchEvent() {
            return false;
        }

        public DropdownPopup(Context context, AttributeSet attributeSet, int i) {
            super(context, attributeSet, i);
            setAnchorView(Spinner.this);
            setModal(true);
            setPromptPosition(0);
            setOnItemClickListener(new OnItemClickListener(Spinner.this) {
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                    Spinner.this.setSelection(i);
                    if (Spinner.this.getOnItemClickListener() != null) {
                        DropdownPopup dropdownPopup = DropdownPopup.this;
                        Spinner.this.performItemClick(view, i, dropdownPopup.mAdapter.getItemId(i));
                    }
                    DropdownPopup.this.dismiss();
                }
            });
        }

        public void setAdapter(ListAdapter listAdapter) {
            super.setAdapter(listAdapter);
            this.mAdapter = listAdapter;
        }

        public CharSequence getHintText() {
            return this.mHintText;
        }

        public void setPromptText(CharSequence charSequence) {
            this.mHintText = charSequence;
        }

        /* access modifiers changed from: 0000 */
        public void computeContentWidth() {
            int i;
            Drawable background = getBackground();
            int i2 = 0;
            if (background != null) {
                background.getPadding(Spinner.this.mTempRect);
                if (ViewUtils.isLayoutRtl(Spinner.this)) {
                    i = Spinner.this.mTempRect.right;
                } else {
                    i = -Spinner.this.mTempRect.left;
                }
                i2 = i;
            } else {
                Rect access$100 = Spinner.this.mTempRect;
                Spinner.this.mTempRect.right = 0;
                access$100.left = 0;
            }
            int paddingLeft = Spinner.this.getPaddingLeft();
            int paddingRight = Spinner.this.getPaddingRight();
            int width = Spinner.this.getWidth();
            if (Spinner.this.mDropDownWidth == -2) {
                int compatMeasureContentWidth = Spinner.this.compatMeasureContentWidth((SpinnerAdapter) this.mAdapter, getBackground());
                int i3 = (Spinner.this.getContext().getResources().getDisplayMetrics().widthPixels - Spinner.this.mTempRect.left) - Spinner.this.mTempRect.right;
                if (compatMeasureContentWidth > i3) {
                    compatMeasureContentWidth = i3;
                }
                setContentWidth(Math.max(compatMeasureContentWidth, (width - paddingLeft) - paddingRight));
            } else if (Spinner.this.mDropDownWidth == -1) {
                setContentWidth((width - paddingLeft) - paddingRight);
            } else {
                setContentWidth(Spinner.this.mDropDownWidth);
            }
            setHorizontalOffset(ViewUtils.isLayoutRtl(Spinner.this) ? i2 + ((width - paddingRight) - getWidth()) : i2 + paddingLeft);
        }

        public void show() {
            boolean isShowing = isShowing();
            computeContentWidth();
            setInputMethodMode(2);
            super.show();
            getListView().setChoiceMode(1);
            setSelection(Spinner.this.getSelectedItemPosition());
            if (Spinner.this.mDropDownAdapter != null) {
                Spinner.this.mDropDownAdapter.setSelectedItem(Spinner.this.getSelectedItemPosition());
            }
            if (!isShowing) {
                ViewTreeObserver viewTreeObserver = Spinner.this.getViewTreeObserver();
                if (viewTreeObserver != null) {
                    final C19072 r1 = new OnGlobalLayoutListener() {
                        public void onGlobalLayout() {
                            DropdownPopup dropdownPopup = DropdownPopup.this;
                            if (!dropdownPopup.isVisibleToUser(Spinner.this)) {
                                DropdownPopup.this.dismiss();
                                return;
                            }
                            DropdownPopup.this.computeContentWidth();
                            DropdownPopup.super.show();
                        }
                    };
                    viewTreeObserver.addOnGlobalLayoutListener(r1);
                    setOnDismissListener(new OnDismissListener() {
                        public void onDismiss() {
                            ViewTreeObserver viewTreeObserver = Spinner.this.getViewTreeObserver();
                            if (viewTreeObserver != null) {
                                viewTreeObserver.removeGlobalOnLayoutListener(r1);
                            }
                        }
                    });
                }
            }
        }

        /* access modifiers changed from: 0000 */
        public boolean isVisibleToUser(View view) {
            return view.isAttachedToWindow() && view.getGlobalVisibleRect(this.mVisibleRect);
        }
    }

    public Spinner(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842881);
    }

    public Spinner(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, -1);
    }

    public Spinner(Context context, AttributeSet attributeSet, int i, int i2) {
        this(context, attributeSet, i, i2, null);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x009c, code lost:
        if (r12 != null) goto L_0x009e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x009e, code lost:
        r12.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00b0, code lost:
        if (r12 != null) goto L_0x009e;
     */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Spinner(android.content.Context r8, android.util.AttributeSet r9, int r10, int r11, android.content.res.Resources.Theme r12) {
        /*
            r7 = this;
            r7.<init>(r8, r9, r10)
            r0 = 3
            android.graphics.drawable.Drawable[] r0 = new android.graphics.drawable.Drawable[r0]
            r7.mSelectedItemBackground = r0
            android.graphics.Rect r0 = new android.graphics.Rect
            r0.<init>()
            r7.mTempRect = r0
            int[] r0 = com.oneplus.commonctrl.R$styleable.Spinner
            r1 = 0
            android.content.res.TypedArray r0 = r8.obtainStyledAttributes(r9, r0, r10, r1)
            int r2 = com.oneplus.commonctrl.R$styleable.Spinner_android_popupTheme
            int r2 = r0.getResourceId(r2, r1)
            if (r12 == 0) goto L_0x0034
            int r3 = android.os.Build.VERSION.SDK_INT
            r4 = 23
            if (r3 < r4) goto L_0x002c
            android.view.ContextThemeWrapper r2 = new android.view.ContextThemeWrapper
            r2.<init>(r8, r12)
            r7.mPopupContext = r2
            goto L_0x0040
        L_0x002c:
            android.view.ContextThemeWrapper r12 = new android.view.ContextThemeWrapper
            r12.<init>(r8, r2)
            r7.mPopupContext = r12
            goto L_0x0040
        L_0x0034:
            if (r2 == 0) goto L_0x003e
            android.view.ContextThemeWrapper r12 = new android.view.ContextThemeWrapper
            r12.<init>(r8, r2)
            r7.mPopupContext = r12
            goto L_0x0040
        L_0x003e:
            r7.mPopupContext = r8
        L_0x0040:
            android.graphics.drawable.Drawable[] r12 = r7.mSelectedItemBackground
            android.content.res.Resources r2 = r7.getResources()
            int r3 = com.oneplus.commonctrl.R$drawable.op_drop_down_item_background_top
            android.content.Context r4 = r7.mPopupContext
            android.content.res.Resources$Theme r4 = r4.getTheme()
            android.graphics.drawable.Drawable r2 = r2.getDrawable(r3, r4)
            r12[r1] = r2
            android.graphics.drawable.Drawable[] r12 = r7.mSelectedItemBackground
            r2 = 2
            android.content.res.Resources r3 = r7.getResources()
            int r4 = com.oneplus.commonctrl.R$drawable.op_drop_down_item_background_bottom
            android.content.Context r5 = r7.mPopupContext
            android.content.res.Resources$Theme r5 = r5.getTheme()
            android.graphics.drawable.Drawable r3 = r3.getDrawable(r4, r5)
            r12[r2] = r3
            android.graphics.drawable.Drawable[] r12 = r7.mSelectedItemBackground
            android.content.res.Resources r2 = r7.getResources()
            int r3 = com.oneplus.commonctrl.R$drawable.op_drop_down_item_background
            android.content.Context r4 = r7.mPopupContext
            android.content.res.Resources$Theme r4 = r4.getTheme()
            android.graphics.drawable.Drawable r2 = r2.getDrawable(r3, r4)
            r3 = 1
            r12[r3] = r2
            android.content.Context r12 = r7.mPopupContext
            r2 = 0
            if (r12 == 0) goto L_0x00f3
            r12 = -1
            if (r11 != r12) goto L_0x00bb
            int r12 = android.os.Build.VERSION.SDK_INT
            r4 = 11
            if (r12 < r4) goto L_0x00ba
            int[] r12 = ATTRS_ANDROID_SPINNERMODE     // Catch:{ Exception -> 0x00a7, all -> 0x00a4 }
            android.content.res.TypedArray r12 = r8.obtainStyledAttributes(r9, r12, r10, r1)     // Catch:{ Exception -> 0x00a7, all -> 0x00a4 }
            boolean r4 = r12.hasValue(r1)     // Catch:{ Exception -> 0x00a2 }
            if (r4 == 0) goto L_0x009c
            int r11 = r12.getInt(r1, r1)     // Catch:{ Exception -> 0x00a2 }
        L_0x009c:
            if (r12 == 0) goto L_0x00bb
        L_0x009e:
            r12.recycle()
            goto L_0x00bb
        L_0x00a2:
            r4 = move-exception
            goto L_0x00a9
        L_0x00a4:
            r7 = move-exception
            r12 = r2
            goto L_0x00b4
        L_0x00a7:
            r4 = move-exception
            r12 = r2
        L_0x00a9:
            java.lang.String r5 = "OpSpinner"
            java.lang.String r6 = "Could not read android:spinnerMode"
            android.util.Log.i(r5, r6, r4)     // Catch:{ all -> 0x00b3 }
            if (r12 == 0) goto L_0x00bb
            goto L_0x009e
        L_0x00b3:
            r7 = move-exception
        L_0x00b4:
            if (r12 == 0) goto L_0x00b9
            r12.recycle()
        L_0x00b9:
            throw r7
        L_0x00ba:
            r11 = r3
        L_0x00bb:
            if (r11 != r3) goto L_0x00f3
            com.oneplus.lib.widget.Spinner$DropdownPopup r11 = new com.oneplus.lib.widget.Spinner$DropdownPopup
            android.content.Context r12 = r7.mPopupContext
            r11.<init>(r12, r9, r10)
            android.content.Context r12 = r7.mPopupContext
            int[] r4 = com.oneplus.commonctrl.R$styleable.Spinner
            android.content.res.TypedArray r9 = r12.obtainStyledAttributes(r9, r4, r10, r1)
            int r10 = com.oneplus.commonctrl.R$styleable.Spinner_android_dropDownWidth
            r12 = -2
            int r10 = r9.getLayoutDimension(r10, r12)
            r7.mDropDownWidth = r10
            int r10 = com.oneplus.commonctrl.R$styleable.Spinner_android_popupBackground
            android.graphics.drawable.Drawable r10 = r9.getDrawable(r10)
            r11.setBackgroundDrawable(r10)
            int r10 = com.oneplus.commonctrl.R$styleable.Spinner_android_prompt
            java.lang.String r10 = r0.getString(r10)
            r11.setPromptText(r10)
            r9.recycle()
            r7.mPopup = r11
            com.oneplus.lib.widget.Spinner$1 r9 = new com.oneplus.lib.widget.Spinner$1
            r9.<init>(r7, r11)
            r7.mForwardingListener = r9
        L_0x00f3:
            int r9 = com.oneplus.commonctrl.R$styleable.Spinner_android_entries
            java.lang.CharSequence[] r9 = r0.getTextArray(r9)
            if (r9 == 0) goto L_0x010c
            android.widget.ArrayAdapter r10 = new android.widget.ArrayAdapter
            r11 = 17367048(0x1090008, float:2.5162948E-38)
            r10.<init>(r8, r11, r9)
            r8 = 17367049(0x1090009, float:2.516295E-38)
            r10.setDropDownViewResource(r8)
            r7.setAdapter(r10)
        L_0x010c:
            r0.recycle()
            r7.mPopupSet = r3
            android.widget.SpinnerAdapter r8 = r7.mTempAdapter
            if (r8 == 0) goto L_0x011a
            r7.setAdapter(r8)
            r7.mTempAdapter = r2
        L_0x011a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.Spinner.<init>(android.content.Context, android.util.AttributeSet, int, int, android.content.res.Resources$Theme):void");
    }

    public Context getPopupContext() {
        if (this.mPopup != null) {
            return this.mPopupContext;
        }
        if (IS_AT_LEAST_M) {
            return super.getPopupContext();
        }
        return null;
    }

    public void setPopupBackgroundDrawable(Drawable drawable) {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            dropdownPopup.setBackgroundDrawable(drawable);
        } else if (IS_AT_LEAST_JB) {
            super.setPopupBackgroundDrawable(drawable);
        }
    }

    public void setPopupBackgroundResource(int i) {
        setPopupBackgroundDrawable(getResources().getDrawable(i));
    }

    public Drawable getPopupBackground() {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            return dropdownPopup.getBackground();
        }
        if (IS_AT_LEAST_JB) {
            return super.getPopupBackground();
        }
        return null;
    }

    public void setDropDownVerticalOffset(int i) {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            dropdownPopup.setVerticalOffset(i);
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownVerticalOffset(i);
        }
    }

    public int getDropDownVerticalOffset() {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            return dropdownPopup.getVerticalOffset();
        }
        if (IS_AT_LEAST_JB) {
            return super.getDropDownVerticalOffset();
        }
        return 0;
    }

    public void setDropDownHorizontalOffset(int i) {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            dropdownPopup.setHorizontalOffset(i);
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownHorizontalOffset(i);
        }
    }

    public int getDropDownHorizontalOffset() {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            return dropdownPopup.getHorizontalOffset();
        }
        if (IS_AT_LEAST_JB) {
            return super.getDropDownHorizontalOffset();
        }
        return 0;
    }

    public void setDropDownWidth(int i) {
        if (this.mPopup != null) {
            this.mDropDownWidth = i;
        } else if (IS_AT_LEAST_JB) {
            super.setDropDownWidth(i);
        }
    }

    public int getDropDownWidth() {
        if (this.mPopup != null) {
            return this.mDropDownWidth;
        }
        if (IS_AT_LEAST_JB) {
            return super.getDropDownWidth();
        }
        return 0;
    }

    public void setAdapter(SpinnerAdapter spinnerAdapter) {
        if (!this.mPopupSet) {
            this.mTempAdapter = spinnerAdapter;
            return;
        }
        super.setAdapter(spinnerAdapter);
        if (this.mPopup != null) {
            Context context = this.mPopupContext;
            if (context == null) {
                context = getContext();
            }
            this.mDropDownAdapter = new DropDownAdapter(spinnerAdapter, context.getTheme());
            this.mDropDownAdapter.setSelectedItemBackground(this.mSelectedItemBackground);
            this.mPopup.setAdapter(this.mDropDownAdapter);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null && dropdownPopup.isShowing()) {
            this.mPopup.dismiss();
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        ForwardingListener forwardingListener = this.mForwardingListener;
        if (forwardingListener == null || !forwardingListener.onTouch(this, motionEvent)) {
            return super.onTouchEvent(motionEvent);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mPopup != null && MeasureSpec.getMode(i) == Integer.MIN_VALUE) {
            setMeasuredDimension(Math.min(Math.max(getMeasuredWidth(), compatMeasureContentWidth(getAdapter(), getBackground())), MeasureSpec.getSize(i)), getMeasuredHeight());
        }
    }

    public boolean performClick() {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup == null) {
            return super.performClick();
        }
        if (!dropdownPopup.isShowing()) {
            this.mPopup.show();
        }
        return true;
    }

    public void setPrompt(CharSequence charSequence) {
        DropdownPopup dropdownPopup = this.mPopup;
        if (dropdownPopup != null) {
            dropdownPopup.setPromptText(charSequence);
        } else {
            super.setPrompt(charSequence);
        }
    }

    public CharSequence getPrompt() {
        DropdownPopup dropdownPopup = this.mPopup;
        return dropdownPopup != null ? dropdownPopup.getHintText() : super.getPrompt();
    }

    /* access modifiers changed from: 0000 */
    public int compatMeasureContentWidth(SpinnerAdapter spinnerAdapter, Drawable drawable) {
        int i = 0;
        if (spinnerAdapter == null) {
            return 0;
        }
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 0);
        int makeMeasureSpec2 = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 0);
        int max = Math.max(0, getSelectedItemPosition());
        int min = Math.min(spinnerAdapter.getCount(), max + 15);
        int i2 = 0;
        View view = null;
        for (int max2 = Math.max(0, max - (15 - (min - max))); max2 < min; max2++) {
            int itemViewType = spinnerAdapter.getItemViewType(max2);
            if (itemViewType != i) {
                view = null;
                i = itemViewType;
            }
            view = spinnerAdapter.getView(max2, view, this);
            if (view.getLayoutParams() == null) {
                view.setLayoutParams(new LayoutParams(-2, -2));
            }
            view.measure(makeMeasureSpec, makeMeasureSpec2);
            i2 = Math.max(i2, view.getMeasuredWidth());
        }
        if (drawable != null) {
            drawable.getPadding(this.mTempRect);
            Rect rect = this.mTempRect;
            i2 += rect.left + rect.right;
        }
        return i2;
    }
}
