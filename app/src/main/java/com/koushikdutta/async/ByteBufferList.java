package com.koushikdutta.async;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import junit.framework.Assert;

public class ByteBufferList implements Iterable<ByteBuffer> {
    LinkedList<ByteBuffer> mBuffers = new LinkedList();

    public ByteBuffer peek() {
        return (ByteBuffer) this.mBuffers.peek();
    }

    public ByteBufferList(ByteBuffer b) {
        add(b);
    }

    public ByteBufferList(byte[] buf) {
        add(ByteBuffer.wrap(buf));
    }

    public ByteBufferList(){

    }

    public ByteBuffer[] toArray() {
        return (ByteBuffer[]) this.mBuffers.toArray(new ByteBuffer[this.mBuffers.size()]);
    }

    public int remaining() {
        int ret = 0;
        Iterator it = this.mBuffers.iterator();
        while (it.hasNext()) {
            ret += ((ByteBuffer) it.next()).remaining();
        }
        return ret;
    }

    public int getInt() {
        return read(4).getInt();
    }

    public char getByteChar() {
        return (char) read(1).get();
    }

    public int getShort() {
        return read(2).getShort();
    }

    public byte get() {
        return read(1).get();
    }

    public long getLong() {
        return read(8).getLong();
    }

    public void get(byte[] bytes) {
        read(bytes.length).get(bytes);
    }

    public ByteBufferList get(int length) {
        Assert.assertTrue(remaining() >= length);
        ByteBufferList ret = new ByteBufferList();
        int offset = 0;
        Iterator it = this.mBuffers.iterator();
        while (it.hasNext()) {
            ByteBuffer b = (ByteBuffer) it.next();
            int remaining = b.remaining();
            if (remaining != 0) {
                if (offset > length) {
                    break;
                }
                if (offset + remaining > length) {
                    int need = length - offset;
                    ret.add(ByteBuffer.wrap(b.array(), b.arrayOffset() + b.position(), need));
                    b.position(b.position() + need);
                } else {
                    ret.add(ByteBuffer.wrap(b.array(), b.arrayOffset() + b.position(), remaining));
                    b.position(b.limit());
                }
                offset += remaining;
            }
        }
        return ret;
    }

    public ByteBuffer read(int count) {
        Assert.assertTrue(count <= remaining());
        ByteBuffer first = (ByteBuffer) this.mBuffers.peek();
        while (first != null && first.position() == first.limit()) {
            this.mBuffers.remove();
            first = (ByteBuffer) this.mBuffers.peek();
        }
        if (first == null) {
            return ByteBuffer.wrap(new byte[0]);
        }
        if (first.remaining() >= count) {
            return first;
        }
        byte[] bytes = new byte[count];
        int offset = 0;
        ByteBuffer bb = null;
        while (offset < count) {
            bb = (ByteBuffer) this.mBuffers.remove();
            int toRead = Math.min(count - offset, bb.remaining());
            bb.get(bytes, offset, toRead);
            offset += toRead;
        }
        Assert.assertNotNull(bb);
        if (bb.position() < bb.limit()) {
            this.mBuffers.add(0, bb);
        }
        ByteBuffer ret = ByteBuffer.wrap(bytes);
        this.mBuffers.add(0, ret);
        return ret;
    }

    public void trim() {
        read(0);
        if (remaining() == 0) {
            this.mBuffers = new LinkedList();
        }
    }

    public void add(ByteBuffer b) {
        if (b.remaining() > 0) {
            this.mBuffers.add(b);
            trim();
        }
    }

    public void add(int location, ByteBuffer b) {
        this.mBuffers.add(location, b);
    }

    public void add(ByteBufferList b) {
        if (b.remaining() > 0) {
            this.mBuffers.addAll(b.mBuffers);
            trim();
        }
    }

    public void clear() {
        this.mBuffers.clear();
    }

    public ByteBuffer remove() {
        return (ByteBuffer) this.mBuffers.remove();
    }

    public int size() {
        return this.mBuffers.size();
    }

    public Iterator<ByteBuffer> iterator() {
        return this.mBuffers.iterator();
    }

    public void spewString() {
        System.out.println(peekString());
    }

    public String peekString() {
        StringBuilder builder = new StringBuilder();
        Iterator it = iterator();
        while (it.hasNext()) {
            ByteBuffer bb = (ByteBuffer) it.next();
            builder.append(new String(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining()));
        }
        return builder.toString();
    }
}
