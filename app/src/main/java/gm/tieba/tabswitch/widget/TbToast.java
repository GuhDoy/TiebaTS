package gm.tieba.tabswitch.widget;

import androidx.annotation.MainThread;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class TbToast extends XposedContext {
    public static int LENGTH_SHORT = 2000;
    public static int LENGTH_LONG = 3500;

    @MainThread
    public static void showTbToast(String text, int duration) {
        AcRules.findRule(Constants.getMatchers().get("TbToast"), (AcRules.Callback) (rule, clazz, method) -> {
            for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                if (Arrays.toString(md.getParameterTypes()).equals(
                        "[class android.content.Context, class java.lang.String, int]")) {
                    ReflectUtils.callStaticMethod(md, getContext(), text, duration);
                }
            }
        });
    }
}
