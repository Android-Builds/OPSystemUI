package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import androidx.lifecycle.LifecycleOwner;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback;

/* renamed from: com.android.systemui.qs.tiles.RotationLockTile */
public class RotationLockTile extends QSTileImpl<BooleanState> {
    private final RotationLockControllerCallback mCallback = new RotationLockControllerCallback() {
        public void onRotationLockStateChanged(boolean z, boolean z2) {
            RotationLockTile.this.refreshState(Boolean.valueOf(z));
        }
    };
    private final RotationLockController mController;
    private final Icon mIcon = ResourceIcon.get(17302778);

    public int getMetricsCategory() {
        return 123;
    }

    public void handleSetListening(boolean z) {
    }

    public RotationLockTile(QSHost qSHost, RotationLockController rotationLockController) {
        super(qSHost);
        this.mController = rotationLockController;
        this.mController.observe((LifecycleOwner) this, this.mCallback);
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean z = true;
        boolean z2 = !((BooleanState) this.mState).value;
        RotationLockController rotationLockController = this.mController;
        if (z2) {
            z = false;
        }
        rotationLockController.setRotationLocked(z);
        refreshState(Boolean.valueOf(z2));
    }

    public CharSequence getTileLabel() {
        return ((BooleanState) getState()).label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        boolean isRotationLocked = this.mController.isRotationLocked();
        booleanState.value = !isRotationLocked;
        booleanState.label = this.mContext.getString(R$string.quick_settings_rotation_unlocked_label);
        booleanState.icon = this.mIcon;
        booleanState.contentDescription = getAccessibilityString(isRotationLocked);
        booleanState.expandedAccessibilityClassName = Switch.class.getName();
        booleanState.state = booleanState.value ? 2 : 1;
    }

    public static boolean isCurrentOrientationLockPortrait(RotationLockController rotationLockController, Context context) {
        int rotationLockOrientation = rotationLockController.getRotationLockOrientation();
        boolean z = true;
        if (rotationLockOrientation == 0) {
            if (context.getResources().getConfiguration().orientation == 2) {
                z = false;
            }
            return z;
        }
        if (rotationLockOrientation == 2) {
            z = false;
        }
        return z;
    }

    private String getAccessibilityString(boolean z) {
        return this.mContext.getString(R$string.accessibility_quick_settings_rotation);
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        return getAccessibilityString(((BooleanState) this.mState).value);
    }
}
