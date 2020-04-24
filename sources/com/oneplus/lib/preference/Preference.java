package com.oneplus.lib.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$styleable;
import java.util.ArrayList;
import java.util.List;

public class Preference implements Comparable<Preference> {
    private boolean mCanRecycleLayout;
    private Context mContext;
    private Object mDefaultValue;
    private String mDependencyKey;
    private boolean mDependencyMet;
    private List<Preference> mDependents;
    private boolean mEnabled;
    private String mFragment;
    private Drawable mIcon;
    private int mIconResId;
    private long mId;
    private View mImageFrame;
    private Intent mIntent;
    private boolean mIsAvatarIcon;
    private String mKey;
    private int mLayoutResId;
    private OnPreferenceChangeInternalListener mListener;
    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;
    private int mOrder;
    private boolean mParentDependencyMet;
    private boolean mPersistent;
    private PreferenceManager mPreferenceManager;
    private Drawable mSecondaryIcon;
    private int mSecondaryIconResId;
    private boolean mSelectable;
    private boolean mShouldDisableView;
    private CharSequence mSummary;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;
    private CharSequence mTitle;
    private int mTitleRes;
    private ViewGroup mWidgetFrame;
    private int mWidgetLayoutResId;

    public static class BaseSavedState extends AbsSavedState {
        public static final Creator<BaseSavedState> CREATOR = new Creator<BaseSavedState>() {
            public BaseSavedState createFromParcel(Parcel parcel) {
                return new BaseSavedState(parcel);
            }

            public BaseSavedState[] newArray(int i) {
                return new BaseSavedState[i];
            }
        };

        public BaseSavedState(Parcel parcel) {
            super(parcel);
        }
    }

    interface OnPreferenceChangeInternalListener {
        void onPreferenceChange(Preference preference);
    }

    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(Preference preference, Object obj);
    }

    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(Preference preference);
    }

    /* access modifiers changed from: protected */
    public void onClick() {
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return null;
    }

    public Preference(Context context, AttributeSet attributeSet, int i, int i2) {
        this.mOrder = Integer.MAX_VALUE;
        this.mEnabled = true;
        this.mSelectable = true;
        this.mPersistent = true;
        this.mDependencyMet = true;
        this.mParentDependencyMet = true;
        this.mShouldDisableView = true;
        this.mLayoutResId = R$layout.op_preference;
        this.mCanRecycleLayout = true;
        this.mContext = context;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.Preference, i, i2);
        this.mIsAvatarIcon = obtainStyledAttributes.getBoolean(R$styleable.Preference_opUseAvatarIcon, false);
        StringBuilder sb = new StringBuilder();
        sb.append("mIsAvatarIcon = ");
        sb.append(this.mIsAvatarIcon);
        Log.i("Preference", sb.toString());
        this.mIconResId = obtainStyledAttributes.getResourceId(R$styleable.Preference_android_icon, 0);
        this.mKey = obtainStyledAttributes.getString(R$styleable.Preference_android_key);
        this.mTitleRes = obtainStyledAttributes.getResourceId(R$styleable.Preference_android_title, 0);
        this.mTitle = obtainStyledAttributes.getString(R$styleable.Preference_android_title);
        this.mSummary = obtainStyledAttributes.getString(R$styleable.Preference_android_summary);
        this.mOrder = obtainStyledAttributes.getInt(R$styleable.Preference_android_order, this.mOrder);
        this.mFragment = obtainStyledAttributes.getString(R$styleable.Preference_android_fragment);
        this.mLayoutResId = obtainStyledAttributes.getResourceId(R$styleable.Preference_android_layout, this.mLayoutResId);
        this.mWidgetLayoutResId = obtainStyledAttributes.getResourceId(R$styleable.Preference_android_widgetLayout, this.mWidgetLayoutResId);
        this.mEnabled = obtainStyledAttributes.getBoolean(R$styleable.Preference_android_enabled, true);
        this.mSelectable = obtainStyledAttributes.getBoolean(R$styleable.Preference_android_selectable, true);
        this.mPersistent = obtainStyledAttributes.getBoolean(R$styleable.Preference_android_persistent, this.mPersistent);
        this.mDependencyKey = obtainStyledAttributes.getString(R$styleable.Preference_android_dependency);
        if (obtainStyledAttributes.hasValue(R$styleable.Preference_android_defaultValue)) {
            this.mDefaultValue = onGetDefaultValue(obtainStyledAttributes, R$styleable.Preference_android_defaultValue);
        }
        this.mShouldDisableView = obtainStyledAttributes.getBoolean(R$styleable.Preference_android_shouldDisableView, this.mShouldDisableView);
        obtainStyledAttributes.recycle();
    }

    public Preference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Preference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_preferenceStyle);
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public String getFragment() {
        return this.mFragment;
    }

    public void setLayoutResource(int i) {
        if (i != this.mLayoutResId) {
            this.mCanRecycleLayout = false;
        }
        this.mLayoutResId = i;
    }

    public int getLayoutResource() {
        return this.mLayoutResId;
    }

    public int getWidgetLayoutResource() {
        return this.mWidgetLayoutResId;
    }

    public View getView(View view, ViewGroup viewGroup) {
        if (view == null) {
            view = onCreateView(viewGroup);
        }
        onBindView(view);
        return view;
    }

    /* access modifiers changed from: protected */
    public View onCreateView(ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View inflate = layoutInflater.inflate(this.mLayoutResId, viewGroup, false);
        this.mWidgetFrame = (ViewGroup) inflate.findViewById(16908312);
        ViewGroup viewGroup2 = this.mWidgetFrame;
        if (viewGroup2 != null) {
            int i = this.mWidgetLayoutResId;
            if (i != 0) {
                layoutInflater.inflate(i, viewGroup2);
            } else {
                viewGroup2.setVisibility(8);
            }
            if (isSummaryEmpty() && this.mWidgetFrame.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) this.mWidgetFrame.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.topMargin = 0;
                    layoutParams.gravity = 16;
                    this.mWidgetFrame.setLayoutParams(layoutParams);
                }
            }
        }
        return inflate;
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        TextView textView = (TextView) view.findViewById(16908310);
        int i = 8;
        if (textView != null) {
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                textView.setText(title);
                textView.setVisibility(0);
            } else {
                textView.setVisibility(8);
            }
        }
        TextView textView2 = (TextView) view.findViewById(16908304);
        if (textView2 != null) {
            CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                textView2.setText(summary);
                textView2.setVisibility(0);
            } else {
                textView2.setVisibility(8);
            }
        }
        ImageView imageView = (ImageView) view.findViewById(16908294);
        if (imageView != null) {
            if (!(this.mIconResId == 0 && this.mIcon == null)) {
                if (this.mIcon == null) {
                    this.mIcon = getContext().getDrawable(this.mIconResId);
                }
                Drawable drawable = this.mIcon;
                if (drawable != null) {
                    imageView.setImageDrawable(drawable);
                }
            }
            imageView.setVisibility(this.mIcon != null ? 0 : 8);
        }
        this.mImageFrame = view.findViewById(R$id.icon_frame);
        View view2 = this.mImageFrame;
        if (view2 != null) {
            view2.setVisibility(this.mIcon != null ? 0 : 8);
        }
        doUpdateImageFrameParams();
        View findViewById = view.findViewById(R$id.text_layout);
        if (findViewById != null) {
            View findViewById2 = view.findViewById(R$id.top_space);
            LayoutParams layoutParams = (LayoutParams) findViewById.getLayoutParams();
            if (!(findViewById == null || layoutParams == null)) {
                View view3 = this.mImageFrame;
                if (view3 != null && view3.getVisibility() != 8) {
                    layoutParams.leftMargin = 0;
                    layoutParams.setMarginStart(0);
                } else if (!this.mIsAvatarIcon) {
                    layoutParams.leftMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_left2);
                    layoutParams.setMarginStart(this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_left2));
                } else {
                    layoutParams.leftMargin = 0;
                }
                if (isSummaryEmpty()) {
                    layoutParams.gravity = 16;
                    layoutParams.height = -2;
                    if (findViewById2 != null) {
                        findViewById2.setVisibility(8);
                    }
                    findViewById.setLayoutParams(layoutParams);
                } else if (findViewById2 != null) {
                    findViewById2.setVisibility(0);
                }
            }
        }
        ImageView imageView2 = (ImageView) view.findViewById(R$id.secondary_icon);
        if (imageView2 != null) {
            if (!(this.mSecondaryIconResId == 0 && this.mSecondaryIcon == null)) {
                if (this.mSecondaryIcon == null) {
                    this.mSecondaryIcon = getContext().getDrawable(this.mSecondaryIconResId);
                }
                Drawable drawable2 = this.mSecondaryIcon;
                if (drawable2 != null) {
                    imageView2.setImageDrawable(drawable2);
                }
            }
            if (this.mSecondaryIcon != null) {
                i = 0;
            }
            imageView2.setVisibility(i);
        }
        if (this.mShouldDisableView) {
            setEnabledStateOnViews(view, isEnabled());
        }
    }

    private void setEnabledStateOnViews(View view, boolean z) {
        view.setEnabled(z);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                setEnabledStateOnViews(viewGroup.getChildAt(childCount), z);
            }
        }
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    private void doUpdateImageFrameParams() {
        View view = this.mImageFrame;
        if (view != null && view.getVisibility() != 8) {
            LayoutParams layoutParams = (LayoutParams) this.mImageFrame.getLayoutParams();
            if (layoutParams != null) {
                if (this.mIsAvatarIcon) {
                    layoutParams.width = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_avatar_mini);
                    layoutParams.height = layoutParams.width;
                    layoutParams.setMarginStart(this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_avatar_left2));
                    layoutParams.setMarginEnd(this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_avatar_right3));
                    if (isSummaryEmpty()) {
                        layoutParams.topMargin = 0;
                        layoutParams.gravity = 16;
                        layoutParams.height = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_list_item_height_one_line2);
                    } else {
                        layoutParams.gravity = 8388659;
                        layoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_avatar_top3);
                    }
                } else if (isSummaryEmpty()) {
                    layoutParams.topMargin = 0;
                    layoutParams.gravity = 16;
                } else {
                    layoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_margin_top4);
                }
                this.mImageFrame.setLayoutParams(layoutParams);
            }
        }
    }

    public CharSequence getSummary() {
        return this.mSummary;
    }

    /* access modifiers changed from: protected */
    public void setSummaryOnFromTwoState(CharSequence charSequence) {
        this.mSummaryOn = charSequence;
    }

    /* access modifiers changed from: protected */
    public void setSummaryOffFromTwoState(CharSequence charSequence) {
        this.mSummaryOff = charSequence;
    }

    public boolean isSummaryEmpty() {
        return TextUtils.isEmpty(this.mSummary) && TextUtils.isEmpty(this.mSummaryOn) && TextUtils.isEmpty(this.mSummaryOff);
    }

    public boolean isEnabled() {
        return this.mEnabled && this.mDependencyMet && this.mParentDependencyMet;
    }

    public boolean isSelectable() {
        return this.mSelectable;
    }

    /* access modifiers changed from: 0000 */
    public long getId() {
        return this.mId;
    }

    public boolean hasKey() {
        return !TextUtils.isEmpty(this.mKey);
    }

    public boolean isPersistent() {
        return this.mPersistent;
    }

    /* access modifiers changed from: protected */
    public boolean shouldPersist() {
        return this.mPreferenceManager != null && isPersistent() && hasKey();
    }

    /* access modifiers changed from: protected */
    public boolean callChangeListener(Object obj) {
        OnPreferenceChangeListener onPreferenceChangeListener = this.mOnChangeListener;
        if (onPreferenceChangeListener == null) {
            return true;
        }
        return onPreferenceChangeListener.onPreferenceChange(this, obj);
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (isEnabled()) {
            onClick();
            OnPreferenceClickListener onPreferenceClickListener = this.mOnClickListener;
            if (onPreferenceClickListener == null || !onPreferenceClickListener.onPreferenceClick(this)) {
                PreferenceManager preferenceManager = getPreferenceManager();
                if (preferenceManager == null) {
                    if (this.mIntent != null) {
                        getContext().startActivity(this.mIntent);
                    }
                    return;
                }
                preferenceManager.getOnPreferenceTreeClickListener();
                throw null;
            }
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public int compareTo(Preference preference) {
        int i = this.mOrder;
        int i2 = preference.mOrder;
        if (i != i2) {
            return i - i2;
        }
        CharSequence charSequence = this.mTitle;
        CharSequence charSequence2 = preference.mTitle;
        if (charSequence == charSequence2) {
            return 0;
        }
        if (charSequence == null) {
            return 1;
        }
        if (charSequence2 == null) {
            return -1;
        }
        return CharSequences.compareToIgnoreCase(charSequence, charSequence2);
    }

    /* access modifiers changed from: 0000 */
    public final void setOnPreferenceChangeInternalListener(OnPreferenceChangeInternalListener onPreferenceChangeInternalListener) {
        this.mListener = onPreferenceChangeInternalListener;
    }

    /* access modifiers changed from: protected */
    public void notifyChanged() {
        OnPreferenceChangeInternalListener onPreferenceChangeInternalListener = this.mListener;
        if (onPreferenceChangeInternalListener != null) {
            onPreferenceChangeInternalListener.onPreferenceChange(this);
        }
    }

    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToActivity() {
        registerDependency();
    }

    private void registerDependency() {
        if (!TextUtils.isEmpty(this.mDependencyKey)) {
            Preference findPreferenceInHierarchy = findPreferenceInHierarchy(this.mDependencyKey);
            if (findPreferenceInHierarchy != null) {
                findPreferenceInHierarchy.registerDependent(this);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Dependency \"");
            sb.append(this.mDependencyKey);
            sb.append("\" not found for preference \"");
            sb.append(this.mKey);
            sb.append("\" (title: \"");
            sb.append(this.mTitle);
            sb.append("\"");
            throw new IllegalStateException(sb.toString());
        }
    }

    /* access modifiers changed from: protected */
    public Preference findPreferenceInHierarchy(String str) {
        if (!TextUtils.isEmpty(str)) {
            PreferenceManager preferenceManager = this.mPreferenceManager;
            if (preferenceManager != null) {
                preferenceManager.findPreference(str);
                throw null;
            }
        }
        return null;
    }

    private void registerDependent(Preference preference) {
        if (this.mDependents == null) {
            this.mDependents = new ArrayList();
        }
        this.mDependents.add(preference);
        preference.onDependencyChanged(this, shouldDisableDependents());
    }

    public void notifyDependencyChange(boolean z) {
        List<Preference> list = this.mDependents;
        if (list != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                ((Preference) list.get(i)).onDependencyChanged(this, z);
            }
        }
    }

    public void onDependencyChanged(Preference preference, boolean z) {
        if (this.mDependencyMet == z) {
            this.mDependencyMet = !z;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public void onParentChanged(Preference preference, boolean z) {
        if (this.mParentDependencyMet == z) {
            this.mParentDependencyMet = !z;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    /* access modifiers changed from: protected */
    public String getPersistedString(String str) {
        if (!shouldPersist()) {
            return str;
        }
        this.mPreferenceManager.getSharedPreferences();
        throw null;
    }

    /* access modifiers changed from: protected */
    public boolean persistInt(int i) {
        if (!shouldPersist()) {
            return false;
        }
        if (i == getPersistedInt(~i)) {
            return true;
        }
        this.mPreferenceManager.getEditor();
        throw null;
    }

    /* access modifiers changed from: protected */
    public int getPersistedInt(int i) {
        if (!shouldPersist()) {
            return i;
        }
        this.mPreferenceManager.getSharedPreferences();
        throw null;
    }

    /* access modifiers changed from: protected */
    public boolean persistBoolean(boolean z) {
        if (!shouldPersist()) {
            return false;
        }
        if (z == getPersistedBoolean(!z)) {
            return true;
        }
        this.mPreferenceManager.getEditor();
        throw null;
    }

    /* access modifiers changed from: protected */
    public boolean getPersistedBoolean(boolean z) {
        if (!shouldPersist()) {
            return z;
        }
        this.mPreferenceManager.getSharedPreferences();
        throw null;
    }

    /* access modifiers changed from: 0000 */
    public boolean canRecycleLayout() {
        return this.mCanRecycleLayout;
    }

    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    /* access modifiers changed from: 0000 */
    public StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title);
            sb.append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary);
            sb.append(' ');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }
}
