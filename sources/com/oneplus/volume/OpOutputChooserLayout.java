package com.oneplus.volume;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.p007qs.AutoSizingList;
import com.oneplus.volume.OpOutputChooserLayout.Item;

public class OpOutputChooserLayout extends LinearLayout {
    private static final boolean DEBUG = Log.isLoggable("OutputChooserLayout", 3);
    private final Adapter mAdapter = new Adapter();
    /* access modifiers changed from: private */
    public Callback mCallback;
    /* access modifiers changed from: private */
    public final Context mContext;
    private View mEmpty;
    private ImageView mEmptyIcon;
    private TextView mEmptyText;
    private final C2132H mHandler = new C2132H();
    private AutoSizingList mItemList;
    /* access modifiers changed from: private */
    public Item[] mItems;
    /* access modifiers changed from: private */
    public boolean mItemsVisible = true;
    private ImageButton mSettingsBackIcon;
    private String mTag;
    private TextView mTitle;

    private class Adapter extends BaseAdapter {
        public long getItemId(int i) {
            return 0;
        }

        private Adapter() {
        }

        public int getCount() {
            if (OpOutputChooserLayout.this.mItems != null) {
                return OpOutputChooserLayout.this.mItems.length;
            }
            return 0;
        }

        public Object getItem(int i) {
            return OpOutputChooserLayout.this.mItems[i];
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            Item item = OpOutputChooserLayout.this.mItems[i];
            if (view == null) {
                view = LayoutInflater.from(OpOutputChooserLayout.this.mContext).inflate(R$layout.output_chooser_item, viewGroup, false);
            }
            view.setVisibility(OpOutputChooserLayout.this.mItemsVisible ? 0 : 4);
            ImageView imageView = (ImageView) view.findViewById(16908294);
            Drawable drawable = item.icon;
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
            } else {
                imageView.setImageResource(item.iconResId);
            }
            TextView textView = (TextView) view.findViewById(16908310);
            textView.setText(item.line1);
            TextView textView2 = (TextView) view.findViewById(16908304);
            int i2 = 1;
            boolean z = !TextUtils.isEmpty(item.line2);
            if (!z) {
                i2 = 2;
            }
            textView.setMaxLines(i2);
            textView2.setVisibility(z ? 0 : 8);
            textView2.setText(z ? item.line2 : null);
            view.setOnClickListener(new OnClickListener(item) {
                private final /* synthetic */ Item f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    Adapter.this.lambda$getView$0$OpOutputChooserLayout$Adapter(this.f$1, view);
                }
            });
            ImageView imageView2 = (ImageView) view.findViewById(16908296);
            if (OpOutputChooserLayout.this.mCallback != null) {
                int primaryTextColor = OpOutputChooserLayout.this.mCallback.getPrimaryTextColor();
                int secondaryTextColor = OpOutputChooserLayout.this.mCallback.getSecondaryTextColor();
                int iconColor = OpOutputChooserLayout.this.mCallback.getIconColor();
                imageView.setColorFilter(iconColor);
                imageView2.setColorFilter(iconColor);
                textView.setTextColor(primaryTextColor);
                textView2.setTextColor(secondaryTextColor);
            }
            if (item.selected) {
                imageView2.setImageResource(R$drawable.ic_output_chooser_check);
                imageView2.setVisibility(0);
                imageView2.setClickable(false);
            } else if (item.icon2 != -1) {
                imageView2.setVisibility(0);
                imageView2.setImageResource(item.icon2);
                imageView2.setClickable(false);
            } else {
                imageView2.setVisibility(8);
            }
            return view;
        }

        public /* synthetic */ void lambda$getView$0$OpOutputChooserLayout$Adapter(Item item, View view) {
            if (OpOutputChooserLayout.this.mCallback != null) {
                OpOutputChooserLayout.this.mCallback.onDetailItemClick(item);
            }
        }
    }

    public interface Callback {
        void backToVolumeDialog();

        int getIconColor();

        int getPrimaryTextColor();

        int getSecondaryTextColor();

        void onDetailItemClick(Item item);
    }

    /* renamed from: com.oneplus.volume.OpOutputChooserLayout$H */
    private class C2132H extends Handler {
        public C2132H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            int i = message.what;
            boolean z = true;
            if (i == 1) {
                OpOutputChooserLayout.this.handleSetItems((Item[]) message.obj);
            } else if (i == 2) {
                OpOutputChooserLayout.this.handleSetCallback((Callback) message.obj);
            } else if (i == 3) {
                OpOutputChooserLayout opOutputChooserLayout = OpOutputChooserLayout.this;
                if (message.arg1 == 0) {
                    z = false;
                }
                opOutputChooserLayout.handleSetItemsVisible(z);
            }
        }
    }

    public static class Item {
        public static int DEVICE_TYPE_BT = 3;
        public static int DEVICE_TYPE_HEADSET = 2;
        public static int DEVICE_TYPE_MEDIA_ROUTER = 4;
        public static int DEVICE_TYPE_PHONE = 1;
        public boolean canDisconnect;
        public int deviceType = 0;
        public Drawable icon;
        public int icon2 = -1;
        public int iconResId;
        public CharSequence line1;
        public CharSequence line2;
        public boolean selected;
        public Object tag;
    }

    public OpOutputChooserLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        this.mTag = "OutputChooserLayout";
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mItemList = (AutoSizingList) findViewById(16908298);
        this.mItemList.setVisibility(8);
        this.mItemList.setAdapter(this.mAdapter);
        this.mEmpty = findViewById(16908292);
        this.mEmpty.setVisibility(8);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(R$id.empty_text);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(R$id.empty_icon);
        this.mTitle = (TextView) findViewById(R$id.title);
        this.mSettingsBackIcon = (ImageButton) findViewById(R$id.settings_back);
        ImageButton imageButton = this.mSettingsBackIcon;
        if (imageButton != null) {
            imageButton.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    OpOutputChooserLayout.this.lambda$onFinishInflate$0$OpOutputChooserLayout(view);
                }
            });
        }
    }

    public /* synthetic */ void lambda$onFinishInflate$0$OpOutputChooserLayout(View view) {
        if (this.mCallback != null) {
            Log.i("OutputChooserLayout", "mSettingsBackIcon click");
            this.mCallback.backToVolumeDialog();
        }
    }

    public void setBackKeyColor(int i) {
        ImageButton imageButton = this.mSettingsBackIcon;
        if (imageButton != null) {
            imageButton.setColorFilter(i);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mEmptyText, R$dimen.qs_detail_empty_text_size);
        int childCount = this.mItemList.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mItemList.getChildAt(i);
            FontSizeUtils.updateFontSize(childAt, R$id.empty_text, R$dimen.qs_detail_item_primary_text_size);
            FontSizeUtils.updateFontSize(childAt, 16908304, R$dimen.qs_detail_item_secondary_text_size);
            FontSizeUtils.updateFontSize(childAt, 16908310, R$dimen.qs_detail_header_text_size);
        }
    }

    public void setTitle(int i) {
        this.mTitle.setText(i);
    }

    public void setTitleColor(int i) {
        this.mTitle.setTextColor(i);
    }

    public void setEmptyState(String str) {
        this.mEmptyText.setText(str);
    }

    public void setEmptyTextColor(int i) {
        this.mEmptyText.setTextColor(i);
    }

    public void setEmptyIconColor(int i) {
        this.mEmptyIcon.setImageTintList(ColorStateList.valueOf(i));
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        this.mCallback = null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(2, callback).sendToTarget();
    }

    public void setItems(Item[] itemArr) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, itemArr).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleSetCallback(Callback callback) {
        this.mCallback = callback;
    }

    /* access modifiers changed from: private */
    public void handleSetItems(Item[] itemArr) {
        int i = 0;
        int length = itemArr != null ? itemArr.length : 0;
        this.mEmpty.setVisibility(length == 0 ? 0 : 8);
        AutoSizingList autoSizingList = this.mItemList;
        if (length == 0) {
            i = 8;
        }
        autoSizingList.setVisibility(i);
        this.mItems = itemArr;
        this.mAdapter.notifyDataSetChanged();
    }

    /* access modifiers changed from: private */
    public void handleSetItemsVisible(boolean z) {
        if (this.mItemsVisible != z) {
            this.mItemsVisible = z;
            for (int i = 0; i < this.mItemList.getChildCount(); i++) {
                this.mItemList.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
            }
        }
    }
}
