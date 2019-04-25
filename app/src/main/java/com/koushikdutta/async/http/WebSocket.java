package com.koushikdutta.async.http;

import com.koushikdutta.async.AsyncSocket;

public interface WebSocket extends AsyncSocket {

    public interface StringCallback {
        void onStringAvailable(String str);
    }

    AsyncSocket getSocket();

    StringCallback getStringCallback();

    boolean isBuffering();

    void send(String str);

    void send(byte[] bArr);

    void setStringCallback(StringCallback stringCallback);
}
