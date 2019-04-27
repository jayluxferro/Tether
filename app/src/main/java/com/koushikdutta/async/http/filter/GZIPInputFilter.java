package com.koushikdutta.async.http.filter;

import android.support.v4.view.MotionEventCompat;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataEmitterReader;
import com.koushikdutta.async.PushParser;
import com.koushikdutta.async.TapCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.libcore.Memory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import static com.koushikdutta.async.http.libcore.Memory.peekShort;

public class GZIPInputFilter extends InflaterInputFilter {
    private static final int FCOMMENT = 16;
    private static final int FEXTRA = 4;
    private static final int FHCRC = 2;
    private static final int FNAME = 8;
    protected CRC32 crc = new CRC32();
    DataEmitterReader mHeaderParser;
    boolean mNeedsHeader = true;

    public GZIPInputFilter() {
        super(new Inflater(true));
    }

    public static int unsignedToBytes(byte b) {
        return b & MotionEventCompat.ACTION_MASK;
    }

    public void onDataAvailable(final DataEmitter emitter, ByteBufferList bb) {
        if (this.mNeedsHeader) {
            final PushParser parser = new PushParser(emitter);
            parser.readBuffer(10).tap(new TapCallback() {
                int flags;
                boolean hcrc;

                /* renamed from: com.koushikdutta.async.http.filter.GZIPInputFilter$1$2 */
                class C02872 implements DataCallback {
                    C02872() {
                    }

                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        if (hcrc) {
                            while (bb.size() > 0) {
                                ByteBuffer b = bb.remove();
                                GZIPInputFilter.this.crc.update(b.array(), b.arrayOffset() + b.position(), b.remaining());
                            }
                        }
                    }
                }

                public void tap(byte[] header) {
                    boolean z = true;
                    short magic = peekShort(header, 0, ByteOrder.LITTLE_ENDIAN);
                    if (peekShort(header, 0, ByteOrder.LITTLE_ENDIAN) != (short) -29921) {
                        GZIPInputFilter.this.report(new IOException(String.format("unknown format (magic number %x)", new Object[]{Short.valueOf(magic)})));
                        return;
                    }
                    this.flags = header[3];
                    if ((this.flags & 2) == 0) {
                        z = false;
                    }
                    this.hcrc = z;
                    if (this.hcrc) {
                        GZIPInputFilter.this.crc.update(header, 0, header.length);
                    }
                    if ((this.flags & 4) != 0) {
                        PushParser readBuffer = parser.readBuffer(2);
                        final PushParser pushParser = parser;
                        readBuffer.tap(new TapCallback() {

                            /* renamed from: com.koushikdutta.async.http.filter.GZIPInputFilter$1$1$1 */
                            class C02851 extends TapCallback {
                                C02851() {
                                }

                                public void tap(byte[] buf) {
                                    if (hcrc) {
                                        GZIPInputFilter.this.crc.update(buf, 0, buf.length);
                                    }
                                    next();
                                }
                            }

                            public void tap(byte[] header) {
                                if (hcrc) {
                                    GZIPInputFilter.this.crc.update(header, 0, 2);
                                }
                                pushParser.readBuffer(peekShort(header, 0, ByteOrder.LITTLE_ENDIAN) & 65535).tap(new C02851());
                            }
                        });
                    }
                    next();
                }

                private void next() {
                    PushParser parser = new PushParser(emitter);
                    DataCallback summer = new C02872();
                    if ((this.flags & 8) != 0) {
                        parser.until((byte) 0, summer);
                    }
                    if ((this.flags & 16) != 0) {
                        parser.until((byte) 0, summer);
                    }
                    if (this.hcrc) {
                        parser.readBuffer(2);
                    } else {
                        parser.noop();
                    }
                    final DataEmitter dataEmitter = emitter;
                    parser.tap(new TapCallback() {
                        public void tap(byte[] header) {
                            if (header != null) {
                                if (((short) ((int) GZIPInputFilter.this.crc.getValue())) != peekShort(header, 0, ByteOrder.LITTLE_ENDIAN)) {
                                    GZIPInputFilter.this.report(new IOException("CRC mismatch"));
                                    return;
                                }
                                GZIPInputFilter.this.crc.reset();
                            }
                            GZIPInputFilter.this.mNeedsHeader = false;
                            dataEmitter.setDataCallback(GZIPInputFilter.this);
                        }
                    });
                }
            });
            return;
        }
        super.onDataAvailable(emitter, bb);
    }
}
