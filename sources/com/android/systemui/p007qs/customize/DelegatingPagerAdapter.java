package com.android.systemui.p007qs.customize;

import android.database.DataSetObserver;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager.widget.PagerAdapter;

/* renamed from: com.android.systemui.qs.customize.DelegatingPagerAdapter */
public class DelegatingPagerAdapter extends PagerAdapter {
    private final PagerAdapter mDelegate;

    /* renamed from: com.android.systemui.qs.customize.DelegatingPagerAdapter$MyDataSetObserver */
    private static class MyDataSetObserver extends DataSetObserver {
        final DelegatingPagerAdapter mParent;

        private MyDataSetObserver(DelegatingPagerAdapter delegatingPagerAdapter) {
            this.mParent = delegatingPagerAdapter;
        }

        public void onChanged() {
            DelegatingPagerAdapter delegatingPagerAdapter = this.mParent;
            if (delegatingPagerAdapter != null) {
                delegatingPagerAdapter.superNotifyDataSetChanged();
            }
        }

        public void onInvalidated() {
            onChanged();
        }
    }

    public DelegatingPagerAdapter(PagerAdapter pagerAdapter) {
        this.mDelegate = pagerAdapter;
        pagerAdapter.registerDataSetObserver(new MyDataSetObserver());
    }

    public PagerAdapter getDelegate() {
        return this.mDelegate;
    }

    public int getCount() {
        return this.mDelegate.getCount();
    }

    public void startUpdate(ViewGroup viewGroup) {
        this.mDelegate.startUpdate(viewGroup);
    }

    public Object instantiateItem(ViewGroup viewGroup, int i) {
        return this.mDelegate.instantiateItem(viewGroup, i);
    }

    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        this.mDelegate.destroyItem(viewGroup, i, obj);
    }

    public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
        this.mDelegate.setPrimaryItem(viewGroup, i, obj);
    }

    public void finishUpdate(ViewGroup viewGroup) {
        this.mDelegate.finishUpdate(viewGroup);
    }

    @Deprecated
    public void startUpdate(View view) {
        this.mDelegate.startUpdate(view);
    }

    @Deprecated
    public Object instantiateItem(View view, int i) {
        return this.mDelegate.instantiateItem(view, i);
    }

    @Deprecated
    public void destroyItem(View view, int i, Object obj) {
        this.mDelegate.destroyItem(view, i, obj);
    }

    @Deprecated
    public void setPrimaryItem(View view, int i, Object obj) {
        this.mDelegate.setPrimaryItem(view, i, obj);
    }

    @Deprecated
    public void finishUpdate(View view) {
        this.mDelegate.finishUpdate(view);
    }

    public boolean isViewFromObject(View view, Object obj) {
        return this.mDelegate.isViewFromObject(view, obj);
    }

    public Parcelable saveState() {
        return this.mDelegate.saveState();
    }

    public void restoreState(Parcelable parcelable, ClassLoader classLoader) {
        this.mDelegate.restoreState(parcelable, classLoader);
    }

    public int getItemPosition(Object obj) {
        return this.mDelegate.getItemPosition(obj);
    }

    public void notifyDataSetChanged() {
        this.mDelegate.notifyDataSetChanged();
    }

    /* access modifiers changed from: 0000 */
    public void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDelegate.registerDataSetObserver(dataSetObserver);
    }

    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDelegate.unregisterDataSetObserver(dataSetObserver);
    }

    public CharSequence getPageTitle(int i) {
        return this.mDelegate.getPageTitle(i);
    }

    public float getPageWidth(int i) {
        return this.mDelegate.getPageWidth(i);
    }
}
