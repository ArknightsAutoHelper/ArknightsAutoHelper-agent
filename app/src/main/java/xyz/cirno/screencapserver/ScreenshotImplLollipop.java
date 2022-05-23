package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenshotImplLollipop implements IScreenshotImpl {
    // implementation for Lollipop - Nougat (API 21-24)
    // no color mode
    // max 2 displays
    // Bitmap is software-backed

    // API used:

    // Bitmap nativeScreenshot(IBinder displayToken,
    //            Rect sourceCrop, int width, int height, int minLayer, int maxLayer,
    //            boolean allLayers, boolean useIdentityTransform, int rotation); // for Lollipop+
    // IBinder getBuiltInDisplay(int builtInDisplayId)

    private final Method getBuiltinDisplay;
    private final Method nativeScreenshotLollipop;

    public ScreenshotImplLollipop() {
        try {
            getBuiltinDisplay = SurfaceControlWrapper.CLASS.getDeclaredMethod("getBuiltInDisplay", int.class);
            nativeScreenshotLollipop = SurfaceControlWrapper.CLASS.getDeclaredMethod("nativeScreenshot",
                    IBinder.class, Rect.class, int.class, int.class, int.class, int.class, boolean.class, boolean.class, int.class);
            nativeScreenshotLollipop.setAccessible(true);
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
            Rect rc = new Rect();
            bmp = (Bitmap) nativeScreenshotLollipop.invoke(null, display, rc, width, height, 0, 0, true, false, 0);
            ScreenshotImage.ColorSpace color = ScreenshotImage.ColorSpace.UNKNOWN;
            // FIXME: rotation
            return new ScreenshotImage(bmp, color, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
