package xyz.cirno.aah.agent;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Size;
import android.view.DisplayInfo;
import android.view.SurfaceControlAllVersion;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("PrivateApi, SoonBlockedPrivateApi")
public class CaptureSessionVirtualDisplay extends CaptureSession {

    private final int sourceDisplayId;
    private final boolean captureSecureLayers;
    private IBinder virtualDisplayToken;
    private ImageReader imageReader;
    private int sourceLayerStack;
    private Size currentSize;
    private ScreenshotImage.ColorSpace colorSpace = ScreenshotImage.ColorSpace.UNKNOWN;

    private Field colorModeField;

    private final Object lock = new Object();
    private boolean displayChanged = false;
    private final HandlerThread handlerThread;
    private final Handler handler;

    private Image lastImage;
    // private long lastImageTime = -1;
    private final Object lastImageLock = new Object();

    private final AutoResetEvent frameAvailableEvent;

    public CaptureSessionVirtualDisplay(int sourceDisplayId, boolean captureSecureLayers) {
        this.sourceDisplayId = sourceDisplayId;
        this.captureSecureLayers = captureSecureLayers;

        try {
            colorModeField = DisplayInfo.class.getDeclaredField("colorMode");
        } catch (NoSuchFieldException ignored) {
            colorModeField = null;
        }
        frameAvailableEvent = new AutoResetEvent(false);
        handlerThread = new HandlerThread("CaptureSessionVirtualDisplay");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        createVirtualDisplay();
        getDisplayManager().registerCallback(new IDisplayManagerCallback.Stub() {
            @Override
            public void onDisplayEvent(int displayId, int event) {
                CaptureSessionVirtualDisplay.this.onDisplayEvent(displayId, event);
            }
        });
    }


    @SuppressLint("WrongConstant")
    private void createVirtualDisplay() {
        destroyVirtualDisplay();
        DisplayInfo sourceDisplayInfo = getDisplayManager().getDisplayInfo(sourceDisplayId);

        Rect rc = new Rect(0, 0, sourceDisplayInfo.logicalWidth, sourceDisplayInfo.logicalHeight);
        currentSize = new Size(sourceDisplayInfo.logicalWidth, sourceDisplayInfo.logicalHeight);
        sourceLayerStack = sourceDisplayInfo.layerStack;
        virtualDisplayToken = SurfaceControlAllVersion.createDisplay("ScreenCapServer", captureSecureLayers);
        if (colorModeField != null) {
            // virtual display is always composed in sRGB color space.
            colorSpace = ScreenshotImage.ColorSpace.SRGB;
        }
        imageReader = ImageReader.newInstance(rc.width(), rc.height(), PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image newimg = imageReader.acquireNextImage();
                long t = System.nanoTime();
                if (newimg == null) {
                    return;
                }
                synchronized (lastImageLock) {
                    // lastImageTime = t;
                    if (lastImage != null) {
                        lastImage.close();
                    }
                    lastImage = newimg;
                }
                frameAvailableEvent.set();
            }
        }, handler);
        SurfaceControlAllVersion.openTransaction();
        try {
            SurfaceControlAllVersion.setDisplaySurface(virtualDisplayToken, imageReader.getSurface());
            SurfaceControlAllVersion.setDisplayProjection(virtualDisplayToken, 0, rc, rc);
            SurfaceControlAllVersion.setDisplayLayerStack(virtualDisplayToken, sourceLayerStack);
        } finally {
            SurfaceControlAllVersion.closeTransaction();
        }
    }

    private void destroyVirtualDisplay() {
        if (virtualDisplayToken != null) {
            SurfaceControlAllVersion.destroyDisplay(virtualDisplayToken);
            virtualDisplayToken = null;
        }
    }

    private ScreenshotImage fetchLastImage() {
        Image img = lastImage;
        Image.Plane plane = img.getPlanes()[0];
        int width = img.getWidth();
        int height = img.getHeight();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        ByteBuffer buf = plane.getBuffer().duplicate();
        int validSize = plane.getRowStride() * (img.getHeight() - 1) + plane.getPixelStride() * img.getWidth();
        buf.limit(validSize);
        // the buffer may be hardware-backed, copy to CPU
        ByteBuffer swbuf = ByteBuffer.allocate(validSize);
        swbuf.put(buf);
        swbuf.flip();
        return new ScreenshotImage(swbuf, width, height, rowStride, pixelStride, colorSpace, 0, img.getTimestamp());
    }

    @Override
    public ScreenshotImage screenshot() {
        if (lastImage == null) {
            try {
                frameAvailableEvent.waitOne();
            } catch (InterruptedException ignored) {}
        }
        synchronized (lastImageLock) {
            return fetchLastImage();
        }
    }

    private void handleDisplayChanged() {
        synchronized (lock) {
            IDisplayManager displayManager = getDisplayManager();
            DisplayInfo info = displayManager.getDisplayInfo(sourceDisplayId);
            // layer stack (content source) changed
            if (info.layerStack != sourceLayerStack) {
                displayChanged = true;
            }
            // size changed (including orientation)
            if (!new Size(info.logicalWidth, info.logicalHeight).equals(currentSize)) {
                displayChanged = true;
            }
            // color mode changed
            if (colorModeField != null) {
                // FIXME: DisplayInfo.colorMode does not reflect Surfaceflinger composition space
                /*
                int colorMode = info.colorMode;
                colorSpace = ScreenshotImage.ColorSpace.fromHwcMode(colorMode);
                SurfaceControlAllVersion.setActiveColorMode(virtualDisplayToken, colorMode);
                */
            }
            if (displayChanged) {
                createVirtualDisplay();
            }
        }
    }

    public void onDisplayEvent(int displayId, int event) {
        System.out.printf("onDisplayEvent: displayId=%d, event=%d\n", displayId, event);
        if (displayId == sourceDisplayId) {
            if (event == IDisplayManagerCallback.EVENT_DISPLAY_CHANGED) {
                handleDisplayChanged();
            } else if (event == IDisplayManagerCallback.EVENT_DISPLAY_REMOVED) {
                destroyVirtualDisplay();
            }
        }
    }

    @Override
    public void close() {
        destroyVirtualDisplay();
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        handlerThread.quit();
    }

}
