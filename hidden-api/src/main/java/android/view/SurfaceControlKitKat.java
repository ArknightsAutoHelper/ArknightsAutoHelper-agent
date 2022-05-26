package android.view;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public class SurfaceControlKitKat {
    public static Bitmap screenshot(int width, int height) {
        throw new UnsupportedOperationException();
    }
    IBinder getBuiltInDisplay(int builtInDisplayId){
        throw new UnsupportedOperationException();
    }

}
