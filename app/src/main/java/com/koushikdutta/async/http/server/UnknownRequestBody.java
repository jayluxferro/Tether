package com.koushikdutta.async.http.server;

import com.koushikdutta.async.NullDataCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import junit.framework.Assert;

public class UnknownRequestBody extends AsyncHttpRequestBodyBase {
    private String mContentType;

    public UnknownRequestBody(String contentType) {
        super(contentType);
        setDataCallback(new NullDataCallback());
    }

    public void write(AsyncHttpRequest request, AsyncHttpResponse sink) {
        Assert.fail();
    }

    public String getContentType() {
        return this.mContentType;
    }
}
