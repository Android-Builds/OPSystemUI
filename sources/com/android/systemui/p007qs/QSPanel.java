package com.android.systemui.p007qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.airbnb.lottie.C0526R;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.DumpController;
import com.android.systemui.Dumpable;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.p007qs.PagedTileLayout.PageListener;
import com.android.systemui.p007qs.QSHost.Callback;
import com.android.systemui.p007qs.customize.QSCustomizer;
import com.android.systemui.p007qs.external.CustomTile;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTile.State;
import com.android.systemui.plugins.p006qs.QSTileView;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSeekBar;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.settings.ToggleSliderView;
import com.android.systemui.statusbar.phone.WLBSwitchController;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.BrightnessMirrorController.BrightnessMirrorListener;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/* renamed from: com.android.systemui.qs.QSPanel */
public class QSPanel extends LinearLayout implements Tunable, Callback, BrightnessMirrorListener, Dumpable {
    private BrightnessController mBrightnessController;
    private View mBrightnessMirror;
    private BrightnessMirrorController mBrightnessMirrorController;
    protected final View mBrightnessView;
    private QSDetail.Callback mCallback;
    protected final Context mContext;
    /* access modifiers changed from: private */
    public QSCustomizer mCustomizePanel;
    /* access modifiers changed from: private */
    public Record mDetailRecord;
    private View mDivider;
    private DumpController mDumpController;
    protected boolean mExpanded;
    protected QSSecurityFooter mFooter;
    private PageIndicator mFooterPageIndicator;
    private boolean mGridContentVisible;
    /* access modifiers changed from: private */
    public final C0994H mHandler;
    protected QSTileHost mHost;
    protected boolean mListening;
    private final MetricsLogger mMetricsLogger;
    private PageIndicator mPanelPageIndicator;
    private final QSTileRevealController mQsTileRevealController;
    protected final ArrayList<TileRecord> mRecords;
    protected QSTileLayout mTileLayout;

    /* renamed from: com.android.systemui.qs.QSPanel$H */
    private class C0994H extends Handler {
        private C0994H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            boolean z = true;
            if (i == 1) {
                QSPanel qSPanel = QSPanel.this;
                Record record = (Record) message.obj;
                if (message.arg1 == 0) {
                    z = false;
                }
                qSPanel.handleShowDetail(record, z);
            } else if (i == 3) {
                QSPanel.this.announceForAccessibility((CharSequence) message.obj);
            }
        }
    }

    /* renamed from: com.android.systemui.qs.QSPanel$QSTileLayout */
    public interface QSTileLayout {
        void addTile(TileRecord tileRecord);

        int getNumVisibleTiles();

        int getOffsetTop(TileRecord tileRecord);

        void removeTile(TileRecord tileRecord);

        void restoreInstanceState(Bundle bundle) {
        }

        void saveInstanceState(Bundle bundle) {
        }

        void setExpansion(float f) {
        }

        void setListening(boolean z);

        boolean updateResources();
    }

    /* renamed from: com.android.systemui.qs.QSPanel$Record */
    protected static class Record {
        DetailAdapter detailAdapter;

        /* renamed from: x */
        int f87x;

        /* renamed from: y */
        int f88y;

        protected Record() {
        }
    }

    /* renamed from: com.android.systemui.qs.QSPanel$TileRecord */
    public static final class TileRecord extends Record {
        public QSTile.Callback callback;
        public boolean scanState;
        public QSTile tile;
        public QSTileView tileView;
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, null);
    }

    public QSPanel(Context context, AttributeSet attributeSet, DumpController dumpController) {
        super(context, attributeSet);
        this.mRecords = new ArrayList<>();
        this.mHandler = new C0994H();
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mGridContentVisible = true;
        this.mContext = context;
        setOrientation(1);
        this.mTileLayout = (QSTileLayout) LayoutInflater.from(this.mContext).inflate(R$layout.qs_paged_tile_layout, this, false);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout);
        this.mPanelPageIndicator = (PageIndicator) LayoutInflater.from(context).inflate(R$layout.qs_page_indicator, this, false);
        addView(this.mPanelPageIndicator);
        ((PagedTileLayout) this.mTileLayout).setPageIndicator(this.mPanelPageIndicator);
        this.mQsTileRevealController = new QSTileRevealController(this.mContext, this, (PagedTileLayout) this.mTileLayout);
        this.mBrightnessView = LayoutInflater.from(this.mContext).inflate(R$layout.quick_settings_brightness_dialog, this, false);
        addView(this.mBrightnessView);
        this.mFooter = new QSSecurityFooter(this, context);
        addView(this.mFooter.getView());
        ((ToggleSeekBar) this.mBrightnessView.findViewById(R$id.slider)).setProgressBackgroundTintMode(Mode.SRC);
        this.mBrightnessController = new BrightnessController(getContext(), (ImageView) findViewById(R$id.brightness_level), (ImageView) findViewById(R$id.brightness_icon), (ToggleSlider) findViewById(R$id.brightness_slider));
        this.mDumpController = dumpController;
        updateResources();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int paddingBottom = getPaddingBottom() + getPaddingTop();
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                int measuredHeight = paddingBottom + childAt.getMeasuredHeight();
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) childAt.getLayoutParams();
                paddingBottom = measuredHeight + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), paddingBottom);
    }

    public View getDivider() {
        return this.mDivider;
    }

    public View getPageIndicator() {
        return this.mPanelPageIndicator;
    }

    public QSTileRevealController getQsTileRevealController() {
        return this.mQsTileRevealController;
    }

    public boolean isShowingCustomize() {
        QSCustomizer qSCustomizer = this.mCustomizePanel;
        return qSCustomizer != null && qSCustomizer.isCustomizing();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "qs_show_brightness");
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            setTiles(qSTileHost.getTiles());
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.addCallback((BrightnessMirrorListener) this);
        }
        DumpController dumpController = this.mDumpController;
        if (dumpController != null) {
            dumpController.addListener(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            qSTileHost.removeCallback(this);
        }
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            ((TileRecord) it.next()).tile.removeCallbacks();
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.removeCallback((BrightnessMirrorListener) this);
        }
        DumpController dumpController = this.mDumpController;
        if (dumpController != null) {
            dumpController.removeListener(this);
        }
        super.onDetachedFromWindow();
    }

    public void onTilesChanged() {
        setTiles(this.mHost.getTiles());
    }

    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            updateViewVisibilityForTuningValue(this.mBrightnessView, str2);
        }
    }

    private void updateViewVisibilityForTuningValue(View view, String str) {
        view.setVisibility(TunerService.parseIntegerSwitch(str, true) ? 0 : 8);
    }

    public void openDetails(String str) {
        QSTile tile = getTile(str);
        if (tile != null) {
            showDetailAdapter(true, tile.getDetailAdapter(), new int[]{getWidth() / 2, 0});
        }
    }

    private QSTile getTile(String str) {
        for (int i = 0; i < this.mRecords.size(); i++) {
            if (str.equals(((TileRecord) this.mRecords.get(i)).tile.getTileSpec())) {
                return ((TileRecord) this.mRecords.get(i)).tile;
            }
        }
        return this.mHost.createTile(str);
    }

    public void setBrightnessMirror(BrightnessMirrorController brightnessMirrorController) {
        BrightnessMirrorController brightnessMirrorController2 = this.mBrightnessMirrorController;
        if (brightnessMirrorController2 != null) {
            brightnessMirrorController2.removeCallback((BrightnessMirrorListener) this);
        }
        this.mBrightnessMirrorController = brightnessMirrorController;
        BrightnessMirrorController brightnessMirrorController3 = this.mBrightnessMirrorController;
        if (brightnessMirrorController3 != null) {
            brightnessMirrorController3.addCallback((BrightnessMirrorListener) this);
        }
        updateBrightnessMirror();
    }

    public void onBrightnessMirrorReinflated(View view) {
        updateBrightnessMirror();
    }

    public void updateThemeColor() {
        int i;
        if (ThemeColorUtils.getCurrentTheme() == 2) {
            i = -1;
        } else {
            i = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        }
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_PROGRESS_BACKGROUND);
        int color2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_TILE_OFF);
        int color3 = ThemeColorUtils.getColor(ThemeColorUtils.QS_PANEL_PRIMARY);
        int thumbBackground = ThemeColorUtils.getThumbBackground();
        ToggleSeekBar toggleSeekBar = (ToggleSeekBar) this.mBrightnessView.findViewById(R$id.slider);
        toggleSeekBar.setThumbTintList(ColorStateList.valueOf(i));
        toggleSeekBar.setProgressTintList(ColorStateList.valueOf(i));
        toggleSeekBar.setProgressBackgroundTintList(ColorStateList.valueOf(color));
        toggleSeekBar.setBackgroundDrawable(this.mContext.getResources().getDrawable(thumbBackground));
        ((ImageView) this.mBrightnessView.findViewById(R$id.brightness_level)).setImageTintList(ColorStateList.valueOf(color2));
        ((ImageView) this.mBrightnessView.findViewById(R$id.brightness_icon)).setImageTintList(ColorStateList.valueOf(i));
        View view = this.mBrightnessMirror;
        if (view != null) {
            ToggleSeekBar toggleSeekBar2 = (ToggleSeekBar) view.findViewById(R$id.slider);
            ((FrameLayout) this.mBrightnessMirror.findViewById(R$id.brightness_mirror_frame)).setBackgroundTintList(ColorStateList.valueOf(color3));
            toggleSeekBar2.setThumbTintList(ColorStateList.valueOf(i));
            toggleSeekBar2.setProgressTintList(ColorStateList.valueOf(i));
            toggleSeekBar2.setProgressBackgroundTintList(ColorStateList.valueOf(color));
            toggleSeekBar2.setBackgroundDrawable(this.mContext.getResources().getDrawable(thumbBackground));
            ((ImageView) this.mBrightnessMirror.findViewById(R$id.brightness_level)).setImageTintList(ColorStateList.valueOf(color2));
            ((ImageView) this.mBrightnessMirror.findViewById(R$id.brightness_icon)).setImageTintList(ColorStateList.valueOf(i));
        }
        this.mFooter.updateThemeColor();
    }

    /* access modifiers changed from: 0000 */
    public View getBrightnessView() {
        return this.mBrightnessView;
    }

    public void setCallback(QSDetail.Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        this.mHost = qSTileHost;
        this.mHost.addCallback(this);
        setTiles(this.mHost.getTiles());
        this.mFooter.setHostEnvironment(qSTileHost);
        this.mCustomizePanel = qSCustomizer;
        QSCustomizer qSCustomizer2 = this.mCustomizePanel;
        if (qSCustomizer2 != null) {
            qSCustomizer2.setHost(this.mHost);
        }
    }

    public void setFooterPageIndicator(PageIndicator pageIndicator) {
        if (this.mTileLayout instanceof PagedTileLayout) {
            this.mFooterPageIndicator = pageIndicator;
            updatePageIndicator();
        }
    }

    private void updatePageIndicator() {
        if (this.mTileLayout instanceof PagedTileLayout) {
            PageIndicator pageIndicator = this.mFooterPageIndicator;
            if (pageIndicator != null) {
                pageIndicator.setVisibility(8);
            }
            ((PagedTileLayout) this.mTileLayout).setPageIndicator(this.mPanelPageIndicator);
        }
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Log.d("QSPanel", "updateResources");
        Resources resources = this.mContext.getResources();
        setPadding(0, resources.getDimensionPixelSize(R$dimen.qs_panel_padding_top), 0, resources.getDimensionPixelSize(R$dimen.qs_panel_padding_bottom));
        updatePageIndicator();
        refreshAllTiles();
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout != null) {
            qSTileLayout.updateResources();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mFooter.onConfigurationChanged();
        updateResources();
        updateBrightnessMirror();
    }

    public void updateBrightnessMirror() {
        if (this.mBrightnessMirrorController != null) {
            ToggleSliderView toggleSliderView = (ToggleSliderView) findViewById(R$id.brightness_slider);
            toggleSliderView.setMirror((ToggleSliderView) this.mBrightnessMirrorController.getMirror().findViewById(R$id.brightness_slider));
            toggleSliderView.setMirrorController(this.mBrightnessMirrorController);
            this.mBrightnessMirror = this.mBrightnessMirrorController.getMirror();
            this.mBrightnessController.setMirrorView(this.mBrightnessMirror);
            ((ToggleSeekBar) this.mBrightnessMirror.findViewById(R$id.slider)).setProgressBackgroundTintMode(Mode.SRC);
            updateThemeColor();
        }
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("setExpanded: ");
            sb.append(this.mExpanded);
            sb.append("->");
            sb.append(z);
            Log.d("QSPanel", sb.toString());
            this.mExpanded = z;
            if (!this.mExpanded) {
                QSTileLayout qSTileLayout = this.mTileLayout;
                if (qSTileLayout instanceof PagedTileLayout) {
                    ((PagedTileLayout) qSTileLayout).setCurrentItem(0, false);
                }
            }
            this.mMetricsLogger.visibility(C0526R.styleable.AppCompatTheme_textColorSearchUrl, this.mExpanded);
            if (!this.mExpanded) {
                QSCustomizer qSCustomizer = this.mCustomizePanel;
                if (qSCustomizer == null || !qSCustomizer.isShown()) {
                    showDetail(false, this.mDetailRecord);
                } else {
                    this.mCustomizePanel.hideNoAnimation();
                }
            } else {
                logTiles();
            }
        }
    }

    public void setPageListener(PageListener pageListener) {
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout instanceof PagedTileLayout) {
            ((PagedTileLayout) qSTileLayout).setPageListener(pageListener);
        }
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    public void setListening(boolean z) {
        if (this.mListening != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("setListening: ");
            sb.append(this.mListening);
            sb.append("->");
            sb.append(z);
            Log.d("QSPanel", sb.toString());
            this.mListening = z;
            QSTileLayout qSTileLayout = this.mTileLayout;
            if (qSTileLayout != null) {
                qSTileLayout.setListening(z);
            }
            if (this.mListening) {
                refreshAllTiles();
            }
        }
    }

    public void setListening(boolean z, boolean z2) {
        setListening(z && z2);
        getFooter().setListening(z);
        setBrightnessListening(z);
    }

    public void setBrightnessListening(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("setListeningBrightness: ");
            sb.append(z);
            Log.d("QSPanel", sb.toString());
        }
        if (z) {
            this.mBrightnessController.registerCallbacks();
        } else {
            this.mBrightnessController.unregisterCallbacks();
        }
    }

    public void refreshAllTiles() {
        Log.d("QSPanel", "refreshAllTiles");
        this.mBrightnessController.checkRestrictionAndSetEnabled();
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            ((TileRecord) it.next()).tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean z, DetailAdapter detailAdapter, int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        ((View) getParent()).getLocationInWindow(iArr);
        Record record = new Record();
        record.detailAdapter = detailAdapter;
        record.f87x = i - iArr[0];
        record.f88y = i2 - iArr[1];
        iArr[0] = i;
        iArr[1] = i2;
        showDetail(z, record);
    }

    /* access modifiers changed from: protected */
    public void showDetail(boolean z, Record record) {
        this.mHandler.obtainMessage(1, z ? 1 : 0, 0, record).sendToTarget();
    }

    public void setTiles(Collection<QSTile> collection) {
        setTiles(collection, false);
    }

    public void setTiles(Collection<QSTile> collection, boolean z) {
        if (!z) {
            this.mQsTileRevealController.updateRevealedTiles(collection);
        }
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord tileRecord = (TileRecord) it.next();
            this.mTileLayout.removeTile(tileRecord);
            tileRecord.tile.removeCallback(tileRecord.callback);
        }
        this.mRecords.clear();
        for (QSTile addTile : collection) {
            addTile(addTile, z);
        }
    }

    /* access modifiers changed from: protected */
    public void drawTile(TileRecord tileRecord, State state) {
        tileRecord.tileView.onStateChanged(state);
    }

    /* access modifiers changed from: protected */
    public QSTileView createTileView(QSTile qSTile, boolean z) {
        return this.mHost.createTileView(qSTile, z);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowDetail() {
        return this.mExpanded;
    }

    /* access modifiers changed from: protected */
    public TileRecord addTile(QSTile qSTile, boolean z) {
        final TileRecord tileRecord = new TileRecord();
        tileRecord.tile = qSTile;
        tileRecord.tileView = createTileView(qSTile, z);
        C09921 r2 = new QSTile.Callback() {
            public void onStateChanged(State state) {
                QSPanel.this.drawTile(tileRecord, state);
            }

            public void onShowDetail(boolean z) {
                if (QSPanel.this.shouldShowDetail()) {
                    QSPanel.this.showDetail(z, tileRecord);
                }
            }

            public void onToggleStateChanged(boolean z) {
                if (QSPanel.this.mDetailRecord == tileRecord) {
                    QSPanel.this.fireToggleStateChanged(z);
                }
            }

            public void onScanStateChanged(boolean z) {
                tileRecord.scanState = z;
                Record access$100 = QSPanel.this.mDetailRecord;
                TileRecord tileRecord = tileRecord;
                if (access$100 == tileRecord) {
                    QSPanel.this.fireScanStateChanged(tileRecord.scanState);
                }
            }

            public void onAnnouncementRequested(CharSequence charSequence) {
                if (charSequence != null) {
                    QSPanel.this.mHandler.obtainMessage(3, charSequence).sendToTarget();
                }
            }
        };
        tileRecord.tile.addCallback(r2);
        tileRecord.callback = r2;
        tileRecord.tileView.init(tileRecord.tile);
        tileRecord.tile.refreshState();
        this.mRecords.add(tileRecord);
        QSTileLayout qSTileLayout = this.mTileLayout;
        if (qSTileLayout != null) {
            qSTileLayout.addTile(tileRecord);
        }
        return tileRecord;
    }

    public void showEdit(final View view) {
        OpMdmLogger.log("quick_edit", "", "1");
        view.post(new Runnable() {
            public void run() {
                if (QSPanel.this.mCustomizePanel != null && !QSPanel.this.mCustomizePanel.isCustomizing()) {
                    int[] locationOnScreen = view.getLocationOnScreen();
                    QSPanel.this.mCustomizePanel.show(locationOnScreen[0] + (view.getWidth() / 2), locationOnScreen[1] + (view.getHeight() / 2));
                }
            }
        });
    }

    public void closeDetail() {
        QSCustomizer qSCustomizer = this.mCustomizePanel;
        if (qSCustomizer == null || !qSCustomizer.isShown()) {
            showDetail(false, this.mDetailRecord);
        } else {
            this.mCustomizePanel.hide();
        }
    }

    /* access modifiers changed from: protected */
    public void handleShowDetail(Record record, boolean z) {
        int i;
        if (record instanceof TileRecord) {
            handleShowDetailTile((TileRecord) record, z);
            return;
        }
        int i2 = 0;
        if (record != null) {
            i2 = record.f87x;
            i = record.f88y;
        } else {
            i = 0;
        }
        handleShowDetailImpl(record, z, i2, i);
    }

    private void handleShowDetailTile(TileRecord tileRecord, boolean z) {
        if ((this.mDetailRecord != null) != z || this.mDetailRecord != tileRecord) {
            if (z) {
                tileRecord.detailAdapter = tileRecord.tile.getDetailAdapter();
                if (tileRecord.detailAdapter == null) {
                    return;
                }
            }
            tileRecord.tile.setDetailListening(z);
            handleShowDetailImpl(tileRecord, z, tileRecord.tileView.getLeft() + (tileRecord.tileView.getWidth() / 2), tileRecord.tileView.getDetailY() + this.mTileLayout.getOffsetTop(tileRecord) + getTop());
        }
    }

    private void handleShowDetailImpl(Record record, boolean z, int i, int i2) {
        DetailAdapter detailAdapter = null;
        setDetailRecord(z ? record : null);
        if (z) {
            detailAdapter = record.detailAdapter;
        }
        fireShowingDetail(detailAdapter, i, i2);
    }

    /* access modifiers changed from: protected */
    public void setDetailRecord(Record record) {
        if (record != this.mDetailRecord) {
            this.mDetailRecord = record;
            Record record2 = this.mDetailRecord;
            fireScanStateChanged((record2 instanceof TileRecord) && ((TileRecord) record2).scanState);
        }
    }

    /* access modifiers changed from: 0000 */
    public void setGridContentVisibility(boolean z) {
        int i = z ? 0 : 4;
        setVisibility(i);
        if (this.mGridContentVisible != z) {
            this.mMetricsLogger.visibility(C0526R.styleable.AppCompatTheme_textColorSearchUrl, i);
        }
        this.mGridContentVisible = z;
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            QSTile qSTile = ((TileRecord) this.mRecords.get(i)).tile;
            this.mMetricsLogger.write(qSTile.populate(new LogMaker(qSTile.getMetricsCategory()).setType(1)));
        }
    }

    private void fireShowingDetail(DetailAdapter detailAdapter, int i, int i2) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onShowingDetail(detailAdapter, i, i2);
        }
    }

    /* access modifiers changed from: private */
    public void fireToggleStateChanged(boolean z) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onToggleStateChanged(z);
        }
    }

    /* access modifiers changed from: private */
    public void fireScanStateChanged(boolean z) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.onScanStateChanged(z);
        }
    }

    public void clickTile(ComponentName componentName) {
        String spec = CustomTile.toSpec(componentName);
        int size = this.mRecords.size();
        for (int i = 0; i < size; i++) {
            if (((TileRecord) this.mRecords.get(i)).tile.getTileSpec().equals(spec)) {
                ((TileRecord) this.mRecords.get(i)).tile.click();
                return;
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public QSTileLayout getTileLayout() {
        return this.mTileLayout;
    }

    /* access modifiers changed from: 0000 */
    public QSTileView getTileView(QSTile qSTile) {
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord tileRecord = (TileRecord) it.next();
            if (tileRecord.tile == qSTile) {
                return tileRecord.tileView;
            }
        }
        return null;
    }

    public QSSecurityFooter getFooter() {
        return this.mFooter;
    }

    public void showDeviceMonitoringDialog() {
        this.mFooter.showDeviceMonitoringDialog();
    }

    public void setMargins(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (childAt != this.mTileLayout) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                layoutParams.leftMargin = i;
                layoutParams.rightMargin = i;
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(":");
        printWriter.println(sb.toString());
        printWriter.println("  Tile records:");
        Iterator it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord tileRecord = (TileRecord) it.next();
            if (tileRecord.tile instanceof Dumpable) {
                String str = "    ";
                printWriter.print(str);
                ((Dumpable) tileRecord.tile).dump(fileDescriptor, printWriter, strArr);
                printWriter.print(str);
                printWriter.println(tileRecord.tileView.toString());
            }
        }
    }

    public void setIsExpanding(boolean z) {
        this.mFooter.setIsExpanding(z);
    }

    public void updateWLBIndicators(boolean[] zArr) {
        QSDetail.Callback callback = this.mCallback;
        if (callback != null) {
            callback.updateWlbIndicators(zArr);
        }
    }

    public void updateWLBExpansion(float f) {
        ((WLBSwitchController) Dependency.get(WLBSwitchController.class)).updateExpansionState(f);
    }

    public void updateWLBHeaderExpansion(float f) {
        WLBSwitchController wLBSwitchController = (WLBSwitchController) Dependency.get(WLBSwitchController.class);
        if (wLBSwitchController != null) {
            wLBSwitchController.updateHeaderExpansion(f);
        }
    }
}
