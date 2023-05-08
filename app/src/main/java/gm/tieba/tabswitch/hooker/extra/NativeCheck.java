package gm.tieba.tabswitch.hooker.extra;

import android.annotation.SuppressLint;

import gm.tieba.tabswitch.XposedContext;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class NativeCheck extends XposedContext {
    static {
        load("check");
    }

    public static native boolean inline(String name);

    public static native boolean isFindClassInline();

    public static native boolean findXposed();

    public static native int access(String path);

    public static native String fopen(String path);
}
