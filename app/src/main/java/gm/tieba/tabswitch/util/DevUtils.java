package gm.tieba.tabswitch.util;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class DevUtils extends XposedContext {
    public static void logJSONObject() {
        XposedBridge.hookAllConstructors(JSONObject.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log(String.valueOf(param.args[0]));
            }
        });
    }

    @TestOnly
    private static void logMethod(Method method) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log(method.getDeclaringClass().getName() + ": " + method.getName());
            }
        });
    }

    public static void logMethods(String className) {
        for (Method method : XposedHelpers.findClass(className, sClassLoader).getDeclaredMethods()) {
            logMethod(method);
        }
    }

    public static void logMethods(String className, String methodName) {
        for (Method method : XposedHelpers.findClass(className, sClassLoader).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                logMethod(method);
            }
        }
    }

    public static void disableMethods(String className) {
        for (Method method : XposedHelpers.findClass(className, sClassLoader).getDeclaredMethods()) {
            disableMethod(method);
        }
    }

    public static void disableMethods(String className, String methodName) {
        for (Method method : XposedHelpers.findClass(className, sClassLoader).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                disableMethod(method);
            }
        }
    }

    @TestOnly
    private static void disableMethod(Method method) {
        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(
                method.getReturnType().equals(boolean.class) ? true : null));
    }
}
