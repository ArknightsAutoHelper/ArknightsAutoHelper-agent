package android.hardware.display;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.view.DisplayInfo;

public interface IDisplayManager extends IInterface {

    DisplayInfo getDisplayInfo(int displayId);
    int[] getDisplayIds();

    void registerCallback(IDisplayManagerCallback callback);


    abstract class Stub extends Binder implements IDisplayManager {
        public static IDisplayManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
