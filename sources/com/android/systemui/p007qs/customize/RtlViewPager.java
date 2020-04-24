package com.android.systemui.p007qs.customize;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import com.oneplus.support.core.p010os.ParcelableCompat;
import com.oneplus.support.core.p010os.ParcelableCompatCreatorCallbacks;
import java.util.HashMap;

/* renamed from: com.android.systemui.qs.customize.RtlViewPager */
public class RtlViewPager extends ViewPager {
    private int mLayoutDirection = 0;
    private HashMap<OnPageChangeListener, ReversingOnPageChangeListener> mPageChangeListeners = new HashMap<>();

    /* renamed from: com.android.systemui.qs.customize.RtlViewPager$ReversingAdapter */
    private class ReversingAdapter extends DelegatingPagerAdapter {
        public ReversingAdapter(PagerAdapter pagerAdapter) {
            super(pagerAdapter);
        }

        public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            super.destroyItem(viewGroup, i, obj);
        }

        public void destroyItem(View view, int i, Object obj) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            super.destroyItem(view, i, obj);
        }

        public int getItemPosition(Object obj) {
            int itemPosition = super.getItemPosition(obj);
            if (!RtlViewPager.this.isRtl()) {
                return itemPosition;
            }
            if (itemPosition == -1 || itemPosition == -2) {
                return -2;
            }
            return (getCount() - itemPosition) - 1;
        }

        public CharSequence getPageTitle(int i) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            return super.getPageTitle(i);
        }

        public float getPageWidth(int i) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            return super.getPageWidth(i);
        }

        public Object instantiateItem(ViewGroup viewGroup, int i) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            return super.instantiateItem(viewGroup, i);
        }

        public Object instantiateItem(View view, int i) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            return super.instantiateItem(view, i);
        }

        public void setPrimaryItem(View view, int i, Object obj) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            super.setPrimaryItem(view, i, obj);
        }

        public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
            if (RtlViewPager.this.isRtl()) {
                i = (getCount() - i) - 1;
            }
            super.setPrimaryItem(viewGroup, i, obj);
        }
    }

    /* renamed from: com.android.systemui.qs.customize.RtlViewPager$ReversingOnPageChangeListener */
    private class ReversingOnPageChangeListener implements OnPageChangeListener {
        private final OnPageChangeListener mListener;

        public ReversingOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
            this.mListener = onPageChangeListener;
        }

        public void onPageScrolled(int i, float f, int i2) {
            int width = RtlViewPager.this.getWidth();
            PagerAdapter access$401 = RtlViewPager.super.getAdapter();
            if (RtlViewPager.this.isRtl() && access$401 != null) {
                int count = access$401.getCount();
                float f2 = (float) width;
                int pageWidth = ((int) ((1.0f - access$401.getPageWidth(i)) * f2)) + i2;
                while (i < count && pageWidth > 0) {
                    i++;
                    pageWidth -= (int) (access$401.getPageWidth(i) * f2);
                }
                i = (count - i) - 1;
                i2 = -pageWidth;
                f = ((float) i2) / (f2 * access$401.getPageWidth(i));
            }
            this.mListener.onPageScrolled(i, f, i2);
        }

        public void onPageSelected(int i) {
            PagerAdapter access$601 = RtlViewPager.super.getAdapter();
            if (RtlViewPager.this.isRtl() && access$601 != null) {
                i = (access$601.getCount() - i) - 1;
            }
            this.mListener.onPageSelected(i);
        }

        public void onPageScrollStateChanged(int i) {
            this.mListener.onPageScrollStateChanged(i);
        }
    }

    /* renamed from: com.android.systemui.qs.customize.RtlViewPager$SavedState */
    public static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            public SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        });
        /* access modifiers changed from: private */
        public final int mLayoutDirection;
        /* access modifiers changed from: private */
        public final Parcelable mViewPagerSavedState;

        public int describeContents() {
            return 0;
        }

        private SavedState(Parcelable parcelable, int i) {
            this.mViewPagerSavedState = parcelable;
            this.mLayoutDirection = i;
        }

        private SavedState(Parcel parcel, ClassLoader classLoader) {
            if (classLoader == null) {
                classLoader = SavedState.class.getClassLoader();
            }
            this.mViewPagerSavedState = parcel.readParcelable(classLoader);
            this.mLayoutDirection = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.mViewPagerSavedState, i);
            parcel.writeInt(this.mLayoutDirection);
        }
    }

    public RtlViewPager(Context context) {
        super(context);
    }

    public RtlViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        int i2 = 0;
        int i3 = 1;
        if (i != 1) {
            i3 = 0;
        }
        if (i3 != this.mLayoutDirection) {
            PagerAdapter adapter = super.getAdapter();
            if (adapter != null) {
                i2 = getCurrentItem();
            }
            this.mLayoutDirection = i3;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                setCurrentItem(i2);
            }
        }
    }

    public void setAdapter(PagerAdapter pagerAdapter) {
        if (pagerAdapter != null) {
            pagerAdapter = new ReversingAdapter(pagerAdapter);
        }
        super.setAdapter(pagerAdapter);
        setCurrentItem(0);
    }

    public PagerAdapter getAdapter() {
        ReversingAdapter reversingAdapter = (ReversingAdapter) super.getAdapter();
        if (reversingAdapter == null) {
            return null;
        }
        return reversingAdapter.getDelegate();
    }

    /* access modifiers changed from: private */
    public boolean isRtl() {
        return this.mLayoutDirection == 1;
    }

    public int getCurrentItem() {
        int currentItem = super.getCurrentItem();
        PagerAdapter adapter = super.getAdapter();
        return (adapter == null || !isRtl()) ? currentItem : (adapter.getCount() - currentItem) - 1;
    }

    public void setCurrentItem(int i, boolean z) {
        PagerAdapter adapter = super.getAdapter();
        if (adapter != null && isRtl()) {
            i = (adapter.getCount() - i) - 1;
        }
        super.setCurrentItem(i, z);
    }

    public void setCurrentItem(int i) {
        PagerAdapter adapter = super.getAdapter();
        if (adapter != null && isRtl()) {
            i = (adapter.getCount() - i) - 1;
        }
        super.setCurrentItem(i);
    }

    public Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), this.mLayoutDirection);
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        this.mLayoutDirection = savedState.mLayoutDirection;
        super.onRestoreInstanceState(savedState.mViewPagerSavedState);
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        super.setOnPageChangeListener(new ReversingOnPageChangeListener(onPageChangeListener));
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        if (MeasureSpec.getMode(i2) == 0) {
            int i3 = 0;
            for (int i4 = 0; i4 < getChildCount(); i4++) {
                View childAt = getChildAt(i4);
                childAt.measure(i, MeasureSpec.makeMeasureSpec(0, 0));
                int measuredHeight = childAt.getMeasuredHeight();
                if (measuredHeight > i3) {
                    i3 = measuredHeight;
                }
            }
            i2 = MeasureSpec.makeMeasureSpec(i3, 1073741824);
        }
        super.onMeasure(i, i2);
    }
}
