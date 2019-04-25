package com.koushikdutta.async;

import android.os.Build.VERSION;
import android.util.Log;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.ListenCallback;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class AsyncServer {
    public static final String LOGTAG = "NIO";
    static AsyncServer mInstance = new C01051();
    Thread mAffinity;
    private boolean mAutoStart = false;
    LinkedList<Scheduled> mQueue = new LinkedList();
    private Selector mSelector;

    private static class Scheduled {
        public Runnable runnable;
        public long time;

        public Scheduled(Runnable runnable, long time) {
            this.runnable = runnable;
            this.time = time;
        }
    }

    /* renamed from: com.koushikdutta.async.AsyncServer$1 */
    class C01051 extends AsyncServer {
        C01051() {
            setAutostart(true);
        }
    }

    static {
        try {
            if (VERSION.SDK_INT <= 8) {
                System.setProperty("java.net.preferIPv4Stack", "true");
                System.setProperty("java.net.preferIPv6Addresses", "false");
            }
        } catch (Throwable th) {
        }
    }

    public static AsyncServer getDefault() {
        return mInstance;
    }

    public void setAutostart(boolean autoStart) {
        this.mAutoStart = autoStart;
    }

    public boolean getAutoStart() {
        return this.mAutoStart;
    }

    private void autostart() {
        if (this.mAutoStart) {
            run(false, true);
        }
    }

    private void handleSocket(AsyncSocketImpl handler) throws ClosedChannelException {
        SelectionKey ckey = handler.getChannel().register(this.mSelector);
        ckey.attach(handler);
        handler.setup(this, ckey);
    }

    public void removeAllCallbacks(Object scheduled) {
        synchronized (this) {
            this.mQueue.remove(scheduled);
        }
    }

    public Object postDelayed(Runnable runnable, long delay) {
        Scheduled s;
        synchronized (this) {
            if (delay != 0) {
                delay += System.currentTimeMillis();
            }
            LinkedList linkedList = this.mQueue;
            s = new Scheduled(runnable, delay);
            linkedList.add(s);
            autostart();
            if (!(Thread.currentThread() == this.mAffinity || this.mSelector == null)) {
                this.mSelector.wakeup();
            }
        }
        return s;
    }

    public Object post(Runnable runnable) {
        return postDelayed(runnable, 0);
    }

    public void run(final Runnable runnable) {
        if (Thread.currentThread() == this.mAffinity) {
            post(runnable);
            runQueue(this.mQueue);
            return;
        }
        final Semaphore semaphore = new Semaphore(0);
        post(new Runnable() {
            public void run() {
                runnable.run();
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runQueue(LinkedList<Scheduled> queue) {
        long now = System.currentTimeMillis();
        Collection later = null;
        while (queue.size() > 0) {
            Scheduled s = (Scheduled) queue.remove();
            if (s.time < now) {
                s.runnable.run();
            } else {
                if (later == null) {
                    later = new LinkedList();
                }
                later.add(s);
            }
        }
        if (later != null) {
            queue.addAll(later);
        }
    }

    public void stop() {
        synchronized (this) {
            if (this.mSelector == null) {
                return;
            }
            final Selector currentSelector = this.mSelector;
            post(new Runnable() {
                public void run() {
                    AsyncServer.shutdownEverything(currentSelector);
                }
            });
            this.mQueue = new LinkedList();
            this.mSelector = null;
            this.mAffinity = null;
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDataTransmitted(int transmitted) {
    }

    public void listen(final InetAddress host, final int port, final ListenCallback handler) {
        post(new Runnable() {
            public void run() {
                try {
                    InetSocketAddress isa;
                    final ServerSocketChannel server = ServerSocketChannel.open();
                    ServerSocketChannelWrapper wrapper = new ServerSocketChannelWrapper(server);
                    if (host == null) {
                        isa = new InetSocketAddress(port);
                    } else {
                        isa = new InetSocketAddress(host, port);
                    }
                    server.socket().bind(isa);
                    final SelectionKey key = wrapper.register(AsyncServer.this.mSelector);
                    key.attach(handler);
                    handler.onListening(new AsyncServerSocket() {
                        public void stop() {
                            try {
                                server.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                key.cancel();
                            } catch (Exception e2) {
                            }
                        }
                    });
                } catch (Exception e) {
                    handler.onCompleted(e);
                    e.printStackTrace();
                }
            }
        });
    }

    private void connectSocketInternal(SocketChannel socket, ChannelWrapper sc, SocketAddress remote, ConnectCallback handler, SimpleCancelable cancel) {
        synchronized (cancel) {
            if (cancel.isCanceled()) {
                return;
            }
            SelectionKey ckey = null;
            try {
                ckey = sc.register(this.mSelector);
                ckey.attach(handler);
                socket.connect(remote);
            } catch (Exception e) {
                if (ckey != null) {
                    ckey.cancel();
                }
                handler.onConnectCompleted(e, null);
            }
        }
    }

    private SimpleCancelable prepareConnectSocketCancelable(SocketChannel socket, final ChannelWrapper sc) {
        return new SimpleCancelable() {
            public Cancelable cancel() {
                synchronized (this) {
                    super.cancel();
                    try {
                        sc.close();
                    } catch (IOException e) {
                    }
                }
                return this;
            }
        };
    }

    public Cancelable connectSocket(SocketAddress remote, ConnectCallback handler) {
        try {
            final SocketChannel socket = SocketChannel.open();
            final ChannelWrapper sc = new SocketChannelWrapper(socket);
            final SimpleCancelable cancel = prepareConnectSocketCancelable(socket, sc);
            final SocketAddress socketAddress = remote;
            final ConnectCallback connectCallback = handler;
            post(new Runnable() {
                public void run() {
                    AsyncServer.this.connectSocketInternal(socket, sc, socketAddress, connectCallback, cancel);
                }
            });
            return cancel;
        } catch (Exception e) {
            handler.onConnectCompleted(e, null);
            return SimpleCancelable.COMPLETED;
        }
    }

    public Cancelable connectSocket(String host, int port, ConnectCallback handler) {
        try {
            final SocketChannel socket = SocketChannel.open();
            final ChannelWrapper sc = new SocketChannelWrapper(socket);
            final SimpleCancelable cancel = prepareConnectSocketCancelable(socket, sc);
            final String str = host;
            final int i = port;
            final ConnectCallback connectCallback = handler;
            post(new Runnable() {
                public void run() {
                    try {
                        AsyncServer.this.connectSocketInternal(socket, sc, new InetSocketAddress(str, i), connectCallback, cancel);
                    } catch (Exception e) {
                        cancel.setComplete(true);
                        connectCallback.onConnectCompleted(e, null);
                    }
                }
            });
            return cancel;
        } catch (Exception e) {
            handler.onConnectCompleted(e, null);
            return SimpleCancelable.COMPLETED;
        }
    }

    public AsyncSocket connectDatagram(final SocketAddress remote) throws IOException {
        final DatagramChannel socket = DatagramChannel.open();
        final AsyncSocketImpl handler = new AsyncSocketImpl();
        handler.attach(socket);
        run(new Runnable() {
            public void run() {
                try {
                    AsyncServer.this.handleSocket(handler);
                    socket.connect(remote);
                } catch (Exception e) {
                }
            }
        });
        return handler;
    }

    public void run() {
        run(false, false);
    }

    public void run(final boolean keepRunning, boolean newThread) {
        synchronized (this) {
            if (this.mSelector != null) {
                return;
            }
            try {
                final Selector selector = SelectorProvider.provider().openSelector();
                this.mSelector = selector;
                final LinkedList<Scheduled> queue = this.mQueue;
                if (newThread) {
                    this.mAffinity = new Thread() {
                        public void run() {
                            AsyncServer.run(AsyncServer.this, selector, queue, keepRunning);
                        }
                    };
                    this.mAffinity.start();
                    return;
                }
                this.mAffinity = Thread.currentThread();
                run(this, selector, queue, keepRunning);
            } catch (IOException e) {
            }
        }
    }

    private static void run(AsyncServer server, Selector selector, LinkedList<Scheduled> queue, boolean keepRunning) {
        while (true) {
            try {
                runLoop(server, selector, queue, keepRunning);
            } catch (Exception e) {
                Log.i(LOGTAG, "exception?");
                e.printStackTrace();
            }
            if (!selector.isOpen() || (selector.keys().size() <= 0 && !keepRunning)) {
                shutdownEverything(selector);
            }
        }
        shutdownEverything(selector);
        synchronized (server) {
            if (server.mSelector == selector) {
                server.mQueue = new LinkedList();
                server.mSelector = null;
                server.mAffinity = null;
            }
        }
        Log.i(LOGTAG, "****AsyncServer has shut down.****");
    }

    private static void shutdownEverything(Selector selector) {
        try {
            for (SelectionKey key : selector.keys()) {
                try {
                    key.cancel();
                } catch (Exception e) {
                }
            }
        } catch (Exception e2) {
        }
        try {
            selector.close();
        } catch (Exception e3) {
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            if (r5 == false) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:12:0x001d, code skipped:
            r17.select(100);
     */
    /* JADX WARNING: Missing block: B:13:0x0024, code skipped:
            r8 = r17.selectedKeys();
            r13 = r8.iterator();
     */
    /* JADX WARNING: Missing block: B:15:0x0030, code skipped:
            if (r13.hasNext() != false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:16:0x0032, code skipped:
            r8.clear();
     */
    /* JADX WARNING: Missing block: B:21:0x0039, code skipped:
            r4 = (java.nio.channels.SelectionKey) r13.next();
     */
    /* JADX WARNING: Missing block: B:24:0x0043, code skipped:
            if (r4.isAcceptable() == false) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:25:0x0045, code skipped:
            r10 = ((java.nio.channels.ServerSocketChannel) r4.channel()).accept();
     */
    /* JADX WARNING: Missing block: B:26:0x004f, code skipped:
            if (r10 == null) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:27:0x0051, code skipped:
            r10.configureBlocking(false);
            r1 = r10.register(r17, 1);
            r11 = (com.koushikdutta.async.callback.ListenCallback) r4.attachment();
            r3 = new com.koushikdutta.async.AsyncSocketImpl();
            r3.attach(r10);
            r3.setup(r16, r1);
            r1.attach(r3);
            r11.onAccepted(r3);
     */
    /* JADX WARNING: Missing block: B:29:0x0076, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:30:0x0077, code skipped:
            android.util.Log.i(LOGTAG, "inner loop exception");
            r2.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:33:0x0086, code skipped:
            if (r4.isReadable() == false) goto L_0x0098;
     */
    /* JADX WARNING: Missing block: B:34:0x0088, code skipped:
            r16.onDataTransmitted(((com.koushikdutta.async.AsyncSocketImpl) r4.attachment()).onReadable());
     */
    /* JADX WARNING: Missing block: B:36:0x009c, code skipped:
            if (r4.isWritable() == false) goto L_0x00a8;
     */
    /* JADX WARNING: Missing block: B:37:0x009e, code skipped:
            ((com.koushikdutta.async.AsyncSocketImpl) r4.attachment()).onDataWritable();
     */
    /* JADX WARNING: Missing block: B:39:0x00ac, code skipped:
            if (r4.isConnectable() == false) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:40:0x00ae, code skipped:
            r3 = (com.koushikdutta.async.callback.ConnectCallback) r4.attachment();
            r10 = (java.nio.channels.SocketChannel) r4.channel();
            r4.interestOps(1);
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            r10.finishConnect();
            r6 = new com.koushikdutta.async.AsyncSocketImpl();
            r6.setup(r16, r4);
            r6.attach(r10);
            r4.attach(r6);
            r3.onConnectCompleted(null, r6);
     */
    /* JADX WARNING: Missing block: B:44:0x00d7, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            r4.cancel();
            r10.close();
            r3.onConnectCompleted(r2, null);
     */
    /* JADX WARNING: Missing block: B:47:0x00e4, code skipped:
            android.util.Log.i(LOGTAG, "wtf");
            junit.framework.Assert.fail();
     */
    /* JADX WARNING: Missing block: B:63:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void runLoop(AsyncServer server, Selector selector, LinkedList<Scheduled> queue, boolean keepRunning) throws IOException {
        boolean needsSelect = true;
        synchronized (server) {
            runQueue(queue);
            if (selector.selectNow() != 0) {
                needsSelect = false;
            } else if (selector.keys().size() == 0 && !keepRunning) {
            }
        }
    }

    public Thread getAffinity() {
        return this.mAffinity;
    }

    public boolean isAffinityThread() {
        return this.mAffinity == Thread.currentThread();
    }
}
