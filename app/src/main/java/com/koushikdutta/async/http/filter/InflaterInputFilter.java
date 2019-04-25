package com.koushikdutta.async.http.filter;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataCallback;
import com.koushikdutta.async.Util;
import java.nio.ByteBuffer;
import java.util.zip.Inflater;
import junit.framework.Assert;

public class InflaterInputFilter extends FilteredDataCallback {
    private Inflater mInflater;

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        try {
            ByteBufferList transformed = new ByteBufferList();
            ByteBuffer output = ByteBuffer.allocate(bb.remaining() * 2);
            int totalInflated = 0;
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                if (b.hasRemaining()) {
                    int totalRead = b.remaining();
                    this.mInflater.setInput(b.array(), b.arrayOffset() + b.position(), b.remaining());
                    do {
                        int inflated = this.mInflater.inflate(output.array(), output.arrayOffset() + output.position(), output.remaining());
                        totalInflated += inflated;
                        output.position(output.position() + inflated);
                        if (!output.hasRemaining()) {
                            output.limit(output.position());
                            output.position(0);
                            transformed.add(output);
                            Assert.assertNotSame(Integer.valueOf(totalRead), Integer.valueOf(0));
                            output = ByteBuffer.allocate(output.capacity() * 2);
                        }
                        if (this.mInflater.needsInput()) {
                            break;
                        }
                    } while (!this.mInflater.finished());
                }
            }
            output.limit(output.position());
            output.position(0);
            transformed.add(output);
            Util.emitAllData((DataEmitter) this, transformed);
        } catch (Exception ex) {
            report(ex);
        }
    }

    public InflaterInputFilter() {
        this(new Inflater());
    }

    public InflaterInputFilter(Inflater inflater) {
        this.mInflater = inflater;
    }
}
