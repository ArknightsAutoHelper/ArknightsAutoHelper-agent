package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ScreenshotImplKitKat implements IScreenshotImpl {
    // implementation for KitKat (API 19)
    // no color space
    // max 2 displays
    // Bitmap is software-backed

    // API used:
    // Bitmap nativeScreenshot(IBinder displayToken,
    //            int width, int height, int minLayer, int maxLayer, boolean allLayers); // for KitKat
    // IBinder getBuiltInDisplay(int builtInDisplayId)

    private final Method getBuiltinDisplay;
    private final Method nativeScreenshot;

    public ScreenshotImplKitKat() {
        try {
            getBuiltinDisplay = SurfaceControlWrapper.CLASS.getDeclaredMethod("getBuiltInDisplay", int.class);
            nativeScreenshot = SurfaceControlWrapper.CLASS.getDeclaredMethod("nativeScreenshot",
                    IBinder.class, int.class, int.class, int.class, int.class, boolean.class);
            nativeScreenshot.setAccessible(true);
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
            Bitmap bmp;
            bmp = (Bitmap) nativeScreenshot.invoke(null, display, width, height, 0, 0, true);
            ScreenshotImage.ColorSpace color = ScreenshotImage.ColorSpace.UNKNOWN;
            // FIXME: rotation
            return new ScreenshotImage(bmp, color, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
