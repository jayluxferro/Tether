package com.koushikdutta.async.stream;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class OutputStreamDataCallback implements DataCallback, CompletedCallback {
    private OutputStream mOutput;

    public OutputStreamDataCallback(OutputStream os) {
        this.mOutput = os;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        try {
            Iterator it = bb.iterator();
            while (it.hasNext()) {
                ByteBuffer b = (ByteBuffer) it.next();
                this.mOutput.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
            }
        } catch (Exception ex) {
            onCompleted(ex);
        }
        bb.clear();
    }

    public void close() {
        try {
            this.mOutput.close();
        } catch (IOException e) {
            onCompleted(e);
        }
    }

    public void onCompleted(Exception error) {
        error.printStackTrace();
    }
}
