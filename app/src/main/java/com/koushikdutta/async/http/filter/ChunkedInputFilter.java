package com.koushikdutta.async.http.filter;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataCallback;
import com.koushikdutta.async.Util;
import junit.framework.Assert;

public class ChunkedInputFilter extends FilteredDataCallback {
    /* renamed from: $SWITCH_TABLE$com$koushikdutta$async$http$filter$ChunkedInputFilter$State */
    private static /* synthetic */ int[] f374x1f7b8617;
    private int mChunkLength = 0;
    private int mChunkLengthRemaining = 0;
    private State mState = State.CHUNK_LEN;

    private enum State {
        CHUNK_LEN,
        CHUNK_LEN_CR,
        CHUNK_LEN_CRLF,
        CHUNK,
        CHUNK_CR,
        CHUNK_CRLF,
        COMPLETE
    }

    /* renamed from: $SWITCH_TABLE$com$koushikdutta$async$http$filter$ChunkedInputFilter$State */
    static /* synthetic */ int[] m305x1f7b8617() {
        int[] iArr = f374x1f7b8617;
        if (iArr == null) {
            iArr = new int[State.values().length];
            try {
                iArr[State.CHUNK.ordinal()] = 4;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[State.CHUNK_CR.ordinal()] = 5;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[State.CHUNK_CRLF.ordinal()] = 6;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[State.CHUNK_LEN.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[State.CHUNK_LEN_CR.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[State.CHUNK_LEN_CRLF.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[State.COMPLETE.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            f374x1f7b8617 = iArr;
        }
        return iArr;
    }

    private boolean checkByte(char b, char value) {
        if (b == value) {
            return true;
        }
        report(new Exception(new StringBuilder(String.valueOf(value)).append(" was expeceted, got ").append(b).toString()));
        return false;
    }

    private boolean checkLF(char b) {
        return checkByte(b, '\n');
    }

    private boolean checkCR(char b) {
        return checkByte(b, '\r');
    }

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        while (bb.remaining() > 0) {
            try {
                switch (m305x1f7b8617()[this.mState.ordinal()]) {
                    case 1:
                        char c = bb.getByteChar();
                        if (c == 13) {
                            this.mState = State.CHUNK_LEN_CR;
                        } else {
                            this.mChunkLength *= 16;
                            if (c >= 'a' && c <= 'f') {
                                this.mChunkLength += (c - 97) + 10;
                            } else if (c >= '0' && c <= '9') {
                                this.mChunkLength += c - 48;
                            } else if (c < 'A' || c > 'F') {
                                report(new Exception("invalid chunk length: " + c));
                                return;
                            } else {
                                this.mChunkLength += (c - 65) + 10;
                            }
                        }
                        this.mChunkLengthRemaining = this.mChunkLength;
                        break;
                    case 2:
                        if (checkLF(bb.getByteChar())) {
                            this.mState = State.CHUNK;
                            break;
                        }
                        return;
                    case 4:
                        int remaining = bb.remaining();
                        int reading = Math.min(this.mChunkLengthRemaining, remaining);
                        this.mChunkLengthRemaining -= reading;
                        if (this.mChunkLengthRemaining == 0) {
                            this.mState = State.CHUNK_CR;
                        }
                        if (reading == 0) {
                            break;
                        }
                        ByteBufferList chunk = bb.get(reading);
                        int newRemaining = bb.remaining();
                        Assert.assertEquals(remaining, chunk.remaining() + bb.remaining());
                        Assert.assertEquals(reading, chunk.remaining());
                        Util.emitAllData((DataEmitter) this, chunk);
                        Assert.assertEquals(newRemaining, bb.remaining());
                        break;
                    case 5:
                        if (checkCR(bb.getByteChar())) {
                            this.mState = State.CHUNK_CRLF;
                            break;
                        }
                        return;
                    case 6:
                        if (checkLF(bb.getByteChar())) {
                            if (this.mChunkLength > 0) {
                                this.mState = State.CHUNK_LEN;
                            } else {
                                this.mState = State.COMPLETE;
                                report(null);
                            }
                            this.mChunkLength = 0;
                            break;
                        }
                        return;
                    case 7:
                        Exception fail = new Exception("Continued receiving data after chunk complete");
                        report(fail);
                        report(fail);
                        return;
                    default:
                        break;
                }
            } catch (Exception ex) {
                report(ex);
                return;
            }
        }
    }
}
