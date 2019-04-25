package com.koushikdutta.async.http;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.LineEmitter;
import com.koushikdutta.async.LineEmitter.StringCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.server.AsyncHttpRequestBodyBase;
import com.koushikdutta.async.http.server.BoundaryEmitter;

public class MultipartFormDataBody extends AsyncHttpRequestBodyBase {
    public static final String CONTENT_TYPE = "multipart/form-data";
    String boundary;
    BoundaryEmitter boundaryEmitter;
    MultipartCallback mCallback;

    public MultipartFormDataBody(String contentType, String[] values) {
        super(contentType);
        for (String value : values) {
            String[] splits = value.split("=");
            if (splits.length == 2 && "boundary".equals(splits[0])) {
                this.boundary = splits[1];
                this.boundaryEmitter = new BoundaryEmitter(this.boundary) {
                    /* Access modifiers changed, original: protected */
                    public void onBoundaryStart() {
                        final RawHeaders headers = new RawHeaders();
                        new LineEmitter(MultipartFormDataBody.this.boundaryEmitter).setLineCallback(new StringCallback() {

                            /* renamed from: com.koushikdutta.async.http.MultipartFormDataBody$1$1$1 */
                            class C02811 implements DataCallback {
                                int total;

                                C02811() {
                                }

                                public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                                    this.total += bb.remaining();
                                    bb.clear();
                                }
                            }

                            public void onStringAvailable(String s) {
                                if ("\r".equals(s)) {
                                    DataCallback callback = MultipartFormDataBody.this.onPart(new Part(headers));
                                    if (callback == null) {
                                        callback = new C02811();
                                    }
                                    MultipartFormDataBody.this.boundaryEmitter.setDataCallback(callback);
                                    return;
                                }
                                headers.addLine(s);
                            }
                        });
                    }

                    /* Access modifiers changed, original: protected */
                    public void onBoundaryEnd() {
                    }
                };
                return;
            }
        }
        report(new Exception("No boundary found for multipart/form-data"));
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        this.boundaryEmitter.onDataAvailable(emitter, bb);
    }

    public void setMultipartCallback(MultipartCallback callback) {
        this.mCallback = callback;
    }

    public MultipartCallback getMultipartCallback() {
        return this.mCallback;
    }

    private DataCallback onPart(Part part) {
        if (this.mCallback == null) {
            return null;
        }
        return this.mCallback.onPart(part);
    }
}
