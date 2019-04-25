package com.koushikdutta.async.http;

import com.koushikdutta.async.http.libcore.RawHeaders;
import java.io.File;
import java.util.Map;

public class Part {
    Map<String, String> mContentDisposition = HeaderMap.parse(this.mHeaders, "Content-Disposition");
    RawHeaders mHeaders;

    public Part(RawHeaders headers) {
        this.mHeaders = headers;
    }

    public RawHeaders getRawHeaders() {
        return this.mHeaders;
    }

    public String getContentType() {
        return this.mHeaders.get("Content-Type");
    }

    public String getFilename() {
        String file = (String) this.mContentDisposition.get("filename");
        if (file == null) {
            return null;
        }
        return new File(file).getName();
    }

    public boolean isFile() {
        return this.mContentDisposition.containsKey("filename");
    }
}
