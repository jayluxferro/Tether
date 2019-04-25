package com.koushikdutta.async;

import javax.net.ssl.SSLPeerUnverifiedException;

public class AsyncSSLException extends SSLPeerUnverifiedException {
    private boolean mIgnore = false;

    public AsyncSSLException() {
        super("Peer not trusted by any of the system trust managers.");
    }

    public void setIgnore(boolean ignore) {
        this.mIgnore = ignore;
    }

    public boolean getIgnore() {
        return this.mIgnore;
    }
}
