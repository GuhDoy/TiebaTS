package gm.tieba.tabswitch.hooker.extra;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;

public class StackTrace extends BaseHooker implements IHooker {
    public static List<String> sStes = new ArrayList<>();

    public void hook() throws Throwable {
        for (Method method : sClassLoader.loadClass("com.baidu.tieba.LogoActivity").getDeclaredMethods()) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    List<String> sts = new ArrayList<>();
                    StackTraceElement[] stes = Thread.currentThread().getStackTrace();
                    boolean isXposedStackTrace = false;
                    for (StackTraceElement ste : stes) {
                        String name = ste.getClassName();
                        if (name.contains("Activity")
                                || name.equals("android.app.Instrumentation")) break;
                        if (isXposedStackTrace) sts.add(name);
                        if (name.equals("java.lang.Thread")) isXposedStackTrace = true;
                    }

                    for (String st : sts) {
                        if (!sStes.contains(st)) sStes.add(st);
                    }
                }
            });
        }
    }
}
