package gm.tieba.tabswitch.util;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;

public class DevelopmentHelper extends Hook {
    public static void hookAllMethods(ClassLoader classLoader, String clazz) throws Throwable {
        for (Method method : classLoader.loadClass(clazz).getDeclaredMethods()) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(clazz);
                    XposedBridge.log(method.getName());
                }
            });
        }
    }
}