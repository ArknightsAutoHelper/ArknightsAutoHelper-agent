package xyz.cirno.screencapserver;

import android.graphics.Bitmap;
import android.os.IBinder;

public interface IScreenshotImpl {
    IBinder resolvePhysicalDisplay(long physicalDisplayId);
    ScreenshotImage screenshot(IBinder display, int width, int height);
}
