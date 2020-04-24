package com.android.systemui.p007qs.customize;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.Toolbar.OnMenuItemClickListener;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.PageIndicator;
import com.android.systemui.p007qs.QSDetailClipper;
import com.android.systemui.p007qs.QSTileHost;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.plugins.p006qs.QSTile;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import com.oneplus.util.ThemeColorUtils;
import java.util.ArrayList;

/* renamed from: com.android.systemui.qs.customize.QSCustomizer */
public class QSCustomizer extends LinearLayout implements OnMenuItemClickListener {
    /* access modifiers changed from: private */
    public boolean isShown;
    private final QSDetailClipper mClipper;
    private final AnimatorListener mCollapseAnimationListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            QSCustomizer.this.setCustomizing(false);
        }

        public void onAnimationCancel(Animator animator) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            QSCustomizer.this.setCustomizing(false);
        }
    };
    private View mContainer;
    private boolean mCustomizing;
    private View mDivider;
    private TextView mDragLabel;
    private final AnimatorListener mExpandAnimationListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            if (QSCustomizer.this.isShown) {
                QSCustomizer.this.setCustomizing(true);
            }
            QSCustomizer.this.mOpening = false;
            if (QSCustomizer.this.mNotifQsContainer != null) {
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
            QSCustomizer.this.mPageManager.calculateItemLocation();
        }

        public void onAnimationCancel(Animator animator) {
            QSCustomizer.this.mOpening = false;
            if (QSCustomizer.this.mNotifQsContainer != null) {
                QSCustomizer.this.mNotifQsContainer.setCustomizerAnimating(false);
            }
            QSCustomizer.this.mPageManager.calculateItemLocation();
        }
    };
    private QSTileHost mHost;
    private boolean mIsShowingNavBackdrop;
    private final Callback mKeyguardCallback = new Callback() {
        public final void onKeyguardShowingChanged() {
            QSCustomizer.this.lambda$new$0$QSCustomizer();
        }
    };
    private final LightBarController mLightBarController;
    private QSEditViewPager mLowerPages;
    /* access modifiers changed from: private */
    public NotificationsQuickSettingsContainer mNotifQsContainer;
    /* access modifiers changed from: private */
    public boolean mOpening;
    /* access modifiers changed from: private */
    public QSEditPageManager mPageManager;
    private C0959QS mQs;
    private final TileQueryHelper mTileQueryHelper;
    private Toolbar mToolbar;
    private QSEditViewPager mUpperPages;

    /* renamed from: mX */
    private int f89mX;

    /* renamed from: mY */
    private int f90mY;

    /* renamed from: com.android.systemui.qs.customize.QSCustomizer$EmptyClickListener */
    public class EmptyClickListener implements OnClickListener {
        public void onClick(View view) {
        }

        public EmptyClickListener() {
        }
    }

    public QSCustomizer(Context context, AttributeSet attributeSet) {
        super(new ContextThemeWrapper(context, ThemeColorUtils.getEditTheme()), attributeSet);
        LayoutInflater.from(getContext()).inflate(R$layout.qs_customize_panel_content2, this);
        this.mContainer = findViewById(R$id.customize_container);
        this.mClipper = new QSDetailClipper(findViewById(R$id.customize_container));
        this.mToolbar = (Toolbar) findViewById(16908682);
        this.mContext.getTheme().resolveAttribute(16843531, new TypedValue(), true);
        this.mToolbar.setNavigationIcon(getResources().getDrawable(R$drawable.ic_qs_edit_back));
        this.mToolbar.setNavigationOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                QSCustomizer.this.hide();
            }
        });
        this.mToolbar.setOverflowIcon(getResources().getDrawable(R$drawable.ic_qs_edit_menu));
        this.mToolbar.setOnMenuItemClickListener(this);
        this.mToolbar.setTitle(R$string.qs_edit);
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        this.mUpperPages = (QSEditViewPager) findViewById(R$id.upperPages);
        this.mLowerPages = (QSEditViewPager) findViewById(R$id.lowerPages);
        this.mUpperPages.setPageIndicator((PageIndicator) findViewById(R$id.upperIndicator));
        this.mLowerPages.setPageIndicator((PageIndicator) findViewById(R$id.lowerIndicator));
        this.mDragLabel = (TextView) findViewById(R$id.dragLabel);
        this.mPageManager = new QSEditPageManager(context, this.mUpperPages, this.mLowerPages, this.mDragLabel);
        this.mPageManager.setLayoutRTL(isLayoutRtl());
        this.mTileQueryHelper = new TileQueryHelper(context, this.mPageManager);
        updateNavBackDrop(getResources().getConfiguration());
        this.mDivider = findViewById(R$id.divider);
        this.mDragLabel.setOnClickListener(new EmptyClickListener());
        this.mToolbar.setOnClickListener(new EmptyClickListener());
        setOnClickListener(new EmptyClickListener());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        this.mNotifQsContainer = null;
        ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
        this.mTileQueryHelper.destroyTiles();
        super.onDetachedFromWindow();
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        this.mPageManager.setLayoutRTL(isLayoutRtl());
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        updateThemeColor();
        this.mToolbar.getMenu().add(0, 1, 0, this.mContext.getString(17040939));
    }

    /* access modifiers changed from: protected */
    public void updateThemeColor() {
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_PANEL_PRIMARY);
        int color2 = ThemeColorUtils.getColor(ThemeColorUtils.QS_BUTTON);
        int color3 = ThemeColorUtils.getColor(ThemeColorUtils.QS_PRIMARY_TEXT);
        int color4 = ThemeColorUtils.getColor(ThemeColorUtils.QS_SECONDARY_TEXT);
        int color5 = ThemeColorUtils.getColor(ThemeColorUtils.QS_SEPARATOR);
        if (ThemeColorUtils.getCurrentTheme() != 2) {
            ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        }
        int color6 = ThemeColorUtils.getColor(ThemeColorUtils.QS_EDIT_BOTTOM);
        int childCount = this.mToolbar.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mToolbar.getChildAt(i);
            if (childAt instanceof ImageButton) {
                ImageButton imageButton = (ImageButton) childAt;
                imageButton.setImageTintList(ColorStateList.valueOf(color2));
                imageButton.setBackgroundResource(R$drawable.ripple_drawable_dark);
            } else if (childAt instanceof TextView) {
                ((TextView) childAt).setTextColor(color3);
            }
        }
        setBackgroundTintList(ColorStateList.valueOf(color6));
        this.mContainer.setBackgroundTintList(ColorStateList.valueOf(color));
        this.mLowerPages.setBackgroundColor(color6);
        this.mUpperPages.setBackgroundColor(color);
        findViewById(R$id.toolbar_panel).setBackgroundColor(color);
        findViewById(R$id.upper_indicator_panel).setBackgroundColor(color);
        findViewById(R$id.lower_indicator_panel).setBackgroundColor(color6);
        this.mToolbar.setPopupTheme(ThemeColorUtils.getPopTheme());
        if (this.mToolbar.getOverflowIcon() != null) {
            this.mToolbar.getOverflowIcon().setTint(color2);
        }
        this.mDragLabel.setBackgroundColor(color6);
        this.mDragLabel.setTextColor(color4);
        this.mDivider.setBackgroundColor(color5);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (configuration.orientation == 2) {
            hide();
        }
        updateNavBackDrop(configuration);
        this.mTileQueryHelper.recalcEditPage();
    }

    private void updateNavBackDrop(Configuration configuration) {
        View findViewById = findViewById(R$id.nav_bar_background);
        int i = 0;
        this.mIsShowingNavBackdrop = (configuration.smallestScreenWidthDp >= 600 || configuration.orientation != 2) && !QuickStepContract.isGesturalMode(((OverviewProxyService) Dependency.get(OverviewProxyService.class)).getNavBarMode());
        if (findViewById != null) {
            if (!this.mIsShowingNavBackdrop) {
                i = 8;
            }
            findViewById.setVisibility(i);
        }
        updateNavColors();
    }

    private void updateNavColors() {
        this.mLightBarController.setQsCustomizing(this.mIsShowingNavBackdrop && this.isShown);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        this.mPageManager.setHost(qSTileHost);
        queryTiles();
    }

    public void setContainer(NotificationsQuickSettingsContainer notificationsQuickSettingsContainer) {
        this.mNotifQsContainer = notificationsQuickSettingsContainer;
    }

    public void setQs(C0959QS qs) {
        this.mQs = qs;
    }

    public void show(int i, int i2) {
        if (!this.isShown) {
            Log.d("QSCustomizer", "show edit UI");
            int[] locationOnScreen = findViewById(R$id.customize_container).getLocationOnScreen();
            this.f89mX = i - locationOnScreen[0];
            this.f90mY = i2 - locationOnScreen[1];
            MetricsLogger.visible(getContext(), 358);
            this.isShown = true;
            this.mOpening = true;
            setTileSpecs();
            setVisibility(0);
            this.mClipper.animateCircularClip(this.f89mX, this.f90mY, true, this.mExpandAnimationListener);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(true);
            updateNavColors();
        }
    }

    public void showImmediately() {
        if (!this.isShown) {
            setVisibility(0);
            this.mClipper.showBackground();
            this.isShown = true;
            setTileSpecs();
            setCustomizing(true);
            queryTiles();
            this.mNotifQsContainer.setCustomizerAnimating(false);
            this.mNotifQsContainer.setCustomizerShowing(true);
            updateNavColors();
        }
    }

    private void queryTiles() {
        this.mTileQueryHelper.queryTiles(this.mHost);
    }

    public void hide() {
        if (this.isShown) {
            Log.d("QSCustomizer", "hide edit UI");
            MetricsLogger.hidden(getContext(), 358);
            this.isShown = false;
            this.mToolbar.dismissPopupMenus();
            setCustomizing(false);
            save();
            this.mClipper.animateCircularClip(this.f89mX, this.f90mY, false, this.mCollapseAnimationListener);
            this.mNotifQsContainer.setCustomizerAnimating(true);
            this.mNotifQsContainer.setCustomizerShowing(false);
            updateNavColors();
            this.mTileQueryHelper.destroyTiles();
        }
    }

    public void hideNoAnimation() {
        MetricsLogger.hidden(getContext(), 358);
        this.isShown = false;
        this.mToolbar.dismissPopupMenus();
        setCustomizing(false);
        save();
        setVisibility(8);
        this.mNotifQsContainer.setCustomizerShowing(false);
        this.mTileQueryHelper.destroyTiles();
    }

    public boolean isShown() {
        return this.isShown;
    }

    /* access modifiers changed from: private */
    public void setCustomizing(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setCustomizing=");
        sb.append(z);
        Log.d("QSCustomizer", sb.toString());
        this.mCustomizing = z;
        this.mQs.notifyCustomizeChanged();
    }

    public boolean isCustomizing() {
        return this.mCustomizing;
    }

    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            MetricsLogger.action(getContext(), 359);
            reset();
        }
        return false;
    }

    private void reset() {
        ArrayList arrayList = new ArrayList();
        for (String add : this.mContext.getString(R$string.quick_settings_tiles_default).split(",")) {
            arrayList.add(add);
        }
        this.mPageManager.resetTileSpecs(this.mHost, arrayList);
    }

    private void setTileSpecs() {
        ArrayList arrayList = new ArrayList();
        for (QSTile tileSpec : this.mHost.getTiles()) {
            arrayList.add(tileSpec.getTileSpec());
        }
        this.mPageManager.setTileSpecs(arrayList);
    }

    private void save() {
        if (this.mTileQueryHelper.isFinished()) {
            this.mPageManager.saveSpecs(this.mHost);
        }
    }

    public void saveInstanceState(Bundle bundle) {
        if (this.isShown) {
            ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardCallback);
        }
        bundle.putBoolean("qs_customizing", this.mCustomizing);
    }

    public void restoreInstanceState(Bundle bundle) {
        if (bundle.getBoolean("qs_customizing")) {
            setVisibility(0);
            addOnLayoutChangeListener(new OnLayoutChangeListener() {
                public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    QSCustomizer.this.removeOnLayoutChangeListener(this);
                    QSCustomizer.this.showImmediately();
                }
            });
        }
    }

    public void setEditLocation(int i, int i2) {
        int[] locationOnScreen = findViewById(R$id.customize_container).getLocationOnScreen();
        this.f89mX = i - locationOnScreen[0];
        this.f90mY = i2 - locationOnScreen[1];
    }

    public /* synthetic */ void lambda$new$0$QSCustomizer() {
        if (isAttachedToWindow() && ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing() && !this.mOpening) {
            hideNoAnimation();
        }
    }
}
