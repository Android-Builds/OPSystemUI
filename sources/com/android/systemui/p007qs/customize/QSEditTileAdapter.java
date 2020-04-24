package com.android.systemui.p007qs.customize;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.android.systemui.R$integer;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.customize.QSEditTileAdapter.DragListener;
import com.android.systemui.p007qs.customize.TileQueryHelper.TileInfo;
import com.android.systemui.p007qs.tileimpl.QSIconViewImpl;
import java.util.ArrayList;
import java.util.List;

/* renamed from: com.android.systemui.qs.customize.QSEditTileAdapter */
public class QSEditTileAdapter extends Adapter<TileViewHolder> {
    /* access modifiers changed from: private */
    public static int mGoToPage = 0;
    /* access modifiers changed from: private */
    public static long mLastPageTime = 0;
    /* access modifiers changed from: private */
    public static int mPositionSource = -1;
    /* access modifiers changed from: private */
    public static int mPositionTarget = -1;
    /* access modifiers changed from: private */
    public static TileInfo mSelectedItem;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    private ItemLocations mItemLocations = null;
    /* access modifiers changed from: private */
    public QSEditPageManager mManager = null;
    /* access modifiers changed from: private */
    public DragShadowBuilder mShadowBuilder = null;
    private List<TileInfo> mTiles;

    /* renamed from: com.android.systemui.qs.customize.QSEditTileAdapter$DragListener */
    public class DragListener implements OnDragListener {
        private final Runnable mScrollWorker = new Runnable() {
            public void run() {
                DragListener.this.scrollPage(QSEditTileAdapter.mGoToPage);
            }
        };

        public DragListener() {
        }

        private void moveItem() {
            List itemList = ((QSEditTileAdapter) QSEditTileAdapter.this.mManager.mSource.getAdapter()).getItemList();
            itemList.add(QSEditTileAdapter.mPositionTarget, (TileInfo) itemList.remove(QSEditTileAdapter.mPositionSource));
            QSEditTileAdapter.this.notifyItemMoved(QSEditTileAdapter.mPositionSource, QSEditTileAdapter.mPositionTarget);
            QSEditTileAdapter.mPositionSource = QSEditTileAdapter.mPositionTarget;
        }

        /* access modifiers changed from: private */
        public void scrollPage(int i) {
            QSEditTileAdapter.mLastPageTime = 0;
            if (i == 1) {
                QSEditTileAdapter.this.mManager.scrollNextPage();
            } else {
                QSEditTileAdapter.this.mManager.scrollPrevPage();
            }
        }

        private void checkIfDragToPage(int i) {
            if (i == 0) {
                QSEditTileAdapter.mLastPageTime = 0;
                QSEditTileAdapter.this.mHandler.removeCallbacks(this.mScrollWorker);
                return;
            }
            boolean z = true;
            if ((i != 1 || !QSEditTileAdapter.this.mManager.canScrollToNextPage()) && i != 2) {
                z = false;
            }
            if (QSEditTileAdapter.mLastPageTime == 0 && z) {
                QSEditTileAdapter.mLastPageTime = SystemClock.uptimeMillis();
                QSEditTileAdapter.mGoToPage = i;
                QSEditTileAdapter.this.mHandler.removeCallbacks(this.mScrollWorker);
                QSEditTileAdapter.this.mHandler.postDelayed(this.mScrollWorker, 1000);
            }
        }

        public boolean onDrag(View view, DragEvent dragEvent) {
            int action = dragEvent.getAction();
            View view2 = (View) dragEvent.getLocalState();
            if (view instanceof RecyclerView) {
                QSEditTileAdapter.this.mManager.mTarget = (RecyclerView) view;
                if (QSEditTileAdapter.this.mManager.mSource == QSEditTileAdapter.this.mManager.mTarget || QSEditTileAdapter.this.mManager.isPageFull(QSEditTileAdapter.this.mManager.mTarget)) {
                    if (action == 2) {
                        checkIfDragToPage(QSEditTileAdapter.this.getTargetPage(view, dragEvent));
                    }
                    return true;
                }
                QSEditTileAdapter.mPositionTarget = QSEditTileAdapter.this.getIndexInPage(view, dragEvent);
            } else {
                QSEditTileAdapter.this.mManager.mTarget = (RecyclerView) view.getParent();
                QSEditTileAdapter.mPositionTarget = ((TileViewHolder) view.getTag()).getAdapterPosition();
            }
            String str = "QSEditTileAdapter";
            if (QSEditTileAdapter.this.mManager.isInLowerPage(QSEditTileAdapter.this.mManager.mSource) && QSEditTileAdapter.this.mManager.isInLowerPage(QSEditTileAdapter.this.mManager.mTarget)) {
                Log.d(str, "from lower to lower, skip it");
                return true;
            } else if (!QSEditTileAdapter.this.mManager.canRemoveTile() && QSEditTileAdapter.this.mManager.mSource != QSEditTileAdapter.this.mManager.mTarget) {
                return true;
            } else {
                switch (action) {
                    case 2:
                        if (QSEditTileAdapter.this.mManager.mSource != QSEditTileAdapter.this.mManager.mTarget) {
                            if (!(QSEditTileAdapter.mPositionSource == -1 || QSEditTileAdapter.mPositionTarget == -1)) {
                                boolean z = QSEditTileAdapter.this.mManager.isInUpperPage(QSEditTileAdapter.this.mManager.mSource) != QSEditTileAdapter.this.mManager.isInUpperPage(QSEditTileAdapter.this.mManager.mTarget);
                                if (!z && QSEditTileAdapter.this.mManager.isPageFull(QSEditTileAdapter.this.mManager.mSource) && QSEditTileAdapter.this.mManager.isPageEmpty(QSEditTileAdapter.this.mManager.mTarget)) {
                                    Log.d(str, "Moving item in fulled page to empty page, skip it.");
                                    break;
                                } else {
                                    QSEditTileAdapter qSEditTileAdapter = (QSEditTileAdapter) QSEditTileAdapter.this.mManager.mSource.getAdapter();
                                    QSEditTileAdapter qSEditTileAdapter2 = (QSEditTileAdapter) QSEditTileAdapter.this.mManager.mTarget.getAdapter();
                                    if (z) {
                                        QSEditTileAdapter.this.mManager.onBeforeItemAdded(QSEditTileAdapter.this.mManager.mTarget);
                                        QSEditTileAdapter.this.mManager.onBeforeItemRemoved(QSEditTileAdapter.this.mManager.mSource);
                                    }
                                    TileInfo tileInfo = (TileInfo) qSEditTileAdapter.getItemList().remove(QSEditTileAdapter.mPositionSource);
                                    qSEditTileAdapter.notifyItemRemoved(QSEditTileAdapter.mPositionSource);
                                    List itemList = qSEditTileAdapter2.getItemList();
                                    if (QSEditTileAdapter.mPositionTarget > itemList.size()) {
                                        QSEditTileAdapter.mPositionTarget = itemList.size();
                                    }
                                    itemList.add(QSEditTileAdapter.mPositionTarget, tileInfo);
                                    qSEditTileAdapter2.notifyItemInserted(QSEditTileAdapter.mPositionTarget);
                                    if (z) {
                                        QSEditTileAdapter.this.mHandler.post(new Runnable(QSEditTileAdapter.this.mManager.mSource) {
                                            private final /* synthetic */ RecyclerView f$1;

                                            {
                                                this.f$1 = r2;
                                            }

                                            public final void run() {
                                                DragListener.this.lambda$onDrag$0$QSEditTileAdapter$DragListener(this.f$1);
                                            }
                                        });
                                        QSEditTileAdapter.mPositionSource = QSEditTileAdapter.mPositionTarget;
                                    } else if (QSEditTileAdapter.this.mManager.rebuildPager(QSEditTileAdapter.this.mManager.mTarget) == 1) {
                                        QSEditTileAdapter.mPositionSource = QSEditTileAdapter.mPositionTarget - 1;
                                    } else {
                                        QSEditTileAdapter.mPositionSource = QSEditTileAdapter.mPositionTarget;
                                    }
                                    QSEditTileAdapter.this.mManager.mSource = QSEditTileAdapter.this.mManager.mTarget;
                                    break;
                                }
                            }
                        } else if (QSEditTileAdapter.this.mManager.isInUpperPage(QSEditTileAdapter.this.mManager.mSource)) {
                            checkIfDragToPage(QSEditTileAdapter.this.getTargetPage(view, dragEvent));
                            if (!(QSEditTileAdapter.mPositionSource == QSEditTileAdapter.this.getIndexInPage(view, dragEvent) || QSEditTileAdapter.mPositionTarget == QSEditTileAdapter.mPositionSource || QSEditTileAdapter.mPositionSource == -1 || QSEditTileAdapter.mPositionTarget == -1)) {
                                moveItem();
                                break;
                            }
                        }
                        break;
                    case 3:
                    case 4:
                        if (!QSEditTileAdapter.mSelectedItem.isVisible) {
                            QSEditTileAdapter.mSelectedItem.isVisible = true;
                        }
                        QSEditTileAdapter.mLastPageTime = 0;
                        QSEditTileAdapter.this.mHandler.removeCallbacks(this.mScrollWorker);
                        QSEditTileAdapter.this.mManager.endDragAndDrop();
                        QSEditTileAdapter.this.mHandler.post(new Runnable() {
                            public final void run() {
                                DragListener.this.lambda$onDrag$1$QSEditTileAdapter$DragListener();
                            }
                        });
                        break;
                    case 5:
                        if (QSEditTileAdapter.this.mManager.mSource == QSEditTileAdapter.this.mManager.mTarget && QSEditTileAdapter.this.mManager.isInUpperPage(QSEditTileAdapter.this.mManager.mSource)) {
                            int access$1100 = QSEditTileAdapter.this.getIndexInPage(view, dragEvent);
                            if (!(QSEditTileAdapter.mPositionSource == -1 || QSEditTileAdapter.mPositionTarget == -1 || QSEditTileAdapter.mPositionSource == access$1100 || QSEditTileAdapter.mPositionTarget == QSEditTileAdapter.mPositionSource)) {
                                moveItem();
                                break;
                            }
                        }
                }
                return true;
            }
        }

        public /* synthetic */ void lambda$onDrag$0$QSEditTileAdapter$DragListener(RecyclerView recyclerView) {
            QSEditTileAdapter.this.mManager.onAfterItemRemoved(recyclerView);
        }

        public /* synthetic */ void lambda$onDrag$1$QSEditTileAdapter$DragListener() {
            Log.d("QSEditTileAdapter", "post view changing event");
            QSEditTileAdapter.this.mManager.onAfterItemAdded(QSEditTileAdapter.this.mManager.mSource);
            ((QSEditTileAdapter) QSEditTileAdapter.this.mManager.mSource.getAdapter()).notifyItemChanged(QSEditTileAdapter.mPositionSource);
        }
    }

    /* renamed from: com.android.systemui.qs.customize.QSEditTileAdapter$TileViewHolder */
    public class TileViewHolder extends ViewHolder {
        /* access modifiers changed from: private */
        public TileInfo mData = null;
        /* access modifiers changed from: private */
        public CustomizeTileView mTileView;

        public TileViewHolder(View view) {
            super(view);
            if (view instanceof CustomizeTileView) {
                this.mTileView = (CustomizeTileView) view;
                this.mTileView.setBackground(null);
                this.mTileView.getIcon().disableAnimation();
            }
        }

        public void setVisible(boolean z) {
            TileInfo tileInfo = this.mData;
            if (tileInfo != null) {
                tileInfo.isVisible = z;
                if (z) {
                    this.mTileView.setVisibility(0);
                } else {
                    this.mTileView.setVisibility(4);
                }
            }
        }

        public void setData(TileInfo tileInfo) {
            this.mData = tileInfo;
            setVisible(this.mData.isVisible);
        }
    }

    public QSEditTileAdapter(List<TileInfo> list, ItemLocations itemLocations, QSEditPageManager qSEditPageManager) {
        this.mTiles = list;
        if (this.mTiles == null) {
            this.mTiles = new ArrayList();
        }
        this.mItemLocations = itemLocations;
        this.mManager = qSEditPageManager;
    }

    public TileViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        return new TileViewHolder(new CustomizeTileView(context, new QSIconViewImpl(context)));
    }

    public void onBindViewHolder(TileViewHolder tileViewHolder, int i) {
        TileInfo tileInfo = (TileInfo) this.mTiles.get(i);
        tileViewHolder.mTileView.onStateChanged(tileInfo.state);
        tileViewHolder.mTileView.setShowAppLabel(!tileInfo.isSystem);
        tileViewHolder.mTileView.setTag(tileViewHolder);
        tileViewHolder.setData(tileInfo);
        tileViewHolder.mTileView.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View view) {
                String str;
                String str2 = "";
                ClipData newPlainText = ClipData.newPlainText(str2, str2);
                QSEditTileAdapter.this.mShadowBuilder = new DragShadowBuilder(view);
                view.startDrag(newPlainText, QSEditTileAdapter.this.mShadowBuilder, view, 0);
                TileViewHolder tileViewHolder = (TileViewHolder) view.getTag();
                tileViewHolder.setVisible(false);
                QSEditTileAdapter.mSelectedItem = tileViewHolder.mData;
                QSEditTileAdapter.this.mManager.mSource = (RecyclerView) view.getParent();
                QSEditTileAdapter.mPositionSource = ((TileViewHolder) view.getTag()).getAdapterPosition();
                QSEditTileAdapter.this.mManager.beginDragAndDrop(QSEditTileAdapter.this.mManager.mSource);
                Resources resources = view.getContext().getResources();
                int integer = resources.getInteger(R$integer.quick_settings_min_num_tiles);
                if (QSEditTileAdapter.this.mManager.canRemoveTile()) {
                    str = resources.getString(R$string.drag_to_remove_tiles);
                } else {
                    str = resources.getString(R$string.drag_to_remove_disabled, new Object[]{Integer.valueOf(integer)});
                }
                QSEditTileAdapter.this.mManager.getDragLabel().setText(str);
                return true;
            }
        });
        tileViewHolder.mTileView.setOnDragListener(getDragInstance());
    }

    public ItemLocations getItemLocations() {
        return this.mItemLocations;
    }

    /* access modifiers changed from: private */
    public int getIndexInPage(View view, DragEvent dragEvent) {
        Point touchPositionFromDragEvent = getTouchPositionFromDragEvent(view, dragEvent);
        int positionIndex = this.mItemLocations.getPositionIndex(touchPositionFromDragEvent.x, touchPositionFromDragEvent.y);
        if (positionIndex >= 0) {
            return Math.min(positionIndex, getItemCount());
        }
        return getItemCount() - 1;
    }

    /* access modifiers changed from: private */
    public int getTargetPage(View view, DragEvent dragEvent) {
        Point touchPositionFromDragEvent = getTouchPositionFromDragEvent(view, dragEvent);
        if (this.mItemLocations.isGoingToNextPage(touchPositionFromDragEvent.x)) {
            return 1;
        }
        return this.mItemLocations.isGoingToPrevPage(touchPositionFromDragEvent.x) ? 2 : 0;
    }

    public boolean isPageEmpty() {
        return getItemCount() == 0;
    }

    public boolean isPageFull() {
        return getItemCount() == this.mItemLocations.getMaxItems();
    }

    public boolean isPageMoreThanFull() {
        return getItemCount() > this.mItemLocations.getMaxItems();
    }

    private Point getTouchPositionFromDragEvent(View view, DragEvent dragEvent) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        return new Point(rect.left + Math.round(dragEvent.getX()), rect.top + Math.round(dragEvent.getY()));
    }

    public DragListener getDragInstance() {
        return new DragListener();
    }

    public int getItemCount() {
        return this.mTiles.size();
    }

    public List<TileInfo> getItemList() {
        return this.mTiles;
    }
}
