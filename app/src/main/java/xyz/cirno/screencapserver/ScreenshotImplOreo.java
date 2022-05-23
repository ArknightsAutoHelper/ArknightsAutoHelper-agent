package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(Build.VERSION_CODES.O)
public class ScreenshotImplOreo implements IScreenshotImpl {
    // implementation for Oreo - Pie (API 26-28)
    // supports color management
    // max 2 displays
    // Bitmap is attached to BufferQueue/CpuConsumer

    // API used:

    // Bitmap nativeScreenshot(IBinder displayToken,
    //            Rect sourceCrop, int width, int height, int minLayer, int maxLayer,
    //            boolean allLayers, boolean useIdentityTransform, int rotation); // for Lollipop+
    // IBinder getBuiltInDisplay(int builtInDisplayId)

    private final Method getBuiltinDisplay;
    private final Method nativeScreenshotLollipop;
    private final Method getActiveColorMode;


    public ScreenshotImplOreo() {
        try {
            getBuiltinDisplay = SurfaceControlWrapper.CLASS.getDeclaredMethod("getBuiltInDisplay", int.class);
            nativeScreenshotLollipop = SurfaceControlWrapper.CLASS.getDeclaredMethod("nativeScreenshot",
                    IBinder.class, Rect.class, int.class, int.class, int.class, int.class, boolean.class, boolean.class, int.class);
            nativeScreenshotLollipop.setAccessible(true);
            getActiveColorMode = SurfaceControlWrapper.CLASS.getDeclaredMethod("getActiveColorMode", IBinder.class);

        } catch (Exception e) {
            throw new RuntimeException("ScreenshotImplKitKat initialization failed");
        }
    }

    @Override
    public IBinder resolvePhysicalDisplay(long physicalDisplayId) {
        if (physicalDisplayId != 0 && physicalDisplayId != 1) {
            throw new IllegalArgumentException("physicalDisplayId");
        }
        try {
            return (IBinder) getBuiltinDisplay.invoke(null, (int)physicalDisplayId);
        } catch (Exception e) {
            throw new RuntimeException("resolvePhysicalDisplay invocation failed", e);
        }

    }

    @Override
    public ScreenshotImage screenshot(IBinder display, int width, int height) {
        try {
            Rect rc = new Rect();
            Bitmap hwbmp = (Bitmap) nativeScreenshotLollipop.invoke(null, display, rc, width, height, 0, 0, true, false, 0);
            Bitmap bmp = hwbmp.copy(Bitmap.Config.ARGB_8888, false);
            hwbmp.recycle();
            ScreenshotImage.ColorSpace color = ScreenshotImage.ColorSpace.UNKNOWN;
            Object hwc_color_boxed = getActiveColorMode.invoke(null, display);
            if (hwc_color_boxed != null) {
                int hwc_color = (int)hwc_color_boxed;
                if (hwc_color == 7) {
                    // COLOR_MODE_SRGB
                    color = ScreenshotImage.ColorSpace.SRGB;
                } else if (hwc_color == 9) {
                    // COLOR_MODE_DISPLAY_P3
                    color = ScreenshotImage.ColorSpace.DISPLAY_P3;
                }
            }
            // FIXME: rotation
            return new ScreenshotImage(bmp, color, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
