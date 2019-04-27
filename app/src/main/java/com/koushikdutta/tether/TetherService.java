package com.koushikdutta.tether;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.BufferedDataSink;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.PushParser;
import com.koushikdutta.async.TapCallback;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.ListenCallback;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import junit.framework.Assert;

public class TetherService extends Service {
    private static final String LOGTAG = "Tether";
    public static final int TETHER_VERSION = 6;
    boolean mConnected = false;
    Handler mHandler = new Handler();
    byte[] mIPv6Prefix;
    int mReceived = 0;
    AsyncServer mRelay = new C02021();
    int mSent = 0;
    boolean mStoppedSelf = false;
    int mTetherVersion = 0;

    /* renamed from: com.koushikdutta.tether.TetherService$2 */
    class C02002 implements Runnable {
        C02002() {
        }

        public void run() {
            TetherService.this.updateStats();
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherService$4 */
    class C02014 extends Thread {
        C02014() {
        }

        public void run() {
            setPriority(10);
            try {
                TetherService.this.createListener();
                TetherService.this.mRelay.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class SocketInfo {
        BufferedDataSink sink;
        AsyncSocket socket;

        public SocketInfo(AsyncSocket socket) {
            this.sink = new BufferedDataSink(socket);
            this.socket = socket;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherService$1 */
    class C02021 extends AsyncServer {
        int mTransmitted = 0;

        C02021() {
        }

        /* Access modifiers changed, original: protected */
        public void onDataTransmitted(int transmitted) {
            TetherService.this.checkQuota();
            this.mTransmitted += transmitted;
        }
    }

    /* renamed from: com.koushikdutta.tether.TetherService$3 */
    class C02033 implements ListenCallback {
        AsyncSocket local;
        BufferedDataSink mLocalSink;
        Hashtable<Integer, SocketInfo> mSockets = new Hashtable();

        /* renamed from: com.koushikdutta.tether.TetherService$3$1 */
        class C02041 implements CompletedCallback {
            C02041() {
            }

            public void onCompleted(Exception error) {
                for (SocketInfo socket : C02033.this.mSockets.values()) {
                    socket.socket.close();
                }
                C02033.this.mSockets.clear();
                TetherService.this.mConnected = false;
            }
        }

        /* renamed from: com.koushikdutta.tether.TetherService$3$2 */
        class C02052 extends TapCallback {
            C02052() {
            }

            public void tap(int socket, byte command, byte[] data) {
                C02033.this.dataLoop();
                SocketInfo remote;
                if (command == (byte) 2) {
                    try {
                        InetAddress remoteAddress;
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        int protocol = bb.get();
                        byte[] ip = new byte[4];
                        bb.get(ip);
                        int port = bb.getInt();
                        boolean z = bb.position() == bb.limit() || bb.position() == bb.limit() - 1;
                        Assert.assertTrue(z);
                        final int i = socket;
                        ConnectCallback connectCallback = new ConnectCallback() {

                            /* renamed from: com.koushikdutta.tether.TetherService$3$2$1$1 */
                            class C02071 implements CompletedCallback {
                                C02071() {
                                }

                                public void onCompleted(Exception error) {
                                    onRemoteClosed();
                                }
                            }

                            /* renamed from: com.koushikdutta.tether.TetherService$3$2$1$2 */
                            class C02082 implements CompletedCallback {
                                C02082() {
                                }

                                public void onCompleted(Exception error) {
                                }
                            }

                            public void onRemoteClosed() {
                                C02033.this.mSockets.remove(Integer.valueOf(i));
                                C02033.this.writeCommand(i, (byte) 0);
                            }

                            public void onConnectCompleted(Exception ex, AsyncSocket remote) {
                                if (ex != null) {
                                    onRemoteClosed();
                                    return;
                                }
                                C02033.this.mSockets.put(Integer.valueOf(i), new SocketInfo(remote));
                                C02033.this.writeCommand(i, (byte) 2);
                                remote.setClosedCallback(new C02071());
                                remote.setEndCallback(new C02082());

                                remote.setDataCallback(new DataCallback() {
                                    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBuffer) {
                                        try {
                                            TetherService.this.logReceived(byteBuffer.remaining());
                                            ByteArrayOutputStream bout = new ByteArrayOutputStream(9);
                                            DataOutputStream dout = new DataOutputStream(bout);
                                            dout.writeInt(i);
                                            dout.writeByte(1);
                                            dout.writeInt(byteBuffer.remaining());
                                            byteBuffer.add(0, ByteBuffer.wrap(bout.toByteArray()));
                                            C02033.this.mLocalSink.write(byteBuffer);
                                        } catch (Exception e) {
                                        }
                                    }
                                });
                            }
                        };
                        if (TetherService.this.mIPv6Prefix != null) {
                            byte[] bytes = new byte[16];
                            for (int i2 = 0; i2 < TetherService.this.mIPv6Prefix.length; i2++) {
                                bytes[i2] = TetherService.this.mIPv6Prefix[i2];
                            }
                            bytes[12] = ip[0];
                            bytes[13] = ip[1];
                            bytes[14] = ip[2];
                            bytes[15] = ip[3];
                            remoteAddress = InetAddress.getByAddress(bytes);
                        } else {
                            remoteAddress = InetAddress.getByAddress(ip);
                        }
                        SocketAddress sockAddr = new InetSocketAddress(remoteAddress, port);
                        if (protocol == 6) {
                            TetherService.this.mRelay.connectSocket(sockAddr, connectCallback);
                        } else {
                            connectCallback.onConnectCompleted(null, TetherService.this.mRelay.connectDatagram(sockAddr));
                        }
                    } catch (Exception e) {
                        C02033.this.writeCommand(socket, (byte) 0);
                    }
                } else if (command == (byte) 0) {
                    remote = (SocketInfo) C02033.this.mSockets.get(Integer.valueOf(socket));
                    if (remote != null) {
                        remote.socket.close();
                    }
                } else if (command == (byte) 1) {
                    TetherService.this.logSent(data.length);
                    remote = (SocketInfo) C02033.this.mSockets.get(Integer.valueOf(socket));
                    if (remote == null) {
                        System.out.println("why no socket?");
                    } else if (TetherService.this.mTetherVersion > 3 || !remote.socket.isChunked()) {
                        remote.sink.write(ByteBuffer.wrap(data));
                    } else {
                        remote.sink.write(ByteBuffer.wrap(data, 4, data.length - 4));
                    }
                } else if (command == (byte) 3) {
                    TetherService.this.mTetherVersion = socket;
                    if (TetherService.this.mTetherVersion > 3) {
                        C02033.this.writeCommand(6, (byte) 3);
                    }
                } else {
                    Assert.fail();
                }
            }
        }

        C02033() {
        }

        public void onListening(AsyncServerSocket socket) {
        }

        public void onAccepted(AsyncSocket handler) {
            if (this.local != null) {
                this.local.close();
                this.local = null;
            }
            this.local = handler;
            this.mLocalSink = new BufferedDataSink(this.local);
            this.local.setClosedCallback(new C02041());
            dataLoop();
        }

        /* Access modifiers changed, original: 0000 */
        public void dataLoop() {
            new PushParser(this.local).readInt().readByte().readLenBuffer().tap(new C02052());
        }

        private void writeCommand(int socket, byte command) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream(9);
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeInt(socket);
                dout.writeByte(command);
                dout.writeInt(0);
                dout.flush();
                byte[] bytes = bout.toByteArray();
                Assert.assertEquals(bytes.length, 9);
                this.mLocalSink.write(ByteBuffer.wrap(bytes));
            } catch (Exception e) {
            }
        }

        public void onCompleted(Exception error) {
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    /* Access modifiers changed, original: 0000 */
    public void logSent(int sent) {
        this.mConnected = true;
        this.mSent += sent;
    }

    /* Access modifiers changed, original: 0000 */
    public void logReceived(int received) {
        this.mConnected = true;
        this.mReceived += received;
    }

    /* Access modifiers changed, original: 0000 */
    public void updateStats() {
        if (this.mRelay != null) {
            Intent intent = new Intent("com.koushikdutta.tether.TETHER_STATS");
            intent.putExtra("sent", this.mSent);
            intent.putExtra("received", this.mReceived);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("version", this.mTetherVersion);
            sendBroadcast(intent);
            this.mHandler.postDelayed(new C02002(), 500);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void checkQuota() {

    }

    private void createListener() throws UnknownHostException, IOException {
        this.mRelay.listen(InetAddress.getLocalHost(), 30002, new C02033());
    }

    public void onCreate() {
        Log.i(LOGTAG, "Tether service has been created.");
        boolean hasIPv4 = false;
        boolean hasIPv6 = false;
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                Enumeration<InetAddress> as = ((NetworkInterface) nifs.nextElement()).getInetAddresses();
                while (as.hasMoreElements()) {
                    InetAddress a = (InetAddress) as.nextElement();
                    if (!a.isLoopbackAddress()) {
                        if (a.getAddress().length == 4) {
                            hasIPv4 = true;
                            break;
                        } else if (a.getAddress().length == 16) {
                            hasIPv6 = true;
                            break;
                        }
                    }
                }
            }
            if (!hasIPv4 && hasIPv6) {
                InetAddress[] addrs = InetAddress.getAllByName("yahoo.com");
                int length = addrs.length;
                int i = 0;
                while (i < length) {
                    byte[] bytes = addrs[i].getAddress();
                    if (bytes.length == 16) {
                        this.mIPv6Prefix = new byte[12];
                        for (int i2 = 0; i2 < this.mIPv6Prefix.length; i2++) {
                            this.mIPv6Prefix[i2] = bytes[i2];
                        }
                    } else {
                        i++;
                    }
                }
            }
        } catch (Exception e) {
        }
        new C02014().start();
        updateStats();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification();
        n.icon = R.drawable.ic_stat_notification;
        n.tickerText = getString(R.string.app_name);
        n.when = 0;
        n.flags = 34;

        n.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, TetherActivity.class), 0);

        nm.notify(100, n);
    }

    public void onDestroy() {
        super.onDestroy();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(100);
        this.mRelay.stop();
        this.mRelay = null;
    }
}
