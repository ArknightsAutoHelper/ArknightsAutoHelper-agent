package xyz.cirno.aah.agent;

import android.app.IActivityManager;
import android.content.ClipData;
import android.content.IClipboard;
import android.os.ServiceManager;

import java.lang.reflect.Method;

public abstract class ClipboardWrapper {
    private static final String PACKAGE_NAME = "com.android.shell";
    public abstract ClipData getPrimaryClip();
    public abstract void setPrimaryClip(ClipData clipData);

    private static ClipboardWrapper instance;
    public static ClipboardWrapper getInstance() {
        if (instance == null) {
            try {
                Method getPrimaryClipWithUserId = IClipboard.class.getMethod("getPrimaryClip", String.class, int.class);
                instance = new ClipboardMultiUserImpl();
            } catch (NoSuchMethodException e) {
                instance = new ClipboardLegacyImpl();
            }
        }
        return instance;
    }

    private static class ClipboardLegacyImpl extends ClipboardWrapper {
        private final IClipboard clipboard;

        public ClipboardLegacyImpl() {
            clipboard = IClipboard.Stub.asInterface(ServiceManager.getService("clipboard"));
        }

        @Override
        public ClipData getPrimaryClip() {
            return clipboard.getPrimaryClip(PACKAGE_NAME);
        }

        @Override
        public void setPrimaryClip(ClipData clipData) {
            clipboard.setPrimaryClip(clipData, PACKAGE_NAME);
        }
    }

    private static class ClipboardMultiUserImpl extends ClipboardWrapper {
        private final IClipboard clipboard;
        private final IActivityManager am;

        public ClipboardMultiUserImpl() {
            clipboard = IClipboard.Stub.asInterface(ServiceManager.getService("clipboard"));
            am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
        }

        private int getUserId() {
            return am.getCurrentUserId();
        }

        @Override
        public ClipData getPrimaryClip() {
            return clipboard.getPrimaryClip(PACKAGE_NAME, getUserId());
        }

        @Override
        public void setPrimaryClip(ClipData clipData) {
            clipboard.setPrimaryClip(clipData, PACKAGE_NAME, getUserId());
        }
    }
}
