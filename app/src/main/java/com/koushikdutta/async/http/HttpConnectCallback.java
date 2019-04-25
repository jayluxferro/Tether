package com.koushikdutta.async.http;

public interface HttpConnectCallback {
    void onConnectCompleted(Exception exception, AsyncHttpResponse asyncHttpResponse);
}
