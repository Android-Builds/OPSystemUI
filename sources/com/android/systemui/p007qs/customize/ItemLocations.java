package com.android.systemui.p007qs.customize;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

/* renamed from: com.android.systemui.qs.customize.ItemLocations */
public class ItemLocations {
    private static int FLIP_PAGE_WIDTH = 80;
    private static int SCREEN_WIDTH = 1080;
    private int mColumns;
    private boolean mIsLayoutRTL = false;
    private int mItemHeight;
    private Rect[] mItemLocations;
    private int mItemWidth;
    private int mItems = 0;
    private int mMargin;
    private Point mParentLocation;
    private int mRows;

    public ItemLocations(int i, int i2, int i3, int i4, int i5) {
        SCREEN_WIDTH = Resources.getSystem().getDisplayMetrics().widthPixels;
        StringBuilder sb = new StringBuilder();
        sb.append("SCREEN_WIDTH=");
        sb.append(SCREEN_WIDTH);
        Log.d("ItemLocations", sb.toString());
        this.mParentLocation = new Point(0, 0);
        this.mItemWidth = i;
        this.mItemHeight = i2;
        this.mMargin = i3;
        this.mColumns = i4;
        this.mRows = i5;
        this.mItems = i4 * i5;
        this.mItemLocations = new Rect[this.mItems];
        initLocationItems();
    }

    public void setParentLocation(int i, int i2, int i3) {
        this.mParentLocation.set(i, i2);
        this.mItemWidth = i3 / this.mColumns;
        initLocationItems();
    }

    public int getMaxItems() {
        return this.mItems;
    }

    public int getColumns() {
        return this.mColumns;
    }

    private void initLocationItems() {
        int i = this.mItemHeight;
        int i2 = this.mMargin;
        int i3 = i + (i2 * 2);
        int i4 = this.mItemWidth + (i2 * 2);
        StringBuilder sb = new StringBuilder();
        sb.append("mItemWidth=");
        sb.append(this.mItemWidth);
        sb.append(", mItemHeight=");
        sb.append(this.mItemHeight);
        String str = "ItemLocations";
        Log.d(str, sb.toString());
        Point point = this.mParentLocation;
        int i5 = point.x;
        int i6 = 0;
        int i7 = point.y;
        int i8 = i3;
        int i9 = 0;
        while (true) {
            int i10 = this.mRows;
            if (i9 < i10) {
                if (i9 == i10 - 1) {
                    i8 += 10;
                }
                int i11 = i7 + 0;
                int i12 = i6;
                int i13 = i5;
                for (int i14 = 0; i14 < this.mColumns; i14++) {
                    int i15 = i13 + 0;
                    int i16 = i15 + i4;
                    this.mItemLocations[i12] = new Rect(i15, i11, i16, i11 + i8);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Location=");
                    sb2.append(this.mItemLocations[i12]);
                    Log.d(str, sb2.toString());
                    i12++;
                    i13 = i16 + 0;
                }
                i5 = this.mParentLocation.x;
                i7 = i11 + i8 + 0;
                i9++;
                i6 = i12;
            } else {
                return;
            }
        }
    }

    public int getPositionIndex(int i, int i2) {
        int i3 = 0;
        while (i3 < this.mItems) {
            if (!this.mItemLocations[i3].contains(i, i2)) {
                i3++;
            } else if (!isLayoutRTL()) {
                return i3;
            } else {
                int i4 = this.mColumns;
                return ((((i3 / i4) * i4) + i4) - (i3 % i4)) - 1;
            }
        }
        return -1;
    }

    public void setLayoutRTL(boolean z) {
        this.mIsLayoutRTL = z;
    }

    public boolean isLayoutRTL() {
        return this.mIsLayoutRTL;
    }

    public boolean isGoingToNextPage(int i) {
        boolean z = true;
        if (isLayoutRTL()) {
            if (i >= FLIP_PAGE_WIDTH) {
                z = false;
            }
            return z;
        }
        if (i <= SCREEN_WIDTH - FLIP_PAGE_WIDTH) {
            z = false;
        }
        return z;
    }

    public boolean isGoingToPrevPage(int i) {
        boolean z = true;
        if (isLayoutRTL()) {
            if (i <= SCREEN_WIDTH - FLIP_PAGE_WIDTH) {
                z = false;
            }
            return z;
        }
        if (i >= FLIP_PAGE_WIDTH) {
            z = false;
        }
        return z;
    }
}
