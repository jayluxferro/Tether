package com.koushikdutta.async.http.server;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataCallback;
import java.nio.ByteBuffer;
import junit.framework.Assert;

public class BoundaryEmitter extends FilteredDataCallback {
    private byte[] boundary;
    int state = 0;

    public BoundaryEmitter(String boundary) {
        this.boundary = ("--" + boundary).getBytes();
    }

    /* Access modifiers changed, original: protected */
    public void onBoundaryStart() {
    }

    /* Access modifiers changed, original: protected */
    public void onBoundaryEnd() {
    }

    private static int matches(byte[] a1, int o1, byte[] a2, int o2, int count) {
        boolean z = true;
        Assert.assertTrue(count <= a1.length - o1);
        if (count > a2.length - o2) {
            z = false;
        }
        Assert.assertTrue(z);
        int i = 0;
        while (i < count) {
            if (a1[o1] != a2[o2]) {
                return i;
            }
            i++;
            o1++;
            o2++;
        }
        return count;
    }

    /* Access modifiers changed, original: protected */
    public void report(Exception e) {
        e.printStackTrace();
        super.report(e);
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        ByteBuffer b;
        ByteBufferList list;
        if (this.state > 0) {
            bb.add(0, ByteBuffer.wrap(this.boundary, 0, this.state).duplicate());
            this.state = 0;
        }
        int last = 0;
        byte[] buf = new byte[bb.remaining()];
        bb.get(buf);
        int i = 0;
        while (i < buf.length) {
            if (this.state >= 0) {
                if (buf[i] == this.boundary[this.state]) {
                    this.state++;
                    if (this.state == this.boundary.length) {
                        this.state = -1;
                    }
                } else if (this.state > 0) {
                    i -= this.state;
                    this.state = 0;
                }
            } else if (this.state == -1) {
                if (buf[i] == (byte) 13) {
                    this.state = -4;
                    int len = ((i - last) - this.boundary.length) - 2;
                    if (len >= 0) {
                        b = ByteBuffer.wrap(buf, last, len);
                        list = new ByteBufferList();
                        list.add(b);
                        super.onDataAvailable(emitter, list);
                    } else {
                        Assert.assertEquals(-2, len);
                    }
                    onBoundaryStart();
                } else if (buf[i] == (byte) 45) {
                    this.state = -2;
                } else {
                    report(new Exception("Invalid multipart/form-data. Expected \r or -"));
                    return;
                }
            } else if (this.state == -2) {
                if (buf[i] == (byte) 45) {
                    this.state = -3;
                } else {
                    report(new Exception("Invalid multipart/form-data. Expected -"));
                    return;
                }
            } else if (this.state == -3) {
                if (buf[i] == (byte) 13) {
                    this.state = -4;
                    b = ByteBuffer.wrap(buf, last, ((i - last) - this.boundary.length) - 4);
                    list = new ByteBufferList();
                    list.add(b);
                    super.onDataAvailable(emitter, list);
                    onBoundaryEnd();
                } else {
                    report(new Exception("Invalid multipart/form-data. Expected \r"));
                    return;
                }
            } else if (this.state != -4) {
                Assert.fail();
                report(new Exception("Invalid multipart/form-data. Unknown state?"));
            } else if (buf[i] == (byte) 10) {
                last = i + 1;
                this.state = 0;
            } else {
                report(new Exception("Invalid multipart/form-data. Expected \n"));
            }
            i++;
        }
        if (last < buf.length) {
            b = ByteBuffer.wrap(buf, last, (buf.length - last) - Math.max(this.state, 0));
            list = new ByteBufferList();
            list.add(b);
            super.onDataAvailable(emitter, list);
        }
    }
}
