package com.koushikdutta.async.http.filter;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.FilteredDataSink;
import java.nio.ByteBuffer;

public class ChunkedOutputFilter extends FilteredDataSink {
    public ChunkedOutputFilter(DataSink sink) {
        super(sink);
    }

    public ByteBufferList filter(ByteBufferList bb) {
        bb.add(0, ByteBuffer.wrap(new StringBuilder(String.valueOf(Integer.toString(bb.remaining(), 16))).append("\r\n").toString().getBytes()));
        bb.add(ByteBuffer.wrap("\r\n".getBytes()));
        return bb;
    }
}
