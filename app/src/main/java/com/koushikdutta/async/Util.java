package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.filter.ChunkedInputFilter;
import com.koushikdutta.async.http.filter.GZIPInputFilter;
import com.koushikdutta.async.http.filter.InflaterInputFilter;
import com.koushikdutta.async.http.libcore.RawHeaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
        final InputStream is;
        try {
            is = new FileInputStream(file);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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

    public static DataCallback getBodyDecoder(DataCallback callback, RawHeaders headers, boolean server, CompletedCallback reporter) {
        int _contentLength;
        if ("gzip".equals(headers.get("Content-Encoding"))) {
            GZIPInputFilter gunzipper = new GZIPInputFilter();
            gunzipper.setDataCallback(callback);
            gunzipper.setEndCallback(reporter);
            callback = gunzipper;
        } else if ("deflate".equals(headers.get("Content-Encoding"))) {
            InflaterInputFilter inflater = new InflaterInputFilter();
            inflater.setEndCallback(reporter);
            inflater.setDataCallback(callback);
            callback = inflater;
        }
        try {
            _contentLength = Integer.parseInt(headers.get("Content-Length"));
        } catch (Exception e) {
            _contentLength = -1;
        }
        final int contentLength = _contentLength;
        if (-1 != contentLength) {
            if (contentLength < 0) {
                reporter.onCompleted(new Exception("not using chunked encoding, and no content-length found."));
                return callback;
            } else if (contentLength == 0) {
                reporter.onCompleted(null);
                return callback;
            } else {
                FilteredDataCallback contentLengthWatcher = new FilteredDataCallback() {
                    int totalRead = 0;

                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        Assert.assertTrue(this.totalRead < contentLength);
                        ByteBufferList list = bb.get(Math.min(contentLength - this.totalRead, bb.remaining()));
                        this.totalRead += list.remaining();
                        super.onDataAvailable(emitter, list);
                        if (this.totalRead == contentLength) {
                            report(null);
                        }
                    }
                };
                contentLengthWatcher.setDataCallback(callback);
                contentLengthWatcher.setEndCallback(reporter);
                return contentLengthWatcher;
            }
        } else if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
            ChunkedInputFilter chunker = new ChunkedInputFilter();
            chunker.setEndCallback(reporter);
            chunker.setDataCallback(callback);
            return chunker;
        } else if (!server) {
            return callback;
        } else {
            reporter.onCompleted(null);
            return callback;
        }
    }
}
