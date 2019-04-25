package com.koushikdutta.async.http.server;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.BufferedDataSink;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.FilteredDataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.filter.ChunkedOutputFilter;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.ResponseHeaders;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import junit.framework.Assert;
import org.json.JSONObject;

public class AsyncHttpServerResponseImpl implements AsyncHttpServerResponse {
    FilteredDataSink mChunker;
    private int mContentLength = -1;
    boolean mEnded;
    boolean mHasWritten = false;
    private boolean mHeadWritten = false;
    private ResponseHeaders mHeaders = new ResponseHeaders(null, this.mRawHeaders);
    private RawHeaders mRawHeaders = new RawHeaders();
    AsyncHttpServerRequestImpl mRequest;
    BufferedDataSink mSink;
    AsyncSocket mSocket;

    /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServerResponseImpl$1 */
    class C02921 implements CompletedCallback {
        C02921() {
        }

        public void onCompleted(Exception ex) {
            AsyncHttpServerResponseImpl.this.end();
        }
    }

    public ResponseHeaders getHeaders() {
        return this.mHeaders;
    }

    public AsyncSocket getSocket() {
        return this.mSocket;
    }

    AsyncHttpServerResponseImpl(AsyncSocket socket, AsyncHttpServerRequestImpl req) {
        this.mSocket = socket;
        this.mSink = new BufferedDataSink(socket);
        this.mRequest = req;
        this.mRawHeaders.set("Connection", "Keep-Alive");
    }

    public void write(ByteBuffer bb) {
        if (bb.remaining() != 0) {
            writeInternal(bb);
        }
    }

    private void writeInternal(ByteBuffer bb) {
        initFirstWrite();
        this.mChunker.write(bb);
    }

    /* Access modifiers changed, original: 0000 */
    public void initFirstWrite() {
        if (!this.mHasWritten) {
            Assert.assertTrue(this.mContentLength < 0);
            Assert.assertNotNull(this.mRawHeaders.getStatusLine());
            this.mRawHeaders.set("Transfer-Encoding", "Chunked");
            writeHead();
            this.mSink.setMaxBuffer(0);
            this.mHasWritten = true;
            this.mChunker = new ChunkedOutputFilter(this.mSink);
        }
    }

    private void writeInternal(ByteBufferList bb) {
        Assert.assertTrue(!this.mEnded);
        initFirstWrite();
        this.mChunker.write(bb);
    }

    public void write(ByteBufferList bb) {
        if (bb.remaining() != 0) {
            writeInternal(bb);
        }
    }

    public void setWriteableCallback(WritableCallback handler) {
        initFirstWrite();
        this.mChunker.setWriteableCallback(handler);
    }

    public WritableCallback getWriteableCallback() {
        initFirstWrite();
        return this.mChunker.getWriteableCallback();
    }

    public void end() {
        if (this.mRawHeaders.get("Transfer-Encoding") == null) {
            send("text/html", "");
            onEnd();
            return;
        }
        initFirstWrite();
        this.mChunker.setMaxBuffer(Integer.MAX_VALUE);
        this.mChunker.write(new ByteBufferList());
        onEnd();
    }

    public void writeHead() {
        Assert.assertFalse(this.mHeadWritten);
        this.mHeadWritten = true;
        this.mSink.write(ByteBuffer.wrap(this.mRawHeaders.toHeaderString().getBytes()));
    }

    public void setContentType(String contentType) {
        Assert.assertFalse(this.mHeadWritten);
        this.mRawHeaders.set("Content-Type", contentType);
    }

    public void send(String contentType, String string) {
        try {
            if (this.mRawHeaders.getStatusLine() == null) {
                responseCode(200);
            }
            Assert.assertTrue(this.mContentLength < 0);
            byte[] bytes = string.getBytes("UTF-8");
            this.mContentLength = bytes.length;
            this.mRawHeaders.set("Content-Length", Integer.toString(bytes.length));
            this.mRawHeaders.set("Content-Type", contentType);
            writeHead();
            this.mSink.write(ByteBuffer.wrap(string.getBytes()));
            onEnd();
        } catch (UnsupportedEncodingException e) {
            Assert.fail();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onEnd() {
        this.mEnded = true;
    }

    /* Access modifiers changed, original: protected */
    public void report(Exception e) {
    }

    public void send(String string) {
        responseCode(200);
        send("text/html", string);
    }

    public void send(JSONObject json) {
        send("application/json", json.toString());
    }

    public void sendFile(File file) {
        try {
            InputStream fin = new FileInputStream(file);
            this.mRawHeaders.set("Content-Type", AsyncHttpServer.getContentType(file.getAbsolutePath()));
            responseCode(200);
            Util.pump(fin, (DataSink) this, new C02921());
        } catch (FileNotFoundException e) {
            responseCode(404);
            end();
        }
    }

    public void responseCode(int code) {
        String status = AsyncHttpServer.getResponseCodeDescription(code);
        this.mRawHeaders.setStatusLine(String.format("HTTP/1.1 %d %s", new Object[]{Integer.valueOf(code), status}));
    }

    public void redirect(String location) {
        responseCode(302);
        this.mRawHeaders.set("Location", location);
        end();
    }

    public void onCompleted(Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
        end();
    }

    public boolean isOpen() {
        return this.mSink.isOpen();
    }

    public void close() {
        end();
        if (this.mChunker != null) {
            this.mChunker.close();
        } else {
            this.mSink.close();
        }
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
