package com.koushikdutta.async.http;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.libcore.ResponseHeaders;

public interface AsyncHttpResponse extends DataEmitter, DataSink {
    AsyncSocket detachSocket();

    void end();

    CompletedCallback getEndCallback();

    ResponseHeaders getHeaders();

    boolean isReusedSocket();

    void setEndCallback(CompletedCallback completedCallback);
}
