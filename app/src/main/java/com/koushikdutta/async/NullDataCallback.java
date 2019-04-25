package com.koushikdutta.async;

import android.util.Log;
import com.koushikdutta.async.callback.DataCallback;

public class NullDataCallback implements DataCallback {
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        Log.i(AsyncServer.LOGTAG, "NullDataCallback invoked?");
        bb.clear();
    }
}
