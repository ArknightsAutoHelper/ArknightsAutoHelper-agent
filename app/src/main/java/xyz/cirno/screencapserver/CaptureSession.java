package xyz.cirno.screencapserver;

import android.graphics.Bitmap;
import android.hardware.display.IDisplayManager;
import android.os.IBinder;
import android.os.ServiceManager;

import java.io.Closeable;

public abstract class CaptureSession implements Closeable {
    public abstract ScreenshotImage screenshot();


    private static final IDisplayManager sDisplayManager = IDisplayManager.Stub.asInterface(
            ServiceManager.getService("display"));
    public static IDisplayManager getDisplayManager() {
        return sDisplayManager;
    }

    public static CaptureSession createForDisplay(int logicalDisplayId) {
        return null;
    }
}
