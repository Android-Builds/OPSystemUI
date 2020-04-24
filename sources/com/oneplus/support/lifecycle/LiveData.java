package com.oneplus.support.lifecycle;

import com.oneplus.support.collection.SafeIterableMap;
import com.oneplus.support.executor.ArchTaskExecutor;
import com.oneplus.support.lifecycle.Lifecycle.Event;
import com.oneplus.support.lifecycle.Lifecycle.State;
import java.util.Map.Entry;

public abstract class LiveData<T> {
    /* access modifiers changed from: private */
    public static final Object NOT_SET = new Object();
    /* access modifiers changed from: private */
    public int mActiveCount = 0;
    private volatile Object mData;
    /* access modifiers changed from: private */
    public final Object mDataLock = new Object();
    private boolean mDispatchInvalidated;
    private boolean mDispatchingValue;
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap<>();
    /* access modifiers changed from: private */
    public volatile Object mPendingData;
    private final Runnable mPostValueRunnable;
    private int mVersion;

    class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(LifecycleOwner lifecycleOwner, Observer<? super T> observer) {
            super(observer);
            this.mOwner = lifecycleOwner;
        }

        /* access modifiers changed from: 0000 */
        public boolean shouldBeActive() {
            return this.mOwner.getLifecycle().getCurrentState().isAtLeast(State.STARTED);
        }

        public void onStateChanged(LifecycleOwner lifecycleOwner, Event event) {
            if (this.mOwner.getLifecycle().getCurrentState() == State.DESTROYED) {
                LiveData.this.removeObserver(this.mObserver);
            } else {
                activeStateChanged(shouldBeActive());
            }
        }

        /* access modifiers changed from: 0000 */
        public boolean isAttachedTo(LifecycleOwner lifecycleOwner) {
            return this.mOwner == lifecycleOwner;
        }

        /* access modifiers changed from: 0000 */
        public void detachObserver() {
            this.mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ObserverWrapper {
        boolean mActive;
        int mLastVersion = -1;
        final Observer<? super T> mObserver;

        /* access modifiers changed from: 0000 */
        public void detachObserver() {
        }

        /* access modifiers changed from: 0000 */
        public boolean isAttachedTo(LifecycleOwner lifecycleOwner) {
            return false;
        }

        /* access modifiers changed from: 0000 */
        public abstract boolean shouldBeActive();

        ObserverWrapper(Observer<? super T> observer) {
            this.mObserver = observer;
        }

        /* access modifiers changed from: 0000 */
        public void activeStateChanged(boolean z) {
            if (z != this.mActive) {
                this.mActive = z;
                int i = 1;
                boolean z2 = LiveData.this.mActiveCount == 0;
                LiveData liveData = LiveData.this;
                if (!this.mActive) {
                    i = -1;
                }
                LiveData.access$312(liveData, i);
                if (z2 && this.mActive) {
                    LiveData.this.onActive();
                }
                if (LiveData.this.mActiveCount == 0 && !this.mActive) {
                    LiveData.this.onInactive();
                }
                if (this.mActive) {
                    LiveData.this.dispatchingValue(this);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onActive() {
    }

    /* access modifiers changed from: protected */
    public void onInactive() {
    }

    public LiveData() {
        Object obj = NOT_SET;
        this.mData = obj;
        this.mPendingData = obj;
        this.mVersion = -1;
        this.mPostValueRunnable = new Runnable() {
            public void run() {
                Object access$100;
                synchronized (LiveData.this.mDataLock) {
                    access$100 = LiveData.this.mPendingData;
                    LiveData.this.mPendingData = LiveData.NOT_SET;
                }
                LiveData.this.setValue(access$100);
            }
        };
    }

    static /* synthetic */ int access$312(LiveData liveData, int i) {
        int i2 = liveData.mActiveCount + i;
        liveData.mActiveCount = i2;
        return i2;
    }

    private void considerNotify(ObserverWrapper observerWrapper) {
        if (observerWrapper.mActive) {
            if (!observerWrapper.shouldBeActive()) {
                observerWrapper.activeStateChanged(false);
                return;
            }
            int i = observerWrapper.mLastVersion;
            int i2 = this.mVersion;
            if (i < i2) {
                observerWrapper.mLastVersion = i2;
                observerWrapper.mObserver.onChanged(this.mData);
            }
        }
    }

    /* access modifiers changed from: private */
    public void dispatchingValue(ObserverWrapper observerWrapper) {
        if (this.mDispatchingValue) {
            this.mDispatchInvalidated = true;
            return;
        }
        this.mDispatchingValue = true;
        do {
            this.mDispatchInvalidated = false;
            if (observerWrapper == null) {
                IteratorWithAdditions iteratorWithAdditions = this.mObservers.iteratorWithAdditions();
                while (iteratorWithAdditions.hasNext()) {
                    considerNotify((ObserverWrapper) ((Entry) iteratorWithAdditions.next()).getValue());
                    if (this.mDispatchInvalidated) {
                        break;
                    }
                }
            } else {
                considerNotify(observerWrapper);
                observerWrapper = null;
            }
        } while (this.mDispatchInvalidated);
        this.mDispatchingValue = false;
    }

    public void observe(LifecycleOwner lifecycleOwner, Observer<? super T> observer) {
        assertMainThread("observe");
        if (lifecycleOwner.getLifecycle().getCurrentState() != State.DESTROYED) {
            LifecycleBoundObserver lifecycleBoundObserver = new LifecycleBoundObserver(lifecycleOwner, observer);
            ObserverWrapper observerWrapper = (ObserverWrapper) this.mObservers.putIfAbsent(observer, lifecycleBoundObserver);
            if (observerWrapper != null && !observerWrapper.isAttachedTo(lifecycleOwner)) {
                throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
            } else if (observerWrapper == null) {
                lifecycleOwner.getLifecycle().addObserver(lifecycleBoundObserver);
            }
        }
    }

    public void removeObserver(Observer<? super T> observer) {
        assertMainThread("removeObserver");
        ObserverWrapper observerWrapper = (ObserverWrapper) this.mObservers.remove(observer);
        if (observerWrapper != null) {
            observerWrapper.detachObserver();
            observerWrapper.activeStateChanged(false);
        }
    }

    /* access modifiers changed from: protected */
    public void setValue(T t) {
        assertMainThread("setValue");
        this.mVersion++;
        this.mData = t;
        dispatchingValue(null);
    }

    private static void assertMainThread(String str) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot invoke ");
            sb.append(str);
            sb.append(" on a background thread");
            throw new IllegalStateException(sb.toString());
        }
    }
}
