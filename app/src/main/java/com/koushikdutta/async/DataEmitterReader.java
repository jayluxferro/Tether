package com.koushikdutta.async;

import com.koushikdutta.async.callback.DataCallback;
import junit.framework.Assert;

public class DataEmitterReader implements DataCallback {
    ByteBufferList mPendingData = new ByteBufferList();
    DataCallback mPendingRead;
    int mPendingReadLength;

    public void read(int count, DataCallback callback) {
        Assert.assertNull(this.mPendingRead);
        this.mPendingReadLength = count;
        this.mPendingRead = callback;
        this.mPendingData = new ByteBufferList();
    }

    private boolean handlePendingData(DataEmitter emitter) {
        if (this.mPendingReadLength > this.mPendingData.remaining()) {
            return false;
        }
        DataCallback pendingRead = this.mPendingRead;
        this.mPendingRead = null;
        pendingRead.onDataAvailable(emitter, this.mPendingData);
        return true;
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        Assert.assertNotNull(this.mPendingRead);
        do {
            this.mPendingData.add(bb.get(Math.min(bb.remaining(), this.mPendingReadLength - this.mPendingData.remaining())));
            if (!handlePendingData(emitter)) {
                return;
            }
        } while (this.mPendingRead != null);
    }
}
