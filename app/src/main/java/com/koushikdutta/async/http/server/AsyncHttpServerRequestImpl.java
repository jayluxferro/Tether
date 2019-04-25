package com.koushikdutta.async.http.server;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.LineEmitter;
import com.koushikdutta.async.LineEmitter.StringCallback;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.Util;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import java.util.regex.Matcher;

public abstract class AsyncHttpServerRequestImpl implements AsyncHttpServerRequest, CompletedCallback {
    AsyncHttpRequestBody mBody;
    private CompletedCallback mCompleted;
    StringCallback mHeaderCallback = new C02912();
    private RequestHeaders mHeaders = new RequestHeaders(null, this.mRawHeaders);
    Matcher mMatcher;
    private RawHeaders mRawHeaders = new RawHeaders();
    private CompletedCallback mReporter = new C02901();
    AsyncSocket mSocket;

    /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServerRequestImpl$1 */
    class C02901 implements CompletedCallback {
        C02901() {
        }

        public void onCompleted(Exception error) {
            AsyncHttpServerRequestImpl.this.onCompleted(error);
        }
    }

    /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServerRequestImpl$2 */
    class C02912 implements StringCallback {
        C02912() {
        }

        public void onStringAvailable(String s) {
            try {
                if (AsyncHttpServerRequestImpl.this.mRawHeaders.getStatusLine() == null) {
                    AsyncHttpServerRequestImpl.this.mRawHeaders.setStatusLine(s);
                    if (!AsyncHttpServerRequestImpl.this.mRawHeaders.getStatusLine().contains("HTTP/")) {
                        AsyncHttpServerRequestImpl.this.onNotHttp();
                        AsyncHttpServerRequestImpl.this.mSocket.setDataCallback(null);
                    }
                } else if ("\r".equals(s)) {
                    AsyncHttpServerRequestImpl.this.mBody = Util.getBody(AsyncHttpServerRequestImpl.this.mRawHeaders);
                    AsyncHttpServerRequestImpl.this.mSocket.setDataCallback(Util.getBodyDecoder(AsyncHttpServerRequestImpl.this.mBody, AsyncHttpServerRequestImpl.this.mRawHeaders, true, AsyncHttpServerRequestImpl.this.mReporter));
                    AsyncHttpServerRequestImpl.this.onHeadersReceived();
                } else {
                    AsyncHttpServerRequestImpl.this.mRawHeaders.addLine(s);
                }
            } catch (Exception ex) {
                AsyncHttpServerRequestImpl.this.onCompleted(ex);
            }
        }
    }

    public abstract void onHeadersReceived();

    public void setEndCallback(CompletedCallback callback) {
        this.mCompleted = callback;
    }

    public CompletedCallback getEndCallback() {
        return this.mCompleted;
    }

    public void onCompleted(Exception e) {
        if (this.mBody != null) {
            this.mBody.onCompleted(e);
        }
        if (this.mCompleted != null) {
            this.mCompleted.onCompleted(e);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onNotHttp() {
        System.out.println("not http: " + this.mRawHeaders.getStatusLine());
        System.out.println("not http: " + this.mRawHeaders.getStatusLine().length());
    }

    /* Access modifiers changed, original: 0000 */
    public RawHeaders getRawHeaders() {
        return this.mRawHeaders;
    }

    /* Access modifiers changed, original: 0000 */
    public void setSocket(AsyncSocket socket) {
        this.mSocket = socket;
        new LineEmitter(this.mSocket).setLineCallback(this.mHeaderCallback);
    }

    public AsyncSocket getSocket() {
        return this.mSocket;
    }

    public RequestHeaders getHeaders() {
        return this.mHeaders;
    }

    public void setDataCallback(DataCallback callback) {
        this.mSocket.setDataCallback(callback);
    }

    public DataCallback getDataCallback() {
        return this.mSocket.getDataCallback();
    }

    public boolean isChunked() {
        return this.mSocket.isChunked();
    }

    public Matcher getMatcher() {
        return this.mMatcher;
    }

    public AsyncHttpRequestBody getBody() {
        return this.mBody;
    }

    public void pause() {
        this.mSocket.pause();
    }

    public void resume() {
        this.mSocket.resume();
    }

    public boolean isPaused() {
        return this.mSocket.isPaused();
    }
}
