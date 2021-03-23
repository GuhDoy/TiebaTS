package gm.tieba.tabswitch.util;

import android.view.MotionEvent;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class Development extends Hook {
    public static void logAllMethods(String className, ClassLoader classLoader) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods())
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(className);
                    XposedBridge.log(method.getName());
                }
            });
    }

    public static void disableMethods(String className, ClassLoader classLoader) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods())
            disableMethod(method);
    }

    public static void disableMethods(String className, ClassLoader classLoader, String methodName) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods())
            if (method.getName().equals(methodName))
                disableMethod(method);
    }

    private static void disableMethod(Method method) throws Throwable {
        if (method.getReturnType().getTypeName().equals("boolean"))
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
        else XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
    }

    public static void logMotionEvent(String className, ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod(className, classLoader, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                XposedBridge.log(String.valueOf(param.args[0]));
            }
        });
    }
}