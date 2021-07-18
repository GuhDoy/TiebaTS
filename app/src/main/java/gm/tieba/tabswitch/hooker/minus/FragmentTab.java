package gm.tieba.tabswitch.hooker.minus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.Parser;

public class FragmentTab extends XposedContext implements IHooker {
    private static boolean sIsFirstHook = true;

    public void hook() throws Throwable {
        AcRules.findRule(sRes.getString(R.string.FragmentTab), (AcRules.Callback) (rule, clazz, method) -> {
            for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                if (Arrays.toString(md.getParameterTypes()).equals("[class java.util.ArrayList]")) {
                    XposedBridge.hookMethod(md, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!sIsFirstHook) return;
                            for (String fieldName : Parser.parseMainTabActivityConfig()) {
                                if (Preferences.getStringSet("fragment_tab").contains(fieldName)) {
                                    Class<?> clazz = XposedHelpers.findClass(
                                            "com.baidu.tbadk.core.atomData.MainTabActivityConfig", sClassLoader);
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
        });
    }
}
