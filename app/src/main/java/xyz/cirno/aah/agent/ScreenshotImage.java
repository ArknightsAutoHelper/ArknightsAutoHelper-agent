package xyz.cirno.aah.agent;

import java.nio.ByteBuffer;

public class ScreenshotImage {


    public enum ColorSpace {
        UNKNOWN,
        SRGB,
        DISPLAY_P3;

        public static ScreenshotImage.ColorSpace fromHwcMode(int mode) {
            switch (mode) {
                case 7:
                    return SRGB;
                case 9:
                    return DISPLAY_P3;
                default:
                    return UNKNOWN;
            }
        }
    }
    public final ByteBuffer data;
    public final int width;
    public final int height;
    public final int rowStride;
    public final int pixelStride;
    public final ColorSpace colorSpace;
    public final int rotation;
    public final long timestamp;

    public ScreenshotImage(ByteBuffer data, int width, int height, int rowStride, int pixelStride, ColorSpace colorSpace, int rotation, long timestamp) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.rowStride = rowStride;
        this.pixelStride = pixelStride;
        this.colorSpace = colorSpace;
        this.rotation = rotation;
        this.timestamp = timestamp;
    }
}
