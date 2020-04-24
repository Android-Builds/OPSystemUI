package com.android.systemui.p007qs.tiles;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.lifecycle.LifecycleOwner;
import com.airbnb.lottie.C0526R;
import com.android.internal.app.MediaRouteDialogPresenter;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSDetailItems;
import com.android.systemui.p007qs.QSDetailItems.Item;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/* renamed from: com.android.systemui.qs.tiles.CastTile */
public class CastTile extends QSTileImpl<BooleanState> {
    /* access modifiers changed from: private */
    public static final Intent CAST_SETTINGS = new Intent("android.settings.CAST_SETTINGS");
    private final ActivityStarter mActivityStarter;
    private final Callback mCallback = new Callback();
    /* access modifiers changed from: private */
    public final CastController mController;
    private final CastDetailAdapter mDetailAdapter;
    private Dialog mDialog;
    private final KeyguardMonitor mKeyguard;
    private final NetworkController mNetworkController;
    private final SignalCallback mSignalCallback = new SignalCallback() {
        public void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
            boolean z5 = z && iconState2.visible;
            if (z5 != CastTile.this.mWifiConnected) {
                CastTile.this.mWifiConnected = z5;
                CastTile.this.refreshState();
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mWifiConnected;

    /* renamed from: com.android.systemui.qs.tiles.CastTile$Callback */
    private final class Callback implements com.android.systemui.statusbar.policy.CastController.Callback, com.android.systemui.statusbar.policy.KeyguardMonitor.Callback {
        private Callback() {
        }

        public void onCastDevicesChanged() {
            CastTile.this.refreshState();
        }

        public void onKeyguardShowingChanged() {
            CastTile.this.refreshState();
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.CastTile$CastDetailAdapter */
    private final class CastDetailAdapter implements DetailAdapter, com.android.systemui.p007qs.QSDetailItems.Callback {
        private QSDetailItems mItems;
        /* access modifiers changed from: private */
        public final LinkedHashMap<String, CastDevice> mVisibleOrder;

        public int getMetricsCategory() {
            return 151;
        }

        public Boolean getToggleState() {
            return null;
        }

        public void setToggleState(boolean z) {
        }

        private CastDetailAdapter() {
            this.mVisibleOrder = new LinkedHashMap<>();
        }

        public CharSequence getTitle() {
            return CastTile.this.mContext.getString(R$string.quick_settings_cast_title);
        }

        public Intent getSettingsIntent() {
            return CastTile.CAST_SETTINGS;
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            this.mItems = QSDetailItems.convertOrInflate(context, view, viewGroup);
            this.mItems.setTagSuffix("Cast");
            if (view == null) {
                if (QSTileImpl.DEBUG) {
                    Log.d(CastTile.this.TAG, "addOnAttachStateChangeListener");
                }
                this.mItems.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    public void onViewAttachedToWindow(View view) {
                        if (QSTileImpl.DEBUG) {
                            Log.d(CastTile.this.TAG, "onViewAttachedToWindow");
                        }
                    }

                    public void onViewDetachedFromWindow(View view) {
                        if (QSTileImpl.DEBUG) {
                            Log.d(CastTile.this.TAG, "onViewDetachedFromWindow");
                        }
                        CastDetailAdapter.this.mVisibleOrder.clear();
                    }
                });
            }
            this.mItems.setEmptyState(R$drawable.ic_qs_cast_detail_empty, R$string.quick_settings_cast_detail_empty_text);
            this.mItems.setCallback(this);
            updateItems(CastTile.this.mController.getCastDevices());
            CastTile.this.mController.setDiscovering(true);
            return this.mItems;
        }

        /* access modifiers changed from: private */
        public void updateItems(List<CastDevice> list) {
            int i;
            if (this.mItems != null) {
                Item[] itemArr = null;
                if (list != null && !list.isEmpty()) {
                    Iterator it = list.iterator();
                    while (true) {
                        i = 0;
                        if (!it.hasNext()) {
                            break;
                        }
                        CastDevice castDevice = (CastDevice) it.next();
                        if (castDevice.state == 2) {
                            Item item = new Item();
                            item.iconResId = R$drawable.ic_cast_connected;
                            item.line1 = CastTile.this.getDeviceName(castDevice);
                            item.line2 = CastTile.this.mContext.getString(R$string.quick_settings_connected);
                            item.tag = castDevice;
                            item.canDisconnect = true;
                            itemArr = new Item[]{item};
                            break;
                        }
                    }
                    if (itemArr == null) {
                        for (CastDevice castDevice2 : list) {
                            this.mVisibleOrder.put(castDevice2.f99id, castDevice2);
                        }
                        itemArr = new Item[list.size()];
                        for (String str : this.mVisibleOrder.keySet()) {
                            CastDevice castDevice3 = (CastDevice) this.mVisibleOrder.get(str);
                            if (list.contains(castDevice3)) {
                                Item item2 = new Item();
                                item2.iconResId = R$drawable.ic_cast;
                                item2.line1 = CastTile.this.getDeviceName(castDevice3);
                                if (castDevice3.state == 1) {
                                    item2.line2 = CastTile.this.mContext.getString(R$string.quick_settings_connecting);
                                }
                                item2.tag = castDevice3;
                                int i2 = i + 1;
                                itemArr[i] = item2;
                                i = i2;
                            }
                        }
                    }
                }
                this.mItems.setItems(itemArr);
            }
        }

        public void onDetailItemClick(Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 157);
                CastTile.this.mController.startCasting((CastDevice) item.tag);
            }
        }

        public void onDetailItemDisconnect(Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 158);
                CastTile.this.mController.stopCasting((CastDevice) item.tag);
            }
        }
    }

    public int getMetricsCategory() {
        return C0526R.styleable.AppCompatTheme_tooltipForegroundColor;
    }

    public CastTile(QSHost qSHost, CastController castController, KeyguardMonitor keyguardMonitor, NetworkController networkController, ActivityStarter activityStarter) {
        super(qSHost);
        this.mController = castController;
        this.mDetailAdapter = new CastDetailAdapter();
        this.mKeyguard = keyguardMonitor;
        this.mNetworkController = networkController;
        this.mActivityStarter = activityStarter;
        this.mController.observe((LifecycleOwner) this, this.mCallback);
        this.mKeyguard.observe((LifecycleOwner) this, this.mCallback);
        this.mNetworkController.observe((LifecycleOwner) this, this.mSignalCallback);
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public void handleSetListening(boolean z) {
        if (QSTileImpl.DEBUG) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("handleSetListening ");
            sb.append(z);
            Log.d(str, sb.toString());
        }
        if (!z) {
            this.mController.setDiscovering(false);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int i) {
        super.handleUserSwitch(i);
        this.mController.setCurrentUserId(i);
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.CAST_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((BooleanState) getState()).state != 0) {
            List activeDevices = getActiveDevices();
            if (activeDevices.isEmpty() || (((CastDevice) activeDevices.get(0)).tag instanceof RouteInfo)) {
                this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
                    public final void run() {
                        CastTile.this.lambda$handleClick$0$CastTile();
                    }
                });
            } else {
                this.mController.stopCasting((CastDevice) activeDevices.get(0));
            }
        }
    }

    public /* synthetic */ void lambda$handleClick$0$CastTile() {
        showDetail(true);
    }

    private List<CastDevice> getActiveDevices() {
        ArrayList arrayList = new ArrayList();
        for (CastDevice castDevice : this.mController.getCastDevices()) {
            int i = castDevice.state;
            if (i == 2 || i == 1) {
                arrayList.add(castDevice);
            }
        }
        return arrayList;
    }

    public void showDetail(boolean z) {
        this.mUiHandler.post(new Runnable() {
            public final void run() {
                CastTile.this.lambda$showDetail$3$CastTile();
            }
        });
    }

    public /* synthetic */ void lambda$showDetail$3$CastTile() {
        this.mDialog = MediaRouteDialogPresenter.createDialog(this.mContext, 4, new OnClickListener() {
            public final void onClick(View view) {
                CastTile.this.lambda$showDetail$1$CastTile(view);
            }
        });
        this.mDialog.getWindow().setType(2009);
        SystemUIDialog.setShowForAllUsers(this.mDialog, true);
        SystemUIDialog.registerDismissListener(this.mDialog);
        SystemUIDialog.setWindowOnTop(this.mDialog);
        this.mUiHandler.post(new Runnable() {
            public final void run() {
                CastTile.this.lambda$showDetail$2$CastTile();
            }
        });
        this.mHost.collapsePanels();
    }

    public /* synthetic */ void lambda$showDetail$1$CastTile(View view) {
        this.mDialog.dismiss();
        this.mActivityStarter.postStartActivityDismissingKeyguard(getLongClickIntent(), 0);
    }

    public /* synthetic */ void lambda$showDetail$2$CastTile() {
        this.mDialog.show();
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_cast_title);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        String str;
        int i;
        int i2;
        booleanState.label = this.mContext.getString(R$string.quick_settings_cast_title);
        booleanState.contentDescription = booleanState.label;
        boolean z = false;
        booleanState.value = false;
        List castDevices = this.mController.getCastDevices();
        Iterator it = castDevices.iterator();
        boolean z2 = false;
        while (true) {
            str = ",";
            i = 2;
            if (!it.hasNext()) {
                z = z2;
                break;
            }
            CastDevice castDevice = (CastDevice) it.next();
            int i3 = castDevice.state;
            if (i3 == 2) {
                booleanState.value = true;
                booleanState.secondaryLabel = getDeviceName(castDevice);
                StringBuilder sb = new StringBuilder();
                sb.append(booleanState.contentDescription);
                sb.append(str);
                sb.append(this.mContext.getString(R$string.accessibility_cast_name, new Object[]{booleanState.label}));
                booleanState.contentDescription = sb.toString();
                break;
            } else if (i3 == 1) {
                z2 = true;
            }
        }
        if (z && !booleanState.value) {
            booleanState.secondaryLabel = this.mContext.getString(R$string.quick_settings_connecting);
        }
        if (booleanState.value) {
            i2 = R$drawable.ic_cast_connected;
        } else {
            i2 = R$drawable.ic_cast;
        }
        booleanState.icon = ResourceIcon.get(i2);
        if (!booleanState.value) {
            i = 1;
        }
        booleanState.state = i;
        if (!booleanState.value) {
            booleanState.secondaryLabel = "";
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(booleanState.contentDescription);
        sb2.append(str);
        sb2.append(this.mContext.getString(R$string.accessibility_quick_settings_open_details));
        booleanState.contentDescription = sb2.toString();
        booleanState.expandedAccessibilityClassName = Button.class.getName();
        this.mDetailAdapter.updateItems(castDevices);
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (!((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_casting_turned_off);
        }
        return null;
    }

    /* access modifiers changed from: private */
    public String getDeviceName(CastDevice castDevice) {
        String str = castDevice.name;
        return str != null ? str : this.mContext.getString(R$string.quick_settings_cast_device_default_name);
    }
}
