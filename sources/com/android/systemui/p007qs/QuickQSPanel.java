package com.android.systemui.p007qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.R$dimen;
import com.android.systemui.R$integer;
import com.android.systemui.p007qs.QSPanel.TileRecord;
import com.android.systemui.p007qs.customize.QSCustomizer;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTile.SignalState;
import com.android.systemui.plugins.p006qs.QSTile.State;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/* renamed from: com.android.systemui.qs.QuickQSPanel */
public class QuickQSPanel extends QSPanel {
    private static int mDefaultMaxTiles;
    private boolean mDisabledByPolicy;
    protected QSPanel mFullPanel;
    private int mMaxTiles;
    private final Tunable mNumTiles = new Tunable() {
        public void onTuningChanged(String str, String str2) {
            QuickQSPanel quickQSPanel = QuickQSPanel.this;
            quickQSPanel.setMaxTiles(QuickQSPanel.getNumQuickTiles(quickQSPanel.mContext));
        }
    };

    /* renamed from: com.android.systemui.qs.QuickQSPanel$HeaderTileLayout */
    private static class HeaderTileLayout extends TileLayout {
        private Rect mClippingBounds = new Rect();
        private int mLandscapeMargin = 0;

        public HeaderTileLayout(Context context) {
            super(context);
            setClipChildren(false);
            setClipToPadding(false);
            LayoutParams layoutParams = new LayoutParams(-1, -1);
            layoutParams.gravity = 1;
            setLayoutParams(layoutParams);
        }

        /* access modifiers changed from: protected */
        public void onConfigurationChanged(Configuration configuration) {
            super.onConfigurationChanged(configuration);
            updateResources();
            if (configuration.orientation == 2) {
                this.mLandscapeMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.op_quick_qs_panel_landscape_side_margin);
            } else {
                this.mLandscapeMargin = 0;
            }
        }

        public void onFinishInflate() {
            updateResources();
        }

        private ViewGroup.LayoutParams generateTileLayoutParams() {
            return new ViewGroup.LayoutParams(this.mCellWidth, this.mCellHeight);
        }

        /* access modifiers changed from: protected */
        public void addTileView(TileRecord tileRecord) {
            addView(tileRecord.tileView, getChildCount(), generateTileLayoutParams());
        }

        /* access modifiers changed from: protected */
        public void onLayout(boolean z, int i, int i2, int i3, int i4) {
            this.mClippingBounds.set(0, 0, i3 - i, 10000);
            setClipBounds(this.mClippingBounds);
            calculateColumns();
            int i5 = 0;
            while (i5 < this.mRecords.size()) {
                ((TileRecord) this.mRecords.get(i5)).tileView.setVisibility(i5 < this.mColumns ? 0 : 8);
                i5++;
            }
            setAccessibilityOrder();
            layoutTileRecords(this.mColumns);
        }

        public boolean updateResources() {
            this.mCellMarginTop = this.mContext.getResources().getDimensionPixelSize(R$dimen.op_quick_qs_tile_margin_top);
            this.mCellWidth = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_quick_tile_size);
            this.mCellHeight = this.mCellWidth + this.mCellMarginTop;
            return false;
        }

        private boolean calculateColumns() {
            int i;
            int i2 = this.mColumns;
            int size = this.mRecords.size();
            boolean z = false;
            if (size == 0) {
                this.mColumns = 0;
                return true;
            }
            int measuredWidth = ((getMeasuredWidth() - getPaddingStart()) - getPaddingEnd()) - (this.mLandscapeMargin * 2);
            int max = (measuredWidth - (this.mCellWidth * size)) / Math.max(1, size - 1);
            if (max > 0) {
                this.mCellMarginHorizontal = max;
                this.mColumns = size;
            } else {
                int i3 = this.mCellWidth;
                if (i3 == 0) {
                    i = 1;
                } else {
                    i = Math.min(size, measuredWidth / i3);
                }
                this.mColumns = i;
                int i4 = this.mColumns;
                this.mCellMarginHorizontal = (measuredWidth - (this.mCellWidth * i4)) / (i4 - 1);
            }
            if (this.mColumns != i2) {
                z = true;
            }
            return z;
        }

        /* JADX WARNING: type inference failed for: r1v1, types: [android.view.View] */
        /* JADX WARNING: type inference failed for: r1v2 */
        /* JADX WARNING: type inference failed for: r1v3, types: [android.view.View] */
        /* JADX WARNING: type inference failed for: r1v4 */
        /* JADX WARNING: type inference failed for: r1v5 */
        /* JADX WARNING: type inference failed for: r1v6 */
        /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r1v2
          assigns: []
          uses: []
          mth insns count: 30
        	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
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
        private void setAccessibilityOrder() {
            /*
                r5 = this;
                java.util.ArrayList<com.android.systemui.qs.QSPanel$TileRecord> r0 = r5.mRecords
                if (r0 == 0) goto L_0x0044
                int r0 = r0.size()
                if (r0 <= 0) goto L_0x0044
                java.util.ArrayList<com.android.systemui.qs.QSPanel$TileRecord> r0 = r5.mRecords
                java.util.Iterator r0 = r0.iterator()
                r1 = r5
            L_0x0011:
                boolean r2 = r0.hasNext()
                if (r2 == 0) goto L_0x002f
                java.lang.Object r2 = r0.next()
                com.android.systemui.qs.QSPanel$TileRecord r2 = (com.android.systemui.p007qs.QSPanel.TileRecord) r2
                com.android.systemui.plugins.qs.QSTileView r3 = r2.tileView
                int r3 = r3.getVisibility()
                r4 = 8
                if (r3 != r4) goto L_0x0028
                goto L_0x0011
            L_0x0028:
                com.android.systemui.plugins.qs.QSTileView r2 = r2.tileView
                android.view.View r1 = r2.updateAccessibilityOrder(r1)
                goto L_0x0011
            L_0x002f:
                java.util.ArrayList<com.android.systemui.qs.QSPanel$TileRecord> r5 = r5.mRecords
                int r0 = r5.size()
                int r0 = r0 + -1
                java.lang.Object r5 = r5.get(r0)
                com.android.systemui.qs.QSPanel$TileRecord r5 = (com.android.systemui.p007qs.QSPanel.TileRecord) r5
                com.android.systemui.plugins.qs.QSTileView r5 = r5.tileView
                int r0 = com.android.systemui.R$id.expand_indicator
                r5.setAccessibilityTraversalBefore(r0)
            L_0x0044:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.p007qs.QuickQSPanel.HeaderTileLayout.setAccessibilityOrder():void");
        }

        /* access modifiers changed from: protected */
        public void onMeasure(int i, int i2) {
            Iterator it = this.mRecords.iterator();
            while (it.hasNext()) {
                TileRecord tileRecord = (TileRecord) it.next();
                if (tileRecord.tileView.getVisibility() != 8) {
                    tileRecord.tileView.measure(TileLayout.exactly(this.mCellWidth), TileLayout.exactly(this.mCellHeight));
                }
            }
            int i3 = this.mCellHeight;
            if (i3 < 0) {
                i3 = 0;
            }
            setMeasuredDimension(MeasureSpec.getSize(i), i3);
        }

        public int getNumVisibleTiles() {
            return this.mColumns;
        }

        /* access modifiers changed from: protected */
        public int getColumnStart(int i) {
            return this.mLandscapeMargin + getPaddingStart() + (i * (this.mCellWidth + this.mCellMarginHorizontal));
        }
    }

    public void setPadding(int i, int i2, int i3, int i4) {
    }

    public QuickQSPanel(Context context, AttributeSet attributeSet, DumpController dumpController) {
        super(context, attributeSet, dumpController);
        QSSecurityFooter qSSecurityFooter = this.mFooter;
        if (qSSecurityFooter != null) {
            removeView(qSSecurityFooter.getView());
        }
        if (this.mTileLayout != null) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                this.mTileLayout.removeTile((TileRecord) this.mRecords.get(i));
            }
            removeView((View) this.mTileLayout);
        }
        mDefaultMaxTiles = getResources().getInteger(R$integer.quick_qs_panel_max_columns);
        this.mTileLayout = new HeaderTileLayout(context);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout, 0);
        super.setPadding(0, 0, 0, 0);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this.mNumTiles, "sysui_qqs_count");
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this.mNumTiles);
    }

    public void setQSPanelAndHeader(QSPanel qSPanel, View view) {
        this.mFullPanel = qSPanel;
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowDetail() {
        return !this.mExpanded;
    }

    /* access modifiers changed from: protected */
    public void drawTile(TileRecord tileRecord, State state) {
        if (state instanceof SignalState) {
            SignalState signalState = new SignalState();
            state.copyTo(signalState);
            signalState.activityIn = false;
            signalState.activityOut = false;
            state = signalState;
        }
        super.drawTile(tileRecord, state);
    }

    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        super.setHost(qSTileHost, qSCustomizer);
        setTiles(this.mHost.getTiles());
    }

    public void setMaxTiles(int i) {
        this.mMaxTiles = i;
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            setTiles(qSTileHost.getTiles());
        }
    }

    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            super.onTuningChanged(str, "0");
        }
    }

    public void setTiles(Collection<QSTile> collection) {
        ArrayList arrayList = new ArrayList();
        for (QSTile add : collection) {
            arrayList.add(add);
            if (arrayList.size() == this.mMaxTiles) {
                break;
            }
        }
        super.setTiles(arrayList, true);
    }

    public static int getNumQuickTiles(Context context) {
        return ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qqs_count", mDefaultMaxTiles);
    }

    /* access modifiers changed from: 0000 */
    public void setDisabledByPolicy(boolean z) {
        if (z != this.mDisabledByPolicy) {
            this.mDisabledByPolicy = z;
            setVisibility(z ? 8 : 0);
        }
    }

    public void setVisibility(int i) {
        if (this.mDisabledByPolicy) {
            if (getVisibility() != 8) {
                i = 8;
            } else {
                return;
            }
        }
        super.setVisibility(i);
    }
}
