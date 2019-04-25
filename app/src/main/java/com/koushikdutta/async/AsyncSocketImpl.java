package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import junit.framework.Assert;

class AsyncSocketImpl implements AsyncSocket {
    boolean closeReported;
    private ChannelWrapper mChannel;
    CompletedCallback mClosedHander;
    private CompletedCallback mCompletedCallback;
    DataCallback mDataHandler;
    boolean mEndReported;
    private SelectionKey mKey;
    boolean mPaused = false;
    Exception mPendingEndException;
    private AsyncServer mServer;
    int mToAlloc = 0;
    WritableCallback mWriteableHandler;
    private ByteBufferList pending;

    /* renamed from: com.koushikdutta.async.AsyncSocketImpl$3 */
    class C01093 implements Runnable {
        C01093() {
        }

        public void run() {
            AsyncSocketImpl.this.pause();
        }
    }

    /* renamed from: com.koushikdutta.async.AsyncSocketImpl$4 */
    class C01104 implements Runnable {
        C01104() {
        }

        public void run() {
            AsyncSocketImpl.this.resume();
        }
    }

    AsyncSocketImpl() {
    }

    public boolean isChunked() {
        return this.mChannel.isChunked();
    }

    /* Access modifiers changed, original: 0000 */
    public void attach(SocketChannel channel) throws IOException {
        this.mChannel = new SocketChannelWrapper(channel);
    }

    /* Access modifiers changed, original: 0000 */
    public void attach(DatagramChannel channel) throws IOException {
        this.mChannel = new DatagramChannelWrapper(channel);
    }

    /* Access modifiers changed, original: 0000 */
    public ChannelWrapper getChannel() {
        return this.mChannel;
    }

    public void onDataWritable() {
        Assert.assertNotNull(this.mWriteableHandler);
        this.mWriteableHandler.onWriteable();
    }

    /* Access modifiers changed, original: 0000 */
    public void setup(AsyncServer server, SelectionKey key) {
        this.mServer = server;
        this.mKey = key;
    }

    public void write(final ByteBufferList list) {
        if (this.mServer.getAffinity() != Thread.currentThread()) {
            this.mServer.run(new Runnable() {
                public void run() {
                    AsyncSocketImpl.this.write(list);
                }
            });
        } else if (this.mChannel.isConnected()) {
            try {
                this.mChannel.write(list.toArray());
                handleRemaining(list.remaining());
            } catch (IOException e) {
                close();
                reportEndPending(e);
                reportClose(e);
            }
        } else {
            Assert.assertFalse(this.mChannel.isChunked());
        }
    }

    private void handleRemaining(int remaining) {
        if (remaining > 0) {
            Assert.assertFalse(this.mChannel.isChunked());
            this.mKey.interestOps(5);
            return;
        }
        this.mKey.interestOps(1);
    }

    public void write(final ByteBuffer b) {
        if (this.mServer.getAffinity() != Thread.currentThread()) {
            this.mServer.run(new Runnable() {
                public void run() {
                    AsyncSocketImpl.this.write(b);
                }
            });
            return;
        }
        try {
            if (this.mChannel.isConnected()) {
                this.mChannel.write(b);
                handleRemaining(b.remaining());
                return;
            }
            Assert.assertFalse(this.mChannel.isChunked());
        } catch (IOException ex) {
            close();
            reportEndPending(ex);
            reportClose(ex);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public int onReadable() {
        boolean z = false;
        spitPending();
        if (this.mPaused) {
            return 0;
        }
        int total = 0;
        boolean closed = false;
        int maxAlloc = 262144;
        try {
            if (this.mChannel.isChunked()) {
                maxAlloc = 8192;
            }
            ByteBuffer b = ByteBuffer.allocate(Math.min(Math.max(this.mToAlloc, 4096), maxAlloc));
            int read = this.mChannel.read(b);
            if (read < 0) {
                closeInternal();
                closed = true;
            } else {
                total = 0 + read;
            }
            if (read > 0) {
                this.mToAlloc = read * 2;
                b.limit(b.position());
                b.position(0);
                ByteBufferList list = new ByteBufferList(b);
                Util.emitAllData((DataEmitter) this, list);
                if (b.remaining() != 0) {
                    if (this.pending == null) {
                        z = true;
                    }
                    Assert.assertTrue(z);
                    this.pending = list;
                }
            }
            if (!closed) {
                return total;
            }
            reportEndPending(null);
            reportClose(null);
            return total;
        } catch (Exception e) {
            closeInternal();
            reportEndPending(e);
            reportClose(e);
            return 0;
        }
    }

    private void reportClose(Exception e) {
        if (!this.closeReported) {
            this.closeReported = true;
            if (this.mClosedHander != null) {
                this.mClosedHander.onCompleted(e);
                this.mClosedHander = null;
            }
        }
    }

    public void close() {
        closeInternal();
        reportClose(null);
    }

    public void closeInternal() {
        this.mKey.cancel();
        try {
            this.mChannel.close();
        } catch (IOException e) {
        }
    }

    public void setWriteableCallback(WritableCallback handler) {
        this.mWriteableHandler = handler;
    }

    public void setDataCallback(DataCallback callback) {
        this.mDataHandler = callback;
    }

    public DataCallback getDataCallback() {
        return this.mDataHandler;
    }

    public void setClosedCallback(CompletedCallback handler) {
        this.mClosedHander = handler;
    }

    public CompletedCallback getClosedCallback() {
        return this.mClosedHander;
    }

    public WritableCallback getWriteableCallback() {
        return this.mWriteableHandler;
    }

    /* Access modifiers changed, original: 0000 */
    public void reportEnd(Exception e) {
        if (!this.mEndReported) {
            this.mEndReported = true;
            if (this.mCompletedCallback != null) {
                this.mCompletedCallback.onCompleted(e);
            } else if (e != null) {
                e.printStackTrace();
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void reportEndPending(Exception e) {
        if (this.pending != null) {
            this.mPendingEndException = e;
        } else {
            reportEnd(e);
        }
    }

    public void setEndCallback(CompletedCallback callback) {
        this.mCompletedCallback = callback;
    }

    public CompletedCallback getEndCallback() {
        return this.mCompletedCallback;
    }

    public boolean isOpen() {
        return this.mChannel.isConnected() && this.mKey.isValid();
    }

    public void pause() {
        if (this.mServer.getAffinity() != Thread.currentThread()) {
            this.mServer.run(new C01093());
        } else if (!this.mPaused) {
            this.mPaused = true;
            try {
                this.mKey.interestOps(this.mKey.interestOps() & -2);
            } catch (Exception e) {
            }
        }
    }

    private void spitPending() {
        if (this.pending != null) {
            Util.emitAllData((DataEmitter) this, this.pending);
            if (this.pending.remaining() == 0) {
                this.pending = null;
            }
        }
    }

    public void resume() {
        if (this.mServer.getAffinity() != Thread.currentThread()) {
            this.mServer.run(new C01104());
        } else if (this.mPaused) {
            this.mPaused = false;
            try {
                this.mKey.interestOps(this.mKey.interestOps() | 1);
            } catch (Exception e) {
            }
            spitPending();
            if (!isOpen()) {
                reportEndPending(this.mPendingEndException);
            }
        }
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public AsyncServer getServer() {
        return this.mServer;
    }
}
