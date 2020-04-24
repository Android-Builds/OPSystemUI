package com.android.settingslib.suggestions;

import android.service.settings.suggestions.Suggestion;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import com.android.settingslib.suggestions.SuggestionController.ServiceConnectionListener;
import java.util.List;

public class SuggestionControllerMixinCompat implements ServiceConnectionListener, LifecycleObserver, LoaderCallbacks<List<Suggestion>> {
    private final SuggestionController mSuggestionController;

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
}
