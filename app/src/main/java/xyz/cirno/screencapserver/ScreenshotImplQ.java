package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.ColorSpace;
import android.os.IBinder;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(29)
public class ScreenshotImplQ implements IScreenshotImpl {
    // implementation for Q and R (API 29-30)
    // supports multiple displays
    // supports color management
    // Bitmap is hardware-backed
    // uses API:
    // IBinder getPhysicalDisplayToken(long physicalDisplayId)
    // ScreenshotGraphicBuffer screenshotToBuffer(IBinder display, Rect sourceCrop,
    //            int width, int height, boolean useIdentityTransform, int rotation)

    private final Method screenshotToBuffer;
    private final Method getPhysicalDisplayToken;
    private final Method screenshotGraphicBuffer_getGraphicBuffer;
    private final Method screenshotGraphicBuffer_getColorSpace;
    private final Method bitmap_wrapHardwareBuffer;

    private final int sRGB_id;
    private final int displayP3_id;

    public ScreenshotImplQ() {
        try {
            getPhysicalDisplayToken = SurfaceControlWrapper.CLASS.getDeclaredMethod("getPhysicalDisplayToken", long.class);
            screenshotToBuffer = SurfaceControlWrapper.CLASS.getDeclaredMethod("screenshotToBuffer", IBinder.class, Rect.class, int.class, int.class, boolean.class, int.class);
            Class<?> screenshotGraphicBufferClass = Class.forName("android.view.SurfaceControl$ScreenshotGraphicBuffer");
            screenshotGraphicBuffer_getGraphicBuffer = screenshotGraphicBufferClass.getDeclaredMethod("getGraphicBuffer");
            screenshotGraphicBuffer_getColorSpace = screenshotGraphicBufferClass.getDeclaredMethod("getColorSpace");
            Class<?> graphicBufferClass = Class.forName("android.graphics.GraphicBuffer");
            bitmap_wrapHardwareBuffer = Bitmap.class.getDeclaredMethod("wrapHardwareBuffer", graphicBufferClass, ColorSpace.class);
            sRGB_id = ColorSpace.get(ColorSpace.Named.SRGB).getId();
            displayP3_id = ColorSpace.get(ColorSpace.Named.DISPLAY_P3).getId();
        } catch (Exception e) {
            throw new RuntimeException("ScreenshotImplQ initialization failed");
        }
    }

    @Override
    public IBinder resolvePhysicalDisplay(long physicalDisplayId) {
        try {
            return (IBinder) getPhysicalDisplayToken.invoke(null, physicalDisplayId);
        } catch (Exception e) {
            throw new RuntimeException("resolvePhysicalDisplay invocation failed", e);
        }
    }

    @Override
    public ScreenshotImage screenshot(IBinder display, int width, int height) {
        try {
            Rect rc = new Rect();
            Object sgb = screenshotToBuffer.invoke(null, display, rc, width, height, false, 0);
            Object gb = screenshotGraphicBuffer_getGraphicBuffer.invoke(sgb);
            ColorSpace cs = (ColorSpace) screenshotGraphicBuffer_getColorSpace.invoke(sgb);
            Bitmap hwbmp = (Bitmap) bitmap_wrapHardwareBuffer.invoke(null, gb, cs);
            Bitmap bmp = hwbmp.copy(Bitmap.Config.ARGB_8888, false);
            hwbmp.recycle();
            ScreenshotImage.ColorSpace color = ScreenshotImage.ColorSpace.UNKNOWN;
            if (cs != null) {
                int csid = cs.getId();
                if (csid == sRGB_id) color = ScreenshotImage.ColorSpace.SRGB;
                else if (csid == displayP3_id) color = ScreenshotImage.ColorSpace.DISPLAY_P3;
            }
            return new ScreenshotImage(bmp, color, 0);
        } catch (Exception e) {
            throw new RuntimeException("screenshot failed", e);
        }
    }
}
