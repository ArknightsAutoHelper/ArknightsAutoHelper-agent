package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(31)
public class ScreenshotImplS implements IScreenshotImpl {
    // implementation for S - master (API 31+)
    // uses API:
    // IBinder getPhysicalDisplayToken(long physicalDisplayId)
    // ScreenshotHardwareBuffer captureDisplay(DisplayCaptureArgs captureArgs)

    private final Method getPhysicalDisplayToken;
    private final Constructor<?> displayCaptureArgsBuilder_ctor;
    private final Method displayCaptureArgsBuilder_setSize;
    private final Method displayCaptureArgsBuilder_build;
    private final Method screenshotHardwareBuffer_asBitmap;
    private final Method captureDisplay;

    private final int sRGB_id;
    private final int displayP3_id;

    public ScreenshotImplS() {
        try {
            getPhysicalDisplayToken = SurfaceControlWrapper.CLASS.getDeclaredMethod("getPhysicalDisplayToken", long.class);
            Class<?> screenshotHardwareBufferClass = Class.forName("android.view.SurfaceControl$ScreenshotHardwareBuffer");
            screenshotHardwareBuffer_asBitmap = screenshotHardwareBufferClass.getDeclaredMethod("asBitmap");
            Class<?> displayCaptureArgsClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
            Class<?> displayCaptureArgsBuilderClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs$Builder");
            displayCaptureArgsBuilder_ctor = displayCaptureArgsBuilderClass.getDeclaredConstructor(IBinder.class);
            displayCaptureArgsBuilder_setSize = displayCaptureArgsBuilderClass.getDeclaredMethod("setSize", int.class, int.class);
            displayCaptureArgsBuilder_build = displayCaptureArgsBuilderClass.getDeclaredMethod("build");
            captureDisplay = SurfaceControlWrapper.CLASS.getDeclaredMethod("captureDisplay", displayCaptureArgsClass);
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
            Object builder = displayCaptureArgsBuilder_ctor.newInstance(display);
            displayCaptureArgsBuilder_setSize.invoke(builder, width, height);
            Object args = displayCaptureArgsBuilder_build.invoke(builder);
            Object shb = captureDisplay.invoke(null, args);
            Bitmap hwbmp = (Bitmap) screenshotHardwareBuffer_asBitmap.invoke(shb);
            Bitmap bmp = hwbmp.copy(Bitmap.Config.ARGB_8888, false);
            ColorSpace cs = bmp.getColorSpace();
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
