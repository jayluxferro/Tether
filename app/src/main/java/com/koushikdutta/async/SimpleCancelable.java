package com.koushikdutta.async;

import junit.framework.Assert;

public class SimpleCancelable implements Cancelable {
    public static final SimpleCancelable COMPLETED = new SimpleCancelable().setComplete(true);
    boolean canceled;
    boolean complete;

    public boolean isCompleted() {
        return this.complete;
    }

    public SimpleCancelable setComplete(boolean complete) {
        Assert.assertTrue(this != COMPLETED);
        this.complete = complete;
        return this;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public Cancelable cancel() {
        this.canceled = true;
        return this;
    }
}
