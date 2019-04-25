package com.koushikdutta.async.callback;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;

public interface DataCallback {
    void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList);
}
