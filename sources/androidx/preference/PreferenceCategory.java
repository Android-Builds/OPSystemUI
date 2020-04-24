package androidx.preference;

import android.content.Context;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;

public class PreferenceCategory extends PreferenceGroup {
    public boolean isEnabled() {
        return false;
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, R$attr.preferenceCategoryStyle, 16842892));
    }

    public PreferenceCategory(Context context) {
        this(context, null);
    }

    public boolean shouldDisableDependents() {
        return !super.isEnabled();
    }

    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        int i = VERSION.SDK_INT;
        if (i >= 28) {
            preferenceViewHolder.itemView.setAccessibilityHeading(true);
        } else if (i < 21) {
            TypedValue typedValue = new TypedValue();
            if (getContext().getTheme().resolveAttribute(R$attr.colorAccent, typedValue, true)) {
                TextView textView = (TextView) preferenceViewHolder.findViewById(16908310);
                if (textView != null && textView.getCurrentTextColor() == ContextCompat.getColor(getContext(), R$color.preference_fallback_accent_color)) {
                    textView.setTextColor(typedValue.data);
                }
            }
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfoCompat);
        if (VERSION.SDK_INT < 28) {
            CollectionItemInfoCompat collectionItemInfo = accessibilityNodeInfoCompat.getCollectionItemInfo();
            if (collectionItemInfo != null) {
                accessibilityNodeInfoCompat.setCollectionItemInfo(CollectionItemInfoCompat.obtain(collectionItemInfo.getRowIndex(), collectionItemInfo.getRowSpan(), collectionItemInfo.getColumnIndex(), collectionItemInfo.getColumnSpan(), true, collectionItemInfo.isSelected()));
            }
        }
    }
}
