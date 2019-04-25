package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;

public class BufferedDataEmitter implements DataEmitter, DataCallback {
    ByteBufferList mBuffers = new ByteBufferList();
    DataCallback mDataCallback;
    DataEmitter mEmitter;
    private boolean mPaused;

    public BufferedDataEmitter(DataEmitter emitter) {
        this.mEmitter = emitter;
        this.mEmitter.setDataCallback(this);
    }

    public void onDataAvailable() {
        if (this.mDataCallback != null && !this.mPaused && this.mBuffers.remaining() > 0) {
            this.mDataCallback.onDataAvailable(this, this.mBuffers);
        }
    }

    public void setDataCallback(DataCallback callback) {
        this.mDataCallback = callback;
    }

    public DataCallback getDataCallback() {
        return this.mDataCallback;
    }

    public boolean isChunked() {
        return false;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        this.mBuffers.add(bb);
        bb.clear();
        onDataAvailable();
    }

    public void pause() {
        this.mPaused = true;
    }

    public void resume() {
        if (this.mPaused) {
            this.mPaused = false;
            onDataAvailable();
        }
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public void setEndCallback(CompletedCallback callback) {
        this.mEmitter.setEndCallback(callback);
    }

    public CompletedCallback getEndCallback() {
        return this.mEmitter.getEndCallback();
    }
}
