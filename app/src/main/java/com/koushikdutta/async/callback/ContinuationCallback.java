package com.koushikdutta.async.callback;

import com.koushikdutta.async.Continuation;

public interface ContinuationCallback {
    void onContinue(Continuation continuation, CompletedCallback completedCallback) throws Exception;
}
