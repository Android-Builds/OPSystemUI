package androidx.fragment.app;

import androidx.lifecycle.Lifecycle.State;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public abstract class FragmentTransaction {
    boolean mAddToBackStack;
    boolean mAllowAddToBackStack = true;
    int mBreadCrumbShortTitleRes;
    CharSequence mBreadCrumbShortTitleText;
    int mBreadCrumbTitleRes;
    CharSequence mBreadCrumbTitleText;
    ArrayList<Runnable> mCommitRunnables;
    int mEnterAnim;
    int mExitAnim;
    String mName;
    ArrayList<C0289Op> mOps = new ArrayList<>();
    int mPopEnterAnim;
    int mPopExitAnim;
    boolean mReorderingAllowed = false;
    ArrayList<String> mSharedElementSourceNames;
    ArrayList<String> mSharedElementTargetNames;
    int mTransition;
    int mTransitionStyle;

    /* renamed from: androidx.fragment.app.FragmentTransaction$Op */
    static final class C0289Op {
        int mCmd;
        State mCurrentMaxState;
        int mEnterAnim;
        int mExitAnim;
        Fragment mFragment;
        State mOldMaxState;
        int mPopEnterAnim;
        int mPopExitAnim;

        C0289Op() {
        }

        C0289Op(int i, Fragment fragment) {
            this.mCmd = i;
            this.mFragment = fragment;
            State state = State.RESUMED;
            this.mOldMaxState = state;
            this.mCurrentMaxState = state;
        }
    }

    public abstract int commit();

    public abstract int commitAllowingStateLoss();

    /* access modifiers changed from: 0000 */
    public void addOp(C0289Op op) {
        this.mOps.add(op);
        op.mEnterAnim = this.mEnterAnim;
        op.mExitAnim = this.mExitAnim;
        op.mPopEnterAnim = this.mPopEnterAnim;
        op.mPopExitAnim = this.mPopExitAnim;
    }

    public FragmentTransaction add(Fragment fragment, String str) {
        doAddOp(0, fragment, str, 1);
        return this;
    }

    public FragmentTransaction add(int i, Fragment fragment, String str) {
        doAddOp(i, fragment, str, 1);
        return this;
    }

    /* access modifiers changed from: 0000 */
    public void doAddOp(int i, Fragment fragment, String str, int i2) {
        Class cls = fragment.getClass();
        int modifiers = cls.getModifiers();
        if (cls.isAnonymousClass() || !Modifier.isPublic(modifiers) || (cls.isMemberClass() && !Modifier.isStatic(modifiers))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fragment ");
            sb.append(cls.getCanonicalName());
            sb.append(" must be a public static class to be  properly recreated from instance state.");
            throw new IllegalStateException(sb.toString());
        }
        String str2 = " now ";
        String str3 = ": was ";
        if (str != null) {
            String str4 = fragment.mTag;
            if (str4 == null || str.equals(str4)) {
                fragment.mTag = str;
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Can't change tag of fragment ");
                sb2.append(fragment);
                sb2.append(str3);
                sb2.append(fragment.mTag);
                sb2.append(str2);
                sb2.append(str);
                throw new IllegalStateException(sb2.toString());
            }
        }
        if (i != 0) {
            if (i != -1) {
                int i3 = fragment.mFragmentId;
                if (i3 == 0 || i3 == i) {
                    fragment.mFragmentId = i;
                    fragment.mContainerId = i;
                } else {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Can't change container ID of fragment ");
                    sb3.append(fragment);
                    sb3.append(str3);
                    sb3.append(fragment.mFragmentId);
                    sb3.append(str2);
                    sb3.append(i);
                    throw new IllegalStateException(sb3.toString());
                }
            } else {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Can't add fragment ");
                sb4.append(fragment);
                sb4.append(" with tag ");
                sb4.append(str);
                sb4.append(" to container view with no id");
                throw new IllegalArgumentException(sb4.toString());
            }
        }
        addOp(new C0289Op(i2, fragment));
    }

    public FragmentTransaction remove(Fragment fragment) {
        addOp(new C0289Op(3, fragment));
        return this;
    }

    public FragmentTransaction detach(Fragment fragment) {
        addOp(new C0289Op(6, fragment));
        return this;
    }

    public FragmentTransaction attach(Fragment fragment) {
        addOp(new C0289Op(7, fragment));
        return this;
    }
}
