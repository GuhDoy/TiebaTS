package gm.tieba.tabswitch.util;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class DevelopmentHelper extends Hook {
    public static void hookAllMethods(ClassLoader classLoader, String clazz) throws Throwable {
        Method[] methods = classLoader.loadClass(clazz).getDeclaredMethods();
        for (Method method : methods) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Log.e("gm.tieba.tabswitch", clazz);
                    Log.e("gm.tieba.tabswitch", method.getName());
                }
            });
        }
    }

    public static void hookAllFields( Method method) {
        Field[] fields = method.getClass().getDeclaredFields();
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.get(method) != null) {
                        Log.e("gm.tieba.tabswitch", field.getName());
                        Log.e("gm.tieba.tabswitch", field.get(method).toString());
                    }
                }
            }
        });
    }
}