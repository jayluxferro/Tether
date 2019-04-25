package com.koushikdutta.async.http.server;

import android.content.Context;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.UrlEncodedFormBody;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocketImpl;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.Assert;

public class AsyncHttpServer {
    private static Hashtable<Integer, String> mCodes = new Hashtable();
    static Hashtable<String, String> mContentTypes = new Hashtable();
    Hashtable<String, ArrayList<Pair>> mActions = new Hashtable();
    CompletedCallback mCompletedCallback;
    ListenCallback mListenCallback = new C01551();
    ArrayList<AsyncServerSocket> mListeners = new ArrayList();

    private static class Pair {
        HttpServerRequestCallback callback;
        Pattern regex;

        private Pair() {
        }

        /* synthetic */ Pair(Pair pair) {
            this();
        }
    }

    public interface WebSocketRequestCallback {
        void onConnected(WebSocket webSocket, RequestHeaders requestHeaders);
    }

    /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServer$1 */
    class C01551 implements ListenCallback {
        C01551() {
        }

        public void onAccepted(final AsyncSocket socket) {
            new AsyncHttpServerRequestImpl() {
                String fullPath;
                boolean hasContinued;
                Pair match;
                String path;
                boolean requestComplete;
                AsyncHttpServerResponseImpl res;
                boolean responseComplete;

                /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServer$1$1$1 */
                class C01571 implements CompletedCallback {
                    C01571() {
                    }

                    public void onCompleted(Exception ex) {
                        C01561.this.resume();
                        if (ex != null) {
                            AsyncHttpServer.this.report(ex);
                            return;
                        }
                        C01561.this.hasContinued = true;
                        C01561.this.onHeadersReceived();
                    }
                }

                /* Access modifiers changed, original: protected */
                public void onHeadersReceived() {
                    RawHeaders headers = getRawHeaders();
                    if (this.hasContinued || !"100-continue".equals(headers.get("Expect"))) {
                        String[] parts = headers.getStatusLine().split(" ");
                        this.fullPath = parts[1];
                        this.path = this.fullPath.split("\\?")[0];
                        String action = parts[0];
                        synchronized (AsyncHttpServer.this.mActions) {
                            ArrayList<Pair> pairs = (ArrayList) AsyncHttpServer.this.mActions.get(action);
                            if (pairs != null) {
                                Iterator it = pairs.iterator();
                                while (it.hasNext()) {
                                    Pair p = (Pair) it.next();
                                    Matcher m = p.regex.matcher(this.path);
                                    if (m.matches()) {
                                        this.mMatcher = m;
                                        this.match = p;
                                        break;
                                    }
                                }
                            }
                        }
                        this.res = new AsyncHttpServerResponseImpl(socket, this) {
                            /* Access modifiers changed, original: protected */
                            public void onEnd() {
                                C01561.this.responseComplete = true;
                                C01561.this.handleOnCompleted();
                            }
                        };
                        AsyncHttpServer.this.onRequest(this, this.res);
                        if (this.match == null) {
                            this.res.responseCode(404);
                            this.res.end();
                            return;
                        } else if (!getBody().readFullyOnRequest()) {
                            this.match.callback.onRequest(this, this.res);
                            return;
                        } else if (this.requestComplete) {
                            this.match.callback.onRequest(this, this.res);
                            return;
                        } else {
                            return;
                        }
                    }
                    pause();
                    Util.writeAll(this.mSocket, "HTTP/1.1 100 Continue\r\n".getBytes(), new C01571());
                }

                public void onCompleted(Exception e) {
                    this.requestComplete = true;
                    super.onCompleted(e);
                    this.mSocket.setDataCallback(null);
                    this.mSocket.pause();
                    handleOnCompleted();
                    if (getBody().readFullyOnRequest()) {
                        this.match.callback.onRequest(this, this.res);
                    }
                }

                private void handleOnCompleted() {
                    if (this.requestComplete && this.responseComplete) {
                        C01551.this.onAccepted(socket);
                    }
                }

                public String getPath() {
                    return this.path;
                }

                public Map<String, String> getQuery() {
                    String[] parts = this.fullPath.split("\\?", 2);
                    if (parts.length < 2) {
                        return new Hashtable();
                    }
                    return UrlEncodedFormBody.parse(parts[1]);
                }
            }.setSocket(socket);
            socket.resume();
        }

        public void onCompleted(Exception error) {
            AsyncHttpServer.this.report(error);
        }

        public void onListening(AsyncServerSocket socket) {
            AsyncHttpServer.this.mListeners.add(socket);
        }
    }

    public AsyncHttpServer() {
        mContentTypes.put("js", "application/javascript");
        mContentTypes.put("json", "application/json");
        mContentTypes.put("png", "image/png");
        mContentTypes.put("jpg", "image/jpeg");
        mContentTypes.put("html", "text/html");
        mContentTypes.put("css", "text/css");
        mContentTypes.put("mp4", "video/mp4");
    }

    public void stop() {
        if (this.mListeners != null) {
            Iterator it = this.mListeners.iterator();
            while (it.hasNext()) {
                ((AsyncServerSocket) it.next()).stop();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
    }

    public void listen(AsyncServer server, int port) {
        server.listen(null, port, this.mListenCallback);
    }

    private void report(Exception ex) {
        if (this.mCompletedCallback != null) {
            this.mCompletedCallback.onCompleted(ex);
        }
    }

    public void listen(int port) {
        listen(AsyncServer.getDefault(), port);
    }

    public ListenCallback getListenCallback() {
        return this.mListenCallback;
    }

    public void setErrorCallback(CompletedCallback callback) {
        this.mCompletedCallback = callback;
    }

    public CompletedCallback getErrorCallback() {
        return this.mCompletedCallback;
    }

    public void addAction(String action, String regex, HttpServerRequestCallback callback) {
        Pair p = new Pair();
        p.regex = Pattern.compile("^" + regex);
        p.callback = callback;
        synchronized (this.mActions) {
            ArrayList<Pair> pairs = (ArrayList) this.mActions.get(action);
            if (pairs == null) {
                pairs = new ArrayList();
                this.mActions.put(action, pairs);
            }
            pairs.add(p);
        }
    }

    public void websocket(String regex, final WebSocketRequestCallback callback) {
        get(regex, new HttpServerRequestCallback() {
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                boolean hasUpgrade = false;
                String connection = request.getHeaders().getHeaders().get("Connection");
                if (connection != null) {
                    for (String c : connection.split(",")) {
                        if ("Upgrade".equalsIgnoreCase(c.trim())) {
                            hasUpgrade = true;
                            break;
                        }
                    }
                }
                if ("websocket".equals(request.getHeaders().getHeaders().get("Upgrade")) && hasUpgrade) {
                    callback.onConnected(new WebSocketImpl(request, response), request.getHeaders());
                    return;
                }
                response.responseCode(404);
                response.end();
            }
        });
    }

    public void get(String regex, HttpServerRequestCallback callback) {
        addAction(AsyncHttpGet.METHOD, regex, callback);
    }

    public void post(String regex, HttpServerRequestCallback callback) {
        addAction(AsyncHttpPost.METHOD, regex, callback);
    }

    public static InputStream getAssetStream(Context context, String asset) {
        String apkPath = context.getPackageResourcePath();
        String assetPath = "assets/" + asset;
        try {
            ZipFile zip = new ZipFile(apkPath);
            Enumeration<?> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().equals(assetPath)) {
                    return zip.getInputStream(entry);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    static {
        mCodes.put(Integer.valueOf(200), "OK");
        mCodes.put(Integer.valueOf(101), "Switching Protocols");
        mCodes.put(Integer.valueOf(404), "Not Found");
    }

    public static String getContentType(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String ct = (String) mContentTypes.get(path.substring(index + 1));
            if (ct != null) {
                return ct;
            }
        }
        return "text/plain";
    }

    public void directory(Context _context, String regex, final String assetPath) {
        final Context context = _context.getApplicationContext();
        addAction(AsyncHttpGet.METHOD, regex, new HttpServerRequestCallback() {
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                InputStream is = AsyncHttpServer.getAssetStream(context, assetPath + path);
                if (is == null) {
                    response.responseCode(404);
                    response.end();
                    return;
                }
                response.responseCode(200);
                response.getHeaders().getHeaders().add("Content-Type", AsyncHttpServer.getContentType(path));
                Util.pump(is, (DataSink) response, new CompletedCallback() {
                    public void onCompleted(Exception ex) {
                        response.end();
                    }
                });
            }
        });
    }

    public void directory(String regex, File directory) {
        directory(regex, directory, false);
    }

    public void directory(String regex, final File directory, final boolean list) {
        Assert.assertTrue(directory.isDirectory());
        addAction(AsyncHttpGet.METHOD, regex, new HttpServerRequestCallback() {

            /* renamed from: com.koushikdutta.async.http.server.AsyncHttpServer$4$1 */
            class C01491 implements Comparator<File> {
                C01491() {
                }

                public int compare(File lhs, File rhs) {
                    return lhs.getName().compareTo(rhs.getName());
                }
            }

            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                File file = new File(directory, request.getMatcher().replaceAll(""));
                if (file.isDirectory() && list) {
                    ArrayList<File> dirs = new ArrayList();
                    ArrayList<File> files = new ArrayList();
                    for (File f : file.listFiles()) {
                        if (f.isDirectory()) {
                            dirs.add(f);
                        } else {
                            files.add(f);
                        }
                    }
                    Comparator<File> c = new C01491();
                    Collections.sort(dirs, c);
                    Collections.sort(files, c);
                    files.addAll(0, dirs);
                } else if (file.isFile()) {
                    try {
                        InputStream is = new FileInputStream(file);
                        response.responseCode(200);
                        Util.pump(is, (DataSink) response, new CompletedCallback() {
                            public void onCompleted(Exception ex) {
                                response.end();
                            }
                        });
                    } catch (Exception e) {
                        response.responseCode(404);
                        response.end();
                    }
                } else {
                    response.responseCode(404);
                    response.end();
                }
            }
        });
    }

    public static String getResponseCodeDescription(int code) {
        String d = (String) mCodes.get(Integer.valueOf(code));
        if (d == null) {
            return "Unknown";
        }
        return d;
    }
}
