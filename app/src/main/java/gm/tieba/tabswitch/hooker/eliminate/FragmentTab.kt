package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher
import gm.tieba.tabswitch.util.findFirstMethodByExactType
import gm.tieba.tabswitch.util.setObjectField
import org.luckypray.dexkit.query.matchers.ClassMatcher

class FragmentTab : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "fragment_tab"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            StringMatcher("has_show_message_tab_tips"),
            SmaliMatcher("Lcom/airbnb/lottie/LottieAnimationView;->setImageResource(I)V").apply {
                classMatcher = ClassMatcher.create().usingStrings("has_show_message_tab_tips")
            }
        )
    }

    @Throws(Throwable::class)
    override fun hook() {
        findRule("has_show_message_tab_tips") { _, clazz, _ ->
            val method = findFirstMethodByExactType(clazz, ArrayList::class.java)
            hookBeforeMethod(method) { param ->
                val tabsToRemove = HashSet<String>().apply {
                    if (getBoolean("home_recommend")) {
                        add("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic")
                    }
                    if (getBoolean("enter_forum")) {
                        add("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic")
                    }
                    if (getBoolean("write_thread")) {
                        add("com.baidu.tieba.write.bottomButton.WriteThreadDelegateStatic")
                        findRule("Lcom/airbnb/lottie/LottieAnimationView;->setImageResource(I)V") { _, clazz, method ->
                            val md = XposedHelpers.findMethodExactIfExists(clazz, sClassLoader, method)
                            md?.let {
                                hookBeforeMethod(md) {param ->
                                    setObjectField(
                                        param.thisObject,
                                        "com.baidu.tbadk.widget.lottie.TBLottieAnimationView",
                                        null
                                    )
                                    param.setResult(null)
                                }
                            }
                        }
                    }
                    if (getBoolean("im_message")) {
                        add("com.baidu.tieba.imMessageCenter.im.chat.notify.ImMessageCenterDelegateStatic")
                        add("com.baidu.tieba.immessagecenter.im.chat.notify.ImMessageCenterDelegateStatic")
                    }
                }

                val list = param.args[0] as? ArrayList<*>
                list?.removeIf { tab: Any -> tabsToRemove.contains(tab.javaClass.getName()) }
            }
        }
    }
}
