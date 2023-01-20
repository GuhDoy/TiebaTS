package gm.tieba.tabswitch.hooker.extra;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class StackTrace extends XposedContext implements IHooker {
    public static List<String> sStes = new ArrayList<>();

    @NonNull
    @Override
    public String key() {
        return "check_stack_trace";
    }

    @Override
    public void hook() throws Throwable {
        for (Method method : XposedHelpers.findClass("com.baidu.tieba.LogoActivity", sClassLoader).getDeclaredMethods()) {
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
