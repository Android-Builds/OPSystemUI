package com.android.systemui.statusbar.notification.row;

import android.app.Notification;
import android.app.Notification.Builder;
import android.content.Context;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.RemoteViews.OnViewAppliedListener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ImageMessageConsumer;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.notification.MediaNotificationProcessor;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.InflatedSmartReplies;
import com.android.systemui.statusbar.policy.InflatedSmartReplies.SmartRepliesAndActions;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.util.Assert;
import com.oneplus.notification.OpNotificationController;
import java.util.HashMap;

public class NotificationContentInflater {
    private static final OpNotificationController sOPNotificationController = ((OpNotificationController) Dependency.get(OpNotificationController.class));
    private final ArrayMap<Integer, RemoteViews> mCachedContentViews = new ArrayMap<>();
    private InflationCallback mCallback;
    private boolean mInflateSynchronously = false;
    private int mInflationFlags = 99;
    private boolean mIsChildInGroup;
    private boolean mIsLowPriority;
    private boolean mRedactAmbient;
    private OnClickHandler mRemoteViewClickHandler;
    private final ExpandableNotificationRow mRow;
    private boolean mUsesIncreasedHeadsUpHeight;
    private boolean mUsesIncreasedHeight;

    @VisibleForTesting
    static abstract class ApplyCallback {
        public abstract RemoteViews getRemoteView();

        public abstract void setResultView(View view);

        ApplyCallback() {
        }
    }

    public static class AsyncInflationTask extends AsyncTask<Void, Void, InflationProgress> implements InflationCallback, InflationTask {
        private final ArrayMap<Integer, RemoteViews> mCachedContentViews;
        private final InflationCallback mCallback;
        private CancellationSignal mCancellationSignal;
        private final Context mContext;
        private Exception mError;
        private final boolean mInflateSynchronously;
        private final boolean mIsChildInGroup;
        private final boolean mIsLowPriority;
        private int mReInflateFlags;
        private final boolean mRedactAmbient;
        private OnClickHandler mRemoteViewClickHandler;
        private ExpandableNotificationRow mRow;
        private final StatusBarNotification mSbn;
        private final boolean mUsesIncreasedHeadsUpHeight;
        private final boolean mUsesIncreasedHeight;

        private AsyncInflationTask(StatusBarNotification statusBarNotification, boolean z, int i, ArrayMap<Integer, RemoteViews> arrayMap, ExpandableNotificationRow expandableNotificationRow, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, InflationCallback inflationCallback, OnClickHandler onClickHandler) {
            this.mRow = expandableNotificationRow;
            this.mSbn = statusBarNotification;
            this.mInflateSynchronously = z;
            this.mReInflateFlags = i;
            this.mCachedContentViews = arrayMap;
            this.mContext = this.mRow.getContext();
            this.mIsLowPriority = z2;
            this.mIsChildInGroup = z3;
            this.mUsesIncreasedHeight = z4;
            this.mUsesIncreasedHeadsUpHeight = z5;
            this.mRedactAmbient = z6;
            this.mRemoteViewClickHandler = onClickHandler;
            this.mCallback = inflationCallback;
            expandableNotificationRow.getEntry().setInflationTask(this);
        }

        @VisibleForTesting
        public int getReInflateFlags() {
            return this.mReInflateFlags;
        }

        /* access modifiers changed from: protected */
        public InflationProgress doInBackground(Void... voidArr) {
            try {
                Builder recoverBuilder = Builder.recoverBuilder(this.mContext, this.mSbn.getNotification());
                Context packageContext = this.mSbn.getPackageContext(this.mContext);
                Notification notification = this.mSbn.getNotification();
                if (notification.isMediaNotification()) {
                    new MediaNotificationProcessor(this.mContext, packageContext).processNotification(notification, recoverBuilder);
                }
                InflationProgress access$1900 = NotificationContentInflater.createRemoteViews(this.mReInflateFlags, recoverBuilder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, packageContext);
                NotificationContentInflater.inflateSmartReplyViews(access$1900, this.mReInflateFlags, this.mRow.getEntry(), this.mRow.getContext(), this.mRow.getHeadsUpManager(), this.mRow.getExistingSmartRepliesAndActions());
                return access$1900;
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(InflationProgress inflationProgress) {
            Exception exc = this.mError;
            if (exc == null) {
                this.mCancellationSignal = NotificationContentInflater.apply(this.mInflateSynchronously, inflationProgress, this.mReInflateFlags, this.mCachedContentViews, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, this);
                return;
            }
            handleError(exc);
        }

        private void handleError(Exception exc) {
            this.mRow.getEntry().onInflationTaskFinished();
            StatusBarNotification statusBarNotification = this.mRow.getStatusBarNotification();
            StringBuilder sb = new StringBuilder();
            sb.append(statusBarNotification.getPackageName());
            sb.append("/0x");
            sb.append(Integer.toHexString(statusBarNotification.getId()));
            String sb2 = sb.toString();
            StringBuilder sb3 = new StringBuilder();
            sb3.append("couldn't inflate view for notification ");
            sb3.append(sb2);
            Log.e("StatusBar", sb3.toString(), exc);
            InflationCallback inflationCallback = this.mCallback;
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Couldn't inflate contentViews");
            sb4.append(exc);
            inflationCallback.handleInflationException(statusBarNotification, new InflationException(sb4.toString()));
        }

        public void abort() {
            cancel(true);
            CancellationSignal cancellationSignal = this.mCancellationSignal;
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }

        public void supersedeTask(InflationTask inflationTask) {
            if (inflationTask instanceof AsyncInflationTask) {
                this.mReInflateFlags = ((AsyncInflationTask) inflationTask).mReInflateFlags | this.mReInflateFlags;
            }
        }

        public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
            handleError(exc);
        }

        public void onAsyncInflationFinished(NotificationEntry notificationEntry, int i) {
            this.mRow.getEntry().onInflationTaskFinished();
            this.mRow.onNotificationUpdated();
            this.mCallback.onAsyncInflationFinished(this.mRow.getEntry(), i);
            this.mRow.getImageResolver().purgeCache();
        }
    }

    public interface InflationCallback {
        void handleInflationException(StatusBarNotification statusBarNotification, Exception exc);

        void onAsyncInflationFinished(NotificationEntry notificationEntry, int i);
    }

    @VisibleForTesting
    static class InflationProgress {
        /* access modifiers changed from: private */
        public InflatedSmartReplies expandedInflatedSmartReplies;
        /* access modifiers changed from: private */
        public InflatedSmartReplies headsUpInflatedSmartReplies;
        /* access modifiers changed from: private */
        public CharSequence headsUpStatusBarText;
        /* access modifiers changed from: private */
        public CharSequence headsUpStatusBarTextPublic;
        /* access modifiers changed from: private */
        public View inflatedAmbientView;
        /* access modifiers changed from: private */
        public View inflatedContentView;
        /* access modifiers changed from: private */
        public View inflatedExpandedView;
        /* access modifiers changed from: private */
        public View inflatedHeadsUpView;
        /* access modifiers changed from: private */
        public View inflatedPublicView;
        /* access modifiers changed from: private */
        public View inflatedQuickReplyHeadsUpView;
        /* access modifiers changed from: private */
        public View inflatedQuickReplyView;
        /* access modifiers changed from: private */
        public RemoteViews newAmbientView;
        /* access modifiers changed from: private */
        public RemoteViews newContentView;
        /* access modifiers changed from: private */
        public RemoteViews newExpandedView;
        /* access modifiers changed from: private */
        public RemoteViews newHeadsUpView;
        /* access modifiers changed from: private */
        public RemoteViews newPublicView;
        @VisibleForTesting
        Context packageContext;

        InflationProgress() {
        }
    }

    public NotificationContentInflater(ExpandableNotificationRow expandableNotificationRow) {
        this.mRow = expandableNotificationRow;
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
    }

    public void setIsChildInGroup(boolean z) {
        if (z != this.mIsChildInGroup) {
            this.mIsChildInGroup = z;
            if (this.mIsLowPriority) {
                inflateNotificationViews(3);
            }
        }
    }

    public void setUsesIncreasedHeight(boolean z) {
        this.mUsesIncreasedHeight = z;
    }

    public void setUsesIncreasedHeadsUpHeight(boolean z) {
        this.mUsesIncreasedHeadsUpHeight = z;
    }

    public void setRemoteViewClickHandler(OnClickHandler onClickHandler) {
        this.mRemoteViewClickHandler = onClickHandler;
    }

    public void updateNeedsRedaction(boolean z) {
        this.mRedactAmbient = z;
        if (this.mRow.getEntry() != null) {
            int i = 8;
            if (z) {
                i = 24;
            }
            inflateNotificationViews(i);
        }
    }

    public void updateInflationFlag(int i, boolean z) {
        if (z) {
            this.mInflationFlags = i | this.mInflationFlags;
        } else if ((i & 99) == 0) {
            this.mInflationFlags = (~i) & this.mInflationFlags;
        }
    }

    @VisibleForTesting
    public void addInflationFlags(int i) {
        this.mInflationFlags = i | this.mInflationFlags;
    }

    public boolean isInflationFlagSet(int i) {
        return (this.mInflationFlags & i) != 0;
    }

    public void inflateNotificationViews() {
        inflateNotificationViews(this.mInflationFlags);
    }

    private void inflateNotificationViews(int i) {
        if (!this.mRow.isRemoved()) {
            int i2 = i & this.mInflationFlags;
            StatusBarNotification statusBarNotification = this.mRow.getEntry().notification;
            this.mRow.getImageResolver().preloadImages(statusBarNotification.getNotification());
            AsyncInflationTask asyncInflationTask = new AsyncInflationTask(statusBarNotification, this.mInflateSynchronously, i2, this.mCachedContentViews, this.mRow, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, this.mCallback, this.mRemoteViewClickHandler);
            if (this.mInflateSynchronously) {
                asyncInflationTask.onPostExecute(asyncInflationTask.doInBackground(new Void[0]));
            } else {
                asyncInflationTask.execute(new Void[0]);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public InflationProgress inflateNotificationViews(boolean z, int i, Builder builder, Context context) {
        InflationProgress createRemoteViews = createRemoteViews(i, builder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, context);
        inflateSmartReplyViews(createRemoteViews, i, this.mRow.getEntry(), this.mRow.getContext(), this.mRow.getHeadsUpManager(), this.mRow.getExistingSmartRepliesAndActions());
        apply(z, createRemoteViews, i, this.mCachedContentViews, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, null);
        return createRemoteViews;
    }

    public void freeNotificationView(int i) {
        if ((this.mInflationFlags & i) == 0) {
            if (i != 4) {
                if (i == 8) {
                    boolean isContentViewInactive = this.mRow.getPrivateLayout().isContentViewInactive(4);
                    boolean isContentViewInactive2 = this.mRow.getPublicLayout().isContentViewInactive(4);
                    if (isContentViewInactive) {
                        this.mRow.getPrivateLayout().setAmbientChild(null);
                    }
                    if (isContentViewInactive2) {
                        this.mRow.getPublicLayout().setAmbientChild(null);
                    }
                    if (isContentViewInactive && isContentViewInactive2) {
                        this.mCachedContentViews.remove(Integer.valueOf(8));
                    }
                } else if (i == 16 && this.mRow.getPublicLayout().isContentViewInactive(0)) {
                    this.mRow.getPublicLayout().setContractedChild(null);
                    this.mCachedContentViews.remove(Integer.valueOf(16));
                }
            } else if (this.mRow.getPrivateLayout().isContentViewInactive(2)) {
                this.mRow.getPrivateLayout().setHeadsUpChild(null);
                this.mCachedContentViews.remove(Integer.valueOf(4));
                this.mRow.getPrivateLayout().setHeadsUpInflatedSmartReplies(null);
            }
        }
    }

    /* access modifiers changed from: private */
    public static InflationProgress inflateSmartReplyViews(InflationProgress inflationProgress, int i, NotificationEntry notificationEntry, Context context, HeadsUpManager headsUpManager, SmartRepliesAndActions smartRepliesAndActions) {
        SmartReplyConstants smartReplyConstants = (SmartReplyConstants) Dependency.get(SmartReplyConstants.class);
        SmartReplyController smartReplyController = (SmartReplyController) Dependency.get(SmartReplyController.class);
        if (!((i & 2) == 0 || inflationProgress.newExpandedView == null)) {
            inflationProgress.expandedInflatedSmartReplies = InflatedSmartReplies.inflate(context, notificationEntry, smartReplyConstants, smartReplyController, headsUpManager, smartRepliesAndActions);
        }
        if (!((i & 4) == 0 || inflationProgress.newHeadsUpView == null)) {
            inflationProgress.headsUpInflatedSmartReplies = InflatedSmartReplies.inflate(context, notificationEntry, smartReplyConstants, smartReplyController, headsUpManager, smartRepliesAndActions);
        }
        return inflationProgress;
    }

    /* access modifiers changed from: private */
    public static InflationProgress createRemoteViews(int i, Builder builder, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, Context context) {
        RemoteViews remoteViews;
        InflationProgress inflationProgress = new InflationProgress();
        boolean z6 = z && !z2;
        if ((i & 1) != 0) {
            inflationProgress.newContentView = createContentView(builder, z6, z3);
        }
        if ((i & 2) != 0) {
            inflationProgress.newExpandedView = createExpandedView(builder, z6);
        }
        if ((i & 4) != 0) {
            inflationProgress.newHeadsUpView = builder.createHeadsUpContentView(z4);
        }
        if ((i & 16) != 0) {
            inflationProgress.newPublicView = builder.makePublicContentView();
        }
        if ((i & 8) != 0) {
            if (z5) {
                remoteViews = builder.makePublicAmbientNotification();
            } else {
                remoteViews = builder.makeAmbientNotification();
            }
            inflationProgress.newAmbientView = remoteViews;
        }
        inflationProgress.packageContext = context;
        inflationProgress.headsUpStatusBarText = builder.getHeadsUpStatusBarText(false);
        inflationProgress.headsUpStatusBarTextPublic = builder.getHeadsUpStatusBarText(true);
        return inflationProgress;
    }

    public static CancellationSignal apply(boolean z, InflationProgress inflationProgress, int i, ArrayMap<Integer, RemoteViews> arrayMap, ExpandableNotificationRow expandableNotificationRow, boolean z2, OnClickHandler onClickHandler, InflationCallback inflationCallback) {
        HashMap hashMap;
        NotificationEntry notificationEntry;
        NotificationContentView notificationContentView;
        NotificationContentView notificationContentView2;
        ArrayMap<Integer, RemoteViews> arrayMap2;
        NotificationContentView notificationContentView3;
        boolean z3;
        final InflationProgress inflationProgress2;
        NotificationContentView notificationContentView4;
        boolean z4;
        ArrayMap<Integer, RemoteViews> arrayMap3;
        final InflationProgress inflationProgress3;
        NotificationEntry notificationEntry2;
        final InflationProgress inflationProgress4 = inflationProgress;
        ArrayMap<Integer, RemoteViews> arrayMap4 = arrayMap;
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        HashMap hashMap2 = new HashMap();
        NotificationEntry entry = expandableNotificationRow.getEntry();
        boolean isOnQuickReplyList = sOPNotificationController.isOnQuickReplyList(entry.notification.getPackageName());
        if ((i & 1) != 0) {
            boolean z5 = !canReapplyRemoteView(inflationProgress.newContentView, (RemoteViews) arrayMap4.get(Integer.valueOf(1)));
            C12571 r19 = new ApplyCallback() {
                public void setResultView(View view) {
                    InflationProgress.this.inflatedContentView = view;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newContentView;
                }
            };
            notificationEntry = entry;
            hashMap = hashMap2;
            notificationContentView2 = publicLayout;
            notificationContentView = privateLayout;
            arrayMap2 = arrayMap4;
            applyRemoteView(z, inflationProgress, i, 1, arrayMap, expandableNotificationRow, z2, z5, onClickHandler, inflationCallback, privateLayout, privateLayout.getContractedChild(), privateLayout.getVisibleWrapper(0), hashMap, r19);
        } else {
            notificationEntry = entry;
            hashMap = hashMap2;
            notificationContentView2 = publicLayout;
            notificationContentView = privateLayout;
            arrayMap2 = arrayMap4;
        }
        if ((i & 2) == 0 || inflationProgress.newExpandedView == null) {
            inflationProgress2 = inflationProgress;
            notificationContentView3 = notificationContentView;
            z3 = true;
        } else {
            boolean z6 = !canReapplyRemoteView(inflationProgress.newExpandedView, (RemoteViews) arrayMap2.get(Integer.valueOf(2)));
            ArrayMap<Integer, RemoteViews> arrayMap5 = arrayMap2;
            inflationProgress2 = inflationProgress;
            NotificationContentView notificationContentView5 = notificationContentView;
            notificationContentView3 = notificationContentView5;
            z3 = true;
            applyRemoteView(z, inflationProgress, i, 2, arrayMap, expandableNotificationRow, z2, z6, onClickHandler, inflationCallback, notificationContentView5, notificationContentView.getExpandedChild(), notificationContentView5.getVisibleWrapper(1), hashMap, new ApplyCallback() {
                public void setResultView(View view) {
                    InflationProgress.this.inflatedExpandedView = view;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newExpandedView;
                }
            });
        }
        if (!((i & 4) == 0 || inflationProgress.newHeadsUpView == null)) {
            NotificationContentView notificationContentView6 = notificationContentView3;
            applyRemoteView(z, inflationProgress, i, 4, arrayMap, expandableNotificationRow, z2, !canReapplyRemoteView(inflationProgress.newHeadsUpView, (RemoteViews) arrayMap.get(Integer.valueOf(4))), onClickHandler, inflationCallback, notificationContentView6, notificationContentView3.getHeadsUpChild(), notificationContentView6.getVisibleWrapper(2), hashMap, new ApplyCallback() {
                public void setResultView(View view) {
                    InflationProgress.this.inflatedHeadsUpView = view;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newHeadsUpView;
                }
            });
        }
        if ((i & 16) != 0) {
            ArrayMap<Integer, RemoteViews> arrayMap6 = arrayMap;
            boolean z7 = !canReapplyRemoteView(inflationProgress.newPublicView, (RemoteViews) arrayMap6.get(Integer.valueOf(16)));
            C12604 r13 = new ApplyCallback() {
                public void setResultView(View view) {
                    InflationProgress.this.inflatedPublicView = view;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newPublicView;
                }
            };
            NotificationContentView notificationContentView7 = notificationContentView2;
            z4 = false;
            notificationContentView4 = notificationContentView7;
            arrayMap3 = arrayMap6;
            applyRemoteView(z, inflationProgress, i, 16, arrayMap, expandableNotificationRow, z2, z7, onClickHandler, inflationCallback, notificationContentView7, notificationContentView2.getContractedChild(), notificationContentView7.getVisibleWrapper(0), hashMap, r13);
        } else {
            arrayMap3 = arrayMap;
            notificationContentView4 = notificationContentView2;
            z4 = false;
        }
        if ((i & 8) != 0) {
            NotificationContentView notificationContentView8 = z2 ? notificationContentView4 : notificationContentView3;
            boolean z8 = (!canReapplyAmbient(expandableNotificationRow, z2) || !canReapplyRemoteView(inflationProgress.newAmbientView, (RemoteViews) arrayMap3.get(Integer.valueOf(8)))) ? z3 : z4;
            inflationProgress3 = inflationProgress;
            applyRemoteView(z, inflationProgress, i, 8, arrayMap, expandableNotificationRow, z2, z8, onClickHandler, inflationCallback, notificationContentView8, notificationContentView8.getAmbientChild(), notificationContentView8.getVisibleWrapper(4), hashMap, new ApplyCallback() {
                public void setResultView(View view) {
                    InflationProgress.this.inflatedAmbientView = view;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newAmbientView;
                }
            });
        } else {
            inflationProgress3 = inflationProgress;
        }
        if ((i & 32) == 0 || !isOnQuickReplyList) {
            notificationEntry2 = notificationEntry;
        } else {
            notificationEntry2 = notificationEntry;
            inflationProgress3.inflatedQuickReplyView = sOPNotificationController.getQuickReplyView(notificationEntry2.notification);
        }
        if ((i & 64) != 0 && isOnQuickReplyList) {
            inflationProgress3.inflatedQuickReplyHeadsUpView = sOPNotificationController.getQuickReplyView(notificationEntry2.notification);
        }
        finishIfDone(inflationProgress, i, arrayMap, hashMap, inflationCallback, expandableNotificationRow, z2);
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new OnCancelListener(hashMap) {
            private final /* synthetic */ HashMap f$0;

            {
                this.f$0 = r1;
            }

            public final void onCancel() {
                this.f$0.values().forEach($$Lambda$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
            }
        });
        return cancellationSignal;
    }

    @VisibleForTesting
    static void applyRemoteView(boolean z, InflationProgress inflationProgress, int i, int i2, ArrayMap<Integer, RemoteViews> arrayMap, ExpandableNotificationRow expandableNotificationRow, boolean z2, boolean z3, OnClickHandler onClickHandler, InflationCallback inflationCallback, NotificationContentView notificationContentView, View view, NotificationViewWrapper notificationViewWrapper, HashMap<Integer, CancellationSignal> hashMap, ApplyCallback applyCallback) {
        CancellationSignal cancellationSignal;
        InflationProgress inflationProgress2 = inflationProgress;
        OnClickHandler onClickHandler2 = onClickHandler;
        HashMap<Integer, CancellationSignal> hashMap2 = hashMap;
        RemoteViews remoteView = applyCallback.getRemoteView();
        if (z) {
            if (z3) {
                try {
                    View apply = remoteView.apply(inflationProgress2.packageContext, notificationContentView, onClickHandler2);
                    apply.setIsRootNamespace(true);
                    applyCallback.setResultView(apply);
                } catch (Exception e) {
                    handleInflationError(hashMap2, e, expandableNotificationRow.getStatusBarNotification(), inflationCallback);
                    hashMap2.put(Integer.valueOf(i2), new CancellationSignal());
                }
            } else {
                remoteView.reapply(inflationProgress2.packageContext, view, onClickHandler2);
                notificationViewWrapper.onReinflated();
            }
            return;
        }
        InflationCallback inflationCallback2 = inflationCallback;
        NotificationContentView notificationContentView2 = notificationContentView;
        View view2 = view;
        final ApplyCallback applyCallback2 = applyCallback;
        final ExpandableNotificationRow expandableNotificationRow2 = expandableNotificationRow;
        final boolean z4 = z3;
        final NotificationViewWrapper notificationViewWrapper2 = notificationViewWrapper;
        final HashMap<Integer, CancellationSignal> hashMap3 = hashMap;
        final int i3 = i2;
        final InflationProgress inflationProgress3 = inflationProgress;
        final int i4 = i;
        final ArrayMap<Integer, RemoteViews> arrayMap2 = arrayMap;
        final InflationCallback inflationCallback3 = inflationCallback;
        final boolean z5 = z2;
        RemoteViews remoteViews = remoteView;
        final View view3 = view;
        final RemoteViews remoteViews2 = remoteViews;
        final NotificationContentView notificationContentView3 = notificationContentView;
        final OnClickHandler onClickHandler3 = onClickHandler;
        C12626 r1 = new OnViewAppliedListener() {
            public void onViewInflated(View view) {
                if (view instanceof ImageMessageConsumer) {
                    ((ImageMessageConsumer) view).setImageResolver(ExpandableNotificationRow.this.getImageResolver());
                }
            }

            public void onViewApplied(View view) {
                if (z4) {
                    view.setIsRootNamespace(true);
                    applyCallback2.setResultView(view);
                } else {
                    NotificationViewWrapper notificationViewWrapper = notificationViewWrapper2;
                    if (notificationViewWrapper != null) {
                        notificationViewWrapper.onReinflated();
                    }
                }
                hashMap3.remove(Integer.valueOf(i3));
                NotificationContentInflater.finishIfDone(inflationProgress3, i4, arrayMap2, hashMap3, inflationCallback3, ExpandableNotificationRow.this, z5);
            }

            public void onError(Exception exc) {
                try {
                    View view = view3;
                    if (z4) {
                        view = remoteViews2.apply(inflationProgress3.packageContext, notificationContentView3, onClickHandler3);
                    } else {
                        remoteViews2.reapply(inflationProgress3.packageContext, view3, onClickHandler3);
                    }
                    Log.wtf("NotifContentInflater", "Async Inflation failed but normal inflation finished normally.", exc);
                    onViewApplied(view);
                } catch (Exception unused) {
                    hashMap3.remove(Integer.valueOf(i3));
                    NotificationContentInflater.handleInflationError(hashMap3, exc, ExpandableNotificationRow.this.getStatusBarNotification(), inflationCallback3);
                }
            }
        };
        if (z3) {
            cancellationSignal = remoteViews.applyAsync(inflationProgress2.packageContext, notificationContentView, null, r1, onClickHandler);
        } else {
            cancellationSignal = remoteViews.reapplyAsync(inflationProgress2.packageContext, view, null, r1, onClickHandler);
        }
        hashMap.put(Integer.valueOf(i2), cancellationSignal);
    }

    /* access modifiers changed from: private */
    public static void handleInflationError(HashMap<Integer, CancellationSignal> hashMap, Exception exc, StatusBarNotification statusBarNotification, InflationCallback inflationCallback) {
        Assert.isMainThread();
        hashMap.values().forEach($$Lambda$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
        if (inflationCallback != null) {
            inflationCallback.handleInflationException(statusBarNotification, exc);
        }
    }

    /* access modifiers changed from: private */
    public static boolean finishIfDone(InflationProgress inflationProgress, int i, ArrayMap<Integer, RemoteViews> arrayMap, HashMap<Integer, CancellationSignal> hashMap, InflationCallback inflationCallback, ExpandableNotificationRow expandableNotificationRow, boolean z) {
        Assert.isMainThread();
        NotificationEntry entry = expandableNotificationRow.getEntry();
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        boolean z2 = false;
        if (!hashMap.isEmpty()) {
            return false;
        }
        if ((i & 1) != 0) {
            if (inflationProgress.inflatedContentView != null) {
                privateLayout.setContractedChild(inflationProgress.inflatedContentView);
                arrayMap.put(Integer.valueOf(1), inflationProgress.newContentView);
            } else if (arrayMap.get(Integer.valueOf(1)) != null) {
                arrayMap.put(Integer.valueOf(1), inflationProgress.newContentView);
            }
        }
        if ((i & 2) != 0) {
            if (inflationProgress.inflatedExpandedView != null) {
                privateLayout.setExpandedChild(inflationProgress.inflatedExpandedView);
                arrayMap.put(Integer.valueOf(2), inflationProgress.newExpandedView);
            } else if (inflationProgress.newExpandedView == null) {
                privateLayout.setExpandedChild(null);
                arrayMap.put(Integer.valueOf(2), null);
            } else if (arrayMap.get(Integer.valueOf(2)) != null) {
                arrayMap.put(Integer.valueOf(2), inflationProgress.newExpandedView);
            }
            if (inflationProgress.newExpandedView != null) {
                privateLayout.setExpandedInflatedSmartReplies(inflationProgress.expandedInflatedSmartReplies);
            } else {
                privateLayout.setExpandedInflatedSmartReplies(null);
            }
            if (inflationProgress.newExpandedView != null) {
                z2 = true;
            }
            expandableNotificationRow.setExpandable(z2);
        }
        if ((i & 4) != 0) {
            if (inflationProgress.inflatedHeadsUpView != null) {
                privateLayout.setHeadsUpChild(inflationProgress.inflatedHeadsUpView);
                arrayMap.put(Integer.valueOf(4), inflationProgress.newHeadsUpView);
            } else if (inflationProgress.newHeadsUpView == null) {
                privateLayout.setHeadsUpChild(null);
                arrayMap.put(Integer.valueOf(4), null);
            } else if (arrayMap.get(Integer.valueOf(4)) != null) {
                arrayMap.put(Integer.valueOf(4), inflationProgress.newHeadsUpView);
            }
            if (inflationProgress.newHeadsUpView != null) {
                privateLayout.setHeadsUpInflatedSmartReplies(inflationProgress.headsUpInflatedSmartReplies);
            } else {
                privateLayout.setHeadsUpInflatedSmartReplies(null);
            }
        }
        if ((i & 16) != 0) {
            if (inflationProgress.inflatedPublicView != null) {
                publicLayout.setContractedChild(inflationProgress.inflatedPublicView);
                arrayMap.put(Integer.valueOf(16), inflationProgress.newPublicView);
            } else if (arrayMap.get(Integer.valueOf(16)) != null) {
                arrayMap.put(Integer.valueOf(16), inflationProgress.newPublicView);
            }
        }
        if ((i & 8) != 0) {
            if (inflationProgress.inflatedAmbientView != null) {
                NotificationContentView notificationContentView = z ? publicLayout : privateLayout;
                if (z) {
                    publicLayout = privateLayout;
                }
                notificationContentView.setAmbientChild(inflationProgress.inflatedAmbientView);
                publicLayout.setAmbientChild(null);
                arrayMap.put(Integer.valueOf(8), inflationProgress.newAmbientView);
            } else if (arrayMap.get(Integer.valueOf(8)) != null) {
                arrayMap.put(Integer.valueOf(8), inflationProgress.newAmbientView);
            }
        }
        if (!((i & 32) == 0 || inflationProgress.inflatedQuickReplyView == null)) {
            privateLayout.setQuickReplyContractedChild(inflationProgress.inflatedQuickReplyView);
        }
        if (!((i & 64) == 0 || inflationProgress.inflatedQuickReplyHeadsUpView == null)) {
            privateLayout.setQuickReplyHeadsUpChild(inflationProgress.inflatedQuickReplyHeadsUpView);
        }
        entry.headsUpStatusBarText = inflationProgress.headsUpStatusBarText;
        entry.headsUpStatusBarTextPublic = inflationProgress.headsUpStatusBarTextPublic;
        if (inflationCallback != null) {
            inflationCallback.onAsyncInflationFinished(expandableNotificationRow.getEntry(), i);
        }
        return true;
    }

    private static RemoteViews createExpandedView(Builder builder, boolean z) {
        RemoteViews createBigContentView = builder.createBigContentView();
        if (createBigContentView != null) {
            return createBigContentView;
        }
        if (!z) {
            return null;
        }
        RemoteViews createContentView = builder.createContentView();
        Builder.makeHeaderExpanded(createContentView);
        return createContentView;
    }

    private static RemoteViews createContentView(Builder builder, boolean z, boolean z2) {
        if (z) {
            return builder.makeLowPriorityContentView(false);
        }
        return builder.createContentView(z2);
    }

    @VisibleForTesting
    static boolean canReapplyRemoteView(RemoteViews remoteViews, RemoteViews remoteViews2) {
        if (remoteViews == null && remoteViews2 == null) {
            return true;
        }
        if (remoteViews == null || remoteViews2 == null || remoteViews2.getPackage() == null || remoteViews.getPackage() == null || !remoteViews.getPackage().equals(remoteViews2.getPackage()) || remoteViews.getLayoutId() != remoteViews2.getLayoutId() || remoteViews2.hasFlags(1)) {
            return false;
        }
        return true;
    }

    public void setInflationCallback(InflationCallback inflationCallback) {
        this.mCallback = inflationCallback;
    }

    public void clearCachesAndReInflate() {
        this.mCachedContentViews.clear();
        inflateNotificationViews();
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setInflateSynchronously(boolean z) {
        this.mInflateSynchronously = z;
    }

    private static boolean canReapplyAmbient(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        NotificationContentView notificationContentView;
        if (z) {
            notificationContentView = expandableNotificationRow.getPublicLayout();
        } else {
            notificationContentView = expandableNotificationRow.getPrivateLayout();
        }
        return notificationContentView.getAmbientChild() != null;
    }

    public void reinflateQuickReply() {
        inflateNotificationViews(96);
    }
}
