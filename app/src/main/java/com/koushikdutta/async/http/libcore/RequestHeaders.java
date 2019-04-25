package com.koushikdutta.async.http.libcore;

import com.koushikdutta.async.http.libcore.HeaderParser.CacheControlHandler;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class RequestHeaders {
    private String acceptEncoding;
    private String connection;
    private int contentLength = -1;
    private String contentType;
    private boolean hasAuthorization;
    private final RawHeaders headers;
    private String host;
    private String ifModifiedSince;
    private String ifNoneMatch;
    private int maxAgeSeconds = -1;
    private int maxStaleSeconds = -1;
    private int minFreshSeconds = -1;
    private boolean noCache;
    private boolean onlyIfCached;
    private String proxyAuthorization;
    private String transferEncoding;
    private final URI uri;
    private String userAgent;

    /* renamed from: com.koushikdutta.async.http.libcore.RequestHeaders$1 */
    class C01471 implements CacheControlHandler {
        C01471() {
        }

        public void handle(String directive, String parameter) {
            if (directive.equalsIgnoreCase("no-cache")) {
                RequestHeaders.this.noCache = true;
            } else if (directive.equalsIgnoreCase("max-age")) {
                RequestHeaders.this.maxAgeSeconds = HeaderParser.parseSeconds(parameter);
            } else if (directive.equalsIgnoreCase("max-stale")) {
                RequestHeaders.this.maxStaleSeconds = HeaderParser.parseSeconds(parameter);
            } else if (directive.equalsIgnoreCase("min-fresh")) {
                RequestHeaders.this.minFreshSeconds = HeaderParser.parseSeconds(parameter);
            } else if (directive.equalsIgnoreCase("only-if-cached")) {
                RequestHeaders.this.onlyIfCached = true;
            }
        }
    }

    public RequestHeaders(URI uri, RawHeaders headers) {
        this.uri = uri;
        this.headers = headers;
        CacheControlHandler handler = new C01471();
        for (int i = 0; i < headers.length(); i++) {
            String fieldName = headers.getFieldName(i);
            String value = headers.getValue(i);
            if ("Cache-Control".equalsIgnoreCase(fieldName)) {
                HeaderParser.parseCacheControl(value, handler);
            } else if ("Pragma".equalsIgnoreCase(fieldName)) {
                if (value.equalsIgnoreCase("no-cache")) {
                    this.noCache = true;
                }
            } else if ("If-None-Match".equalsIgnoreCase(fieldName)) {
                this.ifNoneMatch = value;
            } else if ("If-Modified-Since".equalsIgnoreCase(fieldName)) {
                this.ifModifiedSince = value;
            } else if ("Authorization".equalsIgnoreCase(fieldName)) {
                this.hasAuthorization = true;
            } else if ("Content-Length".equalsIgnoreCase(fieldName)) {
                try {
                    this.contentLength = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                }
            } else if ("Transfer-Encoding".equalsIgnoreCase(fieldName)) {
                this.transferEncoding = value;
            } else if ("User-Agent".equalsIgnoreCase(fieldName)) {
                this.userAgent = value;
            } else if ("Host".equalsIgnoreCase(fieldName)) {
                this.host = value;
            } else if ("Connection".equalsIgnoreCase(fieldName)) {
                this.connection = value;
            } else if ("Accept-Encoding".equalsIgnoreCase(fieldName)) {
                this.acceptEncoding = value;
            } else if ("Content-Type".equalsIgnoreCase(fieldName)) {
                this.contentType = value;
            } else if ("Proxy-Authorization".equalsIgnoreCase(fieldName)) {
                this.proxyAuthorization = value;
            }
        }
    }

    public boolean isChunked() {
        return "chunked".equalsIgnoreCase(this.transferEncoding);
    }

    public boolean hasConnectionClose() {
        return "close".equalsIgnoreCase(this.connection);
    }

    public URI getUri() {
        return this.uri;
    }

    public RawHeaders getHeaders() {
        return this.headers;
    }

    public boolean isNoCache() {
        return this.noCache;
    }

    public int getMaxAgeSeconds() {
        return this.maxAgeSeconds;
    }

    public int getMaxStaleSeconds() {
        return this.maxStaleSeconds;
    }

    public int getMinFreshSeconds() {
        return this.minFreshSeconds;
    }

    public boolean isOnlyIfCached() {
        return this.onlyIfCached;
    }

    public boolean hasAuthorization() {
        return this.hasAuthorization;
    }

    public int getContentLength() {
        return this.contentLength;
    }

    public String getTransferEncoding() {
        return this.transferEncoding;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public String getHost() {
        return this.host;
    }

    public String getConnection() {
        return this.connection;
    }

    public String getAcceptEncoding() {
        return this.acceptEncoding;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    public String getIfNoneMatch() {
        return this.ifNoneMatch;
    }

    public String getProxyAuthorization() {
        return this.proxyAuthorization;
    }

    public void setChunked() {
        if (this.transferEncoding != null) {
            this.headers.removeAll("Transfer-Encoding");
        }
        this.headers.add("Transfer-Encoding", "chunked");
        this.transferEncoding = "chunked";
    }

    public void setContentLength(int contentLength) {
        if (this.contentLength != -1) {
            this.headers.removeAll("Content-Length");
        }
        this.headers.add("Content-Length", Integer.toString(contentLength));
        this.contentLength = contentLength;
    }

    public void setUserAgent(String userAgent) {
        if (this.userAgent != null) {
            this.headers.removeAll("User-Agent");
        }
        this.headers.add("User-Agent", userAgent);
        this.userAgent = userAgent;
    }

    public void setHost(String host) {
        if (this.host != null) {
            this.headers.removeAll("Host");
        }
        this.headers.add("Host", host);
        this.host = host;
    }

    public void setConnection(String connection) {
        if (this.connection != null) {
            this.headers.removeAll("Connection");
        }
        this.headers.add("Connection", connection);
        this.connection = connection;
    }

    public void setAcceptEncoding(String acceptEncoding) {
        if (this.acceptEncoding != null) {
            this.headers.removeAll("Accept-Encoding");
        }
        this.headers.add("Accept-Encoding", acceptEncoding);
        this.acceptEncoding = acceptEncoding;
    }

    public void setContentType(String contentType) {
        if (this.contentType != null) {
            this.headers.removeAll("Content-Type");
        }
        this.headers.add("Content-Type", contentType);
        this.contentType = contentType;
    }

    public void setIfModifiedSince(Date date) {
        if (this.ifModifiedSince != null) {
            this.headers.removeAll("If-Modified-Since");
        }
        String formattedDate = HttpDate.format(date);
        this.headers.add("If-Modified-Since", formattedDate);
        this.ifModifiedSince = formattedDate;
    }

    public void setIfNoneMatch(String ifNoneMatch) {
        if (this.ifNoneMatch != null) {
            this.headers.removeAll("If-None-Match");
        }
        this.headers.add("If-None-Match", ifNoneMatch);
        this.ifNoneMatch = ifNoneMatch;
    }

    public boolean hasConditions() {
        return (this.ifModifiedSince == null && this.ifNoneMatch == null) ? false : true;
    }

    public void addCookies(Map<String, List<String>> allCookieHeaders) {
        for (Entry<String, List<String>> entry : allCookieHeaders.entrySet()) {
            String key = (String) entry.getKey();
            if ("Cookie".equalsIgnoreCase(key) || "Cookie2".equalsIgnoreCase(key)) {
                this.headers.addAll(key, (List) entry.getValue());
            }
        }
    }
}
