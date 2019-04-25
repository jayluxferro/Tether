package com.koushikdutta.async;

import java.nio.ByteBuffer;
import junit.framework.Assert;

public class FilteredDataSink extends BufferedDataSink {
    public FilteredDataSink(DataSink sink) {
        super(sink);
        setMaxBuffer(0);
    }

    public ByteBufferList filter(ByteBufferList bb) {
        return bb;
    }

    public final void write(ByteBuffer bb) {
        if (!isBuffering() || getMaxBuffer() == Integer.MAX_VALUE) {
            ByteBufferList list = new ByteBufferList();
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            Assert.assertTrue(bb.remaining() == 0);
            list.add(ByteBuffer.wrap(bytes));
            super.write(filter(list), true);
        }
    }

    public final void write(ByteBufferList bb) {
        if (!isBuffering() || getMaxBuffer() == Integer.MAX_VALUE) {
            boolean z;
            ByteBufferList filtered = filter(bb);
            if (bb == null || filtered == bb || bb.remaining() == 0) {
                z = true;
            } else {
                z = false;
            }
            Assert.assertTrue(z);
            super.write(filtered, true);
            if (bb != null) {
                bb.clear();
            }
        }
    }
}
