package gm.tieba.tabswitch.hookImpl;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;

public class HomeRecommend extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        if (Hook.preferenceMap.get("purify_enter") == null || !(Boolean) Hook.preferenceMap.get("purify_enter")) {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Field field;
                        try {
                            field = param.getResult().getClass().getField("type");
                        } catch (NoSuchFieldException e) {
                            field = param.getResult().getClass().getField("e");
                        }
                        field.setAccessible(true);
                        field.setInt(param.getResult(), 2);
                        Class<?> clazz = classLoader.loadClass("com.baidu.tieba.R$string");
                        Field field2 = clazz.getField("home_recommend");
                        field2.setAccessible(true);
                        field2.setInt(param.getResult(), clazz.getField("enter_forum").getInt(null));
                    }
                });
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        } else {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", classLoader, "createFragmentTabStructure", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Field field;
                    try {
                        field = param.getResult().getClass().getField("type");
                    } catch (NoSuchFieldException e) {
                        field = param.getResult().getClass().getField("e");
                    }
                    field.setAccessible(true);
                    field.setInt(param.getResult(), 2);
                    Class<?> clazz = classLoader.loadClass("com.baidu.tieba.R$string");
                    Field field2 = clazz.getField("home_recommend");
                    field2.setAccessible(true);
                    field2.setInt(param.getResult(), clazz.getField("enter_forum").getInt(null));
                }
            });
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        }
    }
}