package xyz.cirno.screencapserver;

import android.graphics.Bitmap;

public class ScreenshotImage {
    public enum ColorSpace {
        UNKNOWN,
        SRGB,
        DISPLAY_P3,
    }
    private final Bitmap bitmap;
    private final ColorSpace colorSpace;
    private final int rotation;

    public ScreenshotImage(Bitmap bitmap, ColorSpace colorSpace, int rotation) {
        this.bitmap = bitmap;
        this.colorSpace = colorSpace;
        this.rotation = rotation;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    public int getRotation() {
        return rotation;
    }
}
