package com.koushikdutta.async;

public interface AsyncSocket extends DataEmitter, DataSink {
    AsyncServer getServer();
}
