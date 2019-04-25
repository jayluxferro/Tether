package com.koushikdutta.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;

abstract class ChannelWrapper implements ReadableByteChannel {
    private AbstractSelectableChannel mChannel;

    public abstract boolean isConnected();

    public abstract SelectionKey register(Selector selector) throws ClosedChannelException;

    public abstract int write(ByteBuffer byteBuffer) throws IOException;

    public abstract int write(ByteBuffer[] byteBufferArr) throws IOException;

    ChannelWrapper(AbstractSelectableChannel channel) throws IOException {
        channel.configureBlocking(false);
        this.mChannel = channel;
    }

    public SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return this.mChannel.register(sel, ops);
    }

    public boolean isChunked() {
        return false;
    }

    public boolean isOpen() {
        return this.mChannel.isOpen();
    }

    public void close() throws IOException {
        this.mChannel.close();
    }
}
