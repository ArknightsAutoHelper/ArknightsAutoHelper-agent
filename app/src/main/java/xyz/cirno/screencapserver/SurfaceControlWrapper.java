package xyz.cirno.screencapserver;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.Surface;

import java.lang.reflect.Method;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
public class SurfaceControlWrapper {
    public static final Class<?> CLASS;

    private static final Method openTransactionMethod;
    private static final Method closeTransactionMethod;
    private static final Method setDisplaySurfaceMethod;
    private static final Method setDisplayProjectionMethod;
    private static final Method setDisplayLayerStackMethod;
    private static final Method createDisplayMethod;
    private static final Method destroyDisplayMethod;


    static {
        try {
            CLASS = Class.forName("android.view.SurfaceControl");
            openTransactionMethod = CLASS.getDeclaredMethod("openTransaction");
            closeTransactionMethod = CLASS.getDeclaredMethod("closeTransactionMethod");
            setDisplaySurfaceMethod = CLASS.getDeclaredMethod("setDisplaySurface", IBinder.class, Surface.class);
            setDisplayProjectionMethod = CLASS.getDeclaredMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class);
            setDisplayLayerStackMethod = CLASS.getDeclaredMethod("setDisplayLayerStack", IBinder.class, int.class);
            createDisplayMethod = CLASS.getDeclaredMethod("createDisplay", String.class, boolean.class);
            destroyDisplayMethod = CLASS.getDeclaredMethod("destroyDisplay", IBinder.class);
        } catch (Exception e) {
            System.err.println("failed to get android.view.SurfaceControl");
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void openTransaction() {
        try {
            openTransactionMethod.invoke(null);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void closeTransaction() {
        try {
            closeTransactionMethod.invoke(null);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        try {
            setDisplaySurfaceMethod.invoke(null, displayToken, surface);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        try {
            setDisplayProjectionMethod.invoke(null, displayToken, orientation, layerStackRect, displayRect);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        try {
            setDisplayLayerStackMethod.invoke(null, displayToken, layerStack);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder createDisplay(String name, boolean secure) {
        try {
            return (IBinder) createDisplayMethod.invoke(null, name, secure);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void destroyDisplay(IBinder displayToken) {
        try {
            destroyDisplayMethod.invoke(null, displayToken);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
