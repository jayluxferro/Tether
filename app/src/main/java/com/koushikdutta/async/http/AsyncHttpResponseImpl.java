package com.koushikdutta.async.http;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.FilteredDataCallback;
import com.koushikdutta.async.LineEmitter;
import com.koushikdutta.async.LineEmitter.StringCallback;
import com.koushikdutta.async.NullDataCallback;
import com.koushikdutta.async.Util;
//import com.koushikdutta.async.http.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.filter.ChunkedOutputFilter;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.ResponseHeaders;
import java.nio.ByteBuffer;
import junit.framework.Assert;

abstract class AsyncHttpResponseImpl extends FilteredDataCallback implements AsyncHttpResponse {
    boolean mCompleted = false;
    private boolean mFirstWrite = true;
    StringCallback mHeaderCallback = new C02772();
    private ResponseHeaders mHeaders;
    private RawHeaders mRawHeaders = new RawHeaders();
    private CompletedCallback mReporter = new C02761();
    private AsyncHttpRequest mRequest;
    DataSink mSink;
    private AsyncSocket mSocket;
    private AsyncHttpRequestBody mWriter;

    /* renamed from: com.koushikdutta.async.http.AsyncHttpResponseImpl$1 */
    class C02761 implements CompletedCallback {
        C02761() {
        }

        public void onCompleted(Exception error) {
            AsyncHttpResponseImpl.this.report(error);
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpResponseImpl$2 */
    class C02772 implements StringCallback {
        C02772() {
        }

        public void onStringAvailable(String s) {
            try {
                if (AsyncHttpResponseImpl.this.mRawHeaders.getStatusLine() == null) {
                    AsyncHttpResponseImpl.this.mRawHeaders.setStatusLine(s);
                } else if ("\r".equals(s)) {
                    AsyncHttpResponseImpl.this.mHeaders = new ResponseHeaders(AsyncHttpResponseImpl.this.mRequest.getUri(), AsyncHttpResponseImpl.this.mRawHeaders);
                    AsyncHttpResponseImpl.this.onHeadersReceived();
                    if (AsyncHttpResponseImpl.this.mSocket != null) {
                        AsyncHttpResponseImpl.this.mSocket.setDataCallback(Util.getBodyDecoder(AsyncHttpResponseImpl.this, AsyncHttpResponseImpl.this.mRawHeaders, false, AsyncHttpResponseImpl.this.mReporter));
                    }
                } else {
                    AsyncHttpResponseImpl.this.mRawHeaders.addLine(s);
                }
            } catch (Exception ex) {
                AsyncHttpResponseImpl.this.report(ex);
            }
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpResponseImpl$3 */
    class C02783 implements CompletedCallback {
        C02783() {
        }

        public void onCompleted(Exception ex) {
            if (AsyncHttpResponseImpl.this.mWriter != null) {
                AsyncHttpResponseImpl.this.mWriter.write(AsyncHttpResponseImpl.this.mRequest, AsyncHttpResponseImpl.this);
            }
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpResponseImpl$4 */
    class C02794 implements CompletedCallback {
        C02794() {
        }

        public void onCompleted(Exception ex) {
            if (!AsyncHttpResponseImpl.this.mCompleted) {
                AsyncHttpResponseImpl.this.report(new Exception("connection closed before response completed."));
            }
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpResponseImpl$5 */
    class C03105 extends NullDataCallback {
        C03105() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            super.onDataAvailable(emitter, bb);
            AsyncHttpResponseImpl.this.mSocket.close();
        }
    }

    public abstract void onHeadersReceived();

    public RawHeaders getRawHeaders() {
        return this.mRawHeaders;
    }

    public AsyncSocket getSocket() {
        return this.mSocket;
    }

    /* Access modifiers changed, original: 0000 */
    public void setSocket(AsyncSocket exchange) {
        this.mSocket = exchange;
        if (this.mSocket != null) {
            this.mWriter = this.mRequest.getBody();
            if (this.mWriter != null) {
                this.mRequest.getHeaders().setContentType(this.mWriter.getContentType());
                if (this.mWriter.length() != -1) {
                    this.mRequest.getHeaders().setContentLength(this.mWriter.length());
                    this.mSink = this.mSocket;
                } else {
                    this.mRequest.getHeaders().getHeaders().set("Transfer-Encoding", "Chunked");
                    this.mSink = new ChunkedOutputFilter(this.mSocket);
                }
            } else {
                this.mSink = this.mSocket;
            }
            Util.writeAll((DataSink) exchange, this.mRequest.getRequestString().getBytes(), new C02783());
            new LineEmitter(exchange).setLineCallback(this.mHeaderCallback);
            this.mSocket.setEndCallback(this.mReporter);
            this.mSocket.setClosedCallback(new C02794());
        }
    }

    /* Access modifiers changed, original: protected */
    public void report(Exception e) {
        super.report(e);
        this.mSocket.setDataCallback(new C03105());
        this.mSocket.setWriteableCallback(null);
        this.mSocket.setClosedCallback(null);
        this.mSocket.setEndCallback(null);
        this.mCompleted = true;
    }

    public AsyncHttpResponseImpl(AsyncHttpRequest request) {
        this.mRequest = request;
    }

    public ResponseHeaders getHeaders() {
        return this.mHeaders;
    }

    private void assertContent() {
        boolean z = false;
        if (this.mFirstWrite) {
            this.mFirstWrite = false;
            Assert.assertNotNull(this.mRequest.getHeaders().getHeaders().get("Content-Type"));
            if (!(this.mRequest.getHeaders().getHeaders().get("Transfer-Encoding") == null && this.mRequest.getHeaders().getContentLength() == -1)) {
                z = true;
            }
            Assert.assertTrue(z);
        }
    }

    public void write(ByteBuffer bb) {
        assertContent();
        this.mSink.write(bb);
    }

    public void write(ByteBufferList bb) {
        assertContent();
        this.mSink.write(bb);
    }

    public void end() {
        write(ByteBuffer.wrap(new byte[0]));
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mSink.setWriteableCallback(handler);
    }

    public WritableCallback getWriteableCallback() {
        return this.mSink.getWriteableCallback();
    }

    public boolean isOpen() {
        return this.mSink.isOpen();
    }

    public void close() {
        this.mSink.close();
    }

    public void setClosedCallback(CompletedCallback handler) {
        this.mSink.setClosedCallback(handler);
    }

    public CompletedCallback getClosedCallback() {
        return this.mSink.getClosedCallback();
    }

    public AsyncServer getServer() {
        return this.mSocket.getServer();
    }
}
