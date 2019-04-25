package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDataSink extends FilteredDataSink {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    boolean first = true;
    ZipOutputStream zop = new ZipOutputStream(this.bout);

    public ZipDataSink(DataSink sink) {
        super(sink);
    }

    public void putNextEntry(ZipEntry ze) throws IOException {
        this.zop.putNextEntry(ze);
    }

    public void closeEntry() throws IOException {
        this.zop.closeEntry();
    }

    /* Access modifiers changed, original: protected */
    public void report(Exception e) {
        CompletedCallback closed = getClosedCallback();
        if (closed != null) {
            closed.onCompleted(e);
        }
    }

    public void close() {
        try {
            this.zop.close();
            setMaxBuffer(Integer.MAX_VALUE);
            write(new ByteBufferList());
            super.close();
        } catch (IOException e) {
            report(e);
        }
    }

    public ByteBufferList filter(ByteBufferList bb) {
        if (bb != null) {
            try {
                Iterator it = bb.iterator();
                while (it.hasNext()) {
                    ByteBuffer b = (ByteBuffer) it.next();
                    this.zop.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                }
            } catch (IOException e) {
                report(e);
                return null;
            }
        }
        ByteBufferList ret = new ByteBufferList(this.bout.toByteArray());
        this.bout.reset();
        bb.clear();
        return ret;
    }
}
