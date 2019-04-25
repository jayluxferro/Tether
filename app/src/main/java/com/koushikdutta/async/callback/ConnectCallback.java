package com.koushikdutta.async.callback;

import com.koushikdutta.async.AsyncSocket;

public interface ConnectCallback {
    void onConnectCompleted(Exception exception, AsyncSocket asyncSocket);
}
