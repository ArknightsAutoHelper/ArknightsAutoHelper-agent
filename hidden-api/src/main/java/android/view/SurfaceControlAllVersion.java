package android.view;

import android.graphics.Rect;
import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public class SurfaceControlAllVersion {
    public static void openTransaction() {
        throw new UnsupportedOperationException();
    }

    public static void closeTransaction() {
        throw new UnsupportedOperationException();
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        throw new UnsupportedOperationException();
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        throw new UnsupportedOperationException();
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        throw new UnsupportedOperationException();
    }

    public static IBinder createDisplay(String name, boolean secure) {
        throw new UnsupportedOperationException();
    }

    public static void destroyDisplay(IBinder displayToken) {
        throw new UnsupportedOperationException();
    }

}
