package com.android.systemui.statusbar.phone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.systemui.Dumpable;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.p007qs.QSPanel;
import com.android.systemui.plugins.p006qs.DetailAdapter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class WLBSwitchController implements Dumpable {
    /* access modifiers changed from: private */
    public static final String TAG = "WLBSwitchController";
    private boolean isAdminUser = true;
    private boolean isWLBDetailClosed = true;
    /* access modifiers changed from: private */
    public final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private WLBControllerCallbacks mCallBacks;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                WLBSwitchController.this.mService = new Messenger(iBinder);
                WLBSwitchController.this.mIsBound = true;
                Message obtain = Message.obtain(null, 1);
                obtain.replyTo = WLBSwitchController.this.mMessenger;
                WLBSwitchController.this.mService.send(obtain);
                Message obtain2 = Message.obtain();
                obtain2.what = 5;
                WLBSwitchController.this.mService.send(obtain2);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            WLBSwitchController.this.mService = null;
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    private int mCurrentMode = 0;
    /* access modifiers changed from: private */
    public WLBControllerCallbacks mDetailViewCallBack;
    private float mHeaderExpansion = -1.0f;
    /* access modifiers changed from: private */
    public boolean mIsBound;
    /* access modifiers changed from: private */
    public boolean mIsFullyExpanded;
    /* access modifiers changed from: private */
    public final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ArrayList<WLBModeItem> mModes = new ArrayList<>();
    private float mPreviousExpansion = 0.0f;
    /* access modifiers changed from: private */
    public QSPanel mQsPanel;
    /* access modifiers changed from: private */
    public Messenger mService = null;
    private WLBSwitchCallbacks mSwitchCallbacks;
    public final DetailAdapter wlbDetailAdapter = new DetailAdapter() {
        private Intent wlbSettingsIntent = new Intent("com.oneplus.intent.ACTION_LAUNCH_WLB");

        public int getMetricsCategory() {
            return 2006;
        }

        public Boolean getToggleState() {
            return null;
        }

        public void setToggleState(boolean z) {
        }

        public CharSequence getTitle() {
            return WLBSwitchController.this.mContext.getString(R$string.work_life_balance_mode);
        }

        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            WLBDetailView wLBDetailView;
            if (!(view instanceof WLBDetailView)) {
                wLBDetailView = WLBDetailView.inflate(context, viewGroup, false);
                wLBDetailView.createAndSetAdapter(WLBSwitchController.this);
            } else {
                wLBDetailView = (WLBDetailView) view;
            }
            wLBDetailView.refreshAdapter(getActiveMode());
            wLBDetailView.setIsFullyExpanded(WLBSwitchController.this.mIsFullyExpanded);
            return wLBDetailView;
        }

        public Intent getSettingsIntent() {
            this.wlbSettingsIntent.putExtra("launch_from_system_ui", true);
            return this.wlbSettingsIntent;
        }

        public int getActiveMode() {
            return System.getInt(WLBSwitchController.this.mContext.getContentResolver(), "oneplus_wlb_activated_mode", 0);
        }
    };

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final WLBSwitchController mController;

        protected BaseUserAdapter(WLBSwitchController wLBSwitchController) {
            this.mController = wLBSwitchController;
            wLBSwitchController.addAdapter(new WeakReference(this));
        }

        public int getCount() {
            return this.mController.getModes().size();
        }

        public WLBModeItem getItem(int i) {
            return (WLBModeItem) this.mController.getModes().get(i);
        }

        public long getItemId(int i) {
            return (long) ((WLBModeItem) this.mController.getModes().get(i)).getMode();
        }
    }

    class IncomingHandler extends Handler {
        IncomingHandler() {
        }

        /* JADX WARNING: type inference failed for: r5v0 */
        /* JADX WARNING: type inference failed for: r5v1, types: [int] */
        /* JADX WARNING: type inference failed for: r5v2, types: [int] */
        /* JADX WARNING: type inference failed for: r5v3, types: [boolean] */
        /* JADX WARNING: type inference failed for: r5v4 */
        /* JADX WARNING: type inference failed for: r5v5, types: [boolean] */
        /* JADX WARNING: type inference failed for: r5v6 */
        /* JADX WARNING: type inference failed for: r5v7 */
        /* JADX WARNING: type inference failed for: r5v8 */
        /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r5v4
          assigns: [?[int, float, boolean, short, byte, char, OBJECT, ARRAY], int]
          uses: [boolean, ?[int, byte, short, char], int]
          mth insns count: 125
        	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
        	at jadx.core.ProcessClass.process(ProcessClass.java:30)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:49)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:49)
        	at jadx.core.ProcessClass.process(ProcessClass.java:35)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
         */
        /* JADX WARNING: Unknown variable types count: 3 */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r10) {
            /*
                r9 = this;
                int r0 = r10.what
                r1 = 5
                if (r0 == r1) goto L_0x000a
                super.handleMessage(r10)
                goto L_0x0153
            L_0x000a:
                java.lang.Object r10 = r10.obj
                android.os.Bundle r10 = (android.os.Bundle) r10
                java.lang.String r0 = "configured"
                boolean[] r0 = r10.getBooleanArray(r0)
                java.lang.String r1 = "trigger_values_work"
                java.lang.String[] r1 = r10.getStringArray(r1)
                java.lang.String r2 = "trigger_values_life"
                java.lang.String[] r10 = r10.getStringArray(r2)
                java.lang.String r2 = com.android.systemui.statusbar.phone.WLBSwitchController.TAG
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = " trigger values work "
                r3.append(r4)
                java.lang.String r4 = java.util.Arrays.toString(r1)
                r3.append(r4)
                java.lang.String r4 = " life "
                r3.append(r4)
                java.lang.String r4 = java.util.Arrays.toString(r10)
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                android.util.Log.i(r2, r3)
                com.android.systemui.statusbar.phone.WLBSwitchController r2 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                java.util.ArrayList r2 = r2.getModes()
                java.util.Iterator r2 = r2.iterator()
            L_0x0054:
                boolean r3 = r2.hasNext()
                r4 = 2
                r5 = 0
                r6 = 1
                if (r3 == 0) goto L_0x00d2
                java.lang.Object r3 = r2.next()
                com.android.systemui.statusbar.phone.WLBSwitchController$WLBModeItem r3 = (com.android.systemui.statusbar.phone.WLBSwitchController.WLBModeItem) r3
                int r7 = r3.getMode()
                if (r7 != r6) goto L_0x0096
                boolean r4 = r0[r5]
                r3.setConfigured(r4)
                boolean r4 = r0[r5]
                if (r4 == 0) goto L_0x007a
                java.lang.String r4 = r9.getTriggerName(r1)
                r3.setTriggerName(r4)
                goto L_0x0089
            L_0x007a:
                com.android.systemui.statusbar.phone.WLBSwitchController r4 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                android.content.Context r4 = r4.mContext
                int r7 = com.android.systemui.R$string.qs_panel_set_up
                java.lang.String r4 = r4.getString(r7)
                r3.setTriggerName(r4)
            L_0x0089:
                com.android.systemui.statusbar.phone.WLBSwitchController r4 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                int r4 = r4.getCurrentMode()
                if (r4 != r6) goto L_0x0092
                r5 = r6
            L_0x0092:
                r3.setActive(r5)
                goto L_0x0054
            L_0x0096:
                int r7 = r3.getMode()
                if (r7 != r4) goto L_0x00c9
                boolean r7 = r0[r6]
                r3.setConfigured(r7)
                boolean r7 = r0[r6]
                if (r7 == 0) goto L_0x00ad
                java.lang.String r7 = r9.getTriggerName(r10)
                r3.setTriggerName(r7)
                goto L_0x00bc
            L_0x00ad:
                com.android.systemui.statusbar.phone.WLBSwitchController r7 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                android.content.Context r7 = r7.mContext
                int r8 = com.android.systemui.R$string.qs_panel_set_up
                java.lang.String r7 = r7.getString(r8)
                r3.setTriggerName(r7)
            L_0x00bc:
                com.android.systemui.statusbar.phone.WLBSwitchController r7 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                int r7 = r7.getCurrentMode()
                if (r7 != r4) goto L_0x00c5
                r5 = r6
            L_0x00c5:
                r3.setActive(r5)
                goto L_0x0054
            L_0x00c9:
                r3.setConfigured(r5)
                java.lang.String r4 = ""
                r3.setTriggerName(r4)
                goto L_0x0054
            L_0x00d2:
                com.android.systemui.statusbar.phone.WLBSwitchController r0 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                r0.doUnbindService()
                r0 = 3
                boolean[] r0 = new boolean[r0]
                r0 = {0, 0, 0} // fill-array
                com.android.systemui.statusbar.phone.WLBSwitchController r2 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                int r2 = r2.getCurrentMode()
                if (r2 != r6) goto L_0x00f5
                r10 = r5
            L_0x00e6:
                int r2 = r0.length
                if (r10 >= r2) goto L_0x010d
                r2 = r1[r10]
                boolean r2 = android.text.TextUtils.isEmpty(r2)
                r2 = r2 ^ r6
                r0[r10] = r2
                int r10 = r10 + 1
                goto L_0x00e6
            L_0x00f5:
                com.android.systemui.statusbar.phone.WLBSwitchController r1 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                int r1 = r1.getCurrentMode()
                if (r1 != r4) goto L_0x010d
                r1 = r5
            L_0x00fe:
                int r2 = r0.length
                if (r1 >= r2) goto L_0x010d
                r2 = r10[r1]
                boolean r2 = android.text.TextUtils.isEmpty(r2)
                r2 = r2 ^ r6
                r0[r1] = r2
                int r1 = r1 + 1
                goto L_0x00fe
            L_0x010d:
                com.android.systemui.statusbar.phone.WLBSwitchController r10 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                com.android.systemui.qs.QSPanel r10 = r10.mQsPanel
                r10.updateWLBIndicators(r0)
            L_0x0116:
                com.android.systemui.statusbar.phone.WLBSwitchController r10 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                java.util.ArrayList r10 = r10.mAdapters
                int r10 = r10.size()
                if (r5 >= r10) goto L_0x013c
                com.android.systemui.statusbar.phone.WLBSwitchController r10 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                java.util.ArrayList r10 = r10.mAdapters
                java.lang.Object r10 = r10.get(r5)
                java.lang.ref.WeakReference r10 = (java.lang.ref.WeakReference) r10
                java.lang.Object r10 = r10.get()
                com.android.systemui.statusbar.phone.WLBSwitchController$BaseUserAdapter r10 = (com.android.systemui.statusbar.phone.WLBSwitchController.BaseUserAdapter) r10
                if (r10 == 0) goto L_0x0139
                r10.notifyDataSetChanged()
            L_0x0139:
                int r5 = r5 + 1
                goto L_0x0116
            L_0x013c:
                com.android.systemui.statusbar.phone.WLBSwitchController r10 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                com.android.systemui.statusbar.phone.WLBSwitchController$WLBControllerCallbacks r10 = r10.mDetailViewCallBack
                if (r10 == 0) goto L_0x0153
                com.android.systemui.statusbar.phone.WLBSwitchController r10 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                com.android.systemui.statusbar.phone.WLBSwitchController$WLBControllerCallbacks r10 = r10.mDetailViewCallBack
                com.android.systemui.statusbar.phone.WLBSwitchController r9 = com.android.systemui.statusbar.phone.WLBSwitchController.this
                int r9 = r9.getCurrentMode()
                r10.onWLBModeChanged(r9)
            L_0x0153:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.WLBSwitchController.IncomingHandler.handleMessage(android.os.Message):void");
        }

        private String getTriggerName(String[] strArr) {
            for (String str : strArr) {
                if (!TextUtils.isEmpty(str)) {
                    return str;
                }
            }
            return "";
        }
    }

    public interface WLBControllerCallbacks {
        void hideStatusBarIcon();

        void onExpansionChanged(float f);

        void onWLBModeChanged(int i);
    }

    public static final class WLBModeItem {
        private boolean isActive;
        private boolean isConfigured;
        private int mode;
        private String modeName;
        private Drawable picture = null;
        private String triggerName;

        public String getTriggerName() {
            return this.triggerName;
        }

        public void setTriggerName(String str) {
            this.triggerName = str;
        }

        public boolean isConfigured() {
            return this.isConfigured;
        }

        public void setConfigured(boolean z) {
            this.isConfigured = z;
        }

        public void setPicture(Drawable drawable) {
            this.picture = drawable;
        }

        public boolean isActive() {
            return this.isActive;
        }

        public void setActive(boolean z) {
            this.isActive = z;
        }

        public String getModeName() {
            return this.modeName;
        }

        public void setModeName(String str) {
            this.modeName = str;
        }

        public int getMode() {
            return this.mode;
        }

        public void setMode(int i) {
            this.mode = i;
        }

        public Drawable getPicture() {
            return this.picture;
        }
    }

    public interface WLBSwitchCallbacks {
        void updateSwitchState();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    public WLBSwitchController(Context context) {
        this.mContext = context;
        setCurrentMode(System.getInt(this.mContext.getContentResolver(), "oneplus_wlb_activated_mode", 0));
        processModes();
    }

    private void processModes() {
        for (int i = 0; i < 3; i++) {
            WLBModeItem wLBModeItem = new WLBModeItem();
            boolean z = true;
            if (i == 0) {
                wLBModeItem.setModeName(this.mContext.getString(R$string.qs_panel_work_mode));
                wLBModeItem.setMode(1);
                wLBModeItem.setPicture(this.mContext.getDrawable(R$drawable.qs_panel_work_icon));
                wLBModeItem.setActive(getCurrentMode() == 1);
                if (System.getInt(this.mContext.getContentResolver(), "work_configured", 0) != 1) {
                    z = false;
                }
                wLBModeItem.setConfigured(z);
            } else if (i == 1) {
                wLBModeItem.setModeName(this.mContext.getString(R$string.qs_panel_life_mode));
                wLBModeItem.setMode(2);
                wLBModeItem.setPicture(this.mContext.getDrawable(R$drawable.qs_panel_life_icon));
                wLBModeItem.setActive(getCurrentMode() == 2);
                if (System.getInt(this.mContext.getContentResolver(), "life_configured", 0) != 1) {
                    z = false;
                }
                wLBModeItem.setConfigured(z);
            } else if (i == 2) {
                wLBModeItem.setModeName(this.mContext.getString(R$string.qs_panel_none));
                wLBModeItem.setMode(0);
                wLBModeItem.setPicture(this.mContext.getDrawable(R$drawable.qs_panel_off_icon));
                if (getCurrentMode() != 0) {
                    z = false;
                }
                wLBModeItem.setActive(z);
            }
            this.mModes.add(wLBModeItem);
        }
    }

    public void setAdminUser(boolean z) {
        this.isAdminUser = z;
    }

    public int getCurrentMode() {
        return this.mCurrentMode;
    }

    public void setCurrentMode(int i) {
        this.mCurrentMode = i;
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
    }

    public void setIsFullyExpanded(boolean z) {
        this.mIsFullyExpanded = z;
    }

    public ArrayList<WLBModeItem> getModes() {
        return this.mModes;
    }

    /* access modifiers changed from: private */
    public void addAdapter(WeakReference<BaseUserAdapter> weakReference) {
        this.mAdapters.add(weakReference);
    }

    /* access modifiers changed from: 0000 */
    public void doBindService() {
        Intent intent = new Intent("com.oneplus.intent.WLB_BIND_SERVICE");
        intent.setPackage("com.oneplus.opwlb");
        intent.putExtra("extra_mode", this.mCurrentMode);
        this.mContext.bindService(intent, this.mConnection, 1);
    }

    /* access modifiers changed from: 0000 */
    public void doUnbindService() {
        if (this.mIsBound) {
            if (this.mService != null) {
                try {
                    Message obtain = Message.obtain(null, 2);
                    obtain.replyTo = this.mMessenger;
                    this.mService.send(obtain);
                } catch (RemoteException unused) {
                }
            }
            this.mContext.unbindService(this.mConnection);
            this.mIsBound = false;
        }
    }

    public void onWLBModeChanged() {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onWLBModeChanged WLB controller mCallBacks ");
        sb.append(this.mCallBacks);
        sb.append(" mDetailViewCallBack ");
        sb.append(this.mDetailViewCallBack);
        Log.d(str, sb.toString());
        if (this.isAdminUser) {
            WLBControllerCallbacks wLBControllerCallbacks = this.mCallBacks;
            if (wLBControllerCallbacks != null) {
                wLBControllerCallbacks.onWLBModeChanged(getCurrentMode());
            }
            WLBControllerCallbacks wLBControllerCallbacks2 = this.mDetailViewCallBack;
            if (wLBControllerCallbacks2 != null) {
                wLBControllerCallbacks2.onWLBModeChanged(getCurrentMode());
            }
        }
    }

    public void hideStatusBarIcon() {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("hideStatusBarIcon: Callbacks ");
        sb.append(this.mCallBacks);
        Log.d(str, sb.toString());
        WLBControllerCallbacks wLBControllerCallbacks = this.mCallBacks;
        if (wLBControllerCallbacks != null) {
            wLBControllerCallbacks.hideStatusBarIcon();
        }
    }

    public void setCallBacks(WLBControllerCallbacks wLBControllerCallbacks) {
        this.mCallBacks = wLBControllerCallbacks;
    }

    public void setDetailViewCallBack(WLBControllerCallbacks wLBControllerCallbacks) {
        this.mDetailViewCallBack = wLBControllerCallbacks;
    }

    public void updateExpansionState(float f) {
        if (this.mPreviousExpansion != f) {
            this.mPreviousExpansion = f;
            WLBControllerCallbacks wLBControllerCallbacks = this.mDetailViewCallBack;
            if (wLBControllerCallbacks != null) {
                wLBControllerCallbacks.onExpansionChanged(f);
            }
            WLBSwitchCallbacks wLBSwitchCallbacks = this.mSwitchCallbacks;
            if (wLBSwitchCallbacks != null) {
                wLBSwitchCallbacks.updateSwitchState();
            }
        }
    }

    public void updateHeaderExpansion(float f) {
        if (f != this.mHeaderExpansion) {
            this.mHeaderExpansion = f;
            if (((double) f) == 0.0d || this.isWLBDetailClosed) {
                this.isWLBDetailClosed = false;
                Log.d(TAG, "QS panel detail opened");
            } else if (this.mQsPanel != null) {
                Log.d(TAG, "QS panel detail closed");
                this.mQsPanel.closeDetail();
                this.isWLBDetailClosed = true;
            }
        }
    }

    public void setSwitchCallbacks(WLBSwitchCallbacks wLBSwitchCallbacks) {
        this.mSwitchCallbacks = wLBSwitchCallbacks;
    }
}
