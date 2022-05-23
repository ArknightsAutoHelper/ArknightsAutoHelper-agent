package xyz.cirno.screencapserver;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Entry {
    public static void main(String[] args) {
        IScreenshotImpl impl = getScreenshotImpl();
        System.out.printf("using implementation %s\n", impl.getClass().getName());

        long t0 = System.nanoTime();

        IBinder displayToken = impl.resolvePhysicalDisplay(0);
        System.out.println("displayToken resolved");

        System.out.println("first run");
        runScreenshot(impl, displayToken);
        System.out.println("second run");
        runScreenshot(impl, displayToken);

    }

    private static void runScreenshot(IScreenshotImpl impl, IBinder displayToken) {
        long t0 = System.nanoTime();
        ScreenshotImage img = impl.screenshot(displayToken, 0, 0);
        Bitmap bmp = img.getBitmap();
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int stride = bmp.getRowBytes();
        int allocSize = bmp.getAllocationByteCount();

        Buffer buf = ByteBuffer.allocate(bmp.getAllocationByteCount());
        bmp.copyPixelsToBuffer(buf);
        long t1 = System.nanoTime();
        System.out.printf("screenshot copyPixelsToBuffer take %.03f ms\n", (t1-t0)/1.0e6);
        System.out.printf("width %d, height %d, stride %d, height*stride %d, alloc size %d\n", width, height, stride, height*stride, allocSize);

        try (OutputStream s = new FileOutputStream("/data/local/tmp/a.png")) {
            t0 = System.nanoTime();
            img.getBitmap().compress(Bitmap.CompressFormat.PNG, 0, s);
            t1 = System.nanoTime();
            s.close();
            System.out.printf("compressed to /data/local/tmp/a.png in %.03f ms\n", (t1-t0)/1.0e6);
        } catch (IOException ignored) {}
    }

    private static IScreenshotImpl getScreenshotImpl() {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.KITKAT:
                return new ScreenshotImplKitKat();
            case Build.VERSION_CODES.LOLLIPOP:
            case Build.VERSION_CODES.LOLLIPOP_MR1:
            case Build.VERSION_CODES.M:
            case Build.VERSION_CODES.N:
                return new ScreenshotImplLollipop();
            case Build.VERSION_CODES.N_MR1:
                return new ScreenshotImplNougatMR1();
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.P:
                return new ScreenshotImplOreo();
            case Build.VERSION_CODES.Q:
            case Build.VERSION_CODES.R:
                return new ScreenshotImplQ();
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return new ScreenshotImplS();
                } else {
                    throw new UnsupportedOperationException("version not supported");
                }

        }
    }
}
