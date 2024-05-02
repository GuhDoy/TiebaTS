package gm.tieba.tabswitch.hooker.eliminate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.util.getDimen
import gm.tieba.tabswitch.util.getObjectField
import org.luckypray.dexkit.query.matchers.ClassMatcher

class PurgeMy : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "purge_my"
    }

    private val mGridTopPadding = getDimen("tbds25").toInt()
    override fun matchers(): List<Matcher> {
        return listOf(
            SmaliMatcher("Lcom/baidu/tieba/personCenter/view/PersonOftenFuncItemView;-><init>(Landroid/content/Context;)V"),
            SmaliMatcher(
                "Lcom/baidu/nadcore/download/basic/AdAppStateManager;->instance()Lcom/baidu/nadcore/download/basic/AdAppStateManager;"
            ).apply {
                classMatcher = ClassMatcher.create().usingStrings("隐私设置")
            }
        )
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            "tbclient.Profile.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType) { param ->

            // 我的贴吧会员
            XposedHelpers.setObjectField(param.thisObject, "vip_banner", null)

            // 横幅广告
            XposedHelpers.setObjectField(param.thisObject, "banner", ArrayList<Any>())

            // 度小满 有钱花
            XposedHelpers.setObjectField(param.thisObject, "finance_tab", null)

            // 小程序
            XposedHelpers.setObjectField(param.thisObject, "recom_naws_list", ArrayList<Any>())
        }

        hookBeforeMethod(
            "tbclient.User\$Builder",
            "build", Boolean::class.javaPrimitiveType) { param ->
            XposedHelpers.setObjectField(param.thisObject, "user_growth", null)
        }

        // Add padding to the top of 常用功能
        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                "Lcom/baidu/tieba/personCenter/view/PersonOftenFuncItemView;-><init>(Landroid/content/Context;)V" ->
                    hookAfterConstructor(
                        clazz,
                        "com.baidu.tbadk.TbPageContext"
                    ) { param ->
                        val mView = getObjectField(param.thisObject, View::class.java)
                        mView?.setPadding(mView.getPaddingLeft(), mGridTopPadding, mView.getPaddingRight(), 0)
                    }

                "Lcom/baidu/nadcore/download/basic/AdAppStateManager;->instance()Lcom/baidu/nadcore/download/basic/AdAppStateManager;" ->
                    hookReplaceMethod(clazz, method) { null }
            }
        }

        // 12.56+
        val personCenterMemberCardViewClass = XposedHelpers.findClassIfExists(
            "com.baidu.tieba.personCenter.view.PersonCenterMemberCardView",
            sClassLoader
        )
        personCenterMemberCardViewClass?.let {
            hookAfterConstructor(
                it,
                View::class.java
            ) { param ->
                val mView = getObjectField(param.thisObject, View::class.java)
                (mView?.parent as? ViewGroup)?.removeView(mView)
            }
        }
    }
}
