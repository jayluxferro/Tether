package com.koushikdutta.async.http;

import android.net.Uri;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class UrlEncodedFormBody implements AsyncHttpRequestBody {
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private ByteBufferList data = null;
    private byte[] mBodyBytes;
    private Iterable<NameValuePair> mParameters;

    /* renamed from: com.koushikdutta.async.http.UrlEncodedFormBody$1 */
    class C02831 implements CompletedCallback {
        C02831() {
        }

        public void onCompleted(Exception ex) {
        }
    }

    public UrlEncodedFormBody(Iterable<NameValuePair> parameters) {
        this.mParameters = parameters;
        buildData();
    }

    public UrlEncodedFormBody(){
        buildData();
    }

    private void buildData() {
        boolean first = true;
        StringBuilder b = new StringBuilder();
        for (NameValuePair pair : this.mParameters) {
            if (!first) {
                b.append('&');
            }
            first = false;
            b.append(URLEncoder.encode(pair.getName()));
            b.append('=');
            b.append(URLEncoder.encode(pair.getValue()));
        }
        try {
            this.mBodyBytes = b.toString().getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
        }
    }

    public void write(AsyncHttpRequest request, AsyncHttpResponse response) {
        Util.writeAll((DataSink) response, this.mBodyBytes, new C02831());
    }

    public String getContentType() {
        return CONTENT_TYPE;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        if (this.data == null) {
            this.data = new ByteBufferList();
        }
        this.data.add(bb);
        bb.clear();
    }

    public static Map<String, String> parse(String data) {
        HashMap<String, String> map = new HashMap();
        for (String p : data.split("&")) {
            String[] pair = p.split("=", 2);
            if (pair.length != 0) {
                String name = Uri.decode(pair[0]);
                String value = null;
                if (pair.length == 2) {
                    value = Uri.decode(pair[1]);
                }
                map.put(name, value);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public void onCompleted(Exception ex) {
        ArrayList<NameValuePair> params = new ArrayList();
        this.mParameters = params;
        for (String p : this.data.peekString().split("&")) {
            String[] pair = p.split("=", 2);
            if (pair.length != 0) {
                String name = Uri.decode(pair[0]);
                String value = null;
                if (pair.length == 2) {
                    value = Uri.decode(pair[1]);
                }
                params.add(new BasicNameValuePair(name, value));
            }
        }
    }

    public Iterable<NameValuePair> getParameters() {
        return this.mParameters;
    }

    public Map<String, String> getParameterMap() {
        HashMap<String, String> map = new HashMap();
        for (NameValuePair pair : this.mParameters) {
            if (!map.containsKey(pair.getName())) {
                map.put(pair.getName(), pair.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public boolean readFullyOnRequest() {
        return true;
    }

    public int length() {
        return this.mBodyBytes.length;
    }
}
