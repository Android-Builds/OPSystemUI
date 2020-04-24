package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.p007qs.PseudoGridView;
import com.android.systemui.p007qs.PseudoGridView.ViewGroupAdapterBridge;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.WLBSwitchController.BaseUserAdapter;
import com.android.systemui.statusbar.phone.WLBSwitchController.WLBControllerCallbacks;
import com.android.systemui.statusbar.phone.WLBSwitchController.WLBModeItem;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.ThemeColorUtils;

public class WLBDetailView extends PseudoGridView implements WLBControllerCallbacks {
    /* access modifiers changed from: private */
    public static final String TAG = "WLBDetailView";
    private Adapter mAdapter;
    /* access modifiers changed from: private */
    public float mExpansion;
    private boolean mIsFullyExpanded;

    public static class Adapter extends BaseUserAdapter implements OnClickListener {
        private final Context mContext;
        protected WLBSwitchController mController;
        private WLBDetailView mDetailView;
        private boolean mFullyExpanded;
        private ViewGroup mViewGroup;

        public Adapter(Context context, WLBSwitchController wLBSwitchController, WLBDetailView wLBDetailView) {
            super(wLBSwitchController);
            this.mContext = context;
            this.mController = wLBSwitchController;
            this.mDetailView = wLBDetailView;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            WLBModeItem item = getItem(i);
            this.mViewGroup = viewGroup;
            return createUserDetailItemView(view, viewGroup, item);
        }

        public WLBDeatailItemView createUserDetailItemView(View view, ViewGroup viewGroup, WLBModeItem wLBModeItem) {
            WLBDeatailItemView convertOrInflate = WLBDeatailItemView.convertOrInflate(this.mContext, view, viewGroup);
            if (convertOrInflate != view) {
                convertOrInflate.setOnClickListener(this);
            }
            convertOrInflate.bind(wLBModeItem.getModeName(), wLBModeItem.getPicture(), wLBModeItem.getTriggerName());
            convertOrInflate.updateThemeColor(wLBModeItem.isActive());
            convertOrInflate.setTag(wLBModeItem);
            setDescVisibility(convertOrInflate, this.mFullyExpanded ? 0 : 8, true, this.mFullyExpanded ? 1.0f : 0.0f);
            return convertOrInflate;
        }

        public void onClick(View view) {
            if (view.getTag() instanceof WLBModeItem) {
                WLBModeItem wLBModeItem = (WLBModeItem) view.getTag();
                int mode = wLBModeItem.getMode();
                updateActiveMode(mode, true);
                if (!wLBModeItem.isActive()) {
                    ((ShadeController) Dependency.get(ShadeController.class)).collapsePanel();
                }
                boolean[] zArr = {false, false, false};
                zArr[getPositionByMode(mode)] = true;
                for (int i = 0; i < getCount(); i++) {
                    getItem(i).setActive(zArr[i]);
                }
                WLBDetailView wLBDetailView = this.mDetailView;
                setLayoutDescVisibility((wLBDetailView == null || ((double) wLBDetailView.mExpansion) != 0.0d) ? 0 : 8, false, 0.0f);
            }
        }

        private void launchConfiguration(int i) {
            Intent intent = new Intent("com.oneplus.intent.ACTION_MODE_CONFIGURE");
            intent.setPackage("com.oneplus.opwlb");
            intent.putExtra("is_setup_mode", true);
            intent.putExtra("extra_mode", i);
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
        }

        private void sendBroadcastToApplication(int i) {
            Intent intent = new Intent("com.oneplus.intent.WLB_MANUAL_SELECTION");
            intent.setPackage("com.oneplus.opwlb");
            intent.putExtra("extra_mode", i);
            this.mContext.sendBroadcast(intent);
        }

        public void updateActiveMode(int i, boolean z) {
            String access$100 = WLBDetailView.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Updating mode ");
            sb.append(i);
            Log.i(access$100, sb.toString());
            int positionByMode = getPositionByMode(i);
            String str = "C22AG9UUDL";
            String str2 = "1";
            String str3 = "qt_mode_change_menu";
            if (i != 0) {
                if (i != 1) {
                    if (i != 2) {
                        return;
                    }
                } else if (z) {
                    OpMdmLogger.log(str3, "qt_menu_change_work", str2, str);
                }
                if (z && i == 2) {
                    OpMdmLogger.log(str3, "qt_menu_change_Life", str2, str);
                }
                if (getItem(positionByMode).isConfigured()) {
                    this.mController.setCurrentMode(i);
                    this.mController.doUnbindService();
                    this.mController.doBindService();
                    if (z) {
                        sendBroadcastToApplication(i);
                        return;
                    }
                    return;
                }
                ((ShadeController) Dependency.get(ShadeController.class)).collapsePanel();
                launchConfiguration(i);
                return;
            }
            if (z) {
                sendBroadcastToApplication(i);
                OpMdmLogger.log(str3, "qt_menu_change_none", str2, str);
            }
            this.mController.setCurrentMode(i);
            this.mController.doUnbindService();
            this.mController.doBindService();
        }

        public void updateUI(int i) {
            int i2;
            int i3;
            boolean[] zArr = {false, false, false};
            try {
                zArr[getPositionByMode(i)] = true;
                for (int i4 = 0; i4 < zArr.length; i4++) {
                    ImageView imageView = (ImageView) ((WLBDeatailItemView) this.mViewGroup.getChildAt(i4)).findViewById(R$id.user_picture);
                    if (zArr[i4]) {
                        imageView.setBackground(this.mContext.getDrawable(R$drawable.wlb_avathar_bg_enabled));
                        imageView.setImageTintList(ColorStateList.valueOf(this.mContext.getResources().getColor(R$color.oneplus_contorl_icon_color_accent_active_dark)));
                    } else {
                        int currentTheme = ThemeColorUtils.getCurrentTheme();
                        if (currentTheme == 0) {
                            i3 = R$drawable.wlb_avathar_bg_disabled_light;
                            i2 = R$color.oneplus_contorl_icon_color_active_light;
                        } else if (currentTheme == 1 || currentTheme == 2) {
                            i3 = R$drawable.wlb_avathar_bg_disabled_dark;
                            i2 = R$color.oneplus_contorl_icon_color_active_dark;
                        } else {
                            i3 = R$drawable.wlb_avathar_bg_disabled_light;
                            i2 = R$color.oneplus_contorl_icon_color_active_light;
                        }
                        imageView.setBackground(this.mContext.getDrawable(i3));
                        imageView.setImageTintList(ColorStateList.valueOf(this.mContext.getResources().getColor(i2)));
                    }
                }
            } catch (Exception e) {
                Log.d(WLBDetailView.TAG, "updateUI: exception caught", e);
            }
        }

        private int getPositionByMode(int i) {
            if (i == 1) {
                return 0;
            }
            if (i == 2) {
                return 1;
            }
            if (i == 0) {
                return 2;
            }
            Log.i(WLBDetailView.TAG, "invalid position");
            return -1;
        }

        public void updateExpansion(float f) {
            if (((double) f) != 0.0d) {
                setLayoutDescVisibility(0, true, f);
            }
        }

        public void setFullyExpanded(boolean z) {
            this.mFullyExpanded = z;
        }

        private void setLayoutDescVisibility(int i, boolean z, float f) {
            if (this.mViewGroup != null) {
                for (int i2 = 0; i2 < 3; i2++) {
                    setDescVisibility(this.mViewGroup.getChildAt(i2), i, z, f);
                }
            }
        }

        private void setDescVisibility(View view, int i, boolean z, float f) {
            if (view != null) {
                LinearLayout linearLayout = (LinearLayout) view.findViewById(R$id.layout_desc);
                linearLayout.setVisibility(i);
                if (z) {
                    linearLayout.setAlpha(f);
                    return;
                }
                return;
            }
            Log.d(WLBDetailView.TAG, "updateExpansion: Item is null");
        }
    }

    public void hideStatusBarIcon() {
    }

    public WLBDetailView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void createAndSetAdapter(WLBSwitchController wLBSwitchController) {
        this.mAdapter = new Adapter(this.mContext, wLBSwitchController, this);
        ViewGroupAdapterBridge.link(this, this.mAdapter);
        ((WLBSwitchController) Dependency.get(WLBSwitchController.class)).setDetailViewCallBack(this);
    }

    public static WLBDetailView inflate(Context context, ViewGroup viewGroup, boolean z) {
        return (WLBDetailView) LayoutInflater.from(context).inflate(R$layout.qs_wlb_detail_panel, viewGroup, z);
    }

    public void refreshAdapter(int i) {
        this.mAdapter.updateActiveMode(i, false);
    }

    public void onWLBModeChanged(int i) {
        Adapter adapter = this.mAdapter;
        if (adapter != null) {
            adapter.updateUI(i);
        }
    }

    public void onExpansionChanged(float f) {
        this.mExpansion = f;
        if (f > 0.0f) {
            this.mAdapter.updateExpansion(f);
        }
    }

    public void setIsFullyExpanded(boolean z) {
        this.mIsFullyExpanded = z;
        Adapter adapter = this.mAdapter;
        if (adapter != null) {
            adapter.setFullyExpanded(z);
        }
    }
}
