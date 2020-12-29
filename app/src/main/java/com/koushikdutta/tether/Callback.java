package com.koushikdutta.tether;

public interface Callback<T, V> {
    V onCallback(T t);
}