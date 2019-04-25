package com.koushikdutta.async.http.libcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public final class RawHeaders {
    private static final Comparator<String> FIELD_NAME_COMPARATOR = new C01461();
    private int httpMinorVersion = 1;
    private final List<String> namesAndValues = new ArrayList(20);
    private int responseCode = -1;
    private String responseMessage;
    private String statusLine;

    /* renamed from: com.koushikdutta.async.http.libcore.RawHeaders$1 */
    class C01461 implements Comparator<String> {
        C01461() {
        }

        public int compare(String a, String b) {
            if (a == b) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            if (b == null) {
                return 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        }
    }

    public RawHeaders(RawHeaders copyFrom) {
        this.namesAndValues.addAll(copyFrom.namesAndValues);
        this.statusLine = copyFrom.statusLine;
        this.httpMinorVersion = copyFrom.httpMinorVersion;
        this.responseCode = copyFrom.responseCode;
        this.responseMessage = copyFrom.responseMessage;
    }

    public void setStatusLine(String statusLine) {
        statusLine = statusLine.trim();
        this.statusLine = statusLine;
        if (statusLine != null && statusLine.startsWith("HTTP/")) {
            statusLine = statusLine.trim();
            int mark = statusLine.indexOf(" ") + 1;
            if (mark != 0) {
                if (statusLine.charAt(mark - 2) != '1') {
                    this.httpMinorVersion = 0;
                }
                int last = mark + 3;
                if (last > statusLine.length()) {
                    last = statusLine.length();
                }
                this.responseCode = Integer.parseInt(statusLine.substring(mark, last));
                if (last + 1 <= statusLine.length()) {
                    this.responseMessage = statusLine.substring(last + 1);
                }
            }
        }
    }

    public String getStatusLine() {
        return this.statusLine;
    }

    public int getHttpMinorVersion() {
        return this.httpMinorVersion != -1 ? this.httpMinorVersion : 1;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    public void addLine(String line) {
        int index = line.indexOf(":");
        if (index == -1) {
            add("", line);
        } else {
            add(line.substring(0, index), line.substring(index + 1));
        }
    }

    public void add(String fieldName, String value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName == null");
        } else if (value == null) {
            System.err.println("Ignoring HTTP header field '" + fieldName + "' because its value is null");
        } else {
            this.namesAndValues.add(fieldName);
            this.namesAndValues.add(value.trim());
        }
    }

    public void removeAll(String fieldName) {
        for (int i = 0; i < this.namesAndValues.size(); i += 2) {
            if (fieldName.equalsIgnoreCase((String) this.namesAndValues.get(i))) {
                this.namesAndValues.remove(i);
                this.namesAndValues.remove(i);
            }
        }
    }

    public void addAll(String fieldName, List<String> headerFields) {
        for (String value : headerFields) {
            add(fieldName, value);
        }
    }

    public void set(String fieldName, String value) {
        removeAll(fieldName);
        add(fieldName, value);
    }

    public int length() {
        return this.namesAndValues.size() / 2;
    }

    public String getFieldName(int index) {
        int fieldNameIndex = index * 2;
        if (fieldNameIndex < 0 || fieldNameIndex >= this.namesAndValues.size()) {
            return null;
        }
        return (String) this.namesAndValues.get(fieldNameIndex);
    }

    public String getValue(int index) {
        int valueIndex = (index * 2) + 1;
        if (valueIndex < 0 || valueIndex >= this.namesAndValues.size()) {
            return null;
        }
        return (String) this.namesAndValues.get(valueIndex);
    }

    public String get(String fieldName) {
        for (int i = this.namesAndValues.size() - 2; i >= 0; i -= 2) {
            if (fieldName.equalsIgnoreCase((String) this.namesAndValues.get(i))) {
                return (String) this.namesAndValues.get(i + 1);
            }
        }
        return null;
    }

    public RawHeaders getAll(Set<String> fieldNames) {
        RawHeaders result = new RawHeaders();
        for (int i = 0; i < this.namesAndValues.size(); i += 2) {
            String fieldName = (String) this.namesAndValues.get(i);
            if (fieldNames.contains(fieldName)) {
                result.add(fieldName, (String) this.namesAndValues.get(i + 1));
            }
        }
        return result;
    }

    public String toHeaderString() {
        StringBuilder result = new StringBuilder(256);
        result.append(this.statusLine).append("\r\n");
        for (int i = 0; i < this.namesAndValues.size(); i += 2) {
            result.append((String) this.namesAndValues.get(i)).append(": ").append((String) this.namesAndValues.get(i + 1)).append("\r\n");
        }
        result.append("\r\n");
        return result.toString();
    }

    public Map<String, List<String>> toMultimap() {
        Map<String, List<String>> result = new TreeMap(FIELD_NAME_COMPARATOR);
        for (int i = 0; i < this.namesAndValues.size(); i += 2) {
            String fieldName = (String) this.namesAndValues.get(i);
            String value = (String) this.namesAndValues.get(i + 1);
            List<String> allValues = new ArrayList();
            List<String> otherValues = (List) result.get(fieldName);
            if (otherValues != null) {
                allValues.addAll(otherValues);
            }
            allValues.add(value);
            result.put(fieldName, Collections.unmodifiableList(allValues));
        }
        if (this.statusLine != null) {
            result.put(null, Collections.unmodifiableList(Collections.singletonList(this.statusLine)));
        }
        return Collections.unmodifiableMap(result);
    }

    public static RawHeaders fromMultimap(Map<String, List<String>> map) {
        RawHeaders result = new RawHeaders();
        for (Entry<String, List<String>> entry : map.entrySet()) {
            String fieldName = (String) entry.getKey();
            List<String> values = (List) entry.getValue();
            if (fieldName != null) {
                result.addAll(fieldName, values);
            } else if (!values.isEmpty()) {
                result.setStatusLine((String) values.get(values.size() - 1));
            }
        }
        return result;
    }
}
