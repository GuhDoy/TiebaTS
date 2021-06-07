package gm.tieba.tabswitch.util;

import android.annotation.SuppressLint;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.XposedInit;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class Native {
    static {
        String soPath = XposedInit.sPath + "!/lib/armeabi-v7a/libtshide.so";
        for (int i = 0; i < 3; i++) {
            try {
                System.load(soPath);
                break;
            } catch (UnsatisfiedLinkError e) {
                XposedBridge.log(i + soPath);
                XposedBridge.log(e);
            }
        }
    }

    public static native boolean inline(String name);

    public static native int access(String path);

    public static native int sysaccess(String path);

    public static native String fopen(String path);

    public static native boolean findXposed();
}
