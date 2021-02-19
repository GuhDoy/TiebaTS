package gm.tieba.tabswitch.hookImpl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;

public class RedTip extends Hook {
    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.tab.PagerSlidingTabBaseStrip", lpparam.classLoader, "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip", lpparam.classLoader, "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip", lpparam.classLoader, "setShowHotTopicRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.ScrollFragmentTabHost", lpparam.classLoader, "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.personalize.view.HomeTabBarView", lpparam.classLoader, "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.personalize.view.HomeTabBarView", lpparam.classLoader, "setShowHotTopicRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        //底栏红点
        Method method;
        try {
            method = lpparam.classLoader.loadClass("com.baidu.tbadk.core.view.MessageRedDotView").getDeclaredMethod("onChangeSkinType");
        } catch (NoSuchMethodException e) {
            method = lpparam.classLoader.loadClass("com.baidu.tbadk.core.view.MessageRedDotView").getDeclaredMethod("e");
        }
        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
        //我的ArrayList红点
        //搜索"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1"，参数为[boolean]的方法查找调用
        Class<?> clazz;
        try {
            clazz = lpparam.classLoader.loadClass("com.baidu.tieba.personCenter.b.b$2");
        } catch (ClassNotFoundException e) {
            clazz = lpparam.classLoader.loadClass("e.b.m0.d2.d.b$b");
        }
        XposedBridge.hookAllMethods(clazz, "onMessage", XC_MethodReplacement.returnConstant(null));
    }
}