package gm.tieba.tabswitch.util;

import org.json.JSONObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.XposedInit;

public class Development extends XposedInit {
    public static void logJSONObject() throws Throwable {
        XposedBridge.hookAllConstructors(JSONObject.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log(String.valueOf(param.args[0]));
            }
        });
    }

    private static void logMethod(Method method) throws Throwable {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.getResult() == null) return;
                XposedBridge.log(method.getName());
                XposedBridge.log(param.getResult().toString());
            }
        });
    }

    public static void logMethods(String className, ClassLoader classLoader) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods()) {
            logMethod(method);
        }
    }

    public static void logMethods(String className, ClassLoader classLoader, String methodName) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                logMethod(method);
            }
        }
    }

    public static void disableMethods(String className, ClassLoader classLoader) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods()) {
            disableMethod(method);
        }
    }

    public static void disableMethods(String className, ClassLoader classLoader, String methodName) throws Throwable {
        for (Method method : classLoader.loadClass(className).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                disableMethod(method);
            }
        }
    }

    private static void disableMethod(Method method) throws Throwable {
        if (method.getReturnType().getTypeName().equals("boolean")) {
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
        } else {
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
        }
    }
}