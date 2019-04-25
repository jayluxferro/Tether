package com.koushikdutta.async.http;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;

public interface AsyncHttpRequestBody extends DataCallback, CompletedCallback {
    String getContentType();

    int length();

    boolean readFullyOnRequest();

    void write(AsyncHttpRequest asyncHttpRequest, AsyncHttpResponse asyncHttpResponse);
}
