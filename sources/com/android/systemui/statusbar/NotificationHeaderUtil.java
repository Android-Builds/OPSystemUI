package com.android.systemui.statusbar;

import android.app.Notification;
import android.graphics.PorterDuff.Mode;
import android.text.TextUtils;
import android.view.NotificationHeaderView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationContentView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NotificationHeaderUtil {
    private static final ResultApplicator mGreyApplicator = new ResultApplicator() {
        public void apply(View view, boolean z) {
            NotificationHeaderView notificationHeaderView = (NotificationHeaderView) view;
            ImageView imageView = (ImageView) view.findViewById(16908294);
            ImageView imageView2 = (ImageView) view.findViewById(16908898);
            applyToChild(imageView, z, notificationHeaderView.getOriginalIconColor());
            applyToChild(imageView2, z, notificationHeaderView.getOriginalNotificationColor());
        }

        private void applyToChild(View view, boolean z, int i) {
            boolean z2 = true;
            if (i != 1) {
                ImageView imageView = (ImageView) view;
                imageView.getDrawable().mutate();
                if (z) {
                    if ((view.getContext().getResources().getConfiguration().uiMode & 48) != 32) {
                        z2 = false;
                    }
                    imageView.getDrawable().setColorFilter(ContrastColorUtil.resolveColor(view.getContext(), 0, z2), Mode.SRC_ATOP);
                    return;
                }
                imageView.getDrawable().setColorFilter(i, Mode.SRC_ATOP);
            }
        }
    };
    private static final IconComparator sGreyComparator = new IconComparator() {
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return !hasSameIcon(obj, obj2) || hasSameColor(obj, obj2);
        }
    };
    private static final DataExtractor sIconExtractor = new DataExtractor() {
        public Object extractData(ExpandableNotificationRow expandableNotificationRow) {
            return expandableNotificationRow.getStatusBarNotification().getNotification();
        }
    };
    private static final IconComparator sIconVisibilityComparator = new IconComparator() {
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return hasSameIcon(obj, obj2) && hasSameColor(obj, obj2);
        }
    };
    /* access modifiers changed from: private */
    public static final TextViewComparator sTextViewComparator = new TextViewComparator();
    /* access modifiers changed from: private */
    public static final VisibilityApplicator sVisibilityApplicator = new VisibilityApplicator();
    private final ArrayList<HeaderProcessor> mComparators = new ArrayList<>();
    private final HashSet<Integer> mDividers = new HashSet<>();
    private final ExpandableNotificationRow mRow;

    private interface DataExtractor {
        Object extractData(ExpandableNotificationRow expandableNotificationRow);
    }

    private static class HeaderProcessor {
        private final ResultApplicator mApplicator;
        private boolean mApply;
        private ViewComparator mComparator;
        private final DataExtractor mExtractor;
        private final int mId;
        private Object mParentData;
        private final ExpandableNotificationRow mParentRow;
        private View mParentView;

        public static HeaderProcessor forTextView(ExpandableNotificationRow expandableNotificationRow, int i) {
            HeaderProcessor headerProcessor = new HeaderProcessor(expandableNotificationRow, i, null, NotificationHeaderUtil.sTextViewComparator, NotificationHeaderUtil.sVisibilityApplicator);
            return headerProcessor;
        }

        HeaderProcessor(ExpandableNotificationRow expandableNotificationRow, int i, DataExtractor dataExtractor, ViewComparator viewComparator, ResultApplicator resultApplicator) {
            this.mId = i;
            this.mExtractor = dataExtractor;
            this.mApplicator = resultApplicator;
            this.mComparator = viewComparator;
            this.mParentRow = expandableNotificationRow;
        }

        public void init() {
            this.mParentView = this.mParentRow.getNotificationHeader().findViewById(this.mId);
            DataExtractor dataExtractor = this.mExtractor;
            this.mParentData = dataExtractor == null ? null : dataExtractor.extractData(this.mParentRow);
            this.mApply = !this.mComparator.isEmpty(this.mParentView);
        }

        public void compareToHeader(ExpandableNotificationRow expandableNotificationRow) {
            if (this.mApply) {
                NotificationHeaderView contractedNotificationHeader = expandableNotificationRow.getContractedNotificationHeader();
                if (contractedNotificationHeader != null) {
                    DataExtractor dataExtractor = this.mExtractor;
                    this.mApply = this.mComparator.compare(this.mParentView, contractedNotificationHeader.findViewById(this.mId), this.mParentData, dataExtractor == null ? null : dataExtractor.extractData(expandableNotificationRow));
                }
            }
        }

        public void apply(ExpandableNotificationRow expandableNotificationRow) {
            apply(expandableNotificationRow, false);
        }

        public void apply(ExpandableNotificationRow expandableNotificationRow, boolean z) {
            boolean z2 = this.mApply && !z;
            if (expandableNotificationRow.isSummaryWithChildren()) {
                applyToView(z2, expandableNotificationRow.getNotificationHeader());
                return;
            }
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getContractedChild());
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getHeadsUpChild());
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getExpandedChild());
        }

        private void applyToView(boolean z, View view) {
            if (view != null) {
                View findViewById = view.findViewById(this.mId);
                if (findViewById != null && !this.mComparator.isEmpty(findViewById)) {
                    this.mApplicator.apply(findViewById, z);
                }
            }
        }
    }

    private static abstract class IconComparator implements ViewComparator {
        public boolean isEmpty(View view) {
            return false;
        }

        private IconComparator() {
        }

        /* access modifiers changed from: protected */
        public boolean hasSameIcon(Object obj, Object obj2) {
            return ((Notification) obj).getSmallIcon().sameAs(((Notification) obj2).getSmallIcon());
        }

        /* access modifiers changed from: protected */
        public boolean hasSameColor(Object obj, Object obj2) {
            return ((Notification) obj).color == ((Notification) obj2).color;
        }
    }

    private interface ResultApplicator {
        void apply(View view, boolean z);
    }

    private static class TextViewComparator implements ViewComparator {
        private TextViewComparator() {
        }

        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return ((TextView) view).getText().equals(((TextView) view2).getText());
        }

        public boolean isEmpty(View view) {
            return TextUtils.isEmpty(((TextView) view).getText());
        }
    }

    private interface ViewComparator {
        boolean compare(View view, View view2, Object obj, Object obj2);

        boolean isEmpty(View view);
    }

    private static class VisibilityApplicator implements ResultApplicator {
        private VisibilityApplicator() {
        }

        public void apply(View view, boolean z) {
            view.setVisibility(z ? 8 : 0);
        }
    }

    public NotificationHeaderUtil(ExpandableNotificationRow expandableNotificationRow) {
        this.mRow = expandableNotificationRow;
        ArrayList<HeaderProcessor> arrayList = this.mComparators;
        HeaderProcessor headerProcessor = new HeaderProcessor(this.mRow, 16908294, sIconExtractor, sIconVisibilityComparator, sVisibilityApplicator);
        arrayList.add(headerProcessor);
        ArrayList<HeaderProcessor> arrayList2 = this.mComparators;
        HeaderProcessor headerProcessor2 = new HeaderProcessor(this.mRow, 16909164, sIconExtractor, sGreyComparator, mGreyApplicator);
        arrayList2.add(headerProcessor2);
        ArrayList<HeaderProcessor> arrayList3 = this.mComparators;
        HeaderProcessor headerProcessor3 = new HeaderProcessor(this.mRow, 16909258, null, new ViewComparator() {
            public boolean compare(View view, View view2, Object obj, Object obj2) {
                return view.getVisibility() != 8;
            }

            public boolean isEmpty(View view) {
                if (!(view instanceof ImageView) || ((ImageView) view).getDrawable() != null) {
                    return false;
                }
                return true;
            }
        }, sVisibilityApplicator);
        arrayList3.add(headerProcessor3);
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, 16908731));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, 16908977));
        this.mDividers.add(Integer.valueOf(16908978));
        this.mDividers.add(Integer.valueOf(16908980));
        this.mDividers.add(Integer.valueOf(16909465));
    }

    public void updateChildrenHeaderAppearance() {
        List notificationChildren = this.mRow.getNotificationChildren();
        if (notificationChildren != null) {
            for (int i = 0; i < this.mComparators.size(); i++) {
                ((HeaderProcessor) this.mComparators.get(i)).init();
            }
            for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) notificationChildren.get(i2);
                for (int i3 = 0; i3 < this.mComparators.size(); i3++) {
                    ((HeaderProcessor) this.mComparators.get(i3)).compareToHeader(expandableNotificationRow);
                }
            }
            for (int i4 = 0; i4 < notificationChildren.size(); i4++) {
                ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) notificationChildren.get(i4);
                for (int i5 = 0; i5 < this.mComparators.size(); i5++) {
                    ((HeaderProcessor) this.mComparators.get(i5)).apply(expandableNotificationRow2);
                }
                sanitizeHeaderViews(expandableNotificationRow2);
            }
        }
    }

    private void sanitizeHeaderViews(ExpandableNotificationRow expandableNotificationRow) {
        if (expandableNotificationRow.isSummaryWithChildren()) {
            sanitizeHeader(expandableNotificationRow.getNotificationHeader());
            return;
        }
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        sanitizeChild(privateLayout.getContractedChild());
        sanitizeChild(privateLayout.getHeadsUpChild());
        sanitizeChild(privateLayout.getExpandedChild());
    }

    private void sanitizeChild(View view) {
        if (view != null) {
            sanitizeHeader(view.findViewById(16909164));
        }
    }

    private void sanitizeHeader(NotificationHeaderView notificationHeaderView) {
        int i;
        boolean z;
        View view;
        boolean z2;
        if (notificationHeaderView != null) {
            int childCount = notificationHeaderView.getChildCount();
            View findViewById = notificationHeaderView.findViewById(16909461);
            int i2 = 1;
            while (true) {
                i = childCount - 1;
                if (i2 >= i) {
                    z = false;
                    break;
                }
                View childAt = notificationHeaderView.getChildAt(i2);
                if ((childAt instanceof TextView) && childAt.getVisibility() != 8 && !this.mDividers.contains(Integer.valueOf(childAt.getId())) && childAt != findViewById) {
                    z = true;
                    break;
                }
                i2++;
            }
            findViewById.setVisibility((!z || this.mRow.getStatusBarNotification().getNotification().showsTime()) ? 0 : 8);
            View view2 = null;
            int i3 = 1;
            while (i3 < i) {
                View childAt2 = notificationHeaderView.getChildAt(i3);
                if (this.mDividers.contains(Integer.valueOf(childAt2.getId()))) {
                    while (true) {
                        i3++;
                        if (i3 >= i) {
                            break;
                        }
                        view = notificationHeaderView.getChildAt(i3);
                        if (this.mDividers.contains(Integer.valueOf(view.getId()))) {
                            i3--;
                            break;
                        } else if (view.getVisibility() != 8 && (view instanceof TextView)) {
                            if (view2 != null) {
                                z2 = true;
                            }
                        }
                    }
                    view = view2;
                    z2 = false;
                    childAt2.setVisibility(z2 ? 0 : 8);
                    view2 = view;
                } else if (childAt2.getVisibility() != 8 && (childAt2 instanceof TextView)) {
                    view2 = childAt2;
                }
                i3++;
            }
        }
    }

    public void restoreNotificationHeader(ExpandableNotificationRow expandableNotificationRow) {
        for (int i = 0; i < this.mComparators.size(); i++) {
            ((HeaderProcessor) this.mComparators.get(i)).apply(expandableNotificationRow, true);
        }
        sanitizeHeaderViews(expandableNotificationRow);
    }
}
