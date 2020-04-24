package com.oneplus.lib.widget.recyclerview;

import com.oneplus.lib.widget.recyclerview.RecyclerView.ViewHolder;
import java.util.ArrayList;
import java.util.List;

class AdapterHelper implements Callback {
    final Callback mCallback;
    final boolean mDisableRecycler;
    Runnable mOnItemProcessedCallback;
    final OpReorderer mOpReorderer;
    final ArrayList<UpdateOp> mPendingUpdates;
    final ArrayList<UpdateOp> mPostponedList;
    private Pools$Pool<UpdateOp> mUpdateOpPool;

    interface Callback {
        ViewHolder findViewHolder(int i);

        void markViewHoldersUpdated(int i, int i2, Object obj);

        void offsetPositionsForAdd(int i, int i2);

        void offsetPositionsForMove(int i, int i2);

        void offsetPositionsForRemovingInvisible(int i, int i2);

        void offsetPositionsForRemovingLaidOutOrNewView(int i, int i2);

        void onDispatchFirstPass(UpdateOp updateOp);

        void onDispatchSecondPass(UpdateOp updateOp);
    }

    static class UpdateOp {
        int cmd;
        int itemCount;
        Object payload;
        int positionStart;

        UpdateOp(int i, int i2, int i3, Object obj) {
            this.cmd = i;
            this.positionStart = i2;
            this.itemCount = i3;
            this.payload = obj;
        }

        /* access modifiers changed from: 0000 */
        public String cmdToString() {
            int i = this.cmd;
            if (i == 0) {
                return "add";
            }
            if (i == 1) {
                return "rm";
            }
            if (i != 2) {
                return i != 3 ? "??" : "mv";
            }
            return "up";
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append("[");
            sb.append(cmdToString());
            sb.append(",s:");
            sb.append(this.positionStart);
            sb.append("c:");
            sb.append(this.itemCount);
            sb.append(",p:");
            sb.append(this.payload);
            sb.append("]");
            return sb.toString();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || UpdateOp.class != obj.getClass()) {
                return false;
            }
            UpdateOp updateOp = (UpdateOp) obj;
            int i = this.cmd;
            if (i != updateOp.cmd) {
                return false;
            }
            if (i == 3 && Math.abs(this.itemCount - this.positionStart) == 1 && this.itemCount == updateOp.positionStart && this.positionStart == updateOp.itemCount) {
                return true;
            }
            if (this.itemCount != updateOp.itemCount || this.positionStart != updateOp.positionStart) {
                return false;
            }
            Object obj2 = this.payload;
            if (obj2 != null) {
                if (!obj2.equals(updateOp.payload)) {
                    return false;
                }
            } else if (updateOp.payload != null) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return (((this.cmd * 31) + this.positionStart) * 31) + this.itemCount;
        }
    }

    AdapterHelper(Callback callback) {
        this(callback, false);
    }

    AdapterHelper(Callback callback, boolean z) {
        this.mUpdateOpPool = new Pools$SimplePool(30);
        this.mPendingUpdates = new ArrayList<>();
        this.mPostponedList = new ArrayList<>();
        this.mCallback = callback;
        this.mDisableRecycler = z;
        this.mOpReorderer = new OpReorderer(this);
    }

    /* access modifiers changed from: 0000 */
    public void reset() {
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
        recycleUpdateOpsAndClearList(this.mPostponedList);
    }

    /* access modifiers changed from: 0000 */
    public void preProcess() {
        this.mOpReorderer.reorderOps(this.mPendingUpdates);
        int size = this.mPendingUpdates.size();
        for (int i = 0; i < size; i++) {
            UpdateOp updateOp = (UpdateOp) this.mPendingUpdates.get(i);
            int i2 = updateOp.cmd;
            if (i2 == 0) {
                applyAdd(updateOp);
            } else if (i2 == 1) {
                applyRemove(updateOp);
            } else if (i2 == 2) {
                applyUpdate(updateOp);
            } else if (i2 == 3) {
                applyMove(updateOp);
            }
            Runnable runnable = this.mOnItemProcessedCallback;
            if (runnable != null) {
                runnable.run();
            }
        }
        this.mPendingUpdates.clear();
    }

    /* access modifiers changed from: 0000 */
    public void consumePostponedUpdates() {
        int size = this.mPostponedList.size();
        for (int i = 0; i < size; i++) {
            this.mCallback.onDispatchSecondPass((UpdateOp) this.mPostponedList.get(i));
        }
        recycleUpdateOpsAndClearList(this.mPostponedList);
    }

    private void applyMove(UpdateOp updateOp) {
        postponeAndUpdateViewHolders(updateOp);
    }

    private void applyRemove(UpdateOp updateOp) {
        boolean z;
        boolean z2;
        boolean z3;
        int i = updateOp.positionStart;
        int i2 = 0;
        boolean z4 = true;
        int i3 = updateOp.itemCount + i;
        int i4 = i;
        while (i4 < i3) {
            if (this.mCallback.findViewHolder(i4) != null || canFindInPreLayout(i4)) {
                if (!z4) {
                    dispatchAndUpdateViewHolders(obtainUpdateOp(1, i, i2, null));
                    z3 = true;
                } else {
                    z3 = false;
                }
                z = true;
            } else {
                if (z4) {
                    postponeAndUpdateViewHolders(obtainUpdateOp(1, i, i2, null));
                    z2 = true;
                } else {
                    z2 = false;
                }
                z = false;
            }
            if (z2) {
                i4 -= i2;
                i3 -= i2;
                i2 = 1;
            } else {
                i2++;
            }
            i4++;
            z4 = z;
        }
        if (i2 != updateOp.itemCount) {
            recycleUpdateOp(updateOp);
            updateOp = obtainUpdateOp(1, i, i2, null);
        }
        if (!z4) {
            dispatchAndUpdateViewHolders(updateOp);
        } else {
            postponeAndUpdateViewHolders(updateOp);
        }
    }

    private void applyUpdate(UpdateOp updateOp) {
        int i = updateOp.positionStart;
        int i2 = updateOp.itemCount + i;
        int i3 = i;
        boolean z = true;
        int i4 = 0;
        while (i < i2) {
            if (this.mCallback.findViewHolder(i) != null || canFindInPreLayout(i)) {
                if (!z) {
                    dispatchAndUpdateViewHolders(obtainUpdateOp(2, i3, i4, updateOp.payload));
                    i3 = i;
                    i4 = 0;
                }
                z = true;
            } else {
                if (z) {
                    postponeAndUpdateViewHolders(obtainUpdateOp(2, i3, i4, updateOp.payload));
                    i3 = i;
                    i4 = 0;
                }
                z = false;
            }
            i4++;
            i++;
        }
        if (i4 != updateOp.itemCount) {
            Object obj = updateOp.payload;
            recycleUpdateOp(updateOp);
            updateOp = obtainUpdateOp(2, i3, i4, obj);
        }
        if (!z) {
            dispatchAndUpdateViewHolders(updateOp);
        } else {
            postponeAndUpdateViewHolders(updateOp);
        }
    }

    private void dispatchAndUpdateViewHolders(UpdateOp updateOp) {
        int i;
        int i2 = updateOp.cmd;
        if (i2 == 0 || i2 == 3) {
            throw new IllegalArgumentException("should not dispatch add or move for pre layout");
        }
        int updatePositionWithPostponed = updatePositionWithPostponed(updateOp.positionStart, i2);
        int i3 = updateOp.positionStart;
        int i4 = updateOp.cmd;
        if (i4 == 1) {
            i = 0;
        } else if (i4 == 2) {
            i = 1;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("op should be remove or update.");
            sb.append(updateOp);
            throw new IllegalArgumentException(sb.toString());
        }
        int i5 = updatePositionWithPostponed;
        int i6 = i3;
        int i7 = 1;
        for (int i8 = 1; i8 < updateOp.itemCount; i8++) {
            int updatePositionWithPostponed2 = updatePositionWithPostponed(updateOp.positionStart + (i * i8), updateOp.cmd);
            int i9 = updateOp.cmd;
            if (i9 == 1 ? updatePositionWithPostponed2 == i5 : i9 == 2 && updatePositionWithPostponed2 == i5 + 1) {
                i7++;
            } else {
                UpdateOp obtainUpdateOp = obtainUpdateOp(updateOp.cmd, i5, i7, updateOp.payload);
                dispatchFirstPassAndUpdateViewHolders(obtainUpdateOp, i6);
                recycleUpdateOp(obtainUpdateOp);
                if (updateOp.cmd == 2) {
                    i6 += i7;
                }
                i7 = 1;
                i5 = updatePositionWithPostponed2;
            }
        }
        Object obj = updateOp.payload;
        recycleUpdateOp(updateOp);
        if (i7 > 0) {
            UpdateOp obtainUpdateOp2 = obtainUpdateOp(updateOp.cmd, i5, i7, obj);
            dispatchFirstPassAndUpdateViewHolders(obtainUpdateOp2, i6);
            recycleUpdateOp(obtainUpdateOp2);
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchFirstPassAndUpdateViewHolders(UpdateOp updateOp, int i) {
        this.mCallback.onDispatchFirstPass(updateOp);
        int i2 = updateOp.cmd;
        if (i2 == 1) {
            this.mCallback.offsetPositionsForRemovingInvisible(i, updateOp.itemCount);
        } else if (i2 == 2) {
            this.mCallback.markViewHoldersUpdated(i, updateOp.itemCount, updateOp.payload);
        } else {
            throw new IllegalArgumentException("only remove and update ops can be dispatched in first pass");
        }
    }

    private int updatePositionWithPostponed(int i, int i2) {
        for (int size = this.mPostponedList.size() - 1; size >= 0; size--) {
            UpdateOp updateOp = (UpdateOp) this.mPostponedList.get(size);
            int i3 = updateOp.cmd;
            if (i3 == 3) {
                int i4 = updateOp.positionStart;
                int i5 = updateOp.itemCount;
                if (i4 >= i5) {
                    int i6 = i5;
                    i5 = i4;
                    i4 = i6;
                }
                if (i < i4 || i > i5) {
                    int i7 = updateOp.positionStart;
                    if (i < i7) {
                        if (i2 == 0) {
                            updateOp.positionStart = i7 + 1;
                            updateOp.itemCount++;
                        } else if (i2 == 1) {
                            updateOp.positionStart = i7 - 1;
                            updateOp.itemCount--;
                        }
                    }
                } else {
                    int i8 = updateOp.positionStart;
                    if (i4 == i8) {
                        if (i2 == 0) {
                            updateOp.itemCount++;
                        } else if (i2 == 1) {
                            updateOp.itemCount--;
                        }
                        i++;
                    } else {
                        if (i2 == 0) {
                            updateOp.positionStart = i8 + 1;
                        } else if (i2 == 1) {
                            updateOp.positionStart = i8 - 1;
                        }
                        i--;
                    }
                }
            } else {
                int i9 = updateOp.positionStart;
                if (i9 <= i) {
                    if (i3 == 0) {
                        i -= updateOp.itemCount;
                    } else if (i3 == 1) {
                        i += updateOp.itemCount;
                    }
                } else if (i2 == 0) {
                    updateOp.positionStart = i9 + 1;
                } else if (i2 == 1) {
                    updateOp.positionStart = i9 - 1;
                }
            }
        }
        for (int size2 = this.mPostponedList.size() - 1; size2 >= 0; size2--) {
            UpdateOp updateOp2 = (UpdateOp) this.mPostponedList.get(size2);
            if (updateOp2.cmd == 3) {
                int i10 = updateOp2.itemCount;
                if (i10 == updateOp2.positionStart || i10 < 0) {
                    this.mPostponedList.remove(size2);
                    recycleUpdateOp(updateOp2);
                }
            } else if (updateOp2.itemCount <= 0) {
                this.mPostponedList.remove(size2);
                recycleUpdateOp(updateOp2);
            }
        }
        return i;
    }

    private boolean canFindInPreLayout(int i) {
        int size = this.mPostponedList.size();
        for (int i2 = 0; i2 < size; i2++) {
            UpdateOp updateOp = (UpdateOp) this.mPostponedList.get(i2);
            int i3 = updateOp.cmd;
            if (i3 == 3) {
                if (findPositionOffset(updateOp.itemCount, i2 + 1) == i) {
                    return true;
                }
            } else if (i3 == 0) {
                int i4 = updateOp.positionStart;
                int i5 = updateOp.itemCount + i4;
                while (i4 < i5) {
                    if (findPositionOffset(i4, i2 + 1) == i) {
                        return true;
                    }
                    i4++;
                }
                continue;
            } else {
                continue;
            }
        }
        return false;
    }

    private void applyAdd(UpdateOp updateOp) {
        postponeAndUpdateViewHolders(updateOp);
    }

    private void postponeAndUpdateViewHolders(UpdateOp updateOp) {
        this.mPostponedList.add(updateOp);
        int i = updateOp.cmd;
        if (i == 0) {
            this.mCallback.offsetPositionsForAdd(updateOp.positionStart, updateOp.itemCount);
        } else if (i == 1) {
            this.mCallback.offsetPositionsForRemovingLaidOutOrNewView(updateOp.positionStart, updateOp.itemCount);
        } else if (i == 2) {
            this.mCallback.markViewHoldersUpdated(updateOp.positionStart, updateOp.itemCount, updateOp.payload);
        } else if (i == 3) {
            this.mCallback.offsetPositionsForMove(updateOp.positionStart, updateOp.itemCount);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown update op type for ");
            sb.append(updateOp);
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean hasPendingUpdates() {
        return this.mPendingUpdates.size() > 0;
    }

    /* access modifiers changed from: 0000 */
    public int findPositionOffset(int i, int i2) {
        int size = this.mPostponedList.size();
        while (i2 < size) {
            UpdateOp updateOp = (UpdateOp) this.mPostponedList.get(i2);
            int i3 = updateOp.cmd;
            if (i3 == 3) {
                int i4 = updateOp.positionStart;
                if (i4 == i) {
                    i = updateOp.itemCount;
                } else {
                    if (i4 < i) {
                        i--;
                    }
                    if (updateOp.itemCount <= i) {
                        i++;
                    }
                }
            } else {
                int i5 = updateOp.positionStart;
                if (i5 > i) {
                    continue;
                } else if (i3 == 1) {
                    int i6 = updateOp.itemCount;
                    if (i < i5 + i6) {
                        return -1;
                    }
                    i -= i6;
                } else if (i3 == 0) {
                    i += updateOp.itemCount;
                }
            }
            i2++;
        }
        return i;
    }

    /* access modifiers changed from: 0000 */
    public void consumeUpdatesInOnePass() {
        consumePostponedUpdates();
        int size = this.mPendingUpdates.size();
        for (int i = 0; i < size; i++) {
            UpdateOp updateOp = (UpdateOp) this.mPendingUpdates.get(i);
            int i2 = updateOp.cmd;
            if (i2 == 0) {
                this.mCallback.onDispatchSecondPass(updateOp);
                this.mCallback.offsetPositionsForAdd(updateOp.positionStart, updateOp.itemCount);
            } else if (i2 == 1) {
                this.mCallback.onDispatchSecondPass(updateOp);
                this.mCallback.offsetPositionsForRemovingInvisible(updateOp.positionStart, updateOp.itemCount);
            } else if (i2 == 2) {
                this.mCallback.onDispatchSecondPass(updateOp);
                this.mCallback.markViewHoldersUpdated(updateOp.positionStart, updateOp.itemCount, updateOp.payload);
            } else if (i2 == 3) {
                this.mCallback.onDispatchSecondPass(updateOp);
                this.mCallback.offsetPositionsForMove(updateOp.positionStart, updateOp.itemCount);
            }
            Runnable runnable = this.mOnItemProcessedCallback;
            if (runnable != null) {
                runnable.run();
            }
        }
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
    }

    public UpdateOp obtainUpdateOp(int i, int i2, int i3, Object obj) {
        UpdateOp updateOp = (UpdateOp) this.mUpdateOpPool.acquire();
        if (updateOp == null) {
            return new UpdateOp(i, i2, i3, obj);
        }
        updateOp.cmd = i;
        updateOp.positionStart = i2;
        updateOp.itemCount = i3;
        updateOp.payload = obj;
        return updateOp;
    }

    public void recycleUpdateOp(UpdateOp updateOp) {
        if (!this.mDisableRecycler) {
            updateOp.payload = null;
            this.mUpdateOpPool.release(updateOp);
        }
    }

    /* access modifiers changed from: 0000 */
    public void recycleUpdateOpsAndClearList(List<UpdateOp> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            recycleUpdateOp((UpdateOp) list.get(i));
        }
        list.clear();
    }
}
