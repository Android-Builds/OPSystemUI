package com.android.systemui.p007qs.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SysUIToast;
import com.android.systemui.p007qs.PseudoGridView;
import com.android.systemui.p007qs.PseudoGridView.ViewGroupAdapterBridge;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.UserSwitcherController.BaseUserAdapter;
import com.android.systemui.statusbar.policy.UserSwitcherController.UserRecord;
import com.oneplus.systemui.util.OpMdmLogger;

/* renamed from: com.android.systemui.qs.tiles.UserDetailView */
public class UserDetailView extends PseudoGridView {
    protected Adapter mAdapter;

    /* renamed from: com.android.systemui.qs.tiles.UserDetailView$Adapter */
    public static class Adapter extends BaseUserAdapter implements OnClickListener {
        private final Context mContext;
        protected UserSwitcherController mController;

        public Adapter(Context context, UserSwitcherController userSwitcherController) {
            super(userSwitcherController);
            this.mContext = context;
            this.mController = userSwitcherController;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            return createUserDetailItemView(view, viewGroup, getItem(i));
        }

        public UserDetailItemView createUserDetailItemView(View view, ViewGroup viewGroup, UserRecord userRecord) {
            UserDetailItemView convertOrInflate = UserDetailItemView.convertOrInflate(this.mContext, view, viewGroup);
            if (convertOrInflate != view) {
                convertOrInflate.setOnClickListener(this);
            }
            String name = getName(this.mContext, userRecord);
            Bitmap bitmap = userRecord.picture;
            if (bitmap == null) {
                convertOrInflate.bind(name, getDrawable(this.mContext, userRecord), userRecord.resolveId());
            } else {
                convertOrInflate.bind(name, bitmap, userRecord.info.id);
            }
            convertOrInflate.updateThemeColor(userRecord.isAddUser);
            convertOrInflate.setActivated(userRecord.isCurrent);
            convertOrInflate.setDisabledByAdmin(userRecord.isDisabledByAdmin);
            userRecord.isStorageInsufficient = false;
            if (!userRecord.isSwitchToEnabled) {
                convertOrInflate.setEnabled(false);
            } else if (userRecord.isAddUser || (userRecord.isGuest && userRecord.info == null)) {
                long availableInternalMemorySize = UserDetailView.getAvailableInternalMemorySize();
                StringBuilder sb = new StringBuilder();
                sb.append("Available storage size=");
                sb.append(availableInternalMemorySize);
                sb.append(" bytes");
                String str = "UserDetailView";
                Log.d(str, sb.toString());
                if (availableInternalMemorySize < -1149239296) {
                    Log.d(str, "Storage size is too small, disable add user function");
                    convertOrInflate.setEnabled(false);
                    userRecord.isSwitchToEnabled = false;
                    userRecord.isStorageInsufficient = true;
                }
            }
            convertOrInflate.setTag(userRecord);
            return convertOrInflate;
        }

        public void onClick(View view) {
            UserRecord userRecord = (UserRecord) view.getTag();
            if (userRecord.isDisabledByAdmin) {
                this.mController.startActivity(RestrictedLockUtils.getShowAdminSupportDetailsIntent(this.mContext, userRecord.enforcedAdmin));
            } else if (userRecord.isSwitchToEnabled) {
                MetricsLogger.action(this.mContext, 156);
                String str = "1";
                String str2 = "quick_user";
                if (userRecord.isGuest) {
                    OpMdmLogger.log(str2, "guest", str);
                } else if (!userRecord.isAddUser) {
                    OpMdmLogger.log(str2, "switch", str);
                }
                switchTo(userRecord);
            } else if (userRecord.isStorageInsufficient) {
                Context context = this.mContext;
                SysUIToast.makeText(context, (CharSequence) context.getString(R$string.quick_settings_switch_user_storage_insufficient), 0).show();
            }
        }
    }

    public UserDetailView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public static UserDetailView inflate(Context context, ViewGroup viewGroup, boolean z) {
        return (UserDetailView) LayoutInflater.from(context).inflate(R$layout.qs_user_detail, viewGroup, z);
    }

    public void createAndSetAdapter(UserSwitcherController userSwitcherController) {
        this.mAdapter = new Adapter(this.mContext, userSwitcherController);
        ViewGroupAdapterBridge.link(this, this.mAdapter);
    }

    public void refreshAdapter() {
        this.mAdapter.refresh();
    }

    public static long getAvailableInternalMemorySize() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
    }
}
