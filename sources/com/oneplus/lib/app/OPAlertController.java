package com.oneplus.lib.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import java.lang.ref.WeakReference;

public class OPAlertController {
    private ListAdapter mAdapter;
    private int mAlertDialogLayout;
    private final OnClickListener mButtonHandler = new OnClickListener() {
        public void onClick(View view) {
            Message message = (view != OPAlertController.this.mButtonPositive || OPAlertController.this.mButtonPositiveMessage == null) ? (view != OPAlertController.this.mButtonNegative || OPAlertController.this.mButtonNegativeMessage == null) ? (view != OPAlertController.this.mButtonNeutral || OPAlertController.this.mButtonNeutralMessage == null) ? null : Message.obtain(OPAlertController.this.mButtonNeutralMessage) : Message.obtain(OPAlertController.this.mButtonNegativeMessage) : Message.obtain(OPAlertController.this.mButtonPositiveMessage);
            if (message != null) {
                message.sendToTarget();
            }
            OPAlertController.this.mHandler.obtainMessage(1, OPAlertController.this.mDialogInterface).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public Button mButtonNegative;
    /* access modifiers changed from: private */
    public Message mButtonNegativeMessage;
    private CharSequence mButtonNegativeText;
    /* access modifiers changed from: private */
    public Button mButtonNeutral;
    /* access modifiers changed from: private */
    public Message mButtonNeutralMessage;
    private CharSequence mButtonNeutralText;
    /* access modifiers changed from: private */
    public Button mButtonPositive;
    /* access modifiers changed from: private */
    public Message mButtonPositiveMessage;
    private CharSequence mButtonPositiveText;
    private int mCheckedItem = -1;
    /* access modifiers changed from: private */
    public final Context mContext;
    private View mCustomTitleView;
    /* access modifiers changed from: private */
    public final DialogInterface mDialogInterface;
    private boolean mForceInverseBackground;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private Drawable mIcon;
    private int mIconId = 0;
    private ImageView mIconView;
    private int mListItemLayout;
    private int mListLayout;
    private ListView mListView;
    private CharSequence mMessage;
    private TextView mMessageView;
    private int mMultiChoiceItemLayout;
    private boolean mOnlyDarkTheme;
    private boolean mOnlyLightTheme;
    private int mProgressStyle = -1;
    private ScrollView mScrollView;
    private int mSingleChoiceItemLayout;
    private CharSequence mTitle;
    private TextView mTitleView;
    private LinearLayout mTitle_template;
    private View mView;
    private int mViewLayoutResId;
    private int mViewSpacingBottom;
    private int mViewSpacingLeft;
    private int mViewSpacingRight;
    private boolean mViewSpacingSpecified = false;
    private int mViewSpacingTop;
    private final Window mWindow;

    public static class AlertParams {
        public boolean mCancelable;
        public int mCheckedItem = -1;
        public final Context mContext;
        public Drawable mIcon;
        public int mIconAttrId = 0;
        public int mIconId = 0;
        public final LayoutInflater mInflater;
        public boolean mIsSingleChoice;
        public CharSequence[] mItems;
        public CharSequence mMessage;
        public DialogInterface.OnClickListener mNegativeButtonListener;
        public CharSequence mNegativeButtonText;
        public DialogInterface.OnClickListener mOnClickListener;
        public boolean mOnlyDarkTheme = false;
        public boolean mOnlyLightTheme = false;
        public DialogInterface.OnClickListener mPositiveButtonListener;
        public CharSequence mPositiveButtonText;
        public boolean mRecycleOnMeasure = true;
        public CharSequence mTitle;
        public View mView;
        public int mViewLayoutResId;
        public boolean mViewSpacingSpecified = false;

        public AlertParams(Context context) {
            this.mContext = context;
            this.mCancelable = true;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }
    }

    private static final class ButtonHandler extends Handler {
        private WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialogInterface) {
            this.mDialog = new WeakReference<>(dialogInterface);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == -3 || i == -2 || i == -1) {
                ((DialogInterface.OnClickListener) message.obj).onClick((DialogInterface) this.mDialog.get(), message.what);
            } else if (i == 1) {
                ((DialogInterface) message.obj).dismiss();
            }
        }
    }

    public static class RecycleListView extends ListView {
        boolean mRecycleOnMeasure = true;

        public RecycleListView(Context context) {
            super(context);
        }

        public RecycleListView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public RecycleListView(Context context, AttributeSet attributeSet, int i) {
            super(context, attributeSet, i);
        }

        public RecycleListView(Context context, AttributeSet attributeSet, int i, int i2) {
            super(context, attributeSet, i, i2);
        }

        /* access modifiers changed from: protected */
        public boolean recycleOnMeasure() {
            return this.mRecycleOnMeasure;
        }
    }

    private static boolean shouldCenterSingleButton(Context context) {
        return false;
    }

    public OPAlertController(Context context, DialogInterface dialogInterface, Window window) {
        this.mContext = context;
        this.mDialogInterface = dialogInterface;
        Log.i("OPAlertController", "OPAlertController start !!!");
        this.mWindow = window;
        this.mHandler = new ButtonHandler(dialogInterface);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(null, R$styleable.OPAlertDialog, R$attr.OPAlertDialogStyle, 0);
        this.mAlertDialogLayout = obtainStyledAttributes.getResourceId(R$styleable.OPAlertDialog_android_layout, R$layout.op_alert_dialog_material);
        this.mListLayout = obtainStyledAttributes.getResourceId(R$styleable.OPAlertDialog_op_listLayout, R$layout.op_select_dialog_material);
        this.mMultiChoiceItemLayout = obtainStyledAttributes.getResourceId(R$styleable.OPAlertDialog_op_multiChoiceItemLayout, R$layout.op_select_dialog_multichoice_material);
        this.mSingleChoiceItemLayout = obtainStyledAttributes.getResourceId(R$styleable.OPAlertDialog_op_singleChoiceItemLayout, R$layout.op_select_dialog_singlechoice_material);
        this.mListItemLayout = obtainStyledAttributes.getResourceId(R$styleable.OPAlertDialog_op_listItemLayout, R$layout.op_select_dialog_item_material);
        obtainStyledAttributes.recycle();
    }

    static boolean canTextInput(View view) {
        if (view.onCheckIsTextEditor()) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        while (childCount > 0) {
            childCount--;
            if (canTextInput(viewGroup.getChildAt(childCount))) {
                return true;
            }
        }
        return false;
    }

    public void installContent() {
        this.mWindow.requestFeature(1);
        this.mWindow.setContentView(this.mAlertDialogLayout);
        setupView();
        setupDecor();
    }

    public void setTitle(CharSequence charSequence) {
        this.mTitle = charSequence;
        TextView textView = this.mTitleView;
        if (textView != null) {
            textView.setText(charSequence);
        }
        updateMessageView();
    }

    public void setMessage(CharSequence charSequence) {
        this.mMessage = charSequence;
        TextView textView = this.mMessageView;
        if (textView != null) {
            textView.setText(charSequence);
        }
        updateMessageView();
    }

    private void updateTitleView() {
        updateMessageView();
        if (this.mTitleView != null) {
            boolean z = false;
            boolean z2 = !TextUtils.isEmpty(this.mMessage);
            if (!(this.mIconId == 0 && this.mIcon == null && this.mListView == null && z2)) {
                z = true;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("isBold : ");
            sb.append(z);
            String str = "OPAlertController";
            Log.i(str, sb.toString());
            if (z) {
                this.mTitleView.setTextAppearance(this.mContext, R$style.oneplus_contorl_text_style_title);
            } else {
                LinearLayout linearLayout = this.mTitle_template;
                if (linearLayout != null) {
                    linearLayout.setPadding(linearLayout.getPaddingStart(), this.mTitle_template.getPaddingTop(), this.mTitle_template.getPaddingEnd(), this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_bottom1));
                }
            }
            if (this.mOnlyDarkTheme) {
                this.mTitleView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_primary_dark));
            } else if (this.mOnlyLightTheme) {
                this.mTitleView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_primary_light));
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mTitleView.getTextSize() : ");
            sb2.append(this.mTitleView.getTextSize());
            Log.i(str, sb2.toString());
        }
    }

    private ColorStateList getColorCompat(int i) {
        if (VERSION.SDK_INT > 23) {
            return this.mContext.getResources().getColorStateList(i, this.mContext.getTheme());
        }
        return this.mContext.getResources().getColorStateList(i);
    }

    private void updateMessageView() {
        int i;
        int i2;
        if (this.mMessageView != null) {
            if (!TextUtils.isEmpty(this.mTitle)) {
                i2 = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_top1);
                i = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_bottom1);
                if (this.mIconId == 0 && this.mIcon == null && this.mListView == null) {
                    i += this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_top2);
                }
                this.mMessageView.setTextAppearance(this.mContext, R$style.oneplus_contorl_text_style_body1);
                if (this.mOnlyDarkTheme) {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_secondary_dark));
                } else if (this.mOnlyLightTheme) {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_secondary_light));
                } else {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_secondary_default));
                }
            } else {
                i2 = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_top3);
                i = this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_layout_margin_bottom2);
                this.mMessageView.setTextAppearance(this.mContext, R$style.oneplus_contorl_text_style_subheading);
                if (this.mOnlyDarkTheme) {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_primary_dark));
                } else if (this.mOnlyLightTheme) {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_primary_light));
                } else {
                    this.mMessageView.setTextColor(getColorCompat(R$color.oneplus_contorl_text_color_primary_default));
                }
            }
            this.mMessageView.setPadding(0, i2, 0, i);
        }
    }

    public void setButton(int i, CharSequence charSequence, DialogInterface.OnClickListener onClickListener, Message message) {
        if (message == null && onClickListener != null) {
            message = this.mHandler.obtainMessage(i, onClickListener);
        }
        if (i == -3) {
            this.mButtonNeutralText = charSequence;
            this.mButtonNeutralMessage = message;
        } else if (i == -2) {
            this.mButtonNegativeText = charSequence;
            this.mButtonNegativeMessage = message;
        } else if (i == -1) {
            this.mButtonPositiveText = charSequence;
            this.mButtonPositiveMessage = message;
        } else {
            throw new IllegalArgumentException("Button does not exist");
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        ScrollView scrollView = this.mScrollView;
        return scrollView != null && scrollView.executeKeyEvent(keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        ScrollView scrollView = this.mScrollView;
        return scrollView != null && scrollView.executeKeyEvent(keyEvent);
    }

    private void setupDecor() {
        View decorView = this.mWindow.getDecorView();
        final View findViewById = this.mWindow.findViewById(R$id.parentPanel);
        if (findViewById != null && decorView != null) {
            decorView.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    if (windowInsets.isRound()) {
                        int dimensionPixelOffset = OPAlertController.this.mContext.getResources().getDimensionPixelOffset(R$dimen.alert_dialog_round_padding);
                        findViewById.setPadding(dimensionPixelOffset, dimensionPixelOffset, dimensionPixelOffset, dimensionPixelOffset);
                    }
                    return windowInsets.consumeSystemWindowInsets();
                }
            });
            decorView.setFitsSystemWindows(true);
            decorView.requestApplyInsets();
        }
    }

    private ViewGroup resolvePanel(View view, View view2) {
        if (view == null) {
            if (view2 instanceof ViewStub) {
                view2 = ((ViewStub) view2).inflate();
            }
            return (ViewGroup) view2;
        }
        if (view2 != null) {
            ViewParent parent = view2.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(view2);
            }
        }
        if (view instanceof ViewStub) {
            view = ((ViewStub) view).inflate();
        }
        return (ViewGroup) view;
    }

    private void setupView() {
        View findViewById = this.mWindow.findViewById(R$id.parentPanel);
        View findViewById2 = findViewById.findViewById(R$id.topPanel);
        View findViewById3 = findViewById.findViewById(R$id.contentPanel);
        View findViewById4 = findViewById.findViewById(R$id.buttonPanel);
        ViewGroup viewGroup = (ViewGroup) findViewById.findViewById(R$id.customPanel);
        setupCustomContent(viewGroup);
        View findViewById5 = viewGroup.findViewById(R$id.topPanel);
        View findViewById6 = viewGroup.findViewById(R$id.contentPanel);
        View findViewById7 = viewGroup.findViewById(R$id.buttonPanel);
        ViewGroup resolvePanel = resolvePanel(findViewById5, findViewById2);
        ViewGroup resolvePanel2 = resolvePanel(findViewById6, findViewById3);
        ViewGroup resolvePanel3 = resolvePanel(findViewById7, findViewById4);
        setupContent(resolvePanel2);
        setupButtons(resolvePanel3);
        setupTitle(resolvePanel);
        boolean z = (viewGroup == null || viewGroup.getVisibility() == 8) ? false : true;
        boolean z2 = (resolvePanel == null || resolvePanel.getVisibility() == 8) ? false : true;
        boolean z3 = (resolvePanel3 == null || resolvePanel3.getVisibility() == 8) ? false : true;
        if (!z3 && resolvePanel2 != null) {
            View findViewById8 = resolvePanel2.findViewById(R$id.textSpacerNoButtons);
            if (findViewById8 != null && TextUtils.isEmpty(this.mTitle)) {
                findViewById8.setVisibility(0);
            }
        }
        if (z2) {
            ScrollView scrollView = this.mScrollView;
            if (scrollView != null) {
                scrollView.setClipToPadding(true);
            }
        }
        if (!z) {
            View view = this.mListView;
            if (view == null) {
                view = this.mScrollView;
            }
            if (view != null) {
                view.setScrollIndicators((z3 ? (char) 2 : 0) | z2 ? 1 : 0, 3);
            }
        }
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R$styleable.OPAlertDialog, 16842845, 0);
        setBackground(obtainStyledAttributes, resolvePanel, resolvePanel2, viewGroup, resolvePanel3, z2, z, z3);
        obtainStyledAttributes.recycle();
    }

    private void setupCustomContent(ViewGroup viewGroup) {
        View view = this.mView;
        boolean z = false;
        if (view == null) {
            view = this.mViewLayoutResId != 0 ? LayoutInflater.from(this.mContext).inflate(this.mViewLayoutResId, viewGroup, false) : null;
        }
        if (view != null) {
            z = true;
        }
        if (!z || !canTextInput(view)) {
            this.mWindow.setFlags(131072, 131072);
        }
        if (z) {
            FrameLayout frameLayout = (FrameLayout) this.mWindow.findViewById(16908331);
            frameLayout.addView(view, new LayoutParams(-1, -1));
            if (this.mViewSpacingSpecified) {
                frameLayout.setPadding(this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
            }
            if (this.mListView != null) {
                ((LinearLayout.LayoutParams) viewGroup.getLayoutParams()).weight = 0.0f;
                return;
            }
            return;
        }
        viewGroup.setVisibility(8);
    }

    private void setupTitle(ViewGroup viewGroup) {
        if (this.mCustomTitleView != null) {
            viewGroup.addView(this.mCustomTitleView, 0, new LayoutParams(-1, -2));
            this.mWindow.findViewById(R$id.title_template).setVisibility(8);
            return;
        }
        this.mIconView = (ImageView) this.mWindow.findViewById(16908294);
        this.mTitle_template = (LinearLayout) this.mWindow.findViewById(R$id.title_template);
        if (!TextUtils.isEmpty(this.mTitle)) {
            this.mTitleView = (TextView) this.mWindow.findViewById(R$id.alertTitle);
            this.mTitleView.setText(this.mTitle);
            int i = this.mIconId;
            if (i != 0) {
                this.mIconView.setImageResource(i);
            } else {
                Drawable drawable = this.mIcon;
                if (drawable != null) {
                    this.mIconView.setImageDrawable(drawable);
                } else {
                    this.mTitleView.setPadding(this.mIconView.getPaddingLeft(), this.mIconView.getPaddingTop(), this.mIconView.getPaddingRight(), this.mIconView.getPaddingBottom());
                    this.mIconView.setVisibility(8);
                }
            }
        } else {
            this.mWindow.findViewById(R$id.title_template).setVisibility(8);
            this.mIconView.setVisibility(8);
            viewGroup.setVisibility(8);
        }
        updateTitleView();
    }

    private void setupContent(ViewGroup viewGroup) {
        this.mScrollView = (ScrollView) viewGroup.findViewById(R$id.scrollView);
        this.mScrollView.setFocusable(false);
        this.mMessageView = (TextView) viewGroup.findViewById(16908299);
        TextView textView = this.mMessageView;
        if (textView != null) {
            CharSequence charSequence = this.mMessage;
            if (charSequence != null) {
                textView.setText(charSequence);
            } else {
                textView.setVisibility(8);
                this.mScrollView.removeView(this.mMessageView);
                if (this.mListView != null) {
                    ViewGroup viewGroup2 = (ViewGroup) this.mScrollView.getParent();
                    int indexOfChild = viewGroup2.indexOfChild(this.mScrollView);
                    viewGroup2.removeViewAt(indexOfChild);
                    viewGroup2.addView(this.mListView, indexOfChild, new LayoutParams(-1, -1));
                } else {
                    viewGroup.setVisibility(8);
                }
            }
        }
    }

    private void setupButtons(ViewGroup viewGroup) {
        boolean z;
        this.mButtonPositive = (Button) viewGroup.findViewById(16908313);
        this.mButtonPositive.setOnClickListener(this.mButtonHandler);
        boolean z2 = true;
        if (TextUtils.isEmpty(this.mButtonPositiveText)) {
            this.mButtonPositive.setVisibility(8);
            z = false;
        } else {
            this.mButtonPositive.setText(this.mButtonPositiveText);
            this.mButtonPositive.setVisibility(0);
            z = true;
        }
        this.mButtonNegative = (Button) viewGroup.findViewById(16908314);
        this.mButtonNegative.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNegativeText)) {
            this.mButtonNegative.setVisibility(8);
        } else {
            this.mButtonNegative.setText(this.mButtonNegativeText);
            this.mButtonNegative.setVisibility(0);
            z |= true;
        }
        this.mButtonNeutral = (Button) viewGroup.findViewById(16908315);
        this.mButtonNeutral.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNeutralText)) {
            this.mButtonNeutral.setVisibility(8);
        } else {
            this.mButtonNeutral.setText(this.mButtonNeutralText);
            this.mButtonNeutral.setVisibility(0);
            z |= true;
        }
        if (shouldCenterSingleButton(this.mContext)) {
            if (z) {
                centerButton(this.mButtonPositive);
            } else if (z) {
                centerButton(this.mButtonNegative);
            } else if (z) {
                centerButton(this.mButtonNeutral);
            }
        }
        if (!z) {
            z2 = false;
        }
        if (!z2) {
            viewGroup.setVisibility(8);
        }
    }

    private void centerButton(Button button) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button.getLayoutParams();
        layoutParams.gravity = 1;
        layoutParams.weight = 0.5f;
        button.setLayoutParams(layoutParams);
    }

    private void setBackground(TypedArray typedArray, View view, View view2, View view3, View view4, boolean z, boolean z2, boolean z3) {
        int i;
        View[] viewArr = new View[4];
        boolean[] zArr = new boolean[4];
        if (z) {
            viewArr[0] = view;
            zArr[0] = false;
            i = 1;
        } else {
            i = 0;
        }
        View view5 = null;
        if (view2.getVisibility() == 8) {
            view2 = null;
        }
        viewArr[i] = view2;
        zArr[i] = this.mListView != null;
        int i2 = i + 1;
        if (z2) {
            viewArr[i2] = view3;
            zArr[i2] = this.mForceInverseBackground;
            i2++;
        }
        if (z3) {
            viewArr[i2] = view4;
            zArr[i2] = true;
        }
        boolean z4 = false;
        for (int i3 = 0; i3 < viewArr.length; i3++) {
            View view6 = viewArr[i3];
            if (view6 != null) {
                if (view5 != null) {
                    if (!z4) {
                        view5.setBackgroundResource(0);
                    } else {
                        view5.setBackgroundResource(0);
                    }
                    z4 = true;
                }
                boolean z5 = zArr[i3];
                view5 = view6;
            }
        }
        if (view5 != null) {
            if (z4) {
                view5.setBackgroundResource(0);
            } else {
                view5.setBackgroundResource(0);
            }
        }
        ListView listView = this.mListView;
        if (listView != null) {
            ListAdapter listAdapter = this.mAdapter;
            if (listAdapter != null) {
                listView.setAdapter(listAdapter);
                int i4 = this.mCheckedItem;
                if (i4 > -1) {
                    listView.setItemChecked(i4, true);
                    listView.setSelection(i4);
                }
            }
        }
    }
}
