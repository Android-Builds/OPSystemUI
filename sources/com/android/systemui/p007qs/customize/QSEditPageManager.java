package com.android.systemui.p007qs.customize;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import com.android.systemui.R$dimen;
import com.android.systemui.R$integer;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSTileHost;
import com.android.systemui.p007qs.customize.TileQueryHelper.TileInfo;
import com.android.systemui.p007qs.customize.TileQueryHelper.TileStateListener;
import java.util.ArrayList;
import java.util.List;

/* renamed from: com.android.systemui.qs.customize.QSEditPageManager */
public class QSEditPageManager implements TileStateListener {
    private List<TileInfo> mAllTiles;
    private boolean mCanRemoveTile = true;
    /* access modifiers changed from: private */
    public Context mContext;
    private List<String> mCurrentSpecs;
    TextView mDragLabel;
    private QSTileHost mHost;
    private ItemLocations mLowerLocations;
    private QSEditViewPager mLowerPager;
    private List<TileInfo> mLowerTiles = new ArrayList();
    RecyclerView mSource = null;
    RecyclerView mTarget = null;
    private ItemLocations mUpperLocations;
    private QSEditViewPager mUpperPager;
    private List<TileInfo> mUpperTiles = new ArrayList();

    public QSEditPageManager(Context context, QSEditViewPager qSEditViewPager, QSEditViewPager qSEditViewPager2, TextView textView) {
        this.mContext = context;
        this.mUpperPager = qSEditViewPager;
        this.mLowerPager = qSEditViewPager2;
        this.mDragLabel = textView;
        reloadResources();
    }

    private void reloadResources() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_edit_tile_width);
        int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_edit_tile_height);
        int dimensionPixelSize3 = this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_edit_tile_margin);
        int integer = this.mContext.getResources().getInteger(R$integer.quick_settings_num_columns);
        int integer2 = this.mContext.getResources().getInteger(R$integer.quick_settings_max_rows);
        int integer3 = this.mContext.getResources().getInteger(R$integer.quick_settings_num_rows_lower);
        StringBuilder sb = new StringBuilder();
        sb.append("rows=");
        sb.append(integer2);
        sb.append(", lower_rows=");
        sb.append(integer3);
        Log.d("QSEditPageManager", sb.toString());
        int i = dimensionPixelSize;
        int i2 = dimensionPixelSize2;
        int i3 = dimensionPixelSize3;
        int i4 = integer;
        ItemLocations itemLocations = new ItemLocations(i, i2, i3, i4, integer2);
        this.mUpperLocations = itemLocations;
        ItemLocations itemLocations2 = new ItemLocations(i, i2, i3, i4, integer3);
        this.mLowerLocations = itemLocations2;
    }

    public void recalcEditPage() {
        reloadResources();
        recalcSpecs();
    }

    public void setLayoutRTL(boolean z) {
        this.mUpperLocations.setLayoutRTL(z);
        this.mLowerLocations.setLayoutRTL(z);
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<TileInfo> list, ItemLocations itemLocations, QSEditViewPager qSEditViewPager) {
        QSEditTileAdapter qSEditTileAdapter = new QSEditTileAdapter(list, itemLocations, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this.mContext, itemLocations.getColumns()) {
            public boolean canScrollVertically() {
                return false;
            }

            public LayoutParams generateDefaultLayoutParams() {
                LayoutParams generateDefaultLayoutParams = super.generateDefaultLayoutParams();
                generateDefaultLayoutParams.bottomMargin = QSEditPageManager.this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_tile_margin_row);
                generateDefaultLayoutParams.rightMargin = QSEditPageManager.this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_edit_tile_margin);
                generateDefaultLayoutParams.leftMargin = QSEditPageManager.this.mContext.getResources().getDimensionPixelSize(R$dimen.qs_edit_tile_margin);
                return generateDefaultLayoutParams;
            }
        });
        recyclerView.setAdapter(qSEditTileAdapter);
        recyclerView.setOnDragListener(qSEditTileAdapter.getDragInstance());
        addPage(qSEditViewPager, recyclerView);
    }

    public void beginDragAndDrop(RecyclerView recyclerView) {
        boolean isInUpperPage = isInUpperPage(recyclerView);
        int itemCount = ((QSEditTileAdapter) recyclerView.getAdapter()).getItemCount();
        int count = this.mUpperPager.getAdapter().getCount();
        String str = "QSEditPageManager";
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("beginDragAndDrop isUpperPage: ");
            sb.append(isInUpperPage);
            sb.append("itemCount: ");
            sb.append(itemCount);
            sb.append(" pageCount: ");
            sb.append(count);
            Log.d(str, sb.toString());
        }
        if (!isInUpperPage || itemCount > 6 || count >= 2) {
            this.mCanRemoveTile = true;
        } else {
            this.mCanRemoveTile = false;
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("beginDragAndDrop mCanRemoveTile: ");
            sb2.append(this.mCanRemoveTile);
            Log.d(str, sb2.toString());
        }
    }

    public boolean canRemoveTile() {
        return this.mCanRemoveTile;
    }

    public TextView getDragLabel() {
        return this.mDragLabel;
    }

    public void endDragAndDrop() {
        this.mDragLabel.setText(this.mContext.getResources().getString(R$string.drag_to_add_tiles));
    }

    public boolean canScrollToNextPage() {
        if (!isPageEmpty(getPage(this.mUpperPager, this.mUpperPager.getCurrentItem() + 1))) {
            return true;
        }
        return false;
    }

    public void scrollNextPage() {
        int currentItem = this.mUpperPager.getCurrentItem();
        if (currentItem < this.mUpperPager.getAdapter().getCount()) {
            this.mUpperPager.setCurrentItem(currentItem + 1, true);
        }
    }

    public void scrollPrevPage() {
        int currentItem = this.mUpperPager.getCurrentItem();
        if (currentItem > 0) {
            this.mUpperPager.setCurrentItem(currentItem - 1, true);
        }
    }

    private void addPage(QSEditViewPager qSEditViewPager, RecyclerView recyclerView) {
        ((QSEditPageAdapter) qSEditViewPager.getAdapter()).addPage(recyclerView);
        qSEditViewPager.updateIndicator();
    }

    private void removePage(QSEditViewPager qSEditViewPager, int i) {
        ((QSEditPageAdapter) qSEditViewPager.getAdapter()).removePage(i);
        qSEditViewPager.updateIndicator();
    }

    private RecyclerView getPage(QSEditViewPager qSEditViewPager, int i) {
        return ((QSEditPageAdapter) qSEditViewPager.getAdapter()).getPage(i);
    }

    private QSEditViewPager getPager(RecyclerView recyclerView) {
        if (isInUpperPage(recyclerView)) {
            return this.mUpperPager;
        }
        return this.mLowerPager;
    }

    private int getPageCount(QSEditViewPager qSEditViewPager) {
        return qSEditViewPager.getAdapter().getCount();
    }

    public boolean isInUpperPage(RecyclerView recyclerView) {
        return ((QSEditPageAdapter) this.mUpperPager.getAdapter()).containsPage(recyclerView);
    }

    public boolean isInLowerPage(RecyclerView recyclerView) {
        return ((QSEditPageAdapter) this.mLowerPager.getAdapter()).containsPage(recyclerView);
    }

    public boolean isPageEmpty(RecyclerView recyclerView) {
        return ((QSEditTileAdapter) recyclerView.getAdapter()).isPageEmpty();
    }

    public boolean isPageFull(RecyclerView recyclerView) {
        return ((QSEditTileAdapter) recyclerView.getAdapter()).isPageFull();
    }

    public boolean isPageMoreThanFull(RecyclerView recyclerView) {
        return ((QSEditTileAdapter) recyclerView.getAdapter()).isPageMoreThanFull();
    }

    public ItemLocations getItemLocations(RecyclerView recyclerView) {
        return ((QSEditTileAdapter) recyclerView.getAdapter()).getItemLocations();
    }

    private void fillPager(QSEditViewPager qSEditViewPager, List<TileInfo> list, ItemLocations itemLocations) {
        int maxItems = itemLocations.getMaxItems();
        int ceil = (int) Math.ceil(((double) list.size()) / ((double) maxItems));
        qSEditViewPager.setAdapter(new QSEditPageAdapter());
        for (int i = 0; i < ceil; i++) {
            RecyclerView recyclerView = new RecyclerView(this.mContext);
            recyclerView.setTag(Integer.valueOf(i));
            int i2 = i * maxItems;
            int i3 = i2 + maxItems;
            int min = Math.min(i3, list.size());
            setupRecyclerView(recyclerView, new ArrayList(list.subList(i2, min)), itemLocations, qSEditViewPager);
            if (i == ceil - 1 && min == i3) {
                RecyclerView recyclerView2 = new RecyclerView(this.mContext);
                recyclerView2.setTag(Integer.valueOf(ceil));
                setupRecyclerView(recyclerView2, null, itemLocations, qSEditViewPager);
            }
        }
        if (ceil == 0) {
            RecyclerView recyclerView3 = new RecyclerView(this.mContext);
            recyclerView3.setTag(Integer.valueOf(0));
            setupRecyclerView(recyclerView3, new ArrayList(), itemLocations, qSEditViewPager);
        }
        qSEditViewPager.setCurrentItem(0);
    }

    public void onBeforeItemAdded(RecyclerView recyclerView) {
        QSEditViewPager pager = getPager(recyclerView);
        ItemLocations itemLocations = getItemLocations(recyclerView);
        StringBuilder sb = new StringBuilder();
        sb.append("onBeforeItemAdded:items=");
        sb.append(((QSEditTileAdapter) recyclerView.getAdapter()).getItemCount());
        String str = "QSEditPageManager";
        Log.d(str, sb.toString());
        if (isPageFull(recyclerView)) {
            Log.d(str, "onBeforeItemAdded:page is full, move last item to the next page");
            int intValue = ((Integer) recyclerView.getTag()).intValue() + 1;
            for (int i = intValue; i < getPageCount(pager); i++) {
                RecyclerView page = getPage(pager, i);
                QSEditTileAdapter qSEditTileAdapter = (QSEditTileAdapter) getPage(pager, i - 1).getAdapter();
                TileInfo tileInfo = null;
                if (i == intValue) {
                    tileInfo = (TileInfo) qSEditTileAdapter.getItemList().remove(itemLocations.getMaxItems() - 1);
                    qSEditTileAdapter.notifyItemRemoved(itemLocations.getMaxItems() - 1);
                } else if (qSEditTileAdapter.getItemCount() > itemLocations.getMaxItems()) {
                    tileInfo = (TileInfo) qSEditTileAdapter.getItemList().remove(itemLocations.getMaxItems());
                    qSEditTileAdapter.notifyItemRemoved(itemLocations.getMaxItems());
                }
                QSEditTileAdapter qSEditTileAdapter2 = (QSEditTileAdapter) page.getAdapter();
                if (tileInfo != null) {
                    qSEditTileAdapter2.getItemList().add(0, tileInfo);
                    qSEditTileAdapter2.notifyItemInserted(0);
                }
            }
        }
    }

    public void onBeforeItemRemoved(RecyclerView recyclerView) {
        QSEditViewPager pager = getPager(recyclerView);
        if (isPageFull(recyclerView)) {
            int intValue = ((Integer) recyclerView.getTag()).intValue();
            if (intValue < getPageCount(pager) - 1) {
                while (true) {
                    intValue++;
                    if (intValue < getPageCount(pager)) {
                        RecyclerView page = getPage(pager, intValue);
                        QSEditTileAdapter qSEditTileAdapter = (QSEditTileAdapter) page.getAdapter();
                        QSEditTileAdapter qSEditTileAdapter2 = (QSEditTileAdapter) getPage(pager, intValue - 1).getAdapter();
                        if (qSEditTileAdapter.getItemCount() > 0) {
                            TileInfo tileInfo = (TileInfo) qSEditTileAdapter.getItemList().remove(0);
                            qSEditTileAdapter.notifyItemRemoved(0);
                            qSEditTileAdapter2.getItemList().add(tileInfo);
                            qSEditTileAdapter2.notifyItemInserted(qSEditTileAdapter2.getItemList().size() - 1);
                        }
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void onAfterItemAdded(RecyclerView recyclerView) {
        QSEditViewPager pager = getPager(recyclerView);
        ItemLocations itemLocations = getItemLocations(recyclerView);
        RecyclerView page = getPage(pager, getPageCount(pager) - 1);
        StringBuilder sb = new StringBuilder();
        sb.append("onAfterItemAdded:items=");
        sb.append(((QSEditTileAdapter) page.getAdapter()).getItemCount());
        String str = "QSEditPageManager";
        Log.d(str, sb.toString());
        if (isPageFull(page)) {
            Log.d(str, "onAfterItemAdded:page is full, add an empty page");
            RecyclerView recyclerView2 = new RecyclerView(this.mContext);
            recyclerView2.setTag(Integer.valueOf(getPageCount(pager)));
            setupRecyclerView(recyclerView2, null, itemLocations, pager);
            pager.getAdapter().notifyDataSetChanged();
        }
    }

    public void onAfterItemRemoved(RecyclerView recyclerView) {
        QSEditViewPager pager = getPager(recyclerView);
        if (getPageCount(pager) >= 2) {
            RecyclerView page = getPage(pager, getPageCount(pager) - 1);
            RecyclerView page2 = getPage(pager, getPageCount(pager) - 2);
            if (page.getAdapter().getItemCount() == 0 && !isPageFull(page2)) {
                Log.d("QSEditPageManager", "onAfterItemRemoved:remove the empty page");
                removePage(pager, getPageCount(pager) - 1);
                pager.getAdapter().notifyDataSetChanged();
            }
        }
    }

    public int rebuildPager(RecyclerView recyclerView) {
        QSEditViewPager pager = getPager(recyclerView);
        ItemLocations itemLocations = getItemLocations(recyclerView);
        int pageCount = getPageCount(pager);
        int i = 0;
        int i2 = 0;
        while (i < pageCount - 1) {
            RecyclerView page = getPage(pager, i);
            QSEditTileAdapter qSEditTileAdapter = (QSEditTileAdapter) page.getAdapter();
            i++;
            QSEditTileAdapter qSEditTileAdapter2 = (QSEditTileAdapter) getPage(pager, i).getAdapter();
            if (isPageMoreThanFull(page)) {
                TileInfo tileInfo = (TileInfo) qSEditTileAdapter.getItemList().remove(itemLocations.getMaxItems());
                qSEditTileAdapter.notifyItemRemoved(itemLocations.getMaxItems());
                qSEditTileAdapter2.getItemList().add(0, tileInfo);
                qSEditTileAdapter2.notifyItemInserted(0);
                i2 = -1;
            } else if (!isPageFull(page)) {
                TileInfo tileInfo2 = (TileInfo) qSEditTileAdapter2.getItemList().remove(0);
                qSEditTileAdapter2.notifyItemRemoved(0);
                qSEditTileAdapter.getItemList().add(tileInfo2);
                qSEditTileAdapter.notifyItemInserted(qSEditTileAdapter.getItemList().size() - 1);
                i2 = 1;
            }
        }
        return i2;
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public void onTilesChanged(List<TileInfo> list) {
        this.mAllTiles = list;
    }

    public void saveSpecs(QSTileHost qSTileHost) {
        ArrayList arrayList = new ArrayList();
        QSEditViewPager qSEditViewPager = this.mUpperPager;
        if (qSEditViewPager != null && qSEditViewPager.getAdapter() != null) {
            int pageCount = getPageCount(this.mUpperPager);
            for (int i = 0; i < pageCount; i++) {
                List itemList = ((QSEditTileAdapter) getPage(this.mUpperPager, i).getAdapter()).getItemList();
                int i2 = 0;
                while (i2 < itemList.size() && itemList.get(i2) != null) {
                    arrayList.add(((TileInfo) itemList.get(i2)).spec);
                    i2++;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("newSpecs=");
            sb.append(arrayList);
            Log.d("QSEditPageManager", sb.toString());
            qSTileHost.changeTiles(this.mCurrentSpecs, arrayList);
            this.mCurrentSpecs = arrayList;
        }
    }

    public void resetTileSpecs(QSTileHost qSTileHost, List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("resetTileSpecs=");
        sb.append(list);
        Log.d("QSEditPageManager", sb.toString());
        qSTileHost.changeTiles(this.mCurrentSpecs, list);
        setTileSpecs(list);
    }

    public void setTileSpecs(List<String> list) {
        list.equals(this.mCurrentSpecs);
        this.mCurrentSpecs = list;
        recalcSpecs();
    }

    public void recalcSpecs() {
        if (this.mCurrentSpecs != null) {
            List<TileInfo> list = this.mAllTiles;
            if (list != null) {
                this.mLowerTiles = new ArrayList(list);
                this.mUpperTiles.clear();
                for (int i = 0; i < this.mCurrentSpecs.size(); i++) {
                    TileInfo andRemoveLower = getAndRemoveLower((String) this.mCurrentSpecs.get(i));
                    if (andRemoveLower != null) {
                        this.mUpperTiles.add(andRemoveLower);
                    }
                }
                ArrayList arrayList = new ArrayList();
                for (int i2 = 0; i2 < this.mLowerTiles.size(); i2++) {
                    TileInfo tileInfo = (TileInfo) this.mLowerTiles.get(i2);
                    if (!tileInfo.isSystem) {
                        arrayList.add(tileInfo);
                    }
                }
                this.mLowerTiles.removeAll(arrayList);
                this.mLowerTiles.addAll(arrayList);
                fillPager(this.mUpperPager, this.mUpperTiles, this.mUpperLocations);
                fillPager(this.mLowerPager, this.mLowerTiles, this.mLowerLocations);
            }
        }
    }

    public void calculateItemLocation() {
        int[] iArr = new int[2];
        this.mUpperPager.getLocationOnScreen(iArr);
        this.mUpperLocations.setParentLocation(iArr[0], iArr[1], this.mUpperPager.getWidth());
        StringBuilder sb = new StringBuilder();
        sb.append("mUpperPager=");
        sb.append(iArr[0]);
        String str = ", ";
        sb.append(str);
        sb.append(iArr[1]);
        String str2 = "QSEditPageManager";
        Log.d(str2, sb.toString());
        this.mLowerPager.getLocationOnScreen(iArr);
        this.mLowerLocations.setParentLocation(iArr[0], iArr[1], this.mLowerPager.getWidth());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("mLowerPager=");
        sb2.append(iArr[0]);
        sb2.append(str);
        sb2.append(iArr[1]);
        Log.d(str2, sb2.toString());
    }

    private TileInfo getAndRemoveLower(String str) {
        int i = 0;
        while (i < this.mLowerTiles.size()) {
            try {
                if (((TileInfo) this.mLowerTiles.get(i)).spec.equals(str)) {
                    return (TileInfo) this.mLowerTiles.remove(i);
                }
                i++;
            } catch (NullPointerException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("getAndRemoveLower: number of tiles=");
                sb.append(this.mLowerTiles.size());
                sb.append(", i=");
                sb.append(i);
                Log.d("QSEditPageManager", sb.toString());
                printTiles(this.mLowerTiles);
                printTiles(this.mAllTiles);
                throw e;
            }
        }
        return null;
    }

    private void printTiles(List<TileInfo> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(":tile ");
            sb.append(i);
            sb.append(" = ");
            TileInfo tileInfo = (TileInfo) list.get(i);
            if (tileInfo != null) {
                sb.append(tileInfo.spec);
            } else {
                sb.append("NULL");
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("printTiles: tiles size=");
        sb2.append(list.size());
        sb2.append(" => ");
        sb2.append(sb.toString());
        Log.d("QSEditPageManager", sb2.toString());
    }
}
