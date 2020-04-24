package com.android.systemui.bubbles;

import android.app.ActivityOptions;
import android.app.ActivityView;
import android.app.ActivityView.StateCallback;
import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.app.Notification.BubbleMetadata;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StatsLog;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.R$styleable;
import com.android.systemui.recents.TriangleShape;
import com.android.systemui.statusbar.AlphaOptimizedButton;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.stack.ExpandableViewState;

public class BubbleExpandedView extends LinearLayout implements OnClickListener {
    /* access modifiers changed from: private */
    public ActivityView mActivityView;
    /* access modifiers changed from: private */
    public boolean mActivityViewReady;
    private Drawable mAppIcon;
    private String mAppName;
    /* access modifiers changed from: private */
    public BubbleController mBubbleController;
    private int mBubbleHeight;
    /* access modifiers changed from: private */
    public PendingIntent mBubbleIntent;
    /* access modifiers changed from: private */
    public NotificationEntry mEntry;
    private boolean mKeyboardVisible;
    private int mMinHeight;
    private boolean mNeedsNewHeight;
    private ExpandableNotificationRow mNotifRow;
    private INotificationManager mNotificationManagerService;
    private OnBubbleBlockedListener mOnBubbleBlockedListener;
    private PackageManager mPm;
    private ShapeDrawable mPointerDrawable;
    private int mPointerHeight;
    private int mPointerMargin;
    private View mPointerView;
    private int mPointerWidth;
    private AlphaOptimizedButton mSettingsIcon;
    private int mSettingsIconHeight;
    private BubbleStackView mStackView;
    private StateCallback mStateCallback;

    public interface OnBubbleBlockedListener {
    }

    public BubbleExpandedView(Context context) {
        this(context, null);
    }

    public BubbleExpandedView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BubbleExpandedView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public BubbleExpandedView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mActivityViewReady = false;
        this.mBubbleController = (BubbleController) Dependency.get(BubbleController.class);
        this.mStateCallback = new StateCallback() {
            public void onActivityViewReady(ActivityView activityView) {
                if (!BubbleExpandedView.this.mActivityViewReady) {
                    BubbleExpandedView.this.mActivityViewReady = true;
                    BubbleExpandedView.this.post(new Runnable(ActivityOptions.makeCustomAnimation(BubbleExpandedView.this.getContext(), 0, 0)) {
                        private final /* synthetic */ ActivityOptions f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C08101.this.lambda$onActivityViewReady$0$BubbleExpandedView$1(this.f$1);
                        }
                    });
                }
            }

            public /* synthetic */ void lambda$onActivityViewReady$0$BubbleExpandedView$1(ActivityOptions activityOptions) {
                BubbleExpandedView.this.mActivityView.startActivity(BubbleExpandedView.this.mBubbleIntent, activityOptions);
            }

            public void onActivityViewDestroyed(ActivityView activityView) {
                BubbleExpandedView.this.mActivityViewReady = false;
            }

            public void onTaskRemovalStarted(int i) {
                if (BubbleExpandedView.this.mEntry != null) {
                    BubbleExpandedView.this.post(new Runnable() {
                        public final void run() {
                            C08101.this.lambda$onTaskRemovalStarted$1$BubbleExpandedView$1();
                        }
                    });
                }
            }

            public /* synthetic */ void lambda$onTaskRemovalStarted$1$BubbleExpandedView$1() {
                BubbleExpandedView.this.mBubbleController.removeBubble(BubbleExpandedView.this.mEntry.key, 3);
            }
        };
        this.mPm = context.getPackageManager();
        this.mMinHeight = getResources().getDimensionPixelSize(R$dimen.bubble_expanded_default_height);
        this.mPointerMargin = getResources().getDimensionPixelSize(R$dimen.bubble_pointer_margin);
        try {
            this.mNotificationManagerService = Stub.asInterface(ServiceManager.getServiceOrThrow("notification"));
        } catch (ServiceNotFoundException e) {
            Log.w("BubbleExpandedView", e);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = getResources();
        this.mPointerView = findViewById(R$id.pointer_view);
        this.mPointerWidth = resources.getDimensionPixelSize(R$dimen.bubble_pointer_width);
        this.mPointerHeight = resources.getDimensionPixelSize(R$dimen.bubble_pointer_height);
        this.mPointerDrawable = new ShapeDrawable(TriangleShape.create((float) this.mPointerWidth, (float) this.mPointerHeight, true));
        this.mPointerView.setBackground(this.mPointerDrawable);
        this.mPointerView.setVisibility(8);
        this.mSettingsIconHeight = getContext().getResources().getDimensionPixelSize(R$dimen.bubble_expanded_header_height);
        this.mSettingsIcon = (AlphaOptimizedButton) findViewById(R$id.settings_button);
        this.mSettingsIcon.setOnClickListener(this);
        this.mActivityView = new ActivityView(this.mContext, null, 0, true);
        addView(this.mActivityView);
        bringChildToFront(this.mActivityView);
        bringChildToFront(this.mSettingsIcon);
        applyThemeAttrs();
        setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return BubbleExpandedView.this.lambda$onFinishInflate$0$BubbleExpandedView(view, windowInsets);
            }
        });
    }

    public /* synthetic */ WindowInsets lambda$onFinishInflate$0$BubbleExpandedView(View view, WindowInsets windowInsets) {
        this.mKeyboardVisible = windowInsets.getSystemWindowInsetBottom() - windowInsets.getStableInsetBottom() != 0;
        if (!this.mKeyboardVisible && this.mNeedsNewHeight) {
            updateHeight();
        }
        return view.onApplyWindowInsets(windowInsets);
    }

    /* access modifiers changed from: 0000 */
    public void applyThemeAttrs() {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(R$styleable.BubbleExpandedView);
        int color = obtainStyledAttributes.getColor(R$styleable.BubbleExpandedView_android_colorBackgroundFloating, -1);
        float dimension = obtainStyledAttributes.getDimension(R$styleable.BubbleExpandedView_android_dialogCornerRadius, 0.0f);
        obtainStyledAttributes.recycle();
        this.mPointerDrawable.setTint(color);
        if (ScreenDecorationsUtils.supportsRoundedCornersOnWindows(this.mContext.getResources())) {
            this.mActivityView.setCornerRadius(dimension);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mKeyboardVisible = false;
        this.mNeedsNewHeight = false;
        ActivityView activityView = this.mActivityView;
        if (activityView != null) {
            activityView.setForwardedInsets(Insets.of(0, 0, 0, 0));
        }
    }

    /* access modifiers changed from: 0000 */
    public void updateInsets(WindowInsets windowInsets) {
        if (usingActivityView()) {
            Point point = new Point();
            this.mActivityView.getContext().getDisplay().getSize(point);
            this.mActivityView.setForwardedInsets(Insets.of(0, 0, 0, Math.max(0, ((this.mActivityView.getLocationOnScreen()[1] + this.mActivityView.getHeight()) + (windowInsets.getSystemWindowInsetBottom() - windowInsets.getStableInsetBottom())) - point.y)));
        }
    }

    public void setOnBlockedListener(OnBubbleBlockedListener onBubbleBlockedListener) {
        this.mOnBubbleBlockedListener = onBubbleBlockedListener;
    }

    public void setEntry(NotificationEntry notificationEntry, BubbleStackView bubbleStackView, String str) {
        this.mStackView = bubbleStackView;
        this.mEntry = notificationEntry;
        this.mAppName = str;
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(notificationEntry.notification.getPackageName(), 795136);
            if (applicationInfo != null) {
                this.mAppIcon = this.mPm.getApplicationIcon(applicationInfo);
            }
        } catch (NameNotFoundException unused) {
        }
        if (this.mAppIcon == null) {
            this.mAppIcon = this.mPm.getDefaultActivityIcon();
        }
        applyThemeAttrs();
        showSettingsIcon();
        updateExpandedView();
    }

    public void populateExpandedView() {
        if (usingActivityView()) {
            this.mActivityView.setCallback(this.mStateCallback);
        } else {
            ViewGroup viewGroup = (ViewGroup) this.mNotifRow.getParent();
            if (viewGroup != this) {
                if (viewGroup != null) {
                    viewGroup.removeView(this.mNotifRow);
                }
                addView(this.mNotifRow, 1);
            }
        }
    }

    public void update(NotificationEntry notificationEntry) {
        if (notificationEntry.key.equals(this.mEntry.key)) {
            this.mEntry = notificationEntry;
            updateSettingsContentDescription();
            updateHeight();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Trying to update entry with different key, new entry: ");
        sb.append(notificationEntry.key);
        sb.append(" old entry: ");
        sb.append(this.mEntry.key);
        Log.w("BubbleExpandedView", sb.toString());
    }

    private void updateExpandedView() {
        this.mBubbleIntent = getBubbleIntent(this.mEntry);
        if (this.mBubbleIntent != null) {
            ExpandableNotificationRow expandableNotificationRow = this.mNotifRow;
            if (expandableNotificationRow != null) {
                removeView(expandableNotificationRow);
                this.mNotifRow = null;
            }
            this.mActivityView.setVisibility(0);
        }
        updateView();
    }

    /* access modifiers changed from: 0000 */
    public boolean performBackPressIfNeeded() {
        if (!usingActivityView()) {
            return false;
        }
        this.mActivityView.performBackPress();
        return true;
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0089  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0090  */
    /* JADX WARNING: Removed duplicated region for block: B:28:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateHeight() {
        /*
            r5 = this;
            boolean r0 = r5.usingActivityView()
            if (r0 == 0) goto L_0x009d
            com.android.systemui.statusbar.notification.collection.NotificationEntry r0 = r5.mEntry
            android.app.Notification$BubbleMetadata r0 = r0.getBubbleMetadata()
            r1 = 1
            r2 = 0
            if (r0 != 0) goto L_0x0018
            com.android.systemui.bubbles.BubbleStackView r0 = r5.mStackView
            int r0 = r0.getMaxExpandedHeight()
        L_0x0016:
            float r0 = (float) r0
            goto L_0x005e
        L_0x0018:
            int r3 = r0.getDesiredHeightResId()
            if (r3 == 0) goto L_0x0020
            r3 = r1
            goto L_0x0021
        L_0x0020:
            r3 = r2
        L_0x0021:
            if (r3 == 0) goto L_0x0041
            int r0 = r0.getDesiredHeightResId()
            com.android.systemui.statusbar.notification.collection.NotificationEntry r3 = r5.mEntry
            android.service.notification.StatusBarNotification r3 = r3.notification
            java.lang.String r3 = r3.getPackageName()
            com.android.systemui.statusbar.notification.collection.NotificationEntry r4 = r5.mEntry
            android.service.notification.StatusBarNotification r4 = r4.notification
            android.os.UserHandle r4 = r4.getUser()
            int r4 = r4.getIdentifier()
            int r0 = r5.getDimenForPackageUser(r0, r3, r4)
            float r0 = (float) r0
            goto L_0x0055
        L_0x0041:
            int r0 = r0.getDesiredHeight()
            float r0 = (float) r0
            android.content.Context r3 = r5.getContext()
            android.content.res.Resources r3 = r3.getResources()
            android.util.DisplayMetrics r3 = r3.getDisplayMetrics()
            float r3 = r3.density
            float r0 = r0 * r3
        L_0x0055:
            r3 = 0
            int r3 = (r0 > r3 ? 1 : (r0 == r3 ? 0 : -1))
            if (r3 <= 0) goto L_0x005b
            goto L_0x005e
        L_0x005b:
            int r0 = r5.mMinHeight
            goto L_0x0016
        L_0x005e:
            com.android.systemui.bubbles.BubbleStackView r3 = r5.mStackView
            int r3 = r3.getMaxExpandedHeight()
            int r4 = r5.mSettingsIconHeight
            int r3 = r3 - r4
            int r4 = r5.mPointerHeight
            int r3 = r3 - r4
            int r4 = r5.mPointerMargin
            int r3 = r3 - r4
            float r3 = (float) r3
            float r0 = java.lang.Math.min(r0, r3)
            int r3 = r5.mMinHeight
            float r3 = (float) r3
            float r0 = java.lang.Math.max(r0, r3)
            android.app.ActivityView r3 = r5.mActivityView
            android.view.ViewGroup$LayoutParams r3 = r3.getLayoutParams()
            android.widget.LinearLayout$LayoutParams r3 = (android.widget.LinearLayout.LayoutParams) r3
            int r4 = r3.height
            float r4 = (float) r4
            int r4 = (r4 > r0 ? 1 : (r4 == r0 ? 0 : -1))
            if (r4 == 0) goto L_0x0089
            goto L_0x008a
        L_0x0089:
            r1 = r2
        L_0x008a:
            r5.mNeedsNewHeight = r1
            boolean r1 = r5.mKeyboardVisible
            if (r1 != 0) goto L_0x00aa
            int r0 = (int) r0
            r3.height = r0
            r5.mBubbleHeight = r0
            android.app.ActivityView r0 = r5.mActivityView
            r0.setLayoutParams(r3)
            r5.mNeedsNewHeight = r2
            goto L_0x00aa
        L_0x009d:
            com.android.systemui.statusbar.notification.row.ExpandableNotificationRow r0 = r5.mNotifRow
            if (r0 == 0) goto L_0x00a6
            int r0 = r0.getIntrinsicHeight()
            goto L_0x00a8
        L_0x00a6:
            int r0 = r5.mMinHeight
        L_0x00a8:
            r5.mBubbleHeight = r0
        L_0x00aa:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.bubbles.BubbleExpandedView.updateHeight():void");
    }

    public void onClick(View view) {
        NotificationEntry notificationEntry = this.mEntry;
        if (notificationEntry != null) {
            notificationEntry.notification.getNotification();
            if (view.getId() == R$id.settings_button) {
                this.mStackView.collapseStack(new Runnable(getSettingsIntent(this.mEntry.notification.getPackageName(), this.mEntry.notification.getUid())) {
                    private final /* synthetic */ Intent f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        BubbleExpandedView.this.lambda$onClick$1$BubbleExpandedView(this.f$1);
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$onClick$1$BubbleExpandedView(Intent intent) {
        this.mContext.startActivityAsUser(intent, this.mEntry.notification.getUser());
        logBubbleClickEvent(this.mEntry, 9);
    }

    private void updateSettingsContentDescription() {
        this.mSettingsIcon.setContentDescription(getResources().getString(R$string.bubbles_settings_button_description, new Object[]{this.mAppName}));
    }

    /* access modifiers changed from: 0000 */
    public void showSettingsIcon() {
        updateSettingsContentDescription();
        this.mSettingsIcon.setVisibility(0);
    }

    public void updateView() {
        if (!usingActivityView() || this.mActivityView.getVisibility() != 0 || !this.mActivityView.isAttachedToWindow()) {
            ExpandableNotificationRow expandableNotificationRow = this.mNotifRow;
            if (expandableNotificationRow != null) {
                applyRowState(expandableNotificationRow);
            }
        } else {
            this.mActivityView.onLocationChanged();
        }
        updateHeight();
    }

    public void setPointerPosition(float f) {
        this.mPointerView.setTranslationX(f - (((float) this.mPointerWidth) / 2.0f));
        this.mPointerView.setVisibility(0);
    }

    public void cleanUpExpandedState() {
        removeView(this.mNotifRow);
        ActivityView activityView = this.mActivityView;
        if (activityView != null) {
            if (this.mActivityViewReady) {
                activityView.release();
            }
            removeView(this.mActivityView);
            this.mActivityView = null;
            this.mActivityViewReady = false;
        }
    }

    private boolean usingActivityView() {
        return (this.mBubbleIntent == null || this.mActivityView == null) ? false : true;
    }

    public int getVirtualDisplayId() {
        if (usingActivityView()) {
            return this.mActivityView.getVirtualDisplayId();
        }
        return -1;
    }

    private void applyRowState(ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.reset();
        expandableNotificationRow.setHeadsUp(false);
        expandableNotificationRow.resetTranslation();
        expandableNotificationRow.setOnKeyguard(false);
        expandableNotificationRow.setOnAmbient(false);
        expandableNotificationRow.setClipBottomAmount(0);
        expandableNotificationRow.setClipTopAmount(0);
        expandableNotificationRow.setContentTransformationAmount(0.0f, false);
        expandableNotificationRow.setIconsVisible(true);
        ExpandableViewState viewState = expandableNotificationRow.getViewState();
        if (viewState == null) {
            viewState = new ExpandableViewState();
        }
        viewState.height = expandableNotificationRow.getIntrinsicHeight();
        viewState.gone = false;
        viewState.hidden = false;
        viewState.dimmed = false;
        viewState.dark = false;
        viewState.alpha = 1.0f;
        viewState.notGoneIndex = -1;
        viewState.xTranslation = 0.0f;
        viewState.yTranslation = 0.0f;
        viewState.zTranslation = 0.0f;
        viewState.scaleX = 1.0f;
        viewState.scaleY = 1.0f;
        viewState.inShelf = true;
        viewState.headsUpIsVisible = false;
        viewState.applyToView(expandableNotificationRow);
    }

    private Intent getSettingsIntent(String str, int i) {
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", str);
        intent.putExtra("app_uid", i);
        intent.addFlags(134217728);
        intent.addFlags(268435456);
        intent.addFlags(536870912);
        return intent;
    }

    private PendingIntent getBubbleIntent(NotificationEntry notificationEntry) {
        BubbleMetadata bubbleMetadata = notificationEntry.notification.getNotification().getBubbleMetadata();
        if (!BubbleController.canLaunchInActivityView(this.mContext, notificationEntry) || bubbleMetadata == null) {
            return null;
        }
        return bubbleMetadata.getIntent();
    }

    private void logBubbleClickEvent(NotificationEntry notificationEntry, int i) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        String packageName = statusBarNotification.getPackageName();
        String channelId = statusBarNotification.getNotification().getChannelId();
        int id = statusBarNotification.getId();
        BubbleStackView bubbleStackView = this.mStackView;
        StatsLog.write(149, packageName, channelId, id, bubbleStackView.getBubbleIndex(bubbleStackView.getExpandedBubble()), this.mStackView.getBubbleCount(), i, this.mStackView.getNormalizedXPosition(), this.mStackView.getNormalizedYPosition(), notificationEntry.showInShadeWhenBubble(), notificationEntry.isForegroundService(), BubbleController.isForegroundApp(this.mContext, statusBarNotification.getPackageName()));
    }

    private int getDimenForPackageUser(int i, String str, int i2) {
        if (str != null) {
            if (i2 == -1) {
                i2 = 0;
            }
            try {
                return this.mPm.getResourcesForApplicationAsUser(str, i2).getDimensionPixelSize(i);
            } catch (NameNotFoundException unused) {
            } catch (NotFoundException e) {
                Log.e("BubbleExpandedView", "Couldn't find desired height res id", e);
            }
        }
        return 0;
    }
}
