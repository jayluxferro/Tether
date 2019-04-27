package com.koushikdutta.async.http;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.koushikdutta.async.AsyncSSLException;
import com.koushikdutta.async.AsyncSSLSocket;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.Cancelable;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.NullDataCallback;
import com.koushikdutta.async.SimpleCancelable;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.RequestCallback;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.stream.OutputStreamDataCallback;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

public class AsyncHttpClient {
    private static Hashtable<String, HashSet<AsyncSocket>> mSockets = new Hashtable();

    private interface ResultConvert {
        Object convert(ByteBufferList byteBufferList) throws Exception;
    }

    public interface WebSocketConnectCallback {
        void onCompleted(Exception exception, WebSocket webSocket);
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpClient$3 */
    static class C01303 implements ResultConvert {
        C01303() {
        }

        public Object convert(ByteBufferList b) {
            return b;
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpClient$4 */
    static class C01314 implements ResultConvert {
        C01314() {
        }

        public Object convert(ByteBufferList bb) {
            StringBuilder builder = new StringBuilder();
            Iterator it = bb.iterator();
            while (it.hasNext()) {
                ByteBuffer b = (ByteBuffer) it.next();
                builder.append(new String(b.array(), b.arrayOffset() + b.position(), b.remaining()));
            }
            return builder.toString();
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpClient$5 */
    static class C01325 implements ResultConvert {
        C01325() {
        }

        public Object convert(ByteBufferList bb) throws JSONException {
            StringBuilder builder = new StringBuilder();
            Iterator it = bb.iterator();
            while (it.hasNext()) {
                ByteBuffer b = (ByteBuffer) it.next();
                builder.append(new String(b.array(), b.arrayOffset() + b.position(), b.remaining()));
            }
            return new JSONObject(builder.toString());
        }
    }

    private static abstract class InternalConnectCallback implements ConnectCallback {
        boolean reused;

        private InternalConnectCallback() {
            this.reused = false;
        }

        /* synthetic */ InternalConnectCallback(InternalConnectCallback internalConnectCallback) {
            this();
        }
    }

    public static abstract class RequestCallbackBase<T> implements RequestCallback<T> {
        public void onProgress(AsyncHttpResponse response, int downloaded, int total) {
        }
    }

    /* renamed from: com.koushikdutta.async.http.AsyncHttpClient$1 */
    static class C01341 extends InternalConnectCallback {
        AsyncSocket cancelSocket;
        Object scheduled;
        private final /* synthetic */ HttpConnectCallback val$callback;
        private final /* synthetic */ CancelableImpl val$cancel;
        private final /* synthetic */ int val$finalPort;
        private final /* synthetic */ String val$lookup;
        private final /* synthetic */ int val$redirectCount;
        private final /* synthetic */ AsyncHttpRequest val$request;
        private final /* synthetic */ AsyncServer val$server;
        private final /* synthetic */ URI val$uri;

        C01341(AsyncHttpRequest asyncHttpRequest, AsyncServer asyncServer, final CancelableImpl cancelableImpl, final HttpConnectCallback httpConnectCallback, URI uri, int i, int i2, String str) {
            super();
            this.val$request = asyncHttpRequest;
            this.val$server = asyncServer;
            this.val$cancel = cancelableImpl;
            this.val$callback = httpConnectCallback;
            this.val$uri = uri;
            this.val$finalPort = i;
            this.val$redirectCount = i2;
            this.val$lookup = str;

            if (asyncHttpRequest.getTimeout() > 0) {
                this.scheduled = asyncServer.postDelayed(new Runnable() {
                    public void run() {
                        cancelableImpl.cancel();
                        if (C01341.this.cancelSocket != null) {
                            C01341.this.cancelSocket.close();
                        }
                        AsyncHttpClient.reportConnectedCompleted(cancelableImpl, new TimeoutException(), null, httpConnectCallback);
                    }
                }, (long) asyncHttpRequest.getTimeout());
            } else {
                this.scheduled = null;
            }
        }

        public void onConnectCompleted(Exception ex, AsyncSocket socket) {
            if (!this.val$cancel.isCanceled()) {
                this.cancelSocket = socket;
                if (ex != null) {
                    AsyncHttpClient.reportConnectedCompleted(this.val$cancel, ex, null, this.val$callback);
                    return;
                }
                AsyncHttpRequest asyncHttpRequest = this.val$request;
                final CancelableImpl cancelableImpl = this.val$cancel;
                final AsyncServer asyncServer = this.val$server;
                final AsyncHttpRequest asyncHttpRequest2 = this.val$request;
                final URI uri = this.val$uri;
                final HttpConnectCallback httpConnectCallback = this.val$callback;
                final int i = this.val$redirectCount;
                final String str = this.val$lookup;
                AsyncHttpResponseImpl ret = new AsyncHttpResponseImpl(asyncHttpRequest) {
                    boolean headersReceived;
                    boolean keepalive = false;

                    /* Access modifiers changed, original: protected */
                    public void onHeadersReceived() {
                        try {
                            if (!cancelableImpl.isCanceled()) {
                                if (C01341.this.scheduled != null) {
                                    asyncServer.removeAllCallbacks(C01341.this.scheduled);
                                }
                                this.headersReceived = true;
                                RawHeaders headers = getRawHeaders();
                                String kas = headers.get("Connection");
                                if (kas != null && "keep-alive".toLowerCase().equals(kas.toLowerCase())) {
                                    this.keepalive = true;
                                }
                                if ((headers.getResponseCode() == 301 || headers.getResponseCode() == 302) && asyncHttpRequest2.getFollowRedirect()) {
                                    URI redirect = URI.create(headers.get("Location"));
                                    if (redirect == null || redirect.getScheme() == null) {
                                        redirect = URI.create(new StringBuilder(String.valueOf(uri.toString().substring(0, uri.toString().length() - uri.getPath().length()))).append(headers.get("Location")).toString());
                                    }
                                    AsyncHttpClient.execute(asyncServer, new AsyncHttpRequest(redirect, asyncHttpRequest2.getMethod()), httpConnectCallback, i + 1, cancelableImpl);
                                    setDataCallback(new NullDataCallback());
                                    return;
                                }
                                AsyncHttpClient.reportConnectedCompleted(cancelableImpl, null, this, httpConnectCallback);
                            }
                        } catch (Exception ex) {
                            AsyncHttpClient.reportConnectedCompleted(cancelableImpl, ex, null, httpConnectCallback);
                        }
                    }

                    /* Access modifiers changed, original: protected */
                    public void report(Exception ex) {
                        if (!cancelableImpl.isCanceled()) {
                            if (ex instanceof AsyncSSLException) {
                                AsyncSSLException ase = (AsyncSSLException) ex;
                                asyncHttpRequest2.onHandshakeException(ase);
                                if (ase.getIgnore()) {
                                    return;
                                }
                            }
                            final AsyncSocket socket = getSocket();
                            if (socket != null) {
                                super.report(ex);
                                if (socket.isOpen() && ex == null) {
                                    if (this.keepalive) {
                                        HashSet<AsyncSocket> sockets = (HashSet) AsyncHttpClient.mSockets.get(str);
                                        if (sockets == null) {
                                            sockets = new HashSet();
                                            AsyncHttpClient.mSockets.put(str, sockets);
                                        }
                                        final HashSet<AsyncSocket> ss = sockets;
                                        synchronized (sockets) {
                                            sockets.add(socket);
                                            socket.setClosedCallback(new CompletedCallback() {
                                                public void onCompleted(Exception ex) {
                                                    synchronized (ss) {
                                                        ss.remove(socket);
                                                    }
                                                    socket.setClosedCallback(null);
                                                }
                                            });
                                        }
                                        return;
                                    }
                                    socket.close();
                                } else if (!this.headersReceived && ex != null) {
                                    AsyncHttpClient.reportConnectedCompleted(cancelableImpl, ex, null, httpConnectCallback);
                                }
                            }
                        }
                    }

                    public AsyncSocket detachSocket() {
                        AsyncSocket socket = getSocket();
                        if (socket == null) {
                            return null;
                        }
                        socket.setWriteableCallback(null);
                        socket.setClosedCallback(null);
                        socket.setEndCallback(null);
                        socket.setDataCallback(null);
                        setSocket(null);
                        return socket;
                    }

                    public boolean isReusedSocket() {
                        return C01341.this.reused;
                    }
                };
                if (!this.reused && this.val$request.getUri().getScheme().equals("https")) {
                    socket = new AsyncSSLSocket(socket, this.val$uri.getHost(), this.val$finalPort);
                }
                ret.setSocket(socket);
            } else if (socket != null) {
                socket.close();
            }
        }
    }

    private static class CancelableImpl extends SimpleCancelable {
        private CancelableImpl() {
        }

        /* synthetic */ CancelableImpl(CancelableImpl cancelableImpl) {
            this();
        }

        /* synthetic */ CancelableImpl(CancelableImpl cancelableImpl, CancelableImpl cancelableImpl2) {
            this();
        }
    }

    public static abstract class DownloadCallback extends RequestCallbackBase<ByteBufferList> {
    }

    public static abstract class FileCallback extends RequestCallbackBase<File> {
    }

    public static abstract class JSONObjectCallback extends RequestCallbackBase<JSONObject> {
    }

    public static abstract class StringCallback extends RequestCallbackBase<String> {
    }

    private static class CancelableRequest extends CancelableImpl {
        AsyncHttpResponse response;

        private CancelableRequest() {
            super();
        }

        /* synthetic */ CancelableRequest(CancelableRequest cancelableRequest) {
            this();
        }

        /* synthetic */ CancelableRequest(CancelableRequest cancelableRequest, CancelableRequest cancelableRequest2) {
            this();
        }

        public Cancelable cancel() {
            Cancelable ret = super.cancel();
            if (this.response != null) {
                this.response.close();
            }
            return ret;
        }
    }

    public static Cancelable execute(AsyncHttpRequest request, HttpConnectCallback callback) {
        return execute(AsyncServer.getDefault(), request, callback);
    }

    public static Cancelable execute(AsyncServer server, AsyncHttpRequest request, HttpConnectCallback callback) {
        CancelableImpl ret = new CancelableImpl();
        ret = new CancelableImpl();
        execute(server, request, callback, 0, ret);
        return ret;
    }

    private static void reportConnectedCompleted(CancelableImpl cancel, Exception ex, AsyncHttpResponseImpl response, HttpConnectCallback callback) {
        cancel.setComplete(true);
        callback.onConnectCompleted(ex, response);
    }

    private static void execute(AsyncServer server, AsyncHttpRequest request, HttpConnectCallback callback, int redirectCount, CancelableImpl cancel) {
        if (redirectCount > 5) {
            reportConnectedCompleted(cancel, new Exception("too many redirects"), null, callback);
            return;
        }
        URI uri = request.getUri();
        int port = uri.getPort();
        if (port == -1) {
            if (uri.getScheme().equals("http")) {
                port = 80;
            } else if (uri.getScheme().equals("https")) {
                port = 443;
            } else {
                reportConnectedCompleted(cancel, new Exception("invalid uri scheme"), null, callback);
                return;
            }
        }
        String lookup = uri.getScheme() + "//" + uri.getHost() + ":" + port;
        final InternalConnectCallback socketConnected = new C01341(request, server, cancel, callback, uri, port, redirectCount, lookup);
        HashSet<AsyncSocket> sockets = (HashSet) mSockets.get(lookup);
        if (sockets != null) {
            synchronized (sockets) {
                Iterator it = sockets.iterator();
                while (it.hasNext()) {
                    final AsyncSocket socket = (AsyncSocket) it.next();
                    if (socket.isOpen()) {
                        sockets.remove(socket);
                        socket.setClosedCallback(null);
                        server.post(new Runnable() {
                            public void run() {
                                Log.i("Async", "Reusing socket.");
                                socketConnected.reused = true;
                                socketConnected.onConnectCompleted(null, socket);
                            }
                        });
                        return;
                    }
                }
            }
        }
        server.connectSocket(uri.getHost(), port, socketConnected);
    }

    public static Cancelable execute(URI uri, HttpConnectCallback callback) {
        return execute(AsyncServer.getDefault(), new AsyncHttpGet(uri), callback);
    }

    public static Cancelable execute(String uri, HttpConnectCallback callback) {
        return execute(AsyncServer.getDefault(), new AsyncHttpGet(URI.create(uri)), callback);
    }

    public static Cancelable get(String uri, DownloadCallback callback) {
        return get(uri, (RequestCallback) callback, new C01303());
    }

    public static Cancelable get(String uri, StringCallback callback) {
        return execute(new AsyncHttpGet(uri), callback);
    }

    public static Cancelable execute(AsyncHttpRequest req, StringCallback callback) {
        return execute(req, (RequestCallback) callback, new C01314());
    }

    public static Cancelable get(String uri, JSONObjectCallback callback) {
        return execute(new AsyncHttpGet(uri), callback);
    }

    public static Cancelable execute(AsyncHttpRequest req, JSONObjectCallback callback) {
        return execute(req, (RequestCallback) callback, new C01325());
    }

    private static void invoke(Handler handler, final RequestCallback callback, AsyncServer server, final AsyncHttpResponse response, final Exception e, final Object result) {
        if (callback != null) {
            if (handler == null) {
                server.post(new Runnable() {
                    public void run() {
                        callback.onCompleted(e, response, result);
                    }
                });
            } else {
                handler.post(new Runnable() {
                    public void run() {
                        callback.onCompleted(e, response, result);
                    }
                });
            }
        }
    }

    private static void invokeProgress(RequestCallback callback, AsyncHttpResponse response, int downloaded, int total) {
        if (callback != null) {
            callback.onProgress(response, downloaded, total);
        }
    }

    public static Cancelable get(String uri, String filename, FileCallback callback) {
        return execute(new AsyncHttpGet(uri), filename, callback);
    }

    public static Cancelable get(String uri, final DataSink sink, final CompletedCallback callback) {
        sink.setClosedCallback(callback);
        return execute(new AsyncHttpGet(URI.create(uri)), new HttpConnectCallback() {
            public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                if (ex != null) {
                    callback.onCompleted(ex);
                } else {
                    Util.pump((DataEmitter) response, sink, callback);
                }
            }
        });
    }

    public static Cancelable execute(AsyncHttpRequest req, String filename, FileCallback callback) {
        Handler handler;
        if (Looper.myLooper() == null) {
            handler = null;
        } else {
            handler = new Handler();
        }
        final File file = new File(filename);
        final CancelableRequest cancel = new CancelableRequest() {
            public Cancelable cancel() {
                Cancelable ret = super.cancel();
                file.delete();
                return ret;
            }
        };
        file.getParentFile().mkdirs();
        try {
            final FileOutputStream fout = new FileOutputStream(file);
            final Handler handler2 = handler;
            final FileCallback fileCallback = callback;
            execute(AsyncServer.getDefault(), req, new HttpConnectCallback() {
                int mDownloaded = 0;

                public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                    if (ex != null) {
                        try {
                            fout.close();
                        } catch (IOException e) {
                        }
                        file.delete();
                        AsyncHttpClient.invoke(handler2, fileCallback, AsyncServer.getDefault(), response, ex, null);
                        return;
                    }
                    cancel.response = response;
                    final int contentLength = response.getHeaders().getContentLength();
                    FileOutputStream fileOutputStream = fout;

                    final AsyncHttpResponse asyncHttpResponse = response;
                    response.setDataCallback(new OutputStreamDataCallback(fileOutputStream) {
                        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                            mDownloaded += bb.remaining();
                            super.onDataAvailable(emitter, bb);
                            AsyncHttpClient.invokeProgress(fileCallback, asyncHttpResponse, mDownloaded, contentLength);
                        }
                    });
                    final FileOutputStream fileOutputStream2 = fout;
                    final Handler handler = handler2;
                    final FileCallback fileCallback2 = fileCallback;
                    final AsyncHttpResponse asyncHttpResponse2 = response;
                    response.setEndCallback(new CompletedCallback() {
                        public void onCompleted(Exception ex) {
                            try {
                                fileOutputStream2.close();
                                if (ex != null) {
                                    file.delete();
                                    AsyncHttpClient.invoke(handler, fileCallback2, AsyncServer.getDefault(), asyncHttpResponse2, ex, null);
                                    return;
                                }
                                AsyncHttpClient.invoke(handler, fileCallback2, AsyncServer.getDefault(), asyncHttpResponse2, null, file);
                            } catch (IOException e) {
                                AsyncHttpClient.invoke(handler, fileCallback2, AsyncServer.getDefault(), asyncHttpResponse2, e, null);
                            }
                        }
                    });
                }
            }, 0, cancel);
            return cancel;
        } catch (FileNotFoundException e) {
            invoke(handler, callback, AsyncServer.getDefault(), null, e, null);
            return SimpleCancelable.COMPLETED;
        }
    }

    private static Cancelable execute(AsyncHttpRequest req, final RequestCallback callback, final ResultConvert convert) {
        final Handler handler = Looper.myLooper() == null ? null : new Handler();
        final CancelableRequest cancel = new CancelableRequest();
        execute(AsyncServer.getDefault(), req, new HttpConnectCallback() {
            ByteBufferList buffer = new ByteBufferList();
            int mDownloaded = 0;

            public void onConnectCompleted(Exception ex, final AsyncHttpResponse response) {
                if (ex != null) {
                    AsyncHttpClient.invoke(handler, callback, AsyncServer.getDefault(), response, ex, null);
                    return;
                }
                cancel.response = response;
                final int contentLength = response.getHeaders().getContentLength();
                final RequestCallback requestCallback = callback;
                response.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {

                        mDownloaded += bb.remaining();
                        buffer.add(bb);
                        bb.clear();
                        AsyncHttpClient.invokeProgress(requestCallback, response, mDownloaded, contentLength);
                    }
                });
                final ResultConvert resultConvert = convert;

                final RequestCallback requestCallback2 = callback;
                final AsyncHttpResponse asyncHttpResponse = response;
                response.setEndCallback(new CompletedCallback() {
                    public void onCompleted(Exception ex) {
                        try {
                            Object obj;
                            Object value = resultConvert.convert(buffer);

                            RequestCallback requestCallback = requestCallback2;
                            AsyncServer asyncServer = AsyncServer.getDefault();

                            if (buffer != null) {
                                obj = value;
                            } else {
                                obj = null;
                            }
                            AsyncHttpClient.invoke(handler, requestCallback, asyncServer, asyncHttpResponse, ex, obj);
                        } catch (Exception e) {
                            AsyncHttpClient.invoke(handler, requestCallback2, AsyncServer.getDefault(), asyncHttpResponse, e, null);
                        }
                    }
                });
            }
        }, 0, cancel);
        return cancel;
    }

    private static Cancelable get(String uri, RequestCallback callback, ResultConvert convert) {
        return execute(new AsyncHttpGet(URI.create(uri)), callback, convert);
    }

    public static void websocket(final AsyncHttpRequest req, String protocol, final WebSocketConnectCallback callback) {
        WebSocketImpl.addWebSocketUpgradeHeaders(req.getHeaders().getHeaders(), protocol);
        execute(req, new HttpConnectCallback() {
            public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                if (ex != null) {
                    callback.onCompleted(ex, null);
                    return;
                }
                WebSocket ws = WebSocketImpl.finishHandshake(req.getHeaders().getHeaders(), response);
                if (ws == null) {
                    ex = new Exception("Unable to complete websocket handshake");
                }
                callback.onCompleted(ex, ws);
            }
        });
    }

    public static void websocket(String uri, String protocol, WebSocketConnectCallback callback) {
        websocket(new AsyncHttpGet(uri), protocol, callback);
    }
}
