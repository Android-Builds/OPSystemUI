package androidx.recyclerview.widget;

import android.view.View;
import com.airbnb.lottie.C0526R;

class ViewBoundsCheck {
    BoundFlags mBoundFlags = new BoundFlags();
    final Callback mCallback;

    static class BoundFlags {
        int mBoundFlags = 0;
        int mChildEnd;
        int mChildStart;
        int mRvEnd;
        int mRvStart;

        /* access modifiers changed from: 0000 */
        public int compare(int i, int i2) {
            if (i > i2) {
                return 1;
            }
            return i == i2 ? 2 : 4;
        }

        BoundFlags() {
        }

        /* access modifiers changed from: 0000 */
        public void setBounds(int i, int i2, int i3, int i4) {
            this.mRvStart = i;
            this.mRvEnd = i2;
            this.mChildStart = i3;
            this.mChildEnd = i4;
        }

        /* access modifiers changed from: 0000 */
        public void addFlags(int i) {
            this.mBoundFlags = i | this.mBoundFlags;
        }

        /* access modifiers changed from: 0000 */
        public void resetFlags() {
            this.mBoundFlags = 0;
        }

        /* access modifiers changed from: 0000 */
        public boolean boundsMatch() {
            int i = this.mBoundFlags;
            if ((i & 7) != 0 && (i & (compare(this.mChildStart, this.mRvStart) << 0)) == 0) {
                return false;
            }
            int i2 = this.mBoundFlags;
            if ((i2 & C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle) != 0 && (i2 & (compare(this.mChildStart, this.mRvEnd) << 4)) == 0) {
                return false;
            }
            int i3 = this.mBoundFlags;
            if ((i3 & 1792) != 0 && (i3 & (compare(this.mChildEnd, this.mRvStart) << 8)) == 0) {
                return false;
            }
            int i4 = this.mBoundFlags;
            if ((i4 & 28672) == 0 || ((compare(this.mChildEnd, this.mRvEnd) << 12) & i4) != 0) {
                return true;
            }
            return false;
        }
    }

    interface Callback {
        View getChildAt(int i);

        int getChildEnd(View view);

        int getChildStart(View view);

        int getParentEnd();

        int getParentStart();
    }

    ViewBoundsCheck(Callback callback) {
        this.mCallback = callback;
    }

    /* access modifiers changed from: 0000 */
    public View findOneViewWithinBoundFlags(int i, int i2, int i3, int i4) {
        int parentStart = this.mCallback.getParentStart();
        int parentEnd = this.mCallback.getParentEnd();
        int i5 = i2 > i ? 1 : -1;
        View view = null;
        while (i != i2) {
            View childAt = this.mCallback.getChildAt(i);
            this.mBoundFlags.setBounds(parentStart, parentEnd, this.mCallback.getChildStart(childAt), this.mCallback.getChildEnd(childAt));
            if (i3 != 0) {
                this.mBoundFlags.resetFlags();
                this.mBoundFlags.addFlags(i3);
                if (this.mBoundFlags.boundsMatch()) {
                    return childAt;
                }
            }
            if (i4 != 0) {
                this.mBoundFlags.resetFlags();
                this.mBoundFlags.addFlags(i4);
                if (this.mBoundFlags.boundsMatch()) {
                    view = childAt;
                }
            }
            i += i5;
        }
        return view;
    }

    /* access modifiers changed from: 0000 */
    public boolean isViewWithinBoundFlags(View view, int i) {
        this.mBoundFlags.setBounds(this.mCallback.getParentStart(), this.mCallback.getParentEnd(), this.mCallback.getChildStart(view), this.mCallback.getChildEnd(view));
        if (i == 0) {
            return false;
        }
        this.mBoundFlags.resetFlags();
        this.mBoundFlags.addFlags(i);
        return this.mBoundFlags.boundsMatch();
    }
}
