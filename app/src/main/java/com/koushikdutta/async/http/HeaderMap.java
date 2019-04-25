package com.koushikdutta.async.http;

import com.koushikdutta.async.http.libcore.RawHeaders;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HeaderMap {
    public static Map<String, String> parse(RawHeaders headers, String header) {
        HashMap<String, String> map = new HashMap();
        for (String part : headers.get(header).split(";")) {
            String[] pair = part.split("=", 2);
            String key = pair[0].trim();
            String v = null;
            if (pair.length > 1) {
                v = pair[1];
            }
            map.put(key, v);
        }
        return Collections.unmodifiableMap(map);
    }
}
