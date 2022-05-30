package xyz.cirno.scrsrv;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Looper;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

public class Main {
    public static void main(String[] args) {
        Looper.prepareMainLooper();
        try (LocalServerSocket server = new LocalServerSocket("scrsrv")) {
            System.out.println("listening on AF_UNIX:\"\\0scrsrv\"");
            LocalSocket s = server.accept();
            System.out.println("accepted");
            handleConnection(s.getInputStream(), s.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleConnection(InputStream is, OutputStream os) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        DataOutputStream dos = new DataOutputStream(os);
        CaptureSession session = null;
        LZ4Compressor compressor = null;
        try {
            while (true) {
                int command = dis.readInt();
                int payloadLength = dis.readInt();
                if (command == 0x44495350 /* 'DISP' */) {
                    if (payloadLength != 8) {
                        dos.writeInt(0x4641494c /* 'FAIL' */);
                        throw new RuntimeException("command DISP: invalid payload length");
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
                        throw new RuntimeException("command SYNC: invalid payload length");
                    }
                    dos.writeInt(0x4f4b4159 /* 'OKAY' */);
                    dos.writeInt(8); // response length
                    dos.writeLong(System.nanoTime());
                } else if (command == 0x53434150 /* 'SCAP' */) {
//                    System.out.println("received SCAP");
                    if (payloadLength != 4) {
                        dos.writeInt(0x4641494c /* 'FAIL' */);
                        throw new RuntimeException("command SCAP: invalid payload length");
                    }
                    int compressLevel = dis.readInt();
                    if (session == null) {
                        dos.writeInt(0x4f4b4159 /* 'OKAY' */);
                        dos.writeInt(32); // response length
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeInt(0);
                        dos.writeLong(0);
                        dos.writeInt(0);
                    } else {
                        // long t0 = System.nanoTime();
                        ScreenshotImage sci = session.screenshot();
                        // long t1 = System.nanoTime();
                        // System.err.printf("Image: size %dx%d, colorspace %s, blocking fetched in %.3fms, rendered %.3f ms ago\n", sci.width, sci.height, sci.colorSpace, (t1 - t0) / 1e6, (t1 - sci.timestamp) / 1e6);
                        ByteBuffer bufferToSend;
                        int decompressedSize;
                        if (compressLevel == 0) {
                            decompressedSize = 0;
                            bufferToSend = sci.data;
                        } else {
                            if (compressor == null) {
                                compressor = LZ4Factory.nativeInstance().fastCompressor();
                            }
                            // compress image
                            decompressedSize = sci.data.remaining();
                            ByteBuffer compressed = ByteBuffer.allocate(compressor.maxCompressedLength(decompressedSize));
                            compressor.compress(sci.data, compressed);
                            compressed.flip();
                            bufferToSend = compressed;
                        }
                        dos.writeInt(0x4f4b4159); // 'OKAY'
                        dos.writeInt(32 + bufferToSend.remaining()); // response length
                        dos.writeInt(sci.width);
                        dos.writeInt(sci.height);
                        dos.writeInt(sci.pixelStride);
                        dos.writeInt(sci.rowStride);
                        dos.writeInt(sci.colorSpace.ordinal());
                        dos.writeLong(sci.timestamp);
                        dos.writeInt(decompressedSize);
                        Channels.newChannel(dos).write(bufferToSend);
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
            is.close();
            os.close();
        }
    }
}
