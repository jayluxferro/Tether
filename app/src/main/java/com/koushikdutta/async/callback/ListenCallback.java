package com.koushikdutta.async.callback;

import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;

public interface ListenCallback extends CompletedCallback {
    void onAccepted(AsyncSocket asyncSocket);

    void onListening(AsyncServerSocket asyncServerSocket);
}
