package com.android.systemui.p007qs;

import android.util.Log;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;
import com.android.systemui.Dependency;
import com.android.systemui.p007qs.PagedTileLayout.PageListener;
import com.android.systemui.p007qs.PagedTileLayout.TilePage;
import com.android.systemui.p007qs.QSHost.Callback;
import com.android.systemui.p007qs.QSPanel.QSTileLayout;
import com.android.systemui.p007qs.TouchAnimator.Builder;
import com.android.systemui.p007qs.TouchAnimator.Listener;
import com.android.systemui.p007qs.TouchAnimator.ListenerAdapter;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.plugins.p006qs.QSTileView;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/* renamed from: com.android.systemui.qs.QSAnimator */
public class QSAnimator implements Callback, PageListener, Listener, OnLayoutChangeListener, OnAttachStateChangeListener, Tunable {
    private final ArrayList<View> mAllViews = new ArrayList<>();
    private boolean mAllowFancy;
    private TouchAnimator mBrightnessAnimator;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private boolean mFullRows;
    private QSTileHost mHost;
    /* access modifiers changed from: private */
    public float mLastPosition;
    private final Listener mNonFirstPageListener = new ListenerAdapter() {
        public void onAnimationAtEnd() {
            QSAnimator.this.mQuickQsPanel.setVisibility(4);
        }

        public void onAnimationStarted() {
            QSAnimator.this.mQuickQsPanel.setVisibility(0);
        }
    };
    private TouchAnimator mNonfirstPageAnimator;
    private TouchAnimator mNonfirstPageDelayedAnimator;
    private int mNumQuickTiles;
    private boolean mOnFirstPage = true;
    private boolean mOnKeyguard;
    private TouchAnimator mPageIndicatorAnimator;
    private PagedTileLayout mPagedLayout;
    private final C0959QS mQs;
    private final QSPanel mQsPanel;
    /* access modifiers changed from: private */
    public final QuickQSPanel mQuickQsPanel;
    private final ArrayList<View> mQuickQsViews = new ArrayList<>();
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private Runnable mUpdateAnimators = new Runnable() {
        public void run() {
            QSAnimator.this.updateAnimators();
            QSAnimator qSAnimator = QSAnimator.this;
            qSAnimator.setPosition(qSAnimator.mLastPosition);
        }
    };

    public QSAnimator(C0959QS qs, QuickQSPanel quickQSPanel, QSPanel qSPanel) {
        this.mQs = qs;
        this.mQuickQsPanel = quickQSPanel;
        this.mQsPanel = qSPanel;
        this.mQsPanel.addOnAttachStateChangeListener(this);
        qs.getView().addOnLayoutChangeListener(this);
        if (this.mQsPanel.isAttachedToWindow()) {
            onViewAttachedToWindow(null);
        }
        QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
        if (tileLayout instanceof PagedTileLayout) {
            this.mPagedLayout = (PagedTileLayout) tileLayout;
        } else {
            Log.w("QSAnimator", "QS Not using page layout");
        }
        qSPanel.setPageListener(this);
    }

    public void onRtlChanged() {
        updateAnimators();
    }

    public void setOnKeyguard(boolean z) {
        this.mOnKeyguard = z;
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnKeyguard) {
            clearAnimationState();
        }
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        qSTileHost.addCallback(this);
        updateAnimators();
    }

    public void onViewAttachedToWindow(View view) {
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_qs_fancy_anim", "sysui_qs_move_whole_rows", "sysui_qqs_count");
    }

    public void onViewDetachedFromWindow(View view) {
        QSTileHost qSTileHost = this.mHost;
        if (qSTileHost != null) {
            qSTileHost.removeCallback(this);
        }
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    public void onTuningChanged(String str, String str2) {
        if ("sysui_qs_fancy_anim".equals(str)) {
            this.mAllowFancy = TunerService.parseIntegerSwitch(str2, true);
            if (!this.mAllowFancy) {
                clearAnimationState();
            }
        } else if ("sysui_qs_move_whole_rows".equals(str)) {
            this.mFullRows = TunerService.parseIntegerSwitch(str2, true);
        } else if ("sysui_qqs_count".equals(str)) {
            this.mNumQuickTiles = QuickQSPanel.getNumQuickTiles(this.mQs.getContext());
            clearAnimationState();
        }
        updateAnimators();
    }

    public void onPageChanged(boolean z) {
        if (this.mOnFirstPage != z) {
            if (!z) {
                clearAnimationState();
            } else {
                updateAnimators();
            }
            this.mOnFirstPage = z;
        }
    }

    /* access modifiers changed from: private */
    public void updateAnimators() {
        String str;
        int i;
        float f;
        Collection collection;
        int i2;
        int i3;
        int[] iArr;
        boolean z;
        int[] iArr2;
        boolean z2;
        int[] iArr3;
        Builder builder = new Builder();
        Builder builder2 = new Builder();
        Builder builder3 = new Builder();
        if (this.mQsPanel.getHost() != null) {
            Collection tiles = this.mQsPanel.getHost().getTiles();
            int[] iArr4 = new int[2];
            int[] iArr5 = new int[2];
            clearAnimationState();
            this.mAllViews.clear();
            this.mQuickQsViews.clear();
            QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
            this.mAllViews.add((View) tileLayout);
            int measuredHeight = this.mQs.getView() != null ? this.mQs.getView().getMeasuredHeight() : 0;
            int measuredWidth = this.mQs.getView() != null ? this.mQs.getView().getMeasuredWidth() : 0;
            int bottom = (measuredHeight - this.mQs.getHeader().getBottom()) + this.mQs.getHeader().getPaddingBottom();
            float f2 = (float) bottom;
            String str2 = "translationY";
            builder.addFloat(tileLayout, str2, f2, 0.0f);
            Iterator it = tiles.iterator();
            int i4 = 0;
            int i5 = 0;
            while (true) {
                str = "alpha";
                if (!it.hasNext()) {
                    break;
                }
                QSTile qSTile = (QSTile) it.next();
                Iterator it2 = it;
                QSTileView tileView = this.mQsPanel.getTileView(qSTile);
                if (tileView == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("tileView is null ");
                    sb.append(qSTile.getTileSpec());
                    Log.e("QSAnimator", sb.toString());
                    collection = tiles;
                    i2 = bottom;
                    i = measuredWidth;
                    f = f2;
                } else {
                    collection = tiles;
                    View iconView = tileView.getIcon().getIconView();
                    i2 = bottom;
                    View view = this.mQs.getView();
                    f = f2;
                    i = measuredWidth;
                    String str3 = "translationX";
                    if (i4 >= this.mQuickQsPanel.getTileLayout().getNumVisibleTiles() || !this.mAllowFancy) {
                        int[] iArr6 = iArr5;
                        if (!this.mFullRows || i4 >= tileLayout.getNumVisibleTiles()) {
                            iArr = iArr4;
                            iArr2 = iArr6;
                            builder.addFloat(tileView, str, 0.0f, 1.0f);
                            bottom = i2;
                            z2 = false;
                            z = true;
                            builder.addFloat(tileView, str2, (float) (-bottom), 0.0f);
                            this.mAllViews.add(tileView);
                            i4++;
                            boolean z3 = z;
                            it = it2;
                            tiles = collection;
                            f2 = f;
                            i3 = i;
                            iArr4 = iArr;
                            int[] iArr7 = iArr2;
                            boolean z4 = z2;
                            iArr5 = iArr7;
                        } else {
                            iArr4[0] = iArr4[0] + i5;
                            iArr2 = iArr6;
                            getRelativePosition(iArr2, iconView, view);
                            int i6 = iArr2[0] - iArr4[0];
                            int i7 = iArr2[1] - iArr4[1];
                            iArr = iArr4;
                            builder.addFloat(tileView, str2, f, 0.0f);
                            builder2.addFloat(tileView, str3, (float) (-i6), 0.0f);
                            builder3.addFloat(tileView, str2, (float) (-i7), 0.0f);
                            this.mAllViews.add(iconView);
                            bottom = i2;
                        }
                    } else {
                        QSTileView tileView2 = this.mQuickQsPanel.getTileView(qSTile);
                        if (tileView2 != null) {
                            int i8 = iArr4[0];
                            getRelativePosition(iArr4, tileView2.getIcon().getIconView(), view);
                            getRelativePosition(iArr5, iconView, view);
                            int i9 = iArr5[0] - iArr4[0];
                            int i10 = iArr5[1] - iArr4[1];
                            i5 = iArr4[0] - i8;
                            if (i4 < tileLayout.getNumVisibleTiles()) {
                                builder2.addFloat(tileView2, str3, 0.0f, (float) i9);
                                builder3.addFloat(tileView2, str2, 0.0f, (float) i10);
                                builder2.addFloat(tileView, str3, (float) (-i9), 0.0f);
                                builder3.addFloat(tileView, str2, (float) (-i10), 0.0f);
                                iArr3 = iArr5;
                            } else {
                                iArr3 = iArr5;
                                builder.addFloat(tileView2, str, 1.0f, 0.0f);
                                builder3.addFloat(tileView2, str2, 0.0f, (float) i10);
                                builder2.addFloat(tileView2, str3, 0.0f, (float) (this.mQsPanel.isLayoutRtl() ? i9 - i : i9 + i));
                            }
                            this.mQuickQsViews.add(tileView.getIconWithBackground());
                            this.mAllViews.add(tileView.getIcon());
                            this.mAllViews.add(tileView2);
                            iArr = iArr4;
                            bottom = i2;
                            iArr2 = iArr3;
                        }
                    }
                    z2 = false;
                    z = true;
                    this.mAllViews.add(tileView);
                    i4++;
                    boolean z32 = z;
                    it = it2;
                    tiles = collection;
                    f2 = f;
                    i3 = i;
                    iArr4 = iArr;
                    int[] iArr72 = iArr2;
                    boolean z42 = z2;
                    iArr5 = iArr72;
                }
                it = it2;
                bottom = i2;
                tiles = collection;
                f2 = f;
                i3 = i;
            }
            Collection collection2 = tiles;
            if (this.mAllowFancy) {
                View brightnessView = this.mQsPanel.getBrightnessView();
                if (brightnessView != null) {
                    Builder builder4 = new Builder();
                    builder4.setStartDelay(0.86f);
                    builder4.addFloat(brightnessView, str, 0.0f, 1.0f);
                    this.mBrightnessAnimator = builder4.build();
                    this.mAllViews.add(brightnessView);
                } else {
                    this.mBrightnessAnimator = null;
                }
                builder.setListener(this);
                this.mFirstPageAnimator = builder.build();
                Builder builder5 = new Builder();
                builder5.setStartDelay(0.46f);
                builder5.addFloat(tileLayout, str, 0.0f, 1.0f);
                builder5.addFloat(this.mQsPanel.getFooter().getView(), str, 0.0f, 1.0f);
                this.mFirstPageDelayedAnimator = builder5.build();
                Builder builder6 = new Builder();
                builder6.setStartDelay(0.86f);
                builder6.addFloat(this.mQsPanel.getPageIndicator(), str, 0.0f, 1.0f);
                this.mPageIndicatorAnimator = builder6.build();
                this.mAllViews.add(this.mQsPanel.getPageIndicator());
                this.mAllViews.add(this.mQsPanel.getFooter().getView());
                float f3 = collection2.size() <= 3 ? 1.0f : collection2.size() <= 6 ? 0.4f : 0.0f;
                PathInterpolatorBuilder pathInterpolatorBuilder = new PathInterpolatorBuilder(0.0f, 0.0f, f3, 1.0f);
                builder2.setInterpolator(pathInterpolatorBuilder.getXInterpolator());
                builder3.setInterpolator(pathInterpolatorBuilder.getYInterpolator());
                this.mTranslationXAnimator = builder2.build();
                this.mTranslationYAnimator = builder3.build();
            }
            Builder builder7 = new Builder();
            builder7.addFloat(this.mQuickQsPanel, str, 1.0f, 0.0f);
            builder7.addFloat(this.mQsPanel.getPageIndicator(), str, 0.0f, 1.0f);
            builder7.setListener(this.mNonFirstPageListener);
            builder7.setEndDelay(0.5f);
            this.mNonfirstPageAnimator = builder7.build();
            Builder builder8 = new Builder();
            builder8.setStartDelay(0.14f);
            builder8.addFloat(tileLayout, str, 0.0f, 1.0f);
            this.mNonfirstPageDelayedAnimator = builder8.build();
        }
    }

    private void getRelativePosition(int[] iArr, View view, View view2) {
        iArr[0] = (view.getWidth() / 2) + 0;
        iArr[1] = 0;
        getRelativePositionInt(iArr, view, view2);
    }

    private void getRelativePositionInt(int[] iArr, View view, View view2) {
        if (view != view2 && view != null) {
            if (!(view instanceof TilePage)) {
                iArr[0] = iArr[0] + view.getLeft();
                iArr[1] = iArr[1] + view.getTop();
            }
            getRelativePositionInt(iArr, (View) view.getParent(), view2);
        }
    }

    public void setPosition(float f) {
        if (this.mFirstPageAnimator != null && !this.mOnKeyguard) {
            this.mLastPosition = f;
            if (this.mAllowFancy) {
                if (this.mOnFirstPage) {
                    this.mQuickQsPanel.setAlpha(1.0f);
                    this.mFirstPageAnimator.setPosition(f);
                    this.mFirstPageDelayedAnimator.setPosition(f);
                    this.mTranslationXAnimator.setPosition(f);
                    this.mTranslationYAnimator.setPosition(f);
                } else {
                    this.mNonfirstPageAnimator.setPosition(f);
                    this.mNonfirstPageDelayedAnimator.setPosition(f);
                }
                TouchAnimator touchAnimator = this.mBrightnessAnimator;
                if (touchAnimator != null) {
                    touchAnimator.setPosition(f);
                }
                TouchAnimator touchAnimator2 = this.mPageIndicatorAnimator;
                if (touchAnimator2 != null) {
                    touchAnimator2.setPosition(f);
                }
            }
        }
    }

    public void onAnimationAtStart() {
        this.mQuickQsPanel.setVisibility(0);
    }

    public void onAnimationAtEnd() {
        this.mQuickQsPanel.setVisibility(4);
        int size = this.mQuickQsViews.size();
        for (int i = 0; i < size; i++) {
            ((View) this.mQuickQsViews.get(i)).setVisibility(0);
        }
    }

    public void onAnimationStarted() {
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnFirstPage) {
            int size = this.mQuickQsViews.size();
            for (int i = 0; i < size; i++) {
                ((View) this.mQuickQsViews.get(i)).setVisibility(4);
            }
        }
    }

    private void clearAnimationState() {
        int size = this.mAllViews.size();
        this.mQuickQsPanel.setAlpha(0.0f);
        for (int i = 0; i < size; i++) {
            View view = (View) this.mAllViews.get(i);
            view.setAlpha(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        }
        int size2 = this.mQuickQsViews.size();
        for (int i2 = 0; i2 < size2; i2++) {
            ((View) this.mQuickQsViews.get(i2)).setVisibility(0);
        }
    }

    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mQsPanel.post(this.mUpdateAnimators);
    }

    public void onTilesChanged() {
        this.mQsPanel.post(this.mUpdateAnimators);
    }
}
