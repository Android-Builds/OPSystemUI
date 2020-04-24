package com.android.systemui.statusbar.policy;

import android.app.Notification.Action;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.TextPaint;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.Button;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$styleable;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter.OnDismissAction;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationEntry.EditedSuggestionInfo;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartActions;
import com.android.systemui.statusbar.policy.SmartReplyView.SmartReplies;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class SmartReplyView extends ViewGroup {
    private static final Comparator<View> DECREASING_MEASURED_WIDTH_WITHOUT_PADDING_COMPARATOR = $$Lambda$SmartReplyView$UA3QkbRzztEFRlbb86djKcGIV5E.INSTANCE;
    private static final int MEASURE_SPEC_ANY_LENGTH = MeasureSpec.makeMeasureSpec(0, 0);
    private static SmartReplyConstants mConstants;
    private static KeyguardDismissUtil mKeyguardDismissUtil;
    private static NotificationRemoteInputManager mRemoteInputManager;
    private ActivityStarter mActivityStarter;
    private final BreakIterator mBreakIterator;
    private PriorityQueue<Button> mCandidateButtonQueueForSqueezing;
    private int mCurrentBackgroundColor;
    private final int mDefaultBackgroundColor;
    private final int mDefaultStrokeColor;
    private final int mDefaultTextColor;
    private final int mDefaultTextColorDarkBg;
    private final int mDoubleLineButtonPaddingHorizontal;
    private final int mHeightUpperLimit = NotificationUtils.getFontScaledHeight(this.mContext, R$dimen.smart_reply_button_max_height);
    private final double mMinStrokeContrast;
    private final int mRippleColor;
    private final int mRippleColorDarkBg;
    private final int mSingleLineButtonPaddingHorizontal;
    private final int mSingleToDoubleLineButtonWidthIncrease;
    private boolean mSmartRepliesGeneratedByAssistant = false;
    private View mSmartReplyContainer;
    private final int mSpacing;
    private final int mStrokeWidth;

    private static class DelayedOnClickListener implements OnClickListener {
        private final OnClickListener mActualListener;
        private final long mInitDelayMs;
        private final long mInitTimeMs = SystemClock.elapsedRealtime();

        DelayedOnClickListener(OnClickListener onClickListener, long j) {
            this.mActualListener = onClickListener;
            this.mInitDelayMs = j;
        }

        public void onClick(View view) {
            if (hasFinishedInitialization()) {
                this.mActualListener.onClick(view);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Accidental Smart Suggestion click registered, delay: ");
            sb.append(this.mInitDelayMs);
            Log.i("SmartReplyView", sb.toString());
        }

        private boolean hasFinishedInitialization() {
            return SystemClock.elapsedRealtime() >= this.mInitTimeMs + this.mInitDelayMs;
        }
    }

    @VisibleForTesting
    static class LayoutParams extends android.view.ViewGroup.LayoutParams {
        /* access modifiers changed from: private */
        public SmartButtonType buttonType;
        /* access modifiers changed from: private */
        public boolean show;
        /* access modifiers changed from: private */
        public int squeezeStatus;

        private LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.show = false;
            this.squeezeStatus = 0;
            this.buttonType = SmartButtonType.REPLY;
        }

        private LayoutParams(int i, int i2) {
            super(i, i2);
            this.show = false;
            this.squeezeStatus = 0;
            this.buttonType = SmartButtonType.REPLY;
        }

        /* access modifiers changed from: 0000 */
        @VisibleForTesting
        public boolean isShown() {
            return this.show;
        }
    }

    public static class SmartActions {
        public final List<Action> actions;
        public final boolean fromAssistant;

        public SmartActions(List<Action> list, boolean z) {
            this.actions = list;
            this.fromAssistant = z;
        }
    }

    private enum SmartButtonType {
        REPLY,
        ACTION
    }

    public static class SmartReplies {
        public final CharSequence[] choices;
        public final boolean fromAssistant;
        public final PendingIntent pendingIntent;
        public final RemoteInput remoteInput;

        public SmartReplies(CharSequence[] charSequenceArr, RemoteInput remoteInput2, PendingIntent pendingIntent2, boolean z) {
            this.choices = charSequenceArr;
            this.remoteInput = remoteInput2;
            this.pendingIntent = pendingIntent2;
            this.fromAssistant = z;
        }
    }

    private static class SmartSuggestionMeasures {
        int mButtonPaddingHorizontal = -1;
        int mMaxChildHeight = -1;
        int mMeasuredWidth = -1;

        SmartSuggestionMeasures(int i, int i2, int i3) {
            this.mMeasuredWidth = i;
            this.mMaxChildHeight = i2;
            this.mButtonPaddingHorizontal = i3;
        }

        public SmartSuggestionMeasures clone() {
            return new SmartSuggestionMeasures(this.mMeasuredWidth, this.mMaxChildHeight, this.mButtonPaddingHorizontal);
        }
    }

    static /* synthetic */ int lambda$static$0(View view, View view2) {
        return ((view2.getMeasuredWidth() - view2.getPaddingLeft()) - view2.getPaddingRight()) - ((view.getMeasuredWidth() - view.getPaddingLeft()) - view.getPaddingRight());
    }

    public SmartReplyView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentBackgroundColor = context.getColor(R$color.smart_reply_button_background);
        this.mDefaultBackgroundColor = this.mCurrentBackgroundColor;
        this.mDefaultTextColor = this.mContext.getColor(R$color.smart_reply_button_text);
        this.mDefaultTextColorDarkBg = this.mContext.getColor(R$color.smart_reply_button_text_dark_bg);
        this.mDefaultStrokeColor = this.mContext.getColor(R$color.smart_reply_button_stroke);
        this.mRippleColor = this.mContext.getColor(R$color.notification_ripple_untinted_color);
        this.mRippleColorDarkBg = Color.argb(Color.alpha(this.mRippleColor), 255, 255, 255);
        this.mMinStrokeContrast = ContrastColorUtil.calculateContrast(this.mDefaultStrokeColor, this.mDefaultBackgroundColor);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SmartReplyView, 0, 0);
        int indexCount = obtainStyledAttributes.getIndexCount();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < indexCount; i5++) {
            int index = obtainStyledAttributes.getIndex(i5);
            if (index == R$styleable.SmartReplyView_spacing) {
                i2 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_singleLineButtonPaddingHorizontal) {
                i3 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_doubleLineButtonPaddingHorizontal) {
                i4 = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            } else if (index == R$styleable.SmartReplyView_buttonStrokeWidth) {
                i = obtainStyledAttributes.getDimensionPixelSize(i5, 0);
            }
        }
        obtainStyledAttributes.recycle();
        this.mStrokeWidth = i;
        this.mSpacing = i2;
        this.mSingleLineButtonPaddingHorizontal = i3;
        this.mDoubleLineButtonPaddingHorizontal = i4;
        this.mSingleToDoubleLineButtonWidthIncrease = (i4 - i3) * 2;
        this.mBreakIterator = BreakIterator.getLineInstance();
        reallocateCandidateButtonQueueForSqueezing();
    }

    public int getHeightUpperLimit() {
        return this.mHeightUpperLimit;
    }

    private void reallocateCandidateButtonQueueForSqueezing() {
        this.mCandidateButtonQueueForSqueezing = new PriorityQueue<>(Math.max(getChildCount(), 1), DECREASING_MEASURED_WIDTH_WITHOUT_PADDING_COMPARATOR);
    }

    public void resetSmartSuggestions(View view) {
        this.mSmartReplyContainer = view;
        removeAllViews();
        this.mCurrentBackgroundColor = this.mDefaultBackgroundColor;
    }

    public void addPreInflatedButtons(List<Button> list) {
        for (Button addView : list) {
            addView(addView);
        }
        reallocateCandidateButtonQueueForSqueezing();
    }

    public List<Button> inflateRepliesFromRemoteInput(SmartReplies smartReplies, SmartReplyController smartReplyController, NotificationEntry notificationEntry, boolean z) {
        ArrayList arrayList = new ArrayList();
        if (!(smartReplies.remoteInput == null || smartReplies.pendingIntent == null || smartReplies.choices == null)) {
            for (int i = 0; i < smartReplies.choices.length; i++) {
                arrayList.add(inflateReplyButton(this, getContext(), i, smartReplies, smartReplyController, notificationEntry, z));
            }
            this.mSmartRepliesGeneratedByAssistant = smartReplies.fromAssistant;
        }
        return arrayList;
    }

    public List<Button> inflateSmartActions(SmartActions smartActions, SmartReplyController smartReplyController, NotificationEntry notificationEntry, HeadsUpManager headsUpManager, boolean z) {
        SmartActions smartActions2 = smartActions;
        ArrayList arrayList = new ArrayList();
        int size = smartActions2.actions.size();
        for (int i = 0; i < size; i++) {
            if (((Action) smartActions2.actions.get(i)).actionIntent != null) {
                arrayList.add(inflateActionButton(this, getContext(), i, smartActions, smartReplyController, notificationEntry, headsUpManager, z));
            }
        }
        return arrayList;
    }

    public static SmartReplyView inflate(Context context) {
        mConstants = (SmartReplyConstants) Dependency.get(SmartReplyConstants.class);
        mKeyguardDismissUtil = (KeyguardDismissUtil) Dependency.get(KeyguardDismissUtil.class);
        mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        return (SmartReplyView) LayoutInflater.from(context).inflate(R$layout.smart_reply_view, null);
    }

    @VisibleForTesting
    static Button inflateReplyButton(SmartReplyView smartReplyView, Context context, int i, SmartReplies smartReplies, SmartReplyController smartReplyController, NotificationEntry notificationEntry, boolean z) {
        SmartReplyView smartReplyView2 = smartReplyView;
        Button button = (Button) LayoutInflater.from(context).inflate(R$layout.smart_reply_button, smartReplyView, false);
        SmartReplies smartReplies2 = smartReplies;
        CharSequence charSequence = smartReplies2.choices[i];
        button.setText(charSequence);
        $$Lambda$SmartReplyView$rVuoX0krAdMy7xAwdbzCHW8AzI r0 = new OnDismissAction(smartReplies2, charSequence, i, button, smartReplyController, notificationEntry, context) {
            private final /* synthetic */ SmartReplies f$1;
            private final /* synthetic */ CharSequence f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ Button f$4;
            private final /* synthetic */ SmartReplyController f$5;
            private final /* synthetic */ NotificationEntry f$6;
            private final /* synthetic */ Context f$7;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
                this.f$6 = r7;
                this.f$7 = r8;
            }

            public final boolean onDismiss() {
                return SmartReplyView.lambda$inflateReplyButton$1(SmartReplyView.this, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
            }
        };
        OnClickListener r02 = new OnClickListener(r0) {
            private final /* synthetic */ OnDismissAction f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                SmartReplyView.mKeyguardDismissUtil.executeWhenUnlocked(this.f$1);
            }
        };
        if (z) {
            r02 = new DelayedOnClickListener(r02, mConstants.getOnClickInitDelay());
        }
        button.setOnClickListener(r02);
        button.setAccessibilityDelegate(new AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                accessibilityNodeInfo.addAction(new AccessibilityAction(16, SmartReplyView.this.getResources().getString(R$string.accessibility_send_smart_reply)));
            }
        });
        setButtonColors(button, smartReplyView2.mCurrentBackgroundColor, smartReplyView2.mDefaultStrokeColor, smartReplyView2.mDefaultTextColor, smartReplyView2.mRippleColor, smartReplyView2.mStrokeWidth);
        return button;
    }

    static /* synthetic */ boolean lambda$inflateReplyButton$1(SmartReplyView smartReplyView, SmartReplies smartReplies, CharSequence charSequence, int i, Button button, SmartReplyController smartReplyController, NotificationEntry notificationEntry, Context context) {
        SmartReplies smartReplies2 = smartReplies;
        if (mConstants.getEffectiveEditChoicesBeforeSending(smartReplies2.remoteInput.getEditChoicesBeforeSending())) {
            EditedSuggestionInfo editedSuggestionInfo = new EditedSuggestionInfo(charSequence, i);
            NotificationRemoteInputManager notificationRemoteInputManager = mRemoteInputManager;
            RemoteInput remoteInput = smartReplies2.remoteInput;
            Button button2 = button;
            notificationRemoteInputManager.activateRemoteInput(button2, new RemoteInput[]{remoteInput}, remoteInput, smartReplies2.pendingIntent, editedSuggestionInfo);
            return false;
        }
        CharSequence charSequence2 = charSequence;
        int i2 = i;
        smartReplyController.smartReplySent(notificationEntry, i, button.getText(), NotificationLogger.getNotificationLocation(notificationEntry).toMetricsEventEnum(), false);
        Bundle bundle = new Bundle();
        bundle.putString(smartReplies2.remoteInput.getResultKey(), charSequence.toString());
        Intent addFlags = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(new RemoteInput[]{smartReplies2.remoteInput}, addFlags, bundle);
        RemoteInput.setResultsSource(addFlags, 1);
        notificationEntry.setHasSentReply();
        try {
            smartReplies2.pendingIntent.send(context, 0, addFlags);
        } catch (CanceledException e) {
            Log.w("SmartReplyView", "Unable to send smart reply", e);
        }
        smartReplyView.mSmartReplyContainer.setVisibility(8);
        return false;
    }

    /* JADX WARNING: type inference failed for: r9v1, types: [android.view.View$OnClickListener] */
    /* JADX WARNING: type inference failed for: r0v10, types: [com.android.systemui.statusbar.policy.SmartReplyView$DelayedOnClickListener] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Unknown variable types count: 1 */
    @com.android.internal.annotations.VisibleForTesting
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static android.widget.Button inflateActionButton(com.android.systemui.statusbar.policy.SmartReplyView r10, android.content.Context r11, int r12, com.android.systemui.statusbar.policy.SmartReplyView.SmartActions r13, com.android.systemui.statusbar.SmartReplyController r14, com.android.systemui.statusbar.notification.collection.NotificationEntry r15, com.android.systemui.statusbar.policy.HeadsUpManager r16, boolean r17) {
        /*
            r6 = r13
            java.util.List<android.app.Notification$Action> r0 = r6.actions
            r5 = r12
            java.lang.Object r0 = r0.get(r12)
            r2 = r0
            android.app.Notification$Action r2 = (android.app.Notification.Action) r2
            android.view.LayoutInflater r0 = android.view.LayoutInflater.from(r11)
            int r1 = com.android.systemui.R$layout.smart_action_button
            r3 = 0
            r4 = r10
            android.view.View r0 = r0.inflate(r1, r10, r3)
            r8 = r0
            android.widget.Button r8 = (android.widget.Button) r8
            java.lang.CharSequence r0 = r2.title
            r8.setText(r0)
            android.graphics.drawable.Icon r0 = r2.getIcon()
            r1 = r11
            android.graphics.drawable.Drawable r0 = r0.loadDrawable(r11)
            android.content.res.Resources r1 = r11.getResources()
            int r7 = com.android.systemui.R$dimen.smart_action_button_icon_size
            int r1 = r1.getDimensionPixelSize(r7)
            r0.setBounds(r3, r3, r1, r1)
            r1 = 0
            r8.setCompoundDrawables(r0, r1, r1, r1)
            com.android.systemui.statusbar.policy.-$$Lambda$SmartReplyView$tct0o0Zp_9czv90IHtUOrdcaxl0 r9 = new com.android.systemui.statusbar.policy.-$$Lambda$SmartReplyView$tct0o0Zp_9czv90IHtUOrdcaxl0
            r0 = r9
            r1 = r10
            r3 = r14
            r4 = r15
            r7 = r16
            r0.<init>(r2, r3, r4, r5, r6, r7)
            if (r17 == 0) goto L_0x0052
            com.android.systemui.statusbar.policy.SmartReplyView$DelayedOnClickListener r0 = new com.android.systemui.statusbar.policy.SmartReplyView$DelayedOnClickListener
            com.android.systemui.statusbar.policy.SmartReplyConstants r1 = mConstants
            long r1 = r1.getOnClickInitDelay()
            r0.<init>(r9, r1)
            r9 = r0
        L_0x0052:
            r8.setOnClickListener(r9)
            android.view.ViewGroup$LayoutParams r0 = r8.getLayoutParams()
            com.android.systemui.statusbar.policy.SmartReplyView$LayoutParams r0 = (com.android.systemui.statusbar.policy.SmartReplyView.LayoutParams) r0
            com.android.systemui.statusbar.policy.SmartReplyView$SmartButtonType r1 = com.android.systemui.statusbar.policy.SmartReplyView.SmartButtonType.ACTION
            r0.buttonType = r1
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.SmartReplyView.inflateActionButton(com.android.systemui.statusbar.policy.SmartReplyView, android.content.Context, int, com.android.systemui.statusbar.policy.SmartReplyView$SmartActions, com.android.systemui.statusbar.SmartReplyController, com.android.systemui.statusbar.notification.collection.NotificationEntry, com.android.systemui.statusbar.policy.HeadsUpManager, boolean):android.widget.Button");
    }

    static /* synthetic */ void lambda$inflateActionButton$4(SmartReplyView smartReplyView, Action action, SmartReplyController smartReplyController, NotificationEntry notificationEntry, int i, SmartActions smartActions, HeadsUpManager headsUpManager, View view) {
        ActivityStarter activityStarter = smartReplyView.getActivityStarter();
        PendingIntent pendingIntent = action.actionIntent;
        $$Lambda$SmartReplyView$TA933H11Yl_oDGgX0f0ntr5xGgI r0 = new Runnable(notificationEntry, i, action, smartActions, headsUpManager) {
            private final /* synthetic */ NotificationEntry f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ Action f$3;
            private final /* synthetic */ SmartActions f$4;
            private final /* synthetic */ HeadsUpManager f$5;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
            }

            public final void run() {
                SmartReplyView.lambda$inflateActionButton$3(SmartReplyController.this, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
            }
        };
        activityStarter.startPendingIntentDismissingKeyguard(pendingIntent, r0, notificationEntry.getRow());
    }

    static /* synthetic */ void lambda$inflateActionButton$3(SmartReplyController smartReplyController, NotificationEntry notificationEntry, int i, Action action, SmartActions smartActions, HeadsUpManager headsUpManager) {
        smartReplyController.smartActionClicked(notificationEntry, i, action, smartActions.fromAssistant);
        headsUpManager.removeNotification(notificationEntry.key, true);
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(this.mContext, attributeSet);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    /* access modifiers changed from: protected */
    public android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams.width, layoutParams.height);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        Iterator it;
        int i4;
        int i5;
        int i6 = i2;
        if (MeasureSpec.getMode(i) == 0) {
            i3 = Integer.MAX_VALUE;
        } else {
            i3 = MeasureSpec.getSize(i);
        }
        resetButtonsLayoutParams();
        if (!this.mCandidateButtonQueueForSqueezing.isEmpty()) {
            Log.wtf("SmartReplyView", "Single line button queue leaked between onMeasure calls");
            this.mCandidateButtonQueueForSqueezing.clear();
        }
        SmartSuggestionMeasures smartSuggestionMeasures = new SmartSuggestionMeasures(this.mPaddingLeft + this.mPaddingRight, 0, this.mSingleLineButtonPaddingHorizontal);
        List filterActionsOrReplies = filterActionsOrReplies(SmartButtonType.ACTION);
        List<View> filterActionsOrReplies2 = filterActionsOrReplies(SmartButtonType.REPLY);
        ArrayList<Button> arrayList = new ArrayList<>(filterActionsOrReplies);
        arrayList.addAll(filterActionsOrReplies2);
        ArrayList arrayList2 = new ArrayList();
        int maxNumActions = mConstants.getMaxNumActions();
        Iterator it2 = arrayList.iterator();
        int i7 = 0;
        SmartSuggestionMeasures smartSuggestionMeasures2 = null;
        SmartSuggestionMeasures smartSuggestionMeasures3 = smartSuggestionMeasures;
        int i8 = 0;
        while (it2.hasNext()) {
            View view = (View) it2.next();
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (maxNumActions == -1 || layoutParams.buttonType != SmartButtonType.ACTION || i8 < maxNumActions) {
                i4 = maxNumActions;
                it = it2;
                view.setPadding(smartSuggestionMeasures3.mButtonPaddingHorizontal, view.getPaddingTop(), smartSuggestionMeasures3.mButtonPaddingHorizontal, view.getPaddingBottom());
                view.measure(MEASURE_SPEC_ANY_LENGTH, i6);
                arrayList2.add(view);
                Button button = (Button) view;
                int lineCount = button.getLineCount();
                if (lineCount >= 1 && lineCount <= 2) {
                    if (lineCount == 1) {
                        this.mCandidateButtonQueueForSqueezing.add(button);
                    }
                    SmartSuggestionMeasures clone = smartSuggestionMeasures3.clone();
                    if (smartSuggestionMeasures2 == null && layoutParams.buttonType == SmartButtonType.REPLY) {
                        smartSuggestionMeasures2 = smartSuggestionMeasures3.clone();
                    }
                    if (i7 == 0) {
                        i5 = 0;
                    } else {
                        i5 = this.mSpacing;
                    }
                    int measuredWidth = view.getMeasuredWidth();
                    int measuredHeight = view.getMeasuredHeight();
                    SmartSuggestionMeasures smartSuggestionMeasures4 = clone;
                    smartSuggestionMeasures3.mMeasuredWidth += i5 + measuredWidth;
                    smartSuggestionMeasures3.mMaxChildHeight = Math.max(smartSuggestionMeasures3.mMaxChildHeight, measuredHeight);
                    if (smartSuggestionMeasures3.mButtonPaddingHorizontal == this.mSingleLineButtonPaddingHorizontal && (lineCount == 2 || smartSuggestionMeasures3.mMeasuredWidth > i3)) {
                        smartSuggestionMeasures3.mMeasuredWidth += (i7 + 1) * this.mSingleToDoubleLineButtonWidthIncrease;
                        smartSuggestionMeasures3.mButtonPaddingHorizontal = this.mDoubleLineButtonPaddingHorizontal;
                    }
                    if (smartSuggestionMeasures3.mMeasuredWidth > i3) {
                        while (smartSuggestionMeasures3.mMeasuredWidth > i3 && !this.mCandidateButtonQueueForSqueezing.isEmpty()) {
                            Button button2 = (Button) this.mCandidateButtonQueueForSqueezing.poll();
                            int squeezeButton = squeezeButton(button2, i6);
                            if (squeezeButton != -1) {
                                smartSuggestionMeasures3.mMaxChildHeight = Math.max(smartSuggestionMeasures3.mMaxChildHeight, button2.getMeasuredHeight());
                                smartSuggestionMeasures3.mMeasuredWidth -= squeezeButton;
                            }
                        }
                        if (smartSuggestionMeasures3.mMeasuredWidth > i3) {
                            markButtonsWithPendingSqueezeStatusAs(3, arrayList2);
                            maxNumActions = i4;
                            it2 = it;
                            smartSuggestionMeasures3 = smartSuggestionMeasures4;
                        } else {
                            markButtonsWithPendingSqueezeStatusAs(2, arrayList2);
                        }
                    }
                    layoutParams.show = true;
                    i7++;
                    if (layoutParams.buttonType == SmartButtonType.ACTION) {
                        i8++;
                    }
                }
            } else {
                i4 = maxNumActions;
                it = it2;
            }
            maxNumActions = i4;
            it2 = it;
        }
        if (this.mSmartRepliesGeneratedByAssistant && !gotEnoughSmartReplies(filterActionsOrReplies2)) {
            for (View layoutParams2 : filterActionsOrReplies2) {
                ((LayoutParams) layoutParams2.getLayoutParams()).show = false;
            }
            smartSuggestionMeasures3 = smartSuggestionMeasures2;
        }
        this.mCandidateButtonQueueForSqueezing.clear();
        remeasureButtonsIfNecessary(smartSuggestionMeasures3.mButtonPaddingHorizontal, smartSuggestionMeasures3.mMaxChildHeight);
        int max = Math.max(getSuggestedMinimumHeight(), this.mPaddingTop + smartSuggestionMeasures3.mMaxChildHeight + this.mPaddingBottom);
        for (Button cornerRadius : arrayList) {
            setCornerRadius(cornerRadius, ((float) max) / 2.0f);
        }
        setMeasuredDimension(ViewGroup.resolveSize(Math.max(getSuggestedMinimumWidth(), smartSuggestionMeasures3.mMeasuredWidth), i), ViewGroup.resolveSize(max, i6));
    }

    private boolean gotEnoughSmartReplies(List<View> list) {
        int i = 0;
        for (View layoutParams : list) {
            if (((LayoutParams) layoutParams.getLayoutParams()).show) {
                i++;
            }
        }
        if (i == 0 || i >= mConstants.getMinNumSystemGeneratedReplies()) {
            return true;
        }
        return false;
    }

    private List<View> filterActionsOrReplies(SmartButtonType smartButtonType) {
        ArrayList arrayList = new ArrayList();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (childAt.getVisibility() == 0 && (childAt instanceof Button) && layoutParams.buttonType == smartButtonType) {
                arrayList.add(childAt);
            }
        }
        return arrayList;
    }

    private void resetButtonsLayoutParams() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i).getLayoutParams();
            layoutParams.show = false;
            layoutParams.squeezeStatus = 0;
        }
    }

    private int squeezeButton(Button button, int i) {
        int estimateOptimalSqueezedButtonTextWidth = estimateOptimalSqueezedButtonTextWidth(button);
        if (estimateOptimalSqueezedButtonTextWidth == -1) {
            return -1;
        }
        return squeezeButtonToTextWidth(button, i, estimateOptimalSqueezedButtonTextWidth);
    }

    private int estimateOptimalSqueezedButtonTextWidth(Button button) {
        String charSequence = button.getText().toString();
        TransformationMethod transformationMethod = button.getTransformationMethod();
        if (transformationMethod != null) {
            charSequence = transformationMethod.getTransformation(charSequence, button).toString();
        }
        int length = charSequence.length();
        this.mBreakIterator.setText(charSequence);
        if (this.mBreakIterator.preceding(length / 2) == -1 && this.mBreakIterator.next() == -1) {
            return -1;
        }
        TextPaint paint = button.getPaint();
        int current = this.mBreakIterator.current();
        float desiredWidth = Layout.getDesiredWidth(charSequence, 0, current, paint);
        float desiredWidth2 = Layout.getDesiredWidth(charSequence, current, length, paint);
        float max = Math.max(desiredWidth, desiredWidth2);
        int i = (desiredWidth > desiredWidth2 ? 1 : (desiredWidth == desiredWidth2 ? 0 : -1));
        if (i != 0) {
            boolean z = i > 0;
            int maxSqueezeRemeasureAttempts = mConstants.getMaxSqueezeRemeasureAttempts();
            float f = max;
            int i2 = 0;
            while (true) {
                if (i2 >= maxSqueezeRemeasureAttempts) {
                    break;
                }
                BreakIterator breakIterator = this.mBreakIterator;
                int previous = z ? breakIterator.previous() : breakIterator.next();
                if (previous != -1) {
                    float desiredWidth3 = Layout.getDesiredWidth(charSequence, 0, previous, paint);
                    float desiredWidth4 = Layout.getDesiredWidth(charSequence, previous, length, paint);
                    float max2 = Math.max(desiredWidth3, desiredWidth4);
                    if (max2 >= f) {
                        break;
                    }
                    if (!z ? desiredWidth3 >= desiredWidth4 : desiredWidth3 <= desiredWidth4) {
                        max = max2;
                        break;
                    }
                    i2++;
                    f = max2;
                } else {
                    break;
                }
            }
            max = f;
        }
        return (int) Math.ceil((double) max);
    }

    private int getLeftCompoundDrawableWidthWithPadding(Button button) {
        Drawable drawable = button.getCompoundDrawables()[0];
        if (drawable == null) {
            return 0;
        }
        return drawable.getBounds().width() + button.getCompoundDrawablePadding();
    }

    private int squeezeButtonToTextWidth(Button button, int i, int i2) {
        int measuredWidth = button.getMeasuredWidth();
        if (button.getPaddingLeft() != this.mDoubleLineButtonPaddingHorizontal) {
            measuredWidth += this.mSingleToDoubleLineButtonWidthIncrease;
        }
        button.setPadding(this.mDoubleLineButtonPaddingHorizontal, button.getPaddingTop(), this.mDoubleLineButtonPaddingHorizontal, button.getPaddingBottom());
        button.measure(MeasureSpec.makeMeasureSpec((this.mDoubleLineButtonPaddingHorizontal * 2) + i2 + getLeftCompoundDrawableWidthWithPadding(button), Integer.MIN_VALUE), i);
        int measuredWidth2 = button.getMeasuredWidth();
        LayoutParams layoutParams = (LayoutParams) button.getLayoutParams();
        if (button.getLineCount() > 2 || measuredWidth2 >= measuredWidth) {
            layoutParams.squeezeStatus = 3;
            return -1;
        }
        layoutParams.squeezeStatus = 1;
        return measuredWidth - measuredWidth2;
    }

    private void remeasureButtonsIfNecessary(int i, int i2) {
        boolean z;
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(i2, 1073741824);
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (layoutParams.show) {
                int measuredWidth = childAt.getMeasuredWidth();
                if (layoutParams.squeezeStatus == 3) {
                    measuredWidth = Integer.MAX_VALUE;
                    z = true;
                } else {
                    z = false;
                }
                if (childAt.getPaddingLeft() != i) {
                    if (measuredWidth != Integer.MAX_VALUE) {
                        if (i == this.mSingleLineButtonPaddingHorizontal) {
                            measuredWidth -= this.mSingleToDoubleLineButtonWidthIncrease;
                        } else {
                            measuredWidth += this.mSingleToDoubleLineButtonWidthIncrease;
                        }
                    }
                    childAt.setPadding(i, childAt.getPaddingTop(), i, childAt.getPaddingBottom());
                    z = true;
                }
                if (childAt.getMeasuredHeight() != i2) {
                    z = true;
                }
                if (z) {
                    childAt.measure(MeasureSpec.makeMeasureSpec(measuredWidth, Integer.MIN_VALUE), makeMeasureSpec);
                }
            }
        }
    }

    private void markButtonsWithPendingSqueezeStatusAs(int i, List<View> list) {
        for (View layoutParams : list) {
            LayoutParams layoutParams2 = (LayoutParams) layoutParams.getLayoutParams();
            if (layoutParams2.squeezeStatus == 1) {
                layoutParams2.squeezeStatus = i;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        boolean z2 = true;
        if (getLayoutDirection() != 1) {
            z2 = false;
        }
        int i5 = z2 ? (i3 - i) - this.mPaddingRight : this.mPaddingLeft;
        int childCount = getChildCount();
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = getChildAt(i6);
            if (((LayoutParams) childAt.getLayoutParams()).show) {
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                int i7 = z2 ? i5 - measuredWidth : i5;
                childAt.layout(i7, 0, i7 + measuredWidth, measuredHeight);
                int i8 = measuredWidth + this.mSpacing;
                i5 = z2 ? i5 - i8 : i5 + i8;
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View view, long j) {
        return ((LayoutParams) view.getLayoutParams()).show && super.drawChild(canvas, view, j);
    }

    public void setBackgroundTintColor(int i) {
        if (i != this.mCurrentBackgroundColor) {
            this.mCurrentBackgroundColor = i;
            boolean z = !ContrastColorUtil.isColorLight(i);
            int i2 = -16777216 | i;
            int ensureTextContrast = ContrastColorUtil.ensureTextContrast(z ? this.mDefaultTextColorDarkBg : this.mDefaultTextColor, i2, z);
            int ensureContrast = ContrastColorUtil.ensureContrast(this.mDefaultStrokeColor, i2, z, this.mMinStrokeContrast);
            int i3 = z ? this.mRippleColorDarkBg : this.mRippleColor;
            int childCount = getChildCount();
            for (int i4 = 0; i4 < childCount; i4++) {
                setButtonColors((Button) getChildAt(i4), i, ensureContrast, ensureTextContrast, i3, this.mStrokeWidth);
            }
        }
    }

    private static void setButtonColors(Button button, int i, int i2, int i3, int i4, int i5) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable) {
            Drawable mutate = background.mutate();
            RippleDrawable rippleDrawable = (RippleDrawable) mutate;
            rippleDrawable.setColor(ColorStateList.valueOf(i4));
            Drawable drawable = rippleDrawable.getDrawable(0);
            if (drawable instanceof InsetDrawable) {
                Drawable drawable2 = ((InsetDrawable) drawable).getDrawable();
                if (drawable2 instanceof GradientDrawable) {
                    GradientDrawable gradientDrawable = (GradientDrawable) drawable2;
                    gradientDrawable.setColor(i);
                    gradientDrawable.setStroke(i5, i2);
                }
            }
            button.setBackground(mutate);
        }
        button.setTextColor(i3);
    }

    private void setCornerRadius(Button button, float f) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable) {
            Drawable drawable = ((RippleDrawable) background.mutate()).getDrawable(0);
            if (drawable instanceof InsetDrawable) {
                Drawable drawable2 = ((InsetDrawable) drawable).getDrawable();
                if (drawable2 instanceof GradientDrawable) {
                    ((GradientDrawable) drawable2).setCornerRadius(f);
                }
            }
        }
    }

    private ActivityStarter getActivityStarter() {
        if (this.mActivityStarter == null) {
            this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        }
        return this.mActivityStarter;
    }
}
