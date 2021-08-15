package gm.tieba.tabswitch;

import android.content.Context;

import java.lang.ref.WeakReference;

public abstract class XposedContext {
    protected static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    public static String sPath;

    protected static Context getContext() {
        return sContextRef.get();
    }
}
