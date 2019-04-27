package com.koushikdutta.async.http.libcore;

import android.support.v4.view.MotionEventCompat;
import java.nio.ByteOrder;

public final class Memory {
    public static native void memmove(Object obj, int i, Object obj2, int i2, long j);

    public static native byte peekByte(int i);

    public static native void peekByteArray(int i, byte[] bArr, int i2, int i3);

    public static native void peekCharArray(int i, char[] cArr, int i2, int i3, boolean z);

    public static native void peekDoubleArray(int i, double[] dArr, int i2, int i3, boolean z);

    public static native void peekFloatArray(int i, float[] fArr, int i2, int i3, boolean z);

    public static native int peekInt(int i, boolean z);

    public static native void peekIntArray(int i, int[] iArr, int i2, int i3, boolean z);

    public static native long peekLong(int i, boolean z);

    public static native void peekLongArray(int i, long[] jArr, int i2, int i3, boolean z);

    public static native short peekShort(int i, boolean z);

    public static native void peekShortArray(int i, short[] sArr, int i2, int i3, boolean z);

    public static native void pokeByte(int i, byte b);

    public static native void pokeByteArray(int i, byte[] bArr, int i2, int i3);

    public static native void pokeCharArray(int i, char[] cArr, int i2, int i3, boolean z);

    public static native void pokeDoubleArray(int i, double[] dArr, int i2, int i3, boolean z);

    public static native void pokeFloatArray(int i, float[] fArr, int i2, int i3, boolean z);

    public static native void pokeInt(int i, int i2, boolean z);

    public static native void pokeIntArray(int i, int[] iArr, int i2, int i3, boolean z);

    public static native void pokeLong(int i, long j, boolean z);

    public static native void pokeLongArray(int i, long[] jArr, int i2, int i3, boolean z);

    public static native void pokeShort(int i, short s, boolean z);

    public static native void pokeShortArray(int i, short[] sArr, int i2, int i3, boolean z);

    public static native void unsafeBulkGet(Object obj, int i, int i2, byte[] bArr, int i3, int i4, boolean z);

    public static native void unsafeBulkPut(byte[] bArr, int i, int i2, Object obj, int i3, int i4, boolean z);

    private Memory() {
    }

    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        int offset2;
        int i;
        if (order == ByteOrder.BIG_ENDIAN) {
            offset2 = offset + 1;
            offset = offset2 + 1;
            offset2 = offset + 1;
            i = ((((src[offset] & MotionEventCompat.ACTION_MASK) << 24) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 0);
            offset = offset2;
            return i;
        }
        offset2 = offset + 1;
        offset = offset2 + 1;
        offset2 = offset + 1;
        i = ((((src[offset] & MotionEventCompat.ACTION_MASK) << 0) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 24);
        offset = offset2;
        return i;
    }

    public static long peekLong(byte[] src, int offset, ByteOrder order) {
        int offset2;
        int l;
        if (order == ByteOrder.BIG_ENDIAN) {
            offset2 = offset + 1;
            offset = offset2 + 1;
            offset2 = offset + 1;
            offset = offset2 + 1;
            int h = ((((src[offset] & MotionEventCompat.ACTION_MASK) << 24) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 0);
            offset2 = offset + 1;
            offset = offset2 + 1;
            offset2 = offset + 1;
            l = ((((src[offset] & MotionEventCompat.ACTION_MASK) << 24) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 0);
            offset = offset2;
            return (((long) h) << 32) | (((long) l) & 4294967295L);
        }
        offset2 = offset + 1;
        offset = offset2 + 1;
        offset2 = offset + 1;
        offset = offset2 + 1;
        l = ((((src[offset] & MotionEventCompat.ACTION_MASK) << 0) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 24);
        offset2 = offset + 1;
        offset = offset2 + 1;
        offset2 = offset + 1;
        long j = (((long) (((((src[offset] & MotionEventCompat.ACTION_MASK) << 0) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 8)) | ((src[offset] & MotionEventCompat.ACTION_MASK) << 16)) | ((src[offset2] & MotionEventCompat.ACTION_MASK) << 24))) << 32) | (((long) l) & 4294967295L);
        offset = offset2;
        return j;
    }

    public static short peekShort(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (short) ((src[offset] << 8) | (src[offset + 1] & MotionEventCompat.ACTION_MASK));
        }
        return (short) ((src[offset + 1] << 8) | (src[offset] & MotionEventCompat.ACTION_MASK));
    }

    public static void pokeInt(byte[] dst, int offset, int value, ByteOrder order) {
        int i;
        if (order == ByteOrder.BIG_ENDIAN) {
            i = offset + 1;
            dst[offset] = (byte) ((value >> 24) & MotionEventCompat.ACTION_MASK);
            offset = i + 1;
            dst[i] = (byte) ((value >> 16) & MotionEventCompat.ACTION_MASK);
            i = offset + 1;
            dst[offset] = (byte) ((value >> 8) & MotionEventCompat.ACTION_MASK);
            dst[i] = (byte) ((value >> 0) & MotionEventCompat.ACTION_MASK);
            offset = i;
            return;
        }
        i = offset + 1;
        dst[offset] = (byte) ((value >> 0) & MotionEventCompat.ACTION_MASK);
        offset = i + 1;
        dst[i] = (byte) ((value >> 8) & MotionEventCompat.ACTION_MASK);
        i = offset + 1;
        dst[offset] = (byte) ((value >> 16) & MotionEventCompat.ACTION_MASK);
        dst[i] = (byte) ((value >> 24) & MotionEventCompat.ACTION_MASK);
        offset = i;
    }

    public static void pokeLong(byte[] dst, int offset, long value, ByteOrder order) {
        int i;
        int i2;
        if (order == ByteOrder.BIG_ENDIAN) {
            i = (int) (value >> 32);
            i2 = offset + 1;
            dst[offset] = (byte) ((i >> 24) & MotionEventCompat.ACTION_MASK);
            offset = i2 + 1;
            dst[i2] = (byte) ((i >> 16) & MotionEventCompat.ACTION_MASK);
            i2 = offset + 1;
            dst[offset] = (byte) ((i >> 8) & MotionEventCompat.ACTION_MASK);
            offset = i2 + 1;
            dst[i2] = (byte) ((i >> 0) & MotionEventCompat.ACTION_MASK);
            i = (int) value;
            i2 = offset + 1;
            dst[offset] = (byte) ((i >> 24) & MotionEventCompat.ACTION_MASK);
            offset = i2 + 1;
            dst[i2] = (byte) ((i >> 16) & MotionEventCompat.ACTION_MASK);
            i2 = offset + 1;
            dst[offset] = (byte) ((i >> 8) & MotionEventCompat.ACTION_MASK);
            dst[i2] = (byte) ((i >> 0) & MotionEventCompat.ACTION_MASK);
            offset = i2;
            return;
        }
        i = (int) value;
        i2 = offset + 1;
        dst[offset] = (byte) ((i >> 0) & MotionEventCompat.ACTION_MASK);
        offset = i2 + 1;
        dst[i2] = (byte) ((i >> 8) & MotionEventCompat.ACTION_MASK);
        i2 = offset + 1;
        dst[offset] = (byte) ((i >> 16) & MotionEventCompat.ACTION_MASK);
        offset = i2 + 1;
        dst[i2] = (byte) ((i >> 24) & MotionEventCompat.ACTION_MASK);
        i = (int) (value >> 32);
        i2 = offset + 1;
        dst[offset] = (byte) ((i >> 0) & MotionEventCompat.ACTION_MASK);
        offset = i2 + 1;
        dst[i2] = (byte) ((i >> 8) & MotionEventCompat.ACTION_MASK);
        i2 = offset + 1;
        dst[offset] = (byte) ((i >> 16) & MotionEventCompat.ACTION_MASK);
        dst[i2] = (byte) ((i >> 24) & MotionEventCompat.ACTION_MASK);
        offset = i2;
    }

    public static void pokeShort(byte[] dst, int offset, short value, ByteOrder order) {
        int offset2;
        if (order == ByteOrder.BIG_ENDIAN) {
            offset2 = offset + 1;
            dst[offset] = (byte) ((value >> 8) & MotionEventCompat.ACTION_MASK);
            dst[offset2] = (byte) ((value >> 0) & MotionEventCompat.ACTION_MASK);
            offset = offset2;
            return;
        }
        offset2 = offset + 1;
        dst[offset] = (byte) ((value >> 0) & MotionEventCompat.ACTION_MASK);
        dst[offset2] = (byte) ((value >> 8) & MotionEventCompat.ACTION_MASK);
        offset = offset2;
    }
}
