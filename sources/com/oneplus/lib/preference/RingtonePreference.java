package com.oneplus.lib.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.preference.PreferenceManager.OnActivityResultListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RingtonePreference extends Preference implements OnActivityResultListener {
    private static Method getDefaultRingtoneUriBySubId;
    private int mRingtoneType;
    private boolean mShowDefault;
    private boolean mShowSilent;
    private int mSubscriptionID;

    public RingtonePreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mSubscriptionID = 0;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.RingtonePreference, i, i2);
        this.mRingtoneType = obtainStyledAttributes.getInt(R$styleable.RingtonePreference_android_ringtoneType, 1);
        this.mShowDefault = obtainStyledAttributes.getBoolean(R$styleable.RingtonePreference_android_showDefault, true);
        this.mShowSilent = obtainStyledAttributes.getBoolean(R$styleable.RingtonePreference_android_showSilent, true);
        obtainStyledAttributes.recycle();
    }

    public RingtonePreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RingtonePreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_ringtonePreferenceStyle);
    }

    public int getRingtoneType() {
        return this.mRingtoneType;
    }

    public int getSubId() {
        return this.mSubscriptionID;
    }

    /* access modifiers changed from: protected */
    public void onClick() {
        Intent intent;
        if (VERSION.SDK_INT >= 26) {
            intent = new Intent("oneplus.intent.action.RINGTONE_PICKER");
        } else {
            intent = new Intent("android.intent.action.oneplus.RINGTONE_PICKER");
        }
        onPrepareRingtonePickerIntent(intent);
        getPreferenceManager().getFragment();
        throw null;
    }

    /* access modifiers changed from: protected */
    public void onPrepareRingtonePickerIntent(Intent intent) {
        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", onRestoreRingtone());
        intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", this.mShowDefault);
        if (this.mShowDefault) {
            String str = "android.intent.extra.ringtone.DEFAULT_URI";
            if (getRingtoneType() == 1) {
                try {
                    if (getDefaultRingtoneUriBySubId == null) {
                        getDefaultRingtoneUriBySubId = RingtoneManager.class.getDeclaredMethod("getDefaultRingtoneUriBySubId", new Class[]{Integer.TYPE});
                    }
                    if (getDefaultRingtoneUriBySubId != null) {
                        intent.putExtra(str, (Uri) getDefaultRingtoneUriBySubId.invoke(null, new Object[]{Integer.valueOf(getSubId())}));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e2) {
                    e2.printStackTrace();
                } catch (InvocationTargetException e3) {
                    e3.printStackTrace();
                } catch (NoSuchMethodException e4) {
                    e4.printStackTrace();
                }
            } else {
                intent.putExtra(str, RingtoneManager.getDefaultUri(getRingtoneType()));
            }
        }
        intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", this.mShowSilent);
        intent.putExtra("android.intent.extra.ringtone.TYPE", this.mRingtoneType);
        intent.putExtra("android.intent.extra.ringtone.TITLE", getTitle());
        intent.putExtra("android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS", 64);
    }

    /* access modifiers changed from: protected */
    public Uri onRestoreRingtone() {
        String persistedString = getPersistedString(null);
        if (!TextUtils.isEmpty(persistedString)) {
            return Uri.parse(persistedString);
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray typedArray, int i) {
        return typedArray.getString(i);
    }
}
