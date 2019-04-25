package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import java.nio.ByteBuffer;
import junit.framework.Assert;

public class BufferedDataSink implements DataSink {
    boolean closePending;
    DataSink mDataSink;
    int mMaxBuffer = Integer.MAX_VALUE;
    ByteBufferList mPendingWrites;
    WritableCallback mWritable;

    /* renamed from: com.koushikdutta.async.BufferedDataSink$1 */
    class C02741 implements WritableCallback {
        C02741() {
        }

        public void onWriteable() {
            BufferedDataSink.this.writePending();
            if (BufferedDataSink.this.closePending) {
                BufferedDataSink.this.mDataSink.close();
            }
        }
    }

    public BufferedDataSink(DataSink datasink) {
        this.mDataSink = datasink;
        this.mDataSink.setWriteableCallback(new C02741());
    }

    public boolean isBuffering() {
        return this.mPendingWrites != null;
    }

    public DataSink getDataSink() {
        return this.mDataSink;
    }

    private void writePending() {
        if (this.mPendingWrites != null) {
            this.mDataSink.write(this.mPendingWrites);
            if (this.mPendingWrites.remaining() == 0) {
                this.mPendingWrites = null;
            }
        }
        if (this.mPendingWrites == null && this.mWritable != null) {
            this.mWritable.onWriteable();
        }
    }

    public void write(ByteBuffer bb) {
        if (this.mPendingWrites == null) {
            this.mDataSink.write(bb);
        }
        if (bb.remaining() > 0) {
            int toRead = Math.min(bb.remaining(), this.mMaxBuffer);
            if (toRead > 0) {
                if (this.mPendingWrites == null) {
                    this.mPendingWrites = new ByteBufferList();
                }
                byte[] bytes = new byte[toRead];
                bb.get(bytes);
                this.mPendingWrites.add(ByteBuffer.wrap(bytes));
            }
        }
    }

    public void write(ByteBufferList bb) {
        write(bb, false);
    }

    /* Access modifiers changed, original: protected */
    public void write(ByteBufferList bb, boolean ignoreBuffer) {
        if (this.mPendingWrites == null) {
            this.mDataSink.write(bb);
        }
        if (bb.remaining() > 0) {
            int toRead = Math.min(bb.remaining(), this.mMaxBuffer);
            if (ignoreBuffer) {
                toRead = bb.remaining();
            }
            if (toRead > 0) {
                if (this.mPendingWrites == null) {
                    this.mPendingWrites = new ByteBufferList();
                }
                this.mPendingWrites.add(bb.get(toRead));
            }
        }
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mWritable = handler;
    }

    public WritableCallback getWriteableCallback() {
        return this.mWritable;
    }

    public int remaining() {
        if (this.mPendingWrites == null) {
            return 0;
        }
        return this.mPendingWrites.remaining();
    }

    public int getMaxBuffer() {
        return this.mMaxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        Assert.assertTrue(maxBuffer >= 0);
        this.mMaxBuffer = maxBuffer;
    }

    public boolean isOpen() {
        return !this.closePending && this.mDataSink.isOpen();
    }

    public void close() {
        if (this.mPendingWrites != null) {
            this.closePending = true;
        } else {
            this.mDataSink.close();
        }
    }

    public void setClosedCallback(CompletedCallback handler) {
        this.mDataSink.setClosedCallback(handler);
    }

    public CompletedCallback getClosedCallback() {
        return this.mDataSink.getClosedCallback();
    }

    public AsyncServer getServer() {
        return this.mDataSink.getServer();
    }
}
