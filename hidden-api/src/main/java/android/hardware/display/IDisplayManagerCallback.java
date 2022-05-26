package android.hardware.display;

import android.os.Binder;

public interface IDisplayManagerCallback {
    int EVENT_DISPLAY_ADDED = 1;
    int EVENT_DISPLAY_CHANGED = 2;
    int EVENT_DISPLAY_REMOVED = 3;
    int EVENT_DISPLAY_BRIGHTNESS_CHANGED = 4;

    void onDisplayEvent(int displayId, int event);
    abstract class Stub extends Binder implements IDisplayManagerCallback {}
}
