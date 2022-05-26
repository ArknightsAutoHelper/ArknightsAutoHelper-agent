package xyz.cirno.screencapserver;

import android.hardware.display.IDisplayManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Looper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.Channels;

public class Entry {
    public static void main(String[] args) {
        Looper.prepareMainLooper();
        try (LocalServerSocket server = new LocalServerSocket("scrsrv")) {
            System.out.println("listening on AF_UNIX:\"\\0scrsrv\"");
            LocalSocket s = server.accept();
            System.out.println("accepted");
            handleConnection(s);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleConnection(LocalSocket s) throws IOException {
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        CaptureSession session = null;
        try {
            while (true) {
                int command = dis.readInt();
                int payloadLength = dis.readInt();
                if (command == 0x44495350 /* 'DISP' */) {
                    if (payloadLength != 8) {
                        dos.writeInt(0x4641494c /* 'FAIL' */);
                        throw new RuntimeException("invalid payload length");
                    }
                    int displayId = dis.readInt();
                    boolean secure = dis.readInt() != 0;
                    session = new CaptureSessionVirtualDisplay(displayId, secure);
                    dos.writeInt(0x4f4b4159 /* 'OKAY' */);
                    dos.writeInt(0);
                } else if (command == 0x53594e43 /* 'SYNC' */) {
                    long t = System.nanoTime();
                    if (payloadLength != 0) {
                        dos.writeInt(0x4641494c /* 'FAIL' */);
                        throw new RuntimeException("invalid payload length");
                    }
                    dos.writeInt(0x4f4b4159 /* 'OKAY' */);
                    dos.writeInt(8); // response length
                    dos.writeLong(System.nanoTime());
                    System.out.println("received SYNC");
                } else if (command == 0x53434150 /* 'SCAP' */) {
                    System.out.println("received SCAP");
                    if (payloadLength != 0) {
                        dos.writeInt(0x4641494c /* 'FAIL' */);
                        throw new RuntimeException("invalid payload length");
                    }
                    if (session == null) {
                        dos.writeInt(0x4f4b4159 /* 'OKAY' */);
                        dos.writeInt(28); // response length
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeLong(0);
                    } else {
                        // long t0 = System.nanoTime();
                        ScreenshotImage sci = session.screenshot();
                        // long t1 = System.nanoTime();
                        // System.err.printf("Image: size %dx%d, colorspace %s, blocking fetched in %.3fms, rendered %.3f ms ago\n", sci.width, sci.height, sci.colorSpace, (t1 - t0) / 1e6, (t1 - sci.timestamp) / 1e6);
                        dos.writeInt(0x4f4b4159); // 'OKAY'
                        dos.writeInt(28 + sci.data.remaining()); // response length
                        dos.writeInt(sci.width);
                        dos.writeInt(sci.height);
                        dos.writeInt(sci.pixelStride);
                        dos.writeInt(sci.rowStride);
                        dos.writeInt(sci.colorSpace.ordinal());
                        dos.writeLong(sci.timestamp);
                        Channels.newChannel(dos).write(sci.data);
                    }
                }
                dos.flush();
            }
        } catch (EOFException e) {
            System.out.println("connection closed");
        } finally {
            if (session != null) {
                session.close();
            }
            s.close();
        }
    }
}
