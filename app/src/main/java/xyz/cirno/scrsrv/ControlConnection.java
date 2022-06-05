package xyz.cirno.scrsrv;

import static xyz.cirno.scrsrv.Util.readFully;
import static xyz.cirno.scrsrv.Util.writeFully;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.hardware.input.InputManagerHidden;
import android.net.LocalServerSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressLint("BlockedPrivateApi")
public class ControlConnection {

    // flags
    public static final int DISPLAY_FLAG_SCREEN_CAPTURE = 0x1;
    public static final int DISPLAY_FLAG_SCREEN_CAPTURE_SECURE = 0x3;
    public static final int DISPLAY_FLAG_INPUT = 0x4;

    public static final int EVENT_FLAG_ASYNC = 0x1;
    public static final int EVENT_FLAG_MERGE_MULTITOUCH_MOVE = 0x2;


    private static final Method KeyEvent_setDisplayId;



    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private static final int COMMAND_OPEN = 0x4f50454e; // 'OPEN'
    private static final int COMMAND_SET_DISPLAY = 0x44495350; // 'DISP'
    private static final int COMMAND_SYNC = 0x53594e43; // 'SYNC'
    private static final int COMMAND_SCREEN_CAPTURE = 0x53434150; // 'SCAP'
    private static final int COMMAND_BEGIN_BATCH = 0x42454742; // 'BEGB'
    private static final int COMMAND_END_BATCH = 0x454e4442; // 'ENDB'
    private static final int COMMAND_TOUCH_EVENT = 0x544f5543; // 'TOUC'
    private static final int COMMAND_KEY_EVENT = 0x4b455920; // 'KEY '
    private static final int COMMAND_KEY_PRESS = 0x4b505253; // 'KPRS'
    private static final int COMMAND_SEND_TEXT = 0x54455854; // 'TEXT'



    private static final int TOKEN_OK = 0x4f4b4159; // 'OKAY
    private static final int TOKEN_FAIL = 0x4641494c; // 'FAIL'



    private static final Set<Integer> bootstrapCommands = new HashSet<>(Collections.singletonList(COMMAND_OPEN));
    private static final Set<Integer> subConnectionCommands = new HashSet<>(Arrays.asList(
        COMMAND_SET_DISPLAY,
        COMMAND_SYNC,
        COMMAND_SCREEN_CAPTURE,
        COMMAND_TOUCH_EVENT,
        COMMAND_KEY_EVENT,
        COMMAND_KEY_PRESS,
        COMMAND_SEND_TEXT,
        COMMAND_BEGIN_BATCH,
        COMMAND_END_BATCH
    ));
    private static final Set<Integer> fullCommands = new HashSet<>();

    static {
        fullCommands.addAll(bootstrapCommands);
        fullCommands.addAll(subConnectionCommands);
    }

    private final ReadableByteChannel inChannel;
    private final WritableByteChannel outChannel;
    private Set<Integer> acceptedCommands;

    private int displayId = -1;
    private CaptureSession session = null;
    private LZ4Compressor compressor = null;

    private TouchStateTracker touchStateTracker = null;
    private InputManagerHidden inputManager = null;
    private ClipboardWrapper clipboard = null;

    private boolean inBatchEvent = false;
    private long lastBatchEventTime = 0;
    private final List<BatchEventRecord> batchEvents = new ArrayList<>();

    private static class BatchEventRecord {
        public final InputEvent event;
        public final int mode;

        public BatchEventRecord(InputEvent event, int mode) {
            this.event = event;
            this.mode = mode;
        }
    }


    static {
        Method meth = null;
        try {
            meth = KeyEvent.class.getMethod("setDisplayId", int.class);
        } catch (NoSuchMethodException ignored) {}
        KeyEvent_setDisplayId = meth;
    }


    private ControlConnection(ReadableByteChannel inChannel, WritableByteChannel outChannel, Set<Integer> acceptedCommands) {
        this.inChannel = inChannel;
        this.outChannel = outChannel;
        this.acceptedCommands = acceptedCommands;
    }

    public static ControlConnection newBootstrapConnection(ReadableByteChannel inChannel, WritableByteChannel outChannel) {
        return new ControlConnection(inChannel, outChannel, bootstrapCommands);
    }

    public static ControlConnection newSubConnection(ReadableByteChannel inChannel, WritableByteChannel outChannel) {
        return new ControlConnection(inChannel, outChannel, subConnectionCommands);
    }

    public void run() {
        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(8);
        while (true) {
            try {
                headerBuffer.rewind();
                readFully(inChannel, headerBuffer);
                headerBuffer.flip();
                int command = headerBuffer.getInt();
                int payloadLength = headerBuffer.getInt();
                ByteBuffer payload;
                if (payloadLength > 0) {
                    payload = ByteBuffer.allocateDirect(payloadLength);
                    inChannel.read(payload);
                    payload.flip();
                } else {
                    payload = EMPTY_BUFFER;
                }
                ByteBuffer response = EMPTY_BUFFER;
                int responseToken = TOKEN_OK;
                try {
                    if (!acceptedCommands.contains(command)) {
                        throw new UnsupportedOperationException(String.format("Unsupported command %08x", command));
                    }
                    switch (command) {
                        case COMMAND_OPEN:
                            response = handleCommandOPEN(payload);
                            break;
                        case COMMAND_SET_DISPLAY:
                            handleCommandDISP(payload);
                            break;
                        case COMMAND_SYNC:
                            response = handleCommandSYNC(payload);
                            break;
                        case COMMAND_SCREEN_CAPTURE:
                            response = handleCommandSCAP(payload);
                            break;
                        case COMMAND_TOUCH_EVENT:
                            handleCommandTOUC(payload);
                            break;
                        case COMMAND_KEY_EVENT:
                            handleCommandKEY(payload);
                            break;
                        case COMMAND_KEY_PRESS:
                            handleCommandKPRS(payload);
                            break;
                        case COMMAND_SEND_TEXT:
                            handleCommandTEXT(payload);
                            break;
                        case COMMAND_BEGIN_BATCH:
                            handleCommandBEGB(payload);
                            break;
                        case COMMAND_END_BATCH:
                            handleCommandENDB(payload);
                            break;
                        default:
                            throw new UnsupportedOperationException(String.format("Unsupported command %08x", command));
                    }
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    responseToken = TOKEN_FAIL;
                    byte[] bytesMessage = sw.toString().getBytes(StandardCharsets.UTF_8);
                    response = ByteBuffer.wrap(bytesMessage);
                }
                headerBuffer.rewind();
                headerBuffer.putInt(responseToken);
                headerBuffer.putInt(response.remaining());
                headerBuffer.flip();
                writeFully(outChannel, headerBuffer);
                writeFully(outChannel, response);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            inChannel.close();
            outChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer handleCommandOPEN(ByteBuffer payload) throws IOException {
        /*
         * OPEN command payload
         * int type; // 0: use this connection, 1: connect to address, 2: listen on address
         * int addressFamily; // 0: IPv4, 1: IPv6, 2: abstract
         * byte[] address;
         * byte[] connectionPayload;
         *
         * response payload:
         * byte[] address; // if type == 2
         * */
        int payloadLength = payload.remaining();
        if (payloadLength < 8) {
            throw new RuntimeException("command OPEN: invalid payload length");
        }

        int type = payload.getInt();
        int addressFamily = payload.getInt();
        if (type == 0) {
            if (acceptedCommands != bootstrapCommands) {
                throw new RuntimeException("this connection is already initialized");
            }
            acceptedCommands = fullCommands;
            return EMPTY_BUFFER;
        }


        int minimumPayloadLength, addressLength;

        if (addressFamily == 0 || addressFamily == 1) {
            if (addressFamily == 0) {
                addressLength = 4;
            } else {
                addressLength = 16;
            }

            minimumPayloadLength = 8 + addressLength + 2;
            if (payloadLength < minimumPayloadLength) {
                throw new RuntimeException("command OPEN: invalid payload length");
            }

            byte[] addressBytes = new byte[addressLength];
            payload.get(addressBytes);
            int port = payload.getShort() & 0xffff;
            final SocketAddress address = new java.net.InetSocketAddress(java.net.InetAddress.getByAddress(addressBytes), port);
            final ByteBuffer connectionPayload = payload.duplicate();

            if (type == 1) {
                ConnectionManager.runOutgoingConnection(address, connectionPayload);
            } else if (type == 2) {
                final ServerSocket ss = new ServerSocket();
                ss.bind(address);
                InetSocketAddress boundAddress = (InetSocketAddress) ss.getLocalSocketAddress();
                byte[] ip = boundAddress.getAddress().getAddress();
                int listenPort = boundAddress.getPort();
                ByteBuffer response = ByteBuffer.allocate(ip.length + 2);
                response.put(ip);
                response.putShort((short) (listenPort & 0xFFFF));
                ConnectionManager.runIncomingConnection(ss, connectionPayload);
                response.flip();
                return response;
            } else {
                throw new RuntimeException("command OPEN: invalid type");
            }
        } else if (addressFamily == 2) {
            addressLength = payload.getShort();
            byte[] addressBytes = new byte[addressLength];
            payload.get(addressBytes);
            final String address = new String(addressBytes, StandardCharsets.UTF_8);
            final ByteBuffer connectionPayload = payload.duplicate();
            LocalSocketAddress endpoint = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
            if (type == 1) {
                ConnectionManager.runOutgoingConnection(endpoint, connectionPayload);
            } else if (type == 2) {
                final LocalServerSocket lss = new LocalServerSocket(address);
                ConnectionManager.runIncomingConnection(lss, connectionPayload);
                ByteBuffer result = ByteBuffer.allocate(addressBytes.length + 2);
                result.putShort((short) addressBytes.length);
                result.put(addressBytes);
                result.flip();
                return result;
            }
        } else {
            throw new RuntimeException("command OPEN: invalid addressFamily");
        }

        return EMPTY_BUFFER;
    }

    public void handleCommandDISP(ByteBuffer payload) {
        /*
         * DISP request payload
         * int displayId;
         * int flags;
         */
        int payloadLength = payload.remaining();
        if (payloadLength != 8) {
            throw new RuntimeException("command DISP: invalid payload length");
        }
        displayId = payload.getInt();
        int flags = payload.getInt();
        if ((flags & DISPLAY_FLAG_SCREEN_CAPTURE) != 0) {
            boolean secure = (flags & DISPLAY_FLAG_SCREEN_CAPTURE_SECURE) != 0;
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            session = new CaptureSessionVirtualDisplay(displayId, secure);
        }
        if ((flags & DISPLAY_FLAG_INPUT) != 0) {
            touchStateTracker = new TouchStateTracker(displayId);
            inputManager = InputManagerHidden.getInstance();
            clipboard = ClipboardWrapper.getInstance();
        }
    }

    public ByteBuffer handleCommandSYNC(ByteBuffer payload) {
        /* SYNC command
         * no request payload
         *
         * response:
         * long nanoTime;
         */
        int payloadLength = payload.remaining();
        long t = System.nanoTime();
        if (payloadLength != 0) {
            throw new RuntimeException("command SYNC: invalid payload length");
        }
        ByteBuffer result = ByteBuffer.allocateDirect(8);
        result.putLong(t);
        return (ByteBuffer) result.flip();
    }

    public ByteBuffer handleCommandSCAP(ByteBuffer payload) {
        /* SCAP command payload
         * int compress; // 0: uncompressed, 1: compressed
         *
         * response:
         * int width;
         * int height;
         * int pixelStride;
         * int rowStride;
         * int colorSpace;
         * long timestamp;
         * int originalSize; // 0 for uncompressed data
         * byte[] data; // remaining size of response
         */
        int payloadLength = payload.remaining();
        if (payloadLength != 4) {
            throw new RuntimeException("command SCAP: invalid payload length");
        }
        int compressLevel = payload.getInt();
        ByteBuffer result;
        if (session == null) {
            throw new RuntimeException("command SCAP: connection not initialized for screen capture");
        } else {
            ScreenshotImage sci = session.screenshot();
            // System.err.printf("Image: size %dx%d, colorspace %s, blocking fetched in %.3fms, rendered %.3f ms ago\n", sci.width, sci.height, sci.colorSpace, (t1 - t0) / 1e6, (t1 - sci.timestamp) / 1e6);
            int decompressedSize;
            int maxDataLength;
            if (compressLevel == 0) {
                maxDataLength = sci.data.remaining();
                decompressedSize = 0;
            } else {
                if (compressor == null) {
                    compressor = LZ4Factory.nativeInstance().fastCompressor();
                }
                decompressedSize = sci.data.remaining();
                maxDataLength = compressor.maxCompressedLength(decompressedSize);
            }
            result = ByteBuffer.allocateDirect(32 + maxDataLength);
            result.putInt(sci.width);
            result.putInt(sci.height);
            result.putInt(sci.pixelStride);
            result.putInt(sci.rowStride);
            result.putInt(sci.colorSpace.ordinal());
            result.putLong(sci.timestamp);
            result.putInt(decompressedSize);
            if (compressLevel == 0) {
                result.put(sci.data);
            } else {
                // compress image
                compressor.compress(sci.data, result);
            }
        }
        return (ByteBuffer) result.flip();
    }

    private long getEventTime() {
        if (inBatchEvent) {
            return lastBatchEventTime;
        }
        return SystemClock.uptimeMillis();
    }

    private void handleCommandBEGB(ByteBuffer payload) {
        if (inBatchEvent) {
            throw new RuntimeException("command BEGB: already in batch event");
        }
        inBatchEvent = true;
        lastBatchEventTime = SystemClock.uptimeMillis();
    }

    private void handleCommandENDB(ByteBuffer payload) {
        if (!inBatchEvent) {
            throw new RuntimeException("command ENDB: not in batch event");
        }
        inBatchEvent = false;

        BatchEventRecord[] pendingEvents;
        synchronized (batchEvents) {
            pendingEvents = batchEvents.toArray(new BatchEventRecord[batchEvents.size()]);
            batchEvents.clear();
        }
        for (BatchEventRecord record : pendingEvents) {
            inputManager.injectInputEvent(record.event, record.mode);
        }
    }

    private void injectEvent(InputEvent ev, int mode) {
        if (inBatchEvent) {
            synchronized (batchEvents) {
                batchEvents.add(new BatchEventRecord(ev, mode));
            }
        } else {
            inputManager.injectInputEvent(ev, mode);
        }
    }

    private void handleCommandTOUC(ByteBuffer payload) {
        if (touchStateTracker == null || inputManager == null) {
            throw new RuntimeException("command TOUC: controller not initialized");
        }
        int payloadLength = payload.remaining();
        if (payloadLength != 24) {
            throw new RuntimeException("command TOUC: invalid payload length");
        }

        int action = payload.getInt();
        int pointerId = payload.getInt();
        float x = payload.getFloat();
        float y = payload.getFloat();
        float pressure = payload.getFloat();
        int flags = payload.getInt();
        boolean async = (flags & EVENT_FLAG_ASYNC) != 0;
        boolean deferred = (flags & EVENT_FLAG_MERGE_MULTITOUCH_MOVE) != 0;
        int mode = async ? InputManagerHidden.INJECT_INPUT_EVENT_MODE_ASYNC : InputManagerHidden.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH;

        touchStateTracker.updateState(getEventTime(), action, pointerId, x, y, pressure, deferred);
        for (MotionEvent ev : touchStateTracker.fetchPendingEvents()) {
            injectEvent(ev, mode);
        }
    }

    private void handleCommandKEY(ByteBuffer payload) {
        if (inputManager == null) {
            throw new RuntimeException("command KEY: controller not initialized");
        }
        int payloadLength = payload.remaining();
        if (payloadLength != 12) {
            throw new RuntimeException("command KEY: invalid payload length");
        }

        int action = payload.getInt();
        int keyCode = payload.getInt();
        int metaState = payload.getInt();
        boolean async = payload.getInt() != 0;
        int mode = async ? InputManagerHidden.INJECT_INPUT_EVENT_MODE_ASYNC : InputManagerHidden.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH;

        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) {
            throw new RuntimeException("command KEY: invalid action");
        }

        final long now = getEventTime();
        KeyEvent ev = new KeyEvent(now, now, action, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);

        injectEvent(ev, mode);
    }

    private void sendKeyEvent(int keyCode) {
        sendKeyEvent(keyCode, 0);
    }

    private void sendKeyEvent(int keyCode, int metaState) {
        final long now = getEventTime();

        KeyEvent event = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0,
                metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        if (KeyEvent_setDisplayId != null) {
            try {
                KeyEvent_setDisplayId.invoke(event, displayId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        injectEvent(event, InputManagerHidden.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
        injectEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP), InputManagerHidden.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    private void handleCommandKPRS(ByteBuffer payload) {
        if (inputManager == null) {
            throw new RuntimeException("command KPRS: controller not initialized");
        }
        int payloadLength = payload.remaining();
        if (payloadLength != 8) {
            throw new RuntimeException("command KPRS: invalid payload length");
        }

        int keyCode = payload.getInt();
        int metaState = payload.getInt();
        sendKeyEvent(keyCode, metaState);
    }

    private void handleCommandTEXT(ByteBuffer payload) {
        if (inputManager == null) {
            throw new RuntimeException("command TEXT: controller not initialized");
        }
        int payloadLength = payload.remaining();
        if (payloadLength == 0) {
            return;
        }
        if (inBatchEvent) {
            throw new RuntimeException("command TEXT: cannot send text in batch event");
        }
        String text = StandardCharsets.UTF_8.decode(payload).toString();
        char[] chars = text.toCharArray();
        final KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        final KeyEvent[] events = kcm.getEvents(chars);
        if (events != null) {
            for (KeyEvent ev : events) {
                ev.setSource(InputDevice.SOURCE_KEYBOARD);
                if (KeyEvent_setDisplayId != null) {
                    try {
                        KeyEvent_setDisplayId.invoke(ev, displayId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                injectEvent(ev, InputManagerHidden.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // fallback to clipboard approach
            ClipData oldClip = clipboard.getPrimaryClip();
            ClipData clip = ClipData.newPlainText(null, text);
            clipboard.setPrimaryClip(clip);
            sendKeyEvent(KeyEvent.KEYCODE_PASTE);
            try {
                if (oldClip != null) {
                    clipboard.setPrimaryClip(oldClip);
                }
            } catch (Exception ignore) {}
        } else {
            throw new RuntimeException("command TEXT: text cannot be injected");
        }
    }

}