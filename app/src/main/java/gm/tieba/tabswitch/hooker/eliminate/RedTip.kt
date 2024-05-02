package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class RedTip : XposedContext(), IHooker {
    override fun key(): String {
        return "red_tip"
    }

    @Throws(Throwable::class)
    override fun hook() {
//        hookReplaceMethod(
//            "com.baidu.tbadk.widget.tab.PagerSlidingTabBaseStrip",
//            "setShowConcernRedTip", Boolean::class.javaPrimitiveType
//        ) { null }
        hookReplaceMethod(
            "com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip",
            "setShowConcernRedTip", Boolean::class.javaPrimitiveType
        ) { null }
        hookReplaceMethod(
            "com.baidu.tieba.homepage.framework.indicator.PagerSlidingTabStrip",
            "setShowHotTopicRedTip", Boolean::class.javaPrimitiveType
        ) { null }
        hookReplaceMethod(
            "com.baidu.tieba.homepage.framework.indicator.ScrollFragmentTabHost",
            "setShowConcernRedTip", Boolean::class.javaPrimitiveType
        ) { null }
        hookReplaceMethod(
            "com.baidu.tieba.homepage.personalize.view.HomeTabBarView",
            "setShowConcernRedTip", Boolean::class.javaPrimitiveType
        ) { null }

        //底栏红点
        try {
            hookReplaceMethod(
                "com.baidu.tbadk.core.view.MessageRedDotView",
                "onChangeSkinType"
            ) { null }
        } catch (e: NoSuchMethodError) {
            hookReplaceMethod(
                "com.baidu.tbadk.core.view.MessageRedDotView",
                "e"
            ) { null }
        }
    }
}
