package com.koushikdutta.async.http.server;

public interface HttpServerRequestCallback {
    void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse);
}
