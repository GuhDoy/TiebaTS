package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class RedTip extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "red_tip";
    }

    @Override
    public void hook() throws Throwable {
//        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.tab.PagerSlidingTabBaseStrip", sClassLoader,
//                "setShowConcernRedTip", boolean.class, XC_MethodReplacement.returnConstant(null));
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
        } catch (final NoSuchMethodError e) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.view.MessageRedDotView", sClassLoader,
                    "e", XC_MethodReplacement.returnConstant(null));
        }
    }
}
