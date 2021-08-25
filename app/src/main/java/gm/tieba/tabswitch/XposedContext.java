package gm.tieba.tabswitch;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public abstract class XposedContext {
    protected static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    public static String sPath;
    public static Map<String, Throwable> sExceptions = new HashMap<>();

    protected static Context getContext() {
        return sContextRef.get();
    }
}
