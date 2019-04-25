package com.koushikdutta.async.callback;

import com.koushikdutta.async.http.AsyncHttpResponse;

public interface RequestCallback<T> {
    void onCompleted(Exception exception, AsyncHttpResponse asyncHttpResponse, T t);

    void onProgress(AsyncHttpResponse asyncHttpResponse, int i, int i2);
}
