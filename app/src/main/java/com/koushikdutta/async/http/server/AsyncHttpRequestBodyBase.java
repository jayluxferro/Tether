package com.koushikdutta.async.http.server;

import com.koushikdutta.async.FilteredDataCallback;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.AsyncHttpResponse;
import junit.framework.Assert;

public class AsyncHttpRequestBodyBase extends FilteredDataCallback implements AsyncHttpRequestBody {
    private String mContentType;

    public AsyncHttpRequestBodyBase(String contentType) {
        this.mContentType = contentType;
    }

    public void write(AsyncHttpRequest request, AsyncHttpResponse sink) {
        Assert.fail();
    }

    public String getContentType() {
        return this.mContentType;
    }

    public void onCompleted(Exception ex) {
        CompletedCallback callback = getEndCallback();
        if (callback != null) {
            callback.onCompleted(ex);
        }
    }

    public boolean readFullyOnRequest() {
        return false;
    }

    public int length() {
        return -1;
    }
}
