package gm.tieba.tabswitch.hooker.eliminate

import android.view.View
import android.widget.LinearLayout
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.MethodNameMatcher
import gm.tieba.tabswitch.hooker.deobfuscation.ResMatcher
import gm.tieba.tabswitch.util.findFirstMethodByExactReturnType
import gm.tieba.tabswitch.util.getDimen
import gm.tieba.tabswitch.util.getObjectField
import gm.tieba.tabswitch.util.getR
import org.luckypray.dexkit.query.matchers.ClassMatcher
import java.lang.reflect.Modifier

class PurgeEnter : XposedContext(), IHooker, Obfuscated {

    private val mLayoutOffset = getDimen("tbds50").toInt()
    private var mInitLayoutHeight = -1
    private var mPbListViewInnerViewConstructorName: String? = null
    private lateinit var mRecForumClassName: String
    private lateinit var mRecForumSetNextPageMethodName: String

    override fun key(): String {
        return "purge_enter"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            ResMatcher(getR("dimen", "tbds400").toLong(), "dimen.tbds400").apply {
                classMatcher = ClassMatcher.create().usingStrings("enter_forum_login_tip")
            },
            MethodNameMatcher("onSuccess", "purge_enter_on_success").apply {
                classMatcher = ClassMatcher.create().usingStrings("enter_forum_login_tip")
            }
        )
    }

    override fun hook() {
        hookReplaceMethod(
            "com.baidu.tieba.enterForum.recforum.message.RecommendForumRespondedMessage",
            "getRecommendForumData"
        ) { null }

        mPbListViewInnerViewConstructorName = findClass("com.baidu.tbadk.core.view.PbListView").superclass.declaredMethods.find { method ->
            method.returnType.toString().endsWith("View") && !Modifier.isAbstract(method.modifiers)
        }?.name

        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                "dimen.tbds400" -> {
                    mRecForumClassName = clazz
                    mRecForumSetNextPageMethodName = method
                    hookReplaceMethod(clazz, method) { param ->
                        val pbListView = getObjectField(param.thisObject, "com.baidu.tbadk.core.view.PbListView")
                        val pbListViewInnerView =
                            XposedHelpers.callMethod(pbListView, mPbListViewInnerViewConstructorName) as View
                        val bdListView =
                            getObjectField(param.thisObject, "com.baidu.adp.widget.ListView.BdListView")
                        if (pbListViewInnerView.parent == null) {
                            XposedHelpers.callMethod(bdListView, "setNextPage", pbListView)
                            XposedHelpers.callMethod(bdListView, "setOverScrollMode", View.OVER_SCROLL_ALWAYS)
                        }
                        val linearLayout = getObjectField(pbListView, "android.widget.LinearLayout") as LinearLayout
                        val layoutParams = LinearLayout.LayoutParams(linearLayout.layoutParams)
                        if (mInitLayoutHeight == -1) {
                            mInitLayoutHeight = layoutParams.height + mLayoutOffset
                        }
                        layoutParams.height = mInitLayoutHeight
                        linearLayout.setLayoutParams(layoutParams)
                        XposedHelpers.callMethod(bdListView, "setExOnSrollToBottomListener", null as Any?)
                    }
                }

                "purge_enter_on_success" ->
                    hookReplaceMethod(
                        clazz,
                        method, Boolean::class.javaPrimitiveType
                    ) { param ->
                        val enterForumRec = getObjectField(param.thisObject, mRecForumClassName)
                        XposedHelpers.callMethod(enterForumRec, mRecForumSetNextPageMethodName)
                    }
            }
        }

        try {   // 12.56.4.0+ 禁用WebView进吧页
            hookReplaceMethod(
                findFirstMethodByExactReturnType(
                    "com.baidu.tieba.enterForum.helper.HybridEnterForumHelper",
                    Boolean::class.javaPrimitiveType!!
                )
            ) { false }
        } catch (ignored: ClassNotFoundError) {
        }
    }
}
