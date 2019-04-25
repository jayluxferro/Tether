package com.koushikdutta.async;

public interface Cancelable {
    Cancelable cancel();

    boolean isCanceled();

    boolean isCompleted();
}
