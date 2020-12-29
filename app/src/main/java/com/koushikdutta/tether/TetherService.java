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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    AsyncServer mRelay = new AsyncServer() {
        int mTransmitted = 0;

        /* access modifiers changed from: protected */
        public void onDataReceived(int transmitted) {
            super.onDataReceived(transmitted);
            onDataTransmitted(transmitted);
        }

        /* access modifiers changed from: protected */
        public void onDataSent(int transmitted) {
            super.onDataSent(transmitted);
            onDataReceived(transmitted);
        }

        /* access modifiers changed from: protected */
        public void onDataTransmitted(int transmitted) {
            TetherService.this.checkQuota();
            this.mTransmitted += transmitted;
            if (!Helper.mPurchased && this.mTransmitted > 1000000) {
                this.mTransmitted = 0;
            }
        }
    };
    int mSent = 0;
    boolean mStoppedSelf = false;
    int mTetherVersion = 0;

    public IBinder onBind(Intent intent) {
        return null;
    }


    public void logSent(int sent) {
        this.mConnected = true;
        this.mSent += sent;
    }


    public void logReceived(int received) {
        this.mConnected = true;
        this.mReceived += received;
    }

    /* access modifiers changed from: package-private */
    public void updateStats() {
        if (this.mRelay != null) {
            Intent intent = new Intent("com.koushikdutta.tether.TETHER_STATS");
            intent.putExtra("sent", this.mSent);
            intent.putExtra("received", this.mReceived);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("version", this.mTetherVersion);
            sendBroadcast(intent);
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    TetherService.this.updateStats();
                }
            }, 500);
        }
    }

    public void checkQuota() {
        // do nothing
    }

    private static class SocketInfo {
        BufferedDataSink sink;
        AsyncSocket socket;

        public SocketInfo(AsyncSocket socket2) {
            this.sink = new BufferedDataSink(socket2);
            this.socket = socket2;
        }
    }

    private void createListener() {
        this.mRelay.listen((InetAddress) null, 30002, new ListenCallback() {
            AsyncSocket local;
            BufferedDataSink mLocalSink;
            Hashtable<Integer, SocketInfo> mSockets = new Hashtable<>();

            public void onListening(AsyncServerSocket socket) {
                Log.i(TetherService.LOGTAG, "Listening");
            }

            public void onAccepted(AsyncSocket handler) {
                if (this.local != null) {
                    this.local.close();
                    this.local = null;
                }
                this.local = handler;
                this.mLocalSink = new BufferedDataSink(this.local);
                this.local.setClosedCallback(new CompletedCallback() {
                    public void onCompleted(Exception error) {
                        for (SocketInfo socket : mSockets.values()) {
                            socket.socket.close();
                        }
                        mSockets.clear();
                        TetherService.this.mConnected = false;
                    }
                });
                dataLoop();
            }

            /* access modifiers changed from: package-private */
            public void dataLoop() {
                new PushParser(this.local).setOrder(ByteOrder.BIG_ENDIAN).readInt().readByte().readLenByteArray().tap(new TapCallback() {
                    public void tap(int socket, byte command, byte[] data) {
                        InetAddress remoteAddress;
                        dataLoop();
                        if (command == 2) {
                            try {
                                ByteBuffer bb = ByteBuffer.wrap(data);
                                int protocol = bb.get();
                                byte[] ip = new byte[4];
                                bb.get(ip);
                                int port = bb.getInt();
                                Assert.assertTrue(bb.position() == bb.limit() || bb.position() == bb.limit() + -1);
                                final int i = socket;
                                ConnectCallback connectCallback = new ConnectCallback() {
                                    public void onRemoteClosed() {
                                        mSockets.remove(i);
                                        writeCommand(i, (byte) 0);
                                    }

                                    public void onConnectCompleted(Exception ex, AsyncSocket remote) {
                                        if (ex != null) {
                                            onRemoteClosed();
                                            return;
                                        }
                                        mSockets.put(i, new SocketInfo(remote));
                                        writeCommand(i, (byte) 2);
                                        remote.setClosedCallback(new CompletedCallback() {
                                            public void onCompleted(Exception error) {
                                                onRemoteClosed();
                                            }
                                        });
                                        remote.setEndCallback(new CompletedCallback() {
                                            public void onCompleted(Exception error) {
                                            }
                                        });
                                        remote.setDataCallback(new DataCallback() {
                                            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBuffer) {
                                                try {
                                                    TetherService.this.logReceived(byteBuffer.remaining());
                                                    ByteArrayOutputStream bout = new ByteArrayOutputStream(9);
                                                    DataOutputStream dout = new DataOutputStream(bout);
                                                    dout.writeInt(i);
                                                    dout.writeByte(1);
                                                    dout.writeInt(byteBuffer.remaining());
                                                    byteBuffer.addFirst(ByteBuffer.wrap(bout.toByteArray()));
                                                    mLocalSink.write(byteBuffer);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
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
                                InetSocketAddress sockAddr = new InetSocketAddress(remoteAddress, port);
                                if (protocol == 6) {
                                    TetherService.this.mRelay.connectSocket(sockAddr, connectCallback);
                                } else {
                                    connectCallback.onConnectCompleted((Exception) null, TetherService.this.mRelay.connectDatagram(sockAddr));
                                }
                            } catch (Exception e) {
                                writeCommand(socket, (byte) 0);
                            }
                        } else if (command == 0) {
                            SocketInfo remote = mSockets.get(socket);
                            if (remote != null) {
                                remote.socket.close();
                            }
                        } else if (command == 1) {
                            TetherService.this.logSent(data.length);
                            SocketInfo remote2 = mSockets.get(socket);
                            if (remote2 == null) {
                                System.out.println("why no socket?");
                            } else if (TetherService.this.mTetherVersion > 3 || !remote2.socket.isChunked()) {
                                remote2.sink.write(new ByteBufferList(ByteBuffer.wrap(data)));
                            } else {
                                remote2.sink.write(new ByteBufferList(ByteBuffer.wrap(data, 4, data.length - 4)));
                            }
                        } else if (command == 3) {
                            TetherService.this.mTetherVersion = socket;
                            if (TetherService.this.mTetherVersion > 3) {
                                writeCommand(6, (byte) 3);
                            }
                        } else {
                            Assert.fail();
                        }
                    }
                });
            }

            /* access modifiers changed from: private */
            public void writeCommand(int socket, byte command) {
                try {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream(9);
                    DataOutputStream dout = new DataOutputStream(bout);
                    dout.writeInt(socket);
                    dout.writeByte(command);
                    dout.writeInt(0);
                    dout.flush();
                    byte[] bytes = bout.toByteArray();
                    Assert.assertEquals(bytes.length, 9);
                    this.mLocalSink.write(new ByteBufferList(ByteBuffer.wrap(bytes)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onCompleted(Exception error) {
            }
        });
    }

    public void onCreate() {
        Log.i(LOGTAG, "Tether service has been created.");
        boolean hasIPv4 = false;
        boolean hasIPv6 = false;
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                Enumeration<InetAddress> as = nifs.nextElement().getInetAddresses();
                while (true) {
                    if (!as.hasMoreElements()) {
                        break;
                    }
                    InetAddress a = as.nextElement();
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
                        System.arraycopy(bytes, 0, this.mIPv6Prefix, 0, this.mIPv6Prefix.length);
                    } else {
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        createListener();
        updateStats();
        Notification.Builder n = new Notification.Builder(TetherService.this);
        n.setSmallIcon(R.drawable.ic_stat_notification);
        n.setTicker(getString(R.string.app_name));
        n.setContentTitle(getString(R.string.app_name));
        n.setContentText(getString(R.string.tethering));
        n.setWhen(0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, TetherActivity.class), 0);
        Notification _n = n.getNotification();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(100, _n);
    }

    public void onDestroy() {
        super.onDestroy();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(100);
        this.mRelay.stop();
        this.mRelay = null;
    }
}