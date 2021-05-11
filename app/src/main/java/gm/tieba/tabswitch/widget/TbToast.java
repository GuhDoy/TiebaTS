package gm.tieba.tabswitch.widget;

import android.content.Context;
import android.content.res.Resources;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Rule;

public class TbToast {
    public static int LENGTH_SHORT = 2000;
    public static int LENGTH_LONG = 3500;

    public static void showTbToast(ClassLoader classLoader, Context context, Resources res, String text, int duration) {
        try {
            Rule.findRule(res.getString(R.string.TbToast), new Rule.Callback() {
                @Override
                public void onRuleFound(String rule, String clazz, String method) throws Throwable {
                    for (Method md : classLoader.loadClass(clazz).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals(
                                "[class android.content.Context, class java.lang.String, int]")) {
                            md.invoke(null, context, text, duration);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
