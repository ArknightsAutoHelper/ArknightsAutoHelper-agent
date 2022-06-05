package xyz.cirno.aah.agent;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Util {
    public static ByteBuffer readFully(ReadableByteChannel ch, int size) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(size);
        int read = 0;
        do {
            read = ch.read(b);
            if (read == -1) {
                throw new EOFException();
            }
        } while (b.hasRemaining());
        b.flip();
        return b;
    }

    public static void readFully(ReadableByteChannel ch, ByteBuffer dst) throws IOException {
        int read = 0;
        do {
            read = ch.read(dst);
            if (read == -1) {
                throw new EOFException();
            }
        } while (dst.hasRemaining());
    }

    public static void writeFully(WritableByteChannel ch, ByteBuffer src) throws IOException {
        while (src.hasRemaining()) {
            ch.write(src);
        }
    }
}
