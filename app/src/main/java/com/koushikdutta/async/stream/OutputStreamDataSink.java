package com.koushikdutta.async.stream;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class OutputStreamDataSink implements DataSink {
    boolean closeReported;
    CompletedCallback mClosedCallback;
    OutputStream mStream;
    WritableCallback mWritable;

    public void setOutputStream(OutputStream stream) {
        this.mStream = stream;
    }

    public OutputStream getOutputStream() {
        return this.mStream;
    }

    public void write(ByteBuffer bb) {
        try {
            this.mStream.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        } catch (IOException e) {
            reportClose(e);
        }
        bb.position(0);
        bb.limit(0);
    }

    public void write(ByteBufferList bb) {
        try {
            Iterator it = bb.iterator();
            while (it.hasNext()) {
                ByteBuffer b = (ByteBuffer) it.next();
                this.mStream.write(b.array(), b.arrayOffset() + b.position(), bb.remaining());
            }
        } catch (IOException e) {
            reportClose(e);
        }
        bb.clear();
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mWritable = handler;
    }

    public WritableCallback getWriteableCallback() {
        return this.mWritable;
    }

    public boolean isOpen() {
        return this.closeReported;
    }

    public void close() {
        try {
            if (this.mStream != null) {
                this.mStream.close();
            }
            reportClose(null);
        } catch (IOException e) {
            reportClose(e);
        }
    }

    public void reportClose(Exception ex) {
        if (!this.closeReported) {
            this.closeReported = true;
            if (this.mClosedCallback != null) {
                this.mClosedCallback.onCompleted(ex);
            }
        }
    }

    public void setClosedCallback(CompletedCallback handler) {
        this.mClosedCallback = handler;
    }

    public CompletedCallback getClosedCallback() {
        return this.mClosedCallback;
    }

    public AsyncServer getServer() {
        return AsyncServer.getDefault();
    }
}
