package android.app;

import android.os.Binder;
import android.os.IBinder;

public interface IActivityManager {
    int getCurrentUserId();

    abstract class Stub extends Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
