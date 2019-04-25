package com.koushikdutta.async.http;

import com.koushikdutta.async.AsyncSSLException;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import java.net.URI;
import java.util.List;
import junit.framework.Assert;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;

public class AsyncHttpRequest implements HttpRequest {
    private AsyncHttpRequestBody mBody;
    private boolean mFollowRedirect = true;
    private RequestHeaders mHeaders;
    private String mMethod;
    private RawHeaders mRawHeaders = new RawHeaders();
    int mTimeout;
    HttpParams params;

    /* renamed from: com.koushikdutta.async.http.AsyncHttpRequest$1 */
    class C01381 implements RequestLine {
        C01381() {
        }

        public String getUri() {
            return getUri().toString();
        }

        public ProtocolVersion getProtocolVersion() {
            return new ProtocolVersion("HTTP", 1, 1);
        }

        public String getMethod() {
            return AsyncHttpRequest.this.mMethod;
        }

        public String toString() {
            String path = AsyncHttpRequest.this.getUri().getPath();
            if (path.length() == 0) {
                path = "/";
            }
            String query = AsyncHttpRequest.this.getUri().getRawQuery();
            if (!(query == null || query.length() == 0)) {
                path = new StringBuilder(String.valueOf(path)).append("?").append(query).toString();
            }
            return String.format("%s %s HTTP/1.1", new Object[]{AsyncHttpRequest.this.mMethod, path});
        }
    }

    public RequestLine getRequestLine() {
        return new C01381();
    }

    /* Access modifiers changed, original: protected|final */
    public final String getDefaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? agent : "Java" + System.getProperty("java.version");
    }

    public String getMethod() {
        return this.mMethod;
    }

    public AsyncHttpRequest(URI uri, String method) {
        Assert.assertNotNull(uri);
        this.mMethod = method;
        this.mHeaders = new RequestHeaders(uri, this.mRawHeaders);
        this.mRawHeaders.setStatusLine(getRequestLine().toString());
        this.mHeaders.setHost(uri.getHost());
        this.mHeaders.setUserAgent(getDefaultUserAgent());
        this.mHeaders.setAcceptEncoding("gzip, deflate");
        this.mHeaders.getHeaders().set("Connection", "keep-alive");
        this.mHeaders.getHeaders().set("Accept", "*/*");
    }

    public URI getUri() {
        return this.mHeaders.getUri();
    }

    public RequestHeaders getHeaders() {
        return this.mHeaders;
    }

    public String getRequestString() {
        return this.mRawHeaders.toHeaderString();
    }

    public boolean getFollowRedirect() {
        return this.mFollowRedirect;
    }

    public void setFollowRedirect(boolean follow) {
        this.mFollowRedirect = follow;
    }

    public void setBody(AsyncHttpRequestBody body) {
        this.mBody = body;
    }

    public AsyncHttpRequestBody getBody() {
        return this.mBody;
    }

    public void onHandshakeException(AsyncSSLException e) {
    }

    public static AsyncHttpRequest create(HttpRequestBase request) {
        AsyncHttpRequest ret = new AsyncHttpRequest(request.getURI(), request.getMethod());
        for (Header header : request.getAllHeaders()) {
            ret.getHeaders().getHeaders().set(header.getName(), header.getValue());
        }
        return ret;
    }

    public void addHeader(Header header) {
        getHeaders().getHeaders().add(header.getName(), header.getValue());
    }

    public void addHeader(String name, String value) {
        getHeaders().getHeaders().add(name, value);
    }

    public boolean containsHeader(String name) {
        return getHeaders().getHeaders().get(name) != null;
    }

    public Header[] getAllHeaders() {
        Header[] ret = new Header[getHeaders().getHeaders().length()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new BasicHeader(getHeaders().getHeaders().getFieldName(i), getHeaders().getHeaders().getValue(i));
        }
        return ret;
    }

    public Header getFirstHeader(String name) {
        String value = getHeaders().getHeaders().get(name);
        if (value == null) {
            return null;
        }
        return new BasicHeader(name, value);
    }

    public Header[] getHeaders(String name) {
        List<String> vals = (List) getHeaders().getHeaders().toMultimap().get(name);
        if (vals == null) {
            return new Header[0];
        }
        Header[] ret = new Header[vals.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new BasicHeader(name, (String) vals.get(i));
        }
        return ret;
    }

    public Header getLastHeader(String name) {
        Header[] vals = getHeaders(name);
        if (vals.length == 0) {
            return null;
        }
        return vals[vals.length - 1];
    }

    public HttpParams getParams() {
        return this.params;
    }

    public ProtocolVersion getProtocolVersion() {
        return new ProtocolVersion("HTTP", 1, 1);
    }

    public HeaderIterator headerIterator() {
        Assert.fail();
        return null;
    }

    public HeaderIterator headerIterator(String name) {
        Assert.fail();
        return null;
    }

    public void removeHeader(Header header) {
        getHeaders().getHeaders().removeAll(header.getName());
    }

    public void removeHeaders(String name) {
        getHeaders().getHeaders().removeAll(name);
    }

    public void setHeader(Header header) {
        setHeader(header.getName(), header.getValue());
    }

    public void setHeader(String name, String value) {
        getHeaders().getHeaders().set(name, value);
    }

    public void setHeaders(Header[] headers) {
        for (Header header : headers) {
            setHeader(header);
        }
    }

    public void setParams(HttpParams params) {
        this.params = params;
    }

    public int getTimeout() {
        return this.mTimeout;
    }

    public void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }
}
