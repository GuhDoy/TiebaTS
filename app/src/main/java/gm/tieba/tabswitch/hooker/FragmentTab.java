package gm.tieba.tabswitch.hooker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.Rule;
import gm.tieba.tabswitch.util.Parser;

public class FragmentTab extends BaseHooker implements IHooker {
    private static boolean sIsFirstHook = true;

    public void hook() throws Throwable {
        Rule.findRule(sRes.getString(R.string.FragmentTab), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) throws Throwable {
                for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                    if (Arrays.toString(md.getParameterTypes()).equals("[class java.util.ArrayList]")) {
                        XposedBridge.hookMethod(md, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!sIsFirstHook) return;
                                for (String fieldName : Parser.parseMainTabActivityConfig()) {
                                    if (Preferences.getStringSet("fragment_tab").contains(fieldName)) {
                                        Class<?> clazz = sClassLoader.loadClass(
                                                "com.baidu.tbadk.core.atomData.MainTabActivityConfig");
                                        XposedHelpers.setStaticBooleanField(clazz, fieldName,
                                                !XposedHelpers.getStaticBooleanField(clazz, fieldName));
                                    }
                                }

                                if (Preferences.getBoolean("home_recommend")) {
                                    ArrayList<?> list = (ArrayList<?>) param.args[0];
                                    for (Object tab : list) {
                                        if ("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic"
                                                .equals(tab.getClass().getName())) {
                                            list.remove(tab);
                                            break;
                                        }
                                    }
                                }

                                sIsFirstHook = false;
                            }
                        });
                    }
                }
            }
        });
    }
}
