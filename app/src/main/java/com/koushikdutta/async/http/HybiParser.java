package com.koushikdutta.async.http;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataEmitterReader;
import com.koushikdutta.async.callback.DataCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

abstract class HybiParser {
    private static final int BYTE = 255;
    private static final int FIN = 128;
    private static final List<Integer> FRAGMENTED_OPCODES = Arrays.asList(new Integer[]{Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2)});
    private static final int LENGTH = 127;
    private static final int MASK = 128;
    private static final int MODE_BINARY = 2;
    private static final int MODE_TEXT = 1;
    private static final int OPCODE = 15;
    private static final List<Integer> OPCODES = Arrays.asList(new Integer[]{Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(8), Integer.valueOf(9), Integer.valueOf(10)});
    private static final int OP_BINARY = 2;
    private static final int OP_CLOSE = 8;
    private static final int OP_CONTINUATION = 0;
    private static final int OP_PING = 9;
    private static final int OP_PONG = 10;
    private static final int OP_TEXT = 1;
    private static final int RSV1 = 64;
    private static final int RSV2 = 32;
    private static final int RSV3 = 16;
    private static final String TAG = "HybiParser";
    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();
    private boolean mClosed = false;
    private boolean mFinal;
    private int mLength;
    private int mLengthSize;
    private byte[] mMask = new byte[0];
    private boolean mMasked;
    private boolean mMasking = true;
    private int mMode;
    private int mOpcode;
    private byte[] mPayload = new byte[0];
    private DataEmitterReader mReader = new DataEmitterReader();
    private int mStage;
    DataCallback mStage0 = new C01391();
    DataCallback mStage1 = new C01402();
    DataCallback mStage2 = new C01413();
    DataCallback mStage3 = new C01424();
    DataCallback mStage4 = new C01435();

    public static class ProtocolError extends IOException {
        public ProtocolError(String detailMessage) {
            super(detailMessage);
        }
    }

    /* renamed from: com.koushikdutta.async.http.HybiParser$1 */
    class C01391 implements DataCallback {
        C01391() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            try {
                HybiParser.this.parseOpcode(bb.get());
            } catch (ProtocolError e) {
                HybiParser.this.report(e);
                e.printStackTrace();
            }
            HybiParser.this.parse();
        }
    }

    /* renamed from: com.koushikdutta.async.http.HybiParser$2 */
    class C01402 implements DataCallback {
        C01402() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            HybiParser.this.parseLength(bb.get());
            HybiParser.this.parse();
        }
    }

    /* renamed from: com.koushikdutta.async.http.HybiParser$3 */
    class C01413 implements DataCallback {
        C01413() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            byte[] bytes = new byte[HybiParser.this.mLengthSize];
            bb.get(bytes);
            try {
                HybiParser.this.parseExtendedLength(bytes);
            } catch (ProtocolError e) {
                HybiParser.this.report(e);
                e.printStackTrace();
            }
            HybiParser.this.parse();
        }
    }

    /* renamed from: com.koushikdutta.async.http.HybiParser$4 */
    class C01424 implements DataCallback {
        C01424() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            HybiParser.this.mMask = new byte[4];
            bb.get(HybiParser.this.mMask);
            HybiParser.this.mStage = 4;
            HybiParser.this.parse();
        }
    }

    /* renamed from: com.koushikdutta.async.http.HybiParser$5 */
    class C01435 implements DataCallback {
        C01435() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            HybiParser.this.mPayload = new byte[HybiParser.this.mLength];
            bb.get(HybiParser.this.mPayload);
            try {
                HybiParser.this.emitFrame();
            } catch (IOException e) {
                HybiParser.this.report(e);
                e.printStackTrace();
            }
            HybiParser.this.mStage = 0;
            HybiParser.this.parse();
        }
    }

    public abstract void onDisconnect(int i, String str);

    public abstract void onMessage(String str);

    public abstract void onMessage(byte[] bArr);

    public abstract void report(Exception exception);

    public abstract void sendFrame(byte[] bArr);

    private static byte[] mask(byte[] payload, byte[] mask, int offset) {
        if (mask.length != 0) {
            for (int i = 0; i < payload.length - offset; i++) {
                payload[offset + i] = (byte) (payload[offset + i] ^ mask[i % 4]);
            }
        }
        return payload;
    }

    public void setMasking(boolean masking) {
        this.mMasking = masking;
    }

    /* Access modifiers changed, original: 0000 */
    public void parse() {
        switch (this.mStage) {
            case 0:
                this.mReader.read(1, this.mStage0);
                return;
            case 1:
                this.mReader.read(1, this.mStage1);
                return;
            case 2:
                this.mReader.read(this.mLengthSize, this.mStage2);
                return;
            case 3:
                this.mReader.read(4, this.mStage3);
                return;
            case 4:
                this.mReader.read(this.mLength, this.mStage4);
                return;
            default:
                return;
        }
    }

    public HybiParser(DataEmitter socket) {
        socket.setDataCallback(this.mReader);
        parse();
    }

    private void parseOpcode(byte data) throws ProtocolError {
        boolean rsv1;
        if ((data & 64) == 64) {
            rsv1 = true;
        } else {
            rsv1 = false;
        }
        boolean rsv2;
        if ((data & 32) == 32) {
            rsv2 = true;
        } else {
            rsv2 = false;
        }
        boolean rsv3;
        if ((data & 16) == 16) {
            rsv3 = true;
        } else {
            rsv3 = false;
        }
        if (rsv1 || rsv2 || rsv3) {
            throw new ProtocolError("RSV not zero");
        }
        this.mFinal = (data & 128) == 128;
        this.mOpcode = data & 15;
        this.mMask = new byte[0];
        this.mPayload = new byte[0];
        if (!OPCODES.contains(Integer.valueOf(this.mOpcode))) {
            throw new ProtocolError("Bad opcode");
        } else if (FRAGMENTED_OPCODES.contains(Integer.valueOf(this.mOpcode)) || this.mFinal) {
            this.mStage = 1;
        } else {
            throw new ProtocolError("Expected non-final packet");
        }
    }

    private void parseLength(byte data) {
        this.mMasked = (data & 128) == 128;
        this.mLength = data & LENGTH;
        if (this.mLength < 0 || this.mLength > 125) {
            this.mLengthSize = this.mLength == 126 ? 2 : 8;
            this.mStage = 2;
            return;
        }
        this.mStage = this.mMasked ? 3 : 4;
    }

    private void parseExtendedLength(byte[] buffer) throws ProtocolError {
        this.mLength = getInteger(buffer);
        this.mStage = this.mMasked ? 3 : 4;
    }

    public byte[] frame(String data) {
        return frame(data, 1, -1);
    }

    public byte[] frame(byte[] data) {
        return frame(data, 2, -1);
    }

    private byte[] frame(byte[] data, int opcode, int errorCode) {
        return frame((Object) data, opcode, errorCode);
    }

    private byte[] frame(String data, int opcode, int errorCode) {
        return frame((Object) data, opcode, errorCode);
    }

    private byte[] frame(Object data, int opcode, int errorCode) {
        if (this.mClosed) {
            return null;
        }
        byte[] buffer = data instanceof String ? decode((String) data) : (byte[]) data;
        int insert = errorCode > 0 ? 2 : 0;
        int length = buffer.length + insert;
        int header = length <= 125 ? 2 : length <= 65535 ? 4 : 10;
        int offset = header + (this.mMasking ? 4 : 0);
        int masked = this.mMasking ? 128 : 0;
        byte[] frame = new byte[(length + offset)];
        frame[0] = (byte) (((byte) opcode) | -128);
        if (length <= 125) {
            frame[1] = (byte) (masked | length);
        } else if (length <= 65535) {
            frame[1] = (byte) (masked | 126);
            frame[2] = (byte) ((int) Math.floor((double) (length / 256)));
            frame[3] = (byte) (length & 255);
        } else {
            frame[1] = (byte) (masked | LENGTH);
            frame[2] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 56.0d))) & 255);
            frame[3] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 48.0d))) & 255);
            frame[4] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 40.0d))) & 255);
            frame[5] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 32.0d))) & 255);
            frame[6] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 24.0d))) & 255);
            frame[7] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 16.0d))) & 255);
            frame[8] = (byte) (((int) Math.floor(((double) length) / Math.pow(2.0d, 8.0d))) & 255);
            frame[9] = (byte) (length & 255);
        }
        if (errorCode > 0) {
            frame[offset] = (byte) (((int) Math.floor((double) (errorCode / 256))) & 255);
            frame[offset + 1] = (byte) (errorCode & 255);
        }
        System.arraycopy(buffer, 0, frame, offset + insert, buffer.length);
        if (!this.mMasking) {
            return frame;
        }
        byte[] mask = new byte[]{(byte) ((int) Math.floor(Math.random() * 256.0d)), (byte) ((int) Math.floor(Math.random() * 256.0d)), (byte) ((int) Math.floor(Math.random() * 256.0d)), (byte) ((int) Math.floor(Math.random() * 256.0d))};
        System.arraycopy(mask, 0, frame, header, mask.length);
        mask(frame, mask, offset);
        return frame;
    }

    public void ping(String message) {
    }

    public void close(int code, String reason) {
        if (!this.mClosed) {
            sendFrame(frame(reason, 8, code));
            this.mClosed = true;
        }
    }

    private void emitFrame() throws IOException {
        int code = 0;
        byte[] payload = mask(this.mPayload, this.mMask, 0);
        int opcode = this.mOpcode;
        if (opcode == 0) {
            if (this.mMode == 0) {
                throw new ProtocolError("Mode was not set.");
            }
            this.mBuffer.write(payload);
            if (this.mFinal) {
                byte[] message = this.mBuffer.toByteArray();
                if (this.mMode == 1) {
                    onMessage(encode(message));
                } else {
                    onMessage(message);
                }
                reset();
            }
        } else if (opcode == 1) {
            if (this.mFinal) {
                onMessage(encode(payload));
                return;
            }
            this.mMode = 1;
            this.mBuffer.write(payload);
        } else if (opcode == 2) {
            if (this.mFinal) {
                onMessage(payload);
                return;
            }
            this.mMode = 2;
            this.mBuffer.write(payload);
        } else if (opcode == 8) {
            if (payload.length >= 2) {
                code = (payload[0] * 256) + payload[1];
            }
            onDisconnect(code, payload.length > 2 ? encode(slice(payload, 2)) : null);
        } else if (opcode == 9) {
            if (payload.length > 125) {
                throw new ProtocolError("Ping payload too large");
            }
            sendFrame(frame(payload, 10, -1));
        } else if (opcode == 10) {
            encode(payload);
        }
    }

    private void reset() {
        this.mMode = 0;
        this.mBuffer.reset();
    }

    private String encode(byte[] buffer) {
        try {
            return new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] decode(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private int getInteger(byte[] bytes) throws ProtocolError {
        long i = byteArrayToLong(bytes, 0, bytes.length);
        if (i >= 0 && i <= 2147483647L) {
            return (int) i;
        }
        throw new ProtocolError("Bad integer: " + i);
    }

    private byte[] slice(byte[] array, int start) {
        byte[] copy = new byte[(array.length - start)];
        System.arraycopy(array, start, copy, 0, array.length - start);
        return copy;
    }

    private static long byteArrayToLong(byte[] b, int offset, int length) {
        if (b.length < length) {
            throw new IllegalArgumentException("length must be less than or equal to b.length");
        }
        long value = 0;
        for (int i = 0; i < length; i++) {
            value += (long) ((b[i + offset] & 255) << (((length - 1) - i) * 8));
        }
        return value;
    }
}
