package androidx.leanback.widget;

import android.view.View;
import androidx.leanback.widget.ItemAlignmentFacet.ItemAlignmentDef;

class ItemAlignment {
    public final Axis horizontal = new Axis(0);
    private Axis mMainAxis = this.horizontal;
    private int mOrientation = 0;
    private Axis mSecondAxis = this.vertical;
    public final Axis vertical = new Axis(1);

    static final class Axis extends ItemAlignmentDef {
        private int mOrientation;

        Axis(int i) {
            this.mOrientation = i;
        }

        public int getAlignmentPosition(View view) {
            return ItemAlignmentFacetHelper.getAlignmentPosition(view, this, this.mOrientation);
        }
    }

    ItemAlignment() {
    }

    public final void setOrientation(int i) {
        this.mOrientation = i;
        if (this.mOrientation == 0) {
            this.mMainAxis = this.horizontal;
            this.mSecondAxis = this.vertical;
            return;
        }
        this.mMainAxis = this.vertical;
        this.mSecondAxis = this.horizontal;
    }
}
