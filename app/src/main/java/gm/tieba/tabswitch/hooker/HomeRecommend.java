package gm.tieba.tabswitch.hooker;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Preferences;

public class HomeRecommend extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        XposedHelpers.setStaticIntField(sClassLoader.loadClass("com.baidu.tieba.R$string"), "home_recommend",
                XposedHelpers.getStaticIntField(sClassLoader.loadClass("com.baidu.tieba.R$string"), "enter_forum"));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic", sClassLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        if (Preferences.getIsPurifyEnter()) {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", sClassLoader, "createFragmentTabStructure", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        XposedHelpers.setIntField(param.getResult(), "type", 2);
                    } catch (NoSuchFieldError e) {
                        XposedHelpers.setIntField(param.getResult(), "e", 2);
                    }
                }
            });
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", sClassLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            } catch (XposedHelpers.ClassNotFoundError ignored) {
            }
        } else {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", sClassLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
            try {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", sClassLoader, "createFragmentTabStructure", new XC_MethodHook() {
                    @Override
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