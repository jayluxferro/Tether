package com.koushikdutta.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

class SocketChannelWrapper extends ChannelWrapper {
    SocketChannel mChannel;

    SocketChannelWrapper(SocketChannel channel) throws IOException {
        super(channel);
        this.mChannel = channel;
    }

    public int read(ByteBuffer buffer) throws IOException {
        return this.mChannel.read(buffer);
    }

    public boolean isConnected() {
        return this.mChannel.isConnected();
    }

    public int write(ByteBuffer src) throws IOException {
        return this.mChannel.write(src);
    }

    public int write(ByteBuffer[] src) throws IOException {
        return (int) this.mChannel.write(src);
    }

    public SelectionKey register(Selector sel) throws ClosedChannelException {
        return register(sel, 8);
    }
}
