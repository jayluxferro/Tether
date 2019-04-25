package com.koushikdutta.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import junit.framework.Assert;

class ServerSocketChannelWrapper extends ChannelWrapper {
    ServerSocketChannel mChannel;

    ServerSocketChannelWrapper(ServerSocketChannel channel) throws IOException {
        super(channel);
        this.mChannel = channel;
    }

    public int read(ByteBuffer buffer) throws IOException {
        String msg = "Can't read ServerSocketChannel";
        Assert.fail("Can't read ServerSocketChannel");
        throw new IOException("Can't read ServerSocketChannel");
    }

    public boolean isConnected() {
        Assert.fail("ServerSocketChannel is never connected");
        return false;
    }

    public int write(ByteBuffer src) throws IOException {
        String msg = "Can't write ServerSocketChannel";
        Assert.fail("Can't write ServerSocketChannel");
        throw new IOException("Can't write ServerSocketChannel");
    }

    public SelectionKey register(Selector sel) throws ClosedChannelException {
        return this.mChannel.register(sel, 16);
    }

    public int write(ByteBuffer[] src) throws IOException {
        String msg = "Can't write ServerSocketChannel";
        Assert.fail("Can't write ServerSocketChannel");
        throw new IOException("Can't write ServerSocketChannel");
    }
}
