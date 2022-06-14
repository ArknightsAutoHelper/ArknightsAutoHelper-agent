package xyz.cirno.aah.agent;

import android.hardware.display.IDisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.os.ServiceManager;

import java.util.ArrayList;
import java.util.List;

public class DisplayManagerCallbackManager {
    private static DisplayManagerCallbackManager instance;

    public static DisplayManagerCallbackManager getInstance() {
        if (instance == null) {
            instance = new DisplayManagerCallbackManager();
        }
        return instance;
    }

    private DisplayManagerCallbackManager() {
        IDisplayManager dm = IDisplayManager.Stub.asInterface(ServiceManager.getService("display"));
        dm.registerCallback(new IDisplayManagerCallback.Stub() {
            @Override
            public void onDisplayEvent(int displayId, int event) {
                DisplayManagerCallbackManager.this.onDisplayEvent(displayId, event);
            }
        });
    }

    private final List<IDisplayManagerCallback> callbackList = new ArrayList<>();

    public void registerCallback(IDisplayManagerCallback callback) {
        callbackList.add(callback);
    }

    public void unregisterCallback(IDisplayManagerCallback callback) {
        callbackList.remove(callback);
    }

    public void onDisplayEvent(int displayId, int event) {
        for (IDisplayManagerCallback callback : callbackList) {
            try {
                callback.onDisplayEvent(displayId, event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
