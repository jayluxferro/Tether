package com.koushikdutta.async;

import com.koushikdutta.async.callback.DataCallback;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import junit.framework.Assert;

public class PushParser {
    static Hashtable<Class, Method> mTable = new Hashtable();
    private ArrayList<Object> mArgs = new ArrayList();
    private TapCallback mCallback;
    DataEmitter mEmitter;
    int mNeeded = 0;
    DataEmitterReader mReader;
    private LinkedList<Object> mWaiting = new LinkedList();

    static class BufferWaiter {
        int length;

        BufferWaiter() {
        }
    }

    static class UntilWaiter {
        DataCallback callback;
        byte value;

        UntilWaiter() {
        }
    }

    /* renamed from: com.koushikdutta.async.PushParser$1 */
    class C01131 implements DataCallback {
        C01131() {
            onDataAvailable(PushParser.this.mEmitter, null);
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            while (PushParser.this.mWaiting.size() > 0) {
                try {
                    Class waiting = PushParser.this.mWaiting.peek();
                    if (waiting != null) {
                        PushParser pushParser;
                        if (waiting == Integer.TYPE) {
                            PushParser.this.mArgs.add(Integer.valueOf(bb.getInt()));
                            pushParser = PushParser.this;
                            pushParser.mNeeded -= 4;
                        } else if (waiting == Short.TYPE) {
                            PushParser.this.mArgs.add(Integer.valueOf(bb.getShort()));
                            pushParser = PushParser.this;
                            pushParser.mNeeded -= 2;
                        } else if (waiting == Byte.TYPE) {
                            PushParser.this.mArgs.add(Byte.valueOf(bb.get()));
                            pushParser = PushParser.this;
                            pushParser.mNeeded--;
                        } else if (waiting == Long.TYPE) {
                            PushParser.this.mArgs.add(Long.valueOf(bb.getLong()));
                            pushParser = PushParser.this;
                            pushParser.mNeeded -= 8;
                        } else if (waiting == Object.class) {
                            PushParser.this.mArgs.add(null);
                        } else if (waiting instanceof UntilWaiter) {
                            UntilWaiter uw = (UntilWaiter) waiting;
                            boolean found = false;
                            ByteBufferList cb = new ByteBufferList();
                            ByteBuffer lastBuffer = null;
                            do {
                                if (lastBuffer != bb.peek()) {
                                    lastBuffer.mark();
                                    if (lastBuffer != null) {
                                        lastBuffer.reset();
                                        cb.add(lastBuffer);
                                    }
                                    lastBuffer = bb.peek();
                                }
                                if (bb.remaining() <= 0) {
                                    break;
                                }
                                if (bb.get() != uw.value) {
                                    found = true;
                                    continue;
                                } else {
                                    found = false;
                                    continue;
                                }
                            } while (found);
                            int mark = lastBuffer.position();
                            lastBuffer.reset();
                            cb.add(ByteBuffer.wrap(lastBuffer.array(), lastBuffer.arrayOffset() + lastBuffer.position(), mark - lastBuffer.position()));
                            lastBuffer.position(mark);
                            if (!found) {
                                if (uw.callback != null) {
                                    uw.callback.onDataAvailable(emitter, cb);
                                }
                                throw new Exception();
                            }
                        } else if ((waiting instanceof BufferWaiter) || (waiting instanceof StringWaiter)) {
                            BufferWaiter bw = (BufferWaiter) waiting;
                            int length = bw.length;
                            if (length == -1) {
                                length = ((Integer) PushParser.this.mArgs.get(PushParser.this.mArgs.size() - 1)).intValue();
                                PushParser.this.mArgs.remove(PushParser.this.mArgs.size() - 1);
                                bw.length = length;
                                pushParser = PushParser.this;
                                pushParser.mNeeded += length;
                            }
                            if (bb.remaining() < length) {
                                throw new Exception();
                            }
                            byte[] bytes = null;
                            if (length > 0) {
                                bytes = new byte[length];
                                bb.get(bytes);
                            }
                            pushParser = PushParser.this;
                            pushParser.mNeeded -= length;
                            if (waiting instanceof StringWaiter) {
                                PushParser.this.mArgs.add(new String(bytes));
                            } else {
                                PushParser.this.mArgs.add(bytes);
                            }
                        } else {
                            Assert.fail();
                        }
                        PushParser.this.mWaiting.remove();
                    }
                } catch (Exception e) {
                    Assert.assertTrue(PushParser.this.mNeeded != 0);
                    PushParser.this.mReader.read(PushParser.this.mNeeded, this);
                    return;
                }
            }
            try {
                Object[] args = PushParser.this.mArgs.toArray();
                PushParser.this.mArgs.clear();
                TapCallback callback = PushParser.this.mCallback;
                PushParser.this.mCallback = null;
                PushParser.getTap(callback).invoke(callback, args);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    static class StringWaiter extends BufferWaiter {
        StringWaiter() {
        }
    }

    public PushParser readInt() {
        this.mNeeded += 4;
        this.mWaiting.add(Integer.TYPE);
        return this;
    }

    public PushParser readByte() {
        this.mNeeded++;
        this.mWaiting.add(Byte.TYPE);
        return this;
    }

    public PushParser readShort() {
        this.mNeeded += 2;
        this.mWaiting.add(Short.TYPE);
        return this;
    }

    public PushParser readLong() {
        this.mNeeded += 8;
        this.mWaiting.add(Long.TYPE);
        return this;
    }

    public PushParser readBuffer(int length) {
        if (length != -1) {
            this.mNeeded += length;
        }
        BufferWaiter bw = new BufferWaiter();
        bw.length = length;
        this.mWaiting.add(bw);
        return this;
    }

    public PushParser readLenBuffer() {
        readInt();
        BufferWaiter bw = new BufferWaiter();
        bw.length = -1;
        this.mWaiting.add(bw);
        return this;
    }

    public PushParser readString() {
        readInt();
        StringWaiter bw = new StringWaiter();
        bw.length = -1;
        this.mWaiting.add(bw);
        return this;
    }

    public PushParser until(byte b, DataCallback callback) {
        UntilWaiter waiter = new UntilWaiter();
        waiter.value = b;
        waiter.callback = callback;
        this.mWaiting.add(Byte.valueOf(b));
        return this;
    }

    public PushParser noop() {
        this.mWaiting.add(Object.class);
        return this;
    }

    public PushParser(DataEmitter s) {
        this.mEmitter = s;
        this.mReader = new DataEmitterReader();
        this.mEmitter.setDataCallback(this.mReader);
    }

    /* Access modifiers changed, original: 0000 */
    public Exception stack() {
        try {
            throw new Exception();
        } catch (Exception e) {
            return e;
        }
    }

    public void tap(TapCallback callback) {
        Assert.assertNull(this.mCallback);
        Assert.assertTrue(this.mWaiting.size() > 0);
        this.mCallback = callback;
        C01131 c01131 = new C01131();
    }

    static Method getTap(TapCallback callback) {
        Method found = (Method) mTable.get(callback.getClass());
        if (found != null) {
            return found;
        }
        for (Method method : callback.getClass().getMethods()) {
            if ("tap".equals(method.getName())) {
                mTable.put(callback.getClass(), method);
                return method;
            }
        }
        Assert.fail("AndroidAsync: tap callback could not be found. Proguard? Use this in your proguard config:\n" + "-keep class * extends com.koushikdutta.async.TapCallback {\n    *;\n}\n");
        return null;
    }
}
