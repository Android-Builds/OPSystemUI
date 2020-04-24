package com.android.systemui.p007qs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.statusbar.CommandQueue;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.ThemeColorUtils;
import java.util.Objects;

/* renamed from: com.android.systemui.qs.QSDetail */
public class QSDetail extends LinearLayout {
    /* access modifiers changed from: private */
    public boolean mAnimatingOpen;
    private QSDetailClipper mClipper;
    /* access modifiers changed from: private */
    public boolean mClosingDetail;
    /* access modifiers changed from: private */
    public DetailAdapter mDetailAdapter;
    /* access modifiers changed from: private */
    public ViewGroup mDetailContent;
    protected TextView mDetailDoneButton;
    protected TextView mDetailSettingsButton;
    private final SparseArray<View> mDetailViews = new SparseArray<>();
    /* access modifiers changed from: private */
    public View mFooter;
    private boolean mFullyExpanded;
    /* access modifiers changed from: private */
    public QuickStatusBarHeader mHeader;
    private final AnimatorListenerAdapter mHideGridContentWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animator) {
            animator.removeListener(this);
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }

        public void onAnimationEnd(Animator animator) {
            if (QSDetail.this.mDetailAdapter != null) {
                QSDetail.this.mQsPanel.setGridContentVisibility(false);
                QSDetail.this.mHeader.setVisibility(4);
                QSDetail.this.mFooter.setVisibility(4);
            }
            QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }
    };
    protected QSTileHost mHost;
    /* access modifiers changed from: private */
    public LinearLayout mLayoutWlbIndicators;
    private int mOpenX;
    private int mOpenY;
    protected View mQsDetailHeader;
    private ImageView mQsDetailHeaderBack;
    protected ImageView mQsDetailHeaderProgress;
    protected Switch mQsDetailHeaderSwitch;
    protected TextView mQsDetailHeaderTitle;
    /* access modifiers changed from: private */
    public QSPanel mQsPanel;
    protected Callback mQsPanelCallback = new Callback() {
        public void onToggleStateChanged(final boolean z) {
            QSDetail.this.post(new Runnable() {
                public void run() {
                    QSDetail qSDetail = QSDetail.this;
                    qSDetail.handleToggleStateChanged(z, qSDetail.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                }
            });
        }

        public void onShowingDetail(final DetailAdapter detailAdapter, final int i, final int i2) {
            StringBuilder sb = new StringBuilder();
            sb.append("onShowingDetail: animatingOpen=");
            sb.append(QSDetail.this.mAnimatingOpen);
            sb.append(", closingDetail=");
            sb.append(QSDetail.this.mClosingDetail);
            String str = "QSDetail";
            Log.d(str, sb.toString());
            if (QSDetail.this.mAnimatingOpen || QSDetail.this.mClosingDetail) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Still animating detail, skip this operation:mAnimatingOpen=");
                sb2.append(QSDetail.this.mAnimatingOpen);
                sb2.append(", mClosingDetail=");
                sb2.append(QSDetail.this.mClosingDetail);
                Log.d(str, sb2.toString());
                return;
            }
            QSDetail.this.post(new Runnable() {
                public void run() {
                    if (QSDetail.this.isAttachedToWindow()) {
                        QSDetail.this.handleShowingDetail(detailAdapter, i, i2, false);
                    }
                }
            });
        }

        public void onScanStateChanged(final boolean z) {
            QSDetail.this.post(new Runnable() {
                public void run() {
                    QSDetail.this.handleScanStateChanged(z);
                }
            });
        }

        public void updateWlbIndicators(boolean[] zArr) {
            for (int i = 0; i < zArr.length; i++) {
                updateThemeColor((ImageView) QSDetail.this.mLayoutWlbIndicators.getChildAt(i), zArr[i]);
            }
        }

        private void updateThemeColor(ImageView imageView, boolean z) {
            imageView.setImageTintList(ColorStateList.valueOf(ThemeColorUtils.getColor(ThemeColorUtils.QS_WLB_INDICATOR)));
            if (z) {
                imageView.setAlpha(1.0f);
            } else {
                imageView.setAlpha(0.2f);
            }
        }
    };
    private boolean mScanState;
    private boolean mSwitchState;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            QSDetail.this.mDetailContent.removeAllViews();
            QSDetail.this.setVisibility(4);
            QSDetail.this.mClosingDetail = false;
        }
    };
    private boolean mTriggeredExpand;

    /* renamed from: com.android.systemui.qs.QSDetail$Callback */
    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(DetailAdapter detailAdapter, int i, int i2);

        void onToggleStateChanged(boolean z);

        void updateWlbIndicators(boolean[] zArr);
    }

    public QSDetail(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, R$dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, R$dimen.qs_detail_button_text_size);
        for (int i = 0; i < this.mDetailViews.size(); i++) {
            ((View) this.mDetailViews.valueAt(i)).dispatchConfigurationChanged(configuration);
        }
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        this.mQsDetailHeaderBack.setRotation(i == 1 ? 180.0f : 0.0f);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDetailContent = (ViewGroup) findViewById(16908290);
        this.mDetailSettingsButton = (TextView) findViewById(16908314);
        this.mDetailDoneButton = (TextView) findViewById(16908313);
        this.mQsDetailHeader = findViewById(R$id.qs_detail_header);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(16908310);
        this.mQsDetailHeaderSwitch = (Switch) this.mQsDetailHeader.findViewById(16908311);
        this.mQsDetailHeaderProgress = (ImageView) findViewById(R$id.qs_detail_header_progress);
        this.mLayoutWlbIndicators = (LinearLayout) findViewById(R$id.layout_wlb_indicators);
        updateDetailText();
        this.mClipper = new QSDetailClipper(this);
        C09741 r0 = new OnClickListener() {
            public void onClick(View view) {
                QSDetail qSDetail = QSDetail.this;
                qSDetail.announceForAccessibility(qSDetail.mContext.getString(R$string.accessibility_desc_quick_settings));
                QSDetail.this.mQsPanel.closeDetail();
            }
        };
        this.mDetailDoneButton.setOnClickListener(r0);
        this.mQsDetailHeaderBack = (ImageView) this.mQsDetailHeader.findViewById(16909530);
        this.mQsDetailHeaderBack.setOnClickListener(r0);
        updateThemeColor();
    }

    /* access modifiers changed from: protected */
    public void updateThemeColor() {
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_PRIMARY_TEXT);
        int color2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_BUTTON);
        int color3 = ThemeColorUtils.getColor(ThemeColorUtils.QS_PANEL_PRIMARY);
        this.mDetailSettingsButton.setTextColor(color2);
        this.mDetailDoneButton.setTextColor(color2);
        this.mQsDetailHeaderTitle.setTextColor(color);
        this.mQsDetailHeaderBack.setImageTintList(ColorStateList.valueOf(color2));
        setBackgroundTintList(ColorStateList.valueOf(color3));
    }

    public void setQsPanel(QSPanel qSPanel, QuickStatusBarHeader quickStatusBarHeader, View view) {
        this.mQsPanel = qSPanel;
        this.mHeader = quickStatusBarHeader;
        this.mFooter = view;
        this.mHeader.setCallback(this.mQsPanelCallback);
        this.mQsPanel.setCallback(this.mQsPanelCallback);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public boolean isShowingDetail() {
        return this.mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean z) {
        if (this.mFullyExpanded != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("setFullyExpanded: ");
            sb.append(z);
            Log.d("QSDetail", sb.toString());
        }
        this.mFullyExpanded = z;
    }

    public void setExpanded(boolean z) {
        if (!z) {
            this.mTriggeredExpand = false;
        }
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R$string.quick_settings_done);
        this.mDetailSettingsButton.setText(R$string.quick_settings_more_settings);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public void handleShowingDetail(DetailAdapter detailAdapter, int i, int i2, boolean z) {
        AnimatorListenerAdapter animatorListenerAdapter;
        boolean z2 = detailAdapter != null;
        setClickable(z2);
        if (z2) {
            setupDetailHeader(detailAdapter);
            if (!z || this.mFullyExpanded) {
                this.mTriggeredExpand = false;
            } else {
                this.mTriggeredExpand = true;
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateExpandSettingsPanel(null);
            }
            this.mOpenX = i;
            this.mOpenY = i2;
        } else {
            i = this.mOpenX;
            i2 = this.mOpenY;
            if (z && this.mTriggeredExpand) {
                ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels();
                this.mTriggeredExpand = false;
            }
        }
        boolean z3 = (this.mDetailAdapter != null) != (detailAdapter != null);
        if (z3 || this.mDetailAdapter != detailAdapter) {
            if (detailAdapter != null) {
                int metricsCategory = detailAdapter.getMetricsCategory();
                View createDetailView = detailAdapter.createDetailView(this.mContext, (View) this.mDetailViews.get(metricsCategory), this.mDetailContent);
                if (createDetailView != null) {
                    setupDetailFooter(detailAdapter);
                    this.mDetailContent.removeAllViews();
                    this.mDetailContent.addView(createDetailView);
                    this.mDetailViews.put(metricsCategory, createDetailView);
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).visible(detailAdapter.getMetricsCategory());
                    announceForAccessibility(this.mContext.getString(R$string.accessibility_quick_settings_detail, new Object[]{detailAdapter.getTitle()}));
                    this.mDetailAdapter = detailAdapter;
                    animatorListenerAdapter = this.mHideGridContentWhenDone;
                    setVisibility(0);
                } else {
                    throw new IllegalStateException("Must return detail view");
                }
            } else {
                if (this.mDetailAdapter != null) {
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).hidden(this.mDetailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                this.mDetailAdapter = null;
                animatorListenerAdapter = this.mTeardownDetailWhenDone;
                this.mHeader.setVisibility(0);
                this.mFooter.setVisibility(0);
                this.mQsPanel.setGridContentVisibility(true);
                this.mQsPanelCallback.onScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            animateDetailVisibleDiff(i, i2, z3, animatorListenerAdapter);
        }
    }

    /* access modifiers changed from: protected */
    public void animateDetailVisibleDiff(int i, int i2, boolean z, AnimatorListener animatorListener) {
        if (z) {
            boolean z2 = true;
            this.mAnimatingOpen = this.mDetailAdapter != null;
            if (this.mFullyExpanded || this.mDetailAdapter != null) {
                setAlpha(1.0f);
                QSDetailClipper qSDetailClipper = this.mClipper;
                if (this.mDetailAdapter == null) {
                    z2 = false;
                }
                qSDetailClipper.animateCircularClip(i, i2, z2, animatorListener);
                return;
            }
            animate().alpha(0.0f).setDuration(300).setListener(animatorListener).start();
        }
    }

    /* access modifiers changed from: protected */
    public void setupDetailFooter(DetailAdapter detailAdapter) {
        Intent settingsIntent = detailAdapter.getSettingsIntent();
        this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
        this.mDetailSettingsButton.setOnClickListener(new OnClickListener(settingsIntent) {
            private final /* synthetic */ Intent f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                QSDetail.lambda$setupDetailFooter$0(DetailAdapter.this, this.f$1, view);
            }
        });
    }

    static /* synthetic */ void lambda$setupDetailFooter$0(DetailAdapter detailAdapter, Intent intent, View view) {
        OpMdmLogger.log("qt_mode_change_menu", "qt_menu_settings", "1", "C22AG9UUDL");
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).action(929, detailAdapter.getMetricsCategory());
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
    }

    /* access modifiers changed from: protected */
    public void setupDetailHeader(final DetailAdapter detailAdapter) {
        this.mQsDetailHeaderTitle.setText(detailAdapter.getTitle());
        if (detailAdapter.getMetricsCategory() == 2006) {
            this.mLayoutWlbIndicators.setVisibility(0);
            this.mDetailSettingsButton.setText(getContext().getString(R$string.quick_settings_settings_label));
            this.mDetailSettingsButton.setTag("wlb_settings");
        } else {
            updateDetailText();
            this.mDetailSettingsButton.setTag("");
            this.mLayoutWlbIndicators.setVisibility(8);
        }
        Boolean toggleState = detailAdapter.getToggleState();
        if (toggleState == null) {
            this.mQsDetailHeaderSwitch.setVisibility(4);
            this.mQsDetailHeader.setClickable(false);
            return;
        }
        this.mQsDetailHeaderSwitch.setVisibility(0);
        handleToggleStateChanged(toggleState.booleanValue(), detailAdapter.getToggleEnabled());
        this.mQsDetailHeader.setClickable(true);
        this.mQsDetailHeader.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                boolean z = !QSDetail.this.mQsDetailHeaderSwitch.isChecked();
                QSDetail.this.mQsDetailHeaderSwitch.setChecked(z);
                detailAdapter.setToggleState(z);
            }
        });
    }

    /* access modifiers changed from: private */
    public void handleToggleStateChanged(boolean z, boolean z2) {
        this.mSwitchState = z;
        if (!this.mAnimatingOpen) {
            this.mQsDetailHeaderSwitch.setChecked(z);
            this.mQsDetailHeader.setEnabled(z2);
            this.mQsDetailHeaderSwitch.setEnabled(z2);
        }
    }

    /* access modifiers changed from: private */
    public void handleScanStateChanged(boolean z) {
        if (this.mScanState != z) {
            this.mScanState = z;
            Animatable animatable = (Animatable) this.mQsDetailHeaderProgress.getDrawable();
            if (z) {
                this.mQsDetailHeaderProgress.animate().cancel();
                ViewPropertyAnimator alpha = this.mQsDetailHeaderProgress.animate().alpha(1.0f);
                Objects.requireNonNull(animatable);
                alpha.withEndAction(new Runnable(animatable) {
                    private final /* synthetic */ Animatable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void run() {
                        this.f$0.start();
                    }
                }).start();
            } else {
                this.mQsDetailHeaderProgress.animate().cancel();
                ViewPropertyAnimator alpha2 = this.mQsDetailHeaderProgress.animate().alpha(0.0f);
                Objects.requireNonNull(animatable);
                alpha2.withEndAction(new Runnable(animatable) {
                    private final /* synthetic */ Animatable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void run() {
                        this.f$0.stop();
                    }
                }).start();
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkPendingAnimations() {
        boolean z = this.mSwitchState;
        DetailAdapter detailAdapter = this.mDetailAdapter;
        handleToggleStateChanged(z, detailAdapter != null && detailAdapter.getToggleEnabled());
    }
}
