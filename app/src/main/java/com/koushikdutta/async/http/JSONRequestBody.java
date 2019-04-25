package com.koushikdutta.async.http;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import org.json.JSONObject;

public class JSONRequestBody implements AsyncHttpRequestBody {
    private ByteBufferList data;
    JSONObject json;
    byte[] mBodyBytes;

    /* renamed from: com.koushikdutta.async.http.JSONRequestBody$1 */
    class C02801 implements CompletedCallback {
        C02801() {
        }

        public void onCompleted(Exception ex) {
        }
    }

    public JSONRequestBody() {
        this.data = null;
    }

    public JSONRequestBody(JSONObject json) {
        this();
        this.json = json;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        if (this.data == null) {
            this.data = new ByteBufferList();
        }
        this.data.add(bb);
        bb.clear();
    }

    public void onCompleted(Exception ex) {
    }

    public void write(AsyncHttpRequest request, AsyncHttpResponse sink) {
        Util.writeAll((DataSink) sink, this.mBodyBytes, new C02801());
    }

    public String getContentType() {
        return "application/json";
    }

    public boolean readFullyOnRequest() {
        return true;
    }

    public int length() {
        this.mBodyBytes = this.json.toString().getBytes();
        return this.mBodyBytes.length;
    }
}
