package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.KeyEvent.DispatcherState;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filter.FilterListener;
import android.widget.ListAdapter;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;

public class AutoCompleteTextView extends EditText implements FilterListener {
    private ListAdapter mAdapter;
    private boolean mBlockCompletion;
    private int mDropDownAnchorId;
    private boolean mDropDownDismissedOnCompletion;
    private Filter mFilter;
    private int mHintResource;
    private CharSequence mHintText;
    private TextView mHintView;
    private OnItemClickListener mItemClickListener;
    private int mLastKeyCode;
    private boolean mOpenBefore;
    private final PassThroughClickListener mPassThroughClickListener;
    private final ListPopupWindow mPopup;
    private boolean mPopupCanBeUpdated;
    private final Context mPopupContext;
    private int mThreshold;
    private Validator mValidator;

    /* renamed from: com.oneplus.lib.widget.AutoCompleteTextView$1 */
    class C18691 implements OnDismissListener {
    }

    private class DropDownItemClickListener implements OnItemClickListener {
        private DropDownItemClickListener() {
        }

        /* synthetic */ DropDownItemClickListener(AutoCompleteTextView autoCompleteTextView, C18691 r2) {
            this();
        }

        public void onItemClick(AdapterView adapterView, View view, int i, long j) {
            AutoCompleteTextView.this.performCompletion(view, i, j);
        }
    }

    private class MyWatcher implements TextWatcher {
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        private MyWatcher() {
        }

        /* synthetic */ MyWatcher(AutoCompleteTextView autoCompleteTextView, C18691 r2) {
            this();
        }

        public void afterTextChanged(Editable editable) {
            AutoCompleteTextView.this.doAfterTextChanged();
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            AutoCompleteTextView.this.doBeforeTextChanged();
        }
    }

    private class PassThroughClickListener implements OnClickListener {
        /* access modifiers changed from: private */
        public OnClickListener mWrapped;

        private PassThroughClickListener() {
        }

        /* synthetic */ PassThroughClickListener(AutoCompleteTextView autoCompleteTextView, C18691 r2) {
            this();
        }

        public void onClick(View view) {
            AutoCompleteTextView.this.onClickImpl();
            OnClickListener onClickListener = this.mWrapped;
            if (onClickListener != null) {
                onClickListener.onClick(view);
            }
        }
    }

    public interface Validator {
        CharSequence fixText(CharSequence charSequence);

        boolean isValid(CharSequence charSequence);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842859);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        this(context, attributeSet, i, i2, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i, int i2, Theme theme) {
        super(context, attributeSet, i, i2);
        this.mDropDownDismissedOnCompletion = true;
        this.mLastKeyCode = 0;
        this.mValidator = null;
        this.mPopupCanBeUpdated = true;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPAutoCompleteTextView, i, i2);
        if (theme != null) {
            this.mPopupContext = new ContextThemeWrapper(context, theme);
        } else {
            int resourceId = obtainStyledAttributes.getResourceId(R$styleable.OPAutoCompleteTextView_android_popupTheme, 0);
            if (resourceId != 0) {
                this.mPopupContext = new ContextThemeWrapper(context, resourceId);
            } else {
                this.mPopupContext = context;
            }
        }
        Context context2 = this.mPopupContext;
        TypedArray obtainStyledAttributes2 = context2 != context ? context2.obtainStyledAttributes(attributeSet, R$styleable.OPAutoCompleteTextView, i, i2) : obtainStyledAttributes;
        Drawable drawable = obtainStyledAttributes2.getDrawable(R$styleable.OPAutoCompleteTextView_android_dropDownSelector);
        int layoutDimension = obtainStyledAttributes2.getLayoutDimension(R$styleable.OPAutoCompleteTextView_android_dropDownWidth, -2);
        int layoutDimension2 = obtainStyledAttributes2.getLayoutDimension(R$styleable.OPAutoCompleteTextView_android_dropDownHeight, -2);
        int resourceId2 = obtainStyledAttributes2.getResourceId(R$styleable.OPAutoCompleteTextView_android_completionHintView, R$layout.op_simple_dropdown_hint);
        CharSequence text = obtainStyledAttributes2.getText(R$styleable.OPAutoCompleteTextView_android_completionHint);
        if (obtainStyledAttributes2 != obtainStyledAttributes) {
            obtainStyledAttributes2.recycle();
        }
        this.mPopup = new ListPopupWindow(this.mPopupContext, attributeSet, i, i2);
        this.mPopup.setSoftInputMode(16);
        this.mPopup.setPromptPosition(1);
        this.mPopup.setListSelector(drawable);
        this.mPopup.setOnItemClickListener(new DropDownItemClickListener(this, null));
        this.mPopup.setWidth(layoutDimension);
        this.mPopup.setHeight(layoutDimension2);
        this.mHintResource = resourceId2;
        setCompletionHint(text);
        this.mDropDownAnchorId = obtainStyledAttributes.getResourceId(R$styleable.OPAutoCompleteTextView_android_dropDownAnchor, -1);
        this.mThreshold = obtainStyledAttributes.getInt(R$styleable.OPAutoCompleteTextView_android_completionThreshold, 2);
        obtainStyledAttributes.recycle();
        int inputType = getInputType();
        if ((inputType & 15) == 1) {
            setRawInputType(inputType | 65536);
        }
        setFocusable(true);
        addTextChangedListener(new MyWatcher(this, null));
        this.mPassThroughClickListener = new PassThroughClickListener(this, null);
        super.setOnClickListener(this.mPassThroughClickListener);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mPassThroughClickListener.mWrapped = onClickListener;
    }

    /* access modifiers changed from: private */
    public void onClickImpl() {
        if (isPopupShowing()) {
            ensureImeVisible(true);
        }
    }

    public void setCompletionHint(CharSequence charSequence) {
        this.mHintText = charSequence;
        if (charSequence != null) {
            TextView textView = this.mHintView;
            if (textView == null) {
                TextView textView2 = (TextView) LayoutInflater.from(this.mPopupContext).inflate(this.mHintResource, null).findViewById(16908308);
                textView2.setText(this.mHintText);
                this.mHintView = textView2;
                this.mPopup.setPromptView(textView2);
                return;
            }
            textView.setText(charSequence);
            return;
        }
        this.mPopup.setPromptView(null);
        this.mHintView = null;
    }

    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (i == 4 && isPopupShowing() && !this.mPopup.isDropDownAlwaysVisible()) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                DispatcherState keyDispatcherState = getKeyDispatcherState();
                if (keyDispatcherState != null) {
                    keyDispatcherState.startTracking(keyEvent, this);
                }
                return true;
            } else if (keyEvent.getAction() == 1) {
                DispatcherState keyDispatcherState2 = getKeyDispatcherState();
                if (keyDispatcherState2 != null) {
                    keyDispatcherState2.handleUpEvent(keyEvent);
                }
                if (keyEvent.isTracking() && !keyEvent.isCanceled()) {
                    dismissDropDown();
                    return true;
                }
            }
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mPopup.onKeyUp(i, keyEvent) && (i == 23 || i == 61 || i == 66)) {
            if (keyEvent.hasNoModifiers()) {
                performCompletion();
            }
            return true;
        } else if (!isPopupShowing() || i != 61 || !keyEvent.hasNoModifiers()) {
            return super.onKeyUp(i, keyEvent);
        } else {
            performCompletion();
            return true;
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mPopup.onKeyDown(i, keyEvent)) {
            return true;
        }
        if (!isPopupShowing() && i == 20 && keyEvent.hasNoModifiers()) {
            performValidation();
        }
        if (isPopupShowing() && i == 61 && keyEvent.hasNoModifiers()) {
            return true;
        }
        this.mLastKeyCode = i;
        boolean onKeyDown = super.onKeyDown(i, keyEvent);
        this.mLastKeyCode = 0;
        if (onKeyDown && isPopupShowing()) {
            clearListSelection();
        }
        return onKeyDown;
    }

    public boolean enoughToFilter() {
        return getText().length() >= this.mThreshold;
    }

    public void doBeforeTextChanged() {
        if (!this.mBlockCompletion) {
            this.mOpenBefore = isPopupShowing();
        }
    }

    public void doAfterTextChanged() {
        if (!this.mBlockCompletion) {
            if (!this.mOpenBefore || isPopupShowing()) {
                if (!enoughToFilter()) {
                    if (!this.mPopup.isDropDownAlwaysVisible()) {
                        dismissDropDown();
                    }
                    Filter filter = this.mFilter;
                    if (filter != null) {
                        filter.filter(null);
                    }
                } else if (this.mFilter != null) {
                    this.mPopupCanBeUpdated = true;
                    performFiltering(getText(), this.mLastKeyCode);
                }
            }
        }
    }

    public boolean isPopupShowing() {
        return this.mPopup.isShowing();
    }

    /* access modifiers changed from: protected */
    public CharSequence convertSelectionToString(Object obj) {
        return this.mFilter.convertResultToString(obj);
    }

    public void clearListSelection() {
        this.mPopup.clearListSelection();
    }

    /* access modifiers changed from: protected */
    public void performFiltering(CharSequence charSequence, int i) {
        this.mFilter.filter(charSequence, this);
    }

    public void performCompletion() {
        performCompletion(null, -1, -1);
    }

    public void onCommitCompletion(CompletionInfo completionInfo) {
        if (isPopupShowing()) {
            this.mPopup.performItemClick(completionInfo.getPosition());
        }
    }

    /* access modifiers changed from: private */
    public void performCompletion(View view, int i, long j) {
        Object obj;
        if (isPopupShowing()) {
            if (i < 0) {
                obj = this.mPopup.getSelectedItem();
            } else {
                obj = this.mAdapter.getItem(i);
            }
            if (obj == null) {
                Log.w("AutoCompleteTextView", "performCompletion: no selected item");
                return;
            }
            this.mBlockCompletion = true;
            replaceText(convertSelectionToString(obj));
            this.mBlockCompletion = false;
            if (this.mItemClickListener != null) {
                ListPopupWindow listPopupWindow = this.mPopup;
                if (view == null || i < 0) {
                    view = listPopupWindow.getSelectedView();
                    i = listPopupWindow.getSelectedItemPosition();
                    j = listPopupWindow.getSelectedItemId();
                }
                this.mItemClickListener.onItemClick(listPopupWindow.getListView(), view, i, j);
            }
        }
        if (this.mDropDownDismissedOnCompletion && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    /* access modifiers changed from: protected */
    public void replaceText(CharSequence charSequence) {
        clearComposingText();
        setText(charSequence);
        Editable text = getText();
        Selection.setSelection(text, text.length());
    }

    public void onFilterComplete(int i) {
        updateDropDownForFilter(i);
    }

    private void updateDropDownForFilter(int i) {
        if (getWindowVisibility() != 8) {
            boolean isDropDownAlwaysVisible = this.mPopup.isDropDownAlwaysVisible();
            boolean enoughToFilter = enoughToFilter();
            if ((i > 0 || isDropDownAlwaysVisible) && enoughToFilter) {
                if (hasFocus() && hasWindowFocus() && this.mPopupCanBeUpdated) {
                    showDropDown();
                }
            } else if (!isDropDownAlwaysVisible && isPopupShowing()) {
                dismissDropDown();
                this.mPopupCanBeUpdated = true;
            }
        }
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (!z && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    /* access modifiers changed from: protected */
    public void onDisplayHint(int i) {
        super.onDisplayHint(i);
        if (i == 4 && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    /* access modifiers changed from: protected */
    public void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (VERSION.SDK_INT < 24 || !isTemporarilyDetached()) {
            if (!z) {
                performValidation();
            }
            if (!z && !this.mPopup.isDropDownAlwaysVisible()) {
                dismissDropDown();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        dismissDropDown();
        super.onDetachedFromWindow();
    }

    public void dismissDropDown() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.displayCompletions(this, null);
        }
        this.mPopup.dismiss();
        this.mPopupCanBeUpdated = false;
    }

    /* access modifiers changed from: protected */
    public boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        if (isPopupShowing()) {
            showDropDown();
        }
        return frame;
    }

    public void ensureImeVisible(boolean z) {
        this.mPopup.setInputMethodMode(z ? 1 : 2);
        if (this.mPopup.isDropDownAlwaysVisible() || (this.mFilter != null && enoughToFilter())) {
            showDropDown();
        }
    }

    public void showDropDown() {
        buildImeCompletions();
        if (this.mPopup.getAnchorView() == null) {
            if (this.mDropDownAnchorId != -1) {
                this.mPopup.setAnchorView(getRootView().findViewById(this.mDropDownAnchorId));
            } else {
                this.mPopup.setAnchorView(this);
            }
        }
        if (!isPopupShowing()) {
            this.mPopup.setInputMethodMode(1);
            this.mPopup.setListItemExpandMax(3);
        }
        this.mPopup.show();
        this.mPopup.getListView().setOverScrollMode(0);
    }

    private void buildImeCompletions() {
        CompletionInfo[] completionInfoArr;
        ListAdapter listAdapter = this.mAdapter;
        if (listAdapter != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
            if (inputMethodManager != null) {
                int min = Math.min(listAdapter.getCount(), 20);
                CompletionInfo[] completionInfoArr2 = new CompletionInfo[min];
                int i = 0;
                for (int i2 = 0; i2 < min; i2++) {
                    if (listAdapter.isEnabled(i2)) {
                        completionInfoArr2[i] = new CompletionInfo(listAdapter.getItemId(i2), i, convertSelectionToString(listAdapter.getItem(i2)));
                        i++;
                    }
                }
                if (i != min) {
                    completionInfoArr = new CompletionInfo[i];
                    System.arraycopy(completionInfoArr2, 0, completionInfoArr, 0, i);
                } else {
                    completionInfoArr = completionInfoArr2;
                }
                inputMethodManager.displayCompletions(this, completionInfoArr);
            }
        }
    }

    public void performValidation() {
        if (this.mValidator != null) {
            Editable text = getText();
            if (!TextUtils.isEmpty(text) && !this.mValidator.isValid(text)) {
                setText(this.mValidator.fixText(text));
            }
        }
    }
}
