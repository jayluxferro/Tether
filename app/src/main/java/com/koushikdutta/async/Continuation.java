package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ContinuationCallback;
import java.util.LinkedList;
import junit.framework.Assert;

public class Continuation implements ContinuationCallback, Runnable, Cancelable {
    CompletedCallback callback;
    boolean cancel;
    Runnable cancelCallback;
    boolean completed;
    private boolean inNext;
    LinkedList<ContinuationCallback> mCallbacks;
    Continuation parent;
    boolean started;
    private boolean waiting;

    /* renamed from: com.koushikdutta.async.Continuation$2 */
    class C02752 implements CompletedCallback {
        boolean mThisCompleted;

        C02752() {
        }

        public void onCompleted(Exception ex) {
            if (!this.mThisCompleted) {
                this.mThisCompleted = true;
                Assert.assertTrue(Continuation.this.waiting);
                Continuation.this.waiting = false;
                if (ex == null) {
                    Continuation.this.next();
                } else {
                    Continuation.this.reportCompleted(ex);
                }
            }
        }
    }

    public CompletedCallback getCallback() {
        return this.callback;
    }

    public void setCallback(CompletedCallback callback) {
        this.callback = callback;
    }

    public Runnable getCancelCallback() {
        return this.cancelCallback;
    }

    public void setCancelCallback(Runnable cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public void setCancelCallback(final Cancelable cancel) {
        if (cancel == null) {
            this.cancelCallback = null;
        } else {
            this.cancelCallback = new Runnable() {
                public void run() {
                    cancel.cancel();
                }
            };
        }
    }

    public Continuation() {
        this(null);
    }

    public Continuation(CompletedCallback callback) {
        this(callback, null);
    }

    public Continuation(CompletedCallback callback, Runnable cancelCallback) {
        this.mCallbacks = new LinkedList();
        this.cancelCallback = cancelCallback;
        this.callback = callback;
    }

    private CompletedCallback wrap() {
        return new C02752();
    }

    /* Access modifiers changed, original: 0000 */
    public void reportCompleted(Exception ex) {
        if (!this.cancel) {
            this.completed = true;
            if (this.callback != null) {
                this.callback.onCompleted(ex);
            }
        }
    }

    public void add(ContinuationCallback callback) {
        this.mCallbacks.add(callback);
    }

    public void insert(ContinuationCallback callback) {
        this.mCallbacks.add(0, callback);
    }

    private void next() {
        if (!this.inNext) {
            if (isCanceled()) {
                reportCompleted(null);
                return;
            }
            while (this.mCallbacks.size() > 0 && !this.waiting && !this.completed && !this.cancel) {
                ContinuationCallback cb = (ContinuationCallback) this.mCallbacks.remove();
                try {
                    this.inNext = true;
                    this.waiting = true;
                    cb.onContinue(this, wrap());
                } catch (Exception e) {
                    reportCompleted(e);
                } finally {
                    this.inNext = false;
                }
            }
            if (!this.waiting && !this.completed && !this.cancel) {
                reportCompleted(null);
            }
        }
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public boolean isCanceled() {
        return this.cancel || (this.parent != null && this.parent.isCanceled());
    }

    public Cancelable cancel() {
        cancelThis();
        if (this.parent != null) {
            this.parent.cancel();
        }
        return this;
    }

    public void cancelThis() {
        if (!isCanceled()) {
            this.cancel = true;
            if (this.cancelCallback != null) {
                this.cancelCallback.run();
            }
        }
    }

    public Continuation start() {
        boolean z;
        if (this.started) {
            z = false;
        } else {
            z = true;
        }
        Assert.assertTrue(z);
        this.started = true;
        next();
        return this;
    }

    public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
        this.parent = continuation;
        setCallback(next);
        setCancelCallback(continuation.getCancelCallback());
        start();
    }

    public void run() {
        start();
    }
}
