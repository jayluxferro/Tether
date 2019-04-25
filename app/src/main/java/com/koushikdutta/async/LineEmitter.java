package com.koushikdutta.async;

import com.koushikdutta.async.callback.DataCallback;
import junit.framework.Assert;

public class LineEmitter {
    StringBuilder data = new StringBuilder();
    StringCallback mLineCallback;

    public interface StringCallback {
        void onStringAvailable(String str);
    }

    /* renamed from: com.koushikdutta.async.LineEmitter$1 */
    class C01121 implements DataCallback {
        C01121() {
        }

        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            while (bb.remaining() > 0) {
                byte b = bb.get();
                if (b == (byte) 10) {
                    Assert.assertNotNull(LineEmitter.this.mLineCallback);
                    LineEmitter.this.mLineCallback.onStringAvailable(LineEmitter.this.data.toString());
                    if (emitter.getDataCallback() == this) {
                        LineEmitter.this.data = new StringBuilder();
                    } else {
                        return;
                    }
                }
                LineEmitter.this.data.append((char) b);
            }
        }
    }

    public LineEmitter(DataEmitter emitter) {
        emitter.setDataCallback(new C01121());
    }

    public void setLineCallback(StringCallback callback) {
        this.mLineCallback = callback;
    }

    public StringCallback getLineCallback() {
        return this.mLineCallback;
    }
}
