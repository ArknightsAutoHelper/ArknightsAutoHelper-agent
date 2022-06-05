package xyz.cirno.scrsrv;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Looper;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: aah-agent <socket-name>");
            System.out.println("  <socket-name> is the name of the abstract socket to listen on");
            System.exit(1);
        }
        try (LocalServerSocket s = new LocalServerSocket(args[0])) {
            System.out.printf("bootstrap connection: listening on AF_UNIX abstract: %s\n", args[0]);
            LocalSocket conn = s.accept();
            System.out.println("connected");
            ControlConnection.newBootstrapConnection(Channels.newChannel(conn.getInputStream()), Channels.newChannel(conn.getOutputStream())).run();
            // the bootstrap connection is closed, wait for sub-connections to finish
            ConnectionManager.blockingShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
