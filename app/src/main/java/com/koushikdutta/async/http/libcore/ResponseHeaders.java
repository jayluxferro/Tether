package com.koushikdutta.async.http.libcore;

import com.koushikdutta.async.http.libcore.HeaderParser.CacheControlHandler;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public final class ResponseHeaders {
    private static final String RECEIVED_MILLIS = "X-Android-Received-Millis";
    private static final String SENT_MILLIS = "X-Android-Sent-Millis";
    private int ageSeconds = -1;
    private String connection;
    private String contentEncoding;
    private int contentLength = -1;
    private String etag;
    private Date expires;
    private final RawHeaders headers;
    private boolean isPublic;
    private Date lastModified;
    private int maxAgeSeconds = -1;
    private boolean mustRevalidate;
    private boolean noCache;
    private boolean noStore;
    private String proxyAuthenticate;
    private long receivedResponseMillis;
    private int sMaxAgeSeconds = -1;
    private long sentRequestMillis;
    private Date servedDate;
    private String transferEncoding;
    private final URI uri;
    private Set<String> varyFields = Collections.emptySet();
    private String wwwAuthenticate;

    /* renamed from: com.koushikdutta.async.http.libcore.ResponseHeaders$1 */
    class C01481 implements CacheControlHandler {
        C01481() {
        }

        public void handle(String directive, String parameter) {
            if (directive.equalsIgnoreCase("no-cache")) {
                ResponseHeaders.this.noCache = true;
            } else if (directive.equalsIgnoreCase("no-store")) {
                ResponseHeaders.this.noStore = true;
            } else if (directive.equalsIgnoreCase("max-age")) {
                ResponseHeaders.this.maxAgeSeconds = HeaderParser.parseSeconds(parameter);
            } else if (directive.equalsIgnoreCase("s-maxage")) {
                ResponseHeaders.this.sMaxAgeSeconds = HeaderParser.parseSeconds(parameter);
            } else if (directive.equalsIgnoreCase("public")) {
                ResponseHeaders.this.isPublic = true;
            } else if (directive.equalsIgnoreCase("must-revalidate")) {
                ResponseHeaders.this.mustRevalidate = true;
            }
        }
    }

    public ResponseHeaders(URI uri, RawHeaders headers) {
        this.uri = uri;
        this.headers = headers;
        CacheControlHandler handler = new C01481();
        for (int i = 0; i < headers.length(); i++) {
            String fieldName = headers.getFieldName(i);
            String value = headers.getValue(i);
            if ("Cache-Control".equalsIgnoreCase(fieldName)) {
                HeaderParser.parseCacheControl(value, handler);
            } else if ("Date".equalsIgnoreCase(fieldName)) {
                this.servedDate = HttpDate.parse(value);
            } else if ("Expires".equalsIgnoreCase(fieldName)) {
                this.expires = HttpDate.parse(value);
            } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                this.lastModified = HttpDate.parse(value);
            } else if ("ETag".equalsIgnoreCase(fieldName)) {
                this.etag = value;
            } else if ("Pragma".equalsIgnoreCase(fieldName)) {
                if (value.equalsIgnoreCase("no-cache")) {
                    this.noCache = true;
                }
            } else if ("Age".equalsIgnoreCase(fieldName)) {
                this.ageSeconds = HeaderParser.parseSeconds(value);
            } else if ("Vary".equalsIgnoreCase(fieldName)) {
                if (this.varyFields.isEmpty()) {
                    this.varyFields = new TreeSet(String.CASE_INSENSITIVE_ORDER);
                }
                for (String varyField : value.split(",")) {
                    this.varyFields.add(varyField.trim());
                }
            } else if ("Content-Encoding".equalsIgnoreCase(fieldName)) {
                this.contentEncoding = value;
            } else if ("Transfer-Encoding".equalsIgnoreCase(fieldName)) {
                this.transferEncoding = value;
            } else if ("Content-Length".equalsIgnoreCase(fieldName)) {
                try {
                    this.contentLength = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                }
            } else if ("Connection".equalsIgnoreCase(fieldName)) {
                this.connection = value;
            } else if ("Proxy-Authenticate".equalsIgnoreCase(fieldName)) {
                this.proxyAuthenticate = value;
            } else if ("WWW-Authenticate".equalsIgnoreCase(fieldName)) {
                this.wwwAuthenticate = value;
            } else if (SENT_MILLIS.equalsIgnoreCase(fieldName)) {
                this.sentRequestMillis = Long.parseLong(value);
            } else if (RECEIVED_MILLIS.equalsIgnoreCase(fieldName)) {
                this.receivedResponseMillis = Long.parseLong(value);
            }
        }
    }

    public boolean isContentEncodingGzip() {
        return "gzip".equalsIgnoreCase(this.contentEncoding);
    }

    public void stripContentEncoding() {
        this.contentEncoding = null;
        this.headers.removeAll("Content-Encoding");
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

    public Date getServedDate() {
        return this.servedDate;
    }

    public Date getLastModified() {
        return this.lastModified;
    }

    public Date getExpires() {
        return this.expires;
    }

    public boolean isNoCache() {
        return this.noCache;
    }

    public boolean isNoStore() {
        return this.noStore;
    }

    public int getMaxAgeSeconds() {
        return this.maxAgeSeconds;
    }

    public int getSMaxAgeSeconds() {
        return this.sMaxAgeSeconds;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public boolean isMustRevalidate() {
        return this.mustRevalidate;
    }

    public String getEtag() {
        return this.etag;
    }

    public Set<String> getVaryFields() {
        return this.varyFields;
    }

    public String getContentEncoding() {
        return this.contentEncoding;
    }

    public int getContentLength() {
        return this.contentLength;
    }

    public String getConnection() {
        return this.connection;
    }

    public String getProxyAuthenticate() {
        return this.proxyAuthenticate;
    }

    public String getWwwAuthenticate() {
        return this.wwwAuthenticate;
    }

    public void setLocalTimestamps(long sentRequestMillis, long receivedResponseMillis) {
        this.sentRequestMillis = sentRequestMillis;
        this.headers.add(SENT_MILLIS, Long.toString(sentRequestMillis));
        this.receivedResponseMillis = receivedResponseMillis;
        this.headers.add(RECEIVED_MILLIS, Long.toString(receivedResponseMillis));
    }

    private long computeAge(long nowMillis) {
        long receivedAge;
        long apparentReceivedAge = 0;
        if (this.servedDate != null) {
            apparentReceivedAge = Math.max(0, this.receivedResponseMillis - this.servedDate.getTime());
        }
        if (this.ageSeconds != -1) {
            receivedAge = Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis((long) this.ageSeconds));
        } else {
            receivedAge = apparentReceivedAge;
        }
        return (receivedAge + (this.receivedResponseMillis - this.sentRequestMillis)) + (nowMillis - this.receivedResponseMillis);
    }

    private long computeFreshnessLifetime() {
        if (this.maxAgeSeconds != -1) {
            return TimeUnit.SECONDS.toMillis((long) this.maxAgeSeconds);
        }
        long delta;
        if (this.expires != null) {
            delta = this.expires.getTime() - (this.servedDate != null ? this.servedDate.getTime() : this.receivedResponseMillis);
            if (delta <= 0) {
                delta = 0;
            }
            return delta;
        } else if (this.lastModified == null || this.uri.getRawQuery() != null) {
            return 0;
        } else {
            delta = (this.servedDate != null ? this.servedDate.getTime() : this.sentRequestMillis) - this.lastModified.getTime();
            if (delta > 0) {
                return delta / 10;
            }
            return 0;
        }
    }

    private boolean isFreshnessLifetimeHeuristic() {
        return this.maxAgeSeconds == -1 && this.expires == null;
    }

    public boolean isCacheable(RequestHeaders request) {
        int responseCode = this.headers.getResponseCode();
        if (responseCode != 200 && responseCode != 203 && responseCode != 300 && responseCode != 301 && responseCode != 410) {
            return false;
        }
        if ((!request.hasAuthorization() || this.isPublic || this.mustRevalidate || this.sMaxAgeSeconds != -1) && !this.noStore) {
            return true;
        }
        return false;
    }

    public boolean hasVaryAll() {
        return this.varyFields.contains("*");
    }

    public boolean varyMatches(Map<String, List<String>> cachedRequest, Map<String, List<String>> newRequest) {
        for (String field : this.varyFields) {
            if (!Objects.equal(cachedRequest.get(field), newRequest.get(field))) {
                return false;
            }
        }
        return true;
    }

    public ResponseSource chooseResponseSource(long nowMillis, RequestHeaders request) {
        if (!isCacheable(request)) {
            return ResponseSource.NETWORK;
        }
        if (request.isNoCache() || request.hasConditions()) {
            return ResponseSource.NETWORK;
        }
        long ageMillis = computeAge(nowMillis);
        long freshMillis = computeFreshnessLifetime();
        if (request.getMaxAgeSeconds() != -1) {
            freshMillis = Math.min(freshMillis, TimeUnit.SECONDS.toMillis((long) request.getMaxAgeSeconds()));
        }
        long minFreshMillis = 0;
        if (request.getMinFreshSeconds() != -1) {
            minFreshMillis = TimeUnit.SECONDS.toMillis((long) request.getMinFreshSeconds());
        }
        long maxStaleMillis = 0;
        if (!(this.mustRevalidate || request.getMaxStaleSeconds() == -1)) {
            maxStaleMillis = TimeUnit.SECONDS.toMillis((long) request.getMaxStaleSeconds());
        }
        if (this.noCache || ageMillis + minFreshMillis >= freshMillis + maxStaleMillis) {
            if (this.lastModified != null) {
                request.setIfModifiedSince(this.lastModified);
            } else if (this.servedDate != null) {
                request.setIfModifiedSince(this.servedDate);
            }
            if (this.etag != null) {
                request.setIfNoneMatch(this.etag);
            }
            if (request.hasConditions()) {
                return ResponseSource.CONDITIONAL_CACHE;
            }
            return ResponseSource.NETWORK;
        }
        if (ageMillis + minFreshMillis >= freshMillis) {
            this.headers.add("Warning", "110 HttpURLConnection \"Response is stale\"");
        }
        if (ageMillis > 86400000 && isFreshnessLifetimeHeuristic()) {
            this.headers.add("Warning", "113 HttpURLConnection \"Heuristic expiration\"");
        }
        return ResponseSource.CACHE;
    }

    public boolean validate(ResponseHeaders networkResponse) {
        if (networkResponse.headers.getResponseCode() == 304) {
            return true;
        }
        if (this.lastModified == null || networkResponse.lastModified == null || networkResponse.lastModified.getTime() >= this.lastModified.getTime()) {
            return false;
        }
        return true;
    }

    public ResponseHeaders combine(ResponseHeaders network) {
        int i;
        String fieldName;
        RawHeaders result = new RawHeaders();
        for (i = 0; i < this.headers.length(); i++) {
            fieldName = this.headers.getFieldName(i);
            String value = this.headers.getValue(i);
            if (!(fieldName.equals("Warning") && value.startsWith("1")) && (!isEndToEnd(fieldName) || network.headers.get(fieldName) == null)) {
                result.add(fieldName, value);
            }
        }
        for (i = 0; i < network.headers.length(); i++) {
            fieldName = network.headers.getFieldName(i);
            if (isEndToEnd(fieldName)) {
                result.add(fieldName, network.headers.getValue(i));
            }
        }
        return new ResponseHeaders(this.uri, result);
    }

    private static boolean isEndToEnd(String fieldName) {
        return (fieldName.equalsIgnoreCase("Connection") || fieldName.equalsIgnoreCase("Keep-Alive") || fieldName.equalsIgnoreCase("Proxy-Authenticate") || fieldName.equalsIgnoreCase("Proxy-Authorization") || fieldName.equalsIgnoreCase("TE") || fieldName.equalsIgnoreCase("Trailers") || fieldName.equalsIgnoreCase("Transfer-Encoding") || fieldName.equalsIgnoreCase("Upgrade")) ? false : true;
    }
}
