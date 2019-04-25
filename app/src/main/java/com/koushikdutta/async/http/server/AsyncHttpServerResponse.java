package com.koushikdutta.async.http.server;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.libcore.ResponseHeaders;
import java.io.File;
import org.json.JSONObject;

public interface AsyncHttpServerResponse extends DataSink, CompletedCallback {
    void end();

    ResponseHeaders getHeaders();

    AsyncSocket getSocket();

    void onCompleted(Exception exception);

    void redirect(String str);

    void responseCode(int i);

    void send(String str);

    void send(String str, String str2);

    void send(JSONObject jSONObject);

    void sendFile(File file);

    void setContentType(String str);

    void writeHead();
}
