package com.android.settingslib.suggestions;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.android.settingslib.suggestions.SuggestionController.ServiceConnectionListener;
import java.util.List;

@Deprecated
public class SuggestionControllerMixin implements ServiceConnectionListener, LifecycleObserver, LoaderCallbacks<List<Suggestion>> {
    private final Context mContext;
    private final SuggestionControllerHost mHost;
    private final SuggestionController mSuggestionController;
    private boolean mSuggestionLoaded;

    public interface SuggestionControllerHost {
        void onSuggestionReady(List<Suggestion> list);
    }

    @OnLifecycleEvent(Event.ON_START)
    public void onStart() {
        this.mSuggestionController.start();
        throw null;
    }

    @OnLifecycleEvent(Event.ON_STOP)
    public void onStop() {
        this.mSuggestionController.stop();
        throw null;
    }

    public Loader<List<Suggestion>> onCreateLoader(int i, Bundle bundle) {
        if (i == 42) {
            this.mSuggestionLoaded = false;
            return new SuggestionLoader(this.mContext, this.mSuggestionController);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("This loader id is not supported ");
        sb.append(i);
        throw new IllegalArgumentException(sb.toString());
    }

    public void onLoadFinished(Loader<List<Suggestion>> loader, List<Suggestion> list) {
        this.mSuggestionLoaded = true;
        this.mHost.onSuggestionReady(list);
    }

    public void onLoaderReset(Loader<List<Suggestion>> loader) {
        this.mSuggestionLoaded = false;
    }
}
