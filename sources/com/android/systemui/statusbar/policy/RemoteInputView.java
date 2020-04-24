package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Editable;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationEntry.EditedSuggestionInfo;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.phone.LightBarController;
import java.util.function.Consumer;

public class RemoteInputView extends LinearLayout implements OnClickListener, TextWatcher {
    public static final Object VIEW_TAG = new Object();
    private RemoteInputController mController;
    /* access modifiers changed from: private */
    public RemoteEditText mEditText;
    /* access modifiers changed from: private */
    public NotificationEntry mEntry;
    private Consumer<Boolean> mOnVisibilityChangedListener;
    private PendingIntent mPendingIntent;
    private ProgressBar mProgressBar;
    private RemoteInput mRemoteInput;
    private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler = ((RemoteInputQuickSettingsDisabler) Dependency.get(RemoteInputQuickSettingsDisabler.class));
    private RemoteInput[] mRemoteInputs;
    /* access modifiers changed from: private */
    public boolean mRemoved;
    private boolean mResetting;
    private int mRevealCx;
    private int mRevealCy;
    private int mRevealR;
    private ImageButton mSendButton;
    public final Object mToken = new Object();
    /* access modifiers changed from: private */
    public NotificationViewWrapper mWrapper;

    public static class RemoteEditText extends EditText {
        private final Drawable mBackground = getBackground();
        private LightBarController mLightBarController = ((LightBarController) Dependency.get(LightBarController.class));
        /* access modifiers changed from: private */
        public RemoteInputView mRemoteInputView;
        boolean mShowImeOnInputConnection;

        public RemoteEditText(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        /* access modifiers changed from: private */
        public void defocusIfNeeded(boolean z) {
            RemoteInputView remoteInputView = this.mRemoteInputView;
            if ((remoteInputView == null || !remoteInputView.mEntry.getRow().isChangingPosition()) && !isTemporarilyDetached()) {
                if (isFocusable() && isEnabled()) {
                    setInnerFocusable(false);
                    RemoteInputView remoteInputView2 = this.mRemoteInputView;
                    if (remoteInputView2 != null) {
                        remoteInputView2.onDefocus(z);
                    }
                    this.mShowImeOnInputConnection = false;
                }
                return;
            }
            if (isTemporarilyDetached()) {
                RemoteInputView remoteInputView3 = this.mRemoteInputView;
                if (remoteInputView3 != null) {
                    remoteInputView3.mEntry.remoteInputText = getText();
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onVisibilityChanged(View view, int i) {
            super.onVisibilityChanged(view, i);
            if (!isShown()) {
                defocusIfNeeded(false);
            }
        }

        /* access modifiers changed from: protected */
        public void onFocusChanged(boolean z, int i, Rect rect) {
            super.onFocusChanged(z, i, rect);
            if (!z) {
                defocusIfNeeded(true);
            }
            if (!this.mRemoteInputView.mRemoved) {
                this.mLightBarController.setDirectReplying(z);
            }
        }

        public void getFocusedRect(Rect rect) {
            super.getFocusedRect(rect);
            rect.top = this.mScrollY;
            rect.bottom = this.mScrollY + (this.mBottom - this.mTop);
        }

        public boolean requestRectangleOnScreen(Rect rect) {
            return this.mRemoteInputView.requestScrollTo();
        }

        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            if (i == 4) {
                return true;
            }
            return super.onKeyDown(i, keyEvent);
        }

        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            if (i != 4) {
                return super.onKeyUp(i, keyEvent);
            }
            defocusIfNeeded(true);
            return true;
        }

        public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == 4 && keyEvent.getAction() == 1) {
                defocusIfNeeded(true);
            }
            return super.onKeyPreIme(i, keyEvent);
        }

        public boolean onCheckIsTextEditor() {
            RemoteInputView remoteInputView = this.mRemoteInputView;
            if ((remoteInputView != null && remoteInputView.mRemoved) || !super.onCheckIsTextEditor()) {
                return false;
            }
            return true;
        }

        public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
            InputConnection onCreateInputConnection = super.onCreateInputConnection(editorInfo);
            if (this.mShowImeOnInputConnection && onCreateInputConnection != null) {
                final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(InputMethodManager.class);
                if (inputMethodManager != null) {
                    post(new Runnable() {
                        public void run() {
                            inputMethodManager.viewClicked(RemoteEditText.this);
                            inputMethodManager.showSoftInput(RemoteEditText.this, 0);
                        }
                    });
                }
            }
            return onCreateInputConnection;
        }

        public void onCommitCompletion(CompletionInfo completionInfo) {
            clearComposingText();
            setText(completionInfo.getText());
            setSelection(getText().length());
        }

        /* access modifiers changed from: 0000 */
        public void setInnerFocusable(boolean z) {
            setFocusableInTouchMode(z);
            setFocusable(z);
            setCursorVisible(z);
            if (z) {
                requestFocus();
                setBackground(this.mBackground);
                return;
            }
            setBackground(null);
        }
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public RemoteInputView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mProgressBar = (ProgressBar) findViewById(R$id.remote_input_progress);
        this.mSendButton = (ImageButton) findViewById(R$id.remote_input_send);
        this.mSendButton.setOnClickListener(this);
        this.mEditText = (RemoteEditText) getChildAt(0);
        this.mEditText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean z = keyEvent == null && (i == 6 || i == 5 || i == 4);
                boolean z2 = keyEvent != null && KeyEvent.isConfirmKey(keyEvent.getKeyCode()) && keyEvent.getAction() == 0;
                if (!z && !z2) {
                    return false;
                }
                if (RemoteInputView.this.mEditText.length() > 0) {
                    RemoteInputView.this.sendRemoteInput();
                }
                return true;
            }
        });
        this.mEditText.addTextChangedListener(this);
        this.mEditText.setInnerFocusable(false);
        this.mEditText.mRemoteInputView = this;
    }

    /* access modifiers changed from: private */
    public void sendRemoteInput() {
        Bundle bundle = new Bundle();
        bundle.putString(this.mRemoteInput.getResultKey(), this.mEditText.getText().toString());
        Intent addFlags = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(this.mRemoteInputs, addFlags, bundle);
        if (this.mEntry.editedSuggestionInfo == null) {
            RemoteInput.setResultsSource(addFlags, 0);
        } else {
            RemoteInput.setResultsSource(addFlags, 1);
        }
        this.mEditText.setEnabled(false);
        this.mSendButton.setVisibility(4);
        this.mProgressBar.setVisibility(0);
        this.mEntry.remoteInputText = this.mEditText.getText();
        this.mEntry.lastRemoteInputSent = SystemClock.elapsedRealtime();
        this.mController.addSpinning(this.mEntry.key, this.mToken);
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEditText.mShowImeOnInputConnection = false;
        this.mController.remoteInputSent(this.mEntry);
        this.mEntry.setHasSentReply();
        ((ShortcutManager) getContext().getSystemService(ShortcutManager.class)).onApplicationActive(this.mEntry.notification.getPackageName(), this.mEntry.notification.getUser().getIdentifier());
        MetricsLogger.action(this.mContext, 398, this.mEntry.notification.getPackageName());
        try {
            this.mPendingIntent.send(this.mContext, 0, addFlags);
        } catch (CanceledException e) {
            Log.i("RemoteInput", "Unable to send remote input result", e);
            MetricsLogger.action(this.mContext, 399, this.mEntry.notification.getPackageName());
        }
    }

    public CharSequence getText() {
        return this.mEditText.getText();
    }

    public static RemoteInputView inflate(Context context, ViewGroup viewGroup, NotificationEntry notificationEntry, RemoteInputController remoteInputController) {
        RemoteInputView remoteInputView = (RemoteInputView) LayoutInflater.from(context).inflate(R$layout.remote_input, viewGroup, false);
        remoteInputView.mController = remoteInputController;
        remoteInputView.mEntry = notificationEntry;
        remoteInputView.mEditText.setTextOperationUser(computeTextOperationUser(notificationEntry.notification.getUser()));
        remoteInputView.setTag(VIEW_TAG);
        return remoteInputView;
    }

    public void onClick(View view) {
        if (view == this.mSendButton) {
            sendRemoteInput();
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        return true;
    }

    /* access modifiers changed from: private */
    public void onDefocus(boolean z) {
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEntry.remoteInputText = this.mEditText.getText();
        if (!this.mRemoved) {
            if (z) {
                int i = this.mRevealR;
                if (i > 0) {
                    Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, (float) i, 0.0f);
                    createCircularReveal.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                    createCircularReveal.setDuration(150);
                    createCircularReveal.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animator) {
                            RemoteInputView.this.setVisibility(4);
                            if (RemoteInputView.this.mWrapper != null) {
                                RemoteInputView.this.mWrapper.setRemoteInputVisible(false);
                            }
                        }
                    });
                    createCircularReveal.start();
                }
            }
            setVisibility(4);
            NotificationViewWrapper notificationViewWrapper = this.mWrapper;
            if (notificationViewWrapper != null) {
                notificationViewWrapper.setRemoteInputVisible(false);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("remote input de-active from ");
        sb.append(this.mEntry.notification.getPackageName());
        Log.d("RemoteInput", sb.toString());
        this.mRemoteInputQuickSettingsDisabler.setRemoteInputActive(false);
        MetricsLogger.action(this.mContext, 400, this.mEntry.notification.getPackageName());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mEntry.getRow().isChangingPosition() && getVisibility() == 0 && this.mEditText.isFocusable()) {
            this.mEditText.requestFocus();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mEntry.getRow().isChangingPosition() && !isTemporarilyDetached()) {
            this.mController.removeRemoteInput(this.mEntry, this.mToken);
            this.mController.removeSpinning(this.mEntry.key, this.mToken);
        }
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.mPendingIntent = pendingIntent;
    }

    public void setRemoteInput(RemoteInput[] remoteInputArr, RemoteInput remoteInput, EditedSuggestionInfo editedSuggestionInfo) {
        this.mRemoteInputs = remoteInputArr;
        this.mRemoteInput = remoteInput;
        this.mEditText.setHint(this.mRemoteInput.getLabel());
        NotificationEntry notificationEntry = this.mEntry;
        notificationEntry.editedSuggestionInfo = editedSuggestionInfo;
        if (editedSuggestionInfo != null) {
            notificationEntry.remoteInputText = editedSuggestionInfo.originalText;
        }
    }

    public void focusAnimated() {
        if (getVisibility() != 0) {
            Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, 0.0f, (float) this.mRevealR);
            createCircularReveal.setDuration(360);
            createCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            createCircularReveal.start();
        }
        focus();
    }

    private static UserHandle computeTextOperationUser(UserHandle userHandle) {
        return UserHandle.ALL.equals(userHandle) ? UserHandle.of(ActivityManager.getCurrentUser()) : userHandle;
    }

    public void focus() {
        MetricsLogger.action(this.mContext, 397, this.mEntry.notification.getPackageName());
        setVisibility(0);
        NotificationViewWrapper notificationViewWrapper = this.mWrapper;
        if (notificationViewWrapper != null) {
            notificationViewWrapper.setRemoteInputVisible(true);
        }
        this.mEditText.setInnerFocusable(true);
        RemoteEditText remoteEditText = this.mEditText;
        remoteEditText.mShowImeOnInputConnection = true;
        remoteEditText.setText(this.mEntry.remoteInputText);
        RemoteEditText remoteEditText2 = this.mEditText;
        remoteEditText2.setSelection(remoteEditText2.getText().length());
        this.mEditText.requestFocus();
        this.mController.addRemoteInput(this.mEntry, this.mToken);
        StringBuilder sb = new StringBuilder();
        sb.append("remote input active from ");
        sb.append(this.mEntry.notification.getPackageName());
        Log.d("RemoteInput", sb.toString());
        this.mRemoteInputQuickSettingsDisabler.setRemoteInputActive(true);
        updateSendButton();
    }

    public void onNotificationUpdateOrReset() {
        if (this.mProgressBar.getVisibility() == 0) {
            reset();
        }
        if (isActive()) {
            NotificationViewWrapper notificationViewWrapper = this.mWrapper;
            if (notificationViewWrapper != null) {
                notificationViewWrapper.setRemoteInputVisible(true);
            }
        }
    }

    private void reset() {
        this.mResetting = true;
        this.mEntry.remoteInputTextWhenReset = SpannedString.valueOf(this.mEditText.getText());
        this.mEditText.getText().clear();
        this.mEditText.setEnabled(true);
        this.mSendButton.setVisibility(0);
        this.mProgressBar.setVisibility(4);
        this.mController.removeSpinning(this.mEntry.key, this.mToken);
        updateSendButton();
        onDefocus(false);
        this.mResetting = false;
    }

    public boolean onRequestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        if (!this.mResetting || view != this.mEditText) {
            return super.onRequestSendAccessibilityEvent(view, accessibilityEvent);
        }
        return false;
    }

    private void updateSendButton() {
        this.mSendButton.setEnabled(this.mEditText.getText().length() != 0);
    }

    public void afterTextChanged(Editable editable) {
        updateSendButton();
    }

    public void close() {
        this.mEditText.defocusIfNeeded(false);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mController.requestDisallowLongPressAndDismiss();
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    public boolean requestScrollTo() {
        this.mController.lockScrollTo(this.mEntry);
        return true;
    }

    public boolean isActive() {
        RemoteEditText remoteEditText = this.mEditText;
        boolean z = false;
        if (remoteEditText == null) {
            return false;
        }
        if (remoteEditText.isFocused() && this.mEditText.isEnabled()) {
            z = true;
        }
        return z;
    }

    public void stealFocusFrom(RemoteInputView remoteInputView) {
        remoteInputView.close();
        setPendingIntent(remoteInputView.mPendingIntent);
        setRemoteInput(remoteInputView.mRemoteInputs, remoteInputView.mRemoteInput, this.mEntry.editedSuggestionInfo);
        setRevealParameters(remoteInputView.mRevealCx, remoteInputView.mRevealCy, remoteInputView.mRevealR);
        focus();
    }

    public boolean updatePendingIntentFromActions(Action[] actionArr) {
        PendingIntent pendingIntent = this.mPendingIntent;
        if (!(pendingIntent == null || actionArr == null)) {
            Intent intent = pendingIntent.getIntent();
            if (intent == null) {
                return false;
            }
            for (Action action : actionArr) {
                RemoteInput[] remoteInputs = action.getRemoteInputs();
                PendingIntent pendingIntent2 = action.actionIntent;
                if (!(pendingIntent2 == null || remoteInputs == null || !intent.filterEquals(pendingIntent2.getIntent()))) {
                    RemoteInput remoteInput = null;
                    for (RemoteInput remoteInput2 : remoteInputs) {
                        if (remoteInput2.getAllowFreeFormInput()) {
                            remoteInput = remoteInput2;
                        }
                    }
                    if (remoteInput != null) {
                        setPendingIntent(action.actionIntent);
                        setRemoteInput(remoteInputs, remoteInput, null);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PendingIntent getPendingIntent() {
        return this.mPendingIntent;
    }

    public void setRemoved() {
        this.mRemoved = true;
    }

    public void setRevealParameters(int i, int i2, int i3) {
        this.mRevealCx = i;
        this.mRevealCy = i2;
        this.mRevealR = i3;
    }

    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        detachViewFromParent(this.mEditText);
    }

    public void dispatchFinishTemporaryDetach() {
        if (isAttachedToWindow()) {
            RemoteEditText remoteEditText = this.mEditText;
            attachViewToParent(remoteEditText, 0, remoteEditText.getLayoutParams());
        } else {
            removeDetachedView(this.mEditText, false);
        }
        super.dispatchFinishTemporaryDetach();
    }

    public void setWrapper(NotificationViewWrapper notificationViewWrapper) {
        this.mWrapper = notificationViewWrapper;
    }

    public void setOnVisibilityChangedListener(Consumer<Boolean> consumer) {
        this.mOnVisibilityChangedListener = consumer;
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this) {
            Consumer<Boolean> consumer = this.mOnVisibilityChangedListener;
            if (consumer != null) {
                consumer.accept(Boolean.valueOf(i == 0));
            }
        }
    }

    public boolean isSending() {
        return getVisibility() == 0 && this.mController.isSpinning(this.mEntry.key, this.mToken);
    }
}
