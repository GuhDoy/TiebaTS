package gm.tieba.tabswitch.hooker.eliminate;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;

public class RedTip extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.tab.PagerSlidingTabBaseStrip", sClassLoader,
                "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip", sClassLoader,
                "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip", sClassLoader,
                "setShowHotTopicRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.ScrollFragmentTabHost", sClassLoader,
                "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.personalize.view.HomeTabBarView", sClassLoader,
                "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
        //底栏红点
        try {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.view.MessageRedDotView", sClassLoader,
                    "onChangeSkinType", XC_MethodReplacement.returnConstant(null));
        } catch (NoSuchMethodError e) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.view.MessageRedDotView", sClassLoader,
                    "e", XC_MethodReplacement.returnConstant(null));
        }
        //我的ArrayList红点：搜索"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1"，参数为[boolean]的方法查找调用
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.personCenter.b.b$2", sClassLoader),
                    "onMessage", XC_MethodReplacement.returnConstant(null));
        } catch (XposedHelpers.ClassNotFoundError e) {
            AcRules.findRule("\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\"",
                    (AcRules.Callback) (rule, clazz, method) -> {
                        for (int j = 0; j < 2; j++) {
                            clazz = clazz.substring(0, clazz.lastIndexOf("."));
                        }
                        clazz += ".d.b$b";
                        XposedBridge.hookAllMethods(XposedHelpers.findClass(clazz, sClassLoader),
                                "onMessage", XC_MethodReplacement.returnConstant(null));
                    });
        }
    }
}
