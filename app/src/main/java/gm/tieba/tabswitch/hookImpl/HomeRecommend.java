package gm.tieba.tabswitch.hookImpl;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class HomeRecommend extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.setStaticIntField(classLoader.loadClass("com.baidu.tieba.R$string"), "home_recommend",
                XposedHelpers.getStaticIntField(classLoader.loadClass("com.baidu.tieba.R$string"), "enter_forum"));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        if (Objects.equals(Hook.preferenceMap.get("purify_enter"), true)) {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", classLoader, "createFragmentTabStructure", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedHelpers.setIntField(param.getResult(), "type", 2);
                    } catch (NoSuchFieldError e) {
                        XposedHelpers.setIntField(param.getResult(), "e", 2);
                    }
                }
            });
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        } else {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            XposedHelpers.setIntField(param.getResult(), "type", 2);
                        } catch (NoSuchFieldError e) {
                            XposedHelpers.setIntField(param.getResult(), "e", 2);
                        }
                    }
                });
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        }
    }
}