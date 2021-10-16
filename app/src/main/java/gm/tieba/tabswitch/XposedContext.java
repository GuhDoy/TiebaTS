package gm.tieba.tabswitch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public abstract class XposedContext {
    private static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    protected static Map<String, Throwable> sExceptions = new HashMap<>();
    protected static String sPath;
    private static Handler sHandler;

    protected static void attachBaseContext(Context context) {
        sContextRef = new WeakReference<>(context.getApplicationContext());
        sHandler = new Handler(Looper.getMainLooper());
    }

    protected static Context getContext() {
        return sContextRef.get();
    }

    protected static void runOnUiThread(Runnable r) {
        sHandler.post(r);
    }
}
