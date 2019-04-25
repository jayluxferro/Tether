package com.koushikdutta.async.callback;

public interface ResultCallback<T> {
    void onCompleted(Exception exception, T t);
}
