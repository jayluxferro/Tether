package com.koushikdutta.async.http.server;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import java.util.Map;
import java.util.regex.Matcher;

public interface AsyncHttpServerRequest extends DataEmitter {
    AsyncHttpRequestBody getBody();

    RequestHeaders getHeaders();

    Matcher getMatcher();

    String getPath();

    Map<String, String> getQuery();

    AsyncSocket getSocket();
}
