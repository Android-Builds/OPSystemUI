package com.android.systemui.p007qs.customize;

import android.content.Context;
import android.util.AttributeSet;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import com.android.systemui.p007qs.PageIndicator;

/* renamed from: com.android.systemui.qs.customize.QSEditViewPager */
public class QSEditViewPager extends RtlViewPager {
    /* access modifiers changed from: private */
    public PageIndicator mPageIndicator;

    public QSEditViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int i) {
            }

            public void onPageSelected(int i) {
            }

            public void onPageScrolled(int i, float f, int i2) {
                if (QSEditViewPager.this.mPageIndicator != null) {
                    QSEditViewPager.this.mPageIndicator.setLocation(((float) i) + f);
                }
            }
        });
    }

    public void setPageIndicator(PageIndicator pageIndicator) {
        this.mPageIndicator = pageIndicator;
    }

    public void updateIndicator() {
        int count = getAdapter().getCount();
        this.mPageIndicator.setNumPages(count);
        this.mPageIndicator.setVisibility(count > 1 ? 0 : 4);
    }
}
