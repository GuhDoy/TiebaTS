package gm.tieba.tabswitch;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;

public abstract class XposedContext {
    private static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    protected static Map<String, Throwable> sExceptions = new HashMap<>(0);
    protected static String sPath;
    private static Handler sHandler;

    protected static void attachBaseContext(final Context context) {
        if (sContextRef != null) {
            throw new IllegalStateException("Base context already set");
        }
        sContextRef = new WeakReference<>(context.getApplicationContext());
        sHandler = new Handler(Looper.getMainLooper());
    }

    protected static Context getContext() {
        return sContextRef.get();
    }

    protected static void load(final String filename) {
        final var soPaths = Arrays.stream(Build.SUPPORTED_ABIS)
                .map(abi -> sPath + "!/lib/" + abi + "/lib" + filename + ".so")
                .collect(Collectors.toList());
        UnsatisfiedLinkError err = null;
        for (final var soPath : soPaths) {
            try {
                System.load(soPath);
                err = null;
                break;
            } catch (final UnsatisfiedLinkError e) {
                err = e;
            }
        }
        if (err != null) {
            XposedBridge.log(err);
            throw err;
        }
    }

    protected static void runOnUiThread(final Runnable r) {
        sHandler.post(r);
    }
}
