package android.content;

import android.os.Binder;
import android.os.IBinder;

public interface IClipboard {
    void setPrimaryClip(ClipData clipData, String packageName, int userId);
    ClipData getPrimaryClip(String packageName, int userId);

    void setPrimaryClip(ClipData clipData, String packageName);
    ClipData getPrimaryClip(String packageName);

    abstract class Stub extends Binder implements IClipboard {
        public static IClipboard asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
