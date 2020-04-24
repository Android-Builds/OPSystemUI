package android.support.p000v4.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.p000v4.util.Preconditions;
import android.view.LayoutInflater;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* renamed from: android.support.v4.app.FragmentHostCallback */
public abstract class FragmentHostCallback<E> extends FragmentContainer {
    private final Activity mActivity;
    private final Context mContext;
    final FragmentManagerImpl mFragmentManager;
    private final Handler mHandler;
    private final int mWindowAnimations;

    /* access modifiers changed from: 0000 */
    public abstract void onAttachFragment(Fragment fragment);

    public abstract void onDump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    public abstract LayoutInflater onGetLayoutInflater();

    public abstract int onGetWindowAnimations();

    public abstract boolean onHasWindowAnimations();

    public abstract boolean onShouldSaveFragmentState(Fragment fragment);

    public abstract void onSupportInvalidateOptionsMenu();

    FragmentHostCallback(FragmentActivity fragmentActivity) {
        this(fragmentActivity, fragmentActivity, fragmentActivity.mHandler, 0);
    }

    FragmentHostCallback(Activity activity, Context context, Handler handler, int i) {
        this.mFragmentManager = new FragmentManagerImpl();
        this.mActivity = activity;
        Preconditions.checkNotNull(context, "context == null");
        this.mContext = context;
        Preconditions.checkNotNull(handler, "handler == null");
        this.mHandler = handler;
        this.mWindowAnimations = i;
    }

    /* access modifiers changed from: 0000 */
    public Activity getActivity() {
        return this.mActivity;
    }

    /* access modifiers changed from: 0000 */
    public Context getContext() {
        return this.mContext;
    }

    /* access modifiers changed from: 0000 */
    public Handler getHandler() {
        return this.mHandler;
    }

    /* access modifiers changed from: 0000 */
    public FragmentManagerImpl getFragmentManagerImpl() {
        return this.mFragmentManager;
    }
}
