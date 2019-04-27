package com.koushikdutta.async.http;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataCallback;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.filter.ChunkedInputFilter;
import com.koushikdutta.async.http.filter.GZIPInputFilter;
import com.koushikdutta.async.http.filter.InflaterInputFilter;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.server.UnknownRequestBody;
import junit.framework.Assert;

public class Util {
    public static AsyncHttpRequestBody getBody(RawHeaders headers) {
        String contentType = headers.get("Content-Type");
        if (contentType != null) {
            String[] values = contentType.split(";");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            for (String ct : values) {
                if (UrlEncodedFormBody.CONTENT_TYPE.equals(ct)) {
                    return new UrlEncodedFormBody();
                }
                if (MultipartFormDataBody.CONTENT_TYPE.equals(ct)) {
                    return new MultipartFormDataBody(contentType, values);
                }
            }
        }
        return new UnknownRequestBody(contentType);
    }

    public static DataCallback getBodyDecoder(DataCallback callback, RawHeaders headers, boolean server, CompletedCallback reporter) {
        int _contentLength;
        if ("gzip".equals(headers.get("Content-Encoding"))) {
            GZIPInputFilter gunzipper = new GZIPInputFilter();
            gunzipper.setDataCallback(callback);
            gunzipper.setEndCallback(reporter);
            callback = gunzipper;
        } else if ("deflate".equals(headers.get("Content-Encoding"))) {
            InflaterInputFilter inflater = new InflaterInputFilter();
            inflater.setEndCallback(reporter);
            inflater.setDataCallback(callback);
            callback = inflater;
        }
        try {
            _contentLength = Integer.parseInt(headers.get("Content-Length"));
        } catch (Exception e) {
            _contentLength = -1;
        }
        final int contentLength = _contentLength;
        if (-1 != contentLength) {
            if (contentLength < 0) {
                reporter.onCompleted(new Exception("not using chunked encoding, and no content-length found."));
                return callback;
            } else if (contentLength == 0) {
                reporter.onCompleted(null);
                return callback;
            } else {
                FilteredDataCallback contentLengthWatcher = new FilteredDataCallback() {
                    int totalRead = 0;

                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        Assert.assertTrue(this.totalRead < contentLength);
                        ByteBufferList list = bb.get(Math.min(contentLength - this.totalRead, bb.remaining()));
                        this.totalRead += list.remaining();
                        super.onDataAvailable(emitter, list);
                        if (this.totalRead == contentLength) {
                            report(null);
                        }
                    }
                };
                contentLengthWatcher.setDataCallback(callback);
                contentLengthWatcher.setEndCallback(reporter);
                return contentLengthWatcher;
            }
        } else if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
            ChunkedInputFilter chunker = new ChunkedInputFilter();
            chunker.setEndCallback(reporter);
            chunker.setDataCallback(callback);
            return chunker;
        } else if (!server) {
            return callback;
        } else {
            reporter.onCompleted(null);
            return callback;
        }
    }
}
