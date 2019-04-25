package com.koushikdutta.async;

import android.os.Build.VERSION;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import junit.framework.Assert;
import org.apache.http.conn.ssl.StrictHostnameVerifier;

public class AsyncSSLSocket implements AsyncSocket {
    static SSLContext ctx;
    SSLEngine engine;
    boolean finishedHandshake = false;
    DataCallback mDataCallback;
    BufferedDataEmitter mEmitter;
    private String mHost;
    private int mPort;
    ByteBuffer mReadTmp = ByteBuffer.allocate(8192);
    BufferedDataSink mSink;
    AsyncSocket mSocket;
    boolean mUnwrapping = false;
    private boolean mWrapping = false;
    ByteBuffer mWriteTmp = ByteBuffer.allocate(8192);
    WritableCallback mWriteableCallback;

    /* renamed from: com.koushikdutta.async.AsyncSSLSocket$1 */
    class C00961 implements X509TrustManager {
        C00961() {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
            for (X509Certificate cert : certs) {
                cert.getCriticalExtensionOIDs().remove("2.5.29.15");
            }
        }
    }

    /* renamed from: com.koushikdutta.async.AsyncSSLSocket$2 */
    class C02732 implements DataCallback {
        C02732() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            if (!AsyncSSLSocket.this.mUnwrapping) {
                try {
                    ByteBuffer b;
                    AsyncSSLSocket.this.mUnwrapping = true;
                    ByteBufferList out = new ByteBufferList();
                    AsyncSSLSocket.this.mReadTmp.position(0);
                    AsyncSSLSocket.this.mReadTmp.limit(AsyncSSLSocket.this.mReadTmp.capacity());
                    if (bb.size() > 1) {
                        b = bb.read(bb.remaining());
                    } else if (bb.size() == 1) {
                        b = bb.peek();
                    } else {
                        b = ByteBuffer.allocate(0);
                    }
                    while (true) {
                        int remaining = b.remaining();
                        SSLEngineResult res = AsyncSSLSocket.this.engine.unwrap(b, AsyncSSLSocket.this.mReadTmp);
                        if (res.getStatus() == Status.BUFFER_OVERFLOW) {
                            AsyncSSLSocket.this.addToPending(out);
                            AsyncSSLSocket.this.mReadTmp = ByteBuffer.allocate(AsyncSSLSocket.this.mReadTmp.remaining() * 2);
                            remaining = -1;
                        }
                        AsyncSSLSocket.this.handleResult(res);
                        if (b.remaining() == remaining) {
                            break;
                        }
                    }
                    AsyncSSLSocket.this.addToPending(out);
                    Util.emitAllData(AsyncSSLSocket.this, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AsyncSSLSocket.this.report(ex);
                } finally {
                    AsyncSSLSocket.this.mUnwrapping = false;
                }
            }
        }
    }

    public AsyncSSLSocket(AsyncSocket socket, String host, int port) {
        this.mSocket = socket;
        if (host != null) {
            this.engine = ctx.createSSLEngine(host, port);
        } else {
            this.engine = ctx.createSSLEngine();
        }
        this.mHost = host;
        this.mPort = port;
        this.engine.setUseClientMode(true);
        this.mSink = new BufferedDataSink(socket);
        this.mSink.setMaxBuffer(0);
        this.mEmitter = new BufferedDataEmitter(socket);
        this.mEmitter.setDataCallback(new C02732());
    }

    /* Access modifiers changed, original: 0000 */
    public void addToPending(ByteBufferList out) {
        if (this.mReadTmp.position() > 0) {
            this.mReadTmp.limit(this.mReadTmp.position());
            this.mReadTmp.position(0);
            out.add(this.mReadTmp);
            this.mReadTmp = ByteBuffer.allocate(this.mReadTmp.capacity());
        }
    }

    static {
        try {
            if (VERSION.SDK_INT <= 15) {
                throw new Exception();
            }
            ctx = SSLContext.getInstance("Default");
        } catch (Exception ex) {
            try {
                ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{new C00961()}, null);
            } catch (Exception ex2) {
                ex.printStackTrace();
                ex2.printStackTrace();
            }
        }
    }

    public String getHost() {
        return this.mHost;
    }

    public int getPort() {
        return this.mPort;
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x006e A:{Catch:{ Exception -> 0x007f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleResult(SSLEngineResult res) {
        if (res.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            this.engine.getDelegatedTask().run();
        }
        if (res.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
            write(ByteBuffer.allocate(0));
        }
        if (res.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) {
            this.mEmitter.onDataAvailable();
        }
        try {
            if (!this.finishedHandshake) {
                if (this.engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING || this.engine.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(null);
                    boolean trusted = false;
                    TrustManager[] trustManagers = tmf.getTrustManagers();
                    int length = trustManagers.length;
                    int i = 0;
                    while (i < length) {
                        try {
                            X509Certificate[] certs = (X509Certificate[]) this.engine.getSession().getPeerCertificates();
                            ((X509TrustManager) trustManagers[i]).checkServerTrusted(certs, "SSL");
                            if (this.mHost != null) {
                                new StrictHostnameVerifier().verify(this.mHost, StrictHostnameVerifier.getCNs(certs[0]), StrictHostnameVerifier.getDNSSubjectAlts(certs[0]));
                            }
                            trusted = true;
                            this.finishedHandshake = true;
                            if (!trusted) {
                                AsyncSSLException e = new AsyncSSLException();
                                report(e);
                                if (!e.getIgnore()) {
                                    throw e;
                                }
                            }
                            Assert.assertNotNull(this.mWriteableCallback);
                            this.mWriteableCallback.onWriteable();
                            this.mEmitter.onDataAvailable();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            i++;
                        }
                    }
                    this.finishedHandshake = true;
                    if (trusted) {
                    }
                    Assert.assertNotNull(this.mWriteableCallback);
                    this.mWriteableCallback.onWriteable();
                    this.mEmitter.onDataAvailable();
                }
            }
        } catch (Exception ex2) {
            report(ex2);
        }
    }

    private void writeTmp() {
        this.mWriteTmp.limit(this.mWriteTmp.position());
        this.mWriteTmp.position(0);
        if (this.mWriteTmp.remaining() > 0) {
            this.mSink.write(this.mWriteTmp);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public boolean checkWrapResult(SSLEngineResult res) {
        if (res.getStatus() != Status.BUFFER_OVERFLOW) {
            return true;
        }
        this.mWriteTmp = ByteBuffer.allocate(this.mWriteTmp.remaining() * 2);
        return false;
    }

    public void write(ByteBuffer bb) {
        if (!this.mWrapping && this.mSink.remaining() <= 0) {
            this.mWrapping = true;
            SSLEngineResult res = null;
            do {
                if (this.finishedHandshake && bb.remaining() == 0) {
                    this.mWrapping = false;
                    return;
                }
                int remaining = bb.remaining();
                this.mWriteTmp.position(0);
                this.mWriteTmp.limit(this.mWriteTmp.capacity());
                try {
                    res = this.engine.wrap(bb, this.mWriteTmp);
                    if (!checkWrapResult(res)) {
                        remaining = -1;
                    }
                    writeTmp();
                    handleResult(res);
                } catch (SSLException e) {
                    report(e);
                }
                if (remaining != bb.remaining() || (res != null && res.getHandshakeStatus() == HandshakeStatus.NEED_WRAP)) {
                }
            } while (this.mSink.remaining() == 0);
            this.mWrapping = false;
        }
    }

    public void write(ByteBufferList bb) {
        if (!this.mWrapping && this.mSink.remaining() <= 0) {
            this.mWrapping = true;
            SSLEngineResult res = null;
            do {
                if (this.finishedHandshake && bb.remaining() == 0) {
                    this.mWrapping = false;
                    return;
                }
                int remaining = bb.remaining();
                this.mWriteTmp.position(0);
                this.mWriteTmp.limit(this.mWriteTmp.capacity());
                try {
                    res = this.engine.wrap(bb.toArray(), this.mWriteTmp);
                    if (!checkWrapResult(res)) {
                        remaining = -1;
                    }
                    writeTmp();
                    handleResult(res);
                } catch (SSLException e) {
                    report(e);
                }
                if (remaining != bb.remaining() || (res != null && res.getHandshakeStatus() == HandshakeStatus.NEED_WRAP)) {
                }
            } while (this.mSink.remaining() == 0);
            this.mWrapping = false;
        }
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mWriteableCallback = handler;
    }

    public WritableCallback getWriteableCallback() {
        return this.mWriteableCallback;
    }

    private void report(Exception e) {
        CompletedCallback cb = getEndCallback();
        if (cb != null) {
            cb.onCompleted(e);
        }
    }

    public void setDataCallback(DataCallback callback) {
        this.mDataCallback = callback;
    }

    public DataCallback getDataCallback() {
        return this.mDataCallback;
    }

    public boolean isChunked() {
        return this.mSocket.isChunked();
    }

    public boolean isOpen() {
        return this.mSocket.isOpen();
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
        this.mSocket.setEndCallback(callback);
    }

    public CompletedCallback getEndCallback() {
        return this.mSocket.getEndCallback();
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

    public AsyncServer getServer() {
        return this.mSocket.getServer();
    }
}
