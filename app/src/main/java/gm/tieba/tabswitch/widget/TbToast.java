package gm.tieba.tabswitch.widget;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Rule;

public class TbToast extends BaseHooker {
    public static int LENGTH_SHORT = 2000;
    public static int LENGTH_LONG = 3500;

    public static void showTbToast(String text, int duration) {
        try {
            Rule.findRule(sRes.getString(R.string.TbToast), new Rule.Callback() {
                @Override
                public void onRuleFound(String rule, String clazz, String method) throws Throwable {
                    for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals(
                                "[class android.content.Context, class java.lang.String, int]")) {
                            md.invoke(null, getContext(), text, duration);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
