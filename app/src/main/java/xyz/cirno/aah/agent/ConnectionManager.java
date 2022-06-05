package xyz.cirno.aah.agent;

import static xyz.cirno.aah.agent.Util.readFully;
import static xyz.cirno.aah.agent.Util.writeFully;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionManager {

    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final HashSet<Closeable> pendingSockets = new HashSet<>();


    private static String describeLocalSocketAddress(LocalSocketAddress address) {
        return String.format("AF_UNIX abstract: %s", address.getName());
    }

    private interface AbstractSocket extends Closeable {
        InputStream getInputStream() throws IOException;
        OutputStream getOutputStream() throws IOException;
        void connect(Object endpoint) throws IOException;
        String describeEndpoint(Object endpoint);
    }

    private interface AbstractServerSocket extends Closeable {
        AbstractSocket accept() throws IOException;
        String describeLocalEndpoint();
    }

    private static class SocketWrapper implements AbstractSocket {
        private final Socket _socket;

        public SocketWrapper(Socket socket) {
            _socket = socket;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return _socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return _socket.getOutputStream();
        }

        @Override
        public void connect(Object endpoint) throws IOException {
            _socket.setTcpNoDelay(true);
            _socket.connect((SocketAddress) endpoint);
        }

        @Override
        public String describeEndpoint(Object endpoint) {
            return endpoint.toString();
        }

        @Override
        public void close() throws IOException {
            _socket.close();
        }
    }

    private static class ServerSocketWrapper implements AbstractServerSocket {
        private final ServerSocket _serverSocket;

        public ServerSocketWrapper(ServerSocket serverSocket) {
            _serverSocket = serverSocket;
        }

        @Override
        public AbstractSocket accept() throws IOException {
            Socket socket = _serverSocket.accept();
            socket.setTcpNoDelay(true);
            return new SocketWrapper(socket);
        }

        @Override
        public void close() throws IOException {
            _serverSocket.close();
        }

        @Override
        public String describeLocalEndpoint() {
            return _serverSocket.getLocalSocketAddress().toString();
        }
    }

    private static class LocalSocketWrapper implements AbstractSocket {
        private final LocalSocket _localSocket;

        public LocalSocketWrapper(LocalSocket localSocket) {
            _localSocket = localSocket;
        }

        @Override
        public void connect(Object endpoint) throws IOException {
            _localSocket.connect((LocalSocketAddress) endpoint);
        }

        @Override
        public String describeEndpoint(Object endpoint) {
            LocalSocketAddress lsa = (LocalSocketAddress) endpoint;
            return describeLocalSocketAddress(lsa);
        }

        @Override
        public void close() throws IOException {
            _localSocket.close();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return _localSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return _localSocket.getOutputStream();
        }
    }

    private static class LocalServerSocketWrapper implements AbstractServerSocket {
        private final LocalServerSocket _localServerSocket;

        public LocalServerSocketWrapper(LocalServerSocket localServerSocket) {
            _localServerSocket = localServerSocket;
        }

        @Override
        public AbstractSocket accept() throws IOException {
            return new LocalSocketWrapper(_localServerSocket.accept());
        }

        @Override
        public void close() throws IOException {
            _localServerSocket.close();
        }

        @Override
        public String describeLocalEndpoint() {
            return describeLocalSocketAddress(_localServerSocket.getLocalSocketAddress());
        }
    }

    private static class OutgoingConnectionHandler implements Runnable {
        private final AbstractSocket _connector;
        private final Object _endpoint;
        private final ByteBuffer _auth_payload;

        public OutgoingConnectionHandler(AbstractSocket connector, Object endpoint, ByteBuffer auth_payload) {
            _connector = connector;
            _endpoint = endpoint;
            _auth_payload = auth_payload;
        }

        @Override
        public void run() {
            try {
                addPendingSocket(_connector);
                System.out.println("new outgoing sub-connection to " + _connector.describeEndpoint(_endpoint));
                _connector.connect(_endpoint);
                removePendingSocket(_connector);
                System.out.println("connected to " + _connector.describeEndpoint(_endpoint));
                ReadableByteChannel ic = Channels.newChannel(_connector.getInputStream());
                WritableByteChannel oc = Channels.newChannel(_connector.getOutputStream());
                writeFully(oc, _auth_payload.duplicate());
                ControlConnection handler = ControlConnection.newSubConnection(ic, oc);
                handler.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class IncomingConnectionHandler implements Runnable {
        private final AbstractServerSocket _acceptor;
        private final ByteBuffer _auth_payload;

        public IncomingConnectionHandler(AbstractServerSocket acceptor, ByteBuffer auth_payload) {
            _acceptor = acceptor;
            _auth_payload = auth_payload;
        }

        @Override
        public void run() {
            try {
                addPendingSocket(_acceptor);
                System.out.printf("waiting for incoming sub-connection on %s\n", _acceptor.describeLocalEndpoint());
                AbstractSocket connection = _acceptor.accept();
                removePendingSocket(_acceptor);
                System.out.printf("accepted connection on %s\n", _acceptor.describeLocalEndpoint());
                _acceptor.close();
                ReadableByteChannel ic = Channels.newChannel(connection.getInputStream());
                WritableByteChannel oc = Channels.newChannel(connection.getOutputStream());
                ByteBuffer peerPayload = ByteBuffer.allocate(_auth_payload.remaining());
                readFully(ic, peerPayload);
                peerPayload.flip();
                if (!peerPayload.equals(_auth_payload)) {
                    throw new IOException("invalid connection payload");
                }
                ControlConnection handler = ControlConnection.newSubConnection(ic, oc);
                handler.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void runIncomingConnection(ServerSocket sock, ByteBuffer auth_payload) {
        pool.execute(new IncomingConnectionHandler(new ServerSocketWrapper(sock), auth_payload.duplicate()));
    }

    public static void runIncomingConnection(LocalServerSocket sock, ByteBuffer auth_payload) {
        pool.execute(new IncomingConnectionHandler(new LocalServerSocketWrapper(sock), auth_payload.duplicate()));
    }

    public static void runOutgoingConnection(SocketAddress endpoint, ByteBuffer auth_payload) {
        pool.execute(new OutgoingConnectionHandler(new SocketWrapper(new Socket()), endpoint, auth_payload.duplicate()));
    }

    public static void runOutgoingConnection(LocalSocketAddress endpoint, ByteBuffer auth_payload) {
        pool.execute(new OutgoingConnectionHandler(new LocalSocketWrapper(new LocalSocket()), endpoint, auth_payload.duplicate()));
    }

    protected static void addPendingSocket(Closeable socket) {
        synchronized (pendingSockets) {
            pendingSockets.add(socket);
        }
    }

    protected static void removePendingSocket(Closeable socket) {
        synchronized (pendingSockets) {
            pendingSockets.remove(socket);
        }
    }

    public static void closePendingSockets() {
        synchronized (pendingSockets) {
            for (Closeable socket : pendingSockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            pendingSockets.clear();
        }
    }

    public static void blockingShutdown() {
        pool.shutdown();
        closePendingSockets();
        try {
            while (!pool.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.SECONDS)) {
                // wait again if timeout elapsed
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

