package com.koushikdutta.async.http;

import android.util.Base64;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.BufferedDataSink;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.UUID;

public class WebSocketImpl implements WebSocket {
    static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private DataCallback mDataCallback;
    CompletedCallback mExceptionCallback;
    HybiParser mParser;
    BufferedDataSink mSink;
    private AsyncSocket mSocket;
    private StringCallback mStringCallback;
    private LinkedList<ByteBufferList> pending;

    private static String SHA1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            return Base64.encodeToString(md.digest(), 0);
        } catch (Exception e) {
            return null;
        }
    }

    private void addAndEmit(ByteBufferList bb) {
        if (this.pending == null) {
            Util.emitAllData((DataEmitter) this, bb);
            if (bb.remaining() > 0) {
                this.pending = new LinkedList();
                this.pending.add(bb);
                return;
            }
            return;
        }
        while (!isPaused()) {
            bb = (ByteBufferList) this.pending.remove();
            Util.emitAllData((DataEmitter) this, bb);
            if (bb.remaining() > 0) {
                this.pending.add(0, bb);
            }
        }
        if (this.pending.size() == 0) {
            this.pending = null;
        }
    }

    public void setupParser() {
        this.mParser = new HybiParser(this.mSocket) {
            /* Access modifiers changed, original: protected */
            public void report(Exception ex) {
                if (WebSocketImpl.this.mExceptionCallback != null) {
                    WebSocketImpl.this.mExceptionCallback.onCompleted(ex);
                }
            }

            /* Access modifiers changed, original: protected */
            public void onMessage(byte[] payload) {
                WebSocketImpl.this.addAndEmit(new ByteBufferList(payload));
            }

            /* Access modifiers changed, original: protected */
            public void onMessage(String payload) {
                if (WebSocketImpl.this.mStringCallback != null) {
                    WebSocketImpl.this.mStringCallback.onStringAvailable(payload);
                }
            }

            /* Access modifiers changed, original: protected */
            public void onDisconnect(int code, String reason) {
                WebSocketImpl.this.mSocket.close();
            }

            /* Access modifiers changed, original: protected */
            public void sendFrame(byte[] frame) {
                WebSocketImpl.this.mSink.write(ByteBuffer.wrap(frame));
            }
        };
        this.mParser.setMasking(false);
        if (this.mSocket.isPaused()) {
            this.mSocket.resume();
        }
    }

    public WebSocketImpl(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
        this(request.getSocket());
        String sha1 = SHA1(new StringBuilder(String.valueOf(request.getHeaders().getHeaders().get("Sec-WebSocket-Key"))).append(MAGIC).toString());
        String origin = request.getHeaders().getHeaders().get("Origin");
        response.responseCode(101);
        response.getHeaders().getHeaders().set("Upgrade", "WebSocket");
        response.getHeaders().getHeaders().set("Connection", "Upgrade");
        response.getHeaders().getHeaders().set("Sec-WebSocket-Accept", sha1);
        response.writeHead();
        setupParser();
    }

    public static void addWebSocketUpgradeHeaders(RawHeaders headers, String protocol) {
        String key = UUID.randomUUID().toString();
        headers.set("Sec-WebSocket-Version", "13");
        headers.set("Sec-WebSocket-Key", key);
        headers.set("Connection", "Upgrade");
        headers.set("Upgrade", "websocket");
        if (protocol != null) {
            headers.set("Sec-WebSocket-Protocol", protocol);
        }
    }

    public WebSocketImpl(AsyncSocket socket) {
        this.mSocket = socket;
        this.mSink = new BufferedDataSink(this.mSocket);
    }

    public static WebSocket finishHandshake(RawHeaders requestHeaders, AsyncHttpResponse response) {
        if (response == null || response.getHeaders().getHeaders().getResponseCode() != 101 || !"websocket".equalsIgnoreCase(response.getHeaders().getHeaders().get("Upgrade"))) {
            return null;
        }
        String sha1 = response.getHeaders().getHeaders().get("Sec-WebSocket-Accept");
        if (sha1 == null) {
            return null;
        }
        String key = requestHeaders.get("Sec-WebSocket-Key");
        if (key == null || !sha1.equalsIgnoreCase(SHA1(new StringBuilder(String.valueOf(key)).append(MAGIC).toString()).trim())) {
            return null;
        }
        WebSocket ret = new WebSocketImpl(response.detachSocket());
        ((WebSocketImpl) ret).setupParser();
        return ret;
    }

    public void close() {
        this.mSocket.close();
    }

    public void setClosedCallback(CompletedCallback handler) {
        this.mSocket.setClosedCallback(handler);
    }

    public CompletedCallback getClosedCallback() {
        return this.mSocket.getClosedCallback();
    }

    public void setEndCallback(CompletedCallback callback) {
        this.mExceptionCallback = callback;
    }

    public CompletedCallback getEndCallback() {
        return this.mExceptionCallback;
    }

    public void send(byte[] bytes) {
        this.mSink.write(ByteBuffer.wrap(this.mParser.frame(bytes)));
    }

    public void send(String string) {
        this.mSink.write(ByteBuffer.wrap(this.mParser.frame(string)));
    }

    public void setStringCallback(StringCallback callback) {
        this.mStringCallback = callback;
    }

    public void setDataCallback(DataCallback callback) {
        this.mDataCallback = callback;
    }

    public StringCallback getStringCallback() {
        return this.mStringCallback;
    }

    public DataCallback getDataCallback() {
        return this.mDataCallback;
    }

    public boolean isOpen() {
        return this.mSocket.isOpen();
    }

    public boolean isBuffering() {
        return this.mSink.remaining() > 0;
    }

    public void write(ByteBuffer bb) {
        byte[] buf = new byte[bb.remaining()];
        bb.get(buf);
        bb.position(0);
        bb.limit(0);
        send(buf);
    }

    public void write(ByteBufferList bb) {
        byte[] buf = new byte[bb.remaining()];
        bb.get(buf);
        bb.clear();
        send(buf);
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mSink.setWriteableCallback(handler);
    }

    public WritableCallback getWriteableCallback() {
        return this.mSink.getWriteableCallback();
    }

    public AsyncSocket getSocket() {
        return this.mSocket;
    }

    public AsyncServer getServer() {
        return this.mSocket.getServer();
    }

    public boolean isChunked() {
        return false;
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
