package xyz.cirno.aah.agent;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
@TargetApi(Build.VERSION_CODES.KITKAT)
public class CaptureSessionKitKat extends CaptureSession {
    // implementation for KitKat (API 19)
    // no color space
    // Bitmap is software-backed

    public CaptureSessionKitKat(int logicalDisplayId) {
        if (logicalDisplayId != 0) {
            throw new IllegalArgumentException("only display 0 is supported on KitKat");
        }
    }

    @Override
    public ScreenshotImage screenshot() {
        throw new UnsupportedOperationException();
//        try {
//            Bitmap bmp = SurfaceControlKitKat.screenshot(0, 0);
//            // FIXME: rotation
//            return new ScreenshotImage(bmp, ScreenshotImage.ColorSpace.UNKNOWN, 0);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
    }

    @Override
    public void close() {

    }
}
