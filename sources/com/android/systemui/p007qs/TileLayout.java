package com.android.systemui.p007qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.systemui.R$dimen;
import com.android.systemui.R$integer;
import com.android.systemui.p007qs.QSPanel.QSTileLayout;
import com.android.systemui.p007qs.QSPanel.TileRecord;
import com.android.systemui.plugins.p006qs.QSTileView;
import java.util.ArrayList;
import java.util.Iterator;

/* renamed from: com.android.systemui.qs.TileLayout */
public class TileLayout extends ViewGroup implements QSTileLayout {
    protected int mCellHeight;
    protected int mCellMarginHorizontal;
    protected int mCellMarginTop;
    protected int mCellMarginVertical;
    protected int mCellWidth;
    protected int mColumns;
    private boolean mListening;
    protected int mMaxAllowedRows;
    protected final ArrayList<TileRecord> mRecords;
    protected int mRows;
    protected int mSidePadding;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRows = 1;
        this.mRecords = new ArrayList<>();
        this.mMaxAllowedRows = 3;
        setFocusableInTouchMode(true);
        updateResources();
    }

    public int getOffsetTop(TileRecord tileRecord) {
        return getTop();
    }

    public void setListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            Iterator it = this.mRecords.iterator();
            while (it.hasNext()) {
                ((TileRecord) it.next()).tile.setListening(this, this.mListening);
            }
        }
    }

    public void addTile(TileRecord tileRecord) {
        this.mRecords.add(tileRecord);
        tileRecord.tile.setListening(this, this.mListening);
        addTileView(tileRecord);
    }

    /* access modifiers changed from: protected */
    public void addTileView(TileRecord tileRecord) {
        addView(tileRecord.tileView);
    }

    public void removeTile(TileRecord tileRecord) {
        this.mRecords.remove(tileRecord);
        tileRecord.tile.setListening(this, false);
        removeView(tileRecord.tileView);
    }

    public void removeAllViews() {
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            ((TileRecord) it.next()).tile.setListening(this, false);
        }
        this.mRecords.clear();
        super.removeAllViews();
    }

    public boolean updateResources() {
        Resources resources = this.mContext.getResources();
        int max = Math.max(1, resources.getInteger(R$integer.quick_settings_num_columns));
        this.mCellHeight = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_tile_height);
        this.mCellMarginHorizontal = resources.getDimensionPixelSize(R$dimen.qs_tile_margin_horizontal);
        this.mCellMarginVertical = resources.getDimensionPixelSize(R$dimen.qs_tile_margin_vertical);
        this.mCellMarginTop = resources.getDimensionPixelSize(R$dimen.qs_tile_margin_top);
        this.mSidePadding = resources.getDimensionPixelOffset(R$dimen.qs_tile_layout_margin_side);
        this.mMaxAllowedRows = Math.max(1, getResources().getInteger(R$integer.quick_settings_max_rows));
        if (this.mColumns == max) {
            return false;
        }
        this.mColumns = max;
        requestLayout();
        return true;
    }

    /* JADX WARNING: type inference failed for: r0v4, types: [android.view.View] */
    /* JADX WARNING: type inference failed for: r0v9 */
    /* JADX WARNING: type inference failed for: r0v10, types: [android.view.View] */
    /* JADX WARNING: type inference failed for: r0v14 */
    /* JADX WARNING: type inference failed for: r0v15 */
    /* JADX WARNING: type inference failed for: r0v16 */
    /* access modifiers changed from: protected */
    /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r0v9
      assigns: []
      uses: []
      mth insns count: 58
    	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
    	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
    	at jadx.core.ProcessClass.process(ProcessClass.java:30)
    	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:49)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:49)
    	at jadx.core.ProcessClass.process(ProcessClass.java:35)
    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
     */
    /* JADX WARNING: Unknown variable types count: 3 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onMeasure(int r6, int r7) {
        /*
            r5 = this;
            java.util.ArrayList<com.android.systemui.qs.QSPanel$TileRecord> r0 = r5.mRecords
            int r0 = r0.size()
            int r6 = android.view.View.MeasureSpec.getSize(r6)
            int r1 = r5.getPaddingStart()
            int r1 = r6 - r1
            int r2 = r5.getPaddingEnd()
            int r1 = r1 - r2
            int r7 = android.view.View.MeasureSpec.getMode(r7)
            if (r7 != 0) goto L_0x0023
            int r7 = r5.mColumns
            int r0 = r0 + r7
            int r0 = r0 + -1
            int r0 = r0 / r7
            r5.mRows = r0
        L_0x0023:
            int r7 = r5.mSidePadding
            int r7 = r7 * 2
            int r1 = r1 - r7
            int r7 = r5.mCellMarginHorizontal
            int r0 = r5.mColumns
            int r7 = r7 * r0
            int r1 = r1 - r7
            int r1 = r1 / r0
            r5.mCellWidth = r1
            java.util.ArrayList<com.android.systemui.qs.QSPanel$TileRecord> r7 = r5.mRecords
            java.util.Iterator r7 = r7.iterator()
            r0 = r5
        L_0x0038:
            boolean r1 = r7.hasNext()
            if (r1 == 0) goto L_0x0067
            java.lang.Object r1 = r7.next()
            com.android.systemui.qs.QSPanel$TileRecord r1 = (com.android.systemui.p007qs.QSPanel.TileRecord) r1
            com.android.systemui.plugins.qs.QSTileView r2 = r1.tileView
            int r2 = r2.getVisibility()
            r3 = 8
            if (r2 != r3) goto L_0x004f
            goto L_0x0038
        L_0x004f:
            com.android.systemui.plugins.qs.QSTileView r2 = r1.tileView
            int r3 = r5.mCellWidth
            int r3 = exactly(r3)
            int r4 = r5.mCellHeight
            int r4 = exactly(r4)
            r2.measure(r3, r4)
            com.android.systemui.plugins.qs.QSTileView r1 = r1.tileView
            android.view.View r0 = r1.updateAccessibilityOrder(r0)
            goto L_0x0038
        L_0x0067:
            int r7 = r5.mCellHeight
            int r0 = r5.mCellMarginVertical
            int r7 = r7 + r0
            int r1 = r5.mRows
            int r7 = r7 * r1
            r2 = 0
            if (r1 == 0) goto L_0x0077
            int r1 = r5.mCellMarginTop
            int r0 = r1 - r0
            goto L_0x0078
        L_0x0077:
            r0 = r2
        L_0x0078:
            int r7 = r7 + r0
            if (r7 >= 0) goto L_0x007c
            r7 = r2
        L_0x007c:
            r5.setMeasuredDimension(r6, r7)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.TileLayout.onMeasure(int, int):void");
    }

    public boolean updateMaxRows(int i, int i2) {
        int size = MeasureSpec.getSize(i) - this.mCellMarginTop;
        int i3 = this.mCellMarginVertical;
        int i4 = size + i3;
        int i5 = this.mRows;
        this.mRows = i4 / (this.mCellHeight + i3);
        int i6 = this.mRows;
        int i7 = this.mMaxAllowedRows;
        if (i6 >= i7) {
            this.mRows = i7;
        } else if (i6 <= 1) {
            this.mRows = 1;
        }
        int i8 = this.mRows;
        int i9 = this.mColumns;
        if (i8 > ((i2 + i9) - 1) / i9) {
            this.mRows = ((i2 + i9) - 1) / i9;
        }
        if (i5 != this.mRows) {
            return true;
        }
        return false;
    }

    protected static int exactly(int i) {
        return MeasureSpec.makeMeasureSpec(i, 1073741824);
    }

    /* access modifiers changed from: protected */
    public void layoutTileRecords(int i) {
        boolean z = getLayoutDirection() == 1;
        int min = Math.min(i, this.mRows * this.mColumns);
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i2 < min) {
            if (i3 == this.mColumns) {
                i4++;
                i3 = 0;
            }
            TileRecord tileRecord = (TileRecord) this.mRecords.get(i2);
            int rowTop = getRowTop(i4);
            int columnStart = getColumnStart(z ? (this.mColumns - i3) - 1 : i3);
            int i5 = this.mCellWidth + columnStart;
            QSTileView qSTileView = tileRecord.tileView;
            qSTileView.layout(columnStart, rowTop, i5, qSTileView.getMeasuredHeight() + rowTop);
            i2++;
            i3++;
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layoutTileRecords(this.mRecords.size());
    }

    private int getRowTop(int i) {
        return (i * (this.mCellHeight + this.mCellMarginVertical)) + this.mCellMarginTop;
    }

    /* access modifiers changed from: protected */
    public int getColumnStart(int i) {
        int paddingStart = getPaddingStart() + this.mSidePadding;
        int i2 = this.mCellMarginHorizontal;
        return paddingStart + (i2 / 2) + (i * (this.mCellWidth + i2));
    }

    public int getNumVisibleTiles() {
        return this.mRecords.size();
    }
}
