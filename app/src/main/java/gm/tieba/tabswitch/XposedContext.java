package gm.tieba.tabswitch;

import android.content.Context;
import android.content.res.Resources;

import java.lang.ref.WeakReference;

public abstract class XposedContext {
    protected static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    protected static Resources sRes;
    public static String sPath;

    protected XposedContext() {
    }

    protected static Context getContext() {
        return sContextRef.get();
    }
}
