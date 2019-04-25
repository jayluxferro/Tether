package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import junit.framework.Assert;

public class FilteredDataCallback implements DataEmitter, DataCallback {
    CompletedCallback mCompletedCallback;
    private DataCallback mDataCallback;
    private boolean mPaused;
    private ByteBufferList pending;

    public void setDataCallback(DataCallback callback) {
        this.mDataCallback = callback;
    }

    public DataCallback getDataCallback() {
        return this.mDataCallback;
    }

    public boolean isChunked() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void report(Exception e) {
        if (this.mCompletedCallback != null) {
            this.mCompletedCallback.onCompleted(e);
        }
    }

    public CompletedCallback getEndCallback() {
        return this.mCompletedCallback;
    }

    public void setEndCallback(CompletedCallback callback) {
        this.mCompletedCallback = callback;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        Assert.assertNull(this.pending);
        Assert.assertNotNull(this.mDataCallback);
        Util.emitAllData((DataEmitter) this, bb);
        if (bb.remaining() > 0) {
            this.pending = bb;
        }
    }

    public void pause() {
        this.mPaused = true;
    }

    public void resume() {
        if (this.mPaused) {
            this.mPaused = false;
            if (this.pending != null) {
                Assert.assertNotNull(this.mDataCallback);
                Util.emitAllData((DataEmitter) this, this.pending);
                if (this.pending.remaining() == 0) {
                    this.pending = null;
                }
            }
        }
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public int remaining() {
        if (this.pending == null) {
            return 0;
        }
        return this.pending.remaining();
    }
}
