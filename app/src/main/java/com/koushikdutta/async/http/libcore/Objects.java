package com.koushikdutta.async.http.libcore;

public final class Objects {
    private Objects() {
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }
}
