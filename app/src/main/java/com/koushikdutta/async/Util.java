package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import junit.framework.Assert;

public class Util {
    public static void emitAllData(DataEmitter emitter, ByteBufferList list) {
        DataCallback handler = null;
        while (!emitter.isPaused()) {
            handler = emitter.getDataCallback();
            if (handler == null) {
                break;
            }
            int remaining = list.remaining();
            if (remaining > 0) {
                handler.onDataAvailable(emitter, list);
                if (remaining == list.remaining() && handler == emitter.getDataCallback()) {
                    Assert.fail("mDataHandler failed to consume data, yet remains the mDataHandler.");
                    break;
                }
            } else {
                break;
            }
        }
        if (list.remaining() != 0 && !emitter.isPaused()) {
            System.out.println("Data: " + list.peekString());
            System.out.println("handler: " + handler);
            Assert.fail();
        }
    }

    public static void emitAllData(DataEmitter emitter, ByteBuffer b) {
        ByteBufferList list = new ByteBufferList();
        list.add(b);
        emitAllData(emitter, list);
        b.position(b.limit());
    }

    public static void pump(final InputStream is, final DataSink ds, final CompletedCallback callback) {
        WritableCallback cb = new WritableCallback() {
            byte[] buffer = new byte[8192];
            ByteBuffer pending = ByteBuffer.wrap(this.buffer);

            private void close() {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void onWriteable() {
                int remaining;
                do {
                    try {
                        if (this.pending.remaining() == 0) {
                            int read = is.read(this.buffer);
                            if (read == -1) {
                                close();
                                callback.onCompleted(null);
                                return;
                            }
                            this.pending.position(0);
                            this.pending.limit(read);
                        }
                        remaining = this.pending.remaining();
                        ds.write(this.pending);
                    } catch (Exception e) {
                        close();
                        callback.onCompleted(e);
                        return;
                    }
                } while (remaining != this.pending.remaining());
            }
        };
        ds.setWriteableCallback(cb);
        ds.setClosedCallback(callback);
        cb.onWriteable();
    }

    public static void pump(final DataEmitter emitter, final DataSink sink, CompletedCallback callback) {
        emitter.setDataCallback(new DataCallback() {
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                sink.write(bb);
                if (bb.remaining() > 0) {
                    emitter.pause();
                }
            }
        });
        sink.setWriteableCallback(new WritableCallback() {
            public void onWriteable() {
                emitter.resume();
            }
        });
        emitter.setEndCallback(callback);
        sink.setClosedCallback(callback);
    }

    public static void stream(AsyncSocket s1, AsyncSocket s2, CompletedCallback callback) {
        pump((DataEmitter) s1, (DataSink) s2, callback);
        pump((DataEmitter) s2, (DataSink) s1, callback);
    }

    public static void pump(File file, DataSink ds, final CompletedCallback callback) {
        if (file == null || ds == null) {
            try {
                callback.onCompleted(null);
                return;
            } catch (Exception e) {
                callback.onCompleted(e);
                return;
            }
        }
        final InputStream is = new FileInputStream(file);
        pump(is, ds, new CompletedCallback() {
            public void onCompleted(Exception ex) {
                try {
                    is.close();
                    callback.onCompleted(ex);
                } catch (IOException e) {
                    callback.onCompleted(e);
                }
            }
        });
    }

    public static void writeAll(final DataSink sink, final ByteBufferList bb, final CompletedCallback callback) {
        sink.setWriteableCallback(new WritableCallback() {
            public void onWriteable() {
                if (bb.remaining() != 0) {
                    sink.write(bb);
                    if (bb.remaining() == 0 && callback != null) {
                        callback.onCompleted(null);
                    }
                }
            }
        });
        sink.write(bb);
        if (bb.remaining() == 0 && callback != null) {
            callback.onCompleted(null);
        }
    }

    public static void writeAll(DataSink sink, byte[] bytes, CompletedCallback callback) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        ByteBufferList bbl = new ByteBufferList();
        bbl.add(bb);
        writeAll(sink, bbl, callback);
    }
}
