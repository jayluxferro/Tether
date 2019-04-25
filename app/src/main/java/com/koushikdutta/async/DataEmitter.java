package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;

public interface DataEmitter {
    DataCallback getDataCallback();

    CompletedCallback getEndCallback();

    boolean isChunked();

    boolean isPaused();

    void pause();

    void resume();

    void setDataCallback(DataCallback dataCallback);

    void setEndCallback(CompletedCallback completedCallback);
}
